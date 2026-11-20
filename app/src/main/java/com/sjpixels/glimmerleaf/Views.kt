package com.sjpixels.glimmerleaf

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.PathInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** The kit's standard easing: cubic Bézier (.2, 0, 0, 1). */
fun kitEase() = PathInterpolator(0.2f, 0f, 0f, 1f)

/** Line icons from design/kit/svg/icons, converted to VectorDrawables (24-unit grid, 2-unit stroke). */
enum class Icon(val res: Int) {
    PLAY(R.drawable.ic_play), PAUSE(R.drawable.ic_pause), STOP(R.drawable.ic_stop),
    PREV(R.drawable.ic_previous), NEXT(R.drawable.ic_next), BACK(R.drawable.ic_back),
    SUN(R.drawable.ic_sun), MOON(R.drawable.ic_moon), TRASH(R.drawable.ic_trash), SHARE(R.drawable.ic_share),
    CHECK(R.drawable.ic_check), LOCK(R.drawable.ic_lock), HOME(R.drawable.ic_home), BOOK(R.drawable.ic_book),
    HISTORY(R.drawable.ic_history), CHART(R.drawable.ic_chart), GEAR(R.drawable.ic_gear), SPARK(R.drawable.ic_sparkle),
    CHEVRON(R.drawable.ic_chevron_right), CHEVRON_DOWN(R.drawable.ic_chevron_down), BELL(R.drawable.ic_bell),
    HEART(R.drawable.ic_heart), SPEAKER(R.drawable.ic_speaker), PLUS(R.drawable.ic_plus), EDIT(R.drawable.ic_edit),
}

/** Single-colour line illustrations from design/kit/svg/illustrations (72-unit grid). */
enum class Art(val res: Int) {
    NOTICING(R.drawable.ill_noticing), CONNECTION(R.drawable.ill_connection),
    BODY_HOME_WORLD(R.drawable.ill_body_home_world), TRANSFORMATION(R.drawable.ill_transformation),
    SUNRISE(R.drawable.ill_sunrise), GOLDEN_EVENING(R.drawable.ill_golden_evening),
    NO_JOURNEY(R.drawable.ill_no_journey), NO_HISTORY(R.drawable.ill_no_history), NO_INSIGHTS(R.drawable.ill_no_insights);

    companion object {
        /** The illustration for the week a day belongs to. */
        fun forDay(day: Int): Art = when ((day - 1) / 7) {
            0 -> NOTICING
            1 -> CONNECTION
            2 -> BODY_HOME_WORLD
            else -> TRANSFORMATION
        }
    }
}

/** Tinted vector drawables, inflated once per (resource, colour) and reused on every draw. */
private val tintedCache = HashMap<Long, Drawable>()

/** Draws a vector drawable resource tinted with one colour into the given box. */
fun Context.drawTinted(c: Canvas, res: Int, color: Int, left: Float, top: Float, size: Float) {
    val key = (res.toLong() shl 32) or (color.toLong() and 0xFFFFFFFFL)
    val d = tintedCache.getOrPut(key) {
        (applicationContext.getDrawable(res)?.mutate() ?: return).apply { setTint(color) }
    }
    d.setBounds(left.toInt(), top.toInt(), (left + size).toInt(), (top + size).toInt())
    d.draw(c)
}

/** A tinted vector icon. Draws at [sizeDp]; the parent may give it a larger touch area. */
class IconView(ctx: Context, icon: Icon, color: Int, private val sizeDp: Int = 24) : View(ctx) {
    var icon: Icon = icon
        set(v) { field = v; invalidate() }

