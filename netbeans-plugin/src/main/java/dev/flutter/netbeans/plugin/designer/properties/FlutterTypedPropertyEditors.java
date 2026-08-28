package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Catalog-shape admission and editors for safe schema-v1 property values. */
final class FlutterTypedPropertyEditors {
    private FlutterTypedPropertyEditors() {
    }

    static Optional<Binding> binding(PropertyDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        Set<PropertyValueKind> kinds = definition.acceptedKinds();
        EditorKind editorKind;
        if (kinds.equals(EnumSet.of(PropertyValueKind.STRING))) {
            editorKind = EditorKind.STRING;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.BOOLEAN))) {
            editorKind = EditorKind.BOOLEAN;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.INTEGER))) {
            editorKind = EditorKind.INTEGER;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.DOUBLE))) {
            editorKind = EditorKind.DOUBLE;
        } else if (kinds.equals(EnumSet.of(
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE))) {
            editorKind = EditorKind.NUMBER;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.ENUM))
                && definition.constraints().getFirst()
                        instanceof PropertyValueConstraint.EnumValues) {
            editorKind = EditorKind.ENUM;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.EDGE_INSETS))
                && definition.constraints().getFirst()
                        instanceof PropertyValueConstraint.EdgeInsetsValues) {
            editorKind = EditorKind.EDGE_INSETS;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.COLOR))) {
            editorKind = EditorKind.COLOR;
        } else {
            return Optional.empty();
        }
        return Optional.of(new Binding(definition, editorKind));
    }

    enum EditorKind {
        STRING,
        BOOLEAN,
        INTEGER,
        DOUBLE,
        NUMBER,
        ENUM,
        EDGE_INSETS,
        COLOR
    }

    record Binding(PropertyDefinition definition, EditorKind editorKind) {
        Binding {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(editorKind, "editorKind");
        }

        boolean optional() {
            return !definition.parameter().required();
        }

        FlutterPropertyCellValue validate(FlutterPropertyCellValue candidate) {
            Objects.requireNonNull(candidate, "candidate");
            if (candidate.explicitValue().isEmpty()) {
                if (!optional()) {
                    throw invalid("Required property '" + definition.name().value()
                            + "' cannot be unset.");
                }
                return candidate;
            }
            PropertyValue value = candidate.explicitValue().orElseThrow();
            Optional<PropertyValueConstraint> constraint = definition.constraints().stream()
                    .filter(item -> item.kind() == value.kind())
                    .findFirst();
            if (constraint.isEmpty() || !constraint.orElseThrow().accepts(value)) {
                String accepted = definition.constraints().stream()
                        .map(PropertyValueConstraint::description)
                        .reduce((left, right) -> left + "; " + right)
                        .orElse("no accepted values");
                throw invalid("Property '" + definition.name().value()
                        + "' expects " + accepted + ".");
            }
            return candidate;
        }

        PropertyEditor createEditor() {
            return switch (editorKind) {
                case STRING -> new StringEditor(this);
                case BOOLEAN -> new BooleanEditor(this);
                case INTEGER -> new IntegerEditor(this);
                case DOUBLE -> new DoubleEditor(this);
                case NUMBER -> new NumberEditor(this);
                case ENUM -> new CatalogEnumEditor(this);
                case EDGE_INSETS -> new EdgeInsetsEditor(this);
                case COLOR -> new ColorEditor(this);
            };
        }

        private static IllegalArgumentException invalid(String message) {
            return new IllegalArgumentException(message);
        }
    }

    private abstract static class TypedEditor extends PropertyEditorSupport
            implements ExPropertyEditor {
        final Binding binding;
        private PropertyEnv environment;
        private Component customEditor;

        TypedEditor(Binding binding) {
            this.binding = Objects.requireNonNull(binding, "binding");
            super.setValue(FlutterPropertyCellValue.unset());
        }

        @Override
        public final void setValue(Object value) {
            if (!(value instanceof FlutterPropertyCellValue candidate)) {
                throw new IllegalArgumentException(
                        "Flutter property editor requires FlutterPropertyCellValue.");
            }
            super.setValue(binding.validate(candidate));
        }

        final FlutterPropertyCellValue cellValue() {
            return (FlutterPropertyCellValue) getValue();
        }

        final Optional<PropertyValue> explicitValue() {
            return cellValue().explicitValue();
        }

        final boolean parseUnset(String text) {
            String normalized = Objects.requireNonNull(text, "text").strip();
            if (binding.optional()
                    && (normalized.isEmpty()
                    || FlutterPropertyCellValue.NOT_SET_TEXT.equals(normalized))) {
                setValue(FlutterPropertyCellValue.unset());
                return true;
            }
            return false;
        }

        final String unsetText() {
            return FlutterPropertyCellValue.NOT_SET_TEXT;
        }

        final void setExplicit(PropertyValue value) {
            setValue(FlutterPropertyCellValue.explicit(value));
        }

        @Override
        public final void attachEnv(PropertyEnv environment) {
            this.environment = Objects.requireNonNull(environment, "environment");
            customEditor = null;
            FlutterPropertyEditorComponents.inplaceFactory(binding)
                    .ifPresent(environment::registerInplaceEditorFactory);
        }

        @Override
        public final boolean supportsCustomEditor() {
            return FlutterPropertyEditorComponents.supportsCustomEditor(binding);
        }

        @Override
        public final Component getCustomEditor() {
            if (!supportsCustomEditor()) {
                return null;
            }
            if (customEditor == null) {
                customEditor = FlutterPropertyEditorComponents.customEditor(
                        this, binding, environment);
            }
            return customEditor;
        }

        @Override
        public final boolean isPaintable() {
            return binding.editorKind() == EditorKind.COLOR;
        }

        @Override
        public final void paintValue(Graphics graphics, Rectangle box) {
            if (binding.editorKind() == EditorKind.COLOR) {
                FlutterPropertyEditorComponents.paintColorValue(
                        graphics, box, cellValue());
            } else {
                super.paintValue(graphics, box);
            }
        }
    }

    private static final class StringEditor extends TypedEditor {
        StringEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.StringValue.class::cast)
                    .map(PropertyValue.StringValue::value)
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            Objects.requireNonNull(text, "text");
            // Every String, including the presentation token and empty text,
            // is a valid explicit value. Optional String reset is deliberately
            // available only through Node.Property.restoreDefaultValue().
            setExplicit(new PropertyValue.StringValue(text));
        }
    }

    private static final class BooleanEditor extends TypedEditor {
        private final String[] tags;

        BooleanEditor(Binding binding) {
            super(binding);
            tags = binding.optional()
                    ? new String[]{FlutterPropertyCellValue.NOT_SET_TEXT, "true", "false"}
                    : new String[]{"true", "false"};
        }

        @Override
        public String[] getTags() {
            return tags.clone();
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.BooleanValue.class::cast)
                    .map(value -> Boolean.toString(value.value()))
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            Objects.requireNonNull(text, "text");
            if (parseUnset(text)) {
                return;
            }
            if ("true".equalsIgnoreCase(text.strip())) {
                setExplicit(new PropertyValue.BooleanValue(true));
            } else if ("false".equalsIgnoreCase(text.strip())) {
                setExplicit(new PropertyValue.BooleanValue(false));
            } else {
                throw new IllegalArgumentException("Expected true or false.");
            }
        }
    }

    private static final class IntegerEditor extends TypedEditor {
        IntegerEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.IntegerValue.class::cast)
                    .map(value -> value.value().toString())
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            try {
                setExplicit(new PropertyValue.IntegerValue(
                        new BigInteger(text.strip())));
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException("Expected a whole number.", failure);
            }
        }
    }

    private static final class DoubleEditor extends TypedEditor {
        DoubleEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.DoubleValue.class::cast)
                    .map(value -> value.value().toPlainString())
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            try {
                setExplicit(new PropertyValue.DoubleValue(
                        new BigDecimal(text.strip())));
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException("Expected a decimal number.", failure);
            }
        }
    }

    private static final class NumberEditor extends TypedEditor {
        NumberEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue().map(value -> switch (value) {
                case PropertyValue.IntegerValue integer -> integer.value().toString();
                case PropertyValue.DoubleValue decimal -> decimal.value().toPlainString();
                default -> throw new IllegalStateException(
                        "Unexpected numeric value " + value.kind());
            }).orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            String normalized = text.strip();
            try {
                if (normalized.indexOf('.') >= 0
                        || normalized.indexOf('e') >= 0
                        || normalized.indexOf('E') >= 0) {
                    setExplicit(new PropertyValue.DoubleValue(
                            new BigDecimal(normalized)));
                } else {
                    setExplicit(new PropertyValue.IntegerValue(
                            new BigInteger(normalized)));
                }
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException("Expected a number.", failure);
            }
        }
    }

    private static final class CatalogEnumEditor extends TypedEditor {
        private final PropertyValueConstraint.EnumValues constraint;
        private final String[] tags;

        CatalogEnumEditor(Binding binding) {
            super(binding);
            constraint = binding.definition().constraints().stream()
                    .filter(PropertyValueConstraint.EnumValues.class::isInstance)
                    .map(PropertyValueConstraint.EnumValues.class::cast)
                    .findFirst()
                    .orElseThrow();
            ArrayList<String> values = new ArrayList<>();
            if (binding.optional()) {
                values.add(FlutterPropertyCellValue.NOT_SET_TEXT);
            }
            values.addAll(constraint.values());
            tags = values.toArray(String[]::new);
        }

        @Override
        public String[] getTags() {
            return tags.clone();
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.EnumValue.class::cast)
                    .map(PropertyValue.EnumValue::value)
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            Objects.requireNonNull(text, "text");
            if (parseUnset(text)) {
                return;
            }
            String normalized = text.strip();
            if (!constraint.values().contains(normalized)) {
                throw new IllegalArgumentException(
                        "Expected one of " + constraint.values() + ".");
            }
            setExplicit(new PropertyValue.EnumValue(
                    constraint.dartType().name(), normalized));
        }
    }

    private static final class EdgeInsetsEditor extends TypedEditor {
        EdgeInsetsEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.EdgeInsetsValue.class::cast)
                    .map(value -> number(value.left()) + ", "
                            + number(value.top()) + ", "
                            + number(value.right()) + ", "
                            + number(value.bottom()))
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            List<String> parts = Arrays.stream(text.split(",", -1))
                    .map(String::strip)
                    .toList();
            if (parts.size() != 1 && parts.size() != 4) {
                throw new IllegalArgumentException(
                        "Expected one value or left, top, right, bottom.");
            }
            try {
                BigDecimal left;
                BigDecimal top;
                BigDecimal right;
                BigDecimal bottom;
                if (parts.size() == 1) {
                    left = new BigDecimal(parts.getFirst());
                    top = left;
                    right = left;
                    bottom = left;
                } else {
                    left = new BigDecimal(parts.get(0));
                    top = new BigDecimal(parts.get(1));
                    right = new BigDecimal(parts.get(2));
                    bottom = new BigDecimal(parts.get(3));
                }
                setExplicit(new PropertyValue.EdgeInsetsValue(
                        left, top, right, bottom));
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException(
                        "Edge insets must contain decimal numbers.", failure);
            }
        }

        private static String number(BigDecimal value) {
            return value.toPlainString();
        }
    }

    private static final class ColorEditor extends TypedEditor {
        ColorEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.ColorValue.class::cast)
                    .map(PropertyValue.ColorValue::wireArgb)
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            String normalized = text.strip();
            if (normalized.length() >= 2
                    && normalized.substring(0, 2).equalsIgnoreCase("0x")) {
                normalized = "0x" + normalized.substring(2)
                        .toUpperCase(java.util.Locale.ROOT);
            }
            setExplicit(PropertyValue.ColorValue.fromWireArgb(normalized));
        }
    }
}
