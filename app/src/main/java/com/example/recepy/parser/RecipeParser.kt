package com.example.recepy.parser

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

data class ParsedRecipe(
    val title: String,
    val imageUrl: String?,
    val ingredients: List<String>,
    val steps: List<String>
)

class RecipeParser {

    fun parse(document: Document): ParsedRecipe {
        return parseFromJsonLd(document) ?: parseHeuristic(document)
    }

    fun parseFromText(rawText: String): ParsedRecipe {
        val lines = rawText
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return ParsedRecipe(
                title = "מתכון ללא כותרת",
                imageUrl = null,
                ingredients = emptyList(),
                steps = emptyList()
            )
        }

        val loweredLines = lines.map { it.lowercase() }
        val ingredientHeaderIndex = loweredLines.indexOfFirst(::isIngredientHeader)
        val instructionsHeaderIndex = loweredLines.indexOfFirst(::isInstructionHeader)

        val title = extractTitleFromText(lines, loweredLines)

        val ingredients = collectSectionLines(
            lines = lines,
            sectionHeaderIndex = ingredientHeaderIndex,
            otherHeaderIndex = instructionsHeaderIndex
        ).flatMap(::splitText)
            .map(::cleanPastedLine)
            .filter { it.isNotBlank() }

        val steps = collectSectionLines(
            lines = lines,
            sectionHeaderIndex = instructionsHeaderIndex,
            otherHeaderIndex = ingredientHeaderIndex
        ).flatMap(::splitText)
            .map(::cleanPastedLine)
            .filter { it.isNotBlank() }

        val fallbackSteps = if (ingredients.isEmpty() && steps.isEmpty()) {
            lines.drop(1).map(::cleanPastedLine).filter { it.isNotBlank() }
        } else {
            steps
        }

