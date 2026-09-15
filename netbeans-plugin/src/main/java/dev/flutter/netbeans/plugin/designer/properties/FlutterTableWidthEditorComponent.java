package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.TableColumnWidths;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.*;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Structured six-family width editor. Nested dialogs edit local drafts only. */
final class FlutterTableWidthEditorComponent extends FlutterPropertyEditorComponents.CommitOnValidPanel {
    private final boolean mapping;
    private final SortedMap<Integer,TableColumnWidths.Width> widths=new TreeMap<>();
    private final DefaultListModel<Integer> entries=new DefaultListModel<>();
    private final JList<Integer> list=new JList<>(entries);
    private final JLabel status=new JLabel();
    private final WidthControl single;

    FlutterTableWidthEditorComponent(PropertyEditor editor,FlutterTypedPropertyEditors.Binding binding,PropertyEnv environment) {
        super(editor,binding,environment);
        mapping=binding.definition().name().value().equals("columnWidths");
        String initial=((PropertyValue.StringValue)initialValue().explicitValue().orElseThrow()).value();
        setLayout(new BorderLayout(8,8)); setName("flutter.tableWidth.editor");
        getAccessibleContext().setAccessibleName(mapping ? "Indexed table column widths" : "Default table column width");
        if(mapping) {
            widths.putAll(TableColumnWidths.parseMap(initial)); single=null;
            widths.keySet().forEach(entries::addElement);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer((owner,value,index,selected,focus) -> {
                JLabel label=new JLabel("Column "+value+"  —  "+widths.get(value).encode());
                label.setOpaque(true); label.setBackground(selected ? owner.getSelectionBackground():owner.getBackground());
                label.setForeground(selected ? owner.getSelectionForeground():owner.getForeground()); return label;
            });
            list.getAccessibleContext().setAccessibleName("Column width overrides");
            add(new JScrollPane(list),BorderLayout.CENTER);
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEADING));
            for(String label:java.util.List.of("Add column width","Edit width","Remove override")) {
                JButton button=new JButton(label); buttons.add(button);
                button.addActionListener(e -> {
                    if(label.equals("Remove override")) {
                        Integer index=list.getSelectedValue(); if(index!=null) { widths.remove(index); entries.removeElement(index); refresh(); } return;
                    }
                    if(label.equals("Edit width")) {
                        Integer index=list.getSelectedValue(); if(index==null) return;
                        var control=new WidthControl(widths.get(index),0,()->{});
                        if(editDialog(control,"Column "+index+" width")) { widths.put(index,control.value()); list.repaint(); refresh(); } return;
                    }
                    JSpinner index=new JSpinner(new SpinnerNumberModel(0,0,9999,1));
                    index.getAccessibleContext().setAccessibleName("Zero-based column index");
                    WidthControl control=new WidthControl(TableColumnWidths.parse("flex(1)"),0,()->{});
                    JPanel panel=new JPanel(new BorderLayout()); JPanel top=new JPanel(); top.add(new JLabel("Column index:")); top.add(index);
                    panel.add(top,BorderLayout.NORTH); panel.add(control,BorderLayout.CENTER);
                    while(JOptionPane.showConfirmDialog(this,panel,"Add column width",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
                        try {
                            index.commitEdit(); int i=((Number)index.getValue()).intValue();
                            if(widths.containsKey(i)) throw new IllegalArgumentException("Column "+i+" already has an override; use Edit width.");
                            var draft=new TreeMap<>(widths); draft.put(i,control.value());
                            TableColumnWidths.parseMap(TableColumnWidths.encodeMap(draft));
                            widths.put(i,control.value()); entries.clear(); widths.keySet().forEach(entries::addElement); list.setSelectedValue(i,true); refresh(); break;
                        } catch(Exception ex) { JOptionPane.showMessageDialog(this,ex.getMessage(),"Invalid width",JOptionPane.ERROR_MESSAGE); }
                    }
                });
            }
            add(buttons,BorderLayout.NORTH);
        } else {
            single=new WidthControl(TableColumnWidths.parse(initial),0,this::refresh);
            add(single,BorderLayout.NORTH);
        }
        status.setText("Widths are finite; flex factors must be positive. Unspecified columns use the table default.");
        add(status,BorderLayout.SOUTH); activate(); refresh();
    }
    private boolean editDialog(WidthControl control,String title) {
        while(JOptionPane.showConfirmDialog(this,control,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try { TableColumnWidths.parse(control.value().encode()); return true; }
            catch(IllegalArgumentException ex) { JOptionPane.showMessageDialog(this,ex.getMessage(),"Invalid width",JOptionPane.ERROR_MESSAGE); }
        }
        return false;
    }
    private void refresh() {
        try {
            String value=mapping ? TableColumnWidths.encodeMap(widths) : single.value().encode();
            if(mapping) TableColumnWidths.parseMap(value); else TableColumnWidths.parse(value);
            markValid(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(value)));
            clearInvalid(status,"Validated table width draft"); status.setText(value.isEmpty() ? "No overrides; all columns use the default width." : value);
        } catch(IllegalArgumentException ex) { markInvalid(ex.getMessage(),status); status.setText(ex.getMessage()); }
    }
    @Override boolean prepareCommit() { refresh(); return environment.getState()!=PropertyEnv.STATE_INVALID; }

    private final class WidthControl extends JPanel {
        final JComboBox<TableColumnWidths.Family> family=new JComboBox<>(TableColumnWidths.Family.values());
        final JTextField number=new JTextField(12);
        final JCheckBox intrinsicFlex=new JCheckBox("Use intrinsic flex factor");
        final JButton first=new JButton("Edit first width"), second=new JButton("Edit second width");
        TableColumnWidths.Width a=TableColumnWidths.parse("fixed(40)"),b=TableColumnWidths.parse("intrinsic()");
        final int depth; final Runnable changed;
        WidthControl(TableColumnWidths.Width initial,int depth,Runnable changed) {
            super(new GridLayout(0,2,8,8)); this.depth=depth; this.changed=changed;
            family.setName("flutter.tableWidth.family"); family.getAccessibleContext().setAccessibleName("Width family");
            number.setName("flutter.tableWidth.number"); number.getAccessibleContext().setAccessibleName("Width or flex factor");
            family.setSelectedItem(initial.family()); number.setText(initial.value().map(BigDecimal::toString).orElse("1"));
            intrinsicFlex.setSelected(initial.value().isPresent()); initial.a().ifPresent(v->a=v); initial.b().ifPresent(v->b=v);
            add(new JLabel("Width family:")); add(family); add(new JLabel("Pixels / fraction / flex:")); add(number);
            add(intrinsicFlex); add(new JLabel()); add(first); add(second);
            family.addActionListener(e->update()); intrinsicFlex.addActionListener(e->update());
            number.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e){changed.run();} public void removeUpdate(DocumentEvent e){changed.run();} public void changedUpdate(DocumentEvent e){changed.run();}
            });
            first.addActionListener(e->nested(true)); second.addActionListener(e->nested(false));
            updateEnabled();
        }
        private void nested(boolean left) {
            var control=new WidthControl(left?a:b,depth+1,()->{});
            if(editDialog(control,left?"First width":"Second width")) { if(left)a=control.value();else b=control.value(); changed.run(); }
        }
        private void update() { updateEnabled(); changed.run(); }
        private void updateEnabled() {
            var f=(TableColumnWidths.Family)family.getSelectedItem();
            boolean pair=f==TableColumnWidths.Family.MIN||f==TableColumnWidths.Family.MAX;
            number.setEnabled(!pair&&(f!=TableColumnWidths.Family.INTRINSIC||intrinsicFlex.isSelected()));
            intrinsicFlex.setEnabled(f==TableColumnWidths.Family.INTRINSIC);
            first.setEnabled(pair&&depth<TableColumnWidths.MAX_DEPTH); second.setEnabled(first.isEnabled());
        }
        TableColumnWidths.Width value() {
            var f=(TableColumnWidths.Family)family.getSelectedItem();
            boolean pair=f==TableColumnWidths.Family.MIN||f==TableColumnWidths.Family.MAX;
            // Parse the bounded grammar before constructing numbers; a pasted exponent
            // must never expand into an unbounded BigDecimal.toPlainString allocation.
            String argument=pair ? a.encode()+","+b.encode()
                    : f==TableColumnWidths.Family.INTRINSIC&&!intrinsicFlex.isSelected() ? "" : number.getText().trim();
            return TableColumnWidths.parse(f.name().toLowerCase(Locale.ROOT)+"("+argument+")");
        }
    }
}
