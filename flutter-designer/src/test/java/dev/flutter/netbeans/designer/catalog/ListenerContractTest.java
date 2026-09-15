package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ListenerContractTest {
    private static final WidgetTypeId TYPE = ListenerWidgetPropertySchema.LISTENER_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final List<String> CALLBACKS = List.of("onPointerDown", "onPointerMove", "onPointerUp",
            "onPointerHover", "onPointerCancel", "onPointerPanZoomStart", "onPointerPanZoomUpdate",
            "onPointerPanZoomEnd", "onPointerSignal");
    private static final List<String> EVENT_TYPES = List.of("PointerDownEvent", "PointerMoveEvent", "PointerUpEvent",
            "PointerHoverEvent", "PointerCancelEvent", "PointerPanZoomStartEvent", "PointerPanZoomUpdateEvent",
            "PointerPanZoomEndEvent", "PointerSignalEvent");

    @Test
    void exactConstConstructorContractHasTenOptionalPropertiesAndOneOptionalChild() {
        var definition = definition();
        assertEquals("Listener", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.interaction", 500, 20, "Listener"), definition.palette());
        var expected = new ArrayList<>(CALLBACKS);
        expected.add("behavior");
        assertEquals(expected, definition.properties().stream().map(property -> property.name().value()).toList());
        assertEquals(10, ListenerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(9, ListenerWidgetPropertySchema.CALLBACK_PROPERTY_COUNT);
        assertEquals(1, ListenerWidgetPropertySchema.SLOT_COUNT);
        assertEquals(CALLBACKS, List.copyOf(ListenerWidgetPropertySchema.callbackDefinitions().keySet()));
        int order = 1;
        for (var property : definition.properties()) {
            assertEquals(DartParameter.named(order++, false), property.parameter());
            assertTrue(property.creationDefault().isEmpty());
            var metadata = ListenerWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(property.name().value(), metadata.dartName());
            assertFalse(metadata.description().isBlank());
            assertFalse(metadata.group().description().isBlank());
        }
        assertTrue(ListenerWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
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
        assertTrue(valid(prototype));
    }

    @Test
    void allNineEventsHaveExactTypedNullableBindingsAndNoSyntheticDefaults() {
        var events = WidgetEventCatalog.eventsFor(definition());
        assertEquals(CALLBACKS, events.stream().map(event -> event.propertyName().value()).toList());
        assertEquals("onPointerDown", WidgetEventCatalog.defaultEventFor(definition()).orElseThrow().propertyName().value());
        for (int index = 0; index < events.size(); index++) {
            var event = events.get(index);
            var property = definition().property(event.propertyName()).orElseThrow();
            assertEquals(Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE,
                    PropertyValueKind.NULL), property.acceptedKinds());
            assertEquals(WidgetEventDescriptor.Kind.EVENT, event.kind());
            assertEquals(EVENT_TYPES.get(index) + "Listener", event.callbackType());
            assertEquals("void Function(" + EVENT_TYPES.get(index) + ")", event.signature().dartFunctionType());
            assertEquals(List.of(new WidgetEventDescriptor.Parameter(EVENT_TYPES.get(index), "event")), event.signature().parameters());
            assertEquals(List.of("package:flutter/gestures.dart"), event.signature().importUris());
            assertFalse(event.required());
            assertFalse(event.sdkRequired());
            assertTrue(event.nullableCallback());
            assertTrue(event.allowsExplicitNull());
            assertTrue(event.creationDefault().isEmpty());
            assertTrue(valid(node(Map.of(event.propertyName(), new PropertyValue.NullValue()))));
            assertTrue(valid(node(Map.of(event.propertyName(), event.bindingValue("_handler", property)))));
            assertTrue(valid(node(Map.of(event.propertyName(), new PropertyValue.CallbackValue("noop")))));
            assertTrue(valid(node(Map.of(event.propertyName(), reference()))));
            assertFalse(valid(node(Map.of(event.propertyName(), new PropertyValue.StringValue("noop")))));
            assertFalse(valid(node(Map.of(event.propertyName(), new PropertyValue.DartExpressionValue("(event) {}")))));
        }
    }

    @Test
    void behaviorAllowsOnlyItsThreeNonNullablePresetsAndEveryCallbackCanCoexist() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        CALLBACKS.forEach(name -> values.put(new PropertyName(name), new PropertyValue.CallbackValue("_handler")));
        assertTrue(valid(node(values)), "Raw pointer callbacks have no GestureDetector recognizer conflicts");
        for (String behavior : List.of("deferToChild", "opaque", "translucent")) {
            values.put(new PropertyName("behavior"), new PropertyValue.EnumValue("HitTestBehavior", behavior));
            assertTrue(valid(node(values)), behavior);
        }
        for (var invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(true),
                new PropertyValue.StringValue("opaque"), new PropertyValue.EnumValue("HitTestBehavior", "unknown"),
                new PropertyValue.EnumValue("Clip", "none"), new PropertyValue.DartExpressionValue("behavior"))) {
            assertFalse(valid(node(Map.of(new PropertyName("behavior"), invalid))), invalid.toString());
        }
        assertFalse(valid(node(Map.of(new PropertyName("onTap"), new PropertyValue.CallbackValue("_tap")))));
    }

    @Test
    void schema16RoundTripsEveryCallbackBindingWithoutCollapsingOmittedNullOrReferences() throws Exception {
        var codec = new FdDocumentCodec();
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        for (String name : CALLBACKS) {
            for (var value : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_handler"),
                    new PropertyValue.CallbackValue("noop"), reference())) {
                var original = document(node(Map.of(new PropertyName(name), value)));
                var encoded = codec.encode(original);
                var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)).document();
                assertEquals(original, decoded);
                assertEquals(value, decoded.root().properties().get(new PropertyName(name)));
                assertArrayEquals(encoded.copyBytes(), codec.encode(decoded).copyBytes());
            }
        }
        var omitted = document(node(Map.of()));
        assertEquals(Map.of(), assertInstanceOf(FdDecodeResult.Current.class,
                codec.decode(codec.encode(omitted))).document().root().properties());
    }

    @Test
    void constructorAndAllNineTypedefsMatchPinnedSdkIncludingServiceOwnedHover() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        assumeTrue(sdk != null && !sdk.isBlank(), "Set flutter.events.sdk to verify the pinned SDK source");
        Path sources = Path.of(sdk, "packages", "flutter", "lib", "src");
        String source = Files.readString(sources.resolve("widgets/basic.dart"));
        int begin = source.indexOf("  const Listener({");
        assertTrue(begin >= 0);
        String constructor = source.substring(begin, source.indexOf("  });", begin));
        var matcher = Pattern.compile("(?:this|super)\\.(\\w+)").matcher(constructor);
        List<String> arguments = new ArrayList<>();
        while (matcher.find()) arguments.add(matcher.group(1));
        var expected = new ArrayList<String>();
        expected.add("key"); expected.addAll(CALLBACKS); expected.add("behavior"); expected.add("child");
        assertEquals(expected, arguments);
        assertTrue(constructor.contains("this.behavior = HitTestBehavior.deferToChild"));
        String typedefs = Files.readString(sources.resolve("rendering/proxy_box.dart"))
                + Files.readString(sources.resolve("services/mouse_tracking.dart"));
        for (var event : WidgetEventCatalog.eventsFor(definition())) {
            assertTrue(source.contains("final " + event.callbackType() + "? " + event.propertyName().value() + ";"), event.toString());
            assertTrue(typedefs.contains("typedef " + event.callbackType() + " = void Function("
                    + event.signature().parameters().getFirst().type() + " event);"), event.toString());
        }
    }

    private static PropertyValue.DartObjectReferenceValue reference() {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/handlers.dart"),
                "handler", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values) {
        return new WidgetNode(StableId.random(), TYPE, values, Map.of(CHILD, WidgetSlot.SingleSlot.empty()));
    }
    private static boolean valid(WidgetNode node) {
        return new WidgetTreeValidator().validate(document(node), BuiltInWidgetCatalog.getDefault()).valid();
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
}
