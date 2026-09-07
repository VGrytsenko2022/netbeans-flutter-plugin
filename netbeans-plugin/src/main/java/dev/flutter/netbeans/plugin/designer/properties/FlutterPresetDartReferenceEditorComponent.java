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

/** Closed preset/typed-reference union with unpublished nested drafts until OK. */
final class FlutterPresetDartReferenceEditorComponent {
    static final String MODE_NAME = "flutter.presetReference.mode";
    static final String PRESET_NAME = "flutter.presetReference.preset";
    static final String NOTE_NAME = "flutter.presetReference.note";
    static final String OMIT = "Use SDK default (omit argument)";
    static final String PRESET = "Built-in preset";
    static final String PROJECT = "Project reference";
    static final String NULL = "Inherit (null)";

    private FlutterPresetDartReferenceEditorComponent() { }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new PresetReferencePanel(editor, binding, environment);
    }

    private static final class PresetReferencePanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final JComboBox<String> preset;
        private final JPanel cards = new JPanel(new CardLayout());
        private final JTextArea note = new JTextArea(3, 56);
        private final FlutterPropertyEditorComponents.CommitOnValidPanel referencePanel;
        private final String expectedType;
        private boolean refreshing;

        PresetReferencePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            if (binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.PRESET_DART_REFERENCE || binding.stringPresets().isEmpty())
                throw new IllegalArgumentException("Preset-reference editor requires a closed reviewed preset list and typed reference.");
            var referenceConstraint = binding.definition().constraints().stream()
                    .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow();
            expectedType = referenceConstraint.expectedDartType();
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(710, 500)); setName("flutter.presetReference.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " preset or Dart reference editor");
            getAccessibleContext().setAccessibleDescription("Choose omission, a reviewed preset, or an analyzer-verified " + expectedType + ". All drafts remain local until OK.");
            var modes = new java.util.ArrayList<String>();
            if (binding.optional()) modes.add(OMIT);
            if (binding.definition().acceptedKinds().contains(dev.flutter.netbeans.designer.model.PropertyValueKind.NULL)) modes.add(NULL);
            modes.add(PRESET); modes.add(PROJECT);
            mode = new JComboBox<>(modes.toArray(String[]::new));
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Value source");
            mode.getAccessibleContext().setAccessibleDescription("An explicit preset, including default, is distinct from omission.");
            var heading = new JPanel(new BorderLayout(8, 0)); var sourceLabel = new JLabel("Source:"); sourceLabel.setLabelFor(mode);
            heading.add(sourceLabel, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            preset = new JComboBox<>(binding.stringPresets().toArray(String[]::new)); preset.setName(PRESET_NAME);
            preset.getAccessibleContext().setAccessibleName("Reviewed preset");
            preset.getAccessibleContext().setAccessibleDescription("Only catalog-reviewed presets are available; custom conditions require a typed project reference.");
            var presets = new JPanel(new BorderLayout(8, 0)); var presetLabel = new JLabel("Preset:"); presetLabel.setLabelFor(preset);
            presets.add(presetLabel, BorderLayout.WEST); presets.add(preset, BorderLayout.CENTER); cards.add(presets, PRESET);
            var initial = initialValue().explicitValue().orElse(null);
            if (initial instanceof PropertyValue.StringValue value) {
                if (!binding.stringPresets().contains(value.value())) throw new IllegalArgumentException("Stored preset is not in the reviewed preset list.");
                preset.setSelectedItem(value.value());
            }
            var referenceDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true),
                    List.of(referenceConstraint), Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow();
            var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.DartObjectReferenceValue ? initial
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_reference", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            var referenceEnvironment = PropertyEnv.create(new FeatureDescriptor());
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnvironment);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearRoot(referencePanel);
            cards.add(referencePanel, PROJECT); cards.add(new JPanel(), OMIT); cards.add(new JPanel(), NULL); add(cards, BorderLayout.CENTER);
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Preset and project reference behavior"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : PRESET : initial instanceof PropertyValue.NullValue ? NULL : initial instanceof PropertyValue.StringValue ? PRESET : PROJECT);
            mode.addActionListener(ignored -> refresh(true)); preset.addActionListener(ignored -> refresh(true));
            referenceEnvironment.addPropertyChangeListener(ignored -> refresh(true)); activate(); refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            if (refreshing) return true;
            refreshing = true;
            try {
                String selected = (String) mode.getSelectedItem(); ((CardLayout) cards.getLayout()).show(cards, selected);
                String description = PROJECT.equals(selected)
                        ? "The analyzer verifies " + expectedType + ". "
                                + ("ScrollNotificationPredicate".equals(expectedType)
                                        ? "Isolated Canvas retains the wrapper and editable child, disables refresh activation, and reports the custom predicate; it never substitutes another filter. "
                                        : "Isolated Canvas does not execute project code and reports any preview limitation explicitly. ")
                                + "Use a typed getter or zero-argument factory for configured functions."
                        : PRESET.equals(selected) ? "Stores the selected reviewed preset explicitly. The default preset is retained as a value; it is not omission."
                        : NULL.equals(selected) ? "Explicit null stops lower-priority local state entries and lets the SDK resolve its theme/default cursor. It is distinct from an omitted local entry."
                        : "Omits this argument or local entry. Other property values are unchanged; lower-priority local states may still apply.";
                note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
                var candidate = PROJECT.equals(selected) ? requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue()
                        : PRESET.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.StringValue((String) preset.getSelectedItem()))
                        : NULL.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()) : FlutterPropertyCellValue.unset();
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
