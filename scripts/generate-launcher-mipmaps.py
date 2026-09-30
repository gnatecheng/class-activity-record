#!/usr/bin/env python3
"""Regenerate launcher mipmaps (delegates to generate-tuanji-launcher.py)."""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
subprocess.run([sys.executable, str(ROOT / "scripts/generate-tuanji-launcher.py")], check=True)
