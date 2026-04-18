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
/*//////////////////////
Coded by Jamie Coleman
17/04/26
*//////////////////////
class EditCardActivity : AppCompatActivity() {

    /**
     This screen lets the user edit an existing flashcard.
     It uses the same layout as the add card screen but pre-fills the fields.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_card)

        val cardId = intent.getIntExtra("card_id", -1)
        val cardFront = intent.getStringExtra("card_front") ?: ""
        val cardBack = intent.getStringExtra("card_back") ?: ""
        val deckId = intent.getIntExtra("deck_id", -1)
        val userId = intent.getIntExtra("user_id", -1)

        val backBtn: Button = findViewById(R.id.editCardBackBtn)
        val homeBtn: Button = findViewById(R.id.editCardHomeBtn)
        val frontInput: EditText = findViewById(R.id.frontInput)
        val backInput: EditText = findViewById(R.id.backInput)
        val saveBtn: Button = findViewById(R.id.saveChangesBtn)

        frontInput.setText(cardFront)
        backInput.setText(cardBack)

        // Return to the previous screen when the back button is pressed.
        backBtn.setOnClickListener { finish() }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
        }

        // Save the updated card when the user presses the button.
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

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val db = StudyMateDatabase.getInstance(application)
                    db.cardDao().update(
                        FlashCard(
                            id = cardId,
                            userId = userId,
                            deckId = deckId,
                            front = front,
                            back = back
                        )
                    )
                }
                Toast.makeText(this@EditCardActivity, getString(R.string.card_updated_message), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

