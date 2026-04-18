package uws.ac.uk.studymate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.loginBtn)
        val createAccountButton = findViewById<Button>(R.id.createAccountBtn)

        loginButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        createAccountButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
}