    var color: Int = color
        set(v) { field = v; invalidate() }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = context.dp(sizeDp)
        setMeasuredDimension(resolveSize(s, widthMeasureSpec), resolveSize(s, heightMeasureSpec))
    }

    override fun onDraw(c: Canvas) {
        val s = min(width, height).toFloat()
        context.drawTinted(c, icon.res, color, (width - s) / 2f, (height - s) / 2f, s)
    }

    companion object {
        /** A four-point sparkle, used for decoration and confetti. */
        fun sparkPath(cx: Float, cy: Float, r: Float): Path {
            val k = r * 0.18f
            return Path().apply {
                moveTo(cx, cy - r)
                cubicTo(cx + k, cy - k, cx + k, cy - k, cx + r, cy)
                cubicTo(cx + k, cy + k, cx + k, cy + k, cx, cy + r)
                cubicTo(cx - k, cy + k, cx - k, cy + k, cx - r, cy)
                cubicTo(cx - k, cy - k, cx - k, cy - k, cx, cy - r)
                close()
            }
        }
    }
}

/** A tinted line illustration (decorative, so hidden from accessibility services). */
class ArtView(ctx: Context, private val art: Art, private val color: Int, private val sizeDp: Int = 72) : View(ctx) {
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = context.dp(sizeDp)
        setMeasuredDimension(resolveSize(s, widthMeasureSpec), resolveSize(s, heightMeasureSpec))
    }

    override fun onDraw(c: Canvas) {
        val s = min(width, height).toFloat()
        context.drawTinted(c, art.res, color, (width - s) / 2f, (height - s) / 2f, s)
    }
}

/** Progress bar: 8dp surfaceAlt track with a solid accent value. */
class ProgressLine(ctx: Context, private val p: Pal) : View(ctx) {
    var fraction = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val r = height / 2f
        paint.color = p.surfaceAlt
        c.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, paint)
        paint.color = p.line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        c.drawRoundRect(0.5f, 0.5f, width - 0.5f, height - 0.5f, r, r, paint)
        paint.style = Paint.Style.FILL
        if (fraction > 0f) {
            val w = max(height.toFloat(), width * fraction)
            paint.color = p.accent
            c.drawRoundRect(0f, 0f, w, height.toFloat(), r, r, paint)
        }
    }
}

/**
 * Hero card background: surface with a radial glow from the top right (glow at 16% opacity at the
 * centre, 6% at 48dp, 0% at 112dp), three static four-point sparkles (4, 6 and 8dp) along the
 * outer edge and a 1dp line border.
 */
class GlowDrawable(private val p: Pal, private val radius: Float, private val density: Float, private val seed: Int = 7) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()

    override fun draw(c: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val rf = RectF(b)
        clip.reset()
        clip.addRoundRect(rf, radius, radius, Path.Direction.CW)
        c.save()
        c.clipPath(clip)
        paint.shader = null
        paint.color = p.surface
        c.drawRect(rf, paint)
        val gx = rf.right - 24 * density
        val gy = rf.top + 24 * density
        paint.shader = RadialGradient(
            gx, gy, 112 * density,
            intArrayOf(withAlpha(p.glow, 41), withAlpha(p.glow, 15), withAlpha(p.glow, 0)),
            floatArrayOf(0f, 48f / 112f, 1f), Shader.TileMode.CLAMP,
        )
        c.drawRect(rf, paint)
        paint.shader = null
        // Three sparkles hugging the top-right edge, away from the text column.
        val rnd = Random(seed)
        paint.color = p.glow
        val spots = listOf(
            Triple(rf.right - 20 * density, rf.top + 18 * density, 8f),
            Triple(rf.right - 44 * density, rf.top + 12 * density, 4f),
            Triple(rf.right - 14 * density, rf.top + 46 * density, 6f),
        )
        for ((x, y, d) in spots) {
            val jitter = (rnd.nextFloat() - 0.5f) * 4 * density
            c.drawPath(IconView.sparkPath(x + jitter, y, d * density / 2f), paint)
        }
        c.restore()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = p.line
        val h = density / 2
        c.drawRoundRect(RectF(rf.left + h, rf.top + h, rf.right - h, rf.bottom - h), radius, radius, paint)
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getOutline(outline: android.graphics.Outline) {
        outline.setRoundRect(bounds, radius)
    }
}

