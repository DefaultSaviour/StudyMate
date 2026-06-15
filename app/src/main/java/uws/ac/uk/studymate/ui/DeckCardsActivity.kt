package uws.ac.uk.studymate.ui

import android.animation.ObjectAnimator
import android.content.Intent
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.ui.viewmodels.DeckCardsSummary
import uws.ac.uk.studymate.ui.viewmodels.DeckCardsViewModel

class DeckCardsActivity : AppCompatActivity() {

    private lateinit var vm: DeckCardsViewModel

    private lateinit var card: MaterialCardView
    private lateinit var listPanel: LinearLayout
    private lateinit var addPanel: View
    private lateinit var editPanel: View

    private lateinit var titleText: TextView
    private lateinit var subText: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var addCardBtn: MaterialButton
    private lateinit var reviewBtn: MaterialButton

    private lateinit var addFront: TextInputEditText
    private lateinit var addBack: TextInputEditText
    private lateinit var addConfirmBtn: MaterialButton
    private lateinit var addCancelBtn: MaterialButton

    private lateinit var editFront: TextInputEditText
    private lateinit var editBack: TextInputEditText
    private lateinit var editConfirmBtn: MaterialButton
    private lateinit var editCancelBtn: MaterialButton

    private lateinit var adapter: CardListAdapter

    private var deckId: Int = -1
    private var deckName: String = "Deck"
    private var editingCard: FlashCard? = null

    private enum class Panel { LIST, ADD, EDIT }
    private var currentPanel = Panel.LIST
    private var isAnimating = false

