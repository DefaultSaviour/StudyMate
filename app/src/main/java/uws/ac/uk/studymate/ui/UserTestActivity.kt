package uws.ac.uk.studymate.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.UserTestViewModel

class UserTestActivity : AppCompatActivity() {

    private lateinit var vm: UserTestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_test)

        vm = ViewModelProvider(this)[UserTestViewModel::class.java]

        val name = findViewById<EditText>(R.id.nameInput)
        val email = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)
        val addBtn = findViewById<Button>(R.id.addUserBtn)
        val loadBtn = findViewById<Button>(R.id.loadUsersBtn)
        val deleteBtn = findViewById<Button>(R.id.deleteUserBtn)
        val deleteIdInput = findViewById<EditText>(R.id.deleteIdInput)
        val usersOutput = findViewById<TextView>(R.id.usersOutput)

        addBtn.setOnClickListener {
            vm.addUser(
                name.text.toString(),
                email.text.toString(),
                password.text.toString()
            )
        }

        loadBtn.setOnClickListener {
            vm.loadUsers()
        }

        deleteBtn.setOnClickListener {
            val idStr = deleteIdInput.text.toString()
            if (idStr.isNotEmpty()) {
                vm.deleteUser(idStr.toInt())
            }
        }

        vm.users.observe(this) { users ->
            val text = users.joinToString("\n") { "ID: ${it.id} | ${it.name} | ${it.email}" }
            usersOutput.text = if (text.isEmpty()) "No users found" else text
        }
    }
}
