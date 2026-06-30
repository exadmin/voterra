package com.github.exadmin.voterra.web;

import com.github.exadmin.voterra.model.Question;
import com.github.exadmin.voterra.model.VoteSession;
import com.github.exadmin.voterra.model.VoteSnapshot;
import com.github.exadmin.voterra.model.VoteSummary;

public record VoteSnapshotResponse(
    boolean active,
    boolean open,
    SessionResponse session,
    VoteSummary summary,
    long remainingMillis
) {
    public static VoteSnapshotResponse from(VoteSnapshot snapshot) {
        return new VoteSnapshotResponse(
            snapshot.active(),
            snapshot.open(),
            snapshot.session() == null ? null : SessionResponse.from(snapshot.session()),
            snapshot.summary(),
            snapshot.remainingMillis()
        );
    }

    public record SessionResponse(
        String sessionId,
        Question question,
        String startedAt,
        String endsAt
    ) {
        static SessionResponse from(VoteSession session) {
            return new SessionResponse(
                session.sessionId(),
                session.question(),
                session.startedAt().toString(),
                session.endsAt().toString()
            );
        }
    }
}
