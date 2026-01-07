package mathquiz.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemPoolTest {

    @Test
    void additionPoolSize() {
        // maxSum=5: pairs where a+b <= 5, a,b >= 1
        // (1,1), (1,2), (1,3), (1,4), (2,1), (2,2), (2,3), (3,1), (3,2), (4,1) = 10
        var pool = ProblemPool.forAddition(5);
        assertThat(pool.size()).isEqualTo(10);
        assertThat(pool.operation()).isEqualTo(Operation.ADDITION);
    }

    @Test
    void additionPoolAllSumsWithinBound() {
        var pool = ProblemPool.forAddition(10);
        for (var p : pool.all()) {
            assertThat(p.operation()).isEqualTo(Operation.ADDITION);
            assertThat(p.operand1()).isGreaterThanOrEqualTo(1);
            assertThat(p.operand2()).isGreaterThanOrEqualTo(1);
            assertThat(p.operand1() + p.operand2()).isLessThanOrEqualTo(10);
        }
    }

    @Test
    void subtractionPoolSize() {
        // maxMinuend=5: pairs where a <= 5, b < a, a,b >= 1
        // (2,1), (3,1), (3,2), (4,1), (4,2), (4,3), (5,1), (5,2), (5,3), (5,4) = 10
        var pool = ProblemPool.forSubtraction(5);
        assertThat(pool.size()).isEqualTo(10);
    }

    @Test
    void subtractionPoolAllResultsNonNegative() {
        var pool = ProblemPool.forSubtraction(10);
        for (var p : pool.all()) {
            assertThat(p.operation()).isEqualTo(Operation.SUBTRACTION);
            assertThat(p.operand1()).isGreaterThanOrEqualTo(p.operand2());
            assertThat(p.answer()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void multiplicationPoolSize() {
        // maxFactor=5: 5×5 = 25 pairs
        var pool = ProblemPool.forMultiplication(5);
        assertThat(pool.size()).isEqualTo(25);
    }

    @Test
    void multiplicationPoolAllWithinBounds() {
        var pool = ProblemPool.forMultiplication(5);
        for (var p : pool.all()) {
            assertThat(p.operation()).isEqualTo(Operation.MULTIPLICATION);
            assertThat(p.operand1()).isBetween(1, 5);
            assertThat(p.operand2()).isBetween(1, 5);
        }
    }

    @Test
    void divisionPoolSize() {
        // maxFactor=5: divisor 1..5, quotient 1..5 = 25 pairs
        var pool = ProblemPool.forDivision(5);
        assertThat(pool.size()).isEqualTo(25);
    }

    @Test
    void divisionPoolAllExactDivision() {
        var pool = ProblemPool.forDivision(5);
        for (var p : pool.all()) {
            assertThat(p.operation()).isEqualTo(Operation.DIVISION);
            assertThat(p.operand1() % p.operand2())
                .as("Division must be exact: %d ÷ %d", p.operand1(), p.operand2())
                .isEqualTo(0);
            assertThat(p.answer()).isBetween(1, 5);
            assertThat(p.operand2()).isBetween(1, 5);
        }
    }

    @Test
    void pickRandomReturnsFromPool() {
        var pool = ProblemPool.forAddition(10);
        var random = new Random(42);
        
        for (int i = 0; i < 100; i++) {
            var problem = pool.pickRandom(random);
            assertThat(pool.all()).contains(problem);
        }
    }

    @Test
    void forOperationCreatesCorrectPool() {
        assertThat(ProblemPool.forOperation(Operation.ADDITION, 10).operation())
            .isEqualTo(Operation.ADDITION);
        assertThat(ProblemPool.forOperation(Operation.SUBTRACTION, 10).operation())
            .isEqualTo(Operation.SUBTRACTION);
        assertThat(ProblemPool.forOperation(Operation.MULTIPLICATION, 5).operation())
            .isEqualTo(Operation.MULTIPLICATION);
        assertThat(ProblemPool.forOperation(Operation.DIVISION, 5).operation())
            .isEqualTo(Operation.DIVISION);
    }
}
