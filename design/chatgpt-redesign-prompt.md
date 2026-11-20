You are a senior mobile product designer and brand designer. Redesign the complete UI of an Android app I am publishing on Google Play and build its UI kit and brand assets. I have attached two reference images: the new logo and an example promotional image. Work through the numbered deliverables at the end, in order.

THE APP
- Name: Glimmerleaf. Tagline: "28 days to notice what is good". Marketing headline (from the attached example): "What did you notice today?"
- What it does: a fully offline 28-day gratitude practice called "The 28-Day Gratitude Journey". Each day has one practice: a one-paragraph intro, exactly 4 steps and an affirmation. There is also a morning ritual ("Morning: Five Thanks") and an evening ritual ("Evening: The Golden Moment"). Users can have any practice read aloud by the phone's own offline voice, write in a guided journal and pick a mood. Reminders arrive every 3 hours from 9:00 to 21:00 until the day's practice is done. Users can see their history and analytics. There are no accounts, no internet and no ads.
- Journey rules the design must show clearly: days run in order; after finishing a day, the next one "rests" until the next morning (the user can still open it early); any day can be repeated; after all 28 the journey is archived and a new one can start; the journey can be reset.
- Platform: native Android phones (Android 8 to 16), portrait, edge-to-edge (the app draws behind the status and navigation bars). Design at 360×800dp and check it still works at 320dp wide and with the system font size at 200%.
- Languages: English, Hindi, German, Spanish, Japanese, Russian, French, Chinese and Korean. None are right-to-left. Layouts must handle long German and Russian text and Devanagari, Chinese, Japanese and Korean scripts without clipping. Test with these real titles:
  - Longest English day title: "The People Who Keep Life Running"
  - Longest German day title: "Die Menschen, die das Leben am Laufen halten"
  - A Russian button label that wraps: "Открыть практику"
- Credit: every screen shows "Created with Love by sj.pixels" in small, readable text at the top centre. "sj.pixels" is a link to https://sjpixels.com/.

BRAND: USE THE ATTACHED LOGO
- Logo (attached): a golden open book with a single leaf sprouting from its spine and two small four-point sparkles, on a deep brown rounded square, with a "Glimmerleaf" serif wordmark. Keep this concept and its colours. Refine it, don't replace it: clean up the curves, balance the sparkles and make the leaf readable at 24dp.
- Brand colours sampled from the logo: deep brown #321209, golden page #FEC74E, page edge #EAA114, leaf #FEC13A, cream #FDF4E5. The attached example uses a dark brown button #441E0D, a gold chip #F8CD6D, a parchment screen #F5E2CA and ink #2F140A.
- Visual direction from the attached example: warm morning light, parchment, sepia ink, an elegant high-contrast serif for headings, a clean sans-serif for body text, a dark-brown pill primary button, a soft gold chip for the day label, and small single-colour line illustrations (like the coffee cup) that sit inside the app. The sunlit photography (cup, window, flowers) is for marketing images only, never inside the app.
- Mood: calm, warm and a little magical, like a beautiful personal journal rather than a productivity tool. Use generous spacing, rounded cards, hairline borders or gentle shadows, and tiny scattered sparkles.
- All artwork must be original. Do not imitate any existing book cover, book title, author's branding or company logo. Do not show real people.

HOW THE APP IS BUILT (design within these limits)
- Native Android Views written in Kotlin. There is no Jetpack Compose, no Material Components library and no third-party libraries. Every component is drawn in code. Gradients, glows, sparkles, the progress ring, charts and confetti are drawn on a Canvas, and icons are vector paths. So:
  - Specify every effect numerically (gradient stops and angles, glow radius and opacity, shadow elevation in dp, corner radius in dp, sparkle count and size range), not as "a soft glow".
  - Keep animations simple and specify them: duration in ms, easing and what changes (alpha, scale, translation).
  - No blur-behind/frosted glass, no video and no Lottie files.