/** Page background: linear gradient from bg (0%) to bg2 (100%) at 155°, as in CSS. */
class BackdropDrawable(private val p: Pal) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(c: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        val a = Math.toRadians(155.0)
        val dx = sin(a).toFloat()
        val dy = -cos(a).toFloat()
        val half = (abs(w * dx) + abs(h * dy)) / 2f
        val cx = w / 2f
        val cy = h / 2f
        paint.shader = LinearGradient(cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half, p.bg, p.bg2, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

/** Progress ring: 8dp surfaceAlt track, accent value from −90°, grows over 420ms. */
class RingView(ctx: Context, private val p: Pal, private val sizeDp: Int = 104) : View(ctx) {
    private var target = 0f
    private var shown = 0f
    var center = ""
    var sub = ""
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Fonts.serifBold }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Fonts.sans }

    fun setFraction(f: Float, animate: Boolean = true) {
        target = f.coerceIn(0f, 1f)
        if (!animate) { shown = target; invalidate(); return }
        ValueAnimator.ofFloat(0f, target).apply {
            duration = 420
            interpolator = kitEase()
            addUpdateListener { shown = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = context.dp(sizeDp)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val sw = context.dpf(8)
        val r = RectF(sw / 2, sw / 2, width - sw / 2, height - sw / 2)
        track.strokeWidth = sw
        track.color = p.surfaceAlt
        c.drawOval(r, track)
        if (shown > 0f) {
            track.color = p.accent
            c.drawArc(r, -90f, 360f * shown, false, track)
        }
        big.color = p.ink
        big.textSize = width * 0.2f
        small.color = p.muted
        small.textSize = context.dpf(12) * resources.configuration.fontScale.coerceAtMost(1.3f)
        val y = height / 2f + big.textSize * 0.15f
        c.drawText(center, width / 2f, y, big)
        c.drawText(sub, width / 2f, y + small.textSize * 1.5f, small)
    }
}

/** Sessions per practice: 28 columns in a 96dp plot. Muted bars; the current day is accent and outlined. */
class BarChartView(ctx: Context, private val p: Pal, private val values: IntArray, private val highlight: Int?) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Fonts.sans }
    private var grow = 1f

    init {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 420
            interpolator = kitEase()
            addUpdateListener { grow = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), context.dp(96 + 24))
    }

    override fun onDraw(c: Canvas) {
        val n = values.size
        if (n == 0) return
        label.textSize = context.dpf(11) * resources.configuration.fontScale.coerceAtMost(1.3f)
        label.color = p.muted
        val top = context.dpf(4)
        val bottom = top + context.dpf(96)
        val maxV = max(1, values.maxOrNull() ?: 1)
        val slot = width.toFloat() / n
        val bw = slot * 0.56f
        val minH = context.dpf(3)
        for (i in 0 until n) {
            val x = i * slot + (slot - bw) / 2
            val v = values[i]
            val cur = highlight == i + 1
            val hgt = if (v > 0) max((bottom - top) * v / maxV * grow, bw) else minH
            paint.style = Paint.Style.FILL
            if (cur) {
                // The current day: a full-height accentSoft column with an accent outline marks "today".
                val o = context.dpf(2)
                paint.color = p.accentSoft
                c.drawRoundRect(x - o, top, x + bw + o, bottom + o, bw / 2 + o, bw / 2 + o, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = context.dpf(1.5f)
                paint.color = p.accent
                c.drawRoundRect(x - o, top, x + bw + o, bottom + o, bw / 2 + o, bw / 2 + o, paint)
                paint.style = Paint.Style.FILL
            }
            paint.color = when {
                cur -> p.accent
                v > 0 -> p.muted
                else -> p.surfaceAlt
            }
            if (v > 0 || !cur) c.drawRoundRect(x, bottom - hgt, x + bw, bottom, bw / 2, bw / 2, paint)
            val d = i + 1
            if (d == 1 || d % 7 == 0) c.drawText(d.toString(), x + bw / 2, height - context.dpf(4), label)
        }
    }
}

