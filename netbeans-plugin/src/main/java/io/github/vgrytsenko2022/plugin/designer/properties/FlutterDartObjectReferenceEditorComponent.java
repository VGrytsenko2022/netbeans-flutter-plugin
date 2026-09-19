package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyEditor;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Transactional editor for one closed project-Dart object reference. */
final class FlutterDartObjectReferenceEditorComponent {
    static final String RADIO_GROUP_CALLBACK_ATTRIBUTE = "flutter.radioGroup.callback";
    static final String CHECKBOX_TILE_CALLBACK_ATTRIBUTE = "flutter.checkboxListTile.callback";
    static final String SWITCH_TILE_CALLBACK_ATTRIBUTE = "flutter.switchListTile.callback";
    static final String RADIO_TILE_CALLBACK_ATTRIBUTE = "flutter.radioListTile.callback";
    static final String APP_BAR_PREDICATE_ATTRIBUTE = "flutter.appBar.notificationPredicate";
    static final String ELEVATED_BUTTON_LAYER_ATTRIBUTE = "flutter.elevatedButton.layerBuilder";
    static final String TEXT_FIELD_BUILDER_ATTRIBUTE = "flutter.textField.builder";
    static final String LIST_VIEW_EXTENT_BUILDER_ATTRIBUTE = "flutter.listView.itemExtentBuilder";
    static final String PANEL_NAME = "flutter.dartObjectReference.editor";
    static final String DEFAULT_NAME = "flutter.dartObjectReference.default";
    static final String SCOPE_NAME = "flutter.dartObjectReference.scope";
    static final String LIBRARY_URI_NAME = "flutter.dartObjectReference.libraryUri";
    static final String ROOT_SYMBOL_NAME = "flutter.dartObjectReference.rootSymbol";
    static final String MEMBER_NAME = "flutter.dartObjectReference.member";
    static final String ACCESS_NAME = "flutter.dartObjectReference.access";
    static final String CONSTANT_NAME = "flutter.dartObjectReference.constant";
    static final String PREVIEW_NAME = "flutter.dartObjectReference.preview";

    static final String CURRENT_LIBRARY_TEXT = "Current Dart library";
    static final String IMPORTED_LIBRARY_TEXT = "Imported package library";
    static final String EXISTING_VALUE_TEXT = "Existing value";
    static final String INVOCATION_TEXT =
            "Zero-argument constructor, factory, or function";

    private FlutterDartObjectReferenceEditorComponent() {
    }