    private lateinit var listElems: List<Pair<View, Float>>
    private lateinit var addElems: List<Pair<View, Float>>
    private lateinit var editElems: List<Pair<View, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_cards)
        uws.ac.uk.studymate.util.KeyboardInsets.apply(this)

        deckId = intent.getIntExtra("deck_id", -1)
        deckName = intent.getStringExtra("deck_name") ?: "Deck"

        vm = ViewModelProvider(this)[DeckCardsViewModel::class.java]

        bindViews()
        titleText.text = deckName

        setupRecycler()
        setupClicks()
        setupBackHandler()
        setupWindowInsets()

        uws.ac.uk.studymate.util.OrbField.scatter(
            findViewById(R.id.cardsCard),
            listOf(findViewById(R.id.backBtn))
        )

        val d = resources.displayMetrics.density
        card.translationY = 200f * d
        card.alpha = 0f
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        card.animate().translationY(0f).alpha(1f).setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f)).setStartDelay(60)
            .withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()

        vm.summary.observe(this) { applySummary(it) }
        vm.sessionExpired.observe(this) { if (it) openLogin() }
        vm.message.observe(this) { msg ->
            if (msg.isNullOrBlank()) return@observe
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            if (msg == "Card added" || msg == "Card updated") swapToPanel(Panel.LIST)
        }
    }

    override fun onResume() {
        super.onResume()
        vm.load(deckId, deckName)
    }

    private fun bindViews() {
        card = findViewById(R.id.cardsCard)
        listPanel = findViewById(R.id.listPanel)
        addPanel = findViewById(R.id.addPanel)
        editPanel = findViewById(R.id.editPanel)

        titleText = findViewById(R.id.listTitle)
        subText = findViewById(R.id.listSubText)
        recycler = findViewById(R.id.cardsRecycler)
        emptyText = findViewById(R.id.emptyStateText)
        addCardBtn = findViewById(R.id.addCardBtn)
        reviewBtn = findViewById(R.id.reviewBtn)

        addFront = findViewById(R.id.addFrontInput)
        addBack = findViewById(R.id.addBackInput)
        addConfirmBtn = findViewById(R.id.addConfirmBtn)
        addCancelBtn = findViewById(R.id.addCancelBtn)

        editFront = findViewById(R.id.editFrontInput)
        editBack = findViewById(R.id.editBackInput)
        editConfirmBtn = findViewById(R.id.editConfirmBtn)
        editCancelBtn = findViewById(R.id.editCancelBtn)

        listElems = listOf(
            titleText                                    to -1f,
            subText                                       to  1f,
            findViewById<View>(R.id.actionRow)           to -1f,
            findViewById<View>(R.id.listSectionLabel)    to  1f,
            recycler                                      to -1f,
            emptyText                                     to  1f
        )
        addElems = listOf(
            findViewById<View>(R.id.addTitleText)    to -1f,
            findViewById<View>(R.id.addSubText)      to  1f,
            findViewById<View>(R.id.addFrontLayout)  to -1f,
            findViewById<View>(R.id.addBackLayout)   to  1f,
            addConfirmBtn                              to -1f,
            addCancelBtn                               to  1f
        )
        editElems = listOf(
            findViewById<View>(R.id.editTitleText)    to -1f,
            findViewById<View>(R.id.editSubText)      to  1f,
            findViewById<View>(R.id.editFrontLayout)  to -1f,
            findViewById<View>(R.id.editBackLayout)   to  1f,
            editConfirmBtn                              to -1f,
            editCancelBtn                               to  1f
        )
    }

    private fun setupRecycler() {
        adapter = CardListAdapter(
            items = emptyList(),
            onEdit = { openEditFor(it) },
            onDelete = { confirmDelete(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    private fun setupClicks() {
        findViewById<MaterialButton>(R.id.backBtn).setOnClickListener { finish() }
        addCardBtn.setOnClickListener { openAddPanel() }
        reviewBtn.setOnClickListener {
            startActivity(
                Intent().setClassName(packageName, "$packageName.ui.ReviewDeckActivity")
                    .putExtra("deck_id", deckId)
                    .putExtra("deck_name", titleText.text.toString())
            )
        }
        addCancelBtn.setOnClickListener { swapToPanel(Panel.LIST) }
        editCancelBtn.setOnClickListener { swapToPanel(Panel.LIST) }
        addConfirmBtn.setOnClickListener {
            vm.addCard(addFront.text?.toString().orEmpty(), addBack.text?.toString().orEmpty())
        }
        editConfirmBtn.setOnClickListener {
            val original = editingCard ?: return@setOnClickListener
            vm.updateCard(original, editFront.text?.toString().orEmpty(), editBack.text?.toString().orEmpty())
        }
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
            listPanel.setPadding(listPanel.paddingLeft, listPanel.paddingTop,
                listPanel.paddingRight, navBar + base)
            addPanel.setPadding(0, 0, 0, navBar + base)
            editPanel.setPadding(0, 0, 0, navBar + base)
            insets
        }
    }

    private fun applySummary(summary: DeckCardsSummary) {
        titleText.text = summary.deckName
        deckName = summary.deckName
        val cardWord = if (summary.cards.size == 1) "card" else "cards"
        val base = "${summary.subjectName} • ${summary.cards.size} $cardWord"
        subText.text = if (summary.dueText.isNotEmpty()) "$base\n${summary.dueText}" else base
        adapter.submit(summary.cards)
        val isEmpty = summary.cards.isEmpty()
        emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE

        // Gate Start review behind having at least one card.
        reviewBtn.isEnabled = !isEmpty
        reviewBtn.alpha = if (isEmpty) 0.45f else 1f
    }

    private fun openAddPanel() {
        addFront.setText("")
        addBack.setText("")
        swapToPanel(Panel.ADD)
    }

    private fun openEditFor(c: FlashCard) {
        editingCard = c
        editFront.setText(c.front)
        editBack.setText(c.back)
        swapToPanel(Panel.EDIT)
    }

    private fun confirmDelete(c: FlashCard) {
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Delete card")
            .setMessage("Delete this card from the deck?")
            .setPositiveButton("Delete") { _, _ -> vm.deleteCard(c) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun swapToPanel(target: Panel) {
        if (isAnimating || target == currentPanel) return

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


    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
