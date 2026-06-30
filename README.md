# Voterra

Voterra is a Java web application for live audience voting during online sessions. An administrator loads a prepared
question set, activates one question at a time, and participants vote from their browsers without passwords.

The server provides both the participant page and the admin console. Participants enter their first and last name, wait
for an active question, and can change their answer until the countdown ends. After the timeout, the page shows the
aggregated result. The admin console is available at `/admin`.

## Technology

- Java 24
- Maven
- JDK `HttpServer`
- Jackson for JSON
- SLF4J with Log4j 2 for logging
- JUnit Jupiter for tests

The application does not use Spring or Quarkus.

## Question File

Pass the question file path with `--questions-json-file`. The file contains an array of questions:

```json
[
  {
    "id": "q1",
    "text": "Which Java version should this project target?",
    "multipleChoice": false,
    "durationSeconds": 30,
    "options": [
      {"id": "java21", "text": "Java 21"},
      {"id": "java24", "text": "Java 24"}
    ]
  }
]
```

Rules:

- Each question must have 2 to 6 options.
- `multipleChoice: false` means participants can choose one option.
- `multipleChoice: true` means participants can choose more than one option.
- `durationSeconds` is optional. The default is 30 seconds.

See [sample-questions.json](sample-questions.json) for a working example.

## Production Run

Build the runnable JAR:

```bash
mvn -q package
```

Run the server:

```bash
java -jar target/voterra-server-1.0.jar --admin-password "secret" --questions-json-file sample-questions.json
```

Open:

- Participant page: `http://localhost:8080/`
- Admin console: `http://localhost:8080/admin`

Optional arguments:

- `--port 9090` changes the HTTP port. The default is `8080`.
- `--results-json-file /path/to/results.json` changes the results file path. The default is `results.json`.

The application writes logs to the console and to `all-logs.log` at DEBUG level. Finished voting sessions are appended to
the results JSON file with the voting session start timestamp.

## Local Development

Run the test suite:

```bash
mvn -q test
```

Run the app locally with the sample questions:

```bash
mvn -q package
java -jar target/voterra-server-1.0.jar \
  --admin-password secret \
  --questions-json-file sample-questions.json
```

During local testing, use:

- Participant page: `http://localhost:8080/`
- Admin console: `http://localhost:8080/admin`
- Admin password: `secret`

Stop the server with `Ctrl+C`.
