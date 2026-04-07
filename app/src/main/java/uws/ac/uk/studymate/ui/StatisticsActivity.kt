package uws.ac.uk.studymate.ui

import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.ui.viewmodels.StatisticsMetric
import uws.ac.uk.studymate.ui.viewmodels.StatisticsSummary
import uws.ac.uk.studymate.ui.viewmodels.StatisticsViewModel
import uws.ac.uk.studymate.ui.viewmodels.SubjectDeckProgress

class StatisticsActivity : AppCompatActivity() {

    private lateinit var statisticsVm: StatisticsViewModel
    private lateinit var statisticsTitleText: TextView
    private lateinit var metricsContainer: LinearLayout
    private lateinit var subjectProgressContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the ViewModel used by this screen.
        statisticsVm = ViewModelProvider(this)[StatisticsViewModel::class.java]

        // Build a simple screen in code so this page does not depend on extra XML files.
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
        }

        statisticsTitleText = TextView(this).apply {
            text = "Statistics"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val homeBtn = Button(this).apply {
            text = "Home"
        }

        val metricsTitleText = TextView(this).apply {
            text = "Summary"
            val topPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
            textSize = 18f
        }

        metricsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        val progressTitleText = TextView(this).apply {
            text = "Subject progress"
            val topPadding = (24 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
            textSize = 18f
        }

        subjectProgressContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        headerRow.addView(statisticsTitleText)
        headerRow.addView(homeBtn)

        contentLayout.addView(headerRow)
        contentLayout.addView(metricsTitleText)
        contentLayout.addView(metricsContainer)
        contentLayout.addView(progressTitleText)
        contentLayout.addView(subjectProgressContainer)

        setContentView(
            ScrollView(this).apply {
                addView(contentLayout)
            }
        )

        // Show the latest statistics data when the ViewModel finishes loading it.
        statisticsVm.statisticsSummary.observe(this) { summary ->
            showStatistics(summary)
        }

        // Send the user back to login when there is no valid session.
        statisticsVm.sessionExpired.observe(this) { expired ->
            if (expired) {
                openLogin()
            }
        }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the statistics each time the user returns to this screen.
        statisticsVm.loadStatistics()
    }

    // Replace this screen with the login screen when the session ends.
    private fun openLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    // Return to the home screen from the top-right button.
    private fun openHome() {
        startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
    }

    // Show the latest title, summary cards, and subject progress rows.
    private fun showStatistics(summary: StatisticsSummary) {
        statisticsTitleText.text = summary.titleText
        renderMetrics(summary.metrics)
        renderSubjectProgress(summary.subjectProgress)
    }

    // Build the two-row summary section shown near the top of the screen.
    private fun renderMetrics(metrics: List<StatisticsMetric>) {
        metricsContainer.removeAllViews()

        metrics.chunked(2).forEach { metricRow ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }

            metricRow.forEachIndexed { index, metric ->
                rowLayout.addView(createMetricCard(metric, index == 0))
            }

            if (metricRow.size == 1) {
                rowLayout.addView(createMetricSpacer())
            }

            metricsContainer.addView(rowLayout)
        }
    }

    // Build the subject-by-subject progress list shown under the summary cards.
    private fun renderSubjectProgress(subjectProgress: List<SubjectDeckProgress>) {
        subjectProgressContainer.removeAllViews()

        if (subjectProgress.isEmpty()) {
            subjectProgressContainer.addView(
                TextView(this).apply {
                    text = "No subject decks or progress saved yet"
                    val topPadding = (12 * resources.displayMetrics.density).toInt()
                    setPadding(0, topPadding, 0, 0)
                }
            )
            return
        }

        subjectProgress.forEach { item ->
            subjectProgressContainer.addView(createSubjectProgressCard(item))
        }
    }

    // Create one simple metric card for the summary grid.
    private fun createMetricCard(metric: StatisticsMetric, isLeftCard: Boolean): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F2F2F2"))
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                if (isLeftCard) {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                } else {
                    marginStart = (8 * resources.displayMetrics.density).toInt()
                }
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }

        val valueText = TextView(this).apply {
            text = metric.value
            textSize = 26f
        }

        val labelText = TextView(this).apply {
            text = metric.label
            val topPadding = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        card.addView(valueText)
        card.addView(labelText)
        return card
    }

    // Add a blank card-sized spacer when the metric count is odd.
    private fun createMetricSpacer(): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginStart = (8 * resources.displayMetrics.density).toInt()
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }
    }

    // Create one subject progress card with a colored progress bar.
    private fun createSubjectProgressCard(item: SubjectDeckProgress): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8F8F8"))
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        val subjectNameText = TextView(this).apply {
            text = item.subjectName
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        val countText = TextView(this).apply {
            text = "${item.completedCards}/${item.totalCards}"
        }

        val deckNameText = TextView(this).apply {
            text = item.deckLabel
            val topPadding = (6 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = if (item.totalCards > 0) item.totalCards else 1
            progress = item.completedCards
            val topPadding = (10 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = topPadding
            }
            val progressColor = parseSubjectColor(item.colorHex)
            progressTintList = ColorStateList.valueOf(progressColor)
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#D9D9D9"))
        }

        titleRow.addView(subjectNameText)
        titleRow.addView(countText)

        card.addView(titleRow)
        card.addView(deckNameText)
        card.addView(progressBar)
        return card
    }

    // Read the saved subject color, or fall back to a safe blue when it is missing.
    private fun parseSubjectColor(colorHex: String?): Int {
        return try {
            if (colorHex.isNullOrBlank()) Color.parseColor("#3B82F6") else Color.parseColor(colorHex)
        } catch (_: Exception) {
            Color.parseColor("#3B82F6")
        }
    }
}

