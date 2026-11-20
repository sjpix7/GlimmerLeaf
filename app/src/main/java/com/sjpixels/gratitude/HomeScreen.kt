package com.sjpixels.gratitude

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import java.time.LocalTime

class HomeScreen(act: MainActivity) : Screen(act) {

    override fun build(ui: Ui): View {
        val c = ui.c
        val p = ui.p
        val st = Journey.state(act)
        val page = ui.screen()

        val hour = LocalTime.now().hour
        val greet = when {
            hour < 12 -> c.t("greet_morning")
            hour < 17 -> c.t("greet_afternoon")
            else -> c.t("greet_evening")
        }
        page.addView(ui.muted(ui.longToday(), 13f), ui.lp(t = 6))
        page.addView(ui.h1(greet), ui.lp(t = 2, b = 18))

        // ---------------------------------------------------------------- hero card
        val hero = ui.col(22, 22).apply {
            background = GlowDrawable(p, ui.dpf(26), act.resources.displayMetrics.density, 3)
        }
        page.addView(hero, ui.lp())

        val last = st.lastCycle
        when {
            st.cycle == null -> {
                if (last != null && last.status == Cycle.STATUS_COMPLETED) {
                    hero.addView(IconView(act, Icon.HEART, p.accent, 30), ui.lp(ui.dp(30), ui.dp(30), b = 10))
                    hero.addView(ui.h2(c.t("journey_complete_title")))
                    hero.addView(ui.body(c.t("journey_complete_body")), ui.lp(t = 8, b = 18))
                    hero.addView(ui.button(c.t("start_again"), icon = Icon.SPARK) { act.startJourney(); act.refresh() }, ui.lp())
                } else {
                    hero.addView(ui.eyebrow(c.journeyTitle))
                    hero.addView(ui.text(c.t("start_title"), 25f, p.ink, Fonts.serifBold, 1.15f), ui.lp(t = 8))
                    hero.addView(ui.body(c.t("start_body")), ui.lp(t = 10, b = 20))
                    hero.addView(ui.button(c.t("start_button"), icon = Icon.SPARK) {
                        act.startJourney()
                        act.refresh()
                    }, ui.lp())
                    hero.addView(ui.button(c.t("explore_button"), Btn.GHOST) { act.selectTab(1) }, ui.lp(t = 10))
                }
            }
            st.current == null -> {
                // All done (only momentarily, before the journey is archived)
                hero.addView(ui.h2(c.t("journey_complete_title")))
            }
            st.isResting() -> {
                val next = st.current
                hero.addView(ui.row {
                    addView(IconView(act, Icon.CHECK, p.accent, 20), ui.lp(ui.dp(20), ui.dp(20), r = 8))
                    addView(ui.eyebrow(c.t("today_done_title")))
                })
                hero.addView(ui.text(c.t("next_unlocks", "n" to next, "time" to ui.relative(st.unlockAt)), 15f, p.muted), ui.lp(t = 12))
                hero.addView(ui.text(c.day(next).title, 24f, p.ink, Fonts.serifBold, 1.15f), ui.lp(t = 4))
                hero.addView(ui.text(c.day(next).theme, 15f, p.muted, Fonts.serifItalic), ui.lp(t = 4, b = 14))
                hero.addView(ui.progress(st.doneCount / 28f), ui.lp(MATCH, ui.dp(8)))
                hero.addView(ui.muted(c.t("progress_of_28", "n" to st.doneCount)), ui.lp(t = 6, b = 16))
                hero.addView(ui.button(c.t("begin_now"), Btn.SECONDARY) { act.openPractice(next) }, ui.lp())
            }
            else -> {
                val day = st.current
                val d = c.day(day)
                hero.addView(ui.eyebrow(c.t("day_n_of_28", "n" to day) + " · " + c.weekTitle(day)))
                hero.addView(ui.text(d.title, 27f, p.ink, Fonts.serifBold, 1.12f), ui.lp(t = 8))
                hero.addView(ui.text(d.theme, 15.5f, p.muted, Fonts.serifItalic, 1.3f), ui.lp(t = 4, b = 16))
                hero.addView(ui.progress(st.doneCount / 28f), ui.lp(MATCH, ui.dp(8)))
                hero.addView(ui.muted(c.t("progress_of_28", "n" to st.doneCount)), ui.lp(t = 6, b = 18))
                hero.addView(ui.row {
                    addView(ui.button(c.t("open_practice"), icon = Icon.BOOK) { act.openPractice(day) }, ui.lp(0, WRAP, 1f, r = 10))
                    addView(ui.button(c.t("listen"), Btn.SECONDARY, Icon.SPEAKER) {
                        act.push(PracticeScreen(act, day, autoplay = true))
                    }, ui.lp(WRAP, WRAP))
                }, ui.lp())
            }
        }

        // ---------------------------------------------------------------- reminder line
        if (st.cycle != null && st.current != null) {
            val next = Reminders.nextTime(act)
            val label = when {
                !act.prefs.remindersOn -> c.t("reminders_off")
                next != null -> c.t("next_reminder", "time" to ui.relative(next))
                else -> c.t("reminders_off")
            }
            page.addView(ui.row {
                addView(IconView(act, Icon.BELL, p.muted, 16), ui.lp(ui.dp(16), ui.dp(16), r = 8))
                addView(ui.muted(label, 13f))
            }, ui.lp(t = 14, l = 6))
        }

        // ---------------------------------------------------------------- this week strip
        if (st.cycle != null) {
            val focus = st.current ?: 28
            val week = (focus - 1) / 7
            page.addView(ui.sectionTitle(c.t("week_n", "n" to week + 1) + " · " + c.weeks.getOrElse(week) { "" }))
            val strip = ui.card(14)
            val row = ui.row(Gravity.CENTER)
            for (i in 1..7) {
                val day = week * 7 + i
                val cell = ui.col {
                    gravity = Gravity.CENTER_HORIZONTAL
                    addView(DayBadge(act, p, day, st.statusOf(day), 38))
                }
                ui.clickable(cell, ui.dpf(14)) { act.openPractice(day) }
                row.addView(cell, ui.lp(0, WRAP, 1f))
            }
            strip.addView(row, ui.lp())
            page.addView(strip, ui.lp())
        }

        // ---------------------------------------------------------------- rituals
        page.addView(ui.sectionTitle(c.t("rituals_title")))
        page.addView(ui.muted(c.t("rituals_hint")), ui.lp(b = 12, t = -6))
        val rituals = ui.row(Gravity.TOP)
        c.rituals.forEachIndexed { i, r ->
            val card = ui.card(16) {
                addView(IconView(act, if (r.id == "morning") Icon.SUN else Icon.MOON, p.accent, 22), ui.lp(ui.dp(22), ui.dp(22), b = 10))
                addView(ui.h3(r.title))
                addView(ui.muted(r.whenText, 12.5f), ui.lp(t = 4))
            }
            ui.clickable(card) { act.openRitual(r.id) }
            rituals.addView(card, ui.lp(0, MATCH, 1f, l = if (i > 0) 6 else 0, r = if (i == 0) 6 else 0))
        }
        page.addView(rituals, ui.lp())

        return ui.page(page)
    }
}

/** Reads a daily ritual aloud. */
class RitualScreen(act: MainActivity, private val id: String) : Screen(act) {
    private var reader: ReaderController? = null

    override val refreshOnResume: Boolean = false

    override fun build(ui: Ui): View {
        val c = ui.c
        val p = ui.p
        val r = c.ritual(id) ?: c.rituals.first()
        val page = ui.screen()
        page.addView(ui.eyebrow(c.t("rituals_title")), ui.lp(t = 8))
        page.addView(ui.h1(r.title), ui.lp(t = 6))
        page.addView(ui.text(r.whenText, 15f, p.muted, Fonts.serifItalic), ui.lp(t = 4, b = 18))

        val chunks = listOf(r.title, r.text)
        val textView = ui.body(r.text).apply { setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4)) }
        val titleProxy = View(act)
        val rc = ReaderController(act, ui, chunks, listOf(titleProxy, textView))
        reader = rc
        page.addView(rc.buildCard(), ui.lp(b = 20))
        val card = ui.card(20) { addView(textView) }
        page.addView(card, ui.lp())
        return ui.page(page)
    }

    override fun onLeave() {
        reader?.detach(stop = true)
    }
}
