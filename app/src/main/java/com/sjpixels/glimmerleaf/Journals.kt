package com.sjpixels.glimmerleaf

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Calls [block] after every text change. */
fun EditText.onChanged(block: () -> Unit) = addTextChangedListener(object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) = block()
})

/**
 * Numbered single-line fields with a "+ Add another" button underneath. Starts with [min] fields
 * (or as many as [saved] needs) and grows without limit. Returns the live list of fields.
 */
fun numberedList(ui: Ui, parent: LinearLayout, saved: List<String>, min: Int, watch: (EditText) -> Unit): MutableList<EditText> {
    val p = ui.p
    val list = ArrayList<EditText>()
    val rows = ui.col()
    parent.addView(rows, ui.lp())
    fun addRow(text: String): EditText {
        val r = ui.row(Gravity.CENTER_VERTICAL)
        r.addView(ui.text("${list.size + 1}", 14f, p.muted, Fonts.sansMedium, gravity = Gravity.START, lineSp = 20f), ui.lp(ui.dp(28), WRAP))
        val e = ui.input("", false, text)
        watch(e)
        r.addView(e, ui.lp(0, WRAP, 1f))
        rows.addView(r, ui.lp(b = 8))
        list += e
        return e
    }
    val n = maxOf(min, saved.indexOfLast { it.isNotBlank() } + 1)
    repeat(n) { addRow(saved.getOrElse(it) { "" }) }
    parent.addView(ui.button(ui.c.t("add_more"), Btn.GHOST, Icon.PLUS) {
        val e = addRow("")
        e.requestFocus()
        e.post { ui.ctx.getSystemService(InputMethodManager::class.java)?.showSoftInput(e, 0) }
    }, ui.lp(WRAP, WRAP, l = 16))
    return list
}

private fun fullDate(ui: Ui, d: LocalDate): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Content.locale(ui.c.lang)).format(d)

private fun bullets(ui: Ui, card: LinearLayout, label: String, items: List<String>) {
    val shown = items.filter { it.isNotBlank() }
    if (shown.isEmpty()) return
    card.addView(ui.eyebrow(label), ui.lp(t = 12, b = 4))
    shown.forEach { card.addView(ui.text("•  $it", 16f, ui.p.ink, Fonts.sans, lineSp = 24f).apply { setTextIsSelectable(true) }, ui.lp(t = 2)) }
}

/** A saved day of wins and thanks. */
fun dailyCard(act: MainActivity, ui: Ui, e: DailyEntry): View {
    val c = ui.c
    val p = ui.p
    val date = LocalDate.parse(e.date)
    val card = ui.card(16)
    card.addView(ui.row {
        addView(ui.col {
            addView(ui.h3(fullDate(ui, date)))
            addView(ui.muted(c.t("wins_thanks_n", "w" to e.winCount, "t" to e.thanksCount)))
        }, ui.lp(0, WRAP, 1f))
        addView(ui.iconButton(Icon.EDIT, c.t("edit"), p.ink) { act.push(DailyScreen(act, date)) })
        addView(ui.iconButton(Icon.TRASH, c.t("delete"), p.muted) {
            act.confirm(c.t("delete_daily_q"), c.t("delete"), danger = true) {
                Db.get(act).deleteDaily(e.date)
                act.toast(c.t("deleted"))
                act.refresh()
            }
        })
    })
    bullets(ui, card, c.t("daily_wins"), e.wins)
    bullets(ui, card, c.t("daily_thanks"), e.thanks)
    return card
}

/** A journal entry with its timestamps, full text, and edit / delete. */
fun journalCard(act: MainActivity, ui: Ui, e: JournalEntry): View {
    val c = ui.c
    val p = ui.p
    val card = ui.card(16)
    card.addView(ui.row {
        addView(ui.col {
            addView(ui.muted(ui.dateTime(e.createdAt)))
            if (e.updatedAt - e.createdAt > 60_000L) addView(ui.muted(c.t("journal_edited", "time" to ui.dateTime(e.updatedAt)), 12f))
        }, ui.lp(0, WRAP, 1f))
        addView(ui.iconButton(Icon.EDIT, c.t("edit"), p.ink) { act.push(JournalEditScreen(act, e.id)) })
        addView(ui.iconButton(Icon.TRASH, c.t("delete"), p.muted) {
            act.confirm(c.t("delete_journal_q"), c.t("delete"), danger = true) {
                Db.get(act).deleteJournal(e.id)
                act.toast(c.t("deleted"))
                act.refresh()
            }
        })
    })
    card.addView(ui.body(e.text).apply { setTextIsSelectable(true) }, ui.lp(t = 8))
    return card
}

