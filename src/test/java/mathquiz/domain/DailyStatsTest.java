package mathquiz.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static mathquiz.domain.DailyStats.*;
import static org.assertj.core.api.Assertions.assertThat;

class DailyStatsTest {

    @Test
    void initialValues() {
        var stats = new DailyStats(LocalDate.now());
        
        assertThat(stats.problemsSolved()).isEqualTo(0);
        assertThat(stats.problemsCorrect()).isEqualTo(0);
        assertThat(stats.starsEarned()).isEqualTo(0);
        assertThat(stats.bestStreak()).isEqualTo(0);
        assertThat(stats.currentStreak()).isEqualTo(0);
    }

    @Test
    void calculateStars() {
        var stats = new DailyStats(
            LocalDate.now(), 30, 25, 0, 0, 0
        );
        
        int expectedStars = 25 / PROBLEMS_PER_STAR;
        assertThat(stats.calculateStars()).isEqualTo(expectedStars);
    }

    @Test
    void starsCappedAtMax() {
        var stats = new DailyStats(
            LocalDate.now(), 1000, 1000, 0, 0, 0
        );
        
        assertThat(stats.calculateStars()).isEqualTo(MAX_STARS_PER_DAY);
    }

    @Test
    void recordAnswerUpdatesStats() {
        var stats = new DailyStats(LocalDate.now());
        
        stats.recordAnswer(true);
        assertThat(stats.problemsSolved()).isEqualTo(1);
        assertThat(stats.problemsCorrect()).isEqualTo(1);
        assertThat(stats.currentStreak()).isEqualTo(1);
        assertThat(stats.bestStreak()).isEqualTo(1);

        stats.recordAnswer(false);
        assertThat(stats.problemsSolved()).isEqualTo(2);
        assertThat(stats.problemsCorrect()).isEqualTo(1);
        assertThat(stats.currentStreak()).isEqualTo(0);
        assertThat(stats.bestStreak()).isEqualTo(1);
    }

    @Test
    void recordAnswerUpdatesStars() {
        var stats = new DailyStats(LocalDate.now());
        
        // Answer PROBLEMS_PER_STAR correct answers
        for (int i = 0; i < PROBLEMS_PER_STAR; i++) {
            stats.recordAnswer(true);
        }
        
        assertThat(stats.starsEarned()).isEqualTo(1);
    }

    @Test
    void accuracyCalculation() {
        var stats = new DailyStats(LocalDate.now(), 10, 7, 0, 0, 0);
        assertThat(stats.accuracy()).isEqualTo(70.0);
    }

    @Test
    void calculateNewStarsNoBelowThreshold() {
        int newStars = calculateNewStars(5, 8);
        assertThat(newStars).isEqualTo(0);
    }

    @Test
    void calculateNewStarsOneCrossing() {
        int newStars = calculateNewStars(PROBLEMS_PER_STAR - 1, PROBLEMS_PER_STAR);
        assertThat(newStars).isEqualTo(1);
    }

    @Test
    void calculateNewStarsMultiple() {
        int newStars = calculateNewStars(0, PROBLEMS_PER_STAR * 3);
        assertThat(newStars).isEqualTo(3);
    }

    @Test
    void calculateNewStarsRespectsMax() {
        int newStars = calculateNewStars(0, PROBLEMS_PER_STAR * 100);
        assertThat(newStars).isEqualTo(MAX_STARS_PER_DAY);
    }
}
