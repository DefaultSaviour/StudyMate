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
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.UserSettingsViewModel
/*//////////////////////
Coded by Jamie Coleman
06/04/26
fixed 09/04/26
 updated 16/04/26
 *//////////////////////
class UserSettingsActivity : AppCompatActivity() {

    private lateinit var settingsTitleText: TextView
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var settingsDetailsText: TextView
    private lateinit var settingsVm: UserSettingsViewModel
    private var isUpdatingNotificationsSwitch = false

    /**
     This screen gives the user one place to check their saved settings and log out.
     it started as a very plain page, and later got the home button and the live settings text.
     it now keeps the settings simple and leaves the risky logout action easy to find.
     UI team might need to fix this but i think it right?
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the ViewModel used by this screen.
        settingsVm = ViewModelProvider(this)[UserSettingsViewModel::class.java]

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
            text = getString(R.string.user_settings_title)
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = getString(R.string.home_button)
        }

        val notificationsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val notificationsLabel = TextView(this).apply {
            text = getString(R.string.push_notifications_label)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        notificationsSwitch = SwitchCompat(this).apply {
            text = getString(R.string.setting_toggle_off)
        }

        settingsDetailsText = TextView(this).apply {
            text = getString(R.string.settings_loading)
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val logoutBtn = Button(this).apply {
            text = getString(R.string.logout_button)
            val topPadding = (24 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        headerRow.addView(settingsTitleText)
        headerRow.addView(homeBtn)

        notificationsRow.addView(notificationsLabel)
        notificationsRow.addView(notificationsSwitch)

        contentLayout.addView(headerRow)
        contentLayout.addView(notificationsRow)
        contentLayout.addView(settingsDetailsText)
        contentLayout.addView(logoutBtn)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )

        // Clear the session and return to login when the user logs out.
        logoutBtn.setOnClickListener {
            settingsVm.logout()
            openLogin()
        }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
        }

        // Let the user turn push notifications on or off after they have answered once.
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingNotificationsSwitch) {
                return@setOnCheckedChangeListener
            }

            updateNotificationsSwitchText(isChecked)
            settingsVm.updatePushNotifications(isChecked)
        }

        // Show the latest saved settings on screen.
        settingsVm.settingsSummary.observe(this) { summary ->
            settingsTitleText.text = summary.titleText
            applyNotificationsSwitchState(summary.notificationsEnabled)
            settingsDetailsText.text = summary.detailsText
        }

        // Return to login if the saved session is missing or no longer valid.
        settingsVm.sessionExpired.observe(this) { expired ->
            if (expired) {
                openLogin()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the settings each time the user returns to this screen.
        settingsVm.loadSettings()
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

    // Update the switch without re-triggering its save code.
    private fun applyNotificationsSwitchState(enabled: Boolean) {
        isUpdatingNotificationsSwitch = true
        notificationsSwitch.isChecked = enabled
        updateNotificationsSwitchText(enabled)
        isUpdatingNotificationsSwitch = false
    }

    // Keep the small On or Off label beside the switch easy to read.
    private fun updateNotificationsSwitchText(enabled: Boolean) {
        notificationsSwitch.text = getString(
            if (enabled) R.string.setting_toggle_on else R.string.setting_toggle_off
        )
    }

}

