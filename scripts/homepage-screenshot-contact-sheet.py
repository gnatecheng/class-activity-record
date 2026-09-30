#!/usr/bin/env python3
"""Build a contact sheet: c-week zh/light reference row + four captured locale/theme rows."""
from __future__ import annotations

from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    raise SystemExit("Install Pillow: pip install Pillow")

ROOT = Path(__file__).resolve().parents[1]
REF_DIR = Path("/tmp/c-week/site/assets/screens/class-record")
CAPTURED = ROOT / "docs" / "screenshots" / "homepage"
OUT = Path("/opt/cursor/artifacts/homepage-screenshot-contact-sheet.png")
STEMS = ["01-home", "02-attendance", "03-payment", "04-ledger", "05-members"]
ROWS = [
    ("c-week zh/light (reference)", REF_DIR),
    ("group-matters zh/light", CAPTURED / "zh" / "light"),
    ("group-matters zh/dark", CAPTURED / "zh" / "dark"),
    ("group-matters en/light", CAPTURED / "en" / "light"),
    ("group-matters en/dark", CAPTURED / "en" / "dark"),
]
THUMB_W = 180
LABEL_H = 28
PAD = 8


def load_thumb(path: Path) -> Image.Image:
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    thumb_h = max(1, round(h * THUMB_W / w))
    return im.resize((THUMB_W, thumb_h), Image.Resampling.LANCZOS)


def main() -> None:
    thumbs = [[load_thumb(row_dir / f"{stem}.webp") for stem in STEMS] for _, row_dir in ROWS]
    row_heights = [max(t.size[1] for t in row) + LABEL_H for row in thumbs]
    width = PAD * 2 + len(STEMS) * (THUMB_W + PAD)
    height = PAD * 2 + sum(row_heights) + PAD * (len(ROWS) - 1)
    sheet = Image.new("RGB", (width, height), (245, 245, 245))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    y = PAD
    for (label, _), row_thumbs, row_h in zip(ROWS, thumbs, row_heights):
        draw.text((PAD, y), label, fill=(20, 20, 20), font=font)
        y += LABEL_H
        x = PAD
        for thumb in row_thumbs:
            sheet.paste(thumb, (x, y), thumb)
            x += THUMB_W + PAD
        y += row_h - LABEL_H + PAD
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT, optimize=True)
    print(OUT, OUT.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
