package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DartSymbolReference;
import dev.flutter.netbeans.designer.catalog.AppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MaterialThemeTokenCatalog;
import dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ParameterStyle;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.validation.ValidationIssue;
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Pure deterministic generator for versioned .fd managed Dart payloads. */
public final class DartRegionGenerator {
    public static final String PROFILE_ID = "fd-dart-regions-v1";

    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String GESTURES_IMPORT = "package:flutter/gestures.dart";
    private static final String SERVICES_IMPORT = "package:flutter/services.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String VECTOR_MATH_64_LIBRARY =
            "package:vector_math/vector_math_64.dart";
    private static final String DART_CONVERT_IMPORT = "dart:convert";
    private static final String DART_UI_IMPORT = "dart:ui";
    private static final String UNRESOLVED_IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAA"
            + "AARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/"
            + "j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=";
    private static final int INLINE_CONSTRUCTOR_LIMIT = 100;
    private static final Comparator<ConstructorArgument> ARGUMENT_ORDER = Comparator
            .comparing((ConstructorArgument value) -> value.parameter().style())
            .thenComparingInt(value -> value.parameter().order())
            .thenComparing(ConstructorArgument::name);
    private static final Comparator<CompositeMember> COMPOSITE_MEMBER_ORDER = Comparator
            .comparingInt(CompositeMember::order)
            .thenComparing(CompositeMember::name);
    private static final List<ElevatedButtonState> ELEVATED_BUTTON_STATES = List.of(
            new ElevatedButtonState("styleDisabled", "disabled"),
            new ElevatedButtonState("stylePressed", "pressed"),
            new ElevatedButtonState("styleHovered", "hovered"),
            new ElevatedButtonState("styleFocused", "focused"),
            new ElevatedButtonState("style", "any"));

    private final DartGenerationLimits limits;
    private final WidgetTreeValidator validator;

    public DartRegionGenerator() {
        this(DartGenerationLimits.defaults(), ValidationLimits.defaults());
    }

    public DartRegionGenerator(DartGenerationLimits limits) {
        this(limits, ValidationLimits.defaults());
    }

    public DartRegionGenerator(
            DartGenerationLimits limits,
            ValidationLimits validationLimits) {
        this.limits = Objects.requireNonNull(limits, "limits");
        this.validator = new WidgetTreeValidator(Objects.requireNonNull(
                validationLimits, "validationLimits"));
    }

    public DartGenerationResult generate(
            DesignerDocument document,
            WidgetCatalog catalog) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(catalog, "catalog");

        ValidationResult validation = validator.validate(document, catalog);
        if (!validation.valid()) {
            ValidationIssue first = validation.errors().getFirst();
            return failure(validation, new DartGenerationDiagnostic(
                    DartGenerationDiagnosticCode.MODEL_INVALID,
                    first.path(),
                    first.widgetId(),
                    Optional.empty(),
                    "Dart generation is blocked by " + first.code() + ": " + first.message()));
        }
        if (document.source().widgetKind() != WidgetClassKind.STATELESS) {
            return failure(validation, diagnostic(
                    DartGenerationDiagnosticCode.UNSUPPORTED_WIDGET_KIND,
                    "/source/widgetKind",
                    Optional.empty(),
                    Optional.empty(),
                    "Dart generation profile " + PROFILE_ID
                    + " supports only a stateless designer class; received "
                    + document.source().widgetKind().wireName() + "."));
        }