    static String tooltipReferenceDescription(String name, String expectedType) {
        if (expectedType.equals("Tween<Object>")) return "Tween<T> must match the selected Value type and nullability. Transfer a fresh exclusive instance, preferably from a zero-argument factory: Flutter mutates it. Tween.end must be non-null even for nullable T. Use a project typedef for SDK/complex types; draft T, Tween and Builder together. Canvas never executes project tweens.";
        if (expectedType.equals("ValueListenable<Object>") || expectedType.equals("ValueWidgetBuilder<Object>")) return
                "Value-based builders use the selected Value type T, including nullability: the analyzer verifies Widget Function(BuildContext, T, Widget?) and its paired ValueListenable<T> or Tween<T>. "
                + "Use a typed reference, getter, member or zero-argument factory. Change T, nullability, source and Builder together in the Value type dialog. "
                + "Both source and Builder are required. ValueListenableBuilder uses a constant built-in value or nullable null preset; TweenAnimationBuilder creates a fresh default tween with a non-null end, and all project types require a custom tween. "
                + "Child is optional and must match the box/sliver placement. The application owns notifier/controller disposal; a Tween must be transferred exclusively to Flutter without sharing or later mutation. Canvas never evaluates project sources or callbacks.";

        if (name.equals("shortcut") && expectedType.equals("MenuSerializableShortcut")) return
                "Choose a strict non-null MenuSerializableShortcut reference/getter/member or zero-argument factory, including a source-owned custom implementation. Explicit null is a separate supported outer mode. "
                + "This is a menu shortcut hint, not automatic global key registration. Application code owns registration and execution. "
                + "Setting whole Shortcut, including explicit null, atomically clears eight local activator fields. Local SingleActivator supports all 432 reviewed trigger keys plus modifiers and Num lock; CharacterActivator preserves its exact case-sensitive match string, including Unicode or empty strings, plus Control/Alt/Meta and Include repeats, not Shift or Num lock. "
                + "Choose a local anchor before modifiers. Resetting that anchor clears the local shortcut; Undo restores all prior fields. Canvas never executes shortcut factories or project code. Cancel preserves child IDs and the stored reference.";
        if (name.equals("data") && expectedType.equals("TooltipThemeData")) return
                "The analyzer requires a non-null TooltipThemeData reference, getter/member or zero-argument factory. Explicit null, raw expressions and callbacks are not accepted. "
                + "Whole Data cannot coexist with any of the 46 local leaves, including explicit null or local State bindings. Reset local values and remove bindings before choosing Data; reset Data before editing local leaves. Nothing is silently cleared. "
                + "When Data and all local leaves are unset, the generator constructs const TooltipThemeData(). This replaces the nearest theme rather than merging with an outer TooltipTheme. "
                + "Canvas never executes project theme data and uses an explicitly approximate local preview. Cancel leaves the reference, child and user-owned source unchanged.";
        if (name.equals("richMessage") && expectedType.equals("InlineSpan")) return
                "The analyzer requires a non-null InlineSpan reference, getter/member or zero-argument factory. TextSpan, WidgetSpan and custom span trees, recognizers and their lifecycle remain in user-owned Dart; this editor does not accept raw expressions or invent a local span-tree format. "
                + "Setting Rich message clears non-null Message in the same undoable edit. Clearing the last non-null content is rejected; set Message to switch back. Empty plain text is valid and suppresses the SDK overlay. "
                + "Canvas never executes project spans or WidgetSpan children and shows an explicitly labelled surrogate. Ignore pointer defaults differ for plain and rich content. Cancel changes neither content nor the anchor child.";
        if (name.equals("positionDelegate") && expectedType.equals("TooltipPositionDelegate")) return
                "The analyzer requires a non-null TooltipPositionDelegate: Offset Function(TooltipPositionContext context). The returned Offset must not be null. Context supplies target, targetSize, tooltipSize, overlaySize and the resolved verticalOffset/preferBelow. "
                + "Choose a reference, getter/member or zero-argument factory returning the function. This is a positioning delegate in Properties, not a native Event or Widget builder. "
                + "Omission/null restores SDK positioning. Canvas never executes the delegate and explicitly approximates its position; user Dart code and Child stay unchanged.";
        if (name.equals("onTriggered") && expectedType.equals("TooltipTriggeredCallback")) return
                "The analyzer requires TooltipTriggeredCallback with exact signature void(). In the pinned SDK it is called for accepted tap/long-press triggers, not mouse hover or ensureTooltipVisible(). "
                + "Omission, explicit null and No-op are distinct and do not disable tooltip visibility. Use the shared Events workflow to create, navigate to or rename the user-owned handler. "
                + "Manual trigger mode still permits mouse hover. Canvas never executes project callbacks; Cancel preserves all tooltip content, styles and Child.";
        return null;
    }

    static String textFieldBuilderDescription(String name) {
        String contract = "buildCounter".equals(name)
                ? "InputCounterWidgetBuilder? accepts a nullable callback. A non-null function has the exact signature Widget? Function(BuildContext, {required int currentLength, required int? maxLength, required bool isFocused}). "
                        + "The three named parameters are required, including nullable int? maxLength. An omitted/null callback uses SDK counter logic, subject to maxLength and decoration. A callback returning null hides both its counter and generated Semantics; that is not the same as a null callback. "
                : "EditableTextContextMenuBuilder? accepts a nullable callback. A non-null function has the exact signature Widget Function(BuildContext, EditableTextState). Its returned Widget must not be null. Omission uses the SDK platform default menu; an explicit null or a nullable reference resolving to null disables the context menu. ";
        return contract + "Use a current-file or package reference, getter/member, or zero-argument factory returning the callback, including a nullable callback. "
                + "Canvas never executes project builders: it shows an SDK-default approximation for custom references and cannot reproduce the callback result or null. Verify custom visuals in the real application or a Flutter widget test. "
                + "Cancel leaves both builders, onChanged and user Dart bodies unchanged.";
    }

    static String listViewExtentBuilderDescription() {
        return "ItemExtentBuilder? accepts a nullable callback with signature double? Function(int index, SliverLayoutDimensions dimensions). "
                + "Flutter rendering's SliverLayoutDimensions supplies scrollOffset, precedingScrollExtent, viewportMainAxisExtent and crossAxisExtent. "
                + "Omission or a null callback uses natural child sizing, or an existing fixed itemExtent. Return a valid extent for every actual child; a null result is only an out-of-range marker, not a default size or a way to truncate existing children. "
                + "Use a current-file or package reference, getter/member, or zero-argument factory returning the callback, including a nullable callback. "
                + "Reset fixed itemExtent before setting a reference. To return to fixed sizing, reset itemExtentBuilder or set explicit null first. Even a nullable reference cannot coexist with fixed itemExtent because its runtime result is unknown; no property is silently cleared. "
                + "Canvas never executes the project builder and shows a natural-size approximation, with an explicit warning. It cannot reproduce per-item extents or the callback's out-of-range result. "
                + "Children, their stored state and other scroll properties remain unchanged; Cancel publishes nothing.";
    }

