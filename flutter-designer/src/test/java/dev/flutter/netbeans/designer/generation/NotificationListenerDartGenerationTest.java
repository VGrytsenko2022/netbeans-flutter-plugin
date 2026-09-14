package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationListenerDartGenerationTest {
    private static final WidgetTypeId TYPE = NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE;

    @Test void allFourteenConstGenericPresetsHaveBoundProofEvenWithoutCallback() {
        for (String name : NotificationListenerWidgetPropertySchema.typePresets()) {
            for (boolean explicitNull : List.of(false, true)) {
                var fields = new LinkedHashMap<PropertyName, PropertyValue>();
                fields.put(p("notificationType"), new PropertyValue.StringValue(name));
                if (explicitNull) fields.put(p("onNotification"), new PropertyValue.NullValue());
                var generated = generate(fields);
                String build = generated.build().payload();
                assertTrue(build.contains("return const NotificationListener<" + name + ">("), build);
                assertTrue(build.contains("child: const Text("), build);
                assertFalse(build.contains("notificationType:"), build);
                assertEquals(explicitNull, build.contains("onNotification: null"));
                var proofs = generated.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).toList();
                assertEquals(1, proofs.size());
                var occurrence = proofs.getFirst();
                var proof = occurrence.staticTypeRequirement().orElseThrow();
                assertEquals("Type", proof.expectedDartType());
                assertEquals(Optional.of(name), proof.sourceTypeOverride());
                assertEquals(Optional.of("Notification"), proof.sourceTypeBound());
                assertEquals("package:flutter/widgets.dart", occurrence.libraryUri());
                assertEquals("/root/properties/notificationType", occurrence.modelPath());
                assertEquals(name, build.substring(proof.expressionOffset(), proof.expressionEndOffset()));
            }
        }
    }

    @Test void everySelectedTypeIsCarriedIntoTypedAndLegacyCallbackProofWithoutChangingRepresentation() {
        for (String name : NotificationListenerWidgetPropertySchema.typePresets()) {
            var typed = generate(Map.of(p("notificationType"), new PropertyValue.StringValue(name), p("onNotification"), reference(Optional.empty(), "_notice")));
            var legacy = generate(Map.of(p("notificationType"), new PropertyValue.StringValue(name), p("onNotification"), new PropertyValue.CallbackValue("_notice")));
            assertEquals(typed.build().payload(), legacy.build().payload());
            for (var generated : List.of(typed, legacy)) {
                assertFalse(generated.build().payload().contains("const NotificationListener<"));
                var callback = generated.symbolOccurrences().stream().filter(s -> s.modelPath().equals("/root/properties/onNotification/rootSymbol")).findFirst().orElseThrow();
                var proof = callback.staticTypeRequirement().orElseThrow();
                assertEquals(NotificationListenerWidgetPropertySchema.CALLBACK_TYPE, proof.expectedDartType());
                assertEquals(Optional.of(name), proof.sourceTypeOverride());
                assertTrue(proof.sourceTypeBound().isEmpty(), "The separate generic T proof owns the bound");
                assertEquals("_notice", generated.build().payload().substring(proof.expressionOffset(), proof.expressionEndOffset()));
                assertEquals(2, generated.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).count());
            }
        }
    }

    @Test void importedAndLocalCustomTypesRetainExactTypeIdentityAndIndependentBoundWhenHandlerIsMissingOrBound() {
        for (Optional<String> library : List.of(Optional.<String>empty(), Optional.of("package:app/notices.dart"))) {
            for (boolean bind : List.of(false, true)) {
                var fields = new LinkedHashMap<PropertyName, PropertyValue>();
                fields.put(p("notificationType"), reference(library, "CustomNotice"));
                if (bind) fields.put(p("onNotification"), reference(Optional.of("package:app/handlers.dart"), "handleNotice"));
                var generated = generate(fields);
                var type = generated.symbolOccurrences().stream().filter(s -> s.modelPath().equals("/root/properties/notificationType/rootSymbol")).findFirst().orElseThrow();
                assertEquals(library.orElse(DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI), type.libraryUri());
                var typeProof = type.staticTypeRequirement().orElseThrow();
                assertEquals(Optional.of("Notification"), typeProof.sourceTypeBound());
                String selected = typeProof.sourceTypeOverride().orElseThrow();
                assertTrue(selected.endsWith("CustomNotice"));
                assertEquals(selected, generated.build().payload().substring(typeProof.expressionOffset(), typeProof.expressionEndOffset()));
                assertTrue(generated.build().payload().contains("NotificationListener<" + selected + ">"));
                assertEquals(!bind, generated.build().payload().contains("const NotificationListener"));
                if (library.isPresent()) {
                    var prefix = generated.importPlan().directives().stream().filter(d -> d.uri().equals(library.orElseThrow())).findFirst().orElseThrow().prefix().orElseThrow();
                    assertEquals(prefix + ".CustomNotice", selected);
                }
                if (bind) {
                    var callback = generated.symbolOccurrences().stream().filter(s -> s.modelPath().equals("/root/properties/onNotification/rootSymbol")).findFirst().orElseThrow();
                    assertEquals("package:app/handlers.dart", callback.libraryUri());
                    assertEquals(Optional.of(selected), callback.staticTypeRequirement().orElseThrow().sourceTypeOverride());
                }
            }
        }
    }

    @Test void callbackMembersAndFactoriesUseWholeExpressionProofWithoutWeakeningTypeReferenceRestrictions() {
        for (var callback : List.of(new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/handlers.dart"), "Handlers", Optional.of("onNotice"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_callbackFactory", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)))) {
            var generated = generate(Map.of(p("notificationType"), new PropertyValue.StringValue("ScrollNotification"), p("onNotification"), callback));
            var occurrence = generated.symbolOccurrences().stream().filter(s -> s.modelPath().startsWith("/root/properties/onNotification/") && s.staticTypeRequirement().isPresent()).findFirst().orElseThrow();
            var proof = occurrence.staticTypeRequirement().orElseThrow();
            assertEquals(Optional.of("ScrollNotification"), proof.sourceTypeOverride());
            String expression = generated.build().payload().substring(proof.expressionOffset(), proof.expressionEndOffset());
            assertTrue(callback.member().isPresent() ? expression.endsWith("Handlers.onNotice") : expression.equals("_callbackFactory()"), expression);
        }
    }

    @Test void boundRecordPreservesOldConstructorsAndShiftsWithoutBroadeningRadioOrArbitraryBounds() {
        var old = new GeneratedDartStaticTypeRequirement(2, 3, "Type", Optional.of("types.Choice?"));
        assertTrue(old.sourceTypeBound().isEmpty());
        assertEquals(Optional.of("types.Choice?"), old.shifted(11).sourceTypeOverride());
        var bound = new GeneratedDartStaticTypeRequirement(2, 3, "Type", Optional.of("types.Notice"), Optional.of("Notification"));
        assertEquals(13, bound.shifted(11).expressionOffset());
        assertEquals(bound.sourceTypeBound(), bound.shifted(11).sourceTypeBound());
        assertEquals(bound.sourceTypeOverride(), bound.shifted(11).sourceTypeOverride());
        for (String wrongBound : List.of("Object", "Widget", "FocusNode", "Notification?", "pkg.Notification")) {
            assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, "Type", Optional.of("Notice"), Optional.of(wrongBound)));
        }
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, "Type", Optional.empty(), Optional.of("Notification")));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, "Type", Optional.of("Notice?"), Optional.of("Notification")));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, "Object", Optional.of("Notice"), Optional.of("Notification")));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, NotificationListenerWidgetPropertySchema.CALLBACK_TYPE, Optional.of("Notice?")));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, "VoidCallback", Optional.of("Notice")));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, "Type", Optional.of("List<Notice>"), Optional.of("Notification")));
        assertTrue(new GeneratedDartStaticTypeRequirement(0, 1, "FocusNode?").sourceTypeBound().isEmpty());
    }

    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DartObjectReferenceValue reference(Optional<String> library, String name) {
        return new PropertyValue.DartObjectReferenceValue(library, name, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static GeneratedDartRegions generate(Map<PropertyName, PropertyValue> properties) {
        var props = new LinkedHashMap<PropertyName, PropertyValue>();
        props.put(p("notificationType"), new PropertyValue.StringValue("Notification")); props.putAll(properties);
        var child = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        var root = new WidgetNode(StableId.random(), TYPE, props, Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(child))));
        var region = new ManagedRegion("0".repeat(64));
        var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
        var result = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
}
