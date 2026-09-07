package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.util.List;
import java.util.Optional;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Exact nullable typed-reference union; nested project drafts are unpublished until OK. */
final class FlutterNullableDartReferenceEditorComponent {
    static final String MODE_NAME = "flutter.nullableReference.mode";
    static final String NOTE_NAME = "flutter.nullableReference.note";
    static final String OMIT = "Use SDK default (omit argument)";
    static final String PROJECT = "Project reference";

    private FlutterNullableDartReferenceEditorComponent() { }

    static String nullText(FlutterTypedPropertyEditors.Binding binding) {
        return binding.definition().name().value().equals("labels") ? "No labels (null)" : "Explicit null";
    }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new NullableReferencePanel(editor, binding, environment);
    }

    private static final class NullableReferencePanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final JPanel cards = new JPanel(new CardLayout());
        private final JTextArea note = new JTextArea(3, 56);
        private final FlutterPropertyEditorComponents.CommitOnValidPanel referencePanel;
        private final String expectedType;
        private final String nullLabel;
        private boolean refreshing;

        NullableReferencePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            if (binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.NULLABLE_DART_REFERENCE)
                throw new IllegalArgumentException("Nullable-reference editor requires the exact null/typed-reference union.");
            var constraint = binding.definition().constraints().stream()
                    .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow();
            expectedType = constraint.expectedDartType(); nullLabel = nullText(binding);
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(710, 500)); setName("flutter.nullableReference.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " nullable Dart reference editor");
            getAccessibleContext().setAccessibleDescription("Choose omission, explicit null, or an analyzer-verified " + expectedType + ". Drafts remain local until OK.");
            mode = new JComboBox<>(binding.optional() ? new String[]{OMIT, nullLabel, PROJECT} : new String[]{nullLabel, PROJECT});
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Value source");
            mode.getAccessibleContext().setAccessibleDescription("Explicit null is retained separately from omission; project references require a non-null " + expectedType + ".");
            var heading = new JPanel(new BorderLayout(8, 0)); var label = new JLabel("Source:"); label.setLabelFor(mode);
            heading.add(label, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            var initial = initialValue().explicitValue().orElse(null);
            var referenceDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true), List.of(constraint), Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow();
            var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.DartObjectReferenceValue ? initial
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_reference", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            var referenceEnvironment = PropertyEnv.create(new FeatureDescriptor());
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnvironment);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearRoot(referencePanel);
            cards.add(referencePanel, PROJECT); cards.add(new JPanel(), OMIT); cards.add(new JPanel(), nullLabel); add(cards, BorderLayout.CENTER);
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Nullable project reference behavior"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : nullLabel : initial instanceof PropertyValue.NullValue ? nullLabel : PROJECT);
            mode.addActionListener(ignored -> refresh(true)); referenceEnvironment.addPropertyChangeListener(ignored -> refresh(true));
            activate(); refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            if (refreshing) return true;
            refreshing = true;
            try {
                String selected = (String) mode.getSelectedItem(); ((CardLayout) cards.getLayout()).show(cards, selected);
                String description = PROJECT.equals(selected)
                        ? "The analyzer verifies a non-null " + expectedType + " reference or zero-argument factory. Canvas never executes project code and reports any isolated preview limitation."
                        : nullLabel.equals(selected) ? "Stores explicit null. For RangeSlider labels this disables both value-indicator labels; local label fields are removed atomically when committed."
                        : "Omits the argument and preserves the SDK default. This is distinct from storing null.";
                note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
                var candidate = PROJECT.equals(selected) ? requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue()
                        : nullLabel.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()) : FlutterPropertyCellValue.unset();
                clearInvalid(note, description);
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
