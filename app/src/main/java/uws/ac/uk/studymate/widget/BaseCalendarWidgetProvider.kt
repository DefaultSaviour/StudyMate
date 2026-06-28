package uws.ac.uk.studymate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.ui.LoginActivity
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.ColorUtils
import uws.ac.uk.studymate.util.SessionManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

abstract class BaseCalendarWidgetProvider : AppWidgetProvider() {

    abstract val layoutId: Int
    open val showBottomSections: Boolean = true

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, layoutId)
        val sessionManager = SessionManager(context)
        val userId = sessionManager.getLoggedInUserId() ?: sessionManager.getLastUserId()

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) 
            ?: Intent(context, LoginActivity::class.java)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        if (userId == null) {
            if (showBottomSections) {
                views.setTextViewText(R.id.widget_next_assignment_title, "Not logged in")
                views.setTextViewText(R.id.widget_next_assignment_date, "Tap to login to StudyMate")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        val db = StudyMateDatabase.getInstance(context)
        val assignments = db.assignmentDao().getAssignments(userId)
        val customEvents = db.customEventDao().getEventsForUser(userId)
        val now = LocalDateTime.now()
        val todayStr = LocalDate.now().toString()
        val deckReviews = db.deckDao().getDeckReviewDates(userId, todayStr)

        if (showBottomSections) {
            val dueCards = db.cardDao().getDueCardsActive(userId, todayStr, now.toString())
            val nextDueAssignment = findNextDueAssignment(assignments, now)
            if (nextDueAssignment != null) {
                views.setTextViewText(R.id.widget_next_assignment_title, nextDueAssignment.first.title)
                views.setTextViewText(
                    R.id.widget_next_assignment_date,
                    AssignmentDateTimeUtils.formatDueDate(nextDueAssignment.second)
                )
            } else {
                views.setTextViewText(R.id.widget_next_assignment_title, "No upcoming assignments")
                views.setTextViewText(R.id.widget_next_assignment_date, "Tap to add one")
            }
            views.setTextViewText(R.id.widget_flashcards_count, dueCards.size.toString())
        }

        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dateFormatter = DateTimeFormatter.ofPattern("d")
        
        fun bindDay(
            dayOffset: Long,
            labelId: Int,
            dateId: Int,
            dot1Id: Int,
            dot2Id: Int,
            dot3Id: Int,
            plusId: Int,
            labelStr: String
        ) {
            val date = startOfWeek.plusDays(dayOffset)
            views.setTextViewText(labelId, labelStr)
            views.setTextViewText(dateId, date.format(dateFormatter))
            
            if (date == today) {
                views.setTextColor(dateId, context.getColor(R.color.gold))
                views.setTextColor(labelId, context.getColor(R.color.gold))
                views.setInt(dateId, "setBackgroundResource", R.drawable.shape_ring_gold)
            } else {
                views.setTextColor(dateId, context.getColor(R.color.surface))
                views.setTextColor(labelId, context.getColor(R.color.gold_light))
                views.setInt(dateId, "setBackgroundResource", 0)
            }

            val dayAssignments = assignments.filter { 
                val dueAt = AssignmentDateTimeUtils.parseDueDate(it.dueDate)
                dueAt != null && dueAt.toLocalDate() == date
            }
            val dayEvents = customEvents.filter {
                val eventDate = try { LocalDate.parse(it.date) } catch(e: Exception) { null }
                eventDate == date
            }
            val dayReviews = deckReviews.filter {
                val reviewDate = try { LocalDate.parse(it.dueAt) } catch(e: Exception) { null }
                reviewDate == date
            }
            
            val items = (dayAssignments.map { Triple(0, false, it.color) } + 
                         dayReviews.map { Triple(1, true, it.assignmentColor) } +
                         dayEvents.map { Triple(2, false, it.color) }).sortedBy { it.first }
            val count = items.size
            
            fun applyIcon(dotId: Int, index: Int) {
                if (index < count) {
                    views.setViewVisibility(dotId, View.VISIBLE)
                    val isDash = items[index].second
                    val colorHex = items[index].third
                    val shapeRes = if (isDash) R.drawable.shape_indicator_dash else R.drawable.shape_indicator_dot
                    views.setImageViewResource(dotId, shapeRes)
                    views.setInt(dotId, "setColorFilter", ColorUtils.parseOrDefault(colorHex))
                } else {
                    views.setViewVisibility(dotId, View.GONE)
                }
            }
            
            applyIcon(dot1Id, 0)
            applyIcon(dot2Id, 1)
            applyIcon(dot3Id, 2)
            
            if (count > 3) {
                views.setViewVisibility(plusId, View.VISIBLE)
                views.setTextViewText(plusId, "+${count - 3}")
            } else {
                views.setViewVisibility(plusId, View.GONE)
            }
        }

        bindDay(0, R.id.widget_day_1_label, R.id.widget_day_1_date, R.id.widget_day_1_dot1, R.id.widget_day_1_dot2, R.id.widget_day_1_dot3, R.id.widget_day_1_plus, "M")
        bindDay(1, R.id.widget_day_2_label, R.id.widget_day_2_date, R.id.widget_day_2_dot1, R.id.widget_day_2_dot2, R.id.widget_day_2_dot3, R.id.widget_day_2_plus, "T")
        bindDay(2, R.id.widget_day_3_label, R.id.widget_day_3_date, R.id.widget_day_3_dot1, R.id.widget_day_3_dot2, R.id.widget_day_3_dot3, R.id.widget_day_3_plus, "W")
        bindDay(3, R.id.widget_day_4_label, R.id.widget_day_4_date, R.id.widget_day_4_dot1, R.id.widget_day_4_dot2, R.id.widget_day_4_dot3, R.id.widget_day_4_plus, "T")
        bindDay(4, R.id.widget_day_5_label, R.id.widget_day_5_date, R.id.widget_day_5_dot1, R.id.widget_day_5_dot2, R.id.widget_day_5_dot3, R.id.widget_day_5_plus, "F")
        bindDay(5, R.id.widget_day_6_label, R.id.widget_day_6_date, R.id.widget_day_6_dot1, R.id.widget_day_6_dot2, R.id.widget_day_6_dot3, R.id.widget_day_6_plus, "S")
        bindDay(6, R.id.widget_day_7_label, R.id.widget_day_7_date, R.id.widget_day_7_dot1, R.id.widget_day_7_dot2, R.id.widget_day_7_dot3, R.id.widget_day_7_plus, "S")

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun findNextDueAssignment(assignments: List<Assignment>, now: LocalDateTime): Pair<Assignment, LocalDateTime>? {
        return assignments
            .mapNotNull { assignment ->
                val dueAt = AssignmentDateTimeUtils.parseDueDate(assignment.dueDate) ?: return@mapNotNull null
                if (AssignmentDateTimeUtils.isComplete(assignment.completedAt, assignment.dueDate, now)) {
                    return@mapNotNull null
                }
                assignment to dueAt
            }
            .minByOrNull { (_, dueAt) -> dueAt }
    }
}
