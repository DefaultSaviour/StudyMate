package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.SubjectProgress

class SubjectProgressRepo(private val db: StudyMateDatabase) {

    suspend fun addProgress(progress: SubjectProgress) = db.subjectProgressDao().insert(progress)

    suspend fun getProgressForUser(userId: Int) = db.subjectProgressDao().getAll(userId)
}

//why didnt i add remove?? reread over this and check to be safe

// check notes, I need to make sure the naming scheme is consistent