package uws.ac.uk.studymate.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.Subject
/*//////////////////////
Coded by Jamie Coleman
 24/03/26
  updated 7/04/26
 *//////////////////////
// Joins a subject with all the assignments that belong to it.
// Room fills in the assignments list automatically using the relationship.
data class SubjectWithAssignments(
    @Embedded val subject: Subject,                                    // The subject itself.
    @Relation(parentColumn = "id", entityColumn = "subject_id")
    val assignments: List<Assignment>                                  // All assignments under this subject.
)

