#!/usr/bin/env python3
"""Resize Roborazzi PNG captures to 540px-wide WebP for docs/screenshots/homepage/."""
from __future__ import annotations

from pathlib import Path

try:
    from PIL import Image
except ImportError:
    raise SystemExit("Install Pillow: pip install Pillow")

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "app" / "build" / "homepage-screenshots-raw"
OUT = ROOT / "docs" / "screenshots" / "homepage"
TARGET_WIDTH = 540
WEBP_QUALITY = 85
NAMES = [
    "01-home",
    "02-attendance",
    "03-payment",
    "04-ledger",
    "05-members",
]
LOCALES = ("zh", "en")
THEMES = ("light", "dark")


def export_one(png: Path, webp: Path) -> None:
    im = Image.open(png).convert("RGBA")
    w, h = im.size
    if w != TARGET_WIDTH:
        new_h = max(1, round(h * TARGET_WIDTH / w))
        im = im.resize((TARGET_WIDTH, new_h), Image.Resampling.LANCZOS)
    webp.parent.mkdir(parents=True, exist_ok=True)
    im.save(webp, "WEBP", quality=WEBP_QUALITY, method=6)


def main() -> None:
    if not RAW.is_dir():
        raise SystemExit(f"Missing raw captures: {RAW} (run recordRoborazziDebug first)")
    count = 0
    for lang in LOCALES:
        for theme in THEMES:
            for stem in NAMES:
                png = RAW / lang / theme / f"{stem}.png"
                if not png.is_file():
                    raise SystemExit(f"Missing capture: {png}")
                webp = OUT / lang / theme / f"{stem}.webp"
                export_one(png, webp)
                count += 1
                print(webp.relative_to(ROOT), webp.stat().st_size, "bytes")
    print(f"Exported {count} WebP files to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
