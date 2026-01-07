package mathquiz.service;

import mathquiz.domain.*;
import mathquiz.storage.Repository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Main quiz orchestration - generates problems, processes answers.
 */
public class QuizService {

    private final Repository repo;
    private final int userId;
    private final Clock clock;
    private final Random random;

    private DifficultyManager difficulty;
    private DailyStats dailyStats;
    private SessionStats sessionStats;
    private Problem currentProblem;

    public QuizService(Repository repo, int userId, Clock clock) {
        this(repo, userId, clock, new Random());
    }

    public QuizService(Repository repo, int userId, Clock clock, Random random) {
        this.repo = repo;
        this.userId = userId;
        this.clock = clock;
        this.random = random;

        // Load or create difficulty manager
        this.difficulty = repo.getDifficulty(userId);

        // Load or create today's stats
        LocalDate today = LocalDate.now(clock);
        this.dailyStats = repo.getDailyStats(userId, today)
                .orElse(new DailyStats(today));

        // Initialize session stats from daily stats to preserve streak across restarts
        this.sessionStats = new SessionStats(
                dailyStats.currentStreak(),
                dailyStats.bestStreak(),
                0, 0
        );
    }

    public SessionStats sessionStats() {
        return sessionStats;
    }

    public DailyStats dailyStats() {
        return dailyStats;
    }

    public DifficultyManager difficulty() {
        return difficulty;
    }

    public Problem currentProblem() {
        return currentProblem;
    }

    /**
     * Get currently unlocked operations.
     */
    public List<Operation> getUnlockedOperations() {
        return difficulty.getUnlockedOperations();
    }

    /**
     * Manually unlock an operation.
     * @return true if operation was newly unlocked
     */
    public boolean unlockOperation(Operation operation) {
        boolean result = difficulty.unlockOperation(operation);
        if (result) {
            repo.saveDifficulty(userId, difficulty);
        }
        return result;
    }

    /**
     * Get the next problem to present.
     *
     * If operation is null, picks randomly from unlocked operations.
     * Uses spaced repetition to prioritize due problems.
     *
     * @param operation Specific operation, or null for random from unlocked
     * @return The next problem
     */
    public Problem getNextProblem(Operation operation) {
        // Check for due problems first (spaced repetition)
        List<ProblemStats> dueProblems = repo.getDueProblems(userId, clock.instant(), 10);
        Set<Operation> unlocked = new HashSet<>(getUnlockedOperations());

        // Filter to only unlocked operations
        List<ProblemStats> dueUnlocked = dueProblems.stream()
                .filter(p -> unlocked.contains(p.operation()))
                .toList();

        if (!dueUnlocked.isEmpty()) {
            // Review a due problem (pick the most overdue one - first in list)
            ProblemStats stats = dueUnlocked.get(0);
            currentProblem = new Problem(stats.operand1(), stats.operand2(), stats.operation());
            return currentProblem;
        }

        // No due problems - generate a new one
        if (operation == null) {
            // Pick randomly from unlocked operations
            List<Operation> unlockedList = getUnlockedOperations();
            if (unlockedList.isEmpty()) {
                operation = Operation.ADDITION; // Fallback
            } else {
                operation = unlockedList.get(random.nextInt(unlockedList.size()));
            }
        }

        // Generate a new problem within the current range
        int[] range = difficulty.getRange(operation);
        int maxNumber = range[1];

        ProblemPool pool = ProblemPool.forOperation(operation, maxNumber);
        currentProblem = pool.pickRandom(random);

        return currentProblem;
    }

    /**
     * Submit an answer for the current problem.
     *
     * @param answer The user's answer
     * @return Result with feedback information
     * @throws IllegalStateException if no current problem
     */
    public AnswerResult submitAnswer(int answer) {
        if (currentProblem == null) {
            throw new IllegalStateException("No current problem - call getNextProblem first");
        }

        Problem problem = currentProblem;
        boolean correct = problem.check(answer);

        // Update session stats
        sessionStats.recordAnswer(correct);

        // Update spaced repetition stats
        ProblemStats existingStats = repo.getProblemStats(
                userId, problem.operation(), problem.operand1(), problem.operand2()
        ).orElse(ProblemStats.newStats(problem.operation(), problem.operand1(), problem.operand2()));

        ProblemStats newStats = SpacedRepetition.updateStats(existingStats, correct, clock);
        repo.saveProblemStats(userId, newStats);

        // Update difficulty and check for progression
        Set<Operation> prevUnlocked = new HashSet<>(getUnlockedOperations());
        boolean rangeExpanded = difficulty.recordAttempt(problem.operation(), correct);
        Set<Operation> newUnlocked = new HashSet<>(getUnlockedOperations());

        Operation newOperationUnlocked = null;
        newUnlocked.removeAll(prevUnlocked);
        if (!newUnlocked.isEmpty()) {
            newOperationUnlocked = newUnlocked.iterator().next();
        }

        repo.saveDifficulty(userId, difficulty);

        // Update daily stats
        int prevCorrect = dailyStats.problemsCorrect();
        dailyStats.recordAnswer(correct);

        // Calculate new stars
        int newStars = DailyStats.calculateNewStars(prevCorrect, dailyStats.problemsCorrect());

        repo.saveDailyStats(userId, dailyStats);

        // Record in history
        repo.recordAttempt(userId, problem.operation(), problem.operand1(), problem.operand2(),
                correct, clock.instant());

        // Clear current problem
        currentProblem = null;

        return new AnswerResult(
                correct,
                problem.answer(),
                sessionStats.currentStreak(),
                newStars,
                rangeExpanded,
                newOperationUnlocked
        );
    }

    /**
     * Get total stars earned across all days.
     */
    public int getTotalStars() {
        return repo.getTotalStars(userId);
    }
}
