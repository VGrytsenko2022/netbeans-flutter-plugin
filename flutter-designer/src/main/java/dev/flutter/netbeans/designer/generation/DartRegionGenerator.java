package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DartSymbolReference;
import dev.flutter.netbeans.designer.catalog.MaterialThemeTokenCatalog;
import dev.flutter.netbeans.designer.catalog.ParameterStyle;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Pure deterministic generator for versioned .fd managed Dart payloads. */
public final class DartRegionGenerator {
    public static final String PROFILE_ID = "fd-dart-regions-v1";

    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";
    private static final int INLINE_CONSTRUCTOR_LIMIT = 100;
    private static final Comparator<ConstructorArgument> ARGUMENT_ORDER = Comparator
            .comparing((ConstructorArgument value) -> value.parameter().style())
            .thenComparingInt(value -> value.parameter().order())
            .thenComparing(ConstructorArgument::name);
    private static final Comparator<CompositeMember> COMPOSITE_MEMBER_ORDER = Comparator
            .comparingInt(CompositeMember::order)
            .thenComparing(CompositeMember::name);

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
                usedDefinitions.values(), limits.maxImports(), requiresMaterialTheme);
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
            if (node.type().equals(TextWidgetPropertySchema.TEXT_TYPE)
                    && TextWidgetPropertySchema.isCompound(property.name())) {
                continue;
            }
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
        if (node.type().equals(TextWidgetPropertySchema.TEXT_TYPE)) {
            appendTextCompoundArguments(
                    node, definition, path, baseIndent + 2, context, arguments);
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
        boolean constant = members.stream().allMatch(
                member -> member.rendered().constant());
        RenderedSymbol symbol = context.planner().renderedSymbol(
                WIDGETS_IMPORT, dartClass);
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
            default -> false;
        };
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
                boolean requiresMaterialTheme) {
            TreeSet<String> uris = new TreeSet<>();
            uris.add(requiresMaterialTheme ? MATERIAL_IMPORT : WIDGETS_IMPORT);
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
