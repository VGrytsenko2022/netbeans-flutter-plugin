package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import java.util.Objects;

/** Immutable metadata for one named widget slot. */
public record SlotDefinition(
        SlotName name,
        DartParameter parameter,
        SlotCardinality cardinality,
        int minChildren,
        int maxChildren,
        SlotAcceptance acceptance) {

    public SlotDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(cardinality, "cardinality");
        Objects.requireNonNull(acceptance, "acceptance");
        if (minChildren < 0 || maxChildren < minChildren || maxChildren > 10_000) {
            throw new IllegalArgumentException("Invalid child bounds for slot " + name);
        }
        if (cardinality == SlotCardinality.SINGLE && maxChildren > 1) {
            throw new IllegalArgumentException("A single slot cannot contain more than one child: " + name);
        }
    }
}
