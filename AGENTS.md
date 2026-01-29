# Math Quiz Web

## What We're Building

A web-based math quiz app for kids learning arithmetic (addition, subtraction, multiplication, division).

**Target users:**
- Lars's kids (Birgit and Oskar)
- Potentially shared with their kindergarten (runs on a PC there)


## Key Features

- **Multi-user profiles** — each child has their own progress
- **Progressive difficulty** — number ranges expand as accuracy improves
- **Operation unlocking** — master addition → unlock subtraction → multiplication → division
- **SM-2 spaced repetition** — missed problems come back more often
- **Estonian voice** — problems read aloud via Neurokõne TTS (cached server-side)
- **Gamification** — streaks and daily stars to keep kids motivated
- **Big colorful numbers** — kid-friendly, easy to read

## Tech Stack

- **Backend:** Java 21 + Javalin + SQLite
- **Frontend:** HTMX + JTE templates + minimal vanilla JS (for audio)
- **TTS:** Neurokõne API with content-addressed server-side caching
- **Hosting:** exe.dev VM

## Project Status

See `plan.md` for the 9-milestone implementation plan.

## Deployment (exe.dev)

- **VM:** math-quiz.exe.xyz (user: `exedev`)
- **App URL:** https://math-quiz.exe.xyz (proxies to :8000)
- **Deploy:**
  ```bash
  ./gradlew clean build -x test
  scp build/libs/math-quiz-web-1.0-SNAPSHOT.jar math-quiz.exe.xyz:~/math-quiz.jar
  ssh math-quiz.exe.xyz "pkill -f math-quiz.jar; PORT=8000 nohup java -Xmx256m -Xms128m -XX:+UseSerialGC -jar ~/math-quiz.jar > ~/app.log 2>&1 </dev/null &"
  ```
  
  **JVM flags explained:**
  - `-Xmx256m -Xms128m` — Cap heap to reduce memory footprint
  - `-XX:+UseSerialGC` — Lower memory overhead for small apps
  - Optional: `-XX:NativeMemoryTracking=summary` for memory debugging
- **Make public:** `ssh exe.dev share set-public math-quiz`
- **Set port:** `ssh exe.dev share port math-quiz 8000`
- **View logs:** `ssh math-quiz.exe.xyz "tail -f ~/app.log"`

**SSH gotcha:** When running background processes via SSH, use `</dev/null` to fully detach stdin, otherwise SSH hangs waiting for file descriptors to close.

## Tips

Use tmux when running the server.
