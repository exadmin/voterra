package com.github.exadmin.voterra.model;

import java.util.List;

public record Question(
    String id,
    String text,
    List<QuestionOption> options,
    boolean multipleChoice,
    int durationSeconds
) {
}
