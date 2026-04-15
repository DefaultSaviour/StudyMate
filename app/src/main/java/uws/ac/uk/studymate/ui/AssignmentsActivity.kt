package uws.ac.uk.studymate.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.*
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.AssignmentIcons
import uws.ac.uk.studymate.util.SessionManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class AssignmentsActivity : AppCompatActivity() {

    private lateinit var assignmentsTitleText: TextView
    private lateinit var emptyAssignmentsText: TextView
    private lateinit var assignmentsListContainer: LinearLayout
    private lateinit var addAssignmentBtn: Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        db = StudyMateDatabase.getInstance(application)
        repo = UserRepo(db)

        setContentView(R.layout.activity_assignments)

       //XML
        val backBtn: ImageButton = findViewById(R.id.backBtn)
        val homeBtn: ImageButton = findViewById(R.id.homeBtn)
        assignmentsTitleText = findViewById(R.id.assignmentsTitleText)
        addAssignmentBtn = findViewById(R.id.addAssignmentBtn)
        assignmentsListContainer = findViewById(R.id.assignmentsListContainer)
        emptyAssignmentsText = findViewById(R.id.assignmentsItemsText)

        homeBtn.setOnClickListener {
            openHome()
        }

        backBtn.setOnClickListener {
            finish()
        }

        // Open Add Assignment screen
        addAssignmentBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.AddAssignmentActivity"))
        }
    }

    override fun onResume() {
        super.onResume()
        loadAssignments()
    }

    private fun openLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    private fun openHome() {
        startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
    }

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

    private fun buildUpcomingAssignments(
        assignments: List<Assignment>,
        subjectsById: Map<Int, Subject>
    ): List<UpcomingAssignmentItem> {
        val now = LocalDateTime.now()
        return assignments
            .mapNotNull { assignment ->
                val dueAt = parseDueDate(assignment.dueDate) ?: return@mapNotNull null
                if (dueAt.isBefore(now)) return@mapNotNull null
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

    private fun createAssignmentCard(item: UpcomingAssignmentItem): LinearLayout {
        val view = layoutInflater.inflate(R.layout.item_assignment_card, null)

        val icon = view.findViewById<ImageView>(R.id.assignmentIcon)
        val subject = view.findViewById<TextView>(R.id.subjectName)
        val title = view.findViewById<TextView>(R.id.assignmentTitle)
        val due = view.findViewById<TextView>(R.id.dueDate)

        val subjectColor = parseSubjectColor(item.subjectColorHex)

        subject.text = item.subjectName
        title.text = "Assignment: ${item.title}"
        due.text = "Time due: ${formatDueDate(item.dueAt)}"

        // Set colored badge background
        icon.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 14f
            setColor(subjectColor)
        }
        icon.setImageResource(AssignmentIcons.drawableForKey(item.iconKey))
        icon.setColorFilter(Color.WHITE)

        return view as LinearLayout
    }

    private fun parseDueDate(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        val trimmedValue = value.trim()
        return try {
            LocalDateTime.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(trimmedValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
            } catch (_: Exception) {
                try {
                    LocalDateTime.parse(trimmedValue, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                } catch (_: Exception) {
                    try {
                        LocalDate.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay()
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }

    private fun formatDueDate(dueAt: LocalDateTime): String {
        return dueAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
    }

    private fun parseSubjectColor(colorHex: String?): Int {
        return try {
            if (colorHex.isNullOrBlank())
                ContextCompat.getColor(this, R.color.studymate_border)
            else
                Color.parseColor(colorHex)
        } catch (_: Exception) {
            ContextCompat.getColor(this, R.color.studymate_border)
        }
    }
    }