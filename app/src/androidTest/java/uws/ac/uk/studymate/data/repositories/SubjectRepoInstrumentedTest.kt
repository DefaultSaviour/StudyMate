package uws.ac.uk.studymate.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
 13/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class SubjectRepoInstrumentedTest : RoomDbTestBase() {

    // SUBREP1
    // Save one subject through the repository.
    // Check the saved id, name, and color come back correctly.
    @Test
    fun addSubject_savesAndReturnsTheNewSubjectId() = runBlocking {
        val repo = SubjectRepo(db)
        val userId = insertUser(email = "subject-repo-save@example.com")

        val subjectId = repo.addSubject(userId = userId, name = "Maths", color = "#FF0000")
        val subjects = repo.getSubjects(userId)

        assertEquals(1, subjects.size)
        assertEquals(subjectId.toInt(), subjects.first().id)
        assertEquals("Maths", subjects.first().name)
        assertEquals("#FF0000", subjects.first().color)
    }

    // SUBREP2
    // Find a saved subject even when the name uses different letter casing.
    // This checks the repository name lookup is not case sensitive.
    @Test
    fun getSubjectByName_ignoresLetterCase() = runBlocking {
        val repo = SubjectRepo(db)
        val userId = insertUser(email = "subject-repo-name@example.com")
        repo.addSubject(userId = userId, name = "Physics", color = "#00FF00")

        val subject = repo.getSubjectByName(userId, "pHySiCs")

        assertNotNull(subject)
        assertEquals("Physics", subject?.name)
    }

    // SUBREP3
    // Load one subject together with the assignments linked to it.
    // Check the repository returns the correct subject and assignment count.
    @Test
    fun getSubjectsWithAssignments_returnsRelatedAssignments() = runBlocking {
        val repo = SubjectRepo(db)
        val userId = insertUser(email = "subject-repo-assignments@example.com")
        val subjectId = repo.addSubject(userId = userId, name = "English", color = "#0000FF").toInt()
        insertAssignment(userId = userId, subjectId = subjectId, title = "Essay")
        insertAssignment(userId = userId, subjectId = subjectId, title = "Presentation")

        val result = repo.getSubjectsWithAssignments(userId)

        assertEquals(1, result.size)
        assertEquals("English", result.first().subject.name)
        assertEquals(2, result.first().assignments.size)
    }

    // SUBREP4
    // Delete one saved subject through the repository.
    // Make sure it no longer appears in the user's subject list.
    @Test
    fun deleteSubject_removesTheSubject() = runBlocking {
        val repo = SubjectRepo(db)
        val userId = insertUser(email = "subject-repo-delete@example.com")
        val subjectId = repo.addSubject(userId = userId, name = "Art", color = "#ABCDEF").toInt()
        val subject = repo.getSubjects(userId).first { it.id == subjectId }

        repo.deleteSubject(subject)

        assertEquals(0, repo.getSubjects(userId).size)
        assertNull(repo.getSubjectByName(userId, "Art"))
    }
}

