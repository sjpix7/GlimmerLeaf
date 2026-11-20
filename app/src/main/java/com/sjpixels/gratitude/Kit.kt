package com.sjpixels.gratitude

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
    ): TextView = TextView(ctx).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(color)
        typeface = font
        setLineSpacing(0f, lineMult)
        this.gravity = gravity
    }

    fun eyebrow(s: String, color: Int = p.accent) = text(s.uppercase(), 11.5f, color, Fonts.sansBold).apply {
        letterSpacing = 0.12f
    }

    fun h1(s: String) = text(s, 28f, p.ink, Fonts.serifBold, 1.12f)
    fun h2(s: String) = text(s, 20f, p.ink, Fonts.serifBold, 1.15f)
    fun h3(s: String) = text(s, 16.5f, p.ink, Fonts.sansMedium, 1.2f)
    fun body(s: String) = text(s, 16f, p.ink, Fonts.sans, 1.42f)
    fun muted(s: String, size: Float = 13.5f) = text(s, size, p.muted, Fonts.sans, 1.3f)

    fun ripple(content: Drawable?, mask: Drawable?): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(withAlpha(p.accent, 50)), content, mask)

    fun card(pad: Int = 18, color: Int = p.surface, block: LinearLayout.() -> Unit = {}): LinearLayout =
        col(pad, pad, block).apply {
            background = rounded(color, dpf(22), p.line, dp(1))
        }

    fun clickable(v: View, radius: Float = dpf(22), onClick: () -> Unit) {
        val bg = v.background
        v.background = ripple(bg, rounded(Color.WHITE, radius))
        v.isClickable = true
        v.isFocusable = true
        v.setOnClickListener { onClick() }
    }

    fun button(label: String, kind: Btn = Btn.PRIMARY, icon: Icon? = null, onClick: () -> Unit): LinearLayout {
        val (bgColor, fg, stroke) = when (kind) {
            Btn.PRIMARY -> Triple(p.accent, p.onAccent, 0)
            Btn.SECONDARY -> Triple(p.accentSoft, p.ink, 0)
            Btn.GHOST -> Triple(Color.TRANSPARENT, p.accent, p.line)
            Btn.DANGER -> Triple(Color.TRANSPARENT, p.danger, withAlpha(p.danger, 120))
        }
        return row(Gravity.CENTER) {
            minimumHeight = dp(48)
            setPadding(dp(20), dp(10), dp(20), dp(10))
            val shape = rounded(bgColor, dpf(26), stroke, if (stroke != 0) dp(1) else 0)
            background = ripple(shape, rounded(Color.WHITE, dpf(26)))
            if (icon != null) {
                addView(IconView(ctx, icon, fg, 20), lp(dp(20), dp(20), r = 8))
            }
            addView(text(label, 15f, fg, Fonts.sansMedium, 1.1f, Gravity.CENTER))
            isClickable = true
            isFocusable = true
            contentDescription = label
            setOnClickListener { onClick() }
        }
    }

    fun chip(label: String, bg: Int = p.accentSoft, fg: Int = p.ink): TextView =
        text(label, 12.5f, fg, Fonts.sansMedium, 1.1f).apply {
            setPadding(dp(10), dp(4), dp(10), dp(5))
            background = rounded(bg, dpf(14))
        }

    fun iconButton(icon: Icon, desc: String, color: Int = p.ink, size: Int = 44, iconSize: Int = 22, bg: Int = Color.TRANSPARENT, onClick: () -> Unit): FrameLayout =
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

    fun screen(): LinearLayout = col(20, 8)

    fun sectionTitle(s: String, top: Int = 26): TextView = h2(s).apply { layoutParams = lp(t = top, b = 12) }

    fun input(hint: String, multiline: Boolean, value: String): EditText = EditText(ctx).apply {
        setText(value)
        this.hint = hint
        setHintTextColor(withAlpha(p.muted, 150))
        setTextColor(p.ink)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
        typeface = Fonts.sans
        background = rounded(p.surfaceAlt, dpf(14), p.line, dp(1))
        setPadding(dp(14), dp(11), dp(14), dp(11))
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

val MOODS = listOf("😔", "😐", "🙂", "😊", "🤩")
