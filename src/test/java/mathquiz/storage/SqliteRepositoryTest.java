package mathquiz.storage;

import mathquiz.domain.*;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for SqliteRepository.
 * Uses in-memory SQLite database for isolation.
 */
class SqliteRepositoryTest {

    private DatabaseSetup.SetupResult setup;
    private SqliteRepository repo;

    @BeforeEach
    void setUp() {
        setup = DatabaseSetup.setupInMemory();
        repo = new SqliteRepository(setup.jdbi());
    }

    @AfterEach
    void tearDown() {
        setup.close();
    }

    // --- User tests ---

    @Test
    void createUser_assignsIdAndTimestamp() {
        User user = repo.createUser("Alice");

        assertThat(user.id()).isGreaterThan(0);
        assertThat(user.name()).isEqualTo("Alice");
        assertThat(user.createdAt()).isNotNull();
    }

    @Test
    void getUsers_returnsAllUsersOrderedByName() {
        repo.createUser("Zoe");
        repo.createUser("Alice");
        repo.createUser("Bob");

        List<User> users = repo.getUsers();

        assertThat(users).hasSize(3);
        assertThat(users.get(0).name()).isEqualTo("Alice");
        assertThat(users.get(1).name()).isEqualTo("Bob");
        assertThat(users.get(2).name()).isEqualTo("Zoe");
    }

    @Test
    void getUser_existingUser_returnsUser() {
        User created = repo.createUser("Alice");

        Optional<User> fetched = repo.getUser(created.id());

        assertThat(fetched).isPresent();
        assertThat(fetched.get().name()).isEqualTo("Alice");
    }

    @Test
    void getUser_nonExistentUser_returnsEmpty() {
        Optional<User> fetched = repo.getUser(999);

        assertThat(fetched).isEmpty();
    }

    @Test
    void createUser_duplicateName_throwsException() {
        repo.createUser("Alice");

        assertThatThrownBy(() -> repo.createUser("Alice"))
            .isInstanceOf(Exception.class);
    }

    // --- Problem stats tests ---

    @Test
    void saveProblemStats_newStats_canBeRetrieved() {
        User user = repo.createUser("Alice");
        ProblemStats stats = new ProblemStats(
            Operation.ADDITION, 3, 5,
            2.5, 1.0, Instant.now().plusSeconds(86400),
            1, 5, 4
        );

        repo.saveProblemStats(user.id(), stats);

        Optional<ProblemStats> fetched = repo.getProblemStats(
            user.id(), Operation.ADDITION, 3, 5
        );
        assertThat(fetched).isPresent();
        assertThat(fetched.get().easeFactor()).isEqualTo(2.5);
        assertThat(fetched.get().totalAttempts()).isEqualTo(5);
        assertThat(fetched.get().totalCorrect()).isEqualTo(4);
    }

    @Test
    void saveProblemStats_updateExisting_updatesValues() {
        User user = repo.createUser("Alice");
        ProblemStats initial = new ProblemStats(
            Operation.ADDITION, 3, 5,
            2.5, 1.0, null, 1, 5, 4
        );
        repo.saveProblemStats(user.id(), initial);

        ProblemStats updated = new ProblemStats(
            Operation.ADDITION, 3, 5,
            2.3, 2.0, Instant.now(), 2, 10, 8
        );
        repo.saveProblemStats(user.id(), updated);

        Optional<ProblemStats> fetched = repo.getProblemStats(
            user.id(), Operation.ADDITION, 3, 5
        );
        assertThat(fetched).isPresent();
        assertThat(fetched.get().easeFactor()).isEqualTo(2.3);
        assertThat(fetched.get().totalAttempts()).isEqualTo(10);
    }

    @Test
    void getAllProblemStats_returnsAllForUser() {
        User user = repo.createUser("Alice");
        repo.saveProblemStats(user.id(), ProblemStats.newStats(Operation.ADDITION, 1, 2));
        repo.saveProblemStats(user.id(), ProblemStats.newStats(Operation.ADDITION, 3, 4));
        repo.saveProblemStats(user.id(), ProblemStats.newStats(Operation.SUBTRACTION, 5, 3));

        List<ProblemStats> all = repo.getAllProblemStats(user.id());

        assertThat(all).hasSize(3);
    }

    @Test
    void getDueProblems_returnsDueAndNeverReviewed() {
        User user = repo.createUser("Alice");
        Instant now = Instant.now();

        // Due problem (review time in the past)
        ProblemStats due = new ProblemStats(
            Operation.ADDITION, 1, 1,
            2.5, 1.0, now.minusSeconds(3600), 1, 1, 1
        );
        repo.saveProblemStats(user.id(), due);

        // Not due (review time in future)
        ProblemStats notDue = new ProblemStats(
            Operation.ADDITION, 2, 2,
            2.5, 1.0, now.plusSeconds(86400), 1, 1, 1
        );
        repo.saveProblemStats(user.id(), notDue);

        // Never reviewed (null next_review)
        ProblemStats neverReviewed = ProblemStats.newStats(Operation.ADDITION, 3, 3);
        repo.saveProblemStats(user.id(), neverReviewed);

        List<ProblemStats> dueProblems = repo.getDueProblems(user.id(), now, 10);

        assertThat(dueProblems).hasSize(2);
        // Never reviewed should come first (NULLS FIRST)
        assertThat(dueProblems.get(0).operand1()).isEqualTo(3);
        assertThat(dueProblems.get(1).operand1()).isEqualTo(1);
    }

    // --- Difficulty tests ---

    @Test
    void getDifficulty_newUser_returnsDefault() {
        User user = repo.createUser("Alice");

        DifficultyManager dm = repo.getDifficulty(user.id());

        assertThat(dm.getUnlockedOperations()).containsExactly(Operation.ADDITION);
        assertThat(dm.getProgress(Operation.ADDITION).maxNumber()).isEqualTo(5);
    }

