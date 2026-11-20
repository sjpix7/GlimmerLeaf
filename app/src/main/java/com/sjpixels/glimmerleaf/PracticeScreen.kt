package com.sjpixels.glimmerleaf

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The "listen" card: play / pause, previous, stop, next. Highlights the paragraph being read
 * and keeps it on screen.
 */
class ReaderController(
    private val act: MainActivity,
    private val ui: Ui,
    private val chunks: List<String>,
    private val views: List<View>,
) : Speaker.Listener {

    private var playIcon: IconView? = null
    private var status: TextView? = null
    private var highlighted: View? = null
    private var highlightedBg: Drawable? = null

    fun buildCard(): View {
        val p = ui.p
        val c = ui.c
        val card = ui.card(16)
        val top = ui.row()
        val play = ui.iconButton(Icon.PLAY, c.t("play"), p.onAccent, 56, 24, p.accent) { toggle() }
        playIcon = play.tag as IconView
        top.addView(play, ui.lp(ui.dp(56), ui.dp(56), r = 16))
        val info = ui.col {
            addView(ui.cardTitle(c.t("listen")))
            val gender = if (act.prefs.voiceGender == "male") c.t("voice_male") else c.t("voice_female")
            status = ui.muted((Content.NATIVE[c.lang] ?: c.lang) + " · " + gender)
            addView(status, ui.lp(t = 2))
        }
        top.addView(info, ui.lp(0, WRAP, 1f))
        card.addView(top, ui.lp())

        val controls = ui.row(Gravity.CENTER)
        controls.addView(ui.iconButton(Icon.PREV, c.t("previous"), p.ink, 48, 24) { act.speaker.previous(act.prefs) })
        controls.addView(ui.space(0), ui.lp(ui.dp(16), 1))
        controls.addView(ui.iconButton(Icon.STOP, c.t("stop"), p.ink, 48, 24) {
            act.speaker.stop()
            clearHighlight()
        })
        controls.addView(ui.space(0), ui.lp(ui.dp(16), 1))
        controls.addView(ui.iconButton(Icon.NEXT, c.t("next"), p.ink, 48, 24) { act.speaker.next(act.prefs) })
        card.addView(controls, ui.lp(t = 12))

        attach()
        val s = act.speaker
        if (s.isLoaded(chunks)) {
            onPlayingChanged(s.playing)
            if (s.playing) card.post { highlight(s.index, scroll = false) }
        }
        return card
    }

    fun attach() {
        act.speaker.listener = this
    }

    fun detach(stop: Boolean) {
        val s = act.speaker
        if (stop && s.isLoaded(chunks)) s.stop()
        if (s.listener === this) s.listener = null
    }

    fun toggle() {
        val s = act.speaker
        attach()
        when {
            s.playing && s.isLoaded(chunks) -> s.pause()
            s.isLoaded(chunks) -> s.resume(act.prefs)
            else -> {
                status?.text = ui.c.t("tts_starting")
                s.play(act.prefs, chunks, 0)
            }
        }
    }

    override fun onChunk(index: Int) = highlight(index, scroll = true)

    override fun onFinished() {
        clearHighlight()
    }

    override fun onPlayingChanged(playing: Boolean) {
        playIcon?.icon = if (playing) Icon.PAUSE else Icon.PLAY
        val c = ui.c
        val gender = if (act.prefs.voiceGender == "male") c.t("voice_male") else c.t("voice_female")
        status?.text = if (playing) c.t("now_reading") else (Content.NATIVE[c.lang] ?: c.lang) + " · " + gender
        (playIcon?.parent as? View)?.contentDescription = if (playing) c.t("pause") else c.t("play")
    }

    override fun onLanguageMissing() {
        onPlayingChanged(false)
        val c = ui.c
        act.confirm(c.t("tts_unavailable"), c.t("tts_install")) { openVoiceInstaller(act) }
    }

    private fun clearHighlight() {
        highlighted?.background = highlightedBg
        highlighted = null
        highlightedBg = null
    }

    private fun highlight(i: Int, scroll: Boolean) {
        clearHighlight()
        val v = views.getOrNull(i) ?: return
        highlighted = v
        highlightedBg = v.background
        v.background = rounded(ui.p.accentSoft, ui.dpf(12))
        if (scroll) scrollTo(v)
    }

    private fun scrollTo(v: View) {
        var top = 0
        var cur: View? = v
        var sv: ScrollView? = null
        while (cur != null) {
            val parent = cur.parent
            if (parent is ScrollView) { sv = parent; break }
            top += cur.top
            cur = parent as? View
        }
        sv?.smoothScrollTo(0, (top - ui.dp(140)).coerceAtLeast(0))
    }
}

