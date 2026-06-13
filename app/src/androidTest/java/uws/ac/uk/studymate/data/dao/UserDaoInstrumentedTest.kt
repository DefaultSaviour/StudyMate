package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
2/04/26
updated 13/04/26
 updated 16/04/26 - added push notifications
 */////////////
@RunWith(AndroidJUnit4::class)
class UserDaoInstrumentedTest : RoomDbTestBase() {

    // USRDAO1
    // Save one user and find them again in both lookup methods.
    // This checks loading by id and by email.
    @Test
    fun insertUser_canBeFoundByIdAndEmail() = runBlocking {
        val userId = insertUser(email = "findme@example.com")

        val byId = db.userDao().getById(userId)
        val byEmail = db.userDao().getByEmail("findme@example.com")

        assertNotNull(byId)
        assertNotNull(byEmail)
        assertEquals(userId, byId?.id)
        assertEquals("findme@example.com", byEmail?.email)
    }

    // USRDAO2
    // Return every user that was saved.
    // This checks the full list count is correct.
    @Test
    fun getAll_returnsEverySavedUser() = runBlocking {
        insertUser(email = "one@example.com")
        insertUser(email = "two@example.com")

        val users = db.userDao().getAll()

        assertEquals(2, users.size)
    }

    // USRDAO3
    // Load one user together with their settings and stats.
    // This checks the joined user data comes back in one result, including the push notification choice.
    @Test
    fun getUserWithMeta_returnsUserSettingsAndStatsTogether() = runBlocking {
        val userId = insertUser(email = "meta@example.com", pushNotificationsEnabled = false)
        insertSettings(userId = userId, darkModeEnabled = true)
        insertStats(userId = userId, assignmentsCount = 5, flashcardsCount = 8, streakDays = 2)

        val result = db.userDao().getUserWithMeta(userId)

        assertNotNull(result)
        assertEquals(userId, result?.user?.id)
        assertEquals(false, result?.user?.pushNotificationsEnabled)
        assertEquals(true, result?.settings?.darkModeEnabled)
        assertEquals(5, result?.stats?.assignmentsCount)
        assertEquals(8, result?.stats?.flashcardsCount)
        assertEquals(2, result?.stats?.streakDays)
    }

    // USRDAO4
    // Delete one user and remove the rows linked to them.
    // Their settings and stats should disappear as well.
    @Test
    fun deleteUser_removesTheirSettingsAndStats() = runBlocking {
        val userId = insertUser(email = "delete@example.com")
        insertSettings(userId = userId)
        insertStats(userId = userId)

        db.userDao().delete(
            User(
                id = userId,
                name = "Test User",
                email = "delete@example.com",
                passwordHash = "hash",
                passwordSalt = "salt"
            )
        )

        assertNull(db.userDao().getById(userId))
        assertNull(db.userSettingsDao().get(userId))
        assertNull(db.userStatsDao().get(userId))
    }

    // USRDAO5
    // Stop two users from sharing the same username.
    // (As of v8, `name` is the unique identifier — email is no longer unique.)
    // The second save should fail and the table should keep one row.
    @Test
    fun duplicateName_isRejected() = runBlocking {
        insertUser(name = "Jamie")

        val error = runCatching {
            insertUser(name = "Jamie")
        }.exceptionOrNull()

        assertTrue(error != null)
        assertEquals(1, db.userDao().getAll().size)
    }

    // USRDAO6
    // Two accounts may now share an email (it's an internal placeholder).
    // Both saves should succeed.
    @Test
    fun duplicateEmail_isAllowed() = runBlocking {
        insertUser(name = "First", email = "shared@example.com")
        insertUser(name = "Second", email = "shared@example.com")

        assertEquals(2, db.userDao().getAll().size)
    }

    // USRDAO7
    // Look a user up by username, case-insensitively.
    @Test
    fun getByName_isCaseInsensitive() = runBlocking {
        val userId = insertUser(name = "Jamie")

        val found = db.userDao().getByName("jAmIe")

        assertNotNull(found)
        assertEquals(userId, found?.id)
    }

    // USRDAO8
    // Return the single biometric-only account, or null when none exists.
    @Test
    fun getBiometricOnlyUser_returnsOnlyTheBiometricAccount() = runBlocking {
        assertNull(db.userDao().getBiometricOnlyUser())

        insertUser(name = "PasswordUser", authMode = "password")
        val bioId = insertUser(name = "BioUser", authMode = "biometric_only")

        val found = db.userDao().getBiometricOnlyUser()

        assertNotNull(found)
        assertEquals(bioId, found?.id)
        assertEquals("biometric_only", found?.authMode)
    }
}

