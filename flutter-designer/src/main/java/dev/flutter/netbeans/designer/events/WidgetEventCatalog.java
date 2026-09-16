package dev.flutter.netbeans.designer.events;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.AppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GestureDetectorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListenerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MouseRegionWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FocusWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.NotificationListenerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Explicit callback inventory reviewed against Flutter 3.44.8 source and API documentation.
 * No property is classified by an {@code on...} name or by merely being a Dart object.
 * Constructor/API gaps are recorded separately in docs/EVENTS_API_AUDIT.md.
 */
public final class WidgetEventCatalog {
    private static final String WIDGETS = "package:flutter/widgets.dart";
    private static final String MATERIAL = "package:flutter/material.dart";
    private static final String GESTURES = "package:flutter/gestures.dart";
    private static final Map<String, Map<String, CallbackSpec>> INVENTORY = inventory();

    private WidgetEventCatalog() { }

    public static Optional<WidgetEventDescriptor> find(WidgetTypeId type, PropertyName propertyName) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(propertyName, "propertyName");
        return BuiltInWidgetCatalog.getDefault().find(type)
                .flatMap(definition -> descriptor(definition, propertyName));
    }

    /** Includes callable builders/predicates/formatters; the Events tab filters {@link WidgetEventDescriptor.Kind#EVENT}. */
    public static List<WidgetEventDescriptor> eventsFor(WidgetDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        ArrayList<WidgetEventDescriptor> result = new ArrayList<>();
        for (PropertyDefinition property : definition.properties()) {
            descriptor(definition, property.name()).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    public static Optional<WidgetEventDescriptor> defaultEventFor(WidgetDefinition definition) {
        return eventsFor(definition).stream().filter(WidgetEventDescriptor::defaultEvent).findFirst();
    }

    private static Optional<WidgetEventDescriptor> descriptor(WidgetDefinition definition, PropertyName name) {
        CallbackSpec spec = INVENTORY.getOrDefault(definition.typeId().value(), Map.of()).get(name.value());
        if (spec == null) return Optional.empty();
        return definition.property(name).map(property -> {
            boolean legacy = property.acceptedKinds().contains(PropertyValueKind.CALLBACK);
            String expectedType = property.constraints().stream()
                    .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast)
                    .map(PropertyValueConstraint.DartObjectReferenceValues::expectedDartType)
                    .findFirst().orElse(null);
            if (!legacy && !spec.type().equals(expectedType)) {
                throw new IllegalArgumentException("Callable inventory/schema mismatch: "
                        + definition.typeId() + "." + name + " expects " + spec.type());
            }
            String widget = definition.dartClassName();
            boolean sdkRequired = name.value().equals("onRefresh")
                    || name.value().equals("onChanged") && Set.of("Checkbox", "CheckboxListTile", "Switch", "SwitchListTile", "Slider", "RangeSlider", "RadioGroup").contains(widget)
                    || name.value().equals("onPressed") && Set.of("ElevatedButton", "TextButton", "OutlinedButton", "FilledButton", "IconButton", "FloatingActionButton").contains(widget);
            if ((definition.typeId().equals(dev.flutter.netbeans.designer.catalog.LayoutBuilderWidgetPropertySchema.TYPE) || definition.typeId().equals(dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.TYPE) || dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.supports(definition.typeId()) || dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.supports(definition.typeId()) || dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.supports(definition.typeId()) || dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(definition.typeId()) || dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(definition.typeId()))
                    || definition.typeId().equals(dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.TYPE)
                    || dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.find(definition.typeId()).isPresent()
                    || dev.flutter.netbeans.designer.catalog.SliverFixedExtentListWidgetPropertySchema.find(definition.typeId()).isPresent()
                    || dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.find(definition.typeId()).isPresent()
                    || dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.find(definition.typeId()).isPresent()) {
                sdkRequired = property.parameter().required();
            }
            boolean nullable = !(widget.equals("RadioGroup") && name.value().equals("onChanged"))
                    && !name.value().equals("onRefresh") && spec.kind() != WidgetEventDescriptor.Kind.PREDICATE
                    && !(widget.equals("Scaffold") && name.value().equals("bottomSheetScrimBuilder"));
            if ((definition.typeId().equals(dev.flutter.netbeans.designer.catalog.LayoutBuilderWidgetPropertySchema.TYPE) || definition.typeId().equals(dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.TYPE) || dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.supports(definition.typeId()) || dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.supports(definition.typeId()) || dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.supports(definition.typeId()) || dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(definition.typeId()) || dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(definition.typeId()))
                    || definition.typeId().equals(dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.TYPE)
                    || dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.find(definition.typeId()).isPresent()
                    || dev.flutter.netbeans.designer.catalog.SliverFixedExtentListWidgetPropertySchema.find(definition.typeId()).isPresent()
                    || dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.find(definition.typeId()).isPresent()
                    || dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.find(definition.typeId()).isPresent()) {
                nullable = property.acceptedKinds().contains(PropertyValueKind.NULL);
            }
            if (definition.typeId().equals(dev.flutter.netbeans.designer.catalog.MatrixTransitionWidgetPropertySchema.TYPE)
                    || definition.typeId().equals(dev.flutter.netbeans.designer.catalog.ShaderMaskWidgetPropertySchema.TYPE)) {
                sdkRequired = property.parameter().required();
                nullable = false;
            }
            if ((definition.typeId().equals(dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.TYPE) || definition.typeId().equals(dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.TYPE))) nullable = property.acceptedKinds().contains(PropertyValueKind.NULL);
            if (definition.typeId().equals(dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.TYPE) || definition.typeId().equals(dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.TYPE)) {
                sdkRequired=property.parameter().required();
                nullable=property.acceptedKinds().contains(PropertyValueKind.NULL);
            }
            if (dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.supports(definition.typeId())
                    || definition.typeId().equals(dev.flutter.netbeans.designer.catalog.BottomSheetWidgetPropertySchema.TYPE)) {
                sdkRequired = property.parameter().required();
                nullable = property.acceptedKinds().contains(PropertyValueKind.NULL);
            }
            if (definition.typeId().value().equals("flutter.widgets.SliverFillViewport")) nullable = false;
            return new WidgetEventDescriptor(name, spec.type(), spec.kind(), spec.signature(),
                    property.parameter().required(), property.acceptedKinds().contains(PropertyValueKind.NULL),
                    sdkRequired, nullable, spec.defaultEvent(), property.creationDefault(), unsetBehavior(widget, name.value()),
                    widget.equals("RefreshIndicator") && name.value().equals("onStatusChange") ? List.of("noSpinner")
                            : widget.equals("Focus") && Set.of("onKeyEvent", "onKey").contains(name.value()) ? List.of("standard") : List.of(),
                    companionProperties(name.value()));
        });
    }

    private static List<PropertyName> companionProperties(String name) {
        return switch (name) {
            case "onBackgroundImageError" -> List.of(new PropertyName("backgroundImage"));
            case "onForegroundImageError" -> List.of(new PropertyName("foregroundImage"));
            case "onActiveThumbImageError" -> List.of(new PropertyName("activeThumbImage"));
            case "onInactiveThumbImageError" -> List.of(new PropertyName("inactiveThumbImage"));
            default -> List.of();
        };
    }

    private static String unsetBehavior(String widget, String property) {
        if (widget.equals("BottomSheet")) return dev.flutter.netbeans.designer.catalog.BottomSheetWidgetPropertySchema.help(property);
        if (widget.equals("SnackBar") || widget.equals("SnackBarAction")) return dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.help(property);
        if (widget.equals("MaterialBanner")) return dev.flutter.netbeans.designer.catalog.MaterialBannerWidgetPropertySchema.help(property);
        if ((widget.equals("ModalBarrier") || widget.equals("AnimatedModalBarrier")) && property.equals("onDismiss")) return "Unset/null uses Navigator.maybePop when Dismissible is true; otherwise the retained callback is ignored. A bound callback owns dismissal; Canvas suppresses callback, route changes and alert sounds.";
        if (widget.equals("CalendarDatePicker") && property.equals("onDateChanged")) return "Required ValueChanged<DateTime>. Disconnect restores the explicit no-op; it never writes null or removes the required callback.";
        if (widget.equals("ShaderMask") && property.equals("shaderCallback")) return "Required Shader Function(Rect bounds). Disconnect restores the opaque-white gradient; it never writes null or removes the required callback.";
        if (widget.equals("MatrixTransition") && property.equals("onTransform")) return "Required TransformCallback or a structured fixed-matrix callback. Null/unset is not allowed; disconnect restores the identity matrix preset. Return a fresh Matrix4 from the animation value.";
        if (widget.equals("TweenAnimationBuilder") && property.equals("builder")) return "Required typed builder or Child preset; null/unset are not allowed. Match the selected box/sliver result.";
        if (widget.equals("ListenableBuilder") || widget.equals("AnimatedBuilder") || widget.equals("ValueListenableBuilder")) return "Required callback: retain Child or a typed project reference. Unset/null is not allowed; Child and result must match the selected box/sliver placement.";
        if (widget.equals("DeviceOrientationBuilder")) return "Required callback: retain Empty or a typed project reference. Unset/null is not allowed; return a widget matching the selected box/sliver placement projection.";
        if (widget.equals("LayoutBuilder") || widget.equals("OrientationBuilder")) return "Required callback: retain the empty box preset or a typed project reference. Unset/null is not allowed; return a box widget, not a sliver.";
        if (widget.equals("SliverLayoutBuilder")) return "Required callback: retain the empty sliver preset or a typed project reference. Unset/null is not allowed; the result must be a sliver, not a box.";

        if (widget.equals("SliverFixedExtentList") || widget.equals("SliverPrototypeExtentList") || widget.equals("SliverVariedExtentList")) {
            if (property.equals("itemExtentBuilder")) return "Required extent callback: retain the 48 preset or a typed project reference. Return a finite non-negative extent for each actual child; null only out of range. Unset/null is not allowed.";
            return property.equals("itemBuilder")
                    ? "Required callback: retain the empty preset or a typed project reference. Empty returns null immediately; unset/null is not allowed."
                    : "Omission/null disables custom key-to-item-index lookup; supply a typed callback to retain child state when builder items reorder.";
        }
        if (Set.of("SliverList", "SliverGrid").contains(widget)) {
            if (property.equals("itemBuilder")) return "Required callback: retain the explicit empty preset or a typed project reference. Empty returns null immediately and builds no items; unset/null is not allowed.";
            if (property.equals("separatorBuilder")) return "Required callback: retain the shrink preset or a typed project reference returning a non-null Widget. Unset/null is not allowed.";
            return "Omission/null disables custom child-index lookup. SliverList.separated must not combine child-index and item-index callbacks; reset one explicitly.";
        }
        if (widget.equals("MenuItemButton") && property.equals("onPressed")) {
            return "Enabled with no reference generates a no-op; disabled emits null and retains the reference. Native activation applies focus changes before its post-frame callback; shortcut is a hint, not a registered action.";
        }
        if (widget.equals("Tooltip") && property.equals("onTriggered")) {
            return "Omission/null leaves notification unset. Flutter 3.44.8 invokes this callback for accepted tap/long-press triggers, not hover or ensureTooltipVisible().";
        }
        if (widget.equals("Tooltip") && property.equals("positionDelegate")) {
            return "Omission/null preserves SDK tooltip positioning. A custom delegate returns Offset from TooltipPositionContext; it is not an Event or a Widget builder.";
        }
        if (widget.equals("ListView") && property.equals("itemExtentBuilder")) {
            return "Omission or a null callback uses normal child sizing, or itemExtent when configured. A callback must provide a valid extent for every actual child and return null only for an out-of-range index, not to request default sizing or truncate existing children. A project reference cannot coexist with itemExtent.";
        }
        if (widget.equals("TextField") && property.equals("buildCounter")) {
            return "Omission or a null callback uses Flutter's default counter. A callback returning null hides the counter and its Semantics.";
        }
        if (widget.equals("TextField") && property.equals("contextMenuBuilder")) {
            return "Omission uses Flutter's default context menu; an explicit null or null callback reference disables the context menu.";
        }
        if (widget.equals("Scaffold") && property.equals("bottomSheetScrimBuilder")) {
            return "Omission uses Flutter's default animated bottom-sheet scrim. The callback cannot be null; returning null produces no scrim.";
        }
        if (widget.equals("NotificationListener") && property.equals("onNotification")) {
            return "Omission/null continues notification bubbling. A callback returns true to stop propagation or false to continue.";
        }
        if (property.equals("onRefresh")) return "Unset generates an async no-op that completes immediately.";
        if (property.equals("onChanged") && Set.of("RadioGroup", "CheckboxListTile", "SwitchListTile").contains(widget)) {
            return "Required model argument: retain the explicit no-op, a handler, or an admitted null value.";
        }
        if (property.equals("onChanged") && Set.of("Checkbox", "Switch", "Slider", "RangeSlider").contains(widget)) {
            return "Unset generates a no-op when enabled and null when disabled; it does not update the value.";
        }
        if (property.equals("onChanged") && Set.of("Radio", "RadioListTile").contains(widget)) {
            return "Unset preserves RadioGroup ownership; the creation default is an explicit no-op for legacy mode.";
        }
        if (widget.equals("SimpleDialogOption") && property.equals("onPressed")) {
            return "Unset/null disables selection. A handler may return the dialog result through Navigator.pop(context, result). Canvas never executes handlers.";
        }
        if (property.equals("onPressed")) {
            return "Unset uses the existing enabled/disabled and long-press rules; enabled controls may generate a no-op.";
        }
        if (property.equals("notificationPredicate")) return "Unset preserves Flutter's default depth-zero predicate.";
        return "Unset omits this argument and preserves Flutter's default behavior.";
    }

    private static Map<String, Map<String, CallbackSpec>> inventory() {
        Map<String, Map<String, CallbackSpec>> widgets = new LinkedHashMap<>();
        for (String type : dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.TYPES) {
            add(widgets, type, "onStretchTrigger", "AsyncCallback", true, "Future<void>");
        }
        add(widgets, NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE.value(), "onNotification",
                NotificationListenerWidgetPropertySchema.CALLBACK_TYPE, true, "bool", "Notification:notification");
        FocusWidgetPropertySchema.callbackDefinitions().forEach((name, definition) ->
                add(widgets, FocusWidgetPropertySchema.FOCUS_TYPE.value(), name, definition.callbackType().orElseThrow(),
                        name.equals("onFocusChange"), definition.returnType(), definition.parameters().toArray(String[]::new)));
        MouseRegionWidgetPropertySchema.callbackDefinitions().forEach((name, definition) ->
                add(widgets, MouseRegionWidgetPropertySchema.MOUSE_REGION_TYPE.value(),
                        name, definition.callbackType().orElseThrow(), name.equals("onEnter"), "void",
                        definition.parameterType().orElseThrow() + ":event"));
        ListenerWidgetPropertySchema.callbackDefinitions().forEach((name, definition) ->
                add(widgets, ListenerWidgetPropertySchema.LISTENER_TYPE.value(),
                        name, definition.callbackType().orElseThrow(), name.equals("onPointerDown"), "void",
                        definition.parameterType().orElseThrow() + ":event"));
        GestureDetectorWidgetPropertySchema.callbackDefinitions().forEach((name, definition) ->
                add(widgets, GestureDetectorWidgetPropertySchema.GESTURE_DETECTOR_TYPE.value(),
                        name, definition.callbackType().orElseThrow(), name.equals("onTap"), "void",
                        definition.parameterType().map(type -> new String[] {type + ":details"})
                                .orElseGet(() -> new String[0])));
        add(widgets, "flutter.widgets.AnimatedAlign", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.AnimatedPadding", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.AnimatedSlide", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.AnimatedScale", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.AnimatedRotation", "onEnd", "VoidCallback", true, "void");
        for (var type : dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.TYPES)
            add(widgets, type.value(), "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.AnimatedDefaultTextStyle", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.AnimatedPhysicalModel", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.material.AnimatedTheme", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.AnimatedFractionallySizedBox", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.AnimatedSize", "onEnd", "VoidCallback", true, "void");
        addCallable(widgets, "flutter.widgets.AnimatedSwitcher", "transitionBuilder", "AnimatedSwitcherTransitionBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "Widget:child", "Animation<double>:animation");
        addCallable(widgets, "flutter.widgets.AnimatedSwitcher", "layoutBuilder", "AnimatedSwitcherLayoutBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "Widget?:currentChild", "List<Widget>:previousChildren");
        add(widgets, "flutter.widgets.AnimatedCrossFade", "onEnd", "VoidCallback", true, "void");
        addCallable(widgets, "flutter.widgets.AnimatedCrossFade", "layoutBuilder", "AnimatedCrossFadeBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "Widget:topChild", "Key:topChildKey", "Widget:bottomChild", "Key:bottomChildKey");
        add(widgets, "flutter.widgets.AnimatedContainer", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.AnimatedOpacity", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.SliverAnimatedOpacity", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.TweenAnimationBuilder", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.widgets.TweenAnimationBuilder.sliver", "onEnd", "VoidCallback", true, "void");
        add(widgets, "flutter.material.Scaffold", "onDrawerChanged", "DrawerCallback", false, "void", "bool:isOpened");
        add(widgets, "flutter.material.Scaffold", "onEndDrawerChanged", "DrawerCallback", false, "void", "bool:isOpened");
        addCallable(widgets, "flutter.material.Scaffold", "bottomSheetScrimBuilder", ScaffoldWidgetPropertySchema.BOTTOM_SHEET_SCRIM_BUILDER_TYPE,
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget?", "BuildContext:context", "Animation<double>:animation");
        addCallable(widgets, AppBarWidgetPropertySchema.APP_BAR_TYPE.value(), "notificationPredicate",
                AppBarWidgetPropertySchema.NOTIFICATION_PREDICATE_TYPE,
                WidgetEventDescriptor.Kind.PREDICATE, false, "bool", "ScrollNotification:notification");
        addCallable(widgets, "flutter.material.Scrollbar", "notificationPredicate",
                "ScrollNotificationPredicate", WidgetEventDescriptor.Kind.PREDICATE,
                false, "bool", "ScrollNotification:notification");
        for (String widget : List.of("ElevatedButton", "TextButton", "OutlinedButton", "FilledButton")) {
            String type = "flutter.material." + widget;
            add(widgets, type, "onPressed", "VoidCallback", true, "void");
            add(widgets, type, "onLongPress", "VoidCallback", false, "void");
            add(widgets, type, "onHover", "ValueChanged<bool>", false, "void", "bool:isHovered");
            add(widgets, type, "onFocusChange", "ValueChanged<bool>", false, "void", "bool:hasFocus");
            buttonBuilders(widgets, type);
        }
        add(widgets, "flutter.material.FloatingActionButton", "onPressed", "VoidCallback", true, "void");
        add(widgets, "flutter.material.IconButton", "onPressed", "VoidCallback", true, "void");
        add(widgets, "flutter.material.SimpleDialogOption", "onPressed", "VoidCallback?", true, "void");
        add(widgets, "flutter.material.IconButton", "onLongPress", "VoidCallback", false, "void");
        add(widgets, "flutter.material.IconButton", "onHover", "ValueChanged<bool>", false, "void", "bool:isHovered");
        buttonBuilders(widgets, "flutter.material.IconButton");
        for (String widget : List.of("Checkbox", "CheckboxListTile")) {
            add(widgets, "flutter.material." + widget, "onChanged", "ValueChanged<bool?>", true, "void", "bool?:value");
        }
        add(widgets, "flutter.material.CheckboxListTile", "onFocusChange", "ValueChanged<bool>", false, "void", "bool:hasFocus");
        add(widgets, "flutter.material.Switch", "onChanged", "ValueChanged<bool>", true, "void", "bool:value");
        add(widgets, "flutter.material.Switch", "onFocusChange", "ValueChanged<bool>", false, "void", "bool:hasFocus");
        add(widgets, "flutter.material.SwitchListTile", "onChanged", "ValueChanged<bool>", true, "void", "bool:value");
        add(widgets, "flutter.material.SwitchListTile", "onFocusChange", "ValueChanged<bool>", false, "void", "bool:hasFocus");
        add(widgets, "flutter.material.RadioListTile", "onChanged", "ValueChanged<Object?>", true, "void", "Object?:value");
        add(widgets, "flutter.material.RadioListTile", "onFocusChange", "ValueChanged<bool>", false, "void", "bool:hasFocus");
        add(widgets, "flutter.material.ExpansionTile", "onExpansionChanged", "ValueChanged<bool>", true, "void", "bool:isExpanded");
        add(widgets, "flutter.material.Tooltip", "onTriggered", "TooltipTriggeredCallback", true, "void");
        add(widgets, "flutter.material.MenuItemButton", "onPressed", "VoidCallback", true, "void");
        add(widgets, "flutter.material.MenuItemButton", "onHover", "ValueChanged<bool>", false, "void", "bool:value");
        add(widgets, "flutter.material.MenuItemButton", "onFocusChange", "ValueChanged<bool>", false, "void", "bool:value");
        buttonBuilders(widgets, "flutter.material.MenuItemButton");
        add(widgets, "flutter.material.MenuAnchor", "onOpen", "VoidCallback", true, "void");
        add(widgets, "flutter.material.MenuAnchor", "onClose", "VoidCallback", false, "void");
        add(widgets, "flutter.material.MenuAnchor", "onAnimationStatusChanged", "ValueChanged<AnimationStatus>", false, "void", "AnimationStatus:status");
        add(widgets, "flutter.material.NavigationBar", "onDestinationSelected", "ValueChanged<int>", false, "void", "int:index");
        add(widgets, "flutter.material.NavigationRail", "onDestinationSelected", "ValueChanged<int>", false, "void", "int:index");
        add(widgets, "flutter.material.NavigationDrawer", "onDestinationSelected", "ValueChanged<int>", false, "void", "int:index");
        add(widgets, "flutter.material.BottomNavigationBar", "onTap", "ValueChanged<int>", false, "void", "int:index");
        add(widgets, "flutter.widgets.PageView", "onPageChanged", "ValueChanged<int>", false, "void", "int:index");
        add(widgets, "flutter.widgets.ListWheelScrollView", "onSelectedItemChanged", "ValueChanged<int>", false, "void", "int:index");
        add(widgets, "flutter.material.SubmenuButton", "onHover", "ValueChanged<bool>", false, "void", "bool:value");
        add(widgets, "flutter.material.SubmenuButton", "onFocusChange", "ValueChanged<bool>", false, "void", "bool:value");
        add(widgets, "flutter.material.SubmenuButton", "onOpen", "VoidCallback", true, "void");
        add(widgets, "flutter.material.SubmenuButton", "onClose", "VoidCallback", false, "void");
        add(widgets, "flutter.material.SubmenuButton", "onAnimationStatusChanged", "ValueChanged<AnimationStatus>", false, "void", "AnimationStatus:status");
        addCallable(widgets, "flutter.material.SubmenuButton", "styleBackgroundBuilder", "ButtonLayerBuilder", WidgetEventDescriptor.Kind.BUILDER,
                false, "Widget", "BuildContext:context", "Set<WidgetState>:states", "Widget?:child");
        addCallable(widgets, "flutter.material.SubmenuButton", "styleForegroundBuilder", "ButtonLayerBuilder", WidgetEventDescriptor.Kind.BUILDER,
                false, "Widget", "BuildContext:context", "Set<WidgetState>:states", "Widget?:child");
        addCallable(widgets, "flutter.widgets.LayoutBuilder", "builder", "LayoutWidgetBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "BoxConstraints:constraints");
        addCallable(widgets, "flutter.widgets.OrientationBuilder", "builder", "OrientationWidgetBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "Orientation:orientation");
        for (String type : List.of("flutter.widgets.ValueListenableBuilder", "flutter.widgets.ValueListenableBuilder.sliver",
                "flutter.widgets.TweenAnimationBuilder", "flutter.widgets.TweenAnimationBuilder.sliver")) {
            // Catalog template only: generation replaces Object with the independently proven selected T.
            addCallable(widgets, type, "builder", "ValueWidgetBuilder<Object>",
                    WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "Object:value", "Widget?:child");
        }
        addCallable(widgets, "flutter.widgets.AnimatedBuilder", "builder", "TransitionBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "Widget?:child");
        addCallable(widgets, "flutter.widgets.AnimatedBuilder.sliver", "builder", "TransitionBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "Widget?:child");
        addCallable(widgets, "flutter.widgets.ListenableBuilder", "builder", "TransitionBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "Widget?:child");
        addCallable(widgets, "flutter.widgets.ListenableBuilder.sliver", "builder", "TransitionBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "Widget?:child");
        addCallable(widgets, "flutter.widgets.DeviceOrientationBuilder", "builder", "OrientationWidgetBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "Orientation:orientation");
        addCallable(widgets, "flutter.widgets.DeviceOrientationBuilder.sliver", "builder", "OrientationWidgetBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "Orientation:orientation");
        addCallable(widgets, "flutter.widgets.SliverLayoutBuilder", "builder", "SliverLayoutWidgetBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "SliverConstraints:constraints");
        addCallable(widgets, "flutter.material.MenuAnchor", "builder", "MenuAnchorChildBuilder", WidgetEventDescriptor.Kind.BUILDER,
                false, "Widget", "BuildContext:context", "MenuController:controller", "Widget?:child");
        add(widgets, "flutter.widgets.ModalBarrier", "onDismiss", "VoidCallback?", true, "void");
        add(widgets, "flutter.widgets.AnimatedModalBarrier", "onDismiss", "VoidCallback?", true, "void");
        addCallable(widgets, "flutter.widgets.ShaderMask", "shaderCallback", "ShaderCallback",
                WidgetEventDescriptor.Kind.DELEGATE, false, "Shader", "Rect:bounds");
        addCallable(widgets, "flutter.widgets.MatrixTransition", "onTransform", "TransformCallback",
                WidgetEventDescriptor.Kind.DELEGATE, false, "Matrix4", "double:animationValue");
        addCallable(widgets, "flutter.material.Tooltip", "positionDelegate", "TooltipPositionDelegate", WidgetEventDescriptor.Kind.DELEGATE, false, "Offset", "TooltipPositionContext:context");
        for (String name : List.of("onActiveThumbImageError", "onInactiveThumbImageError")) {
            add(widgets, "flutter.material.Switch", name, "ImageErrorListener", false, "void", "Object:exception", "StackTrace?:stackTrace");
            add(widgets, "flutter.material.SwitchListTile", name, "ImageErrorListener", false, "void", "Object:exception", "StackTrace?:stackTrace");
        }
        for (String name : List.of("onBackgroundImageError", "onForegroundImageError")) {
            add(widgets, "flutter.material.CircleAvatar", name, "ImageErrorListener", false, "void", "Object:exception", "StackTrace?:stackTrace");
        }
        add(widgets, "flutter.material.ListTile", "onTap", "VoidCallback", true, "void");
        add(widgets, "flutter.material.ListTile", "onLongPress", "VoidCallback", false, "void");
        add(widgets, "flutter.material.ListTile", "onFocusChange", "ValueChanged<bool>", false, "void", "bool:hasFocus");
        for (String widget : List.of("Slider", "RangeSlider")) {
            String parameterType = widget.equals("Slider") ? "double" : "RangeValues";
            for (String name : List.of("onChanged", "onChangeStart", "onChangeEnd")) {
                add(widgets, "flutter.material." + widget, name, "ValueChanged<" + parameterType + ">", name.equals("onChanged"),
                        "void", parameterType + ":value");
            }
            addCallable(widgets, "flutter.material." + widget, "semanticFormatterCallback", "SemanticFormatterCallback",
                    WidgetEventDescriptor.Kind.FORMATTER, false, "String", "double:value");
        }
        for (String type : List.of("flutter.material.Radio", "flutter.widgets.RadioGroup")) {
            // Object? is contravariantly assignable for every supported Radio<T>; raw T is not in the form scope.
            add(widgets, type, "onChanged", "ValueChanged<Object?>", true, "void", "Object?:value");
        }
        add(widgets, "flutter.material.RefreshIndicator", "onRefresh", "RefreshCallback", true, "Future<void>");
        add(widgets, "flutter.material.RefreshIndicator", "onStatusChange", "ValueChanged<RefreshIndicatorStatus?>", false,
                "void", "RefreshIndicatorStatus?:status");
        addCallable(widgets, "flutter.material.RefreshIndicator", "notificationPredicate", "ScrollNotificationPredicate",
                WidgetEventDescriptor.Kind.PREDICATE, false, "bool", "ScrollNotification:notification");
        String bottomSheet = "flutter.material.BottomSheet";
        add(widgets, "flutter.material.SnackBar", "onVisible", "VoidCallback?", true, "void");
        add(widgets, "flutter.material.MaterialBanner", "onVisible", "VoidCallback?", true, "void");
        add(widgets, "flutter.material.SnackBarAction", "onPressed", "VoidCallback", true, "void");
        add(widgets, bottomSheet, "onClosing", "VoidCallback", true, "void");
        add(widgets, bottomSheet, "onDragStart", "BottomSheetDragStartHandler?", false, "void", "DragStartDetails:details");
        addCallable(widgets, bottomSheet, "onDragEnd", "BottomSheetDragEndHandler?", WidgetEventDescriptor.Kind.EVENT,
                false, "void", List.of(new WidgetEventDescriptor.Parameter("DragEndDetails", "details"),
                        WidgetEventDescriptor.Parameter.requiredNamed("bool", "isClosing")));
        addCallable(widgets, bottomSheet, "builder", "WidgetBuilder", WidgetEventDescriptor.Kind.BUILDER,
                false, "Widget", "BuildContext:context");
        String textField = "flutter.material.TextField";
        addCallable(widgets, textField, "buildCounter", TextFieldWidgetPropertySchema.INPUT_COUNTER_BUILDER_TYPE,
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget?", List.of(
                        new WidgetEventDescriptor.Parameter("BuildContext", "context"),
                        WidgetEventDescriptor.Parameter.requiredNamed("int", "currentLength"),
                        WidgetEventDescriptor.Parameter.requiredNamed("int?", "maxLength"),
                        WidgetEventDescriptor.Parameter.requiredNamed("bool", "isFocused")));
        addCallable(widgets, textField, "contextMenuBuilder", TextFieldWidgetPropertySchema.CONTEXT_MENU_BUILDER_TYPE,
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget", "BuildContext:context", "EditableTextState:editableTextState");
        add(widgets, textField, "onChanged", "ValueChanged<String>", true, "void", "String:value");
        add(widgets, textField, "onSubmitted", "ValueChanged<String>", false, "void", "String:value");
        add(widgets, textField, "onEditingComplete", "VoidCallback", false, "void");
        add(widgets, textField, "onAppPrivateCommand", "AppPrivateCommandCallback", false, "void", "String:action", "Map<String, dynamic>:data");
        add(widgets, textField, "onTap", "GestureTapCallback", false, "void");
        add(widgets, textField, "onTapOutside", "TapRegionCallback", false, "void", "PointerDownEvent:event");
        add(widgets, textField, "onTapUpOutside", "TapRegionUpCallback", false, "void", "PointerUpEvent:event");
        addCallable(widgets, "flutter.widgets.Image", "frameBuilder", "ImageFrameBuilder", WidgetEventDescriptor.Kind.BUILDER, false,
                "Widget", "BuildContext:context", "Widget:child", "int?:frame", "bool:wasSynchronouslyLoaded");
        addCallable(widgets, "flutter.widgets.ListView", "itemExtentBuilder", ListViewWidgetPropertySchema.ITEM_EXTENT_BUILDER_TYPE,
                WidgetEventDescriptor.Kind.BUILDER, false, "double?", "int:index", "SliverLayoutDimensions:dimensions");
        addCallable(widgets, "flutter.widgets.Image", "loadingBuilder", "ImageLoadingBuilder", WidgetEventDescriptor.Kind.BUILDER, false,
                "Widget", "BuildContext:context", "Widget:child", "ImageChunkEvent?:loadingProgress");
        for(String name:List.of("placeholderErrorBuilder","imageErrorBuilder"))
            addCallable(widgets, "flutter.widgets.FadeInImage", name, "ImageErrorWidgetBuilder?", WidgetEventDescriptor.Kind.BUILDER, false,
                    "Widget", "BuildContext:context", "Object:error", "StackTrace?:stackTrace");
        addCallable(widgets, "flutter.widgets.Image", "errorBuilder", "ImageErrorWidgetBuilder", WidgetEventDescriptor.Kind.BUILDER, false,
                "Widget", "BuildContext:context", "Object:error", "StackTrace?:stackTrace");
        for (var kind : dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.Kind.values()) {
            for (var field : dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.fields(kind)) {
                if (field.type().equals("NullableIndexedWidgetBuilder") || field.type().equals("IndexedWidgetBuilder")) {
                    addCallable(widgets, kind.type().value(), field.name(), field.type(),
                            WidgetEventDescriptor.Kind.BUILDER, false,
                            field.type().equals("IndexedWidgetBuilder") ? "Widget" : "Widget?",
                            "BuildContext:context", "int:index");
                } else if (field.type().equals("ChildIndexGetter?")) {
                    addCallable(widgets, kind.type().value(), field.name(), field.type(),
                            WidgetEventDescriptor.Kind.DELEGATE, false, "int?", "Key:key");
                }
            }
        }
        addCallable(widgets, "flutter.widgets.SliverFixedExtentList.builder", "itemBuilder", "NullableIndexedWidgetBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget?", "BuildContext:context", "int:index");
        addCallable(widgets, "flutter.widgets.SliverFixedExtentList.builder", "findChildIndexCallback", "ChildIndexGetter?",
                WidgetEventDescriptor.Kind.DELEGATE, false, "int?", "Key:key");
        addCallable(widgets, "flutter.widgets.SliverPrototypeExtentList.builder", "itemBuilder", "NullableIndexedWidgetBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget?", "BuildContext:context", "int:index");
        addCallable(widgets, "flutter.widgets.SliverPrototypeExtentList.builder", "findChildIndexCallback", "ChildIndexGetter?",
                WidgetEventDescriptor.Kind.DELEGATE, false, "int?", "Key:key");
        addCallable(widgets, "flutter.widgets.SliverVariedExtentList.builder", "itemBuilder", "NullableIndexedWidgetBuilder",
                WidgetEventDescriptor.Kind.BUILDER, false, "Widget?", "BuildContext:context", "int:index");
        addCallable(widgets, "flutter.widgets.SliverVariedExtentList.builder", "findChildIndexCallback", "ChildIndexGetter?",
                WidgetEventDescriptor.Kind.DELEGATE, false, "int?", "Key:key");
        for (var kind : dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.Kind.values()) {
            addCallable(widgets, kind.type().value(), "itemExtentBuilder", "ItemExtentBuilder",
                    WidgetEventDescriptor.Kind.BUILDER, false, "double?", "int:index", "SliverLayoutDimensions:dimensions");
        }
        addCallable(widgets, "flutter.widgets.SliverFillViewport", "semanticIndexCallback", "SemanticIndexCallback",
                WidgetEventDescriptor.Kind.DELEGATE, false, "int?", "Widget:widget", "int:localIndex");
        addCallable(widgets,"flutter.material.DateRangePickerDialog","selectableDayPredicate","SelectableDayForRangePredicate",
                WidgetEventDescriptor.Kind.PREDICATE,false,"bool","DateTime:day","DateTime?:selectedStartDay","DateTime?:selectedEndDay");
        add(widgets,"flutter.material.TimePickerDialog","onEntryModeChanged","EntryModeChangeCallback",true,"void","TimePickerEntryMode:mode");
        add(widgets,"flutter.material.InputDatePickerFormField","onDateSubmitted","ValueChanged<DateTime>",true,"void","DateTime:date");
        add(widgets,"flutter.material.InputDatePickerFormField","onDateSaved","ValueChanged<DateTime>",false,"void","DateTime:date");
        addCallable(widgets,"flutter.material.InputDatePickerFormField","selectableDayPredicate","SelectableDayPredicate",
                WidgetEventDescriptor.Kind.PREDICATE,false,"bool","DateTime:date");
        add(widgets,"flutter.material.CalendarDatePicker","onDateChanged","ValueChanged<DateTime>",true,"void","DateTime:date");
        add(widgets,"flutter.material.CalendarDatePicker","onDisplayedMonthChanged","ValueChanged<DateTime>",false,"void","DateTime:date");
        addCallable(widgets,"flutter.material.CalendarDatePicker","selectableDayPredicate","SelectableDayPredicate",
                WidgetEventDescriptor.Kind.PREDICATE,false,"bool","DateTime:date");
        add(widgets,"flutter.material.DatePickerDialog","onDatePickerModeChange","ValueChanged<DatePickerEntryMode>",true,"void","DatePickerEntryMode:mode");
        addCallable(widgets,"flutter.material.DatePickerDialog","selectableDayPredicate","SelectableDayPredicate",
                WidgetEventDescriptor.Kind.PREDICATE,false,"bool","DateTime:date");
        add(widgets,"flutter.material.PaginatedDataTable","onSelectAll","ValueSetter<bool?>",false,"void","bool?:selected");
        add(widgets,"flutter.material.PaginatedDataTable","onPageChanged","ValueChanged<int>",true,"void","int:firstRowIndex");
        add(widgets,"flutter.material.PaginatedDataTable","onRowsPerPageChanged","ValueChanged<int?>",false,"void","int?:rowsPerPage");
        add(widgets,"flutter.material.DataTable","onSelectAll","ValueSetter<bool?>",true,"void","bool?:selected");
        add(widgets,"flutter.material.DataColumn","onSort","DataColumnSortCallback",true,"void","int:columnIndex","bool:ascending");
        for(String row:List.of("flutter.material.DataRow","flutter.material.DataRow.byIndex")) {
            add(widgets,row,"onSelectChanged","ValueChanged<bool?>",true,"void","bool?:selected");
            add(widgets,row,"onLongPress","GestureLongPressCallback",false,"void");
            add(widgets,row,"onHover","ValueChanged<bool>",false,"void","bool:hovered");
        }
        for(String name:List.of("onTap","onDoubleTap"))add(widgets,"flutter.material.DataCell",name,"GestureTapCallback",name.equals("onTap"),"void");
        add(widgets,"flutter.material.DataCell","onLongPress","GestureLongPressCallback",false,"void");
        add(widgets,"flutter.material.DataCell","onTapDown","GestureTapDownCallback",false,"void","TapDownDetails:details");
        add(widgets,"flutter.material.DataCell","onTapCancel","GestureTapCancelCallback",false,"void");
        Map<String, Map<String, CallbackSpec>> result = new LinkedHashMap<>();
        widgets.forEach((type, values) -> result.put(type, Map.copyOf(values)));
        return Map.copyOf(result);
    }

    private static void buttonBuilders(Map<String, Map<String, CallbackSpec>> widgets, String type) {
        for (String name : List.of("styleBackgroundBuilder", "styleForegroundBuilder")) {
            addCallable(widgets, type, name, "ButtonLayerBuilder", WidgetEventDescriptor.Kind.BUILDER, false,
                    "Widget", "BuildContext:context", "Set<WidgetState>:states", "Widget?:child");
        }
    }

    private static void add(Map<String, Map<String, CallbackSpec>> widgets, String type, String name,
            String callbackType, boolean defaultEvent, String result, String... parameters) {
        addCallable(widgets, type, name, callbackType, WidgetEventDescriptor.Kind.EVENT, defaultEvent, result, parameters);
    }

    private static void addCallable(Map<String, Map<String, CallbackSpec>> widgets, String type, String name,
            String callbackType, WidgetEventDescriptor.Kind kind, boolean defaultEvent, String result, String... parameters) {
        List<WidgetEventDescriptor.Parameter> args = java.util.Arrays.stream(parameters).map(value -> {
            int separator = value.lastIndexOf(':');
            return new WidgetEventDescriptor.Parameter(value.substring(0, separator), value.substring(separator + 1));
        }).toList();
        addCallable(widgets, type, name, callbackType, kind, defaultEvent, result, args);
    }

    private static void addCallable(Map<String, Map<String, CallbackSpec>> widgets, String type, String name,
            String callbackType, WidgetEventDescriptor.Kind kind, boolean defaultEvent, String result,
            List<WidgetEventDescriptor.Parameter> args) {
        List<String> imports = new ArrayList<>();
        if (result.equals("Future<void>")) imports.add("dart:async");
        if (callbackType.equals("EntryModeChangeCallback") || callbackType.equals("DataColumnSortCallback") || callbackType.equals("ValueChanged<DatePickerEntryMode>") || callbackType.equals("SelectableDayPredicate")) imports.add(MATERIAL);
        if (callbackType.equals("ShaderCallback")) imports.add(WIDGETS);
        if (callbackType.equals(NotificationListenerWidgetPropertySchema.CALLBACK_TYPE) || callbackType.equals("ChildIndexGetter?")) imports.add(WIDGETS);
        if (callbackType.equals("TooltipPositionDelegate") || callbackType.equals("TooltipTriggeredCallback")) imports.add(WIDGETS);
        if (callbackType.equals("ValueChanged<AnimationStatus>") || callbackType.equals("TransformCallback")) imports.add(WIDGETS);
        if (Set.of("SliverLayoutWidgetBuilder", "LayoutWidgetBuilder").contains(callbackType)) imports.add("package:flutter/rendering.dart");
        if (callbackType.equals("FocusOnKeyEventCallback") || callbackType.equals("FocusOnKeyCallback")) {
            imports.add(WIDGETS);
            imports.add("package:flutter/services.dart");
        }
        if (callbackType.startsWith("Gesture")
                || args.stream().anyMatch(value -> value.type().startsWith("Pointer"))) imports.add(GESTURES);
        if (callbackType.equals("MenuAnchorChildBuilder") || callbackType.equals("ButtonLayerBuilder") || callbackType.contains("RangeValues") || callbackType.contains("RefreshIndicatorStatus")
                || callbackType.equals(TextFieldWidgetPropertySchema.INPUT_COUNTER_BUILDER_TYPE)) {
            imports.add(MATERIAL);
        } else if (callbackType.equals(ListViewWidgetPropertySchema.ITEM_EXTENT_BUILDER_TYPE)) {
            imports.add("package:flutter/rendering.dart");
        } else if (kind == WidgetEventDescriptor.Kind.BUILDER || callbackType.equals("ScrollNotificationPredicate")) {
            imports.add(WIDGETS);
        }
        CallbackSpec spec = new CallbackSpec(callbackType, kind, new WidgetEventDescriptor.Signature(result, args, imports), defaultEvent);
        if (widgets.computeIfAbsent(type, ignored -> new LinkedHashMap<>()).putIfAbsent(name, spec) != null) {
            throw new IllegalStateException("Duplicate event descriptor: " + type + "." + name);
        }
    }

    private record CallbackSpec(String type, WidgetEventDescriptor.Kind kind,
            WidgetEventDescriptor.Signature signature, boolean defaultEvent) { }
}
