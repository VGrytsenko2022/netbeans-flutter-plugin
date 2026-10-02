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
        private final String nullLabel;
        private final boolean appBarPredicate;
        private final boolean radioTileCallback;
        private boolean refreshing;

        PresetReferencePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            if (binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.PRESET_DART_REFERENCE || binding.stringPresets().isEmpty())
                throw new IllegalArgumentException("Preset-reference editor requires a closed reviewed preset list and typed reference.");
            var referenceConstraint = binding.definition().constraints().stream()
                    .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow();
            expectedType = referenceConstraint.expectedDartType();
            radioTileCallback = environment != null && Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(
                    FlutterDartObjectReferenceEditorComponent.RADIO_TILE_CALLBACK_ATTRIBUTE));
            appBarPredicate = binding.definition().name().value().equals("notificationPredicate")
                    && expectedType.equals("ScrollNotificationPredicate") && environment != null
                    && Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(
                            FlutterDartObjectReferenceEditorComponent.APP_BAR_PREDICATE_ATTRIBUTE));
            nullLabel = isRadioCallback() || isListTileCallback() || isCheckboxTileCallback() || isSwitchTileCallback() || isImageErrorCallback() || isExpansionCallback()
                    || binding.definition().name().value().equals("onTriggered") && expectedType.equals("TooltipTriggeredCallback") ? "Explicit null" : NULL;
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(710, 500)); setName("flutter.presetReference.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " preset or Dart reference editor");
            getAccessibleContext().setAccessibleDescription("Choose omission, a reviewed preset, or an analyzer-verified " + expectedType + ". All drafts remain local until OK.");
            if (appBarPredicate) {
                note.setRows(6);
                setPreferredSize(new Dimension(740, 570));
                getAccessibleContext().setAccessibleDescription(getAccessibleContext().getAccessibleDescription()
                        + " AppBar accepts matching notifications for scrolled-under elevation; true does not stop notification propagation. Omission uses the SDK depth-zero filter.");
            }
            var modes = new java.util.ArrayList<String>();
            if (binding.optional()) modes.add(OMIT);
            if (binding.definition().acceptedKinds().contains(io.github.vgrytsenko2022.designer.model.PropertyValueKind.NULL)) modes.add(nullLabel);
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
            if (expectedType.equals("AnimatedIconData")) {
                getAccessibleContext().setAccessibleDescription("Choose one of 14 AnimatedIcons presets or an analyzer-verified AnimatedIconData source. Icon is required; drafts remain local until OK.");
                preset.getAccessibleContext().setAccessibleDescription("AnimatedIcons presets with SVG frames at 0, 50 and 100 percent. Project sources must return Flutter icon data.");
                preset.setRenderer(new javax.swing.DefaultListCellRenderer(){
                    @Override public Component getListCellRendererComponent(javax.swing.JList<?> list,Object value,int index,boolean selected,boolean focus){
                        var label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                        if(value instanceof String name)FlutterAnimatedIconPreview.decorate(label,name);
                        return label;
                    }
                });
                var preview=new JLabel();preview.setName(FlutterAnimatedIconPreview.PREVIEW_NAME);
                Runnable update=()->FlutterAnimatedIconPreview.decorate(preview,(String)preset.getSelectedItem());
                preset.addActionListener(event->update.run());update.run();
                presets.add(preview,BorderLayout.SOUTH);
            }
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
            var referenceDescriptor = new FeatureDescriptor();
            referenceDescriptor.setValue(FlutterDartObjectReferenceEditorComponent.RADIO_GROUP_CALLBACK_ATTRIBUTE, isRadioCallback() && !binding.optional());
            referenceDescriptor.setValue(FlutterDartObjectReferenceEditorComponent.CHECKBOX_TILE_CALLBACK_ATTRIBUTE, isCheckboxTileCallback());
            referenceDescriptor.setValue(FlutterDartObjectReferenceEditorComponent.SWITCH_TILE_CALLBACK_ATTRIBUTE, isSwitchTileCallback());
            referenceDescriptor.setValue(FlutterDartObjectReferenceEditorComponent.RADIO_TILE_CALLBACK_ATTRIBUTE, radioTileCallback);
            referenceDescriptor.setValue(FlutterDartObjectReferenceEditorComponent.APP_BAR_PREDICATE_ATTRIBUTE, appBarPredicate);
            var referenceEnvironment = PropertyEnv.create(referenceDescriptor);
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnvironment);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearRoot(referencePanel);
            cards.add(referencePanel, PROJECT); cards.add(new JPanel(), OMIT); cards.add(new JPanel(), nullLabel); add(cards, BorderLayout.CENTER);
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Preset and project reference behavior"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : PRESET : initial instanceof PropertyValue.NullValue ? nullLabel : initial instanceof PropertyValue.StringValue ? PRESET : PROJECT);
            mode.addActionListener(ignored -> refresh(true)); preset.addActionListener(ignored -> refresh(true));
            referenceEnvironment.addPropertyChangeListener(ignored -> refresh(true)); activate(); refresh(true);
        }

        private boolean isRadioCallback() { return binding.definition().name().value().equals("onChanged") && expectedType.equals("ValueChanged<Object?>"); }
        private boolean isExpansionCallback() { return binding.definition().name().value().equals("onExpansionChanged") && expectedType.equals("ValueChanged<bool>"); }
        private boolean isCheckboxTileCallback() {
            return !binding.optional() && binding.stringPresets().equals(List.of("noop"))
                    && binding.definition().name().value().equals("onChanged") && expectedType.equals("ValueChanged<bool?>");
        }
        private boolean isListTileCallback() {
            return binding.stringPresets().equals(List.of("noop")) && (List.of("onTap", "onLongPress").contains(binding.definition().name().value()) && expectedType.equals("VoidCallback")
                    || binding.definition().name().value().equals("onFocusChange") && expectedType.equals("ValueChanged<bool>"));
        }
        private boolean isSwitchTileCallback() {
            return !binding.optional() && binding.stringPresets().equals(List.of("noop"))
                    && binding.definition().name().value().equals("onChanged") && expectedType.equals("ValueChanged<bool>");
        }
        private boolean isImageErrorCallback() {
            return binding.stringPresets().equals(List.of("noop")) && expectedType.equals("ImageErrorListener");
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
: nullLabel.equals(selected) ? isRadioCallback() ? "Stores a null Radio callback. Activation can still come from a registry or inherited RadioGroup; this is not the No-op preset." : "Explicit null stops lower-priority local state entries and lets the SDK resolve its theme/default cursor. It is distinct from an omitted local entry."
                        : "Omits this argument or local entry. Other property values are unchanged; lower-priority local states may still apply.";
                if (isListTileCallback()) description = PROJECT.equals(selected)
                        ? "Stores the strictly typed ListTile callback. Disabling the tile retains this argument and its semantic presence. Isolated Canvas never executes project callbacks."
                        : PRESET.equals(selected) ? "Stores an explicit no-op callback. Gesture callback presence may enable pointer and button semantics, even when the tile is disabled; it is not null or omission."
                        : nullLabel.equals(selected) ? "Stores explicit null: this callback is absent. This is distinct from the explicit no-op and from omitting the argument."
                        : "Omits this ListTile callback argument. No handler is injected and all other callback fields remain unchanged.";
                if (isCheckboxTileCallback()) description = PROJECT.equals(selected)
                        ? "Stores the required typed CheckboxListTile callback unchanged. Enabled is independent and nullable. Isolated Canvas never executes project callbacks."
                        : PRESET.equals(selected) ? "Stores an explicit no-op callback. It is distinct from null; the SDK combines callback presence with Enabled to determine activation."
                        : "Stores explicit null for the required On changed argument. The checkbox cannot request a value change; Enabled remains independent and unchanged.";
                if (isSwitchTileCallback()) description = PROJECT.equals(selected)
                        ? "Stores the required ValueChanged<bool> SwitchListTile callback. It requests a new controlled value; user code or a State binding must update it. Isolated Canvas never executes the callback."
                        : PRESET.equals(selected) ? "Stores an explicit no-op callback. The tile is enabled but its controlled Value does not change automatically. There is no separate Enabled or Tristate property."
                        : "Stores explicit null for required On changed and disables the SwitchListTile. Value, Selected and every child remain unchanged; null is distinct from No-op and cannot be omitted.";
                if (isImageErrorCallback()) description = PROJECT.equals(selected)
                        ? "Stores a typed ImageErrorListener reference or factory. A non-null callback requires its matching thumb image. Canvas never executes project callbacks."
                        : PRESET.equals(selected) ? "Stores an explicit no-op image-error callback. Set its matching thumb image first."
                        : nullLabel.equals(selected) ? "Stores explicit null: no image-error handler. No image provider is required for null."
                        : "Omits this image-error callback. Other callbacks and both image providers remain unchanged.";
                if (radioTileCallback) description = (PROJECT.equals(selected)
                        ? "Stores the legacy callback after exact ValueChanged<T?> analysis for the selected type. Use a reference/getter/member or zero-argument factory; handler source remains unchanged."
                        : PRESET.equals(selected) ? "Stores an explicit No-op callback. It does not update the controlled legacy Group value automatically and is distinct from null or omission."
                        : nullLabel.equals(selected) ? "Stores explicit null for legacy On changed. Modern RadioGroup may still own activation; null is not a universal disable switch."
                        : "Omits legacy On changed. Modern RadioGroup may still own activation. Restore Default omits the argument instead of recreating the creation-time No-op.")
                        + " Modern RadioGroup is the preferred group owner. RadioListTile has no groupRegistry property. Enabled is independently nullable. Canvas never executes project callbacks. Cancel publishes nothing.";
                if (binding.definition().name().value().equals("cursor") && expectedType.equals("MouseCursor")) {
                    description = PROJECT.equals(selected)
                            ? "The analyzer verifies a non-null MouseCursor reference or zero-argument factory. Designer Canvas never executes project cursor code and reports the isolated preview limitation."
                            : PRESET.equals(selected) ? "Stores this reviewed MouseCursor preset explicitly, including defer or uncontrolled. It is distinct from omission."
                            : "Omits the cursor argument and uses MouseCursor.defer. Other MouseRegion properties remain unchanged.";
                }
                if (appBarPredicate) {
                    description = (PROJECT.equals(selected)
                            ? "The analyzer verifies a non-null ScrollNotificationPredicate: bool Function(ScrollNotification). Use a function reference, getter, member, or zero-argument factory returning that function. Isolated Canvas never executes the project predicate; its SDK default depth-zero filter is an explicit preview approximation."
                            : PRESET.equals(selected)
                                    ? "Stores the selected preset explicitly. default and depthZero accept notification.depth == 0; all accepts every depth. The default preset is distinct from omission."
                                    : "Omits notificationPredicate and restores the SDK default: notification.depth == 0. Restore Default has the same effect; it never supplies a null predicate.")
                            + " Returning true accepts a notification for AppBar scrolled-under elevation; it does not stop notification propagation. Other properties and user Dart bodies remain unchanged; Cancel publishes nothing.";
                }
                if (isExpansionCallback()) {
                    description = (PROJECT.equals(selected) ? "The analyzer verifies a non-null ValueChanged<bool> reference/getter/member or zero-argument factory. "
                            : PRESET.equals(selected) ? "No-op observes nothing but leaves expansion enabled. "
                            : nullLabel.equals(selected) ? "Stores explicit null: no expansion callback is installed; the tile can still expand. "
                            : "Omits onExpansionChanged; expansion remains available and no callback is installed. ")
                            + "The callback receives true when expanding starts and false when collapsing starts, including controller changes. Initially expanded is only a seed, not a controlled current value; no two-way State binding is generated. "
                            + "Use Events for user-owned handler bodies. Canvas never calls project callbacks. Cancel preserves the callback and every child slot.";
                }
                if (binding.definition().name().value().startsWith("expansionAnimationStyle")
                        && (expectedType.equals("AnimationStyle") || expectedType.equals("Curve"))) {
                    description = (PROJECT.equals(selected) ? "The analyzer verifies a non-null " + expectedType + " reference, getter, member or zero-argument factory. "
                            : PRESET.equals(selected) ? expectedType.equals("AnimationStyle") ? "noAnimation stores AnimationStyle.noAnimation explicitly, setting both durations to zero. "
                                    : "Stores the selected reviewed Curves constant explicitly; no arbitrary curve expression is accepted. "
                            : nullLabel.equals(selected) ? "Stores explicit null, preserving the SDK/theme fallback for this field. " : "Omits this field and preserves the SDK/theme default. ")
                            + "Whole AnimationStyle and its four local fields switch atomically; the other shape/style families and child slots remain unchanged. "
                            + "The pinned ExpansionTile SDK ignores reverseDuration even though it is stored and generated. Custom curves and styles are never executed in isolated Canvas; its rendering is an approximation. Cancel publishes nothing.";
                }
                if (expectedType.equals("AnimatedIconData")) description = PROJECT.equals(selected)
                        ? "Requires non-null AnimatedIconData returned from Flutter AnimatedIcons. Custom subclasses are opaque and fail Flutter's private-data cast. Canvas never executes this source and previews menu_close."
                        : "All 14 pinned AnimatedIcons constants. SVG previews show 0%, 50%, 100%; Progress controls the actual frame. Required Icon cannot be unset or null; Cancel publishes nothing.";
                String tooltip = FlutterDartObjectReferenceEditorComponent.tooltipReferenceDescription(binding.definition().name().value(), expectedType);
                if (tooltip != null) description = tooltip;
                if (expectedType.equals("Animation<RelativeRect>") && binding.definition().name().value().equals("rect"))
                    description = PROJECT.equals(selected)
                        ? "Requires non-null Animation<RelativeRect>. Controller and Tween lifetime remain in Dart. Canvas previews zero physical insets and never executes this reference. Local inset values are retained."
                        : "Uses the four physical Rect left/top/right/bottom inset fields in logical pixels. All zero fills the Stack. Editing any inset selects this local stopped animation atomically; RTL does not reverse the insets.";
                if (expectedType.equals("Animation<Rect?>") && binding.definition().name().value().equals("rect"))
                    description = PROJECT.equals(selected)
                        ? "Requires a non-null Animation<Rect?> (compatible Animation<Rect> is allowed). Canvas never executes project references and previews Rect.fromLTWH(0,0,48,48). Controllers remain in Dart."
                        : "local uses physical LTWH fields. null creates AlwaysStoppedAnimation<Rect?>(null); Flutter uses Rect.zero. Reference Size is separate, not a scale. Editing a rectangle field selects local; RTL does not reverse coordinates.";
                if (expectedType.equals("Size") && binding.definition().name().value().equals("size"))
                    description = "Reference Size is not actual Stack size or a scale. Local signed width/height are preserved. A project Size is analyzer-verified and previews as Size(48,48). Editing either dimension selects local Size without changing the rectangle animation.";
                note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
                var candidate = PROJECT.equals(selected) ? requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue()
                        : PRESET.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.StringValue((String) preset.getSelectedItem()))
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
