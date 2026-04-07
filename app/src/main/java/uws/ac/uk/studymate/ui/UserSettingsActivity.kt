package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
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
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager

class UserSettingsActivity : AppCompatActivity() {

    private lateinit var settingsTitleText: TextView
    private lateinit var settingsDetailsText: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var repo: UserRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the helpers used by this screen.
        sessionManager = SessionManager(this)
        repo = UserRepo(StudyMateDatabase.getInstance(application))

        // Build a simple screen in code so this page does not depend on extra XML files.
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        settingsTitleText = TextView(this).apply {
            text = "User settings"
            textSize = 24f
        }

        settingsDetailsText = TextView(this).apply {
            text = "Loading user settings..."
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val logoutBtn = Button(this).apply {
            text = "Logout"
            val topPadding = (24 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        contentLayout.addView(settingsTitleText)
        contentLayout.addView(settingsDetailsText)
        contentLayout.addView(logoutBtn)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )

        // Clear the session and return to login when the user logs out.
        logoutBtn.setOnClickListener {
            sessionManager.logout()
            openLogin()
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the settings each time the user returns to this screen.
        loadSettings()
    }

    // Replace this screen with the login screen when the session ends.
    private fun openLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    // Load the user's saved settings and show them on this screen.
    private fun loadSettings() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val userId = sessionManager.getLoggedInUserId() ?: return@withContext null
                val userWithMeta = repo.getUserWithMeta(userId) ?: return@withContext null
                val settings = userWithMeta.settings
                userWithMeta.user.name to buildSettingsText(
                    notificationsEnabled = settings?.notificationsEnabled ?: true,
                    darkModeEnabled = settings?.darkModeEnabled ?: false,
                    timezone = settings?.timezone ?: "UTC"
                )
            }

            if (result == null) {
                sessionManager.logout()
                openLogin()
                return@launch
            }

            settingsTitleText.text = "Settings for ${result.first}"
            settingsDetailsText.text = result.second
        }
    }

    // Turn the saved settings into plain English for the user settings screen.
    private fun buildSettingsText(
        notificationsEnabled: Boolean,
        darkModeEnabled: Boolean,
        timezone: String
    ): String {
        val notificationsText = if (notificationsEnabled) "On" else "Off"
        val darkModeText = if (darkModeEnabled) "On" else "Off"
        return "Notifications: $notificationsText\nDark mode: $darkModeText\nTimezone: $timezone"
    }
}

