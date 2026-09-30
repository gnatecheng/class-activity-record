#!/usr/bin/env python3
"""Generate 团团记 launcher icons (团 character, blue family) for mipmaps and docs/."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
DOCS = ROOT / "docs"
BG_TOP = (41, 98, 255)
BG_BOTTOM = (26, 68, 204)
FG = (255, 255, 255)

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def find_cjk_font(size: int) -> ImageFont.FreeTypeFont:
    candidates = [
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
        "/usr/share/fonts/truetype/noto/NotoSansCJK-Bold.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansSC-Bold.otf",
    ]
    for path in candidates:
        if Path(path).is_file():
            return ImageFont.truetype(path, size=size)
    raise SystemExit("Install Noto Sans CJK (fonts-noto-cjk) for launcher generation")


def lerp(a: int, b: int, t: float) -> int:
    return int(a + (b - a) * t)


def draw_background(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for y in range(size):
        t = y / max(size - 1, 1)
        color = (
            lerp(BG_TOP[0], BG_BOTTOM[0], t),
            lerp(BG_TOP[1], BG_BOTTOM[1], t),
            lerp(BG_TOP[2], BG_BOTTOM[2], t),
            255,
        )
        draw.line([(0, y), (size, y)], fill=color)
    return img


def draw_glyph(size: int, *, transparent_bg: bool) -> Image.Image:
    img = (
        Image.new("RGBA", (size, size), (0, 0, 0, 0))
        if transparent_bg
        else draw_background(size)
    )
    draw = ImageDraw.Draw(img)
    font_size = round(size * 0.52)
    font = find_cjk_font(font_size)
    text = "团"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x = (size - tw) / 2 - bbox[0]
    y = (size - th) / 2 - bbox[1] - size * 0.02
    draw.text((x, y), text, font=font, fill=FG + (255,))
    return img


def save_png(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.convert("RGBA").save(path, optimize=True)


def main() -> None:
    # Adaptive foreground (108dp @ xxxhdpi = 432px)
    fg_adaptive = draw_glyph(432, transparent_bg=True)
    save_png(fg_adaptive, RES / "drawable-nodpi/ic_launcher_foreground.png")

    for folder, size in DENSITIES.items():
        icon = draw_glyph(size, transparent_bg=False)
        out_dir = RES / folder
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            save_png(icon, out_dir / name)
            print(out_dir / name, (out_dir / name).stat().st_size, "bytes")

    icon_512 = draw_glyph(512, transparent_bg=False)
    save_png(icon_512, DOCS / "launcher-icon-512.png")
    icon_512.save(DOCS / "launcher-icon.webp", "WEBP", quality=88, method=6)
    print(DOCS / "launcher-icon-512.png", (DOCS / "launcher-icon-512.png").stat().st_size)
    print(DOCS / "launcher-icon.webp", (DOCS / "launcher-icon.webp").stat().st_size)


if __name__ == "__main__":
    main()
