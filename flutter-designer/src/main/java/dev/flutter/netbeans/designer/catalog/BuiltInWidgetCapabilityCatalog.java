package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Immutable fail-closed capability gate for the reviewed built-in widgets.
 *
 * <p>This is the single Java-side source for Palette admission, writable
 * Properties and native Canvas projection. A contributed or altered definition
 * cannot acquire a capability merely by reusing a built-in type id.</p>
 */
public final class BuiltInWidgetCapabilityCatalog {
    private static final Set<PropertyValueKind> CANVAS_VALUE_KINDS =
            Collections.unmodifiableSet(EnumSet.of(
                    PropertyValueKind.STRING,
                    PropertyValueKind.BOOLEAN,
                    PropertyValueKind.INTEGER,
                    PropertyValueKind.DOUBLE,
                    PropertyValueKind.ENUM,
                    PropertyValueKind.COLOR,
                    PropertyValueKind.EDGE_INSETS,
                    PropertyValueKind.THEME_TOKEN,
                    PropertyValueKind.PAINT,
                    PropertyValueKind.SHADOW_LIST,
                    PropertyValueKind.FONT_FEATURE_LIST,
                    PropertyValueKind.FONT_VARIATION_LIST));
    private static final Set<PropertyValueKind> NUMERIC_SCHEMA_KINDS =
            Collections.unmodifiableSet(EnumSet.of(
                    PropertyValueKind.INTEGER,
                    PropertyValueKind.DOUBLE,
                    PropertyValueKind.EDGE_INSETS));
    private static final String WIDGETS_LIBRARY = "package:flutter/widgets.dart";
    private static final List<String> REVIEWED_COLOR_THEME_TOKENS = List.of(
            "material.colorScheme.primary",
            "material.colorScheme.onPrimary",
            "material.colorScheme.primaryContainer",
            "material.colorScheme.onPrimaryContainer",
            "material.colorScheme.primaryFixed",
            "material.colorScheme.primaryFixedDim",
            "material.colorScheme.onPrimaryFixed",
            "material.colorScheme.onPrimaryFixedVariant",
            "material.colorScheme.secondary",
            "material.colorScheme.onSecondary",
            "material.colorScheme.secondaryContainer",
            "material.colorScheme.onSecondaryContainer",
            "material.colorScheme.secondaryFixed",
            "material.colorScheme.secondaryFixedDim",
            "material.colorScheme.onSecondaryFixed",
            "material.colorScheme.onSecondaryFixedVariant",
            "material.colorScheme.tertiary",
            "material.colorScheme.onTertiary",
            "material.colorScheme.tertiaryContainer",
            "material.colorScheme.onTertiaryContainer",
            "material.colorScheme.tertiaryFixed",
            "material.colorScheme.tertiaryFixedDim",
            "material.colorScheme.onTertiaryFixed",
            "material.colorScheme.onTertiaryFixedVariant",
            "material.colorScheme.error",
            "material.colorScheme.onError",
            "material.colorScheme.errorContainer",
            "material.colorScheme.onErrorContainer",
            "material.colorScheme.surface",
            "material.colorScheme.onSurface",
            "material.colorScheme.surfaceDim",
            "material.colorScheme.surfaceBright",
            "material.colorScheme.surfaceContainerLowest",
            "material.colorScheme.surfaceContainerLow",
            "material.colorScheme.surfaceContainer",
            "material.colorScheme.surfaceContainerHigh",
            "material.colorScheme.surfaceContainerHighest",
            "material.colorScheme.onSurfaceVariant",
            "material.colorScheme.outline",
            "material.colorScheme.outlineVariant",
            "material.colorScheme.shadow",
            "material.colorScheme.scrim",
            "material.colorScheme.inverseSurface",
            "material.colorScheme.onInverseSurface",
            "material.colorScheme.inversePrimary",
            "material.colorScheme.surfaceTint");
    private static final List<String> REVIEWED_TEXT_THEME_TOKENS = List.of(
            "material.textTheme.displayLarge",
            "material.textTheme.displayMedium",
            "material.textTheme.displaySmall",
            "material.textTheme.headlineLarge",
            "material.textTheme.headlineMedium",
            "material.textTheme.headlineSmall",
            "material.textTheme.titleLarge",
            "material.textTheme.titleMedium",
            "material.textTheme.titleSmall",
            "material.textTheme.bodyLarge",
            "material.textTheme.bodyMedium",
            "material.textTheme.bodySmall",
            "material.textTheme.labelLarge",
            "material.textTheme.labelMedium",
            "material.textTheme.labelSmall");

    private static final Set<WidgetCapability> STATIC_EDITABLE = capabilities(
            WidgetCapability.PROPERTIES,
            WidgetCapability.CANVAS,
            WidgetCapability.CREATE,
            WidgetCapability.DND);
    private static final Set<WidgetCapability> STATIC_STRUCTURAL = capabilities(
            WidgetCapability.CANVAS,
            WidgetCapability.CREATE,
            WidgetCapability.DND);

