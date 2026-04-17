package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.SubjectProgress
/*//////////////////////
Coded by Jamie Coleman
 11/03/26
 *//////////////////////
// Handles subject progress operations through the DAO.
class SubjectProgressRepo(private val db: StudyMateDatabase) {

    // Save a new progress record for a subject.
    suspend fun addProgress(progress: SubjectProgress) = db.subjectProgressDao().insert(progress)

    // Get all progress records that belong to a specific user.
    suspend fun getProgressForUser(userId: Int) = db.subjectProgressDao().getAll(userId)
}
