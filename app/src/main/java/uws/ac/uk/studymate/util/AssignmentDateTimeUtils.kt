package uws.ac.uk.studymate.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object AssignmentDateTimeUtils {

    private val readableDueDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    private val readableTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val legacyDueDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun parseDueDate(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) {
            return null
        }

        val trimmedValue = value.trim()

        try {
            return LocalDateTime.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (_: Exception) {
        }

        try {
            return OffsetDateTime.parse(trimmedValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
        } catch (_: Exception) {
        }

        try {
            return LocalDateTime.parse(trimmedValue, legacyDueDateFormatter)
        } catch (_: Exception) {
        }

        try {
            return LocalDate.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay()
        } catch (_: Exception) {
        }

        return null
    }

    fun formatDueDate(dueAt: LocalDateTime): String {
        return dueAt.format(readableDueDateFormatter)
    }

    fun formatDueTime(dueAt: LocalDateTime): String {
        return dueAt.format(readableTimeFormatter)
    }
}

