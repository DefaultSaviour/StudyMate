package uws.ac.uk.studymate.util

import android.graphics.Color
import androidx.annotation.ColorInt

// Shared colour parsing for subject/deck/assignment swatches. Every list adapter
// and screen that tints a row from a stored hex string used to carry its own
// try/catch with the same gold fallback — this is that single helper.
object ColorUtils {

    // The brand gold (@color/gold) parsed once, used as the fallback when a
    // subject has no colour or an unparseable one.
    @ColorInt
    val DEFAULT_GOLD: Int = Color.parseColor("#C4A24A")

    // Parse a stored hex colour, falling back to brand gold for null/blank/invalid.
    @ColorInt
    fun parseOrDefault(hex: String?): Int {
        if (hex.isNullOrBlank()) return DEFAULT_GOLD
        return try {
            Color.parseColor(hex)
        } catch (_: IllegalArgumentException) {
            DEFAULT_GOLD
        }
    }
}
