package uws.ac.uk.studymate.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.ui.viewmodels.FlashcardDecksViewModel
import uws.ac.uk.studymate.ui.viewmodels.SubjectDecksGroup
/*//////////////////////
Coded by Jamie Coleman
 17/04/26
 *//////////////////////
class FlashcardDecksActivity : AppCompatActivity() {

    private lateinit var decksVm: FlashcardDecksViewModel
    private lateinit var screenTitleText: TextView
    private lateinit var decksListContainer: LinearLayout
    private lateinit var emptyText: TextView

    private var subjects: List<Subject> = emptyList()

    /**
     This screen shows all flashcard decks grouped by subject.
     The user can tap a deck to review or alter it, or create a new deck.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard_decks)

        // Set up the ViewModel used by this screen.
        decksVm = ViewModelProvider(this)[FlashcardDecksViewModel::class.java]

        screenTitleText = findViewById(R.id.flashcardDecksTitleText)
        decksListContainer = findViewById(R.id.decksListContainer)
        emptyText = findViewById(R.id.flashcardDecksEmptyText)
        val homeBtn: Button = findViewById(R.id.flashcardDecksHomeBtn)
        val createDeckBtn: Button = findViewById(R.id.createDeckBtn)

        // Show the latest deck data when the ViewModel finishes loading it.
        decksVm.screenSummary.observe(this) { summary ->
            screenTitleText.text = summary.titleText
            subjects = summary.subjects
            showDeckGroups(summary.groups)
        }

        // Send the user back to login when there is no valid session.
        decksVm.sessionExpired.observe(this) { expired ->
            if (expired) {
                openLogin()
            }
        }

        // Show validation and save messages from the ViewModel.
        decksVm.message.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to the alter deck screen after a new deck is created.
        decksVm.createdDeckId.observe(this) { deckId ->
            if (deckId != null) {
                decksVm.clearCreatedDeckId()
                openAlterDeck(deckId)
            }
        }

        // Return to the main home screen from the top-right button.
        homeBtn.setOnClickListener {
            openHome()
        }

        // Show the create deck dialog when the user presses the button.
        createDeckBtn.setOnClickListener {
            showCreateDeckDialog()
        }
    }

    override fun onResume() {
        super.onResume()

        // Reload the deck list each time the user returns to this screen.
        decksVm.loadScreen()
    }

    // Show the deck groups on screen, one subject heading followed by its deck buttons.
    private fun showDeckGroups(groups: List<SubjectDecksGroup>) {
        decksListContainer.removeAllViews()

        if (groups.isEmpty()) {
            emptyText.text = getString(R.string.no_decks_yet_create_one)
            emptyText.visibility = android.view.View.VISIBLE
            return
        }

        emptyText.visibility = android.view.View.GONE

        groups.forEach { group ->
            // Add a subject heading.
            decksListContainer.addView(
                TextView(this).apply {
                    text = group.subject.name
                    textSize = 18f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(parseSubjectColor(group.subject.color))
                    val topPadding = (20 * resources.displayMetrics.density).toInt()
                    setPadding(0, topPadding, 0, 0)
                }
            )

            // Add a button for each deck under this subject.
            group.decks.forEach { deck ->
                decksListContainer.addView(
                    Button(this).apply {
                        text = deck.name
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                            topMargin = (8 * resources.displayMetrics.density).toInt()
                        }
                        setOnClickListener {
                            openDeckOptions(deck.id, deck.name)
                        }
                    }
                )
            }
        }
    }

    // Show a dialog that lets the user pick a subject and name for the new deck.
    private fun showCreateDeckDialog() {
        if (subjects.isEmpty()) {
            Toast.makeText(this, getString(R.string.add_subject_first_message), Toast.LENGTH_SHORT).show()
            return
        }

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val nameInput = EditText(this).apply {
            hint = getString(R.string.enter_deck_name_hint)
        }

        val subjectSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@FlashcardDecksActivity,
                android.R.layout.simple_spinner_item,
                subjects.map { it.name }
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        dialogLayout.addView(TextView(this).apply { text = getString(R.string.deck_name_label) })
        dialogLayout.addView(nameInput)
        dialogLayout.addView(TextView(this).apply {
            text = getString(R.string.subject_label)
            val topPadding = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, topPadding, 0, 0)
        })
        dialogLayout.addView(subjectSpinner)

        AlertDialog.Builder(this)
            .setTitle(R.string.create_new_deck_title)
            .setView(dialogLayout)
            .setPositiveButton(R.string.create_button) { _, _ ->
                val selectedSubject = subjects.getOrNull(subjectSpinner.selectedItemPosition)
                decksVm.createDeck(nameInput.text.toString(), selectedSubject)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    // Read the saved subject color, or fall back to a safe dark color when it is missing.
    private fun parseSubjectColor(colorHex: String?): Int {
        return try {
            if (colorHex.isNullOrBlank()) "#444444".toColorInt() else colorHex.toColorInt()
        } catch (_: Exception) {
            "#444444".toColorInt()
        }
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

    // Open the deck options screen for the chosen deck.
    private fun openDeckOptions(deckId: Int, deckName: String) {
        startActivity(
            Intent().setClassName(packageName, "$packageName.ui.DeckOptionsActivity")
                .putExtra("deck_id", deckId)
                .putExtra("deck_name", deckName)
        )
    }

    // Open the alter deck screen directly after creating a new deck.
    private fun openAlterDeck(deckId: Int) {
        startActivity(
            Intent().setClassName(packageName, "$packageName.ui.AlterDeckActivity")
                .putExtra("deck_id", deckId)
                .putExtra("deck_name", "New deck")
        )
    }
}

