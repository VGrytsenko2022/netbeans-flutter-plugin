package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.model.PropertyValue;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable Property Sheet value for one explicit Flutter constructor argument.
 *
 * <p>The wrapper deliberately keeps {@code unset} separate from every concrete
 * schema value. In particular, an empty string and {@code false} remain real
 * explicit values and are never confused with an absent constructor argument.</p>
 */
public record FlutterPropertyCellValue(Optional<PropertyValue> explicitValue, Optional<RadioTypeEdit> radioTypeEdit) {
    public FlutterPropertyCellValue(Optional<PropertyValue> explicitValue) {
        this(explicitValue, Optional.empty());
    }
    public static final String NOT_SET_TEXT = "<not set>";

    public FlutterPropertyCellValue {
        explicitValue = Objects.requireNonNull(explicitValue, "explicitValue");
        radioTypeEdit = Objects.requireNonNull(radioTypeEdit, "radioTypeEdit");
        if (radioTypeEdit.isPresent() && !explicitValue.equals(radioTypeEdit.orElseThrow().requested().get("valueType"))) {
            throw new IllegalArgumentException("Dependent type transaction must match the displayed type.");
        }
    }

    public static FlutterPropertyCellValue explicit(PropertyValue value) {
        return new FlutterPropertyCellValue(Optional.of(
                Objects.requireNonNull(value, "value")));
    }

    public static FlutterPropertyCellValue unset() {
        return new FlutterPropertyCellValue(Optional.empty());
    }

    /** UI-only closed type transaction; legacy Radio name also serves ValueListenableBuilder. Never a model/wire value or arbitrary property patch. */
    public record RadioTypeEdit(
            io.github.vgrytsenko2022.designer.model.WidgetTypeId widgetType,
            io.github.vgrytsenko2022.designer.model.StableId widgetId,
            java.util.Map<String, Optional<PropertyValue>> baseline,
            java.util.Map<String, Optional<PropertyValue>> requested) {
        public static final java.util.List<String> FIELDS = java.util.List.of("valueType", "nullableValueType", "value", "groupValue");
        public static final java.util.List<String> TWEEN_FIELDS = java.util.List.of("valueType", "nullableValueType", "tween", "builder");
        public static final java.util.List<String> VALUE_LISTENABLE_FIELDS = java.util.List.of("valueType", "nullableValueType", "valueListenable", "builder");
        public static final java.util.List<String> GROUP_FIELDS = java.util.List.of("valueType", "nullableValueType", "groupValue");
        public static final java.util.List<String> TILE_FIELDS = java.util.List.of("valueType", "nullableValueType", "value", "groupValue", "onChanged");
        public RadioTypeEdit(io.github.vgrytsenko2022.designer.model.StableId widgetId,
                java.util.Map<String, Optional<PropertyValue>> baseline,
                java.util.Map<String, Optional<PropertyValue>> requested) {
            this(io.github.vgrytsenko2022.designer.catalog.RadioWidgetPropertySchema.RADIO_TYPE, widgetId, baseline, requested);
        }
        public RadioTypeEdit {
            Objects.requireNonNull(widgetType, "widgetType");
            Objects.requireNonNull(widgetId, "widgetId");
            baseline = java.util.Map.copyOf(baseline);
            requested = java.util.Map.copyOf(requested);
            var allowed = java.util.Set.copyOf(fields(widgetType));
            if (io.github.vgrytsenko2022.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(widgetType)
                    && (requested.getOrDefault("tween", Optional.empty()).isEmpty() || requested.getOrDefault("builder", Optional.empty()).isEmpty()))
                throw new IllegalArgumentException("TweenAnimationBuilder tween and builder are required.");
            if (io.github.vgrytsenko2022.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(widgetType)
                    && (requested.getOrDefault("valueListenable", Optional.empty()).isEmpty() || requested.getOrDefault("builder", Optional.empty()).isEmpty()))
                throw new IllegalArgumentException("ValueListenableBuilder source and builder are required.");
            if (!baseline.keySet().equals(allowed) || !requested.keySet().equals(allowed)
                    || requested.get("valueType").isEmpty() || (allowed.contains("value") && requested.get("value").isEmpty())) {
                throw new IllegalArgumentException("A type edit contains exactly its reviewed type, nullability and dependent fields; required fields cannot be omitted.");
            }
        }
        public static java.util.List<String> fields(io.github.vgrytsenko2022.designer.model.WidgetTypeId type) {
            if (io.github.vgrytsenko2022.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(type)) return TWEEN_FIELDS;
            if (io.github.vgrytsenko2022.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(type)) return VALUE_LISTENABLE_FIELDS;
            if (io.github.vgrytsenko2022.designer.catalog.RadioWidgetPropertySchema.RADIO_TYPE.equals(type)) return FIELDS;
            if (io.github.vgrytsenko2022.designer.catalog.RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.equals(type)) return GROUP_FIELDS;
            if (io.github.vgrytsenko2022.designer.catalog.RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE.equals(type)) return TILE_FIELDS;
            throw new IllegalArgumentException("Dependent type edits are restricted to Radio, RadioGroup, RadioListTile ValueListenableBuilder and TweenAnimationBuilder.");
        }
        public static boolean supports(io.github.vgrytsenko2022.designer.model.WidgetTypeId type) {
            return io.github.vgrytsenko2022.designer.catalog.RadioWidgetPropertySchema.RADIO_TYPE.equals(type)
                    || io.github.vgrytsenko2022.designer.catalog.RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.equals(type)
                    || io.github.vgrytsenko2022.designer.catalog.RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE.equals(type)
                    || io.github.vgrytsenko2022.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(type)
                    || io.github.vgrytsenko2022.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(type);
        }
        public static java.util.Map<String, Optional<PropertyValue>> snapshot(io.github.vgrytsenko2022.designer.model.WidgetNode widget) {
            var result = new java.util.LinkedHashMap<String, Optional<PropertyValue>>();
            fields(widget.type()).forEach(name -> result.put(name, Optional.ofNullable(widget.properties().get(new io.github.vgrytsenko2022.designer.model.PropertyName(name)))));
            return java.util.Map.copyOf(result);
        }
    }

    public boolean isExplicit() {
        return explicitValue.isPresent();
    }
}
