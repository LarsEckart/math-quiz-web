package mathquiz.domain;

import java.time.Instant;

/**
 * Tracks spaced repetition stats for a specific problem.
 * Immutable - updates return new instances.
 */
public record ProblemStats(
    Operation operation,
    int operand1,
    int operand2,
    double easeFactor,
    double intervalDays,
    Instant nextReview,
    int repetitions,
    int totalAttempts,
    int totalCorrect
) {
    /** SM-2 default ease factor */
    public static final double DEFAULT_EASE = 2.5;

    /**
     * Create new stats for a problem that hasn't been seen yet.
     */
    public static ProblemStats newStats(Operation operation, int operand1, int operand2) {
        return new ProblemStats(
            operation, operand1, operand2,
            DEFAULT_EASE, 0.0, null, 0, 0, 0
        );
    }

    /**
     * Return accuracy as percentage (0-100).
     */
    public double accuracy() {
        if (totalAttempts == 0) {
            return 0.0;
        }
        return (totalCorrect * 100.0) / totalAttempts;
    }

    /**
     * Check if this problem is due for review.
     */
    public boolean isDue(Instant now) {
        if (nextReview == null) {
            return true;
        }
        return !now.isBefore(nextReview);
    }

    /**
     * Calculate priority for problem selection.
     * Lower values = higher priority.
     * - Due/overdue problems get priority based on how overdue
     * - Never-seen problems get high priority
     */
    public double priority(Instant now) {
        if (nextReview == null) {
            // Never reviewed - high priority (but not highest)
            return 1.0;
        }

        long secondsDiff = now.getEpochSecond() - nextReview.getEpochSecond();
        double hoursDiff = secondsDiff / 3600.0;

        if (secondsDiff >= 0) {
            // Overdue - the more overdue, the higher priority (more negative)
            return -hoursDiff;
        }

        // Not due yet - lower priority
        return -hoursDiff; // positive hours until due
    }
}
