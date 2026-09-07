package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.util.Optional;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Exact local-value/reference union; nested editors never publish into the owning cell. */
final class FlutterLocalDartReferenceEditorComponent {
    static final String MODE_NAME = "flutter.localReference.mode";
    static final String OMIT = "Use Flutter default (omit argument)";
    static final String COLOR = "Literal or theme color";
    static final String INSETS = "Physical or directional insets";
    static final String PROJECT = "Project reference";
    private FlutterLocalDartReferenceEditorComponent() { }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new LocalReferencePanel(editor, binding, environment);
    }

    private static final class LocalReferencePanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final String localMode;
        private final CardLayout layout = new CardLayout();
        private final JPanel cards = new JPanel(layout);
        private final JTextArea note = new JTextArea(3, 52);
        private final FlutterPropertyEditorComponents.CommitOnValidPanel localPanel;
        private final FlutterPropertyEditorComponents.CommitOnValidPanel referencePanel;
        private boolean refreshing;

        LocalReferencePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            boolean color = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.COLOR_REFERENCE;
            localMode = color ? COLOR : INSETS;
            mode = new JComboBox<>(binding.optional() ? new String[]{OMIT, localMode, PROJECT} : new String[]{localMode, PROJECT});
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(720, 590));
            setName("flutter.localReference.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " local value or project reference editor");
            getAccessibleContext().setAccessibleDescription("Choose an omitted argument, a typed local value, or a strictly verified project reference. Drafts stay local until OK; Cancel preserves the original value.");
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Value source");
            mode.getAccessibleContext().setAccessibleDescription("Local value and project reference are exclusive; changing source does not commit either draft.");
            var heading = new JPanel(new BorderLayout(8, 0)); var label = new JLabel("Source:"); label.setLabelFor(mode);
            heading.add(label, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            var initial = initialValue().explicitValue().orElse(null);
            var localDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true),
                    binding.definition().constraints().stream().filter(value -> value.kind() != PropertyValueKind.DART_OBJECT_REFERENCE).toList(), Optional.empty());
            var localBinding = FlutterTypedPropertyEditors.binding(localDefinition).orElseThrow();
            var localEditor = localBinding.createEditor();
            PropertyValue initialLocal = initial != null && !(initial instanceof PropertyValue.DartObjectReferenceValue) ? initial
                    : color ? new PropertyValue.ColorValue(0xff000000L)
                    : new PropertyValue.EdgeInsetsValue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            localEditor.setValue(FlutterPropertyCellValue.explicit(initialLocal));
            var localEnvironment = PropertyEnv.create(new FeatureDescriptor());
            localPanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterPropertyEditorComponents.customEditor(localEditor, localBinding, localEnvironment);
            cards.add(localPanel, localMode);
            var referenceDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true),
                    binding.definition().constraints().stream().filter(value -> value.kind() == PropertyValueKind.DART_OBJECT_REFERENCE).toList(), Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow();
            var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.DartObjectReferenceValue ? initial
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_reference", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            var referenceEnvironment = PropertyEnv.create(new FeatureDescriptor());
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnvironment);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearRoot(referencePanel);
            cards.add(referencePanel, PROJECT); cards.add(new JPanel(), OMIT); add(cards, BorderLayout.CENTER);
            note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Local value and reference behavior"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : localMode
                    : initial instanceof PropertyValue.DartObjectReferenceValue ? PROJECT : localMode);
            mode.addActionListener(ignored -> refresh(true));
            localEnvironment.addPropertyChangeListener(ignored -> refresh(true));
            referenceEnvironment.addPropertyChangeListener(ignored -> refresh(true));
            activate(); refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            if (refreshing) return true;
            refreshing = true;
            try {
                String selected = (String) mode.getSelectedItem(); layout.show(cards, selected);
                String text = PROJECT.equals(selected)
                        ? "Stores a strict typed project reference or factory. The analyzer verifies the exact required type; isolated Canvas never executes project code."
                        : OMIT.equals(selected) ? "Omits this argument and preserves Flutter theme/default resolution. No local value is synthesized."
                        : "Stores the exact local typed value. It is exclusive with a project reference; drafts in the inactive source do not change this choice.";
                note.setText(text); note.getAccessibleContext().setAccessibleDescription(text);
                var candidate = PROJECT.equals(selected) ? requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue()
                        : localMode.equals(selected) ? requestValidation ? localPanel.stagedDraftValue() : localPanel.validatedDraftValue()
                        : FlutterPropertyCellValue.unset();
                clearInvalid(note, text);
                if (requestValidation) markValid(candidate); else stageValid(candidate);
                return true;
            } catch (IllegalArgumentException failure) { markInvalid(failure.getMessage(), note); return false; }
            finally { refreshing = false; }
        }

        private static void clearRoot(Container parent) {
            for (var component : parent.getComponents()) {
                if (component instanceof JTextField field && FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME.equals(field.getName())) field.setText("");
                else if (component instanceof Container child) clearRoot(child);
            }
        }
    }
}
