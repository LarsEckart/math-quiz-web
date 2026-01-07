package mathquiz.domain;

import java.time.Instant;

/**
 * A user (player) of the math quiz.
 */
public record User(int id, String name, Instant createdAt) {
}
