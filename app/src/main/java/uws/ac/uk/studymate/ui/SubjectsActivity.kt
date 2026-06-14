package uws.ac.uk.studymate.ui

import uws.ac.uk.studymate.util.ColorUtils

import android.animation.ObjectAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.relations.SubjectWithAssignments
import uws.ac.uk.studymate.ui.viewmodels.SubjectColorChoice
import uws.ac.uk.studymate.ui.viewmodels.SubjectsSummary
import uws.ac.uk.studymate.ui.viewmodels.SubjectsViewModel

/*//////////////////////
Coded by Jamie Coleman
06/04/26
redesigned 18/04/26 — wood-glass UI, RecyclerView, inline add/edit panel-swap
 *//////////////////////
class SubjectsActivity : AppCompatActivity() {

    private lateinit var subjectsVm: SubjectsViewModel

    // Top-level views
    private lateinit var subjectsCard: MaterialCardView
    private lateinit var listPanel: LinearLayout
    private lateinit var addPanel: View
    private lateinit var editPanel: View

    // List panel
    private lateinit var subjectsRecycler: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var createSubjectBtn: MaterialButton

    // Add panel
    private lateinit var addNameInput: TextInputEditText
    private lateinit var addColorRow: LinearLayout
    private lateinit var addConfirmBtn: MaterialButton
    private lateinit var addCancelBtn: MaterialButton

    // Edit panel
    private lateinit var editNameInput: TextInputEditText
    private lateinit var editColorRow: LinearLayout
    private lateinit var editConfirmBtn: MaterialButton
    private lateinit var editCancelBtn: MaterialButton

    private lateinit var adapter: SubjectListAdapter

    private var colorChoices: List<SubjectColorChoice> = emptyList()
    private var addSelectedColor: SubjectColorChoice? = null
    private var editSelectedColor: SubjectColorChoice? = null
    private var editingSubject: Subject? = null

    private enum class Panel { LIST, ADD, EDIT }
    private var currentPanel: Panel = Panel.LIST
    private var isAnimating = false

