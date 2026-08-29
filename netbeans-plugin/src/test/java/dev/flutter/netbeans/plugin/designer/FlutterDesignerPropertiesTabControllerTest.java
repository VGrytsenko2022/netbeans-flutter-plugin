package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetPropertiesNode;
import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.netbeans.swing.tabcontrol.TabData;
import org.netbeans.swing.tabcontrol.TabbedContainer;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.windows.TopComponent;

class FlutterDesignerPropertiesTabControllerTest {
    private static final StableId FIRST = new StableId(
            UUID.fromString("11111111-1111-4111-8111-111111111111"));
    private static final StableId SECOND = new StableId(
            UUID.fromString("22222222-2222-4222-8222-222222222222"));
    private static final StableId THIRD = new StableId(
            UUID.fromString("33333333-3333-4333-8333-333333333333"));

    @Test
    void differentWidgetSelectsGeneralAfterItsExactSetsArePublished()
            throws Exception {
        TestContext context = createOnEdt(() -> {
            Fixture fixture = fixture(FIRST);
            FlutterDesignerPropertiesTabController controller =
                    new FlutterDesignerPropertiesTabController(
                            () -> fixture.propertiesWindow());
            controller.selectionChanged(FIRST);
            return new TestContext(fixture, controller);
        });
        flushEdt();
        onEdt(() -> {
            Fixture fixture = context.fixture();
            FlutterDesignerPropertiesTabController controller =
                    context.controller();
            assertEquals(0, fixture.tabs().getSelectionModel().getSelectedIndex());
            fixture.tabs().getSelectionModel().setSelectedIndex(1);

            controller.selectionChanged(SECOND);
            assertEquals(1, fixture.tabs().getSelectionModel().getSelectedIndex(),
                    "old General sets must not satisfy a new widget request");
            assertTrue(controller.observingForTests());

            publish(fixture.tabs(), SECOND);
            fixture.tabs().getSelectionModel().setSelectedIndex(1);
        });
        flushEdt();
        onEdt(() -> {
            Fixture fixture = context.fixture();
            FlutterDesignerPropertiesTabController controller =
                    context.controller();
            assertEquals(0, fixture.tabs().getSelectionModel().getSelectedIndex());
            assertEquals(SECOND, controller.selectedWidgetIdForTests());
            assertFalse(controller.observingForTests(),
                    "the asynchronous listener must be temporary");
        });
    }

    @Test
    void sameWidgetPreservesTheUsersSlotsSelection() throws Exception {
        TestContext context = createOnEdt(() -> {
            Fixture fixture = fixture(FIRST);
            FlutterDesignerPropertiesTabController controller =
                    new FlutterDesignerPropertiesTabController(
                            () -> fixture.propertiesWindow());
            controller.selectionChanged(FIRST);
            return new TestContext(fixture, controller);
        });
        flushEdt();
        onEdt(() -> {
            Fixture fixture = context.fixture();
            fixture.tabs().getSelectionModel().setSelectedIndex(1);
            context.controller().selectionChanged(FIRST);
        });
        flushEdt();
        onEdt(() -> {
            Fixture fixture = context.fixture();

            assertEquals(1, fixture.tabs().getSelectionModel().getSelectedIndex());
            assertFalse(context.controller().observingForTests());
        });
    }

    @Test
    void stalePublicationCannotMoveANewerWidgetSelection() throws Exception {
        TestContext context = createOnEdt(() -> {
            Fixture fixture = fixture(FIRST);
            FlutterDesignerPropertiesTabController controller =
                    new FlutterDesignerPropertiesTabController(
                            () -> fixture.propertiesWindow());
            controller.selectionChanged(FIRST);
            return new TestContext(fixture, controller);
        });
        flushEdt();
        onEdt(() -> {
            Fixture fixture = context.fixture();
            FlutterDesignerPropertiesTabController controller =
                    context.controller();
            fixture.tabs().getSelectionModel().setSelectedIndex(1);
            controller.selectionChanged(SECOND);
            controller.selectionChanged(THIRD);

            publish(fixture.tabs(), SECOND);
            fixture.tabs().getSelectionModel().setSelectedIndex(1);
        });
        flushEdt();
        onEdt(() -> {
            Fixture fixture = context.fixture();
            FlutterDesignerPropertiesTabController controller =
                    context.controller();
            assertEquals(1, fixture.tabs().getSelectionModel().getSelectedIndex(),
                    "a stale model must not satisfy the current desired StableId");
            assertTrue(controller.observingForTests());

            publish(fixture.tabs(), THIRD);
            fixture.tabs().getSelectionModel().setSelectedIndex(1);
        });
        flushEdt();
        onEdt(() -> {
            Fixture fixture = context.fixture();
            FlutterDesignerPropertiesTabController controller =
                    context.controller();
            assertEquals(0, fixture.tabs().getSelectionModel().getSelectedIndex());
            assertEquals(THIRD, controller.selectedWidgetIdForTests());
            assertFalse(controller.observingForTests());
        });
    }

