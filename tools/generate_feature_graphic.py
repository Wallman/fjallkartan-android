#!/usr/bin/env python3
"""Generate the 1024x500 Google Play feature graphic from the app icon.

Regenerate after the icon or tagline changes:
    python3 tools/generate_feature_graphic.py
"""

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ICON = ROOT / "fastlane" / "metadata" / "android" / "en-US" / "images" / "icon.png"
METADATA_ROOT = ROOT / "fastlane" / "metadata" / "android"

WIDTH, HEIGHT = 1024, 500
TOP_COLOR = (246, 228, 190)  # cream, sampled from the app icon sky
BOTTOM_COLOR = (52, 94, 101)  # teal, sampled from the app icon foreground
TITLE = "Fjällkartan"
TAGLINE_LINES = ("Offline topographic maps", "for Nordic hiking")


def build_background() -> Image.Image:
    bg = Image.new("RGB", (WIDTH, HEIGHT))
    for y in range(HEIGHT):
        t = y / (HEIGHT - 1)
        pixel = tuple(
            round(TOP_COLOR[i] + (BOTTOM_COLOR[i] - TOP_COLOR[i]) * t) for i in range(3)
        )
        for x in range(WIDTH):
            bg.putpixel((x, y), pixel)

    draw = ImageDraw.Draw(bg, "RGBA")
    for i in range(-6, 14):
        y0 = i * 60
        points = [
            (x, y0 + 22 * math.sin(x / 140 + i)) for x in range(0, WIDTH + 1, 16)
        ]
        draw.line(points, fill=(255, 255, 255, 26), width=2)
    return bg


def paste_icon(bg: Image.Image) -> tuple[int, int]:
    icon_size = 340
    icon = Image.open(ICON).convert("RGBA").resize((icon_size, icon_size), Image.LANCZOS)

    mask = Image.new("L", (icon_size, icon_size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, icon_size, icon_size], radius=round(icon_size * 0.22), fill=255
    )
    icon.putalpha(mask)

    icon_x, icon_y = 70, (HEIGHT - icon_size) // 2
    bg.paste(icon, (icon_x, icon_y), icon)
    return icon_x, icon_size


def draw_text(bg: Image.Image, text_x: int) -> None:
    draw = ImageDraw.Draw(bg, "RGBA")
    title_font = ImageFont.truetype(
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf", 92
    )
    sub_font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 38)

    draw.text((text_x, 150), TITLE, font=title_font, fill=(255, 255, 255, 255))
    draw.text(
        (text_x, 260), TAGLINE_LINES[0], font=sub_font, fill=(240, 240, 235, 235)
    )
    draw.text(
        (text_x, 305), TAGLINE_LINES[1], font=sub_font, fill=(240, 240, 235, 235)
    )


def main() -> None:
    bg = build_background()
    icon_x, icon_size = paste_icon(bg)
    draw_text(bg, icon_x + icon_size + 60)

    locales = sorted(
        p.name for p in METADATA_ROOT.iterdir() if p.is_dir() and (p / "images").is_dir()
    )
    for locale in locales:
        destination = METADATA_ROOT / locale / "images" / "featureGraphic.png"
        bg.save(destination)
    print(f"Wrote feature graphic for {len(locales)} locales")


if __name__ == "__main__":
    main()
