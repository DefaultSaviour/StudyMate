package uws.ac.uk.studymate.ui

import uws.ac.uk.studymate.R
import android.content.res.ColorStateList
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.ui.viewmodels.StatisticsSummary
import uws.ac.uk.studymate.ui.viewmodels.StatisticsViewModel
import uws.ac.uk.studymate.ui.viewmodels.SubjectDeckProgress
import androidx.core.graphics.toColorInt

class StatisticsActivity : AppCompatActivity() {

    private lateinit var statisticsVm: StatisticsViewModel
    private lateinit var statisticsTitleText: TextView
    private lateinit var metricsContainer: LinearLayout
    private lateinit var subjectProgressContainer: LinearLayout

    /**
     This screen gives the user a quick view of their progress.
     it started as a plain text page, and later got the summary cards and subject progress list.
     it now keeps the main numbers at the top and the subject detail underneath.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_statistics)

        // Set up the ViewModel used by this screen.
        statisticsVm = ViewModelProvider(this)[StatisticsViewModel::class.java]

        statisticsTitleText = findViewById(R.id.statisticsTitleText)
        metricsContainer = findViewById(R.id.metricsContainer)
        subjectProgressContainer = findViewById(R.id.subjectProgressContainer)

        val homeBtn: ImageButton = findViewById(R.id.homeBtn)
        homeBtn.setOnClickListener { openHome()
        }

        val backBtn: ImageButton = findViewById(R.id.backBtn)
        backBtn.setOnClickListener { finish()
        }

        // Show the latest statistics data when the ViewModel finishes loading it.
        statisticsVm.statisticsSummary.observe(this) { summary ->
            showStatistics(summary)
        }

        // Send the user back to login when there is no valid session.
//        statisticsVm.sessionExpired.observe(this) { expired ->
//            if (expired) {
//                openLogin()
//            }
//        } disabled for testing
    }

    override fun onResume() {
        super.onResume()

        // Reload the statistics each time the user returns to this screen.
        statisticsVm.loadStatistics()
    }

    // Show the latest title, summary cards, and subject progress rows.
    private fun showStatistics(summary: StatisticsSummary) {
        statisticsTitleText.text = summary.titleText
        renderSubjectProgress(summary.subjectProgress)
    }

    /**
     Show the subject progress section under the main numbers.
     this started as simple text, and later changed into one row per subject.
     it now makes it easier to see which subject is moving and which one is still behind.
     **/
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

    // Inflate subject_progress_row.xml for each subject row
    private fun createSubjectProgressCard(item: SubjectDeckProgress): LinearLayout {
        val row = layoutInflater.inflate(
            R.layout.subject_progress_row,
            subjectProgressContainer,
            false
        )

        val subjectNameText = row.findViewById<TextView>(R.id.subjectNameText)
        val countText = row.findViewById<TextView>(R.id.countText)
        val deckNameText = row.findViewById<TextView>(R.id.deckNameText)
        val progressBar = row.findViewById<ProgressBar>(R.id.progressBar)

        subjectNameText.text = item.subjectName
        countText.text = getString(R.string.subject_progress_count, item.completedCards, item.totalCards)
        deckNameText.text = item.deckLabel

        progressBar.max = if (item.totalCards > 0) item.totalCards else 1
        progressBar.progress = item.completedCards
        progressBar.progressTintList = ColorStateList.valueOf(parseSubjectColor(item.colorHex))
        progressBar.progressBackgroundTintList = ColorStateList.valueOf("#D9D9D9".toColorInt())

        return row as LinearLayout
    }

    // Read the saved subject color, or fall back to a safe blue when it is missing.
    private fun parseSubjectColor(colorHex: String?): Int {
        return try {
            if (colorHex.isNullOrBlank()) "#3B82F6".toColorInt() else colorHex.toColorInt()
        } catch (_: Exception) {
            "#3B82F6".toColorInt()
        }
    }
    // Open login screen
    private fun openLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    // Open home screen
    private fun openHome() {
        startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
    }
}

