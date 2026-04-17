package uws.ac.uk.studymate.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
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
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.AssignmentIcons
import uws.ac.uk.studymate.util.SessionManager
import uws.ac.uk.studymate.LoginActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
/*//////////////////////
Coded by Jamie Coleman
05/04/26
fixed 08/04/26
 *//////////////////////
class AssignmentsActivity : AppCompatActivity() {

   private lateinit var assignmentsTitleText: TextView
    private lateinit var emptyAssignmentsText: TextView
    private lateinit var assignmentsListContainer: LinearLayout
    private lateinit var sessionManager: SessionManager
    private lateinit var repo: UserRepo
    private lateinit var db: StudyMateDatabase

    private data class UpcomingAssignmentItem(
        val title: String,
        val dueAt: LocalDateTime,
        val subjectName: String,
        val subjectColorHex: String?,
        val iconKey: String
    )

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

        emptyAssignmentsText = TextView(this).apply {
            text = "No assignments yet"
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        assignmentsListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        headerRow.addView(assignmentsTitleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(addAssignmentBtn)
        contentLayout.addView(emptyAssignmentsText)
        contentLayout.addView(assignmentsListContainer)

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
                user.name to buildUpcomingAssignments(assignments, subjectsById)
            }

            if (result == null) {
                sessionManager.logout()
                openLogin()
                return@launch
            }

            assignmentsTitleText.text = "Assignments for ${result.first}"
            showAssignments(result.second)
        }
    }

    // Keep only upcoming assignments and sort them so the nearest due work appears first.
    private fun buildUpcomingAssignments(
        assignments: List<Assignment>,
        subjectsById: Map<Int, Subject>
    ): List<UpcomingAssignmentItem> {
        val now = LocalDateTime.now()
        return assignments
            .mapNotNull { assignment ->
                val dueAt = parseDueDate(assignment.dueDate) ?: return@mapNotNull null
                if (dueAt.isBefore(now)) {
                    return@mapNotNull null
                }

                val subject = subjectsById[assignment.subjectId]
                UpcomingAssignmentItem(
                    title = assignment.title,
                    dueAt = dueAt,
                    subjectName = subject?.name ?: "Unknown subject",
                    subjectColorHex = subject?.color,
                    iconKey = assignment.icon
                )
            }
            .sortedWith(
                compareBy<UpcomingAssignmentItem> { it.dueAt }
                    .thenBy { it.subjectName.lowercase() }
                    .thenBy { it.title.lowercase() }
            )
    }

    // Show the current upcoming assignments as simple cards with the chosen icon.
    private fun showAssignments(assignments: List<UpcomingAssignmentItem>) {
        assignmentsListContainer.removeAllViews()

        if (assignments.isEmpty()) {
            emptyAssignmentsText.text = "No upcoming assignments yet"
            emptyAssignmentsText.visibility = TextView.VISIBLE
            return
        }

        emptyAssignmentsText.visibility = TextView.GONE
        assignments.forEach { item ->
            assignmentsListContainer.addView(createAssignmentCard(item))
        }
    }

    // Build one assignment card with the saved icon inside a box that uses the subject color.
    private fun createAssignmentCard(item: UpcomingAssignmentItem): LinearLayout {
        val subjectColor = parseSubjectColor(item.subjectColorHex)
        val padding = (14 * resources.displayMetrics.density).toInt()
        val badgeSize = (48 * resources.displayMetrics.density).toInt()
        val iconSize = (22 * resources.displayMetrics.density).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
            background = buildAssignmentCardBackground()

            addView(
                LinearLayout(this@AssignmentsActivity).apply {
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(badgeSize, badgeSize).apply {
                        marginEnd = (12 * resources.displayMetrics.density).toInt()
                    }
                    background = buildIconBadgeBackground(subjectColor)
                    addView(
                        ImageView(this@AssignmentsActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                            setImageResource(AssignmentIcons.drawableForKey(item.iconKey))
                            setColorFilter(Color.WHITE)
                        }
                    )
                }
            )

            addView(
                LinearLayout(this@AssignmentsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)

                    addView(
                        TextView(this@AssignmentsActivity).apply {
                            text = item.subjectName
                            textSize = 14f
                            setTextColor(Color.BLACK)
                        }
                    )

                    addView(
                        TextView(this@AssignmentsActivity).apply {
                            text = "Assignment: ${item.title}"
                            textSize = 16f
                            setTextColor(Color.BLACK)
                        }
                    )

                    addView(
                        TextView(this@AssignmentsActivity).apply {
                            text = "Time due: ${formatDueDate(item.dueAt)}"
                            textSize = 14f
                            setTextColor(Color.BLACK)
                        }
                    )
                }
            )
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

    // Read the saved subject color, or fall back to a safe dark color when it is missing.
    private fun parseSubjectColor(colorHex: String?): Int {
        return try {
            if (colorHex.isNullOrBlank()) Color.parseColor("#444444") else Color.parseColor(colorHex)
        } catch (_: Exception) {
            Color.parseColor("#444444")
        }
    }

    // Keep each assignment card clean and light so the icon badge stands out.
    private fun buildAssignmentCardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f * resources.displayMetrics.density
            setColor(Color.parseColor("#F7F7F7"))
            setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#D8D8D8"))
        }
    }

    // Use the subject color behind the icon so the assignment can be recognised quickly.
    private fun buildIconBadgeBackground(subjectColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 14f * resources.displayMetrics.density
            setColor(subjectColor)
        }
    }
}


