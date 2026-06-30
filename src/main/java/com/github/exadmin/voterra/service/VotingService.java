package com.github.exadmin.voterra.service;

import com.github.exadmin.voterra.model.Question;
import com.github.exadmin.voterra.model.QuestionOption;
import com.github.exadmin.voterra.model.VoteSession;
import com.github.exadmin.voterra.model.VoteSnapshot;
import com.github.exadmin.voterra.model.VoteSummary;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class VotingService {
    private final Clock clock;
    private VoteSession activeSession;
    private final Map<String, Set<String>> votesByParticipant = new LinkedHashMap<>();

    public VotingService(Clock clock) {
        this.clock = clock;
    }

    public synchronized VoteSession activate(Question question) {
        if (activeSession != null && Duration.between(clock.instant(), activeSession.endsAt()).toMillis() > 0) {
            throw new IllegalStateException("Cannot activate a question while voting is open");
        }
        Instant startedAt = clock.instant();
        Instant endsAt = startedAt.plusSeconds(question.durationSeconds());
        activeSession = new VoteSession(UUID.randomUUID().toString(), question, startedAt, endsAt);
        votesByParticipant.clear();
        return activeSession;
    }

    public synchronized void submitVote(String participantName, List<String> optionIds) {
        if (participantName == null || participantName.isBlank()) {
            throw new IllegalArgumentException("Participant name is required");
        }
        VoteSession session = requireOpenSession();
        Set<String> submitted = validateOptions(session.question(), optionIds);
        votesByParticipant.put(participantName.trim(), submitted);
    }

    public synchronized VoteSnapshot snapshot() {
        if (activeSession == null) {
            return VoteSnapshot.inactive();
        }

        long remainingMillis = Duration.between(clock.instant(), activeSession.endsAt()).toMillis();
        boolean open = remainingMillis > 0;
        return new VoteSnapshot(true, open, activeSession, summarize(), Math.max(remainingMillis, 0));
    }

    private VoteSession requireOpenSession() {
        VoteSnapshot snapshot = snapshot();
        if (!snapshot.active() || !snapshot.open()) {
            throw new IllegalStateException("Voting is closed");
        }
        return activeSession;
    }

    private Set<String> validateOptions(Question question, List<String> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            throw new IllegalArgumentException("At least one option must be selected");
        }
        if (!question.multipleChoice() && optionIds.size() != 1) {
            throw new IllegalArgumentException("This question accepts exactly one option");
        }

        Set<String> allowed = question.options().stream()
            .map(QuestionOption::id)
            .collect(Collectors.toUnmodifiableSet());
        Set<String> submitted = new LinkedHashSet<>(optionIds);
        if (!allowed.containsAll(submitted)) {
            throw new IllegalArgumentException("Vote contains an unknown option");
        }
        return Set.copyOf(submitted);
    }

    private VoteSummary summarize() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (QuestionOption option : activeSession.question().options()) {
            counts.put(option.id(), 0);
        }

        for (Set<String> selectedOptionIds : votesByParticipant.values()) {
            for (String optionId : selectedOptionIds) {
                counts.computeIfPresent(optionId, (key, count) -> count + 1);
            }
        }

        return new VoteSummary(Map.copyOf(counts), votesByParticipant.size());
    }
}
