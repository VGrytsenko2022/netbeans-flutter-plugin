package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.events.WidgetEventDescriptor;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.state.WidgetStateBindingCatalog;
import io.github.vgrytsenko2022.designer.state.WidgetStatePropertyBindingCatalog;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NotificationListenerContractTest {
    private static final WidgetTypeId TYPE = NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    @Test void fullConstConstructorHasExplicitGenericTypeOneNullableCallbackAndRequiredChild() {
        var definition = definition();
        assertTrue(definition.constConstructor());
        assertEquals("NotificationListener", definition.dartClassName());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.interaction", 500, 50, "NotificationListener"), definition.palette());
        assertEquals(List.of("notificationType", "onNotification"), definition.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(2, NotificationListenerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(DartParameter.named(1, true), definition.properties().getFirst().parameter());
        assertEquals(DartParameter.named(2, false), definition.properties().getLast().parameter());
        assertEquals(Set.of(PropertyValueKind.STRING, PropertyValueKind.DART_OBJECT_REFERENCE), definition.properties().getFirst().acceptedKinds());
        var child = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(0, true), child.parameter());
        assertEquals(1, child.minChildren());
        assertEquals(1, child.maxChildren());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(new PropertyName("notificationType"), new PropertyValue.StringValue("Notification")), prototype.properties());
        assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertTrue(valid(node(Map.of())));
        assertTrue(WidgetStateBindingCatalog.find(prototype).isEmpty());
        assertTrue(WidgetStatePropertyBindingCatalog.descriptors(prototype).isEmpty());
    }

    @Test void allFourteenPresetsAndCustomSimpleTypeReferencesAreAdmittedButOtherTypeFormsFailClosed() {
        assertEquals(List.of("Notification", "LayoutChangedNotification", "ScrollNotification", "ScrollStartNotification",
                "ScrollUpdateNotification", "OverscrollNotification", "ScrollEndNotification", "UserScrollNotification",
                "SizeChangedLayoutNotification", "ScrollMetricsNotification", "OverscrollIndicatorNotification",
                "DraggableScrollableNotification", "KeepAliveNotification", "NavigationNotification"), NotificationListenerWidgetPropertySchema.typePresets());
        for (String preset : NotificationListenerWidgetPropertySchema.typePresets()) assertTrue(valid(node(Map.of(p("notificationType"), new PropertyValue.StringValue(preset)))));
        assertTrue(valid(node(Map.of(p("notificationType"), reference("CustomNotice", Optional.empty(), false)))));
        assertTrue(valid(node(Map.of(p("notificationType"), reference("CustomAlias", Optional.of("package:app/notices.dart"), false)))));
        for (String invalid : List.of("String", "ScrollNotification?", "List<Notification>", "dynamic", "UnknownPreset")) {
            assertFalse(valid(node(Map.of(p("notificationType"), new PropertyValue.StringValue(invalid)))), invalid);
        }
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.DartExpressionValue("CustomNotice"),
                reference("createType", Optional.empty(), true),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Owner", Optional.of("Nested"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()))) {
            assertFalse(valid(node(Map.of(p("notificationType"), invalid))), invalid.toString());
        }
        var root = node(Map.of());
        assertFalse(valid(new WidgetNode(root.id(), TYPE, Map.of(), root.slots())), "The selected generic type is required, not silently inferred");
    }

    @Test void notificationIsANullableBoolEventWithSharedContravariantSignatureAndNoStateProducer() {
        var event = WidgetEventCatalog.defaultEventFor(definition()).orElseThrow();
        assertEquals(List.of(event), WidgetEventCatalog.eventsFor(definition()));
        assertEquals(p("onNotification"), event.propertyName());
        assertEquals(WidgetEventDescriptor.Kind.EVENT, event.kind());
        assertEquals("NotificationListenerCallback<Notification>", event.callbackType());
        assertEquals("bool Function(Notification)", event.signature().dartFunctionType());
        assertEquals("bool _notice(Notification notification)", event.signature().declaration("_notice"));
        assertEquals(List.of("package:flutter/widgets.dart"), event.signature().importUris());
        assertTrue(event.nullableCallback());
        assertTrue(event.allowsExplicitNull());
        assertTrue(event.creationDefault().isEmpty());
        assertFalse(event.required());
        assertFalse(event.sdkRequired());
        assertTrue(event.unsetBehavior().contains("true to stop"));
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_notice"), reference("_notice", Optional.empty(), false))) {
            assertTrue(valid(node(Map.of(p("onNotification"), value))));
        }
        assertFalse(valid(node(Map.of(p("onNotification"), new PropertyValue.BooleanValue(false)))));
        assertFalse(valid(node(Map.of(p("onNotification"), new PropertyValue.DartExpressionValue("(_) => true")))));
    }

    @Test void schema16PreservesAllTypeAndCallbackVariantsWithoutCollapsingOmissionOrNull() throws Exception {
        var types = new ArrayList<PropertyValue>();
        NotificationListenerWidgetPropertySchema.typePresets().forEach(name -> types.add(new PropertyValue.StringValue(name)));
        types.add(reference("CustomNotice", Optional.of("package:app/notices.dart"), false));
        var codec = new FdDocumentCodec();
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        for (PropertyValue type : types) {
            var props = new LinkedHashMap<PropertyName, PropertyValue>();
            props.put(p("notificationType"), type);
            for (Optional<PropertyValue> callback : List.<Optional<PropertyValue>>of(Optional.empty(), Optional.of(new PropertyValue.NullValue()),
                    Optional.of(new PropertyValue.CallbackValue("_notice")), Optional.of(reference("handler", Optional.of("package:app/handlers.dart"), false)))) {
                callback.ifPresentOrElse(value -> props.put(p("onNotification"), value), () -> props.remove(p("onNotification")));
                var original = document(node(props));
                var encoded = codec.encode(original);
                var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)).document();
                assertEquals(original, restored);
                assertArrayEquals(encoded.copyBytes(), codec.encode(restored).copyBytes());
            }
        }
    }

    @Test void exactConstructorTypedefAndTypePresetsRemainExportedByPinnedWidgetsLibrary() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        assumeTrue(sdk != null && !sdk.isBlank(), "Set flutter.events.sdk for pinned SDK verification");
        Path library = Path.of(sdk, "packages/flutter/lib");
        String source = Files.readString(library.resolve("src/widgets/notification_listener.dart"));
        assertTrue(source.contains("class NotificationListener<T extends Notification> extends ProxyWidget"));
        assertTrue(source.contains("const NotificationListener({super.key, required super.child, this.onNotification});"));
        assertTrue(source.contains("typedef NotificationListenerCallback<T extends Notification> = bool Function(T notification);"));
        assertTrue(source.contains("final NotificationListenerCallback<T>? onNotification;"));
        assertTrue(source.contains("listener.onNotification != null && notification is T"));
        String exports = Files.readString(library.resolve("widgets.dart"));
        StringBuilder exportedSource = new StringBuilder();
        var matcher = java.util.regex.Pattern.compile("export '([^']+)'").matcher(exports);
        while (matcher.find()) {
            if (!matcher.group(1).startsWith("src/")) continue;
            Path exported = library.resolve(matcher.group(1));
            if (Files.isRegularFile(exported)) exportedSource.append(Files.readString(exported));
        }
        for (String name : NotificationListenerWidgetPropertySchema.typePresets()) {
            assertTrue(java.util.regex.Pattern.compile("\\bclass\\s+" + name + "\\b").matcher(exportedSource).find(), name);
        }
    }

    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DartObjectReferenceValue reference(String name, Optional<String> library, boolean factory) {
        return new PropertyValue.DartObjectReferenceValue(library, name, Optional.empty(), factory
                ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory ? Optional.of(false) : Optional.empty());
    }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> overrides) {
        var props = new LinkedHashMap<PropertyName, PropertyValue>();
        props.put(p("notificationType"), new PropertyValue.StringValue("Notification")); props.putAll(overrides);
        var text = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        return new WidgetNode(StableId.random(), TYPE, props, Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(text))));
    }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
}
