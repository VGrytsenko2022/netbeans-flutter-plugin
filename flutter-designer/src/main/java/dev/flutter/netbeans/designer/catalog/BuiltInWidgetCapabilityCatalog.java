package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.catalog.MaterialWidgetPropertySchema;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Immutable fail-closed capability gate for the reviewed built-in widgets.
 *
 * <p>This is the single Java-side source for Palette admission, writable
 * Properties and native Canvas projection. A contributed or altered definition
 * cannot acquire a capability merely by reusing a built-in type id.</p>
 */
public final class BuiltInWidgetCapabilityCatalog {
    private static final Set<PropertyValueKind> CANVAS_VALUE_KINDS =
            Collections.unmodifiableSet(EnumSet.of(
                    PropertyValueKind.STRING,
                    PropertyValueKind.BOOLEAN,
                    PropertyValueKind.INTEGER,
                    PropertyValueKind.DOUBLE,
                    PropertyValueKind.ENUM,
                    PropertyValueKind.COLOR,
                    PropertyValueKind.EDGE_INSETS,
                    PropertyValueKind.ICON_DATA,
                    PropertyValueKind.THEME_TOKEN,
                    PropertyValueKind.PAINT,
                    PropertyValueKind.SHADOW_LIST,
                    PropertyValueKind.FONT_FEATURE_LIST,
                    PropertyValueKind.FONT_VARIATION_LIST,
                    PropertyValueKind.ALIGNMENT_GEOMETRY,
                    PropertyValueKind.OFFSET,
                    PropertyValueKind.POINTER_DEVICE_KIND_SET,
                    PropertyValueKind.SIZE,
                    PropertyValueKind.BOX_CONSTRAINTS,
                    PropertyValueKind.MATRIX4,
                    PropertyValueKind.IMAGE_PROVIDER,
                    PropertyValueKind.BORDER_RADIUS,
                    PropertyValueKind.SHAPE_BORDER_CLIPPER,
                    PropertyValueKind.DART_OBJECT_REFERENCE,
                    PropertyValueKind.BOX_DECORATION,
                    PropertyValueKind.NULL,
                    PropertyValueKind.CALLBACK));
    private static final Set<PropertyValueKind> NUMERIC_SCHEMA_KINDS =
            Collections.unmodifiableSet(EnumSet.of(
                    PropertyValueKind.INTEGER,
                    PropertyValueKind.DOUBLE,
                    PropertyValueKind.EDGE_INSETS));
    private static final String WIDGETS_LIBRARY = "package:flutter/widgets.dart";
    private static final String RENDERING_LIBRARY = "package:flutter/rendering.dart";
    private static final String MATERIAL_LIBRARY = "package:flutter/material.dart";
    private static final String GESTURES_LIBRARY = "package:flutter/gestures.dart";
    private static final String SERVICES_LIBRARY = "package:flutter/services.dart";
    private static final String DART_UI_LIBRARY = "dart:ui";
    private static final String IMAGE_PROVIDER_CONTRACT_FINGERPRINT =
            "imageProvider:v1:asset,exactAsset:package:exactScale:"
            + "resize(1..16384,exact,fit,allowUpscaling)";
    private static final String BORDER_RADIUS_CONTRACT_FINGERPRINT =
            "borderRadius:v1:physical,directional:finiteNonNegative";
    private static final String PHYSICAL_BORDER_RADIUS_CONTRACT_FINGERPRINT =
            "borderRadius:v1:physical:finiteNonNegative";
    private static final String DART_OBJECT_REFERENCE_CONTRACT_PREFIX =
            "dartObjectReference:v1:";
    private static final List<String> REVIEWED_COLOR_THEME_TOKENS = List.of(
            "material.colorScheme.primary",
            "material.colorScheme.onPrimary",
            "material.colorScheme.primaryContainer",
            "material.colorScheme.onPrimaryContainer",
            "material.colorScheme.primaryFixed",
            "material.colorScheme.primaryFixedDim",
            "material.colorScheme.onPrimaryFixed",
            "material.colorScheme.onPrimaryFixedVariant",
            "material.colorScheme.secondary",
            "material.colorScheme.onSecondary",
            "material.colorScheme.secondaryContainer",
            "material.colorScheme.onSecondaryContainer",
            "material.colorScheme.secondaryFixed",
            "material.colorScheme.secondaryFixedDim",
            "material.colorScheme.onSecondaryFixed",
            "material.colorScheme.onSecondaryFixedVariant",
            "material.colorScheme.tertiary",
            "material.colorScheme.onTertiary",
            "material.colorScheme.tertiaryContainer",
            "material.colorScheme.onTertiaryContainer",
            "material.colorScheme.tertiaryFixed",
            "material.colorScheme.tertiaryFixedDim",
            "material.colorScheme.onTertiaryFixed",
            "material.colorScheme.onTertiaryFixedVariant",
            "material.colorScheme.error",
            "material.colorScheme.onError",
            "material.colorScheme.errorContainer",
            "material.colorScheme.onErrorContainer",
            "material.colorScheme.surface",
            "material.colorScheme.onSurface",
            "material.colorScheme.surfaceDim",
            "material.colorScheme.surfaceBright",
            "material.colorScheme.surfaceContainerLowest",
            "material.colorScheme.surfaceContainerLow",
            "material.colorScheme.surfaceContainer",
            "material.colorScheme.surfaceContainerHigh",
            "material.colorScheme.surfaceContainerHighest",
            "material.colorScheme.onSurfaceVariant",
            "material.colorScheme.outline",
            "material.colorScheme.outlineVariant",
            "material.colorScheme.shadow",
            "material.colorScheme.scrim",
            "material.colorScheme.inverseSurface",
            "material.colorScheme.onInverseSurface",
            "material.colorScheme.inversePrimary",
            "material.colorScheme.surfaceTint");
    private static final List<String> REVIEWED_TEXT_THEME_TOKENS = List.of(
            "material.textTheme.displayLarge",
            "material.textTheme.displayMedium",
            "material.textTheme.displaySmall",
            "material.textTheme.headlineLarge",
            "material.textTheme.headlineMedium",
            "material.textTheme.headlineSmall",
            "material.textTheme.titleLarge",
            "material.textTheme.titleMedium",
            "material.textTheme.titleSmall",
            "material.textTheme.bodyLarge",
            "material.textTheme.bodyMedium",
            "material.textTheme.bodySmall",
            "material.textTheme.labelLarge",
            "material.textTheme.labelMedium",
            "material.textTheme.labelSmall");

    private static final Set<WidgetCapability> STATIC_EDITABLE = capabilities(
            WidgetCapability.PROPERTIES,
            WidgetCapability.CANVAS,
            WidgetCapability.CREATE,
            WidgetCapability.DND);
    private static final Set<WidgetCapability> STATIC_STRUCTURAL = capabilities(
            WidgetCapability.CANVAS,
            WidgetCapability.CREATE,
            WidgetCapability.DND);

