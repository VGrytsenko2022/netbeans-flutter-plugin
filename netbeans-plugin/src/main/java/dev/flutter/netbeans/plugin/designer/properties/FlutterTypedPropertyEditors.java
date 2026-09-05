package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.ThemeToken;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Catalog-shape admission and editors for safe versioned .fd property values. */
final class FlutterTypedPropertyEditors {
    private FlutterTypedPropertyEditors() {
    }

    static Optional<Binding> binding(PropertyDefinition definition) {
        return binding(definition, Optional.empty(), false, List.of());
    }

    static Optional<Binding> binding(
            PropertyDefinition definition,
            Optional<TextWidgetPropertySchema.Definition> textSchema) {
        return binding(definition, textSchema, false, List.of());
    }

    static Optional<Binding> binding(
            PropertyDefinition definition,
            Optional<TextWidgetPropertySchema.Definition> textSchema,
            boolean newlineStringList,
            List<String> stringPresets) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(textSchema, "textSchema");
        Objects.requireNonNull(stringPresets, "stringPresets");
        List<String> presets = stringPresets.stream()
                .map(value -> Objects.requireNonNull(value, "stringPresets contains null"))
                .map(String::strip)
                .toList();
        if (presets.stream().anyMatch(String::isEmpty)
                || presets.size() != presets.stream().distinct().count()) {
            throw new IllegalArgumentException(
                    "String editor presets must be non-blank and unique.");
        }
        Set<PropertyValueKind> kinds = definition.acceptedKinds();
        EditorKind editorKind;
        if (kinds.equals(EnumSet.of(PropertyValueKind.STRING))) {
            if (!presets.isEmpty()) {
                editorKind = EditorKind.STRING_PRESET;
            } else {
                editorKind = textSchema
                    .filter(value -> value.encoding()
                    == TextWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST)
                    .map(ignored -> EditorKind.NEWLINE_STRING_LIST)
                    .orElse(newlineStringList
                            ? EditorKind.NEWLINE_STRING_LIST : EditorKind.STRING);
            }
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.BOOLEAN))) {
            editorKind = EditorKind.BOOLEAN;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.INTEGER))) {
            editorKind = EditorKind.INTEGER;
        } else if (kinds.equals(EnumSet.of(
                PropertyValueKind.INTEGER, PropertyValueKind.NULL))) {
            editorKind = EditorKind.NULLABLE_INTEGER;
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
        } else if (kinds.equals(EnumSet.of(
                PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.ThemeTokenValues.class::isInstance)) {
            editorKind = EditorKind.THEME_COLOR;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.THEME_TOKEN))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.ThemeTokenValues.class::isInstance)) {
            editorKind = EditorKind.THEME_TOKEN;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.CALLBACK))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.CallbackReference.class::isInstance)) {
            editorKind = EditorKind.CALLBACK;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.PAINT))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.PaintValues.class::isInstance)) {
            editorKind = EditorKind.PAINT;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.SHADOW_LIST))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.ShadowListValues.class::isInstance)) {
            editorKind = EditorKind.SHADOW_LIST;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.FONT_FEATURE_LIST))) {
            editorKind = EditorKind.FONT_FEATURE_LIST;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.FONT_VARIATION_LIST))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.FontVariationListValues.class::isInstance)) {
            editorKind = EditorKind.FONT_VARIATION_LIST;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.ICON_DATA))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.MaterialIconValues.class::isInstance)) {
            editorKind = EditorKind.ICON_DATA;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.ALIGNMENT_GEOMETRY))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.AlignmentGeometryValues.class::isInstance)) {
            editorKind = EditorKind.ALIGNMENT_GEOMETRY;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.SIZE))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.SizeValues.class::isInstance)) {
            editorKind = EditorKind.SIZE;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.OFFSET))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.OffsetValues.class::isInstance)) {
            editorKind = EditorKind.OFFSET;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.BOX_CONSTRAINTS))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.BoxConstraintsValues.class::isInstance)) {
            editorKind = EditorKind.BOX_CONSTRAINTS;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.MATRIX4))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.Matrix4Values.class::isInstance)) {
            editorKind = EditorKind.MATRIX4;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.IMAGE_PROVIDER))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.ImageProviderValues.class::isInstance)) {
            editorKind = EditorKind.IMAGE_PROVIDER;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.IMAGE_PROVIDER, PropertyValueKind.NULL))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.ImageProviderValues.class::isInstance)) {
            editorKind = EditorKind.NULLABLE_IMAGE_PROVIDER;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.BOX_DECORATION))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.BoxDecorationValues.class::isInstance)) {
            editorKind = EditorKind.BOX_DECORATION;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.BORDER_RADIUS))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.BorderRadiusValues.class::isInstance)) {
            editorKind = EditorKind.BORDER_RADIUS;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.SHAPE_BORDER_CLIPPER,
                PropertyValueKind.DART_OBJECT_REFERENCE))) {
            editorKind = EditorKind.SHAPE_BORDER_CLIPPER;
        } else if (kinds.equals(EnumSet.of(PropertyValueKind.DART_OBJECT_REFERENCE))
                && definition.constraints().stream().anyMatch(
                        PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)) {
            editorKind = EditorKind.DART_OBJECT_REFERENCE;
        } else {
            return Optional.empty();
        }
        return Optional.of(new Binding(
                definition, editorKind, textSchema, List.copyOf(presets)));
    }

    enum EditorKind {
        STRING,
        STRING_PRESET,
        NEWLINE_STRING_LIST,
        BOOLEAN,
        INTEGER,
        NULLABLE_INTEGER,
        DOUBLE,
        NUMBER,
        ENUM,
        EDGE_INSETS,
        COLOR,
        THEME_COLOR,
        THEME_TOKEN,
        CALLBACK,
        PAINT,
        SHADOW_LIST,
        FONT_FEATURE_LIST,
        FONT_VARIATION_LIST,
        ICON_DATA,
        ALIGNMENT_GEOMETRY,
        SIZE,
        OFFSET,
        BOX_CONSTRAINTS,
        MATRIX4,
        IMAGE_PROVIDER,
        NULLABLE_IMAGE_PROVIDER,
        BOX_DECORATION,
        BORDER_RADIUS,
        DART_OBJECT_REFERENCE,
        SHAPE_BORDER_CLIPPER
    }

    record Binding(
            PropertyDefinition definition,
            EditorKind editorKind,
            Optional<TextWidgetPropertySchema.Definition> textSchema,
            List<String> stringPresets) {
        Binding {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(editorKind, "editorKind");
            Objects.requireNonNull(textSchema, "textSchema");
            stringPresets = List.copyOf(
                    Objects.requireNonNull(stringPresets, "stringPresets"));
            if ((editorKind == EditorKind.STRING_PRESET) != !stringPresets.isEmpty()) {
                throw new IllegalArgumentException(
                        "Only the String preset editor may carry preset values.");
            }
        }

        boolean optional() {
            return !definition.parameter().required();
        }

        boolean directionalEdgeInsetsAllowed() {
            if (editorKind != EditorKind.EDGE_INSETS) {
                throw new IllegalStateException(
                        "Directional EdgeInsets capability is only defined for EdgeInsets editors.");
            }
            return definition.constraints().stream()
                    .filter(PropertyValueConstraint.EdgeInsetsValues.class::isInstance)
                    .map(PropertyValueConstraint.EdgeInsetsValues.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "EdgeInsets editor is missing its typed constraint."))
                    .directionalAllowed();
        }

        boolean directionalBorderRadiusAllowed() {
            if (editorKind != EditorKind.BORDER_RADIUS) {
                throw new IllegalStateException(
                        "Directional BorderRadius capability is only defined for radius editors.");
            }
            return definition.constraints().stream()
                    .filter(PropertyValueConstraint.BorderRadiusValues.class::isInstance)
                    .map(PropertyValueConstraint.BorderRadiusValues.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "BorderRadius editor is missing its typed constraint."))
                    .directionalAllowed();
        }

        List<ThemeToken> allowedThemeTokens() {
            return definition.constraints().stream()
                    .filter(PropertyValueConstraint.ThemeTokenValues.class::isInstance)
                    .map(PropertyValueConstraint.ThemeTokenValues.class::cast)
                    .flatMap(value -> value.wireIds().stream())
                    .distinct().map(ThemeToken::new).toList();
        }

        List<ThemeToken> allowedColorSourceTokens() {
            return definition.constraints().stream().flatMap(constraint -> switch (constraint) {
                case PropertyValueConstraint.ThemeTokenValues values ->
                    values.wireIds().stream();
                case PropertyValueConstraint.PaintValues values ->
                    values.colorThemeTokenIds().stream();
                case PropertyValueConstraint.ShadowListValues values ->
                    values.colorThemeTokenIds().stream();
                case PropertyValueConstraint.BoxDecorationValues values ->
                    values.colorThemeTokenIds().stream();
                default -> java.util.stream.Stream.empty();
            }).distinct().map(ThemeToken::new)
                    .filter(ThemeToken::isColorSchemeToken).toList();
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
                case STRING_PRESET -> new StringPresetEditor(this);
                case NEWLINE_STRING_LIST -> new NewlineStringListEditor(this);
                case BOOLEAN -> new BooleanEditor(this);
                case INTEGER -> new IntegerEditor(this);
                case NULLABLE_INTEGER -> new NullableIntegerEditor(this);
                case DOUBLE -> new DoubleEditor(this);
                case NUMBER -> new NumberEditor(this);
                case ENUM -> new CatalogEnumEditor(this);
                case EDGE_INSETS -> new EdgeInsetsEditor(this);
                case COLOR -> new ColorEditor(this);
                case THEME_COLOR -> new ThemeColorEditor(this);
                case THEME_TOKEN -> new ThemeTokenEditor(this);
                case CALLBACK -> new CallbackEditor(this);
                case PAINT, SHADOW_LIST, FONT_FEATURE_LIST, FONT_VARIATION_LIST,
                        ICON_DATA, ALIGNMENT_GEOMETRY, SIZE, OFFSET, BOX_CONSTRAINTS,
                        MATRIX4, IMAGE_PROVIDER, NULLABLE_IMAGE_PROVIDER, BOX_DECORATION, BORDER_RADIUS,
                        DART_OBJECT_REFERENCE, SHAPE_BORDER_CLIPPER ->
                    new StructuredEditor(this);
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
            return binding.editorKind() == EditorKind.BOOLEAN
                    || binding.editorKind() == EditorKind.COLOR
                    || binding.editorKind() == EditorKind.THEME_COLOR
                    || FlutterPropertyValuePreview.isPaintable(
                            binding.editorKind());
        }

        @Override
        public final void paintValue(Graphics graphics, Rectangle box) {
            if (binding.editorKind() == EditorKind.BOOLEAN) {
                FlutterPropertyEditorComponents.paintBooleanValue(
                        graphics, box, cellValue());
            } else if (binding.editorKind() == EditorKind.COLOR
                    || binding.editorKind() == EditorKind.THEME_COLOR) {
                FlutterPropertyEditorComponents.paintColorValue(
                        graphics, box, cellValue());
            } else if (FlutterPropertyValuePreview.isPaintable(
                    binding.editorKind())) {
                FlutterPropertyValuePreview.paintValue(
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

    private static final class StringPresetEditor extends TypedEditor {
        private final String[] tags;

        StringPresetEditor(Binding binding) {
            super(binding);
            List<String> values = new ArrayList<>();
            if (binding.optional()) {
                values.add(FlutterPropertyCellValue.NOT_SET_TEXT);
            }
            values.addAll(binding.stringPresets());
            tags = values.toArray(String[]::new);
        }

        @Override
        public String[] getTags() {
            return tags.clone();
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
            if (parseUnset(text)) {
                return;
            }
            String value = text.strip();
            if (!binding.stringPresets().contains(value)) {
                throw new IllegalArgumentException(
                        "Expected one of " + binding.stringPresets() + '.');
            }
            setExplicit(new PropertyValue.StringValue(value));
        }
    }

    /** Strict Dart handler identifier; arbitrary Dart expressions are never admitted. */
    private static final class CallbackEditor extends TypedEditor {
        CallbackEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.CallbackValue.class::cast)
                    .map(PropertyValue.CallbackValue::handler)
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            String handler = Objects.requireNonNull(text, "text").strip();
            try {
                setExplicit(new PropertyValue.CallbackValue(handler));
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException(
                        "Expected a Dart callback identifier such as onPressed or _handlePress.",
                        failure);
            }
        }
    }

    private static final class NewlineStringListEditor extends TypedEditor {
        NewlineStringListEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.StringValue.class::cast)
                    .map(PropertyValue.StringValue::value)
                    .map(FlutterTypedPropertyEditors::newlineListSummary)
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            Objects.requireNonNull(text, "text");
            setExplicit(new PropertyValue.StringValue(
                    normalizeNewlineList(text)));
        }
    }

    private static final class BooleanEditor extends TypedEditor {
        BooleanEditor(Binding binding) {
            super(binding);
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

    private static final class NullableIntegerEditor extends TypedEditor {
        NullableIntegerEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue().map(value -> switch (value) {
                case PropertyValue.NullValue ignored -> "null";
                case PropertyValue.IntegerValue integer ->
                    integer.value().toString();
                default -> throw new IllegalStateException(
                        "Unexpected nullable-integer value " + value.kind());
            }).orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            String normalized = Objects.requireNonNull(text, "text").strip();
            if ("null".equalsIgnoreCase(normalized)) {
                setExplicit(new PropertyValue.NullValue());
                return;
            }
            try {
                setExplicit(new PropertyValue.IntegerValue(
                        new BigInteger(normalized)));
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException(
                        "Expected null or a whole-number child index.", failure);
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
                boolean decimalSyntax = normalized.indexOf('.') >= 0
                        || normalized.indexOf('e') >= 0
                        || normalized.indexOf('E') >= 0;
                boolean preservesExistingDouble = explicitValue()
                        .filter(PropertyValue.DoubleValue.class::isInstance)
                        .isPresent();
                if (decimalSyntax || preservesExistingDouble) {
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
            return explicitValue().map(value -> switch (value) {
                case PropertyValue.EdgeInsetsValue physical ->
                    number(physical.left()) + ", "
                            + number(physical.top()) + ", "
                            + number(physical.right()) + ", "
                            + number(physical.bottom());
                case PropertyValue.EdgeInsetsDirectionalValue directional ->
                    "directional: " + number(directional.start()) + ", "
                            + number(directional.top()) + ", "
                            + number(directional.end()) + ", "
                            + number(directional.bottom());
                default -> throw new IllegalStateException(
                        "Expected an edge-insets property value.");
            }).orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            String normalized = Objects.requireNonNull(text, "text").strip();
            String lower = normalized.toLowerCase(Locale.ROOT);
            boolean directional = lower.startsWith("directional:");
            boolean symmetric = lower.startsWith("symmetric:");
            boolean explicitlyPhysical = lower.startsWith("physical:");
            boolean all = lower.startsWith("all:");
            if (directional || symmetric || explicitlyPhysical || all) {
                normalized = normalized.substring(normalized.indexOf(':') + 1).strip();
            }
            List<String> parts = Arrays.stream(normalized.split(",", -1))
                    .map(String::strip)
                    .toList();
            int expected = symmetric ? 2 : (all ? 1 : 4);
            if (!directional && !symmetric && !explicitlyPhysical && !all
                    && parts.size() == 1) {
                all = true;
                expected = 1;
            }
            if (parts.size() != expected) {
                throw new IllegalArgumentException(
                        "Expected all: value; symmetric: horizontal, vertical; "
                        + "physical: left, top, right, bottom; or directional: "
                        + "start, top, end, bottom.");
            }
            try {
                if (all) {
                    BigDecimal value = new BigDecimal(parts.getFirst());
                    setExplicit(new PropertyValue.EdgeInsetsValue(
                            value, value, value, value));
                    return;
                }
                if (symmetric) {
                    BigDecimal horizontal = new BigDecimal(parts.get(0));
                    BigDecimal vertical = new BigDecimal(parts.get(1));
                    setExplicit(new PropertyValue.EdgeInsetsValue(
                            horizontal, vertical, horizontal, vertical));
                    return;
                }
                BigDecimal first = new BigDecimal(parts.get(0));
                BigDecimal top = new BigDecimal(parts.get(1));
                BigDecimal third = new BigDecimal(parts.get(2));
                BigDecimal bottom = new BigDecimal(parts.get(3));
                setExplicit(directional
                        ? new PropertyValue.EdgeInsetsDirectionalValue(
                                first, top, third, bottom)
                        : new PropertyValue.EdgeInsetsValue(
                                first, top, third, bottom));
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

    /** Literal ARGB or a reviewed semantic Material ColorScheme role. */
    private static final class ThemeColorEditor extends TypedEditor {
        ThemeColorEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            return explicitValue().map(value -> switch (value) {
                case PropertyValue.ColorValue color -> color.wireArgb();
                case PropertyValue.ThemeTokenValue theme ->
                    "Theme: " + FlutterThemePropertyRoles.displayRole(theme.token());
                default -> throw new IllegalStateException(
                        "Unexpected theme-aware color " + value.kind());
            }).orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            String normalized = Objects.requireNonNull(text, "text").strip();
            if (normalized.regionMatches(true, 0, "Theme:", 0, 6)) {
                normalized = normalized.substring(6).strip();
            }
            var token = FlutterThemePropertyRoles.findColorToken(
                    normalized, binding.allowedColorSourceTokens());
            if (token.isPresent()) {
                setExplicit(new PropertyValue.ThemeTokenValue(token.orElseThrow()));
                return;
            }
            if (normalized.length() >= 2
                    && normalized.substring(0, 2).equalsIgnoreCase("0x")) {
                normalized = "0x" + normalized.substring(2)
                        .toUpperCase(java.util.Locale.ROOT);
            }
            setExplicit(PropertyValue.ColorValue.fromWireArgb(normalized));
        }
    }

    /** Closed combo of reviewed Material TextTheme roles. */
    private static final class ThemeTokenEditor extends TypedEditor {
        private final String[] tags;

        ThemeTokenEditor(Binding binding) {
            super(binding);
            ArrayList<String> values = new ArrayList<>();
            if (binding.optional()) {
                values.add(FlutterPropertyCellValue.NOT_SET_TEXT);
            }
            values.addAll(FlutterThemePropertyRoles.textStyleDisplayRoles(
                    binding.allowedThemeTokens()));
            tags = values.toArray(String[]::new);
        }

        @Override
        public String[] getTags() {
            return tags.clone();
        }

        @Override
        public String getAsText() {
            return explicitValue()
                    .map(PropertyValue.ThemeTokenValue.class::cast)
                    .map(PropertyValue.ThemeTokenValue::token)
                    .map(FlutterThemePropertyRoles::displayRole)
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            var token = FlutterThemePropertyRoles.findTextStyleToken(
                    Objects.requireNonNull(text, "text").strip(),
                    binding.allowedThemeTokens());
            if (token.isEmpty()) {
                throw new IllegalArgumentException(
                        "Expected a reviewed Material TextTheme role.");
            }
            setExplicit(new PropertyValue.ThemeTokenValue(token.orElseThrow()));
        }
    }

    /** One compact cell whose complete immutable value is edited in a dialog. */
    private static final class StructuredEditor extends TypedEditor {
        StructuredEditor(Binding binding) {
            super(binding);
        }

        @Override
        public String getAsText() {
            if (binding.editorKind() == EditorKind.NULLABLE_IMAGE_PROVIDER
                    && explicitValue().orElse(null) instanceof PropertyValue.NullValue) {
                return "None (empty image icon)";
            }
            return explicitValue().map(PropertyValueFormatter::format)
                    .orElseGet(this::unsetText);
        }

        @Override
        public void setAsText(String text) {
            if (parseUnset(text)) {
                return;
            }
            throw new IllegalArgumentException(
                    "Use the custom editor button to edit this structured value.");
        }
    }

    static String normalizeNewlineList(String text) {
        Objects.requireNonNull(text, "text");
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String separator = normalized.indexOf('\n') >= 0 ? "\n" : ",";
        return Arrays.stream(normalized.split(separator, -1))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private static String newlineListSummary(String value) {
        return value.lines()
                .map(String::strip)
                .filter(item -> !item.isEmpty())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }
}