/**
 * Activity grid: one column per week (Monday on top), from [start] for counts.size / 7 weeks.
 * surfaceAlt for none, then 25/50/75/100% accent; days before install or in the future are outlined
 * only, so they read as unavailable rather than empty. Month names run along the top. Cells have a
 * fixed size, so a long history grows wider and the caller puts it in a horizontal scroller.
 */
class HeatmapView(
    ctx: Context, private val p: Pal, private val counts: IntArray, private val start: java.time.LocalDate,
    private val locale: java.util.Locale, private val cell: Float,
) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Fonts.sans
        textSize = context.dpf(11) * resources.configuration.fontScale.coerceAtMost(1.3f)
        color = p.muted
    }
    private val weeks = counts.size / 7
    private val gap = context.dpf(4)
    private val header = label.textSize + context.dpf(8)
    private val month = java.time.format.DateTimeFormatter.ofPattern("LLL", locale)
    private val monthYear = java.time.format.DateTimeFormatter.ofPattern("LLL yyyy", locale)
    /** Room around the grid so the selection outline isn't clipped. */
    private val pad = context.dpf(4)

    /** Week columns outlined as the chosen period (year, month or week), or null for none. */
    var highlight: IntRange? = null
        set(v) { field = v; invalidate() }

    /** Draw the day of the month inside each square. */
    var showDates = false
        set(v) { field = v; invalidate() }
    private val dayText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Fonts.sans }
    private val dayTextBold = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Fonts.sansBold }

    /** Left edge of a week column, in this view's coordinates. */
    fun weekX(week: Int): Float = pad + week * (cell + gap)
    val columnWidth: Float get() = cell

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = weeks * (cell + gap) - gap + pad * 2
        setMeasuredDimension(kotlin.math.ceil(w).toInt(), kotlin.math.ceil(header + cell * 7 + gap * 6 + pad * 2).toInt())
    }

    override fun onDraw(c: Canvas) {
        val maxV = max(1, counts.maxOrNull() ?: 1)
        val rad = context.dpf(4)
        c.save()
        c.translate(pad, pad)
        // Month labels above the first week that contains the 1st of the month (plus the very first week).
        var lastEnd = -Float.MAX_VALUE
        for (wk in 0 until weeks) {
            val monday = start.plusWeeks(wk.toLong())
            val first = (0 until 7).map { monday.plusDays(it.toLong()) }.firstOrNull { it.dayOfMonth == 1 }
            val d = first ?: if (wk == 0) monday else null
            if (d == null) continue
            val text = (if (d.monthValue == 1 || wk == 0) monthYear else month).format(d)
            val x = wk * (cell + gap)
            if (x < lastEnd + context.dpf(6)) continue
            c.drawText(text, x, label.textSize, label)
            lastEnd = x + label.measureText(text)
        }
        val ts = min(cell * 0.45f, context.dpf(12))
        dayText.textSize = ts
        dayTextBold.textSize = ts
        for (i in counts.indices) {
            val x = (i / 7) * (cell + gap)
            val y = header + (i % 7) * (cell + gap)
            val v = counts[i]
            var step = 0
            if (v < 0) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = context.dpf(1)
                paint.color = p.line
                c.drawRoundRect(x + 0.5f, y + 0.5f, x + cell - 0.5f, y + cell - 0.5f, rad, rad, paint)
                paint.style = Paint.Style.FILL
            } else {
                if (v > 0) step = ((v * 4 + maxV - 1) / maxV).coerceIn(1, 4)
                paint.color = if (v == 0) p.surfaceAlt else blend(p.surfaceAlt, p.accent, step / 4f)
                c.drawRoundRect(x, y, x + cell, y + cell, rad, rad, paint)
            }
            if (showDates) {
                val date = start.plusDays(i.toLong())
                // The 1st of each month is bold so months are easy to find.
                val tp = if (date.dayOfMonth == 1) dayTextBold else dayText
                tp.color = when {
                    v < 0 -> withAlpha(p.muted, 140)
                    step >= 3 -> p.onAccent
                    else -> p.ink
                }
                c.drawText(date.dayOfMonth.toString(), x + cell / 2, y + cell / 2 - (tp.ascent() + tp.descent()) / 2, tp)
            }
        }
        highlight?.let { r ->
            val from = r.first.coerceIn(0, weeks - 1)
            val to = r.last.coerceIn(0, weeks - 1)
            val o = context.dpf(2.5f)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = context.dpf(2)
            paint.color = p.accent
            c.drawRoundRect(from * (cell + gap) - o, header - o, to * (cell + gap) + cell + o, header + cell * 7 + gap * 6 + o,
                rad + o, rad + o, paint)
            paint.style = Paint.Style.FILL
        }
        c.restore()
    }
}

