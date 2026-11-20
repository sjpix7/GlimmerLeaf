package com.sjpixels.glimmerleaf

import android.view.Gravity
import android.view.View
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Analytics: progress, streaks, repetitions, activity and mood. */
class InsightsScreen(act: MainActivity) : Screen(act) {

    /** The activity grid keeps its own picker state across rebuilds. */
    private val activity = ActivitySection(act)

    override fun build(ui: Ui): View {
        val c = ui.c
        val p = ui.p
        val db = Db.get(act)
        val all = db.allSessions()
        val cycles = db.cycles()
        val focus = cycles.firstOrNull { it.isActive } ?: cycles.firstOrNull()
        val page = ui.screen()

        page.addView(ui.eyebrow(c.t("tab_insights")), ui.lp(t = 8))
        page.addView(ui.h1(c.t("insights_title")), ui.lp(t = 4, b = 16))

        if (all.isEmpty() && focus == null) {
            val empty = ui.card(24) {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(ArtView(act, Art.NO_INSIGHTS, p.accent, 72), ui.lp(ui.dp(72), ui.dp(72), b = 16))
                addView(ui.text(c.t("no_data"), 16f, p.muted, Fonts.sans, gravity = Gravity.CENTER, lineSp = 24f))
            }
            page.addView(empty, ui.lp())
            return ui.page(page)
        }

        // dates & streaks
        val dates = all.map { Journey.localDate(it.completedAt) }
        val dateSet = dates.toHashSet()
        val today = LocalDate.now()
        var cur = 0
        var d = if (today in dateSet) today else today.minusDays(1)
        while (d in dateSet) { cur++; d = d.minusDays(1) }
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (x in dateSet.sorted()) {
            run = if (prev != null && ChronoUnit.DAYS.between(prev, x) == 1L) run + 1 else 1
            best = maxOf(best, run)
            prev = x
        }

        // ---------------------------------------------------------- this journey
        if (focus != null) {
            val sessions = all.filter { it.cycleId == focus.id }
            val counts = IntArray(Content.DAYS)
            sessions.forEach { if (it.day in 1..Content.DAYS) counts[it.day - 1]++ }
            val done = counts.count { it > 0 }
            val current = if (focus.isActive) (1..Content.DAYS).firstOrNull { counts[it - 1] == 0 } else null

            page.addView(ui.sectionTitle(c.t("this_journey") + " · " + c.t("journey_n", "n" to focus.num), 4))
            val card = ui.card(16)
            val ring = RingView(act, p, 104).apply {
                center = "$done/28"
                sub = "${(done * 100) / 28}%"
                contentDescription = c.t("done_left", "done" to done, "left" to 28 - done)
            }
            ring.setFraction(done / 28f)
            val stats = ui.col {
                addView(ui.h3(c.t("done_left", "done" to done, "left" to 28 - done)))
                addView(statLine(ui, c.t("total_sessions"), sessions.size.toString()), ui.lp(t = 12))
                addView(statLine(ui, c.t("notes_written"), sessions.sumOf { it.noteCount }.toString()), ui.lp(t = 8))
            }
            // At large font sizes the ring and stats stack.
            if (act.resources.configuration.fontScale > 1.3f) {
                card.addView(ring, ui.lp(WRAP, WRAP))
                card.addView(stats, ui.lp(t = 16))
            } else {
                card.addView(ui.row {
                    addView(ring, ui.lp(WRAP, WRAP, r = 16))
                    addView(stats, ui.lp(0, WRAP, 1f))
                }, ui.lp())
            }
            card.addView(ui.h3(c.t("repetitions_chart")), ui.lp(t = 20, b = 8))
            card.addView(BarChartView(act, p, counts, current), ui.lp())
            page.addView(card, ui.lp())
        }

        // ---------------------------------------------------------- all time tiles
        page.addView(ui.sectionTitle(c.t("all_time")))
        val tiles = listOf(
            Triple(Icon.SPARK, c.t("current_streak"), "$cur ${c.t(if (cur == 1) "day_short" else "days_short")}"),
            Triple(Icon.HEART, c.t("best_streak"), "$best ${c.t(if (best == 1) "day_short" else "days_short")}"),
            Triple(Icon.CHECK, c.t("total_sessions"), all.size.toString()),
            Triple(Icon.BOOK, c.t("notes_written"), all.sumOf { it.noteCount }.toString()),
        )
        val oneColumn = act.resources.configuration.fontScale > 1.3f
        for (r in 0 until 2) {
            val row = ui.row(Gravity.TOP)
            for (k in 0 until 2) {
                val (ic, label, value) = tiles[r * 2 + k]
                val t = ui.card(16) {
                    addView(IconView(act, ic, p.accent, 24), ui.lp(ui.dp(24), ui.dp(24), b = 12))
                    addView(ui.h2(value))
                    addView(ui.muted(label), ui.lp(t = 4))
                }
                if (oneColumn) page.addView(t, ui.lp(b = 8))
                else row.addView(t, ui.lp(0, MATCH, 1f, l = if (k == 1) 4 else 0, r = if (k == 0) 4 else 0))
            }
            if (!oneColumn) page.addView(row, ui.lp(b = 8))
        }
        val completed = cycles.count { it.status == Cycle.STATUS_COMPLETED }
        page.addView(ui.card(16) {
            addView(ui.row {
                addView(IconView(act, Icon.HEART, p.accent, 24), ui.lp(ui.dp(24), ui.dp(24), r = 12))
                addView(ui.h3(c.t("journeys_completed")), ui.lp(0, WRAP, 1f))
                addView(ui.h2(completed.toString()))
            })
        }, ui.lp())

        // ---------------------------------------------------------- activity heatmap
        activity.build(ui, page, dates)

        // ---------------------------------------------------------- mood
        val moods = all.filter { it.mood in 1..5 }.takeLast(20).map { it.mood }
        if (moods.isNotEmpty()) {
            page.addView(ui.sectionTitle(c.t("mood_chart")))
            page.addView(ui.card(16) { addView(LineChartView(act, p, moods), ui.lp()) }, ui.lp())
        }
        return ui.page(page)
    }

    private fun statLine(ui: Ui, label: String, value: String): View = ui.row {
        addView(ui.muted(label), ui.lp(0, WRAP, 1f))
        addView(ui.text(value, 16f, ui.p.ink, Fonts.sansMedium, lineSp = 24f))
    }
}
