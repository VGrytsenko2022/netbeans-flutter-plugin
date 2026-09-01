package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
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
                    PropertyValueKind.ICON_DATA,
                    PropertyValueKind.THEME_TOKEN,
                    PropertyValueKind.PAINT,
                    PropertyValueKind.SHADOW_LIST,
                    PropertyValueKind.FONT_FEATURE_LIST,
                    PropertyValueKind.FONT_VARIATION_LIST,
                    PropertyValueKind.ALIGNMENT_GEOMETRY,
                    PropertyValueKind.BOX_CONSTRAINTS,
                    PropertyValueKind.MATRIX4,
                    PropertyValueKind.IMAGE_PROVIDER,
                    PropertyValueKind.BOX_DECORATION,
                    PropertyValueKind.CALLBACK));
    private static final Set<PropertyValueKind> NUMERIC_SCHEMA_KINDS =
            Collections.unmodifiableSet(EnumSet.of(
                    PropertyValueKind.INTEGER,
                    PropertyValueKind.DOUBLE,
                    PropertyValueKind.EDGE_INSETS));
    private static final String WIDGETS_LIBRARY = "package:flutter/widgets.dart";
    private static final String MATERIAL_LIBRARY = "package:flutter/material.dart";
    private static final String GESTURES_LIBRARY = "package:flutter/gestures.dart";
    private static final String SERVICES_LIBRARY = "package:flutter/services.dart";
    private static final String DART_UI_LIBRARY = "dart:ui";
    private static final String IMAGE_PROVIDER_CONTRACT_FINGERPRINT =
            "imageProvider:v1:asset,exactAsset:package:exactScale:"
            + "resize(1..16384,exact,fit,allowUpscaling)";
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
            Map.entry("flutter.material.Scaffold", STATIC_EDITABLE),
            Map.entry("flutter.material.AppBar", STATIC_EDITABLE),
            Map.entry("flutter.material.ElevatedButton", STATIC_EDITABLE),
            Map.entry("flutter.material.TextField", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Column", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Row", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Padding", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Center", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Text", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Icon", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SizedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AspectRatio", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Container", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Opacity", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Align", STATIC_EDITABLE),
            Map.entry("flutter.widgets.FractionallySizedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Stack", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Expanded", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Image", STATIC_EDITABLE));

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
            ZERO_TO_ONE_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE,
                    bounds(BigDecimal.ZERO, true, BigDecimal.ONE, true));
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            MINUS_FOUR_TO_FOUR_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE,
                    bounds(BigDecimal.valueOf(-4), true,
                            BigDecimal.valueOf(4), true));
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            POSITIVE_FONT_AXIS_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE,
                    bounds(BigDecimal.ZERO, false,
                            BigDecimal.valueOf(32768), false));
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            GRADE_AXIS_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE,
                    bounds(BigDecimal.valueOf(-32768), true,
                            BigDecimal.valueOf(32768), false));
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            UNBOUNDED_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE, UNBOUNDED_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            POSITIVE_INTEGER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER, POSITIVE_PORTABLE_INTEGER);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            NON_NEGATIVE_INTEGER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER, NON_NEGATIVE_PORTABLE_INTEGER);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            MAX_LENGTH_INTEGER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER,
                    bounds(
                            BigDecimal.valueOf(-1),
                            true,
                            new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER),
                            true));
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
            Map.entry("flutter.material.Scaffold", scaffoldProjection()),
            Map.entry("flutter.material.AppBar", appBarProjection()),
            Map.entry("flutter.material.ElevatedButton", elevatedButtonProjection()),
            Map.entry("flutter.material.TextField", textFieldProjection()),
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
            Map.entry("flutter.widgets.AspectRatio", projection(Map.ofEntries(
                    requiredDefaultNumericProperty(
                            "aspectRatio",
                            "double:1",
                            POSITIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Opacity", projection(Map.ofEntries(
                    requiredDefaultNumericProperty(
                            "opacity",
                            "double:1",
                            ZERO_TO_ONE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    property("alwaysIncludeSemantics", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Align", projection(Map.ofEntries(
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    numericProperty(
                            "widthFactor",
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER,
                            PropertyValueKind.DOUBLE),
                    numericProperty(
                            "heightFactor",
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER,
                            PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.FractionallySizedBox", projection(Map.ofEntries(
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    numericProperty(
                            "widthFactor",
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER,
                            PropertyValueKind.DOUBLE),
                    numericProperty(
                            "heightFactor",
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER,
                            PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Stack", projection(Map.ofEntries(
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                    enumProperty(
                            "fit", "StackFit", "loose", "expand", "passthrough"),
                    enumProperty(
                            "clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer")),
                    Map.of("children", listSlotSchema(false, 0, 10_000)))),
            Map.entry("flutter.widgets.Expanded", projection(Map.of(
                    "flex", numericSchema(
                            NON_NEGATIVE_INTEGER_BOUNDS,
                            PropertyValueKind.INTEGER)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.Image", imageProjection()),
            Map.entry("flutter.widgets.Container", containerProjection()),
            Map.entry("flutter.widgets.Icon", iconProjection()),
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
                    slot.maxChildren(),
                    slotAcceptanceFingerprint(slot.acceptance())));
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

    private static CanvasProjection containerProjection() {
        String decorationFingerprint = boxDecorationFingerprint(
                REVIEWED_COLOR_THEME_TOKENS);
        return projection(Map.ofEntries(
                Map.entry("alignment", constrainedSchema(
                        PropertyValueKind.ALIGNMENT_GEOMETRY,
                        "alignmentGeometry")),
                edgeInsetsProperty("padding", true),
                colorOrThemeProperty("color"),
                property("isAntiAlias", PropertyValueKind.BOOLEAN),
                Map.entry("decoration", constrainedSchema(
                        PropertyValueKind.BOX_DECORATION,
                        decorationFingerprint)),
                Map.entry("foregroundDecoration", constrainedSchema(
                        PropertyValueKind.BOX_DECORATION,
                        decorationFingerprint)),
                numericProperty(
                        "width", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "height", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                Map.entry("constraints", constrainedSchema(
                        PropertyValueKind.BOX_CONSTRAINTS,
                        "boxConstraints")),
                edgeInsetsProperty("margin", true),
                Map.entry("transform", constrainedSchema(
                        PropertyValueKind.MATRIX4,
                        "matrix4")),
                Map.entry("transformAlignment", constrainedSchema(
                        PropertyValueKind.ALIGNMENT_GEOMETRY,
                        "alignmentGeometry")),
                enumProperty(
                        "clipBehavior", "Clip", "none", "hardEdge",
                        "antiAlias", "antiAliasWithSaveLayer")),
                Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection imageProjection() {
        return projection(Map.ofEntries(
                requiredConstrainedProperty(
                        "image",
                        PropertyValueKind.IMAGE_PROVIDER,
                        IMAGE_PROVIDER_CONTRACT_FINGERPRINT),
                callbackProperty("frameBuilder"),
                callbackProperty("loadingBuilder"),
                callbackProperty("errorBuilder"),
                property("semanticLabel", PropertyValueKind.STRING),
                property("excludeFromSemantics", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "width", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "height", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                colorOrThemeProperty("color"),
                numericProperty(
                        "opacity", ZERO_TO_ONE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "colorBlendMode", "BlendMode",
                        "clear", "src", "dst", "srcOver", "dstOver",
                        "srcIn", "dstIn", "srcOut", "dstOut", "srcATop", "dstATop",
                        "xor", "plus", "modulate", "screen", "overlay", "darken",
                        "lighten", "colorDodge", "colorBurn", "hardLight", "softLight",
                        "difference", "exclusion", "multiply", "hue", "saturation",
                        "color", "luminosity"),
                enumProperty(
                        "fit", "BoxFit", "fill", "contain", "cover", "fitWidth",
                        "fitHeight", "none", "scaleDown"),
                Map.entry("alignment", constrainedSchema(
                        PropertyValueKind.ALIGNMENT_GEOMETRY,
                        "alignmentGeometry")),
                enumProperty(
                        "repeat", "ImageRepeat", "repeat", "repeatX", "repeatY",
                        "noRepeat"),
                numericProperty(
                        "centerSliceLeft", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "centerSliceTop", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "centerSliceRight", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "centerSliceBottom", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("matchTextDirection", PropertyValueKind.BOOLEAN),
                property("gaplessPlayback", PropertyValueKind.BOOLEAN),
                property("isAntiAlias", PropertyValueKind.BOOLEAN),
                enumProperty(
                        "filterQuality", "FilterQuality", "none", "low", "medium",
                        "high")),
                Map.of());
    }

    private static CanvasProjection textFieldProjection() {
        return projection(Map.ofEntries(
                stringPatternProperty(
                        "keyboardType",
                        "(?:text|multiline|number|numberSigned|numberDecimal|numberSignedDecimal|phone|datetime|emailAddress|url|visiblePassword|name|streetAddress|none|webSearch|twitter)"),
                enumPropertyForLibrary(
                        "textInputAction", SERVICES_LIBRARY, "TextInputAction",
                        "none", "unspecified", "done", "go", "search", "send",
                        "next", "previous", "continueAction", "join", "route",
                        "emergencyCall", "newline"),
                enumPropertyForLibrary(
                        "textCapitalization", SERVICES_LIBRARY, "TextCapitalization",
                        "words", "sentences", "characters", "none"),
                enumProperty(
                        "textAlign", "TextAlign",
                        "left", "right", "center", "justify", "start", "end"),
                stringPatternProperty(
                        "textAlignVertical", "(?:top|center|bottom)"),
                enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                property("readOnly", PropertyValueKind.BOOLEAN),
                property("showCursor", PropertyValueKind.BOOLEAN),
                property("autofocus", PropertyValueKind.BOOLEAN),
                stringPatternProperty(
                        "obscuringCharacter",
                        "[\\u0000-\\uD7FF\\uE000-\\uFFFF]"),
                property("obscureText", PropertyValueKind.BOOLEAN),
                property("autocorrect", PropertyValueKind.BOOLEAN),
                enumPropertyForLibrary(
                        "smartDashesType", SERVICES_LIBRARY, "SmartDashesType",
                        "disabled", "enabled"),
                enumPropertyForLibrary(
                        "smartQuotesType", SERVICES_LIBRARY, "SmartQuotesType",
                        "disabled", "enabled"),
                property("enableSuggestions", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "maxLines", POSITIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                numericProperty(
                        "minLines", POSITIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                property("expands", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "maxLength", MAX_LENGTH_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                enumPropertyForLibrary(
                        "maxLengthEnforcement", SERVICES_LIBRARY,
                        "MaxLengthEnforcement",
                        "none", "enforced", "truncateAfterCompositionEnds"),
                callbackProperty("onChanged"),
                callbackProperty("onEditingComplete"),
                callbackProperty("onSubmitted"),
                callbackProperty("onAppPrivateCommand"),
                property("enabled", PropertyValueKind.BOOLEAN),
                property("ignorePointers", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "cursorWidth", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "cursorHeight", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "cursorRadiusX", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "cursorRadiusY", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("cursorOpacityAnimates", PropertyValueKind.BOOLEAN),
                colorOrThemeProperty("cursorColor"),
                colorOrThemeProperty("cursorErrorColor"),
                enumPropertyForLibrary(
                        "selectionHeightStyle", DART_UI_LIBRARY, "BoxHeightStyle",
                        "tight", "max", "includeLineSpacingMiddle",
                        "includeLineSpacingTop", "includeLineSpacingBottom", "strut"),
                enumPropertyForLibrary(
                        "selectionWidthStyle", DART_UI_LIBRARY, "BoxWidthStyle",
                        "tight", "max"),
                enumProperty("keyboardAppearance", "Brightness", "dark", "light"),
                numericProperty(
                        "scrollPaddingLeft", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "scrollPaddingTop", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "scrollPaddingRight", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "scrollPaddingBottom", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumPropertyForLibrary(
                        "dragStartBehavior", GESTURES_LIBRARY, "DragStartBehavior",
                        "down", "start"),
                property("enableInteractiveSelection", PropertyValueKind.BOOLEAN),
                property("selectAllOnFocus", PropertyValueKind.BOOLEAN),
                callbackProperty("onTap"),
                property("onTapAlwaysCalled", PropertyValueKind.BOOLEAN),
                callbackProperty("onTapOutside"),
                callbackProperty("onTapUpOutside"),
                stringPatternProperty(
                        "mouseCursor",
                        "(?:none|basic|click|forbidden|wait|progress|contextMenu|help|text|verticalText|cell|precise|move|grab|grabbing|noDrop|alias|copy|disappearing|allScroll|resizeLeftRight|resizeUpDown|resizeUpLeftDownRight|resizeUpRightDownLeft|resizeUp|resizeDown|resizeLeft|resizeRight|resizeUpLeft|resizeUpRight|resizeDownLeft|resizeDownRight|resizeColumn|resizeRow|zoomIn|zoomOut)"),
                enumProperty(
                        "clipBehavior", "Clip", "none", "hardEdge", "antiAlias",
                        "antiAliasWithSaveLayer"),
                stringLengthProperty("restorationId", 1, 256),
                property("stylusHandwritingEnabled", PropertyValueKind.BOOLEAN),
                property("enableIMEPersonalizedLearning", PropertyValueKind.BOOLEAN),
                property("enableInlinePrediction", PropertyValueKind.BOOLEAN),
                property("canRequestFocus", PropertyValueKind.BOOLEAN)),
                Map.of());
    }

    private static CanvasProjection scaffoldProjection() {
        LinkedHashMap<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        put(properties, stringPatternProperty(
                "floatingActionButtonLocation",
                "(?:startTop|miniStartTop|centerTop|miniCenterTop|endTop|miniEndTop|startFloat|miniStartFloat|centerFloat|miniCenterFloat|endFloat|miniEndFloat|startDocked|miniStartDocked|centerDocked|miniCenterDocked|endDocked|miniEndDocked|endContained)"));
        put(properties, stringPatternProperty(
                "floatingActionButtonAnimator", "(?:scaling|noAnimation)"));
        put(properties, stringPatternProperty(
                "persistentFooterAlignment",
                "(?:topStart|topCenter|topEnd|centerStart|center|centerEnd|bottomStart|bottomCenter|bottomEnd)"));
        put(properties, callbackProperty("onDrawerChanged"));
        put(properties, callbackProperty("onEndDrawerChanged"));
        put(properties, colorOrThemeProperty("backgroundColor"));
        put(properties, property("resizeToAvoidBottomInset", PropertyValueKind.BOOLEAN));
        put(properties, property("primary", PropertyValueKind.BOOLEAN));
        put(properties, enumPropertyForLibrary(
                "drawerDragStartBehavior", GESTURES_LIBRARY,
                "DragStartBehavior", "down", "start"));
        put(properties, property("extendBody", PropertyValueKind.BOOLEAN));
        put(properties, property("drawerBarrierDismissible", PropertyValueKind.BOOLEAN));
        put(properties, property("extendBodyBehindAppBar", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty("drawerScrimColor"));
        put(properties, numericProperty(
                "drawerEdgeDragWidth", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, property("drawerEnableOpenDragGesture", PropertyValueKind.BOOLEAN));
        put(properties, property(
                "endDrawerEnableOpenDragGesture", PropertyValueKind.BOOLEAN));
        put(properties, stringLengthProperty("restorationId", 1, 256));
        return projection(properties, Map.of(
                "appBar", traitSingleSlotSchema(
                        false, 0, BuiltInWidgetCatalog.PREFERRED_SIZE_WIDGET_TRAIT),
                "body", singleSlotSchema(false, 0),
                "floatingActionButton", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection appBarProjection() {
        LinkedHashMap<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        put(properties, colorOrThemeProperty("backgroundColor"));
        put(properties, property("centerTitle", PropertyValueKind.BOOLEAN));
        put(properties, numericProperty(
                "elevation", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, property("automaticallyImplyLeading", PropertyValueKind.BOOLEAN));
        put(properties, property("automaticallyImplyActions", PropertyValueKind.BOOLEAN));
        put(properties, numericProperty(
                "scrolledUnderElevation", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, stringPatternProperty(
                "notificationPredicate", "(?:default|depthZero|all)"));
        put(properties, colorOrThemeProperty("shadowColor"));
        put(properties, colorOrThemeProperty("surfaceTintColor"));
        put(properties, colorOrThemeProperty("foregroundColor"));
        put(properties, property("primary", PropertyValueKind.BOOLEAN));
        put(properties, property("excludeHeaderSemantics", PropertyValueKind.BOOLEAN));
        put(properties, numericProperty(
                "titleSpacing", UNBOUNDED_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "toolbarOpacity", ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "bottomOpacity", ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "toolbarHeight", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "leadingWidth", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, property("forceMaterialTransparency", PropertyValueKind.BOOLEAN));
        put(properties, property("useDefaultSemanticsOrder", PropertyValueKind.BOOLEAN));
        put(properties, enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias",
                "antiAliasWithSaveLayer"));
        put(properties, edgeInsetsProperty("actionsPadding", true));
        put(properties, property("animateColor", PropertyValueKind.BOOLEAN));

        put(properties, stringPatternProperty(
                "shapeKind",
                "(?:roundedRectangle|stadium|circle|beveledRectangle|continuousRectangle)"));
        put(properties, colorOrThemeProperty("shapeSideColor"));
        put(properties, numericProperty(
                "shapeSideWidth", NON_NEGATIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                "shapeSideStyle", "BorderStyle", "none", "solid"));
        put(properties, numericProperty(
                "shapeSideStrokeAlign", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        for (String radius : List.of(
                "shapeRadiusTopLeft", "shapeRadiusTopRight",
                "shapeRadiusBottomRight", "shapeRadiusBottomLeft")) {
            put(properties, numericProperty(
                    radius, NON_NEGATIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        }
        put(properties, numericProperty(
                "shapeCircleEccentricity", ZERO_TO_ONE_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));

        appendIconThemeProjection(properties, "iconTheme");
        appendIconThemeProjection(properties, "actionsIconTheme");
        appendTextStyleProjection(properties, "toolbarTextStyle");
        appendTextStyleProjection(properties, "titleTextStyle");

        put(properties, colorOrThemeProperty(
                "systemOverlayStyleSystemNavigationBarColor"));
        put(properties, colorOrThemeProperty(
                "systemOverlayStyleSystemNavigationBarDividerColor"));
        put(properties, enumProperty(
                "systemOverlayStyleSystemNavigationBarIconBrightness",
                "Brightness", "light", "dark"));
        put(properties, property(
                "systemOverlayStyleSystemNavigationBarContrastEnforced",
                PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty("systemOverlayStyleStatusBarColor"));
        put(properties, enumProperty(
                "systemOverlayStyleStatusBarBrightness",
                "Brightness", "light", "dark"));
        put(properties, enumProperty(
                "systemOverlayStyleStatusBarIconBrightness",
                "Brightness", "light", "dark"));
        put(properties, property(
                "systemOverlayStyleSystemStatusBarContrastEnforced",
                PropertyValueKind.BOOLEAN));

        return projection(properties, Map.of(
                "leading", singleSlotSchema(false, 0),
                "title", singleSlotSchema(false, 0),
                "actions", listSlotSchema(false, 0, 10_000),
                "flexibleSpace", singleSlotSchema(false, 0),
                "bottom", traitSingleSlotSchema(
                        false, 0,
                BuiltInWidgetCatalog.PREFERRED_SIZE_WIDGET_TRAIT)));
    }

    private static CanvasProjection elevatedButtonProjection() {
        LinkedHashMap<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        put(properties, optionalDefaultProperty(
                "enabled", "boolean:true", PropertyValueKind.BOOLEAN));
        for (String callback : List.of(
                "onPressed", "onLongPress", "onHover", "onFocusChange")) {
            put(properties, callbackProperty(callback));
        }
        put(properties, property("autofocus", PropertyValueKind.BOOLEAN));
        put(properties, enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias",
                "antiAliasWithSaveLayer"));

        for (String prefix : List.of(
                "style", "styleDisabled", "stylePressed",
                "styleHovered", "styleFocused")) {
            appendElevatedButtonStateProjection(properties, prefix);
            appendElevatedButtonTextProjection(properties, prefix);
        }

        put(properties, numericProperty(
                "styleVisualDensityHorizontal",
                MINUS_FOUR_TO_FOUR_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "styleVisualDensityVertical",
                MINUS_FOUR_TO_FOUR_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, materialEnumProperty(
                "styleTapTargetSize", "MaterialTapTargetSize",
                "padded", "shrinkWrap"));
        put(properties, numericProperty(
                "styleAnimationDurationMs",
                Map.of(PropertyValueKind.INTEGER, NON_NEGATIVE_PORTABLE_INTEGER),
                PropertyValueKind.INTEGER));
        put(properties, property("styleEnableFeedback", PropertyValueKind.BOOLEAN));
        put(properties, stringPatternProperty(
                "styleAlignmentKind", "(?:physical|directional)"));
        put(properties, numericProperty(
                "styleAlignmentX", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "styleAlignmentY", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, stringPatternProperty(
                "styleSplashFactory",
                "(?:inkRipple|inkSplash|inkSparkle|noSplash)"));

        if (properties.size()
                != ElevatedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ElevatedButton Canvas projection must contain exactly "
                    + ElevatedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT
                    + " properties; actual=" + properties.size());
        }
        return projection(properties, Map.of(
                "child", singleSlotSchema(true, 0)));
    }

    private static void appendElevatedButtonStateProjection(
            Map<String, CanvasPropertyContract> properties,
            String prefix) {
        for (String suffix : List.of(
                "BackgroundColor", "ForegroundColor", "OverlayColor",
                "ShadowColor", "SurfaceTintColor")) {
            put(properties, colorOrThemeProperty(prefix + suffix));
        }
        put(properties, numericProperty(
                prefix + "Elevation", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, edgeInsetsProperty(prefix + "Padding", true));
        for (String suffix : List.of(
                "MinimumWidth", "MinimumHeight", "FixedWidth", "FixedHeight",
                "MaximumWidth", "MaximumHeight")) {
            put(properties, numericProperty(
                    prefix + suffix, NON_NEGATIVE_NUMBER_BOUNDS,
                    PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        }
        put(properties, colorOrThemeProperty(prefix + "IconColor"));
        put(properties, numericProperty(
                prefix + "IconSize", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, colorOrThemeProperty(prefix + "SideColor"));
        put(properties, numericProperty(
                prefix + "SideWidth", NON_NEGATIVE_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                prefix + "SideStyle", "BorderStyle", "none", "solid"));
        put(properties, numericProperty(
                prefix + "SideStrokeAlign", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, stringPatternProperty(
                prefix + "ShapeKind",
                "(?:roundedRectangle|roundedSuperellipse|stadium|circle|beveledRectangle|continuousRectangle)"));
        for (String suffix : List.of(
                "ShapeRadiusTopLeft", "ShapeRadiusTopRight",
                "ShapeRadiusBottomRight", "ShapeRadiusBottomLeft")) {
            put(properties, numericProperty(
                    prefix + suffix, NON_NEGATIVE_DOUBLE_BOUNDS,
                    PropertyValueKind.DOUBLE));
        }
        put(properties, numericProperty(
                prefix + "ShapeCircleEccentricity",
                ZERO_TO_ONE_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, stringPatternProperty(
                prefix + "MouseCursor",
                "(?:none|basic|click|forbidden|wait|progress|contextMenu|help|text|verticalText|cell|precise|move|grab|grabbing|noDrop|alias|copy|disappearing|allScroll|resizeLeftRight|resizeUpDown|resizeUpLeftDownRight|resizeUpRightDownLeft|resizeUp|resizeDown|resizeLeft|resizeRight|resizeUpLeft|resizeUpRight|resizeDownLeft|resizeDownRight|resizeColumn|resizeRow|zoomIn|zoomOut)"));
    }

    private static void appendElevatedButtonTextProjection(
            Map<String, CanvasPropertyContract> properties,
            String prefix) {
        String text = prefix + "Text";
        put(properties, themeTokenProperty(
                text + "Theme", REVIEWED_TEXT_THEME_TOKENS));
        put(properties, property(text + "Inherit", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty(text + "BackgroundColor"));
        put(properties, numericProperty(
                text + "FontSize", NON_NEGATIVE_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                text + "FontWeight", "FontWeight",
                "w100", "w200", "w300", "w400", "w500",
                "w600", "w700", "w800", "w900"));
        put(properties, enumProperty(
                text + "FontStyle", "FontStyle", "normal", "italic"));
        put(properties, numericProperty(
                text + "LetterSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                text + "WordSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                text + "TextBaseline", "TextBaseline", "alphabetic", "ideographic"));
        put(properties, numericProperty(
                text + "Height", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                text + "LeadingDistribution", "TextLeadingDistribution",
                "proportional", "even"));
        put(properties, stringPatternProperty(
                text + "LocaleLanguageCode", "(?:[a-z]{2,3}|[a-z]{5,8})"));
        put(properties, stringPatternProperty(
                text + "LocaleScriptCode", "[A-Z][a-z]{3}"));
        put(properties, stringPatternProperty(
                text + "LocaleCountryCode", "(?:[A-Z]{2}|[0-9]{3})"));
        put(properties, paintProperty(text + "Background"));
        put(properties, shadowProperty(text + "Shadows"));
        put(properties, property(
                text + "FontFeatures", PropertyValueKind.FONT_FEATURE_LIST));
        put(properties, fontVariationProperty(text + "FontVariations"));
        put(properties, property(
                text + "DecorationUnderline", PropertyValueKind.BOOLEAN));
        put(properties, property(
                text + "DecorationOverline", PropertyValueKind.BOOLEAN));
        put(properties, property(
                text + "DecorationLineThrough", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty(text + "DecorationColor"));
        put(properties, enumProperty(
                text + "DecorationStyle", "TextDecorationStyle",
                "solid", "double", "dotted", "dashed", "wavy"));
        put(properties, numericProperty(
                text + "DecorationThickness", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, stringLengthProperty(text + "FontFamily", 1, 256));
        put(properties, stringLengthProperty(text + "FontFamilyFallback", 0, 4096));
        put(properties, stringLengthProperty(text + "Package", 1, 256));
        put(properties, enumProperty(
                text + "Overflow", "TextOverflow",
                "clip", "fade", "ellipsis", "visible"));
    }

    private static void appendIconThemeProjection(
            Map<String, CanvasPropertyContract> properties,
            String prefix) {
        put(properties, numericProperty(
                prefix + "Size", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "Fill", ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "Weight", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "Grade", GRADE_AXIS_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "OpticalSize", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, colorOrThemeProperty(prefix + "Color"));
        put(properties, numericProperty(
                prefix + "Opacity", ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, shadowProperty(prefix + "Shadows"));
        put(properties, property(prefix + "ApplyTextScaling", PropertyValueKind.BOOLEAN));
    }

    private static void appendTextStyleProjection(
            Map<String, CanvasPropertyContract> properties,
            String prefix) {
        put(properties, themeTokenProperty(prefix + "ThemeTextStyle", REVIEWED_TEXT_THEME_TOKENS));
        put(properties, property(prefix + "Inherit", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty(prefix + "Color"));
        put(properties, colorOrThemeProperty(prefix + "BackgroundColor"));
        put(properties, numericProperty(
                prefix + "FontSize", NON_NEGATIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                prefix + "FontWeight", "FontWeight",
                "w100", "w200", "w300", "w400", "w500",
                "w600", "w700", "w800", "w900"));
        put(properties, enumProperty(
                prefix + "FontStyle", "FontStyle", "normal", "italic"));
        put(properties, numericProperty(
                prefix + "LetterSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "WordSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                prefix + "TextBaseline", "TextBaseline", "alphabetic", "ideographic"));
        put(properties, numericProperty(
                prefix + "Height", UNBOUNDED_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                prefix + "LeadingDistribution", "TextLeadingDistribution",
                "proportional", "even"));
        put(properties, stringPatternProperty(
                prefix + "LocaleLanguageCode", "(?:[a-z]{2,3}|[a-z]{5,8})"));
        put(properties, stringPatternProperty(
                prefix + "LocaleScriptCode", "[A-Z][a-z]{3}"));
        put(properties, stringPatternProperty(
                prefix + "LocaleCountryCode", "(?:[A-Z]{2}|[0-9]{3})"));
        put(properties, paintProperty(prefix + "Foreground"));
        put(properties, paintProperty(prefix + "Background"));
        put(properties, shadowProperty(prefix + "Shadows"));
        put(properties, property(prefix + "FontFeatures", PropertyValueKind.FONT_FEATURE_LIST));
        put(properties, fontVariationProperty(prefix + "FontVariations"));
        put(properties, property(prefix + "DecorationUnderline", PropertyValueKind.BOOLEAN));
        put(properties, property(prefix + "DecorationOverline", PropertyValueKind.BOOLEAN));
        put(properties, property(prefix + "DecorationLineThrough", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty(prefix + "DecorationColor"));
        put(properties, enumProperty(
                prefix + "DecorationStyle", "TextDecorationStyle",
                "solid", "double", "dotted", "dashed", "wavy"));
        put(properties, numericProperty(
                prefix + "DecorationThickness", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, property(prefix + "DebugLabel", PropertyValueKind.STRING));
        put(properties, stringLengthProperty(prefix + "FontFamily", 1, 256));
        put(properties, stringLengthProperty(prefix + "FontFamilyFallback", 0, 4096));
        put(properties, stringLengthProperty(prefix + "Package", 1, 256));
        put(properties, enumProperty(
                prefix + "Overflow", "TextOverflow", "clip", "fade",
                "ellipsis", "visible"));
    }

    private static void put(
            Map<String, CanvasPropertyContract> properties,
            Map.Entry<String, CanvasPropertyContract> entry) {
        if (properties.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
            throw new ExceptionInInitializerError(
                    "Duplicate reviewed Canvas property " + entry.getKey());
        }
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

    private static CanvasProjection iconProjection() {
        return projection(Map.ofEntries(
                requiredDefaultConstrainedProperty(
                        "icon",
                        "iconData:58873:TWF0ZXJpYWxJY29ucw:-:0:-",
                        PropertyValueKind.ICON_DATA,
                        "materialIcons:3.44.8:058e0af2c2:8825:"
                        + "ba88e3e23962ada6537523aa113811d9719b988412815bf084f50a0aa78137f0"),
                numericProperty(
                        "size", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "fill", ZERO_TO_ONE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "weight", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "grade", GRADE_AXIS_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "opticalSize", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                colorOrThemeProperty("color"),
                shadowProperty("shadows"),
                property("semanticLabel", PropertyValueKind.STRING),
                enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                property("applyTextScaling", PropertyValueKind.BOOLEAN),
                enumProperty(
                        "blendMode", "BlendMode",
                        "clear", "src", "dst", "srcOver", "dstOver",
                        "srcIn", "dstIn", "srcOut", "dstOut", "srcATop", "dstATop",
                        "xor", "plus", "modulate", "screen", "overlay", "darken",
                        "lighten", "colorDodge", "colorBurn", "hardLight", "softLight",
                        "difference", "exclusion", "multiply", "hue", "saturation",
                        "color", "luminosity"),
                enumProperty(
                        "fontWeight", "FontWeight",
                        "w100", "w200", "w300", "w400", "w500",
                        "w600", "w700", "w800", "w900")),
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
        return enumPropertyForLibrary(name, WIDGETS_LIBRARY, enumType, values);
    }

    private static Map.Entry<String, CanvasPropertyContract> materialEnumProperty(
            String name,
            String enumType,
            String... values) {
        return enumPropertyForLibrary(name, MATERIAL_LIBRARY, enumType, values);
    }

    private static Map.Entry<String, CanvasPropertyContract> enumPropertyForLibrary(
            String name,
            String library,
            String enumType,
            String... values) {
        String fingerprint = "enum:" + base64(library) + ':'
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

    private static Map.Entry<String, CanvasPropertyContract> edgeInsetsProperty(
            String name,
            boolean nonNegative) {
        CanvasNumericBounds numeric = nonNegative
                ? NON_NEGATIVE_NUMERIC : UNBOUNDED_NUMERIC;
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(PropertyValueKind.EDGE_INSETS),
                false,
                Optional.empty(),
                Map.of(PropertyValueKind.EDGE_INSETS, numeric),
                Map.of(
                        PropertyValueKind.EDGE_INSETS,
                        "edgeInsets:" + (nonNegative ? '1' : '0')
                        + ':' + numeric.fingerprint())));
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

    private static Map.Entry<String, CanvasPropertyContract>
            requiredDefaultNumericProperty(
                    String name,
                    String creationDefaultFingerprint,
                    Map<PropertyValueKind, CanvasNumericBounds> numericBounds,
                    PropertyValueKind... kinds) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kinds),
                true,
                Optional.of(creationDefaultFingerprint),
                numericBounds,
                rangeConstraintFingerprints(numericBounds)));
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

    private static Map.Entry<String, CanvasPropertyContract> optionalDefaultProperty(
            String name,
            String creationDefaultFingerprint,
            PropertyValueKind... kinds) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kinds),
                false,
                Optional.of(creationDefaultFingerprint),
                Map.of(),
                anyConstraintFingerprints(kinds)));
    }

    private static Map.Entry<String, CanvasPropertyContract> callbackProperty(
            String name) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.CALLBACK, "callbackReference"));
    }

    private static Map.Entry<String, CanvasPropertyContract>
            requiredDefaultConstrainedProperty(
                    String name,
                    String creationDefaultFingerprint,
                    PropertyValueKind kind,
                    String constraintFingerprint) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kind),
                true,
                Optional.of(creationDefaultFingerprint),
                Map.of(),
                Map.of(kind, constraintFingerprint)));
    }

    private static Map.Entry<String, CanvasPropertyContract>
            requiredConstrainedProperty(
                    String name,
                    PropertyValueKind kind,
                    String constraintFingerprint) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kind),
                true,
                Optional.empty(),
                Map.of(),
                Map.of(kind, constraintFingerprint)));
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
                SlotCardinality.SINGLE, required, minimumChildren, 1, "any");
    }

    private static CanvasSlotContract traitSingleSlotSchema(
            boolean required,
            int minimumChildren,
            String trait) {
        return new CanvasSlotContract(
                SlotCardinality.SINGLE, required, minimumChildren, 1,
                "trait:" + base64(trait));
    }

    private static CanvasSlotContract listSlotSchema(
            boolean required,
            int minimumChildren,
            int maximumChildren) {
        return new CanvasSlotContract(
                SlotCardinality.LIST,
                required,
                minimumChildren,
                maximumChildren,
                "any");
    }

    private static String slotAcceptanceFingerprint(SlotAcceptance acceptance) {
        return switch (acceptance) {
            case SlotAcceptance.AnyWidget ignored -> "any";
            case SlotAcceptance.HasTrait trait -> "trait:" + base64(trait.trait());
            case SlotAcceptance.ExactTypes types -> "types:"
                    + types.typeIds().stream()
                            .map(type -> base64(type.value()))
                            .sorted()
                            .collect(Collectors.joining(","));
        };
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

    private static String boxDecorationFingerprint(List<String> themeTokenIds) {
        return "boxDecoration:v2:"
                + IMAGE_PROVIDER_CONTRACT_FINGERPRINT
                + ":decorationImage:v1:"
                + "onError,colorFilter(mode,matrix20,linearToSrgbGamma,"
                + "srgbToLinearGamma,saturation),fit,alignment,centerSlice,"
                + "repeat,matchTextDirection,scale,opacity,filterQuality,"
                + "invertColors,isAntiAlias:centerSliceFit(except:cover,none):theme="
                + themeTokenIds.stream().sorted().collect(Collectors.joining(","));
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
        if (constraint instanceof PropertyValueConstraint.AlignmentGeometryValues) {
            return "alignmentGeometry";
        }
        if (constraint instanceof PropertyValueConstraint.BoxConstraintsValues) {
            return "boxConstraints";
        }
        if (constraint instanceof PropertyValueConstraint.Matrix4Values) {
            return "matrix4";
        }
        if (constraint instanceof PropertyValueConstraint.ImageProviderValues) {
            return IMAGE_PROVIDER_CONTRACT_FINGERPRINT;
        }
        if (constraint instanceof PropertyValueConstraint.BoxDecorationValues values) {
            return boxDecorationFingerprint(values.colorThemeTokenIds());
        }
        if (constraint instanceof PropertyValueConstraint.StringLength length) {
            return "length:" + length.minimum() + ':' + length.maximum();
        }
        if (constraint instanceof PropertyValueConstraint.StringPattern pattern) {
            return "pattern:" + base64(pattern.regularExpression());
        }
        if (constraint instanceof PropertyValueConstraint.IconDataValues) {
            return "iconData:0:1114111:55296:57343:256:32:1:1:"
                    + "061C,200E,200F,2028-202E,2066-2069,FEFF";
        }
        if (constraint instanceof PropertyValueConstraint.MaterialIconValues) {
            MaterialIconRegistry.SourceMetadata metadata =
                    MaterialIconRegistry.bundled().metadata();
            return "materialIcons:" + metadata.flutterVersion()
                    + ':' + metadata.flutterRevision()
                    + ':' + metadata.iconCount()
                    + ':' + metadata.sourceSha256();
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
        if (constraint instanceof PropertyValueConstraint.CallbackReference) {
            return "callbackReference";
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
        if (value instanceof PropertyValue.BooleanValue bool) {
            return "boolean:" + bool.value();
        }
        if (value instanceof PropertyValue.DoubleValue decimal) {
            return "double:" + decimalText(decimal.value());
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
        if (value instanceof PropertyValue.IconDataValue icon) {
            return "iconData:"
                    + icon.codePoint().map(String::valueOf).orElse("-") + ':'
                    + icon.fontFamily().map(BuiltInWidgetCapabilityCatalog::base64)
                            .orElse("-") + ':'
                    + icon.fontPackage().map(BuiltInWidgetCapabilityCatalog::base64)
                            .orElse("-") + ':'
                    + (icon.matchTextDirection() ? '1' : '0') + ':'
                    + (icon.fontFamilyFallback().isEmpty()
                            ? "-"
                            : icon.fontFamilyFallback().stream()
                                    .map(BuiltInWidgetCapabilityCatalog::base64)
                                    .collect(Collectors.joining(",")));
        }
        throw new ExceptionInInitializerError(
                "Canvas creation default requires a reviewed typed fingerprint for "
                + value.kind().wireName());
    }

    private static String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean validSlotAcceptanceFingerprint(String value) {
        if (value.equals("any")) {
            return true;
        }
        if (value.startsWith("trait:")) {
            String decoded = decodeCanonicalBase64(value.substring("trait:".length()));
            if (decoded == null) {
                return false;
            }
            try {
                new SlotAcceptance.HasTrait(decoded);
                return true;
            } catch (IllegalArgumentException invalidTrait) {
                return false;
            }
        }
        if (!value.startsWith("types:")) {
            return false;
        }
        String encoded = value.substring("types:".length());
        if (encoded.isEmpty()) {
            return false;
        }
        List<String> tokens = List.of(encoded.split(",", -1));
        if (!tokens.equals(tokens.stream().sorted().distinct().toList())) {
            return false;
        }
        for (String token : tokens) {
            String decoded = decodeCanonicalBase64(token);
            if (decoded == null) {
                return false;
            }
            try {
                new dev.flutter.netbeans.designer.model.WidgetTypeId(decoded);
            } catch (IllegalArgumentException invalidType) {
                return false;
            }
        }
        return true;
    }

    private static String decodeCanonicalBase64(String encoded) {
        if (encoded.isEmpty() || encoded.indexOf('=') >= 0) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return base64(decoded).equals(encoded) ? decoded : null;
        } catch (IllegalArgumentException | CharacterCodingException invalid) {
            return null;
        }
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
                    .append('|').append(contract.acceptanceFingerprint())
                    .append('\n'));
            WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                    .find(new WidgetTypeId(type))
                    .orElseThrow();
            WidgetPlacementRules.capabilityFingerprintLines(definition)
                    .forEach(line -> result.append(line).append('\n'));
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
            int maximumChildren,
            String acceptanceFingerprint) {

        public CanvasSlotContract(
                SlotCardinality cardinality,
                boolean required,
                int minimumChildren,
                int maximumChildren) {
            this(cardinality, required, minimumChildren, maximumChildren, "any");
        }

        public CanvasSlotContract {
            Objects.requireNonNull(cardinality, "cardinality");
            Objects.requireNonNull(acceptanceFingerprint, "acceptanceFingerprint");
            if (minimumChildren < 0
                    || maximumChildren < minimumChildren
                    || maximumChildren > 10_000
                    || (cardinality == SlotCardinality.SINGLE
                    && maximumChildren > 1)) {
                throw new IllegalArgumentException(
                        "Invalid Canvas slot child bounds");
            }
            if (!validSlotAcceptanceFingerprint(acceptanceFingerprint)) {
                throw new IllegalArgumentException(
                        "Invalid Canvas slot acceptance fingerprint");
            }
        }
    }
}
