package com.github.exadmin.voterra.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.exadmin.voterra.model.Question;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuestionLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsQuestionsFromJson() throws IOException {
        Path file = tempDir.resolve("questions.json");
        Files.writeString(file, """
            [
              {
                "id": "q1",
                "text": "Choose one",
                "multipleChoice": false,
                "durationSeconds": 45,
                "options": [
                  {"id": "a", "text": "Answer A"},
                  {"id": "b", "text": "Answer B"}
                ]
              }
            ]
            """);

        List<Question> questions = new QuestionLoader().load(file);

        assertEquals(1, questions.size());
        assertEquals("q1", questions.getFirst().id());
        assertEquals("Choose one", questions.getFirst().text());
        assertEquals(45, questions.getFirst().durationSeconds());
        assertEquals(2, questions.getFirst().options().size());
    }

    @Test
    void usesThirtySecondsWhenDurationIsMissing() throws IOException {
        Path file = tempDir.resolve("questions.json");
        Files.writeString(file, """
            [
              {
                "id": "q1",
                "text": "Choose one",
                "multipleChoice": false,
                "options": [
                  {"id": "a", "text": "Answer A"},
                  {"id": "b", "text": "Answer B"}
                ]
              }
            ]
            """);

        List<Question> questions = new QuestionLoader().load(file);

        assertEquals(30, questions.getFirst().durationSeconds());
    }

    @Test
    void rejectsQuestionWithTooFewOptions() throws IOException {
        Path file = tempDir.resolve("questions.json");
        Files.writeString(file, """
            [
              {
                "id": "q1",
                "text": "Choose one",
                "multipleChoice": false,
                "options": [
                  {"id": "a", "text": "Answer A"}
                ]
              }
            ]
            """);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
            new QuestionLoader().load(file)
        );

        assertEquals("Question q1 must have between 2 and 6 options", error.getMessage());
    }
}
