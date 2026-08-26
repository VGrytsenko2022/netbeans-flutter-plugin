package dev.flutter.netbeans.designer.generation;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** One canonical Dart import directive and its optional generated prefix. */
public record DartImportDirective(String uri, Optional<String> prefix) {
    private static final Pattern PREFIX = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public DartImportDirective {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("Import URI must not be blank");
        }
        Objects.requireNonNull(prefix, "prefix");
        if (prefix.isPresent() && !PREFIX.matcher(prefix.orElseThrow()).matches()) {
            throw new IllegalArgumentException("Invalid Dart import prefix: " + prefix.orElseThrow());
        }
    }
}
