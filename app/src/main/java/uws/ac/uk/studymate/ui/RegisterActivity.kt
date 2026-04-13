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
import uws.ac.uk.studymate.ui.viewmodels.RegisterViewModel
/*//////////////////////
Coded by Jamie Coleman
15/03/26
fixed 06/04/24
 *//////////////////////
class RegisterActivity : AppCompatActivity() {

    private lateinit var registerVm: RegisterViewModel

    /**
     This screen lets the user make an account and go straight into the app.
     it started as a basic form, and later got the success message and direct move to home.
     it now keeps the register flow clear without adding too much extra stuff
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Set up the ViewModel used by this screen.
        registerVm = ViewModelProvider(this)[RegisterViewModel::class.java]

        // Get the views used on this screen.
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val backToLoginBtn = findViewById<Button>(R.id.backToLoginBtn)
        val registerMessage = findViewById<TextView>(R.id.registerMessage)

        // Send the entered details to the registration ViewModel.
        registerBtn.setOnClickListener {
            registerVm.register(
                nameInput.text.toString(),
                emailInput.text.toString(),
                passwordInput.text.toString()
            )
        }

        // Go back to the login screen without creating an account.
        backToLoginBtn.setOnClickListener {
            finish()
        }

        // Show a success message and open the home screen when registration works.
        registerVm.registrationSuccess.observe(this) { success ->
            if (success) {
                registerMessage.text = "Account created successfully"
                registerMessage.setTextColor("#2E7D32".toColorInt())
                registerMessage.visibility = View.VISIBLE

                val homeIntent = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(homeIntent)
            }
        }

        // Show the latest error message when registration fails.
        registerVm.errorMessage.observe(this) { message ->
            if (message != null) {
                registerMessage.text = message
                registerMessage.setTextColor("#C62828".toColorInt())
                registerMessage.visibility = View.VISIBLE
            } else {
                registerMessage.visibility = View.GONE
            }
        }
    }
}

