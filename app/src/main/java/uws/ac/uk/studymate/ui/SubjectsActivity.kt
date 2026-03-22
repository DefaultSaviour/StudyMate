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
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.ui.viewmodels.SubjectViewModel

class SubjectsActivity : AppCompatActivity() {

    private val viewModel: SubjectViewModel by viewModels()
    private var currentSubjects: List<Subject> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subjects)

        val input = findViewById<EditText>(R.id.subjectInput)
        val button = findViewById<Button>(R.id.addSubjectBtn)
        val listView = findViewById<ListView>(R.id.subjectsList)

        // Temp user ID
        val userId = 1 // replaced with logged-in user

        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )

        listView.adapter = adapter

        viewModel.loadSubjects(userId)

        viewModel.subjects.observe(this) { subjects ->
            currentSubjects = subjects
            adapter.clear()
            adapter.addAll(subjects.map { it.name })
        }

        button.setOnClickListener {
            val text = input.text.toString()

            if (text.isNotEmpty()) {
                viewModel.addSubject(userId, text, null)
                input.text.clear()
            } else {
                Toast.makeText(this, "Enter a subject", Toast.LENGTH_SHORT).show()
            }
        }

        //added long press for delete
        //check with team
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val subjectToDelete = currentSubjects[position]
            viewModel.deleteSubject(userId, subjectToDelete)
            Toast.makeText(this, "${subjectToDelete.name} deleted", Toast.LENGTH_SHORT).show()
            true
        }
    }
}