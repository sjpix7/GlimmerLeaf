package com.sjpixels.glimmerleaf

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class Icon {
    PLAY, PAUSE, STOP, PREV, NEXT, BACK, SUN, MOON, TRASH, SHARE, CHECK, LOCK, HOME, BOOK, HISTORY, CHART,
    GEAR, SPARK, CHEVRON, CHEVRON_DOWN, BELL, HEART, SPEAKER
}

/** Hand-drawn line icons on a 24-unit grid, tinted with any colour. */
class IconView(ctx: Context, icon: Icon, color: Int, private val sizeDp: Int = 24) : View(ctx) {
    var icon: Icon = icon
        set(v) { field = v; invalidate() }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 2f
        this.color = color
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    var color: Int
        get() = stroke.color
        set(v) { stroke.color = v; fill.color = v; invalidate() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = context.dp(sizeDp)
        setMeasuredDimension(resolveSize(s, widthMeasureSpec), resolveSize(s, heightMeasureSpec))
    }

    override fun onDraw(c: Canvas) {
        val s = min(width, height) / 24f
        c.save()
        c.translate((width - 24 * s) / 2f, (height - 24 * s) / 2f)
        c.scale(s, s)
        stroke.strokeWidth = 2f
        drawIcon(c, icon, stroke, fill)
        c.restore()
    }

