package uws.ac.uk.studymate.util

import android.app.Activity
import android.content.pm.ActivityInfo
import uws.ac.uk.studymate.R

/**
 * Applies the per-form-factor orientation policy.
 *
 * The whole design is portrait-first, so phones stay locked to portrait. Large
 * screens (sw600dp — tablets and unfolded foldables) override `lock_portrait` to
 * false so they can rotate freely; the glass card caps + centres in either
 * orientation (see `@dimen/card_max_width` and CLAUDE.md "Large-screen support").
 *
 * Called from every Activity's onCreate instead of a static
 * `android:screenOrientation` in the manifest, because that attribute can't be
 * qualified by a resource bucket.
 */
object OrientationLock {
    fun apply(activity: Activity) {
        activity.requestedOrientation =
            if (activity.resources.getBoolean(R.bool.lock_portrait)) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            }
    }
}
