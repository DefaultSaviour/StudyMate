package uws.ac.uk.studymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uws.ac.uk.studymate.data.entities.FlashCard
import kotlin.random.Random

/*//////////////////////
Unit tests for the Mock Exam question generator (1.2). Pure Kotlin, no Room/Android.
 *//////////////////////
class ExamGeneratorTest {

    private fun deckOf(n: Int, uniqueBacks: Boolean = true) = (1..n).map { i ->
        FlashCard(
            id = i,
            userId = 1,
            deckId = 1,
            front = "front$i",
            back = if (uniqueBacks) "back$i" else "sameBack"
        )
    }

    @Test
    fun fewerThanMinCardsProducesNoQuestions() {
        val cards = deckOf(ExamGenerator.MIN_CARDS - 1)
        assertTrue(ExamGenerator.generate(cards).isEmpty())
    }

    @Test
    fun exactlyMinCardsBoundaryWorks() {
        val cards = deckOf(ExamGenerator.MIN_CARDS)
        val questions = ExamGenerator.generate(cards, random = Random(1))
        assertEquals(ExamGenerator.MIN_CARDS, questions.size)
        questions.forEach { q ->
            assertEquals(ExamGenerator.OPTIONS_PER_QUESTION, q.options.size)
            assertTrue(q.correctIndex in q.options.indices)
        }
    }

    @Test
    fun everyQuestionHasExactlyOneCorrectOptionMatchingThePrompt() {
        val cards = deckOf(20)
        val cardsById = cards.associateBy { it.id }
        val questions = ExamGenerator.generate(cards, random = Random(42))
        questions.forEach { q ->
            val correctCard = cardsById.getValue(q.cardId)
            assertEquals(correctCard.front, q.prompt)
            assertEquals(correctCard.back, q.options[q.correctIndex])
        }
    }

    @Test
    fun questionCountRespectsTheCap() {
        val cards = deckOf(50)
        val questions = ExamGenerator.generate(cards, maxQuestions = 10, random = Random(7))
        assertEquals(10, questions.size)
    }

    @Test
    fun fullDeckUnderCapUsesEveryCard() {
        val cards = deckOf(12)
        val questions = ExamGenerator.generate(cards, maxQuestions = ExamGenerator.DEFAULT_MAX_QUESTIONS, random = Random(3))
        assertEquals(12, questions.size)
    }

    @Test
    fun degenerateDeckWithDuplicateBacksStillProducesThreeOptions() {
        // Every card shares the same back text — the "distinct" distractor filter
        // can't find 2 non-matching distractors, so it must fall back to any other card.
        val cards = deckOf(ExamGenerator.MIN_CARDS, uniqueBacks = false)
        val questions = ExamGenerator.generate(cards, random = Random(5))
        assertEquals(ExamGenerator.MIN_CARDS, questions.size)
        questions.forEach { q ->
            assertEquals(ExamGenerator.OPTIONS_PER_QUESTION, q.options.size)
            assertTrue(q.correctIndex in q.options.indices)
        }
    }

    @Test
    fun distractorsAvoidDuplicatingTheCorrectTextWhenPossible() {
        val cards = deckOf(20) // unique backs, plenty of distinct distractors available
        val questions = ExamGenerator.generate(cards, random = Random(9))
        questions.forEach { q ->
            val correctText = q.options[q.correctIndex]
            val occurrences = q.options.count { it == correctText }
            assertEquals(1, occurrences)
        }
    }
}
