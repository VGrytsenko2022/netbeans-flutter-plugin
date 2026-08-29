package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DartNumericLiterals;
import dev.flutter.netbeans.designer.catalog.MaterialThemeTokenCatalog;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTable;
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
        Node.Property<?> softWrap = findProperty(node, "softWrap");

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
                assertEquals("", number.getText(),
                        "an unset numeric value must enter edit mode as an empty draft");
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
            assertEquals("", field.getText());
            assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT, editor.getAsText(),
                    "the inactive property presentation must remain <not set>");
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
    void nullableDoubleAcceptsZeroAndRestoresUnsetWithoutChangingItsKind()
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
                            new PropertyValue.DoubleValue(BigDecimal.ZERO)),
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
            assertEquals("", field.getText());
            firstEditor.setValue(FlutterPropertyCellValue.explicit(
                    new PropertyValue.IntegerValue(BigInteger.valueOf(4))));
            inplace.reset();
            assertEquals("4", field.getText());
            firstEditor.setValue(FlutterPropertyCellValue.unset());
            inplace.reset();
            assertEquals("", field.getText(),
                    "resetting an active numeric editor to default must clear its draft");
            field.setText("0");
            assertEquals("error", field.getClientProperty("JComponent.outline"));

            inplace.clear();
            assertNull(inplace.getPropertyEditor());
            assertEquals("", field.getText());
            assertNull(field.getClientProperty("JComponent.outline"));
            assertNull(field.getToolTipText());
            assertNull(field.getAccessibleContext().getAccessibleName());

            PropertyEditor secondEditor = binding.createEditor();
            secondEditor.setValue(FlutterPropertyCellValue.unset());
            inplace.connect(secondEditor, PropertyEnv.create(descriptor(
                    "Max Lines", "Positive line limit.")));
            assertEquals("", field.getText(),
                    "a reused editor must not restore the presentation placeholder");
            secondEditor.setValue(FlutterPropertyCellValue.explicit(
                    new PropertyValue.IntegerValue(BigInteger.valueOf(4))));
            inplace.reset();
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

            JComboBox<?> sourceMode = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_MODE_NAME);
            assertNotNull(sourceMode);
            sourceMode.setSelectedItem("Not set");
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
    void edgeInsetsDialogExposesStableComponentsAndRestoresEveryPaddingMode()
            throws Exception {
        assertEdgeInsetsDialogRoundTrip(edge("8", "8", "8", "8"), "All sides");
        assertEdgeInsetsDialogRoundTrip(edge("4", "8", "4", "8"), "Symmetric");
        assertEdgeInsetsDialogRoundTrip(edge("1", "2", "3", "4"),
                "Physical (left/right)");
        assertEdgeInsetsDialogRoundTrip(directionalEdge("1", "2", "3", "4"),
                "Directional (start/end)");
    }

    @Test
    void edgeInsetsDialogCommitsAllSymmetricPhysicalAndDirectionalAtomically()
            throws Exception {
        assertEdgeInsetsDialogCommit(
                "All sides",
                Map.of(FlutterPropertyEditorComponents.EDGE_ALL_NAME, "8.5"),
                edge("8.5", "8.5", "8.5", "8.5"));
        assertEdgeInsetsDialogCommit(
                "Symmetric",
                Map.of(
                        "flutter.edgeInsets.horizontal", "3.25",
                        "flutter.edgeInsets.vertical", "7.5"),
                edge("3.25", "7.5", "3.25", "7.5"));
        assertEdgeInsetsDialogCommit(
                "Physical (left/right)",
                Map.of(
                        "flutter.edgeInsets.left", "1",
                        "flutter.edgeInsets.top", "2.5",
                        "flutter.edgeInsets.right", "3",
                        "flutter.edgeInsets.bottom", "4.25"),
                edge("1", "2.5", "3", "4.25"));
        assertEdgeInsetsDialogCommit(
                "Directional (start/end)",
                Map.of(
                        "flutter.edgeInsets.directional.start", "5",
                        "flutter.edgeInsets.directional.top", "6.5",
                        "flutter.edgeInsets.directional.end", "7",
                        "flutter.edgeInsets.directional.bottom", "8.25"),
                directionalEdge("5", "6.5", "7", "8.25"));
    }

    @Test
    void edgeInsetsDialogRejectsMalformedAndNegativeDraftsInEveryMode()
            throws Exception {
        assertEdgeInsetsDialogInvalid(
                "All sides", FlutterPropertyEditorComponents.EDGE_ALL_NAME, "not-a-number");
        assertEdgeInsetsDialogInvalid(
                "Symmetric", "flutter.edgeInsets.horizontal", "-1");
        assertEdgeInsetsDialogInvalid(
                "Physical (left/right)", "flutter.edgeInsets.top", "");
        assertEdgeInsetsDialogInvalid(
                "Directional (start/end)",
                "flutter.edgeInsets.directional.end", "-0.25");
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

    @Test
    void fontFallbackCustomEditorKeepsOneFamilyPerLineAndCommitsAtomically()
            throws Exception {
        PropertyDefinition definition = property(
                "flutter.widgets.Text", "styleFontFamilyFallback");
        FlutterTypedPropertyEditors.Binding binding = FlutterTypedPropertyEditors
                .binding(definition, TextWidgetPropertySchema.find(definition.name()))
                .orElseThrow();
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue("Roboto\nNoto Sans"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Font fallbacks", "Fallback font families, one per line."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTextArea text = findNamed(panel, JTextArea.class,
                    FlutterPropertyEditorComponents.NEWLINE_LIST_TEXT_NAME);
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)");
            assertNotNull(text);
            assertNotNull(useDefault);
            assertFalse(useDefault.isSelected());
            assertTrue(text.isEnabled());
            assertEquals("Roboto\nNoto Sans", text.getText());

            text.setText(" Inter \r\n\r\n Noto Color Emoji ");
            assertEquals(initial, editor.getValue(),
                    "font-list document events must remain a local draft");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.StringValue(
                                    "Inter\nNoto Color Emoji")),
                    editor.getValue());
            return null;
        });
    }

    @Test
    void themeAwareColorOffersTypedModesAndCommitsOnlyOnDialogOk()
            throws Exception {
        PropertyDefinition definition = new PropertyDefinition(
                new PropertyName("styleColor"),
                DartParameter.named(0, false),
                List.of(
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
                        new PropertyValueConstraint.ThemeTokenValues(List.of(
                                "material.colorScheme.primary",
                                "material.colorScheme.error"))),
                java.util.Optional.empty());
        FlutterTypedPropertyEditors.Binding binding = binding(definition);
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                PropertyValue.ColorValue.fromWireArgb("0xFF112233"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Color", "Literal or Material ColorScheme role."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger commits = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> mode = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_MODE_NAME);
            JComboBox<?> role = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_ROLE_NAME);
            assertNotNull(mode);
            assertNotNull(role);
            assertEquals(2, role.getItemCount());
            assertEquals("Primary", role.getItemAt(0));
            assertEquals("Error", role.getItemAt(1));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("semantic Material ColorScheme role"));

            mode.setSelectedItem("Theme role");
            role.setSelectedItem("Primary");
            assertEquals(initial, editor.getValue(),
                    "theme selection remains a local dialog draft");
            assertEquals(0, commits.get());

            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                    new PropertyValue.ThemeTokenValue(
                            new ThemeToken("material.colorScheme.primary"))),
                    editor.getValue());
            assertEquals(1, commits.get());
            environment.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(1, commits.get(), "one OK publishes one immutable value");
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(initial);
        PropertyEnv cancelEnvironment = PropertyEnv.create(descriptor(
                "Color", "Literal or Material ColorScheme role."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelEnvironment);
        onEdt(() -> {
            Component panel = cancelled.getCustomEditor();
            JComboBox<?> mode = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_MODE_NAME);
            JComboBox<?> role = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_ROLE_NAME);
            mode.setSelectedItem("Theme role");
            role.setSelectedItem("Error");
            // A NetBeans Cancel closes the custom editor without STATE_VALID.
            assertEquals(initial, cancelled.getValue());
            return null;
        });
    }

    @Test
    void textThemeRoleUsesAClosedAccessibleComboAndSupportsReset() {
        PropertyDefinition subsetDefinition = new PropertyDefinition(
                new PropertyName("styleThemeTextStyle"),
                DartParameter.named(0, false),
                List.of(new PropertyValueConstraint.ThemeTokenValues(List.of(
                        "material.textTheme.bodyMedium",
                        "material.textTheme.titleLarge"))),
                java.util.Optional.empty());
        FlutterTypedPropertyEditors.Binding binding = binding(subsetDefinition);
        PropertyEditor editor = binding.createEditor();

        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT,
                "Body Medium", "Title Large"), List.of(editor.getTags()));
        editor.setAsText("Body Medium");
        assertEquals(FlutterPropertyCellValue.explicit(
                new PropertyValue.ThemeTokenValue(
                        new ThemeToken("material.textTheme.bodyMedium"))),
                editor.getValue());
        editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT);
        assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("madeUpRole"));
    }

    @Test
    void paintEditorCommitsTheCompleteStructuredDraftAndCanBeEditedAgain()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(complexProperty(
                "styleForeground", PropertyValueKind.PAINT));
        PropertyValue.PaintValue initialPaint = PropertyValue.PaintValue.defaults(
                new ColorSource.Literal(0xFF010203L));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initialPaint));
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Foreground paint", "Structured dart:ui Paint."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> style = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.PAINT_STYLE_NAME);
            JTextField width = findNamed(panel, JTextField.class,
                    FlutterComplexPropertyEditorComponents.PAINT_STROKE_WIDTH_NAME);
            assertNotNull(style);
            assertNotNull(width);
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("atomically"));

            style.setSelectedItem("stroke");
            width.setText("1E+400");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(initialPaint),
                    editor.getValue());
            width.setText("2.5");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(initialPaint),
                    editor.getValue());
            environment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.PaintValue accepted = assertInstanceOf(
                    PropertyValue.PaintValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(PropertyValue.PaintValue.Style.STROKE, accepted.style());
            assertEquals(new BigDecimal("2.5"), accepted.strokeWidth());
            return null;
        });

        PropertyEditor repeated = binding.createEditor();
        repeated.setValue(editor.getValue());
        PropertyEnv repeatedEnvironment = PropertyEnv.create(descriptor(
                "Foreground paint", "Structured dart:ui Paint."));
        ((ExPropertyEditor) repeated).attachEnv(repeatedEnvironment);
        onEdt(() -> {
            Component panel = repeated.getCustomEditor();
            assertEquals("stroke", findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.PAINT_STYLE_NAME)
                    .getSelectedItem());
            assertEquals("2.5", findNamed(panel, JTextField.class,
                    FlutterComplexPropertyEditorComponents.PAINT_STROKE_WIDTH_NAME)
                    .getText());
            return null;
        });
    }

    @Test
    void shadowTableIsOrderedTypedAndTransactional() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(complexProperty(
                "styleShadows", PropertyValueKind.SHADOW_LIST));
        PropertyValue.ShadowListValue initialValue = new PropertyValue.ShadowListValue(
                List.of(new PropertyValue.ShadowListValue.Shadow(
                        StableId.parse("f0bf1d8a-c780-4654-9770-91d53600e770"),
                        new ColorSource.Theme(new ThemeToken(
                                "material.colorScheme.shadow")),
                        BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3))));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(initialValue);
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Shadows", "Ordered typed shadows."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTable table = findNamed(panel, JTable.class,
                    FlutterComplexPropertyEditorComponents.SHADOW_TABLE_NAME);
            assertNotNull(table);
            assertTrue(table.getAccessibleContext().getAccessibleDescription()
                    .contains("Color accepts"));
            assertNotNull(findButton(panel, "Add"));
            assertNotNull(findButton(panel, "Remove"));
            assertNotNull(findButton(panel, "Up"));
            assertNotNull(findButton(panel, "Down"));

            table.getModel().setValueAt("1E-400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            table.getModel().setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertTrue(table.editCellAt(0, 3));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("-1");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(initial, editor.getValue());
            assertTrue(table.editCellAt(0, 3));
            JTextField validActiveCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            validActiveCell.setText("4.5");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_VALID, environment.getState(),
                    "a valid active cell must commit with one OK validation");
            PropertyValue.ShadowListValue accepted = assertInstanceOf(
                    PropertyValue.ShadowListValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(new BigDecimal("4.5"), accepted.items().getFirst().blurRadius());
            assertInstanceOf(ColorSource.Theme.class,
                    accepted.items().getFirst().color());
            return null;
        });
    }

    @Test
    void featureAndVariationTablesValidateTagsRangesAndExposePresets()
            throws Exception {
        PropertyValue.FontFeatureListValue featureValue =
                new PropertyValue.FontFeatureListValue(List.of(
                        new PropertyValue.FontFeatureListValue.FontFeature(
                                StableId.parse("2f41fc98-28a5-4698-972e-c7f3c2c53b38"),
                                "liga", 1)));
        PropertyEditor featureEditor = binding(complexProperty(
                "styleFontFeatures", PropertyValueKind.FONT_FEATURE_LIST)).createEditor();
        featureEditor.setValue(FlutterPropertyCellValue.explicit(featureValue));
        PropertyEnv featureEnvironment = PropertyEnv.create(descriptor(
                "Font features", "OpenType features."));
        ((ExPropertyEditor) featureEditor).attachEnv(featureEnvironment);

        onEdt(() -> {
            Component panel = featureEditor.getCustomEditor();
            JTable table = findNamed(panel, JTable.class,
                    FlutterComplexPropertyEditorComponents.FONT_FEATURE_TABLE_NAME);
            assertNotNull(findNamed(panel, JComboBox.class,
                    "flutter.fontFeatures.preset"));
            table.getModel().setValueAt("bad", 0, 0);
            assertEquals(PropertyEnv.STATE_INVALID, featureEnvironment.getState());
            table.getModel().setValueAt("smcp", 0, 0);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    featureEnvironment.getState());
            featureEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_VALID, featureEnvironment.getState(),
                    "an idle valid table must remain valid after OK");
            PropertyValue.FontFeatureListValue accepted = assertInstanceOf(
                    PropertyValue.FontFeatureListValue.class,
                    ((FlutterPropertyCellValue) featureEditor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals("smcp", accepted.items().getFirst().tag());
            return null;
        });

        PropertyValue.FontVariationListValue variationValue =
                new PropertyValue.FontVariationListValue(List.of(
                        new PropertyValue.FontVariationListValue.FontVariation(
                                StableId.parse("e3b1b676-ed66-4caf-94c5-fbe6b19ff8b1"),
                                "wght", BigDecimal.valueOf(400))));
        PropertyEditor variationEditor = binding(complexProperty(
                "styleFontVariations", PropertyValueKind.FONT_VARIATION_LIST)).createEditor();
        variationEditor.setValue(FlutterPropertyCellValue.explicit(variationValue));
        PropertyEnv variationEnvironment = PropertyEnv.create(descriptor(
                "Font variations", "Variable font axes."));
        ((ExPropertyEditor) variationEditor).attachEnv(variationEnvironment);

        onEdt(() -> {
            Component panel = variationEditor.getCustomEditor();
            JTable table = findNamed(panel, JTable.class,
                    FlutterComplexPropertyEditorComponents.FONT_VARIATION_TABLE_NAME);
            assertNotNull(findNamed(panel, JComboBox.class,
                    "flutter.fontVariations.preset"));
            table.getModel().setValueAt("700.000000000000000001", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, variationEnvironment.getState());
            table.getModel().setValueAt("0", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, variationEnvironment.getState());
            table.getModel().setValueAt("700", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    variationEnvironment.getState());
            variationEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_VALID, variationEnvironment.getState(),
                    "a valid variation table must remain valid after OK");
            PropertyValue.FontVariationListValue accepted = assertInstanceOf(
                    PropertyValue.FontVariationListValue.class,
                    ((FlutterPropertyCellValue) variationEditor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(0, BigDecimal.valueOf(700).compareTo(
                    accepted.items().getFirst().value()));
            return null;
        });
    }

    private static void assertEdgeInsetsDialogRoundTrip(
            PropertyValue initialInsets,
            String expectedMode) throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Padding", "padding"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(initialInsets);
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Padding", "Non-negative physical or directional edge insets."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertEquals("flutter.edgeInsets.custom", panel.getName());
            JComboBox<?> mode = findNamed(
                    panel, JComboBox.class, "flutter.edgeInsets.mode");
            assertNotNull(mode);
            assertEquals(List.of(
                    "All sides",
                    "Symmetric",
                    "Physical (left/right)",
                    "Directional (start/end)"), comboLabels(mode));
            assertEquals(expectedMode, String.valueOf(mode.getSelectedItem()));

            for (String name : List.of(
                    FlutterPropertyEditorComponents.EDGE_ALL_NAME,
                    "flutter.edgeInsets.horizontal",
                    "flutter.edgeInsets.vertical",
                    "flutter.edgeInsets.left",
                    "flutter.edgeInsets.top",
                    "flutter.edgeInsets.right",
                    "flutter.edgeInsets.bottom",
                    "flutter.edgeInsets.directional.start",
                    "flutter.edgeInsets.directional.top",
                    "flutter.edgeInsets.directional.end",
                    "flutter.edgeInsets.directional.bottom")) {
                assertNotNull(findNamed(panel, JTextField.class, name), name);
            }
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(initial, editor.getValue(),
                    "opening and accepting the " + expectedMode
                    + " editor must preserve its exact typed value");
            return null;
        });
    }

    private static void assertEdgeInsetsDialogCommit(
            String modeLabel,
            Map<String, String> fieldValues,
            PropertyValue expected) throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Padding", "padding"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                edge("16", "16", "16", "16"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Padding", "Non-negative physical or directional edge insets."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> mode = findNamed(
                    panel, JComboBox.class, "flutter.edgeInsets.mode");
            assertNotNull(mode);
            selectLabel(mode, modeLabel);
            for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
                JTextField field = findNamed(
                        panel, JTextField.class, entry.getKey());
                assertNotNull(field, entry.getKey());
                field.setText(entry.getValue());
            }

            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(initial, editor.getValue(),
                    "document events must keep the " + modeLabel + " value as a local draft");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
            return null;
        });
    }

    private static void assertEdgeInsetsDialogInvalid(
            String modeLabel,
            String fieldName,
            String invalidText) throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Padding", "padding"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                edge("16", "16", "16", "16"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Padding", "Non-negative physical or directional edge insets."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> mode = findNamed(
                    panel, JComboBox.class, "flutter.edgeInsets.mode");
            assertNotNull(mode);
            selectLabel(mode, modeLabel);
            JTextField field = findNamed(panel, JTextField.class, fieldName);
            assertNotNull(field, fieldName);
            field.setText(invalidText);

            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", field.getClientProperty("JComponent.outline"));
            assertTrue(field.getAccessibleContext().getAccessibleDescription()
                    .startsWith("Invalid value."));
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(initial, editor.getValue(),
                    "an invalid " + modeLabel + " draft must never commit");
            return null;
        });
    }

    private static List<String> comboLabels(JComboBox<?> combo) {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (int index = 0; index < combo.getItemCount(); index++) {
            labels.add(String.valueOf(combo.getItemAt(index)));
        }
        return List.copyOf(labels);
    }

    private static void selectLabel(JComboBox<?> combo, String label) {
        for (int index = 0; index < combo.getItemCount(); index++) {
            if (label.equals(String.valueOf(combo.getItemAt(index)))) {
                combo.setSelectedIndex(index);
                return;
            }
        }
        throw new AssertionError("Missing combo item " + label);
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
        return java.util.Arrays.stream(node.getPropertySets())
                .flatMap(set -> java.util.Arrays.stream(set.getProperties()))
                .filter(property -> property.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private static FlutterTypedPropertyEditors.Binding binding(
            PropertyDefinition definition) {
        return FlutterTypedPropertyEditors.binding(definition).orElseThrow();
    }

    private static PropertyDefinition complexProperty(
            String name, PropertyValueKind... kinds) {
        List<PropertyValueConstraint> constraints;
        Set<PropertyValueKind> shape = Set.of(kinds);
        List<String> colors = MaterialThemeTokenCatalog.colorRoles()
                .keySet().stream().sorted().toList();
        if (shape.equals(Set.of(
                PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN))) {
            constraints = List.of(
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
                    new PropertyValueConstraint.ThemeTokenValues(colors));
        } else if (shape.equals(Set.of(PropertyValueKind.THEME_TOKEN))) {
            constraints = List.of(new PropertyValueConstraint.ThemeTokenValues(
                    MaterialThemeTokenCatalog.textStyleRoles().keySet().stream()
                            .sorted().toList()));
        } else if (shape.equals(Set.of(PropertyValueKind.PAINT))) {
            constraints = List.of(new PropertyValueConstraint.PaintValues(colors));
        } else if (shape.equals(Set.of(PropertyValueKind.SHADOW_LIST))) {
            constraints = List.of(new PropertyValueConstraint.ShadowListValues(colors));
        } else if (shape.equals(Set.of(PropertyValueKind.FONT_VARIATION_LIST))) {
            constraints = List.of(new PropertyValueConstraint.FontVariationListValues());
        } else {
            constraints = java.util.Arrays.stream(kinds)
                    .map(kind -> (PropertyValueConstraint)
                    new PropertyValueConstraint.AnyValue(kind)).toList();
        }
        return new PropertyDefinition(
                new PropertyName(name),
                DartParameter.named(0, false),
                constraints,
                java.util.Optional.empty());
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
            String left, String top, String right, String bottom) {
        return new PropertyValue.EdgeInsetsValue(
                new BigDecimal(left), new BigDecimal(top),
                new BigDecimal(right), new BigDecimal(bottom));
    }

    private static PropertyValue.EdgeInsetsDirectionalValue directionalEdge(
            String start, String top, String end, String bottom) {
        return new PropertyValue.EdgeInsetsDirectionalValue(
                new BigDecimal(start), new BigDecimal(top),
                new BigDecimal(end), new BigDecimal(bottom));
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

    private static JButton findButton(Component root, String text) {
        if (root instanceof JButton button && text.equals(button.getText())) {
            return button;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButton(child, text);
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
