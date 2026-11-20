# Publishing Glimmerleaf on Google Play (organisation account)

Everything Play asks for, in the order the Console asks for it. Copy the text blocks as they are.

## What you need

| Item | Where it is |
| --- | --- |
| App bundle (v1.0.0, versionCode 1) | `release/glimmerleaf-1.0.0-1.aab` (rebuild with `./gradlew bundleRelease`) |
| Upload key | `~/AndroidKeys/glimmerleaf-upload.jks`, alias `upload`, password in `keystore.properties` |
| App icon 512 × 512 | `store/play_icon_512.png` |
| Feature graphic 1024 × 500 | `store/play_feature_1024x500.png` |
| Phone screenshots 1080 × 1920 (7) | `store/screenshots/` |
| Privacy policy URL | `https://sjpix7.github.io/GlimmerLeaf/privacy.html` (after GitHub Pages is on) |

> **Back up the upload key now.** Copy `glimmerleaf-upload.jks` and the password from `keystore.properties` into your password manager. Every future update must be signed with this key. If it is lost, Google can reset it, but only after a support request.

Organisation accounts don't have the "12 testers for 14 days" closed-test rule that applies to new personal accounts, so you can publish straight to production.

---

## 1. Turn on the privacy policy page

1. Make sure the GitHub repository `sjpix7/GlimmerLeaf` is **public** (GitHub Pages on a private repo needs a paid plan).
2. GitHub → repository → **Settings → Pages** → Source: **Deploy from a branch**, Branch: **main**, folder **/docs** → Save.
3. After a minute, open `https://sjpix7.github.io/GlimmerLeaf/privacy.html` and check that it loads.

## 2. Create the app

Play Console → **Create app**:

- App name: `Glimmerleaf: Gratitude Journal`
- Default language: English (United States) – en-US
- App or game: **App**
- Free or paid: **Free** (this can't be changed to paid later)
- Tick the two declarations (Developer Program Policies, US export laws) → **Create app**

## 3. App content (Policy → App content)

| Form | Answer |
| --- | --- |
| Privacy policy | `https://sjpix7.github.io/GlimmerLeaf/privacy.html` |
| App access | **All functionality is available without any access restrictions** |
| Ads | **No, my app does not contain ads** |
| Content rating | Start the questionnaire, category **All other app types**. Answer **No** to violence, sexuality, language, controlled substances, gambling, user-to-user interaction, sharing location and digital purchases. Expected result: Everyone / PEGI 3. |
| Target audience | Ages **13–15, 16–17 and 18+** (don't tick under 13, which would bring in the Families policy). "Could the app unintentionally appeal to children?" **No** |
| News app | **No** |
| Data safety | See below |
| Advertising ID | **No**, the app doesn't use an advertising ID |
| Government app | **No** |
| Financial features | **My app doesn't provide any financial features** |
| Health | Read the definitions. Glimmerleaf is a gratitude journal with an optional mood emoji; it gives no medical, therapy or diagnosis features. If the form requires a choice, pick the option closest to general wellness/journaling, or "no health features" if none fits. |

### Data safety

- Does your app collect or share any of the required user data types? **No**
  (Everything stays in the app's storage on the device; Android's own device backup doesn't count as collection by the developer.)
- Is all user data encrypted in transit? Not applicable (nothing is transmitted). If the form still asks, nothing leaves the device.
- Account deletion: not applicable (there are no accounts).
- Preview the summary: it should read **"No data collected · No data shared"** → Submit.

### Permissions you may be asked about

- `SCHEDULE_EXACT_ALARM`: optional, lets practice reminders arrive on time; the user grants it in Settings. (No declaration is needed for this one; only `USE_EXACT_ALARM` needs one, and the app doesn't use it.)
- `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`: daily reminders, restored after a restart.

## 4. Store settings (Grow → Store presence → Store settings)

- Category: **Lifestyle** (Health & Fitness also fits, but brings more health-policy questions)
- Tags: Journal, Self-care, Wellness (pick what Play offers)
- Contact email: an address you check (shown publicly)
- Website: `https://sjpixels.com/`

## 5. Main store listing (Grow → Store presence → Main store listing)

**App name** (30 max)

```
Glimmerleaf: Gratitude Journal
```

**Short description** (80 max)

```
28 days of small gratitude practices, a daily journal and gentle reminders.
```

**Full description** (4000 max)

```
What did you notice today?

Glimmerleaf is a calm, beautiful gratitude journal built around The 28-Day Gratitude Journey: one short practice a day that trains your heart to notice what is good.

THE 28-DAY JOURNEY
• Four weeks: Noticing, Connection, Body, Home & World, and Transformation
• Each day has a short introduction, four simple steps and an affirmation to carry with you
• Days unlock in order, so the practice builds gently; repeat any day you love
• A morning ritual (Five Thanks) and an evening ritual (The Golden Moment)

WRITE IT DOWN
• A guided journal for every practice, with as many lines as you need
• Daily gratitude: list today's small wins and what you're grateful for
• Gratitude journal: write freely whenever your heart is full, as often as you like, and edit or delete entries later
• Pick a mood after each practice

LISTEN
• Have any practice read aloud by your phone's own offline voice
• Choose a female or male voice and the reading speed

GENTLE REMINDERS
• Reminders every few hours until the day's practice is done
• Choose the first and last reminder time and how often they come

SEE YOUR PROGRESS
• History of every journey, day, daily list and journal entry
• Insights: streaks, sessions, a mood chart and an activity calendar that goes back to the day you installed the app

MAKE IT YOURS
• 7 colour themes, each with a day and a night mode
• 9 languages: English, हिन्दी, Deutsch, Español, 日本語, Русский, Français, 中文, 한국어

PRIVATE BY DESIGN
• Works fully offline
• No accounts, no ads, no tracking
• Your notes stay on your phone and in your own device backup

Created with love by sj.pixels.
```

**Graphics**

- App icon: `store/play_icon_512.png`
- Feature graphic: `store/play_feature_1024x500.png`
- Phone screenshots: upload all 7 from `store/screenshots/` in order (01 to 07)
- Tablet screenshots: optional; skip them for now

## 6. Upload the release

Recommended: send it to **Internal testing** first (Testing → Internal testing). It usually goes live within minutes and lets you install it from the Play Store on your own phone. Then promote the same release to Production.

1. **Testing → Internal testing → Create new release**
2. App signing: accept **Google Play App Signing** (default). Google keeps the app signing key; you sign uploads with your upload key.
3. Upload `release/glimmerleaf-1.0.0-1.aab`
4. Release name: `1.0.0 (1)`
5. Release notes:

   ```
   <en-US>
   First release of Glimmerleaf: the 28-day gratitude journey, daily gratitude, a free-writing journal, read-aloud, reminders, insights, 7 themes and 9 languages.
   </en-US>
   ```

6. **Next → Save → Testers**: create an email list with your own Google account, copy the opt-in link, open it on your phone and install.
7. When it looks right: **Production → Create new release → Add from library** (pick the same bundle) → Countries/regions: **Add all** → **Review release → Start rollout to production**.
8. Go to **Publishing overview** → **Send for review**. The first review for a new app usually takes a few days.

## 7. Future updates

1. Increase `versionCode` (and `versionName`) in `app/build.gradle.kts`.
2. `./gradlew bundleRelease` (uses `keystore.properties` automatically).
3. Upload `app/build/outputs/bundle/release/app-release.aab` as a new release.
