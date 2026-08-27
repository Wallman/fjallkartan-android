#!/usr/bin/env python3
"""Convert the iOS vector legend symbols to Android PNG drawables.

Uses PyMuPDF (pip install pymupdf) to rasterize each PDF directly at the
target resolution. `sips --resampleWidth` renders these small (~10pt)
vector symbols at a low internal resolution and then upscales the bitmap,
which looks blurry once magnified to legend row size; PyMuPDF re-renders
the vector paths at the requested pixel size instead, so the output stays
crisp.
"""

from __future__ import annotations

import argparse
from pathlib import Path

import fitz  # PyMuPDF

TARGET_WIDTH = 320


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--ios-root",
        type=Path,
        default=Path("../fjallkartan/fjallkartan/Assets.xcassets"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("app/src/main/res/drawable-nodpi"),
    )
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    for country in ("LegendNO", "LegendSE"):
        prefix = country.lower().replace("legend", "legend_")
        for source in sorted((args.ios_root / country).glob("*.imageset/*.pdf")):
            output = args.output / f"{prefix}_{source.stem}.png"
            doc = fitz.open(source)
            page = doc[0]
            zoom = TARGET_WIDTH / page.rect.width
            pixmap = page.get_pixmap(matrix=fitz.Matrix(zoom, zoom), alpha=True)
            pixmap.save(output)
            doc.close()


if __name__ == "__main__":
    main()
