package uws.ac.uk.studymate.ui

import uws.ac.uk.studymate.util.ColorUtils

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.ui.viewmodels.DeckListItem
import uws.ac.uk.studymate.ui.viewmodels.FlashcardDecksSummary
import uws.ac.uk.studymate.ui.viewmodels.FlashcardDecksViewModel
import uws.ac.uk.studymate.util.AssignmentIcons

/*//////////////////////
Coded by Jamie Coleman
17/04/26
redesigned 18/04/26 — wood-glass UI, RecyclerView, inline add/edit panel-swap
 *//////////////////////
class FlashcardDecksActivity : AppCompatActivity() {

    private lateinit var vm: FlashcardDecksViewModel

    private lateinit var card: MaterialCardView
    private lateinit var listPanel: LinearLayout
    private lateinit var addPanel: View
    private lateinit var editPanel: View

    private lateinit var recycler: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var createDeckBtn: MaterialButton

    private lateinit var addNameInput: TextInputEditText
    private lateinit var addSubjectRow: LinearLayout
    private lateinit var addConfirmBtn: MaterialButton
    private lateinit var addCancelBtn: MaterialButton

    // Progressive-glow guidance on the New-deck panel: name → subject → save.
    private lateinit var addSubjectGlowWrap: View
    private lateinit var addNameGlow: PulseRingView
    private lateinit var addSubjectGlow: PulseRingView
    private lateinit var addSaveGlow: PulseRingView
    private val addGlows: List<PulseRingView> by lazy {
        listOf(addNameGlow, addSubjectGlow, addSaveGlow)
    }
    private var assignmentStepUnlocked = false

    private lateinit var editNameInput: TextInputEditText
    private lateinit var editSubjectRow: LinearLayout
    private lateinit var editConfirmBtn: MaterialButton
    private lateinit var editCancelBtn: MaterialButton

    private lateinit var adapter: DeckListAdapter

    private var assignments: List<Assignment> = emptyList()
    private var addAssignment: Assignment? = null
    private var editAssignment: Assignment? = null
    private var editingDeck: FlashcardDeck? = null

    // When opened from a calendar assignment the screen is scoped to that one
    // assignment: only its decks are listed and a new deck pre-picks it.
    private var scopedAssignmentId: Int = -1
    private var scopedAssignmentName: String? = null

    private enum class Panel { LIST, ADD, EDIT }
    private var currentPanel: Panel = Panel.LIST
    private var isAnimating = false

