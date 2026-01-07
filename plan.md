# Math Quiz Web — Implementation Plan

> Web-based math quiz for kids, ported from Python terminal app.  
> Stack: Java 21 + Javalin + SQLite + HTMX + JTE templates  
> Hosting: Oracle Cloud Always Free VM
> Port of my python app, /Users/lars/GitHub/terminal-math-quiz

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Browser (HTMX)                              │
│  • Big colorful numbers, immediate feedback                         │
│  • Minimal JS: audio playback + 'r' to repeat                       │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ HTTP (HTML fragments)
┌───────────────────────────────▼─────────────────────────────────────┐
│                      Javalin Web Layer                              │
│  Routes: /players, /quiz, /quiz/problem, /quiz/answer, /audio/{hash}│
│  Templates: JTE (server-rendered)                                   │
│  Session: stores userId + currentProblem                            │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────┐
│                       Service Layer                                 │
│  QuizService: orchestrates problem generation, answer submission    │
│  TtsCacheService: content-addressed audio caching                   │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────┐
│                       Domain Layer (pure Java)                      │
│  Problem, Operation, SpacedRepetition, DifficultyManager, Stats     │
│  No I/O — fully unit testable                                       │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────┐
│                      Storage Layer                                  │
│  Repository interface + SqliteRepository (Jdbi/JDBC)                │
│  Flyway migrations                                                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Template engine | **JTE** | Fast, type-safe, great for HTMX fragments |
| DB access | **jooq** | Less boilerplate than JDBC, still lightweight |
| Migrations | **Flyway** | Standard, simple SQL files |
| Session storage | **HTTP session** | Simple; stores userId + currentProblem |
| Audio caching | **Content-addressed** | `sha256(speaker\|text).wav` — no DB lookup needed |
| Hosting | **Oracle Cloud Always Free** | Persistent disk, no cold starts, 200GB storage |

---

## Milestones

### Milestone 1: Project Skeleton ✅
**Goal:** Compiles, runs, deploys "Hello"

**Build:**
- Gradle project (Java 21)
- Dependencies: Javalin, JTE, sqlite-jdbc, Jdbi, HikariCP, Flyway, Logback, JUnit 5
- Basic app: `GET /health` → `"ok"`, `GET /` → rendered template

**Package layout:**
```
src/main/java/
  mathquiz/
    App.java
    web/
      Routes.java
      handlers/
      middleware/
    domain/
    service/
    storage/
    tts/
src/main/resources/
  jte/              (templates)
  public/           (CSS, JS)
  db/migration/     (Flyway SQL)
```

**Test:** Smoke test `/health`  
**Deliverable:** Runnable jar serving health endpoint

---

### Milestone 2: Port Domain Layer
**Goal:** Pure Java domain logic, unit-tested

**Port from Python:**
- `Operation` enum (+, -, ×, ÷)
- `Problem` record: `operand1`, `operand2`, `operation`, `answer()`, `check(int)`
- `ProblemPool`: pre-generates all valid problems for an operation + range
- `ProblemStats`: ease_factor, interval, next_review, repetitions, totals
- `SpacedRepetition.updateStats(stats, correct, clock)`
- `DifficultyManager` + `OperationProgress`: thresholds, range expansion, unlock logic
- `DailyStats`, `SessionStats`, `calculateNewStars(...)`

**Problem Generation — changed from Python:**

The Python app generates random operands on-the-fly. We'll instead pre-generate all valid combinations and pick randomly from the pool. This ensures uniform distribution and simpler logic.

```
ProblemPool.forAddition(maxSum=10):
  → generates: 1+1, 1+2, ..., 1+9, 2+1, 2+2, ..., 5+5
  → all pairs where a + b <= maxSum and a,b >= 1

ProblemPool.forSubtraction(maxMinuend=10):
  → generates: 2-1, 3-1, 3-2, 4-1, ..., 10-9
  → all pairs where a >= b and a <= maxMinuend and b >= 1

ProblemPool.forMultiplication(maxFactor=5):
  → generates: 1×1, 1×2, ..., 5×5
  → all pairs where a,b <= maxFactor

ProblemPool.forDivision(maxFactor=5):
  → generates: 1÷1, 2÷1, 2÷2, 3÷1, ..., 25÷5
  → all pairs (a,b) where a = b × quotient, quotient <= maxFactor, b <= maxFactor
```

**Usage:**
```java
var pool = ProblemPool.forAddition(10);
var problem = pool.pickRandom(random);  // uniform distribution
```

**Test (JUnit 5):**
- ProblemPool generates correct number of combinations
- All generated problems satisfy constraints (sum bounds, non-negative results, exact division)
- SpacedRepetition state transitions
- DifficultyManager expansion and unlock triggers

**Deliverable:** Green tests, domain ready for services

---

### Milestone 3: SQLite Schema + Repository
**Goal:** Persistence layer, integration-tested

**Schema (Flyway migrations):**
```sql
-- V1__initial_schema.sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  created_at TEXT NOT NULL
);

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
```

**Repository interface:** Mirror Python's `Repository` protocol  
**SqliteRepository:** Implement with Jdbi

**Test:** Round-trip tests with temp SQLite file  
**Deliverable:** Working persistence with migrations

---

### Milestone 4: Port QuizService
**Goal:** Service orchestration, service-layer tests