fun openVoiceInstaller(act: MainActivity) {
    try {
        act.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        try {
            act.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: ActivityNotFoundException) {
            act.toast(act.pack.t("tts_unavailable"))
        }
    }
}

/** One day's practice: read, listen, journal, complete. */
class PracticeScreen(act: MainActivity, private val dayNum: Int, private val autoplay: Boolean = false) : Screen(act) {

    private val openedAt = System.currentTimeMillis()
    private var reader: ReaderController? = null
    private val fields = ArrayList<List<EditText>>()
    private var prompts: List<String> = emptyList()
    private var reflection: EditText? = null
    private var mood = 0
    private var moodLoaded = false
    private var draftLabel: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val saver = Runnable { saveDraft(true) }
    private var autoplayDone = false

    override val refreshOnResume: Boolean = false

    override fun build(ui: Ui): View {
        val c = ui.c
        val p = ui.p
        val st = Journey.state(act)
        val status = st.statusOf(dayNum)
        val d = c.day(dayNum)
        val count = st.counts[dayNum]
        val page = ui.screen()

        val chunks = ArrayList<String>()
        val chunkViews = ArrayList<View>()

        page.addView(ui.row { addView(ui.chip(c.t("day_n_of_28", "n" to dayNum) + " · " + c.weekTitle(dayNum))) }, ui.lp(t = 8))
        val headBlock = ui.col {
            setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4))
            addView(ui.h1(d.title))
            addView(ui.themeLine(d.theme), ui.lp(t = 4))
        }
        page.addView(headBlock, ui.lp(t = 2, l = -4, r = -4))
        chunks += c.t("day_n", "n" to dayNum) + ". " + d.title + ". " + d.theme
        chunkViews += headBlock

        // status chip
        val chipText = when (status) {
            DayStatus.DONE -> if (count > 1) c.t("status_done_times", "n" to count) else c.t("status_done")
            DayStatus.CURRENT -> if (st.isResting()) c.t("next_unlocks", "n" to dayNum, "time" to ui.relative(st.unlockAt)) else c.t("status_current")
            DayStatus.UPCOMING -> c.t("status_upcoming")
            DayStatus.NONE -> null
        }
        if (chipText != null) {
            val chipRow = ui.row {
                val chip = when (status) {
                    DayStatus.DONE -> ui.chip(chipText, p.accent, p.onAccent, Icon.CHECK)
                    DayStatus.CURRENT -> ui.chip(chipText, icon = if (st.isResting()) Icon.LOCK else Icon.BOOK)
                    else -> ui.chip(chipText)
                }
                addView(chip)
            }
            page.addView(chipRow, ui.lp(t = 12))
        }
        if (status == DayStatus.UPCOMING || (status == DayStatus.CURRENT && st.isResting())) {
            val banner = ui.row(Gravity.TOP) {
                setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(12))
                background = rounded(p.surfaceAlt, ui.dpf(16), p.line, ui.dp(1))
                addView(IconView(act, Icon.SPARK, p.ink, 20), ui.lp(ui.dp(20), ui.dp(20), r = 12))
                addView(ui.text(c.t("future_banner"), 14f, p.ink, Fonts.sans, lineSp = 20f), ui.lp(0, WRAP, 1f))
            }
            page.addView(banner, ui.lp(t = 12))
        }

        // reader card placeholder (inserted once all chunks are known)
        val readerIndex = page.childCount

        // intro
        for (para in d.intro) {
            val tv = ui.body(para).apply { setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4)) }
            page.addView(tv, ui.lp(t = 8, l = -8, r = -8))
            chunks += para
            chunkViews += tv
        }

        // steps
        page.addView(ui.sectionTitle(c.t("section_practice")))
        d.steps.forEachIndexed { i, step ->
            val rowV = ui.row(Gravity.TOP) {
                // Step card: 16dp inset, 24dp number gutter, 12dp gap.
                setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16))
                background = rounded(p.surface, ui.dpf(16), p.line, ui.dp(1))
                val num = ui.text((i + 1).toString(), 16f, p.ink, Fonts.serifBold, gravity = Gravity.START, lineSp = 24f)
                addView(num, ui.lp(ui.dp(24), WRAP, r = 12))
                addView(ui.body(step), ui.lp(0, WRAP, 1f))
            }
            page.addView(rowV, ui.lp(b = 12))
            chunks += c.t("step_n", "n" to i + 1) + ". " + step
            chunkViews += rowV
        }

        // affirmation
        page.addView(ui.sectionTitle(c.t("section_affirmation")))
        // Quote card: 20dp inset on accentSoft, 32sp decorative opening quote, italic ink.
        val aff = ui.col(20, 20) {
            background = rounded(p.accentSoft, ui.dpf(16))
            addView(ui.text("“", 32f, p.ink, Fonts.serifBold, lineSp = 32f).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            addView(ui.themeLine(d.affirmation, p.ink), ui.lp(t = 4))
        }
        page.addView(aff, ui.lp())
        chunks += d.affirmation
        chunkViews += aff

        val rc = ReaderController(act, ui, chunks, chunkViews)
        reader = rc
        page.addView(rc.buildCard(), readerIndex, ui.lp(t = 18, b = 6))

        // ------------------------------------------------------------ journal
        val jHead = ui.row {
            addView(ui.h2(c.t("section_journal")), ui.lp(0, WRAP, 1f))
            draftLabel = ui.muted("", 12f).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
            addView(draftLabel)
        }
        page.addView(jHead, ui.lp(t = 28, b = 12))

        val draft = Db.get(act).draft(dayNum)
        if (!moodLoaded) {
            mood = draft?.mood ?: 0
            moodLoaded = true
        }
        fields.clear()
        val sections = c.journalFor(d)
        prompts = sections.map { it.prompt }
        val journalCard = ui.card(16)
        sections.forEachIndexed { si, sec ->
            journalCard.addView(ui.h3(sec.prompt), ui.lp(t = if (si == 0) 0 else 20, b = 8))
            val saved = draft?.notes?.sections?.getOrNull(si)?.items ?: emptyList()
            val list = ArrayList<EditText>()
            if (sec.multiline) {
                val e = ui.input("", true, saved.getOrNull(0) ?: "")
                watch(e)
                journalCard.addView(e, ui.lp())
                list += e
            } else {
                for (k in 0 until sec.count) {
                    val r = ui.row(Gravity.CENTER_VERTICAL)
                    r.addView(ui.text("${k + 1}", 14f, p.muted, Fonts.sansMedium, gravity = Gravity.START, lineSp = 20f), ui.lp(ui.dp(24), WRAP))
                    val e = ui.input("", false, saved.getOrNull(k) ?: "")
                    watch(e)
                    r.addView(e, ui.lp(0, WRAP, 1f))
                    journalCard.addView(r, ui.lp(b = 8))
                    list += e
                }
            }
            fields += list
        }
        journalCard.addView(ui.h3(c.t("reflection_label")), ui.lp(t = 20, b = 8))
        val refl = ui.input(c.t("reflection_hint"), true, draft?.notes?.reflection ?: "")
        watch(refl)
        reflection = refl
        journalCard.addView(refl, ui.lp())

        journalCard.addView(ui.h3(c.t("mood_label")), ui.lp(t = 20, b = 8))
        val moodRow = ui.row(Gravity.CENTER)
        val moodViews = ArrayList<TextView>()
        val moodNames = listOf("sad", "neutral", "okay", "happy", "delighted")
        MOODS.forEachIndexed { i, m ->
            val tv = ui.text(m, 24f, p.ink, Fonts.sans, 1f, Gravity.CENTER)
            tv.contentDescription = moodNames[i]
            tv.isClickable = true
            tv.isFocusable = true
            moodViews += tv
            tv.setOnClickListener {
                mood = if (mood == i + 1) 0 else i + 1
                styleMoods(ui, moodViews)
                handler.removeCallbacks(saver)
                handler.postDelayed(saver, 400)
            }
            moodRow.addView(tv, ui.lp(0, ui.dp(48), 1f, l = 2, r = 2))
        }
        styleMoods(ui, moodViews)
        journalCard.addView(moodRow, ui.lp())
        page.addView(journalCard, ui.lp())

        val label = if (count > 0) c.t("complete_again_button") else c.t("complete_button")
        page.addView(ui.button(label, icon = Icon.CHECK) { complete() }, ui.lp(t = 16))

        // ------------------------------------------------------------ previous entries
        val cycle = st.cycle
        if (cycle != null) {
            val sessions = Db.get(act).sessionsForDay(cycle.id, dayNum)
            page.addView(ui.sectionTitle(c.t("entries_title")))
            if (sessions.isEmpty()) {
                page.addView(ui.card { addView(ui.muted(c.t("no_entries"))) }, ui.lp())
            } else {
                for (s in sessions) page.addView(entryCard(ui, s) {
                    act.confirm(c.t("delete_session_q"), c.t("delete"), danger = true) {
                        Journey.deleteSession(act, s.id)
                        act.toast(c.t("deleted"))
                        act.refresh()
                    }
                }, ui.lp(b = 12))
            }
        }

        if (autoplay && !autoplayDone) {
            autoplayDone = true
            page.post { rc.toggle() }
        }
        return ui.page(page)
    }

    private fun styleMoods(ui: Ui, views: List<TextView>) {
        views.forEachIndexed { i, tv ->
            val sel = mood == i + 1
            // Unselected surface; selected accentSoft with a 2dp accent ring.
            tv.background = rounded(if (sel) ui.p.accentSoft else ui.p.surface, ui.dpf(16), if (sel) ui.p.accent else ui.p.line, ui.dp(if (sel) 2 else 1))
            tv.isSelected = sel
        }
    }

    private fun watch(e: EditText) {
        e.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacks(saver)
                handler.postDelayed(saver, 700)
            }
        })
    }

    private fun collectNotes(): Notes {
        val sections = fields.mapIndexed { i, list ->
            NoteSection(prompts.getOrElse(i) { "" }, list.map { it.text.toString().trim() })
        }
        return Notes(sections, reflection?.text?.toString()?.trim() ?: "")
    }

    private fun saveDraft(showLabel: Boolean) {
        if (fields.isEmpty()) return
        val notes = collectNotes()
        val db = Db.get(act)
        if (notes.isEmpty() && mood == 0) {
            db.clearDraft(dayNum)
        } else {
            db.saveDraft(dayNum, notes, mood, System.currentTimeMillis())
            if (showLabel) draftLabel?.text = act.pack.t("draft_saved")
        }
    }

    private fun complete() {
        act.hideKeyboard()
        val c = act.pack
        if (Journey.state(act).cycle == null) {
            act.confirm(c.t("need_journey_body"), c.t("yes_start"), title = c.t("need_journey_title")) {
                act.startJourney()
                doComplete()
            }
        } else {
            doComplete()
        }
    }

    private fun doComplete() {
        handler.removeCallbacks(saver)
        val notes = collectNotes()
        val duration = ((System.currentTimeMillis() - openedAt) / 1000L).coerceIn(0L, 7200L).toInt()
        val result = Journey.complete(act, dayNum, notes, mood, duration)
        reader?.detach(stop = true)
        for (list in fields) for (e in list) e.setText("")
        reflection?.setText("")
        mood = 0
        handler.removeCallbacks(saver)
        act.refresh()
        act.celebrate(result)
    }

    override fun onHide() {
        // Keeps reading if the app goes to the background; a rebuilt page re-attaches its own reader card.
        handler.removeCallbacks(saver)
        saveDraft(false)
        super.onHide()
    }

    override fun onLeave() {
        reader?.detach(stop = true)
    }
}

