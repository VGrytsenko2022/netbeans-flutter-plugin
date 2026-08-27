package dev.flutter.netbeans.plugin.designer.palette;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.plugin.designer.icons.FlutterWidgetIconRegistry;
import java.awt.datatransfer.DataFlavor;
import java.lang.reflect.Field;
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
    private static final Set<String> CORE_V1 = Set.of(
            "flutter.material.Scaffold",
            "flutter.widgets.Column",
            "flutter.widgets.Row",
            "flutter.widgets.Text",
            "flutter.widgets.Padding",
            "flutter.widgets.Center");

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
                definition -> CORE_V1.contains(definition.typeId().value()));
        Node[] categories = root(controller).getChildren().getNodes(true);

        assertEquals(
                List.of("flutter.material", "flutter.layout", "flutter.basic"),
                Arrays.stream(categories).map(Node::getName).toList());
        assertEquals(List.of("Scaffold"), itemLabels(categories[0]));
        assertEquals(List.of("Column", "Row", "Padding", "Center"), itemLabels(categories[1]));
        assertEquals(List.of("Text"), itemLabels(categories[2]));

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
    void sixCoreItemNodesDeclareTheirMatchingUniqueRegistryIconsWithoutRendering()
            throws ReflectiveOperationException {
        PaletteController controller = FlutterDesignerPalette.create(
                CATALOG,
                definition -> CORE_V1.contains(definition.typeId().value()));
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

        assertEquals(CORE_V1, nodeIcons.keySet());
        assertEquals(6, Set.copyOf(nodeIcons.values()).size(),
                "palette items must not share a generic widget icon");
    }

    private static Node root(PaletteController controller) {
        return controller.getRoot().lookup(Node.class);
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
