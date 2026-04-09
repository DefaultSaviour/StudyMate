package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
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

class AssignmentsActivity : AppCompatActivity() {

   private lateinit var assignmentsTitleText: TextView
    private lateinit var assignmentsItemsText: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var repo: UserRepo
    private lateinit var db: StudyMateDatabase

    /**
     This screen shows the user's assignment list in one place.
     it started from the old calendar list screen, and later got renamed because this made more sense.
     it now keeps only the upcoming assignments and shows them in a cleaner order.

     **/
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

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
        }

        assignmentsTitleText = TextView(this).apply {
            text = "Assignments"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = "Home"
        }

        val addAssignmentBtn = Button(this).apply {
            text = "Add assignment"
        }

        assignmentsItemsText = TextView(this).apply {
            text = "No assignments yet"
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        headerRow.addView(assignmentsTitleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(addAssignmentBtn)
        contentLayout.addView(assignmentsItemsText)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
        }

        // Open the add assignment screen from the assignments page.
        addAssignmentBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.AddAssignmentActivity"))
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the assignment list each time the user returns to this screen.
        loadAssignments()
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

    // Load the user's saved assignments and show them on this screen.
    private fun loadAssignments() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val userId = sessionManager.getLoggedInUserId() ?: return@withContext null
                val user = repo.getUser(userId) ?: return@withContext null
                val assignments = db.assignmentDao().getAssignments(userId)
                val subjectsById = db.subjectDao().getSubjects(userId).associateBy { it.id }
                user.name to buildAssignmentsItemsText(assignments, subjectsById.mapValues { it.value.name })
            }

            if (result == null) {
                sessionManager.logout()
                openLogin()
                return@launch
            }

            assignmentsTitleText.text = "Assignments for ${result.first}"
            assignmentsItemsText.text = result.second
        }
    }

    // Turn the saved assignments into a simple readable list for the assignments screen.
    private fun buildAssignmentsItemsText(assignments: List<Assignment>, subjectNamesById: Map<Int, String>): String {
        if (assignments.isEmpty()) {
            return "No assignments yet"
        }

        val now = LocalDateTime.now()
        val upcomingAssignments = assignments
            .mapNotNull { assignment ->
                val dueAt = parseDueDate(assignment.dueDate) ?: return@mapNotNull null
                if (dueAt.isBefore(now)) {
                    return@mapNotNull null
                }
                Triple(assignment, dueAt, subjectNamesById[assignment.subjectId] ?: "Unknown subject")
            }
            .sortedWith(
                compareBy<Triple<Assignment, LocalDateTime, String>> { it.second }
                    .thenBy { it.third.lowercase() }
                    .thenBy { it.first.title.lowercase() }
            )

        if (upcomingAssignments.isEmpty()) {
            return "No upcoming assignments yet"
        }

        return upcomingAssignments.joinToString("\n\n") { (assignment, dueAt, subjectName) ->
            "$subjectName\nAssignment: ${assignment.title}\nTime due: ${formatDueDate(dueAt)}"
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

    // Format the due date in a simple readable way for the assignments screen.
    private fun formatDueDate(dueAt: LocalDateTime): String {
        return dueAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
    }
}


