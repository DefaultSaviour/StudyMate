package uws.ac.uk.studymate.util

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.doOnPreDraw

/**
 * Shared wood-glass entrance animation: the glass card slides up into place, then the
 * content rows stagger in beneath it.
 *
 * Two problems this solves (1.1):
 *
 * 1. **The "pop".** Every screen used to start `card.animate()` SYNCHRONOUSLY in
 *    `onCreate`, before the first layout/inset pass had settled. The slide animates
 *    toward `translationY = 0`, but `0` resolves against a layout position that then
 *    shifts ~20-40px (the percent header guideline + edge-to-edge insets resolving on
 *    a later pass), so the card visibly jumped at the end. Fix: defer the start to
 *    [doOnPreDraw] (after measure + layout + inset dispatch), so `translationY(0)`
 *    targets the genuine final position.
 *
 * 2. **The lag/stutter (turned out to be the backgrounds, not this).** During 1.1 the
 *    card slide looked like the cause, but the real culprit was oversized background
 *    photos decoding on the main thread (see WhatToDo.md / the bg_*.jpg downsize). With
 *    those fixed, the card slide is cheap again — provided it's **slide-only** (a
 *    `translationY` translate under `withLayer()`) and NEVER an alpha fade (fading a
 *    translucent near-full-screen card over the wood forces per-frame offscreen
 *    compositing, which does lag). So [animateCard] defaults to **true** (slide). Set it
 *    false to leave the card static and animate only the content stagger + orbs.
 */
object Entrance {

    /**
     * Master switch (perf A/B). When false, [play] is a no-op: the card + content just
     * appear at their final state with NO entrance animation. Use this to isolate our
     * animation code from the platform's activity-open transition. If screen-open is
     * still janky with this false AND orbs off, the cost is the incoming activity's
     * inflation + overdraw, not our animations. Set true to restore the entrance.
     */
    private const val ENABLED = true

    /**
     * @param card     the glass MaterialCardView (or any root card view).
     * @param stagger  content rows that fade + rise in, in order.
     * @param animateCard  true (default) = slide the card up (slide-only, no alpha,
     *                     pop-free via doOnPreDraw); false = card static, only the
     *                     content stagger + orbs animate.
     * @param staggerOffsetDp  how far below each content row starts.
     */
    fun play(
        card: View,
        stagger: List<View> = emptyList(),
        animateCard: Boolean = true,
        cardOffsetDp: Float = 110f,
        cardDuration: Long = 380,
        cardStartDelay: Long = 40,
        staggerOffsetDp: Float = 24f,
        staggerDuration: Long = 340,
        staggerStartDelay: Long = 180,
        staggerStep: Long = 55,
    ) {
        if (!ENABLED) return   // perf A/B: snap to final state, no entrance animation.

        val density = card.resources.displayMetrics.density

        // Apply start offsets immediately so nothing flashes at its final position.
        // (translationY is relative, so this is always `offset` below where it lays out.)
        // No alpha on the card — fading a translucent near-full-screen layer over the
        // wood is the expensive part; the content fading in already reads as a reveal.
        if (animateCard) card.translationY = cardOffsetDp * density
        stagger.forEach { v ->
            v.alpha = 0f
            v.translationY = staggerOffsetDp * density
        }

        // Start only once layout + insets have settled (one-shot). When the card slides,
        // this is also the "pop" fix: translationY(0) targets the genuine final position.
        card.doOnPreDraw {
            if (animateCard) {
                // Slide only (no alpha) under a managed hardware layer = a cheap
                // cached-bitmap translate. Never add .alpha() here — it lags.
                card.animate()
                    .translationY(0f)
                    .setDuration(cardDuration)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .setStartDelay(cardStartDelay)
                    .withLayer()
                    .start()
            }

            stagger.forEachIndexed { i, v ->
                v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(staggerDuration)
                    .setInterpolator(DecelerateInterpolator(1.3f))
                    .setStartDelay(staggerStartDelay + i * staggerStep)
                    .start()
            }
        }
    }
}
