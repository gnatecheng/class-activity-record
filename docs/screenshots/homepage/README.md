# Homepage screenshots (Roborazzi)

Marketing-style homepage captures are generated on the JVM with **Robolectric + Roborazzi**, using the real Compose screens and the same demo seed as the app (`DemoDataSeeder`).

## Regenerate

From the repo root (requires Python 3 + Pillow, and `rsvg-convert` only for launcher icon regen):

```bash
./gradlew :app:exportHomepageScreenshots
```

This runs `recordRoborazziDebug`, writes PNGs under `app/build/homepage-screenshots-raw/`, then exports **540px-wide WebP** files here:

- `zh/light/`, `zh/dark/`, `en/light/`, `en/dark/` — `01-home.webp` … `05-members.webp`

Clock is fixed to **2026-09-28** in `HomepageScreenshotTest`. Device qualifier: **360×780 dp @ xxhdpi** (portrait phone).

## Tests only (no WebP export)

```bash
./gradlew :app:recordRoborazziDebug
```
