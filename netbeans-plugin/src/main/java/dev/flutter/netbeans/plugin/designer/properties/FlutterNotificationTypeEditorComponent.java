package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.NotificationListenerWidgetPropertySchema;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyEditor;
import java.util.Optional;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Required closed notification type identity, never a raw Dart expression. */
final class FlutterNotificationTypeEditorComponent {
    static final String MODE_NAME = "flutter.notificationType.mode";
    static final String PRESET_NAME = "flutter.notificationType.preset";
    static final String LIBRARY_NAME = "flutter.notificationType.library";
    static final String SYMBOL_NAME = "flutter.notificationType.symbol";
    static final String NOTE_NAME = "flutter.notificationType.note";
    static final String BUILTIN = "Flutter notification type";
    static final String CURRENT = "Current-file class or typedef";
    static final String PACKAGE = "Package class or typedef";
    private FlutterNotificationTypeEditorComponent() { }
    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new TypePanel(editor, binding, environment);
    }
    private static final class TypePanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode = new JComboBox<>(new String[]{BUILTIN, CURRENT, PACKAGE});
        private final JComboBox<String> preset = new JComboBox<>(NotificationListenerWidgetPropertySchema.notificationTypes().toArray(String[]::new));
        private final JTextField library = new JTextField(38), symbol = new JTextField(32);
        private final JTextArea note = new JTextArea(5, 58);
        TypePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(8, 8)); setPreferredSize(new Dimension(700, 300));
            getAccessibleContext().setAccessibleName("NotificationListener notification type editor");
            getAccessibleContext().setAccessibleDescription("Choose a reviewed Flutter type or a simple project class/typedef that extends Notification. No raw expressions or nullable types. Cancel changes nothing.");
            mode.setName(MODE_NAME); preset.setName(PRESET_NAME); library.setName(LIBRARY_NAME); symbol.setName(SYMBOL_NAME); note.setName(NOTE_NAME);
            mode.getAccessibleContext().setAccessibleName("Notification type source"); preset.getAccessibleContext().setAccessibleName("Flutter notification type");
            library.getAccessibleContext().setAccessibleName("Type package library"); symbol.getAccessibleContext().setAccessibleName("Notification class or typedef symbol");
            var form = new JPanel(new GridLayout(0, 2, 8, 8));
            row(form, "Source:", mode); row(form, "Flutter type:", preset); row(form, "Package library:", library); row(form, "Type symbol:", symbol);
            add(form, BorderLayout.CENTER); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true); add(note, BorderLayout.SOUTH);
            var initial = initialValue().explicitValue().orElseGet(() -> new PropertyValue.StringValue("Notification"));
            if (initial instanceof PropertyValue.StringValue value) preset.setSelectedItem(value.value());
            else if (initial instanceof PropertyValue.DartObjectReferenceValue value) {
                library.setText(value.libraryUri().orElse("")); symbol.setText(value.rootSymbol());
                mode.setSelectedItem(value.libraryUri().isPresent() ? PACKAGE : CURRENT);
            }
            mode.addActionListener(ignored -> refresh(true)); preset.addActionListener(ignored -> refresh(true));
            var listener = new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent event) { refresh(true); }
                @Override public void removeUpdate(DocumentEvent event) { refresh(true); }
                @Override public void changedUpdate(DocumentEvent event) { refresh(true); }
            };
            library.getDocument().addDocumentListener(listener); symbol.getDocument().addDocumentListener(listener);
            activate(); refresh(true);
        }
        private static void row(JPanel form, String title, JComponent field) {
            var label = new JLabel(title); label.setLabelFor(field); form.add(label); form.add(field);
        }
        @Override boolean prepareCommit() { return refresh(false); }
        private boolean refresh(boolean requestValidation) {
            boolean builtin = BUILTIN.equals(mode.getSelectedItem()), imported = PACKAGE.equals(mode.getSelectedItem());
            preset.setEnabled(builtin); library.setEnabled(imported); symbol.setEnabled(!builtin);
            String description = "T must be a non-nullable subtype of Notification, verified by the Dart analyzer. "
                    + "Changing T retains the existing handler unchanged and rechecks its compatibility; it never rewrites user code. "
                    + "Name a complex generic type with a project typedef. OK applies one type edit; Cancel changes nothing.";
            note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
            try {
                PropertyValue value = builtin ? new PropertyValue.StringValue((String) preset.getSelectedItem())
                        : new PropertyValue.DartObjectReferenceValue(imported ? Optional.of(library.getText().strip()) : Optional.empty(),
                                symbol.getText().strip(), Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
                var result = FlutterPropertyCellValue.explicit(value);
                clearInvalid(note, description);
                if (requestValidation) markValid(result); else stageValid(result);
                return true;
            } catch (IllegalArgumentException failure) { markInvalid("Invalid notification type: " + failure.getMessage(), note); return false; }
        }
    }
}
