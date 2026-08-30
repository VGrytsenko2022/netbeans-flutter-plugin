package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Declarative, immutable description of one supported Flutter widget
 * constructor. It contains no NetBeans or Swing services.
 */
public final class WidgetDefinition {
    /**
     * Hard per-widget schema budget. Complex, state-aware Material controls
     * legitimately expose more than 256 independently resettable leaves; the
     * bound remains deliberately finite so contributed catalogs cannot turn a
     * validation pass into an unbounded allocation.
     */
    public static final int MAX_PROPERTIES = 512;
    public static final int MAX_SLOTS = 128;

    private static final Pattern TRAIT = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{0,254}$");
    private static final Comparator<PropertyDefinition> PROPERTY_ORDER = Comparator
            .comparing((PropertyDefinition value) -> value.parameter().style())
            .thenComparingInt(value -> value.parameter().order())
            .thenComparing(value -> value.name().value());
    private static final Comparator<SlotDefinition> SLOT_ORDER = Comparator
            .comparing((SlotDefinition value) -> value.parameter().style())
            .thenComparingInt(value -> value.parameter().order())
            .thenComparing(value -> value.name().value());

    private final WidgetTypeId typeId;
    private final String dartClassName;
    private final Optional<String> namedConstructor;
    private final boolean constConstructor;
    private final String dartLibraryUri;
    private final List<String> importUris;
    private final NavigableSet<String> traits;
    private final PaletteMetadata palette;
    private final List<PropertyDefinition> properties;
    private final List<SlotDefinition> slots;
    private final NavigableMap<String, PropertyDefinition> propertiesByName;
    private final NavigableMap<String, SlotDefinition> slotsByName;

    public WidgetDefinition(
            WidgetTypeId typeId,
            String dartClassName,
            Optional<String> namedConstructor,
            boolean constConstructor,
            String dartLibraryUri,
            Collection<String> importUris,
            Collection<String> traits,
            PaletteMetadata palette,
            Collection<PropertyDefinition> properties,
            Collection<SlotDefinition> slots) {
        this.typeId = Objects.requireNonNull(typeId, "typeId");
        this.dartClassName = requireDartIdentifier(dartClassName, "Dart class name");
        this.namedConstructor = Objects.requireNonNull(namedConstructor, "namedConstructor")
                .map(value -> requireDartIdentifier(value, "named constructor"));
        this.constConstructor = constConstructor;
        this.dartLibraryUri = DartImportUris.requireValid(dartLibraryUri, "Dart widget library URI");
        this.importUris = copyImportUris(importUris);
        if (!this.importUris.contains(this.dartLibraryUri)) {
            throw new IllegalArgumentException(
                    "Dart widget library URI must be present in importUris: " + this.dartLibraryUri);
        }
        this.traits = copyTraits(traits);
        this.palette = Objects.requireNonNull(palette, "palette");
        this.properties = copyProperties(properties);
        this.slots = copySlots(slots);
        validateReferencedSymbols();
        if (this.properties.size() > MAX_PROPERTIES) {
            throw new IllegalArgumentException(
                    "A widget definition cannot expose more than " + MAX_PROPERTIES + " properties");
        }
        if (this.slots.size() > MAX_SLOTS) {
            throw new IllegalArgumentException(
                    "A widget definition cannot expose more than " + MAX_SLOTS + " slots");
        }
        this.propertiesByName = indexProperties(this.properties);
        this.slotsByName = indexSlots(this.slots);
        validateConstructorParameters();
    }

    public WidgetTypeId typeId() {
        return typeId;
    }

    public String dartClassName() {
        return dartClassName;
    }

    public Optional<String> namedConstructor() {
        return namedConstructor;
    }

    public boolean constConstructor() {
        return constConstructor;
    }

    public String dartLibraryUri() {
        return dartLibraryUri;
    }

    public List<String> importUris() {
        return importUris;
    }

    public NavigableSet<String> traits() {
        return traits;
    }

    public PaletteMetadata palette() {
        return palette;
    }

    public List<PropertyDefinition> properties() {
        return properties;
    }

    public List<SlotDefinition> slots() {
        return slots;
    }

    public Optional<PropertyDefinition> property(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(propertiesByName.get(name.value()));
    }

    public Optional<SlotDefinition> slot(SlotName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(slotsByName.get(name.value()));
    }

    private void validateConstructorParameters() {
        HashMap<ParameterKey, String> positions = new HashMap<>();
        for (PropertyDefinition property : properties) {
            DartIdentifiers.requirePublicIdentifier(property.name().value(), "property name");
            registerParameter(positions, property.parameter(), property.name().value());
        }
        for (SlotDefinition slot : slots) {
            DartIdentifiers.requirePublicIdentifier(slot.name().value(), "slot name");
            registerParameter(positions, slot.parameter(), slot.name().value());
            if (propertiesByName.containsKey(slot.name().value())) {
                throw new IllegalArgumentException(
                        "A constructor argument cannot be both a property and a slot: " + slot.name().value());
            }
        }

        ArrayList<DartParameter> positional = new ArrayList<>();
        properties.stream().map(PropertyDefinition::parameter)
                .filter(value -> value.style() == ParameterStyle.POSITIONAL)
                .forEach(positional::add);
        slots.stream().map(SlotDefinition::parameter)
                .filter(value -> value.style() == ParameterStyle.POSITIONAL)
                .forEach(positional::add);
        positional.sort(Comparator.comparingInt(DartParameter::order));
        boolean optionalSeen = false;
        for (int index = 0; index < positional.size(); index++) {
            DartParameter parameter = positional.get(index);
            if (parameter.order() != index) {
                throw new IllegalArgumentException("Positional constructor parameter orders must be contiguous");
            }
            if (!parameter.required()) {
                optionalSeen = true;
            } else if (optionalSeen) {
                throw new IllegalArgumentException("Required positional parameters must precede optional parameters");
            }
        }
    }

