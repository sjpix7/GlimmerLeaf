package com.sjpixels.glimmerleaf

import android.view.Gravity
import android.view.View

/** Table of contents: daily rituals and all 28 practices, grouped by week. Tap any one to open it. */
class ContentsScreen(act: MainActivity) : Screen(act) {

    override fun build(ui: Ui): View {
        val c = ui.c
        val p = ui.p
        val st = Journey.state(act)
        val page = ui.screen()

        page.addView(ui.eyebrow(c.t("tab_contents")), ui.lp(t = 8))
        page.addView(ui.h1(c.journeyTitle), ui.lp(t = 4))
        page.addView(ui.themeLine(c.journeySubtitle), ui.lp(t = 8))
        if (st.cycle != null) {
            page.addView(ui.progress(st.doneCount / 28f), ui.lp(MATCH, ui.dp(8), t = 16))
            page.addView(ui.muted(c.t("done_left", "done" to st.doneCount, "left" to 28 - st.doneCount)), ui.lp(t = 4))
        }

        // rituals
        page.addView(ui.sectionTitle(c.t("rituals_title")))
        val rCard = ui.card(4)
        c.rituals.forEachIndexed { i, r ->
            if (i > 0) rCard.addView(ui.divider(), ui.lp(l = 12, r = 12))
            val row = ui.row {
                minimumHeight = ui.dp(64)
                setPadding(ui.dp(12), ui.dp(8), ui.dp(8), ui.dp(8))
                val ic = ui.col {
                    gravity = Gravity.CENTER
                    background = oval(p.accentSoft)
                    addView(IconView(act, if (r.id == "morning") Icon.SUN else Icon.MOON, p.ink, 20), ui.lp(ui.dp(20), ui.dp(20)))
                }
                addView(ic, ui.lp(ui.dp(32), ui.dp(32), r = 12))
                addView(ui.col {
                    addView(ui.h3(r.title))
                    addView(ui.muted(r.whenText))
                }, ui.lp(0, WRAP, 1f))
                addView(IconView(act, Icon.CHEVRON, p.muted, 20), ui.lp(ui.dp(20), ui.dp(20), l = 8))
            }
            ui.clickable(row, ui.dpf(12)) { act.openRitual(r.id) }
            rCard.addView(row, ui.lp())
        }
        page.addView(rCard, ui.lp())

        // weeks
        val resting = st.isResting()
        for (w in 0 until 4) {
            page.addView(ui.eyebrow(c.t("week_n", "n" to w + 1)), ui.lp(t = 28))
            page.addView(ui.h2(c.weeks.getOrElse(w) { "" }), ui.lp(t = 4, b = 12))
            val card = ui.card(4)
            for (k in 1..7) {
                val day = w * 7 + k
                val d = c.day(day)
                val status = st.statusOf(day)
                val restingDay = status == DayStatus.CURRENT && resting
                if (k > 1) card.addView(ui.divider(), ui.lp(l = 60, r = 12))
                val row = ui.row {
                    minimumHeight = ui.dp(64)
                    setPadding(ui.dp(4), ui.dp(8), ui.dp(8), ui.dp(8))
                    addView(DayBadge(act, p, day, status, 48, restingDay), ui.lp(WRAP, WRAP, r = 8))
                    addView(ui.col {
                        addView(ui.h3(d.title))
                        addView(ui.muted(d.theme))
                        // Chips sit under the title so long titles never get squeezed.
                        val n = st.counts[day]
                        val chip = when {
                            // While resting until tomorrow, the next practice isn't "today's" yet.
                            restingDay -> ui.chip(c.t("status_upcoming"), icon = Icon.LOCK)
                            status == DayStatus.CURRENT -> ui.chip(c.t("status_current"), p.accent, p.onAccent, Icon.BOOK)
                            n > 1 -> ui.chip("×$n")
                            else -> null
                        }
                        if (chip != null) addView(ui.row { addView(chip) }, ui.lp(t = 4))
                    }, ui.lp(0, WRAP, 1f))
                    addView(IconView(act, Icon.CHEVRON, p.muted, 20), ui.lp(ui.dp(20), ui.dp(20), l = 8))
                    contentDescription = c.t("day_n", "n" to day) + " · " + d.title
                }
                ui.clickable(row, ui.dpf(12)) { act.openPractice(day) }
                card.addView(row, ui.lp())
            }
            page.addView(card, ui.lp())
        }
        return ui.page(page)
    }
}