    private static final Map<String, Set<WidgetCapability>> CAPABILITIES = Map.ofEntries(
            Map.entry("flutter.material.Scaffold", STATIC_STRUCTURAL),
            Map.entry("flutter.material.AppBar", Set.of()),
            Map.entry("flutter.material.ElevatedButton", Set.of()),
            Map.entry("flutter.widgets.Column", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Row", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Padding", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Center", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Text", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Icon", Set.of()),
            Map.entry("flutter.widgets.SizedBox", STATIC_EDITABLE));

    private static final CanvasNumericBounds UNBOUNDED_NUMERIC =
            bounds(null, true, null, true);
    private static final CanvasNumericBounds NON_NEGATIVE_NUMERIC =
            bounds(BigDecimal.ZERO, true, null, true);
    private static final CanvasNumericBounds POSITIVE_NUMERIC =
            bounds(BigDecimal.ZERO, false, null, true);
    private static final CanvasNumericBounds NON_NEGATIVE_PORTABLE_INTEGER =
            bounds(
                    BigDecimal.ZERO,
                    true,
                    new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER),
                    true);
    private static final CanvasNumericBounds POSITIVE_PORTABLE_INTEGER =
            bounds(
                    BigDecimal.ONE,
                    true,
                    new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER),
                    true);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            NON_NEGATIVE_NUMBER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER, NON_NEGATIVE_PORTABLE_INTEGER,
                    PropertyValueKind.DOUBLE, NON_NEGATIVE_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            NON_NEGATIVE_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE, NON_NEGATIVE_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            POSITIVE_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE, POSITIVE_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            UNBOUNDED_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE, UNBOUNDED_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            POSITIVE_INTEGER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER, POSITIVE_PORTABLE_INTEGER);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            NON_NEGATIVE_EDGE_INSETS_BOUNDS = Map.of(
                    PropertyValueKind.EDGE_INSETS, NON_NEGATIVE_NUMERIC);

    /*
     * Independent Canvas-wire contract. This deliberate schema duplication is
     * the review gate between the extensible Java catalog and the isolated Dart
     * runner: every below-type property and slot field is explicit, so a bound,
     * required/default value or cardinality change fails until both sides are
     * reviewed together.
     */
    private static final Map<String, CanvasProjection> CANVAS_PROJECTIONS = Map.ofEntries(
            Map.entry("flutter.material.Scaffold", projection(Map.of(
                    "backgroundColor", propertySchema(PropertyValueKind.COLOR),
                    "resizeToAvoidBottomInset", propertySchema(PropertyValueKind.BOOLEAN)),
                    Map.of(
                            "appBar", singleSlotSchema(false, 0),
                            "body", singleSlotSchema(false, 0),
                            "floatingActionButton", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Column", flexProjection()),
            Map.entry("flutter.widgets.Row", flexProjection()),
            Map.entry("flutter.widgets.Padding", projection(Map.of(
                    "padding", requiredDefaultEdgeInsetsSchema(
                            "edgeInsets:16,16,16,16",
                            true)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Center", projection(Map.of(
                    "widthFactor", numericSchema(
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    "heightFactor", numericSchema(
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.SizedBox", projection(Map.of(
                    "width", numericSchema(
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    "height", numericSchema(
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Text", textProjection()));

    static {
        validateCatalogParity();
    }

    private BuiltInWidgetCapabilityCatalog() {
    }

    /** Returns the reviewed capabilities of one exact canonical definition. */
    public static Set<WidgetCapability> capabilities(WidgetDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (!isExactBuiltInDefinition(definition)) {
            return Set.of();
        }
        return CAPABILITIES.getOrDefault(definition.typeId().value(), Set.of());
    }

    /** Returns whether one exact canonical definition has the requested capability. */
    public static boolean supports(
            WidgetDefinition definition,
            WidgetCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return capabilities(definition).contains(capability);
    }

    /** Returns canonical built-ins carrying one capability in deterministic Palette order. */
    public static List<WidgetDefinition> definitionsSupporting(WidgetCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return BuiltInWidgetCatalog.getDefault().paletteDefinitions().stream()
                .filter(definition -> supports(definition, capability))
                .toList();
    }

    /**
     * Returns the independent reviewed Canvas-wire schema for one exact
     * canonical definition. The explicit schema is intentionally separate from
     * the extensible catalog and is parity-checked during class initialization.
     */
    public static Optional<CanvasProjection> canvasProjection(
            WidgetDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (!supports(definition, WidgetCapability.CANVAS)) {
            return Optional.empty();
        }
        return Optional.of(CANVAS_PROJECTIONS.get(definition.typeId().value()));
    }

    private static boolean isExactBuiltInDefinition(WidgetDefinition definition) {
        return BuiltInWidgetCatalog.getDefault().find(definition.typeId())
                .filter(definition::equals)
                .isPresent();
    }

    private static Set<WidgetCapability> capabilities(WidgetCapability... values) {
        return Set.of(values);
    }

    private static void validateCatalogParity() {
        Set<String> catalogTypes = BuiltInWidgetCatalog.getDefault().definitions().stream()
                .map(definition -> definition.typeId().value())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!CAPABILITIES.keySet().equals(catalogTypes)) {
            TreeSet<String> missing = new TreeSet<>(catalogTypes);
            missing.removeAll(CAPABILITIES.keySet());
            TreeSet<String> unknown = new TreeSet<>(CAPABILITIES.keySet());
            unknown.removeAll(catalogTypes);
            throw new ExceptionInInitializerError(
                    "Built-in widget capability parity failed; missing=" + missing
                    + ", unknown=" + unknown);
        }
        for (WidgetDefinition definition : BuiltInWidgetCatalog.getDefault().definitions()) {
            Set<WidgetCapability> supported = CAPABILITIES.get(
                    definition.typeId().value());
            requireDependencies(definition, supported);
            validateCanvasProjection(definition, supported);
        }
    }

    private static void requireDependencies(
            WidgetDefinition definition,
            Set<WidgetCapability> supported) {
        EnumSet<WidgetCapability> required = EnumSet.noneOf(WidgetCapability.class);
        if (supported.contains(WidgetCapability.CREATE)) {
            required.add(WidgetCapability.CANVAS);
        }
        if (supported.contains(WidgetCapability.DND)) {
            required.add(WidgetCapability.CREATE);
            required.add(WidgetCapability.CANVAS);
        }
        if (!supported.containsAll(required)) {
            required.removeAll(supported);
            throw new ExceptionInInitializerError(
                    "Widget capability dependencies are missing for "
                    + definition.typeId().value() + ": " + required);
        }
    }

    private static void validateCanvasProjection(
            WidgetDefinition definition,
            Set<WidgetCapability> supported) {
        CanvasProjection declared = CANVAS_PROJECTIONS.get(
                definition.typeId().value());
        if (!supported.contains(WidgetCapability.CANVAS)) {
            if (declared != null) {
                throw new ExceptionInInitializerError(
                        "Canvas schema exists without Canvas capability for "
                        + definition.typeId().value());
            }
            return;
        }
        if (declared == null) {
            throw new ExceptionInInitializerError(
                    "Canvas capability has no reviewed schema for "
                    + definition.typeId().value());
        }
        requireCanvasSchemaParity(definition, declared);
    }

    static void requireCanvasSchemaParity(
            WidgetDefinition definition,
            CanvasProjection declared) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(declared, "declared");
        CanvasProjection catalogProjection = catalogProjection(definition);
        if (!declared.equals(catalogProjection)) {
            throw new ExceptionInInitializerError(
                    "Canvas schema parity failed for " + definition.typeId().value()
                    + "; declared=" + declared + ", catalog=" + catalogProjection);
        }
    }

    private static CanvasProjection catalogProjection(WidgetDefinition definition) {
        LinkedHashMap<PropertyName, CanvasPropertyContract> catalogProperties =
                new LinkedHashMap<>();
        for (PropertyDefinition property : definition.properties()) {
            if (!CANVAS_VALUE_KINDS.containsAll(property.acceptedKinds())) {
                throw new ExceptionInInitializerError(
                        "Canvas schema contains an unsupported value kind on "
                        + definition.typeId().value() + '.' + property.name().value());
            }
            catalogProperties.put(property.name(), propertyContract(property));
        }
        LinkedHashMap<SlotName, CanvasSlotContract> catalogSlots =
                new LinkedHashMap<>();
        for (SlotDefinition slot : definition.slots()) {
            catalogSlots.put(slot.name(), new CanvasSlotContract(
                    slot.cardinality(),
                    slot.parameter().required(),
                    slot.minChildren(),
                    slot.maxChildren()));
        }
        return CanvasProjection.of(catalogProperties, catalogSlots);
    }

    private static CanvasProjection flexProjection() {
        return projection(Map.ofEntries(
                enumProperty(
                        "mainAxisAlignment", "MainAxisAlignment",
                        "start", "end", "center", "spaceBetween",
                        "spaceAround", "spaceEvenly"),
                enumProperty("mainAxisSize", "MainAxisSize", "min", "max"),
                enumProperty(
                        "crossAxisAlignment", "CrossAxisAlignment",
                        "start", "end", "center", "stretch", "baseline"),
                enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                enumProperty("verticalDirection", "VerticalDirection", "up", "down"),
                enumProperty(
                        "textBaseline", "TextBaseline",
                        "alphabetic", "ideographic"),
                numericProperty(
                        "spacing", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE)),
                Map.of("children", listSlotSchema(false, 0, 10_000)));
    }

    private static CanvasProjection textProjection() {
        return projection(Map.ofEntries(
                requiredDefaultProperty(
                        "data", "string:VGV4dA", PropertyValueKind.STRING),
                enumProperty(
                        "textAlign", "TextAlign",
                        "start", "end", "left", "right", "center", "justify"),
                enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                property("softWrap", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "maxLines", POSITIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                enumProperty(
                        "overflow", "TextOverflow",
                        "clip", "fade", "ellipsis", "visible"),
                property("semanticsLabel", PropertyValueKind.STRING),
                property("semanticsIdentifier", PropertyValueKind.STRING),
                enumProperty(
                        "textWidthBasis", "TextWidthBasis",
                        "parent", "longestLine"),
                colorOrThemeProperty("selectionColor"),
                stringPatternProperty(
                        "localeLanguageCode", "(?:[a-z]{2,3}|[a-z]{5,8})"),
                stringPatternProperty(
                        "localeScriptCode", "[A-Z][a-z]{3}"),
                stringPatternProperty(
                        "localeCountryCode", "(?:[A-Z]{2}|[0-9]{3})"),
                numericProperty(
                        "textScalerFactor", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("textHeightApplyFirstAscent", PropertyValueKind.BOOLEAN),
                property("textHeightApplyLastDescent", PropertyValueKind.BOOLEAN),
                enumProperty(
                        "textHeightLeadingDistribution", "TextLeadingDistribution",
                        "proportional", "even"),
                property("styleInherit", PropertyValueKind.BOOLEAN),
                themeTokenProperty("styleThemeTextStyle", REVIEWED_TEXT_THEME_TOKENS),
                colorOrThemeProperty("styleColor"),
                colorOrThemeProperty("styleBackgroundColor"),
                numericProperty(
                        "styleFontSize", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "styleFontWeight", "FontWeight",
                        "w100", "w200", "w300", "w400", "w500",
                        "w600", "w700", "w800", "w900"),
                enumProperty(
                        "styleFontStyle", "FontStyle", "normal", "italic"),
                numericProperty(
                        "styleLetterSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "styleWordSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "styleTextBaseline", "TextBaseline",
                        "alphabetic", "ideographic"),
                numericProperty(
                        "styleHeight", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "styleLeadingDistribution", "TextLeadingDistribution",
                        "proportional", "even"),
                stringPatternProperty(
                        "styleLocaleLanguageCode", "(?:[a-z]{2,3}|[a-z]{5,8})"),
                stringPatternProperty(
                        "styleLocaleScriptCode", "[A-Z][a-z]{3}"),
                stringPatternProperty(
                        "styleLocaleCountryCode", "(?:[A-Z]{2}|[0-9]{3})"),
                property("styleDecorationUnderline", PropertyValueKind.BOOLEAN),
                property("styleDecorationOverline", PropertyValueKind.BOOLEAN),
                property("styleDecorationLineThrough", PropertyValueKind.BOOLEAN),
                paintProperty("styleForeground"),
                paintProperty("styleBackground"),
                shadowProperty("styleShadows"),
                property("styleFontFeatures", PropertyValueKind.FONT_FEATURE_LIST),
                fontVariationProperty("styleFontVariations"),
                colorOrThemeProperty("styleDecorationColor"),
                enumProperty(
                        "styleDecorationStyle", "TextDecorationStyle",
                        "solid", "double", "dotted", "dashed", "wavy"),
                numericProperty(
                        "styleDecorationThickness", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("styleDebugLabel", PropertyValueKind.STRING),
                stringLengthProperty("styleFontFamily", 1, 256),
                stringLengthProperty("styleFontFamilyFallback", 0, 4096),
                stringLengthProperty("stylePackage", 1, 256),
                enumProperty(
                        "styleOverflow", "TextOverflow",
                        "clip", "fade", "ellipsis", "visible"),
                stringLengthProperty("strutFontFamily", 1, 256),
                stringLengthProperty("strutFontFamilyFallback", 0, 4096),
                numericProperty(
                        "strutFontSize", POSITIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "strutHeight", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "strutLeadingDistribution", "TextLeadingDistribution",
                        "proportional", "even"),
                numericProperty(
                        "strutLeading", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "strutFontWeight", "FontWeight",
                        "w100", "w200", "w300", "w400", "w500",
                        "w600", "w700", "w800", "w900"),
                enumProperty(
                        "strutFontStyle", "FontStyle", "normal", "italic"),
                property("strutForceHeight", PropertyValueKind.BOOLEAN),
                property("strutDebugLabel", PropertyValueKind.STRING),
                stringLengthProperty("strutPackage", 1, 256)),
                Map.of());
    }

    private static CanvasProjection projection(
            Map<String, CanvasPropertyContract> properties,
            Map<String, CanvasSlotContract> slots) {
        LinkedHashMap<PropertyName, CanvasPropertyContract> typedProperties =
                new LinkedHashMap<>();
        properties.forEach((name, contract) ->
                typedProperties.put(new PropertyName(name), contract));
        LinkedHashMap<SlotName, CanvasSlotContract> typedSlots =
                new LinkedHashMap<>();
        slots.forEach((name, contract) ->
                typedSlots.put(new SlotName(name), contract));
        return CanvasProjection.of(typedProperties, typedSlots);
    }

    private static CanvasPropertyContract propertySchema(
            PropertyValueKind... kinds) {
        return new CanvasPropertyContract(
                Set.of(kinds),
                false,
                Optional.empty(),
                Map.of(),
                anyConstraintFingerprints(kinds));
    }

    private static CanvasPropertyContract numericSchema(
            Map<PropertyValueKind, CanvasNumericBounds> numericBounds,
            PropertyValueKind... kinds) {
        return new CanvasPropertyContract(
                Set.of(kinds),
                false,
                Optional.empty(),
                numericBounds,
                rangeConstraintFingerprints(numericBounds));
    }

    private static CanvasPropertyContract requiredDefaultEdgeInsetsSchema(
            String creationDefaultFingerprint,
            boolean nonNegative) {
        CanvasNumericBounds bounds = nonNegative
                ? NON_NEGATIVE_NUMERIC
                : UNBOUNDED_NUMERIC;
        return new CanvasPropertyContract(
                Set.of(PropertyValueKind.EDGE_INSETS),
                true,
                Optional.of(creationDefaultFingerprint),
                Map.of(PropertyValueKind.EDGE_INSETS, bounds),
                Map.of(
                        PropertyValueKind.EDGE_INSETS,
                        "edgeInsets:" + (nonNegative ? '1' : '0')
                        + ':' + bounds.fingerprint()));
    }

    private static Map.Entry<String, CanvasPropertyContract> property(
            String name,
            PropertyValueKind... kinds) {
        return Map.entry(name, propertySchema(kinds));
    }

    private static Map.Entry<String, CanvasPropertyContract> enumProperty(
            String name,
            String enumType,
            String... values) {
        String fingerprint = "enum:" + base64(WIDGETS_LIBRARY) + ':'
                + enumType + ':' + java.util.Arrays.stream(values).sorted()
                        .collect(Collectors.joining(","));
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.ENUM, fingerprint));
    }

    private static Map.Entry<String, CanvasPropertyContract> stringLengthProperty(
            String name,
            int minimum,
            int maximum) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.STRING,
                "length:" + minimum + ':' + maximum));
    }

    private static Map.Entry<String, CanvasPropertyContract> stringPatternProperty(
            String name,
            String pattern) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.STRING, "pattern:" + base64(pattern)));
    }

    private static Map.Entry<String, CanvasPropertyContract> themeTokenProperty(
            String name,
            List<String> tokens) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.THEME_TOKEN,
                "tokens:" + tokens.stream().sorted()
                        .collect(Collectors.joining(","))));
    }

    private static Map.Entry<String, CanvasPropertyContract> colorOrThemeProperty(
            String name) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                false,
                Optional.empty(),
                Map.of(),
                Map.of(
                        PropertyValueKind.COLOR, "any",
                        PropertyValueKind.THEME_TOKEN,
                        "tokens:" + REVIEWED_COLOR_THEME_TOKENS.stream().sorted()
                                .collect(Collectors.joining(",")))));
    }

    private static Map.Entry<String, CanvasPropertyContract> paintProperty(
            String name) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.PAINT,
                "paintTokens:" + REVIEWED_COLOR_THEME_TOKENS.stream().sorted()
                        .collect(Collectors.joining(","))));
    }

    private static Map.Entry<String, CanvasPropertyContract> shadowProperty(
            String name) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.SHADOW_LIST,
                "shadowTokens:" + REVIEWED_COLOR_THEME_TOKENS.stream().sorted()
                        .collect(Collectors.joining(","))));
    }

    private static Map.Entry<String, CanvasPropertyContract> fontVariationProperty(
            String name) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.FONT_VARIATION_LIST, "fontVariationList"));
    }

    private static CanvasPropertyContract constrainedSchema(
            PropertyValueKind kind,
            String fingerprint) {
        return new CanvasPropertyContract(
                Set.of(kind),
                false,
                Optional.empty(),
                Map.of(),
                Map.of(kind, fingerprint));
    }

    private static Map.Entry<String, CanvasPropertyContract> numericProperty(
            String name,
            Map<PropertyValueKind, CanvasNumericBounds> numericBounds,
            PropertyValueKind... kinds) {
        return Map.entry(name, numericSchema(numericBounds, kinds));
    }

    private static Map.Entry<String, CanvasPropertyContract> requiredDefaultProperty(
            String name,
            String creationDefaultFingerprint,
            PropertyValueKind... kinds) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kinds),
                true,
                Optional.of(creationDefaultFingerprint),
                Map.of(),
                anyConstraintFingerprints(kinds)));
    }

    private static Map<PropertyValueKind, String> anyConstraintFingerprints(
            PropertyValueKind... kinds) {
        LinkedHashMap<PropertyValueKind, String> result = new LinkedHashMap<>();
        for (PropertyValueKind kind : kinds) {
            result.put(kind, "any");
        }
        return Map.copyOf(result);
    }

    private static Map<PropertyValueKind, String> rangeConstraintFingerprints(
            Map<PropertyValueKind, CanvasNumericBounds> bounds) {
        return bounds.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> "range:" + entry.getValue().fingerprint()));
    }

    private static CanvasSlotContract singleSlotSchema(
            boolean required,
            int minimumChildren) {
        return new CanvasSlotContract(
                SlotCardinality.SINGLE, required, minimumChildren, 1);
    }

    private static CanvasSlotContract listSlotSchema(
            boolean required,
            int minimumChildren,
            int maximumChildren) {
        return new CanvasSlotContract(
                SlotCardinality.LIST,
                required,
                minimumChildren,
                maximumChildren);
    }

    private static CanvasPropertyContract propertyContract(
            PropertyDefinition property) {
        LinkedHashMap<PropertyValueKind, CanvasNumericBounds> numericBounds =
                new LinkedHashMap<>();
        LinkedHashMap<PropertyValueKind, String> constraintFingerprints =
                new LinkedHashMap<>();
        for (PropertyValueConstraint constraint : property.constraints()) {
            constraintFingerprints.put(
                    constraint.kind(), constraintFingerprint(constraint));
            if (constraint instanceof PropertyValueConstraint.IntegerRange range) {
                numericBounds.put(PropertyValueKind.INTEGER, bounds(
                        decimal(range.minimum()),
                        true,
                        decimal(range.maximum()),
                        true));
            } else if (constraint instanceof PropertyValueConstraint.DoubleRange range) {
                numericBounds.put(PropertyValueKind.DOUBLE, bounds(
                        range.minimum(),
                        range.minimumInclusive(),
                        range.maximum(),
                        range.maximumInclusive()));
            } else if (constraint instanceof PropertyValueConstraint.EdgeInsetsValues edgeInsets) {
                numericBounds.put(PropertyValueKind.EDGE_INSETS,
                        edgeInsets.nonNegative()
                                ? NON_NEGATIVE_NUMERIC
                                : UNBOUNDED_NUMERIC);
            }
        }
        EnumSet<PropertyValueKind> expectedNumeric =
                EnumSet.noneOf(PropertyValueKind.class);
        expectedNumeric.addAll(property.acceptedKinds());
        expectedNumeric.retainAll(NUMERIC_SCHEMA_KINDS);
        if (!numericBounds.keySet().equals(expectedNumeric)) {
            throw new ExceptionInInitializerError(
                    "Canvas numeric schema is incomplete for "
                    + property.name().value() + "; expected=" + expectedNumeric
                    + ", actual=" + numericBounds.keySet());
        }
        return new CanvasPropertyContract(
                property.acceptedKinds(),
                property.parameter().required(),
                property.creationDefault().map(
                        BuiltInWidgetCapabilityCatalog::defaultFingerprint),
                numericBounds,
                constraintFingerprints);
    }

    private static String constraintFingerprint(
            PropertyValueConstraint constraint) {
        if (constraint instanceof PropertyValueConstraint.AnyValue) {
            return "any";
        }
        if (constraint instanceof PropertyValueConstraint.IntegerRange range) {
            return "range:" + bounds(
                    decimal(range.minimum()), true,
                    decimal(range.maximum()), true).fingerprint();
        }
        if (constraint instanceof PropertyValueConstraint.DoubleRange range) {
            return "range:" + bounds(
                    range.minimum(), range.minimumInclusive(),
                    range.maximum(), range.maximumInclusive()).fingerprint();
        }
        if (constraint instanceof PropertyValueConstraint.EdgeInsetsValues edgeInsets) {
            CanvasNumericBounds numeric = edgeInsets.nonNegative()
                    ? NON_NEGATIVE_NUMERIC
                    : UNBOUNDED_NUMERIC;
            return "edgeInsets:" + (edgeInsets.nonNegative() ? '1' : '0')
                    + ':' + numeric.fingerprint();
        }
        if (constraint instanceof PropertyValueConstraint.StringLength length) {
            return "length:" + length.minimum() + ':' + length.maximum();
        }
        if (constraint instanceof PropertyValueConstraint.StringPattern pattern) {
            return "pattern:" + base64(pattern.regularExpression());
        }
        if (constraint instanceof PropertyValueConstraint.EnumValues values) {
            return "enum:" + base64(values.dartType().libraryUri()) + ':'
                    + values.dartType().name() + ':'
                    + values.values().stream().sorted()
                            .collect(Collectors.joining(","));
        }
        if (constraint instanceof PropertyValueConstraint.ThemeTokenValues values) {
            return "tokens:" + values.wireIds().stream().sorted()
                    .collect(Collectors.joining(","));
        }
        if (constraint instanceof PropertyValueConstraint.PaintValues values) {
            return "paintTokens:" + values.colorThemeTokenIds().stream().sorted()
                    .collect(Collectors.joining(","));
        }
        if (constraint instanceof PropertyValueConstraint.ShadowListValues values) {
            return "shadowTokens:" + values.colorThemeTokenIds().stream().sorted()
                    .collect(Collectors.joining(","));
        }
        if (constraint instanceof PropertyValueConstraint.FontVariationListValues) {
            return "fontVariationList";
        }
        throw new ExceptionInInitializerError(
                "Canvas constraint requires a reviewed fingerprint: "
                + constraint.getClass().getName());
    }

    private static BigDecimal decimal(java.math.BigInteger value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static String defaultFingerprint(PropertyValue value) {
        if (value instanceof PropertyValue.StringValue string) {
            return "string:" + base64(string.value());
        }
        if (value instanceof PropertyValue.EdgeInsetsValue insets) {
            return "edgeInsets:" + decimalText(insets.left()) + ','
                    + decimalText(insets.top()) + ','
                    + decimalText(insets.right()) + ','
                    + decimalText(insets.bottom());
        }
        if (value instanceof PropertyValue.EdgeInsetsDirectionalValue insets) {
            return "edgeInsetsDirectional:" + decimalText(insets.start()) + ','
                    + decimalText(insets.top()) + ','
                    + decimalText(insets.end()) + ','
                    + decimalText(insets.bottom());
        }
        throw new ExceptionInInitializerError(
                "Canvas creation default requires a reviewed typed fingerprint for "
                + value.kind().wireName());
    }

    private static String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static CanvasNumericBounds bounds(
            BigDecimal minimum,
            boolean minimumInclusive,
            BigDecimal maximum,
            boolean maximumInclusive) {
        return new CanvasNumericBounds(
                minimum, minimumInclusive, maximum, maximumInclusive);
    }

    /**
     * Returns the deterministic exact contract mirrored by the packaged Dart
     * decoder. The representation is intentionally line-oriented so source
     * parity tests can compare it without a permissive parser.
     */
    public static String reviewedCanvasSchemaContract() {
        StringBuilder result = new StringBuilder();
        new TreeMap<>(CANVAS_PROJECTIONS).forEach((type, projection) -> {
            result.append("W|").append(type).append('\n');
            projection.propertyContracts().forEach((name, contract) -> {
                Map<String, String> wireConstraints =
                        wireConstraintFingerprints(contract);
                Map<String, CanvasNumericBounds> wireNumeric =
                        wireNumericBounds(contract);
                String kinds = String.join(",", wireConstraints.keySet());
                String numeric = wireNumeric.entrySet().stream()
                        .map(entry -> entry.getKey() + ':'
                                + entry.getValue().fingerprint())
                        .collect(Collectors.joining(";"));
                String constraints = wireConstraints.entrySet().stream()
                        .map(entry -> entry.getKey() + ':' + entry.getValue())
                        .collect(Collectors.joining(";"));
                result.append("P|").append(name.value())
                        .append('|').append(kinds)
                        .append('|').append(contract.required() ? '1' : '0')
                        .append('|').append(contract.creationDefaultFingerprint()
                                .orElse("-"))
                        .append('|').append(numeric.isEmpty() ? "-" : numeric)
                        .append('|').append(constraints)
                        .append('\n');
            });
            projection.slotContracts().forEach((name, contract) -> result
                    .append("S|").append(name.value())
                    .append('|').append(contract.cardinality().wireName())
                    .append('|').append(contract.required() ? '1' : '0')
                    .append('|').append(contract.minimumChildren())
                    .append('|').append(contract.maximumChildren())
                    .append('\n'));
        });
        return result.toString();
    }

    private static Map<String, CanvasNumericBounds> wireNumericBounds(
            CanvasPropertyContract contract) {
        TreeMap<String, CanvasNumericBounds> result = new TreeMap<>();
        contract.numericBounds().forEach((kind, bounds) -> {
            result.put(kind.wireName(), bounds);
            if (kind == PropertyValueKind.EDGE_INSETS) {
                result.put("edgeInsetsDirectional", bounds);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> wireConstraintFingerprints(
            CanvasPropertyContract contract) {
        TreeMap<String, String> result = new TreeMap<>();
        contract.constraintFingerprints().forEach((kind, fingerprint) -> {
            result.put(kind.wireName(), fingerprint);
            if (kind == PropertyValueKind.EDGE_INSETS) {
                // The semantic Java value kind deliberately covers both wire
                // variants decoded by the isolated Dart Canvas runtime.
                result.put("edgeInsetsDirectional", fingerprint);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static String decimalText(BigDecimal value) {
        if (value == null) {
            return "*";
        }
        BigDecimal canonical = value.stripTrailingZeros();
        return canonical.signum() == 0 ? "0" : canonical.toPlainString();
    }

    /** Immutable independently reviewed schema admitted to the native Canvas wire. */
    public record CanvasProjection(
            Map<PropertyName, Set<PropertyValueKind>> properties,
            Set<SlotName> slots,
            Map<PropertyName, CanvasPropertyContract> propertyContracts,
            Map<SlotName, CanvasSlotContract> slotContracts) {

        public CanvasProjection {
            properties = copyProperties(properties);
            slots = copySlots(slots);
            propertyContracts = copyPropertyContracts(propertyContracts);
            slotContracts = copySlotContracts(slotContracts);
            Map<PropertyName, Set<PropertyValueKind>> derivedProperties =
                    propertyContracts.entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().acceptedKinds(),
                            (left, right) -> left,
                            () -> new TreeMap<>(java.util.Comparator.comparing(
                                    PropertyName::value))));
            if (!properties.equals(derivedProperties)
                    || !slots.equals(slotContracts.keySet())) {
                throw new IllegalArgumentException(
                        "Canvas projection summary and exact contracts disagree");
            }
        }

        static CanvasProjection of(
                Map<PropertyName, CanvasPropertyContract> properties,
                Map<SlotName, CanvasSlotContract> slots) {
            Map<PropertyName, Set<PropertyValueKind>> kinds =
                    properties.entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().acceptedKinds()));
            return new CanvasProjection(kinds, slots.keySet(), properties, slots);
        }

        private static Map<PropertyName, Set<PropertyValueKind>> copyProperties(
                Map<PropertyName, Set<PropertyValueKind>> values) {
            Objects.requireNonNull(values, "properties");
            TreeMap<PropertyName, Set<PropertyValueKind>> copied = new TreeMap<>(
                    java.util.Comparator.comparing(PropertyName::value));
            values.forEach((name, kinds) -> copied.put(
                    Objects.requireNonNull(name, "properties contains null name"),
                    Set.copyOf(Objects.requireNonNull(
                            kinds, "properties contains null kinds"))));
            return Collections.unmodifiableMap(copied);
        }

        private static Set<SlotName> copySlots(Set<SlotName> values) {
            Objects.requireNonNull(values, "slots");
            TreeSet<SlotName> copied = new TreeSet<>(
                    java.util.Comparator.comparing(SlotName::value));
            copied.addAll(values);
            return Collections.unmodifiableSet(copied);
        }

        private static Map<PropertyName, CanvasPropertyContract> copyPropertyContracts(
                Map<PropertyName, CanvasPropertyContract> values) {
            Objects.requireNonNull(values, "propertyContracts");
            TreeMap<PropertyName, CanvasPropertyContract> copied = new TreeMap<>(
                    java.util.Comparator.comparing(PropertyName::value));
            values.forEach((name, contract) -> copied.put(
                    Objects.requireNonNull(name,
                            "propertyContracts contains null name"),
                    Objects.requireNonNull(contract,
                            "propertyContracts contains null contract")));
            return Collections.unmodifiableMap(copied);
        }

        private static Map<SlotName, CanvasSlotContract> copySlotContracts(
                Map<SlotName, CanvasSlotContract> values) {
            Objects.requireNonNull(values, "slotContracts");
            TreeMap<SlotName, CanvasSlotContract> copied = new TreeMap<>(
                    java.util.Comparator.comparing(SlotName::value));
            values.forEach((name, contract) -> copied.put(
                    Objects.requireNonNull(name,
                            "slotContracts contains null name"),
                    Objects.requireNonNull(contract,
                            "slotContracts contains null contract")));
            return Collections.unmodifiableMap(copied);
        }
    }

    /** Exact below-type Canvas property contract. */
    public record CanvasPropertyContract(
            Set<PropertyValueKind> acceptedKinds,
            boolean required,
            Optional<String> creationDefaultFingerprint,
            Map<PropertyValueKind, CanvasNumericBounds> numericBounds,
            Map<PropertyValueKind, String> constraintFingerprints) {

        public CanvasPropertyContract {
            Objects.requireNonNull(acceptedKinds, "acceptedKinds");
            Objects.requireNonNull(
                    creationDefaultFingerprint, "creationDefaultFingerprint");
            Objects.requireNonNull(numericBounds, "numericBounds");
            Objects.requireNonNull(
                    constraintFingerprints, "constraintFingerprints");
            acceptedKinds = Collections.unmodifiableSet(
                    acceptedKinds.isEmpty()
                            ? EnumSet.noneOf(PropertyValueKind.class)
                            : EnumSet.copyOf(acceptedKinds));
            if (acceptedKinds.isEmpty()) {
                throw new IllegalArgumentException(
                        "Canvas property must accept at least one kind");
            }
            creationDefaultFingerprint.ifPresent(value -> {
                if (value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('|') >= 0) {
                    throw new IllegalArgumentException(
                            "Canvas default fingerprint is not canonical");
                }
            });
            TreeMap<PropertyValueKind, CanvasNumericBounds> copiedBounds =
                    new TreeMap<>(java.util.Comparator.comparingInt(Enum::ordinal));
            numericBounds.forEach((kind, value) -> copiedBounds.put(
                    Objects.requireNonNull(kind,
                            "numericBounds contains null kind"),
                    Objects.requireNonNull(value,
                            "numericBounds contains null value")));
            EnumSet<PropertyValueKind> expectedBounds =
                    EnumSet.noneOf(PropertyValueKind.class);
            expectedBounds.addAll(acceptedKinds);
            expectedBounds.retainAll(NUMERIC_SCHEMA_KINDS);
            if (!copiedBounds.keySet().equals(expectedBounds)) {
                throw new IllegalArgumentException(
                        "Canvas numeric bounds must exactly cover numeric kinds; expected="
                        + expectedBounds + ", actual=" + copiedBounds.keySet());
            }
            numericBounds = Collections.unmodifiableMap(copiedBounds);
            TreeMap<PropertyValueKind, String> copiedConstraints =
                    new TreeMap<>(java.util.Comparator.comparingInt(Enum::ordinal));
            constraintFingerprints.forEach((kind, value) -> copiedConstraints.put(
                    Objects.requireNonNull(kind,
                            "constraintFingerprints contains null kind"),
                    Objects.requireNonNull(value,
                            "constraintFingerprints contains null value")));
            if (!copiedConstraints.keySet().equals(acceptedKinds)) {
                throw new IllegalArgumentException(
                        "Canvas constraint fingerprints must exactly cover accepted kinds; "
                        + "expected=" + acceptedKinds + ", actual="
                        + copiedConstraints.keySet());
            }
            copiedConstraints.values().forEach(value -> {
                if (value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('|') >= 0) {
                    throw new IllegalArgumentException(
                            "Canvas constraint fingerprint is not canonical");
                }
            });
            constraintFingerprints = Collections.unmodifiableMap(copiedConstraints);
        }
    }

    /** Inclusive/exclusive finite-or-unbounded numeric contract. */
    public record CanvasNumericBounds(
            BigDecimal minimum,
            boolean minimumInclusive,
            BigDecimal maximum,
            boolean maximumInclusive) {

        public CanvasNumericBounds {
            minimum = canonicalDecimal(minimum);
            maximum = canonicalDecimal(maximum);
            if (minimum == null) {
                minimumInclusive = true;
            }
            if (maximum == null) {
                maximumInclusive = true;
            }
            if (minimum != null && maximum != null) {
                int comparison = minimum.compareTo(maximum);
                if (comparison > 0
                        || (comparison == 0
                        && (!minimumInclusive || !maximumInclusive))) {
                    throw new IllegalArgumentException(
                            "Canvas numeric minimum exceeds maximum");
                }
            }
        }

        String fingerprint() {
            return decimalText(minimum) + ':' + (minimumInclusive ? '1' : '0')
                    + ':' + decimalText(maximum) + ':'
                    + (maximumInclusive ? '1' : '0');
        }

        private static BigDecimal canonicalDecimal(BigDecimal value) {
            if (value == null) {
                return null;
            }
            if (!DartNumericLiterals.isRepresentableDouble(value)) {
                throw new IllegalArgumentException(
                        "Canvas numeric bound is not a representable Dart double");
            }
            BigDecimal canonical = value.stripTrailingZeros();
            return canonical.signum() == 0 ? BigDecimal.ZERO : canonical;
        }
    }

    /** Exact below-type Canvas slot contract. */
    public record CanvasSlotContract(
            SlotCardinality cardinality,
            boolean required,
            int minimumChildren,
            int maximumChildren) {

        public CanvasSlotContract {
            Objects.requireNonNull(cardinality, "cardinality");
            if (minimumChildren < 0
                    || maximumChildren < minimumChildren
                    || maximumChildren > 10_000
                    || (cardinality == SlotCardinality.SINGLE
                    && maximumChildren > 1)) {
                throw new IllegalArgumentException(
                        "Invalid Canvas slot child bounds");
            }
        }
    }
}
