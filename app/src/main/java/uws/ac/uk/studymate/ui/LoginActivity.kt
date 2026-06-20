package uws.ac.uk.studymate.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.ui.viewmodels.LoginViewModel
import uws.ac.uk.studymate.ui.viewmodels.RegisterViewModel
import uws.ac.uk.studymate.util.AssignmentIcons
import uws.ac.uk.studymate.util.BiometricLoginManager
import uws.ac.uk.studymate.util.SessionManager

/*//////////////////////
Multi-user, one-bio sign-in router.

Three panels live in the card:
  signInPanel          — username + password (+ biometric quick button if the
                         device's biometric account is set up).
  signupChoosePanel    — username only + two buttons:
                           "Use fingerprint or screen lock" (creates the bio
                            account, only available if no one else holds it)
                           "Set a password instead" (slides to password panel)
  signupPasswordPanel  — password + confirm + create.

Routing on launch:
  no users at all   → signupChoosePanel
  any user exists   → signInPanel

Every cold start invalidates the cached session — the user always
re-authenticates.
 *//////////////////////
class LoginActivity : AppCompatActivity() {

    private lateinit var loginVm: LoginViewModel
    private lateinit var registerVm: RegisterViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var biometricManager: BiometricLoginManager
    private lateinit var userRepo: UserRepo

    private val iconCycleHandler = Handler(Looper.getMainLooper())
    private var iconCycleRunnable: Runnable? = null

    private lateinit var signInPanel: View
    private lateinit var signupChoosePanel: View
    private lateinit var signupPasswordPanel: View

    private var pendingSignupName: String = ""
    private var biometricInFlight: Boolean = false

    // When we're in the minimal returning-user view, this holds the name we
    // should sign in with (since the username field is hidden).
    private var returningUserName: String? = null

    // True once the splash flip-cycle + fade-out have completed (or right away
    // when we re-enter from sign-out and skip the splash). Auto-launched
    // biometric prompts wait for this so they don't pop while the splash is
    // still on screen.
    private var splashDone: Boolean = false
    private var pendingBiometricAutoLaunch: Boolean = false

    // Auto-login: when the returning password account has it enabled we sign the
    // session in immediately but defer opening Home until the splash has fully
    // played, so the user still sees the branded launch (not an abrupt jump).
    private var pendingAutoLaunchHome: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Always call installSplashScreen so the post-splash theme swap runs
        // (otherwise the activity keeps the black splash background). When
        // re-entering from sign-out we release the splash gate immediately
        // so the user doesn't sit through the 3-second flip again.
        val skipSplash = intent.getBooleanExtra(EXTRA_SKIP_SPLASH, false)

        val splash = installSplashScreen()
        var splashHoldDone = skipSplash
        splash.setKeepOnScreenCondition { !splashHoldDone }

