package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.PaletteMetadata;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.plugin.designer.icons.FlutterWidgetIconRegistry;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import javax.swing.Action;
import org.netbeans.spi.palette.DragAndDropHandler;
import org.netbeans.spi.palette.PaletteActions;
import org.netbeans.spi.palette.PaletteController;
import org.netbeans.spi.palette.PaletteFactory;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.NewType;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;

/**
 * Builds a context-local, read-only NetBeans palette from one immutable widget
 * catalog snapshot.
 *
 * <p>The supplied predicate is evaluated while the palette is built and is not
 * retained. Consequently each active designer context owns an independent
 * {@link PaletteController}; the factory has no mutable global palette state.</p>
 */
public final class FlutterDesignerPalette {
    private static final Action[] NO_ACTIONS = new Action[0];
    private static final NewType[] NO_NEW_TYPES = new NewType[0];
    private static final PaletteActions READ_ONLY_ACTIONS = new ReadOnlyPaletteActions();
    private static final DragAndDropHandler READ_ONLY_DND = new ReadOnlyDragAndDropHandler();

    private FlutterDesignerPalette() {
    }

    /**
     * Creates a new controller whose categories and items preserve the exact
     * deterministic order of {@link WidgetCatalog#paletteDefinitions()}.
     * Empty categories are omitted after filtering.
     */
    public static PaletteController create(
            WidgetCatalog catalog,
            Predicate<? super WidgetDefinition> includeDefinition) {
        return create(catalog, includeDefinition, READ_ONLY_DND);
    }

