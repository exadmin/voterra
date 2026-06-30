package com.github.exadmin.voterra.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.exadmin.voterra.model.Question;
import com.github.exadmin.voterra.model.QuestionOption;
import com.github.exadmin.voterra.model.VoteSession;
import com.github.exadmin.voterra.model.VoteSnapshot;
import com.github.exadmin.voterra.model.VoteSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VoteSnapshotResponseTest {
    @Test
    void serializesSessionTimestampsAsStrings() throws Exception {
        Question question = new Question("q1", "Choose one", List.of(
            new QuestionOption("a", "Answer A"),
            new QuestionOption("b", "Answer B")
        ), false, 30);
        VoteSnapshot snapshot = new VoteSnapshot(
            true,
            true,
            new VoteSession("s1", question, Instant.parse("2026-06-30T10:00:00Z"), Instant.parse("2026-06-30T10:00:30Z")),
            new VoteSummary(Map.of("a", 1, "b", 0), 1),
            30000
        );

        JsonNode json = new ObjectMapper().readTree(
            new ObjectMapper().writeValueAsBytes(VoteSnapshotResponse.from(snapshot))
        );

        assertEquals("2026-06-30T10:00:00Z", json.get("session").get("startedAt").asText());
        assertEquals("2026-06-30T10:00:30Z", json.get("session").get("endsAt").asText());
    }
}
