package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuItemButtonContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    @Test void complete520RowsAndThreeOptionalChildrenPreserveSdkConstructor() {
        assertEquals(520, definition().properties().size()); assertEquals(498, MenuItemButtonWidgetPropertySchema.localStyleProperties().size());
        assertEquals(8, MenuItemButtonWidgetPropertySchema.shortcutLocalProperties().size());
        assertEquals(new ArrayList<>(MenuItemButtonWidgetPropertySchema.definitions().keySet()), definition().properties().stream().map(v -> v.name().value()).toList());
        assertEquals(new PaletteMetadata("flutter.material", 100, 330, "MenuItemButton"), definition().palette());
        assertTrue(definition().constConstructor()); assertTrue(definition().traits().isEmpty());
        for (int i = 0; i < 520; i++) assertEquals(DartParameter.named(i, i == 0), definition().properties().get(i).parameter());
        assertEquals(List.of("child", "leadingIcon", "trailingIcon"), definition().slots().stream().map(v -> v.name().value()).toList());
        for (int i = 0; i < 3; i++) { var slot = definition().slots().get(i); assertEquals(DartParameter.named(520 + i, false), slot.parameter()); assertEquals(0, slot.minChildren()); }
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertEquals(Map.of(p("enabled"), new PropertyValue.BooleanValue(true)), prototype.properties()); assertTrue(valid(prototype));
        for (String absent : List.of("variant", "onLongPress", "isSemanticButton", "iconAlignment")) assertTrue(definition().property(p(absent)).isEmpty());
        assertEquals(16, DesignerDocument.SCHEMA_VERSION); assertEquals(15, WidgetCatalog.API_VERSION);
    }
    @Test void activationOptionalSlotsAndNativeClipAreSparseAndConstCorrect() throws Exception {
        assertTrue(generated(node(Map.of())).build().payload().contains("onPressed: () {}"));
        for (int mask = 0; mask < 8; mask++) {
            var original = node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false), p("onPressed"), reference("_pressed")));
            var slots = new LinkedHashMap<SlotName, WidgetSlot>();
            for (int i = 0; i < 3; i++) { var slot = definition().slots().get(i); if ((mask & (1 << i)) != 0) slots.put(slot.name(), new WidgetSlot.SingleSlot(Optional.of(text(slot.name().value())))); }
            var root = new WidgetNode(original.id(), original.type(), original.properties(), slots);
            var output = generated(root); String source = output.build().payload();
            assertTrue(source.contains("const MenuItemButton("), source); assertTrue(source.contains("onPressed: null"));
            assertFalse(source.contains("_pressed")); assertFalse(source.contains("onLongPress:")); assertFalse(source.contains("enabled:"));
            assertFalse(source.contains("clipBehavior:")); roundTrip(root);
        }
        var source = generated(node(Map.of(p("styleBackgroundBuilder"), reference("_layer"), p("clipBehavior"), new PropertyValue.EnumValue("Clip", "none")))).build().payload();
        assertTrue(source.contains("clipBehavior: Clip.none")); assertFalse(source.contains("Clip.antiAlias"));
    }
    @Test void shortcutsCoverAll432KeysAndPreserveUnicodeCharactersAndOriginalPaths() throws Exception {
        assertEquals(432, MenuItemButtonWidgetPropertySchema.logicalKeyboardKeys().size());
        for (String name : MenuShortcutKeyCatalog.names()) {
            var root = node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false), p("shortcutTrigger"), new PropertyValue.EnumValue("LogicalKeyboardKey", name)));
            var output = generated(root); String source = output.build().payload();
            assertTrue(source.contains("SingleActivator("), source);
            assertTrue(source.contains(".LogicalKeyboardKey." + name + ")"), source);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.symbolName().equals("LogicalKeyboardKey") && v.libraryUri().equals("package:flutter/services.dart")));
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.symbolName().equals("SingleActivator") && v.modelPath().equals("/root/properties/shortcutTrigger")));
            assertSymbols(output);
        }
        for (String value : List.of("", "a", "A", "Ж", "👩‍💻", "multi\n'\\value")) {
            var root = node(Map.of(p("shortcutCharacter"), s(value), p("shortcutAlt"), new PropertyValue.BooleanValue(true), p("shortcutIncludeRepeats"), new PropertyValue.BooleanValue(false)));
            String source = generated(root).build().payload(); assertTrue(source.contains("CharacterActivator(")); assertTrue(source.contains("alt: true")); assertTrue(source.contains("includeRepeats: false")); roundTrip(root);
        }
        assertFalse(accepts("shortcutTrigger", new PropertyValue.EnumValue("LogicalKeyboardKey", "control")));
        assertFalse(accepts("shortcutTrigger", s("LogicalKeyboardKey.keyA")));
    }
    @Test void shortcutAndWholeStyleConflictsFailClosedWithoutErasingAnyField() {
        for (String modifier : MenuItemButtonWidgetPropertySchema.shortcutLocalProperties().subList(2, 8)) {
            PropertyValue value = modifier.equals("shortcutNumLock") ? new PropertyValue.EnumValue("LockState", "ignored") : new PropertyValue.BooleanValue(false);
            assertFalse(valid(node(Map.of(p(modifier), value))));
        }
        var key = new PropertyValue.EnumValue("LogicalKeyboardKey", "keyA");
        assertFalse(valid(node(Map.of(p("shortcutTrigger"), key, p("shortcutCharacter"), s("A")))));
        for (PropertyValue whole : List.of(reference("_shortcut"), new PropertyValue.NullValue())) {
            assertFalse(valid(node(Map.of(p("shortcut"), whole, p("shortcutTrigger"), key))));
            assertTrue(valid(node(Map.of(p("shortcut"), whole))));
        }
        assertFalse(valid(node(Map.of(p("shortcutCharacter"), s("A"), p("shortcutShift"), new PropertyValue.BooleanValue(false)))));
        assertFalse(valid(node(Map.of(p("shortcutCharacter"), s("A"), p("shortcutNumLock"), new PropertyValue.EnumValue("LockState", "ignored")))));
        for (PropertyValue whole : List.of(reference("_style"), new PropertyValue.NullValue())) {
            assertFalse(valid(node(Map.of(p("style"), whole, p("styleEnableFeedback"), new PropertyValue.BooleanValue(true)))));
            assertTrue(valid(node(Map.of(p("style"), whole))));
        }
    }
    @Test void allSharedStyleBranchesUseMenuButtonThemeAndNativeMenuDefaultsWithoutChangingOtherFamilies() throws Exception {
        for (boolean paints : List.of(false, true)) for (boolean circles : List.of(false, true)) {
            var root = node(full(paints, circles)); var output = generated(root); String source = output.build().payload();
            assertTrue(source.contains("MenuButtonTheme.of(context)"), source); assertTrue(source.contains("MenuItemButton(onPressed: null"));
            assertFalse(source.contains("TextButtonTheme")); assertFalse(source.contains("TextButton("));
            assertTrue(source.contains("backgroundBuilder:")); assertTrue(source.contains("foregroundBuilder:")); assertSymbols(output); roundTrip(root);
        }
        for (String family : List.of("TextButton", "OutlinedButton", "FilledButton", "IconButton")) {
            var shared = CATALOG.find(new WidgetTypeId("flutter.material." + family)).orElseThrow();
            for (String name : MenuItemButtonWidgetPropertySchema.localStyleProperties()) assertEquals(shared.property(p(name)).orElseThrow().constraints(), definition().property(p(name)).orElseThrow().constraints(), family + ':' + name);
        }
    }
    @Test void strictReferenceFamiliesAndExplicitNullKeepEveryProofAndOmissionDistinct() throws Exception {
        var types = Map.of("onPressed", "VoidCallback", "onHover", "ValueChanged<bool>", "onFocusChange", "ValueChanged<bool>", "focusNode", "FocusNode", "statesController", "WidgetStatesController", "style", "ButtonStyle", "shortcut", "MenuSerializableShortcut", "styleBackgroundBuilder", "ButtonLayerBuilder", "styleForegroundBuilder", "ButtonLayerBuilder");
        for (var entry : types.entrySet()) for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean factory : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:menu/values.dart") : Optional.empty(), "menuValues", member ? Optional.of("value") : Optional.empty(), factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
            var root = node(Map.of(p(entry.getKey()), reference)); var output = generated(root); assertSymbols(output); roundTrip(root);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().equals("/root/properties/" + entry.getKey() + (member ? "/member" : "/rootSymbol")) && v.staticTypeRequirement().map(t -> t.expectedDartType().equals(entry.getValue())).orElse(false)), () -> entry + ": " + output.symbolOccurrences());
            assertFalse(accepts(entry.getKey(), new PropertyValue.CallbackValue("_callback"))); assertFalse(accepts(entry.getKey(), s("() => value")));
        }
        for (String name : List.of("onHover", "onFocusChange", "focusNode", "statesController", "style", "shortcut", "semanticsLabel")) {
            var root = node(Map.of(p(name), new PropertyValue.NullValue())); assertTrue(generated(root).build().payload().contains(name + ": null")); roundTrip(root);
        }
        for (String name : List.of("onPressed", "enabled", "autofocus", "requestFocusOnHover", "closeOnActivate", "clipBehavior", "overflowAxis")) assertFalse(accepts(name, new PropertyValue.NullValue()));
    }
    @Test void exactlyThreeEventsTwoBuildersAndFourSafeConsumersWithoutStateProducer() {
        var events = WidgetEventCatalog.eventsFor(definition()); assertEquals(5, events.size());
        assertEquals(3, events.stream().filter(v -> v.kind() == WidgetEventDescriptor.Kind.EVENT).count());
        assertEquals(2, events.stream().filter(v -> v.kind() == WidgetEventDescriptor.Kind.BUILDER).count());
        assertFalse(WidgetEventCatalog.find(definition().typeId(), p("shortcut")).isPresent());
        assertFalse(WidgetStateBindingCatalog.find(node(Map.of())).isPresent());
        assertEquals(4, WidgetStatePropertyBindingCatalog.descriptors(node(Map.of())).size());
    }
    @Test void enabledStateOverridesPreviewLiteralAndRetainsPressedProofWithoutOuterEnabledArgument() {
        for (var binding : List.of(
                new StatePropertyBinding("_active", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                new StatePropertyBinding("_active", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS,
                        Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.ONE))))) {
            var original = node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false), p("onPressed"), reference("_pressed")));
            var bindings = Map.of(p("enabled"), binding);
            var root = new WidgetNode(original.id(), original.type(), original.properties(), original.slots(), original.extensions(), Optional.empty(), bindings);
            var output = generated(root); String source = output.build().payload();
            assertTrue(source.contains(" ? _pressed : null)"), source); assertFalse(source.contains("enabled:"));
            assertTrue(source.contains(switch (binding.transform()) {
                case NOT -> "!_active";
                case EQUALS -> "(_choice == 1)";
                default -> "_active";
            }), source); assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().equals("/root/properties/onPressed/rootSymbol") && v.staticTypeRequirement().map(t -> t.expectedDartType().equals("VoidCallback")).orElse(false)), output.symbolOccurrences().toString());
            assertEquals(new PropertyValue.BooleanValue(false), root.properties().get(p("enabled")));
        }
    }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), CATALOG); assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics()); return result.generated().orElseThrow(); }
    private static void roundTrip(WidgetNode root) throws Exception { var codec = new FdDocumentCodec(); var doc = document(root); var encoded = codec.encode(doc); var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)); assertEquals(doc, decoded.document()); assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes()); }
    private static void assertSymbols(GeneratedDartRegions output) { for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString()); assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count()); }
}
