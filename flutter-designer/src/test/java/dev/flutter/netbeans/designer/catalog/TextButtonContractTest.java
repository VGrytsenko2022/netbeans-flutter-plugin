package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.TextButtonTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class TextButtonContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void completeConstructorAndAllNineSparseStateBucketsFitOneClosed511RowContract() {
        var definition = definition();
        assertEquals(511, definition.properties().size());
        assertEquals(511, TextButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT);
        assertEquals(498, TextButtonWidgetPropertySchema.localStyleProperties().size());
        assertEquals(9, TextButtonWidgetPropertySchema.statePrefixes().size());
        assertEquals(12, TextButtonWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(12, TextButtonWidgetPropertySchema.COMMON_STYLE_PROPERTY_COUNT);
        assertEquals("TextButton", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.material", 100, 140, "TextButton"), definition.palette());
        assertEquals(new ArrayList<>(TextButtonWidgetPropertySchema.definitions().keySet()),
                definition.properties().stream().map(value -> value.name().value()).toList());
        for (int index = 0; index < definition.properties().size(); index++) {
            var property = definition.properties().get(index);
            boolean required = Set.of("enabled", "variant").contains(property.name().value());
            assertEquals(DartParameter.named(index, required), property.parameter(), property.name().value());
            assertEquals(required, property.creationDefault().isPresent());
            assertFalse(TextButtonWidgetPropertySchema.find(property.name()).orElseThrow().description().isBlank());
        }
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(p("enabled"), new PropertyValue.BooleanValue(true), p("variant"), s("standard")), prototype.properties());
        assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertEquals(DartParameter.named(511, true), definition.slots().getFirst().parameter());
        assertEquals(1, definition.slots().getFirst().minChildren());
        assertEquals(DartParameter.named(512, false), definition.slots().getLast().parameter());
        assertEquals(0, definition.slots().getLast().minChildren());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void activationPreservesNoOpLongPressOnlyAndDisabledReferencesWithoutExecutingThem() {
        var fresh = generated(node(Map.of()));
        assertTrue(fresh.build().payload().contains("onPressed: () {}"));
        assertFalse(fresh.build().payload().contains("const TextButton("));
        assertFalse(fresh.build().payload().contains("enabled:"));
        assertFalse(fresh.build().payload().contains("variant:"));
        var longOnly = generated(node(Map.of(p("onLongPress"), reference("longPress"))));
        assertTrue(longOnly.build().payload().contains("onPressed: null"));
        assertTrue(longOnly.build().payload().contains("buttonValues.longPress"));
        var disabled = generated(node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false),
                p("onPressed"), reference("press"), p("onLongPress"), reference("longPress"))));
        assertTrue(disabled.build().payload().contains("const TextButton("));
        assertTrue(disabled.build().payload().contains("onPressed: null"));
        assertTrue(disabled.build().payload().contains("onLongPress: null"));
        assertFalse(disabled.build().payload().contains("buttonValues"));
        assertEquals(generated(node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false)))).imports().payload(),
                disabled.imports().payload(), "Inactive stored callbacks must not add unused imports to live Dart.");
        assertTrue(disabled.symbolOccurrences().stream().noneMatch(symbol -> symbol.modelPath().contains("onPressed")
                || symbol.modelPath().contains("onLongPress")));
        for (String required : List.of("enabled", "variant")) {
            var values = new LinkedHashMap<>(node(Map.of()).properties());
            values.remove(p(required));
            assertFalse(valid(new WidgetNode(StableId.random(), definition().typeId(), values, node(Map.of()).slots())));
        }
    }

    @Test
    void bothConstructorsRetainStableChildAndOptionalIconAndEnforceBranchSpecificArguments() {
        var base = node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false)));
        assertTrue(generated(base).build().payload().contains("child: const Text('Label')"));
        for (boolean hasIcon : List.of(false, true)) {
            var icon = iconNode(Map.of(p("enabled"), new PropertyValue.BooleanValue(false)), hasIcon);
            var output = generated(icon);
            String dart = output.build().payload();
            assertTrue(dart.contains("TextButton.icon("), dart);
            assertFalse(dart.contains("const TextButton.icon("), dart);
            assertTrue(dart.contains("label: const Text('Label')"), dart);
            assertFalse(dart.contains("child:"), dart);
            assertEquals(hasIcon, dart.contains("icon: const Text('Icon')"));
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals("icon")
                    && symbol.modelPath().equals("/root/properties/variant")));
            assertSymbols(output);
            assertFalse(valid(iconNode(Map.of(p("isSemanticButton"), new PropertyValue.NullValue()), hasIcon)));
        }
        assertFalse(valid(node(Map.of(p("iconAlignment"), new PropertyValue.EnumValue("IconAlignment", "end")))));
        var invalidSlots = new LinkedHashMap<>(base.slots());
        invalidSlots.put(new SlotName("icon"), new WidgetSlot.SingleSlot(Optional.of(text("Must not disappear"))));
        assertFalse(valid(new WidgetNode(base.id(), base.type(), base.properties(), invalidSlots)));
        assertTrue(TextButtonWidgetPropertySchema.slotUnavailableReason(base, new SlotName("icon")).isPresent());
        assertTrue(TextButtonWidgetPropertySchema.slotUnavailableReason(base, new SlotName("child")).isEmpty());
    }

    @Test
    void nullAndOmissionRemainDistinctForSemanticButtonAndBothClipDefaults() {
        for (PropertyValue value : List.of(new PropertyValue.BooleanValue(false), new PropertyValue.BooleanValue(true), new PropertyValue.NullValue())) {
            var source = generated(node(Map.of(p("isSemanticButton"), value))).build().payload();
            assertTrue(source.contains("isSemanticButton: " + (value instanceof PropertyValue.NullValue ? "null" : ((PropertyValue.BooleanValue) value).value())));
        }
        for (boolean icon : List.of(false, true)) {
            var empty = icon ? iconNode(Map.of(), false) : node(Map.of());
            assertFalse(generated(empty).build().payload().contains("clipBehavior:"));
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.EnumValue("Clip", "none"), new PropertyValue.EnumValue("Clip", "antiAlias"))) {
                var values = Map.of(p("clipBehavior"), value);
                var source = generated(icon ? iconNode(values, false) : node(values)).build().payload();
                assertTrue(source.contains("clipBehavior: " + (value instanceof PropertyValue.NullValue ? "null" : "Clip." + ((PropertyValue.EnumValue) value).value())));
            }
        }
        assertFalse(accepts("autofocus", new PropertyValue.NullValue()));
        assertFalse(accepts("iconAlignment", new PropertyValue.NullValue()));
    }

    @Test
    void everySdkStateHasIndependentSparseResolutionAndExactDocumentedPriority() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String prefix : TextButtonWidgetPropertySchema.statePrefixes()) {
            values.put(p(prefix + "BackgroundColor"), new PropertyValue.ColorValue(0xff112233L));
        }
        String dart = generated(node(values)).build().payload();
        int offset = -1;
        for (String state : TextButtonWidgetPropertySchema.statePriority()) {
            int next = dart.indexOf("WidgetState." + state + ":", offset + 1);
            assertTrue(next > offset, state + dart);
            offset = next;
        }
        for (String prefix : TextButtonWidgetPropertySchema.statePrefixes()) {
            String one = generated(node(Map.of(p(prefix + "ForegroundColor"), new PropertyValue.ColorValue(0xff123456L)))).build().payload();
            if (!prefix.equals("styleDisabled")) assertTrue(one.contains("WidgetState.disabled: null"), one);
        }
    }

    @Test
    void fullSparseFamiliesCoverAll511RowsAndRoundTripDenseModelsWithEveryTypedValue() throws Exception {
        Set<PropertyName> covered = new HashSet<>();
        var codec = new FdDocumentCodec();
        int largest = 0;
        for (boolean icon : List.of(false, true)) for (boolean paints : List.of(false, true)) for (boolean circles : List.of(false, true)) {
            var values = full(icon, paints, circles);
            covered.addAll(values.keySet());
            largest = Math.max(largest, values.size());
            var root = icon ? iconNode(values, true) : node(values);
            var doc = document(root);
            var encoded = codec.encode(doc);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded));
            assertEquals(doc, decoded.document());
            var output = generated(root);
            assertSymbols(output);
            String dart = output.build().payload();
            assertFalse(dart.contains("ElevatedButton"), dart);
            assertTrue(dart.contains("TextButtonTheme.of(context)"));
            assertTrue(dart.contains("ButtonStyle("));
            assertTrue(dart.contains("buttonValues.styleBackgroundBuilder"), dart);
            assertTrue(dart.contains("buttonValues.styleForegroundBuilder"), dart);
            assertTrue(dart.contains("iconAlignment: IconAlignment.end"));
        }
        covered.add(p("style"));
        assertEquals(511, covered.size());
        assertEquals(491, largest);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
    }

    @Test
    void partialStyleDefaultsUseActualTextButtonFamilyAndIconPresenceWithProvenance() {
        for (boolean icon : List.of(false, true)) for (boolean hasIcon : List.of(false, true)) {
            Map<PropertyName, PropertyValue> values = Map.of(p("styleMinimumWidth"), d("20"), p("styleVisualDensityHorizontal"), d("-1"));
            var output = generated(icon ? iconNode(values, hasIcon) : node(values));
            String dart = output.build().payload();
            assertFalse(dart.contains("ElevatedButton"), dart);
            assertTrue(dart.contains(icon
                    ? "frameworkDefaults = (TextButton.icon(onPressed: null, label: const SizedBox()"
                    : "frameworkDefaults = (const TextButton(onPressed: null, child: const SizedBox()"), dart);
            assertEquals(icon && hasIcon, dart.contains("label: const SizedBox(), icon: const SizedBox()"));
            assertSymbols(output);
        }
    }

    @Test
    void allReferenceLocationsAcceptEveryClosedAccessFormAndRejectRawLegacyAndNullForms() throws Exception {
        var codec = new FdDocumentCodec();
        for (String name : List.of("onPressed", "onLongPress", "onHover", "onFocusChange", "focusNode", "statesController",
                "style", "styleBackgroundBuilder", "styleForegroundBuilder")) {
            for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (int access = 0; access < 3; access++) {
                var reference = new PropertyValue.DartObjectReferenceValue(
                        imported ? Optional.of("package:buttons/references.dart") : Optional.empty(), "TextButton",
                        member ? Optional.of("configured") : Optional.empty(), access == 0
                                ? PropertyValue.DartObjectReferenceValue.Access.REFERENCE : PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                        access == 0 ? Optional.empty() : Optional.of(access == 2));
                var root = node(Map.of(p(name), reference));
                var output = generated(root);
                assertSymbols(output);
                assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.modelPath().equals("/root/properties/" + name + "/rootSymbol")));
                var encoded = codec.encode(document(root));
                assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)).document());
            }
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), s("handler"), new PropertyValue.CallbackValue("handler"), new PropertyValue.DartExpressionValue("handler"))) {
                assertFalse(accepts(name, invalid), name);
            }
        }
    }

    @Test
    void projectStyleExcludesEveryLocalLeafAndPreservesConstnessAndThemeOverrides() {
        for (String name : TextButtonWidgetPropertySchema.localStyleProperties()) {
            assertFalse(valid(node(Map.of(p("style"), reference("fullStyle"), p(name), value(name)))), name);
        }
        var output = generated(node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false), p("style"), reference("fullStyle"))));
        assertTrue(output.build().payload().contains("buttonValues.fullStyle"), output.build().payload());
        assertFalse(output.build().payload().contains("ButtonStyle("));
        assertFalse(output.build().payload().contains("const TextButton("));
        assertFalse(output.build().payload().contains("TextButtonTheme"));
        var constant = new PropertyValue.DartObjectReferenceValue(Optional.of("package:buttons/styles.dart"), "ButtonStyle",
                Optional.of("empty"), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true));
        assertTrue(generated(node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false), p("style"), constant)))
                .build().payload().contains("return const TextButton("));
    }

    @Test
    void all128EnabledStateCombinationsValidateDimensionsAndShapePriorityWithoutCrossStateLeaks() {
        List<String> states = TextButtonWidgetPropertySchema.statePrefixes().stream()
                .filter(prefix -> !prefix.equals("style") && !prefix.equals("styleDisabled")).toList();
        for (int mask = 0; mask < (1 << states.size()); mask++) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("styleMaximumWidth"), d("200"));
            values.put(p("styleShapeKind"), s("roundedRectangle"));
            for (int i = 0; i < states.size(); i++) if ((mask & (1 << i)) != 0) {
                values.put(p(states.get(i) + "MinimumWidth"), d("120"));
                values.put(p(states.get(i) + "ShapeRadiusTopLeft"), d("4"));
            }
            assertTrue(valid(node(values)), "mask=" + mask);
            for (String prefix : states) {
                var invalid = new LinkedHashMap<>(values);
                invalid.put(p(prefix + "MinimumWidth"), d("201"));
                assertFalse(valid(node(invalid)), prefix + " mask=" + mask);
            }
        }
        for (String prefix : TextButtonWidgetPropertySchema.statePrefixes()) {
            assertFalse(valid(node(Map.of(p(prefix + "TextBackgroundColor"), value(prefix + "TextBackgroundColor"),
                    p(prefix + "TextBackground"), value(prefix + "TextBackground")))));
            assertFalse(valid(node(Map.of(p(prefix + "ShapeCircleEccentricity"), d("0.5")))));
            assertFalse(valid(node(Map.of(p(prefix + "TextTheme"), value(prefix + "TextTheme")))));
        }
    }

    @Test
    void requiredChildNeverAcceptsMissingEmptyOrParentDataAndNoUnsupportedConstructorArgumentLeaks() {
        var base = node(Map.of());
        assertFalse(valid(new WidgetNode(base.id(), base.type(), base.properties(), Map.of())));
        assertFalse(valid(new WidgetNode(base.id(), base.type(), base.properties(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()))));
        for (var source : CATALOG.definitions()) {
            boolean allowed = !Set.of("flutter.widgets.Expanded", "flutter.widgets.Flexible", "flutter.widgets.Spacer", "flutter.widgets.LayoutId", "flutter.widgets.TableRow", "flutter.widgets.TableCell").contains(source.typeId().value()) && !WidgetPlacementRules.isStackPositionedWidget(source) && !WidgetPlacementRules.isSliverWidget(source);
            for (var slot : definition().slots()) assertEquals(allowed, WidgetPlacementRules.accepts(definition(), slot, source));
        }
        for (String name : List.of("label", "tooltip", "key", "controller", "foregroundColor", "mouseCursor")) {
            assertFalse(valid(node(Map.of(p(name), s("unsupported")))), name);
        }
    }

    private static WidgetNode iconNode(Map<PropertyName, PropertyValue> values, boolean hasIcon) {
        var source = node(values);
        var properties = new LinkedHashMap<>(source.properties());
        properties.put(p("variant"), s("icon"));
        var slots = new LinkedHashMap<>(source.slots());
        if (hasIcon) slots.put(new SlotName("icon"), new WidgetSlot.SingleSlot(Optional.of(text("Icon"))));
        return new WidgetNode(source.id(), source.type(), properties, slots);
    }
    private static boolean accepts(String name, PropertyValue value) {
        return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(c -> c.accepts(value));
    }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
    private static void assertSymbols(GeneratedDartRegions output) {
        String source = output.build().payload();
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}