    @Test
    void saveDifficulty_canBeRetrieved() {
        User user = repo.createUser("Alice");
        DifficultyManager dm = new DifficultyManager();

        // Simulate progress
        dm.getProgress(Operation.ADDITION).expandRange(10);
        dm.unlockOperation(Operation.SUBTRACTION);

        repo.saveDifficulty(user.id(), dm);

        DifficultyManager fetched = repo.getDifficulty(user.id());

        assertThat(fetched.getUnlockedOperations())
            .containsExactlyInAnyOrder(Operation.ADDITION, Operation.SUBTRACTION);
        assertThat(fetched.getProgress(Operation.ADDITION).maxNumber()).isEqualTo(15);
    }

    @Test
    void saveDifficulty_preservesRangeStats() {
        User user = repo.createUser("Alice");
        DifficultyManager dm = new DifficultyManager();

        // Record some attempts
        dm.getProgress(Operation.ADDITION).recordAttempt(true);
        dm.getProgress(Operation.ADDITION).recordAttempt(true);
        dm.getProgress(Operation.ADDITION).recordAttempt(false);

        repo.saveDifficulty(user.id(), dm);

        DifficultyManager fetched = repo.getDifficulty(user.id());
        OperationProgress progress = fetched.getProgress(Operation.ADDITION);

        assertThat(progress.problemsAtCurrentRange()).isEqualTo(3);
        assertThat(progress.correctAtCurrentRange()).isEqualTo(2);
    }

    // --- Daily stats tests ---

    @Test
    void getDailyStats_newDay_returnsEmpty() {
        User user = repo.createUser("Alice");

        Optional<DailyStats> stats = repo.getDailyStats(user.id(), LocalDate.now());

        assertThat(stats).isEmpty();
    }

    @Test
    void saveDailyStats_canBeRetrieved() {
        User user = repo.createUser("Alice");
        LocalDate today = LocalDate.of(2024, 1, 15);
        DailyStats stats = new DailyStats(today, 25, 20, 2, 10, 5);

        repo.saveDailyStats(user.id(), stats);

        Optional<DailyStats> fetched = repo.getDailyStats(user.id(), today);

        assertThat(fetched).isPresent();
        assertThat(fetched.get().problemsSolved()).isEqualTo(25);
        assertThat(fetched.get().problemsCorrect()).isEqualTo(20);
        assertThat(fetched.get().starsEarned()).isEqualTo(2);
        assertThat(fetched.get().bestStreak()).isEqualTo(10);
        assertThat(fetched.get().currentStreak()).isEqualTo(5);
    }

    @Test
    void saveDailyStats_updateExisting_updatesValues() {
        User user = repo.createUser("Alice");
        LocalDate today = LocalDate.of(2024, 1, 15);

        DailyStats initial = new DailyStats(today, 10, 8, 0, 5, 3);
        repo.saveDailyStats(user.id(), initial);

        DailyStats updated = new DailyStats(today, 25, 20, 2, 10, 5);
        repo.saveDailyStats(user.id(), updated);

        Optional<DailyStats> fetched = repo.getDailyStats(user.id(), today);

        assertThat(fetched).isPresent();
        assertThat(fetched.get().problemsSolved()).isEqualTo(25);
    }

    @Test
    void getTotalStars_sumsAcrossDays() {
        User user = repo.createUser("Alice");

        repo.saveDailyStats(user.id(), new DailyStats(LocalDate.of(2024, 1, 1), 30, 30, 3, 5, 5));
        repo.saveDailyStats(user.id(), new DailyStats(LocalDate.of(2024, 1, 2), 50, 50, 5, 10, 10));
        repo.saveDailyStats(user.id(), new DailyStats(LocalDate.of(2024, 1, 3), 20, 20, 2, 3, 3));

        int totalStars = repo.getTotalStars(user.id());

        assertThat(totalStars).isEqualTo(10);
    }

    @Test
    void getTotalStars_noStats_returnsZero() {
        User user = repo.createUser("Alice");

        int totalStars = repo.getTotalStars(user.id());

        assertThat(totalStars).isEqualTo(0);
    }

    // --- Attempt history tests ---

    @Test
    void recordAttempt_storesInHistory() {
        User user = repo.createUser("Alice");
        Instant now = Instant.now();

        repo.recordAttempt(user.id(), Operation.ADDITION, 3, 5, true, now);
        repo.recordAttempt(user.id(), Operation.ADDITION, 2, 4, false, now);

        // Verify by querying directly
        List<Integer> correct = setup.jdbi().withHandle(handle ->
            handle.createQuery("SELECT correct FROM attempts WHERE user_id = :userId ORDER BY id")
                .bind("userId", user.id())
                .mapTo(Integer.class)
                .list()
        );

        assertThat(correct).containsExactly(1, 0);
    }

    // --- Isolation tests ---

    @Test
    void differentUsers_haveIsolatedData() {
        User alice = repo.createUser("Alice");
        User bob = repo.createUser("Bob");

        // Save stats for Alice
        repo.saveProblemStats(alice.id(), ProblemStats.newStats(Operation.ADDITION, 1, 1));
        repo.saveDailyStats(alice.id(), new DailyStats(LocalDate.now(), 10, 10, 1, 5, 5));

        // Bob should have empty data
        assertThat(repo.getAllProblemStats(bob.id())).isEmpty();
        assertThat(repo.getDailyStats(bob.id(), LocalDate.now())).isEmpty();
        assertThat(repo.getTotalStars(bob.id())).isEqualTo(0);
    }
}
