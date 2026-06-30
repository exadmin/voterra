package com.github.exadmin.voterra;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record AppConfig(int port, String adminPassword, Path questionsJsonFile, Path resultsJsonFile) {
    public static AppConfig parse(String[] args) {
        Map<String, String> values = parsePairs(args);
        String adminPassword = required(values, "--admin-password");
        Path questionsFile = Path.of(required(values, "--questions-json-file"));
        int port = parsePort(values.getOrDefault("--port", "8080"));
        Path resultsFile = Path.of(values.getOrDefault("--results-json-file", "results.json"));
        return new AppConfig(port, adminPassword, questionsFile, resultsFile);
    }

    private static Map<String, String> parsePairs(String[] args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Arguments must be provided as --name value pairs");
        }

        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String name = args[i];
            if (!name.startsWith("--")) {
                throw new IllegalArgumentException("Invalid argument name: " + name);
            }
            values.put(name, args[i + 1]);
        }
        return values;
    }

    private static String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required argument: " + name);
        }
        return value;
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("--port must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--port must be between 1 and 65535", e);
        }
    }
}
