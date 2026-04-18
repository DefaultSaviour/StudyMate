package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
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
        setContentView(R.layout.activity_alter_deck)

        deckId = intent.getIntExtra("deck_id", -1)
        deckName = intent.getStringExtra("deck_name") ?: "Deck"

        val backBtn: Button = findViewById(R.id.alterDeckBackBtn)
        val titleText: TextView = findViewById(R.id.alterDeckTitleText)
        val homeBtn: Button = findViewById(R.id.alterDeckHomeBtn)
        val addCardBtn: Button = findViewById(R.id.addCardBtn)
        val removeCardsBtn: Button = findViewById(R.id.removeCardsBtn)
        val modifyCardsBtn: Button = findViewById(R.id.modifyCardsBtn)
        val deleteDeckBtn: Button = findViewById(R.id.deleteDeckBtn)

        titleText.text = deckName

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
                .setTitle(R.string.delete_deck_title)
                .setMessage(getString(R.string.delete_deck_message, deckName))
                .setPositiveButton(R.string.delete_button) { _, _ ->
                    deleteDeck()
                }
                .setNegativeButton(R.string.cancel_button, null)
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
            Toast.makeText(this@AlterDeckActivity, getString(R.string.deck_deleted_message), Toast.LENGTH_SHORT).show()
            // Return to the flashcard decks list after deleting.
            val intent = Intent().setClassName(packageName, "$packageName.ui.FlashcardDecksActivity")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}

