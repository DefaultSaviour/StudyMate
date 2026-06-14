package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
/*//////////////////////
Coded by Jamie Coleman
 12/03/26
 *//////////////////////
// Handles assignment-related database operations through the DAO.
class AssignmentRepo(private val db: StudyMateDatabase) {

    // Save a new assignment to the database and return its generated id.
    suspend fun addAssignment(assignment: Assignment) = db.assignmentDao().insert(assignment)

    // Look up a single assignment by id; null if it no longer exists.
    suspend fun getAssignmentById(id: Int) = db.assignmentDao().getById(id)

    // Update an existing assignment's details.
    suspend fun updateAssignment(assignment: Assignment) = db.assignmentDao().update(assignment)

    // Remove an assignment from the database.
    suspend fun deleteAssignment(assignment: Assignment) = db.assignmentDao().delete(assignment)

    // Get all assignments that belong to a specific user.
    suspend fun getAssignments(userId: Int) = db.assignmentDao().getAssignments(userId)

    // Mark an assignment done (pass an ISO instant) or not done (pass null).
    suspend fun setCompleted(assignment: Assignment, completedAt: String?) =
        db.assignmentDao().setCompleted(assignment.id, completedAt)
}
