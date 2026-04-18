package uws.ac.uk.studymate.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.AssignmentsItem
import uws.ac.uk.studymate.ui.viewmodels.AssignmentsViewModel
import uws.ac.uk.studymate.util.AssignmentIcons
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
/*//////////////////////
Coded by Jamie Coleman
05/04/26
fixed 08/04/26
 *//////////////////////
class AssignmentsActivity : AppCompatActivity() {

    private lateinit var assignmentsVm: AssignmentsViewModel
    private lateinit var assignmentsTitleText: TextView
    private lateinit var emptyAssignmentsText: TextView
    private lateinit var assignmentsListContainer: LinearLayout

    /**
     This screen shows the user's assignment list in one place.
     it started from the old calendar list screen, and later got renamed because this made more sense.
     it now keeps only the upcoming assignments and shows them in a cleaner order.

     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the ViewModel used by this screen.
        assignmentsVm = ViewModelProvider(this)[AssignmentsViewModel::class.java]

        // Inflate the XML layout and bind the dynamic views used by rendering methods.
        setContentView(R.layout.activity_assignments)
        assignmentsTitleText = findViewById(R.id.assignmentsTitleText)
        val homeBtn: Button = findViewById(R.id.homeBtn)
        val addAssignmentBtn: Button = findViewById(R.id.addAssignmentBtn)
        emptyAssignmentsText = findViewById(R.id.emptyAssignmentsText)
        assignmentsListContainer = findViewById(R.id.assignmentsListContainer)

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
        }

        // Open the add assignment screen from the assignments page.
        addAssignmentBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.AddAssignmentActivity"))
        }

        assignmentsVm.assignmentsSummary.observe(this) { summary ->
            assignmentsTitleText.text = summary.titleText
            showAssignments(summary.items)
        }

        assignmentsVm.sessionExpired.observe(this) { expired ->
            if (expired) {
                openLogin()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the assignment list each time the user returns to this screen.
        assignmentsVm.loadAssignments()
    }

    // Replace this screen with the login screen when the session ends.
    private fun openLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    // Return to the main home screen from the top-right button.
    private fun openHome() {
        startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
    }

    // Show the current upcoming assignments as simple cards with the chosen icon.
    private fun showAssignments(assignments: List<AssignmentsItem>) {
        assignmentsListContainer.removeAllViews()

        if (assignments.isEmpty()) {
            emptyAssignmentsText.text = getString(R.string.no_upcoming_assignments_yet)
            emptyAssignmentsText.visibility = TextView.VISIBLE
            return
        }

        emptyAssignmentsText.visibility = TextView.GONE
        assignments.forEach { item ->
            assignmentsListContainer.addView(createAssignmentCard(item))
        }
    }

    // Build one assignment card with the saved icon inside a box that uses the subject color.
    private fun createAssignmentCard(item: AssignmentsItem): LinearLayout {
        val subjectColor = parseSubjectColor(item.subjectColorHex)
        val padding = (14 * resources.displayMetrics.density).toInt()
        val badgeSize = (48 * resources.displayMetrics.density).toInt()
        val iconSize = (22 * resources.displayMetrics.density).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
            background = buildAssignmentCardBackground()

            addView(
                LinearLayout(this@AssignmentsActivity).apply {
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(badgeSize, badgeSize).apply {
                        marginEnd = (12 * resources.displayMetrics.density).toInt()
                    }
                    background = buildIconBadgeBackground(subjectColor)
                    addView(
                        ImageView(this@AssignmentsActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                            setImageResource(AssignmentIcons.drawableForKey(item.iconKey))
                            setColorFilter(Color.WHITE)
                        }
                    )
                }
            )

            addView(
                LinearLayout(this@AssignmentsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)

                    addView(
                        TextView(this@AssignmentsActivity).apply {
                            text = item.subjectName
                            textSize = 14f
                            setTextColor(Color.BLACK)
                        }
                    )

                    addView(
                        TextView(this@AssignmentsActivity).apply {
                            text = getString(R.string.assignment_label_with_title, item.title)
                            textSize = 16f
                            setTextColor(Color.BLACK)
                        }
                    )

                    addView(
                        TextView(this@AssignmentsActivity).apply {
                            text = getString(
                                R.string.time_due_label_with_value,
                                AssignmentDateTimeUtils.formatDueDate(item.dueAt)
                            )
                            textSize = 14f
                            setTextColor(Color.BLACK)
                        }
                    )
                }
            )
        }
    }


    // Read the saved subject color, or fall back to a safe dark color when it is missing.
    private fun parseSubjectColor(colorHex: String?): Int {
        return try {
            if (colorHex.isNullOrBlank()) Color.parseColor("#444444") else Color.parseColor(colorHex)
        } catch (_: Exception) {
            Color.parseColor("#444444")
        }
    }

    // Keep each assignment card clean and light so the icon badge stands out.
    private fun buildAssignmentCardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f * resources.displayMetrics.density
            setColor(Color.parseColor("#F7F7F7"))
            setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#D8D8D8"))
        }
    }

    // Use the subject color behind the icon so the assignment can be recognised quickly.
    private fun buildIconBadgeBackground(subjectColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 14f * resources.displayMetrics.density
            setColor(subjectColor)
        }
    }
}


