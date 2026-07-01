package uws.ac.uk.studymate.ui

import uws.ac.uk.studymate.util.ColorUtils

import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.DatePicker
import android.widget.TimePicker
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
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
import uws.ac.uk.studymate.ui.viewmodels.AssignmentsItem
import uws.ac.uk.studymate.ui.viewmodels.AssignmentsViewModel
import uws.ac.uk.studymate.ui.viewmodels.ColorChoice
import uws.ac.uk.studymate.util.AssignmentIcons
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/*//////////////////////
Coded by Jamie Coleman
05/04/26
redesigned 18/04/26 — wood-glass UI, RecyclerView, inline add/edit/icon panel-swap
merged 17/06/26 — Subject folded in: the old subject picker is now a colour picker
 *//////////////////////
class AssignmentsActivity : AppCompatActivity() {

    private lateinit var vm: AssignmentsViewModel

    private lateinit var card: MaterialCardView
    private lateinit var listPanel: LinearLayout
    private lateinit var addPanel: View
    private lateinit var editPanel: View
    private lateinit var iconPanel: LinearLayout
    private lateinit var checklistPanel: LinearLayout

    private lateinit var checklistIcon: ImageView
    private lateinit var checklistTitle: TextView
    private lateinit var checklistSubText: TextView
    private lateinit var checklistEmptyText: TextView
    private lateinit var checklistRecycler: RecyclerView
    private lateinit var checklistAddInput: TextInputEditText
    private lateinit var checklistAddBtn: MaterialButton
    private lateinit var taskAdapter: TaskListAdapter
    private var checklistAssignmentId: Int? = null

    private lateinit var recycler: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var createAssignmentBtn: MaterialButton
    private lateinit var adapter: AssignmentListAdapter

    private lateinit var addTitleInput: TextInputEditText
    private lateinit var addColorRow: LinearLayout
    private lateinit var addPickDueBtn: MaterialButton
    private lateinit var addPickIconBtn: MaterialButton
    private lateinit var addConfirmBtn: MaterialButton
    private lateinit var addCancelBtn: MaterialButton

    // Progressive-glow guidance on the New-assignment panel: each step's field
    // glows when it's the next required input, and stays locked until then.
    private lateinit var addColorGlowWrap: View
    private lateinit var addTitleGlow: PulseRingView
    private lateinit var addColorGlow: PulseRingView
    private lateinit var addDueGlow: PulseRingView
    private lateinit var addIconGlow: PulseRingView
    private lateinit var addSaveGlow: PulseRingView
    private val addGlows: List<PulseRingView> by lazy {
        listOf(addTitleGlow, addColorGlow, addDueGlow, addIconGlow, addSaveGlow)
    }
    private var colorStepUnlocked = false

    private lateinit var editTitleInput: TextInputEditText
    private lateinit var editColorRow: LinearLayout
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

    private var colorChoices: List<ColorChoice> = emptyList()

    // Working form state
    private var addColor: ColorChoice? = null
    private var addDueDate: LocalDateTime? = null
    private var addIconKey: String? = null

    private var editingAssignment: Assignment? = null
    private var editColor: ColorChoice? = null
    private var editDueDate: LocalDateTime? = null
    private var editIconKey: String? = null

