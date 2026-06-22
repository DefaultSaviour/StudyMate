package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.AssignmentTask
/*//////////////////////
Checklist items under an assignment (0.9J). All DB access goes through here.
 *//////////////////////
class AssignmentTaskRepo(private val db: StudyMateDatabase) {

    // All checklist items for an assignment, in display order.
    suspend fun getForAssignment(assignmentId: Int) =
        db.assignmentTaskDao().getForAssignment(assignmentId)

    // Append a new item at the end of the list; returns its generated id.
    suspend fun add(userId: Int, assignmentId: Int, text: String, createdAt: String): Long {
        val position = db.assignmentTaskDao().maxPosition(assignmentId) + 1
        return db.assignmentTaskDao().insert(
            AssignmentTask(
                userId = userId,
                assignmentId = assignmentId,
                text = text,
                position = position,
                createdAt = createdAt
            )
        )
    }

    // Tick / untick one item.
    suspend fun setDone(taskId: Int, done: Boolean) = db.assignmentTaskDao().setDone(taskId, done)

    // Remove one item.
    suspend fun delete(task: AssignmentTask) = db.assignmentTaskDao().delete(task)

    // (done, total) for the "N of M done" progress label.
    suspend fun progress(assignmentId: Int): Pair<Int, Int> =
        db.assignmentTaskDao().countDoneForAssignment(assignmentId) to
            db.assignmentTaskDao().countForAssignment(assignmentId)
}
