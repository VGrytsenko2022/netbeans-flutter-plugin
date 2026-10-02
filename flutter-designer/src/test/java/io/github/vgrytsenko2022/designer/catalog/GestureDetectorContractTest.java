package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.events.WidgetEventDescriptor;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GestureDetectorContractTest {
    private static final WidgetTypeId TYPE = GestureDetectorWidgetPropertySchema.GESTURE_DETECTOR_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final List<String> CALLBACKS = List.of(
            "onTapDown",
            "onTapUp",
            "onTap",
            "onTapMove",
            "onTapCancel",
            "onSecondaryTap",
            "onSecondaryTapDown",
            "onSecondaryTapUp",
            "onSecondaryTapCancel",
            "onTertiaryTapDown",
            "onTertiaryTapUp",
            "onTertiaryTapCancel",
            "onDoubleTapDown",
            "onDoubleTap",
            "onDoubleTapCancel",
            "onLongPressDown",
            "onLongPressCancel",
            "onLongPress",
            "onLongPressStart",
            "onLongPressMoveUpdate",
            "onLongPressUp",
            "onLongPressEnd",
            "onSecondaryLongPressDown",
            "onSecondaryLongPressCancel",
            "onSecondaryLongPress",
            "onSecondaryLongPressStart",
            "onSecondaryLongPressMoveUpdate",
            "onSecondaryLongPressUp",
            "onSecondaryLongPressEnd",
            "onTertiaryLongPressDown",
            "onTertiaryLongPressCancel",
            "onTertiaryLongPress",
            "onTertiaryLongPressStart",
            "onTertiaryLongPressMoveUpdate",
            "onTertiaryLongPressUp",
            "onTertiaryLongPressEnd",
            "onVerticalDragDown",
            "onVerticalDragStart",
            "onVerticalDragUpdate",
            "onVerticalDragEnd",
            "onVerticalDragCancel",
            "onHorizontalDragDown",
            "onHorizontalDragStart",
            "onHorizontalDragUpdate",
            "onHorizontalDragEnd",
            "onHorizontalDragCancel",
            "onForcePressStart",
            "onForcePressPeak",
            "onForcePressUpdate",
            "onForcePressEnd",
            "onPanDown",
            "onPanStart",
            "onPanUpdate",
            "onPanEnd",
            "onPanCancel",
            "onScaleStart",
            "onScaleUpdate",
            "onScaleEnd");
    private static final List<String> CONFIGURATION = List.of("behavior", "excludeFromSemantics",
            "dragStartBehavior", "trackpadScrollCausesScale", "trackpadScrollToScaleFactor", "supportedDevices");

    @Test
    void completeNonConstSdkContractHasSixtyFourOptionalPropertiesAndOneOptionalChild() {
        var definition = definition();
        assertEquals("GestureDetector", definition.dartClassName());
        assertFalse(definition.constConstructor());
        assertEquals(Optional.empty(), definition.namedConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.interaction", 500, 10, "GestureDetector"), definition.palette());
        var expected = new ArrayList<>(CALLBACKS);
        expected.addAll(CONFIGURATION);
        assertEquals(expected, definition.properties().stream().map(property -> property.name().value()).toList());
        assertEquals(64, GestureDetectorWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(58, GestureDetectorWidgetPropertySchema.CALLBACK_PROPERTY_COUNT);
        assertEquals(1, GestureDetectorWidgetPropertySchema.SLOT_COUNT);
        int order = 1;
        for (var property : definition.properties()) {
            assertEquals(DartParameter.named(order++, false), property.parameter());
            assertTrue(property.creationDefault().isEmpty());
            var metadata = GestureDetectorWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(property.name().value(), metadata.dartName());
            assertFalse(metadata.description().isBlank());
            assertFalse(metadata.group().description().isBlank());
        }
        assertTrue(GestureDetectorWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
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
    void allFiftyEightEventsHaveExactTypedNullableBindingsAndNoSyntheticCreationHandlers() {
        var definition = definition();
        var events = WidgetEventCatalog.eventsFor(definition);
        assertEquals(CALLBACKS, events.stream().map(event -> event.propertyName().value()).toList());
        assertEquals("onTap", WidgetEventCatalog.defaultEventFor(definition).orElseThrow().propertyName().value());
        for (var event : events) {
            var property = definition.property(event.propertyName()).orElseThrow();
            assertEquals(Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE,
                    PropertyValueKind.NULL), property.acceptedKinds());
            assertEquals(WidgetEventDescriptor.Kind.EVENT, event.kind());
            assertFalse(event.required());
            assertFalse(event.sdkRequired());
            assertTrue(event.nullableCallback());
            assertTrue(event.allowsExplicitNull());
            assertTrue(event.creationDefault().isEmpty());
            assertEquals(List.of("package:flutter/gestures.dart"), event.signature().importUris());
            var expected = GestureDetectorWidgetPropertySchema.find(event.propertyName()).orElseThrow();
            assertEquals(expected.callbackType().orElseThrow(), event.callbackType());
            assertEquals("void Function(" + expected.parameterType().orElse("") + ")",
                    event.signature().dartFunctionType());
            assertTrue(valid(node(Map.of(event.propertyName(), new PropertyValue.NullValue()))));
            assertTrue(valid(node(Map.of(event.propertyName(), event.bindingValue("_handler", property)))));
            var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/handlers.dart"),
                    "handler", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            assertTrue(valid(node(Map.of(event.propertyName(), reference))));
            assertFalse(valid(node(Map.of(event.propertyName(), new PropertyValue.StringValue("noop")))));
            assertFalse(valid(node(Map.of(event.propertyName(), new PropertyValue.DartExpressionValue("() {}")))));
        }
    }

    @Test
    void configurationRetainsOmittedNullAndEmptyWhileRejectingWrongValueKinds() throws Exception {
        var codec = new FdDocumentCodec();
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(new PropertyName("behavior"), new PropertyValue.EnumValue("HitTestBehavior", "translucent"));
        values.put(new PropertyName("excludeFromSemantics"), new PropertyValue.BooleanValue(true));
        values.put(new PropertyName("dragStartBehavior"), new PropertyValue.EnumValue("DragStartBehavior", "down"));
        values.put(new PropertyName("trackpadScrollCausesScale"), new PropertyValue.BooleanValue(true));
        values.put(new PropertyName("trackpadScrollToScaleFactor"),
                new PropertyValue.OffsetValue(new BigDecimal("-0.25"), new BigDecimal("0.75")));
        values.put(new PropertyName("supportedDevices"),
                new PropertyValue.PointerDeviceKindSetValue(List.of(PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind.MOUSE)));
        assertTrue(valid(node(values)));
        var original = document(node(values));
        var encoded = codec.encode(original);
        assertEquals(original, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)).document());
        for (String name : CONFIGURATION) {
            assertFalse(valid(node(Map.of(new PropertyName(name), new PropertyValue.DartExpressionValue("anything()")))), name);
            assertFalse(valid(node(Map.of(new PropertyName(name), new PropertyValue.StringValue("anything")))), name);
        }
        for (String nullable : List.of("behavior", "supportedDevices")) {
            assertTrue(valid(node(Map.of(new PropertyName(nullable), new PropertyValue.NullValue()))), nullable);
        }
        for (String nonNullable : List.of("excludeFromSemantics", "dragStartBehavior",
                "trackpadScrollCausesScale", "trackpadScrollToScaleFactor")) {
            assertFalse(valid(node(Map.of(new PropertyName(nonNullable), new PropertyValue.NullValue()))), nonNullable);
        }
        assertTrue(valid(node(Map.of(new PropertyName("supportedDevices"), new PropertyValue.PointerDeviceKindSetValue(List.of())))));
        assertFalse(valid(node(Map.of(new PropertyName("behavior"), new PropertyValue.EnumValue("Clip", "none")))));
        assertFalse(valid(node(Map.of(new PropertyName("dragStartBehavior"), new PropertyValue.EnumValue("DragStartBehavior", "unknown")))));
        assertFalse(valid(node(Map.of(new PropertyName("supportedDevices"), new PropertyValue.EnumValue("PointerDeviceKind", "mouse")))));
        assertFalse(valid(node(Map.of(new PropertyName("trackpadScrollToScaleFactor"), new PropertyValue.IntegerValue(BigInteger.ONE)))));
    }

    @Test
    void recognizerConflictsMirrorActualSdkAssertionRatherThanRejectingHorizontalPlusVerticalAlone() {
        for (int mask = 0; mask < 16; mask++) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            String[] names = {"onPanStart", "onScaleUpdate", "onVerticalDragEnd", "onHorizontalDragStart"};
            for (int bit = 0; bit < names.length; bit++) {
                if ((mask & (1 << bit)) != 0) values.put(new PropertyName(names[bit]), new PropertyValue.CallbackValue("_event"));
            }
            boolean pan = (mask & 1) != 0;
            boolean scale = (mask & 2) != 0;
            boolean vertical = (mask & 4) != 0;
            boolean horizontal = (mask & 8) != 0;
            boolean conflict = pan && scale || vertical && horizontal && (pan || scale);
            assertEquals(!conflict, valid(node(values)), "recognizer mask " + mask);
            assertEquals(conflict, GestureDetectorWidgetPropertySchema.gestureConflict(node(values)).isPresent());
        }
        for (String suffix : List.of("Start", "Update", "End")) {
            assertFalse(valid(node(Map.of(new PropertyName("onPan" + suffix), new PropertyValue.CallbackValue("_pan"),
                    new PropertyName("onScale" + suffix), new PropertyValue.CallbackValue("_scale")))));
        }
        var downOnly = Map.<PropertyName, PropertyValue>of(
                new PropertyName("onPanDown"), new PropertyValue.CallbackValue("_pan"),
                new PropertyName("onPanCancel"), new PropertyValue.CallbackValue("_cancel"),
                new PropertyName("onVerticalDragDown"), new PropertyValue.CallbackValue("_vertical"),
                new PropertyName("onHorizontalDragDown"), new PropertyValue.CallbackValue("_horizontal"),
                new PropertyName("onScaleStart"), new PropertyValue.CallbackValue("_scale"),
                new PropertyName("onPanStart"), new PropertyValue.NullValue());
        assertTrue(valid(node(downOnly)), "Down/cancel-only callbacks do not participate in the constructor assertion");
    }

    @Test
    void inventoryMatchesPinnedSdkConstructorFieldsAndEveryConcreteTypedefSignature() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        assumeTrue(sdk != null && !sdk.isBlank(), "Set flutter.events.sdk to verify the pinned SDK source");
        Path sources = Path.of(sdk, "packages", "flutter", "lib", "src");
        String source = Files.readString(sources.resolve("widgets/gesture_detector.dart"));
        int begin = source.indexOf("  GestureDetector({");
        String constructor = source.substring(begin, source.indexOf("  }) : assert", begin));
        var matcher = Pattern.compile("this\\.(\\w+)").matcher(constructor);
        List<String> sdkArguments = new ArrayList<>();
        while (matcher.find()) sdkArguments.add(matcher.group(1));
        var expected = new ArrayList<String>();
        expected.add("child"); expected.addAll(CALLBACKS); expected.addAll(CONFIGURATION);
        assertEquals(expected, sdkArguments);
        StringBuilder gestures = new StringBuilder();
        try (var files = Files.list(sources.resolve("gestures"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".dart")).toList()) {
                gestures.append(Files.readString(file)).append('\n');
            }
        }
        for (var event : WidgetEventCatalog.eventsFor(definition())) {
            assertTrue(source.contains("final " + event.callbackType() + "? " + event.propertyName().value() + ";"), event.toString());
            String args = event.signature().parameters().isEmpty() ? ""
                    : event.signature().parameters().getFirst().type() + " details";
            assertTrue(gestures.toString().contains("typedef " + event.callbackType() + " = void Function(" + args + ");"), event.toString());
        }
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

