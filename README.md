**English** | [中文](README.zh-CN.md)

[Homepage: Etai Apps — Group Matters section](https://etais.dev/#group-matters)

# Group Matters

Android app for tracking group activities—attendance, payments, expense splits, and checklists—for any small group (class, club, dorm, team, etc.). Data stays on device (Room); no account or network required.

- **App name:** 团团记 (English UI: **Group Matters**)
- **applicationId:** `com.classrecord.app`
- **Minimum OS:** Android 8.0 (API 26)
- **Version:** 1.5.2 (versionCode 9)
- **UI languages:** Simplified Chinese / English (switch in Settings)

## Features

- One group profile: editable group name
- Add, edit, and soft-archive members (optional member ID and notes); bulk paste import
- Subgroups within the group: multi-select members; a member can belong to several subgroups; empty subgroups cannot be activity scopes
- Four activity types: attendance / payment / split / checklist
- Activity scope: whole group or a subgroup; member list is snapshotted at creation—later subgroup edits do not change history
- Amounts stored in fen (cents); UI shows yuan with ¥ in English
- Split remainder assigned to the first N members
- Activity detail defaults to “unfinished only”; tap to update status; edit amounts for payment/split
- Copy summary / reminder text (shareable); “open new period” re-snapshots the current roster
- Group ledger: income/expense entries and balance; payment activities can post collected totals to the ledger
- Backup / restore (zip, includes payment receipt images); export members, activity progress, and ledger CSV
- Split: exclude members, weight or fixed amounts; payment/split notes and photo attachments
- Optional sort by member ID; attendance supports continuous roll call
- Home screen widget: unfinished counts for recent in-progress activities (with combined total when several); tap opens the app
- Home search by title; filter active / archived; archive or unarchive from detail menu
- Dark theme: Material 3, follow system or fixed light/dark in Settings (DataStore)
- Reminder text formatted for WeChat-style group messages (one line per person with amounts); copy/share unfinished lists and CSV from detail
- Bulk import: member ID first, tab-separated paste; hints for duplicate names/IDs; restore archived members while keeping history
- Quick title templates for roll call, group fund, shared expense split, etc. when creating activities or opening a new period
- **Settings → About:** version, build time, GitHub repository link

## Changelog

### 1.5.2

- Chinese app name **团团记** (renamed from 多人事务 to 团团记; English name **Group Matters** unchanged)
- Unified **group fund** wording (was “class fund” in English UI)
- New launcher icon featuring **团**; refreshed homepage screenshot sets
- Neutral **group / member** wording throughout (no class- or school-specific UI copy); demo data uses a generic club-style group

### 1.5.1

- Full English copy rewrite (natural group/member activity wording)
- Fix in-app language switching on Android 13/14 (AppCompat app locales + `localeConfig`)
- Demo group loads in the current UI language (empty database only)
- English UI shows amounts with ¥ (no bare “元”)

### 1.5.0

- Renamed to 多人事务 / **Group Matters**
- Settings: Simplified Chinese / English
- Dark theme: system or fixed light/dark
- Settings **About** page (version, last update, GitHub)

## Local development

Requires JDK 17+ and the Android SDK. Create `local.properties` at the repo root:

```
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
./gradlew assembleDebug
```

Debug APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Install

The debug build is signed with the debug key and can be sideloaded. Allow “unknown sources” / “install unknown apps” on the device.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or transfer the APK to the phone and open it. The home screen widget appears as **Unfinished count** (or localized equivalent) in the widget picker.

## GitHub releases

Push a **semver tag** `vX.Y.Z` that **exactly matches** `versionName` in `app/build.gradle.kts` (e.g. tag `v1.5.1` when `versionName = "1.5.1"`). The **Release APK** workflow builds a **signed, non-debuggable** release APK and publishes:

`group-matters-<versionName>.apk` (e.g. `group-matters-1.5.1.apk`).

Downloads: [Releases](https://github.com/gnatecheng/group-matters/releases/latest).

Manual **workflow_dispatch** only verifies the signed release build; it does **not** create a Release. Missing signing secrets fail the job (no debug APK is ever published).

### Release signing & GitHub Secrets

GitHub Actions [`.github/workflows/release-apk.yml`](.github/workflows/release-apk.yml) runs `assembleRelease` with the repo’s release keystore. If required secrets are missing, the workflow **fails with a clear error**—it will not ship an unsigned or debug APK.

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the release keystore file |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | (Optional) key password; defaults to store password |

Generate a keystore locally (never commit it):

```bash
keytool -genkey -v -keystore release.keystore -alias group-matters \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.keystore   # paste into ANDROID_KEYSTORE_BASE64
```

Local **debug** builds (`assembleDebug`) work without these variables. To build a signed release locally, set `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and optionally `ANDROID_KEY_PASSWORD` (see `app/build.gradle.kts`).

### Upgrading from debug installs to signed releases

Release APKs are signed with the **release** key. They **cannot install over** an existing **debug-signed** build with the same `applicationId` (`com.classrecord.app`). To migrate:

1. **Back up** in the app: **Settings → backup to zip**.
2. Install the new **signed release** APK (uninstall the debug build first if Android blocks the update).
3. **Restore** from the zip in Settings.

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · DataStore · App Widget · Flow · Coroutines · MVVM

## License

MIT — see [LICENSE](LICENSE).
