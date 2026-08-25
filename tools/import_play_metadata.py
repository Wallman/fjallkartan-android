#!/usr/bin/env python3
"""Create Google Play metadata from the checked-in iOS App Store copy."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IOS_METADATA = ROOT.parent / "fjallkartan" / "fastlane" / "metadata"
PLAY_METADATA = ROOT / "fastlane" / "metadata" / "android"

LOCALES = {
    "da": "da-DK",
    "de-DE": "de-DE",
    "en-US": "en-US",
    "es-ES": "es-ES",
    "fi": "fi-FI",
    "fr-FR": "fr-FR",
    "it": "it-IT",
    "nl-NL": "nl-NL",
    "no": "no-NO",
    "sv": "sv-SE",
    "zh-Hans": "zh-CN",
}

FIELDS = {
    "name": "title",
    "subtitle": "short_description",
    "description": "full_description",
}


def main() -> None:
    for source_locale, play_locale in LOCALES.items():
        source = IOS_METADATA / source_locale
        destination = PLAY_METADATA / play_locale
        destination.mkdir(parents=True, exist_ok=True)
        for source_field, play_field in FIELDS.items():
            value = (source / f"{source_field}.txt").read_text().strip()
            (destination / f"{play_field}.txt").write_text(value + "\n")
    print(f"Wrote Google Play metadata for {len(LOCALES)} locales to {PLAY_METADATA}")


if __name__ == "__main__":
    main()
