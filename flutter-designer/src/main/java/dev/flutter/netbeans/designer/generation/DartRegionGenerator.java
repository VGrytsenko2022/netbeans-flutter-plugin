package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DartSymbolReference;
import dev.flutter.netbeans.designer.catalog.ParameterStyle;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Pure deterministic generator for schema-v1 managed Dart payloads. */
public final class DartRegionGenerator {
    public static final String PROFILE_ID = "fd-dart-regions-v1";

    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";
    private static final int INLINE_CONSTRUCTOR_LIMIT = 100;
    private static final Comparator<ConstructorArgument> ARGUMENT_ORDER = Comparator
            .comparing((ConstructorArgument value) -> value.parameter().style())
            .thenComparingInt(value -> value.parameter().order())
            .thenComparing(ConstructorArgument::name);

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
                usedDefinitions.values(), limits.maxImports());
        return new GenerationContext(catalog, planner.plan(), planner, 0);
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

        ArrayList<ConstructorArgument> arguments = new ArrayList<>();
        for (PropertyDefinition property : definition.properties()) {
            PropertyValue value = node.properties().get(property.name());
            if (value != null) {
                String propertyPath = path + "/properties/" + pointer(property.name().value());
                arguments.add(new ConstructorArgument(
                        property.parameter(),
                        property.name().value(),
                        false,
                        renderProperty(value, property, propertyPath, node.id(), context)));
            }
        }
        for (SlotDefinition slot : definition.slots()) {
            WidgetSlot value = node.slots().get(slot.name());
            if (value != null) {
                String slotPath = path + "/slots/" + pointer(slot.name().value());
                arguments.add(new ConstructorArgument(
                        slot.parameter(),
                        slot.name().value(),
                        true,
                        renderSlot(value, slotPath, baseIndent + 2, context)));
            }
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
            return scalar(
                    constructor + "()",
                    constant,
                    path,
                    node.id(),
                    context,
                    List.of(classOccurrence));
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
                return scalar(
                        inline.toString(),
                        constant,
                        path,
                        node.id(),
                        context,
                        inlineOccurrences);
            }
        }

        LineAccumulator lines = new LineAccumulator(
                context.maxRenderedUtf8Bytes(), path, node.id());
        lines.add(constructor + "(", List.of(classOccurrence));
        int argumentIndent = baseIndent + 2;
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
        lines.add(spaces(baseIndent) + ')');
        return lines.build(constant);
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

    private RenderedValue renderProperty(
            PropertyValue value,
            PropertyDefinition definition,
            String path,
            StableId widgetId,
            GenerationContext context) {
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
                int maximumImports) {
            TreeSet<String> uris = new TreeSet<>();
            uris.add(WIDGETS_IMPORT);
            for (WidgetDefinition definition : definitions) {
                for (String uri : definition.importUris()) {
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
