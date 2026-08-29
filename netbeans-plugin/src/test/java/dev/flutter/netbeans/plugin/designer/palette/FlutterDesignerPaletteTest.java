package dev.flutter.netbeans.plugin.designer.palette;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
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
            "flutter.widgets.Column",
            "flutter.widgets.Row",
            "flutter.widgets.Text",
            "flutter.widgets.Icon",
            "flutter.widgets.Padding",
            "flutter.widgets.Center",
            "flutter.widgets.SizedBox");
    private static final Set<String> NON_CANVAS_BUILT_INS = Set.of(
            "flutter.material.ElevatedButton");

    @Test
    void preservesCatalogCategoryAndItemOrderWithLocalizedCategoryLabels() {
        PaletteController controller = FlutterDesignerPalette.create(CATALOG, ignored -> true);
        Node[] categories = root(controller).getChildren().getNodes(true);

        assertEquals(
                List.of("flutter.material", "flutter.layout", "flutter.basic"),
                Arrays.stream(categories).map(Node::getName).toList());
        assertEquals(
                List.of("Material", "Layout", "Basic"),
                Arrays.stream(categories).map(Node::getDisplayName).toList());
        assertEquals(List.of("Scaffold", "AppBar", "Elevated Button"), itemLabels(categories[0]));
        assertEquals(List.of("Column", "Row", "Padding", "Center", "SizedBox"), itemLabels(categories[1]));
        assertEquals(List.of("Text", "Icon"), itemLabels(categories[2]));

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
                List.of("flutter.material", "flutter.layout", "flutter.basic"),
                Arrays.stream(categories).map(Node::getName).toList());
        assertEquals(List.of("Scaffold", "AppBar"), itemLabels(categories[0]));
        assertEquals(List.of("Column", "Row", "Padding", "Center", "SizedBox"),
                itemLabels(categories[1]));
        assertEquals(List.of("Text", "Icon"), itemLabels(categories[2]));

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
    void nineCanvasItemNodesDeclareTheirMatchingUniqueRegistryIconsWithoutRendering()
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
        assertEquals(9, Set.copyOf(nodeIcons.values()).size(),
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
