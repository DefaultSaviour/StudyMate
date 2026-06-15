package uws.ac.uk.studymate.util

import android.animation.ObjectAnimator
import android.graphics.Rect
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import uws.ac.uk.studymate.R
import kotlin.random.Random

/**
 * Scatters the ambient floating "orbs" into the wood band ABOVE a glass card.
 *
 * Why this exists: orbs used to be hand-placed per screen, tuned to one device.
 * On a short screen the band shrinks and the percentage-positioned card rises,
 * so fixed-position orbs slid under the card. This instead MEASURES the real
 * band at layout time and drops in as many non-overlapping orbs as actually fit
 * — room for 10 → 10, room for 2 → 2. Orbs:
 *   - never overlap each other or the [avoid] views (the top-right settings /
 *     back button),
 *   - are anchored to the card's top edge, so when the keyboard pushes the card
 *     up they ride up with it (going off-screen is fine — they're decoration),
 *   - never sit under the card (the band stops at the card's top).
 *
 * Cost is a single rejection-sampling pass at layout; no per-frame work beyond
 * the existing float animation.
 */
object OrbField {

    private val ICONS = AssignmentIcons.options.map { it.drawableResId }

    /**
     * Call once (e.g. in onCreate). [card] is the glass MaterialCardView; [avoid]
     * are views the orbs must not overlap (typically the single top-right button).
     */
    fun scatter(card: View, avoid: List<View> = emptyList()) {
        card.doOnLayout { v ->
            // Defer out of the layout pass — adding views inside doOnLayout calls
            // requestLayout() mid-layout, which Android drops, leaving the orbs
            // unmeasured and corrupting the constraint solve. post() runs after.
            v.post {
                val root = v.parent as? ConstraintLayout ?: return@post
                place(root, v, avoid)
            }
        }
    }

    private fun place(root: ConstraintLayout, card: View, avoid: List<View>) {
        val density = root.resources.displayMetrics.density
        fun dp(v: Float) = (v * density).toInt()

        val statusTop = ViewCompat.getRootWindowInsets(root)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: dp(24f)

        val bandTop = statusTop + dp(6f)
        val bandBottom = card.top - dp(4f)        // never under the card
        val bandLeft = dp(8f)
        val bandRight = root.width - dp(8f)
        val bandH = bandBottom - bandTop
        val bandW = bandRight - bandLeft
        if (bandH < dp(44f) || bandW < dp(44f)) return   // no usable band

        // Keep-out rectangles (the button(s) plus breathing room).
        val pad = dp(10f)
        val blocked = avoid.filter { it.width > 0 && it.height > 0 }.map {
            Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad)
        }

        // Scale orb size to the band height so two rows always fit (a short band
        // otherwise collapses the scatter to a single line).
        val gap = dp(6f)
        val bandHdp = bandH / density
        // Cap raised for bigger orbs on roomy bands, but the (bandH-8)/2 term still
        // limits short bands to a size where TWO rows fit (no 1-row line regression).
        val maxSizeDp = ((bandHdp - 8f) / 2f).coerceIn(28f, 54f)
        val minSizeDp = (maxSizeDp - 14f).coerceAtLeast(24f)
        val orbPx = dp(maxSizeDp)

        // Lay the band out as a jittered grid so orbs land in DIFFERENT rows (a
        // real 2D scatter) instead of filling one long horizontal line. n items of
        // size s with gaps fit when n <= (band + gap) / (s + gap).
        val rows = ((bandH + gap) / (orbPx + gap)).coerceIn(1, 3)
        val cols = ((bandW + gap) / (orbPx + gap)).coerceIn(1, 9)
        val colW = bandW / cols
        val rowH = bandH / rows

        // Visit cells in random order and fill a fraction of them, up to a cap, so
        // the count stays tasteful even on a very wide screen (e.g. a foldable).
        val cells = ArrayList<Pair<Int, Int>>(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) cells.add(c to r)
        cells.shuffle()

        val icons = ICONS.shuffled()
        // Deterministic count (~60% of cells, clamped) so it's reliable instead of a
        // high-variance per-cell coin flip — wide screens get more, short ones fewer.
        val target = (rows * cols * 6 / 10).coerceIn(5, 14)
        val placed = ArrayList<Rect>()
        var made = 0
        for ((c, r) in cells) {
            if (made >= target) break
            val size = dp(Random.nextInt(minSizeDp.toInt(), maxSizeDp.toInt() + 1).toFloat())
            val cellLeft = bandLeft + c * colW
            val cellTop = bandTop + r * rowH
            val xHi = (cellLeft + colW - size).coerceAtLeast(cellLeft)
            val yHi = (cellTop + rowH - size).coerceAtLeast(cellTop)
            val x = if (xHi > cellLeft) Random.nextInt(cellLeft, xHi + 1) else cellLeft
            val y = if (yHi > cellTop) Random.nextInt(cellTop, yHi + 1) else cellTop
            val rect = Rect(x, y, x + size, y + size)
            if (blocked.any { Rect.intersects(it, rect) }) continue
            val grown = Rect(rect.left - gap, rect.top - gap, rect.right + gap, rect.bottom + gap)
            if (placed.any { Rect.intersects(it, grown) }) continue
            placed.add(rect)
            addOrb(root, card, rect, size, density, icons[made % icons.size])
            made++
        }
    }

    private fun addOrb(root: ConstraintLayout, card: View, rect: Rect, size: Int, density: Float, iconRes: Int) {
        val orb = ImageView(root.context).apply {
            setImageResource(iconRes)
            setBackgroundResource(R.drawable.bg_orb)
            val p = (size * 0.22f).toInt()
            setPadding(p, p, p, p)
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = (30..44).random() / 100f
            contentDescription = null
        }
        val lp = ConstraintLayout.LayoutParams(size, size).apply {
            // Anchor to the card's top edge so orbs follow it up under the keyboard.
            bottomToTop = card.id
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = rect.left
            bottomMargin = (card.top - rect.bottom).coerceAtLeast(0)
        }
        // Add on top: positioned in the empty band above the card (and clear of
        // the top-right button), so z-order doesn't cause overlap — but this keeps
        // them ABOVE any full-screen background ImageView some screens add (e.g.
        // the centerCrop wood on Calendar/Settings), which would otherwise hide them.
        root.addView(orb, lp)

        val amp = (8..18).random() * density
        orb.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        ObjectAnimator.ofFloat(orb, View.TRANSLATION_Y, 0f, -amp).apply {
            duration = (3900..9000).random().toLong()
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
            // Jump each orb to a random point in its cycle so they don't all swell
            // in unison (the "in sync" look) — instant phase desync.
            currentPlayTime = (0 until duration).random()
        }
    }
}
