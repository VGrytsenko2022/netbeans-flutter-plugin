package dev.flutter.netbeans.designer.events;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WidgetEventCatalogTest {
    private static final Set<String> TYPED_CALLABLES = Set.of("EntryModeChangeCallback", "SelectableDayForRangePredicate", "SelectableDayPredicate", "ValueChanged<DatePickerEntryMode>", "ValueChanged<DateTime>", "GestureTapCallback", "GestureLongPressCallback", "GestureTapDownCallback", "GestureTapCancelCallback", "DataColumnSortCallback", "ValueSetter<bool?>", "VoidCallback?", "VoidCallback", "ValueChanged<bool>",
            "ValueChanged<bool?>", "ValueChanged<Object?>", "ValueChanged<double>", "ValueChanged<RangeValues>",
            "ValueChanged<RefreshIndicatorStatus?>", "ImageErrorListener", "RefreshCallback", "AsyncCallback",
            "ScrollNotificationPredicate", "SemanticFormatterCallback", "ButtonLayerBuilder", "MenuAnchorChildBuilder", "ValueChanged<AnimationStatus>", "ValueChanged<int>", "ValueChanged<int?>",
            "Widget? Function(BuildContext, Animation<double>)", "InputCounterWidgetBuilder?", "EditableTextContextMenuBuilder?", "ItemExtentBuilder?", "ItemExtentBuilder", "TooltipTriggeredCallback", "TooltipPositionDelegate", "TransformCallback", "ShaderCallback", "NullableIndexedWidgetBuilder", "IndexedWidgetBuilder", "ChildIndexGetter?", "SemanticIndexCallback", "SliverLayoutWidgetBuilder", "LayoutWidgetBuilder", "OrientationWidgetBuilder", "TransitionBuilder", "AnimatedCrossFadeBuilder", "AnimatedSwitcherTransitionBuilder", "AnimatedSwitcherLayoutBuilder", "ValueWidgetBuilder<Object>", "ImageErrorWidgetBuilder?");

    @Test
    void coversEveryCallablePropertyAcrossAllOneHundredDefinitionsWithoutTreatingObjectsAsEvents() {
        var definitions = BuiltInWidgetCatalog.getDefault().definitions();
        assertEquals(238, definitions.size());
        int callables = 0;
        for (WidgetDefinition widget : definitions) {
            for (PropertyDefinition property : widget.properties()) {
                // Builder.builder is a construction callback, not an
                // interaction/event surface; it is edited in Properties and
                // deliberately excluded from the Events tab.
                boolean callable = !widget.typeId().value().equals("flutter.widgets.Builder")
                        && property.acceptedKinds().contains(PropertyValueKind.CALLBACK)
                        || property.constraints().stream().anyMatch(value ->
                                value instanceof PropertyValueConstraint.DartObjectReferenceValues reference
                                        && TYPED_CALLABLES.contains(reference.expectedDartType()));
                var descriptor = WidgetEventCatalog.find(widget.typeId(), property.name());
                assertEquals(callable, descriptor.isPresent(), widget.dartClassName() + "." + property.name());
                if (callable) {
                    callables++;
                    assertTrue(WidgetEventCatalog.eventsFor(widget).contains(descriptor.orElseThrow()));
                }
            }
        }
        assertEquals(266, callables);
        assertEquals(99, definitions.stream().filter(widget -> !WidgetEventCatalog.eventsFor(widget).isEmpty()).count());
    }

    @Test
    void separatesEventsBuildersPredicatesAndFormatters() {
        var all = BuiltInWidgetCatalog.getDefault().definitions().stream()
                .flatMap(widget -> WidgetEventCatalog.eventsFor(widget).stream()).toList();
        assertEquals(Map.of(WidgetEventDescriptor.Kind.EVENT, 196L, WidgetEventDescriptor.Kind.BUILDER, 50L,
                WidgetEventDescriptor.Kind.PREDICATE, 7L, WidgetEventDescriptor.Kind.FORMATTER, 2L, WidgetEventDescriptor.Kind.DELEGATE, 11L),
                all.stream().collect(Collectors.groupingBy(WidgetEventDescriptor::kind, Collectors.counting())));
        assertEquals(70, BuiltInWidgetCatalog.getDefault().definitions().stream()
                .filter(widget -> WidgetEventCatalog.eventsFor(widget).stream()
                        .anyMatch(event -> event.kind() == WidgetEventDescriptor.Kind.EVENT)).count());
        assertFalse(find("TextField", "onTapAlwaysCalled").isPresent());
        assertFalse(find("IconButton", "focusNode").isPresent());
        assertFalse(find("Switch", "thumbColor").isPresent());
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, findWidgets("Image", "errorBuilder").kind());
    }

    @Test
    void coversInheritedButtonEventsWithoutInventingIconButtonFocusEvent() {
        for (String name : List.of("ElevatedButton", "TextButton", "OutlinedButton", "FilledButton")) {
            assertEquals(List.of("onPressed", "onLongPress", "onHover", "onFocusChange"),
                    WidgetEventCatalog.eventsFor(widget(name)).stream()
                            .filter(value -> value.kind() == WidgetEventDescriptor.Kind.EVENT)
                            .map(value -> value.propertyName().value()).toList());
        }
        assertTrue(find("IconButton", "onFocusChange").isEmpty());
    }

    @Test
    void preservesNullableValuesGenericRadioAndMapParameters() {
        assertEquals("void Function(bool?)", descriptor("Checkbox", "onChanged").signature().dartFunctionType());
        assertEquals("void Function(bool)", descriptor("Switch", "onChanged").signature().dartFunctionType());
        assertEquals("void Function(Object?)", descriptor("Radio", "onChanged").signature().dartFunctionType());
        assertEquals("void Function(Object?)", findWidgets("RadioGroup", "onChanged").signature().dartFunctionType());
        assertEquals("void Function(String, Map<String, dynamic>)",
                descriptor("TextField", "onAppPrivateCommand").signature().dartFunctionType());
        assertEquals("void Function(PointerDownEvent)", descriptor("TextField", "onTapOutside").signature().dartFunctionType());
        assertEquals("void Function(PointerUpEvent)", descriptor("TextField", "onTapUpOutside").signature().dartFunctionType());
    }

    @Test
    void builderSignaturesMatchSdkIncludingNullableChildFrameAndStackTrace() {
        assertEquals("Widget Function(BuildContext, Widget, int?, bool)",
                findWidgets("Image", "frameBuilder").signature().dartFunctionType());
        assertEquals("Widget Function(BuildContext, Widget, ImageChunkEvent?)",
                findWidgets("Image", "loadingBuilder").signature().dartFunctionType());
        assertEquals("Widget Function(BuildContext, Object, StackTrace?)",
                findWidgets("Image", "errorBuilder").signature().dartFunctionType());
        assertEquals("Widget Function(BuildContext, Set<WidgetState>, Widget?)",
                descriptor("TextButton", "styleBackgroundBuilder").signature().dartFunctionType());
        assertEquals("bool Function(ScrollNotification)", descriptor("RefreshIndicator", "notificationPredicate").signature().dartFunctionType());
        assertEquals("String Function(double)", descriptor("RangeSlider", "semanticFormatterCallback").signature().dartFunctionType());
    }

    @Test
    void requiredModelParameterSdkRequiredAndNullabilityAreDistinct() {
        var checkbox = descriptor("Checkbox", "onChanged");
        assertFalse(checkbox.required());
        assertTrue(checkbox.sdkRequired());
        assertTrue(checkbox.nullableCallback());
        assertFalse(checkbox.allowsExplicitNull());
        var tile = descriptor("CheckboxListTile", "onChanged");
        assertTrue(tile.required());
        assertTrue(tile.sdkRequired());
        assertTrue(tile.nullableCallback());
        assertTrue(tile.allowsExplicitNull());
        assertEquals(new PropertyValue.StringValue("noop"), tile.creationDefault().orElseThrow());
        var radioGroup = findWidgets("RadioGroup", "onChanged");
        assertTrue(radioGroup.required());
        assertFalse(radioGroup.nullableCallback());
        assertFalse(radioGroup.allowsExplicitNull());
        var refresh = descriptor("RefreshIndicator", "onRefresh");
        assertFalse(refresh.required());
        assertTrue(refresh.sdkRequired());
        assertFalse(refresh.nullableCallback());
        assertTrue(refresh.unsetBehavior().contains("async no-op"));
        assertTrue(descriptor("IconButton", "onPressed").sdkRequired());
        assertTrue(descriptor("FloatingActionButton", "onPressed").sdkRequired());
    }

    @Test
    void carriesVariantAndImagePrerequisites() {
        assertEquals(List.of("noSpinner"), descriptor("RefreshIndicator", "onStatusChange").availableVariants());
        assertEquals(List.of(new PropertyName("backgroundImage")), descriptor("CircleAvatar", "onBackgroundImageError").requiredCompanionProperties());
        assertEquals(List.of(new PropertyName("activeThumbImage")), descriptor("Switch", "onActiveThumbImageError").requiredCompanionProperties());
    }

    @Test
    void choosesOneDefaultEventAndNeverABuilder() {
        for (WidgetDefinition widget : BuiltInWidgetCatalog.getDefault().definitions()) {
            assertTrue(WidgetEventCatalog.eventsFor(widget).stream().filter(WidgetEventDescriptor::defaultEvent).count() <= 1);
            WidgetEventCatalog.defaultEventFor(widget).ifPresent(value -> assertEquals(WidgetEventDescriptor.Kind.EVENT, value.kind()));
        }
        assertEquals("onChanged", WidgetEventCatalog.defaultEventFor(widget("TextField")).orElseThrow().propertyName().value());
        assertTrue(WidgetEventCatalog.defaultEventFor(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Image")).orElseThrow()).isEmpty());
    }

    @Test
    void bindingRetainsLegacyAndTypedSchemaAndRejectsWrongPropertyAndInvalidNames() {
        for (String name : List.of("TextField", "Checkbox", "Radio", "CheckboxListTile")) {
            var widget = widget(name);
            var event = descriptor(name, "onChanged");
            var property = widget.property(new PropertyName("onChanged")).orElseThrow();
            var value = event.bindingValue("_changed", property);
            if (name.equals("TextField")) assertEquals(new PropertyValue.CallbackValue("_changed"), value);
            else assertInstanceOf(PropertyValue.DartObjectReferenceValue.class, value);
            assertTrue(property.constraints().stream().anyMatch(constraint -> constraint.accepts(value)));
            for (String bad : List.of("void", "a.b", "handler()", "x); injected();", "class", "")) {
                assertThrows(IllegalArgumentException.class, () -> event.bindingValue(bad, property));
            }
            assertThrows(IllegalArgumentException.class,
                    () -> event.bindingValue("_changed", widget.property(new PropertyName("value")).orElse(propertyFor("TextField", "onTap"))));
        }
    }

    @Test
    void generatesNonGenericStandaloneMethodStubsAndRequiredImports() {
        String simple = descriptor("TextField", "onChanged").createStub("_searchChanged");
        assertTrue(simple.startsWith("void _searchChanged(String value) {\n"));
        assertFalse(simple.contains("class "));
        assertTrue(descriptor("RefreshIndicator", "onRefresh").createStub("_reload").startsWith("Future<void> _reload() async {"));
        assertEquals(List.of("dart:async"), descriptor("RefreshIndicator", "onRefresh").signature().importUris());
        assertEquals(List.of("package:flutter/material.dart"), descriptor("RangeSlider", "onChanged").signature().importUris());
        assertEquals(List.of("package:flutter/gestures.dart"), descriptor("TextField", "onTapOutside").signature().importUris());
        assertTrue(findWidgets("Image", "errorBuilder").createStub("_imageError").contains("throw UnimplementedError"));
        assertFalse(findWidgets("RadioGroup", "onChanged").createStub("_selectionChanged").contains("T?"));
    }

    @Test
    void doesNotPretendUnsupportedSdkCallbackPropertiesAreImplemented() {
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, descriptor("TextField", "buildCounter").kind());
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, descriptor("TextField", "contextMenuBuilder").kind());
        assertEquals(WidgetEventDescriptor.Kind.PREDICATE, descriptor("AppBar", "notificationPredicate").kind());
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, descriptor("Scaffold", "bottomSheetScrimBuilder").kind());
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, descriptor("ElevatedButton", "styleBackgroundBuilder").kind());
    }

    @Test
    void metadataCannotInjectSourceOrDuplicateParameterNames() {
        assertThrows(IllegalArgumentException.class, () -> new WidgetEventDescriptor.Parameter("String); bad(", "value"));
        assertThrows(IllegalArgumentException.class, () -> new WidgetEventDescriptor.Parameter("ValueChanged<bool", "value"));
        assertThrows(IllegalArgumentException.class, () -> new WidgetEventDescriptor.Signature("void",
                List.of(new WidgetEventDescriptor.Parameter("bool", "value"), new WidgetEventDescriptor.Parameter("int", "value")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WidgetEventDescriptor.Signature("void", List.of(), List.of("bad'; injected")));
        assertThrows(IllegalArgumentException.class, () -> descriptor("TextField", "onChanged").createStub("x() {}"));
    }

    private static WidgetDefinition widget(String name) {
        return BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material." + name)).orElseThrow();
    }

    private static PropertyDefinition propertyFor(String widget, String property) {
        return widget(widget).property(new PropertyName(property)).orElseThrow();
    }

    private static java.util.Optional<WidgetEventDescriptor> find(String widget, String property) {
        return WidgetEventCatalog.find(new WidgetTypeId("flutter.material." + widget), new PropertyName(property));
    }

    private static WidgetEventDescriptor descriptor(String widget, String property) {
        return find(widget, property).orElseThrow();
    }

    private static WidgetEventDescriptor findWidgets(String widget, String property) {
        return WidgetEventCatalog.find(new WidgetTypeId("flutter.widgets." + widget), new PropertyName(property)).orElseThrow();
    }
}