        if (!skipSplash) {
            splash.setOnExitAnimationListener { splashView ->
                val iconView = splashView.iconView
                val density = resources.displayMetrics.density
                iconView.cameraDistance = 14000f * density

                val frames = listOf(
                    R.drawable.app_logo_1,
                    R.drawable.app_logo_2,
                    R.drawable.app_logo_3
                )
                var idx = 0

                // The system splash renders the first icon at the launcher-icon
                // size (~192dp). When we replace the drawable via setImageResource
                // the ImageView falls back to its own scaleType and the next icons
                // come out smaller. Bump the scale on every swap so each frame
                // matches the size of the original system-splash icon.
                val matchSize = 1.45f

                val handler = Handler(Looper.getMainLooper())
                // The very first flip hides the system-splash icon. We shorten
                // its outgoing rotation so the user sees logo_1 disappear faster
                // (the system splash already held it for its own minimum duration).
                var firstFlip = true
                val flip = object : Runnable {
                    override fun run() {
                        idx = (idx + 1) % frames.size
                        val outDur = if (firstFlip) 90L else 190L
                        firstFlip = false
                        iconView.animate()
                            .rotationX(90f)
                            .setDuration(outDur)
                            .setInterpolator(AccelerateInterpolator(1.5f))
                            .withEndAction {
                                (iconView as? ImageView)?.setImageResource(frames[idx])
                                iconView.scaleX = matchSize
                                iconView.scaleY = matchSize
                                iconView.rotationX = -90f
                                iconView.animate()
                                    .rotationX(0f)
                                    .setDuration(190)
                                    .setInterpolator(DecelerateInterpolator(1.5f))
                                    .start()
                            }
                            .start()
                    }
                }
                handler.post(flip)
                handler.postDelayed(flip, 500)
                handler.postDelayed(flip, 1000)

                handler.postDelayed({
                    // Kick off the post-splash actions (incl. auto-launching the
                    // biometric prompt) at the START of the fade so the system
                    // sheet has time to render behind the fading splash.
                    onSplashFinished()
                    splashView.view.animate()
                        .alpha(0f)
                        .setDuration(280)
                        .withEndAction { splashView.remove() }
                        .start()
                }, 1500)
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)

        sessionManager = SessionManager(this)
        biometricManager = BiometricLoginManager(this)
        userRepo = UserRepo(StudyMateDatabase.getInstance(this))
        uws.ac.uk.studymate.util.KeyboardInsets.apply(this)

        // Cold launch / re-entry both invalidate any cached session.
        sessionManager.logout()

        loginVm    = ViewModelProvider(this)[LoginViewModel::class.java]
        registerVm = ViewModelProvider(this)[RegisterViewModel::class.java]

        bindPanels()
        wireSignInPanel()
        wireSignupChoosePanel()
        wireSignupPasswordPanel()
        observeViewModels()

        runEntranceAnimation()
        startIconCycle()

        // For the no-splash path the gate is already open. For the splash path
        // open it once the first frame settles.
        if (!skipSplash) {
            window.decorView.post { splashHoldDone = true }
        } else {
            splashDone = true
        }

        decidePanelAsync()
    }

    /** Called when the splash flip-cycle + fade have fully completed. */
    private fun onSplashFinished() {
        splashDone = true
        if (pendingAutoLaunchHome) {
            pendingAutoLaunchHome = false
            openHome()
            return
        }
        if (pendingBiometricAutoLaunch) {
            pendingBiometricAutoLaunch = false
            launchBiometricLogin()
        }
    }

    private fun bindPanels() {
        signInPanel         = findViewById(R.id.signInPanel)
        signupChoosePanel   = findViewById(R.id.signupChoosePanel)
        signupPasswordPanel = findViewById(R.id.signupPasswordPanel)

        signInPanel.visibility = View.INVISIBLE
        signupChoosePanel.visibility = View.INVISIBLE
        signupPasswordPanel.visibility = View.INVISIBLE
    }

    private fun decidePanelAsync() {
        val skipSplash = intent.getBooleanExtra(EXTRA_SKIP_SPLASH, false)
        val notifUsername = intent.getStringExtra(EXTRA_NOTIFICATION_USERNAME)
        lifecycleScope.launch {
            val state = withContext(Dispatchers.IO) {
                val all = userRepo.getAllUsers()
                val lastId = sessionManager.getLastUserId()
                val last = lastId?.let { id -> all.firstOrNull { it.id == id } }
                val notifUser = notifUsername?.let { name ->
                    all.firstOrNull { it.name.equals(name, ignoreCase = true) }
                }
                Quadruple(
                    anyUsers = all.isNotEmpty(),
                    hasPasswordUsers = all.any { it.authMode == SessionManager.AUTH_MODE_PASSWORD },
                    bioOwnerExists = all.any { it.authMode == SessionManager.AUTH_MODE_BIOMETRIC_ONLY },
                    lastUser = last,
                    notificationUser = notifUser
                )
            }
            when {
                // A notification was tapped — sign in as that user.
                state.notificationUser != null -> showReturningSignIn(
                    state.notificationUser.name,
                    state.notificationUser.id,
                    state.notificationUser.authMode == SessionManager.AUTH_MODE_BIOMETRIC_ONLY
                )
                // Cold launch + a remembered PASSWORD account with auto-login on
                // → bypass the password and sign straight back in. (Biometric
                // accounts always keep their prompt — bypassing it would defeat
                // the whole point of that mode.)
                !skipSplash && state.lastUser != null &&
                    state.lastUser.authMode == SessionManager.AUTH_MODE_PASSWORD &&
                    state.lastUser.autoLoginEnabled -> {
                    withContext(Dispatchers.IO) {
                        sessionManager.login(state.lastUser.id)
                        sessionManager.setLastUserId(state.lastUser.id)
                    }
                    if (splashDone) openHome() else pendingAutoLaunchHome = true
                }
                // Cold launch + a remembered user → straight to their prompt.
                !skipSplash && state.lastUser != null -> showReturningSignIn(
                    state.lastUser.name,
                    state.lastUser.id,
                    state.lastUser.authMode == SessionManager.AUTH_MODE_BIOMETRIC_ONLY
                )
                state.anyUsers -> showSignInPanel(state.hasPasswordUsers, state.bioOwnerExists)
                else -> showSignupChoosePanel(bioTaken = false)
            }
        }
    }

