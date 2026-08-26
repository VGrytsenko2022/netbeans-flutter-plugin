package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable metadata for one Dart constructor property. */
public record PropertyDefinition(
        PropertyName name,
        DartParameter parameter,
        List<PropertyValueConstraint> constraints,
        Optional<PropertyValue> creationDefault) {

    public PropertyDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(constraints, "constraints");
        Objects.requireNonNull(creationDefault, "creationDefault");
        ArrayList<PropertyValueConstraint> ordered = new ArrayList<>(constraints);
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("A property must accept at least one value kind: " + name);
        }
        if (ordered.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("constraints contains null");
        }
        ordered.sort(Comparator.comparingInt(value -> value.kind().ordinal()));
        Set<PropertyValueKind> kinds = new HashSet<>();
        for (PropertyValueConstraint constraint : ordered) {
            if (!kinds.add(constraint.kind())) {
                throw new IllegalArgumentException("Duplicate constraint for " + constraint.kind() + ": " + name);
            }
        }
        List<PropertyValueConstraint> acceptedConstraints = List.copyOf(ordered);
        constraints = acceptedConstraints;
        if (creationDefault.isPresent()) {
            PropertyValue value = creationDefault.orElseThrow();
            PropertyValueConstraint matching = acceptedConstraints.stream()
                    .filter(constraint -> constraint.kind() == value.kind())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Creation default kind " + value.kind() + " is not accepted by " + name));
            if (!matching.accepts(value)) {
                throw new IllegalArgumentException(
                        "Creation default for " + name + " violates " + matching.description());
            }
        }
    }

    public Set<PropertyValueKind> acceptedKinds() {
        EnumSet<PropertyValueKind> result = EnumSet.noneOf(PropertyValueKind.class);
        for (PropertyValueConstraint constraint : constraints) {
            result.add(constraint.kind());
        }
        return Collections.unmodifiableSet(result);
    }
}