    companion object {
        private fun p(block: Path.() -> Unit) = Path().apply(block)

        fun drawIcon(c: Canvas, icon: Icon, st: Paint, fi: Paint) {
            when (icon) {
                Icon.PLAY -> c.drawPath(p { moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close() }, fi)
                Icon.PAUSE -> {
                    c.drawRoundRect(RectF(6.5f, 5f, 10f, 19f), 1.2f, 1.2f, fi)
                    c.drawRoundRect(RectF(14f, 5f, 17.5f, 19f), 1.2f, 1.2f, fi)
                }
                Icon.STOP -> c.drawRoundRect(RectF(6.5f, 6.5f, 17.5f, 17.5f), 2f, 2f, fi)
                Icon.PREV -> {
                    c.drawPath(p { moveTo(18f, 6f); lineTo(9.5f, 12f); lineTo(18f, 18f); close() }, fi)
                    c.drawLine(7f, 6f, 7f, 18f, st)
                }
                Icon.NEXT -> {
                    c.drawPath(p { moveTo(6f, 6f); lineTo(14.5f, 12f); lineTo(6f, 18f); close() }, fi)
                    c.drawLine(17f, 6f, 17f, 18f, st)
                }
                Icon.BACK -> c.drawPath(p { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }, st)
                Icon.CHEVRON -> c.drawPath(p { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }, st)
                Icon.CHEVRON_DOWN -> c.drawPath(p { moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f) }, st)
                Icon.SUN -> {
                    c.drawCircle(12f, 12f, 4f, st)
                    for (i in 0 until 8) {
                        val a = i * PI / 4
                        c.drawLine(
                            12f + 7f * cos(a).toFloat(), 12f + 7f * sin(a).toFloat(),
                            12f + 9.5f * cos(a).toFloat(), 12f + 9.5f * sin(a).toFloat(), st
                        )
                    }
                }
                Icon.MOON -> {
                    val a = Path().apply { addCircle(12f, 12f, 8f, Path.Direction.CW) }
                    val b = Path().apply { addCircle(16.5f, 8.5f, 7f, Path.Direction.CW) }
                    a.op(b, Path.Op.DIFFERENCE)
                    c.drawPath(a, fi)
                }
                Icon.TRASH -> {
                    c.drawLine(4.5f, 7f, 19.5f, 7f, st)
                    c.drawLine(10f, 4f, 14f, 4f, st)
                    c.drawPath(p { moveTo(6.5f, 7f); lineTo(7.5f, 20f); lineTo(16.5f, 20f); lineTo(17.5f, 7f) }, st)
                    c.drawLine(10f, 11f, 10f, 16f, st)
                    c.drawLine(14f, 11f, 14f, 16f, st)
                }
                Icon.SHARE -> {
                    c.drawCircle(6f, 12f, 2.5f, st)
                    c.drawCircle(17.5f, 6f, 2.5f, st)
                    c.drawCircle(17.5f, 18f, 2.5f, st)
                    c.drawLine(8.3f, 10.8f, 15.2f, 7.2f, st)
                    c.drawLine(8.3f, 13.2f, 15.2f, 16.8f, st)
                }
                Icon.CHECK -> c.drawPath(p { moveTo(5f, 12.5f); lineTo(10f, 17f); lineTo(19f, 7f) }, st)
                Icon.LOCK -> {
                    c.drawRoundRect(RectF(5.5f, 11f, 18.5f, 20f), 2f, 2f, st)
                    c.drawPath(p { moveTo(8.5f, 11f); lineTo(8.5f, 8f); arcTo(RectF(8.5f, 4.5f, 15.5f, 11.5f), 180f, 180f); lineTo(15.5f, 11f) }, st)
                }
                Icon.HOME -> {
                    c.drawPath(p { moveTo(3.5f, 11.5f); lineTo(12f, 4f); lineTo(20.5f, 11.5f) }, st)
                    c.drawPath(p { moveTo(6f, 10f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 10f) }, st)
                }
                Icon.BOOK -> {
                    c.drawPath(p {
                        moveTo(12f, 6.5f)
                        cubicTo(10f, 5f, 6f, 4.8f, 3f, 5.6f); lineTo(3f, 19f)
                        cubicTo(6f, 18.2f, 10f, 18.4f, 12f, 19.8f)
                        cubicTo(14f, 18.4f, 18f, 18.2f, 21f, 19f); lineTo(21f, 5.6f)
                        cubicTo(18f, 4.8f, 14f, 5f, 12f, 6.5f); close()
                    }, st)
                    c.drawLine(12f, 6.5f, 12f, 19.8f, st)
                }
                Icon.HISTORY -> {
                    c.drawCircle(12f, 12f, 8.5f, st)
                    c.drawPath(p { moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f) }, st)
                }
                Icon.CHART -> {
                    c.drawLine(5f, 20f, 5f, 13f, st)
                    c.drawLine(10f, 20f, 10f, 8f, st)
                    c.drawLine(15f, 20f, 15f, 11f, st)
                    c.drawLine(20f, 20f, 20f, 5f, st)
                }
                Icon.GEAR -> {
                    c.drawCircle(12f, 12f, 3f, st)
                    c.drawCircle(12f, 12f, 6.5f, st)
                    for (i in 0 until 8) {
                        val a = i * PI / 4
                        c.drawLine(
                            12f + 6.5f * cos(a).toFloat(), 12f + 6.5f * sin(a).toFloat(),
                            12f + 9.3f * cos(a).toFloat(), 12f + 9.3f * sin(a).toFloat(), st
                        )
                    }
                }
                Icon.SPARK -> c.drawPath(sparkPath(12f, 12f, 10f), fi)
                Icon.BELL -> {
                    c.drawPath(p {
                        moveTo(6f, 16.5f); lineTo(6f, 11f)
                        arcTo(RectF(6f, 5f, 18f, 17f), 180f, 180f)
                        lineTo(18f, 16.5f); lineTo(19.5f, 18f); lineTo(4.5f, 18f); close()
                    }, st)
                    c.drawLine(10f, 21f, 14f, 21f, st)
                }
                Icon.HEART -> c.drawPath(heartPath(), fi)
                Icon.SPEAKER -> {
                    c.drawPath(p { moveTo(4f, 9.5f); lineTo(8f, 9.5f); lineTo(12.5f, 5.5f); lineTo(12.5f, 18.5f); lineTo(8f, 14.5f); lineTo(4f, 14.5f); close() }, fi)
                    c.drawArc(RectF(11f, 8.5f, 18f, 15.5f), -55f, 110f, false, st)
                    c.drawArc(RectF(11f, 5f, 21.5f, 19f), -55f, 110f, false, st)
                }
            }
        }

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

        fun heartPath(): Path = Path().apply {
            moveTo(12f, 20f)
            cubicTo(5f, 15f, 3f, 11.5f, 3f, 8.5f)
            cubicTo(3f, 6f, 5f, 4f, 7.5f, 4f)
            cubicTo(9.3f, 4f, 11f, 5f, 12f, 6.8f)
            cubicTo(13f, 5f, 14.7f, 4f, 16.5f, 4f)
            cubicTo(19f, 4f, 21f, 6f, 21f, 8.5f)
            cubicTo(21f, 11.5f, 19f, 15f, 12f, 20f)
            close()
        }
    }
}

