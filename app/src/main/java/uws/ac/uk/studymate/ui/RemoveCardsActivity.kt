package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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

        val deckId = intent.getIntExtra("deck_id", -1)

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
            text = "Remove cards"
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

        removeBtn = Button(this).apply {
            text = "Remove selected"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (20 * resources.displayMetrics.density).toInt()
            }
            isEnabled = false
        }

        headerRow.addView(backBtn)
        headerRow.addView(titleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(emptyText)
        contentLayout.addView(cardsContainer)
        contentLayout.addView(removeBtn)

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
            emptyText.visibility = TextView.VISIBLE
            removeBtn.isEnabled = false
            return
        }

        emptyText.visibility = TextView.GONE

        cards.forEach { card ->
            cardsContainer.addView(
                CheckBox(this).apply {
                    text = "${card.front} -- ${card.back}"
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
            Toast.makeText(this@RemoveCardsActivity, "Cards removed", Toast.LENGTH_SHORT).show()
            loadCards(deckId)
        }
    }
}


