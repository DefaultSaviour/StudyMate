package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment

// Handles assignment-related database operations through the DAO.
class AssignmentRepo(private val db: StudyMateDatabase) {

    // Save a new assignment to the database.
    suspend fun addAssignment(assignment: Assignment) = db.assignmentDao().insert(assignment)

    // Get all assignments that belong to a specific user.
    suspend fun getAssignments(userId: Int) = db.assignmentDao().getAssignments(userId)
}
