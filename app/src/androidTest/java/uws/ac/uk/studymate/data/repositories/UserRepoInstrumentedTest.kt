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
 */////////////
@RunWith(AndroidJUnit4::class)
class UserRepoInstrumentedTest : RoomDbTestBase() {

    // USRREP1
    // Create a new user with the default extra rows.
    // Check the settings, stats, and saved password data were all created properly.
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
        assertEquals(true, settings?.notificationsEnabled)
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
    // Check the saved notification and dark mode values change.
    @Test
    fun updateSettings_changesSavedValues() = runBlocking {
        val repo = UserRepo(db)
        val userId = repo.createUserWithDefaults(
            name = "Jamie",
            email = "repo-settings@example.com",
            password = "Password123"
        )

        repo.updateSettings(userId = userId, notifications = false, darkMode = true)

        val settings = db.userSettingsDao().get(userId)

        assertEquals(false, settings?.notificationsEnabled)
        assertEquals(true, settings?.darkModeEnabled)
        assertEquals("UTC", settings?.timezone)
    }

    // USRREP5
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
}

