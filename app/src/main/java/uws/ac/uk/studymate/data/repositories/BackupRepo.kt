package uws.ac.uk.studymate.data.repositories

import androidx.room.withTransaction
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.util.BackupSerializer.AssignmentNode
import uws.ac.uk.studymate.util.BackupSerializer.BackupData
import uws.ac.uk.studymate.util.BackupSerializer.CardNode
import uws.ac.uk.studymate.util.BackupSerializer.DeckNode

/*//////////////////////
Coded by Jamie Coleman
 17/06/26
 v2 17/06/26 — flat model (Subject merged into Assignment)
 *//////////////////////
// Reads a user's whole study tree into backup DTOs, and restores a backup back
// into the database under a given user. All foreign keys are re-stamped on import
// (the DTOs carry no ids) so a backup made on one device/account lands cleanly
// under whoever is currently logged in. See util/BackupSerializer for the format.
class BackupRepo(private val db: StudyMateDatabase) {

    // Counts of what was added during an import, for the success message.
    data class ImportSummary(
        val assignments: Int,
        val decks: Int,
        val cards: Int
    )

    // Read everything owned by [userId] into the nested backup structure.
    suspend fun export(userId: Int): BackupData {
        val assignments = db.assignmentDao().getAssignments(userId).map { assignment ->
            val decks = db.deckDao().getDecksForAssignment(assignment.id).map { deck ->
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
            AssignmentNode(
                title = assignment.title,
                color = assignment.color,
                dueDate = assignment.dueDate,
                icon = assignment.icon,
                completedAt = assignment.completedAt,
                decks = decks
            )
        }
        return BackupData(assignments)
    }

    // Insert [data] under [userId]. Additive (never deletes existing data) and
    // wrapped in a single transaction so a malformed file can't half-import.
    // Assignments merge by name (case-insensitive); decks/cards are always
    // created under the resolved assignment.
    suspend fun import(userId: Int, data: BackupData): ImportSummary {
        var newAssignments = 0
        var newDecks = 0
        var newCards = 0

        db.withTransaction {
            for (a: AssignmentNode in data.assignments) {
                // Reuse an existing assignment of the same name, else create it.
                val existing = db.assignmentDao().getByName(userId, a.title)
                val assignmentId: Int = if (existing != null) {
                    existing.id
                } else {
                    newAssignments++
                    db.assignmentDao().insert(
                        Assignment(
                            userId = userId,
                            title = a.title,
                            color = a.color,
                            dueDate = a.dueDate,
                            icon = a.icon,
                            completedAt = a.completedAt
                        )
                    ).toInt()
                }

                for (d: DeckNode in a.decks) {
                    val deckId = db.deckDao().insert(
                        FlashcardDeck(userId = userId, assignmentId = assignmentId, name = d.name)
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

        return ImportSummary(newAssignments, newDecks, newCards)
    }
}
