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
import uws.ac.uk.studymate.util.BackupSerializer.TaskNode

/*//////////////////////
Coded by Jamie Coleman
 17/06/26
 *//////////////////////
// Unit tests for the backup JSON format (v2, flat: assignments -> decks -> cards).
// Pure logic, no Android/Room.
class BackupSerializerTest {

    private fun sample() = BackupData(
        assignments = listOf(
            AssignmentNode(
                title = "Biology - Cells",
                color = "#C4A24A",
                dueDate = "2026-07-01T09:00",
                icon = "science",
                completedAt = null,
                decks = listOf(
                    DeckNode(
                        name = "Cell Structure",
                        cards = listOf(
                            CardNode("What is mitosis?", "Cell division", 2.5, 0, 0, null, null),
                            CardNode("Mitochondria?", "Powerhouse", 2.6, 6, 2, "2026-07-10", "2026-06-17T08:00:00Z")
                        )
                    )
                ),
                tasks = listOf(
                    TaskNode("Read chapter 4", isDone = true, position = 0),
                    TaskNode("Practice problems", isDone = false, position = 1)
                )
            ),
            AssignmentNode(
                title = "History essay",
                color = null,
                dueDate = "2026-07-02T10:00",
                icon = "assignment",
                completedAt = "2026-06-10T12:00:00Z",
                decks = emptyList()
            )
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
        val card = parsed.assignments[0].decks[0].cards[1]
        assertEquals(2.6, card.easeFactor, 0.0001)
        assertEquals(6, card.intervalDays)
        assertEquals(2, card.repetitions)
        assertEquals("2026-07-10", card.dueAt)
        assertEquals("2026-06-17T08:00:00Z", card.lastReviewedAt)
    }

    @Test
    fun roundTrip_preservesAssignmentFields() {
        val parsed = BackupSerializer.fromJson(
            BackupSerializer.toJson(sample(), "2026-06-17T12:00:00Z")
        )
        assertEquals("#C4A24A", parsed.assignments[0].color)
        assertEquals("2026-07-01T09:00", parsed.assignments[0].dueDate)
        assertEquals("science", parsed.assignments[0].icon)
        assertEquals("2026-06-10T12:00:00Z", parsed.assignments[1].completedAt)
        assertNull(parsed.assignments[1].color)
    }

    @Test
    fun roundTrip_preservesChecklistTasks() {
        val parsed = BackupSerializer.fromJson(
            BackupSerializer.toJson(sample(), "2026-06-17T12:00:00Z")
        )
        val tasks = parsed.assignments[0].tasks
        assertEquals(2, tasks.size)
        assertEquals("Read chapter 4", tasks[0].text)
        assertTrue(tasks[0].isDone)
        assertEquals(1, tasks[1].position)
        // An assignment with no tasks round-trips as an empty list, not null.
        assertTrue(parsed.assignments[1].tasks.isEmpty())
    }

    @Test
    fun fromJson_acceptsV2BackupWithNoTasks() {
        // A v2 backup (pre-checklist) must still import — its assignments just have
        // no tasks.
        val json = """
            {
              "format": "studymate-backup",
              "version": 2,
              "assignments": [
                { "title": "Math", "dueDate": "2026-07-01T09:00",
                  "decks": [ { "name": "Algebra", "cards": [ { "front": "2+2", "back": "4" } ] } ] }
              ]
            }
        """.trimIndent()
        val parsed = BackupSerializer.fromJson(json)
        assertEquals("Math", parsed.assignments.single().title)
        assertTrue(parsed.assignments.single().tasks.isEmpty())
    }

    @Test
    fun fromJson_rejectsWrongFormat() {
        val json = """{"format":"something-else","version":2,"assignments":[]}"""
        assertThrows(BackupSerializer.InvalidBackupException::class.java) {
            BackupSerializer.fromJson(json)
        }
    }

    @Test
    fun fromJson_rejectsFutureVersion() {
        val json = """{"format":"studymate-backup","version":999,"assignments":[]}"""
        assertThrows(BackupSerializer.InvalidBackupException::class.java) {
            BackupSerializer.fromJson(json)
        }
    }

    @Test
    fun fromJson_rejectsOlderIncompatibleVersion() {
        // v1 (nested subjects) can't map into the flat model — must be rejected.
        val json = """{"format":"studymate-backup","version":1,"subjects":[]}"""
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
    fun fromJson_rejectsMissingAssignments() {
        val json = """{"format":"studymate-backup","version":2}"""
        assertThrows(BackupSerializer.InvalidBackupException::class.java) {
            BackupSerializer.fromJson(json)
        }
    }

    @Test
    fun fromJson_toleratesMissingOptionalFields() {
        // Minimal: an assignment with just a title; a card with just front/back.
        val json = """
            {
              "format": "studymate-backup",
              "version": 2,
              "assignments": [
                { "title": "Math",
                  "decks": [ { "name": "Algebra", "cards": [ { "front": "2+2", "back": "4" } ] } ] }
              ]
            }
        """.trimIndent()
        val parsed = BackupSerializer.fromJson(json)
        val assignment = parsed.assignments.single()
        assertEquals("Math", assignment.title)
        assertNull(assignment.color)
        assertNull(assignment.dueDate)
        assertEquals("assignment", assignment.icon)   // default
        val card = assignment.decks.single().cards.single()
        assertEquals("2+2", card.front)
        assertEquals(2.5, card.easeFactor, 0.0001)   // default
        assertEquals(0, card.intervalDays)            // default
        assertNull(card.dueAt)
    }

    @Test
    fun fromJson_skipsNamelessAssignmentsAndEmptyCards() {
        val json = """
            {
              "format": "studymate-backup",
              "version": 2,
              "assignments": [
                { "title": "  " },
                { "title": "Chem",
                  "decks": [ { "name": "Acids", "cards": [
                    { "front": "", "back": "" },
                    { "front": "pH of water?", "back": "7" }
                  ] } ] }
              ]
            }
        """.trimIndent()
        val parsed = BackupSerializer.fromJson(json)
        assertEquals(1, parsed.assignments.size)
        assertEquals("Chem", parsed.assignments[0].title)
        assertEquals(1, parsed.assignments[0].decks[0].cards.size)  // empty card skipped
        assertTrue(parsed.assignments[0].decks[0].cards[0].front == "pH of water?")
    }
}
