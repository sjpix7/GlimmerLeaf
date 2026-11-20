package com.sjpixels.glimmerleaf

import android.view.Gravity
import android.view.View
import android.widget.TextView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Analytics: progress, streaks, repetitions, activity and mood. */
class InsightsScreen(act: MainActivity) : Screen(act) {

    /** What the activity pickers point at. Kept across rebuilds; [level] is null until the user picks. */
    private enum class Level { YEAR, MONTH, WEEK }
    private var level: Level? = null
    private var selWeek: LocalDate? = null   // Monday of the chosen week
    private var selMonth: YearMonth? = null

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
        // Heading row: "Activity" on the left, the small "show dates" switch on the right.
        val activityHead = ui.row {
            addView(ui.h2(c.t("activity_chart")), ui.lp(0, WRAP, 1f, r = 8))
        }
        page.addView(activityHead, ui.lp(t = 28, b = 12))
        // Everything since the app was installed (or the first session, if that is earlier, e.g. after a
        // restore), at least 12 weeks. Cells are sized so 12 weeks fill the card; longer histories scroll
        // sideways and open at the current week.
        val installed = try {
            act.packageManager.getPackageInfo(act.packageName, 0).firstInstallTime
        } catch (e: Exception) { System.currentTimeMillis() }
        val since = listOfNotNull(Journey.localDate(installed), dates.minOrNull()).min()
        val thisMonday = today.with(DayOfWeek.MONDAY)
        val weeks = maxOf(12, ChronoUnit.WEEKS.between(since.with(DayOfWeek.MONDAY), thisMonday).toInt() + 1)
        val start = thisMonday.minusWeeks((weeks - 1).toLong())
        val heat = IntArray(weeks * 7)
        val byDate = dates.groupingBy { it }.eachCount()
        for (i in heat.indices) {
            val day = start.plusDays(i.toLong())
            heat[i] = if (day.isAfter(today) || day.isBefore(since)) -1 else byDate[day] ?: 0
        }
        val gap = ui.dpf(4)
        val inner = act.resources.displayMetrics.widthPixels - ui.dpf(20 * 2 + 16 * 2)
        val cell = ((inner - gap * 11) / 12).coerceIn(ui.dpf(12), ui.dpf(32))
        val activeDays = byDate.size
        val heatmap = HeatmapView(act, p, heat, start, Content.locale(c.lang), cell).apply {
            showDates = act.prefs.activityDates
            contentDescription = c.t("activity_since", "date" to ui.date(Journey.millisOf(since)), "n" to activeDays)
        }
        val scroller = android.widget.HorizontalScrollView(act).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            // A soft fade on the edge with more weeks hints that the grid scrolls.
            isHorizontalFadingEdgeEnabled = true
            setFadingEdgeLength(ui.dp(24))
            addView(heatmap)
        }

        // Year / month / week pickers: each lists only periods since install, and they cascade
        // (a year narrows the months, a month narrows the weeks). Picking scrolls the grid there
        // and outlines the period.
        val loc = Content.locale(c.lang)
        val monthFmt = java.time.format.DateTimeFormatter.ofPattern("LLLL", loc)
        val dayFmt = java.time.format.DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(loc, "MMMd"), loc)
        val dayOnly = java.time.format.DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(loc, "d"), loc)
        // "Nov 16–22", or "Nov 30 – Dec 6" when the week spans two months.
        fun weekLabel(monday: LocalDate): String {
            val sunday = monday.plusDays(6)
            return if (sunday.month == monday.month) dayFmt.format(monday) + "–" + dayOnly.format(sunday)
            else dayFmt.format(monday) + " – " + dayFmt.format(sunday)
        }
        fun weekIndex(monday: LocalDate) = ChronoUnit.WEEKS.between(start, monday).toInt()
        val firstMonth = YearMonth.from(since)
        val lastMonth = YearMonth.from(today)
        fun monthsOf(year: Int) = (1..12).map { YearMonth.of(year, it) }.filter { it >= firstMonth && it <= lastMonth }
        fun weeksOf(m: YearMonth): List<LocalDate> {
            val from = maxOf(m.atDay(1), since).with(DayOfWeek.MONDAY)
            val to = minOf(m.atEndOfMonth(), today)
            return generateSequence(from) { it.plusWeeks(1) }.takeWhile { !it.isAfter(to) }.toList()
        }
        if (selWeek == null) {
            selWeek = thisMonday
            selMonth = lastMonth
        }

        lateinit var yearBtn: TextView
        lateinit var monthBtn: TextView
        lateinit var weekBtn: TextView

        fun applySelection(scroll: Boolean) {
            val wk = selWeek ?: thisMonday
            val m = selMonth ?: lastMonth
            yearBtn.text = m.year.toString()
            monthBtn.text = monthFmt.format(m).replaceFirstChar { it.titlecase(loc) }
            weekBtn.text = weekLabel(wk)
            yearBtn.contentDescription = c.t("pick_year") + ": " + yearBtn.text
            monthBtn.contentDescription = c.t("pick_month") + ": " + monthBtn.text
            weekBtn.contentDescription = c.t("pick_week") + ": " + weekBtn.text
            val range = when (level) {
                Level.YEAR -> {
                    val ms = monthsOf(m.year)
                    weekIndex(weeksOf(ms.first()).first())..weekIndex(weeksOf(ms.last()).last())
                }
                Level.MONTH -> weeksOf(m).let { weekIndex(it.first())..weekIndex(it.last()) }
                Level.WEEK -> weekIndex(wk)..weekIndex(wk)
                null -> null
            }
            heatmap.highlight = range
            if (!scroll) return
            scroller.post {
                if (range == null) { scroller.fullScroll(View.FOCUS_RIGHT); return@post }
                val left = heatmap.weekX(range.first)
                val right = heatmap.weekX(range.last) + heatmap.columnWidth
                val view = scroller.width.toFloat()
                // Centre the period if it fits, otherwise show its start.
                val x = if (right - left <= view) (left + right) / 2f - view / 2f else left - ui.dpf(8)
                scroller.smoothScrollTo(x.toInt().coerceAtLeast(0), 0)
            }
        }

        fun pick(title: String, labels: List<String>, checked: Int, onPick: (Int) -> Unit) {
            val d = act.dialogBuilder()
                .setTitle(title)
                .setSingleChoiceItems(act.choiceAdapter(labels.toTypedArray()), checked) { dlg, which ->
                    dlg.dismiss()
                    onPick(which)
                }
                .setNegativeButton(c.t("cancel"), null)
                .create()
            d.setOnShowListener { act.styleDialog(d) }
            d.show()
        }

        fun dropdown(onClick: () -> Unit): TextView = ui.text("", 14f, p.ink, Fonts.sansMedium, gravity = Gravity.CENTER_VERTICAL, lineSp = 20f).apply {
            minHeight = ui.dp(48)
            maxLines = 2
            setPadding(ui.dp(12), ui.dp(8), ui.dp(8), ui.dp(8))
            background = rounded(p.surface, ui.dpf(24), p.muted, ui.dp(1))
            val chevron = act.getDrawable(R.drawable.ic_chevron_down)?.mutate()?.apply {
                setTint(p.muted)
                setBounds(0, 0, ui.dp(18), ui.dp(18))
            }
            setCompoundDrawablesRelative(null, null, chevron, null)
            compoundDrawablePadding = ui.dp(4)
            ui.clickable(this, ui.dpf(24), onClick)
        }

        yearBtn = dropdown {
            val years = (since.year..today.year).toList()
            pick(c.t("pick_year"), years.map { it.toString() }, years.indexOf(selMonth?.year ?: today.year)) { i ->
                val ms = monthsOf(years[i])
                selMonth = ms.first()
                selWeek = weeksOf(ms.first()).first()
                level = Level.YEAR
                applySelection(true)
            }
        }
        monthBtn = dropdown {
            val ms = monthsOf(selMonth?.year ?: today.year)
            pick(c.t("pick_month"), ms.map { monthFmt.format(it).replaceFirstChar { ch -> ch.titlecase(loc) } }, ms.indexOf(selMonth)) { i ->
                selMonth = ms[i]
                selWeek = weeksOf(ms[i]).first()
                level = Level.MONTH
                applySelection(true)
            }
        }
        weekBtn = dropdown {
            val ws = weeksOf(selMonth ?: lastMonth)
            pick(c.t("pick_week"), ws.map { weekLabel(it) }, ws.indexOf(selWeek)) { i ->
                selWeek = ws[i]
                level = Level.WEEK
                applySelection(true)
            }
        }
        val pickers = if (act.resources.configuration.fontScale > 1.3f) {
            ui.col {
                addView(yearBtn, ui.lp())
                addView(monthBtn, ui.lp(t = 8))
                addView(weekBtn, ui.lp(t = 8))
            }
        } else {
            ui.row(Gravity.TOP) {
                addView(yearBtn, ui.lp(0, MATCH, 0.9f, r = 8))
                addView(monthBtn, ui.lp(0, MATCH, 1.2f, r = 8))
                addView(weekBtn, ui.lp(0, MATCH, 1.5f))
            }
        }
        applySelection(true)

        // Small "show dates" switch: puts the day of the month in every square.
        activityHead.addView(ui.muted(c.t("show_dates")).apply { gravity = Gravity.END }, ui.lp(WRAP, WRAP, r = 4))
        activityHead.addView(KitSwitch(act, p, act.prefs.activityDates, small = true) { on ->
            act.prefs.activityDates = on
            heatmap.showDates = on
        }.apply { contentDescription = c.t("show_dates") })

        page.addView(ui.card(16) {
            addView(pickers, ui.lp(b = 12))
            addView(scroller, ui.lp())
            addView(ui.muted(c.t("activity_since", "date" to ui.date(Journey.millisOf(since)), "n" to activeDays)), ui.lp(t = 12))
        }, ui.lp())

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
