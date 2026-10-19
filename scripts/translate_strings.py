
import copy
import re
import time
import xml.etree.ElementTree as ET
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
SOURCE = RES / "values" / "strings.xml"
API = "http://127.0.0.1:5000/translate"

LANGUAGES = {
    "Brazilian Portuguese": ("pt", "values-pt-rBR"),
    "Spanish": ("es", "values-es"),
    "Simplified Chinese": ("zh", "values-zh-rCN"),
    "Japanese": ("ja", "values-ja"),
}

PLACEHOLDER = re.compile(
    r"%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[a-zA-Z%]"
)


def translate(text, language):
    if not text or not text.strip():
        return text

    protected = {}

    def replace(match):
        token = f"ZXQTOKEN{len(protected)}QXZ"
        protected[token] = match.group()
        return token

    original = text
    text = PLACEHOLDER.sub(replace, text)

    for attempt in range(3):
        try:
            response = requests.post(
                API,
                data={
                    "q": text,
                    "source": "en",
                    "target": language,
                    "format": "text",
                },
                timeout=60,
            )
            response.raise_for_status()
            result = response.json()["translatedText"]

            for token, value in protected.items():
                result = result.replace(token, value)

            if any(token in result for token in protected):
                return original

            return result

        except requests.RequestException as error:
            if attempt == 2:
                print(f"Translation failed: {error}")
                return None
            time.sleep(2)

    return None


def resource_key(element):
    return element.tag, element.get("name", "")


def translate_element(element, language):
    if element.get("translatable") == "false":
        return False

    changed = False

    if element.tag in ("string", "item"):
        if element.text and element.text.strip():
            result = translate(element.text, language)
            if result is None:
                return False
            element.text = result
            changed = True

        for child in element:
            if child.tail and child.tail.strip():
                result = translate(child.tail, language)
                if result is None:
                    return False
                child.tail = result
                changed = True

    elif element.tag in ("string-array", "array"):
        for item in element.findall("item"):
            if item.text and item.text.strip():
                result = translate(item.text, language)
                if result is None:
                    return False
                item.text = result
                changed = True

    elif element.tag == "plurals":
        for item in element.findall("item"):
            if item.text and item.text.strip():
                result = translate(item.text, language)
                if result is None:
                    return False
                item.text = result
                changed = True

    return changed


def save_xml(root, destination):
    tree = ET.ElementTree(root)
    ET.indent(tree, space="    ")
    tree.write(
        destination,
        encoding="utf-8",
        xml_declaration=True,
    )


def main():
    if not SOURCE.exists():
        raise SystemExit(f"English source not found: {SOURCE}")

    try:
        requests.get(API.replace("/translate", "/languages"), timeout=5).raise_for_status()
    except requests.RequestException:
        raise SystemExit(
            "LibreTranslate is not running at http://127.0.0.1:5000"
        )

    print("\nSmartIsland Translation Generator")
    print("1. Reuse existing translations and add missing strings")
    print("2. Start from scratch and regenerate all translations")
    print("3. Exit")

    choice = input("\nChoose (1/2/3): ").strip()

    if choice == "3":
        return
    if choice not in ("1", "2"):
        raise SystemExit("Invalid choice.")

    source_root = ET.parse(SOURCE).getroot()

    for name, (language, folder) in LANGUAGES.items():
        destination = RES / folder / "strings.xml"
        destination.parent.mkdir(parents=True, exist_ok=True)

        existing = {}
        if choice == "1" and destination.exists():
            existing_root = ET.parse(destination).getroot()
            existing = {
                resource_key(element): element
                for element in existing_root
            }

        output_root = ET.Element("resources")
        failures = 0

        print(f"\n{name}: {destination.relative_to(ROOT)}")

        for source_element in source_root:
            key = resource_key(source_element)

            if source_element.get("translatable") == "false":
                output_root.append(copy.deepcopy(source_element))
                continue

            if choice == "1" and key in existing:
                output_root.append(copy.deepcopy(existing[key]))
                continue

            element = copy.deepcopy(source_element)

            if not translate_element(element, language):
                has_text = bool(
                    (element.text and element.text.strip())
                    or list(element)
                )
                if has_text:
                    failures += 1
                    if key in existing:
                        output_root.append(copy.deepcopy(existing[key]))
                    else:
                        output_root.append(copy.deepcopy(source_element))
                    continue

            output_root.append(element)

        if failures:
            print(f"Warning: {failures} resource(s) could not be translated.")

        save_xml(output_root, destination)
        print("Saved.")

    print("\nFinished. Review the generated XML before building.")


if __name__ == "__main__":
    main()
