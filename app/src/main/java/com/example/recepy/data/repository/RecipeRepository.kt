package com.example.recepy.data.repository

import android.content.Context
import com.example.recepy.data.local.RecipeDao
import com.example.recepy.data.local.RecipeEntity
import com.example.recepy.parser.RecipeParser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class Recipe(
    val id: Long = -1,
    val title: String,
    val imageUrl: String?,
    val ingredients: List<String>,
    val steps: List<String>,
    val sourceUrl: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val rating: Int = 0,
    val notes: String = "",
    val lastCooked: Long = 0L
)

class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val parser: RecipeParser = RecipeParser(),
    private val gson: Gson = Gson()
) {

    private data class BundledRecipePayload(
        val title: String = "",
        val imageUrl: String? = null,
        val ingredients: List<String> = emptyList(),
        val steps: List<String> = emptyList(),
        val sourceUrl: String = "",
        val dateAdded: Long = 0L,
        val fingerprint: String = "",
        val isFavorite: Boolean = false,
        val tags: List<String> = emptyList()
    )

    private val listType = object : TypeToken<List<String>>() {}.type
    private val bundledRecipesType = object : TypeToken<List<BundledRecipePayload>>() {}.type

    suspend fun extractRecipeFromUrl(rawUrl: String): Recipe {
        val normalizedUrl = normalizeUrl(rawUrl)
        val document = Jsoup.connect(normalizedUrl)
            .timeout(10_000) // Reduced timeout for faster loading
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36") // More standard user agent
            .get()

        val parsedRecipe = parser.parse(document)

        return Recipe(
            title = parsedRecipe.title,
            imageUrl = parsedRecipe.imageUrl,
            ingredients = parsedRecipe.ingredients,
            steps = parsedRecipe.steps,
            sourceUrl = normalizedUrl,
            dateAdded = System.currentTimeMillis(),
            isFavorite = false,
            tags = emptyList()
        )
    }

    fun extractRecipeFromText(rawText: String): Recipe {
        val parsedRecipe = parser.parseFromText(rawText)

        return Recipe(
            title = parsedRecipe.title,
            imageUrl = null,
            ingredients = parsedRecipe.ingredients,
            steps = parsedRecipe.steps,
            sourceUrl = "pasted-text",
            dateAdded = System.currentTimeMillis(),
            isFavorite = false,
            tags = emptyList()
        )
    }

    fun observeSavedRecipes(): Flow<List<Recipe>> {
        return recipeDao.observeRecipes().map { entities ->
            entities.map(::entityToRecipe)
        }
    }

    suspend fun getRecipeById(id: Long): Recipe? {
        return recipeDao.getRecipeById(id)?.let(::entityToRecipe)
    }

    suspend fun saveRecipe(recipe: Recipe): Long {
        val allExisting = recipeDao.getAllRecipesOnce()
        
        val normalizedCurrentUrl = normalizeUrl(recipe.sourceUrl)
        val isWebUrl = normalizedCurrentUrl.startsWith("http", ignoreCase = true)
        
        // 1. If it's a web URL, check if that URL already exists (normalized)
        if (isWebUrl) {
            val duplicateUrl = allExisting.find { 
                normalizeUrl(it.sourceUrl) == normalizedCurrentUrl 
            }
            if (duplicateUrl != null) return -2L // Special code for duplicate URL
        }

        // 2. Check if content (fingerprint) or title already exists
        val currentFingerprint = recipeFingerprint(recipe)
        val normalizedTitle = normalizeFingerprintValue(recipe.title)
        
        val duplicateContent = allExisting.any { 
            normalizeFingerprintValue(it.title) == normalizedTitle ||
            recipeFingerprint(entityToRecipe(it)) == currentFingerprint
        }
        if (duplicateContent) return -3L // Special code for duplicate content

        val safeDate = if (recipe.dateAdded > 0L) recipe.dateAdded else System.currentTimeMillis()
        val entity = recipeToEntity(recipe.copy(dateAdded = safeDate))
        return recipeDao.insert(entity)
    }

    suspend fun deleteRecipe(id: Long) {
        recipeDao.deleteById(id)
    }

    private val importMutex = Mutex()

    suspend fun exportAllRecipesJson(): String {
        val recipes = recipeDao.getAllRecipesOnce()
            .map(::entityToRecipe)
            .filter { it.sourceUrl != "מתכון מערכת" }
        return gson.toJson(recipes)
    }

    suspend fun importRecipesFromJson(json: String): Int = importMutex.withLock {
        val type = object : TypeToken<List<BundledRecipePayload>>() {}.type
        val payloads = runCatching {
            gson.fromJson<List<BundledRecipePayload>>(json, type)
        }.getOrNull() ?: return 0
        
        return performImport(payloads)
    }

    suspend fun importRemoteRecipes(url: String): Int = importMutex.withLock {
        return withContext(Dispatchers.IO) {
            val json = runCatching {
                java.net.URL(url).readText(StandardCharsets.UTF_8)
            }.getOrNull() ?: return@withContext 0
            
            val type = object : TypeToken<List<BundledRecipePayload>>() {}.type
            val payloads = runCatching {
                gson.fromJson<List<BundledRecipePayload>>(json, type)
            }.getOrNull() ?: return@withContext 0
            
            performImport(payloads)
        }
    }

    suspend fun importBundledRecipes(context: Context, assetName: String = "recipes_seed.json"): Int = importMutex.withLock {
        val bundledRecipes = loadBundledRecipes(context, assetName)
        if (bundledRecipes.isEmpty()) return 0
        return performImport(bundledRecipes)
    }

    private suspend fun performImport(payloads: List<BundledRecipePayload>): Int {
        val allExisting = recipeDao.getAllRecipesOnce()
        
        // Track what we've already processed IN THIS RUN to catch duplicates within the same JSON
        val processedUrls = mutableSetOf<String>()
        val processedFingerprints = mutableSetOf<String>()
        val processedTitles = mutableSetOf<String>()

        // Initialize with what's already in DB
        allExisting.forEach { 
            processedUrls.add(normalizeUrl(it.sourceUrl))
            processedFingerprints.add(recipeFingerprint(entityToRecipe(it)))
            processedTitles.add(normalizeFingerprintValue(it.title))
        }

        var insertedCount = 0

        payloads.forEach { payload ->
            val title = payload.title.trim()
            if (title.isBlank()) return@forEach

            val ingredients = payload.ingredients.map { it.trim() }.filter { it.isNotBlank() }
            val steps = payload.steps.map { it.trim() }.filter { it.isNotBlank() }

            val normalizedPayloadUrl = normalizeUrl(payload.sourceUrl)
            val isWebUrl = normalizedPayloadUrl.startsWith("http", ignoreCase = true)
            val currentFingerprint = buildFingerprint(title, ingredients, steps)
            val normalizedTitle = normalizeFingerprintValue(title)

            // 1. Check if exists by URL
            if (isWebUrl && processedUrls.contains(normalizedPayloadUrl)) {
                // Potential image update if existing has none
                val existing = allExisting.find { normalizeUrl(it.sourceUrl) == normalizedPayloadUrl }
                if (existing != null && existing.imageUrl.isNullOrBlank() && !payload.imageUrl.isNullOrBlank()) {
                    recipeDao.insert(existing.copy(imageUrl = payload.imageUrl))
                    insertedCount++
                }
                return@forEach
            }

            // 2. Check if exists by Title or Content
            if (processedTitles.contains(normalizedTitle) || processedFingerprints.contains(currentFingerprint)) {
                return@forEach
            }

            // If we reach here, it's a new recipe
            val recipe = Recipe(
                title = title,
                imageUrl = payload.imageUrl,
                ingredients = ingredients,
                steps = steps,
                sourceUrl = if (isWebUrl) normalizedPayloadUrl else "מתכון מערכת",
                dateAdded = payload.dateAdded.takeIf { it > 0L } ?: System.currentTimeMillis(),
                isFavorite = payload.isFavorite,
                tags = payload.tags
            )

            recipeDao.insert(recipeToEntity(recipe))
            
            // CRITICAL: Add to our tracking sets so the NEXT iteration knows it exists!
            processedUrls.add(normalizedPayloadUrl)
            processedTitles.add(normalizedTitle)
            processedFingerprints.add(currentFingerprint)

            insertedCount++
        }

        return insertedCount
    }

    private fun loadBundledRecipes(context: Context, assetName: String): List<BundledRecipePayload> {
        val json = runCatching {
            context.assets.open(assetName).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return emptyList()

        if (json.isBlank()) return emptyList()

        return runCatching {
            gson.fromJson<List<BundledRecipePayload>>(json, bundledRecipesType)
        }.getOrNull().orEmpty()
    }

    private fun entityToRecipe(entity: RecipeEntity): Recipe {
        return Recipe(
            id = entity.id,
            title = entity.title,
            imageUrl = entity.imageUrl,
            ingredients = parseJsonList(entity.ingredients),
            steps = parseJsonList(entity.steps),
            sourceUrl = entity.sourceUrl,
            dateAdded = entity.dateAdded,
            isFavorite = entity.isFavorite,
            tags = parseJsonList(entity.tags ?: "[]"),
            rating = entity.rating,
            notes = entity.notes,
            lastCooked = entity.lastCooked
        )
    }

    private fun recipeToEntity(recipe: Recipe): RecipeEntity {
        return RecipeEntity(
            id = if (recipe.id > 0) recipe.id else 0,
            title = recipe.title,
            imageUrl = recipe.imageUrl,
            ingredients = gson.toJson(recipe.ingredients),
            steps = gson.toJson(recipe.steps),
            sourceUrl = recipe.sourceUrl,
            dateAdded = recipe.dateAdded,
            isFavorite = recipe.isFavorite,
            tags = gson.toJson(recipe.tags),
            rating = recipe.rating,
            notes = recipe.notes,
            lastCooked = recipe.lastCooked
        )
    }

    private fun parseJsonList(value: String): List<String> {
        return runCatching { gson.fromJson<List<String>>(value, listType) }.getOrNull().orEmpty()
    }

    private fun recipeFingerprint(recipe: Recipe): String {
        return buildFingerprint(recipe.title, recipe.ingredients, recipe.steps)
    }

    private fun buildFingerprint(title: String, ingredients: List<String>, steps: List<String>): String {
        val canonical = buildString {
            append(normalizeFingerprintValue(title))
            append("|")
            append(ingredients.joinToString(separator = "|") { normalizeFingerprintValue(it) })
            append("|")
            append(steps.joinToString(separator = "|") { normalizeFingerprintValue(it) })
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun normalizeFingerprintValue(value: String): String {
        return value.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}