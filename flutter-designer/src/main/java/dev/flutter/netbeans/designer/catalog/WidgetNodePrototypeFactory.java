package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Creates detached immutable widget prototypes from exact catalog definitions. */
public final class WidgetNodePrototypeFactory {
    private WidgetNodePrototypeFactory() {
    }

    /**
     * Creates one prototype with the supplied stable identifier.
     *
     * <p>Only explicit catalog creation defaults are materialized. Every
     * declared slot is present and empty with the cardinality declared by its
     * {@link SlotDefinition}. A required slot with a positive minimum therefore
     * remains intentionally incomplete: the detached value is suitable as an
     * atomic wrapper payload, while document validation still rejects inserting
     * it without its required child.</p>
     *
     * @param definition exact immutable catalog definition
     * @param id stable identifier for the new widget
     * @return a detached immutable widget prototype
     */
    public static WidgetNode create(WidgetDefinition definition, StableId id) {
        return create(definition, id, Map.of());
    }

    /**
     * Creates one prototype with explicit creation-time property values.
     *
     * <p>Explicit values override catalog creation defaults. Unknown property
     * names, values outside their catalog constraints, and required properties
     * left without either an explicit value or a reviewed creation default are
     * rejected before a model node is created. Required child slots remain
     * detached and empty so wrapper commands can still assemble them
     * atomically.</p>
     *
     * @param definition exact immutable catalog definition
     * @param id stable identifier for the new widget
     * @param creationValues explicit values selected by the creation workflow
     * @return a detached immutable widget prototype
     */
    public static WidgetNode create(
            WidgetDefinition definition,
            StableId id,
            Map<PropertyName, ? extends PropertyValue> creationValues) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(id, "id");
        return createNode(
                definition,
                id,
                validatedCreationProperties(definition, creationValues));
    }

    private static LinkedHashMap<PropertyName, PropertyValue>
            validatedCreationProperties(
                    WidgetDefinition definition,
                    Map<PropertyName, ? extends PropertyValue> creationValues) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(creationValues, "creationValues");

        for (Map.Entry<PropertyName, ? extends PropertyValue> entry
                : creationValues.entrySet()) {
            PropertyName name = Objects.requireNonNull(
                    entry.getKey(), "creationValues contains null property name");
            PropertyValue value = Objects.requireNonNull(
                    entry.getValue(), "creationValues contains null value for " + name);
            PropertyDefinition property = definition.property(name)
                    .orElseThrow(() -> new IllegalArgumentException(
                    "Unknown creation property '" + name.value() + "' for widget '"
                    + definition.typeId().value() + "'."));
            PropertyValueConstraint constraint = property.constraints().stream()
                    .filter(candidate -> candidate.kind() == value.kind())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                    "Creation property '" + name.value() + "' for widget '"
                    + definition.typeId().value() + "' rejects value kind '"
                    + value.kind().wireName() + "'."));
            if (!constraint.accepts(value)) {
                throw new IllegalArgumentException(
                        "Creation property '" + name.value() + "' for widget '"
                        + definition.typeId().value() + "' violates catalog constraint '"
                        + constraint.description() + "'.");
            }
        }

        LinkedHashMap<PropertyName, PropertyValue> result =
                new LinkedHashMap<>(definition.properties().size());
        for (PropertyDefinition property : definition.properties()) {
            PropertyValue explicit = creationValues.get(property.name());
            if (explicit != null) {
                result.put(property.name(), explicit);
            } else if (property.creationDefault().isPresent()) {
                result.put(property.name(), property.creationDefault().orElseThrow());
            } else if (property.parameter().required()) {
                throw new IllegalArgumentException(
                        "Required creation property '" + property.name().value()
                        + "' is missing for widget '" + definition.typeId().value() + "'.");
            }
        }
        return result;
    }

    private static WidgetNode createNode(
            WidgetDefinition definition,
            StableId id,
            Map<PropertyName, PropertyValue> properties) {
        LinkedHashMap<SlotName, WidgetSlot> slots =
                new LinkedHashMap<>(definition.slots().size());
        for (SlotDefinition slot : definition.slots()) {
            WidgetSlot empty = switch (slot.cardinality()) {
                case SINGLE -> WidgetSlot.SingleSlot.empty();
                case LIST -> new WidgetSlot.ListSlot(List.of());
            };
            slots.put(slot.name(), empty);
        }

        return new WidgetNode(id, definition.typeId(), properties, slots);
    }

    /**
     * Creates one prototype using exactly one identifier supplied on demand.
     *
     * @param definition exact immutable catalog definition
     * @param idSupplier supplier invoked once for the new widget identifier
     * @return a detached immutable widget prototype
     */
    public static WidgetNode create(
            WidgetDefinition definition,
            Supplier<StableId> idSupplier) {
        return create(definition, idSupplier, Map.of());
    }

    /** Creates one explicitly configured prototype with one supplied id. */
    public static WidgetNode create(
            WidgetDefinition definition,
            Supplier<StableId> idSupplier,
            Map<PropertyName, ? extends PropertyValue> creationValues) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(idSupplier, "idSupplier");
        LinkedHashMap<PropertyName, PropertyValue> properties =
                validatedCreationProperties(definition, creationValues);
        StableId id = Objects.requireNonNull(idSupplier.get(), "idSupplier returned null");
        return createNode(definition, id, properties);
    }
}
