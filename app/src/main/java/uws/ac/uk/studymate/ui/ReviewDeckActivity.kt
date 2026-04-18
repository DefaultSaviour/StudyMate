package uws.ac.uk.studymate.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashCard
/*//////////////////////
Coded by Jamie Coleman
 18/04/26
*//////////////////////
class ReviewDeckActivity : AppCompatActivity() {

    private lateinit var cardCountText: TextView
    private lateinit var cardContentText: TextView
    private lateinit var flipBtn: Button
    private lateinit var prevBtn: Button
    private lateinit var nextBtn: Button

    private var cards: List<FlashCard> = emptyList()
    private var currentIndex = 0
    private var showingFront = true

    /**
     This screen lets the user review flashcards one at a time.
     Tap the card area or the flip button to reveal the back.
     Use next and previous to move through the deck.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_deck)

        val deckId = intent.getIntExtra("deck_id", -1)
        val deckName = intent.getStringExtra("deck_name") ?: "Deck"

        val backBtn: Button = findViewById(R.id.reviewDeckBackBtn)
        val titleText: TextView = findViewById(R.id.reviewDeckTitleText)
        val homeBtn: Button = findViewById(R.id.reviewDeckHomeBtn)
        cardCountText = findViewById(R.id.cardCountText)
        cardContentText = findViewById(R.id.cardContentText)
        flipBtn = findViewById(R.id.flipBtn)
        prevBtn = findViewById(R.id.prevBtn)
        nextBtn = findViewById(R.id.nextBtn)

        titleText.text = deckName
        cardContentText.setOnClickListener { flipCard() }

        // Return to the previous screen when the back button is pressed.
        backBtn.setOnClickListener { finish() }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
        }

        flipBtn.setOnClickListener { flipCard() }
        prevBtn.setOnClickListener { showPrevious() }
        nextBtn.setOnClickListener { showNext() }

        // Load the cards for this deck from the database.
        loadCards(deckId)
    }

    // Load all cards that belong to this deck.
    private fun loadCards(deckId: Int) {
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val db = StudyMateDatabase.getInstance(application)
                db.cardDao().getCards(deckId)
            }

            cards = loaded
            currentIndex = 0
            showingFront = true

            if (cards.isEmpty()) {
                cardCountText.text = getString(R.string.deck_has_no_cards)
                cardContentText.text = getString(R.string.add_cards_from_alter_deck)
                flipBtn.isEnabled = false
                prevBtn.isEnabled = false
                nextBtn.isEnabled = false
            } else {
                showCurrentCard()
            }
        }
    }

    // Show the current card on screen.
    private fun showCurrentCard() {
        if (cards.isEmpty()) return

        showingFront = true
        val card = cards[currentIndex]
        cardCountText.text = getString(R.string.card_position, currentIndex + 1, cards.size)
        cardContentText.text = card.front
        cardContentText.setTypeface(null, Typeface.BOLD)
        prevBtn.isEnabled = currentIndex > 0
        nextBtn.isEnabled = currentIndex < cards.size - 1
    }

    // Flip between front and back of the current card.
    private fun flipCard() {
        if (cards.isEmpty()) return

        val card = cards[currentIndex]
        showingFront = !showingFront
        if (showingFront) {
            cardContentText.text = card.front
            cardContentText.setTypeface(null, Typeface.BOLD)
        } else {
            cardContentText.text = card.back
            cardContentText.setTypeface(null, Typeface.NORMAL)
        }
    }

    // Move to the previous card.
    private fun showPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            showCurrentCard()
        }
    }

    // Move to the next card.
    private fun showNext() {
        if (currentIndex < cards.size - 1) {
            currentIndex++
            showCurrentCard()
        }
    }
}

