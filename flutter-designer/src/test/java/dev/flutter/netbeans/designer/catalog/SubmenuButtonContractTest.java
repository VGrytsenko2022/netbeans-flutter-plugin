package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.*;
import dev.flutter.netbeans.designer.validation.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class SubmenuButtonContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    @Test void allTwentyOneArguments721PropertiesAndFourSlotsPreserveNullableRequiredChild() throws Exception {
        assertEquals(721, definition().properties().size());
        assertEquals(498, SubmenuButtonWidgetPropertySchema.localStyleProperties().size());
        assertEquals(203, SubmenuButtonWidgetPropertySchema.menuStyleProperties().size());
        assertEquals(4, SubmenuButtonWidgetPropertySchema.submenuIconLocalProperties().size());
        assertEquals(new ArrayList<>(SubmenuButtonWidgetPropertySchema.definitions().keySet()), definition().properties().stream().map(v -> v.name().value()).toList());
        assertEquals(new PaletteMetadata("flutter.material", 100, 350, "SubmenuButton"), definition().palette());
        assertTrue(definition().constConstructor());
        for (int i = 0; i < 721; i++) assertEquals(DartParameter.named(i, false), definition().properties().get(i).parameter());
        assertEquals(List.of("child", "leadingIcon", "trailingIcon", "menuChildren"), definition().slots().stream().map(v -> v.name().value()).toList());
        assertEquals(DartParameter.named(721, true), definition().slots().get(0).parameter());
        assertEquals(DartParameter.named(724, true), definition().slots().get(3).parameter());
        assertTrue(definition().slots().stream().allMatch(v -> v.minChildren() == 0));
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertTrue(prototype.properties().isEmpty());
        String source = generated(prototype).build().payload();
        assertTrue(source.contains("const SubmenuButton("), source); assertTrue(source.contains("child: null")); assertTrue(source.contains("menuChildren:"));
        for (String absent : List.of("enabled", "onPressed", "onLongPress", "variant", "padding", "autofocus", "isSemanticButton")) assertTrue(definition().property(p(absent)).isEmpty(), absent);
        for (String absent : List.of("onPressed:", "controller:", "hoverOpenDelay:", "style:", "submenuIcon:")) assertFalse(source.contains(absent), source);
        roundTrip(prototype); assertEquals(16, DesignerDocument.SCHEMA_VERSION); assertEquals(15, WidgetCatalog.API_VERSION);
    }
    @Test void everyNullableDirectReferenceAndFactoryKeepsExactProofPaths() throws Exception {
        var types = Map.ofEntries(Map.entry("controller", "MenuController"), Map.entry("style", "ButtonStyle"), Map.entry("menuStyle", "MenuStyle"), Map.entry("alignmentOffset", "Offset"), Map.entry("focusNode", "FocusNode"), Map.entry("statesController", "WidgetStatesController"), Map.entry("submenuIcon", "WidgetStateProperty<Widget?>"), Map.entry("onOpen", "VoidCallback"), Map.entry("onClose", "VoidCallback"), Map.entry("onHover", "ValueChanged<bool>"), Map.entry("onFocusChange", "ValueChanged<bool>"), Map.entry("onAnimationStatusChanged", "ValueChanged<AnimationStatus>"), Map.entry("hoverOpenDelayUs", "Duration"));
        for (var entry : types.entrySet()) for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean factory : List.of(false, true)) {
            var ref = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:menus/values.dart") : Optional.empty(), "menuValues", member ? Optional.of("value") : Optional.empty(), factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
            var output = generated(node(Map.of(p(entry.getKey()), ref))); assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().equals("/root/properties/" + entry.getKey() + (member ? "/member" : "/rootSymbol")) && v.staticTypeRequirement().map(t -> t.expectedDartType().equals(entry.getValue())).orElse(false)), () -> entry + ": " + output.symbolOccurrences());
            roundTrip(node(Map.of(p(entry.getKey()), ref)));
            assertFalse(accepts(entry.getKey(), new PropertyValue.CallbackValue("_handler")));
        }
        for (String name : types.keySet()) {
            if (name.equals("hoverOpenDelayUs")) { assertFalse(accepts(name, new PropertyValue.NullValue())); continue; }
            assertTrue(generated(node(Map.of(p(name), new PropertyValue.NullValue()))).build().payload().contains(name + ": null"));
        }
    }
    @Test void denseStylesExceedOldBudgetWithoutMixingNamespacesOrNativeDefaults() throws Exception {
        Set<String> observed = new HashSet<>();
        for (boolean circles : List.of(false, true)) {
            var values = full(circles); values.keySet().forEach(v -> observed.add(v.value()));
            assertTrue(values.size() > 512); var root = node(values); var output = generated(root); String source = output.build().payload();
            assertTrue(source.contains("style: ButtonStyle("), source); assertTrue(source.contains("menuStyle: MenuStyle("), source);
            assertTrue(source.contains("MenuButtonTheme.of(context).style?."), source);
            assertTrue(source.contains("SubmenuButton(menuChildren: [], child: null)"), source);
            assertTrue(source.contains("MenuTheme.of(context).style?."), source);
            assertTrue(source.contains("EdgeInsetsDirectional.symmetric(vertical: 8.0)"), source);
            assertFalse(source.contains("const MenuAnchor(")); assertFalse(source.contains("onPressed:"));
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().startsWith("/root/properties/menuStyle") && v.id().contains(":submenu-menu-style:")));
            assertTrue(output.symbolOccurrences().stream().filter(v -> v.modelPath().startsWith("/root/properties/menuStyle")).allMatch(v -> v.id().contains(":submenu-menu-style:")));
            assertSymbols(output); roundTrip(root);
            var limited = new WidgetTreeValidator(new ValidationLimits(256, 10000, 512, 128, 1000)).validate(document(root), CATALOG);
            assertFalse(limited.valid());
        }
        var all = new HashSet<>(SubmenuButtonWidgetPropertySchema.localStyleProperties()); all.addAll(SubmenuButtonWidgetPropertySchema.menuStyleProperties());
        assertEquals(all, observed);
    }
    @Test void styleConflictsAndNestedInvalidGeometryKeepOriginalPropertyPaths() {
        for (String whole : List.of("style", "menuStyle", "submenuIcon")) {
            String local = switch (whole) { case "style" -> "styleElevation"; case "menuStyle" -> "menuStyleElevation"; default -> "submenuIconDefault"; };
            PropertyValue value = whole.equals("submenuIcon") ? new PropertyValue.NullValue() : new PropertyValue.DoubleValue(BigDecimal.ONE);
            for (PropertyValue ref : List.of(reference("_whole"), new PropertyValue.NullValue())) {
                var root = node(Map.of(p(whole), ref, p(local), value)); assertFalse(valid(root)); assertEquals(2, root.properties().size());
            }
        }
        assertTrue(valid(node(Map.of(p("style"), reference("_button"), p("menuStyleElevation"), new PropertyValue.DoubleValue(BigDecimal.ONE)))));
        assertTrue(valid(node(Map.of(p("menuStyle"), reference("_menu"), p("styleElevation"), new PropertyValue.DoubleValue(BigDecimal.ONE)))));
        for (String name : List.of("menuStyleShapeRadiusTopLeft", "menuStyleAlignmentX")) {
            var result = new WidgetTreeValidator().validate(document(node(Map.of(p(name), new PropertyValue.DoubleValue(BigDecimal.ONE)))), CATALOG);
            assertFalse(result.valid()); assertTrue(result.errors().stream().allMatch(v -> v.path().startsWith("/root/properties/menuStyle")), result.issues().toString());
        }
        assertFalse(valid(node(Map.of(p("menuStyleMinimumWidth"), new PropertyValue.DoubleValue(BigDecimal.TEN), p("menuStyleMaximumWidth"), new PropertyValue.DoubleValue(BigDecimal.ONE)))));
    }
    @Test void localIndicatorPriorityDistinguishesAbsentNullAndIconNone() throws Exception {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("submenuIconDefault"), PropertyValue.IconDataValue.none());
        values.put(p("submenuIconDisabled"), new PropertyValue.NullValue());
        values.put(p("submenuIconHovered"), reference("_hoveredWidget"));
        values.put(p("submenuIconFocused"), new PropertyValue.NullValue());
        var output = generated(node(values)); String source = output.build().payload();
        assertTrue(source.contains("Icon(null)"), source);
        assertTrue(source.indexOf("WidgetState.disabled") < source.indexOf("WidgetState.hovered"));
        assertTrue(source.indexOf("WidgetState.hovered") < source.indexOf("WidgetState.focused"));
        assertTrue(source.indexOf("WidgetState.focused") < source.indexOf("WidgetState.any"));
        assertTrue(source.contains("WidgetState.disabled: null")); assertTrue(source.contains("WidgetState.focused: null"));
        assertSymbols(output); roundTrip(node(values));
        String sparse = generated(node(Map.of(p("submenuIconHovered"), new PropertyValue.NullValue()))).build().payload();
        assertFalse(sparse.contains("WidgetState.disabled")); assertFalse(sparse.contains("WidgetState.any"));
    }
    @Test void signedMicrosecondsHaveDedicatedCoreDurationProofAndNoPrecisionLoss() {
        for (long value : new long[]{-9007199254740991L, -1, 0, 1, 9007199254740991L}) {
            var root = node(Map.of(p("hoverOpenDelayUs"), new PropertyValue.IntegerValue(BigInteger.valueOf(value))));
            var output = generated(root); assertTrue(output.build().payload().contains("hoverOpenDelay: const Duration(microseconds: " + value + ")"), output.build().payload());
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.id().equals("widget:" + root.id() + ":submenu-button-core-duration:hoverOpenDelayUs") && v.libraryUri().equals("dart:core") && v.staticTypeRequirement().isEmpty())); assertSymbols(output);
        }
        assertFalse(accepts("hoverOpenDelayUs", new PropertyValue.IntegerValue(BigInteger.valueOf(9007199254740992L))));
    }
    @Test void fiveNativeEventsTwoBuildersAndOnlyTwoConsumersHaveNoInventedProducer() {
        var events = WidgetEventCatalog.eventsFor(definition()); assertEquals(7, events.size());
        assertEquals(5, events.stream().filter(v -> v.kind() == WidgetEventDescriptor.Kind.EVENT).count());
        assertEquals(2, events.stream().filter(v -> v.kind() == WidgetEventDescriptor.Kind.BUILDER).count());
        assertEquals(2, WidgetStatePropertyBindingCatalog.descriptors(node(Map.of())).size());
        assertTrue(WidgetStateBindingCatalog.find(node(Map.of())).isEmpty());
        assertTrue(WidgetEventCatalog.find(definition().typeId(), p("onPressed")).isEmpty());
        assertTrue(WidgetStatePropertyBindingCatalog.find(node(Map.of()), p("submenuIconHovered")).isEmpty());
    }
    @Test void pinnedConstructorAndPrepassQuirksRemainExplicit() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk"); Assumptions.assumeTrue(sdk != null && !sdk.isBlank());
        String source = Files.readString(Path.of(sdk, "packages/flutter/lib/src/material/menu_anchor.dart"));
        String constructor = source.substring(source.indexOf("const SubmenuButton({"), source.indexOf("const SubmenuButton({") + 700);
        for (String name : List.of("hoverOpenDelay = Duration.zero", "required this.child", "required this.menuChildren", "this.submenuIcon")) assertTrue(constructor.contains(name), name);
        assertTrue(source.contains("_kMenuVerticalMinPadding = 8;"));
        assertTrue(source.contains(".resolve(widget.statesController?.value ?? const <WidgetState>{})!"));
    }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), CATALOG); assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics()); return result.generated().orElseThrow(); }
    private static void roundTrip(WidgetNode root) throws Exception { var codec = new FdDocumentCodec(); var doc = document(root); var encoded = codec.encode(doc); var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)); assertEquals(doc, decoded.document()); assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes()); }
    private static void assertSymbols(GeneratedDartRegions output) { for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString()); assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count()); }
}