/** Thin rounded progress bar. */
class ProgressLine(ctx: Context, private val p: Pal) : View(ctx) {
    var fraction = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val r = height / 2f
        paint.shader = null
        paint.color = withAlpha(p.ink, if (p.dark) 40 else 22)
        c.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, paint)
        if (fraction > 0f) {
            val w = max(height.toFloat(), width * fraction)
            paint.shader = LinearGradient(0f, 0f, w, 0f, p.accent, blend(p.accent, p.glow, 0.55f), Shader.TileMode.CLAMP)
            c.drawRoundRect(0f, 0f, w, height.toFloat(), r, r, paint)
        }
    }
}

/** Card background with the warm parchment glow and scattered sparkles. */
class GlowDrawable(private val p: Pal, private val radius: Float, private val density: Float, private val seed: Int = 7) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()

    override fun draw(c: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val rf = RectF(b)
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        clip.reset()
        clip.addRoundRect(rf, radius, radius, Path.Direction.CW)
        c.save()
        c.clipPath(clip)
        paint.shader = LinearGradient(rf.left, rf.top, rf.right, rf.bottom,
            blend(p.surface, p.accentSoft, 0.25f), blend(p.surface, p.accentSoft, 0.9f), Shader.TileMode.CLAMP)
        c.drawRect(rf, paint)
        paint.shader = RadialGradient(rf.left + w * 0.78f, rf.top + h * 0.18f, max(w, h) * 0.7f,
            withAlpha(p.glow, if (p.dark) 120 else 170), withAlpha(p.glow, 0), Shader.TileMode.CLAMP)
        c.drawRect(rf, paint)
        paint.shader = null
        val rnd = Random(seed)
        repeat(30) { i ->
            val x = rf.left + rnd.nextFloat() * w
            val y = rf.top + rnd.nextFloat() * h
            val near = 1f - (kotlin.math.abs(x - (rf.left + w * 0.78f)) / w)
            paint.color = withAlpha(if (p.dark) p.glow else blend(p.glow, p.accent, 0.3f), (60 + 150 * near * rnd.nextFloat()).toInt())
            val r = (0.8f + rnd.nextFloat() * 1.8f) * density
            if (i % 6 == 0) c.drawPath(IconView.sparkPath(x, y, r * 3.2f), paint) else c.drawCircle(x, y, r, paint)
        }
        c.restore()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = withAlpha(p.glow, if (p.dark) 70 else 110)
        c.drawRoundRect(RectF(rf.left + density / 2, rf.top + density / 2, rf.right - density / 2, rf.bottom - density / 2), radius, radius, paint)
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

/** App background: soft vertical gradient with a candle-like glow at the top. */
class BackdropDrawable(private val p: Pal) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(c: Canvas) {
        val b: Rect = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        paint.shader = LinearGradient(0f, 0f, 0f, h, p.bg, blend(p.bg, p.bg2, 0.75f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = RadialGradient(w * 0.5f, h * 0.02f, max(w, 1f) * 0.9f,
            withAlpha(p.glow, if (p.dark) 46 else 70), withAlpha(p.glow, 0), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

/** Circular progress ring with a big number in the middle. */
class RingView(ctx: Context, private val p: Pal, private val sizeDp: Int) : View(ctx) {
    private var target = 0f
    private var shown = 0f
    var center = ""
    var sub = ""
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Fonts.serifBold }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Fonts.sans }

    fun setFraction(f: Float, animate: Boolean = true) {
        target = f.coerceIn(0f, 1f)
        if (!animate) { shown = target; invalidate(); return }
        ValueAnimator.ofFloat(0f, target).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            addUpdateListener { shown = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = context.dp(sizeDp)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val sw = width * 0.085f
        val r = RectF(sw, sw, width - sw, height - sw)
        track.strokeWidth = sw
        track.color = withAlpha(p.ink, if (p.dark) 38 else 20)
        c.drawOval(r, track)
        arc.strokeWidth = sw
        arc.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), p.accent, blend(p.accent, p.glow, 0.6f), Shader.TileMode.CLAMP)
        if (shown > 0f) c.drawArc(r, -90f, 360f * shown, false, arc)
        big.color = p.ink
        big.textSize = width * 0.24f
        small.color = p.muted
        small.textSize = width * 0.085f
        c.drawText(center, width / 2f, height / 2f + big.textSize * 0.2f, big)
        c.drawText(sub, width / 2f, height / 2f + big.textSize * 0.2f + small.textSize * 1.7f, small)
    }
}

/** Vertical bars, one per practice day. */
class BarChartView(ctx: Context, private val p: Pal, private val values: IntArray, private val highlight: Int?) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Fonts.sans }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), context.dp(150))
    }

    override fun onDraw(c: Canvas) {
        val n = values.size
        if (n == 0) return
        label.textSize = context.dpf(10.5f)
        label.color = p.muted
        val bottom = height - context.dpf(18)
        val top = context.dpf(14)
        val maxV = max(1, values.maxOrNull() ?: 1)
        val slot = width.toFloat() / n
        val bw = slot * 0.62f
        for (i in 0 until n) {
            val x = i * slot + (slot - bw) / 2
            val v = values[i]
            paint.color = withAlpha(p.ink, if (p.dark) 30 else 16)
            c.drawRoundRect(x, top, x + bw, bottom, bw / 2, bw / 2, paint)
            if (v > 0) {
                val hgt = (bottom - top) * v / maxV
                paint.color = if (highlight == i + 1) p.glow else p.accent
                c.drawRoundRect(x, bottom - max(hgt, bw), x + bw, bottom, bw / 2, bw / 2, paint)
            }
            val d = i + 1
            if (d == 1 || d % 7 == 0) c.drawText(d.toString(), x + bw / 2, height - context.dpf(3), label)
        }
        label.color = p.muted
        label.textAlign = Paint.Align.LEFT
        c.drawText("max $maxV", 0f, context.dpf(10), label)
        label.textAlign = Paint.Align.CENTER
    }
}