    private static final Map<String, Set<WidgetCapability>> CAPABILITIES = Map.ofEntries(
            Map.entry("flutter.material.Scaffold", STATIC_EDITABLE),
            Map.entry("flutter.material.AppBar", STATIC_EDITABLE),
            Map.entry("flutter.material.FlexibleSpaceBar", STATIC_EDITABLE),
            Map.entry("flutter.material.FlexibleSpaceBarSettings", STATIC_EDITABLE),
            Map.entry("flutter.material.SliverAppBar", STATIC_EDITABLE),
            Map.entry("flutter.material.SliverAppBar.medium", STATIC_EDITABLE),
            Map.entry("flutter.material.SliverAppBar.large", STATIC_EDITABLE),
            Map.entry("flutter.material.ElevatedButton", STATIC_EDITABLE),
            Map.entry("flutter.material.TextButton", STATIC_EDITABLE),
            Map.entry("flutter.material.OutlinedButton", STATIC_EDITABLE),
            Map.entry("flutter.material.FilledButton", STATIC_EDITABLE),
            Map.entry("flutter.material.FloatingActionButton", STATIC_EDITABLE),
            Map.entry("flutter.material.IconButton", STATIC_EDITABLE),
            Map.entry("flutter.material.Checkbox", STATIC_EDITABLE),
            Map.entry("flutter.material.Radio", STATIC_EDITABLE),
            Map.entry("flutter.widgets.RadioGroup", STATIC_EDITABLE),
            Map.entry("flutter.material.ListTile", STATIC_EDITABLE),
            Map.entry("flutter.material.CheckboxListTile", STATIC_EDITABLE),
            Map.entry("flutter.material.SwitchListTile", STATIC_EDITABLE),
            Map.entry("flutter.material.RadioListTile", STATIC_EDITABLE),
            Map.entry("flutter.material.ExpansionTile", STATIC_EDITABLE),
            Map.entry("flutter.material.Tooltip", STATIC_EDITABLE),
            Map.entry("flutter.material.TooltipVisibility", STATIC_EDITABLE),
            Map.entry("flutter.material.TooltipTheme", STATIC_EDITABLE),
            Map.entry("flutter.material.MenuItemButton", STATIC_EDITABLE),
            Map.entry("flutter.material.MenuAnchor", STATIC_EDITABLE),
            Map.entry("flutter.material.SubmenuButton", STATIC_EDITABLE),
            Map.entry("flutter.material.MenuBar", STATIC_EDITABLE),
            Map.entry("flutter.material.NavigationBar", STATIC_EDITABLE),
            Map.entry("flutter.material.NavigationRail", STATIC_EDITABLE),
            Map.entry("flutter.material.NavigationDrawer", STATIC_EDITABLE),
            Map.entry("flutter.material.Drawer", STATIC_EDITABLE),
            Map.entry("flutter.material.BottomAppBar", STATIC_EDITABLE),
            Map.entry("flutter.material.BottomNavigationBar", STATIC_EDITABLE),
            Map.entry("flutter.material.Material", STATIC_EDITABLE),
            Map.entry("flutter.material.Scrollbar", STATIC_EDITABLE),
            Map.entry("flutter.material.Switch", STATIC_EDITABLE),
            Map.entry("flutter.material.Slider", STATIC_EDITABLE),
            Map.entry("flutter.material.RangeSlider", STATIC_EDITABLE),
            Map.entry("flutter.material.TextField", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Column", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Row", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Wrap", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Padding", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Center", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Text", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Icon", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SizedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AspectRatio", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Container", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Opacity", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Align", STATIC_EDITABLE),
            Map.entry("flutter.widgets.FractionallySizedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.FittedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ConstrainedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.UnconstrainedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.LimitedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.OverflowBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Stack", STATIC_EDITABLE),
            Map.entry("flutter.widgets.IndexedStack", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Expanded", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Flexible", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Spacer", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Baseline", STATIC_EDITABLE),
            Map.entry("flutter.widgets.IntrinsicHeight", STATIC_STRUCTURAL),
            Map.entry("flutter.widgets.IntrinsicWidth", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Offstage", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SizedOverflowBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Transform", STATIC_EDITABLE),
            Map.entry("flutter.widgets.RotatedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.PreferredSize", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Builder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ListBody", STATIC_EDITABLE),
            Map.entry("flutter.widgets.OverflowBar", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SafeArea", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ListView", STATIC_EDITABLE),
            Map.entry("flutter.widgets.GridView", STATIC_EDITABLE),
            Map.entry("flutter.widgets.GridView.extent", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SingleChildScrollView", STATIC_EDITABLE),
            Map.entry("flutter.widgets.PageView", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ListWheelScrollView", STATIC_EDITABLE),
            Map.entry("flutter.widgets.CustomScrollView", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverToBoxAdapter", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverMainAxisGroup", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverCrossAxisGroup", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverConstrainedCrossAxis", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverCrossAxisExpanded", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverList", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverGrid", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverGrid.extent", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverList.builder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverList.separated", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverList.delegate", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverGrid.builder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverGrid.list", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverGrid.delegate", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverIgnorePointer", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverOffstage", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverSafeArea", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedAlign", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedFractionallySizedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedPadding", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedSlide", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedScale", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedRotation", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedPositioned", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedPositioned.fromRect", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedPositionedDirectional", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedDefaultTextStyle", STATIC_EDITABLE),
            Map.entry("flutter.widgets.DefaultTextStyle", STATIC_EDITABLE),
            Map.entry("flutter.widgets.DefaultTextStyleTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.DefaultTextStyle.merge", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedPhysicalModel", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedSize", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedCrossFade", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedSwitcher", STATIC_EDITABLE),
            Map.entry("flutter.material.AnimatedTheme", STATIC_EDITABLE),
            Map.entry("flutter.material.Theme", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedContainer", STATIC_EDITABLE),
            Map.entry("flutter.widgets.RotationTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SizeTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.PositionedTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.RelativePositionedTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.DecoratedBoxTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AlignTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.MatrixTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ModalBarrier", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedModalBarrier", STATIC_EDITABLE),
            Map.entry("flutter.material.AnimatedIcon", STATIC_EDITABLE),
            Map.entry("flutter.widgets.FadeInImage", STATIC_EDITABLE),
            Map.entry("flutter.widgets.RawImage", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ColorFiltered", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ImageFiltered", STATIC_EDITABLE),
            Map.entry("flutter.widgets.BackdropFilter", STATIC_EDITABLE),
            Map.entry("flutter.widgets.BackdropFilter.grouped", STATIC_EDITABLE),
            Map.entry("flutter.widgets.BackdropGroup", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ScaleTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SlideTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.FadeTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverFadeTransition", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedOpacity", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverAnimatedOpacity", STATIC_EDITABLE),
            Map.entry("flutter.widgets.LayoutBuilder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.OrientationBuilder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.DeviceOrientationBuilder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ValueListenableBuilder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ValueListenableBuilder.sliver", STATIC_EDITABLE),
            Map.entry("flutter.widgets.TweenAnimationBuilder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.TweenAnimationBuilder.sliver", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedBuilder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AnimatedBuilder.sliver", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ListenableBuilder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ListenableBuilder.sliver", STATIC_EDITABLE),
            Map.entry("flutter.widgets.DeviceOrientationBuilder.sliver", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverLayoutBuilder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverPersistentHeader", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverFloatingHeader", STATIC_EDITABLE),
            Map.entry("flutter.widgets.PinnedHeaderSliver", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverResizingHeader", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverVisibility", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverVisibility.maintain", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverOpacity", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverPadding", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverFillRemaining", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverFillViewport", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverFillViewport.delegate", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverFixedExtentList", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverVariedExtentList", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverFixedExtentList.builder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverVariedExtentList.builder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverFixedExtentList.delegate", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverVariedExtentList.delegate", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverPrototypeExtentList", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverPrototypeExtentList.builder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.SliverPrototypeExtentList.delegate", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Image", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ImageIcon", STATIC_EDITABLE),
            Map.entry("flutter.material.Divider", STATIC_EDITABLE),
            Map.entry("flutter.material.VerticalDivider", STATIC_EDITABLE),
            Map.entry("flutter.material.Card", STATIC_EDITABLE),
            Map.entry("flutter.material.Badge", STATIC_EDITABLE),
            Map.entry("flutter.material.CircleAvatar", STATIC_EDITABLE),
            Map.entry("flutter.material.LinearProgressIndicator", STATIC_EDITABLE),
            Map.entry("flutter.material.CircularProgressIndicator", STATIC_EDITABLE),
            Map.entry("flutter.material.RefreshProgressIndicator", STATIC_EDITABLE),
            Map.entry("flutter.material.RefreshIndicator", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ColoredBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Placeholder", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Directionality", STATIC_EDITABLE),
            Map.entry("flutter.widgets.DecoratedBox", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ClipRect", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ClipOval", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ClipRRect", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ClipPath", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ClipRSuperellipse", STATIC_EDITABLE),
            Map.entry("flutter.widgets.PhysicalModel", STATIC_EDITABLE),
            Map.entry("flutter.widgets.PhysicalShape", STATIC_EDITABLE),
            Map.entry("flutter.widgets.RepaintBoundary", STATIC_STRUCTURAL),
            Map.entry("flutter.widgets.IgnorePointer", STATIC_EDITABLE),
            Map.entry("flutter.widgets.GestureDetector", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Listener", STATIC_EDITABLE),
            Map.entry("flutter.widgets.MouseRegion", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Focus", STATIC_EDITABLE),
            Map.entry("flutter.widgets.NotificationListener", STATIC_EDITABLE),
            Map.entry("flutter.widgets.AbsorbPointer", STATIC_EDITABLE),
            Map.entry("flutter.widgets.Visibility", STATIC_EDITABLE),
            Map.entry("flutter.widgets.TickerMode", STATIC_EDITABLE),
            Map.entry("flutter.widgets.DefaultTextHeightBehavior", STATIC_EDITABLE),
            Map.entry("flutter.widgets.DefaultSelectionStyle", STATIC_EDITABLE),
            Map.entry("flutter.widgets.IconTheme", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ExcludeSemantics", STATIC_EDITABLE),
            Map.entry("flutter.widgets.BlockSemantics", STATIC_EDITABLE),
            Map.entry("flutter.widgets.MergeSemantics", STATIC_STRUCTURAL),
            Map.entry("flutter.widgets.IndexedSemantics", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ExcludeFocus", STATIC_EDITABLE),
            Map.entry("flutter.widgets.ExcludeFocusTraversal", STATIC_EDITABLE));

    private static final CanvasNumericBounds UNBOUNDED_NUMERIC =
            bounds(null, true, null, true);
    private static final CanvasNumericBounds NON_NEGATIVE_NUMERIC =
            bounds(BigDecimal.ZERO, true, null, true);
    private static final CanvasNumericBounds POSITIVE_NUMERIC =
            bounds(BigDecimal.ZERO, false, null, true);
    private static final CanvasNumericBounds PERSPECTIVE_NUMERIC =
            bounds(BigDecimal.ZERO, false, BigDecimal.valueOf(0.01), true);
    private static final CanvasNumericBounds NON_NEGATIVE_PORTABLE_INTEGER =
            bounds(
                    BigDecimal.ZERO,
                    true,
                    new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER),
                    true);
    private static final CanvasNumericBounds POSITIVE_PORTABLE_INTEGER =
            bounds(
                    BigDecimal.ONE,
                    true,
                    new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER),
                    true);
    private static final CanvasNumericBounds SIGNED_PORTABLE_INTEGER =
            bounds(
                    new BigDecimal(DartNumericLiterals.MIN_PORTABLE_INTEGER),
                    true,
                    new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER),
                    true);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            NON_NEGATIVE_NUMBER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER, NON_NEGATIVE_PORTABLE_INTEGER,
                    PropertyValueKind.DOUBLE, NON_NEGATIVE_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            NON_NEGATIVE_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE, NON_NEGATIVE_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            POSITIVE_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE, POSITIVE_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            POSITIVE_NUMBER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER, POSITIVE_PORTABLE_INTEGER,
                    PropertyValueKind.DOUBLE, POSITIVE_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            PERSPECTIVE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE, PERSPECTIVE_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            ZERO_TO_ONE_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE,
                    bounds(BigDecimal.ZERO, true, BigDecimal.ONE, true));
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            MINUS_FOUR_TO_FOUR_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE,
                    bounds(BigDecimal.valueOf(-4), true,
                            BigDecimal.valueOf(4), true));
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            POSITIVE_FONT_AXIS_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE,
                    bounds(BigDecimal.ZERO, false,
                            BigDecimal.valueOf(32768), false));
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            GRADE_AXIS_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE,
                    bounds(BigDecimal.valueOf(-32768), true,
                            BigDecimal.valueOf(32768), false));
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            UNBOUNDED_DOUBLE_BOUNDS = Map.of(
                    PropertyValueKind.DOUBLE, UNBOUNDED_NUMERIC);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            POSITIVE_INTEGER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER, POSITIVE_PORTABLE_INTEGER);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            SIGNED_PORTABLE_INTEGER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER, SIGNED_PORTABLE_INTEGER);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            NON_NEGATIVE_INTEGER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER, NON_NEGATIVE_PORTABLE_INTEGER);
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            MAX_LENGTH_INTEGER_BOUNDS = Map.of(
                    PropertyValueKind.INTEGER,
                    bounds(
                            BigDecimal.valueOf(-1),
                            true,
                            new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER),
                            true));
    private static final Map<PropertyValueKind, CanvasNumericBounds>
            NON_NEGATIVE_EDGE_INSETS_BOUNDS = Map.of(
                    PropertyValueKind.EDGE_INSETS, NON_NEGATIVE_NUMERIC);

    /*
     * Independent Canvas-wire contract. This deliberate schema duplication is
     * the review gate between the extensible Java catalog and the isolated Dart
     * runner: every below-type property and slot field is explicit, so a bound,
     * required/default value or cardinality change fails until both sides are
     * reviewed together.
     */
    private static final Map<String, CanvasProjection> CANVAS_PROJECTIONS = Map.ofEntries(
            Map.entry("flutter.material.Scaffold", scaffoldProjection()),
            Map.entry("flutter.material.AppBar", appBarProjection()),
            Map.entry("flutter.material.FlexibleSpaceBarSettings", projection(
                    FlexibleSpaceBarSettingsWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.material.FlexibleSpaceBar", projection(
                    FlexibleSpaceBarWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("title", singleSlotSchema(false, 0), "background", singleSlotSchema(false, 0)))),
            Map.entry("flutter.material.SliverAppBar", sliverAppBarProjection()),
            Map.entry("flutter.material.SliverAppBar.medium", sliverAppBarProjection()),
            Map.entry("flutter.material.SliverAppBar.large", sliverAppBarProjection()),
            Map.entry("flutter.material.ElevatedButton", elevatedButtonProjection()),
            Map.entry("flutter.material.TextButton", fullStyleButtonProjection("TextButton")),
            Map.entry("flutter.material.OutlinedButton", fullStyleButtonProjection("OutlinedButton")),
            Map.entry("flutter.material.FilledButton", fullStyleButtonProjection("FilledButton")),
            Map.entry("flutter.material.FloatingActionButton", floatingActionButtonProjection()),
            Map.entry("flutter.material.IconButton", iconButtonProjection()),
            Map.entry("flutter.material.Checkbox", checkboxProjection()),
            Map.entry("flutter.material.Radio", radioProjection()),
            Map.entry("flutter.widgets.RadioGroup", radioGroupProjection()),
            Map.entry("flutter.material.ListTile", listTileProjection()),
            Map.entry("flutter.material.CheckboxListTile", checkboxListTileProjection()),
            Map.entry("flutter.material.SwitchListTile", switchListTileProjection()),
            Map.entry("flutter.material.RadioListTile", radioListTileProjection()),
            Map.entry("flutter.material.ExpansionTile", expansionTileProjection()),
            Map.entry("flutter.material.Tooltip", tooltipProjection()),
            Map.entry("flutter.material.TooltipTheme", tooltipThemeProjection()),
            Map.entry("flutter.material.MenuItemButton", menuItemButtonProjection()),
            Map.entry("flutter.material.MenuAnchor", menuAnchorProjection()),
            Map.entry("flutter.material.SubmenuButton", submenuButtonProjection()),
            Map.entry("flutter.material.MenuBar", menuBarProjection()),
            Map.entry("flutter.material.NavigationBar", navigationBarProjection()),
            Map.entry("flutter.material.NavigationRail", navigationRailProjection()),
            Map.entry("flutter.material.NavigationDrawer", navigationDrawerProjection()),
            Map.entry("flutter.material.Drawer", drawerProjection()),
            Map.entry("flutter.material.BottomAppBar", bottomAppBarProjection()),
            Map.entry("flutter.material.BottomNavigationBar", bottomNavigationBarProjection()),
            Map.entry("flutter.material.Material", materialProjection()),
            Map.entry("flutter.material.Scrollbar", scrollbarProjection()),
            Map.entry("flutter.material.TooltipVisibility", projection(Map.ofEntries(
                    requiredDefaultProperty("visible", "boolean:true", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.material.Switch", switchProjection()),
            Map.entry("flutter.material.Slider", sliderProjection()),
            Map.entry("flutter.material.RangeSlider", rangeSliderProjection()),
            Map.entry("flutter.material.TextField", textFieldProjection()),
            Map.entry("flutter.widgets.Column", flexProjection()),
            Map.entry("flutter.widgets.Row", flexProjection()),
            Map.entry("flutter.widgets.Wrap", wrapProjection()),
            Map.entry("flutter.widgets.Padding", projection(Map.of(
                    "padding", requiredDefaultEdgeInsetsSchema(
                            "edgeInsets:16,16,16,16",
                            true)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Center", projection(Map.of(
                    "widthFactor", numericSchema(
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    "heightFactor", numericSchema(
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.SizedBox", projection(Map.of(
                    "width", numericSchema(
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    "height", numericSchema(
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.AspectRatio", projection(Map.ofEntries(
                    requiredDefaultNumericProperty(
                            "aspectRatio",
                            "double:1",
                            POSITIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Opacity", projection(Map.ofEntries(
                    requiredDefaultNumericProperty(
                            "opacity",
                            "double:1",
                            ZERO_TO_ONE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    property("alwaysIncludeSemantics", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Align", projection(Map.ofEntries(
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    numericProperty(
                            "widthFactor",
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER,
                            PropertyValueKind.DOUBLE),
                    numericProperty(
                            "heightFactor",
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER,
                            PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.FractionallySizedBox", projection(Map.ofEntries(
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    numericProperty(
                            "widthFactor",
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER,
                            PropertyValueKind.DOUBLE),
                    numericProperty(
                            "heightFactor",
                            NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER,
                            PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.FittedBox", projection(Map.ofEntries(
                    enumProperty(
                            "fit", "BoxFit", "fill", "contain", "cover",
                            "fitWidth", "fitHeight", "none", "scaleDown"),
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    enumProperty(
                            "clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ConstrainedBox", projection(Map.ofEntries(
                    requiredDefaultConstrainedProperty(
                            "constraints",
                            "boxConstraints:0,inf,0,inf",
                            PropertyValueKind.BOX_CONSTRAINTS,
                            "boxConstraints:v2:finiteOrPositiveInfinity")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.UnconstrainedBox", projection(Map.ofEntries(
                    enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    enumProperty(
                            "constrainedAxis", "Axis", "horizontal", "vertical"),
                    enumProperty(
                            "clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.LimitedBox", projection(Map.of(
                    "maxWidth", numericSchema(
                            NON_NEGATIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    "maxHeight", numericSchema(
                            NON_NEGATIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.OverflowBox", projection(Map.ofEntries(
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    numericProperty(
                            "minWidth",
                            NON_NEGATIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    numericProperty(
                            "maxWidth",
                            NON_NEGATIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    numericProperty(
                            "minHeight",
                            NON_NEGATIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    numericProperty(
                            "maxHeight",
                            NON_NEGATIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    enumPropertyForLibrary(
                            "fit", RENDERING_LIBRARY,
                            "OverflowBoxFit", "max", "deferToChild")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Stack", projection(Map.ofEntries(
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                    enumProperty(
                            "fit", "StackFit", "loose", "expand", "passthrough"),
                    enumProperty(
                            "clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer")),
                    Map.of("children", listSlotSchema(false, 0, 10_000)))),
            Map.entry("flutter.widgets.Expanded", projection(Map.of(
                    "flex", numericSchema(
                            NON_NEGATIVE_INTEGER_BOUNDS,
                            PropertyValueKind.INTEGER)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.Flexible", projection(Map.ofEntries(
                    Map.entry("flex", numericSchema(
                            NON_NEGATIVE_INTEGER_BOUNDS,
                            PropertyValueKind.INTEGER)),
                    enumPropertyForLibrary(
                            "fit", RENDERING_LIBRARY,
                            "FlexFit", "loose", "tight")),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.Spacer", projection(Map.of(
                    "flex", numericSchema(
                            POSITIVE_INTEGER_BOUNDS,
                            PropertyValueKind.INTEGER)),
                    Map.of())),
            Map.entry("flutter.widgets.Baseline", projection(Map.ofEntries(
                    requiredDefaultNumericProperty(
                            "baseline",
                            "double:24",
                            UNBOUNDED_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    requiredDefaultConstrainedProperty(
                            "baselineType",
                            "enum:TextBaseline:alphabetic",
                            PropertyValueKind.ENUM,
                            "enum:" + base64(WIDGETS_LIBRARY)
                            + ":TextBaseline:alphabetic,ideographic")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.IntrinsicHeight", projection(
                    Map.of(),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.IntrinsicWidth", projection(Map.of(
                    "stepWidth", numericSchema(
                            NON_NEGATIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    "stepHeight", numericSchema(
                            NON_NEGATIVE_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Offstage", projection(Map.ofEntries(
                    property("offstage", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.SizedOverflowBox", projection(Map.ofEntries(
                    requiredDefaultConstrainedProperty(
                            "size",
                            "size:100,100",
                            PropertyValueKind.SIZE,
                            "size:finiteNonNegative"),
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry"))),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Transform", projection(Map.ofEntries(
                    requiredDefaultConstrainedProperty(
                            "transform",
                            "matrix4:1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1",
                            PropertyValueKind.MATRIX4,
                            "matrix4"),
                    Map.entry("origin", constrainedSchema(
                            PropertyValueKind.OFFSET,
                            "offset:finiteSigned")),
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    property("transformHitTests", PropertyValueKind.BOOLEAN),
                    enumProperty(
                            "filterQuality", "FilterQuality",
                            "none", "low", "medium", "high")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.RotatedBox", projection(Map.ofEntries(
                    requiredDefaultNumericProperty(
                            "quarterTurns",
                            "integer:1",
                            SIGNED_PORTABLE_INTEGER_BOUNDS,
                            PropertyValueKind.INTEGER)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.PreferredSize", projection(Map.ofEntries(
                    requiredDefaultConstrainedProperty(
                            "preferredSize",
                            "size:100,56",
                            PropertyValueKind.SIZE,
                            "size:finiteNonNegative")),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.Builder", projection(Map.ofEntries(
                    Map.entry("builder", new CanvasPropertyContract(
                            Set.of(PropertyValueKind.CALLBACK),
                            true,
                            Optional.of("string:bm9vcA"),
                            Map.of(),
                            Map.of(PropertyValueKind.CALLBACK, "callbackReference")))),
                    Map.of())),
            Map.entry("flutter.widgets.ListBody", projection(Map.ofEntries(
                    enumProperty("mainAxis", "Axis", "horizontal", "vertical"),
                    property("reverse", PropertyValueKind.BOOLEAN)),
                    Map.of("children", listSlotSchema(false, 0, 10_000)))),
            Map.entry("flutter.widgets.IndexedStack", projection(Map.ofEntries(
                    Map.entry("alignment", constrainedSchema(
                            PropertyValueKind.ALIGNMENT_GEOMETRY,
                            "alignmentGeometry")),
                    enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                    enumProperty(
                            "clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer"),
                    enumProperty(
                            "sizing", "StackFit", "loose", "expand", "passthrough"),
                    Map.entry("index", new CanvasPropertyContract(
                            Set.of(PropertyValueKind.INTEGER, PropertyValueKind.NULL),
                            false,
                            Optional.empty(),
                            Map.of(PropertyValueKind.INTEGER,
                                    NON_NEGATIVE_PORTABLE_INTEGER),
                            Map.of(
                                    PropertyValueKind.INTEGER,
                                    "range:" + NON_NEGATIVE_PORTABLE_INTEGER.fingerprint(),
                                    PropertyValueKind.NULL,
                                    "any")))),
                    Map.of("children", listSlotSchema(false, 0, 10_000)))),
            Map.entry("flutter.widgets.OverflowBar", projection(Map.ofEntries(
                    numericProperty(
                            "spacing", UNBOUNDED_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    enumProperty(
                            "alignment", "MainAxisAlignment",
                            "start", "end", "center", "spaceBetween",
                            "spaceAround", "spaceEvenly"),
                    numericProperty(
                            "overflowSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                            PropertyValueKind.DOUBLE),
                    enumProperty(
                            "overflowAlignment", "OverflowBarAlignment",
                            "start", "end", "center"),
                    enumProperty(
                            "overflowDirection", "VerticalDirection", "up", "down"),
                    enumProperty("textDirection", "TextDirection", "rtl", "ltr")),
                    Map.of("children", listSlotSchema(false, 0, 10_000)))),
            Map.entry("flutter.widgets.SafeArea", projection(Map.ofEntries(
                    property("left", PropertyValueKind.BOOLEAN),
                    property("top", PropertyValueKind.BOOLEAN),
                    property("right", PropertyValueKind.BOOLEAN),
                    property("bottom", PropertyValueKind.BOOLEAN),
                    Map.entry("minimum", physicalEdgeInsetsSchema(false)),
                    property("maintainBottomViewPadding", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.ListView", listViewProjection()),
            Map.entry("flutter.widgets.GridView", gridViewCountProjection()),
            Map.entry("flutter.widgets.GridView.extent", gridViewExtentProjection()),
            Map.entry("flutter.widgets.SingleChildScrollView",
                    singleChildScrollViewProjection()),
            Map.entry("flutter.widgets.PageView", pageViewProjection()),
            Map.entry("flutter.widgets.ListWheelScrollView", listWheelScrollViewProjection()),
            Map.entry("flutter.widgets.CustomScrollView", customScrollViewProjection()),
            Map.entry("flutter.widgets.SliverMainAxisGroup", projection(Map.of(),
                    Map.of("slivers", traitListSlotSchema(true, 0, 10_000, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverCrossAxisGroup", projection(Map.of(),
                    Map.of("slivers", traitListSlotSchema(true, 0, 10_000, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverList", sliverChildrenProjection(true, false)),
            Map.entry("flutter.widgets.SliverGrid", sliverChildrenProjection(false, false)),
            Map.entry("flutter.widgets.SliverGrid.extent", sliverChildrenProjection(false, true)),
            Map.entry("flutter.widgets.SliverFillViewport", sliverDynamicProjection("flutter.widgets.SliverFillViewport")),
            Map.entry("flutter.widgets.SliverFillViewport.delegate", sliverDynamicProjection("flutter.widgets.SliverFillViewport.delegate")),
            Map.entry("flutter.widgets.SliverFixedExtentList", sliverDynamicProjection("flutter.widgets.SliverFixedExtentList")),
            Map.entry("flutter.widgets.SliverVariedExtentList", sliverDynamicProjection("flutter.widgets.SliverVariedExtentList")),
            Map.entry("flutter.widgets.SliverFixedExtentList.builder", sliverDynamicProjection("flutter.widgets.SliverFixedExtentList.builder")),
            Map.entry("flutter.widgets.SliverVariedExtentList.builder", sliverDynamicProjection("flutter.widgets.SliverVariedExtentList.builder")),
            Map.entry("flutter.widgets.SliverFixedExtentList.delegate", sliverDynamicProjection("flutter.widgets.SliverFixedExtentList.delegate")),
            Map.entry("flutter.widgets.SliverVariedExtentList.delegate", sliverDynamicProjection("flutter.widgets.SliverVariedExtentList.delegate")),
            Map.entry("flutter.widgets.SliverPrototypeExtentList", sliverPrototypeProjection("flutter.widgets.SliverPrototypeExtentList")),
            Map.entry("flutter.widgets.SliverPrototypeExtentList.builder", sliverPrototypeProjection("flutter.widgets.SliverPrototypeExtentList.builder")),
            Map.entry("flutter.widgets.SliverPrototypeExtentList.delegate", sliverPrototypeProjection("flutter.widgets.SliverPrototypeExtentList.delegate")),
            Map.entry("flutter.widgets.SliverList.builder", sliverDynamicProjection("flutter.widgets.SliverList.builder")),
            Map.entry("flutter.widgets.SliverList.separated", sliverDynamicProjection("flutter.widgets.SliverList.separated")),
            Map.entry("flutter.widgets.SliverList.delegate", sliverDynamicProjection("flutter.widgets.SliverList.delegate")),
            Map.entry("flutter.widgets.SliverGrid.builder", sliverDynamicProjection("flutter.widgets.SliverGrid.builder")),
            Map.entry("flutter.widgets.SliverGrid.list", sliverDynamicProjection("flutter.widgets.SliverGrid.list")),
            Map.entry("flutter.widgets.SliverGrid.delegate", sliverDynamicProjection("flutter.widgets.SliverGrid.delegate")),
            Map.entry("flutter.widgets.SliverConstrainedCrossAxis", projection(
                    Map.of("maxExtent", propertyContract(SliverConstrainedCrossAxisWidgetPropertySchema.maxExtent())),
                    Map.of("sliver", traitSingleSlotSchema(true, 1, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverCrossAxisExpanded", projection(
                    Map.of("flex", propertyContract(SliverCrossAxisExpandedWidgetPropertySchema.flex())),
                    Map.of("sliver", traitSingleSlotSchema(true, 1, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverIgnorePointer", projection(
                    Map.of("ignoring", propertyContract(SliverIgnorePointerWidgetPropertySchema.properties().getFirst()),
                            "ignoringSemantics", propertyContract(SliverIgnorePointerWidgetPropertySchema.properties().get(1))),
                    Map.of("sliver", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverVisibility", projection(
                    SliverVisibilityWidgetPropertySchema.properties(false).stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("sliver", traitSingleSlotSchema(true, 1, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT),
                            "replacementSliver", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverVisibility.maintain", projection(
                    Map.of("visible", propertyContract(SliverVisibilityWidgetPropertySchema.properties(true).getFirst())),
                    Map.of("sliver", traitSingleSlotSchema(true, 1, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT),
                            "replacementSliver", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverFloatingHeader", projection(
                    SliverFloatingHeaderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.PinnedHeaderSliver", projection(Map.of(),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.SliverResizingHeader", projection(Map.of(),
                    Map.of("minExtentPrototype", singleSlotSchema(false, 0),
                            "maxExtentPrototype", singleSlotSchema(false, 0), "child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.SliverPersistentHeader", projection(
                    SliverPersistentHeaderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)), Map.of())),
            Map.entry("flutter.widgets.LayoutBuilder", projection(
                    Map.of("builder", propertyContract(LayoutBuilderWidgetPropertySchema.properties().getFirst())),
                    Map.of())),
            Map.entry("flutter.widgets.OrientationBuilder", projection(
                    Map.of("builder", propertyContract(OrientationBuilderWidgetPropertySchema.properties().getFirst())),
                    Map.of())),
            Map.entry("flutter.widgets.ValueListenableBuilder", projection(
                    ValueListenableBuilderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ValueListenableBuilder.sliver", projection(
                    ValueListenableBuilderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.TweenAnimationBuilder", projection(
                    TweenAnimationBuilderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.TweenAnimationBuilder.sliver", projection(
                    TweenAnimationBuilderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.AnimatedBuilder", projection(
                    AnimatedBuilderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.AnimatedBuilder.sliver", projection(
                    AnimatedBuilderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.ListenableBuilder", projection(
                    ListenableBuilderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ListenableBuilder.sliver", projection(
                    ListenableBuilderWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.DeviceOrientationBuilder", projection(
                    Map.of("builder", propertyContract(DeviceOrientationBuilderWidgetPropertySchema.properties().getFirst())),
                    Map.of())),
            Map.entry("flutter.widgets.DeviceOrientationBuilder.sliver", projection(
                    Map.of("builder", propertyContract(DeviceOrientationBuilderWidgetPropertySchema.properties().getFirst())),
                    Map.of())),
            Map.entry("flutter.widgets.SliverLayoutBuilder", projection(
                    Map.of("builder", propertyContract(SliverLayoutBuilderWidgetPropertySchema.properties().getFirst())),
                    Map.of())),
            Map.entry("flutter.widgets.AnimatedSlide", projection(
                    AnimatedSlideWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.AnimatedScale", projection(
                    AnimatedScaleWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),            Map.entry("flutter.widgets.AnimatedContainer", projection(
                    AnimatedContainerWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry(AnimatedPositionedWidgetPropertySchema.TYPE.value(), projection(
                    AnimatedPositionedWidgetPropertySchema.properties(AnimatedPositionedWidgetPropertySchema.TYPE).stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry(AnimatedPositionedWidgetPropertySchema.RECT_TYPE.value(), projection(
                    AnimatedPositionedWidgetPropertySchema.properties(AnimatedPositionedWidgetPropertySchema.RECT_TYPE).stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry(AnimatedPositionedWidgetPropertySchema.DIRECTIONAL_TYPE.value(), projection(
                    AnimatedPositionedWidgetPropertySchema.properties(AnimatedPositionedWidgetPropertySchema.DIRECTIONAL_TYPE).stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.AnimatedPhysicalModel", projection(
                    AnimatedPhysicalModelWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.DefaultTextStyleTransition", projection(
                    DefaultTextStyleTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.DefaultTextStyle", projection(
                    DefaultTextStyleWidgetPropertySchema.properties(DefaultTextStyleWidgetPropertySchema.TYPE).stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.DefaultTextStyle.merge", projection(
                    DefaultTextStyleWidgetPropertySchema.properties(DefaultTextStyleWidgetPropertySchema.MERGE_TYPE).stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.AnimatedDefaultTextStyle", projection(
                    AnimatedDefaultTextStyleWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.material.Theme", projection(
                    ThemeWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.material.AnimatedTheme", projection(
                    AnimatedThemeWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.AnimatedSwitcher", projection(
                    AnimatedSwitcherWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.AnimatedCrossFade", projection(
                    AnimatedCrossFadeWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("firstChild", singleSlotSchema(true, 1), "secondChild", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.AnimatedSize", projection(
                    AnimatedSizeWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.AnimatedRotation", projection(
                    AnimatedRotationWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.AnimatedPadding", projection(
                    AnimatedPaddingWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.AnimatedAlign", projection(
                    AnimatedAlignWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.AnimatedFractionallySizedBox", projection(
                    AnimatedFractionallySizedBoxWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.RotationTransition", projection(
                    RotationTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.PositionedTransition", projection(
                    PositionedTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.RelativePositionedTransition", projection(
                    RelativePositionedTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.AlignTransition", projection(
                    AlignTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.BackdropFilter", projection(
                    BackdropFilterWidgetPropertySchema.properties(new WidgetTypeId("flutter.widgets.BackdropFilter")).stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)), Map.of("child", singleSlotSchema(false,0)))),
            Map.entry("flutter.widgets.BackdropFilter.grouped", projection(
                    BackdropFilterWidgetPropertySchema.properties(new WidgetTypeId("flutter.widgets.BackdropFilter.grouped")).stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)), Map.of("child", singleSlotSchema(false,0)))),
            Map.entry("flutter.widgets.BackdropGroup", projection(
                    BackdropFilterWidgetPropertySchema.properties(new WidgetTypeId("flutter.widgets.BackdropGroup")).stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)), Map.of("child", singleSlotSchema(true,1)))),
            Map.entry("flutter.widgets.ImageFiltered", projection(
                    ImageFilteredWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)), Map.of("child", singleSlotSchema(false,0)))),
            Map.entry("flutter.widgets.ColorFiltered", projection(
                    ColorFilteredWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)), Map.of("child", singleSlotSchema(false,0)))),
            Map.entry("flutter.widgets.RawImage", projection(
                    RawImageWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)), Map.of())),
            Map.entry("flutter.widgets.FadeInImage", projection(
                    FadeInImageWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),Map.of())),
            Map.entry("flutter.material.AnimatedIcon", projection(
                    AnimatedIconWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),Map.of())),
            Map.entry("flutter.widgets.AnimatedModalBarrier", projection(
                    AnimatedModalBarrierWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)), Map.of())),
            Map.entry("flutter.widgets.ModalBarrier", projection(
                    ModalBarrierWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)), Map.of())),
            Map.entry("flutter.widgets.MatrixTransition", projection(
                    MatrixTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.DecoratedBoxTransition", projection(
                    DecoratedBoxTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.SizeTransition", projection(
                    SizeTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ScaleTransition", projection(
                    ScaleTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.SlideTransition", projection(
                    SlideTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.FadeTransition", projection(
                    FadeTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.SliverFadeTransition", projection(
                    FadeTransitionWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("sliver", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.AnimatedOpacity", projection(
                    AnimatedOpacityWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.SliverAnimatedOpacity", projection(
                    SliverAnimatedOpacityWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("sliver", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverSafeArea", projection(
                    SliverSafeAreaWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                            p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                    Map.of("sliver", traitSingleSlotSchema(true, 1, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverOffstage", projection(
                    Map.of("offstage", propertyContract(SliverOffstageWidgetPropertySchema.properties().getFirst())),
                    Map.of("sliver", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverOpacity", projection(
                    Map.of("opacity", propertyContract(SliverOpacityWidgetPropertySchema.opacity()),
                            "alwaysIncludeSemantics", propertyContract(SliverOpacityWidgetPropertySchema.properties().get(1))),
                    Map.of("sliver", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverPadding", projection(
                    Map.of("padding", propertyContract(SliverPaddingWidgetPropertySchema.padding())),
                    Map.of("sliver", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)))),
            Map.entry("flutter.widgets.SliverFillRemaining", projection(
                    Map.ofEntries(property("hasScrollBody", PropertyValueKind.BOOLEAN),
                            property("fillOverscroll", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.SliverToBoxAdapter", projection(
                    Map.of(), Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Image", imageProjection()),
            Map.entry("flutter.material.Card", cardProjection()),
            Map.entry("flutter.material.Badge", badgeProjection()),
            Map.entry("flutter.material.CircleAvatar", circleAvatarProjection()),
            Map.entry("flutter.material.LinearProgressIndicator", linearProgressIndicatorProjection()),
            Map.entry("flutter.material.CircularProgressIndicator", circularProgressIndicatorProjection()),
            Map.entry("flutter.material.RefreshProgressIndicator", refreshProgressIndicatorProjection()),
            Map.entry("flutter.material.RefreshIndicator", refreshIndicatorProjection()),
            Map.entry("flutter.material.Divider", projection(Map.ofEntries(
                    numericProperty("height", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numericProperty("thickness", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numericProperty("indent", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numericProperty("endIndent", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    colorOrThemeProperty("color"),
                    Map.entry("radius", constrainedSchema(PropertyValueKind.BORDER_RADIUS, BORDER_RADIUS_CONTRACT_FINGERPRINT))), Map.of())),
            Map.entry("flutter.material.VerticalDivider", projection(Map.ofEntries(
                    numericProperty("width", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numericProperty("thickness", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numericProperty("indent", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numericProperty("endIndent", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    colorOrThemeProperty("color"),
                    Map.entry("radius", constrainedSchema(PropertyValueKind.BORDER_RADIUS, BORDER_RADIUS_CONTRACT_FINGERPRINT))), Map.of())),
            Map.entry("flutter.widgets.ImageIcon", projection(Map.ofEntries(
                    Map.entry("image", new CanvasPropertyContract(
                            Set.of(PropertyValueKind.IMAGE_PROVIDER, PropertyValueKind.NULL),
                            true, Optional.of("null"), Map.of(),
                            Map.of(PropertyValueKind.IMAGE_PROVIDER, IMAGE_PROVIDER_CONTRACT_FINGERPRINT,
                                    PropertyValueKind.NULL, "any"))),
                    numericProperty("size", NON_NEGATIVE_NUMBER_BOUNDS,
                            PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    colorOrThemeProperty("color"),
                    property("semanticLabel", PropertyValueKind.STRING)), Map.of())),
            Map.entry("flutter.widgets.ColoredBox", coloredBoxProjection()),
            Map.entry("flutter.widgets.PhysicalModel", physicalModelProjection()),
            Map.entry("flutter.widgets.PhysicalShape", physicalShapeProjection()),
            Map.entry("flutter.widgets.RepaintBoundary", projection(
                    Map.of(), Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.GestureDetector", gestureDetectorProjection()),
            Map.entry("flutter.widgets.Listener", listenerProjection()),
            Map.entry("flutter.widgets.MouseRegion", mouseRegionProjection()),
            Map.entry("flutter.widgets.Focus", focusProjection()),
            Map.entry("flutter.widgets.NotificationListener", notificationListenerProjection()),
            Map.entry("flutter.widgets.IgnorePointer", projection(Map.ofEntries(
                    property("ignoring", PropertyValueKind.BOOLEAN),
                    property("ignoringSemantics", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.AbsorbPointer", projection(Map.ofEntries(
                    property("absorbing", PropertyValueKind.BOOLEAN),
                    property("ignoringSemantics", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.Placeholder", placeholderProjection()),
            Map.entry("flutter.widgets.Directionality", projection(Map.ofEntries(
                    requiredDefaultConstrainedProperty(
                            "textDirection",
                            "enum:TextDirection:ltr",
                            PropertyValueKind.ENUM,
                            "enum:" + base64(WIDGETS_LIBRARY)
                            + ":TextDirection:ltr,rtl")),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.DecoratedBox", projection(Map.ofEntries(
                    requiredDefaultConstrainedProperty(
                            "decoration",
                            "boxDecoration:empty",
                            PropertyValueKind.BOX_DECORATION,
                            boxDecorationFingerprint(REVIEWED_COLOR_THEME_TOKENS)),
                    enumPropertyForLibrary(
                            "position",
                            RENDERING_LIBRARY,
                            "DecorationPosition",
                            "background",
                            "foreground")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ClipRect", projection(Map.ofEntries(
                    Map.entry("clipper", constrainedSchema(
                            PropertyValueKind.DART_OBJECT_REFERENCE,
                            DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "CustomClipper<Rect>:currentOrPackage:"
                            + "root,optionalMember:reference,"
                            + "zeroArgumentInvocation:requiredConstnessBoolean(false,true)")),
                    enumProperty(
                            "clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ClipOval", projection(Map.ofEntries(
                    Map.entry("clipper", constrainedSchema(
                            PropertyValueKind.DART_OBJECT_REFERENCE,
                            DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "CustomClipper<Rect>:currentOrPackage:"
                            + "root,optionalMember:reference,"
                            + "zeroArgumentInvocation:requiredConstnessBoolean(false,true)")),
                    enumProperty(
                            "clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ClipRRect", projection(Map.ofEntries(
                    Map.entry("borderRadius", constrainedSchema(
                            PropertyValueKind.BORDER_RADIUS,
                            BORDER_RADIUS_CONTRACT_FINGERPRINT)),
                    Map.entry("clipper", constrainedSchema(
                            PropertyValueKind.DART_OBJECT_REFERENCE,
                            DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "CustomClipper<RRect>:currentOrPackage:"
                            + "root,optionalMember:reference,"
                            + "zeroArgumentInvocation:requiredConstnessBoolean(false,true)")),
                    enumProperty(
                            "clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ClipRSuperellipse", projection(Map.ofEntries(
                    Map.entry("borderRadius", constrainedSchema(
                            PropertyValueKind.BORDER_RADIUS,
                            BORDER_RADIUS_CONTRACT_FINGERPRINT)),
                    Map.entry("clipper", constrainedSchema(
                            PropertyValueKind.DART_OBJECT_REFERENCE,
                            DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "CustomClipper<RSuperellipse>:currentOrPackage:"
                            + "root,optionalMember:reference,"
                            + "zeroArgumentInvocation:requiredConstnessBoolean(false,true)")),
                    enumProperty(
                            "clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ClipPath", projection(Map.ofEntries(
                    Map.entry("clipper", constrainedSchema(
                            PropertyValueKind.DART_OBJECT_REFERENCE,
                            DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "CustomClipper<Path>:currentOrPackage:"
                            + "root,optionalMember:reference,"
                            + "zeroArgumentInvocation:requiredConstnessBoolean(false,true)")),
                    Map.entry("shape", constrainedSchema(
                            PropertyValueKind.DART_OBJECT_REFERENCE,
                            DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "ShapeBorder:currentOrPackage:"
                            + "root,optionalMember:reference,"
                            + "zeroArgumentInvocation:requiredConstnessBoolean(false,true)")),
                    enumProperty("clipBehavior", "Clip", "none", "hardEdge",
                            "antiAlias", "antiAliasWithSaveLayer")),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ExcludeSemantics", projection(Map.ofEntries(
                    property("excluding", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.BlockSemantics", projection(Map.ofEntries(
                    property("blocking", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.MergeSemantics", projection(
                    Map.of(), Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.IndexedSemantics", projection(Map.ofEntries(
                    requiredDefaultNumericProperty("index", "integer:0",
                            SIGNED_PORTABLE_INTEGER_BOUNDS, PropertyValueKind.INTEGER)),
                    Map.of("child", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.ExcludeFocus", projection(Map.ofEntries(
                    property("excluding", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.ExcludeFocusTraversal", projection(Map.ofEntries(
                    property("excluding", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.Visibility", projection(Map.ofEntries(
                    property("visible", PropertyValueKind.BOOLEAN),
                    property("maintainState", PropertyValueKind.BOOLEAN),
                    property("maintainAnimation", PropertyValueKind.BOOLEAN),
                    property("maintainSize", PropertyValueKind.BOOLEAN),
                    property("maintainSemantics", PropertyValueKind.BOOLEAN),
                    property("maintainInteractivity", PropertyValueKind.BOOLEAN),
                    property("maintainFocusability", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(true, 1),
                            "replacement", singleSlotSchema(false, 0)))),
            Map.entry("flutter.widgets.TickerMode", projection(Map.ofEntries(
                    requiredDefaultProperty("enabled", "boolean:true", PropertyValueKind.BOOLEAN),
                    property("forceFrames", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.DefaultTextHeightBehavior", projection(Map.ofEntries(
                    property("textHeightApplyFirstAscent", PropertyValueKind.BOOLEAN),
                    property("textHeightApplyLastDescent", PropertyValueKind.BOOLEAN),
                    enumProperty("textHeightLeadingDistribution", "TextLeadingDistribution", "proportional", "even")),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.DefaultSelectionStyle", projection(Map.ofEntries(
                    colorOrThemeProperty("cursorColor"),
                    colorOrThemeProperty("selectionColor"),
                    stringPatternProperty("mouseCursor", DefaultSelectionStyleWidgetPropertySchema.mouseCursorPattern()),
                    requiredDefaultProperty("merge", "boolean:false", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.IconTheme", projection(Map.ofEntries(
                    numericProperty("size", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numericProperty("fill", ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE),
                    numericProperty("weight", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE),
                    numericProperty("grade", GRADE_AXIS_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE),
                    numericProperty("opticalSize", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE),
                    colorOrThemeProperty("color"),
                    numericProperty("opacity", UNBOUNDED_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE),
                    shadowProperty("shadows"),
                    property("applyTextScaling", PropertyValueKind.BOOLEAN),
                    requiredDefaultProperty("merge", "boolean:false", PropertyValueKind.BOOLEAN)),
                    Map.of("child", singleSlotSchema(true, 1)))),
            Map.entry("flutter.widgets.Container", containerProjection()),
            Map.entry("flutter.widgets.Icon", iconProjection()),
            Map.entry("flutter.widgets.Text", textProjection()));

    static {
        validateCatalogParity();
    }

    private BuiltInWidgetCapabilityCatalog() {
    }

    /** Returns the reviewed capabilities of one exact canonical definition. */
    public static Set<WidgetCapability> capabilities(WidgetDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (!isExactBuiltInDefinition(definition)) {
            return Set.of();
        }
        return CAPABILITIES.getOrDefault(definition.typeId().value(), Set.of());
    }

    /** Returns whether one exact canonical definition has the requested capability. */
    public static boolean supports(
            WidgetDefinition definition,
            WidgetCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return capabilities(definition).contains(capability);
    }

    /** Returns canonical built-ins carrying one capability in deterministic Palette order. */
    public static List<WidgetDefinition> definitionsSupporting(WidgetCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return BuiltInWidgetCatalog.getDefault().paletteDefinitions().stream()
                .filter(definition -> supports(definition, capability))
                .toList();
    }

    /**
     * Returns the independent reviewed Canvas-wire schema for one exact
     * canonical definition. The explicit schema is intentionally separate from
     * the extensible catalog and is parity-checked during class initialization.
     */
    public static Optional<CanvasProjection> canvasProjection(
            WidgetDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (!supports(definition, WidgetCapability.CANVAS)) {
            return Optional.empty();
        }
        return Optional.of(CANVAS_PROJECTIONS.get(definition.typeId().value()));
    }

    private static boolean isExactBuiltInDefinition(WidgetDefinition definition) {
        return BuiltInWidgetCatalog.getDefault().find(definition.typeId())
                .filter(definition::equals)
                .isPresent();
    }

    private static Set<WidgetCapability> capabilities(WidgetCapability... values) {
        return Set.of(values);
    }

    private static void validateCatalogParity() {
        Set<String> catalogTypes = BuiltInWidgetCatalog.getDefault().definitions().stream()
                .map(definition -> definition.typeId().value())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!CAPABILITIES.keySet().equals(catalogTypes)) {
            TreeSet<String> missing = new TreeSet<>(catalogTypes);
            missing.removeAll(CAPABILITIES.keySet());
            TreeSet<String> unknown = new TreeSet<>(CAPABILITIES.keySet());
            unknown.removeAll(catalogTypes);
            throw new ExceptionInInitializerError(
                    "Built-in widget capability parity failed; missing=" + missing
                    + ", unknown=" + unknown);
        }
        for (WidgetDefinition definition : BuiltInWidgetCatalog.getDefault().definitions()) {
            Set<WidgetCapability> supported = CAPABILITIES.get(
                    definition.typeId().value());
            requireDependencies(definition, supported);
            validateCanvasProjection(definition, supported);
        }
    }

    private static void requireDependencies(
            WidgetDefinition definition,
            Set<WidgetCapability> supported) {
        EnumSet<WidgetCapability> required = EnumSet.noneOf(WidgetCapability.class);
        if (supported.contains(WidgetCapability.CREATE)) {
            required.add(WidgetCapability.CANVAS);
        }
        if (supported.contains(WidgetCapability.DND)) {
            required.add(WidgetCapability.CREATE);
            required.add(WidgetCapability.CANVAS);
        }
        if (!supported.containsAll(required)) {
            required.removeAll(supported);
            throw new ExceptionInInitializerError(
                    "Widget capability dependencies are missing for "
                    + definition.typeId().value() + ": " + required);
        }
    }

    private static void validateCanvasProjection(
            WidgetDefinition definition,
            Set<WidgetCapability> supported) {
        CanvasProjection declared = CANVAS_PROJECTIONS.get(
                definition.typeId().value());
        if (!supported.contains(WidgetCapability.CANVAS)) {
            if (declared != null) {
                throw new ExceptionInInitializerError(
                        "Canvas schema exists without Canvas capability for "
                        + definition.typeId().value());
            }
            return;
        }
        if (declared == null) {
            throw new ExceptionInInitializerError(
                    "Canvas capability has no reviewed schema for "
                    + definition.typeId().value());
        }
        requireCanvasSchemaParity(definition, declared);
    }

    static void requireCanvasSchemaParity(
            WidgetDefinition definition,
            CanvasProjection declared) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(declared, "declared");
        CanvasProjection catalogProjection = catalogProjection(definition);
        if (!declared.equals(catalogProjection)) {
            throw new ExceptionInInitializerError(
                    "Canvas schema parity failed for " + definition.typeId().value()
                    + "; declared=" + declared + ", catalog=" + catalogProjection);
        }
    }

    private static CanvasProjection catalogProjection(WidgetDefinition definition) {
        LinkedHashMap<PropertyName, CanvasPropertyContract> catalogProperties =
                new LinkedHashMap<>();
        for (PropertyDefinition property : definition.properties()) {
            if (!CANVAS_VALUE_KINDS.containsAll(property.acceptedKinds())) {
                throw new ExceptionInInitializerError(
                        "Canvas schema contains an unsupported value kind on "
                        + definition.typeId().value() + '.' + property.name().value());
            }
            catalogProperties.put(property.name(), propertyContract(property));
        }
        LinkedHashMap<SlotName, CanvasSlotContract> catalogSlots =
                new LinkedHashMap<>();
        for (SlotDefinition slot : definition.slots()) {
            catalogSlots.put(slot.name(), new CanvasSlotContract(
                    slot.cardinality(),
                    slot.parameter().required(),
                    slot.minChildren(),
                    slot.maxChildren(),
                    slotAcceptanceFingerprint(slot.acceptance())));
        }
        return CanvasProjection.of(catalogProperties, catalogSlots);
    }

    private static CanvasProjection flexProjection() {
        return projection(Map.ofEntries(
                enumProperty(
                        "mainAxisAlignment", "MainAxisAlignment",
                        "start", "end", "center", "spaceBetween",
                        "spaceAround", "spaceEvenly"),
                enumProperty("mainAxisSize", "MainAxisSize", "min", "max"),
                enumProperty(
                        "crossAxisAlignment", "CrossAxisAlignment",
                        "start", "end", "center", "stretch", "baseline"),
                enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                enumProperty("verticalDirection", "VerticalDirection", "up", "down"),
                enumProperty(
                        "textBaseline", "TextBaseline",
                        "alphabetic", "ideographic"),
                numericProperty(
                        "spacing", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE)),
                Map.of("children", listSlotSchema(false, 0, 10_000)));
    }

    private static CanvasProjection wrapProjection() {
        return projection(Map.ofEntries(
                enumProperty("direction", "Axis", "horizontal", "vertical"),
                enumProperty(
                        "alignment", "WrapAlignment",
                        "start", "end", "center", "spaceBetween",
                        "spaceAround", "spaceEvenly"),
                numericProperty(
                        "spacing", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "runAlignment", "WrapAlignment",
                        "start", "end", "center", "spaceBetween",
                        "spaceAround", "spaceEvenly"),
                numericProperty(
                        "runSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "crossAxisAlignment", "WrapCrossAlignment",
                        "start", "end", "center"),
                enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                enumProperty("verticalDirection", "VerticalDirection", "up", "down"),
                enumProperty(
                        "clipBehavior", "Clip", "none", "hardEdge",
                        "antiAlias", "antiAliasWithSaveLayer")),
                Map.of("children", listSlotSchema(false, 0, 10_000)));
    }

    private static CanvasProjection listViewProjection() {
        return projection(Map.ofEntries(
                enumProperty("scrollDirection", "Axis", "horizontal", "vertical"),
                property("reverse", PropertyValueKind.BOOLEAN),
                property("primary", PropertyValueKind.BOOLEAN),
                stringPatternProperty(
                        "physics",
                        "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)"),
                property("shrinkWrap", PropertyValueKind.BOOLEAN),
                edgeInsetsProperty("padding", true),
                numericProperty(
                        "itemExtent", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                Map.entry("itemExtentBuilder", new CanvasPropertyContract(
                        Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL), false,
                        Optional.empty(), Map.of(), Map.of(
                                PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("ItemExtentBuilder?"),
                                PropertyValueKind.NULL, "any"))),
                property("addAutomaticKeepAlives", PropertyValueKind.BOOLEAN),
                property("addRepaintBoundaries", PropertyValueKind.BOOLEAN),
                property("addSemanticIndexes", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "scrollCacheExtent", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "semanticChildCount", NON_NEGATIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                enumPropertyForLibrary(
                        "dragStartBehavior", GESTURES_LIBRARY,
                        "DragStartBehavior", "down", "start"),
                enumProperty(
                        "keyboardDismissBehavior",
                        "ScrollViewKeyboardDismissBehavior", "manual", "onDrag"),
                stringLengthProperty("restorationId", 1, 256),
                enumProperty(
                        "clipBehavior", "Clip", "none", "hardEdge",
                        "antiAlias", "antiAliasWithSaveLayer"),
                enumPropertyForLibrary(
                        "hitTestBehavior", RENDERING_LIBRARY,
                        "HitTestBehavior", "deferToChild", "opaque", "translucent")),
                Map.of("children", listSlotSchema(false, 0, 10_000)));
    }

    private static CanvasProjection gridViewCountProjection() {
        return projection(Map.ofEntries(
                enumProperty("scrollDirection", "Axis", "horizontal", "vertical"),
                property("reverse", PropertyValueKind.BOOLEAN),
                property("primary", PropertyValueKind.BOOLEAN),
                stringPatternProperty(
                        "physics",
                        "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)"),
                property("shrinkWrap", PropertyValueKind.BOOLEAN),
                edgeInsetsProperty("padding", true),
                requiredDefaultNumericProperty(
                        "crossAxisCount",
                        "integer:2",
                        POSITIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                numericProperty(
                        "mainAxisSpacing", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "crossAxisSpacing", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "childAspectRatio", POSITIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "mainAxisExtent", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("addAutomaticKeepAlives", PropertyValueKind.BOOLEAN),
                property("addRepaintBoundaries", PropertyValueKind.BOOLEAN),
                property("addSemanticIndexes", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "scrollCacheExtent", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "semanticChildCount", NON_NEGATIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                enumPropertyForLibrary(
                        "dragStartBehavior", GESTURES_LIBRARY,
                        "DragStartBehavior", "down", "start"),
                enumProperty(
                        "keyboardDismissBehavior",
                        "ScrollViewKeyboardDismissBehavior", "manual", "onDrag"),
                stringLengthProperty("restorationId", 1, 256),
                enumProperty(
                        "clipBehavior", "Clip", "none", "hardEdge",
                        "antiAlias", "antiAliasWithSaveLayer"),
                enumPropertyForLibrary(
                        "hitTestBehavior", RENDERING_LIBRARY,
                        "HitTestBehavior", "deferToChild", "opaque", "translucent")),
                Map.of("children", listSlotSchema(false, 0, 10_000)));
    }

    private static CanvasProjection gridViewExtentProjection() {
        return projection(Map.ofEntries(
                enumProperty("scrollDirection", "Axis", "horizontal", "vertical"),
                property("reverse", PropertyValueKind.BOOLEAN),
                property("primary", PropertyValueKind.BOOLEAN),
                stringPatternProperty(
                        "physics",
                        "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)"),
                property("shrinkWrap", PropertyValueKind.BOOLEAN),
                edgeInsetsProperty("padding", true),
                requiredDefaultNumericProperty(
                        "maxCrossAxisExtent",
                        "double:200",
                        POSITIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "mainAxisSpacing", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "crossAxisSpacing", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "childAspectRatio", POSITIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "mainAxisExtent", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("addAutomaticKeepAlives", PropertyValueKind.BOOLEAN),
                property("addRepaintBoundaries", PropertyValueKind.BOOLEAN),
                property("addSemanticIndexes", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "scrollCacheExtent", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "semanticChildCount", NON_NEGATIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                enumPropertyForLibrary(
                        "dragStartBehavior", GESTURES_LIBRARY,
                        "DragStartBehavior", "down", "start"),
                enumProperty(
                        "keyboardDismissBehavior",
                        "ScrollViewKeyboardDismissBehavior", "manual", "onDrag"),
                stringLengthProperty("restorationId", 1, 256),
                enumProperty(
                        "clipBehavior", "Clip", "none", "hardEdge",
                        "antiAlias", "antiAliasWithSaveLayer"),
                enumPropertyForLibrary(
                        "hitTestBehavior", RENDERING_LIBRARY,
                        "HitTestBehavior", "deferToChild", "opaque", "translucent")),
                Map.of("children", listSlotSchema(false, 0, 10_000)));
    }

    private static CanvasProjection singleChildScrollViewProjection() {
        return projection(Map.ofEntries(
                enumProperty("scrollDirection", "Axis", "horizontal", "vertical"),
                property("reverse", PropertyValueKind.BOOLEAN),
                edgeInsetsProperty("padding", true),
                property("primary", PropertyValueKind.BOOLEAN),
                stringPatternProperty(
                        "physics",
                        "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)"),
                enumPropertyForLibrary(
                        "dragStartBehavior", GESTURES_LIBRARY,
                        "DragStartBehavior", "down", "start"),
                enumProperty(
                        "clipBehavior", "Clip", "none", "hardEdge",
                        "antiAlias", "antiAliasWithSaveLayer"),
                enumPropertyForLibrary(
                        "hitTestBehavior", RENDERING_LIBRARY,
                        "HitTestBehavior", "deferToChild", "opaque", "translucent"),
                stringLengthProperty("restorationId", 1, 256),
                enumProperty(
                        "keyboardDismissBehavior",
                        "ScrollViewKeyboardDismissBehavior", "manual", "onDrag")),
                Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection sliverPrototypeProjection(String type) {
        var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        for (var property : definition.properties()) properties.put(property.name().value(), propertyContract(property));
        return projection(properties, type.endsWith("List")
                ? Map.of("prototypeItem", singleSlotSchema(true, 0), "children", listSlotSchema(true, 0, 10_000))
                : Map.of("prototypeItem", singleSlotSchema(true, 0)));
    }

    private static CanvasProjection sliverDynamicProjection(String type) {
        var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        for (var property : definition.properties()) properties.put(property.name().value(), propertyContract(property));
        return projection(properties, definition.slots().isEmpty() ? Map.of()
                : Map.of("children", listSlotSchema(true, 0, 10_000)));
    }

    private static CanvasProjection sliverChildrenProjection(boolean list, boolean extent) {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        if (list) {
            for (String name : List.of("addAutomaticKeepAlives", "addRepaintBoundaries", "addSemanticIndexes")) {
                properties.put(name, propertySchema(PropertyValueKind.BOOLEAN));
            }
        } else {
            properties.put(extent ? "maxCrossAxisExtent" : "crossAxisCount",
                    requiredDefaultNumericProperty(extent ? "maxCrossAxisExtent" : "crossAxisCount",
                            extent ? "double:200" : "integer:2",
                            extent ? POSITIVE_DOUBLE_BOUNDS : POSITIVE_INTEGER_BOUNDS,
                            extent ? PropertyValueKind.DOUBLE : PropertyValueKind.INTEGER).getValue());
            properties.put("mainAxisSpacing", numericSchema(NON_NEGATIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
            properties.put("crossAxisSpacing", numericSchema(NON_NEGATIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
            properties.put("childAspectRatio", numericSchema(POSITIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        }
        return projection(properties, Map.of("children", listSlotSchema(list, 0, 10_000)));
    }

    private static CanvasProjection customScrollViewProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("scrollDirection", enumProperty("scrollDirection", "Axis", "horizontal", "vertical").getValue());
        properties.put("reverse", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("controller", new CanvasPropertyContract(
                Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL), false,
                Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("ScrollController"),
                        PropertyValueKind.NULL, "any")));
        properties.put("primary", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("physics", stringPatternProperty(
                "physics", "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)").getValue());
        properties.put("shrinkWrap", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("anchor", numericSchema(ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        properties.put("scrollCacheExtent", numericSchema(
                NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        properties.put("paintOrder", enumProperty("paintOrder", "SliverPaintOrder", "firstIsTop", "lastIsTop").getValue());
        properties.put("semanticChildCount", numericSchema(
                NON_NEGATIVE_INTEGER_BOUNDS, PropertyValueKind.INTEGER));
        properties.put("dragStartBehavior", enumPropertyForLibrary(
                "dragStartBehavior", GESTURES_LIBRARY, "DragStartBehavior", "down", "start").getValue());
        properties.put("keyboardDismissBehavior", enumProperty(
                "keyboardDismissBehavior", "ScrollViewKeyboardDismissBehavior", "manual", "onDrag").getValue());
        properties.put("restorationId", stringLengthProperty("restorationId", 1, 256).getValue());
        properties.put("clipBehavior", enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer").getValue());
        properties.put("hitTestBehavior", enumPropertyForLibrary(
                "hitTestBehavior", RENDERING_LIBRARY, "HitTestBehavior", "deferToChild", "opaque", "translucent").getValue());
        if (properties.size() != CustomScrollViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("CustomScrollView projection count: " + properties.size());
        }
        return projection(properties, Map.of(
                "slivers", traitListSlotSchema(false, 0, 10_000, BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)));
    }

    private static CanvasProjection pageViewProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("scrollDirection", enumProperty("scrollDirection", "Axis", "horizontal", "vertical").getValue());
        properties.put("reverse", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("controller", new CanvasPropertyContract(
                Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL), false,
                Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("PageController"),
                        PropertyValueKind.NULL, "any")));
        properties.put("physics", stringPatternProperty(
                "physics", "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)").getValue());
        properties.put("pageSnapping", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("onPageChanged", new CanvasPropertyContract(
                Set.of(PropertyValueKind.STRING, PropertyValueKind.NULL,
                        PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE), false,
                Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.STRING, "pattern:" + base64("noop"),
                        PropertyValueKind.NULL, "any",
                        PropertyValueKind.CALLBACK, "callbackReference",
                        PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("ValueChanged<int>"))));
        properties.put("dragStartBehavior", enumPropertyForLibrary(
                "dragStartBehavior", GESTURES_LIBRARY, "DragStartBehavior", "down", "start").getValue());
        properties.put("allowImplicitScrolling", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("scrollCacheExtent", expansionNullable(numericSchema(
                NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)));
        properties.put("restorationId", stringLengthProperty("restorationId", 1, 256).getValue());
        properties.put("clipBehavior", enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer").getValue());
        properties.put("hitTestBehavior", enumPropertyForLibrary(
                "hitTestBehavior", RENDERING_LIBRARY, "HitTestBehavior", "deferToChild", "opaque", "translucent").getValue());
        properties.put("scrollBehavior", new CanvasPropertyContract(
                Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL), false,
                Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("ScrollBehavior"),
                        PropertyValueKind.NULL, "any")));
        properties.put("padEnds", propertySchema(PropertyValueKind.BOOLEAN));
        if (properties.size() != PageViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("PageView projection count: " + properties.size());
        }
        return projection(properties, Map.of("children", listSlotSchema(false, 0, 10_000)));
    }

    private static CanvasProjection listWheelScrollViewProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("controller", new CanvasPropertyContract(
                Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL), false,
                Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("ScrollController"),
                        PropertyValueKind.NULL, "any")));
        properties.put("physics", stringPatternProperty(
                "physics", "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)").getValue());
        properties.put("diameterRatio", numericSchema(POSITIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        properties.put("perspective", numericSchema(PERSPECTIVE_BOUNDS, PropertyValueKind.DOUBLE));
        properties.put("offAxisFraction", numericSchema(UNBOUNDED_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        properties.put("useMagnifier", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("magnification", numericSchema(POSITIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        properties.put("overAndUnderCenterOpacity", numericSchema(ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        properties.put("itemExtent", new CanvasPropertyContract(
                Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE), true,
                Optional.of("integer:50"), POSITIVE_NUMBER_BOUNDS,
                rangeConstraintFingerprints(POSITIVE_NUMBER_BOUNDS)));
        properties.put("squeeze", numericSchema(POSITIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        properties.put("onSelectedItemChanged", new CanvasPropertyContract(
                Set.of(PropertyValueKind.STRING, PropertyValueKind.NULL,
                        PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE), false,
                Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.STRING, "pattern:" + base64("noop"),
                        PropertyValueKind.NULL, "any",
                        PropertyValueKind.CALLBACK, "callbackReference",
                        PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("ValueChanged<int>"))));
        properties.put("renderChildrenOutsideViewport", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("clipBehavior", enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer").getValue());
        properties.put("hitTestBehavior", enumPropertyForLibrary(
                "hitTestBehavior", RENDERING_LIBRARY, "HitTestBehavior", "deferToChild", "opaque", "translucent").getValue());
        properties.put("restorationId", stringLengthProperty("restorationId", 1, 256).getValue());
        properties.put("scrollBehavior", new CanvasPropertyContract(
                Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL), false,
                Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("ScrollBehavior"),
                        PropertyValueKind.NULL, "any")));
        properties.put("dragStartBehavior", enumPropertyForLibrary(
                "dragStartBehavior", GESTURES_LIBRARY, "DragStartBehavior", "down", "start").getValue());
        properties.put("changeReportingBehavior", enumProperty(
                "changeReportingBehavior", "ChangeReportingBehavior", "onScrollEnd", "onScrollUpdate").getValue());
        if (properties.size() != ListWheelScrollViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("ListWheelScrollView projection count: " + properties.size());
        }
        return projection(properties, Map.of("children", listSlotSchema(false, 0, 10_000)));
    }

    private static CanvasProjection coloredBoxProjection() {
        return projection(Map.ofEntries(
                requiredDefaultColorOrThemeProperty(
                        "color", "color:0xFF2196F3"),
                property("isAntiAlias", PropertyValueKind.BOOLEAN)),
                Map.of("child", singleSlotSchema(false, 0)));
    }

    private static final String SHAPE_BORDER_CLIPPER_FINGERPRINT =
            "shapeBorderClipper:v1:roundedRectangle,beveledRectangle,continuousRectangle,roundedSuperellipse,circle,stadium:finiteNonNegativeRadius:explicitDirectional:ltr,rtl";

    private static CanvasProjection physicalShapeProjection() {
        return projection(Map.ofEntries(
                Map.entry("clipper", new CanvasPropertyContract(
                        Set.of(PropertyValueKind.SHAPE_BORDER_CLIPPER, PropertyValueKind.DART_OBJECT_REFERENCE),
                        true, Optional.of("shapeBorderClipper:roundedRectangle:physicalZero:none"), Map.of(),
                        Map.of(PropertyValueKind.SHAPE_BORDER_CLIPPER, SHAPE_BORDER_CLIPPER_FINGERPRINT,
                                PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                + "CustomClipper<Path>:currentOrPackage:root,optionalMember:reference,"
                                + "zeroArgumentInvocation:requiredConstnessBoolean(false,true)"))),
                enumProperty("clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                numericProperty("elevation", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                requiredDefaultColorOrThemeProperty("color", "color:0xFF2196F3"),
                colorOrThemeProperty("shadowColor")), Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection physicalModelProjection() {
        return projection(Map.ofEntries(
                enumProperty("shape", "BoxShape", "rectangle", "circle"),
                enumProperty("clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                Map.entry("borderRadius", constrainedSchema(PropertyValueKind.BORDER_RADIUS,
                        PHYSICAL_BORDER_RADIUS_CONTRACT_FINGERPRINT)),
                numericProperty("elevation", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                requiredDefaultColorOrThemeProperty("color", "color:0xFF2196F3"),
                colorOrThemeProperty("shadowColor")),
                Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection placeholderProjection() {
        return projection(Map.ofEntries(
                colorOrThemeProperty("color"),
                numericProperty(
                        "strokeWidth", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "fallbackWidth", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "fallbackHeight", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)),
                Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection containerProjection() {
        String decorationFingerprint = boxDecorationFingerprint(
                REVIEWED_COLOR_THEME_TOKENS);
        return projection(Map.ofEntries(
                Map.entry("alignment", constrainedSchema(
                        PropertyValueKind.ALIGNMENT_GEOMETRY,
                        "alignmentGeometry")),
                edgeInsetsProperty("padding", true),
                colorOrThemeProperty("color"),
                property("isAntiAlias", PropertyValueKind.BOOLEAN),
                Map.entry("decoration", constrainedSchema(
                        PropertyValueKind.BOX_DECORATION,
                        decorationFingerprint)),
                Map.entry("foregroundDecoration", constrainedSchema(
                        PropertyValueKind.BOX_DECORATION,
                        decorationFingerprint)),
                numericProperty(
                        "width", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "height", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                Map.entry("constraints", constrainedSchema(
                        PropertyValueKind.BOX_CONSTRAINTS,
                        "boxConstraints:v2:finiteOrPositiveInfinity")),
                edgeInsetsProperty("margin", true),
                Map.entry("transform", constrainedSchema(
                        PropertyValueKind.MATRIX4,
                        "matrix4")),
                Map.entry("transformAlignment", constrainedSchema(
                        PropertyValueKind.ALIGNMENT_GEOMETRY,
                        "alignmentGeometry")),
                enumProperty(
                        "clipBehavior", "Clip", "none", "hardEdge",
                        "antiAlias", "antiAliasWithSaveLayer")),
                Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection imageProjection() {
        return projection(Map.ofEntries(
                requiredConstrainedProperty(
                        "image",
                        PropertyValueKind.IMAGE_PROVIDER,
                        IMAGE_PROVIDER_CONTRACT_FINGERPRINT),
                callbackProperty("frameBuilder"),
                callbackProperty("loadingBuilder"),
                callbackProperty("errorBuilder"),
                property("semanticLabel", PropertyValueKind.STRING),
                property("excludeFromSemantics", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "width", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "height", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                colorOrThemeProperty("color"),
                numericProperty(
                        "opacity", ZERO_TO_ONE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "colorBlendMode", "BlendMode",
                        "clear", "src", "dst", "srcOver", "dstOver",
                        "srcIn", "dstIn", "srcOut", "dstOut", "srcATop", "dstATop",
                        "xor", "plus", "modulate", "screen", "overlay", "darken",
                        "lighten", "colorDodge", "colorBurn", "hardLight", "softLight",
                        "difference", "exclusion", "multiply", "hue", "saturation",
                        "color", "luminosity"),
                enumProperty(
                        "fit", "BoxFit", "fill", "contain", "cover", "fitWidth",
                        "fitHeight", "none", "scaleDown"),
                Map.entry("alignment", constrainedSchema(
                        PropertyValueKind.ALIGNMENT_GEOMETRY,
                        "alignmentGeometry")),
                enumProperty(
                        "repeat", "ImageRepeat", "repeat", "repeatX", "repeatY",
                        "noRepeat"),
                numericProperty(
                        "centerSliceLeft", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "centerSliceTop", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "centerSliceRight", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "centerSliceBottom", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("matchTextDirection", PropertyValueKind.BOOLEAN),
                property("gaplessPlayback", PropertyValueKind.BOOLEAN),
                property("isAntiAlias", PropertyValueKind.BOOLEAN),
                enumProperty(
                        "filterQuality", "FilterQuality", "none", "low", "medium",
                        "high")),
                Map.of());
    }

    private static CanvasProjection textFieldProjection() {
        return projection(Map.ofEntries(
                stringPatternProperty(
                        "keyboardType",
                        "(?:text|multiline|number|numberSigned|numberDecimal|numberSignedDecimal|phone|datetime|emailAddress|url|visiblePassword|name|streetAddress|none|webSearch|twitter)"),
                enumPropertyForLibrary(
                        "textInputAction", SERVICES_LIBRARY, "TextInputAction",
                        "none", "unspecified", "done", "go", "search", "send",
                        "next", "previous", "continueAction", "join", "route",
                        "emergencyCall", "newline"),
                enumPropertyForLibrary(
                        "textCapitalization", SERVICES_LIBRARY, "TextCapitalization",
                        "words", "sentences", "characters", "none"),
                enumProperty(
                        "textAlign", "TextAlign",
                        "left", "right", "center", "justify", "start", "end"),
                stringPatternProperty(
                        "textAlignVertical", "(?:top|center|bottom)"),
                enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                property("readOnly", PropertyValueKind.BOOLEAN),
                property("showCursor", PropertyValueKind.BOOLEAN),
                property("autofocus", PropertyValueKind.BOOLEAN),
                stringPatternProperty(
                        "obscuringCharacter",
                        "[\\u0000-\\uD7FF\\uE000-\\uFFFF]"),
                property("obscureText", PropertyValueKind.BOOLEAN),
                property("autocorrect", PropertyValueKind.BOOLEAN),
                enumPropertyForLibrary(
                        "smartDashesType", SERVICES_LIBRARY, "SmartDashesType",
                        "disabled", "enabled"),
                enumPropertyForLibrary(
                        "smartQuotesType", SERVICES_LIBRARY, "SmartQuotesType",
                        "disabled", "enabled"),
                property("enableSuggestions", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "maxLines", POSITIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                numericProperty(
                        "minLines", POSITIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                property("expands", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "maxLength", MAX_LENGTH_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                enumPropertyForLibrary(
                        "maxLengthEnforcement", SERVICES_LIBRARY,
                        "MaxLengthEnforcement",
                        "none", "enforced", "truncateAfterCompositionEnds"),
                callbackProperty("onChanged"),
                callbackProperty("onEditingComplete"),
                callbackProperty("onSubmitted"),
                callbackProperty("onAppPrivateCommand"),
                property("enabled", PropertyValueKind.BOOLEAN),
                property("ignorePointers", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "cursorWidth", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "cursorHeight", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "cursorRadiusX", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "cursorRadiusY", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("cursorOpacityAnimates", PropertyValueKind.BOOLEAN),
                colorOrThemeProperty("cursorColor"),
                colorOrThemeProperty("cursorErrorColor"),
                enumPropertyForLibrary(
                        "selectionHeightStyle", DART_UI_LIBRARY, "BoxHeightStyle",
                        "tight", "max", "includeLineSpacingMiddle",
                        "includeLineSpacingTop", "includeLineSpacingBottom", "strut"),
                enumPropertyForLibrary(
                        "selectionWidthStyle", DART_UI_LIBRARY, "BoxWidthStyle",
                        "tight", "max"),
                enumProperty("keyboardAppearance", "Brightness", "dark", "light"),
                numericProperty(
                        "scrollPaddingLeft", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "scrollPaddingTop", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "scrollPaddingRight", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "scrollPaddingBottom", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumPropertyForLibrary(
                        "dragStartBehavior", GESTURES_LIBRARY, "DragStartBehavior",
                        "down", "start"),
                property("enableInteractiveSelection", PropertyValueKind.BOOLEAN),
                property("selectAllOnFocus", PropertyValueKind.BOOLEAN),
                callbackProperty("onTap"),
                property("onTapAlwaysCalled", PropertyValueKind.BOOLEAN),
                callbackProperty("onTapOutside"),
                callbackProperty("onTapUpOutside"),
                stringPatternProperty(
                        "mouseCursor",
                        "(?:none|basic|click|forbidden|wait|progress|contextMenu|help|text|verticalText|cell|precise|move|grab|grabbing|noDrop|alias|copy|disappearing|allScroll|resizeLeftRight|resizeUpDown|resizeUpLeftDownRight|resizeUpRightDownLeft|resizeUp|resizeDown|resizeLeft|resizeRight|resizeUpLeft|resizeUpRight|resizeDownLeft|resizeDownRight|resizeColumn|resizeRow|zoomIn|zoomOut)"),
                enumProperty(
                        "clipBehavior", "Clip", "none", "hardEdge", "antiAlias",
                        "antiAliasWithSaveLayer"),
                stringLengthProperty("restorationId", 1, 256),
                property("stylusHandwritingEnabled", PropertyValueKind.BOOLEAN),
                property("enableIMEPersonalizedLearning", PropertyValueKind.BOOLEAN),
                property("enableInlinePrediction", PropertyValueKind.BOOLEAN),
                property("canRequestFocus", PropertyValueKind.BOOLEAN),
                textFieldBuilderProjection("buildCounter", "InputCounterWidgetBuilder?"),
                textFieldBuilderProjection("contextMenuBuilder", "EditableTextContextMenuBuilder?")),
                Map.of());
    }

    private static Map.Entry<String, CanvasPropertyContract> textFieldBuilderProjection(String name, String type) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL), false,
                Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint(type),
                        PropertyValueKind.NULL, "any")));
    }

    private static CanvasProjection scaffoldProjection() {
        LinkedHashMap<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        put(properties, stringPatternProperty(
                "floatingActionButtonLocation",
                "(?:startTop|miniStartTop|centerTop|miniCenterTop|endTop|miniEndTop|startFloat|miniStartFloat|centerFloat|miniCenterFloat|endFloat|miniEndFloat|startDocked|miniStartDocked|centerDocked|miniCenterDocked|endDocked|miniEndDocked|endContained)"));
        put(properties, stringPatternProperty(
                "floatingActionButtonAnimator", "(?:scaling|noAnimation)"));
        put(properties, stringPatternProperty(
                "persistentFooterAlignment",
                "(?:topStart|topCenter|topEnd|centerStart|center|centerEnd|bottomStart|bottomCenter|bottomEnd)"));
        put(properties, callbackProperty("onDrawerChanged"));
        put(properties, callbackProperty("onEndDrawerChanged"));
        properties.put("bottomSheetScrimBuilder", constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                refreshIndicatorReferenceFingerprint("Widget? Function(BuildContext, Animation<double>)")));
        put(properties, colorOrThemeProperty("backgroundColor"));
        put(properties, property("resizeToAvoidBottomInset", PropertyValueKind.BOOLEAN));
        put(properties, property("primary", PropertyValueKind.BOOLEAN));
        put(properties, enumPropertyForLibrary(
                "drawerDragStartBehavior", GESTURES_LIBRARY,
                "DragStartBehavior", "down", "start"));
        put(properties, property("extendBody", PropertyValueKind.BOOLEAN));
        put(properties, property("drawerBarrierDismissible", PropertyValueKind.BOOLEAN));
        put(properties, property("extendBodyBehindAppBar", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty("drawerScrimColor"));
        put(properties, numericProperty(
                "drawerEdgeDragWidth", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, property("drawerEnableOpenDragGesture", PropertyValueKind.BOOLEAN));
        put(properties, property(
                "endDrawerEnableOpenDragGesture", PropertyValueKind.BOOLEAN));
        put(properties, stringLengthProperty("restorationId", 1, 256));
        return projection(properties, Map.of(
                "appBar", traitSingleSlotSchema(
                        false, 0, BuiltInWidgetCatalog.PREFERRED_SIZE_WIDGET_TRAIT),
                "body", singleSlotSchema(false, 0),
                "floatingActionButton", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection appBarProjection() {
        LinkedHashMap<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        put(properties, colorOrThemeProperty("backgroundColor"));
        put(properties, property("centerTitle", PropertyValueKind.BOOLEAN));
        put(properties, numericProperty(
                "elevation", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, property("automaticallyImplyLeading", PropertyValueKind.BOOLEAN));
        put(properties, property("automaticallyImplyActions", PropertyValueKind.BOOLEAN));
        put(properties, numericProperty(
                "scrolledUnderElevation", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        Map<PropertyValueKind, String> predicate = Map.of(
                PropertyValueKind.STRING, "pattern:" + base64("(?:default|depthZero|all)"),
                PropertyValueKind.DART_OBJECT_REFERENCE,
                refreshIndicatorReferenceFingerprint("ScrollNotificationPredicate"));
        properties.put("notificationPredicate", new CanvasPropertyContract(predicate.keySet(), false,
                Optional.empty(), Map.of(), predicate));
        put(properties, colorOrThemeProperty("shadowColor"));
        put(properties, colorOrThemeProperty("surfaceTintColor"));
        put(properties, colorOrThemeProperty("foregroundColor"));
        put(properties, property("primary", PropertyValueKind.BOOLEAN));
        put(properties, property("excludeHeaderSemantics", PropertyValueKind.BOOLEAN));
        put(properties, numericProperty(
                "titleSpacing", UNBOUNDED_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "toolbarOpacity", ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "bottomOpacity", ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "toolbarHeight", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "leadingWidth", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, property("forceMaterialTransparency", PropertyValueKind.BOOLEAN));
        put(properties, property("useDefaultSemanticsOrder", PropertyValueKind.BOOLEAN));
        put(properties, enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias",
                "antiAliasWithSaveLayer"));
        put(properties, edgeInsetsProperty("actionsPadding", true));
        put(properties, property("animateColor", PropertyValueKind.BOOLEAN));

        put(properties, stringPatternProperty(
                "shapeKind",
                "(?:roundedRectangle|stadium|circle|beveledRectangle|continuousRectangle)"));
        put(properties, colorOrThemeProperty("shapeSideColor"));
        put(properties, numericProperty(
                "shapeSideWidth", NON_NEGATIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                "shapeSideStyle", "BorderStyle", "none", "solid"));
        put(properties, numericProperty(
                "shapeSideStrokeAlign", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        for (String radius : List.of(
                "shapeRadiusTopLeft", "shapeRadiusTopRight",
                "shapeRadiusBottomRight", "shapeRadiusBottomLeft")) {
            put(properties, numericProperty(
                    radius, NON_NEGATIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        }
        put(properties, numericProperty(
                "shapeCircleEccentricity", ZERO_TO_ONE_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));

        appendIconThemeProjection(properties, "iconTheme");
        appendIconThemeProjection(properties, "actionsIconTheme");
        appendTextStyleProjection(properties, "toolbarTextStyle");
        appendTextStyleProjection(properties, "titleTextStyle");

        put(properties, colorOrThemeProperty(
                "systemOverlayStyleSystemNavigationBarColor"));
        put(properties, colorOrThemeProperty(
                "systemOverlayStyleSystemNavigationBarDividerColor"));
        put(properties, enumProperty(
                "systemOverlayStyleSystemNavigationBarIconBrightness",
                "Brightness", "light", "dark"));
        put(properties, property(
                "systemOverlayStyleSystemNavigationBarContrastEnforced",
                PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty("systemOverlayStyleStatusBarColor"));
        put(properties, enumProperty(
                "systemOverlayStyleStatusBarBrightness",
                "Brightness", "light", "dark"));
        put(properties, enumProperty(
                "systemOverlayStyleStatusBarIconBrightness",
                "Brightness", "light", "dark"));
        put(properties, property(
                "systemOverlayStyleSystemStatusBarContrastEnforced",
                PropertyValueKind.BOOLEAN));

        return projection(properties, Map.of(
                "leading", singleSlotSchema(false, 0),
                "title", singleSlotSchema(false, 0),
                "actions", listSlotSchema(false, 0, 10_000),
                "flexibleSpace", singleSlotSchema(false, 0),
                "bottom", traitSingleSlotSchema(
                        false, 0,
                BuiltInWidgetCatalog.PREFERRED_SIZE_WIDGET_TRAIT)));
    }

    private static CanvasProjection menuAnchorProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        Set<String> suffixes = Set.of("BackgroundColor", "ShadowColor", "SurfaceTintColor", "Elevation", "Padding",
                "MinimumWidth", "MinimumHeight", "FixedWidth", "FixedHeight", "MaximumWidth", "MaximumHeight",
                "SideColor", "SideWidth", "SideStyle", "SideStrokeAlign", "ShapeKind", "ShapeRadiusTopLeft",
                "ShapeRadiusTopRight", "ShapeRadiusBottomRight", "ShapeRadiusBottomLeft", "ShapeCircleEccentricity", "MouseCursor");
        Set<String> common = Set.of("styleVisualDensityHorizontal", "styleVisualDensityVertical", "styleAlignmentKind", "styleAlignmentX", "styleAlignmentY");
        fullStyleButtonProjection("TextButton").propertyContracts().forEach((name, contract) -> {
            if (common.contains(name.value()) || List.of("style", "styleDisabled", "styleError", "styleDragged", "stylePressed", "styleSelected", "styleScrolledUnder", "styleHovered", "styleFocused").stream()
                    .anyMatch(prefix -> name.value().startsWith(prefix) && suffixes.contains(name.value().substring(prefix.length())))) {
                properties.put(name.value(), contract);
            }
        });
        for (var entry : Map.of("controller", "MenuController", "childFocusNode", "FocusNode", "style", "MenuStyle",
                "layerLink", "LayerLink", "onOpen", "VoidCallback", "onClose", "VoidCallback",
                "onAnimationStatusChanged", "ValueChanged<AnimationStatus>", "builder", "MenuAnchorChildBuilder").entrySet()) {
            properties.put(entry.getKey(), expansionNullable(listTileReference(entry.getValue())));
        }
        properties.put("alignmentOffset", expansionNullable(listTileAddReference(constrainedSchema(PropertyValueKind.OFFSET, "offset:finiteSigned"), "Offset")));
        properties.put("reservedPadding", expansionNullable(listTileAddReference(edgeInsetsProperty("reservedPadding", false).getValue(), "EdgeInsetsGeometry")));
        for (String name : List.of("anchorTapClosesMenu", "consumeOutsideTap", "crossAxisUnconstrained", "useRootOverlay", "animated")) properties.put(name, propertySchema(PropertyValueKind.BOOLEAN));
        put(properties, enumProperty("clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
        if (properties.size() != 219) throw new ExceptionInInitializerError("MenuAnchor projection count: " + properties.size());
        return projection(properties, Map.of("menuChildren", listSlotSchema(true, 0, 10_000), "child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection submenuButtonProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        fullStyleButtonProjection("TextButton").propertyContracts().forEach((name, contract) -> {
            if (name.value().startsWith("style") && !name.value().equals("style")) properties.put(name.value(), contract);
        });
        menuAnchorProjection().propertyContracts().forEach((name, contract) -> {
            if (name.value().startsWith("style") && !name.value().equals("style")) properties.put("menuStyle" + name.value().substring(5), contract);
        });
        for (var entry : Map.ofEntries(Map.entry("onHover", "ValueChanged<bool>"), Map.entry("onFocusChange", "ValueChanged<bool>"),
                Map.entry("onOpen", "VoidCallback"), Map.entry("onClose", "VoidCallback"), Map.entry("controller", "MenuController"),
                Map.entry("style", "ButtonStyle"), Map.entry("menuStyle", "MenuStyle"), Map.entry("focusNode", "FocusNode"),
                Map.entry("statesController", "WidgetStatesController"), Map.entry("submenuIcon", "WidgetStateProperty<Widget?>"),
                Map.entry("onAnimationStatusChanged", "ValueChanged<AnimationStatus>")).entrySet()) {
            properties.put(entry.getKey(), expansionNullable(listTileReference(entry.getValue())));
        }
        properties.put("alignmentOffset", expansionNullable(listTileAddReference(constrainedSchema(PropertyValueKind.OFFSET, "offset:finiteSigned"), "Offset")));
        put(properties, enumProperty("clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
        for (String name : List.of("useRootOverlay", "animated")) properties.put(name, propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("hoverOpenDelayUs", listTileAddReference(numericSchema(
                Map.of(PropertyValueKind.INTEGER, bounds(new BigDecimal(DartNumericLiterals.MIN_PORTABLE_INTEGER), true,
                        new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER), true)), PropertyValueKind.INTEGER), "Duration"));
        for (String name : List.of("submenuIconDefault", "submenuIconDisabled", "submenuIconHovered", "submenuIconFocused")) {
            properties.put(name, expansionNullable(listTileAddReference(constrainedSchema(PropertyValueKind.ICON_DATA,
                    "materialIcons:3.44.8:058e0af2c2:8825:ba88e3e23962ada6537523aa113811d9719b988412815bf084f50a0aa78137f0"), "Widget")));
        }
        if (properties.size() != 721) throw new ExceptionInInitializerError("SubmenuButton projection count: " + properties.size());
        return projection(properties, Map.of("child", singleSlotSchema(true, 0), "leadingIcon", singleSlotSchema(false, 0),
                "trailingIcon", singleSlotSchema(false, 0), "menuChildren", listSlotSchema(true, 0, 10_000)));
    }

    private static CanvasProjection menuBarProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        menuAnchorProjection().propertyContracts().forEach((name, contract) -> {
            if (name.value().startsWith("style") && !name.value().equals("style")) {
                properties.put(name.value(), contract);
            }
        });
        properties.put("style", expansionNullable(listTileReference("MenuStyle")));
        put(properties, enumProperty("clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
        properties.put("controller", expansionNullable(listTileReference("MenuController")));
        if (properties.size() != MenuBarWidgetPropertySchema.FLATTENED_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("MenuBar projection count: " + properties.size());
        }
        return projection(properties, Map.of("children", listSlotSchema(true, 0, 10_000)));
    }

    private static CanvasProjection navigationBarProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("animationDurationUs", expansionNullable(listTileAddReference(numericSchema(
                Map.of(PropertyValueKind.INTEGER, bounds(
                        new BigDecimal(DartNumericLiterals.MIN_PORTABLE_INTEGER), true,
                        new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER), true)),
                PropertyValueKind.INTEGER), "Duration")));
        properties.put("selectedIndex", requiredDefaultNumericProperty(
                "selectedIndex", "integer:0", Map.of(
                        PropertyValueKind.INTEGER, bounds(BigDecimal.ZERO, true,
                                new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER), true)),
                PropertyValueKind.INTEGER).getValue());
        properties.put("onDestinationSelected", new CanvasPropertyContract(
                Set.of(PropertyValueKind.STRING, PropertyValueKind.NULL,
                        PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE),
                false, Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.STRING, "pattern:" + base64("noop"),
                        PropertyValueKind.NULL, "any",
                        PropertyValueKind.CALLBACK, "callbackReference",
                        PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                + "ValueChanged<int>:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)")));
        for (String name : NavigationBarWidgetPropertySchema.colorProperties()) {
            properties.put(name, expansionNullable(listTileAddReference(
                    colorOrThemeProperty(name).getValue(), "Color")));
        }
        properties.put("elevation", expansionNullable(cardNumberSchema(null, null)));
        properties.put("indicatorShape", expansionNullable(listTileReference("ShapeBorder")));
        properties.put("height", expansionNullable(cardNumberSchema(null, null)));
        properties.put("labelBehavior", expansionNullable(enumPropertyForLibrary(
                "labelBehavior", MATERIAL_LIBRARY, "NavigationDestinationLabelBehavior",
                "alwaysShow", "onlyShowSelected", "alwaysHide").getValue()));
        properties.put("overlayColor", expansionNullable(listTileReference("WidgetStateProperty<Color?>")));
        properties.put("labelTextStyle", expansionNullable(listTileReference("WidgetStateProperty<TextStyle?>")));
        properties.put("labelPadding", expansionNullable(listTileAddReference(
                edgeInsetsProperty("labelPadding", true).getValue(), "EdgeInsetsGeometry")));
        properties.put("maintainBottomViewPadding", propertySchema(PropertyValueKind.BOOLEAN));
        if (properties.size() != NavigationBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("NavigationBar projection count: " + properties.size());
        }
        return projection(properties, Map.of("destinations", listSlotSchema(true, 0, 10_000)));
    }

    private static CanvasProjection navigationRailProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        for (String name : NavigationRailWidgetPropertySchema.colorProperties()) {
            properties.put(name, expansionNullable(listTileAddReference(
                    colorOrThemeProperty(name).getValue(), "Color")));
        }
        for (String name : List.of("extended", "leadingAtTop", "trailingAtBottom", "scrollable")) {
            properties.put(name, propertySchema(PropertyValueKind.BOOLEAN));
        }
        Map<PropertyValueKind, String> selectedIndexConstraints =
                new java.util.EnumMap<>(PropertyValueKind.class);
        selectedIndexConstraints.putAll(
                rangeConstraintFingerprints(NON_NEGATIVE_INTEGER_BOUNDS));
        selectedIndexConstraints.put(PropertyValueKind.NULL, "any");
        properties.put("selectedIndex", new CanvasPropertyContract(
                Set.of(PropertyValueKind.INTEGER, PropertyValueKind.NULL), true,
                Optional.of("integer:0"), NON_NEGATIVE_INTEGER_BOUNDS,
                Map.copyOf(selectedIndexConstraints)));
        properties.put("onDestinationSelected", new CanvasPropertyContract(
                Set.of(PropertyValueKind.STRING, PropertyValueKind.NULL,
                        PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE),
                false, Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.STRING, "pattern:" + base64("noop"),
                        PropertyValueKind.NULL, "any",
                        PropertyValueKind.CALLBACK, "callbackReference",
                        PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                + "ValueChanged<int>:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)")));
        for (String name : List.of("elevation", "minWidth", "minExtendedWidth")) {
            properties.put(name, expansionNullable(numericSchema(
                    Map.of(PropertyValueKind.INTEGER, POSITIVE_PORTABLE_INTEGER,
                            PropertyValueKind.DOUBLE, POSITIVE_NUMERIC),
                    PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)));
        }
        properties.put("groupAlignment", expansionNullable(numericSchema(
                Map.of(PropertyValueKind.INTEGER, bounds(BigDecimal.valueOf(-1), true, BigDecimal.ONE, true),
                        PropertyValueKind.DOUBLE, bounds(BigDecimal.valueOf(-1), true, BigDecimal.ONE, true)),
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)));
        properties.put("labelType", expansionNullable(enumPropertyForLibrary(
                "labelType", MATERIAL_LIBRARY, "NavigationRailLabelType", "none", "selected", "all").getValue()));
        for (String name : List.of("unselectedLabelTextStyle", "selectedLabelTextStyle")) {
            properties.put(name, expansionNullable(listTileReference("TextStyle")));
        }
        for (String name : List.of("unselectedIconTheme", "selectedIconTheme")) {
            properties.put(name, expansionNullable(listTileReference("IconThemeData")));
        }
        properties.put("useIndicator", expansionNullable(propertySchema(PropertyValueKind.BOOLEAN)));
        properties.put("indicatorShape", expansionNullable(listTileReference("ShapeBorder")));
        properties.put("mainAxisAlignment", expansionNullable(enumProperty(
                "mainAxisAlignment", "MainAxisAlignment", "start", "end", "center", "spaceBetween",
                "spaceAround", "spaceEvenly").getValue()));
        if (properties.size() != NavigationRailWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("NavigationRail projection count: " + properties.size());
        }
        return projection(properties, Map.of(
                "leading", singleSlotSchema(false, 0),
                "trailing", singleSlotSchema(false, 0),
                "destinations", listSlotSchema(true, 0, 10_000)));
    }

    private static CanvasProjection navigationDrawerProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        for (String name : NavigationDrawerWidgetPropertySchema.colorProperties()) {
            properties.put(name, expansionNullable(listTileAddReference(
                    colorOrThemeProperty(name).getValue(), "Color")));
        }
        properties.put("elevation", expansionNullable(cardNumberSchema(null, null)));
        properties.put("indicatorShape", expansionNullable(listTileReference("ShapeBorder")));
        properties.put("onDestinationSelected", new CanvasPropertyContract(
                Set.of(PropertyValueKind.STRING, PropertyValueKind.NULL,
                        PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE),
                false, Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.STRING, "pattern:" + base64("noop"),
                        PropertyValueKind.NULL, "any",
                        PropertyValueKind.CALLBACK, "callbackReference",
                        PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                + "ValueChanged<int>:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)")));
        Map<PropertyValueKind, String> selectedIndexConstraints =
                new java.util.EnumMap<>(PropertyValueKind.class);
        selectedIndexConstraints.putAll(rangeConstraintFingerprints(NON_NEGATIVE_INTEGER_BOUNDS));
        selectedIndexConstraints.put(PropertyValueKind.NULL, "any");
        properties.put("selectedIndex", new CanvasPropertyContract(
                Set.of(PropertyValueKind.INTEGER, PropertyValueKind.NULL), true,
                Optional.of("integer:0"), NON_NEGATIVE_INTEGER_BOUNDS,
                Map.copyOf(selectedIndexConstraints)));
        properties.put("tilePadding", expansionNullable(listTileAddReference(
                edgeInsetsProperty("tilePadding", true).getValue(), "EdgeInsetsGeometry")));
        if (properties.size() != NavigationDrawerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("NavigationDrawer projection count: " + properties.size());
        }
        return projection(properties, Map.of(
                "header", singleSlotSchema(false, 0),
                "footer", singleSlotSchema(false, 0),
                "children", listSlotSchema(true, 0, 10_000)));
    }

    private static CanvasProjection drawerProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        for (String name : List.of("backgroundColor", "shadowColor", "surfaceTintColor")) {
            properties.put(name, expansionNullable(listTileAddReference(
                    colorOrThemeProperty(name).getValue(), "Color")));
        }
        properties.put("elevation", expansionNullable(cardNumberSchema(BigDecimal.ZERO, null)));
        properties.put("shape", expansionNullable(listTileReference("ShapeBorder")));
        properties.put("width", expansionNullable(cardNumberSchema(BigDecimal.ZERO, null)));
        properties.put("semanticLabel", propertySchema(PropertyValueKind.STRING, PropertyValueKind.NULL));
        properties.put("clipBehavior", expansionNullable(enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias",
                "antiAliasWithSaveLayer").getValue()));
        if (properties.size() != DrawerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Drawer projection count: " + properties.size());
        }
        return projection(properties, Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection sliverAppBarProjection() {
        return projection(SliverAppBarWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(
                p -> p.name().value(), BuiltInWidgetCapabilityCatalog::propertyContract)),
                Map.of("leading", singleSlotSchema(false, 0), "title", singleSlotSchema(false, 0),
                        "actions", listSlotSchema(false, 0, 10_000), "flexibleSpace", singleSlotSchema(false, 0),
                        "bottom", traitSingleSlotSchema(false, 0, BuiltInWidgetCatalog.PREFERRED_SIZE_WIDGET_TRAIT)));
    }

    private static CanvasProjection bottomAppBarProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        for (String name : List.of("color", "shadowColor", "surfaceTintColor")) {
            properties.put(name, expansionNullable(listTileAddReference(
                    colorOrThemeProperty(name).getValue(), "Color")));
        }
        properties.put("elevation", expansionNullable(cardNumberSchema(BigDecimal.ZERO, null)));
        properties.put("shape", expansionNullable(listTileReference("NotchedShape")));
        properties.put("clipBehavior", enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias",
                "antiAliasWithSaveLayer").getValue());
        properties.put("notchMargin", cardNumberSchema(BigDecimal.ZERO, null));
        properties.put("padding", expansionNullable(listTileAddReference(
                edgeInsetsProperty("padding", false).getValue(), "EdgeInsetsGeometry")));
        properties.put("height", expansionNullable(cardNumberSchema(BigDecimal.ZERO, null)));
        if (properties.size() != BottomAppBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("BottomAppBar projection count: " + properties.size());
        }
        return projection(properties, Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection bottomNavigationBarProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("onTap", new CanvasPropertyContract(
                Set.of(PropertyValueKind.STRING, PropertyValueKind.NULL,
                        PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE), false,
                Optional.empty(), Map.of(), Map.of(
                        PropertyValueKind.STRING, "pattern:" + base64("noop"),
                        PropertyValueKind.NULL, "any",
                        PropertyValueKind.CALLBACK, "callbackReference",
                        PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                + "ValueChanged<int>:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)")));
        properties.put("currentIndex", requiredDefaultNumericProperty(
                "currentIndex", "integer:0", Map.of(
                        PropertyValueKind.INTEGER, bounds(BigDecimal.ZERO, true,
                                new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER), true)),
                PropertyValueKind.INTEGER).getValue());
        properties.put("elevation", expansionNullable(cardNumberSchema(BigDecimal.ZERO, null)));
        properties.put("barType", expansionNullable(enumPropertyForLibrary(
                "barType", MATERIAL_LIBRARY, "BottomNavigationBarType", "fixed", "shifting").getValue()));
        for (String name : BottomNavigationBarWidgetPropertySchema.colorProperties()) {
            properties.put(name, expansionNullable(listTileAddReference(
                    colorOrThemeProperty(name).getValue(), "Color")));
        }
        for (String name : List.of("iconSize", "selectedFontSize", "unselectedFontSize")) {
            properties.put(name, cardNumberSchema(BigDecimal.ZERO, null));
        }
        for (String name : List.of("selectedIconTheme", "unselectedIconTheme", "selectedLabelStyle", "unselectedLabelStyle", "mouseCursor")) {
            properties.put(name, expansionNullable(listTileReference(switch (name) {
                case "selectedIconTheme", "unselectedIconTheme" -> "IconThemeData";
                case "selectedLabelStyle", "unselectedLabelStyle" -> "TextStyle";
                default -> "MouseCursor";
            })));
        }
        for (String name : List.of("showSelectedLabels", "showUnselectedLabels", "enableFeedback")) {
            properties.put(name, expansionNullable(propertySchema(PropertyValueKind.BOOLEAN)));
        }
        properties.put("landscapeLayout", expansionNullable(enumPropertyForLibrary(
                "landscapeLayout", MATERIAL_LIBRARY, "BottomNavigationBarLandscapeLayout",
                "spread", "centered", "linear").getValue()));
        properties.put("useLegacyColorScheme", propertySchema(PropertyValueKind.BOOLEAN));
        if (properties.size() != BottomNavigationBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("BottomNavigationBar projection count: " + properties.size());
        }
        return projection(properties, Map.of("items", listSlotSchema(true, 0, 10_000)));
    }

    private static CanvasProjection materialProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("materialType", enumPropertyForLibrary(
                "materialType", MATERIAL_LIBRARY, "MaterialType",
                "canvas", "card", "circle", "button", "transparency").getValue());
        properties.put("elevation", cardNumberSchema(BigDecimal.ZERO, null));
        for (String name : List.of("color", "shadowColor", "surfaceTintColor")) {
            properties.put(name, expansionNullable(listTileAddReference(
                    colorOrThemeProperty(name).getValue(), "Color")));
        }
        properties.put("textStyle", expansionNullable(listTileReference("TextStyle")));
        properties.put("borderRadius", expansionNullable(listTileAddReference(
                constrainedSchema(PropertyValueKind.BORDER_RADIUS, BORDER_RADIUS_CONTRACT_FINGERPRINT),
                "BorderRadiusGeometry")));
        properties.put("shape", expansionNullable(listTileReference("ShapeBorder")));
        properties.put("borderOnForeground", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("clipBehavior", enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer").getValue());
        properties.put("animationDurationUs", expansionNullable(listTileAddReference(numericSchema(
                Map.of(PropertyValueKind.INTEGER, bounds(
                        new BigDecimal(DartNumericLiterals.MIN_PORTABLE_INTEGER), true,
                        new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER), true)),
                PropertyValueKind.INTEGER), "Duration")));
        properties.put("animateColor", propertySchema(PropertyValueKind.BOOLEAN));
        if (properties.size() != MaterialWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Material projection count: " + properties.size());
        }
        return projection(properties, Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection scrollbarProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("controller", expansionNullable(listTileReference("ScrollController")));
        properties.put("thumbVisibility", propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL));
        properties.put("trackVisibility", propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL));
        properties.put("thickness", expansionNullable(numericSchema(
                NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)));
        properties.put("radius", expansionNullable(listTileReference("Radius")));
        properties.put("notificationPredicate", expansionNullable(listTileReference("ScrollNotificationPredicate")));
        properties.put("interactive", propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL));
        properties.put("scrollbarOrientation", expansionNullable(enumProperty(
                "scrollbarOrientation", "ScrollbarOrientation", "left", "right", "top", "bottom").getValue()));
        if (properties.size() != ScrollbarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Scrollbar projection count: " + properties.size());
        }
        return projection(properties, Map.of("child", singleSlotSchema(true, 1)));
    }

    private static CanvasProjection menuItemButtonProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        fullStyleButtonProjection("TextButton").propertyContracts().forEach((name, contract) -> {
            if (name.value().startsWith("style")) properties.put(name.value(), contract);
        });
        properties.put("style", expansionNullable(listTileReference("ButtonStyle")));
        put(properties, requiredDefaultProperty("enabled", "boolean:true", PropertyValueKind.BOOLEAN));
        properties.put("onPressed", listTileReference("VoidCallback"));
        for (String name : List.of("onHover", "onFocusChange")) properties.put(name, expansionNullable(listTileReference("ValueChanged<bool>")));
        properties.put("focusNode", expansionNullable(listTileReference("FocusNode")));
        properties.put("statesController", expansionNullable(listTileReference("WidgetStatesController")));
        for (String name : List.of("autofocus", "requestFocusOnHover", "closeOnActivate", "shortcutControl", "shortcutShift", "shortcutAlt", "shortcutMeta", "shortcutIncludeRepeats"))
            properties.put(name, propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("shortcut", expansionNullable(listTileReference("MenuSerializableShortcut")));
        properties.put("semanticsLabel", propertySchema(PropertyValueKind.STRING, PropertyValueKind.NULL));
        properties.put("shortcutCharacter", propertySchema(PropertyValueKind.STRING));
        put(properties, enumProperty("clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
        put(properties, enumProperty("overflowAxis", "Axis", "horizontal", "vertical"));
        put(properties, enumProperty("shortcutNumLock", "LockState", "ignored", "locked", "unlocked"));
        put(properties, enumPropertyForLibrary("shortcutTrigger", MenuShortcutKeyCatalog.LIBRARY_URI, "LogicalKeyboardKey", MenuShortcutKeyCatalog.names().toArray(String[]::new)));
        if (properties.size() != 520) throw new ExceptionInInitializerError("MenuItemButton projection count: " + properties.size());
        return projection(properties, Map.of("child", singleSlotSchema(false, 0), "leadingIcon", singleSlotSchema(false, 0), "trailingIcon", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection fullStyleButtonProjection(String familyName) {
        boolean outlined = !familyName.equals("TextButton");
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        put(properties, requiredDefaultProperty("enabled", "boolean:true", PropertyValueKind.BOOLEAN));
        for (String name : List.of("onPressed", "onLongPress", "onHover", "onFocusChange")) {
            properties.put(name, constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                    refreshIndicatorReferenceFingerprint(name.equals("onPressed") || name.equals("onLongPress")
                            ? "VoidCallback" : "ValueChanged<bool>")));
        }
        properties.put("focusNode", constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                refreshIndicatorReferenceFingerprint("FocusNode")));
        put(properties, property("autofocus", PropertyValueKind.BOOLEAN));
        CanvasPropertyContract clip = enumProperty("clipBehavior", "Clip", "none", "hardEdge", "antiAlias",
                "antiAliasWithSaveLayer").getValue();
        Map<PropertyValueKind, String> nullableClip = new java.util.EnumMap<>(PropertyValueKind.class);
        nullableClip.putAll(clip.constraintFingerprints());
        nullableClip.put(PropertyValueKind.NULL, "any");
        properties.put("clipBehavior", new CanvasPropertyContract(nullableClip.keySet(), false,
                Optional.empty(), Map.of(), nullableClip));
        properties.put("statesController", constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                refreshIndicatorReferenceFingerprint("WidgetStatesController")));
        if (!outlined) {
            put(properties, property("isSemanticButton", PropertyValueKind.BOOLEAN, PropertyValueKind.NULL));
        }
        put(properties, materialEnumProperty("iconAlignment", "IconAlignment", "start", "end"));
        put(properties, requiredDefaultConstrainedProperty("variant", "string:" + base64("standard"),
                PropertyValueKind.STRING, "pattern:" + base64(familyName.equals("FilledButton")
                        ? "(?:standard|icon|tonal|tonalIcon)" : "(?:standard|icon)")));
        for (String prefix : TextButtonWidgetPropertySchema.statePrefixes()) {
            appendElevatedButtonStateProjection(properties, prefix);
            appendElevatedButtonTextProjection(properties, prefix);
        }
        elevatedButtonProjection().propertyContracts().forEach((name, property) -> {
            if (ElevatedButtonWidgetPropertySchema.find(name).orElseThrow().group()
                    == ElevatedButtonWidgetPropertySchema.Group.COMMON_STYLE
                    && !name.value().equals("styleBackgroundBuilder")
                    && !name.value().equals("styleForegroundBuilder")) {
                properties.put(name.value(), property);
            }
        });
        put(properties, materialEnumProperty("styleIconAlignment", "IconAlignment", "start", "end"));
        for (String name : List.of("styleBackgroundBuilder", "styleForegroundBuilder")) {
            properties.put(name, constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                    refreshIndicatorReferenceFingerprint("ButtonLayerBuilder")));
        }
        properties.put("style", constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                refreshIndicatorReferenceFingerprint("ButtonStyle")));
        if (properties.size() != (outlined ? OutlinedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT
                : TextButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT)) {
            throw new ExceptionInInitializerError(familyName + " projection count: " + properties.size());
        }
        return projection(properties, Map.of("child", singleSlotSchema(true, familyName.equals("FilledButton") ? 0 : 1),
                "icon", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection elevatedButtonProjection() {
        LinkedHashMap<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        put(properties, optionalDefaultProperty(
                "enabled", "boolean:true", PropertyValueKind.BOOLEAN));
        for (String callback : List.of(
                "onPressed", "onLongPress", "onHover", "onFocusChange")) {
            put(properties, callbackProperty(callback));
        }
        put(properties, property("autofocus", PropertyValueKind.BOOLEAN));
        put(properties, enumProperty(
                "clipBehavior", "Clip", "none", "hardEdge", "antiAlias",
                "antiAliasWithSaveLayer"));

        for (String prefix : List.of(
                "style", "styleDisabled", "stylePressed",
                "styleHovered", "styleFocused")) {
            appendElevatedButtonStateProjection(properties, prefix);
            appendElevatedButtonTextProjection(properties, prefix);
        }

        put(properties, numericProperty(
                "styleVisualDensityHorizontal",
                MINUS_FOUR_TO_FOUR_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "styleVisualDensityVertical",
                MINUS_FOUR_TO_FOUR_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, materialEnumProperty(
                "styleTapTargetSize", "MaterialTapTargetSize",
                "padded", "shrinkWrap"));
        put(properties, numericProperty(
                "styleAnimationDurationMs",
                Map.of(PropertyValueKind.INTEGER, NON_NEGATIVE_PORTABLE_INTEGER),
                PropertyValueKind.INTEGER));
        put(properties, property("styleEnableFeedback", PropertyValueKind.BOOLEAN));
        put(properties, stringPatternProperty(
                "styleAlignmentKind", "(?:physical|directional)"));
        put(properties, numericProperty(
                "styleAlignmentX", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                "styleAlignmentY", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, stringPatternProperty(
                "styleSplashFactory",
                "(?:inkRipple|inkSplash|inkSparkle|noSplash)"));

        for (String name : List.of("styleBackgroundBuilder", "styleForegroundBuilder")) {
            properties.put(name, constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                    refreshIndicatorReferenceFingerprint("ButtonLayerBuilder")));
        }

        if (properties.size()
                != ElevatedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ElevatedButton Canvas projection must contain exactly "
                    + ElevatedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT
                    + " properties; actual=" + properties.size());
        }
        return projection(properties, Map.of(
                "child", singleSlotSchema(true, 0)));
    }

    private static void appendElevatedButtonStateProjection(
            Map<String, CanvasPropertyContract> properties,
            String prefix) {
        for (String suffix : List.of(
                "BackgroundColor", "ForegroundColor", "OverlayColor",
                "ShadowColor", "SurfaceTintColor")) {
            put(properties, colorOrThemeProperty(prefix + suffix));
        }
        put(properties, numericProperty(
                prefix + "Elevation", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, edgeInsetsProperty(prefix + "Padding", true));
        for (String suffix : List.of(
                "MinimumWidth", "MinimumHeight", "FixedWidth", "FixedHeight",
                "MaximumWidth", "MaximumHeight")) {
            put(properties, numericProperty(
                    prefix + suffix, NON_NEGATIVE_NUMBER_BOUNDS,
                    PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        }
        put(properties, colorOrThemeProperty(prefix + "IconColor"));
        put(properties, numericProperty(
                prefix + "IconSize", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, colorOrThemeProperty(prefix + "SideColor"));
        put(properties, numericProperty(
                prefix + "SideWidth", NON_NEGATIVE_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                prefix + "SideStyle", "BorderStyle", "none", "solid"));
        put(properties, numericProperty(
                prefix + "SideStrokeAlign", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, stringPatternProperty(
                prefix + "ShapeKind",
                "(?:roundedRectangle|roundedSuperellipse|stadium|circle|beveledRectangle|continuousRectangle)"));
        for (String suffix : List.of(
                "ShapeRadiusTopLeft", "ShapeRadiusTopRight",
                "ShapeRadiusBottomRight", "ShapeRadiusBottomLeft")) {
            put(properties, numericProperty(
                    prefix + suffix, NON_NEGATIVE_DOUBLE_BOUNDS,
                    PropertyValueKind.DOUBLE));
        }
        put(properties, numericProperty(
                prefix + "ShapeCircleEccentricity",
                ZERO_TO_ONE_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, stringPatternProperty(
                prefix + "MouseCursor",
                "(?:none|basic|click|forbidden|wait|progress|contextMenu|help|text|verticalText|cell|precise|move|grab|grabbing|noDrop|alias|copy|disappearing|allScroll|resizeLeftRight|resizeUpDown|resizeUpLeftDownRight|resizeUpRightDownLeft|resizeUp|resizeDown|resizeLeft|resizeRight|resizeUpLeft|resizeUpRight|resizeDownLeft|resizeDownRight|resizeColumn|resizeRow|zoomIn|zoomOut)"));
    }

    private static void appendElevatedButtonTextProjection(
            Map<String, CanvasPropertyContract> properties,
            String prefix) {
        String text = prefix + "Text";
        put(properties, themeTokenProperty(
                text + "Theme", REVIEWED_TEXT_THEME_TOKENS));
        put(properties, property(text + "Inherit", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty(text + "BackgroundColor"));
        put(properties, numericProperty(
                text + "FontSize", NON_NEGATIVE_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                text + "FontWeight", "FontWeight",
                "w100", "w200", "w300", "w400", "w500",
                "w600", "w700", "w800", "w900"));
        put(properties, enumProperty(
                text + "FontStyle", "FontStyle", "normal", "italic"));
        put(properties, numericProperty(
                text + "LetterSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                text + "WordSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                text + "TextBaseline", "TextBaseline", "alphabetic", "ideographic"));
        put(properties, numericProperty(
                text + "Height", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                text + "LeadingDistribution", "TextLeadingDistribution",
                "proportional", "even"));
        put(properties, stringPatternProperty(
                text + "LocaleLanguageCode", "(?:[a-z]{2,3}|[a-z]{5,8})"));
        put(properties, stringPatternProperty(
                text + "LocaleScriptCode", "[A-Z][a-z]{3}"));
        put(properties, stringPatternProperty(
                text + "LocaleCountryCode", "(?:[A-Z]{2}|[0-9]{3})"));
        put(properties, paintProperty(text + "Background"));
        put(properties, shadowProperty(text + "Shadows"));
        put(properties, property(
                text + "FontFeatures", PropertyValueKind.FONT_FEATURE_LIST));
        put(properties, fontVariationProperty(text + "FontVariations"));
        put(properties, property(
                text + "DecorationUnderline", PropertyValueKind.BOOLEAN));
        put(properties, property(
                text + "DecorationOverline", PropertyValueKind.BOOLEAN));
        put(properties, property(
                text + "DecorationLineThrough", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty(text + "DecorationColor"));
        put(properties, enumProperty(
                text + "DecorationStyle", "TextDecorationStyle",
                "solid", "double", "dotted", "dashed", "wavy"));
        put(properties, numericProperty(
                text + "DecorationThickness", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, stringLengthProperty(text + "FontFamily", 1, 256));
        put(properties, stringLengthProperty(text + "FontFamilyFallback", 0, 4096));
        put(properties, stringLengthProperty(text + "Package", 1, 256));
        put(properties, enumProperty(
                text + "Overflow", "TextOverflow",
                "clip", "fade", "ellipsis", "visible"));
    }

    private static void appendIconThemeProjection(
            Map<String, CanvasPropertyContract> properties,
            String prefix) {
        put(properties, numericProperty(
                prefix + "Size", NON_NEGATIVE_NUMBER_BOUNDS,
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "Fill", ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "Weight", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "Grade", GRADE_AXIS_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "OpticalSize", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, colorOrThemeProperty(prefix + "Color"));
        put(properties, numericProperty(
                prefix + "Opacity", ZERO_TO_ONE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, shadowProperty(prefix + "Shadows"));
        put(properties, property(prefix + "ApplyTextScaling", PropertyValueKind.BOOLEAN));
    }

    private static void appendTextStyleProjection(
            Map<String, CanvasPropertyContract> properties,
            String prefix) {
        put(properties, themeTokenProperty(prefix + "ThemeTextStyle", REVIEWED_TEXT_THEME_TOKENS));
        put(properties, property(prefix + "Inherit", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty(prefix + "Color"));
        put(properties, colorOrThemeProperty(prefix + "BackgroundColor"));
        put(properties, numericProperty(
                prefix + "FontSize", NON_NEGATIVE_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                prefix + "FontWeight", "FontWeight",
                "w100", "w200", "w300", "w400", "w500",
                "w600", "w700", "w800", "w900"));
        put(properties, enumProperty(
                prefix + "FontStyle", "FontStyle", "normal", "italic"));
        put(properties, numericProperty(
                prefix + "LetterSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, numericProperty(
                prefix + "WordSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                prefix + "TextBaseline", "TextBaseline", "alphabetic", "ideographic"));
        put(properties, numericProperty(
                prefix + "Height", UNBOUNDED_DOUBLE_BOUNDS, PropertyValueKind.DOUBLE));
        put(properties, enumProperty(
                prefix + "LeadingDistribution", "TextLeadingDistribution",
                "proportional", "even"));
        put(properties, stringPatternProperty(
                prefix + "LocaleLanguageCode", "(?:[a-z]{2,3}|[a-z]{5,8})"));
        put(properties, stringPatternProperty(
                prefix + "LocaleScriptCode", "[A-Z][a-z]{3}"));
        put(properties, stringPatternProperty(
                prefix + "LocaleCountryCode", "(?:[A-Z]{2}|[0-9]{3})"));
        put(properties, paintProperty(prefix + "Foreground"));
        put(properties, paintProperty(prefix + "Background"));
        put(properties, shadowProperty(prefix + "Shadows"));
        put(properties, property(prefix + "FontFeatures", PropertyValueKind.FONT_FEATURE_LIST));
        put(properties, fontVariationProperty(prefix + "FontVariations"));
        put(properties, property(prefix + "DecorationUnderline", PropertyValueKind.BOOLEAN));
        put(properties, property(prefix + "DecorationOverline", PropertyValueKind.BOOLEAN));
        put(properties, property(prefix + "DecorationLineThrough", PropertyValueKind.BOOLEAN));
        put(properties, colorOrThemeProperty(prefix + "DecorationColor"));
        put(properties, enumProperty(
                prefix + "DecorationStyle", "TextDecorationStyle",
                "solid", "double", "dotted", "dashed", "wavy"));
        put(properties, numericProperty(
                prefix + "DecorationThickness", UNBOUNDED_DOUBLE_BOUNDS,
                PropertyValueKind.DOUBLE));
        put(properties, property(prefix + "DebugLabel", PropertyValueKind.STRING));
        put(properties, stringLengthProperty(prefix + "FontFamily", 1, 256));
        put(properties, stringLengthProperty(prefix + "FontFamilyFallback", 0, 4096));
        put(properties, stringLengthProperty(prefix + "Package", 1, 256));
        put(properties, enumProperty(
                prefix + "Overflow", "TextOverflow", "clip", "fade",
                "ellipsis", "visible"));
    }

    private static void put(
            Map<String, CanvasPropertyContract> properties,
            Map.Entry<String, CanvasPropertyContract> entry) {
        if (properties.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
            throw new ExceptionInInitializerError(
                    "Duplicate reviewed Canvas property " + entry.getKey());
        }
    }

    private static CanvasProjection textProjection() {
        return projection(Map.ofEntries(
                requiredDefaultProperty(
                        "data", "string:VGV4dA", PropertyValueKind.STRING),
                enumProperty(
                        "textAlign", "TextAlign",
                        "start", "end", "left", "right", "center", "justify"),
                enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                property("softWrap", PropertyValueKind.BOOLEAN),
                numericProperty(
                        "maxLines", POSITIVE_INTEGER_BOUNDS,
                        PropertyValueKind.INTEGER),
                enumProperty(
                        "overflow", "TextOverflow",
                        "clip", "fade", "ellipsis", "visible"),
                property("semanticsLabel", PropertyValueKind.STRING),
                property("semanticsIdentifier", PropertyValueKind.STRING),
                enumProperty(
                        "textWidthBasis", "TextWidthBasis",
                        "parent", "longestLine"),
                colorOrThemeProperty("selectionColor"),
                stringPatternProperty(
                        "localeLanguageCode", "(?:[a-z]{2,3}|[a-z]{5,8})"),
                stringPatternProperty(
                        "localeScriptCode", "[A-Z][a-z]{3}"),
                stringPatternProperty(
                        "localeCountryCode", "(?:[A-Z]{2}|[0-9]{3})"),
                numericProperty(
                        "textScalerFactor", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("textHeightApplyFirstAscent", PropertyValueKind.BOOLEAN),
                property("textHeightApplyLastDescent", PropertyValueKind.BOOLEAN),
                enumProperty(
                        "textHeightLeadingDistribution", "TextLeadingDistribution",
                        "proportional", "even"),
                property("styleInherit", PropertyValueKind.BOOLEAN),
                themeTokenProperty("styleThemeTextStyle", REVIEWED_TEXT_THEME_TOKENS),
                colorOrThemeProperty("styleColor"),
                colorOrThemeProperty("styleBackgroundColor"),
                numericProperty(
                        "styleFontSize", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "styleFontWeight", "FontWeight",
                        "w100", "w200", "w300", "w400", "w500",
                        "w600", "w700", "w800", "w900"),
                enumProperty(
                        "styleFontStyle", "FontStyle", "normal", "italic"),
                numericProperty(
                        "styleLetterSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "styleWordSpacing", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "styleTextBaseline", "TextBaseline",
                        "alphabetic", "ideographic"),
                numericProperty(
                        "styleHeight", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "styleLeadingDistribution", "TextLeadingDistribution",
                        "proportional", "even"),
                stringPatternProperty(
                        "styleLocaleLanguageCode", "(?:[a-z]{2,3}|[a-z]{5,8})"),
                stringPatternProperty(
                        "styleLocaleScriptCode", "[A-Z][a-z]{3}"),
                stringPatternProperty(
                        "styleLocaleCountryCode", "(?:[A-Z]{2}|[0-9]{3})"),
                property("styleDecorationUnderline", PropertyValueKind.BOOLEAN),
                property("styleDecorationOverline", PropertyValueKind.BOOLEAN),
                property("styleDecorationLineThrough", PropertyValueKind.BOOLEAN),
                paintProperty("styleForeground"),
                paintProperty("styleBackground"),
                shadowProperty("styleShadows"),
                property("styleFontFeatures", PropertyValueKind.FONT_FEATURE_LIST),
                fontVariationProperty("styleFontVariations"),
                colorOrThemeProperty("styleDecorationColor"),
                enumProperty(
                        "styleDecorationStyle", "TextDecorationStyle",
                        "solid", "double", "dotted", "dashed", "wavy"),
                numericProperty(
                        "styleDecorationThickness", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                property("styleDebugLabel", PropertyValueKind.STRING),
                stringLengthProperty("styleFontFamily", 1, 256),
                stringLengthProperty("styleFontFamilyFallback", 0, 4096),
                stringLengthProperty("stylePackage", 1, 256),
                enumProperty(
                        "styleOverflow", "TextOverflow",
                        "clip", "fade", "ellipsis", "visible"),
                stringLengthProperty("strutFontFamily", 1, 256),
                stringLengthProperty("strutFontFamilyFallback", 0, 4096),
                numericProperty(
                        "strutFontSize", POSITIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "strutHeight", UNBOUNDED_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "strutLeadingDistribution", "TextLeadingDistribution",
                        "proportional", "even"),
                numericProperty(
                        "strutLeading", NON_NEGATIVE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                enumProperty(
                        "strutFontWeight", "FontWeight",
                        "w100", "w200", "w300", "w400", "w500",
                        "w600", "w700", "w800", "w900"),
                enumProperty(
                        "strutFontStyle", "FontStyle", "normal", "italic"),
                property("strutForceHeight", PropertyValueKind.BOOLEAN),
                property("strutDebugLabel", PropertyValueKind.STRING),
                stringLengthProperty("strutPackage", 1, 256)),
                Map.of());
    }

    private static CanvasProjection iconProjection() {
        return projection(Map.ofEntries(
                requiredDefaultConstrainedProperty(
                        "icon",
                        "iconData:58873:TWF0ZXJpYWxJY29ucw:-:0:-",
                        PropertyValueKind.ICON_DATA,
                        "materialIcons:3.44.8:058e0af2c2:8825:"
                        + "ba88e3e23962ada6537523aa113811d9719b988412815bf084f50a0aa78137f0"),
                numericProperty(
                        "size", NON_NEGATIVE_NUMBER_BOUNDS,
                        PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                numericProperty(
                        "fill", ZERO_TO_ONE_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "weight", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "grade", GRADE_AXIS_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                numericProperty(
                        "opticalSize", POSITIVE_FONT_AXIS_DOUBLE_BOUNDS,
                        PropertyValueKind.DOUBLE),
                colorOrThemeProperty("color"),
                shadowProperty("shadows"),
                property("semanticLabel", PropertyValueKind.STRING),
                enumProperty("textDirection", "TextDirection", "rtl", "ltr"),
                property("applyTextScaling", PropertyValueKind.BOOLEAN),
                enumProperty(
                        "blendMode", "BlendMode",
                        "clear", "src", "dst", "srcOver", "dstOver",
                        "srcIn", "dstIn", "srcOut", "dstOut", "srcATop", "dstATop",
                        "xor", "plus", "modulate", "screen", "overlay", "darken",
                        "lighten", "colorDodge", "colorBurn", "hardLight", "softLight",
                        "difference", "exclusion", "multiply", "hue", "saturation",
                        "color", "luminosity"),
                enumProperty(
                        "fontWeight", "FontWeight",
                        "w100", "w200", "w300", "w400", "w500",
                        "w600", "w700", "w800", "w900")),
                Map.of());
    }

    private static CanvasProjection projection(
            Map<String, CanvasPropertyContract> properties,
            Map<String, CanvasSlotContract> slots) {
        LinkedHashMap<PropertyName, CanvasPropertyContract> typedProperties =
                new LinkedHashMap<>();
        properties.forEach((name, contract) ->
                typedProperties.put(new PropertyName(name), contract));
        LinkedHashMap<SlotName, CanvasSlotContract> typedSlots =
                new LinkedHashMap<>();
        slots.forEach((name, contract) ->
                typedSlots.put(new SlotName(name), contract));
        return CanvasProjection.of(typedProperties, typedSlots);
    }

    private static CanvasPropertyContract propertySchema(
            PropertyValueKind... kinds) {
        return new CanvasPropertyContract(
                Set.of(kinds),
                false,
                Optional.empty(),
                Map.of(),
                anyConstraintFingerprints(kinds));
    }

    private static CanvasProjection refreshIndicatorProjection() {
        CanvasPropertyContract signed = cardNumberSchema(null, null);
        Map<PropertyValueKind, String> predicate = Map.of(
                PropertyValueKind.STRING, "pattern:" + base64("(?:default|depthZero|all)"),
                PropertyValueKind.DART_OBJECT_REFERENCE,
                refreshIndicatorReferenceFingerprint("ScrollNotificationPredicate"));
        return projection(Map.ofEntries(
                numericProperty("displacement", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                Map.entry("edgeOffset", signed),
                Map.entry("onRefresh", constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                        refreshIndicatorReferenceFingerprint("RefreshCallback"))),
                colorOrThemeProperty("color"), colorOrThemeProperty("backgroundColor"),
                Map.entry("notificationPredicate", new CanvasPropertyContract(predicate.keySet(), false,
                        Optional.empty(), Map.of(), predicate)),
                property("semanticsLabel", PropertyValueKind.STRING), property("semanticsValue", PropertyValueKind.STRING),
                Map.entry("strokeWidth", signed),
                Map.entry("triggerMode", constrainedSchema(PropertyValueKind.ENUM,
                        "enum:" + base64("package:flutter/material.dart") + ":RefreshIndicatorTriggerMode:anywhere,onEdge")),
                numericProperty("elevation", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                Map.entry("onStatusChange", constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                        refreshIndicatorReferenceFingerprint("ValueChanged<RefreshIndicatorStatus?>"))),
                Map.entry("variant", new CanvasPropertyContract(Set.of(PropertyValueKind.STRING), true,
                        Optional.of("string:" + base64("material")), Map.of(),
                        Map.of(PropertyValueKind.STRING, "pattern:" + base64("(?:material|adaptive|noSpinner)"))))),
                Map.of("child", singleSlotSchema(true, 1)));
    }

    private static String refreshIndicatorReferenceFingerprint(String type) {
        return DART_OBJECT_REFERENCE_CONTRACT_PREFIX + type
                + ":currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)";
    }

    private static CanvasProjection notificationListenerProjection() {
        Map<PropertyValueKind, String> typeConstraints = Map.of(
                PropertyValueKind.STRING, "pattern:" + base64("(?:Notification|LayoutChangedNotification|ScrollNotification|ScrollStartNotification|ScrollUpdateNotification|OverscrollNotification|ScrollEndNotification|UserScrollNotification|SizeChangedLayoutNotification|ScrollMetricsNotification|OverscrollIndicatorNotification|DraggableScrollableNotification|KeepAliveNotification|NavigationNotification)"),
                PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("Type"));
        Map<PropertyValueKind, String> callbackConstraints = Map.of(
                PropertyValueKind.CALLBACK, "callbackReference",
                PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("NotificationListenerCallback<Notification>"),
                PropertyValueKind.NULL, "any");
        return projection(Map.of(
                "notificationType", new CanvasPropertyContract(typeConstraints.keySet(), true,
                        Optional.of("string:" + base64("Notification")), Map.of(), typeConstraints),
                "onNotification", new CanvasPropertyContract(callbackConstraints.keySet(), false,
                        Optional.empty(), Map.of(), callbackConstraints)),
                Map.of("child", singleSlotSchema(true, 1)));
    }

    private static CanvasProjection focusProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("variant", new CanvasPropertyContract(Set.of(PropertyValueKind.STRING), false,
                Optional.of("string:" + base64("standard")), Map.of(),
                Map.of(PropertyValueKind.STRING, "pattern:" + base64("(?:standard|withExternalFocusNode)"))));
        for (String name : List.of("focusNode", "parentNode")) {
            properties.put(name, new CanvasPropertyContract(
                    Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL), false,
                    Optional.empty(), Map.of(), Map.of(
                            PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint("FocusNode?"),
                            PropertyValueKind.NULL, "any")));
        }
        for (var callback : Map.of("onFocusChange", "ValueChanged<bool>",
                "onKeyEvent", "FocusOnKeyEventCallback", "onKey", "FocusOnKeyCallback").entrySet()) {
            properties.put(callback.getKey(), new CanvasPropertyContract(
                    Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                    false, Optional.empty(), Map.of(), Map.of(
                            PropertyValueKind.CALLBACK, "callbackReference",
                            PropertyValueKind.DART_OBJECT_REFERENCE, refreshIndicatorReferenceFingerprint(callback.getValue()),
                            PropertyValueKind.NULL, "any")));
        }
        for (String name : List.of("canRequestFocus", "skipTraversal", "descendantsAreFocusable", "descendantsAreTraversable")) {
            properties.put(name, propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL));
        }
        properties.put("autofocus", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("includeSemantics", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("debugLabel", propertySchema(PropertyValueKind.STRING, PropertyValueKind.NULL));
        return projection(properties, Map.of("child", singleSlotSchema(true, 1)));
    }

    private static CanvasProjection mouseRegionProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("onEnter", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerEnterEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onExit", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerExitEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onHover", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerHoverEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("cursor", iconButtonProjection().propertyContracts().get(new PropertyName("mouseCursor")));
        properties.put("opaque", propertySchema(PropertyValueKind.BOOLEAN));
        var behavior = enumPropertyForLibrary("hitTestBehavior", RENDERING_LIBRARY,
                "HitTestBehavior", "deferToChild", "opaque", "translucent").getValue();
        Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
        constraints.putAll(behavior.constraintFingerprints());
        constraints.put(PropertyValueKind.NULL, "any");
        properties.put("hitTestBehavior", new CanvasPropertyContract(constraints.keySet(), false,
                Optional.empty(), Map.of(), constraints));
        return projection(properties, Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection listenerProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("onPointerDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerDownEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPointerMove", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerMoveEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPointerUp", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerUpEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPointerHover", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerHoverEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPointerCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerCancelEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPointerPanZoomStart", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerPanZoomStartEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPointerPanZoomUpdate", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerPanZoomUpdateEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPointerPanZoomEnd", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerPanZoomEndEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPointerSignal", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "PointerSignalEventListener:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("behavior", enumPropertyForLibrary("behavior", RENDERING_LIBRARY,
                "HitTestBehavior", "deferToChild", "opaque", "translucent").getValue());
        return projection(properties, Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection gestureDetectorProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("onTapDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTapUp", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapUpCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTap", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTapMove", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapMoveCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTapCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryTap", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryTapDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryTapUp", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapUpCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryTapCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryTapDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryTapUp", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapUpCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryTapCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onDoubleTapDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onDoubleTap", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onDoubleTapCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureTapCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onLongPressDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onLongPressCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onLongPress", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onLongPressStart", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressStartCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onLongPressMoveUpdate", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressMoveUpdateCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onLongPressUp", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressUpCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onLongPressEnd", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressEndCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryLongPressDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryLongPressCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryLongPress", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryLongPressStart", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressStartCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryLongPressMoveUpdate", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressMoveUpdateCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryLongPressUp", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressUpCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onSecondaryLongPressEnd", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressEndCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryLongPressDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryLongPressCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryLongPress", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryLongPressStart", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressStartCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryLongPressMoveUpdate", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressMoveUpdateCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryLongPressUp", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressUpCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onTertiaryLongPressEnd", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureLongPressEndCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onVerticalDragDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onVerticalDragStart", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragStartCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onVerticalDragUpdate", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragUpdateCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onVerticalDragEnd", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragEndCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onVerticalDragCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onHorizontalDragDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onHorizontalDragStart", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragStartCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onHorizontalDragUpdate", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragUpdateCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onHorizontalDragEnd", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragEndCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onHorizontalDragCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPanDown", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragDownCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPanStart", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragStartCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPanUpdate", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragUpdateCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPanEnd", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragEndCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onPanCancel", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureDragCancelCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onScaleStart", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureScaleStartCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onScaleUpdate", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureScaleUpdateCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onScaleEnd", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureScaleEndCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onForcePressStart", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureForcePressStartCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onForcePressPeak", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureForcePressPeakCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onForcePressUpdate", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureForcePressUpdateCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        properties.put("onForcePressEnd", new CanvasPropertyContract(
                Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                false, Optional.empty(), Map.of(), Map.of(
                    PropertyValueKind.CALLBACK, "callbackReference",
                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                            + "GestureForcePressEndCallback:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)",
                    PropertyValueKind.NULL, "any")));
        CanvasPropertyContract behavior = enumPropertyForLibrary("behavior", RENDERING_LIBRARY, "HitTestBehavior",
                "deferToChild", "opaque", "translucent").getValue();
        Map<PropertyValueKind, String> behaviorConstraints = new java.util.EnumMap<>(PropertyValueKind.class);
        behaviorConstraints.putAll(behavior.constraintFingerprints());
        behaviorConstraints.put(PropertyValueKind.NULL, "any");
        properties.put("behavior", new CanvasPropertyContract(behaviorConstraints.keySet(), false,
                Optional.empty(), Map.of(), behaviorConstraints));
        properties.put("excludeFromSemantics", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("dragStartBehavior", enumPropertyForLibrary("dragStartBehavior", GESTURES_LIBRARY, "DragStartBehavior", "down", "start").getValue());
        properties.put("trackpadScrollCausesScale", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("trackpadScrollToScaleFactor", constrainedSchema(PropertyValueKind.OFFSET, "offset:finiteSigned"));
        properties.put("supportedDevices", propertySchema(PropertyValueKind.POINTER_DEVICE_KIND_SET, PropertyValueKind.NULL));
        return projection(properties, Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection refreshProgressIndicatorProjection() {
        CanvasPropertyContract signed = cardNumberSchema(null, null);
        Map<PropertyValueKind, String> widthConstraints = new java.util.EnumMap<>(PropertyValueKind.class);
        widthConstraints.putAll(signed.constraintFingerprints());
        widthConstraints.put(PropertyValueKind.NULL, "any");
        CanvasPropertyContract width = new CanvasPropertyContract(widthConstraints.keySet(), false,
                Optional.empty(), signed.numericBounds(), widthConstraints);
        return projection(Map.ofEntries(
                Map.entry("value", signed), colorOrThemeProperty("backgroundColor"), colorOrThemeProperty("color"),
                Map.entry("valueColor", linearProgressIndicatorProjection().propertyContracts().get(new PropertyName("valueColor"))),
                Map.entry("strokeWidth", width), Map.entry("strokeAlign", signed),
                property("semanticsLabel", PropertyValueKind.STRING), property("semanticsValue", PropertyValueKind.STRING),
                enumProperty("strokeCap", "StrokeCap", "butt", "round", "square"),
                numericProperty("elevation", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                edgeInsetsProperty("indicatorMargin", true), edgeInsetsProperty("indicatorPadding", true)), Map.of());
    }

    private static CanvasProjection circularProgressIndicatorProjection() {
        CanvasPropertyContract signed = cardNumberSchema(null, null);
        CanvasPropertyContract valueColor = linearProgressIndicatorProjection().propertyContracts()
                .get(new PropertyName("valueColor"));
        return projection(Map.ofEntries(
                Map.entry("value", signed), colorOrThemeProperty("backgroundColor"), colorOrThemeProperty("color"),
                Map.entry("valueColor", valueColor), Map.entry("strokeWidth", signed), Map.entry("strokeAlign", signed),
                property("semanticsLabel", PropertyValueKind.STRING), property("semanticsValue", PropertyValueKind.STRING),
                enumProperty("strokeCap", "StrokeCap", "butt", "round", "square"),
                Map.entry("constraints", constrainedSchema(PropertyValueKind.BOX_CONSTRAINTS,
                        "boxConstraints:v2:finiteOrPositiveInfinity")),
                Map.entry("trackGap", withPositiveInfinity(signed)), property("year2023", PropertyValueKind.BOOLEAN),
                edgeInsetsProperty("padding", true),
                Map.entry("controller", constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                        DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                        + "AnimationController:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)")),
                Map.entry("variant", new CanvasPropertyContract(Set.of(PropertyValueKind.STRING), true,
                        Optional.of("string:" + base64("material")), Map.of(),
                        Map.of(PropertyValueKind.STRING, "pattern:" + base64("(?:material|adaptive)"))))), Map.of());
    }

    private static CanvasProjection linearProgressIndicatorProjection() {
        CanvasPropertyContract signed = cardNumberSchema(null, null);
        CanvasPropertyContract positive = numericSchema(Map.of(
                PropertyValueKind.INTEGER, bounds(BigDecimal.ONE, true, new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER), true),
                PropertyValueKind.DOUBLE, bounds(BigDecimal.ZERO, false, null, true)),
                PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE);
        Map<PropertyValueKind, String> valueColorConstraints = new java.util.EnumMap<>(PropertyValueKind.class);
        valueColorConstraints.putAll(colorOrThemeProperty("valueColor").getValue().constraintFingerprints());
        valueColorConstraints.put(PropertyValueKind.NULL, "any");
        valueColorConstraints.put(PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                + "Animation<Color?>:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
        CanvasPropertyContract valueColor = new CanvasPropertyContract(valueColorConstraints.keySet(), false,
                Optional.empty(), Map.of(), valueColorConstraints);
        return projection(Map.ofEntries(
                Map.entry("value", signed), colorOrThemeProperty("backgroundColor"), colorOrThemeProperty("color"),
                Map.entry("valueColor", valueColor), Map.entry("minHeight", withPositiveInfinity(positive)),
                property("semanticsLabel", PropertyValueKind.STRING), property("semanticsValue", PropertyValueKind.STRING),
                Map.entry("borderRadius", constrainedSchema(PropertyValueKind.BORDER_RADIUS, BORDER_RADIUS_CONTRACT_FINGERPRINT)),
                colorOrThemeProperty("stopIndicatorColor"), Map.entry("stopIndicatorRadius", withPositiveInfinity(signed)),
                Map.entry("trackGap", withPositiveInfinity(signed)), property("year2023", PropertyValueKind.BOOLEAN),
                Map.entry("controller", constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                        DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                        + "AnimationController:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)"))), Map.of());
    }

    private static CanvasPropertyContract withPositiveInfinity(CanvasPropertyContract numeric) {
        Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
        constraints.putAll(numeric.constraintFingerprints());
        constraints.put(PropertyValueKind.ENUM, "enum:" + base64("dart:core") + ":double:infinity");
        return new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), numeric.numericBounds(), constraints);
    }

    private static CanvasProjection circleAvatarProjection() {
        Map<PropertyValueKind, String> radiusFingerprints = new java.util.EnumMap<>(PropertyValueKind.class);
        radiusFingerprints.putAll(rangeConstraintFingerprints(NON_NEGATIVE_NUMBER_BOUNDS));
        radiusFingerprints.put(PropertyValueKind.ENUM,
                "enum:" + base64("dart:core") + ":double:infinity");
        CanvasPropertyContract radius = new CanvasPropertyContract(
                Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE, PropertyValueKind.ENUM),
                false, Optional.empty(), NON_NEGATIVE_NUMBER_BOUNDS, radiusFingerprints);
        return projection(Map.ofEntries(
                colorOrThemeProperty("backgroundColor"),
                Map.entry("backgroundImage", constrainedSchema(
                        PropertyValueKind.IMAGE_PROVIDER, IMAGE_PROVIDER_CONTRACT_FINGERPRINT)),
                Map.entry("foregroundImage", constrainedSchema(
                        PropertyValueKind.IMAGE_PROVIDER, IMAGE_PROVIDER_CONTRACT_FINGERPRINT)),
                callbackProperty("onBackgroundImageError"),
                callbackProperty("onForegroundImageError"),
                colorOrThemeProperty("foregroundColor"),
                Map.entry("radius", radius), Map.entry("minRadius", radius), Map.entry("maxRadius", radius)),
                Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection badgeProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>(Map.ofEntries(
                colorOrThemeProperty("backgroundColor"), colorOrThemeProperty("textColor"),
                numericProperty("smallSize", NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                Map.entry("largeSize", cardNumberSchema(null, null)),
                edgeInsetsProperty("padding", true),
                Map.entry("alignment", constrainedSchema(PropertyValueKind.ALIGNMENT_GEOMETRY, "alignmentGeometry")),
                Map.entry("offset", constrainedSchema(PropertyValueKind.OFFSET, "offset:finiteSigned")),
                property("isLabelVisible", PropertyValueKind.BOOLEAN),
                numericProperty("count", NON_NEGATIVE_INTEGER_BOUNDS, PropertyValueKind.INTEGER),
                numericProperty("maxCount", POSITIVE_INTEGER_BOUNDS, PropertyValueKind.INTEGER)));
        appendTextStyleProjection(properties, "textStyle");
        return projection(properties, Map.of("label", singleSlotSchema(false, 0), "child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection rangeSliderProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        CanvasProjection slider = sliderProjection();
        CanvasProjection checkbox = checkboxProjection();
        for (String name : RangeSliderWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            if (RangeSliderWidgetPropertySchema.overlayColorStateProperties().contains(name)) {
                value = slider.propertyContracts().get(new PropertyName(name));
            } else if (RangeSliderWidgetPropertySchema.mouseCursorStateProperties().contains(name)) {
                var cursor = checkbox.propertyContracts().get(new PropertyName("mouseCursor"));
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(cursor.constraintFingerprints());
                constraints.put(PropertyValueKind.NULL, "any");
                value = new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), Map.of(), constraints);
            } else if (RangeSliderWidgetPropertySchema.rangeProperties().contains(name)) {
                var number = cardNumberSchema(null, null);
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(number.constraintFingerprints());
                constraints.put(PropertyValueKind.ENUM, "enum:" + base64("dart:core") + ":double:infinity,negativeInfinity");
                boolean required = name.startsWith("values");
                value = new CanvasPropertyContract(constraints.keySet(), required,
                        required ? Optional.of(name.equals("valuesStart") ? "integer:0" : "integer:1") : Optional.empty(),
                        number.numericBounds(), constraints);
            } else if (List.of("onChanged", "onChangeStart", "onChangeEnd", "labels", "mouseCursor").contains(name)) {
                String type = switch (name) {
                    case "labels" -> "RangeLabels";
                    case "mouseCursor" -> "WidgetStateProperty<MouseCursor?>";
                    default -> "ValueChanged<RangeValues>";
                };
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.put(PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                        + type + ":currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
                if (name.equals("labels")) {
                    constraints.put(PropertyValueKind.NULL, "any");
                }
                value = new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), Map.of(), constraints);
            } else if (RangeSliderWidgetPropertySchema.labelProperties().contains(name)) {
                value = propertySchema(PropertyValueKind.STRING);
            } else {
                value = slider.propertyContracts().get(new PropertyName(name));
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of());
    }

    private static CanvasProjection sliderProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        CanvasProjection checkbox = checkboxProjection();
        for (String name : SliderWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            if (SliderWidgetPropertySchema.overlayColorStateProperties().contains(name)) {
                var color = colorOrThemeProperty(name).getValue();
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(color.constraintFingerprints());
                constraints.put(PropertyValueKind.NULL, "any");
                value = new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), Map.of(), constraints);
            } else if (SliderWidgetPropertySchema.rangeProperties().contains(name)) {
                var number = cardNumberSchema(null, null);
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(number.constraintFingerprints());
                constraints.put(PropertyValueKind.ENUM, "enum:" + base64("dart:core") + ":double:infinity,negativeInfinity");
                if (name.equals("secondaryTrackValue")) {
                    constraints.put(PropertyValueKind.NULL, "any");
                }
                value = new CanvasPropertyContract(constraints.keySet(), name.equals("value"),
                        name.equals("value") ? Optional.of("integer:0") : Optional.empty(), number.numericBounds(), constraints);
            } else {
                value = switch (name) {
                    case "enabled" -> requiredDefaultProperty(name, "boolean:true", PropertyValueKind.BOOLEAN).getValue();
                    case "variant" -> requiredDefaultConstrainedProperty(name, "string:" + base64("standard"),
                            PropertyValueKind.STRING, "pattern:" + base64("(?:standard|adaptive)")).getValue();
                    case "onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "overlayColor", "focusNode" -> {
                        String type = switch (name) {
                            case "onChanged", "onChangeStart", "onChangeEnd" -> "ValueChanged<double>";
                            case "semanticFormatterCallback" -> "SemanticFormatterCallback";
                            case "overlayColor" -> "WidgetStateProperty<Color?>";
                            default -> "FocusNode";
                        };
                        yield constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                + type + ":currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
                    }
                    case "activeColor", "inactiveColor", "secondaryActiveColor", "thumbColor" -> colorOrThemeProperty(name).getValue();
                    case "divisions" -> {
                        var number = numericSchema(POSITIVE_INTEGER_BOUNDS, PropertyValueKind.INTEGER);
                        Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                        constraints.putAll(number.constraintFingerprints());
                        constraints.put(PropertyValueKind.NULL, "any");
                        yield new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), number.numericBounds(), constraints);
                    }
                    case "label" -> propertySchema(PropertyValueKind.STRING);
                    case "mouseCursor" -> checkbox.propertyContracts().get(new PropertyName(name));
                    case "allowedInteraction" -> enumPropertyForLibrary(name, MATERIAL_LIBRARY, "SliderInteraction", "tapAndSlide", "tapOnly", "slideOnly", "slideThumb").getValue();
                    case "padding" -> edgeInsetsProperty(name, true).getValue();
                    case "showValueIndicator" -> enumPropertyForLibrary(name, MATERIAL_LIBRARY, "ShowValueIndicator", "onlyForDiscrete", "onlyForContinuous", "always", "onDrag", "alwaysVisible", "never").getValue();
                    case "year2023" -> propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL);
                    default -> propertySchema(PropertyValueKind.BOOLEAN);
                };
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of());
    }

    private static CanvasProjection switchProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        CanvasProjection icon = iconProjection();
        CanvasProjection checkbox = checkboxProjection();
        for (String name : SwitchWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            Optional<String> iconName = SwitchWidgetPropertySchema.iconSourceName(name);
            if (iconName.isPresent()) {
                CanvasPropertyContract original = icon.propertyContracts().get(new PropertyName(iconName.orElseThrow()));
                value = new CanvasPropertyContract(original.acceptedKinds(), false, Optional.empty(),
                        original.numericBounds(), original.constraintFingerprints());
            } else if (SwitchWidgetPropertySchema.thumbIconLocalProperties().contains(name)) {
                value = constrainedSchema(PropertyValueKind.STRING, "pattern:" + base64("(?:icon|inherit)"));
            } else if (SwitchWidgetPropertySchema.colorFamilies().stream()
                    .anyMatch(family -> SwitchWidgetPropertySchema.colorStateProperties(family).contains(name))) {
                var color = colorOrThemeProperty(name).getValue();
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(color.constraintFingerprints());
                constraints.put(PropertyValueKind.NULL, "any");
                value = new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), Map.of(), constraints);
            } else if (SwitchWidgetPropertySchema.outlineWidthStateProperties().contains(name)) {
                var number = withPositiveInfinity(cardNumberSchema(null, null));
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(number.constraintFingerprints());
                constraints.put(PropertyValueKind.NULL, "any");
                value = new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), number.numericBounds(), constraints);
            } else {
                value = switch (name) {
                    case "value" -> requiredDefaultProperty(name, "boolean:false", PropertyValueKind.BOOLEAN).getValue();
                    case "enabled" -> requiredDefaultProperty(name, "boolean:true", PropertyValueKind.BOOLEAN).getValue();
                    case "variant" -> requiredDefaultConstrainedProperty(name, "string:" + base64("standard"),
                            PropertyValueKind.STRING, "pattern:" + base64("(?:standard|adaptive)")).getValue();
                    case "onChanged", "onFocusChange", "onActiveThumbImageError", "onInactiveThumbImageError",
                            "thumbColor", "trackColor", "trackOutlineColor", "overlayColor", "trackOutlineWidth", "thumbIcon", "focusNode" -> {
                        String type = switch (name) {
                            case "onChanged", "onFocusChange" -> "ValueChanged<bool>";
                            case "onActiveThumbImageError", "onInactiveThumbImageError" -> "ImageErrorListener";
                            case "thumbColor", "trackColor", "trackOutlineColor", "overlayColor" -> "WidgetStateProperty<Color?>";
                            case "trackOutlineWidth" -> "WidgetStateProperty<double?>";
                            case "thumbIcon" -> "WidgetStateProperty<Icon?>";
                            default -> "FocusNode";
                        };
                        yield constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                + type + ":currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
                    }
                    case "activeThumbImage", "inactiveThumbImage" -> constrainedSchema(PropertyValueKind.IMAGE_PROVIDER, IMAGE_PROVIDER_CONTRACT_FINGERPRINT);
                    case "activeColor", "activeThumbColor", "activeTrackColor", "inactiveThumbColor", "inactiveTrackColor", "focusColor", "hoverColor" -> colorOrThemeProperty(name).getValue();
                    case "splashRadius", "mouseCursor", "materialTapTargetSize" -> checkbox.propertyContracts().get(new PropertyName(name));
                    case "dragStartBehavior" -> enumPropertyForLibrary(name, GESTURES_LIBRARY, "DragStartBehavior", "down", "start").getValue();
                    case "padding" -> edgeInsetsProperty(name, true).getValue();
                    case "applyCupertinoTheme" -> propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL);
                    default -> propertySchema(PropertyValueKind.BOOLEAN);
                };
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of());
    }

    private static CanvasProjection radioProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        CanvasProjection shared = checkboxProjection();
        for (String name : RadioWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            if (List.of("fillColor", "overlayColor", "backgroundColor").stream()
                    .anyMatch(family -> RadioWidgetPropertySchema.colorStateProperties(family).contains(name))) {
                value = shared.propertyContracts().get(new PropertyName("fillColorDefault"));
            } else if (RadioWidgetPropertySchema.innerRadiusStateProperties().contains(name) || name.equals("splashRadius")) {
                var number = cardNumberSchema(null, null);
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(number.constraintFingerprints());
                constraints.put(PropertyValueKind.ENUM, "enum:" + base64("dart:core") + ":double:infinity,negativeInfinity");
                if (!name.equals("splashRadius")) constraints.put(PropertyValueKind.NULL, "any");
                value = new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), number.numericBounds(), constraints);
            } else if (RadioWidgetPropertySchema.sideLocalProperties().contains(name)) {
                value = shared.propertyContracts().get(new PropertyName(name));
            } else if (List.of("value", "groupValue", "valueType", "onChanged", "groupRegistry", "visualDensity", "innerRadius").contains(name)) {
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                Map<PropertyValueKind, CanvasNumericBounds> bounds = Map.of();
                String type = switch (name) {
                    case "value", "groupValue" -> "Object?";
                    case "valueType" -> "Type";
                    case "onChanged" -> "ValueChanged<Object?>";
                    case "groupRegistry" -> "RadioGroupRegistry<Object>";
                    case "visualDensity" -> "VisualDensity";
                    default -> "WidgetStateProperty<double?>";
                };
                constraints.put(PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                        + type + ":currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
                if (name.equals("value") || name.equals("groupValue")) {
                    var number = cardNumberSchema(null, null);
                    constraints.putAll(number.constraintFingerprints());
                    constraints.put(PropertyValueKind.ENUM, "enum:" + base64("dart:core") + ":double:infinity,nan,negativeInfinity");
                    bounds = number.numericBounds();
                    constraints.put(PropertyValueKind.STRING, "any");
                    constraints.put(PropertyValueKind.BOOLEAN, "any");
                }
                if (List.of("value", "groupValue", "onChanged", "groupRegistry").contains(name)) constraints.put(PropertyValueKind.NULL, "any");
                if (name.equals("onChanged")) constraints.put(PropertyValueKind.STRING, "pattern:" + base64("noop"));
                if (name.equals("valueType")) constraints.put(PropertyValueKind.STRING, "pattern:" + base64("(?:String|int|double|num|bool|Object)"));
                String creation = switch (name) {
                    case "value" -> "string:" + base64("option");
                    case "valueType" -> "string:" + base64("String");
                    case "onChanged" -> "string:" + base64("noop");
                    default -> null;
                };
                value = new CanvasPropertyContract(constraints.keySet(), List.of("value", "valueType").contains(name),
                        Optional.ofNullable(creation), bounds, constraints);
            } else {
                value = switch (name) {
                    case "enabled" -> propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL);
                    case "backgroundColor" -> shared.propertyContracts().get(new PropertyName("fillColor"));
                    case "toggleable", "useCupertinoCheckmarkStyle", "nullableValueType" -> propertySchema(PropertyValueKind.BOOLEAN);
                    default -> shared.propertyContracts().get(new PropertyName(name));
                };
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of());
    }

    private static CanvasProjection tooltipThemeProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("data", listTileReference("TooltipThemeData"));
        var excluded = Set.of("message", "richMessage", "ignorePointer", "enableTapToDismiss",
                "onTriggered", "mouseCursor", "positionDelegate");
        tooltipProjection().propertyContracts().forEach((name, contract) -> {
            if (!excluded.contains(name.value())) properties.put(name.value(), contract);
        });
        return projection(properties, Map.of("child", singleSlotSchema(true, 1)));
    }

    private static CanvasProjection tooltipProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        properties.put("message", new CanvasPropertyContract(Set.of(PropertyValueKind.STRING, PropertyValueKind.NULL),
                false, Optional.of("string:" + base64("Tooltip")), Map.of(),
                Map.of(PropertyValueKind.STRING, "any", PropertyValueKind.NULL, "any")));
        properties.put("richMessage", expansionNullable(listTileReference("InlineSpan")));
        var number = listTileProjection().propertyContracts().get(new PropertyName("minTileHeight"));
        properties.put("height", number);
        properties.put("verticalOffset", number);
        properties.put("constraints", expansionNullable(listTileAddReference(
                constrainedSchema(PropertyValueKind.BOX_CONSTRAINTS, "boxConstraints:v2:finiteOrPositiveInfinity"), "BoxConstraints")));
        for (String name : List.of("padding", "margin")) {
            properties.put(name, expansionNullable(listTileProjection().propertyContracts().get(new PropertyName("contentPadding"))));
        }
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableFeedback", "ignorePointer")) {
            properties.put(name, propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL));
        }
        properties.put("enableTapToDismiss", propertySchema(PropertyValueKind.BOOLEAN));
        properties.put("decoration", expansionNullable(listTileAddReference(constrainedSchema(
                PropertyValueKind.BOX_DECORATION, boxDecorationFingerprint(REVIEWED_COLOR_THEME_TOKENS)), "Decoration")));
        properties.put("textStyle", expansionNullable(listTileReference("TextStyle")));
        properties.put("textAlign", expansionNullable(enumProperty("textAlign", "TextAlign", "left", "right", "center", "justify", "start", "end").getValue()));
        var duration = expansionNullable(listTileAddReference(numericSchema(
                Map.of(PropertyValueKind.INTEGER, bounds(new BigDecimal(DartNumericLiterals.MIN_PORTABLE_INTEGER), true,
                        new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER), true)), PropertyValueKind.INTEGER), "Duration"));
        for (String name : List.of("waitDurationUs", "showDurationUs", "exitDurationUs")) properties.put(name, duration);
        properties.put("triggerMode", expansionNullable(enumProperty("triggerMode", "TooltipTriggerMode", "manual", "longPress", "tap").getValue()));
        var callback = listTileReference("TooltipTriggeredCallback");
        Map<PropertyValueKind, String> callbackConstraints = new java.util.EnumMap<>(PropertyValueKind.class);
        callbackConstraints.putAll(callback.constraintFingerprints());
        callbackConstraints.put(PropertyValueKind.STRING, "pattern:" + base64("noop"));
        callbackConstraints.put(PropertyValueKind.NULL, "any");
        properties.put("onTriggered", new CanvasPropertyContract(callbackConstraints.keySet(), false, Optional.empty(), Map.of(), callbackConstraints));
        properties.put("mouseCursor", expansionNullable(iconButtonProjection().propertyContracts().get(new PropertyName("mouseCursor"))));
        properties.put("positionDelegate", expansionNullable(listTileReference("TooltipPositionDelegate")));
        appendTextStyleProjection(properties, "textStyle");
        return projection(properties, Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection expansionTileProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        var tile = listTileProjection();
        var shapes = cardProjection();
        properties.put("onExpansionChanged", tile.propertyContracts().get(new PropertyName("onFocusChange")));
        for (String name : List.of("showTrailingIcon", "initiallyExpanded", "maintainState", "enabled", "internalAddSemanticForOnTap")) {
            properties.put(name, propertySchema(PropertyValueKind.BOOLEAN));
        }
        for (String name : List.of("dense", "enableFeedback")) properties.put(name, propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL));
        for (String name : List.of("tilePadding", "childrenPadding")) {
            properties.put(name, expansionNullable(tile.propertyContracts().get(new PropertyName("contentPadding"))));
        }
        properties.put("expandedCrossAxisAlignment", expansionNullable(enumProperty("expandedCrossAxisAlignment", "CrossAxisAlignment", "start", "end", "center", "stretch").getValue()));
        properties.put("expandedAlignment", expansionNullable(listTileAddReference(constrainedSchema(PropertyValueKind.ALIGNMENT_GEOMETRY, "alignmentGeometry"), "AlignmentGeometry")));
        for (String name : List.of("backgroundColor", "collapsedBackgroundColor", "textColor", "collapsedTextColor", "iconColor", "collapsedIconColor", "splashColor")) {
            properties.put(name, expansionNullable(listTileAddReference(colorOrThemeProperty(name).getValue(), "Color")));
        }
        for (String family : List.of("shape", "collapsedShape")) {
            properties.put(family, expansionNullable(listTileReference("ShapeBorder")));
            for (String name : CardWidgetPropertySchema.definitions().keySet()) {
                if (name.startsWith("shape") && !name.equals("shape")) {
                    properties.put(family + name.substring(5), shapes.propertyContracts().get(new PropertyName(name)));
                }
            }
        }
        properties.put("clipBehavior", expansionNullable(enumProperty("clipBehavior", "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer").getValue()));
        properties.put("controlAffinity", expansionNullable(materialEnumProperty("controlAffinity", "ListTileControlAffinity", "leading", "trailing", "platform").getValue()));
        properties.put("controller", expansionNullable(listTileReference("ExpansibleController")));
        properties.put("visualDensity", expansionNullable(listTileReference("VisualDensity")));
        properties.put("statesController", expansionNullable(listTileReference("WidgetStatesController")));
        properties.put("minTileHeight", tile.propertyContracts().get(new PropertyName("minTileHeight")));
        for (String axis : List.of("Horizontal", "Vertical")) properties.put("visualDensity" + axis, tile.propertyContracts().get(new PropertyName("visualDensity" + axis)));
        properties.put("expansionAnimationStyle", expansionNullable(listTileAddReference(stringPatternProperty("expansionAnimationStyle", "noAnimation").getValue(), "AnimationStyle")));
        var duration = expansionNullable(listTileAddReference(numericSchema(
                Map.of(PropertyValueKind.INTEGER, bounds(new BigDecimal(DartNumericLiterals.MIN_PORTABLE_INTEGER), true,
                        new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER), true)), PropertyValueKind.INTEGER), "Duration"));
        for (String name : List.of("DurationUs", "ReverseDurationUs")) properties.put("expansionAnimationStyle" + name, duration);
        String curves = "(?:linear|decelerate|fastLinearToSlowEaseIn|fastEaseInToSlowEaseOut|ease|easeIn|easeInToLinear|easeInSine|easeInQuad|easeInCubic|easeInQuart|easeInQuint|easeInExpo|easeInCirc|easeInBack|easeOut|linearToEaseOut|easeOutSine|easeOutQuad|easeOutCubic|easeOutQuart|easeOutQuint|easeOutExpo|easeOutCirc|easeOutBack|easeInOut|easeInOutSine|easeInOutQuad|easeInOutCubic|easeInOutCubicEmphasized|easeInOutQuart|easeInOutQuint|easeInOutExpo|easeInOutCirc|easeInOutBack|fastOutSlowIn|slowMiddle|bounceIn|bounceOut|bounceInOut|elasticIn|elasticOut|elasticInOut)";
        var curve = expansionNullable(listTileAddReference(stringPatternProperty("curve", curves).getValue(), "Curve"));
        for (String name : List.of("Curve", "ReverseCurve")) properties.put("expansionAnimationStyle" + name, curve);
        return projection(properties, Map.of("title", singleSlotSchema(true, 1), "leading", singleSlotSchema(false, 0),
                "subtitle", singleSlotSchema(false, 0), "trailing", singleSlotSchema(false, 0), "children", listSlotSchema(false, 0, 10_000)));
    }

    private static CanvasPropertyContract expansionNullable(CanvasPropertyContract property) {
        Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
        constraints.putAll(property.constraintFingerprints());
        constraints.put(PropertyValueKind.NULL, "any");
        return new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), property.numericBounds(), constraints);
    }

    private static CanvasProjection radioListTileProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        var tile = checkboxListTileProjection();
        var radio = radioProjection();
        for (String name : RadioListTileWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            if (List.of("value", "groupValue", "onChanged", "valueType", "nullableValueType").contains(name)) {
                value = radio.propertyContracts().get(new PropertyName(name));
            } else if (name.equals("radioScaleFactor")) {
                value = tile.propertyContracts().get(new PropertyName("checkboxScaleFactor"));
            } else if (name.startsWith("radio")) {
                String source = Character.toLowerCase(name.charAt(5)) + name.substring(6);
                value = radio.propertyContracts().get(new PropertyName(source));
            } else {
                value = Optional.ofNullable(tile.propertyContracts().get(new PropertyName(name)))
                        .orElseGet(() -> radio.propertyContracts().get(new PropertyName(name)));
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of("title", singleSlotSchema(false, 0),
                "subtitle", singleSlotSchema(false, 0), "secondary", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection switchListTileProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        var tile = checkboxListTileProjection();
        var control = switchProjection();
        for (String name : SwitchListTileWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            if (SwitchListTileWidgetPropertySchema.colorProperties().contains(name)) {
                value = listTileAddReference(colorOrThemeProperty(name).getValue(), "Color");
            } else if (List.of("onChanged", "onActiveThumbImageError", "onInactiveThumbImageError").contains(name)) {
                var reference = listTileReference(name.equals("onChanged") ? "ValueChanged<bool>" : "ImageErrorListener");
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(reference.constraintFingerprints());
                constraints.put(PropertyValueKind.STRING, "pattern:" + base64("noop"));
                constraints.put(PropertyValueKind.NULL, "any");
                value = new CanvasPropertyContract(constraints.keySet(), name.equals("onChanged"),
                        name.equals("onChanged") ? Optional.of("string:" + base64("noop")) : Optional.empty(), Map.of(), constraints);
            } else if (name.equals("value")) {
                value = control.propertyContracts().get(new PropertyName(name));
            } else {
                value = Optional.ofNullable(tile.propertyContracts().get(new PropertyName(name)))
                        .orElseGet(() -> control.propertyContracts().get(new PropertyName(name)));
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of("title", singleSlotSchema(false, 0),
                "subtitle", singleSlotSchema(false, 0), "secondary", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection checkboxListTileProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        var tile = listTileProjection();
        var checkbox = checkboxProjection();
        var shapes = cardProjection();
        for (String name : CheckboxListTileWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            var shapeName = CheckboxListTileWidgetPropertySchema.shapeSourceName(name);
            if (shapeName.isPresent()) {
                value = shapes.propertyContracts().get(new PropertyName(shapeName.orElseThrow()));
            } else if (CheckboxListTileWidgetPropertySchema.colorProperties().contains(name)) {
                value = listTileAddReference(colorOrThemeProperty(name).getValue(), "Color");
            } else if (CheckboxListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)) {
                value = checkbox.propertyContracts().get(new PropertyName("mouseCursor"));
            } else if (List.of("splashRadius", "checkboxScaleFactor").contains(name)) {
                var number = cardNumberSchema(null, null);
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(number.constraintFingerprints());
                constraints.put(PropertyValueKind.ENUM, "enum:" + base64("dart:core") + ":double:infinity,nan,negativeInfinity");
                if (name.equals("splashRadius")) constraints.put(PropertyValueKind.NULL, "any");
                value = new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), number.numericBounds(), constraints);
            } else {
                value = switch (name) {
                    case "onChanged" -> {
                        var reference = listTileReference("ValueChanged<bool?>");
                        Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                        constraints.putAll(reference.constraintFingerprints());
                        constraints.put(PropertyValueKind.STRING, "pattern:" + base64("noop"));
                        constraints.put(PropertyValueKind.NULL, "any");
                        yield new CanvasPropertyContract(constraints.keySet(), true, Optional.of("string:" + base64("noop")), Map.of(), constraints);
                    }
                    case "checkboxShape" -> listTileReference("OutlinedBorder");
                    case "enabled" -> new CanvasPropertyContract(Set.of(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL), false,
                            Optional.empty(), Map.of(), Map.of(PropertyValueKind.BOOLEAN, "any", PropertyValueKind.NULL, "any"));
                    case "controlAffinity" -> materialEnumProperty(name, "ListTileControlAffinity", "leading", "trailing", "platform").getValue();
                    case "checkboxSemanticLabel" -> propertySchema(PropertyValueKind.STRING);
                    case "variant" -> new CanvasPropertyContract(Set.of(PropertyValueKind.STRING), true, Optional.of("string:" + base64("standard")),
                            Map.of(), Map.of(PropertyValueKind.STRING, "pattern:" + base64("(?:standard|adaptive)")));
                    default -> Optional.ofNullable(tile.propertyContracts().get(new PropertyName(name)))
                            .orElseGet(() -> checkbox.propertyContracts().get(new PropertyName(name)));
                };
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of("title", singleSlotSchema(false, 0), "subtitle", singleSlotSchema(false, 0), "secondary", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection listTileProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        var text = badgeProjection();
        var shapes = cardProjection();
        var checkbox = checkboxProjection();
        for (String name : ListTileWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            var family = ListTileWidgetPropertySchema.textStyleFamily(new PropertyName(name));
            if (family.isPresent()) {
                value = text.propertyContracts().get(new PropertyName("textStyle" + name.substring(family.orElseThrow().length())));
            } else if (ListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name)) {
                value = shapes.propertyContracts().get(new PropertyName(name));
            } else if (ListTileWidgetPropertySchema.stateColorFamilies().stream().anyMatch(key -> ListTileWidgetPropertySchema.colorStateProperties(key).contains(name))) {
                value = colorOrThemeProperty(name).getValue();
            } else if (ListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name) || name.equals("mouseCursor")) {
                value = checkbox.propertyContracts().get(new PropertyName("mouseCursor"));
            } else if (ListTileWidgetPropertySchema.colorProperties().contains(name)) {
                value = listTileAddReference(colorOrThemeProperty(name).getValue(), "Color");
            } else if (ListTileWidgetPropertySchema.geometryProperties().contains(name)) {
                var number = cardNumberSchema(null, null);
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(number.constraintFingerprints());
                constraints.put(PropertyValueKind.ENUM, "enum:" + base64("dart:core") + ":double:infinity,nan,negativeInfinity");
                constraints.put(PropertyValueKind.NULL, "any");
                value = new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), number.numericBounds(), constraints);
            } else {
                value = switch (name) {
                    case "visualDensity" -> listTileReference("VisualDensity");
                    case "visualDensityHorizontal", "visualDensityVertical" -> checkbox.propertyContracts().get(new PropertyName(name));
                    case "shape" -> listTileReference("ShapeBorder");
                    case "titleTextStyle", "subtitleTextStyle", "leadingAndTrailingTextStyle" -> listTileReference("TextStyle");
                    case "contentPadding" -> listTileAddReference(edgeInsetsProperty(name, false).getValue(), "EdgeInsetsGeometry");
                    case "style" -> materialEnumProperty(name, "ListTileStyle", "list", "drawer").getValue();
                    case "titleAlignment" -> materialEnumProperty(name, "ListTileTitleAlignment", "threeLine", "titleHeight", "top", "center", "bottom").getValue();
                    case "focusNode" -> listTileReference("FocusNode");
                    case "statesController" -> listTileReference("WidgetStatesController");
                    case "isThreeLine", "dense", "enableFeedback" -> new CanvasPropertyContract(Set.of(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL), false,
                            Optional.empty(), Map.of(), Map.of(PropertyValueKind.BOOLEAN, "any", PropertyValueKind.NULL, "any"));
                    case "onTap", "onLongPress", "onFocusChange" -> {
                        var reference = listTileReference(name.equals("onFocusChange") ? "ValueChanged<bool>" : "VoidCallback");
                        Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                        constraints.putAll(reference.constraintFingerprints());
                        constraints.put(PropertyValueKind.STRING, "pattern:" + base64("noop"));
                        constraints.put(PropertyValueKind.NULL, "any");
                        yield new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), Map.of(), constraints);
                    }
                    default -> propertySchema(PropertyValueKind.BOOLEAN);
                };
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of("leading", singleSlotSchema(false, 0), "title", singleSlotSchema(false, 0),
                "subtitle", singleSlotSchema(false, 0), "trailing", singleSlotSchema(false, 0)));
    }

    private static CanvasPropertyContract listTileReference(String type) {
        return constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX + type
                + ":currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
    }

    private static CanvasPropertyContract listTileAddReference(CanvasPropertyContract local, String type) {
        Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
        constraints.putAll(local.constraintFingerprints());
        constraints.putAll(listTileReference(type).constraintFingerprints());
        return new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), local.numericBounds(), constraints);
    }

    private static CanvasProjection radioGroupProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        CanvasProjection shared = radioProjection();
        for (String name : RadioGroupWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value = shared.propertyContracts().get(new PropertyName(name));
            if (name.equals("onChanged")) {
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(value.constraintFingerprints());
                constraints.remove(PropertyValueKind.NULL);
                value = new CanvasPropertyContract(constraints.keySet(), true,
                        Optional.of("string:" + base64("noop")), Map.of(), constraints);
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of("child", singleSlotSchema(true, 1)));
    }

    private static CanvasProjection checkboxProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        CanvasProjection shapes = cardProjection();
        CanvasProjection icon = iconButtonProjection();
        for (String name : CheckboxWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            if (CheckboxWidgetPropertySchema.builtInShapePropertyNames().contains(name)) {
                value = shapes.propertyContracts().get(new PropertyName(name));
            } else if (CheckboxWidgetPropertySchema.colorStateProperties("fillColor").contains(name)
                    || CheckboxWidgetPropertySchema.colorStateProperties("overlayColor").contains(name)) {
                var color = colorOrThemeProperty(name).getValue();
                Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                constraints.putAll(color.constraintFingerprints());
                constraints.put(PropertyValueKind.NULL, "any");
                value = new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), Map.of(), constraints);
            } else if (CheckboxWidgetPropertySchema.sideLocalProperties().contains(name) && !name.equals("sideStateful")) {
                value = name.endsWith("Mode") ? constrainedSchema(PropertyValueKind.STRING, "pattern:" + base64("(?:border|inherit)"))
                        : name.endsWith("Color") ? colorOrThemeProperty(name).getValue()
                        : name.endsWith("Width") ? cardNumberSchema(BigDecimal.ZERO, null)
                        : name.endsWith("Style") ? enumProperty(name, "BorderStyle", "none", "solid").getValue()
                        : cardNumberSchema(null, null);
            } else {
                value = switch (name) {
                    case "value" -> requiredDefaultProperty(name, "boolean:false", PropertyValueKind.BOOLEAN, PropertyValueKind.NULL).getValue();
                    case "enabled" -> requiredDefaultProperty(name, "boolean:true", PropertyValueKind.BOOLEAN).getValue();
                    case "variant" -> requiredDefaultConstrainedProperty(name, "string:" + base64("standard"),
                            PropertyValueKind.STRING, "pattern:" + base64("(?:standard|adaptive)")).getValue();
                    case "onChanged", "fillColor", "overlayColor", "shape", "side", "focusNode" -> {
                        String type = switch (name) {
                            case "onChanged" -> "ValueChanged<bool?>";
                            case "fillColor", "overlayColor" -> "WidgetStateProperty<Color?>";
                            case "shape" -> "OutlinedBorder";
                            case "side" -> "BorderSide";
                            default -> "FocusNode";
                        };
                        yield constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                + type + ":currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
                    }
                    case "activeColor", "checkColor", "focusColor", "hoverColor" -> colorOrThemeProperty(name).getValue();
                    case "splashRadius" -> withPositiveInfinity(cardNumberSchema(null, null));
                    case "visualDensityHorizontal", "visualDensityVertical", "mouseCursor" -> icon.propertyContracts().get(new PropertyName(name));
                    case "materialTapTargetSize" -> materialEnumProperty(name, "MaterialTapTargetSize", "padded", "shrinkWrap").getValue();
                    case "semanticLabel" -> propertySchema(PropertyValueKind.STRING);
                    default -> propertySchema(PropertyValueKind.BOOLEAN);
                };
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of());
    }

    private static CanvasProjection iconButtonProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        CanvasProjection shared = fullStyleButtonProjection("TextButton");
        CanvasProjection fab = floatingActionButtonProjection();
        for (String name : IconButtonWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value;
            if (IconButtonWidgetPropertySchema.localStyleProperties().contains(name)) {
                value = shared.propertyContracts().get(new PropertyName(name));
            } else {
                value = switch (name) {
                    case "iconSize" -> withPositiveInfinity(cardNumberSchema(null, null));
                    case "splashRadius" -> withPositiveInfinity(new CanvasPropertyContract(
                            Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE), false, Optional.empty(),
                            Map.of(PropertyValueKind.INTEGER, bounds(BigDecimal.ONE, true, new BigDecimal("9007199254740991"), true),
                                    PropertyValueKind.DOUBLE, bounds(BigDecimal.ZERO, false, null, true)),
                            rangeConstraintFingerprints(Map.of(
                                    PropertyValueKind.INTEGER, bounds(BigDecimal.ONE, true, new BigDecimal("9007199254740991"), true),
                                    PropertyValueKind.DOUBLE, bounds(BigDecimal.ZERO, false, null, true)))));
                    case "visualDensityHorizontal", "visualDensityVertical" -> shared.propertyContracts().get(new PropertyName("style" + Character.toUpperCase(name.charAt(0)) + name.substring(1)));
                    case "padding" -> edgeInsetsProperty(name, true).getValue();
                    case "alignment" -> constrainedSchema(PropertyValueKind.ALIGNMENT_GEOMETRY, "alignmentGeometry");
                    case "color", "focusColor", "hoverColor", "highlightColor", "splashColor", "disabledColor" -> colorOrThemeProperty(name).getValue();
                    case "onPressed", "onLongPress", "onHover", "focusNode", "statesController", "style" -> shared.propertyContracts().get(new PropertyName(name));
                    case "mouseCursor" -> fab.propertyContracts().get(new PropertyName(name));
                    case "constraints" -> constrainedSchema(PropertyValueKind.BOX_CONSTRAINTS, "boxConstraints:v2:finiteOrPositiveInfinity");
                    case "isSelected" -> propertySchema(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL);
                    case "variant" -> requiredDefaultConstrainedProperty(name, "string:" + base64("standard"),
                            PropertyValueKind.STRING, "pattern:" + base64("(?:standard|filled|filledTonal|outlined)")).getValue();
                    case "enabled" -> requiredDefaultProperty(name, "boolean:true", PropertyValueKind.BOOLEAN).getValue();
                    case "tooltip" -> propertySchema(PropertyValueKind.STRING);
                    default -> propertySchema(PropertyValueKind.BOOLEAN);
                };
            }
            properties.put(name, value);
        }
        return projection(properties, Map.of("icon", singleSlotSchema(true, 1), "selectedIcon", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection floatingActionButtonProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        CanvasProjection shapes = cardProjection();
        for (String name : FloatingActionButtonWidgetPropertySchema.definitions().keySet()) {
            if (FloatingActionButtonWidgetPropertySchema.isTextStyleProperty(new PropertyName(name))) {
                continue;
            }
            CanvasPropertyContract value;
            if (name.equals("shape") || CardWidgetPropertySchema.builtInShapePropertyNames().contains(name)) {
                value = shapes.propertyContracts().get(new PropertyName(name));
            } else {
                value = switch (name) {
                    case "foregroundColor", "backgroundColor", "focusColor", "hoverColor", "splashColor" -> colorOrThemeProperty(name).getValue();
                    case "enabled" -> new CanvasPropertyContract(Set.of(PropertyValueKind.BOOLEAN), true,
                            Optional.of("boolean:true"), Map.of(), Map.of(PropertyValueKind.BOOLEAN, "any"));
                    case "mini", "autofocus", "isExtended", "enableFeedback" -> propertySchema(PropertyValueKind.BOOLEAN);
                    case "variant" -> new CanvasPropertyContract(Set.of(PropertyValueKind.STRING), true,
                            Optional.of("string:" + base64("standard")), Map.of(),
                            Map.of(PropertyValueKind.STRING, "pattern:" + base64("(?:standard|small|large|extended)")));
                    case "tooltip" -> propertySchema(PropertyValueKind.STRING);
                    case "heroTag" -> {
                        CanvasPropertyContract number = cardNumberSchema(null, null);
                        Map<PropertyValueKind, String> constraints = new java.util.EnumMap<>(PropertyValueKind.class);
                        constraints.putAll(number.constraintFingerprints());
                        constraints.put(PropertyValueKind.NULL, "any");
                        constraints.put(PropertyValueKind.STRING, "any");
                        constraints.put(PropertyValueKind.BOOLEAN, "any");
                        constraints.put(PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                + "Object:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
                        yield new CanvasPropertyContract(constraints.keySet(), false, Optional.empty(), number.numericBounds(), constraints);
                    }
                    case "onPressed", "focusNode" -> constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                            DART_OBJECT_REFERENCE_CONTRACT_PREFIX + (name.equals("onPressed") ? "VoidCallback" : "FocusNode")
                            + ":currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
                    case "mouseCursor" -> new CanvasPropertyContract(Set.of(PropertyValueKind.STRING, PropertyValueKind.DART_OBJECT_REFERENCE),
                            false, Optional.empty(), Map.of(), Map.of(
                                    PropertyValueKind.STRING, "pattern:" + base64(DefaultSelectionStyleWidgetPropertySchema.mouseCursorPattern()),
                                    PropertyValueKind.DART_OBJECT_REFERENCE, DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                                            + "MouseCursor:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)"));
                    case "clipBehavior" -> enumProperty(name, "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer").getValue();
                    case "materialTapTargetSize" -> materialEnumProperty(name, "MaterialTapTargetSize", "padded", "shrinkWrap").getValue();
                    case "extendedPadding" -> edgeInsetsProperty(name, true).getValue();
                    default -> withPositiveInfinity(cardNumberSchema(name.equals("extendedIconLabelSpacing") ? null : BigDecimal.ZERO, null));
                };
            }
            properties.put(name, value);
        }
        appendTextStyleProjection(properties, "extendedTextStyle");
        return projection(properties, Map.of("child", singleSlotSchema(true, 0), "icon", singleSlotSchema(false, 0)));
    }

    private static CanvasProjection cardProjection() {
        Map<String, CanvasPropertyContract> properties = new LinkedHashMap<>();
        for (String name : CardWidgetPropertySchema.definitions().keySet()) {
            CanvasPropertyContract value = switch (name) {
                case "color", "shadowColor", "surfaceTintColor", "shapeSideColor" -> colorOrThemeProperty(name).getValue();
                case "borderOnForeground", "semanticContainer" -> propertySchema(PropertyValueKind.BOOLEAN);
                case "margin" -> edgeInsetsProperty(name, true).getValue();
                case "clipBehavior" -> enumProperty(name, "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer").getValue();
                case "shapeSideStyle" -> enumProperty(name, "BorderStyle", "none", "solid").getValue();
                case "variant" -> new CanvasPropertyContract(Set.of(PropertyValueKind.STRING), true,
                        Optional.of("string:" + base64("elevated")), Map.of(),
                        Map.of(PropertyValueKind.STRING, "pattern:" + base64("(?:elevated|filled|outlined)")));
                case "shapeKind" -> stringPatternProperty(name, "(?:" + String.join("|", CardWidgetPropertySchema.shapeKinds()) + ")").getValue();
                case "shape" -> constrainedSchema(PropertyValueKind.DART_OBJECT_REFERENCE,
                        DART_OBJECT_REFERENCE_CONTRACT_PREFIX + "ShapeBorder:currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)");
                case "shapeRadius" -> constrainedSchema(PropertyValueKind.BORDER_RADIUS, BORDER_RADIUS_CONTRACT_FINGERPRINT);
                case "elevation", "shapeSideWidth" -> numericSchema(NON_NEGATIVE_NUMBER_BOUNDS, PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE);
                case "shapePoints" -> cardNumberSchema(BigDecimal.valueOf(2), null);
                case "shapeCircleEccentricity", "shapeInnerRadiusRatio", "shapePointRounding", "shapeValleyRounding", "shapeSquash",
                        "shapeStartSize", "shapeEndSize", "shapeTopSize", "shapeBottomSize" -> cardNumberSchema(BigDecimal.ZERO, BigDecimal.ONE);
                default -> cardNumberSchema(null, null);
            };
            properties.put(name, value);
        }
        return projection(properties, Map.of("child", singleSlotSchema(false, 0)));
    }

    private static CanvasPropertyContract cardNumberSchema(BigDecimal minimum, BigDecimal maximum) {
        return numericSchema(Map.of(
                PropertyValueKind.INTEGER, bounds(minimum == null ? new BigDecimal(DartNumericLiterals.MIN_PORTABLE_INTEGER) : minimum, true,
                        maximum == null ? new BigDecimal(DartNumericLiterals.MAX_PORTABLE_INTEGER) : maximum, true),
                PropertyValueKind.DOUBLE, bounds(minimum, true, maximum, true)), PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE);
    }

    private static CanvasPropertyContract numericSchema(
            Map<PropertyValueKind, CanvasNumericBounds> numericBounds,
            PropertyValueKind... kinds) {
        return new CanvasPropertyContract(
                Set.of(kinds),
                false,
                Optional.empty(),
                numericBounds,
                rangeConstraintFingerprints(numericBounds));
    }

    private static CanvasPropertyContract requiredDefaultEdgeInsetsSchema(
            String creationDefaultFingerprint,
            boolean nonNegative) {
        CanvasNumericBounds bounds = nonNegative
                ? NON_NEGATIVE_NUMERIC
                : UNBOUNDED_NUMERIC;
        return new CanvasPropertyContract(
                Set.of(PropertyValueKind.EDGE_INSETS),
                true,
                Optional.of(creationDefaultFingerprint),
                Map.of(PropertyValueKind.EDGE_INSETS, bounds),
                Map.of(
                        PropertyValueKind.EDGE_INSETS,
                        "edgeInsets:" + (nonNegative ? '1' : '0')
                        + ':' + bounds.fingerprint()));
    }

    private static CanvasPropertyContract physicalEdgeInsetsSchema(
            boolean nonNegative) {
        CanvasNumericBounds bounds = nonNegative
                ? NON_NEGATIVE_NUMERIC
                : UNBOUNDED_NUMERIC;
        return new CanvasPropertyContract(
                Set.of(PropertyValueKind.EDGE_INSETS),
                false,
                Optional.empty(),
                Map.of(PropertyValueKind.EDGE_INSETS, bounds),
                Map.of(
                        PropertyValueKind.EDGE_INSETS,
                        "edgeInsetsPhysical:" + (nonNegative ? '1' : '0')
                        + ':' + bounds.fingerprint()));
    }

    private static Map.Entry<String, CanvasPropertyContract> property(
            String name,
            PropertyValueKind... kinds) {
        return Map.entry(name, propertySchema(kinds));
    }

    private static Map.Entry<String, CanvasPropertyContract> enumProperty(
            String name,
            String enumType,
            String... values) {
        return enumPropertyForLibrary(name, WIDGETS_LIBRARY, enumType, values);
    }

    private static Map.Entry<String, CanvasPropertyContract> materialEnumProperty(
            String name,
            String enumType,
            String... values) {
        return enumPropertyForLibrary(name, MATERIAL_LIBRARY, enumType, values);
    }

    private static Map.Entry<String, CanvasPropertyContract> enumPropertyForLibrary(
            String name,
            String library,
            String enumType,
            String... values) {
        String fingerprint = "enum:" + base64(library) + ':'
                + enumType + ':' + java.util.Arrays.stream(values).sorted()
                        .collect(Collectors.joining(","));
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.ENUM, fingerprint));
    }

    private static Map.Entry<String, CanvasPropertyContract> stringLengthProperty(
            String name,
            int minimum,
            int maximum) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.STRING,
                "length:" + minimum + ':' + maximum));
    }

    private static Map.Entry<String, CanvasPropertyContract> stringPatternProperty(
            String name,
            String pattern) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.STRING, "pattern:" + base64(pattern)));
    }

    private static Map.Entry<String, CanvasPropertyContract> edgeInsetsProperty(
            String name,
            boolean nonNegative) {
        CanvasNumericBounds numeric = nonNegative
                ? NON_NEGATIVE_NUMERIC : UNBOUNDED_NUMERIC;
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(PropertyValueKind.EDGE_INSETS),
                false,
                Optional.empty(),
                Map.of(PropertyValueKind.EDGE_INSETS, numeric),
                Map.of(
                        PropertyValueKind.EDGE_INSETS,
                        "edgeInsets:" + (nonNegative ? '1' : '0')
                        + ':' + numeric.fingerprint())));
    }

    private static Map.Entry<String, CanvasPropertyContract> themeTokenProperty(
            String name,
            List<String> tokens) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.THEME_TOKEN,
                "tokens:" + tokens.stream().sorted()
                        .collect(Collectors.joining(","))));
    }

    private static Map.Entry<String, CanvasPropertyContract> colorOrThemeProperty(
            String name) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                false,
                Optional.empty(),
                Map.of(),
                Map.of(
                        PropertyValueKind.COLOR, "any",
                        PropertyValueKind.THEME_TOKEN,
                        "tokens:" + REVIEWED_COLOR_THEME_TOKENS.stream().sorted()
                                .collect(Collectors.joining(",")))));
    }

    private static Map.Entry<String, CanvasPropertyContract>
            requiredDefaultColorOrThemeProperty(
                    String name,
                    String creationDefaultFingerprint) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                true,
                Optional.of(creationDefaultFingerprint),
                Map.of(),
                Map.of(
                        PropertyValueKind.COLOR, "any",
                        PropertyValueKind.THEME_TOKEN,
                        "tokens:" + REVIEWED_COLOR_THEME_TOKENS.stream().sorted()
                                .collect(Collectors.joining(",")))));
    }

    private static Map.Entry<String, CanvasPropertyContract> paintProperty(
            String name) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.PAINT,
                "paintTokens:" + REVIEWED_COLOR_THEME_TOKENS.stream().sorted()
                        .collect(Collectors.joining(","))));
    }

    private static Map.Entry<String, CanvasPropertyContract> shadowProperty(
            String name) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.SHADOW_LIST,
                "shadowTokens:" + REVIEWED_COLOR_THEME_TOKENS.stream().sorted()
                        .collect(Collectors.joining(","))));
    }

    private static Map.Entry<String, CanvasPropertyContract> fontVariationProperty(
            String name) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.FONT_VARIATION_LIST, "fontVariationList"));
    }

    private static CanvasPropertyContract constrainedSchema(
            PropertyValueKind kind,
            String fingerprint) {
        return new CanvasPropertyContract(
                Set.of(kind),
                false,
                Optional.empty(),
                Map.of(),
                Map.of(kind, fingerprint));
    }

    private static Map.Entry<String, CanvasPropertyContract> numericProperty(
            String name,
            Map<PropertyValueKind, CanvasNumericBounds> numericBounds,
            PropertyValueKind... kinds) {
        return Map.entry(name, numericSchema(numericBounds, kinds));
    }

    private static Map.Entry<String, CanvasPropertyContract>
            requiredDefaultNumericProperty(
                    String name,
                    String creationDefaultFingerprint,
                    Map<PropertyValueKind, CanvasNumericBounds> numericBounds,
                    PropertyValueKind... kinds) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kinds),
                true,
                Optional.of(creationDefaultFingerprint),
                numericBounds,
                rangeConstraintFingerprints(numericBounds)));
    }

    private static Map.Entry<String, CanvasPropertyContract> requiredDefaultProperty(
            String name,
            String creationDefaultFingerprint,
            PropertyValueKind... kinds) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kinds),
                true,
                Optional.of(creationDefaultFingerprint),
                Map.of(),
                anyConstraintFingerprints(kinds)));
    }

    private static Map.Entry<String, CanvasPropertyContract> optionalDefaultProperty(
            String name,
            String creationDefaultFingerprint,
            PropertyValueKind... kinds) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kinds),
                false,
                Optional.of(creationDefaultFingerprint),
                Map.of(),
                anyConstraintFingerprints(kinds)));
    }

    private static Map.Entry<String, CanvasPropertyContract> callbackProperty(
            String name) {
        return Map.entry(name, constrainedSchema(
                PropertyValueKind.CALLBACK, "callbackReference"));
    }

    private static Map.Entry<String, CanvasPropertyContract>
            requiredDefaultConstrainedProperty(
                    String name,
                    String creationDefaultFingerprint,
                    PropertyValueKind kind,
                    String constraintFingerprint) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kind),
                true,
                Optional.of(creationDefaultFingerprint),
                Map.of(),
                Map.of(kind, constraintFingerprint)));
    }

    private static Map.Entry<String, CanvasPropertyContract>
            requiredConstrainedProperty(
                    String name,
                    PropertyValueKind kind,
                    String constraintFingerprint) {
        return Map.entry(name, new CanvasPropertyContract(
                Set.of(kind),
                true,
                Optional.empty(),
                Map.of(),
                Map.of(kind, constraintFingerprint)));
    }

    private static Map<PropertyValueKind, String> anyConstraintFingerprints(
            PropertyValueKind... kinds) {
        LinkedHashMap<PropertyValueKind, String> result = new LinkedHashMap<>();
        for (PropertyValueKind kind : kinds) {
            result.put(kind, "any");
        }
        return Map.copyOf(result);
    }

    private static Map<PropertyValueKind, String> rangeConstraintFingerprints(
            Map<PropertyValueKind, CanvasNumericBounds> bounds) {
        return bounds.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> "range:" + entry.getValue().fingerprint()));
    }

    private static CanvasSlotContract singleSlotSchema(
            boolean required,
            int minimumChildren) {
        return new CanvasSlotContract(
                SlotCardinality.SINGLE, required, minimumChildren, 1, "any");
    }

    private static CanvasSlotContract traitSingleSlotSchema(
            boolean required,
            int minimumChildren,
            String trait) {
        return new CanvasSlotContract(
                SlotCardinality.SINGLE, required, minimumChildren, 1,
                "trait:" + base64(trait));
    }

    private static CanvasSlotContract listSlotSchema(
            boolean required,
            int minimumChildren,
            int maximumChildren) {
        return new CanvasSlotContract(
                SlotCardinality.LIST,
                required,
                minimumChildren,
                maximumChildren,
                "any");
    }

    private static CanvasSlotContract traitListSlotSchema(
            boolean required,
            int minimumChildren,
            int maximumChildren,
            String trait) {
        return new CanvasSlotContract(
                SlotCardinality.LIST,
                required,
                minimumChildren,
                maximumChildren,
                "trait:" + base64(trait));
    }

    private static String slotAcceptanceFingerprint(SlotAcceptance acceptance) {
        return switch (acceptance) {
            case SlotAcceptance.AnyWidget ignored -> "any";
            case SlotAcceptance.HasTrait trait -> "trait:" + base64(trait.trait());
            case SlotAcceptance.ExactTypes types -> "types:"
                    + types.typeIds().stream()
                            .map(type -> base64(type.value()))
                            .sorted()
                            .collect(Collectors.joining(","));
        };
    }

    private static CanvasPropertyContract propertyContract(
            PropertyDefinition property) {
        LinkedHashMap<PropertyValueKind, CanvasNumericBounds> numericBounds =
                new LinkedHashMap<>();
        LinkedHashMap<PropertyValueKind, String> constraintFingerprints =
                new LinkedHashMap<>();
        for (PropertyValueConstraint constraint : property.constraints()) {
            constraintFingerprints.put(
                    constraint.kind(), constraintFingerprint(constraint));
            if (constraint instanceof PropertyValueConstraint.IntegerRange range) {
                numericBounds.put(PropertyValueKind.INTEGER, bounds(
                        decimal(range.minimum()),
                        true,
                        decimal(range.maximum()),
                        true));
            } else if (constraint instanceof PropertyValueConstraint.DoubleRange range) {
                numericBounds.put(PropertyValueKind.DOUBLE, bounds(
                        range.minimum(),
                        range.minimumInclusive(),
                        range.maximum(),
                        range.maximumInclusive()));
            } else if (constraint instanceof PropertyValueConstraint.EdgeInsetsValues edgeInsets) {
                numericBounds.put(PropertyValueKind.EDGE_INSETS,
                        edgeInsets.nonNegative()
                                ? NON_NEGATIVE_NUMERIC
                                : UNBOUNDED_NUMERIC);
            }
        }
        EnumSet<PropertyValueKind> expectedNumeric =
                EnumSet.noneOf(PropertyValueKind.class);
        expectedNumeric.addAll(property.acceptedKinds());
        expectedNumeric.retainAll(NUMERIC_SCHEMA_KINDS);
        if (!numericBounds.keySet().equals(expectedNumeric)) {
            throw new ExceptionInInitializerError(
                    "Canvas numeric schema is incomplete for "
                    + property.name().value() + "; expected=" + expectedNumeric
                    + ", actual=" + numericBounds.keySet());
        }
        return new CanvasPropertyContract(
                property.acceptedKinds(),
                property.parameter().required(),
                property.creationDefault().map(
                        BuiltInWidgetCapabilityCatalog::defaultFingerprint),
                numericBounds,
                constraintFingerprints);
    }

    private static String boxDecorationFingerprint(List<String> themeTokenIds) {
        return "boxDecoration:v2:"
                + IMAGE_PROVIDER_CONTRACT_FINGERPRINT
                + ":decorationImage:v1:"
                + "onError,colorFilter(mode,matrix20,linearToSrgbGamma,"
                + "srgbToLinearGamma,saturation),fit,alignment,centerSlice,"
                + "repeat,matchTextDirection,scale,opacity,filterQuality,"
                + "invertColors,isAntiAlias:centerSliceFit(except:cover,none):theme="
                + themeTokenIds.stream().sorted().collect(Collectors.joining(","));
    }

    private static String constraintFingerprint(
            PropertyValueConstraint constraint) {
        if (constraint instanceof PropertyValueConstraint.AnyValue) {
            return "any";
        }
        if (constraint instanceof PropertyValueConstraint.IntegerRange range) {
            return "range:" + bounds(
                    decimal(range.minimum()), true,
                    decimal(range.maximum()), true).fingerprint();
        }
        if (constraint instanceof PropertyValueConstraint.DoubleRange range) {
            return "range:" + bounds(
                    range.minimum(), range.minimumInclusive(),
                    range.maximum(), range.maximumInclusive()).fingerprint();
        }
        if (constraint instanceof PropertyValueConstraint.EdgeInsetsValues edgeInsets) {
            CanvasNumericBounds numeric = edgeInsets.nonNegative()
                    ? NON_NEGATIVE_NUMERIC
                    : UNBOUNDED_NUMERIC;
            if (!edgeInsets.directionalAllowed()) {
                return "edgeInsetsPhysical:"
                        + (edgeInsets.nonNegative() ? '1' : '0')
                        + ':' + numeric.fingerprint();
            }
            return "edgeInsets:" + (edgeInsets.nonNegative() ? '1' : '0')
                    + ':' + numeric.fingerprint();
        }
        if (constraint instanceof PropertyValueConstraint.AlignmentValues) {
            return "alignment:physical";
        }
        if (constraint instanceof PropertyValueConstraint.AlignmentGeometryValues) {
            return "alignmentGeometry";
        }
        if (constraint instanceof PropertyValueConstraint.OffsetValues) {
            return "offset:finiteSigned";
        }
        if (constraint instanceof PropertyValueConstraint.SizeValues) {
            return "size:finiteNonNegative";
        }
        if (constraint instanceof PropertyValueConstraint.BoxConstraintsValues) {
            return "boxConstraints:v2:finiteOrPositiveInfinity";
        }
        if (constraint instanceof PropertyValueConstraint.Matrix4Values) {
            return "matrix4";
        }
        if (constraint instanceof PropertyValueConstraint.ImageProviderValues) {
            return IMAGE_PROVIDER_CONTRACT_FINGERPRINT;
        }
        if (constraint instanceof PropertyValueConstraint.BorderRadiusValues values) {
            return values.directionalAllowed() ? BORDER_RADIUS_CONTRACT_FINGERPRINT
                    : PHYSICAL_BORDER_RADIUS_CONTRACT_FINGERPRINT;
        }
        if (constraint instanceof PropertyValueConstraint.ShapeBorderClipperValues) {
            return SHAPE_BORDER_CLIPPER_FINGERPRINT;
        }
        if (constraint instanceof PropertyValueConstraint.DartObjectReferenceValues values) {
            return DART_OBJECT_REFERENCE_CONTRACT_PREFIX
                    + values.expectedDartType()
                    + ":currentOrPackage:root,optionalMember:reference,"
                    + "zeroArgumentInvocation:requiredConstnessBoolean(false,true)";
        }
        if (constraint instanceof PropertyValueConstraint.BoxDecorationValues values) {
            return boxDecorationFingerprint(values.colorThemeTokenIds());
        }
        if (constraint instanceof PropertyValueConstraint.StringLength length) {
            return "length:" + length.minimum() + ':' + length.maximum();
        }
        if (constraint instanceof PropertyValueConstraint.StringPattern pattern) {
            return "pattern:" + base64(pattern.regularExpression());
        }
        if (constraint instanceof PropertyValueConstraint.IconDataValues) {
            return "iconData:0:1114111:55296:57343:256:32:1:1:"
                    + "061C,200E,200F,2028-202E,2066-2069,FEFF";
        }
        if (constraint instanceof PropertyValueConstraint.MaterialIconValues) {
            MaterialIconRegistry.SourceMetadata metadata =
                    MaterialIconRegistry.bundled().metadata();
            return "materialIcons:" + metadata.flutterVersion()
                    + ':' + metadata.flutterRevision()
                    + ':' + metadata.iconCount()
                    + ':' + metadata.sourceSha256();
        }
        if (constraint instanceof PropertyValueConstraint.EnumValues values) {
            return "enum:" + base64(values.dartType().libraryUri()) + ':'
                    + values.dartType().name() + ':'
                    + values.values().stream().sorted()
                            .collect(Collectors.joining(","));
        }
        if (constraint instanceof PropertyValueConstraint.ThemeTokenValues values) {
            return "tokens:" + values.wireIds().stream().sorted()
                    .collect(Collectors.joining(","));
        }
        if (constraint instanceof PropertyValueConstraint.PaintValues values) {
            return "paintTokens:" + values.colorThemeTokenIds().stream().sorted()
                    .collect(Collectors.joining(","));
        }
        if (constraint instanceof PropertyValueConstraint.ShadowListValues values) {
            return "shadowTokens:" + values.colorThemeTokenIds().stream().sorted()
                    .collect(Collectors.joining(","));
        }
        if (constraint instanceof PropertyValueConstraint.FontVariationListValues) {
            return "fontVariationList";
        }
        if (constraint instanceof PropertyValueConstraint.CallbackReference) {
            return "callbackReference";
        }
        throw new ExceptionInInitializerError(
                "Canvas constraint requires a reviewed fingerprint: "
                + constraint.getClass().getName());
    }

    private static BigDecimal decimal(java.math.BigInteger value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static String defaultFingerprint(PropertyValue value) {
        if (value.equals(SliverPersistentHeaderWidgetPropertySchema.INITIAL_DELEGATE)) {
            return "dartObjectReference:sliverPersistentHeaderDelegate:starter-v1";
        }
        if (value instanceof PropertyValue.ShapeBorderClipperValue clipper) {
            if (!clipper.equals(PropertyValue.ShapeBorderClipperValue.defaultValue())) {
                throw new ExceptionInInitializerError("Canvas shape clipper default must be reviewed rounded rectangle with zero radii");
            }
            return "shapeBorderClipper:roundedRectangle:physicalZero:none";
        }
        if (value instanceof PropertyValue.NullValue) {
            return "null";
        }
        if (value instanceof PropertyValue.StringValue string) {
            return "string:" + base64(string.value());
        }
        if (value instanceof PropertyValue.CallbackValue callback) {
            // Callback identifiers are intentionally reduced to the same
            // reviewed no-op/string fingerprint used by the isolated Canvas.
            // The executable identifier itself never crosses the Canvas
            // boundary; this fingerprint only locks the creation default.
            return "string:" + base64(callback.handler());
        }
        if (value instanceof PropertyValue.BooleanValue bool) {
            return "boolean:" + bool.value();
        }
        if (value instanceof PropertyValue.DoubleValue decimal) {
            return "double:" + decimalText(decimal.value());
        }
        if (value instanceof PropertyValue.IntegerValue integer) {
            return "integer:" + integer.value();
        }
        if (value instanceof PropertyValue.EnumValue enumValue) {
            return "enum:" + enumValue.type() + ':' + enumValue.value();
        }
        if (value instanceof PropertyValue.ColorValue color) {
            return "color:" + color.wireArgb();
        }
        if (value instanceof PropertyValue.EdgeInsetsValue insets) {
            return "edgeInsets:" + decimalText(insets.left()) + ','
                    + decimalText(insets.top()) + ','
                    + decimalText(insets.right()) + ','
                    + decimalText(insets.bottom());
        }
        if (value instanceof PropertyValue.EdgeInsetsDirectionalValue insets) {
            return "edgeInsetsDirectional:" + decimalText(insets.start()) + ','
                    + decimalText(insets.top()) + ','
                    + decimalText(insets.end()) + ','
                    + decimalText(insets.bottom());
        }
        if (value instanceof PropertyValue.AlignmentGeometryValue alignment) {
            return "alignmentGeometry:" + (alignment.basis() == PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL
                    ? "physical" : "directional") + ':' + decimalText(alignment.horizontal()) + ':' + decimalText(alignment.vertical());
        }
        if (value instanceof PropertyValue.OffsetValue offset) {
            return "offset:" + decimalText(offset.dx()) + ',' + decimalText(offset.dy());
        }
        if (value instanceof PropertyValue.SizeValue size) {
            return "size:" + decimalText(size.width()) + ','
                    + decimalText(size.height());
        }
        if (value instanceof PropertyValue.Matrix4Value matrix) {
            return "matrix4:" + matrix.storage().stream()
                    .map(BuiltInWidgetCapabilityCatalog::decimalText)
                    .collect(Collectors.joining(","));
        }
        if (value instanceof PropertyValue.BoxConstraintsValue constraints) {
            return "boxConstraints:"
                    + boxConstraintBoundFingerprint(constraints.minWidth()) + ','
                    + boxConstraintBoundFingerprint(constraints.maxWidth()) + ','
                    + boxConstraintBoundFingerprint(constraints.minHeight()) + ','
                    + boxConstraintBoundFingerprint(constraints.maxHeight());
        }
        if (value instanceof PropertyValue.BoxDecorationValue decoration) {
            if (decoration.color().isEmpty()
                    && decoration.image().isEmpty()
                    && decoration.border().isEmpty()
                    && decoration.borderRadius().isEmpty()
                    && decoration.boxShadow().isEmpty()
                    && decoration.gradient().isEmpty()
                    && decoration.backgroundBlendMode().isEmpty()
                    && decoration.shape()
                            == PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE) {
                return "boxDecoration:empty";
            }
            throw new ExceptionInInitializerError(
                    "Canvas creation default supports only an exact empty "
                    + "rectangular BoxDecoration");
        }
        if (value instanceof PropertyValue.IconDataValue icon) {
            return "iconData:"
                    + icon.codePoint().map(String::valueOf).orElse("-") + ':'
                    + icon.fontFamily().map(BuiltInWidgetCapabilityCatalog::base64)
                            .orElse("-") + ':'
                    + icon.fontPackage().map(BuiltInWidgetCapabilityCatalog::base64)
                            .orElse("-") + ':'
                    + (icon.matchTextDirection() ? '1' : '0') + ':'
                    + (icon.fontFamilyFallback().isEmpty()
                            ? "-"
                            : icon.fontFamilyFallback().stream()
                                    .map(BuiltInWidgetCapabilityCatalog::base64)
                                    .collect(Collectors.joining(",")));
        }
        if (value instanceof PropertyValue.ImageProviderValue provider && provider.isUnresolved()) {
            return "imageProvider:unresolved";
        }
        throw new ExceptionInInitializerError(
                "Canvas creation default requires a reviewed typed fingerprint for "
                + value.kind().wireName());
    }

    private static String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static String boxConstraintBoundFingerprint(
            PropertyValue.BoxConstraintBound bound) {
        return bound.finiteValue()
                .map(BuiltInWidgetCapabilityCatalog::decimalText)
                .orElse("inf");
    }

    private static boolean validSlotAcceptanceFingerprint(String value) {
        if (value.equals("any")) {
            return true;
        }
        if (value.startsWith("trait:")) {
            String decoded = decodeCanonicalBase64(value.substring("trait:".length()));
            if (decoded == null) {
                return false;
            }
            try {
                new SlotAcceptance.HasTrait(decoded);
                return true;
            } catch (IllegalArgumentException invalidTrait) {
                return false;
            }
        }
        if (!value.startsWith("types:")) {
            return false;
        }
        String encoded = value.substring("types:".length());
        if (encoded.isEmpty()) {
            return false;
        }
        List<String> tokens = List.of(encoded.split(",", -1));
        if (!tokens.equals(tokens.stream().sorted().distinct().toList())) {
            return false;
        }
        for (String token : tokens) {
            String decoded = decodeCanonicalBase64(token);
            if (decoded == null) {
                return false;
            }
            try {
                new dev.flutter.netbeans.designer.model.WidgetTypeId(decoded);
            } catch (IllegalArgumentException invalidType) {
                return false;
            }
        }
        return true;
    }

    private static String decodeCanonicalBase64(String encoded) {
        if (encoded.isEmpty() || encoded.indexOf('=') >= 0) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return base64(decoded).equals(encoded) ? decoded : null;
        } catch (IllegalArgumentException | CharacterCodingException invalid) {
            return null;
        }
    }

    private static CanvasNumericBounds bounds(
            BigDecimal minimum,
            boolean minimumInclusive,
            BigDecimal maximum,
            boolean maximumInclusive) {
        return new CanvasNumericBounds(
                minimum, minimumInclusive, maximum, maximumInclusive);
    }

    /**
     * Returns the deterministic exact contract mirrored by the packaged Dart
     * decoder. The representation is intentionally line-oriented so source
     * parity tests can compare it without a permissive parser.
     */
    public static String reviewedCanvasSchemaContract() {
        StringBuilder result = new StringBuilder();
        new TreeMap<>(CANVAS_PROJECTIONS).forEach((type, projection) -> {
            result.append("W|").append(type).append('\n');
            projection.propertyContracts().forEach((name, contract) -> {
                Map<String, String> wireConstraints =
                        wireConstraintFingerprints(contract);
                Map<String, CanvasNumericBounds> wireNumeric =
                        wireNumericBounds(contract);
                String kinds = String.join(",", wireConstraints.keySet());
                String numeric = wireNumeric.entrySet().stream()
                        .map(entry -> entry.getKey() + ':'
                                + entry.getValue().fingerprint())
                        .collect(Collectors.joining(";"));
                String constraints = wireConstraints.entrySet().stream()
                        .map(entry -> entry.getKey() + ':' + entry.getValue())
                        .collect(Collectors.joining(";"));
                result.append("P|").append(name.value())
                        .append('|').append(kinds)
                        .append('|').append(contract.required() ? '1' : '0')
                        .append('|').append(contract.creationDefaultFingerprint()
                                .orElse("-"))
                        .append('|').append(numeric.isEmpty() ? "-" : numeric)
                        .append('|').append(constraints)
                        .append('\n');
            });
            projection.slotContracts().forEach((name, contract) -> result
                    .append("S|").append(name.value())
                    .append('|').append(contract.cardinality().wireName())
                    .append('|').append(contract.required() ? '1' : '0')
                    .append('|').append(contract.minimumChildren())
                    .append('|').append(contract.maximumChildren())
                    .append('|').append(contract.acceptanceFingerprint())
                    .append('\n'));
            WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                    .find(new WidgetTypeId(type))
                    .orElseThrow();
            WidgetPlacementRules.capabilityFingerprintLines(definition)
                    .forEach(line -> result.append(line).append('\n'));
        });
        return result.toString();
    }

    private static Map<String, CanvasNumericBounds> wireNumericBounds(
            CanvasPropertyContract contract) {
        TreeMap<String, CanvasNumericBounds> result = new TreeMap<>();
        contract.numericBounds().forEach((kind, bounds) -> {
            result.put(kind.wireName(), bounds);
            if (kind == PropertyValueKind.EDGE_INSETS
                    && allowsDirectionalEdgeInsets(contract)) {
                result.put("edgeInsetsDirectional", bounds);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> wireConstraintFingerprints(
            CanvasPropertyContract contract) {
        TreeMap<String, String> result = new TreeMap<>();
        contract.constraintFingerprints().forEach((kind, fingerprint) -> {
            result.put(kind.wireName(), fingerprint);
            if (kind == PropertyValueKind.EDGE_INSETS
                    && allowsDirectionalEdgeInsets(contract)) {
                // The semantic Java value kind deliberately covers both wire
                // variants decoded by the isolated Dart Canvas runtime.
                result.put("edgeInsetsDirectional", fingerprint);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static boolean allowsDirectionalEdgeInsets(
            CanvasPropertyContract contract) {
        return !contract.constraintFingerprints()
                .getOrDefault(PropertyValueKind.EDGE_INSETS, "")
                .startsWith("edgeInsetsPhysical:");
    }

    private static String decimalText(BigDecimal value) {
        if (value == null) {
            return "*";
        }
        BigDecimal canonical = value.stripTrailingZeros();
        return canonical.signum() == 0 ? "0" : canonical.toPlainString();
    }

    /** Immutable independently reviewed schema admitted to the native Canvas wire. */
    public record CanvasProjection(
            Map<PropertyName, Set<PropertyValueKind>> properties,
            Set<SlotName> slots,
            Map<PropertyName, CanvasPropertyContract> propertyContracts,
            Map<SlotName, CanvasSlotContract> slotContracts) {

        public CanvasProjection {
            properties = copyProperties(properties);
            slots = copySlots(slots);
            propertyContracts = copyPropertyContracts(propertyContracts);
            slotContracts = copySlotContracts(slotContracts);
            Map<PropertyName, Set<PropertyValueKind>> derivedProperties =
                    propertyContracts.entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().acceptedKinds(),
                            (left, right) -> left,
                            () -> new TreeMap<>(java.util.Comparator.comparing(
                                    PropertyName::value))));
            if (!properties.equals(derivedProperties)
                    || !slots.equals(slotContracts.keySet())) {
                throw new IllegalArgumentException(
                        "Canvas projection summary and exact contracts disagree");
            }
        }

        static CanvasProjection of(
                Map<PropertyName, CanvasPropertyContract> properties,
                Map<SlotName, CanvasSlotContract> slots) {
            Map<PropertyName, Set<PropertyValueKind>> kinds =
                    properties.entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().acceptedKinds()));
            return new CanvasProjection(kinds, slots.keySet(), properties, slots);
        }

        private static Map<PropertyName, Set<PropertyValueKind>> copyProperties(
                Map<PropertyName, Set<PropertyValueKind>> values) {
            Objects.requireNonNull(values, "properties");
            TreeMap<PropertyName, Set<PropertyValueKind>> copied = new TreeMap<>(
                    java.util.Comparator.comparing(PropertyName::value));
            values.forEach((name, kinds) -> copied.put(
                    Objects.requireNonNull(name, "properties contains null name"),
                    Set.copyOf(Objects.requireNonNull(
                            kinds, "properties contains null kinds"))));
            return Collections.unmodifiableMap(copied);
        }

        private static Set<SlotName> copySlots(Set<SlotName> values) {
            Objects.requireNonNull(values, "slots");
            TreeSet<SlotName> copied = new TreeSet<>(
                    java.util.Comparator.comparing(SlotName::value));
            copied.addAll(values);
            return Collections.unmodifiableSet(copied);
        }

        private static Map<PropertyName, CanvasPropertyContract> copyPropertyContracts(
                Map<PropertyName, CanvasPropertyContract> values) {
            Objects.requireNonNull(values, "propertyContracts");
            TreeMap<PropertyName, CanvasPropertyContract> copied = new TreeMap<>(
                    java.util.Comparator.comparing(PropertyName::value));
            values.forEach((name, contract) -> copied.put(
                    Objects.requireNonNull(name,
                            "propertyContracts contains null name"),
                    Objects.requireNonNull(contract,
                            "propertyContracts contains null contract")));
            return Collections.unmodifiableMap(copied);
        }

        private static Map<SlotName, CanvasSlotContract> copySlotContracts(
                Map<SlotName, CanvasSlotContract> values) {
            Objects.requireNonNull(values, "slotContracts");
            TreeMap<SlotName, CanvasSlotContract> copied = new TreeMap<>(
                    java.util.Comparator.comparing(SlotName::value));
            values.forEach((name, contract) -> copied.put(
                    Objects.requireNonNull(name,
                            "slotContracts contains null name"),
                    Objects.requireNonNull(contract,
                            "slotContracts contains null contract")));
            return Collections.unmodifiableMap(copied);
        }
    }

    /** Exact below-type Canvas property contract. */
    public record CanvasPropertyContract(
            Set<PropertyValueKind> acceptedKinds,
            boolean required,
            Optional<String> creationDefaultFingerprint,
            Map<PropertyValueKind, CanvasNumericBounds> numericBounds,
            Map<PropertyValueKind, String> constraintFingerprints) {

        public CanvasPropertyContract {
            Objects.requireNonNull(acceptedKinds, "acceptedKinds");
            Objects.requireNonNull(
                    creationDefaultFingerprint, "creationDefaultFingerprint");
            Objects.requireNonNull(numericBounds, "numericBounds");
            Objects.requireNonNull(
                    constraintFingerprints, "constraintFingerprints");
            acceptedKinds = Collections.unmodifiableSet(
                    acceptedKinds.isEmpty()
                            ? EnumSet.noneOf(PropertyValueKind.class)
                            : EnumSet.copyOf(acceptedKinds));
            if (acceptedKinds.isEmpty()) {
                throw new IllegalArgumentException(
                        "Canvas property must accept at least one kind");
            }
            creationDefaultFingerprint.ifPresent(value -> {
                if (value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('|') >= 0) {
                    throw new IllegalArgumentException(
                            "Canvas default fingerprint is not canonical");
                }
            });
            TreeMap<PropertyValueKind, CanvasNumericBounds> copiedBounds =
                    new TreeMap<>(java.util.Comparator.comparingInt(Enum::ordinal));
            numericBounds.forEach((kind, value) -> copiedBounds.put(
                    Objects.requireNonNull(kind,
                            "numericBounds contains null kind"),
                    Objects.requireNonNull(value,
                            "numericBounds contains null value")));
            EnumSet<PropertyValueKind> expectedBounds =
                    EnumSet.noneOf(PropertyValueKind.class);
            expectedBounds.addAll(acceptedKinds);
            expectedBounds.retainAll(NUMERIC_SCHEMA_KINDS);
            if (!copiedBounds.keySet().equals(expectedBounds)) {
                throw new IllegalArgumentException(
                        "Canvas numeric bounds must exactly cover numeric kinds; expected="
                        + expectedBounds + ", actual=" + copiedBounds.keySet());
            }
            numericBounds = Collections.unmodifiableMap(copiedBounds);
            TreeMap<PropertyValueKind, String> copiedConstraints =
                    new TreeMap<>(java.util.Comparator.comparingInt(Enum::ordinal));
            constraintFingerprints.forEach((kind, value) -> copiedConstraints.put(
                    Objects.requireNonNull(kind,
                            "constraintFingerprints contains null kind"),
                    Objects.requireNonNull(value,
                            "constraintFingerprints contains null value")));
            if (!copiedConstraints.keySet().equals(acceptedKinds)) {
                throw new IllegalArgumentException(
                        "Canvas constraint fingerprints must exactly cover accepted kinds; "
                        + "expected=" + acceptedKinds + ", actual="
                        + copiedConstraints.keySet());
            }
            copiedConstraints.values().forEach(value -> {
                if (value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('|') >= 0) {
                    throw new IllegalArgumentException(
                            "Canvas constraint fingerprint is not canonical");
                }
            });
            constraintFingerprints = Collections.unmodifiableMap(copiedConstraints);
        }
    }

    /** Inclusive/exclusive finite-or-unbounded numeric contract. */
    public record CanvasNumericBounds(
            BigDecimal minimum,
            boolean minimumInclusive,
            BigDecimal maximum,
            boolean maximumInclusive) {

        public CanvasNumericBounds {
            minimum = canonicalDecimal(minimum);
            maximum = canonicalDecimal(maximum);
            if (minimum == null) {
                minimumInclusive = true;
            }
            if (maximum == null) {
                maximumInclusive = true;
            }
            if (minimum != null && maximum != null) {
                int comparison = minimum.compareTo(maximum);
                if (comparison > 0
                        || (comparison == 0
                        && (!minimumInclusive || !maximumInclusive))) {
                    throw new IllegalArgumentException(
                            "Canvas numeric minimum exceeds maximum");
                }
            }
        }

        String fingerprint() {
            return decimalText(minimum) + ':' + (minimumInclusive ? '1' : '0')
                    + ':' + decimalText(maximum) + ':'
                    + (maximumInclusive ? '1' : '0');
        }

        private static BigDecimal canonicalDecimal(BigDecimal value) {
            if (value == null) {
                return null;
            }
            if (!DartNumericLiterals.isRepresentableDouble(value)) {
                throw new IllegalArgumentException(
                        "Canvas numeric bound is not a representable Dart double");
            }
            BigDecimal canonical = value.stripTrailingZeros();
            return canonical.signum() == 0 ? BigDecimal.ZERO : canonical;
        }
    }

    /** Exact below-type Canvas slot contract. */
    public record CanvasSlotContract(
            SlotCardinality cardinality,
            boolean required,
            int minimumChildren,
            int maximumChildren,
            String acceptanceFingerprint) {

        public CanvasSlotContract(
                SlotCardinality cardinality,
                boolean required,
                int minimumChildren,
                int maximumChildren) {
            this(cardinality, required, minimumChildren, maximumChildren, "any");
        }

        public CanvasSlotContract {
            Objects.requireNonNull(cardinality, "cardinality");
            Objects.requireNonNull(acceptanceFingerprint, "acceptanceFingerprint");
            if (minimumChildren < 0
                    || maximumChildren < minimumChildren
                    || maximumChildren > 10_000
                    || (cardinality == SlotCardinality.SINGLE
                    && maximumChildren > 1)) {
                throw new IllegalArgumentException(
                        "Invalid Canvas slot child bounds");
            }
            if (!validSlotAcceptanceFingerprint(acceptanceFingerprint)) {
                throw new IllegalArgumentException(
                        "Invalid Canvas slot acceptance fingerprint");
            }
        }
    }
}
