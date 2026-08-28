package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DartNumericLiterals;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyPanel;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class FlutterPropertyEditorComponentsTest {

    @Test
    void realNetBeansPropertyPanelInstallsTheTriStateCheckbox()
            throws Exception {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        WidgetNode widget = new WidgetNode(
                StableId.parse("fd96a765-01c9-45cb-aeb8-fe5613db4f79"),
                definition.typeId(),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue("Text")),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });
        Node.Property<?> softWrap = java.util.Arrays.stream(
                        node.getPropertySets()[1].getProperties())
                .filter(property -> property.getName().equals("softWrap"))
                .findFirst().orElseThrow();

        onEdt(() -> {
            PropertyPanel panel = new PropertyPanel(
                    softWrap,
                    PropertyPanel.PREF_INPUT_STATE | PropertyPanel.PREF_TABLEUI);
            panel.setSize(320, 28);
            panel.addNotify();
            panel.doLayout();
            try {
                JCheckBox checkbox = findNamed(panel, JCheckBox.class,
                        FlutterPropertyEditorComponents.BOOLEAN_COMPONENT_NAME);
                assertNotNull(checkbox,
                        "NetBeans PropertyPanel must honor ExPropertyEditor.attachEnv");
                assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT,
                        checkbox.getText());
            } finally {
                panel.removeNotify();
            }
            return null;
        });
    }

    @Test
    void realNetBeansPropertyPanelUsesEnumComboAndBoundedNumericField()
            throws Exception {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        WidgetNode widget = new WidgetNode(
                StableId.parse("62913f77-08a1-46eb-91f3-a63dd9c009a6"),
                definition.typeId(),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue("Text")),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });
        Node.Property<?> textAlign = findProperty(node, "textAlign");
        Node.Property<?> maxLines = findProperty(node, "maxLines");

        onEdt(() -> {
            PropertyPanel enumPanel = new PropertyPanel(
                    textAlign,
                    PropertyPanel.PREF_INPUT_STATE | PropertyPanel.PREF_TABLEUI);
            PropertyPanel numericPanel = new PropertyPanel(
                    maxLines,
                    PropertyPanel.PREF_INPUT_STATE | PropertyPanel.PREF_TABLEUI);
            enumPanel.setSize(320, 28);
            numericPanel.setSize(320, 28);
            enumPanel.addNotify();
            numericPanel.addNotify();
            enumPanel.doLayout();
            numericPanel.doLayout();
            try {
                JComboBox<?> combo = findFirst(enumPanel, JComboBox.class);
                assertNotNull(combo);
                assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT,
                        combo.getItemAt(0));
                assertTrue(combo.getItemCount() > 2,
                        "catalog enum values must be offered by the real combo");
                JTextField number = findNamed(
                        numericPanel, JTextField.class,
                        FlutterPropertyEditorComponents.NUMERIC_COMPONENT_NAME);
                assertNotNull(number);
                assertTrue(number.getToolTipText().contains("integer range 1"));
            } finally {
                enumPanel.removeNotify();
                numericPanel.removeNotify();
            }
            return null;
        });
    }

    @Test
    void optionalBooleanUsesAccessibleTriStateCheckboxAndOneTypedCommit()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "softWrap"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();
        FeatureDescriptor descriptor = descriptor(
                "Soft Wrap", "Controls whether text may wrap.");
        PropertyEnv environment = PropertyEnv.create(descriptor);
        AtomicInteger events = new AtomicInteger();

        onEdt(() -> {
            inplace.addActionListener(event -> {
                assertSame(inplace, event.getSource());
                assertEquals(InplaceEditor.COMMAND_SUCCESS,
                        event.getActionCommand());
                events.incrementAndGet();
            });
            inplace.connect(editor, environment);
            JCheckBox checkbox = assertInstanceOf(
                    JCheckBox.class, inplace.getComponent());
            assertEquals(FlutterPropertyEditorComponents.BOOLEAN_COMPONENT_NAME,
                    checkbox.getName());
            assertEquals("Soft Wrap",
                    checkbox.getAccessibleContext().getAccessibleName());
            assertTrue(checkbox.getAccessibleContext()
                    .getAccessibleDescription().contains("default"));
            assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT,
                    checkbox.getText());

            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(true)),
                    inplace.getValue());
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(false)),
                    inplace.getValue());
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.unset(), inplace.getValue());
            assertEquals(3, events.get());
            inplace.clear();
            assertNull(inplace.getPropertyEditor());
            assertEquals("", checkbox.getText());
            assertFalse(checkbox.isSelected());
            assertNull(checkbox.getClientProperty("JButton.selectedState"));
            return null;
        });
    }

    @Test
    void requiredBooleanNeverProducesUnset() throws Exception {
        PropertyDefinition definition = new PropertyDefinition(
                new PropertyName("enabled"),
                DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.AnyValue(
                        PropertyValueKind.BOOLEAN)),
                java.util.Optional.empty());
        FlutterTypedPropertyEditors.Binding binding = binding(definition);
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.BooleanValue(false)));
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();

        onEdt(() -> {
            inplace.connect(editor, PropertyEnv.create(descriptor(
                    "Enabled", "Required boolean.")));
            JCheckBox checkbox = (JCheckBox) inplace.getComponent();
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(true)),
                    inplace.getValue());
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(false)),
                    inplace.getValue());
            return null;
        });
    }

    @Test
    void boundedIntegerControlRejectsZeroAndCommitsOneExactly()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "maxLines"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Max Lines", "Positive line limit."));
        AtomicInteger commits = new AtomicInteger();

        onEdt(() -> {
            inplace.addActionListener(ignored -> {
                editor.setAsText((String) inplace.getValue());
                commits.incrementAndGet();
            });
            inplace.connect(editor, environment);
            JTextField field = assertInstanceOf(
                    JTextField.class, inplace.getComponent());
            assertEquals(FlutterPropertyEditorComponents.NUMERIC_COMPONENT_NAME,
                    field.getName());
            assertTrue(field.getAccessibleContext()
                    .getAccessibleDescription().contains("integer range 1"));

            field.setText("0");
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(0, commits.get());
            assertEquals("0", inplace.getValue(),
                    "InplaceEditor must expose the invalid draft so NetBeans can reject it");
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());

            field.setText("3");
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_VALID, environment.getState());
            assertEquals(1, commits.get());
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.IntegerValue(BigInteger.valueOf(3))),
                    editor.getValue());

            field.setText(DartNumericLiterals.MAX_PORTABLE_INTEGER.toString());
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_VALID, environment.getState());
            assertEquals(2, commits.get());
            field.setText(DartNumericLiterals.MAX_PORTABLE_INTEGER
                    .add(BigInteger.ONE).toString());
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(2, commits.get());
            return null;
        });
    }

    @Test
    void doubleControlRetainsDoubleKindAndNonNegativeConstraint()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Column", "spacing"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Spacing", "Non-negative spacing."));

        onEdt(() -> {
            inplace.addActionListener(ignored ->
                    editor.setAsText((String) inplace.getValue()));
            inplace.connect(editor, environment);
            JTextField field = (JTextField) inplace.getComponent();
            field.setText("2");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                    editor.getValue());
            field.setText("-0.01");
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("-0.01", inplace.getValue());
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                    editor.getValue(),
                    "an invalid draft must not replace the last valid value");
            return null;
        });
    }

    @Test
    void nullableNumberAcceptsZeroAndRestoresUnsetWithoutChangingItsKind()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Center", "widthFactor"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.DoubleValue(BigDecimal.ONE)));
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();

        onEdt(() -> {
            inplace.addActionListener(ignored ->
                    editor.setAsText((String) inplace.getValue()));
            inplace.connect(editor, PropertyEnv.create(descriptor(
                    "Width Factor", "Optional non-negative factor.")));
            JTextField field = (JTextField) inplace.getComponent();

            field.setText("0");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.IntegerValue(BigInteger.ZERO)),
                    editor.getValue());
            field.setText("0.5");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.DoubleValue(new BigDecimal("0.5"))),
                    editor.getValue());
            field.setText("");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
            return null;
        });
    }

    @Test
    void numericEditorClearRemovesInvalidDraftBeforeReuse() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "maxLines"));
        PropertyEditor firstEditor = binding.createEditor();
        firstEditor.setValue(FlutterPropertyCellValue.unset());
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();

        onEdt(() -> {
            inplace.connect(firstEditor, PropertyEnv.create(descriptor(
                    "Max Lines", "Positive line limit.")));
            JTextField field = (JTextField) inplace.getComponent();
            field.setText("0");
            assertEquals("error", field.getClientProperty("JComponent.outline"));

            inplace.clear();
            assertNull(inplace.getPropertyEditor());
            assertEquals("", field.getText());
            assertNull(field.getClientProperty("JComponent.outline"));
            assertNull(field.getToolTipText());
            assertNull(field.getAccessibleContext().getAccessibleName());

            PropertyEditor secondEditor = binding.createEditor();
            secondEditor.setValue(FlutterPropertyCellValue.explicit(
                    new PropertyValue.IntegerValue(BigInteger.valueOf(4))));
            inplace.connect(secondEditor, PropertyEnv.create(descriptor(
                    "Max Lines", "Positive line limit.")));
            assertEquals("4", field.getText());
            inplace.setValue("7");
            assertEquals("7", inplace.getValue(),
                    "setValue must accept every type returned by getValue");
            return null;
        });
    }

    @Test
    void colorChooserIsTransactionalAndPreservesExactAlpha() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "selectionColor"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                PropertyValue.ColorValue.fromWireArgb("0x80112233"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Selection Color", "Exact Flutter ARGB selection color."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JColorChooser chooser = findNamed(panel, JColorChooser.class,
                    FlutterPropertyEditorComponents.COLOR_CHOOSER_NAME);
            JSpinner alpha = findNamed(panel, JSpinner.class,
                    FlutterPropertyEditorComponents.COLOR_ALPHA_NAME);
            JTextField argb = findNamed(panel, JTextField.class,
                    FlutterPropertyEditorComponents.COLOR_ARGB_NAME);
            assertNotNull(chooser);
            assertNotNull(alpha);
            assertNotNull(argb);
            JTextField alphaText = ((JSpinner.DefaultEditor) alpha.getEditor())
                    .getTextField();
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    environment.getState());

            chooser.setColor(new Color(0xAA, 0xBB, 0xCC));
            alphaText.setText("64");
            assertEquals(initial, editor.getValue(),
                    "chooser preview events must remain a local draft");
            assertEquals(0, committedChanges.get());
            assertEquals("0x40AABBCC", argb.getText());

            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                    PropertyValue.ColorValue.fromWireArgb("0x40AABBCC")),
                    editor.getValue());
            assertEquals(1, committedChanges.get());
            environment.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(1, committedChanges.get(),
                    "one accepted dialog may publish exactly one value");

            argb.setText("#AABBCC");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(
                            PropertyValue.ColorValue.fromWireArgb("0x40AABBCC")),
                    editor.getValue());

            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)");
            assertNotNull(useDefault);
            useDefault.doClick();
            assertFalse(argb.isEnabled());
            assertNull(argb.getClientProperty("JComponent.outline"));
            assertTrue(argb.getAccessibleContext()
                    .getAccessibleDescription().contains("0xAARRGGBB"));
            return null;
        });
    }

    @Test
    void manuallyTypedColorAlphaValidatesBoundsAndRecoversBeforeCommit()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "selectionColor"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                PropertyValue.ColorValue.fromWireArgb("0xFF112233"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Selection Color", "Exact Flutter ARGB selection color."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JColorChooser chooser = findNamed(panel, JColorChooser.class,
                    FlutterPropertyEditorComponents.COLOR_CHOOSER_NAME);
            JSpinner alpha = findNamed(panel, JSpinner.class,
                    FlutterPropertyEditorComponents.COLOR_ALPHA_NAME);
            assertNotNull(chooser);
            assertNotNull(alpha);
            JTextField alphaText = ((JSpinner.DefaultEditor) alpha.getEditor())
                    .getTextField();

            alphaText.setText("999");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error",
                    alphaText.getClientProperty("JComponent.outline"));
            assertEquals(initial, editor.getValue());

            chooser.setColor(new Color(0x44, 0x55, 0x66));
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState(),
                    "RGB changes must not hide an invalid alpha draft");
            alphaText.setText("0");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    environment.getState());
            assertNull(alphaText.getClientProperty("JComponent.outline"));

            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            PropertyValue.ColorValue.fromWireArgb("0x00445566")),
                    editor.getValue());
            return null;
        });
    }

    @Test
    void edgeInsetsDialogCommitsFourSidesAtomicallyAndRejectsNegative()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Padding", "padding"));
        PropertyEditor editor = binding.createEditor();
        PropertyValue.EdgeInsetsValue initialInsets = edge(16, 16, 16, 16);
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                initialInsets);
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Padding", "Physical non-negative edge insets."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTextField left = findNamed(panel, JTextField.class,
                    "flutter.edgeInsets.left");
            JTextField top = findNamed(panel, JTextField.class,
                    "flutter.edgeInsets.top");
            JTextField right = findNamed(panel, JTextField.class,
                    "flutter.edgeInsets.right");
            JTextField bottom = findNamed(panel, JTextField.class,
                    "flutter.edgeInsets.bottom");
            assertNotNull(left);
            assertNotNull(top);
            assertNotNull(right);
            assertNotNull(bottom);
            JTextField all = findNamed(panel, JTextField.class,
                    FlutterPropertyEditorComponents.EDGE_ALL_NAME);
            JButton applyAll = findFirst(panel, JButton.class);
            assertNotNull(all);
            assertNotNull(applyAll);

            top.setText("-1");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", top.getClientProperty("JComponent.outline"));
            all.setText("8");
            applyAll.doClick();
            for (JTextField field : List.of(left, top, right, bottom)) {
                assertEquals("8", field.getText());
                assertNull(field.getClientProperty("JComponent.outline"));
                assertFalse(field.getAccessibleContext()
                        .getAccessibleDescription().startsWith("Invalid"));
            }
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    environment.getState());

            left.setText("1");
            top.setText("2.5");
            right.setText("3");
            bottom.setText("4.25");
            assertEquals(initial, editor.getValue(),
                    "field document events must not partially publish an inset");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.EdgeInsetsValue(
                                    BigDecimal.ONE,
                                    new BigDecimal("2.5"),
                                    BigDecimal.valueOf(3),
                                    new BigDecimal("4.25"))),
                    editor.getValue());

            top.setText("-1");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.EdgeInsetsValue(
                                    BigDecimal.ONE,
                                    new BigDecimal("2.5"),
                                    BigDecimal.valueOf(3),
                                    new BigDecimal("4.25"))),
                    editor.getValue(),
                    "an invalid four-field draft must never commit");
            return null;
        });
    }

    @Test
    void optionalStringCustomEditorKeepsLiteralNotSetDistinctFromReset()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "semanticsLabel"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Semantics Label", "Optional accessibility text."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)");
            JTextArea text = findNamed(panel, JTextArea.class,
                    "flutter.string.text");
            assertNotNull(useDefault);
            assertNotNull(text);
            assertTrue(useDefault.isSelected());
            assertFalse(text.isEnabled());

            useDefault.doClick();
            text.setText(FlutterPropertyCellValue.NOT_SET_TEXT);
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.StringValue(
                                    FlutterPropertyCellValue.NOT_SET_TEXT)),
                    editor.getValue());
            return null;
        });
    }

    private static PropertyDefinition property(String widgetType, String name) {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId(widgetType)).orElseThrow();
        return definition.properties().stream()
                .filter(property -> property.name().value().equals(name))
                .findFirst().orElseThrow();
    }

    private static Node.Property<?> findProperty(
            FlutterWidgetPropertiesNode node, String name) {
        return java.util.Arrays.stream(node.getPropertySets()[1].getProperties())
                .filter(property -> property.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private static FlutterTypedPropertyEditors.Binding binding(
            PropertyDefinition definition) {
        return FlutterTypedPropertyEditors.binding(definition).orElseThrow();
    }

    private static FeatureDescriptor descriptor(
            String displayName, String description) {
        FeatureDescriptor descriptor = new FeatureDescriptor();
        descriptor.setName(displayName.replace(' ', '_'));
        descriptor.setDisplayName(displayName);
        descriptor.setShortDescription(description);
        return descriptor;
    }

    private static PropertyValue.EdgeInsetsValue edge(
            int left, int top, int right, int bottom) {
        return new PropertyValue.EdgeInsetsValue(
                BigDecimal.valueOf(left), BigDecimal.valueOf(top),
                BigDecimal.valueOf(right), BigDecimal.valueOf(bottom));
    }

    private static <T extends Component> T findNamed(
            Component root, Class<T> type, String name) {
        if (type.isInstance(root) && name.equals(root.getName())) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findNamed(child, type, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findFirst(
            Component root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findFirst(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findByText(
            Component root, Class<T> type, String text) {
        if (type.isInstance(root)
                && root instanceof JCheckBox checkBox
                && text.equals(checkBox.getText())) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findByText(child, type, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T onEdt(Callable<T> operation) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return operation.call();
        }
        FutureTask<T> task = new FutureTask<>(operation);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