    private enum class Panel { LIST, ADD, EDIT, ICON, DATE, TIME, CHECKLIST }
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
    private lateinit var checklistElems: List<Pair<View, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignments)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)
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
            val colorsChanged = colorChoices != summary.colorChoices
            colorChoices = summary.colorChoices
            adapter.submit(summary.items)
            val isEmpty = summary.items.isEmpty()

            emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
            
            // Refresh swatches only if the colour list ever changes or they haven't been built yet.
            if (addColorRow.childCount == 0 || colorsChanged) {
                buildColorSwatches(addColorRow) { tappedColor ->
                    if (!colorStepUnlocked) return@buildColorSwatches
                    uws.ac.uk.studymate.util.Keyboard.hide(this)
                    addColor = tappedColor
                    highlightSelectedColor(addColorRow, tappedColor)
                    updateAddProgress()
                }
                buildColorSwatches(editColorRow) { tappedColor ->
                    uws.ac.uk.studymate.util.Keyboard.hide(this)
                    editColor = tappedColor
                    highlightSelectedColor(editColorRow, tappedColor)
                    updateEditIconEnabled()
                }
            }
        }
        vm.checklist.observe(this) { state -> renderChecklist(state) }
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
        checklistPanel = findViewById(R.id.checklistPanel)

        checklistIcon = findViewById(R.id.checklistIcon)
        checklistTitle = findViewById(R.id.checklistTitle)
        checklistSubText = findViewById(R.id.checklistSubText)
        checklistEmptyText = findViewById(R.id.checklistEmptyText)
        checklistRecycler = findViewById(R.id.checklistRecycler)
        checklistAddInput = findViewById(R.id.checklistAddInput)
        checklistAddBtn = findViewById(R.id.checklistAddBtn)

        recycler = findViewById(R.id.assignmentsRecycler)
        emptyText = findViewById(R.id.emptyStateText)
        createAssignmentBtn = findViewById(R.id.createAssignmentBtn)

        addTitleInput = findViewById(R.id.addTitleInput)
        addColorRow = findViewById(R.id.addSubjectRow)
        addPickDueBtn = findViewById(R.id.addPickDueBtn)
        addPickIconBtn = findViewById(R.id.addPickIconBtn)
        addConfirmBtn = findViewById(R.id.addConfirmBtn)
        addCancelBtn = findViewById(R.id.addCancelBtn)

        addColorGlowWrap = findViewById(R.id.addSubjectGlowWrap)
        addTitleGlow = findViewById(R.id.addTitleGlow)
        addColorGlow = findViewById(R.id.addSubjectGlow)
        addDueGlow = findViewById(R.id.addDueGlow)
        addIconGlow = findViewById(R.id.addIconGlow)
        addSaveGlow = findViewById(R.id.addSaveGlow)
        // Round the title field's box to 12dp so the glow ring lines up with it.
        val r12 = 12f * resources.displayMetrics.density
        findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.addTitleLayout)
            .setBoxCornerRadii(r12, r12, r12, r12)

        editTitleInput = findViewById(R.id.editTitleInput)
        editColorRow = findViewById(R.id.editSubjectRow)
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

        listElems = listOf(
            findViewById<View>(R.id.listTitle)            to -1f,
            findViewById<View>(R.id.listSubText)          to  1f,
            createAssignmentBtn                            to -1f,
            findViewById<View>(R.id.listSectionLabel)     to  1f,
            recycler                                       to -1f,
            emptyText                                      to  1f
        )
        // Order: name → colour → icon → due date → save.
        addElems = listOf(
            findViewById<View>(R.id.addTitleText)       to -1f,
            findViewById<View>(R.id.addSubText)         to  1f,
            findViewById<View>(R.id.addTitleGlowWrap)   to -1f,
            findViewById<View>(R.id.addSubjectLabel)    to  1f,
            findViewById<View>(R.id.addSubjectGlowWrap) to -1f,
            findViewById<View>(R.id.addIconLabel)       to  1f,
            findViewById<View>(R.id.addIconGlowWrap)    to -1f,
            findViewById<View>(R.id.addDueLabel)        to  1f,
            findViewById<View>(R.id.addDueGlowWrap)     to -1f,
            findViewById<View>(R.id.addSaveGlowWrap)    to  1f,
            addCancelBtn                                 to -1f
        )
        editElems = listOf(
            findViewById<View>(R.id.editTitleText)     to -1f,
            findViewById<View>(R.id.editSubText)       to  1f,
            findViewById<View>(R.id.editTitleLayout)   to -1f,
            findViewById<View>(R.id.editSubjectLabel)  to  1f,
            findViewById<View>(R.id.editSubjectRow)    to -1f,
            findViewById<View>(R.id.editIconLabel)     to  1f,
            editPickIconBtn                            to -1f,
            findViewById<View>(R.id.editDueLabel)      to  1f,
            editPickDueBtn                              to -1f,
            editConfirmBtn                             to  1f,
            editCancelBtn                              to -1f
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
            timeConfirmBtn                        to  1f
        )
        checklistElems = listOf(
            findViewById<View>(R.id.checklistHeaderRow) to -1f,
            checklistSubText                            to  1f,
            checklistRecycler                           to -1f,
            checklistEmptyText                          to  1f,
            findViewById<View>(R.id.checklistAddRow)    to  1f
        )
    }

    private fun setupRecycler() {
        adapter = AssignmentListAdapter(
            items = emptyList(),
            onEdit = { openEditFor(it) },
            onDelete = { confirmDelete(it) },
            onToggleDone = { confirmToggleDone(it) },
            onOpenChecklist = { openChecklistFor(it) },
            onOpenDecks = { openDecksFor(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        taskAdapter = TaskListAdapter(
            items = emptyList(),
            onToggle = { vm.toggleTask(it) },
            onDelete = { confirmDeleteTask(it) },
            onClick = { showTaskText(it) }
        )
        checklistRecycler.layoutManager = LinearLayoutManager(this)
        checklistRecycler.adapter = taskAdapter
    }

    private fun setupClicks() {
        // Delegate to the panel-aware back handler (setupBackHandler) instead of
        // jumping straight to Home — the top icon must always step back exactly one
        // panel (e.g. TIME -> DATE), same as system back, never skip past unsaved
        // form state.
        findViewById<MaterialButton>(R.id.homeBtn).setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        createAssignmentBtn.setOnClickListener {
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

        timeConfirmBtn.setOnClickListener {
            val picked = readDuePanels()
            when (duePickerOrigin) {
                Panel.ADD -> {
                    addDueDate = picked
                    addPickDueBtn.text = formatDueButton(picked)
                    updateAddProgress()
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
            populateIconGrid(addColor?.hex)
            swapToPanel(Panel.ICON)
        }
        editPickIconBtn.setOnClickListener {
            iconPickerOrigin = Panel.EDIT
            populateIconGrid(editColor?.hex)
            swapToPanel(Panel.ICON)
        }

        addConfirmBtn.setOnClickListener {
            vm.addAssignment(
                title = addTitleInput.text?.toString().orEmpty(),
                colorHex = addColor?.hex,
                dueDate = addDueDate?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                iconKey = addIconKey
            )
        }
        editConfirmBtn.setOnClickListener {
            val original = editingAssignment ?: return@setOnClickListener
            vm.updateAssignment(
                original = original,
                title = editTitleInput.text?.toString().orEmpty(),
                colorHex = editColor?.hex,
                dueDate = editDueDate?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                iconKey = editIconKey
            )
        }

        // React to title changes so we can ungate the next step as soon as
        // all the required fields are filled in.
        addTitleInput.addTextChangedListener(simpleWatcher { updateAddProgress() })
        editTitleInput.addTextChangedListener(simpleWatcher { updateEditIconEnabled() })

        // Checklist panel.
        checklistAddBtn.setOnClickListener { addCurrentTask() }
        checklistAddInput.setOnEditorActionListener { _, _, _ ->
            addCurrentTask()
            true
        }
    }

    private fun addCurrentTask() {
        val id = checklistAssignmentId ?: return
        val text = checklistAddInput.text?.toString().orEmpty()
        if (text.isBlank()) {
            checklistAddInput.requestFocus()
            uws.ac.uk.studymate.util.Keyboard.show(checklistAddInput)
            return
        }
        vm.addTask(id, text)
        checklistAddInput.setText("")
        uws.ac.uk.studymate.util.Keyboard.hide(this)
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentPanel) {
                    Panel.LIST -> openHome()
                    Panel.ADD, Panel.EDIT, Panel.CHECKLIST -> swapToPanel(Panel.LIST)
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
            checklistPanel.setPadding(
                checklistPanel.paddingLeft, checklistPanel.paddingTop,
                checklistPanel.paddingRight, navBar + base
            )
            insets
        }
    }

    // ─────────────────── Colour swatches ───────────────────

    // Lay the colour swatches out in fixed rows of three (built into a vertical
    // container) so the picker is two tidy rows and NEVER needs a horizontal scroll.
    private fun buildColorSwatches(container: LinearLayout, onTap: (ColorChoice) -> Unit) {
        container.removeAllViews()
        val density = resources.displayMetrics.density
        val cellW = (62 * density).toInt()
        val dot = (40 * density).toInt()
        val hMargin = (6 * density).toInt()
        val vMargin = (6 * density).toInt()

        colorChoices.chunked(3).forEachIndexed { rowIndex, group ->
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { if (rowIndex > 0) topMargin = vMargin }
            }

            group.forEach { choice ->
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(cellW, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        marginStart = hMargin
                        marginEnd = hMargin
                    }
                    tag = choice
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onTap(choice) }
                }

                val swatch = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dot, dot)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(parseColor(choice.hex))
                        setStroke((2 * density).toInt(), Color.parseColor("#66FAF8F5"))
                    }
                }

                val label = TextView(this).apply {
                    text = choice.label
                    textSize = 11f
                    setTextColor(Color.parseColor("#D4BC7E"))
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        cellW, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (4 * density).toInt() }
                }

                item.addView(swatch)
                item.addView(label)
                rowView.addView(item)
            }
            container.addView(rowView)
        }
    }

    private fun highlightSelectedColor(container: LinearLayout, selected: ColorChoice) {
        val density = resources.displayMetrics.density
        for (r in 0 until container.childCount) {
            val rowView = container.getChildAt(r) as? LinearLayout ?: continue
            for (i in 0 until rowView.childCount) {
                val item = rowView.getChildAt(i) as? LinearLayout ?: continue
                val choice = item.tag as? ColorChoice ?: continue
                val bg = item.getChildAt(0)?.background as? GradientDrawable ?: continue
                if (choice.hex.equals(selected.hex, ignoreCase = true)) {
                    bg.setStroke((3 * density).toInt(), Color.parseColor("#FFC4A24A"))
                } else {
                    bg.setStroke((2 * density).toInt(), Color.parseColor("#66FAF8F5"))
                }
            }
        }
    }

    // ─────────────────── Icon picker ───────────────────

    private fun populateIconGrid(colorHex: String?) {
        iconGridContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val tile = (54 * density).toInt()
        val gap = (8 * density).toInt()
        val tint = parseColor(colorHex)
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
                    // Equal-weight width so the 5 columns always divide the row
                    // and never overflow / clip on a narrow card (height stays
                    // fixed so the tiles stay roughly square).
                    layoutParams = LinearLayout.LayoutParams(0, tile, 1f).apply {
                        marginStart = gap / 2
                        marginEnd = gap / 2
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 12f * density
                        setColor(Color.parseColor("#59000000"))
                        setStroke((1 * density).toInt(), Color.parseColor("#55C4A24A"))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        when (iconPickerOrigin) {
                            Panel.ADD -> {
                                addIconKey = option.key
                                refreshIconButtonLabel(addPickIconBtn, option.key, true)
                                updateAddProgress()
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

    // Progressive guidance on the New-assignment panel: walk name → colour →
    // icon → due date → save, unlocking each step only once the ones before it are
    // done, and glowing the single next-required field. Cancel and the name field
    // are always available.
    private fun updateAddProgress() {
        val hasTitle = !addTitleInput.text.isNullOrBlank()
        val hasColor = addColor != null
        val hasIcon = !addIconKey.isNullOrBlank()
        val hasDue = addDueDate != null

        colorStepUnlocked = hasTitle
        addColorGlowWrap.alpha = if (hasTitle) 1f else 0.45f

        addPickIconBtn.isEnabled = hasTitle && hasColor
        addPickIconBtn.alpha = if (addPickIconBtn.isEnabled) 1f else 0.45f

        addPickDueBtn.isEnabled = hasTitle && hasColor && hasIcon
        addPickDueBtn.alpha = if (addPickDueBtn.isEnabled) 1f else 0.45f

        addConfirmBtn.isEnabled = hasTitle && hasColor && hasIcon && hasDue
        addConfirmBtn.alpha = if (addConfirmBtn.isEnabled) 1f else 0.45f

        val active = when {
            !hasTitle -> addTitleGlow
            !hasColor -> addColorGlow
            !hasIcon -> addIconGlow
            !hasDue -> addDueGlow
            else -> addSaveGlow
        }
        setActiveGlow(active)
    }

    // Glow exactly one step's ring (or none). Others are stopped + hidden.
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

    // Same order as the add panel: name → colour → icon → due date → save.
    private fun updateEditIconEnabled() {
        val hasTitle = !editTitleInput.text.isNullOrBlank()
        val hasColor = editColor != null
        val hasIcon = !editIconKey.isNullOrBlank()
        val hasDue = editDueDate != null

        editPickIconBtn.isEnabled = hasTitle && hasColor
        editPickIconBtn.alpha = if (editPickIconBtn.isEnabled) 1f else 0.45f

        editPickDueBtn.isEnabled = hasTitle && hasColor && hasIcon
        editPickDueBtn.alpha = if (editPickDueBtn.isEnabled) 1f else 0.45f

        val saveReady = hasTitle && hasColor && hasIcon && hasDue
        editConfirmBtn.isEnabled = saveReady
        editConfirmBtn.alpha = if (saveReady) 1f else 0.45f
    }

    // ─────────────────── Open add / edit ───────────────────

    private fun openAddPanel() {
        addTitleInput.setText("")
        addColor = null
        addDueDate = null
        addIconKey = null
        addPickDueBtn.text = "Pick due date"
        addPickIconBtn.text = "Choose icon"
        // Clear any swatch highlight from a previous session.
        buildColorSwatches(addColorRow) { tapped ->
            if (!colorStepUnlocked) return@buildColorSwatches
            uws.ac.uk.studymate.util.Keyboard.hide(this)
            addColor = tapped
            highlightSelectedColor(addColorRow, tapped)
            updateAddProgress()
        }
        updateAddProgress()
        swapToPanel(Panel.ADD)
    }

    private fun openEditFor(item: AssignmentsItem) {
        editingAssignment = item.assignment
        editTitleInput.setText(item.assignment.title)
        editColor = colorChoices.firstOrNull { it.hex.equals(item.assignment.color, ignoreCase = true) }
        editDueDate = item.dueAt
        editIconKey = item.assignment.icon
        editPickDueBtn.text = formatDueButton(item.dueAt)
        refreshIconButtonLabel(editPickIconBtn, item.assignment.icon, isAdd = false)
        buildColorSwatches(editColorRow) { tapped ->
            editColor = tapped
            highlightSelectedColor(editColorRow, tapped)
            updateEditIconEnabled()
        }
        editColor?.let { highlightSelectedColor(editColorRow, it) }
        updateEditIconEnabled()
        swapToPanel(Panel.EDIT)
    }

    // ─────────────────── Checklist ───────────────────

    private fun openChecklistFor(item: AssignmentsItem) {
        checklistAssignmentId = item.assignment.id
        checklistAddInput.setText("")
        // Tint the header icon in the assignment's colour, matching the list row.
        checklistIcon.setImageResource(AssignmentIcons.drawableForKey(item.iconKey))
        checklistIcon.setColorFilter(ColorUtils.parseOrDefault(item.colorHex))
        checklistTitle.text = item.assignment.title
        // Show last-known counts immediately; the observer refreshes once loaded.
        checklistSubText.text = "${item.taskDone} of ${item.taskTotal} done"
        taskAdapter.submit(emptyList())
        checklistEmptyText.visibility = View.GONE
        vm.loadChecklist(item.assignment.id)
        swapToPanel(Panel.CHECKLIST)
    }

    private fun renderChecklist(state: uws.ac.uk.studymate.ui.viewmodels.ChecklistState) {
        // Ignore a stale emission for an assignment we're no longer viewing.
        if (state.assignment.id != checklistAssignmentId) return
        checklistTitle.text = state.assignment.title
        val done = state.tasks.count { it.isDone }
        checklistSubText.text = "$done of ${state.tasks.size} done"
        taskAdapter.submit(state.tasks)
        // Recycler stays in the (weighted) layout in both states; only the empty
        // placeholder toggles, so the add field + Back never jump position.
        checklistEmptyText.visibility = if (state.tasks.isEmpty()) View.VISIBLE else View.GONE
    }

    // Show one checklist item's full text (rows are truncated to two lines).
    private fun showTaskText(task: uws.ac.uk.studymate.data.entities.AssignmentTask) {
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle(if (task.isDone) "Checklist item (done)" else "Checklist item")
            .setMessage(task.text)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun confirmDeleteTask(task: uws.ac.uk.studymate.data.entities.AssignmentTask) {
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Delete checklist item")
            .setMessage("Delete \"${task.text}\"?")
            .setPositiveButton("Delete") { _, _ -> vm.deleteTask(task) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(item: AssignmentsItem) {
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Delete assignment")
            .setMessage("Delete \"${item.assignment.title}\" and its flashcard decks?")
            .setPositiveButton("Delete") { _, _ -> vm.deleteAssignment(item.assignment) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openDecksFor(item: AssignmentsItem) {
        startActivity(
            Intent().setClassName(packageName, "$packageName.ui.FlashcardDecksActivity")
                .putExtra("scoped_assignment_id", item.assignment.id)
                .putExtra("scoped_assignment_name", item.assignment.title)
        )
    }

    // Tapping the done circle. If it's already done, just un-mark it. If not,
    // show a themed pop-up explaining what marking-done does before confirming.
    private fun confirmToggleDone(item: AssignmentsItem) {
        if (item.isCompleted) {
            vm.toggleComplete(item)
            return
        }
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Mark as done?")
            .setMessage(
                "Finishing an assignment early clears it from your list, stops its " +
                    "reminders, and counts it as complete in your statistics.\n\n" +
                    "Assignments are also counted as done automatically once their due date passes."
            )
            .setPositiveButton("Mark done") { _, _ -> vm.toggleComplete(item) }
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

        // Leaving a panel (often a text-entry one) — make sure the keyboard goes too.
        uws.ac.uk.studymate.util.Keyboard.hide(this)

        // Returning from the checklist: refresh the list so its "done/total" hint updates.
        if (currentPanel == Panel.CHECKLIST && target == Panel.LIST) vm.loadAssignments()

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

        if (target == Panel.TIME || target == Panel.DATE) {
            uws.ac.uk.studymate.util.OrbField.pause()
        } else {
            uws.ac.uk.studymate.util.OrbField.resume()
        }

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

        // Only run the progressive glow while the New-assignment panel is showing.
        if (target == Panel.ADD) updateAddProgress() else stopAllAddGlows()
    }

    private fun panelView(p: Panel): View = when (p) {
        Panel.LIST -> listPanel
        Panel.ADD -> addPanel
        Panel.EDIT -> editPanel
        Panel.ICON -> iconPanel
        Panel.DATE -> datePanel
        Panel.TIME -> timePanel
        Panel.CHECKLIST -> checklistPanel
    }

    private fun panelElems(p: Panel): List<Pair<View, Float>> = when (p) {
        Panel.LIST -> listElems
        Panel.ADD -> addElems
        Panel.EDIT -> editElems
        Panel.ICON -> iconElems
        Panel.DATE -> dateElems
        Panel.TIME -> timeElems
        Panel.CHECKLIST -> checklistElems
    }

    // ─────────────────── Entrance + orbs ───────────────────

    private fun setupFloatingOrbs() {
        uws.ac.uk.studymate.util.OrbField.scatter(
            findViewById(R.id.assignmentsCard),
            listOf(findViewById(R.id.homeBtn))
        )
    }

    private fun runEntranceAnimation() {
        uws.ac.uk.studymate.util.Entrance.play(card)
    }

    // ─────────────────── Helpers ───────────────────

    private fun parseColor(hex: String?): Int = ColorUtils.parseOrDefault(hex)

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
        // Return to the existing Dashboard instead of launching a new one, so the
        // back stack stays a clean Dashboard -> screen -> sub-screen hierarchy.
        finish()
    }
}
