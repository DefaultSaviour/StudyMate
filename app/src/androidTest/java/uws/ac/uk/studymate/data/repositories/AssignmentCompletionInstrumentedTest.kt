package uws.ac.uk.studymate.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase

@RunWith(AndroidJUnit4::class)
class AssignmentCompletionInstrumentedTest : RoomDbTestBase() {

    // ASGCOMP1
    // setCompleted stamps and clears completed_at without touching other fields.
    @Test
    fun setCompleted_marksDoneThenNotDone() = runBlocking {
        val userId = insertUser()
        insertAssignment(userId, title = "Essay")

        val repo = AssignmentRepo(db)
        val assignment = repo.getAssignments(userId).first()
        assertNull(assignment.completedAt)

        repo.setCompleted(assignment, "2026-06-13T10:00:00Z")
        val done = repo.getAssignments(userId).first()
        assertNotNull(done.completedAt)
        assertEquals("Essay", done.title) // unchanged

        repo.setCompleted(done, null)
        assertNull(repo.getAssignments(userId).first().completedAt)
    }
}
