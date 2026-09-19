package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.DartParameter;
import io.github.vgrytsenko2022.designer.catalog.PropertyDefinition;
import io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Exact signed Duration microseconds or analyzer-verified Duration, never a Dart expression. */
final class FlutterDurationReferenceEditorComponent {
    static final String MODE_NAME = "flutter.duration.mode";
    static final String VALUE_NAME = "flutter.duration.microseconds";
    static final String NOTE_NAME = "flutter.duration.note";
    static final String OMIT = "Use SDK default (omit field)";
    static final String NULL = "Explicit null";
    static final String LITERAL = "Duration in microseconds";
    static final String PROJECT = "Project Duration reference";

    private FlutterDurationReferenceEditorComponent() { }
    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new DurationPanel(editor, binding, environment);
    }
    private static final class DurationPanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final JTextField microseconds = new JTextField(28);
        private final JTextArea note = new JTextArea(5, 60);
        private final JPanel cards = new JPanel(new CardLayout());
        private final FlutterPropertyEditorComponents.CommitOnValidPanel referencePanel;
        private boolean refreshing;

        DurationPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            if (binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.DURATION_REFERENCE)
                throw new IllegalArgumentException("Duration editor requires exact signed integer/Duration references with catalog-declared nullability.");
            setName("flutter.duration.editor"); setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(750, 565));
            getAccessibleContext().setAccessibleName("Duration value editor");
            boolean nullable = binding.definition().acceptedKinds().contains(io.github.vgrytsenko2022.designer.model.PropertyValueKind.NULL);
            getAccessibleContext().setAccessibleDescription("Choose omission, " + (nullable ? "explicit null, " : "") + "signed integer microseconds or an analyzer-verified Duration reference. Cancel publishes nothing.");
            var modes = new java.util.ArrayList<String>(); if (binding.optional()) modes.add(OMIT); if (nullable) modes.add(NULL); modes.add(LITERAL); modes.add(PROJECT);
            mode = new JComboBox<>(modes.toArray(String[]::new));
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Duration value source");
            var heading = new JPanel(new BorderLayout(8, 0)); var label = new JLabel("Source:"); label.setLabelFor(mode); heading.add(label, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            microseconds.setName(VALUE_NAME); microseconds.getAccessibleContext().setAccessibleName("Signed duration in microseconds");
            microseconds.getAccessibleContext().setAccessibleDescription("An exact signed integer; 1000000 microseconds equals one second. No decimals, units, rounding or Dart expressions.");
            var literal = new JPanel(new BorderLayout(8, 0)); var valueLabel = new JLabel("Microseconds:"); valueLabel.setLabelFor(microseconds); literal.add(valueLabel, BorderLayout.WEST); literal.add(microseconds, BorderLayout.CENTER);
            var literalCard = new JPanel(new BorderLayout()); literalCard.add(literal, BorderLayout.NORTH); cards.add(literalCard, LITERAL);
            cards.add(new JPanel(), OMIT); cards.add(new JPanel(), NULL);
            var initial = initialValue().explicitValue().orElse(null); microseconds.setText(initial instanceof PropertyValue.IntegerValue integer ? integer.value().toString() : "0");
            var constraint = binding.definition().constraints().stream().filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance).map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow();
            var referenceDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true), List.of(constraint), Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow(); var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.DartObjectReferenceValue ? initial
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_duration", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            var referenceEnv = PropertyEnv.create(new FeatureDescriptor());
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel) FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnv);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearRoot(referencePanel);
            cards.add(referencePanel, PROJECT); add(cards, BorderLayout.CENTER);
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true); note.getAccessibleContext().setAccessibleName("Duration semantics and runtime limitations"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? OMIT : initial instanceof PropertyValue.NullValue ? NULL : initial instanceof PropertyValue.IntegerValue ? LITERAL : PROJECT);
            mode.addActionListener(ignored -> refresh(true)); referenceEnv.addPropertyChangeListener(ignored -> refresh(true));
            microseconds.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent event) { refresh(true); }
                @Override public void removeUpdate(DocumentEvent event) { refresh(true); }
                @Override public void changedUpdate(DocumentEvent event) { refresh(true); }
            });
            activate(); refresh(true);
        }
        @Override boolean prepareCommit() { return refresh(false); }
        private boolean refresh(boolean requestValidation) {
            if (refreshing) return true; refreshing = true;
            try {
                String selected = (String) mode.getSelectedItem(); ((CardLayout) cards.getLayout()).show(cards, selected);
                String description = "Exact signed microseconds are preserved without rounding; zero disables that AnimationStyle duration. Negative Duration values are valid Dart data but may fail the consuming runtime animation's assertions. "
                        + "Omission and explicit null remain distinct. Project Duration references, getters, members and zero-argument factories are analyzed but never executed in Canvas. "
                        + "ExpansionTile in the pinned SDK ignores reverseDuration even though it is retained and generated; forward duration still applies. Whole AnimationStyle and its local fields switch atomically; Cancel changes nothing.";
                String name = binding.definition().name().value();
                if (name.equals("durationUs") || name.equals("reverseDurationUs")) {
                    description = "Nonnegative integer microseconds; 1000000 equals one second. Zero completes immediately. "
                            + "Project Duration references are analyzed but never executed in Canvas. Cancel publishes nothing. "
                            + (name.equals("reverseDurationUs")
                                ? "Omission/null uses Duration. AnimatedCrossFade uses this for its reverse cross-fade. AnimatedSwitcher captures this per entry for outgoing transitions. AnimatedSize in pinned Flutter 3.44.8 runs ordinary growth and shrink transitions forward using Duration."
                                : "This Duration is required; omission and explicit null are not allowed. Custom-reference preview uses 300000 microseconds.");
                }
                if (name.equals("hoverOpenDelayUs")) {
                    description = "Exact signed Duration microseconds control SubmenuButton hover opening; 1000000 microseconds equals one second. "
                            + "Omission uses Duration.zero (no hover delay); explicit null is not allowed. A positive delay on a direct MenuBar child is rejected by the pinned SDK. "
                            + "Negative values remain exact Duration data and native Timer treats them as zero delay. Project Duration references, getters, members and zero-argument factories are analyzed but never executed in Canvas. Cancel publishes nothing.";
                }
                if (List.of("animationStyleDurationUs", "animationStyleReverseDurationUs").contains(name)) {
                    description = "SliverFloatingHeader uses independent show/hide durations. Exact signed microseconds are preserved; 1000000 equals one second. "
                            + "Omission/null uses 300000; zero completes immediately. Negative stored values are unsafe when native animation starts, so only Canvas uses zero with a warning. "
                            + "Nullable Duration references/getters/factories are analyzed but never executed in Canvas. Both durations are honored. Whole/local style changes are atomic; Cancel changes nothing.";
                }
                if (List.of("waitDurationUs", "showDurationUs", "exitDurationUs").contains(name)) {
                    description = "Exact signed Duration microseconds are preserved without rounding; 1000000 microseconds equals one second. "
                            + switch (name) {
                                case "waitDurationUs" -> "Wait duration controls mouse hover delay before showing the tooltip; SDK fallback is zero. ";
                                case "showDurationUs" -> "Show duration controls visibility after tap or long-press release, not mouse hover; SDK fallback is 1500000 microseconds. ";
                                default -> "Exit duration controls dismissal delay after the mouse stops hovering; SDK fallback is 100000 microseconds. ";
                            }
                            + "Omission and explicit null retain TooltipTheme/SDK fallback separately. Negative values remain exact Duration data; the SDK determines their runtime treatment. "
                            + "Project Duration references, getters, members and zero-argument factories are analyzed but never executed by Canvas. Each duration is independent; Cancel publishes nothing.";
                }
                note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
                var candidate = PROJECT.equals(selected) ? requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue()
                        : LITERAL.equals(selected) ? FlutterPropertyCellValue.explicit(integer())
                        : NULL.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()) : FlutterPropertyCellValue.unset();
                clearInvalid(note, description); if (requestValidation) markValid(candidate); else stageValid(candidate); return true;
            } catch (IllegalArgumentException failure) { markInvalid(failure.getMessage(), note); return false; }
            finally { refreshing = false; }
        }
        private PropertyValue.IntegerValue integer() {
            String text = microseconds.getText().strip(); if (!text.matches("[+-]?[0-9]+")) throw new IllegalArgumentException("Enter an exact signed integer number of microseconds.");
            return new PropertyValue.IntegerValue(new BigInteger(text));
        }
        private static void clearRoot(Container parent) { for (var component : parent.getComponents()) { if (component instanceof JTextField field && FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME.equals(field.getName())) field.setText(""); else if (component instanceof Container child) clearRoot(child); } }
    }
}
