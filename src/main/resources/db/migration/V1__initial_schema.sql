-- Users table
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL
);

-- Spaced repetition stats per problem
CREATE TABLE problem_stats (
    user_id INTEGER NOT NULL,
    operation TEXT NOT NULL,
    operand1 INTEGER NOT NULL,
    operand2 INTEGER NOT NULL,
    ease_factor REAL NOT NULL,
    interval_days REAL NOT NULL,
    next_review_ts INTEGER,
    repetitions INTEGER NOT NULL,
    total_attempts INTEGER NOT NULL,
    total_correct INTEGER NOT NULL,
    PRIMARY KEY (user_id, operation, operand1, operand2)
);
CREATE INDEX idx_problem_stats_due ON problem_stats(user_id, next_review_ts);

-- Progress per operation (range, unlock status)
CREATE TABLE operation_progress (
    user_id INTEGER NOT NULL,
    operation TEXT NOT NULL,
    max_number INTEGER NOT NULL,
    unlocked INTEGER NOT NULL,
    manually_unlocked INTEGER NOT NULL,
    problems_at_current_range INTEGER NOT NULL,
    correct_at_current_range INTEGER NOT NULL,
    PRIMARY KEY (user_id, operation)
);

-- Daily stats (stars, streaks)
CREATE TABLE daily_stats (
    user_id INTEGER NOT NULL,
    day TEXT NOT NULL,
    problems_solved INTEGER NOT NULL,
    problems_correct INTEGER NOT NULL,
    stars_earned INTEGER NOT NULL,
    best_streak INTEGER NOT NULL,
    current_streak INTEGER NOT NULL,
    PRIMARY KEY (user_id, day)
);

-- Attempt history (for analytics)
CREATE TABLE attempts (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    ts INTEGER NOT NULL,
    operation TEXT NOT NULL,
    operand1 INTEGER NOT NULL,
    operand2 INTEGER NOT NULL,
    correct INTEGER NOT NULL
);
CREATE INDEX idx_attempts_user_ts ON attempts(user_id, ts);
