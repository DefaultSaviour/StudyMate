package uws.ac.uk.studymate.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.relations.SubjectWithAssignments
import uws.ac.uk.studymate.ui.viewmodels.SubjectColorChoice
import uws.ac.uk.studymate.ui.viewmodels.SubjectsSummary
import uws.ac.uk.studymate.ui.viewmodels.SubjectsViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
/*//////////////////////
Coded by Jamie Coleman
06/04/26
updated 09/04/26
 *//////////////////////
class SubjectsActivity : AppCompatActivity() {

    private lateinit var subjectsVm: SubjectsViewModel
    private lateinit var screenTitleText: TextView
    private lateinit var removeSubjectSpinner: Spinner
    private lateinit var assignmentsOutputText: TextView
    private lateinit var addSubjectNameInput: EditText
    private lateinit var colorSpinner: Spinner

    private var subjectsWithAssignments: List<SubjectWithAssignments> = emptyList()
    private var colorChoices: List<SubjectColorChoice> = emptyList()
    private var selectedSubjectId: Int? = null

    /**
    This screen lets the user add subjects and remove them in one place.
    it started as a simple subject manager, and later got the assignment preview and delete warning.
    it now keeps the risky delete part and the add part separate so the page is easier to follow.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the ViewModel used by this screen.
        subjectsVm = ViewModelProvider(this)[SubjectsViewModel::class.java]

        //Use XML layout instead.
        setContentView(R.layout.activity_subjects)

        // Link  XML views so this screen matches the current app setup.
        screenTitleText = findViewById(R.id.screenTitleText)
        removeSubjectSpinner = findViewById(R.id.removeSubjectSpinner)
        assignmentsOutputText = findViewById(R.id.assignmentsOutputText)
        addSubjectNameInput = findViewById(R.id.addSubjectNameInput)
        colorSpinner = findViewById(R.id.colorSpinner)

        val backBtn: ImageButton = findViewById(R.id.backBtn)
        val homeBtn: ImageButton = findViewById(R.id.homeBtn)
        val deleteSubjectBtn: Button = findViewById(R.id.deleteSubjectBtn)
        val addSubjectBtn: Button = findViewById(R.id.addSubjectBtn)

        // Show the latest subject data when the ViewModel finishes loading it.
        subjectsVm.screenSummary.observe(this) { summary ->
            showScreen(summary)
        }

        // Send the user back to login when there is no valid session.
        subjectsVm.sessionExpired.observe(this) { expired ->
            if (expired) {
                openLogin()
            }
        }

        // Show validation and save messages from the ViewModel.
        subjectsVm.message.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                if (message == "Subject added") {
                    addSubjectNameInput.text.clear()
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

        // Return to the previous screen from the top-left button.
        backBtn.setOnClickListener {
            finish()
        }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
        }

        // Update the assignments list when the selected subject changes.
        removeSubjectSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedSubjectId = selectedSubject()?.id
                showAssignmentsForSelectedSubject()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedSubjectId = null
                assignmentsOutputText.text = getString(R.string.select_a_subject_to_see_its_assignments)            }
        }

        // Ask for confirmation before deleting the chosen subject.
        deleteSubjectBtn.setOnClickListener {
            val subject = selectedSubject()
            if (subject == null) {
                Toast.makeText(this, getString(R.string.choose_subject_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_subject_title))
                .setMessage(getString(R.string.delete_subject_message))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    subjectsVm.deleteSubject(subject)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Save a new subject when the user presses the button.
        addSubjectBtn.setOnClickListener {
            subjectsVm.addSubject(
                name = addSubjectNameInput.text.toString(),
                colorChoice = selectedColorChoice()
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the screen each time the user returns so the subject list stays current.
        subjectsVm.loadScreen()
    }

    // Replace this screen with the login screen when the session ends.
    private fun openLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    // Return to the main home screen from the top-right button.
    private fun openHome() {
        startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
    }

    // Show the latest title, subject dropdown data, and color choices.
    private fun showScreen(summary: SubjectsSummary) {
        screenTitleText.text = summary.titleText
        subjectsWithAssignments = summary.subjectsWithAssignments
        colorChoices = summary.colorChoices
        showSubjectDropdown(summary.subjectsWithAssignments)
        showColorDropdown(summary.colorChoices)
        showAssignmentsForSelectedSubject()
    }

    // Fill the remove-subject dropdown with the current subjects.
    private fun showSubjectDropdown(subjectsWithAssignments: List<SubjectWithAssignments>) {
        val names = if (subjectsWithAssignments.isEmpty()) {
            listOf(getString(R.string.no_subjects_available))        } else {
            subjectsWithAssignments.map { it.subject.name }
        }

        removeSubjectSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            names
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        if (subjectsWithAssignments.isEmpty()) {
            selectedSubjectId = null
            return
        }

        val selectedIndex = subjectsWithAssignments.indexOfFirst { it.subject.id == selectedSubjectId }
            .takeIf { it >= 0 } ?: 0
        removeSubjectSpinner.setSelection(selectedIndex)
        selectedSubjectId = subjectsWithAssignments[selectedIndex].subject.id
    }

    // Fill the color dropdown with a small fixed list of readable color names.
    private fun showColorDropdown(colorChoices: List<SubjectColorChoice>) {
        colorSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            colorChoices.map { it.label }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    // Show the assignments that belong to the selected subject.
    private fun showAssignmentsForSelectedSubject() {
        val selectedSubject = selectedSubjectWithAssignments()
        if (selectedSubject == null) {
            assignmentsOutputText.text = getString(R.string.select_a_subject_to_see_its_assignments)
            return
        }

        val assignments = selectedSubject.assignments.sortedWith(
            compareBy<Assignment> { parseDueDate(it.dueDate) == null }
                .thenBy { parseDueDate(it.dueDate) ?: LocalDateTime.MAX }
                .thenBy { it.title.lowercase() }
        )

        if (assignments.isEmpty()) {
            assignmentsOutputText.text = getString(R.string.no_assignments_subject)
            return
        }

        assignmentsOutputText.text = assignments.joinToString("\n\n") { assignment ->
            buildAssignmentText(assignment)
        }
    }

    // Return the currently selected subject from the remove-subject dropdown.
    private fun selectedSubject(): Subject? {
        return selectedSubjectWithAssignments()?.subject
    }

    // Return the currently selected subject together with its assignments.
    private fun selectedSubjectWithAssignments(): SubjectWithAssignments? {
        if (subjectsWithAssignments.isEmpty()) {
            return null
        }

        val position = removeSubjectSpinner.selectedItemPosition
        return subjectsWithAssignments.getOrNull(position)
    }

    // Return the currently selected color choice from the add-subject dropdown.
    private fun selectedColorChoice(): SubjectColorChoice? {
        if (colorChoices.isEmpty()) {
            return null
        }

        val position = colorSpinner.selectedItemPosition
        return colorChoices.getOrNull(position)
    }

    // Build a clear block of text for one assignment under the selected subject.
    private fun buildAssignmentText(assignment: Assignment): String {
        val dueAt = parseDueDate(assignment.dueDate)
        val dueText = if (dueAt == null) {
            getString(R.string.no_due_date)
        } else {
            formatDueDate(dueAt)
        }

        val upcomingText = if (dueAt != null && !dueAt.isBefore(LocalDateTime.now())) {
            "\n${getString(R.string.upcoming_due_date)}"
        } else {
            ""
        }

        return "${assignment.title}\nDue: $dueText$upcomingText"
    }

    // Try a few simple date formats so this screen can read saved assignment due dates.
    private fun parseDueDate(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) {
            return null
        }

        val trimmedValue = value.trim()

        try {
            return LocalDateTime.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (_: Exception) {
        }

        try {
            return OffsetDateTime.parse(trimmedValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
        } catch (_: Exception) {
        }

        try {
            return LocalDateTime.parse(trimmedValue, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } catch (_: Exception) {
        }

        try {
            return LocalDate.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay()
        } catch (_: Exception) {
        }

        return null
    }

    // Format the due date in a simple readable way for this screen.
    private fun formatDueDate(dueAt: LocalDateTime): String {
        return dueAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
    }
}
