package uws.ac.uk.studymate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.ui.AssignmentsActivity
import uws.ac.uk.studymate.ui.FlashcardDeckActivity
import uws.ac.uk.studymate.ui.SubjectsActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_dashboard)

        val subjectsButton = findViewById<Button>(R.id.subjectsButton)
        val assignmentsButton = findViewById<Button>(R.id.assignmentsButton)
        val flashcardsButton = findViewById<Button>(R.id.flashcardsButton)

        subjectsButton.setOnClickListener {
            startActivity(Intent(this, SubjectsActivity::class.java))
        }

        assignmentsButton.setOnClickListener {
            startActivity(Intent(this, AssignmentsActivity::class.java))
        }

        flashcardsButton.setOnClickListener {
            startActivity(Intent(this, FlashcardDeckActivity::class.java))
        }
    }
}