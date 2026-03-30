import requests
import json
import re
import hashlib
import time
from urllib.parse import urljoin

HEADERS = {
    "User-Agent": "Mozilla/5.0",
    "Accept-Language": "he-IL,he;q=0.9,en-US;q=0.8",
    "Accept": "text/html",
    "Connection": "keep-alive",
}

OUTPUT_FILE = "./pyy/recipes.json"

# -------------------------
# HTML
# -------------------------
def fetch_html(url):
    session = requests.Session()

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language": "he-IL,he;q=0.9,en-US;q=0.8",
        "Accept-Encoding": "gzip, deflate, br",
        "Connection": "keep-alive",
        "Upgrade-Insecure-Requests": "1",
        "Referer": "https://www.mako.co.il/",
        "Cache-Control": "max-age=0",
    }

    # קודם נכנסים לעמוד הראשי (חשוב!!)
    session.get("https://www.mako.co.il", headers=headers)

    # ואז לבקשה האמיתית
    res = session.get(url, headers=headers, timeout=10)
    res.raise_for_status()

    return res.text

# -------------------------
# JSON-LD
# -------------------------
def extract_ldjson(html):
    matches = re.findall(
        r'<script type="application/ld\+json">(.*?)</script>',
        html,
        re.DOTALL
    )

    for match in matches:
        try:
            data = json.loads(match)

            if isinstance(data, list):
                for item in data:
                    if item.get("@type") == "Recipe":
                        return item

            elif isinstance(data, dict):
                if data.get("@type") == "Recipe":
                    return data

        except:
            continue

    return None

# -------------------------
# תמונה
# -------------------------
def extract_image(image_field):
    if not image_field:
        return None

    if isinstance(image_field, str):
        return image_field

    if isinstance(image_field, dict):
        return image_field.get("url")

    if isinstance(image_field, list):
        first = image_field[0]

        if isinstance(first, str):
            return first

        if isinstance(first, dict):
            return first.get("url")

    return None


def extract_image_from_html(html):
    match = re.search(r'<img[^>]+src="([^"]+)"', html)
    if match:
        return match.group(1)
    return None

# -------------------------
# קישורים מעמוד קטגוריה
# -------------------------
def extract_recipe_links(html, base_url="https://www.mako.co.il"):
    links = re.findall(r'href="([^"]+Recipe-[^"]+\.htm)"', html)

    full_links = []
    for link in links:
        if link.startswith("http"):
            full_links.append(link)
        else:
            full_links.append(urljoin(base_url, link))

    return list(set(full_links))

# -------------------------
# מיפוי
# -------------------------
def map_recipe(data, source_url, html):
    if not data:
        return None

    title = data.get("name")
    ingredients = data.get("recipeIngredient", [])

    steps = []
    instructions = data.get("recipeInstructions", [])

    for step in instructions:
        if isinstance(step, dict):
            text = step.get("text")
            if text:
                steps.append(text)
        elif isinstance(step, str):
            steps.append(step)

    if not title or len(ingredients) == 0:
        return None

    image = extract_image(data.get("image"))

    if not image:
        image = extract_image_from_html(html)

    return {
        "title": title,
        "imageUrl": image,
        "ingredients": ingredients,
        "steps": steps,
        "sourceUrl": source_url,
        "dateAdded": int(time.time() * 1000),
        "fingerprint": hashlib.sha256(title.encode()).hexdigest(),
        "tags": ["טבעוני"]
    }

# -------------------------
# שמירה
# -------------------------
def save_recipe(recipe, file_path):
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            existing = json.load(f)
    except:
        existing = []

    fingerprints = {r["fingerprint"] for r in existing}

    if recipe["fingerprint"] not in fingerprints:
        existing.append(recipe)

    with open(file_path, "w", encoding="utf-8") as f:
        json.dump(existing, f, ensure_ascii=False, indent=2)

# -------------------------
# סקרייפ מתכון
# -------------------------
def scrape_mako(url):
    html = fetch_html(url)
    data = extract_ldjson(html)
    return map_recipe(data, url, html)

# -------------------------
# סקרייפ קטגוריה
# -------------------------
def scrape_category(url):
    html = fetch_html(url)
    links = extract_recipe_links(html)

    print(f"נמצאו {len(links)} מתכונים\n")

    for link in links:
        try:
            recipe = scrape_mako(link)

            if recipe:
                save_recipe(recipe, OUTPUT_FILE)
                print("✔ נשמר:", recipe["title"])
            else:
                print("❌ לא מתכון:", link)

        except Exception as e:
            print("שגיאה:", link, e)

# -------------------------
# MAIN
# -------------------------
if __name__ == "__main__":
    print("הדבק קישור (מתכון או קטגוריה)\n")

    while True:
        url = input("URL: ").strip()


        if url == "":
            print("סיום.")
            break

        try:
            if "recipes_column" in url:
                pages = input("pages: ").strip()
                for i in range(1, int(pages) + 1):
                    page_url = f"{url}&page={i}"
                    print(f"\nעמוד {i}: {page_url}")
                    scrape_category(f"{url}&page={i}")
            else:
                recipe = scrape_mako(url)

                if recipe:
                    save_recipe(recipe, OUTPUT_FILE)
                    print("✔ נשמר:", recipe["title"])
                else:
                    print("❌ לא נמצא מתכון")

        except Exception as e:
            print("שגיאה:", e)