package uws.ac.uk.studymate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        val registerText = findViewById<TextView>(R.id.registerText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        // go to register
        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // go to dashboard
        loginButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

    }
}