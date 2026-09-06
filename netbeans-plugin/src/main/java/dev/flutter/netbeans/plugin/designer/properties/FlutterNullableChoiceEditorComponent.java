package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Exact nullable Boolean/enum unions; omission and explicit null never collapse. */
final class FlutterNullableChoiceEditorComponent {
    static final String MODE_NAME = "flutter.nullableChoice.mode";
    static final String VALUE_NAME = "flutter.nullableChoice.value";
    static final String NOTE_NAME = "flutter.nullableChoice.note";
    static final String OMIT = "Use constructor default (omit argument)";
    static final String NULL = "Explicit null";
    static final String VALUE = "Explicit value";
    static final String NULL_TEXT = "Explicit null";

    private FlutterNullableChoiceEditorComponent() { }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new NullableChoicePanel(editor, binding, environment);
    }

    private static final class NullableChoicePanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final JCheckBox booleanValue = new JCheckBox();
        private final JComboBox<String> enumValue;
        private final JTextArea note = new JTextArea(4, 50);
        private final boolean booleanKind;
        private final PropertyValueConstraint.EnumValues enumConstraint;

        NullableChoicePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            booleanKind = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.NULLABLE_BOOLEAN;
            if (!booleanKind && binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.NULLABLE_ENUM)
                throw new IllegalArgumentException("Nullable choice requires exactly Boolean|null or enum|null.");
            enumConstraint = booleanKind ? null : binding.definition().constraints().stream()
                    .filter(PropertyValueConstraint.EnumValues.class::isInstance).map(PropertyValueConstraint.EnumValues.class::cast).findFirst().orElseThrow();
            enumValue = new JComboBox<>(booleanKind ? new String[0] : enumConstraint.values().toArray(String[]::new));
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(560, 210)); setName("flutter.nullableChoice.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " nullable " + (booleanKind ? "Boolean" : "enum") + " editor");
            getAccessibleContext().setAccessibleDescription("Choose omission, explicit null, or a concrete value. Cancel preserves all saved states; only OK commits.");
            mode = new JComboBox<>(binding.optional() ? new String[]{OMIT, NULL, VALUE} : new String[]{NULL, VALUE});
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Value source");
            var source = new JPanel(new BorderLayout(8, 0)); var sourceLabel = new JLabel("Source:"); sourceLabel.setLabelFor(mode);
            source.add(sourceLabel, BorderLayout.WEST); source.add(mode, BorderLayout.CENTER); add(source, BorderLayout.NORTH);
            var control = booleanKind ? booleanValue : enumValue; control.setName(VALUE_NAME);
            control.getAccessibleContext().setAccessibleName(booleanKind ? "Explicit Boolean value" : "Explicit enum value");
            booleanValue.setOpaque(false); booleanValue.setHorizontalAlignment(SwingConstants.CENTER); booleanValue.setVerticalAlignment(SwingConstants.CENTER);
            add(control, BorderLayout.CENTER);
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Nullable value semantics"); add(note, BorderLayout.SOUTH);
            var initial = initialValue().explicitValue().orElse(null);
            if (initial instanceof PropertyValue.BooleanValue value) booleanValue.setSelected(value.value());
            if (initial instanceof PropertyValue.EnumValue value) enumValue.setSelectedItem(value.value());
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : NULL : initial instanceof PropertyValue.NullValue ? NULL : VALUE);
            mode.addActionListener(ignored -> refresh(true)); booleanValue.addActionListener(ignored -> refresh(true)); enumValue.addActionListener(ignored -> refresh(true));
            activate(); refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            String selected = (String) mode.getSelectedItem();
            booleanValue.setEnabled(VALUE.equals(selected)); enumValue.setEnabled(VALUE.equals(selected));
            String property = binding.definition().name().value();
            String semantics = property.equals("isSemanticButton")
                    ? "Omission uses true. Explicit null suppresses the button role announcement; true and false remain explicit Boolean values."
                    : property.equals("clipBehavior")
                            ? "Omission keeps the active constructor default: standard forwards null, icon defaults to Clip.none. Explicit null is retained and uses SDK automatic clipping."
                            : "Omission preserves the constructor default. Explicit null and each concrete value remain distinct.";
            note.setText(semantics); note.getAccessibleContext().setAccessibleDescription(semantics);
            booleanValue.getAccessibleContext().setAccessibleDescription("Boolean value " + booleanValue.isSelected() + "; centered checkbox.");
            try {
                var candidate = OMIT.equals(selected) ? FlutterPropertyCellValue.unset()
                        : NULL.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())
                        : FlutterPropertyCellValue.explicit(booleanKind ? new PropertyValue.BooleanValue(booleanValue.isSelected())
                                : new PropertyValue.EnumValue(enumConstraint.dartType().name(), (String) enumValue.getSelectedItem()));
                clearInvalid(note, semantics);
                if (requestValidation) markValid(candidate); else stageValid(candidate);
                return true;
            } catch (IllegalArgumentException failure) { markInvalid(failure.getMessage(), note); return false; }
        }
    }
}
