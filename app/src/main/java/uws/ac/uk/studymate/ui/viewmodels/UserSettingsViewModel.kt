package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionUserResolver
/*//////////////////////
Coded by Jamie Coleman
06/04/26
fixed 09/04/26
 updated 16/04/26
  updated 16/04/26
 *//////////////////////
// Holds the text that the user settings screen needs to display.
data class UserSettingsSummary(
    val titleText: String,
    val detailsText: String,
    val notificationsEnabled: Boolean
)

class UserSettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load the current user's settings.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep user lookup logic out of the ViewModel.
    private val repo = UserRepo(db)

    // Use the shared session resolver so login validation stays consistent with other screens.
    private val sessionResolver = SessionUserResolver(application, repo)

    // This private value stores the latest settings text.
    // It is mutable here so only the ViewModel can change it.
    private val _settingsSummary = MutableLiveData<UserSettingsSummary>()

    // This public version lets the UI observe the latest settings data.
    val settingsSummary: LiveData<UserSettingsSummary> = _settingsSummary

    // This private value stores whether the session is missing or no longer valid.
    // It is mutable here so only the ViewModel can change it.
    private val _sessionExpired = MutableLiveData<Boolean>()

    // This public version lets the UI react when it needs to send the user back to login.
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun loadSettings() {
        // Run the database work on a background thread.
        viewModelScope.launch(Dispatchers.IO) {

            // Stop early when there is no valid logged-in user.
            val session = sessionResolver.requireUserWithMeta()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val userWithMeta = session.value

            // Build the settings text for this screen.
            val settings = userWithMeta.settings
            val summary = UserSettingsSummary(
                titleText = "Settings for ${userWithMeta.user.name}",
                notificationsEnabled = userWithMeta.user.pushNotificationsEnabled ?: false,
                detailsText = buildSettingsText(
                    darkModeEnabled = settings?.darkModeEnabled ?: false,
                    timezone = settings?.timezone ?: "UTC"
                )
            )

            // Send the finished settings data back to the UI.
            _settingsSummary.postValue(summary)
            _sessionExpired.postValue(false)
        }
    }

    // Save the user's push notification choice from the settings screen.
    fun updatePushNotifications(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: run {
                _sessionExpired.postValue(true)
                return@launch
            }

            repo.updatePushNotifications(session.userId, enabled)
        }
    }

    // Clear the saved session when the user logs out from the settings screen.
    fun logout() {
        sessionResolver.logout()
    }

    // Turn the saved settings into plain English for the user settings screen.
    private fun buildSettingsText(
        darkModeEnabled: Boolean,
        timezone: String
    ): String {
        val darkModeText = if (darkModeEnabled) "On" else "Off"
        return "Dark mode: $darkModeText\nTimezone: $timezone"
    }
}

