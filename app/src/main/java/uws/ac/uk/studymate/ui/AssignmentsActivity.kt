package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.AssignmentViewModel

class AssignmentsActivity : AppCompatActivity() {

    private val viewModel: AssignmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignments)

        val addButton = findViewById<Button>(R.id.addAssignmentBtn)
        val listView = findViewById<ListView>(R.id.assignmentsList)

        // TEMP: test user ID
        val userId = 1

        // Adapter to display assignments
        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        listView.adapter = adapter

        // Observe assignments from ViewModel
        viewModel.loadAssignments(userId)
        viewModel.assignments.observe(this) { assignments ->
            adapter.clear()
            adapter.addAll(assignments.map { "${it.title} - ${it.dueDate ?: "No due date"}" })
        }

        // Open AddAssignmentActivity when button clicked
        addButton.setOnClickListener {
            startActivity(Intent(this, AddAssignmentActivity::class.java))
        }

        // Click an assignment to open AssignmentDetailActivity
        listView.setOnItemClickListener { _, _, position, _ ->
            val assignment = viewModel.assignments.value?.get(position)
            val intent = Intent(this, AssignmentDetailActivity::class.java).apply {
                putExtra("title", assignment?.title)
                putExtra("subject", "Sample Subject") // TEMP placeholder
                putExtra("dueDate", assignment?.dueDate)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = 1 // TEMP: replace with logged-in user ID
        viewModel.loadAssignments(userId)
    }
}