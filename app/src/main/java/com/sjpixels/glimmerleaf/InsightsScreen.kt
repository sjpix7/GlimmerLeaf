package com.sjpixels.glimmerleaf

import android.view.Gravity
import android.view.View
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Analytics: progress, streaks, repetitions, activity and mood. */
class InsightsScreen(act: MainActivity) : Screen(act) {

    override fun build(ui: Ui): View {
        val c = ui.c
        val p = ui.p
        val db = Db.get(act)
        val all = db.allSessions()
        val cycles = db.cycles()
        val focus = cycles.firstOrNull { it.isActive } ?: cycles.firstOrNull()
        val page = ui.screen()

        page.addView(ui.eyebrow(c.t("tab_insights")), ui.lp(t = 8))
        page.addView(ui.h1(c.t("insights_title")), ui.lp(t = 6, b = 16))

        if (all.isEmpty() && focus == null) {
            val empty = ui.card(24) {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(IconView(act, Icon.CHART, p.accent, 36), ui.lp(ui.dp(36), ui.dp(36), b = 12))
                addView(ui.text(c.t("no_data"), 15f, p.muted, Fonts.sans, 1.4f, Gravity.CENTER))
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
            val card = ui.card(18)
            val top = ui.row {
                val ring = RingView(act, p, 128).apply {
                    center = "$done"
                    sub = "/ 28"
                }
                addView(ring, ui.lp(WRAP, WRAP, r = 18))
                ring.setFraction(done / 28f)
                addView(ui.col {
                    addView(ui.text(c.t("done_left", "done" to done, "left" to 28 - done), 16f, p.ink, Fonts.sansMedium))
                    addView(ui.muted("${(done * 100) / 28}%", 13f), ui.lp(t = 4))
                    addView(statLine(ui, c.t("total_sessions"), sessions.size.toString()), ui.lp(t = 14))
                    addView(statLine(ui, c.t("notes_written"), sessions.sumOf { it.noteCount }.toString()), ui.lp(t = 6))
                }, ui.lp(0, WRAP, 1f))
            }
            card.addView(top, ui.lp())
            card.addView(ui.text(c.t("repetitions_chart"), 14f, p.ink, Fonts.sansMedium), ui.lp(t = 20, b = 8))
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
        for (r in 0 until 2) {
            val row = ui.row(Gravity.TOP)
            for (k in 0 until 2) {
                val (ic, label, value) = tiles[r * 2 + k]
                val t = ui.card(16) {
                    addView(IconView(act, ic, p.accent, 20), ui.lp(ui.dp(20), ui.dp(20), b = 10))
                    addView(ui.text(value, 24f, p.ink, Fonts.serifBold, 1.1f))
                    addView(ui.muted(label, 12.5f), ui.lp(t = 2))
                }
                row.addView(t, ui.lp(0, MATCH, 1f, l = if (k == 1) 6 else 0, r = if (k == 0) 6 else 0))
            }
            page.addView(row, ui.lp(b = 12))
        }
        val completed = cycles.count { it.status == Cycle.STATUS_COMPLETED }
        page.addView(ui.card(16) {
            addView(ui.row {
                addView(IconView(act, Icon.HEART, p.accent, 20), ui.lp(ui.dp(20), ui.dp(20), r = 12))
                addView(ui.text(c.t("journeys_completed"), 15f, p.ink, Fonts.sansMedium), ui.lp(0, WRAP, 1f))
                addView(ui.text(completed.toString(), 22f, p.ink, Fonts.serifBold))
            })
        }, ui.lp())

        // ---------------------------------------------------------- activity heatmap
        page.addView(ui.sectionTitle(c.t("activity_chart")))
        val weeks = 12
        val start = today.with(DayOfWeek.MONDAY).minusWeeks((weeks - 1).toLong())
        val heat = IntArray(weeks * 7)
        val byDate = dates.groupingBy { it }.eachCount()
        for (i in heat.indices) {
            val day = start.plusDays(i.toLong())
            heat[i] = if (day.isAfter(today)) -1 else byDate[day] ?: 0
        }
        page.addView(ui.card(16) { addView(HeatmapView(act, p, heat), ui.lp()) }, ui.lp())

        // ---------------------------------------------------------- mood
        val moods = all.filter { it.mood in 1..5 }.takeLast(20).map { it.mood }
        if (moods.isNotEmpty()) {
            page.addView(ui.sectionTitle(c.t("mood_chart")))
            page.addView(ui.card(16) { addView(LineChartView(act, p, moods), ui.lp()) }, ui.lp())
        }
        return ui.page(page)
    }

    private fun statLine(ui: Ui, label: String, value: String): View = ui.row {
        addView(ui.muted(label, 13f), ui.lp(0, WRAP, 1f))
        addView(ui.text(value, 15f, ui.p.ink, Fonts.sansBold))
    }
}
