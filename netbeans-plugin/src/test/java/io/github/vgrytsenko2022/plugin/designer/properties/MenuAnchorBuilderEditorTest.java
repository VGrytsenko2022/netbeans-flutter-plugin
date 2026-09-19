package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.command.DesignerCommand;
import io.github.vgrytsenko2022.designer.model.*;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyEditor;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import static org.junit.jupiter.api.Assertions.*;

class MenuAnchorBuilderEditorTest {
    @Test void explicitTemplateConfirmationCreatesOnlyOnActionAndNeverNavigatesOrMutatesOtherProperties() throws Exception {
        var context = new Context(); var widget = MenuAnchorPropertyContractTest.prototype(); var commands = new ArrayList<DesignerCommand>();
        var node = MenuAnchorPropertyContractTest.node(widget, commands); node.updateEventsContext(context);
        var row = MenuAnchorPropertyContractTest.cell(node, "builder");
        onEdt(() -> {
            var editor = row.getPropertyEditor(); assertInstanceOf(FlutterMenuAnchorBuilderPropertyEditor.class, editor);
            ((ExPropertyEditor) editor).attachEnv(PropertyEnv.create(row)); var panel = editor.getCustomEditor();
            String help = find(panel, JTextArea.class, FlutterMenuAnchorBuilderPropertyEditor.GUIDANCE_NAME).getText();
            assertTrue(help.contains("TextButton")); assertTrue(help.contains("interactive Child is not rewired")); assertTrue(help.contains("No automatic Save or navigation"));
            var create = find(panel, JButton.class, FlutterMenuAnchorBuilderPropertyEditor.CREATE_NAME); assertFalse(create.isEnabled());
            find(panel, JTextField.class, FlutterMenuAnchorBuilderPropertyEditor.METHOD_NAME).setText("_openMyMenu");
            assertTrue(context.creates.isEmpty(), "Opening, editing or cancelling the dialog publishes nothing.");
            find(panel, JCheckBox.class, FlutterMenuAnchorBuilderPropertyEditor.CONFIRM_NAME).doClick(); assertTrue(create.isEnabled()); create.doClick();
            assertEquals(List.of("_openMyMenu"), context.creates); assertEquals(0, context.navigations); assertTrue(commands.isEmpty());
            return null;
        });
        onEdt(() -> null); assertEquals(FlutterPropertyCellValue.unset(), row.getValue()); assertEquals(widget.slots(), MenuAnchorPropertyContractTest.prototype().slots());
    }

    @Test void staleSnapshotAndReadOnlyWithdrawalBlockConfirmedCreateWithoutSourceActions() throws Exception {
        for (boolean stale : List.of(false, true)) {
            var context = new Context(); var widget = MenuAnchorPropertyContractTest.prototype(); var commands = new ArrayList<DesignerCommand>();
            var node = MenuAnchorPropertyContractTest.node(widget, commands); node.updateEventsContext(context); var row = MenuAnchorPropertyContractTest.cell(node, "builder");
            onEdt(() -> {
                var editor = row.getPropertyEditor(); ((ExPropertyEditor) editor).attachEnv(PropertyEnv.create(row)); var panel = editor.getCustomEditor();
                find(panel, JCheckBox.class, FlutterMenuAnchorBuilderPropertyEditor.CONFIRM_NAME).doClick();
                if (stale) node.updateEventsContext(new Context()); else node.refreshPresentation(widget, MenuAnchorPropertyContractTest.DEF, null, null, null, FlutterImageAssetChoices.empty());
                find(panel, JButton.class, FlutterMenuAnchorBuilderPropertyEditor.CREATE_NAME).doClick(); assertTrue(context.creates.isEmpty()); assertTrue(commands.isEmpty());
                assertTrue(find(panel, JTextArea.class, FlutterMenuAnchorBuilderPropertyEditor.STATUS_NAME).getText().contains(stale ? "revision changed" : "read-only")
                        || find(panel, JTextArea.class, FlutterMenuAnchorBuilderPropertyEditor.STATUS_NAME).getText().contains("revision changed"));
                assertSame(row, MenuAnchorPropertyContractTest.cell(node, "builder")); return null;
            });
        }
    }

