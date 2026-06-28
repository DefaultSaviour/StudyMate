package uws.ac.uk.studymate.util

import android.animation.ObjectAnimator
import android.graphics.Rect
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import uws.ac.uk.studymate.R
import kotlin.random.Random

/**
 * Scatters the ambient floating "orbs" into the wood that FRAMES a glass card.
 *
 * Why this exists: orbs used to be hand-placed per screen, tuned to one device.
 * On a short screen the band shrinks and the percentage-positioned card rises, so
 * fixed-position orbs slid under the card. This instead MEASURES the real wood at
 * layout time and drops a non-overlapping jittered-grid scatter into it.
 *
 * On a phone the card fills the width, so the only wood is the band ABOVE the card
 * and that's the only place orbs land — exactly the original behaviour. On a large
 * screen (sw600dp) the card is capped + centred (see `@dimen/card_max_width`), so
 * wood also appears in the LEFT and RIGHT gutters; the same scatter rules run in
 * those regions too, framing the card. The side strips compute to zero width on a
 * phone, so there is no form-factor branching — it just falls out of the geometry.
 *
 * Orbs:
 *   - never overlap each other or the [avoid] views (the top-right button),
 *   - never sit under the card (regions are the wood MINUS the card rect),
 *   - in the top band, are anchored to the card's top edge so they ride up with it
 *     under the keyboard; side-strip orbs stay put (the keyboard docks at the
 *     bottom and the card already handles that inset).
 *
 * The total count is capped (see [BUDGET]) and split across regions so a wide
 * tablet frame stays ambient rather than swarmed.
 */
object OrbField {

    private val ICONS = AssignmentIcons.options.map { it.drawableResId }

    /**
     * Master switch (perf A/B). When false, [scatter] is a no-op and NO orbs are
     * created or animated — use this to confirm whether the ambient orbs are what
     * janks screen-open on a budget GPU. Set back to true to restore them.
     */
    private const val ENABLED = true

    /** Max orbs across the whole frame — keeps it calm on a wide tablet. */
    private const val BUDGET = 16

    private enum class Anchor { CARD_TOP, PARENT }

    private class Region(val rect: Rect, val anchor: Anchor) {
        var rows = 1
        var cols = 1
        var target = 0
    }

    /**
     * Call once (e.g. in onCreate). [card] is the glass MaterialCardView; [avoid]
     * are views the orbs must not overlap (typically the single top-right button).
     */
    fun scatter(card: View, avoid: List<View> = emptyList()) {
        if (!ENABLED) return
        card.doOnLayout { v ->
            // Defer out of the layout pass — adding views inside doOnLayout calls
            // requestLayout() mid-layout, which Android drops, leaving the orbs
            // unmeasured and corrupting the constraint solve. post() runs right after,
            // so the orbs fade in WITH the entrance (no extra delay — the old ~550ms
            // defer was only to dodge the oversized-image decode, which is now fixed).
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

        val gap = dp(6f)
        val edge = dp(8f)

        // The top band: full width, status bar → card top. This region exists on
        // every device and drives the (uniform) orb size.
        val topBandTop = statusTop + dp(6f)
        val topBandBottom = card.top - dp(4f)
        val topBandH = topBandBottom - topBandTop
        if (topBandH < dp(44f)) return   // no usable wood at all

        // Scale orb size to the top-band height so two rows always fit (a short band
        // otherwise collapses the scatter to a single line). Used for ALL regions so
        // the orbs read as one population, not three different sizes.
        val maxSizeDp = ((topBandH / density - 8f) / 2f).coerceIn(28f, 54f)
        val minSizeDp = (maxSizeDp - 14f).coerceAtLeast(24f)
        val orbPx = dp(maxSizeDp)

        // Regions = the wood MINUS the card. Top band always; left/right gutters only
        // when the card is narrower than the screen (tablets / unfolded foldables).
        val regions = ArrayList<Region>(3)
        regions.add(Region(Rect(edge, topBandTop, root.width - edge, topBandBottom), Anchor.CARD_TOP))

        val sideTop = card.top + gap
        val sideBottom = card.bottom - gap
        val leftStrip = Rect(edge, sideTop, card.left - gap, sideBottom)
        val rightStrip = Rect(card.right + gap, sideTop, root.width - edge, sideBottom)
        if (leftStrip.width() >= orbPx && leftStrip.height() >= orbPx) {
            regions.add(Region(leftStrip, Anchor.PARENT))
        }
        if (rightStrip.width() >= orbPx && rightStrip.height() >= orbPx) {
            regions.add(Region(rightStrip, Anchor.PARENT))
        }

        // Keep-out rectangles (the button(s) plus breathing room).
        val pad = dp(10f)
        val blocked = avoid.filter { it.width > 0 && it.height > 0 }.map {
            Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad)
        }

        // Per-region jittered grid + deterministic target (~60% of cells), then scale
        // the whole frame down to BUDGET so a wide tablet doesn't get swarmed. On a
        // phone there's only the top region and its target is already < BUDGET, so the
        // scale is 1 and the result is identical to the original single-band scatter.
        for (region in regions) {
            val w = region.rect.width()
            val h = region.rect.height()
            region.rows = ((h + gap) / (orbPx + gap)).coerceIn(1, 3)
            region.cols = ((w + gap) / (orbPx + gap)).coerceIn(1, 9)
            region.target = (region.rows * region.cols * 6 / 10).coerceIn(if (region.anchor == Anchor.CARD_TOP) 5 else 2, 14)
        }
        val rawTotal = regions.sumOf { it.target }
        val scale = if (rawTotal > BUDGET) BUDGET.toFloat() / rawTotal else 1f

        val icons = ICONS.shuffled()
        val placed = ArrayList<Rect>()
        var made = 0
        for (region in regions) {
            val regionTarget = (region.target * scale).toInt()
                .coerceAtLeast(if (region.anchor == Anchor.CARD_TOP) 3 else 1)
            made += fillRegion(root, card, region, regionTarget, orbPx, minSizeDp, maxSizeDp, gap, blocked, placed, icons, made)
        }
    }

