package mathquiz.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemTest {

    @Test
    void additionAnswer() {
        var p = new Problem(3, 5, Operation.ADDITION);
        assertThat(p.answer()).isEqualTo(8);
    }

    @Test
    void subtractionAnswer() {
        var p = new Problem(10, 4, Operation.SUBTRACTION);
        assertThat(p.answer()).isEqualTo(6);
    }

    @Test
    void multiplicationAnswer() {
        var p = new Problem(7, 6, Operation.MULTIPLICATION);
        assertThat(p.answer()).isEqualTo(42);
    }

    @Test
    void divisionAnswer() {
        var p = new Problem(20, 4, Operation.DIVISION);
        assertThat(p.answer()).isEqualTo(5);
    }

    @Test
    void checkCorrect() {
        var p = new Problem(2, 3, Operation.ADDITION);
        assertThat(p.check(5)).isTrue();
    }

    @Test
    void checkIncorrect() {
        var p = new Problem(2, 3, Operation.ADDITION);
        assertThat(p.check(6)).isFalse();
    }

    @Test
    void stringRepresentation() {
        var p1 = new Problem(3, 4, Operation.ADDITION);
        assertThat(p1.toString()).isEqualTo("3 + 4");

        var p2 = new Problem(10, 5, Operation.SUBTRACTION);
        assertThat(p2.toString()).isEqualTo("10 - 5");

        var p3 = new Problem(6, 7, Operation.MULTIPLICATION);
        assertThat(p3.toString()).isEqualTo("6 × 7");

        var p4 = new Problem(24, 6, Operation.DIVISION);
        assertThat(p4.toString()).isEqualTo("24 ÷ 6");
    }

    @Test
    void problemIsImmutable() {
        var p1 = new Problem(1, 2, Operation.ADDITION);
        var p2 = new Problem(1, 2, Operation.ADDITION);
        
        // Records are value-based
        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }
}
