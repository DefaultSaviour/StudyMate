package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment

class AssignmentRepo(private val db: StudyMateDatabase) {

    suspend fun addAssignment(assignment: Assignment) = db.assignmentDao().insert(assignment)

    suspend fun getAssignments(userId: Int) = db.assignmentDao().getAssignments(userId)
}

// i didint think i needed delete here either???