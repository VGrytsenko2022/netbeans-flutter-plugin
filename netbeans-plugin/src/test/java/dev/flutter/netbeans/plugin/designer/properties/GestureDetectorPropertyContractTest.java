package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.GestureDetectorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValue.PointerDeviceKindSetValue;
import dev.flutter.netbeans.designer.model.PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind;
import dev.flutter.netbeans.designer.model.StableId;
import java.awt.Component;
import java.awt.Container;
import java.beans.FeatureDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class GestureDetectorPropertyContractTest {
    private static final dev.flutter.netbeans.designer.catalog.WidgetDefinition DEFINITION = BuiltInWidgetCatalog.getDefault()
            .find(GestureDetectorWidgetPropertySchema.GESTURE_DETECTOR_TYPE).orElseThrow();

    @Test
    void exposesAllSixConfigurationPropertiesAndFiftyEightTypedEventsExactlyOnce() {
        var definition = DEFINITION;
        var node = node(new Context());
        var events = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals("Events", events.getValue("tabName"));
        assertEquals(58, events.getProperties().length);
        assertEquals(58, WidgetEventCatalog.eventsFor(definition).size());
        assertEquals(64, definition.properties().size());
        for (var field : definition.properties()) {
            var row = property(node, field.name().value());
            assertTrue(row.canWrite(), field.name().value());
            assertTrue(row.supportsDefaultValue(), field.name().value());
            assertNotNull(FlutterTypedPropertyEditors.binding(field).orElseThrow(), field.name().value());
        }
        for (String name : List.of("excludeFromSemantics", "trackpadScrollCausesScale")) {
            var binding = FlutterTypedPropertyEditors.binding(definition.property(new PropertyName(name)).orElseThrow()).orElseThrow();
            assertEquals(FlutterTypedPropertyEditors.EditorKind.BOOLEAN, binding.editorKind());
            assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isPresent());
            var editor = property(node, name).getPropertyEditor();
            editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)));
            assertNull(editor.getTags(), "Explicit Boolean editing remains a checkbox, not a combo box.");
        }
        assertEquals(FlutterTypedPropertyEditors.EditorKind.OFFSET,
                FlutterTypedPropertyEditors.binding(definition.property(new PropertyName("trackpadScrollToScaleFactor")).orElseThrow()).orElseThrow().editorKind());
    }

    @Test
    void pointerDeviceSetPreservesOmissionNullEmptyAndAllSixKindsWithCancelSafeCheckboxDrafts() throws Exception {
        var binding = FlutterTypedPropertyEditors.binding(DEFINITION.property(new PropertyName("supportedDevices")).orElseThrow()).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.POINTER_DEVICE_KIND_SET, binding.editorKind());
        for (var initial : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),
                FlutterPropertyCellValue.explicit(new PointerDeviceKindSetValue(List.of())),
                FlutterPropertyCellValue.explicit(new PointerDeviceKindSetValue(Arrays.asList(PointerDeviceKind.values()))))) {
            for (String action : List.of("unchanged", "omit", "null", "clear", "select", "cancel")) {
                var editor = binding.createEditor(); editor.setValue(initial);
                var env = PropertyEnv.create(new FeatureDescriptor()); ((ExPropertyEditor) editor).attachEnv(env);
                var changes = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> changes.incrementAndGet());
                SwingUtilities.invokeAndWait(() -> {
                    var panel = editor.getCustomEditor();
                    var mode = find(panel, FlutterPointerDeviceKindSetEditorComponent.MODE_NAME, JComboBox.class);
                    assertEquals(3, mode.getItemCount());
                    assertEquals(6, Arrays.stream(PointerDeviceKind.values()).filter(kind -> find(panel,
                            FlutterPointerDeviceKindSetEditorComponent.VALUE_PREFIX + kind.wireName(), JCheckBox.class) != null).count());
                    FlutterPropertyCellValue expected = initial;
                    switch (action) {
                        case "omit" -> { mode.setSelectedItem(FlutterPointerDeviceKindSetEditorComponent.OMIT); expected = FlutterPropertyCellValue.unset(); }
                        case "null" -> { mode.setSelectedItem(FlutterPointerDeviceKindSetEditorComponent.NULL); expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()); }
                        case "clear", "select", "cancel" -> {
                            mode.setSelectedItem(FlutterPointerDeviceKindSetEditorComponent.SET);
                            find(panel, FlutterPointerDeviceKindSetEditorComponent.CLEAR_NAME, JButton.class).doClick();
                            if (!action.equals("clear")) {
                                find(panel, FlutterPointerDeviceKindSetEditorComponent.VALUE_PREFIX + "touch", JCheckBox.class).doClick();
                                find(panel, FlutterPointerDeviceKindSetEditorComponent.VALUE_PREFIX + "trackpad", JCheckBox.class).doClick();
                            }
                            expected = FlutterPropertyCellValue.explicit(new PointerDeviceKindSetValue(action.equals("clear") ? List.of()
                                    : List.of(PointerDeviceKind.TOUCH, PointerDeviceKind.TRACKPAD)));
                        }
                        default -> { }
                    }
                    assertEquals(initial, editor.getValue(), "Opening/editing/canceling must not commit the draft.");
                    assertEquals(0, changes.get());
                    boolean explicit = FlutterPointerDeviceKindSetEditorComponent.SET.equals(mode.getSelectedItem());
                    for (PointerDeviceKind kind : PointerDeviceKind.values()) assertEquals(explicit,
                            find(panel, FlutterPointerDeviceKindSetEditorComponent.VALUE_PREFIX + kind.wireName(), JCheckBox.class).isEnabled());
                    assertTrue(find(panel, FlutterPointerDeviceKindSetEditorComponent.PREVIEW_NAME, JTextArea.class).getText().contains("empty set accepts no device kinds"));
                    if (!action.equals("cancel")) { env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue()); }
                });
            }
        }
    }

    @Test
    void nullableGestureCallbackEditorPreservesLocalAndImportedReferencesWithoutPublishingDrafts() throws Exception {
        var binding = FlutterTypedPropertyEditors.binding(DEFINITION.property(new PropertyName("onTap")).orElseThrow()).orElseThrow();
        var imported = new PropertyValue.DartObjectReferenceValue(java.util.Optional.of("package:app/events.dart"),
                "tap", java.util.Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, java.util.Optional.empty());
        for (var initial : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),
                FlutterPropertyCellValue.explicit(new PropertyValue.CallbackValue("_tap")), FlutterPropertyCellValue.explicit(imported))) {
            for (String action : List.of("unchanged", "local", "null", "omit", "cancel")) {
                SwingUtilities.invokeAndWait(() -> {
                    var editor = binding.createEditor(); editor.setValue(initial);
                    var env = PropertyEnv.create(new FeatureDescriptor()); ((ExPropertyEditor) editor).attachEnv(env);
                    var panel = editor.getCustomEditor();
                    var mode = find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class);
                    assertEquals(4, mode.getItemCount());
                    var expected = initial;
                    switch (action) {
                        case "local", "cancel" -> {
                            mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.LOCAL);
                            find(panel, FlutterNullableDartReferenceEditorComponent.HANDLER_NAME, JTextField.class).setText("_newTap");
                            expected = FlutterPropertyCellValue.explicit(new PropertyValue.CallbackValue("_newTap"));
                        }
                        case "null" -> { mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.nullText(binding)); expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()); }
                        case "omit" -> { mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.OMIT); expected = FlutterPropertyCellValue.unset(); }
                        default -> { }
                    }
                    assertEquals(initial, editor.getValue());
                    if (!action.equals("cancel")) { env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue()); }
                });
            }
        }
    }

    @Test
    void everyGestureEventUsesTheSharedAtomicCreateFlowAndStaleEditorsCannotDispatch() throws Exception {
        var context = new Context();
        for (var event : WidgetEventCatalog.eventsFor(DEFINITION)) {
            var node = node(context);
            SwingUtilities.invokeAndWait(() -> {
                var editor = assertInstanceOf(FlutterWidgetEventPropertyEditor.class, property(node, event.propertyName().value()).getPropertyEditor());
                var panel = editor.getCustomEditor();
                find(panel, FlutterWidgetEventPropertyEditor.HANDLER_NAME, JTextField.class).setText("_gestureHandler");
                find(panel, FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).doClick();
                assertEquals(event.propertyName(), context.created);
            });
        }
        assertEquals(58, context.changes);
        var node = node(context); Component[] old = new Component[1];
        SwingUtilities.invokeAndWait(() -> old[0] = property(node, "onTap").getPropertyEditor().getCustomEditor());
        node.updateEventsContext(new Context());
        SwingUtilities.invokeAndWait(() -> {
            find(old[0], FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).doClick();
            assertTrue(find(old[0], FlutterWidgetEventPropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            assertFalse(find(old[0], FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).isEnabled());
        });
        assertEquals(58, context.changes);
    }

    private static FlutterWidgetPropertiesNode node(Context context) {
        var node = new FlutterWidgetPropertiesNode(Children.LEAF,
                WidgetNodePrototypeFactory.create(DEFINITION, StableId.random()), DEFINITION,
                ignored -> fail("An Events editor must not dispatch an ordinary property draft."));
        node.updateEventsContext(context); return node;
    }
    private static Node.Property<?> property(FlutterWidgetPropertiesNode node, String name) {
        return Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(row -> row.getName().equals(name)).findFirst().orElseThrow();
    }
    private static <T extends Component> T find(Component root, String name, Class<T> type) {
        if (type.isInstance(root) && name.equals(root.getName())) return type.cast(root);
        if (root instanceof Container container) for (Component child : container.getComponents()) {
            try { return find(child, name, type); } catch (IllegalArgumentException ignored) { }
        }
        throw new IllegalArgumentException("Missing component " + name);
    }
    private static final class Context implements FlutterWidgetEventsContext {
        int changes; PropertyName created;
        @Override public String unavailableReason() { return ""; }
        @Override public CompletionStage<List<Handler>> discover(PropertyName property) { return CompletableFuture.completedFuture(List.of()); }
        @Override public CompletionStage<Void> create(PropertyName property, String name) { changes++; created = property; return CompletableFuture.completedFuture(null); }
        @Override public CompletionStage<Void> bind(PropertyName property, Handler handler) { throw new UnsupportedOperationException(); }
        @Override public CompletionStage<Void> rename(PropertyName property, String name) { throw new UnsupportedOperationException(); }
        @Override public CompletionStage<Void> disconnect(PropertyName property) { throw new UnsupportedOperationException(); }
        @Override public CompletionStage<Void> navigate(PropertyName property) { throw new UnsupportedOperationException(); }
        @Override public CompletionStage<Void> editBinding(PropertyName property, java.util.Optional<PropertyValue> value) { throw new UnsupportedOperationException(); }
    }
}
