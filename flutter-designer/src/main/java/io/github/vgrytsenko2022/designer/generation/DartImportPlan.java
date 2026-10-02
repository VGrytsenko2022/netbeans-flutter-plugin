package io.github.vgrytsenko2022.designer.generation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable, URI-ordered import plan used by one generated document. */
public record DartImportPlan(List<DartImportDirective> directives) {

    public DartImportPlan {
        directives = List.copyOf(Objects.requireNonNull(directives, "directives"));
        String previous = null;
        HashSet<String> prefixes = new HashSet<>();
        for (DartImportDirective directive : directives) {
            Objects.requireNonNull(directive, "directives contains null");
            if (previous != null && previous.compareTo(directive.uri()) >= 0) {
                throw new IllegalArgumentException("Import directives must be unique and URI ordered");
            }
            Optional<String> prefix = directive.prefix();
            if (prefix.isPresent() && !prefixes.add(prefix.orElseThrow())) {
                throw new IllegalArgumentException("Import prefixes must be unique");
            }
            previous = directive.uri();
        }
    }
}
