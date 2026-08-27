#!/usr/bin/env python3
"""Composes Google Play screenshots from the raw device captures.

Reuses ../fjallkartan/tools/compose_screenshots.py for the palette, per-language
captions, scene list and device-frame drawing, so the two stores' screenshots
never drift apart in wording or style. Only the canvas geometry differs here:
Google Play caps a screenshot's longer side at twice its shorter side, which
the iOS iPhone canvas (1320x2868, ratio ~2.17) violates, so Android uses its
own portrait canvases sized to stay under that limit.

Regenerate after replacing a raw screenshot or editing the shared captions in
the iOS repo's compose_screenshots.py:
    python3 tools/generate_play_screenshots.py
"""

import importlib.util
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RAW_ROOT = ROOT / "raw-screenshots"
METADATA_ROOT = ROOT / "fastlane" / "metadata" / "android"

IOS_TOOLS = ROOT.parent / "fjallkartan" / "tools"


def _load_compose_screenshots():
    path = IOS_TOOLS / "compose_screenshots.py"
    if not path.is_file():
        raise SystemExit(
            f"expected the iOS reference project at {path}; "
            "check out Wallman/fjallkartan next to this repo"
        )
    spec = importlib.util.spec_from_file_location("compose_screenshots", path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


compose_screenshots = _load_compose_screenshots()

# compose() only reads canvas/device_width/device_top/device_corner/
# text_safe_width/out_prefix off this dict; raw_dir_name and per_locale_raw
# are the iOS script's own lookup keys and aren't needed here since the raw
# directory is passed in directly below.
DEVICES = {
    "phone": {
        "raw_dir": RAW_ROOT / "phone",
        "out_dir_name": "phoneScreenshots",
        "canvas": (1434, 2868),  # exactly 2:1, Google Play's max allowed ratio
        "out_prefix": "",
        "device_width": 0.76,
        # Pushed lower than the iOS iPhone canvas (0.255) to leave enough
        # room for the caption: the raw phone capture (1344x2992) is more
        # elongated than iOS's own raw capture, and this canvas is narrower
        # relative to its height, so the same absolute caption font sizes
        # need more vertical space above the device frame.
        "device_top": 0.30,
        "device_corner": 0.125,
        "text_safe_width": 1250,
    },
    "tablet": {
        "raw_dir": RAW_ROOT / "tablet",
        "out_dir_name": "tenInchScreenshots",
        "canvas": (1600, 2560),  # matches the raw 10" capture's own ratio
        "out_prefix": "",
        "device_width": 0.69,
        "device_top": 0.266,
        "device_corner": 0.045,
        "text_safe_width": 1430,
    },
}

# compose_screenshots.py's language tag -> this repo's Play locale directory,
# matching the mapping already used by tools/import_play_metadata.py.
PLAY_LOCALES = {
    "en": "en-US",
    "sv": "sv-SE",
    "nb": "no-NO",
    "da": "da-DK",
    "fi": "fi-FI",
    "de": "de-DE",
    "fr": "fr-FR",
    "it": "it-IT",
    "es": "es-ES",
    "nl": "nl-NL",
    "zh-Hans": "zh-CN",
}


def main() -> None:
    missing_locales = sorted(set(PLAY_LOCALES.values()) - {p.name for p in METADATA_ROOT.iterdir()})
    if missing_locales:
        raise SystemExit(f"no Play metadata locale directory for: {', '.join(missing_locales)}")

    written = 0
    for device in DEVICES.values():
        for language, play_locale in PLAY_LOCALES.items():
            out_dir = METADATA_ROOT / play_locale / "images" / device["out_dir_name"]
            for existing in out_dir.glob("*.png") if out_dir.is_dir() else ():
                existing.unlink()
            for scene in compose_screenshots.SCENES:
                compose_screenshots.compose(scene, device["raw_dir"], out_dir, device, language)
                written += 1
    print(f"Wrote {written} Play screenshots across {len(DEVICES)} device types and {len(PLAY_LOCALES)} locales")


if __name__ == "__main__":
    main()
