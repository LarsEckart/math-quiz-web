package mathquiz.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionStatsTest {

    @Test
    void initialValues() {
        var stats = new SessionStats();
        
        assertThat(stats.currentStreak()).isEqualTo(0);
        assertThat(stats.bestStreakToday()).isEqualTo(0);
        assertThat(stats.problemsSolved()).isEqualTo(0);
        assertThat(stats.problemsCorrect()).isEqualTo(0);
    }

    @Test
    void correctAnswerIncreasesStreak() {
        var stats = new SessionStats();
        
        stats.recordAnswer(true);
        
        assertThat(stats.currentStreak()).isEqualTo(1);
        assertThat(stats.bestStreakToday()).isEqualTo(1);
        assertThat(stats.problemsCorrect()).isEqualTo(1);
    }

    @Test
    void incorrectAnswerResetsStreak() {
        var stats = new SessionStats();
        stats.recordAnswer(true);
        stats.recordAnswer(true);
        
        stats.recordAnswer(false);
        
        assertThat(stats.currentStreak()).isEqualTo(0);
        assertThat(stats.bestStreakToday()).isEqualTo(2);
    }

    @Test
    void bestStreakPreserved() {
        var stats = new SessionStats();
        
        // Build a streak of 5
        for (int i = 0; i < 5; i++) {
            stats.recordAnswer(true);
        }
        assertThat(stats.bestStreakToday()).isEqualTo(5);

        // Break it
        stats.recordAnswer(false);
        assertThat(stats.currentStreak()).isEqualTo(0);
        assertThat(stats.bestStreakToday()).isEqualTo(5);

        // Start new streak of 3
        for (int i = 0; i < 3; i++) {
            stats.recordAnswer(true);
        }
        assertThat(stats.currentStreak()).isEqualTo(3);
        assertThat(stats.bestStreakToday()).isEqualTo(5);  // Still 5
    }

    @Test
    void accuracyCalculation() {
        var stats = new SessionStats();
        
        for (int i = 0; i < 7; i++) {
            stats.recordAnswer(true);
        }
        for (int i = 0; i < 3; i++) {
            stats.recordAnswer(false);
        }
        
        assertThat(stats.accuracy()).isEqualTo(70.0);
    }

    @Test
    void accuracyZeroDivision() {
        var stats = new SessionStats();
        assertThat(stats.accuracy()).isEqualTo(0.0);
    }
}
