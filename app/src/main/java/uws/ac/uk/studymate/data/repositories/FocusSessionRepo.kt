package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FocusSession
/*//////////////////////
Logged focus blocks (0.9J) — powers the focus statistics. Derived history;
excluded from backups (like Review_Logs).
 *//////////////////////
class FocusSessionRepo(private val db: StudyMateDatabase) {

    // Log one completed focus block (focused seconds only, breaks excluded).
    // A zero/empty block is ignored so nothing pollutes the totals.
    suspend fun log(userId: Int, assignmentId: Int?, focusedSeconds: Int, endedAt: String) {
        if (focusedSeconds <= 0) return
        db.focusSessionDao().insert(
            FocusSession(
                userId = userId,
                assignmentId = assignmentId,
                focusedSeconds = focusedSeconds,
                endedAt = endedAt
            )
        )
    }

    // Total focused seconds since the given ISO instant (today / this week).
    suspend fun focusedSecondsSince(userId: Int, sinceIso: String) =
        db.focusSessionDao().sumFocusedSecondsSince(userId, sinceIso)
}
