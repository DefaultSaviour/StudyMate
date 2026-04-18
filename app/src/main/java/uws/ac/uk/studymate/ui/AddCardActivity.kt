package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
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
        setContentView(R.layout.activity_add_card)

        val deckId = intent.getIntExtra("deck_id", -1)

        val backBtn: Button = findViewById(R.id.addCardBackBtn)
        val homeBtn: Button = findViewById(R.id.addCardHomeBtn)
        val frontInput: EditText = findViewById(R.id.frontInput)
        val backInput: EditText = findViewById(R.id.backInput)
        val saveBtn: Button = findViewById(R.id.saveCardBtn)

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
                Toast.makeText(this, getString(R.string.enter_front_text_message), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (back.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_back_text_message), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = SessionManager(this).getLoggedInUserId()
            if (userId == null) {
                Toast.makeText(this, getString(R.string.session_expired_message), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@AddCardActivity, getString(R.string.card_added_message), Toast.LENGTH_SHORT).show()
                frontInput.text.clear()
                backInput.text.clear()
            }
        }
    }
}


