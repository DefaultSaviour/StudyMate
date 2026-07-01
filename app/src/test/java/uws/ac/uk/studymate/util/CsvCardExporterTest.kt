package uws.ac.uk.studymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uws.ac.uk.studymate.data.entities.FlashCard

/*//////////////////////
Unit tests for the flashcard CSV export (1.2, peer deck sharing) — mainly that it
round-trips through CsvCardParser, since that's the whole point: a shared deck's
CSV must be consumable by the recipient's own "Paste cards" button.
 *//////////////////////
class CsvCardExporterTest {

    private fun card(front: String, back: String) =
        FlashCard(userId = 1, deckId = 1, front = front, back = back)

    @Test
    fun roundTripsSimpleCards() {
        val cards = listOf(card("cat", "gato"), card("dog", "perro"))
        val csv = CsvCardExporter.toCsv(cards)
        val parsed = CsvCardParser.parse(csv)

        assertEquals(2, parsed.cards.size)
        assertEquals(CsvCardParser.ParsedCard("cat", "gato"), parsed.cards[0])
        assertEquals(CsvCardParser.ParsedCard("dog", "perro"), parsed.cards[1])
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun roundTripsFieldsWithCommasQuotesAndNewlines() {
        val cards = listOf(
            card("apple, the fruit", "a fruit, red"),
            card("she said \"hi\"", "greeting"),
            card("line one\nline two", "multi-line back")
        )
        val csv = CsvCardExporter.toCsv(cards)
        val parsed = CsvCardParser.parse(csv)

        assertEquals(3, parsed.cards.size)
        assertEquals("apple, the fruit", parsed.cards[0].front)
        assertEquals("a fruit, red", parsed.cards[0].back)
        assertEquals("she said \"hi\"", parsed.cards[1].front)
        assertEquals("line one\nline two", parsed.cards[2].front)
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun emptyListProducesJustTheHeader() {
        val csv = CsvCardExporter.toCsv(emptyList())
        assertEquals("front,back\n", csv)
        assertEquals(0, CsvCardParser.parse(csv).cards.size)
    }

    @Test
    fun quotesOnlyWhenNeeded() {
        val csv = CsvCardExporter.toCsv(listOf(card("plain", "text")))
        assertTrue(csv.contains("plain,text"))
        assertTrue(!csv.contains("\"plain\""))
    }
}
