package com.sjpixels.gratitude

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
        page.addView(ui.h1(c.journeyTitle), ui.lp(t = 6))
        page.addView(ui.text(c.journeySubtitle, 15f, p.muted, Fonts.serifItalic, 1.35f), ui.lp(t = 6))
        if (st.cycle != null) {
            page.addView(ui.progress(st.doneCount / 28f), ui.lp(MATCH, ui.dp(8), t = 16))
            page.addView(ui.muted(c.t("done_left", "done" to st.doneCount, "left" to 28 - st.doneCount)), ui.lp(t = 6))
        }

        // rituals
        page.addView(ui.sectionTitle(c.t("rituals_title")))
        val rCard = ui.card(6)
        c.rituals.forEachIndexed { i, r ->
            if (i > 0) rCard.addView(ui.divider(), ui.lp(l = 14, r = 14))
            val row = ui.row {
                setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12))
                val ic = ui.col {
                    gravity = Gravity.CENTER
                    background = oval(p.accentSoft)
                    addView(IconView(act, if (r.id == "morning") Icon.SUN else Icon.MOON, p.accent, 20), ui.lp(ui.dp(20), ui.dp(20)))
                }
                addView(ic, ui.lp(ui.dp(42), ui.dp(42), r = 14))
                addView(ui.col {
                    addView(ui.h3(r.title))
                    addView(ui.muted(r.whenText, 13f), ui.lp(t = 2))
                }, ui.lp(0, WRAP, 1f))
                addView(IconView(act, Icon.CHEVRON, p.muted, 18), ui.lp(ui.dp(18), ui.dp(18)))
            }
            ui.clickable(row, ui.dpf(16)) { act.openRitual(r.id) }
            rCard.addView(row, ui.lp())
        }
        page.addView(rCard, ui.lp())

        // weeks
        for (w in 0 until 4) {
            page.addView(ui.eyebrow(c.t("week_n", "n" to w + 1)), ui.lp(t = 28))
            page.addView(ui.h2(c.weeks.getOrElse(w) { "" }), ui.lp(t = 4, b = 12))
            val card = ui.card(6)
            for (k in 1..7) {
                val day = w * 7 + k
                val d = c.day(day)
                val status = st.statusOf(day)
                if (k > 1) card.addView(ui.divider(), ui.lp(l = 68, r = 14))
                val row = ui.row {
                    setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12))
                    addView(DayBadge(act, p, day, status, 42), ui.lp(WRAP, WRAP, r = 14))
                    addView(ui.col {
                        addView(ui.text(d.title, 16f, p.ink, Fonts.serifBold, 1.2f))
                        addView(ui.muted(d.theme, 13f), ui.lp(t = 2))
                    }, ui.lp(0, WRAP, 1f))
                    val n = st.counts[day]
                    when {
                        status == DayStatus.CURRENT -> addView(ui.chip(c.t("status_current"), p.accent, p.onAccent), ui.lp(WRAP, WRAP, l = 6))
                        n > 1 -> addView(ui.chip("×$n"), ui.lp(WRAP, WRAP, l = 6))
                        else -> addView(IconView(act, Icon.CHEVRON, p.muted, 18), ui.lp(ui.dp(18), ui.dp(18), l = 6))
                    }
                }
                ui.clickable(row, ui.dpf(16)) { act.openPractice(day) }
                card.addView(row, ui.lp())
            }
            page.addView(card, ui.lp())
        }
        return ui.page(page)
    }
}
