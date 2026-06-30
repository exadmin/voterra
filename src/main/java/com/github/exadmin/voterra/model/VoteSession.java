package com.github.exadmin.voterra.model;

import java.time.Instant;

public record VoteSession(
    String sessionId,
    Question question,
    Instant startedAt,
    Instant endsAt
) {
}
