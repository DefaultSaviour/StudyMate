package uws.ac.uk.studymate.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uws.ac.uk.studymate.R
/*//////////////////////
Coded by Jamie Coleman
10/03/26
- i dont remeber when i removed this
 *//////////////////////


class MainActivity : AppCompatActivity() {

    /**
     This screen is the older app entry page that mostly keeps the basic window setup.
     it started as the default activity from the project template, and it has stayed very small.
     it now mainly keeps the screen edges behaving properly.
     this can be cleaned up later if the app no longer needs it.
     **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}