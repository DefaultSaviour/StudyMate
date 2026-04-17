package uws.ac.uk.studymate.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.entities.SubjectProgress
/*//////////////////////
Coded by Jamie Coleman
 24/03/26
 *//////////////////////
// Joins a subject with its progress record.
// Room fills in the progress automatically using the relationship.
data class SubjectWithProgress(
    @Embedded val subject: Subject,                                  // The subject itself.
    @Relation(parentColumn = "id", entityColumn = "subject_id")
    val progress: SubjectProgress?                                   // The progress for this subject, or null if none exists.
)

