package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.relations.SubjectWithAssignments
/*//////////////////////
Coded by Jamie Coleman
 11/03/26
  updated 7/04/26
 *//////////////////////
// subject database operations through the DAO.
class SubjectRepo(private val db: StudyMateDatabase) {

    // Create a new subject and return its generated ID.
    suspend fun addSubject(userId: Int, name: String, color: String?) : Long {
        return db.subjectDao().insert(Subject(userId = userId, name = name, color = color))
    }

    // Get all subjects that belong to a specific user.
    suspend fun getSubjects(userId: Int) = db.subjectDao().getSubjects(userId)

    // Get all subjects together with their assignments for one user.
    suspend fun getSubjectsWithAssignments(userId: Int): List<SubjectWithAssignments> {
        return db.subjectDao().getSubjectsWithAssignments(userId)
    }

    // Find a subject by name for one user, or return null if it does not exist.
    suspend fun getSubjectByName(userId: Int, name: String): Subject? {
        return db.subjectDao().getByName(userId, name)
    }

    // Remove a subject from the database.
    suspend fun deleteSubject(subject: Subject) = db.subjectDao().delete(subject)
}
