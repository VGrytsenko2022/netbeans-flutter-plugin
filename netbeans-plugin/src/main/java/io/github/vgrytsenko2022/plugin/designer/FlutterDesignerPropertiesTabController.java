package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.plugin.designer.properties.FlutterWidgetPropertiesNode;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.swing.tabcontrol.TabData;
import org.netbeans.swing.tabcontrol.TabDataModel;
import org.netbeans.swing.tabcontrol.TabbedContainer;
import org.openide.nodes.Node;
import org.openide.windows.WindowManager;

/**
 * Selects the General Properties tab when the selected designer widget changes.
 *
 * <p>The standard NetBeans PropertySheet deliberately carries its last selected
 * property group to the next Node. This controller narrows the designer
 * behavior without taking ownership of the Properties window: it waits until
 * the standard tab container publishes property sets for the exact requested
 * widget, then selects General once. Re-publishing an immutable Properties Node
 * for the same widget therefore preserves the user's current tab.</p>
 */
final class FlutterDesignerPropertiesTabController {
    private final Supplier<? extends Container> propertiesWindowSupplier;
    private final Set<Container> observedContainers = Collections.newSetFromMap(
            new IdentityHashMap<>());
    private final ContainerListener containerListener = new ContainerListener() {
        @Override
        public void componentAdded(ContainerEvent event) {
            propertiesStructureChanged();
        }

        @Override
        public void componentRemoved(ContainerEvent event) {
            propertiesStructureChanged();
        }
    };
    private final ChangeListener tabModelListener = this::tabModelChanged;

    private StableId selectedWidgetId;
    private StableId pendingWidgetId;
    private Container observedRoot;
    private TabDataModel observedTabModel;
    private boolean refreshingObservation;
    private long selectionEpoch;
    private long scheduledEpoch = -1;

    FlutterDesignerPropertiesTabController() {
        this(() -> WindowManager.getDefault().findTopComponent(
                FlutterDesignerAuxiliaryWindows.PROPERTIES_ID));
    }

    FlutterDesignerPropertiesTabController(
            Supplier<? extends Container> propertiesWindowSupplier) {
        this.propertiesWindowSupplier = Objects.requireNonNull(
                propertiesWindowSupplier, "propertiesWindowSupplier");
    }