        return ParsedRecipe(
            title = title.ifBlank { "מתכון ללא כותרת" },
            imageUrl = null,
            ingredients = ingredients.distinct().take(80),
            steps = fallbackSteps.distinct().take(80)
        )
    }

    private fun parseFromJsonLd(document: Document): ParsedRecipe? {
        for (script in document.select("script[type=application/ld+json]")) {
            val json = script.data().ifBlank { script.html() }.trim()
            if (json.isBlank()) continue
            val parsed = runCatching {
                val root = JsonParser.parseString(json)
                val recipe = findRecipe(root) ?: return@runCatching null
                val ingredients = normalize(readStrings(recipe.get("recipeIngredient")))
                val steps = normalize(readInstructions(recipe.get("recipeInstructions")))
                if (ingredients.isEmpty() && steps.isEmpty()) return@runCatching null
                ParsedRecipe(
                    title = extractTitle(recipe, document),
                    imageUrl = extractImage(recipe.get("image"), document) ?: fallbackImage(document),
                    ingredients = ingredients,
                    steps = steps
                )
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    private fun parseHeuristic(document: Document): ParsedRecipe {
        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: document.selectFirst("h1")?.text()?.takeIf { it.isNotBlank() }
            ?: document.title().ifBlank { "מתכון ללא כותרת" }

        val image = document.selectFirst("meta[property=og:image]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?.let { toAbsUrl(document, it) }
            ?: fallbackImage(document)

        val ingredients = extractListNearKeywords(document, INGREDIENT_KEYWORDS)
            .ifEmpty { normalize(document.select("ul li").map { it.text() }.take(25)) }

        val steps = extractListNearKeywords(document, INSTRUCTION_KEYWORDS)
            .ifEmpty { normalize(document.select("ol li").map { it.text() }.take(30)) }

        return ParsedRecipe(title, image, ingredients, steps)
    }

    private fun findRecipe(element: JsonElement?): JsonObject? {
        if (element == null || element.isJsonNull) return null
        return when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                if (isRecipeType(obj.get("@type"))) obj
                else {
                    obj.entrySet().forEach { (_, child) ->
                        val found = findRecipe(child)
                        if (found != null) return found
                    }
                    null
                }
            }
            element.isJsonArray -> {
                element.asJsonArray.forEach {
                    val found = findRecipe(it)
                    if (found != null) return found
                }
                null
            }
            else -> null
        }
    }

    private fun isRecipeType(element: JsonElement?): Boolean {
        if (element == null || element.isJsonNull) return false
        return when {
            element.isJsonPrimitive -> element.asString.contains("Recipe", ignoreCase = true)
            element.isJsonArray -> element.asJsonArray.any { isRecipeType(it) }
            else -> false
        }
    }

    private fun extractTitle(recipe: JsonObject, document: Document): String {
        val name = recipe.get("name")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        return if (name.isNotBlank()) name
        else document.selectFirst("h1")?.text()?.takeIf { it.isNotBlank() }
            ?: document.title().ifBlank { "מתכון ללא כותרת" }
    }

    private fun readStrings(element: JsonElement?): List<String> {
        if (element == null || element.isJsonNull) return emptyList()
        return when {
            element.isJsonPrimitive -> splitText(element.asString)
            element.isJsonArray -> element.asJsonArray.flatMap { readStrings(it) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                listOf("text", "name", "value", "description")
                    .firstNotNullOfOrNull { key -> obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString }
                    ?.let(::splitText)
                    .orEmpty()
            }
            else -> emptyList()
        }
    }

    private fun readInstructions(element: JsonElement?): List<String> {
        if (element == null || element.isJsonNull) return emptyList()
        return when {
            element.isJsonPrimitive -> splitText(element.asString)
            element.isJsonArray -> element.asJsonArray.flatMap { readInstructions(it) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val text = obj.get("text")?.takeIf { it.isJsonPrimitive }?.asString
                if (!text.isNullOrBlank()) splitText(text)
                else readInstructions(obj.get("itemListElement")) + readInstructions(obj.get("steps"))
            }
            else -> emptyList()
        }
    }

    private fun extractListNearKeywords(document: Document, keywords: List<String>): List<String> {
        val normalizedKeywords = keywords.map { it.lowercase() }
        
        // Strategy 1: Find by section/article containing the keyword
        val containers = document.select("section,article,div[class*=recipe],div[id*=recipe]")
            .filter { hasKeyword(it.text().take(300), normalizedKeywords) }
        
        for (container in containers) {
            val items = container.select("li, p").map { it.text().trim() }
                .filter { it.length > 2 && !hasKeyword(it, normalizedKeywords) }
            if (items.size >= 3) return normalize(items)
        }

        // Strategy 2: Find headings and look for the next list/paragraph block
        val headings = document.select("h1,h2,h3,h4,h5,h6,strong,b,p[class*=title],p[class*=header]")
            .filter { hasKeyword(it.text(), normalizedKeywords) }

        for (heading in headings) {
            var sibling = heading.nextElementSibling()
            // Look ahead up to 3 siblings for a list or group of paragraphs
            repeat(3) {
                if (sibling != null) {
                    val items = sibling!!.select("li").map { it.text() }
                        .ifEmpty { 
                            // If no <li>, check if it's a div containing many paragraphs
                            sibling!!.select("p").map { it.text() }.filter { it.length > 5 }
                        }
                    if (items.size >= 2) return normalize(items)
                    sibling = sibling!!.nextElementSibling()
                }
            }
        }
        return emptyList()
    }

    private fun extractImage(element: JsonElement?, document: Document): String? {
        if (element == null || element.isJsonNull) return null
        return when {
            element.isJsonPrimitive -> toAbsUrl(document, element.asString)
            element.isJsonArray -> element.asJsonArray.asSequence().mapNotNull { extractImage(it, document) }.firstOrNull()
            element.isJsonObject -> {
                val obj = element.asJsonObject
                listOf("url", "contentUrl", "thumbnailUrl")
                    .firstNotNullOfOrNull { key -> obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString }
                    ?.let { toAbsUrl(document, it) }
            }
            else -> null
        }
    }

    private fun fallbackImage(document: Document): String? {
        val og = document.selectFirst("meta[property=og:image]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?.let { toAbsUrl(document, it) }
        if (og != null) return og

        val large = document.select("img[src]").firstOrNull { img ->
            (img.attr("width").toIntOrNull() ?: 0) >= 300 ||
                (img.attr("height").toIntOrNull() ?: 0) >= 220
        }
        if (large != null) return large.absUrl("src").ifBlank { large.attr("src") }

        return document.selectFirst("img[src]")
            ?.let { image -> image.absUrl("src").ifBlank { image.attr("src") } }
            ?.takeIf { it.isNotBlank() }
    }

    private fun normalize(lines: List<String>): List<String> {
        return lines.flatMap(::splitText)
            .map { Jsoup.parse(it).text() }
            .map { it.replace(Regex("^[\\-•·\\d\\.\\)\\s]+"), "").trim() }
            .filter { it.length > 1 }
            .distinct()
            .take(80)
    }

    private fun splitText(value: String): List<String> {
        return value.replace("\r", "\n")
            .split("\n", "•", "●", "|", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun extractTitleFromText(lines: List<String>, loweredLines: List<String>): String {
        val explicitTitle = lines.firstNotNullOfOrNull { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2 && TITLE_KEYWORDS.any { parts[0].lowercase().contains(it) }) {
                parts[1].trim().takeIf { it.isNotBlank() }
            } else {
                null
            }
        }
        if (!explicitTitle.isNullOrBlank()) {
            return explicitTitle
        }

        val firstLine = lines.first()
        if (!isIngredientHeader(loweredLines.first()) && !isInstructionHeader(loweredLines.first())) {
            if (firstLine.contains(":")) {
                val afterColon = firstLine.substringAfter(":", "").trim()
                if (afterColon.isNotBlank()) {
                    return afterColon
                }
            }
            return cleanPastedLine(firstLine)
        }

        return "מתכון ללא כותרת"
    }

    private fun collectSectionLines(
        lines: List<String>,
        sectionHeaderIndex: Int,
        otherHeaderIndex: Int
    ): List<String> {
        if (sectionHeaderIndex < 0 || sectionHeaderIndex >= lines.lastIndex + 1) {
            return emptyList()
        }

        val sectionHeaderLine = lines[sectionHeaderIndex]
        val inlineContent = sectionHeaderLine.substringAfter(":", "").trim()

        val from = sectionHeaderIndex + 1
        val until = if (otherHeaderIndex > sectionHeaderIndex) {
            otherHeaderIndex
        } else {
            lines.size
        }

        val sectionLines = mutableListOf<String>()
        if (inlineContent.isNotBlank()) {
            sectionLines.add(inlineContent)
        }
        sectionLines.addAll(lines.subList(from.coerceAtMost(lines.size), until.coerceAtMost(lines.size)))

        return sectionLines
    }

    private fun cleanPastedLine(line: String): String {
        return line
            .replace(Regex("^[•\\-*\\s]+"), "")
            .replace(Regex("^\\d+[\\).:\\-\\s]+"), "")
            .trim()
    }

    private fun isIngredientHeader(line: String): Boolean {
        return TEXT_INGREDIENT_HEADERS.any { line.contains(it) }
    }

    private fun isInstructionHeader(line: String): Boolean {
        return TEXT_INSTRUCTION_HEADERS.any { line.contains(it) }
    }

    private fun hasKeyword(text: String, keywords: List<String>): Boolean {
        val lowered = text.lowercase()
        return keywords.any { lowered.contains(it) }
    }

    private fun toAbsUrl(document: Document, url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) return trimmed
        if (trimmed.startsWith("//")) return "https:$trimmed"
        return runCatching { URL(URL(document.location()), trimmed).toString() }.getOrNull() ?: trimmed
    }

    private companion object {
        val INGREDIENT_KEYWORDS = listOf(
            "ingredients", "ingredient", "מצרכים", "רכיבים", "מה צריך", "החומרים", "המרכיבים"
        )
        val INSTRUCTION_KEYWORDS = listOf(
            "instructions", "instruction", "directions", "method", "preparation",
            "אופן הכנה", "הוראות הכנה", "שלבי הכנה", "איך מכינים", "הכנה", "תהליך ההכנה"
        )
        val TEXT_INGREDIENT_HEADERS = listOf("מצרכים", "רכיבים", "מה צריך", "החומרים", "ingredients")
        val TEXT_INSTRUCTION_HEADERS = listOf(
            "הוראות", "אופן הכנה", "הכנה", "איך מכינים", "instructions", "directions"
        )
        val TITLE_KEYWORDS = listOf("שם", "title", "recipe", "מתכון")
    }
}
