package uws.ac.uk.studymate.data.repositories

import androidx.room.Embedded
import androidx.room.Relation
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.entities.SubjectProgress

data class SubjectWithProgress(
    @Embedded val subject: Subject,
    @Relation(parentColumn = "id", entityColumn = "subject_id")
    val progress: SubjectProgress?
)


// comment in exactly how this works!!