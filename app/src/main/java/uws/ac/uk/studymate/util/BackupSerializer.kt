package uws.ac.uk.studymate.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/*//////////////////////
Coded by Jamie Coleman
 17/06/26
 v2 17/06/26 — Subject merged into Assignment, so the format is now flat:
               assignments[] -> decks[] -> cards[] (no subjects layer).
 *//////////////////////
// Converts a user's study data to/from the StudyMate backup JSON format.
//
// The format is a NESTED tree (assignments -> decks -> cards). It deliberately
// carries NO database IDs: primary keys are auto-generated and device-local, so
// they are meaningless on another device. Relationships are expressed purely by
// nesting, then re-stamped with the importing user's id and freshly generated
// parent ids when the backup is restored (see BackupRepo).
//
// Pure logic over plain data classes (no Room / Android), so it is unit-testable.
// Uses org.json, which ships with Android — no extra runtime dependency.
object BackupSerializer {

    const val FORMAT = "studymate-backup"
    // v3 (0.9J) adds a checklist (`tasks`) under each assignment. v2 backups still
    // import (their assignments simply have no tasks).
    const val VERSION = 3
    private const val MIN_SUPPORTED_VERSION = 2

    // ── Plain DTOs mirroring the nested backup format ──
    data class BackupData(val assignments: List<AssignmentNode>)

    data class AssignmentNode(
        val title: String,
        val color: String?,
        val dueDate: String?,
        val icon: String,
        val completedAt: String?,
        val decks: List<DeckNode>,
        val tasks: List<TaskNode> = emptyList()
    )

    data class TaskNode(
        val text: String,
        val isDone: Boolean,
        val position: Int
    )

    data class DeckNode(
        val name: String,
        val cards: List<CardNode>
    )

    data class CardNode(
        val front: String,
        val back: String,
        val easeFactor: Double,
        val intervalDays: Int,
        val repetitions: Int,
        val dueAt: String?,
        val lastReviewedAt: String?
    )

    // Thrown when a file isn't a StudyMate backup we can read.
    class InvalidBackupException(message: String) : Exception(message)

    // ── Serialize ──
    fun toJson(data: BackupData, exportedAt: String): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", exportedAt)

        val assignments = JSONArray()
        for (a in data.assignments) {
            val ao = JSONObject()
            ao.put("title", a.title)
            ao.put("color", a.color ?: JSONObject.NULL)
            ao.put("dueDate", a.dueDate ?: JSONObject.NULL)
            ao.put("icon", a.icon)
            ao.put("completedAt", a.completedAt ?: JSONObject.NULL)

            val decks = JSONArray()
            for (d in a.decks) {
                val deckObj = JSONObject()
                deckObj.put("name", d.name)
                val cards = JSONArray()
                for (c in d.cards) {
                    val co = JSONObject()
                    co.put("front", c.front)
                    co.put("back", c.back)
                    co.put("easeFactor", c.easeFactor)
                    co.put("intervalDays", c.intervalDays)
                    co.put("repetitions", c.repetitions)
                    co.put("dueAt", c.dueAt ?: JSONObject.NULL)
                    co.put("lastReviewedAt", c.lastReviewedAt ?: JSONObject.NULL)
                    cards.put(co)
                }
                deckObj.put("cards", cards)
                decks.put(deckObj)
            }
            ao.put("decks", decks)

            val tasks = JSONArray()
            for (t in a.tasks) {
                val to = JSONObject()
                to.put("text", t.text)
                to.put("isDone", t.isDone)
                to.put("position", t.position)
                tasks.put(to)
            }
            ao.put("tasks", tasks)

            assignments.put(ao)
        }
        root.put("assignments", assignments)
        return root.toString(2)
    }

    // ── Parse ──
    fun fromJson(raw: String): BackupData {
        val root = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            throw InvalidBackupException("That file isn't valid JSON.")
        }

        if (root.optString("format") != FORMAT) {
            throw InvalidBackupException("That file isn't a StudyMate backup.")
        }
        val version = root.optInt("version", -1)
        if (version < 1) {
            throw InvalidBackupException("This backup is missing its version.")
        }
        if (version > VERSION) {
            throw InvalidBackupException("This backup was made by a newer version of StudyMate.")
        }
        if (version < MIN_SUPPORTED_VERSION) {
            // v1 nested subjects above assignments and had no due date on the top
            // level — there is no clean mapping into the new flat model. v2 and v3
            // both import (v2 just has no checklist tasks).
            throw InvalidBackupException("This backup was made by an older, incompatible version of StudyMate.")
        }
        if (!root.has("assignments")) {
            throw InvalidBackupException("This backup is missing its data.")
        }

        val assignmentsJson = root.optJSONArray("assignments") ?: JSONArray()
        val assignments = ArrayList<AssignmentNode>(assignmentsJson.length())
        for (i in 0 until assignmentsJson.length()) {
            val ao = assignmentsJson.getJSONObject(i)
            val title = ao.optString("title").trim()
            if (title.isEmpty()) continue   // skip nameless assignments rather than fail the whole import
            assignments.add(
                AssignmentNode(
                    title = title,
                    color = ao.optStringOrNull("color"),
                    dueDate = ao.optStringOrNull("dueDate"),
                    icon = ao.optString("icon", "assignment").ifBlank { "assignment" },
                    completedAt = ao.optStringOrNull("completedAt"),
                    decks = parseDecks(ao.optJSONArray("decks")),
                    tasks = parseTasks(ao.optJSONArray("tasks"))
                )
            )
        }
        return BackupData(assignments)
    }

    private fun parseDecks(arr: JSONArray?): List<DeckNode> {
        if (arr == null) return emptyList()
        val out = ArrayList<DeckNode>(arr.length())
        for (i in 0 until arr.length()) {
            val deckObj = arr.getJSONObject(i)
            val name = deckObj.optString("name").trim()
            if (name.isEmpty()) continue
            out.add(DeckNode(name = name, cards = parseCards(deckObj.optJSONArray("cards"))))
        }
        return out
    }

    private fun parseCards(arr: JSONArray?): List<CardNode> {
        if (arr == null) return emptyList()
        val out = ArrayList<CardNode>(arr.length())
        for (i in 0 until arr.length()) {
            val co = arr.getJSONObject(i)
            val front = co.optString("front")
            val back = co.optString("back")
            // A card with neither side is junk; skip it.
            if (front.isBlank() && back.isBlank()) continue
            out.add(
                CardNode(
                    front = front,
                    back = back,
                    easeFactor = co.optDouble("easeFactor", 2.5),
                    intervalDays = co.optInt("intervalDays", 0),
                    repetitions = co.optInt("repetitions", 0),
                    dueAt = co.optStringOrNull("dueAt"),
                    lastReviewedAt = co.optStringOrNull("lastReviewedAt")
                )
            )
        }
        return out
    }

    private fun parseTasks(arr: JSONArray?): List<TaskNode> {
        if (arr == null) return emptyList()
        val out = ArrayList<TaskNode>(arr.length())
        for (i in 0 until arr.length()) {
            val to = arr.getJSONObject(i)
            val text = to.optString("text").trim()
            if (text.isEmpty()) continue   // skip blank items rather than fail the import
            out.add(
                TaskNode(
                    text = text,
                    isDone = to.optBoolean("isDone", false),
                    position = to.optInt("position", i)
                )
            )
        }
        return out
    }

    // Returns the string value for a key, or null if absent / JSON null / blank.
    private fun JSONObject.optStringOrNull(key: String): String? {
        if (isNull(key) || !has(key)) return null
        val v = optString(key)
        return v.ifBlank { null }
    }
}
