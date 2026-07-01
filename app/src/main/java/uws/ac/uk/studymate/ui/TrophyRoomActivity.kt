package uws.ac.uk.studymate.ui

import android.content.Intent
import android.graphics.PorterDuff
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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.TrophyRoomViewModel
import uws.ac.uk.studymate.ui.viewmodels.TrophyUiState
import uws.ac.uk.studymate.util.TrophyProgress

/*//////////////////////
Trophy Room (1.2): a 2-column grid of tiles, one per trophy (see TrophyRoomViewModel
for how each value is computed). Locked trophies stay visible — dimmed to the same
alpha DeckListAdapter already uses for completed decks (0.55) — rather than being
hidden, so the room reads as "here's everything you could earn", not just what you
have. Tapping a tile panel-swaps (same slide choreography as DeckCardsActivity /
AssignmentsActivity) to a detail panel showing the trophy's description and all 5
ranks with their requirements.
 *//////////////////////
class TrophyRoomActivity : AppCompatActivity() {

    private lateinit var vm: TrophyRoomViewModel

    private lateinit var gridPanel: View
    private lateinit var detailPanel: View
    private lateinit var trophyContainer: LinearLayout
    private lateinit var trophyTitleText: View
    private lateinit var trophySubtitleText: View

    private lateinit var detailHeaderRow: LinearLayout
    private lateinit var detailAboutLabel: View
    private lateinit var detailDescriptionText: TextView
    private lateinit var detailRanksLabel: View
    private lateinit var detailRanksContainer: LinearLayout

    private enum class Panel { GRID, DETAIL }
    private var currentPanel = Panel.GRID
    private var isAnimating = false

