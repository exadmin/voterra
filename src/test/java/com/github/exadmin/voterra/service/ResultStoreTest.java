package com.github.exadmin.voterra.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.exadmin.voterra.model.Question;
import com.github.exadmin.voterra.model.QuestionOption;
import com.github.exadmin.voterra.model.VoteSession;
import com.github.exadmin.voterra.model.VoteSummary;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResultStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void appendsRepeatedSessionsWithDistinctStartTimestamps() throws IOException {
        Path resultsFile = tempDir.resolve("results.json");
        ResultStore store = new ResultStore(resultsFile);
        Question question = new Question("q1", "Choose one", List.of(
            new QuestionOption("a", "Answer A"),
            new QuestionOption("b", "Answer B")
        ), false, 30);

        store.appendFinishedSession(
            new VoteSession("s1", question, Instant.parse("2026-06-30T10:00:00Z"), Instant.parse("2026-06-30T10:00:30Z")),
            new VoteSummary(Map.of("a", 1, "b", 0), 1)
        );
        store.appendFinishedSession(
            new VoteSession("s2", question, Instant.parse("2026-06-30T11:00:00Z"), Instant.parse("2026-06-30T11:00:30Z")),
            new VoteSummary(Map.of("a", 0, "b", 1), 1)
        );

        JsonNode root = new ObjectMapper().readTree(resultsFile.toFile());

        assertEquals(2, root.size());
        assertEquals("2026-06-30T10:00:00Z", root.get(0).get("startedAt").asText());
        assertEquals("2026-06-30T11:00:00Z", root.get(1).get("startedAt").asText());
        assertEquals(1, root.get(1).get("summary").get("countsByOptionId").get("b").asInt());
    }
}
