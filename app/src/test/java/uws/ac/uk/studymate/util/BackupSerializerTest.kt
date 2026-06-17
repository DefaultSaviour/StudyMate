package uws.ac.uk.studymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import uws.ac.uk.studymate.util.BackupSerializer.AssignmentNode
import uws.ac.uk.studymate.util.BackupSerializer.BackupData
import uws.ac.uk.studymate.util.BackupSerializer.CardNode
import uws.ac.uk.studymate.util.BackupSerializer.DeckNode
import uws.ac.uk.studymate.util.BackupSerializer.SubjectNode

/*//////////////////////
Coded by Jamie Coleman
 17/06/26
 *//////////////////////
// Unit tests for the backup JSON format. Pure logic, no Android/Room.
class BackupSerializerTest {

    private fun sample() = BackupData(
        subjects = listOf(
            SubjectNode(
                name = "Biology",
                color = "#C4A24A",
                assignments = listOf(
                    AssignmentNode("Essay", "2026-07-01T09:00", "assignment", null),
                    AssignmentNode("Lab report", null, "science", "2026-06-10T12:00:00Z")
                ),
                decks = listOf(
                    DeckNode(
                        name = "Cell Structure",
                        cards = listOf(
                            CardNode("What is mitosis?", "Cell division", 2.5, 0, 0, null, null),
                            CardNode("Mitochondria?", "Powerhouse", 2.6, 6, 2, "2026-07-10", "2026-06-17T08:00:00Z")
                        )
                    )
                )
            ),
            SubjectNode("History", null, emptyList(), emptyList())
        )
    )

    @Test
    fun roundTrip_preservesEverything() {
        val original = sample()
        val json = BackupSerializer.toJson(original, "2026-06-17T12:00:00Z")
        val parsed = BackupSerializer.fromJson(json)
        assertEquals(original, parsed)
    }

    @Test
    fun roundTrip_preservesSchedulingFields() {
        val parsed = BackupSerializer.fromJson(
            BackupSerializer.toJson(sample(), "2026-06-17T12:00:00Z")
        )
        val card = parsed.subjects[0].decks[0].cards[1]
        assertEquals(2.6, card.easeFactor, 0.0001)
        assertEquals(6, card.intervalDays)
        assertEquals(2, card.repetitions)
        assertEquals("2026-07-10", card.dueAt)
        assertEquals("2026-06-17T08:00:00Z", card.lastReviewedAt)
    }

    @Test
    fun fromJson_rejectsWrongFormat() {
        val json = """{"format":"something-else","version":1,"subjects":[]}"""
        assertThrows(BackupSerializer.InvalidBackupException::class.java) {
            BackupSerializer.fromJson(json)
        }
    }

    @Test
    fun fromJson_rejectsFutureVersion() {
        val json = """{"format":"studymate-backup","version":999,"subjects":[]}"""
        assertThrows(BackupSerializer.InvalidBackupException::class.java) {
            BackupSerializer.fromJson(json)
        }
    }

    @Test
    fun fromJson_rejectsMalformedJson() {
        assertThrows(BackupSerializer.InvalidBackupException::class.java) {
            BackupSerializer.fromJson("not json at all {{{")
        }
    }

    @Test
    fun fromJson_rejectsMissingSubjects() {
        val json = """{"format":"studymate-backup","version":1}"""
        assertThrows(BackupSerializer.InvalidBackupException::class.java) {
            BackupSerializer.fromJson(json)
        }
    }

    @Test
    fun fromJson_toleratesMissingOptionalFields() {
        // Minimal: a subject with just a name; a card with just front/back.
        val json = """
            {
              "format": "studymate-backup",
              "version": 1,
              "subjects": [
                { "name": "Math",
                  "decks": [ { "name": "Algebra", "cards": [ { "front": "2+2", "back": "4" } ] } ] }
              ]
            }
        """.trimIndent()
        val parsed = BackupSerializer.fromJson(json)
        val subject = parsed.subjects.single()
        assertEquals("Math", subject.name)
        assertNull(subject.color)
        assertTrue(subject.assignments.isEmpty())
        val card = subject.decks.single().cards.single()
        assertEquals("2+2", card.front)
        assertEquals(2.5, card.easeFactor, 0.0001)   // default
        assertEquals(0, card.intervalDays)            // default
        assertNull(card.dueAt)
    }

    @Test
    fun fromJson_skipsNamelessSubjectsAndEmptyCards() {
        val json = """
            {
              "format": "studymate-backup",
              "version": 1,
              "subjects": [
                { "name": "  " },
                { "name": "Chem",
                  "decks": [ { "name": "Acids", "cards": [
                    { "front": "", "back": "" },
                    { "front": "pH of water?", "back": "7" }
                  ] } ] }
              ]
            }
        """.trimIndent()
        val parsed = BackupSerializer.fromJson(json)
        assertEquals(1, parsed.subjects.size)
        assertEquals("Chem", parsed.subjects[0].name)
        assertEquals(1, parsed.subjects[0].decks[0].cards.size)  // empty card skipped
    }
}
