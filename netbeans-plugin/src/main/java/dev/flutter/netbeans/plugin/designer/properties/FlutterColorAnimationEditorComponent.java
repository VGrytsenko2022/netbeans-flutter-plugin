package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
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

/** Cancel-safe union of stopped nullable colors and a typed project animation reference. */
final class FlutterColorAnimationEditorComponent {
    static final String MODE_NAME = "flutter.colorAnimation.mode";
    static final String NOTE_NAME = "flutter.colorAnimation.note";
    static final String UNSET = "Use Flutter default (omit valueColor)";
    static final String STOPPED_COLOR = "Stopped color (literal or theme)";
    static final String STOPPED_NULL = "Stopped null (use color/theme fallback)";
    static final String PROJECT = "Project Animation<Color?>";

    private FlutterColorAnimationEditorComponent() { }

    static Component customEditor(PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new AnimationPanel(editor, binding, environment);
    }

    private static final class AnimationPanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode = new JComboBox<>(new String[]{UNSET, STOPPED_COLOR, STOPPED_NULL, PROJECT});
        private final CardLayout layout = new CardLayout();
        private final JPanel cards = new JPanel(layout);
        private final JTextArea note = new JTextArea(3, 55);
        private final FlutterPropertyEditorComponents.CommitOnValidPanel colorPanel;
        private final FlutterPropertyEditorComponents.CommitOnValidPanel referencePanel;
        private boolean refreshing;

        AnimationPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(720, 610));
            setName("flutter.colorAnimation.editor");
            getAccessibleContext().setAccessibleName("Progress indicator valueColor animation editor");
            getAccessibleContext().setAccessibleDescription("Optional stopped nullable color or analyzer-verified Animation<Color?>. All drafts remain local until OK; cancel preserves the original value.");
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Color animation source");
            mode.getAccessibleContext().setAccessibleDescription("Choose omission, stopped literal/theme color, explicit stopped null, or a project animation reference.");
            var heading = new JPanel(new BorderLayout(8, 0)); var label = new JLabel("Source:"); label.setLabelFor(mode);
            heading.add(label, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            var initial = initialValue().explicitValue().orElse(null);

            var colorDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true),
                    binding.definition().constraints().stream().filter(value -> value.kind() == PropertyValueKind.COLOR
                            || value.kind() == PropertyValueKind.THEME_TOKEN).toList(), Optional.empty());
            var colorBinding = FlutterTypedPropertyEditors.binding(colorDefinition).orElseThrow();
            var colorEditor = colorBinding.createEditor();
            colorEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.ColorValue
                    || initial instanceof PropertyValue.ThemeTokenValue ? initial : new PropertyValue.ColorValue(0xff000000L)));
            var colorEnvironment = PropertyEnv.create(new FeatureDescriptor());
            colorPanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterComplexPropertyEditorComponents.customEditor(colorEditor, colorBinding, colorEnvironment);
            cards.add(colorPanel, STOPPED_COLOR);

            var referenceDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true),
                    List.of(new PropertyValueConstraint.DartObjectReferenceValues("Animation<Color?>")), Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow();
            var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.DartObjectReferenceValue ? initial
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_valueColor", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            var referenceEnvironment = PropertyEnv.create(new FeatureDescriptor());
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnvironment);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearRoot(referencePanel);
            cards.add(referencePanel, PROJECT);
            cards.add(new JPanel(), UNSET); cards.add(new JPanel(), STOPPED_NULL); add(cards, BorderLayout.CENTER);
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Color animation behavior and preview note"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? UNSET : initial instanceof PropertyValue.NullValue ? STOPPED_NULL
                    : initial instanceof PropertyValue.DartObjectReferenceValue ? PROJECT : STOPPED_COLOR);
            mode.addActionListener(ignored -> refresh(true));
            colorEnvironment.addPropertyChangeListener(ignored -> refresh(true));
            referenceEnvironment.addPropertyChangeListener(ignored -> refresh(true));
            activate(); refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            if (refreshing) return true;
            refreshing = true;
            try {
                String selected = (String) mode.getSelectedItem(); layout.show(cards, selected);
                String text = switch (selected) {
                    case PROJECT -> "The analyzer verifies Animation<Color?>. Project code is not executed by isolated Canvas; dynamic animation preview is explicitly unavailable. Use a getter or factory for configured objects.";
                    case STOPPED_NULL -> "Stores AlwaysStoppedAnimation<Color?>(null), not omission. Flutter falls back to Color and then the progress-indicator theme. The explicit null choice is retained when saving and reopening.";
                    case STOPPED_COLOR -> "Stores AlwaysStoppedAnimation<Color> with a literal ARGB or theme color. This takes precedence over Color without clearing it. Theme color follows the active project theme.";
                    default -> "Omits valueColor and preserves Flutter's Color/theme fallback. Omission is different from an explicit stopped-null animation.";
                };
                note.setText(text); note.getAccessibleContext().setAccessibleDescription(text);
                var candidate = switch (selected) {
                    case PROJECT -> requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue();
                    case STOPPED_COLOR -> requestValidation ? colorPanel.stagedDraftValue() : colorPanel.validatedDraftValue();
                    case STOPPED_NULL -> FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
                    default -> FlutterPropertyCellValue.unset();
                };
                clearInvalid(note, text);
                if (requestValidation) markValid(candidate); else stageValid(candidate);
                return true;
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), note); return false;
            } finally { refreshing = false; }
        }

        private static void clearRoot(Container parent) {
            for (var component : parent.getComponents()) {
                if (component instanceof JTextField field && FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME.equals(field.getName())) field.setText("");
                else if (component instanceof Container child) clearRoot(child);
            }
        }
    }
}
