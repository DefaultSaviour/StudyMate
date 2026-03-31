package uws.ac.uk.studymate.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.RegisterViewModel
import uws.ac.uk.studymate.ui.viewmodels.UserTestViewModel

class UserTestActivity : AppCompatActivity() {

    private lateinit var registerVm: RegisterViewModel
    private lateinit var testVm: UserTestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_test)

        // Two ViewModels - RegisterViewModel handles adding users (with validation),
        // UserTestViewModel handles loading and deleting users
        registerVm = ViewModelProvider(this)[RegisterViewModel::class.java]
        testVm = ViewModelProvider(this)[UserTestViewModel::class.java]

        // Get references to all the UI elements
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val addBtn = findViewById<Button>(R.id.addUserBtn)
        val registerMessage = findViewById<TextView>(R.id.registerMessage)
        val loadBtn = findViewById<Button>(R.id.loadUsersBtn)
        val usersOutput = findViewById<TextView>(R.id.usersOutput)
        val deleteIdInput = findViewById<EditText>(R.id.deleteIdInput)
        val deleteBtn = findViewById<Button>(R.id.deleteUserBtn)

        // Add user button - passes inputs to RegisterViewModel which validates first
        addBtn.setOnClickListener {
            registerVm.register(
                nameInput.text.toString(),
                emailInput.text.toString(),
                passwordInput.text.toString()
            )
        }

        // If registration succeeded - show green message and reload the user list
        registerVm.registrationSuccess.observe(this) { success ->
            if (success) {
                registerMessage.text = "User added successfully"
                registerMessage.setTextColor(Color.parseColor("#2E7D32")) // dark green
                registerMessage.visibility = View.VISIBLE
                testVm.loadUsers()
            }
        }

        // If there was an error - show it in red under the button
        registerVm.errorMessage.observe(this) { message ->
            if (message != null) {
                registerMessage.text = message
                registerMessage.setTextColor(Color.parseColor("#C62828")) // dark red
                registerMessage.visibility = View.VISIBLE
            } else {
                registerMessage.visibility = View.GONE
            }
        }

        // Load button - fetches all users and displays them
        loadBtn.setOnClickListener {
            testVm.loadUsers()
        }

        // Display each user with their ID, name, email, hash and salt
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

        // Delete button - only runs if the ID field is not empty
        deleteBtn.setOnClickListener {
            val idStr = deleteIdInput.text.toString()
            if (idStr.isNotEmpty()) {
                testVm.deleteUser(idStr.toInt())
                deleteIdInput.text.clear()
                testVm.loadUsers() // refresh the list after deleting
            }
        }
    }
}