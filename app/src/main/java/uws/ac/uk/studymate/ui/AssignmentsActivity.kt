package uws.ac.uk.studymate.ui

import android.animation.ObjectAnimator
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.DatePicker
import android.widget.TimePicker
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
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
import com.google.android.material.textfield.TextInputEditText
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.ui.viewmodels.AssignmentsItem
import uws.ac.uk.studymate.ui.viewmodels.AssignmentsViewModel
import uws.ac.uk.studymate.util.AssignmentIcons
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/*//////////////////////
Coded by Jamie Coleman
05/04/26
redesigned 18/04/26 — wood-glass UI, RecyclerView, inline add/edit/icon panel-swap
 *//////////////////////
class AssignmentsActivity : AppCompatActivity() {

    private lateinit var vm: AssignmentsViewModel

    private lateinit var card: MaterialCardView
    private lateinit var listPanel: LinearLayout
    private lateinit var addPanel: View
    private lateinit var editPanel: View
    private lateinit var iconPanel: LinearLayout

    private lateinit var recycler: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: AssignmentListAdapter

    private lateinit var addTitleInput: TextInputEditText
    private lateinit var addSubjectRow: LinearLayout
    private lateinit var addPickDueBtn: MaterialButton
    private lateinit var addPickIconBtn: MaterialButton
    private lateinit var addConfirmBtn: MaterialButton
    private lateinit var addCancelBtn: MaterialButton

    private lateinit var editTitleInput: TextInputEditText
    private lateinit var editSubjectRow: LinearLayout
    private lateinit var editPickDueBtn: MaterialButton
    private lateinit var editPickIconBtn: MaterialButton
    private lateinit var editConfirmBtn: MaterialButton
    private lateinit var editCancelBtn: MaterialButton

    private lateinit var iconGridContainer: LinearLayout

    private lateinit var datePanel: LinearLayout
    private lateinit var datePanelDate: DatePicker
    private lateinit var dateNextBtn: MaterialButton
    private lateinit var dateCancelBtn: MaterialButton

    private lateinit var timePanel: LinearLayout
    private lateinit var timePanelTime: TimePicker
    private lateinit var timeConfirmBtn: MaterialButton
    private lateinit var timeBackBtn: MaterialButton

    private var subjects: List<Subject> = emptyList()

    // Working form state
    private var addSubject: Subject? = null
    private var addDueDate: LocalDateTime? = null
    private var addIconKey: String? = null

    private var editingAssignment: Assignment? = null
    private var editSubject: Subject? = null
    private var editDueDate: LocalDateTime? = null
    private var editIconKey: String? = null

    private enum class Panel { LIST, ADD, EDIT, ICON, DATE, TIME }
    private var currentPanel: Panel = Panel.LIST
    // The panel we came from when we entered a sub-picker — back returns there.
    private var iconPickerOrigin: Panel = Panel.ADD
    private var duePickerOrigin: Panel = Panel.ADD
    private var isAnimating = false