    static Component customEditor(
            PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding,
            PropertyEnv environment) {
        if (binding.editorKind()
                != FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE) {
            throw new IllegalArgumentException(
                    "Dart object reference editor requires its typed binding.");
        }
        return new ReferencePanel(editor, binding, environment);
    }

    private enum Scope {
        CURRENT_LIBRARY(CURRENT_LIBRARY_TEXT),
        IMPORTED_LIBRARY(IMPORTED_LIBRARY_TEXT);

        private final String label;

        Scope(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum AccessChoice {
        REFERENCE(
                EXISTING_VALUE_TEXT,
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE),
        INVOCATION(
                INVOCATION_TEXT,
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION);

        private final String label;
        private final PropertyValue.DartObjectReferenceValue.Access access;

        AccessChoice(
                String label,
                PropertyValue.DartObjectReferenceValue.Access access) {
            this.label = label;
            this.access = access;
        }

        @Override
        public String toString() {
            return label;
        }

        static AccessChoice from(
                PropertyValue.DartObjectReferenceValue.Access access) {
            return access == PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                    ? REFERENCE : INVOCATION;
        }
    }

    private static final class ReferencePanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private static final String LIBRARY_DESCRIPTION =
                "Canonical package: URI declared by this project's .dart_tool/package_config.json.";
        private static final String ROOT_DESCRIPTION =
                "Dart root symbol. Imported symbols must be public; the current library may use a private symbol.";
        private static final String MEMBER_DESCRIPTION =
                "Optional member, named constructor, static field/getter, factory, or function name.";
        private static final String ACCESS_DESCRIPTION =
                "Use an existing value, or invoke a zero-argument constructor, factory, or function.";
        private static final String CONSTANT_DESCRIPTION =
                "Emit const for a zero-argument invocation. Candidate analysis verifies that it is legal.";

        private final JCheckBox useDefault;
        private final JComboBox<Scope> scope = new JComboBox<>(Scope.values());
        private final JTextField libraryUri = new JTextField(38);
        private final JTextField rootSymbol = new JTextField(28);
        private final JTextField member = new JTextField(28);
        private final JComboBox<AccessChoice> access =
                new JComboBox<>(AccessChoice.values());
        private final JCheckBox constant = new JCheckBox("Const invocation");
        private final JLabel preview = new JLabel();
        private final String expectedDartType;
        private final String argumentName;
        private final String argumentDisplayName;
        private final boolean shapeBranch;
        private final boolean bottomSheetScrimBranch;
        private final boolean appBarPredicateBranch;
        private final boolean elevatedButtonLayerBranch;
        private final boolean textFieldBuilderBranch;
        private final boolean listViewExtentBuilderBranch;
        private final boolean noOpRefreshBranch;
        private final boolean buttonActivationBranch;
        private boolean updating;

        ReferencePanel(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            expectedDartType = binding.definition().constraints().stream()
                    .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast)
                    .map(PropertyValueConstraint.DartObjectReferenceValues::expectedDartType)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Dart object reference binding has no typed constraint."));
            argumentName = binding.definition().name().value();
            argumentDisplayName = Character.toUpperCase(argumentName.charAt(0))
                    + argumentName.substring(1);
            shapeBranch = "shape".equals(argumentName)
                    && "ShapeBorder".equals(expectedDartType);
            bottomSheetScrimBranch = "bottomSheetScrimBuilder".equals(argumentName)
                    && "Widget? Function(BuildContext, Animation<double>)".equals(expectedDartType);
            appBarPredicateBranch = "notificationPredicate".equals(argumentName)
                    && "ScrollNotificationPredicate".equals(expectedDartType)
                    && environment != null && Boolean.TRUE.equals(
                            environment.getFeatureDescriptor().getValue(APP_BAR_PREDICATE_ATTRIBUTE));
            elevatedButtonLayerBranch = ("styleBackgroundBuilder".equals(argumentName) || "styleForegroundBuilder".equals(argumentName))
                    && "ButtonLayerBuilder".equals(expectedDartType) && environment != null
                    && Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(ELEVATED_BUTTON_LAYER_ATTRIBUTE));
            textFieldBuilderBranch = ("buildCounter".equals(argumentName) && "InputCounterWidgetBuilder?".equals(expectedDartType)
                    || "contextMenuBuilder".equals(argumentName) && "EditableTextContextMenuBuilder?".equals(expectedDartType))
                    && environment != null && Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(TEXT_FIELD_BUILDER_ATTRIBUTE));
            listViewExtentBuilderBranch = "itemExtentBuilder".equals(argumentName) && "ItemExtentBuilder?".equals(expectedDartType)
                    && environment != null && Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(LIST_VIEW_EXTENT_BUILDER_ATTRIBUTE));
            noOpRefreshBranch = "onRefresh".equals(argumentName) && "RefreshCallback".equals(expectedDartType);
            buttonActivationBranch = "onPressed".equals(argumentName) && "VoidCallback".equals(expectedDartType)
                    || "onChanged".equals(argumentName) && ("ValueChanged<bool?>".equals(expectedDartType)
                            || "ValueChanged<bool>".equals(expectedDartType) || "ValueChanged<double>".equals(expectedDartType)
                            || "ValueChanged<RangeValues>".equals(expectedDartType));
            useDefault = new JCheckBox(elevatedButtonLayerBranch ? "Use theme/framework builder (omit local override)" : noOpRefreshBranch ? "Use generated no-op refresh callback"
                    : buttonActivationBranch ? "Use Designer activation default" : "Use Flutter default (omit " + argumentName + ")");

            setLayout(new BorderLayout(0, 8));
            setName(PANEL_NAME);
            setPreferredSize(textFieldBuilderBranch || listViewExtentBuilderBranch ? new Dimension(780, 470) : elevatedButtonLayerBranch ? new Dimension(800, 580) : bottomSheetScrimBranch ? new Dimension(780, 520) : new Dimension(650, 340));
            getAccessibleContext().setAccessibleName(
                    binding.definition().name().value()
                            + " Dart object reference editor");
            getAccessibleContext().setAccessibleDescription(description());

            useDefault.setName(DEFAULT_NAME);
            useDefault.getAccessibleContext().setAccessibleName(
                    elevatedButtonLayerBranch ? "Use theme/framework builder without this local override" : bottomSheetScrimBranch ? "Use Flutter default bottom-sheet scrim" : noOpRefreshBranch ? "Use generated no-op refresh callback" : buttonActivationBranch ? "Use Designer activation default" : "Use Flutter default without " + argumentDisplayName);
            useDefault.getAccessibleContext().setAccessibleDescription(
                    elevatedButtonLayerBranch ? "Omits only this local ButtonStyle builder. The theme may still supply a builder; reset does not force no layer or change the other builder."
                            : bottomSheetScrimBranch ? "Omits bottomSheetScrimBuilder and restores Flutter's built-in animated scrim; never sets the callback to null."
                            : noOpRefreshBranch ? "Removes the project reference and generates the required async no-op callback, not a null callback."
                            : buttonActivationBranch ? "Removes the project reference. The active widget contract determines the generated no-op or null callback from Enabled and other activation callbacks. See the property's help for the exact widget policy."
                            : "When selected, removes the optional " + argumentName + " Dart object reference.");
            if (binding.optional()) {
                add(useDefault, BorderLayout.NORTH);
            }

            JPanel form = new JPanel(new GridBagLayout());
            scope.setName(SCOPE_NAME);
            scope.getAccessibleContext().setAccessibleName("Dart object reference scope");
            scope.getAccessibleContext().setAccessibleDescription(
                    "Choose the paired Dart library or a declared package: library import.");
            libraryUri.setName(LIBRARY_URI_NAME);
            libraryUri.getAccessibleContext().setAccessibleName("Imported package library URI");
            libraryUri.getAccessibleContext().setAccessibleDescription(LIBRARY_DESCRIPTION);
            rootSymbol.setName(ROOT_SYMBOL_NAME);
            rootSymbol.getAccessibleContext().setAccessibleName("Dart root symbol");
            rootSymbol.getAccessibleContext().setAccessibleDescription(ROOT_DESCRIPTION);
            member.setName(MEMBER_NAME);
            member.getAccessibleContext().setAccessibleName("Optional Dart member");
            member.getAccessibleContext().setAccessibleDescription(MEMBER_DESCRIPTION);
            access.setName(ACCESS_NAME);
            access.getAccessibleContext().setAccessibleName("Dart object access");
            access.getAccessibleContext().setAccessibleDescription(ACCESS_DESCRIPTION);
            constant.setName(CONSTANT_NAME);
            constant.getAccessibleContext().setAccessibleName("Const invocation");
            constant.getAccessibleContext().setAccessibleDescription(CONSTANT_DESCRIPTION);
            preview.setName(PREVIEW_NAME);
            preview.getAccessibleContext().setAccessibleName(
                    "Generated " + argumentName + " reference preview");

            addRow(form, 0, "Scope:", scope);
            addRow(form, 1, "Package library URI:", libraryUri);
            addRow(form, 2, "Root symbol:", rootSymbol);
            addRow(form, 3, "Optional member:", member);
            addRow(form, 4, "Access:", access);
            addRow(form, 5, "Invocation:", constant);
            addRow(form, 6, "Preview:", preview);
            add(form, BorderLayout.CENTER);

            JTextArea note = new JTextArea(description(), textFieldBuilderBranch || listViewExtentBuilderBranch ? 8 : elevatedButtonLayerBranch ? 13 : bottomSheetScrimBranch ? 10 : 4, textFieldBuilderBranch || listViewExtentBuilderBranch || elevatedButtonLayerBranch || bottomSheetScrimBranch ? 76 : 56);
            note.setEditable(false);
            note.setFocusable(false);
            note.setOpaque(false);
            note.setLineWrap(true);
            note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName(
                    argumentDisplayName + " Dart reference validation and preview note");
            note.getAccessibleContext().setAccessibleDescription(description());
            add(note, BorderLayout.SOUTH);

            PropertyValue.DartObjectReferenceValue initial = initialValue()
                    .explicitValue()
                    .filter(PropertyValue.DartObjectReferenceValue.class::isInstance)
                    .map(PropertyValue.DartObjectReferenceValue.class::cast)
                    .orElse(null);
            updating = true;
            try {
                useDefault.setSelected(initial == null);
                scope.setSelectedItem(initial != null && initial.libraryUri().isPresent()
                        ? Scope.IMPORTED_LIBRARY : Scope.CURRENT_LIBRARY);
                libraryUri.setText(initial == null
                        ? "" : initial.libraryUri().orElse(""));
                rootSymbol.setText(initial == null ? "" : initial.rootSymbol());
                member.setText(initial == null ? "" : initial.member().orElse(""));
                access.setSelectedItem(initial == null
                        ? AccessChoice.REFERENCE : AccessChoice.from(initial.access()));
                constant.setSelected(initial != null
                        && initial.constant().orElse(false));
            } finally {
                updating = false;
            }

            useDefault.addActionListener(ignored -> refreshDraft(true));
            scope.addActionListener(ignored -> refreshDraft(true));
            access.addActionListener(ignored -> {
                if (!updating && selectedAccess() == AccessChoice.REFERENCE) {
                    constant.setSelected(false);
                }
                refreshDraft(true);
            });
            constant.addActionListener(ignored -> refreshDraft(true));
            libraryUri.getDocument().addDocumentListener(listener(() -> refreshDraft(true)));
            rootSymbol.getDocument().addDocumentListener(listener(() -> refreshDraft(true)));
            member.getDocument().addDocumentListener(listener(() -> refreshDraft(true)));

            refreshDraft(false);
            activate();
        }

        @Override
        boolean prepareCommit() {
            // JTextComponent document changes are synchronous. Re-reading all
            // controls here also captures the field that still owns focus when
            // NetBeans turns OK into STATE_VALID.
            return refreshDraft(false);
        }

        private boolean refreshDraft(boolean requestValidation) {
            if (updating) {
                return true;
            }
            boolean unset = binding.optional() && useDefault.isSelected();
            boolean imported = selectedScope() == Scope.IMPORTED_LIBRARY;
            boolean invocation = selectedAccess() == AccessChoice.INVOCATION;
            scope.setEnabled(!unset);
            libraryUri.setEnabled(!unset && imported);
            rootSymbol.setEnabled(!unset);
            member.setEnabled(!unset);
            access.setEnabled(!unset);
            constant.setEnabled(!unset && invocation);

            clearInvalid(libraryUri, LIBRARY_DESCRIPTION);
            clearInvalid(rootSymbol, ROOT_DESCRIPTION);
            clearInvalid(member, MEMBER_DESCRIPTION);
            clearInvalid(access, ACCESS_DESCRIPTION);
            clearInvalid(constant, CONSTANT_DESCRIPTION);
            updatePreview(unset, imported, invocation);

            if (unset) {
                return accept(FlutterPropertyCellValue.unset(), requestValidation);
            }
            if (imported && libraryUri.getText().strip().isEmpty()) {
                return reject(
                        "Enter a canonical package: URI declared by this project's package_config.json.",
                        libraryUri);
            }
            if (rootSymbol.getText().strip().isEmpty()) {
                return reject("Enter the Dart root symbol.", rootSymbol);
            }

            Optional<String> uri = imported
                    ? Optional.of(libraryUri.getText().strip()) : Optional.empty();
            Optional<String> optionalMember = member.getText().strip().isEmpty()
                    ? Optional.empty() : Optional.of(member.getText().strip());
            PropertyValue.DartObjectReferenceValue.Access modelAccess =
                    selectedAccess().access;
            Optional<Boolean> constness = invocation
                    ? Optional.of(constant.isSelected()) : Optional.empty();
            try {
                FlutterPropertyCellValue candidate = FlutterPropertyCellValue.explicit(
                        new PropertyValue.DartObjectReferenceValue(
                                uri,
                                rootSymbol.getText().strip(),
                                optionalMember,
                                modelAccess,
                                constness));
                binding.validate(candidate);
                return accept(candidate, requestValidation);
            } catch (IllegalArgumentException failure) {
                String message = Objects.requireNonNullElse(
                        failure.getMessage(), "Invalid Dart object reference.");
                JComponent target = message.toLowerCase(java.util.Locale.ROOT)
                        .contains("member") ? member
                        : message.toLowerCase(java.util.Locale.ROOT).contains("library")
                                ? libraryUri : rootSymbol;
                return reject(message, target);
            }
        }

        private boolean accept(
                FlutterPropertyCellValue candidate,
                boolean requestValidation) {
            if (requestValidation) {
                markValid(candidate);
            } else {
                stageValid(candidate);
            }
            return true;
        }

        private boolean reject(String message, JComponent component) {
            markInvalid(message, component);
            return false;
        }

        private void updatePreview(boolean unset, boolean imported, boolean invocation) {
            String rendered;
            if (unset) {
                rendered = noOpRefreshBranch ? "onRefresh: () async {}"
                        : buttonActivationBranch ? argumentName + ": <generated from Enabled and activation policy>"
                        : elevatedButtonLayerBranch ? argumentName + ": <theme/framework fallback; local builder omitted>"
                        : shapeBranch || bottomSheetScrimBranch ? argumentName + ": <Flutter default; argument omitted>"
                        : argumentName + ": <Flutter default null>";
            } else {
                String symbol = rootSymbol.getText().strip();
                if (symbol.isEmpty()) {
                    symbol = "<root symbol>";
                }
                String selectedMember = member.getText().strip();
                if (!selectedMember.isEmpty()) {
                    symbol += "." + selectedMember;
                }
                if (invocation) {
                    symbol = (constant.isSelected() ? "const " : "") + symbol + "()";
                }
                rendered = argumentName + ": " + symbol;
                if (imported) {
                    String uri = libraryUri.getText().strip();
                    rendered += "  [" + (uri.isEmpty() ? "package:…" : uri) + "]";
                } else {
                    rendered += "  [current Dart library]";
                }
            }
            preview.setText(rendered);
            preview.setToolTipText(rendered);
            preview.getAccessibleContext().setAccessibleDescription(
                    "Live generated Dart preview: " + rendered);
        }

        private Scope selectedScope() {
            return Objects.requireNonNull(
                    (Scope) scope.getSelectedItem(), "Dart reference scope");
        }

        private AccessChoice selectedAccess() {
            return Objects.requireNonNull(
                    (AccessChoice) access.getSelectedItem(), "Dart reference access");
        }

        private String description() {
            if (listViewExtentBuilderBranch) return listViewExtentBuilderDescription();
            if (textFieldBuilderBranch) return textFieldBuilderDescription(argumentName);
            if (elevatedButtonLayerBranch) {
                return "The analyzer requires a non-null ButtonLayerBuilder: Widget Function(BuildContext, Set<WidgetState>, Widget? child). "
                        + "The child may be null; the returned Widget must not be null. Choose a current-file or package reference, getter/member, or zero-argument factory returning the function. "
                        + ("styleBackgroundBuilder".equals(argumentName)
                                ? "The background builder wraps the whole button content, including padding, inside its Material. "
                                : "The foreground builder wraps the nullable child inside the button's padding and alignment. ")
                        + "Use theme/framework builder / Restore Default omits only this local override; a theme builder may remain active. "
                        + "With clipBehavior omitted, either effective local/theme builder makes the SDK use Clip.antiAlias; without either builder it uses Clip.none. An explicit clipBehavior wins. "
                        + "Canvas never executes your custom builder. Its structural placeholder keeps the editable child and builder-presence clipping, but custom layer geometry and clipping are approximate. "
                        + "The other builder, style leaves and user Dart body remain unchanged; Cancel publishes nothing.";
            }
            if (bottomSheetScrimBranch) {
                return "The analyzer requires a non-null Widget? Function(BuildContext, Animation<double>) callback. "
                        + "Choose a current-file or package reference, getter/member, or zero-argument factory returning that function. "
                        + "The animation is 0.0 at 70% bottom-sheet screen coverage and 1.0 at 100%. "
                        + "The builder may return null to show no scrim; setting the callback itself to null is invalid. "
                        + "Use Flutter default / Restore Default omits the argument and restores Flutter's built-in animated scrim. "
                        + "Canvas previews the SDK default only and never executes your custom builder. Cancel leaves the reference and user code unchanged.";
            }
            if (Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(CHECKBOX_TILE_CALLBACK_ATTRIBUTE))
                    && "onChanged".equals(argumentName) && "ValueChanged<bool?>".equals(expectedDartType)) {
                return "The analyzer verifies the required CheckboxListTile callback as ValueChanged<bool?>. "
                        + "Choose a non-null project reference, explicit No-op preset or explicit null; omission is not accepted. "
                        + "Enabled is independently nullable and never rewrites this callback. Isolated Canvas does not execute project callbacks.";
            }
            if (Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(SWITCH_TILE_CALLBACK_ATTRIBUTE))
                    && "onChanged".equals(argumentName) && "ValueChanged<bool>".equals(expectedDartType)) {
                return "The analyzer verifies the required SwitchListTile callback as ValueChanged<bool>. "
                        + "Choose a non-null reference/getter/member or zero-argument factory returning that function. "
                        + "No-op enables the tile without automatically changing its controlled Value; explicit null disables it. Omission is invalid. "
                        + "No Enabled or Tristate property is invented. Isolated Canvas never executes project callbacks.";
            }
            if (Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(RADIO_GROUP_CALLBACK_ATTRIBUTE))
                    && "onChanged".equals(argumentName) && "ValueChanged<Object?>".equals(expectedDartType)) {
                return "The analyzer checks the required RadioGroup callback against ValueChanged<T?> for the selected type. "
                        + "Select the explicit No-op preset or a strict non-null project reference; null and omission are not accepted. "
                        + "Isolated Canvas never executes project callbacks. Stored Group Value and descendant Radio values/types are not changed by a request.";
            }
            if (Boolean.TRUE.equals(environment.getFeatureDescriptor().getValue(RADIO_TILE_CALLBACK_ATTRIBUTE))
                    && "onChanged".equals(argumentName) && "ValueChanged<Object?>".equals(expectedDartType)) {
                return "The analyzer checks RadioListTile On changed against ValueChanged<T?> for the selected value type. "
                        + "Use a typed reference/getter/member or zero-argument factory returning the function; the source method is never rewritten when T changes. "
                        + "Modern RadioGroup is the preferred group owner. Legacy Group value and On changed remain explicit; omission, null and No-op are distinct. "
                        + "There is no groupRegistry field on RadioListTile. Canvas never invokes project callbacks.";
            }
            if ("controller".equals(argumentName) && "ExpansibleController".equals(expectedDartType)) {
                return "The analyzer verifies a non-null ExpansibleController reference, getter, member or zero-argument factory. "
                        + "Its owner creates, retains and disposes the controller; Designer does not synthesize lifecycle code or call expand/collapse. "
                        + "Initially expanded is only a seed. Control current expansion through the controller in application code, outside build; there is no generated two-way State binding. "
                        + "Canvas uses an isolated controller and never accesses the project instance. Omission/null permits SDK ownership. Cancel changes nothing.";
            }
            if (argumentName.startsWith("expansionAnimationStyle") && java.util.List.of("AnimationStyle", "Curve", "Duration").contains(expectedDartType)) {
                return "The analyzer verifies a non-null " + expectedDartType + " reference, getter, member or zero-argument factory. "
                        + "Custom styles, curves and durations are not executed in isolated Canvas. Omission and explicit null preserve SDK/theme fallback. "
                        + "The pinned ExpansionTile SDK ignores reverseDuration even though it is retained and generated. Whole/local AnimationStyle changes are atomic and preserve other families and all child slots.";
            }
            String tooltip = tooltipReferenceDescription(argumentName, expectedDartType);
            if (tooltip != null) return tooltip;
            String base = "The Dart analyzer validates that the selected Dart "
                    + "symbol is assignable to " + expectedDartType + ". ";
            if ("onNotification".equals(argumentName) && "NotificationListenerCallback<Notification>".equals(expectedDartType)) {
                base = "The analyzer checks this bool callback against NotificationListenerCallback<T> for the selected notification type. True stops bubbling; false, omission or explicit null continues. Canvas never executes it. ";
            } else if (("value".equals(argumentName) || "groupValue".equals(argumentName)) && "Object?".equals(expectedDartType)) {
                base = "The analyzer checks this Radio value against the selected type, including its nullable form where permitted. Dynamic references are rejected. ";
            } else if ("onChanged".equals(argumentName) && "ValueChanged<Object?>".equals(expectedDartType)) {
                base = "The analyzer checks the Radio callback against ValueChanged<T?> for the selected type. Null and omission are distinct from the explicit No-op preset. Enabled false preserves this callback. ";
            } else if ("groupRegistry".equals(argumentName) && "RadioGroupRegistry<Object>".equals(expectedDartType)) {
                base = "The analyzer checks the registry against the selected Radio type and its registerClient contract. Null or omission preserves inherited RadioGroup lookup. ";
            }
            if (noOpRefreshBranch) {
                base += "Omission generates the required onRefresh: () async {} no-op; it does not emit null or omit the required Dart argument. ";
            }
            if (buttonActivationBranch) {
                base += "Unset removes the project callback, not the required Dart argument. The active widget contract determines the generated no-op or null callback from Enabled and other activation callbacks. See the property's help for the exact widget policy. ";
            }
            if ("CustomClipper<RRect>".equals(expectedDartType)) {
                base += "When configured, Flutter ignores ClipRRect.borderRadius. ";
            } else if ("CustomClipper<RSuperellipse>".equals(expectedDartType)) {
                base += "When configured, Flutter ignores "
                        + "ClipRSuperellipse.borderRadius. ";
            } else if ("CustomClipper<Path>".equals(expectedDartType) && binding.optional()) {
                base += "Setting Clipper first clears Shape and selects the unnamed "
                        + "ClipPath constructor. ";
            } else if ("ShapeBorder".equals(expectedDartType)) {
                base += "A project-defined shape replaces any mutually exclusive shape configuration "
                        + "atomically. Omission restores the widget's Flutter/theme default. ";
            }
            if ("onChanged".equals(argumentName) && "ValueChanged<Object?>".equals(expectedDartType)) {
                return base + "Isolated Canvas retains the actual SDK Radio and never executes the project callback. An active callback is replaced by a benign controlled preview callback with a concrete diagnostic; disabled or inherited-group-ignored callbacks cause no false preview warning. Stored Value and Group value never change from gestures.";
            }
            if (noOpRefreshBranch) {
                return base + "Isolated Canvas retains the wrapper and editable child, but disables refresh activation and reports this project callback; it never fakes successful refresh.";
            }
            if ("onStatusChange".equals(argumentName) && "ValueChanged<RefreshIndicatorStatus?>".equals(expectedDartType)) {
                return base + "Isolated Canvas retains the wrapper and editable child, skips this observer with a diagnostic, and permits the SDK refresh cycle.";
            }
            if ("notificationPredicate".equals(argumentName) && "ScrollNotificationPredicate".equals(expectedDartType)) {
                if (appBarPredicateBranch) {
                    return base + "The non-null predicate is bool Function(ScrollNotification). Returning true accepts a notification for AppBar scrolled-under elevation; it does not stop notification propagation. Isolated Canvas never executes the project predicate and previews the SDK default depth-zero filter as an explicit approximation.";
                }
                return base + "Isolated Canvas retains the wrapper and editable child, but disables refresh activation and reports the custom predicate; it never substitutes another filter.";
            }
            return base
                    + "The isolated Canvas cannot execute project or dependency Dart and displays an "
                    + "explicit preview-unavailable state.";
        }

        private static void addRow(
                JPanel panel,
                int row,
                String labelText,
                JComponent component) {
            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = row;
            labelConstraints.anchor = GridBagConstraints.LINE_END;
            labelConstraints.insets = new Insets(3, 0, 3, 8);
            JLabel label = new JLabel(labelText);
            label.setLabelFor(component);
            panel.add(label, labelConstraints);

            GridBagConstraints valueConstraints = new GridBagConstraints();
            valueConstraints.gridx = 1;
            valueConstraints.gridy = row;
            valueConstraints.weightx = 1;
            valueConstraints.fill = GridBagConstraints.HORIZONTAL;
            valueConstraints.anchor = GridBagConstraints.LINE_START;
            valueConstraints.insets = new Insets(3, 0, 3, 0);
            panel.add(component, valueConstraints);
        }

        private static DocumentListener listener(Runnable operation) {
            return new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    operation.run();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    operation.run();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    operation.run();
                }
            };
        }
    }
}
