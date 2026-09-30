#!/usr/bin/env python3
"""Regenerate mipmap PNG launcher icons from drawable/ic_launcher_fg.xml and ic_launcher_bg."""
from __future__ import annotations

import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FG = ROOT / "app/src/main/res/drawable/ic_launcher_fg.xml"
BG_COLOR = "#2962FF"
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def paths_from_vector(xml_text: str) -> list[tuple[str, str]]:
    root = ET.fromstring(xml_text)
    out: list[tuple[str, str]] = []
    for path in root.iter("path"):
        d = path.get(f"{ANDROID_NS}pathData")
        fill = path.get(f"{ANDROID_NS}fillColor", "#000000")
        if d:
            out.append((d, fill))
    return out


def compose_svg(fg_paths: list[tuple[str, str]]) -> str:
    fg = "\n    ".join(f'<path d="{d}" fill="{fill}"/>' for d, fill in fg_paths)
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">
  <rect width="108" height="108" fill="{BG_COLOR}"/>
  {fg}
</svg>
"""


def rsvg_to_png(svg: str, size: int, out_png: Path) -> None:
    with tempfile.NamedTemporaryFile("w", suffix=".svg", delete=False, encoding="utf-8") as f:
        f.write(svg)
        svg_path = f.name
    subprocess.run(
        ["rsvg-convert", "-w", str(size), "-h", str(size), svg_path, "-o", str(out_png)],
        check=True,
    )
    Path(svg_path).unlink(missing_ok=True)


def main() -> None:
    fg_paths = paths_from_vector(FG.read_text(encoding="utf-8"))
    svg = compose_svg(fg_paths)
    for folder, size in DENSITIES.items():
        out_dir = ROOT / "app/src/main/res" / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            out = out_dir / name
            rsvg_to_png(svg, size, out)
            print(out.relative_to(ROOT), out.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
