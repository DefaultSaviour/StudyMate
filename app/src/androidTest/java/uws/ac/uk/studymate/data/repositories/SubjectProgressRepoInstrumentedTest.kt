package uws.ac.uk.studymate.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.SubjectProgress
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
 13/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class SubjectProgressRepoInstrumentedTest : RoomDbTestBase() {

    // PRGREP1
    // Save one subject progress row through the repository.
    // Check the completed and total task values are returned correctly.
    @Test
    fun addProgress_savesTheProgressRow() = runBlocking {
        val repo = SubjectProgressRepo(db)
        val userId = insertUser(email = "progress-repo-save@example.com")
        val subjectId = insertSubject(userId = userId, name = "Biology")

        repo.addProgress(
            SubjectProgress(
                userId = userId,
                subjectId = subjectId,
                completedTasks = 2,
                totalTasks = 6
            )
        )

        val progressRows = repo.getProgressForUser(userId)

        assertEquals(1, progressRows.size)
        assertEquals(2, progressRows.first().completedTasks)
        assertEquals(6, progressRows.first().totalTasks)
    }

    // PRGREP2
    // Only return progress rows for the selected user.
    // Make sure another user's row is not included.
    @Test
    fun getProgressForUser_returnsOnlyThatUsersRows() = runBlocking {
        val repo = SubjectProgressRepo(db)
        val firstUserId = insertUser(email = "progress-repo-one@example.com")
        val secondUserId = insertUser(email = "progress-repo-two@example.com")
        val firstSubjectId = insertSubject(userId = firstUserId, name = "Maths")
        val secondSubjectId = insertSubject(userId = secondUserId, name = "Science")

        repo.addProgress(
            SubjectProgress(
                userId = firstUserId,
                subjectId = firstSubjectId,
                completedTasks = 1,
                totalTasks = 4
            )
        )
        repo.addProgress(
            SubjectProgress(
                userId = secondUserId,
                subjectId = secondSubjectId,
                completedTasks = 3,
                totalTasks = 5
            )
        )

        val progressRows = repo.getProgressForUser(firstUserId)

        assertEquals(1, progressRows.size)
        assertEquals(firstSubjectId, progressRows.first().subjectId)
    }
}

