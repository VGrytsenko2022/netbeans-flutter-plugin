package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/** Immutable, deterministically iterable widget-definition catalog. */
public final class WidgetCatalog {
    public static final int API_VERSION = 6;

    private static final Comparator<WidgetDefinition> PALETTE_ORDER = Comparator
            .comparingInt((WidgetDefinition value) -> value.palette().categoryOrder())
            .thenComparing(value -> value.palette().categoryId())
            .thenComparingInt(value -> value.palette().itemOrder())
            .thenComparing(value -> value.typeId().value());

    private final NavigableMap<String, WidgetDefinition> definitionsById;
    private final List<WidgetDefinition> definitions;
    private final List<WidgetDefinition> paletteDefinitions;

    private WidgetCatalog(NavigableMap<String, WidgetDefinition> definitionsById) {
        this.definitionsById = Collections.unmodifiableNavigableMap(new TreeMap<>(definitionsById));
        this.definitions = List.copyOf(this.definitionsById.values());
        validatePaletteCategories(this.definitions);
        ArrayList<WidgetDefinition> palette = new ArrayList<>(this.definitions);
        palette.sort(PALETTE_ORDER);
        this.paletteDefinitions = List.copyOf(palette);
    }

    public static WidgetCatalog strict(Collection<WidgetDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        TreeMap<String, WidgetDefinition> indexed = new TreeMap<>();
        for (WidgetDefinition definition : definitions) {
            Objects.requireNonNull(definition, "definitions contains null");
            WidgetDefinition previous = indexed.putIfAbsent(definition.typeId().value(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate widget type id: " + definition.typeId().value());
            }
        }
        return new WidgetCatalog(indexed);
    }

    static WidgetCatalog fromIndexed(NavigableMap<String, WidgetDefinition> definitions) {
        return new WidgetCatalog(definitions);
    }

    public Optional<WidgetDefinition> find(WidgetTypeId typeId) {
        Objects.requireNonNull(typeId, "typeId");
        return Optional.ofNullable(definitionsById.get(typeId.value()));
    }

    public List<WidgetDefinition> definitions() {
        return definitions;
    }

    public List<WidgetDefinition> paletteDefinitions() {
        return paletteDefinitions;
    }

    NavigableMap<String, WidgetDefinition> indexedDefinitions() {
        return definitionsById;
    }

    private static void validatePaletteCategories(List<WidgetDefinition> definitions) {
        TreeMap<String, TreeSet<Integer>> ordersByCategory = new TreeMap<>();
        for (WidgetDefinition definition : definitions) {
            PaletteMetadata palette = definition.palette();
            ordersByCategory.computeIfAbsent(palette.categoryId(), ignored -> new TreeSet<>())
                    .add(palette.categoryOrder());
        }
        for (Map.Entry<String, TreeSet<Integer>> entry : ordersByCategory.entrySet()) {
            if (entry.getValue().size() > 1) {
                throw new IllegalArgumentException(
                        "Palette category " + entry.getKey()
                        + " has inconsistent orders " + entry.getValue());
            }
        }
    }
}
