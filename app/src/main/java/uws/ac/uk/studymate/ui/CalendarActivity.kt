package uws.ac.uk.studymate.ui

import uws.ac.uk.studymate.util.ColorUtils

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.CalendarEvent
import uws.ac.uk.studymate.ui.viewmodels.EventType
import uws.ac.uk.studymate.ui.viewmodels.CalendarSummary
import uws.ac.uk.studymate.ui.viewmodels.CalendarViewModel
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.AssignmentIcons
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarActivity : AppCompatActivity() {

    private lateinit var vm: CalendarViewModel

    private lateinit var card: MaterialCardView
    private lateinit var monthPanel: LinearLayout
    private lateinit var dayPanel: LinearLayout
    private lateinit var monthLabel: TextView
    private lateinit var weekdayHeaderRow: LinearLayout
    private lateinit var calendarGrid: LinearLayout
    private lateinit var dayTitle: TextView
    private lateinit var daySubText: TextView
    private lateinit var dayList: LinearLayout
    private lateinit var dayBackBtn: MaterialButton

    private var currentMonth: YearMonth = YearMonth.now()
    private var entriesByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap()
    private var selectedDate: LocalDate = LocalDate.now()

    private enum class Panel { MONTH, DAY }
    private var currentPanel = Panel.MONTH
    private var isAnimating = false

    private lateinit var monthElems: List<Pair<View, Float>>
    private lateinit var dayElems: List<Pair<View, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)

        vm = ViewModelProvider(this)[CalendarViewModel::class.java]

        card = findViewById(R.id.calendarCard)
        monthPanel = findViewById(R.id.monthPanel)
        dayPanel = findViewById(R.id.dayPanel)
        monthLabel = findViewById(R.id.monthLabelText)
        weekdayHeaderRow = findViewById(R.id.weekdayHeaderRow)
        calendarGrid = findViewById(R.id.calendarGrid)
        dayTitle = findViewById(R.id.dayPanelTitle)
        daySubText = findViewById(R.id.dayPanelSubText)
        dayList = findViewById(R.id.dayPanelList)
        dayBackBtn = findViewById(R.id.dayBackBtn)

        monthElems = listOf(
            findViewById<View>(R.id.monthNavRow)        to -1f,
            weekdayHeaderRow                              to  1f,
            calendarGrid                                  to -1f
        )
        dayElems = listOf(
            dayTitle    to -1f,
            daySubText  to  1f,
            dayList     to -1f,
            dayBackBtn  to  1f
        )

        findViewById<MaterialButton>(R.id.homeBtn).setOnClickListener { openHome() }
        findViewById<MaterialButton>(R.id.previousMonthBtn).setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            renderMonth()
        }
        findViewById<MaterialButton>(R.id.nextMonthBtn).setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            renderMonth()
        }
        findViewById<MaterialButton>(R.id.dayAddEventBtn).setOnClickListener {
            showEventTitleDialog(selectedDate)
        }
        dayBackBtn.setOnClickListener { swapToPanel(Panel.MONTH) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentPanel) {
                    Panel.MONTH -> openHome()
                    Panel.DAY -> swapToPanel(Panel.MONTH)
                }
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(card) { _, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            monthPanel.setPadding(monthPanel.paddingLeft, monthPanel.paddingTop, monthPanel.paddingRight, navBar + base)
            dayPanel.setPadding(dayPanel.paddingLeft, dayPanel.paddingTop, dayPanel.paddingRight, navBar + base)
            insets
        }

        uws.ac.uk.studymate.util.OrbField.scatter(
            findViewById(R.id.calendarCard),
            listOf(findViewById(R.id.homeBtn))
        )

        uws.ac.uk.studymate.util.Entrance.play(card)

        buildWeekdayHeader()

        vm.calendarSummary.observe(this) { summary -> applySummary(summary) }
        vm.sessionExpired.observe(this) { if (it) openLogin() }
    }

    override fun onResume() {
        super.onResume()
        vm.loadCalendar()
    }

    private fun applySummary(summary: CalendarSummary) {
        entriesByDate = summary.entriesByDate
        renderMonth()
    }

    // ───────── Month grid ─────────

    private fun buildWeekdayHeader() {
        weekdayHeaderRow.removeAllViews()
        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        val full = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        labels.forEachIndexed { i, label ->
            weekdayHeaderRow.addView(
                TextView(this).apply {
                    text = label
                    // Spell the day out for TalkBack — a bare "T" reads ambiguously.
                    contentDescription = full[i]
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#D4BC7E"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
            )
        }
    }

    private fun renderMonth() {
        monthLabel.text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        calendarGrid.removeAllViews()

        val firstDayOfMonth = currentMonth.atDay(1)
        val leadingBlanks = firstDayOfMonth.dayOfWeek.value - 1  // 0 = Monday
        val daysInMonth = currentMonth.lengthOfMonth()
        val today = LocalDate.now()

        // Always render 6 rows for a consistent grid height.
        var dayNumber = 1
        for (rowIndex in 0 until 6) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }
            for (col in 0 until 7) {
                val cellPosition = rowIndex * 7 + col
                val showDay = cellPosition >= leadingBlanks && dayNumber <= daysInMonth
                if (showDay) {
                    val date = currentMonth.atDay(dayNumber)
                    row.addView(createDayCell(date, today, entriesByDate[date].orEmpty()))
                    dayNumber++
                } else {
                    row.addView(createBlankCell())
                }
            }
            calendarGrid.addView(row)
        }
    }

    private fun createBlankCell(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            // Padding day — nothing for TalkBack to land on.
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    private fun createDayCell(
        date: LocalDate,
        today: LocalDate,
        entries: List<CalendarEvent>
    ): View {
        val density = resources.displayMetrics.density
        val hasEntries = entries.isNotEmpty()
        val isPast = date.isBefore(today)
        val isToday = date == today

        val container = FrameLayout(this).apply {
            val margin = (3 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(margin, margin, margin, margin)
            }
            if (hasEntries) {
                background = androidx.core.content.ContextCompat.getDrawable(
                    this@CalendarActivity, R.drawable.bg_day_cell_active
                )
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { openDayDetail(date, entries) }
            if (isPast) alpha = 0.5f
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = (4 * density).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Day number, with "today" ring background
        val numberContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (26 * density).toInt(), (26 * density).toInt()
            )
            if (isToday) {
                background = androidx.core.content.ContextCompat.getDrawable(
                    this@CalendarActivity, R.drawable.bg_day_today
                )
            }
            addView(
                TextView(this@CalendarActivity).apply {
                    text = date.dayOfMonth.toString()
                    textSize = 13f
                    setTextColor(Color.parseColor("#FAF8F5"))
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            )
        }
        column.addView(numberContainer)

        if (hasEntries) {
            val dotsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (3 * density).toInt() }
            }
            entries.take(3).forEach { entry ->
                val isDash = entry.type == EventType.DECK_REVIEW
                val width = if (isDash) 10 else 6
                val height = if (isDash) 3 else 6
                val dot = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (width * density).toInt(), (height * density).toInt()
                    ).apply {
                        marginStart = (2 * density).toInt()
                        marginEnd = (2 * density).toInt()
                    }
                    background = GradientDrawable().apply {
                        shape = if (isDash) GradientDrawable.RECTANGLE else GradientDrawable.OVAL
                        setColor(parseColor(entry.colorHex))
                        if (isDash) cornerRadius = 1.5f * density
                    }
                }
                dotsRow.addView(dot)
            }
            column.addView(dotsRow)
        }

        // Accessibility: let the whole cell read as one node ("Today, 15 June, 2
        // assignments due") instead of TalkBack landing on the bare day number and
        // the colour dots separately.
        column.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        val dateLabel = date.format(DateTimeFormatter.ofPattern("d MMMM"))
        val spokenDate = if (isToday) getString(R.string.cd_today_prefix, dateLabel) else dateLabel
        container.contentDescription = if (hasEntries) {
            resources.getQuantityString(R.plurals.cd_day_assignments, entries.size, spokenDate, entries.size)
        } else {
            getString(R.string.cd_day_no_assignments, spokenDate)
        }

        container.addView(column)
        return container
    }

    private fun parseColor(hex: String?): Int = ColorUtils.parseOrDefault(hex)

    // ───────── Day detail ─────────

    private fun openDayDetail(date: LocalDate, entries: List<CalendarEvent>) {
        selectedDate = date
        dayTitle.text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
        daySubText.text = when (entries.size) {
            1 -> "1 due today"
            else -> "${entries.size} due today"
        }
        renderDayList(entries)
        swapToPanel(Panel.DAY)
    }

    private fun renderDayList(entries: List<CalendarEvent>) {
        dayList.removeAllViews()
        val density = resources.displayMetrics.density
        val sorted = entries // already sorted by VM
        // Max 9 rows so nothing scrolls; if more, last shows "+N more".
        val maxRows = 9
        val visible = if (sorted.size > maxRows) sorted.take(maxRows - 1) else sorted
        visible.forEach { entry -> dayList.addView(buildEventRow(entry, density)) }
        if (sorted.size > maxRows) {
            dayList.addView(
                TextView(this).apply {
                    text = "+${sorted.size - (maxRows - 1)} more — open Assignments for the full list"
                    textSize = 12f
                    setTextColor(Color.parseColor("#B2FAF8F5"))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (10 * density).toInt() }
                }
            )
        }
    }

    private fun buildEventRow(entry: CalendarEvent, density: Float): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            background = androidx.core.content.ContextCompat.getDrawable(
                this@CalendarActivity, R.drawable.bg_subject_row
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (6 * density).toInt() }
            // Tapping the assignment opens its flashcard decks.
            if (entry.type == EventType.ASSIGNMENT) {
                isClickable = true
                isFocusable = true
                setOnClickListener { openDecksFor(entry) }
            } else if (entry.type == EventType.DECK_REVIEW) {
                isClickable = true
                isFocusable = true
                setOnClickListener { openReviewFor(entry) }
            } else {
                isClickable = false
            }
        }

        val color = parseColor(entry.colorHex)

        // Just the icon in the assignment's colour — no badge background or outline.
        val badge = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (38 * density).toInt(), (38 * density).toInt()
            )
            addView(
                ImageView(this@CalendarActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        (26 * density).toInt(), (26 * density).toInt(), Gravity.CENTER
                    )
                    setImageResource(AssignmentIcons.drawableForKey(entry.iconKey))
                    setColorFilter(color)
                }
            )
        }

        val text = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = (10 * density).toInt() }
            addView(TextView(this@CalendarActivity).apply {
                text = entry.title
                textSize = 14f
                setTextColor(Color.parseColor("#FAF8F5"))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            addView(TextView(this@CalendarActivity).apply {
                text = entry.timeText ?: "All day"
                textSize = 11f
                setTextColor(Color.parseColor("#B2FAF8F5"))
                maxLines = 1
            })
        }

        // Accessibility: read the row as one node and announce that it's actionable,
        // rather than TalkBack stopping on the colour icon and each text line.
        badge.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        text.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        row.contentDescription = getString(
            R.string.cd_calendar_assignment_row,
            entry.title,
            entry.timeText ?: "All day"
        )

        row.addView(badge)
        row.addView(text)
        return row
    }

    // ───────── Panel swap ─────────

    private fun swapToPanel(target: Panel) {
        if (isAnimating || target == currentPanel) return
        val outgoingPanel = if (currentPanel == Panel.MONTH) monthPanel else dayPanel
        val incomingPanel = if (target == Panel.MONTH) monthPanel else dayPanel
        val outgoingElems = if (currentPanel == Panel.MONTH) monthElems else dayElems
        val incomingElems = if (target == Panel.MONTH) monthElems else dayElems

        val goingForward = currentPanel == Panel.MONTH && target == Panel.DAY
        val sign = if (goingForward) 1f else -1f

        val w = outgoingPanel.width.toFloat()
        val stagger = 72L
        val exitDur = 420L
        val enterDur = 440L
        val enterStart = (outgoingElems.size - 1) * stagger + exitDur
        val exitEase = AccelerateInterpolator(1.3f)
        val enterEase = DecelerateInterpolator(1.3f)

        isAnimating = true
        incomingElems.forEach { (v, dir) -> v.translationX = w * dir * sign }
        incomingPanel.visibility = View.VISIBLE

        outgoingElems.forEachIndexed { i, (v, dir) ->
            v.animate()
                .translationX(w * dir * sign)
                .setDuration(exitDur)
                .setStartDelay(i * stagger)
                .setInterpolator(exitEase)
                .start()
        }
        incomingElems.forEachIndexed { i, (v, _) ->
            v.animate()
                .translationX(0f)
                .setDuration(enterDur)
                .setStartDelay(enterStart + i * stagger)
                .setInterpolator(enterEase)
                .start()
        }

        val hideDelay = (outgoingElems.size - 1) * stagger + exitDur + 50L
        outgoingPanel.postDelayed({
            outgoingPanel.visibility = View.INVISIBLE
            outgoingElems.forEach { (v, _) -> v.translationX = 0f }
            isAnimating = false
        }, hideDelay)

        currentPanel = target
    }

    // ───────── Orbs / helpers ─────────


    // Open the Flashcards decks screen scoped to the tapped assignment.
    private fun openDecksFor(entry: CalendarEvent) {
        startActivity(
            Intent().setClassName(packageName, "$packageName.ui.FlashcardDecksActivity")
                .putExtra("scoped_assignment_id", entry.id)
                .putExtra("scoped_assignment_name", entry.title)
        )
    }

    private fun openReviewFor(entry: CalendarEvent) {
        startActivity(
            Intent().setClassName(packageName, "$packageName.ui.ReviewDeckActivity")
                .putExtra("deck_id", entry.id)
                .putExtra("deck_name", entry.title.removePrefix("Review: "))
        )
    }

    // (removed unused showAddEventDialog since we use showEventTitleDialog directly)

    private fun showEventTitleDialog(date: LocalDate) {
        val input = android.widget.EditText(this).apply {
            hint = "Event title"
            setTextColor(Color.parseColor("#FAF8F5"))
            setHintTextColor(Color.parseColor("#B2FAF8F5"))
        }
        val layout = LinearLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("New event for ${date.format(DateTimeFormatter.ofPattern("d MMM"))}")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    vm.addCustomEvent(title, date, "#C4A24A", "event")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openLogin() {
        val i = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(i)
    }

    private fun openHome() {
        // Return to the existing Dashboard instead of launching a new one, so the
        // back stack stays a clean Dashboard -> screen -> sub-screen hierarchy.
        finish()
    }
}
