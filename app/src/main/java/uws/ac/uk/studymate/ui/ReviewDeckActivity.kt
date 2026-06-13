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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashCard

class ReviewDeckActivity : AppCompatActivity() {

    private lateinit var card: MaterialCardView
    private lateinit var flipCard: MaterialCardView
    private lateinit var titleText: TextView
    private lateinit var cardCountText: TextView
    private lateinit var cardContentText: TextView
    private lateinit var flipBtn: MaterialButton
    private lateinit var prevBtn: MaterialButton
    private lateinit var nextBtn: MaterialButton

    private var cards: List<FlashCard> = emptyList()
    private var currentIndex = 0
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
        flipBtn = findViewById(R.id.flipBtn)
        prevBtn = findViewById(R.id.prevBtn)
        nextBtn = findViewById(R.id.nextBtn)

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
        findViewById<MaterialCardView>(R.id.flipCard).setOnClickListener { flipCard() }
        cardContentText.setOnClickListener { flipCard() }
        flipBtn.setOnClickListener { flipCard() }
        prevBtn.setOnClickListener { showPrevious() }
        nextBtn.setOnClickListener { showNext() }

        val d = resources.displayMetrics.density
        card.translationY = 200f * d
        card.alpha = 0f
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        card.animate().translationY(0f).alpha(1f).setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f)).setStartDelay(60)
            .withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()

        loadCards(deckId)
    }

    private fun loadCards(deckId: Int) {
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                StudyMateDatabase.getInstance(application).cardDao().getCards(deckId)
            }
            cards = loaded
            currentIndex = 0
            showingFront = true

            if (cards.isEmpty()) {
                cardCountText.text = "No cards yet"
                cardContentText.text = "Add cards from Manage cards"
                flipBtn.isEnabled = false
                prevBtn.isEnabled = false
                nextBtn.isEnabled = false
                flipBtn.alpha = 0.45f
                prevBtn.alpha = 0.45f
                nextBtn.alpha = 0.45f
            } else {
                showCurrentCard()
            }
        }
    }

    private fun showCurrentCard() {
        if (cards.isEmpty()) return
        showingFront = true
        val c = cards[currentIndex]
        cardCountText.text = "Card ${currentIndex + 1} of ${cards.size}"
        cardContentText.text = c.front
        cardContentText.setTypeface(null, Typeface.BOLD)
        prevBtn.isEnabled = currentIndex > 0
        nextBtn.isEnabled = currentIndex < cards.size - 1
        prevBtn.alpha = if (prevBtn.isEnabled) 1f else 0.45f
        nextBtn.alpha = if (nextBtn.isEnabled) 1f else 0.45f
    }

    private fun flipCard() {
        if (cards.isEmpty() || isFlipping) return
        val c = cards[currentIndex]
        val density = resources.displayMetrics.density
        flipCard.cameraDistance = 14000f * density
        isFlipping = true

        flipCard.animate()
            .rotationX(90f)
            .setDuration(500)
            .setInterpolator(AccelerateInterpolator(1.5f))
            .withEndAction {
                showingFront = !showingFront
                if (showingFront) {
                    cardContentText.text = c.front
                    cardContentText.setTypeface(null, Typeface.BOLD)
                } else {
                    cardContentText.text = c.back
                    cardContentText.setTypeface(null, Typeface.NORMAL)
                }
                flipCard.rotationX = -90f
                flipCard.animate()
                    .rotationX(0f)
                    .setDuration(500)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .withEndAction { isFlipping = false }
                    .start()
            }
            .start()
    }

    private fun showPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            showCurrentCard()
        }
    }

    private fun showNext() {
        if (currentIndex < cards.size - 1) {
            currentIndex++
            showCurrentCard()
        }
    }
}
