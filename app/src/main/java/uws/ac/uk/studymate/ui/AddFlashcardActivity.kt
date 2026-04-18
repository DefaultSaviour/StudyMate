package uws.ac.uk.studymate.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R

class AddFlashcardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_card)

        val deckSpinner = findViewById<Spinner>(R.id.deckSpinner)
        val questionInput = findViewById<EditText>(R.id.questionInput)
        val answerInput = findViewById<EditText>(R.id.answerInput)
        val saveButton = findViewById<Button>(R.id.saveCardButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        val decks = listOf("Mobile Development", "Database Systems")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            decks
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deckSpinner.adapter = adapter

        saveButton.setOnClickListener {
            val selectedDeck = deckSpinner.selectedItem.toString()
            val question = questionInput.text.toString().trim()
            val answer = answerInput.text.toString().trim()

            if (question.isEmpty() || answer.isEmpty()) {
                Toast.makeText(this, "Please enter both question and answer", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Card saved to $selectedDeck", Toast.LENGTH_SHORT).show()
                questionInput.text.clear()
                answerInput.text.clear()
                deckSpinner.setSelection(0)
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }
}
