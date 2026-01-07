package mathquiz.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * SM-2 based spaced repetition algorithm.
 * Stateless - all methods are pure functions.
 */
public final class SpacedRepetition {

    /** Minimum ease factor (prevents it from going too low) */
    public static final double MIN_EASE = 1.3;

    private SpacedRepetition() {}

    /**
     * Update problem stats after an answer.
     * Returns a new ProblemStats instance (immutable pattern).
     *
     * @param stats   Current stats for the problem
     * @param correct Whether the answer was correct
     * @param clock   Clock for determining next review time
     * @return New stats reflecting the answer
     */
    public static ProblemStats updateStats(ProblemStats stats, boolean correct, Clock clock) {
        Instant now = clock.instant();
        int newTotalAttempts = stats.totalAttempts() + 1;
        int newTotalCorrect = stats.totalCorrect() + (correct ? 1 : 0);

        if (correct) {
            return handleCorrect(stats, now, newTotalAttempts, newTotalCorrect);
        } else {
            return handleIncorrect(stats, now, newTotalAttempts, newTotalCorrect);
        }
    }

    private static ProblemStats handleCorrect(
            ProblemStats stats, Instant now,
            int newTotalAttempts, int newTotalCorrect) {

        int newReps = stats.repetitions() + 1;
        double newInterval;

        if (newReps == 1) {
            newInterval = 1.0 / 24; // 1 hour in days
        } else if (newReps == 2) {
            newInterval = 1.0 / 6;  // 4 hours in days
        } else if (newReps == 3) {
            newInterval = 1.0;      // 1 day
        } else {
            newInterval = stats.intervalDays() * stats.easeFactor();
        }

        // SM-2 ease factor update for quality=4 (correct with hesitation)
        // EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
        // For q=4: EF' = EF + (0.1 - 1 * (0.08 + 1 * 0.02)) = EF + 0
        int quality = 4;
        double easeAdjustment = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02);
        double newEase = Math.max(MIN_EASE, stats.easeFactor() + easeAdjustment);

        Instant newNextReview = now.plus(daysToMillis(newInterval));

        return new ProblemStats(
            stats.operation(), stats.operand1(), stats.operand2(),
            newEase, newInterval, newNextReview, newReps,
            newTotalAttempts, newTotalCorrect
        );
    }

    private static ProblemStats handleIncorrect(
            ProblemStats stats, Instant now,
            int newTotalAttempts, int newTotalCorrect) {

        // Reset repetitions, reduce ease factor
        double newEase = Math.max(MIN_EASE, stats.easeFactor() - 0.2);

        // Due again in 1 minute (for immediate retry)
        Instant newNextReview = now.plus(Duration.ofMinutes(1));

        return new ProblemStats(
            stats.operation(), stats.operand1(), stats.operand2(),
            newEase, 0.0, newNextReview, 0,
            newTotalAttempts, newTotalCorrect
        );
    }

    private static Duration daysToMillis(double days) {
        return Duration.ofMillis((long) (days * 24 * 60 * 60 * 1000));
    }
}
