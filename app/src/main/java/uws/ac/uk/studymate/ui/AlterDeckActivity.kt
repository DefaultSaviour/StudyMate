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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.data.StudyMateDatabase
/*//////////////////////
Coded by Jamie Coleman
 18/04/26
*//////////////////////
class AlterDeckActivity : AppCompatActivity() {

    private var deckId = -1
    private var deckName = "Deck"

    /**
     This screen gives the user four ways to change a deck:
     add a card, remove cards, modify cards, or delete the whole deck.
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
            text = deckName
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = "Home"
        }

        val addCardBtn = Button(this).apply {
            text = "Add card"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (24 * resources.displayMetrics.density).toInt()
            }
            minHeight = (64 * resources.displayMetrics.density).toInt()
        }

        val removeCardsBtn = Button(this).apply {
            text = "Remove cards"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
            minHeight = (64 * resources.displayMetrics.density).toInt()
        }

        val modifyCardsBtn = Button(this).apply {
            text = "Modify cards"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
            minHeight = (64 * resources.displayMetrics.density).toInt()
        }

        val deleteDeckBtn = Button(this).apply {
            text = "Delete deck"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (24 * resources.displayMetrics.density).toInt()
            }
            minHeight = (64 * resources.displayMetrics.density).toInt()
        }

        headerRow.addView(backBtn)
        headerRow.addView(titleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(addCardBtn)
        contentLayout.addView(removeCardsBtn)
        contentLayout.addView(modifyCardsBtn)
        contentLayout.addView(deleteDeckBtn)

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

        // Open the add card screen for this deck.
        addCardBtn.setOnClickListener {
            startActivity(
                Intent().setClassName(packageName, "$packageName.ui.AddCardActivity")
                    .putExtra("deck_id", deckId)
                    .putExtra("deck_name", deckName)
            )
        }

        // Open the remove cards screen for this deck.
        removeCardsBtn.setOnClickListener {
            startActivity(
                Intent().setClassName(packageName, "$packageName.ui.RemoveCardsActivity")
                    .putExtra("deck_id", deckId)
                    .putExtra("deck_name", deckName)
            )
        }

        // Open the modify cards screen for this deck.
        modifyCardsBtn.setOnClickListener {
            startActivity(
                Intent().setClassName(packageName, "$packageName.ui.ModifyCardsActivity")
                    .putExtra("deck_id", deckId)
                    .putExtra("deck_name", deckName)
            )
        }

        // Ask for confirmation before deleting the entire deck.
        deleteDeckBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete deck")
                .setMessage("This will delete \"$deckName\" and all its cards. Do you want to continue?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteDeck()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // Delete all cards in the deck first, then delete the deck itself.
    private fun deleteDeck() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = StudyMateDatabase.getInstance(application)
                db.cardDao().deleteCardsByDeck(deckId)
                val deck = db.deckDao().getDecks(
                    uws.ac.uk.studymate.util.SessionManager(this@AlterDeckActivity)
                        .getLoggedInUserId() ?: return@withContext
                ).firstOrNull { it.id == deckId } ?: return@withContext
                db.deckDao().delete(deck)
            }
            Toast.makeText(this@AlterDeckActivity, "Deck deleted", Toast.LENGTH_SHORT).show()
            // Return to the flashcard decks list after deleting.
            val intent = Intent().setClassName(packageName, "$packageName.ui.FlashcardDecksActivity")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}

