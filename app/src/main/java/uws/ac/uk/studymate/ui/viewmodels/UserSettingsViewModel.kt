package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.repositories.BackupRepo
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.BackupSerializer
import uws.ac.uk.studymate.util.SessionManager
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.TextSanitizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
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
    val autoLoginEnabled: Boolean,
    // Auto-login only makes sense for password accounts (it bypasses the typed
    // password). Hide the row entirely for biometric-only accounts.
    val isPasswordAccount: Boolean,
    val deckCount: Int,
    val flashcardCount: Int,
    val assignmentCount: Int,
    val assignmentsDueThisWeek: Int
)

class UserSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val repo = UserRepo(db)
    private val backupRepo = BackupRepo(db)
    private val sessionResolver = SessionUserResolver(application, repo)

    private val _settingsSummary = MutableLiveData<UserSettingsSummary>()
    val settingsSummary: LiveData<UserSettingsSummary> = _settingsSummary

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // Carries the id of the user that was deleted (so the Activity can decide
    // whether to clear that user's biometric credentials, since the session is
    // already gone by the time this fires). 0 means "no deletion".
    private val _accountDeleted = MutableLiveData<Int>()
    val accountDeleted: LiveData<Int> = _accountDeleted

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

            val decksWithCards = db.deckDao().getDecksWithCards(userId)
            val assignments = db.assignmentDao().getAssignments(userId)

            val now = LocalDateTime.now()
            val weekFromNow = now.plusDays(7)
            
            val activeAssignments = assignments.filter { 
                !AssignmentDateTimeUtils.isComplete(it.completedAt, it.dueDate, now) 
            }
            
            val dueThisWeek = activeAssignments.count { a ->
                val due = AssignmentDateTimeUtils.parseDueDate(a.dueDate) ?: return@count false
                !due.isBefore(now) && due.isBefore(weekFromNow)
            }
            
            // Count decks that are unassigned OR assigned to an active assignment
            val activeDecks = decksWithCards.filter { d ->
                if (d.deck.assignmentId == -1) true
                else assignments.find { it.id == d.deck.assignmentId }?.let {
                    !AssignmentDateTimeUtils.isComplete(it.completedAt, it.dueDate, now)
                } ?: true
            }

            _settingsSummary.postValue(
                UserSettingsSummary(
                    titleText = "Settings",
                    userName = user.name,
                    email = user.email,
                    memberSinceText = formatMemberSince(user.createdAt),
                    notificationsEnabled = user.pushNotificationsEnabled ?: false,
                    autoLoginEnabled = user.autoLoginEnabled,
                    isPasswordAccount = user.authMode == SessionManager.AUTH_MODE_PASSWORD,
                    deckCount = activeDecks.size,
                    flashcardCount = activeDecks.sumOf { it.cards.size },
                    assignmentCount = activeAssignments.size,
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
                uws.ac.uk.studymate.notifications.ReviewReminderScheduler
                    .scheduleForUser(app, session.userId)
                val customEvents = db.customEventDao().getEventsForUser(session.userId)
                customEvents.forEach { event ->
                    uws.ac.uk.studymate.notifications.CustomEventScheduler.scheduleForEvent(app, event, refreshed)
                }
            } else {
                uws.ac.uk.studymate.notifications.AssignmentReminderScheduler
                    .cancelAllForUser(app, session.userId)
                uws.ac.uk.studymate.notifications.ReviewReminderScheduler
                    .cancelForUser(app, session.userId)
                val customEvents = db.customEventDao().getEventsForUser(session.userId)
                customEvents.forEach { event ->
                    uws.ac.uk.studymate.notifications.CustomEventScheduler.cancelForEvent(app, event.id)
                }
            }
        }
    }

    fun updateAutoLogin(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: run {
                _sessionExpired.postValue(true)
                return@launch
            }
            repo.updateAutoLogin(session.userId, enabled)
        }
    }

    fun logout() {
        sessionResolver.logout()
        // No user/lastUser left — the widget should show "Not logged in" right away.
        uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
    }

    fun updateAccount(
        newName: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val cleanName = TextSanitizer.singleLine(newName)
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

            // Reject a rename that collides with another account. The `name`
            // column is uniquely indexed, so without this the DB update throws
            // SQLiteConstraintException and crashes the IO coroutine.
            val existing = repo.getUserByName(cleanName)
            if (existing != null && existing.id != current.id) {
                _message.postValue("That username is already taken")
                return@launch
            }

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
            // Capture the id before logout — the Activity needs it to decide
            // whether to clear this user's biometric credentials.
            val deletedUserId = current.id
            repo.deleteUser(current)
            sessionResolver.logout()
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            _accountDeleted.postValue(deletedUserId)
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

    // ── Data backup (export / import) ──

    // Outcome of an export/import op, observed by the Activity to toast the user.
    sealed class DataOpResult {
        data class ExportSuccess(val assignments: Int, val decks: Int, val cards: Int) : DataOpResult()
        data class ImportSuccess(val summary: BackupRepo.ImportSummary) : DataOpResult()
        data class Error(val message: String) : DataOpResult()
    }

    private val _dataOpResult = MutableLiveData<DataOpResult?>()
    val dataOpResult: LiveData<DataOpResult?> = _dataOpResult

    fun consumeDataOpResult() {
        _dataOpResult.value = null
    }

    // Write the current user's whole study tree to [uri] as backup JSON.
    fun exportTo(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: run {
                _sessionExpired.postValue(true)
                return@launch
            }
            try {
                val data = backupRepo.export(session.userId)
                val json = BackupSerializer.toJson(data, Instant.now().toString())
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw java.io.IOException("Couldn't open the file for writing.")
                _dataOpResult.postValue(
                    DataOpResult.ExportSuccess(
                        assignments = data.assignments.size,
                        decks = data.assignments.sumOf { it.decks.size },
                        cards = data.assignments.sumOf { a -> a.decks.sumOf { it.cards.size } }
                    )
                )
            } catch (e: Exception) {
                _dataOpResult.postValue(DataOpResult.Error("Couldn't save the backup."))
            }
        }
    }

    // Read a backup file at [uri] and add its contents under the current user.
    fun importFrom(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: run {
                _sessionExpired.postValue(true)
                return@launch
            }
            try {
                val raw = resolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: throw java.io.IOException("Couldn't open the file for reading.")
                val data = BackupSerializer.fromJson(raw)
                val summary = backupRepo.import(session.userId, data)
                _dataOpResult.postValue(DataOpResult.ImportSuccess(summary))
                // Restored assignments/decks/cards/tasks can change everything the
                // widget shows (next due, due-card count, week dots).
                uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
                loadSettings()  // refresh the "at a glance" counts
            } catch (e: BackupSerializer.InvalidBackupException) {
                _dataOpResult.postValue(DataOpResult.Error(e.message ?: "That file isn't a StudyMate backup."))
            } catch (e: Exception) {
                _dataOpResult.postValue(DataOpResult.Error("Couldn't read the file."))
            }
        }
    }

    private fun formatMemberSince(createdAt: String?): String {
        if (createdAt.isNullOrBlank()) return "Member since today"
        val date = parseCreatedDate(createdAt) ?: return "Member since today"
        return "Member since ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
    }

    // createdAt is stored either as an ISO-8601 instant (Instant.now().toString(),
    // e.g. "2026-06-13T10:15:30Z") for app-created users, or as a SQLite
    // CURRENT_TIMESTAMP ("2026-06-13 10:15:30", UTC) for older migrated rows.
    // LocalDateTime.parse handles neither, so try both explicitly.
    private fun parseCreatedDate(createdAt: String): LocalDate? {
        return try {
            Instant.parse(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(createdAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDate()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
