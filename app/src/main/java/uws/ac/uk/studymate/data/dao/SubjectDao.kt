package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.relations.SubjectWithAssignments
import uws.ac.uk.studymate.data.relations.SubjectWithDecks
import uws.ac.uk.studymate.data.relations.SubjectWithProgress
/*//////////////////////
Coded by Jamie Coleman
 10/03/26
 updated 18/03/26
 updated 05/04/26
 *//////////////////////
// Provides database operations for the Subjects table.
@Dao
interface SubjectDao {
    // Save a new subject and return its generated ID.
    @Insert suspend fun insert(subject: Subject): Long
    // Update an existing subject's details.
    @Update suspend fun update(subject: Subject)
    // Remove a subject from the database.
    @Delete suspend fun delete(subject: Subject)

    // Get all subjects that belong to a specific user.
    @Query("SELECT * FROM Subjects WHERE user_id = :userId")
    suspend fun getSubjects(userId: Int): List<Subject>

    // Get each subject together with its progress record for a user.
    @Transaction
    @Query("SELECT * FROM Subjects WHERE user_id = :userId")
    suspend fun getSubjectsWithProgress(userId: Int): List<SubjectWithProgress>

    // Get each subject together with all its assignments for a user.
    @Transaction
    @Query("SELECT * FROM Subjects WHERE user_id = :userId")
    suspend fun getSubjectsWithAssignments(userId: Int): List<SubjectWithAssignments>

    // Get each subject together with all its flashcard decks for a user.
    @Transaction
    @Query("SELECT * FROM Subjects WHERE user_id = :userId")
    suspend fun getSubjectsWithDecks(userId: Int): List<SubjectWithDecks>

    // Find one subject by name for a user, ignoring letter case.
    @Query("SELECT * FROM Subjects WHERE user_id = :userId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(userId: Int, name: String): Subject?
}
