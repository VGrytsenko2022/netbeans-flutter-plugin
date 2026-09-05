package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RepaintBoundaryContractTest {
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.RepaintBoundary");
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void exactSdkStructuralContractHasNoScalarPropertiesAndOneOptionalChild() {
        var definition = definition();
        assertEquals("RepaintBoundary", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(Optional.empty(), definition.namedConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 150, "RepaintBoundary"), definition.palette());
        assertTrue(definition.properties().isEmpty());
        assertTrue(definition.traits().isEmpty());
        assertEquals(1, definition.slots().size());
        var child = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(0, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertTrue(projection.properties().isEmpty());
        assertTrue(projection.propertyContracts().isEmpty());
        assertEquals(Set.of(CHILD), projection.slots());
    }

    @Test
    void keyDerivedConveniencesAndAlteredDefinitionsCannotBorrowCanonicalCapabilities() {
        var canonical = definition();
        for (String named : List.of("wrap", "wrapAll")) {
            var altered = new WidgetDefinition(TYPE, canonical.dartClassName(), Optional.of(named), true,
                    canonical.dartLibraryUri(), canonical.importUris(), canonical.traits(), canonical.palette(),
                    canonical.properties(), canonical.slots());
            assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(altered).isEmpty());
            assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(altered).isEmpty());
        }
        var missingChildContract = new WidgetDefinition(TYPE, canonical.dartClassName(), Optional.empty(), true,
                canonical.dartLibraryUri(), canonical.importUris(), canonical.traits(), canonical.palette(),
                canonical.properties(), List.of());
        assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(missingChildContract).isEmpty());
    }

    @Test
    void emptyAndNestedChildrenGenerateDeterministicConstAndExactSdkNavigationEvidence() {
        for (var child : List.of(Optional.<WidgetNode>empty(), Optional.of(text(false)),
                Optional.of(boundary(Optional.of(text(false)))))) {
            var root = boundary(child);
            var first = generate(root);
            var second = generate(root);
            assertEquals(first, second);
            String build = first.build().payload();
            assertTrue(build.contains("return const RepaintBoundary("), build);
            assertTrue(build.contains(child.isEmpty() ? "child: null" : "child: const"), build);
            assertEquals("import 'package:flutter/widgets.dart';\n", first.imports().payload());
            var occurrences = first.symbolOccurrences().stream()
                    .filter(item -> item.symbolName().equals("RepaintBoundary")).toList();
            assertFalse(occurrences.isEmpty());
            for (var occurrence : occurrences) {
                assertEquals("package:flutter/widgets.dart", occurrence.libraryUri());
                assertEquals("RepaintBoundary", build.substring(occurrence.offset(), occurrence.endOffset()));
            }
            for (String invented : List.of("key:", "wrap(", "wrapAll(", "toImage(", "debugRepaint")) {
                assertFalse(build.contains(invented), build);
            }
        }
    }

    @Test
    void nonConstDescendantRemovesConstFromBothBoundariesWithoutChangingChildSemantics() {
        var generated = generate(boundary(Optional.of(boundary(Optional.of(text(true))))));
        String build = generated.build().payload();
        assertTrue(build.contains("return RepaintBoundary("), build);
        assertTrue(build.contains("child: RepaintBoundary("), build);
        assertFalse(build.contains("const RepaintBoundary("), build);
        assertTrue(build.contains("Theme.of(context).colorScheme.primary"), build);
        assertEquals("import 'package:flutter/material.dart';\n", generated.imports().payload());
    }

    @Test
    void zeroPropertyModelsRoundTripWithoutChangingVersionsOrStableIdentity() throws Exception {
        var codec = new FdDocumentCodec();
        for (var child : List.of(Optional.<WidgetNode>empty(), Optional.of(text(false)))) {
            var original = document(boundary(child));
            var bytes = codec.encode(original);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
            assertEquals(original, decoded.document());
            assertFalse(decoded.migrated());
            assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
        }
        assertEquals(13, DesignerDocument.SCHEMA_VERSION);
        assertEquals(14, WidgetCatalog.API_VERSION);
        assertEquals(18, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void modelValidatorRejectsInventedPropertiesUnknownSlotsAndWrongCardinality() {
        var validator = new WidgetTreeValidator();
        assertTrue(validator.validate(document(boundary(Optional.empty())), BuiltInWidgetCatalog.getDefault()).valid());
        for (String field : List.of("key", "index", "wrap", "wrapAll", "toImage", "isRepaintBoundary")) {
            var invalid = new WidgetNode(StableId.random(), TYPE,
                    Map.of(new PropertyName(field), new PropertyValue.BooleanValue(true)), Map.of());
            var result = validator.validate(document(invalid), BuiltInWidgetCatalog.getDefault());
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(issue -> issue.path().equals("/root/properties/" + field)));
        }
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(CHILD, new WidgetSlot.ListSlot(List.of(text(false)))),
                Map.of(new SlotName("children"), WidgetSlot.SingleSlot.of(text(false))))) {
            var invalid = new WidgetNode(StableId.random(), TYPE, Map.of(), slots);
            assertFalse(validator.validate(document(invalid), BuiltInWidgetCatalog.getDefault()).valid());
        }
    }

    private static WidgetDefinition definition() {
        return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();
    }
    private static WidgetNode boundary(Optional<WidgetNode> child) {
        return new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(CHILD, new WidgetSlot.SingleSlot(child)));
    }
    private static WidgetNode text(boolean themed) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), themed
                ? Map.of(new PropertyName("data"), new PropertyValue.StringValue("Retained child"),
                        new PropertyName("styleColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")))
                : Map.of(new PropertyName("data"), new PropertyValue.StringValue("Retained child")), Map.of());
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
    private static GeneratedDartRegions generate(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
}
