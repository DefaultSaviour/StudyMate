package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
class RemoveCardsActivity : AppCompatActivity() {

    private lateinit var cardsContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var removeBtn: Button

    private var cards: List<FlashCard> = emptyList()
    private val selectedIds = mutableSetOf<Int>()

    /**
     This screen shows a list of cards with checkboxes so the user
     can tick the ones they want to remove, then press the remove button.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_cards)

        val deckId = intent.getIntExtra("deck_id", -1)

        val backBtn: Button = findViewById(R.id.removeCardsBackBtn)
        val titleText: TextView = findViewById(R.id.removeCardsTitleText)
        val homeBtn: Button = findViewById(R.id.removeCardsHomeBtn)
        emptyText = findViewById(R.id.removeCardsEmptyText)
        cardsContainer = findViewById(R.id.removeCardsContainer)
        removeBtn = findViewById(R.id.removeSelectedBtn)

        titleText.text = getString(R.string.remove_cards_title)

        // Return to the previous screen when the back button is pressed.
        backBtn.setOnClickListener { finish() }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
        }

        // Remove all selected cards when the button is pressed.
        removeBtn.setOnClickListener {
            removeSelectedCards(deckId)
        }

        // Load the cards for this deck.
        loadCards(deckId)
    }

    // Load all cards from the deck and show them with checkboxes.
    private fun loadCards(deckId: Int) {
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                StudyMateDatabase.getInstance(application).cardDao().getCards(deckId)
            }

            cards = loaded
            selectedIds.clear()
            showCards()
        }
    }

    // Show each card as a checkbox row with the front text.
    private fun showCards() {
        cardsContainer.removeAllViews()

        if (cards.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            removeBtn.isEnabled = false
            return
        }

        emptyText.visibility = View.GONE

        cards.forEach { card ->
            cardsContainer.addView(
                CheckBox(this).apply {
                    text = getString(R.string.flashcard_question_answer, card.front, card.back)
                    textSize = 16f
                    val topPadding = (8 * resources.displayMetrics.density).toInt()
                    setPadding(paddingLeft, topPadding, paddingRight, topPadding)
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) selectedIds.add(card.id) else selectedIds.remove(card.id)
                        removeBtn.isEnabled = selectedIds.isNotEmpty()
                    }
                }
            )
        }
    }

    // Delete the selected cards from the database and refresh the list.
    private fun removeSelectedCards(deckId: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = StudyMateDatabase.getInstance(application)
                cards.filter { it.id in selectedIds }.forEach { card ->
                    db.cardDao().delete(card)
                }
            }
            Toast.makeText(this@RemoveCardsActivity, getString(R.string.cards_removed_message), Toast.LENGTH_SHORT).show()
            loadCards(deckId)
        }
    }
}


