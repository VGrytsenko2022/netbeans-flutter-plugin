package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.Component;
import java.awt.Container;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlutterWidgetSlotPropertyEditorTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final SlotName BODY = new SlotName("body");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void addIsATransactionalDraftAndDispatchesOneExactIntentAfterOk()
            throws Exception {
        WidgetDefinition scaffoldDefinition = definition("flutter.material.Scaffold");
        WidgetNode scaffold = new WidgetNode(
                id("082e80e3-06ed-418f-9478-18d8fcb68333"),
                scaffoldDefinition.typeId(),
                Map.of(),
                Map.of(BODY, WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        DesignerDocument document = document(scaffold);
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document,
                CATALOG,
                List.of(type("flutter.widgets.Center"), type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                scaffold,
                scaffoldDefinition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> body = slotProperty(node, "body");
        PropertyEditor editor = body.getPropertyEditor();
        editor.setValue(body.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Body"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            assertNotNull(action);
            assertNotNull(addType);
            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Center");

            assertEquals("Empty", editor.getAsText(),
                    "dialog controls must keep an uncommitted local draft");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotCellValue staged = assertInstanceOf(
                    FlutterWidgetSlotCellValue.class, editor.getValue());
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    staged.mutation().orElseThrow());
            assertEquals(scaffold.id(), add.ownerId());
            assertEquals(BODY, add.slotName());
            assertEquals(type("flutter.widgets.Center"), add.widgetType());
            assertEquals(0, add.index());

            body.setValue(staged);
            body.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted slot dialog may consume its revision lease once");
            return null;
        });
    }

    @Test
    void listSlotOffersExactReorderPositionsAndDirectChildRemoval()
            throws Exception {
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetNode first = text(
                id("a5bf370b-4189-497a-b529-ed4464ae6203"), "first");
        WidgetNode second = text(
                id("18adca4f-9d75-4455-9ecf-da7964b0ba9f"), "second");
        WidgetNode third = text(
                id("554e32b4-bc66-42d4-946c-d847be359054"), "third");
        WidgetNode column = new WidgetNode(
                id("c1289524-b4f4-47db-9245-4dfb0cc044fa"),
                columnDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(first, second, third))),
                Extensions.empty());
        DesignerDocument document = document(column);
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document, CATALOG, List.of(type("flutter.widgets.Text")));
        FlutterWidgetSlotPropertyEditor editor = new FlutterWidgetSlotPropertyEditor(
                column,
                columnDefinition,
                columnDefinition.slot(CHILDREN).orElseThrow(),
                context);
        PropertyEnv environment = PropertyEnv.create(descriptor("Children"));
        editor.attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> moveSource = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.MOVE_SOURCE_NAME,
                    JComboBox.class);
            JComboBox<?> position = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.POSITION_NAME,
                    JComboBox.class);
            JList<?> current = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.CURRENT_LIST_NAME,
                    JList.class);

            assertEquals(3, current.getModel().getSize());
            assertTrue(labels(action).contains("Move existing widget here"));
            assertTrue(labels(action).contains("Remove selected widget"));
            selectLabel(action, "Move existing widget here");
            selectContains(moveSource, first.id().toString());
            assertEquals(2, position.getItemCount(),
                    "same-slot no-op and post-removal out-of-range positions are absent");
            assertTrue(labels(position).contains("Position 2 (index 1)"));
            assertTrue(labels(position).contains("Last (index 2)"));
            selectLabel(position, "Last (index 2)");
            environment.setState(PropertyEnv.STATE_VALID);

            FlutterWidgetSlotMutation.Move move = assertInstanceOf(
                    FlutterWidgetSlotMutation.Move.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(first.id(), move.sourceId());
            assertEquals(column.id(), move.ownerId());
            assertEquals(CHILDREN, move.slotName());
            assertEquals(2, move.postRemovalIndex());
            assertEquals("Children slot editor",
                    ((JComponent) custom).getAccessibleContext().getAccessibleName());
            return null;
        });
    }

    @Test
    void movingCurrentLastChildDoesNotMislabelMiddlePositionAsLast()
            throws Exception {
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetNode first = text(
                id("249df88b-8a5b-4710-b253-1f6f5aba8f50"), "first");
        WidgetNode second = text(
                id("f154f77f-95b2-433b-bd41-4c7b408d4279"), "second");
        WidgetNode third = text(
                id("263c7021-35f1-4698-bd03-6c0d7937f5dd"), "third");
        WidgetNode column = new WidgetNode(
                id("e3428420-d69a-4545-9f57-dff4adb5bad3"),
                columnDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(first, second, third))),
                Extensions.empty());
        FlutterWidgetSlotPropertyEditor editor = new FlutterWidgetSlotPropertyEditor(
                column,
                columnDefinition,
                columnDefinition.slot(CHILDREN).orElseThrow(),
                new FlutterWidgetSlotEditorContext(
                        document(column),
                        CATALOG,
                        List.of(type("flutter.widgets.Text"))));
        PropertyEnv environment = PropertyEnv.create(descriptor("Children"));
        editor.attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> moveSource = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.MOVE_SOURCE_NAME,
                    JComboBox.class);
            JComboBox<?> position = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.POSITION_NAME,
                    JComboBox.class);

            selectLabel(action, "Move existing widget here");
            selectContains(moveSource, third.id().toString());

            assertEquals(List.of(
                    "First (index 0)",
                    "Position 2 (index 1)"), labels(position));
            assertFalse(labels(position).stream()
                    .anyMatch(label -> label.startsWith("Last")),
                    "the actual last position is a no-op and must not be offered");
            return null;
        });
    }

    @Test
    void occupiedSingleSlotDoesNotOfferImplicitReplacement() throws Exception {
        WidgetDefinition scaffoldDefinition = definition("flutter.material.Scaffold");
        WidgetNode center = new WidgetNode(
                id("e3a3ed63-209c-49fc-9bb7-d61a967522bd"),
                type("flutter.widgets.Center"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        WidgetNode scaffold = new WidgetNode(
                id("e00a3e9a-abf4-4a26-8af4-8fb1192f9d3b"),
                scaffoldDefinition.typeId(),
                Map.of(),
                Map.of(BODY, WidgetSlot.SingleSlot.of(center)),
                Extensions.empty());
        FlutterWidgetSlotPropertyEditor editor = new FlutterWidgetSlotPropertyEditor(
                scaffold,
                scaffoldDefinition,
                scaffoldDefinition.slot(BODY).orElseThrow(),
                new FlutterWidgetSlotEditorContext(
                        document(scaffold),
                        CATALOG,
                        List.of(type("flutter.widgets.Text"))));
        PropertyEnv environment = PropertyEnv.create(descriptor("Body"));
        editor.attachEnv(environment);

        onEdt(() -> {
            JComboBox<?> action = component(
                    editor.getCustomEditor(),
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            assertFalse(labels(action).contains("Add new widget"));
            assertTrue(labels(action).contains("Clear slot"));
            return null;
        });
    }

    private static WidgetDefinition definition(String value) {
        return CATALOG.find(type(value)).orElseThrow();
    }

    private static WidgetTypeId type(String value) {
        return new WidgetTypeId(value);
    }

    private static StableId id(String value) {
        return StableId.parse(value);
    }

    private static WidgetNode text(StableId id, String data) {
        return new WidgetNode(
                id,
                type("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(data)),
                Map.of(),
                Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        ManagedRegion checksum = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(
                id("0954b8d3-c41b-4796-9b22-977181523ec5"),
                new DartSourceDescriptor(
                        "slot_screen.dart",
                        "SlotScreen",
                        WidgetClassKind.STATELESS,
                        Optional.empty(),
                        new ManagedRegions(checksum, checksum)),
                root);
    }

    private static FeatureDescriptor descriptor(String displayName) {
        FeatureDescriptor descriptor = new FeatureDescriptor();
        descriptor.setName(displayName);
        descriptor.setDisplayName(displayName);
        return descriptor;
    }

    @SuppressWarnings("unchecked")
    private static Node.Property<FlutterWidgetSlotCellValue> slotProperty(
            FlutterWidgetPropertiesNode node,
            String name) {
        Node.Property<?> property = Arrays.stream(node.getPropertySets())
                .filter(set -> FlutterWidgetPropertiesNode.SLOTS_SET_NAME
                        .equals(set.getName()))
                .flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(FlutterWidgetSlotCellValue.class, property.getValueType());
        return (Node.Property<FlutterWidgetSlotCellValue>) property;
    }

    private static List<String> labels(JComboBox<?> combo) {
        ArrayList<String> values = new ArrayList<>();
        for (int index = 0; index < combo.getItemCount(); index++) {
            values.add(combo.getItemAt(index).toString());
        }
        return values;
    }

    private static void selectLabel(JComboBox<?> combo, String label) {
        for (int index = 0; index < combo.getItemCount(); index++) {
            if (label.equals(combo.getItemAt(index).toString())) {
                combo.setSelectedIndex(index);
                return;
            }
        }
        throw new AssertionError("Missing combo item " + label + " in " + labels(combo));
    }

    private static void selectContains(JComboBox<?> combo, String fragment) {
        for (int index = 0; index < combo.getItemCount(); index++) {
            if (combo.getItemAt(index).toString().contains(fragment)) {
                combo.setSelectedIndex(index);
                return;
            }
        }
        throw new AssertionError(
                "Missing combo item containing " + fragment + " in " + labels(combo));
    }

    private static <T extends Component> T component(
            Component root,
            String name,
            Class<T> type) {
        if (type.isInstance(root) && name.equals(root.getName())) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = component(child, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T onEdt(ThrowingSupplier<T> operation) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return operation.get();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(operation.get());
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
        return result.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
