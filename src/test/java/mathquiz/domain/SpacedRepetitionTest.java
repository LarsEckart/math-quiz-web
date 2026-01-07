package mathquiz.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SpacedRepetitionTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-01-07T12:00:00Z"),
        ZoneOffset.UTC
    );

    @Test
    void correctAnswerIncreasesInterval() {
        var stats = ProblemStats.newStats(Operation.ADDITION, 1, 1);

        var newStats = SpacedRepetition.updateStats(stats, true, FIXED_CLOCK);

        assertThat(newStats.repetitions()).isEqualTo(1);
        assertThat(newStats.intervalDays()).isGreaterThan(0);
        assertThat(newStats.nextReview()).isNotNull();
        assertThat(newStats.totalAttempts()).isEqualTo(1);
        assertThat(newStats.totalCorrect()).isEqualTo(1);
    }

    @Test
    void incorrectAnswerResetsRepetitions() {
        var stats = new ProblemStats(
            Operation.ADDITION, 1, 1,
            2.5, 1.0, Instant.now(), 3,
            5, 4
        );

        var newStats = SpacedRepetition.updateStats(stats, false, FIXED_CLOCK);

        assertThat(newStats.repetitions()).isEqualTo(0);
        assertThat(newStats.intervalDays()).isEqualTo(0.0);
        assertThat(newStats.totalAttempts()).isEqualTo(6);
        assertThat(newStats.totalCorrect()).isEqualTo(4);
    }

    @Test
    void incorrectAnswerReducesEase() {
        var stats = new ProblemStats(
            Operation.ADDITION, 1, 1,
            2.5, 0.0, null, 0,
            0, 0
        );

        var newStats = SpacedRepetition.updateStats(stats, false, FIXED_CLOCK);

        assertThat(newStats.easeFactor()).isLessThan(2.5);
    }

    @Test
    void easeFactorHasMinimum() {
        var stats = new ProblemStats(
            Operation.ADDITION, 1, 1,
            SpacedRepetition.MIN_EASE, 0.0, null, 0,
            0, 0
        );

        var newStats = SpacedRepetition.updateStats(stats, false, FIXED_CLOCK);

        assertThat(newStats.easeFactor()).isGreaterThanOrEqualTo(SpacedRepetition.MIN_EASE);
    }

    @Test
    void intervalGrowsWithRepetitions() {
        var stats = ProblemStats.newStats(Operation.ADDITION, 1, 1);

        double[] intervals = new double[5];
        for (int i = 0; i < 5; i++) {
            stats = SpacedRepetition.updateStats(stats, true, FIXED_CLOCK);
            intervals[i] = stats.intervalDays();
        }

        // Each interval should be >= previous
        for (int i = 1; i < intervals.length; i++) {
            assertThat(intervals[i])
                .as("interval[%d] should be >= interval[%d]", i, i - 1)
                .isGreaterThanOrEqualTo(intervals[i - 1]);
        }
    }

    @Test
    void statsAreImmutable() {
        var original = ProblemStats.newStats(Operation.ADDITION, 1, 1);
        var newStats = SpacedRepetition.updateStats(original, true, FIXED_CLOCK);

        assertThat(original.repetitions()).isEqualTo(0);
        assertThat(newStats.repetitions()).isEqualTo(1);
        assertThat(original).isNotSameAs(newStats);
    }

    @Test
    void firstThreeIntervalsAreFixed() {
        var stats = ProblemStats.newStats(Operation.ADDITION, 1, 1);

        // First correct: 1 hour
        stats = SpacedRepetition.updateStats(stats, true, FIXED_CLOCK);
        assertThat(stats.intervalDays()).isCloseTo(1.0 / 24, withinPercentage(1));

        // Second correct: 4 hours
        stats = SpacedRepetition.updateStats(stats, true, FIXED_CLOCK);
        assertThat(stats.intervalDays()).isCloseTo(1.0 / 6, withinPercentage(1));

        // Third correct: 1 day
        stats = SpacedRepetition.updateStats(stats, true, FIXED_CLOCK);
        assertThat(stats.intervalDays()).isCloseTo(1.0, withinPercentage(1));
    }

    private static org.assertj.core.data.Percentage withinPercentage(double percentage) {
        return org.assertj.core.data.Percentage.withPercentage(percentage);
    }
}
