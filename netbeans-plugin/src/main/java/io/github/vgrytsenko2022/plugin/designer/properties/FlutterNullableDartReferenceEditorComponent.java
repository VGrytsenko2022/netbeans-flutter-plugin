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
import java.util.List;
import java.util.Optional;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Nullable typed reference, optionally including a closed local callback; drafts publish only on OK. */
final class FlutterNullableDartReferenceEditorComponent {
    static final String MODE_NAME = "flutter.nullableReference.mode";
    static final String NOTE_NAME = "flutter.nullableReference.note";
    static final String OMIT = "Use SDK default (omit argument)";
    static final String PROJECT = "Project reference";
    static final String LOCAL = "Local event handler";
    static final String PRESET = "Built-in no-op";
    static final String HANDLER_NAME = "flutter.nullableReference.handler";

    private FlutterNullableDartReferenceEditorComponent() { }

    static String nullText(FlutterTypedPropertyEditors.Binding binding) {
        if (listViewExtentBuilder(binding)) return "Use existing sizing (explicit null)";
        if (textFieldBuilder(binding)) return binding.definition().name().value().equals("buildCounter")
                ? "SDK counter (explicit null)" : "Disable context menu (explicit null)";
        return binding.definition().name().value().equals("labels") ? "No labels (null)" : "Explicit null";
    }

    private static boolean listViewExtentBuilder(FlutterTypedPropertyEditors.Binding binding) {
        return binding.definition().name().value().equals("itemExtentBuilder")
                && binding.definition().constraints().stream().anyMatch(value -> value instanceof PropertyValueConstraint.DartObjectReferenceValues reference
                        && reference.expectedDartType().equals("ItemExtentBuilder?"));
    }

