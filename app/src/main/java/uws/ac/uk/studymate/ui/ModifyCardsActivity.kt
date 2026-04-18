package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
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
17/04/26
updated 18/04/26
*//////////////////////
class ModifyCardsActivity : AppCompatActivity() {

    private lateinit var cardsContainer: LinearLayout
    private lateinit var emptyText: TextView
    private var deckId = -1
    private var deckName = "Deck"

    /**
     This screen shows a list of cards in the deck.
     Tapping a card opens the edit screen so the user can change its text.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_cards)

        deckId = intent.getIntExtra("deck_id", -1)
        deckName = intent.getStringExtra("deck_name") ?: "Deck"

        val backBtn: Button = findViewById(R.id.modifyCardsBackBtn)
        val titleText: TextView = findViewById(R.id.modifyCardsTitleText)
        val homeBtn: Button = findViewById(R.id.modifyCardsHomeBtn)
        emptyText = findViewById(R.id.modifyCardsEmptyText)
        cardsContainer = findViewById(R.id.modifyCardsContainer)

        titleText.text = deckName

        // Return to the previous screen when the back button is pressed.
        backBtn.setOnClickListener { finish() }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the card list each time the user returns so edits are visible.
        loadCards()
    }

    // Load all cards from the deck and show them as tappable buttons.
    private fun loadCards() {
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                StudyMateDatabase.getInstance(application).cardDao().getCards(deckId)
            }

            showCards(loaded)
        }
    }

    // Show each card as a tappable button with the front text.
    private fun showCards(cards: List<FlashCard>) {
        cardsContainer.removeAllViews()

        if (cards.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            return
        }

        emptyText.visibility = View.GONE

        cards.forEach { card ->
            cardsContainer.addView(
                Button(this).apply {
                    text = getString(R.string.flashcard_question_answer, card.front, card.back)
                    isAllCaps = false
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        topMargin = (8 * resources.displayMetrics.density).toInt()
                    }
                    setOnClickListener {
                        startActivity(
                            Intent().setClassName(packageName, "$packageName.ui.EditCardActivity")
                                .putExtra("card_id", card.id)
                                .putExtra("card_front", card.front)
                                .putExtra("card_back", card.back)
                                .putExtra("deck_id", deckId)
                                .putExtra("user_id", card.userId)
                        )
                    }
                }
            )
        }
    }
}

