package mathquiz.domain;

/**
 * Immutable math problem with two operands and an operation.
 */
public record Problem(int operand1, int operand2, Operation operation) {

    /**
     * Calculate the correct answer.
     */
    public int answer() {
        return switch (operation) {
            case ADDITION -> operand1 + operand2;
            case SUBTRACTION -> operand1 - operand2;
            case MULTIPLICATION -> operand1 * operand2;
            case DIVISION -> operand1 / operand2;
        };
    }

    /**
     * Check if the given response is correct.
     */
    public boolean check(int response) {
        return response == answer();
    }

    @Override
    public String toString() {
        return operand1 + " " + operation.symbol() + " " + operand2;
    }
}
