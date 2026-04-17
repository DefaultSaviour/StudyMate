package uws.ac.uk.studymate.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
 12/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class AssignmentRepoInstrumentedTest : RoomDbTestBase() {

    // ASSREP1
    // Save one assignment through the repository.
    // Check the saved title and icon can be loaded back.
    @Test
    fun addAssignment_savesTheAssignment() = runBlocking {
        val repo = AssignmentRepo(db)
        val userId = insertUser(email = "assignment-repo-save@example.com")
        val subjectId = insertSubject(userId = userId, name = "Maths")

        repo.addAssignment(
            Assignment(
                userId = userId,
                subjectId = subjectId,
                title = "Worksheet",
                dueDate = "2026-06-01T10:00",
                icon = "calculator"
            )
        )

        val assignments = repo.getAssignments(userId)

        assertEquals(1, assignments.size)
        assertEquals("Worksheet", assignments.first().title)
        assertEquals("calculator", assignments.first().icon)
    }

    // ASSREP2
    // Only return assignments that belong to the selected user.
    // Also check the repository still gives them back in due date order.
    @Test
    fun getAssignments_returnsOnlyThatUsersAssignmentsInOrder() = runBlocking {
        val repo = AssignmentRepo(db)
        val firstUserId = insertUser(email = "assignment-repo-one@example.com")
        val secondUserId = insertUser(email = "assignment-repo-two@example.com")
        val firstSubjectId = insertSubject(userId = firstUserId, name = "Science")
        val secondSubjectId = insertSubject(userId = secondUserId, name = "History")

        repo.addAssignment(
            Assignment(
                userId = firstUserId,
                subjectId = firstSubjectId,
                title = "Later",
                dueDate = "2026-07-03T12:00",
                icon = "book"
            )
        )
        repo.addAssignment(
            Assignment(
                userId = firstUserId,
                subjectId = firstSubjectId,
                title = "Sooner",
                dueDate = "2026-07-01T09:00",
                icon = "book"
            )
        )
        repo.addAssignment(
            Assignment(
                userId = secondUserId,
                subjectId = secondSubjectId,
                title = "Other User",
                dueDate = "2026-06-01T09:00",
                icon = "book"
            )
        )

        val assignments = repo.getAssignments(firstUserId)

        assertEquals(2, assignments.size)
        assertEquals("Sooner", assignments[0].title)
        assertEquals("Later", assignments[1].title)
    }
}

