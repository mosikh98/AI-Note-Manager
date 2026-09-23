# AI Note Manager

A native Android app (Kotlin + Jetpack Compose, Material 3) for taking notes that can be
automatically reorganized by an AI model of your choice — paste messy text, tap
**"✨ Organize with AI"**, and get back a structured note (title, headings, bullet/numbered
lists, checklists) while preserving the original meaning. AI is fully optional: the app is a
complete, offline-capable note-taking app on its own.

## Features

- **Notes** — create, edit, delete, favorite. Rich visual rendering (headings, bold/italic,
  bullet/numbered lists, checkable checklists) — never raw Markdown symbols on screen.
- **AI Organize** — sends note text to your configured AI endpoint, shows an Original vs.
  AI Result preview, and lets you Apply or Cancel before anything changes.
- **Attachments** — attach images or any file type via the system picker; shows name, type
  icon, size; open or remove.
- **Export** — TXT, Markdown, HTML, PDF, DOCX, JSON, shared via the Android share sheet.
- **Fully configurable AI provider** — works with any OpenAI-compatible
  `/v1/chat/completions` endpoint: OpenAI, OpenRouter, a self-hosted/local model, or a
  custom gateway. Save multiple provider configs and switch the active one. API keys are
  stored in `EncryptedSharedPreferences` (Android Keystore-backed) — never hard-coded,
  never logged.
- **Cloud backup** — connect a custom server URL + API key and upload/back up notes and
  attachments. Uploads only happen when you explicitly tap "Backup to cloud" on a note —
  never silently.
- Material 3, dark/light themes, empty/loading/error states, confirmation dialogs.

## Project structure

```
app/src/main/java/com/ainote/manager/
  data/       Room entities, DAOs, database, encrypted-prefs secret storage, repository
  ai/         AiClient — talks to any OpenAI-compatible chat/completions endpoint
  export/     TXT / Markdown / HTML / PDF / DOCX / JSON exporters
  cloud/      CloudUploader + WorkManager upload worker
  ui/         Compose screens, navigation, theme, reusable components
```

## Building in Android Studio

1. Open this folder in Android Studio (Koala/2024.1 or newer recommended).
2. Let Gradle sync — it will download all dependencies from Google/Maven Central.
3. Run on a device/emulator (min SDK 26 / Android 8.0), or **Build > Generate Signed Bundle / APK**.

## Building from the command line

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK
```

### About the Gradle wrapper jar

This repo includes `gradlew` / `gradlew.bat` and `gradle/wrapper/gradle-wrapper.properties`,
but **not** the binary `gradle-wrapper.jar` (binaries aren't produced by the tool that
generated this project). Before running `./gradlew` for the first time, generate it once
with a local Gradle install:

```bash
gradle wrapper --gradle-version 8.7
```

After that, `./gradlew` works normally and the generated jar can be committed to your repo
so this step isn't needed again. The GitHub Actions workflow below does this automatically
on every run, so CI builds work out of the box with no local setup.

## Building via GitHub Actions

Push this repo to GitHub and the workflow at `.github/workflows/build-apk.yml` runs
automatically: it sets up JDK 17 and the Android SDK, regenerates the Gradle wrapper,
builds `assembleRelease`, and uploads `app-release.apk` as a workflow artifact (Actions tab
→ the run → Artifacts). You can also trigger it manually from the Actions tab
(`workflow_dispatch`).

The workflow signs the release build with a throwaway debug-style keystore generated at
build time, so the APK installs on a device without extra setup. **For a real production
release**, replace the "Create debug keystore" step with your own signing keystore stored in
GitHub Secrets (`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
`RELEASE_KEY_PASSWORD`), decoded to `app/release.keystore`, and pointed to by the same env
vars already read in `app/build.gradle.kts`.

## Configuring the AI provider (in-app)

Settings ⚙️ → **AI Provider Settings** → **+**:

- **Provider name** — a label, e.g. "OpenAI" or "My local server"
- **API Base URL** — e.g. `https://api.openai.com/v1`, `https://openrouter.ai/api/v1`, or
  your own server's base URL. The app appends `/chat/completions` if it isn't already there.
- **Model name** — e.g. `gpt-4o-mini`, or whatever your endpoint expects.
- **API Key** — stored encrypted on-device.
- **Organization ID** (optional), **custom headers** (optional JSON) for gateways that need
  extra auth headers.

You can save several configs and tap **Use** to switch the active one at any time.

## Configuring cloud backup (in-app)

☁️ icon on the notes list → **+** → enter a **Server URL** and **API Key**. The app POSTs a
`multipart/form-data` request (a `metadata` JSON part with title/content, plus one part per
attachment file) to `<server URL>/notes` — point this at any backend you control that accepts
that shape. Tap **Use** to make a saved destination active, then use the cloud icon on a note
to back it up.

## No secrets in source

No API keys or tokens are hard-coded anywhere in this repository. All secrets are entered by
the user at runtime and stored only in `EncryptedSharedPreferences`.

## Out of scope (per spec v1)

Camera capture, AI vision/image analysis, semantic search, and two-way cloud sync with
conflict resolution are not implemented — cloud backup is one-way upload only.
