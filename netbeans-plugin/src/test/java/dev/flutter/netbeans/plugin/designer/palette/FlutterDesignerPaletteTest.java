package dev.flutter.netbeans.plugin.designer.palette;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewExtentWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SingleChildScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PageViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListWheelScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CustomScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SliverToBoxAdapterWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.plugin.designer.icons.FlutterWidgetIconRegistry;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import javax.swing.Action;
import org.junit.jupiter.api.Test;
import org.netbeans.spi.palette.DragAndDropHandler;
import org.netbeans.spi.palette.PaletteActions;
import org.netbeans.spi.palette.PaletteController;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

class FlutterDesignerPaletteTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final Set<String> CANVAS_WIDGETS = Set.of(
            "flutter.material.Scaffold",
            "flutter.material.AppBar",
            "flutter.material.ElevatedButton",
            "flutter.material.TextField",
            "flutter.material.Divider",
            "flutter.material.VerticalDivider",
            "flutter.material.Card",
            "flutter.material.Badge",
            "flutter.material.CircleAvatar",
            "flutter.material.LinearProgressIndicator",
            "flutter.material.CircularProgressIndicator",
            "flutter.material.RefreshProgressIndicator",
            "flutter.material.RefreshIndicator",
            "flutter.material.TextButton",
            "flutter.material.OutlinedButton",
            "flutter.material.FilledButton",
            "flutter.material.FloatingActionButton",
            "flutter.material.IconButton",
            "flutter.material.Checkbox",
            "flutter.material.Switch",
            "flutter.material.Slider",
            "flutter.material.RangeSlider",
            "flutter.material.Radio",
            "flutter.widgets.RadioGroup",
            "flutter.material.ListTile",
            "flutter.material.CheckboxListTile",
            "flutter.material.SwitchListTile",
            "flutter.material.RadioListTile",
            "flutter.material.ExpansionTile",
            "flutter.material.Tooltip",
            "flutter.material.TooltipVisibility",
            "flutter.material.TooltipTheme",
            "flutter.material.MenuItemButton",
            "flutter.material.MenuAnchor",
            "flutter.material.SubmenuButton",
            "flutter.material.MenuBar",
            "flutter.material.NavigationBar",
            "flutter.material.NavigationRail",
            "flutter.material.NavigationDrawer",
            "flutter.material.Drawer",
            "flutter.material.BottomAppBar",
            "flutter.material.BottomNavigationBar",
            "flutter.material.Material",
            "flutter.material.Scrollbar",
            "flutter.material.SliverAppBar",
            "flutter.material.SliverAppBar.medium",
            "flutter.material.SliverAppBar.large",
            "flutter.material.FlexibleSpaceBar", "flutter.material.FlexibleSpaceBarSettings", "flutter.material.AnimatedTheme", "flutter.material.Theme", "flutter.material.AnimatedIcon", "flutter.widgets.FadeInImage", "flutter.widgets.RawImage", "flutter.widgets.ColorFiltered", "flutter.widgets.ImageFiltered", "flutter.widgets.BackdropFilter", "flutter.widgets.BackdropFilter.grouped", "flutter.widgets.BackdropGroup", "flutter.widgets.ShaderMask", "flutter.widgets.CustomPaint",
            "flutter.widgets.GestureDetector",
            "flutter.widgets.Listener",
            "flutter.widgets.MouseRegion",
            "flutter.widgets.Focus",
            "flutter.widgets.NotificationListener",
            "flutter.widgets.Column",
            "flutter.widgets.Row",
            "flutter.widgets.Wrap",
            "flutter.widgets.Text",
            "flutter.widgets.Icon",
            "flutter.widgets.Image",
            "flutter.widgets.ColoredBox",
            "flutter.widgets.Placeholder",
            "flutter.widgets.Directionality",
            "flutter.widgets.DecoratedBox",
            "flutter.widgets.Builder",
            "flutter.widgets.ClipRect",
            "flutter.widgets.ClipOval",
            "flutter.widgets.ClipRRect",
            "flutter.widgets.ClipPath",
            "flutter.widgets.ClipRSuperellipse",
            "flutter.widgets.PhysicalModel",
                    "flutter.widgets.PhysicalShape",
                    "flutter.widgets.RepaintBoundary",
                    "flutter.widgets.IgnorePointer",
                    "flutter.widgets.AbsorbPointer",
                    "flutter.widgets.BlockSemantics",
                    "flutter.widgets.MergeSemantics",
                    "flutter.widgets.IndexedSemantics",
                    "flutter.widgets.ExcludeFocus",
                    "flutter.widgets.ExcludeFocusTraversal",
                    "flutter.widgets.Visibility",
                    "flutter.widgets.TickerMode",
                    "flutter.widgets.DefaultTextHeightBehavior",
                    "flutter.widgets.DefaultSelectionStyle",
                    "flutter.widgets.IconTheme",
                    "flutter.widgets.ImageIcon",
                    "flutter.widgets.DefaultTextStyle",
                    "flutter.widgets.DefaultTextStyle.merge",
            "flutter.widgets.ExcludeSemantics",
            "flutter.widgets.IndexedStack",
            "flutter.widgets.Padding",
            "flutter.widgets.Center",
            "flutter.widgets.SizedBox",
            "flutter.widgets.AspectRatio",
            "flutter.widgets.Container",
            "flutter.widgets.Opacity",
            "flutter.widgets.Align",
            "flutter.widgets.FractionallySizedBox",
            "flutter.widgets.FittedBox",
            "flutter.widgets.ConstrainedBox",
            "flutter.widgets.UnconstrainedBox",
            "flutter.widgets.LimitedBox",
            "flutter.widgets.OverflowBox",
            "flutter.widgets.Stack",
            "flutter.widgets.Expanded",
            "flutter.widgets.Flexible",
            "flutter.widgets.Spacer",
            "flutter.widgets.Baseline",
            "flutter.widgets.IntrinsicHeight",
            "flutter.widgets.IntrinsicWidth",
            "flutter.widgets.Offstage",
            "flutter.widgets.SizedOverflowBox",
            "flutter.widgets.Transform",
            "flutter.widgets.RotatedBox",
            "flutter.widgets.PreferredSize",
            "flutter.widgets.ListBody",
            "flutter.widgets.OverflowBar",
            "flutter.widgets.SafeArea",
            "flutter.widgets.LayoutBuilder", "flutter.widgets.OrientationBuilder", "flutter.widgets.DeviceOrientationBuilder", "flutter.widgets.ListenableBuilder", "flutter.widgets.AnimatedBuilder", "flutter.widgets.ValueListenableBuilder", "flutter.widgets.TweenAnimationBuilder", "flutter.widgets.AnimatedOpacity", "flutter.widgets.AnimatedAlign", "flutter.widgets.AnimatedPadding", "flutter.widgets.AnimatedSlide", "flutter.widgets.AnimatedScale", "flutter.widgets.AnimatedRotation", "flutter.widgets.AnimatedContainer", "flutter.widgets.AnimatedSize", "flutter.widgets.AnimatedPositioned", "flutter.widgets.AnimatedPositioned.fromRect", "flutter.widgets.AnimatedPositionedDirectional", "flutter.widgets.AnimatedDefaultTextStyle", "flutter.widgets.AnimatedPhysicalModel", "flutter.widgets.AnimatedFractionallySizedBox", "flutter.widgets.AnimatedCrossFade", "flutter.widgets.AnimatedSwitcher", "flutter.widgets.DefaultTextStyleTransition", "flutter.widgets.FadeTransition", "flutter.widgets.SlideTransition", "flutter.widgets.ScaleTransition", "flutter.widgets.RotationTransition", "flutter.widgets.SizeTransition", "flutter.widgets.PositionedTransition", "flutter.widgets.RelativePositionedTransition", "flutter.widgets.DecoratedBoxTransition", "flutter.widgets.AlignTransition", "flutter.widgets.MatrixTransition", "flutter.widgets.CustomSingleChildLayout", "flutter.widgets.CustomMultiChildLayout", "flutter.widgets.LayoutId", "flutter.widgets.ModalBarrier", "flutter.widgets.AnimatedModalBarrier",
            "flutter.widgets.ListView",
            GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value(),
            GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE.value(),
            SingleChildScrollViewWidgetPropertySchema
                    .SINGLE_CHILD_SCROLL_VIEW_TYPE.value(),
            PageViewWidgetPropertySchema.PAGE_VIEW_TYPE.value(),
            ListWheelScrollViewWidgetPropertySchema.LIST_WHEEL_SCROLL_VIEW_TYPE.value(),
            CustomScrollViewWidgetPropertySchema.CUSTOM_SCROLL_VIEW_TYPE.value(),
            SliverToBoxAdapterWidgetPropertySchema.SLIVER_TO_BOX_ADAPTER_TYPE.value(),
            "flutter.widgets.SliverList", "flutter.widgets.SliverGrid", "flutter.widgets.SliverGrid.extent", "flutter.widgets.SliverList.builder", "flutter.widgets.SliverList.separated", "flutter.widgets.SliverList.delegate", "flutter.widgets.SliverGrid.builder", "flutter.widgets.SliverGrid.list", "flutter.widgets.SliverGrid.delegate", "flutter.widgets.SliverPadding", "flutter.widgets.SliverFillRemaining", "flutter.widgets.SliverFillViewport", "flutter.widgets.SliverFillViewport.delegate", "flutter.widgets.SliverFixedExtentList", "flutter.widgets.SliverFixedExtentList.builder", "flutter.widgets.SliverFixedExtentList.delegate", "flutter.widgets.SliverPrototypeExtentList", "flutter.widgets.SliverPrototypeExtentList.builder", "flutter.widgets.SliverPrototypeExtentList.delegate", "flutter.widgets.SliverVariedExtentList", "flutter.widgets.SliverVariedExtentList.builder", "flutter.widgets.SliverVariedExtentList.delegate", "flutter.widgets.SliverMainAxisGroup", "flutter.widgets.SliverCrossAxisGroup", "flutter.widgets.SliverCrossAxisExpanded", "flutter.widgets.SliverConstrainedCrossAxis", "flutter.widgets.SliverOpacity", "flutter.widgets.SliverIgnorePointer", "flutter.widgets.SliverOffstage", "flutter.widgets.SliverVisibility", "flutter.widgets.SliverVisibility.maintain", "flutter.widgets.SliverSafeArea", "flutter.widgets.SliverAnimatedOpacity", "flutter.widgets.SliverLayoutBuilder", "flutter.widgets.SliverPersistentHeader", "flutter.widgets.SliverResizingHeader", "flutter.widgets.PinnedHeaderSliver", "flutter.widgets.SliverFloatingHeader", "flutter.widgets.DeviceOrientationBuilder.sliver", "flutter.widgets.ListenableBuilder.sliver", "flutter.widgets.AnimatedBuilder.sliver", "flutter.widgets.ValueListenableBuilder.sliver", "flutter.widgets.TweenAnimationBuilder.sliver", "flutter.widgets.SliverFadeTransition");
    private static final Set<String> NON_CANVAS_BUILT_INS = Set.of();

    @Test
    void listTilePaletteExplainsAll176RowsOptionalSlotsAndExplicitStateDefaults() throws ReflectiveOperationException {
        String type = "flutter.material.ListTile"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("ListTile", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 250, "ListTile"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(176, definition.properties().size()); assertEquals(4, definition.slots().size()); assertTrue(definition.constConstructor());
        assertTrue(definition.properties().stream().allMatch(field -> field.creationDefault().isEmpty() && !field.parameter().required()));
        for (String hint : List.of("176 typed", "four optional", "No title or callback", "Three line", "explicit Default", "centered checkboxes", "NaN", "without executing"))
            assertTrue(node.getShortDescription().contains(hint), hint);
    }

    @Test
    void radioGroupPaletteExplainsRequiredWrapperAndScopedThreeFieldTypeEdit() throws ReflectiveOperationException {
        String type = "flutter.widgets.RadioGroup"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("RadioGroup", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 240, "RadioGroup"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        for (String hint : List.of("four typed properties", "Child is required", "No-op", "callback null and omission are rejected",
                "atomically", "descendant Radio types and values remain unchanged", "centered checkboxes", "never executed")) assertTrue(node.getShortDescription().contains(hint), hint);
    }

    @Test
    void radioPaletteExplainsCompleteGenericTypeNullableActivationAndStateFamilies() throws ReflectiveOperationException {
        String type = "flutter.material.Radio"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type); assertEquals("Radio", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 230, "Radio"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(107, definition.properties().size()); assertTrue(definition.slots().isEmpty()); assertTrue(definition.constConstructor());
        for (String token : List.of("107", "Standard/Adaptive", "No-op", "nullable", "Type/Value/Group value", "45 local", "centered", "No child", "never executed"))
            assertTrue(node.getShortDescription().contains(token), token + ": " + node.getShortDescription());
    }

    @Test
    void rangeSliderPaletteExplainsAll37RowsControlledEndpointsLabelsAndStatefulCursor() throws ReflectiveOperationException {
        String type = "flutter.material.RangeSlider"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("RangeSlider", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 220, "RangeSlider"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(37, definition.properties().size()); assertTrue(definition.slots().isEmpty()); assertFalse(definition.constConstructor());
        for (String hint : List.of("non-const", "37 typed", "Infinity", "without clamping", "RangeLabels", "explicit null", "centered checkboxes", "41 reviewed", "Undo", "padding"))
            assertTrue(node.getShortDescription().contains(hint), hint);
    }

    @Test
    void sliderPaletteExplainsAll33RowsBothConstructorsControlledRangeAndContinuousModes() throws ReflectiveOperationException {
        String type = "flutter.material.Slider"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("Slider", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 210, "Slider"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(33, definition.properties().size()); assertTrue(definition.slots().isEmpty());
        for (String hint : List.of("Slider.adaptive", "33 typed", "signed Infinity", "without clamping", "nine nullable overlay", "Divisions null", "centered checkboxes", "WidgetStateProperty", "Undo", "Padding"))
            assertTrue(node.getShortDescription().contains(hint), hint + ": " + node.getShortDescription());
    }

    @Test
    void switchPaletteExplainsAll201RowsBothConstructorsAndExactIconStateModes() throws ReflectiveOperationException {
        String type = "flutter.material.Switch"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("Switch", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 200, "Switch"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertTrue(definition.slots().isEmpty());
        for (String hint : List.of("Switch.adaptive", "201 typed", "Icon(null)", "13 Icon", "nine complete", "centered checkboxes", "WidgetStateProperty", "error callback", "Undo", "Apply Cupertino theme"))
            assertTrue(node.getShortDescription().contains(hint), hint + ": " + node.getShortDescription());
    }

    @Test
    void checkboxPaletteExplainsControlledMixedValueStateResolutionAndAll106Rows() throws ReflectiveOperationException {
        String type = "flutter.material.Checkbox"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("Checkbox", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 190, "Checkbox"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(106, definition.properties().size()); assertTrue(definition.slots().isEmpty());
        for (String hint : List.of("Checkbox.adaptive", "106 editable", "null (mixed)", "Tristate", "centered checkboxes", "nine fill", "border/inherit", "OutlinedBorder", "ValueChanged<bool?>", "Undo", "CupertinoCheckbox"))
            assertTrue(node.getShortDescription().contains(hint), hint);
    }

    @Test
    void iconButtonPaletteExplainsFourConstructorsRequiredIconAndAllStyleRows() throws ReflectiveOperationException {
        String type = "flutter.material.IconButton"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("IconButton", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 180, "IconButton"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(524, definition.properties().size()); assertEquals(2, definition.slots().size());
        assertEquals(1, definition.slot(new dev.flutter.netbeans.designer.model.SlotName("icon")).orElseThrow().minChildren());
        assertEquals(0, definition.slot(new dev.flutter.netbeans.designer.model.SlotName("selectedIcon")).orElseThrow().minChildren());
        for (String hint : List.of("IconButton.filled", "IconButton.filledTonal", "IconButton.outlined", "required Icon", "Selected icon",
                "524 scalar", "498 local", "centered checkboxes", "SDK null", "isolated Canvas", "Material 2"))
            assertTrue(node.getShortDescription().contains(hint), hint);
    }

    @Test
    void floatingActionButtonPaletteExplainsAllFourConstructorsAndTypedHeroIdentity() throws ReflectiveOperationException {
        String type = "flutter.material.FloatingActionButton"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("FloatingActionButton", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 170, "FloatingActionButton"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(78, definition.properties().size()); assertEquals(2, definition.slots().size());
        assertEquals(0, definition.slots().getFirst().minChildren()); assertEquals(0, definition.slots().getLast().minChildren());
        for (String hint : List.of("FloatingActionButton.small", "FloatingActionButton.large", "FloatingActionButton.extended", "empty Child",
                "Extended requires", "Move or clear Icon", "Hero tag", "41 reviewed cursor", "22-field", "31-field", "isolated Canvas"))
            assertTrue(node.getShortDescription().contains(hint), hint);
    }

    @Test
    void filledButtonPaletteExplainsFourConstructorsAllNineStyleBucketsAndSafeIconTransitions() throws ReflectiveOperationException {
        String type = "flutter.material.FilledButton"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("FilledButton", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 160, "FilledButton"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(510, definition.properties().size()); assertEquals(2, definition.slots().size());
        assertEquals(0, definition.slots().getFirst().minChildren()); assertEquals(0, definition.slots().getLast().minChildren());
        for (String hint : List.of("FilledButton.icon", "FilledButton.tonal", "FilledButton.tonalIcon", "empty Child", "Move or clear a populated Icon", "project callbacks", "FocusNode", "WidgetStatesController", "ButtonStyle",
                "ButtonLayerBuilder", "disabled/error/dragged/pressed/selected/scrolledUnder/hovered/focused/default", "Clip.none", "isolated Canvas"))
            assertTrue(node.getShortDescription().contains(hint), hint);
    }

    @Test
    void outlinedButtonPaletteExplainsBothConstructorsAllNineStyleBucketsAndSafeIconTransitions() throws ReflectiveOperationException {
        String type = "flutter.material.OutlinedButton"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("OutlinedButton", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 150, "OutlinedButton"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(510, definition.properties().size()); assertEquals(2, definition.slots().size());
        assertEquals(1, definition.slots().getFirst().minChildren()); assertEquals(0, definition.slots().getLast().minChildren());
        for (String hint : List.of("OutlinedButton.icon", "required Child", "cleared", "VoidCallback", "FocusNode", "WidgetStatesController", "ButtonStyle",
                "ButtonLayerBuilder", "disabled/error/dragged/pressed/selected/scrolledUnder/hovered/focused/default", "null and omission", "isolated Canvas"))
            assertTrue(node.getShortDescription().contains(hint), hint);
    }

    @Test
    void textButtonPaletteExplainsBothConstructorsAllNineStyleBucketsAndSafeIconTransitions() throws ReflectiveOperationException {
        String type = "flutter.material.TextButton"; var definition = CATALOG.find(new dev.flutter.netbeans.designer.model.WidgetTypeId(type)).orElseThrow();
        var node = itemNode(FlutterDesignerPalette.create(CATALOG, item -> type.equals(item.typeId().value())), type);
        assertEquals("TextButton", node.getDisplayName());
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 140, "TextButton"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(511, definition.properties().size()); assertEquals(2, definition.slots().size());
        assertEquals(1, definition.slots().getFirst().minChildren()); assertEquals(0, definition.slots().getLast().minChildren());
        for (String hint : List.of("TextButton.icon", "required Child", "cleared", "VoidCallback", "FocusNode", "WidgetStatesController", "ButtonStyle",
                "ButtonLayerBuilder", "disabled/error/dragged/pressed/selected/scrolledUnder/hovered/focused/default", "null and omission", "isolated Canvas"))
            assertTrue(node.getShortDescription().contains(hint), hint);
    }

    @Test
    void refreshIndicatorPaletteExplainsAllThreeConstructorsRequiredChildTypedCallbacksAndNoOpDefault() throws ReflectiveOperationException {
        String type = "flutter.material.RefreshIndicator";
        var controller = FlutterDesignerPalette.create(CATALOG, definition -> type.equals(definition.typeId().value()));
        var node = itemNode(controller, type); var definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 130, "RefreshIndicator"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(13, definition.properties().size()); assertEquals(1, definition.slots().size()); assertEquals(1, definition.slots().getFirst().minChildren());
        for (String hint : List.of("13 scalar", "required child", "No spinner", "async no-op", "default/depthZero/all",
                "ScrollNotificationPredicate", "RefreshCallback", "atomically", "never executes", "vertical ScrollView")) assertTrue(node.getShortDescription().contains(hint), hint);
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void refreshProgressPaletteExposesTwelveOptionalFieldsAndDistinctNullWidthWithoutChildSlots() throws ReflectiveOperationException {
        String type = "flutter.material.RefreshProgressIndicator";
        var controller = FlutterDesignerPalette.create(CATALOG, definition -> type.equals(definition.typeId().value()));
        var node = itemNode(controller, type); var definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 120, "RefreshProgressIndicator"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(12, definition.properties().size()); assertTrue(definition.slots().isEmpty());
        assertTrue(definition.properties().stream().noneMatch(p -> p.parameter().required()));
        for (String hint : List.of("12 optional", "no child slots", "not the RefreshIndicator", "2.5", "null", "fallback 4", "Animation<Color?>", "cannot execute")) assertTrue(node.getShortDescription().contains(hint), hint);
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void circularProgressPaletteExposesBothConstructorsFullOptionalSurfaceAndDedicatedIcon() throws ReflectiveOperationException {
        String type = "flutter.material.CircularProgressIndicator";
        var controller = FlutterDesignerPalette.create(CATALOG, definition -> type.equals(definition.typeId().value()));
        var node = itemNode(controller, type); var definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 110, "CircularProgressIndicator"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(15, definition.properties().size()); assertTrue(definition.slots().isEmpty());
        assertEquals(1, definition.properties().stream().filter(p -> p.parameter().required()).count());
        for (String hint : List.of("material and adaptive", "14 optional", "no child slots", "Animation<Color?>", "one undoable edit", "Year 2023", "cannot execute", "constructor selector cannot be unset")) assertTrue(node.getShortDescription().contains(hint), hint);
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void linearProgressPaletteExposesThirteenOptionalFieldsAndNoChildrenOrDefaults() throws ReflectiveOperationException {
        String type = "flutter.material.LinearProgressIndicator";
        var controller = FlutterDesignerPalette.create(CATALOG, definition -> type.equals(definition.typeId().value()));
        var node = itemNode(controller, type); var definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 100, "LinearProgressIndicator"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(13, definition.properties().size()); assertTrue(definition.slots().isEmpty());
        for (String hint : List.of("13 optional", "no child slots", "clamps its display", "atomically", "Animation<Color?>", "Infinity", "Year 2023", "cannot execute")) assertTrue(node.getShortDescription().contains(hint), hint);
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void circleAvatarPaletteExposesNineOptionalFieldsNoAssetRequirementAndDedicatedIcon() throws ReflectiveOperationException {
        String typeId = "flutter.material.CircleAvatar";
        var controller = FlutterDesignerPalette.create(CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        var definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 90, "CircleAvatar"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(9, definition.properties().size()); assertEquals(1, definition.slots().size());
        for (String hint : List.of("nine optional", "optional Child", "Infinity", "rejected, not clamped", "one undoable edit", "requires no asset", "ResizeImage")) {
            assertTrue(node.getShortDescription().contains(hint), hint);
        }
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void badgePaletteExposesAll41OptionalFieldsAndCountModeWithoutInventedSelector() throws ReflectiveOperationException {
        String typeId = "flutter.material.Badge";
        var controller = FlutterDesignerPalette.create(CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        var definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 80, "Badge"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals("Badge", node.getDisplayName()); assertEquals(41, definition.properties().size());
        assertEquals(2, definition.slots().size());
        for (String hint : List.of("41 optional", "TextStyle", "Badge.count", "clearing or moving Label", "one undoable edit", "unset/reset", "foreground Paint")) {
            assertTrue(node.getShortDescription().contains(hint), hint);
        }
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void cardPaletteExposesAll31FieldsMaterialOrderAndDedicatedIcon() throws ReflectiveOperationException {
        String typeId = "flutter.material.Card";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 70, "Card"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals("Card", node.getDisplayName());
        for (String hint : List.of("31 property rows", "elevated", "filled", "outlined", "optional Child", "ten built-in", "unset/reset", "ShapeBorder")) {
            assertTrue(node.getShortDescription().contains(hint), hint);
        }
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void verticalDividerPaletteExposesAllSixFieldsMaterialOrderAndDedicatedIcon() throws ReflectiveOperationException {
        String typeId = "flutter.material.VerticalDivider";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 60, "VerticalDivider"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals("VerticalDivider", node.getDisplayName());
        for (String hint : List.of("six optional fields unset", "positive thickness", "elliptical",
                "No child slots", "Restore Default", "DividerTheme", "TOP", "BOTTOM", "bounded parent height")) {
            assertTrue(node.getShortDescription().contains(hint), hint);
        }
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void dividerPaletteExposesAllSixFieldsMaterialOrderAndDedicatedIcon() throws ReflectiveOperationException {
        String typeId = "flutter.material.Divider";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 50, "Divider"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals("Divider", node.getDisplayName());
        for (String hint : List.of("six optional fields unset", "positive thickness", "elliptical",
                "No child slots", "Restore Default", "DividerTheme")) {
            assertTrue(node.getShortDescription().contains(hint), hint);
        }
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void imageIconPaletteExplainsNullableImageAndDedicatedIcon() throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ImageIcon";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.basic", 300, 230, "ImageIcon"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals("ImageIcon", node.getDisplayName());
        for (String hint : List.of("explicit None", "cannot be omitted or reset", "ResizeImage",
                "No child slots", "alpha mask", "IconTheme")) {
            assertTrue(node.getShortDescription().contains(hint), hint);
        }
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void iconThemePaletteExplainsAllNineSdkFieldsRequiredMergeAndDedicatedIcon() throws ReflectiveOperationException {
        String typeId = "flutter.widgets.IconTheme";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.basic", 300, 220, "IconTheme"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("IconTheme", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("required child"));
        assertTrue(node.getShortDescription().contains("All nine SDK fields are optional and resettable"));
        assertTrue(node.getShortDescription().contains("required Designer-only choice created as false"));
        assertTrue(node.getShortDescription().contains("IconTheme.merge and inherits unset fields"));
        assertTrue(node.getShortDescription().contains("Flutter clamps it to 0..1"));
        assertTrue(node.getShortDescription().contains("stable IDs and explicit empty lists"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void defaultSelectionStylePaletteExplainsDirectAndMergeModesWithDedicatedIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.DefaultSelectionStyle";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.basic", 300, 210, "DefaultSelectionStyle"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("DefaultSelectionStyle", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("required child"));
        assertTrue(node.getShortDescription().contains("True uses DefaultSelectionStyle.merge and inherits unset fields"));
        assertTrue(node.getShortDescription().contains("required Designer-only choice created as false"));
        assertTrue(node.getShortDescription().contains("cannot be unset or reset"));
        assertTrue(node.getShortDescription().contains("direct constructor and clears them"));
        assertTrue(node.getShortDescription().contains("41 closed presets"));
        assertTrue(node.getShortDescription().contains("does not make a child selectable"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void defaultTextHeightBehaviorPaletteExplainsRequiredObjectAndNearestDefaultsWithDedicatedIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.DefaultTextHeightBehavior";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.basic", 300, 200, "DefaultTextHeightBehavior"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("DefaultTextHeightBehavior", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("required child"));
        assertTrue(node.getShortDescription().contains("required behavior object is always emitted"));
        assertTrue(node.getShortDescription().contains("does not pass through an outer behavior"));
        assertTrue(node.getShortDescription().contains("no font size or height is invented"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void tickerModePaletteExplainsRequiredEnabledIndependentForceFramesAndDedicatedIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.TickerMode";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.basic", 300, 190, "TickerMode"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("TickerMode", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("required child"));
        assertTrue(node.getShortDescription().contains("not an SDK default"));
        assertTrue(node.getShortDescription().contains("AND"));
        assertTrue(node.getShortDescription().contains("OR independently"));
        assertTrue(node.getShortDescription().contains("battery usage"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void menuItemButtonPaletteExplainsOptionalSlotsAllStylesAndDisplayOnlyShortcuts() throws ReflectiveOperationException {
        String typeId = "flutter.material.MenuItemButton";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId); WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 330, "MenuItemButton"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        for (String phrase : List.of("520 typed", "498 local", "all optional", "Three native Events", "Four independent boolean", "432 reviewed", "not global key registrations", "no key is invented", "never executed"))
            assertTrue(node.getShortDescription().contains(phrase), node.getShortDescription());
        assertTrue(definition.slots().stream().allMatch(slot -> slot.minChildren() == 0));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node)); assertTrue(declaredIconPath(node).endsWith("/menuitembutton.svg"));
    }

    @Test
    void tooltipThemePaletteExplainsCompleteLocalDataAndExplicitConflicts() throws ReflectiveOperationException {
        String typeId = "flutter.material.TooltipTheme";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId); WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 320, "TooltipTheme"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals("TooltipTheme", node.getDisplayName());
        for (String phrase : List.of("required Child", "47 properties", "46 local leaves", "31 TextStyle", "explicit null and State binding", "without silent data loss", "not an outer-theme merge", "No native Events", "never executes")) {
            assertTrue(node.getShortDescription().contains(phrase), node.getShortDescription());
        }
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
        assertTrue(declaredIconPath(node).endsWith("/tooltiptheme.svg"));
    }

    @Test
    void tooltipVisibilityPaletteExplainsRequiredWrapNearestScopeAndIndependentStatePreview()
            throws ReflectiveOperationException {
        String typeId = "flutter.material.TooltipVisibility";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId); WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.material", 100, 310, "TooltipVisibility"), node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals("TooltipVisibility", node.getDisplayName());
        for (String phrase : List.of("required Child", "not an SDK default", "nearest scope wins", "nearer true overrides an outer false", "literal Canvas preview", "There are no native Events")) {
            assertTrue(node.getShortDescription().contains(phrase), node.getShortDescription());
        }
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
        assertEquals("tooltipvisibility.svg", declaredIconPath(node).substring(declaredIconPath(node).lastIndexOf('/') + 1));
    }

    @Test
    void visibilityPaletteExplainsAllMaintainFlagsAndOptionalReplacementWithDedicatedIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Visibility";
        PaletteController controller = FlutterDesignerPalette.create(CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(), "flutter.basic", 300, 180, "Visibility"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Visibility", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("required child"));
        assertTrue(node.getShortDescription().contains("Visibility.maintain"));
        assertTrue(node.getShortDescription().contains("discard descendant state"));
        assertTrue(node.getShortDescription().contains("optional Replacement"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(), declaredIconPath(node));
    }

    @Test
    void preservesCatalogCategoryAndItemOrderWithLocalizedCategoryLabels() {
        PaletteController controller = FlutterDesignerPalette.create(CATALOG, ignored -> true);
        Node[] categories = root(controller).getChildren().getNodes(true);

        assertEquals(221, CATALOG.definitions().size());
        assertEquals(185, CATALOG.definitions().stream()
                .filter(WidgetDefinition::constConstructor)
                .count());

        assertEquals(
                List.of("flutter.material", "flutter.layout", "flutter.scrolling",
                        "flutter.basic", "flutter.accessibility", "flutter.interaction"),
                Arrays.stream(categories).map(Node::getName).toList());
        assertEquals(
                List.of("Material", "Layout", "Scrolling", "Basic", "Accessibility", "Interaction"),
                Arrays.stream(categories).map(Node::getDisplayName).toList());
        assertEquals(List.of("Scaffold", "AppBar", "Elevated Button", "Text Field", "Divider", "VerticalDivider", "Card", "Badge", "CircleAvatar", "LinearProgressIndicator", "CircularProgressIndicator", "RefreshProgressIndicator", "RefreshIndicator", "TextButton", "OutlinedButton", "FilledButton", "FloatingActionButton", "IconButton", "Checkbox", "Switch", "Slider", "RangeSlider", "Radio", "RadioGroup", "ListTile", "CheckboxListTile", "SwitchListTile", "RadioListTile", "ExpansionTile", "Tooltip", "TooltipVisibility", "TooltipTheme", "MenuItemButton", "MenuAnchor", "SubmenuButton", "MenuBar", "NavigationBar", "NavigationRail", "NavigationDrawer", "Drawer", "BottomAppBar", "BottomNavigationBar", "Material", "Scrollbar", "SliverAppBar", "SliverAppBar.medium", "SliverAppBar.large", "FlexibleSpaceBar", "FlexibleSpaceBarSettings", "AnimatedTheme", "Theme", "AnimatedIcon"),
                itemLabels(categories[0]));
        assertEquals(52, itemLabels(categories[0]).size());
        assertEquals(List.of(
                "Column", "Row", "Wrap", "Padding", "Center", "SizedBox", "AspectRatio",
                "Container", "Opacity", "Align", "FractionallySizedBox", "FittedBox",
                "ConstrainedBox", "UnconstrainedBox", "LimitedBox", "OverflowBox", "Stack",
                "Indexed Stack", "Expanded", "Flexible", "Spacer", "Baseline", "IntrinsicHeight",
                "IntrinsicWidth", "Offstage", "SizedOverflowBox", "Transform",
                "RotatedBox", "PreferredSize", "ListBody", "OverflowBar", "SafeArea", "LayoutBuilder", "OrientationBuilder", "DeviceOrientationBuilder", "ListenableBuilder", "AnimatedBuilder", "ValueListenableBuilder", "TweenAnimationBuilder", "AnimatedOpacity", "AnimatedAlign", "AnimatedPadding", "AnimatedSlide", "AnimatedScale", "AnimatedRotation", "AnimatedContainer", "AnimatedSize", "AnimatedPositioned", "AnimatedPositioned.fromRect", "AnimatedPositionedDirectional", "AnimatedDefaultTextStyle", "AnimatedPhysicalModel", "AnimatedFractionallySizedBox", "AnimatedCrossFade", "AnimatedSwitcher", "DefaultTextStyleTransition", "FadeTransition", "SlideTransition", "ScaleTransition", "RotationTransition", "SizeTransition", "PositionedTransition", "RelativePositionedTransition", "DecoratedBoxTransition", "AlignTransition", "MatrixTransition", "CustomSingleChildLayout", "CustomMultiChildLayout", "LayoutId"),
                itemLabels(categories[1]));
        assertEquals(69, itemLabels(categories[1]).size());
        assertEquals(List.of("ListView", "GridView.count", "GridView.extent", "SingleChildScrollView", "PageView", "ListWheelScrollView", "CustomScrollView", "SliverToBoxAdapter", "SliverList.list", "SliverGrid.count", "SliverGrid.extent", "SliverList.builder", "SliverList.separated", "SliverList.new", "SliverGrid.builder", "SliverGrid.list", "SliverGrid.new", "SliverPadding", "SliverFillRemaining", "SliverFillViewport", "SliverFillViewport.delegate", "SliverFixedExtentList.list", "SliverFixedExtentList.builder", "SliverFixedExtentList.new", "SliverPrototypeExtentList.list", "SliverPrototypeExtentList.builder", "SliverPrototypeExtentList.new", "SliverVariedExtentList.list", "SliverVariedExtentList.builder", "SliverVariedExtentList.new", "SliverMainAxisGroup", "SliverCrossAxisGroup", "SliverCrossAxisExpanded", "SliverConstrainedCrossAxis", "SliverOpacity", "SliverIgnorePointer", "SliverOffstage", "SliverVisibility", "SliverVisibility.maintain", "SliverSafeArea", "SliverAnimatedOpacity", "SliverLayoutBuilder", "SliverPersistentHeader", "SliverResizingHeader", "PinnedHeaderSliver", "SliverFloatingHeader", "DeviceOrientationBuilder (sliver)", "ListenableBuilder (sliver)", "AnimatedBuilder (sliver)", "ValueListenableBuilder (sliver)", "TweenAnimationBuilder (sliver)", "SliverFadeTransition"),
                itemLabels(categories[2]));
        assertEquals(52, itemLabels(categories[2]).size());
        assertEquals(List.of(
                "Text", "Icon", "Image", "ColoredBox", "Placeholder", "Directionality",
                "DecoratedBox", "Builder", "ClipRect", "ClipOval", "ClipRRect", "ClipPath",
                "ClipRSuperellipse", "PhysicalModel", "PhysicalShape", "RepaintBoundary", "IgnorePointer", "AbsorbPointer", "Visibility", "TickerMode", "DefaultTextHeightBehavior", "DefaultSelectionStyle", "IconTheme", "ImageIcon", "DefaultTextStyle", "DefaultTextStyle.merge", "ModalBarrier", "AnimatedModalBarrier", "FadeInImage", "RawImage", "ColorFiltered", "ImageFiltered", "BackdropFilter", "BackdropFilter.grouped", "BackdropGroup", "ShaderMask", "CustomPaint"),
                itemLabels(categories[3]));
        assertEquals(37, itemLabels(categories[3]).size());
        assertEquals(List.of("Exclude Semantics", "BlockSemantics", "MergeSemantics", "IndexedSemantics", "ExcludeFocus", "ExcludeFocusTraversal"), itemLabels(categories[4]));
        assertEquals(6, itemLabels(categories[4]).size());
        assertEquals(List.of("GestureDetector", "Listener", "MouseRegion", "Focus", "NotificationListener"), itemLabels(categories[5]));

        FlutterDesignerPaletteCategory material = categories[0].getLookup()
                .lookup(FlutterDesignerPaletteCategory.class);
        assertEquals(new FlutterDesignerPaletteCategory("flutter.material", 100, "Material"), material);
        assertEquals(new FlutterDesignerPaletteCategory(
                "flutter.accessibility", 400, "Accessibility"),
                categories[4].getLookup().lookup(FlutterDesignerPaletteCategory.class));
    }

    @Test
    void filtersDefinitionsBeforeGroupingAndOmitsEmptyCategories() {
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> CANVAS_WIDGETS.contains(definition.typeId().value()));
        Node[] categories = root(controller).getChildren().getNodes(true);

        assertEquals(
                List.of("flutter.material", "flutter.layout", "flutter.scrolling",
                        "flutter.basic", "flutter.accessibility", "flutter.interaction"),
                Arrays.stream(categories).map(Node::getName).toList());
        assertEquals(List.of("Scaffold", "AppBar", "Elevated Button", "Text Field", "Divider", "VerticalDivider", "Card", "Badge", "CircleAvatar", "LinearProgressIndicator", "CircularProgressIndicator", "RefreshProgressIndicator", "RefreshIndicator", "TextButton", "OutlinedButton", "FilledButton", "FloatingActionButton", "IconButton", "Checkbox", "Switch", "Slider", "RangeSlider", "Radio", "RadioGroup", "ListTile", "CheckboxListTile", "SwitchListTile", "RadioListTile", "ExpansionTile", "Tooltip", "TooltipVisibility", "TooltipTheme", "MenuItemButton", "MenuAnchor", "SubmenuButton", "MenuBar", "NavigationBar", "NavigationRail", "NavigationDrawer", "Drawer", "BottomAppBar", "BottomNavigationBar", "Material", "Scrollbar", "SliverAppBar", "SliverAppBar.medium", "SliverAppBar.large", "FlexibleSpaceBar", "FlexibleSpaceBarSettings", "AnimatedTheme", "Theme", "AnimatedIcon"),
                itemLabels(categories[0]));
        assertEquals(List.of(
                "Column", "Row", "Wrap", "Padding", "Center", "SizedBox", "AspectRatio",
                "Container", "Opacity", "Align", "FractionallySizedBox", "FittedBox",
                "ConstrainedBox", "UnconstrainedBox", "LimitedBox", "OverflowBox", "Stack",
                "Indexed Stack", "Expanded", "Flexible", "Spacer", "Baseline", "IntrinsicHeight",
                "IntrinsicWidth", "Offstage", "SizedOverflowBox", "Transform",
                "RotatedBox", "PreferredSize", "ListBody", "OverflowBar", "SafeArea", "LayoutBuilder", "OrientationBuilder", "DeviceOrientationBuilder", "ListenableBuilder", "AnimatedBuilder", "ValueListenableBuilder", "TweenAnimationBuilder", "AnimatedOpacity", "AnimatedAlign", "AnimatedPadding", "AnimatedSlide", "AnimatedScale", "AnimatedRotation", "AnimatedContainer", "AnimatedSize", "AnimatedPositioned", "AnimatedPositioned.fromRect", "AnimatedPositionedDirectional", "AnimatedDefaultTextStyle", "AnimatedPhysicalModel", "AnimatedFractionallySizedBox", "AnimatedCrossFade", "AnimatedSwitcher", "DefaultTextStyleTransition", "FadeTransition", "SlideTransition", "ScaleTransition", "RotationTransition", "SizeTransition", "PositionedTransition", "RelativePositionedTransition", "DecoratedBoxTransition", "AlignTransition", "MatrixTransition", "CustomSingleChildLayout", "CustomMultiChildLayout", "LayoutId"),
                itemLabels(categories[1]));
        assertEquals(List.of("ListView", "GridView.count", "GridView.extent", "SingleChildScrollView", "PageView", "ListWheelScrollView", "CustomScrollView", "SliverToBoxAdapter", "SliverList.list", "SliverGrid.count", "SliverGrid.extent", "SliverList.builder", "SliverList.separated", "SliverList.new", "SliverGrid.builder", "SliverGrid.list", "SliverGrid.new", "SliverPadding", "SliverFillRemaining", "SliverFillViewport", "SliverFillViewport.delegate", "SliverFixedExtentList.list", "SliverFixedExtentList.builder", "SliverFixedExtentList.new", "SliverPrototypeExtentList.list", "SliverPrototypeExtentList.builder", "SliverPrototypeExtentList.new", "SliverVariedExtentList.list", "SliverVariedExtentList.builder", "SliverVariedExtentList.new", "SliverMainAxisGroup", "SliverCrossAxisGroup", "SliverCrossAxisExpanded", "SliverConstrainedCrossAxis", "SliverOpacity", "SliverIgnorePointer", "SliverOffstage", "SliverVisibility", "SliverVisibility.maintain", "SliverSafeArea", "SliverAnimatedOpacity", "SliverLayoutBuilder", "SliverPersistentHeader", "SliverResizingHeader", "PinnedHeaderSliver", "SliverFloatingHeader", "DeviceOrientationBuilder (sliver)", "ListenableBuilder (sliver)", "AnimatedBuilder (sliver)", "ValueListenableBuilder (sliver)", "TweenAnimationBuilder (sliver)", "SliverFadeTransition"),
                itemLabels(categories[2]));
        assertEquals(List.of(
                "Text", "Icon", "Image", "ColoredBox", "Placeholder", "Directionality",
                "DecoratedBox", "Builder", "ClipRect", "ClipOval", "ClipRRect", "ClipPath",
                "ClipRSuperellipse", "PhysicalModel", "PhysicalShape", "RepaintBoundary", "IgnorePointer", "AbsorbPointer", "Visibility", "TickerMode", "DefaultTextHeightBehavior", "DefaultSelectionStyle", "IconTheme", "ImageIcon", "DefaultTextStyle", "DefaultTextStyle.merge", "ModalBarrier", "AnimatedModalBarrier", "FadeInImage", "RawImage", "ColorFiltered", "ImageFiltered", "BackdropFilter", "BackdropFilter.grouped", "BackdropGroup", "ShaderMask", "CustomPaint"),
                itemLabels(categories[3]));
        assertEquals(List.of("Exclude Semantics", "BlockSemantics", "MergeSemantics", "IndexedSemantics", "ExcludeFocus", "ExcludeFocusTraversal"), itemLabels(categories[4]));

        PaletteController textOnly = FlutterDesignerPalette.create(
                CATALOG,
                definition -> definition.typeId().value().equals("flutter.widgets.Text"));
        Node[] textCategories = root(textOnly).getChildren().getNodes(true);
        assertEquals(1, textCategories.length);
        assertEquals("flutter.basic", textCategories[0].getName());
        assertEquals(List.of("Text"), itemLabels(textCategories[0]));
    }

    @Test
    void itemLookupExposesImmutableItemAndExactCatalogDefinition() {
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> definition.typeId().value().equals("flutter.widgets.Padding"));
        Node itemNode = root(controller).getChildren().getNodes(true)[0]
                .getChildren().getNodes(true)[0];
        FlutterDesignerPaletteItem item = itemNode.getLookup().lookup(FlutterDesignerPaletteItem.class);
        WidgetDefinition definition = itemNode.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(), "flutter.layout", 200, 30, "Padding"),
                item);
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals(definition.typeId().value(), itemNode.getName());
        assertEquals(definition.palette().displayName(), itemNode.getDisplayName());
    }

    @Test
    void aspectRatioPaletteSelectionExposesReviewedMetadataAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.AspectRatio";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        60,
                        "AspectRatio"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("AspectRatio", node.getDisplayName());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void opacityPaletteSelectionExposesReviewedMetadataAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Opacity";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        80,
                        "Opacity"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Opacity", node.getDisplayName());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void alignPaletteSelectionExposesReviewedMetadataAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Align";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        90,
                        "Align"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Align", node.getDisplayName());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void fractionallySizedBoxPaletteSelectionExposesReviewedMetadataAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.FractionallySizedBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        100,
                        "FractionallySizedBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("FractionallySizedBox", node.getDisplayName());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void fittedBoxPaletteSelectionExposesReviewedMetadataAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.FittedBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        105,
                        "FittedBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("FittedBox", node.getDisplayName());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void constrainedBoxPaletteSelectionExposesReviewedMetadataIconAndDescription()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ConstrainedBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        106,
                        "ConstrainedBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("ConstrainedBox", node.getDisplayName());
        assertEquals(
                "Constrain an optional child with required minimum and maximum width and "
                + "height bounds. Palette creation starts unconstrained; unbounded and "
                + "expanding axes remain explicit.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void unconstrainedBoxPaletteSelectionExposesReviewedMetadataIconAndDescription()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.UnconstrainedBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        107,
                        "UnconstrainedBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("UnconstrainedBox", node.getDisplayName());
        assertEquals(
                "Let an optional child keep its natural size by removing constraints from both "
                + "axes or retaining exactly one axis, with explicit alignment, direction, and "
                + "clipping controls.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void limitedBoxPaletteSelectionExposesReviewedMetadataIconAndDescription()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.LimitedBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        108,
                        "LimitedBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("LimitedBox", node.getDisplayName());
        assertEquals(
                "Apply optional maximum width and height only when the corresponding incoming "
                + "axis is unbounded, while preserving bounded parent constraints and an "
                + "optional child.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void overflowBoxPaletteSelectionExposesReviewedMetadataIconAndDescription()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.OverflowBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        109,
                        "OverflowBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("OverflowBox", node.getDisplayName());
        assertEquals(
                "Override any combination of the incoming minimum and maximum width or height "
                + "constraints, align the optional child, and choose whether this box adopts "
                + "the largest permitted size or follows its child within the parent constraints.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void stackPaletteSelectionExposesReviewedMetadataAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Stack";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        110,
                        "Stack"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Stack", node.getDisplayName());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void expandedPaletteSelectionExposesReviewedWrapperMetadataAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Expanded";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        120,
                        "Expanded"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Expanded", node.getDisplayName());
        assertEquals(
                "Wrap an existing direct child of Row.children or Column.children with "
                + "Expanded. Drop Expanded on that child; terminal insertion and empty "
                + "placeholders are unavailable because Expanded.child is required.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void flexiblePaletteSelectionExposesReviewedWrapperMetadataAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Flexible";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        130,
                        "Flexible"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Flexible", node.getDisplayName());
        assertEquals(
                "Wrap an existing direct child of Row.children or Column.children with "
                + "Flexible. Drop Flexible on that child; terminal insertion and empty "
                + "placeholders are unavailable because Flexible.child is required.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void spacerPaletteSelectionExposesReviewedTerminalFlexMetadataAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Spacer";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        140,
                        "Spacer"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Spacer", node.getDisplayName());
        assertEquals(
                "Insert an empty flexible gap directly into Row.children or "
                + "Column.children. Spacer has no child; omitted flex preserves "
                + "Flutter's positive default of 1.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void baselinePaletteSelectionExposesReviewedMetadataIconAndCreationDefaults()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Baseline";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        150,
                        "Baseline"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Baseline", node.getDisplayName());
        assertEquals(
                "Position an optional child so its alphabetic or ideographic baseline "
                + "sits at a required logical-pixel offset from the top. Palette creation "
                + "starts at offset 24 with the alphabetic baseline.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void intrinsicHeightPaletteSelectionExposesPerformanceWarningAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.IntrinsicHeight";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        160,
                        "IntrinsicHeight"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("IntrinsicHeight", node.getDisplayName());
        assertEquals(
                "Size an optional child to its intrinsic height before final layout. "
                + "This adds a speculative layout pass and can be O(N²) in tree depth, "
                + "so avoid it when constraints can express the layout.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void intrinsicWidthPaletteSelectionExposesSnappingConstraintsPerformanceAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.IntrinsicWidth";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        170,
                        "IntrinsicWidth"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("IntrinsicWidth", node.getDisplayName());
        assertEquals(
                "Size an optional child to its maximum intrinsic width, optionally "
                + "snapping width and height to non-negative step multiples while honoring "
                + "parent constraints. A null or zero stepWidth uses the maximum intrinsic "
                + "width; a null or zero stepHeight leaves height unconstrained. This adds "
                + "a speculative layout pass and can be O(N²) in tree depth.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void offstagePaletteSelectionExplainsHiddenActiveChildAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Offstage";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        180,
                        "Offstage"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Offstage", node.getDisplayName());
        assertEquals(
                "Keep an optional child laid out and active while optionally removing it "
                + "from paint, hit testing, and the parent layout space. An offstage child "
                + "can still receive focus and run animations; remove it from the tree "
                + "instead when hiding it long-term.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void sizedOverflowBoxPaletteSelectionExplainsConstraintsOverflowAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.SizedOverflowBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        190,
                        "SizedOverflowBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("SizedOverflowBox", node.getDisplayName());
        assertEquals(
                "Request a required logical size while passing the original parent "
                + "constraints unchanged to an optional child. Parent constraints still "
                + "constrain this box; alignment positions a differently sized child, "
                + "which may paint outside the box.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void transformPaletteSelectionExplainsPaintOnlyMatrixDefaultsAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Transform";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        200,
                        "Transform"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Transform", node.getDisplayName());
        assertEquals(
                "Apply a required finite 4×4 Matrix4 to an optional child during painting "
                + "without changing its layout size, with origin, alignment, "
                + "transformed-hit-test, and filter-quality controls. Palette creation "
                + "starts with the identity matrix.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void rotatedBoxPaletteSelectionExplainsLayoutRotationDefaultAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.RotatedBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        210,
                        "RotatedBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("RotatedBox", node.getDisplayName());
        assertEquals(
                "Rotate an optional child clockwise by a required signed number of "
                + "quarter turns before layout. Odd quarter turns swap the child’s "
                + "width and height; palette creation starts at one quarter turn.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void listBodyPaletteSelectionExplainsAxisOrderingDefaultsAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ListBody";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        220,
                        "ListBody"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("ListBody", node.getDisplayName());
        assertEquals(
                "Arrange an ordered child list sequentially along a vertical or "
                + "horizontal main axis, stretching every child to the parent dimension "
                + "on the cross axis. ListBody does not scroll; palette creation preserves "
                + "Flutter’s vertical-axis and forward-order defaults.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void overflowBarPaletteSelectionExplainsResponsiveLayoutDefaultsAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.OverflowBar";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        230,
                        "OverflowBar"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("OverflowBar", node.getDisplayName());
        assertEquals(
                "Lay out an ordered child list in one horizontal row while it fits, then "
                + "switch the same children to a vertical overflow column when their total "
                + "width exceeds the available width. Palette creation preserves Flutter’s "
                + "zero spacing, start overflow alignment, downward overflow direction, "
                + "and ambient text direction defaults.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void safeAreaPaletteSelectionExplainsRequiredWrapperDefaultsAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.SafeArea";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        240,
                        "SafeArea"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("SafeArea", node.getDisplayName());
        assertEquals(
                "Wrap an existing widget so MediaQuery padding keeps it clear of system "
                + "intrusions on selected sides. The wrapper preserves Flutter’s four "
                + "enabled sides, zero minimum inset, and disabled bottom-view-padding "
                + "maintenance defaults until explicitly edited.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void gridViewCountPaletteSelectionExplainsStaticGridDefaultsAndIcon()
            throws ReflectiveOperationException {
        String typeId = GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value();
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals("flutter.scrolling", definition.palette().categoryId());
        assertEquals(250, definition.palette().categoryOrder());
        assertEquals(20, definition.palette().itemOrder());
        assertEquals("GridView.count", node.getDisplayName());
        assertEquals(
                "Create a static scrolling grid with a required positive cross-axis count "
                + "and exact ordered children. Palette creation starts with two columns "
                + "and preserves Flutter’s vertical scrolling, unit child aspect ratio, "
                + "zero spacing, and default child lifecycle, clipping, semantics, and "
                + "restoration behavior.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void singleChildScrollViewPaletteSelectionExplainsPreservedDefaultsAndIcon()
            throws ReflectiveOperationException {
        String typeId = SingleChildScrollViewWidgetPropertySchema
                .SINGLE_CHILD_SCROLL_VIEW_TYPE.value();
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertNotNull(definition);
        assertEquals("flutter.scrolling", definition.palette().categoryId());
        assertEquals(250, definition.palette().categoryOrder());
        assertEquals(30, definition.palette().itemOrder());
        assertEquals("SingleChildScrollView", node.getDisplayName());
        assertEquals(
                "Create one optional child inside a vertically or horizontally scrolling "
                + "viewport. Palette creation preserves Flutter’s vertical, forward, "
                + "platform-physics, opaque hit-test, hard-edge clipping, inherited "
                + "keyboard-dismissal, and ambient primary-controller defaults; runtime "
                + "controller state remains outside this Designer slice.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void imagePaletteSelectionExplainsRequiredDeclaredAssetCreation()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Image";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        30,
                        "Image"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertTrue(node.getShortDescription().contains(
                "deterministic first sorted asset"));
        assertTrue(node.getShortDescription().contains(
                "required Image.image"));
        assertTrue(node.getShortDescription().contains(
                "editable placeholder"));
        assertTrue(node.getShortDescription().contains(
                "replaced in Image properties"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void coloredBoxPaletteSelectionExplainsRequiredColorAndPreservedAntiAliasDefault()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ColoredBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        40,
                        "ColoredBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals("ColoredBox", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("required solid color"));
        assertTrue(node.getShortDescription().contains("opaque blue literal"));
        assertTrue(node.getShortDescription().contains("enabled default"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void placeholderPaletteSelectionExplainsPaintFallbackDefaultsAndOptionalChild()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Placeholder";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        50,
                        "Placeholder"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Placeholder", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("optional child"));
        assertTrue(node.getShortDescription().contains("blue-grey"));
        assertTrue(node.getShortDescription().contains("400 × 400"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void directionalityPaletteSelectionExplainsRequiredWrapperDirectionAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.Directionality";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        60,
                        "Directionality"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Directionality", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("explicit inherited text-flow"));
        assertTrue(node.getShortDescription().contains("left-to-right"));
        assertTrue(node.getShortDescription().contains("required child"));
        assertTrue(node.getShortDescription().contains("LTR and RTL"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void decoratedBoxPaletteSelectionExplainsTypedDecorationPositionAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.DecoratedBox";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        70,
                        "DecoratedBox"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("DecoratedBox", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("BoxDecoration"));
        assertTrue(node.getShortDescription().contains("behind or in front"));
        assertTrue(node.getShortDescription().contains("optional child"));
        assertTrue(node.getShortDescription().contains("typed decoration editor"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void excludeSemanticsPaletteSelectionExplainsAccessibilityEffectAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ExcludeSemantics";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.accessibility",
                        400,
                        10,
                        "ExcludeSemantics"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Exclude Semantics", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("accessibility semantics tree"));
        assertTrue(node.getShortDescription().contains("layout, painting, and hit testing"));
        assertTrue(node.getShortDescription().contains("excluding=true"));
        assertTrue(node.getShortDescription().contains("explicit false"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void indexedStackPaletteSelectionExplainsVisibleIndexAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.IndexedStack";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.layout",
                        200,
                        115,
                        "IndexedStack"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("Indexed Stack", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("painting, hit-testing"));
        assertTrue(node.getShortDescription().contains("default 0"));
        assertTrue(node.getShortDescription().contains("null"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void clipRectPaletteSelectionExplainsRectangularClippingAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ClipRect";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        80,
                        "ClipRect"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("ClipRect", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("rectangular bounds"));
        assertTrue(node.getShortDescription().contains("hard-edge default"));
        assertTrue(node.getShortDescription()
                .toLowerCase(java.util.Locale.ROOT)
                .contains("custom clippers"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void clipOvalPaletteSelectionExplainsInscribedOvalClippingAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ClipOval";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        90,
                        "ClipOval"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("ClipOval", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("oval inscribed"));
        assertTrue(node.getShortDescription().contains("anti-alias default"));
        assertTrue(node.getShortDescription().contains("custom clippers"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void clipRRectPaletteSelectionExplainsTypedRoundedClippingAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ClipRRect";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        100,
                        "ClipRRect"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("ClipRRect", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("rounded rectangular"));
        assertTrue(node.getShortDescription().contains("direction-aware"));
        assertTrue(node.getShortDescription().contains("BorderRadius.zero"));
        assertTrue(node.getShortDescription()
                .toLowerCase(java.util.Locale.ROOT)
                .contains("custom clippers"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void clipRSuperellipsePaletteSelectionExplainsTypedRoundedClippingAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ClipRSuperellipse";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        120,
                        "ClipRSuperellipse"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("ClipRSuperellipse", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("rounded superellipse"));
        assertTrue(node.getShortDescription().contains("direction-aware"));
        assertTrue(node.getShortDescription().contains("BorderRadius.zero"));
        assertTrue(node.getShortDescription()
                .toLowerCase(java.util.Locale.ROOT)
                .contains("customclipper<rsuperellipse>"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void excludeFocusPaletteSelectionExplainsRequiredChildAndFocusTransitionAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ExcludeFocus";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.accessibility", 400, 50, "ExcludeFocus"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("ExcludeFocus", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("required child"));
        assertTrue(node.getShortDescription().contains("true unfocuses descendants"));
        assertTrue(node.getShortDescription().contains("without automatically restoring it"));
        assertTrue(node.getShortDescription().contains("no empty child is created"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void excludeFocusTraversalPaletteSelectionExplainsRequiredChildAndFocusTransitionAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ExcludeFocusTraversal";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.accessibility", 400, 60, "ExcludeFocusTraversal"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("ExcludeFocusTraversal", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("required child"));
        assertTrue(node.getShortDescription().contains("Tab traversal skips descendants"));
        assertTrue(node.getShortDescription().contains("direct requestFocus remains allowed and existing focus is retained"));
        assertTrue(node.getShortDescription().contains("no empty child is created"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void blockSemanticsPaletteSelectionExplainsAccessibilityPaintOrderAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.BlockSemantics";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.accessibility", 400, 20, "BlockSemantics"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("BlockSemantics", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("earlier-painted semantic nodes"));
        assertTrue(node.getShortDescription().contains("same semantics container"));
        assertTrue(node.getShortDescription().contains("Own child and later nodes remain"));
        assertTrue(node.getShortDescription().contains("pointer hit testing are unchanged"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void absorbPointerPaletteSelectionExplainsFullSurfaceAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.AbsorbPointer";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.basic", 300, 170, "AbsorbPointer"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("AbsorbPointer", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("preserving layout and painting"));
        assertTrue(node.getShortDescription().contains("Both booleans are editable"));
        assertTrue(node.getShortDescription().contains("events do not pass through"));
        assertTrue(node.getShortDescription().contains("Deprecated Ignoring semantics"));
        assertTrue(node.getShortDescription().contains("Designer selection and editing remain available"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void ignorePointerPaletteSelectionExplainsFullSurfaceAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.IgnorePointer";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.basic", 300, 160, "IgnorePointer"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("IgnorePointer", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("preserving layout and painting"));
        assertTrue(node.getShortDescription().contains("Both booleans are editable"));
        assertTrue(node.getShortDescription().contains("Deprecated Ignoring semantics"));
        assertTrue(node.getShortDescription().contains("Designer editing remains available"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void indexedSemanticsPaletteSelectionExplainsFullSurfaceAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.IndexedSemantics";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.accessibility", 400, 40, "IndexedSemantics"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("IndexedSemantics", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("required signed integer index"));
        assertTrue(node.getShortDescription().contains("Creation starts at 0"));
        assertTrue(node.getShortDescription().contains("Index cannot be unset"));
        assertTrue(node.getShortDescription().contains("does not sort children"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void mergeSemanticsPaletteSelectionExplainsFullSurfaceAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.MergeSemantics";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.accessibility", 400, 30, "MergeSemantics"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("MergeSemantics", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("semantics subtree into one node"));
        assertTrue(node.getShortDescription().contains("No scalar constructor properties"));
        assertTrue(node.getShortDescription().contains("Labels are joined with newlines"));
        assertTrue(node.getShortDescription().contains("first handler in tree order"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void repaintBoundaryPaletteSelectionExplainsFullSurfaceAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.RepaintBoundary";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.basic", 300, 150, "RepaintBoundary"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("RepaintBoundary", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("separate display list"));
        assertTrue(node.getShortDescription().contains("No scalar properties"));
        assertTrue(node.getShortDescription().contains("Designer owns widget identity"));
        assertTrue(node.getShortDescription().contains("not a guaranteed performance improvement"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void physicalShapePaletteSelectionExplainsFullSurfaceAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.PhysicalShape";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.basic", 300, 140, "PhysicalShape"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("PhysicalShape", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("elevated path-shaped surface"));
        assertTrue(node.getShortDescription().contains("ShapeBorderClipper"));
        assertTrue(node.getShortDescription().contains("CustomClipper<Path>"));
        assertTrue(node.getShortDescription().contains("preview is explicitly unavailable"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void physicalModelPaletteSelectionExplainsFullSurfaceAndDistinctIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.PhysicalModel";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG, definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);
        assertEquals(new FlutterDesignerPaletteItem(definition.typeId(),
                "flutter.basic", 300, 130, "PhysicalModel"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("PhysicalModel", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("elevated physical surface"));
        assertTrue(node.getShortDescription().contains("physical elliptical"));
        assertTrue(node.getShortDescription().contains("ignores but preserves"));
        assertFalse(node.getShortDescription().contains("clipper"));
        assertEquals(FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void clipPathPaletteSelectionExplainsExclusiveTypedBranchesAndIcon()
            throws ReflectiveOperationException {
        String typeId = "flutter.widgets.ClipPath";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.basic",
                        300,
                        110,
                        "ClipPath"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertSame(CATALOG.find(definition.typeId()).orElseThrow(), definition);
        assertEquals("ClipPath", node.getDisplayName());
        assertTrue(node.getShortDescription().contains("CustomClipper<Path>"));
        assertTrue(node.getShortDescription().contains("ShapeBorder"));
        assertTrue(node.getShortDescription().contains("mutually exclusive"));
        assertTrue(node.getShortDescription().contains("non-const ClipPath.shape"));
        assertTrue(node.getShortDescription().contains("anti-alias"));
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void textFieldPaletteSelectionExplainsRuntimeOnlyEditingState()
            throws ReflectiveOperationException {
        String typeId = "flutter.material.TextField";
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> typeId.equals(definition.typeId().value()));
        Node node = itemNode(controller, typeId);
        WidgetDefinition definition = node.getLookup().lookup(WidgetDefinition.class);

        assertEquals(
                new FlutterDesignerPaletteItem(
                        definition.typeId(),
                        "flutter.material",
                        100,
                        40,
                        "Text Field"),
                node.getLookup().lookup(FlutterDesignerPaletteItem.class));
        assertEquals(
                "Create a Material TextField leaf immediately, without a creation dialog "
                + "or stored constructor defaults. Runtime typed text, selection, "
                + "controller state, and focus state are not stored by Designer.",
                node.getShortDescription());
        assertEquals(
                FlutterWidgetIconRegistry.findIconPath(definition.typeId()).orElseThrow(),
                declaredIconPath(node));
    }

    @Test
    void exposesNoPaletteActionsAndRejectsPaletteMutation() {
        PaletteController controller = FlutterDesignerPalette.create(CATALOG, ignored -> true);
        Lookup rootLookup = controller.getRoot();
        PaletteActions actions = rootLookup.lookup(PaletteActions.class);
        DragAndDropHandler dragAndDrop = rootLookup.lookup(DragAndDropHandler.class);

        assertArrayEquals(new Action[0], actions.getImportActions());
        assertArrayEquals(new Action[0], actions.getCustomPaletteActions());
        assertArrayEquals(new Action[0], actions.getCustomCategoryActions(Lookup.EMPTY));
        assertArrayEquals(new Action[0], actions.getCustomItemActions(Lookup.EMPTY));
        assertNull(actions.getPreferredAction(Lookup.EMPTY));
        assertNull(actions.getRefreshAction());
        assertNull(actions.getResetAction());

        assertFalse(dragAndDrop.canDrop(Lookup.EMPTY, new DataFlavor[0], 0));
        assertFalse(dragAndDrop.doDrop(Lookup.EMPTY, null, 0, 0));
        assertFalse(dragAndDrop.canReorderCategories(Lookup.EMPTY));
        assertFalse(dragAndDrop.moveCategory(Lookup.EMPTY, 0));

        Node root = root(controller);
        assertReadOnly(root);
        for (Node category : root.getChildren().getNodes(true)) {
            assertReadOnly(category);
            for (Node item : category.getChildren().getNodes(true)) {
                assertReadOnly(item);
            }
        }
    }

    @Test
    void createsIndependentControllerAndNodeGraphsForEachContext() {
        PaletteController first = FlutterDesignerPalette.create(CATALOG, ignored -> true);
        PaletteController second = FlutterDesignerPalette.create(CATALOG, ignored -> true);

        assertNotSame(first, second);
        assertNotSame(root(first), root(second));
        assertNotSame(
                root(first).getChildren().getNodes(true)[0],
                root(second).getChildren().getNodes(true)[0]);
        assertTrue(first.getSelectedItem().lookupAll(Object.class).isEmpty());
        assertTrue(second.getSelectedItem().lookupAll(Object.class).isEmpty());
    }

    @Test
    void tokenizesOnlyAllowlistedTextWithNoDefinitionInTheStringPayload()
            throws Exception {
        FlutterDesignerPaletteDragRegistry registry =
                new FlutterDesignerPaletteDragRegistry();
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                ignored -> true,
                registry,
                () -> true,
                definition -> definition.typeId().value().equals(
                        "flutter.widgets.Text"));
        Transferable textTransfer = itemNode(
                controller, "flutter.widgets.Text").drag();

        assertTrue(textTransfer.isDataFlavorSupported(DataFlavor.stringFlavor));
        String token = assertInstanceOf(
                String.class,
                textTransfer.getTransferData(DataFlavor.stringFlavor));
        assertTrue(token.matches(
                "nbfdnd:v1:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}:"
                + "[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"));
        assertFalse(token.contains("flutter.widgets.Text"));
        assertFalse(token.contains("WidgetDefinition"));
        assertEquals(
                "flutter.widgets.Text",
                registry.consume(token).orElseThrow().value());

        Transferable paddingTransfer = itemNode(
                controller, "flutter.widgets.Padding").drag();
        assertFalse(paddingTransfer.isDataFlavorSupported(DataFlavor.stringFlavor));
        assertEquals(0, registry.outstandingCount());
    }

    @Test
    void tokenizesEveryCanvasSupportedPaletteWidgetWithItsAuthoritativeType()
            throws Exception {
        FlutterDesignerPaletteDragRegistry registry =
                new FlutterDesignerPaletteDragRegistry();
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                CanvasModelPayloadCodec::supports,
                registry,
                () -> true,
                CanvasModelPayloadCodec::supports);

        assertEquals(CANVAS_WIDGETS, itemTypeIds(controller),
                "the Canvas Palette must expose exactly the payload-codec surface");
        assertTrue(java.util.Collections.disjoint(
                itemTypeIds(controller), NON_CANVAS_BUILT_INS));
        for (String typeId : CANVAS_WIDGETS.stream().sorted().toList()) {
            Transferable transfer = itemNode(controller, typeId).drag();
            assertTrue(transfer.isDataFlavorSupported(DataFlavor.stringFlavor), typeId);
            String token = assertInstanceOf(
                    String.class,
                    transfer.getTransferData(DataFlavor.stringFlavor),
                    typeId);
            assertEquals(typeId, registry.consume(token).orElseThrow().value(), typeId);
            assertTrue(registry.consume(token).isEmpty(),
                    typeId + " token must be one-shot");
        }
        assertEquals(0, registry.outstandingCount());

        FlutterDesignerPaletteDragRegistry allCatalogRegistry =
                new FlutterDesignerPaletteDragRegistry();
        PaletteController allCatalog = FlutterDesignerPalette.create(
                CATALOG,
                ignored -> true,
                allCatalogRegistry,
                () -> true,
                CanvasModelPayloadCodec::supports);
        for (String typeId : NON_CANVAS_BUILT_INS.stream().sorted().toList()) {
            Transferable transfer = itemNode(allCatalog, typeId).drag();
            assertFalse(transfer.isDataFlavorSupported(DataFlavor.stringFlavor),
                    typeId + " must not become draggable through the Canvas token path");
        }
        assertEquals(0, allCatalogRegistry.outstandingCount());
    }

    @Test
    void startingAnotherTextDragRevokesTheCanceledTransferToken()
            throws Exception {
        FlutterDesignerPaletteDragRegistry registry =
                new FlutterDesignerPaletteDragRegistry();
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                ignored -> true,
                registry,
                () -> true,
                definition -> definition.typeId().value().equals(
                        "flutter.widgets.Text"));
        Transferable canceled = itemNode(
                controller, "flutter.widgets.Text").drag();
        String canceledToken = (String) canceled.getTransferData(
                DataFlavor.stringFlavor);

        Transferable replacement = itemNode(
                controller, "flutter.widgets.Text").drag();
        String replacementToken = (String) replacement.getTransferData(
                DataFlavor.stringFlavor);

        assertEquals(1, registry.outstandingCount());
        assertTrue(registry.consume(canceledToken).isEmpty());
        assertEquals(
                "flutter.widgets.Text",
                registry.consume(replacementToken).orElseThrow().value());
    }

    @Test
    void reportsTheOpaqueTokenAndExactDefinitionBeforeNativeDragBegins()
            throws Exception {
        FlutterDesignerPaletteDragRegistry registry =
                new FlutterDesignerPaletteDragRegistry();
        List<String> tokens = new ArrayList<>();
        List<WidgetDefinition> definitions = new ArrayList<>();
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                ignored -> true,
                registry,
                () -> true,
                definition -> definition.typeId().value().equals(
                        "flutter.material.AppBar"),
                (token, definition) -> {
                    tokens.add(token);
                    definitions.add(definition);
                    return true;
                });

        Transferable transfer = itemNode(
                controller, "flutter.material.AppBar").drag();
        String token = assertInstanceOf(
                String.class,
                transfer.getTransferData(DataFlavor.stringFlavor));

        assertEquals(List.of(token), tokens);
        assertEquals(List.of(CATALOG.find(
                new dev.flutter.netbeans.designer.model.WidgetTypeId(
                        "flutter.material.AppBar")).orElseThrow()), definitions);
        assertEquals("flutter.material.AppBar",
                registry.consume(token).orElseThrow().value());
    }

    @Test
    void revokesTheTokenWhenSourceProjectionFailsBeforeTransferPublication()
            throws Exception {
        FlutterDesignerPaletteDragRegistry registry =
                new FlutterDesignerPaletteDragRegistry();
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                ignored -> true,
                registry,
                () -> true,
                ignored -> true,
                (token, definition) -> {
                    throw new IllegalStateException("projection unavailable");
                });

        Transferable transfer = itemNode(
                controller, "flutter.material.AppBar").drag();

        assertFalse(transfer.isDataFlavorSupported(DataFlavor.stringFlavor));
        assertEquals(0, registry.outstandingCount());
    }

    @Test
    void revokesTheTokenWhenSourceProjectionDeclinesAuthorization()
            throws Exception {
        FlutterDesignerPaletteDragRegistry registry =
                new FlutterDesignerPaletteDragRegistry();
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                ignored -> true,
                registry,
                () -> true,
                ignored -> true,
                (token, definition) -> false);

        Transferable transfer = itemNode(
                controller, "flutter.material.AppBar").drag();

        assertFalse(transfer.isDataFlavorSupported(DataFlavor.stringFlavor));
        assertEquals(0, registry.outstandingCount());
    }

    @Test
    void disabledTokenSourceAddsNoFlavorAndDoesNotEvaluateTheAllowlist()
            throws Exception {
        FlutterDesignerPaletteDragRegistry registry =
                new FlutterDesignerPaletteDragRegistry();
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                ignored -> true,
                registry,
                () -> false,
                definition -> {
                    throw new AssertionError("disabled drag must not consult allowlist");
                });
        Transferable transfer = itemNode(
                controller, "flutter.widgets.Text").drag();

        assertFalse(transfer.isDataFlavorSupported(DataFlavor.stringFlavor));
        assertEquals(0, registry.outstandingCount());
    }

    @Test
    void tokenEnabledPaletteStillRejectsImportsAndReordering() {
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                ignored -> true,
                new FlutterDesignerPaletteDragRegistry(),
                () -> true,
                ignored -> true);
        DragAndDropHandler dragAndDrop = dragAndDrop(controller);

        assertFalse(dragAndDrop.canDrop(
                Lookup.EMPTY,
                new DataFlavor[]{DataFlavor.stringFlavor},
                java.awt.dnd.DnDConstants.ACTION_COPY));
        assertFalse(dragAndDrop.doDrop(
                Lookup.EMPTY,
                new java.awt.datatransfer.StringSelection("forged"),
                java.awt.dnd.DnDConstants.ACTION_COPY,
                0));
        assertFalse(dragAndDrop.canReorderCategories(Lookup.EMPTY));
        assertFalse(dragAndDrop.moveCategory(Lookup.EMPTY, 0));
    }

    @Test
    void fortyNineCanvasItemNodesDeclareTheirMatchingUniqueRegistryIconsWithoutRendering()
            throws ReflectiveOperationException {
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> CANVAS_WIDGETS.contains(definition.typeId().value()));
        LinkedHashMap<String, String> nodeIcons = new LinkedHashMap<>();

        for (Node category : root(controller).getChildren().getNodes(true)) {
            for (Node item : category.getChildren().getNodes(true)) {
                WidgetDefinition definition = item.getLookup().lookup(WidgetDefinition.class);
                String expectedIcon = FlutterWidgetIconRegistry
                        .findIconPath(definition.typeId())
                        .orElseThrow();
                nodeIcons.put(definition.typeId().value(), declaredIconPath(item));
                assertEquals(expectedIcon, declaredIconPath(item), definition.typeId().value());
            }
        }

        assertEquals(CANVAS_WIDGETS, nodeIcons.keySet());
        assertEquals(221, Set.copyOf(nodeIcons.values()).size(),
                "palette items must not share a generic widget icon");
    }

    private static Node root(PaletteController controller) {
        return controller.getRoot().lookup(Node.class);
    }

    private static DragAndDropHandler dragAndDrop(PaletteController controller) {
        return controller.getRoot().lookup(DragAndDropHandler.class);
    }

    private static Node itemNode(PaletteController controller, String typeId) {
        return Arrays.stream(root(controller).getChildren().getNodes(true))
                .flatMap(category -> Arrays.stream(
                        category.getChildren().getNodes(true)))
                .filter(node -> typeId.equals(node.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static Set<String> itemTypeIds(PaletteController controller) {
        return Set.copyOf(Arrays.stream(
                        root(controller).getChildren().getNodes(true))
                .flatMap(category -> Arrays.stream(
                        category.getChildren().getNodes(true)))
                .map(Node::getName)
                .toList());
    }

    private static List<String> itemLabels(Node category) {
        return Arrays.stream(category.getChildren().getNodes(true))
                .map(Node::getDisplayName)
                .toList();
    }

    private static void assertReadOnly(Node node) {
        assertFalse(node.canRename());
        assertFalse(node.canDestroy());
        assertFalse(node.canCopy());
        assertFalse(node.canCut());
        assertNull(node.getPreferredAction());
        assertEquals(0, node.getNewTypes().length);
        assertEquals(Boolean.TRUE, node.getValue(PaletteController.ATTR_IS_READONLY));
    }

    private static String declaredIconPath(Node node) throws ReflectiveOperationException {
        if (node instanceof FilterNode) {
            Field original = FilterNode.class.getDeclaredField("original");
            original.setAccessible(true);
            return declaredIconPath((Node) original.get(node));
        }
        Field base = AbstractNode.class.getDeclaredField("iconBase");
        Field extension = AbstractNode.class.getDeclaredField("iconExtension");
        base.setAccessible(true);
        extension.setAccessible(true);
        return base.get(node) + (String) extension.get(node);
    }
}
