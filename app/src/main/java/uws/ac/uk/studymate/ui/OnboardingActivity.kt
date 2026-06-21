package uws.ac.uk.studymate.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.OnboardingViewModel
import uws.ac.uk.studymate.util.OrbField
import uws.ac.uk.studymate.util.OrientationLock
import kotlin.math.abs

/*//////////////////////
First-run onboarding (0.9E).
A 4-page wood-glass swipe carousel shown once, right after a new account is
created (launched from LoginActivity on registrationSuccess). The first three
pages explain the app (assignments, spaced repetition, privacy); the fourth is a
call to action that offers a guided first action — reviewing the "How StudyMate
works" sample deck that SampleContentSeeder pre-loaded.

On the CTA page the primary button becomes "Try the sample deck", which drops the
user into a real review session (ReviewDeckActivity with EXTRA_FROM_ONBOARDING, so
it returns to Home when finished). Skip (any page) goes straight to Home. There is
no persisted "seen" flag — this screen is only ever launched on account creation.
 *//////////////////////
class OnboardingActivity : AppCompatActivity() {

    private lateinit var vm: OnboardingViewModel
    private var sampleDeck: OnboardingViewModel.SampleDeck? = null

    private lateinit var pages: List<View>
    private lateinit var dots: List<ImageView>
    private lateinit var nextBtn: MaterialButton
    private lateinit var skipBtn: MaterialButton

    private var current = 0

    private val dotActive = "#C4A24A".toColorInt()       // gold
    private val dotInactive = "#66D4BC7E".toColorInt()   // faded gold-light

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        OrientationLock.apply(this)

        val card = findViewById<MaterialCardView>(R.id.onboardingCard)
        val content = findViewById<View>(R.id.onboardingContent)
        val pageContainer = findViewById<View>(R.id.pageContainer)

        pages = listOf(
            findViewById(R.id.page0),
            findViewById(R.id.page1),
            findViewById(R.id.page2),
            findViewById(R.id.page3)
        )
        dots = listOf(
            findViewById(R.id.dot0),
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3)
        )
        nextBtn = findViewById(R.id.nextBtn)
        skipBtn = findViewById(R.id.skipBtn)

        // Pad the card content above the system nav bar (edge-to-edge).
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(
                view.paddingLeft, view.paddingTop, view.paddingRight,
                navBar + (20 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        vm = ViewModelProvider(this)[OnboardingViewModel::class.java]
        vm.sampleDeck.observe(this) { sampleDeck = it }
        vm.loadSampleDeck()

        // On the last page the primary button is the CTA (review the deck); before
        // that it just advances the carousel. Skip always goes straight to Home.
        nextBtn.setOnClickListener {
            if (current == pages.lastIndex) startGuidedReview() else goTo(current + 1)
        }
        skipBtn.setOnClickListener { goHome() }

        // Swipe left/right to move between pages.
        val gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                val start = e1 ?: return false
                val dx = e2.x - start.x
                if (abs(dx) > 80 && abs(velocityX) > 120 && abs(dx) > abs(e2.y - start.y)) {
                    if (dx < 0) goTo(current + 1) else goTo(current - 1)
                    return true
                }
                return false
            }
        })
        pageContainer.setOnTouchListener { _, event -> gestures.onTouchEvent(event) }

        updateChrome()

        // Ambient orbs in the wood band above the card.
        OrbField.scatter(card, emptyList())

        // Entrance: card slides up and fades in (matches the other screens).
        val density = resources.displayMetrics.density
        card.translationY = 200f * density
        card.alpha = 0f
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .setStartDelay(60)
            .withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()
    }

    // Animate from the current page to [target], sliding both in the swipe direction.
    private fun goTo(target: Int) {
        if (target == current || target < 0 || target >= pages.size) return
        val forward = target > current
        val outView = pages[current]
        val inView = pages[target]

        val width = pages[current].width.toFloat().takeIf { it > 0f }
            ?: resources.displayMetrics.widthPixels.toFloat()

        inView.translationX = if (forward) width else -width
        inView.alpha = 0f
        inView.visibility = View.VISIBLE
        inView.animate()
            .translationX(0f).alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        outView.animate()
            .translationX(if (forward) -width else width).alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                outView.visibility = View.INVISIBLE
                outView.translationX = 0f
                outView.alpha = 1f
            }
            .start()

        current = target
        updateChrome()
    }

    // Sync the dots and the Next/CTA + Skip buttons to the current page.
    private fun updateChrome() {
        dots.forEachIndexed { i, dot ->
            dot.setColorFilter(if (i == current) dotActive else dotInactive)
        }
        val onCtaPage = current == pages.lastIndex
        nextBtn.setText(if (onCtaPage) R.string.onboarding_try_button else R.string.onboarding_next)
        skipBtn.setText(if (onCtaPage) R.string.onboarding_skip_for_now else R.string.onboarding_skip)
    }

    // Launch a real review of the seeded sample deck. The review screen returns to
    // Home when done (EXTRA_FROM_ONBOARDING). Falls back to Home if the deck is
    // somehow missing or not resolved yet.
    private fun startGuidedReview() {
        // Onboarding is done — the sample assignment has served its purpose.
        vm.completeSampleAssignment()
        val deck = sampleDeck
        if (deck == null) {
            goHome()
            return
        }
        startActivity(
            Intent(this, ReviewDeckActivity::class.java)
                .putExtra("deck_id", deck.id)
                .putExtra("deck_name", deck.name)
                .putExtra(ReviewDeckActivity.EXTRA_FROM_ONBOARDING, true)
        )
        finish()
    }

    private fun goHome() {
        // Leaving onboarding (Skip, or the CTA fallback when the deck is missing) —
        // mark the sample assignment done. Idempotent, so a double-call is harmless.
        vm.completeSampleAssignment()
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
