package com.github.exadmin.voterra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.exadmin.voterra.model.QuestionOption;
import com.github.exadmin.voterra.model.VoteSession;
import com.github.exadmin.voterra.model.VoteSummary;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResultStore {
    private final Path resultsFile;
    private final ObjectMapper objectMapper;

    public ResultStore(Path resultsFile) {
        this.resultsFile = resultsFile;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public synchronized void appendFinishedSession(VoteSession session, VoteSummary summary) throws IOException {
        ArrayNode root = readExistingResults();
        root.add(toResultNode(session, summary));
        Path parent = resultsFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        objectMapper.writeValue(resultsFile.toFile(), root);
    }

    private ArrayNode readExistingResults() throws IOException {
        if (!Files.exists(resultsFile) || Files.size(resultsFile) == 0) {
            return objectMapper.createArrayNode();
        }

        JsonNode root = objectMapper.readTree(resultsFile.toFile());
        if (!root.isArray()) {
            throw new IllegalStateException("Results file must contain a JSON array");
        }
        return (ArrayNode) root;
    }

    private ObjectNode toResultNode(VoteSession session, VoteSummary summary) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sessionId", session.sessionId());
        node.put("questionId", session.question().id());
        node.put("questionText", session.question().text());
        node.put("multipleChoice", session.question().multipleChoice());
        node.put("startedAt", session.startedAt().toString());
        node.put("endedAt", session.endsAt().toString());

        ArrayNode options = objectMapper.createArrayNode();
        for (QuestionOption option : session.question().options()) {
            ObjectNode optionNode = objectMapper.createObjectNode();
            optionNode.put("id", option.id());
            optionNode.put("text", option.text());
            options.add(optionNode);
        }
        node.set("options", options);
        node.set("summary", objectMapper.valueToTree(summary));
        return node;
    }
}
