package com.github.exadmin.voterra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AppConfigTest {
    @Test
    void parsesRequiredArgumentsAndDefaults() {
        AppConfig config = AppConfig.parse(new String[] {
            "--admin-password", "secret",
            "--questions-json-file", "questions.json"
        });

        assertEquals(8080, config.port());
        assertEquals("secret", config.adminPassword());
        assertEquals(Path.of("questions.json"), config.questionsJsonFile());
        assertEquals(Path.of("results.json"), config.resultsJsonFile());
    }

    @Test
    void parsesOptionalPortAndResultsFile() {
        AppConfig config = AppConfig.parse(new String[] {
            "--admin-password", "secret",
            "--questions-json-file", "questions.json",
            "--port", "9090",
            "--results-json-file", "custom-results.json"
        });

        assertEquals(9090, config.port());
        assertEquals(Path.of("custom-results.json"), config.resultsJsonFile());
    }

    @Test
    void rejectsMissingAdminPassword() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
            AppConfig.parse(new String[] {"--questions-json-file", "questions.json"})
        );

        assertEquals("Missing required argument: --admin-password", error.getMessage());
    }

    @Test
    void rejectsInvalidPort() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
            AppConfig.parse(new String[] {
                "--admin-password", "secret",
                "--questions-json-file", "questions.json",
                "--port", "0"
            })
        );

        assertEquals("--port must be between 1 and 65535", error.getMessage());
    }
}
