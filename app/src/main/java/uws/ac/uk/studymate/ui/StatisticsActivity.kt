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
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager

class StatisticsActivity : AppCompatActivity() {

    private lateinit var statisticsTitleText: TextView
    private lateinit var statisticsDetailsText: TextView
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

        statisticsTitleText = TextView(this).apply {
            text = "Statistics"
            textSize = 24f
        }

        statisticsDetailsText = TextView(this).apply {
            text = "Assignments: 0\nFlashcards: 0\nStreak: 0 day(s)"
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        contentLayout.addView(statisticsTitleText)
        contentLayout.addView(statisticsDetailsText)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )
    }

    override fun onResume() {
        super.onResume()

        // Reload the statistics each time the user returns to this screen.
        loadStatistics()
    }

    // Replace this screen with the login screen when the session ends.
    private fun openLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    // Load the user's saved statistics and show them on this screen.
    private fun loadStatistics() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val userId = sessionManager.getLoggedInUserId() ?: return@withContext null
                val userWithMeta = repo.getUserWithMeta(userId) ?: return@withContext null
                val assignmentsCount = db.assignmentDao().getAssignments(userId).size
                val flashcardCount = userWithMeta.stats?.flashcardsCount ?: 0
                val streakDays = userWithMeta.stats?.streakDays ?: 0
                userWithMeta.user.name to buildStatisticsText(assignmentsCount, flashcardCount, streakDays)
            }

            if (result == null) {
                sessionManager.logout()
                openLogin()
                return@launch
            }

            statisticsTitleText.text = "Statistics for ${result.first}"
            statisticsDetailsText.text = result.second
        }
    }

    // Turn the saved numbers into plain English for the statistics screen.
    private fun buildStatisticsText(
        assignmentCount: Int,
        flashcardCount: Int,
        streakDays: Int
    ): String {
        return "Assignments: $assignmentCount\nFlashcards: $flashcardCount\nStreak: $streakDays day(s)"
    }
}

