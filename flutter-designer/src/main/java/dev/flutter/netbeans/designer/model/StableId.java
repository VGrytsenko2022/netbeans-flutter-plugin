package dev.flutter.netbeans.designer.model;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stable RFC-4122 identifier used by designer documents and widget nodes. */
public record StableId(UUID value) implements Comparable<StableId> {
    private static final Pattern EXTERNAL_FORM = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

    public StableId {
        Objects.requireNonNull(value, "value");
        if (value.variant() != 2 || value.version() < 1 || value.version() > 5) {
            throw new IllegalArgumentException("Stable id must be an RFC-4122 UUID with version 1 through 5: " + value);
        }
    }

    public static StableId parse(String text) {
        Objects.requireNonNull(text, "text");
        if (!EXTERNAL_FORM.matcher(text).matches()) {
            throw new IllegalArgumentException("Stable id must use the lowercase Flutter Designer UUID format: " + text);
        }
        return new StableId(UUID.fromString(text));
    }

    public static StableId random() {
        return new StableId(UUID.randomUUID());
    }

    @Override
    public int compareTo(StableId other) {
        Objects.requireNonNull(other, "other");
        return toString().compareTo(other.toString());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
