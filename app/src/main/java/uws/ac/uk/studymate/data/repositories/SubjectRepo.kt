package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Subject

// Handles subject-related database operations through the DAO.
class SubjectRepo(private val db: StudyMateDatabase) {

    // Create a new subject and return its generated ID.
    suspend fun addSubject(userId: Int, name: String, color: String?) : Long {
        return db.subjectDao().insert(Subject(userId = userId, name = name, color = color))
    }

    // Get all subjects that belong to a specific user.
    suspend fun getSubjects(userId: Int) = db.subjectDao().getSubjects(userId)

    // Remove a subject from the database.
    suspend fun deleteSubject(subject: Subject) = db.subjectDao().delete(subject)
}
