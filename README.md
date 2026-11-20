<div align="center">

<img src="store/play_icon_512.png" alt="Gratitude Journey icon" width="120" height="120" />

# Gratitude Journey

**A 28-day gratitude practice for Android that works fully offline.**<br/>
Four weeks of small daily practices that train your heart to notice what is good.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/minSdk-26%20(Android%208.0)-brightgreen)](#-build-it)
[![Target SDK](https://img.shields.io/badge/targetSdk-36%20(Android%2016)-blue)](#-build-it)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Dependencies](https://img.shields.io/badge/dependencies-0-success)](app/build.gradle.kts)
[![Offline](https://img.shields.io/badge/works-100%25%20offline-orange)](#-privacy)
[![License: GPL v3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)

<img src="store/play_feature_1024x500.png" alt="Gratitude Journey feature graphic" width="100%" />

</div>

---

## 📖 Table of contents

- [✨ Features](#-features)
- [🌍 Languages](#-languages)
- [🔒 Privacy](#-privacy)
- [🛠️ Build it](#️-build-it)
- [🚀 Publish to Google Play](#-publish-to-google-play)
- [✏️ Change the practices](#️-change-the-practices)
- [🗣️ Notes on voices](#️-notes-on-voices)
- [📁 Project structure](#-project-structure)
- [✅ Before release](#-before-release)
- [📄 License](#-license)

---

## ✨ Features

| | Feature | Where |
|:-:|---|---|
| 📅 | **28 daily practices** plus morning and evening rituals, in 9 languages | `assets/content/*.json` |
| 🧭 | **Journey engine:** days run in order, future days can be finished early, days can be repeated, the journey archives itself when all 28 are done, and it can be reset | `Journey.kt` |
| ⏰ | **Smart reminders** from 9:00, every 3 hours, until the day's practice is done (all adjustable) | `Reminders.kt` |
| 🔊 | **Read-aloud** with the phone's offline text-to-speech: female or male voice, a specific-voice picker and speed control | `Speaker.kt`, `PracticeScreen.kt` |
| 📝 | **Structured journal** for each practice: numbered prompts, a reflection, mood, and drafts that save automatically | `PracticeScreen.kt` |
| 🗂️ | **History** of every journey, with delete per journey, day or entry, and share as text | `HistoryScreen.kt` |
| 📊 | **Insights:** progress ring, streaks, sessions per practice, a 12-week activity grid and a mood trend | `InsightsScreen.kt` |
| 🎨 | **7 colour themes** with a day/night switch, and confetti when you finish | `Theme.kt`, `Views.kt` |
| 💜 | "Created with Love by sj.pixels" credit with a link | `MainActivity.kt` |

### The four weeks

| Week | Theme |
|:-:|---|
| 1️⃣ | 🌱 Noticing |
| 2️⃣ | 🤝 Connection |
| 3️⃣ | 🏡 Body, Home & World |
| 4️⃣ | 🦋 Transformation |

---

## 🌍 Languages

| | Language | File |
|:-:|---|---|
| 🇬🇧 | English | `en.json` |
| 🇩🇪 | Deutsch | `de.json` |
| 🇪🇸 | Español | `es.json` |
| 🇫🇷 | Français | `fr.json` |
| 🇮🇳 | हिन्दी | `hi.json` |
| 🇯🇵 | 日本語 | `ja.json` |
| 🇰🇷 | 한국어 | `ko.json` |
| 🇷🇺 | Русский | `ru.json` |
| 🇨🇳 | 中文 | `zh.json` |

The interface strings are in `res/values-*/strings.xml`. Any string that's missing falls back to English.

---

## 🔒 Privacy

> 🛡️ **No accounts, no analytics, no ads, no tracking, and no third-party SDKs.**

- 📱 All data (journeys, notes, mood and settings) stays **on the device**, in SQLite and SharedPreferences.
- 🌐 The app never needs an internet connection.
- 🔔 Reminders are scheduled locally.
- 📤 Data leaves the app only when you tap **Share** yourself.

The full policy is in [`PRIVACY.md`](PRIVACY.md).

---

## 🛠️ Build it

**Requirements:** the latest stable [Android Studio](https://developer.android.com/studio) and JDK 17.

| Component | Version |
|---|---|
| Gradle | 8.14.3 |
| Android Gradle Plugin | 8.11.1 |
| Kotlin | 2.2.0 |
| minSdk / targetSdk | 26 / 36 |

1. **File → Open** and select this folder, then let Gradle sync. If Studio offers to upgrade AGP, accept.
2. Connect a phone with USB debugging turned on, or start an emulator.
3. Press **Run ▶**.

Or build from the command line:

```bash
./gradlew assembleDebug        # debug APK
./gradlew installDebug         # install on a connected device
```

---

## 🚀 Publish to Google Play

<details>
<summary><b>Step-by-step guide</b> (click to expand)</summary>

1. **🆔 App ID:** change `applicationId` in `app/build.gradle.kts` if you want to (it's currently `com.sjpixels.gratitude`). It can't be changed after the first upload.
2. **🔑 Sign the bundle:** go to **Build → Generate Signed App Bundle / APK → Android App Bundle**. Create an upload key, keep the `.jks` file and its passwords safe, and build the `release` variant. The output is `app/release/app-release.aab`.

   To do this from the command line instead, create `keystore.properties` in the project root:

   ```properties
   storeFile=upload-key.jks
   storePassword=••••••
   keyAlias=upload
   keyPassword=••••••
   ```

   Then run:

   ```bash
   ./gradlew bundleRelease
   ```

   > ⚠️ `keystore.properties`, `*.jks` and `*.keystore` are in `.gitignore`. Never commit them.

3. **🏪 Play Console:** create the app, upload the `.aab`, and fill in:
   - **Store listing:** `store/play_icon_512.png`, `store/play_feature_1024x500.png`, and 2–8 phone screenshots.
   - **Privacy policy:** required. A ready-to-host text is in `PRIVACY.md`.
   - **Data safety:** the app collects and shares **no data**.
   - **Content rating:** no objectionable content.
   - **Permissions:** `SCHEDULE_EXACT_ALARM` is used only for practice reminders, and the user grants it in Settings. Without it, reminders arrive within a 10-minute window.

</details>

---

## ✏️ Change the practices

Each language is a single JSON file in `app/src/main/assets/content/`. Keep the same structure:

```jsonc
{
  "lang": "en",
  "meta": { "journeyTitle": "…", "journeySubtitle": "…", "weeks": ["…", "…", "…", "…"] },
  "rituals": [ { "id": "morning", "title": "…", "when": "…", "text": "…", "prompt": "…", "count": 5 } ],
  "days": [
    // exactly 28 entries, each with:
    // day, title, theme, intro, steps, journal (prompts with count), affirmation
  ],
  "ui": { /* interface strings */ }
}
```

After editing a file, check that it's still valid JSON:

```bash
for f in app/src/main/assets/content/*.json; do python3 -m json.tool "$f" > /dev/null && echo "✅ $f"; done
```

---

## 🗣️ Notes on voices

- The app uses only voices that are **already installed** on the phone. **Settings → Reading voice → Install or manage voices** opens the system installer. Once a language's voice is downloaded, read-aloud works offline.
- Android doesn't label voices as male or female, so the app recognises common Google and Samsung voices by name. If no voice of the chosen gender is installed, it changes the pitch of the default voice.
- Users can always pick an exact voice from the list, and each one plays a preview.

---

## 📁 Project structure

```
GratitudeJourney/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/content/        # 📚 practices, one JSON per language
│       ├── java/com/sjpixels/gratitude/
│       │   ├── MainActivity.kt    # entry point, navigation, credit
│       │   ├── Journey.kt         # 🧭 journey engine
│       │   ├── Content.kt         # content loading
│       │   ├── Db.kt · Prefs.kt   # 💾 SQLite + settings
│       │   ├── Reminders.kt       # ⏰ notifications
│       │   ├── Speaker.kt         # 🔊 text-to-speech
│       │   ├── *Screen.kt         # 🖼️ Home, Practice, History, Insights, Settings, Contents
│       │   └── Theme.kt · Views.kt · Kit.kt  # 🎨 UI toolkit
│       └── res/                   # icons, strings (9 locales), themes
├── store/                         # 🏪 Play Store graphics
├── PRIVACY.md
└── LICENSE
```

---

## ✅ Before release

- [ ] Replace `CREDIT_URL` in `MainActivity.kt`. It's currently `https://sj.pixels`, which isn't a reachable address because there's no `.pixels` domain.
- [ ] Increase `versionCode` (and `versionName`) in `app/build.gradle.kts` for every upload.
- [ ] Take 2–8 phone screenshots for the store listing.
- [ ] Host `PRIVACY.md` at a public URL for Play Console.

---

## 📄 License

Released under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE).

<div align="center">

<br/>

Made with 💜 by **sj.pixels**

</div>