    // Small carrier for the launch-time DB lookup result.
    private data class Quadruple(
        val anyUsers: Boolean,
        val hasPasswordUsers: Boolean,
        val bioOwnerExists: Boolean,
        val lastUser: uws.ac.uk.studymate.data.entities.User?,
        val notificationUser: uws.ac.uk.studymate.data.entities.User?
    )

    // ── Sign-in panel ─────────────────────────────────────────────────────────

    private fun wireSignInPanel() {
        val usernameInput      = findViewById<EditText>(R.id.usernameInput)
        val passwordInput      = findViewById<EditText>(R.id.passwordInput)
        val loginBtn           = findViewById<MaterialButton>(R.id.loginBtn)
        val biometricBtn       = findViewById<MaterialButton>(R.id.biometricBtn)
        val createNewBtn       = findViewById<MaterialButton>(R.id.createNewAccountBtn)
        val differentAccount   = findViewById<TextView>(R.id.differentAccountLink)

        loginBtn.setOnClickListener {
            // In the minimal returning-user view the username field is hidden,
            // so we use the remembered name from the last sign-in instead.
            val name = returningUserName ?: usernameInput.text.toString()
            loginVm.signInWithPassword(name, passwordInput.text.toString())
        }
        biometricBtn.setOnClickListener { launchBiometricLogin() }
        createNewBtn.setOnClickListener {
            lifecycleScope.launch {
                val bioTaken = withContext(Dispatchers.IO) {
                    userRepo.getBiometricOnlyUser() != null
                }
                showSignupChoosePanel(bioTaken)
            }
        }
        differentAccount.setOnClickListener {
            // Drop the remembered user and switch to the full picker.
            returningUserName = null
            showSignInPanelFresh()
        }
    }

    /**
     * Full sign-in panel.
     *  - Username + password + Sign In are shown only when a password account
     *    actually exists on the device.
     *  - Biometric quick-button is shown only when the device's biometric slot
     *    is taken and the platform can run the prompt.
     *  - "Create new account" is always available so the user can add one.
     */
    private fun showSignInPanel(hasPasswordUsers: Boolean, bioOwnerExists: Boolean) {
        showOnlyPanel(signInPanel)
        returningUserName = null
        findViewById<TextView>(R.id.cardSubText).text = "Welcome back"
        findViewById<EditText>(R.id.usernameInput).setText("")
        findViewById<EditText>(R.id.passwordInput).setText("")
        findViewById<TextView>(R.id.loginMessage).visibility = View.GONE

        val pwVis = if (hasPasswordUsers) View.VISIBLE else View.GONE
        findViewById<View>(R.id.usernameInputLayout).visibility = pwVis
        findViewById<View>(R.id.passwordInputLayout).visibility = pwVis
        findViewById<View>(R.id.loginBtn).visibility = pwVis

        findViewById<View>(R.id.signInDivider).visibility = View.VISIBLE
        findViewById<View>(R.id.createNewAccountBtn).visibility = View.VISIBLE
        findViewById<View>(R.id.differentAccountLink).visibility = View.GONE

        val biometricBtn = findViewById<MaterialButton>(R.id.biometricBtn)
        val hasBio = bioOwnerExists &&
            biometricManager.isEnabled() &&
            biometricManager.isReadyToAuthenticate() &&
            biometricManager.storedUserId() > 0
        if (hasBio) {
            biometricBtn.visibility = View.VISIBLE
            biometricBtn.text = "Use fingerprint or screen lock"
        } else {
            biometricBtn.visibility = View.GONE
        }
        // "or" divider only when both Sign In and the biometric button are shown.
        findViewById<View>(R.id.orDivider).visibility =
            if (hasPasswordUsers && hasBio) View.VISIBLE else View.GONE
    }

