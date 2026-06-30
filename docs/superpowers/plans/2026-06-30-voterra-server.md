# Voterra Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Java 24 Maven-based Voterra live voting server.

**Architecture:** Use JDK `HttpServer` for HTTP, Jackson for JSON, Log4j through SLF4J for logging, and isolated domain classes for testable voting behavior. Static frontend assets are served from classpath resources.

**Tech Stack:** Java 24, Maven, JDK `HttpServer`, Jackson, SLF4J, Log4j 2, JUnit Jupiter.

## Global Constraints

- Maven GAV is `com.github.exadmin:voterra-server:1.0`.
- Do not use Spring or Quarkus.
- Do not add dependencies beyond Jackson, SLF4J, Log4j, and JUnit Jupiter.
- All code, comments, logs, and UI text are in English.
- Default HTTP port is `8080`; `--port` overrides it.
- `--admin-password` and `--questions-json-file` are required.
- `--results-json-file` defaults to `results.json`.
- Question duration defaults to 30 seconds.
- UI uses dark blue and gold tones inspired by "Who Wants to Be a Millionaire".

---

### Task 1: Maven Skeleton And Core Models

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/github/exadmin/voterra/AppConfig.java`
- Create: `src/main/java/com/github/exadmin/voterra/model/Question.java`
- Create: `src/main/java/com/github/exadmin/voterra/model/QuestionOption.java`
- Test: `src/test/java/com/github/exadmin/voterra/AppConfigTest.java`

**Interfaces:**
- Produces: `AppConfig.parse(String[] args)` and immutable question model records.

- [x] Write failing tests for required and optional CLI arguments.
- [x] Run tests and verify they fail because production classes do not exist.
- [x] Add Maven configuration and minimal model/config implementation.
- [x] Run tests and verify they pass.

### Task 2: Question Loading And Validation

**Files:**
- Create: `src/main/java/com/github/exadmin/voterra/service/QuestionLoader.java`
- Test: `src/test/java/com/github/exadmin/voterra/service/QuestionLoaderTest.java`

**Interfaces:**
- Consumes: `Question` and `QuestionOption`.
- Produces: `QuestionLoader.load(Path path)`.

- [x] Write failing tests for valid JSON, default duration, and option count validation.
- [x] Run tests and verify they fail.
- [x] Implement Jackson-based loading and validation.
- [x] Run tests and verify they pass.

### Task 3: Voting Session State And Results

**Files:**
- Create: `src/main/java/com/github/exadmin/voterra/model/VoteSession.java`
- Create: `src/main/java/com/github/exadmin/voterra/model/VoteSummary.java`
- Create: `src/main/java/com/github/exadmin/voterra/service/VotingService.java`
- Test: `src/test/java/com/github/exadmin/voterra/service/VotingServiceTest.java`

**Interfaces:**
- Consumes: `Question`.
- Produces: `VotingService.activate`, `VotingService.submitVote`, `VotingService.snapshot`.

- [x] Write failing tests for single-choice replacement, multiple-choice counting, and timeout closure.
- [x] Run tests and verify they fail.
- [x] Implement synchronized voting state and summaries.
- [x] Run tests and verify they pass.

### Task 4: Result Persistence

**Files:**
- Create: `src/main/java/com/github/exadmin/voterra/service/ResultStore.java`
- Test: `src/test/java/com/github/exadmin/voterra/service/ResultStoreTest.java`

**Interfaces:**
- Consumes: `VoteSession` and `VoteSummary`.
- Produces: `ResultStore.appendFinishedSession`.

- [x] Write failing test that stores repeated sessions with distinct timestamps.
- [x] Run tests and verify it fails.
- [x] Implement JSON array persistence.
- [x] Run tests and verify it passes.

### Task 5: HTTP Server And Frontend

**Files:**
- Create: `src/main/java/com/github/exadmin/voterra/Main.java`
- Create: `src/main/java/com/github/exadmin/voterra/web/VoterraServer.java`
- Create: `src/main/resources/web/index.html`
- Create: `src/main/resources/web/admin.html`
- Create: `src/main/resources/web/styles.css`
- Create: `src/main/resources/web/app.js`
- Create: `src/main/resources/web/admin.js`
- Create: `src/main/resources/log4j2.xml`
- Create: `sample-questions.json`

**Interfaces:**
- Consumes: `AppConfig`, `QuestionLoader`, `VotingService`, and `ResultStore`.
- Produces: runnable server on `/`, `/admin`, and `/api/*`.

- [x] Add HTTP endpoints and static resource serving.
- [x] Add participant and admin pages.
- [x] Add Log4j console and file logging at DEBUG.
- [x] Run the full Maven test suite.
- [x] Package the application.
