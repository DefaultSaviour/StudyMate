package uws.ac.uk.studymate.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/*//////////////////////
Unit tests for the "is this assignment complete?" rule (0.9I). The app has no
"overdue" state: complete = marked done OR due date passed.
 *//////////////////////
class AssignmentDateTimeUtilsTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 6, 21, 12, 0)

    @Test
    fun markedDoneIsCompleteEvenWhenDueInFuture() {
        // A future due date doesn't matter once it's been ticked off.
        val future = now.plusDays(3).toString()
        assertTrue(AssignmentDateTimeUtils.isComplete(completedAt = "2026-06-20T09:00:00", dueDate = future, now = now))
    }

    @Test
    fun pastDueIsComplete() {
        val past = now.minusMinutes(1).toString()
        assertTrue(AssignmentDateTimeUtils.isComplete(completedAt = null, dueDate = past, now = now))
    }

    @Test
    fun futureDueIsNotComplete() {
        val future = now.plusHours(1).toString()
        assertFalse(AssignmentDateTimeUtils.isComplete(completedAt = null, dueDate = future, now = now))
    }

    @Test
    fun nullDueAndNotMarkedIsNotComplete() {
        assertFalse(AssignmentDateTimeUtils.isComplete(completedAt = null, dueDate = null, now = now))
    }

    @Test
    fun unparseableDueAndNotMarkedIsNotComplete() {
        assertFalse(AssignmentDateTimeUtils.isComplete(completedAt = null, dueDate = "not a date", now = now))
    }
}
