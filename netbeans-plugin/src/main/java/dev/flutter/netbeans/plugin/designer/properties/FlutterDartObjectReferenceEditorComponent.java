package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.model.PropertyValue;
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
            noOpRefreshBranch = "onRefresh".equals(argumentName) && "RefreshCallback".equals(expectedDartType);
            buttonActivationBranch = "onPressed".equals(argumentName) && "VoidCallback".equals(expectedDartType)
                    || "onChanged".equals(argumentName) && ("ValueChanged<bool?>".equals(expectedDartType)
                            || "ValueChanged<bool>".equals(expectedDartType) || "ValueChanged<double>".equals(expectedDartType)
                            || "ValueChanged<RangeValues>".equals(expectedDartType));
            useDefault = new JCheckBox(noOpRefreshBranch ? "Use generated no-op refresh callback"
                    : buttonActivationBranch ? "Use Designer activation default" : "Use Flutter default (omit " + argumentName + ")");

            setLayout(new BorderLayout(0, 8));
            setName(PANEL_NAME);
            setPreferredSize(new Dimension(650, 340));
            getAccessibleContext().setAccessibleName(
                    binding.definition().name().value()
                            + " Dart object reference editor");
            getAccessibleContext().setAccessibleDescription(description());

            useDefault.setName(DEFAULT_NAME);
            useDefault.getAccessibleContext().setAccessibleName(
                    noOpRefreshBranch ? "Use generated no-op refresh callback" : buttonActivationBranch ? "Use Designer activation default" : "Use Flutter default without " + argumentDisplayName);
            useDefault.getAccessibleContext().setAccessibleDescription(
                    noOpRefreshBranch ? "Removes the project reference and generates the required async no-op callback, not a null callback."
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

            JTextArea note = new JTextArea(description(), 4, 56);
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
                        : shapeBranch ? argumentName + ": <Flutter default; argument omitted>"
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
            String base = "The Dart analyzer validates that the selected Dart "
                    + "symbol is assignable to " + expectedDartType + ". ";
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
            if (noOpRefreshBranch) {
                return base + "Isolated Canvas retains the wrapper and editable child, but disables refresh activation and reports this project callback; it never fakes successful refresh.";
            }
            if ("onStatusChange".equals(argumentName) && "ValueChanged<RefreshIndicatorStatus?>".equals(expectedDartType)) {
                return base + "Isolated Canvas retains the wrapper and editable child, skips this observer with a diagnostic, and permits the SDK refresh cycle.";
            }
            if ("notificationPredicate".equals(argumentName) && "ScrollNotificationPredicate".equals(expectedDartType)) {
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
