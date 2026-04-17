package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.ui.viewmodels.HomeViewModel
/*//////////////////////
Coded by Jamie Coleman
15/03/26 - ??? was it ???
updated 18/03/26
updated 28/03/26
updated 09/04/26
updated 16/04/26
 *//////////////////////
class HomeActivity : AppCompatActivity() {

    private lateinit var homeVm: HomeViewModel
    private var isPushNotificationsDialogShowing = false

    /**
     This screen is the main hub that sends the user to the rest of the app.
     it started as a small home page, and later got more buttons as the other screens were added.
     it now shows the next due work and the main places the user needs to go
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Set up the ViewModel used by this screen.
        homeVm = ViewModelProvider(this)[HomeViewModel::class.java]

        // Get the views used on this screen.
        val userSettingsBtn = findViewById<Button>(R.id.userSettingsBtn)
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val nextDueCountdownText = findViewById<TextView>(R.id.nextDueCountdownText)
        val nextDueDetailsText = findViewById<TextView>(R.id.nextDueDetailsText)
        val assignmentsBtn = findViewById<Button>(R.id.assignmentsBtn)
        val flashcardsBtn = findViewById<Button>(R.id.flashcardsBtn)
        val subjectsBtn = findViewById<Button>(R.id.subjectsBtn)
        val calendarBtn = findViewById<Button>(R.id.calendarBtn)
        val statisticsBtn = findViewById<Button>(R.id.statisticsBtn)
        // Disabled for now: this testing-only ClearAllData button used to wipe every table.
        // It is commented out so it can be re-enabled later.
//        val clearDataBtn = findViewById<Button>(R.id.clearDataBtn)

        // Show the latest dashboard data when the ViewModel finishes loading it.
        homeVm.homeSummary.observe(this) { summary ->
            welcomeText.text = summary.welcomeText
            nextDueCountdownText.text = summary.nextDueCountdown
            nextDueDetailsText.text = summary.nextDueDetails
        }

        // Send the user back to login when there is no valid session.
        homeVm.sessionExpired.observe(this) { expired ->
            if (expired) {
                openLogin()
            }
        }

        // Ask about push notifications only after the user has reached this home screen.
        homeVm.userNeedingPushChoice.observe(this) { user ->
            if (user == null || isPushNotificationsDialogShowing) {
                return@observe
            }

            homeVm.clearUserNeedingPushChoice()
            showPushNotificationsChoice(user)
        }

        // Disabled for now: this testing-only observer used to react after wiping every table.
        // It is commented out so it can be re-enabled later.
//        homeVm.allDataCleared.observe(this) { cleared ->
//            if (cleared) {
//                Toast.makeText(this, "All saved data deleted", Toast.LENGTH_SHORT).show()
//                openLogin()
//            }
//        }

        // Open the user settings screen from the button at the top left.
        userSettingsBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.UserSettingsActivity"))
        }

        // Open the assignments screen because assignment items now live there.
        assignmentsBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.AssignmentsActivity"))
        }

        flashcardsBtn.setOnClickListener {
            Toast.makeText(this, "Flashcards screen not built yet", Toast.LENGTH_SHORT).show()
        }

        // Open the subjects screen so the user can add or remove subjects.
        subjectsBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.SubjectsActivity"))
        }

        // Open the calendar screen so the user can see assignment dates in a month view.
        calendarBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.CalendarActivity"))
        }


        // Open the full statistics screen because stats no longer live on home.
        statisticsBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.StatisticsActivity"))
        }

        // Disabled for now: this testing-only click handler used to wipe every table.
        // It is commented out so it can be re-enabled later.
//        clearDataBtn.setOnClickListener {
//            homeVm.clearAllData()
//        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the dashboard each time the user returns to this screen.
        homeVm.loadHome()
    }

    // Replace the home screen with the login screen when the session ends.
    private fun openLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    // Ask the user if they want push notifications now that they are past the login screen.
    private fun showPushNotificationsChoice(user: User) {
        isPushNotificationsDialogShowing = true

        AlertDialog.Builder(this)
            .setTitle(R.string.push_notifications_prompt_title)
            .setMessage(R.string.push_notifications_prompt_message)
            .setCancelable(false)
            .setPositiveButton(R.string.yes_button) { _, _ ->
                savePushNotificationsChoice(user.id, true)
            }
            .setNegativeButton(R.string.no_button) { _, _ ->
                savePushNotificationsChoice(user.id, false)
            }
            .show()
    }

    // Save the user's choice and keep the home screen open.
    private fun savePushNotificationsChoice(userId: Int, enabled: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                homeVm.savePushNotificationsChoice(userId, enabled)
            }
            isPushNotificationsDialogShowing = false
        }
    }
}

