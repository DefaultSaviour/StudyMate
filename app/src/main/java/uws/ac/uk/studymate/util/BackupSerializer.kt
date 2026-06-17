package uws.ac.uk.studymate.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/*//////////////////////
Coded by Jamie Coleman
 17/06/26
 *//////////////////////
// Converts a user's study data to/from the StudyMate backup JSON format.
//
// The format is a NESTED tree (subjects -> assignments + decks -> cards). It
// deliberately carries NO database IDs: primary keys are auto-generated and
// device-local, so they are meaningless on another device. Relationships are
// expressed purely by nesting, then re-stamped with the importing user's id and
// freshly generated parent ids when the backup is restored (see BackupRepo).
//
// Pure logic over plain data classes (no Room / Android), so it is unit-testable.
// Uses org.json, which ships with Android — no extra runtime dependency.
object BackupSerializer {

    const val FORMAT = "studymate-backup"
    const val VERSION = 1

    // ── Plain DTOs mirroring the nested backup format ──
    data class BackupData(val subjects: List<SubjectNode>)

    data class SubjectNode(
        val name: String,
        val color: String?,
        val assignments: List<AssignmentNode>,
        val decks: List<DeckNode>
    )

    data class AssignmentNode(
        val title: String,
        val dueDate: String?,
        val icon: String,
        val completedAt: String?
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

        val subjects = JSONArray()
        for (s in data.subjects) {
            val so = JSONObject()
            so.put("name", s.name)
            so.put("color", s.color ?: JSONObject.NULL)

            val assignments = JSONArray()
            for (a in s.assignments) {
                val ao = JSONObject()
                ao.put("title", a.title)
                ao.put("dueDate", a.dueDate ?: JSONObject.NULL)
                ao.put("icon", a.icon)
                ao.put("completedAt", a.completedAt ?: JSONObject.NULL)
                assignments.put(ao)
            }
            so.put("assignments", assignments)

            val decks = JSONArray()
            for (d in s.decks) {
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
            so.put("decks", decks)

            subjects.put(so)
        }
        root.put("subjects", subjects)
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
        if (version < 1 || version > VERSION) {
            throw InvalidBackupException("This backup was made by a newer version of StudyMate.")
        }
        if (!root.has("subjects")) {
            throw InvalidBackupException("This backup is missing its data.")
        }

        val subjectsJson = root.optJSONArray("subjects") ?: JSONArray()
        val subjects = ArrayList<SubjectNode>(subjectsJson.length())
        for (i in 0 until subjectsJson.length()) {
            val so = subjectsJson.getJSONObject(i)
            val name = so.optString("name").trim()
            if (name.isEmpty()) continue   // skip nameless subjects rather than fail the whole import
            subjects.add(
                SubjectNode(
                    name = name,
                    color = so.optStringOrNull("color"),
                    assignments = parseAssignments(so.optJSONArray("assignments")),
                    decks = parseDecks(so.optJSONArray("decks"))
                )
            )
        }
        return BackupData(subjects)
    }

    private fun parseAssignments(arr: JSONArray?): List<AssignmentNode> {
        if (arr == null) return emptyList()
        val out = ArrayList<AssignmentNode>(arr.length())
        for (i in 0 until arr.length()) {
            val ao = arr.getJSONObject(i)
            val title = ao.optString("title").trim()
            if (title.isEmpty()) continue
            out.add(
                AssignmentNode(
                    title = title,
                    dueDate = ao.optStringOrNull("dueDate"),
                    icon = ao.optString("icon", "assignment").ifBlank { "assignment" },
                    completedAt = ao.optStringOrNull("completedAt")
                )
            )
        }
        return out
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

    // Returns the string value for a key, or null if absent / JSON null / blank.
    private fun JSONObject.optStringOrNull(key: String): String? {
        if (isNull(key) || !has(key)) return null
        val v = optString(key)
        return v.ifBlank { null }
    }
}
