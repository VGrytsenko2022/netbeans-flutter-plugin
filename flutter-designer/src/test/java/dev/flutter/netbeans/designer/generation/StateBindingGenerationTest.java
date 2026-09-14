package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StateBindingGenerationTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test void controlledValuesBecomeProvedFieldReadsWithoutChangingPreviewProperties() {
        for (String type : List.of("flutter.material.Checkbox", "flutter.material.CheckboxListTile",
                "flutter.material.Switch", "flutter.material.Slider", "flutter.material.RangeSlider",
                "flutter.widgets.RadioGroup")) {
            WidgetNode plain = prototype(type);
            WidgetNode bound = bind(plain);
            var result = new DartRegionGenerator().generate(document(bound), CATALOG);
            assertTrue(result.successful(), type + ": " + result.diagnostics());
            var generated = result.generated().orElseThrow();
            String argument = type.endsWith("RadioGroup") ? "groupValue" : type.endsWith("RangeSlider") ? "values" : "value";
            assertTrue(generated.build().payload().contains(argument + ": _stateValue"), generated.build().payload());
            assertFalse(generated.build().payload().contains("const " + type.substring(type.lastIndexOf('.') + 1) + "("));
            assertEquals(plain.properties(), bound.properties(), "Preview literals must remain unchanged");
            var fields = generated.symbolOccurrences().stream()
                    .filter(value -> value.modelPath().endsWith("/stateBinding/fieldName/rootSymbol")).toList();
            assertEquals(1, fields.size());
            assertEquals("_stateValue", fields.getFirst().symbolName());
            assertEquals(DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI, fields.getFirst().libraryUri());
            assertTrue(fields.getFirst().staticTypeRequirement().isPresent());
            var restored = new WidgetNode(bound.id(), bound.type(), bound.properties(), bound.slots(), bound.extensions());
            assertEquals(new DartRegionGenerator().generate(document(plain), CATALOG).generated().orElseThrow().build(),
                    new DartRegionGenerator().generate(document(restored), CATALOG).generated().orElseThrow().build());
        }
    }

    @Test void nullableCheckboxAndEveryRadioBuiltinRetainExactClosedTypeProof() {
        WidgetNode checkbox = with(prototype("flutter.material.Checkbox"), "tristate", new PropertyValue.BooleanValue(true));
        checkbox = with(checkbox, "value", new PropertyValue.NullValue());
        assertEquals("bool?", fieldProof(bind(checkbox)).expectedDartType());
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            WidgetNode radio = with(prototype("flutter.widgets.RadioGroup"), "valueType", new PropertyValue.StringValue(type));
            var proof = fieldProof(bind(radio));
            assertEquals("Object?", proof.expectedDartType());
            assertEquals(Optional.of(type), proof.sourceTypeOverride());
            radio = with(radio, "nullableValueType", new PropertyValue.BooleanValue(true));
            assertEquals(Optional.of(type + "?"), fieldProof(bind(radio)).sourceTypeOverride());
        }
    }

    @Test void radioProjectTypeUsesTheSameQualifiedTypeIdentityAsTheConstructor() {
        WidgetNode radio = with(prototype("flutter.widgets.RadioGroup"), "valueType",
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/types.dart"), "Choice",
                        Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
        var proof = fieldProof(bind(radio));
        assertEquals("Object?", proof.expectedDartType());
        assertTrue(proof.sourceTypeOverride().orElseThrow().endsWith(".Choice"));
    }

    @Test void dependentPropertiesRetainPreviewAndExactFieldProofForEveryTransform() {
        record Sample(String widget, String property, StateBinding.Type type,
                StatePropertyBinding.Transform transform, Optional<PropertyValue> comparison, String expression) {}
        for (Sample sample : List.of(
                new Sample("flutter.widgets.Text", "data", StateBinding.Type.STRING, StatePropertyBinding.Transform.DIRECT, Optional.empty(), "_field"),
                new Sample("flutter.widgets.Text", "data", StateBinding.Type.NULLABLE_INT, StatePropertyBinding.Transform.TO_STRING, Optional.empty(), "_field.toString()"),
                new Sample("flutter.widgets.Text", "data", StateBinding.Type.TEXT_CONTROLLER, StatePropertyBinding.Transform.TEXT, Optional.empty(), "_field.text"),
                new Sample("flutter.widgets.Offstage", "offstage", StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT, Optional.empty(), "!_field"),
                new Sample("flutter.widgets.MouseRegion", "opaque", StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT, Optional.empty(), "opaque: _field"),
                new Sample("flutter.widgets.MouseRegion", "opaque", StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT, Optional.empty(), "opaque: !_field"),
                new Sample("flutter.widgets.MouseRegion", "opaque", StateBinding.Type.NULLABLE_INT, StatePropertyBinding.Transform.EQUALS,
                        Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.ONE)), "opaque: (_field == 1)"),
                new Sample("flutter.widgets.GestureDetector", "excludeFromSemantics", StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT, Optional.empty(), "excludeFromSemantics: _field"),
                new Sample("flutter.widgets.GestureDetector", "excludeFromSemantics", StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT, Optional.empty(), "excludeFromSemantics: !_field"),
                new Sample("flutter.widgets.GestureDetector", "trackpadScrollCausesScale", StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT, Optional.empty(), "trackpadScrollCausesScale: _field"),
                new Sample("flutter.widgets.GestureDetector", "trackpadScrollCausesScale", StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT, Optional.empty(), "trackpadScrollCausesScale: !_field"),
                new Sample("flutter.widgets.Visibility", "visible", StateBinding.Type.NULLABLE_STRING, StatePropertyBinding.Transform.EQUALS,
                        Optional.of(new PropertyValue.StringValue("it's selected")), "(_field == 'it\\'s selected')"),
                new Sample("flutter.widgets.Opacity", "opacity", StateBinding.Type.DOUBLE, StatePropertyBinding.Transform.CLAMP, Optional.empty(), "_field.clamp(0.0, 1.0).toDouble()"))) {
            WidgetNode plain = prototype(sample.widget());
            var binding = new StatePropertyBinding("_field", sample.type(), Optional.empty(), sample.transform(), sample.comparison());
            WidgetNode bound = consumer(plain, sample.property(), binding);
            var result = new DartRegionGenerator().generate(document(bound), CATALOG);
            assertTrue(result.successful(), () -> sample + ": " + result.diagnostics());
            var generated = result.generated().orElseThrow();
            assertTrue(generated.build().payload().contains(sample.expression()), generated.build().payload());
            assertEquals(plain.properties(), bound.properties());
            var proof = generated.symbolOccurrences().stream().filter(value -> value.modelPath()
                    .endsWith("/propertyBindings/" + sample.property() + "/fieldName/rootSymbol")).toList();
            assertEquals(1, proof.size(), generated.symbolOccurrences().toString());
            assertEquals("_field", proof.getFirst().symbolName());
            assertEquals(WidgetStateBindingCatalog.dartType(sample.type(), Optional.empty()),
                    proof.getFirst().staticTypeRequirement().orElseThrow().expectedDartType());
        }
    }

    @Test void emptyAndPopulatedIndexedStackAlwaysUseSafeBoundsAndRetainFieldEvidence() {
        WidgetNode plain = prototype("flutter.widgets.IndexedStack");
        var binding = new StatePropertyBinding("_index", StateBinding.Type.INT, Optional.empty(),
                StatePropertyBinding.Transform.CLAMP, Optional.empty());
        for (int count : List.of(0, 1, 3)) {
            List<WidgetNode> children = java.util.stream.IntStream.range(0, count)
                    .mapToObj(index -> prototype("flutter.widgets.Text")).toList();
            WidgetNode stack = new WidgetNode(plain.id(), plain.type(), plain.properties(),
                    Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
            var result = new DartRegionGenerator().generate(document(consumer(stack, "index", binding)), CATALOG);
            assertTrue(result.successful(), result.diagnostics().toString());
            var generated = result.generated().orElseThrow();
            assertTrue(generated.build().payload().contains(count == 0 ? "(_index < 0 ? null : null)"
                    : "_index.clamp(0, " + (count - 1) + ").toInt()"), generated.build().payload());
            assertEquals(1, generated.symbolOccurrences().stream().filter(value -> value.modelPath()
                    .endsWith("/propertyBindings/index/fieldName/rootSymbol")).count());
        }
    }

    @Test void projectTypedConsumerHasItsOwnTypeProvenanceWithoutRadioGroup() {
        var type = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/types.dart"), "Choice",
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var binding = new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_REFERENCE, Optional.of(type),
                StatePropertyBinding.Transform.TO_STRING, Optional.empty());
        var result = new DartRegionGenerator().generate(document(consumer(prototype("flutter.widgets.Text"), "data", binding)), CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        var generated = result.generated().orElseThrow();
        assertTrue(generated.build().payload().contains(".Choice?).toString()"), generated.build().payload());
        assertTrue(generated.symbolOccurrences().stream().anyMatch(value -> value.symbolName().equals("Choice")
                && value.libraryUri().equals("package:app/types.dart") && value.modelPath().contains("/referenceType/")));
        assertTrue(generated.symbolOccurrences().stream().anyMatch(value -> value.symbolName().equals("_choice")
                && value.staticTypeRequirement().orElseThrow().sourceTypeOverride().orElse("").endsWith(".Choice?")));
    }

    private static WidgetNode consumer(WidgetNode node, String property, StatePropertyBinding binding) {
        return new WidgetNode(node.id(), node.type(), node.properties(), node.slots(), node.extensions(), node.stateBinding(),
                Map.of(new PropertyName(property), binding));
    }

    private static GeneratedDartStaticTypeRequirement fieldProof(WidgetNode node) {
        var generated = new DartRegionGenerator().generate(document(node), CATALOG);
        assertTrue(generated.successful(), () -> generated.diagnostics().toString());
        return generated.generated().orElseThrow().symbolOccurrences().stream()
                .filter(value -> value.modelPath().endsWith("/stateBinding/fieldName/rootSymbol"))
                .findFirst().orElseThrow().staticTypeRequirement().orElseThrow();
    }

    private static WidgetNode bind(WidgetNode node) {
        return new WidgetNode(node.id(), node.type(), node.properties(), node.slots(), node.extensions(),
                Optional.of(WidgetStateBindingCatalog.createBinding(node, "_stateValue", "_stateChanged")));
    }

    private static WidgetNode prototype(String type) {
        WidgetNode node = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId(type)).orElseThrow(), StableId.random());
        if (type.equals("flutter.widgets.RadioGroup") || type.equals("flutter.widgets.Visibility")) {
            node = new WidgetNode(node.id(), node.type(), node.properties(), Map.of(new SlotName("child"),
                    WidgetSlot.SingleSlot.of(WidgetNodePrototypeFactory.create(CATALOG.find(
                            new WidgetTypeId("flutter.widgets.SizedBox")).orElseThrow(), StableId.random()))));
        }
        return node;
    }

    private static WidgetNode with(WidgetNode node, String name, PropertyValue value) {
        var properties = new LinkedHashMap<>(node.properties());
        properties.put(new PropertyName(name), value);
        return new WidgetNode(node.id(), node.type(), properties, node.slots());
    }

    private static DesignerDocument document(WidgetNode node) {
        var descriptor = new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATEFUL,
                Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion("0".repeat(64)), new ManagedRegion("0".repeat(64))));
        return new DesignerDocument(StableId.random(), descriptor, node);
    }
}