/** GitHub-style activity grid: 12 weeks x 7 days. */
class HeatmapView(ctx: Context, private val p: Pal, private val counts: IntArray) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val weeks = counts.size / 7

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val gap = context.dpf(4)
        val cell = (w - gap * (weeks - 1)) / weeks
        setMeasuredDimension(w, (cell * 7 + gap * 6).toInt() + 1)
    }

    override fun onDraw(c: Canvas) {
        val gap = context.dpf(4)
        val cell = (width - gap * (weeks - 1)) / weeks
        val maxV = max(1, counts.maxOrNull() ?: 1)
        for (i in counts.indices) {
            val wk = i / 7
            val dow = i % 7
            val x = wk * (cell + gap)
            val y = dow * (cell + gap)
            val v = counts[i]
            paint.color = when {
                v < 0 -> withAlpha(p.ink, 0)
                v == 0 -> withAlpha(p.ink, if (p.dark) 28 else 14)
                else -> blend(p.accentSoft, p.accent, 0.35f + 0.65f * v / maxV)
            }
            c.drawRoundRect(x, y, x + cell, y + cell, cell * 0.28f, cell * 0.28f, paint)
        }
    }
}

/** Mood line chart (values 1..5). */
class LineChartView(ctx: Context, private val p: Pal, private val values: List<Int>) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val emoji = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), context.dp(140))
    }

    override fun onDraw(c: Canvas) {
        val left = context.dpf(26)
        val top = context.dpf(10)
        val bottom = height - context.dpf(10)
        emoji.textSize = context.dpf(12)
        for (m in 1..5) {
            val y = bottom - (bottom - top) * (m - 1) / 4f
            paint.color = withAlpha(p.ink, if (p.dark) 30 else 18)
            paint.strokeWidth = context.dpf(1)
            c.drawLine(left, y, width.toFloat(), y, paint)
            c.drawText(MOODS[m - 1], context.dpf(11), y + context.dpf(4), emoji)
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
        paint.strokeWidth = context.dpf(2.5f)
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = p.accent
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        values.forEachIndexed { i, v ->
            val x = left + context.dpf(4) + i * step
            val y = bottom - (bottom - top) * (v - 1) / 4f
            paint.color = p.surface
            c.drawCircle(x, y, context.dpf(4.5f), paint)
            paint.color = p.accent
            c.drawCircle(x, y, context.dpf(3f), paint)
        }
    }
}

