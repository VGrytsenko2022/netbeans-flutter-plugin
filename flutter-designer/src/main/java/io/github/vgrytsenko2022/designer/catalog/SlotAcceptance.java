package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Declarative child-type rule for a constructor slot. */
public sealed interface SlotAcceptance permits
        SlotAcceptance.AnyWidget,
        SlotAcceptance.HasTrait,
        SlotAcceptance.ExactTypes {

    boolean accepts(WidgetDefinition definition);

    record AnyWidget() implements SlotAcceptance {
        @Override
        public boolean accepts(WidgetDefinition definition) {
            Objects.requireNonNull(definition, "definition");
            return true;
        }
    }

    record HasTrait(String trait) implements SlotAcceptance {
        private static final Pattern TRAIT = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{0,254}$");

        public HasTrait {
            Objects.requireNonNull(trait, "trait");
            if (!TRAIT.matcher(trait).matches()) {
                throw new IllegalArgumentException("Invalid widget trait: " + trait);
            }
        }

        @Override
        public boolean accepts(WidgetDefinition definition) {
            Objects.requireNonNull(definition, "definition");
            return definition.traits().contains(trait);
        }
    }

    record ExactTypes(List<WidgetTypeId> typeIds) implements SlotAcceptance {
        public ExactTypes {
            Objects.requireNonNull(typeIds, "typeIds");
            ArrayList<WidgetTypeId> ordered = new ArrayList<>(typeIds);
            if (ordered.isEmpty() || ordered.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Exact type acceptance must contain at least one type");
            }
            if (ordered.size() != new HashSet<>(ordered).size()) {
                throw new IllegalArgumentException("Exact accepted widget types must be unique");
            }
            ordered.sort(Comparator.comparing(WidgetTypeId::value));
            typeIds = List.copyOf(ordered);
        }

        @Override
        public boolean accepts(WidgetDefinition definition) {
            Objects.requireNonNull(definition, "definition");
            return typeIds.contains(definition.typeId());
        }
    }
}
