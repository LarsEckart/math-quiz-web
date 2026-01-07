package mathquiz.service;

import mathquiz.domain.*;
import mathquiz.storage.DatabaseSetup;
import mathquiz.storage.SqliteRepository;
import org.junit.jupiter.api.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for QuizService.
 * Uses SqliteRepository with in-memory database.
 */
class QuizServiceTest {

    private DatabaseSetup.SetupResult setup;
    private SqliteRepository repo;
    private Clock clock;
    private int userId;

    @BeforeEach
    void setUp() {
        setup = DatabaseSetup.setupInMemory();
        repo = new SqliteRepository(setup.jdbi());
        clock = Clock.fixed(Instant.parse("2024-06-15T10:00:00Z"), ZoneId.of("UTC"));
        userId = repo.createUser("TestUser").id();
    }

    @AfterEach
    void tearDown() {
        setup.close();
    }

    private QuizService createService() {
        return new QuizService(repo, userId, clock);
    }

    private QuizService createServiceWithFixedRandom(long seed) {
        return new QuizService(repo, userId, clock, new Random(seed));
    }

    // --- Basic problem generation ---

    @Test
    void getNextProblem_returnsProblem() {
        QuizService service = createService();

        Problem problem = service.getNextProblem(null);

        assertThat(problem).isNotNull();
        assertThat(problem.operation()).isEqualTo(Operation.ADDITION); // Only addition unlocked
    }

    @Test
    void getNextProblem_withSpecificOperation_returnsThatOperation() {
        QuizService service = createService();
        service.unlockOperation(Operation.MULTIPLICATION);

        Problem problem = service.getNextProblem(Operation.MULTIPLICATION);

        assertThat(problem.operation()).isEqualTo(Operation.MULTIPLICATION);
    }

    // --- Answer submission ---

    @Test
    void submitAnswer_correct_returnsCorrectResult() {
        QuizService service = createService();
        Problem problem = service.getNextProblem(null);

        AnswerResult result = service.submitAnswer(problem.answer());

        assertThat(result.correct()).isTrue();
        assertThat(result.correctAnswer()).isEqualTo(problem.answer());
        assertThat(result.streak()).isEqualTo(1);
    }

    @Test
    void submitAnswer_incorrect_returnsIncorrectResult() {
        QuizService service = createService();
        Problem problem = service.getNextProblem(null);
        int wrongAnswer = problem.answer() + 100;

        AnswerResult result = service.submitAnswer(wrongAnswer);

        assertThat(result.correct()).isFalse();
        assertThat(result.correctAnswer()).isEqualTo(problem.answer());
        assertThat(result.streak()).isEqualTo(0);
    }

    @Test
    void submitAnswer_withoutProblem_throwsException() {
        QuizService service = createService();

        assertThatThrownBy(() -> service.submitAnswer(42))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No current problem");
    }

    // --- Streak tracking ---

    @Test
    void streak_buildsWithCorrectAnswers() {
        QuizService service = createService();

        for (int i = 1; i <= 5; i++) {
            Problem problem = service.getNextProblem(null);
            AnswerResult result = service.submitAnswer(problem.answer());
            assertThat(result.streak()).isEqualTo(i);
        }
    }

    @Test
    void streak_resetsOnWrongAnswer() {
        QuizService service = createService();

        // Build streak of 3
        for (int i = 0; i < 3; i++) {
            Problem problem = service.getNextProblem(null);
            service.submitAnswer(problem.answer());
        }

        // Wrong answer
        Problem problem = service.getNextProblem(null);
        AnswerResult result = service.submitAnswer(problem.answer() + 100);
        assertThat(result.streak()).isEqualTo(0);

        // Correct again
        problem = service.getNextProblem(null);
        result = service.submitAnswer(problem.answer());
        assertThat(result.streak()).isEqualTo(1);
    }

    // --- Operation unlocking ---

    @Test
    void onlyAdditionUnlocked_initially() {
        QuizService service = createService();

        assertThat(service.getUnlockedOperations())
            .containsExactly(Operation.ADDITION);
    }

    @Test
    void unlockOperation_unlocksNewOperation() {
        QuizService service = createService();

        boolean result = service.unlockOperation(Operation.MULTIPLICATION);

        assertThat(result).isTrue();
        assertThat(service.getUnlockedOperations())
            .containsExactlyInAnyOrder(Operation.ADDITION, Operation.MULTIPLICATION);
    }

    @Test
    void unlockOperation_alreadyUnlocked_returnsFalse() {
        QuizService service = createService();

        boolean result = service.unlockOperation(Operation.ADDITION);

        assertThat(result).isFalse();
    }

