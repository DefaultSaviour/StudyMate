package uws.ac.uk.studymate.data.repositories

import androidx.room.withTransaction
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.AssignmentTask
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import java.time.LocalDateTime

/*//////////////////////
First-run content (0.9E).
Seeds a single "Getting Started" assignment with one tutorial deck under a freshly
created account, so the app is never empty on day one and the dashboard's
"Review due decks" loop has something live to demo. Called once per new account
from RegisterViewModel (every new account, not just the first on the device).

Pure DB insert — mirrors the insert pattern in BackupRepo: one transaction,
assignment -> deck -> cards, all foreign keys stamped with the new user/parent.
Cards are left at due_at = null so they count as due immediately.
 *//////////////////////
class SampleContentSeeder(private val db: StudyMateDatabase) {

    // Insert the sample study tree under [userId]. Additive and transactional.
    suspend fun seed(userId: Int) {
        db.withTransaction {
            val assignmentId = db.assignmentDao().insert(
                Assignment(
                    userId = userId,
                    title = ASSIGNMENT_TITLE,
                    color = ASSIGNMENT_COLOR,
                    // Due 1 hour out (ISO_LOCAL_DATE_TIME — the first format
                    // AssignmentDateTimeUtils.parseDueDate accepts). Deliberately short so the
                    // demo assignment can never trigger reminders (T-7d/T-1d/day-of all fall in
                    // the past) alongside the user's real study notifications. Finishing
                    // onboarding marks it complete (OnboardingViewModel.completeSampleAssignment),
                    // so the +1h is now just a safety net: if the process is killed mid-onboarding
                    // it still auto-completes once the hour passes (a past-due assignment counts
                    // as done — there is no "overdue" state).
                    dueDate = LocalDateTime.now().plusHours(1).toString(),
                    icon = ASSIGNMENT_ICON,
                    completedAt = null
                )
            ).toInt()

            val deckId = db.deckDao().insert(
                FlashcardDeck(userId = userId, assignmentId = assignmentId, name = DECK_NAME)
            ).toInt()

            for ((front, back) in SAMPLE_CARDS) {
                db.cardDao().insert(
                    FlashCard(
                        userId = userId,
                        deckId = deckId,
                        front = front,
                        back = back
                        // ease/interval/repetitions default; dueAt = null => due now (new card).
                    )
                )
            }

            // A short checklist (0.9J) so the assignment-checklist + focus-timer
            // feature is demoed on day one. Deletable like everything else.
            val now = LocalDateTime.now().toString()
            SAMPLE_TASKS.forEachIndexed { index, text ->
                db.assignmentTaskDao().insert(
                    AssignmentTask(
                        userId = userId,
                        assignmentId = assignmentId,
                        text = text,
                        position = index,
                        createdAt = now
                    )
                )
            }
        }
    }

    companion object {
        const val ASSIGNMENT_TITLE = "Getting Started"
        const val DECK_NAME = "How StudyMate works"

        // A friendly blue so it stands out from the gold UI; any hex is fine
        // (ColorUtils.parseOrDefault tolerates it). "english" is a valid AssignmentIcons key.
        private const val ASSIGNMENT_COLOR = "#5B8DEF"
        private const val ASSIGNMENT_ICON = "english"

        // Question / answer pairs. These teach the app and double as a working
        // review-loop demo. Kept here as seed data (not chrome); lift to strings
        // if the app is ever localised.
        val SAMPLE_CARDS: List<Pair<String, String>> = listOf(
            "Welcome! What is StudyMate?" to
                "Your private, offline study companion — track assignments and revise with spaced-repetition flashcards. Nothing leaves your phone.",
            "What is spaced repetition?" to
                "Reviewing each card just before you'd forget it, at growing intervals. StudyMate uses the proven SM-2 method.",
            "After you flip a card, what do the buttons do?" to
                "Again = see it again now • Wrong = reset, due tomorrow • Correct = pushes it further out.",
            "How do I add my own study material?" to
                "Open Assignments to make a subject (name, colour, icon, due date), then add flashcard decks and cards under it.",
            "Will I get reminders?" to
                "Yes — per-assignment reminders and a daily 'cards due' nudge, all on-device. Turn them on in Settings.",
            "Done with this demo deck?" to
                "Delete it any time from the Flashcards screen, then start building your own. Good luck!"
        )

        // A short demo checklist (0.9J): tap an assignment to open it, or pick the
        // assignment in the focus timer to tick these off while you study.
        val SAMPLE_TASKS: List<String> = listOf(
            "Open this assignment to see its checklist",
            "Pick this assignment in the focus timer",
            "Tick an item off while you study"
        )
    }
}
