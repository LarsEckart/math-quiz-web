package mathquiz.domain;

import org.junit.jupiter.api.Test;

import static mathquiz.domain.OperationProgress.*;
import static org.assertj.core.api.Assertions.assertThat;

class DifficultyManagerTest {

    @Test
    void onlyAdditionUnlockedByDefault() {
        var dm = new DifficultyManager();
        var unlocked = dm.getUnlockedOperations();
        
        assertThat(unlocked).containsExactly(Operation.ADDITION);
        assertThat(dm.isUnlocked(Operation.ADDITION)).isTrue();
        assertThat(dm.isUnlocked(Operation.SUBTRACTION)).isFalse();
        assertThat(dm.isUnlocked(Operation.MULTIPLICATION)).isFalse();
        assertThat(dm.isUnlocked(Operation.DIVISION)).isFalse();
    }

    @Test
    void getRangeReturnsTuple() {
        var dm = new DifficultyManager();
        int[] range = dm.getRange(Operation.ADDITION);
        
        assertThat(range[0]).isEqualTo(1);
        assertThat(range[1]).isEqualTo(5);  // Starting range for addition
    }

    @Test
    void recordAttemptUpdatesProgress() {
        var dm = new DifficultyManager();
        
        dm.recordAttempt(Operation.ADDITION, true);
        
        var prog = dm.getProgress(Operation.ADDITION);
        assertThat(prog.problemsAtCurrentRange()).isEqualTo(1);
        assertThat(prog.correctAtCurrentRange()).isEqualTo(1);
    }

    @Test
    void unlockOperationManually() {
        var dm = new DifficultyManager();
        assertThat(dm.isUnlocked(Operation.MULTIPLICATION)).isFalse();

        boolean result = dm.unlockOperation(Operation.MULTIPLICATION);
        
        assertThat(result).isTrue();
        assertThat(dm.isUnlocked(Operation.MULTIPLICATION)).isTrue();
        assertThat(dm.getProgress(Operation.MULTIPLICATION).isManuallyUnlocked()).isTrue();
    }

    @Test
    void unlockAlreadyUnlockedReturnsFalse() {
        var dm = new DifficultyManager();
        
        boolean result = dm.unlockOperation(Operation.ADDITION);
        
        assertThat(result).isFalse();
    }

    @Test
    void rangeExpansionReturnsTrue() {
        var dm = new DifficultyManager();
        var prog = dm.getProgress(Operation.ADDITION);
        int originalMax = prog.maxNumber();

        // Set up to trigger expansion on next correct answer
        for (int i = 0; i < MIN_PROBLEMS_TO_EXPAND - 1; i++) {
            prog.recordAttempt(true);
        }

        // This should trigger expansion
        boolean result = dm.recordAttempt(Operation.ADDITION, true);
        
        assertThat(result).isTrue();
        assertThat(prog.maxNumber()).isGreaterThan(originalMax);
    }

    @Test
    void subtractionUnlocksWhenAdditionReachesThreshold() {
        var dm = new DifficultyManager();
        var addProgress = dm.getProgress(Operation.ADDITION);

        // Set addition to be near unlock threshold
        // RANGE_TO_UNLOCK_NEXT_OP is 25, starting is 5, so need 4 expansions of 5
        // Let's set it to 20 directly and trigger one more expansion
        while (addProgress.maxNumber() < RANGE_TO_UNLOCK_NEXT_OP - 5) {
            addProgress.expandRange(5);
        }

        // Now set up to trigger one more expansion
        for (int i = 0; i < MIN_PROBLEMS_TO_EXPAND - 1; i++) {
            addProgress.recordAttempt(true);
        }

        // This should trigger expansion and unlock subtraction
        dm.recordAttempt(Operation.ADDITION, true);

        assertThat(addProgress.maxNumber()).isGreaterThanOrEqualTo(RANGE_TO_UNLOCK_NEXT_OP);
        assertThat(dm.isUnlocked(Operation.SUBTRACTION)).isTrue();
    }

    @Test
    void noProgressionWithLowAccuracy() {
        var dm = new DifficultyManager();
        var prog = dm.getProgress(Operation.ADDITION);

        // Answer many problems with low accuracy
        for (int i = 0; i < 20; i++) {
            dm.recordAttempt(Operation.ADDITION, i % 2 == 0);  // 50% accuracy
        }

        // Range should not have expanded
        assertThat(prog.maxNumber()).isEqualTo(5);
    }
}
