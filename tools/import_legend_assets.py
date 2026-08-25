#!/usr/bin/env python3
"""Convert the iOS vector legend symbols to Android PNG drawables."""

from __future__ import annotations

import argparse
import subprocess
from pathlib import Path


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
            subprocess.run(
                [
                    "sips",
                    "-s",
                    "format",
                    "png",
                    "--resampleWidth",
                    "160",
                    str(source),
                    "--out",
                    str(output),
                ],
                check=True,
                stdout=subprocess.DEVNULL,
            )


if __name__ == "__main__":
    main()