    /** Fills one rectangular region with up to [target] non-overlapping orbs. */
    private fun fillRegion(
        root: ConstraintLayout,
        card: View,
        region: Region,
        target: Int,
        orbPx: Int,
        minSizeDp: Float,
        maxSizeDp: Float,
        gap: Int,
        blocked: List<Rect>,
        placed: ArrayList<Rect>,
        icons: List<Int>,
        startIndex: Int,
    ): Int {
        val density = root.resources.displayMetrics.density
        fun dp(v: Float) = (v * density).toInt()

        val colW = region.rect.width() / region.cols
        val rowH = region.rect.height() / region.rows

        // Visit cells in random order so the chosen subset is a real 2D scatter
        // (different rows AND columns), never a horizontal line.
        val cells = ArrayList<Pair<Int, Int>>(region.rows * region.cols)
        for (r in 0 until region.rows) for (c in 0 until region.cols) cells.add(c to r)
        cells.shuffle()

        var made = 0
        for ((c, r) in cells) {
            if (made >= target) break
            val size = dp(Random.nextInt(minSizeDp.toInt(), maxSizeDp.toInt() + 1).toFloat())
            val cellLeft = region.rect.left + c * colW
            val cellTop = region.rect.top + r * rowH
            val xHi = (cellLeft + colW - size).coerceAtLeast(cellLeft)
            val yHi = (cellTop + rowH - size).coerceAtLeast(cellTop)
            val x = if (xHi > cellLeft) Random.nextInt(cellLeft, xHi + 1) else cellLeft
            val y = if (yHi > cellTop) Random.nextInt(cellTop, yHi + 1) else cellTop
            val rect = Rect(x, y, x + size, y + size)
            if (blocked.any { Rect.intersects(it, rect) }) continue
            val grown = Rect(rect.left - gap, rect.top - gap, rect.right + gap, rect.bottom + gap)
            if (placed.any { Rect.intersects(it, grown) }) continue
            placed.add(rect)
            addOrb(root, card, region.anchor, rect, size, density, icons[(startIndex + made) % icons.size])
            made++
        }
        return made
    }

    private fun addOrb(root: ConstraintLayout, card: View, anchor: Anchor, rect: Rect, size: Int, density: Float, iconRes: Int) {
        val orb = ImageView(root.context).apply {
            setImageResource(iconRes)
            setBackgroundResource(R.drawable.bg_orb)
            val p = (size * 0.22f).toInt()
            setPadding(p, p, p, p)
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = (30..44).random() / 100f
            // Pure decoration — keep TalkBack out of the wood band entirely.
            contentDescription = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val lp = ConstraintLayout.LayoutParams(size, size).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = rect.left
            when (anchor) {
                // Top band: anchor to the card's top edge so orbs follow it up under
                // the keyboard (off-screen is fine — they're decoration).
                Anchor.CARD_TOP -> {
                    bottomToTop = card.id
                    bottomMargin = (card.top - rect.bottom).coerceAtLeast(0)
                }
                // Side gutters (tablets): static placement beside the card.
                Anchor.PARENT -> {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    topMargin = rect.top
                }
            }
        }
        // Add on top so orbs sit ABOVE the full-screen centerCrop wood ImageView each
        // screen now uses for its background (otherwise they'd be hidden behind it).
        root.addView(orb, lp)

        val amp = (8..18).random() * density

        // Entrance (1.1): orbs are constraint-anchored SIBLINGS of the card, so a card
        // slide wouldn't carry them — they used to appear abruptly ("pop"). A quick
        // alpha-only fade (started right after layout in scatter()'s post{}) brings them
        // in WITH the window, finishing ~260ms in so they land as it settles. Alpha-only
        // (no translate / per-orb layer) keeps it cheap. Hand off to the ambient float
        // after; the small per-orb start jitter stops them fading in lockstep.
        val restAlpha = orb.alpha
        orb.alpha = 0f
        orb.animate()
            .alpha(restAlpha)
            .setDuration(260)
            .setStartDelay((0..120).random().toLong())
            .setInterpolator(DecelerateInterpolator(1.4f))
            .withEndAction { startFloat(orb, amp) }
            .start()
    }

    /** The ambient, never-ending up/down drift, started after the entrance fade. */
    private fun startFloat(orb: View, amp: Float) {
        orb.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        ObjectAnimator.ofFloat(orb, View.TRANSLATION_Y, 0f, -amp).apply {
            duration = (3900..9000).random().toLong()
            // Desync via a random START DELAY + the duration variance above, NOT via
            // currentPlayTime — jumping the play position would snap the orb to a
            // mid-drift offset the instant the float starts (a visible "pop" right after
            // the fade-in). Starting from translationY=0 (its rest spot) is seamless.
            startDelay = (0..2200).random().toLong()
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}
