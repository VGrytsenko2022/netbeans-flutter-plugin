package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.generation.GeneratedDartStaticTypeRequirement;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FocusContractTest {
    private static final WidgetTypeId TYPE = FocusWidgetPropertySchema.FOCUS_TYPE;
    private static final SlotName CHILD = new SlotName("child");

    @Test void completeConstructorHasTwelveSdkFieldsSelectorAndRequiredChild() {
        var definition = definition();
        assertEquals("Focus", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(new PaletteMetadata("flutter.interaction", 500, 40, "Focus"), definition.palette());
        assertEquals(List.of("focusNode", "parentNode", "autofocus", "onFocusChange", "onKeyEvent", "onKey",
                "canRequestFocus", "skipTraversal", "descendantsAreFocusable", "descendantsAreTraversable",
                "includeSemantics", "debugLabel", "variant"), definition.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(12, FocusWidgetPropertySchema.SDK_PROPERTY_COUNT);
        assertEquals(13, FocusWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        int order = 1;
        for (var property : definition.properties()) {
            assertEquals(DartParameter.named(order++, false), property.parameter());
            assertFalse(FocusWidgetPropertySchema.find(property.name()).orElseThrow().description().isBlank());
            assertEquals(property.name().value().equals("variant"), property.creationDefault().isPresent());
        }
        var slot = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(0, true), slot.parameter());
        assertEquals(1, slot.minChildren());
        assertEquals(1, slot.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, slot.acceptance());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("standard")), prototype.properties());
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertFalse(valid(prototype));
        assertTrue(valid(node(Map.of())));
    }

    @Test void standardNullabilityExternalRequiredNodeAndInactivePreservationAreExact() {
        for (var property : definition().properties()) {
            boolean nullable = !Set.of("autofocus", "includeSemantics", "variant").contains(property.name().value());
            assertEquals(nullable, valid(node(Map.of(property.name(), new PropertyValue.NullValue()))), property.name().value());
        }
        var external = new LinkedHashMap<PropertyName, PropertyValue>();
        external.put(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode"));
        assertFalse(valid(node(external)));
        external.put(new PropertyName("focusNode"), new PropertyValue.NullValue());
        assertFalse(valid(node(external)));
        external.put(new PropertyName("focusNode"), reference("_node"));
        assertTrue(valid(node(external)));
        external.put(new PropertyName("parentNode"), new PropertyValue.NullValue());
        external.put(new PropertyName("onKeyEvent"), reference("_key"));
        external.put(new PropertyName("debugLabel"), new PropertyValue.StringValue("Retained"));
        external.put(new PropertyName("skipTraversal"), new PropertyValue.BooleanValue(true));
        assertTrue(valid(node(external)), "Inactive valid fields remain stored, not deleted or forbidden");
        assertEquals(7, FocusWidgetPropertySchema.standardOnlyProperties().size());
        for (String name : FocusWidgetPropertySchema.standardOnlyProperties()) {
            assertFalse(FocusWidgetPropertySchema.propertyAvailable(node(external), new PropertyName(name)));
        }
        for (String name : List.of("focusNode", "parentNode", "autofocus", "onFocusChange", "includeSemantics")) {
            assertTrue(FocusWidgetPropertySchema.propertyAvailable(node(external), new PropertyName(name)));
        }
        assertFalse(valid(node(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("invented")))));
        assertFalse(valid(node(Map.of(new PropertyName("focusNode"), new PropertyValue.DartExpressionValue("FocusNode()")))));
    }

    @Test void eventsHaveExactReturnTypesParametersImportsAndConstructorAvailability() {
        var events = WidgetEventCatalog.eventsFor(definition());
        assertEquals(List.of("onFocusChange", "onKeyEvent", "onKey"), events.stream().map(e -> e.propertyName().value()).toList());
        assertEquals("onFocusChange", WidgetEventCatalog.defaultEventFor(definition()).orElseThrow().propertyName().value());
        assertEquals(List.of("ValueChanged<bool>", "FocusOnKeyEventCallback", "FocusOnKeyCallback"), events.stream().map(e -> e.callbackType()).toList());
        assertEquals(List.of("void Function(bool)", "KeyEventResult Function(FocusNode, KeyEvent)",
                "KeyEventResult Function(FocusNode, RawKeyEvent)"), events.stream().map(e -> e.signature().dartFunctionType()).toList());
        for (var event : events) {
            var property = definition().property(event.propertyName()).orElseThrow();
            assertEquals(Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL), property.acceptedKinds());
            assertTrue(event.nullableCallback());
            assertTrue(event.allowsExplicitNull());
            assertTrue(event.creationDefault().isEmpty());
            assertTrue(valid(node(Map.of(event.propertyName(), new PropertyValue.CallbackValue("_handler")))));
            assertTrue(valid(node(Map.of(event.propertyName(), reference("_handler")))));
            if (!event.propertyName().value().equals("onFocusChange")) {
                assertEquals(List.of("package:flutter/widgets.dart", "package:flutter/services.dart"), event.signature().importUris());
                assertEquals(List.of("standard"), event.availableVariants());
                assertTrue(event.createStub("_handler").contains("throw UnimplementedError"));
            }
        }
    }

    @Test void schema16RoundTripsBothConstructorsEveryValueAndInactiveFieldsWithoutChangingRepresentation() throws Exception {
        var codec = new FdDocumentCodec();
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        var fields = new LinkedHashMap<PropertyName, PropertyValue>();
        fields.put(new PropertyName("focusNode"), reference("_node"));
        fields.put(new PropertyName("parentNode"), new PropertyValue.NullValue());
        FocusWidgetPropertySchema.booleanProperties().forEach(name -> fields.put(new PropertyName(name), new PropertyValue.BooleanValue(true)));
        fields.put(new PropertyName("debugLabel"), new PropertyValue.StringValue("Saved label"));
        for (String variant : List.of("standard", "withExternalFocusNode")) {
            fields.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
            for (var callback : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_key"), reference("_key"))) {
                FocusWidgetPropertySchema.callbackDefinitions().keySet().forEach(name -> fields.put(new PropertyName(name), callback));
                var original = document(node(fields));
                var encoded = codec.encode(original);
                var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)).document();
                assertEquals(original, restored);
                assertArrayEquals(encoded.copyBytes(), codec.encode(restored).copyBytes());
            }
        }
    }

    @Test void nullableFocusNodeProofExtendsOnlyTheReviewedClosedType() {
        assertDoesNotThrow(() -> new PropertyValueConstraint.DartObjectReferenceValues("FocusNode?"));
        assertDoesNotThrow(() -> new GeneratedDartStaticTypeRequirement(0, 1, "FocusNode?"));
        for (String unsupported : List.of("MouseCursor??", "FocusNode??", "FocusNode | Object", "foo.FocusNode?")) {
            assertThrows(IllegalArgumentException.class, () -> new PropertyValueConstraint.DartObjectReferenceValues(unsupported));
            assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, unsupported));
        }
    }

    @Test void standardAndExternalConstructorArgumentsAndKeyboardTypedefsMatchPinnedSdk() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        assumeTrue(sdk != null && !sdk.isBlank(), "Set flutter.events.sdk for pinned SDK verification");
        Path widgets = Path.of(sdk, "packages/flutter/lib/src/widgets");
        String source = Files.readString(widgets.resolve("focus_scope.dart"));
        int begin = source.indexOf("  const Focus({");
        assertTrue(begin >= 0);
        String standard = source.substring(begin, source.indexOf("})", begin));
        for (String name : FocusWidgetPropertySchema.definitions().keySet()) {
            if (!name.equals("variant")) assertTrue(standard.matches("(?s).*\\b" + name + "\\b.*"), name);
        }
        assertTrue(standard.contains("required this.child"));
        assertTrue(standard.contains("this.autofocus = false"));
        assertTrue(standard.contains("this.includeSemantics = true"));
        begin = source.indexOf("  const factory Focus.withExternalFocusNode({");
        assertTrue(begin >= 0);
        String external = source.substring(begin, source.indexOf("})", begin));
        assertTrue(external.contains("required FocusNode focusNode"));
        assertTrue(external.contains("FocusNode? parentNode"));
        for (String name : FocusWidgetPropertySchema.standardOnlyProperties()) assertFalse(external.matches("(?s).*\\b" + name + "\\b.*"), name);
        String manager = Files.readString(widgets.resolve("focus_manager.dart"));
        assertTrue(manager.contains("typedef FocusOnKeyCallback = KeyEventResult Function(FocusNode node, RawKeyEvent event);"));
        assertTrue(manager.contains("typedef FocusOnKeyEventCallback = KeyEventResult Function(FocusNode node, KeyEvent event);"));
    }

    private static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values) {
        var child = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        return new WidgetNode(StableId.random(), TYPE, values, Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(child))));
    }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
}
