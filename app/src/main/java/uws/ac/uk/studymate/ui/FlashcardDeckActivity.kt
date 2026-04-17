package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R

class FlashcardDeckActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard_deck)

        val backToDashboardText = findViewById<TextView>(R.id.backToDashboardText)
        val mobileDeckButton = findViewById<Button>(R.id.mobileDeckButton)
        val databaseDeckButton = findViewById<Button>(R.id.databaseDeckButton)
        val addCardButton = findViewById<Button>(R.id.addCardButton)

        // Back to Dashboard
        backToDashboardText.setOnClickListener {
            finish()
        }

        // Open Mobile Deck
        mobileDeckButton.setOnClickListener {
            val intent = Intent(this, FlashcardViewerActivity::class.java)
            intent.putExtra("deckName", "Mobile Development")
            startActivity(intent)
        }

        // Open Database Deck
        databaseDeckButton.setOnClickListener {
            val intent = Intent(this, FlashcardViewerActivity::class.java)
            intent.putExtra("deckName", "Database Systems")
            startActivity(intent)
        }

        // Add New Flashcard
        addCardButton.setOnClickListener {
            startActivity(Intent(this, AddFlashcardActivity::class.java))
        }
    }
}
