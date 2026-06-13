package uws.ac.uk.studymate.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
 13/04/26
  updated 16/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class UserRepoInstrumentedTest : RoomDbTestBase() {

    // USRREP1
    // Create a new user with the default extra rows.
    // Check the settings, stats, unanswered notification flag, and saved password data were all created properly.
    @Test
    fun createUserWithDefaults_createsUserSettingsAndStatsAndHashesPassword() = runBlocking {
        val repo = UserRepo(db)

        val userId = repo.createUserWithDefaults(
            name = "Jamie",
            email = "repo-create@example.com",
            password = "Secret123"
        )

        val user = db.userDao().getById(userId)
        val settings = db.userSettingsDao().get(userId)
        val stats = db.userStatsDao().get(userId)

        assertNotNull(user)
        assertNotNull(settings)
        assertNotNull(stats)
        assertNotEquals("Secret123", user?.passwordHash)
        assertTrue(user?.passwordSalt?.isNotBlank() == true)
        assertEquals(null, user?.pushNotificationsEnabled)
        assertEquals(false, settings?.darkModeEnabled)
        assertEquals("UTC", settings?.timezone)
        assertEquals(0, stats?.assignmentsCount)
        assertEquals(0, stats?.flashcardsCount)
        assertEquals(0, stats?.streakDays)
    }

    // USRREP2
    // Log in with the correct email and password.
    // Make sure the repository returns the saved user.
    @Test
    fun authenticateUser_returnsUserWhenPasswordIsCorrect() = runBlocking {
        val repo = UserRepo(db)
        repo.createUserWithDefaults(
            name = "Jamie",
            email = "repo-login@example.com",
            password = "CorrectPassword"
        )

        val user = repo.authenticateUser("repo-login@example.com", "CorrectPassword")

        assertNotNull(user)
        assertEquals("repo-login@example.com", user?.email)
    }

    // USRREP3
    // Try to log in with the wrong password.
    // The repository should reject the login and return nothing.
    @Test
    fun authenticateUser_returnsNullWhenPasswordIsWrong() = runBlocking {
        val repo = UserRepo(db)
        repo.createUserWithDefaults(
            name = "Jamie",
            email = "repo-wrong-password@example.com",
            password = "CorrectPassword"
        )

        val user = repo.authenticateUser("repo-wrong-password@example.com", "WrongPassword")

        assertNull(user)
    }

    // USRREP4
    // Update one user's settings through the repository.
    // Check the saved notification answer moves onto the user row and dark mode still updates.
    @Test
    fun updateSettings_changesSavedValues() = runBlocking {
        val repo = UserRepo(db)
        val userId = repo.createUserWithDefaults(
            name = "Jamie",
            email = "repo-settings@example.com",
            password = "Password123"
        )

        repo.updateSettings(userId = userId, notifications = false, darkMode = true)

        val user = db.userDao().getById(userId)
        val settings = db.userSettingsDao().get(userId)

        assertEquals(false, user?.pushNotificationsEnabled)
        assertEquals(true, settings?.darkModeEnabled)
        assertEquals("UTC", settings?.timezone)
    }

    // USRREP5
    // Save one user's push notification choice through the new helper.
    // This checks the answer is stored on the user row.
    @Test
    fun updatePushNotifications_changesSavedUserChoice() = runBlocking {
        val repo = UserRepo(db)
        val userId = repo.createUserWithDefaults(
            name = "Jamie",
            email = "repo-push@example.com",
            password = "Password123"
        )

        repo.updatePushNotifications(userId, true)

        val user = db.userDao().getById(userId)

        assertEquals(true, user?.pushNotificationsEnabled)
    }

    // USRREP6
    // Delete one user through the repository.
    // Their linked settings and stats rows should be removed too.
    @Test
    fun deleteUser_removesTheUserAndTheirDefaultRows() = runBlocking {
        val repo = UserRepo(db)
        val userId = repo.createUserWithDefaults(
            name = "Jamie",
            email = "repo-delete@example.com",
            password = "Password123"
        )
        val user = db.userDao().getById(userId)

        repo.deleteUser(user!!)

        assertNull(db.userDao().getById(userId))
        assertNull(db.userSettingsDao().get(userId))
        assertNull(db.userStatsDao().get(userId))
    }

    // USRREP7
    // Create a biometric-only account and confirm the auth mode is stored
    // and surfaced through the one-bio lookup.
    @Test
    fun createUserWithDefaults_storesBiometricAuthMode() = runBlocking {
        val repo = UserRepo(db)

        val bioId = repo.createUserWithDefaults(
            name = "BioUser",
            email = "bio@example.com",
            password = "Secret123",
            authMode = "biometric_only"
        )

        val user = db.userDao().getById(bioId)
        assertEquals("biometric_only", user?.authMode)
        assertEquals(bioId, repo.getBiometricOnlyUser()?.id)
    }

    // USRREP8
    // Multi-user, one-bio model: several password accounts can coexist, but
    // getBiometricOnlyUser returns the single biometric account (or null).
    @Test
    fun getBiometricOnlyUser_isNullUntilABiometricAccountExists() = runBlocking {
        val repo = UserRepo(db)
        repo.createUserWithDefaults("PwOne", "pw1@example.com", "Secret123")
        repo.createUserWithDefaults("PwTwo", "pw2@example.com", "Secret123")

        assertNull(repo.getBiometricOnlyUser())

        val bioId = repo.createUserWithDefaults(
            name = "Bio",
            email = "bio@example.com",
            password = "Secret123",
            authMode = "biometric_only"
        )

        assertEquals(bioId, repo.getBiometricOnlyUser()?.id)
        assertEquals(3, repo.getAllUsers().size)
    }

    // USRREP9
    // Look a user up by their username through the repository (case-insensitive).
    @Test
    fun getUserByName_findsTheAccountCaseInsensitively() = runBlocking {
        val repo = UserRepo(db)
        val userId = repo.createUserWithDefaults("Jamie", "name@example.com", "Secret123")

        val found = repo.getUserByName("JAMIE")

        assertNotNull(found)
        assertEquals(userId, found?.id)
    }

    // USRREP10
    // Usernames must be unique — creating a second account with a taken name fails.
    @Test
    fun createUserWithDefaults_rejectsDuplicateName() = runBlocking {
        val repo = UserRepo(db)
        repo.createUserWithDefaults("Jamie", "first@example.com", "Secret123")

        val error = runCatching {
            repo.createUserWithDefaults("Jamie", "second@example.com", "Secret123")
        }.exceptionOrNull()

        assertTrue(error != null)
        assertEquals(1, repo.getAllUsers().size)
    }
}