/** Celebration confetti: ribbons, dots and little stars falling with gravity. */
class ConfettiView(ctx: Context, private val colors: IntArray) : View(ctx) {
    private class Bit(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var rot: Float, val vr: Float, val w: Float, val h: Float, val color: Int, val shape: Int,
    )

    private val bits = ArrayList<Bit>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var last = 0L

    fun burst(count: Int = 150) {
        if (width == 0) { post { burst(count) }; return }
        val d = context.resources.displayMetrics.density
        repeat(count) {
            val fromLeft = it % 2 == 0
            val x = if (fromLeft) width * 0.1f else width * 0.9f
            val angle = if (fromLeft) Random.nextDouble(-80.0, -30.0) else Random.nextDouble(-150.0, -100.0)
            val speed = Random.nextDouble(700.0, 1500.0).toFloat() * d / 2.6f
            bits += Bit(
                x = x, y = height * 0.62f,
                vx = (cos(Math.toRadians(angle)) * speed).toFloat(),
                vy = (sin(Math.toRadians(angle)) * speed).toFloat(),
                rot = Random.nextFloat() * 360f, vr = Random.nextFloat() * 600f - 300f,
                w = (5 + Random.nextFloat() * 6) * d, h = (8 + Random.nextFloat() * 8) * d,
                color = colors[Random.nextInt(colors.size)], shape = Random.nextInt(3),
            )
        }
        repeat(count / 3) {
            bits += Bit(
                x = Random.nextFloat() * width, y = -Random.nextFloat() * height * 0.5f,
                vx = Random.nextFloat() * 60f - 30f, vy = Random.nextFloat() * 120f + 60f,
                rot = Random.nextFloat() * 360f, vr = Random.nextFloat() * 400f - 200f,
                w = (5 + Random.nextFloat() * 5) * d, h = (8 + Random.nextFloat() * 6) * d,
                color = colors[Random.nextInt(colors.size)], shape = Random.nextInt(3),
            )
        }
        last = 0L
        postInvalidateOnAnimation()
    }

    override fun onDraw(c: Canvas) {
        val now = SystemClock.uptimeMillis()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1000f).coerceAtMost(0.05f)
        last = now
        val d = context.resources.displayMetrics.density
        val g = 900f * d / 2.6f
        val it = bits.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.vy += g * dt
            b.vx *= 0.99f
            b.vy = min(b.vy, 520f * d / 2.6f)
            b.x += b.vx * dt + sin((now / 300.0) + b.rot).toFloat() * 0.6f
            b.y += b.vy * dt
            b.rot += b.vr * dt
            if (b.y > height + 40 * d) { it.remove(); continue }
            paint.color = b.color
            c.save()
            c.translate(b.x, b.y)
            c.rotate(b.rot)
            when (b.shape) {
                0 -> c.drawRect(-b.w / 2, -b.h / 2, b.w / 2, b.h / 2, paint)
                1 -> c.drawCircle(0f, 0f, b.w / 2, paint)
                else -> c.drawPath(IconView.sparkPath(0f, 0f, b.h * 0.6f), paint)
            }
            c.restore()
        }
        if (bits.isNotEmpty()) postInvalidateOnAnimation()
    }
}

