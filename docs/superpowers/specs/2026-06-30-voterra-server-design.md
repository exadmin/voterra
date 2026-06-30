# Voterra Server Design

## Goal

Build a Java 24 Maven web application that runs a live audience voting server without Spring or Quarkus.

## Architecture

The application uses the JDK `HttpServer` as the embedded web server. Static HTML, CSS, and JavaScript are served from classpath resources, and JSON endpoints expose the participant and admin workflows.

The domain logic is isolated from HTTP handling. Question loading, vote session state, vote aggregation, and result persistence are implemented as focused Java classes so they can be tested without starting a server.

## Dependencies

The approved dependencies are:

- Jackson for JSON parsing and writing.
- SLF4J with Log4j for logging.
- JUnit Jupiter for tests.

No Spring, Quarkus, or additional application framework is used.

## Runtime Behavior

At startup, the application requires `--admin-password` and `--questions-json-file`. It accepts `--port`, defaulting to `8080`, and `--results-json-file`, defaulting to `results.json`.

Participants open `/`, enter first and last name, and then poll the server for the active question. While voting is open, they can submit or change their answer. When voting closes, the UI displays aggregated results.

Administrators open `/admin`, enter the configured password, view questions loaded from JSON, activate one question at a time, and see current or final results.

## Voting Rules

Each question has text, two to six answer options, a single-choice or multiple-choice marker, and an optional duration. If duration is omitted, the server uses 30 seconds.

Votes are stored by participant name, so each participant has one effective vote per question session. For multiple-choice questions, each selected option receives one count from that participant.

## Persistence

Every finished voting session is appended to `results.json`. Each stored session includes the session start timestamp so repeated voting for the same question remains distinguishable after restarts.

## Interface Style

All code, comments, log messages, and UI text are in English. The pages use a dark blue and gold visual style inspired by "Who Wants to Be a Millionaire".
