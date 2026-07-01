package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.ExamViewModel

/*//////////////////////
Mock Exam Simulator (1.2): multiple-choice questions built from a deck's own cards.
Purely for assessment/cramming — never touches SM-2 scheduling or Review_Logs (see
ExamViewModel). Loaded once from onCreate (not onResume) so the in-progress session
survives a magnify-answer dialog or a rotation, same reasoning as ReviewDeckActivity.
 *//////////////////////
class ExamActivity : AppCompatActivity() {

    private lateinit var vm: ExamViewModel

    private lateinit var card: com.google.android.material.card.MaterialCardView
    private lateinit var questionGroup: View
    private lateinit var emptyGroup: View
    private lateinit var doneGroup: View

    private lateinit var progressText: android.widget.TextView
    private lateinit var questionText: android.widget.TextView
    private lateinit var optionBtns: List<MaterialButton>
    private lateinit var nextBtn: MaterialButton

    private lateinit var emptyText: android.widget.TextView
    private lateinit var scoreText: android.widget.TextView

    private var deckId: Int = -1
    private var deckName: String = "Deck"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)

        deckId = intent.getIntExtra("deck_id", -1)
        deckName = intent.getStringExtra("deck_name") ?: "Deck"

        vm = ViewModelProvider(this)[ExamViewModel::class.java]

        bindViews()
        setupClicks()
        setupWindowInsets()

        uws.ac.uk.studymate.util.OrbField.scatter(card, listOf(findViewById(R.id.examBackBtn)))
        uws.ac.uk.studymate.util.Entrance.play(card)

        vm.state.observe(this) { render(it) }
        vm.sessionExpired.observe(this) { if (it) openLogin() }

        vm.load(deckId, deckName)
    }

    private fun bindViews() {
        card = findViewById(R.id.examCard)
        questionGroup = findViewById(R.id.questionGroup)
        emptyGroup = findViewById(R.id.examEmptyGroup)
        doneGroup = findViewById(R.id.examDoneGroup)

        progressText = findViewById(R.id.examProgressText)
        questionText = findViewById(R.id.examQuestionText)
        optionBtns = listOf(
            findViewById(R.id.examOption1Btn),
            findViewById(R.id.examOption2Btn),
            findViewById(R.id.examOption3Btn)
        )
        nextBtn = findViewById(R.id.examNextBtn)

        emptyText = findViewById(R.id.examEmptyText)
        scoreText = findViewById(R.id.examScoreText)
    }

    // Edge-to-edge rule: pad the scroll content past the nav bar so the Next /
    // score buttons are never hidden behind it (same listener every screen uses).
    private fun setupWindowInsets() {
        val scrollView = findViewById<View>(R.id.examScrollView)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val navBar = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            view.setPadding(0, 0, 0, navBar + base)
            insets
        }
    }

    private fun setupClicks() {
        findViewById<MaterialButton>(R.id.examBackBtn).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.examRetryBtn).setOnClickListener { vm.restart() }
        nextBtn.setOnClickListener { vm.next() }

        optionBtns.forEachIndexed { index, btn ->
            btn.setOnClickListener { vm.answer(index) }
            btn.setOnLongClickListener {
                showMagnified(btn.text?.toString().orEmpty())
                true
            }
        }
    }

    private fun showMagnified(text: String) {
        if (text.isBlank()) return
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setMessage(text)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun render(state: ExamViewModel.State) {
        questionGroup.visibility = View.GONE
        emptyGroup.visibility = View.GONE
        doneGroup.visibility = View.GONE

        when (state) {
            is ExamViewModel.State.Loading -> { /* nothing visible yet */ }

            is ExamViewModel.State.Empty -> {
                emptyGroup.visibility = View.VISIBLE
                emptyText.text = getString(R.string.exam_not_enough_cards_message, EXAM_MIN_CARDS)
            }

            is ExamViewModel.State.Question -> {
                questionGroup.visibility = View.VISIBLE
                progressText.text = getString(R.string.exam_progress_format, state.index + 1, state.total)
                questionText.text = state.prompt
                questionText.contentDescription = getString(
                    R.string.cd_exam_question, state.index + 1, state.total, state.prompt
                )

                optionBtns.forEachIndexed { i, btn ->
                    val optionText = state.options.getOrNull(i)
                    if (optionText == null) {
                        btn.visibility = View.GONE
                        return@forEachIndexed
                    }
                    btn.visibility = View.VISIBLE
                    btn.contentDescription = getString(R.string.cd_exam_option, optionText)

                    if (!state.revealed) {
                        btn.text = optionText
                        btn.setTextColor(getColor(R.color.gold_light))
                        btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.gold_light))
                    } else {
                        // Match the ViewModel's text-based grading: any option whose
                        // text equals the correct answer's text shows as correct, so a
                        // duplicated answer text never displays as "wrong".
                        val correctText = state.options[state.correctIndex]
                        val isCorrect = optionText == correctText
                        val isChosenWrong = i == state.selectedIndex && !isCorrect
                        val colorRes: Int
                        when {
                            isCorrect -> {
                                btn.text = getString(R.string.exam_option_correct_prefix, optionText)
                                colorRes = R.color.success_text
                            }
                            isChosenWrong -> {
                                btn.text = getString(R.string.exam_option_wrong_prefix, optionText)
                                colorRes = R.color.error_text
                            }
                            else -> {
                                btn.text = optionText
                                colorRes = R.color.gold_light
                            }
                        }
                        btn.setTextColor(getColor(colorRes))
                        btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(colorRes))
                    }
                    btn.isEnabled = !state.revealed
                }

                nextBtn.isEnabled = state.revealed
                nextBtn.alpha = if (state.revealed) 1f else 0.45f
            }

            is ExamViewModel.State.Done -> {
                doneGroup.visibility = View.VISIBLE
                val pct = if (state.total == 0) 0 else (state.correctCount * 100) / state.total
                scoreText.text = getString(R.string.exam_score_message, state.correctCount, state.total, pct)
            }
        }
    }

    private fun openLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    companion object {
        private const val EXAM_MIN_CARDS = uws.ac.uk.studymate.util.ExamGenerator.MIN_CARDS
    }
}
