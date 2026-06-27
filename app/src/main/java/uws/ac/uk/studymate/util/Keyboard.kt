package uws.ac.uk.studymate.util

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager

/*//////////////////////
Small helper for dismissing the soft keyboard.

Under API 35 edge-to-edge the IME doesn't always close on its own when focus
leaves a field or when the activity navigates away — it can linger over the next
screen. Call hide() whenever we leave a text-input context (panel swap, before
launching another activity) and from a global onActivityPaused hook so the
keyboard never bleeds onto the following page.
 *//////////////////////
object Keyboard {

    fun hide(activity: Activity) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
        // Prefer the currently focused view's window token; fall back to the decor view.
        val focus = activity.currentFocus ?: activity.window.decorView
        imm.hideSoftInputFromWindow(focus.windowToken, 0)
        activity.currentFocus?.clearFocus()
    }

    fun show(view: android.view.View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}
