package uws.ac.uk.studymate.data.repositories

import androidx.room.withTransaction
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.util.BackupSerializer.AssignmentNode
import uws.ac.uk.studymate.util.BackupSerializer.BackupData
import uws.ac.uk.studymate.util.BackupSerializer.CardNode
import uws.ac.uk.studymate.util.BackupSerializer.DeckNode
import uws.ac.uk.studymate.util.BackupSerializer.SubjectNode

/*//////////////////////
Coded by Jamie Coleman
 17/06/26
 *//////////////////////
// Reads a user's whole study tree into backup DTOs, and restores a backup back
// into the database under a given user. All foreign keys are re-stamped on import
// (the DTOs carry no ids) so a backup made on one device/account lands cleanly
// under whoever is currently logged in. See util/BackupSerializer for the format.
class BackupRepo(private val db: StudyMateDatabase) {

    // Counts of what was added during an import, for the success message.
    data class ImportSummary(
        val subjects: Int,
        val assignments: Int,
        val decks: Int,
        val cards: Int
    )

    // Read everything owned by [userId] into the nested backup structure.
    suspend fun export(userId: Int): BackupData {
        val subjects = db.subjectDao().getSubjects(userId).map { subject ->
            val assignments = db.assignmentDao().getAssignmentsForSubject(subject.id).map { a ->
                AssignmentNode(
                    title = a.title,
                    dueDate = a.dueDate,
                    icon = a.icon,
                    completedAt = a.completedAt
                )
            }
            val decks = db.deckDao().getDecksForSubject(subject.id).map { deck ->
                val cards = db.cardDao().getCards(deck.id).map { c ->
                    CardNode(
                        front = c.front,
                        back = c.back,
                        easeFactor = c.easeFactor,
                        intervalDays = c.intervalDays,
                        repetitions = c.repetitions,
                        dueAt = c.dueAt,
                        lastReviewedAt = c.lastReviewedAt
                    )
                }
                DeckNode(name = deck.name, cards = cards)
            }
            SubjectNode(
                name = subject.name,
                color = subject.color,
                assignments = assignments,
                decks = decks
            )
        }
        return BackupData(subjects)
    }

    // Insert [data] under [userId]. Additive (never deletes existing data) and
    // wrapped in a single transaction so a malformed file can't half-import.
    // Subjects merge by name (case-insensitive); decks/assignments/cards are
    // always created under the resolved subject.
    suspend fun import(userId: Int, data: BackupData): ImportSummary {
        var newSubjects = 0
        var newAssignments = 0
        var newDecks = 0
        var newCards = 0

        db.withTransaction {
            for (s: SubjectNode in data.subjects) {
                // Reuse an existing subject of the same name, else create it.
                val existing = db.subjectDao().getByName(userId, s.name)
                val subjectId: Int = if (existing != null) {
                    existing.id
                } else {
                    newSubjects++
                    db.subjectDao().insert(
                        Subject(userId = userId, name = s.name, color = s.color)
                    ).toInt()
                }

                for (a: AssignmentNode in s.assignments) {
                    db.assignmentDao().insert(
                        Assignment(
                            userId = userId,
                            subjectId = subjectId,
                            title = a.title,
                            dueDate = a.dueDate,
                            icon = a.icon,
                            completedAt = a.completedAt
                        )
                    )
                    newAssignments++
                }

                for (d: DeckNode in s.decks) {
                    val deckId = db.deckDao().insert(
                        FlashcardDeck(userId = userId, subjectId = subjectId, name = d.name)
                    ).toInt()
                    newDecks++

                    for (c: CardNode in d.cards) {
                        db.cardDao().insert(
                            FlashCard(
                                userId = userId,
                                deckId = deckId,
                                front = c.front,
                                back = c.back,
                                easeFactor = c.easeFactor,
                                intervalDays = c.intervalDays,
                                repetitions = c.repetitions,
                                dueAt = c.dueAt,
                                lastReviewedAt = c.lastReviewedAt
                            )
                        )
                        newCards++
                    }
                }
            }
        }

        return ImportSummary(newSubjects, newAssignments, newDecks, newCards)
    }
}
