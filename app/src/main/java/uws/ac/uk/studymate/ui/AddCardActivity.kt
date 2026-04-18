package uws.ac.uk.studymate.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R

class AddCardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_card)

        val addCardBackBtn = findViewById<Button>(R.id.addCardBackBtn)
        val addCardHomeBtn = findViewById<Button>(R.id.addCardHomeBtn)
        val saveCardBtn = findViewById<Button>(R.id.saveCardBtn)

        addCardBackBtn.setOnClickListener {
            finish()
        }

        addCardHomeBtn.setOnClickListener {
            finish()
        }

        saveCardBtn.setOnClickListener {
            finish()
        }
    }
}