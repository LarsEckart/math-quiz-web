package mathquiz.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pre-generated pool of all valid problems for an operation and range.
 * Ensures uniform distribution when picking randomly.
 */
public final class ProblemPool {
    private final List<Problem> problems;
    private final Operation operation;

    private ProblemPool(Operation operation, List<Problem> problems) {
        this.operation = operation;
        this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
    }

    /**
     * Create a pool of addition problems where sum <= maxSum.
     * e.g., maxSum=10 → 1+1, 1+2, ..., 5+5 (all pairs where a+b <= maxSum)
     */
    public static ProblemPool forAddition(int maxSum) {
        List<Problem> pool = new ArrayList<>();
        for (int a = 1; a <= maxSum - 1; a++) {
            for (int b = 1; b <= maxSum - a; b++) {
                pool.add(new Problem(a, b, Operation.ADDITION));
            }
        }
        return new ProblemPool(Operation.ADDITION, pool);
    }

    /**
     * Create a pool of subtraction problems where minuend <= maxMinuend.
     * e.g., maxMinuend=10 → 2-1, 3-1, 3-2, ..., 10-9
     * Results are always non-negative (a >= b).
     */
    public static ProblemPool forSubtraction(int maxMinuend) {
        List<Problem> pool = new ArrayList<>();
        for (int a = 2; a <= maxMinuend; a++) {
            for (int b = 1; b < a; b++) {
                pool.add(new Problem(a, b, Operation.SUBTRACTION));
            }
        }
        return new ProblemPool(Operation.SUBTRACTION, pool);
    }

    /**
     * Create a pool of multiplication problems where both factors <= maxFactor.
     * e.g., maxFactor=5 → 1×1, 1×2, ..., 5×5
     */
    public static ProblemPool forMultiplication(int maxFactor) {
        List<Problem> pool = new ArrayList<>();
        for (int a = 1; a <= maxFactor; a++) {
            for (int b = 1; b <= maxFactor; b++) {
                pool.add(new Problem(a, b, Operation.MULTIPLICATION));
            }
        }
        return new ProblemPool(Operation.MULTIPLICATION, pool);
    }

    /**
     * Create a pool of division problems where divisor and quotient <= maxFactor.
     * e.g., maxFactor=5 → 1÷1, 2÷1, 2÷2, ..., 25÷5
     * All divisions are exact (no remainder).
     */
    public static ProblemPool forDivision(int maxFactor) {
        List<Problem> pool = new ArrayList<>();
        for (int divisor = 1; divisor <= maxFactor; divisor++) {
            for (int quotient = 1; quotient <= maxFactor; quotient++) {
                int dividend = divisor * quotient;
                pool.add(new Problem(dividend, divisor, Operation.DIVISION));
            }
        }
        return new ProblemPool(Operation.DIVISION, pool);
    }

    /**
     * Create a pool for the given operation and max number.
     */
    public static ProblemPool forOperation(Operation operation, int maxNumber) {
        return switch (operation) {
            case ADDITION -> forAddition(maxNumber);
            case SUBTRACTION -> forSubtraction(maxNumber);
            case MULTIPLICATION -> forMultiplication(maxNumber);
            case DIVISION -> forDivision(maxNumber);
        };
    }

    /**
     * Pick a random problem from the pool.
     */
    public Problem pickRandom(Random random) {
        if (problems.isEmpty()) {
            throw new IllegalStateException("Cannot pick from empty pool");
        }
        return problems.get(random.nextInt(problems.size()));
    }

    /**
     * Get all problems in the pool.
     */
    public List<Problem> all() {
        return problems;
    }

    /**
     * Get the number of problems in the pool.
     */
    public int size() {
        return problems.size();
    }

    /**
     * Get the operation for this pool.
     */
    public Operation operation() {
        return operation;
    }
}
