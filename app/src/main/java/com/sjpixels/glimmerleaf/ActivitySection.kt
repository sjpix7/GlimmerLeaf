package com.sjpixels.glimmerleaf

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * The activity grid with its heading, "Show dates" switch, and year / month / week pickers.
 * Used on Insights and in History (Journeys). Each screen owns one instance, so the chosen
 * period survives rebuilds of that screen.
 */
class ActivitySection(private val act: MainActivity) {
    private enum class Level { YEAR, MONTH, WEEK }
    private var level: Level? = null
    private var selWeek: LocalDate? = null   // Monday of the chosen week
    private var selMonth: YearMonth? = null

    /** Adds the section to [page]. [dates] holds one entry per completed session (local dates). */
    fun build(ui: Ui, page: LinearLayout, dates: List<LocalDate>, top: Int = 28) {
        val c = ui.c
        val p = ui.p
        val today = LocalDate.now()
        // Heading row: "Activity" on the left, the small "show dates" switch on the right.
        val activityHead = ui.row {
            addView(ui.h2(c.t("activity_chart")), ui.lp(0, WRAP, 1f, r = 8))
        }
        page.addView(activityHead, ui.lp(t = top, b = 12))
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

    }
}
