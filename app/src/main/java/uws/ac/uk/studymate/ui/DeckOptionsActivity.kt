package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R
/*//////////////////////
Coded by Jamie Coleman
 17/04/26
 *//////////////////////
class DeckOptionsActivity : AppCompatActivity() {

    /**
     This screen shows two choices for a selected deck: review or alter.
     It receives the deck ID and name through intent extras.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_options)

        val deckId = intent.getIntExtra("deck_id", -1)
        val deckName = intent.getStringExtra("deck_name") ?: "Deck"

        val backBtn: Button = findViewById(R.id.deckOptionsBackBtn)
        val titleText: TextView = findViewById(R.id.deckOptionsTitleText)
        val homeBtn: Button = findViewById(R.id.deckOptionsHomeBtn)
        val reviewDeckBtn: Button = findViewById(R.id.reviewDeckBtn)
        val alterDeckBtn: Button = findViewById(R.id.alterDeckBtn)

        titleText.text = deckName

        // Return to the previous screen when the back button is pressed.
        backBtn.setOnClickListener {
            finish()
        }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
        }

        // Open the card reviewer for this deck.
        reviewDeckBtn.setOnClickListener {
            startActivity(
                Intent().setClassName(packageName, "$packageName.ui.ReviewDeckActivity")
                    .putExtra("deck_id", deckId)
                    .putExtra("deck_name", deckName)
            )
        }

        // Open the alter deck screen for this deck.
        alterDeckBtn.setOnClickListener {
            startActivity(
                Intent().setClassName(packageName, "$packageName.ui.AlterDeckActivity")
                    .putExtra("deck_id", deckId)
                    .putExtra("deck_name", deckName)
            )
        }
    }
}

