package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DartSymbolReference;
import dev.flutter.netbeans.designer.catalog.AppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CardWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BadgeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CircleAvatarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.LinearProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CircularProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RefreshProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RefreshIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MenuItemButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MenuAnchorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MenuBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.NavigationBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.NavigationRailWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.NavigationDrawerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BottomNavigationBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MaterialWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SubmenuButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.OutlinedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FilledButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CheckboxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CheckboxListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RadioWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RadioListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TooltipWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TooltipThemeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RadioGroupWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SwitchWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SwitchListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SliderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RangeSliderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FloatingActionButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewExtentWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MaterialThemeTokenCatalog;
import dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ParameterStyle;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SingleChildScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PageViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListWheelScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CustomScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.AnimatedDefaultTextStyleWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DefaultTextStyleWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DefaultTextStyleTransitionWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DefaultTextHeightBehaviorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DefaultSelectionStyleWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MouseRegionWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FocusWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.NotificationListenerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BuilderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconThemeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.StateBinding;
import dev.flutter.netbeans.designer.model.StatePropertyBinding;
import dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog;
import dev.flutter.netbeans.designer.state.WidgetStatePropertyBindingCatalog;
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
    public static final String CURRENT_PROJECT_LIBRARY_URI = "project:current";
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
        // StatelessWidget.build and State<Owner>.build share the same managed
        // payload. The source-integrity gate owns proof of the enclosing class.
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
            int additionalSourceProbes = document.source().widgetKind() == WidgetClassKind.STATEFUL ? 1 : 0;
            if (symbolOccurrences.size()
                    > limits.candidateCapacityBudget()
                            .maxGeneratedSymbolOccurrences() - additionalSourceProbes) {
                throw abort(diagnostic(
                        DartGenerationDiagnosticCode.SYMBOL_PROBE_LIMIT,
                        "/source/managedRegions/build",
                        Optional.empty(),
                        Optional.of(DartManagedRegionId.BUILD),
                        "Generated Dart requires " + symbolOccurrences.size()
                        + " symbol occurrences plus "
                        + (limits.candidateCapacityBudget()
                                .reservedSourceSymbolProbes() + additionalSourceProbes)
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
        TreeSet<String> valueImportUris = new TreeSet<>();
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
            current.node().stateBinding().flatMap(StateBinding::referenceType)
                    .flatMap(PropertyValue.DartObjectReferenceValue::libraryUri).ifPresent(valueImportUris::add);
            for (var binding : current.node().propertyBindings().entrySet()) {
                if (FocusWidgetPropertySchema.propertyAvailable(current.node(), binding.getKey())
                        && SwitchListTileWidgetPropertySchema.propertyAvailable(current.node(), binding.getKey())
                        && RadioListTileWidgetPropertySchema.propertyAvailable(current.node(), binding.getKey())) {
                    binding.getValue().referenceType().flatMap(PropertyValue.DartObjectReferenceValue::libraryUri).ifPresent(valueImportUris::add);
                }
            }
            current.node().properties().entrySet().stream()
                    .filter(entry -> emittedReferenceProperty(current.node(), entry.getKey()))
                    .map(Map.Entry::getValue)
                    .filter(PropertyValue.DartObjectReferenceValue.class::isInstance)
                    .map(PropertyValue.DartObjectReferenceValue.class::cast)
                    .map(PropertyValue.DartObjectReferenceValue::libraryUri)
                    .flatMap(Optional::stream)
                    .forEach(valueImportUris::add);
            requiresMaterialTheme |= current.node().properties().values().stream()
                    .anyMatch(DartRegionGenerator::requiresMaterialTheme);
            requiresServices |= (current.node().type().equals(
                    AppBarWidgetPropertySchema.APP_BAR_TYPE) || dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.isType(current.node().type()))
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
            requiresDartUi |= current.node().properties().values().stream()
                    .anyMatch(PropertyValue.PointerDeviceKindSetValue.class::isInstance);

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
                usedDefinitions.values(), valueImportUris, limits.maxImports(),
                requiresMaterialTheme,
                requiresServices, requiresGestures, requiresRendering,
                requiresDartConvert, requiresDartUi);
        return new GenerationContext(catalog, planner.plan(), planner, 0);
    }

    private static boolean emittedReferenceProperty(WidgetNode node, PropertyName name) {
        if (node.type().equals(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE)
                && node.propertyBindings().containsKey(new PropertyName("enabled"))) return true;
        if (!FocusWidgetPropertySchema.propertyAvailable(node, name) || !SwitchListTileWidgetPropertySchema.propertyAvailable(node, name)
                || !RadioListTileWidgetPropertySchema.propertyAvailable(node, name)) return false;
        if (node.type().equals(CheckboxWidgetPropertySchema.CHECKBOX_TYPE)
                || node.type().equals(SwitchWidgetPropertySchema.SWITCH_TYPE)
                || node.type().equals(SliderWidgetPropertySchema.SLIDER_TYPE)
                || node.type().equals(RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE)) {
            return !name.value().equals("onChanged") || !new PropertyValue.BooleanValue(false).equals(
                    node.properties().get(new PropertyName("enabled")));
        }
        return !(OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)
                || node.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE))
                || !new PropertyValue.BooleanValue(false).equals(
                        node.properties().get(new PropertyName("enabled")))
                || !(name.value().equals("onPressed") || name.value().equals("onLongPress"));
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
                GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE)
                || node.type().equals(GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE);
        boolean pageView = node.type().equals(
                PageViewWidgetPropertySchema.PAGE_VIEW_TYPE);
        boolean listWheelScrollView = node.type().equals(
                ListWheelScrollViewWidgetPropertySchema.LIST_WHEEL_SCROLL_VIEW_TYPE);
        boolean customScrollView = node.type().equals(
                CustomScrollViewWidgetPropertySchema.CUSTOM_SCROLL_VIEW_TYPE);
        int constructorBaseIndent = textField || listView || gridView || pageView || listWheelScrollView || customScrollView
                ? baseIndent + 4 : baseIndent;

        ArrayList<ConstructorArgument> arguments = new ArrayList<>();
        if (node.type().equals(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE)) {
            context.buttonFamilies().put(node.id(), new ButtonFamily("MenuAnchor", "MenuTheme", false, false, "", node));
        }
        if (node.type().equals(MenuBarWidgetPropertySchema.MENU_BAR_TYPE)) {
            context.buttonFamilies().put(node.id(), new ButtonFamily("MenuBar", "MenuTheme", false, false, "", node));
        }
        if (OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)) {
            boolean icon = OutlinedButtonWidgetPropertySchema.isIconVariant(node);
            boolean hasIcon = node.slots().get(new SlotName("icon")) instanceof WidgetSlot.SingleSlot slot
                    && slot.child().isPresent();
            String family = node.type().equals(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE) ? "SubmenuButton"
                    : node.type().equals(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE) ? "MenuItemButton"
                    : node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE) ? "IconButton"
                    : node.type().equals(FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE) ? "FilledButton"
                    : node.type().equals(OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE)
                    ? "OutlinedButton" : "TextButton";
            context.buttonFamilies().put(node.id(), new ButtonFamily(family,
                    family.equals("MenuItemButton") || family.equals("SubmenuButton") ? "MenuButtonTheme" : family + "Theme", icon, hasIcon,
                    node.type().equals(FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE)
                            ? FilledButtonWidgetPropertySchema.constructorName(node)
                            : node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE)
                                    ? IconButtonWidgetPropertySchema.constructorName(node) : icon ? "icon" : "", node));
        }
        for (PropertyDefinition property : definition.properties()) {
            if ((node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.ThemeWidgetPropertySchema.TYPE))
                    && property.name().value().equals("data") && node.properties().get(property.name()) instanceof PropertyValue.StringValue) continue;
            if ((node.type().equals(dev.flutter.netbeans.designer.catalog.SliverAnimatedOpacityWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedOpacityWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSlideWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedScaleWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.TYPE)
                    || dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.supports(node.type()) || node.type().equals(AnimatedDefaultTextStyleWidgetPropertySchema.TYPE) || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedPhysicalModelWidgetPropertySchema.TYPE) || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.TYPE) || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedFractionallySizedBoxWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedRotationWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedPaddingWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedAlignWidgetPropertySchema.TYPE))
                    && Set.of("durationUs", "curve").contains(property.name().value())) continue;
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.TYPE)) continue;
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.FadeInImageWidgetPropertySchema.TYPE)
                    && Set.of("fadeOutDurationUs", "fadeInDurationUs", "fadeOutCurve", "fadeInCurve").contains(property.name().value())) continue;
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSwitcherWidgetPropertySchema.TYPE)
                    && Set.of("durationUs", "reverseDurationUs", "switchInCurve", "switchOutCurve").contains(property.name().value())) continue;
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedCrossFadeWidgetPropertySchema.TYPE)
                    && Set.of("durationUs", "reverseDurationUs", "firstCurve", "secondCurve", "sizeCurve").contains(property.name().value())) continue;
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.TYPE)
                    && property.name().value().equals("reverseDurationUs")) continue;
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.TYPE)) continue;
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.RECT_TYPE)
                    && property.name().value().startsWith("rect")) continue;
            if (node.type().equals(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE)
                    && SubmenuButtonWidgetPropertySchema.isCompound(property.name())) continue;
            if (node.type().equals(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE)
                    && MenuAnchorWidgetPropertySchema.isCompound(property.name())) continue;
            if (node.type().equals(MenuBarWidgetPropertySchema.MENU_BAR_TYPE)
                    && MenuBarWidgetPropertySchema.isCompound(property.name())) continue;
            if (node.type().equals(NavigationBarWidgetPropertySchema.NAVIGATION_BAR_TYPE)
                    && (NavigationBarWidgetPropertySchema.durationProperties().contains(property.name().value())
                    || property.name().value().equals("onDestinationSelected"))) continue;
            if (node.type().equals(BottomNavigationBarWidgetPropertySchema.BOTTOM_NAVIGATION_BAR_TYPE)
                    && property.name().value().equals("onTap")) continue;
            if (node.type().equals(MaterialWidgetPropertySchema.MATERIAL_TYPE)
                    && property.name().value().equals("animationDurationUs")) continue;
            if (node.type().equals(NavigationRailWidgetPropertySchema.NAVIGATION_RAIL_TYPE)
                    && property.name().value().equals("onDestinationSelected")) continue;
            if (node.type().equals(NavigationDrawerWidgetPropertySchema.NAVIGATION_DRAWER_TYPE)
                    && property.name().value().equals("onDestinationSelected")) continue;
            if (node.type().equals(PageViewWidgetPropertySchema.PAGE_VIEW_TYPE)
                    && property.name().value().equals("onPageChanged")) continue;
            if (node.type().equals(ListWheelScrollViewWidgetPropertySchema.LIST_WHEEL_SCROLL_VIEW_TYPE)
                    && property.name().value().equals("onSelectedItemChanged")) continue;
            if (node.type().equals(TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE)) continue;
            if (node.type().equals(TooltipWidgetPropertySchema.TOOLTIP_TYPE)
                    && (TooltipWidgetPropertySchema.isTextStyleProperty(property.name())
                    || TooltipWidgetPropertySchema.durationProperties().contains(property.name().value())
                    || property.name().value().equals("onTriggered")
                    || property.name().value().equals("mouseCursor") && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.TYPE)
                    && (property.name().value().equals("animationStyle")
                        || dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.LOCAL_STYLE.contains(property.name().value()))) continue;
            if (node.type().equals(ExpansionTileWidgetPropertySchema.EXPANSION_TILE_TYPE)
                    && (property.parameter().order() >= 33 || property.name().value().equals("onExpansionChanged")
                    || property.name().value().equals("expansionAnimationStyle")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE)
                    && (property.parameter().order() >= 40
                    || !RadioListTileWidgetPropertySchema.propertyAvailable(node, property.name())
                    || Set.of("value", "groupValue", "onChanged", "onFocusChange").contains(property.name().value())
                    || property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE)
                    && (property.parameter().order() >= 44
                    || !SwitchListTileWidgetPropertySchema.propertyAvailable(node, property.name())
                    || Set.of("onChanged", "onFocusChange", "onActiveThumbImageError", "onInactiveThumbImageError").contains(property.name().value())
                    || property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(node.type())) continue;
            if (dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(node.type())
                    && !property.name().value().equals("onEnd")) continue;
            if (usesListenableBuilderPresets(node)
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue) continue;
            if (node.type().equals(NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE)) continue;
            if (node.type().equals(BuilderWidgetPropertySchema.BUILDER_TYPE)
                    && property.name().value().equals("builder")) continue;
            if ((usesEmptyBoxBuilderPreset(node) || dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.SLIVER_TYPE.equals(node.type())
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.TYPE)
                    || dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.find(node.type()).isPresent()
                    || dev.flutter.netbeans.designer.catalog.SliverFixedExtentListWidgetPropertySchema.find(node.type()).isPresent()
                || dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.find(node.type()).isPresent()
                || dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.find(node.type()).isPresent()
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.SliverFillViewportWidgetPropertySchema.DELEGATE))
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue) continue;
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.SliverFillViewportWidgetPropertySchema.CHILDREN)
                    && property.parameter().order() >= 3) continue;
            if (node.type().equals(FocusWidgetPropertySchema.FOCUS_TYPE)
                    && (property.name().value().equals("variant")
                    || !FocusWidgetPropertySchema.propertyAvailable(node, property.name()))) continue;
            if (node.type().equals(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE)) continue;
            if (node.type().equals(CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE)
                    && (property.parameter().order() >= 40
                    || Set.of("onChanged", "onFocusChange").contains(property.name().value())
                    || property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(ListTileWidgetPropertySchema.LIST_TILE_TYPE)
                    && (property.parameter().order() >= 37
                    || Set.of("onTap", "onLongPress", "onFocusChange").contains(property.name().value())
                    || property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(RadioWidgetPropertySchema.RADIO_TYPE)
                    && (property.parameter().order() >= 23
                    || Set.of("value", "groupValue", "onChanged", "groupRegistry", "visualDensityHorizontal", "visualDensityVertical").contains(property.name().value())
                    || property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE)
                    && (property.parameter().order() >= 18
                    || Set.of("valuesStart", "valuesEnd", "labelsStart", "labelsEnd", "onChanged").contains(property.name().value()))) continue;
            if (node.type().equals(SliderWidgetPropertySchema.SLIDER_TYPE)
                    && (property.parameter().order() >= 22 || property.name().value().equals("onChanged")
                    || property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(SwitchWidgetPropertySchema.SWITCH_TYPE)
                    && (property.parameter().order() >= 28 || property.name().value().equals("onChanged")
                    || property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(CheckboxWidgetPropertySchema.CHECKBOX_TYPE)
                    && (property.parameter().order() >= 20
                    || Set.of("onChanged", "visualDensityHorizontal", "visualDensityVertical").contains(property.name().value())
                    || property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE)
                    && (IconButtonWidgetPropertySchema.isCompound(property.name())
                    || property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            if (node.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE)
                    && (Set.of("variant", "enabled", "onPressed").contains(property.name().value())
                    || FloatingActionButtonWidgetPropertySchema.isTextStyleProperty(property.name())
                    || FloatingActionButtonWidgetPropertySchema.builtInShapePropertyNames().contains(property.name().value())
                    || (property.name().value().equals("mouseCursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue))) continue;
            if (OutlinedButtonWidgetPropertySchema.isFullStyleButton(node)
                    && TextButtonWidgetPropertySchema.isCompound(property.name())) continue;
            if (node.type().equals(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE)
                    && MenuItemButtonWidgetPropertySchema.isCompound(property.name())) continue;
            if (node.type().equals(RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE)
                    && property.name().value().equals("variant")) continue;
            if (node.type().equals(CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE)
                    && property.name().value().equals("variant")) continue;
            if (node.type().equals(BadgeWidgetPropertySchema.BADGE_TYPE)
                    && BadgeWidgetPropertySchema.isTextStyleProperty(property.name())) continue;
            if (node.type().equals(CardWidgetPropertySchema.CARD_TYPE)
                    && (property.name().value().equals("variant")
                    || CardWidgetPropertySchema.builtInShapePropertyNames().contains(property.name().value()))) {
                continue;
            }
            if (node.type().equals(IconThemeWidgetPropertySchema.ICON_THEME_TYPE)) {
                // The nine leaves form required data; merge chooses a static helper.
                continue;
            }
            if (node.type().equals(DefaultSelectionStyleWidgetPropertySchema.DEFAULT_SELECTION_STYLE_TYPE)
                    && (property.name().value().equals("merge") || property.name().value().equals("mouseCursor"))) {
                // merge selects a static helper, while mouseCursor is a closed SDK constant.
                continue;
            }
            if (node.type().equals(MouseRegionWidgetPropertySchema.MOUSE_REGION_TYPE)
                    && property.name().value().equals("cursor")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue) {
                // A reviewed cursor preset is an SDK constant, never a Dart string literal.
                continue;
            }
            if (node.type().equals(DefaultTextHeightBehaviorWidgetPropertySchema.DEFAULT_TEXT_HEIGHT_BEHAVIOR_TYPE)) {
                // All three flattened leaves belong inside the required composite.
                continue;
            }
            if (node.type().equals(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE)
                    && ScaffoldWidgetPropertySchema.isStaticPreset(property.name())) {
                continue;
            }
            if ((node.type().equals(TextWidgetPropertySchema.TEXT_TYPE)
                    || DefaultTextStyleWidgetPropertySchema.sharesTextProjection(node.type()))
                    && (TextWidgetPropertySchema.isCompound(property.name()) || property.name().value().equals("style"))) {
                continue;
            }
            if (dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarWidgetPropertySchema.TYPE.equals(node.type())
                    && property.name().value().equals("stretchModes")
                    && node.properties().get(property.name()) instanceof PropertyValue.StringValue modes) {
                var symbol = context.planner().renderedSymbol(MATERIAL_IMPORT, "StretchMode");
                String valuePath = path + "/properties/stretchModes";
                String prefix = "widget:" + node.id() + ":stretch-modes";
                var expression = new StringBuilder("const <");
                var symbols = new ArrayList<GeneratedDartSymbolOccurrence>();
                symbols.add(occurrence(prefix + ":type", expression.length() + symbol.nameOffset(),
                        symbol.name(), symbol.libraryUri(), valuePath, Optional.of(node.id())));
                expression.append(symbol.text()).append(">[");
                if (!modes.value().equals("none")) for (String mode : modes.value().split(",")) {
                    if (expression.charAt(expression.length() - 1) != '[') expression.append(", ");
                    symbols.add(occurrence(prefix + ":" + mode, expression.length() + symbol.nameOffset(),
                            symbol.name(), symbol.libraryUri(), valuePath, Optional.of(node.id())));
                    expression.append(symbol.text()).append('.').append(mode);
                }
                expression.append(']');
                arguments.add(new ConstructorArgument(property.parameter(), "stretchModes", false,
                        scalar(expression.toString(), true, valuePath, node.id(), context, symbols)));
                continue;
            }
            if (dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.isType(node.type())
                    && property.name().value().equals("onStretchTrigger") && node.properties().get(property.name()) instanceof PropertyValue.StringValue) {
                arguments.add(new ConstructorArgument(property.parameter(), "onStretchTrigger", false,
                        scalar("() async {}", false, path + "/properties/onStretchTrigger", node.id(), context)));
                continue;
            }
            if ((node.type().equals(AppBarWidgetPropertySchema.APP_BAR_TYPE)
                    || dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.isType(node.type()))
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
            if ((node.type().equals(GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE)
                    && GridViewCountWidgetPropertySchema.isSynthesized(property.name()))
                    || (node.type().equals(GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE)
                    && GridViewExtentWidgetPropertySchema.isSynthesized(property.name()))) {
                continue;
            }
            if (node.type().equals(SingleChildScrollViewWidgetPropertySchema
                    .SINGLE_CHILD_SCROLL_VIEW_TYPE)
                    && SingleChildScrollViewWidgetPropertySchema
                            .isSynthesized(property.name())) {
                continue;
            }
            if (node.type().equals(PageViewWidgetPropertySchema.PAGE_VIEW_TYPE)
                    && PageViewWidgetPropertySchema.isSynthesized(property.name())) {
                continue;
            }
            if (node.type().equals(CustomScrollViewWidgetPropertySchema.CUSTOM_SCROLL_VIEW_TYPE)
                    && CustomScrollViewWidgetPropertySchema.isSynthesized(property.name())) {
                continue;
            }
            PropertyValue value = node.properties().get(property.name());
            if (value != null) {
                String propertyPath = path + "/properties/" + pointer(property.name().value());
                arguments.add(new ConstructorArgument(
                        property.parameter(),
                        property.name().value().equals("barType") || property.name().value().equals("materialType")
                                ? "type" : property.name().value(),
                        false,
                        node.type().equals(FocusWidgetPropertySchema.FOCUS_TYPE)
                                && FocusWidgetPropertySchema.usesExternalNode(node)
                                && property.name().value().equals("focusNode")
                                && value instanceof PropertyValue.DartObjectReferenceValue focusNode
                                ? renderDartObjectReference(focusNode, "FocusNode", propertyPath, node.id(), context)
                                : ((node.type().equals(LinearProgressIndicatorWidgetPropertySchema.LINEAR_PROGRESS_INDICATOR_TYPE)
                                || node.type().equals(CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE)
                                || node.type().equals(RefreshProgressIndicatorWidgetPropertySchema.REFRESH_PROGRESS_INDICATOR_TYPE))
                                && property.name().value().equals("valueColor")
                                || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedModalBarrierWidgetPropertySchema.TYPE)
                                && property.name().value().equals("color"))
                                && !(value instanceof PropertyValue.DartObjectReferenceValue)
                                ? renderProgressValueColor(value, property, propertyPath, node.id(), context)
                                : node.type().equals(RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE)
                                        && property.name().value().equals("notificationPredicate")
                                        && value instanceof PropertyValue.StringValue preset
                                        ? renderRefreshNotificationPredicate(preset, propertyPath, node.id(), context)
                                : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedIconWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("icon") && value instanceof PropertyValue.StringValue preset
                                        ? renderAnimatedIconPreset(preset.value(), propertyPath, node.id(), context)
                                : (node.type().equals(dev.flutter.netbeans.designer.catalog.RotationTransitionWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("turns")
                                        || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedIconWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("progress")
                                        || node.type().equals(dev.flutter.netbeans.designer.catalog.RawImageWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("opacity") && !(value instanceof PropertyValue.NullValue))
                                        && !(value instanceof PropertyValue.DartObjectReferenceValue)
                                        ? renderStoppedTurns(value, propertyPath, node.id(), context)
                                : node.type().equals(dev.flutter.netbeans.designer.catalog.SizeTransitionWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("sizeFactor")
                                        && !(value instanceof PropertyValue.DartObjectReferenceValue)
                                        ? renderStoppedSizeFactor(value, propertyPath, node.id(), context)
                                : node.type().equals(dev.flutter.netbeans.designer.catalog.ScaleTransitionWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("scale")
                                        && !(value instanceof PropertyValue.DartObjectReferenceValue)
                                        ? renderStoppedScale(value, propertyPath, node.id(), context)
                                : node.type().equals(dev.flutter.netbeans.designer.catalog.MatrixTransitionWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("animation")
                                        && !(value instanceof PropertyValue.DartObjectReferenceValue)
                                        ? renderStoppedMatrixAnimation(value, propertyPath, node.id(), context)
                                : node.type().equals(dev.flutter.netbeans.designer.catalog.MatrixTransitionWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("onTransform")
                                        && value instanceof PropertyValue.Matrix4Value matrix
                                        ? renderMatrixCallback(matrix, propertyPath, node.id(), context)
                                : node.type().equals(dev.flutter.netbeans.designer.catalog.AlignTransitionWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("alignment")
                                        && value instanceof PropertyValue.AlignmentGeometryValue
                                        ? renderStoppedAlignment(renderProperty(value, property, propertyPath, node.id(), context,
                                            constructorBaseIndent + 2), propertyPath, node.id(), context)
                                : node.type().equals(dev.flutter.netbeans.designer.catalog.DecoratedBoxTransitionWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("decoration")
                                        && value instanceof PropertyValue.BoxDecorationValue
                                        ? renderStoppedDecoration(renderProperty(value, property, propertyPath, node.id(), context,
                                            constructorBaseIndent + 2), propertyPath, node.id(), context)
                                : node.type().equals(dev.flutter.netbeans.designer.catalog.SlideTransitionWidgetPropertySchema.TYPE)
                                        && property.name().value().equals("position")
                                        && value instanceof PropertyValue.OffsetValue offset
                                        ? renderStoppedOffset(renderOffset(offset, propertyPath, node.id(), context), propertyPath, node.id(), context)
                                : dev.flutter.netbeans.designer.catalog.FadeTransitionWidgetPropertySchema.supports(node.type())
                                        && property.name().value().equals("opacity")
                                        && !(value instanceof PropertyValue.DartObjectReferenceValue)
                                        ? renderStoppedOpacity(value, propertyPath, node.id(), context)
                                : renderProperty(value, property, propertyPath, node.id(), context,
                                        constructorBaseIndent + 2)));
            }
        }
        if (node.type().equals(RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE)
                && !node.properties().containsKey(new PropertyName("onRefresh"))) {
            arguments.add(new ConstructorArgument(DartParameter.named(2, true), "onRefresh", false,
                    scalar("() async {}", false, path + "/properties/onRefresh", node.id(), context)));
        }
        for (SlotDefinition slot : definition.slots()) {
            WidgetSlot value = node.slots().get(slot.name());
            if ((node.type().equals(dev.flutter.netbeans.designer.catalog.SliverAnimatedOpacityWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.FadeTransitionWidgetPropertySchema.SLIVER_TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.SliverOpacityWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.SliverIgnorePointerWidgetPropertySchema.TYPE)
                    || node.type().equals(dev.flutter.netbeans.designer.catalog.SliverOffstageWidgetPropertySchema.TYPE))
                    && slot.name().value().equals("sliver")
                    && (value == null || value instanceof WidgetSlot.SingleSlot single && single.child().isEmpty())) {
                // Flutter 3.44.8 accepts null in the constructor but RenderProxySliver
                // dereferences its child during layout. Keep the FD slot empty and
                // lower it to a zero-extent native sliver in generated Dart and Canvas.
                String slotPath = path + "/slots/sliver";
                var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, "SliverToBoxAdapter");
                var occurrence = occurrence("widget:" + node.id() + ":emptySliver",
                        6 + symbol.nameOffset(), symbol.name(), symbol.libraryUri(), slotPath, Optional.of(node.id()));
                arguments.add(new ConstructorArgument(slot.parameter(), "sliver", true,
                        scalar("const " + symbol.text() + "()", true, slotPath, node.id(), context, List.of(occurrence))));
                continue;
            }
            if (dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.find(node.type()).isPresent()
                    && slot.name().value().equals("prototypeItem")
                    && (value == null || value instanceof WidgetSlot.SingleSlot single && single.child().isEmpty())) {
                String slotPath = path + "/slots/prototypeItem";
                var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, "SizedBox");
                var occurrence = occurrence("widget:" + node.id() + ":prototypePreset",
                        6 + symbol.nameOffset(), symbol.name(), symbol.libraryUri(), slotPath, Optional.of(node.id()));
                arguments.add(new ConstructorArgument(slot.parameter(), "prototypeItem", true,
                        scalar("const " + symbol.text() + "(width: 48.0, height: 48.0)", true,
                                slotPath, node.id(), context, List.of(occurrence))));
                continue;
            }
            if (node.type().equals(dev.flutter.netbeans.designer.catalog.SliverFillViewportWidgetPropertySchema.CHILDREN)) {
                arguments.add(new ConstructorArgument(slot.parameter(), "delegate", true,
                        renderViewportDelegate(node, definition, path, constructorBaseIndent + 2, context)));
                continue;
            }
            if (node.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE)
                    && slot.name().value().equals("icon")
                    && !FloatingActionButtonWidgetPropertySchema.isExtendedConstructor(node)) continue;
            if (OutlinedButtonWidgetPropertySchema.isFullStyleButton(node)
                    && slot.name().value().equals("icon") && !OutlinedButtonWidgetPropertySchema.isIconVariant(node)) continue;
            if (BadgeWidgetPropertySchema.isCountMode(node) && slot.name().value().equals("label")) continue;
            if (value != null) {
                if ((node.type().value().equals("flutter.widgets.Transform")
                        || (node.type().value().equals("flutter.widgets.Visibility")
                        && slot.name().value().equals("replacement"))
                        || (dev.flutter.netbeans.designer.catalog.SliverVisibilityWidgetPropertySchema.supports(node.type())
                        && slot.name().value().equals("replacementSliver")))
                        && !slot.parameter().required()
                        && value instanceof WidgetSlot.SingleSlot single
                        && single.child().isEmpty()) {
                    // Preserve omitted Transform.child and non-null Visibility/
                    // SliverVisibility replacement defaults; empty is not Dart null.
                    continue;
                }
                String slotPath = path + "/slots/" + pointer(slot.name().value());
                arguments.add(new ConstructorArgument(
                        slot.parameter(),
                        (OutlinedButtonWidgetPropertySchema.isIconVariant(node)
                                || FloatingActionButtonWidgetPropertySchema.isExtendedConstructor(node))
                                && slot.name().value().equals("child")
                                ? "label" : slot.name().value(),
                        true,
                        isNavigationDestinationsSlot(node, slot.name())
                                ? renderNavigationDestinations(value, slotPath,
                                        constructorBaseIndent + 2, context,
                                        node.type().equals(NavigationRailWidgetPropertySchema.NAVIGATION_RAIL_TYPE),
                                        node.type().equals(NavigationDrawerWidgetPropertySchema.NAVIGATION_DRAWER_TYPE),
                                        node.type().equals(BottomNavigationBarWidgetPropertySchema.BOTTOM_NAVIGATION_BAR_TYPE))
                                : renderSlot(value, slotPath, constructorBaseIndent + 2, context)));
            }
        }
        if (DefaultTextStyleWidgetPropertySchema.sharesTextProjection(node.type())) {
            var styleProperty = definition.property(new PropertyName("style")).orElseThrow();
            var styleValue = node.properties().get(styleProperty.name());
            boolean local = styleValue instanceof PropertyValue.StringValue;
            appendTextCompoundArguments(node, definition, path, constructorBaseIndent + 2, context, arguments, "style", 0,
                name -> (local && AnimatedDefaultTextStyleWidgetPropertySchema.styleLeaf(name)
                    || !node.properties().containsKey(new PropertyName("textHeightBehavior")) && AnimatedDefaultTextStyleWidgetPropertySchema.heightLeaf(name))
                    ? TextWidgetPropertySchema.find(name) : Optional.empty());
            if (!local && styleValue != null) arguments.add(new ConstructorArgument(styleProperty.parameter(), "style", false,
                    renderProperty(styleValue, styleProperty, path + "/properties/style", node.id(), context)));
            else if (local && arguments.stream().noneMatch(a -> a.name().equals("style")))
                arguments.add(new ConstructorArgument(styleProperty.parameter(), "style", false,
                    renderNamedCompositeMembers("TextStyle", Optional.empty(), List.of(), constructorBaseIndent + 2, path + "/properties/style", node.id(), context)));
            if (local && node.type().equals(DefaultTextStyleTransitionWidgetPropertySchema.TYPE)) {
                for (int index = 0; index < arguments.size(); index++) {
                    var argument = arguments.get(index);
                    if (argument.name().equals("style")) arguments.set(index, new ConstructorArgument(
                            argument.parameter(), argument.name(), false,
                            renderStoppedTextStyle(argument.value(), path + "/properties/style", node.id(), context)));
                }
            }
        }
        if (node.type().equals(TextWidgetPropertySchema.TEXT_TYPE) || node.type().equals(BadgeWidgetPropertySchema.BADGE_TYPE)
                || node.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE)) {
            appendTextCompoundArguments(
                    node, definition, path, constructorBaseIndent + 2,
                    context, arguments);
        }
        if (node.type().equals(CardWidgetPropertySchema.CARD_TYPE)
                || node.type().equals(ListTileWidgetPropertySchema.LIST_TILE_TYPE)
                || node.type().equals(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE)
                || node.type().equals(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE)
                || node.type().equals(CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE)
                || node.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE)
                || node.type().equals(CheckboxWidgetPropertySchema.CHECKBOX_TYPE)) {
            appendCardShape(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(CheckboxWidgetPropertySchema.CHECKBOX_TYPE)) {
            appendCheckboxArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE)) {
            appendCheckboxListTileArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(RadioWidgetPropertySchema.RADIO_TYPE)) {
            appendRadioArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE)) {
            appendRadioListTileArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.TYPE)) {
            appendFloatingHeaderAnimationStyle(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(ExpansionTileWidgetPropertySchema.EXPANSION_TILE_TYPE)) {
            appendExpansionTileArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(TooltipWidgetPropertySchema.TOOLTIP_TYPE)) {
            appendTooltipArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE)) {
            appendTooltipThemeArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE)) {
            appendRadioIdentityArguments(node, definition, path, context, arguments, List.of("groupValue", "onChanged"));
        }
        if (node.type().equals(NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE)) {
            appendNotificationListenerArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(BuilderWidgetPropertySchema.BUILDER_TYPE)) {
            appendBuilderArguments(node, definition, path, context, arguments);
        }
        if (usesEmptyBoxBuilderPreset(node) || dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.SLIVER_TYPE.equals(node.type())
                || node.type().equals(dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.TYPE)
                || dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.find(node.type()).isPresent()
                || dev.flutter.netbeans.designer.catalog.SliverFixedExtentListWidgetPropertySchema.find(node.type()).isPresent()
                || dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.find(node.type()).isPresent()
                || dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.find(node.type()).isPresent()
                || node.type().equals(dev.flutter.netbeans.designer.catalog.SliverFillViewportWidgetPropertySchema.DELEGATE)) {
            appendSliverPresetArguments(node, definition, path, context, arguments);
        }
        if (dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(node.type())) {
            appendValueListenableBuilderArguments(node, definition, path, context, arguments);
        }
        if (dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(node.type())) {
            appendTweenAnimationBuilderArguments(node, definition, path, context, arguments);
        }
        if (usesListenableBuilderPresets(node)) {
            appendListenableBuilderPresets(node, definition, path, context, arguments);
        }
        if (node.type().equals(ListTileWidgetPropertySchema.LIST_TILE_TYPE)) {
            appendListTileArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(SwitchWidgetPropertySchema.SWITCH_TYPE)) {
            appendSwitchArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE)) {
            appendSwitchListTileArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(SliderWidgetPropertySchema.SLIDER_TYPE)) {
            appendSliderArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE)) {
            appendRangeSliderArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE)) {
            PropertyDefinition pressed = definition.property(new PropertyName("onPressed")).orElseThrow();
            PropertyValue reference = node.properties().get(pressed.name());
            boolean enabled = !new PropertyValue.BooleanValue(false).equals(node.properties().get(new PropertyName("enabled")));
            RenderedValue callback = !enabled
                    ? scalar("null", true, path + "/properties/enabled", node.id(), context)
                    : reference == null ? scalar("() {}", false, path + "/properties/onPressed", node.id(), context)
                    : renderProperty(reference, pressed, path + "/properties/onPressed", node.id(), context);
            arguments.add(new ConstructorArgument(pressed.parameter(), "onPressed", false, callback));
            if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) {
                appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
            }
        }
        if (node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE)) {
            if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) {
                appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
            }
            RenderedValue density = renderIconButtonDirectDensity(node, definition, path, context);
            if (density != null) {
                arguments.add(new ConstructorArgument(DartParameter.named(1, false), "visualDensity", false, density));
            }
        }
        if (node.type().equals(IconThemeWidgetPropertySchema.ICON_THEME_TYPE)) {
            ArrayList<CompositeMember> members = new ArrayList<>();
            for (PropertyDefinition property : definition.properties()) {
                if (property.name().value().equals("merge")) continue;
                PropertyValue value = node.properties().get(property.name());
                if (value == null) continue;
                var binding = IconThemeWidgetPropertySchema.find(property.name()).orElseThrow();
                String propertyPath = path + "/properties/" + pointer(property.name().value());
                members.add(new CompositeMember(binding.dartName(), binding.dartOrder(),
                        renderProperty(value, property, propertyPath, node.id(), context)));
            }
            members.sort(COMPOSITE_MEMBER_ORDER);
            // Empty data is required and intentionally shadows the outer theme in direct mode.
            arguments.add(new ConstructorArgument(DartParameter.named(0, true), "data", false,
                    renderNamedCompositeMembers("IconThemeData", Optional.empty(), members,
                            constructorBaseIndent + 2, path + "/properties/data", node.id(), context)));
        }
        if (node.type().equals(DefaultTextHeightBehaviorWidgetPropertySchema.DEFAULT_TEXT_HEIGHT_BEHAVIOR_TYPE)) {
            ArrayList<CompositeMember> members = new ArrayList<>();
            for (PropertyDefinition property : definition.properties()) {
                PropertyValue value = node.properties().get(property.name());
                if (value == null) continue;
                var binding = DefaultTextHeightBehaviorWidgetPropertySchema.find(property.name()).orElseThrow();
                String propertyPath = path + "/properties/" + pointer(property.name().value());
                members.add(new CompositeMember(binding.dartName(), binding.dartOrder(),
                        renderProperty(value, property, propertyPath, node.id(), context)));
            }
            members.sort(COMPOSITE_MEMBER_ORDER);
            // Unlike Text's optional composite, all-unset still overrides ambient
            // values with const TextHeightBehavior() (true/true/proportional).
            arguments.add(new ConstructorArgument(DartParameter.named(0, true), "textHeightBehavior", false,
                    renderNamedCompositeMembers("TextHeightBehavior", Optional.empty(), members,
                            constructorBaseIndent + 2, path + "/properties/textHeightBehavior", node.id(), context)));
        }
        if (node.type().equals(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE)) {
            appendScaffoldStaticPresetArguments(
                    node, definition, path, context, arguments);
        }
        if (node.type().equals(AppBarWidgetPropertySchema.APP_BAR_TYPE)
                || dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.isType(node.type())) {
            appendAppBarCompoundArguments(
                    node, definition, path, constructorBaseIndent + 2,
                    context, arguments);
        }
        if (node.type().equals(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE)
                || node.type().equals(MenuBarWidgetPropertySchema.MENU_BAR_TYPE)) {
            appendMenuAnchorStyleArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(NavigationBarWidgetPropertySchema.NAVIGATION_BAR_TYPE)) {
            appendNavigationBarArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(BottomNavigationBarWidgetPropertySchema.BOTTOM_NAVIGATION_BAR_TYPE)) {
            appendBottomNavigationBarArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.SliverAnimatedOpacityWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedOpacityWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSlideWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedScaleWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.TYPE)
                || dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.supports(node.type()) || node.type().equals(AnimatedDefaultTextStyleWidgetPropertySchema.TYPE) || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedPhysicalModelWidgetPropertySchema.TYPE) || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.TYPE) || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedFractionallySizedBoxWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedRotationWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedPaddingWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedAlignWidgetPropertySchema.TYPE)
                || dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(node.type())) {
            for (String name : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.TYPE)
                    ? List.of("durationUs", "reverseDurationUs", "curve") : List.of("durationUs", "curve")) {
                PropertyValue value = node.properties().get(new PropertyName(name));
                if (value == null) continue;
                var property = definition.property(new PropertyName(name)).orElseThrow();
                String valuePath = path + "/properties/" + name;
                RenderedValue rendered = (name.equals("durationUs") || name.equals("reverseDurationUs")) && value instanceof PropertyValue.IntegerValue integer
                        ? scalar("const Duration(microseconds: " + integer.value() + ")", true, valuePath, node.id(), context,
                            List.of(occurrence("widget:" + node.id() + (node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.TYPE) ? ":animated-theme-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedFractionallySizedBoxWidgetPropertySchema.TYPE) ? ":animated-fractionally-sized-box-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedPhysicalModelWidgetPropertySchema.TYPE) ? ":animated-physical-model-duration" : dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(node.type())
                                        ? ":tween-animation-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedOpacityWidgetPropertySchema.TYPE)
                                            ? ":animated-opacity-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedAlignWidgetPropertySchema.TYPE)
                                                ? ":animated-align-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedPaddingWidgetPropertySchema.TYPE)
                                                    ? ":animated-padding-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSlideWidgetPropertySchema.TYPE)
                                                        ? ":animated-slide-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedScaleWidgetPropertySchema.TYPE)
                                                            ? ":animated-scale-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedRotationWidgetPropertySchema.TYPE)
                                                                ? ":animated-rotation-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.TYPE)
                                                                    ? ":animated-container-duration" : node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.TYPE)
                                                                        ? (name.equals("reverseDurationUs") ? ":animated-size-reverse-duration" : ":animated-size-duration")
                                                                        : (dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.supports(node.type()) || node.type().equals(AnimatedDefaultTextStyleWidgetPropertySchema.TYPE))
                                                                            ? (node.type().equals(AnimatedDefaultTextStyleWidgetPropertySchema.TYPE) ? ":animated-default-text-style-duration" : ":animated-positioned-duration") : ":sliver-animated-opacity-duration"),
                                6, "Duration", "dart:core", valuePath, Optional.of(node.id()))))
                        : name.equals("curve") && value instanceof PropertyValue.StringValue preset
                            ? renderExpansionTilePreset("Curves", preset.value(), valuePath, node.id(), context)
                            : renderProperty(value, property, valuePath, node.id(), context);
                arguments.add(new ConstructorArgument(property.parameter(), name.equals("durationUs") ? "duration"
                        : name.equals("reverseDurationUs") ? "reverseDuration" : name, false, rendered));
            }
        }
        if ((node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.ThemeWidgetPropertySchema.TYPE))
                && node.properties().get(new PropertyName("data")) instanceof PropertyValue.StringValue preset) {
            var property = definition.property(new PropertyName("data")).orElseThrow();
            String valuePath = path + "/properties/data";
            var symbol = context.planner().renderedSymbol(MATERIAL_IMPORT, "ThemeData");
            String factory = preset.value().replace("M2", "");
            String occurrencePrefix = node.type().equals(dev.flutter.netbeans.designer.catalog.ThemeWidgetPropertySchema.TYPE)
                    ? ":theme-" : ":animated-theme-";
            String expression = symbol.text() + "." + factory + (preset.value().endsWith("M2") ? "(useMaterial3: false)" : "()");
            arguments.add(new ConstructorArgument(property.parameter(), "data", false,
                scalar(expression, false, valuePath, node.id(), context, List.of(
                    occurrence("widget:" + node.id() + occurrencePrefix + "data", symbol.nameOffset(), symbol.name(), MATERIAL_IMPORT, valuePath, Optional.of(node.id())),
                    occurrence("widget:" + node.id() + occurrencePrefix + "factory", symbol.text().length()+1, factory, MATERIAL_IMPORT, valuePath, Optional.of(node.id()))))));
        }
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.TYPE)) {
            var property=definition.property(new PropertyName("colorFilter")).orElseThrow();
            String filterPath=path+"/properties/colorFilter";
            var filter=node.properties().get(property.name());
            RenderedValue rendered;
            if(filter instanceof PropertyValue.DartObjectReferenceValue reference) {
                rendered=renderDartObjectReference(reference,"ColorFilter",filterPath,node.id(),context);
            } else {
                String preset=((PropertyValue.StringValue)filter).value();
                if(preset.equals("mode")) {
                    var colorProperty=definition.property(new PropertyName("color")).orElseThrow();
                    var color=node.properties().getOrDefault(colorProperty.name(),new PropertyValue.ColorValue(0));
                    var blendProperty=definition.property(new PropertyName("blendMode")).orElseThrow();
                    var blend=node.properties().getOrDefault(blendProperty.name(),new PropertyValue.EnumValue("BlendMode","srcOver"));
                    rendered=renderPositionalCompositeValues("ColorFilter",Optional.of("mode"),List.of(
                            renderProperty(color,colorProperty,path+"/properties/color",node.id(),context),
                            renderProperty(blend,blendProperty,path+"/properties/blendMode",node.id(),context)),filterPath,node.id(),context);
                } else if(preset.equals("matrix")) {
                    var values=new ArrayList<BigDecimal>();
                    for(int row=0;row<4;row++)for(int col=0;col<5;col++)
                        values.add(dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.number(node,"m"+row+col,row==col?BigDecimal.ONE:BigDecimal.ZERO));
                    rendered=renderDecorationColorFilter(new PropertyValue.DecorationImageValue.Matrix(values),filterPath,node.id(),context);
                } else if(preset.equals("saturation")) {
                    rendered=renderDecorationColorFilter(new PropertyValue.DecorationImageValue.Saturation(
                            dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.number(node,"saturation",BigDecimal.ONE)),filterPath,node.id(),context);
                } else {
                    rendered=renderDecorationColorFilter(preset.equals("linearToSrgbGamma")
                            ? new PropertyValue.DecorationImageValue.LinearToSrgbGamma()
                            : new PropertyValue.DecorationImageValue.SrgbToLinearGamma(),filterPath,node.id(),context);
                }
            }
            arguments.add(new ConstructorArgument(property.parameter(),"colorFilter",false,rendered));
        }
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.FadeInImageWidgetPropertySchema.TYPE)) {
            for (String name : List.of("fadeOutDurationUs", "fadeInDurationUs", "fadeOutCurve", "fadeInCurve")) {
                PropertyValue value = node.properties().get(new PropertyName(name));
                if (value == null) continue;
                var property = definition.property(new PropertyName(name)).orElseThrow();
                String valuePath = path + "/properties/" + name;
                RenderedValue rendered = name.endsWith("Us") && value instanceof PropertyValue.IntegerValue integer
                        ? scalar("const Duration(microseconds: " + integer.value() + ")", true, valuePath, node.id(), context,
                            List.of(occurrence("widget:" + node.id() + ":fade-in-image-" + name,
                                6, "Duration", "dart:core", valuePath, Optional.of(node.id()))))
                        : value instanceof PropertyValue.StringValue preset
                            ? renderExpansionTilePreset("Curves", preset.value(), valuePath, node.id(), context)
                            : renderProperty(value, property, valuePath, node.id(), context);
                arguments.add(new ConstructorArgument(property.parameter(), name.endsWith("Us")
                        ? name.substring(0, name.length() - 2) : name, false, rendered));
            }
        }
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedSwitcherWidgetPropertySchema.TYPE)) {
            for (String name : List.of("durationUs", "reverseDurationUs", "switchInCurve", "switchOutCurve")) {
                PropertyValue value = node.properties().get(new PropertyName(name));
                if (value == null) continue;
                var property = definition.property(new PropertyName(name)).orElseThrow();
                String valuePath = path + "/properties/" + name;
                RenderedValue rendered = name.endsWith("Us") && value instanceof PropertyValue.IntegerValue integer
                        ? scalar("const Duration(microseconds: " + integer.value() + ")", true, valuePath, node.id(), context,
                            List.of(occurrence("widget:" + node.id() + (name.equals("durationUs") ? ":animated-switcher-duration" : ":animated-switcher-reverse-duration"),
                                6, "Duration", "dart:core", valuePath, Optional.of(node.id()))))
                        : name.endsWith("Curve") && value instanceof PropertyValue.StringValue preset
                            ? renderExpansionTilePreset("Curves", preset.value(), valuePath, node.id(), context)
                            : renderProperty(value, property, valuePath, node.id(), context);
                arguments.add(new ConstructorArgument(property.parameter(), name.equals("durationUs") ? "duration"
                        : name.equals("reverseDurationUs") ? "reverseDuration" : name, false, rendered));
            }
        }
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedCrossFadeWidgetPropertySchema.TYPE)) {
            for (String name : List.of("durationUs", "reverseDurationUs", "firstCurve", "secondCurve", "sizeCurve")) {
                PropertyValue value = node.properties().get(new PropertyName(name));
                if (value == null) continue;
                var property = definition.property(new PropertyName(name)).orElseThrow();
                String valuePath = path + "/properties/" + name;
                RenderedValue rendered = name.endsWith("Us") && value instanceof PropertyValue.IntegerValue integer
                        ? scalar("const Duration(microseconds: " + integer.value() + ")", true, valuePath, node.id(), context,
                            List.of(occurrence("widget:" + node.id() + (name.equals("durationUs") ? ":animated-cross-fade-duration" : ":animated-cross-fade-reverse-duration"),
                                6, "Duration", "dart:core", valuePath, Optional.of(node.id()))))
                        : name.endsWith("Curve") && value instanceof PropertyValue.StringValue preset
                            ? renderExpansionTilePreset("Curves", preset.value(), valuePath, node.id(), context)
                            : renderProperty(value, property, valuePath, node.id(), context);
                arguments.add(new ConstructorArgument(property.parameter(), name.equals("durationUs") ? "duration"
                        : name.equals("reverseDurationUs") ? "reverseDuration" : name, false, rendered));
            }
        }
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.TYPE)) {
            for (String name : List.of("rect", "size")) {
                var property = definition.property(new PropertyName(name)).orElseThrow();
                var value = node.properties().get(property.name());
                String valuePath = path + "/properties/" + name;
                RenderedValue rendered;
                if (value instanceof PropertyValue.StringValue preset) {
                    if (preset.value().equals("null")) rendered = scalar("null", true, valuePath, node.id(), context, List.of());
                    else {
                        var fields = name.equals("rect")
                            ? dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.RECT_FIELDS
                            : dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.SIZE_FIELDS;
                        var components = new ArrayList<RenderedValue>();
                        for (String field : fields) components.add(renderProperty(node.properties().get(new PropertyName(field)),
                            definition.property(new PropertyName(field)).orElseThrow(), path + "/properties/" + field, node.id(), context));
                        rendered = renderPositionalCompositeValues(name.equals("rect") ? "Rect" : "Size",
                            name.equals("rect") ? Optional.of("fromLTWH") : Optional.empty(), components, valuePath, node.id(), context);
                    }
                    if (name.equals("rect")) rendered = renderStoppedNullableRect(rendered, valuePath, node.id(), context);
                } else rendered = renderProperty(value, property, valuePath, node.id(), context);
                arguments.add(new ConstructorArgument(property.parameter(), name, false, rendered));
            }
        }
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.TYPE)) {
            var property = definition.property(new PropertyName("rect")).orElseThrow();
            var value = node.properties().get(property.name());
            String rectPath = path + "/properties/rect";
            RenderedValue rect;
            if (value instanceof PropertyValue.StringValue) {
                var components = new ArrayList<RenderedValue>();
                for (String name : dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.EDGES)
                    components.add(renderProperty(node.properties().get(new PropertyName(name)),
                        definition.property(new PropertyName(name)).orElseThrow(), path + "/properties/" + name, node.id(), context));
                rect = renderStoppedRelativeRect(renderPositionalCompositeValues("RelativeRect", Optional.of("fromLTRB"),
                    components, rectPath, node.id(), context), rectPath, node.id(), context);
            } else rect = renderProperty(value, property, rectPath, node.id(), context);
            arguments.add(new ConstructorArgument(property.parameter(), "rect", false, rect));
        }
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.RECT_TYPE)) {
            var property = definition.property(new PropertyName("rect")).orElseThrow();
            var value = node.properties().get(property.name());
            String rectPath = path + "/properties/rect";
            RenderedValue rect;
            if (value instanceof PropertyValue.StringValue) {
                var components = new ArrayList<RenderedValue>();
                for (String name : List.of("rectLeft", "rectTop", "rectWidth", "rectHeight"))
                    components.add(renderProperty(node.properties().get(new PropertyName(name)),
                        definition.property(new PropertyName(name)).orElseThrow(),
                        path + "/properties/" + name, node.id(), context));
                rect = renderPositionalCompositeValues("Rect", Optional.of("fromLTWH"), components, rectPath, node.id(), context);
            } else rect = renderProperty(value, property, rectPath, node.id(), context);
            arguments.add(new ConstructorArgument(property.parameter(), "rect", false, rect));
        }
        if (node.type().equals(MaterialWidgetPropertySchema.MATERIAL_TYPE)) {
            appendMaterialArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(NavigationRailWidgetPropertySchema.NAVIGATION_RAIL_TYPE)) {
            appendNavigationRailArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(NavigationDrawerWidgetPropertySchema.NAVIGATION_DRAWER_TYPE)) {
            appendNavigationDrawerArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE)) {
            appendSubmenuButtonArguments(node, definition, path, constructorBaseIndent + 2, context, arguments);
        }
        if (node.type().equals(ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE)
                || OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)) {
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
        if (node.type().equals(GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE)
                || node.type().equals(GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE)) {
            appendStaticScrollViewSynthesizedArguments(
                    node, definition, path, context, arguments);
        }
        if (node.type().equals(SingleChildScrollViewWidgetPropertySchema
                .SINGLE_CHILD_SCROLL_VIEW_TYPE)) {
            appendStaticScrollViewPhysics(
                    node, definition, path, context, arguments);
        }
        if (node.type().equals(PageViewWidgetPropertySchema.PAGE_VIEW_TYPE)) {
            appendStaticScrollViewSynthesizedArguments(
                    node, definition, path, context, arguments);
            appendPageViewArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(ListWheelScrollViewWidgetPropertySchema.LIST_WHEEL_SCROLL_VIEW_TYPE)) {
            appendStaticScrollViewPhysics(
                    node, definition, path, context, arguments);
            appendListWheelScrollViewArguments(node, definition, path, context, arguments);
        }
        if (node.type().equals(CustomScrollViewWidgetPropertySchema.CUSTOM_SCROLL_VIEW_TYPE)) {
            appendStaticScrollViewSynthesizedArguments(
                    node, definition, path, context, arguments);
        }
        if (node.type().equals(DefaultSelectionStyleWidgetPropertySchema.DEFAULT_SELECTION_STYLE_TYPE)) {
            appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
        }
        if (node.type().equals(MouseRegionWidgetPropertySchema.MOUSE_REGION_TYPE)
                && node.properties().get(new PropertyName("cursor")) instanceof PropertyValue.StringValue) {
            appendMouseCursorPreset(node, definition, "cursor", path, context, arguments);
        }
        appendStateBindingArgument(node, definition, path, context, arguments);
        if (!node.type().equals(TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE)) {
            appendPropertyStateArguments(node, definition, path, context, arguments);
        }
        arguments.sort(ARGUMENT_ORDER);

        // ClipPath.shape is a static Widget factory, not a named constructor.
        // Even a const ShapeBorder must not make the helper or its ancestors const.
        boolean clipPathShape = node.type().value().equals("flutter.widgets.ClipPath")
                && node.properties().containsKey(new PropertyName("shape"));
        boolean selectionMerge = node.type().equals(DefaultSelectionStyleWidgetPropertySchema.DEFAULT_SELECTION_STYLE_TYPE)
                && new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("merge")));
        boolean iconThemeMerge = node.type().equals(IconThemeWidgetPropertySchema.ICON_THEME_TYPE)
                && new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("merge")));
        boolean constant = !OutlinedButtonWidgetPropertySchema.isIconVariant(node)
                && !BadgeWidgetPropertySchema.isCountMode(node) && !clipPathShape && !selectionMerge && !iconThemeMerge && definition.constConstructor()
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
        ArrayList<GeneratedDartSymbolOccurrence> constructorOccurrences = new ArrayList<>();
        constructorOccurrences.add(classOccurrence);
        if (node.type().equals(NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE)) {
            RenderedSymbol type = notificationTypeSymbol(node, context);
            int typeStart = constructor.length() + 1;
            boolean projectType = node.properties().get(new PropertyName("notificationType")) instanceof PropertyValue.DartObjectReferenceValue;
            constructorOccurrences.add(occurrence(projectType
                            ? "widget:" + node.id() + ":property-reference:" + path + "/properties/notificationType:root"
                            : "widget:" + node.id() + ":notification-type",
                    typeStart + type.nameOffset(), type.name(), type.libraryUri(),
                    path + "/properties/notificationType" + (projectType ? "/rootSymbol" : ""), Optional.of(node.id()),
                    Optional.of(new GeneratedDartStaticTypeRequirement(typeStart, type.text().length(), "Type",
                            Optional.of(type.text()), Optional.of("Notification")))));
            constructor += "<" + type.text() + ">";
        }
        if (node.type().equals(RadioWidgetPropertySchema.RADIO_TYPE)
                || node.type().equals(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE)
                || node.type().equals(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE)
                || dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(node.type())
                || dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(node.type())) {
            RenderedSymbol type = radioTypeSymbol(node, context);
            String selectedType = type.text() + (RadioWidgetPropertySchema.nullableValueType(node) ? "?" : "");
            int typeStart = constructor.length() + 1;
            boolean projectType = node.properties().get(new PropertyName("valueType")) instanceof PropertyValue.DartObjectReferenceValue;
            constructorOccurrences.add(occurrence(projectType
                            ? "widget:" + node.id() + ":property-reference:" + path + "/properties/valueType:root"
                            : "widget:" + node.id() + (node.type().equals(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE)
                                    ? ":radio-group-core-value-type" : dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(node.type())
                                            ? ":value-listenable-core-value-type" : dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(node.type())
                                                    ? ":tween-animation-core-value-type" : ":radio-core-value-type"),
                    typeStart + type.nameOffset(), type.name(), type.libraryUri(),
                    path + "/properties/valueType" + (projectType ? "/rootSymbol" : ""), Optional.of(node.id()),
                    Optional.of(new GeneratedDartStaticTypeRequirement(typeStart, type.text().length(), "Type", Optional.of(selectedType)))));
            constructor += "<" + selectedType + ">";
        }
        if (node.type().equals(FocusWidgetPropertySchema.FOCUS_TYPE) && FocusWidgetPropertySchema.usesExternalNode(node)) {
            constructorOccurrences.add(occurrence("widget:" + node.id() + ":focusConstructor",
                    constructor.length() + 1, "withExternalFocusNode", renderedClass.libraryUri(),
                    path + "/properties/variant", Optional.of(node.id())));
            constructor += ".withExternalFocusNode";
        } else if (node.type().equals(FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE)
                && !FilledButtonWidgetPropertySchema.constructorName(node).isEmpty()) {
            String member = FilledButtonWidgetPropertySchema.constructorName(node);
            constructorOccurrences.add(occurrence("widget:" + node.id() + ":filledButtonConstructor",
                    constructor.length() + 1, member, renderedClass.libraryUri(),
                    path + "/properties/variant", Optional.of(node.id())));
            constructor += "." + member;
        } else if ((node.type().equals(CheckboxWidgetPropertySchema.CHECKBOX_TYPE)
                || node.type().equals(CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE)
                || node.type().equals(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE)
                || node.type().equals(RadioWidgetPropertySchema.RADIO_TYPE)
                || node.type().equals(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE)
                || node.type().equals(SwitchWidgetPropertySchema.SWITCH_TYPE)
                || node.type().equals(SliderWidgetPropertySchema.SLIDER_TYPE))
                && new PropertyValue.StringValue("adaptive").equals(node.properties().get(new PropertyName("variant")))) {
            constructorOccurrences.add(occurrence("widget:" + node.id() + ":checkboxConstructor",
                    constructor.length() + 1, "adaptive", renderedClass.libraryUri(),
                    path + "/properties/variant", Optional.of(node.id())));
            constructor += ".adaptive";
        } else if (node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE)) {
            String member = IconButtonWidgetPropertySchema.constructorName(node);
            if (!member.isEmpty()) {
                constructorOccurrences.add(occurrence("widget:" + node.id() + ":iconButtonConstructor",
                        constructor.length() + 1, member, renderedClass.libraryUri(),
                        path + "/properties/variant", Optional.of(node.id())));
                constructor += "." + member;
            }
        } else if (OutlinedButtonWidgetPropertySchema.isIconVariant(node)) {
            constructorOccurrences.add(occurrence("widget:" + node.id()
                    + (node.type().equals(OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE)
                            ? ":outlinedButtonIconConstructor" : ":textButtonIconConstructor"),
                    constructor.length() + 1, "icon", renderedClass.libraryUri(),
                    path + "/properties/variant", Optional.of(node.id())));
            constructor += ".icon";
        } else if (clipPathShape) {
            constructorOccurrences.add(occurrence(
                    "widget:" + node.id() + ":shapeFactory", constructor.length() + 1,
                    "shape", renderedClass.libraryUri(), path, Optional.of(node.id())));
            constructor += ".shape";
        } else if (node.type().equals(DefaultTextStyleWidgetPropertySchema.MERGE_TYPE)) {
            constructorOccurrences.add(occurrence("widget:" + node.id() + ":defaultTextStyleMergeFactory",
                    constructor.length() + 1, "merge", renderedClass.libraryUri(), path, Optional.of(node.id())));
            constructor += ".merge";
        } else if (selectionMerge || iconThemeMerge) {
            constructorOccurrences.add(occurrence(
                    "widget:" + node.id() + (iconThemeMerge ? ":iconThemeMergeFactory" : ":selectionMergeFactory"), constructor.length() + 1,
                    "merge", renderedClass.libraryUri(), path + "/properties/merge", Optional.of(node.id())));
            constructor += ".merge";
        } else if (BadgeWidgetPropertySchema.isCountMode(node)) {
            constructorOccurrences.add(occurrence("widget:" + node.id() + ":badgeCountConstructor", constructor.length() + 1,
                    "count", renderedClass.libraryUri(), path + "/properties/count", Optional.of(node.id())));
            constructor += ".count";
        } else if (node.type().equals(RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE)) {
            String variant = ((PropertyValue.StringValue) node.properties().get(new PropertyName("variant"))).value();
            if (!variant.equals("material")) {
                constructorOccurrences.add(occurrence("widget:" + node.id() + ":refreshVariantConstructor",
                        constructor.length() + 1, variant, renderedClass.libraryUri(),
                        path + "/properties/variant", Optional.of(node.id())));
                constructor += "." + variant;
            }
        } else if (node.type().equals(CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE)) {
            String variant = ((PropertyValue.StringValue) node.properties().get(new PropertyName("variant"))).value();
            if (variant.equals("adaptive")) {
                constructorOccurrences.add(occurrence("widget:" + node.id() + ":circularAdaptiveConstructor",
                        constructor.length() + 1, "adaptive", renderedClass.libraryUri(),
                        path + "/properties/variant", Optional.of(node.id())));
                constructor += ".adaptive";
            }
        } else if (node.type().equals(CardWidgetPropertySchema.CARD_TYPE)) {
            String variant = ((PropertyValue.StringValue) node.properties().get(new PropertyName("variant"))).value();
            if (!variant.equals("elevated")) {
                constructorOccurrences.add(occurrence("widget:" + node.id() + ":cardVariant", constructor.length() + 1,
                        variant, renderedClass.libraryUri(), path + "/properties/variant", Optional.of(node.id())));
                constructor += "." + variant;
            }
        } else if (node.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE)) {
            String variant = FloatingActionButtonWidgetPropertySchema.variant(node);
            if (!variant.equals("standard")) {
                constructorOccurrences.add(occurrence("widget:" + node.id() + ":floatingActionButtonConstructor",
                        constructor.length() + 1, variant, renderedClass.libraryUri(),
                        path + "/properties/variant", Optional.of(node.id())));
                constructor += "." + variant;
            }
        } else if (definition.namedConstructor().isPresent()) {
            constructor += "." + definition.namedConstructor().orElseThrow();
        }
        if (arguments.isEmpty()) {
            RenderedValue rendered = scalar(
                    constructor + "()",
                    constant,
                    path,
                    node.id(),
                    context,
                    constructorOccurrences);
            return wrapConstraintGuardIfNeeded(
                    node, path, baseIndent, context, rendered);
        }

        if (arguments.stream().noneMatch(ConstructorArgument::slot)
                && arguments.stream().allMatch(value -> value.value().lines().size() == 1)) {
            StringBuilder inline = new StringBuilder(constructor).append('(');
            ArrayList<GeneratedDartSymbolOccurrence> inlineOccurrences =
                    new ArrayList<>();
            inlineOccurrences.addAll(constructorOccurrences);
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
        lines.add(constructor + "(", constructorOccurrences);
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

    private RenderedValue renderAnimatedIconPreset(String member,String path,StableId widgetId,GenerationContext context) {
        var symbol=context.planner().renderedSymbol(MATERIAL_IMPORT,"AnimatedIcons");
        return scalar(symbol.text()+'.'+member,true,path,widgetId,context,List.of(
                occurrence("widget:"+widgetId+":animated-icon-data",symbol.nameOffset(),symbol.name(),symbol.libraryUri(),path,Optional.of(widgetId)),
                occurrence("widget:"+widgetId+":animated-icon-member",symbol.text().length()+1,member,symbol.libraryUri(),path,Optional.of(widgetId))));
    }

    private RenderedValue renderStoppedTurns(
            PropertyValue value, String path, StableId widgetId, GenerationContext context) {
        var number = value instanceof PropertyValue.DoubleValue decimal ? decimal.value()
                : new java.math.BigDecimal(((PropertyValue.IntegerValue) value).value());
        var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        return scalar("const " + symbol.text() + "<double>(" + dartDouble(number) + ")", true, path, widgetId, context,
                List.of(occurrence("widget:" + widgetId + ":stopped-turns-animation", 6 + symbol.nameOffset(),
                        symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId))));
    }

    private RenderedValue renderStoppedSizeFactor(
            PropertyValue value, String path, StableId widgetId, GenerationContext context) {
        var number = value instanceof PropertyValue.DoubleValue decimal ? decimal.value()
                : new java.math.BigDecimal(((PropertyValue.IntegerValue) value).value());
        var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        return scalar("const " + symbol.text() + "<double>(" + dartDouble(number) + ")", true, path, widgetId, context,
                List.of(occurrence("widget:" + widgetId + ":stopped-size-animation", 6 + symbol.nameOffset(),
                        symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId))));
    }

    private RenderedValue renderStoppedScale(
            PropertyValue value, String path, StableId widgetId, GenerationContext context) {
        var number = value instanceof PropertyValue.DoubleValue decimal ? decimal.value()
                : new java.math.BigDecimal(((PropertyValue.IntegerValue) value).value());
        var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        return scalar("const " + symbol.text() + "<double>(" + dartDouble(number) + ")", true, path, widgetId, context,
                List.of(occurrence("widget:" + widgetId + ":stopped-scale-animation", 6 + symbol.nameOffset(),
                        symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId))));
    }

    private RenderedValue renderStoppedMatrixAnimation(
            PropertyValue value, String path, StableId widgetId, GenerationContext context) {
        var number = value instanceof PropertyValue.DoubleValue decimal ? decimal.value()
                : new java.math.BigDecimal(((PropertyValue.IntegerValue) value).value());
        var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        return scalar("const " + symbol.text() + "<double>(" + dartDouble(number) + ")", true, path, widgetId, context,
                List.of(occurrence("widget:" + widgetId + ":stopped-matrix-animation", 6 + symbol.nameOffset(),
                        symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId))));
    }

    private RenderedValue renderMatrixCallback(
            PropertyValue.Matrix4Value matrix, String path, StableId widgetId, GenerationContext context) {
        var value = renderMatrix4(matrix, path, widgetId, context);
        String prefix = "(double animationValue) => ";
        return scalar(prefix + value.joined(), false, path, widgetId, context,
                value.symbolOccurrences().stream().map(symbol -> symbol.shifted(prefix.length())).toList());
    }

    private RenderedValue renderStoppedOpacity(
            PropertyValue value, String path, StableId widgetId, GenerationContext context) {
        var number = value instanceof PropertyValue.DoubleValue decimal ? decimal.value()
                : new java.math.BigDecimal(((PropertyValue.IntegerValue) value).value());
        var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        return scalar("const " + symbol.text() + "<double>(" + dartDouble(number) + ")", true, path, widgetId, context,
                List.of(occurrence("widget:" + widgetId + ":stopped-opacity-animation", 6 + symbol.nameOffset(),
                        symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId))));
    }

    private RenderedValue renderStoppedRelativeRect(
            RenderedValue offset, String path, StableId widgetId, GenerationContext context) {
        RenderedSymbol animation = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, "RelativeRect");
        String constant = offset.constant() ? "const " : "";
        String prefix = constant + animation.text() + "<" + type.text() + ">(";
        var symbols = new ArrayList<GeneratedDartSymbolOccurrence>();
        symbols.add(occurrence("widget:" + widgetId + ":stopped-relative-rect-animation",
                constant.length() + animation.nameOffset(), animation.name(), animation.libraryUri(), path, Optional.of(widgetId)));
        symbols.add(occurrence("widget:" + widgetId + ":stopped-relative-rect-type",
                constant.length() + animation.text().length() + 1 + type.nameOffset(), type.name(), type.libraryUri(), path, Optional.of(widgetId)));
        offset.symbolOccurrences().forEach(symbol -> symbols.add(symbol.shifted(prefix.length())));
        var rendered = scalar(prefix + offset.joined() + ")", offset.constant(), path, widgetId, context, symbols);
        return new RenderedValue(java.util.Arrays.asList(rendered.joined().split("\\n", -1)),
                rendered.constant(), rendered.utf8Size(), rendered.symbolOccurrences());
    }


    private RenderedValue renderStoppedDecoration(
            RenderedValue offset, String path, StableId widgetId, GenerationContext context) {
        RenderedSymbol animation = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, "Decoration");
        String constant = offset.constant() ? "const " : "";
        String prefix = constant + animation.text() + "<" + type.text() + ">(";
        var symbols = new ArrayList<GeneratedDartSymbolOccurrence>();
        symbols.add(occurrence("widget:" + widgetId + ":stopped-decoration-animation",
                constant.length() + animation.nameOffset(), animation.name(), animation.libraryUri(), path, Optional.of(widgetId)));
        symbols.add(occurrence("widget:" + widgetId + ":stopped-decoration-type",
                constant.length() + animation.text().length() + 1 + type.nameOffset(), type.name(), type.libraryUri(), path, Optional.of(widgetId)));
        offset.symbolOccurrences().forEach(symbol -> symbols.add(symbol.shifted(prefix.length())));
        var rendered = scalar(prefix + offset.joined() + ")", offset.constant(), path, widgetId, context, symbols);
        return new RenderedValue(java.util.Arrays.asList(rendered.joined().split("\\n", -1)),
                rendered.constant(), rendered.utf8Size(), rendered.symbolOccurrences());
    }


    private RenderedValue renderStoppedAlignment(
            RenderedValue alignment, String path, StableId widgetId, GenerationContext context) {
        RenderedSymbol animation = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlignmentGeometry");
        String constant = alignment.constant() ? "const " : "";
        String prefix = constant + animation.text() + "<" + type.text() + ">(";
        var symbols = new ArrayList<GeneratedDartSymbolOccurrence>();
        symbols.add(occurrence("widget:" + widgetId + ":stopped-alignment-animation",
                constant.length() + animation.nameOffset(), animation.name(), animation.libraryUri(), path, Optional.of(widgetId)));
        symbols.add(occurrence("widget:" + widgetId + ":stopped-alignment-type",
                constant.length() + animation.text().length() + 1 + type.nameOffset(), type.name(), type.libraryUri(), path, Optional.of(widgetId)));
        alignment.symbolOccurrences().forEach(symbol -> symbols.add(symbol.shifted(prefix.length())));
        var rendered = scalar(prefix + alignment.joined() + ")", alignment.constant(), path, widgetId, context, symbols);
        return new RenderedValue(java.util.Arrays.asList(rendered.joined().split("\\n", -1)),
                rendered.constant(), rendered.utf8Size(), rendered.symbolOccurrences());
    }


    private RenderedValue renderStoppedNullableRect(
            RenderedValue offset, String path, StableId widgetId, GenerationContext context) {
        RenderedSymbol animation = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, "Rect");
        String constant = offset.constant() ? "const " : "";
        String prefix = constant + animation.text() + "<" + type.text() + "?>(";
        var symbols = new ArrayList<GeneratedDartSymbolOccurrence>();
        symbols.add(occurrence("widget:" + widgetId + ":stopped-nullable-rect-animation",
                constant.length() + animation.nameOffset(), animation.name(), animation.libraryUri(), path, Optional.of(widgetId)));
        symbols.add(occurrence("widget:" + widgetId + ":stopped-nullable-rect-type",
                constant.length() + animation.text().length() + 1 + type.nameOffset(), type.name(), type.libraryUri(), path, Optional.of(widgetId)));
        offset.symbolOccurrences().forEach(symbol -> symbols.add(symbol.shifted(prefix.length())));
        var rendered = scalar(prefix + offset.joined() + ")", offset.constant(), path, widgetId, context, symbols);
        return new RenderedValue(java.util.Arrays.asList(rendered.joined().split("\\n", -1)),
                rendered.constant(), rendered.utf8Size(), rendered.symbolOccurrences());
    }


    private RenderedValue renderStoppedOffset(
            RenderedValue offset, String path, StableId widgetId, GenerationContext context) {
        RenderedSymbol animation = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, "Offset");
        String constant = offset.constant() ? "const " : "";
        String prefix = constant + animation.text() + "<" + type.text() + ">(";
        var symbols = new ArrayList<GeneratedDartSymbolOccurrence>();
        symbols.add(occurrence("widget:" + widgetId + ":stopped-offset-animation",
                constant.length() + animation.nameOffset(), animation.name(), animation.libraryUri(), path, Optional.of(widgetId)));
        symbols.add(occurrence("widget:" + widgetId + ":stopped-offset-type",
                constant.length() + animation.text().length() + 1 + type.nameOffset(), type.name(), type.libraryUri(), path, Optional.of(widgetId)));
        offset.symbolOccurrences().forEach(symbol -> symbols.add(symbol.shifted(prefix.length())));
        var rendered = scalar(prefix + offset.joined() + ")", offset.constant(), path, widgetId, context, symbols);
        return new RenderedValue(java.util.Arrays.asList(rendered.joined().split("\\n", -1)),
                rendered.constant(), rendered.utf8Size(), rendered.symbolOccurrences());
    }

    private RenderedValue renderStoppedTextStyle(
            RenderedValue style, String path, StableId widgetId, GenerationContext context) {
        RenderedSymbol animation = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, "TextStyle");
        String constant = style.constant() ? "const " : "";
        String prefix = constant + animation.text() + "<" + type.text() + ">(";
        var symbols = new ArrayList<GeneratedDartSymbolOccurrence>();
        symbols.add(occurrence("widget:" + widgetId + ":stopped-text-style-animation",
                constant.length() + animation.nameOffset(), animation.name(), animation.libraryUri(), path, Optional.of(widgetId)));
        symbols.add(occurrence("widget:" + widgetId + ":stopped-text-style-type",
                constant.length() + animation.text().length() + 1 + type.nameOffset(), type.name(), type.libraryUri(), path, Optional.of(widgetId)));
        style.symbolOccurrences().forEach(symbol -> symbols.add(symbol.shifted(prefix.length())));
        var rendered = scalar(prefix + style.joined() + ")", style.constant(), path, widgetId, context, symbols);
        return new RenderedValue(java.util.Arrays.asList(rendered.joined().split("\\n", -1)),
                rendered.constant(), rendered.utf8Size(), rendered.symbolOccurrences());
    }

    private RenderedValue renderProgressValueColor(
            PropertyValue value, PropertyDefinition property, String path, StableId widgetId,
            GenerationContext context) {
        RenderedValue color = renderProperty(value, property, path, widgetId, context);
        RenderedSymbol animation = context.planner().renderedSymbol(WIDGETS_IMPORT, "AlwaysStoppedAnimation");
        RenderedSymbol colorType = context.planner().renderedSymbol(WIDGETS_IMPORT, "Color");
        String constant = color.constant() ? "const " : "";
        String prefix = constant + animation.text() + "<" + colorType.text()
                + (value instanceof PropertyValue.NullValue ? "?" : "") + ">(";
        List<GeneratedDartSymbolOccurrence> symbols = new ArrayList<>();
        symbols.add(occurrence("widget:" + widgetId + ":progress-value-animation",
                constant.length() + animation.nameOffset(), animation.name(), animation.libraryUri(), path, Optional.of(widgetId)));
        symbols.add(occurrence("widget:" + widgetId + ":progress-value-color-type",
                constant.length() + animation.text().length() + 1 + colorType.nameOffset(),
                colorType.name(), colorType.libraryUri(), path, Optional.of(widgetId)));
        color.symbolOccurrences().forEach(symbol -> symbols.add(symbol.shifted(prefix.length())));
        return scalar(prefix + color.joined() + ")", color.constant(), path, widgetId, context, symbols);
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

    private static List<ElevatedButtonState> buttonStates(WidgetNode node) {
        if (!OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)
                && !node.type().equals(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE)
                && !node.type().equals(MenuBarWidgetPropertySchema.MENU_BAR_TYPE)) {
            return ELEVATED_BUTTON_STATES;
        }
        return TextButtonWidgetPropertySchema.statePriority().stream()
                .map(state -> new ElevatedButtonState(state.equals("any") ? "style"
                        : "style" + Character.toUpperCase(state.charAt(0)) + state.substring(1), state))
                .toList();
    }

    private static List<String> enabledButtonStates(Map<String, ?> layers) {
        return layers.keySet().stream()
                .filter(state -> !state.equals("disabled") && !state.equals("any")).toList();
    }

    private void appendSubmenuButtonArguments(WidgetNode node, WidgetDefinition definition,
            String path, int valueIndent, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyName delayName = new PropertyName("hoverOpenDelayUs");
        PropertyValue delay = node.properties().get(delayName);
        if (delay != null) {
            String delayPath = path + "/properties/hoverOpenDelayUs";
            RenderedValue rendered = delay instanceof PropertyValue.IntegerValue integer
                    ? scalar("const Duration(microseconds: " + integer.value() + ")", true, delayPath, node.id(), context,
                            List.of(occurrence("widget:" + node.id() + ":submenu-button-core-duration:hoverOpenDelayUs",
                                    6, "Duration", "dart:core", delayPath, Optional.of(node.id()))))
                    : renderProperty(delay, definition.property(delayName).orElseThrow(), delayPath, node.id(), context);
            arguments.add(new ConstructorArgument(definition.property(delayName).orElseThrow().parameter(), "hoverOpenDelay", false, rendered));
        }
        if (!node.properties().containsKey(new PropertyName("menuStyle"))) {
            WidgetNode projection = SubmenuButtonWidgetPropertySchema.menuStyleProjection(node);
            ArrayList<ConstructorArgument> projected = new ArrayList<>();
            ButtonFamily previous = context.buttonFamilies().put(node.id(),
                    new ButtonFamily("SubmenuButtonMenuStyle", "MenuTheme", false, false, "", projection));
            try {
                appendMenuAnchorStyleArguments(projection, context.catalog().find(projection.type()).orElseThrow(),
                        path, valueIndent, context, projected);
            } finally {
                if (previous == null) context.buttonFamilies().remove(node.id());
                else context.buttonFamilies().put(node.id(), previous);
            }
            for (ConstructorArgument argument : projected) {
                RenderedValue source = argument.value();
                RenderedValue namespaced = new RenderedValue(source.lines(), source.constant(), source.utf8Size(),
                        source.symbolOccurrences().stream().map(symbol -> new GeneratedDartSymbolOccurrence(
                                symbol.id().replace("widget:" + node.id() + ":", "widget:" + node.id() + ":submenu-menu-style:")
                                        .replace(path + "/properties/style", path + "/properties/menuStyle"),
                                symbol.region(), symbol.offset(), symbol.length(), symbol.symbolName(), symbol.libraryUri(),
                                symbol.modelPath().replace(path + "/properties/style", path + "/properties/menuStyle"),
                                symbol.widgetId(), symbol.staticTypeRequirement())).toList());
                arguments.add(new ConstructorArgument(definition.property(new PropertyName("menuStyle")).orElseThrow().parameter(),
                        "menuStyle", false, namespaced));
            }
        }
        if (!node.properties().containsKey(new PropertyName("submenuIcon"))) {
            ArrayList<ElevatedButtonStateEntry> entries = new ArrayList<>();
            for (String state : List.of("Disabled", "Hovered", "Focused", "Default")) {
                PropertyName name = new PropertyName("submenuIcon" + state);
                PropertyValue value = node.properties().get(name);
                if (value == null) continue;
                String valuePath = path + "/properties/" + name.value();
                RenderedValue rendered = renderProperty(value, definition.property(name).orElseThrow(), valuePath, node.id(), context);
                if (value instanceof PropertyValue.IconDataValue) {
                    StringBuilder icon = new StringBuilder("const ");
                    ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
                    appendElevatedSymbol(icon, occurrences, context.planner().renderedSymbol(WIDGETS_IMPORT, "Icon"),
                            "widget:" + node.id() + ":submenu-icon:" + state, valuePath, node.id());
                    icon.append('('); appendRendered(icon, occurrences, rendered); icon.append(')');
                    rendered = scalar(icon.toString(), true, valuePath, node.id(), context, occurrences);
                }
                rendered = scopedIconButtonValue(rendered, name.value());
                entries.add(new ElevatedButtonStateEntry(state.equals("Default") ? "any" : state.toLowerCase(java.util.Locale.ROOT), rendered));
            }
            if (!entries.isEmpty()) arguments.add(new ConstructorArgument(
                    definition.property(new PropertyName("submenuIcon")).orElseThrow().parameter(), "submenuIcon", false,
                    renderCheckboxStateMap("WidgetStateProperty", "Widget", entries,
                            path + "/properties/submenuIcon", node.id(), context)));
        }
    }

    private RenderedValue submenuMenuPaddingFallback(RenderedValue local, String path,
            StableId id, GenerationContext context) {
        StringBuilder result = new StringBuilder();
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        String scope = "widget:" + id + ":submenu-menu-padding:";
        appendElevatedSymbol(result, occurrences, context.planner().renderedSymbol(MATERIAL_IMPORT, "WidgetStateProperty"), scope + "property", path, id);
        result.append(".resolveWith<");
        appendElevatedSymbol(result, occurrences, context.planner().renderedSymbol(WIDGETS_IMPORT, "EdgeInsetsGeometry"), scope + "type", path, id);
        result.append("?>((states) => ("); appendRendered(result, occurrences, local);
        result.append(").resolve(states) ?? ");
        appendElevatedSymbol(result, occurrences, context.planner().renderedSymbol(MATERIAL_IMPORT, "MenuTheme"), scope + "theme", path, id);
        result.append(".of(context).style?.padding?.resolve(states) ?? const ");
        appendElevatedSymbol(result, occurrences, context.planner().renderedSymbol(WIDGETS_IMPORT, "EdgeInsetsDirectional"), scope + "default", path, id);
        result.append(".symmetric(vertical: 8.0))");
        return scalar(result.toString(), false, path, id, context, occurrences);
    }

    private void appendMenuAnchorStyleArguments(WidgetNode node, WidgetDefinition definition,
            String path, int valueIndent, GenerationContext context, List<ConstructorArgument> arguments) {
        if (node.properties().containsKey(new PropertyName("style"))) return;
        LinkedHashMap<String, Map<String, RenderedValue>> states = new LinkedHashMap<>();
        for (ElevatedButtonState state : buttonStates(node)) {
            LinkedHashMap<String, RenderedValue> values = new LinkedHashMap<>();
            for (String field : List.of("BackgroundColor", "ShadowColor", "SurfaceTintColor", "Elevation", "Padding")) {
                putElevatedScalar(node, definition, state.prefix() + field,
                        Character.toLowerCase(field.charAt(0)) + field.substring(1), path, context, values);
            }
            putIfPresent(values, "mouseCursor", renderElevatedCursor(node, definition, state.prefix() + "MouseCursor", path, context));
            states.put(state.key(), Map.copyOf(values));
        }
        ArrayList<CompositeMember> style = new ArrayList<>();
        int order = 0;
        for (String field : List.of("backgroundColor", "shadowColor", "surfaceTintColor", "elevation", "padding", "mouseCursor")) {
            String type = switch (field) {
                case "elevation" -> "double"; case "padding" -> "EdgeInsetsGeometry";
                case "mouseCursor" -> "MouseCursor"; default -> "Color";
            };
            appendElevatedStateProperty(style, states, field, type, order++, path, node.id(), context);
        }
        if (context.buttonFamily(node.id()).className().equals("SubmenuButtonMenuStyle")) {
            for (int i = 0; i < style.size(); i++) {
                CompositeMember member = style.get(i);
                if (member.name().equals("padding")) style.set(i, new CompositeMember(member.name(), member.order(),
                        submenuMenuPaddingFallback(member.rendered(), path + "/properties/stylePadding", node.id(), context)));
            }
        }
        ElevatedButtonBoundSizePair sizes = renderElevatedBoundSizeStateProperties(node, definition, path, context);
        if (sizes != null) {
            style.add(new CompositeMember("minimumSize", 6, sizes.minimum()));
            style.add(new CompositeMember("maximumSize", 8, sizes.maximum()));
        }
        RenderedValue fixed = renderElevatedSizeStateProperty(node, definition, "Fixed", path, context);
        if (fixed != null) style.add(new CompositeMember("fixedSize", 7, fixed));
        RenderedValue side = renderElevatedSideStateProperty(node, definition, path, context);
        if (side != null) style.add(new CompositeMember("side", 9, side));
        RenderedValue shape = renderElevatedShapeStateProperty(node, definition, path, context);
        if (shape != null) style.add(new CompositeMember("shape", 10, shape));
        if (node.properties().containsKey(new PropertyName("styleVisualDensityHorizontal"))
                || node.properties().containsKey(new PropertyName("styleVisualDensityVertical"))) {
            ArrayList<CompositeMember> density = new ArrayList<>();
            for (String axis : List.of("Horizontal", "Vertical")) {
                PropertyDefinition property = definition.property(new PropertyName("styleVisualDensity" + axis)).orElseThrow();
                if (!node.properties().containsKey(property.name())) continue;
                density.add(new CompositeMember(axis.toLowerCase(java.util.Locale.ROOT), density.size(),
                        renderProperty(node.properties().get(property.name()), property,
                                path + "/properties/" + property.name().value(), node.id(), context)));
            }
            style.add(new CompositeMember("visualDensity", 11, renderNamedCompositeMembers(MATERIAL_IMPORT,
                    "VisualDensity", Optional.empty(), density, valueIndent + 2,
                    path + "/properties/styleVisualDensity", node.id(), context)));
        }
        RenderedValue alignment = renderElevatedAlignment(node, definition, path, valueIndent + 2, context);
        if (alignment != null) style.add(new CompositeMember("alignment", 12, alignment));
        if (style.isEmpty()) return;
        style.sort(COMPOSITE_MEMBER_ORDER);
        arguments.add(new ConstructorArgument(definition.property(new PropertyName("style")).orElseThrow().parameter(),
                "style", false, renderNamedCompositeMembers(MATERIAL_IMPORT, "MenuStyle", Optional.empty(), style,
                        valueIndent, path + "/properties/style", node.id(), context)));
    }

    private void appendElevatedButtonCompoundArguments(
            WidgetNode node,
            WidgetDefinition definition,
            String path,
            int valueIndent,
            GenerationContext context,
            List<ConstructorArgument> arguments) {
        appendElevatedButtonCallbacks(node, definition, path, context, arguments);

        // A project ButtonStyle is emitted by the ordinary direct-property path.
        // It is mutually exclusive with every local style leaf.
        if (OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)
                && node.properties().containsKey(new PropertyName("style"))) return;

        LinkedHashMap<String, Map<String, RenderedValue>> states = new LinkedHashMap<>();
        for (ElevatedButtonState state : buttonStates(node)) {
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
                    node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE)
                            || node.type().equals(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE)
                            || node.type().equals(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE)
                            ? definition.property(new PropertyName("style")).orElseThrow().parameter() : DartParameter.named(4, false),
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
        if (node.type().equals(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE)) return;
        if (node.type().equals(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE)) {
            appendMenuItemButtonArguments(node, definition, path, context, arguments);
            return;
        }
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
        } else if (longValue != null && !node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE)) {
            pressed = scalar("null", true,
                    path + "/properties/onPressed", node.id(), context);
        } else {
            pressed = scalar("() {}", false,
                    path + "/properties/onPressed", node.id(), context);
        }
        arguments.add(new ConstructorArgument(
                node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE) ? onPressed.parameter() : DartParameter.named(0, true), "onPressed", false, pressed));

        if (!enabled) {
            arguments.add(new ConstructorArgument(
                    node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE) ? onLongPress.parameter() : DartParameter.named(1, false), "onLongPress", false,
                    scalar("null", true,
                            path + "/properties/enabled", node.id(), context)));
        } else if (longValue != null) {
            arguments.add(new ConstructorArgument(
                    node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE) ? onLongPress.parameter() : DartParameter.named(1, false), "onLongPress", false,
                    renderProperty(
                            longValue, onLongPress,
                            path + "/properties/onLongPress", node.id(), context)));
        }

    }

    private void appendMenuItemButtonArguments(WidgetNode node, WidgetDefinition definition,
            String path, GenerationContext context, List<ConstructorArgument> arguments) {
        boolean enabled = node.propertyBindings().containsKey(new PropertyName("enabled"))
                || !new PropertyValue.BooleanValue(false).equals(node.properties().get(new PropertyName("enabled")));
        PropertyDefinition pressedProperty = definition.property(new PropertyName("onPressed")).orElseThrow();
        PropertyValue pressedValue = node.properties().get(pressedProperty.name());
        RenderedValue pressed = !enabled ? scalar("null", true, path + "/properties/enabled", node.id(), context)
                : pressedValue == null ? scalar("() {}", false, path + "/properties/onPressed", node.id(), context)
                : renderProperty(pressedValue, pressedProperty, path + "/properties/onPressed", node.id(), context);
        arguments.add(new ConstructorArgument(pressedProperty.parameter(), "onPressed", false, pressed));
        String anchor = node.properties().containsKey(new PropertyName("shortcutTrigger")) ? "shortcutTrigger"
                : node.properties().containsKey(new PropertyName("shortcutCharacter")) ? "shortcutCharacter" : null;
        if (anchor == null) return;
        String shortcutPath = path + "/properties/" + anchor;
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT,
                anchor.equals("shortcutTrigger") ? "SingleActivator" : "CharacterActivator");
        StringBuilder text = new StringBuilder("const ").append(type.text()).append('(');
        var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
        occurrences.add(occurrence("widget:" + node.id() + ":menu-shortcut:" + type.name(),
                6 + type.nameOffset(), type.name(), type.libraryUri(), shortcutPath, Optional.of(node.id())));
        appendRendered(text, occurrences, renderProperty(node.properties().get(new PropertyName(anchor)),
                definition.property(new PropertyName(anchor)).orElseThrow(), shortcutPath, node.id(), context));
        for (String local : MenuItemButtonWidgetPropertySchema.shortcutLocalProperties()) {
            if (local.equals("shortcutTrigger") || local.equals("shortcutCharacter")) continue;
            PropertyValue value = node.properties().get(new PropertyName(local));
            if (value == null) continue;
            String argument = Character.toLowerCase(local.charAt(8)) + local.substring(9);
            text.append(", ").append(argument).append(": ");
            appendRendered(text, occurrences, renderProperty(value,
                    definition.property(new PropertyName(local)).orElseThrow(),
                    path + "/properties/" + local, node.id(), context));
        }
        text.append(')');
        arguments.add(new ConstructorArgument(definition.property(new PropertyName("shortcut")).orElseThrow().parameter(),
                "shortcut", false, scalar(text.toString(), true, shortcutPath, node.id(), context, occurrences)));
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
        RenderedValue disabledValue = states.get("disabled").get(dartName);
        RenderedValue fallbackValue = states.get("any").get(dartName);
        if (states.values().stream().noneMatch(values -> values.containsKey(dartName))) {
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
        for (String state : enabledButtonStates(states)) {
            RenderedValue value = states.get(state).get(dartName);
            if (value != null) {
                entries.add(new ElevatedButtonStateEntry(state, value));
            }
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
        List<String> configuredStates = enabledButtonStates(layers).reversed()
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
        for (String state : enabledButtonStates(layers).reversed()) {
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
        for (String state : enabledButtonStates(layers)) {
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
        for (ElevatedButtonState state : buttonStates(node)) {
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
        ButtonFamily family = context.buttonFamily(widgetId);
        if (family.className().equals("IconButton")
                && (field.equals("minimumSize") || field.equals("maximumSize"))
                && family.node().properties().get(new PropertyName("constraints")) != null) {
            WidgetDefinition definition = context.catalog().find(family.node().type()).orElseThrow();
            PropertyDefinition property = definition.property(new PropertyName("constraints")).orElseThrow();
            String directPath = path.substring(0, path.indexOf("/properties/")) + "/properties/constraints";
            RenderedValue value = renderProperty(family.node().properties().get(property.name()),
                    property, directPath, widgetId, context);
            RenderedSymbol size = context.planner().renderedSymbol(WIDGETS_IMPORT, "Size");
            appendElevatedSymbol(rendered, occurrences, size,
                    "widget:" + widgetId + ":icon-button-direct-size:" + field + ':' + path, path, widgetId);
            rendered.append("((");
            appendRendered(rendered, occurrences, scopedIconButtonValue(value, path + ':' + field + ":width"));
            rendered.append(field.equals("minimumSize") ? ").minWidth, (" : ").maxWidth, (");
            value = renderProperty(family.node().properties().get(property.name()), property, directPath, widgetId, context);
            appendRendered(rendered, occurrences, scopedIconButtonValue(value, path + ':' + field + ":height"));
            rendered.append(field.equals("minimumSize") ? ").minHeight)" : ").maxHeight)");
            return;
        }
        RenderedSymbol themeType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, context.buttonFamily(widgetId).themeName());
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
        if (Set.of("MenuAnchor", "MenuBar", "SubmenuButtonMenuStyle").contains(context.buttonFamily(widgetId).className())) {
            // MenuAnchor has no public defaultStyleOf. These are the only native
            // _MenuDefaultsM3 leaves consulted by sparse size/side/shape lowering:
            // sizes and side remain null, and the panel shape has radius four.
            String source = "  const $MenuStyle frameworkDefaults = $MenuStyle(\n"
                    + "    shape: $WidgetStatePropertyAll<$OutlinedBorder>($RoundedRectangleBorder(\n"
                    + "      borderRadius: $BorderRadius.all($Radius.circular(4.0)))),\n"
                    + "  );\n";
            java.util.regex.Matcher symbols = java.util.regex.Pattern.compile("\\$([A-Za-z][A-Za-z0-9]*)").matcher(source);
            int previous = 0;
            int index = 0;
            while (symbols.find()) {
                rendered.append(source, previous, symbols.start());
                String name = symbols.group(1);
                String library = Set.of("MenuStyle", "WidgetStatePropertyAll").contains(name) ? MATERIAL_IMPORT : WIDGETS_IMPORT;
                appendElevatedSymbol(rendered, occurrences, context.planner().renderedSymbol(library, name),
                        "widget:" + widgetId + ":menu-anchor-default:" + scope + ':' + index++ + ':' + path, path, widgetId);
                previous = symbols.end();
            }
            rendered.append(source, previous, source.length());
            return;
        }
        if (context.buttonFamily(widgetId).className().equals("SubmenuButton")) {
            RenderedSymbol styleType = context.planner().renderedSymbol(MATERIAL_IMPORT, "ButtonStyle");
            RenderedSymbol buttonType = context.planner().renderedSymbol(MATERIAL_IMPORT, "SubmenuButton");
            rendered.append("  final ");
            appendElevatedSymbol(rendered, occurrences, styleType, "widget:" + widgetId + ":submenu-button-default:type:" + scope + ':' + path, path, widgetId);
            rendered.append(" frameworkDefaults = (const ");
            appendElevatedSymbol(rendered, occurrences, buttonType, "widget:" + widgetId + ":submenu-button-default:constructor:" + scope + ':' + path, path, widgetId);
            rendered.append("(menuChildren: [], child: null)).defaultStyleOf(context);\n");
            return;
        }
        if (context.buttonFamily(widgetId).className().equals("IconButton")) {
            appendIconButtonFrameworkDefaults(rendered, occurrences, path, widgetId, context, scope);
            return;
        }
        RenderedSymbol styleType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, "ButtonStyle");
        RenderedSymbol buttonType = context.planner().renderedSymbol(
                MATERIAL_IMPORT, context.buttonFamily(widgetId).className());
        rendered.append("  final ");
        appendElevatedSymbol(
                rendered, occurrences, styleType,
                "widget:" + widgetId + ":button-style:framework-style-type:"
                + scope + ':' + path,
                path, widgetId);
        ButtonFamily family = context.buttonFamily(widgetId);
        rendered.append(" frameworkDefaults = (");
        if (!family.iconConstructor()) rendered.append("const ");
        appendElevatedSymbol(
                rendered, occurrences, buttonType,
                "widget:" + widgetId + ":button-style:default-button:"
                + scope + ':' + path,
                path, widgetId);
        if (family.className().equals("ElevatedButton")) {
            rendered.append("(onPressed: null, child: null)).defaultStyleOf(context);\n");
        } else {
            if (!family.constructorName().isEmpty()) {
                rendered.append('.');
                int offset = rendered.length();
                rendered.append(family.constructorName());
                occurrences.add(occurrence("widget:" + widgetId + ":button-style:default-icon:"
                        + scope + ':' + path, offset, family.constructorName(), MATERIAL_IMPORT, path, Optional.of(widgetId)));
            }
            rendered.append("(onPressed: null, ")
                    .append(family.iconConstructor() ? "label: " : "child: ");
            appendButtonDefaultPlaceholder(rendered, occurrences, widgetId, context, path, scope + ":child");
            if (family.iconConstructor() && family.iconPresent()) {
                rendered.append(", icon: ");
                appendButtonDefaultPlaceholder(rendered, occurrences, widgetId, context, path, scope + ":icon");
            }
            rendered.append(")).defaultStyleOf(context);\n");
        }
    }

    /**
     * Only compound fields consumed by the sparse lowering are projected here.
     * Actual IconButton still resolves all colors, overlays and scalar defaults.
     * This pinned public-API expression does not inspect private Flutter elements.
     */
    private void appendIconButtonFrameworkDefaults(StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences, String path, StableId widgetId,
            GenerationContext context, String scope) {
        String source = "  final $ButtonStyle frameworkDefaults = $ButtonStyle(\n"
                + "    minimumSize: const $WidgetStatePropertyAll<$Size>($Size(40.0, 40.0)),\n"
                + "    maximumSize: const $WidgetStatePropertyAll<$Size>($Size.infinite),\n"
                + "    shape: const $WidgetStatePropertyAll<$OutlinedBorder>($StadiumBorder()),\n"
                + "    visualDensity: $VisualDensity.standard,\n";
        if (context.buttonFamily(widgetId).constructorName().equals("outlined")) {
            source += "    side: $WidgetStateProperty.resolveWith<$BorderSide?>((states) {\n"
                    + "      if (states.contains($WidgetState.selected)) return null;\n"
                    + "      return $BorderSide(color: states.contains($WidgetState.disabled)\n"
                    + "          ? $Theme.of(context).colorScheme.onSurface.withOpacity(0.12)\n"
                    + "          : $Theme.of(context).colorScheme.outline);\n"
                    + "    }),\n";
        }
        source += "  );\n";
        java.util.regex.Matcher symbols = java.util.regex.Pattern.compile("\\$([A-Za-z][A-Za-z0-9]*)").matcher(source);
        int previous = 0;
        int index = 0;
        while (symbols.find()) {
            rendered.append(source, previous, symbols.start());
            String name = symbols.group(1);
            String library = Set.of("Size", "OutlinedBorder", "StadiumBorder", "BorderSide").contains(name)
                    ? WIDGETS_IMPORT : MATERIAL_IMPORT;
            appendElevatedSymbol(rendered, occurrences, context.planner().renderedSymbol(library, name),
                    "widget:" + widgetId + ":icon-button-default:" + scope + ':' + index++ + ':' + path,
                    path, widgetId);
            previous = symbols.end();
        }
        rendered.append(source, previous, source.length());
    }

    private void appendRangeSliderArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition changed = definition.property(new PropertyName("onChanged")).orElseThrow();
        PropertyValue reference = node.properties().get(changed.name());
        boolean enabled = new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("enabled")));
        RenderedValue callback = !enabled
                ? scalar("null", true, path + "/properties/enabled", node.id(), context)
                : reference == null ? scalar("(_) {}", false, path + "/properties/onChanged", node.id(), context)
                : renderProperty(reference, changed, path + "/properties/onChanged", node.id(), context);
        arguments.add(new ConstructorArgument(changed.parameter(), "onChanged", false, callback));
        arguments.add(new ConstructorArgument(definition.property(new PropertyName("valuesStart")).orElseThrow().parameter(),
                "values", false, renderRangeSliderPair(node, definition, "values", "RangeValues", path, context)));
        if (RangeSliderWidgetPropertySchema.labelProperties().stream().anyMatch(name -> node.properties().containsKey(new PropertyName(name)))) {
            arguments.add(new ConstructorArgument(definition.property(new PropertyName("labels")).orElseThrow().parameter(),
                    "labels", false, renderRangeSliderPair(node, definition, "labels", "RangeLabels", path, context)));
        }
        for (String family : List.of("overlayColor", "mouseCursor")) {
            ArrayList<ElevatedButtonStateEntry> entries = new ArrayList<>();
            for (String state : RangeSliderWidgetPropertySchema.statePriority()) {
                String name = family + Character.toUpperCase(state.charAt(0)) + state.substring(1);
                PropertyValue value = node.properties().get(new PropertyName(name));
                if (value != null) {
                    PropertyDefinition property = definition.property(new PropertyName(name)).orElseThrow();
                    String fieldPath = path + "/properties/" + name;
                    RenderedValue rendered = family.equals("mouseCursor") && value instanceof PropertyValue.StringValue preset
                            ? renderRangeSliderCursor(preset, fieldPath, node.id(), context)
                            : renderProperty(value, property, fieldPath, node.id(), context);
                    entries.add(new ElevatedButtonStateEntry(state.equals("default") ? "any" : state, rendered));
                }
            }
            if (!entries.isEmpty()) {
                arguments.add(new ConstructorArgument(definition.property(new PropertyName(family)).orElseThrow().parameter(),
                        family, false, renderCheckboxStateMap("WidgetStateProperty", family.equals("overlayColor") ? "Color" : "MouseCursor",
                                entries, path + "/properties/" + family, node.id(), context)));
            }
        }
    }

    private RenderedValue renderRangeSliderPair(WidgetNode node, WidgetDefinition definition, String family,
            String type, String path, GenerationContext context) {
        String firstPath = path + "/properties/" + family + "Start";
        if (family.equals("labels") && !node.properties().containsKey(new PropertyName("labelsStart"))) {
            firstPath = path + "/properties/labelsEnd";
        }
        RenderedSymbol symbol = context.planner().renderedSymbol(MATERIAL_IMPORT, type);
        StringBuilder rendered = new StringBuilder("const ");
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedSymbol(rendered, occurrences, symbol, "widget:" + node.id() + ":range-pair:" + family,
                firstPath, node.id());
        rendered.append('(');
        for (String suffix : List.of("Start", "End")) {
            if (suffix.equals("End")) rendered.append(", ");
            PropertyName name = new PropertyName(family + suffix);
            PropertyValue value = node.properties().get(name);
            RenderedValue part = value == null
                    ? scalar("''", true, firstPath, node.id(), context)
                    : renderProperty(value, definition.property(name).orElseThrow(), path + "/properties/" + name.value(), node.id(), context);
            appendRendered(rendered, occurrences, part);
        }
        rendered.append(')');
        return scalar(rendered.toString(), true, firstPath, node.id(), context, occurrences);
    }

    private RenderedValue renderRangeSliderCursor(PropertyValue.StringValue preset, String path,
            StableId widgetId, GenerationContext context) {
        String owner = preset.value().equals("defer") || preset.value().equals("uncontrolled")
                ? "MouseCursor" : Set.of("clickable", "adaptiveClickable", "textable").contains(preset.value())
                        ? "WidgetStateMouseCursor" : "SystemMouseCursors";
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, owner);
        return scalar(type.text() + '.' + preset.value(), true, path, widgetId, context,
                List.of(occurrence("widget:" + widgetId + ":range-cursor:" + path, type.nameOffset(),
                        type.name(), type.libraryUri(), path, Optional.of(widgetId)),
                        occurrence("widget:" + widgetId + ":range-cursor-member:" + path, type.text().length() + 1,
                                preset.value(), type.libraryUri(), path, Optional.of(widgetId))));
    }

    private void appendSliderArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition changed = definition.property(new PropertyName("onChanged")).orElseThrow();
        PropertyValue reference = node.properties().get(changed.name());
        boolean enabled = new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("enabled")));
        RenderedValue callback = !enabled
                ? scalar("null", true, path + "/properties/enabled", node.id(), context)
                : reference == null ? scalar("(_) {}", false, path + "/properties/onChanged", node.id(), context)
                : renderProperty(reference, changed, path + "/properties/onChanged", node.id(), context);
        arguments.add(new ConstructorArgument(changed.parameter(), "onChanged", false, callback));
        if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) {
            appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
        }
        ArrayList<ElevatedButtonStateEntry> entries = new ArrayList<>();
        for (String state : SliderWidgetPropertySchema.statePriority()) {
            String name = "overlayColor" + Character.toUpperCase(state.charAt(0)) + state.substring(1);
            PropertyValue value = node.properties().get(new PropertyName(name));
            if (value != null) {
                entries.add(new ElevatedButtonStateEntry(state.equals("default") ? "any" : state,
                        renderProperty(value, definition.property(new PropertyName(name)).orElseThrow(),
                                path + "/properties/" + name, node.id(), context)));
            }
        }
        if (!entries.isEmpty()) {
            arguments.add(new ConstructorArgument(definition.property(new PropertyName("overlayColor")).orElseThrow().parameter(),
                    "overlayColor", false, renderCheckboxStateMap("WidgetStateProperty", "Color", entries,
                            path + "/properties/overlayColor", node.id(), context)));
        }
    }

    private void appendSwitchArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition changed = definition.property(new PropertyName("onChanged")).orElseThrow();
        PropertyValue reference = node.properties().get(changed.name());
        boolean enabled = new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("enabled")));
        RenderedValue callback = !enabled
                ? scalar("null", true, path + "/properties/enabled", node.id(), context)
                : reference == null ? scalar("(_) {}", false, path + "/properties/onChanged", node.id(), context)
                : renderProperty(reference, changed, path + "/properties/onChanged", node.id(), context);
        arguments.add(new ConstructorArgument(changed.parameter(), "onChanged", false, callback));
        if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) {
            appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
        }
        appendSwitchStateArguments(node, definition, path, context, arguments);
    }

    private void appendSwitchStateArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        List<String> families = new ArrayList<>(SwitchWidgetPropertySchema.colorFamilies());
        if (node.type().equals(SwitchWidgetPropertySchema.SWITCH_TYPE)) families.add("trackOutlineWidth");
        families.add("thumbIcon");
        for (String family : families) {
            ArrayList<ElevatedButtonStateEntry> entries = new ArrayList<>();
            for (String state : SwitchWidgetPropertySchema.statePriority()) {
                String suffix = Character.toUpperCase(state.charAt(0)) + state.substring(1);
                String name = family + suffix;
                RenderedValue rendered;
                if (family.equals("thumbIcon")) {
                    rendered = renderSwitchThumbIcon(node, definition, suffix, path, context);
                } else {
                    PropertyValue value = node.properties().get(new PropertyName(name));
                    rendered = value == null ? null : renderProperty(value, definition.property(new PropertyName(name)).orElseThrow(),
                            path + "/properties/" + name, node.id(), context);
                }
                if (rendered != null) {
                    entries.add(new ElevatedButtonStateEntry(state.equals("default") ? "any" : state, rendered));
                }
            }
            if (!entries.isEmpty()) {
                arguments.add(new ConstructorArgument(definition.property(new PropertyName(family)).orElseThrow().parameter(),
                        family, false, renderCheckboxStateMap("WidgetStateProperty",
                                family.equals("thumbIcon") ? "Icon" : family.equals("trackOutlineWidth") ? "double" : "Color",
                                entries, path + "/properties/" + family, node.id(), context)));
            }
        }
    }

    private RenderedValue renderSwitchThumbIcon(WidgetNode node, WidgetDefinition definition,
            String state, String path, GenerationContext context) {
        List<String> bucket = SwitchWidgetPropertySchema.thumbIconBucketProperties(state);
        String modeName = bucket.getFirst();
        PropertyValue mode = node.properties().get(new PropertyName(modeName));
        if (new PropertyValue.StringValue("inherit").equals(mode)) {
            return scalar("null", true, path + "/properties/" + modeName, node.id(), context);
        }
        if (mode == null && bucket.stream().noneMatch(name -> node.properties().containsKey(new PropertyName(name)))) {
            return null;
        }
        ArrayList<CompositeMember> members = new ArrayList<>();
        for (String name : bucket.subList(1, bucket.size())) {
            PropertyValue value = node.properties().get(new PropertyName(name));
            if (value != null) {
                RenderedValue field = renderProperty(value, definition.property(new PropertyName(name)).orElseThrow(),
                        path + "/properties/" + name, node.id(), context);
                // The shared IconData renderer historically has one data occurrence per widget.
                // Here nine independent Icon values share a widget id; preserve every model path
                // while assigning distinct proof identities to each emitted field occurrence.
                field = new RenderedValue(field.lines(), field.constant(), field.utf8Size(),
                        field.symbolOccurrences().stream().map(symbol -> new GeneratedDartSymbolOccurrence(
                                symbol.id() + ":switch-icon-field:" + name, symbol.region(), symbol.offset(), symbol.length(),
                                symbol.symbolName(), symbol.libraryUri(), symbol.modelPath(), symbol.widgetId(),
                                symbol.staticTypeRequirement())).toList());
                members.add(new CompositeMember(SwitchWidgetPropertySchema.iconSourceName(name).orElseThrow(), members.size(),
                        field));
            }
        }
        boolean constant = members.stream().allMatch(member -> member.rendered().constant());
        RenderedSymbol icon = context.planner().renderedSymbol(WIDGETS_IMPORT, "Icon");
        String iconPath = path + "/properties/thumbIcon" + state;
        StringBuilder rendered = new StringBuilder(constant ? "const " : "");
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedSymbol(rendered, occurrences, icon, "widget:" + node.id() + ":switch-icon:" + state, iconPath, node.id());
        rendered.append('(');
        CompositeMember data = members.stream().filter(member -> member.name().equals("icon")).findFirst().orElse(null);
        if (data == null) {
            rendered.append("null");
        } else {
            appendRendered(rendered, occurrences, data.rendered());
        }
        for (CompositeMember member : members) {
            if (!member.name().equals("icon")) {
                rendered.append(", ").append(member.name()).append(": ");
                appendRendered(rendered, occurrences, member.rendered());
            }
        }
        rendered.append(')');
        return scalar(rendered.toString(), constant, iconPath, node.id(), context, occurrences);
    }

    /** Replace only the reviewed runtime argument; model properties remain design previews. */
    private void appendStateBindingArgument(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        if (node.stateBinding().isEmpty()) return;
        StateBinding binding = node.stateBinding().orElseThrow();
        String argumentName = WidgetStateBindingCatalog.find(node).orElseThrow().runtimeArgumentName();
        boolean radio = node.type().equals(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE)
                || node.type().equals(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE);
        // Retain the existing Radio output and constructor-proven type identity
        // so schema-14 bound forms keep their exact managed-region fingerprints.
        RenderedValue field = radio ? renderDartObjectReference(new PropertyValue.DartObjectReferenceValue(
                Optional.empty(), binding.fieldName(), Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                "Object?", path + "/stateBinding/fieldName", node.id(), context,
                Optional.of(radioTypeSymbol(node, context).text()
                        + (RadioWidgetPropertySchema.nullableValueType(node) ? "?" : "")))
                : renderStateField(binding.fieldName(), binding.type(), binding.referenceType(),
                        path + "/stateBinding/fieldName", node.id(), context);
        if (node.type().equals(ListTileWidgetPropertySchema.LIST_TILE_TYPE)
                && binding.action() == StateBinding.Action.SELECT) {
            field = compareStateField(field, binding.selectedValue().orElseThrow(),
                    path + "/stateBinding/selectedValue", node.id(), context);
        }
        replaceStateArgument(node, definition, arguments, argumentName, argumentName, field);
    }

    private static String stateFieldDartType(StateBinding.Type type) {
        return switch (type) {
            case BOOL -> "bool";
            case STRING -> "String";
            case INT -> "int";
            case NUM -> "num";
            case TEXT_CONTROLLER -> "TextEditingController";
            case NULLABLE_BOOL -> "bool?";
            case DOUBLE -> "double";
            case RANGE_VALUES -> "RangeValues";
            case NULLABLE_STRING -> "String?";
            case NULLABLE_INT -> "int?";
            case NULLABLE_DOUBLE -> "double?";
            case NULLABLE_NUM -> "num?";
            case NULLABLE_OBJECT, NULLABLE_REFERENCE -> "Object?";
        };
    }

    private RenderedValue renderStateField(String fieldName, StateBinding.Type type,
            Optional<PropertyValue.DartObjectReferenceValue> referenceType, String path,
            StableId widgetId, GenerationContext context) {
        Optional<RenderedSymbol> referenceSymbol = referenceType.map(reference -> reference.libraryUri().isPresent()
                ? context.planner().renderedSymbol(reference.libraryUri().orElseThrow(), reference.rootSymbol())
                : new RenderedSymbol(reference.rootSymbol(), reference.rootSymbol(), CURRENT_PROJECT_LIBRARY_URI, 0));
        RenderedValue field = renderDartObjectReference(new PropertyValue.DartObjectReferenceValue(
                Optional.empty(), fieldName, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                stateFieldDartType(type), path, widgetId, context,
                referenceSymbol.map(symbol -> symbol.text() + "?"));
        if (referenceSymbol.isEmpty()) return field;
        // The explicit type token gives the custom field witness independently
        // manifested provenance even when no RadioGroup constructor is present.
        RenderedSymbol symbol = referenceSymbol.orElseThrow();
        StringBuilder text = new StringBuilder("(");
        var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
        appendRendered(text, occurrences, field);
        text.append(" as ");
        int typeOffset = text.length() + symbol.nameOffset();
        text.append(symbol.text()).append("?)");
        occurrences.add(occurrence("widget:" + widgetId + ":property-reference:" + path + "/referenceType:root",
                typeOffset, symbol.name(), symbol.libraryUri(), path + "/referenceType/rootSymbol", Optional.of(widgetId)));
        return scalar(text.toString(), false, path, widgetId, context, occurrences);
    }

    private void appendPropertyStateArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        for (var entry : node.propertyBindings().entrySet()) {
            if (!FocusWidgetPropertySchema.propertyAvailable(node, entry.getKey())
                    || !SwitchListTileWidgetPropertySchema.propertyAvailable(node, entry.getKey())
                    || !RadioListTileWidgetPropertySchema.propertyAvailable(node, entry.getKey())) continue;
            var descriptor = WidgetStatePropertyBindingCatalog.find(node, entry.getKey()).orElseThrow();
            StatePropertyBinding binding = entry.getValue();
            String bindingPath = path + "/propertyBindings/" + pointer(entry.getKey().value());
            RenderedValue field = renderStateField(binding.fieldName(), binding.type(), binding.referenceType(),
                    bindingPath + "/fieldName", node.id(), context);
            RenderedValue rendered = switch (binding.transform()) {
                case DIRECT -> field;
                case TO_STRING -> stateSuffix(field, ".toString()", bindingPath, node.id(), context);
                case TEXT -> stateSuffix(field, ".text", bindingPath, node.id(), context);
                case NOT -> {
                    var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
                    StringBuilder expression = new StringBuilder("!");
                    appendRendered(expression, occurrences, field);
                    yield scalar(expression.toString(), false, bindingPath, node.id(), context, occurrences);
                }
                case EQUALS -> compareStateField(field, binding.comparisonValue().orElseThrow(),
                        bindingPath + "/comparisonValue", node.id(), context);
                case CLAMP -> {
                    if (node.type().value().equals("flutter.widgets.IndexedStack")) {
                        WidgetSlot slot = node.slots().get(new SlotName("children"));
                        int count = slot instanceof WidgetSlot.ListSlot list ? list.children().size() : 0;
                        // An empty IndexedStack has no selectable child. Keep
                        // the typed field occurrence, but pass null to Flutter.
                        String suffix = count == 0 ? " < 0 ? null : null"
                                : ".clamp(0, " + (count - 1) + ").toInt()";
                        var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
                        StringBuilder expression = new StringBuilder("(");
                        appendRendered(expression, occurrences, field);
                        expression.append(suffix).append(')');
                        yield scalar(expression.toString(), false, bindingPath, node.id(), context, occurrences);
                    }
                    yield stateSuffix(field, ".clamp(0.0, 1.0).toDouble()", bindingPath, node.id(), context);
                }
            };
            if (node.type().equals(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE)
                    && entry.getKey().value().equals("enabled")) {
                ConstructorArgument pressed = arguments.stream().filter(value -> value.name().equals("onPressed")).findFirst().orElseThrow();
                var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
                StringBuilder conditional = new StringBuilder("(");
                appendRendered(conditional, occurrences, rendered);
                conditional.append(" ? ");
                appendRendered(conditional, occurrences, pressed.value());
                conditional.append(" : null)");
                replaceStateArgument(node, definition, arguments, "onPressed", "onPressed",
                        scalar(conditional.toString(), false, bindingPath, node.id(), context, occurrences));
            } else {
                replaceStateArgument(node, definition, arguments, entry.getKey().value(), descriptor.dartArgumentName(), rendered);
            }
        }
    }

    private RenderedValue stateSuffix(RenderedValue field, String suffix, String path,
            StableId widgetId, GenerationContext context) {
        return scalar(field.joined() + suffix, false, path, widgetId, context, field.symbolOccurrences());
    }

    private RenderedValue compareStateField(RenderedValue field, PropertyValue comparison, String path,
            StableId widgetId, GenerationContext context) {
        String literal = switch (comparison) {
            case PropertyValue.NullValue ignored -> "null";
            case PropertyValue.BooleanValue value -> Boolean.toString(value.value());
            case PropertyValue.IntegerValue value -> value.value().toString();
            case PropertyValue.DoubleValue value -> dartDouble(value.value());
            case PropertyValue.StringValue value -> dartString(value.value(), path, widgetId, context.maxRenderedUtf8Bytes());
            default -> throw new IllegalArgumentException("State comparison requires a closed scalar or null literal");
        };
        StringBuilder text = new StringBuilder("(");
        var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
        appendRendered(text, occurrences, field);
        text.append(" == ").append(literal).append(')');
        return scalar(text.toString(), false, path, widgetId, context, occurrences);
    }

    private void replaceStateArgument(WidgetNode node, WidgetDefinition definition,
            List<ConstructorArgument> arguments, String propertyName, String argumentName, RenderedValue field) {
        int existing = -1;
        for (int index = 0; index < arguments.size(); index++) {
            if (arguments.get(index).name().equals(argumentName)) {
                if (existing >= 0) throw new IllegalArgumentException("Duplicate bound constructor argument");
                existing = index;
            }
        }
        if (existing >= 0) {
            ConstructorArgument argument = arguments.get(existing);
            arguments.set(existing, new ConstructorArgument(argument.parameter(), argument.name(), false, field));
        } else {
            // TextField's runtime controller is created through the ownership
            // workflow, not exposed as an unowned generic property reference.
            DartParameter parameter = node.type().equals(TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE)
                    && argumentName.equals("controller") ? DartParameter.named(0, false)
                    : definition.property(new PropertyName(propertyName)).orElseThrow().parameter();
            arguments.add(new ConstructorArgument(parameter, argumentName, false, field));
        }
    }

    private RenderedSymbol notificationTypeSymbol(WidgetNode node, GenerationContext context) {
        PropertyValue value = node.properties().get(new PropertyName("notificationType"));
        if (value instanceof PropertyValue.StringValue preset) {
            return context.planner().renderedSymbol(WIDGETS_IMPORT, preset.value());
        }
        PropertyValue.DartObjectReferenceValue reference = (PropertyValue.DartObjectReferenceValue) value;
        return reference.libraryUri().isPresent()
                ? context.planner().renderedSymbol(reference.libraryUri().orElseThrow(), reference.rootSymbol())
                : new RenderedSymbol(reference.rootSymbol(), reference.rootSymbol(), CURRENT_PROJECT_LIBRARY_URI, 0);
    }

    private void appendNotificationListenerArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyName key = new PropertyName("onNotification");
        PropertyValue value = node.properties().get(key);
        if (value == null) return;
        PropertyDefinition property = definition.property(key).orElseThrow();
        String valuePath = path + "/properties/onNotification";
        PropertyValue referenceValue = value instanceof PropertyValue.CallbackValue callback
                ? new PropertyValue.DartObjectReferenceValue(Optional.empty(), callback.handler(), Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()) : value;
        RenderedValue rendered = referenceValue instanceof PropertyValue.DartObjectReferenceValue reference
                ? renderDartObjectReference(reference, NotificationListenerWidgetPropertySchema.CALLBACK_TYPE,
                        valuePath, node.id(), context, Optional.of(notificationTypeSymbol(node, context).text()))
                : renderProperty(value, property, valuePath, node.id(), context);
        arguments.add(new ConstructorArgument(property.parameter(), "onNotification", false, rendered));
    }

    private RenderedValue renderViewportDelegate(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context) {
        String slotPath = path + "/slots/children";
        var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, "SliverChildListDelegate");
        var lines = new LineAccumulator(context.maxRenderedUtf8Bytes(), slotPath, node.id());
        lines.addBlock(symbol.text() + "(",
                List.of(occurrence("widget:" + node.id() + ":viewportDelegate",
                        symbol.nameOffset(), symbol.name(), symbol.libraryUri(), slotPath, Optional.of(node.id()))), 0);
        var children = renderSlot(node.slots().get(new SlotName("children")), slotPath, indent + 2, context);
        lines.addBlock(spaces(indent + 2) + children.joined() + ",", children.symbolOccurrences(), indent + 2);
        for (var property : definition.properties()) {
            if (property.parameter().order() < 3) continue;
            var value = node.properties().get(property.name());
            if (value == null) continue;
            var rendered = renderProperty(value, property, path + "/properties/" + property.name().value(), node.id(), context);
            String prefix = spaces(indent + 2) + property.name().value() + ": ";
            lines.addBlock(prefix + rendered.joined() + ",", rendered.symbolOccurrences(), prefix.length());
        }
        lines.add(spaces(indent) + ")");
        // Mutable list delegate retains key lookup/reordering; do not substitute .fixed.
        return lines.build(false);
    }

    private void appendValueListenableBuilderArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        boolean sliver = dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.SLIVER_TYPE.equals(node.type());
        RenderedSymbol type = radioTypeSymbol(node, context);
        String selectedType = type.text() + (RadioWidgetPropertySchema.nullableValueType(node) ? "?" : "");
        for (String name : List.of("valueListenable", "builder")) {
            var property = definition.property(new PropertyName(name)).orElseThrow();
            var value = node.properties().get(property.name());
            String valuePath = path + "/properties/" + name;
            boolean source = name.equals("valueListenable");
            RenderedValue rendered;
            if (value instanceof PropertyValue.DartObjectReferenceValue reference) {
                rendered = renderDartObjectReference(reference, source ? "ValueListenable<Object>" : "ValueWidgetBuilder<Object>",
                        valuePath, node.id(), context, Optional.of(selectedType));
            } else {
                String symbolName = source ? "AlwaysStoppedAnimation" : sliver ? "SliverToBoxAdapter" : "SizedBox";
                var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, symbolName);
                String prefix = source ? "const " : "(context, value, child) => child ?? const ";
                String suffix = source ? "<" + selectedType + ">("
                        + dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.constantLiteral(node) + ")"
                        : sliver ? "()" : ".shrink()";
                var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
                occurrences.add(occurrence("widget:" + node.id() + ":valueListenablePreset:" + name,
                        prefix.length() + symbol.nameOffset(), symbol.name(), symbol.libraryUri(), valuePath, Optional.of(node.id())));
                if (source) occurrences.add(occurrence("widget:" + node.id() + ":value-listenable-preset-core-type",
                        prefix.length() + symbol.text().length() + 1 + type.nameOffset(), type.name(), type.libraryUri(),
                        path + "/properties/valueType", Optional.of(node.id()),
                        Optional.of(new GeneratedDartStaticTypeRequirement(prefix.length() + symbol.text().length() + 1,
                                type.text().length(), "Type", Optional.of(selectedType)))));
                else if (!sliver) occurrences.add(occurrence("widget:" + node.id() + ":valueListenablePreset:shrink",
                        prefix.length() + symbol.text().length() + 1, "shrink", symbol.libraryUri(), valuePath, Optional.of(node.id())));
                rendered = scalar(prefix + symbol.text() + suffix, source, valuePath, node.id(), context, occurrences);
            }
            arguments.add(new ConstructorArgument(property.parameter(), name, false, rendered));
        }
    }

    private void appendTweenAnimationBuilderArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        boolean sliver = dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE.equals(node.type());
        RenderedSymbol type = radioTypeSymbol(node, context);
        String selectedType = type.text() + (RadioWidgetPropertySchema.nullableValueType(node) ? "?" : "");
        for (String name : List.of("tween", "builder")) {
            var property = definition.property(new PropertyName(name)).orElseThrow();
            var value = node.properties().get(property.name());
            String valuePath = path + "/properties/" + name;
            boolean source = name.equals("tween");
            RenderedValue rendered;
            if (value instanceof PropertyValue.DartObjectReferenceValue reference) {
                rendered = renderDartObjectReference(reference, source ? "Tween<Object>" : "ValueWidgetBuilder<Object>",
                        valuePath, node.id(), context, Optional.of(selectedType));
            } else {
                String symbolName = source ? dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.presetClass(node) : sliver ? "SliverToBoxAdapter" : "SizedBox";
                var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, symbolName);
                String prefix = source ? "" : "(context, value, child) => child ?? const ";
                boolean generic = !symbolName.equals("IntTween");
                String suffix = source ? (generic ? "<" + selectedType + ">" : "")
                        + (symbolName.equals("ConstantTween") ? "(" + tweenConstantLiteral(node) + ")"
                                : symbolName.equals("IntTween") ? "(begin: 0, end: 1)" : "(begin: 0.0, end: 1.0)")
                        : sliver ? "()" : ".shrink()";
                var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
                occurrences.add(occurrence("widget:" + node.id() + ":tweenPreset:" + name,
                        prefix.length() + symbol.nameOffset(), symbol.name(), symbol.libraryUri(), valuePath, Optional.of(node.id())));
                if (source && generic) occurrences.add(occurrence("widget:" + node.id() + ":tween-animation-preset-core-type",
                        prefix.length() + symbol.text().length() + 1 + type.nameOffset(), type.name(), type.libraryUri(),
                        path + "/properties/valueType", Optional.of(node.id()),
                        Optional.of(new GeneratedDartStaticTypeRequirement(prefix.length() + symbol.text().length() + 1,
                                type.text().length(), "Type", Optional.of(selectedType)))));
                else if (!source && !sliver) occurrences.add(occurrence("widget:" + node.id() + ":tweenPreset:shrink",
                        prefix.length() + symbol.text().length() + 1, "shrink", symbol.libraryUri(), valuePath, Optional.of(node.id())));
                rendered = scalar(prefix + symbol.text() + suffix, false, valuePath, node.id(), context, occurrences);
            }
            arguments.add(new ConstructorArgument(property.parameter(), name, false, rendered));
        }
    }

    private static String tweenConstantLiteral(WidgetNode node) {
        return switch (((PropertyValue.StringValue) node.properties().get(new PropertyName("valueType"))).value()) {
            case "String" -> "''";
            case "bool" -> "false";
            case "Object" -> "0";
            default -> throw new IllegalArgumentException("Not a constant tween preset");
        };
    }

    private static boolean usesListenableBuilderPresets(WidgetNode node) {
        return dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.supports(node.type())
                || dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.supports(node.type());
    }

    private void appendListenableBuilderPresets(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        boolean sliver = dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.SLIVER_TYPE.equals(node.type())
                || dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.SLIVER_TYPE.equals(node.type());
        for (var property : definition.properties()) {
            if (!(node.properties().get(property.name()) instanceof PropertyValue.StringValue)) continue;
            String name = property.name().value();
            boolean source = name.equals("listenable") || name.equals("animation");
            String symbolName = source ? "AlwaysStoppedAnimation" : sliver ? "SliverToBoxAdapter" : "SizedBox";
            var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, symbolName);
            String prefix = source ? "const " : "(context, child) => child ?? const ";
            String suffix = source ? "<double>(0.0)" : sliver ? "()" : ".shrink()";
            String valuePath = path + "/properties/" + name;
            var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
            occurrences.add(occurrence("widget:" + node.id() + ":listenablePreset:" + name,
                    prefix.length() + symbol.nameOffset(), symbol.name(), symbol.libraryUri(), valuePath, Optional.of(node.id())));
            if (!source && !sliver) occurrences.add(occurrence("widget:" + node.id() + ":listenablePreset:shrink",
                    prefix.length() + symbol.text().length() + 1, "shrink", symbol.libraryUri(), valuePath, Optional.of(node.id())));
            arguments.add(new ConstructorArgument(property.parameter(), name, false,
                    scalar(prefix + symbol.text() + suffix, source, valuePath, node.id(), context, occurrences)));
        }
    }

    private static boolean usesEmptyBoxBuilderPreset(WidgetNode node) {
        return node.type().equals(dev.flutter.netbeans.designer.catalog.LayoutBuilderWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.TYPE)
                || node.type().equals(dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.TYPE);
    }

    private void appendSliverPresetArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        for (var property : definition.properties()) {
            if (!(node.properties().get(property.name()) instanceof PropertyValue.StringValue preset)) continue;
            String name = property.name().value();
            String valuePath = path + "/properties/" + name;
            RenderedValue value;
            if (name.equals("itemExtentBuilder")) {
                // Bound the reviewed preset where the generated constructor knows its child count.
                Integer count = node.slots().get(new SlotName("children")) instanceof WidgetSlot.ListSlot children
                        ? children.children().size() : null;
                String limit = count == null ? "" : " && index < " + count;
                if (count == null && node.properties().get(new PropertyName("itemCount")) instanceof PropertyValue.IntegerValue items) {
                    limit = " && index < " + items.value();
                }
                value = scalar("(index, dimensions) => index >= 0" + limit + " ? 48.0 : null",
                        false, valuePath, node.id(), context);
            } else if (name.equals("itemBuilder")) {
                value = scalar("(_, index) => null", false, valuePath, node.id(), context);
            } else {
                String symbolName = switch (name) {
                    case "builder" -> usesEmptyBoxBuilderPreset(node)
                            ? "SizedBox" : "SliverToBoxAdapter";
                    case "separatorBuilder" -> "SizedBox";
                    case "delegate" -> "SliverChildListDelegate";
                    case "gridDelegate" -> preset.value().equals("maxExtent")
                            ? "SliverGridDelegateWithMaxCrossAxisExtent" : "SliverGridDelegateWithFixedCrossAxisCount";
                    default -> throw new IllegalArgumentException("Unknown sliver preset property: " + name);
                };
                var symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, symbolName);
                String prefix = name.equals("builder") ? ((node.type().equals(dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.TYPE) || dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.supports(node.type())) ? "(context, orientation) => const " : "(context, constraints) => const ")
                        : name.equals("separatorBuilder") ? "(_, index) => const "
                        : name.equals("gridDelegate") ? "const " : "";
                String suffix = switch (name) {
                    case "builder" -> usesEmptyBoxBuilderPreset(node) ? ".shrink()" : "()";
                    case "separatorBuilder" -> ".shrink()";
                    case "delegate" -> "(const [])";
                    default -> preset.value().equals("maxExtent")
                            ? "(maxCrossAxisExtent: 200.0)" : "(crossAxisCount: 2)";
                };
                var occurrences = new ArrayList<GeneratedDartSymbolOccurrence>();
                occurrences.add(occurrence("widget:" + node.id() + ":sliverPreset:" + name,
                        prefix.length() + symbol.nameOffset(), symbol.name(), symbol.libraryUri(),
                        valuePath, Optional.of(node.id())));
                if (name.equals("separatorBuilder") || name.equals("builder") && symbolName.equals("SizedBox")) occurrences.add(occurrence(
                        "widget:" + node.id() + ":sliverPreset:shrink",
                        prefix.length() + symbol.text().length() + 1, "shrink", symbol.libraryUri(),
                        valuePath, Optional.of(node.id())));
                value = scalar(prefix + symbol.text() + suffix, name.equals("gridDelegate"),
                        valuePath, node.id(), context, occurrences);
            }
            arguments.add(new ConstructorArgument(property.parameter(), name, false, value));
        }
    }

    private void appendBuilderArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition property = definition.property(new PropertyName("builder")).orElseThrow();
        PropertyValue value = node.properties().get(property.name());
        if (value == null) {
            throw new IllegalArgumentException("Builder requires its builder callback.");
        }
        String valuePath = path + "/properties/builder";
        RenderedValue rendered;
        if (value instanceof PropertyValue.CallbackValue callback) {
            if (callback.handler().equals("noop")) {
                rendered = scalar("(_) => const SizedBox.shrink()", false, valuePath, node.id(), context);
            } else {
                rendered = renderDartObjectReference(new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), callback.handler(), Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                        BuilderWidgetPropertySchema.CALLBACK_TYPE, valuePath, node.id(), context);
            }
        } else {
            rendered = renderProperty(value, property, valuePath, node.id(), context);
        }
        arguments.add(new ConstructorArgument(property.parameter(), "builder", false, rendered));
    }

    private RenderedSymbol radioTypeSymbol(WidgetNode node, GenerationContext context) {
        PropertyValue value = node.properties().get(new PropertyName("valueType"));
        if (value instanceof PropertyValue.StringValue builtin) {
            // Keep the user's original core scope. Exact navigation proof rejects
            // a shadowed or unavailable builtin; no implicit import is broadened.
            return new RenderedSymbol(builtin.value(), builtin.value(), "dart:core", 0);
        }
        PropertyValue.DartObjectReferenceValue reference = (PropertyValue.DartObjectReferenceValue) value;
        return reference.libraryUri().isPresent()
                ? context.planner().renderedSymbol(reference.libraryUri().orElseThrow(), reference.rootSymbol())
                : new RenderedSymbol(reference.rootSymbol(), reference.rootSymbol(), CURRENT_PROJECT_LIBRARY_URI, 0);
    }

    private void appendTooltipThemeArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition data = definition.property(new PropertyName("data")).orElseThrow();
        String dataPath = path + "/properties/data";
        PropertyValue whole = node.properties().get(data.name());
        if (whole != null) {
            arguments.add(new ConstructorArgument(data.parameter(), "data", false,
                    renderProperty(whole, data, dataPath, node.id(), context, indent)));
            return;
        }
        var localArguments = new ArrayList<ConstructorArgument>();
        for (String name : TooltipThemeWidgetPropertySchema.dataFields()) {
            PropertyDefinition property = definition.property(new PropertyName(name)).orElseThrow();
            PropertyValue value = node.properties().get(property.name());
            if (value == null) continue;
            String valuePath = path + "/properties/" + name;
            RenderedValue rendered = TooltipThemeWidgetPropertySchema.durationProperties().contains(name)
                    && value instanceof PropertyValue.IntegerValue duration
                    ? scalar("const Duration(microseconds: " + duration.value() + ")", true, valuePath, node.id(), context,
                            List.of(occurrence("widget:" + node.id() + ":tooltip-theme-core-duration:" + name,
                                    6, "Duration", "dart:core", valuePath, Optional.of(node.id()))))
                    : renderProperty(value, property, valuePath, node.id(), context, indent + 2);
            localArguments.add(new ConstructorArgument(property.parameter(),
                    TooltipThemeWidgetPropertySchema.find(name).orElseThrow().dartName(), false, rendered));
        }
        appendTextCompoundArguments(node, definition, path, indent + 2, context, localArguments, "textStyle",
                definition.property(new PropertyName("textStyle")).orElseThrow().parameter().order(),
                TooltipThemeWidgetPropertySchema::textStyleBinding);
        // These bindings belong to TooltipThemeData, not to the outer widget.
        appendPropertyStateArguments(node, definition, path, context, localArguments);
        localArguments.sort(ARGUMENT_ORDER);
        var members = localArguments.stream().map(argument -> new CompositeMember(
                argument.name(), argument.parameter().order(), argument.value())).toList();
        arguments.add(new ConstructorArgument(data.parameter(), "data", false,
                renderNamedCompositeMembers(MATERIAL_IMPORT, "TooltipThemeData", Optional.empty(), members,
                        indent, dataPath, node.id(), context)));
    }

    private void appendTooltipArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition callback = definition.property(new PropertyName("onTriggered")).orElseThrow();
        PropertyValue handler = node.properties().get(callback.name());
        if (handler != null) arguments.add(new ConstructorArgument(callback.parameter(), "onTriggered", false,
                handler instanceof PropertyValue.StringValue
                        ? scalar("() {}", false, path + "/properties/onTriggered", node.id(), context)
                        : renderProperty(handler, callback, path + "/properties/onTriggered", node.id(), context)));
        for (String name : TooltipWidgetPropertySchema.durationProperties()) {
            PropertyDefinition property = definition.property(new PropertyName(name)).orElseThrow();
            PropertyValue value = node.properties().get(property.name());
            if (value == null) continue;
            String valuePath = path + "/properties/" + name;
            RenderedValue rendered = value instanceof PropertyValue.IntegerValue duration
                    ? scalar("const Duration(microseconds: " + duration.value() + ")", true, valuePath, node.id(), context,
                            List.of(occurrence("widget:" + node.id() + ":tooltip-core-duration:" + name,
                                    6, "Duration", "dart:core", valuePath, Optional.of(node.id()))))
                    : renderProperty(value, property, valuePath, node.id(), context);
            arguments.add(new ConstructorArgument(property.parameter(), TooltipWidgetPropertySchema.find(name).orElseThrow().dartName(), false, rendered));
        }
        if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) appendMouseCursorPreset(node, definition, "mouseCursor", path, context, arguments);
        appendTextCompoundArguments(node, definition, path, indent, context, arguments, "textStyle",
                definition.property(new PropertyName("textStyle")).orElseThrow().parameter().order(), TooltipWidgetPropertySchema::textStyleBinding);
    }

    private void appendNavigationBarArguments(WidgetNode node, WidgetDefinition definition,
            String path, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition callback = definition
                .property(new PropertyName("onDestinationSelected")).orElseThrow();
        PropertyValue handler = node.properties().get(callback.name());
        if (handler != null) {
            String callbackPath = path + "/properties/onDestinationSelected";
            RenderedValue rendered = handler instanceof PropertyValue.StringValue
                    ? scalar("(_) {}", false, callbackPath, node.id(), context)
                    : renderProperty(handler, callback, callbackPath, node.id(), context);
            arguments.add(new ConstructorArgument(callback.parameter(), "onDestinationSelected", false, rendered));
        }

        PropertyDefinition duration = definition
                .property(new PropertyName("animationDurationUs")).orElseThrow();
        PropertyValue durationValue = node.properties().get(duration.name());
        if (durationValue == null) return;
        String durationPath = path + "/properties/animationDurationUs";
        RenderedValue rendered = durationValue instanceof PropertyValue.IntegerValue integer
                ? scalar("const Duration(microseconds: " + integer.value() + ")", true,
                        durationPath, node.id(), context,
                        List.of(occurrence("widget:" + node.id() + ":navigation-bar-core-duration",
                                6, "Duration", "dart:core", durationPath, Optional.of(node.id()))))
                : renderProperty(durationValue, duration, durationPath, node.id(), context);
        arguments.add(new ConstructorArgument(duration.parameter(), "animationDuration", false, rendered));
    }

    private void appendPageViewArguments(WidgetNode node, WidgetDefinition definition,
            String path, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition callback = definition
                .property(new PropertyName("onPageChanged")).orElseThrow();
        PropertyValue handler = node.properties().get(callback.name());
        if (handler == null) return;
        String callbackPath = path + "/properties/onPageChanged";
        RenderedValue rendered = handler instanceof PropertyValue.StringValue
                ? scalar("(_) {}", false, callbackPath, node.id(), context)
                : renderProperty(handler, callback, callbackPath, node.id(), context);
        arguments.add(new ConstructorArgument(callback.parameter(), "onPageChanged", false, rendered));
    }

    private void appendListWheelScrollViewArguments(WidgetNode node, WidgetDefinition definition,
            String path, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition callback = definition.property(new PropertyName("onSelectedItemChanged")).orElseThrow();
        PropertyValue handler = node.properties().get(callback.name());
        if (handler == null) return;
        String callbackPath = path + "/properties/onSelectedItemChanged";
        RenderedValue rendered = handler instanceof PropertyValue.StringValue
                ? scalar("(_) {}", false, callbackPath, node.id(), context)
                : renderProperty(handler, callback, callbackPath, node.id(), context);
        arguments.add(new ConstructorArgument(callback.parameter(), "onSelectedItemChanged", false, rendered));
    }

    private void appendBottomNavigationBarArguments(WidgetNode node, WidgetDefinition definition,
            String path, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition callback = definition.property(new PropertyName("onTap")).orElseThrow();
        PropertyValue handler = node.properties().get(callback.name());
        if (handler == null) return;
        String callbackPath = path + "/properties/onTap";
        RenderedValue rendered = handler instanceof PropertyValue.StringValue
                ? scalar("(_) {}", false, callbackPath, node.id(), context)
                : renderProperty(handler, callback, callbackPath, node.id(), context);
        arguments.add(new ConstructorArgument(callback.parameter(), "onTap", false, rendered));
    }

    private void appendMaterialArguments(WidgetNode node, WidgetDefinition definition,
            String path, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition duration = definition.property(new PropertyName("animationDurationUs")).orElseThrow();
        PropertyValue durationValue = node.properties().get(duration.name());
        if (durationValue == null) return;
        String durationPath = path + "/properties/animationDurationUs";
        RenderedValue rendered = durationValue instanceof PropertyValue.IntegerValue integer
                ? scalar("const Duration(microseconds: " + integer.value() + ")", true,
                        durationPath, node.id(), context,
                        List.of(occurrence("widget:" + node.id() + ":material-core-duration",
                                6, "Duration", "dart:core", durationPath, Optional.of(node.id()))))
                : renderProperty(durationValue, duration, durationPath, node.id(), context);
        arguments.add(new ConstructorArgument(duration.parameter(), "animationDuration", false, rendered));
    }

    private void appendNavigationRailArguments(WidgetNode node, WidgetDefinition definition,
            String path, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition callback = definition
                .property(new PropertyName("onDestinationSelected")).orElseThrow();
        PropertyValue handler = node.properties().get(callback.name());
        if (handler == null) return;
        String callbackPath = path + "/properties/onDestinationSelected";
        RenderedValue rendered = handler instanceof PropertyValue.StringValue
                ? scalar("(_) {}", false, callbackPath, node.id(), context)
                : renderProperty(handler, callback, callbackPath, node.id(), context);
        arguments.add(new ConstructorArgument(callback.parameter(), "onDestinationSelected", false, rendered));
    }

    private void appendNavigationDrawerArguments(WidgetNode node, WidgetDefinition definition,
            String path, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition callback = definition
                .property(new PropertyName("onDestinationSelected")).orElseThrow();
        PropertyValue handler = node.properties().get(callback.name());
        if (handler == null) return;
        String callbackPath = path + "/properties/onDestinationSelected";
        RenderedValue rendered = handler instanceof PropertyValue.StringValue
                ? scalar("(_) {}", false, callbackPath, node.id(), context)
                : renderProperty(handler, callback, callbackPath, node.id(), context);
        arguments.add(new ConstructorArgument(callback.parameter(), "onDestinationSelected", false, rendered));
    }

    private void appendFloatingHeaderAnimationStyle(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition style = definition.property(new PropertyName("animationStyle")).orElseThrow();
        if (node.properties().get(style.name()) instanceof PropertyValue.StringValue preset) {
            arguments.add(new ConstructorArgument(style.parameter(), "animationStyle", false,
                    renderExpansionTilePreset("AnimationStyle", preset.value(), path + "/properties/animationStyle", node.id(), context)));
        }
        else if (node.properties().get(style.name()) != null) {
            arguments.add(new ConstructorArgument(style.parameter(), "animationStyle", false,
                    renderProperty(node.properties().get(style.name()), style, path + "/properties/animationStyle", node.id(), context)));
        }
        var members = new ArrayList<CompositeMember>();
        for (String name : dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.LOCAL_STYLE) {
            PropertyDefinition property = definition.property(new PropertyName(name)).orElseThrow();
            PropertyValue value = node.properties().get(property.name());
            if (value == null) continue;
            String valuePath = path + "/properties/" + name;
            RenderedValue rendered;
            if (value instanceof PropertyValue.IntegerValue duration) {
                // Retain implicit core scope, but require exact SDK navigation evidence.
                rendered = scalar("const Duration(microseconds: " + duration.value() + ")", true, valuePath, node.id(), context,
                        List.of(occurrence("widget:" + node.id() + ":sliver-floating-header-core-duration:" + name,
                                6, "Duration", "dart:core", valuePath, Optional.of(node.id()))));
            } else if (value instanceof PropertyValue.StringValue curve) {
                rendered = renderExpansionTilePreset("Curves", curve.value(), valuePath, node.id(), context);
            } else rendered = renderProperty(value, property, valuePath, node.id(), context);
            String suffix = name.substring("animationStyle".length());
            String dartName = Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
            if (dartName.endsWith("Us")) dartName = dartName.substring(0, dartName.length() - 2);
            members.add(new CompositeMember(dartName, members.size(), rendered));
        }
        if (!members.isEmpty()) arguments.add(new ConstructorArgument(style.parameter(), "animationStyle", false,
                renderNamedCompositeMembers("AnimationStyle", Optional.empty(), members, indent,
                        path + "/properties/animationStyle", node.id(), context)));
    }

    private void appendExpansionTileArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition callback = definition.property(new PropertyName("onExpansionChanged")).orElseThrow();
        PropertyValue handler = node.properties().get(callback.name());
        if (handler != null) arguments.add(new ConstructorArgument(callback.parameter(), "onExpansionChanged", false,
                handler instanceof PropertyValue.StringValue
                        ? scalar("(_) {}", false, path + "/properties/onExpansionChanged", node.id(), context)
                        : renderProperty(handler, callback, path + "/properties/onExpansionChanged", node.id(), context)));
        RenderedValue density = renderIconButtonDirectDensity(node, definition, path, context);
        if (density != null) arguments.add(new ConstructorArgument(definition.property(new PropertyName("visualDensity")).orElseThrow().parameter(), "visualDensity", false, density));
        for (String family : ExpansionTileWidgetPropertySchema.shapeFamilies()) appendCardShape(node, definition, path, indent, context, arguments, family);
        PropertyDefinition style = definition.property(new PropertyName("expansionAnimationStyle")).orElseThrow();
        if (node.properties().get(style.name()) instanceof PropertyValue.StringValue preset) {
            arguments.add(new ConstructorArgument(style.parameter(), "expansionAnimationStyle", false,
                    renderExpansionTilePreset("AnimationStyle", preset.value(), path + "/properties/expansionAnimationStyle", node.id(), context)));
        }
        var members = new ArrayList<CompositeMember>();
        for (String name : ExpansionTileWidgetPropertySchema.animationStyleLocalProperties()) {
            PropertyDefinition property = definition.property(new PropertyName(name)).orElseThrow();
            PropertyValue value = node.properties().get(property.name());
            if (value == null) continue;
            String valuePath = path + "/properties/" + name;
            RenderedValue rendered;
            if (value instanceof PropertyValue.IntegerValue duration) {
                // Retain implicit core scope, but require exact SDK navigation evidence.
                rendered = scalar("const Duration(microseconds: " + duration.value() + ")", true, valuePath, node.id(), context,
                        List.of(occurrence("widget:" + node.id() + ":expansion-tile-core-duration:" + name,
                                6, "Duration", "dart:core", valuePath, Optional.of(node.id()))));
            } else if (value instanceof PropertyValue.StringValue curve) {
                rendered = renderExpansionTilePreset("Curves", curve.value(), valuePath, node.id(), context);
            } else rendered = renderProperty(value, property, valuePath, node.id(), context);
            String suffix = name.substring("expansionAnimationStyle".length());
            String dartName = Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
            if (dartName.endsWith("Us")) dartName = dartName.substring(0, dartName.length() - 2);
            members.add(new CompositeMember(dartName, members.size(), rendered));
        }
        if (!members.isEmpty()) arguments.add(new ConstructorArgument(style.parameter(), "expansionAnimationStyle", false,
                renderNamedCompositeMembers("AnimationStyle", Optional.empty(), members, indent,
                        path + "/properties/expansionAnimationStyle", node.id(), context)));
    }

    private RenderedValue renderExpansionTilePreset(String owner, String member, String path,
            StableId widgetId, GenerationContext context) {
        RenderedSymbol symbol = context.planner().renderedSymbol(WIDGETS_IMPORT, owner);
        return scalar(symbol.text() + '.' + member, true, path, widgetId, context,
                List.of(occurrence("widget:" + widgetId + ":expansion-tile-preset:" + path, symbol.nameOffset(),
                        symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId)),
                        occurrence("widget:" + widgetId + ":expansion-tile-preset-member:" + path, symbol.text().length() + 1,
                                member, symbol.libraryUri(), path, Optional.of(widgetId))));
    }

    private void appendRadioIdentityArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments, List<String> names) {
        String selectedType = radioTypeSymbol(node, context).text()
                + (RadioWidgetPropertySchema.nullableValueType(node) ? "?" : "");
        for (String name : names) {
            PropertyName key = new PropertyName(name);
            PropertyValue value = node.properties().get(key);
            if (value == null) continue;
            PropertyDefinition property = definition.property(key).orElseThrow();
            String valuePath = path + "/properties/" + name;
            RenderedValue rendered;
            if (name.equals("onChanged") && value instanceof PropertyValue.StringValue) {
                rendered = scalar("(_) {}", false, valuePath, node.id(), context);
            } else if (value instanceof PropertyValue.EnumValue numeric && numeric.type().equals("double")) {
                String literal = switch (numeric.value()) {
                    case "infinity" -> "(1.0 / 0.0)";
                    case "negativeInfinity" -> "(-1.0 / 0.0)";
                    case "nan" -> "(0.0 / 0.0)";
                    default -> throw new IllegalArgumentException("Unreviewed Radio numeric constant");
                };
                rendered = scalar(literal, true, valuePath, node.id(), context);
            } else if (value instanceof PropertyValue.DartObjectReferenceValue reference) {
                String expected = switch (name) {
                    case "value" -> "Object";
                    case "groupValue" -> "Object?";
                    case "onChanged" -> "ValueChanged<Object?>";
                    default -> "RadioGroupRegistry<Object>";
                };
                rendered = renderDartObjectReference(reference, expected, valuePath, node.id(), context, Optional.of(selectedType));
            } else {
                rendered = renderProperty(value, property, valuePath, node.id(), context);
            }
            arguments.add(new ConstructorArgument(property.parameter(), name, false, rendered));
        }
    }

    private void appendListTileArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        for (String name : List.of("onTap", "onLongPress", "onFocusChange")) {
            PropertyDefinition property = definition.property(new PropertyName(name)).orElseThrow();
            PropertyValue value = node.properties().get(property.name());
            if (value == null) continue;
            String valuePath = path + "/properties/" + name;
            RenderedValue rendered = value instanceof PropertyValue.StringValue
                    ? scalar(name.equals("onFocusChange") ? "(_) {}" : "() {}", false, valuePath, node.id(), context)
                    : renderProperty(value, property, valuePath, node.id(), context);
            arguments.add(new ConstructorArgument(property.parameter(), name, false, rendered));
        }
        if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) {
            appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
        }
        RenderedValue density = renderIconButtonDirectDensity(node, definition, path, context);
        if (density != null) arguments.add(new ConstructorArgument(definition.property(new PropertyName("visualDensity")).orElseThrow().parameter(), "visualDensity", false, density));
        for (String family : ListTileWidgetPropertySchema.styleFamilies()) {
            appendTextCompoundArguments(node, definition, path, indent, context, arguments, family,
                    definition.property(new PropertyName(family)).orElseThrow().parameter().order(),
                    name -> ListTileWidgetPropertySchema.textStyleFamily(name).filter(family::equals)
                            .flatMap(ignored -> ListTileWidgetPropertySchema.textStyleBinding(name)));
        }
        for (String family : List.of("iconColor", "textColor", "mouseCursor")) {
            List<ElevatedButtonStateEntry> entries = new ArrayList<>();
            for (String state : ListTileWidgetPropertySchema.statePriority()) {
                String suffix = Character.toUpperCase(state.charAt(0)) + state.substring(1);
                PropertyName name = new PropertyName(family + suffix);
                PropertyValue value = node.properties().get(name);
                if (value == null) continue;
                RenderedValue rendered = family.equals("mouseCursor") && value instanceof PropertyValue.StringValue
                        ? renderElevatedCursor(node, definition, name.value(), path, context)
                        : renderProperty(value, definition.property(name).orElseThrow(), path + "/properties/" + name.value(), node.id(), context);
                entries.add(new ElevatedButtonStateEntry(state.equals("default") ? "any" : state, rendered));
            }
            if (!entries.isEmpty()) arguments.add(new ConstructorArgument(definition.property(new PropertyName(family)).orElseThrow().parameter(), family, false,
                    renderCheckboxStateMap(family.equals("mouseCursor") ? "WidgetStateMouseCursor" : "WidgetStateColor",
                            family.equals("mouseCursor") ? "MouseCursor" : "Color", entries, path + "/properties/" + family, node.id(), context, false)));
        }
    }

    private void appendRadioArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        appendRadioIdentityArguments(node, definition, path, context, arguments,
                List.of("value", "groupValue", "onChanged", "groupRegistry"));
        if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) {
            appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
        }
        RenderedValue density = renderIconButtonDirectDensity(node, definition, path, context);
        if (density != null) arguments.add(new ConstructorArgument(definition.property(new PropertyName("visualDensity")).orElseThrow().parameter(), "visualDensity", false, density));
        for (String family : List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius")) {
            ArrayList<ElevatedButtonStateEntry> entries = new ArrayList<>();
            for (String state : RadioWidgetPropertySchema.statePriority()) {
                String suffix = Character.toUpperCase(state.charAt(0)) + state.substring(1);
                PropertyName name = new PropertyName(family + suffix);
                PropertyValue value = node.properties().get(name);
                if (value != null) entries.add(new ElevatedButtonStateEntry(state.equals("default") ? "any" : state,
                        renderProperty(value, definition.property(name).orElseThrow(), path + "/properties/" + name, node.id(), context)));
            }
            if (!entries.isEmpty()) arguments.add(new ConstructorArgument(definition.property(new PropertyName(family)).orElseThrow().parameter(),
                    family, false, renderCheckboxStateMap("WidgetStateProperty", family.equals("innerRadius") ? "double" : "Color", entries,
                            path + "/properties/" + family, node.id(), context)));
        }
        appendCheckboxSideArguments(node, definition, path, indent, context, arguments);
    }

    private void appendRadioListTileArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        appendRadioIdentityArguments(node, definition, path, context, arguments, List.of("value", "groupValue", "onChanged"));
        var focusProperty = definition.property(new PropertyName("onFocusChange")).orElseThrow();
        PropertyValue focus = node.properties().get(focusProperty.name());
        if (focus != null) arguments.add(new ConstructorArgument(focusProperty.parameter(), "onFocusChange", false,
                focus instanceof PropertyValue.StringValue ? scalar("(_) {}", false, path + "/properties/onFocusChange", node.id(), context)
                        : renderProperty(focus, focusProperty, path + "/properties/onFocusChange", node.id(), context)));
        if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
        RenderedValue density = renderIconButtonDirectDensity(node, definition, path, context);
        if (density != null) arguments.add(new ConstructorArgument(definition.property(new PropertyName("visualDensity")).orElseThrow().parameter(), "visualDensity", false, density));
        for (String family : List.of("fillColor", "overlayColor", "radioBackgroundColor", "radioInnerRadius", "mouseCursor")) {
            List<ElevatedButtonStateEntry> entries = new ArrayList<>();
            for (String state : RadioListTileWidgetPropertySchema.statePriority()) {
                String name = family + Character.toUpperCase(state.charAt(0)) + state.substring(1);
                PropertyValue value = node.properties().get(new PropertyName(name));
                if (value == null) continue;
                RenderedValue rendered = family.equals("mouseCursor") && value instanceof PropertyValue.StringValue
                        ? renderElevatedCursor(node, definition, name, path, context)
                        : renderProperty(value, definition.property(new PropertyName(name)).orElseThrow(), path + "/properties/" + name, node.id(), context);
                entries.add(new ElevatedButtonStateEntry(state.equals("default") ? "any" : state, rendered));
            }
            if (!entries.isEmpty()) arguments.add(new ConstructorArgument(definition.property(new PropertyName(family)).orElseThrow().parameter(), family, false,
                    renderCheckboxStateMap(family.equals("mouseCursor") ? "WidgetStateMouseCursor" : "WidgetStateProperty",
                            family.equals("mouseCursor") ? "MouseCursor" : family.equals("radioInnerRadius") ? "double" : "Color",
                            entries, path + "/properties/" + family, node.id(), context, !family.equals("mouseCursor"))));
        }
        appendCheckboxSideArguments(node, definition, path, indent, context, arguments, "radioSide");
    }

    private void appendSwitchListTileArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        for (String name : List.of("onChanged", "onFocusChange", "onActiveThumbImageError", "onInactiveThumbImageError")) {
            PropertyDefinition property = definition.property(new PropertyName(name)).orElseThrow();
            PropertyValue value = node.properties().get(property.name());
            if (value == null) continue;
            String valuePath = path + "/properties/" + name;
            RenderedValue rendered = value instanceof PropertyValue.StringValue
                    ? scalar(name.endsWith("ImageError") ? "(_, __) {}" : "(_) {}", false, valuePath, node.id(), context)
                    : renderProperty(value, property, valuePath, node.id(), context);
            arguments.add(new ConstructorArgument(property.parameter(), name, false, rendered));
        }
        if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
        RenderedValue density = renderIconButtonDirectDensity(node, definition, path, context);
        if (density != null) arguments.add(new ConstructorArgument(definition.property(new PropertyName("visualDensity")).orElseThrow().parameter(), "visualDensity", false, density));
        appendSwitchStateArguments(node, definition, path, context, arguments);
        List<ElevatedButtonStateEntry> entries = new ArrayList<>();
        for (String state : SwitchListTileWidgetPropertySchema.statePriority()) {
            String name = "mouseCursor" + Character.toUpperCase(state.charAt(0)) + state.substring(1);
            PropertyValue value = node.properties().get(new PropertyName(name));
            if (value == null) continue;
            RenderedValue rendered = value instanceof PropertyValue.StringValue ? renderElevatedCursor(node, definition, name, path, context)
                    : renderProperty(value, definition.property(new PropertyName(name)).orElseThrow(), path + "/properties/" + name, node.id(), context);
            entries.add(new ElevatedButtonStateEntry(state.equals("default") ? "any" : state, rendered));
        }
        if (!entries.isEmpty()) arguments.add(new ConstructorArgument(definition.property(new PropertyName("mouseCursor")).orElseThrow().parameter(), "mouseCursor", false,
                renderCheckboxStateMap("WidgetStateMouseCursor", "MouseCursor", entries, path + "/properties/mouseCursor", node.id(), context, false)));
    }

    private void appendCheckboxListTileArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        for (String name : List.of("onChanged", "onFocusChange")) {
            PropertyDefinition property = definition.property(new PropertyName(name)).orElseThrow();
            PropertyValue value = node.properties().get(property.name());
            if (value == null) continue;
            String valuePath = path + "/properties/" + name;
            RenderedValue rendered = value instanceof PropertyValue.StringValue
                    ? scalar("(_) {}", false, valuePath, node.id(), context)
                    : renderProperty(value, property, valuePath, node.id(), context);
            arguments.add(new ConstructorArgument(property.parameter(), name, false, rendered));
        }
        if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) {
            appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
        }
        RenderedValue density = renderIconButtonDirectDensity(node, definition, path, context);
        if (density != null) arguments.add(new ConstructorArgument(definition.property(new PropertyName("visualDensity")).orElseThrow().parameter(), "visualDensity", false, density));
        appendCardShape(node, definition, path, indent, context, arguments, "checkboxShape");
        appendCheckboxStateColorArguments(node, definition, path, context, arguments);
        appendCheckboxSideArguments(node, definition, path, indent, context, arguments);
        List<ElevatedButtonStateEntry> entries = new ArrayList<>();
        for (String state : CheckboxListTileWidgetPropertySchema.statePriority()) {
            String name = "mouseCursor" + Character.toUpperCase(state.charAt(0)) + state.substring(1);
            PropertyValue value = node.properties().get(new PropertyName(name));
            if (value == null) continue;
            RenderedValue rendered = value instanceof PropertyValue.StringValue
                    ? renderElevatedCursor(node, definition, name, path, context)
                    : renderProperty(value, definition.property(new PropertyName(name)).orElseThrow(), path + "/properties/" + name, node.id(), context);
            entries.add(new ElevatedButtonStateEntry(state.equals("default") ? "any" : state, rendered));
        }
        if (!entries.isEmpty()) arguments.add(new ConstructorArgument(definition.property(new PropertyName("mouseCursor")).orElseThrow().parameter(), "mouseCursor", false,
                renderCheckboxStateMap("WidgetStateMouseCursor", "MouseCursor", entries, path + "/properties/mouseCursor", node.id(), context, false)));
    }

    private void appendCheckboxArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        PropertyDefinition changed = definition.property(new PropertyName("onChanged")).orElseThrow();
        PropertyValue reference = node.properties().get(changed.name());
        boolean enabled = new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("enabled")));
        RenderedValue callback = !enabled
                ? scalar("null", true, path + "/properties/enabled", node.id(), context)
                : reference == null ? scalar("(_) {}", false, path + "/properties/onChanged", node.id(), context)
                : renderProperty(reference, changed, path + "/properties/onChanged", node.id(), context);
        arguments.add(new ConstructorArgument(DartParameter.named(2, true), "onChanged", false, callback));
        if (node.properties().get(new PropertyName("mouseCursor")) instanceof PropertyValue.StringValue) {
            appendDefaultSelectionStyleCursor(node, definition, path, context, arguments);
        }
        RenderedValue density = renderIconButtonDirectDensity(node, definition, path, context);
        if (density != null) {
            arguments.add(new ConstructorArgument(DartParameter.named(12, false), "visualDensity", false, density));
        }
        appendCheckboxStateColorArguments(node, definition, path, context, arguments);
        appendCheckboxSideArguments(node, definition, path, indent, context, arguments);
    }

    private void appendCheckboxStateColorArguments(WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        for (String family : List.of("fillColor", "overlayColor")) {
            ArrayList<ElevatedButtonStateEntry> entries = new ArrayList<>();
            for (String state : CheckboxWidgetPropertySchema.statePriority()) {
                String suffix = Character.toUpperCase(state.charAt(0)) + state.substring(1);
                PropertyName name = new PropertyName(family + suffix);
                PropertyValue value = node.properties().get(name);
                if (value != null) {
                    entries.add(new ElevatedButtonStateEntry(state.equals("default") ? "any" : state,
                            renderProperty(value, definition.property(name).orElseThrow(),
                                    path + "/properties/" + name.value(), node.id(), context)));
                }
            }
            if (!entries.isEmpty()) {
                arguments.add(new ConstructorArgument(definition.property(new PropertyName(family)).orElseThrow().parameter(),
                        family, false, renderCheckboxStateMap("WidgetStateProperty", "Color", entries,
                                path + "/properties/" + family, node.id(), context)));
            }
        }
    }

    private void appendCheckboxSideArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments) {
        appendCheckboxSideArguments(node, definition, path, indent, context, arguments, "side");
    }

    private void appendCheckboxSideArguments(WidgetNode node, WidgetDefinition definition, String path,
            int indent, GenerationContext context, List<ConstructorArgument> arguments, String family) {
        DartParameter parameter = definition.property(new PropertyName(family)).orElseThrow().parameter();
        boolean stateful = new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName(family + "Stateful")));
        RenderedValue baseSide = renderCheckboxSide(node, definition, family, path, indent, context, false);
        if (!stateful) {
            if (baseSide != null) {
                arguments.add(new ConstructorArgument(parameter, family, false, baseSide));
            }
            return;
        }
        ArrayList<ElevatedButtonStateEntry> entries = new ArrayList<>();
        for (String state : CheckboxWidgetPropertySchema.sideStates()) {
            String prefix = family + state;
            PropertyValue mode = node.properties().get(new PropertyName(prefix + "Mode"));
            RenderedValue value = new PropertyValue.StringValue("inherit").equals(mode)
                    ? scalar("null", true, path + "/properties/" + prefix + "Mode", node.id(), context)
                    : renderCheckboxSide(node, definition, prefix, path, indent, context,
                            new PropertyValue.StringValue("border").equals(mode));
            if (value != null) {
                entries.add(new ElevatedButtonStateEntry(Character.toLowerCase(state.charAt(0)) + state.substring(1), value));
            }
        }
        entries.add(new ElevatedButtonStateEntry("any", baseSide == null
                ? scalar("null", true, path + "/properties/" + family + "Stateful", node.id(), context) : baseSide));
        arguments.add(new ConstructorArgument(parameter, family, false,
                renderCheckboxStateMap("WidgetStateBorderSide", "BorderSide", entries,
                        path + "/properties/" + family, node.id(), context)));
    }

    private RenderedValue renderCheckboxSide(WidgetNode node, WidgetDefinition definition, String prefix,
            String path, int indent, GenerationContext context, boolean force) {
        ArrayList<CompositeMember> members = new ArrayList<>();
        for (String suffix : List.of("Color", "Width", "Style", "StrokeAlign")) {
            addCardMember(members, node, definition, path, prefix + suffix,
                    Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1), members.size(), indent, context);
        }
        return members.isEmpty() && !force ? null : renderNamedCompositeMembers("BorderSide", Optional.empty(),
                members, indent, path + "/properties/" + prefix, node.id(), context);
    }

    /** Literal maps remain const; explicit null wins the first-match rule and defers to the SDK. */
    private RenderedValue renderCheckboxStateMap(String owner, String valueType,
            List<ElevatedButtonStateEntry> entries, String path, StableId widgetId, GenerationContext context) {
        return renderCheckboxStateMap(owner, valueType, entries, path, widgetId, context, true);
    }

    private RenderedValue renderCheckboxStateMap(String owner, String valueType,
            List<ElevatedButtonStateEntry> entries, String path, StableId widgetId, GenerationContext context, boolean nullableValue) {
        RenderedSymbol propertyType = context.planner().renderedSymbol(MATERIAL_IMPORT, owner);
        // Downward inference from Switch.trackOutlineWidth supplies double? without changing
        // the user's implicit dart:core import or depending on an unqualified core type name.
        boolean inferredNumericType = valueType.equals("double");
        RenderedSymbol valueSymbol = inferredNumericType ? null : context.planner().renderedSymbol(WIDGETS_IMPORT, valueType);
        RenderedSymbol constraintType = context.planner().renderedSymbol(MATERIAL_IMPORT, "WidgetStatesConstraint");
        RenderedSymbol stateType = context.planner().renderedSymbol(MATERIAL_IMPORT, "WidgetState");
        boolean constant = entries.stream().allMatch(entry -> entry.value().constant());
        StringBuilder rendered = new StringBuilder(constant ? "const " : "");
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        String id = "widget:" + widgetId + ":checkbox-map:" + path;
        appendElevatedSymbol(rendered, occurrences, propertyType, id + ":owner", path, widgetId);
        if (owner.equals("WidgetStateProperty") && !inferredNumericType) {
            rendered.append('<');
            appendElevatedSymbol(rendered, occurrences, valueSymbol, id + ":value-type", path, widgetId);
            rendered.append(nullableValue ? "?>" : ">");
        }
        rendered.append('.');
        occurrences.add(occurrence(id + ":factory", rendered.length(), "fromMap", propertyType.libraryUri(), path, Optional.of(widgetId)));
        rendered.append("fromMap(");
        if (!inferredNumericType) {
            rendered.append('<');
            appendElevatedSymbol(rendered, occurrences, constraintType, id + ":constraint-type", path, widgetId);
            rendered.append(", ");
            appendElevatedSymbol(rendered, occurrences, valueSymbol, id + ":map-value-type", path, widgetId);
            rendered.append(nullableValue ? "?>" : ">");
        }
        rendered.append('{');
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                rendered.append(", ");
            }
            ElevatedButtonStateEntry entry = entries.get(i);
            appendElevatedSymbol(rendered, occurrences, stateType, id + ":state:" + entry.state(), path, widgetId);
            rendered.append('.');
            occurrences.add(occurrence(id + ":state-member:" + entry.state(), rendered.length(), entry.state(),
                    stateType.libraryUri(), path, Optional.of(widgetId)));
            rendered.append(entry.state()).append(": ");
            appendRendered(rendered, occurrences, entry.value());
        }
        rendered.append("})");
        return scalar(rendered.toString(), constant, path, widgetId, context, occurrences);
    }

    private RenderedValue renderIconButtonDirectDensity(WidgetNode node,
            WidgetDefinition definition, String path, GenerationContext context) {
        ArrayList<CompositeMember> axes = new ArrayList<>();
        for (String axis : List.of("Horizontal", "Vertical")) {
            PropertyDefinition property = definition.property(new PropertyName("visualDensity" + axis)).orElseThrow();
            PropertyValue value = node.properties().get(property.name());
            if (value != null) {
                axes.add(new CompositeMember(axis.toLowerCase(java.util.Locale.ROOT), axes.size(),
                        renderProperty(value, property, path + "/properties/" + property.name().value(), node.id(), context)));
            }
        }
        return axes.isEmpty() ? null : renderNamedCompositeMembers(MATERIAL_IMPORT, "VisualDensity",
                Optional.empty(), axes, 0, path + "/properties/visualDensity", node.id(), context);
    }

    /** Repeated fixed composite expressions retain model paths but each emitted token needs its own evidence id. */
    private static RenderedValue scopedIconButtonValue(RenderedValue value, String scope) {
        return new RenderedValue(value.lines(), value.constant(), value.utf8Size(),
                value.symbolOccurrences().stream().map(symbol -> new GeneratedDartSymbolOccurrence(
                        symbol.id() + ":icon-button-use:" + scope, symbol.region(), symbol.offset(), symbol.length(),
                        symbol.symbolName(), symbol.libraryUri(), symbol.modelPath(), symbol.widgetId(),
                        symbol.staticTypeRequirement())).toList());
    }

    private void appendButtonDefaultPlaceholder(StringBuilder rendered,
            List<GeneratedDartSymbolOccurrence> occurrences, StableId widgetId,
            GenerationContext context, String path, String scope) {
        RenderedSymbol placeholder = context.planner().renderedSymbol(WIDGETS_IMPORT, "SizedBox");
        rendered.append("const ");
        appendElevatedSymbol(rendered, occurrences, placeholder,
                "widget:" + widgetId + ":button-style:default-placeholder:" + scope + ':' + path,
                path, widgetId);
        rendered.append("()");
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

        for (String state : enabledButtonStates(layers)) {
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
        for (String state : enabledButtonStates(layers)) {
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
        for (String state : enabledButtonStates(layers).reversed()) {
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
        for (ElevatedButtonState state : buttonStates(node)) {
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
                        OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)
                                ? TextButtonWidgetPropertySchema.sharedStyleDefinition(property.name().value())
                                : ElevatedButtonWidgetPropertySchema.find(property.name()).orElseThrow();
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
        if (OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)) {
            addElevatedCommonDirect(node, definition, "styleIconAlignment", "iconAlignment", 13,
                    path, context, style);
        }
        addElevatedCommonDirect(node, definition, "styleBackgroundBuilder", "backgroundBuilder", 23,
                path, context, style);
        addElevatedCommonDirect(node, definition, "styleForegroundBuilder", "foregroundBuilder", 24,
                path, context, style);
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
                MATERIAL_IMPORT, context.buttonFamily(node.id()).themeName());
        StringBuilder rendered = new StringBuilder("(() {\n");
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        appendElevatedFrameworkDefaultsDeclaration(
                rendered, occurrences, propertyPath, node.id(), context,
                "visualDensity");
        rendered.append("  final inherited = ");
        boolean directDensity = node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE)
                && (node.properties().containsKey(new PropertyName("visualDensityHorizontal"))
                || node.properties().containsKey(new PropertyName("visualDensityVertical")));
        if (directDensity) {
            appendRendered(rendered, occurrences, scopedIconButtonValue(
                    renderIconButtonDirectDensity(node, definition, path, context), "style-density-fallback"));
        } else {
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
        rendered.append(".standard");
        }
        rendered.append(";\n  return ");
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

    private RenderedValue renderRefreshNotificationPredicate(PropertyValue.StringValue preset,
            String propertyPath, StableId widgetId, GenerationContext context) {
        return switch (preset.value()) {
            case "default" -> {
                RenderedSymbol symbol = context.planner().renderedSymbol(
                        MATERIAL_IMPORT, "defaultScrollNotificationPredicate");
                yield scalar(symbol.text(), true, propertyPath, widgetId, context,
                        List.of(occurrence("widget:" + widgetId + ":refreshNotificationPredicate",
                                symbol.nameOffset(), symbol.name(), symbol.libraryUri(),
                                propertyPath, Optional.of(widgetId))));
            }
            case "depthZero" -> scalar("(notification) => notification.depth == 0",
                    false, propertyPath, widgetId, context);
            case "all" -> scalar("(_) => true", false, propertyPath, widgetId, context);
            default -> throw catalogInconsistency(propertyPath, widgetId,
                    "Unsupported validated RefreshIndicator notification predicate preset '" + preset.value() + "'.");
        };
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
        if (member.value() instanceof PropertyValue.DartObjectReferenceValue) {
            arguments.add(new ConstructorArgument(
                    member.property().parameter(), "notificationPredicate", false, member.rendered()));
            return;
        }
        if (!(member.value() instanceof PropertyValue.StringValue preset)) {
            throw catalogInconsistency(
                    member.propertyPath(path), widgetId,
                    "AppBar notificationPredicate must be a reviewed string preset or typed project reference.");
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

    private void appendCardShape(WidgetNode node, WidgetDefinition definition, String path, int indent,
            GenerationContext context, List<ConstructorArgument> arguments) {
        appendCardShape(node, definition, path, indent, context, arguments, "shape");
    }

    private void appendCardShape(WidgetNode node, WidgetDefinition definition, String path, int indent,
            GenerationContext context, List<ConstructorArgument> arguments, String family) {
        PropertyValue kindValue = node.properties().get(new PropertyName(family + "Kind"));
        if (!(kindValue instanceof PropertyValue.StringValue kind)) return;
        ArrayList<CompositeMember> members = new ArrayList<>();
        ArrayList<CompositeMember> side = new ArrayList<>();
        for (String suffix : List.of("Color", "Width", "Style", "StrokeAlign")) {
            addCardMember(side, node, definition, path, family + "Side" + suffix,
                    Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1), side.size(), indent + 2, context);
        }
        if (!side.isEmpty()) members.add(new CompositeMember("side", 0,
                renderNamedCompositeMembers("BorderSide", Optional.empty(), side, indent + 2,
                        path + "/properties/" + family + "/side", node.id(), context)));
        addCardMember(members, node, definition, path, family + "Radius", "borderRadius", 1, indent + 2, context);
        addCardMember(members, node, definition, path, family + "CircleEccentricity", "eccentricity", 1, indent + 2, context);
        addCardMember(members, node, definition, path, family + "Points", kind.value().equals("polygon") ? "sides" : "points", 1, indent + 2, context);
        for (String suffix : List.of("InnerRadiusRatio", "PointRounding", "ValleyRounding", "Rotation", "Squash")) {
            addCardMember(members, node, definition, path, family + suffix,
                    Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1), members.size() + 2, indent + 2, context);
        }
        for (String edge : List.of("Start", "End", "Top", "Bottom")) {
            ArrayList<CompositeMember> fields = new ArrayList<>();
            addCardMember(fields, node, definition, path, family + edge + "Size", "size", 0, indent + 4, context);
            addCardMember(fields, node, definition, path, family + edge + "Alignment", "alignment", 1, indent + 4, context);
            if (!fields.isEmpty()) {
                String name = Character.toLowerCase(edge.charAt(0)) + edge.substring(1);
                members.add(new CompositeMember(name, members.size() + 2,
                        renderNamedCompositeMembers("LinearBorderEdge", Optional.empty(), fields, indent + 2,
                                path + "/properties/" + family + "/" + name, node.id(), context)));
            }
        }
        String dartClass = switch (kind.value()) {
            case "roundedRectangle" -> "RoundedRectangleBorder";
            case "beveledRectangle" -> "BeveledRectangleBorder";
            case "continuousRectangle" -> "ContinuousRectangleBorder";
            case "roundedSuperellipse" -> "RoundedSuperellipseBorder";
            case "circle" -> "CircleBorder";
            case "oval" -> "OvalBorder";
            case "stadium" -> "StadiumBorder";
            case "linear" -> "LinearBorder";
            case "star", "polygon" -> "StarBorder";
            default -> throw catalogInconsistency(path + "/properties/" + family + "Kind", node.id(), "Unknown Card shape");
        };
        RenderedValue rendered = renderNamedCompositeMembers(dartClass,
                kind.value().equals("polygon") ? Optional.of("polygon") : Optional.empty(),
                members, indent, path + "/properties/" + family, node.id(), context);
        if (kind.value().equals("polygon")) {
            var symbol = rendered.symbolOccurrences().getFirst();
            var occurrences = new ArrayList<>(rendered.symbolOccurrences());
            occurrences.add(1, occurrence("widget:" + node.id() + ":cardPolygonConstructor" + (family.equals("shape") ? "" : ":" + family), symbol.endOffset() + 1,
                    "polygon", symbol.libraryUri(), path + "/properties/" + family + "Kind", Optional.of(node.id())));
            rendered = new RenderedValue(rendered.lines(), rendered.constant(), rendered.utf8Size(), occurrences);
        }
        arguments.add(new ConstructorArgument(definition.property(new PropertyName(family)).orElseThrow().parameter(),
                family, false, rendered));
    }

    private void addCardMember(List<CompositeMember> members, WidgetNode node, WidgetDefinition definition, String path,
            String propertyName, String dartName, int order, int indent, GenerationContext context) {
        PropertyName name = new PropertyName(propertyName);
        PropertyValue value = node.properties().get(name);
        if (value != null) members.add(new CompositeMember(dartName, order,
                renderProperty(value, definition.property(name).orElseThrow(), path + "/properties/" + propertyName,
                        node.id(), context, indent)));
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
        boolean badge = node.type().equals(BadgeWidgetPropertySchema.BADGE_TYPE);
        boolean floating = node.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE);
        String styleArgument = floating ? "extendedTextStyle" : badge ? "textStyle" : "style";
        int styleOrder = floating ? 25 : badge ? 12 : 0;
        appendTextCompoundArguments(node, definition, path, valueIndent, context, arguments, styleArgument, styleOrder,
                name -> floating ? FloatingActionButtonWidgetPropertySchema.textStyleBinding(name)
                        : badge ? BadgeWidgetPropertySchema.textStyleBinding(name) : TextWidgetPropertySchema.find(name));
    }

    private void appendTextCompoundArguments(WidgetNode node, WidgetDefinition definition, String path,
            int valueIndent, GenerationContext context, List<ConstructorArgument> arguments, String styleArgument, int styleOrder,
            java.util.function.Function<PropertyName, Optional<TextWidgetPropertySchema.Definition>> bindings) {
        Map<TextWidgetPropertySchema.Target, List<TextMember>> grouped = new HashMap<>();
        for (PropertyDefinition property : definition.properties()) {
            TextWidgetPropertySchema.Definition binding = bindings.apply(property.name()).orElse(null);
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
                valueIndent + 2, path + "/properties/" + styleArgument + "Locale", node.id(), context);
        if (styleLocale != null) {
            styleMembers.add(new CompositeMember("locale", 11, styleLocale));
        }
        RenderedValue decoration = renderTextDecoration(
                grouped.get(TextWidgetPropertySchema.Target.TEXT_STYLE_DECORATION),
                path + "/properties/" + styleArgument + "Decoration", node.id(), context);
        if (decoration != null) {
            styleMembers.add(new CompositeMember("decoration", 17, decoration));
        }
        List<TextMember> themeStyles = grouped.get(
                TextWidgetPropertySchema.Target.TEXT_STYLE_THEME);
        RenderedValue themeStyle = themeStyles == null || themeStyles.isEmpty()
                ? null : themeStyles.getFirst().rendered();
        if (themeStyle != null && styleMembers.isEmpty()) {
            arguments.add(new ConstructorArgument(
                    DartParameter.named(styleOrder, false), styleArgument, false,
                    DefaultTextStyleWidgetPropertySchema.requiresStyle(node.type())
                        ? renderTextStyleCopyWith(themeStyle, List.of(), path + "/properties/style", node.id(), context) : themeStyle));
        } else if (themeStyle != null) {
            styleMembers.sort(COMPOSITE_MEMBER_ORDER);
            arguments.add(new ConstructorArgument(
                    DartParameter.named(styleOrder, false),
                    styleArgument,
                    false,
                    renderTextStyleCopyWith(
                            themeStyle, styleMembers, path + "/properties/" + styleArgument,
                            node.id(), context)));
        } else if (!styleMembers.isEmpty()) {
            styleMembers.sort(COMPOSITE_MEMBER_ORDER);
            arguments.add(new ConstructorArgument(
                    DartParameter.named(styleOrder, false),
                    styleArgument,
                    false,
                    renderNamedCompositeMembers(
                            "TextStyle", Optional.empty(), styleMembers,
                            valueIndent, path + "/properties/" + styleArgument, node.id(), context)));
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
                        "widget:" + widgetId + ":theme-token:" + token.wireId() + ':' + path,
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
                    "widget:" + widgetId + ":shadow:" + shadow.id() + ':' + itemPath,
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
                    "widget:" + widgetId + ":shadow-offset:" + shadow.id() + ':' + itemPath,
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
                            "widget:" + widgetId + ":font-feature:" + feature.id() + ':' + itemPath,
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
                            "widget:" + widgetId + ":font-variation:" + variation.id() + ':' + itemPath,
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
                "widget:" + widgetId + ':' + occurrenceKind + ":element-type:" + path,
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
                "widget:" + widgetId + ":theme-style-fallback"
                        + (ListTileWidgetPropertySchema.styleFamilies().stream().anyMatch(family -> path.endsWith("/" + family)) ? ":" + path : ""),
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

    private static boolean isNavigationDestinationsSlot(WidgetNode node, SlotName slotName) {
        if (slotName.value().equals("destinations")) {
            return node.type().equals(NavigationBarWidgetPropertySchema.NAVIGATION_BAR_TYPE)
                    || node.type().equals(NavigationRailWidgetPropertySchema.NAVIGATION_RAIL_TYPE);
        }
        if (slotName.value().equals("items")) {
            return node.type().equals(BottomNavigationBarWidgetPropertySchema.BOTTOM_NAVIGATION_BAR_TYPE);
        }
        return slotName.value().equals("children")
                && node.type().equals(NavigationDrawerWidgetPropertySchema.NAVIGATION_DRAWER_TYPE);
    }

    /**
     * The designer stores destination entries as ordinary widget children so
     * that every palette widget can be dropped into the list. Flutter's
     * NavigationBar/Rail APIs, however, require Navigation*Destination
     * objects. Keep that model convenient while emitting the exact SDK type,
     * using each child as the destination icon and a deterministic label.
     */
    private RenderedValue renderNavigationDestinations(
            WidgetSlot slot,
            String path,
            int baseIndent,
            GenerationContext context,
            boolean rail,
            boolean drawer,
            boolean bottomNavigation) {
        if (!(slot instanceof WidgetSlot.ListSlot destinations)) {
            return renderSlot(slot, path, baseIndent, context);
        }
        if (destinations.children().isEmpty()) {
            return scalar("[]", true, path, Optional.empty(), context);
        }
        LineAccumulator lines = new LineAccumulator(
                context.maxRenderedUtf8Bytes(), path, null);
        lines.add("[");
        int childIndent = baseIndent + 2;
        boolean constant = true;
        String destinationType = drawer
                ? "NavigationDrawerDestination"
                : rail ? "NavigationRailDestination"
                : bottomNavigation ? "BottomNavigationBarItem" : "NavigationDestination";
        for (int index = 0; index < destinations.children().size(); index++) {
            WidgetNode child = destinations.children().get(index);
            RenderedValue destination = renderNavigationDestination(
                    child,
                    path + "/children/" + index,
                    childIndent,
                    context,
                    destinationType,
                    index,
                    bottomNavigation);
            constant &= destination.constant();
            String emitted = spaces(childIndent) + destination.joined() + ',';
            lines.addBlock(
                    emitted,
                    destination.symbolOccurrences(),
                    spaces(childIndent).length());
        }
        lines.add(spaces(baseIndent) + "]");
        return lines.build(constant);
    }

    private RenderedValue renderNavigationDestination(
            WidgetNode child,
            String path,
            int baseIndent,
            GenerationContext context,
            String destinationType,
            int index,
            boolean bottomNavigation) {
        RenderedValue icon = renderNode(child, path + "/icon", baseIndent + 2, context);
        RenderedSymbol destinationSymbol = context.planner().renderedSymbol(
                MATERIAL_IMPORT, destinationType);
        RenderedSymbol textSymbol = context.planner().renderedSymbol(WIDGETS_IMPORT, "Text");
        boolean constant = icon.constant();
        String constructor = (constant ? "const " : "") + destinationSymbol.text() + "(";
        LineAccumulator lines = new LineAccumulator(
                context.maxRenderedUtf8Bytes(), path, child.id());
        lines.add(
                constructor,
                List.of(occurrence(
                        "widget:" + child.id() + ":navigation-destination-constructor",
                        (constant ? "const ".length() : 0) + destinationSymbol.nameOffset(),
                        destinationSymbol.name(),
                        destinationSymbol.libraryUri(),
                        path,
                        Optional.of(child.id()))));
        int argumentIndent = baseIndent + 2;
        String iconPrefix = spaces(argumentIndent) + "icon: ";
        lines.addBlock(
                iconPrefix + icon.joined() + ",",
                icon.symbolOccurrences(),
                iconPrefix.length());
        String labelPrefix;
        String label;
        if (bottomNavigation) {
            labelPrefix = spaces(argumentIndent) + "label: ";
            label = labelPrefix + "'Item " + (index + 1) + "',";
        } else {
            labelPrefix = spaces(argumentIndent) + "label: const ";
            label = labelPrefix + textSymbol.text() + "('Destination " + (index + 1) + "'),";
        }
        lines.add(label, bottomNavigation ? List.of() : List.of(occurrence(
                "widget:" + child.id() + ":navigation-destination-label",
                labelPrefix.length() + textSymbol.nameOffset(), textSymbol.name(),
                textSymbol.libraryUri(), path, Optional.of(child.id()))));
        lines.add(spaces(baseIndent) + ")");
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

    private void appendDefaultSelectionStyleCursor(
            WidgetNode node, WidgetDefinition definition, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        appendMouseCursorPreset(node, definition, "mouseCursor", path, context, arguments);
    }

    private void appendMouseCursorPreset(
            WidgetNode node, WidgetDefinition definition, String propertyName, String path,
            GenerationContext context, List<ConstructorArgument> arguments) {
        var name = new PropertyName(propertyName);
        PropertyValue value = node.properties().get(name);
        if (value == null) return;
        String propertyPath = path + "/properties/" + propertyName;
        if (!(value instanceof PropertyValue.StringValue preset)) {
            throw catalogInconsistency(propertyPath, node.id(), "Mouse cursor must be a validated preset string.");
        }
        String owner = preset.value().equals("defer") || preset.value().equals("uncontrolled")
                ? "MouseCursor" : Set.of("clickable", "adaptiveClickable", "textable").contains(preset.value())
                        ? "WidgetStateMouseCursor" : "SystemMouseCursors";
        RenderedSymbol type = context.planner().renderedSymbol(WIDGETS_IMPORT, owner);
        arguments.add(new ConstructorArgument(definition.property(name).orElseThrow().parameter(), propertyName, false,
                scalar(type.text() + '.' + preset.value(), true, propertyPath, node.id(), context,
                        List.of(occurrence("widget:" + node.id() + ":selectionCursor", type.nameOffset(),
                                type.name(), type.libraryUri(), propertyPath, Optional.of(node.id())),
                                occurrence("widget:" + node.id() + ":selectionCursorMember", type.text().length() + 1,
                                        preset.value(), type.libraryUri(), propertyPath, Optional.of(node.id()))))));
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
                || node.type().equals(GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE)
                || node.type().equals(GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE)
                || node.type().equals(PageViewWidgetPropertySchema.PAGE_VIEW_TYPE)
                || node.type().equals(ListWheelScrollViewWidgetPropertySchema.LIST_WHEEL_SCROLL_VIEW_TYPE)
                || node.type().equals(CustomScrollViewWidgetPropertySchema.CUSTOM_SCROLL_VIEW_TYPE);
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
        if (value instanceof PropertyValue.DartObjectReferenceValue reference) {
            PropertyValueConstraint.DartObjectReferenceValues constraint =
                    definition.constraints().stream()
                            .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                            .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast)
                            .findFirst()
                            .orElseThrow(() -> catalogInconsistency(
                            path, widgetId,
                            "A Dart object reference has no expected static-type constraint."));
            return renderDartObjectReference(
                    reference,
                    constraint.expectedDartType(),
                    path,
                    widgetId,
                    context);
        }
        if (value instanceof PropertyValue.NullValue) {
            return scalar("null", true, path, widgetId, context);
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
        if (value instanceof PropertyValue.BorderRadiusValue borderRadius) {
            return renderBorderRadius(
                    borderRadius.geometry(), valueIndent,
                    path + "/geometry", widgetId, context);
        }
        if (value instanceof PropertyValue.ShapeBorderClipperValue clipper) {
            String shapeClass = switch (clipper.shape()) {
                case ROUNDED_RECTANGLE -> "RoundedRectangleBorder";
                case BEVELED_RECTANGLE -> "BeveledRectangleBorder";
                case CONTINUOUS_RECTANGLE -> "ContinuousRectangleBorder";
                case ROUNDED_SUPERELLIPSE -> "RoundedSuperellipseBorder";
                case CIRCLE -> "CircleBorder";
                case STADIUM -> "StadiumBorder";
            };
            List<CompositeMember> shapeMembers = clipper.shape().supportsRadius()
                    ? List.of(new CompositeMember("borderRadius", 0, renderBorderRadius(
                            clipper.borderRadius(), valueIndent + 4, path + "/borderRadius", widgetId, context)))
                    : List.of();
            ArrayList<CompositeMember> members = new ArrayList<>();
            members.add(new CompositeMember("shape", 0, renderNamedCompositeMembers(
                    shapeClass, Optional.empty(), shapeMembers, valueIndent + 2,
                    path + "/shape", widgetId, context)));
            clipper.textDirection().ifPresent(direction -> members.add(new CompositeMember(
                    "textDirection", 1, renderEnumSymbol("TextDirection", direction.wireName(),
                            "shape-clipper-direction", path + "/textDirection", widgetId, context))));
            return renderNamedCompositeMembers("ShapeBorderClipper", Optional.empty(), members,
                    valueIndent, path, widgetId, context);
        }
        if (value instanceof PropertyValue.BoxDecorationValue decoration) {
            return renderBoxDecoration(
                    decoration, valueIndent, path, widgetId, context);
        }
        if (value instanceof PropertyValue.PointerDeviceKindSetValue devices) {
            RenderedSymbol symbol = context.planner().renderedSymbol(
                    DART_UI_IMPORT, "PointerDeviceKind");
            StringBuilder expression = new StringBuilder("const <");
            ArrayList<GeneratedDartSymbolOccurrence> symbols = new ArrayList<>();
            String occurrencePrefix = "widget:" + widgetId + ":property:"
                    + definition.name().value() + ":device-type";
            symbols.add(occurrence(occurrencePrefix,
                    expression.length() + symbol.nameOffset(), symbol.name(),
                    symbol.libraryUri(), path, Optional.of(widgetId)));
            expression.append(symbol.text()).append(">{");
            for (var device : devices.values()) {
                if (expression.charAt(expression.length() - 1) != '{') {
                    expression.append(", ");
                }
                symbols.add(occurrence(occurrencePrefix + ":" + device.wireName(),
                        expression.length() + symbol.nameOffset(), symbol.name(),
                        symbol.libraryUri(), path, Optional.of(widgetId)));
                expression.append(symbol.text()).append('.').append(device.wireName());
            }
            expression.append('}');
            return scalar(expression.toString(), true, path, widgetId, context, symbols);
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
            if (binding.dartType().libraryUri().equals("dart:core")
                    && binding.dartType().name().equals("double")
                    && enumValue.type().equals("double")
                    && Set.of("infinity", "negativeInfinity", "nan").contains(enumValue.value())) {
                // A prefixed dart:core import suppresses Dart's implicit core scope.
                // These fixed, compile-time IEEE expressions preserve signed infinity
                // without changing user imports or accepting arbitrary expressions.
                return scalar(switch (enumValue.value()) {
                    case "negativeInfinity" -> "(-1.0 / 0.0)";
                    case "nan" -> "(0.0 / 0.0)";
                    default -> "(1.0 / 0.0)";
                },
                        true, path, widgetId, context);
            }
            RenderedSymbol symbol = context.planner().renderedSymbol(binding.dartType());
            return scalar(
                    symbol.text() + "." + enumValue.value(),
                    true,
                    path,
                    widgetId,
                    context,
                    List.of(occurrence(
                            "widget:" + widgetId + ":property:" + definition.name().value() + ":enum-type",
                            symbol.nameOffset(), symbol.name(), symbol.libraryUri(), path, Optional.of(widgetId))));
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
            var typedContract = definition.constraints().stream()
                    .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst();
            if (typedContract.isPresent()) {
                // The persisted shorthand must carry the same proof as its typed
                // reference equivalent, including when source ignores diagnostics.
                return renderDartObjectReference(new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), callback.handler(), Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                        typedContract.orElseThrow().expectedDartType(), path, widgetId, context);
            }
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

    private RenderedValue renderDartObjectReference(
            PropertyValue.DartObjectReferenceValue value,
            String expectedDartType,
            String path,
            StableId widgetId,
            GenerationContext context) {
        return renderDartObjectReference(value, expectedDartType, path, widgetId, context, Optional.empty());
    }

    private RenderedValue renderDartObjectReference(
            PropertyValue.DartObjectReferenceValue value, String expectedDartType, String path,
            StableId widgetId, GenerationContext context, Optional<String> sourceTypeOverride) {
        String constantPrefix = value.constant().orElse(false) ? "const " : "";
        String rootText;
        int rootNameOffset;
        String libraryUri;
        if (value.libraryUri().isPresent()) {
            RenderedSymbol root = context.planner().renderedSymbol(
                    value.libraryUri().orElseThrow(), value.rootSymbol());
            rootText = root.text();
            rootNameOffset = root.nameOffset();
            libraryUri = root.libraryUri();
        } else {
            rootText = value.rootSymbol();
            rootNameOffset = 0;
            libraryUri = CURRENT_PROJECT_LIBRARY_URI;
        }

        StringBuilder rendered = new StringBuilder(constantPrefix).append(rootText);
        int rootOffset = constantPrefix.length() + rootNameOffset;
        int[] memberOffset = {-1};
        value.member().ifPresent(member -> {
            rendered.append('.');
            memberOffset[0] = rendered.length();
            rendered.append(member);
        });
        if (value.access()
                == PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION) {
            rendered.append("()");
        }
        GeneratedDartStaticTypeRequirement staticTypeRequirement =
                new GeneratedDartStaticTypeRequirement(
                        0, rendered.length(), expectedDartType, sourceTypeOverride);
        ArrayList<GeneratedDartSymbolOccurrence> occurrences = new ArrayList<>();
        occurrences.add(occurrence(
                "widget:" + widgetId + ":property-reference:" + path + ":root",
                rootOffset,
                value.rootSymbol(),
                libraryUri,
                path + "/rootSymbol",
                Optional.of(widgetId),
                value.member().isEmpty()
                        ? Optional.of(staticTypeRequirement) : Optional.empty()));
        value.member().ifPresent(member -> occurrences.add(occurrence(
                "widget:" + widgetId + ":property-reference:" + path + ":member",
                memberOffset[0],
                member,
                libraryUri,
                path + "/member",
                Optional.of(widgetId),
                Optional.of(staticTypeRequirement))));
        return scalar(
                rendered.toString(),
                value.access()
                        == PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                        && value.constant().orElseThrow(),
                path,
                widgetId,
                context,
                occurrences);
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

    private static GeneratedDartSymbolOccurrence occurrence(
            String id,
            int offset,
            String symbolName,
            String libraryUri,
            String modelPath,
            Optional<StableId> widgetId,
            Optional<GeneratedDartStaticTypeRequirement> staticTypeRequirement) {
        return new GeneratedDartSymbolOccurrence(
                id,
                DartManagedRegionId.BUILD,
                offset,
                symbolName.length(),
                symbolName,
                libraryUri,
                modelPath,
                widgetId,
                staticTypeRequirement);
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
            int maxRenderedUtf8Bytes,
            Map<StableId, ButtonFamily> buttonFamilies) {
        private GenerationContext(WidgetCatalog catalog, DartImportPlan importPlan,
                ImportPlanner planner, int maxRenderedUtf8Bytes) {
            this(catalog, importPlan, planner, maxRenderedUtf8Bytes, new LinkedHashMap<>());
        }

        ButtonFamily buttonFamily(StableId widgetId) {
            return buttonFamilies.getOrDefault(widgetId,
                    new ButtonFamily("ElevatedButton", "ElevatedButtonTheme", false, false, "", null));
        }
    }

    private record ButtonFamily(String className, String themeName,
            boolean iconConstructor, boolean iconPresent, String constructorName, WidgetNode node) { }

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
                Iterable<String> valueImportUris,
                int maximumImports,
                boolean requiresMaterialTheme,
                boolean requiresServices,
                boolean requiresGestures,
                boolean requiresRendering,
                boolean requiresDartConvert,
                boolean requiresDartUi) {
            TreeSet<String> uris = new TreeSet<>();
            uris.add(requiresMaterialTheme ? MATERIAL_IMPORT : WIDGETS_IMPORT);
            for (String uri : valueImportUris) {
                uris.add(uri);
            }
            if (requiresDartConvert) {
                uris.add(DART_CONVERT_IMPORT);
            }
            if (requiresServices) {
                uris.add(SERVICES_IMPORT);
            }
            for (WidgetDefinition definition : definitions) {
                for (String uri : definition.importUris()) {
                    if ((definition.typeId().equals(dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.TYPE)
                            || definition.typeId().equals(dev.flutter.netbeans.designer.catalog.SliverConstrainedCrossAxisWidgetPropertySchema.TYPE)
                            || definition.typeId().equals(CircleAvatarWidgetPropertySchema.CIRCLE_AVATAR_TYPE)
                            || definition.typeId().equals(LinearProgressIndicatorWidgetPropertySchema.LINEAR_PROGRESS_INDICATOR_TYPE)
                            || definition.typeId().equals(CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE)
                            || definition.typeId().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE)
                            || definition.typeId().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE)
                            || definition.typeId().equals(CheckboxWidgetPropertySchema.CHECKBOX_TYPE)
                            || definition.typeId().equals(CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE)
                            || definition.typeId().equals(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE)
                            || definition.typeId().equals(RadioWidgetPropertySchema.RADIO_TYPE)
                            || definition.typeId().equals(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE)
                            || definition.typeId().equals(ExpansionTileWidgetPropertySchema.EXPANSION_TILE_TYPE)
                            || definition.typeId().equals(TooltipWidgetPropertySchema.TOOLTIP_TYPE)
                            || definition.typeId().equals(TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE)
                            || definition.typeId().equals(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE)
                            || definition.typeId().equals(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE)
                            || definition.typeId().equals(ListTileWidgetPropertySchema.LIST_TILE_TYPE)
                            || definition.typeId().equals(SwitchWidgetPropertySchema.SWITCH_TYPE)
                            || definition.typeId().equals(SliderWidgetPropertySchema.SLIDER_TYPE)
                            || definition.typeId().equals(RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE))
                            && uri.equals("dart:core") && !uris.contains(uri)) {
                        continue;
                    }
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
