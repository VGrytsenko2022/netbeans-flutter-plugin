package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.util.List;
import java.util.Optional;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.openide.explorer.propertysheet.PropertyEnv;

/** One transactional union editor; nested editors never publish to the model. */
final class FlutterShapeBorderClipperEditorComponent {
    static final String MODE_NAME = "flutter.shapeBorderClipper.mode";
    static final String SHAPE_NAME = "flutter.shapeBorderClipper.shape";
    static final String DIRECTION_NAME = "flutter.shapeBorderClipper.direction";
    static final String NOTE_NAME = "flutter.shapeBorderClipper.note";
    private static final String BUILTIN = "Built-in ShapeBorderClipper";
    private static final String PROJECT = "Project CustomClipper<Path>";

    private FlutterShapeBorderClipperEditorComponent() { }

    static Component customEditor(PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new ClipperPanel(editor, binding, environment);
    }

    private static final class ClipperPanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode = new JComboBox<>(new String[]{BUILTIN, PROJECT});
        private final JComboBox<PropertyValue.ShapeBorderClipperValue.Shape> shape =
                new JComboBox<>(PropertyValue.ShapeBorderClipperValue.Shape.values());
        private final JComboBox<String> direction =
                new JComboBox<>(new String[]{"Not specified", "Left to right", "Right to left"});
        private final CardLayout layout = new CardLayout();
        private final JPanel cards = new JPanel(layout);
        private final JTextArea note = new JTextArea(3, 55);
        private final FlutterPropertyEditorComponents.CommitOnValidPanel radiusPanel;
        private final FlutterPropertyEditorComponents.CommitOnValidPanel referencePanel;
        private boolean refreshing;

        ClipperPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(720, 530));
            setName("flutter.shapeBorderClipper.editor");
            getAccessibleContext().setAccessibleName("PhysicalShape clipper editor");
            getAccessibleContext().setAccessibleDescription(
                    "Required CustomClipper<Path>: a typed built-in shape or an analyzer-verified project reference. "
                    + "All changes remain local until OK; cancel leaves the original value unchanged.");
            named(mode, MODE_NAME, "Clipper source");
            named(shape, SHAPE_NAME, "Built-in clipping shape");
            shape.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list,
                        Object value, int index, boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    if (value instanceof PropertyValue.ShapeBorderClipperValue.Shape choice) {
                        setText(switch (choice) {
                            case ROUNDED_RECTANGLE -> "Rounded rectangle";
                            case BEVELED_RECTANGLE -> "Beveled rectangle";
                            case CONTINUOUS_RECTANGLE -> "Continuous rectangle";
                            case ROUNDED_SUPERELLIPSE -> "Rounded superellipse";
                            case CIRCLE -> "Circle";
                            case STADIUM -> "Stadium";
                        });
                    }
                    return this;
                }
            });
            named(direction, DIRECTION_NAME, "Explicit shape text direction");
            JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEADING));
            addLabeled(heading, "Source:", mode);
            add(heading, BorderLayout.NORTH);
            PropertyValue initial = initialValue().explicitValue().orElseThrow();
            PropertyValue.ShapeBorderClipperValue preset = initial
                    instanceof PropertyValue.ShapeBorderClipperValue value
                    ? value : PropertyValue.ShapeBorderClipperValue.defaultValue();
            PropertyDefinition radiusDefinition = new PropertyDefinition(
                    new PropertyName("borderRadius"), DartParameter.named(0, true),
                    List.of(new PropertyValueConstraint.BorderRadiusValues(true)), Optional.empty());
            var radiusBinding = FlutterTypedPropertyEditors.binding(radiusDefinition).orElseThrow();
            var radiusEditor = radiusBinding.createEditor();
            radiusEditor.setValue(FlutterPropertyCellValue.explicit(
                    new PropertyValue.BorderRadiusValue(preset.borderRadius())));
            PropertyEnv radiusEnv = PropertyEnv.create(new FeatureDescriptor());
            radiusPanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterContainerPropertyEditorComponents.customEditor(radiusEditor, radiusBinding, radiusEnv);
            JPanel builtin = new JPanel(new BorderLayout(0, 8));
            JPanel options = new JPanel(new FlowLayout(FlowLayout.LEADING));
            addLabeled(options, "Shape:", shape);
            addLabeled(options, "Text direction:", direction);
            builtin.add(options, BorderLayout.NORTH);
            builtin.add(radiusPanel, BorderLayout.CENTER);
            cards.add(builtin, BUILTIN);

            PropertyDefinition referenceDefinition = new PropertyDefinition(
                    binding.definition().name(), DartParameter.named(0, true),
                    List.of(new PropertyValueConstraint.DartObjectReferenceValues("CustomClipper<Path>")),
                    Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow();
            var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial
                    instanceof PropertyValue.DartObjectReferenceValue reference ? reference
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_clipper",
                            Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                            Optional.empty())));
            PropertyEnv referenceEnv = PropertyEnv.create(new FeatureDescriptor());
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterDartObjectReferenceEditorComponent.customEditor(
                            referenceEditor, referenceBinding, referenceEnv);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) {
                clearRoot(referencePanel);
            }
            cards.add(referencePanel, PROJECT);
            add(cards, BorderLayout.CENTER);
            note.setName(NOTE_NAME);
            note.setEditable(false);
            note.setOpaque(false);
            note.setLineWrap(true);
            note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Clipper geometry and Canvas preview note");
            add(note, BorderLayout.SOUTH);
            shape.setSelectedItem(preset.shape());
            direction.setSelectedIndex(preset.textDirection().map(value ->
                    value == PropertyValue.ShapeBorderClipperValue.TextDirection.LTR ? 1 : 2).orElse(0));
            mode.setSelectedIndex(initial instanceof PropertyValue.DartObjectReferenceValue ? 1 : 0);
            mode.addActionListener(ignored -> refresh(true));
            shape.addActionListener(ignored -> refresh(true));
            direction.addActionListener(ignored -> refresh(true));
            radiusEnv.addPropertyChangeListener(ignored -> refresh(true));
            referenceEnv.addPropertyChangeListener(ignored -> refresh(true));
            activate();
            refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            if (refreshing) { return true; }
            refreshing = true;
            try {
                boolean project = mode.getSelectedIndex() == 1;
                layout.show(cards, project ? PROJECT : BUILTIN);
                var selectedShape = (PropertyValue.ShapeBorderClipperValue.Shape) shape.getSelectedItem();
                String text = project
                        ? "The Dart analyzer verifies CustomClipper<Path>. Project code is not executed in the isolated "
                                + "Canvas: custom clip, fill and shadow preview is unavailable. The child remains visible. "
                                + "Use a project getter or factory for configured constructor arguments."
                        : "Built-in geometry is rendered by Flutter. Directional radii require explicit text direction "
                                + "for rounded, beveled, continuous and superellipse shapes. Circle and stadium ignore "
                                + "but preserve the radius settings; changing shape does not delete them.";
                note.setText(text);
                note.getAccessibleContext().setAccessibleDescription(text);
                PropertyValue value;
                if (project) {
                    value = (requestValidation ? referencePanel.stagedDraftValue()
                            : referencePanel.validatedDraftValue()).explicitValue().orElseThrow();
                } else {
                    var radius = (PropertyValue.BorderRadiusValue)
                            (requestValidation ? radiusPanel.stagedDraftValue()
                                    : radiusPanel.validatedDraftValue()).explicitValue().orElseThrow();
                    Optional<PropertyValue.ShapeBorderClipperValue.TextDirection> textDirection =
                            direction.getSelectedIndex() == 0 ? Optional.empty()
                            : Optional.of(direction.getSelectedIndex() == 1
                                    ? PropertyValue.ShapeBorderClipperValue.TextDirection.LTR
                                    : PropertyValue.ShapeBorderClipperValue.TextDirection.RTL);
                    value = new PropertyValue.ShapeBorderClipperValue(
                            selectedShape, radius.geometry(), textDirection);
                }
                clearInvalid(note, text);
                var candidate = FlutterPropertyCellValue.explicit(value);
                if (requestValidation) { markValid(candidate); } else { stageValid(candidate); }
                return true;
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), note);
                return false;
            } finally { refreshing = false; }
        }

        private static void clearRoot(Container parent) {
            for (Component component : parent.getComponents()) {
                if (component instanceof JTextField field
                        && FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME.equals(field.getName())) {
                    field.setText("");
                } else if (component instanceof Container child) { clearRoot(child); }
            }
        }

        private static void named(JComboBox<?> component, String name, String label) {
            component.setName(name);
            component.getAccessibleContext().setAccessibleName(label);
        }

        private static void addLabeled(JPanel panel, String text, Component component) {
            JLabel label = new JLabel(text);
            label.setLabelFor(component);
            panel.add(label);
            panel.add(component);
        }
    }
}
