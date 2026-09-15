package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.TableGrid;
import dev.flutter.netbeans.designer.catalog.DataTableGrid;
import dev.flutter.netbeans.designer.command.EditDataTableGrid;
import dev.flutter.netbeans.designer.model.*;
import java.awt.*;
import java.beans.PropertyEditorSupport;
import javax.swing.*;
import javax.swing.event.*;
import org.openide.explorer.propertysheet.*;

/** One cancel-safe, snapshot-fenced grid operation, applied through the normal command session. */
final class FlutterDataTableGridPropertyEditor extends PropertyEditorSupport implements ExPropertyEditor {
    private final WidgetNode table;
    private PropertyEnv environment;
    FlutterDataTableGridPropertyEditor(WidgetNode table) { this.table=table; }
    @Override public String getAsText() {
        var rows=DataTableGrid.children(table,DataTableGrid.ROWS);
        return (dev.flutter.netbeans.designer.catalog.PaginatedDataTableWidgetPropertySchema.TYPE.equals(table.type()) ? "Source-owned rows; " : rows.size()+" rows × ")+DataTableGrid.children(table,DataTableGrid.COLUMNS).size()+" columns";
    }
    @Override public void setAsText(String text) { throw new IllegalArgumentException("Use the table grid editor."); }
    @Override public boolean supportsCustomEditor() { return true; }
    @Override public void attachEnv(PropertyEnv env) { environment=env; }
    @Override public Component getCustomEditor() {
        var env=environment!=null?environment:PropertyEnv.create(new java.beans.FeatureDescriptor());
        JPanel panel=new JPanel(new GridLayout(0,2,8,8)); panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        panel.getAccessibleContext().setAccessibleName("Data table rows and columns");
        boolean paginated=dev.flutter.netbeans.designer.catalog.PaginatedDataTableWidgetPropertySchema.TYPE.equals(table.type());
        JComboBox<TableGrid.Operation> operation=new JComboBox<>(java.util.Arrays.stream(TableGrid.Operation.values())
                .filter(op -> !paginated || op.name().endsWith("COLUMN")).toArray(TableGrid.Operation[]::new));
        operation.setName("flutter.dataTableGrid.operation"); operation.getAccessibleContext().setAccessibleName("Grid operation");
        int count=DataTableGrid.children(table,DataTableGrid.ROWS).size();
        int columns=DataTableGrid.children(table,DataTableGrid.COLUMNS).size();
        // Existing larger source-authored grids must still open for removal/reorder.
        int maximum=Math.max(1000,Math.max(count,columns));
        JSpinner index=new JSpinner(new SpinnerNumberModel(paginated?columns:count,0,maximum,1));
        JSpinner destination=new JSpinner(new SpinnerNumberModel(0,0,maximum,1));
        index.setName("flutter.dataTableGrid.index"); destination.setName("flutter.dataTableGrid.destination");
        index.getAccessibleContext().setAccessibleName("Zero-based row or column index");
        destination.getAccessibleContext().setAccessibleName("Post-removal destination index");
        JLabel status=new JLabel(); status.getAccessibleContext().setAccessibleName("Grid validation");
        panel.add(new JLabel(getAsText())); panel.add(new JLabel("Indices are zero-based."));
        panel.add(new JLabel("Operation:")); panel.add(operation); panel.add(new JLabel("Index:")); panel.add(index);
        panel.add(new JLabel("Move destination:")); panel.add(destination);
        panel.add(new JLabel(paginated?"Update source cells to match changed columns.":"Removal deletes complete rows/columns.")); panel.add(status);
        StableId seed=StableId.random();
        EditDataTableGrid[] draft=new EditDataTableGrid[1];
        Runnable validate=()->{
            try {
                var op=(TableGrid.Operation)operation.getSelectedItem();
                destination.setEnabled(op==TableGrid.Operation.MOVE_ROW||op==TableGrid.Operation.MOVE_COLUMN);
                var command=new EditDataTableGrid(table,op,((Number)index.getValue()).intValue(),((Number)destination.getValue()).intValue(),seed);
                var result=DataTableGrid.edit(table,op,command.index(),command.destination(),seed);
                DataTableGrid.relationshipError(result).ifPresent(message->{throw new IllegalArgumentException(message);});
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
