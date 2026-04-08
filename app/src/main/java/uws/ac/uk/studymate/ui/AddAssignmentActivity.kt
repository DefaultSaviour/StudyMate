package uws.ac.uk.studymate.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.AssignmentViewModel
import java.util.Calendar

class AddAssignmentActivity : AppCompatActivity() {

    private val viewModel: AssignmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_assignment)

        // --- Views ---
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        val titleInput = findViewById<EditText>(R.id.assignmentTitleInput)
        val dueDateInput = findViewById<EditText>(R.id.dueDateInput)
        val subjectSpinner = findViewById<Spinner>(R.id.subjectSpinner)
        val saveBtn = findViewById<Button>(R.id.saveAssignmentBtn)

        // --- Temporary subjects list (for UI testing) ---
        val subjects = listOf("Mobile Development", "History", "Maths", "Add Subject +")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subjects)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        subjectSpinner.adapter = spinnerAdapter

        // --- Back button ---
        backBtn.setOnClickListener { finish() }

        // --- Spinner selection ---
        var selectedSubjectIndex = 0
        subjectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = subjects[position]
                selectedSubjectIndex = position

                if (selected == "Add Subject +") {
                    // TODO connect back to Subjects page properly
                    Toast.makeText(this@AddAssignmentActivity, "Go add a new subject", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // --- Due date picker ---
        dueDateInput.setOnClickListener {
            showDateTimePicker(dueDateInput)
        }

        // --- Save assignment button ---
        saveBtn.setOnClickListener {
            val title = titleInput.text.toString()
            val dueDate = dueDateInput.text.toString()

            if (title.isBlank()) {
                Toast.makeText(this, "Enter an assignment title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TEMP: replace with actual logged-in user and subject ID
            val userId = 1
            val subjectId = selectedSubjectIndex + 1 // temporary mapping

            // Add assignment via ViewModel
            viewModel.addAssignment(userId, subjectId, title, if (dueDate.isBlank()) null else dueDate)

            Toast.makeText(this, "Assignment saved", Toast.LENGTH_SHORT).show()

            // Finish activity and return to AssignmentsActivity
            finish()
        }
    }

    // --- Date and Time picker helper ---
    private fun showDateTimePicker(dueDateInput: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val formatted = String.format(
                    "%02d/%02d/%04d %02d:%02d",
                    selectedDay,
                    selectedMonth + 1,
                    selectedYear,
                    selectedHour,
                    selectedMinute
                )
                dueDateInput.setText(formatted)
            }, hour, minute, true).show()

        }, year, month, day).show()
    }
}