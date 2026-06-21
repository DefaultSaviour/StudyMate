package uws.ac.uk.studymate.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.FocusTimerViewModel
import uws.ac.uk.studymate.util.FocusTimerEngine
import uws.ac.uk.studymate.util.FocusTimerEngine.Config
import uws.ac.uk.studymate.util.FocusTimerEngine.Phase

/*//////////////////////
Focus / Pomodoro timer screen (0.9G).

Wood-glass screen that runs a focus ⇄ break Pomodoro session. The timer logic
lives in FocusTimerViewModel (timestamp-based, survives rotation / leave-return);
this Activity only renders state, wires the controls, buzzes on phase changes and
collects a custom config.
 *//////////////////////
class FocusTimerActivity : AppCompatActivity() {

    private lateinit var vm: FocusTimerViewModel

    private lateinit var card: MaterialCardView
    private lateinit var phaseLabel: TextView
    private lateinit var roundText: TextView
    private lateinit var countdownText: TextView
    private lateinit var countdownGlow: PulseRingView
    private lateinit var startPauseBtn: MaterialButton
    private lateinit var skipBtn: MaterialButton
    private lateinit var endBtn: MaterialButton
    private lateinit var studyBox: TextView
    private lateinit var breakBox: TextView
    private lateinit var roundsBox: TextView

    private var lastConfig: Config = Config(
        FocusTimerViewModel.DEFAULT_FOCUS,
        FocusTimerViewModel.DEFAULT_BREAK,
        FocusTimerViewModel.DEFAULT_ROUNDS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_timer)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)

        vm = ViewModelProvider(this)[FocusTimerViewModel::class.java]

        bindViews()
        setupClicks()
        setupWindowInsets()

        uws.ac.uk.studymate.util.OrbField.scatter(
            findViewById(R.id.focusCard),
            listOf(findViewById(R.id.backBtn))
        )

        // Entrance: card slides up.
        val d = resources.displayMetrics.density
        card.translationY = 200f * d
        card.alpha = 0f
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        card.animate().translationY(0f).alpha(1f).setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f)).setStartDelay(60)
            .withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()

        vm.config.observe(this) { cfg ->
            lastConfig = cfg
            studyBox.text = cfg.focusMinutes.toString()
            breakBox.text = cfg.breakMinutes.toString()
            roundsBox.text = cfg.rounds.toString()
        }
        vm.state.observe(this) { render(it) }
        vm.phaseEvent.observe(this) { phase ->
            if (phase == null) return@observe
            buzz()
            if (phase == Phase.DONE) {
                Toast.makeText(this, R.string.focus_timer_session_done, Toast.LENGTH_LONG).show()
            }
            vm.consumePhaseEvent()
        }
    }

    private fun bindViews() {
        card = findViewById(R.id.focusCard)
        phaseLabel = findViewById(R.id.phaseLabel)
        roundText = findViewById(R.id.roundText)
        countdownText = findViewById(R.id.countdownText)
        countdownGlow = findViewById(R.id.countdownGlow)
        startPauseBtn = findViewById(R.id.startPauseBtn)
        skipBtn = findViewById(R.id.skipBtn)
        endBtn = findViewById(R.id.endBtn)
        studyBox = findViewById(R.id.studyBox)
        breakBox = findViewById(R.id.breakBox)
        roundsBox = findViewById(R.id.roundsBox)
    }

    private fun setupClicks() {
        findViewById<MaterialButton>(R.id.backBtn).setOnClickListener { finish() }
        startPauseBtn.setOnClickListener {
            val running = vm.state.value?.running == true
            if (running) vm.pause() else vm.start()
        }
        skipBtn.setOnClickListener { vm.skipPhase() }
        endBtn.setOnClickListener { vm.reset() }
        studyBox.setOnClickListener { editStudy() }
        breakBox.setOnClickListener { editBreak() }
        roundsBox.setOnClickListener { editRounds() }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(card) { _, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            findViewById<View>(R.id.focusScrollView).setPadding(0, 0, 0, navBar + base)
            insets
        }
    }

    private fun render(state: FocusTimerEngine.TimerState) {
        countdownText.text = formatTime(state.remainingSeconds)

        phaseLabel.text = when (state.phase) {
            Phase.FOCUS -> getString(R.string.focus_phase_focus)
            Phase.BREAK -> getString(R.string.focus_phase_break)
            Phase.DONE -> getString(R.string.focus_phase_done)
        }

        roundText.text = if (state.phase == Phase.DONE) {
            getString(R.string.focus_session_complete)
        } else {
            getString(R.string.focus_round_format, state.round, lastConfig.rounds)
        }

        // Start / Pause / Resume / Start again
        startPauseBtn.text = when {
            state.running -> getString(R.string.focus_pause)
            state.phase == Phase.DONE -> getString(R.string.focus_start_again)
            state.remainingSeconds < FocusTimerEngine.phaseDurationSeconds(state.phase, lastConfig) ->
                getString(R.string.focus_resume)
            else -> getString(R.string.focus_start)
        }

        // "End round" during focus, "End break" during a break.
        skipBtn.text = getString(
            if (state.phase == Phase.BREAK) R.string.focus_end_break else R.string.focus_end_round
        )
        // "End round/break" and "End" are only available once the session is
        // actually under way — not on the fresh idle screen, and not when finished.
        val atIdleStart = !state.running &&
            state.phase == Phase.FOCUS &&
            state.round == 1 &&
            state.remainingSeconds >= FocusTimerEngine.phaseDurationSeconds(Phase.FOCUS, lastConfig)
        val started = state.phase != Phase.DONE && !atIdleStart
        skipBtn.isEnabled = started
        skipBtn.alpha = if (started) 1f else 0.45f
        endBtn.isEnabled = started
        endBtn.alpha = if (started) 1f else 0.45f

        // Config boxes can only change when the session isn't in progress — i.e.
        // at the idle start screen or once it's finished, never while running OR paused.
        val configEnabled = !started
        listOf(studyBox, breakBox, roundsBox).forEach {
            it.isEnabled = configEnabled
            it.isClickable = configEnabled
            it.alpha = if (configEnabled) 1f else 0.45f
        }

        if (state.running) {
            countdownGlow.visibility = View.VISIBLE
            countdownGlow.startAnimating()
        } else {
            countdownGlow.stopAnimating()
            countdownGlow.visibility = View.GONE
        }
    }

    // Tap a config box → edit just that value, keeping the other two.
    private fun editStudy() = showValueDialog(R.string.focus_edit_study, lastConfig.focusMinutes) { v ->
        vm.applyConfig(v, lastConfig.breakMinutes, lastConfig.rounds)
    }

    private fun editBreak() = showValueDialog(R.string.focus_edit_break, lastConfig.breakMinutes) { v ->
        vm.applyConfig(lastConfig.focusMinutes, v, lastConfig.rounds)
    }

    private fun editRounds() = showValueDialog(R.string.focus_edit_rounds, lastConfig.rounds) { v ->
        vm.applyConfig(lastConfig.focusMinutes, lastConfig.breakMinutes, v)
    }

    private fun showValueDialog(titleRes: Int, current: Int, onSet: (Int) -> Unit) {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
            setTextColor(getColor(R.color.surface))
            setHintTextColor(getColor(R.color.gold_light))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle(titleRes)
            .setView(container)
            .setPositiveButton(R.string.focus_edit_apply) { _, _ ->
                input.text.toString().toIntOrNull()?.let { onSet(it) }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun formatTime(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    private fun buzz() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
