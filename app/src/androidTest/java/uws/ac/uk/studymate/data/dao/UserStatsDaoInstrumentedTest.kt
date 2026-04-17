package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.UserStats
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
2/04/26
updated 13/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class UserStatsDaoInstrumentedTest : RoomDbTestBase() {

    // STTDAO1
    // Save one stats row for a user and load it back.
    // Check the assignment, flashcard, and streak numbers match.
    @Test
    fun insertStats_canBeLoadedForTheUser() = runBlocking {
        val userId = insertUser(email = "stats-load@example.com")
        insertStats(userId = userId, assignmentsCount = 3, flashcardsCount = 9, streakDays = 4)

        val stats = db.userStatsDao().get(userId)

        assertNotNull(stats)
        assertEquals(3, stats?.assignmentsCount)
        assertEquals(9, stats?.flashcardsCount)
        assertEquals(4, stats?.streakDays)
    }

    // STTDAO2
    // Update one stats row that already exists.
    // Make sure all three saved counts change correctly.
    @Test
    fun updateStats_changesSavedValues() = runBlocking {
        val userId = insertUser(email = "stats-update@example.com")
        insertStats(userId = userId, assignmentsCount = 1, flashcardsCount = 2, streakDays = 0)

        db.userStatsDao().update(
            UserStats(
                userId = userId,
                assignmentsCount = 7,
                flashcardsCount = 14,
                streakDays = 5
            )
        )

        val updated = db.userStatsDao().get(userId)

        assertEquals(7, updated?.assignmentsCount)
        assertEquals(14, updated?.flashcardsCount)
        assertEquals(5, updated?.streakDays)
    }
}

