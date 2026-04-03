package uws.ac.uk.studymate.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R

class AddFlashcardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_add_flashcard)

        // Views
        val deckSpinner = findViewById<Spinner>(R.id.deckSpinner)
        val questionInput = findViewById<EditText>(R.id.questionInput)
        val answerInput = findViewById<EditText>(R.id.answerInput)
        val saveButton = findViewById<Button>(R.id.saveCardButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        // SAVE BUTTON
        saveButton.setOnClickListener {
            val selectedDeck = deckSpinner.selectedItem.toString()
            val question = questionInput.text.toString().trim()
            val answer = answerInput.text.toString().trim()

            if (question.isEmpty() || answer.isEmpty()) {
                Toast.makeText(this, "Please enter both question and answer", Toast.LENGTH_SHORT).show()
            } else {
                // For now just show confirmation (we’ll store later)
                Toast.makeText(this, "Card saved to $selectedDeck", Toast.LENGTH_SHORT).show()

                // Clear inputs
                questionInput.text.clear()
                answerInput.text.clear()
            }
        }

        // CANCEL BUTTON
        cancelButton.setOnClickListener {
            finish() // goes back to FlashcardDeckActivity
        }
    }
}