/** Adds up to [limit] saved days, with "Show more" when there are more. */
fun addDailyList(act: MainActivity, ui: Ui, page: LinearLayout, entries: List<DailyEntry>, limit: Int, onMore: () -> Unit) {
    if (entries.isEmpty()) {
        page.addView(ui.card(24) {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(ArtView(act, Art.NO_HISTORY, ui.p.accent, 72), ui.lp(ui.dp(72), ui.dp(72), b = 16))
            addView(ui.text(ui.c.t("daily_empty"), 16f, ui.p.muted, Fonts.sans, gravity = Gravity.CENTER, lineSp = 24f))
        }, ui.lp())
        return
    }
    entries.take(limit).forEach { page.addView(dailyCard(act, ui, it), ui.lp(b = 12)) }
    if (entries.size > limit) page.addView(ui.button(ui.c.t("show_more"), Btn.SECONDARY) { onMore() }, ui.lp())
}

/** Adds up to [limit] journal entries (newest first), with "Show more" when there are more. */
fun addJournalList(act: MainActivity, ui: Ui, page: LinearLayout, limit: Int, onMore: () -> Unit) {
    val db = Db.get(act)
    val entries = db.journalEntries(limit)
    if (entries.isEmpty()) {
        page.addView(ui.card(24) {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(ArtView(act, Art.NO_JOURNEY, ui.p.accent, 72), ui.lp(ui.dp(72), ui.dp(72), b = 16))
            addView(ui.text(ui.c.t("journal_empty"), 16f, ui.p.muted, Fonts.sans, gravity = Gravity.CENTER, lineSp = 24f))
        }, ui.lp())
        return
    }
    entries.forEach { page.addView(journalCard(act, ui, it), ui.lp(b = 12)) }
    if (db.journalCount() > limit) page.addView(ui.button(ui.c.t("show_more"), Btn.SECONDARY) { onMore() }, ui.lp())
}

/**
 * Daily gratitude: small wins and thanks for one day (today by default), at least five of each,
 * with "+ Add another" for more. Saves as you type. Earlier days are listed below.
 */
class DailyScreen(act: MainActivity, private val date: LocalDate = LocalDate.now()) : Screen(act) {
    private var wins: List<EditText> = emptyList()
    private var thanks: List<EditText> = emptyList()
    private var savedLabel: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val saver = Runnable { save(true) }
    private var limit = 7

    override val refreshOnResume: Boolean = false

    override fun build(ui: Ui): View {
        val c = ui.c
        val db = Db.get(act)
        val key = date.toString()
        val entry = db.daily(key)
        val page = ui.screen()

        page.addView(ui.eyebrow(fullDate(ui, date)), ui.lp(t = 8))
        page.addView(ui.row {
            addView(ui.h1(c.t("daily_title")), ui.lp(0, WRAP, 1f, r = 8))
            savedLabel = ui.muted("", 12f).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
            addView(savedLabel)
        }, ui.lp(t = 4))
        page.addView(ui.themeLine(c.t("daily_sub")), ui.lp(t = 4, b = 16))

        val watch: (EditText) -> Unit = { e ->
            e.onChanged {
                handler.removeCallbacks(saver)
                handler.postDelayed(saver, 600)
            }
        }
        val winsCard = ui.card(16)
        winsCard.addView(ui.row {
            addView(IconView(act, Icon.SPARK, ui.p.accent, 24), ui.lp(ui.dp(24), ui.dp(24), r = 12))
            addView(ui.cardTitle(c.t("daily_wins")), ui.lp(0, WRAP, 1f))
        }, ui.lp(b = 12))
        wins = numberedList(ui, winsCard, entry?.wins ?: emptyList(), 5, watch)
        page.addView(winsCard, ui.lp(b = 12))

        val thanksCard = ui.card(16)
        thanksCard.addView(ui.row {
            addView(IconView(act, Icon.HEART, ui.p.accent, 24), ui.lp(ui.dp(24), ui.dp(24), r = 12))
            addView(ui.cardTitle(c.t("daily_thanks")), ui.lp(0, WRAP, 1f))
        }, ui.lp(b = 12))
        thanks = numberedList(ui, thanksCard, entry?.thanks ?: emptyList(), 5, watch)
        page.addView(thanksCard, ui.lp())

        // Earlier days
        val earlier = db.dailyAll().filter { it.date != key }
        if (earlier.isNotEmpty()) {
            page.addView(ui.sectionTitle(c.t("daily_earlier")))
            addDailyList(act, ui, page, earlier, limit) { act.openHistory(1) }
        }
        return ui.page(page)
    }

