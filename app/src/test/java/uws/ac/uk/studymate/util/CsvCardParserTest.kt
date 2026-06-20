package uws.ac.uk.studymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*//////////////////////
Unit tests for the flashcard CSV/TSV import parser (0.9F).
 *//////////////////////
class CsvCardParserTest {

    @Test
    fun basicComma() {
        val r = CsvCardParser.parse("cat,gato\ndog,perro")
        assertEquals(2, r.cards.size)
        assertEquals(CsvCardParser.ParsedCard("cat", "gato"), r.cards[0])
        assertEquals(CsvCardParser.ParsedCard("dog", "perro"), r.cards[1])
        assertEquals(0, r.skipped)
    }

    @Test
    fun tabDelimitedAutoDetected() {
        val r = CsvCardParser.parse("cat\tgato\ndog\tperro")
        assertEquals(2, r.cards.size)
        assertEquals("gato", r.cards[0].back)
    }

    @Test
    fun quotedFieldContainingDelimiter() {
        val r = CsvCardParser.parse("\"apple, the fruit\",\"a fruit, red\"")
        assertEquals(1, r.cards.size)
        assertEquals("apple, the fruit", r.cards[0].front)
        assertEquals("a fruit, red", r.cards[0].back)
    }

    @Test
    fun quotedFieldWithEmbeddedNewlineIsPreserved() {
        val r = CsvCardParser.parse("\"line one\nline two\",back")
        assertEquals(1, r.cards.size)
        assertEquals("line one\nline two", r.cards[0].front)
    }

    @Test
    fun escapedQuotes() {
        val r = CsvCardParser.parse("\"say \"\"hi\"\"\",greeting")
        assertEquals(1, r.cards.size)
        assertEquals("say \"hi\"", r.cards[0].front)
    }

    @Test
    fun headerRowSkipped() {
        val frontBack = CsvCardParser.parse("front,back\ncat,gato")
        assertEquals(1, frontBack.cards.size)
        assertEquals("cat", frontBack.cards[0].front)

        val termDef = CsvCardParser.parse("Term,Definition\ncat,gato")
        assertEquals(1, termDef.cards.size)
    }

    @Test
    fun nonHeaderFirstRowKept() {
        // "cat,gato" is not a known header pair, so it must NOT be dropped.
        val r = CsvCardParser.parse("cat,gato\ndog,perro")
        assertEquals(2, r.cards.size)
    }

    @Test
    fun blankLinesIgnoredAndBadRowsSkipped() {
        val r = CsvCardParser.parse("cat,gato\n\nonlyfront\ndog,\n  ,  \nbird,pajaro")
        // valid: cat/gato, bird/pajaro. skipped: "onlyfront" (1 col), "dog," (blank back).
        // blank line and the all-whitespace row are ignored silently.
        assertEquals(2, r.cards.size)
        assertEquals(2, r.skipped)
    }

    @Test
    fun extraColumnsIgnored() {
        val r = CsvCardParser.parse("cat,gato,noun,tag1")
        assertEquals(1, r.cards.size)
        assertEquals(CsvCardParser.ParsedCard("cat", "gato"), r.cards[0])
    }

    @Test
    fun crlfLineEndings() {
        val r = CsvCardParser.parse("cat,gato\r\ndog,perro\r\n")
        assertEquals(2, r.cards.size)
        assertEquals("perro", r.cards[1].back)
    }

    @Test
    fun whitespaceTrimmed() {
        val r = CsvCardParser.parse("  cat  ,  gato  ")
        assertEquals(CsvCardParser.ParsedCard("cat", "gato"), r.cards[0])
    }

    @Test
    fun emptyInputGivesEmptyResult() {
        assertEquals(0, CsvCardParser.parse("").cards.size)
        assertEquals(0, CsvCardParser.parse("   \n  \n").cards.size)
    }

    @Test
    fun leadingBomStripped() {
        val r = CsvCardParser.parse("﻿front,back\ncat,gato")
        assertEquals(1, r.cards.size)
        assertEquals("cat", r.cards[0].front)
    }

    @Test
    fun capsAtMaxCards() {
        val sb = StringBuilder()
        for (i in 0 until CsvCardParser.MAX_CARDS + 50) sb.append("f$i,b$i\n")
        val r = CsvCardParser.parse(sb.toString())
        assertTrue(r.cards.size <= CsvCardParser.MAX_CARDS)
        assertEquals(CsvCardParser.MAX_CARDS, r.cards.size)
    }
}
