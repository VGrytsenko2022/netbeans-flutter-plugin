package io.github.vgrytsenko2022.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;
import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.NotificationListenerWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.model.*;
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

class NotificationListenerPropertyContractTest {
    private static final io.github.vgrytsenko2022.designer.catalog.WidgetDefinition DEFINITION = BuiltInWidgetCatalog.getDefault()
            .find(NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE).orElseThrow();

    @Test
    void twoPropertiesOneBoolEventAndRequiredChildUseExactTypedEditors() {
        var node = node(new Context(), Map.of());
        assertEquals(2, DEFINITION.properties().size()); assertEquals(1, DEFINITION.slots().getFirst().minChildren());
        var event = WidgetEventCatalog.eventsFor(DEFINITION).getFirst();
        assertEquals("bool", event.signature().returnType()); assertEquals("Notification", event.signature().parameters().getFirst().type());
        assertTrue(event.defaultEvent()); assertEquals(1, WidgetEventCatalog.eventsFor(DEFINITION).size());
        var events = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals("Events", events.getValue("tabName")); assertEquals(1, events.getProperties().length);
        var type = property(node, "notificationType");
        assertTrue(type.canWrite()); assertFalse(type.supportsDefaultValue());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NOTIFICATION_TYPE, typeBinding().editorKind());
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("Notification")), value(type));
        assertTrue(property(node, "onNotification").supportsDefaultValue());
        assertTrue(property(node, "onNotification").getShortDescription().contains("True stops"));
    }

    @Test
    void notificationEventUsesExistingAtomicCreateBindNavigateRenameDisconnectAndNullableReferenceFlows() throws Exception {
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
                            find(panel, FlutterNullableDartReferenceEditorComponent.HANDLER_NAME, JTextField.class).setText("_localNotification");
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
                            find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText("handleNotification");
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
                    if (action.equals("local")) assertEquals(Optional.of(new PropertyValue.CallbackValue("_localNotification")), context.value);
                    if (action.equals("project")) assertEquals(Optional.of(new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/handlers.dart"),
                            "handleNotification", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())), context.value);
                });
            }
        }
    }

    @Test
    void allFourteenPresetsCommitExactlyAndCancelNeverChangesTheOriginalType() throws Exception {
        var types = NotificationListenerWidgetPropertySchema.notificationTypes();
        assertEquals(List.of("Notification", "LayoutChangedNotification", "ScrollNotification", "ScrollStartNotification",
                "ScrollUpdateNotification", "OverscrollNotification", "ScrollEndNotification", "UserScrollNotification",
                "SizeChangedLayoutNotification", "ScrollMetricsNotification", "OverscrollIndicatorNotification",
                "DraggableScrollableNotification", "KeepAliveNotification", "NavigationNotification"), types);
        for (String type : types) SwingUtilities.invokeAndWait(() -> {
            var editor = typeBinding().createEditor(); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("Notification"));
            editor.setValue(initial); var env = environment(editor); var panel = editor.getCustomEditor();
            var mode = find(panel, FlutterNotificationTypeEditorComponent.MODE_NAME, JComboBox.class);
            assertEquals(3, mode.getItemCount(), "No omit, null, generic expression or invocation mode.");
            var presets = find(panel, FlutterNotificationTypeEditorComponent.PRESET_NAME, JComboBox.class);
            assertEquals(14, presets.getItemCount()); presets.setSelectedItem(type);
            assertEquals(initial, editor.getValue(), "Cancel preserves the exact original type.");
            env.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(type)), editor.getValue());
            assertTrue(find(panel, FlutterNotificationTypeEditorComponent.NOTE_NAME, JTextArea.class).getText().contains("retains the existing handler"));
        });
    }

    @Test
    void currentAndPackageTypeSymbolsRoundTripWithoutRawExpressionOrConstEscapeHatches() throws Exception {
        for (boolean imported : List.of(false, true)) {
            var expected = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/notifications.dart") : Optional.empty(),
                    "AppNotice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            SwingUtilities.invokeAndWait(() -> {
                var editor = typeBinding().createEditor(); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("Notification"));
                editor.setValue(initial); var env = environment(editor); var panel = editor.getCustomEditor();
                var mode = find(panel, FlutterNotificationTypeEditorComponent.MODE_NAME, JComboBox.class);
                mode.setSelectedItem(imported ? FlutterNotificationTypeEditorComponent.PACKAGE : FlutterNotificationTypeEditorComponent.CURRENT);
                var uri = find(panel, FlutterNotificationTypeEditorComponent.LIBRARY_NAME, JTextField.class);
                assertEquals(imported, uri.isEnabled()); if (imported) uri.setText("package:app/notifications.dart");
                var symbol = find(panel, FlutterNotificationTypeEditorComponent.SYMBOL_NAME, JTextField.class);
                for (String invalid : List.of("", "AppNotice?", "List<AppNotice>", "Types.AppNotice", "makeNotice()")) {
                    symbol.setText(invalid); assertEquals(org.openide.explorer.propertysheet.PropertyEnv.STATE_INVALID, env.getState());
                    assertEquals(initial, editor.getValue());
                }
                symbol.setText("AppNotice"); assertEquals(initial, editor.getValue());
                env.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
                assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
                var reopened = typeBinding().createEditor(); reopened.setValue(FlutterPropertyCellValue.explicit(expected)); environment(reopened);
                assertEquals(imported ? FlutterNotificationTypeEditorComponent.PACKAGE : FlutterNotificationTypeEditorComponent.CURRENT,
                        find(reopened.getCustomEditor(), FlutterNotificationTypeEditorComponent.MODE_NAME, JComboBox.class).getSelectedItem());
                assertEquals("AppNotice", find(reopened.getCustomEditor(), FlutterNotificationTypeEditorComponent.SYMBOL_NAME, JTextField.class).getText());
            });
        }
        var binding = typeBinding();
        assertThrows(IllegalArgumentException.class, () -> binding.validate(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class, () -> binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        for (var ref : List.of(
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Types", Optional.of("Notice"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "AppNotice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "AppNotice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true))))
            assertThrows(IllegalArgumentException.class, () -> binding.validate(FlutterPropertyCellValue.explicit(ref)));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.DartObjectReferenceValue(Optional.empty(), "AppNotice", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.of(false)));
    }

    @Test
    void changingTypeKeepsRowsHandlerAndUserCodeUntouchedWhileRefreshingOnlyTypeAndCallback() throws Exception {
        var prototype = WidgetNodePrototypeFactory.create(DEFINITION, StableId.random());
        var values = new java.util.LinkedHashMap<>(prototype.properties());
        values.put(new PropertyName("onNotification"), new PropertyValue.CallbackValue("_existingNotice"));
        var widget = new WidgetNode(prototype.id(), prototype.type(), values, prototype.slots());
        var commands = new java.util.ArrayList<io.github.vgrytsenko2022.designer.command.DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEFINITION, commands::add);
        var context = new Context(); node.updateEventsContext(context);
        var type = property(node, "notificationType"); var callback = property(node, "onNotification");
        Component[] oldPanel = new Component[1];
        SwingUtilities.invokeAndWait(() -> oldPanel[0] = callback.getPropertyEditor().getCustomEditor());
        @SuppressWarnings("unchecked") var editable = (Node.Property<FlutterPropertyCellValue>) type;
        editable.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("ScrollNotification")));
        assertEquals(List.of(new io.github.vgrytsenko2022.designer.command.SetProperty(widget.id(), new PropertyName("notificationType"),
                new PropertyValue.StringValue("ScrollNotification"))), commands);
        assertEquals(new PropertyValue.CallbackValue("_existingNotice"), widget.properties().get(new PropertyName("onNotification")));
        values.put(new PropertyName("notificationType"), new PropertyValue.StringValue("ScrollNotification"));
        var next = new WidgetNode(widget.id(), widget.type(), values, widget.slots());
        var changes = new java.util.ArrayList<String>(); node.addPropertyChangeListener(event -> changes.add(event.getPropertyName()));
        node.refreshPresentation(next, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertSame(type, property(node, "notificationType")); assertSame(callback, property(node, "onNotification"));
        assertEquals(Set.of("notificationType", "onNotification"), Set.copyOf(changes));
        assertTrue(callback.getDisplayName().contains("ScrollNotification")); assertTrue(callback.getShortDescription().contains("ScrollNotification"));
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.CallbackValue("_existingNotice")), value(callback));
        SwingUtilities.invokeAndWait(() -> {
            find(oldPanel[0], FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).doClick();
            assertEquals(0, context.calls); assertTrue(find(oldPanel[0], FlutterWidgetEventPropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
        });
    }

    @Test
    void eventGuidanceExplainsSelectedSubtypeUniversalStubAndNotificationPropagation() throws Exception {
        for (String type : NotificationListenerWidgetPropertySchema.notificationTypes()) SwingUtilities.invokeAndWait(() -> {
            var node = node(new Context(), Map.of(new PropertyName("notificationType"), new PropertyValue.StringValue(type)));
            var panel = property(node, "onNotification").getPropertyEditor().getCustomEditor();
            var guidance = find(panel, FlutterWidgetEventPropertyEditor.NOTIFICATION_GUIDANCE_NAME, JTextArea.class).getText();
            assertTrue(guidance.contains(type)); assertTrue(guidance.contains("bool onNotification(Notification notification)"));
            assertTrue(guidance.contains("true to stop")); assertTrue(guidance.contains("explicit null continues"));
            assertTrue(guidance.contains("UnimplementedError")); assertTrue(guidance.contains("Canvas never executes"));
            assertTrue(find(panel, FlutterWidgetEventPropertyEditor.RETURN_GUIDANCE_NAME, JLabel.class).getText().contains("bool"));
            var referenceMode = find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class);
            assertTrue(referenceMode.getAccessibleContext().getAccessibleDescription().contains("for the selected Notification type"));
        });
    }

    private static FlutterTypedPropertyEditors.Binding typeBinding() {
        return FlutterTypedPropertyEditors.binding(DEFINITION.property(new PropertyName("notificationType")).orElseThrow()).orElseThrow();
    }
    private static org.openide.explorer.propertysheet.PropertyEnv environment(java.beans.PropertyEditor editor) {
        var env = org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
        ((org.openide.explorer.propertysheet.ExPropertyEditor) editor).attachEnv(env); return env;
    }
    private static Object value(Node.Property<?> row) {
        try { return row.getValue(); } catch (Exception failure) { throw new AssertionError(failure); }
    }

    @Test
    void staleAndUnavailableEditorsCannotDispatchAndCancelLeavesTheBindingUntouched() throws Exception {
        var context = new Context(); var initial = Map.<PropertyName, PropertyValue>of(new PropertyName("onNotification"), new PropertyValue.CallbackValue("_original"));
        var node = node(context, initial); Component[] panel = new Component[1];
        SwingUtilities.invokeAndWait(() -> {
            var editor = property(node, "onNotification").getPropertyEditor(); panel[0] = editor.getCustomEditor();
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
            var readOnlyPanel = property(readOnly, "onNotification").getPropertyEditor().getCustomEditor();
            assertFalse(find(readOnlyPanel, FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).isEnabled());
            assertFalse(find(readOnlyPanel, FlutterWidgetEventPropertyEditor.DISCONNECT_NAME, JButton.class).isEnabled());
            assertEquals(0, unavailable.calls);
        });
    }

    private static FlutterWidgetPropertiesNode node(Context context, Map<PropertyName, PropertyValue> values) {
        var prototype = WidgetNodePrototypeFactory.create(DEFINITION, StableId.random());
        var properties = new java.util.LinkedHashMap<>(prototype.properties()); properties.putAll(values);
        var widget = new WidgetNode(prototype.id(), prototype.type(), properties, prototype.slots());
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