    @Test void stagedReferenceDraftBlocksCreateBeforeOrAfterConfirmationWithoutPublishingOrDiscardingIt() throws Exception {
        for (boolean confirmFirst : List.of(false, true)) {
            for (String branch : List.of("local", "factory", "null", "invalid")) onEdt(() -> {
                var context = new Context(); var widget = MenuAnchorPropertyContractTest.prototype();
                var commands = new ArrayList<DesignerCommand>(); var node = MenuAnchorPropertyContractTest.node(widget, commands);
                node.updateEventsContext(context); var row = MenuAnchorPropertyContractTest.cell(node, "builder");
                var editor = row.getPropertyEditor(); var env = PropertyEnv.create(row);
                ((ExPropertyEditor) editor).attachEnv(env); var panel = editor.getCustomEditor();
                var tabs = find(panel, JTabbedPane.class, FlutterMenuAnchorBuilderPropertyEditor.TABS_NAME);
                var confirm = find(panel, JCheckBox.class, FlutterMenuAnchorBuilderPropertyEditor.CONFIRM_NAME);
                var create = find(panel, JButton.class, FlutterMenuAnchorBuilderPropertyEditor.CREATE_NAME);
                if (confirmFirst) { tabs.setSelectedIndex(1); confirm.doClick(); assertTrue(create.isEnabled()); }
                var mode = find(panel, JComboBox.class, FlutterNullableDartReferenceEditorComponent.MODE_NAME);
                var root = find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
                FlutterPropertyCellValue expected;
                if (branch.equals("null")) {
                    mode.setSelectedItem("Explicit null"); expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
                } else {
                    mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.PROJECT);
                    root.setText(branch.equals("invalid") ? "unfinished." : branch.equals("factory") ? "makeBuilder" : "_draftBuilder");
                    if (branch.equals("factory")) {
                        find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME).setSelectedIndex(1);
                        find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME).setText("package:app/menu.dart");
                        find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
                        expected = FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/menu.dart"), "makeBuilder", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)));
                    } else expected = branch.equals("invalid") ? null : FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_draftBuilder", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
                }
                assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(), "A Reference draft is not published before outer OK.");
                if (confirmFirst) {
                    // Exercise action-time validation even without another tab or
                    // confirmation change notifying the scoped action panel.
                    assertTrue(create.isEnabled()); create.doClick();
                } else {
                    tabs.setSelectedIndex(1); confirm.doClick(); create.doClick();
                }
                assertFalse(create.isEnabled()); assertTrue(context.creates.isEmpty()); assertEquals(0, context.navigations);
                assertTrue(find(panel, JTextArea.class, FlutterMenuAnchorBuilderPropertyEditor.STATUS_NAME).getText().contains("Reference draft"));
                assertEquals(FlutterPropertyCellValue.unset(), row.getValue()); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                assertTrue(commands.isEmpty()); assertSame(row, MenuAnchorPropertyContractTest.cell(node, "builder"));
                var reopened = row.getPropertyEditor(); ((ExPropertyEditor) reopened).attachEnv(PropertyEnv.create(row)); reopened.getCustomEditor();
                assertEquals(FlutterPropertyCellValue.unset(), reopened.getValue(), "Cancel/reopen must retain the original model, not the draft.");
                if (expected != null) {
                    env.setState(PropertyEnv.STATE_VALID);
                    assertEquals(expected, editor.getValue(), "Blocked Create must retain the exact draft for ordinary outer OK.");
                } else {
                    assertEquals("unfinished.", root.getText()); assertEquals(PropertyEnv.STATE_INVALID, env.getState());
                }
                assertTrue(commands.isEmpty()); assertTrue(context.creates.isEmpty()); return null;
            });
        }
    }

    @Test void returningReferenceToOriginalOmissionReenablesConfirmedTemplateWithoutCommittingADraft() throws Exception {
        onEdt(() -> {
            var context = new Context(); var commands = new ArrayList<DesignerCommand>();
            var node = MenuAnchorPropertyContractTest.node(MenuAnchorPropertyContractTest.prototype(), commands); node.updateEventsContext(context);
            var row = MenuAnchorPropertyContractTest.cell(node, "builder"); var editor = row.getPropertyEditor();
            ((ExPropertyEditor) editor).attachEnv(PropertyEnv.create(row)); var panel = editor.getCustomEditor();
            var tabs = find(panel, JTabbedPane.class, FlutterMenuAnchorBuilderPropertyEditor.TABS_NAME);
            var mode = find(panel, JComboBox.class, FlutterNullableDartReferenceEditorComponent.MODE_NAME);
            mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.PROJECT);
            find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("_draftBuilder");
            tabs.setSelectedIndex(1); find(panel, JCheckBox.class, FlutterMenuAnchorBuilderPropertyEditor.CONFIRM_NAME).doClick();
            var create = find(panel, JButton.class, FlutterMenuAnchorBuilderPropertyEditor.CREATE_NAME); assertFalse(create.isEnabled());
            tabs.setSelectedIndex(0); mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.OMIT); tabs.setSelectedIndex(1);
            assertTrue(create.isEnabled()); assertEquals("", find(panel, JTextArea.class, FlutterMenuAnchorBuilderPropertyEditor.STATUS_NAME).getText());
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue()); create.doClick();
            assertEquals(List.of("_buildMenuAnchor"), context.creates); assertTrue(commands.isEmpty()); assertEquals(0, context.navigations); return null;
        });
        onEdt(() -> null);
    }

    @Test void boundBuilderRequiresResetAndOnlyDirectLocalMethodOffersExplicitNavigation() throws Exception {
        for (boolean imported : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/menu.dart") : Optional.empty(), imported ? "builder" : "_builder", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            var widget = MenuAnchorPropertyContractTest.with(MenuAnchorPropertyContractTest.prototype(), Map.of(new PropertyName("builder"), reference));
            var context = new Context(); var node = MenuAnchorPropertyContractTest.node(widget, new ArrayList<>()); node.updateEventsContext(context); var row = MenuAnchorPropertyContractTest.cell(node, "builder");
            onEdt(() -> {
                var editor = row.getPropertyEditor(); ((ExPropertyEditor) editor).attachEnv(PropertyEnv.create(row)); var panel = editor.getCustomEditor();
                assertFalse(find(panel, JButton.class, FlutterMenuAnchorBuilderPropertyEditor.CREATE_NAME).isEnabled());
                var navigate = find(panel, JButton.class, FlutterMenuAnchorBuilderPropertyEditor.NAVIGATE_NAME); assertEquals(!imported, navigate.isEnabled());
                navigate.doClick(); assertEquals(imported ? 0 : 1, context.navigations); assertTrue(context.creates.isEmpty()); assertEquals(reference, ((FlutterPropertyCellValue) row.getValue()).explicitValue().orElseThrow()); return null;
            });
            onEdt(() -> null);
        }
    }

    @Test void nameCollisionFailureRetainsReferenceDraftAndAllowsCorrection() throws Exception {
        var context = new Context(); context.failure = "Method _existing already exists; no source bytes changed.";
        var widget = MenuAnchorPropertyContractTest.prototype(); var node = MenuAnchorPropertyContractTest.node(widget, new ArrayList<>()); node.updateEventsContext(context); var row = MenuAnchorPropertyContractTest.cell(node, "builder");
        var panel = onEdt(() -> { var editor = row.getPropertyEditor(); ((ExPropertyEditor) editor).attachEnv(PropertyEnv.create(row)); var component = editor.getCustomEditor();
            find(component, JTextField.class, FlutterMenuAnchorBuilderPropertyEditor.METHOD_NAME).setText("_existing"); find(component, JCheckBox.class, FlutterMenuAnchorBuilderPropertyEditor.CONFIRM_NAME).doClick(); find(component, JButton.class, FlutterMenuAnchorBuilderPropertyEditor.CREATE_NAME).doClick(); return component; });
        onEdt(() -> { assertTrue(find(panel, JTextArea.class, FlutterMenuAnchorBuilderPropertyEditor.STATUS_NAME).getText().contains("already exists")); assertTrue(find(panel, JButton.class, FlutterMenuAnchorBuilderPropertyEditor.CREATE_NAME).isEnabled()); assertEquals(FlutterPropertyCellValue.unset(), row.getValue()); return null; });
    }

    @Test void nullableReferenceOmitNullAndFactoryDraftsPreserveCancelAndReopen() throws Exception {
        for (String name : List.of("controller", "childFocusNode", "layerLink", "style", "builder", "onOpen", "onClose", "onAnimationStatusChanged")) {
            for (String branch : List.of("omit", "null", "factory")) onEdt(() -> {
                var binding = FlutterTypedPropertyEditors.binding(MenuAnchorPropertyContractTest.DEF.property(new PropertyName(name)).orElseThrow()).orElseThrow();
                var editor = binding.createEditor(); var initial = FlutterPropertyCellValue.explicit(MenuAnchorPropertyContractTest.value(name)); editor.setValue(initial);
                var env = PropertyEnv.create(new java.beans.FeatureDescriptor()); ((ExPropertyEditor) editor).attachEnv(env); var panel = editor.getCustomEditor();
                var mode = find(panel, JComboBox.class, FlutterNullableDartReferenceEditorComponent.MODE_NAME);
                FlutterPropertyCellValue expected;
                if (branch.equals("omit")) { mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.OMIT); expected = FlutterPropertyCellValue.unset(); }
                else if (branch.equals("null")) { mode.setSelectedItem("Explicit null"); expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()); }
                else {
                    mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.PROJECT);
                    find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME).setSelectedIndex(1);
                    find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME).setText("package:app/menu.dart");
                    find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("makeValue");
                    find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
                    expected = FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/menu.dart"), "makeValue", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)));
                }
                assertEquals(initial, editor.getValue(), "Cancel must retain the previous value."); env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue());
                var reopened = binding.createEditor(); reopened.setValue(expected); var reopenedEnv = PropertyEnv.create(new java.beans.FeatureDescriptor()); ((ExPropertyEditor) reopened).attachEnv(reopenedEnv); reopened.getCustomEditor(); assertEquals(expected, reopened.getValue()); return null;
            });
        }
    }

    @Test void signedOffsetLocalNullOmitAndFactoryModesCommitOnlyOnOkAndReopenExactly() throws Exception {
        var binding = FlutterTypedPropertyEditors.binding(MenuAnchorPropertyContractTest.DEF.property(new PropertyName("alignmentOffset")).orElseThrow()).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.OFFSET_REFERENCE, binding.editorKind());
        for (String branch : List.of(FlutterLocalDartReferenceEditorComponent.OMIT, FlutterLocalDartReferenceEditorComponent.NULL, FlutterLocalDartReferenceEditorComponent.OFFSET, FlutterLocalDartReferenceEditorComponent.PROJECT)) onEdt(() -> {
            var initial = FlutterPropertyCellValue.explicit(MenuAnchorPropertyContractTest.value("alignmentOffset")); var editor = binding.createEditor(); editor.setValue(initial);
            var env = PropertyEnv.create(new java.beans.FeatureDescriptor()); ((ExPropertyEditor) editor).attachEnv(env); var panel = editor.getCustomEditor();
            find(panel, JComboBox.class, FlutterLocalDartReferenceEditorComponent.MODE_NAME).setSelectedItem(branch); FlutterPropertyCellValue expected;
            if (branch.equals(FlutterLocalDartReferenceEditorComponent.OMIT)) expected = FlutterPropertyCellValue.unset();
            else if (branch.equals(FlutterLocalDartReferenceEditorComponent.NULL)) expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
            else if (branch.equals(FlutterLocalDartReferenceEditorComponent.OFFSET)) {
                find(panel, JTextField.class, FlutterOffsetPropertyEditorComponents.DX_COMPONENT_NAME).setText("-12.5"); find(panel, JTextField.class, FlutterOffsetPropertyEditorComponents.DY_COMPONENT_NAME).setText("3.25");
                expected = FlutterPropertyCellValue.explicit(new PropertyValue.OffsetValue(new java.math.BigDecimal("-12.5"), new java.math.BigDecimal("3.25")));
            } else {
                find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("_offsetFactory"); find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
                expected = FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_offsetFactory", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)));
            }
            assertEquals(initial, editor.getValue(), "Cancel preserves Offset and its source mode."); env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue());
            var reopened = binding.createEditor(); reopened.setValue(expected); var next = PropertyEnv.create(new java.beans.FeatureDescriptor()); ((ExPropertyEditor) reopened).attachEnv(next); reopened.getCustomEditor(); assertEquals(expected, reopened.getValue()); return null;
        });
    }

    private static final class Context implements FlutterWidgetEventsContext {
        final List<String> creates = new ArrayList<>(); int navigations; String failure;
        @Override public CompletionStage<Void> createMenuAnchorBuilder(String methodName) { creates.add(methodName); return failure == null ? CompletableFuture.completedFuture(null) : CompletableFuture.failedFuture(new IllegalArgumentException(failure)); }
        @Override public CompletionStage<Void> navigateToMenuAnchorBuilder() { navigations++; return CompletableFuture.completedFuture(null); }
        @Override public CompletionStage<List<Handler>> discover(PropertyName property) { return CompletableFuture.completedFuture(List.of()); }
        @Override public CompletionStage<Void> create(PropertyName property, String name) { throw new AssertionError("Builder must not use Events."); }
        @Override public CompletionStage<Void> bind(PropertyName property, Handler handler) { throw new AssertionError(); }
        @Override public CompletionStage<Void> editBinding(PropertyName property, Optional<PropertyValue> value) { throw new AssertionError(); }
        @Override public CompletionStage<Void> navigate(PropertyName property) { throw new AssertionError(); }
        @Override public CompletionStage<Void> rename(PropertyName property, String name) { throw new AssertionError(); }
        @Override public CompletionStage<Void> disconnect(PropertyName property) { throw new AssertionError(); }
    }
    private static <T extends Component> T find(Component component, Class<T> type, String name) { if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component); if (component instanceof Container container) for (Component child : container.getComponents()) { try { return find(child, type, name); } catch (NoSuchElementException ignored) { } } throw new NoSuchElementException(name); }
    private static <T> T onEdt(Callable<T> operation) throws Exception { var task = new FutureTask<T>(operation); SwingUtilities.invokeAndWait(task); return task.get(); }
}
