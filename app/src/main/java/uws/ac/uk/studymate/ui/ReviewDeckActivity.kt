package uws.ac.uk.studymate.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.ui.viewmodels.ReviewDeckViewModel

// Review session: walk the deck's due cards one at a time. Show the front,
// tap/"Show answer" to reveal the back, then grade Again / Wrong / Correct.
// Again re-shows the card now, Wrong re-shows it later this session, Correct
// advances. Drives off ReviewDeckViewModel.
class ReviewDeckActivity : AppCompatActivity() {

    private lateinit var vm: ReviewDeckViewModel

    private lateinit var card: MaterialCardView
    private lateinit var flipCard: MaterialCardView
    private lateinit var titleText: TextView
    private lateinit var cardCountText: TextView
    private lateinit var cardContentText: TextView
    private lateinit var tapHint: TextView
    private lateinit var flipBtn: MaterialButton
    private lateinit var gradeRow: View
    private lateinit var reviewAllBtn: MaterialButton

    private var currentCard: FlashCard? = null
    private var showingFront = true
    private var isFlipping = false

    // First-run onboarding (0.9E): when launched from the welcome screen, exiting
    // the session (back arrow or the "Done" continue button) goes to Home rather
    // than back to whatever launched us.
    private var fromOnboarding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_deck)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)

        val deckId = intent.getIntExtra("deck_id", -1)
        val deckName = intent.getStringExtra("deck_name") ?: "Deck"
        // When launched from the dashboard "Review due decks" button we get an ordered
        // queue of decks to walk back-to-back instead of a single deck.
        val queueIds = intent.getIntArrayExtra("deck_queue_ids")
        val queueNames = intent.getStringArrayExtra("deck_queue_names")
        fromOnboarding = intent.getBooleanExtra(EXTRA_FROM_ONBOARDING, false)

        card = findViewById(R.id.reviewCard)
        flipCard = findViewById(R.id.flipCard)
        titleText = findViewById(R.id.reviewDeckTitle)
        cardCountText = findViewById(R.id.cardCountText)
        cardContentText = findViewById(R.id.cardContentText)
        tapHint = findViewById(R.id.tapHintText)
        flipBtn = findViewById(R.id.flipBtn)
        gradeRow = findViewById(R.id.gradeRow)
        reviewAllBtn = findViewById(R.id.reviewAllBtn)

        titleText.text = queueNames?.firstOrNull() ?: deckName

        val reviewPanel = findViewById<View>(R.id.reviewPanel)
        ViewCompat.setOnApplyWindowInsetsListener(card) { _, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            reviewPanel.setPadding(
                reviewPanel.paddingLeft, reviewPanel.paddingTop,
                reviewPanel.paddingRight, navBar + base
            )
            insets
        }

        findViewById<MaterialButton>(R.id.backBtn).setOnClickListener {
            if (fromOnboarding) goHome() else finish()
        }
        flipCard.setOnClickListener { revealAnswer() }
        cardContentText.setOnClickListener { revealAnswer() }
        flipBtn.setOnClickListener { revealAnswer() }
        reviewAllBtn.setOnClickListener { vm.reviewAll() }
        findViewById<MaterialButton>(R.id.againBtn).setOnClickListener { grade(ReviewDeckViewModel.Grade.AGAIN) }
        findViewById<MaterialButton>(R.id.wrongBtn).setOnClickListener { grade(ReviewDeckViewModel.Grade.WRONG) }
        findViewById<MaterialButton>(R.id.correctBtn).setOnClickListener { grade(ReviewDeckViewModel.Grade.CORRECT) }

        uws.ac.uk.studymate.util.Entrance.play(card)

        vm = ViewModelProvider(this)[ReviewDeckViewModel::class.java]
        vm.state.observe(this) { render(it) }
        if (queueIds != null && queueIds.isNotEmpty()) {
            vm.loadChain(queueIds.toList(), queueNames?.toList() ?: emptyList())
        } else {
            vm.load(deckId, deckName)
        }
    }

    private fun render(state: ReviewDeckViewModel.State) {
        when (state) {
            is ReviewDeckViewModel.State.Loading -> {
                cardCountText.text = "Loading…"
                cardContentText.text = ""
                showButtons(flip = false, grades = false, reviewAll = false)
                tapHint.visibility = View.GONE
            }
            is ReviewDeckViewModel.State.Empty -> {
                currentCard = null
                titleText.text = state.deckName
                cardCountText.text = "All caught up"
                setContent("No cards are due for review right now. 🎉", bold = false)
                showButtons(flip = false, grades = false, reviewAll = true)
                tapHint.visibility = View.GONE
                setFlipTappable(false)
            }
            is ReviewDeckViewModel.State.Reviewing -> {
                currentCard = state.card
                titleText.text = state.deckName
                showingFront = true
                resetFlipRotation()
                cardCountText.text = "${state.remaining} card${if (state.remaining == 1) "" else "s"} left"
                setContent(state.card.front, bold = true)
                showButtons(flip = true, grades = false, reviewAll = false)
                tapHint.visibility = View.VISIBLE
                setFlipTappable(true)
            }
            is ReviewDeckViewModel.State.Done -> {
                currentCard = null
                titleText.text = state.deckName
                val n = state.reviewedCount
                cardCountText.text = "Review complete"
                setContent("Reviewed $n card${if (n == 1) "" else "s"}.\nGreat work!", bold = false)
                // In onboarding mode, give a clear way out to the dashboard; otherwise
                // the user just taps the top back arrow as before.
                if (fromOnboarding) {
                    reviewAllBtn.text = getString(R.string.onboarding_go_to_dashboard)
                    reviewAllBtn.setOnClickListener { goHome() }
                    showButtons(flip = false, grades = false, reviewAll = true)
                } else {
                    showButtons(flip = false, grades = false, reviewAll = false)
                }
                tapHint.visibility = View.GONE
                setFlipTappable(false)
            }
        }
    }

    // Front -> back reveal (one-way; once the answer is shown the user must grade).
    private fun revealAnswer() {
        val c = currentCard ?: return
        if (!showingFront || isFlipping) return
        val density = resources.displayMetrics.density
        flipCard.cameraDistance = 14000f * density
        isFlipping = true

        flipCard.animate()
            .rotationX(90f)
            .setDuration(500)
            .setInterpolator(AccelerateInterpolator(1.5f))
            .withEndAction {
                showingFront = false
                setContent(c.back, bold = false)
                flipCard.rotationX = -90f
                flipCard.animate()
                    .rotationX(0f)
                    .setDuration(500)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .withEndAction {
                        isFlipping = false
                        showButtons(flip = false, grades = true, reviewAll = false)
                        tapHint.visibility = View.GONE
                    }
                    .start()
            }
            .start()
    }

    private fun grade(grade: ReviewDeckViewModel.Grade) {
        if (isFlipping || currentCard == null) return
        vm.grade(grade)
    }

    private fun setContent(text: String, bold: Boolean) {
        cardContentText.text = text
        cardContentText.setTypeface(null, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun resetFlipRotation() {
        flipCard.rotationX = 0f
    }

    private fun setFlipTappable(enabled: Boolean) {
        flipCard.isClickable = enabled
        cardContentText.isClickable = enabled
    }

    private fun showButtons(flip: Boolean, grades: Boolean, reviewAll: Boolean) {
        flipBtn.visibility = if (flip) View.VISIBLE else View.GONE
        gradeRow.visibility = if (grades) View.VISIBLE else View.GONE
        reviewAllBtn.visibility = if (reviewAll) View.VISIBLE else View.GONE
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    companion object {
        /** Set when the review is launched from first-run onboarding; on exit we go to Home. */
        const val EXTRA_FROM_ONBOARDING = "from_onboarding"
    }
}
