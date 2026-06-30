package com.github.exadmin.voterra.model;

public record VoteSnapshot(
    boolean active,
    boolean open,
    VoteSession session,
    VoteSummary summary,
    long remainingMillis
) {
    public static VoteSnapshot inactive() {
        return new VoteSnapshot(false, false, null, null, 0);
    }
}
