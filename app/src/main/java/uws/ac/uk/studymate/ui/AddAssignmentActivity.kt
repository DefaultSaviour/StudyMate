package uws.ac.uk.studymate.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.AssignmentViewModel

class AddAssignmentActivity : AppCompatActivity() {

    private val viewModel: AssignmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_assignment)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        val titleInput = findViewById<EditText>(R.id.assignmentTitleInput)
        val dueDateInput = findViewById<EditText>(R.id.dueDateInput)
        val subjectSpinner = findViewById<Spinner>(R.id.subjectSpinner)
        val saveAssignmentBtn = findViewById<Button>(R.id.saveAssignmentBtn)

        // TEMP list for UI testing
        // TODO connect to Subject database
        // TODO load subject names for the logged-in user
        val subjects = listOf(
            "Mobile Development",
            "History",
            "Maths",
            "Add Subject +"
        )

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            subjects
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        subjectSpinner.adapter = spinnerAdapter

        backBtn.setOnClickListener {
            finish()
        }

        subjectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSubject = subjects[position]

                if (selectedSubject == "Add Subject +") {
                    // TODO connect back to Subjects page properly
                    // TODO refresh spinner after subject is added
                    startActivity(Intent(this@AddAssignmentActivity, SubjectsActivity::class.java))
                }

                // TODO connect selected spinner item to Subject database ID
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        dueDateInput.setOnClickListener {
            // TODO connect selected date/time to Assignment data
            showDateTimePicker(dueDateInput)
        }

        saveAssignmentBtn.setOnClickListener {
            val title = titleInput.text.toString()
            val dueDate = dueDateInput.text.toString()

            // TEMP test values
            // TODO connect userId to logged-in user
            // TODO connect subjectId to selected subject from database
            val userId = 1
            val subjectId = 2

            if (title.isNotEmpty()) {
                // TODO save assignment to Assignment database
                // TODO connect selected subject properly before saving
                viewModel.addAssignment(
                    userId = userId,
                    subjectId = subjectId,
                    title = title,
                    dueDate = if (dueDate.isBlank()) null else dueDate
                )

                Toast.makeText(this, "Assignment saved", Toast.LENGTH_SHORT).show()

                // TODO refresh Assignments page after save
                finish()
            } else {
                Toast.makeText(this, "Enter an assignment title", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDateTimePicker(dueDateInput: EditText) {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->

            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val formattedDateTime = String.format(
                    "%02d/%02d/%04d %02d:%02d",
                    selectedDay,
                    selectedMonth + 1,
                    selectedYear,
                    selectedHour,
                    selectedMinute
                )

                // TODO connect chosen due date to Assignment database field
                dueDateInput.setText(formattedDateTime)
            }, hour, minute, true).show()

        }, year, month, day).show()
    }
}