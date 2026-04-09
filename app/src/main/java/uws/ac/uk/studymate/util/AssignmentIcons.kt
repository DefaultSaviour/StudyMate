package uws.ac.uk.studymate.util

import androidx.annotation.DrawableRes
import uws.ac.uk.studymate.R

data class AssignmentIconOption(
    val key: String,
    @DrawableRes val drawableResId: Int
)

object AssignmentIcons {
    const val DEFAULT_KEY = "assignment"

    val options: List<AssignmentIconOption> = listOf(
        AssignmentIconOption(
            key = DEFAULT_KEY,
            drawableResId = R.drawable.ic_assignment_assignment
        ),
        AssignmentIconOption(
            key = "calculator",
            drawableResId = R.drawable.ic_assignment_calculator
        ),
        AssignmentIconOption(
            key = "arrow",
            drawableResId = R.drawable.ic_assignment_arrow
        ),
        AssignmentIconOption(
            key = "flag",
            drawableResId = R.drawable.ic_assignment_flag
        ),
        AssignmentIconOption(
            key = "warning",
            drawableResId = R.drawable.ic_assignment_warning
        ),
        AssignmentIconOption(
            key = "book",
            drawableResId = R.drawable.ic_assignment_book
        ),
        AssignmentIconOption(
            key = "schedule",
            drawableResId = R.drawable.ic_assignment_schedule
        ),
        AssignmentIconOption(
            key = "code",
            drawableResId = R.drawable.ic_assignment_code
        ),
        AssignmentIconOption(
            key = "quiz",
            drawableResId = R.drawable.ic_assignment_quiz
        )
    )

    fun optionForKey(key: String?): AssignmentIconOption {
        return options.firstOrNull { it.key == key } ?: options.first()
    }

    @DrawableRes
    fun drawableForKey(key: String?): Int {
        return optionForKey(key).drawableResId
    }
}

