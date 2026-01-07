package mathquiz.domain;

/**
 * Stats for the current session.
 * Mutable - updated as answers are recorded.
 */
public class SessionStats {

    private int currentStreak;
    private int bestStreakToday;
    private int problemsSolved;
    private int problemsCorrect;

    public SessionStats() {
        this(0, 0, 0, 0);
    }

    public SessionStats(int currentStreak, int bestStreakToday, int problemsSolved, int problemsCorrect) {
        this.currentStreak = currentStreak;
        this.bestStreakToday = bestStreakToday;
        this.problemsSolved = problemsSolved;
        this.problemsCorrect = problemsCorrect;
    }

    public int currentStreak() {
        return currentStreak;
    }

    public int bestStreakToday() {
        return bestStreakToday;
    }

    public int problemsSolved() {
        return problemsSolved;
    }

    public int problemsCorrect() {
        return problemsCorrect;
    }

    public double accuracy() {
        if (problemsSolved == 0) {
            return 0.0;
        }
        return (problemsCorrect * 100.0) / problemsSolved;
    }

    /**
     * Record an answer and update streaks.
     */
    public void recordAnswer(boolean correct) {
        problemsSolved++;

        if (correct) {
            problemsCorrect++;
            currentStreak++;
            if (currentStreak > bestStreakToday) {
                bestStreakToday = currentStreak;
            }
        } else {
            currentStreak = 0;
        }
    }
}
