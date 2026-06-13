package uws.ac.uk.studymate.util

import androidx.annotation.DrawableRes
import uws.ac.uk.studymate.R

data class AssignmentIconOption(
    val key: String,
    @DrawableRes val drawableResId: Int
)

object AssignmentIcons {
    const val DEFAULT_KEY = "math"

    val options: List<AssignmentIconOption> = listOf(
        AssignmentIconOption("math",               R.drawable.ic_subject_math),
        AssignmentIconOption("biology",            R.drawable.ic_subject_biology),
        AssignmentIconOption("chemistry",          R.drawable.ic_subject_chemistry),
        AssignmentIconOption("physics",            R.drawable.ic_subject_physics),
        AssignmentIconOption("english",            R.drawable.ic_subject_english),
        AssignmentIconOption("history",            R.drawable.ic_subject_history),
        AssignmentIconOption("geography",          R.drawable.ic_subject_geography),
        AssignmentIconOption("cs",                 R.drawable.ic_subject_cs),
        AssignmentIconOption("art",                R.drawable.ic_subject_art),
        AssignmentIconOption("music",              R.drawable.ic_subject_music),
        AssignmentIconOption("sports",             R.drawable.ic_subject_sports),
        AssignmentIconOption("economics",          R.drawable.ic_subject_economics),
        AssignmentIconOption("psychology",         R.drawable.ic_subject_psychology),
        AssignmentIconOption("law",                R.drawable.ic_subject_law),
        AssignmentIconOption("languages",          R.drawable.ic_subject_languages),
        AssignmentIconOption("engineering",        R.drawable.ic_subject_engineering),
        AssignmentIconOption("business",           R.drawable.ic_subject_business),
        AssignmentIconOption("philosophy",         R.drawable.ic_subject_philosophy),
        AssignmentIconOption("environment",        R.drawable.ic_subject_environment),
        AssignmentIconOption("statistics",         R.drawable.ic_subject_statistics),
        AssignmentIconOption("medicine",           R.drawable.ic_subject_medicine),
        AssignmentIconOption("drama",              R.drawable.ic_subject_drama),
        AssignmentIconOption("astronomy",          R.drawable.ic_subject_astronomy),
        AssignmentIconOption("design",             R.drawable.ic_subject_design),
        AssignmentIconOption("media",              R.drawable.ic_subject_media),
        AssignmentIconOption("sociology",          R.drawable.ic_subject_sociology),
        AssignmentIconOption("architecture",       R.drawable.ic_subject_architecture),
        AssignmentIconOption("nutrition",          R.drawable.ic_subject_nutrition),
        AssignmentIconOption("politics",           R.drawable.ic_subject_politics),
        AssignmentIconOption("religious_studies",  R.drawable.ic_subject_religious_studies)
    )

    fun optionForKey(key: String?): AssignmentIconOption =
        options.firstOrNull { it.key == key } ?: options.first()

    @DrawableRes
    fun drawableForKey(key: String?): Int = optionForKey(key).drawableResId
}
