package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.*;
import java.beans.PropertyEditor;
import javax.swing.*;
import javax.swing.event.*;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Two local Gregorian endpoints; all edits are staged until the owning dialog commits. */
final class FlutterDateRangeEditorComponent extends FlutterPropertyEditorComponents.CommitOnValidPanel {
    private final JTextField start = new JTextField(12), end = new JTextField(12);
    private final JLabel status = new JLabel();
    FlutterDateRangeEditorComponent(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        super(editor,binding,environment);
        var range = DateRangePickerDialogWidgetPropertySchema.range(((PropertyValue.StringValue)initialValue().explicitValue().orElseThrow()).value());
        start.setText(range.start().toString()); end.setText(range.end().toString());
        setName("flutter.dateRange.editor"); setLayout(new BorderLayout(8,8));
        getAccessibleContext().setAccessibleName("Inclusive Gregorian date range");
        var fields = new JPanel(new GridLayout(0,2,8,8));
        field(fields,start,"Start date (YYYY-MM-DD):","flutter.dateRange.start");
        field(fields,end,"End date (YYYY-MM-DD):","flutter.dateRange.end");
        add(fields,BorderLayout.NORTH); add(status,BorderLayout.SOUTH);
        var listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        };
        start.getDocument().addDocumentListener(listener); end.getDocument().addDocumentListener(listener);
        activate(); refresh();
    }
    private static void field(JPanel panel,JTextField field,String text,String name) {
        var label=new JLabel(text); label.setLabelFor(field); field.setName(name);
        field.getAccessibleContext().setAccessibleName(text);
        field.getAccessibleContext().setAccessibleDescription("Gregorian date, years 0001 to 9999. Start and end are inclusive.");
        panel.add(label); panel.add(field);
    }
    private void refresh() {
        try {
            String text=start.getText()+"/"+end.getText();
            DateRangePickerDialogWidgetPropertySchema.range(text);
            markValid(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(text)));
            clearInvalid(status,"Inclusive range; start must be on or before end.");
            status.setText("Inclusive range; start must be on or before end.");
        } catch(IllegalArgumentException invalid) {
            markInvalid(invalid.getMessage(),status); status.setText(invalid.getMessage());
        }
    }
    @Override boolean prepareCommit() { refresh(); return environment.getState()!=PropertyEnv.STATE_INVALID; }
}