- Fonts: the app currently uses the system serif and sans-serif. You may pick ONE free, open-licence serif family from Google Fonts to bundle for headings (name it and the weights). Keep the system sans-serif for body text. Bundled Latin fonts don't cover Devanagari, Chinese, Japanese or Korean, so the app falls back to the system Noto fonts for those. Say how headings should look in those scripts (size and weight adjustments) so the design still holds.
- Give all sizes in dp on a 4dp grid and all text sizes in sp, with line height. Touch targets must be at least 48dp. Text must meet WCAG AA contrast (4.5:1 body, 3:1 large text and icons) in day and night for every theme. List the contrast ratio of ink, muted and on-accent for each theme.
- Icons: simple line icons, 2px stroke, round caps and joins, on a 24dp grid with 2dp padding. The set: home, book, history, chart, gear, play, pause, stop, previous, next, back, chevron-right, chevron-down, bell, sun, moon, heart, sparkle, share, trash, check, lock, speaker. Deliver each as SVG code using only <path> elements with stroke (no fills, masks, filters or text), so I can convert them to Android VectorDrawables.
- In-app illustrations: single-colour line art in the same style as the coffee cup in the example, tinted with a theme colour, delivered as SVG code using stroke paths only. Keep the set small: one per week (Noticing, Connection, Body/Home/World, Transformation), one for each ritual (sunrise, golden evening), and the empty states (no journey yet, no history, no insights). That's 9 illustrations, not 28.
- Mood picker: 5 emoji, 😔 😐 🙂 😊 🤩. Design the selected and unselected states around the emoji; don't replace them with custom faces.

