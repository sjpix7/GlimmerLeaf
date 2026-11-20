package com.sjpixels.glimmerleaf

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.InputType
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object Fonts {
    val serifBold: Typeface = Typeface.create("serif", Typeface.BOLD)
    val serif: Typeface = Typeface.create("serif", Typeface.NORMAL)
    val serifItalic: Typeface = Typeface.create("serif", Typeface.ITALIC)
    val sans: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    val sansMedium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val sansBold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    /** Card titles: serif medium (weight 500) where the platform supports weights, else regular. */
    val serifMedium: Typeface =
        if (android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(serif, 500, false) else serif
}

fun Context.dp(v: Number): Int = (v.toFloat() * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Number): Float = v.toFloat() * resources.displayMetrics.density

fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

fun blend(a: Int, b: Int, t: Float): Int {
    val u = t.coerceIn(0f, 1f)
    fun ch(x: Int, y: Int) = (x + (y - x) * u).toInt()
    return Color.argb(
        ch(Color.alpha(a), Color.alpha(b)), ch(Color.red(a), Color.red(b)),
        ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b))
    )
}

fun rounded(color: Int, radius: Float, strokeColor: Int = 0, strokeWidth: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        setColor(color)
        if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }

fun oval(color: Int, strokeColor: Int = 0, strokeWidth: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }

enum class Btn { PRIMARY, SECONDARY, GHOST, DANGER }

/** Small view-building toolkit so every screen shares one look. */
class Ui(val ctx: Context, val p: Pal, val c: ContentPack) {

    fun dp(v: Number) = ctx.dp(v)
    fun dpf(v: Number) = ctx.dpf(v)

    fun lp(w: Int = MATCH, h: Int = WRAP, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
        LinearLayout.LayoutParams(w, h, weight).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }

    fun flp(w: Int = MATCH, h: Int = WRAP, gravity: Int = Gravity.NO_GRAVITY) =
        FrameLayout.LayoutParams(w, h, gravity)

