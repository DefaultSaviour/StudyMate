package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.FocusSession
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase

/*//////////////////////
Instrumented tests for FocusSessionDao (0.9J) — logged focus-timer blocks that
power the "Focused today / this week" statistics.
 *//////////////////////
@RunWith(AndroidJUnit4::class)
class FocusSessionDaoTest : RoomDbTestBase() {

    @Test
    fun sumFocusedSecondsSince_addsOnlyRowsAtOrAfterTheCutoff() = runBlocking {
        val userId = insertUser()
        val assignmentId = insertAssignment(userId)
        // ISO instants sort lexicographically, so a string compare is a time compare.
        db.focusSessionDao().insert(
            FocusSession(userId = userId, assignmentId = assignmentId, focusedSeconds = 1500, endedAt = "2026-06-22T09:00:00Z")
        )
        db.focusSessionDao().insert(
            FocusSession(userId = userId, assignmentId = null, focusedSeconds = 600, endedAt = "2026-06-22T15:00:00Z")
        )
        db.focusSessionDao().insert(
            FocusSession(userId = userId, assignmentId = assignmentId, focusedSeconds = 999, endedAt = "2026-06-20T09:00:00Z")
        )

        // From start of 22 June: 1500 + 600 (the 20 June row is excluded).
        assertEquals(2100, db.focusSessionDao().sumFocusedSecondsSince(userId, "2026-06-22T00:00:00Z"))
        // From a week earlier: all three.
        assertEquals(3099, db.focusSessionDao().sumFocusedSecondsSince(userId, "2026-06-15T00:00:00Z"))
    }

    @Test
    fun sumFocusedSecondsSince_isZeroWhenNoRows() = runBlocking {
        val userId = insertUser()
        assertEquals(0, db.focusSessionDao().sumFocusedSecondsSince(userId, "2026-06-15T00:00:00Z"))
    }

    @Test
    fun sumFocusedSecondsSince_isScopedPerUser() = runBlocking {
        val userA = insertUser(name = "A")
        val userB = insertUser(name = "B")
        db.focusSessionDao().insert(
            FocusSession(userId = userA, assignmentId = null, focusedSeconds = 1000, endedAt = "2026-06-22T09:00:00Z")
        )
        db.focusSessionDao().insert(
            FocusSession(userId = userB, assignmentId = null, focusedSeconds = 2000, endedAt = "2026-06-22T09:00:00Z")
        )
        assertEquals(1000, db.focusSessionDao().sumFocusedSecondsSince(userA, "2026-06-01T00:00:00Z"))
    }
}
