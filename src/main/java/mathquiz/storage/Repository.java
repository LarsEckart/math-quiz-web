package mathquiz.storage;

import mathquiz.domain.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Abstract repository interface for data persistence.
 */
public interface Repository {

    // --- User management ---

    /**
     * Get all users.
     */
    List<User> getUsers();

    /**
     * Create a new user.
     * @return the created user with assigned ID
     */
    User createUser(String name);

    /**
     * Get user by ID.
     */
    Optional<User> getUser(int userId);

    // --- Problem stats (spaced repetition) ---

    /**
     * Get stats for a specific problem, or empty if never attempted.
     */
    Optional<ProblemStats> getProblemStats(int userId, Operation operation, int operand1, int operand2);

    /**
     * Get all problem stats for a user.
     */
    List<ProblemStats> getAllProblemStats(int userId);

    /**
     * Get problems that are due for review, ordered by priority.
     */
    List<ProblemStats> getDueProblems(int userId, Instant now, int limit);

    /**
     * Save or update problem stats.
     */
    void saveProblemStats(int userId, ProblemStats stats);

    // --- Difficulty progression ---

    /**
     * Get difficulty manager state for user.
     * Returns a new default DifficultyManager if user has no saved progress.
     */
    DifficultyManager getDifficulty(int userId);

    /**
     * Save difficulty manager state.
     */
    void saveDifficulty(int userId, DifficultyManager difficulty);

    // --- Daily stats ---

    /**
     * Get stats for a specific day.
     */
    Optional<DailyStats> getDailyStats(int userId, LocalDate day);

    /**
     * Save or update daily stats.
     */
    void saveDailyStats(int userId, DailyStats stats);

    /**
     * Get total stars earned by user across all days.
     */
    int getTotalStars(int userId);

    // --- Problem history ---

    /**
     * Record a problem attempt in history.
     */
    void recordAttempt(int userId, Operation operation, int operand1, int operand2, boolean correct, Instant timestamp);
}
