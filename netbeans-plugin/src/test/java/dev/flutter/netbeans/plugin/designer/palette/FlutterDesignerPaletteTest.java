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
import dev.flutter.netbeans.designer.catalog.SingleChildScrollViewWidgetPropertySchema;
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
            "flutter.widgets.ListBody",
            "flutter.widgets.OverflowBar",
            "flutter.widgets.SafeArea",
            "flutter.widgets.ListView",
            GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value(),
            SingleChildScrollViewWidgetPropertySchema
                    .SINGLE_CHILD_SCROLL_VIEW_TYPE.value());
    private static final Set<String> NON_CANVAS_BUILT_INS = Set.of();

    @Test
    void preservesCatalogCategoryAndItemOrderWithLocalizedCategoryLabels() {
        PaletteController controller = FlutterDesignerPalette.create(CATALOG, ignored -> true);
        Node[] categories = root(controller).getChildren().getNodes(true);

        assertEquals(44, CATALOG.definitions().size());
        assertEquals(38, CATALOG.definitions().stream()
                .filter(WidgetDefinition::constConstructor)
                .count());

        assertEquals(
                List.of("flutter.material", "flutter.layout", "flutter.scrolling",
                        "flutter.basic"),
                Arrays.stream(categories).map(Node::getName).toList());
        assertEquals(
                List.of("Material", "Layout", "Scrolling", "Basic"),
                Arrays.stream(categories).map(Node::getDisplayName).toList());
        assertEquals(List.of("Scaffold", "AppBar", "Elevated Button", "Text Field"),
                itemLabels(categories[0]));
        assertEquals(List.of(
                "Column", "Row", "Wrap", "Padding", "Center", "SizedBox", "AspectRatio",
                "Container", "Opacity", "Align", "FractionallySizedBox", "FittedBox",
                "ConstrainedBox", "UnconstrainedBox", "LimitedBox", "OverflowBox", "Stack",
                "Expanded", "Flexible", "Spacer", "Baseline", "IntrinsicHeight",
                "IntrinsicWidth", "Offstage", "SizedOverflowBox", "Transform",
                "RotatedBox", "ListBody", "OverflowBar", "SafeArea"),
                itemLabels(categories[1]));
        assertEquals(30, itemLabels(categories[1]).size());
        assertEquals(List.of("ListView", "GridView.count", "SingleChildScrollView"),
                itemLabels(categories[2]));
        assertEquals(List.of(
                "Text", "Icon", "Image", "ColoredBox", "Placeholder", "Directionality",
                "DecoratedBox"),
                itemLabels(categories[3]));

        FlutterDesignerPaletteCategory material = categories[0].getLookup()
                .lookup(FlutterDesignerPaletteCategory.class);
        assertEquals(new FlutterDesignerPaletteCategory("flutter.material", 100, "Material"), material);
    }

    @Test
    void filtersDefinitionsBeforeGroupingAndOmitsEmptyCategories() {
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> CANVAS_WIDGETS.contains(definition.typeId().value()));
        Node[] categories = root(controller).getChildren().getNodes(true);

        assertEquals(
                List.of("flutter.material", "flutter.layout", "flutter.scrolling",
                        "flutter.basic"),
                Arrays.stream(categories).map(Node::getName).toList());
        assertEquals(List.of("Scaffold", "AppBar", "Elevated Button", "Text Field"),
                itemLabels(categories[0]));
        assertEquals(List.of(
                "Column", "Row", "Wrap", "Padding", "Center", "SizedBox", "AspectRatio",
                "Container", "Opacity", "Align", "FractionallySizedBox", "FittedBox",
                "ConstrainedBox", "UnconstrainedBox", "LimitedBox", "OverflowBox", "Stack",
                "Expanded", "Flexible", "Spacer", "Baseline", "IntrinsicHeight",
                "IntrinsicWidth", "Offstage", "SizedOverflowBox", "Transform",
                "RotatedBox", "ListBody", "OverflowBar", "SafeArea"),
                itemLabels(categories[1]));
        assertEquals(List.of("ListView", "GridView.count", "SingleChildScrollView"),
                itemLabels(categories[2]));
        assertEquals(List.of(
                "Text", "Icon", "Image", "ColoredBox", "Placeholder", "Directionality",
                "DecoratedBox"),
                itemLabels(categories[3]));

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
    void fortyFourCanvasItemNodesDeclareTheirMatchingUniqueRegistryIconsWithoutRendering()
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
        assertEquals(44, Set.copyOf(nodeIcons.values()).size(),
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
