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
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager
/*//////////////////////
Coded by Jamie Coleman
06/04/26
fixed 09/04/26
 *//////////////////////
class UserSettingsActivity : AppCompatActivity() {

    private lateinit var settingsTitleText: TextView
    private lateinit var settingsDetailsText: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var repo: UserRepo

    /**
     This screen gives the user one place to check their saved settings and log out.
     it started as a very plain page, and later got the home button and the live settings text.
     it now keeps the settings simple and leaves the risky logout action easy to find.
     UI team might need to fix this but i think it right?
     **/
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

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
        }

        settingsTitleText = TextView(this).apply {
            text = "User settings"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = "Home"
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

        headerRow.addView(settingsTitleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
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

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
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

    // Return to the main home screen from the top-right button.
    private fun openHome() {
        startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
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

