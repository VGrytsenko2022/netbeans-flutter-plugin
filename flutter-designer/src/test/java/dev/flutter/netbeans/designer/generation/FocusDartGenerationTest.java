package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusDartGenerationTest {
    private static final WidgetTypeId TYPE = FocusWidgetPropertySchema.FOCUS_TYPE;

    @Test void defaultFocusIsConstWithRequiredChildAndNoInventedNodeOrCallbacks() {
        var generated = generate(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("standard")));
        String build = generated.build().payload();
        assertTrue(build.contains("return const Focus("), build);
        assertTrue(build.contains("child: const Text("), build);
        for (String name : FocusWidgetPropertySchema.definitions().keySet()) assertFalse(build.contains(name + ":"), name);
        assertFalse(build.contains("FocusNode("), build);
    }

    @Test void allThreeCallbacksUseExactStrictProofForTypedAndLegacyBindings() {
        for (var event : WidgetEventCatalog.eventsFor(BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow())) {
            var typed = generate(Map.of(event.propertyName(), reference(Optional.empty(), "_handler")));
            var legacy = generate(Map.of(event.propertyName(), new PropertyValue.CallbackValue("_handler")));
            assertEquals(typed.build().payload(), legacy.build().payload());
            for (var generated : List.of(typed, legacy)) {
                var symbols = generated.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).toList();
                assertEquals(1, symbols.size());
                var symbol = symbols.getFirst();
                var proof = symbol.staticTypeRequirement().orElseThrow();
                assertEquals(event.callbackType(), proof.expectedDartType());
                assertEquals("_handler", generated.build().payload().substring(proof.expressionOffset(), proof.expressionEndOffset()));
                assertEquals("/root/properties/" + event.propertyName().value() + "/rootSymbol", symbol.modelPath());
            }
            var nil = generate(Map.of(event.propertyName(), new PropertyValue.NullValue()));
            assertTrue(nil.build().payload().contains(event.propertyName().value() + ": null"));
            assertTrue(nil.build().payload().contains("const Focus("));
        }
    }

    @Test void standardNullableAndExternalNonNullableNodeProofAreDistinctIncludingFactoriesAndAliases() {
        for (String variant : List.of("standard", "withExternalFocusNode")) {
            for (var node : List.of(reference(Optional.of("package:app/nodes.dart"), "node"),
                    new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/nodes.dart"), "Nodes", Optional.of("shared"),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                    new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_nodeFactory", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)))) {
                var generated = generate(Map.of(new PropertyName("variant"), new PropertyValue.StringValue(variant),
                        new PropertyName("focusNode"), node, new PropertyName("parentNode"), reference(Optional.empty(), "_parent")));
                var symbols = generated.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).toList();
                assertEquals(2, symbols.size());
                var focused = symbols.stream().filter(s -> s.modelPath().contains("/focusNode/")).findFirst().orElseThrow();
                assertEquals(variant.equals("standard") ? "FocusNode?" : "FocusNode", focused.staticTypeRequirement().orElseThrow().expectedDartType());
                var parent = symbols.stream().filter(s -> s.modelPath().contains("/parentNode/")).findFirst().orElseThrow();
                assertEquals("FocusNode?", parent.staticTypeRequirement().orElseThrow().expectedDartType());
                if (!variant.equals("standard")) {
                    assertTrue(generated.build().payload().contains("Focus.withExternalFocusNode("));
                    var constructor = generated.symbolOccurrences().stream().filter(s -> s.symbolName().equals("withExternalFocusNode")).findFirst().orElseThrow();
                    assertEquals("package:flutter/widgets.dart", constructor.libraryUri());
                    assertEquals("/root/properties/variant", constructor.modelPath());
                    assertEquals("withExternalFocusNode", generated.build().payload().substring(constructor.offset(), constructor.endOffset()));
                }
                node.libraryUri().ifPresent(uri -> assertTrue(generated.imports().payload().contains(uri)));
            }
        }
    }

    @Test void externalSuppressesAllSevenStoredFieldsIncludingTheirImportsAndStaticProofs() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode"));
        values.put(new PropertyName("focusNode"), reference(Optional.empty(), "_node"));
        values.put(new PropertyName("onFocusChange"), new PropertyValue.CallbackValue("_changed"));
        values.put(new PropertyName("onKeyEvent"), reference(Optional.of("package:missing/inactive.dart"), "keyHandler"));
        values.put(new PropertyName("onKey"), reference(Optional.of("package:missing/legacy.dart"), "legacyHandler"));
        values.put(new PropertyName("debugLabel"), new PropertyValue.StringValue("inactive marker"));
        FocusWidgetPropertySchema.booleanProperties().forEach(name -> values.put(new PropertyName(name), new PropertyValue.BooleanValue(true)));
        var generated = generate(values);
        String build = generated.build().payload();
        assertTrue(build.contains("onFocusChange: _changed"), build);
        assertTrue(build.contains("autofocus: true"), build);
        assertTrue(build.contains("includeSemantics: true"), build);
        for (String name : FocusWidgetPropertySchema.standardOnlyProperties()) {
            assertFalse(build.contains(name + ":"), name);
            assertTrue(generated.symbolOccurrences().stream().noneMatch(s -> s.modelPath().contains("/properties/" + name + "/")), name);
        }
        assertFalse(generated.imports().payload().contains("package:missing/"));
        assertEquals(2, generated.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).count());
        values.put(new PropertyName("variant"), new PropertyValue.StringValue("standard"));
        var restored = generate(values);
        assertTrue(restored.imports().payload().contains("package:missing/inactive.dart"));
        assertTrue(restored.build().payload().contains("debugLabel: 'inactive marker'"));
        assertEquals(4, restored.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).count());
    }

    @Test void sixBooleanStateConsumersEmitOnlyActiveBindingsAndRetainInactiveLiteralPreview() {
        for (String variant : List.of("standard", "withExternalFocusNode")) {
            var values = Map.<PropertyName, PropertyValue>of(new PropertyName("variant"), new PropertyValue.StringValue(variant),
                    new PropertyName("focusNode"), reference(Optional.empty(), "_node"));
            var bindings = new LinkedHashMap<PropertyName, StatePropertyBinding>();
            FocusWidgetPropertySchema.booleanProperties().forEach(name -> bindings.put(new PropertyName(name),
                    new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT)));
            var root = node(values, bindings);
            var generated = generate(root, WidgetClassKind.STATEFUL);
            assertEquals(6, root.propertyBindings().size());
            assertTrue(root.stateBinding().isEmpty());
            for (String name : FocusWidgetPropertySchema.booleanProperties()) {
                assertEquals(FocusWidgetPropertySchema.propertyAvailable(root, new PropertyName(name)),
                        generated.build().payload().contains(name + ": !_flag"), name + generated.build().payload());
            }
        }
        var nullable = new StatePropertyBinding("_optional", StateBinding.Type.NULLABLE_BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
        var generated = generate(node(Map.of(), Map.of(new PropertyName("canRequestFocus"), nullable)), WidgetClassKind.STATEFUL);
        assertTrue(generated.build().payload().contains("canRequestFocus: _optional"));
    }

    @Test void inactiveEqualsReferenceStateConsumerDoesNotLeakImportedTypeOrSymbolProof() {
        var binding = new StatePropertyBinding("_selection", StateBinding.Type.NULLABLE_REFERENCE,
                Optional.of(reference(Optional.of("package:missing/selection.dart"), "Selection")),
                StatePropertyBinding.Transform.EQUALS, Optional.of(new PropertyValue.NullValue()));
        var values = Map.<PropertyName, PropertyValue>of(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode"),
                new PropertyName("focusNode"), reference(Optional.empty(), "_node"));
        var root = node(values, Map.of(new PropertyName("skipTraversal"), binding));
        var generated = generate(root, WidgetClassKind.STATEFUL);
        assertFalse(generated.build().payload().contains("skipTraversal:"));
        assertFalse(generated.imports().payload().contains("package:missing/selection.dart"));
        assertTrue(generated.symbolOccurrences().stream().noneMatch(symbol -> symbol.symbolName().equals("Selection") || symbol.symbolName().equals("_selection")));
        assertEquals(binding, root.propertyBindings().get(new PropertyName("skipTraversal")));
    }

    @Test void inactiveConsumerRetainsOldFieldTypeWithoutConflictingWithAnActiveConsumerUntilReactivated() {
        var active = node(Map.of(), Map.of(new PropertyName("autofocus"),
                new StatePropertyBinding("_shared", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT)));
        var retained = new StatePropertyBinding("_shared", StateBinding.Type.NULLABLE_BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
        var external = node(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode"),
                new PropertyName("focusNode"), reference(Optional.empty(), "_node")), Map.of(new PropertyName("canRequestFocus"), retained));
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(active, external))));
        String build = generate(root, WidgetClassKind.STATEFUL).build().payload();
        assertTrue(build.contains("autofocus: _shared"));
        assertFalse(build.contains("canRequestFocus:"));
        assertEquals(retained, external.propertyBindings().get(new PropertyName("canRequestFocus")));
        var standard = new WidgetNode(external.id(), TYPE, Map.of(), external.slots(), external.extensions(), external.stateBinding(), external.propertyBindings());
        var conflict = new WidgetNode(root.id(), root.type(), root.properties(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(active, standard))));
        var region = new ManagedRegion("0".repeat(64));
        var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATEFUL,
                Optional.empty(), new ManagedRegions(region, region)), conflict);
        var result = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault());
        assertFalse(result.successful(), "Reactivated incompatible State member types must fail closed");
    }

    private static PropertyValue.DartObjectReferenceValue reference(Optional<String> library, String name) {
        return new PropertyValue.DartObjectReferenceValue(library, name, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values, Map<PropertyName, StatePropertyBinding> bindings) {
        var text = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        return new WidgetNode(StableId.random(), TYPE, values, Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(text))), Extensions.empty(), Optional.empty(), bindings);
    }
    private static GeneratedDartRegions generate(Map<PropertyName, PropertyValue> values) { return generate(node(values, Map.of()), WidgetClassKind.STATELESS); }
    private static GeneratedDartRegions generate(WidgetNode root, WidgetClassKind kind) {
        var region = new ManagedRegion("0".repeat(64));
        var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.empty(), new ManagedRegions(region, region)), root);
        var result = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
}
