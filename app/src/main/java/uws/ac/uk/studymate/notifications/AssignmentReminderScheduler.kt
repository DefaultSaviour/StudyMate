package uws.ac.uk.studymate.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/*//////////////////////
Schedules / cancels the assignment-reminder WorkManager jobs.

Each assignment gets up to three jobs (week-out, day-out, day-of). Jobs that
would fire in the past are skipped. All jobs for an assignment share the tag
"assignment_<id>" so cancelling deletes the whole set. They also carry a
"user_<id>" tag so the toggle-off flow can wipe everyone for a user at once.

Idempotent: if you schedule again for the same assignment, the previous
WorkManager records are replaced (ExistingWorkPolicy.REPLACE) so we never
accumulate stale entries when the user edits a due date.
 *//////////////////////
object AssignmentReminderScheduler {

    /**
     * Schedule (or re-schedule) all relevant reminder jobs for an assignment.
     * No-op if the user doesn't have push notifications enabled or the due
     * date can't be parsed.
     */
    fun scheduleForAssignment(context: Context, assignment: Assignment, user: User) {
        // Always clear any previous jobs first so stale fires never linger
        // after a due-date change.
        cancelForAssignment(context, assignment.id)

        if (user.pushNotificationsEnabled != true) return
        val dueDate = AssignmentDateTimeUtils.parseDueDate(assignment.dueDate) ?: return

        val now = LocalDateTime.now()
        val reminders = listOf(
            AssignmentReminderWorker.REMINDER_WEEK  to dueDate.minusDays(7),
            AssignmentReminderWorker.REMINDER_DAY   to dueDate.minusDays(1),
            // Day-of fires at 08:00 local time so it doesn't ping at midnight.
            AssignmentReminderWorker.REMINDER_TODAY to dueDate.toLocalDate().atTime(8, 0)
        )

        val wm = WorkManager.getInstance(context)
        reminders.forEach { (type, fireAt) ->
            if (!fireAt.isAfter(now)) return@forEach
            val delayMs = Duration.between(now, fireAt).toMillis()

            val data = Data.Builder()
                .putInt(AssignmentReminderWorker.KEY_ASSIGNMENT_ID, assignment.id)
                .putInt(AssignmentReminderWorker.KEY_USER_ID, user.id)
                .putString(AssignmentReminderWorker.KEY_REMINDER_TYPE, type)
                .build()

            val request = OneTimeWorkRequestBuilder<AssignmentReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tagForAssignment(assignment.id))
                .addTag(tagForUser(user.id))
                .build()

            wm.enqueueUniqueWork(
                uniqueNameFor(assignment.id, type),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun cancelForAssignment(context: Context, assignmentId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagForAssignment(assignmentId))
    }

    fun cancelAllForUser(context: Context, userId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagForUser(userId))
    }

    /**
     * Re-schedule every reminder for a user. Used when the user just enabled
     * push notifications — we walk all of their existing assignments and
     * queue reminders for each that's still in the future.
     */
    fun rescheduleAllForUser(context: Context, user: User, assignments: List<Assignment>) {
        cancelAllForUser(context, user.id)
        if (user.pushNotificationsEnabled != true) return
        assignments.forEach { scheduleForAssignment(context, it, user) }
    }

    private fun tagForAssignment(id: Int) = "assignment_$id"
    private fun tagForUser(id: Int) = "user_$id"
    private fun uniqueNameFor(assignmentId: Int, type: String) =
        "assignment_${assignmentId}_$type"
}
