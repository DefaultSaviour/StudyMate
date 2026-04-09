package uws.ac.uk.studymate.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.LoginViewModel
import uws.ac.uk.studymate.util.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var loginVm: LoginViewModel
    private lateinit var sessionManager: SessionManager

    /**
     This screen lets the user log in and skip ahead if they already signed in.
     it started as a simple email and password screen, and later got the session check at the top.
     it now also shows clear success and error messages.
     the final UI can clean this up later, but the flow is doing the right job for now.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Create the session manager first so this screen can skip login when needed.
        sessionManager = SessionManager(this)

        // Send the user straight to home when a valid session already exists.
        if (sessionManager.isLoggedIn()) {
            openHome()
            return
        }

        // Set up the ViewModel used by this screen.
        loginVm = ViewModelProvider(this)[LoginViewModel::class.java]

        // Get the views used on this screen.
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val createAccountBtn = findViewById<Button>(R.id.createAccountBtn)
        // Disabled for now: this testing-only ClearAllData button used to wipe every table.
        // It is commented out so it can be re-enabled later.
//        val clearDataBtn = findViewById<Button>(R.id.clearDataBtn)
        val loginMessage = findViewById<TextView>(R.id.loginMessage)

        // Pre-fill the email when the user has just finished registering.
        emailInput.setText(intent.getStringExtra(EXTRA_EMAIL).orEmpty())

        // Send the entered login details to the ViewModel.
        loginBtn.setOnClickListener {
            loginVm.login(
                emailInput.text.toString(),
                passwordInput.text.toString()
            )
        }

        // Open the register screen when the user wants to create a new account.
        createAccountBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Disabled for now: this testing-only click handler used to wipe every table.
        // It is commented out so it can be re-enabled later.
//        clearDataBtn.setOnClickListener {
//            loginVm.clearAllData()
//        }

        // Open the home screen when login works.
        loginVm.loginSuccess.observe(this) { success ->
            if (success) {
                loginMessage.text = "Login successful"
                loginMessage.setTextColor("#2E7D32".toColorInt())
                loginMessage.visibility = View.VISIBLE
                openHome()
            }
        }

        // Show the latest error message when login fails.
        loginVm.errorMessage.observe(this) { message ->
            if (message != null) {
                loginMessage.text = message
                loginMessage.setTextColor("#C62828".toColorInt())
                loginMessage.visibility = View.VISIBLE
            } else {
                loginMessage.visibility = View.GONE
            }
        }

        // Disabled for now: this testing-only observer used to show the result of wiping every table.
        // It is commented out so it can be re-enabled later.
//        loginVm.dataCleared.observe(this) { cleared ->
//            if (cleared) {
//                emailInput.text.clear()
//                passwordInput.text.clear()
//                loginMessage.text = "All saved data deleted"
//                loginMessage.setTextColor("#2E7D32".toColorInt())
//                loginMessage.visibility = View.VISIBLE
//            }
//        }
    }

    // Replace the login screen with the home screen after a successful login.
    private fun openHome() {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(homeIntent)
    }

    companion object {
        // Use one extra key when registration sends the user back to login.
        const val EXTRA_EMAIL = "extra_email"
    }
}

