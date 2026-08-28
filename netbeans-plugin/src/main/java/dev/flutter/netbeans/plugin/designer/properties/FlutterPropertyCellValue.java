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
public record FlutterPropertyCellValue(Optional<PropertyValue> explicitValue) {
    public static final String NOT_SET_TEXT = "<not set>";

    public FlutterPropertyCellValue {
        explicitValue = Objects.requireNonNull(explicitValue, "explicitValue");
    }

    public static FlutterPropertyCellValue explicit(PropertyValue value) {
        return new FlutterPropertyCellValue(Optional.of(
                Objects.requireNonNull(value, "value")));
    }

    public static FlutterPropertyCellValue unset() {
        return new FlutterPropertyCellValue(Optional.empty());
    }

    public boolean isExplicit() {
        return explicitValue.isPresent();
    }
}