    fun col(padH: Int = 0, padV: Int = 0, block: LinearLayout.() -> Unit = {}): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(padH), dp(padV), dp(padH), dp(padV))
            block()
        }

    fun row(gravity: Int = Gravity.CENTER_VERTICAL, block: LinearLayout.() -> Unit = {}): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            this.gravity = gravity
            block()
        }

    fun text(
        s: CharSequence,
        size: Float = 15f,
        color: Int = p.ink,
        font: Typeface = Fonts.sans,
        lineMult: Float = 1.25f,
        gravity: Int = Gravity.START,
        lineSp: Float = 0f,
    ): TextView = TextView(ctx).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(color)
        typeface = font
        // Devanagari and CJK get 4sp of extra leading (design kit, typography notes).
        val extra = if (c.lang in TALL_SCRIPTS) 4f else 0f
        if (lineSp > 0f && android.os.Build.VERSION.SDK_INT >= 28) {
            lineHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, lineSp + extra, ctx.resources.displayMetrics).toInt()
        } else if (lineSp > 0f) {
            setLineSpacing(0f, (lineSp + extra) / (size * 1.17f))
        } else {
            setLineSpacing(0f, lineMult + extra / size)
        }
        this.gravity = gravity
        hyphenationFrequency = android.text.Layout.HYPHENATION_FREQUENCY_NONE
    }

    // Type scale from design/kit (size / line height in sp).
    fun eyebrow(s: String, color: Int = p.muted) = text(s, 12f, color, Fonts.sansMedium, lineSp = 16f).apply {
        letterSpacing = 0.5f / 12f
    }

    /** Page title, serif bold 30/36. */
    fun h1(s: String) = text(s, 30f, p.ink, heading(Fonts.serifBold), lineSp = 36f)
    /** Hero and section title, serif bold 24/32. */
    fun h2(s: String) = text(s, 24f, p.ink, heading(Fonts.serifBold), lineSp = 32f)
    /** Card title, serif medium 20/28. */
    fun cardTitle(s: String) = text(s, 20f, p.ink, heading(Fonts.serifMedium), lineSp = 28f)
    /** Row title, sans medium 16/24. */
    fun h3(s: String) = text(s, 16f, p.ink, Fonts.sansMedium, lineSp = 24f)
    /** Theme line and affirmation, serif italic 17/26. */
    fun themeLine(s: String, color: Int = p.muted) = text(s, 17f, color, Fonts.serifItalic, lineSp = 26f)
    /** Body and input, sans 16/24. */
    fun body(s: String) = text(s, 16f, p.ink, Fonts.sans, lineSp = 24f)
    /** Secondary text, sans 14/20 (or caption 12/16). */
    fun muted(s: String, size: Float = 14f) = text(s, size, p.muted, Fonts.sans, lineSp = if (size <= 12.5f) 16f else 20f)

    /** CJK headings use medium weight instead of bold (design kit, typography notes). */
    private fun heading(tf: Typeface): Typeface =
        if (c.lang in CJK && tf == Fonts.serifBold) Fonts.serifMedium else tf

    fun ripple(content: Drawable?, mask: Drawable?): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(withAlpha(p.accent, 50)), content, mask)

    /** Card: 16dp radius, surface with a 1dp line border. */
    fun card(pad: Int = 16, color: Int = p.surface, block: LinearLayout.() -> Unit = {}): LinearLayout =
        col(pad, pad, block).apply {
            background = rounded(color, dpf(16), p.line, dp(1))
        }

    /** Hero: 24dp radius, 20dp padding, surface with the glow recipe and three sparkles. */
    fun hero(seed: Int = 3, block: LinearLayout.() -> Unit = {}): LinearLayout =
        col(20, 20, block).apply {
            background = GlowDrawable(p, dpf(24), ctx.resources.displayMetrics.density, seed)
        }

    /** Press feedback from the kit: scale 1 → 0.98 over 80ms, back in 120ms. */
    fun pressScale(v: View) {
        v.setOnTouchListener { view, e ->
            when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).setInterpolator(kitEase()).start()
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).setInterpolator(kitEase()).start()
            }
            false
        }
    }

    fun clickable(v: View, radius: Float = dpf(16), onClick: () -> Unit) {
        val bg = v.background
        v.background = ripple(bg, rounded(Color.WHITE, radius))
        v.isClickable = true
        v.isFocusable = true
        v.setOnClickListener { onClick() }
    }

    /**
     * Buttons: minimum 48dp tall, 16 × 12 padding, 24dp pill radius, optional 20dp icon with an 8dp gap.
     * Primary accent/onAccent; secondary surface/ink with a muted outline; ghost transparent/ink;
     * danger surface/danger outline. Long labels wrap and stay centred.
     */
    fun button(label: String, kind: Btn = Btn.PRIMARY, icon: Icon? = null, onClick: () -> Unit): LinearLayout {
        val (bgColor, fg, stroke) = when (kind) {
            Btn.PRIMARY -> Triple(p.accent, p.onAccent, 0)
            Btn.SECONDARY -> Triple(p.surface, p.ink, p.muted)
            Btn.GHOST -> Triple(Color.TRANSPARENT, p.ink, 0)
            Btn.DANGER -> Triple(p.surface, p.danger, p.danger)
        }
        return row(Gravity.CENTER) {
            minimumHeight = dp(48)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val shape = rounded(bgColor, dpf(24), stroke, if (stroke != 0) dp(1) else 0)
            background = RippleDrawable(ColorStateList.valueOf(withAlpha(if (kind == Btn.PRIMARY) p.onAccent else p.ink, 31)), shape, rounded(Color.WHITE, dpf(24)))
            if (icon != null) {
                addView(IconView(ctx, icon, fg, 20), lp(dp(20), dp(20), r = 8))
            }
            addView(text(label, 16f, fg, Fonts.sansMedium, gravity = Gravity.CENTER, lineSp = 24f))
            isClickable = true
            isFocusable = true
            contentDescription = label
            setOnClickListener { onClick() }
            pressScale(this)
        }
    }

    /** Chip: 24dp minimum height, 8 × 4 padding, 12dp radius, accentSoft/ink. */
    fun chip(label: String, bg: Int = p.accentSoft, fg: Int = p.ink, icon: Icon? = null): LinearLayout =
        row {
            minimumHeight = dp(24)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = rounded(bg, dpf(12))
            if (icon != null) addView(IconView(ctx, icon, fg, 14), lp(dp(14), dp(14), r = 4))
            addView(text(label, 12f, fg, Fonts.sansMedium, lineSp = 16f))
        }

    fun iconButton(icon: Icon, desc: String, color: Int = p.ink, size: Int = 48, iconSize: Int = 24, bg: Int = Color.TRANSPARENT, onClick: () -> Unit): FrameLayout =
        FrameLayout(ctx).apply {
            val icv = IconView(ctx, icon, color, iconSize)
            addView(icv, FrameLayout.LayoutParams(dp(iconSize), dp(iconSize), Gravity.CENTER))
            background = ripple(oval(bg), oval(Color.WHITE))
            contentDescription = desc
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
            tag = icv
        }

    fun space(h: Int): View = View(ctx).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    /** A hairline. Its height is fixed in onMeasure, so callers can pass any LayoutParams (e.g. lp(t = 12)) for margins. */
    fun divider(): View {
        val h = maxOf(1, dp(1) / 2 + 1)
        return object : View(ctx) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), h)
            }
        }.apply {
            setBackgroundColor(p.line)
            layoutParams = LinearLayout.LayoutParams(MATCH, h)
        }
    }

    fun scroll(content: View): ScrollView = ScrollView(ctx).apply {
        isFillViewport = true
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        addView(content, FrameLayout.LayoutParams(MATCH, WRAP))
    }

    /** Page column: 20dp side margins. */
    fun screen(): LinearLayout = col(20, 12)

    fun sectionTitle(s: String, top: Int = 28): TextView = h2(s).apply { layoutParams = lp(t = top, b = 12) }

    fun input(hint: String, multiline: Boolean, value: String): EditText = EditText(ctx).apply {
        setText(value)
        this.hint = hint
        setHintTextColor(withAlpha(p.muted, 150))
        setTextColor(p.ink)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        typeface = Fonts.sans
        // Numbered field: 48dp minimum (96dp multiline), 12dp inset, 8dp radius, muted border; accent when focused.
        val idle = rounded(p.surfaceAlt, dpf(8), p.muted, dp(1))
        val focused = rounded(p.surfaceAlt, dpf(8), p.accent, dp(2))
        background = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), idle)
        }
        minHeight = dp(if (multiline) 96 else 48)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        if (multiline) {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 4
            gravity = Gravity.TOP or Gravity.START
            isSingleLine = false
        } else {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 4
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            textCursorDrawable = rounded(p.accent, 0f).apply { setSize(dp(2), dp(20)) }
        }
    }

    fun progress(fraction: Float, height: Int = 8): ProgressLine = ProgressLine(ctx, p).apply {
        this.fraction = fraction
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(height))
    }

    // ---------------------------------------------------------------- dates

    private val locale get() = Content.locale(c.lang)

    fun time(ms: Long): String {
        val pattern = if (DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, locale).format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
    }

    fun date(ms: Long): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    fun dateTime(ms: Long): String = date(ms) + " · " + time(ms)

    fun longToday(): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(LocalDate.now())

    /** "today at 12:00", "tomorrow at 9:00" or "12 Nov 2026 at 9:00". */
    fun relative(ms: Long): String {
        val d = Journey.localDate(ms)
        val today = LocalDate.now()
        return when (d) {
            today -> c.t("time_today", "time" to time(ms))
            today.plusDays(1) -> c.t("time_tomorrow", "time" to time(ms))
            else -> c.t("time_on", "date" to date(ms), "time" to time(ms))
        }
    }

    fun minutesLabel(min: Int): String {
        val ms = LocalDate.now().atTime(min / 60, min % 60).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return time(ms)
    }
}