/** Mood after practice (values 1..5): 96dp plot, 2dp accent line, 4dp point markers. */
class LineChartView(ctx: Context, private val p: Pal, private val values: List<Int>) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val emoji = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), context.dp(96 + 16))
    }

    override fun onDraw(c: Canvas) {
        val left = context.dpf(28)
        val top = context.dpf(8)
        val bottom = top + context.dpf(96)
        emoji.textSize = context.dpf(12)
        paint.strokeWidth = context.dpf(1)
        for (m in 1..5) {
            val y = bottom - (bottom - top) * (m - 1) / 4f
            paint.color = p.line
            c.drawLine(left, y, width.toFloat(), y, paint)
            c.drawText(MOODS[m - 1], context.dpf(12), y + context.dpf(4), emoji)
        }
        if (values.isEmpty()) return
        val step = if (values.size > 1) (width - left - context.dpf(8)) / (values.size - 1) else 0f
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = left + context.dpf(4) + i * step
            val y = bottom - (bottom - top) * (v - 1) / 4f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = context.dpf(2)
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = p.accent
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        values.forEachIndexed { i, v ->
            val x = left + context.dpf(4) + i * step
            val y = bottom - (bottom - top) * (v - 1) / 4f
            c.drawCircle(x, y, context.dpf(2), paint)
        }
    }
}

/**
 * Celebration burst: Canvas particles fly outward from the centre of the card and fade.
 * Day: 24 particles, 640ms, 48–112dp. Journey: 40 particles, 880ms, 64–144dp.
 * Uses ValueAnimator, so the system animator duration scale (including "off") is respected.
 */
class ConfettiView(ctx: Context, private val colors: IntArray) : View(ctx) {
    private class Bit(val angle: Float, val dist: Float, val size: Float, val spin: Float, val color: Int, val shape: Int)

    private val bits = ArrayList<Bit>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    var originY = -1f

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun burst(big: Boolean) {
        if (width == 0) { post { burst(big) }; return }
        val d = resources.displayMetrics.density
        val count = if (big) 40 else 24
        val (minD, maxD) = if (big) 64f to 144f else 48f to 112f
        bits.clear()
        repeat(count) { i ->
            bits += Bit(
                angle = (i.toFloat() / count) * 2f * PI.toFloat() + Random.nextFloat() * 0.3f,
                dist = (minD + Random.nextFloat() * (maxD - minD)) * d,
                size = (4 + Random.nextFloat() * 4) * d,
                spin = (Random.nextFloat() * 2f - 1f) * 120f,
                color = colors[i % colors.size],
                shape = i % 3,
            )
        }
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (big) 880 else 640
            interpolator = kitEase()
            addUpdateListener { t = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { bits.clear(); invalidate() }
            })
            start()
        }
    }

    override fun onDraw(c: Canvas) {
        if (bits.isEmpty()) return
        val cx = width / 2f
        val cy = if (originY >= 0f) originY else height / 2f
        paint.alpha = 255
        for (b in bits) {
            paint.color = withAlpha(b.color, (255 * (1f - t)).toInt())
            c.save()
            c.translate(cx + cos(b.angle) * b.dist * t, cy + sin(b.angle) * b.dist * t)
            c.rotate(b.spin * t)
            val s = b.size
            when (b.shape) {
                0 -> c.drawRect(-s / 2, -s / 4, s / 2, s / 4, paint)
                1 -> c.drawCircle(0f, 0f, s / 2, paint)
                else -> c.drawPath(IconView.sparkPath(0f, 0f, s / 2 + 1), paint)
            }
            c.restore()
        }
    }
}

