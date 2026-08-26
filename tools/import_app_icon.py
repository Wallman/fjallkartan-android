#!/usr/bin/env python3
"""Generate Android launcher and Google Play icons from the iOS source icon."""

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (
    ROOT.parent
    / "fjallkartan"
    / "fjallkartan"
    / "Assets.xcassets"
    / "AppIcon.appiconset"
    / "AppIcon-1024.png"
)
RES = ROOT / "app" / "src" / "main" / "res"

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
LAUNCHER_CONTENT_FRACTION = 0.78


def zoom_out_with_edge_extension(
    source: Image.Image,
    content_fraction: float = LAUNCHER_CONTENT_FRACTION,
) -> Image.Image:
    """Inset artwork while extending its edge pixels instead of adding a frame."""
    size = source.width
    inner_size = round(size * content_fraction)
    inset = (size - inner_size) // 2
    far_edge = inset + inner_size
    inner = source.resize((inner_size, inner_size), Image.Resampling.LANCZOS)
    result = Image.new(source.mode, (size, size))
    result.paste(inner, (inset, inset))

    result.paste(inner.crop((0, 0, inner_size, 1)).resize((inner_size, inset)), (inset, 0))
    result.paste(
        inner.crop((0, inner_size - 1, inner_size, inner_size)).resize((inner_size, size - far_edge)),
        (inset, far_edge),
    )
    result.paste(inner.crop((0, 0, 1, inner_size)).resize((inset, inner_size)), (0, inset))
    result.paste(
        inner.crop((inner_size - 1, 0, inner_size, inner_size)).resize((size - far_edge, inner_size)),
        (far_edge, inset),
    )

    result.paste(inner.getpixel((0, 0)), (0, 0, inset, inset))
    result.paste(inner.getpixel((inner_size - 1, 0)), (far_edge, 0, size, inset))
    result.paste(inner.getpixel((0, inner_size - 1)), (0, far_edge, inset, size))
    result.paste(
        inner.getpixel((inner_size - 1, inner_size - 1)),
        (far_edge, far_edge, size, size),
    )
    return result


def inset_transparent(source: Image.Image) -> Image.Image:
    inner_size = round(source.width * LAUNCHER_CONTENT_FRACTION)
    inset = (source.width - inner_size) // 2
    result = Image.new("RGBA", source.size, (0, 0, 0, 0))
    result.alpha_composite(
        source.resize((inner_size, inner_size), Image.Resampling.LANCZOS),
        (inset, inset),
    )
    return result


def rotated_bar(
    cx: float,
    cy: float,
    half_length: float,
    half_thickness: float,
    angle: float,
) -> list[tuple[float, float]]:
    cosine, sine = math.cos(angle), math.sin(angle)
    corners = [
        (-half_length, -half_thickness),
        (half_length, -half_thickness),
        (half_length, half_thickness),
        (-half_length, half_thickness),
    ]
    return [
        (cx + x * cosine - y * sine, cy + x * sine + y * cosine)
        for x, y in corners
    ]


def monochrome_marker(size: int = 1024) -> Image.Image:
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    scale = float(size)
    center_x, center_y = 0.33 * scale, 0.56 * scale
    arm = 0.24 * scale
    thickness = 0.038 * scale
    angle = math.radians(33)
    top = center_y - (arm * math.sin(angle) + thickness * math.cos(angle)) - 0.03 * scale
    draw.rectangle(
        [
            center_x - 0.026 * scale,
            top,
            center_x + 0.026 * scale,
            1.05 * scale,
        ],
        fill=(0, 0, 0, 255),
    )
    for direction in (angle, -angle):
        draw.polygon(
            rotated_bar(center_x, center_y, arm, thickness, direction),
            fill=(0, 0, 0, 255),
        )
    return image


def save_launcher_icons(source: Image.Image) -> None:
    for density, size in DENSITIES.items():
        directory = RES / f"mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        resized = source.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(directory / "ic_launcher.png", optimize=True)

        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        round_icon = resized.convert("RGBA")
        round_icon.putalpha(mask)
        round_icon.save(directory / "ic_launcher_round.png", optimize=True)

        for old_name in ("ic_launcher.webp", "ic_launcher_round.webp"):
            (directory / old_name).unlink(missing_ok=True)


def save_adaptive_layers(source: Image.Image) -> None:
    directory = RES / "drawable-nodpi"
    directory.mkdir(parents=True, exist_ok=True)
    source.save(directory / "ic_launcher_art_bitmap.png", optimize=True)
    inset_transparent(monochrome_marker()).save(
        directory / "ic_launcher_monochrome.png",
        optimize=True,
    )


def save_play_icons(source: Image.Image) -> None:
    metadata = ROOT / "fastlane" / "metadata" / "android"
    icon = source.resize((512, 512), Image.Resampling.LANCZOS)
    for locale in sorted(path for path in metadata.iterdir() if path.is_dir()):
        images = locale / "images"
        images.mkdir(parents=True, exist_ok=True)
        icon.save(images / "icon.png", optimize=True)


def main() -> None:
    source = Image.open(SOURCE).convert("RGB")
    launcher_source = zoom_out_with_edge_extension(source)
    save_launcher_icons(launcher_source)
    save_adaptive_layers(launcher_source)
    save_play_icons(source)
    print("Generated Android launcher and Google Play icons.")


if __name__ == "__main__":
    main()