    private fun save(showLabel: Boolean) {
        if (wins.isEmpty() && thanks.isEmpty()) return
        Db.get(act).saveDaily(date.toString(), wins.map { it.text.toString().trim() }, thanks.map { it.text.toString().trim() },
            System.currentTimeMillis())
        if (showLabel) savedLabel?.text = act.pack.t("saved")
    }

    override fun onHide() {
        handler.removeCallbacks(saver)
        save(false)
        super.onHide()
    }
}

/**
 * Gratitude journal: write freely, as often as you like. Each save is a new entry with its own
 * timestamp; the unsaved text is kept as a draft. Entries are listed below, newest first.
 */
class JournalScreen(act: MainActivity) : Screen(act) {
    private var limit = 20

    override fun build(ui: Ui): View {
        val c = ui.c
        val prefs = act.prefs
        val page = ui.screen()

        page.addView(ui.eyebrow(ui.longToday()), ui.lp(t = 8))
        page.addView(ui.h1(c.t("journal_title")), ui.lp(t = 4))
        page.addView(ui.themeLine(c.t("journal_sub")), ui.lp(t = 4, b = 16))

        val composer = ui.card(16)
        val input = ui.input(c.t("journal_hint"), true, prefs.journalDraft).apply {
            minLines = 6
            onChanged { prefs.journalDraft = text.toString() }
        }
        composer.addView(input, ui.lp())
        composer.addView(ui.button(c.t("journal_save"), icon = Icon.CHECK) {
            val text = input.text.toString().trim()
            if (text.isEmpty()) {
                input.requestFocus()
                return@button
            }
            Db.get(act).insertJournal(text, System.currentTimeMillis())
            prefs.journalDraft = ""
            act.hideKeyboard()
            act.toast(c.t("saved"))
            act.refresh()
        }, ui.lp(t = 12))
        page.addView(composer, ui.lp())

        page.addView(ui.sectionTitle(c.t("journal_entries")))
        addJournalList(act, ui, page, limit) {
            limit += 20
            act.refresh()
        }
        return ui.page(page)
    }
}

/** Edit or delete one journal entry. */
class JournalEditScreen(act: MainActivity, private val id: Long) : Screen(act) {
    override val refreshOnResume: Boolean = false

    override fun build(ui: Ui): View {
        val c = ui.c
        val page = ui.screen()
        val e = Db.get(act).journalEntry(id)
        if (e == null) {
            page.addView(ui.muted(c.t("no_entries")), ui.lp(t = 16))
            return ui.page(page)
        }
        page.addView(ui.eyebrow(ui.dateTime(e.createdAt)), ui.lp(t = 8))
        page.addView(ui.h1(c.t("journal_edit_title")), ui.lp(t = 4, b = 16))
        val input = ui.input(c.t("journal_hint"), true, e.text).apply { minLines = 8 }
        page.addView(input, ui.lp())
        page.addView(ui.button(c.t("save_changes"), icon = Icon.CHECK) {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@button
            if (text != e.text) Db.get(act).updateJournal(id, text, System.currentTimeMillis())
            act.hideKeyboard()
            act.toast(c.t("saved"))
            act.goBack()
        }, ui.lp(t = 16))
        page.addView(ui.button(c.t("delete"), Btn.DANGER, Icon.TRASH) {
            act.confirm(c.t("delete_journal_q"), c.t("delete"), danger = true) {
                Db.get(act).deleteJournal(id)
                act.toast(c.t("deleted"))
                act.goBack()
            }
        }, ui.lp(t = 8))
        return ui.page(page)
    }
}
