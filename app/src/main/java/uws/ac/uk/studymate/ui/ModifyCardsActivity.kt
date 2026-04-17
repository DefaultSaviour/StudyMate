package uws.ac.uk.studymate.ui

import android.content.Intent
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
17/04/26
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

        deckId = intent.getIntExtra("deck_id", -1)
        deckName = intent.getStringExtra("deck_name") ?: "Deck"

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
            text = "Modify cards"
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = "Home"
        }

        emptyText = TextView(this).apply {
            text = "No cards in this deck"
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        cardsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        headerRow.addView(backBtn)
        headerRow.addView(titleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(emptyText)
        contentLayout.addView(cardsContainer)

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
            emptyText.visibility = TextView.VISIBLE
            return
        }

        emptyText.visibility = TextView.GONE

        cards.forEach { card ->
            cardsContainer.addView(
                Button(this).apply {
                    text = "${card.front} -- ${card.back}"
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