    /**
     * Creates a palette whose admitted outgoing drags carry one opaque,
     * short-lived token in {@link DataFlavor#stringFlavor}.
     *
     * <p>The token contains no widget definition or mutation authority. Its
     * authoritative type can be resolved only once through {@code registry}.
     * Palette-internal imports and reordering remain disabled.</p>
     */
    public static PaletteController create(
            WidgetCatalog catalog,
            Predicate<? super WidgetDefinition> includeDefinition,
            FlutterDesignerPaletteDragRegistry registry,
            BooleanSupplier enabled,
            Predicate<? super WidgetDefinition> draggableDefinition) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(enabled, "enabled");
        Objects.requireNonNull(draggableDefinition, "draggableDefinition");
        return create(
                catalog,
                includeDefinition,
                new TokenDragAndDropHandler(
                        registry, enabled, draggableDefinition));
    }

    private static PaletteController create(
            WidgetCatalog catalog,
            Predicate<? super WidgetDefinition> includeDefinition,
            DragAndDropHandler dragAndDropHandler) {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(includeDefinition, "includeDefinition");
        Objects.requireNonNull(dragAndDropHandler, "dragAndDropHandler");

        Map<String, CategoryBuilder> categories = new LinkedHashMap<>();
        for (WidgetDefinition definition : catalog.paletteDefinitions()) {
            if (!includeDefinition.test(definition)) {
                continue;
            }
            PaletteMetadata palette = definition.palette();
            CategoryBuilder category = categories.computeIfAbsent(
                    palette.categoryId(),
                    ignored -> new CategoryBuilder(
                            new FlutterDesignerPaletteCategory(
                                    palette.categoryId(),
                                    palette.categoryOrder(),
                                    categoryDisplayName(palette.categoryId()))));
            category.items().add(new PaletteItemNode(definition));
        }

        List<Node> categoryNodes = categories.values().stream()
                .map(CategoryBuilder::toNode)
                .toList();
        Node root = new PaletteRootNode(categoryNodes);
        return PaletteFactory.createPalette(
                root, READ_ONLY_ACTIONS, null, dragAndDropHandler);
    }

    private static String categoryDisplayName(String categoryId) {
        return switch (categoryId) {
            case "flutter.material" -> message("Category.Material");
            case "flutter.layout" -> message("Category.Layout");
            case "flutter.basic" -> message("Category.Basic");
            default -> categoryId;
        };
    }

    private static String message(String key) {
        return NbBundle.getMessage(FlutterDesignerPalette.class, key);
    }

    private static Children childrenOf(List<? extends Node> nodes) {
        Children.Array children = new Children.Array();
        children.add(nodes.toArray(Node[]::new));
        return children;
    }

    private record CategoryBuilder(
            FlutterDesignerPaletteCategory category,
            List<Node> items) {

        private CategoryBuilder(FlutterDesignerPaletteCategory category) {
            this(category, new ArrayList<>());
        }

        private Node toNode() {
            return new PaletteCategoryNode(category, items);
        }
    }

    private abstract static class ReadOnlyNode extends AbstractNode {
        private ReadOnlyNode(Children children, Lookup lookup) {
            super(children, lookup);
            setValue(PaletteController.ATTR_IS_READONLY, Boolean.TRUE);
        }

        @Override
        public final boolean canRename() {
            return false;
        }

        @Override
        public final boolean canDestroy() {
            return false;
        }

        @Override
        public final boolean canCopy() {
            return false;
        }

        @Override
        public final boolean canCut() {
            return false;
        }

        @Override
        public final Action[] getActions(boolean context) {
            return NO_ACTIONS.clone();
        }

        @Override
        public final Action getPreferredAction() {
            return null;
        }

        @Override
        public final NewType[] getNewTypes() {
            return NO_NEW_TYPES.clone();
        }

        @Override
        public final PasteType getDropType(Transferable transferable, int action, int index) {
            return null;
        }
    }

    private static final class PaletteRootNode extends ReadOnlyNode {
        private PaletteRootNode(List<Node> categoryNodes) {
            super(childrenOf(categoryNodes), Lookup.EMPTY);
            setName("flutter-designer-widgets");
            setDisplayName(message("Palette.Root"));
            setValue(PaletteController.ATTR_SHOW_ITEM_NAMES, Boolean.TRUE);
        }
    }

    private static final class PaletteCategoryNode extends ReadOnlyNode {
        private PaletteCategoryNode(
                FlutterDesignerPaletteCategory category,
                List<Node> itemNodes) {
            super(childrenOf(itemNodes), Lookups.singleton(category));
            setName(category.categoryId());
            setDisplayName(category.displayName());
        }
    }

    private static final class PaletteItemNode extends ReadOnlyNode {
        private PaletteItemNode(WidgetDefinition definition) {
            this(FlutterDesignerPaletteItem.from(definition), definition);
        }

        private PaletteItemNode(
                FlutterDesignerPaletteItem item,
                WidgetDefinition definition) {
            super(Children.LEAF, Lookups.fixed(item, definition));
            setName(item.typeId().value());
            setDisplayName(item.displayName());
            FlutterWidgetIconRegistry.findIconPath(item.typeId())
                    .ifPresent(this::setIconBaseWithExtension);
        }
    }

    private static final class ReadOnlyPaletteActions extends PaletteActions {
        @Override
        public Action[] getImportActions() {
            return NO_ACTIONS.clone();
        }

        @Override
        public Action[] getCustomPaletteActions() {
            return NO_ACTIONS.clone();
        }

        @Override
        public Action[] getCustomCategoryActions(Lookup category) {
            return NO_ACTIONS.clone();
        }

        @Override
        public Action[] getCustomItemActions(Lookup item) {
            return NO_ACTIONS.clone();
        }

        @Override
        public Action getPreferredAction(Lookup item) {
            return null;
        }

        @Override
        public Action getRefreshAction() {
            return null;
        }

        @Override
        public Action getResetAction() {
            return null;
        }
    }

    private static class ReadOnlyDragAndDropHandler extends DragAndDropHandler {
        @Override
        public void customize(ExTransferable transferable, Lookup item) {
            // No Canvas mutation flavor is added in this read-only slice.
        }

        @Override
        public boolean canDrop(Lookup targetCategory, DataFlavor[] flavors, int action) {
            return false;
        }

        @Override
        public boolean doDrop(
                Lookup targetCategory,
                Transferable transferable,
                int action,
                int index) {
            return false;
        }

        @Override
        public boolean canReorderCategories(Lookup category) {
            return false;
        }

        @Override
        public boolean moveCategory(Lookup category, int index) {
            return false;
        }
    }

    private static final class TokenDragAndDropHandler
            extends ReadOnlyDragAndDropHandler {
        private final FlutterDesignerPaletteDragRegistry registry;
        private final BooleanSupplier enabled;
        private final Predicate<? super WidgetDefinition> draggableDefinition;

        private TokenDragAndDropHandler(
                FlutterDesignerPaletteDragRegistry registry,
                BooleanSupplier enabled,
                Predicate<? super WidgetDefinition> draggableDefinition) {
            this.registry = registry;
            this.enabled = enabled;
            this.draggableDefinition = draggableDefinition;
        }

        @Override
        public void customize(ExTransferable transferable, Lookup item) {
            Objects.requireNonNull(transferable, "transferable");
            Objects.requireNonNull(item, "item");
            if (!enabled.getAsBoolean()) {
                return;
            }
            FlutterDesignerPaletteItem paletteItem = item.lookup(
                    FlutterDesignerPaletteItem.class);
            WidgetDefinition definition = item.lookup(WidgetDefinition.class);
            if (paletteItem == null
                    || definition == null
                    || !paletteItem.typeId().equals(definition.typeId())
                    || !draggableDefinition.test(definition)) {
                return;
            }
            registry.issueReplacingOutstanding(definition.typeId()).ifPresent(token ->
                transferable.put(new ExTransferable.Single(
                        DataFlavor.stringFlavor) {
                    @Override
                    protected Object getData() {
                        return token;
                    }
                }));
        }
    }
}
