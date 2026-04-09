package uws.ac.uk.studymate.ui


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
import uws.ac.uk.studymate.ui.viewmodels.UserTestViewModel

class UserTestActivity : AppCompatActivity() {

    private lateinit var registerVm: RegisterViewModel
    private lateinit var testVm: UserTestViewModel

    /**
     This screen is a simple testing page for adding, loading, and deleting users.
     it started as a quick way to check the user table, and later got the add and delete actions together.
     this can stay plain for now because it is mainly for checking things during development.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_test)

        // Set up the ViewModels used by this screen.
        // One handles registration, and the other handles user list actions.
        registerVm = ViewModelProvider(this)[RegisterViewModel::class.java]
        testVm = ViewModelProvider(this)[UserTestViewModel::class.java]

        // Get the views used on this screen.
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val addBtn = findViewById<Button>(R.id.addUserBtn)
        val registerMessage = findViewById<TextView>(R.id.registerMessage)
        val loadBtn = findViewById<Button>(R.id.loadUsersBtn)
        val usersOutput = findViewById<TextView>(R.id.usersOutput)
        val deleteIdInput = findViewById<EditText>(R.id.deleteIdInput)
        val deleteBtn = findViewById<Button>(R.id.deleteUserBtn)

        // Send the entered details to the registration ViewModel.
        addBtn.setOnClickListener {
            registerVm.register(
                nameInput.text.toString(),
                emailInput.text.toString(),
                passwordInput.text.toString()
            )
        }

        // Show a success message and reload the list when registration works.
        registerVm.registrationSuccess.observe(this) { success ->
            if (success) {
                registerMessage.text = "User added successfully"
                registerMessage.setTextColor("#2E7D32".toColorInt()) // dark green
                registerMessage.visibility = View.VISIBLE
                testVm.loadUsers()
            }
        }

        // Show the latest error message when registration fails.
        registerVm.errorMessage.observe(this) { message ->
            if (message != null) {
                registerMessage.text = message
                registerMessage.setTextColor("#C62828".toColorInt()) // dark red
                registerMessage.visibility = View.VISIBLE
            } else {
                registerMessage.visibility = View.GONE
            }
        }

        // Load the current user list when the button is pressed.
        loadBtn.setOnClickListener {
            testVm.loadUsers()
        }

        // Display the saved users, or show a message if there are none.
        testVm.users.observe(this) { users ->
            if (users.isEmpty()) {
                usersOutput.text = "No users found"
            } else {
                usersOutput.text = users.joinToString("\n\n") {
                    "ID: ${it.id}\n" +
                            "Name: ${it.name}\n" +
                            "Email: ${it.email}\n" +
                            "Hash: ${it.passwordHash}\n" +
                            "Salt: ${it.passwordSalt}"
                }
            }
        }

        // Delete the selected user when an ID has been entered.
        deleteBtn.setOnClickListener {
            val idStr = deleteIdInput.text.toString()
            if (idStr.isNotEmpty()) {
                testVm.deleteUser(idStr.toInt())
                deleteIdInput.text.clear()
                testVm.loadUsers() // Refresh the list after deleting.
            }
        }
    }
}