package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;

import dev.flutter.netbeans.designer.catalog.AppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.model.*;
import java.awt.Component;
import java.awt.Container;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class AppBarNotificationPredicatePropertyTest {
    private static final dev.flutter.netbeans.designer.catalog.WidgetDefinition DEFINITION = BuiltInWidgetCatalog.getDefault()
            .find(AppBarWidgetPropertySchema.APP_BAR_TYPE).orElseThrow();
    private static final PropertyName PREDICATE = new PropertyName("notificationPredicate");
    private static final PropertyName PRIMARY = new PropertyName("primary");

    @Test
    void nonnullPredicateStaysInBehaviorWithoutInventingANativeEventOrChangingPropertyCounts() {
        var node = node(widget(Optional.empty()), new ArrayList<>());
        assertEquals(120, DEFINITION.properties().size());
        var behavior = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals("appBarBehavior")).findFirst().orElseThrow();
        assertTrue(Arrays.stream(behavior.getProperties()).anyMatch(row -> row.getName().equals(PREDICATE.value())));
        assertEquals("Properties", behavior.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        assertTrue(Arrays.stream(node.getPropertySets()).noneMatch(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)));
        var descriptor = WidgetEventCatalog.eventsFor(DEFINITION).stream().filter(event -> event.propertyName().equals(PREDICATE)).findFirst().orElseThrow();
        assertEquals(WidgetEventDescriptor.Kind.PREDICATE, descriptor.kind()); assertEquals("bool", descriptor.signature().returnType());
        assertEquals(List.of("ScrollNotification"), descriptor.signature().parameters().stream().map(WidgetEventDescriptor.Parameter::type).toList());
        var catalog = BuiltInWidgetCatalog.getDefault().definitions();
        assertEquals(226, catalog.size()); assertEquals(7495, catalog.stream().mapToInt(definition -> definition.properties().size()).sum());
        assertEquals(240, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).count());
        assertEquals(3, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).filter(event -> event.kind() == WidgetEventDescriptor.Kind.PREDICATE).count());
        assertEquals(174, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).filter(event -> event.kind() == WidgetEventDescriptor.Kind.EVENT).count());
        assertEquals(88, catalog.stream().filter(definition -> !WidgetEventCatalog.eventsFor(definition).isEmpty()).count());
        assertEquals(Set.of(PropertyValueKind.STRING, PropertyValueKind.DART_OBJECT_REFERENCE), DEFINITION.property(PREDICATE).orElseThrow().acceptedKinds());
        assertEquals("ScrollNotificationPredicate", AppBarWidgetPropertySchema.NOTIFICATION_PREDICATE_TYPE);
        assertEquals(List.of("default", "depthZero", "all"), binding().stringPresets());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.PRESET_DART_REFERENCE, binding().editorKind());
        var row = row(node); assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
        assertEquals(Boolean.TRUE, row.getValue(FlutterDartObjectReferenceEditorComponent.APP_BAR_PREDICATE_ATTRIBUTE));
        assertFalse(row.getPropertyEditor() instanceof FlutterWidgetEventPropertyEditor); assertNull(row.getPropertyEditor().getTags());
        for (var invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_predicate"),
                new PropertyValue.StringValue("(notification) => true"), new PropertyValue.StringValue("null")))
            assertThrows(IllegalArgumentException.class, () -> binding().validate(FlutterPropertyCellValue.explicit(invalid)));
        assertDoesNotThrow(() -> binding().validate(FlutterPropertyCellValue.unset()));
    }

    @Test
    void presetsAndOmissionRemainDistinctAndCommitExactlyOnceWithoutNullMode() throws Exception {
        for (String branch : List.of("default", "depthZero", "all", "omit")) {
            var initial = new PropertyValue.StringValue("depthZero"); var node = node(widget(Optional.of(initial)), new ArrayList<>()); var row = row(node);
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(initial));
                var env = environment(editor, row); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
                var panel = editor.getCustomEditor(); var mode = find(panel, FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class);
                assertEquals(3, mode.getItemCount());
                assertEquals(List.of(FlutterPresetDartReferenceEditorComponent.OMIT, FlutterPresetDartReferenceEditorComponent.PRESET,
                        FlutterPresetDartReferenceEditorComponent.PROJECT), java.util.stream.IntStream.range(0, mode.getItemCount()).mapToObj(mode::getItemAt).toList());
                if (branch.equals("omit")) mode.setSelectedItem(FlutterPresetDartReferenceEditorComponent.OMIT);
                else {
                    mode.setSelectedItem(FlutterPresetDartReferenceEditorComponent.PRESET);
                    find(panel, FlutterPresetDartReferenceEditorComponent.PRESET_NAME, JComboBox.class).setSelectedItem(branch);
                }
                assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(), "Cancel cannot publish a preset or omission draft.");
                assertEquals(0, commits.get());
                var expected = branch.equals("omit") ? FlutterPropertyCellValue.unset() : FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(branch));
                env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue());
                assertEquals(1, commits.get());
                env.setState(PropertyEnv.STATE_NEEDS_VALIDATION); env.setState(PropertyEnv.STATE_VALID);
                assertEquals(1, commits.get());
                var reopened = row.getPropertyEditor(); reopened.setValue(expected); environment(reopened, row);
                assertEquals(branch.equals("omit") ? FlutterPresetDartReferenceEditorComponent.OMIT : FlutterPresetDartReferenceEditorComponent.PRESET,
                        find(reopened.getCustomEditor(), FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class).getSelectedItem());
            });
        }
    }

    @Test
    void currentPackageGetterMemberAndZeroArgumentFactoriesRetainExactReferenceOnOkAndReopen() throws Exception {
        for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean invoke : List.of(false, true)) {
            var expected = reference(imported, member, invoke); var row = row(node(widget(Optional.empty()), new ArrayList<>()));
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.unset()); var env = environment(editor, row); var panel = editor.getCustomEditor();
                find(panel, FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterPresetDartReferenceEditorComponent.PROJECT);
                var root = find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class);
                for (String invalid : List.of("", "() => true", "predicate(notification)", "a + b")) {
                    root.setText(invalid); env.setState(PropertyEnv.STATE_VALID);
                    assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                }
                find(panel, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME, JComboBox.class).setSelectedIndex(imported ? 1 : 0);
                if (imported) find(panel, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME, JTextField.class).setText("package:app/predicates.dart");
                root.setText(expected.rootSymbol());
                find(panel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).setText(expected.member().orElse(""));
                find(panel, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME, JComboBox.class).setSelectedIndex(invoke ? 1 : 0);
                assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(), "The nested editor must not publish while editing.");
                var preview = find(panel, FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME, JLabel.class).getText();
                assertTrue(preview.contains(expected.rootSymbol())); assertEquals(invoke, preview.contains("()"));
                env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
                var reopened = row.getPropertyEditor(); reopened.setValue(editor.getValue()); var reopenedEnv = environment(reopened, row); var reopenedPanel = reopened.getCustomEditor();
                assertEquals(FlutterPresetDartReferenceEditorComponent.PROJECT, find(reopenedPanel, FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class).getSelectedItem());
                assertEquals(expected.rootSymbol(), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).getText());
                assertEquals(expected.member().orElse(""), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).getText());
                reopenedEnv.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), reopened.getValue());
            });
        }
    }

    @Test
    void appBarHelpExplainsScrolledUnderAcceptanceAndCanvasApproximationWithoutChangingRefreshHelp() throws Exception {
        var row = row(node(widget(Optional.of(reference(false, false, false))), new ArrayList<>()));
        SwingUtilities.invokeAndWait(() -> {
            var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(reference(false, false, false))); environment(editor, row);
            var panel = editor.getCustomEditor(); var note = find(panel, FlutterPresetDartReferenceEditorComponent.NOTE_NAME, JTextArea.class);
            for (String expected : List.of("bool Function(ScrollNotification)", "true accepts", "scrolled-under elevation", "does not stop notification propagation",
                    "never executes", "depth-zero", "approximation", "getter", "zero-argument factory", "Cancel publishes nothing"))
                assertTrue(note.getText().contains(expected), note.getText());
            var nested = find(panel, FlutterDartObjectReferenceEditorComponent.PANEL_NAME, JPanel.class);
            assertTrue(nested.getAccessibleContext().getAccessibleDescription().contains("scrolled-under elevation"));
            assertFalse(nested.getAccessibleContext().getAccessibleDescription().contains("disables refresh activation"));
            find(panel, FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterPresetDartReferenceEditorComponent.OMIT);
            assertTrue(note.getText().contains("notification.depth == 0")); assertTrue(note.getText().contains("never supplies a null predicate"));
            find(panel, FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterPresetDartReferenceEditorComponent.PRESET);
            assertTrue(note.getText().contains("default and depthZero")); assertTrue(note.getText().contains("all accepts every depth"));
            var refresh = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.RefreshIndicator")).orElseThrow().property(PREDICATE).orElseThrow();
            var refreshEditor = FlutterTypedPropertyEditors.binding(refresh, Optional.empty(), false, AppBarWidgetPropertySchema.notificationPredicatePresets()).orElseThrow().createEditor();
            refreshEditor.setValue(FlutterPropertyCellValue.explicit(reference(false, false, false))); environment(refreshEditor, new FeatureDescriptor());
            var refreshPanel = refreshEditor.getCustomEditor();
            assertTrue(find(refreshPanel, FlutterPresetDartReferenceEditorComponent.NOTE_NAME, JTextArea.class).getText().contains("disables refresh activation"));
            assertFalse(find(refreshPanel, FlutterDartObjectReferenceEditorComponent.PANEL_NAME, JPanel.class).getAccessibleContext().getAccessibleDescription().contains("scrolled-under elevation"));
        });
    }

    @Test
    void cancelResetAndRefreshKeepStableRowsAndOnlyNotifyPredicateWhilePreservingOtherValues() throws Exception {
        var reference = reference(true, true, true); var initial = widget(Optional.of(reference)); var commands = new ArrayList<DesignerCommand>();
        var node = node(initial, commands); var row = row(node); var sets = node.getPropertySets(); var primary = rawRow(node, PRIMARY);
        var editor = row.getPropertyEditor(); editor.setValue(row.getValue());
        SwingUtilities.invokeAndWait(() -> {
            var env = environment(editor, row); var panel = editor.getCustomEditor();
            find(panel, FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterPresetDartReferenceEditorComponent.OMIT);
            assertEquals(FlutterPropertyCellValue.explicit(reference), editor.getValue()); assertTrue(commands.isEmpty());
            env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        });
        row.setValue((FlutterPropertyCellValue) editor.getValue()); assertEquals(List.of(new ResetProperty(initial.id(), PREDICATE)), commands);
        var changes = new ArrayList<String>(); node.addPropertyChangeListener(event -> changes.add(event.getPropertyName()));
        var next = new WidgetNode(initial.id(), initial.type(), Map.of(PRIMARY, initial.properties().get(PRIMARY)), initial.slots());
        node.refreshPresentation(next, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertEquals(List.of(PREDICATE.value()), changes); assertSame(row, row(node)); assertSame(primary, rawRow(node, PRIMARY));
        assertArrayEquals(sets, node.getPropertySets()); assertEquals(FlutterPropertyCellValue.unset(), row.getValue());
        var restored = reference(false, false, false); row.setValue(FlutterPropertyCellValue.explicit(restored));
        assertEquals(new SetProperty(initial.id(), PREDICATE, restored), commands.getLast());
        assertEquals(reference, initial.properties().get(PREDICATE)); assertEquals(new PropertyValue.BooleanValue(true), next.properties().get(PRIMARY));
    }

    @Test
    void withdrawnMutationAuthorityRejectsOpenDialogAndResetThenRestoresSameRow() throws Exception {
        var initial = widget(Optional.of(reference(false, false, false))); var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var row = row(node);
        var editor = row.getPropertyEditor(); editor.setValue(row.getValue()); PropertyEnv[] env = new PropertyEnv[1];
        SwingUtilities.invokeAndWait(() -> {
            env[0] = environment(editor, row); var panel = editor.getCustomEditor();
            find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText("_changedWhileOpen");
        });
        node.refreshPresentation(initial, DEFINITION, null, null, null, FlutterImageAssetChoices.empty());
        assertSame(row, row(node)); assertFalse(row.canWrite());
        SwingUtilities.invokeAndWait(() -> env[0].setState(PropertyEnv.STATE_VALID));
        assertThrows(IllegalAccessException.class, () -> row.setValue((FlutterPropertyCellValue) editor.getValue()));
        assertThrows(IllegalAccessException.class, row::restoreDefaultValue); assertTrue(commands.isEmpty());
        node.refreshPresentation(initial, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertSame(row, row(node)); assertTrue(row.canWrite()); row.restoreDefaultValue();
        assertEquals(List.of(new ResetProperty(initial.id(), PREDICATE)), commands);
        var readOnly = new FlutterWidgetPropertiesNode(Children.LEAF, initial, DEFINITION);
        assertFalse(rawRow(readOnly, PREDICATE).canWrite()); assertFalse(rawRow(readOnly, PREDICATE).supportsDefaultValue());
    }

    private static FlutterTypedPropertyEditors.Binding binding() {
        return FlutterTypedPropertyEditors.binding(DEFINITION.property(PREDICATE).orElseThrow(), Optional.empty(), false,
                AppBarWidgetPropertySchema.notificationPredicatePresets()).orElseThrow();
    }
    private static PropertyValue.DartObjectReferenceValue reference(boolean imported, boolean member, boolean invoke) {
        return new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/predicates.dart") : Optional.empty(),
                member ? "Predicates" : imported ? "predicate" : "_predicate", member ? Optional.of("appBar") : Optional.empty(),
                invoke ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                invoke ? Optional.of(false) : Optional.empty());
    }
    private static WidgetNode widget(Optional<? extends PropertyValue> predicate) {
        var prototype = WidgetNodePrototypeFactory.create(DEFINITION, StableId.random()); var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
        properties.put(PRIMARY, new PropertyValue.BooleanValue(true)); predicate.ifPresent(value -> properties.put(PREDICATE, value));
        return new WidgetNode(prototype.id(), prototype.type(), properties, prototype.slots());
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEFINITION, commands::add); }
    private static Node.Property<?> rawRow(FlutterWidgetPropertiesNode node, PropertyName name) {
        return Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(row -> row.getName().equals(name.value())).findFirst().orElseThrow();
    }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> row(FlutterWidgetPropertiesNode node) { return (Node.Property<FlutterPropertyCellValue>) rawRow(node, PREDICATE); }
    private static PropertyEnv environment(PropertyEditor editor, FeatureDescriptor descriptor) {
        var env = PropertyEnv.create(descriptor); ((ExPropertyEditor) editor).attachEnv(env); return env;
    }
    private static <T extends Component> T find(Component root, String name, Class<T> type) {
        if (type.isInstance(root) && name.equals(root.getName())) return type.cast(root);
        if (root instanceof Container container) for (Component child : container.getComponents()) {
            try { return find(child, name, type); } catch (IllegalArgumentException ignored) { }
        }
        throw new IllegalArgumentException("Missing component " + name);
    }
}
