package uws.ac.uk.studymate.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uws.ac.uk.studymate.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)

        val emailCard = findViewById<LinearLayout>(R.id.emailSettingsCard)
        val emailDropdown = findViewById<LinearLayout>(R.id.emailDropdown)
        val emailArrow = findViewById<ImageView>(R.id.emailArrow)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val saveEmailBtn = findViewById<Button>(R.id.saveEmailBtn)

        val notificationsCard = findViewById<LinearLayout>(R.id.notificationsCard)
        val notificationsDropdown = findViewById<LinearLayout>(R.id.notificationsDropdown)
        val notificationsArrow = findViewById<ImageView>(R.id.notificationsArrow)
        val pushNotificationSwitch = findViewById<Switch>(R.id.pushNotificationSwitch)
        val reminderSwitch = findViewById<Switch>(R.id.reminderSwitch)

        val privacyCard = findViewById<LinearLayout>(R.id.privacyCard)
        val privacyDropdown = findViewById<LinearLayout>(R.id.privacyDropdown)
        val privacyArrow = findViewById<ImageView>(R.id.privacyArrow)

        val logoutCard = findViewById<LinearLayout>(R.id.logoutCard)

        val assignmentsCountText = findViewById<TextView>(R.id.assignmentsCount)
        val flashcardsCountText = findViewById<TextView>(R.id.flashcardsCount)

        // TODO load student name from User database and set to TextView
        // TODO load student email from User database and set to TextView
        // TODO load assignment count from Assignment database for logged-in user
        // TODO load flashcard count from Flashcard database for logged-in user

        // TEMP dynamic values for UI only
        // TODO replace with real Assignment and Flashcard database values
        val tempAssignmentCount = (5..15).random()
        val tempFlashcardCount = (20..60).random()

        assignmentsCountText.text = tempAssignmentCount.toString()
        flashcardsCountText.text = tempFlashcardCount.toString()

        backBtn.setOnClickListener {
            finish()
        }

        emailCard.setOnClickListener {
            toggleDropdown(emailDropdown, emailArrow)
        }

        notificationsCard.setOnClickListener {
            toggleDropdown(notificationsDropdown, notificationsArrow)
        }

        privacyCard.setOnClickListener {
            toggleDropdown(privacyDropdown, privacyArrow)
        }

        saveEmailBtn.setOnClickListener {
            val emailText = emailInput.text.toString().trim()

            if (emailText.isEmpty()) {
                Toast.makeText(this, "Enter an email address", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
            } else {
                // TODO update email in User database
                // TODO connect updated email back to profile display
                Toast.makeText(this, "Email update ready", Toast.LENGTH_SHORT).show()
            }
        }

        // TODO load notification settings from UserSettings database

        reminderSwitch.isEnabled = false
        reminderSwitch.alpha = 0.5f

        pushNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminderSwitch.isEnabled = isChecked
            reminderSwitch.alpha = if (isChecked) 1f else 0.5f

            if (!isChecked) {
                reminderSwitch.isChecked = false
            }

            // TODO save push notification setting to UserSettings database
            Toast.makeText(this, "Notification settings updated", Toast.LENGTH_SHORT).show()
        }

        reminderSwitch.setOnCheckedChangeListener { _, _ ->
            if (reminderSwitch.isEnabled) {
                // TODO save reminder setting to UserSettings database
                Toast.makeText(this, "Reminder settings updated", Toast.LENGTH_SHORT).show()
            }
        }

        logoutCard.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    // TODO clear logged-in user session
                    // TODO return to Login screen
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun toggleDropdown(dropdown: LinearLayout, arrow: ImageView) {
        if (dropdown.visibility == View.GONE) {
            dropdown.visibility = View.VISIBLE
            arrow.rotation = 90f
        } else {
            dropdown.visibility = View.GONE
            arrow.rotation = 270f
        }
    }
}