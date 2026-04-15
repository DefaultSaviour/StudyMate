package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class AllScreensTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val paddingPx = (32 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }

        // List of screens you want to test
        val screens = listOf(
            "Calendar" to CalendarActivity::class.java,
            "Statistics" to StatisticsActivity::class.java,
            "Subjects" to SubjectsActivity::class.java,
            "Assignments" to AssignmentsActivity::class.java,
            "Add Assignment" to AddAssignmentActivity::class.java,
            "User Settings" to UserSettingsActivity::class.java
        )

        // Create a button for each screen
        screens.forEach { (label, activityClass) ->
            val btn = Button(this).apply {
                text = label
                setOnClickListener {
                    startActivity(Intent(this@AllScreensTestActivity, activityClass))
                }
            }
            layout.addView(btn)
        }

        // Wrap layout in ScrollView
        setContentView(ScrollView(this).apply { addView(layout) })
    }
}