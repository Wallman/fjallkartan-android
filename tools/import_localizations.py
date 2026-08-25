#!/usr/bin/env python3
"""Import the iOS string catalogue for Android's product UI."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / "fjallkartan" / "fjallkartan" / "Localizable.xcstrings"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "localizations.json"
LANGUAGES = ("da", "de", "es", "fi", "fr", "it", "nb", "nl", "sv", "zh-Hans")


def main() -> None:
    catalogue = json.loads(SOURCE.read_text())
    result = {language: {} for language in LANGUAGES}
    for key, entry in catalogue["strings"].items():
        for language in LANGUAGES:
            unit = entry.get("localizations", {}).get(language, {}).get("stringUnit", {})
            value = unit.get("value")
            if value:
                result[language][key] = value
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, separators=(",", ":")) + "\n")
    print(f"Wrote {sum(map(len, result.values()))} translations to {OUTPUT}")


if __name__ == "__main__":
    main()
