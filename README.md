# Gratitude Journey: native Android app

A fully offline, 28-day gratitude practice app for Google Play. It's written in Kotlin and uses only the Android platform, with no third-party libraries.

Created with Love by sj.pixels

## What's inside

| Feature | Where |
|---|---|
| 28 daily practices + morning/evening rituals, in 9 languages | `app/src/main/assets/content/*.json` |
| Journey engine: sequential order, early completion of future days, repeats, auto-archive when all 28 are done, reset | `Journey.kt` |
| Reminders from 9:00, every 3 h, until the day's practice is done (all adjustable in Settings) | `Reminders.kt` |
| Read-aloud with the phone's own offline text-to-speech, female/male voice, specific-voice picker, speed | `Speaker.kt`, `PracticeScreen.kt` |
| Structured journal per practice (numbered prompts), reflection, mood, auto-saved drafts | `PracticeScreen.kt` |
| History of every journey, with delete per journey / day / entry, and share as text | `HistoryScreen.kt` |
| Analytics: progress ring, streaks, sessions per practice, 12-week activity grid, mood trend | `InsightsScreen.kt` |
| 7 colour themes + day/night switch (top right), celebration with confetti on completion | `Theme.kt`, `Views.kt`, `MainActivity.kt` |
| "Created with Love by sj.pixels" credit, top centre, with link | `MainActivity.kt` (`CREDIT_URL`) |

All data (journeys, notes, settings) is stored only on the device in SQLite and SharedPreferences.

## Build it

1. Install **Android Studio** (latest stable).
2. **File → Open** and select this folder. Let Gradle sync. It downloads Gradle 8.14.3, the Android Gradle Plugin 8.11.1 and Kotlin 2.2.0. If Studio offers to upgrade AGP, accept.
3. Plug in a phone (with USB debugging on) or start an emulator, then press **Run ▶**.

Requires Android 8.0+ (minSdk 26), targets Android 16 (API 36).

## Publish to Google Play

1. **Change the app ID** if you like: `applicationId` in `app/build.gradle.kts` (currently `com.sjpixels.gratitude`). It can never change after the first upload.
2. **Build → Generate Signed App Bundle / APK → Android App Bundle**. Create an upload key (keep the `.jks` file and passwords safe) and build the `release` variant. You get `app/release/app-release.aab`.
   - Command line alternative: create `keystore.properties` in the project root with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`, then run `./gradlew bundleRelease`.
3. In **Play Console**, create the app, upload the `.aab`, and fill in:
   - **Store listing:** use `store/play_icon_512.png` and `store/play_feature_1024x500.png`. Add 2–8 phone screenshots taken from the running app.
   - **Privacy policy:** required. A ready-to-host text is in `PRIVACY.md`.
   - **Data safety:** the app collects and shares **no data**.
   - **Content rating** questionnaire: no objectionable content.
   - **Permissions:** `SCHEDULE_EXACT_ALARM` is used only for user-facing practice reminders. The user grants it in Settings; without it, reminders arrive within a 10-minute window.

## Change the practices

Each language lives in one JSON file. Keep the same structure (28 `days`, each with `title`, `theme`, `intro`, `steps`, `journal` prompts with `count`, and `affirmation`). After editing, check that the files are still valid JSON. Missing interface strings fall back to English.

## Things to set before release

- `CREDIT_URL` in `MainActivity.kt` is `https://sj.pixels`, as requested. That isn't a reachable web address (there is no `.pixels` domain), so replace it with your real site or profile URL.
- `versionCode` / `versionName` in `app/build.gradle.kts`: increase `versionCode` for every upload.

## Notes on voices

The app uses only voices already installed on the phone. **Settings → Reading voice → Install or manage voices** opens the system TTS installer. Once a language's voice is downloaded, reading works with no internet. Android doesn't label voices as male or female, so the app recognises common Google and Samsung voices by name. If no voice of the chosen gender is installed, it shifts the default voice's pitch. Users can always pick an exact voice from the list (each plays a preview).
