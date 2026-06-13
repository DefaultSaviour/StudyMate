package uws.ac.uk.studymate.util

// Shared input sanitisation for user-entered text. Used by every ViewModel that
// validates a text field before saving, so the "collapse pasted whitespace"
// rule lives in one place (see CLAUDE.md "Input sanitisation").
object TextSanitizer {

    // Single-line fields (names, titles): strip newlines/tabs, collapse runs of
    // whitespace to one space, and trim. Use for any field that must stay on one line.
    fun singleLine(raw: String): String {
        return raw.replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    // Multi-line fields (flashcard front/back): keep newlines, but strip tabs and
    // collapse runs of spaces so pasted content stays tidy.
    fun multiLine(raw: String): String {
        return raw.replace(Regex("[\\t]+"), " ")
            .replace(Regex(" {2,}"), " ")
            .trim()
    }
}