/** A saved session with its notes, used on the practice page and in History. */
fun entryCard(ui: Ui, s: Session, onDelete: (() -> Unit)?): View {
    val p = ui.p
    val c = ui.c
    val card = ui.card(16)
    val head = ui.row {
        addView(ui.muted(ui.dateTime(s.completedAt)), ui.lp(0, WRAP, 1f))
        if (s.mood in 1..5) addView(ui.text(MOODS[s.mood - 1], 20f).apply {
            contentDescription = listOf("sad", "neutral", "okay", "happy", "delighted")[s.mood - 1]
        }, ui.lp(WRAP, WRAP, r = 4))
        if (onDelete != null) addView(ui.iconButton(Icon.TRASH, c.t("delete"), p.muted, 48, 24) { onDelete() })
    }
    card.addView(head, ui.lp())
    var any = false
    for (sec in s.notes.sections) {
        val items = sec.items.filter { it.isNotBlank() }
        if (items.isEmpty()) continue
        any = true
        card.addView(ui.eyebrow(sec.prompt), ui.lp(t = 8, b = 4))
        for (it in items) card.addView(ui.text("•  $it", 16f, p.ink, Fonts.sans, lineSp = 24f), ui.lp(t = 2))
    }
    if (s.notes.reflection.isNotBlank()) {
        any = true
        card.addView(ui.eyebrow(c.t("reflection_label")), ui.lp(t = 8, b = 4))
        card.addView(ui.text(s.notes.reflection, 16f, p.ink, Fonts.sans, lineSp = 24f))
    }
    if (!any) card.addView(ui.muted(c.t("no_entries")), ui.lp(t = 4))
    return card
}
