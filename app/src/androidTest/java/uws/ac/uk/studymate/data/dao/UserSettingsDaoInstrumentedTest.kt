package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.UserSettings
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
2/04/26
updated 13/04/26
 updated 16/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class UserSettingsDaoInstrumentedTest : RoomDbTestBase() {

    // SETDAO1
    // Save one settings row for a user and load it back.
    // Check the saved dark mode value and timezone match.
    @Test
    fun insertSettings_canBeLoadedForTheUser() = runBlocking {
        val userId = insertUser(email = "settings-load@example.com")
        insertSettings(userId = userId, darkModeEnabled = true, timezone = "Europe/London")

        val settings = db.userSettingsDao().get(userId)

        assertNotNull(settings)
        assertEquals(true, settings?.darkModeEnabled)
        assertEquals("Europe/London", settings?.timezone)
    }

    // SETDAO2
    // Update one settings row that already exists.
    // Make sure the changed dark mode value and timezone were stored.
    @Test
    fun updateSettings_changesSavedValues() = runBlocking {
        val userId = insertUser(email = "settings-update@example.com")
        insertSettings(userId = userId, darkModeEnabled = false, timezone = "UTC")

        db.userSettingsDao().update(
            UserSettings(
                userId = userId,
                darkModeEnabled = true,
                timezone = "Europe/London"
            )
        )

        val updated = db.userSettingsDao().get(userId)

        assertEquals(true, updated?.darkModeEnabled)
        assertEquals("Europe/London", updated?.timezone)
    }
}