    @Test
    void clearDetachesBeforeALateModelPublication() throws Exception {
        TestContext context = createOnEdt(() -> {
            Fixture fixture = fixture(FIRST);
            FlutterDesignerPropertiesTabController controller =
                    new FlutterDesignerPropertiesTabController(
                            () -> fixture.propertiesWindow());
            controller.selectionChanged(FIRST);
            return new TestContext(fixture, controller);
        });
        flushEdt();
        onEdt(() -> {
            Fixture fixture = context.fixture();
            FlutterDesignerPropertiesTabController controller =
                    context.controller();
            fixture.tabs().getSelectionModel().setSelectedIndex(1);
            controller.selectionChanged(SECOND);
            assertTrue(controller.observingForTests());

            controller.selectionChanged(null);
            publish(fixture.tabs(), SECOND);
            fixture.tabs().getSelectionModel().setSelectedIndex(1);
        });
        flushEdt();
        onEdt(() -> {
            Fixture fixture = context.fixture();
            FlutterDesignerPropertiesTabController controller =
                    context.controller();

            assertEquals(1, fixture.tabs().getSelectionModel().getSelectedIndex());
            assertEquals(SECOND, controller.selectedWidgetIdForTests(),
                    "a transient empty Explorer selection must preserve identity");
            assertFalse(controller.observingForTests());

            controller.reset();
            assertNull(controller.selectedWidgetIdForTests());
        });
    }

    @Test
    void observesATabContainerAddedAfterTheSelectionEvent() throws Exception {
        AddedContainerContext context = createOnEdt(() -> {
            TopComponent properties = new TopComponent();
            properties.setLayout(new BorderLayout());
            FlutterDesignerPropertiesTabController controller =
                    new FlutterDesignerPropertiesTabController(() -> properties);

            controller.selectionChanged(SECOND);
            assertTrue(controller.observingForTests());

            TabbedContainer tabs = tabs(SECOND);
            tabs.getSelectionModel().setSelectedIndex(1);
            properties.add(tabs, BorderLayout.CENTER);
            tabs.getSelectionModel().setSelectedIndex(1);
            return new AddedContainerContext(controller, tabs);
        });
        flushEdt();
        onEdt(() -> {
            TabbedContainer tabs = context.tabs();
            assertEquals(0, tabs.getSelectionModel().getSelectedIndex());
            assertFalse(context.controller().observingForTests());
        });
    }

    private static Fixture fixture(StableId widgetId) {
        TopComponent properties = new TopComponent();
        properties.setLayout(new BorderLayout());
        TabbedContainer tabs = tabs(widgetId);
        tabs.getSelectionModel().setSelectedIndex(1);
        properties.add(tabs, BorderLayout.CENTER);
        return new Fixture(properties, tabs);
    }

    private static TabbedContainer tabs(StableId widgetId) {
        TabbedContainer tabs = new TabbedContainer(TabbedContainer.TYPE_TOOLBAR);
        publish(tabs, widgetId);
        return tabs;
    }

    private static void publish(TabbedContainer tabs, StableId widgetId) {
        tabs.getModel().setTabs(new TabData[]{
            new TabData(
                    generalSets(widgetId),
                    null,
                    FlutterWidgetPropertiesNode.GENERAL_TAB_NAME,
                    null),
            new TabData(
                    new Node.PropertySet[]{propertySet("slots")},
                    null,
                    FlutterWidgetPropertiesNode.SLOTS_TAB_NAME,
                    null)
        });
    }

    private static Node.PropertySet[] generalSets(StableId widgetId) {
        Sheet.Set identity = propertySet(
                FlutterWidgetPropertiesNode.IDENTITY_SET_NAME);
        identity.put(new PropertySupport.ReadOnly<String>(
                FlutterWidgetPropertiesNode.STABLE_ID_PROPERTY_NAME,
                String.class,
                "Stable ID",
                "Exact widget identity") {
            @Override
            public String getValue() {
                return widgetId.toString();
            }
        });
        return new Node.PropertySet[]{identity, propertySet(Sheet.PROPERTIES)};
    }

    private static Sheet.Set propertySet(String name) {
        Sheet.Set set = new Sheet.Set();
        set.setName(name);
        set.setDisplayName(name);
        return set;
    }

    private static void onEdt(Runnable task)
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(task);
    }

    private static void flushEdt()
            throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
        });
    }

    private static <T> T createOnEdt(EdtSupplier<T> supplier)
            throws InterruptedException, InvocationTargetException {
        AtomicReference<T> result = new AtomicReference<>();
        onEdt(() -> result.set(supplier.get()));
        return result.get();
    }

    @FunctionalInterface
    private interface EdtSupplier<T> {
        T get();
    }

    private record Fixture(
            TopComponent propertiesWindow,
            TabbedContainer tabs) {
    }

    private record TestContext(
            Fixture fixture,
            FlutterDesignerPropertiesTabController controller) {
    }

    private record AddedContainerContext(
            FlutterDesignerPropertiesTabController controller,
            TabbedContainer tabs) {
    }
}
