package mathquiz.storage;

import mathquiz.domain.*;
import org.jdbi.v3.core.Jdbi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SQLite-based repository implementation using Jdbi.
 */
public class SqliteRepository implements Repository {

    private final Jdbi jdbi;

    public SqliteRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    // --- User management ---

    @Override
    public List<User> getUsers() {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT id, name, created_at FROM users ORDER BY name")
                .map((rs, ctx) -> new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    Instant.parse(rs.getString("created_at"))
                ))
                .list()
        );
    }

    @Override
    public User createUser(String name) {
        Instant now = Instant.now();
        return jdbi.withHandle(handle -> {
            handle.createUpdate("INSERT INTO users (name, created_at) VALUES (:name, :createdAt)")
                .bind("name", name)
                .bind("createdAt", now.toString())
                .execute();

            int id = handle.createQuery("SELECT last_insert_rowid()")
                .mapTo(Integer.class)
                .one();

            return new User(id, name, now);
        });
    }

    @Override
    public Optional<User> getUser(int userId) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT id, name, created_at FROM users WHERE id = :id")
                .bind("id", userId)
                .map((rs, ctx) -> new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    Instant.parse(rs.getString("created_at"))
                ))
                .findFirst()
        );
    }

    // --- Problem stats ---

    @Override
    public Optional<ProblemStats> getProblemStats(int userId, Operation operation, int operand1, int operand2) {
        return jdbi.withHandle(handle ->
            handle.createQuery("""
                SELECT operation, operand1, operand2, ease_factor, interval_days,
                       next_review_ts, repetitions, total_attempts, total_correct
                FROM problem_stats
                WHERE user_id = :userId AND operation = :operation
                  AND operand1 = :operand1 AND operand2 = :operand2
                """)
                .bind("userId", userId)
                .bind("operation", operation.name())
                .bind("operand1", operand1)
                .bind("operand2", operand2)
                .map((rs, ctx) -> mapToProblemStats(rs))
                .findFirst()
        );
    }

    @Override
    public List<ProblemStats> getAllProblemStats(int userId) {
        return jdbi.withHandle(handle ->
            handle.createQuery("""
                SELECT operation, operand1, operand2, ease_factor, interval_days,
                       next_review_ts, repetitions, total_attempts, total_correct
                FROM problem_stats
                WHERE user_id = :userId
                """)
                .bind("userId", userId)
                .map((rs, ctx) -> mapToProblemStats(rs))
                .list()
        );
    }

    @Override
    public List<ProblemStats> getDueProblems(int userId, Instant now, int limit) {
        long nowEpoch = now.getEpochSecond();
        return jdbi.withHandle(handle ->
            handle.createQuery("""
                SELECT operation, operand1, operand2, ease_factor, interval_days,
                       next_review_ts, repetitions, total_attempts, total_correct
                FROM problem_stats
                WHERE user_id = :userId
                  AND (next_review_ts IS NULL OR next_review_ts <= :now)
                ORDER BY next_review_ts NULLS FIRST
                LIMIT :limit
                """)
                .bind("userId", userId)
                .bind("now", nowEpoch)
                .bind("limit", limit)
                .map((rs, ctx) -> mapToProblemStats(rs))
                .list()
        );
    }

    @Override
    public void saveProblemStats(int userId, ProblemStats stats) {
        Long nextReviewTs = stats.nextReview() != null ? stats.nextReview().getEpochSecond() : null;

        jdbi.useHandle(handle ->
            handle.createUpdate("""
                INSERT INTO problem_stats (user_id, operation, operand1, operand2, ease_factor,
                                           interval_days, next_review_ts, repetitions,
                                           total_attempts, total_correct)
                VALUES (:userId, :operation, :operand1, :operand2, :easeFactor,
                        :intervalDays, :nextReviewTs, :repetitions,
                        :totalAttempts, :totalCorrect)
                ON CONFLICT(user_id, operation, operand1, operand2) DO UPDATE SET
                    ease_factor = :easeFactor,
                    interval_days = :intervalDays,
                    next_review_ts = :nextReviewTs,
                    repetitions = :repetitions,
                    total_attempts = :totalAttempts,
                    total_correct = :totalCorrect
                """)
                .bind("userId", userId)
                .bind("operation", stats.operation().name())
                .bind("operand1", stats.operand1())
                .bind("operand2", stats.operand2())
                .bind("easeFactor", stats.easeFactor())
                .bind("intervalDays", stats.intervalDays())
                .bind("nextReviewTs", nextReviewTs)
                .bind("repetitions", stats.repetitions())
                .bind("totalAttempts", stats.totalAttempts())
                .bind("totalCorrect", stats.totalCorrect())
                .execute()
        );
    }

    private ProblemStats mapToProblemStats(java.sql.ResultSet rs) throws java.sql.SQLException {
        Long nextReviewTs = rs.getObject("next_review_ts") != null ? rs.getLong("next_review_ts") : null;
        Instant nextReview = nextReviewTs != null ? Instant.ofEpochSecond(nextReviewTs) : null;

        return new ProblemStats(
            Operation.valueOf(rs.getString("operation")),
            rs.getInt("operand1"),
            rs.getInt("operand2"),
            rs.getDouble("ease_factor"),
            rs.getDouble("interval_days"),
            nextReview,
            rs.getInt("repetitions"),
            rs.getInt("total_attempts"),
            rs.getInt("total_correct")
        );
    }

    // --- Difficulty progression ---

    @Override
    public DifficultyManager getDifficulty(int userId) {
        List<OperationProgress> progressList = jdbi.withHandle(handle ->
            handle.createQuery("""
                SELECT operation, max_number, unlocked, manually_unlocked,
                       problems_at_current_range, correct_at_current_range
                FROM operation_progress
                WHERE user_id = :userId
                """)
                .bind("userId", userId)
                .map((rs, ctx) -> new OperationProgress(
                    Operation.valueOf(rs.getString("operation")),
                    rs.getInt("max_number"),
                    rs.getInt("unlocked") == 1,
                    rs.getInt("manually_unlocked") == 1,
                    rs.getInt("problems_at_current_range"),
                    rs.getInt("correct_at_current_range")
                ))
                .list()
        );

        if (progressList.isEmpty()) {
            return new DifficultyManager();
        }

        Map<Operation, OperationProgress> progressMap = new EnumMap<>(Operation.class);
        for (OperationProgress p : progressList) {
            progressMap.put(p.operation(), p);
        }

        // Fill in any missing operations with defaults
        for (Operation op : Operation.values()) {
            if (!progressMap.containsKey(op)) {
                OperationProgress defaultProgress = new OperationProgress(op);
                if (op == Operation.ADDITION) {
                    defaultProgress.unlock();
                }
                progressMap.put(op, defaultProgress);
            }
        }

        return new DifficultyManager(progressMap);
    }

    @Override
    public void saveDifficulty(int userId, DifficultyManager difficulty) {
        jdbi.useHandle(handle -> {
            for (Operation op : Operation.values()) {
                OperationProgress p = difficulty.getProgress(op);
                handle.createUpdate("""
                    INSERT INTO operation_progress (user_id, operation, max_number, unlocked,
                                                    manually_unlocked, problems_at_current_range,
                                                    correct_at_current_range)
                    VALUES (:userId, :operation, :maxNumber, :unlocked, :manuallyUnlocked,
                            :problemsAtCurrentRange, :correctAtCurrentRange)
                    ON CONFLICT(user_id, operation) DO UPDATE SET
                        max_number = :maxNumber,
                        unlocked = :unlocked,
                        manually_unlocked = :manuallyUnlocked,
                        problems_at_current_range = :problemsAtCurrentRange,
                        correct_at_current_range = :correctAtCurrentRange
                    """)
                    .bind("userId", userId)
                    .bind("operation", op.name())
                    .bind("maxNumber", p.maxNumber())
                    .bind("unlocked", p.isUnlocked() ? 1 : 0)
                    .bind("manuallyUnlocked", p.isManuallyUnlocked() ? 1 : 0)
                    .bind("problemsAtCurrentRange", p.problemsAtCurrentRange())
                    .bind("correctAtCurrentRange", p.correctAtCurrentRange())
                    .execute();
            }
        });
    }

    // --- Daily stats ---

    @Override
    public Optional<DailyStats> getDailyStats(int userId, LocalDate day) {
        String dayStr = day.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return jdbi.withHandle(handle ->
            handle.createQuery("""
                SELECT day, problems_solved, problems_correct, stars_earned, best_streak, current_streak
                FROM daily_stats
                WHERE user_id = :userId AND day = :day
                """)
                .bind("userId", userId)
                .bind("day", dayStr)
                .map((rs, ctx) -> new DailyStats(
                    LocalDate.parse(rs.getString("day")),
                    rs.getInt("problems_solved"),
                    rs.getInt("problems_correct"),
                    rs.getInt("stars_earned"),
                    rs.getInt("best_streak"),
                    rs.getInt("current_streak")
                ))
                .findFirst()
        );
    }

    @Override
    public void saveDailyStats(int userId, DailyStats stats) {
        String dayStr = stats.date().format(DateTimeFormatter.ISO_LOCAL_DATE);
        jdbi.useHandle(handle ->
            handle.createUpdate("""
                INSERT INTO daily_stats (user_id, day, problems_solved, problems_correct,
                                         stars_earned, best_streak, current_streak)
                VALUES (:userId, :day, :problemsSolved, :problemsCorrect,
                        :starsEarned, :bestStreak, :currentStreak)
                ON CONFLICT(user_id, day) DO UPDATE SET
                    problems_solved = :problemsSolved,
                    problems_correct = :problemsCorrect,
                    stars_earned = :starsEarned,
                    best_streak = :bestStreak,
                    current_streak = :currentStreak
                """)
                .bind("userId", userId)
                .bind("day", dayStr)
                .bind("problemsSolved", stats.problemsSolved())
                .bind("problemsCorrect", stats.problemsCorrect())
                .bind("starsEarned", stats.starsEarned())
                .bind("bestStreak", stats.bestStreak())
                .bind("currentStreak", stats.currentStreak())
                .execute()
        );
    }

    @Override
    public int getTotalStars(int userId) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT COALESCE(SUM(stars_earned), 0) FROM daily_stats WHERE user_id = :userId")
                .bind("userId", userId)
                .mapTo(Integer.class)
                .one()
        );
    }

    // --- Problem history ---

    @Override
    public void recordAttempt(int userId, Operation operation, int operand1, int operand2, boolean correct, Instant timestamp) {
        jdbi.useHandle(handle ->
            handle.createUpdate("""
                INSERT INTO attempts (user_id, ts, operation, operand1, operand2, correct)
                VALUES (:userId, :ts, :operation, :operand1, :operand2, :correct)
                """)
                .bind("userId", userId)
                .bind("ts", timestamp.getEpochSecond())
                .bind("operation", operation.name())
                .bind("operand1", operand1)
                .bind("operand2", operand2)
                .bind("correct", correct ? 1 : 0)
                .execute()
        );
    }
}
