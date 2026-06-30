package com.github.exadmin.voterra.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.exadmin.voterra.AppConfig;
import com.github.exadmin.voterra.model.Question;
import com.github.exadmin.voterra.model.VoteSnapshot;
import com.github.exadmin.voterra.service.ResultStore;
import com.github.exadmin.voterra.service.VotingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoterraServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoterraServer.class);

    private final AppConfig config;
    private final List<Question> questions;
    private final VotingService votingService;
    private final ResultStore resultStore;
    private final ObjectMapper objectMapper;
    private final Set<String> adminTokens = ConcurrentHashMap.newKeySet();
    private final Set<String> persistedSessionIds = ConcurrentHashMap.newKeySet();
    private HttpServer httpServer;

    public VoterraServer(
        AppConfig config,
        List<Question> questions,
        VotingService votingService,
        ResultStore resultStore
    ) {
        this.config = config;
        this.questions = List.copyOf(questions);
        this.votingService = votingService;
        this.resultStore = resultStore;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(config.port()), 0);
        httpServer.createContext("/", this::handleRoot);
        httpServer.createContext("/admin", exchange -> serveResource(exchange, "web/admin.html", "text/html"));
        httpServer.createContext("/api/state", this::handleState);
        httpServer.createContext("/api/vote", this::handleVote);
        httpServer.createContext("/api/admin/login", this::handleAdminLogin);
        httpServer.createContext("/api/admin/questions", this::handleAdminQuestions);
        httpServer.createContext("/api/admin/activate", this::handleAdminActivate);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.start();
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path)) {
            serveResource(exchange, "web/index.html", "text/html");
            return;
        }
        if (path.startsWith("/web/")) {
            String resource = path.substring(1);
            serveResource(exchange, resource, contentType(resource));
            return;
        }
        sendJson(exchange, 404, Map.of("error", "Not found"));
    }

    private void handleState(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        VoteSnapshot snapshot = votingService.snapshot();
        persistClosedSnapshot(snapshot);
        sendJson(exchange, 200, VoteSnapshotResponse.from(snapshot));
    }

    private void handleVote(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonNode body = readJson(exchange);
        String participantName = text(body, "participantName");
        List<String> optionIds = objectMapper.convertValue(
            body.withArray("optionIds"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
        votingService.submitVote(participantName, optionIds);
        sendJson(exchange, 200, Map.of("ok", true));
    }

    private void handleAdminLogin(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonNode body = readJson(exchange);
        if (!config.adminPassword().equals(text(body, "password"))) {
            sendJson(exchange, 401, Map.of("error", "Invalid admin password"));
            return;
        }

        String token = UUID.randomUUID().toString();
        adminTokens.add(token);
        sendJson(exchange, 200, Map.of("token", token));
    }

    private void handleAdminQuestions(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        requireAdmin(exchange);
        sendJson(exchange, 200, Map.of("questions", questions));
    }

    private void handleAdminActivate(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        requireAdmin(exchange);
        JsonNode body = readJson(exchange);
        String questionId = text(body, "questionId");
        Question question = questions.stream()
            .filter(item -> item.id().equals(questionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown question id: " + questionId));

        try {
            votingService.activate(question);
            persistedSessionIds.clear();
            sendJson(exchange, 200, VoteSnapshotResponse.from(votingService.snapshot()));
            LOGGER.info("Activated question {}", question.id());
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, Map.of("error", e.getMessage()));
        }
    }

    private void persistClosedSnapshot(VoteSnapshot snapshot) {
        if (!snapshot.active() || snapshot.open() || snapshot.session() == null) {
            return;
        }
        if (!persistedSessionIds.add(snapshot.session().sessionId())) {
            return;
        }
        try {
            resultStore.appendFinishedSession(snapshot.session(), snapshot.summary());
            LOGGER.info("Saved results for session {}", snapshot.session().sessionId());
        } catch (IOException e) {
            LOGGER.error("Cannot save voting results for session {}", snapshot.session().sessionId(), e);
        }
    }

    private void requireMethod(HttpExchange exchange, String method) throws IOException {
        if (!method.equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            throw new RequestHandledException();
        }
    }

    private void requireAdmin(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("X-Admin-Token");
        if (token == null || !adminTokens.contains(token)) {
            sendJson(exchange, 401, Map.of("error", "Admin authorization is required"));
            throw new RequestHandledException();
        }
    }

    private JsonNode readJson(HttpExchange exchange) throws IOException {
        try (InputStream body = exchange.getRequestBody()) {
            return objectMapper.readTree(body);
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.asText().isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return field.asText();
    }

    private void serveResource(HttpExchange exchange, String resource, String contentType) throws IOException {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                sendJson(exchange, 404, Map.of("error", "Not found"));
                return;
            }
            byte[] bytes = input.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private String contentType(String resource) {
        if (resource.endsWith(".css")) {
            return "text/css";
        }
        if (resource.endsWith(".js")) {
            return "application/javascript";
        }
        return "text/plain";
    }

    private static class RequestHandledException extends RuntimeException {
    }
}