    private lateinit var listElems: List<Pair<View, Float>>
    private lateinit var addElems: List<Pair<View, Float>>
    private lateinit var editElems: List<Pair<View, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard_decks)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)
        uws.ac.uk.studymate.util.KeyboardInsets.apply(this)

        scopedAssignmentId = intent.getIntExtra("scoped_assignment_id", -1)
        scopedAssignmentName = intent.getStringExtra("scoped_assignment_name")

        vm = ViewModelProvider(this)[FlashcardDecksViewModel::class.java]

        bindViews()
        applyScopedHeader()
        setupRecycler()
        setupClicks()
        setupBackHandler()
        setupWindowInsets()
        setupFloatingOrbs()
        runEntranceAnimation()

        vm.screenSummary.observe(this) { applySummary(it) }
        vm.sessionExpired.observe(this) { if (it) openLogin() }
        vm.message.observe(this) { msg ->
            if (msg.isNullOrBlank()) return@observe
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            if (msg == "Deck updated") swapToPanel(Panel.LIST)
            // "Deck created" is handled via createdDeckId observer below.
        }
        vm.createdDeckId.observe(this) { deckId ->
            if (deckId != null) {
                vm.clearCreatedDeckId()
                swapToPanel(Panel.LIST)
                openAlterDeck(deckId)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.loadScreen(scopedAssignmentId.takeIf { it > 0 })
    }

    // In scoped mode, retitle the header to the assignment so it reads as "that
    // assignment's decks" rather than the whole library.
    private fun applyScopedHeader() {
        val name = scopedAssignmentName
        if (scopedAssignmentId <= 0 || name.isNullOrBlank()) return
        findViewById<TextView>(R.id.listTitle).text = name
        findViewById<TextView>(R.id.listSubText).text = "Decks for this assignment"
    }

    private fun bindViews() {
        card = findViewById(R.id.decksCard)
        listPanel = findViewById(R.id.listPanel)
        addPanel = findViewById(R.id.addPanel)
        editPanel = findViewById(R.id.editPanel)

        recycler = findViewById(R.id.decksRecycler)
        emptyText = findViewById(R.id.emptyStateText)
        createDeckBtn = findViewById(R.id.createDeckBtn)

        addNameInput = findViewById(R.id.addDeckNameInput)
        addSubjectRow = findViewById(R.id.addSubjectRow)
        addConfirmBtn = findViewById(R.id.addConfirmBtn)
        addCancelBtn = findViewById(R.id.addCancelBtn)

        addSubjectGlowWrap = findViewById(R.id.addSubjectGlowWrap)
        addNameGlow = findViewById(R.id.addNameGlow)
        addSubjectGlow = findViewById(R.id.addSubjectGlow)
        addSaveGlow = findViewById(R.id.addSaveGlow)
        val r12 = 12f * resources.displayMetrics.density
        findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.addDeckNameLayout)
            .setBoxCornerRadii(r12, r12, r12, r12)

        editNameInput = findViewById(R.id.editDeckNameInput)
        editSubjectRow = findViewById(R.id.editSubjectRow)
        editConfirmBtn = findViewById(R.id.editConfirmBtn)
        editCancelBtn = findViewById(R.id.editCancelBtn)

        listElems = listOf(
            findViewById<View>(R.id.listTitle)        to -1f,
            findViewById<View>(R.id.listSubText)      to  1f,
            createDeckBtn                              to -1f,
            findViewById<View>(R.id.listSectionLabel) to  1f,
            recycler                                   to -1f,
            emptyText                                  to  1f
        )
        addElems = listOf(
            findViewById<View>(R.id.addTitleText)        to -1f,
            findViewById<View>(R.id.addSubText)          to  1f,
            findViewById<View>(R.id.addNameGlowWrap)     to -1f,
            findViewById<View>(R.id.addSubjectLabel)     to  1f,
            findViewById<View>(R.id.addSubjectGlowWrap)  to -1f,
            findViewById<View>(R.id.addSaveGlowWrap)     to  1f,
            addCancelBtn                                  to -1f
        )
        editElems = listOf(
            findViewById<View>(R.id.editTitleText)       to -1f,
            findViewById<View>(R.id.editSubText)         to  1f,
            findViewById<View>(R.id.editDeckNameLayout)  to -1f,
            findViewById<View>(R.id.editSubjectLabel)    to  1f,
            findViewById<View>(R.id.editSubjectScroll)   to -1f,
            editConfirmBtn                                to  1f,
            editCancelBtn                                 to -1f
        )
    }

    private fun setupRecycler() {
        adapter = DeckListAdapter(
            rows = emptyList(),
            onTap = { openDeckOptions(it.deck.id, it.deck.name) },
            onEdit = { openEditFor(it) },
            onDelete = { confirmDelete(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    private fun setupClicks() {
        findViewById<MaterialButton>(R.id.homeBtn).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        createDeckBtn.setOnClickListener { openAddPanel() }
        addCancelBtn.setOnClickListener { swapToPanel(Panel.LIST) }
        editCancelBtn.setOnClickListener { swapToPanel(Panel.LIST) }

        addConfirmBtn.setOnClickListener {
            if (assignments.isEmpty()) {
                Toast.makeText(this, "Add an assignment before creating a deck", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.createDeck(addNameInput.text?.toString().orEmpty(), addAssignment)
        }
        editConfirmBtn.setOnClickListener {
            val original = editingDeck ?: return@setOnClickListener
            vm.updateDeck(original, editNameInput.text?.toString().orEmpty(), editAssignment)
        }

        // Recompute the New-deck progressive glow as the name is typed.
        addNameInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateAddProgress()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentPanel) {
                    Panel.LIST -> finish()
                    Panel.ADD, Panel.EDIT -> swapToPanel(Panel.LIST)
                }
            }
        })
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(card) { _, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            listPanel.setPadding(
                listPanel.paddingLeft, listPanel.paddingTop,
                listPanel.paddingRight, navBar + base
            )
            addPanel.setPadding(0, 0, 0, navBar + base)
            editPanel.setPadding(0, 0, 0, navBar + base)
            insets
        }
    }

    // ─────────────────── Data ───────────────────

    private fun applySummary(summary: FlashcardDecksSummary) {
        val assignmentsChanged = assignments != summary.assignments
        assignments = summary.assignments

        // Active decks first; completed-assignment decks go under their own header
        // (shown only when there's at least one).
        val active = summary.items.filter { !it.isCompleted }
        val completed = summary.items.filter { it.isCompleted }
        val rows = buildList {
            active.forEach { add(DeckRow.Deck(it)) }
            if (completed.isNotEmpty()) {
                add(DeckRow.Header("COMPLETED ASSIGNMENTS"))
                completed.forEach { add(DeckRow.Deck(it)) }
            }
        }
        adapter.submitRows(rows)

        val isEmpty = summary.items.isEmpty()
        emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (addSubjectRow.childCount == 0 || assignmentsChanged) {
            buildAssignmentSwatches(addSubjectRow) { tapped ->
                if (!assignmentStepUnlocked) return@buildAssignmentSwatches
                uws.ac.uk.studymate.util.Keyboard.hide(this)
                addAssignment = tapped
                highlightSelectedAssignment(addSubjectRow, tapped)
                updateAddProgress()
            }
            buildAssignmentSwatches(editSubjectRow) { tapped ->
                uws.ac.uk.studymate.util.Keyboard.hide(this)
                editAssignment = tapped
                highlightSelectedAssignment(editSubjectRow, tapped)
            }
        }
    }

    // ─────────────────── Assignment swatches ───────────────────
    // (Decks file under an assignment; the picker shows the user's assignments,
    //  each as a colour dot + name — the swatch row view ids are still the old
    //  *Subject* ids in the layout, kept to avoid churn.)

    private fun buildAssignmentSwatches(row: LinearLayout, onTap: (Assignment) -> Unit) {
        row.removeAllViews()
        val density = resources.displayMetrics.density
        val container = (62 * density).toInt()
        val dot = (40 * density).toInt()
        val margin = (8 * density).toInt()

        assignments.forEach { assignment ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(container, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = margin
                }
                tag = assignment
                isClickable = true
                isFocusable = true
                setOnClickListener { onTap(assignment) }
            }

            // Show the assignment's own icon tinted with its colour (instead of a
            // plain coloured circle) so the picker reads as that assignment at a
            // glance. The badge keeps a GradientDrawable background so the selection
            // highlight (a gold stroke) still works via getChildAt(0).
            val swatch = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dot, dot)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#33000000"))
                    setStroke((2 * density).toInt(), Color.parseColor("#66FAF8F5"))
                }
                addView(
                    ImageView(this@FlashcardDecksActivity).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            (24 * density).toInt(), (24 * density).toInt(), Gravity.CENTER
                        )
                        setImageResource(AssignmentIcons.drawableForKey(assignment.icon))
                        setColorFilter(parseSubjectColor(assignment.color))
                    }
                )
            }

            val label = TextView(this).apply {
                text = assignment.title
                textSize = 11f
                setTextColor(Color.parseColor("#D4BC7E"))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    container, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (4 * density).toInt() }
            }

            item.addView(swatch)
            item.addView(label)
            row.addView(item)
        }
    }

    private fun highlightSelectedAssignment(row: LinearLayout, selected: Assignment) {
        val density = resources.displayMetrics.density
        for (i in 0 until row.childCount) {
            val item = row.getChildAt(i) as? LinearLayout ?: continue
            val assignment = item.tag as? Assignment ?: continue
            val swatch = item.getChildAt(0)
            val bg = swatch.background as? GradientDrawable ?: continue
            if (assignment.id == selected.id) {
                bg.setStroke((3 * density).toInt(), Color.parseColor("#FFC4A24A"))
            } else {
                bg.setStroke((2 * density).toInt(), Color.parseColor("#66FAF8F5"))
            }
        }
    }

    // ─────────────────── Open add / edit / delete ───────────────────

    private fun openAddPanel() {
        if (assignments.isEmpty()) {
            Toast.makeText(this, "Add an assignment before creating a deck", Toast.LENGTH_SHORT).show()
            return
        }
        addNameInput.setText("")
        // Scoped mode pre-picks its assignment; otherwise the user must choose one
        // (it's the second step).
        addAssignment = scopedAssignmentId.takeIf { it > 0 }
            ?.let { id -> assignments.firstOrNull { it.id == id } }
        val preselected = addAssignment
        if (preselected != null) {
            highlightSelectedAssignment(addSubjectRow, preselected)
        } else {
            clearSubjectHighlight(addSubjectRow)
        }
        updateAddProgress()
        swapToPanel(Panel.ADD)
    }

    private fun clearSubjectHighlight(row: LinearLayout) {
        val density = resources.displayMetrics.density
        for (i in 0 until row.childCount) {
            val item = row.getChildAt(i) as? LinearLayout ?: continue
            val bg = item.getChildAt(0)?.background as? GradientDrawable ?: continue
            bg.setStroke((2 * density).toInt(), Color.parseColor("#66FAF8F5"))
        }
    }

    // Progressive guidance on the New-deck panel: name → subject → save. Each step
    // unlocks only once the prior one is done, and the next-required field gets the
    // breathing gold glow. Cancel and the name field are always available.
    private fun updateAddProgress() {
        val hasName = !addNameInput.text.isNullOrBlank()
        val hasAssignment = addAssignment != null

        assignmentStepUnlocked = hasName
        addSubjectGlowWrap.alpha = if (hasName) 1f else 0.45f

        addConfirmBtn.isEnabled = hasName && hasAssignment
        addConfirmBtn.alpha = if (addConfirmBtn.isEnabled) 1f else 0.45f

        val active = when {
            !hasName -> addNameGlow
            !hasAssignment -> addSubjectGlow
            else -> addSaveGlow
        }
        setActiveGlow(active)
    }

    private fun setActiveGlow(active: PulseRingView?) {
        addGlows.forEach { glow ->
            if (glow === active) {
                glow.visibility = View.VISIBLE
                glow.startAnimating()
            } else {
                glow.stopAnimating()
                glow.visibility = View.GONE
            }
        }
    }

    private fun stopAllAddGlows() = setActiveGlow(null)

    private fun openEditFor(item: DeckListItem) {
        editingDeck = item.deck
        editNameInput.setText(item.deck.name)
        editAssignment = assignments.firstOrNull { it.id == item.deck.assignmentId }
            ?: assignments.firstOrNull()
        editAssignment?.let { highlightSelectedAssignment(editSubjectRow, it) }
        swapToPanel(Panel.EDIT)
    }

    private fun confirmDelete(item: DeckListItem) {
        val cardWord = if (item.cardCount == 1) "card" else "cards"
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Delete deck")
            .setMessage("This will delete \"${item.deck.name}\" and its ${item.cardCount} $cardWord. Continue?")
            .setPositiveButton("Delete") { _, _ -> vm.deleteDeck(item.deck) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─────────────────── Panel swap ───────────────────

    private fun swapToPanel(target: Panel) {
        if (isAnimating || target == currentPanel) return

        // Leaving a panel (often a text-entry one) — make sure the keyboard goes too.
        uws.ac.uk.studymate.util.Keyboard.hide(this)

        val outgoingPanel = panelView(currentPanel)
        val incomingPanel = panelView(target)
        val outgoingElems = panelElems(currentPanel)
        val incomingElems = panelElems(target)

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

        val hideDelay = (outgoingElems.size - 1) * stagger + exitDur + 50L
        outgoingPanel.postDelayed({
            outgoingPanel.visibility = View.INVISIBLE
            outgoingElems.forEach { (v, _) -> v.translationX = 0f }
            isAnimating = false
        }, hideDelay)

        currentPanel = target

        if (target == Panel.ADD) updateAddProgress() else stopAllAddGlows()
    }

    private fun panelView(p: Panel): View = when (p) {
        Panel.LIST -> listPanel
        Panel.ADD -> addPanel
        Panel.EDIT -> editPanel
    }

    private fun panelElems(p: Panel): List<Pair<View, Float>> = when (p) {
        Panel.LIST -> listElems
        Panel.ADD -> addElems
        Panel.EDIT -> editElems
    }

    // ─────────────────── Entrance + orbs ───────────────────

    private fun setupFloatingOrbs() {
        uws.ac.uk.studymate.util.OrbField.scatter(
            findViewById(R.id.decksCard),
            listOf(findViewById(R.id.homeBtn))
        )
    }


    private fun runEntranceAnimation() {
        uws.ac.uk.studymate.util.Entrance.play(card)
    }

    // ─────────────────── Helpers ───────────────────

    private fun parseSubjectColor(hex: String?): Int = ColorUtils.parseOrDefault(hex)

    private fun handleOnBackPressed() {
        finish()
    }

    private fun openLogin() {
        val i = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(i)
    }

    private fun openHome() {
        // Return to the existing Dashboard instead of launching a new one, so the
        // back stack stays a clean Dashboard -> screen -> sub-screen hierarchy.
        finish()
    }

    private fun openDeckOptions(deckId: Int, deckName: String) {
        startActivity(
            Intent().setClassName(packageName, "$packageName.ui.DeckCardsActivity")
                .putExtra("deck_id", deckId)
                .putExtra("deck_name", deckName)
        )
    }

    private fun openAlterDeck(deckId: Int) {
        // After creating a deck, jump straight to its cards screen so the user can add cards.
        startActivity(
            Intent().setClassName(packageName, "$packageName.ui.DeckCardsActivity")
                .putExtra("deck_id", deckId)
                .putExtra("deck_name", "New deck")
        )
    }
}
