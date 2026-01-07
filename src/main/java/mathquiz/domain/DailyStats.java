package mathquiz.domain;

import java.time.LocalDate;

/**
 * Stats for a single day.
 * Mutable - updated as problems are solved.
 */
public class DailyStats {

    public static final int PROBLEMS_PER_STAR = 10;
    public static final int MAX_STARS_PER_DAY = 10;

    private final LocalDate date;
    private int problemsSolved;
    private int problemsCorrect;
    private int starsEarned;
    private int bestStreak;
    private int currentStreak;

    public DailyStats(LocalDate date) {
        this(date, 0, 0, 0, 0, 0);
    }

    public DailyStats(
            LocalDate date,
            int problemsSolved,
            int problemsCorrect,
            int starsEarned,
            int bestStreak,
            int currentStreak) {
        this.date = date;
        this.problemsSolved = problemsSolved;
        this.problemsCorrect = problemsCorrect;
        this.starsEarned = starsEarned;
        this.bestStreak = bestStreak;
        this.currentStreak = currentStreak;
    }

    public LocalDate date() {
        return date;
    }

    public int problemsSolved() {
        return problemsSolved;
    }

    public int problemsCorrect() {
        return problemsCorrect;
    }

    public int starsEarned() {
        return starsEarned;
    }

    public int bestStreak() {
        return bestStreak;
    }

    public int currentStreak() {
        return currentStreak;
    }

    public double accuracy() {
        if (problemsSolved == 0) {
            return 0.0;
        }
        return (problemsCorrect * 100.0) / problemsSolved;
    }

    /**
     * Calculate stars earned based on correct answers.
     */
    public int calculateStars() {
        int earned = problemsCorrect / PROBLEMS_PER_STAR;
        return Math.min(earned, MAX_STARS_PER_DAY);
    }

    /**
     * Record an answer for today.
     */
    public void recordAnswer(boolean correct) {
        problemsSolved++;
        if (correct) {
            problemsCorrect++;
            currentStreak++;
            if (currentStreak > bestStreak) {
                bestStreak = currentStreak;
            }
        } else {
            currentStreak = 0;
        }
        starsEarned = calculateStars();
    }

    /**
     * Calculate how many NEW stars were earned.
     *
     * @param previousCorrect Total correct answers before this session
     * @param newCorrect      Total correct answers after this session
     * @return Number of newly earned stars (0 if threshold not crossed)
     */
    public static int calculateNewStars(int previousCorrect, int newCorrect) {
        int prevStars = Math.min(previousCorrect / PROBLEMS_PER_STAR, MAX_STARS_PER_DAY);
        int newStars = Math.min(newCorrect / PROBLEMS_PER_STAR, MAX_STARS_PER_DAY);
        return newStars - prevStars;
    }
}
