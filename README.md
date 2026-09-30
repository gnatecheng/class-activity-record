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

When you push a tag (`v*`) or run the **Release APK** workflow manually, release assets are named:

`group-matters-<versionName>-<tag>.apk` (e.g. `group-matters-1.5.0-v1.5.0.apk`).

Downloads: [Releases](https://github.com/gnatecheng/group-matters/releases/latest) (`releases/latest`; no fixed filename required).

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · DataStore · App Widget · Flow · Coroutines · MVVM