    private lateinit var gridElems: List<Pair<View, Float>>
    private lateinit var detailElems: List<Pair<View, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trophy_room)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)

        bindViews()
        setupWindowInsets()
        setupBackHandler()

        val card = findViewById<MaterialCardView>(R.id.trophyCard)
        // The top icon is the ONLY back affordance on this screen — it's already
        // panel-aware (handleBack: DETAIL -> GRID, GRID -> finish()), so a separate
        // in-panel "Back to trophies" button would just duplicate it.
        findViewById<MaterialButton>(R.id.trophyBackBtn).setOnClickListener { handleBack() }

        uws.ac.uk.studymate.util.OrbField.scatter(card, listOf(findViewById(R.id.trophyBackBtn)))
        uws.ac.uk.studymate.util.Entrance.play(card)

        vm = ViewModelProvider(this)[TrophyRoomViewModel::class.java]
        vm.trophies.observe(this) { render(it) }
        vm.sessionExpired.observe(this) { if (it) openLogin() }
    }

    override fun onResume() {
        super.onResume()
        vm.loadTrophies()
    }

    private fun bindViews() {
        gridPanel = findViewById(R.id.gridPanel)
        detailPanel = findViewById(R.id.detailPanel)
        trophyContainer = findViewById(R.id.trophyContainer)
        trophyTitleText = findViewById(R.id.trophyTitleText)
        trophySubtitleText = findViewById(R.id.trophySubtitleText)

        detailHeaderRow = findViewById(R.id.detailHeaderRow)
        detailAboutLabel = findViewById(R.id.detailAboutLabel)
        detailDescriptionText = findViewById(R.id.detailDescriptionText)
        detailRanksLabel = findViewById(R.id.detailRanksLabel)
        detailRanksContainer = findViewById(R.id.detailRanksContainer)

        gridElems = listOf(trophyTitleText to -1f, trophySubtitleText to 1f, trophyContainer to -1f)
        detailElems = listOf(
            detailHeaderRow to -1f,
            detailAboutLabel to 1f,
            detailDescriptionText to -1f,
            detailRanksLabel to 1f,
            detailRanksContainer to -1f
        )
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })
    }

    private fun handleBack() {
        when (currentPanel) {
            Panel.GRID -> finish()
            Panel.DETAIL -> swapToPanel(Panel.GRID)
        }
    }

    private fun setupWindowInsets() {
        val card = findViewById<MaterialCardView>(R.id.trophyCard)
        ViewCompat.setOnApplyWindowInsetsListener(card) { _, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            gridPanel.setPadding(gridPanel.paddingLeft, gridPanel.paddingTop, gridPanel.paddingRight, navBar + base)
            detailPanel.setPadding(detailPanel.paddingLeft, detailPanel.paddingTop, detailPanel.paddingRight, navBar + base)
            insets
        }
    }

    private fun render(trophies: List<TrophyUiState>) {
        trophyContainer.removeAllViews()
        trophies.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            }
            pair.forEachIndexed { i, trophy ->
                row.addView(buildTrophyTile(trophy).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = if (i == 0) dp(5) else 0
                        marginStart = if (i == 1) dp(5) else 0
                    }
                })
            }
            if (pair.size == 1) {
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                })
            }
            trophyContainer.addView(row)
        }
    }

    private fun buildTrophyTile(trophy: TrophyUiState): View {
        val tierColor = ContextCompat.getColor(this, colorResFor(trophy.tier))
        val tierLabel = tierLabelFor(trophy.tier)
        val locked = trophy.tier == TrophyProgress.Tier.NONE
        val progressText = trophy.nextThreshold?.let {
            getString(R.string.trophy_progress_format, trophy.value, it, tierLabelFor(TrophyProgress.nextTier(trophy.tier)))
        } ?: getString(R.string.trophy_maxed_format, trophy.value)

        // 35% black glass-panel background — same tone as the Home dashboard's
        // outlined nav buttons (#59000000), so the trophy tiles read as consistently
        // "solid" against the wood as the rest of the app's chrome.
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_trophy_tile)
            setPadding(dp(12), dp(14), dp(12), dp(12))
            alpha = if (locked) 0.55f else 1f
            isClickable = true
            isFocusable = true
            val outValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = ContextCompat.getDrawable(context, outValue.resourceId)
        }

        val badge = buildIconBadge(trophy.iconRes, tierColor, dp(48), dp(24))
        val nameView = TextView(this).apply {
            text = trophy.name
            setTextColor(ContextCompat.getColor(context, R.color.surface))
            textSize = 14f
            gravity = Gravity.CENTER
            maxLines = 1
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        // Same text-pill treatment (@drawable/bg_text_pill_subtle) already used for
        // the deck "N due" badge and every other short status label in the app.
        val tierView = TextView(this).apply {
            text = tierLabel
            setTextColor(ContextCompat.getColor(context, R.color.gold_light))
            textSize = 11f
            background = ContextCompat.getDrawable(context, R.drawable.bg_text_pill_subtle)
            setPadding(dp(10), dp(3), dp(10), dp(3))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6); gravity = Gravity.CENTER_HORIZONTAL }
        }
        // Progress as plain text — matches the checklist's "N of M done" phrasing;
        // no screen in the app uses a bar graphic, so this doesn't invent one either.
        val progressView = TextView(this).apply {
            text = progressText
            setTextColor(ContextCompat.getColor(context, R.color.gold_light))
            textSize = 12f
            gravity = Gravity.CENTER
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }

        listOf(nameView, tierView, progressView).forEach {
            it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        tile.contentDescription = getString(R.string.cd_trophy_tile, trophy.name, tierLabel, progressText)

        tile.addView(badge)
        tile.addView(nameView)
        tile.addView(tierView)
        tile.addView(progressView)

        tile.setOnClickListener { showTrophyDetail(trophy) }

        return tile
    }

    // Rounded-rect tinted badge — the exact @drawable/bg_icon_badge treatment the
    // assignment icons already use (a plain colour-tinted rounded rect), not an
    // invented circular "medal" that appears nowhere else in the app. The icon
    // drawable comes from TrophyProgress.ROSTER, so a trophy is defined in one place.
    private fun buildIconBadge(iconRes: Int, tintColor: Int, badgeSizePx: Int, iconSizePx: Int): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(badgeSizePx, badgeSizePx)
            background = (ContextCompat.getDrawable(context, R.drawable.bg_icon_badge)?.mutate() as? GradientDrawable)?.apply {
                setColor(darken(tintColor, 0.85f))
            }
            addView(
                ImageView(this@TrophyRoomActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx, Gravity.CENTER)
                    setImageResource(iconRes)
                    setColorFilter(ContextCompat.getColor(context, R.color.navy), PorterDuff.Mode.SRC_IN)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            )
        }
    }

    // ── Detail panel: description + all 5 ranks for one trophy ──

    private fun showTrophyDetail(trophy: TrophyUiState) {
        populateDetailPanel(trophy)
        swapToPanel(Panel.DETAIL)
    }

    private fun populateDetailPanel(trophy: TrophyUiState) {
        val tierColor = ContextCompat.getColor(this, colorResFor(trophy.tier))
        val def = TrophyProgress.ROSTER.first { it.id == trophy.id }

        detailHeaderRow.removeAllViews()
        detailHeaderRow.addView(buildIconBadge(trophy.iconRes, tierColor, dp(56), dp(28)))
        detailHeaderRow.addView(
            TextView(this).apply {
                text = trophy.name
                setTextColor(ContextCompat.getColor(context, R.color.surface))
                textSize = 20f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply { marginStart = dp(14) }
            }
        )

        detailDescriptionText.text = trophy.description

        detailRanksContainer.removeAllViews()
        val tiers = listOf(
            TrophyProgress.Tier.BRONZE, TrophyProgress.Tier.SILVER, TrophyProgress.Tier.GOLD,
            TrophyProgress.Tier.PLATINUM, TrophyProgress.Tier.DIAMOND
        )
        tiers.forEachIndexed { i, tier ->
            val requirement = def.thresholds[i]
            val achieved = trophy.value >= requirement
            detailRanksContainer.addView(buildRankRow(trophy, tier, requirement, achieved))
        }
    }

    private fun buildRankRow(trophy: TrophyUiState, tier: TrophyProgress.Tier, requirement: Int, achieved: Boolean): View {
        val tierColor = ContextCompat.getColor(this, colorResFor(tier))
        val tierLabel = tierLabelFor(tier)
        val statusText = if (achieved) getString(R.string.trophy_rank_achieved)
        else getString(R.string.trophy_rank_requirement_format, requirement)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_trophy_tile)
            setPadding(dp(10), dp(10), dp(12), dp(10))
            alpha = if (achieved) 1f else 0.6f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }

        row.addView(buildIconBadge(trophy.iconRes, tierColor, dp(36), dp(18)))

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            }
        }
        textColumn.addView(
            TextView(this).apply {
                text = tierLabel
                setTextColor(ContextCompat.getColor(context, R.color.surface))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        )
        textColumn.addView(
            TextView(this).apply {
                text = statusText
                setTextColor(if (achieved) tierColor else ContextCompat.getColor(context, R.color.gold_light))
                textSize = 12f
            }
        )
        row.addView(textColumn)

        row.addView(
            ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
                setImageResource(if (achieved) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
                setColorFilter(
                    ContextCompat.getColor(context, if (achieved) R.color.success_text else R.color.gold_light),
                    PorterDuff.Mode.SRC_IN
                )
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        )

        row.contentDescription = getString(R.string.cd_trophy_rank_row, tierLabel, statusText, if (achieved) "achieved" else "not yet achieved")
        return row
    }

    // ── Panel-swap animation (same choreography as DeckCardsActivity / AssignmentsActivity) ──

    private fun swapToPanel(target: Panel) {
        if (isAnimating || target == currentPanel) return

        val outgoingPanel = panelView(currentPanel)
        val incomingPanel = panelView(target)
        val outgoingElems = panelElems(currentPanel)
        val incomingElems = panelElems(target)

        val goingForward = currentPanel == Panel.GRID
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
        Panel.GRID -> gridPanel
        Panel.DETAIL -> detailPanel
    }

    private fun panelElems(p: Panel): List<Pair<View, Float>> = when (p) {
        Panel.GRID -> gridElems
        Panel.DETAIL -> detailElems
    }

    private fun tierLabelFor(tier: TrophyProgress.Tier): String = getString(
        when (tier) {
            TrophyProgress.Tier.NONE -> R.string.trophy_tier_none
            TrophyProgress.Tier.BRONZE -> R.string.trophy_tier_bronze
            TrophyProgress.Tier.SILVER -> R.string.trophy_tier_silver
            TrophyProgress.Tier.GOLD -> R.string.trophy_tier_gold
            TrophyProgress.Tier.PLATINUM -> R.string.trophy_tier_platinum
            TrophyProgress.Tier.DIAMOND -> R.string.trophy_tier_diamond
        }
    )

    private fun colorResFor(tier: TrophyProgress.Tier): Int = when (tier) {
        TrophyProgress.Tier.NONE -> R.color.trophy_locked
        TrophyProgress.Tier.BRONZE -> R.color.trophy_bronze
        TrophyProgress.Tier.SILVER -> R.color.trophy_silver
        TrophyProgress.Tier.GOLD -> R.color.trophy_gold_tier
        TrophyProgress.Tier.PLATINUM -> R.color.trophy_platinum
        TrophyProgress.Tier.DIAMOND -> R.color.trophy_diamond
    }

    // Darkens a colour by scaling its HSV brightness — used for the icon badge fill
    // so the tier colour reads as a tinted glass panel rather than a flat sticker.
    private fun darken(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[2] *= factor
        return android.graphics.Color.HSVToColor(android.graphics.Color.alpha(color), hsv)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun openLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
