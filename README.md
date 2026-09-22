# AI Note Manager

A fully native Android note-taking app with an optional, user-configurable AI layer.

- **Bilingual (فارسی / English)** - language picker on first launch; Persian runs RTL, English runs LTR; switchable later in Settings.

- **Kotlin + Jetpack Compose + Material 3**
- Offline-first local database (**Room**)
- Attachments (SAF / system file picker), tags, folders, favorites
- Rich-ish Markdown editor with a formatting toolbar
- **Bring-your-own AI**: any OpenAI-compatible `/v1/chat/completions` endpoint
- AI actions: Organize, Summarize, Expand, Rewrite, Extract Tasks, Generate Title, Translate, Ask AI
- Export: TXT, Markdown, HTML, PDF, DOCX, JSON
- Modular cloud storage + sync architecture (WorkManager), conflict-aware metadata
- Encrypted API-key storage (Android Keystore / EncryptedSharedPreferences)
- Light / dark theme + accent colors, onboarding, empty & loading states

AI is **optional** — the app is a normal offline note app until you configure a provider.

---

## 1. What it does

Write notes, paste messy text, and let your own AI server organize it into headings,
lists and tasks — with a preview and a compare view before anything is replaced.
Everything local works without internet.

## 2. Open in Android Studio

1. `File -> Open` and select this folder (`AI-Note-Manager/`).
2. Let Gradle sync (JDK 17).
3. Run on a device/emulator with Android 8.0 (API 26)+.

## 3. Build locally

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

> Release builds are unsigned-configurable; add your own keystore in
> `app/build.gradle.kts` (`signingConfigs`) for a store-ready APK.
> If `gradlew` is not executable: `chmod +x gradlew`.

## 4. Build with GitHub Actions (no Android Studio needed)

Push to GitHub — `.github/workflows/build-apk.yml` runs automatically on every push,
or trigger it manually from the **Actions** tab (`workflow_dispatch`).

Workflow: checkout -> JDK 17 -> Gradle 8.7 -> `./gradlew assembleRelease` ->
upload artifacts.

## 5. Where the APK is

- CI: Actions run -> *Build APK* run -> **app-release-apk** artifact
- Local: `app/build/outputs/apk/release/app-release.apk`

## 6. Configure the AI API

`Settings -> AI Providers -> Add Provider`:

| Field    | Example                                   |
|----------|-------------------------------------------|
| Name     | My VPS AI                                 |
| API URL  | `https://gw.example.com/v1`               |
| API Key  | your key (stored encrypted on device)     |
| Model    | any model name your gateway serves        |

The app calls `<API URL>/chat/completions` (OpenAI-compatible). Works with OpenRouter,
local servers, self-hosted gateways, custom VPS proxies.

## 7. Custom OpenAI-compatible API

Any endpoint that accepts:

```
POST {baseUrl}/chat/completions
Authorization: Bearer <key>
{"model": "...", "messages": [...]}
```

## 8. Cloud storage

`Settings -> Cloud storage -> Custom storage server`: server URL + API key.
The `CloudStorage` interface is provider-agnostic — implement it for Drive, Dropbox,
WebDAV or S3 later. Sync state per note: `Synced / Pending upload / Uploading /
Downloaded / Conflict / Error`; conflicts are never silently overwritten.

## 9. Release build

```bash
./gradlew assembleRelease
```

## 10. Customize

- Colors/typography: `app/src/main/java/com/ainotes/app/ui/theme/Theme.kt`
- AI prompts: `ai/PromptManager.kt` or in-app under `Settings -> AI Instructions`
- Default screens: `ui/navigation/AppNav.kt`

## Tests

```bash
./gradlew test
```

## Security

No API keys, tokens or credentials live in this repository — only placeholders.
User keys are encrypted with the Android Keystore and excluded from logs.
