package mathquiz.service;

import mathquiz.domain.Operation;

/**
 * Result of answering a problem.
 */
public record AnswerResult(
    boolean correct,
    int correctAnswer,
    int streak,
    int newStars,
    boolean rangeExpanded,
    Operation newOperationUnlocked
) {
    /**
     * Create a result for a correct answer.
     */
    public static AnswerResult correct(int correctAnswer, int streak, int newStars,
                                       boolean rangeExpanded, Operation newOperationUnlocked) {
        return new AnswerResult(true, correctAnswer, streak, newStars, rangeExpanded, newOperationUnlocked);
    }

    /**
     * Create a result for an incorrect answer.
     */
    public static AnswerResult incorrect(int correctAnswer, int newStars,
                                         boolean rangeExpanded, Operation newOperationUnlocked) {
        return new AnswerResult(false, correctAnswer, 0, newStars, rangeExpanded, newOperationUnlocked);
    }
}
