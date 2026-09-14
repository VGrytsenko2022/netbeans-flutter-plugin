package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ThemeContractTest {
    public static final WidgetTypeId TYPE = ThemeWidgetPropertySchema.TYPE;
    public static final SlotName CHILD = new SlotName("child");
    static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    public static WidgetNode node() {
        var prototype = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random());
        return new WidgetNode(prototype.id(), TYPE, prototype.properties(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(PhysicalModelTestSupport.text())));
    }

    public static DesignerDocument document(WidgetNode node) {
        return PhysicalModelTestSupport.document(node);
    }

    private static WidgetNode data(PropertyValue value) {
        var node = node();
        return new WidgetNode(node.id(), TYPE, Map.of(new PropertyName("data"), value), node.slots());
    }

    private static GeneratedDartRegions generate(WidgetNode node) {
        var result = new DartRegionGenerator().generate(document(node), CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    @Test
    void exactConstructorRequiresDataAndChildAndHasNoAnimationOrEvents() {
        var definition = CATALOG.find(TYPE).orElseThrow();
        assertEquals(List.of("data"), definition.properties().stream().map(p -> p.name().value()).toList());
        assertTrue(definition.constConstructor());
        assertEquals(1, definition.slots().getFirst().parameter().order());
        assertTrue(definition.slots().getFirst().parameter().required());
        assertEquals(1, definition.slots().getFirst().minChildren());
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(definition).isPresent());
        assertTrue(WidgetPlacementRules.evaluateRoot(definition).accepted());
        assertTrue(WidgetEventCatalog.eventsFor(definition).isEmpty());
        assertEquals(Map.of(new PropertyName("data"), new PropertyValue.StringValue("light")), node().properties());
        var code = generate(node()).build().payload();
        assertTrue(code.contains("data: ThemeData.light()"), code);
        assertFalse(code.contains("const Theme("), code);
        for (String argument : List.of("curve:", "duration:", "onEnd:")) assertFalse(code.contains(argument), code);
    }

    @Test
    void sixNonConstFactoriesRoundTripWithExactMaterialSymbolEvidence() throws Exception {
        for (String preset : ThemeWidgetPropertySchema.PRESETS) {
            var document = document(data(new PropertyValue.StringValue(preset)));
            var codec = new FdDocumentCodec();
            assertEquals(document, ((FdDecodeResult.Current) codec.decode(codec.encode(document))).document());
            var result = generate(document.root());
            var code = result.build().payload();
            assertTrue(code.contains("ThemeData." + preset.replace("M2", "")
                    + (preset.endsWith("M2") ? "(useMaterial3: false)" : "()")), code);
            assertFalse(code.contains("const Theme("), code);
            var evidence = result.symbolOccurrences().stream().filter(o -> o.id().contains(":theme-")).toList();
            assertEquals(2, evidence.size());
            assertTrue(evidence.stream().allMatch(o -> o.libraryUri().equals("package:flutter/material.dart")));
        }
    }

    @Test
    void allStrictReferenceFormsAndClosedRequiredValueDomain() {
        for (boolean imported : List.of(false, true))
            for (boolean member : List.of(false, true))
                for (boolean factory : List.of(false, true)) {
                    var value = new PropertyValue.DartObjectReferenceValue(
                            imported ? Optional.of("package:sample/theme.dart") : Optional.empty(),
                            member ? "Themes" : "theme", member ? Optional.of("value") : Optional.empty(),
                            factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                                    : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                            factory ? Optional.of(false) : Optional.empty());
                    assertTrue(generate(data(value)).symbolOccurrences().stream()
                            .flatMap(o -> o.staticTypeRequirement().stream())
                            .anyMatch(t -> t.expectedDartType().equals("ThemeData")));
                }
        for (PropertyValue bad : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(true),
                new PropertyValue.StringValue("Theme.of(context)"), new PropertyValue.StringValue("darkM3"))) {
            assertFalse(new WidgetTreeValidator().validate(document(data(bad)), CATALOG).valid());
        }
        var node = node();
        assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(node.id(), TYPE, Map.of(), node.slots())), CATALOG).valid());
        assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(node.id(), TYPE, node.properties(),
                Map.of(CHILD, WidgetSlot.SingleSlot.empty()))), CATALOG).valid());
    }
}
