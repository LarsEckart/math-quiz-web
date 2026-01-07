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
- **Hosting:** Oracle Cloud Always Free VM

## Project Status

See `plan.md` for the 9-milestone implementation plan.
