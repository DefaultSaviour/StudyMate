package uws.ac.uk.studymate.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.AssignmentViewModel

class AssignmentsActivity : AppCompatActivity() {

    private val viewModel: AssignmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignments)

        val titleInput = findViewById<EditText>(R.id.assignmentTitleInput)
        val dueDateInput = findViewById<EditText>(R.id.dueDateInput)
        val addButton = findViewById<Button>(R.id.addAssignmentBtn)
        val listView = findViewById<ListView>(R.id.assignmentsList)

        // TEMP: using test user/subject IDs for now while testing
        // TODO: replace with real logged-in user ID and selected subject ID later
        val userId = 1
        val subjectId = 2

        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )

        listView.adapter = adapter

        viewModel.loadAssignments(userId)

        viewModel.assignments.observe(this) { assignments ->
            adapter.clear()
            adapter.addAll(
                assignments.map { "${it.title} - ${it.dueDate ?: "No due date"}" }
            )
        }

        addButton.setOnClickListener {
            val title = titleInput.text.toString()
            //replace manual date with proper date picking
            val dueDate = dueDateInput.text.toString()

            if (title.isNotEmpty()) {
                viewModel.addAssignment(
                    userId = userId,
                    subjectId = subjectId,
                    title = title,
                    dueDate = if (dueDate.isBlank()) null else dueDate
                )
                titleInput.text.clear()
                dueDateInput.text.clear()
            } else {
                Toast.makeText(this, "Enter an assignment title", Toast.LENGTH_SHORT).show()
            }
        }
    }
}