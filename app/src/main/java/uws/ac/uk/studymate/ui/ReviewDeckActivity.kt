package uws.ac.uk.studymate.ui

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
import uws.ac.uk.studymate.util.SpacedRepetition

// SM-2 review session: walk the deck's due cards one at a time. Show the front,
// tap/"Show answer" to reveal the back, then grade Again/Hard/Good/Easy — each
// grade schedules the card and advances to the next. Drives off ReviewDeckViewModel.
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_deck)

        val deckId = intent.getIntExtra("deck_id", -1)
        val deckName = intent.getStringExtra("deck_name") ?: "Deck"

        card = findViewById(R.id.reviewCard)
        flipCard = findViewById(R.id.flipCard)
        titleText = findViewById(R.id.reviewDeckTitle)
        cardCountText = findViewById(R.id.cardCountText)
        cardContentText = findViewById(R.id.cardContentText)
        tapHint = findViewById(R.id.tapHintText)
        flipBtn = findViewById(R.id.flipBtn)
        gradeRow = findViewById(R.id.gradeRow)
        reviewAllBtn = findViewById(R.id.reviewAllBtn)

        titleText.text = deckName

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

        findViewById<MaterialButton>(R.id.backBtn).setOnClickListener { finish() }
        flipCard.setOnClickListener { revealAnswer() }
        cardContentText.setOnClickListener { revealAnswer() }
        flipBtn.setOnClickListener { revealAnswer() }
        reviewAllBtn.setOnClickListener { vm.reviewAll() }
        findViewById<MaterialButton>(R.id.againBtn).setOnClickListener { grade(SpacedRepetition.AGAIN) }
        findViewById<MaterialButton>(R.id.hardBtn).setOnClickListener { grade(SpacedRepetition.HARD) }
        findViewById<MaterialButton>(R.id.goodBtn).setOnClickListener { grade(SpacedRepetition.GOOD) }
        findViewById<MaterialButton>(R.id.easyBtn).setOnClickListener { grade(SpacedRepetition.EASY) }

        val d = resources.displayMetrics.density
        card.translationY = 200f * d
        card.alpha = 0f
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        card.animate().translationY(0f).alpha(1f).setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f)).setStartDelay(60)
            .withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()

        vm = ViewModelProvider(this)[ReviewDeckViewModel::class.java]
        vm.state.observe(this) { render(it) }
        vm.load(deckId, deckName)
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
                cardCountText.text = "All caught up"
                setContent("No cards are due for review right now. 🎉", bold = false)
                showButtons(flip = false, grades = false, reviewAll = true)
                tapHint.visibility = View.GONE
                setFlipTappable(false)
            }
            is ReviewDeckViewModel.State.Reviewing -> {
                currentCard = state.card
                showingFront = true
                resetFlipRotation()
                cardCountText.text = "Card ${state.position} of ${state.total}"
                setContent(state.card.front, bold = true)
                showButtons(flip = true, grades = false, reviewAll = false)
                tapHint.visibility = View.VISIBLE
                setFlipTappable(true)
            }
            is ReviewDeckViewModel.State.Done -> {
                currentCard = null
                val n = state.reviewedCount
                cardCountText.text = "Review complete"
                setContent("Reviewed $n card${if (n == 1) "" else "s"}.\nGreat work!", bold = false)
                showButtons(flip = false, grades = false, reviewAll = false)
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

    private fun grade(grade: Int) {
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
}
