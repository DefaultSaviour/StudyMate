package uws.ac.uk.studymate.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
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
updated 17/04/26
updated 18/04/26
 *//////////////////////
class HomeActivity : AppCompatActivity() {

    private lateinit var homeVm: HomeViewModel
    private var isPushNotificationsDialogShowing = false

    // After the user opts in to reminders we fire the real OS POST_NOTIFICATIONS
    // dialog (API 33+). Delivery is re-checked at fire time, so we don't need the
    // result here — granting just lets notifications actually show.
    private val postNotificationsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* no-op: worker re-verifies permission when it fires */ }

    // Cached from the latest dashboard load so the "Review due decks" button knows
    // which decks (in order) to walk through when tapped.
    private var dueDeckIds: List<Int> = emptyList()
    private var dueDeckNames: List<String> = emptyList()

    /**
     This screen is the main hub that sends the user to the rest of the app.
     it started as a small home page, and later got more buttons as the other screens were added.
     it now shows the next due work and the main places the user needs to go
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)

        // Pad the scroll view so the last nav button clears the system nav bar on any device.
        val scrollView = findViewById<NestedScrollView>(R.id.homeScrollView)
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            view.setPadding(0, 0, 0, navBar + base)
            insets
        }

        // Set up the ViewModel used by this screen.
        homeVm = ViewModelProvider(this)[HomeViewModel::class.java]

        // Get the views used on this screen.
        val userSettingsBtn = findViewById<Button>(R.id.userSettingsBtn)
        val nextDueCountdownText = findViewById<TextView>(R.id.nextDueCountdownText)
        val nextDueDetailsText = findViewById<TextView>(R.id.nextDueDetailsText)
        val assignmentsBtn = findViewById<Button>(R.id.assignmentsBtn)
        val flashcardsBtn = findViewById<Button>(R.id.flashcardsBtn)
        val calendarBtn = findViewById<Button>(R.id.calendarBtn)
        val statisticsBtn = findViewById<Button>(R.id.statisticsBtn)
        val reviewDueBtn = findViewById<Button>(R.id.reviewDueBtn)
        val reviewDueGlow = findViewById<PulseRingView>(R.id.reviewDueGlow)
        // Disabled for now: this testing-only ClearAllData button used to wipe every table.
        // It is commented out so it can be re-enabled later.
//        val clearDataBtn = findViewById<Button>(R.id.clearDataBtn)

        // Show the latest dashboard data when the ViewModel finishes loading it.
        homeVm.homeSummary.observe(this) { summary ->
            nextDueCountdownText.text = summary.nextDueCountdown
            nextDueDetailsText.text = summary.nextDueDetails

            // Enable "Review due decks" only when something is actually due; light up
            // the travelling glow while it's active and dim/stop it when it isn't.
            dueDeckIds = summary.dueDeckIds
            dueDeckNames = summary.dueDeckNames
            val hasDue = dueDeckIds.isNotEmpty()
            reviewDueBtn.isEnabled = hasDue
            reviewDueBtn.alpha = if (hasDue) 1f else 0.45f
            reviewDueBtn.text = if (hasDue) {
                "Review ${summary.dueCardCount} card${if (summary.dueCardCount == 1) "" else "s"} now"
            } else {
                "No decks due"
            }
            if (hasDue) {
                reviewDueGlow.visibility = View.VISIBLE
                reviewDueGlow.startAnimating()
            } else {
                reviewDueGlow.stopAnimating()
                reviewDueGlow.visibility = View.GONE
            }
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

        // Open the flashcard decks screen so the user can browse and manage their decks.
        flashcardsBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.FlashcardDecksActivity"))
        }

        // Subjects now live inside the Assignments screen (reached via its "Subjects"
        // button), so there is no longer a dashboard button for them here.

        // Open the calendar screen so the user can see assignment dates in a month view.
        calendarBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.CalendarActivity"))
        }

        // Open the statistics screen — study streak, reviews, assignment progress.
        statisticsBtn.setOnClickListener {
            startActivity(Intent().setClassName(packageName, "$packageName.ui.StatisticsActivity"))
        }

        // Review every due deck back-to-back: hand the ordered deck queue to the review
        // screen, which finishes one deck then immediately starts the next.
        reviewDueBtn.setOnClickListener {
            if (dueDeckIds.isEmpty()) return@setOnClickListener
            startActivity(
                Intent().setClassName(packageName, "$packageName.ui.ReviewDeckActivity")
                    .putExtra("deck_queue_ids", dueDeckIds.toIntArray())
                    .putExtra("deck_queue_names", dueDeckNames.toTypedArray())
            )
        }

        // Disabled for now: this testing-only click handler used to wipe every table.
        // It is commented out so it can be re-enabled later.
//        clearDataBtn.setOnClickListener {
//            homeVm.clearAllData()
//        }

        // Scatter ambient orbs into the wood band above the card (count scales to
        // the available space, avoids the settings button, never under the card).
        uws.ac.uk.studymate.util.OrbField.scatter(
            findViewById(R.id.homeCard),
            listOf(findViewById(R.id.userSettingsBtn))
        )

        // Entrance animation: card slides up, then content staggers in.
        val density = resources.displayMetrics.density
        val homeCard = findViewById<MaterialCardView>(R.id.homeCard)
        val nextDueContainer = findViewById<View>(R.id.nextDueContainer)
        val navDivider = findViewById<View>(R.id.navDivider)
        val navSectionLabel = findViewById<View>(R.id.navSectionLabel)

        homeCard.translationY = 200f * density
        homeCard.alpha = 0f
        homeCard.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val reviewDueContainer = findViewById<View>(R.id.reviewDueContainer)
        val staggerViews = listOf(
            nextDueContainer, navDivider, navSectionLabel,
            assignmentsBtn, flashcardsBtn, calendarBtn, statisticsBtn, reviewDueContainer
        )
        staggerViews.forEach { v ->
            v.alpha = 0f
            v.translationY = 28f * density
        }

        homeCard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .setStartDelay(60)
            .withEndAction { homeCard.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()

        staggerViews.forEachIndexed { i, v ->
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setInterpolator(DecelerateInterpolator(1.3f))
                .setStartDelay(260 + i * 65L)
                .start()
        }
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

    // Ask the user if they want push notifications now that they are past the login
    // screen. Themed to match the wood-glass UI; tapping Yes records the preference
    // and then fires the real Android permission dialog (API 33+).
    private fun showPushNotificationsChoice(user: User) {
        isPushNotificationsDialogShowing = true

        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle(R.string.push_notifications_prompt_title)
            .setMessage(R.string.push_notifications_prompt_message)
            .setCancelable(false)
            .setPositiveButton(R.string.yes_button) { _, _ ->
                savePushNotificationsChoice(user.id, true)
                requestSystemNotificationPermissionIfNeeded()
            }
            .setNegativeButton(R.string.no_button) { _, _ ->
                savePushNotificationsChoice(user.id, false)
            }
            .show()
    }

    // On API 33+ a runtime permission gates delivery — request it once the user has
    // opted in. Pre-33 has no such permission (notifications are on by default).
    private fun requestSystemNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) postNotificationsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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

    private fun floatOrb(view: View, amplitude: Float, duration: Long, delay: Long) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, -amplitude, amplitude).apply {
            this.duration = duration
            startDelay = delay
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}
