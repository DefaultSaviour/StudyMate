package uws.ac.uk.studymate.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        val deckId = intent.getIntExtra("deck_id", -1)
        val deckName = intent.getStringExtra("deck_name") ?: "Deck"

        // Build a simple screen in code so this page matches the current app setup.
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
        }

        val backBtn = Button(this).apply {
            text = "Back"
        }

        val titleText = TextView(this).apply {
            text = deckName
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = "Home"
        }

        cardCountText = TextView(this).apply {
            text = "Loading..."
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (20 * resources.displayMetrics.density).toInt()
            }
        }

        // The card area shows the front or back text inside a simple bordered box.
        cardContentText = TextView(this).apply {
            text = ""
            textSize = 20f
            gravity = Gravity.CENTER
            val cardPadding = (32 * resources.displayMetrics.density).toInt()
            setPadding(cardPadding, cardPadding, cardPadding, cardPadding)
            minHeight = (200 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
            }
            background = buildCardBackground()
            setOnClickListener { flipCard() }
        }

        flipBtn = Button(this).apply {
            text = "Flip"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
            }
        }

        val navRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }

        prevBtn = Button(this).apply {
            text = "Previous"
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
        }

        nextBtn = Button(this).apply {
            text = "Next"
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginStart = (8 * resources.displayMetrics.density).toInt()
            }
        }

        navRow.addView(prevBtn)
        navRow.addView(nextBtn)

        headerRow.addView(backBtn)
        headerRow.addView(titleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(cardCountText)
        contentLayout.addView(cardContentText)
        contentLayout.addView(flipBtn)
        contentLayout.addView(navRow)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )

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
                cardCountText.text = "This deck has no cards yet"
                cardContentText.text = "Add cards from the alter deck screen"
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
        cardCountText.text = "Card ${currentIndex + 1} of ${cards.size}"
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

    // Build a simple bordered box for the card display area.
    private fun buildCardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f * resources.displayMetrics.density
            setColor(Color.parseColor("#F7F7F7"))
            setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#D8D8D8"))
        }
    }
}

