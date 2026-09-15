package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.TableGrid;
import dev.flutter.netbeans.designer.command.EditTableGrid;
import dev.flutter.netbeans.designer.model.*;
import java.awt.*;
import java.beans.PropertyEditorSupport;
import javax.swing.*;
import javax.swing.event.*;
import org.openide.explorer.propertysheet.*;

/** One cancel-safe, snapshot-fenced grid operation, applied through the normal command session. */
final class FlutterTableGridPropertyEditor extends PropertyEditorSupport implements ExPropertyEditor {
    private final WidgetNode table;
    private PropertyEnv environment;
    FlutterTableGridPropertyEditor(WidgetNode table) { this.table=table; }
    @Override public String getAsText() {
        var rows=TableGrid.children(table);
        return rows.size()+" rows × "+(rows.isEmpty()?0:TableGrid.children(rows.getFirst()).size())+" columns";
    }
    @Override public void setAsText(String text) { throw new IllegalArgumentException("Use the table grid editor."); }
    @Override public boolean supportsCustomEditor() { return true; }
    @Override public void attachEnv(PropertyEnv env) { environment=env; }
    @Override public Component getCustomEditor() {
        var env=environment!=null?environment:PropertyEnv.create(new java.beans.FeatureDescriptor());
        JPanel panel=new JPanel(new GridLayout(0,2,8,8)); panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        panel.getAccessibleContext().setAccessibleName("Table rows and columns");
        JComboBox<TableGrid.Operation> operation=new JComboBox<>(TableGrid.Operation.values());
        operation.setName("flutter.tableGrid.operation"); operation.getAccessibleContext().setAccessibleName("Grid operation");
        int count=TableGrid.children(table).size();
        int columns=count==0?0:TableGrid.children(TableGrid.children(table).getFirst()).size();
        // Existing larger source-authored grids must still open for removal/reorder.
        int maximum=Math.max(1000,Math.max(count,columns));
        JSpinner index=new JSpinner(new SpinnerNumberModel(count,0,maximum,1));
        JSpinner destination=new JSpinner(new SpinnerNumberModel(0,0,maximum,1));
        index.setName("flutter.tableGrid.index"); destination.setName("flutter.tableGrid.destination");
        index.getAccessibleContext().setAccessibleName("Zero-based row or column index");
        destination.getAccessibleContext().setAccessibleName("Post-removal destination index");
        JLabel status=new JLabel(); status.getAccessibleContext().setAccessibleName("Grid validation");
        panel.add(new JLabel(getAsText())); panel.add(new JLabel("Indices are zero-based."));
        panel.add(new JLabel("Operation:")); panel.add(operation); panel.add(new JLabel("Index:")); panel.add(index);
        panel.add(new JLabel("Move destination:")); panel.add(destination);
        panel.add(new JLabel("Removal deletes complete rows/columns.")); panel.add(status);
        StableId seed=StableId.random();
        EditTableGrid[] draft=new EditTableGrid[1];
        Runnable validate=()->{
            try {
                var op=(TableGrid.Operation)operation.getSelectedItem();
                destination.setEnabled(op==TableGrid.Operation.MOVE_ROW||op==TableGrid.Operation.MOVE_COLUMN);
                var command=new EditTableGrid(table,op,((Number)index.getValue()).intValue(),((Number)destination.getValue()).intValue(),seed);
                var result=TableGrid.edit(table,op,command.index(),command.destination(),seed);
                TableGrid.relationshipError(result).ifPresent(message->{throw new IllegalArgumentException(message);});
                draft[0]=command; status.setText("Ready; OK applies one undoable change."); env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
            } catch(IllegalArgumentException ex) { draft[0]=null; status.setText(ex.getMessage()); env.setState(PropertyEnv.STATE_INVALID); }
        };
        operation.addActionListener(e->validate.run()); index.addChangeListener(e->validate.run()); destination.addChangeListener(e->validate.run());
        // Commit spinner text before NetBeans reads the result of OK.
        env.addVetoableChangeListener(e->{
            if(PropertyEnv.PROP_STATE.equals(e.getPropertyName())&&e.getNewValue()==PropertyEnv.STATE_VALID) {
                try { index.commitEdit(); destination.commitEdit(); validate.run(); if(draft[0]==null) throw new IllegalArgumentException(status.getText()); setValue(draft[0]); }
                catch(Exception ex) { throw new java.beans.PropertyVetoException(ex.getMessage(),e); }
            }
        });
        validate.run(); return panel;
    }
}
