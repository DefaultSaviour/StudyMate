package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Subject

class SubjectRepo(private val db: StudyMateDatabase) {

    suspend fun addSubject(userId: Int, name: String, color: String?) : Long {
        return db.subjectDao().insert(Subject(userId = userId, name = name, color = color))
        // why does this keep thinking its unit?????????
    }

    suspend fun getSubjects(userId: Int) = db.subjectDao().getSubjects(userId)

    suspend fun deleteSubject(subject: Subject) = db.subjectDao().delete(subject)
}
