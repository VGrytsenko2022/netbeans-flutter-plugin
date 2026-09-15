package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Declarative constraint for one persisted property-value kind. */
public sealed interface PropertyValueConstraint permits
        PropertyValueConstraint.AnyValue,
        PropertyValueConstraint.StringLength,
        PropertyValueConstraint.StringPattern,
        PropertyValueConstraint.IconDataValues,
        PropertyValueConstraint.MaterialIconValues,
        PropertyValueConstraint.ThemeTokenValues,
        PropertyValueConstraint.PaintValues,
        PropertyValueConstraint.ShadowListValues,
        PropertyValueConstraint.FontVariationListValues,
        PropertyValueConstraint.AlignmentGeometryValues,
        PropertyValueConstraint.AlignmentValues,
        PropertyValueConstraint.OffsetValues,
        PropertyValueConstraint.SizeValues,
        PropertyValueConstraint.BoxConstraintsValues,
        PropertyValueConstraint.Matrix4Values,
        PropertyValueConstraint.ImageProviderValues,
        PropertyValueConstraint.BorderRadiusValues,
        PropertyValueConstraint.ShapeBorderClipperValues,
        PropertyValueConstraint.DartObjectReferenceValues,
        PropertyValueConstraint.BoxDecorationValues,
        PropertyValueConstraint.GradientValues,
        PropertyValueConstraint.IntegerRange,
        PropertyValueConstraint.DoubleRange,
        PropertyValueConstraint.EnumValues,
        PropertyValueConstraint.EdgeInsetsValues,
        PropertyValueConstraint.CallbackReference {

    PropertyValueKind kind();

    boolean accepts(PropertyValue value);

    String description();

    /** Accepts the complete schema-level domain of a value kind. */
    record AnyValue(PropertyValueKind kind) implements PropertyValueConstraint {
        public AnyValue {
            Objects.requireNonNull(kind, "kind");
            if (kind == PropertyValueKind.INTEGER
                    || kind == PropertyValueKind.DOUBLE
                    || kind == PropertyValueKind.ENUM
                    || kind == PropertyValueKind.EDGE_INSETS
                    || kind == PropertyValueKind.THEME_TOKEN
                    || kind == PropertyValueKind.PAINT
                    || kind == PropertyValueKind.SHADOW_LIST
                    || kind == PropertyValueKind.FONT_VARIATION_LIST
                    || kind == PropertyValueKind.ALIGNMENT_GEOMETRY
                    || kind == PropertyValueKind.OFFSET
                    || kind == PropertyValueKind.SIZE
                    || kind == PropertyValueKind.BOX_CONSTRAINTS
                    || kind == PropertyValueKind.MATRIX4
                    || kind == PropertyValueKind.IMAGE_PROVIDER
                    || kind == PropertyValueKind.BORDER_RADIUS
                    || kind == PropertyValueKind.SHAPE_BORDER_CLIPPER
                    || kind == PropertyValueKind.DART_OBJECT_REFERENCE
                    || kind == PropertyValueKind.GRADIENT
                    || kind == PropertyValueKind.BOX_DECORATION
                    || kind == PropertyValueKind.ICON_DATA
                    || kind == PropertyValueKind.CALLBACK) {
                throw new IllegalArgumentException(
                        kind.wireName() + " requires a typed catalog constraint");
            }
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value != null && value.kind() == kind;
        }

        @Override
        public String description() {
            return kind.wireName();
        }
    }

    /** Accepts theme tokens from one reviewed semantic role registry. */
    record ThemeTokenValues(List<String> wireIds) implements PropertyValueConstraint {
        public ThemeTokenValues {
            Objects.requireNonNull(wireIds, "wireIds");
            wireIds = List.copyOf(wireIds);
            if (wireIds.isEmpty() || wireIds.size() != new HashSet<>(wireIds).size()) {
                throw new IllegalArgumentException("Theme token ids must be non-empty and unique");
            }
            wireIds.forEach(value -> new dev.flutter.netbeans.designer.model.ThemeToken(value));
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.THEME_TOKEN;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.ThemeTokenValue token
                    && wireIds.contains(token.token().wireId());
        }

        @Override
        public String description() {
            return "reviewed Material theme tokens " + wireIds;
        }
    }

    /** Accepts the typed Paint subset and only reviewed nested theme colors. */
    record PaintValues(List<String> colorThemeTokenIds) implements PropertyValueConstraint {
        public PaintValues {
            colorThemeTokenIds = themeTokenIds(colorThemeTokenIds);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.PAINT;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.PaintValue paint
                    && acceptsColorSource(paint.color(), colorThemeTokenIds)
                    && DartNumericLiterals.isRepresentableDouble(paint.strokeWidth())
                    && DartNumericLiterals.isRepresentableDouble(
                            paint.strokeMiterLimit())
                    && paint.maskFilter().map(mask ->
                        DartNumericLiterals.isRepresentableDouble(mask.sigma()))
                            .orElse(true);
        }

        @Override
        public String description() {
            return "typed Paint with a literal or reviewed Material theme color";
        }
    }

    /** Accepts ordered Shadows and only reviewed nested theme colors. */
    record ShadowListValues(List<String> colorThemeTokenIds)
            implements PropertyValueConstraint {
        public ShadowListValues {
            colorThemeTokenIds = themeTokenIds(colorThemeTokenIds);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.SHADOW_LIST;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.ShadowListValue shadows
                    && shadows.items().stream().allMatch(shadow ->
                        acceptsColorSource(shadow.color(), colorThemeTokenIds)
                        && DartNumericLiterals.isRepresentableDouble(shadow.offsetX())
                        && DartNumericLiterals.isRepresentableDouble(shadow.offsetY())
                        && DartNumericLiterals.isRepresentableDouble(
                                shadow.blurRadius()));
        }

        @Override
        public String description() {
            return "ordered Shadows with literal or reviewed Material theme colors";
        }
    }

    /** Accepts ordered variable-font axes whose values survive Dart-double emission. */
    record FontVariationListValues() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.FONT_VARIATION_LIST;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.FontVariationListValue variations
                    && variations.items().stream().allMatch(variation ->
                        DartNumericLiterals.isRepresentableDouble(variation.value()));
        }

        @Override
        public String description() {
            return "ordered FontVariation values exactly representable as Dart doubles";
        }
    }

    /** Accepts finite, Dart-representable physical or directional alignment coordinates. */
    record AlignmentGeometryValues() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.ALIGNMENT_GEOMETRY;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.AlignmentGeometryValue alignment
                    && DartNumericLiterals.isRepresentableDouble(alignment.horizontal())
                    && DartNumericLiterals.isRepresentableDouble(alignment.vertical());
        }

        @Override
        public String description() {
            return "physical or directional AlignmentGeometry";
        }
    }

    /** Accepts only physical Alignment, not the broader AlignmentGeometry domain. */
    record AlignmentValues() implements PropertyValueConstraint {
        @Override public PropertyValueKind kind() { return PropertyValueKind.ALIGNMENT_GEOMETRY; }
        @Override public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.AlignmentGeometryValue alignment
                    && alignment.basis() == PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL
                    && new AlignmentGeometryValues().accepts(value);
        }
        @Override public String description() { return "finite physical Alignment (not AlignmentDirectional)"; }
    }

    /** Accepts finite signed Offset coordinates exactly representable as Dart doubles. */
    record OffsetValues() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.OFFSET;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.OffsetValue offset
                    && DartNumericLiterals.isRepresentableDouble(offset.dx())
                    && DartNumericLiterals.isRepresentableDouble(offset.dy());
        }

        @Override
        public String description() {
            return "finite signed Offset coordinates";
        }
    }

    /** Accepts finite, non-negative dimensions exactly representable as Dart doubles. */
    record SizeValues() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.SIZE;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.SizeValue size
                    && size.width().signum() >= 0
                    && size.height().signum() >= 0
                    && DartNumericLiterals.isRepresentableDouble(size.width())
                    && DartNumericLiterals.isRepresentableDouble(size.height());
        }

        @Override
        public String description() {
            return "finite non-negative Size dimensions";
        }
    }

    /** Accepts normalized, Dart-representable finite or positive-infinite bounds. */
    record BoxConstraintsValues() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.BOX_CONSTRAINTS;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.BoxConstraintsValue constraints
                    && representable(constraints.minWidth())
                    && representable(constraints.maxWidth())
                    && representable(constraints.minHeight())
                    && representable(constraints.maxHeight());
        }

        @Override
        public String description() {
            return "normalized BoxConstraints with finite or positive-infinite bounds";
        }

        private static boolean representable(PropertyValue.BoxConstraintBound bound) {
            return bound.finiteValue()
                    .map(DartNumericLiterals::isRepresentableDouble)
                    .orElse(true);
        }
    }

    /** Accepts a 16-entry column-major Matrix4 that survives Dart-double emission. */
    record Matrix4Values() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.MATRIX4;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.Matrix4Value matrix
                    && matrix.storage().stream()
                            .allMatch(DartNumericLiterals::isRepresentableDouble);
        }

        @Override
        public String description() {
            return "16-entry column-major Matrix4";
        }
    }

    /** Accepts the complete reviewed asset-only ImageProvider contract. */
    record ImageProviderValues() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.IMAGE_PROVIDER;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.ImageProviderValue provider
                    && acceptsImageProvider(provider);
        }

        @Override
        public String description() {
            return "asset-only ImageProvider with optional bounded ResizeImage";
        }
    }

    /** Closed shape presets with validated radii and explicit directional resolution. */
    record ShapeBorderClipperValues() implements PropertyValueConstraint {
        @Override public PropertyValueKind kind() { return PropertyValueKind.SHAPE_BORDER_CLIPPER; }
        @Override public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.ShapeBorderClipperValue clipper
                    && new BorderRadiusValues().accepts(new PropertyValue.BorderRadiusValue(clipper.borderRadius()));
        }
        @Override public String description() {
            return "ShapeBorderClipper preset with finite non-negative radii and explicit directional resolution";
        }
    }

    /** Typed finite non-negative radii, optionally accepting directional geometry. */
    record BorderRadiusValues(boolean directionalAllowed) implements PropertyValueConstraint {
        /** Preserves the historical BorderRadiusGeometry-capable contract. */
        public BorderRadiusValues() {
            this(true);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.BORDER_RADIUS;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (!(value instanceof PropertyValue.BorderRadiusValue borderRadius)) {
                return false;
            }
            PropertyValue.BoxDecorationValue.BorderRadiusGeometry geometry =
                    borderRadius.geometry();
            if (geometry
                    instanceof PropertyValue.BoxDecorationValue.PhysicalBorderRadius physical) {
                return acceptsRadius(physical.topLeft())
                        && acceptsRadius(physical.topRight())
                        && acceptsRadius(physical.bottomRight())
                        && acceptsRadius(physical.bottomLeft());
            }
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius directional =
                    (PropertyValue.BoxDecorationValue.DirectionalBorderRadius) geometry;
            return directionalAllowed && acceptsRadius(directional.topStart())
                    && acceptsRadius(directional.topEnd())
                    && acceptsRadius(directional.bottomEnd())
                    && acceptsRadius(directional.bottomStart());
        }

        @Override
        public String description() {
            return directionalAllowed
                    ? "physical or directional finite non-negative BorderRadiusGeometry"
                    : "physical finite non-negative BorderRadius (directional geometry is not accepted)";
        }

        private static boolean acceptsRadius(
                PropertyValue.BoxDecorationValue.Radius radius) {
            return DartNumericLiterals.isRepresentableDouble(radius.x())
                    && DartNumericLiterals.isRepresentableDouble(radius.y());
        }
    }

    /**
     * Accepts a closed project-Dart object reference whose assignability to
     * the expected Dart type is proved by candidate analysis before save.
     */
    record DartObjectReferenceValues(String expectedDartType)
            implements PropertyValueConstraint {
        private static final Pattern EXPECTED_TYPE = Pattern.compile(
                "(?:[A-Za-z][A-Za-z0-9_]*(?:<[A-Za-z][A-Za-z0-9_]*\\??>)?|(?:Object|FocusNode|AnimationStyle|Duration|Curve|ShapeBorder|IconThemeData|TextStyle|TextHeightBehavior|BorderRadius|SystemUiOverlayStyle|AlignmentGeometry|Color|Decoration|BoxConstraints|Matrix4|int|double|EdgeInsetsGeometry)\\?)");

        public DartObjectReferenceValues {
            Objects.requireNonNull(expectedDartType, "expectedDartType");
            if (expectedDartType.length() > 128
                    || !EXPECTED_TYPE.matcher(expectedDartType).matches()
                    && !ScaffoldWidgetPropertySchema.BOTTOM_SHEET_SCRIM_BUILDER_TYPE.equals(expectedDartType)
                    && !Set.of(TextFieldWidgetPropertySchema.INPUT_COUNTER_BUILDER_TYPE,
                            TextFieldWidgetPropertySchema.CONTEXT_MENU_BUILDER_TYPE,
                            ListViewWidgetPropertySchema.ITEM_EXTENT_BUILDER_TYPE, "ChildIndexGetter?", "VoidCallback?", "ValueNotifier<EdgeInsets>?", "ImageErrorWidgetBuilder?", "Image?", "Rect?", "BackdropKey?", "Animation<double>?", "CustomPainter?", "Key?", "LocalKey?", "TableBorder?", "Map<int, TableColumnWidth>?").contains(expectedDartType)) {
                throw new IllegalArgumentException(
                        "Expected Dart type must use the closed simple/generic form");
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.DART_OBJECT_REFERENCE;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.DartObjectReferenceValue;
        }

        @Override
        public String description() {
            return "project Dart reference or zero-argument invocation assignable to "
                    + expectedDartType;
        }
    }

    /** All three structured gradient families, finite geometry and reviewed colors. */
    record GradientValues(List<String> colorThemeTokenIds) implements PropertyValueConstraint {
        public GradientValues { colorThemeTokenIds = themeTokenIds(colorThemeTokenIds); }
        @Override public PropertyValueKind kind() { return PropertyValueKind.GRADIENT; }
        @Override public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.GradientValue gradient
                    && new BoxDecorationValues(colorThemeTokenIds).acceptsGradient(gradient.gradient());
        }
        @Override public String description() { return "Linear, radial or sweep gradient with typed stops, directionality, tiling and rotation"; }
    }

    /** Accepts the reviewed BoxDecoration subset and theme colors. */
    record BoxDecorationValues(List<String> colorThemeTokenIds)
            implements PropertyValueConstraint {
        public BoxDecorationValues {
            colorThemeTokenIds = themeTokenIds(colorThemeTokenIds);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.BOX_DECORATION;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (!(value instanceof PropertyValue.BoxDecorationValue decoration)) {
                return false;
            }
            return decoration.color().map(this::acceptsColor).orElse(true)
                    && decoration.image().map(this::acceptsDecorationImage).orElse(true)
                    && decoration.border().map(this::acceptsBorder).orElse(true)
                    && decoration.borderRadius().map(this::acceptsRadiusGeometry).orElse(true)
                    && decoration.boxShadow().stream().allMatch(this::acceptsShadow)
                    && decoration.gradient().map(this::acceptsGradient).orElse(true);
        }

        @Override
        public String description() {
            return "BoxDecoration with typed asset images and literal or reviewed Material theme colors";
        }

        private boolean acceptsColor(ColorSource source) {
            return acceptsColorSource(source, colorThemeTokenIds);
        }

        private boolean acceptsDecorationImage(
                PropertyValue.DecorationImageValue image) {
            return acceptsImageProvider(image.image())
                    && acceptsAlignment(image.alignment())
                    && image.centerSlice().map(rect ->
                            DartNumericLiterals.isRepresentableDouble(rect.left())
                            && DartNumericLiterals.isRepresentableDouble(rect.top())
                            && DartNumericLiterals.isRepresentableDouble(rect.right())
                            && DartNumericLiterals.isRepresentableDouble(rect.bottom()))
                            .orElse(true)
                    && DartNumericLiterals.isRepresentableDouble(image.scale())
                    && DartNumericLiterals.isRepresentableDouble(image.opacity())
                    && image.colorFilter().map(this::acceptsColorFilter).orElse(true);
        }

        private boolean acceptsColorFilter(
                PropertyValue.DecorationImageValue.ColorFilter filter) {
            if (filter instanceof PropertyValue.DecorationImageValue.Mode mode) {
                return acceptsColor(mode.color());
            }
            if (filter instanceof PropertyValue.DecorationImageValue.Matrix matrix) {
                return matrix.values().stream()
                        .allMatch(DartNumericLiterals::isRepresentableDouble);
            }
            if (filter instanceof PropertyValue.DecorationImageValue.Saturation saturation) {
                return DartNumericLiterals.isRepresentableDouble(saturation.value());
            }
            return true;
        }

        private boolean acceptsBorder(PropertyValue.BoxDecorationValue.BoxBorder border) {
            if (border instanceof PropertyValue.BoxDecorationValue.PhysicalBorder physical) {
                return acceptsSide(physical.top()) && acceptsSide(physical.right())
                        && acceptsSide(physical.bottom()) && acceptsSide(physical.left());
            }
            PropertyValue.BoxDecorationValue.DirectionalBorder directional =
                    (PropertyValue.BoxDecorationValue.DirectionalBorder) border;
            return acceptsSide(directional.top()) && acceptsSide(directional.start())
                    && acceptsSide(directional.end()) && acceptsSide(directional.bottom());
        }

        private boolean acceptsSide(PropertyValue.BoxDecorationValue.BorderSide side) {
            return acceptsColor(side.color())
                    && DartNumericLiterals.isRepresentableDouble(side.width())
                    && DartNumericLiterals.isRepresentableDouble(side.strokeAlign());
        }

        private boolean acceptsRadiusGeometry(
                PropertyValue.BoxDecorationValue.BorderRadiusGeometry geometry) {
            if (geometry instanceof PropertyValue.BoxDecorationValue.PhysicalBorderRadius physical) {
                return acceptsRadius(physical.topLeft()) && acceptsRadius(physical.topRight())
                        && acceptsRadius(physical.bottomRight()) && acceptsRadius(physical.bottomLeft());
            }
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius directional =
                    (PropertyValue.BoxDecorationValue.DirectionalBorderRadius) geometry;
            return acceptsRadius(directional.topStart()) && acceptsRadius(directional.topEnd())
                    && acceptsRadius(directional.bottomEnd())
                    && acceptsRadius(directional.bottomStart());
        }

        private boolean acceptsRadius(PropertyValue.BoxDecorationValue.Radius radius) {
            return DartNumericLiterals.isRepresentableDouble(radius.x())
                    && DartNumericLiterals.isRepresentableDouble(radius.y());
        }

        private boolean acceptsShadow(PropertyValue.BoxDecorationValue.BoxShadow shadow) {
            return acceptsColor(shadow.color())
                    && DartNumericLiterals.isRepresentableDouble(shadow.offsetX())
                    && DartNumericLiterals.isRepresentableDouble(shadow.offsetY())
                    && DartNumericLiterals.isRepresentableDouble(shadow.blurRadius())
                    && DartNumericLiterals.isRepresentableDouble(shadow.spreadRadius());
        }

        private boolean acceptsGradient(PropertyValue.BoxDecorationValue.BoxGradient gradient) {
            boolean common = gradient.stops().stream().allMatch(stop ->
                    acceptsColor(stop.color())
                    && DartNumericLiterals.isRepresentableDouble(stop.stop()))
                    && gradient.rotationRadians()
                            .map(DartNumericLiterals::isRepresentableDouble).orElse(true);
            if (!common) {
                return false;
            }
            if (gradient instanceof PropertyValue.BoxDecorationValue.LinearGradient linear) {
                return acceptsAlignment(linear.begin()) && acceptsAlignment(linear.end());
            }
            if (gradient instanceof PropertyValue.BoxDecorationValue.RadialGradient radial) {
                return acceptsAlignment(radial.center())
                        && DartNumericLiterals.isRepresentableDouble(radial.radius())
                        && radial.focal().map(this::acceptsAlignment).orElse(true)
                        && DartNumericLiterals.isRepresentableDouble(radial.focalRadius());
            }
            PropertyValue.BoxDecorationValue.SweepGradient sweep =
                    (PropertyValue.BoxDecorationValue.SweepGradient) gradient;
            return acceptsAlignment(sweep.center())
                    && DartNumericLiterals.isRepresentableDouble(sweep.startAngle())
                    && DartNumericLiterals.isRepresentableDouble(sweep.endAngle());
        }

        private boolean acceptsAlignment(PropertyValue.AlignmentGeometryValue alignment) {
            return DartNumericLiterals.isRepresentableDouble(alignment.horizontal())
                    && DartNumericLiterals.isRepresentableDouble(alignment.vertical());
        }
    }

    private static boolean acceptsImageProvider(
            PropertyValue.ImageProviderValue provider) {
        return provider.exactScale()
                .map(DartNumericLiterals::isRepresentableDouble)
                .orElse(true);
    }

    private static List<String> themeTokenIds(List<String> values) {
        Objects.requireNonNull(values, "colorThemeTokenIds");
        List<String> copied = List.copyOf(values);
        if (copied.isEmpty() || copied.size() != new HashSet<>(copied).size()) {
            throw new IllegalArgumentException("Theme token ids must be non-empty and unique");
        }
        copied.forEach(value -> new dev.flutter.netbeans.designer.model.ThemeToken(value));
        return copied;
    }

    private static boolean acceptsColorSource(ColorSource source, List<String> tokens) {
        return switch (source) {
            case ColorSource.Literal ignored -> true;
            case ColorSource.Theme theme -> tokens.contains(theme.token().wireId());
        };
    }

    record StringLength(int minimum, int maximum) implements PropertyValueConstraint {
        public StringLength {
            if (minimum < 0 || maximum < minimum) {
                throw new IllegalArgumentException("Invalid string length range");
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.STRING;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (!(value instanceof PropertyValue.StringValue stringValue)) {
                return false;
            }
            int length = stringValue.value().codePointCount(0, stringValue.value().length());
            return length >= minimum && length <= maximum;
        }

        @Override
        public String description() {
            return "string length " + minimum + ".." + maximum;
        }
    }

    /** Accepts strings that fully match a catalog-owned regular expression. */
    record StringPattern(String regularExpression, String description)
            implements PropertyValueConstraint {
        public StringPattern {
            Objects.requireNonNull(regularExpression, "regularExpression");
            Objects.requireNonNull(description, "description");
            if (regularExpression.isBlank()) {
                throw new IllegalArgumentException("String pattern must not be blank");
            }
            if (description.isBlank()) {
                throw new IllegalArgumentException("String pattern description must not be blank");
            }
            try {
                Pattern.compile(regularExpression);
            } catch (PatternSyntaxException failure) {
                throw new IllegalArgumentException("Invalid string pattern", failure);
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.STRING;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.StringValue stringValue
                    && Pattern.matches(regularExpression, stringValue.value());
        }
    }

    /** Accepts the complete validated nullable IconData metadata value. */
    record IconDataValues() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.ICON_DATA;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.IconDataValue;
        }

        @Override
        public String description() {
            return "typed nullable IconData metadata";
        }
    }

    /** Accepts None or an exact glyph from the bundled reviewed Material registry. */
    record MaterialIconValues() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.ICON_DATA;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (!(value instanceof PropertyValue.IconDataValue iconData)) {
                return false;
            }
            if (iconData.equals(PropertyValue.IconDataValue.none())) {
                return true;
            }
            MaterialIconRegistry registry = MaterialIconRegistry.bundled();
            if (!iconData.fontFamily().equals(Optional.of(
                    registry.metadata().fontFamily()))
                    || iconData.fontPackage().isPresent()
                    || !iconData.fontFamilyFallback().isEmpty()) {
                return false;
            }
            int codePoint = iconData.codePoint().orElseThrow();
            return registry.entries().stream().anyMatch(candidate ->
                    candidate.codePoint() == codePoint
                    && candidate.matchTextDirection()
                    == iconData.matchTextDirection());
        }

        @Override
        public String description() {
            MaterialIconRegistry.SourceMetadata metadata =
                    MaterialIconRegistry.bundled().metadata();
            return "None or a bundled Flutter " + metadata.flutterVersion()
                    + " Material Icons glyph; project requirement "
                    + metadata.projectRequirement();
        }
    }

    /** Null bounds mean unbounded. */
    record IntegerRange(BigInteger minimum, BigInteger maximum) implements PropertyValueConstraint {
        public IntegerRange {
            if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
                throw new IllegalArgumentException("Integer minimum exceeds maximum");
            }
            if ((minimum != null && !DartNumericLiterals.isPortableInteger(minimum))
                    || (maximum != null && !DartNumericLiterals.isPortableInteger(maximum))) {
                throw new IllegalArgumentException(
                        "Integer range bounds must be portable Dart integer literals");
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.INTEGER;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (!(value instanceof PropertyValue.IntegerValue integerValue)) {
                return false;
            }
            return DartNumericLiterals.isPortableInteger(integerValue.value())
                    && (minimum == null || integerValue.value().compareTo(minimum) >= 0)
                    && (maximum == null || integerValue.value().compareTo(maximum) <= 0);
        }

        @Override
        public String description() {
            return "integer range " + (minimum == null ? "-infinity" : minimum)
                    + ".." + (maximum == null ? "+infinity" : maximum);
        }
    }

    /** Null bounds mean unbounded. */
    record DoubleRange(
            BigDecimal minimum,
            boolean minimumInclusive,
            BigDecimal maximum,
            boolean maximumInclusive) implements PropertyValueConstraint {

        public DoubleRange {
            if ((minimum != null && !DartNumericLiterals.isRepresentableDouble(minimum))
                    || (maximum != null && !DartNumericLiterals.isRepresentableDouble(maximum))) {
                throw new IllegalArgumentException(
                        "Double range bounds must be representable Dart double literals");
            }
            if (minimum != null && maximum != null) {
                int comparison = minimum.compareTo(maximum);
                if (comparison > 0 || (comparison == 0 && (!minimumInclusive || !maximumInclusive))) {
                    throw new IllegalArgumentException("Invalid double range");
                }
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.DOUBLE;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (!(value instanceof PropertyValue.DoubleValue doubleValue)) {
                return false;
            }
            if (!DartNumericLiterals.isRepresentableDouble(doubleValue.value())) {
                return false;
            }
            if (minimum != null) {
                int comparison = doubleValue.value().compareTo(minimum);
                if (comparison < 0 || (comparison == 0 && !minimumInclusive)) {
                    return false;
                }
            }
            if (maximum != null) {
                int comparison = doubleValue.value().compareTo(maximum);
                if (comparison > 0 || (comparison == 0 && !maximumInclusive)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String description() {
            return "double range " + (minimumInclusive ? '[' : '(')
                    + (minimum == null ? "-infinity" : minimum) + ','
                    + (maximum == null ? "+infinity" : maximum)
                    + (maximumInclusive ? ']' : ')');
        }
    }

    record EnumValues(DartSymbolReference dartType, List<String> values) implements PropertyValueConstraint {
        public EnumValues {
            Objects.requireNonNull(dartType, "dartType");
            Objects.requireNonNull(values, "values");
            values = List.copyOf(values);
            if (values.isEmpty() || values.size() != new HashSet<>(values).size()) {
                throw new IllegalArgumentException("Enum values must be non-empty and unique");
            }
            for (String value : values) {
                DartIdentifiers.requirePublicIdentifier(value, "Dart enum value");
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.ENUM;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.EnumValue enumValue
                    && dartType.name().equals(enumValue.type())
                    && values.contains(enumValue.value());
        }

        @Override
        public String description() {
            return dartType.name() + values;
        }
    }

    record CallbackReference() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.CALLBACK;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.CallbackValue callback
                    && DartIdentifiers.isIdentifier(callback.handler());
        }

        @Override
        public String description() {
            return "Dart callback identifier";
        }
    }

    /**
     * Typed EdgeInsets constraint.
     *
     * <p>{@code directionalAllowed} distinguishes APIs accepting the broad
     * {@code EdgeInsetsGeometry} surface from APIs such as {@code SafeArea}
     * whose public parameter is the concrete physical {@code EdgeInsets}
     * type.</p>
     */
    record EdgeInsetsValues(
            boolean nonNegative,
            boolean directionalAllowed) implements PropertyValueConstraint {

        /** Preserves the historical EdgeInsetsGeometry-capable contract. */
        public EdgeInsetsValues(boolean nonNegative) {
            this(nonNegative, true);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.EDGE_INSETS;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (value instanceof PropertyValue.EdgeInsetsValue edgeInsets) {
                return acceptsSides(
                        edgeInsets.left(), edgeInsets.top(),
                        edgeInsets.right(), edgeInsets.bottom());
            }
            if (directionalAllowed
                    && value instanceof PropertyValue.EdgeInsetsDirectionalValue edgeInsets) {
                return acceptsSides(
                        edgeInsets.start(), edgeInsets.top(),
                        edgeInsets.end(), edgeInsets.bottom());
            }
            return false;
        }

        private boolean acceptsSides(
                BigDecimal first,
                BigDecimal top,
                BigDecimal third,
                BigDecimal bottom) {
            return DartNumericLiterals.isRepresentableDouble(first)
                    && DartNumericLiterals.isRepresentableDouble(top)
                    && DartNumericLiterals.isRepresentableDouble(third)
                    && DartNumericLiterals.isRepresentableDouble(bottom)
                    && (!nonNegative
                    || (first.signum() >= 0
                    && top.signum() >= 0
                    && third.signum() >= 0
                    && bottom.signum() >= 0));
        }

        @Override
        public String description() {
            if (directionalAllowed) {
                return nonNegative ? "non-negative edge insets" : "edge insets";
            }
            return nonNegative
                    ? "non-negative physical edge insets"
                    : "physical edge insets";
        }
    }
}