    private static boolean textFieldBuilder(FlutterTypedPropertyEditors.Binding binding) {
        String name = binding.definition().name().value();
        return binding.definition().constraints().stream().anyMatch(value -> value instanceof PropertyValueConstraint.DartObjectReferenceValues reference
                && (name.equals("buildCounter") && reference.expectedDartType().equals("InputCounterWidgetBuilder?")
                        || name.equals("contextMenuBuilder") && reference.expectedDartType().equals("EditableTextContextMenuBuilder?")));
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
        private final boolean textFieldBuilder;
        private final boolean listViewExtentBuilder;
        private final JTextField handler = new JTextField(32);
        private boolean refreshing;

        NullableReferencePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            if (binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.NULLABLE_DART_REFERENCE)
                throw new IllegalArgumentException("Nullable-reference editor requires the exact null/typed-reference union.");
            var constraint = binding.definition().constraints().stream()
                    .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow();
            expectedType = binding.definition().name().value().equals("onNotification")
                    && constraint.expectedDartType().equals("NotificationListenerCallback<Notification>")
                    ? "NotificationListenerCallback<T> for the selected Notification type"
                    : constraint.expectedDartType().equals("Image?") ? "dart:ui.Image? (decoded image, not Widget Image or ImageProvider)"
                    : constraint.expectedDartType();
            nullLabel = nullText(binding);
            textFieldBuilder = textFieldBuilder(binding) && Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(
                    FlutterDartObjectReferenceEditorComponent.TEXT_FIELD_BUILDER_ATTRIBUTE));
            listViewExtentBuilder = listViewExtentBuilder(binding) && Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(
                    FlutterDartObjectReferenceEditorComponent.LIST_VIEW_EXTENT_BUILDER_ATTRIBUTE));
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(710, 500)); setName("flutter.nullableReference.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " nullable Dart reference editor");
            getAccessibleContext().setAccessibleDescription("Choose omission, explicit null, or an analyzer-verified " + expectedType + ". Drafts remain local until OK.");
            if (textFieldBuilder) {
                setPreferredSize(new Dimension(800, 740)); note.setRows(13);
                getAccessibleContext().setAccessibleDescription(FlutterDartObjectReferenceEditorComponent.textFieldBuilderDescription(binding.definition().name().value()));
            }
            if (listViewExtentBuilder) {
                setPreferredSize(new Dimension(800, 740)); note.setRows(13);
                getAccessibleContext().setAccessibleDescription(FlutterDartObjectReferenceEditorComponent.listViewExtentBuilderDescription());
            }
            boolean supportsCallback = binding.definition().constraints().stream()
                    .anyMatch(PropertyValueConstraint.CallbackReference.class::isInstance);
            boolean supportsNoop = binding.definition().constraints().stream()
                    .anyMatch(value -> value instanceof PropertyValueConstraint.StringPattern pattern
                            && pattern.regularExpression().equals("noop"));
            var modes = new java.util.ArrayList<String>();
            if (binding.optional()) modes.add(OMIT);
            modes.add(nullLabel);
            if (supportsNoop) modes.add(PRESET);
            if (supportsCallback) modes.add(LOCAL);
            modes.add(PROJECT);
            mode = new JComboBox<>(modes.toArray(String[]::new));
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Value source");
            mode.getAccessibleContext().setAccessibleDescription("Explicit null is retained separately from omission; project references must be compatible with " + expectedType + ".");
            var heading = new JPanel(new BorderLayout(8, 0)); var label = new JLabel("Source:"); label.setLabelFor(mode);
            heading.add(label, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            var initial = initialValue().explicitValue().orElse(null);
            var referenceDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true), List.of(constraint), Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow();
            var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.DartObjectReferenceValue ? initial
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_reference", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            var referenceDescriptor = new FeatureDescriptor();
            referenceDescriptor.setValue(FlutterDartObjectReferenceEditorComponent.TEXT_FIELD_BUILDER_ATTRIBUTE, textFieldBuilder);
            referenceDescriptor.setValue(FlutterDartObjectReferenceEditorComponent.LIST_VIEW_EXTENT_BUILDER_ATTRIBUTE, listViewExtentBuilder);
            var referenceEnvironment = PropertyEnv.create(referenceDescriptor);
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnvironment);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearRoot(referencePanel);
            cards.add(referencePanel, PROJECT); cards.add(new JPanel(), OMIT); cards.add(new JPanel(), nullLabel);
            if (supportsNoop) cards.add(new JPanel(), PRESET);
            add(cards, BorderLayout.CENTER);
            if (supportsCallback) {
                handler.setName(HANDLER_NAME);
                handler.getAccessibleContext().setAccessibleName("Local event handler name");
                handler.getAccessibleContext().setAccessibleDescription("An existing local Dart method name; use Events to create a new handler body. No arbitrary code is accepted.");
                handler.setText(initial instanceof PropertyValue.CallbackValue callback ? callback.handler() : "");
                var local = new JPanel(new BorderLayout(8, 0)); var handlerLabel = new JLabel("Handler:");
                handlerLabel.setLabelFor(handler); local.add(handlerLabel, BorderLayout.WEST); local.add(handler, BorderLayout.CENTER);
                var localCard = new JPanel(new BorderLayout()); localCard.add(local, BorderLayout.NORTH); cards.add(localCard, LOCAL);
                handler.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { refresh(true); }
                    @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { refresh(true); }
                    @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { refresh(true); }
                });
            }
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Nullable project reference behavior"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : nullLabel
                    : initial instanceof PropertyValue.NullValue ? nullLabel
                    : initial instanceof PropertyValue.StringValue ? PRESET
                    : initial instanceof PropertyValue.CallbackValue ? LOCAL : PROJECT);
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
                        ? "The analyzer verifies a reference or zero-argument factory compatible with " + expectedType + ". Canvas never executes project code and reports any isolated preview limitation."
                        : LOCAL.equals(selected) ? "Binds an existing local method matching " + expectedType + ". Use Events to create, navigate to, or rename its user-owned body. Canvas never executes the handler."
                        : PRESET.equals(selected) ? "Stores an explicit no-op callback. It is distinct from explicit null and omission; the isolated Canvas never invokes it."
                        : nullLabel.equals(selected) ? binding.definition().name().value().equals("labels")
                                ? "Stores explicit null. For RangeSlider labels this disables both value-indicator labels; local label fields are removed atomically when committed."
                                : "Stores explicit null. This remains distinct from omitting the constructor argument."
                        : "Omits the argument and preserves the SDK default. This is distinct from storing null.";
                if (textFieldBuilder) {
                    String name = binding.definition().name().value();
                    description = (PROJECT.equals(selected) ? "Stores the exact analyzer-verified " + expectedType + " reference. "
                            : nullLabel.equals(selected) ? name.equals("buildCounter")
                                    ? "Stores explicit null: SDK counter logic remains active, subject to maxLength and decoration. "
                                    : "Stores explicit null: the context menu is disabled. "
                            : name.equals("buildCounter") ? "Omits buildCounter: SDK counter logic remains active. Restore Default has the same effect. "
                                    : "Omits contextMenuBuilder: restores the SDK platform default menu. Restore Default has the same effect. ")
                            + FlutterDartObjectReferenceEditorComponent.textFieldBuilderDescription(name);
                }
                if (listViewExtentBuilder) {
                    description = (PROJECT.equals(selected) ? "Stores the exact analyzer-verified ItemExtentBuilder? reference. "
                            : nullLabel.equals(selected) ? "Stores explicit null: existing natural or fixed sizing remains active. "
                                    : "Omits itemExtentBuilder and restores existing natural or fixed sizing. Restore Default has the same effect. ")
                            + FlutterDartObjectReferenceEditorComponent.listViewExtentBuilderDescription();
                }
                String tooltip = FlutterDartObjectReferenceEditorComponent.tooltipReferenceDescription(binding.definition().name().value(), expectedType);
                if (tooltip != null) description += " " + tooltip;
                note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
                var candidate = PROJECT.equals(selected) ? requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue()
                        : LOCAL.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.CallbackValue(handler.getText().strip()))
                        : PRESET.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("noop"))
                        : nullLabel.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()) : FlutterPropertyCellValue.unset();
                binding.validate(candidate);
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
