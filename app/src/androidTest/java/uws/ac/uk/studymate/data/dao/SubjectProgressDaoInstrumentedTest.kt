package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
2/04/26
updated 13/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class SubjectProgressDaoInstrumentedTest : RoomDbTestBase() {

    // PRGDAO1
    // Only return progress rows for the selected user.
    // This checks another user's progress is not included.
    @Test
    fun getAll_returnsOnlyThatUsersProgressRows() = runBlocking {
        val firstUserId = insertUser(email = "progress-one@example.com")
        val secondUserId = insertUser(email = "progress-two@example.com")
        val firstSubjectId = insertSubject(userId = firstUserId, name = "Maths")
        val secondSubjectId = insertSubject(userId = secondUserId, name = "Science")

        insertProgress(userId = firstUserId, subjectId = firstSubjectId, completedTasks = 2, totalTasks = 5)
        insertProgress(userId = secondUserId, subjectId = secondSubjectId, completedTasks = 1, totalTasks = 3)

        val progressRows = db.subjectProgressDao().getAll(firstUserId)

        assertEquals(1, progressRows.size)
        assertEquals(firstSubjectId, progressRows.first().subjectId)
    }

    // PRGDAO2
    // Update one saved progress row.
    // Check the completed and total task values both change.
    @Test
    fun updateProgress_changesSavedValues() = runBlocking {
        val userId = insertUser(email = "progress-update@example.com")
        val subjectId = insertSubject(userId = userId, name = "Computing")
        insertProgress(userId = userId, subjectId = subjectId, completedTasks = 1, totalTasks = 4)

        val saved = db.subjectProgressDao().getAll(userId).first()
        db.subjectProgressDao().update(saved.copy(completedTasks = 4, totalTasks = 6))

        val updated = db.subjectProgressDao().getAll(userId).first()

        assertEquals(4, updated.completedTasks)
        assertEquals(6, updated.totalTasks)
    }

    // PRGDAO3
    // Stop the same user and subject pair being saved twice.
    // The table should keep only the original row.
    @Test
    fun duplicateUserAndSubjectPair_isRejected() = runBlocking {
        val userId = insertUser(email = "progress-duplicate@example.com")
        val subjectId = insertSubject(userId = userId, name = "Music")
        insertProgress(userId = userId, subjectId = subjectId, completedTasks = 1, totalTasks = 2)

        val error = runCatching {
            insertProgress(userId = userId, subjectId = subjectId, completedTasks = 2, totalTasks = 5)
        }.exceptionOrNull()

        assertTrue(error != null)
        assertEquals(1, db.subjectProgressDao().getAll(userId).size)
    }
}

