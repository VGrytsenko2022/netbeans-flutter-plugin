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
            throw new IllegalArgumentException("Radio type transaction must match the displayed type.");
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
            dev.flutter.netbeans.designer.model.StableId widgetId,
            java.util.Map<String, Optional<PropertyValue>> baseline,
            java.util.Map<String, Optional<PropertyValue>> requested) {
        public static final java.util.List<String> FIELDS = java.util.List.of("valueType", "nullableValueType", "value", "groupValue");
        public RadioTypeEdit {
            Objects.requireNonNull(widgetId, "widgetId");
            baseline = java.util.Map.copyOf(baseline);
            requested = java.util.Map.copyOf(requested);
            var allowed = java.util.Set.copyOf(FIELDS);
            if (!baseline.keySet().equals(allowed) || !requested.keySet().equals(allowed)
                    || requested.get("valueType").isEmpty() || requested.get("value").isEmpty()) {
                throw new IllegalArgumentException("A Radio type edit contains exactly type, nullability, Value and Group value; required fields cannot be omitted.");
            }
        }
        public static java.util.Map<String, Optional<PropertyValue>> snapshot(dev.flutter.netbeans.designer.model.WidgetNode widget) {
            var result = new java.util.LinkedHashMap<String, Optional<PropertyValue>>();
            FIELDS.forEach(name -> result.put(name, Optional.ofNullable(widget.properties().get(new dev.flutter.netbeans.designer.model.PropertyName(name)))));
            return java.util.Map.copyOf(result);
        }
    }

    public boolean isExplicit() {
        return explicitValue.isPresent();
    }
}
