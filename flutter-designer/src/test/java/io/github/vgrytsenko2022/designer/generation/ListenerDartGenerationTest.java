package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.ListenerWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ListenerDartGenerationTest {
    private static final WidgetTypeId TYPE = ListenerWidgetPropertySchema.LISTENER_TYPE;

    @Test
    void emptyListenerIsConstAndPreservesSdkDefaultsWithoutInventingHandlers() {
        var generated = generate(Map.of());
        String build = generated.build().payload();
        assertTrue(build.contains("return const Listener("), build);
        assertTrue(build.contains("child: null"), build);
        assertFalse(build.contains("onPointer"), build);
        assertFalse(build.contains("behavior:"), build);
        assertFalse(generated.imports().payload().contains("gestures.dart"));
        assertFalse(generated.imports().payload().contains("services.dart"));
        assertFalse(generated.imports().payload().contains("rendering.dart"));
    }

    @Test
    void allNineReferencesAndShorthandHandlersHaveIdenticalExactTypeProofAndSourceRanges() {
        for (var event : WidgetEventCatalog.eventsFor(BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow())) {
            var typed = generate(Map.of(event.propertyName(), reference(Optional.empty(), "_handler")));
            var shorthand = generate(Map.of(event.propertyName(), new PropertyValue.CallbackValue("_handler")));
            assertEquals(typed.build().payload(), shorthand.build().payload());
            for (var generated : List.of(typed, shorthand)) {
                String build = generated.build().payload();
                assertTrue(build.contains(event.propertyName().value() + ": _handler"), build);
                assertFalse(build.contains("const Listener"), build);
                var symbols = generated.symbolOccurrences().stream().filter(s -> s.symbolName().equals("_handler")).toList();
                assertEquals(1, symbols.size());
                var symbol = symbols.getFirst();
                assertEquals("_handler", build.substring(symbol.offset(), symbol.endOffset()));
                assertEquals("/root/properties/" + event.propertyName().value() + "/rootSymbol", symbol.modelPath());
                var proof = symbol.staticTypeRequirement().orElseThrow();
                assertEquals(event.callbackType(), proof.expectedDartType());
                assertEquals("_handler", build.substring(proof.expressionOffset(), proof.expressionEndOffset()));
            }
            var nil = generate(Map.of(event.propertyName(), new PropertyValue.NullValue()));
            assertTrue(nil.build().payload().contains(event.propertyName().value() + ": null"));
            assertTrue(nil.build().payload().contains("const Listener"));
            assertTrue(nil.symbolOccurrences().stream().noneMatch(s -> s.staticTypeRequirement().isPresent()));
        }
    }

    @Test
    void allNineCanBeBoundTogetherAndImportedCallbacksRetainTheirLibraryIdentity() {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        var events = WidgetEventCatalog.eventsFor(BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow());
        events.forEach(event -> properties.put(event.propertyName(),
                reference(Optional.of("package:app/pointer_handlers.dart"), event.propertyName().value())));
        var generated = generate(properties);
        assertEquals(9, generated.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).count());
        for (var event : events) {
            var symbol = generated.symbolOccurrences().stream().filter(s -> s.symbolName().equals(event.propertyName().value()))
                    .findFirst().orElseThrow();
            assertEquals("package:app/pointer_handlers.dart", symbol.libraryUri());
            assertEquals(event.callbackType(), symbol.staticTypeRequirement().orElseThrow().expectedDartType());
        }
    }

    @Test
    void eachBehaviorPresetHasExactRenderingSymbolEvidenceAndNoNullCoercion() {
        for (String behavior : List.of("deferToChild", "opaque", "translucent")) {
            var generated = generate(Map.of(new PropertyName("behavior"), new PropertyValue.EnumValue("HitTestBehavior", behavior)));
            assertTrue(generated.build().payload().contains("HitTestBehavior." + behavior), generated.build().payload());
            assertTrue(generated.build().payload().contains("const Listener"));
            var symbol = generated.symbolOccurrences().stream().filter(s -> s.symbolName().equals("HitTestBehavior"))
                    .findFirst().orElseThrow();
            assertEquals("package:flutter/rendering.dart", symbol.libraryUri());
            assertEquals("/root/properties/behavior", symbol.modelPath());
        }
        var result = new DartRegionGenerator().generate(document(Map.of(new PropertyName("behavior"), new PropertyValue.NullValue())),
                BuiltInWidgetCatalog.getDefault());
        assertFalse(result.successful());
    }

    private static PropertyValue.DartObjectReferenceValue reference(Optional<String> library, String name) {
        return new PropertyValue.DartObjectReferenceValue(library, name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static DesignerDocument document(Map<PropertyName, PropertyValue> properties) {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var prototype = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        var root = new WidgetNode(prototype.id(), TYPE, properties, prototype.slots());
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
    private static GeneratedDartRegions generate(Map<PropertyName, PropertyValue> properties) {
        var document = document(properties);
        var result = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals(result.generated(), new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).generated());
        return result.generated().orElseThrow();
    }
}
