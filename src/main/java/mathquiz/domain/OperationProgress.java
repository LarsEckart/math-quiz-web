package mathquiz.domain;

/**
 * Tracks progress for a single operation.
 * Mutable - updated as the user solves problems.
 */
public class OperationProgress {

    // Thresholds
    public static final double ACCURACY_THRESHOLD_TO_EXPAND = 90.0;
    public static final int MIN_PROBLEMS_TO_EXPAND = 10;
    public static final int RANGE_TO_UNLOCK_NEXT_OP = 25;

    // Starting and max ranges per operation
    private static int startingRange(Operation op) {
        return switch (op) {
            case ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION -> 5;
        };
    }

    private static int maxRange(Operation op) {
        return switch (op) {
            case ADDITION, SUBTRACTION -> 50;
            case MULTIPLICATION, DIVISION -> 10;
        };
    }

    private final Operation operation;
    private int maxNumber;
    private boolean unlocked;
    private boolean manuallyUnlocked;
    private int problemsAtCurrentRange;
    private int correctAtCurrentRange;

    public OperationProgress(Operation operation) {
        this(operation, startingRange(operation), false, false, 0, 0);
    }

    public OperationProgress(
            Operation operation,
            int maxNumber,
            boolean unlocked,
            boolean manuallyUnlocked,
            int problemsAtCurrentRange,
            int correctAtCurrentRange) {
        this.operation = operation;
        this.maxNumber = maxNumber;
        this.unlocked = unlocked;
        this.manuallyUnlocked = manuallyUnlocked;
        this.problemsAtCurrentRange = problemsAtCurrentRange;
        this.correctAtCurrentRange = correctAtCurrentRange;
    }

    public Operation operation() {
        return operation;
    }

    public int maxNumber() {
        return maxNumber;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean isManuallyUnlocked() {
        return manuallyUnlocked;
    }

    public int problemsAtCurrentRange() {
        return problemsAtCurrentRange;
    }

    public int correctAtCurrentRange() {
        return correctAtCurrentRange;
    }

    public int getMaxRange() {
        return maxRange(operation);
    }

    public double accuracyAtCurrentRange() {
        if (problemsAtCurrentRange == 0) {
            return 0.0;
        }
        return (correctAtCurrentRange * 100.0) / problemsAtCurrentRange;
    }

    public boolean shouldExpandRange() {
        if (maxNumber >= getMaxRange()) {
            return false;
        }
        if (problemsAtCurrentRange < MIN_PROBLEMS_TO_EXPAND) {
            return false;
        }
        return accuracyAtCurrentRange() >= ACCURACY_THRESHOLD_TO_EXPAND;
    }

    /**
     * Expand the number range and reset range stats.
     * @param amount Amount to expand by
     */
    public void expandRange(int amount) {
        maxNumber = Math.min(getMaxRange(), maxNumber + amount);
        problemsAtCurrentRange = 0;
        correctAtCurrentRange = 0;
    }

    /**
     * Expand range by default amount (5).
     */
    public void expandRange() {
        expandRange(5);
    }

    /**
     * Record an attempt at the current range.
     */
    public void recordAttempt(boolean correct) {
        problemsAtCurrentRange++;
        if (correct) {
            correctAtCurrentRange++;
        }
    }

    /**
     * Unlock this operation.
     */
    public void unlock() {
        unlocked = true;
    }

    /**
     * Manually unlock this operation (tracks that it was manual).
     */
    public void manualUnlock() {
        unlocked = true;
        manuallyUnlocked = true;
    }
}
