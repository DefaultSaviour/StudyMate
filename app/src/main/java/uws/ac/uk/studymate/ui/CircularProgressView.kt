package uws.ac.uk.studymate.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.min

/**
 * A custom circular progress indicator designed to fit StudyMate's Dark Academic wood-glass theme.
 *
 * It draws:
 * 1. A thin, semi-transparent cream background track circle.
 * 2. A wider, soft blurred gold glow arc drawn underneath the progress arc.
 * 3. A gold foreground progress arc with rounded caps.
 *
 * The progress updates smoothly using a ValueAnimator to transition between ticks.
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        // Software rendering is required for BlurMaskFilter to render correctly.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val density = resources.displayMetrics.density

    // Stroke widths
    private val trackWidth = 2f * density
    private val progressWidth = 4f * density
    private val glowWidth = progressWidth * 2.5f

    // Bounding rect for drawing the arcs
    private val arcBounds = RectF()

    // Paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = trackWidth
        color = Color.parseColor("#26FAF8F5") // 15% opacity cream
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = glowWidth
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#40C4A24A") // ~25% opacity gold
        maskFilter = BlurMaskFilter(8f * density, BlurMaskFilter.Blur.NORMAL)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = progressWidth
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#C4A24A") // Primary brass gold
    }

    // Live progress state
    private var drawnProgress = 0f
    private var animator: ValueAnimator? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Force the view to be a perfect square based on the smaller dimension
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate bounds of the circle, leaving padding for the thickest stroke (glowWidth)
        // to prevent clipping at the view boundaries.
        val maxStroke = glowWidth
        val padding = maxStroke / 2f + 2f * density
        val size = w.toFloat()
        
        arcBounds.set(
            padding,
            padding,
            size - padding,
            size - padding
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        // 1. Draw the background track circle (full 360 degrees)
        val cx = width / 2f
        val cy = height / 2f
        val radius = arcBounds.width() / 2f
        canvas.drawCircle(cx, cy, radius, trackPaint)

        // 2. Draw the glowing arc and foreground progress arc if there is progress
        if (drawnProgress > 0f) {
            val startAngle = -90f // Start from the top (12 o'clock)
            val sweepAngle = drawnProgress * 360f

            // Draw glow arc underneath
            canvas.drawArc(arcBounds, startAngle, sweepAngle, false, glowPaint)

            // Draw primary gold progress arc
            canvas.drawArc(arcBounds, startAngle, sweepAngle, false, progressPaint)
        }
    }

    /**
     * Updates the progress state.
     * @param value Progress fraction between 0.0 and 1.0.
     * @param animate If true, animates the transition smoothly.
     */
    fun setProgress(value: Float, animate: Boolean = true) {
        val clamped = value.coerceIn(0f, 1f)
        if (animate) {
            animateTo(clamped)
        } else {
            animator?.cancel()
            drawnProgress = clamped
            invalidate()
        }
    }

    private fun animateTo(target: Float) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(drawnProgress, target).apply {
            duration = 400L // 400ms is fast enough to keep up with ticks, but smooth to look at
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                drawnProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
}