/** The day / night switch shown at the top right of every screen. */
class DayNightToggle(ctx: Context, private val p: Pal, private var isDark: Boolean, private val onToggle: (Boolean) -> Unit) : View(ctx) {
    private var pos = if (isDark) 1f else 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val st = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 2.2f
    }
    private val fi = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "Day / night"
        setOnClickListener {
            isDark = !isDark
            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            ValueAnimator.ofFloat(pos, if (isDark) 1f else 0f).apply {
                duration = 260
                interpolator = DecelerateInterpolator()
                addUpdateListener { a -> pos = a.animatedValue as Float; invalidate() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onToggle(isDark)
                    }
                })
                start()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(context.dp(58), context.dp(32))
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2
        paint.color = blend(blend(p.accentSoft, p.glow, 0.25f), withAlpha(0xFF1E1A2E.toInt(), 255), pos)
        c.drawRoundRect(0f, 0f, w, h, r, r, paint)
        // tiny stars on the night side
        paint.color = withAlpha(0xFFFFFFFF.toInt(), (180 * pos).toInt())
        c.drawCircle(w * 0.25f, h * 0.32f, h * 0.05f, paint)
        c.drawCircle(w * 0.36f, h * 0.66f, h * 0.035f, paint)
        c.drawCircle(w * 0.17f, h * 0.62f, h * 0.03f, paint)
        val knobR = r - context.dpf(3)
        val cx = r + (w - 2 * r) * pos
        paint.color = blend(0xFFFFFBF0.toInt(), 0xFF3A3552.toInt(), pos)
        c.drawCircle(cx, r, knobR, paint)
        c.save()
        val s = knobR * 1.25f / 12f
        c.translate(cx - 12 * s, r - 12 * s)
        c.scale(s, s)
        if (pos < 0.5f) {
            st.color = 0xFFE09A2D.toInt(); fi.color = st.color
            IconView.drawIcon(c, Icon.SUN, st, fi)
        } else {
            fi.color = 0xFFF5E6B0.toInt(); st.color = fi.color
            IconView.drawIcon(c, Icon.MOON, st, fi)
        }
        c.restore()
    }
}

/** Numbered day circle in the table of contents: done (filled with tick), current (glowing ring) or upcoming. */
class DayBadge(ctx: Context, private val p: Pal, private val day: Int, private val status: DayStatus, private val sizeDp: Int = 42) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create("serif", Typeface.BOLD) }
    private val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; strokeWidth = 2.6f }
    private val fi = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = context.dp(sizeDp)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) - context.dpf(2)
        txt.textSize = r * 0.82f
        when (status) {
            DayStatus.DONE -> {
                paint.style = Paint.Style.FILL
                paint.color = p.accent
                c.drawCircle(cx, cy, r, paint)
                c.save()
                val s = r * 1.1f / 12f
                c.translate(cx - 12 * s, cy - 12 * s)
                c.scale(s, s)
                st.color = p.onAccent
                IconView.drawIcon(c, Icon.CHECK, st, fi)
                c.restore()
            }
            DayStatus.CURRENT -> {
                paint.style = Paint.Style.FILL
                paint.shader = RadialGradient(cx, cy, r * 1.4f, withAlpha(p.glow, 150), withAlpha(p.glow, 0), Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, r, paint)
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = context.dpf(2.5f)
                paint.color = p.accent
                c.drawCircle(cx, cy, r - context.dpf(1.2f), paint)
                txt.color = p.accent
                c.drawText(day.toString(), cx, cy + txt.textSize * 0.35f, txt)
            }
            else -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = context.dpf(1.5f)
                paint.color = p.line
                c.drawCircle(cx, cy, r - context.dpf(1f), paint)
                txt.color = p.muted
                c.drawText(day.toString(), cx, cy + txt.textSize * 0.35f, txt)
            }
        }
        paint.style = Paint.Style.FILL
    }
}
