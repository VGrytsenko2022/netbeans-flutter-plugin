package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.util.Optional;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Local nullable state color: no animation or project-reference semantics. */
final class FlutterNullableColorEditorComponent {
    static final String MODE_NAME = "flutter.nullableColor.mode";
    static final String NOTE_NAME = "flutter.nullableColor.note";
    static final String OMIT = "Not set (omit state entry)";
    static final String INHERIT = "Inherit (null)";
    static final String COLOR = "Explicit color (literal or theme)";

    private FlutterNullableColorEditorComponent() { }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new ColorPanel(editor, binding, environment);
    }

    private static final class ColorPanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final CardLayout layout = new CardLayout();
        private final JPanel cards = new JPanel(layout);
        private final JTextArea note = new JTextArea(3, 55);
        private final FlutterPropertyEditorComponents.CommitOnValidPanel colorPanel;
        private boolean refreshing;

        ColorPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(720, 560));
            setName("flutter.nullableColor.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " nullable state color editor");
            getAccessibleContext().setAccessibleDescription("Omitted entry, explicit null fallback, or literal/theme color. Drafts are local until OK; cancel preserves the value.");
            mode = new JComboBox<>(binding.optional() ? new String[]{OMIT, INHERIT, COLOR} : new String[]{INHERIT, COLOR});
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Nullable state color mode");
            var label = new JLabel("Color mode:"); label.setLabelFor(mode);
            var heading = new JPanel(new BorderLayout(8, 0)); heading.add(label, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            var initial = initialValue().explicitValue().orElse(null);
            var definition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true),
                    binding.definition().constraints().stream().filter(value -> value.kind() == PropertyValueKind.COLOR || value.kind() == PropertyValueKind.THEME_TOKEN).toList(), Optional.empty());
            var colorBinding = FlutterTypedPropertyEditors.binding(definition).orElseThrow();
            var colorEditor = colorBinding.createEditor();
            colorEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.ColorValue || initial instanceof PropertyValue.ThemeTokenValue
                    ? initial : new PropertyValue.ColorValue(0xff000000L)));
            var colorEnvironment = PropertyEnv.create(new FeatureDescriptor());
            colorPanel = (FlutterPropertyEditorComponents.CommitOnValidPanel) FlutterComplexPropertyEditorComponents.customEditor(colorEditor, colorBinding, colorEnvironment);
            cards.add(colorPanel, COLOR); cards.add(new JPanel(), OMIT); cards.add(new JPanel(), INHERIT); add(cards, BorderLayout.CENTER);
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Nullable state color fallback behavior"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : INHERIT : initial instanceof PropertyValue.NullValue ? INHERIT : COLOR);
            mode.addActionListener(ignored -> refresh(true)); colorEnvironment.addPropertyChangeListener(ignored -> refresh(true));
            activate(); refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            if (refreshing) return true;
            refreshing = true;
            try {
                String selected = (String) mode.getSelectedItem(); layout.show(cards, selected);
                String text = COLOR.equals(selected) ? "Stores an explicit literal ARGB or reviewed Material theme color for this state."
                        : INHERIT.equals(selected) ? "Stores an explicit null result for this state. Matching this entry stops local state resolution and delegates to the widget/theme fallback; it does not select a lower local state."
                        : "Removes only this state entry. Remaining state entries and the widget/theme fallback stay available.";
                note.setText(text); note.getAccessibleContext().setAccessibleDescription(text);
                var candidate = COLOR.equals(selected) ? requestValidation ? colorPanel.stagedDraftValue() : colorPanel.validatedDraftValue()
                        : INHERIT.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()) : FlutterPropertyCellValue.unset();
                clearInvalid(note, text); if (requestValidation) markValid(candidate); else stageValid(candidate); return true;
            } catch (IllegalArgumentException failure) { markInvalid(failure.getMessage(), note); return false; }
            finally { refreshing = false; }
        }
    }
}
