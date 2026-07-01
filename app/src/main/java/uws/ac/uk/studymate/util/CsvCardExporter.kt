package uws.ac.uk.studymate.util

import uws.ac.uk.studymate.data.entities.FlashCard

/*//////////////////////
Flashcard CSV export (1.2) — the inverse of CsvCardParser, for peer deck sharing.
Produces the same front/back CSV shape CsvCardParser already reads (header row +
one front,back pair per line), so a recipient's "Paste cards" button can consume it
straight back — no file, no server, symmetric with the 0.9F import path.

RFC-4180-ish: a field is quoted only if it contains the delimiter, a quote, or a
newline; internal quotes are escaped by doubling ("").
 *//////////////////////
object CsvCardExporter {

    private const val DELIMITER = ','

    fun toCsv(cards: List<FlashCard>): String {
        val sb = StringBuilder()
        sb.append("front,back\n")
        for (card in cards) {
            sb.append(quoteIfNeeded(card.front))
            sb.append(DELIMITER)
            sb.append(quoteIfNeeded(card.back))
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun quoteIfNeeded(field: String): String {
        val needsQuoting = field.contains(DELIMITER) || field.contains('"') ||
            field.contains('\n') || field.contains('\r')
        if (!needsQuoting) return field
        return "\"" + field.replace("\"", "\"\"") + "\""
    }
}
