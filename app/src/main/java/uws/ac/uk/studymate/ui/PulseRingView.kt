package uws.ac.uk.studymate.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * A slow, soft "breathing" gold glow around a rounded-rect ring. The halo gently
 * swells (thicker + brighter) and recedes on the same easing the floating orbs use,
 * so it feels warmly lit rather than flashy. Overlaid on the dashboard
 * "Review due decks" button to signal a review is waiting.
 *
 * Purely decorative and never clickable, so taps fall through to the button beneath
 * it in the FrameLayout. The glow blooms slightly OUTSIDE the button border, so every
 * ancestor up to the scroll view keeps clipChildren / clipToPadding = false (see
 * activity_home.xml). Corner radius (12dp) is hand-synced with app:cornerRadius.
 */
class PulseRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val cornerRadius = 12f * density
    private val inset = 1.5f * density             // sit right on the button border

    private val minStroke = 1.5f * density
    private val maxStroke = 4f * density
    private val minAlpha = 45
    private val maxAlpha = 165

    private val ringPath = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E7CE8C")        // warm gold, a touch brighter than the border
        maskFilter = BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL)
    }

    private var phase = 0f                          // 0 (rest) .. 1 (full breath)
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ringPath.reset()
        ringPath.addRoundRect(
            inset, inset, w - inset, h - inset, cornerRadius, cornerRadius, Path.Direction.CW
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        paint.strokeWidth = minStroke + (maxStroke - minStroke) * phase
        paint.alpha = (minAlpha + (maxAlpha - minAlpha) * phase).toInt()
        canvas.drawPath(ringPath, paint)
    }

    fun startAnimating() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400L                        // ~4.8s full breath (in + out)
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopAnimating() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        stopAnimating()
        super.onDetachedFromWindow()
    }
}