    /** Variant that re-queries DB before showing — used by the "Create new account" path. */
    private fun showSignInPanelFresh() {
        lifecycleScope.launch {
            val state = withContext(Dispatchers.IO) {
                val all = userRepo.getAllUsers()
                Pair(all.any { it.authMode == SessionManager.AUTH_MODE_PASSWORD }, all.any { it.authMode == SessionManager.AUTH_MODE_BIOMETRIC_ONLY })
            }
            showSignInPanel(state.first, state.second)
        }
    }

    /**
     * Show the signup chooser. If the device's biometric slot is already
     * taken, hide the "Use fingerprint or screen lock" button entirely so the
     * user can't even start a doomed flow — only the password path is allowed.
     */
    private fun showSignupChoosePanel(bioTaken: Boolean) {
        showOnlyPanel(signupChoosePanel)
        findViewById<EditText>(R.id.regNameInput).setText("")
        findViewById<TextView>(R.id.regMessage).visibility = View.GONE

        val useDeviceBtn = findViewById<MaterialButton>(R.id.useDeviceUnlockBtn)
        val setPasswordBtn = findViewById<MaterialButton>(R.id.setPasswordBtn)
        if (bioTaken) {
            useDeviceBtn.visibility = View.GONE
            // Promote the password button to the primary gold style so the
            // user has a clear primary action.
            setPasswordBtn.setBackgroundColor(0)
            setPasswordBtn.text = "Set a password"
        } else {
            useDeviceBtn.visibility = View.VISIBLE
            setPasswordBtn.text = "Set a password instead"
        }
    }

    /**
     * Minimal sign-in for a user we remember from the last session — used on
     * cold launch only. Hides the username field, the divider, and the
     * "create new account" button. Shows just the prompt that's relevant.
     */
    private fun showReturningSignIn(name: String, userId: Int, isBiometricOnly: Boolean) {
        showOnlyPanel(signInPanel)
        returningUserName = name
        findViewById<TextView>(R.id.cardSubText).text = "Welcome back, $name"
        findViewById<EditText>(R.id.passwordInput).setText("")
        findViewById<TextView>(R.id.loginMessage).visibility = View.GONE

        findViewById<View>(R.id.usernameInputLayout).visibility = View.GONE
        findViewById<View>(R.id.signInDivider).visibility = View.GONE
        findViewById<View>(R.id.createNewAccountBtn).visibility = View.GONE
        findViewById<View>(R.id.differentAccountLink).visibility = View.VISIBLE

        val biometricBtn = findViewById<MaterialButton>(R.id.biometricBtn)

        if (isBiometricOnly) {
            // Biometric account: only the prompt, auto-launched once the
            // splash flip-cycle is fully done — otherwise the system prompt
            // would pop over the splash and feel jarring.
            findViewById<View>(R.id.passwordInputLayout).visibility = View.GONE
            findViewById<View>(R.id.loginBtn).visibility = View.GONE
            findViewById<View>(R.id.orDivider).visibility = View.GONE
            biometricBtn.text = "Sign in"
            biometricBtn.visibility = View.VISIBLE
            if (splashDone) {
                biometricBtn.post { if (!biometricInFlight) launchBiometricLogin() }
            } else {
                pendingBiometricAutoLaunch = true
            }
        } else {
            // Password account: password field + sign-in button. If this user
            // also owns the device's biometric slot, show the quick button too.
            findViewById<View>(R.id.passwordInputLayout).visibility = View.VISIBLE
            findViewById<View>(R.id.loginBtn).visibility = View.VISIBLE
            val canBio = biometricManager.isEnabled() &&
                biometricManager.isReadyToAuthenticate() &&
                biometricManager.storedUserId() == userId
            if (canBio) {
                biometricBtn.text = "Use fingerprint or screen lock"
                biometricBtn.visibility = View.VISIBLE
            } else {
                biometricBtn.visibility = View.GONE
            }
            findViewById<View>(R.id.orDivider).visibility =
                if (canBio) View.VISIBLE else View.GONE
        }
    }

