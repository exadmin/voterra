package com.github.exadmin.voterra.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.exadmin.voterra.model.Question;
import com.github.exadmin.voterra.model.QuestionOption;
import com.github.exadmin.voterra.model.VoteSummary;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class VotingServiceTest {
    @Test
    void replacesSingleChoiceVoteFromSameParticipant() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-30T10:00:00Z"));
        VotingService service = new VotingService(clock);

        service.activate(singleChoiceQuestion());
        service.submitVote("Ada Lovelace", List.of("a"));
        service.submitVote("Ada Lovelace", List.of("b"));

        VoteSummary summary = service.snapshot().summary();

        assertEquals(0, summary.countsByOptionId().get("a"));
        assertEquals(1, summary.countsByOptionId().get("b"));
        assertEquals(1, summary.totalParticipants());
    }

    @Test
    void countsMultipleChoiceSelectionsOncePerParticipant() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-30T10:00:00Z"));
        VotingService service = new VotingService(clock);

        service.activate(multipleChoiceQuestion());
        service.submitVote("Ada Lovelace", List.of("a", "b"));
        service.submitVote("Grace Hopper", List.of("b"));

        VoteSummary summary = service.snapshot().summary();

        assertEquals(1, summary.countsByOptionId().get("a"));
        assertEquals(2, summary.countsByOptionId().get("b"));
        assertEquals(2, summary.totalParticipants());
    }

    @Test
    void closesSessionAfterDeadline() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-30T10:00:00Z"));
        VotingService service = new VotingService(clock);

        service.activate(singleChoiceQuestion());
        clock.advance(Duration.ofSeconds(31));

        assertFalse(service.snapshot().open());
        assertThrows(IllegalStateException.class, () -> service.submitVote("Ada Lovelace", List.of("a")));
    }

    @Test
    void rejectsActivationWhileCurrentSessionIsOpen() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-30T10:00:00Z"));
        VotingService service = new VotingService(clock);

        service.activate(singleChoiceQuestion());

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
            service.activate(multipleChoiceQuestion())
        );

        assertEquals("Cannot activate a question while voting is open", error.getMessage());
    }

    @Test
    void allowsActivationAfterCurrentSessionCloses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-30T10:00:00Z"));
        VotingService service = new VotingService(clock);

        service.activate(singleChoiceQuestion());
        clock.advance(Duration.ofSeconds(31));
        service.activate(multipleChoiceQuestion());

        assertEquals("q2", service.snapshot().session().question().id());
    }

    private Question singleChoiceQuestion() {
        return new Question("q1", "Choose one", List.of(
            new QuestionOption("a", "Answer A"),
            new QuestionOption("b", "Answer B")
        ), false, 30);
    }

    private Question multipleChoiceQuestion() {
        return new Question("q2", "Choose many", List.of(
            new QuestionOption("a", "Answer A"),
            new QuestionOption("b", "Answer B"),
            new QuestionOption("c", "Answer C")
        ), true, 30);
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
