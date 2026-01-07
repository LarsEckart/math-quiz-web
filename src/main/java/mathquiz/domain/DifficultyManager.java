package mathquiz.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static mathquiz.domain.OperationProgress.RANGE_TO_UNLOCK_NEXT_OP;

/**
 * Manages difficulty progression across all operations.
 */
public class DifficultyManager {

    /** Order of operations for unlocking */
    private static final List<Operation> OPERATION_ORDER = List.of(
        Operation.ADDITION,
        Operation.SUBTRACTION,
        Operation.MULTIPLICATION,
        Operation.DIVISION
    );

    private final Map<Operation, OperationProgress> operations;

    public DifficultyManager() {
        this.operations = new EnumMap<>(Operation.class);
        for (Operation op : Operation.values()) {
            OperationProgress progress = new OperationProgress(op);
            // Only addition unlocked by default
            if (op == Operation.ADDITION) {
                progress.unlock();
            }
            operations.put(op, progress);
        }
    }

    /**
     * Create a DifficultyManager with pre-existing progress (e.g., from database).
     */
    public DifficultyManager(Map<Operation, OperationProgress> operations) {
        this.operations = new EnumMap<>(operations);
    }

    /**
     * Get list of currently unlocked operations.
     */
    public List<Operation> getUnlockedOperations() {
        List<Operation> unlocked = new ArrayList<>();
        for (Operation op : Operation.values()) {
            if (operations.get(op).isUnlocked()) {
                unlocked.add(op);
            }
        }
        return unlocked;
    }

    /**
     * Get the current number range for an operation.
     * @return Tuple of (minValue, maxValue)
     */
    public int[] getRange(Operation operation) {
        OperationProgress progress = operations.get(operation);
        return new int[] { 1, progress.maxNumber() };
    }

    /**
     * Get the progress for a specific operation.
     */
    public OperationProgress getProgress(Operation operation) {
        return operations.get(operation);
    }

    /**
     * Record an attempt and check for progression.
     * @return true if range was expanded or new operation was unlocked
     */
    public boolean recordAttempt(Operation operation, boolean correct) {
        OperationProgress progress = operations.get(operation);
        progress.recordAttempt(correct);

        boolean progressionOccurred = false;

        if (progress.shouldExpandRange()) {
            progress.expandRange();
            progressionOccurred = true;

            // Check if this unlocks a new operation
            if (progress.maxNumber() >= RANGE_TO_UNLOCK_NEXT_OP) {
                Operation nextOp = getNextOperation(operation);
                if (nextOp != null && !operations.get(nextOp).isUnlocked()) {
                    operations.get(nextOp).unlock();
                    progressionOccurred = true;
                }
            }
        }

        return progressionOccurred;
    }

    /**
     * Get the next operation in sequence, if any.
     */
    private Operation getNextOperation(Operation current) {
        int idx = OPERATION_ORDER.indexOf(current);
        if (idx >= 0 && idx + 1 < OPERATION_ORDER.size()) {
            return OPERATION_ORDER.get(idx + 1);
        }
        return null;
    }

    /**
     * Manually unlock an operation.
     * @return true if operation was newly unlocked, false if already unlocked
     */
    public boolean unlockOperation(Operation operation) {
        OperationProgress progress = operations.get(operation);
        if (progress.isUnlocked()) {
            return false;
        }
        progress.manualUnlock();
        return true;
    }

    /**
     * Check if an operation is unlocked.
     */
    public boolean isUnlocked(Operation operation) {
        return operations.get(operation).isUnlocked();
    }
}
