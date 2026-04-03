package uws.ac.uk.studymate.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R

class FlashcardViewerActivity : AppCompatActivity() {

    private lateinit var deckNameText: TextView
    private lateinit var cardText: TextView
    private lateinit var flipButton: Button
    private lateinit var nextButton: Button
    private lateinit var previousButton: Button
    private lateinit var flashcardCard: CardView
    private lateinit var backToDecksText: TextView

    private var showingQuestion = true
    private var currentIndex = 0

    private val questions = listOf(
        "What is Room in Android?",
        "What is an Activity?",
        "What is SQLite?"
    )

    private val answers = listOf(
        "A persistence library built on top of SQLite.",
        "A screen in an Android app.",
        "A lightweight local database."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard_viewer)

        deckNameText = findViewById(R.id.deckNameText)
        cardText = findViewById(R.id.cardText)
        flipButton = findViewById(R.id.flipButton)
        nextButton = findViewById(R.id.nextButton)
        previousButton = findViewById(R.id.previousButton)
        flashcardCard = findViewById(R.id.flashcardCard)
        backToDecksText = findViewById(R.id.backToDecksText)

        val deckName = intent.getStringExtra("deckName") ?: "Flashcards"
        deckNameText.text = deckName

        showQuestion()

        backToDecksText.setOnClickListener {
            finish()
        }

        flashcardCard.setOnClickListener {
            flipCard()
        }

        flipButton.setOnClickListener {
            flipCard()
        }

        nextButton.setOnClickListener {
            currentIndex = (currentIndex + 1) % questions.size
            showQuestion()
        }

        previousButton.setOnClickListener {
            currentIndex = if (currentIndex - 1 < 0) questions.size - 1 else currentIndex - 1
            showQuestion()
        }
    }

    private fun flipCard() {
        if (showingQuestion) {
            showAnswer()
        } else {
            showQuestion()
        }
    }

    private fun showQuestion() {
        cardText.text = questions[currentIndex]
        flipButton.text = "Show Answer"
        showingQuestion = true
    }

    private fun showAnswer() {
        cardText.text = answers[currentIndex]
        flipButton.text = "Show Question"
        showingQuestion = false
    }
}