/** The day / night switch: 56×32 accentSoft track, 24dp surface knob with an ink sun or moon, 56×48 touch target. */
class DayNightToggle(ctx: Context, private val p: Pal, private var isDark: Boolean, private val onToggle: (Boolean) -> Unit) : View(ctx) {
    private var pos = if (isDark) 1f else 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener {
            isDark = !isDark
            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            ValueAnimator.ofFloat(pos, if (isDark) 1f else 0f).apply {
                duration = 180
                interpolator = kitEase()
                addUpdateListener { a -> pos = a.animatedValue as Float; invalidate() }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) { onToggle(isDark) }
                })
                start()
            }
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: android.view.accessibility.AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = android.widget.Switch::class.java.name
        info.isCheckable = true
        @Suppress("DEPRECATION")
        info.isChecked = isDark
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(context.dp(56), context.dp(48))
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val th = context.dpf(32)
        val top = (height - th) / 2f
        val r = th / 2
        paint.color = p.accentSoft
        c.drawRoundRect(0f, top, w, top + th, r, r, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = context.dpf(1)
        paint.color = p.line
        c.drawRoundRect(0.5f, top + 0.5f, w - 0.5f, top + th - 0.5f, r, r, paint)
        paint.style = Paint.Style.FILL
        val inset = context.dpf(4)
        val knob = context.dpf(24)
        val kx = inset + (w - 2 * inset - knob) * pos
        val ky = top + inset
        paint.color = p.surface
        c.drawCircle(kx + knob / 2, ky + knob / 2, knob / 2, paint)
        val icon = context.dpf(16)
        context.drawTinted(c, if (pos < 0.5f) Icon.SUN.res else Icon.MOON.res, p.ink, kx + (knob - icon) / 2, ky + (knob - icon) / 2, icon)
    }
}

/**
 * Day badge: a 32dp circle inside a larger touch area.
 * Done: accent with an onAccent tick. Current: 2dp accent ring with an 8dp glow.
 * Upcoming: surface with a muted outline. Resting: like upcoming, with a small lock.
 */
class DayBadge(
    ctx: Context, private val p: Pal, private val day: Int, private val status: DayStatus,
    private val sizeDp: Int = 48, private val resting: Boolean = false,
) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Fonts.sansMedium }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = context.dp(sizeDp)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(context.dpf(16), min(cx, cy) - context.dpf(1))
        txt.textSize = context.dpf(13)
        val ty = cy - (txt.ascent() + txt.descent()) / 2
        paint.style = Paint.Style.FILL
        when {
            status == DayStatus.DONE -> {
                paint.color = p.accent
                c.drawCircle(cx, cy, r, paint)
                val s = r * 1.1f
                context.drawTinted(c, Icon.CHECK.res, p.onAccent, cx - s / 2, cy - s / 2, s)
            }
            status == DayStatus.CURRENT && !resting -> {
                val g = r + context.dpf(8)
                paint.shader = RadialGradient(cx, cy, g, intArrayOf(withAlpha(p.glow, 41), withAlpha(p.glow, 41), withAlpha(p.glow, 0)),
                    floatArrayOf(0f, r / g, 1f), Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, g, paint)
                paint.shader = null
                paint.color = p.surface
                c.drawCircle(cx, cy, r, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = context.dpf(2)
                paint.color = p.accent
                c.drawCircle(cx, cy, r - context.dpf(1), paint)
                txt.color = p.ink
                c.drawText(day.toString(), cx, ty, txt)
            }
            else -> {
                paint.color = p.surface
                c.drawCircle(cx, cy, r, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = context.dpf(1)
                paint.color = p.muted
                c.drawCircle(cx, cy, r - context.dpf(0.5f), paint)
                txt.color = p.muted
                c.drawText(day.toString(), cx, ty, txt)
                if (resting) {
                    val ls = context.dpf(14)
                    val lx = cx + r * 0.62f - ls / 2
                    val ly = cy + r * 0.62f - ls / 2
                    paint.style = Paint.Style.FILL
                    paint.color = p.accentSoft
                    c.drawCircle(lx + ls / 2, ly + ls / 2, ls / 2 + context.dpf(1), paint)
                    context.drawTinted(c, Icon.LOCK.res, p.ink, lx + ls * 0.15f, ly + ls * 0.15f, ls * 0.7f)
                }
            }
        }
        paint.style = Paint.Style.FILL
    }
}

