package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.TimePickerDialogWidgetPropertySchema;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.*;
import java.beans.PropertyEditor;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.*;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Staged hour/minute input; neither field commits until the owner confirms. */
final class FlutterTimeOfDayEditorComponent extends FlutterPropertyEditorComponents.CommitOnValidPanel {
    private final JTextField hour=new JTextField(3),minute=new JTextField(3);
    private final JLabel status=new JLabel();
    FlutterTimeOfDayEditorComponent(PropertyEditor editor,FlutterTypedPropertyEditors.Binding binding,PropertyEnv environment) {
        super(editor,binding,environment);
        var time=TimePickerDialogWidgetPropertySchema.time(((PropertyValue.StringValue)initialValue().explicitValue().orElseThrow()).value());
        hour.setText(String.format(Locale.ROOT,"%02d",time.getHour()));minute.setText(String.format(Locale.ROOT,"%02d",time.getMinute()));
        setName("flutter.timeOfDay.editor");setLayout(new BorderLayout(8,8));
        getAccessibleContext().setAccessibleName("Time of day");
        var fields=new JPanel(new GridLayout(0,2,8,8));
        field(fields,hour,"Hour (0–23):","flutter.timeOfDay.hour");field(fields,minute,"Minute (0–59):","flutter.timeOfDay.minute");
        add(fields,BorderLayout.NORTH);add(status,BorderLayout.SOUTH);
        var listener=new DocumentListener(){
            public void insertUpdate(DocumentEvent e){refresh();}
            public void removeUpdate(DocumentEvent e){refresh();}
            public void changedUpdate(DocumentEvent e){refresh();}
        };
        hour.getDocument().addDocumentListener(listener);minute.getDocument().addDocumentListener(listener);
        activate();refresh();
    }
    private static void field(JPanel panel,JTextField field,String text,String name) {
        var label=new JLabel(text);label.setLabelFor(field);field.setName(name);
        field.getAccessibleContext().setAccessibleName(text);
        field.getAccessibleContext().setAccessibleDescription("24-hour local time, without seconds, date or timezone.");
        panel.add(label);panel.add(field);
    }
    private void refresh() {
        try {
            if(!hour.getText().matches("[0-9]{1,2}")||!minute.getText().matches("[0-9]{1,2}"))
                throw new IllegalArgumentException("Enter numeric hours 0–23 and minutes 0–59.");
            String value=String.format(Locale.ROOT,"%02d:%02d",Integer.parseInt(hour.getText()),Integer.parseInt(minute.getText()));
            TimePickerDialogWidgetPropertySchema.time(value);
            markValid(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(value)));
            clearInvalid(status,"Stored as HH:mm; presentation follows the application's locale and 12/24-hour settings.");
            status.setText("Stored as HH:mm; presentation follows the application's locale and 12/24-hour settings.");
        } catch(IllegalArgumentException invalid){markInvalid(invalid.getMessage(),status);status.setText(invalid.getMessage());}
    }
    @Override boolean prepareCommit(){refresh();return environment.getState()!=PropertyEnv.STATE_INVALID;}
}
