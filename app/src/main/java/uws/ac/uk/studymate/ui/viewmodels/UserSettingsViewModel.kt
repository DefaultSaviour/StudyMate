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
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.SessionUserResolver
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
/*//////////////////////
Coded by Jamie Coleman
06/04/26
extended 18/04/26 — "At a glance" counts replace the old Statistics screen
 *//////////////////////
data class UserSettingsSummary(
    val titleText: String,
    val userName: String,
    val email: String,
    val memberSinceText: String,
    val notificationsEnabled: Boolean,
    val subjectCount: Int,
    val deckCount: Int,
    val flashcardCount: Int,
    val assignmentCount: Int,
    val assignmentsDueThisWeek: Int
)

class UserSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val repo = UserRepo(db)
    private val sessionResolver = SessionUserResolver(application, repo)

    private val _settingsSummary = MutableLiveData<UserSettingsSummary>()
    val settingsSummary: LiveData<UserSettingsSummary> = _settingsSummary

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _accountDeleted = MutableLiveData<Boolean>()
    val accountDeleted: LiveData<Boolean> = _accountDeleted

    data class VerifiedCredentials(val userId: Int, val email: String, val password: String)

    // Set when the user has confirmed their current password while enabling
    // biometric login from Settings. The Activity reads this, stores the
    // credentials in the encrypted store, and then calls [consumeBiometricCredentials].
    private val _biometricCredentialsToSave = MutableLiveData<VerifiedCredentials?>()
    val biometricCredentialsToSave: LiveData<VerifiedCredentials?> = _biometricCredentialsToSave

    fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUserWithMeta()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val userWithMeta = session.value
            val user = userWithMeta.user
            val userId = user.id

            val subjects = db.subjectDao().getSubjects(userId)
            val decksWithCards = db.deckDao().getDecksWithCards(userId)
            val assignments = db.assignmentDao().getAssignments(userId)

            val now = LocalDateTime.now()
            val weekFromNow = now.plusDays(7)
            val dueThisWeek = assignments.count { a ->
                val due = AssignmentDateTimeUtils.parseDueDate(a.dueDate) ?: return@count false
                !due.isBefore(now) && due.isBefore(weekFromNow)
            }

            _settingsSummary.postValue(
                UserSettingsSummary(
                    titleText = "Settings",
                    userName = user.name,
                    email = user.email,
                    memberSinceText = formatMemberSince(user.createdAt),
                    notificationsEnabled = user.pushNotificationsEnabled ?: false,
                    subjectCount = subjects.size,
                    deckCount = decksWithCards.size,
                    flashcardCount = decksWithCards.sumOf { it.cards.size },
                    assignmentCount = assignments.size,
                    assignmentsDueThisWeek = dueThisWeek
                )
            )
            _sessionExpired.postValue(false)
        }
    }

    fun updatePushNotifications(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: run {
                _sessionExpired.postValue(true)
                return@launch
            }
            repo.updatePushNotifications(session.userId, enabled)

            val app = getApplication<Application>()
            if (enabled) {
                // Re-fetch the user so the scheduler sees the flag we just flipped.
                val refreshed = repo.getUser(session.userId) ?: return@launch
                val assignments = db.assignmentDao().getAssignments(session.userId)
                uws.ac.uk.studymate.notifications.AssignmentReminderScheduler
                    .rescheduleAllForUser(app, refreshed, assignments)
            } else {
                uws.ac.uk.studymate.notifications.AssignmentReminderScheduler
                    .cancelAllForUser(app, session.userId)
            }
        }
    }

    fun logout() {
        sessionResolver.logout()
    }

    fun updateAccount(
        newName: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val cleanName = sanitizeSingleLine(newName)
        if (cleanName.isEmpty()) {
            _message.value = "Enter your name"
            return
        }

        // Password change is opt-in. If either field is filled, both must be valid.
        val wantsPasswordChange = newPassword.isNotEmpty() || confirmPassword.isNotEmpty()
        if (wantsPasswordChange) {
            if (newPassword.length < 6) {
                _message.value = "New password must be at least 6 characters"
                return
            }
            if (newPassword != confirmPassword) {
                _message.value = "Passwords don't match"
                return
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: run {
                _sessionExpired.postValue(true)
                return@launch
            }
            val current = repo.getUser(session.userId) ?: return@launch

            // Keep the existing placeholder email — single-user-per-device model
            // means it's not user-visible.
            repo.updateUserNameEmail(current, cleanName, current.email)
            if (wantsPasswordChange) {
                val refreshed = repo.getUser(current.id) ?: current
                repo.updatePassword(refreshed, newPassword)
                _message.postValue("Account and password updated")
            } else {
                _message.postValue("Account updated")
            }
            loadSettings()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: run {
                _sessionExpired.postValue(true)
                return@launch
            }
            val current = repo.getUser(session.userId) ?: return@launch
            repo.deleteUser(current)
            sessionResolver.logout()
            _accountDeleted.postValue(true)
        }
    }

    fun verifyPasswordForBiometric(password: String) {
        if (password.isBlank()) {
            _message.value = "Enter your password"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: run {
                _sessionExpired.postValue(true)
                return@launch
            }
            val current = repo.getUser(session.userId) ?: return@launch
            val authed = repo.authenticateUser(current.email, password)
            if (authed != null) {
                _biometricCredentialsToSave.postValue(
                    VerifiedCredentials(current.id, current.email, password)
                )
            } else {
                _message.postValue("Incorrect password")
            }
        }
    }

    fun consumeBiometricCredentials() {
        _biometricCredentialsToSave.value = null
    }

    private fun sanitizeSingleLine(raw: String): String {
        return raw.replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun formatMemberSince(createdAt: String?): String {
        if (createdAt.isNullOrBlank()) return "Member since today"
        return try {
            val dt = LocalDateTime.parse(createdAt)
            "Member since ${dt.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
        } catch (_: DateTimeParseException) {
            "Member since today"
        }
    }
}
