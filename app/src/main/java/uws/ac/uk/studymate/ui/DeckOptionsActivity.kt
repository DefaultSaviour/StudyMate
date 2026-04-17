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

        val reviewDeckBtn = Button(this).apply {
            text = "Review deck"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (32 * resources.displayMetrics.density).toInt()
            }
            minHeight = (72 * resources.displayMetrics.density).toInt()
        }

        val alterDeckBtn = Button(this).apply {
            text = "Alter deck"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
            }
            minHeight = (72 * resources.displayMetrics.density).toInt()
        }

        headerRow.addView(backBtn)
        headerRow.addView(titleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(reviewDeckBtn)
        contentLayout.addView(alterDeckBtn)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )

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

