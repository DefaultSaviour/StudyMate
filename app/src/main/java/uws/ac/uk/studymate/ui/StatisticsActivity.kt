package uws.ac.uk.studymate.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.StatsSummary
import uws.ac.uk.studymate.ui.viewmodels.StatisticsViewModel
import uws.ac.uk.studymate.ui.viewmodels.SubjectProgressItem
import uws.ac.uk.studymate.util.ColorUtils

// Read-only dashboard of the user's study progress (flashcard reviews, streak,
// assignment completion). Rows are built programmatically from StatsSummary so
// the layout stays small. Wood-glass styling per the design system.
class StatisticsActivity : AppCompatActivity() {

    private lateinit var vm: StatisticsViewModel

    private lateinit var flashcardStatsContainer: LinearLayout
    private lateinit var assignmentStatsContainer: LinearLayout
    private lateinit var subjectProgressContainer: LinearLayout
    private lateinit var subjectEmptyHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)

        flashcardStatsContainer = findViewById(R.id.flashcardStatsContainer)
        assignmentStatsContainer = findViewById(R.id.assignmentStatsContainer)
        subjectProgressContainer = findViewById(R.id.subjectProgressContainer)
        subjectEmptyHint = findViewById(R.id.subjectEmptyHint)

        val card = findViewById<MaterialCardView>(R.id.statsCard)
        val scrollView = findViewById<View>(R.id.statsScrollView)
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            view.setPadding(0, 0, 0, navBar + base)
            insets
        }

        findViewById<MaterialButton>(R.id.backBtn).setOnClickListener { finish() }

        uws.ac.uk.studymate.util.OrbField.scatter(
            findViewById(R.id.statsCard),
            listOf(findViewById(R.id.backBtn))
        )

        runEntranceAnimation(card)

        vm = ViewModelProvider(this)[StatisticsViewModel::class.java]
        vm.summary.observe(this) { render(it) }
        vm.sessionExpired.observe(this) { if (it) openLogin() }
    }

    override fun onResume() {
        super.onResume()
        vm.loadStats()
    }

    private fun render(s: StatsSummary) {
        flashcardStatsContainer.removeAllViews()
        addStatRow(flashcardStatsContainer, "Due for review today", s.cardsDue.toString())
        addStatRow(flashcardStatsContainer, "Reviewed today", s.reviewedToday.toString())
        addStatRow(flashcardStatsContainer, "Reviewed this week", s.reviewedThisWeek.toString())
        addStatRow(flashcardStatsContainer, "Study streak", "${s.streakDays} day${if (s.streakDays == 1) "" else "s"}")
        addStatRow(flashcardStatsContainer, "Mature cards", "${s.matureCards} of ${s.totalCards}")

        assignmentStatsContainer.removeAllViews()
        addStatRow(assignmentStatsContainer, "Completed", s.assignmentsCompleted.toString())
        addStatRow(assignmentStatsContainer, "Completed this week", s.assignmentsCompletedThisWeek.toString())
        addStatRow(assignmentStatsContainer, "Still to do", s.assignmentsPending.toString())
        addStatRow(assignmentStatsContainer, "Due this week", s.assignmentsDueThisWeek.toString())

        subjectProgressContainer.removeAllViews()
        if (s.subjectProgress.isEmpty()) {
            subjectEmptyHint.visibility = View.VISIBLE
        } else {
            subjectEmptyHint.visibility = View.GONE
            s.subjectProgress.forEach { addSubjectRow(subjectProgressContainer, it) }
        }
    }

    private fun addStatRow(container: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_subject_row)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        val labelView = TextView(this).apply {
            text = label
            setTextColor(resources.getColor(R.color.gold_light, theme))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valueView = TextView(this).apply {
            text = value
            setTextColor(resources.getColor(R.color.surface, theme))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        row.addView(labelView)
        row.addView(valueView)
        container.addView(row)
    }

    private fun addSubjectRow(container: LinearLayout, item: SubjectProgressItem) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_subject_row)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        val dot = View(this).apply {
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_color_dot)
            (background as? GradientDrawable)?.setColor(ColorUtils.parseOrDefault(item.colorHex))
            layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { marginEnd = dp(10) }
        }
        val nameView = TextView(this).apply {
            text = item.name
            setTextColor(resources.getColor(R.color.surface, theme))
            textSize = 14f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valueView = TextView(this).apply {
            text = "${item.completed}/${item.total}"
            setTextColor(resources.getColor(R.color.gold_light, theme))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }
        row.addView(dot)
        row.addView(nameView)
        row.addView(valueView)
        container.addView(row)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun runEntranceAnimation(card: MaterialCardView) {
        val density = resources.displayMetrics.density
        card.translationY = 200f * density
        card.alpha = 0f
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .setStartDelay(60)
            .withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()
    }


    private fun openLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
