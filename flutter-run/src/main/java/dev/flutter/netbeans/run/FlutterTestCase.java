package dev.flutter.netbeans.run;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Metadata for a test announced by a {@code testStart} event. */
public record FlutterTestCase(
        long id,
        String name,
        long suiteId,
        List<Long> groupIds,
        Optional<Integer> line,
        Optional<Integer> column,
        Optional<String> url) {

    public FlutterTestCase {
        if (id < 0 || suiteId < 0) {
            throw new IllegalArgumentException("Flutter test ids cannot be negative");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Flutter test name is required");
        }
        groupIds = List.copyOf(groupIds);
        line = positive(line, "line");
        column = positive(column, "column");
        url = Objects.requireNonNull(url, "url")
                .map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    private static Optional<Integer> positive(Optional<Integer> value, String name) {
        Objects.requireNonNull(value, name);
        value.ifPresent(number -> {
            if (number < 1) {
                throw new IllegalArgumentException("Flutter test " + name + " must be one-based");
            }
        });
        return value;
    }
}
