package uws.ac.uk.studymate.util

/*//////////////////////
Flashcard CSV/TSV import (0.9F).
Pure-Kotlin (Android-free, unit-tested) parser that turns a CSV/TSV file the user
authored elsewhere (Quizlet / Anki / a spreadsheet) into front/back card pairs.

Deliberately small but tolerant of real-world files:
  - comma OR tab delimited, auto-detected
  - RFC-4180-ish quoting: "quoted fields" may contain the delimiter, newlines, and
    escaped quotes ("")
  - an optional header row (front/back, question/answer, term/definition) is skipped
  - the first two columns are front/back; any extra columns are ignored (Quizlet/Anki
    add tags/extra fields)
  - blank lines are ignored; rows missing a front or a back are skipped (and counted)

Append-only: callers create new cards from the result. No dedupe, no overwrite.
 *//////////////////////
object CsvCardParser {

    data class ParsedCard(val front: String, val back: String)

    // [cards] are the importable pairs; [skipped] is the number of non-blank rows
    // that couldn't be used (too few columns, or a blank front/back).
    data class Result(val cards: List<ParsedCard>, val skipped: Int)

    // Upper bound so a pathological file can't create an unbounded number of cards.
    const val MAX_CARDS = 2000

    // A leading UTF-8 BOM (Excel-exported CSVs commonly start with one) would
    // otherwise corrupt the first field / header detection.
    private const val BOM = '﻿'

    private val HEADER_PAIRS = setOf(
        "front" to "back",
        "question" to "answer",
        "term" to "definition"
    )

    fun parse(text: String): Result {
        val clean = text.removePrefix(BOM.toString())
        if (clean.isBlank()) return Result(emptyList(), 0)

        val delimiter = detectDelimiter(clean)
        val rows = parseRows(clean, delimiter)
        if (rows.isEmpty()) return Result(emptyList(), 0)

        var start = 0
        if (isHeader(rows[0])) start = 1

        val cards = ArrayList<ParsedCard>()
        var skipped = 0
        for (i in start until rows.size) {
            if (cards.size >= MAX_CARDS) break
            val row = rows[i]
            if (row.all { it.isBlank() }) continue          // blank line — ignore silently
            if (row.size < 2) { skipped++; continue }        // only one column — unusable
            val front = TextSanitizer.multiLine(row[0])
            val back = TextSanitizer.multiLine(row[1])
            if (front.isEmpty() || back.isEmpty()) { skipped++; continue }
            cards.add(ParsedCard(front, back))
        }
        return Result(cards, skipped)
    }

    // Pick comma or tab by whichever occurs more on the first non-blank physical line.
    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
        val commas = firstLine.count { it == ',' }
        val tabs = firstLine.count { it == '\t' }
        return if (tabs > commas) '\t' else ','
    }

    private fun isHeader(row: List<String>): Boolean {
        if (row.size < 2) return false
        val pair = row[0].trim().lowercase() to row[1].trim().lowercase()
        return pair in HEADER_PAIRS
    }

    // RFC-4180-ish row/field reader. `\r` is ignored (so \r\n works); `\n` ends a row;
    // a doubled quote inside a quoted field is an escaped quote.
    private fun parseRows(text: String, delimiter: Char): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ }
                    else inQuotes = false
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> { row.add(field.toString()); field.setLength(0) }
                    '\r' -> { /* ignore — \n ends the row */ }
                    '\n' -> { row.add(field.toString()); field.setLength(0); rows.add(row); row = ArrayList() }
                    else -> field.append(c)
                }
            }
            i++
        }
        // Flush the final field/row, unless it's just a trailing newline's empty row.
        row.add(field.toString())
        if (row.size > 1 || row[0].isNotEmpty()) rows.add(row)
        return rows
    }
}
