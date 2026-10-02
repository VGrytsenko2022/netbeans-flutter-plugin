package io.github.vgrytsenko2022.run;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable snapshot of the accumulated state for one test id. */
public record FlutterTestCaseState(
        long id,
        Optional<FlutterTestCase> test,
        FlutterTestOutcome outcome,
        boolean completed,
        boolean hidden,
        boolean skipped,
        Optional<Long> startedAt,
        Optional<Long> completedAt,
        List<FlutterTestMessage> messages,
        List<FlutterTestError> errors) {

    public FlutterTestCaseState {
        if (id < 0) {
            throw new IllegalArgumentException("Flutter test id cannot be negative");
        }
        Objects.requireNonNull(test, "test");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(completedAt, "completedAt");
        messages = List.copyOf(messages);
        errors = List.copyOf(errors);
    }

    public Optional<Long> durationMillis() {
        if (startedAt.isEmpty() || completedAt.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Math.max(0, completedAt.get() - startedAt.get()));
    }
}
