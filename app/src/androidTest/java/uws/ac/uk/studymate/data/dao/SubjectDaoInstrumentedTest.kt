package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
2/04/26
updated 13/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class SubjectDaoInstrumentedTest : RoomDbTestBase() {

    // SUBDAO1
    // Find a subject even when the name uses different letter casing.
    // This checks the lookup is not case sensitive.
    @Test
    fun getByName_ignoresLetterCase() = runBlocking {
        val userId = insertUser(email = "subjectcase@example.com")
        insertSubject(userId = userId, name = "Maths")

        val subject = db.subjectDao().getByName(userId, "mAtHs")

        assertNotNull(subject)
        assertEquals("Maths", subject?.name)
    }

    // SUBDAO2
    // Only return subjects owned by the chosen user.
    // Make sure subjects from another user stay out of the result.
    @Test
    fun getSubjects_returnsOnlyThatUsersSubjects() = runBlocking {
        val firstUserId = insertUser(email = "first-subject@example.com")
        val secondUserId = insertUser(email = "second-subject@example.com")

        insertSubject(userId = firstUserId, name = "Maths")
        insertSubject(userId = firstUserId, name = "Science")
        insertSubject(userId = secondUserId, name = "History")

        val subjects = db.subjectDao().getSubjects(firstUserId)

        assertEquals(2, subjects.size)
        assertEquals(setOf("Maths", "Science"), subjects.map { it.name }.toSet())
    }

    // SUBDAO3
    // Load each subject together with its saved assignments.
    // This checks the subject-to-assignment link is working.
    @Test
    fun getSubjectsWithAssignments_returnsAssignmentsUnderEachSubject() = runBlocking {
        val userId = insertUser(email = "subject-assignment@example.com")
        val mathsId = insertSubject(userId = userId, name = "Maths")
        val scienceId = insertSubject(userId = userId, name = "Science")

        insertAssignment(userId = userId, subjectId = mathsId, title = "Algebra")
        insertAssignment(userId = userId, subjectId = mathsId, title = "Geometry")
        insertAssignment(userId = userId, subjectId = scienceId, title = "Lab Report")

        val result = db.subjectDao().getSubjectsWithAssignments(userId)

        val maths = result.first { it.subject.id == mathsId }
        val science = result.first { it.subject.id == scienceId }

        assertEquals(2, maths.assignments.size)
        assertEquals(1, science.assignments.size)
    }

    // SUBDAO4
    // Load a subject together with its progress row.
    // Make sure the completed and total values match what was saved.
    @Test
    fun getSubjectsWithProgress_returnsMatchingProgressRecord() = runBlocking {
        val userId = insertUser(email = "subject-progress@example.com")
        val subjectId = insertSubject(userId = userId, name = "Biology")
        insertProgress(userId = userId, subjectId = subjectId, completedTasks = 3, totalTasks = 7)

        val result = db.subjectDao().getSubjectsWithProgress(userId)

        assertEquals(1, result.size)
        assertEquals(3, result.first().progress?.completedTasks)
        assertEquals(7, result.first().progress?.totalTasks)
    }

    // SUBDAO5
    // Load a subject together with its decks.
    // This checks the subject-to-deck link returns the right count.
    @Test
    fun getSubjectsWithDecks_returnsDecksForEachSubject() = runBlocking {
        val userId = insertUser(email = "subject-decks@example.com")
        val subjectId = insertSubject(userId = userId, name = "Chemistry")
        insertDeck(userId = userId, subjectId = subjectId, name = "Week 1")
        insertDeck(userId = userId, subjectId = subjectId, name = "Week 2")

        val result = db.subjectDao().getSubjectsWithDecks(userId)

        assertEquals(1, result.size)
        assertEquals(2, result.first().decks.size)
    }

    // SUBDAO6
    // Delete one subject and its linked child rows.
    // The subject, assignments, and progress row should all be gone.
    @Test
    fun deleteSubject_removesItsAssignmentsAndProgress() = runBlocking {
        val userId = insertUser(email = "subject-delete@example.com")
        val subjectId = insertSubject(userId = userId, name = "Physics")
        insertAssignment(userId = userId, subjectId = subjectId, title = "Forces")
        insertProgress(userId = userId, subjectId = subjectId, completedTasks = 1, totalTasks = 4)

        db.subjectDao().delete(
            Subject(
                id = subjectId,
                userId = userId,
                name = "Physics",
                color = "#FF0000"
            )
        )

        assertNull(db.subjectDao().getByName(userId, "Physics"))
        assertEquals(0, db.assignmentDao().getAssignments(userId).size)
        assertEquals(0, db.subjectDao().getSubjectsWithProgress(userId).size)
    }
}

