package mathquiz.domain;

import org.junit.jupiter.api.Test;

import static mathquiz.domain.OperationProgress.*;
import static org.assertj.core.api.Assertions.assertThat;

class OperationProgressTest {

    @Test
    void initialValues() {
        var prog = new OperationProgress(Operation.ADDITION);

        assertThat(prog.maxNumber()).isEqualTo(5); // STARTING_RANGE for addition
        assertThat(prog.isUnlocked()).isFalse();
        assertThat(prog.problemsAtCurrentRange()).isEqualTo(0);
    }

    @Test
    void accuracyCalculation() {
        var prog = new OperationProgress(
            Operation.ADDITION, 5, false, false, 10, 9
        );
        assertThat(prog.accuracyAtCurrentRange()).isEqualTo(90.0);
    }

    @Test
    void shouldNotExpandWithoutEnoughProblems() {
        var prog = new OperationProgress(
            Operation.ADDITION, 5, false, false,
            5,  // Less than MIN_PROBLEMS_TO_EXPAND
            5   // 100% accuracy
        );
        assertThat(prog.shouldExpandRange()).isFalse();
    }

    @Test
    void shouldNotExpandWithLowAccuracy() {
        var prog = new OperationProgress(
            Operation.ADDITION, 5, false, false,
            MIN_PROBLEMS_TO_EXPAND,
            5  // 50% accuracy
        );
        assertThat(prog.shouldExpandRange()).isFalse();
    }

    @Test
    void shouldExpandWithHighAccuracyAndEnoughProblems() {
        var prog = new OperationProgress(
            Operation.ADDITION, 5, false, false,
            MIN_PROBLEMS_TO_EXPAND,
            MIN_PROBLEMS_TO_EXPAND  // 100%
        );
        assertThat(prog.shouldExpandRange()).isTrue();
    }

    @Test
    void shouldNotExpandBeyondMaxRange() {
        var prog = new OperationProgress(
            Operation.ADDITION, 50, false, false,  // 50 is max for addition
            MIN_PROBLEMS_TO_EXPAND,
            MIN_PROBLEMS_TO_EXPAND
        );
        assertThat(prog.shouldExpandRange()).isFalse();
    }

    @Test
    void expandRangeIncreasesMax() {
        var prog = new OperationProgress(Operation.ADDITION);
        int originalMax = prog.maxNumber();
        
        prog.expandRange(5);
        
        assertThat(prog.maxNumber()).isEqualTo(originalMax + 5);
    }

    @Test
    void expandRangeResetsStats() {
        var prog = new OperationProgress(
            Operation.ADDITION, 5, false, false, 10, 9
        );
        
        prog.expandRange();
        
        assertThat(prog.problemsAtCurrentRange()).isEqualTo(0);
        assertThat(prog.correctAtCurrentRange()).isEqualTo(0);
    }

    @Test
    void expandRangeCapsAtMax() {
        var prog = new OperationProgress(
            Operation.ADDITION, 48, false, false, 0, 0
        );
        
        prog.expandRange(5);  // Would go to 53, but max is 50
        
        assertThat(prog.maxNumber()).isEqualTo(50);
    }

    @Test
    void recordAttemptUpdatesStats() {
        var prog = new OperationProgress(Operation.ADDITION);
        
        prog.recordAttempt(true);
        assertThat(prog.problemsAtCurrentRange()).isEqualTo(1);
        assertThat(prog.correctAtCurrentRange()).isEqualTo(1);

        prog.recordAttempt(false);
        assertThat(prog.problemsAtCurrentRange()).isEqualTo(2);
        assertThat(prog.correctAtCurrentRange()).isEqualTo(1);
    }

    @Test
    void manualUnlockSetsFlag() {
        var prog = new OperationProgress(Operation.MULTIPLICATION);
        
        prog.manualUnlock();
        
        assertThat(prog.isUnlocked()).isTrue();
        assertThat(prog.isManuallyUnlocked()).isTrue();
    }
}
