package io.github.vgrytsenko2022.plugin.designer.persistence;

import java.util.Objects;

/** One immutable and concrete transaction diagnostic. */
public record PairFileTransactionIssue(
        PairFileTransactionIssueCode code,
        PairFileRole target,
        String message) {

    public PairFileTransactionIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(target, "target");
        message = Objects.requireNonNull(message, "message").strip();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