7 COLOUR THEMES (each with a day and a night version)
Keep each hue but refine freely. Golden Parchment is the default and must match the new logo's browns and golds.
1. Golden Parchment (default): day #F4E9D0 / #A26B2A; night #1A120D / #E6B866. Move toward the logo: deep brown #321209 for the primary button and ink, gold #FEC74E for chips and glow.
2. Wax Seal: day #F6EADF / #9B2B2C; night #1B0F10 / #E9857A
3. Amber Glow: day #F8EBD7 / #BF5E17; night #1C1209 / #F4A757
4. Rose Quartz: day #F7EAE7 / #A44B68; night #1B1215 / #E99CB3
5. Twilight Violet: day #F1EBF3 / #6B4A9C; night #15111C / #BBA2EA
6. Sapphire Dusk: day #EBF0F4 / #2D5D8D; night #0E141B / #8FBAEA
7. Emerald Sage: day #EDF1E7 / #3D7250; night #0F160F / #90CC9C
For every theme in both modes, give exactly these tokens (they match the code): bg, bg2 (background gradient end), surface (cards), surfaceAlt (inputs, inner panels), ink (main text), muted (secondary text), accent, onAccent (text on accent), accentSoft (selected pill, soft fills), line (hairline borders), glow (sparkles and highlights) and danger. If the new style needs more (for example a separate "primary button" colour, as in the example, where it's dark brown rather than the accent), add them as clearly named extra tokens and explain where they're used.

GLOBAL LAYOUT
- Top bar, 56dp tall, drawn below the status bar. Left: a back arrow on inner pages, or the small logo mark on the main tabs. Centre: the credit line. Right: a day/night toggle, an animated pill switch (about 56×32dp) whose knob shows a sun or a moon.
- Bottom navigation with 5 tabs: Today, Contents, History, Insights, Settings. Each has an icon and a label, and the selected tab has a soft pill behind the icon. It sits above the system navigation bar and hides while the keyboard is open. Labels must fit "Einstellungen" (German) and "Статистика" (Russian) at 11sp.
- Pages scroll vertically, with 20dp side margins and 28dp bottom padding.

SCREENS (design every state listed; use these real strings)
1. Today
   a. Not started: date line ("Friday, November 20, 2026") and greeting (Good morning / Good afternoon / Good evening). A hero card: eyebrow "THE 28-DAY GRATITUDE JOURNEY", title "Begin your 28-day gratitude journey", body "One short practice a day. Gentle reminders every few hours until it is done. Your notes never leave this phone." Buttons: "Start the journey" (primary) and "Browse the practices" (ghost).
   b. In progress (the hero state in the attached example): eyebrow "DAY 3 OF 28 · NOTICING", title "Small Wins", theme line "Celebrate what went right.", the week's line illustration, a progress bar labelled "2 of 28 complete", and "Open practice" (primary) and "Listen" (secondary) buttons. Below it: a bell icon with "Next reminder today at 12:00 PM"; a "Week 1 · Noticing" strip of 7 day badges (done = filled with a tick, current = glowing ring, upcoming = outline); and "Daily rituals" with two cards, Morning: Five Thanks ("Every morning") and Evening: The Golden Moment ("Every night").
   c. Resting (today's practice is done): "Today's practice is complete", "Day 4 unlocks tomorrow at 9:00 AM", a preview of Day 4's title and a "Begin it now" button.
   d. Journey complete: a celebratory card with "You have completed the whole 28-day journey!" and a "Start a new journey" button.
2. Contents: the title "The 28-Day Gratitude Journey", the subtitle "Four weeks of small daily practices that train your heart to notice what is good.", an overall progress bar ("1 done · 27 left"), a "Daily rituals" card with 2 rows, then Weeks 1 to 4 (Noticing; Connection; Body, Home & World; Transformation) with 7 rows each. Each row has a day badge, the title, a one-line theme and an optional chip: "Today's practice" (accent), "Upcoming" (soft, when the next day is resting) or "×2" for a repeated practice.
3. Practice page (the most important screen):
   - Eyebrow "DAY 3 OF 28 · NOTICING", a large serif title, an italic theme line and a status chip: "✓ Completed ×2", "Today's practice", "Upcoming" or "Day 4 unlocks tomorrow at 9:00 AM".
   - An optional banner for days ahead of schedule: "This practice is ahead of your schedule. You can still do it now; your journey keeps its order."
   - A Listen card: a 56dp round play/pause button, previous / stop / next controls, a status line such as "English · Female", and a "Reading aloud…" state. The paragraph being read gets a soft glow highlight, and the page scrolls to keep it in view.
   - The intro paragraph, then "The practice" with 4 numbered step cards, then "Carry this with you": the affirmation in a glowing quote card with a large opening quote mark.
   - "Your journal" with a small "Draft saved" label. The number of journal sections changes from day to day, so design the pattern rather than one fixed layout: each day has 1 or 2 prompts, each with 1, 3, 5 or 7 numbered single-line inputs or a single multi-line box, and the morning list "This morning's five thanks" (5 numbered lines) is always added at the end. Show Day 3: "Small wins and who or what helped" (5 lines) plus the five thanks. Then a multi-line Reflection box (hint "What did you notice? How do you feel?"), the mood picker ("How do you feel now?") and a full-width "Complete practice" button, which reads "Save another session" if the day is already done.
   - "Your entries for this practice": cards showing date and time, the mood emoji, the notes grouped by prompt, and a delete icon. Also design the empty state ("No entries yet").
4. Ritual page (Morning or Evening): title, when to do it, the ritual text, its line illustration and a Listen card.
5. History: one card per journey, newest first: "Journey 2" with a status chip ("In progress", "Completed" or "Reset"), "Started Nov 20, 2026 · Ended Dec 18, 2026", a progress bar, "12 done · 16 left", "Sessions: 15", and share and delete icons. Show it collapsed and expanded. Expanded shows one row per completed day (badge, "Day 1 · Arrive Where You Are", "Sessions: 1 · 3 notes", delete and expand), and an expanded day shows its entry cards. Include the empty state.
6. Insights:
   - A "This journey · Journey 1" card with an animated progress ring (12 / 28, "43%"), stats (Sessions, Notes written) and a "Sessions per practice" bar chart with 28 thin bars and axis labels 1, 7, 14, 21, 28. The current day's bar is highlighted.
   - "All time" tiles in a 2×2 grid: current streak, best streak ("1 day" / "12 days"), sessions and notes written; plus journeys completed.
   - A 12-week activity grid (like a contribution heatmap) and a "Mood after practice" line chart.
   - An empty state.
7. Settings:
   - Appearance: a theme picker (7 round swatches in rows of 4 and 3, with names; the selected one has a ring) and a "Night mode" switch.
   - Language: a row showing the current language, which opens a picker listing the 9 languages in their own scripts (English, हिन्दी, Deutsch, Español, 日本語, Русский, Français, 中文, 한국어).
   - Reading voice: a Female / Male segmented control, a "Specific voice" row, a reading speed slider (0.50× to 1.50×, value shown), and "Test voice" and "Install or manage voices" buttons.
   - Reminders: an on/off switch, "First reminder" 9:00 AM and "Last reminder" 9:00 PM rows, "Repeat every" 1 h / 2 h / 3 h / 4 h / 6 h (segmented control), an "Exact reminder times" row showing "Not allowed. Tap to allow" or "Allowed", and "Next practice unlocks": "Next morning" / "Right after completing" (segmented control).
   - Journey: an explanation and a "Reset journey" button in the danger style.
   - About: "Glimmerleaf", the version ("Version 1.0.0"), a one-line note and the credit line with a heart icon.
8. Celebration overlay after completing a practice: a dimmed background, a confetti burst (in theme colours), and a card with a big word (one of "Hooray!", "Yippee!", "Congratulations!", "Wonderful!", "Well done!"), "You completed Day 3: Small Wins", the day's affirmation in italics and a "Continue" button. Also design a bigger variant for finishing all 28 days.
9. Dialogs and system UI: confirm delete, reset journey (danger), choose language, choose voice (a list with radio buttons that plays a preview when tapped), the time picker styling, a toast, and the reminder notification (title "Day 3 · Small Wins", text "Your gratitude practice is waiting. A few minutes is all it takes.", using the monochrome notification icon tinted with the accent).

BRAND ASSETS (Google Play's exact requirements)
A. Logo: a vector redraw of the attached logo as SVG code. Provide the symbol alone, a horizontal lockup (symbol left, wordmark right, as in the example) and a stacked lockup (symbol above the wordmark, as in the attached logo), each for light and dark backgrounds. Name the wordmark font, or draw it as paths.
B. App icon (Android adaptive icon): a 108×108dp canvas as two separate layers. The background layer is a full-bleed deep brown (#321209, optionally with a subtle radial glow), with no rounded square drawn in. The foreground layer is the book, leaf and sparkles only, on a transparent background, with all key artwork inside the central 66dp circle so no launcher mask crops it. Also a single-colour monochrome foreground for Android 13+ themed icons. Deliver all three as SVG code on a 108×108 viewBox. Separately, a 512×512 Play Store icon: 32-bit PNG, full square, no rounded corners or drop shadow baked in (Play applies its own mask), under 1 MB.
C. Notification icon: a solid white silhouette of the book and leaf on a transparent background, 24×24dp, legible at that size. SVG code.
D. Feature graphic: exactly 1024×500, JPEG or 24-bit PNG with no transparency. Show the logo, "Glimmerleaf", "28 days to notice what is good" and "Offline · Read aloud · 9 languages". Keep all text and the logo inside the central safe area, at least 64px from every edge, because Play can crop and overlay a play button.
E. Promo posters in the style of the attached example: 1080×1920 portrait (social media and WhatsApp status) and 1080×1080 square.
F. Play Store phone screenshots: 6 images at 1080×1920. Don't use 1080×2400: Play rejects screenshots whose long side is more than twice the short side. Each shows one real screen in a phone frame with a short headline above it: "One gentle practice a day", "Listen in your language", "Write what you're grateful for", "Reminders until it's done", "See your progress", "7 themes, day and night".

RULES FOR GENERATED IMAGES
- Image generators often misspell text. Use the exact strings given here, keep on-image text short, and check the spelling of "Glimmerleaf" in every image.
- Mockups must show only features that exist in this brief. Don't invent extra tabs, sign-in, social sharing feeds, subscriptions or cloud sync. If you think a feature would help, list it separately under "Optional ideas" rather than putting it in the mockups.
- Use one consistent phone frame and the same status bar (time 9:41, wifi, signal, battery) in every mockup.

DELIVER IN THIS ORDER
1. A short design rationale covering mood, typography, colour and how the new logo shapes the UI.
2. Design tokens: colours for all 7 themes in day and night (as a table with contrast ratios, and as JSON using exactly the token names above, colours as #RRGGBB), the type scale (name, font, weight, sp, line height, letter spacing), spacing, corner radii, borders, elevation, glow and gradient recipes, motion (durations and easing), and the icon spec.
3. Component specs, each with size, padding, colours (by token name) and states (default, pressed, selected, disabled, and focused where relevant): buttons (primary, secondary, ghost, danger, with and without icon), cards, the hero card with glow and sparkles, chips, day badges (done, current, upcoming, resting), text inputs (single and multi-line, with number gutter), segmented control, switch, slider, bottom nav, top bar, day/night toggle, progress bar, progress ring, bar chart, heatmap, line chart, mood picker, Listen card (idle, loading, reading, paused), step card, quote card, entry card, dialogs and toasts.
4. Screen-by-screen layout specs for every screen and state above, top to bottom, with dp sizes and token names. Use Golden Parchment in day mode, and add notes for night mode and for long German/Russian text.
5. SVG code: the icon set, the 9 illustrations, the logo lockups, the adaptive icon layers (background, foreground, monochrome) and the notification icon.
6. Images: the logo, the 512px icon, the feature graphic and the posters, then high-fidelity mockups of each screen and state, then the 6 store screenshots. Generate one image at a time and wait for me to say "next" before making the next one.
