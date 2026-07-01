package uws.ac.uk.studymate.util

import uws.ac.uk.studymate.data.entities.FlashCard

/*//////////////////////
Pure Kotlin question-builder for the Mock Exam Simulator (1.2). Turns a deck's cards
into multiple-choice questions: the card's front is the prompt, its back is the
correct answer, and 2 distractor answers are pulled from other cards in the same
deck. No SM-2 interference — this is assessment/cramming only, never touches the
spaced-repetition schedule (see CardRepo.reviewCard, which this never calls).

Android-free and unit-tested, mirroring util/SpacedRepetition and util/FocusTimerEngine.
 *//////////////////////
object ExamGenerator {

    const val MIN_CARDS = 8
    const val OPTIONS_PER_QUESTION = 3
    const val DEFAULT_MAX_QUESTIONS = 30

    data class ExamQuestion(
        val cardId: Int,
        val prompt: String,
        val options: List<String>,
        val correctIndex: Int
    )

    // Returns an empty list if there aren't enough cards to build a real multiple-choice
    // question (need the correct card + at least 2 others to draw distractors from).
    fun generate(
        cards: List<FlashCard>,
        maxQuestions: Int = DEFAULT_MAX_QUESTIONS,
        random: kotlin.random.Random = kotlin.random.Random.Default
    ): List<ExamQuestion> {
        if (cards.size < MIN_CARDS) return emptyList()

        val pool = cards.shuffled(random).take(maxQuestions)
        return pool.map { correctCard -> buildQuestion(correctCard, cards, random) }
    }

    private fun buildQuestion(correctCard: FlashCard, allCards: List<FlashCard>, random: kotlin.random.Random): ExamQuestion {
        val others = allCards.filter { it.id != correctCard.id }

        // Prefer distractors whose text differs from the correct answer, so the exam
        // never shows two options that look "correct". Fall back to any other card if
        // a deck is degenerate (lots of duplicate answers) and that pool runs short.
        val distinctPool = others.filter { it.back != correctCard.back }
        val distractorPool = if (distinctPool.size >= OPTIONS_PER_QUESTION - 1) distinctPool else others

        val distractors = distractorPool.shuffled(random).take(OPTIONS_PER_QUESTION - 1).map { it.back }
        val options = (distractors + correctCard.back).shuffled(random)
        val correctIndex = options.indexOf(correctCard.back)

        return ExamQuestion(
            cardId = correctCard.id,
            prompt = correctCard.front,
            options = options,
            correctIndex = correctIndex
        )
    }
}
