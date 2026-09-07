package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyValue;
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
            throw new IllegalArgumentException("Radio/RadioGroup type transaction must match the displayed type.");
        }
    }

    public static FlutterPropertyCellValue explicit(PropertyValue value) {
        return new FlutterPropertyCellValue(Optional.of(
                Objects.requireNonNull(value, "value")));
    }

    public static FlutterPropertyCellValue unset() {
        return new FlutterPropertyCellValue(Optional.empty());
    }

    /** UI-only, immutable dependent edit; never a model/wire value or arbitrary property patch. */
    public record RadioTypeEdit(
            dev.flutter.netbeans.designer.model.WidgetTypeId widgetType,
            dev.flutter.netbeans.designer.model.StableId widgetId,
            java.util.Map<String, Optional<PropertyValue>> baseline,
            java.util.Map<String, Optional<PropertyValue>> requested) {
        public static final java.util.List<String> FIELDS = java.util.List.of("valueType", "nullableValueType", "value", "groupValue");
        public static final java.util.List<String> GROUP_FIELDS = java.util.List.of("valueType", "nullableValueType", "groupValue");
        public RadioTypeEdit(dev.flutter.netbeans.designer.model.StableId widgetId,
                java.util.Map<String, Optional<PropertyValue>> baseline,
                java.util.Map<String, Optional<PropertyValue>> requested) {
            this(dev.flutter.netbeans.designer.catalog.RadioWidgetPropertySchema.RADIO_TYPE, widgetId, baseline, requested);
        }
        public RadioTypeEdit {
            Objects.requireNonNull(widgetType, "widgetType");
            Objects.requireNonNull(widgetId, "widgetId");
            baseline = java.util.Map.copyOf(baseline);
            requested = java.util.Map.copyOf(requested);
            var allowed = java.util.Set.copyOf(fields(widgetType));
            if (!baseline.keySet().equals(allowed) || !requested.keySet().equals(allowed)
                    || requested.get("valueType").isEmpty() || (allowed.contains("value") && requested.get("value").isEmpty())) {
                throw new IllegalArgumentException("A Radio/RadioGroup type edit contains exactly its type, nullability and dependent value fields; required fields cannot be omitted.");
            }
        }
        public static java.util.List<String> fields(dev.flutter.netbeans.designer.model.WidgetTypeId type) {
            if (dev.flutter.netbeans.designer.catalog.RadioWidgetPropertySchema.RADIO_TYPE.equals(type)) return FIELDS;
            if (dev.flutter.netbeans.designer.catalog.RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.equals(type)) return GROUP_FIELDS;
            throw new IllegalArgumentException("Dependent type edits are restricted to Radio and RadioGroup.");
        }
        public static boolean supports(dev.flutter.netbeans.designer.model.WidgetTypeId type) {
            return dev.flutter.netbeans.designer.catalog.RadioWidgetPropertySchema.RADIO_TYPE.equals(type)
                    || dev.flutter.netbeans.designer.catalog.RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.equals(type);
        }
        public static java.util.Map<String, Optional<PropertyValue>> snapshot(dev.flutter.netbeans.designer.model.WidgetNode widget) {
            var result = new java.util.LinkedHashMap<String, Optional<PropertyValue>>();
            fields(widget.type()).forEach(name -> result.put(name, Optional.ofNullable(widget.properties().get(new dev.flutter.netbeans.designer.model.PropertyName(name)))));
            return java.util.Map.copyOf(result);
        }
    }

    public boolean isExplicit() {
        return explicitValue.isPresent();
    }
}