    private lateinit var listElems: List<Pair<View, Float>>
    private lateinit var addElems: List<Pair<View, Float>>
    private lateinit var editElems: List<Pair<View, Float>>
    private lateinit var iconElems: List<Pair<View, Float>>
    private lateinit var dateElems: List<Pair<View, Float>>
    private lateinit var timeElems: List<Pair<View, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignments)
        uws.ac.uk.studymate.util.KeyboardInsets.apply(this)

        vm = ViewModelProvider(this)[AssignmentsViewModel::class.java]

        bindViews()
        setupRecycler()
        setupClicks()
        setupBackHandler()
        setupWindowInsets()
        setupFloatingOrbs()
        runEntranceAnimation()

        vm.assignmentsSummary.observe(this) { summary ->
            subjects = summary.subjects
            adapter.submit(summary.items)
            val isEmpty = summary.items.isEmpty()
            emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
            // Refresh swatches when subjects change so the picker stays in sync.
            buildSubjectSwatches(addSubjectRow) { tappedSubject ->
                addSubject = tappedSubject
                highlightSelectedSubject(addSubjectRow, tappedSubject)
                updateAddIconEnabled()
            }
            buildSubjectSwatches(editSubjectRow) { tappedSubject ->
                editSubject = tappedSubject
                highlightSelectedSubject(editSubjectRow, tappedSubject)
                updateEditIconEnabled()
            }
        }
        vm.sessionExpired.observe(this) { expired -> if (expired) openLogin() }
        vm.message.observe(this) { msg ->
            if (msg.isNullOrBlank()) return@observe
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            if (msg == "Assignment added" || msg == "Assignment updated") {
                swapToPanel(Panel.LIST)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.loadAssignments()
    }

    // ─────────────────── Setup ───────────────────

    private fun bindViews() {
        card = findViewById(R.id.assignmentsCard)
        listPanel = findViewById(R.id.listPanel)
        addPanel = findViewById(R.id.addPanel)
        editPanel = findViewById(R.id.editPanel)
        iconPanel = findViewById(R.id.iconPanel)

        recycler = findViewById(R.id.assignmentsRecycler)
        emptyText = findViewById(R.id.emptyStateText)

        addTitleInput = findViewById(R.id.addTitleInput)
        addSubjectRow = findViewById(R.id.addSubjectRow)
        addPickDueBtn = findViewById(R.id.addPickDueBtn)
        addPickIconBtn = findViewById(R.id.addPickIconBtn)
        addConfirmBtn = findViewById(R.id.addConfirmBtn)
        addCancelBtn = findViewById(R.id.addCancelBtn)

        editTitleInput = findViewById(R.id.editTitleInput)
        editSubjectRow = findViewById(R.id.editSubjectRow)
        editPickDueBtn = findViewById(R.id.editPickDueBtn)
        editPickIconBtn = findViewById(R.id.editPickIconBtn)
        editConfirmBtn = findViewById(R.id.editConfirmBtn)
        editCancelBtn = findViewById(R.id.editCancelBtn)

        iconGridContainer = findViewById(R.id.iconGridContainer)

        datePanel = findViewById(R.id.datePanel)
        datePanelDate = findViewById(R.id.datePanelDatePicker)
        dateNextBtn = findViewById(R.id.dateNextBtn)
        dateCancelBtn = findViewById(R.id.dateCancelBtn)

        timePanel = findViewById(R.id.timePanel)
        timePanelTime = findViewById(R.id.timePanelTimePicker)
        timePanelTime.setIs24HourView(true)
        timeConfirmBtn = findViewById(R.id.timeConfirmBtn)
        timeBackBtn = findViewById(R.id.timeBackBtn)

        listElems = listOf(
            findViewById<View>(R.id.listTitle)            to -1f,
            findViewById<View>(R.id.listSubText)          to  1f,
            findViewById<View>(R.id.createAssignmentBtn)  to -1f,
            findViewById<View>(R.id.listSectionLabel)     to  1f,
            recycler                                       to -1f,
            emptyText                                      to  1f
        )
        addElems = listOf(
            findViewById<View>(R.id.addTitleText)     to -1f,
            findViewById<View>(R.id.addSubText)       to  1f,
            findViewById<View>(R.id.addTitleLayout)   to -1f,
            findViewById<View>(R.id.addSubjectLabel)  to  1f,
            findViewById<View>(R.id.addSubjectScroll) to -1f,
            findViewById<View>(R.id.addDueLabel)      to  1f,
            addPickDueBtn                              to -1f,
            addPickIconBtn                             to  1f,
            addConfirmBtn                              to -1f,
            addCancelBtn                               to  1f
        )
        editElems = listOf(
            findViewById<View>(R.id.editTitleText)     to -1f,
            findViewById<View>(R.id.editSubText)       to  1f,
            findViewById<View>(R.id.editTitleLayout)   to -1f,
            findViewById<View>(R.id.editSubjectLabel)  to  1f,
            findViewById<View>(R.id.editSubjectScroll) to -1f,
            findViewById<View>(R.id.editDueLabel)      to  1f,
            editPickDueBtn                              to -1f,
            editPickIconBtn                             to  1f,
            editConfirmBtn                              to -1f,
            editCancelBtn                               to  1f
        )
        iconElems = listOf(
            findViewById<View>(R.id.iconPanelTitle)   to -1f,
            findViewById<View>(R.id.iconPanelSubText) to  1f,
            findViewById<View>(R.id.iconScroll)       to -1f
        )
        dateElems = listOf(
            findViewById<View>(R.id.dateContent) to -1f,
            dateNextBtn                           to  1f,
            dateCancelBtn                         to -1f
        )
        timeElems = listOf(
            findViewById<View>(R.id.timeContent) to -1f,
            timeConfirmBtn                        to  1f,
            timeBackBtn                           to -1f
        )
    }

    private fun setupRecycler() {
        adapter = AssignmentListAdapter(
            items = emptyList(),
            onEdit = { openEditFor(it) },
            onDelete = { confirmDelete(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    private fun setupClicks() {
        findViewById<MaterialButton>(R.id.homeBtn).setOnClickListener { openHome() }

        findViewById<MaterialButton>(R.id.createAssignmentBtn).setOnClickListener {
            openAddPanel()
        }

        addCancelBtn.setOnClickListener { swapToPanel(Panel.LIST) }
        editCancelBtn.setOnClickListener { swapToPanel(Panel.LIST) }

        addPickDueBtn.setOnClickListener {
            duePickerOrigin = Panel.ADD
            preloadDuePanels(addDueDate)
            swapToPanel(Panel.DATE)
        }
        editPickDueBtn.setOnClickListener {
            duePickerOrigin = Panel.EDIT
            preloadDuePanels(editDueDate)
            swapToPanel(Panel.DATE)
        }
        dateCancelBtn.setOnClickListener { swapToPanel(duePickerOrigin) }
        dateNextBtn.setOnClickListener { swapToPanel(Panel.TIME) }

        timeBackBtn.setOnClickListener { swapToPanel(Panel.DATE) }
        timeConfirmBtn.setOnClickListener {
            val picked = readDuePanels()
            when (duePickerOrigin) {
                Panel.ADD -> {
                    addDueDate = picked
                    addPickDueBtn.text = formatDueButton(picked)
                    updateAddIconEnabled()
                    swapToPanel(Panel.ADD)
                }
                Panel.EDIT -> {
                    editDueDate = picked
                    editPickDueBtn.text = formatDueButton(picked)
                    updateEditIconEnabled()
                    swapToPanel(Panel.EDIT)
                }
                else -> swapToPanel(Panel.LIST)
            }
        }

        addPickIconBtn.setOnClickListener {
            iconPickerOrigin = Panel.ADD
            populateIconGrid(addSubject?.color)
            swapToPanel(Panel.ICON)
        }
        editPickIconBtn.setOnClickListener {
            iconPickerOrigin = Panel.EDIT
            populateIconGrid(editSubject?.color)
            swapToPanel(Panel.ICON)
        }

        addConfirmBtn.setOnClickListener {
            vm.addAssignment(
                title = addTitleInput.text?.toString().orEmpty(),
                subject = addSubject,
                dueDate = addDueDate?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                iconKey = addIconKey
            )
        }
        editConfirmBtn.setOnClickListener {
            val original = editingAssignment ?: return@setOnClickListener
            vm.updateAssignment(
                original = original,
                title = editTitleInput.text?.toString().orEmpty(),
                subject = editSubject,
                dueDate = editDueDate?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                iconKey = editIconKey
            )
        }

        // React to title changes so we can ungate the icon button as soon as
        // all three required fields are filled in.
        addTitleInput.addTextChangedListener(simpleWatcher { updateAddIconEnabled() })
        editTitleInput.addTextChangedListener(simpleWatcher { updateEditIconEnabled() })
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentPanel) {
                    Panel.LIST -> openHome()
                    Panel.ADD, Panel.EDIT -> swapToPanel(Panel.LIST)
                    Panel.ICON -> swapToPanel(iconPickerOrigin)
                    Panel.DATE -> swapToPanel(duePickerOrigin)
                    Panel.TIME -> swapToPanel(Panel.DATE)
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
            iconPanel.setPadding(
                iconPanel.paddingLeft, iconPanel.paddingTop,
                iconPanel.paddingRight, navBar + base
            )
            datePanel.setPadding(
                datePanel.paddingLeft, datePanel.paddingTop,
                datePanel.paddingRight, navBar + base
            )
            timePanel.setPadding(
                timePanel.paddingLeft, timePanel.paddingTop,
                timePanel.paddingRight, navBar + base
            )
            insets
        }
    }

    // ─────────────────── Subject swatches ───────────────────

    private fun buildSubjectSwatches(row: LinearLayout, onTap: (Subject) -> Unit) {
        row.removeAllViews()
        val density = resources.displayMetrics.density
        val container = (62 * density).toInt()
        val dot = (40 * density).toInt()
        val margin = (8 * density).toInt()

        subjects.forEach { subject ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(container, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = margin
                }
                tag = subject
                isClickable = true
                isFocusable = true
                setOnClickListener { onTap(subject) }
            }

            val swatch = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dot, dot)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(parseSubjectColor(subject.color))
                    setStroke((2 * density).toInt(), Color.parseColor("#66FAF8F5"))
                }
            }

            val label = TextView(this).apply {
                text = subject.name
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

    private fun highlightSelectedSubject(row: LinearLayout, selected: Subject) {
        val density = resources.displayMetrics.density
        for (i in 0 until row.childCount) {
            val item = row.getChildAt(i) as? LinearLayout ?: continue
            val subject = item.tag as? Subject ?: continue
            val swatch = item.getChildAt(0)
            val bg = swatch.background as? GradientDrawable ?: continue
            if (subject.id == selected.id) {
                bg.setStroke((3 * density).toInt(), Color.parseColor("#FFC4A24A"))
            } else {
                bg.setStroke((2 * density).toInt(), Color.parseColor("#66FAF8F5"))
            }
        }
    }

    // ─────────────────── Icon picker ───────────────────

    private fun populateIconGrid(colorHex: String?) {
        iconGridContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val tile = (54 * density).toInt()
        val gap = (8 * density).toInt()
        val tint = parseSubjectColor(colorHex)
        val perRow = 5

        AssignmentIcons.options.chunked(perRow).forEachIndexed { rowIndex, group ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { if (rowIndex > 0) topMargin = gap }
            }

            group.forEach { option ->
                val cell = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(tile, tile).apply {
                        marginStart = gap / 2
                        marginEnd = gap / 2
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 12f * density
                        setColor(Color.parseColor("#33000000"))
                        setStroke((1 * density).toInt(), Color.parseColor("#55C4A24A"))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        when (iconPickerOrigin) {
                            Panel.ADD -> {
                                addIconKey = option.key
                                refreshIconButtonLabel(addPickIconBtn, option.key, true)
                                updateAddIconEnabled()
                                swapToPanel(Panel.ADD)
                            }
                            Panel.EDIT -> {
                                editIconKey = option.key
                                refreshIconButtonLabel(editPickIconBtn, option.key, false)
                                updateEditIconEnabled()
                                swapToPanel(Panel.EDIT)
                            }
                            else -> {}
                        }
                    }
                }

                val img = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        (28 * density).toInt(), (28 * density).toInt(), Gravity.CENTER
                    )
                    setImageResource(option.drawableResId)
                    setColorFilter(tint)
                }
                cell.addView(img)
                row.addView(cell)
            }
            iconGridContainer.addView(row)
        }
    }

    private fun refreshIconButtonLabel(btn: MaterialButton, iconKey: String, isAdd: Boolean) {
        val prettyKey = iconKey.replace('_', ' ').replaceFirstChar { it.uppercase() }
        btn.text = if (isAdd) "Icon: $prettyKey" else "Edit icon: $prettyKey"
    }

    // ─────────────────── Form gating ───────────────────

    private fun updateAddIconEnabled() {
        val coreReady = !addTitleInput.text.isNullOrBlank() &&
                addSubject != null &&
                addDueDate != null
        addPickIconBtn.isEnabled = coreReady
        addPickIconBtn.alpha = if (coreReady) 1f else 0.45f

        val saveReady = coreReady && !addIconKey.isNullOrBlank()
        addConfirmBtn.isEnabled = saveReady
        addConfirmBtn.alpha = if (saveReady) 1f else 0.45f
    }

    private fun updateEditIconEnabled() {
        val coreReady = !editTitleInput.text.isNullOrBlank() &&
                editSubject != null &&
                editDueDate != null
        editPickIconBtn.isEnabled = coreReady
        editPickIconBtn.alpha = if (coreReady) 1f else 0.45f

        val saveReady = coreReady && !editIconKey.isNullOrBlank()
        editConfirmBtn.isEnabled = saveReady
        editConfirmBtn.alpha = if (saveReady) 1f else 0.45f
    }

    // ─────────────────── Open add / edit ───────────────────

    private fun openAddPanel() {
        addTitleInput.setText("")
        addSubject = null
        addDueDate = null
        addIconKey = null
        addPickDueBtn.text = "Pick due date"
        addPickIconBtn.text = "Choose icon"
        // Clear any swatch highlight from a previous session.
        buildSubjectSwatches(addSubjectRow) { tapped ->
            addSubject = tapped
            highlightSelectedSubject(addSubjectRow, tapped)
            updateAddIconEnabled()
        }
        updateAddIconEnabled()
        swapToPanel(Panel.ADD)
    }

    private fun openEditFor(item: AssignmentsItem) {
        editingAssignment = item.assignment
        editTitleInput.setText(item.assignment.title)
        editSubject = subjects.firstOrNull { it.id == item.assignment.subjectId }
        editDueDate = item.dueAt
        editIconKey = item.assignment.icon
        editPickDueBtn.text = formatDueButton(item.dueAt)
        refreshIconButtonLabel(editPickIconBtn, item.assignment.icon, isAdd = false)
        buildSubjectSwatches(editSubjectRow) { tapped ->
            editSubject = tapped
            highlightSelectedSubject(editSubjectRow, tapped)
            updateEditIconEnabled()
        }
        editSubject?.let { highlightSelectedSubject(editSubjectRow, it) }
        updateEditIconEnabled()
        swapToPanel(Panel.EDIT)
    }

    private fun confirmDelete(item: AssignmentsItem) {
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Delete assignment")
            .setMessage("Delete \"${item.assignment.title}\"?")
            .setPositiveButton("Delete") { _, _ -> vm.deleteAssignment(item.assignment) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─────────────────── Date picker ───────────────────

    private fun preloadDuePanels(current: LocalDateTime?) {
        val initial = current ?: LocalDateTime.now()
        datePanelDate.updateDate(initial.year, initial.monthValue - 1, initial.dayOfMonth)
        timePanelTime.hour = initial.hour
        timePanelTime.minute = initial.minute
    }

    private fun readDuePanels(): LocalDateTime {
        val date = LocalDate.of(
            datePanelDate.year,
            datePanelDate.month + 1,
            datePanelDate.dayOfMonth
        )
        return date.atTime(timePanelTime.hour, timePanelTime.minute)
    }

    private fun formatDueButton(dt: LocalDateTime): String {
        return "Due: ${dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}"
    }

    // ─────────────────── Panel swap ───────────────────

    private fun swapToPanel(target: Panel) {
        if (isAnimating || target == currentPanel) return

        val outgoingPanel = panelView(currentPanel)
        val incomingPanel = panelView(target)
        val outgoingElems = panelElems(currentPanel)
        val incomingElems = panelElems(target)

        // Forward = "going deeper": LIST → ADD/EDIT; ADD/EDIT → ICON or DATE;
        // DATE → TIME. Anything else (returning toward LIST) is backward.
        val goingDeeperFromForm = (target == Panel.ICON || target == Panel.DATE) &&
                (currentPanel == Panel.ADD || currentPanel == Panel.EDIT)
        val goingForward = currentPanel == Panel.LIST ||
                goingDeeperFromForm ||
                (currentPanel == Panel.DATE && target == Panel.TIME)
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
    }

    private fun panelView(p: Panel): View = when (p) {
        Panel.LIST -> listPanel
        Panel.ADD -> addPanel
        Panel.EDIT -> editPanel
        Panel.ICON -> iconPanel
        Panel.DATE -> datePanel
        Panel.TIME -> timePanel
    }

    private fun panelElems(p: Panel): List<Pair<View, Float>> = when (p) {
        Panel.LIST -> listElems
        Panel.ADD -> addElems
        Panel.EDIT -> editElems
        Panel.ICON -> iconElems
        Panel.DATE -> dateElems
        Panel.TIME -> timeElems
    }

    // ─────────────────── Entrance + orbs ───────────────────

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
        val d = resources.displayMetrics.density
        card.translationY = 200f * d
        card.alpha = 0f
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .setStartDelay(60)
            .withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()
    }

    // ─────────────────── Helpers ───────────────────

    private fun parseSubjectColor(hex: String?): Int = try {
        if (hex.isNullOrBlank()) Color.parseColor("#C4A24A") else Color.parseColor(hex)
    } catch (_: IllegalArgumentException) {
        Color.parseColor("#C4A24A")
    }

    private fun simpleWatcher(onChanged: () -> Unit): android.text.TextWatcher {
        return object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onChanged()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
    }

    private fun openLogin() {
        val i = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(i)
    }

    private fun openHome() {
        startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
    }
}
