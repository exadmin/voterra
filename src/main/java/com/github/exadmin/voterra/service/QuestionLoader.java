package com.github.exadmin.voterra.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.exadmin.voterra.model.Question;
import com.github.exadmin.voterra.model.QuestionOption;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QuestionLoader {
    private final ObjectMapper objectMapper;

    public QuestionLoader() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<Question> load(Path path) throws IOException {
        List<QuestionFileEntry> entries = objectMapper.readerForListOf(QuestionFileEntry.class).readValue(path.toFile());
        List<Question> questions = new ArrayList<>();
        for (QuestionFileEntry entry : entries) {
            questions.add(toQuestion(entry));
        }
        return List.copyOf(questions);
    }

    private Question toQuestion(QuestionFileEntry entry) {
        requireText(entry.id, "Question id is required");
        requireText(entry.text, "Question " + entry.id + " text is required");
        if (entry.options == null || entry.options.size() < 2 || entry.options.size() > 6) {
            throw new IllegalArgumentException("Question " + entry.id + " must have between 2 and 6 options");
        }

        List<QuestionOption> options = new ArrayList<>();
        for (OptionFileEntry option : entry.options) {
            requireText(option.id, "Question " + entry.id + " has an option without id");
            requireText(option.text, "Question " + entry.id + " option " + option.id + " text is required");
            options.add(new QuestionOption(option.id, option.text));
        }

        int duration = entry.durationSeconds == null ? 30 : entry.durationSeconds;
        if (duration < 1) {
            throw new IllegalArgumentException("Question " + entry.id + " durationSeconds must be positive");
        }

        return new Question(entry.id, entry.text, List.copyOf(options), entry.multipleChoice, duration);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QuestionFileEntry {
        public String id;
        public String text;
        public boolean multipleChoice;
        public Integer durationSeconds;
        public List<OptionFileEntry> options;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OptionFileEntry {
        public String id;
        public String text;
    }
}
