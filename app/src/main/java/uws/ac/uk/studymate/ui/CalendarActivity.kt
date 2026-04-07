package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarTitleText: TextView
    private lateinit var calendarItemsText: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var repo: UserRepo
    private lateinit var db: StudyMateDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the helpers used by this screen.
        sessionManager = SessionManager(this)
        db = StudyMateDatabase.getInstance(application)
        repo = UserRepo(db)

        // Build a simple screen in code so this page does not depend on extra XML files.
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        calendarTitleText = TextView(this).apply {
            text = "Calendar"
            textSize = 24f
        }

        calendarItemsText = TextView(this).apply {
            text = "No calendar items yet"
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        contentLayout.addView(calendarTitleText)
        contentLayout.addView(calendarItemsText)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )
    }

    override fun onResume() {
        super.onResume()

        // Reload the calendar details each time the user returns to this screen.
        loadCalendar()
    }

    // Replace this screen with the login screen when the session ends.
    private fun openLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    // Load the user's saved assignments and show them on this screen.
    private fun loadCalendar() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val userId = sessionManager.getLoggedInUserId() ?: return@withContext null
                val user = repo.getUser(userId) ?: return@withContext null
                val assignments = db.assignmentDao().getAssignments(userId)
                user.name to buildCalendarItemsText(assignments)
            }

            if (result == null) {
                sessionManager.logout()
                openLogin()
                return@launch
            }

            calendarTitleText.text = "Calendar for ${result.first}"
            calendarItemsText.text = result.second
        }
    }

    // Turn the saved assignments into a simple readable list for the calendar screen.
    private fun buildCalendarItemsText(assignments: List<Assignment>): String {
        if (assignments.isEmpty()) {
            return "No calendar items yet"
        }

        val datedAssignments = assignments
            .map { assignment -> assignment to parseDueDate(assignment.dueDate) }
            .sortedWith(
                compareBy<Pair<Assignment, LocalDateTime?>> { it.second == null }
                    .thenBy { it.second ?: LocalDateTime.MAX }
                    .thenBy { it.first.title.lowercase() }
            )

        return datedAssignments.joinToString("\n\n") { (assignment, dueAt) ->
            val dueText = if (dueAt == null) {
                assignment.dueDate?.takeIf { it.isNotBlank() } ?: "No due date saved"
            } else {
                formatDueDate(dueAt)
            }
            "${assignment.title}\nDue: $dueText"
        }
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

    // Format the due date in a simple readable way for the calendar screen.
    private fun formatDueDate(dueAt: LocalDateTime): String {
        return dueAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
    }
}