    // ── Signup chooser panel ──────────────────────────────────────────────────

    private fun wireSignupChoosePanel() {
        val nameInput      = findViewById<EditText>(R.id.regNameInput)
        val useDeviceBtn   = findViewById<MaterialButton>(R.id.useDeviceUnlockBtn)
        val setPasswordBtn = findViewById<MaterialButton>(R.id.setPasswordBtn)

        useDeviceBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isBlank()) {
                showRegMessage("Please enter a username")
                return@setOnClickListener
            }
            if (!biometricManager.hasDeviceLock()) {
                showRegMessage("Set a screen lock in phone settings first")
                return@setOnClickListener
            }
            // Up-front DB checks so the user doesn't get a permission prompt
            // (and then a failure toast) for an invalid signup.
            lifecycleScope.launch {
                val checks = withContext(Dispatchers.IO) {
                    Pair(userRepo.getUserByName(name), userRepo.getBiometricOnlyUser())
                }
                val nameTaken = checks.first
                val bioOwner = checks.second
                if (nameTaken != null) {
                    showRegMessage("\"$name\" is already taken — pick a different username")
                    return@launch
                }
                if (bioOwner != null) {
                    showRegMessage("\"${bioOwner.name}\" already uses fingerprint/screen-lock sign-in on this device")
                    return@launch
                }
                biometricInFlight = true
                biometricManager.promptForDeviceVerification(
                    activity = this@LoginActivity,
                    title = "Confirm to continue",
                    subtitle = "Use your fingerprint, face, or screen lock",
                    onSuccess = {
                        biometricInFlight = false
                        registerVm.createBiometricUser(name)
                    },
                    onError = { msg ->
                        biometricInFlight = false
                        if (msg.isNotBlank()) showRegMessage(msg)
                    }
                )
            }
        }

        setPasswordBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isBlank()) {
                showRegMessage("Please enter a username")
                return@setOnClickListener
            }
            // Stop the user from picking a password against an unavailable name.
            lifecycleScope.launch {
                val taken = withContext(Dispatchers.IO) { userRepo.getUserByName(name) }
                if (taken != null) {
                    showRegMessage("\"$name\" is already taken — pick a different username")
                    return@launch
                }
                pendingSignupName = name
                findViewById<TextView>(R.id.pwSubText).text = "Choose a password for $name"
                swapPanel(signupChoosePanel, signupPasswordPanel, forward = true)
            }
        }
    }

    // ── Signup password panel ─────────────────────────────────────────────────

    private fun wireSignupPasswordPanel() {
        val newPwInput     = findViewById<EditText>(R.id.newPasswordInput)
        val confirmPwInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val createBtn      = findViewById<MaterialButton>(R.id.createAccountBtn)
        val backBtn        = findViewById<MaterialButton>(R.id.pwBackBtn)

        createBtn.setOnClickListener {
            registerVm.createPasswordUser(
                pendingSignupName,
                newPwInput.text.toString(),
                confirmPwInput.text.toString()
            )
        }
        backBtn.setOnClickListener {
            swapPanel(signupPasswordPanel, signupChoosePanel, forward = false)
            findViewById<TextView>(R.id.pwMessage).visibility = View.GONE
        }
    }

    // ── VM observers ──────────────────────────────────────────────────────────

    private fun observeViewModels() {
        val loginMessage = findViewById<TextView>(R.id.loginMessage)
        val regMessage   = findViewById<TextView>(R.id.regMessage)
        val pwMessage    = findViewById<TextView>(R.id.pwMessage)

        loginVm.loginSuccess.observe(this) { success -> if (success) openHome() }
        loginVm.errorMessage.observe(this) { msg ->
            if (msg != null) {
                loginMessage.text = msg
                loginMessage.setTextColor("#E8A48A".toColorInt())
                loginMessage.visibility = View.VISIBLE
            } else {
                loginMessage.visibility = View.GONE
            }
        }

        registerVm.biometricCredentials.observe(this) { creds ->
            if (creds == null) return@observe
            biometricManager.saveCredentials(creds.userId, creds.email, creds.password)
            registerVm.consumeBiometricCredentials()
        }
        // A brand-new account goes through the first-run onboarding carousel (which
        // then lands on Home). Signing into an existing account skips straight to Home.
        registerVm.registrationSuccess.observe(this) { success -> if (success) openOnboarding() }
        registerVm.errorMessage.observe(this) { msg ->
            if (msg.isNullOrBlank()) {
                regMessage.visibility = View.GONE
                pwMessage.visibility = View.GONE
                return@observe
            }
            val target = if (signupPasswordPanel.visibility == View.VISIBLE) pwMessage else regMessage
            target.text = msg
            target.setTextColor("#E8A48A".toColorInt())
            target.visibility = View.VISIBLE
        }
    }

    // ── Biometric helpers ─────────────────────────────────────────────────────

    private fun launchBiometricLogin() {
        if (!biometricManager.hasStoredCredentials()) {
            showLoginMessage("No fingerprint account on this device")
            return
        }
        biometricInFlight = true
        biometricManager.promptForLogin(
            activity = this,
            onSuccess = { creds ->
                biometricInFlight = false
                loginVm.signInWithBiometricCreds(creds.email, creds.password)
            },
            onError = { msg ->
                biometricInFlight = false
                if (msg.isNotBlank()) showLoginMessage(msg)
            }
        )
    }

    // ── Panel transitions ────────────────────────────────────────────────────

    private fun showOnlyPanel(target: View) {
        signInPanel.visibility         = if (target == signInPanel) View.VISIBLE else View.INVISIBLE
        signupChoosePanel.visibility   = if (target == signupChoosePanel) View.VISIBLE else View.INVISIBLE
        signupPasswordPanel.visibility = if (target == signupPasswordPanel) View.VISIBLE else View.INVISIBLE
        signInPanel.translationX = 0f
        signupChoosePanel.translationX = 0f
        signupPasswordPanel.translationX = 0f
    }

    private fun swapPanel(outgoing: View, incoming: View, forward: Boolean) {
        val w = outgoing.width.toFloat().takeIf { it > 0 } ?: resources.displayMetrics.widthPixels.toFloat()
        val dir = if (forward) 1f else -1f
        incoming.translationX = w * dir
        incoming.visibility = View.VISIBLE
        outgoing.animate()
            .translationX(-w * dir)
            .setDuration(360)
            .setInterpolator(AccelerateInterpolator(1.2f))
            .withEndAction {
                outgoing.visibility = View.INVISIBLE
                outgoing.translationX = 0f
            }
            .start()
        incoming.animate()
            .translationX(0f)
            .setDuration(380)
            .setInterpolator(DecelerateInterpolator(1.3f))
            .start()
    }

    // ── Small UI helpers ─────────────────────────────────────────────────────

    private fun showLoginMessage(msg: String) {
        val v = findViewById<TextView>(R.id.loginMessage)
        v.text = msg
        v.setTextColor("#E8A48A".toColorInt())
        v.visibility = View.VISIBLE
    }

    private fun showRegMessage(msg: String) {
        val v = findViewById<TextView>(R.id.regMessage)
        v.text = msg
        v.setTextColor("#E8A48A".toColorInt())
        v.visibility = View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            signupPasswordPanel.visibility == View.VISIBLE -> {
                swapPanel(signupPasswordPanel, signupChoosePanel, forward = false)
            }
            signupChoosePanel.visibility == View.VISIBLE -> {
                // Only let them back out of the chooser if a sign-in exists to fall back to.
                lifecycleScope.launch {
                    val anyUsers = withContext(Dispatchers.IO) { userRepo.hasAnyUsers() }
                    if (anyUsers) showSignInPanelFresh() else {
                        @Suppress("DEPRECATION") super.onBackPressed()
                    }
                }
            }
            else -> @Suppress("DEPRECATION") super.onBackPressed()
        }
    }

    // ── Entrance animation ───────────────────────────────────────────────────

    private fun runEntranceAnimation() {
        val dp = resources.displayMetrics.density
        val loginCard   = findViewById<MaterialCardView>(R.id.loginCard)
        val logoIcon    = findViewById<View>(R.id.logoIcon)
        val appNameText = findViewById<View>(R.id.appNameText)
        val appTagline  = findViewById<View>(R.id.appTaglineText)
        val orbs = listOf(
            findViewById<View>(R.id.orb1), findViewById<View>(R.id.orb2),
            findViewById<View>(R.id.orb3), findViewById<View>(R.id.orb4),
            findViewById<View>(R.id.orb5), findViewById<View>(R.id.orb6),
            findViewById<View>(R.id.orb7), findViewById<View>(R.id.orb8)
        )

        loginCard.translationY = 280f * dp
        logoIcon.alpha = 0f; appNameText.alpha = 0f; appTagline.alpha = 0f

        data class OrbParams(val amp: Float, val dur: Long, val delay: Long)
        val params = listOf(
            OrbParams(14f, 3400L, 0L), OrbParams(10f, 4800L, 1100L),
            OrbParams(17f, 3700L, 550L), OrbParams(11f, 4200L, 700L),
            OrbParams(8f, 3100L, 1800L), OrbParams(15f, 5200L, 300L),
            OrbParams(12f, 4600L, 900L), OrbParams(9f, 3800L, 1400L)
        )
        orbs.zip(params).forEach { (orb, p) -> floatOrb(orb, p.amp * dp, p.dur, p.delay) }

        val brandEase = DecelerateInterpolator(1.8f)
        logoIcon.animate().alpha(1f).setDuration(550).setInterpolator(brandEase).start()
        appNameText.animate().alpha(1f).setDuration(550).setStartDelay(80).setInterpolator(brandEase).start()
        appTagline.animate().alpha(1f).setDuration(550).setStartDelay(160).setInterpolator(brandEase).start()

        loginCard.animate()
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(60)
            .setInterpolator(PathInterpolator(0f, 0f, 0.2f, 1f))
            .start()
    }

    private fun floatOrb(view: View, amplitude: Float, duration: Long, delay: Long) {
        ObjectAnimator.ofFloat(view, "translationY", 0f, -amplitude).apply {
            this.duration = duration
            startDelay = delay
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    // ── Icon rolling cycle ───────────────────────────────────────────────────

    private fun startIconCycle() {
        val logoIcon = findViewById<ImageView>(R.id.logoIcon)
        val icons = AssignmentIcons.options.map { it.drawableResId }
        var index = 0

        logoIcon.cameraDistance = 14000f * resources.displayMetrics.density

        iconCycleRunnable = object : Runnable {
            override fun run() {
                index = (index + 1) % icons.size
                logoIcon.animate()
                    .rotationX(90f)
                    .setDuration(500)
                    .setInterpolator(AccelerateInterpolator(1.5f))
                    .withEndAction {
                        logoIcon.setImageResource(icons[index])
                        logoIcon.rotationX = -90f
                        logoIcon.animate()
                            .rotationX(0f)
                            .setDuration(500)
                            .setInterpolator(DecelerateInterpolator(1.5f))
                            .start()
                    }
                    .start()
                iconCycleHandler.postDelayed(this, 5500)
            }
        }
        iconCycleHandler.postDelayed(iconCycleRunnable!!, 5500)
    }

    override fun onDestroy() {
        super.onDestroy()
        iconCycleRunnable?.let { iconCycleHandler.removeCallbacks(it) }
    }

    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    // First-run only: a freshly created account is shown the onboarding carousel,
    // which itself opens Home when finished/skipped.
    private fun openOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    companion object {
        /** Pass this when re-entering Login (e.g. after sign-out) to skip the 3-second splash. */
        const val EXTRA_SKIP_SPLASH = "extra_skip_splash"
        /** Username carried in from a tapped notification — pre-fills the sign-in field. */
        const val EXTRA_NOTIFICATION_USERNAME = "extra_notification_username"
    }
}
