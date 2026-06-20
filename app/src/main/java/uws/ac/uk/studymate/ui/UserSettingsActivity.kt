package uws.ac.uk.studymate.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.UserSettingsViewModel
import uws.ac.uk.studymate.util.BiometricLoginManager

/*//////////////////////
Coded by Jamie Coleman
06/04/26
redesigned 18/04/26 — wood-glass UI, edit/delete account, panel-swap
 *//////////////////////
class UserSettingsActivity : AppCompatActivity() {

    private lateinit var vm: UserSettingsViewModel
    private lateinit var card: MaterialCardView
    private lateinit var listPanel: View
    private lateinit var editPanel: View

    private lateinit var titleText: TextView
    private lateinit var subText: TextView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var memberSinceText: TextView
    private lateinit var libraryText: TextView
    private lateinit var assignmentsText: TextView
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var autoLoginRow: View
    private lateinit var autoLoginSwitch: SwitchCompat
    private lateinit var biometricRow: View
    private lateinit var biometricSwitch: SwitchCompat
    private lateinit var biometricSubText: TextView
    private lateinit var logoutBtn: MaterialButton
    private lateinit var editAccountBtn: MaterialButton
    private lateinit var deleteAccountBtn: MaterialButton

    private lateinit var editNameInput: TextInputEditText
    private lateinit var editEmailInput: TextInputEditText
    private lateinit var editNewPasswordInput: TextInputEditText
    private lateinit var editConfirmPasswordInput: TextInputEditText
    private lateinit var editConfirmBtn: MaterialButton
    private lateinit var editCancelBtn: MaterialButton
    private lateinit var editDeleteBtn: MaterialButton

    private var isUpdatingSwitch = false
    private var isUpdatingAutoLoginSwitch = false
    private var isUpdatingBiometricSwitch = false
    private lateinit var biometricManager: BiometricLoginManager

