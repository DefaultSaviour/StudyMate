package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
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
import uws.ac.uk.studymate.util.SessionManager
/*//////////////////////
Coded by Jamie Coleman
18/04/26
*//////////////////////
class AddCardActivity : AppCompatActivity() {

    /**
     This screen lets the user add a new flashcard to a deck.
     It has two text fields for the front and back of the card.
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
            text = "Add card"
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = "Home"
        }

        val frontLabel = TextView(this).apply {
            text = "Front (question)"
            val topPadding = (20 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val frontInput = EditText(this).apply {
            hint = "Enter front text"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        val backLabel = TextView(this).apply {
            text = "Back (answer)"
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val backInput = EditText(this).apply {
            hint = "Enter back text"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        val saveBtn = Button(this).apply {
            text = "Save card"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (24 * resources.displayMetrics.density).toInt()
            }
        }

        headerRow.addView(backBtn)
        headerRow.addView(titleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(frontLabel)
        contentLayout.addView(frontInput)
        contentLayout.addView(backLabel)
        contentLayout.addView(backInput)
        contentLayout.addView(saveBtn)

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

        // Save the new card when the user presses the button.
        saveBtn.setOnClickListener {
            val front = frontInput.text.toString().trim()
            val back = backInput.text.toString().trim()

            if (front.isEmpty()) {
                Toast.makeText(this, "Enter the front text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (back.isEmpty()) {
                Toast.makeText(this, "Enter the back text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = SessionManager(this).getLoggedInUserId()
            if (userId == null) {
                Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val db = StudyMateDatabase.getInstance(application)
                    db.cardDao().insert(
                        FlashCard(
                            userId = userId,
                            deckId = deckId,
                            front = front,
                            back = back
                        )
                    )
                }
                Toast.makeText(this@AddCardActivity, "Card added", Toast.LENGTH_SHORT).show()
                frontInput.text.clear()
                backInput.text.clear()
            }
        }
    }
}


