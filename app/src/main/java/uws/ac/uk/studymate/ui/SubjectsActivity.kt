package uws.ac.uk.studymate.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the ViewModel used by this screen.
        subjectsVm = ViewModelProvider(this)[SubjectsViewModel::class.java]

        // Build a simple screen in code so this page matches the current app setup.
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
        }

        screenTitleText = TextView(this).apply {
            text = "Subjects"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = "Home"
        }

        val removeSectionTitle = TextView(this).apply {
            text = "Remove subject"
            textSize = 18f
            val topPadding = (20 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val removeSubjectLabel = TextView(this).apply {
            text = "Choose subject"
            val topPadding = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        removeSubjectSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        assignmentsOutputText = TextView(this).apply {
            text = "Select a subject to see its assignments"
            val topPadding = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val deleteSubjectBtn = Button(this).apply {
            text = "Delete subject"
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val addSectionTitle = TextView(this).apply {
            text = "Add subject"
            textSize = 18f
            val topPadding = (28 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val addSubjectNameLabel = TextView(this).apply {
            text = "Subject name"
            val topPadding = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        addSubjectNameInput = EditText(this).apply {
            hint = "Enter subject name"
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        val colorLabel = TextView(this).apply {
            text = "Subject color"
            val topPadding = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        colorSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        val addSubjectBtn = Button(this).apply {
            text = "Add subject"
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        headerRow.addView(screenTitleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(removeSectionTitle)
        contentLayout.addView(removeSubjectLabel)
        contentLayout.addView(removeSubjectSpinner)
        contentLayout.addView(assignmentsOutputText)
        contentLayout.addView(deleteSubjectBtn)
        contentLayout.addView(addSectionTitle)
        contentLayout.addView(addSubjectNameLabel)
        contentLayout.addView(addSubjectNameInput)
        contentLayout.addView(colorLabel)
        contentLayout.addView(colorSpinner)
        contentLayout.addView(addSubjectBtn)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )

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

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
        }

        // Update the assignments list when the selected subject changes.
        removeSubjectSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedSubjectId = selectedSubject()?.id
                showAssignmentsForSelectedSubject()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedSubjectId = null
                assignmentsOutputText.text = "Select a subject to see its assignments"
            }
        })

        // Ask for confirmation before deleting the chosen subject.
        deleteSubjectBtn.setOnClickListener {
            val subject = selectedSubject()
            if (subject == null) {
                Toast.makeText(this, "Choose a subject first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Delete subject")
                .setMessage("This will delete the subject and its assignments. Do you want to continue?")
                .setPositiveButton("Delete") { _, _ ->
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
            listOf("No subjects available")
        } else {
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
            assignmentsOutputText.text = "Select a subject to see its assignments"
            return
        }

        val assignments = selectedSubject.assignments.sortedWith(
            compareBy<Assignment> { parseDueDate(it.dueDate) == null }
                .thenBy { parseDueDate(it.dueDate) ?: LocalDateTime.MAX }
                .thenBy { it.title.lowercase() }
        )

        if (assignments.isEmpty()) {
            assignmentsOutputText.text = "No assignments for this subject yet"
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
            "No due date"
        } else {
            formatDueDate(dueAt)
        }

        val upcomingText = if (dueAt != null && !dueAt.isBefore(LocalDateTime.now())) {
            "\nUpcoming due date"
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