    // Each pair is (view, direction) — direction is the side this element travels
    // along (-1f = left axis, +1f = right axis). The same direction is reused on
    // entry and exit; the sign is flipped when going "backwards" so the element
    // returns the way it came. Alternate -1 / +1 down the list for the dance.
    private lateinit var listElems: List<Pair<View, Float>>
    private lateinit var addElems: List<Pair<View, Float>>
    private lateinit var editElems: List<Pair<View, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subjects)
        uws.ac.uk.studymate.util.KeyboardInsets.apply(this)

        subjectsVm = ViewModelProvider(this)[SubjectsViewModel::class.java]

        bindViews()
        setupRecycler()
        setupClicks()
        setupBackHandler()
        setupWindowInsets()
        setupFloatingOrbs()
        runEntranceAnimation()

        // Observe ViewModel
        subjectsVm.screenSummary.observe(this) { summary ->
            applySummary(summary)
        }
        subjectsVm.sessionExpired.observe(this) { expired ->
            if (expired) openLogin()
        }
        subjectsVm.message.observe(this) { msg ->
            if (msg.isNullOrBlank()) return@observe
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            if (msg == "Subject added" || msg == "Subject updated") {
                swapToPanel(Panel.LIST)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        subjectsVm.loadScreen()
    }

    private fun bindViews() {
        subjectsCard = findViewById(R.id.subjectsCard)
        listPanel = findViewById(R.id.listPanel)
        addPanel = findViewById(R.id.addPanel)
        editPanel = findViewById(R.id.editPanel)

        subjectsRecycler = findViewById(R.id.subjectsRecycler)
        emptyStateText = findViewById(R.id.emptyStateText)
        createSubjectBtn = findViewById(R.id.createSubjectBtn)

        addNameInput = findViewById(R.id.addSubjectNameInput)
        addColorRow = findViewById(R.id.addColorRow)
        addConfirmBtn = findViewById(R.id.addConfirmBtn)
        addCancelBtn = findViewById(R.id.addCancelBtn)

        editNameInput = findViewById(R.id.editSubjectNameInput)
        editColorRow = findViewById(R.id.editColorRow)
        editConfirmBtn = findViewById(R.id.editConfirmBtn)
        editCancelBtn = findViewById(R.id.editCancelBtn)

        listElems = listOf(
            findViewById<View>(R.id.screenTitleText)   to -1f,
            findViewById<View>(R.id.screenSubText)     to  1f,
            createSubjectBtn                            to -1f,
            findViewById<View>(R.id.listSectionLabel)  to  1f,
            subjectsRecycler                            to -1f,
            emptyStateText                              to  1f
        )
        addElems = listOf(
            findViewById<View>(R.id.addTitleText)         to -1f,
            findViewById<View>(R.id.addSubText)           to  1f,
            findViewById<View>(R.id.addSubjectNameLayout) to -1f,
            findViewById<View>(R.id.addColorLabel)        to  1f,
            addColorRow                                    to -1f,
            addConfirmBtn                                  to  1f,
            addCancelBtn                                   to -1f
        )
        editElems = listOf(
            findViewById<View>(R.id.editTitleText)         to -1f,
            findViewById<View>(R.id.editSubText)           to  1f,
            findViewById<View>(R.id.editSubjectNameLayout) to -1f,
            findViewById<View>(R.id.editColorLabel)        to  1f,
            editColorRow                                    to -1f,
            editConfirmBtn                                  to  1f,
            editCancelBtn                                   to -1f
        )
    }

    private fun setupRecycler() {
        adapter = SubjectListAdapter(
            items = emptyList(),
            onEdit = { item -> openEditFor(item) },
            onDelete = { item -> confirmDelete(item) }
        )
        subjectsRecycler.layoutManager = LinearLayoutManager(this)
        subjectsRecycler.adapter = adapter
    }

    private fun setupClicks() {
        findViewById<MaterialButton>(R.id.homeBtn).setOnClickListener {
            openHome()
        }

        createSubjectBtn.setOnClickListener {
            openAddPanel()
        }

        addCancelBtn.setOnClickListener { swapToPanel(Panel.LIST) }
        editCancelBtn.setOnClickListener { swapToPanel(Panel.LIST) }

        addConfirmBtn.setOnClickListener {
            subjectsVm.addSubject(
                name = addNameInput.text?.toString().orEmpty(),
                colorChoice = addSelectedColor
            )
        }

        editConfirmBtn.setOnClickListener {
            val original = editingSubject ?: return@setOnClickListener
            subjectsVm.updateSubject(
                original = original,
                newName = editNameInput.text?.toString().orEmpty(),
                colorChoice = editSelectedColor
            )
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentPanel) {
                    Panel.LIST -> openHome()
                    Panel.ADD, Panel.EDIT -> swapToPanel(Panel.LIST)
                }
            }
        })
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(subjectsCard) { _, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            // Apply to the inner panels so content clears the nav bar.
            listPanel.setPadding(
                listPanel.paddingLeft,
                listPanel.paddingTop,
                listPanel.paddingRight,
                navBar + base
            )
            addPanel.setPadding(0, 0, 0, navBar + base)
            editPanel.setPadding(0, 0, 0, navBar + base)
            insets
        }
    }

    private fun applySummary(summary: SubjectsSummary) {
        colorChoices = summary.colorChoices
        adapter.submit(summary.subjectsWithAssignments)

        val isEmpty = summary.subjectsWithAssignments.isEmpty()
        emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        subjectsRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE

        // Build color swatches once we know the options.
        if (addColorRow.childCount == 0) {
            buildColorSwatches(addColorRow, isEditPanel = false)
            buildColorSwatches(editColorRow, isEditPanel = true)
        }
    }

    private fun buildColorSwatches(row: LinearLayout, isEditPanel: Boolean) {
        row.removeAllViews()
        val density = resources.displayMetrics.density
        val size = (36 * density).toInt()
        val margin = (8 * density).toInt()

        colorChoices.forEach { choice ->
            val swatch = View(this)
            val lp = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = margin
            }
            swatch.layoutParams = lp
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(parseHex(choice.hex))
                setStroke((2 * density).toInt(), Color.parseColor("#66FAF8F5"))
            }
            swatch.background = bg
            swatch.tag = choice
            swatch.setOnClickListener {
                if (isEditPanel) {
                    editSelectedColor = choice
                    highlightSelected(editColorRow, choice)
                } else {
                    addSelectedColor = choice
                    highlightSelected(addColorRow, choice)
                }
            }
            row.addView(swatch)
        }
    }

    private fun highlightSelected(row: LinearLayout, selected: SubjectColorChoice) {
        val density = resources.displayMetrics.density
        for (i in 0 until row.childCount) {
            val view = row.getChildAt(i)
            val choice = view.tag as? SubjectColorChoice ?: continue
            val bg = view.background as? GradientDrawable ?: continue
            if (choice.hex == selected.hex) {
                bg.setStroke((3 * density).toInt(), Color.parseColor("#FFC4A24A"))
            } else {
                bg.setStroke((2 * density).toInt(), Color.parseColor("#66FAF8F5"))
            }
        }
    }

    private fun openAddPanel() {
        addNameInput.setText("")
        addSelectedColor = colorChoices.firstOrNull()
        addSelectedColor?.let { highlightSelected(addColorRow, it) }
        swapToPanel(Panel.ADD)
    }

    private fun openEditFor(item: SubjectWithAssignments) {
        editingSubject = item.subject
        editNameInput.setText(item.subject.name)
        editSelectedColor = colorChoices.firstOrNull { it.hex.equals(item.subject.color, true) }
            ?: colorChoices.firstOrNull()
        editSelectedColor?.let { highlightSelected(editColorRow, it) }
        swapToPanel(Panel.EDIT)
    }

    private fun confirmDelete(item: SubjectWithAssignments) {
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Delete subject")
            .setMessage("This will delete \"${item.subject.name}\" and its ${item.assignments.size} assignment(s). Continue?")
            .setPositiveButton("Delete") { _, _ -> subjectsVm.deleteSubject(item.subject) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun swapToPanel(target: Panel) {
        if (isAnimating || target == currentPanel) return

        val outgoingPanel = panelView(currentPanel)
        val incomingPanel = panelView(target)
        val outgoingElems = panelElems(currentPanel)
        val incomingElems = panelElems(target)

        // Direction sign: LIST is the "home" panel. Going from LIST → sub-panel
        // is "forward" (+1); coming back to LIST is "backward" (-1).
        val goingForward = currentPanel == Panel.LIST
        val sign = if (goingForward) 1f else -1f

        val w = outgoingPanel.width.toFloat()
        val stagger = 72L
        val exitDur = 420L
        val enterDur = 440L
        val enterStart = (outgoingElems.size - 1) * stagger + exitDur
        val exitEase = AccelerateInterpolator(1.3f)
        val enterEase = DecelerateInterpolator(1.3f)

        isAnimating = true

        // Snap incoming elements to their entry positions before the panel becomes
        // visible so no half-frame at translationX=0 leaks through.
        incomingElems.forEach { (v, dir) -> v.translationX = w * dir * sign }
        incomingPanel.visibility = View.VISIBLE

        outgoingElems.forEachIndexed { i, (v, dir) ->
            v.animate()
                .translationX(w * dir * sign)
                .setDuration(exitDur)
                .setStartDelay(i * stagger)
                .setInterpolator(exitEase)
                .start()
        }

        incomingElems.forEachIndexed { i, (v, _) ->
            v.animate()
                .translationX(0f)
                .setDuration(enterDur)
                .setStartDelay(enterStart + i * stagger)
                .setInterpolator(enterEase)
                .start()
        }

        // Hide outgoing once its last element has cleared, and reset its element
        // translations so the next swap starts from 0 again.
        val hideDelay = (outgoingElems.size - 1) * stagger + exitDur + 50L
        outgoingPanel.postDelayed({
            outgoingPanel.visibility = View.INVISIBLE
            outgoingElems.forEach { (v, _) -> v.translationX = 0f }
            isAnimating = false
        }, hideDelay)

        currentPanel = target
    }

    private fun panelView(panel: Panel): View = when (panel) {
        Panel.LIST -> listPanel
        Panel.ADD -> addPanel
        Panel.EDIT -> editPanel
    }

    private fun panelElems(panel: Panel): List<Pair<View, Float>> = when (panel) {
        Panel.LIST -> listElems
        Panel.ADD -> addElems
        Panel.EDIT -> editElems
    }

    private fun setupFloatingOrbs() {
        floatOrb(findViewById(R.id.orb1), 14f, 3800L, 0L)
        floatOrb(findViewById(R.id.orb2), 17f, 4200L, 500L)
        floatOrb(findViewById(R.id.orb3), 12f, 3600L, 1000L)
        floatOrb(findViewById(R.id.orb4), 15f, 4000L, 300L)
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

    private fun runEntranceAnimation() {
        val density = resources.displayMetrics.density
        subjectsCard.translationY = 200f * density
        subjectsCard.alpha = 0f
        subjectsCard.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        subjectsCard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .setStartDelay(60)
            .withEndAction { subjectsCard.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()
    }

    private fun parseHex(hex: String): Int = ColorUtils.parseOrDefault(hex)

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun openHome() {
        // Return to the existing Dashboard instead of launching a new one, so the
        // back stack stays a clean Dashboard -> screen -> sub-screen hierarchy.
        finish()
    }
}
