package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.model.PropertyValue.PointerDeviceKindSetValue;
import io.github.vgrytsenko2022.designer.model.PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GestureDetectorDartGenerationTest {
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.GestureDetector");

    @Test
    void emptyWrapperIsNonConstAndDoesNotInventRuntimeGesturesOrDeviceFilters() {
        var generated = generate(Map.of());
        String build = generated.build().payload();
        assertTrue(build.contains("return GestureDetector("), build);
        assertTrue(build.contains("child: null"), build);
        assertFalse(build.contains("const GestureDetector"), build);
        assertFalse(build.contains("onTap:"), build);
        assertFalse(build.contains("supportedDevices:"), build);
        assertFalse(generated.imports().payload().contains("dart:ui"));
        assertFalse(generated.imports().payload().contains("gestures.dart"));
    }

    @Test
    void everyDeviceSubsetHasAnExplicitConstantSetTypeAndExactAnalyzerOccurrences() {
        PointerDeviceKind[] devices = PointerDeviceKind.values();
        for (int mask = 0; mask < (1 << devices.length); mask++) {
            List<PointerDeviceKind> selected = new ArrayList<>();
            for (int i = 0; i < devices.length; i++) if ((mask & (1 << i)) != 0) selected.add(devices[i]);
            var generated = generate(Map.of(new PropertyName("supportedDevices"), new PointerDeviceKindSetValue(selected)));
            verifyDeviceEvidence(generated, selected);
        }
    }

    private void verifyDeviceEvidence(GeneratedDartRegions first, List<PointerDeviceKind> selected) {
        String build = first.build().payload();
        String imports = first.imports().payload();
        assertTrue(imports.contains("import 'dart:ui' as "), imports);
        var directive = first.importPlan().directives().stream().filter(d -> d.uri().equals("dart:ui")).findFirst().orElseThrow();
        String type = directive.prefix().orElseThrow() + ".PointerDeviceKind";
        String expected = "supportedDevices: const <" + type + ">{"
                + selected.stream().map(d -> type + "." + d.wireName()).collect(java.util.stream.Collectors.joining(", ")) + "}";
        assertTrue(build.contains(expected), build);
        var symbols = first.symbolOccurrences().stream().filter(s -> s.symbolName().equals("PointerDeviceKind")).toList();
        assertEquals(selected.size() + 1, symbols.size());
        for (var symbol : symbols) {
            assertEquals("dart:ui", symbol.libraryUri());
            assertEquals("PointerDeviceKind", build.substring(symbol.offset(), symbol.endOffset()));
            assertEquals("/root/properties/supportedDevices", symbol.modelPath());
        }
    }

    @Test
    void explicitNullAndEmptySetNeverCollapseIntoOmission() {
        String omitted = generate(Map.of()).build().payload();
        var nil = generate(Map.of(new PropertyName("supportedDevices"), new PropertyValue.NullValue()));
        String empty = generate(Map.of(new PropertyName("supportedDevices"), new PointerDeviceKindSetValue(List.of()))).build().payload();
        assertFalse(omitted.contains("supportedDevices:"));
        assertTrue(nil.build().payload().contains("supportedDevices: null"));
        assertFalse(nil.imports().payload().contains("dart:ui"));
        assertTrue(empty.contains("PointerDeviceKind>{}"), empty);
    }

    @Test
    void all58CallbacksSupportTypedReferencesWithExactFunctionEvidenceAndExplicitNull() {
        var definition = BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();
        var callbacks = WidgetEventCatalog.eventsFor(definition);
        assertEquals(58, callbacks.size());
        for (var event : callbacks) {
            var value = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_handler", Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            var generated = generate(Map.of(event.propertyName(), value));
            assertTrue(generated.build().payload().contains(event.propertyName() + ": _handler"), generated.build().payload());
            var occurrence = generated.symbolOccurrences().stream().filter(s -> s.symbolName().equals("_handler")).findFirst().orElseThrow();
            assertTrue(occurrence.staticTypeRequirement().isPresent(), event.toString());
            var shorthand = generate(Map.of(event.propertyName(), new PropertyValue.CallbackValue("_handler")));
            assertEquals(generated.build().payload(), shorthand.build().payload());
            assertTrue(shorthand.symbolOccurrences().stream().filter(s -> s.symbolName().equals("_handler"))
                    .allMatch(s -> s.staticTypeRequirement().isPresent()));
            assertEquals(1, shorthand.symbolOccurrences().stream().filter(s -> s.symbolName().equals("_handler")).count());
            assertTrue(generate(Map.of(event.propertyName(), new PropertyValue.NullValue())).build().payload()
                    .contains(event.propertyName() + ": null"));
        }
    }

    private static GeneratedDartRegions generate(Map<PropertyName, PropertyValue> properties) {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var prototype = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        var root = new WidgetNode(prototype.id(), TYPE, properties, prototype.slots());
        var region = new ManagedRegion("0".repeat(64));
        var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
        var result = new DartRegionGenerator().generate(document, catalog);
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals(result.generated(), new DartRegionGenerator().generate(document, catalog).generated());
        return result.generated().orElseThrow();
    }
}
