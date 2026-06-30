package com.github.exadmin.voterra.model;

import java.util.Map;

public record VoteSummary(Map<String, Integer> countsByOptionId, int totalParticipants) {
}
