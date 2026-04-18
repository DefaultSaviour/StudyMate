package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*///////////////////
Coded by Jamie Coleman
2/04/26
*////////////////////
@RunWith(AndroidJUnit4::class)
class AssignmentDaoInstrumentedTest : RoomDbTestBase() {

    // ASSDAO1
    // Only return assignments for the selected user.
    // Also check they come back in due date order.
    @Test
    fun getAssignments_returnsOnlyThatUsersAssignmentsInDueDateOrder() = runBlocking {
        val firstUserId = insertUser(email = "assignments-one@example.com")
        val secondUserId = insertUser(email = "assignments-two@example.com")
        val firstSubjectId = insertSubject(userId = firstUserId, name = "Maths")
        val secondSubjectId = insertSubject(userId = secondUserId, name = "Science")

        insertAssignment(userId = firstUserId, subjectId = firstSubjectId, title = "Later", dueDate = "2026-05-10T12:00")
        insertAssignment(userId = firstUserId, subjectId = firstSubjectId, title = "Sooner", dueDate = "2026-05-01T12:00")
        insertAssignment(userId = secondUserId, subjectId = secondSubjectId, title = "Other User", dueDate = "2026-04-01T12:00")

        val assignments = db.assignmentDao().getAssignments(firstUserId)

        assertEquals(2, assignments.size)
        assertEquals("Sooner", assignments[0].title)
        assertEquals("Later", assignments[1].title)
    }

    // ASSDAO2
    // Update one saved assignment.
    // Check the new title, due date, and icon were stored.
    @Test
    fun updateAssignment_changesSavedValues() = runBlocking {
        val userId = insertUser(email = "assignment-update@example.com")
        val subjectId = insertSubject(userId = userId, name = "English")
        insertAssignment(userId = userId, subjectId = subjectId, title = "Draft", dueDate = "2026-05-01T09:00")

        val saved = db.assignmentDao().getAssignments(userId).first()
        db.assignmentDao().update(
            saved.copy(
                title = "Final Draft",
                dueDate = "2026-05-02T10:00",
                icon = "book"
            )
        )

        val updated = db.assignmentDao().getAssignments(userId).first()

        assertEquals("Final Draft", updated.title)
        assertEquals("2026-05-02T10:00", updated.dueDate)
        assertEquals("book", updated.icon)
    }

    // ASSDAO3
    // Delete one saved assignment.
    // Make sure it no longer appears in the user's list.
    @Test
    fun deleteAssignment_removesItFromTheList() = runBlocking {
        val userId = insertUser(email = "assignment-delete@example.com")
        val subjectId = insertSubject(userId = userId, name = "Art")
        insertAssignment(userId = userId, subjectId = subjectId, title = "Poster")

        val saved = db.assignmentDao().getAssignments(userId).first()
        db.assignmentDao().delete(saved)

        assertEquals(0, db.assignmentDao().getAssignments(userId).size)
    }
}

