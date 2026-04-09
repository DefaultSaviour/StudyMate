package uws.ac.uk.studymate.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.ui.viewmodels.CalendarAssignmentEntry
import uws.ac.uk.studymate.ui.viewmodels.CalendarSummary
import uws.ac.uk.studymate.ui.viewmodels.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarVm: CalendarViewModel
    private lateinit var calendarTitleText: TextView
    private lateinit var monthLabelText: TextView
    private lateinit var calendarRowsContainer: LinearLayout

    private var currentMonth: YearMonth = YearMonth.now()
    private var entriesByDate: Map<LocalDate, List<CalendarAssignmentEntry>> = emptyMap()

    /**
     This screen gives the user a simple month view of their assignments.
     it started as a basic day grid, and later got colored markers, outlines, and the popup for each day.
     it now keeps the calendar cleaner by showing stars in the cell and the full list after a tap.
     the calendar flow is much clearer now and actually working
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the ViewModel used by this screen.
        calendarVm = ViewModelProvider(this)[CalendarViewModel::class.java]

        // Build a simple screen in code so each day can be colored by subject.
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

        calendarTitleText = TextView(this).apply {
            text = "Calendar"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = "Home"
        }

        val monthNavRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
        }

        val previousMonthBtn = Button(this).apply {
            text = "<"
        }

        monthLabelText = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val nextMonthBtn = Button(this).apply {
            text = ">"
        }

        val weekdayHeaderRow = createWeekdayHeaderRow()

        calendarRowsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        headerRow.addView(calendarTitleText)
        headerRow.addView(homeBtn)

        monthNavRow.addView(previousMonthBtn)
        monthNavRow.addView(monthLabelText)
        monthNavRow.addView(nextMonthBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(monthNavRow)
        contentLayout.addView(weekdayHeaderRow)
        contentLayout.addView(calendarRowsContainer)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )

        // Show the latest calendar data when the ViewModel finishes loading it.
        calendarVm.calendarSummary.observe(this) { summary ->
            showCalendar(summary)
        }

        // Send the user back to login when there is no valid session.
        calendarVm.sessionExpired.observe(this) { expired ->
            if (expired) {
                openLogin()
            }
        }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
        }

        // Move to the previous month and redraw the calendar.
        previousMonthBtn.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            renderMonth()
        }

        // Move to the next month and redraw the calendar.
        nextMonthBtn.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            renderMonth()
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the calendar each time the user returns to this screen.
        calendarVm.loadCalendar()
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

    // Store the latest calendar data and redraw the visible month.
    private fun showCalendar(summary: CalendarSummary) {
        calendarTitleText.text = summary.titleText
        entriesByDate = summary.entriesByDate
        renderMonth()
    }

    // Draw the currently selected month as a simple 7-column grid.
    private fun renderMonth() {
        monthLabelText.text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        calendarRowsContainer.removeAllViews()

        val firstDayOfMonth = currentMonth.atDay(1)
        val leadingBlankCells = firstDayOfMonth.dayOfWeek.value - 1
        val daysInMonth = currentMonth.lengthOfMonth()
        var dayNumber = 1

        while (dayNumber <= daysInMonth) {
            val weekRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }

            for (columnIndex in 0 until 7) {
                val cellIndex = (dayNumber - 1) + leadingBlankCells
                if (cellIndex / 7 != (dayNumber - 1 + leadingBlankCells) / 7 && columnIndex == 0) {
                    break
                }

                val shouldShowBlank = ((dayNumber == 1) && columnIndex < leadingBlankCells) || dayNumber > daysInMonth
                if (shouldShowBlank) {
                    weekRow.addView(createEmptyDayCell())
                } else {
                    val date = currentMonth.atDay(dayNumber)
                    weekRow.addView(createDayCell(date, entriesByDate[date].orEmpty()))
                    dayNumber += 1
                }
            }

            while (weekRow.childCount < 7) {
                weekRow.addView(createEmptyDayCell())
            }

            calendarRowsContainer.addView(weekRow)
        }
    }

    // Build the weekday header shown above the calendar grid.
    private fun createWeekdayHeaderRow(): LinearLayout {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
            }

            labels.forEach { label ->
                addView(
                    TextView(this@CalendarActivity).apply {
                        text = label
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                    }
                )
            }
        }
    }

    // Build one empty day cell so the month grid lines up correctly.
    private fun createEmptyDayCell(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
                marginStart = (4 * resources.displayMetrics.density).toInt()
                marginEnd = (4 * resources.displayMetrics.density).toInt()
            }
            minimumHeight = (110 * resources.displayMetrics.density).toInt()
        }
    }

    // Build one calendar day cell and color its date with the subject color when assignments exist.
    private fun createDayCell(date: LocalDate, entries: List<CalendarAssignmentEntry>): LinearLayout {
        val isPastDay = date.isBefore(LocalDate.now())
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (8 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
                marginStart = (4 * resources.displayMetrics.density).toInt()
                marginEnd = (4 * resources.displayMetrics.density).toInt()
            }
            minimumHeight = (110 * resources.displayMetrics.density).toInt()
            background = buildDayCellBackground(hasAssignments = entries.isNotEmpty(), isPastDay = isPastDay)
        }

        val dayNumberText = TextView(this).apply {
            text = date.dayOfMonth.toString()
            textSize = 18f
            setTextColor(if (isPastDay) pastDayGray() else Color.BLACK)
        }

        cell.addView(dayNumberText)

        if (entries.isNotEmpty()) {
            cell.addView(createAssignmentMarkers(entries, isPastDay))

            // Open a popup with the day's assignments when the user taps a day that has entries.
            cell.setOnClickListener {
                showAssignmentsDialog(date, entries)
            }
        }

        return cell
    }


     /**
     Show one simple marker per assignment so the calendar stays neat inside each day cell.
     updates to show a star "★" instead of a colored block, and up to 5 stars per day with a "+X" when there are more than 5 assignments.
     updated again to show a colored star using the subject color.
     updated again to only have 2 stars per row to stop them getting cut off.
     the UI people should finalize this anyway they see fit
      **/
    private fun createAssignmentMarkers(entries: List<CalendarAssignmentEntry>, isPastDay: Boolean): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val topPadding = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)

            val visibleEntries = entries.sortedBy { it.dueAt }.take(5)
            visibleEntries.chunked(2).forEachIndexed { rowIndex, rowEntries ->
                addView(
                    LinearLayout(this@CalendarActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        if (rowIndex > 0) {
                            val rowTopPadding = (2 * resources.displayMetrics.density).toInt()
                            setPadding(0, rowTopPadding, 0, 0)
                        }

                        rowEntries.forEach { entry ->
                            addView(
                                TextView(this@CalendarActivity).apply {
                                    text = "★"
                                    textSize = 12f
                                    val endPadding = (2 * resources.displayMetrics.density).toInt()
                                    setPadding(0, 0, endPadding, 0)
                                    setTextColor(if (isPastDay) pastDayGray() else parseSubjectColor(entry.subjectColorHex))
                                }
                            )
                        }
                    }
                )
            }
            // this is working but might need adjusting depending on what the UI people do
            if (entries.size > 5) {
                addView(
                    TextView(this@CalendarActivity).apply {
                        text = "+${entries.size - 5}"
                        textSize = 11f
                        val extraTopPadding = (2 * resources.displayMetrics.density).toInt()
                        setPadding(0, extraTopPadding, 0, 0)
                        setTextColor(if (isPastDay) pastDayGray() else Color.BLACK) // gray if past the date in the calendar
                    }
                )
            }
        }
    }


    /**
     Show the full list for one day after the user taps a calendar cell.
     this started as text inside the cell, and later moved to a popup because the cells got too crowded.
     it now gives each assignment its own colored box and keeps the month view cleaner.
     the final UI can make this popup look better later, but the idea is much easier to read now.
     **/
    private fun showAssignmentsDialog(date: LocalDate, entries: List<CalendarAssignmentEntry>) {
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        entries.sortedBy { it.dueAt }.forEach { entry ->
            contentLayout.addView(createDialogAssignmentBlock(entry))
        }

        AlertDialog.Builder(this)
            .setTitle(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
            .setView(
                ScrollView(this).apply {
                    addView(contentLayout)
                }
            )
            .setPositiveButton("Close", null)
            .show()
    }

    // Build one colored assignment box for the popup shown after tapping a calendar day.
    private fun createDialogAssignmentBlock(entry: CalendarAssignmentEntry): LinearLayout {
        val subjectColor = parseSubjectColor(entry.subjectColorHex)
        val padding = (12 * resources.displayMetrics.density).toInt()
        val topMargin = (8 * resources.displayMetrics.density).toInt()

        val block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(withSubjectTint(subjectColor))
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                this.topMargin = topMargin
            }
        }

        block.addView(
            TextView(this).apply {
                text = entry.subjectName
                textSize = 14f
                setTextColor(Color.BLACK)
            }
        )

        block.addView(
            TextView(this).apply {
                text = entry.assignmentTitle
                textSize = 16f
                setTextColor(Color.BLACK)
            }
        )

        block.addView(
            TextView(this).apply {
                text = entry.dueAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                textSize = 14f
                setTextColor(Color.BLACK)
            }
        )

        return block
    }

    // Read the saved subject color, or fall back to a safe dark text color when it is missing.
    private fun parseSubjectColor(colorHex: String?): Int {
        return try {
            if (colorHex.isNullOrBlank()) Color.parseColor("#222222") else Color.parseColor(colorHex)
        } catch (_: Exception) {
            Color.parseColor("#222222")
        }
    }

    // Use one gray shade for past days so they are easier to spot in the calendar.
    private fun pastDayGray(): Int {
        return Color.parseColor("#9E9E9E")
    }

    // Apply a 40 percent alpha so the subject color becomes a soft shaded background.
    private fun withSubjectTint(color: Int): Int {
        val alpha = (255 * 0.4f).toInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    // Build the basic background for one day cell and add an outline when that day has assignments.
    private fun buildDayCellBackground(hasAssignments: Boolean, isPastDay: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f * resources.displayMetrics.density
            setColor(Color.parseColor("#F7F7F7"))
            if (hasAssignments) {
                val strokeWidth = (2 * resources.displayMetrics.density).toInt()
                setStroke(strokeWidth, if (isPastDay) pastDayGray() else Color.BLACK)
            }
        }
    }
}


// if you are reading this, im as amazed as you that this worked.
// the tutorial was 8 years old