**QuizService:**
- Constructor: `(Repository repo, int userId, Clock clock)`
- `getNextProblem(Operation?)` — checks due problems first, else generates new
- `submitAnswer(int)` → `AnswerResult` (correct, streak, stars, range_expanded, new_unlock)
- `getUnlockedOperations()`, `getTotalStars()`

**Web adaptation:** Store `currentProblem` in HTTP session

**Test:** Service tests with SqliteRepository + temp DB  
**Deliverable:** Quiz orchestration proven via tests

---

### Milestone 5: Web UI v1 (Multi-user + Basic Quiz Loop)
**Goal:** Playable quiz, no audio yet

**Routes:**
```
GET  /              → redirect to /players
GET  /players       → player list + create form
POST /players       → create player
POST /players/{id}/select → set session, redirect to /quiz

GET  /quiz          → quiz shell (loads problem via HTMX)
GET  /quiz/problem  → problem fragment
POST /quiz/answer   → feedback fragment + OOB updates
```

**HTMX flow:**
1. `/quiz` shell loads with `hx-get="/quiz/problem" hx-trigger="load"`
2. Problem fragment has form: `hx-post="/quiz/answer" hx-target="#problem-area"`
3. Answer response includes feedback + auto-advance via `hx-trigger="load delay:800ms"`

**Templates (JTE):**
- `players.jte` — list users, create form
- `quiz.jte` — shell with problem-area div, streak/stars header
- `fragments/problem.jte` — big numbers + input
- `fragments/feedback.jte` — correct/incorrect + OOB counter updates

**Test:** Integration tests for routes  
**Deliverable:** Working multi-user quiz loop

---

### Milestone 6: Kid-Friendly UI
**Goal:** Big colorful numbers, animations, streak/stars display

**CSS (`public/app.css`):**
- Large font with `clamp()` for responsive sizing
- Color-coded operations (green=add, blue=sub, purple=mul, orange=div)
- Feedback animations (shake for wrong, bounce for correct)

**HTMX OOB updates for counters:**
```html
<div id="streak" hx-swap-oob="true">Streak: 5</div>
<div id="stars" hx-swap-oob="true">⭐ 3 / 10</div>
```

**Auto-advance after feedback:**
```html
<div hx-get="/quiz/problem"
     hx-trigger="load delay:800ms"
     hx-target="#problem-area"
     hx-swap="innerHTML"></div>
```

**Test:** Manual testing of animations and counters  
**Deliverable:** Polished, kid-friendly experience

---

### Milestone 7: Estonian TTS + Audio Caching
**Goal:** Voice reads problems, cached server-side, 'r' to repeat

**EstonianSpeechFormatter:**
- Operation words: pluss, miinus, korda, jagatud
- Numbers 1-50 in Estonian (expand later)

**TtsCacheService:**
- Content-addressed: `hash = sha256("neurokone|liivika|" + text)`
- Store: `data/tts/{hash}.wav`
- If cached: return hash. Else: call Neurokõne API, store atomically.

**Route:** `GET /audio/{hash}.wav` — streams cached file with long cache headers

**Problem fragment includes:**
```html
<audio id="tts-audio" src="/audio/{hash}.wav" autoplay></audio>
```

**Minimal JS (`public/app.js`):**
```javascript
document.addEventListener('keydown', (e) => {
  if (e.key === 'r' || e.key === 'R') {
    const audio = document.getElementById('tts-audio');
    if (audio) { audio.currentTime = 0; audio.play(); }
  }
});
```

**Test:** Cache hit/miss verification, audio playback  
**Deliverable:** Estonian voice for all problems, 'r' repeats

---

### Milestone 8: Deployment (Oracle Cloud VM)
**Goal:** Production deployment with TLS

**VM layout:**
```
/opt/mathquiz/app.jar
/var/lib/mathquiz/quiz.db
/var/lib/mathquiz/tts/    (cached .wav files)
```

**systemd service:** `/etc/systemd/system/mathquiz.service`
- Runs jar as `mathquiz` user
- Env vars: `PORT=8080`, `DATA_DIR=/var/lib/mathquiz`
- Restart on failure

**Nginx reverse proxy:**
- `443 → localhost:8080`
- Long cache headers for `/audio/*`
- Let's Encrypt TLS via certbot

**Backups:**
- Nightly cron copies `quiz.db` to dated backup
- TTS cache can be rebuilt (DB is critical)

**Test:** Smoke test on VM, verify persistence across restarts  
**Deliverable:** Public HTTPS deployment

---

### Milestone 9: Polish (Optional)
**Goal:** Maintainability and debugging

- Admin endpoints (behind password): view difficulty, reset stats, clear cache
- Structured logging for answer submissions, TTS cache hits
- Rate limiting on TTS API calls
- End-to-end tests with Playwright

---

## Acceptance Criteria Mapping

| Requirement | Milestone |
|-------------|-----------|
| Multi-user support | 5 |
| Big number display | 6 |
| Estonian voice + cache | 7 |
| Immediate feedback + animation | 6 |
| Streak counter, daily stars | 6 |
| Press 'r' to repeat voice | 7 |

---

## Testing Strategy

| Layer | Test Type | Tools |
|-------|-----------|-------|
| Domain | Unit tests | JUnit 6, AssertJ |
| Repository | Integration tests | Temp SQLite, Flyway |
| Service | Service tests | Repository + temp DB |
| Web | Integration tests | Javalin test tools |
| E2E | Browser tests | Playwright |

---

## Open Questions

- Audio format: keep WAV or transcode to MP3/Opus for smaller size?
Answer: keep WAV