    private void validateReferencedSymbols() {
        for (PropertyDefinition property : properties) {
            for (PropertyValueConstraint constraint : property.constraints()) {
                if (constraint instanceof PropertyValueConstraint.EnumValues enumValues
                        && !importUris.contains(enumValues.dartType().libraryUri())) {
                    throw new IllegalArgumentException(
                            "Dart enum library URI must be present in importUris: "
                            + enumValues.dartType().libraryUri());
                }
            }
        }
    }

    private static void registerParameter(
            HashMap<ParameterKey, String> positions,
            DartParameter parameter,
            String name) {
        ParameterKey key = new ParameterKey(parameter.style(), parameter.order());
        String previous = positions.putIfAbsent(key, name);
        if (previous != null) {
            throw new IllegalArgumentException(
                    "Duplicate " + parameter.style() + " constructor order " + parameter.order()
                    + " for " + previous + " and " + name);
        }
    }

    private record ParameterKey(ParameterStyle style, int order) {
    }

    private static String requireDartIdentifier(String value, String label) {
        return DartIdentifiers.requirePublicIdentifier(value, label);
    }

    private static List<String> copyImportUris(Collection<String> input) {
        Objects.requireNonNull(input, "importUris");
        TreeSet<String> ordered = new TreeSet<>();
        for (String value : input) {
            Objects.requireNonNull(value, "importUris contains null");
            ordered.add(DartImportUris.requireValid(value, "Dart import URI"));
        }
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("A widget definition must declare at least one Dart import URI");
        }
        return List.copyOf(ordered);
    }

    private static NavigableSet<String> copyTraits(Collection<String> input) {
        Objects.requireNonNull(input, "traits");
        TreeSet<String> ordered = new TreeSet<>();
        for (String value : input) {
            Objects.requireNonNull(value, "traits contains null");
            if (!TRAIT.matcher(value).matches()) {
                throw new IllegalArgumentException("Invalid widget trait: " + value);
            }
            ordered.add(value);
        }
        return Collections.unmodifiableNavigableSet(ordered);
    }

    private static List<PropertyDefinition> copyProperties(Collection<PropertyDefinition> input) {
        Objects.requireNonNull(input, "properties");
        ArrayList<PropertyDefinition> result = new ArrayList<>(input);
        if (result.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("properties contains null");
        }
        result.sort(PROPERTY_ORDER);
        return List.copyOf(result);
    }

    private static List<SlotDefinition> copySlots(Collection<SlotDefinition> input) {
        Objects.requireNonNull(input, "slots");
        ArrayList<SlotDefinition> result = new ArrayList<>(input);
        if (result.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("slots contains null");
        }
        result.sort(SLOT_ORDER);
        return List.copyOf(result);
    }

    private static NavigableMap<String, PropertyDefinition> indexProperties(List<PropertyDefinition> values) {
        TreeMap<String, PropertyDefinition> result = new TreeMap<>();
        for (PropertyDefinition value : values) {
            if (result.putIfAbsent(value.name().value(), value) != null) {
                throw new IllegalArgumentException("Duplicate property definition: " + value.name().value());
            }
        }
        return Collections.unmodifiableNavigableMap(result);
    }

    private static NavigableMap<String, SlotDefinition> indexSlots(List<SlotDefinition> values) {
        TreeMap<String, SlotDefinition> result = new TreeMap<>();
        for (SlotDefinition value : values) {
            if (result.putIfAbsent(value.name().value(), value) != null) {
                throw new IllegalArgumentException("Duplicate slot definition: " + value.name().value());
            }
        }
        return Collections.unmodifiableNavigableMap(result);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WidgetDefinition that)) {
            return false;
        }
        return typeId.equals(that.typeId)
                && dartClassName.equals(that.dartClassName)
                && namedConstructor.equals(that.namedConstructor)
                && constConstructor == that.constConstructor
                && dartLibraryUri.equals(that.dartLibraryUri)
                && importUris.equals(that.importUris)
                && traits.equals(that.traits)
                && palette.equals(that.palette)
                && properties.equals(that.properties)
                && slots.equals(that.slots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                typeId,
                dartClassName,
                namedConstructor,
                constConstructor,
                dartLibraryUri,
                importUris,
                traits,
                palette,
                properties,
                slots);
    }

    @Override
    public String toString() {
        return "WidgetDefinition[" + typeId.value() + ']';
    }
}
