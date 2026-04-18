package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

        // Inflate the XML layout and bind controls used by this screen.
        setContentView(R.layout.activity_user_settings)
        settingsTitleText = findViewById(R.id.settingsTitleText)
        val homeBtn: Button = findViewById(R.id.homeBtn)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        settingsDetailsText = findViewById(R.id.settingsDetailsText)
        val logoutBtn: Button = findViewById(R.id.logoutBtn)

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

