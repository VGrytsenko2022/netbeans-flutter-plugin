package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.MouseRegionWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class MouseRegionPropertyContractTest {
    private static final dev.flutter.netbeans.designer.catalog.WidgetDefinition DEFINITION = BuiltInWidgetCatalog.getDefault()
            .find(MouseRegionWidgetPropertySchema.MOUSE_REGION_TYPE).orElseThrow();

    @Test
    void allSixPropertiesAndThreeNullableEventsHaveTypedEditorsAndSeparateChildSlot() {
        var node = node(new Context(), Map.of());
        var events = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals("Events", events.getValue("tabName"));
        assertEquals(3, events.getProperties().length); assertEquals(6, DEFINITION.properties().size());
        assertEquals(3, WidgetEventCatalog.eventsFor(DEFINITION).size());
        assertEquals(1, DEFINITION.slots().size()); assertEquals("child", DEFINITION.slots().getFirst().name().value());
        for (var field : DEFINITION.properties()) {
            var row = property(node, field.name().value());
            assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
            assertNotNull(row.getPropertyEditor(), field.name().value());
            assertEquals(1, Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties()))
                    .filter(candidate -> candidate.getName().equals(field.name().value())).count());
        }
        var editor = property(node, "hitTestBehavior").getPropertyEditor();
        assertEquals(Set.of(FlutterPropertyCellValue.NOT_SET_TEXT, FlutterNullableChoiceEditorComponent.NULL_TEXT, "deferToChild", "opaque", "translucent"), Set.of(editor.getTags()));
        for (String value : List.of("deferToChild", "opaque", "translucent")) {
            editor.setAsText(value);
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("HitTestBehavior", value)), editor.getValue());
        }
        editor.setAsText("null"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), editor.getValue());
        editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
    }

    @Test
    void everyPointerEventUsesExistingAtomicCreateBindNavigateRenameDisconnectAndNullableReferenceFlows() throws Exception {
        for (var event : WidgetEventCatalog.eventsFor(DEFINITION)) {
            for (String action : List.of("create", "bind", "navigate", "rename", "disconnect", "null", "omit", "local", "project")) {
                var context = new Context();
                var node = node(context, Map.of(event.propertyName(), new PropertyValue.CallbackValue("_oldHandler")));
                SwingUtilities.invokeAndWait(() -> {
                    var editor = assertInstanceOf(FlutterWidgetEventPropertyEditor.class, property(node, event.propertyName().value()).getPropertyEditor());
                    var panel = editor.getCustomEditor(); assertEquals(0, context.calls);
                    find(panel, FlutterWidgetEventPropertyEditor.HANDLER_NAME, JTextField.class).setText("_newHandler");
                    String button = switch (action) {
                        case "create" -> FlutterWidgetEventPropertyEditor.CREATE_NAME;
                        case "bind" -> { find(panel, FlutterWidgetEventPropertyEditor.HANDLERS_NAME, JList.class).setSelectedIndex(0); yield FlutterWidgetEventPropertyEditor.BIND_NAME; }
                        case "navigate" -> FlutterWidgetEventPropertyEditor.NAVIGATE_NAME;
                        case "rename" -> FlutterWidgetEventPropertyEditor.RENAME_NAME;
                        case "disconnect" -> FlutterWidgetEventPropertyEditor.DISCONNECT_NAME;
                        case "local" -> {
                            find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.LOCAL);
                            find(panel, FlutterNullableDartReferenceEditorComponent.HANDLER_NAME, JTextField.class).setText("_localMouse");
                            yield FlutterWidgetEventPropertyEditor.REFERENCE_APPLY_NAME;
                        }
                        case "project" -> {
                            find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.PROJECT);
                            var scope = find(panel, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME, JComboBox.class);
                            // Scope entries are typed enum values, not the strings displayed by their renderer.
                            scope.setSelectedIndex(java.util.stream.IntStream.range(0, scope.getItemCount())
                                    .filter(index -> scope.getItemAt(index).toString().equals(FlutterDartObjectReferenceEditorComponent.IMPORTED_LIBRARY_TEXT))
                                    .findFirst().orElseThrow());
                            var library = find(panel, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME, JTextField.class);
                            assertTrue(library.isEnabled(), "Choosing the actual imported scope enables the URI field.");
                            library.setText("package:app/handlers.dart");
                            find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText("handleMouse");
                            yield FlutterWidgetEventPropertyEditor.REFERENCE_APPLY_NAME;
                        }
                        default -> {
                            var binding = FlutterTypedPropertyEditors.binding(DEFINITION.property(event.propertyName()).orElseThrow()).orElseThrow();
                            find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(
                                    action.equals("null") ? FlutterNullableDartReferenceEditorComponent.nullText(binding) : FlutterNullableDartReferenceEditorComponent.OMIT);
                            yield FlutterWidgetEventPropertyEditor.REFERENCE_APPLY_NAME;
                        }
                    };
                    assertEquals(0, context.calls, "Drafts and opening editors are side-effect free.");
                    var control = find(panel, button, JButton.class); assertTrue(control.isEnabled(), action + " " + event.propertyName());
                    control.doClick(); assertEquals(1, context.calls); assertEquals(event.propertyName(), context.property);
                    assertEquals(List.of("null", "omit", "local", "project").contains(action) ? "reference" : action, context.action);
                    if (action.equals("null")) assertEquals(Optional.of(new PropertyValue.NullValue()), context.value);
                    if (action.equals("omit")) assertEquals(Optional.empty(), context.value);
                    if (action.equals("local")) assertEquals(Optional.of(new PropertyValue.CallbackValue("_localMouse")), context.value);
                    if (action.equals("project")) assertEquals(Optional.of(new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/handlers.dart"),
                            "handleMouse", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())), context.value);
                });
            }
        }
    }

    @Test
    void cursorReusesAll41PresetsAndTypedReferencesWithExplicitOmissionAndCancelSafety() throws Exception {
        var presets = dev.flutter.netbeans.designer.catalog.DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
        assertEquals(41, presets.size()); assertTrue(presets.contains("defer")); assertTrue(presets.contains("uncontrolled"));
        var binding = FlutterTypedPropertyEditors.binding(DEFINITION.property(new PropertyName("cursor")).orElseThrow(), Optional.empty(), false, presets).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.PRESET_DART_REFERENCE, binding.editorKind());
        assertThrows(IllegalArgumentException.class, () -> binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        for (String preset : presets) {
            SwingUtilities.invokeAndWait(() -> {
                var editor = binding.createEditor(); editor.setValue(FlutterPropertyCellValue.unset());
                var env = org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
                ((org.openide.explorer.propertysheet.ExPropertyEditor) editor).attachEnv(env);
                var panel = editor.getCustomEditor();
                var mode = find(panel, FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class);
                assertEquals(3, mode.getItemCount(), "The nonnullable cursor has omission/preset/reference, never null.");
                var choices = find(panel, FlutterPresetDartReferenceEditorComponent.PRESET_NAME, JComboBox.class);
                assertEquals(41, choices.getItemCount());
                mode.setSelectedItem(FlutterPresetDartReferenceEditorComponent.PRESET); choices.setSelectedItem(preset);
                assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                env.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
                assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(preset)), editor.getValue());
            });
        }
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/cursors.dart"), "customCursor",
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        for (String action : List.of("unchanged", "omit", "cancel")) {
            SwingUtilities.invokeAndWait(() -> {
                var initial = FlutterPropertyCellValue.explicit(reference);
                var editor = binding.createEditor(); editor.setValue(initial);
                var env = org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
                ((org.openide.explorer.propertysheet.ExPropertyEditor) editor).attachEnv(env);
                var panel = editor.getCustomEditor();
                var mode = find(panel, FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class);
                assertEquals(FlutterPresetDartReferenceEditorComponent.PROJECT, mode.getSelectedItem());
                if (!action.equals("unchanged")) mode.setSelectedItem(FlutterPresetDartReferenceEditorComponent.OMIT);
                assertEquals(initial, editor.getValue(), "Project cursor remains a typed reference; no factory is executed and Cancel changes nothing.");
                if (!action.equals("cancel")) {
                    env.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
                    assertEquals(action.equals("omit") ? FlutterPropertyCellValue.unset() : initial, editor.getValue());
                }
            });
        }
    }

    @Test
    void staleAndUnavailableEditorsCannotDispatchAndCancelLeavesTheBindingUntouched() throws Exception {
        var context = new Context(); var initial = Map.<PropertyName, PropertyValue>of(new PropertyName("onEnter"), new PropertyValue.CallbackValue("_original"));
        var node = node(context, initial); Component[] panel = new Component[1];
        SwingUtilities.invokeAndWait(() -> {
            var editor = property(node, "onEnter").getPropertyEditor(); panel[0] = editor.getCustomEditor();
            find(panel[0], FlutterWidgetEventPropertyEditor.HANDLER_NAME, JTextField.class).setText("_cancelledDraft");
            assertEquals(FlutterPropertyCellValue.explicit(initial.values().iterator().next()), editor.getValue());
            assertEquals(0, context.calls);
        });
        node.updateEventsContext(new Context());
        SwingUtilities.invokeAndWait(() -> {
            find(panel[0], FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).doClick();
            assertFalse(find(panel[0], FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).isEnabled());
            assertTrue(find(panel[0], FlutterWidgetEventPropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            assertEquals(0, context.calls);
            var unavailable = new Context(); unavailable.reason = "Document is read-only";
            var readOnly = node(unavailable, initial);
            var readOnlyPanel = property(readOnly, "onEnter").getPropertyEditor().getCustomEditor();
            assertFalse(find(readOnlyPanel, FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).isEnabled());
            assertFalse(find(readOnlyPanel, FlutterWidgetEventPropertyEditor.DISCONNECT_NAME, JButton.class).isEnabled());
            assertEquals(0, unavailable.calls);
        });
    }

    private static FlutterWidgetPropertiesNode node(Context context, Map<PropertyName, PropertyValue> values) {
        var prototype = WidgetNodePrototypeFactory.create(DEFINITION, StableId.random());
        var widget = new WidgetNode(prototype.id(), prototype.type(), values, prototype.slots());
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEFINITION,
                ignored -> fail("Events must use the source/model operation, not ordinary property mutation."));
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
        int calls; String action, reason = ""; PropertyName property; Optional<PropertyValue> value;
        @Override public String unavailableReason() { return reason; }
        @Override public CompletionStage<List<Handler>> discover(PropertyName property) { return CompletableFuture.completedFuture(List.of(new Handler("_existing", "_existing", "typed pointer event"))); }
        private CompletionStage<Void> called(String action, PropertyName property) { calls++; this.action = action; this.property = property; return CompletableFuture.completedFuture(null); }
        @Override public CompletionStage<Void> create(PropertyName property, String name) { return called("create", property); }
        @Override public CompletionStage<Void> bind(PropertyName property, Handler handler) { return called("bind", property); }
        @Override public CompletionStage<Void> rename(PropertyName property, String name) { return called("rename", property); }
        @Override public CompletionStage<Void> disconnect(PropertyName property) { return called("disconnect", property); }
        @Override public CompletionStage<Void> navigate(PropertyName property) { return called("navigate", property); }
        @Override public CompletionStage<Void> editBinding(PropertyName property, Optional<PropertyValue> value) { this.value = value; return called("reference", property); }
    }
}
