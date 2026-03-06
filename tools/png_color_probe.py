#!/usr/bin/env python3
"""
Extract useful accent colors from a PNG icon.

Usage:
  python3 tools/png_color_probe.py app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png
"""

from __future__ import annotations

import argparse
import colorsys
import json
from collections import Counter
from pathlib import Path

from PIL import Image


def to_hex(rgb: tuple[int, int, int]) -> str:
    return f"#{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}"


def quantize(rgb: tuple[int, int, int], step: int = 8) -> tuple[int, int, int]:
    r, g, b = rgb
    return ((r // step) * step, (g // step) * step, (b // step) * step)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="PNG accent color probe")
    parser.add_argument("png", type=Path, help="Path to PNG file")
    parser.add_argument("--alpha-min", type=int, default=48, help="Minimum alpha to include pixel")
    parser.add_argument("--sat-min", type=float, default=0.20, help="Minimum saturation for chroma sample")
    parser.add_argument("--val-min", type=float, default=0.15, help="Minimum value/brightness for chroma sample")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    image = Image.open(args.png).convert("RGBA")

    opaque = Counter()
    chroma = Counter()
    for r, g, b, a in image.getdata():
        if a < args.alpha_min:
            continue
        q = quantize((r, g, b))
        opaque[q] += 1
        h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
        if s >= args.sat_min and v >= args.val_min:
            chroma[q] += 1

    top_opaque = opaque.most_common(8)
    top_chroma = chroma.most_common(8)
    suggested = top_chroma[0][0] if top_chroma else (top_opaque[0][0] if top_opaque else (0, 0, 0))

    payload = {
        "file": str(args.png),
        "size": {"width": image.width, "height": image.height},
        "top_opaque": [{"hex": to_hex(rgb), "count": count} for rgb, count in top_opaque],
        "top_chroma": [{"hex": to_hex(rgb), "count": count} for rgb, count in top_chroma],
        "suggested_accent": to_hex(suggested),
    }
    print(json.dumps(payload, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
