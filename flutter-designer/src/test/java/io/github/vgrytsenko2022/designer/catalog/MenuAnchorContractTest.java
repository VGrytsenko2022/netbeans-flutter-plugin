package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.state.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.MenuAnchorTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuAnchorContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    @Test void allNineteenConstructorArgumentsAnd219RowsKeepRequiredEmptyListAndOptionalChild() throws Exception {
        assertEquals(219, definition().properties().size());
        assertEquals(203, MenuAnchorWidgetPropertySchema.localStyleProperties().size());
        assertEquals(9, MenuAnchorWidgetPropertySchema.statePrefixes().size());
        assertEquals(new ArrayList<>(MenuAnchorWidgetPropertySchema.definitions().keySet()), definition().properties().stream().map(v -> v.name().value()).toList());
        assertEquals(new PaletteMetadata("flutter.material", 100, 340, "MenuAnchor"), definition().palette());
        assertTrue(definition().constConstructor());
        for (int i = 0; i < 219; i++) assertEquals(DartParameter.named(i, false), definition().properties().get(i).parameter());
        assertEquals(List.of("menuChildren", "child"), definition().slots().stream().map(v -> v.name().value()).toList());
        assertEquals(DartParameter.named(219, true), definition().slots().get(0).parameter());
        assertEquals(DartParameter.named(220, false), definition().slots().get(1).parameter());
        assertEquals(SlotCardinality.LIST, definition().slots().get(0).cardinality());
        assertTrue(definition().slots().stream().allMatch(v -> v.minChildren() == 0));
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertTrue(prototype.properties().isEmpty()); assertTrue(valid(prototype));
        assertTrue(generated(prototype).build().payload().contains("const MenuAnchor("));
        for (String absent : List.of("enabled", "onPressed", "animationStyle", "variant", "styleTextFontSize", "styleForegroundBuilder")) assertTrue(definition().property(p(absent)).isEmpty());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION); assertEquals(16, WidgetCatalog.API_VERSION);
        roundTrip(prototype);
    }
    @Test void sparseNativeDefaultsAllSlotsAndNullsRemainExact() throws Exception {
        for (int count = 0; count < 3; count++) for (boolean child : List.of(false, true)) {
            var original = node(Map.of()); var slots = new LinkedHashMap<SlotName, WidgetSlot>();
            List<WidgetNode> children = new ArrayList<>(); for (int i = 0; i < count; i++) children.add(text("Item " + i));
            slots.put(new SlotName("menuChildren"), new WidgetSlot.ListSlot(children));
            if (child) slots.put(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(text("Anchor"))));
            var root = new WidgetNode(original.id(), original.type(), original.properties(), slots);
            String source = generated(root).build().payload(); assertTrue(source.contains("menuChildren:"));
            assertEquals(child, source.contains("child:")); assertFalse(source.contains("builder:"));
            assertFalse(source.contains("controller:")); assertFalse(source.contains("clipBehavior:")); assertFalse(source.contains("onOpen:")); roundTrip(root);
        }
        for (String name : List.of("controller", "childFocusNode", "style", "alignmentOffset", "reservedPadding", "layerLink", "onOpen", "onClose", "onAnimationStatusChanged", "builder")) {
            var root = node(Map.of(p(name), new PropertyValue.NullValue()));
            assertTrue(generated(root).build().payload().contains(name + ": null")); roundTrip(root);
        }
        for (String name : List.of("clipBehavior", "anchorTapClosesMenu", "consumeOutsideTap", "crossAxisUnconstrained", "useRootOverlay", "animated")) assertFalse(accepts(name, new PropertyValue.NullValue()));
    }
    @Test void everyReferenceFormRetainsExactStrictProofAndOriginalModelPath() throws Exception {
        var types = Map.of("controller", "MenuController", "childFocusNode", "FocusNode", "style", "MenuStyle", "alignmentOffset", "Offset", "reservedPadding", "EdgeInsetsGeometry", "layerLink", "LayerLink", "onOpen", "VoidCallback", "onClose", "VoidCallback", "onAnimationStatusChanged", "ValueChanged<AnimationStatus>", "builder", "MenuAnchorChildBuilder");
        for (var entry : types.entrySet()) for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean factory : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:menus/values.dart") : Optional.empty(), "menuValues", member ? Optional.of("value") : Optional.empty(), factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
            var root = node(Map.of(p(entry.getKey()), reference)); var output = generated(root); assertSymbols(output); roundTrip(root);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().equals("/root/properties/" + entry.getKey() + (member ? "/member" : "/rootSymbol")) && v.staticTypeRequirement().map(t -> t.expectedDartType().equals(entry.getValue())).orElse(false)), () -> entry + ": " + output.symbolOccurrences());
            assertFalse(accepts(entry.getKey(), new PropertyValue.CallbackValue("_callback")));
            assertFalse(accepts(entry.getKey(), s("() => custom()")));
        }
    }
    @Test void everyLocalMenuStyleFamilyUsesPanelMenuThemeAndExactDefaultShapeNotButtonDefaults() throws Exception {
        Set<String> observed = new HashSet<>();
        for (boolean circles : List.of(false, true)) {
            var properties = full(circles); properties.keySet().forEach(v -> observed.add(v.value()));
            var root = node(properties); var output = generated(root); String source = output.build().payload();
            assertTrue(source.contains("MenuStyle(")); assertTrue(source.contains("MenuTheme.of(context).style?."));
            assertTrue(source.contains("Radius.circular(4.0)"));
            assertFalse(source.contains("defaultStyleOf")); assertFalse(source.contains("ButtonStyle(")); assertFalse(source.contains("MenuButtonTheme"));
            for (String field : List.of("backgroundColor", "shadowColor", "surfaceTintColor", "elevation", "padding", "minimumSize", "fixedSize", "maximumSize", "side", "shape", "mouseCursor", "visualDensity", "alignment")) assertTrue(source.contains(field + ":"), field);
            for (String state : MenuAnchorWidgetPropertySchema.statePriority()) if (!state.equals("any")) assertTrue(source.contains("WidgetState." + state), state);
            assertSymbols(output); roundTrip(root);
        }
        assertEquals(new HashSet<>(MenuAnchorWidgetPropertySchema.localStyleProperties()), observed);
        for (String name : observed) assertEquals(CATALOG.find(TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE).orElseThrow().property(p(name)).orElseThrow().constraints(), definition().property(p(name)).orElseThrow().constraints(), name);
    }
    @Test void localDensityUsesConstructorDefaultsAndAlignmentRequiresCompleteTypedCoordinates() {
        for (String axis : List.of("Horizontal", "Vertical")) {
            String source = generated(node(Map.of(p("styleVisualDensity" + axis), new PropertyValue.DoubleValue(BigDecimal.ONE)))).build().payload();
            assertTrue(source.contains("VisualDensity(")); assertTrue(source.contains(axis.toLowerCase(Locale.ROOT) + ": 1.0"), source);
            assertFalse(source.contains("MenuBarTheme")); assertFalse(source.contains("MenuTheme")); assertFalse(source.contains("inherited"));
        }
        assertFalse(valid(node(Map.of(p("styleAlignmentX"), new PropertyValue.DoubleValue(BigDecimal.ZERO)))));
        assertTrue(valid(node(Map.of(p("styleAlignmentKind"), s("directional"), p("styleAlignmentX"), new PropertyValue.DoubleValue(BigDecimal.ZERO), p("styleAlignmentY"), new PropertyValue.DoubleValue(BigDecimal.ONE)))));
    }
    @Test void wholeStyleConflictAndSparseShapeBoundsRejectWithoutAlteringInput() {
        for (PropertyValue whole : List.of(reference("_style"), new PropertyValue.NullValue())) {
            var root = node(Map.of(p("style"), whole, p("styleElevation"), new PropertyValue.DoubleValue(BigDecimal.ONE)));
            assertFalse(valid(root)); assertEquals(2, root.properties().size());
        }
        assertFalse(valid(node(Map.of(p("styleShapeRadiusTopLeft"), new PropertyValue.DoubleValue(BigDecimal.ONE)))));
        assertFalse(valid(node(Map.of(p("styleMinimumWidth"), new PropertyValue.DoubleValue(BigDecimal.TEN), p("styleMaximumWidth"), new PropertyValue.DoubleValue(BigDecimal.ONE)))));
        assertTrue(valid(node(Map.of(p("styleMinimumWidth"), new PropertyValue.DoubleValue(BigDecimal.ONE)))));
    }
    @Test void exactlyThreeNativeEventsOneBuilderAndFourConsumersExcludeInertDeprecatedFlag() {
        var events = WidgetEventCatalog.eventsFor(definition()); assertEquals(4, events.size());
        assertEquals(3, events.stream().filter(v -> v.kind() == WidgetEventDescriptor.Kind.EVENT).count());
        var builder = WidgetEventCatalog.find(definition().typeId(), p("builder")).orElseThrow();
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, builder.kind());
        assertEquals("Widget Function(BuildContext, MenuController, Widget?)", builder.signature().dartFunctionType());
        assertTrue(builder.signature().importUris().contains("package:flutter/material.dart"));
        assertTrue(WidgetEventCatalog.find(definition().typeId(), p("onAnimationStatusChanged")).orElseThrow().signature().importUris().contains("package:flutter/widgets.dart"));
        assertFalse(WidgetStateBindingCatalog.find(node(Map.of())).isPresent());
        assertEquals(4, WidgetStatePropertyBindingCatalog.descriptors(node(Map.of())).size());
        assertTrue(WidgetStatePropertyBindingCatalog.find(node(Map.of()), p("anchorTapClosesMenu")).isEmpty());
        assertTrue(generated(node(Map.of(p("anchorTapClosesMenu"), new PropertyValue.BooleanValue(true)))).build().payload().contains("anchorTapClosesMenu: true"));
    }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), CATALOG); assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics()); return result.generated().orElseThrow(); }
    private static void roundTrip(WidgetNode root) throws Exception { var codec = new FdDocumentCodec(); var doc = document(root); var encoded = codec.encode(doc); var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)); assertEquals(doc, decoded.document()); assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes()); }
    private static void assertSymbols(GeneratedDartRegions output) { for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString()); assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count()); }
}