/** Segmented choice: 48dp cells on surfaceAlt; the selected cell is accent/onAccent. */
fun segmented(ui: Ui, options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit): View {
    val p = ui.p
    // Segmented choice: 48dp cells on surfaceAlt, selected accent/onAccent; labels wrap rather than clip.
    val bar = ui.row {
        setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4))
        background = rounded(p.surfaceAlt, ui.dpf(24), p.line, ui.dp(1))
    }
    options.forEachIndexed { i, (value, label) ->
        val sel = value == selected
        val t = ui.text(label, 14f, if (sel) p.onAccent else p.ink, Fonts.sansMedium, gravity = Gravity.CENTER, lineSp = 20f).apply {
            minHeight = ui.dp(48)
            setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8))
            background = rounded(if (sel) p.accent else 0, ui.dpf(20))
            isSelected = sel
            contentDescription = "$label, ${i + 1} / ${options.size}"
        }
        if (!sel) ui.clickable(t, ui.dpf(20)) { onPick(value) }
        bar.addView(t, ui.lp(0, MATCH, 1f))
    }
    return bar
}

/** Languages whose scripts need extra leading. */
val TALL_SCRIPTS = setOf("hi", "ja", "ko", "zh")
val CJK = setOf("ja", "ko", "zh")

val MOODS = listOf("😔", "😐", "🙂", "😊", "🤩")
