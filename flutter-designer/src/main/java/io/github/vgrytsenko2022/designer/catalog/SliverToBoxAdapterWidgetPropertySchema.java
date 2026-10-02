package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Reviewed Designer projection of Flutter's SliverToBoxAdapter constructor. */
public final class SliverToBoxAdapterWidgetPropertySchema {
    public static final WidgetTypeId SLIVER_TO_BOX_ADAPTER_TYPE =
            new WidgetTypeId("flutter.widgets.SliverToBoxAdapter");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 0;
    public static final int SLOT_COUNT = 1;
    public static final String SLIVER_TRAIT = CustomScrollViewWidgetPropertySchema.SLIVER_TRAIT;

    private SliverToBoxAdapterWidgetPropertySchema() { }

    public static Map<String, String> definitions() {
        return Collections.unmodifiableMap(new LinkedHashMap<>());
    }

    public static Optional<String> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.empty();
    }
}