    @Test
    void unlockOperation_persistedToRepository() {
        QuizService service = createService();
        service.unlockOperation(Operation.SUBTRACTION);

        // Create new service to verify persistence
        QuizService service2 = createService();

        assertThat(service2.getUnlockedOperations())
            .containsExactlyInAnyOrder(Operation.ADDITION, Operation.SUBTRACTION);
    }

    // --- Session stats ---

    @Test
    void sessionStats_trackedCorrectly() {
        QuizService service = createService();

        Problem problem = service.getNextProblem(null);
        service.submitAnswer(problem.answer());

        assertThat(service.sessionStats().problemsSolved()).isEqualTo(1);
        assertThat(service.sessionStats().problemsCorrect()).isEqualTo(1);
    }

    // --- Daily stats ---

    @Test
    void dailyStats_persistedToRepository() {
        QuizService service = createService();
        LocalDate today = LocalDate.now(clock);

        Problem problem = service.getNextProblem(null);
        service.submitAnswer(problem.answer());

        DailyStats persisted = repo.getDailyStats(userId, today).orElseThrow();
        assertThat(persisted.problemsSolved()).isEqualTo(1);
        assertThat(persisted.problemsCorrect()).isEqualTo(1);
    }

    // --- Spaced repetition integration ---

    @Test
    void wrongAnswer_problemHasShortInterval() {
        QuizService service = createService();

        Problem problem = service.getNextProblem(null);
        int op1 = problem.operand1();
        int op2 = problem.operand2();
        Operation op = problem.operation();

        service.submitAnswer(problem.answer() + 100); // Wrong

        ProblemStats stats = repo.getProblemStats(userId, op, op1, op2).orElseThrow();
        assertThat(stats.intervalDays()).isEqualTo(0.0); // Reset
        assertThat(stats.repetitions()).isEqualTo(0);    // Reset
    }

    @Test
    void correctAnswer_savesStatsToRepository() {
        QuizService service = createService();

        Problem problem = service.getNextProblem(null);
        service.submitAnswer(problem.answer());

        ProblemStats stats = repo.getProblemStats(
            userId, problem.operation(), problem.operand1(), problem.operand2()
        ).orElseThrow();

        assertThat(stats.totalAttempts()).isEqualTo(1);
        assertThat(stats.totalCorrect()).isEqualTo(1);
    }

    @Test
    void dueProblem_returnedBeforeNewProblem() {
        // First, answer a problem wrong to make it due soon
        QuizService service1 = createServiceWithFixedRandom(42);

        Problem first = service1.getNextProblem(null);
        int op1 = first.operand1();
        int op2 = first.operand2();
        service1.submitAnswer(first.answer() + 100); // Wrong - will be due in 1 minute

        // Advance clock past the due time
        Clock futureClock = Clock.fixed(
            Instant.parse("2024-06-15T10:02:00Z"), // 2 minutes later
            ZoneId.of("UTC")
        );

        QuizService service2 = new QuizService(repo, userId, futureClock, new Random(999));

        // Should get the due problem, not a random new one
        Problem next = service2.getNextProblem(null);

        assertThat(next.operand1()).isEqualTo(op1);
        assertThat(next.operand2()).isEqualTo(op2);
    }

    // --- Total stars ---

    @Test
    void getTotalStars_sumsAcrossDays() {
        // Manually insert daily stats
        repo.saveDailyStats(userId, new DailyStats(LocalDate.of(2024, 6, 14), 30, 30, 3, 5, 5));
        repo.saveDailyStats(userId, new DailyStats(LocalDate.of(2024, 6, 13), 20, 20, 2, 3, 3));

        QuizService service = createService();

        assertThat(service.getTotalStars()).isEqualTo(5);
    }

    // --- Attempt history ---

    @Test
    void submitAnswer_recordsAttemptInHistory() {
        QuizService service = createService();

        Problem problem = service.getNextProblem(null);
        service.submitAnswer(problem.answer());

        // Verify by querying attempts table
        int count = setup.jdbi().withHandle(handle ->
            handle.createQuery("SELECT COUNT(*) FROM attempts WHERE user_id = :userId")
                .bind("userId", userId)
                .mapTo(Integer.class)
                .one()
        );

        assertThat(count).isEqualTo(1);
    }

    // --- Streak persistence across restarts ---

    @Test
    void streak_persistsAcrossServiceRestarts() {
        QuizService service1 = createService();

        // Build streak of 3
        for (int i = 0; i < 3; i++) {
            Problem problem = service1.getNextProblem(null);
            service1.submitAnswer(problem.answer());
        }

        // Create new service (simulating restart)
        QuizService service2 = createService();

        // Continue streak
        Problem problem = service2.getNextProblem(null);
        AnswerResult result = service2.submitAnswer(problem.answer());

        assertThat(result.streak()).isEqualTo(4);
    }
}
