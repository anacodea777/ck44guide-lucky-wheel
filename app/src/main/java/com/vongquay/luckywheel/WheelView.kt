package com.vongquay.luckywheel

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.PI
import kotlin.math.min
import kotlin.random.Random

/**
 * A spinning wheel drawn on a Canvas. The pointer sits at 12 o'clock; segment 0
 * starts at the pointer when [rotationDeg] is 0 and segments run clockwise.
 */
class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var items: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    /** Called with the index of the winning item when a spin finishes. */
    var onSpinEnd: ((Int) -> Unit)? = null

    /** Called each time a segment boundary passes the pointer. */
    var onSegmentTick: (() -> Unit)? = null

    var isSpinning = false
        private set

    private var rotationDeg = 0f
    private var animator: ValueAnimator? = null
    private var lastTickIndex = -1

    private val palette = intArrayOf(
        0xFFE53935.toInt(), 0xFFFB8C00.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt(),
        0xFF00ACC1.toInt(), 0xFF1E88E5.toInt(), 0xFF5E35B1.toInt(), 0xFFD81B60.toInt(),
    )

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dp(2f)
    }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFC107.toInt()
        strokeWidth = dp(8f)
    }
    private val rimDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFC107.toInt()
        strokeWidth = dp(4f)
    }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC62828.toInt() }
    private val pointerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dp(2.5f)
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
        setShadowLayer(dp(2f), 0f, dp(1f), 0x66000000)
    }

    private val arcRect = RectF()
    private val pointerPath = Path()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) w else min(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val n = items.size
        if (n == 0) return

        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) - dp(10f)
        val sweep = 360f / n
        arcRect.set(cx - r, cy - r, cx + r, cy + r)

        // Segments and labels.
        val textSize = min(r * 0.12f, (2f * PI.toFloat() * r * 0.62f / n) * 0.6f).coerceAtLeast(dp(10f))
        textPaint.textSize = textSize
        val textMax = r * 0.62f
        for (i in 0 until n) {
            val start = -90f + rotationDeg + i * sweep
            segmentPaint.color = colorFor(i, n)
            canvas.drawArc(arcRect, start, sweep, true, segmentPaint)
            if (n > 1) canvas.drawArc(arcRect, start, sweep, true, dividerPaint)

            val label = TextUtils.ellipsize(items[i], textPaint, textMax, TextUtils.TruncateAt.END)
            val mid = start + sweep / 2f
            val midNorm = ((mid % 360f) + 360f) % 360f
            // Keep labels upright: on the left half, flip them and read from the rim inwards.
            val flip = midNorm > 90f && midNorm < 270f
            canvas.save()
            canvas.rotate(if (flip) mid + 180f else mid, cx, cy)
            val baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            textPaint.textAlign = if (flip) Paint.Align.LEFT else Paint.Align.RIGHT
            val x = if (flip) cx - r * 0.88f else cx + r * 0.88f
            canvas.drawText(label, 0, label.length, x, baseline, textPaint)
            canvas.restore()
        }

        // Rim with light bulbs.
        canvas.drawCircle(cx, cy, r, rimPaint)
        val dots = 24
        for (d in 0 until dots) {
            canvas.save()
            canvas.rotate(d * 360f / dots + rotationDeg, cx, cy)
            canvas.drawCircle(cx, cy - r, dp(2.5f), rimDotPaint)
            canvas.restore()
        }

        // Hub.
        val hubR = r * 0.13f
        canvas.drawCircle(cx, cy, hubR, hubPaint)
        canvas.drawCircle(cx, cy, hubR, hubRingPaint)

        // Pointer at 12 o'clock, pointing down into the wheel.
        val pw = r * 0.09f
        val top = cy - r - dp(8f)
        pointerPath.reset()
        pointerPath.moveTo(cx - pw, top)
        pointerPath.lineTo(cx + pw, top)
        pointerPath.lineTo(cx, cy - r + r * 0.16f)
        pointerPath.close()
        canvas.drawPath(pointerPath, pointerPaint)
        canvas.drawPath(pointerPath, pointerStroke)
    }

    private fun colorFor(i: Int, n: Int): Int {
        val size = palette.size
        // Avoid the last segment sharing a colour with segment 0 next to it.
        if (n > 1 && i == n - 1 && i % size == 0) return palette[(i + 2) % size]
        return palette[i % size]
    }

    /** Index of the segment currently under the pointer. */
    private fun indexAt(rotation: Float): Int {
        val n = items.size
        if (n == 0) return -1
        val angle = ((-rotation % 360f) + 360f) % 360f
        return (angle / (360f / n)).toInt().coerceIn(0, n - 1)
    }

    fun spin(random: Random = Random.Default) {
        val n = items.size
        if (isSpinning || n < 2) return

        val sweep = 360f / n
        val target = random.nextInt(n)
        // Land somewhere inside the target segment, not always dead centre.
        val jitter = (random.nextFloat() - 0.5f) * sweep * 0.7f
        val pointerAngle = target * sweep + sweep / 2f + jitter
        val targetMod = ((360f - pointerAngle % 360f) % 360f + 360f) % 360f
        val current = ((rotationDeg % 360f) + 360f) % 360f
        val delta = ((targetMod - current) + 360f) % 360f
        val end = rotationDeg + 360f * (5 + random.nextInt(3)) + delta

        isSpinning = true
        lastTickIndex = indexAt(rotationDeg)
        animator = ValueAnimator.ofFloat(rotationDeg, end).apply {
            duration = 4200L + random.nextLong(1200L)
            interpolator = DecelerateInterpolator(2.2f)
            addUpdateListener {
                rotationDeg = it.animatedValue as Float
                val idx = indexAt(rotationDeg)
                if (idx != lastTickIndex) {
                    lastTickIndex = idx
                    onSegmentTick?.invoke()
                }
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isSpinning = false
                    rotationDeg %= 360f
                    onSpinEnd?.invoke(indexAt(rotationDeg))
                }
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.removeAllListeners()
        animator?.removeAllUpdateListeners()
        animator?.cancel()
        isSpinning = false
        super.onDetachedFromWindow()
    }
}