        try {
            GenerationContext planned = createContext(document.root(), catalog);
            String importsPayload = renderImports(
                    planned.importPlan(), limits.maxTotalPayloadUtf8Bytes());
            GeneratedDartRegion imports = GeneratedDartRegion.create(
                    DartManagedRegionId.IMPORTS, importsPayload);
            String buildPrefix = "  @override\n"
                    + "  Widget build(BuildContext context) {\n"
                    + "    return ";
            String buildSuffix = ";\n  }\n";
            long maximumRootBytes = (long) limits.maxTotalPayloadUtf8Bytes()
                    - imports.utf8Size()
                    - utf8Length(buildPrefix)
                    - utf8Length(buildSuffix);
            if (maximumRootBytes < 1) {
                throw outputLimit(
                        "/source/managedRegions",
                        Optional.empty(),
                        "Generated imports and the fixed build method exceed "
                        + "maxTotalPayloadUtf8Bytes="
                        + limits.maxTotalPayloadUtf8Bytes() + ".");
            }
            GenerationContext context = new GenerationContext(
                    planned.catalog(),
                    planned.importPlan(),
                    planned.planner(),
                    Math.toIntExact(maximumRootBytes));
            RenderedValue root = renderNode(document.root(), "/root", 4, context);
            String buildPayload = buildPrefix + root.joined() + buildSuffix;

            GeneratedDartRegion build = GeneratedDartRegion.create(
                    DartManagedRegionId.BUILD, buildPayload);
            ArrayList<GeneratedDartSymbolOccurrence> symbolOccurrences = new ArrayList<>();
            String flutterUmbrella = context.planner().effectiveUri(WIDGETS_IMPORT);
            symbolOccurrences.add(occurrence(
                    "build:return-type:Widget",
                    "  @override\n  ".length(),
                    "Widget",
                    flutterUmbrella,
                    "/source/managedRegions/build",
                    Optional.empty()));
            symbolOccurrences.add(occurrence(
                    "build:parameter-type:BuildContext",
                    "  @override\n  Widget build(".length(),
                    "BuildContext",
                    flutterUmbrella,
                    "/source/managedRegions/build",
                    Optional.empty()));
            for (GeneratedDartSymbolOccurrence occurrence : root.symbolOccurrences()) {
                symbolOccurrences.add(occurrence.shifted(buildPrefix.length()));
            }
            if (symbolOccurrences.size()
                    > limits.candidateCapacityBudget()
                            .maxGeneratedSymbolOccurrences()) {
                throw abort(diagnostic(
                        DartGenerationDiagnosticCode.SYMBOL_PROBE_LIMIT,
                        "/source/managedRegions/build",
                        Optional.empty(),
                        Optional.of(DartManagedRegionId.BUILD),
                        "Generated Dart requires " + symbolOccurrences.size()
                        + " symbol occurrences plus "
                        + limits.candidateCapacityBudget()
                                .reservedSourceSymbolProbes()
                        + " source-owned probes, exceeding shared capacity profile "
                        + limits.candidateCapacityBudget().profileId()
                        + " with maxSymbolProbes="
                        + limits.candidateCapacityBudget().maxSymbolProbes()
                        + "."));
            }
            long totalBytes = (long) imports.utf8Size() + build.utf8Size();
            if (totalBytes > limits.maxTotalPayloadUtf8Bytes()) {
                throw abort(diagnostic(
                        DartGenerationDiagnosticCode.OUTPUT_SIZE_LIMIT,
                        "/source/managedRegions",
                        Optional.empty(),
                        Optional.empty(),
                        "Generated imports and build payloads require " + totalBytes
                        + " UTF-8 bytes, exceeding maxTotalPayloadUtf8Bytes="
                        + limits.maxTotalPayloadUtf8Bytes() + "."));
            }
            return new DartGenerationResult(
                    validation,
                    Optional.of(new GeneratedDartRegions(
                            imports,
                            build,
                            context.importPlan(),
                            PROFILE_ID,
                            symbolOccurrences,
                            limits.candidateCapacityBudget())),
                    List.of());
        } catch (GenerationAbort failure) {
            return failure(validation, failure.diagnostic());
        } catch (IllegalArgumentException malformedUnicode) {
            return failure(validation, diagnostic(
                    DartGenerationDiagnosticCode.INVALID_UNICODE,
                    "/root",
                    Optional.empty(),
                    Optional.empty(),
                    "Generated Dart could not be encoded as strict UTF-8: "
                    + malformedUnicode.getMessage()));
        }
    }

    private GenerationContext createContext(WidgetNode root, WidgetCatalog catalog) {
        TreeMap<String, WidgetDefinition> usedDefinitions = new TreeMap<>();
        boolean requiresMaterialTheme = false;
        boolean requiresServices = false;
        boolean requiresGestures = false;
        boolean requiresRendering = false;
        boolean requiresDartConvert = false;
        boolean requiresDartUi = false;
        Deque<WidgetAtPath> pending = new ArrayDeque<>();
        pending.push(new WidgetAtPath(root, "/root"));
        while (!pending.isEmpty()) {
            WidgetAtPath current = pending.pop();
            WidgetDefinition definition = catalog.find(current.node().type())
                    .orElseThrow(() -> abort(diagnostic(
                    DartGenerationDiagnosticCode.INTERNAL_CATALOG_INCONSISTENCY,
                    current.path(),
                    Optional.of(current.node().id()),
                    Optional.empty(),
                    "Validated widget type '" + current.node().type().value()
                    + "' disappeared from the generation catalog.")));
            usedDefinitions.putIfAbsent(definition.typeId().value(), definition);
            requiresMaterialTheme |= current.node().properties().values().stream()
                    .anyMatch(DartRegionGenerator::requiresMaterialTheme);
            requiresServices |= current.node().type().equals(
                    AppBarWidgetPropertySchema.APP_BAR_TYPE)
                    && current.node().properties().keySet().stream()
                            .map(PropertyName::value)
                            .anyMatch(name -> name.startsWith("systemOverlayStyle"));
            requiresServices |= current.node().type().equals(
                    TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE)
                    && current.node().properties().containsKey(
                            new PropertyName("keyboardType"));
            requiresServices |= usesEnumLibrary(
                    current.node(), definition, SERVICES_IMPORT);
            requiresGestures |= usesEnumLibrary(
                    current.node(), definition, GESTURES_IMPORT);
            requiresRendering |= usesEnumLibrary(
                    current.node(), definition, RENDERING_IMPORT);
            requiresRendering |= isStaticScrollView(current.node())
                    && current.node().properties().containsKey(
                            new PropertyName("scrollCacheExtent"));
            requiresDartConvert |= current.node().properties().values().stream()
                    .anyMatch(DartRegionGenerator::requiresDartConvert);
            requiresDartUi |= usesEnumLibrary(
                    current.node(), definition, DART_UI_IMPORT);

            ArrayList<WidgetAtPath> children = new ArrayList<>();
            for (Map.Entry<SlotName, WidgetSlot> entry : current.node().slots().entrySet()) {
                String slotPath = current.path() + "/slots/" + pointer(entry.getKey().value());
                if (entry.getValue() instanceof WidgetSlot.SingleSlot single) {
                    single.child().ifPresent(child -> children.add(
                            new WidgetAtPath(child, slotPath + "/child")));
                } else {
                    List<WidgetNode> list = ((WidgetSlot.ListSlot) entry.getValue()).children();
                    for (int index = 0; index < list.size(); index++) {
                        children.add(new WidgetAtPath(
                                list.get(index), slotPath + "/children/" + index));
                    }
                }
            }
            for (int index = children.size() - 1; index >= 0; index--) {
                pending.push(children.get(index));
            }
        }

        ImportPlanner planner = ImportPlanner.create(
                usedDefinitions.values(), limits.maxImports(), requiresMaterialTheme,
                requiresServices, requiresGestures, requiresRendering,
                requiresDartConvert, requiresDartUi);
        return new GenerationContext(catalog, planner.plan(), planner, 0);
    }

    private static boolean requiresDartConvert(PropertyValue value) {
        if (value instanceof PropertyValue.ImageProviderValue provider) {
            return provider.isUnresolved();
        }
        if (value instanceof PropertyValue.BoxDecorationValue decoration) {
            return decoration.image()
                    .map(PropertyValue.DecorationImageValue::image)
                    .map(PropertyValue.ImageProviderValue::isUnresolved)
                    .orElse(false);
        }
        return false;
    }

    private static boolean usesEnumLibrary(
            WidgetNode node,
            WidgetDefinition definition,
            String libraryUri) {
        return node.properties().keySet().stream()
                .map(definition::property)
                .flatMap(Optional::stream)
                .flatMap(property -> property.constraints().stream())
                .filter(PropertyValueConstraint.EnumValues.class::isInstance)
                .map(PropertyValueConstraint.EnumValues.class::cast)
                .anyMatch(constraint -> constraint.dartType().libraryUri()
                        .equals(libraryUri));
    }

    private RenderedValue renderNode(
            WidgetNode node,
            String path,
            int baseIndent,
            GenerationContext context) {
        WidgetDefinition definition = context.catalog().find(node.type())
                .orElseThrow(() -> abort(diagnostic(
                DartGenerationDiagnosticCode.INTERNAL_CATALOG_INCONSISTENCY,
                path,
                Optional.of(node.id()),
                    Optional.empty(),
                    "Validated widget type '" + node.type().value()
                    + "' disappeared from the generation catalog.")));
        boolean textField = node.type().equals(
                TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE);
        boolean listView = node.type().equals(
                ListViewWidgetPropertySchema.LIST_VIEW_TYPE);
        boolean gridView = node.type().equals(
                GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE);
        int constructorBaseIndent = textField || listView || gridView
                ? baseIndent + 4 : baseIndent;

        ArrayList<ConstructorArgument> arguments = new ArrayList<>();
        for (PropertyDefinition property : definition.properties()) {
            if (node.type().equals(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE)
                    && ScaffoldWidgetPropertySchema.isStaticPreset(property.name())) {
                continue;
            }
            if (node.type().equals(TextWidgetPropertySchema.TEXT_TYPE)
                    && TextWidgetPropertySchema.isCompound(property.name())) {
                continue;
            }
            if (node.type().equals(AppBarWidgetPropertySchema.APP_BAR_TYPE)
                    && AppBarWidgetPropertySchema.isCompound(property.name())) {
                continue;
            }
            if (node.type().equals(ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE)
                    && ElevatedButtonWidgetPropertySchema.isCompound(property.name())) {
                continue;
            }
            if (node.type().value().equals("flutter.widgets.Image")
                    && isImageSynthesizedProperty(property.name())) {
                continue;
            }
            if (node.type().equals(TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE)
                    && TextFieldWidgetPropertySchema.isSynthesized(property.name())) {
                continue;
            }
            if (node.type().equals(ListViewWidgetPropertySchema.LIST_VIEW_TYPE)
                    && ListViewWidgetPropertySchema.isSynthesized(property.name())) {
                continue;
            }
            if (node.type().equals(GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE)
                    && GridViewCountWidgetPropertySchema.isSynthesized(property.name())) {
                continue;
            }
            PropertyValue value = node.properties().get(property.name());
            if (value != null) {
                String propertyPath = path + "/properties/" + pointer(property.name().value());
                arguments.add(new ConstructorArgument(
                        property.parameter(),
                        property.name().value(),
                        false,
                        renderProperty(
                                value, property, propertyPath, node.id(), context,
                                constructorBaseIndent + 2)));
            }
        }
        for (SlotDefinition slot : definition.slots()) {
            WidgetSlot value = node.slots().get(slot.name());
            if (value != null) {
                if (node.type().value().equals("flutter.widgets.Transform")
                        && !slot.parameter().required()
                        && value instanceof WidgetSlot.SingleSlot single
                        && single.child().isEmpty()) {
                    // Preserve Transform.new's omitted nullable child in generated Dart.
                    continue;
                }
                String slotPath = path + "/slots/" + pointer(slot.name().value());
                arguments.add(new ConstructorArgument(
                        slot.parameter(),
                        slot.name().value(),
                        true,
                    renderSlot(value, slotPath, constructorBaseIndent + 2, context)));
            }
        }
        if (node.type().equals(TextWidgetPropertySchema.TEXT_TYPE)) {
            appendTextCompoundArguments(
                    node, definition, path, constructorBaseIndent + 2,
                    context, arguments);
        }
        if (node.type().equals(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE)) {
            appendScaffoldStaticPresetArguments(
                    node, definition, path, context, arguments);
        }
        if (node.type().equals(AppBarWidgetPropertySchema.APP_BAR_TYPE)) {
            appendAppBarCompoundArguments(
                    node, definition, path, constructorBaseIndent + 2,
                    context, arguments);
        }
        if (node.type().equals(ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE)) {
            appendElevatedButtonCompoundArguments(
                    node, definition, path, constructorBaseIndent + 2,
                    context, arguments);
        }
        if (node.type().value().equals("flutter.widgets.Image")) {
            appendImageSynthesizedArguments(
                    node, definition, path, context, arguments);
        }
        if (node.type().equals(TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE)) {
            appendTextFieldSynthesizedArguments(
                    node, definition, path, context, arguments);
        }
        if (node.type().equals(ListViewWidgetPropertySchema.LIST_VIEW_TYPE)) {
            appendStaticScrollViewSynthesizedArguments(
                    node, definition, path, context, arguments);
        }
        if (node.type().equals(GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE)) {
            appendStaticScrollViewSynthesizedArguments(
                    node, definition, path, context, arguments);
        }
        arguments.sort(ARGUMENT_ORDER);

        boolean constant = definition.constConstructor()
                && arguments.stream().allMatch(value -> value.value().constant());
        RenderedSymbol renderedClass = context.planner().renderedSymbol(
                definition.dartLibraryUri(), definition.dartClassName());
        String constructor = (constant ? "const " : "") + renderedClass.text();
        GeneratedDartSymbolOccurrence classOccurrence = occurrence(
                "widget:" + node.id() + ":constructor",
                (constant ? "const ".length() : 0) + renderedClass.nameOffset(),
                renderedClass.name(),
                renderedClass.libraryUri(),
                path,
                Optional.of(node.id()));
        if (definition.namedConstructor().isPresent()) {
            constructor += "." + definition.namedConstructor().orElseThrow();
        }
        if (arguments.isEmpty()) {
            RenderedValue rendered = scalar(
                    constructor + "()",
                    constant,
                    path,
                    node.id(),
                    context,
                    List.of(classOccurrence));
            return wrapConstraintGuardIfNeeded(
                    node, path, baseIndent, context, rendered);
        }

        if (arguments.stream().noneMatch(ConstructorArgument::slot)
                && arguments.stream().allMatch(value -> value.value().lines().size() == 1)) {
            StringBuilder inline = new StringBuilder(constructor).append('(');
            ArrayList<GeneratedDartSymbolOccurrence> inlineOccurrences =
                    new ArrayList<>();
            inlineOccurrences.add(classOccurrence);
            for (int index = 0; index < arguments.size(); index++) {
                if (index > 0) {
                    inline.append(", ");
                }
                ConstructorArgument argument = arguments.get(index);
                if (argument.parameter().style() == ParameterStyle.NAMED) {
                    inline.append(argument.name()).append(": ");
                }
                int valueOffset = inline.length();
                inline.append(argument.value().lines().getFirst());
                shiftInto(
                        inlineOccurrences,
                        argument.value().symbolOccurrences(),
                        valueOffset);
            }
            inline.append(')');
            if (inline.codePointCount(0, inline.length()) <= INLINE_CONSTRUCTOR_LIMIT) {
                RenderedValue rendered = scalar(
                        inline.toString(),
                        constant,
                        path,
                        node.id(),
                        context,
                        inlineOccurrences);
                return wrapConstraintGuardIfNeeded(
                        node, path, baseIndent, context, rendered);
            }
        }

        LineAccumulator lines = new LineAccumulator(
                context.maxRenderedUtf8Bytes(), path, node.id());
        lines.add(constructor + "(", List.of(classOccurrence));
        int argumentIndent = constructorBaseIndent + 2;
        for (ConstructorArgument argument : arguments) {
            List<String> valueLines = argument.value().lines();
            String prefix = argument.parameter().style() == ParameterStyle.NAMED
                    ? argument.name() + ": "
                    : "";
            String emitted = spaces(argumentIndent)
                    + prefix
                    + argument.value().joined()
                    + ',';
            lines.addBlock(
                    emitted,
                    argument.value().symbolOccurrences(),
                    spaces(argumentIndent).length() + prefix.length());
        }
        lines.add(spaces(constructorBaseIndent) + ')');
        RenderedValue rendered = lines.build(constant);
        return wrapConstraintGuardIfNeeded(
                node, path, baseIndent, context, rendered);
    }

    private RenderedValue wrapConstraintGuardIfNeeded(
            WidgetNode node,
            String path,
            int baseIndent,
            GenerationContext context,
            RenderedValue rendered) {
        if (node.type().equals(TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE)) {
            return wrapTextFieldConstraintGuard(
                    node, path, baseIndent, context, rendered);
        }
        if (isStaticScrollView(node)) {
            return wrapStaticScrollViewConstraintGuard(
                    node, path, baseIndent, context, rendered);
        }
        return rendered;
    }

    /**
     * Preserves the modeled TextField while supplying finite fallback axes
     * only when arbitrary ancestor wrappers propagate unbounded constraints.
     */
    private RenderedValue wrapTextFieldConstraintGuard(
            WidgetNode node,
            String path,
            int baseIndent,
            GenerationContext context,
            RenderedValue field) {
        RenderedSymbol layoutBuilder = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "LayoutBuilder");
        RenderedSymbol sizedBox = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "SizedBox");
        LineAccumulator lines = new LineAccumulator(
                context.maxRenderedUtf8Bytes(), path, node.id());
        lines.add(
                layoutBuilder.text() + '(',
                List.of(occurrence(
                        "widget:" + node.id() + ":textfield-guard:layout-builder",
                        layoutBuilder.nameOffset(),
                        layoutBuilder.name(),
                        layoutBuilder.libraryUri(),
                        path,
                        Optional.of(node.id()))));

        String builderPrefix = spaces(baseIndent + 2)
                + "builder: (_, constraints) => ";
        lines.add(
                builderPrefix + sizedBox.text() + '(',
                List.of(occurrence(
                        "widget:" + node.id() + ":textfield-guard:sized-box",
                        builderPrefix.length() + sizedBox.nameOffset(),
                        sizedBox.name(),
                        sizedBox.libraryUri(),
                        path,
                        Optional.of(node.id()))));
        int argumentIndent = baseIndent + 4;
        lines.add(spaces(argumentIndent)
                + "width: constraints.hasBoundedWidth ? null : 240,");
        if (Boolean.TRUE.equals(textFieldBoolean(node, "expands"))) {
            lines.add(spaces(argumentIndent)
                    + "height: constraints.hasBoundedHeight ? null : 120,");
        }
        String childPrefix = spaces(argumentIndent) + "child: ";
        lines.addBlock(
                childPrefix + field.joined() + ',',
                field.symbolOccurrences(),
                childPrefix.length());
        lines.add(spaces(baseIndent + 2) + "),");
        lines.add(spaces(baseIndent) + ')');
        return lines.build(false);
    }

    /**
     * Keeps a modeled static ListView/GridView valid under arbitrary flex
     * ancestors. The viewport always needs a bounded cross axis; a
     * non-shrink-wrapped scroll view additionally needs a bounded main axis.
     */
    private RenderedValue wrapStaticScrollViewConstraintGuard(
            WidgetNode node,
            String path,
            int baseIndent,
            GenerationContext context,
            RenderedValue listView) {
        RenderedSymbol layoutBuilder = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "LayoutBuilder");
        RenderedSymbol sizedBox = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "SizedBox");
        boolean horizontal = node.properties().get(
                new PropertyName("scrollDirection"))
                instanceof PropertyValue.EnumValue direction
                && "horizontal".equals(direction.value());
        boolean shrinkWrap = node.properties().get(
                new PropertyName("shrinkWrap"))
                instanceof PropertyValue.BooleanValue value
                && value.value();
        boolean guardWidth = !horizontal || !shrinkWrap;
        boolean guardHeight = horizontal || !shrinkWrap;

        LineAccumulator lines = new LineAccumulator(
                context.maxRenderedUtf8Bytes(), path, node.id());
        lines.add(
                layoutBuilder.text() + '(',
                List.of(occurrence(
                        "widget:" + node.id() + ":list-view-guard:layout-builder",
                        layoutBuilder.nameOffset(),
                        layoutBuilder.name(),
                        layoutBuilder.libraryUri(),
                        path,
                        Optional.of(node.id()))));
        String builderPrefix = spaces(baseIndent + 2)
                + "builder: (_, constraints) => ";
        lines.add(
                builderPrefix + sizedBox.text() + '(',
                List.of(occurrence(
                        "widget:" + node.id() + ":list-view-guard:sized-box",
                        builderPrefix.length() + sizedBox.nameOffset(),
                        sizedBox.name(),
                        sizedBox.libraryUri(),
                        path,
                        Optional.of(node.id()))));
        int argumentIndent = baseIndent + 4;
        if (guardWidth) {
            lines.add(spaces(argumentIndent)
                    + "width: constraints.hasBoundedWidth ? null : 240,");
        }
        if (guardHeight) {
            lines.add(spaces(argumentIndent)
                    + "height: constraints.hasBoundedHeight ? null : 120,");
        }
        String childPrefix = spaces(argumentIndent) + "child: ";
        lines.addBlock(
                childPrefix + listView.joined() + ',',
                listView.symbolOccurrences(),
                childPrefix.length());
        lines.add(spaces(baseIndent + 2) + "),");
        lines.add(spaces(baseIndent) + ')');
        return lines.build(false);
    }

    private void appendScaffoldStaticPresetArguments(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        for (PropertyDefinition property : definition.properties()) {
            ScaffoldWidgetPropertySchema.Definition binding =
                    ScaffoldWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Scaffold catalog property has no schema binding: "
                                    + property.name().value()));
            if (binding.target() == ScaffoldWidgetPropertySchema.Target.DIRECT) {
                continue;
            }
            PropertyValue value = node.properties().get(property.name());
            if (value == null) {
                continue;
            }
            String propertyPath = path + "/properties/" + pointer(property.name().value());
            if (!(value instanceof PropertyValue.StringValue preset)
                    || !binding.presets().contains(preset.value())) {
                throw catalogInconsistency(
                        propertyPath, node.id(),
                        "Scaffold static preset is not part of the reviewed closed set.");
            }
            RenderedSymbol type = context.planner().renderedSymbol(
                    MATERIAL_IMPORT, binding.dartType());
            arguments.add(new ConstructorArgument(
                    property.parameter(),
                    property.name().value(),
                    false,
                    scalar(
                            type.text() + "." + preset.value(),
                            true,
                            propertyPath,
                            node.id(),
                            context,
                            List.of(occurrence(
                                    "widget:" + node.id() + ":scaffold-preset:"
                                    + property.name().value(),
                                    type.nameOffset(),
                                    type.name(),
                                    type.libraryUri(),
                                    propertyPath,
                                    Optional.of(node.id()))))));
        }
    }

    private void appendElevatedButtonCompoundArguments(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            int valueIndent,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        appendElevatedButtonCallbacks(node, definition, path, context, arguments);

        LinkedHashMap<String, Map<String, RenderedValue>> states = new LinkedHashMap<>();
        for (ElevatedButtonState state : ELEVATED_BUTTON_STATES) {
            states.put(state.key(), renderElevatedButtonState(
                    node, definition, state.prefix(), path, valueIndent + 2, context));
        }

        ArrayList<CompositeMember> style = new ArrayList<>();
        RenderedValue textStyle = renderElevatedTextStyleStateProperty(
                node, definition, path, valueIndent + 2, context);
        if (textStyle != null) {
            style.add(new CompositeMember("textStyle", 0, textStyle));
        }
        appendElevatedStateProperty(
                style, states, "backgroundColor", "Color", 1,
                path, node.id(), context);
        appendElevatedStateProperty(
                style, states, "foregroundColor", "Color", 2,
                path, node.id(), context);
        appendElevatedStateProperty(
                style, states, "overlayColor", "Color", 3,
                path, node.id(), context);
        appendElevatedStateProperty(
                style, states, "shadowColor", "Color", 4,
                path, node.id(), context);
        appendElevatedStateProperty(
                style, states, "surfaceTintColor", "Color", 5,
                path, node.id(), context);
        appendElevatedStateProperty(
                style, states, "elevation", "double", 6,
                path, node.id(), context);
        appendElevatedStateProperty(
                style, states, "padding", "EdgeInsetsGeometry", 7,
                path, node.id(), context);
        ElevatedButtonBoundSizePair boundSizes =
                renderElevatedBoundSizeStateProperties(
                        node, definition, path, context);
        if (boundSizes != null) {
            style.add(new CompositeMember(
                    "minimumSize", 8, boundSizes.minimum()));
        }
        RenderedValue fixedSize = renderElevatedSizeStateProperty(
                node, definition, "Fixed", path, context);
        if (fixedSize != null) {
            style.add(new CompositeMember("fixedSize", 9, fixedSize));
        }
        if (boundSizes != null) {
            style.add(new CompositeMember(
                    "maximumSize", 10, boundSizes.maximum()));
        }
        appendElevatedStateProperty(
                style, states, "iconColor", "Color", 11,
                path, node.id(), context);
        appendElevatedStateProperty(
                style, states, "iconSize", "double", 12,
                path, node.id(), context);
        RenderedValue side = renderElevatedSideStateProperty(
                node, definition, path, context);
        if (side != null) {
            style.add(new CompositeMember("side", 14, side));
        }
        RenderedValue shape = renderElevatedShapeStateProperty(
                node, definition, path, context);
        if (shape != null) {
            style.add(new CompositeMember("shape", 15, shape));
        }
        appendElevatedStateProperty(
                style, states, "mouseCursor", "MouseCursor", 16,
                path, node.id(), context);

        appendElevatedButtonCommonStyle(
                node, definition, path, valueIndent + 2, context, style);
        if (!style.isEmpty()) {
            style.sort(COMPOSITE_MEMBER_ORDER);
            arguments.add(new ConstructorArgument(
                    DartParameter.named(4, false),
                    "style",
                    false,
                    renderNamedCompositeMembers(
                            MATERIAL_IMPORT, "ButtonStyle", Optional.empty(),
                            style, valueIndent, path + "/properties/style",
                            node.id(), context)));
        }
    }

    private void appendElevatedButtonCallbacks(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        boolean enabled = !(node.properties().get(new PropertyName("enabled"))
                instanceof PropertyValue.BooleanValue value) || value.value();
        PropertyDefinition onPressed = elevatedProperty(definition, "onPressed");
        PropertyDefinition onLongPress = elevatedProperty(definition, "onLongPress");
        PropertyValue pressedValue = node.properties().get(onPressed.name());
        PropertyValue longValue = node.properties().get(onLongPress.name());

        RenderedValue pressed;
        if (!enabled) {
            pressed = scalar("null", true,
                    path + "/properties/enabled", node.id(), context);
        } else if (pressedValue != null) {
            pressed = renderProperty(
                    pressedValue, onPressed,
                    path + "/properties/onPressed", node.id(), context);
        } else if (longValue != null) {
            pressed = scalar("null", true,
                    path + "/properties/onPressed", node.id(), context);
        } else {
            pressed = scalar("() {}", false,
                    path + "/properties/onPressed", node.id(), context);
        }
        arguments.add(new ConstructorArgument(
                DartParameter.named(0, true), "onPressed", false, pressed));

        if (!enabled) {
            arguments.add(new ConstructorArgument(
                    DartParameter.named(1, false), "onLongPress", false,
                    scalar("null", true,
                            path + "/properties/enabled", node.id(), context)));
        } else if (longValue != null) {
            arguments.add(new ConstructorArgument(
                    DartParameter.named(1, false), "onLongPress", false,
                    renderProperty(
                            longValue, onLongPress,
                            path + "/properties/onLongPress", node.id(), context)));
        }

    }

    private static PropertyDefinition elevatedProperty(
            WidgetDefinition definition,
            String name) {
        return definition.property(new PropertyName(name)).orElseThrow(() ->
                new IllegalStateException(
                        "ElevatedButton catalog is missing property " + name));
    }

    private Map<String, RenderedValue> renderElevatedButtonState(
            WidgetNode node,
            WidgetDefinition definition,
            String prefix,
            String path,
            int valueIndent,
            GenerationContext context) {
        LinkedHashMap<String, RenderedValue> values = new LinkedHashMap<>();
        putElevatedScalar(node, definition, prefix + "BackgroundColor",
                "backgroundColor", path, context, values);
        putElevatedScalar(node, definition, prefix + "ForegroundColor",
                "foregroundColor", path, context, values);
        putElevatedScalar(node, definition, prefix + "OverlayColor",
                "overlayColor", path, context, values);
        putElevatedScalar(node, definition, prefix + "ShadowColor",
                "shadowColor", path, context, values);
        putElevatedScalar(node, definition, prefix + "SurfaceTintColor",
                "surfaceTintColor", path, context, values);
        putElevatedScalar(node, definition, prefix + "Elevation",
                "elevation", path, context, values);
        putElevatedScalar(node, definition, prefix + "Padding",
                "padding", path, context, values);
        putElevatedScalar(node, definition, prefix + "IconColor",
                "iconColor", path, context, values);
        putElevatedScalar(node, definition, prefix + "IconSize",
                "iconSize", path, context, values);

        putIfPresent(values, "mouseCursor", renderElevatedCursor(
                node, definition, prefix + "MouseCursor", path, context));
        return Map.copyOf(values);
    }

    private void putElevatedScalar(
            WidgetNode node,
            WidgetDefinition definition,
            String propertyName,
            String fieldName,
            String path,
            GenerationContext context,
            Map<String, RenderedValue> values) {
        PropertyDefinition property = elevatedProperty(definition, propertyName);
        PropertyValue value = node.properties().get(property.name());
        if (value != null) {
            values.put(fieldName, renderProperty(
                    value, property,
                    path + "/properties/" + pointer(propertyName),
                    node.id(), context));
        }
    }

    private static void putIfPresent(
            Map<String, RenderedValue> values,
            String name,
            RenderedValue value) {
        if (value != null) {
            values.put(name, value);
        }
    }

    private void appendElevatedStateProperty(
            List<CompositeMember> style,
            Map<String, Map<String, RenderedValue>> states,
            String dartName,
            String valueType,
            int order,
            String path,
            StableId widgetId,
            GenerationContext context) {
        Map<String, RenderedValue> disabled = states.get("disabled");
        Map<String, RenderedValue> pressed = states.get("pressed");
        Map<String, RenderedValue> hovered = states.get("hovered");
        Map<String, RenderedValue> focused = states.get("focused");
        Map<String, RenderedValue> fallback = states.get("any");
        RenderedValue disabledValue = disabled.get(dartName);
        RenderedValue pressedValue = pressed.get(dartName);
        RenderedValue hoveredValue = hovered.get(dartName);
        RenderedValue focusedValue = focused.get(dartName);
        RenderedValue fallbackValue = fallback.get(dartName);
        if (disabledValue == null
                && pressedValue == null
                && hoveredValue == null
                && focusedValue == null
                && fallbackValue == null) {
            return;
        }

        ArrayList<ElevatedButtonStateEntry> entries = new ArrayList<>();
        if (disabledValue != null) {
            entries.add(new ElevatedButtonStateEntry("disabled", disabledValue));
        } else {
            // Every locally configured enabled-state value is guarded. A
            // disabled widget can simultaneously retain focus/hover in
            // Flutter, so omitting this exact null would let a lower-priority
            // enabled-state entry leak into disabled rendering.
            entries.add(new ElevatedButtonStateEntry(
                    "disabled",
                    scalar("null", true, path + "/properties/styleDisabled",
                            widgetId, context)));
        }
        if (pressedValue != null) {
            entries.add(new ElevatedButtonStateEntry("pressed", pressedValue));
        }
        if (hoveredValue != null) {
            entries.add(new ElevatedButtonStateEntry("hovered", hoveredValue));
        }
        if (focusedValue != null) {
            entries.add(new ElevatedButtonStateEntry("focused", focusedValue));
        }
        if (fallbackValue != null) {
            entries.add(new ElevatedButtonStateEntry("any", fallbackValue));
        }
        style.add(new CompositeMember(
                dartName,
                order,
                renderElevatedStateMap(
                        dartName, valueType, entries,
                        path + "/properties/style/" + dartName,
                        widgetId, context)));
    }

    private RenderedValue renderElevatedStateMap(
            String propertyName,
            String valueType,
            List<ElevatedButtonStateEntry> entries,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol propertyType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetStateProperty");
        RenderedSymbol constraintType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetStatesConstraint");
        RenderedSymbol stateType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetState");
        RenderedSymbol valueSymbol = valueType.equals("double")
                ? null
                : context.planner().renderedSymbol(WIDGETS_IMPORT, valueType);

        StringBuilder rendered = new StringBuilder();
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedSymbol(
                rendered, occurrences, propertyType,
                "widget:" + widgetId + ":button-style:" + propertyName
                + ":state-property", path, widgetId);
        rendered.append('<');
        appendElevatedType(
                rendered, occurrences, valueType, valueSymbol,
                "widget:" + widgetId + ":button-style:" + propertyName
                + ":value-type-1", path, widgetId);
        rendered.append("?>.fromMap(<");
        appendElevatedSymbol(
                rendered, occurrences, constraintType,
                "widget:" + widgetId + ":button-style:" + propertyName
                + ":constraint-type", path, widgetId);
        rendered.append(", ");
        appendElevatedType(
                rendered, occurrences, valueType, valueSymbol,
                "widget:" + widgetId + ":button-style:" + propertyName
                + ":value-type-2", path, widgetId);
        rendered.append("?>{");
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                rendered.append(", ");
            }
            ElevatedButtonStateEntry entry = entries.get(index);
            appendElevatedSymbol(
                    rendered, occurrences, stateType,
                    "widget:" + widgetId + ":button-style:" + propertyName
                    + ":state:" + entry.state(), path, widgetId);
            rendered.append('.').append(entry.state()).append(": ");
            appendRendered(rendered, occurrences, entry.value());
        }
        rendered.append("})");
        return scalar(rendered.toString(), false, path, widgetId, context, occurrences);
    }

    private static void appendElevatedType(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            String valueType,
            RenderedSymbol symbol,
            String occurrenceId,
            String path,
            StableId widgetId) {
        if (symbol == null) {
            rendered.append(valueType);
        } else {
            appendElevatedSymbol(
                    rendered, occurrences, symbol, occurrenceId, path, widgetId);
        }
    }

    private static void appendElevatedSymbol(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            RenderedSymbol symbol,
            String occurrenceId,
            String path,
            StableId widgetId) {
        int offset = rendered.length();
        rendered.append(symbol.text());
        occurrences.add(occurrence(
                occurrenceId,
                offset + symbol.nameOffset(),
                symbol.name(),
                symbol.libraryUri(),
                path,
                Optional.of(widgetId)));
    }

    private ElevatedButtonBoundSizePair renderElevatedBoundSizeStateProperties(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context) {
        LinkedHashMap<String, ElevatedButtonLayer> layers = elevatedLayers(
                node, definition, path, context,
                List.of(
                        "MinimumWidth", "MinimumHeight",
                        "MaximumWidth", "MaximumHeight"));
        if (layers.values().stream().noneMatch(ElevatedButtonLayer::configured)) {
            return null;
        }
        return new ElevatedButtonBoundSizePair(
                renderElevatedBoundSizeStateProperty(
                        node, layers, false, path, context),
                renderElevatedBoundSizeStateProperty(
                        node, layers, true, path, context));
    }

    private RenderedValue renderElevatedBoundSizeStateProperty(
            WidgetNode node,
            Map<String, ElevatedButtonLayer> layers,
            boolean returnMaximum,
            String path,
            GenerationContext context) {
        String dartField = returnMaximum ? "maximumSize" : "minimumSize";
        String propertyPath = path + "/properties/style/" + dartField;
        RenderedSymbol propertyType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetStateProperty");
        RenderedSymbol sizeType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "Size");
        RenderedSymbol stateType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetState");
        StringBuilder rendered = new StringBuilder();
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedSymbol(
                rendered, occurrences, propertyType,
                "widget:" + node.id() + ":button-style:" + dartField
                + ":paired-resolver",
                propertyPath, node.id());
        rendered.append(".resolveWith<");
        appendElevatedSymbol(
                rendered, occurrences, sizeType,
                "widget:" + node.id() + ":button-style:" + dartField
                + ":paired-value-type",
                propertyPath, node.id());
        rendered.append("?>((states) {\n");
        appendElevatedFrameworkDefaultsDeclaration(
                rendered, occurrences, propertyPath, node.id(), context,
                "paired-" + dartField);
        appendElevatedInheritedBoundSize(
                rendered, occurrences, sizeType, node.id(), context,
                propertyPath, dartField, "minimumSize", "inheritedMinimum");
        appendElevatedInheritedBoundSize(
                rendered, occurrences, sizeType, node.id(), context,
                propertyPath, dartField, "maximumSize", "inheritedMaximum");

        rendered.append("  if (states.contains(");
        appendElevatedSymbol(
                rendered, occurrences, stateType,
                "widget:" + node.id() + ":button-style:" + dartField
                + ":paired-disabled-state",
                propertyPath, node.id());
        rendered.append(".disabled)) {\n");
        ElevatedButtonLayer disabled = layers.get("disabled");
        if (disabled.configured()) {
            appendElevatedBoundSizeResolution(
                    rendered, occurrences, node.id(), layers, disabled,
                    true, returnMaximum, sizeType, stateType,
                    propertyPath, dartField, "disabled");
        } else {
            rendered.append("    return null;\n");
        }
        rendered.append("  }\n");

        ElevatedButtonLayer base = layers.get("any");
        List<String> configuredStates = List.of("focused", "hovered", "pressed")
                .stream()
                .filter(state -> layers.get(state).configured())
                .toList();
        if (!base.configured() && configuredStates.isEmpty()) {
            rendered.append("  return null;\n})");
            return scalar(rendered.toString(), false, propertyPath,
                    node.id(), context, occurrences);
        }
        if (!base.configured()) {
            rendered.append("  if (");
            for (int index = 0; index < configuredStates.size(); index++) {
                if (index > 0) {
                    rendered.append(" && ");
                }
                String state = configuredStates.get(index);
                rendered.append("!states.contains(");
                appendElevatedSymbol(
                        rendered, occurrences, stateType,
                        "widget:" + node.id() + ":button-style:" + dartField
                        + ":paired-applicability:" + state,
                        propertyPath, node.id());
                rendered.append('.').append(state).append(')');
            }
            rendered.append(") {\n    return null;\n  }\n");
        }
        appendElevatedBoundSizeResolution(
                rendered, occurrences, node.id(), layers, null,
                false, returnMaximum, sizeType, stateType,
                propertyPath, dartField, "enabled");
        rendered.append("})");
        return scalar(rendered.toString(), false, propertyPath,
                node.id(), context, occurrences);
    }

    private void appendElevatedInheritedBoundSize(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            RenderedSymbol sizeType,
            StableId widgetId,
            GenerationContext context,
            String path,
            String resolverField,
            String inheritedField,
            String variable) {
        rendered.append("  final ");
        appendElevatedSymbol(
                rendered, occurrences, sizeType,
                "widget:" + widgetId + ":button-style:" + resolverField
                + ":paired-inherited-type:" + inheritedField,
                path, widgetId);
        rendered.append("? ").append(variable).append(" = ");
        appendElevatedInheritedStateValue(
                rendered, occurrences, inheritedField, path, widgetId, context,
                "frameworkDefaults");
        rendered.append(";\n");
    }

    private void appendElevatedBoundSizeResolution(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            StableId widgetId,
            Map<String, ElevatedButtonLayer> layers,
            ElevatedButtonLayer isolated,
            boolean disabled,
            boolean returnMaximum,
            RenderedSymbol sizeType,
            RenderedSymbol stateType,
            String path,
            String resolverField,
            String branch) {
        String indent = disabled ? "    " : "  ";
        rendered.append(indent).append("final effectiveMinimum = ");
        appendElevatedSymbol(
                rendered, occurrences, sizeType,
                "widget:" + widgetId + ":button-style:" + resolverField
                + ":paired-minimum:" + branch,
                path, widgetId);
        rendered.append('(');
        appendElevatedBoundLeaf(
                rendered, occurrences, layers, isolated,
                "MinimumWidth", "inheritedMinimum?.width", "0.0",
                stateType, widgetId, path,
                resolverField + "-minimum-width", disabled);
        rendered.append(", ");
        appendElevatedBoundLeaf(
                rendered, occurrences, layers, isolated,
                "MinimumHeight", "inheritedMinimum?.height", "0.0",
                stateType, widgetId, path,
                resolverField + "-minimum-height", disabled);
        rendered.append(");\n");

        rendered.append(indent).append("final unresolvedMaximum = ");
        appendElevatedSymbol(
                rendered, occurrences, sizeType,
                "widget:" + widgetId + ":button-style:" + resolverField
                + ":paired-unresolved-maximum:" + branch,
                path, widgetId);
        rendered.append('(');
        appendElevatedBoundLeaf(
                rendered, occurrences, layers, isolated,
                "MaximumWidth", "inheritedMaximum?.width", "double.infinity",
                stateType, widgetId, path,
                resolverField + "-maximum-width", disabled);
        rendered.append(", ");
        appendElevatedBoundLeaf(
                rendered, occurrences, layers, isolated,
                "MaximumHeight", "inheritedMaximum?.height", "double.infinity",
                stateType, widgetId, path,
                resolverField + "-maximum-height", disabled);
        rendered.append(");\n");

        rendered.append(indent).append("final effectiveMaximum = ");
        appendElevatedSymbol(
                rendered, occurrences, sizeType,
                "widget:" + widgetId + ":button-style:" + resolverField
                + ":paired-effective-maximum:" + branch,
                path, widgetId);
        rendered.append("(\n")
                .append(indent).append("  unresolvedMaximum.width ")
                .append("< effectiveMinimum.width\n")
                .append(indent).append("      ? effectiveMinimum.width\n")
                .append(indent).append("      : unresolvedMaximum.width,\n")
                .append(indent).append("  unresolvedMaximum.height ")
                .append("< effectiveMinimum.height\n")
                .append(indent).append("      ? effectiveMinimum.height\n")
                .append(indent).append("      : unresolvedMaximum.height,\n")
                .append(indent).append(");\n")
                .append(indent).append("return ")
                .append(returnMaximum ? "effectiveMaximum" : "effectiveMinimum")
                .append(";\n");
    }

    private static void appendElevatedBoundLeaf(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            Map<String, ElevatedButtonLayer> layers,
            ElevatedButtonLayer isolated,
            String suffix,
            String inherited,
            String terminalFallback,
            RenderedSymbol stateType,
            StableId widgetId,
            String path,
            String leafId,
            boolean disabled) {
        if (disabled) {
            appendElevatedLeafFallback(
                    rendered, occurrences, isolated.values().get(suffix), null,
                    inherited, terminalFallback);
            return;
        }
        appendElevatedLayeredStateLeaf(
                rendered, occurrences, layers, suffix, inherited,
                terminalFallback, stateType, widgetId, path, leafId);
    }

    private RenderedValue renderElevatedSizeStateProperty(
            WidgetNode node,
            WidgetDefinition definition,
            String sizeKind,
            String path,
            GenerationContext context) {
        LinkedHashMap<String, ElevatedButtonLayer> layers = elevatedLayers(
                node, definition, path, context,
                List.of(sizeKind + "Width", sizeKind + "Height"));
        if (layers.values().stream().noneMatch(ElevatedButtonLayer::configured)) {
            return null;
        }

        String dartField = Character.toLowerCase(sizeKind.charAt(0))
                + sizeKind.substring(1) + "Size";
        String propertyPath = path + "/properties/style/" + dartField;
        RenderedSymbol propertyType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetStateProperty");
        RenderedSymbol sizeType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "Size");
        RenderedSymbol stateType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetState");
        StringBuilder rendered = new StringBuilder();
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedSymbol(
                rendered, occurrences, propertyType,
                "widget:" + node.id() + ":button-style:" + dartField + ":resolver",
                propertyPath, node.id());
        rendered.append(".resolveWith<");
        appendElevatedSymbol(
                rendered, occurrences, sizeType,
                "widget:" + node.id() + ":button-style:" + dartField + ":value-type",
                propertyPath, node.id());
        rendered.append("?>((states) {\n");
        appendElevatedFrameworkDefaultsDeclaration(
                rendered, occurrences, propertyPath, node.id(), context,
                dartField);
        rendered.append("  final ");
        appendElevatedSymbol(
                rendered, occurrences, sizeType,
                "widget:" + node.id() + ":button-style:" + dartField + ":inherited-type",
                propertyPath, node.id());
        rendered.append("? inherited = ");
        appendElevatedInheritedStateValue(
                rendered, occurrences, dartField, propertyPath, node.id(), context,
                "frameworkDefaults");
        rendered.append(";\n  if (states.contains(");
        appendElevatedSymbol(
                rendered, occurrences, stateType,
                "widget:" + node.id() + ":button-style:" + dartField + ":disabled-state",
                propertyPath, node.id());
        rendered.append(".disabled)) {\n");
        ElevatedButtonLayer disabled = layers.get("disabled");
        if (disabled.configured()) {
            appendElevatedSizeReturn(
                    rendered, occurrences, node, sizeType, sizeKind,
                    disabled, null, propertyPath, context, "disabled");
        } else {
            rendered.append("    return inherited;\n");
        }
        rendered.append("  }\n");
        rendered.append("  return ");
        appendElevatedSymbol(
                rendered, occurrences, sizeType,
                "widget:" + node.id() + ":button-size:" + sizeKind + ":enabled",
                propertyPath, node.id());
        rendered.append('(');
        appendElevatedLayeredStateLeaf(
                rendered, occurrences, layers, sizeKind + "Width",
                "inherited?.width",
                sizeKind.equals("Minimum") ? "0.0" : "double.infinity",
                stateType, node.id(), propertyPath, "width");
        rendered.append(", ");
        appendElevatedLayeredStateLeaf(
                rendered, occurrences, layers, sizeKind + "Height",
                "inherited?.height",
                sizeKind.equals("Minimum") ? "0.0" : "double.infinity",
                stateType, node.id(), propertyPath, "height");
        rendered.append(");\n");
        rendered.append("})");
        return scalar(rendered.toString(), false, propertyPath,
                node.id(), context, occurrences);
    }

    private void appendElevatedSizeReturn(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            WidgetNode node,
            RenderedSymbol sizeType,
            String sizeKind,
            ElevatedButtonLayer selected,
            ElevatedButtonLayer base,
            String path,
            GenerationContext context,
            String branch) {
        String fallback = sizeKind.equals("Minimum") ? "0.0" : "double.infinity";
        rendered.append("    return ");
        appendElevatedSymbol(
                rendered, occurrences, sizeType,
                "widget:" + node.id() + ":button-size:" + sizeKind + ':' + branch,
                path, node.id());
        rendered.append('(');
        appendElevatedLeafFallback(
                rendered, occurrences, selected.values().get(sizeKind + "Width"),
                base == null ? null : base.values().get(sizeKind + "Width"),
                "inherited?.width", fallback);
        rendered.append(", ");
        appendElevatedLeafFallback(
                rendered, occurrences, selected.values().get(sizeKind + "Height"),
                base == null ? null : base.values().get(sizeKind + "Height"),
                "inherited?.height", fallback);
        rendered.append(");\n");
    }

    private RenderedValue renderElevatedSideStateProperty(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context) {
        List<String> suffixes = List.of(
                "SideColor", "SideWidth", "SideStyle", "SideStrokeAlign");
        LinkedHashMap<String, ElevatedButtonLayer> layers = elevatedLayers(
                node, definition, path, context, suffixes);
        if (layers.values().stream().noneMatch(ElevatedButtonLayer::configured)) {
            return null;
        }

        String propertyPath = path + "/properties/style/side";
        RenderedSymbol propertyType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetStateProperty");
        RenderedSymbol sideType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "BorderSide");
        RenderedSymbol stateType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetState");
        StringBuilder rendered = new StringBuilder();
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedSymbol(
                rendered, occurrences, propertyType,
                "widget:" + node.id() + ":button-style:side:resolver",
                propertyPath, node.id());
        rendered.append(".resolveWith<");
        appendElevatedSymbol(
                rendered, occurrences, sideType,
                "widget:" + node.id() + ":button-style:side:value-type",
                propertyPath, node.id());
        rendered.append("?>((states) {\n");
        appendElevatedFrameworkDefaultsDeclaration(
                rendered, occurrences, propertyPath, node.id(), context,
                "side");
        rendered.append("  final ");
        appendElevatedSymbol(
                rendered, occurrences, sideType,
                "widget:" + node.id() + ":button-style:side:inherited-type",
                propertyPath, node.id());
        rendered.append("? inherited = ");
        appendElevatedInheritedStateValue(
                rendered, occurrences, "side", propertyPath, node.id(), context,
                "frameworkDefaults");
        rendered.append(";\n  final ");
        RenderedSymbol shapeType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "OutlinedBorder");
        appendElevatedSymbol(
                rendered, occurrences, shapeType,
                "widget:" + node.id() + ":button-style:side:inherited-shape-type",
                propertyPath, node.id());
        rendered.append("? inheritedShape = ");
        appendElevatedInheritedStateValue(
                rendered, occurrences, "shape", propertyPath, node.id(), context,
                "frameworkDefaults");
        rendered.append(";\n  if (states.contains(");
        appendElevatedSymbol(
                rendered, occurrences, stateType,
                "widget:" + node.id() + ":button-style:side:disabled-state",
                propertyPath, node.id());
        rendered.append(".disabled)) {\n");
        ElevatedButtonLayer disabled = layers.get("disabled");
        if (disabled.configured()) {
            appendElevatedSideReturn(
                    rendered, occurrences, node, sideType,
                    disabled, null, propertyPath, "disabled");
        } else {
            rendered.append("    return inherited;\n");
        }
        rendered.append("  }\n");

        rendered.append("  var resolved = inherited ?? inheritedShape?.side ?? ");
        appendElevatedSymbol(
                rendered, occurrences, sideType,
                "widget:" + node.id() + ":button-side:fallback:enabled",
                propertyPath, node.id());
        rendered.append(".none;\n");
        ElevatedButtonLayer base = layers.get("any");
        if (base.configured()) {
            appendElevatedSideAssignment(
                    rendered, occurrences, base, propertyPath, "any", node.id());
        }
        for (String state : List.of("focused", "hovered", "pressed")) {
            ElevatedButtonLayer layer = layers.get(state);
            if (!layer.configured()) {
                continue;
            }
            rendered.append("  if (states.contains(");
            appendElevatedSymbol(
                    rendered, occurrences, stateType,
                    "widget:" + node.id() + ":button-style:side:" + state + "-state",
                    propertyPath, node.id());
            rendered.append('.').append(state).append(")) {\n");
            appendElevatedSideAssignment(
                    rendered, occurrences, layer,
                    propertyPath, state, node.id());
            rendered.append("  }\n");
        }
        rendered.append("  return resolved;\n");
        rendered.append("})");
        return scalar(rendered.toString(), false, propertyPath,
                node.id(), context, occurrences);
    }

    private void appendElevatedSideReturn(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            WidgetNode node,
            RenderedSymbol sideType,
            ElevatedButtonLayer selected,
            ElevatedButtonLayer base,
            String path,
            String branch) {
        rendered.append("    return (inherited ?? inheritedShape?.side ?? ");
        appendElevatedSymbol(
                rendered, occurrences, sideType,
                "widget:" + node.id() + ":button-side:fallback:" + branch,
                path, node.id());
        rendered.append(".none).copyWith(");
        appendElevatedCopyWithLeaf(
                rendered, occurrences, "color", "SideColor", selected, base);
        appendElevatedCopyWithLeaf(
                rendered, occurrences, "width", "SideWidth", selected, base);
        appendElevatedCopyWithLeaf(
                rendered, occurrences, "style", "SideStyle", selected, base);
        appendElevatedCopyWithLeaf(
                rendered, occurrences, "strokeAlign", "SideStrokeAlign", selected, base);
        trimTrailingCommaSpace(rendered);
        rendered.append(");\n");
    }

    private static void appendElevatedSideAssignment(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            ElevatedButtonLayer layer,
            String path,
            String branch,
            StableId widgetId) {
        rendered.append("    resolved = resolved.copyWith(");
        appendElevatedCopyWithLeaf(
                rendered, occurrences, "color", "SideColor", layer, null);
        appendElevatedCopyWithLeaf(
                rendered, occurrences, "width", "SideWidth", layer, null);
        appendElevatedCopyWithLeaf(
                rendered, occurrences, "style", "SideStyle", layer, null);
        appendElevatedCopyWithLeaf(
                rendered, occurrences, "strokeAlign", "SideStrokeAlign", layer, null);
        trimTrailingCommaSpace(rendered);
        rendered.append(");\n");
    }

    private static void appendElevatedLayeredStateLeaf(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            Map<String, ElevatedButtonLayer> layers,
            String suffix,
            String inherited,
            String terminalFallback,
            RenderedSymbol stateType,
            StableId widgetId,
            String path,
            String leafId) {
        for (String state : List.of("pressed", "hovered", "focused")) {
            RenderedValue value = layers.get(state).values().get(suffix);
            if (value == null) {
                continue;
            }
            rendered.append("states.contains(");
            appendElevatedSymbol(
                    rendered, occurrences, stateType,
                    "widget:" + widgetId + ":button-style:" + leafId
                    + ':' + state + "-state",
                    path, widgetId);
            rendered.append('.').append(state).append(") ? ");
            appendRendered(rendered, occurrences, value);
            rendered.append(" : ");
        }
        RenderedValue base = layers.get("any").values().get(suffix);
        if (base != null) {
            appendRendered(rendered, occurrences, base);
        } else {
            rendered.append(inherited).append(" ?? ").append(terminalFallback);
        }
    }

    private LinkedHashMap<String, ElevatedButtonLayer> elevatedLayers(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<String> suffixes) {
        LinkedHashMap<String, ElevatedButtonLayer> layers = new LinkedHashMap<>();
        for (ElevatedButtonState state : ELEVATED_BUTTON_STATES) {
            LinkedHashMap<String, RenderedValue> values = new LinkedHashMap<>();
            for (String suffix : suffixes) {
                String propertyName = state.prefix() + suffix;
                PropertyDefinition property = elevatedProperty(definition, propertyName);
                PropertyValue value = node.properties().get(property.name());
                if (value != null) {
                    values.put(suffix, renderProperty(
                            value, property,
                            path + "/properties/" + pointer(propertyName),
                            node.id(), context));
                }
            }
            layers.put(state.key(), new ElevatedButtonLayer(
                    state.prefix(), Map.copyOf(values)));
        }
        return layers;
    }

    private void appendElevatedInheritedStateValue(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            String field,
            String path,
            StableId widgetId,
            GenerationContext context,
            String frameworkVariable) {
        RenderedSymbol themeType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "ElevatedButtonTheme");
        appendElevatedSymbol(
                rendered, occurrences, themeType,
                "widget:" + widgetId + ":button-style:" + field + ":theme:" + path,
                path, widgetId);
        rendered.append(".of(context).style?.").append(field)
                .append("?.resolve(states) ?? ")
                .append(frameworkVariable).append('.').append(field)
                .append("?.resolve(states)");
    }

    private void appendElevatedFrameworkDefaultsDeclaration(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            String path,
            StableId widgetId,
            GenerationContext context,
            String scope) {
        RenderedSymbol styleType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "ButtonStyle");
        RenderedSymbol buttonType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "ElevatedButton");
        rendered.append("  final ");
        appendElevatedSymbol(
                rendered, occurrences, styleType,
                "widget:" + widgetId + ":button-style:framework-style-type:"
                + scope + ':' + path,
                path, widgetId);
        rendered.append(" frameworkDefaults = (const ");
        appendElevatedSymbol(
                rendered, occurrences, buttonType,
                "widget:" + widgetId + ":button-style:default-button:"
                + scope + ':' + path,
                path, widgetId);
        rendered.append("(onPressed: null, child: null)).defaultStyleOf(context);\n");
    }

    private static void appendElevatedLeafFallback(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            RenderedValue selected,
            RenderedValue base,
            String inherited,
            String terminalFallback) {
        if (selected != null) {
            appendRendered(rendered, occurrences, selected);
            return;
        }
        if (base != null) {
            appendRendered(rendered, occurrences, base);
            return;
        }
        rendered.append(inherited).append(" ?? ").append(terminalFallback);
    }

    private static void appendElevatedCopyWithLeaf(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            String dartName,
            String suffix,
            ElevatedButtonLayer selected,
            ElevatedButtonLayer base) {
        RenderedValue value = selected.values().get(suffix);
        if (value == null && base != null) {
            value = base.values().get(suffix);
        }
        if (value != null) {
            rendered.append(dartName).append(": ");
            appendRendered(rendered, occurrences, value);
            rendered.append(", ");
        }
    }

    private static void trimTrailingCommaSpace(StringBuilder rendered) {
        if (rendered.length() >= 2
                && rendered.substring(rendered.length() - 2).equals(", ")) {
            rendered.setLength(rendered.length() - 2);
        }
    }

    private void addElevatedMember(
            WidgetNode node,
            WidgetDefinition definition,
            String propertyName,
            String dartName,
            int order,
            String path,
            GenerationContext context,
            List<CompositeMember> members) {
        PropertyDefinition property = elevatedProperty(definition, propertyName);
        PropertyValue value = node.properties().get(property.name());
        if (value != null) {
            members.add(new CompositeMember(
                    dartName,
                    order,
                    renderProperty(value, property,
                            path + "/properties/" + pointer(propertyName),
                            node.id(), context)));
        }
    }

    private RenderedValue renderElevatedCursor(
            WidgetNode node,
            WidgetDefinition definition,
            String propertyName,
            String path,
            GenerationContext context) {
        PropertyDefinition property = elevatedProperty(definition, propertyName);
        PropertyValue value = node.properties().get(property.name());
        if (value == null) {
            return null;
        }
        if (!(value instanceof PropertyValue.StringValue cursor)) {
            throw catalogInconsistency(
                    path + "/properties/" + pointer(propertyName), node.id(),
                    propertyName + " must be a validated cursor preset string.");
        }
        RenderedSymbol cursors = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "SystemMouseCursors");
        return scalar(
                cursors.text() + '.' + cursor.value(),
                true,
                path + "/properties/" + pointer(propertyName),
                node.id(),
                context,
                List.of(occurrence(
                        "widget:" + node.id() + ":button-cursor:" + propertyName,
                        cursors.nameOffset(), cursors.name(), cursors.libraryUri(),
                        path + "/properties/" + pointer(propertyName),
                        Optional.of(node.id()))));
    }

    private RenderedValue renderElevatedShapeStateProperty(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context) {
        List<String> suffixes = List.of(
                "ShapeKind",
                "ShapeRadiusTopLeft", "ShapeRadiusTopRight",
                "ShapeRadiusBottomRight", "ShapeRadiusBottomLeft",
                "ShapeCircleEccentricity");
        LinkedHashMap<String, ElevatedButtonLayer> layers = elevatedLayers(
                node, definition, path, context, suffixes);
        if (layers.values().stream().noneMatch(ElevatedButtonLayer::configured)) {
            return null;
        }

        String propertyPath = path + "/properties/style/shape";
        RenderedSymbol propertyType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetStateProperty");
        RenderedSymbol shapeType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "OutlinedBorder");
        RenderedSymbol borderRadiusType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "BorderRadius");
        RenderedSymbol stateType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetState");
        StringBuilder rendered = new StringBuilder();
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedSymbol(
                rendered, occurrences, propertyType,
                "widget:" + node.id() + ":button-style:shape:resolver",
                propertyPath, node.id());
        rendered.append(".resolveWith<");
        appendElevatedSymbol(
                rendered, occurrences, shapeType,
                "widget:" + node.id() + ":button-style:shape:value-type",
                propertyPath, node.id());
        rendered.append("?>((states) {\n");
        appendElevatedFrameworkDefaultsDeclaration(
                rendered, occurrences, propertyPath, node.id(), context,
                "shape");
        rendered.append("  final ");
        appendElevatedSymbol(
                rendered, occurrences, shapeType,
                "widget:" + node.id() + ":button-style:shape:inherited-type",
                propertyPath, node.id());
        rendered.append("? inherited = ");
        appendElevatedInheritedStateValue(
                rendered, occurrences, "shape", propertyPath, node.id(), context,
                "frameworkDefaults");
        rendered.append(";\n  final ");
        appendElevatedSymbol(
                rendered, occurrences, borderRadiusType,
                "widget:" + node.id() + ":button-style:shape:inherited-radius-type",
                propertyPath, node.id());
        rendered.append(" inheritedRadii = switch (inherited) {\n");
        for (String dartClass : List.of(
                "RoundedRectangleBorder", "RoundedSuperellipseBorder",
                "BeveledRectangleBorder", "ContinuousRectangleBorder")) {
            RenderedSymbol subtype = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, dartClass);
            rendered.append("    ");
            appendElevatedSymbol(
                    rendered, occurrences, subtype,
                    "widget:" + node.id() + ":button-style:shape:inherited-" + dartClass,
                    propertyPath, node.id());
            rendered.append(" value => value.borderRadius.resolve(Directionality.of(context)),\n");
        }
        rendered.append("    _ => ");
        appendElevatedSymbol(
                rendered, occurrences, borderRadiusType,
                "widget:" + node.id() + ":button-style:shape:zero-radius",
                propertyPath, node.id());
        rendered.append(".zero,\n  };\n  if (states.contains(");
        appendElevatedSymbol(
                rendered, occurrences, stateType,
                "widget:" + node.id() + ":button-style:shape:disabled-state",
                propertyPath, node.id());
        rendered.append(".disabled)) {\n");
        ElevatedButtonLayer disabled = layers.get("disabled");
        if (disabled.configured()) {
            appendElevatedShapeReturn(
                    rendered, occurrences, node, disabled, null,
                    propertyPath, context, "disabled");
        } else {
            rendered.append("    return inherited;\n");
        }
        rendered.append("  }\n");

        for (String state : List.of("pressed", "hovered", "focused")) {
            ElevatedButtonLayer layer = layers.get(state);
            if (!layer.values().containsKey("ShapeKind")) {
                continue;
            }
            rendered.append("  if (states.contains(");
            appendElevatedSymbol(
                    rendered, occurrences, stateType,
                    "widget:" + node.id() + ":button-style:shape:" + state + "-state",
                    propertyPath, node.id());
            rendered.append('.').append(state).append(")) {\n");
            appendElevatedLayeredShapeReturn(
                    rendered, occurrences, node, layer, layers,
                    propertyPath, context, state, stateType);
            rendered.append("  }\n");
        }
        ElevatedButtonLayer base = layers.get("any");
        if (base.values().containsKey("ShapeKind")) {
            appendElevatedLayeredShapeReturn(
                    rendered, occurrences, node, base, layers,
                    propertyPath, context, "any", stateType);
        } else {
            rendered.append("  return inherited;\n");
        }
        rendered.append("})");
        return scalar(rendered.toString(), false, propertyPath,
                node.id(), context, occurrences);
    }

    private void appendElevatedShapeReturn(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            WidgetNode node,
            ElevatedButtonLayer selected,
            ElevatedButtonLayer base,
            String path,
            GenerationContext context,
            String branch) {
        PropertyValue rawKind = node.properties().get(new PropertyName(
                selected.prefix() + "ShapeKind"));
        if (!(rawKind instanceof PropertyValue.StringValue kind)) {
            throw catalogInconsistency(
                    path, node.id(),
                    selected.prefix() + "ShapeKind must be a validated shape preset string.");
        }
        String dartClass = switch (kind.value()) {
            case "roundedRectangle" -> "RoundedRectangleBorder";
            case "roundedSuperellipse" -> "RoundedSuperellipseBorder";
            case "stadium" -> "StadiumBorder";
            case "circle" -> "CircleBorder";
            case "beveledRectangle" -> "BeveledRectangleBorder";
            case "continuousRectangle" -> "ContinuousRectangleBorder";
            default -> throw catalogInconsistency(
                    path, node.id(),
                    "Unsupported validated ElevatedButton shape kind '"
                    + kind.value() + "'.");
        };
        RenderedSymbol shapeType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartClass);
        RenderedSymbol sideType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "BorderSide");
        rendered.append("    return ");
        appendElevatedSymbol(
                rendered, occurrences, shapeType,
                "widget:" + node.id() + ":button-shape:" + dartClass + ':' + branch,
                path, node.id());
        rendered.append("(side: inherited?.side ?? ");
        appendElevatedSymbol(
                rendered, occurrences, sideType,
                "widget:" + node.id() + ":button-shape:side-fallback:" + branch,
                path, node.id());
        rendered.append(".none");
        if (Set.of(
                "roundedRectangle", "roundedSuperellipse",
                "beveledRectangle", "continuousRectangle")
                .contains(kind.value())) {
            RenderedSymbol borderRadiusType = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "BorderRadius");
            rendered.append(", borderRadius: ");
            appendElevatedSymbol(
                    rendered, occurrences, borderRadiusType,
                    "widget:" + node.id() + ":button-shape:border-radius:" + branch,
                    path, node.id());
            rendered.append(".only(");
            appendElevatedShapeRadius(
                    rendered, occurrences, node, selected, base,
                    "TopLeft", "topLeft", path, context, branch);
            appendElevatedShapeRadius(
                    rendered, occurrences, node, selected, base,
                    "TopRight", "topRight", path, context, branch);
            appendElevatedShapeRadius(
                    rendered, occurrences, node, selected, base,
                    "BottomRight", "bottomRight", path, context, branch);
            appendElevatedShapeRadius(
                    rendered, occurrences, node, selected, base,
                    "BottomLeft", "bottomLeft", path, context, branch);
            trimTrailingCommaSpace(rendered);
            rendered.append(')');
        } else if (kind.value().equals("circle")) {
            rendered.append(", eccentricity: ");
            appendElevatedLeafFallback(
                    rendered, occurrences,
                    selected.values().get("ShapeCircleEccentricity"),
                    base == null ? null
                            : base.values().get("ShapeCircleEccentricity"),
                    "inherited is CircleBorder ? inherited.eccentricity : null",
                    "0.0");
        }
        rendered.append(");\n");
    }

    private void appendElevatedShapeRadius(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            WidgetNode node,
            ElevatedButtonLayer selected,
            ElevatedButtonLayer base,
            String suffix,
            String dartName,
            String path,
            GenerationContext context,
            String branch) {
        rendered.append(dartName).append(": ");
        RenderedValue value = selected.values().get("ShapeRadius" + suffix);
        if (value == null && base != null) {
            value = base.values().get("ShapeRadius" + suffix);
        }
        if (value == null) {
            rendered.append("inheritedRadii.").append(dartName);
        } else {
            RenderedSymbol radiusType = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "Radius");
            appendElevatedSymbol(
                    rendered, occurrences, radiusType,
                    "widget:" + node.id() + ":button-shape:radius-"
                    + dartName + ':' + branch,
                    path, node.id());
            rendered.append(".circular(");
            appendRendered(rendered, occurrences, value);
            rendered.append(')');
        }
        rendered.append(", ");
    }

    private void appendElevatedLayeredShapeReturn(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            WidgetNode node,
            ElevatedButtonLayer selectedKind,
            Map<String, ElevatedButtonLayer> layers,
            String path,
            GenerationContext context,
            String branch,
            RenderedSymbol stateType) {
        PropertyValue rawKind = node.properties().get(new PropertyName(
                selectedKind.prefix() + "ShapeKind"));
        if (!(rawKind instanceof PropertyValue.StringValue kind)) {
            throw catalogInconsistency(
                    path, node.id(),
                    selectedKind.prefix()
                    + "ShapeKind must be a validated shape preset string.");
        }
        String dartClass = switch (kind.value()) {
            case "roundedRectangle" -> "RoundedRectangleBorder";
            case "roundedSuperellipse" -> "RoundedSuperellipseBorder";
            case "stadium" -> "StadiumBorder";
            case "circle" -> "CircleBorder";
            case "beveledRectangle" -> "BeveledRectangleBorder";
            case "continuousRectangle" -> "ContinuousRectangleBorder";
            default -> throw catalogInconsistency(
                    path, node.id(),
                    "Unsupported validated ElevatedButton shape kind '"
                    + kind.value() + "'.");
        };
        RenderedSymbol shapeType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartClass);
        RenderedSymbol sideType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "BorderSide");
        rendered.append("    return ");
        appendElevatedSymbol(
                rendered, occurrences, shapeType,
                "widget:" + node.id() + ":button-shape:layered-"
                + dartClass + ':' + branch,
                path, node.id());
        rendered.append("(side: inherited?.side ?? ");
        appendElevatedSymbol(
                rendered, occurrences, sideType,
                "widget:" + node.id() + ":button-shape:layered-side:" + branch,
                path, node.id());
        rendered.append(".none");
        if (Set.of(
                "roundedRectangle", "roundedSuperellipse",
                "beveledRectangle", "continuousRectangle")
                .contains(kind.value())) {
            RenderedSymbol borderRadiusType = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "BorderRadius");
            rendered.append(", borderRadius: ");
            appendElevatedSymbol(
                    rendered, occurrences, borderRadiusType,
                    "widget:" + node.id() + ":button-shape:layered-radius:" + branch,
                    path, node.id());
            rendered.append(".only(");
            appendElevatedLayeredShapeRadius(
                    rendered, occurrences, node, layers,
                    "TopLeft", "topLeft", path, context, branch, stateType);
            appendElevatedLayeredShapeRadius(
                    rendered, occurrences, node, layers,
                    "TopRight", "topRight", path, context, branch, stateType);
            appendElevatedLayeredShapeRadius(
                    rendered, occurrences, node, layers,
                    "BottomRight", "bottomRight", path, context, branch, stateType);
            appendElevatedLayeredShapeRadius(
                    rendered, occurrences, node, layers,
                    "BottomLeft", "bottomLeft", path, context, branch, stateType);
            trimTrailingCommaSpace(rendered);
            rendered.append(')');
        } else if (kind.value().equals("circle")) {
            rendered.append(", eccentricity: ");
            appendElevatedLayeredStateLeaf(
                    rendered, occurrences, layers, "ShapeCircleEccentricity",
                    "inherited is CircleBorder ? inherited.eccentricity : null",
                    "0.0", stateType, node.id(), path,
                    "shape-eccentricity-" + branch);
        }
        rendered.append(");\n");
    }

    private void appendElevatedLayeredShapeRadius(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            WidgetNode node,
            Map<String, ElevatedButtonLayer> layers,
            String suffix,
            String dartName,
            String path,
            GenerationContext context,
            String branch,
            RenderedSymbol stateType) {
        rendered.append(dartName).append(": ");
        for (String state : List.of("pressed", "hovered", "focused")) {
            RenderedValue value = layers.get(state).values().get(
                    "ShapeRadius" + suffix);
            if (value == null) {
                continue;
            }
            rendered.append("states.contains(");
            appendElevatedSymbol(
                    rendered, occurrences, stateType,
                    "widget:" + node.id() + ":button-shape:" + dartName
                    + ':' + branch + ':' + state,
                    path, node.id());
            rendered.append('.').append(state).append(") ? ");
            appendElevatedRadiusValue(
                    rendered, occurrences, node, value,
                    path, context, branch + ':' + state + ':' + dartName);
            rendered.append(" : ");
        }
        RenderedValue base = layers.get("any").values().get(
                "ShapeRadius" + suffix);
        if (base != null) {
            appendElevatedRadiusValue(
                    rendered, occurrences, node, base,
                    path, context, branch + ":any:" + dartName);
        } else {
            rendered.append("inheritedRadii.").append(dartName);
        }
        rendered.append(", ");
    }

    private void appendElevatedRadiusValue(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            WidgetNode node,
            RenderedValue value,
            String path,
            GenerationContext context,
            String branch) {
        RenderedSymbol radiusType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "Radius");
        appendElevatedSymbol(
                rendered, occurrences, radiusType,
                "widget:" + node.id() + ":button-shape:layered-radius-value:" + branch,
                path, node.id());
        rendered.append(".circular(");
        appendRendered(rendered, occurrences, value);
        rendered.append(')');
    }

    private RenderedValue renderElevatedTextStyleStateProperty(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            int valueIndent,
            GenerationContext context) {
        LinkedHashMap<String, ElevatedButtonTextLayer> layers =
                elevatedTextLayers(node, definition, path, context);
        if (layers.values().stream().noneMatch(ElevatedButtonTextLayer::configured)) {
            return null;
        }

        String propertyPath = path + "/properties/style/textStyle";
        RenderedSymbol propertyType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetStateProperty");
        RenderedSymbol textStyleType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "TextStyle");
        RenderedSymbol stateType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "WidgetState");
        StringBuilder rendered = new StringBuilder();
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedSymbol(
                rendered, occurrences, propertyType,
                "widget:" + node.id() + ":button-style:textStyle:resolver",
                propertyPath, node.id());
        rendered.append(".resolveWith<");
        appendElevatedSymbol(
                rendered, occurrences, textStyleType,
                "widget:" + node.id() + ":button-style:textStyle:value-type",
                propertyPath, node.id());
        rendered.append("?>((states) {\n");
        appendElevatedFrameworkDefaultsDeclaration(
                rendered, occurrences, propertyPath, node.id(), context,
                "textStyle");
        rendered.append("  final ");
        appendElevatedSymbol(
                rendered, occurrences, textStyleType,
                "widget:" + node.id() + ":button-style:textStyle:inherited-type",
                propertyPath, node.id());
        rendered.append("? inherited = ");
        appendElevatedInheritedStateValue(
                rendered, occurrences, "textStyle",
                propertyPath, node.id(), context, "frameworkDefaults");
        rendered.append(";\n  if (states.contains(");
        appendElevatedSymbol(
                rendered, occurrences, stateType,
                "widget:" + node.id() + ":button-style:textStyle:disabled-state",
                propertyPath, node.id());
        rendered.append(".disabled)) {\n");
        ElevatedButtonTextLayer disabled = layers.get("disabled");
        if (disabled.configured()) {
            rendered.append("    var resolved = inherited ?? const ");
            appendElevatedSymbol(
                    rendered, occurrences, textStyleType,
                    "widget:" + node.id() + ":button-text:fallback:disabled",
                    propertyPath, node.id());
            rendered.append("();\n");
            appendElevatedTextLayer(
                    rendered, occurrences, disabled, "disabled",
                    propertyPath, node.id(), context);
            rendered.append("    return resolved;\n");
        } else {
            rendered.append("    return inherited;\n");
        }
        rendered.append("  }\n  var resolved = inherited ?? const ");
        appendElevatedSymbol(
                rendered, occurrences, textStyleType,
                "widget:" + node.id() + ":button-text:fallback:enabled",
                propertyPath, node.id());
        rendered.append("();\n");
        ElevatedButtonTextLayer base = layers.get("any");
        if (base.configured()) {
            appendElevatedTextLayer(
                    rendered, occurrences, base, "any",
                    propertyPath, node.id(), context);
        }
        for (String state : List.of("focused", "hovered", "pressed")) {
            ElevatedButtonTextLayer layer = layers.get(state);
            if (!layer.configured()) {
                continue;
            }
            rendered.append("  if (states.contains(");
            appendElevatedSymbol(
                    rendered, occurrences, stateType,
                    "widget:" + node.id() + ":button-style:textStyle:"
                    + state + "-state",
                    propertyPath, node.id());
            rendered.append('.').append(state).append(")) {\n");
            appendElevatedTextLayer(
                    rendered, occurrences, layer, state,
                    propertyPath, node.id(), context);
            rendered.append("  }\n");
        }
        rendered.append("  return resolved;\n})");
        return scalar(rendered.toString(), false, propertyPath,
                node.id(), context, occurrences);
    }

    private LinkedHashMap<String, ElevatedButtonTextLayer> elevatedTextLayers(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context) {
        LinkedHashMap<String, ElevatedButtonTextLayer> layers = new LinkedHashMap<>();
        for (ElevatedButtonState state : ELEVATED_BUTTON_STATES) {
            ArrayList<ElevatedButtonTextMember> styleMembers = new ArrayList<>();
            ArrayList<ElevatedButtonTextMember> localeMembers = new ArrayList<>();
            ArrayList<ElevatedButtonTextMember> decorationMembers = new ArrayList<>();
            RenderedValue theme = null;
            String textPrefix = state.prefix() + "Text";
            for (PropertyDefinition property : definition.properties()) {
                String propertyName = property.name().value();
                if (!propertyName.startsWith(textPrefix)) {
                    continue;
                }
                PropertyValue value = node.properties().get(property.name());
                if (value == null) {
                    continue;
                }
                ElevatedButtonWidgetPropertySchema.Definition binding =
                        ElevatedButtonWidgetPropertySchema.find(property.name())
                                .orElseThrow();
                String propertyPath = path + "/properties/" + pointer(propertyName);
                RenderedValue rendered = switch (binding.encoding()) {
                    case SCALAR -> renderProperty(
                            value, property, propertyPath, node.id(), context);
                    case NEWLINE_STRING_LIST -> renderStringList(
                            value, propertyPath, node.id(), context);
                    case DECORATION_FLAG -> renderProperty(
                            value, property, propertyPath, node.id(), context);
                };
                ElevatedButtonTextMember member = new ElevatedButtonTextMember(
                        property, binding, value, rendered);
                switch (binding.target()) {
                    case STYLE_TEXT -> {
                        if (binding.dartName().equals("textStyle.theme")) {
                            theme = rendered;
                        } else {
                            styleMembers.add(member);
                        }
                    }
                    case STYLE_TEXT_LOCALE -> localeMembers.add(member);
                    case STYLE_TEXT_DECORATION -> decorationMembers.add(member);
                    default -> {
                        // Only reviewed TextStyle targets share this prefix.
                    }
                }
            }
            styleMembers.sort(Comparator.comparingInt(
                    member -> member.binding().dartOrder()));
            localeMembers.sort(Comparator.comparingInt(
                    member -> member.binding().dartOrder()));
            decorationMembers.sort(Comparator.comparingInt(
                    member -> member.binding().dartOrder()));
            layers.put(state.key(), new ElevatedButtonTextLayer(
                    state.prefix(), theme,
                    List.copyOf(styleMembers),
                    List.copyOf(localeMembers),
                    List.copyOf(decorationMembers)));
        }
        return layers;
    }

    private void appendElevatedTextLayer(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            ElevatedButtonTextLayer layer,
            String branch,
            String path,
            StableId widgetId,
            GenerationContext context) {
        String local = "local" + Character.toUpperCase(branch.charAt(0))
                + branch.substring(1);
        RenderedSymbol textStyleType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "TextStyle");
        rendered.append("    var ").append(local).append(" = ");
        if (layer.theme() != null) {
            appendRendered(rendered, occurrences, layer.theme());
            rendered.append(" ?? const ");
            appendElevatedSymbol(
                    rendered, occurrences, textStyleType,
                    "widget:" + widgetId + ":button-text:theme-fallback-type:"
                    + branch,
                    path, widgetId);
            rendered.append("()");
        } else {
            rendered.append("const ");
            appendElevatedSymbol(
                    rendered, occurrences, textStyleType,
                    "widget:" + widgetId + ":button-text:local-type:" + branch,
                    path, widgetId);
            rendered.append("()");
        }
        rendered.append(";\n");
        if (layer.theme() != null) {
            RenderedSymbol paintType = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "Paint");
            rendered.append("    if (").append(local)
                    .append(".backgroundColor != null && ")
                    .append(local).append(".background == null) {\n")
                    .append("      ").append(local).append(" = ")
                    .append(local).append(".copyWith(background: (");
            appendElevatedSymbol(
                    rendered, occurrences, paintType,
                    "widget:" + widgetId
                    + ":button-text:theme-background-paint:" + branch,
                    path, widgetId);
            rendered.append("()..color = ").append(local)
                    .append(".backgroundColor!));\n    }\n");
        }
        List<ElevatedButtonTextMember> styleMembers = layer.styleMembers();
        if (styleMembers.isEmpty()
                && layer.localeMembers().isEmpty()
                && layer.decorationMembers().isEmpty()) {
            appendElevatedTextMerge(rendered, layer, local);
            return;
        }
        boolean inheritsLower = elevatedTextLayerInheritsLower(layer);
        List<ElevatedButtonTextMember> copiedStyleMembers = styleMembers.stream()
                .filter(member -> !tail(member.binding().dartName())
                        .equals("package"))
                .toList();
        if (!copiedStyleMembers.isEmpty()
                || !layer.localeMembers().isEmpty()
                || !layer.decorationMembers().isEmpty()) {
            rendered.append("    ").append(local).append(" = ")
                    .append(local).append(".copyWith(");
            for (ElevatedButtonTextMember member : copiedStyleMembers) {
                String dartName = tail(member.binding().dartName());
                if (dartName.equals("backgroundColor")) {
                    rendered.append("background: ");
                    appendElevatedBackgroundPaint(
                            rendered, occurrences, member.rendered(),
                            branch, path, widgetId, context);
                } else {
                    rendered.append(dartName).append(": ");
                    appendRendered(rendered, occurrences, member.rendered());
                }
                rendered.append(", ");
            }
            if (!layer.localeMembers().isEmpty()) {
                rendered.append("locale: ");
                appendElevatedTextLocaleFallback(
                        rendered, occurrences, layer.localeMembers(),
                        branch, path, widgetId, context, local, inheritsLower);
                rendered.append(", ");
            }
            if (!layer.decorationMembers().isEmpty()) {
                rendered.append("decoration: ");
                appendElevatedTextDecorationFallback(
                        rendered, occurrences, layer.decorationMembers(),
                        branch, path, widgetId, context, local, inheritsLower);
                rendered.append(", ");
            }
            trimTrailingCommaSpace(rendered);
            rendered.append(");\n");
        }
        appendElevatedTextMerge(rendered, layer, local);
        appendElevatedTextPackage(
                rendered, occurrences, layer,
                branch, path, widgetId, context);
    }

    private static void appendElevatedTextMerge(
            StringBuilder rendered,
            ElevatedButtonTextLayer layer,
            String local) {
        ElevatedButtonTextMember inherit = elevatedTextMember(
                layer.styleMembers(), "inherit");
        rendered.append("    resolved = resolved.merge(")
                .append(local).append(')');
        if (inherit != null
                && inherit.value() instanceof PropertyValue.BooleanValue value) {
            rendered.append(".copyWith(inherit: ")
                    .append(value.value()).append(')');
        }
        rendered.append(";\n");
    }

    private void appendElevatedTextPackage(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            ElevatedButtonTextLayer layer,
            String branch,
            String path,
            StableId widgetId,
            GenerationContext context) {
        ElevatedButtonTextMember packageMember = elevatedTextMember(
                layer.styleMembers(), "package");
        if (packageMember == null) {
            return;
        }
        ElevatedButtonTextMember familyMember = elevatedTextMember(
                layer.styleMembers(), "fontFamily");
        ElevatedButtonTextMember fallbackMember = elevatedTextMember(
                layer.styleMembers(), "fontFamilyFallback");
        String suffix = Character.toUpperCase(branch.charAt(0))
                + branch.substring(1);
        String packageVariable = "package" + suffix;
        String familyVariable = "resolvedFontFamily" + suffix;
        String fallbackVariable = "repackagedFallback" + suffix;
        rendered.append("    final ").append(packageVariable).append(" = ");
        appendRendered(rendered, occurrences, packageMember.rendered());
        rendered.append(";\n    final ").append(familyVariable)
                .append(" = resolved.fontFamily;\n")
                .append("    if (").append(familyVariable)
                .append(" == null || (").append(familyVariable)
                .append(".startsWith('packages/') && ")
                .append(familyVariable).append(".endsWith('/null'))) {\n")
                .append("      final ").append(fallbackVariable)
                .append(" = resolved.fontFamilyFallback?.map((family) {\n")
                .append("        if (!family.startsWith('packages/')) return family;\n")
                .append("        final separator = family.indexOf('/', 9);\n")
                .append("        return separator < 0 ? family : ")
                .append("family.substring(separator + 1);\n")
                .append("      }).map((family) => 'packages/$")
                .append(packageVariable).append("/$family').toList();\n")
                .append("      resolved = ");
        appendElevatedTextStyleWithoutPackage(
                rendered, occurrences, fallbackVariable,
                branch, path, widgetId, context);
        rendered.append(";\n")
                .append("    } else {\n")
                .append("      resolved = resolved.copyWith(");
        if (familyMember != null) {
            rendered.append("fontFamily: ");
            appendRendered(rendered, occurrences, familyMember.rendered());
            rendered.append(", ");
        }
        if (fallbackMember != null) {
            rendered.append("fontFamilyFallback: ");
            appendRendered(rendered, occurrences, fallbackMember.rendered());
            rendered.append(", ");
        }
        rendered.append("package: ").append(packageVariable).append(");\n")
                .append("    }\n");
    }

    private void appendElevatedTextStyleWithoutPackage(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            String fallbackVariable,
            String branch,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol textStyleType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "TextStyle");
        appendElevatedSymbol(
                rendered, occurrences, textStyleType,
                "widget:" + widgetId
                + ":button-text:package-free-fallback:" + branch,
                path, widgetId);
        rendered.append("(\n")
                .append("        inherit: resolved.inherit,\n")
                .append("        color: resolved.foreground == null ")
                .append("? resolved.color : null,\n")
                .append("        backgroundColor: resolved.background == null ")
                .append("? resolved.backgroundColor : null,\n")
                .append("        fontSize: resolved.fontSize,\n")
                .append("        fontWeight: resolved.fontWeight,\n")
                .append("        fontStyle: resolved.fontStyle,\n")
                .append("        letterSpacing: resolved.letterSpacing,\n")
                .append("        wordSpacing: resolved.wordSpacing,\n")
                .append("        textBaseline: resolved.textBaseline,\n")
                .append("        height: resolved.height,\n")
                .append("        leadingDistribution: resolved.leadingDistribution,\n")
                .append("        locale: resolved.locale,\n")
                .append("        foreground: resolved.foreground,\n")
                .append("        background: resolved.background,\n")
                .append("        shadows: resolved.shadows,\n")
                .append("        fontFeatures: resolved.fontFeatures,\n")
                .append("        fontVariations: resolved.fontVariations,\n")
                .append("        decoration: resolved.decoration,\n")
                .append("        decorationColor: resolved.decorationColor,\n")
                .append("        decorationStyle: resolved.decorationStyle,\n")
                .append("        decorationThickness: resolved.decorationThickness,\n")
                .append("        debugLabel: resolved.debugLabel,\n")
                .append("        fontFamilyFallback: ")
                .append(fallbackVariable).append(",\n")
                .append("        overflow: resolved.overflow,\n")
                .append("      )");
    }

    private static boolean elevatedTextLayerInheritsLower(
            ElevatedButtonTextLayer layer) {
        return layer.styleMembers().stream()
                .filter(member -> tail(member.binding().dartName()).equals("inherit"))
                .map(ElevatedButtonTextMember::value)
                .filter(PropertyValue.BooleanValue.class::isInstance)
                .map(PropertyValue.BooleanValue.class::cast)
                .map(PropertyValue.BooleanValue::value)
                .findFirst()
                .orElse(true);
    }

    private static ElevatedButtonTextMember elevatedTextMember(
            List<ElevatedButtonTextMember> members,
            String dartName) {
        return members.stream()
                .filter(member -> tail(member.binding().dartName())
                        .equals(dartName))
                .findFirst().orElse(null);
    }

    private void appendElevatedBackgroundPaint(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            RenderedValue color,
            String branch,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol paintType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "Paint");
        rendered.append('(');
        appendElevatedSymbol(
                rendered, occurrences, paintType,
                "widget:" + widgetId + ":button-text:background-paint:" + branch,
                path, widgetId);
        rendered.append("()..color = ");
        appendRendered(rendered, occurrences, color);
        rendered.append(')');
    }

    private void appendElevatedTextLocaleFallback(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            List<ElevatedButtonTextMember> members,
            String branch,
            String path,
            StableId widgetId,
            GenerationContext context,
            String local,
            boolean inheritsLower) {
        RenderedSymbol localeType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "Locale");
        appendElevatedSymbol(
                rendered, occurrences, localeType,
                "widget:" + widgetId + ":button-text:locale:" + branch,
                path, widgetId);
        rendered.append(".fromSubtags(languageCode: ");
        appendElevatedTextSubtag(
                rendered, occurrences, members, "languageCode",
                local + ".locale?.languageCode"
                + (inheritsLower
                        ? " ?? resolved.locale?.languageCode ?? 'und'"
                        : " ?? 'und'"));
        rendered.append(", scriptCode: ");
        appendElevatedTextSubtag(
                rendered, occurrences, members, "scriptCode",
                local + ".locale == null"
                + (inheritsLower
                        ? " ? resolved.locale?.scriptCode : "
                        : " ? null : ")
                + local + ".locale?.scriptCode");
        rendered.append(", countryCode: ");
        appendElevatedTextSubtag(
                rendered, occurrences, members, "countryCode",
                local + ".locale == null"
                + (inheritsLower
                        ? " ? resolved.locale?.countryCode : "
                        : " ? null : ")
                + local + ".locale?.countryCode");
        rendered.append(')');
    }

    private static void appendElevatedTextSubtag(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            List<ElevatedButtonTextMember> members,
            String dartName,
            String fallback) {
        ElevatedButtonTextMember match = members.stream()
                .filter(member -> tail(member.binding().dartName()).equals(dartName))
                .findFirst().orElse(null);
        if (match == null) {
            rendered.append(fallback);
        } else {
            appendRendered(rendered, occurrences, match.rendered());
        }
    }

    private void appendElevatedTextDecorationFallback(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            List<ElevatedButtonTextMember> members,
            String branch,
            String path,
            StableId widgetId,
            GenerationContext context,
            String local,
            boolean inheritsLower) {
        RenderedSymbol type = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "TextDecoration");
        rendered.append("(() { final values = <");
        appendElevatedSymbol(
                rendered, occurrences, type,
                "widget:" + widgetId + ":button-text:decoration-list:" + branch,
                path, widgetId);
        rendered.append(">[");
        for (String flag : List.of("underline", "overline", "lineThrough")) {
            ElevatedButtonTextMember member = members.stream()
                    .filter(candidate -> tail(candidate.binding().dartName()).equals(flag))
                    .findFirst().orElse(null);
            rendered.append("if (");
            if (member == null) {
                rendered.append(local).append(".decoration?.contains(");
                appendElevatedSymbol(
                        rendered, occurrences, type,
                        "widget:" + widgetId + ":button-text:decoration-check:"
                        + branch + ':' + flag,
                        path, widgetId);
                rendered.append('.').append(flag).append(") ?? ");
                if (inheritsLower) {
                    rendered.append("resolved.decoration?.contains(");
                    appendElevatedSymbol(
                            rendered, occurrences, type,
                            "widget:" + widgetId
                            + ":button-text:decoration-inherited-check:"
                            + branch + ':' + flag,
                            path, widgetId);
                    rendered.append('.').append(flag).append(") ?? false");
                } else {
                    rendered.append("false");
                }
            } else {
                appendRendered(rendered, occurrences, member.rendered());
            }
            rendered.append(") ");
            appendElevatedSymbol(
                    rendered, occurrences, type,
                    "widget:" + widgetId + ":button-text:decoration-value:"
                    + branch + ':' + flag,
                    path, widgetId);
            rendered.append('.').append(flag).append(", ");
        }
        trimTrailingCommaSpace(rendered);
        rendered.append("]; return values.isEmpty ? ");
        appendElevatedSymbol(
                rendered, occurrences, type,
                "widget:" + widgetId + ":button-text:decoration-none:" + branch,
                path, widgetId);
        rendered.append(".none : ");
        appendElevatedSymbol(
                rendered, occurrences, type,
                "widget:" + widgetId + ":button-text:decoration-combine:" + branch,
                path, widgetId);
        rendered.append(".combine(values); })()");
    }

    private static String tail(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private void appendElevatedButtonCommonStyle(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            int valueIndent,
            GenerationContext context,
            List<CompositeMember> style) {
        RenderedValue density = renderElevatedVisualDensity(
                node, definition, path, valueIndent, context);
        if (density != null) {
            style.add(new CompositeMember("visualDensity", 17, density));
        }
        addElevatedCommonDirect(
                node, definition, "styleTapTargetSize", "tapTargetSize", 18,
                path, context, style);

        PropertyDefinition durationProperty = elevatedProperty(
                definition, "styleAnimationDurationMs");
        PropertyValue durationValue = node.properties().get(durationProperty.name());
        if (durationValue != null) {
            RenderedValue duration = renderNamedCompositeMembers(
                    "Duration", Optional.empty(),
                    List.of(new CompositeMember(
                            "milliseconds", 0,
                            renderProperty(
                                    durationValue, durationProperty,
                                    path + "/properties/styleAnimationDurationMs",
                                    node.id(), context))),
                    valueIndent,
                    path + "/properties/styleAnimationDuration",
                    node.id(), context);
            style.add(new CompositeMember("animationDuration", 19, duration));
        }
        addElevatedCommonDirect(
                node, definition, "styleEnableFeedback", "enableFeedback", 20,
                path, context, style);
        RenderedValue alignment = renderElevatedAlignment(
                node, definition, path, valueIndent, context);
        if (alignment != null) {
            style.add(new CompositeMember("alignment", 21, alignment));
        }
        RenderedValue splash = renderElevatedSplashFactory(
                node, definition, path, context);
        if (splash != null) {
            style.add(new CompositeMember("splashFactory", 22, splash));
        }
    }

    private RenderedValue renderElevatedVisualDensity(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            int valueIndent,
            GenerationContext context) {
        PropertyDefinition horizontalProperty = elevatedProperty(
                definition, "styleVisualDensityHorizontal");
        PropertyDefinition verticalProperty = elevatedProperty(
                definition, "styleVisualDensityVertical");
        PropertyValue horizontalValue = node.properties().get(
                horizontalProperty.name());
        PropertyValue verticalValue = node.properties().get(
                verticalProperty.name());
        if (horizontalValue == null && verticalValue == null) {
            return null;
        }
        String propertyPath = path + "/properties/styleVisualDensity";
        RenderedValue horizontal = horizontalValue == null ? null : renderProperty(
                horizontalValue, horizontalProperty,
                path + "/properties/styleVisualDensityHorizontal",
                node.id(), context);
        RenderedValue vertical = verticalValue == null ? null : renderProperty(
                verticalValue, verticalProperty,
                path + "/properties/styleVisualDensityVertical",
                node.id(), context);
        RenderedSymbol densityType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "VisualDensity");
        RenderedSymbol themeType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "ElevatedButtonTheme");
        StringBuilder rendered = new StringBuilder("(() {\n");
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedFrameworkDefaultsDeclaration(
                rendered, occurrences, propertyPath, node.id(), context,
                "visualDensity");
        rendered.append("  final inherited = ");
        appendElevatedSymbol(
                rendered, occurrences, themeType,
                "widget:" + node.id() + ":button-density:theme",
                propertyPath, node.id());
        rendered.append(".of(context).style?.visualDensity "
                + "?? frameworkDefaults.visualDensity ?? ");
        appendElevatedSymbol(
                rendered, occurrences, densityType,
                "widget:" + node.id() + ":button-density:standard",
                propertyPath, node.id());
        rendered.append(".standard;\n  return ");
        appendElevatedSymbol(
                rendered, occurrences, densityType,
                "widget:" + node.id() + ":button-density:value",
                propertyPath, node.id());
        rendered.append("(horizontal: ");
        if (horizontal == null) {
            rendered.append("inherited.horizontal");
        } else {
            appendRendered(rendered, occurrences, horizontal);
        }
        rendered.append(", vertical: ");
        if (vertical == null) {
            rendered.append("inherited.vertical");
        } else {
            appendRendered(rendered, occurrences, vertical);
        }
        rendered.append(");\n})()");
        return scalar(rendered.toString(), false, propertyPath,
                node.id(), context, occurrences);
    }

    private void addElevatedCommonDirect(
            WidgetNode node,
            WidgetDefinition definition,
            String propertyName,
            String dartName,
            int order,
            String path,
            GenerationContext context,
            List<CompositeMember> style) {
        addElevatedMember(
                node, definition, propertyName, dartName, order,
                path, context, style);
    }

    private RenderedValue renderElevatedAlignment(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            int valueIndent,
            GenerationContext context) {
        PropertyDefinition kindProperty = elevatedProperty(
                definition, "styleAlignmentKind");
        PropertyDefinition xProperty = elevatedProperty(
                definition, "styleAlignmentX");
        PropertyDefinition yProperty = elevatedProperty(
                definition, "styleAlignmentY");
        PropertyValue kindValue = node.properties().get(kindProperty.name());
        PropertyValue xValue = node.properties().get(xProperty.name());
        PropertyValue yValue = node.properties().get(yProperty.name());
        if (kindValue == null && xValue == null && yValue == null) {
            return null;
        }
        if (!(kindValue instanceof PropertyValue.StringValue kind)
                || xValue == null || yValue == null) {
            throw catalogInconsistency(
                    path + "/properties/styleAlignment", node.id(),
                    "ButtonStyle alignment requires kind, X, and Y together.");
        }
        RenderedValue x = renderProperty(
                xValue, xProperty, path + "/properties/styleAlignmentX",
                node.id(), context);
        RenderedValue y = renderProperty(
                yValue, yProperty, path + "/properties/styleAlignmentY",
                node.id(), context);
        String dartClass = kind.value().equals("directional")
                ? "AlignmentDirectional" : "Alignment";
        RenderedSymbol type = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartClass);
        StringBuilder rendered = new StringBuilder();
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        if (x.constant() && y.constant()) {
            rendered.append("const ");
        }
        appendElevatedSymbol(
                rendered, occurrences, type,
                "widget:" + node.id() + ":button-alignment:type",
                path + "/properties/styleAlignment", node.id());
        rendered.append('(');
        appendRendered(rendered, occurrences, x);
        rendered.append(", ");
        appendRendered(rendered, occurrences, y);
        rendered.append(')');
        return scalar(rendered.toString(), x.constant() && y.constant(),
                path + "/properties/styleAlignment", node.id(), context, occurrences);
    }

    private RenderedValue renderElevatedSplashFactory(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context) {
        String propertyName = "styleSplashFactory";
        PropertyDefinition property = elevatedProperty(definition, propertyName);
        PropertyValue value = node.properties().get(property.name());
        if (value == null) {
            return null;
        }
        if (!(value instanceof PropertyValue.StringValue preset)) {
            throw catalogInconsistency(
                    path + "/properties/" + propertyName, node.id(),
                    propertyName + " must be a validated splash preset string.");
        }
        String dartClass = switch (preset.value()) {
            case "inkRipple" -> "InkRipple";
            case "inkSplash" -> "InkSplash";
            case "inkSparkle" -> "InkSparkle";
            case "noSplash" -> "NoSplash";
            default -> throw catalogInconsistency(
                    path + "/properties/" + propertyName, node.id(),
                    "Unsupported validated splash preset '" + preset.value() + "'.");
        };
        RenderedSymbol type = context.planner().renderedSymbol(
                MATERIAL_IMPORT, dartClass);
        return scalar(
                type.text() + ".splashFactory",
                true,
                path + "/properties/" + propertyName,
                node.id(),
                context,
                List.of(occurrence(
                        "widget:" + node.id() + ":button-splash:" + propertyName,
                        type.nameOffset(), type.name(), type.libraryUri(),
                        path + "/properties/" + propertyName,
                        Optional.of(node.id()))));
    }

    private void appendAppBarCompoundArguments(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            int valueIndent,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        Map<AppBarWidgetPropertySchema.Target, List<AppBarMember>> grouped = new HashMap<>();
        for (PropertyDefinition property : definition.properties()) {
            AppBarWidgetPropertySchema.Definition binding = AppBarWidgetPropertySchema
                    .find(property.name()).orElse(null);
            if (binding == null || binding.target() == AppBarWidgetPropertySchema.Target.DIRECT) {
                continue;
            }
            PropertyValue value = node.properties().get(property.name());
            if (value == null) {
                continue;
            }
            String propertyPath = path + "/properties/" + pointer(property.name().value());
            RenderedValue rendered = switch (binding.encoding()) {
                case SCALAR -> renderProperty(
                        value, property, propertyPath, node.id(), context);
                case NEWLINE_STRING_LIST -> renderStringList(
                        value, propertyPath, node.id(), context);
                case DECORATION_FLAG -> renderProperty(
                        value, property, propertyPath, node.id(), context);
            };
            grouped.computeIfAbsent(binding.target(), ignored -> new ArrayList<>())
                    .add(new AppBarMember(property, binding, value, rendered));
        }

        appendAppBarNotificationPredicate(
                grouped.get(AppBarWidgetPropertySchema.Target.NOTIFICATION_PREDICATE),
                path, node.id(), context, arguments);
        appendAppBarShape(grouped, valueIndent, path, node.id(), context, arguments);
        appendAppBarIconTheme(
                grouped.get(AppBarWidgetPropertySchema.Target.ICON_THEME),
                "iconTheme", valueIndent, path, node.id(), context, arguments);
        appendAppBarIconTheme(
                grouped.get(AppBarWidgetPropertySchema.Target.ACTIONS_ICON_THEME),
                "actionsIconTheme", valueIndent, path, node.id(), context, arguments);
        appendAppBarTextStyle(
                grouped,
                AppBarWidgetPropertySchema.Target.TOOLBAR_TEXT_STYLE_THEME,
                AppBarWidgetPropertySchema.Target.TOOLBAR_TEXT_STYLE,
                AppBarWidgetPropertySchema.Target.TOOLBAR_TEXT_STYLE_LOCALE,
                AppBarWidgetPropertySchema.Target.TOOLBAR_TEXT_STYLE_DECORATION,
                "toolbarTextStyle", valueIndent, path, node.id(), context, arguments);
        appendAppBarTextStyle(
                grouped,
                AppBarWidgetPropertySchema.Target.TITLE_TEXT_STYLE_THEME,
                AppBarWidgetPropertySchema.Target.TITLE_TEXT_STYLE,
                AppBarWidgetPropertySchema.Target.TITLE_TEXT_STYLE_LOCALE,
                AppBarWidgetPropertySchema.Target.TITLE_TEXT_STYLE_DECORATION,
                "titleTextStyle", valueIndent, path, node.id(), context, arguments);
        appendAppBarSystemUiOverlayStyle(
                grouped.get(AppBarWidgetPropertySchema.Target.SYSTEM_UI_OVERLAY_STYLE),
                valueIndent, path, node.id(), context, arguments);
    }

    private void appendAppBarNotificationPredicate(
            List<AppBarMember> members,
            String path,
            StableId widgetId,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        if (members == null || members.isEmpty()) {
            return;
        }
        AppBarMember member = members.getFirst();
        if (!(member.value() instanceof PropertyValue.StringValue preset)) {
            throw catalogInconsistency(
                    member.propertyPath(path), widgetId,
                    "AppBar notificationPredicate preset must be a string.");
        }
        String propertyPath = member.propertyPath(path);
        RenderedValue rendered = switch (preset.value()) {
            case "default" -> {
                RenderedSymbol symbol = context.planner().renderedSymbol(
                        MATERIAL_IMPORT, "defaultScrollNotificationPredicate");
                yield scalar(
                        symbol.text(), false, propertyPath, widgetId, context,
                        List.of(occurrence(
                                "widget:" + widgetId + ":app-bar:notification-predicate",
                                symbol.nameOffset(), symbol.name(), symbol.libraryUri(),
                                propertyPath, Optional.of(widgetId))));
            }
            case "depthZero" -> scalar(
                    "(notification) => notification.depth == 0",
                    false, propertyPath, widgetId, context);
            case "all" -> scalar(
                    "(_) => true", false, propertyPath, widgetId, context);
            default -> throw catalogInconsistency(
                    propertyPath, widgetId,
                    "Unsupported validated AppBar notificationPredicate preset '"
                    + preset.value() + "'.");
        };
        arguments.add(new ConstructorArgument(
                member.property().parameter(), "notificationPredicate", false, rendered));
    }

    private void appendAppBarShape(
            Map<AppBarWidgetPropertySchema.Target, List<AppBarMember>> grouped,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        List<AppBarMember> kinds = grouped.get(AppBarWidgetPropertySchema.Target.SHAPE_KIND);
        if (kinds == null || kinds.isEmpty()) {
            return;
        }
        AppBarMember kindMember = kinds.getFirst();
        if (!(kindMember.value() instanceof PropertyValue.StringValue kind)) {
            throw catalogInconsistency(
                    kindMember.propertyPath(path), widgetId,
                    "AppBar shapeKind must be a string.");
        }
        List<AppBarMember> shape = grouped.get(AppBarWidgetPropertySchema.Target.SHAPE);
        ArrayList<CompositeMember> outer = new ArrayList<>();

        ArrayList<CompositeMember> side = appBarMembers(shape, "shapeSide");
        if (!side.isEmpty()) {
            side.sort(COMPOSITE_MEMBER_ORDER);
            outer.add(new CompositeMember(
                    "side", 0,
                    renderNamedCompositeMembers(
                            "BorderSide", Optional.empty(), side, valueIndent + 2,
                            path + "/properties/shape/side", widgetId, context)));
        }

        ArrayList<CompositeMember> radii = new ArrayList<>();
        if (shape != null) {
            for (AppBarMember member : shape) {
                if (!member.property().name().value().startsWith("shapeRadius")) {
                    continue;
                }
                radii.add(new CompositeMember(
                        member.binding().dartName(), member.binding().dartOrder(),
                        renderPositionalComposite(
                                "Radius", Optional.of("circular"), member.rendered(),
                                member.propertyPath(path), widgetId, context)));
            }
        }
        if (!radii.isEmpty()) {
            radii.sort(COMPOSITE_MEMBER_ORDER);
            outer.add(new CompositeMember(
                    "borderRadius", 1,
                    renderNamedCompositeMembers(
                            "BorderRadius", Optional.of("only"), radii, valueIndent + 2,
                            path + "/properties/shape/borderRadius", widgetId, context)));
        }

        if (shape != null) {
            shape.stream()
                    .filter(member -> member.property().name().value()
                            .equals("shapeCircleEccentricity"))
                    .findFirst()
                    .ifPresent(member -> outer.add(new CompositeMember(
                            "eccentricity", 1, member.rendered())));
        }

        String dartClass = switch (kind.value()) {
            case "roundedRectangle" -> "RoundedRectangleBorder";
            case "stadium" -> "StadiumBorder";
            case "circle" -> "CircleBorder";
            case "beveledRectangle" -> "BeveledRectangleBorder";
            case "continuousRectangle" -> "ContinuousRectangleBorder";
            default -> throw catalogInconsistency(
                    kindMember.propertyPath(path), widgetId,
                    "Unsupported validated AppBar shapeKind '" + kind.value() + "'.");
        };
        outer.sort(COMPOSITE_MEMBER_ORDER);
        RenderedValue rendered = renderNamedCompositeMembers(
                dartClass, Optional.empty(), outer, valueIndent,
                path + "/properties/shape", widgetId, context);
        arguments.add(new ConstructorArgument(
                kindMember.property().parameter(), "shape", false, rendered));
    }

    private ArrayList<CompositeMember> appBarMembers(
            List<AppBarMember> members,
            String propertyPrefix) {
        ArrayList<CompositeMember> result = new ArrayList<>();
        if (members == null) {
            return result;
        }
        for (AppBarMember member : members) {
            if (member.property().name().value().startsWith(propertyPrefix)) {
                result.add(new CompositeMember(
                        member.binding().dartName(), member.binding().dartOrder(),
                        member.rendered()));
            }
        }
        return result;
    }

    private void appendAppBarIconTheme(
            List<AppBarMember> members,
            String argumentName,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        if (members == null || members.isEmpty()) {
            return;
        }
        ArrayList<CompositeMember> values = appBarMembers(members, "");
        values.sort(COMPOSITE_MEMBER_ORDER);
        arguments.add(new ConstructorArgument(
                minimumParameter(members), argumentName, false,
                renderNamedCompositeMembers(
                        "IconThemeData", Optional.empty(), values, valueIndent,
                        path + "/properties/" + argumentName, widgetId, context)));
    }

    private void appendAppBarTextStyle(
            Map<AppBarWidgetPropertySchema.Target, List<AppBarMember>> grouped,
            AppBarWidgetPropertySchema.Target themeTarget,
            AppBarWidgetPropertySchema.Target styleTarget,
            AppBarWidgetPropertySchema.Target localeTarget,
            AppBarWidgetPropertySchema.Target decorationTarget,
            String argumentName,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        List<AppBarMember> themeMembers = grouped.get(themeTarget);
        List<AppBarMember> styleMembers = grouped.get(styleTarget);
        List<AppBarMember> localeMembers = grouped.get(localeTarget);
        List<AppBarMember> decorationMembers = grouped.get(decorationTarget);
        ArrayList<AppBarMember> all = new ArrayList<>();
        if (themeMembers != null) {
            all.addAll(themeMembers);
        }
        if (styleMembers != null) {
            all.addAll(styleMembers);
        }
        if (localeMembers != null) {
            all.addAll(localeMembers);
        }
        if (decorationMembers != null) {
            all.addAll(decorationMembers);
        }
        if (all.isEmpty()) {
            return;
        }

        ArrayList<CompositeMember> values = appBarMembers(styleMembers, "");
        RenderedValue locale = renderAppBarLocale(
                localeMembers, valueIndent + 2,
                path + "/properties/" + argumentName + "/locale",
                widgetId, context);
        if (locale != null) {
            values.add(new CompositeMember("locale", 11, locale));
        }
        RenderedValue decoration = renderAppBarTextDecoration(
                decorationMembers,
                path + "/properties/" + argumentName + "/decoration",
                widgetId, context);
        if (decoration != null) {
            values.add(new CompositeMember("decoration", 17, decoration));
        }
        RenderedValue theme = themeMembers == null || themeMembers.isEmpty()
                ? null : themeMembers.getFirst().rendered();
        RenderedValue rendered;
        if (theme != null && values.isEmpty()) {
            rendered = theme;
        } else if (theme != null) {
            values.sort(COMPOSITE_MEMBER_ORDER);
            rendered = renderTextStyleCopyWith(
                    theme, values, path + "/properties/" + argumentName,
                    widgetId, context);
        } else {
            values.sort(COMPOSITE_MEMBER_ORDER);
            rendered = renderNamedCompositeMembers(
                    "TextStyle", Optional.empty(), values, valueIndent,
                    path + "/properties/" + argumentName, widgetId, context);
        }
        arguments.add(new ConstructorArgument(
                minimumParameter(all), argumentName, false, rendered));
    }

    private RenderedValue renderAppBarLocale(
            List<AppBarMember> members,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        if (members == null || members.isEmpty()) {
            return null;
        }
        ArrayList<CompositeMember> values = appBarMembers(members, "");
        values.sort(COMPOSITE_MEMBER_ORDER);
        return renderNamedCompositeMembers(
                "Locale", Optional.of("fromSubtags"), values,
                valueIndent, path, widgetId, context);
    }

    private RenderedValue renderAppBarTextDecoration(
            List<AppBarMember> members,
            String path,
            StableId widgetId,
            GenerationContext context) {
        if (members == null || members.isEmpty()) {
            return null;
        }
        ArrayList<String> enabled = new ArrayList<>();
        for (AppBarMember member : members) {
            if (member.value() instanceof PropertyValue.BooleanValue flag && flag.value()) {
                enabled.add(member.binding().dartName());
            }
        }
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "TextDecoration");
        if (enabled.isEmpty()) {
            return scalar(symbol.text() + ".none", true, path, widgetId, context);
        }
        if (enabled.size() == 1) {
            return scalar(symbol.text() + "." + enabled.getFirst(),
                    true, path, widgetId, context);
        }
        String values = enabled.stream()
                .map(value -> symbol.text() + "." + value)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        return scalar(
                symbol.text() + ".combine(const <" + symbol.text() + ">["
                + values + "])", false, path, widgetId, context);
    }

    private void appendAppBarSystemUiOverlayStyle(
            List<AppBarMember> members,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        if (members == null || members.isEmpty()) {
            return;
        }
        ArrayList<CompositeMember> values = appBarMembers(members, "");
        values.sort(COMPOSITE_MEMBER_ORDER);
        arguments.add(new ConstructorArgument(
                minimumParameter(members), "systemOverlayStyle", false,
                renderNamedCompositeMembers(
                        SERVICES_IMPORT, "SystemUiOverlayStyle", Optional.empty(),
                        values, valueIndent,
                        path + "/properties/systemOverlayStyle", widgetId, context)));
    }

    private static DartParameter minimumParameter(List<AppBarMember> members) {
        return members.stream()
                .map(member -> member.property().parameter())
                .min(Comparator.comparingInt(DartParameter::order))
                .orElseThrow();
    }

    private static GenerationAbort catalogInconsistency(
            String path,
            StableId widgetId,
            String message) {
        return abort(diagnostic(
                DartGenerationDiagnosticCode.INTERNAL_CATALOG_INCONSISTENCY,
                path, Optional.of(widgetId), Optional.of(DartManagedRegionId.BUILD),
                message));
    }

    private void appendTextCompoundArguments(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            int valueIndent,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        Map<TextWidgetPropertySchema.Target, List<TextMember>> grouped = new HashMap<>();
        for (PropertyDefinition property : definition.properties()) {
            TextWidgetPropertySchema.Definition binding = TextWidgetPropertySchema
                    .find(property.name()).orElse(null);
            if (binding == null || binding.target() == TextWidgetPropertySchema.Target.DIRECT) {
                continue;
            }
            PropertyValue value = node.properties().get(property.name());
            if (value == null) {
                continue;
            }
            String propertyPath = path + "/properties/" + pointer(property.name().value());
            RenderedValue rendered = switch (binding.encoding()) {
                case SCALAR -> renderProperty(
                        value, property, propertyPath, node.id(), context);
                case NEWLINE_STRING_LIST -> renderStringList(
                        value, propertyPath, node.id(), context);
                case DECORATION_FLAG -> renderProperty(
                        value, property, propertyPath, node.id(), context);
            };
            grouped.computeIfAbsent(binding.target(), ignored -> new ArrayList<>())
                    .add(new TextMember(property, binding, value, rendered));
        }

        addTextLocaleArgument(
                grouped.get(TextWidgetPropertySchema.Target.TEXT_LOCALE),
                "locale", 2, valueIndent, path, node.id(), context, arguments);

        List<TextMember> scalers = grouped.get(TextWidgetPropertySchema.Target.TEXT_SCALER);
        if (scalers != null && !scalers.isEmpty()) {
            TextMember scaler = scalers.getFirst();
            RenderedValue rendered = renderPositionalComposite(
                    "TextScaler", Optional.of("linear"), scaler.rendered(),
                    scaler.propertyPath(path), node.id(), context);
            arguments.add(new ConstructorArgument(
                    DartParameter.named(3, false), "textScaler", false, rendered));
        }

        List<TextMember> height = grouped.get(
                TextWidgetPropertySchema.Target.TEXT_HEIGHT_BEHAVIOR);
        if (height != null && !height.isEmpty()) {
            arguments.add(new ConstructorArgument(
                    DartParameter.named(7, false),
                    "textHeightBehavior",
                    false,
                    renderNamedComposite(
                            "TextHeightBehavior", Optional.empty(), height,
                            valueIndent, path + "/properties/textHeightBehavior",
                            node.id(), context)));
        }

        ArrayList<CompositeMember> styleMembers = scalarMembers(
                grouped.get(TextWidgetPropertySchema.Target.TEXT_STYLE));
        RenderedValue styleLocale = renderLocale(
                grouped.get(TextWidgetPropertySchema.Target.TEXT_STYLE_LOCALE),
                valueIndent + 2, path + "/properties/styleLocale", node.id(), context);
        if (styleLocale != null) {
            styleMembers.add(new CompositeMember("locale", 11, styleLocale));
        }
        RenderedValue decoration = renderTextDecoration(
                grouped.get(TextWidgetPropertySchema.Target.TEXT_STYLE_DECORATION),
                path + "/properties/styleDecoration", node.id(), context);
        if (decoration != null) {
            styleMembers.add(new CompositeMember("decoration", 17, decoration));
        }
        List<TextMember> themeStyles = grouped.get(
                TextWidgetPropertySchema.Target.TEXT_STYLE_THEME);
        RenderedValue themeStyle = themeStyles == null || themeStyles.isEmpty()
                ? null : themeStyles.getFirst().rendered();
        if (themeStyle != null && styleMembers.isEmpty()) {
            arguments.add(new ConstructorArgument(
                    DartParameter.named(0, false), "style", false, themeStyle));
        } else if (themeStyle != null) {
            styleMembers.sort(COMPOSITE_MEMBER_ORDER);
            arguments.add(new ConstructorArgument(
                    DartParameter.named(0, false),
                    "style",
                    false,
                    renderTextStyleCopyWith(
                            themeStyle, styleMembers, path + "/properties/style",
                            node.id(), context)));
        } else if (!styleMembers.isEmpty()) {
            styleMembers.sort(COMPOSITE_MEMBER_ORDER);
            arguments.add(new ConstructorArgument(
                    DartParameter.named(0, false),
                    "style",
                    false,
                    renderNamedCompositeMembers(
                            "TextStyle", Optional.empty(), styleMembers,
                            valueIndent, path + "/properties/style", node.id(), context)));
        }

        List<TextMember> strut = grouped.get(TextWidgetPropertySchema.Target.STRUT_STYLE);
        if (strut != null && !strut.isEmpty()) {
            arguments.add(new ConstructorArgument(
                    DartParameter.named(1, false),
                    "strutStyle",
                    false,
                    renderNamedComposite(
                            "StrutStyle", Optional.empty(), strut,
                            valueIndent, path + "/properties/strutStyle",
                            node.id(), context)));
        }
    }

    private void addTextLocaleArgument(
            List<TextMember> members,
            String argumentName,
            int argumentOrder,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        RenderedValue locale = renderLocale(
                members, valueIndent, path + "/properties/locale", widgetId, context);
        if (locale != null) {
            arguments.add(new ConstructorArgument(
                    DartParameter.named(argumentOrder, false),
                    argumentName,
                    false,
                    locale));
        }
    }

    private RenderedValue renderLocale(
            List<TextMember> members,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        if (members == null || members.isEmpty()) {
            return null;
        }
        return renderNamedComposite(
                "Locale", Optional.of("fromSubtags"), members,
                valueIndent, path, widgetId, context);
    }

    private RenderedValue renderTextDecoration(
            List<TextMember> members,
            String path,
            StableId widgetId,
            GenerationContext context) {
        if (members == null || members.isEmpty()) {
            return null;
        }
        ArrayList<String> enabled = new ArrayList<>();
        for (TextMember member : members) {
            PropertyValue value = nodeValue(member);
            if (value instanceof PropertyValue.BooleanValue flag && flag.value()) {
                enabled.add(member.binding().dartName());
            }
        }
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "TextDecoration");
        if (enabled.isEmpty()) {
            return scalar(symbol.text() + ".none", true, path, widgetId, context);
        }
        if (enabled.size() == 1) {
            return scalar(symbol.text() + "." + enabled.getFirst(),
                    true, path, widgetId, context);
        }
        String values = enabled.stream()
                .map(value -> symbol.text() + "." + value)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        return scalar(
                symbol.text() + ".combine(const <" + symbol.text() + ">[" + values + "])",
                false, path, widgetId, context);
    }

    private static PropertyValue nodeValue(TextMember member) {
        return member.value();
    }

    private ArrayList<CompositeMember> scalarMembers(List<TextMember> members) {
        ArrayList<CompositeMember> result = new ArrayList<>();
        if (members == null) {
            return result;
        }
        for (TextMember member : members) {
            result.add(new CompositeMember(
                    member.binding().dartName(),
                    member.binding().dartOrder(),
                    member.rendered()));
        }
        return result;
    }

    private RenderedValue renderNamedComposite(
            String dartClass,
            Optional<String> namedConstructor,
            List<TextMember> members,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        ArrayList<CompositeMember> values = scalarMembers(members);
        values.sort(COMPOSITE_MEMBER_ORDER);
        return renderNamedCompositeMembers(
                dartClass, namedConstructor, values, valueIndent,
                path, widgetId, context);
    }

    private RenderedValue renderNamedCompositeMembers(
            String dartClass,
            Optional<String> namedConstructor,
            List<CompositeMember> members,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        return renderNamedCompositeMembers(
                WIDGETS_IMPORT, dartClass, namedConstructor, members,
                valueIndent, path, widgetId, context);
    }

    private RenderedValue renderNamedCompositeMembers(
            String libraryUri,
            String dartClass,
            Optional<String> namedConstructor,
            List<CompositeMember> members,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        boolean constant = members.stream().allMatch(
                member -> member.rendered().constant());
        RenderedSymbol symbol = context.planner().renderedSymbol(
                libraryUri, dartClass);
        String constructor = (constant ? "const " : "") + symbol.text()
                + namedConstructor.map(value -> "." + value).orElse("");
        GeneratedDartSymbolOccurrence classOccurrence = occurrence(
                "widget:" + widgetId + ":compound:" + dartClass + ':' + path,
                (constant ? "const ".length() : 0) + symbol.nameOffset(),
                symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId));

        StringBuilder inline = new StringBuilder(constructor).append('(');
        ArrayList<GeneratedDartSymbolOccurrence> inlineOccurrences = new ArrayList<>();
        inlineOccurrences.add(classOccurrence);
        for (int index = 0; index < members.size(); index++) {
            if (index > 0) {
                inline.append(", ");
            }
            CompositeMember member = members.get(index);
            inline.append(member.name()).append(": ");
            int valueOffset = inline.length();
            inline.append(member.rendered().joined());
            shiftInto(inlineOccurrences, member.rendered().symbolOccurrences(), valueOffset);
        }
        inline.append(')');
        if (members.stream().allMatch(value -> value.rendered().lines().size() == 1)
                && inline.codePointCount(0, inline.length()) <= INLINE_CONSTRUCTOR_LIMIT) {
            return scalar(inline.toString(), constant, path, widgetId, context,
                    inlineOccurrences);
        }

        LineAccumulator lines = new LineAccumulator(
                context.maxRenderedUtf8Bytes(), path, widgetId);
        lines.add(constructor + "(", List.of(classOccurrence));
        int memberIndent = valueIndent + 2;
        for (CompositeMember member : members) {
            String prefix = spaces(memberIndent) + member.name() + ": ";
            lines.addBlock(
                    prefix + member.rendered().joined() + ',',
                    member.rendered().symbolOccurrences(),
                    prefix.length());
        }
        lines.add(spaces(valueIndent) + ')');
        return lines.build(constant);
    }

    private RenderedValue renderPositionalComposite(
            String dartClass,
            Optional<String> namedConstructor,
            RenderedValue argument,
            String path,
            StableId widgetId,
            GenerationContext context) {
        boolean constant = argument.constant();
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartClass);
        String constructor = (constant ? "const " : "") + symbol.text()
                + namedConstructor.map(value -> "." + value).orElse("");
        String text = constructor + '(' + argument.joined() + ')';
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        occurrences.add(occurrence(
                "widget:" + widgetId + ":compound:" + dartClass + ':' + path,
                (constant ? "const ".length() : 0) + symbol.nameOffset(),
                symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId)));
        shiftInto(occurrences, argument.symbolOccurrences(), constructor.length() + 1);
        return scalar(text, constant, path, widgetId, context, occurrences);
    }

    private RenderedValue renderStringList(
            PropertyValue value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        if (!(value instanceof PropertyValue.StringValue string)) {
            throw abort(diagnostic(
                    DartGenerationDiagnosticCode.INTERNAL_CATALOG_INCONSISTENCY,
                    path, Optional.of(widgetId), Optional.of(DartManagedRegionId.BUILD),
                    "A newline-delimited string list requires a string model value."));
        }
        List<String> values = string.value().lines()
                .map(String::strip)
                .filter(item -> !item.isEmpty())
                .toList();
        StringBuilder rendered = new StringBuilder("const <String>[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                rendered.append(", ");
            }
            rendered.append(dartString(
                    values.get(index), path, widgetId,
                    context.maxRenderedUtf8Bytes()));
        }
        rendered.append(']');
        return scalar(rendered.toString(), true, path, widgetId, context);
    }

    private RenderedValue renderThemeToken(
            ThemeToken token,
            String path,
            StableId widgetId,
            GenerationContext context) {
        String receiver;
        if (MaterialThemeTokenCatalog.colorRole(token).isPresent()) {
            receiver = "colorScheme." + MaterialThemeTokenCatalog.colorRole(token).orElseThrow();
        } else if (MaterialThemeTokenCatalog.textStyleRole(token).isPresent()) {
            receiver = "textTheme." + MaterialThemeTokenCatalog.textStyleRole(token).orElseThrow();
        } else {
            throw abort(diagnostic(
                    DartGenerationDiagnosticCode.INTERNAL_CATALOG_INCONSISTENCY,
                    path,
                    Optional.of(widgetId),
                    Optional.of(DartManagedRegionId.BUILD),
                    "Validated property contains an unreviewed Material theme token '"
                    + token.wireId() + "'."));
        }
        RenderedSymbol theme = context.planner().renderedSymbol(MATERIAL_IMPORT, "Theme");
        String rendered = theme.text() + ".of(context)." + receiver;
        return scalar(
                rendered,
                false,
                path,
                widgetId,
                context,
                List.of(occurrence(
                        "widget:" + widgetId + ":theme-token:" + token.wireId(),
                        theme.nameOffset(),
                        theme.name(),
                        theme.libraryUri(),
                        path,
                        Optional.of(widgetId))));
    }

    private RenderedValue renderColorSource(
            ColorSource source,
            String path,
            StableId widgetId,
            GenerationContext context) {
        return switch (source) {
            case ColorSource.Literal literal -> {
                RenderedSymbol color = context.planner().renderedSymbol(WIDGETS_IMPORT, "Color");
                String rendered = "const " + color.text() + "(" + literal.wireArgb() + ")";
                yield scalar(
                        rendered,
                        true,
                        path,
                        widgetId,
                        context,
                        List.of(occurrence(
                                "widget:" + widgetId + ":structured-color:" + path,
                                "const ".length() + color.nameOffset(),
                                color.name(),
                                color.libraryUri(),
                                path,
                                Optional.of(widgetId))));
            }
            case ColorSource.Theme theme ->
                renderThemeToken(theme.token(), path, widgetId, context);
        };
    }

    private RenderedValue renderAlignmentGeometry(
            PropertyValue.AlignmentGeometryValue value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        String dartClass = value.basis()
                == PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL
                ? "Alignment" : "AlignmentDirectional";
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartClass);
        String rendered = "const " + symbol.text() + '('
                + dartDouble(value.horizontal()) + ", "
                + dartDouble(value.vertical()) + ')';
        return scalar(rendered, true, path, widgetId, context,
                List.of(occurrence(
                        "widget:" + widgetId + ":alignment:" + path,
                        "const ".length() + symbol.nameOffset(),
                        symbol.name(), symbol.libraryUri(), path,
                        Optional.of(widgetId))));
    }

    private RenderedValue renderBoxConstraints(
            PropertyValue.BoxConstraintsValue value,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        ArrayList<CompositeMember> members = new ArrayList<>();
        members.add(new CompositeMember("minWidth", 0,
                scalar(dartBoxConstraintBound(value.minWidth()), true, path + "/minWidth",
                        widgetId, context)));
        if (!value.maxWidth().infinite()) {
            members.add(new CompositeMember(
                    "maxWidth", 1,
                    scalar(dartBoxConstraintBound(value.maxWidth()), true,
                            path + "/maxWidth", widgetId, context)));
        }
        members.add(new CompositeMember("minHeight", 2,
                scalar(dartBoxConstraintBound(value.minHeight()), true, path + "/minHeight",
                        widgetId, context)));
        if (!value.maxHeight().infinite()) {
            members.add(new CompositeMember(
                    "maxHeight", 3,
                    scalar(dartBoxConstraintBound(value.maxHeight()), true,
                            path + "/maxHeight", widgetId, context)));
        }
        return renderNamedCompositeMembers(
                "BoxConstraints", Optional.empty(), members, valueIndent,
                path, widgetId, context);
    }

    private RenderedValue renderSize(
            PropertyValue.SizeValue value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "Size");
        String rendered = symbol.text() + '(' + dartDouble(value.width())
                + ", " + dartDouble(value.height()) + ')';
        return scalar(rendered, true, path, widgetId, context,
                List.of(occurrence(
                        "widget:" + widgetId + ":size:" + path,
                        symbol.nameOffset(), symbol.name(), symbol.libraryUri(),
                        path, Optional.of(widgetId))));
    }

    private RenderedValue renderOffset(
            PropertyValue.OffsetValue value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "Offset");
        String rendered = "const " + symbol.text() + '(' + dartDouble(value.dx())
                + ", " + dartDouble(value.dy()) + ')';
        return scalar(rendered, true, path, widgetId, context,
                List.of(occurrence(
                        "widget:" + widgetId + ":offset:" + path,
                        "const ".length() + symbol.nameOffset(),
                        symbol.name(), symbol.libraryUri(), path,
                        Optional.of(widgetId))));
    }

    private RenderedValue renderMatrix4(
            PropertyValue.Matrix4Value value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "Matrix4");
        String storage = value.storage().stream()
                .map(DartRegionGenerator::dartDouble)
                .collect(Collectors.joining(", "));
        String rendered = symbol.text() + ".fromList(<double>[" + storage + "])";
        return scalar(rendered, false, path, widgetId, context,
                List.of(occurrence(
                        "widget:" + widgetId + ":matrix4:" + path,
                        symbol.nameOffset(), symbol.name(), VECTOR_MATH_64_LIBRARY,
                        path, Optional.of(widgetId))));
    }

    private RenderedValue renderBoxDecoration(
            PropertyValue.BoxDecorationValue value,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        ArrayList<CompositeMember> members = new ArrayList<>();
        value.color().ifPresent(color -> members.add(new CompositeMember(
                "color", 0,
                renderColorSource(color, path + "/color", widgetId, context))));
        value.image().ifPresent(image -> members.add(new CompositeMember(
                "image", 1,
                renderDecorationImage(
                        image, valueIndent + 2, path + "/image",
                        widgetId, context))));
        value.border().ifPresent(border -> members.add(new CompositeMember(
                "border", 2,
                renderBoxBorder(border, valueIndent + 2, path + "/border",
                        widgetId, context))));
        value.borderRadius().ifPresent(radius -> members.add(new CompositeMember(
                "borderRadius", 3,
                renderBorderRadius(radius, valueIndent + 2,
                        path + "/borderRadius", widgetId, context))));
        if (!value.boxShadow().isEmpty()) {
            members.add(new CompositeMember(
                    "boxShadow", 4,
                    renderBoxShadows(value.boxShadow(), valueIndent + 2,
                            path + "/boxShadow", widgetId, context)));
        }
        value.gradient().ifPresent(gradient -> members.add(new CompositeMember(
                "gradient", 5,
                renderBoxGradient(gradient, valueIndent + 2,
                        path + "/gradient", widgetId, context))));
        value.backgroundBlendMode().ifPresent(blend -> members.add(
                new CompositeMember(
                        "backgroundBlendMode", 6,
                        renderEnumSymbol(
                                "BlendMode", blend.wireName(), "background-blend-mode",
                                path + "/backgroundBlendMode", widgetId, context))));
        members.add(new CompositeMember(
                "shape", 7,
                renderEnumSymbol(
                        "BoxShape", value.shape().wireName(), "box-shape",
                        path + "/shape", widgetId, context)));
        return renderNamedCompositeMembers(
                "BoxDecoration", Optional.empty(), members, valueIndent,
                path, widgetId, context);
    }

    private static String dartBoxConstraintBound(
            PropertyValue.BoxConstraintBound bound) {
        return bound.finiteValue().map(DartRegionGenerator::dartDouble)
                .orElse("double.infinity");
    }

    private RenderedValue renderImageProvider(
            PropertyValue.ImageProviderValue value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        if (value.isUnresolved()) {
            return renderUnresolvedImageProvider(path, widgetId, context);
        }
        String dartClass = value.providerKind()
                == PropertyValue.ImageProviderValue.ProviderKind.ASSET
                ? "AssetImage" : "ExactAssetImage";
        RenderedSymbol providerSymbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartClass);
        StringBuilder rendered = new StringBuilder("const ");
        int providerOffset = rendered.length() + providerSymbol.nameOffset();
        rendered.append(providerSymbol.text())
                .append('(')
                .append(dartString(
                        value.assetName(), path + "/assetName", widgetId,
                        context.maxRenderedUtf8Bytes()));
        value.exactScale().ifPresent(scale -> rendered
                .append(", scale: ")
                .append(dartDouble(scale)));
        value.packageName().ifPresent(packageName -> rendered
                .append(", package: ")
                .append(dartString(
                        packageName, path + "/packageName", widgetId,
                        context.maxRenderedUtf8Bytes())));
        rendered.append(')');
        RenderedValue provider = scalar(
                rendered.toString(),
                true,
                path,
                widgetId,
                context,
                List.of(occurrence(
                        "widget:" + widgetId + ":image-provider:" + path,
                        providerOffset,
                        providerSymbol.name(),
                        providerSymbol.libraryUri(),
                        path,
                        Optional.of(widgetId))));
        if (value.resize().isEmpty()) {
            return provider;
        }

        PropertyValue.ImageProviderValue.ResizeImageConfig resize =
                value.resize().orElseThrow();
        RenderedSymbol resizeSymbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "ResizeImage");
        RenderedSymbol policySymbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "ResizeImagePolicy");
        StringBuilder wrapper = new StringBuilder("const ");
        int resizeOffset = wrapper.length() + resizeSymbol.nameOffset();
        wrapper.append(resizeSymbol.text()).append('(');
        int providerValueOffset = wrapper.length();
        wrapper.append(provider.joined());
        resize.width().ifPresent(width -> wrapper
                .append(", width: ").append(width));
        resize.height().ifPresent(height -> wrapper
                .append(", height: ").append(height));
        wrapper.append(", policy: ");
        int policyOffset = wrapper.length() + policySymbol.nameOffset();
        wrapper.append(policySymbol.text())
                .append('.')
                .append(resize.policy().wireName())
                .append(", allowUpscaling: ")
                .append(resize.allowUpscaling())
                .append(')');
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        occurrences.add(occurrence(
                "widget:" + widgetId + ":resize-image:" + path,
                resizeOffset,
                resizeSymbol.name(),
                resizeSymbol.libraryUri(),
                path,
                Optional.of(widgetId)));
        shiftInto(occurrences, provider.symbolOccurrences(), providerValueOffset);
        occurrences.add(occurrence(
                "widget:" + widgetId + ":resize-policy:" + path,
                policyOffset,
                policySymbol.name(),
                policySymbol.libraryUri(),
                path + "/resize/policy",
                Optional.of(widgetId)));
        return scalar(
                wrapper.toString(), true, path, widgetId, context, occurrences);
    }

    private RenderedValue renderUnresolvedImageProvider(
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol memoryImage = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "MemoryImage");
        RenderedSymbol base64Decode = context.planner().renderedSymbol(
                DART_CONVERT_IMPORT, "base64Decode");
        String encoded = dartString(
                UNRESOLVED_IMAGE_BASE64,
                path,
                widgetId,
                context.maxRenderedUtf8Bytes());
        String rendered = memoryImage.text() + '(' + base64Decode.text()
                + '(' + encoded + "), scale: 0.125)";
        int decodeOffset = memoryImage.text().length() + 1
                + base64Decode.nameOffset();
        return scalar(
                rendered,
                false,
                path,
                widgetId,
                context,
                List.of(
                        occurrence(
                                "widget:" + widgetId
                                + ":unresolved-image-memory-provider:" + path,
                                memoryImage.nameOffset(),
                                memoryImage.name(),
                                memoryImage.libraryUri(),
                                path,
                                Optional.of(widgetId)),
                        occurrence(
                                "widget:" + widgetId
                                + ":unresolved-image-base64-decode:" + path,
                                decodeOffset,
                                base64Decode.name(),
                                base64Decode.libraryUri(),
                                path,
                                Optional.of(widgetId))));
    }

    private RenderedValue renderDecorationImage(
            PropertyValue.DecorationImageValue value,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        ArrayList<CompositeMember> members = new ArrayList<>();
        members.add(new CompositeMember(
                "image", 0,
                renderImageProvider(value.image(), path + "/image", widgetId, context)));
        value.onError().ifPresent(callback -> members.add(new CompositeMember(
                "onError", 1,
                scalar(callback.handler(), false, path + "/onError",
                        widgetId, context))));
        value.colorFilter().ifPresent(filter -> members.add(new CompositeMember(
                "colorFilter", 2,
                renderDecorationColorFilter(
                        filter, path + "/colorFilter", widgetId, context))));
        value.fit().ifPresent(fit -> members.add(new CompositeMember(
                "fit", 3,
                renderEnumSymbol(
                        "BoxFit", fit.wireName(), "decoration-image-fit",
                        path + "/fit", widgetId, context))));
        members.add(new CompositeMember(
                "alignment", 4,
                renderAlignmentGeometry(
                        value.alignment(), path + "/alignment", widgetId, context)));
        value.centerSlice().ifPresent(rect -> members.add(new CompositeMember(
                "centerSlice", 5,
                renderDecorationRect(rect, path + "/centerSlice", widgetId, context))));
        members.add(new CompositeMember(
                "repeat", 6,
                renderEnumSymbol(
                        "ImageRepeat", value.repeat().wireName(),
                        "decoration-image-repeat", path + "/repeat",
                        widgetId, context)));
        members.add(new CompositeMember(
                "matchTextDirection", 7,
                scalar(Boolean.toString(value.matchTextDirection()), true,
                        path + "/matchTextDirection", widgetId, context)));
        members.add(new CompositeMember(
                "scale", 8,
                scalar(dartDouble(value.scale()), true,
                        path + "/scale", widgetId, context)));
        members.add(new CompositeMember(
                "opacity", 9,
                scalar(dartDouble(value.opacity()), true,
                        path + "/opacity", widgetId, context)));
        members.add(new CompositeMember(
                "filterQuality", 10,
                renderEnumSymbol(
                        "FilterQuality", value.filterQuality().wireName(),
                        "decoration-image-filter-quality",
                        path + "/filterQuality", widgetId, context)));
        members.add(new CompositeMember(
                "invertColors", 11,
                scalar(Boolean.toString(value.invertColors()), true,
                        path + "/invertColors", widgetId, context)));
        members.add(new CompositeMember(
                "isAntiAlias", 12,
                scalar(Boolean.toString(value.isAntiAlias()), true,
                        path + "/isAntiAlias", widgetId, context)));
        return renderNamedCompositeMembers(
                "DecorationImage", Optional.empty(), members, valueIndent,
                path, widgetId, context);
    }

    private RenderedValue renderDecorationColorFilter(
            PropertyValue.DecorationImageValue.ColorFilter value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        if (value instanceof PropertyValue.DecorationImageValue.Mode mode) {
            return renderPositionalCompositeValues(
                    "ColorFilter",
                    Optional.of("mode"),
                    List.of(
                            renderColorSource(
                                    mode.color(), path + "/color", widgetId, context),
                            renderEnumSymbol(
                                    "BlendMode", mode.blendMode().wireName(),
                                    "decoration-image-color-filter-blend-mode",
                                    path + "/blendMode", widgetId, context)),
                    path,
                    widgetId,
                    context);
        }
        if (value instanceof PropertyValue.DecorationImageValue.Matrix matrix) {
            String values = matrix.values().stream()
                    .map(DartRegionGenerator::dartDouble)
                    .collect(Collectors.joining(", "));
            return renderPositionalCompositeValues(
                    "ColorFilter",
                    Optional.of("matrix"),
                    List.of(scalar(
                            "const <double>[" + values + "]", true,
                            path + "/values", widgetId, context)),
                    path,
                    widgetId,
                    context);
        }
        if (value instanceof PropertyValue.DecorationImageValue.LinearToSrgbGamma) {
            return renderNamedCompositeMembers(
                    "ColorFilter", Optional.of("linearToSrgbGamma"), List.of(),
                    0, path, widgetId, context);
        }
        if (value instanceof PropertyValue.DecorationImageValue.SrgbToLinearGamma) {
            return renderNamedCompositeMembers(
                    "ColorFilter", Optional.of("srgbToLinearGamma"), List.of(),
                    0, path, widgetId, context);
        }
        PropertyValue.DecorationImageValue.Saturation saturation =
                (PropertyValue.DecorationImageValue.Saturation) value;
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "ColorFilter");
        String rendered = symbol.text() + ".saturation("
                + dartDouble(saturation.value()) + ')';
        return scalar(
                rendered,
                false,
                path,
                widgetId,
                context,
                List.of(occurrence(
                        "widget:" + widgetId + ":color-filter-saturation:" + path,
                        symbol.nameOffset(),
                        symbol.name(),
                        symbol.libraryUri(),
                        path,
                        Optional.of(widgetId))));
    }

    private RenderedValue renderDecorationRect(
            PropertyValue.DecorationImageValue.Rect value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        return renderPositionalCompositeValues(
                "Rect",
                Optional.of("fromLTRB"),
                List.of(
                        scalar(dartDouble(value.left()), true,
                                path + "/left", widgetId, context),
                        scalar(dartDouble(value.top()), true,
                                path + "/top", widgetId, context),
                        scalar(dartDouble(value.right()), true,
                                path + "/right", widgetId, context),
                        scalar(dartDouble(value.bottom()), true,
                                path + "/bottom", widgetId, context)),
                path,
                widgetId,
                context);
    }

    private RenderedValue renderBoxBorder(
            PropertyValue.BoxDecorationValue.BoxBorder value,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        ArrayList<CompositeMember> members = new ArrayList<>();
        String dartClass;
        if (value instanceof PropertyValue.BoxDecorationValue.PhysicalBorder physical) {
            dartClass = "Border";
            members.add(new CompositeMember("top", 0,
                    renderBorderSide(physical.top(), valueIndent + 2,
                            path + "/top", widgetId, context)));
            members.add(new CompositeMember("right", 1,
                    renderBorderSide(physical.right(), valueIndent + 2,
                            path + "/right", widgetId, context)));
            members.add(new CompositeMember("bottom", 2,
                    renderBorderSide(physical.bottom(), valueIndent + 2,
                            path + "/bottom", widgetId, context)));
            members.add(new CompositeMember("left", 3,
                    renderBorderSide(physical.left(), valueIndent + 2,
                            path + "/left", widgetId, context)));
        } else {
            PropertyValue.BoxDecorationValue.DirectionalBorder directional =
                    (PropertyValue.BoxDecorationValue.DirectionalBorder) value;
            dartClass = "BorderDirectional";
            members.add(new CompositeMember("top", 0,
                    renderBorderSide(directional.top(), valueIndent + 2,
                            path + "/top", widgetId, context)));
            members.add(new CompositeMember("start", 1,
                    renderBorderSide(directional.start(), valueIndent + 2,
                            path + "/start", widgetId, context)));
            members.add(new CompositeMember("end", 2,
                    renderBorderSide(directional.end(), valueIndent + 2,
                            path + "/end", widgetId, context)));
            members.add(new CompositeMember("bottom", 3,
                    renderBorderSide(directional.bottom(), valueIndent + 2,
                            path + "/bottom", widgetId, context)));
        }
        return renderNamedCompositeMembers(
                dartClass, Optional.empty(), members, valueIndent,
                path, widgetId, context);
    }

    private RenderedValue renderBorderSide(
            PropertyValue.BoxDecorationValue.BorderSide value,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        return renderNamedCompositeMembers(
                "BorderSide",
                Optional.empty(),
                List.of(
                        new CompositeMember("color", 0,
                                renderColorSource(value.color(), path + "/color",
                                        widgetId, context)),
                        new CompositeMember("width", 1,
                                scalar(dartDouble(value.width()), true,
                                        path + "/width", widgetId, context)),
                        new CompositeMember("style", 2,
                                renderEnumSymbol(
                                        "BorderStyle", value.style().wireName(),
                                        "border-style", path + "/style",
                                        widgetId, context)),
                        new CompositeMember("strokeAlign", 3,
                                scalar(dartDouble(value.strokeAlign()), true,
                                        path + "/strokeAlign", widgetId, context))),
                valueIndent, path, widgetId, context);
    }

    private RenderedValue renderBorderRadius(
            PropertyValue.BoxDecorationValue.BorderRadiusGeometry value,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        ArrayList<CompositeMember> members = new ArrayList<>();
        String dartClass;
        if (value instanceof PropertyValue.BoxDecorationValue.PhysicalBorderRadius physical) {
            dartClass = "BorderRadius";
            members.add(new CompositeMember("topLeft", 0,
                    renderRadius(physical.topLeft(), path + "/topLeft", widgetId, context)));
            members.add(new CompositeMember("topRight", 1,
                    renderRadius(physical.topRight(), path + "/topRight", widgetId, context)));
            members.add(new CompositeMember("bottomRight", 2,
                    renderRadius(physical.bottomRight(), path + "/bottomRight", widgetId, context)));
            members.add(new CompositeMember("bottomLeft", 3,
                    renderRadius(physical.bottomLeft(), path + "/bottomLeft", widgetId, context)));
        } else {
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius directional =
                    (PropertyValue.BoxDecorationValue.DirectionalBorderRadius) value;
            dartClass = "BorderRadiusDirectional";
            members.add(new CompositeMember("topStart", 0,
                    renderRadius(directional.topStart(), path + "/topStart", widgetId, context)));
            members.add(new CompositeMember("topEnd", 1,
                    renderRadius(directional.topEnd(), path + "/topEnd", widgetId, context)));
            members.add(new CompositeMember("bottomEnd", 2,
                    renderRadius(directional.bottomEnd(), path + "/bottomEnd", widgetId, context)));
            members.add(new CompositeMember("bottomStart", 3,
                    renderRadius(directional.bottomStart(), path + "/bottomStart", widgetId, context)));
        }
        return renderNamedCompositeMembers(
                dartClass, Optional.of("only"), members, valueIndent,
                path, widgetId, context);
    }

    private RenderedValue renderRadius(
            PropertyValue.BoxDecorationValue.Radius value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        return renderPositionalCompositeValues(
                "Radius", Optional.of("elliptical"),
                List.of(
                        scalar(dartDouble(value.x()), true,
                                path + "/x", widgetId, context),
                        scalar(dartDouble(value.y()), true,
                                path + "/y", widgetId, context)),
                path, widgetId, context);
    }

    private RenderedValue renderBoxShadows(
            List<PropertyValue.BoxDecorationValue.BoxShadow> values,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol type = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "BoxShadow");
        ArrayList<RenderedValue> rendered = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            PropertyValue.BoxDecorationValue.BoxShadow value = values.get(index);
            String itemPath = path + '/' + index;
            RenderedValue offset = renderPositionalCompositeValues(
                    "Offset", Optional.empty(),
                    List.of(
                            scalar(dartDouble(value.offsetX()), true,
                                    itemPath + "/offsetX", widgetId, context),
                            scalar(dartDouble(value.offsetY()), true,
                                    itemPath + "/offsetY", widgetId, context)),
                    itemPath + "/offset", widgetId, context);
            rendered.add(renderNamedCompositeMembers(
                    "BoxShadow", Optional.empty(),
                    List.of(
                            new CompositeMember("color", 0,
                                    renderColorSource(value.color(), itemPath + "/color",
                                            widgetId, context)),
                            new CompositeMember("offset", 1, offset),
                            new CompositeMember("blurRadius", 2,
                                    scalar(dartDouble(value.blurRadius()), true,
                                            itemPath + "/blurRadius", widgetId, context)),
                            new CompositeMember("spreadRadius", 3,
                                    scalar(dartDouble(value.spreadRadius()), true,
                                            itemPath + "/spreadRadius", widgetId, context)),
                            new CompositeMember("blurStyle", 4,
                                    renderEnumSymbol(
                                            "BlurStyle", value.blurStyle().wireName(),
                                            "box-shadow-blur-style", itemPath + "/blurStyle",
                                            widgetId, context))),
                    valueIndent, itemPath, widgetId, context));
        }
        return renderTypedList(
                type, rendered, "box-shadow", path, widgetId, context);
    }

    private RenderedValue renderBoxGradient(
            PropertyValue.BoxDecorationValue.BoxGradient value,
            int valueIndent,
            String path,
            StableId widgetId,
            GenerationContext context) {
        ArrayList<CompositeMember> members = new ArrayList<>();
        String dartClass;
        if (value instanceof PropertyValue.BoxDecorationValue.LinearGradient linear) {
            dartClass = "LinearGradient";
            members.add(new CompositeMember("begin", 0,
                    renderAlignmentGeometry(linear.begin(), path + "/begin", widgetId, context)));
            members.add(new CompositeMember("end", 1,
                    renderAlignmentGeometry(linear.end(), path + "/end", widgetId, context)));
            appendGradientCommon(members, 2, linear, path, widgetId, context);
        } else if (value instanceof PropertyValue.BoxDecorationValue.RadialGradient radial) {
            dartClass = "RadialGradient";
            members.add(new CompositeMember("center", 0,
                    renderAlignmentGeometry(radial.center(), path + "/center", widgetId, context)));
            members.add(new CompositeMember("radius", 1,
                    scalar(dartDouble(radial.radius()), true,
                            path + "/radius", widgetId, context)));
            appendGradientCommon(members, 2, radial, path, widgetId, context);
            radial.focal().ifPresent(focal -> members.add(new CompositeMember(
                    "focal", 6,
                    renderAlignmentGeometry(focal, path + "/focal", widgetId, context))));
            members.add(new CompositeMember("focalRadius", 7,
                    scalar(dartDouble(radial.focalRadius()), true,
                            path + "/focalRadius", widgetId, context)));
        } else {
            PropertyValue.BoxDecorationValue.SweepGradient sweep =
                    (PropertyValue.BoxDecorationValue.SweepGradient) value;
            dartClass = "SweepGradient";
            members.add(new CompositeMember("center", 0,
                    renderAlignmentGeometry(sweep.center(), path + "/center", widgetId, context)));
            members.add(new CompositeMember("startAngle", 1,
                    scalar(dartDouble(sweep.startAngle()), true,
                            path + "/startAngle", widgetId, context)));
            members.add(new CompositeMember("endAngle", 2,
                    scalar(dartDouble(sweep.endAngle()), true,
                            path + "/endAngle", widgetId, context)));
            appendGradientCommon(members, 3, sweep, path, widgetId, context);
        }
        return renderNamedCompositeMembers(
                dartClass, Optional.empty(), members, valueIndent,
                path, widgetId, context);
    }

    private void appendGradientCommon(
            List<CompositeMember> members,
            int firstOrder,
            PropertyValue.BoxDecorationValue.BoxGradient gradient,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol colorType = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "Color");
        ArrayList<RenderedValue> colors = new ArrayList<>();
        for (int index = 0; index < gradient.stops().size(); index++) {
            colors.add(renderColorSource(
                    gradient.stops().get(index).color(),
                    path + "/stops/" + index + "/color", widgetId, context));
        }
        members.add(new CompositeMember(
                "colors", firstOrder,
                renderTypedList(colorType, colors, "gradient-color",
                        path + "/colors", widgetId, context)));
        String stops = gradient.stops().stream()
                .map(PropertyValue.BoxDecorationValue.GradientStop::stop)
                .map(DartRegionGenerator::dartDouble)
                .collect(Collectors.joining(", "));
        members.add(new CompositeMember(
                "stops", firstOrder + 1,
                scalar("const <double>[" + stops + "]", true,
                        path + "/stops", widgetId, context)));
        members.add(new CompositeMember(
                "tileMode", firstOrder + 2,
                renderEnumSymbol(
                        "TileMode", gradient.tileMode().wireName(),
                        "gradient-tile-mode", path + "/tileMode",
                        widgetId, context)));
        gradient.rotationRadians().ifPresent(rotation -> members.add(
                new CompositeMember(
                        "transform", firstOrder + 3,
                        renderPositionalCompositeValues(
                                "GradientRotation", Optional.empty(),
                                List.of(scalar(
                                        dartDouble(rotation), true,
                                        path + "/rotationRadians", widgetId, context)),
                                path + "/transform", widgetId, context))));
    }

    private RenderedValue renderEnumSymbol(
            String dartType,
            String value,
            String occurrenceKind,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartType);
        String rendered = symbol.text() + '.' + value;
        return scalar(rendered, true, path, widgetId, context,
                List.of(occurrence(
                        "widget:" + widgetId + ':' + occurrenceKind + ':' + path,
                        symbol.nameOffset(), symbol.name(), symbol.libraryUri(),
                        path, Optional.of(widgetId))));
    }

    private RenderedValue renderPositionalCompositeValues(
            String dartClass,
            Optional<String> namedConstructor,
            List<RenderedValue> arguments,
            String path,
            StableId widgetId,
            GenerationContext context) {
        boolean constant = arguments.stream().allMatch(RenderedValue::constant);
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartClass);
        String constructor = (constant ? "const " : "") + symbol.text()
                + namedConstructor.map(value -> "." + value).orElse("");
        StringBuilder rendered = new StringBuilder(constructor).append('(');
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        occurrences.add(occurrence(
                "widget:" + widgetId + ":compound:" + dartClass + ':' + path,
                (constant ? "const ".length() : 0) + symbol.nameOffset(),
                symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId)));
        for (int index = 0; index < arguments.size(); index++) {
            if (index > 0) {
                rendered.append(", ");
            }
            appendRendered(rendered, occurrences, arguments.get(index));
        }
        rendered.append(')');
        return scalar(rendered.toString(), constant, path, widgetId, context,
                occurrences);
    }

    private RenderedValue renderPaint(
            PropertyValue.PaintValue paint,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol paintType = context.planner().renderedSymbol(WIDGETS_IMPORT, "Paint");
        RenderedValue color = renderColorSource(
                paint.color(), path + "/color", widgetId, context);
        StringBuilder rendered = new StringBuilder("(");
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        int paintOffset = rendered.length();
        rendered.append(paintType.text()).append("()")
                .append("..color = ");
        occurrences.add(occurrence(
                "widget:" + widgetId + ":paint:" + path,
                paintOffset + paintType.nameOffset(),
                paintType.name(),
                paintType.libraryUri(),
                path,
                Optional.of(widgetId)));
        appendRendered(rendered, occurrences, color);
        appendEnumCascade(rendered, occurrences, "blendMode", "BlendMode",
                paint.blendMode().wireName(), path, widgetId, context);
        appendEnumCascade(rendered, occurrences, "style", "PaintingStyle",
                paint.style().wireName(), path, widgetId, context);
        rendered.append("..strokeWidth = ").append(dartDouble(paint.strokeWidth()));
        appendEnumCascade(rendered, occurrences, "strokeCap", "StrokeCap",
                paint.strokeCap().wireName(), path, widgetId, context);
        appendEnumCascade(rendered, occurrences, "strokeJoin", "StrokeJoin",
                paint.strokeJoin().wireName(), path, widgetId, context);
        rendered.append("..strokeMiterLimit = ")
                .append(dartDouble(paint.strokeMiterLimit()))
                .append("..isAntiAlias = ").append(paint.antiAlias());
        appendEnumCascade(rendered, occurrences, "filterQuality", "FilterQuality",
                paint.filterQuality().wireName(), path, widgetId, context);
        rendered.append("..invertColors = ").append(paint.invertColors());
        if (paint.maskFilter().isPresent()) {
            PropertyValue.PaintValue.BlurMask mask = paint.maskFilter().orElseThrow();
            RenderedSymbol maskFilter = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "MaskFilter");
            RenderedSymbol blurStyle = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "BlurStyle");
            rendered.append("..maskFilter = const ");
            int maskOffset = rendered.length();
            rendered.append(maskFilter.text()).append(".blur(");
            occurrences.add(occurrence(
                    "widget:" + widgetId + ":paint-mask:" + path,
                    maskOffset + maskFilter.nameOffset(), maskFilter.name(),
                    maskFilter.libraryUri(), path, Optional.of(widgetId)));
            int blurOffset = rendered.length();
            rendered.append(blurStyle.text()).append('.').append(mask.style().wireName())
                    .append(", ").append(dartDouble(mask.sigma())).append(')');
            occurrences.add(occurrence(
                    "widget:" + widgetId + ":paint-blur-style:" + path,
                    blurOffset + blurStyle.nameOffset(), blurStyle.name(),
                    blurStyle.libraryUri(), path, Optional.of(widgetId)));
        }
        rendered.append(')');
        return scalar(rendered.toString(), false, path, widgetId, context, occurrences);
    }

    private void appendEnumCascade(
            StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences,
            String property,
            String dartType,
            String value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, dartType);
        rendered.append("..").append(property).append(" = ");
        int offset = rendered.length();
        rendered.append(symbol.text()).append('.').append(value);
        occurrences.add(occurrence(
                "widget:" + widgetId + ":paint-enum:" + property + ':' + path,
                offset + symbol.nameOffset(), symbol.name(), symbol.libraryUri(),
                path, Optional.of(widgetId)));
    }

    private RenderedValue renderShadows(
            PropertyValue.ShadowListValue shadows,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol shadowType = context.planner().renderedSymbol(WIDGETS_IMPORT, "Shadow");
        RenderedSymbol offsetType = context.planner().renderedSymbol(WIDGETS_IMPORT, "Offset");
        ArrayList<RenderedValue> items = new ArrayList<>();
        for (int index = 0; index < shadows.items().size(); index++) {
            PropertyValue.ShadowListValue.Shadow shadow = shadows.items().get(index);
            String itemPath = path + "/items/" + index;
            RenderedValue color = renderColorSource(
                    shadow.color(), itemPath + "/color", widgetId, context);
            StringBuilder item = new StringBuilder();
            ArrayList<GeneratedDartSymbolOccurrence> itemOccurrences = new ArrayList<>();
            if (color.constant()) {
                item.append("const ");
            }
            int shadowOffset = item.length();
            item.append(shadowType.text()).append("(color: ");
            itemOccurrences.add(occurrence(
                    "widget:" + widgetId + ":shadow:" + shadow.id(),
                    shadowOffset + shadowType.nameOffset(), shadowType.name(),
                    shadowType.libraryUri(), itemPath, Optional.of(widgetId)));
            appendRendered(item, itemOccurrences, color);
            item.append(", offset: const ");
            int offsetOffset = item.length();
            item.append(offsetType.text()).append('(')
                    .append(dartDouble(shadow.offsetX())).append(", ")
                    .append(dartDouble(shadow.offsetY())).append("), blurRadius: ")
                    .append(dartDouble(shadow.blurRadius())).append(')');
            itemOccurrences.add(occurrence(
                    "widget:" + widgetId + ":shadow-offset:" + shadow.id(),
                    offsetOffset + offsetType.nameOffset(), offsetType.name(),
                    offsetType.libraryUri(), itemPath, Optional.of(widgetId)));
            items.add(scalar(item.toString(), color.constant(), itemPath,
                    widgetId, context, itemOccurrences));
        }
        return renderTypedList(
                shadowType, items, "shadows", path, widgetId, context);
    }

    private RenderedValue renderFontFeatures(
            PropertyValue.FontFeatureListValue features,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, "FontFeature");
        ArrayList<RenderedValue> items = new ArrayList<>();
        for (int index = 0; index < features.items().size(); index++) {
            PropertyValue.FontFeatureListValue.FontFeature feature = features.items().get(index);
            String itemPath = path + "/items/" + index;
            String prefix = "const " + type.text() + '(';
            String item = prefix + dartString(
                    feature.tag(), itemPath + "/tag", widgetId,
                    context.maxRenderedUtf8Bytes()) + ", " + feature.value() + ')';
            items.add(scalar(item, true, itemPath, widgetId, context,
                    List.of(occurrence(
                            "widget:" + widgetId + ":font-feature:" + feature.id(),
                            "const ".length() + type.nameOffset(), type.name(),
                            type.libraryUri(), itemPath, Optional.of(widgetId)))));
        }
        return renderTypedList(type, items, "font-features", path, widgetId, context);
    }

    private RenderedValue renderFontVariations(
            PropertyValue.FontVariationListValue variations,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, "FontVariation");
        ArrayList<RenderedValue> items = new ArrayList<>();
        for (int index = 0; index < variations.items().size(); index++) {
            PropertyValue.FontVariationListValue.FontVariation variation =
                    variations.items().get(index);
            String itemPath = path + "/items/" + index;
            String item = "const " + type.text() + '(' + dartString(
                    variation.axis(), itemPath + "/axis", widgetId,
                    context.maxRenderedUtf8Bytes()) + ", "
                    + dartDouble(variation.value()) + ')';
            items.add(scalar(item, true, itemPath, widgetId, context,
                    List.of(occurrence(
                            "widget:" + widgetId + ":font-variation:" + variation.id(),
                            "const ".length() + type.nameOffset(), type.name(),
                            type.libraryUri(), itemPath, Optional.of(widgetId)))));
        }
        return renderTypedList(type, items, "font-variations", path, widgetId, context);
    }

    private RenderedValue renderTypedList(
            RenderedSymbol elementType,
            List<RenderedValue> items,
            String occurrenceKind,
            String path,
            StableId widgetId,
            GenerationContext context) {
        boolean constant = items.stream().allMatch(RenderedValue::constant);
        StringBuilder rendered = new StringBuilder(constant ? "const <" : "<");
        int typeOffset = rendered.length();
        rendered.append(elementType.text()).append(">[");
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        occurrences.add(occurrence(
                "widget:" + widgetId + ':' + occurrenceKind + ":element-type",
                typeOffset + elementType.nameOffset(), elementType.name(),
                elementType.libraryUri(), path, Optional.of(widgetId)));
        for (int index = 0; index < items.size(); index++) {
            if (index > 0) {
                rendered.append(", ");
            }
            appendRendered(rendered, occurrences, items.get(index));
        }
        rendered.append(']');
        return scalar(rendered.toString(), constant, path, widgetId, context, occurrences);
    }

    private RenderedValue renderTextStyleCopyWith(
            RenderedValue base,
            List<CompositeMember> members,
            String path,
            StableId widgetId,
            GenerationContext context) {
        RenderedSymbol textStyle = context.planner().renderedSymbol(WIDGETS_IMPORT, "TextStyle");
        StringBuilder rendered = new StringBuilder("(");
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendRendered(rendered, occurrences, base);
        rendered.append(" ?? const ");
        int typeOffset = rendered.length();
        rendered.append(textStyle.text()).append("()).copyWith(");
        occurrences.add(occurrence(
                "widget:" + widgetId + ":theme-style-fallback",
                typeOffset + textStyle.nameOffset(), textStyle.name(),
                textStyle.libraryUri(), path, Optional.of(widgetId)));
        for (int index = 0; index < members.size(); index++) {
            if (index > 0) {
                rendered.append(", ");
            }
            CompositeMember member = members.get(index);
            rendered.append(member.name()).append(": ");
            appendRendered(rendered, occurrences, member.rendered());
        }
        rendered.append(')');
        return scalar(rendered.toString(), false, path, widgetId, context, occurrences);
    }

    private static void appendRendered(
            StringBuilder destination,
            List<GeneratedDartSymbolOccurrence> occurrences,
            RenderedValue value) {
        int offset = destination.length();
        destination.append(value.joined());
        shiftInto(occurrences, value.symbolOccurrences(), offset);
    }

    private static boolean requiresMaterialTheme(PropertyValue value) {
        return switch (value) {
            case PropertyValue.ThemeTokenValue ignored -> true;
            case PropertyValue.PaintValue paint -> paint.color() instanceof ColorSource.Theme;
            case PropertyValue.ShadowListValue shadows -> shadows.items().stream()
                    .anyMatch(shadow -> shadow.color() instanceof ColorSource.Theme);
            case PropertyValue.BoxDecorationValue decoration ->
                boxDecorationRequiresMaterialTheme(decoration);
            default -> false;
        };
    }

    private static boolean boxDecorationRequiresMaterialTheme(
            PropertyValue.BoxDecorationValue decoration) {
        if (decoration.color().filter(ColorSource.Theme.class::isInstance).isPresent()) {
            return true;
        }
        if (decoration.border().stream()
                .flatMap(DartRegionGenerator::borderSides)
                .map(PropertyValue.BoxDecorationValue.BorderSide::color)
                .anyMatch(ColorSource.Theme.class::isInstance)) {
            return true;
        }
        if (decoration.boxShadow().stream()
                .map(PropertyValue.BoxDecorationValue.BoxShadow::color)
                .anyMatch(ColorSource.Theme.class::isInstance)) {
            return true;
        }
        if (decoration.image().stream()
                .flatMap(image -> image.colorFilter().stream())
                .filter(PropertyValue.DecorationImageValue.Mode.class::isInstance)
                .map(PropertyValue.DecorationImageValue.Mode.class::cast)
                .map(PropertyValue.DecorationImageValue.Mode::color)
                .anyMatch(ColorSource.Theme.class::isInstance)) {
            return true;
        }
        return decoration.gradient().stream()
                .flatMap(gradient -> gradient.stops().stream())
                .map(PropertyValue.BoxDecorationValue.GradientStop::color)
                .anyMatch(ColorSource.Theme.class::isInstance);
    }

    private static java.util.stream.Stream<PropertyValue.BoxDecorationValue.BorderSide>
            borderSides(PropertyValue.BoxDecorationValue.BoxBorder border) {
        if (border instanceof PropertyValue.BoxDecorationValue.PhysicalBorder physical) {
            return java.util.stream.Stream.of(
                    physical.top(), physical.right(), physical.bottom(), physical.left());
        }
        PropertyValue.BoxDecorationValue.DirectionalBorder directional =
                (PropertyValue.BoxDecorationValue.DirectionalBorder) border;
        return java.util.stream.Stream.of(
                directional.top(), directional.start(), directional.end(),
                directional.bottom());
    }

    private RenderedValue renderSlot(
            WidgetSlot slot,
            String path,
            int baseIndent,
            GenerationContext context) {
        if (slot instanceof WidgetSlot.SingleSlot single) {
            if (single.child().isEmpty()) {
                return scalar("null", true, path, Optional.empty(), context);
            }
            return renderNode(single.child().orElseThrow(), path + "/child", baseIndent, context);
        }

        List<WidgetNode> children = ((WidgetSlot.ListSlot) slot).children();
        if (children.isEmpty()) {
            return scalar("[]", true, path, Optional.empty(), context);
        }
        LineAccumulator lines = new LineAccumulator(
                context.maxRenderedUtf8Bytes(), path, null);
        lines.add("[");
        boolean constant = true;
        int childIndent = baseIndent + 2;
        for (int index = 0; index < children.size(); index++) {
            RenderedValue child = renderNode(
                    children.get(index), path + "/children/" + index, childIndent, context);
            constant &= child.constant();
            String emitted = spaces(childIndent) + child.joined() + ',';
            lines.addBlock(
                    emitted,
                    child.symbolOccurrences(),
                    spaces(childIndent).length());
        }
        lines.add(spaces(baseIndent) + ']');
        return lines.build(constant);
    }

    private static boolean isImageSynthesizedProperty(PropertyName name) {
        return switch (name.value()) {
            case "opacity", "centerSliceLeft", "centerSliceTop",
                    "centerSliceRight", "centerSliceBottom" -> true;
            default -> false;
        };
    }

    private void appendImageSynthesizedArguments(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        PropertyDefinition opacityDefinition = definition
                .property(new PropertyName("opacity")).orElseThrow();
        PropertyValue opacityValue = node.properties().get(opacityDefinition.name());
        if (opacityValue instanceof PropertyValue.DoubleValue opacity) {
            String propertyPath = path + "/properties/opacity";
            RenderedSymbol animation = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "AlwaysStoppedAnimation");
            String rendered = "const " + animation.text() + "<double>("
                    + dartDouble(opacity.value()) + ')';
            arguments.add(new ConstructorArgument(
                    opacityDefinition.parameter(),
                    "opacity",
                    false,
                    scalar(
                            rendered,
                            true,
                            propertyPath,
                            node.id(),
                            context,
                            List.of(occurrence(
                                    "widget:" + node.id()
                                    + ":image-opacity-animation",
                                    "const ".length() + animation.nameOffset(),
                                    animation.name(),
                                    animation.libraryUri(),
                                    propertyPath,
                                    Optional.of(node.id()))))));
        }

        PropertyDefinition leftDefinition = definition
                .property(new PropertyName("centerSliceLeft")).orElseThrow();
        PropertyValue leftValue = node.properties().get(leftDefinition.name());
        if (!(leftValue instanceof PropertyValue.DoubleValue left)) {
            return;
        }
        PropertyValue.DoubleValue top = (PropertyValue.DoubleValue) node.properties().get(
                new PropertyName("centerSliceTop"));
        PropertyValue.DoubleValue right = (PropertyValue.DoubleValue) node.properties().get(
                new PropertyName("centerSliceRight"));
        PropertyValue.DoubleValue bottom = (PropertyValue.DoubleValue) node.properties().get(
                new PropertyName("centerSliceBottom"));
        String centerPath = path + "/properties/centerSliceLeft";
        RenderedValue centerSlice = renderPositionalCompositeValues(
                "Rect",
                Optional.of("fromLTRB"),
                List.of(
                        scalar(dartDouble(left.value()), true,
                                centerPath, node.id(), context),
                        scalar(dartDouble(top.value()), true,
                                path + "/properties/centerSliceTop", node.id(), context),
                        scalar(dartDouble(right.value()), true,
                                path + "/properties/centerSliceRight", node.id(), context),
                        scalar(dartDouble(bottom.value()), true,
                                path + "/properties/centerSliceBottom", node.id(), context)),
                centerPath,
                node.id(),
                context);
        arguments.add(new ConstructorArgument(
                leftDefinition.parameter(),
                "centerSlice",
                false,
                centerSlice));
    }

    private void appendTextFieldSynthesizedArguments(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        appendTextFieldKeyboardType(node, definition, path, context, arguments);
        appendTextFieldStaticPreset(
                node, definition, "textAlignVertical", "textAlignVertical",
                MATERIAL_IMPORT, "TextAlignVertical", path, context, arguments);
        appendTextFieldMaxLength(node, definition, path, context, arguments);
        appendTextFieldRadius(node, definition, path, context, arguments);
        appendTextFieldScrollPadding(node, definition, path, context, arguments);
        appendTextFieldStaticPreset(
                node, definition, "mouseCursor", "mouseCursor",
                WIDGETS_IMPORT, "SystemMouseCursors", path, context, arguments);

        if (Boolean.TRUE.equals(textFieldBoolean(node, "expands"))) {
            for (String propertyName : List.of("maxLines", "minLines")) {
                PropertyDefinition property = definition
                        .property(new PropertyName(propertyName)).orElseThrow();
                String propertyPath = path + "/properties/expands";
                arguments.add(new ConstructorArgument(
                        property.parameter(),
                        propertyName,
                        false,
                        scalar("null", true, propertyPath, node.id(), context)));
            }
        }
    }

    private void appendTextFieldKeyboardType(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        PropertyDefinition property = definition
                .property(new PropertyName("keyboardType")).orElseThrow();
        PropertyValue value = node.properties().get(property.name());
        if (value == null) {
            return;
        }
        String propertyPath = path + "/properties/keyboardType";
        if (!(value instanceof PropertyValue.StringValue preset)) {
            throw catalogInconsistency(
                    propertyPath, node.id(),
                    "TextField keyboardType must be a validated string preset.");
        }
        RenderedSymbol type = context.planner().renderedSymbol(
                SERVICES_IMPORT, "TextInputType");
        String rendered;
        int symbolOffset;
        switch (preset.value()) {
            case "numberSigned" -> {
                rendered = "const " + type.text()
                        + ".numberWithOptions(signed: true)";
                symbolOffset = "const ".length() + type.nameOffset();
            }
            case "numberDecimal" -> {
                rendered = "const " + type.text()
                        + ".numberWithOptions(decimal: true)";
                symbolOffset = "const ".length() + type.nameOffset();
            }
            case "numberSignedDecimal" -> {
                rendered = "const " + type.text()
                        + ".numberWithOptions(signed: true, decimal: true)";
                symbolOffset = "const ".length() + type.nameOffset();
            }
            default -> {
                rendered = type.text() + '.' + preset.value();
                symbolOffset = type.nameOffset();
            }
        }
        arguments.add(new ConstructorArgument(
                property.parameter(),
                "keyboardType",
                false,
                scalar(
                        rendered,
                        true,
                        propertyPath,
                        node.id(),
                        context,
                        List.of(occurrence(
                                "widget:" + node.id()
                                + ":textfield-keyboard-type",
                                symbolOffset,
                                type.name(),
                                type.libraryUri(),
                                propertyPath,
                                Optional.of(node.id()))))));
    }

    private void appendTextFieldStaticPreset(
            WidgetNode node,
            WidgetDefinition definition,
            String propertyName,
            String argumentName,
            String libraryUri,
            String dartType,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        PropertyDefinition property = definition
                .property(new PropertyName(propertyName)).orElseThrow();
        PropertyValue value = node.properties().get(property.name());
        if (value == null) {
            return;
        }
        String propertyPath = path + "/properties/" + pointer(propertyName);
        if (!(value instanceof PropertyValue.StringValue preset)) {
            throw catalogInconsistency(
                    propertyPath, node.id(),
                    "TextField static preset '" + propertyName
                    + "' must be a validated string.");
        }
        RenderedSymbol type = context.planner().renderedSymbol(libraryUri, dartType);
        arguments.add(new ConstructorArgument(
                property.parameter(),
                argumentName,
                false,
                scalar(
                        type.text() + '.' + preset.value(),
                        true,
                        propertyPath,
                        node.id(),
                        context,
                        List.of(occurrence(
                                "widget:" + node.id() + ":textfield-preset:"
                                + propertyName,
                                type.nameOffset(),
                                type.name(),
                                type.libraryUri(),
                                propertyPath,
                                Optional.of(node.id()))))));
    }

    private void appendTextFieldMaxLength(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        PropertyDefinition property = definition
                .property(new PropertyName("maxLength")).orElseThrow();
        PropertyValue value = node.properties().get(property.name());
        if (value == null) {
            return;
        }
        String propertyPath = path + "/properties/maxLength";
        if (!(value instanceof PropertyValue.IntegerValue integer)) {
            throw catalogInconsistency(
                    propertyPath, node.id(),
                    "TextField maxLength must be a validated integer.");
        }
        RenderedValue rendered;
        if (integer.value().equals(java.math.BigInteger.valueOf(-1))) {
            RenderedSymbol type = context.planner().renderedSymbol(
                    MATERIAL_IMPORT, "TextField");
            rendered = scalar(
                    type.text() + ".noMaxLength",
                    true,
                    propertyPath,
                    node.id(),
                    context,
                    List.of(occurrence(
                            "widget:" + node.id() + ":textfield-no-max-length",
                            type.nameOffset(),
                            type.name(),
                            type.libraryUri(),
                            propertyPath,
                            Optional.of(node.id()))));
        } else {
            rendered = scalar(
                    integer.value().toString(), true,
                    propertyPath, node.id(), context);
        }
        arguments.add(new ConstructorArgument(
                property.parameter(), "maxLength", false, rendered));
    }

    private void appendTextFieldRadius(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        PropertyDefinition xProperty = definition
                .property(new PropertyName("cursorRadiusX")).orElseThrow();
        PropertyValue xValue = node.properties().get(xProperty.name());
        if (xValue == null) {
            return;
        }
        PropertyValue yValue = node.properties().get(new PropertyName("cursorRadiusY"));
        if (!(xValue instanceof PropertyValue.DoubleValue x)
                || !(yValue instanceof PropertyValue.DoubleValue y)) {
            throw catalogInconsistency(
                    path + "/properties/cursorRadiusX", node.id(),
                    "TextField cursor radius must contain two validated doubles.");
        }
        String propertyPath = path + "/properties/cursorRadiusX";
        RenderedValue radius = renderPositionalCompositeValues(
                "Radius",
                Optional.of("elliptical"),
                List.of(
                        scalar(dartDouble(x.value()), true,
                                propertyPath, node.id(), context),
                        scalar(dartDouble(y.value()), true,
                                path + "/properties/cursorRadiusY", node.id(), context)),
                propertyPath,
                node.id(),
                context);
        arguments.add(new ConstructorArgument(
                xProperty.parameter(), "cursorRadius", false, radius));
    }

    private void appendTextFieldScrollPadding(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        PropertyDefinition leftProperty = definition
                .property(new PropertyName("scrollPaddingLeft")).orElseThrow();
        PropertyValue leftValue = node.properties().get(leftProperty.name());
        if (leftValue == null) {
            return;
        }
        ArrayList<RenderedValue> values = new ArrayList<>();
        for (String name : List.of(
                "scrollPaddingLeft", "scrollPaddingTop",
                "scrollPaddingRight", "scrollPaddingBottom")) {
            PropertyValue value = node.properties().get(new PropertyName(name));
            if (!(value instanceof PropertyValue.DoubleValue decimal)) {
                throw catalogInconsistency(
                        path + "/properties/" + name, node.id(),
                        "TextField scroll padding must contain four validated doubles.");
            }
            values.add(scalar(
                    dartDouble(decimal.value()), true,
                    path + "/properties/" + name, node.id(), context));
        }
        String propertyPath = path + "/properties/scrollPaddingLeft";
        RenderedValue padding = renderPositionalCompositeValues(
                "EdgeInsets",
                Optional.of("fromLTRB"),
                values,
                propertyPath,
                node.id(),
                context);
        arguments.add(new ConstructorArgument(
                leftProperty.parameter(), "scrollPadding", false, padding));
    }

    private static Boolean textFieldBoolean(
            WidgetNode node,
            String propertyName) {
        PropertyValue value = node.properties().get(new PropertyName(propertyName));
        return value instanceof PropertyValue.BooleanValue flag
                ? flag.value() : null;
    }

    private static boolean isStaticScrollView(WidgetNode node) {
        return node.type().equals(ListViewWidgetPropertySchema.LIST_VIEW_TYPE)
                || node.type().equals(GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE);
    }

    private void appendStaticScrollViewSynthesizedArguments(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        appendStaticScrollViewPhysics(node, definition, path, context, arguments);
        appendStaticScrollViewCacheExtent(
                node, definition, path, context, arguments);
    }

    private void appendStaticScrollViewPhysics(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        PropertyDefinition property = definition
                .property(new PropertyName("physics")).orElseThrow();
        PropertyValue value = node.properties().get(property.name());
        if (value == null) {
            return;
        }
        String propertyPath = path + "/properties/physics";
        if (!(value instanceof PropertyValue.StringValue preset)) {
            throw catalogInconsistency(
                    propertyPath, node.id(),
                    definition.dartClassName()
                    + " physics must be a validated string preset.");
        }
        String dartClass = switch (preset.value()) {
            case "alwaysScrollable" -> "AlwaysScrollableScrollPhysics";
            case "bouncing" -> "BouncingScrollPhysics";
            case "clamping" -> "ClampingScrollPhysics";
            case "neverScrollable" -> "NeverScrollableScrollPhysics";
            case "page" -> "PageScrollPhysics";
            case "rangeMaintaining" -> "RangeMaintainingScrollPhysics";
            default -> throw catalogInconsistency(
                    propertyPath, node.id(),
                    "Unsupported validated " + definition.dartClassName()
                    + " physics preset '"
                    + preset.value() + "'.");
        };
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartClass);
        String rendered = "const " + symbol.text() + "()";
        arguments.add(new ConstructorArgument(
                property.parameter(),
                "physics",
                false,
                scalar(
                        rendered,
                        true,
                        propertyPath,
                        node.id(),
                        context,
                        List.of(occurrence(
                                "widget:" + node.id() + ":list-view-physics",
                                "const ".length() + symbol.nameOffset(),
                                symbol.name(),
                                symbol.libraryUri(),
                                propertyPath,
                                Optional.of(node.id()))))));
    }

    private void appendStaticScrollViewCacheExtent(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        PropertyDefinition property = definition
                .property(new PropertyName("scrollCacheExtent")).orElseThrow();
        PropertyValue value = node.properties().get(property.name());
        if (value == null) {
            return;
        }
        String propertyPath = path + "/properties/scrollCacheExtent";
        if (!(value instanceof PropertyValue.IntegerValue)
                && !(value instanceof PropertyValue.DoubleValue)) {
            throw catalogInconsistency(
                    propertyPath, node.id(),
                    definition.dartClassName()
                    + " scrollCacheExtent must be a validated numeric value.");
        }
        RenderedValue amount = renderProperty(
                value, property, propertyPath, node.id(), context);
        RenderedSymbol symbol = context.planner().renderedSymbol(
                RENDERING_IMPORT, "ScrollCacheExtent");
        String prefix = "const " + symbol.text() + ".pixels(";
        String rendered = prefix + amount.joined() + ')';
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        occurrences.add(occurrence(
                "widget:" + node.id() + ":list-view-scroll-cache-extent",
                "const ".length() + symbol.nameOffset(),
                symbol.name(),
                symbol.libraryUri(),
                propertyPath,
                Optional.of(node.id())));
        shiftInto(occurrences, amount.symbolOccurrences(), prefix.length());
        arguments.add(new ConstructorArgument(
                property.parameter(),
                "scrollCacheExtent",
                false,
                scalar(
                        rendered, true, propertyPath, node.id(), context,
                        occurrences)));
    }

    private RenderedValue renderProperty(
            PropertyValue value,
            PropertyDefinition definition,
            String path,
            StableId widgetId,
            GenerationContext context) {
        return renderProperty(value, definition, path, widgetId, context, 0);
    }

    private RenderedValue renderProperty(
            PropertyValue value,
            PropertyDefinition definition,
            String path,
            StableId widgetId,
            GenerationContext context,
            int valueIndent) {
        if (value instanceof PropertyValue.DartExpressionValue) {
            throw abort(diagnostic(
                    DartGenerationDiagnosticCode.DART_EXPRESSION_UNSUPPORTED,
                    path,
                    Optional.of(widgetId),
                    Optional.of(DartManagedRegionId.BUILD),
                    "Property '" + definition.name().value()
                    + "' contains an opaque Dart expression; generation profile "
                    + PROFILE_ID + " rejects expressions instead of parsing or rewriting them."));
        }
        if (value instanceof PropertyValue.IconDataValue iconData) {
            return renderIconData(iconData, path, widgetId, context);
        }
        if (value instanceof PropertyValue.StringValue string) {
            return scalar(
                    dartString(string.value(), path, widgetId, context.maxRenderedUtf8Bytes()), true,
                    context.maxRenderedUtf8Bytes(), path, Optional.of(widgetId));
        }
        if (value instanceof PropertyValue.BooleanValue bool) {
            return scalar(Boolean.toString(bool.value()), true, path, widgetId, context);
        }
        if (value instanceof PropertyValue.IntegerValue integer) {
            return scalar(integer.value().toString(), true, path, widgetId, context);
        }
        if (value instanceof PropertyValue.DoubleValue decimal) {
            return scalar(dartDouble(decimal.value()), true, path, widgetId, context);
        }
        if (value instanceof PropertyValue.AlignmentGeometryValue alignment) {
            return renderAlignmentGeometry(
                    alignment, path, widgetId, context);
        }
        if (value instanceof PropertyValue.OffsetValue offset) {
            return renderOffset(offset, path, widgetId, context);
        }
        if (value instanceof PropertyValue.SizeValue size) {
            return renderSize(size, path, widgetId, context);
        }
        if (value instanceof PropertyValue.BoxConstraintsValue constraints) {
            return renderBoxConstraints(
                    constraints, valueIndent, path, widgetId, context);
        }
        if (value instanceof PropertyValue.Matrix4Value matrix) {
            return renderMatrix4(matrix, path, widgetId, context);
        }
        if (value instanceof PropertyValue.ImageProviderValue imageProvider) {
            return renderImageProvider(imageProvider, path, widgetId, context);
        }
        if (value instanceof PropertyValue.BoxDecorationValue decoration) {
            return renderBoxDecoration(
                    decoration, valueIndent, path, widgetId, context);
        }
        if (value instanceof PropertyValue.EnumValue enumValue) {
            PropertyValueConstraint.EnumValues binding = definition.constraints().stream()
                    .filter(PropertyValueConstraint.EnumValues.class::isInstance)
                    .map(PropertyValueConstraint.EnumValues.class::cast)
                    .findFirst()
                    .orElseThrow(() -> abort(diagnostic(
                    DartGenerationDiagnosticCode.INTERNAL_CATALOG_INCONSISTENCY,
                    path,
                    Optional.of(widgetId),
                    Optional.of(DartManagedRegionId.BUILD),
                    "Validated enum property '" + definition.name().value()
                    + "' has no Dart enum symbol binding.")));
            RenderedSymbol symbol = context.planner().renderedSymbol(binding.dartType());
            return scalar(
                    symbol.text() + "." + enumValue.value(),
                    true,
                    path,
                    widgetId,
                    context,
                    List.of(occurrence(
                            "widget:" + widgetId + ":property:"
                                    + definition.name().value() + ":enum-type",
                            symbol.nameOffset(),
                            symbol.name(),
                            symbol.libraryUri(),
                            path,
                            Optional.of(widgetId))));
        }
        if (value instanceof PropertyValue.ColorValue color) {
            RenderedSymbol symbol = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "Color");
            return scalar(
                    "const " + symbol.text() + "(" + color.wireArgb() + ")",
                    true,
                    path,
                    widgetId,
                    context,
                    List.of(occurrence(
                            "widget:" + widgetId + ":property:"
                                    + definition.name().value() + ":color-type",
                            "const ".length() + symbol.nameOffset(),
                            symbol.name(),
                            symbol.libraryUri(),
                            path,
                            Optional.of(widgetId))));
        }
        if (value instanceof PropertyValue.EdgeInsetsValue insets) {
            RenderedSymbol symbol = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "EdgeInsets");
            return scalar(
                    "const " + symbol.text() + ".fromLTRB("
                    + dartDouble(insets.left()) + ", "
                    + dartDouble(insets.top()) + ", "
                    + dartDouble(insets.right()) + ", "
                    + dartDouble(insets.bottom()) + ")",
                    true,
                    path,
                    widgetId,
                    context,
                    List.of(occurrence(
                            "widget:" + widgetId + ":property:"
                                    + definition.name().value() + ":edge-insets-type",
                            "const ".length() + symbol.nameOffset(),
                            symbol.name(),
                            symbol.libraryUri(),
                            path,
                            Optional.of(widgetId))));
        }
        if (value instanceof PropertyValue.EdgeInsetsDirectionalValue insets) {
            RenderedSymbol symbol = context.planner().renderedSymbol(
                    WIDGETS_IMPORT, "EdgeInsetsDirectional");
            return scalar(
                    "const " + symbol.text() + ".fromSTEB("
                    + dartDouble(insets.start()) + ", "
                    + dartDouble(insets.top()) + ", "
                    + dartDouble(insets.end()) + ", "
                    + dartDouble(insets.bottom()) + ")",
                    true,
                    path,
                    widgetId,
                    context,
                    List.of(occurrence(
                            "widget:" + widgetId + ":property:"
                                    + definition.name().value()
                                    + ":edge-insets-directional-type",
                            "const ".length() + symbol.nameOffset(),
                            symbol.name(),
                            symbol.libraryUri(),
                            path,
                            Optional.of(widgetId))));
        }
        if (value instanceof PropertyValue.ThemeTokenValue token) {
            return renderThemeToken(token.token(), path, widgetId, context);
        }
        if (value instanceof PropertyValue.PaintValue paint) {
            return renderPaint(paint, path, widgetId, context);
        }
        if (value instanceof PropertyValue.ShadowListValue shadows) {
            return renderShadows(shadows, path, widgetId, context);
        }
        if (value instanceof PropertyValue.FontFeatureListValue features) {
            return renderFontFeatures(features, path, widgetId, context);
        }
        if (value instanceof PropertyValue.FontVariationListValue variations) {
            return renderFontVariations(variations, path, widgetId, context);
        }
        if (value instanceof PropertyValue.AssetValue asset) {
            return scalar(
                    dartString(asset.path(), path, widgetId, context.maxRenderedUtf8Bytes()),
                    true, path, widgetId, context);
        }
        if (value instanceof PropertyValue.CallbackValue callback) {
            return scalar(callback.handler(), false, path, widgetId, context);
        }
        throw abort(diagnostic(
                DartGenerationDiagnosticCode.INTERNAL_CATALOG_INCONSISTENCY,
                path,
                Optional.of(widgetId),
                Optional.of(DartManagedRegionId.BUILD),
                "Generation profile " + PROFILE_ID + " does not recognize property value kind "
                + value.kind().wireName() + "."));
    }

    private RenderedValue renderIconData(
            PropertyValue.IconDataValue value,
            String path,
            StableId widgetId,
            GenerationContext context) {
        if (value.codePoint().isEmpty()) {
            return scalar("null", true, path, widgetId, context);
        }
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, "IconData");
        StringBuilder rendered = new StringBuilder("const ");
        int symbolOffset = rendered.length();
        rendered.append(symbol.text())
                .append("(0x")
                .append(Integer.toHexString(value.codePoint().orElseThrow())
                        .toUpperCase(java.util.Locale.ROOT));
        value.fontFamily().ifPresent(family -> rendered
                .append(", fontFamily: ")
                .append(dartString(
                        family, path, widgetId, context.maxRenderedUtf8Bytes())));
        value.fontPackage().ifPresent(fontPackage -> rendered
                .append(", fontPackage: ")
                .append(dartString(
                        fontPackage, path, widgetId,
                        context.maxRenderedUtf8Bytes())));
        if (value.matchTextDirection()) {
            rendered.append(", matchTextDirection: true");
        }
        if (!value.fontFamilyFallback().isEmpty()) {
            rendered.append(", fontFamilyFallback: <String>[");
            for (int index = 0; index < value.fontFamilyFallback().size(); index++) {
                if (index > 0) {
                    rendered.append(", ");
                }
                rendered.append(dartString(
                        value.fontFamilyFallback().get(index),
                        path, widgetId, context.maxRenderedUtf8Bytes()));
            }
            rendered.append(']');
        }
        rendered.append(')');
        return scalar(
                rendered.toString(),
                true,
                path,
                widgetId,
                context,
                List.of(occurrence(
                        "widget:" + widgetId + ":property:icon:icon-data-type",
                        symbolOffset + symbol.nameOffset(),
                        symbol.name(),
                        symbol.libraryUri(),
                        path,
                        Optional.of(widgetId))));
    }

    private String dartString(
            String value,
            String path,
            StableId widgetId,
            int maximumUtf8Bytes) {
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints > limits.maxValueCodePoints()) {
            throw abort(diagnostic(
                    DartGenerationDiagnosticCode.OUTPUT_SIZE_LIMIT,
                    path,
                    Optional.of(widgetId),
                    Optional.of(DartManagedRegionId.BUILD),
                    "Dart string value contains " + codePoints
                    + " code points, exceeding maxValueCodePoints="
                    + limits.maxValueCodePoints() + "."));
        }

        long escapedBytes = 2;
        for (int index = 0; index < value.length();) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw invalidUnicode(path, widgetId);
                }
            } else if (Character.isLowSurrogate(current)) {
                throw invalidUnicode(path, widgetId);
            }
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);
            escapedBytes += escapedUtf8Size(codePoint);
            if (escapedBytes > maximumUtf8Bytes) {
                throw outputLimit(
                        path,
                        Optional.of(widgetId),
                        "Escaped Dart string exceeds the remaining generated build budget of "
                        + maximumUtf8Bytes + " UTF-8 bytes.");
            }
        }

        StringBuilder escaped = new StringBuilder(
                (int) Math.min((long) value.length() + 2L, maximumUtf8Bytes)).append('\'');
        for (int index = 0; index < value.length();) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw invalidUnicode(path, widgetId);
                }
            } else if (Character.isLowSurrogate(current)) {
                throw invalidUnicode(path, widgetId);
            }
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);
            switch (codePoint) {
                case '\\' -> escaped.append("\\\\");
                case '\'' -> escaped.append("\\'");
                case '$' -> escaped.append("\\$");
                case '\b' -> escaped.append("\\b");
                case '\t' -> escaped.append("\\t");
                case '\n' -> escaped.append("\\n");
                case '\f' -> escaped.append("\\f");
                case '\r' -> escaped.append("\\r");
                default -> {
                    if (codePoint < 0x20 || codePoint == 0x7F
                            || codePoint == 0x2028 || codePoint == 0x2029) {
                        escaped.append("\\u{")
                                .append(Integer.toHexString(codePoint).toUpperCase())
                                .append('}');
                    } else {
                        escaped.appendCodePoint(codePoint);
                    }
                }
            }
        }
        return escaped.append('\'').toString();
    }

    private static int escapedUtf8Size(int codePoint) {
        return switch (codePoint) {
            case '\\', '\'', '$', '\b', '\t', '\n', '\f', '\r' -> 2;
            default -> {
                if (codePoint < 0x20 || codePoint == 0x7F
                        || codePoint == 0x2028 || codePoint == 0x2029) {
                    yield 4 + Integer.toHexString(codePoint).length();
                }
                yield utf8Size(codePoint);
            }
        };
    }

    private static GenerationAbort invalidUnicode(String path, StableId widgetId) {
        return abort(diagnostic(
                DartGenerationDiagnosticCode.INVALID_UNICODE,
                path,
                Optional.of(widgetId),
                Optional.of(DartManagedRegionId.BUILD),
                "Dart string value contains an unpaired UTF-16 surrogate."));
    }

    private static String dartDouble(BigDecimal value) {
        String token = Double.toString(value.doubleValue()).replace('E', 'e');
        return token.replace("e+", "e");
    }

    private static String renderImports(DartImportPlan plan, int maximumUtf8Bytes) {
        StringBuilder output = new StringBuilder();
        long bytes = 0;
        for (DartImportDirective directive : plan.directives()) {
            String line = "import '" + directive.uri() + '\''
                    + directive.prefix().map(prefix -> " as " + prefix).orElse("")
                    + ";\n";
            bytes += utf8Length(line);
            if (bytes > maximumUtf8Bytes) {
                throw outputLimit(
                        "/source/managedRegions/imports",
                        Optional.empty(),
                        "Generated imports exceed maxTotalPayloadUtf8Bytes="
                        + maximumUtf8Bytes + ".");
            }
            output.append(line);
        }
        return output.toString();
    }

    private static RenderedValue scalar(
            String value,
            boolean constant,
            String path,
            StableId widgetId,
            GenerationContext context) {
        return scalar(
                value,
                constant,
                context.maxRenderedUtf8Bytes(),
                path,
                Optional.of(widgetId),
                List.of());
    }

    private static RenderedValue scalar(
            String value,
            boolean constant,
            String path,
            StableId widgetId,
            GenerationContext context,
            List<GeneratedDartSymbolOccurrence> symbolOccurrences) {
        return scalar(
                value,
                constant,
                context.maxRenderedUtf8Bytes(),
                path,
                Optional.of(widgetId),
                symbolOccurrences);
    }

    private static RenderedValue scalar(
            String value,
            boolean constant,
            String path,
            Optional<StableId> widgetId,
            GenerationContext context) {
        return scalar(value, constant, context.maxRenderedUtf8Bytes(), path, widgetId);
    }

    private static RenderedValue scalar(
            String value,
            boolean constant,
            int maximumUtf8Bytes,
            String path,
            Optional<StableId> widgetId) {
        return scalar(
                value,
                constant,
                maximumUtf8Bytes,
                path,
                widgetId,
                List.of());
    }

    private static RenderedValue scalar(
            String value,
            boolean constant,
            int maximumUtf8Bytes,
            String path,
            Optional<StableId> widgetId,
            List<GeneratedDartSymbolOccurrence> symbolOccurrences) {
        long bytes = utf8Length(value);
        if (bytes > maximumUtf8Bytes) {
            throw outputLimit(
                    path,
                    widgetId,
                    "Generated Dart value requires " + bytes
                    + " UTF-8 bytes, exceeding the remaining build budget of "
                    + maximumUtf8Bytes + ".");
        }
        List<GeneratedDartSymbolOccurrence> copied = List.copyOf(
                Objects.requireNonNull(symbolOccurrences, "symbolOccurrences"));
        for (GeneratedDartSymbolOccurrence occurrence : copied) {
            if (occurrence.endOffset() > value.length()
                    || !value.substring(occurrence.offset(), occurrence.endOffset())
                            .equals(occurrence.symbolName())) {
                throw new IllegalArgumentException(
                        "Rendered symbol occurrence does not identify scalar text: "
                        + occurrence.id());
            }
        }
        return new RenderedValue(
                List.of(value),
                constant,
                Math.toIntExact(bytes),
                copied);
    }

    private static GeneratedDartSymbolOccurrence occurrence(
            String id,
            int offset,
            String symbolName,
            String libraryUri,
            String modelPath,
            Optional<StableId> widgetId) {
        return new GeneratedDartSymbolOccurrence(
                id,
                DartManagedRegionId.BUILD,
                offset,
                symbolName.length(),
                symbolName,
                libraryUri,
                modelPath,
                widgetId);
    }

    private static void shiftInto(
            List<GeneratedDartSymbolOccurrence> destination,
            List<GeneratedDartSymbolOccurrence> source,
            int delta) {
        for (GeneratedDartSymbolOccurrence occurrence : source) {
            destination.add(occurrence.shifted(delta));
        }
    }

    private static GenerationAbort outputLimit(
            String path,
            Optional<StableId> widgetId,
            String message) {
        return abort(diagnostic(
                DartGenerationDiagnosticCode.OUTPUT_SIZE_LIMIT,
                path,
                widgetId,
                Optional.empty(),
                message));
    }

    private static long utf8Length(String value) {
        long bytes = 0;
        for (int index = 0; index < value.length();) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw new IllegalArgumentException("unpaired high surrogate");
                }
            } else if (Character.isLowSurrogate(current)) {
                throw new IllegalArgumentException("unpaired low surrogate");
            }
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);
            bytes += utf8Size(codePoint);
        }
        return bytes;
    }

    private static int utf8Size(int codePoint) {
        if (codePoint <= 0x7F) {
            return 1;
        }
        if (codePoint <= 0x7FF) {
            return 2;
        }
        return codePoint <= 0xFFFF ? 3 : 4;
    }

    private static DartGenerationResult failure(
            ValidationResult validation,
            DartGenerationDiagnostic diagnostic) {
        return new DartGenerationResult(validation, Optional.empty(), List.of(diagnostic));
    }

    private static DartGenerationDiagnostic diagnostic(
            DartGenerationDiagnosticCode code,
            String path,
            Optional<StableId> widgetId,
            Optional<DartManagedRegionId> region,
            String message) {
        return new DartGenerationDiagnostic(code, path, widgetId, region, message);
    }

    private static GenerationAbort abort(DartGenerationDiagnostic diagnostic) {
        return new GenerationAbort(diagnostic);
    }

    private static String pointer(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    private static String spaces(int count) {
        return " ".repeat(count);
    }

    private record GenerationContext(
            WidgetCatalog catalog,
            DartImportPlan importPlan,
            ImportPlanner planner,
            int maxRenderedUtf8Bytes) {
    }

    private record WidgetAtPath(WidgetNode node, String path) {
    }

    private record ConstructorArgument(
            DartParameter parameter,
            String name,
            boolean slot,
            RenderedValue value) {
    }

    private record TextMember(
            PropertyDefinition property,
            TextWidgetPropertySchema.Definition binding,
            PropertyValue value,
            RenderedValue rendered) {

        String propertyPath(String widgetPath) {
            return widgetPath + "/properties/" + pointer(property.name().value());
        }
    }

    private record AppBarMember(
            PropertyDefinition property,
            AppBarWidgetPropertySchema.Definition binding,
            PropertyValue value,
            RenderedValue rendered) {

        String propertyPath(String widgetPath) {
            return widgetPath + "/properties/" + pointer(property.name().value());
        }
    }

    private record ElevatedButtonState(String prefix, String key) {
    }

    private record ElevatedButtonStateEntry(
            String state,
            RenderedValue value) {
    }

    private record ElevatedButtonBoundSizePair(
            RenderedValue minimum,
            RenderedValue maximum) {
    }

    private record ElevatedButtonLayer(
            String prefix,
            Map<String, RenderedValue> values) {

        private ElevatedButtonLayer {
            prefix = Objects.requireNonNull(prefix, "prefix");
            values = Map.copyOf(Objects.requireNonNull(values, "values"));
        }

        boolean configured() {
            return !values.isEmpty();
        }
    }

    private record ElevatedButtonTextLayer(
            String prefix,
            RenderedValue theme,
            List<ElevatedButtonTextMember> styleMembers,
            List<ElevatedButtonTextMember> localeMembers,
            List<ElevatedButtonTextMember> decorationMembers) {

        private ElevatedButtonTextLayer {
            prefix = Objects.requireNonNull(prefix, "prefix");
            styleMembers = List.copyOf(Objects.requireNonNull(
                    styleMembers, "styleMembers"));
            localeMembers = List.copyOf(Objects.requireNonNull(
                    localeMembers, "localeMembers"));
            decorationMembers = List.copyOf(Objects.requireNonNull(
                    decorationMembers, "decorationMembers"));
        }

        boolean configured() {
            return theme != null
                    || !styleMembers.isEmpty()
                    || !localeMembers.isEmpty()
                    || !decorationMembers.isEmpty();
        }
    }

    private record ElevatedButtonTextMember(
            PropertyDefinition property,
            ElevatedButtonWidgetPropertySchema.Definition binding,
            PropertyValue value,
            RenderedValue rendered) {
    }

    private record CompositeMember(
            String name,
            int order,
            RenderedValue rendered) {
    }

    private record RenderedValue(
            List<String> lines,
            boolean constant,
            int utf8Size,
            List<GeneratedDartSymbolOccurrence> symbolOccurrences) {
        private RenderedValue {
            lines = List.copyOf(lines);
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Rendered value must contain at least one line");
            }
            symbolOccurrences = List.copyOf(Objects.requireNonNull(
                    symbolOccurrences, "symbolOccurrences"));
        }

        String joined() {
            return String.join("\n", lines);
        }
    }

    private static final class LineAccumulator {
        private final int maximumUtf8Bytes;
        private final String path;
        private final Optional<StableId> widgetId;
        private final ArrayList<String> lines = new ArrayList<>();
        private final ArrayList<GeneratedDartSymbolOccurrence> symbolOccurrences =
                new ArrayList<>();
        private long bytes;
        private int utf16Length;

        private LineAccumulator(int maximumUtf8Bytes, String path, StableId widgetId) {
            this.maximumUtf8Bytes = maximumUtf8Bytes;
            this.path = path;
            this.widgetId = Optional.ofNullable(widgetId);
        }

        void add(String line) {
            add(line, List.of());
        }

        void add(
                String line,
                List<GeneratedDartSymbolOccurrence> lineOccurrences) {
            long addition = utf8Length(line) + (lines.isEmpty() ? 0 : 1);
            if (bytes + addition > maximumUtf8Bytes) {
                throw outputLimit(
                        path,
                        widgetId,
                        "Generated Dart subtree exceeds the remaining build budget of "
                        + maximumUtf8Bytes + " UTF-8 bytes.");
            }
            int start = Math.addExact(utf16Length, lines.isEmpty() ? 0 : 1);
            for (GeneratedDartSymbolOccurrence occurrence : lineOccurrences) {
                if (occurrence.endOffset() > line.length()) {
                    throw new IllegalArgumentException(
                            "Line symbol occurrence is outside rendered text: "
                            + occurrence.id());
                }
                symbolOccurrences.add(occurrence.shifted(start));
            }
            lines.add(line);
            bytes += addition;
            utf16Length = Math.addExact(start, line.length());
        }

        void addAll(List<String> added) {
            for (String line : added) {
                add(line);
            }
        }

        void addBlock(
                String block,
                List<GeneratedDartSymbolOccurrence> blockOccurrences,
                int occurrencePrefixLength) {
            if (occurrencePrefixLength < 0 || occurrencePrefixLength > block.length()) {
                throw new IllegalArgumentException("Invalid rendered block occurrence prefix");
            }
            int blockStart = Math.addExact(utf16Length, lines.isEmpty() ? 0 : 1);
            for (GeneratedDartSymbolOccurrence occurrence : blockOccurrences) {
                GeneratedDartSymbolOccurrence shifted = occurrence.shifted(
                        occurrencePrefixLength);
                if (shifted.endOffset() > block.length()
                        || !block.substring(shifted.offset(), shifted.endOffset())
                                .equals(shifted.symbolName())) {
                    throw new IllegalArgumentException(
                            "Block symbol occurrence does not identify rendered text: "
                            + occurrence.id());
                }
                symbolOccurrences.add(shifted.shifted(blockStart));
            }
            addAll(List.of(block.split("\\n", -1)));
        }

        RenderedValue build(boolean constant) {
            return new RenderedValue(
                    lines,
                    constant,
                    Math.toIntExact(bytes),
                    symbolOccurrences);
        }
    }

    private static final class GenerationAbort extends RuntimeException {
        private final DartGenerationDiagnostic diagnostic;

        private GenerationAbort(DartGenerationDiagnostic diagnostic) {
            super(null, null, false, false);
            this.diagnostic = diagnostic;
        }

        DartGenerationDiagnostic diagnostic() {
            return diagnostic;
        }
    }

    private static final class ImportPlanner {
        private final String umbrellaUri;
        private final DartImportPlan plan;
        private final Map<String, Optional<String>> prefixesByUri;

        private ImportPlanner(
                String umbrellaUri,
                DartImportPlan plan,
                Map<String, Optional<String>> prefixesByUri) {
            this.umbrellaUri = umbrellaUri;
            this.plan = plan;
            this.prefixesByUri = Map.copyOf(prefixesByUri);
        }

        static ImportPlanner create(
                Iterable<WidgetDefinition> definitions,
                int maximumImports,
                boolean requiresMaterialTheme,
                boolean requiresServices,
                boolean requiresGestures,
                boolean requiresRendering,
                boolean requiresDartConvert,
                boolean requiresDartUi) {
            TreeSet<String> uris = new TreeSet<>();
            uris.add(requiresMaterialTheme ? MATERIAL_IMPORT : WIDGETS_IMPORT);
            if (requiresDartConvert) {
                uris.add(DART_CONVERT_IMPORT);
            }
            if (requiresServices) {
                uris.add(SERVICES_IMPORT);
            }
            for (WidgetDefinition definition : definitions) {
                for (String uri : definition.importUris()) {
                    if (uri.equals(GESTURES_IMPORT) && !requiresGestures) {
                        continue;
                    }
                    if (uri.equals(SERVICES_IMPORT) && !requiresServices) {
                        continue;
                    }
                    if (uri.equals(RENDERING_IMPORT) && !requiresRendering) {
                        continue;
                    }
                    if (uri.equals(DART_UI_IMPORT) && !requiresDartUi) {
                        continue;
                    }
                    if (uri.equals(MATERIAL_IMPORT)) {
                        uris.remove(WIDGETS_IMPORT);
                        uris.add(MATERIAL_IMPORT);
                    } else if (!uri.equals(WIDGETS_IMPORT)
                            || !uris.contains(MATERIAL_IMPORT)) {
                        uris.add(uri);
                    }
                    if (uris.size() > maximumImports) {
                        throw abort(diagnostic(
                                DartGenerationDiagnosticCode.IMPORT_LIMIT,
                                "/root",
                                Optional.empty(),
                                Optional.of(DartManagedRegionId.IMPORTS),
                                "Generated Dart requires more than maxImports="
                                + maximumImports + " unique import URIs."));
                    }
                }
            }
            String umbrella = uris.contains(MATERIAL_IMPORT) ? MATERIAL_IMPORT : WIDGETS_IMPORT;
            if (uris.size() > maximumImports) {
                throw abort(diagnostic(
                        DartGenerationDiagnosticCode.IMPORT_LIMIT,
                        "/root",
                        Optional.empty(),
                        Optional.of(DartManagedRegionId.IMPORTS),
                        "Generated Dart requires " + uris.size()
                        + " imports, exceeding maxImports=" + maximumImports + "."));
            }

            HashSet<String> aliases = new HashSet<>();
            HashMap<String, Optional<String>> prefixes = new HashMap<>();
            ArrayList<DartImportDirective> directives = new ArrayList<>();
            for (String uri : uris) {
                Optional<String> prefix = uri.equals(umbrella)
                        ? Optional.empty()
                        : Optional.of(uniquePrefix(uri, aliases));
                prefixes.put(uri, prefix);
                directives.add(new DartImportDirective(uri, prefix));
            }
            if (umbrella.equals(MATERIAL_IMPORT)) {
                prefixes.put(WIDGETS_IMPORT, Optional.empty());
            }
            DartImportPlan plan = new DartImportPlan(directives);
            return new ImportPlanner(umbrella, plan, prefixes);
        }

        DartImportPlan plan() {
            return plan;
        }

        String symbol(DartSymbolReference reference) {
            return renderedSymbol(reference).text();
        }

        String symbol(String uri, String name) {
            return renderedSymbol(uri, name).text();
        }

        RenderedSymbol renderedSymbol(DartSymbolReference reference) {
            return renderedSymbol(reference.libraryUri(), reference.name());
        }

        RenderedSymbol renderedSymbol(String uri, String name) {
            String effectiveUri = effectiveUri(uri);
            Optional<String> prefix = prefixesByUri.get(effectiveUri);
            if (prefix == null) {
                throw abort(diagnostic(
                        DartGenerationDiagnosticCode.INTERNAL_CATALOG_INCONSISTENCY,
                        "/root",
                        Optional.empty(),
                        Optional.of(DartManagedRegionId.IMPORTS),
                        "Dart symbol '" + name + "' requires undeclared import URI '"
                        + effectiveUri + "'."));
            }
            String text = prefix.map(value -> value + "." + name).orElse(name);
            return new RenderedSymbol(
                    text,
                    name,
                    effectiveUri,
                    text.length() - name.length());
        }

        String effectiveUri(String uri) {
            return uri.equals(WIDGETS_IMPORT) ? umbrellaUri : uri;
        }

        private static String uniquePrefix(String uri, Set<String> used) {
            String digest;
            try {
                digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(uri.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("The Java runtime does not provide SHA-256", impossible);
            }
            for (int length = 12; length <= digest.length(); length += 2) {
                String candidate = "_nbfd_" + digest.substring(0, length);
                if (used.add(candidate)) {
                    return candidate;
                }
            }
            int suffix = 2;
            while (!used.add("_nbfd_" + digest + '_' + suffix)) {
                suffix++;
            }
            return "_nbfd_" + digest + '_' + suffix;
        }
    }

    private record RenderedSymbol(
            String text,
            String name,
            String libraryUri,
            int nameOffset) {
    }
}
