package mathquiz.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemStatsTest {

    @Test
    void newStatsHasDefaults() {
        var stats = ProblemStats.newStats(Operation.ADDITION, 3, 5);
        
        assertThat(stats.operation()).isEqualTo(Operation.ADDITION);
        assertThat(stats.operand1()).isEqualTo(3);
        assertThat(stats.operand2()).isEqualTo(5);
        assertThat(stats.easeFactor()).isEqualTo(ProblemStats.DEFAULT_EASE);
        assertThat(stats.intervalDays()).isEqualTo(0.0);
        assertThat(stats.nextReview()).isNull();
        assertThat(stats.repetitions()).isEqualTo(0);
        assertThat(stats.totalAttempts()).isEqualTo(0);
    }

    @Test
    void accuracyCalculation() {
        var stats = new ProblemStats(
            Operation.ADDITION, 1, 1,
            2.5, 0.0, null, 0,
            10, 8
        );
        assertThat(stats.accuracy()).isEqualTo(80.0);
    }

    @Test
    void accuracyZeroAttempts() {
        var stats = ProblemStats.newStats(Operation.ADDITION, 1, 1);
        assertThat(stats.accuracy()).isEqualTo(0.0);
    }

    @Test
    void isDueWhenNeverReviewed() {
        var stats = ProblemStats.newStats(Operation.ADDITION, 1, 1);
        assertThat(stats.isDue(Instant.now())).isTrue();
    }

    @Test
    void isDueWhenPastReviewDate() {
        var now = Instant.now();
        var stats = new ProblemStats(
            Operation.ADDITION, 1, 1,
            2.5, 1.0, now.minus(1, ChronoUnit.HOURS), 1,
            1, 1
        );
        assertThat(stats.isDue(now)).isTrue();
    }

    @Test
    void isNotDueWhenFutureReview() {
        var now = Instant.now();
        var stats = new ProblemStats(
            Operation.ADDITION, 1, 1,
            2.5, 1.0, now.plus(1, ChronoUnit.HOURS), 1,
            1, 1
        );
        assertThat(stats.isDue(now)).isFalse();
    }

    @Test
    void priorityNeverReviewedIsPositive() {
        var stats = ProblemStats.newStats(Operation.ADDITION, 1, 1);
        assertThat(stats.priority(Instant.now())).isEqualTo(1.0);
    }

    @Test
    void priorityOverdueIsNegative() {
        var now = Instant.now();
        var stats = new ProblemStats(
            Operation.ADDITION, 1, 1,
            2.5, 1.0, now.minus(2, ChronoUnit.HOURS), 1,
            1, 1
        );
        // Overdue by 2 hours → priority ~ -2.0
        assertThat(stats.priority(now)).isLessThan(0);
    }
}
