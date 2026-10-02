package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.canvas.payload.CanvasModelPayloadCodec;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.generation.GeneratedDartRegions;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IgnorePointerContractTest {
    private static final WidgetTypeId TYPE = IgnorePointerWidgetPropertySchema.IGNORE_POINTER_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final List<Optional<Boolean>> STATES = List.of(Optional.empty(), Optional.of(false), Optional.of(true));

    @Test
    void exactSdkConstructorAndPropertyHintsIncludeDeprecatedOverrideWithoutCopyingDefaults() {
        var definition = definition();
        assertEquals("IgnorePointer", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(Optional.empty(), definition.namedConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 160, "IgnorePointer"), definition.palette());
        assertEquals(List.of("ignoring", "ignoringSemantics"), definition.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(2, IgnorePointerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, IgnorePointerWidgetPropertySchema.SLOT_COUNT);
        int order = 0;
        for (var property : definition.properties()) {
            assertEquals(DartParameter.named(order++, false), property.parameter());
            assertEquals(Set.of(PropertyValueKind.BOOLEAN), property.acceptedKinds());
            assertTrue(property.creationDefault().isEmpty());
            assertTrue(property.constraints().getFirst().accepts(new PropertyValue.BooleanValue(true)));
            assertTrue(property.constraints().getFirst().accepts(new PropertyValue.BooleanValue(false)));
            assertFalse(property.constraints().getFirst().accepts(new PropertyValue.NullValue()));
            var hint = IgnorePointerWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(property.name().value(), hint.dartName());
            assertFalse(hint.description().isBlank());
            assertFalse(hint.group().description().isBlank());
        }
        var deprecated = IgnorePointerWidgetPropertySchema.find(new PropertyName("ignoringSemantics")).orElseThrow();
        assertTrue(deprecated.displayName().contains("deprecated"));
        assertTrue(deprecated.description().contains("null"));
        assertTrue(deprecated.description().contains("False preserves semantic actions"));
        assertTrue(IgnorePointerWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
        var slot = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(2, false), slot.parameter());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        assertEquals(0, slot.minChildren());
        assertEquals(1, slot.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, slot.acceptance());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void exactBooleanOnlyCapabilityCannotBeBroadenedByClaimingNullableOrRequiredFields() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        for (var property : projection.propertyContracts().values()) {
            assertEquals(Set.of(PropertyValueKind.BOOLEAN), property.acceptedKinds());
            assertEquals(Map.of(PropertyValueKind.BOOLEAN, "any"), property.constraintFingerprints());
            assertFalse(property.required());
            assertTrue(property.creationDefaultFingerprint().isEmpty());
        }
        var altered = new WidgetDefinition(TYPE, definition.dartClassName(), Optional.empty(), true,
                definition.dartLibraryUri(), definition.importUris(), definition.traits(), definition.palette(),
                List.of(definition.properties().getFirst()), definition.slots());
        assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(altered).isEmpty());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(altered).isEmpty());
    }

    @Test
    void allOmissionAndBooleanCombinationsGenerateAndRoundTripWithoutLosingFalseOrChild() throws Exception {
        var codec = new FdDocumentCodec();
        for (var ignoring : STATES) {
            for (var semantics : STATES) {
                for (boolean child : List.of(false, true)) {
                    var root = node(values(ignoring, semantics), child);
                    var document = document(root);
                    assertTrue(new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()).valid());
                    var first = generate(document);
                    assertEquals(first, generate(document));
                    String build = first.build().payload();
                    assertTrue(build.contains("return const IgnorePointer("), build);
                    assertEquals("import 'package:flutter/widgets.dart';\n", first.imports().payload());
                    assertEquals(ignoring.isPresent(), build.contains("ignoring:"), build);
                    assertEquals(semantics.isPresent(), build.contains("ignoringSemantics:"), build);
                    ignoring.ifPresent(value -> assertTrue(build.contains("ignoring: " + value), build));
                    semantics.ifPresent(value -> assertTrue(build.contains("ignoringSemantics: " + value), build));
                    assertTrue(build.contains(child ? "child: const Text('Retained child')" : "child: null"), build);
                    if (ignoring.isPresent() && semantics.isPresent()) {
                        assertTrue(build.indexOf("ignoring:") < build.indexOf("ignoringSemantics:"), build);
                    }
                    assertFalse(build.contains("key:"), build);
                    var symbol = first.symbolOccurrences().stream().filter(s -> s.symbolName().equals("IgnorePointer")).findFirst().orElseThrow();
                    assertEquals("package:flutter/widgets.dart", symbol.libraryUri());
                    assertEquals("IgnorePointer", build.substring(symbol.offset(), symbol.endOffset()));
                    var encoded = codec.encode(document);
                    var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded));
                    assertFalse(decoded.migrated());
                    assertEquals(document, decoded.document());
                    assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes());
                    assertTrue(new String(encoded.copyBytes(), StandardCharsets.UTF_8).contains("\"schemaVersion\": 17"));
                }
            }
        }
    }

    @Test
    void nonConstDescendantPropagatesThroughIgnorePointerWhilePreservingExplicitFalse() {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(
                new PropertyName("data"), new PropertyValue.StringValue("Retained child"),
                new PropertyName("styleColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))), Map.of());
        var root = new WidgetNode(StableId.random(), TYPE, values(Optional.of(false), Optional.of(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(text)));
        var generated = generate(document(root));
        assertTrue(generated.build().payload().contains("return IgnorePointer("));
        assertFalse(generated.build().payload().contains("const IgnorePointer("));
        assertTrue(generated.build().payload().contains("ignoringSemantics: false"));
        assertTrue(generated.build().payload().contains("Theme.of(context).colorScheme.primary"));
    }

    @Test
    void malformedBooleanValuesUnknownFieldsAndListChildrenFailClosed() {
        var validator = new WidgetTreeValidator();
        for (String key : List.of("ignoring", "ignoringSemantics")) {
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                    new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.EnumValue("bool", "false"),
                    new PropertyValue.DartExpressionValue("false"))) {
                var result = validator.validate(document(node(Map.of(new PropertyName(key), value), false)), BuiltInWidgetCatalog.getDefault());
                assertFalse(result.valid());
                assertTrue(result.errors().stream().anyMatch(i -> i.path().equals("/root/properties/" + key)));
            }
        }
        for (String key : List.of("key", "absorbing", "hitTestBehavior")) {
            assertFalse(validator.validate(document(node(Map.of(new PropertyName(key), new PropertyValue.BooleanValue(true)), false)),
                    BuiltInWidgetCatalog.getDefault()).valid());
        }
        var invalid = new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(CHILD, new WidgetSlot.ListSlot(List.of())));
        assertFalse(validator.validate(document(invalid), BuiltInWidgetCatalog.getDefault()).valid());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    private static Map<PropertyName, PropertyValue> values(Optional<Boolean> ignoring, Optional<Boolean> semantics) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        ignoring.ifPresent(value -> values.put(new PropertyName("ignoring"), new PropertyValue.BooleanValue(value)));
        semantics.ifPresent(value -> values.put(new PropertyName("ignoringSemantics"), new PropertyValue.BooleanValue(value)));
        return values;
    }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values, boolean child) {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Retained child")), Map.of());
        return new WidgetNode(StableId.random(), TYPE, values,
                Map.of(CHILD, child ? WidgetSlot.SingleSlot.of(text) : WidgetSlot.SingleSlot.empty()));
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
    private static GeneratedDartRegions generate(DesignerDocument document) {
        var result = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
}
