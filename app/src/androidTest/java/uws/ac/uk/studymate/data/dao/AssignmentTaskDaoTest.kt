package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.AssignmentTask
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase

/*//////////////////////
Instrumented tests for AssignmentTaskDao (0.9J) — the per-assignment checklist.
 *//////////////////////
@RunWith(AndroidJUnit4::class)
class AssignmentTaskDaoTest : RoomDbTestBase() {

    private fun task(userId: Int, assignmentId: Int, text: String, position: Int, done: Boolean = false) =
        AssignmentTask(
            userId = userId,
            assignmentId = assignmentId,
            text = text,
            isDone = done,
            position = position
        )

    @Test
    fun getForAssignment_returnsItemsInPositionOrder() = runBlocking {
        val userId = insertUser()
        val assignmentId = insertAssignment(userId)
        db.assignmentTaskDao().insert(task(userId, assignmentId, "second", position = 1))
        db.assignmentTaskDao().insert(task(userId, assignmentId, "first", position = 0))

        val tasks = db.assignmentTaskDao().getForAssignment(assignmentId)
        assertEquals(listOf("first", "second"), tasks.map { it.text })
    }

    @Test
    fun setDone_togglesTheFlag() = runBlocking {
        val userId = insertUser()
        val assignmentId = insertAssignment(userId)
        val id = db.assignmentTaskDao().insert(task(userId, assignmentId, "do it", position = 0)).toInt()

        db.assignmentTaskDao().setDone(id, true)
        assertTrue(db.assignmentTaskDao().getForAssignment(assignmentId).single().isDone)
        db.assignmentTaskDao().setDone(id, false)
        assertFalse(db.assignmentTaskDao().getForAssignment(assignmentId).single().isDone)
    }

    @Test
    fun maxPosition_isMinusOneWhenEmpty_thenHighest() = runBlocking {
        val userId = insertUser()
        val assignmentId = insertAssignment(userId)
        assertEquals(-1, db.assignmentTaskDao().maxPosition(assignmentId))
        db.assignmentTaskDao().insert(task(userId, assignmentId, "a", position = 0))
        db.assignmentTaskDao().insert(task(userId, assignmentId, "b", position = 5))
        assertEquals(5, db.assignmentTaskDao().maxPosition(assignmentId))
    }

    @Test
    fun counts_and_progressForUser_reflectDoneState() = runBlocking {
        val userId = insertUser()
        val a1 = insertAssignment(userId, title = "A1")
        val a2 = insertAssignment(userId, title = "A2")
        db.assignmentTaskDao().insert(task(userId, a1, "x", 0, done = true))
        db.assignmentTaskDao().insert(task(userId, a1, "y", 1, done = false))
        db.assignmentTaskDao().insert(task(userId, a2, "z", 0, done = false))

        assertEquals(2, db.assignmentTaskDao().countForAssignment(a1))
        assertEquals(1, db.assignmentTaskDao().countDoneForAssignment(a1))

        val progress = db.assignmentTaskDao().progressForUser(userId).associateBy { it.assignmentId }
        assertEquals(2, progress[a1]!!.total)
        assertEquals(1, progress[a1]!!.done)
        assertEquals(1, progress[a2]!!.total)
        assertEquals(0, progress[a2]!!.done)
    }

    @Test
    fun deletingAssignment_cascadesToTasks() = runBlocking {
        val userId = insertUser()
        val assignmentId = insertAssignment(userId)
        db.assignmentTaskDao().insert(task(userId, assignmentId, "gone soon", position = 0))

        val assignment = db.assignmentDao().getById(assignmentId)!!
        db.assignmentDao().delete(assignment)

        assertTrue(db.assignmentTaskDao().getForAssignment(assignmentId).isEmpty())
    }
}
