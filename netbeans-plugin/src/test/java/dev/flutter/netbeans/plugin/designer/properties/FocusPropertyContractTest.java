package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.FocusWidgetPropertySchema;
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

class FocusPropertyContractTest {
    private static final dev.flutter.netbeans.designer.catalog.WidgetDefinition DEFINITION = BuiltInWidgetCatalog.getDefault()
            .find(FocusWidgetPropertySchema.FOCUS_TYPE).orElseThrow();

    @Test
    void allThirteenRowsAndThreeEventsKeepRequiredChildSeparateAndExplainBothConstructors() {
        var node = node(new Context(), Map.of());
        var events = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals("Events", events.getValue("tabName"));
        assertEquals(3, events.getProperties().length); assertEquals(13, DEFINITION.properties().size());
        assertEquals(12, FocusWidgetPropertySchema.SDK_PROPERTY_COUNT);
        assertEquals(3, WidgetEventCatalog.eventsFor(DEFINITION).size());
        assertEquals(1, DEFINITION.slots().size()); assertEquals(1, DEFINITION.slots().getFirst().minChildren());
        for (var field : DEFINITION.properties()) {
            var row = property(node, field.name().value());
            assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue()); assertNotNull(row.getPropertyEditor());
            assertEquals(1, Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties()))
                    .filter(candidate -> candidate.getName().equals(field.name().value())).count());
        }
        assertEquals(Set.of(FlutterPropertyCellValue.NOT_SET_TEXT, "standard", "withExternalFocusNode"),
                Set.of(property(node, "variant").getPropertyEditor().getTags()));
        assertTrue(property(node, "variant").getShortDescription().contains("before"));
        assertTrue(property(node, "focusNode").getShortDescription().contains("dispos"));
        assertTrue(property(node, "onKey").getShortDescription().contains("Deprecated"));
    }

    @Test
    void everyFocusEventUsesExistingAtomicCreateBindNavigateRenameDisconnectAndNullableReferenceFlows() throws Exception {
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
                            find(panel, FlutterNullableDartReferenceEditorComponent.HANDLER_NAME, JTextField.class).setText("_localFocus");
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
                            find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText("handleFocus");
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
                    if (action.equals("local")) assertEquals(Optional.of(new PropertyValue.CallbackValue("_localFocus")), context.value);
                    if (action.equals("project")) assertEquals(Optional.of(new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/handlers.dart"),
                            "handleFocus", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())), context.value);
                });
            }
        }
    }

    @Test
    void nullableDebugLabelPreservesOmissionNullEmptyUnicodeAndCancelledDrafts() throws Exception {
        var binding = binding("debugLabel");
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_STRING, binding.editorKind());
        for (var initial : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),
                FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("")), FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("label")))) {
            for (String mode : List.of(FlutterNullableStringEditorComponent.OMIT, FlutterNullableStringEditorComponent.NULL,
                    FlutterNullableStringEditorComponent.STRING)) for (String literal : List.of("", "Фокус 😀\nsecond line")) {
                SwingUtilities.invokeAndWait(() -> {
                    var editor = binding.createEditor(); editor.setValue(initial);
                    var env = org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
                    ((org.openide.explorer.propertysheet.ExPropertyEditor) editor).attachEnv(env);
                    var panel = editor.getCustomEditor();
                    find(panel, FlutterNullableStringEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(mode);
                    var text = find(panel, FlutterNullableStringEditorComponent.TEXT_NAME, JTextArea.class);
                    text.setText(literal); assertEquals(mode.equals(FlutterNullableStringEditorComponent.STRING), text.isEnabled());
                    assertEquals(initial, editor.getValue(), "Cancel leaves the original exact value untouched.");
                    env.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
                    var expected = mode.equals(FlutterNullableStringEditorComponent.OMIT) ? FlutterPropertyCellValue.unset()
                            : FlutterPropertyCellValue.explicit(mode.equals(FlutterNullableStringEditorComponent.NULL)
                                    ? new PropertyValue.NullValue() : new PropertyValue.StringValue(literal));
                    assertEquals(expected, editor.getValue());
                });
            }
        }
    }

    @Test
    void sixBooleanRowsKeepCenteredCheckboxAndFourNullableRowsPreserveExplicitNull() throws Exception {
        for (String name : FocusWidgetPropertySchema.booleanProperties()) {
            var binding = binding(name);
            boolean nullable = !Set.of("autofocus", "includeSemantics").contains(name);
            assertEquals(nullable ? FlutterTypedPropertyEditors.EditorKind.NULLABLE_BOOLEAN : FlutterTypedPropertyEditors.EditorKind.BOOLEAN,
                    binding.editorKind(), name);
            var initialValues = new java.util.ArrayList<>(List.of(FlutterPropertyCellValue.unset(),
                    FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)),
                    FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false))));
            if (nullable) initialValues.add(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
            else assertThrows(IllegalArgumentException.class, () -> binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
            for (var initial : initialValues) SwingUtilities.invokeAndWait(() -> {
                var editor = binding.createEditor(); editor.setValue(initial);
                var inplace = FlutterPropertyEditorComponents.inplaceFactory(binding).orElseThrow().getInplaceEditor();
                inplace.connect(editor, org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor()));
                var checkbox = assertInstanceOf(JCheckBox.class, inplace.getComponent());
                assertEquals(SwingConstants.CENTER, checkbox.getHorizontalAlignment());
                assertEquals(initial, inplace.getValue()); checkbox.doClick();
                assertInstanceOf(PropertyValue.BooleanValue.class, ((FlutterPropertyCellValue) inplace.getValue()).explicitValue().orElseThrow());
                checkbox.doClick(); assertInstanceOf(PropertyValue.BooleanValue.class, ((FlutterPropertyCellValue) inplace.getValue()).explicitValue().orElseThrow());
                inplace.clear();
            });
        }
    }

    @Test
    void switchingVariantsRetainsEveryRowAndStoredValueAndMakesExternalNodeRequired() throws Exception {
        var values = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
        values.put(new PropertyName("focusNode"), reference("projectFocusNode"));
        values.put(new PropertyName("parentNode"), new PropertyValue.NullValue());
        values.put(new PropertyName("debugLabel"), new PropertyValue.StringValue(""));
        values.put(new PropertyName("onKeyEvent"), new PropertyValue.CallbackValue("_onKey"));
        values.put(new PropertyName("canRequestFocus"), new PropertyValue.NullValue());
        var standard = widget(values); var commands = new java.util.ArrayList<dev.flutter.netbeans.designer.command.DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, standard, DEFINITION, commands::add);
        var rows = Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties()))
                .collect(java.util.stream.Collectors.toMap(Node.Property::getName, row -> row));
        var variantRow = editable(node, "variant");
        variantRow.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("withExternalFocusNode")));
        assertEquals(List.of(new dev.flutter.netbeans.designer.command.SetProperty(standard.id(), new PropertyName("variant"),
                new PropertyValue.StringValue("withExternalFocusNode"))), commands);
        assertEquals(values, standard.properties(), "The selector never deletes retained configuration.");
        values.put(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode"));
        var external = new WidgetNode(standard.id(), standard.type(), values, standard.slots());
        var notifications = new java.util.ArrayList<String>();
        node.addPropertyChangeListener(event -> notifications.add(event.getPropertyName()));
        node.refreshPresentation(external, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
        var affected = new java.util.HashSet<>(FocusWidgetPropertySchema.standardOnlyProperties());
        affected.add("variant"); affected.add("focusNode");
        assertEquals(affected, Set.copyOf(notifications), "Only rows whose displayed state changes are notified; the sheet is never rebuilt.");
        for (var row : rows.entrySet()) assertSame(row.getValue(), property(node, row.getKey()));
        for (String name : FocusWidgetPropertySchema.standardOnlyProperties()) {
            assertTrue(property(node, name).getDisplayName().contains("inactive; retained"), name);
            assertTrue(property(node, name).getShortDescription().contains("not generated"), name);
        }
        var focusNode = editable(node, "focusNode");
        assertFalse(focusNode.supportsDefaultValue()); assertTrue(focusNode.getDisplayName().contains("required"));
        assertThrows(IllegalArgumentException.class, () -> focusNode.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class, () -> focusNode.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertThrows(IllegalAccessException.class, focusNode::restoreDefaultValue);
        assertEquals(1, commands.size(), "Invalid external node edits do not dispatch.");
        node.refreshPresentation(standard, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertSame(focusNode, property(node, "focusNode")); assertTrue(focusNode.supportsDefaultValue());
        assertFalse(property(node, "debugLabel").getDisplayName().contains("inactive"));
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("")), property(node, "debugLabel").getValue());
        for (String name : List.of("focusNode", "parentNode")) {
            var binding = binding(name); assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_DART_REFERENCE, binding.editorKind());
            binding.validate(FlutterPropertyCellValue.unset()); binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
            binding.validate(FlutterPropertyCellValue.explicit(reference("nullableNode")));
        }
    }

    @Test
    void externalKeyEventsDisableOnlyActivationAndExplainTypedReturnStubs() throws Exception {
        for (String name : List.of("onKeyEvent", "onKey", "onFocusChange")) {
            for (String action : List.of("navigate", "rename", "disconnect")) {
                var context = new Context();
                var node = node(context, Map.of(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode"),
                        new PropertyName("focusNode"), reference("focusNode"), new PropertyName(name), new PropertyValue.CallbackValue("_old")));
                SwingUtilities.invokeAndWait(() -> {
                    var editor = property(node, name).getPropertyEditor(); var panel = editor.getCustomEditor();
                    boolean key = !name.equals("onFocusChange");
                    for (String button : List.of(FlutterWidgetEventPropertyEditor.CREATE_NAME, FlutterWidgetEventPropertyEditor.REFERENCE_APPLY_NAME))
                        assertEquals(!key, find(panel, button, JButton.class).isEnabled());
                    find(panel, FlutterWidgetEventPropertyEditor.HANDLERS_NAME, JList.class).setSelectedIndex(0);
                    assertEquals(!key, find(panel, FlutterWidgetEventPropertyEditor.BIND_NAME, JButton.class).isEnabled());
                    if (key) {
                        assertTrue(editor.getAsText().contains("inactive; retained"));
                        assertTrue(find(panel, FlutterWidgetEventPropertyEditor.RETURN_GUIDANCE_NAME, JLabel.class).getText().contains("UnimplementedError"));
                        assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("external"));
                    }
                    find(panel, FlutterWidgetEventPropertyEditor.HANDLER_NAME, JTextField.class).setText("_renamed");
                    var button = find(panel, switch (action) {
                        case "navigate" -> FlutterWidgetEventPropertyEditor.NAVIGATE_NAME;
                        case "rename" -> FlutterWidgetEventPropertyEditor.RENAME_NAME;
                        default -> FlutterWidgetEventPropertyEditor.DISCONNECT_NAME;
                    }, JButton.class);
                    assertTrue(button.isEnabled()); button.doClick(); assertEquals(1, context.calls); assertEquals(action, context.action);
                });
            }
        }
    }

    private static FlutterTypedPropertyEditors.Binding binding(String name) {
        return FlutterTypedPropertyEditors.binding(DEFINITION.property(new PropertyName(name)).orElseThrow()).orElseThrow();
    }
    private static PropertyValue.DartObjectReferenceValue reference(String symbol) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), symbol, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    @SuppressWarnings("unchecked")
    private static Node.Property<FlutterPropertyCellValue> editable(FlutterWidgetPropertiesNode node, String name) {
        return (Node.Property<FlutterPropertyCellValue>) property(node, name);
    }
    private static WidgetNode widget(Map<PropertyName, PropertyValue> values) {
        var prototype = WidgetNodePrototypeFactory.create(DEFINITION, StableId.random());
        return new WidgetNode(prototype.id(), prototype.type(), values, prototype.slots());
    }

    @Test
    void staleAndUnavailableEditorsCannotDispatchAndCancelLeavesTheBindingUntouched() throws Exception {
        var context = new Context(); var initial = Map.<PropertyName, PropertyValue>of(new PropertyName("onFocusChange"), new PropertyValue.CallbackValue("_original"));
        var node = node(context, initial); Component[] panel = new Component[1];
        SwingUtilities.invokeAndWait(() -> {
            var editor = property(node, "onFocusChange").getPropertyEditor(); panel[0] = editor.getCustomEditor();
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
            var readOnlyPanel = property(readOnly, "onFocusChange").getPropertyEditor().getCustomEditor();
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

