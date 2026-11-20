package com.sjpixels.glimmerleaf

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
        page.addView(ui.muted(ui.longToday()), ui.lp(t = 4))
        page.addView(ui.h1(greet), ui.lp(t = 4, b = 20))

        // ---------------------------------------------------------------- hero card
        val hero = ui.hero(3)
        page.addView(hero, ui.lp())
        // Paired buttons stack when the system font is very large.
        val stackButtons = act.resources.configuration.fontScale > 1.3f
        fun art(a: Art) = ArtView(act, a, p.accent, 72)

        val last = st.lastCycle
        when {
            st.cycle == null -> {
                if (last != null && last.status == Cycle.STATUS_COMPLETED) {
                    hero.addView(IconView(act, Icon.HEART, p.accent, 32), ui.lp(ui.dp(32), ui.dp(32), b = 12))
                    hero.addView(ui.h2(c.t("journey_complete_title")))
                    hero.addView(ui.body(c.t("journey_complete_body")), ui.lp(t = 8, b = 20))
                    hero.addView(ui.button(c.t("start_again"), icon = Icon.SPARK) { act.startJourney(); act.refresh() }, ui.lp())
                } else {
                    hero.addView(ui.row { addView(ui.chip(c.journeyTitle)) })
                    hero.addView(ui.h2(c.t("start_title")), ui.lp(t = 12))
                    hero.addView(ui.body(c.t("start_body")), ui.lp(t = 8))
                    hero.addView(art(Art.NO_JOURNEY), ui.lp(ui.dp(72), ui.dp(72), t = 16, b = 16))
                    hero.addView(ui.button(c.t("start_button"), icon = Icon.SPARK) {
                        act.startJourney()
                        act.refresh()
                    }, ui.lp())
                    hero.addView(ui.button(c.t("explore_button"), Btn.GHOST) { act.selectTab(1) }, ui.lp(t = 8))
                }
            }
            st.current == null -> {
                // All done (only momentarily, before the journey is archived)
                hero.addView(ui.h2(c.t("journey_complete_title")))
            }
            st.isResting() -> {
                val next = st.current
                hero.addView(ui.row {
                    val tick = ui.col {
                        gravity = Gravity.CENTER
                        background = oval(p.accent)
                        addView(IconView(act, Icon.CHECK, p.onAccent, 20), ui.lp(ui.dp(20), ui.dp(20)))
                    }
                    addView(tick, ui.lp(ui.dp(32), ui.dp(32), r = 12))
                    addView(ui.cardTitle(c.t("today_done_title")), ui.lp(0, WRAP, 1f))
                })
                hero.addView(ui.muted(c.t("next_unlocks", "n" to next, "time" to ui.relative(st.unlockAt))), ui.lp(t = 16))
                hero.addView(ui.h2(c.day(next).title), ui.lp(t = 4))
                hero.addView(ui.themeLine(c.day(next).theme), ui.lp(t = 4))
                hero.addView(ui.progress(st.doneCount / 28f), ui.lp(MATCH, ui.dp(8), t = 16))
                hero.addView(ui.muted(c.t("progress_of_28", "n" to st.doneCount)), ui.lp(t = 4, b = 16))
                hero.addView(ui.button(c.t("begin_now"), Btn.SECONDARY) { act.openPractice(next) }, ui.lp())
            }
            else -> {
                val day = st.current
                val d = c.day(day)
                hero.addView(ui.row { addView(ui.chip(c.t("day_n_of_28", "n" to day) + " · " + c.weekTitle(day))) })
                hero.addView(ui.h2(d.title), ui.lp(t = 12))
                hero.addView(ui.themeLine(d.theme), ui.lp(t = 4))
                hero.addView(art(Art.forDay(day)), ui.lp(ui.dp(72), ui.dp(72), t = 12))
                hero.addView(ui.progress(st.doneCount / 28f), ui.lp(MATCH, ui.dp(8), t = 12))
                hero.addView(ui.muted(c.t("progress_of_28", "n" to st.doneCount)), ui.lp(t = 4, b = 16))
                val open = ui.button(c.t("open_practice"), icon = Icon.BOOK) { act.openPractice(day) }
                val listen = ui.button(c.t("listen"), Btn.SECONDARY, Icon.SPEAKER) {
                    act.push(PracticeScreen(act, day, autoplay = true))
                }
                if (stackButtons) {
                    hero.addView(open, ui.lp())
                    hero.addView(listen, ui.lp(t = 8))
                } else {
                    hero.addView(ui.row(Gravity.TOP) {
                        addView(open, ui.lp(0, MATCH, 1.4f, r = 8))
                        addView(listen, ui.lp(0, MATCH, 1f))
                    }, ui.lp())
                }
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
                minimumHeight = ui.dp(48)
                addView(IconView(act, Icon.BELL, p.ink, 24), ui.lp(ui.dp(24), ui.dp(24), r = 12))
                addView(ui.muted(label), ui.lp(0, WRAP, 1f))
            }, ui.lp(t = 16, l = 4))
        }

        // ---------------------------------------------------------------- this week strip
        if (st.cycle != null) {
            val focus = st.current ?: 28
            val week = (focus - 1) / 7
            page.addView(ui.sectionTitle(c.t("week_n", "n" to week + 1) + " · " + c.weeks.getOrElse(week) { "" }, 20))
            // Seven 48dp badge targets. The row borrows the page margins so all seven fit at 360dp.
            val row = ui.row(Gravity.CENTER)
            val resting = st.isResting()
            for (i in 1..7) {
                val day = week * 7 + i
                val status = st.statusOf(day)
                val cell = ui.col {
                    gravity = Gravity.CENTER
                    addView(DayBadge(act, p, day, status, 48, resting && status == DayStatus.CURRENT))
                    contentDescription = c.t("day_n", "n" to day) + " · " + c.day(day).title
                }
                ui.clickable(cell, ui.dpf(24)) { act.openPractice(day) }
                row.addView(cell, ui.lp(0, ui.dp(48), 1f))
            }
            page.addView(row, ui.lp(l = -12, r = -12))
        }

        // ---------------------------------------------------------------- rituals
        page.addView(ui.sectionTitle(c.t("rituals_title")))
        page.addView(ui.muted(c.t("rituals_hint")), ui.lp(b = 12, t = -4))
        val rituals = ui.row(Gravity.TOP)
        c.rituals.forEachIndexed { i, r ->
            val card = ui.card(16) {
                addView(IconView(act, if (r.id == "morning") Icon.SUN else Icon.MOON, p.accent, 24), ui.lp(ui.dp(24), ui.dp(24), b = 12))
                addView(ui.h3(r.title))
                addView(ui.muted(r.whenText), ui.lp(t = 4))
            }
            ui.clickable(card) { act.openRitual(r.id) }
            ui.pressScale(card)
            rituals.addView(card, ui.lp(0, MATCH, 1f, l = if (i > 0) 4 else 0, r = if (i == 0) 4 else 0))
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
        page.addView(ui.eyebrow(r.whenText), ui.lp(t = 8))
        page.addView(ui.h1(r.title), ui.lp(t = 4))
        page.addView(ArtView(act, if (r.id == "morning") Art.SUNRISE else Art.GOLDEN_EVENING, p.accent, 72),
            ui.lp(ui.dp(72), ui.dp(72), t = 12, b = 16))

        val chunks = listOf(r.title, r.text)
        val textView = ui.body(r.text).apply { setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8)) }
        val titleProxy = View(act)
        val rc = ReaderController(act, ui, chunks, listOf(titleProxy, textView))
        reader = rc
        page.addView(rc.buildCard(), ui.lp(b = 16))
        val card = ui.card(8) { addView(textView) }
        page.addView(card, ui.lp())
        return ui.page(page)
    }

    override fun onLeave() {
        reader?.detach(stop = true)
    }
}