/**
 * On/off switch: 56×32 track inside a 56×48 target, 24dp knob with 4dp inset; the knob moves in 180ms.
 * Off: surfaceAlt track with a muted outline and muted knob. On: accent track, onAccent knob with a tick.
 */
class KitSwitch(
    ctx: Context, private val p: Pal, checked: Boolean,
    /** Compact variant (36×20 track, 14dp knob) for use beside headings; the touch target stays 48dp tall. */
    private val small: Boolean = false,
    private val onChange: (Boolean) -> Unit,
) : View(ctx) {
    private val trackW get() = context.dpf(if (small) 36 else 56)
    private val trackH get() = context.dpf(if (small) 20 else 32)
    private val inset get() = context.dpf(if (small) 3 else 4)
    private val knob get() = context.dpf(if (small) 14 else 24)
    private val tick get() = context.dpf(if (small) 9 else 14)

    var isOn = checked
        private set
    private var pos = if (checked) 1f else 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener {
            isOn = !isOn
            ValueAnimator.ofFloat(pos, if (isOn) 1f else 0f).apply {
                duration = 180
                interpolator = kitEase()
                addUpdateListener { a -> pos = a.animatedValue as Float; invalidate() }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) { onChange(isOn) }
                })
                start()
            }
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: android.view.accessibility.AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = android.widget.Switch::class.java.name
        info.isCheckable = true
        @Suppress("DEPRECATION")
        info.isChecked = isOn
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(context.dp(if (small) 40 else 56), context.dp(48))
    }

    override fun onDraw(c: Canvas) {
        // The track sits at the right edge of the view (the extra width is touch area).
        c.save()
        c.translate(width - trackW, 0f)
        val w = trackW
        val th = trackH
        val top = (height - th) / 2f
        val r = th / 2
        paint.style = Paint.Style.FILL
        paint.color = blend(p.surfaceAlt, p.accent, pos)
        c.drawRoundRect(0f, top, w, top + th, r, r, paint)
        if (pos < 1f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = context.dpf(1)
            paint.color = withAlpha(p.muted, (255 * (1f - pos)).toInt())
            c.drawRoundRect(0.5f, top + 0.5f, w - 0.5f, top + th - 0.5f, r, r, paint)
            paint.style = Paint.Style.FILL
        }
        val kx = inset + (w - 2 * inset - knob) * pos
        val ky = top + inset
        paint.color = blend(p.muted, p.onAccent, pos)
        c.drawCircle(kx + knob / 2, ky + knob / 2, knob / 2, paint)
        if (pos > 0.5f) {
            val s = tick
            context.drawTinted(c, Icon.CHECK.res, p.accent, kx + (knob - s) / 2, ky + (knob - s) / 2, s)
        }
        c.restore()
    }
}
