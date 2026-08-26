package dev.flutter.netbeans.designer.canvas;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Opaque host-issued identity for one open Canvas backend session. */
public record CanvasSessionId(UUID value) {
    private static final Pattern EXTERNAL_FORM = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

    public CanvasSessionId {
        Objects.requireNonNull(value, "value");
        if (value.variant() != 2 || value.version() < 1 || value.version() > 5) {
            throw new IllegalArgumentException(
                    "Canvas session id must be an RFC-4122 UUID with version 1 through 5: "
                    + value);
        }
    }

    public static CanvasSessionId parse(String text) {
        Objects.requireNonNull(text, "text");
        if (!EXTERNAL_FORM.matcher(text).matches()) {
            throw new IllegalArgumentException(
                    "Canvas session id must use the lowercase RFC-4122 UUID format: "
                    + text);
        }
        return new CanvasSessionId(UUID.fromString(text));
    }

    public static CanvasSessionId random() {
        return new CanvasSessionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
