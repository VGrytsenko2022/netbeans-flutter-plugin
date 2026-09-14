package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValue.PointerDeviceKindSetValue;
import dev.flutter.netbeans.designer.model.PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyEditor;
import java.util.EnumMap;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Closed nullable Set<PointerDeviceKind>; no typed Dart text or implicit null/empty conversion. */
final class FlutterPointerDeviceKindSetEditorComponent {
    static final String MODE_NAME = "flutter.pointerDevices.mode";
    static final String VALUE_PREFIX = "flutter.pointerDevices.value.";
    static final String PREVIEW_NAME = "flutter.pointerDevices.preview";
    static final String CLEAR_NAME = "flutter.pointerDevices.clear";
    static final String OMIT = "Use constructor default (omit argument)";
    static final String NULL = "Explicit null (all device kinds)";
    static final String SET = "Explicit set (checked device kinds only)";

    private FlutterPointerDeviceKindSetEditorComponent() { }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new DeviceSetPanel(editor, binding, environment);
    }

    private static final class DeviceSetPanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final EnumMap<PointerDeviceKind, JCheckBox> devices = new EnumMap<>(PointerDeviceKind.class);
        private final JTextArea preview = new JTextArea(4, 52);
        private final JButton clear = new JButton("Clear set");

        DeviceSetPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(8, 8));
            setPreferredSize(new Dimension(620, 300));
            setName("flutter.pointerDevices.editor");
            getAccessibleContext().setAccessibleName("Supported pointer device kinds");
            getAccessibleContext().setAccessibleDescription("Choose omission, explicit null, or a checkbox set of device kinds. An empty set accepts no device kinds. Only OK commits; Cancel preserves the previous value.");
            mode = new JComboBox<>(binding.optional() ? new String[] {OMIT, NULL, SET} : new String[] {NULL, SET});
            mode.setName(MODE_NAME);
            JLabel source = new JLabel("Source:"); source.setLabelFor(mode);
            JPanel top = new JPanel(new BorderLayout(8, 0)); top.add(source, BorderLayout.WEST); top.add(mode, BorderLayout.CENTER);
            add(top, BorderLayout.NORTH);
            JPanel choices = new JPanel(new GridLayout(0, 2, 8, 8));
            for (PointerDeviceKind kind : PointerDeviceKind.values()) {
                JCheckBox choice = new JCheckBox(switch (kind) {
                    case TOUCH -> "Touch"; case MOUSE -> "Mouse"; case STYLUS -> "Stylus";
                    case INVERTED_STYLUS -> "Inverted stylus"; case TRACKPAD -> "Trackpad"; case UNKNOWN -> "Unknown";
                });
                choice.setName(VALUE_PREFIX + kind.wireName());
                choice.getAccessibleContext().setAccessibleDescription("PointerDeviceKind." + kind.wireName());
                choice.addActionListener(ignored -> refresh(true));
                devices.put(kind, choice); choices.add(choice);
            }
            clear.setName(CLEAR_NAME);
            clear.getAccessibleContext().setAccessibleDescription("Uncheck every device kind while keeping an explicit empty set; does not select null or omission.");
            clear.addActionListener(ignored -> { devices.values().forEach(value -> value.setSelected(false)); refresh(true); });
            JPanel center = new JPanel(new BorderLayout(8, 8)); center.add(choices, BorderLayout.CENTER); center.add(clear, BorderLayout.SOUTH);
            add(center, BorderLayout.CENTER);
            preview.setName(PREVIEW_NAME); preview.setEditable(false); preview.setOpaque(false); preview.setLineWrap(true); preview.setWrapStyleWord(true);
            preview.getAccessibleContext().setAccessibleName("Typed device-set value and semantics"); add(preview, BorderLayout.SOUTH);
            PropertyValue initial = initialValue().explicitValue().orElse(null);
            if (initial instanceof PointerDeviceKindSetValue set) set.values().forEach(value -> devices.get(value).setSelected(true));
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : NULL : initial instanceof PropertyValue.NullValue ? NULL : SET);
            mode.addActionListener(ignored -> refresh(true));
            activate(); refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            boolean explicit = SET.equals(mode.getSelectedItem());
            devices.values().forEach(value -> value.setEnabled(explicit)); clear.setEnabled(explicit);
            List<PointerDeviceKind> selected = devices.entrySet().stream().filter(value -> value.getValue().isSelected()).map(java.util.Map.Entry::getKey).toList();
            FlutterPropertyCellValue candidate = OMIT.equals(mode.getSelectedItem()) ? FlutterPropertyCellValue.unset()
                    : NULL.equals(mode.getSelectedItem()) ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())
                    : FlutterPropertyCellValue.explicit(new PointerDeviceKindSetValue(selected));
            String description = candidate.isExplicit() ? PropertyValueFormatter.format(candidate.explicitValue().orElseThrow()) : "<not set> — argument omitted";
            description += "\nOmission and explicit null both permit all device kinds in Flutter, but remain distinct stored values. An explicit empty set accepts no device kinds. Runtime gestures use this filter; Designer selection remains available.";
            preview.setText(description);
            clearInvalid(preview, description);
            if (requestValidation) markValid(candidate); else stageValid(candidate);
            return true;
        }
    }
}
