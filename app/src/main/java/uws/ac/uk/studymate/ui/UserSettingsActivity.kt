package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager
import uws.ac.uk.studymate.ui.viewmodels.UserSettingsViewModel

class UserSettingsActivity : AppCompatActivity() {

    private lateinit var settingsTitleText: TextView
    private lateinit var studentNameText: TextView
    private lateinit var studentEmailText: TextView
    private lateinit var settingsDetailsText: TextView
    private lateinit var assignmentsCountText: TextView
    private lateinit var flashcardsCountText: TextView

    private lateinit var notificationsCard: LinearLayout
    private lateinit var notificationsDropdown: LinearLayout
    private lateinit var notificationsArrow: ImageView
    private lateinit var pushNotificationSwitch: SwitchCompat
    private lateinit var darkModeSwitch: SwitchCompat

    private lateinit var privacyCard: LinearLayout
    private lateinit var privacyDropdown: LinearLayout
    private lateinit var privacyArrow: ImageView
    private lateinit var saveLoginCheck: CheckBox

    private lateinit var logoutCard: LinearLayout
    private lateinit var backBtn: ImageButton
    private lateinit var homeBtn: ImageButton

    private lateinit var sessionManager: SessionManager
    private lateinit var repo: UserRepo
    private lateinit var viewModel: UserSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_settings)

        // Helpers
        sessionManager = SessionManager(this)
        repo = UserRepo(StudyMateDatabase.getInstance(application))
        viewModel = UserSettingsViewModel(application)

        // Bind views
        backBtn = findViewById(R.id.backBtn)
        homeBtn = findViewById(R.id.homeBtn)
        settingsTitleText = findViewById(R.id.profileTitle)
        studentNameText = findViewById(R.id.studentName)
        studentEmailText = findViewById(R.id.studentEmail)
        settingsDetailsText = findViewById(R.id.settingsDetailsText)

        assignmentsCountText = findViewById(R.id.assignmentsCount)
        flashcardsCountText = findViewById(R.id.flashcardsCount)

        notificationsCard = findViewById(R.id.notificationsCard)
        notificationsDropdown = findViewById(R.id.notificationsDropdown)
        notificationsArrow = findViewById(R.id.notificationsArrow)
        pushNotificationSwitch = findViewById(R.id.pushNotificationSwitch)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)

        privacyCard = findViewById(R.id.privacyCard)
        privacyDropdown = findViewById(R.id.privacyDropdown)
        privacyArrow = findViewById(R.id.privacyArrow)
        saveLoginCheck = findViewById(R.id.saveLoginCheck)

        logoutCard = findViewById(R.id.logoutCard)

        // TEMP dynamic values for UI only
        // TODO replace with real Assignment and Flashcard database values
        val tempAssignmentCount = (5..15).random()
        val tempFlashcardCount = (20..60).random()

        assignmentsCountText.text = tempAssignmentCount.toString()
        flashcardsCountText.text = tempFlashcardCount.toString()

        // Toggle dropdowns
        notificationsCard.setOnClickListener { toggleDropdown(notificationsDropdown, notificationsArrow) }
        privacyCard.setOnClickListener { toggleDropdown(privacyDropdown, privacyArrow) }

        pushNotificationSwitch.setOnCheckedChangeListener { _, _ ->
            updateSettingsStatus()
        }

        darkModeSwitch.setOnCheckedChangeListener { _, _ ->
            updateSettingsStatus()
        }

        // Logout
        logoutCard.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.logout()
                    openLogin()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        backBtn.setOnClickListener { finish() }
        homeBtn.setOnClickListener { openHome() }
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    private fun toggleDropdown(dropdown: LinearLayout, arrow: ImageView) {
        if (dropdown.isGone) {
            dropdown.visibility = LinearLayout.VISIBLE
            arrow.rotation = 90f
        } else {
            dropdown.isGone = true
            arrow.rotation = 270f
        }
    }

    private fun updateSettingsStatus() {
        val notificationsText = if (pushNotificationSwitch.isChecked) "On" else "Off"
        val darkModeText = if (darkModeSwitch.isChecked) "On" else "Off"

        // Pull timezone from settingsDetailsText if available
        val timezoneText = settingsDetailsText.text.toString()
            .split("\n")
            .getOrNull(2)
            ?.substringAfter("Timezone: ") ?: "UTC"

        settingsDetailsText.text = "Notifications: $notificationsText\nDark mode: $darkModeText\nTimezone: $timezoneText"
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

            // Title
            settingsTitleText.text = "Settings for ${result.first}"

            // Apply values to switches
            val lines = result.second.split("\n")

            pushNotificationSwitch.isChecked = lines[0].contains("On")
            darkModeSwitch.isChecked = lines[1].contains("On")

            // Update the text under email
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