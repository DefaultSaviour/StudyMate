package uws.ac.uk.studymate.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R

class AssignmentDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignment_detail)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        backBtn.setOnClickListener { finish() }

        val titleText = findViewById<TextView>(R.id.detailTitle)
        val subjectText = findViewById<TextView>(R.id.detailSubject)
        val dueDateText = findViewById<TextView>(R.id.detailDueDate)

        // Get assignment info from Intent extras
        val assignmentTitle = intent.getStringExtra("title") ?: "Sample Assignment"
        val assignmentSubject = intent.getStringExtra("subject") ?: "Sample Subject"
        val assignmentDue = intent.getStringExtra("dueDate") ?: "01/04/2026"

        titleText.text = assignmentTitle
        subjectText.text = assignmentSubject
        dueDateText.text = assignmentDue
    }
}