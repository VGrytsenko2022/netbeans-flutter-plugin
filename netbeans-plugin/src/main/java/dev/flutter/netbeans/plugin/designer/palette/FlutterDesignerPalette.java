package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PaletteMetadata;
import dev.flutter.netbeans.designer.catalog.SingleChildScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetPlacementRules;
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
        return create(
                catalog,
                includeDefinition,
                registry,
                enabled,
                draggableDefinition,
                (token, definition) -> true);
    }

    /**
     * Creates a tokenized Palette and reports each admitted source token to
     * the owning view before native dragging begins.
     */
    public static PaletteController create(
            WidgetCatalog catalog,
            Predicate<? super WidgetDefinition> includeDefinition,
            FlutterDesignerPaletteDragRegistry registry,
            BooleanSupplier enabled,
            Predicate<? super WidgetDefinition> draggableDefinition,
            PaletteDragSourceListener sourceListener) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(enabled, "enabled");
        Objects.requireNonNull(draggableDefinition, "draggableDefinition");
        Objects.requireNonNull(sourceListener, "sourceListener");
        return create(
                catalog,
                includeDefinition,
                new TokenDragAndDropHandler(
                        registry,
                        enabled,
                        draggableDefinition,
                        sourceListener));
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
            case "flutter.scrolling" -> message("Category.Scrolling");
            case "flutter.basic" -> message("Category.Basic");
            case "flutter.accessibility" -> message("Category.Accessibility");
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
            if ("flutter.widgets.SafeArea".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.SafeArea.Name"));
                setShortDescription(message("Widget.SafeArea.Description"));
            } else if ("flutter.widgets.Directionality".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.Directionality.Name"));
                setShortDescription(message("Widget.Directionality.Description"));
            } else if ("flutter.widgets.DecoratedBox".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.DecoratedBox.Name"));
                setShortDescription(message("Widget.DecoratedBox.Description"));
            } else if ("flutter.widgets.ExcludeSemantics".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.ExcludeSemantics.Name"));
                setShortDescription(message("Widget.ExcludeSemantics.Description"));
            } else if ("flutter.widgets.IndexedStack".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.IndexedStack.Name"));
                setShortDescription(message("Widget.IndexedStack.Description"));
            } else if ("flutter.widgets.ClipRect".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.ClipRect.Name"));
                setShortDescription(message("Widget.ClipRect.Description"));
            } else if ("flutter.widgets.ClipOval".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.ClipOval.Name"));
                setShortDescription(message("Widget.ClipOval.Description"));
            } else if ("flutter.widgets.ClipRRect".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.ClipRRect.Name"));
                setShortDescription(message("Widget.ClipRRect.Description"));
            } else if ("flutter.widgets.ClipPath".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.ClipPath.Name"));
                setShortDescription(message("Widget.ClipPath.Description"));
            } else if ("flutter.widgets.IgnorePointer".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.IgnorePointer.Name"));
                setShortDescription(message("Widget.IgnorePointer.Description"));
            } else if ("flutter.widgets.AbsorbPointer".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.AbsorbPointer.Name"));
                setShortDescription(message("Widget.AbsorbPointer.Description"));
            } else if ("flutter.widgets.BlockSemantics".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.BlockSemantics.Name"));
                setShortDescription(message("Widget.BlockSemantics.Description"));
            } else if ("flutter.widgets.ExcludeFocus".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.ExcludeFocus.Name"));
                setShortDescription(message("Widget.ExcludeFocus.Description"));
            } else if ("flutter.widgets.ExcludeFocusTraversal".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.ExcludeFocusTraversal.Name"));
                setShortDescription(message("Widget.ExcludeFocusTraversal.Description"));
            } else if ("flutter.widgets.Visibility".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.Visibility.Name"));
                setShortDescription(message("Widget.Visibility.Description"));
            } else if ("flutter.widgets.TickerMode".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.TickerMode.Name"));
                setShortDescription(message("Widget.TickerMode.Description"));
            } else if ("flutter.widgets.DefaultTextHeightBehavior".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.DefaultTextHeightBehavior.Name"));
                setShortDescription(message("Widget.DefaultTextHeightBehavior.Description"));
            } else if ("flutter.widgets.DefaultSelectionStyle".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.DefaultSelectionStyle.Name"));
                setShortDescription(message("Widget.DefaultSelectionStyle.Description"));
            } else if ("flutter.material.Divider".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.Divider.Name"));
                setShortDescription(message("Widget.Divider.Description"));
            } else if ("flutter.material.VerticalDivider".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.VerticalDivider.Name"));
                setShortDescription(message("Widget.VerticalDivider.Description"));
            } else if ("flutter.material.Badge".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.Badge.Name"));
                setShortDescription(message("Widget.Badge.Description"));
            } else if ("flutter.material.CircleAvatar".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.CircleAvatar.Name"));
                setShortDescription(message("Widget.CircleAvatar.Description"));
            } else if ("flutter.material.LinearProgressIndicator".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.LinearProgressIndicator.Name"));
                setShortDescription(message("Widget.LinearProgressIndicator.Description"));
            } else if ("flutter.material.CircularProgressIndicator".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.CircularProgressIndicator.Name"));
                setShortDescription(message("Widget.CircularProgressIndicator.Description"));
            } else if ("flutter.material.TextButton".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.TextButton.Name"));
                setShortDescription(message("Widget.TextButton.Description"));
            } else if ("flutter.material.OutlinedButton".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.OutlinedButton.Name"));
                setShortDescription(message("Widget.OutlinedButton.Description"));
            } else if ("flutter.material.FilledButton".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.FilledButton.Name"));
                setShortDescription(message("Widget.FilledButton.Description"));
            } else if ("flutter.material.IconButton".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.IconButton.Name"));
                setShortDescription(message("Widget.IconButton.Description"));
            } else if ("flutter.material.FloatingActionButton".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.FloatingActionButton.Name"));
                setShortDescription(message("Widget.FloatingActionButton.Description"));
            } else if ("flutter.material.RefreshIndicator".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.RefreshIndicator.Name"));
                setShortDescription(message("Widget.RefreshIndicator.Description"));
            } else if ("flutter.material.RefreshProgressIndicator".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.RefreshProgressIndicator.Name"));
                setShortDescription(message("Widget.RefreshProgressIndicator.Description"));
            } else if ("flutter.material.Card".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.Card.Name"));
                setShortDescription(message("Widget.Card.Description"));
            } else if ("flutter.material.CheckboxListTile".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.CheckboxListTile.Name"));
                setShortDescription(message("Widget.CheckboxListTile.Description"));
            } else if ("flutter.material.ListTile".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.ListTile.Name"));
                setShortDescription(message("Widget.ListTile.Description"));
            } else if ("flutter.widgets.RadioGroup".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.RadioGroup.Name"));
                setShortDescription(message("Widget.RadioGroup.Description"));
            } else if ("flutter.material.Radio".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.Radio.Name"));
                setShortDescription(message("Widget.Radio.Description"));
            } else if ("flutter.material.RangeSlider".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.RangeSlider.Name"));
                setShortDescription(message("Widget.RangeSlider.Description"));
            } else if ("flutter.material.Slider".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.Slider.Name"));
                setShortDescription(message("Widget.Slider.Description"));
            } else if ("flutter.material.Switch".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.Switch.Name"));
                setShortDescription(message("Widget.Switch.Description"));
            } else if ("flutter.material.Checkbox".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.Checkbox.Name"));
                setShortDescription(message("Widget.Checkbox.Description"));
            } else if ("flutter.widgets.IconTheme".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.IconTheme.Name"));
                setShortDescription(message("Widget.IconTheme.Description"));
            } else if ("flutter.widgets.IndexedSemantics".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.IndexedSemantics.Name"));
                setShortDescription(message("Widget.IndexedSemantics.Description"));
            } else if ("flutter.widgets.MergeSemantics".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.MergeSemantics.Name"));
                setShortDescription(message("Widget.MergeSemantics.Description"));
            } else if ("flutter.widgets.RepaintBoundary".equals(definition.typeId().value())) {
                setDisplayName(message("Widget.RepaintBoundary.Name"));
                setShortDescription(message("Widget.RepaintBoundary.Description"));
            } else if ("flutter.widgets.PhysicalShape".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.PhysicalShape.Name"));
                setShortDescription(message("Widget.PhysicalShape.Description"));
            } else if ("flutter.widgets.PhysicalModel".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.PhysicalModel.Name"));
                setShortDescription(message("Widget.PhysicalModel.Description"));
            } else if ("flutter.widgets.ClipRSuperellipse".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.ClipRSuperellipse.Name"));
                setShortDescription(message("Widget.ClipRSuperellipse.Description"));
            } else if (WidgetPlacementRules.creationMode(definition)
                    == WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD) {
                String wrapperName = definition.palette().displayName();
                setShortDescription(
                        "Wrap an existing direct child of Row.children or Column.children "
                        + "with " + wrapperName + ". Drop " + wrapperName
                        + " on that child; terminal insertion and empty placeholders are "
                        + "unavailable because " + wrapperName + ".child is required.");
            } else if (WidgetPlacementRules.SPACER_TYPE.equals(
                    definition.typeId().value())) {
                setShortDescription(
                        "Insert an empty flexible gap directly into Row.children or "
                        + "Column.children. Spacer has no child; omitted flex preserves "
                        + "Flutter's positive default of 1.");
            } else if (FlutterImageWidgetCreationValues.IMAGE_TYPE.equals(
                    definition.typeId())) {
                setShortDescription(
                        "Display a declared Flutter image asset. Creation selects the "
                        + "deterministic first sorted asset and sets required Image.image; "
                        + "when no safe declared asset is available, creation uses an "
                        + "editable placeholder that can be replaced in Image properties.");
            } else if (FlutterImageWidgetCreationValues.IMAGE_ICON_TYPE.equals(definition.typeId())) {
                setDisplayName(message("Widget.ImageIcon.Name"));
                setShortDescription(message("Widget.ImageIcon.Description"));
            } else if ("flutter.material.TextField".equals(
                    definition.typeId().value())) {
                setShortDescription(
                        "Create a Material TextField leaf immediately, without a creation "
                        + "dialog or stored constructor defaults. Runtime typed text, "
                        + "selection, controller state, and focus state are not stored "
                        + "by Designer.");
            } else if ("flutter.widgets.ListView".equals(
                    definition.typeId().value())) {
                setShortDescription(
                        "Create an ordered static ListView. Controller-owned state, "
                        + "builders, prototypeItem, and deprecated cacheExtent are outside "
                        + "this Designer slice.");
            } else if (GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.equals(
                    definition.typeId())) {
                setDisplayName(message("Widget.GridViewCount.Name"));
                setShortDescription(message("Widget.GridViewCount.Description"));
            } else if (SingleChildScrollViewWidgetPropertySchema
                    .SINGLE_CHILD_SCROLL_VIEW_TYPE.equals(definition.typeId())) {
                setDisplayName(message("Widget.SingleChildScrollView.Name"));
                setShortDescription(message("Widget.SingleChildScrollView.Description"));
            } else if ("flutter.widgets.ConstrainedBox".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.ConstrainedBox.Description"));
            } else if ("flutter.widgets.UnconstrainedBox".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.UnconstrainedBox.Description"));
            } else if ("flutter.widgets.LimitedBox".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.LimitedBox.Description"));
            } else if ("flutter.widgets.OverflowBox".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.OverflowBox.Description"));
            } else if ("flutter.widgets.Baseline".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.Baseline.Description"));
            } else if ("flutter.widgets.IntrinsicHeight".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.IntrinsicHeight.Description"));
            } else if ("flutter.widgets.IntrinsicWidth".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.IntrinsicWidth.Description"));
            } else if ("flutter.widgets.Offstage".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.Offstage.Description"));
            } else if ("flutter.widgets.SizedOverflowBox".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.SizedOverflowBox.Description"));
            } else if ("flutter.widgets.Transform".equals(
                    definition.typeId().value())) {
                setShortDescription(message("Widget.Transform.Description"));
            } else if ("flutter.widgets.RotatedBox".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.RotatedBox.Name"));
                setShortDescription(message("Widget.RotatedBox.Description"));
            } else if ("flutter.widgets.ListBody".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.ListBody.Name"));
                setShortDescription(message("Widget.ListBody.Description"));
            } else if ("flutter.widgets.OverflowBar".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.OverflowBar.Name"));
                setShortDescription(message("Widget.OverflowBar.Description"));
            } else if ("flutter.widgets.ColoredBox".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.ColoredBox.Name"));
                setShortDescription(message("Widget.ColoredBox.Description"));
            } else if ("flutter.widgets.Placeholder".equals(
                    definition.typeId().value())) {
                setDisplayName(message("Widget.Placeholder.Name"));
                setShortDescription(message("Widget.Placeholder.Description"));
            }
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
        private final PaletteDragSourceListener sourceListener;

        private TokenDragAndDropHandler(
                FlutterDesignerPaletteDragRegistry registry,
                BooleanSupplier enabled,
                Predicate<? super WidgetDefinition> draggableDefinition,
                PaletteDragSourceListener sourceListener) {
            this.registry = registry;
            this.enabled = enabled;
            this.draggableDefinition = draggableDefinition;
            this.sourceListener = sourceListener;
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
            registry.issueReplacingOutstanding(definition.typeId()).ifPresent(token -> {
                try {
                    if (!sourceListener.authorize(token, definition)) {
                        registry.revoke(token);
                        return;
                    }
                    transferable.put(new ExTransferable.Single(
                            DataFlavor.stringFlavor) {
                        @Override
                        protected Object getData() {
                            return token;
                        }
                    });
                } catch (RuntimeException | LinkageError failure) {
                    registry.revoke(token);
                }
            });
        }
    }

    /** Fail-closed authorization edge for one outgoing Palette source. */
    @FunctionalInterface
    public interface PaletteDragSourceListener {
        boolean authorize(String token, WidgetDefinition definition);
    }
}