    /**
     * Publishes the exact selected widget identity, or {@code null} when the
     * designer selection is empty.
     */
    void selectionChanged(StableId widgetId) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> selectionChanged(widgetId));
            return;
        }
        if (widgetId == null) {
            selectionEpoch++;
            pendingWidgetId = null;
            detachObservation();
            return;
        }
        if (widgetId.equals(selectedWidgetId)) {
            return;
        }

        selectedWidgetId = widgetId;
        selectionEpoch++;
        pendingWidgetId = widgetId;
        detachObservation();
        Container root = propertiesWindowSupplier.get();
        if (root == null) {
            return;
        }
        observedRoot = root;
        refreshObservation();
    }

    /** Clears both pending UI work and the remembered widget identity. */
    void reset() {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this::reset);
            return;
        }
        selectionEpoch++;
        selectedWidgetId = null;
        pendingWidgetId = null;
        detachObservation();
    }

    private void propertiesStructureChanged() {
        if (pendingWidgetId == null || refreshingObservation) {
            return;
        }
        refreshObservation();
    }

    private void tabModelChanged(ChangeEvent event) {
        if (event.getSource() != observedTabModel || pendingWidgetId == null) {
            return;
        }
        scheduleGeneralIfPublished();
    }

    private void refreshObservation() {
        if (refreshingObservation || observedRoot == null
                || pendingWidgetId == null) {
            return;
        }
        refreshingObservation = true;
        try {
            detachComponentListeners();
            observeTree(observedRoot);
            setObservedTabModel(findPropertiesTabs(observedRoot));
            scheduleGeneralIfPublished();
        } finally {
            refreshingObservation = false;
        }
    }

    private void observeTree(Container container) {
        if (!observedContainers.add(container)) {
            return;
        }
        container.addContainerListener(containerListener);
        for (Component child : container.getComponents()) {
            if (child instanceof Container childContainer) {
                observeTree(childContainer);
            }
        }
    }

    private static TabbedContainer findPropertiesTabs(Container container) {
        if (container instanceof TabbedContainer tabs
                && hasDesignerTabNames(tabs.getModel())) {
            return tabs;
        }
        for (Component child : container.getComponents()) {
            if (child instanceof Container childContainer) {
                TabbedContainer found = findPropertiesTabs(childContainer);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean hasDesignerTabNames(TabDataModel model) {
        boolean general = false;
        boolean slots = false;
        for (TabData tab : model.getTabs()) {
            general |= FlutterWidgetPropertiesNode.GENERAL_TAB_NAME.equals(
                    tab.getText());
            slots |= FlutterWidgetPropertiesNode.SLOTS_TAB_NAME.equals(
                    tab.getText());
        }
        return general && slots;
    }

    private void setObservedTabModel(TabbedContainer tabs) {
        TabDataModel next = tabs == null ? null : tabs.getModel();
        if (next == observedTabModel) {
            return;
        }
        if (observedTabModel != null) {
            observedTabModel.removeChangeListener(tabModelListener);
        }
        observedTabModel = next;
        if (observedTabModel != null) {
            observedTabModel.addChangeListener(tabModelListener);
        }
    }

    private void scheduleGeneralIfPublished() {
        StableId expected = pendingWidgetId;
        TabDataModel model = observedTabModel;
        if (expected == null || model == null) {
            return;
        }
        int generalIndex = generalTabIndex(model, expected);
        if (generalIndex < 0) {
            return;
        }
        long expectedEpoch = selectionEpoch;
        if (scheduledEpoch == expectedEpoch) {
            return;
        }
        scheduledEpoch = expectedEpoch;
        EventQueue.invokeLater(() -> selectScheduledGeneral(
                expectedEpoch, expected, model));
    }

    private void selectScheduledGeneral(
            long expectedEpoch,
            StableId expected,
            TabDataModel model) {
        if (scheduledEpoch == expectedEpoch) {
            scheduledEpoch = -1;
        }
        if (expectedEpoch != selectionEpoch
                || !expected.equals(pendingWidgetId)
                || model != observedTabModel) {
            return;
        }
        int generalIndex = generalTabIndex(model, expected);
        TabbedContainer tabs = findPropertiesTabs(observedRoot);
        if (generalIndex < 0 || tabs == null || tabs.getModel() != model) {
            return;
        }
        tabs.getSelectionModel().setSelectedIndex(generalIndex);
        pendingWidgetId = null;
        detachObservation();
    }

    private static int generalTabIndex(TabDataModel model, StableId expected) {
        for (int index = 0; index < model.size(); index++) {
            TabData tab = model.getTab(index);
            if (FlutterWidgetPropertiesNode.GENERAL_TAB_NAME.equals(tab.getText())
                    && belongsToWidget(tab.getUserObject(), expected)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean belongsToWidget(Object userObject, StableId expected) {
        if (!(userObject instanceof Node.PropertySet[] sets)) {
            return false;
        }
        for (Node.PropertySet set : sets) {
            if (!FlutterWidgetPropertiesNode.IDENTITY_SET_NAME.equals(
                    set.getName())) {
                continue;
            }
            for (Node.Property<?> property : set.getProperties()) {
                if (!FlutterWidgetPropertiesNode.STABLE_ID_PROPERTY_NAME.equals(
                        property.getName())) {
                    continue;
                }
                try {
                    return expected.toString().equals(property.getValue());
                } catch (IllegalAccessException | InvocationTargetException failure) {
                    return false;
                }
            }
        }
        return false;
    }

    private void detachObservation() {
        setObservedTabModel(null);
        detachComponentListeners();
        observedRoot = null;
    }

    private void detachComponentListeners() {
        for (Container container : observedContainers) {
            container.removeContainerListener(containerListener);
        }
        observedContainers.clear();
    }

    StableId selectedWidgetIdForTests() {
        return selectedWidgetId;
    }

    boolean observingForTests() {
        return observedRoot != null || observedTabModel != null
                || !observedContainers.isEmpty();
    }
}
