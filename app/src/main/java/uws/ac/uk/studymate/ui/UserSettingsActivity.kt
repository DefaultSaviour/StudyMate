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
    private var isUpdatingBiometricSwitch = false
    private lateinit var biometricManager: BiometricLoginManager

    // If true, the next successful POST_NOTIFICATIONS grant fires the test
    // notification right away (the user pressed the test button before granting).
    private var pendingTestFireAfterGrant = false

    // Permission launcher for POST_NOTIFICATIONS. Handles both the toggle-on
    // flow and the test-button flow.
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

            if (pendingTestFireAfterGrant) {
                pendingTestFireAfterGrant = false
                scheduleTestNotification()
            }
        } else {
            isUpdatingSwitch = true
            notificationsSwitch.isChecked = false
            isUpdatingSwitch = false
            pendingTestFireAfterGrant = false
            Toast.makeText(
                this,
                "Notification permission was denied. Enable it in Android Settings → Apps → StudyMate → Notifications.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private enum class Panel { LIST, EDIT }
    private var currentPanel = Panel.LIST
    private var isAnimating = false

    private lateinit var listElems: List<Pair<View, Float>>
    private lateinit var editElems: List<Pair<View, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_settings)
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
            val subjectWord = if (summary.subjectCount == 1) "subject" else "subjects"
            libraryText.text = "${summary.subjectCount} $subjectWord • ${summary.deckCount} $deckWord • ${summary.flashcardCount} $cardWord"

            val assignmentWord = if (summary.assignmentCount == 1) "assignment" else "assignments"
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
        }

        vm.sessionExpired.observe(this) { if (it) openLogin() }
        vm.accountDeleted.observe(this) {
            if (it) {
                // Only nuke the bio store if this user actually owned it.
                if (biometricManager.storedUserId() == currentUserIdOrZero()) {
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
                    if (!currentEmail.isNullOrEmpty() && newPassword.isNotEmpty() && ownerId > 0) {
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
            biometricRow                                        to -1f,
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
                .setMessage("You'll need to enter your email and password again to sign back in.")
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

        // Quick test for the notification pipeline — fires a reminder against the
        // user's first assignment so we don't have to wait for a real reminder
        // window. Debug-only: the row stays GONE in release builds.
        val testBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.testNotificationBtn)
        if (uws.ac.uk.studymate.BuildConfig.DEBUG) {
            testBtn.visibility = View.VISIBLE
            testBtn.setOnClickListener { scheduleTestNotification() }
        } else {
            testBtn.visibility = View.GONE
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
                // One-bio rule: if the slot is held by someone else, refuse.
                val owner = biometricManager.storedUserId()
                val current = vm.settingsSummary.value?.let { /* keeps Kotlin happy */ }
                val currentUserId = currentUserIdOrZero()
                if (biometricManager.isEnabled() && owner != currentUserId && owner > 0) {
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

    /**
     * Fires a notification immediately, bypassing WorkManager and the per-user
     * preference check. Surfaces every failure mode via a toast so we can tell
     * exactly which step is broken: permission, channel, notify(), etc.
     */
    private fun scheduleTestNotification() {
        // Step 1 — OS-level permission (Android 13+ requires it on top of
        // any in-app toggle). Request it directly if missing; the launcher
        // will fire the test once the user grants.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                // If Android won't show the dialog again (user denied twice),
                // we can't re-prompt — send them to system settings instead.
                val canAsk = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) || !hasEverRequestedNotificationPermission()
                if (canAsk) {
                    pendingTestFireAfterGrant = true
                    markNotificationPermissionRequested()
                    postNotificationsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Toast.makeText(
                        this,
                        "Android is blocking the permission dialog. Open Android Settings → Apps → StudyMate → Notifications and turn it on.",
                        Toast.LENGTH_LONG
                    ).show()
                    openAppNotificationSettings()
                }
                return
            }
        }

        // Step 2 — system-side notifications enabled at all? The user can
        // disable an app's notifications from system Settings even after
        // the runtime permission was granted.
        val nm = androidx.core.app.NotificationManagerCompat.from(this)
        if (!nm.areNotificationsEnabled()) {
            Toast.makeText(
                this,
                "Android Settings → StudyMate → Notifications is OFF. Enable it there.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Step 3 — channel exists?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val sysNm = getSystemService(android.app.NotificationManager::class.java)
            val ch = sysNm.getNotificationChannel(
                uws.ac.uk.studymate.StudyMateApplication.CHANNEL_ASSIGNMENT_REMINDERS
            )
            if (ch == null) {
                Toast.makeText(this, "Notification channel missing — bug.", Toast.LENGTH_LONG).show()
                return
            }
            if (ch.importance == android.app.NotificationManager.IMPORTANCE_NONE) {
                Toast.makeText(
                    this,
                    "Android Settings → StudyMate → Notifications → 'Assignment reminders' is OFF.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        // Step 4 — grab the user's first assignment (any will do for the test).
        val userId = uws.ac.uk.studymate.util.SessionManager(this).getLoggedInUserId()
        if (userId == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val db = uws.ac.uk.studymate.data.StudyMateDatabase.getInstance(applicationContext)
            val data = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val assignments = db.assignmentDao().getAssignments(userId)
                val user = db.userDao().getById(userId)
                Pair(assignments.firstOrNull(), user)
            }
            val target = data.first
            val user = data.second
            if (target == null) {
                Toast.makeText(
                    this@UserSettingsActivity,
                    "Create an assignment first",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            if (user == null) {
                Toast.makeText(this@UserSettingsActivity, "User missing", Toast.LENGTH_SHORT).show()
                return@launch
            }
            // Respect the in-app toggle the same way real reminders do — the
            // test button is for verifying the user-visible behaviour, not
            // for bypassing it.
            if (user.pushNotificationsEnabled != true) {
                Toast.makeText(
                    this@UserSettingsActivity,
                    "Turn on push notifications first.",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            // Step 5 — fire it directly via NotificationManagerCompat.
            val tapIntent = android.content.Intent(applicationContext, LoginActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(LoginActivity.EXTRA_NOTIFICATION_USERNAME, user.name)
            }
            val pi = android.app.PendingIntent.getActivity(
                applicationContext, 9999, tapIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val body = "${user.name}: ‘${target.title}’ — test reminder."
            val notif = androidx.core.app.NotificationCompat.Builder(
                applicationContext,
                uws.ac.uk.studymate.StudyMateApplication.CHANNEL_ASSIGNMENT_REMINDERS
            )
                .setSmallIcon(R.drawable.ic_studymate_logo)
                .setContentTitle("StudyMate test")
                .setContentText(body)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .build()
            try {
                nm.notify(9999, notif)
                Toast.makeText(
                    this@UserSettingsActivity,
                    "Notification fired. Check the shade.",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: SecurityException) {
                Toast.makeText(
                    this@UserSettingsActivity,
                    "notify() threw SecurityException: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun hasEverRequestedNotificationPermission(): Boolean =
        getPreferences(MODE_PRIVATE).getBoolean(KEY_NOTIF_PERM_REQUESTED, false)

    private fun markNotificationPermissionRequested() {
        getPreferences(MODE_PRIVATE).edit().putBoolean(KEY_NOTIF_PERM_REQUESTED, true).apply()
    }

    private fun openAppNotificationSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
        startActivity(intent)
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
        isUpdatingBiometricSwitch = true
        biometricSwitch.isChecked = biometricManager.isEnabled()
        isUpdatingBiometricSwitch = false
        biometricSubText.text = when (biometricManager.availability()) {
            BiometricLoginManager.Availability.NONE_ENROLLED ->
                "Set a screen lock in phone settings to enable"
            BiometricLoginManager.Availability.HW_UNAVAILABLE ->
                "Sensor temporarily unavailable"
            else ->
                if (biometricManager.isEnabled()) "Enabled for this device"
                else "Use fingerprint, face, or screen lock"
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
        floatOrb(findViewById(R.id.orb1), 14f, 3800L, 0L)
        floatOrb(findViewById(R.id.orb2), 17f, 4200L, 500L)
        floatOrb(findViewById(R.id.orb3), 12f, 3600L, 1000L)
        floatOrb(findViewById(R.id.orb4), 15f, 4000L, 300L)

        val d = resources.displayMetrics.density
        card.translationY = 200f * d
        card.alpha = 0f
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        card.animate().translationY(0f).alpha(1f).setDuration(540)
            .setInterpolator(DecelerateInterpolator(1.5f)).setStartDelay(60)
            .withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()
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

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(LoginActivity.EXTRA_SKIP_SPLASH, true)
        })
    }

    private fun openHome() {
        startActivity(Intent().setClassName(packageName, "$packageName.ui.HomeActivity"))
    }

    companion object {
        private const val KEY_NOTIF_PERM_REQUESTED = "notif_perm_requested"
    }
}
