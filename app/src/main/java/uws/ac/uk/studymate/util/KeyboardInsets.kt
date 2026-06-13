package uws.ac.uk.studymate.util

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/*//////////////////////
Adds IME-aware bottom padding to an activity's content view so input fields
don't get hidden by the soft keyboard.

API 35 puts every activity into edge-to-edge by default, which means the
window draws *behind* the IME and android:windowSoftInputMode="adjustResize"
no longer shrinks the layout for you. To get the old behaviour back we listen
for IME insets and pad the bottom of the activity's content frame so the
underlying ConstraintLayout / ScrollView shrinks and auto-scrolls the focused
EditText into view.

Call once from onCreate of any activity with text input.
 *//////////////////////
object KeyboardInsets {

    fun apply(activity: Activity) {
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                imeBottom
            )
            insets
        }
    }
}
