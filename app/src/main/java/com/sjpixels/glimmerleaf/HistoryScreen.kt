package com.sjpixels.glimmerleaf

import android.content.Intent
import android.view.Gravity
import android.view.View

/** Every journey ever started, with each day's sessions and notes. Journeys, days and single entries can be deleted. */
class HistoryScreen(act: MainActivity) : Screen(act) {

    private val expandedCycles = HashSet<Long>()
    private val expandedDays = HashSet<String>()
    private var initialised = false

    override fun build(ui: Ui): View {
        val c = ui.c
        val p = ui.p
        val db = Db.get(act)
        val cycles = db.cycles()
        val page = ui.screen()

        page.addView(ui.eyebrow(c.t("tab_history")), ui.lp(t = 8))
        page.addView(ui.h1(c.t("history_title")), ui.lp(t = 4, b = 16))

        if (!initialised) {
            cycles.firstOrNull()?.let { expandedCycles += it.id }
            initialised = true
        }

        if (cycles.isEmpty()) {
            val empty = ui.card(24) {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(ArtView(act, Art.NO_HISTORY, p.accent, 72), ui.lp(ui.dp(72), ui.dp(72), b = 16))
                addView(ui.text(c.t("history_empty"), 16f, p.muted, Fonts.sans, gravity = Gravity.CENTER, lineSp = 24f))
            }
            page.addView(empty, ui.lp())
            return ui.page(page)
        }

        for (cy in cycles) {
            val sessions = db.sessions(cy.id)
            val counts = IntArray(Content.DAYS + 1)
            sessions.forEach { if (it.day in 1..Content.DAYS) counts[it.day]++ }
            val done = (1..Content.DAYS).count { counts[it] > 0 }
            val open = cy.id in expandedCycles

            val card = ui.card(16)
            val head = ui.row {
                addView(ui.col {
                    addView(ui.row {
                        addView(ui.cardTitle(c.t("journey_n", "n" to cy.num)), ui.lp(WRAP, WRAP, r = 8))
                        val (label, bg, fg) = when (cy.status) {
                            Cycle.STATUS_ACTIVE -> Triple(c.t("status_active"), p.accent, p.onAccent)
                            Cycle.STATUS_COMPLETED -> Triple(c.t("status_completed"), p.accentSoft, p.ink)
                            else -> Triple(c.t("status_reset"), p.surfaceAlt, p.muted)
                        }
                        addView(ui.chip(label, bg, fg))
                    })
                    val dates = c.t("started_on", "date" to ui.date(cy.startedAt)) +
                        (cy.endedAt?.let { "  ·  " + c.t("ended_on", "date" to ui.date(it)) } ?: "")
                    addView(ui.muted(dates), ui.lp(t = 4))
                }, ui.lp(0, WRAP, 1f))
                addView(ui.iconButton(Icon.SHARE, c.t("share"), p.ink) { share(ui, cy, sessions) })
                addView(ui.iconButton(Icon.TRASH, c.t("delete"), p.ink) {
                    act.confirm(c.t("delete_journey_q"), c.t("delete"), danger = true) {
                        Journey.deleteCycle(act, cy.id)
                        act.toast(c.t("deleted"))
                        act.refresh()
                    }
                })
                addView(IconView(act, if (open) Icon.CHEVRON_DOWN else Icon.CHEVRON, p.muted, 20), ui.lp(ui.dp(20), ui.dp(20)))
            }
            head.isClickable = true
            head.setOnClickListener {
                if (!expandedCycles.add(cy.id)) expandedCycles.remove(cy.id)
                act.refresh()
            }
            card.addView(head, ui.lp())
            card.addView(ui.progress(done / 28f), ui.lp(MATCH, ui.dp(8), t = 12))
            card.addView(ui.row {
                addView(ui.muted(c.t("done_left", "done" to done, "left" to 28 - done)), ui.lp(0, WRAP, 1f))
                addView(ui.muted(c.t("sessions_n", "n" to sessions.size)))
            }, ui.lp(t = 4))

            if (open) {
                val days = sessions.map { it.day }.distinct().sorted()
                if (days.isEmpty()) card.addView(ui.muted(c.t("no_entries")), ui.lp(t = 12))
                for (day in days) {
                    val key = "${cy.id}:$day"
                    val dayOpen = key in expandedDays
                    val daySessions = sessions.filter { it.day == day }.sortedByDescending { it.completedAt }
                    card.addView(ui.divider(), ui.lp(t = 12))
                    val dayRow = ui.row {
                        minimumHeight = ui.dp(64)
                        setPadding(0, ui.dp(8), 0, ui.dp(4))
                        addView(DayBadge(act, p, day, DayStatus.DONE, 40), ui.lp(WRAP, WRAP, r = 8))
                        addView(ui.col {
                            addView(ui.h3(c.t("day_n", "n" to day) + " · " + c.day(day).title))
                            val notes = daySessions.sumOf { it.noteCount }
                            val notesLabel = if (notes == 1) c.t("notes_count_1") else c.t("notes_count_n", "n" to notes)
                            addView(ui.muted(c.t("sessions_n", "n" to daySessions.size) + " · " + notesLabel))
                        }, ui.lp(0, WRAP, 1f))
                        addView(ui.iconButton(Icon.TRASH, c.t("delete"), p.muted) {
                            act.confirm(c.t("delete_day_q", "n" to day), c.t("delete"), danger = true) {
                                Journey.deleteDay(act, cy.id, day)
                                act.toast(c.t("deleted"))
                                act.refresh()
                            }
                        })
                        addView(IconView(act, if (dayOpen) Icon.CHEVRON_DOWN else Icon.CHEVRON, p.muted, 20), ui.lp(ui.dp(20), ui.dp(20)))
                    }
                    dayRow.isClickable = true
                    dayRow.setOnClickListener {
                        if (!expandedDays.add(key)) expandedDays.remove(key)
                        act.refresh()
                    }
                    card.addView(dayRow, ui.lp())
                    if (dayOpen) {
                        for (s in daySessions) {
                            card.addView(entryCard(ui, s) {
                                act.confirm(c.t("delete_session_q"), c.t("delete"), danger = true) {
                                    Journey.deleteSession(act, s.id)
                                    act.toast(c.t("deleted"))
                                    act.refresh()
                                }
                            }, ui.lp(t = 8))
                        }
                    }
                }
            }
            page.addView(card, ui.lp(b = 12))
        }
        return ui.page(page)
    }

    private fun share(ui: Ui, cy: Cycle, sessions: List<Session>) {
        val c = ui.c
        val sb = StringBuilder()
        sb.append(c.t("app_name")).append(" — ").append(c.t("journey_n", "n" to cy.num)).append('\n')
        sb.append(c.t("started_on", "date" to ui.date(cy.startedAt))).append("\n\n")
        for (s in sessions.sortedWith(compareBy<Session> { it.day }.thenBy { it.completedAt })) {
            sb.append(c.t("day_n", "n" to s.day)).append(" · ").append(c.day(s.day).title)
                .append(" (").append(ui.dateTime(s.completedAt)).append(")\n")
            for (sec in s.notes.sections) {
                val items = sec.items.filter { it.isNotBlank() }
                if (items.isEmpty()) continue
                sb.append("  ").append(sec.prompt).append('\n')
                items.forEach { sb.append("   • ").append(it).append('\n') }
            }
            if (s.notes.reflection.isNotBlank()) sb.append("  ").append(s.notes.reflection).append('\n')
            sb.append('\n')
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, c.t("journey_n", "n" to cy.num))
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        act.startActivity(Intent.createChooser(send, c.t("share")))
    }
}
