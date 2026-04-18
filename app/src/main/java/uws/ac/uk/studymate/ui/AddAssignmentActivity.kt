package uws.ac.uk.studymate.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.app.TimePickerDialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.ui.viewmodels.AddAssignmentViewModel
import uws.ac.uk.studymate.util.AssignmentIconOption
import uws.ac.uk.studymate.util.AssignmentIcons
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
/*//////////////////////
Coded by Jamie Coleman
05/04/26
 *//////////////////////
class AddAssignmentActivity : AppCompatActivity() {

    private lateinit var addAssignmentVm: AddAssignmentViewModel
    private lateinit var screenTitleText: TextView
    private lateinit var titleInput: EditText
    private lateinit var subjectSpinner: Spinner
    private lateinit var dueDateValueText: TextView
    private lateinit var iconOptionsContainer: LinearLayout
    private lateinit var saveAssignmentBtn: Button

    private var subjects: List<Subject> = emptyList()
    private var selectedDueDateTime: LocalDateTime? = null
    private var selectedIconKey: String = AssignmentIcons.DEFAULT_KEY

    /**
     This screen lets the user add a new assignment without leaving the app flow.
     it started with a title, subject, and date, and later got the time picker as well.
     it now saves the full due date and time and keeps the form simple.
     the final UI can style this later, but the steps are clear and working now.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the ViewModel used by this screen.
        addAssignmentVm = ViewModelProvider(this)[AddAssignmentViewModel::class.java]

        // Inflate the XML layout and bind dynamic controls used by this screen.
        setContentView(R.layout.activity_add_assignment)
        val backBtn: Button = findViewById(R.id.backBtn)
        screenTitleText = findViewById(R.id.screenTitleText)
        val homeBtn: Button = findViewById(R.id.homeBtn)
        titleInput = findViewById(R.id.titleInput)
        subjectSpinner = findViewById(R.id.subjectSpinner)
        dueDateValueText = findViewById(R.id.dueDateValueText)
        val pickDueDateBtn: Button = findViewById(R.id.pickDueDateBtn)
        iconOptionsContainer = findViewById(R.id.iconOptionsContainer)
        saveAssignmentBtn = findViewById(R.id.saveAssignmentBtn)

        // Show the latest title and subject list when the ViewModel finishes loading it.
        addAssignmentVm.screenSummary.observe(this) { summary ->
            screenTitleText.text = summary.titleText
            subjects = summary.subjects
            showSubjects(summary.subjects)
            showIconOptions()
        }

        // Send the user back to login when there is no valid session.
        addAssignmentVm.sessionExpired.observe(this) { expired ->
            if (expired) {
                openLogin()
            }
        }

        // Show validation and save messages from the ViewModel.
        addAssignmentVm.message.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

        // Return to calendar after the assignment has been saved.
        addAssignmentVm.assignmentSaved.observe(this) { saved ->
            if (saved) {
                finish()
            }
        }

        // Return to the previous screen when the back button is pressed.
        backBtn.setOnClickListener {
            finish()
        }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
        }

        // Open the date picker so the user can choose a due date.
        pickDueDateBtn.setOnClickListener {
            openDatePicker()
        }

        // Save the assignment when the user presses the button.
        saveAssignmentBtn.setOnClickListener {
            addAssignmentVm.saveAssignment(
                title = titleInput.text.toString(),
                selectedSubject = selectedSubject(),
                dueDate = selectedDueDateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                iconKey = selectedIconKey
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the screen each time the user returns so the subject list stays current.
        addAssignmentVm.loadScreen()
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

    // Fill the dropdown with the subjects that belong to the logged-in user.
    private fun showSubjects(subjects: List<Subject>) {
        val names = if (subjects.isEmpty()) {
            listOf("No subjects available")
        } else {
            subjects.map { it.name }
        }

        subjectSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            names
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        saveAssignmentBtn.isEnabled = subjects.isNotEmpty()
    }

    // Show the small list of icon choices and highlight the one the user picked.
    private fun showIconOptions() {
        iconOptionsContainer.removeAllViews()

        AssignmentIcons.options.chunked(3).forEachIndexed { rowIndex, rowOptions ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    if (rowIndex > 0) {
                        topMargin = (8 * resources.displayMetrics.density).toInt()
                    }
                }
            }

            rowOptions.forEach { option ->
                row.addView(createIconOptionView(option))
            }

            repeat(3 - rowOptions.size) {
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                })
            }

            iconOptionsContainer.addView(row)
        }
    }

    // Build one tappable icon choice for the add assignment screen.
    private fun createIconOptionView(option: AssignmentIconOption): LinearLayout {
        val isSelected = option.key == selectedIconKey
        val outerPadding = (6 * resources.displayMetrics.density).toInt()
        val iconSize = (26 * resources.displayMetrics.density).toInt()
        val badgeSize = (54 * resources.displayMetrics.density).toInt()
        val endPadding = (6 * resources.displayMetrics.density).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginEnd = endPadding
            }
            setPadding(outerPadding, outerPadding, outerPadding, outerPadding)
            background = buildIconOptionBackground(isSelected)
            isClickable = true
            isFocusable = true
            contentDescription = option.key
            setOnClickListener {
                selectedIconKey = option.key
                showIconOptions()
            }

            addView(
                LinearLayout(this@AddAssignmentActivity).apply {
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(badgeSize, badgeSize)
                    background = buildIconBadgeBackground(isSelected)
                    addView(
                        ImageView(this@AddAssignmentActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                            setImageResource(AssignmentIcons.drawableForKey(option.key))
                            setColorFilter(if (isSelected) Color.parseColor("#4F46E5") else Color.parseColor("#5F6368"))
                        }
                    )
                }
            )
        }
    }

    // Use a slightly stronger border on the icon that is currently selected.
    private fun buildIconOptionBackground(isSelected: Boolean): GradientDrawable {
        val strokeWidth = (if (isSelected) 2 else 1) * resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 14f * resources.displayMetrics.density
            setColor(Color.parseColor(if (isSelected) "#EEF2FF" else "#FAFAFA"))
            setStroke(strokeWidth.toInt(), Color.parseColor(if (isSelected) "#6366F1" else "#D9D9E3"))
        }
    }

    // Keep each icon inside its own simple box so the picker feels clear and tidy.
    private fun buildIconBadgeBackground(isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f * resources.displayMetrics.density
            setColor(Color.parseColor(if (isSelected) "#FFFFFF" else "#F3F4F6"))
        }
    }

    // Return the subject that is currently selected in the dropdown.
    private fun selectedSubject(): Subject? {
        if (subjects.isEmpty()) {
            return null
        }

        val index = subjectSpinner.selectedItemPosition
        return subjects.getOrNull(index)
    }

    // Open a date picker first, then a time picker, and show the finished due date and time.
    private fun openDatePicker() {
        val initialDateTime = selectedDueDateTime ?: LocalDateTime.now()
        val initialDate = initialDateTime.toLocalDate()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val chosenDate = LocalDate.of(year, month + 1, dayOfMonth)
                openTimePicker(chosenDate)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    // Open a time picker after the user chooses the date.
    private fun openTimePicker(chosenDate: LocalDate) {
        val initialDateTime = selectedDueDateTime ?: LocalDateTime.now()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedDueDateTime = chosenDate.atTime(hourOfDay, minute)
                dueDateValueText.text = selectedDueDateTime?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"))
                    ?: "No due date selected"
            },
            initialDateTime.hour,
            initialDateTime.minute,
            true
        ).show()
    }
}