    // Permission launcher for POST_NOTIFICATIONS, used by the notifications toggle.
    private val postNotificationsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Make sure the in-app toggle reflects "on" since the user just
            // proved they want notifications.
            if (!notificationsSwitch.isChecked) {
                isUpdatingSwitch = true
                notificationsSwitch.isChecked = true
                isUpdatingSwitch = false
            }
            vm.updatePushNotifications(true)
        } else {
            isUpdatingSwitch = true
            notificationsSwitch.isChecked = false
            isUpdatingSwitch = false
            Toast.makeText(
                this,
                "Notification permission was denied. Enable it in Android Settings → Apps → StudyMate → Notifications.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Storage Access Framework launchers for data backup. Neither needs a runtime
    // storage permission on API 30+ — the system picker grants per-URI access.
    private val exportDataLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) vm.exportTo(contentResolver, uri)
    }
    private val importDataLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importFrom(contentResolver, uri)
    }

    private enum class Panel { LIST, EDIT }
    private var currentPanel = Panel.LIST
    private var isAnimating = false

    private lateinit var listElems: List<Pair<View, Float>>
    private lateinit var editElems: List<Pair<View, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_settings)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)
        uws.ac.uk.studymate.util.KeyboardInsets.apply(this)

        vm = ViewModelProvider(this)[UserSettingsViewModel::class.java]
        biometricManager = BiometricLoginManager(this)

        bindViews()
        setupClicks()
        setupBackHandler()
        setupWindowInsets()
        runEntranceAnimation()

        vm.settingsSummary.observe(this) { summary ->
            titleText.text = summary.titleText
            nameText.text = summary.userName
            memberSinceText.text = summary.memberSinceText

            val deckWord = if (summary.deckCount == 1) "deck" else "decks"
            val cardWord = if (summary.flashcardCount == 1) "card" else "cards"
            val assignmentWord = if (summary.assignmentCount == 1) "assignment" else "assignments"
            libraryText.text = "${summary.assignmentCount} $assignmentWord • ${summary.deckCount} $deckWord • ${summary.flashcardCount} $cardWord"

            val dueText = when (summary.assignmentsDueThisWeek) {
                0 -> "none due this week"
                1 -> "1 due this week"
                else -> "${summary.assignmentsDueThisWeek} due this week"
            }
            assignmentsText.text = "${summary.assignmentCount} $assignmentWord — $dueText"

            // Preload the edit form with the current values.
            if (currentPanel == Panel.LIST) {
                editNameInput.setText(summary.userName)
            }

            isUpdatingSwitch = true
            notificationsSwitch.isChecked = summary.notificationsEnabled
            isUpdatingSwitch = false

            // Auto sign-in is only meaningful for password accounts (there's no
            // password to skip for a biometric-only account) — hide it otherwise.
            autoLoginRow.visibility = if (summary.isPasswordAccount) View.VISIBLE else View.GONE
            isUpdatingAutoLoginSwitch = true
            autoLoginSwitch.isChecked = summary.autoLoginEnabled
            isUpdatingAutoLoginSwitch = false
        }

        vm.sessionExpired.observe(this) { if (it) openLogin() }
        vm.accountDeleted.observe(this) { deletedUserId ->
            if (deletedUserId > 0) {
                // Only nuke the bio store if the *deleted* user actually owned it.
                // The session is already cleared by now, so compare against the
                // id the VM captured before logout — not the live session (which
                // would read 0 and silently orphan the credentials).
                if (biometricManager.storedUserId() == deletedUserId) {
                    biometricManager.clearCredentials()
                }
                uws.ac.uk.studymate.util.SessionManager(this).clearLastUserId()
                openLogin()
            }
        }
        vm.message.observe(this) { msg ->
            if (msg.isNullOrBlank()) return@observe
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            if (msg == "Account updated" || msg == "Account and password updated") {
                // If the user changed their password while biometric login was
                // enabled, the stored credentials are now stale — re-save them
                // with the new password so the next biometric attempt still works.
                if (msg == "Account and password updated" && biometricManager.isEnabled()) {
                    val currentEmail = biometricManager.storedEmail()
                    val ownerId = biometricManager.storedUserId()
                    val newPassword = editNewPasswordInput.text?.toString().orEmpty()
                    // Only refresh the stored credentials if the signed-in user
                    // is the one who owns the biometric slot — otherwise we'd
                    // overwrite another account's credentials with this password.
                    if (!currentEmail.isNullOrEmpty() && newPassword.isNotEmpty() &&
                        ownerId > 0 && ownerId == currentUserIdOrZero()
                    ) {
                        biometricManager.saveCredentials(ownerId, currentEmail, newPassword)
                    }
                }
                swapToPanel(Panel.LIST)
            }
        }
        vm.biometricCredentialsToSave.observe(this) { creds ->
            if (creds == null) return@observe
            biometricManager.saveCredentials(creds.userId, creds.email, creds.password)
            vm.consumeBiometricCredentials()
            refreshBiometricRow()
            Toast.makeText(this, "Fingerprint login enabled", Toast.LENGTH_SHORT).show()
        }
        vm.dataOpResult.observe(this) { result ->
            if (result == null) return@observe
            val msg = when (result) {
                is UserSettingsViewModel.DataOpResult.ExportSuccess ->
                    "Backup saved — ${result.assignments} assignments, ${result.decks} decks, ${result.cards} cards"
                is UserSettingsViewModel.DataOpResult.ImportSuccess -> {
                    val s = result.summary
                    "Imported ${s.assignments} new assignments, ${s.decks} decks, ${s.cards} cards"
                }
                is UserSettingsViewModel.DataOpResult.Error -> result.message
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            vm.consumeDataOpResult()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.loadSettings()
        // Sensor state can change while the app is backgrounded (user added/
        // removed a fingerprint) — refresh on every resume.
        if (::biometricManager.isInitialized) refreshBiometricRow()
    }

    private fun bindViews() {
        card = findViewById(R.id.settingsCard)
        listPanel = findViewById(R.id.listPanel)
        editPanel = findViewById(R.id.editPanel)

        titleText = findViewById(R.id.settingsTitleText)
        subText = findViewById(R.id.settingsSubText)
        nameText = findViewById(R.id.settingsNameText)
        emailText = findViewById(R.id.settingsEmailText)
        memberSinceText = findViewById(R.id.settingsMemberSinceText)
        libraryText = findViewById(R.id.settingsLibraryText)
        assignmentsText = findViewById(R.id.settingsAssignmentsText)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        autoLoginRow = findViewById(R.id.autoLoginRow)
        autoLoginSwitch = findViewById(R.id.autoLoginSwitch)
        biometricRow = findViewById(R.id.biometricRow)
        biometricSwitch = findViewById(R.id.biometricSwitch)
        biometricSubText = findViewById(R.id.biometricSubText)
        logoutBtn = findViewById(R.id.logoutBtn)
        editAccountBtn = findViewById(R.id.editAccountBtn)
        deleteAccountBtn = findViewById(R.id.deleteAccountBtn)

        editNameInput = findViewById(R.id.editNameInput)
        editEmailInput = findViewById(R.id.editEmailInput)
        editNewPasswordInput = findViewById(R.id.editNewPasswordInput)
        editConfirmPasswordInput = findViewById(R.id.editConfirmPasswordInput)
        editConfirmBtn = findViewById(R.id.editConfirmBtn)
        editCancelBtn = findViewById(R.id.editCancelBtn)
        editDeleteBtn = findViewById(R.id.editDeleteBtn)

        listElems = listOf(
            titleText                                          to -1f,
            subText                                             to  1f,
            findViewById<View>(R.id.accountSectionLabel)       to -1f,
            findViewById<View>(R.id.accountRow)                to  1f,
            findViewById<View>(R.id.prefSectionLabel)          to -1f,
            findViewById<View>(R.id.prefRow)                   to  1f,
            autoLoginRow                                        to  1f,
            biometricRow                                        to -1f,
            findViewById<View>(R.id.dataSectionLabel)          to -1f,
            findViewById<View>(R.id.exportDataRow)             to  1f,
            findViewById<View>(R.id.importDataRow)             to -1f,
            findViewById<View>(R.id.glanceSectionLabel)        to -1f,
            findViewById<View>(R.id.glanceRow)                 to  1f,
            logoutBtn                                           to -1f
        )
        editElems = listOf(
            findViewById<View>(R.id.editTitleText)              to -1f,
            findViewById<View>(R.id.editSubText)                to  1f,
            findViewById<View>(R.id.editNameLayout)             to -1f,
            findViewById<View>(R.id.editEmailLayout)            to  1f,
            findViewById<View>(R.id.editPasswordLabel)          to -1f,
            findViewById<View>(R.id.editNewPasswordLayout)      to  1f,
            findViewById<View>(R.id.editConfirmPasswordLayout)  to -1f,
            editConfirmBtn                                       to  1f,
            editCancelBtn                                        to -1f,
            findViewById<View>(R.id.dangerZoneLabel)             to  1f,
            editDeleteBtn                                        to -1f
        )
    }

    private fun setupClicks() {
        findViewById<MaterialButton>(R.id.homeBtn).setOnClickListener {
            if (currentPanel == Panel.EDIT) swapToPanel(Panel.LIST) else openHome()
        }

        editAccountBtn.setOnClickListener {
            // Clear stale password fields so the user doesn't think they're already populated.
            editNewPasswordInput.setText("")
            editConfirmPasswordInput.setText("")
            swapToPanel(Panel.EDIT)
        }
        deleteAccountBtn.setOnClickListener { confirmDeleteAccount() }
        editDeleteBtn.setOnClickListener { confirmDeleteAccount() }
        editCancelBtn.setOnClickListener { swapToPanel(Panel.LIST) }
        editConfirmBtn.setOnClickListener {
            vm.updateAccount(
                editNameInput.text?.toString().orEmpty(),
                editNewPasswordInput.text?.toString().orEmpty(),
                editConfirmPasswordInput.text?.toString().orEmpty()
            )
        }

        logoutBtn.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
                .setTitle("Sign out")
                .setMessage("You'll need to enter your username and password again to sign back in.")
                .setPositiveButton("Sign out") { _, _ ->
                    // Keep the encrypted biometric credentials — they're how
                    // the user signs back in. Only clear them on account
                    // deletion or when the user explicitly disables the
                    // "Quick sign-in" toggle. Clear the "last user" pointer
                    // too so the next launch goes to the full sign-in screen
                    // (the user explicitly asked to leave).
                    vm.logout()
                    uws.ac.uk.studymate.util.SessionManager(this).clearLastUserId()
                    openLogin()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<View>(R.id.exportDataRow).setOnClickListener {
            val date = java.time.LocalDate.now()
            exportDataLauncher.launch("studymate_backup_$date.json")
        }
        findViewById<View>(R.id.importDataRow).setOnClickListener {
            // Some providers report JSON as a generic type, so accept a few.
            importDataLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitch) return@setOnCheckedChangeListener
            if (isChecked) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                        this, android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        vm.updatePushNotifications(true)
                        Toast.makeText(this, "Notifications on", Toast.LENGTH_SHORT).show()
                    } else {
                        markNotificationPermissionRequested()
                        postNotificationsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    vm.updatePushNotifications(true)
                    Toast.makeText(this, "Notifications on", Toast.LENGTH_SHORT).show()
                }
            } else {
                vm.updatePushNotifications(false)
                Toast.makeText(this, "Notifications off", Toast.LENGTH_SHORT).show()
            }
        }

        autoLoginSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingAutoLoginSwitch) return@setOnCheckedChangeListener
            vm.updateAutoLogin(isChecked)
            Toast.makeText(
                this,
                if (isChecked) "Auto sign-in on" else "Auto sign-in off",
                Toast.LENGTH_SHORT
            ).show()
        }

        refreshBiometricRow()
        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingBiometricSwitch) return@setOnCheckedChangeListener
            if (isChecked) {
                if (!biometricManager.hasDeviceLock()) {
                    revertBiometricSwitch(false)
                    Toast.makeText(
                        this,
                        "Set a screen lock in your phone settings first",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnCheckedChangeListener
                }
                // One-bio rule: if the slot is held by another account, refuse.
                // (The switch is also disabled in that case; this is a backstop.)
                if (biometricManager.isOwnedByAnotherUser(currentUserIdOrZero())) {
                    revertBiometricSwitch(false)
                    Toast.makeText(
                        this,
                        "Another account on this device already uses fingerprint/screen-lock sign-in",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnCheckedChangeListener
                }
                promptForPasswordToEnableBiometric()
            } else {
                biometricManager.clearCredentials()
                refreshBiometricRow()
                Toast.makeText(this, "Fingerprint login disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markNotificationPermissionRequested() {
        getPreferences(MODE_PRIVATE).edit().putBoolean(KEY_NOTIF_PERM_REQUESTED, true).apply()
    }

    /** The currently-signed-in user's id, or 0 before the VM has loaded. */
    private fun currentUserIdOrZero(): Int {
        // We don't carry user id directly in the summary, but session manager has it.
        val sm = uws.ac.uk.studymate.util.SessionManager(this)
        return sm.getLoggedInUserId() ?: 0
    }

    /** Updates the biometric row's visibility + switch state from the manager. */
    private fun refreshBiometricRow() {
        // The row is for any device unlock method (fingerprint / face / pattern /
        // PIN / password) — only hide it if the phone has no lock at all.
        if (!biometricManager.hasDeviceLock()) {
            biometricRow.visibility = View.GONE
            return
        }
        biometricRow.visibility = View.VISIBLE

        // Quick sign-in is a single per-device slot owned by exactly one account.
        // Show the toggle's state for THIS account only — never the global flag —
        // and don't offer it at all when another account already owns the slot.
        val uid = currentUserIdOrZero()
        val ownedByMe = biometricManager.isEnabledForUser(uid)
        val ownedByOther = biometricManager.isOwnedByAnotherUser(uid)

        isUpdatingBiometricSwitch = true
        biometricSwitch.isChecked = ownedByMe
        biometricSwitch.isEnabled = !ownedByOther
        isUpdatingBiometricSwitch = false
        biometricRow.alpha = if (ownedByOther) 0.5f else 1f

        biometricSubText.text = when {
            ownedByOther ->
                "Another account on this device already uses quick sign-in"
            biometricManager.availability() == BiometricLoginManager.Availability.NONE_ENROLLED ->
                "Set a screen lock in phone settings to enable"
            biometricManager.availability() == BiometricLoginManager.Availability.HW_UNAVAILABLE ->
                "Sensor temporarily unavailable"
            ownedByMe -> "Enabled for this account"
            else -> "Use fingerprint, face, or screen lock"
        }
    }

    private fun revertBiometricSwitch(targetChecked: Boolean) {
        isUpdatingBiometricSwitch = true
        biometricSwitch.isChecked = targetChecked
        isUpdatingBiometricSwitch = false
    }

    private fun promptForPasswordToEnableBiometric() {
        val container = layoutInflater.inflate(R.layout.dialog_biometric_password, null)
        val input = container.findViewById<TextInputEditText>(R.id.biometricPasswordInput)

        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Confirm your password")
            .setMessage("Re-enter your password so we can store it securely on this device.")
            .setView(container)
            .setPositiveButton("Enable", null)
            .setNegativeButton("Cancel") { _, _ -> revertBiometricSwitch(false) }
            .setOnCancelListener { revertBiometricSwitch(false) }
            .show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pw = input.text?.toString().orEmpty()
            if (pw.isBlank()) {
                container.findViewById<TextInputLayout>(R.id.biometricPasswordLayout).error =
                    "Enter your password"
                return@setOnClickListener
            }
            vm.verifyPasswordForBiometric(pw)
            dialog.dismiss()
        }
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Delete account")
            .setMessage(
                "This will permanently delete your account and every subject, assignment, " +
                "deck and flashcard tied to it. This cannot be undone. Continue?"
            )
            .setPositiveButton("Delete") { _, _ -> vm.deleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentPanel) {
                    Panel.LIST -> openHome()
                    Panel.EDIT -> swapToPanel(Panel.LIST)
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
            editPanel.setPadding(0, 0, 0, navBar + base)
            insets
        }
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
        Panel.EDIT -> editPanel
    }

    private fun panelElems(p: Panel): List<Pair<View, Float>> = when (p) {
        Panel.LIST -> listElems
        Panel.EDIT -> editElems
    }

    private fun runEntranceAnimation() {
        uws.ac.uk.studymate.util.OrbField.scatter(
            findViewById(R.id.settingsCard),
            listOf(findViewById(R.id.homeBtn))
        )

        val d = resources.displayMetrics.density
        card.translationY = 200f * d
        card.alpha = 0f
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        card.animate().translationY(0f).alpha(1f).setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f)).setStartDelay(60)
            .withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()
    }


    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(LoginActivity.EXTRA_SKIP_SPLASH, true)
        })
    }

    private fun openHome() {
        // Return to the existing Dashboard instead of launching a new one, so the
        // back stack stays a clean Dashboard -> screen -> sub-screen hierarchy.
        finish()
    }

    companion object {
        private const val KEY_NOTIF_PERM_REQUESTED = "notif_perm_requested"
    }
}
