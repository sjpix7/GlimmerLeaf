package com.sjpixels.gratitude

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
        page.addView(ui.h1(c.t("history_title")), ui.lp(t = 6, b = 16))

        if (!initialised) {
            cycles.firstOrNull()?.let { expandedCycles += it.id }
            initialised = true
        }

        if (cycles.isEmpty()) {
            val empty = ui.card(24) {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(IconView(act, Icon.BOOK, p.accent, 36), ui.lp(ui.dp(36), ui.dp(36), b = 12))
                addView(ui.text(c.t("history_empty"), 15f, p.muted, Fonts.sans, 1.4f, Gravity.CENTER))
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
                        addView(ui.text(c.t("journey_n", "n" to cy.num), 19f, p.ink, Fonts.serifBold), ui.lp(WRAP, WRAP, r = 10))
                        val (label, bg, fg) = when (cy.status) {
                            Cycle.STATUS_ACTIVE -> Triple(c.t("status_active"), p.accent, p.onAccent)
                            Cycle.STATUS_COMPLETED -> Triple(c.t("status_completed"), p.accentSoft, p.ink)
                            else -> Triple(c.t("status_reset"), withAlpha(p.ink, 20), p.muted)
                        }
                        addView(ui.chip(label, bg, fg))
                    })
                    val dates = c.t("started_on", "date" to ui.date(cy.startedAt)) +
                        (cy.endedAt?.let { "  ·  " + c.t("ended_on", "date" to ui.date(it)) } ?: "")
                    addView(ui.muted(dates, 12.5f), ui.lp(t = 4))
                }, ui.lp(0, WRAP, 1f))
                addView(ui.iconButton(Icon.SHARE, c.t("share"), p.muted, 40, 19) { share(ui, cy, sessions) })
                addView(ui.iconButton(Icon.TRASH, c.t("delete"), p.muted, 40, 19) {
                    act.confirm(c.t("delete_journey_q"), c.t("delete"), danger = true) {
                        Journey.deleteCycle(act, cy.id)
                        act.toast(c.t("deleted"))
                        act.refresh()
                    }
                })
                addView(IconView(act, if (open) Icon.CHEVRON_DOWN else Icon.CHEVRON, p.muted, 18), ui.lp(ui.dp(18), ui.dp(18), l = 2))
            }
            head.isClickable = true
            head.setOnClickListener {
                if (!expandedCycles.add(cy.id)) expandedCycles.remove(cy.id)
                act.refresh()
            }
            card.addView(head, ui.lp())
            card.addView(ui.progress(done / 28f, 6), ui.lp(MATCH, ui.dp(6), t = 12))
            card.addView(ui.row {
                addView(ui.muted(c.t("done_left", "done" to done, "left" to 28 - done), 12.5f), ui.lp(0, WRAP, 1f))
                addView(ui.muted(c.t("sessions_n", "n" to sessions.size), 12.5f))
            }, ui.lp(t = 6))

            if (open) {
                val days = sessions.map { it.day }.distinct().sorted()
                if (days.isEmpty()) card.addView(ui.muted(c.t("no_entries")), ui.lp(t = 12))
                for (day in days) {
                    val key = "${cy.id}:$day"
                    val dayOpen = key in expandedDays
                    val daySessions = sessions.filter { it.day == day }.sortedByDescending { it.completedAt }
                    card.addView(ui.divider(), ui.lp(t = 12))
                    val dayRow = ui.row {
                        setPadding(0, ui.dp(10), 0, ui.dp(4))
                        addView(DayBadge(act, p, day, DayStatus.DONE, 32), ui.lp(WRAP, WRAP, r = 12))
                        addView(ui.col {
                            addView(ui.text(c.t("day_n", "n" to day) + " · " + c.day(day).title, 15f, p.ink, Fonts.sansMedium))
                            val notes = daySessions.sumOf { it.noteCount }
                            val notesLabel = if (notes == 1) c.t("notes_count_1") else c.t("notes_count_n", "n" to notes)
                            addView(ui.muted(c.t("sessions_n", "n" to daySessions.size) + "  ·  " + notesLabel, 12f), ui.lp(t = 2))
                        }, ui.lp(0, WRAP, 1f))
                        addView(ui.iconButton(Icon.TRASH, c.t("delete"), p.muted, 36, 17) {
                            act.confirm(c.t("delete_day_q", "n" to day), c.t("delete"), danger = true) {
                                Journey.deleteDay(act, cy.id, day)
                                act.toast(c.t("deleted"))
                                act.refresh()
                            }
                        })
                        addView(IconView(act, if (dayOpen) Icon.CHEVRON_DOWN else Icon.CHEVRON, p.muted, 16), ui.lp(ui.dp(16), ui.dp(16)))
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
            page.addView(card, ui.lp(b = 14))
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
