package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.DartNumericLiterals;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Transactional, typed editors for Container's structured Flutter values. */
final class FlutterContainerPropertyEditorComponents {
    static final String ALIGNMENT_BASIS_NAME = "flutter.container.alignment.basis";
    static final String ALIGNMENT_HORIZONTAL_NAME = "flutter.container.alignment.horizontal";
    static final String ALIGNMENT_VERTICAL_NAME = "flutter.container.alignment.vertical";
    static final String CONSTRAINTS_MIN_WIDTH_NAME = "flutter.container.constraints.minWidth";
    static final String CONSTRAINTS_MAX_WIDTH_NAME = "flutter.container.constraints.maxWidth";
    static final String CONSTRAINTS_UNBOUNDED_WIDTH_NAME =
            "flutter.container.constraints.unboundedWidth";
    static final String CONSTRAINTS_EXPANDING_WIDTH_NAME =
            "flutter.container.constraints.expandingWidth";
    static final String CONSTRAINTS_MIN_HEIGHT_NAME = "flutter.container.constraints.minHeight";
    static final String CONSTRAINTS_MAX_HEIGHT_NAME = "flutter.container.constraints.maxHeight";
    static final String CONSTRAINTS_UNBOUNDED_HEIGHT_NAME =
            "flutter.container.constraints.unboundedHeight";
    static final String CONSTRAINTS_EXPANDING_HEIGHT_NAME =
            "flutter.container.constraints.expandingHeight";
    static final String MATRIX_TABLE_NAME = "flutter.container.matrix.table";
    static final String DECORATION_TABS_NAME = "flutter.container.decoration.tabs";
    static final String DECORATION_COLOR_MODE_NAME = "flutter.container.decoration.color.mode";
    static final String DECORATION_IMAGE_ENABLED_NAME =
            "flutter.container.decoration.image.enabled";
    static final String DECORATION_IMAGE_ASSET_NAME =
            "flutter.container.decoration.image.asset";
    static final String DECORATION_IMAGE_PROVIDER_NAME =
            "flutter.container.decoration.image.provider";
    static final String DECORATION_IMAGE_FILTER_NAME =
            "flutter.container.decoration.image.colorFilter";
    static final String DECORATION_BORDER_TABLE_NAME = "flutter.container.decoration.border.table";
    static final String DECORATION_RADIUS_TABLE_NAME = "flutter.container.decoration.radius.table";
    static final String BORDER_RADIUS_BASIS_NAME = "flutter.borderRadius.basis";
    static final String BORDER_RADIUS_TABLE_NAME = "flutter.borderRadius.table";
    static final String DECORATION_SHADOW_TABLE_NAME = "flutter.container.decoration.shadow.table";
    static final String DECORATION_GRADIENT_TABLE_NAME = "flutter.container.decoration.gradient.table";

    private static final String NOT_SET = "Not set";
    private static final String LITERAL = "Literal ARGB";
    private static final String THEME = "Theme role";
    private static final String ACCESSIBLE_DESCRIPTION_BASELINE =
            "flutter.container.accessibleDescriptionBaseline";

    private FlutterContainerPropertyEditorComponents() {
    }

    static Component customEditor(
            PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding,
            PropertyEnv environment) {
        return switch (binding.editorKind()) {
            case ALIGNMENT_GEOMETRY -> new AlignmentPanel(editor, binding, environment);
            case BOX_CONSTRAINTS -> new ConstraintsPanel(editor, binding, environment);
            case MATRIX4 -> new MatrixPanel(editor, binding, environment);
            case BOX_DECORATION -> new DecorationPanel(editor, binding, environment);
            case BORDER_RADIUS -> new BorderRadiusPanel(editor, binding, environment);
            default -> throw new IllegalStateException(
                    "No Container structured editor for " + binding.editorKind());
        };
    }

    /** Standalone editor for ClipRRect's typed BorderRadiusGeometry value. */
    private static final class BorderRadiusPanel extends DraftPanel {
        private final JCheckBox useDefault = new JCheckBox(
                "Use Flutter default BorderRadius.zero (omit argument)");
        private final JComboBox<String> basis = new JComboBox<>(
                new String[]{"Physical corners", "Directional corners"});
        private final DefaultTableModel model = nonEditableFirstColumn(
                new String[]{"Corner", "X radius", "Y radius"});
        private final JTable radii = table(model, BORDER_RADIUS_TABLE_NAME);
        private boolean preparingCommit;

        BorderRadiusPanel(PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(620, 280));
            setName("flutter.borderRadius.custom");
            getAccessibleContext().setAccessibleName("Flutter border radius editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits physical BorderRadius or direction-aware "
                    + "BorderRadiusDirectional corner ellipses without raw Dart.");

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
            if (binding.optional()) {
                controls.add(useDefault);
            }
            JLabel basisLabel = new JLabel("Basis:");
            basisLabel.setLabelFor(basis);
            controls.add(basisLabel);
            controls.add(basis);
            basis.setName(BORDER_RADIUS_BASIS_NAME);
            basis.getAccessibleContext().setAccessibleName("Border radius basis");
            basis.getAccessibleContext().setAccessibleDescription(
                    "Choose physical left and right corners or directional "
                    + "start and end corners resolved by text direction.");
            radii.getAccessibleContext().setAccessibleDescription(
                    "Four corner rows with finite non-negative horizontal and "
                    + "vertical radius values.");
            add(controls, BorderLayout.NORTH);
            add(new JScrollPane(radii), BorderLayout.CENTER);

            PropertyValue.BorderRadiusValue initial = initialValue().explicitValue()
                    .map(PropertyValue.BorderRadiusValue.class::cast)
                    .orElseGet(BorderRadiusPanel::zero);
            populate(initial.geometry());
            useDefault.setSelected(initialValue().explicitValue().isEmpty());

            useDefault.addActionListener(ignored -> refresh());
            basis.addActionListener(ignored -> {
                if (!updating) {
                    renameRows(model, basis.getSelectedIndex() == 0
                            ? physicalNames() : directionalNames());
                    refresh();
                }
            });
            model.addTableModelListener(ignored -> {
                if (!preparingCommit && !updating) {
                    refresh();
                }
            });
            updateEnabledState();
            refresh();
            activate();
        }

        private void populate(
                PropertyValue.BoxDecorationValue.BorderRadiusGeometry geometry) {
            updating = true;
            try {
                model.setRowCount(0);
                boolean directional = geometry
                        instanceof PropertyValue.BoxDecorationValue.DirectionalBorderRadius;
                basis.setSelectedIndex(directional ? 1 : 0);
                List<String> names = directional ? directionalNames() : physicalNames();
                List<PropertyValue.BoxDecorationValue.Radius> values;
                if (geometry
                        instanceof PropertyValue.BoxDecorationValue.PhysicalBorderRadius value) {
                    values = List.of(value.topLeft(), value.topRight(),
                            value.bottomRight(), value.bottomLeft());
                } else {
                    PropertyValue.BoxDecorationValue.DirectionalBorderRadius value =
                            (PropertyValue.BoxDecorationValue.DirectionalBorderRadius) geometry;
                    values = List.of(value.topStart(), value.topEnd(),
                            value.bottomEnd(), value.bottomStart());
                }
                for (int index = 0; index < 4; index++) {
                    model.addRow(new Object[]{names.get(index),
                        values.get(index).x().toPlainString(),
                        values.get(index).y().toPlainString()});
                }
            } finally {
                updating = false;
            }
        }

        private void refresh() {
            refresh(true);
        }

        private boolean refresh(boolean requestValidation) {
            if (updating) {
                return false;
            }
            synchronizeLiveDrafts(radii);
            updateEnabledState();
            if (binding.optional() && useDefault.isSelected()) {
                clearErrors(this);
                storeValid(FlutterPropertyCellValue.unset(), requestValidation);
                return true;
            }
            try {
                ArrayList<PropertyValue.BoxDecorationValue.Radius> values =
                        new ArrayList<>(4);
                for (int row = 0; row < 4; row++) {
                    values.add(radius(
                            nonNegative(cell(model, row, 1), "Radius X"),
                            nonNegative(cell(model, row, 2), "Radius Y")));
                }
                PropertyValue.BoxDecorationValue.BorderRadiusGeometry geometry =
                        basis.getSelectedIndex() == 0
                        ? new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                                values.get(0), values.get(1), values.get(2), values.get(3))
                        : new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                                values.get(0), values.get(1), values.get(2), values.get(3));
                clearErrors(this);
                storeValid(FlutterPropertyCellValue.explicit(
                        new PropertyValue.BorderRadiusValue(geometry)), requestValidation);
                return true;
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), radii);
                return false;
            }
        }

        private void updateEnabledState() {
            boolean enabled = !binding.optional() || !useDefault.isSelected();
            basis.setEnabled(enabled);
            radii.setEnabled(enabled);
        }

        @Override
        boolean prepareCommit() {
            preparingCommit = true;
            try {
                if (radii.isEditing()) {
                    int row = radii.getEditingRow();
                    int column = radii.getEditingColumn();
                    Object editedValue = switch (radii.getEditorComponent()) {
                        case JTextField text -> text.getText();
                        case JComboBox<?> combo -> combo.getSelectedItem();
                        default -> radii.getCellEditor().getCellEditorValue();
                    };
                    if (!radii.getCellEditor().stopCellEditing()) {
                        markInvalid(
                                "Finish or correct the active radius cell before applying.",
                                radii);
                        return false;
                    }
                    model.setValueAt(editedValue, row, column);
                }
                return refresh(false);
            } finally {
                preparingCommit = false;
            }
        }

        private static PropertyValue.BorderRadiusValue zero() {
            PropertyValue.BoxDecorationValue.Radius zero =
                    radius(BigDecimal.ZERO, BigDecimal.ZERO);
            return new PropertyValue.BorderRadiusValue(
                    new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                            zero, zero, zero, zero));
        }

        private static List<String> physicalNames() {
            return List.of("Top left", "Top right", "Bottom right", "Bottom left");
        }

        private static List<String> directionalNames() {
            return List.of("Top start", "Top end", "Bottom end", "Bottom start");
        }
    }

    private abstract static class DraftPanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        boolean updating;

        DraftPanel(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
        }

        final void submit(PropertyValue value) {
            if (updating) {
                return;
            }
            try {
                clearErrors(this);
                markValid(FlutterPropertyCellValue.explicit(value));
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), this);
            }
        }

        final void submitOptional(boolean unset, PropertyValue value) {
            if (updating) {
                return;
            }
            try {
                clearErrors(this);
                markValid(unset
                        ? FlutterPropertyCellValue.unset()
                        : FlutterPropertyCellValue.explicit(value));
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), this);
            }
        }

        final void submitUnset() {
            if (!updating) {
                clearErrors(this);
                markValid(FlutterPropertyCellValue.unset());
            }
        }

        final void storeValid(FlutterPropertyCellValue value, boolean requestValidation) {
            if (requestValidation) {
                markValid(value);
            } else {
                stageValid(value);
            }
        }
    }

    private record AlignmentPreset(
            String label,
            PropertyValue.AlignmentGeometryValue.HorizontalBasis basis,
            BigDecimal horizontal,
            BigDecimal vertical) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final class AlignmentPanel extends DraftPanel {
        private static final AlignmentPreset[] PRESETS = {
            preset("Center", 0, 0), preset("Top left", -1, -1),
            preset("Top center", 0, -1), preset("Top right", 1, -1),
            preset("Center left", -1, 0), preset("Center right", 1, 0),
            preset("Bottom left", -1, 1), preset("Bottom center", 0, 1),
            preset("Bottom right", 1, 1)
        };

        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private final JComboBox<String> preset = new JComboBox<>();
        private final JComboBox<PropertyValue.AlignmentGeometryValue.HorizontalBasis> basis =
                new JComboBox<>(PropertyValue.AlignmentGeometryValue.HorizontalBasis.values());
        private final JTextField horizontal = new JTextField(12);
        private final JTextField vertical = new JTextField(12);

        AlignmentPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(480, 245));
            setName("flutter.container.alignment.custom");
            getAccessibleContext().setAccessibleName("Container alignment editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits physical or text-direction-aware Flutter AlignmentGeometry coordinates.");

            preset.addItem("Custom coordinates");
            Arrays.stream(PRESETS).map(AlignmentPreset::label).forEach(preset::addItem);
            basis.setName(ALIGNMENT_BASIS_NAME);
            horizontal.setName(ALIGNMENT_HORIZONTAL_NAME);
            vertical.setName(ALIGNMENT_VERTICAL_NAME);
            basis.getAccessibleContext().setAccessibleName("Horizontal coordinate basis");
            horizontal.getAccessibleContext().setAccessibleName("Horizontal alignment coordinate");
            vertical.getAccessibleContext().setAccessibleName("Vertical alignment coordinate");

            JPanel form = new JPanel(new GridBagLayout());
            int row = 0;
            if (binding.optional()) {
                addWideRow(form, row++, useDefault);
            }
            addRow(form, row++, "Preset:", preset);
            addRow(form, row++, "Horizontal basis:", basis);
            addRow(form, row++, "Horizontal (−1…1):", horizontal);
            addRow(form, row, "Vertical (−1…1):", vertical);
            add(form, BorderLayout.NORTH);

            PropertyValue.AlignmentGeometryValue value = initialValue().explicitValue()
                    .map(PropertyValue.AlignmentGeometryValue.class::cast)
                    .orElseGet(() -> alignment(0, 0));
            updating = true;
            useDefault.setSelected(initialValue().explicitValue().isEmpty());
            basis.setSelectedItem(value.basis());
            horizontal.setText(value.horizontal().toPlainString());
            vertical.setText(value.vertical().toPlainString());
            selectMatchingPreset(value);
            updating = false;
            useDefault.addActionListener(ignored -> refresh());
            basis.addActionListener(ignored -> manualRefresh());
            preset.addActionListener(ignored -> applyPreset());
            horizontal.getDocument().addDocumentListener(listener(this::manualRefresh));
            vertical.getDocument().addDocumentListener(listener(this::manualRefresh));
            refresh();
            activate();
        }

        private void applyPreset() {
            if (updating || preset.getSelectedIndex() <= 0) {
                return;
            }
            AlignmentPreset selected = PRESETS[preset.getSelectedIndex() - 1];
            updating = true;
            basis.setSelectedItem(selected.basis());
            horizontal.setText(selected.horizontal().toPlainString());
            vertical.setText(selected.vertical().toPlainString());
            updating = false;
            refresh();
        }

        private void manualRefresh() {
            if (!updating) {
                updating = true;
                preset.setSelectedIndex(0);
                updating = false;
                refresh();
            }
        }

        private void refresh() {
            boolean unset = binding.optional() && useDefault.isSelected();
            preset.setEnabled(!unset);
            basis.setEnabled(!unset);
            horizontal.setEnabled(!unset);
            vertical.setEnabled(!unset);
            if (unset) {
                submitUnset();
                return;
            }
            try {
                PropertyValue.AlignmentGeometryValue value = new PropertyValue.AlignmentGeometryValue(
                        (PropertyValue.AlignmentGeometryValue.HorizontalBasis) basis.getSelectedItem(),
                        decimal(horizontal, "Horizontal coordinate"),
                        decimal(vertical, "Vertical coordinate"));
                submitOptional(unset, value);
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), this);
            }
        }

        private void selectMatchingPreset(PropertyValue.AlignmentGeometryValue value) {
            for (int index = 0; index < PRESETS.length; index++) {
                AlignmentPreset candidate = PRESETS[index];
                if (candidate.basis() == value.basis()
                        && candidate.horizontal().compareTo(value.horizontal()) == 0
                        && candidate.vertical().compareTo(value.vertical()) == 0) {
                    preset.setSelectedIndex(index + 1);
                    return;
                }
            }
            preset.setSelectedIndex(0);
        }

        private static AlignmentPreset preset(String label, int horizontal, int vertical) {
            return new AlignmentPreset(label,
                    PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                    BigDecimal.valueOf(horizontal), BigDecimal.valueOf(vertical));
        }
    }

    private static final class ConstraintsPanel extends DraftPanel {
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private final JTextField minWidth = new JTextField(12);
        private final JTextField maxWidth = new JTextField(12);
        private final JCheckBox unboundedWidth = new JCheckBox("Unbounded (∞)");
        private final JCheckBox expandingWidth = new JCheckBox("Expand width (∞…∞)");
        private final JTextField minHeight = new JTextField(12);
        private final JTextField maxHeight = new JTextField(12);
        private final JCheckBox unboundedHeight = new JCheckBox("Unbounded (∞)");
        private final JCheckBox expandingHeight = new JCheckBox("Expand height (∞…∞)");
        private final JComboBox<String> preset = new JComboBox<>(new String[]{
            "Custom bounds", "Unconstrained", "Square 0…100", "Viewport 0…1000"
        });

        ConstraintsPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(560, 365));
            setName("flutter.container.constraints.custom");
            getAccessibleContext().setAccessibleName("Flutter box constraints editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits normalized width and height bounds, including independent "
                    + "expanding axes from positive infinity through positive infinity.");
            minWidth.setName(CONSTRAINTS_MIN_WIDTH_NAME);
            maxWidth.setName(CONSTRAINTS_MAX_WIDTH_NAME);
            unboundedWidth.setName(CONSTRAINTS_UNBOUNDED_WIDTH_NAME);
            expandingWidth.setName(CONSTRAINTS_EXPANDING_WIDTH_NAME);
            minHeight.setName(CONSTRAINTS_MIN_HEIGHT_NAME);
            maxHeight.setName(CONSTRAINTS_MAX_HEIGHT_NAME);
            unboundedHeight.setName(CONSTRAINTS_UNBOUNDED_HEIGHT_NAME);
            expandingHeight.setName(CONSTRAINTS_EXPANDING_HEIGHT_NAME);
            minWidth.getAccessibleContext().setAccessibleName("Minimum width");
            maxWidth.getAccessibleContext().setAccessibleName("Maximum width");
            unboundedWidth.getAccessibleContext().setAccessibleName(
                    "Unbounded maximum width");
            expandingWidth.getAccessibleContext().setAccessibleName("Expanding width");
            minHeight.getAccessibleContext().setAccessibleName("Minimum height");
            maxHeight.getAccessibleContext().setAccessibleName("Maximum height");
            unboundedHeight.getAccessibleContext().setAccessibleName(
                    "Unbounded maximum height");
            expandingHeight.getAccessibleContext().setAccessibleName("Expanding height");
            expandingWidth.getAccessibleContext().setAccessibleDescription(
                    "Sets both the minimum and maximum width to positive infinity.");
            expandingHeight.getAccessibleContext().setAccessibleDescription(
                    "Sets both the minimum and maximum height to positive infinity.");

            JPanel form = new JPanel(new GridBagLayout());
            int row = 0;
            if (binding.optional()) {
                addWideRow(form, row++, useDefault);
            }
            addRow(form, row++, "Preset:", preset);
            addRow(form, row++, "Minimum width:", minWidth);
            addRow(form, row++, "Maximum width:", maxWidth, unboundedWidth);
            addWideRow(form, row++, expandingWidth);
            addRow(form, row++, "Minimum height:", minHeight);
            addRow(form, row++, "Maximum height:", maxHeight, unboundedHeight);
            addWideRow(form, row, expandingHeight);
            add(form, BorderLayout.NORTH);

            PropertyValue.BoxConstraintsValue value = initialValue().explicitValue()
                    .map(PropertyValue.BoxConstraintsValue.class::cast)
                    .orElseGet(() -> new PropertyValue.BoxConstraintsValue(
                    BigDecimal.ZERO, Optional.empty(), BigDecimal.ZERO, Optional.empty()));
            updating = true;
            useDefault.setSelected(initialValue().explicitValue().isEmpty());
            minWidth.setText(finiteTextOrZero(value.minWidth()));
            maxWidth.setText(finiteText(value.maxWidth()));
            unboundedWidth.setSelected(value.maxWidth().infinite());
            expandingWidth.setSelected(value.expandingWidth());
            minHeight.setText(finiteTextOrZero(value.minHeight()));
            maxHeight.setText(finiteText(value.maxHeight()));
            unboundedHeight.setSelected(value.maxHeight().infinite());
            expandingHeight.setSelected(value.expandingHeight());
            updating = false;

            useDefault.addActionListener(ignored -> refresh());
            preset.addActionListener(ignored -> applyPreset());
            unboundedWidth.addActionListener(ignored -> manualRefresh());
            expandingWidth.addActionListener(ignored -> manualRefresh());
            unboundedHeight.addActionListener(ignored -> manualRefresh());
            expandingHeight.addActionListener(ignored -> manualRefresh());
            for (JTextField field : List.of(minWidth, maxWidth, minHeight, maxHeight)) {
                field.getDocument().addDocumentListener(listener(this::manualRefresh));
            }
            refresh();
            activate();
        }

        private void applyPreset() {
            if (updating || preset.getSelectedIndex() == 0) {
                return;
            }
            updating = true;
            String maximum = switch (preset.getSelectedIndex()) {
                case 2 -> "100";
                case 3 -> "1000";
                default -> "";
            };
            minWidth.setText("0");
            minHeight.setText("0");
            maxWidth.setText(maximum);
            maxHeight.setText(maximum);
            boolean unbounded = preset.getSelectedIndex() == 1;
            unboundedWidth.setSelected(unbounded);
            unboundedHeight.setSelected(unbounded);
            expandingWidth.setSelected(false);
            expandingHeight.setSelected(false);
            updating = false;
            refresh();
        }

        private void manualRefresh() {
            if (!updating) {
                updating = true;
                preset.setSelectedIndex(0);
                updating = false;
                refresh();
            }
        }

        private void refresh() {
            boolean unset = binding.optional() && useDefault.isSelected();
            boolean expandWidth = expandingWidth.isSelected();
            boolean expandHeight = expandingHeight.isSelected();
            if (expandWidth) {
                unboundedWidth.setSelected(true);
            }
            if (expandHeight) {
                unboundedHeight.setSelected(true);
            }
            preset.setEnabled(!unset);
            expandingWidth.setEnabled(!unset);
            expandingHeight.setEnabled(!unset);
            minWidth.setEnabled(!unset && !expandWidth);
            unboundedWidth.setEnabled(!unset && !expandWidth);
            maxWidth.setEnabled(!unset && !expandWidth && !unboundedWidth.isSelected());
            minHeight.setEnabled(!unset && !expandHeight);
            unboundedHeight.setEnabled(!unset && !expandHeight);
            maxHeight.setEnabled(!unset && !expandHeight && !unboundedHeight.isSelected());
            if (unset) {
                submitUnset();
                return;
            }
            try {
                PropertyValue.BoxConstraintsValue value = new PropertyValue.BoxConstraintsValue(
                        minimumBound(expandWidth, minWidth, "Minimum width"),
                        maximumBound(expandWidth, unboundedWidth, maxWidth, "Maximum width"),
                        minimumBound(expandHeight, minHeight, "Minimum height"),
                        maximumBound(
                                expandHeight, unboundedHeight, maxHeight, "Maximum height"));
                submitOptional(unset, value);
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), this);
            }
        }

        private static String finiteText(PropertyValue.BoxConstraintBound bound) {
            return bound.finiteValue().map(BigDecimal::toPlainString).orElse("");
        }

        private static String finiteTextOrZero(PropertyValue.BoxConstraintBound bound) {
            return bound.finiteValue().map(BigDecimal::toPlainString).orElse("0");
        }

        private static PropertyValue.BoxConstraintBound minimumBound(
                boolean expanding,
                JTextField field,
                String label) {
            return expanding
                    ? PropertyValue.BoxConstraintBound.Infinity.INSTANCE
                    : new PropertyValue.BoxConstraintBound.Finite(decimal(field, label));
        }

        private static PropertyValue.BoxConstraintBound maximumBound(
                boolean expanding,
                JCheckBox unbounded,
                JTextField field,
                String label) {
            return expanding || unbounded.isSelected()
                    ? PropertyValue.BoxConstraintBound.Infinity.INSTANCE
                    : new PropertyValue.BoxConstraintBound.Finite(decimal(field, label));
        }
    }

    private static final class MatrixPanel extends DraftPanel {
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private final JTextField[][] fields = new JTextField[4][4];

        MatrixPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(610, 410));
            setName("flutter.container.matrix.custom");
            getAccessibleContext().setAccessibleName("Container Matrix4 transform editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits a 4 by 4 Matrix4; values are persisted in Flutter column-major order.");

            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
            if (binding.optional()) {
                header.add(useDefault);
            }
            JButton identity = new JButton("Identity");
            JButton translate = new JButton("Translate…");
            JButton scale = new JButton("Scale…");
            JButton rotate = new JButton("Rotate Z…");
            identity.getAccessibleContext().setAccessibleDescription(
                    "Replaces the draft with an identity Matrix4.");
            translate.getAccessibleContext().setAccessibleDescription(
                    "Creates a translation Matrix4 from X, Y, and Z values.");
            scale.getAccessibleContext().setAccessibleDescription(
                    "Creates a scale Matrix4 from X, Y, and Z values.");
            rotate.getAccessibleContext().setAccessibleDescription(
                    "Creates a Z-axis rotation Matrix4 from degrees.");
            header.add(identity);
            header.add(translate);
            header.add(scale);
            header.add(rotate);
            add(header, BorderLayout.NORTH);

            JPanel matrix = new JPanel(new GridBagLayout());
            matrix.setName(MATRIX_TABLE_NAME);
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 4; column++) {
                    JTextField field = new JTextField(8);
                    field.setName("flutter.container.matrix.r" + row + "c" + column);
                    field.getAccessibleContext().setAccessibleName(
                            "Matrix row " + (row + 1) + ", column " + (column + 1));
                    field.getAccessibleContext().setAccessibleDescription(
                            "Finite Matrix4 entry; visual row " + (row + 1)
                            + " and column " + (column + 1) + '.');
                    fields[row][column] = field;
                    GridBagConstraints cell = new GridBagConstraints();
                    cell.gridx = column;
                    cell.gridy = row;
                    cell.weightx = 1;
                    cell.fill = GridBagConstraints.HORIZONTAL;
                    cell.insets = new Insets(4, 4, 4, 4);
                    matrix.add(field, cell);
                }
            }
            add(matrix, BorderLayout.CENTER);

            PropertyValue.Matrix4Value value = initialValue().explicitValue()
                    .map(PropertyValue.Matrix4Value.class::cast)
                    .orElseGet(MatrixPanel::identity);
            updating = true;
            useDefault.setSelected(initialValue().explicitValue().isEmpty());
            populate(value.storage());
            updating = false;
            useDefault.addActionListener(ignored -> refresh());
            for (JTextField[] row : fields) {
                for (JTextField field : row) {
                    field.getDocument().addDocumentListener(listener(this::refresh));
                }
            }
            identity.addActionListener(ignored -> setMatrix(identity().storage()));
            translate.addActionListener(ignored -> editTranslate());
            scale.addActionListener(ignored -> editScale());
            rotate.addActionListener(ignored -> editRotation());
            refresh();
            activate();
        }

        private void editTranslate() {
            BigDecimal[] values = askNumbers(this, "Translate", new String[]{"X", "Y", "Z"},
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            if (values == null) {
                return;
            }
            List<BigDecimal> storage = new ArrayList<>(identity().storage());
            storage.set(12, values[0]);
            storage.set(13, values[1]);
            storage.set(14, values[2]);
            setMatrix(storage);
        }

        private void editScale() {
            BigDecimal[] values = askNumbers(this, "Scale", new String[]{"X", "Y", "Z"},
                    new BigDecimal[]{BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE});
            if (values == null) {
                return;
            }
            List<BigDecimal> storage = new ArrayList<>(identity().storage());
            storage.set(0, values[0]);
            storage.set(5, values[1]);
            storage.set(10, values[2]);
            setMatrix(storage);
        }

        private void editRotation() {
            BigDecimal[] value = askNumbers(this, "Rotate Z", new String[]{"Degrees"},
                    new BigDecimal[]{BigDecimal.ZERO});
            if (value == null) {
                return;
            }
            double radians = Math.toRadians(value[0].doubleValue());
            BigDecimal cosine = BigDecimal.valueOf(Math.cos(radians));
            BigDecimal sine = BigDecimal.valueOf(Math.sin(radians));
            List<BigDecimal> storage = new ArrayList<>(identity().storage());
            storage.set(0, cosine);
            storage.set(1, sine);
            storage.set(4, sine.negate());
            storage.set(5, cosine);
            setMatrix(storage);
        }

        private void setMatrix(List<BigDecimal> storage) {
            updating = true;
            populate(storage);
            updating = false;
            refresh();
        }

        private void populate(List<BigDecimal> storage) {
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 4; column++) {
                    fields[row][column].setText(storage.get(column * 4 + row).toPlainString());
                }
            }
        }

        private void refresh() {
            boolean unset = binding.optional() && useDefault.isSelected();
            for (JTextField[] row : fields) {
                for (JTextField field : row) {
                    field.setEnabled(!unset);
                }
            }
            if (unset) {
                submitUnset();
                return;
            }
            try {
                ArrayList<BigDecimal> storage = new ArrayList<>(16);
                for (int column = 0; column < 4; column++) {
                    for (int row = 0; row < 4; row++) {
                        storage.add(decimal(fields[row][column],
                                "Matrix row " + (row + 1) + ", column " + (column + 1)));
                    }
                }
                submitOptional(unset, new PropertyValue.Matrix4Value(storage));
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), this);
            }
        }

        private static PropertyValue.Matrix4Value identity() {
            ArrayList<BigDecimal> values = new ArrayList<>(16);
            for (int index = 0; index < 16; index++) {
                values.add(index % 5 == 0 ? BigDecimal.ONE : BigDecimal.ZERO);
            }
            return new PropertyValue.Matrix4Value(values);
        }
    }

    private static final class DecorationPanel extends DraftPanel {
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private final JComboBox<PropertyValue.BoxDecorationValue.BoxShape> shape =
                new JComboBox<>(PropertyValue.BoxDecorationValue.BoxShape.values());
        private final ColorSourceEditor fill;
        private final JComboBox<String> blend = new JComboBox<>();

        private final FlutterImageAssetChoices assetChoices;
        private final JCheckBox imageEnabled = new JCheckBox(
                "Enable DecorationImage");
        private final FlutterImageProviderEditorComponent imageProviderEditor;
        private final JCheckBox imageOnErrorEnabled = new JCheckBox(
                "Use typed image-error handler");
        private final JTextField imageOnError = new JTextField(18);
        private final JComboBox<String> imageColorFilter = new JComboBox<>(
                new String[]{"None", "Mode", "Matrix", "Linear to sRGB gamma",
                    "sRGB to linear gamma", "Saturation"});
        private final ColorSourceEditor imageFilterColor;
        private final JComboBox<PropertyValue.PaintValue.BlendMode>
                imageFilterBlend = new JComboBox<>(
                        PropertyValue.PaintValue.BlendMode.values());
        private final JTextField imageFilterMatrix = new JTextField(42);
        private final JTextField imageFilterSaturation = new JTextField("1", 8);
        private final JComboBox<String> imageFit = new JComboBox<>();
        private final JComboBox<PropertyValue.AlignmentGeometryValue.HorizontalBasis>
                imageAlignmentBasis = new JComboBox<>(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.values());
        private final JTextField imageAlignmentHorizontal = new JTextField("0", 8);
        private final JTextField imageAlignmentVertical = new JTextField("0", 8);
        private final JCheckBox imageCenterSliceEnabled = new JCheckBox(
                "Use center slice (nine-patch)");
        private final JTextField imageSliceLeft = new JTextField("0", 7);
        private final JTextField imageSliceTop = new JTextField("0", 7);
        private final JTextField imageSliceRight = new JTextField("1", 7);
        private final JTextField imageSliceBottom = new JTextField("1", 7);
        private final JComboBox<PropertyValue.DecorationImageValue.ImageRepeat>
                imageRepeat = new JComboBox<>(
                        PropertyValue.DecorationImageValue.ImageRepeat.values());
        private final JCheckBox imageMatchTextDirection = new JCheckBox(
                "Mirror for text direction");
        private final JTextField imageScale = new JTextField("1", 8);
        private final JTextField imageOpacity = new JTextField("1", 8);
        private final JComboBox<PropertyValue.PaintValue.FilterQuality>
                imageFilterQuality = new JComboBox<>(
                        PropertyValue.PaintValue.FilterQuality.values());
        private final JCheckBox imageInvertColors = new JCheckBox(
                "Invert colors");
        private final JCheckBox imageAntiAlias = new JCheckBox(
                "Anti-alias image edges");

        private final JCheckBox borderEnabled = new JCheckBox("Enable border");
        private final JComboBox<String> borderBasis = new JComboBox<>(
                new String[]{"Physical: left / right", "Directional: start / end"});
        private final DefaultTableModel borderModel = nonEditableFirstColumn(
                new String[]{"Side", "Color", "Width", "Style", "Stroke align"});
        private final JTable borderTable = table(borderModel, DECORATION_BORDER_TABLE_NAME);

        private final JCheckBox radiusEnabled = new JCheckBox("Enable border radius");
        private final JComboBox<String> radiusBasis = new JComboBox<>(
                new String[]{"Physical corners", "Directional corners"});
        private final DefaultTableModel radiusModel = nonEditableFirstColumn(
                new String[]{"Corner", "X radius", "Y radius"});
        private final JTable radiusTable = table(radiusModel, DECORATION_RADIUS_TABLE_NAME);

        private final DefaultTableModel shadowModel = new DefaultTableModel(
                new String[]{"Color", "X", "Y", "Blur", "Spread", "Blur style"}, 0);
        private final JTable shadowTable = table(shadowModel, DECORATION_SHADOW_TABLE_NAME);
        private final ArrayList<StableId> shadowIds = new ArrayList<>();

        private final JComboBox<String> gradientType = new JComboBox<>(
                new String[]{"None", "Linear", "Radial", "Sweep"});
        private final JComboBox<PropertyValue.BoxDecorationValue.TileMode> tileMode =
                new JComboBox<>(PropertyValue.BoxDecorationValue.TileMode.values());
        private final JCheckBox rotationEnabled = new JCheckBox("Rotation (radians)");
        private final JTextField rotation = new JTextField("0", 9);
        private final JTextField firstX = new JTextField("0", 7);
        private final JTextField firstY = new JTextField("0", 7);
        private final JTextField secondX = new JTextField("0", 7);
        private final JTextField secondY = new JTextField("0", 7);
        private final JComboBox<PropertyValue.AlignmentGeometryValue.HorizontalBasis> firstBasis =
                new JComboBox<>(PropertyValue.AlignmentGeometryValue.HorizontalBasis.values());
        private final JComboBox<PropertyValue.AlignmentGeometryValue.HorizontalBasis> secondBasis =
                new JComboBox<>(PropertyValue.AlignmentGeometryValue.HorizontalBasis.values());
        private final JLabel firstLabel = new JLabel("Begin / center:");
        private final JLabel secondLabel = new JLabel("End / focal:");
        private final JCheckBox secondEnabled = new JCheckBox("Use second alignment");
        private final JTextField radius = new JTextField("0.5", 9);
        private final JTextField focalRadius = new JTextField("0", 9);
        private final JTextField startAngle = new JTextField("0", 9);
        private final JTextField endAngle = new JTextField(
                Double.toString(Math.PI * 2), 9);
        private final DefaultTableModel gradientModel = new DefaultTableModel(
                new String[]{"Color", "Stop (0…1)"}, 0);
        private final JTable gradientTable = table(
                gradientModel, DECORATION_GRADIENT_TABLE_NAME);
        private final ArrayList<StableId> gradientIds = new ArrayList<>();
        private boolean preparingCommit;
        private int currentBorderBasis;

        DecorationPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            assetChoices = assetChoices(environment);
            imageProviderEditor = new FlutterImageProviderEditorComponent(
                    assetChoices,
                    "DecorationImage.image",
                    "flutter.container.decoration.image",
                    FlutterImageProviderEditorComponent.EmptySelectionPolicy
                            .REQUIRE_DECLARED_ASSET,
                    this::refresh);
            configureImageRenderers();
            imageFilterColor = new ColorSourceEditor(
                    binding.allowedColorSourceTokens(), false, this::refresh);
            setLayout(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(900, 680));
            setName("flutter.container.decoration.custom");
            getAccessibleContext().setAccessibleName("Container BoxDecoration editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits the reviewed Flutter BoxDecoration and DecorationImage domains using typed structured controls.");
            fill = new ColorSourceEditor(binding.allowedColorSourceTokens(), true, this::refresh);

            JPanel north = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
            if (binding.optional()) {
                north.add(useDefault);
            }
            add(north, BorderLayout.NORTH);

            JTabbedPane tabs = new JTabbedPane();
            tabs.setName(DECORATION_TABS_NAME);
            tabs.getAccessibleContext().setAccessibleName("BoxDecoration sections");
            tabs.getAccessibleContext().setAccessibleDescription(
                    "Fill, image, border, radius, ordered shadows, and gradient settings.");
            tabs.addTab("Fill", fillPanel());
            tabs.addTab("Image", imagePanel());
            tabs.addTab("Border", borderPanel());
            tabs.addTab("Radius", radiusPanel());
            tabs.addTab("Shadows", shadowPanel());
            tabs.addTab("Gradient", gradientPanel());
            add(tabs, BorderLayout.CENTER);

            populate(initialValue().explicitValue()
                    .map(PropertyValue.BoxDecorationValue.class::cast)
                    .orElseGet(DecorationPanel::emptyDecoration));
            useDefault.setSelected(initialValue().explicitValue().isEmpty());
            installListeners();
            updateEnabledState();
            refresh();
            activate();
        }

        private void configureImageRenderers() {
            displayWith(imageAlignmentBasis, value -> switch (value) {
                case PHYSICAL -> "Physical (left/right)";
                case DIRECTIONAL -> "Directional (start/end)";
            });
            displayWith(imageRepeat, value -> switch (value) {
                case REPEAT -> "Repeat both axes";
                case REPEAT_X -> "Repeat horizontally";
                case REPEAT_Y -> "Repeat vertically";
                case NO_REPEAT -> "No repeat";
            });
            displayWith(imageFilterQuality, value -> switch (value) {
                case NONE -> "None";
                case LOW -> "Low";
                case MEDIUM -> "Medium";
                case HIGH -> "High";
            });
            displayWith(imageFilterBlend,
                    value -> humanizeWireName(value.wireName()));
            displayWith(imageFit, FlutterContainerPropertyEditorComponents
                    ::humanizeWireName);
        }

        private JPanel fillPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            int row = 0;
            addRow(panel, row++, "Shape:", shape);
            addRow(panel, row++, "Fill color:", fill);
            blend.addItem(NOT_SET);
            Arrays.stream(PropertyValue.PaintValue.BlendMode.values())
                    .map(PropertyValue.PaintValue.BlendMode::wireName).forEach(blend::addItem);
            addRow(panel, row, "Background blend:", blend);
            addVerticalGlue(panel, row + 1);
            return panel;
        }

        private JPanel imagePanel() {
            imageEnabled.setName(DECORATION_IMAGE_ENABLED_NAME);
            imageColorFilter.setName(DECORATION_IMAGE_FILTER_NAME);
            imageEnabled.getAccessibleContext().setAccessibleDescription(
                    "Adds or removes the typed DecorationImage from this BoxDecoration draft.");
            imageOnErrorEnabled.getAccessibleContext().setAccessibleDescription(
                    "Enables a validated two-argument Flutter image-error callback identifier.");
            imageOnError.getAccessibleContext().setAccessibleName(
                    "Typed image error handler identifier");
            imageOnError.getAccessibleContext().setAccessibleDescription(
                    "A Dart function identifier only; expressions and callback source are rejected.");
            imageColorFilter.getAccessibleContext().setAccessibleName(
                    "DecorationImage color filter variant");
            imageFilterMatrix.getAccessibleContext().setAccessibleName(
                    "ColorFilter matrix values");
            imageFilterMatrix.getAccessibleContext().setAccessibleDescription(
                    "Exactly twenty finite typed matrix values in row-major order.");
            imageFilterSaturation.getAccessibleContext().setAccessibleName(
                    "ColorFilter saturation amount");
            imageFit.getAccessibleContext().setAccessibleName(
                    "DecorationImage BoxFit");
            imageAlignmentBasis.getAccessibleContext().setAccessibleName(
                    "DecorationImage alignment basis");
            imageAlignmentHorizontal.getAccessibleContext().setAccessibleName(
                    "DecorationImage horizontal alignment");
            imageAlignmentVertical.getAccessibleContext().setAccessibleName(
                    "DecorationImage vertical alignment");
            imageCenterSliceEnabled.getAccessibleContext().setAccessibleDescription(
                    "Enables a strict positive-area nine-patch rectangle; every "
                    + "pinned-SDK fit except cover and none is accepted.");
            imageSliceLeft.getAccessibleContext().setAccessibleName(
                    "Center slice left");
            imageSliceTop.getAccessibleContext().setAccessibleName(
                    "Center slice top");
            imageSliceRight.getAccessibleContext().setAccessibleName(
                    "Center slice right");
            imageSliceBottom.getAccessibleContext().setAccessibleName(
                    "Center slice bottom");
            imageRepeat.getAccessibleContext().setAccessibleName(
                    "DecorationImage repeat");
            imageMatchTextDirection.getAccessibleContext().setAccessibleDescription(
                    "Mirrors the image when the ambient text direction is right-to-left.");
            imageScale.getAccessibleContext().setAccessibleName(
                    "DecorationImage paint scale");
            imageOpacity.getAccessibleContext().setAccessibleName(
                    "DecorationImage opacity");
            imageFilterQuality.getAccessibleContext().setAccessibleName(
                    "DecorationImage filter quality");
            imageInvertColors.getAccessibleContext().setAccessibleDescription(
                    "Requests semantic color inversion for accessibility.");
            imageAntiAlias.getAccessibleContext().setAccessibleDescription(
                    "Anti-aliases image edges while painting.");

            imageFit.addItem(NOT_SET);
            Arrays.stream(PropertyValue.DecorationImageValue.BoxFit.values())
                    .map(PropertyValue.DecorationImageValue.BoxFit::wireName)
                    .forEach(imageFit::addItem);
            imageFilterMatrix.setText(
                    "1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0");

            JPanel form = new JPanel(new GridBagLayout());
            int row = 0;
            addWideRow(form, row++, imageEnabled);
            addWideRow(form, row++, imageProviderEditor);
            addRow(form, row++, "onError:", imageOnErrorEnabled, imageOnError);
            addRow(form, row++, "Color filter:", imageColorFilter);
            addRow(form, row++, "Filter color / blend:", imageFilterColor,
                    imageFilterBlend);
            addRow(form, row++, "Matrix (20 values):", imageFilterMatrix);
            addRow(form, row++, "Saturation:", imageFilterSaturation);
            addRow(form, row++, "Fit:", imageFit);
            addRow(form, row++, "Alignment basis:", imageAlignmentBasis);
            addRow(form, row++, "Alignment X / Y:", flow(
                    imageAlignmentHorizontal,
                    new JLabel("/"),
                    imageAlignmentVertical));
            addWideRow(form, row++, imageCenterSliceEnabled);
            addRow(form, row++, "Center slice L / T / R / B:", flow(
                    imageSliceLeft, imageSliceTop,
                    imageSliceRight, imageSliceBottom));
            addRow(form, row++, "Repeat:", imageRepeat,
                    imageMatchTextDirection);
            addRow(form, row++, "Scale / opacity:", flow(
                    imageScale, new JLabel("/"), imageOpacity));
            addRow(form, row++, "Filter quality:", imageFilterQuality);
            addWideRow(form, row++, flow(imageInvertColors, imageAntiAlias));

            addVerticalGlue(form, row);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(form, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(panel);
            scroll.setBorder(null);
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.add(scroll, BorderLayout.CENTER);
            return wrapper;
        }

        private JPanel borderPanel() {
            installColorCellEditor(borderTable, 1, binding.allowedColorSourceTokens());
            JComboBox<String> styles = new JComboBox<>(Arrays.stream(
                    PropertyValue.BoxDecorationValue.BorderStyle.values())
                    .map(PropertyValue.BoxDecorationValue.BorderStyle::wireName)
                    .toArray(String[]::new));
            borderTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(styles));
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
            controls.add(borderEnabled);
            JLabel basisLabel = new JLabel("Basis:");
            basisLabel.setLabelFor(borderBasis);
            controls.add(basisLabel);
            controls.add(borderBasis);
            borderBasis.getAccessibleContext().setAccessibleName("Border basis");
            borderBasis.getAccessibleContext().setAccessibleDescription(
                    "Chooses physical left and right sides or directional start and end sides.");
            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.add(controls, BorderLayout.NORTH);
            panel.add(new JScrollPane(borderTable), BorderLayout.CENTER);
            return panel;
        }

        private JPanel radiusPanel() {
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
            controls.add(radiusEnabled);
            JLabel basisLabel = new JLabel("Basis:");
            basisLabel.setLabelFor(radiusBasis);
            controls.add(basisLabel);
            controls.add(radiusBasis);
            radiusBasis.getAccessibleContext().setAccessibleName("Border radius basis");
            radiusBasis.getAccessibleContext().setAccessibleDescription(
                    "Chooses physical or directional corner names.");
            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.add(controls, BorderLayout.NORTH);
            panel.add(new JScrollPane(radiusTable), BorderLayout.CENTER);
            return panel;
        }

        private JPanel shadowPanel() {
            installColorCellEditor(shadowTable, 0, binding.allowedColorSourceTokens());
            JComboBox<String> styles = new JComboBox<>(Arrays.stream(
                    PropertyValue.PaintValue.BlurStyle.values())
                    .map(PropertyValue.PaintValue.BlurStyle::wireName)
                    .toArray(String[]::new));
            shadowTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(styles));
            return orderedTablePanel(shadowTable,
                    () -> addShadow(StableId.random(), new Object[]{
                        "0x55000000", "0", "2", "4", "0", "normal"}),
                    () -> removeSelected(shadowTable, shadowModel, shadowIds),
                    () -> moveSelected(shadowTable, shadowModel, shadowIds, -1),
                    () -> moveSelected(shadowTable, shadowModel, shadowIds, 1));
        }

        private JPanel gradientPanel() {
            installColorCellEditor(gradientTable, 0, binding.allowedColorSourceTokens());
            JPanel form = new JPanel(new GridBagLayout());
            int row = 0;
            addRow(form, row++, "Gradient:", gradientType);
            addRow(form, row++, "Tile mode:", tileMode);
            addRow(form, row++, "Transform:", rotationEnabled, rotation);
            addAlignmentRow(form, row++, firstLabel, firstBasis, firstX, firstY);
            addAlignmentRow(form, row++, secondLabel, secondBasis, secondX, secondY);
            addRow(form, row++, "Second alignment:", secondEnabled);
            addRow(form, row++, "Radius:", radius);
            addRow(form, row++, "Focal radius:", focalRadius);
            addRow(form, row++, "Start angle:", startAngle);
            addRow(form, row, "End angle:", endAngle);
            gradientType.getAccessibleContext().setAccessibleName("Gradient type");
            tileMode.getAccessibleContext().setAccessibleName("Gradient tile mode");
            rotation.getAccessibleContext().setAccessibleName("Gradient rotation radians");
            radius.getAccessibleContext().setAccessibleName("Radial gradient radius");
            focalRadius.getAccessibleContext().setAccessibleName("Radial focal radius");
            startAngle.getAccessibleContext().setAccessibleName("Sweep start angle");
            endAngle.getAccessibleContext().setAccessibleName("Sweep end angle");

            JPanel stops = orderedTablePanel(gradientTable,
                    () -> addGradientStop(StableId.random(), new Object[]{
                        gradientModel.getRowCount() == 0 ? "0xFF000000" : "0xFFFFFFFF",
                        gradientModel.getRowCount() == 0 ? "0" : "1"}),
                    () -> removeSelected(gradientTable, gradientModel, gradientIds),
                    () -> moveSelected(gradientTable, gradientModel, gradientIds, -1),
                    () -> moveSelected(gradientTable, gradientModel, gradientIds, 1));
            stops.setBorder(javax.swing.BorderFactory.createTitledBorder("Ordered color stops"));
            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.add(form, BorderLayout.NORTH);
            panel.add(stops, BorderLayout.CENTER);
            return panel;
        }

        private void populate(PropertyValue.BoxDecorationValue value) {
            updating = true;
            try {
                shape.setSelectedItem(value.shape());
                fill.setValue(value.color());
                populateImage(value.image());
                blend.setSelectedItem(value.backgroundBlendMode()
                        .map(PropertyValue.PaintValue.BlendMode::wireName).orElse(NOT_SET));
                populateBorder(value.border());
                populateRadius(value.borderRadius());
                shadowModel.setRowCount(0);
                shadowIds.clear();
                value.boxShadow().forEach(shadow -> addShadow(shadow.id(), new Object[]{
                    colorText(shadow.color()), shadow.offsetX().toPlainString(),
                    shadow.offsetY().toPlainString(), shadow.blurRadius().toPlainString(),
                    shadow.spreadRadius().toPlainString(), shadow.blurStyle().wireName()
                }));
                populateGradient(value.gradient());
            } finally {
                updating = false;
            }
        }

        private void populateImage(
                Optional<PropertyValue.DecorationImageValue> value) {
            imageEnabled.setSelected(value.isPresent());
            PropertyValue.DecorationImageValue image = value.orElseGet(() ->
                    PropertyValue.DecorationImageValue.defaults(
                            PropertyValue.ImageProviderValue.asset(
                                    assetChoices.choices().isEmpty()
                                    ? "assets/image.png"
                                    : assetChoices.choices().getFirst().assetName())));
            if (value.isPresent()) {
                imageProviderEditor.populate(image.image());
            } else {
                imageProviderEditor.selectFirstDeclaredAsset();
            }
            imageOnErrorEnabled.setSelected(image.onError().isPresent());
            imageOnError.setText(image.onError()
                    .map(PropertyValue.CallbackValue::handler).orElse(""));
            populateImageColorFilter(image.colorFilter());
            imageFit.setSelectedItem(image.fit()
                    .map(PropertyValue.DecorationImageValue.BoxFit::wireName)
                    .orElse(NOT_SET));
            imageAlignmentBasis.setSelectedItem(image.alignment().basis());
            imageAlignmentHorizontal.setText(
                    image.alignment().horizontal().toPlainString());
            imageAlignmentVertical.setText(
                    image.alignment().vertical().toPlainString());
            imageCenterSliceEnabled.setSelected(image.centerSlice().isPresent());
            image.centerSlice().ifPresentOrElse(slice -> {
                imageSliceLeft.setText(slice.left().toPlainString());
                imageSliceTop.setText(slice.top().toPlainString());
                imageSliceRight.setText(slice.right().toPlainString());
                imageSliceBottom.setText(slice.bottom().toPlainString());
            }, () -> {
                imageSliceLeft.setText("0");
                imageSliceTop.setText("0");
                imageSliceRight.setText("1");
                imageSliceBottom.setText("1");
            });
            imageRepeat.setSelectedItem(image.repeat());
            imageMatchTextDirection.setSelected(image.matchTextDirection());
            imageScale.setText(image.scale().toPlainString());
            imageOpacity.setText(image.opacity().toPlainString());
            imageFilterQuality.setSelectedItem(image.filterQuality());
            imageInvertColors.setSelected(image.invertColors());
            imageAntiAlias.setSelected(image.isAntiAlias());
        }

        private void populateImageColorFilter(
                Optional<PropertyValue.DecorationImageValue.ColorFilter> value) {
            if (value.isEmpty()) {
                imageColorFilter.setSelectedItem("None");
                imageFilterColor.setValue(Optional.of(
                        new ColorSource.Literal(0xFF000000L)));
                imageFilterBlend.setSelectedItem(
                        PropertyValue.PaintValue.BlendMode.SRC_OVER);
                imageFilterSaturation.setText("1");
                return;
            }
            switch (value.orElseThrow()) {
                case PropertyValue.DecorationImageValue.Mode mode -> {
                    imageColorFilter.setSelectedItem("Mode");
                    imageFilterColor.setValue(Optional.of(mode.color()));
                    imageFilterBlend.setSelectedItem(mode.blendMode());
                }
                case PropertyValue.DecorationImageValue.Matrix matrix -> {
                    imageColorFilter.setSelectedItem("Matrix");
                    imageFilterMatrix.setText(matrix.values().stream()
                            .map(BigDecimal::toPlainString)
                            .collect(java.util.stream.Collectors.joining(", ")));
                }
                case PropertyValue.DecorationImageValue.LinearToSrgbGamma ignored ->
                    imageColorFilter.setSelectedItem("Linear to sRGB gamma");
                case PropertyValue.DecorationImageValue.SrgbToLinearGamma ignored ->
                    imageColorFilter.setSelectedItem("sRGB to linear gamma");
                case PropertyValue.DecorationImageValue.Saturation saturation -> {
                    imageColorFilter.setSelectedItem("Saturation");
                    imageFilterSaturation.setText(
                            saturation.value().toPlainString());
                }
            }
        }

        private void populateBorder(Optional<PropertyValue.BoxDecorationValue.BoxBorder> value) {
            borderModel.setRowCount(0);
            borderEnabled.setSelected(value.isPresent());
            boolean directional = value.orElse(null)
                    instanceof PropertyValue.BoxDecorationValue.DirectionalBorder;
            borderBasis.setSelectedIndex(directional ? 1 : 0);
            currentBorderBasis = directional ? 1 : 0;
            List<String> names = directional
                    ? List.of("Top", "Start", "End", "Bottom")
                    : List.of("Top", "Right", "Bottom", "Left");
            List<PropertyValue.BoxDecorationValue.BorderSide> sides;
            if (value.orElse(null) instanceof PropertyValue.BoxDecorationValue.PhysicalBorder border) {
                sides = List.of(border.top(), border.right(), border.bottom(), border.left());
            } else if (value.orElse(null)
                    instanceof PropertyValue.BoxDecorationValue.DirectionalBorder border) {
                sides = List.of(border.top(), border.start(), border.end(), border.bottom());
            } else {
                sides = List.of(defaultSide(), defaultSide(), defaultSide(), defaultSide());
            }
            for (int index = 0; index < 4; index++) {
                PropertyValue.BoxDecorationValue.BorderSide side = sides.get(index);
                borderModel.addRow(new Object[]{names.get(index), colorText(side.color()),
                    side.width().toPlainString(), side.style().wireName(),
                    side.strokeAlign().toPlainString()});
            }
        }

        private void populateRadius(
                Optional<PropertyValue.BoxDecorationValue.BorderRadiusGeometry> value) {
            radiusModel.setRowCount(0);
            radiusEnabled.setSelected(value.isPresent());
            boolean directional = value.orElse(null)
                    instanceof PropertyValue.BoxDecorationValue.DirectionalBorderRadius;
            radiusBasis.setSelectedIndex(directional ? 1 : 0);
            List<String> names = directional
                    ? List.of("Top start", "Top end", "Bottom end", "Bottom start")
                    : List.of("Top left", "Top right", "Bottom right", "Bottom left");
            List<PropertyValue.BoxDecorationValue.Radius> radii;
            if (value.orElse(null)
                    instanceof PropertyValue.BoxDecorationValue.PhysicalBorderRadius physical) {
                radii = List.of(physical.topLeft(), physical.topRight(),
                        physical.bottomRight(), physical.bottomLeft());
            } else if (value.orElse(null)
                    instanceof PropertyValue.BoxDecorationValue.DirectionalBorderRadius directionalRadius) {
                radii = List.of(directionalRadius.topStart(), directionalRadius.topEnd(),
                        directionalRadius.bottomEnd(), directionalRadius.bottomStart());
            } else {
                PropertyValue.BoxDecorationValue.Radius zero = radius(BigDecimal.ZERO, BigDecimal.ZERO);
                radii = List.of(zero, zero, zero, zero);
            }
            for (int index = 0; index < 4; index++) {
                radiusModel.addRow(new Object[]{names.get(index),
                    radii.get(index).x().toPlainString(), radii.get(index).y().toPlainString()});
            }
        }

        private void populateGradient(
                Optional<PropertyValue.BoxDecorationValue.BoxGradient> value) {
            gradientModel.setRowCount(0);
            gradientIds.clear();
            if (value.isEmpty()) {
                gradientType.setSelectedItem("None");
                addGradientStop(StableId.random(), new Object[]{"0xFF000000", "0"});
                addGradientStop(StableId.random(), new Object[]{"0xFFFFFFFF", "1"});
                return;
            }
            PropertyValue.BoxDecorationValue.BoxGradient gradient = value.orElseThrow();
            tileMode.setSelectedItem(gradient.tileMode());
            rotationEnabled.setSelected(gradient.rotationRadians().isPresent());
            rotation.setText(gradient.rotationRadians().map(BigDecimal::toPlainString).orElse("0"));
            gradient.stops().forEach(stop -> addGradientStop(stop.id(), new Object[]{
                colorText(stop.color()), stop.stop().toPlainString()
            }));
            if (gradient instanceof PropertyValue.BoxDecorationValue.LinearGradient linear) {
                gradientType.setSelectedItem("Linear");
                setAlignment(firstBasis, firstX, firstY, linear.begin());
                setAlignment(secondBasis, secondX, secondY, linear.end());
                secondEnabled.setSelected(true);
            } else if (gradient instanceof PropertyValue.BoxDecorationValue.RadialGradient radial) {
                gradientType.setSelectedItem("Radial");
                setAlignment(firstBasis, firstX, firstY, radial.center());
                radial.focal().ifPresentOrElse(
                        focal -> setAlignment(secondBasis, secondX, secondY, focal),
                        () -> setAlignment(secondBasis, secondX, secondY, alignment(0, 0)));
                secondEnabled.setSelected(radial.focal().isPresent());
                radius.setText(radial.radius().toPlainString());
                focalRadius.setText(radial.focalRadius().toPlainString());
            } else {
                PropertyValue.BoxDecorationValue.SweepGradient sweep =
                        (PropertyValue.BoxDecorationValue.SweepGradient) gradient;
                gradientType.setSelectedItem("Sweep");
                setAlignment(firstBasis, firstX, firstY, sweep.center());
                secondEnabled.setSelected(false);
                startAngle.setText(sweep.startAngle().toPlainString());
                endAngle.setText(sweep.endAngle().toPlainString());
            }
        }

        private void installListeners() {
            useDefault.addActionListener(ignored -> refresh());
            shape.addActionListener(ignored -> refresh());
            blend.addActionListener(ignored -> refresh());
            for (JComboBox<?> combo : List.of(
                    imageColorFilter,
                    imageFilterBlend,
                    imageFit,
                    imageAlignmentBasis,
                    imageRepeat,
                    imageFilterQuality)) {
                combo.addActionListener(ignored -> refresh());
            }
            for (JCheckBox check : List.of(
                    imageEnabled,
                    imageOnErrorEnabled,
                    imageCenterSliceEnabled,
                    imageMatchTextDirection,
                    imageInvertColors,
                    imageAntiAlias)) {
                check.addActionListener(ignored -> refresh());
            }
            for (JTextField field : List.of(
                    imageOnError,
                    imageFilterMatrix,
                    imageFilterSaturation,
                    imageAlignmentHorizontal,
                    imageAlignmentVertical,
                    imageSliceLeft,
                    imageSliceTop,
                    imageSliceRight,
                    imageSliceBottom,
                    imageScale,
                    imageOpacity)) {
                field.getDocument().addDocumentListener(listener(this::refresh));
            }
            borderEnabled.addActionListener(ignored -> refresh());
            borderBasis.addActionListener(ignored -> {
                if (!updating) {
                    int selectedBasis = borderBasis.getSelectedIndex();
                    if (selectedBasis != currentBorderBasis) {
                        updating = true;
                        try {
                            LiveDraftTable drafts = (LiveDraftTable) borderTable;
                            drafts.synchronizeLiveDrafts();
                            if (borderTable.isEditing()) {
                                borderTable.getCellEditor().stopCellEditing();
                            }
                            drafts.clearLiveDrafts();
                            if (selectedBasis == 1) {
                                // Preserve the current LTR visual sides:
                                // top/right/bottom/left -> top/start/end/bottom.
                                remapRows(borderModel,
                                        List.of("Top", "Start", "End", "Bottom"),
                                        0, 3, 1, 2);
                            } else {
                                // top/start/end/bottom -> top/right/bottom/left.
                                remapRows(borderModel,
                                        List.of("Top", "Right", "Bottom", "Left"),
                                        0, 2, 3, 1);
                            }
                            currentBorderBasis = selectedBasis;
                        } finally {
                            updating = false;
                        }
                    }
                    refresh();
                }
            });
            radiusEnabled.addActionListener(ignored -> refresh());
            radiusBasis.addActionListener(ignored -> {
                if (!updating) {
                    renameRows(radiusModel, radiusBasis.getSelectedIndex() == 0
                            ? List.of("Top left", "Top right", "Bottom right", "Bottom left")
                            : List.of("Top start", "Top end", "Bottom end", "Bottom start"));
                    refresh();
                }
            });
            gradientType.addActionListener(ignored -> refresh());
            tileMode.addActionListener(ignored -> refresh());
            rotationEnabled.addActionListener(ignored -> refresh());
            secondEnabled.addActionListener(ignored -> refresh());
            borderModel.addTableModelListener(this::tableChanged);
            radiusModel.addTableModelListener(this::tableChanged);
            shadowModel.addTableModelListener(this::tableChanged);
            gradientModel.addTableModelListener(this::tableChanged);
            for (JTextField field : List.of(rotation, firstX, firstY, secondX, secondY,
                    radius, focalRadius, startAngle, endAngle)) {
                field.getDocument().addDocumentListener(listener(this::refresh));
            }
            firstBasis.addActionListener(ignored -> refresh());
            secondBasis.addActionListener(ignored -> refresh());
        }

        private void tableChanged(TableModelEvent ignored) {
            if (!preparingCommit && !updating) {
                refresh();
            }
        }

        private void refresh() {
            refresh(true);
        }

        private boolean refresh(boolean requestValidation) {
            if (updating) {
                return false;
            }
            synchronizeLiveDrafts(borderTable, radiusTable, shadowTable, gradientTable);
            updateEnabledState();
            boolean unset = binding.optional() && useDefault.isSelected();
            if (unset) {
                clearErrors(this);
                storeValid(FlutterPropertyCellValue.unset(), requestValidation);
                return true;
            }
            try {
                PropertyValue.BoxDecorationValue value = new PropertyValue.BoxDecorationValue(
                        fill.value(), readImage(), readBorder(), readRadius(),
                        readShadows(), readGradient(),
                        NOT_SET.equals(blend.getSelectedItem()) ? Optional.empty()
                                : Optional.of(PropertyValue.PaintValue.BlendMode.fromWireName(
                                        Objects.toString(blend.getSelectedItem()))),
                        (PropertyValue.BoxDecorationValue.BoxShape) shape.getSelectedItem());
                clearErrors(this);
                storeValid(FlutterPropertyCellValue.explicit(value), requestValidation);
                return true;
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), this);
                return false;
            }
        }

        private Optional<PropertyValue.DecorationImageValue> readImage() {
            if (!imageEnabled.isSelected()) {
                return Optional.empty();
            }
            PropertyValue.ImageProviderValue provider = imageProviderEditor.value();
            Optional<PropertyValue.CallbackValue> onError =
                    imageOnErrorEnabled.isSelected()
                    ? Optional.of(new PropertyValue.CallbackValue(
                            imageOnError.getText().strip()))
                    : Optional.empty();
            Optional<PropertyValue.DecorationImageValue.BoxFit> fit =
                    NOT_SET.equals(imageFit.getSelectedItem())
                    ? Optional.empty()
                    : Optional.of(PropertyValue.DecorationImageValue.BoxFit
                            .fromWireName(Objects.toString(
                                    imageFit.getSelectedItem())));
            Optional<PropertyValue.DecorationImageValue.Rect> centerSlice =
                    imageCenterSliceEnabled.isSelected()
                    ? Optional.of(new PropertyValue.DecorationImageValue.Rect(
                            nonNegative(imageSliceLeft.getText(),
                                    "Center slice left"),
                            nonNegative(imageSliceTop.getText(),
                                    "Center slice top"),
                            nonNegative(imageSliceRight.getText(),
                                    "Center slice right"),
                            nonNegative(imageSliceBottom.getText(),
                                    "Center slice bottom")))
                    : Optional.empty();
            return Optional.of(new PropertyValue.DecorationImageValue(
                    provider,
                    onError,
                    readImageColorFilter(),
                    fit,
                    new PropertyValue.AlignmentGeometryValue(
                            (PropertyValue.AlignmentGeometryValue.HorizontalBasis)
                            imageAlignmentBasis.getSelectedItem(),
                            decimal(imageAlignmentHorizontal,
                                    "Image alignment horizontal"),
                            decimal(imageAlignmentVertical,
                                    "Image alignment vertical")),
                    centerSlice,
                    (PropertyValue.DecorationImageValue.ImageRepeat)
                    imageRepeat.getSelectedItem(),
                    imageMatchTextDirection.isSelected(),
                    decimal(imageScale, "DecorationImage scale"),
                    decimal(imageOpacity, "DecorationImage opacity"),
                    (PropertyValue.PaintValue.FilterQuality)
                    imageFilterQuality.getSelectedItem(),
                    imageInvertColors.isSelected(),
                    imageAntiAlias.isSelected()));
        }

        private Optional<PropertyValue.DecorationImageValue.ColorFilter>
                readImageColorFilter() {
            String selected = Objects.toString(
                    imageColorFilter.getSelectedItem(), "None");
            return switch (selected) {
                case "None" -> Optional.empty();
                case "Mode" -> Optional.of(
                        new PropertyValue.DecorationImageValue.Mode(
                                imageFilterColor.value().orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Choose a typed ColorFilter color.")),
                                (PropertyValue.PaintValue.BlendMode)
                                imageFilterBlend.getSelectedItem()));
                case "Matrix" -> Optional.of(
                        new PropertyValue.DecorationImageValue.Matrix(
                                matrixValues(imageFilterMatrix.getText())));
                case "Linear to sRGB gamma" -> Optional.of(
                        new PropertyValue.DecorationImageValue
                                .LinearToSrgbGamma());
                case "sRGB to linear gamma" -> Optional.of(
                        new PropertyValue.DecorationImageValue
                                .SrgbToLinearGamma());
                case "Saturation" -> Optional.of(
                        new PropertyValue.DecorationImageValue.Saturation(
                                decimal(imageFilterSaturation,
                                        "ColorFilter saturation")));
                default -> throw new IllegalArgumentException(
                        "Choose a reviewed ColorFilter variant.");
            };
        }

        @Override
        boolean prepareCommit() {
            preparingCommit = true;
            try {
                for (JTable table : List.of(
                        borderTable, radiusTable, shadowTable, gradientTable)) {
                    if (table.isEditing()) {
                        int row = table.getEditingRow();
                        int column = table.getEditingColumn();
                        Object editedValue = switch (table.getEditorComponent()) {
                            case JTextField text -> text.getText();
                            case JComboBox<?> combo -> combo.getSelectedItem();
                            default -> table.getCellEditor().getCellEditorValue();
                        };
                        if (!table.getCellEditor().stopCellEditing()) {
                            markInvalid(
                                    "Finish or correct the active table cell before applying.",
                                    table);
                            return false;
                        }
                        // Some NetBeans look-and-feels deliver JTable's model event
                        // after PropertyEnv's validation listener. Persist the exact
                        // accepted cell synchronously before rebuilding the draft.
                        table.getModel().setValueAt(editedValue, row, column);
                    }
                }
                return refresh(false);
            } finally {
                preparingCommit = false;
            }
        }

        private Optional<PropertyValue.BoxDecorationValue.BoxBorder> readBorder() {
            if (!borderEnabled.isSelected()) {
                return Optional.empty();
            }
            ArrayList<PropertyValue.BoxDecorationValue.BorderSide> sides = new ArrayList<>();
            for (int row = 0; row < 4; row++) {
                BigDecimal strokeAlign = decimal(cell(borderModel, row, 4), "Stroke align");
                sides.add(new PropertyValue.BoxDecorationValue.BorderSide(
                        parseColor(cell(borderModel, row, 1), binding.allowedColorSourceTokens()),
                        nonNegative(cell(borderModel, row, 2), "Border width"),
                        PropertyValue.BoxDecorationValue.BorderStyle.fromWireName(
                                cell(borderModel, row, 3)), strokeAlign));
            }
            return Optional.of(borderBasis.getSelectedIndex() == 0
                    ? new PropertyValue.BoxDecorationValue.PhysicalBorder(
                            sides.get(0), sides.get(1), sides.get(2), sides.get(3))
                    : new PropertyValue.BoxDecorationValue.DirectionalBorder(
                            sides.get(0), sides.get(1), sides.get(2), sides.get(3)));
        }

        private Optional<PropertyValue.BoxDecorationValue.BorderRadiusGeometry> readRadius() {
            if (!radiusEnabled.isSelected()) {
                return Optional.empty();
            }
            ArrayList<PropertyValue.BoxDecorationValue.Radius> values = new ArrayList<>();
            for (int row = 0; row < 4; row++) {
                values.add(radius(nonNegative(cell(radiusModel, row, 1), "Radius X"),
                        nonNegative(cell(radiusModel, row, 2), "Radius Y")));
            }
            return Optional.of(radiusBasis.getSelectedIndex() == 0
                    ? new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                            values.get(0), values.get(1), values.get(2), values.get(3))
                    : new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                            values.get(0), values.get(1), values.get(2), values.get(3)));
        }

        private List<PropertyValue.BoxDecorationValue.BoxShadow> readShadows() {
            ArrayList<PropertyValue.BoxDecorationValue.BoxShadow> values = new ArrayList<>();
            for (int row = 0; row < shadowModel.getRowCount(); row++) {
                values.add(new PropertyValue.BoxDecorationValue.BoxShadow(
                        shadowIds.get(row),
                        parseColor(cell(shadowModel, row, 0), binding.allowedColorSourceTokens()),
                        decimal(cell(shadowModel, row, 1), "Shadow X"),
                        decimal(cell(shadowModel, row, 2), "Shadow Y"),
                        nonNegative(cell(shadowModel, row, 3), "Shadow blur"),
                        decimal(cell(shadowModel, row, 4), "Shadow spread"),
                        PropertyValue.PaintValue.BlurStyle.fromWireName(
                                cell(shadowModel, row, 5))));
            }
            return values;
        }

        private Optional<PropertyValue.BoxDecorationValue.BoxGradient> readGradient() {
            String type = Objects.toString(gradientType.getSelectedItem(), "None");
            if ("None".equals(type)) {
                return Optional.empty();
            }
            if (gradientModel.getRowCount() < 2) {
                throw new IllegalArgumentException("A gradient requires at least two color stops.");
            }
            ArrayList<PropertyValue.BoxDecorationValue.GradientStop> stops = new ArrayList<>();
            for (int row = 0; row < gradientModel.getRowCount(); row++) {
                stops.add(new PropertyValue.BoxDecorationValue.GradientStop(
                        gradientIds.get(row),
                        parseColor(cell(gradientModel, row, 0), binding.allowedColorSourceTokens()),
                        decimal(cell(gradientModel, row, 1), "Gradient stop")));
            }
            Optional<BigDecimal> transform = rotationEnabled.isSelected()
                    ? Optional.of(decimal(rotation, "Gradient rotation")) : Optional.empty();
            PropertyValue.AlignmentGeometryValue first = readAlignment(
                    firstBasis, firstX, firstY, "First alignment");
            PropertyValue.AlignmentGeometryValue second = readAlignment(
                    secondBasis, secondX, secondY, "Second alignment");
            PropertyValue.BoxDecorationValue.TileMode mode =
                    (PropertyValue.BoxDecorationValue.TileMode) tileMode.getSelectedItem();
            return Optional.of(switch (type) {
                case "Linear" -> new PropertyValue.BoxDecorationValue.LinearGradient(
                        first, second, stops, mode, transform);
                case "Radial" -> new PropertyValue.BoxDecorationValue.RadialGradient(
                        first, nonNegative(radius.getText(), "Gradient radius"),
                        secondEnabled.isSelected() ? Optional.of(second) : Optional.empty(),
                        secondEnabled.isSelected()
                                ? nonNegative(focalRadius.getText(), "Focal radius")
                                : BigDecimal.ZERO,
                        stops, mode, transform);
                case "Sweep" -> new PropertyValue.BoxDecorationValue.SweepGradient(
                        first, decimal(startAngle, "Start angle"),
                        decimal(endAngle, "End angle"), stops, mode, transform);
                default -> throw new IllegalArgumentException("Choose a gradient type.");
            });
        }

        private void updateEnabledState() {
            boolean enabled = !(binding.optional() && useDefault.isSelected());
            for (Component component : getComponents()) {
                setEnabledRecursively(component, enabled, useDefault);
            }
            if (!enabled) {
                return;
            }
            boolean hasImage = imageEnabled.isSelected();
            for (JComponent component : List.of(
                    imageOnErrorEnabled,
                    imageColorFilter,
                    imageFit,
                    imageAlignmentBasis,
                    imageAlignmentHorizontal,
                    imageAlignmentVertical,
                    imageCenterSliceEnabled,
                    imageRepeat,
                    imageMatchTextDirection,
                    imageScale,
                    imageOpacity,
                    imageFilterQuality,
                    imageInvertColors,
                    imageAntiAlias)) {
                component.setEnabled(hasImage);
            }
            imageProviderEditor.updateEnabledState(hasImage);
            imageOnError.setEnabled(
                    hasImage && imageOnErrorEnabled.isSelected());
            String colorFilter = Objects.toString(
                    imageColorFilter.getSelectedItem(), "None");
            boolean modeFilter = hasImage && "Mode".equals(colorFilter);
            imageFilterColor.setEnabled(modeFilter);
            setEnabledRecursively(imageFilterColor, modeFilter, null);
            imageFilterBlend.setEnabled(modeFilter);
            imageFilterMatrix.setEnabled(
                    hasImage && "Matrix".equals(colorFilter));
            imageFilterSaturation.setEnabled(
                    hasImage && "Saturation".equals(colorFilter));
            boolean centerSlice = hasImage
                    && imageCenterSliceEnabled.isSelected();
            imageSliceLeft.setEnabled(centerSlice);
            imageSliceTop.setEnabled(centerSlice);
            imageSliceRight.setEnabled(centerSlice);
            imageSliceBottom.setEnabled(centerSlice);
            borderTable.setEnabled(borderEnabled.isSelected());
            borderBasis.setEnabled(borderEnabled.isSelected());
            boolean circle = shape.getSelectedItem()
                    == PropertyValue.BoxDecorationValue.BoxShape.CIRCLE;
            radiusEnabled.setEnabled(!circle);
            radiusTable.setEnabled(radiusEnabled.isSelected() && !circle);
            radiusBasis.setEnabled(radiusEnabled.isSelected() && !circle);
            if (circle && radiusEnabled.isSelected()) {
                radiusEnabled.setSelected(false);
            }
            boolean gradient = !"None".equals(gradientType.getSelectedItem());
            tileMode.setEnabled(gradient);
            rotationEnabled.setEnabled(gradient);
            rotation.setEnabled(gradient && rotationEnabled.isSelected());
            gradientTable.setEnabled(gradient);
            String type = Objects.toString(gradientType.getSelectedItem(), "None");
            boolean linear = "Linear".equals(type);
            boolean radial = "Radial".equals(type);
            boolean sweep = "Sweep".equals(type);
            firstBasis.setEnabled(gradient);
            firstX.setEnabled(gradient);
            firstY.setEnabled(gradient);
            secondEnabled.setEnabled(radial);
            secondBasis.setEnabled(linear || (radial && secondEnabled.isSelected()));
            secondX.setEnabled(linear || (radial && secondEnabled.isSelected()));
            secondY.setEnabled(linear || (radial && secondEnabled.isSelected()));
            radius.setEnabled(radial);
            focalRadius.setEnabled(radial && secondEnabled.isSelected());
            startAngle.setEnabled(sweep);
            endAngle.setEnabled(sweep);
            firstLabel.setText(linear ? "Begin:" : "Center:");
            secondLabel.setText(linear ? "End:" : "Focal:");
        }

        private void addShadow(StableId id, Object[] row) {
            shadowIds.add(id);
            shadowModel.addRow(row);
        }

        private void addGradientStop(StableId id, Object[] row) {
            gradientIds.add(id);
            gradientModel.addRow(row);
        }

        private static PropertyValue.BoxDecorationValue emptyDecoration() {
            return new PropertyValue.BoxDecorationValue(
                    Optional.empty(), Optional.empty(), Optional.empty(), List.of(),
                    Optional.empty(), Optional.empty(),
                    PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        }
    }

    private static final class ColorSourceEditor extends JPanel {
        private final JComboBox<String> mode = new JComboBox<>();
        private final JTextField argb = new JTextField("0xFF000000", 12);
        private final JComboBox<String> role = new JComboBox<>();
        private final JButton choose = new JButton("Choose…");
        private final List<ThemeToken> allowed;
        private final Runnable changed;
        private boolean updating;

        ColorSourceEditor(List<ThemeToken> allowed, boolean optional, Runnable changed) {
            super(new FlowLayout(FlowLayout.LEADING, 4, 0));
            this.allowed = List.copyOf(allowed);
            this.changed = Objects.requireNonNull(changed, "changed");
            if (optional) {
                mode.addItem(NOT_SET);
            }
            mode.addItem(LITERAL);
            mode.addItem(THEME);
            mode.setName(DECORATION_COLOR_MODE_NAME);
            FlutterThemePropertyRoles.colorDisplayRoles(allowed).forEach(role::addItem);
            add(mode);
            add(argb);
            add(role);
            add(choose);
            getAccessibleContext().setAccessibleName("Theme-aware color source");
            getAccessibleContext().setAccessibleDescription(
                    "Chooses no fill, an exact ARGB literal, or a semantic project theme role.");
            mode.getAccessibleContext().setAccessibleName("Color source mode");
            argb.getAccessibleContext().setAccessibleName("Literal ARGB color");
            argb.getAccessibleContext().setAccessibleDescription(
                    "Exact eight-digit Flutter color in 0xAARRGGBB form.");
            role.getAccessibleContext().setAccessibleName("Material ColorScheme role");
            choose.getAccessibleContext().setAccessibleDescription(
                    "Opens a visual RGB chooser while preserving the alpha channel.");
            mode.addActionListener(ignored -> notifyChange());
            role.addActionListener(ignored -> notifyChange());
            argb.getDocument().addDocumentListener(listener(this::notifyChange));
            choose.addActionListener(ignored -> chooseColor());
            updateEnabled();
        }

        void setValue(Optional<ColorSource> value) {
            updating = true;
            try {
                if (value.isEmpty()) {
                    mode.setSelectedItem(NOT_SET);
                } else if (value.orElseThrow() instanceof ColorSource.Literal literal) {
                    mode.setSelectedItem(LITERAL);
                    argb.setText(literal.wireArgb());
                } else {
                    ColorSource.Theme theme = (ColorSource.Theme) value.orElseThrow();
                    mode.setSelectedItem(THEME);
                    role.setSelectedItem(FlutterThemePropertyRoles.displayRole(theme.token()));
                }
            } finally {
                updating = false;
            }
            updateEnabled();
        }

        Optional<ColorSource> value() {
            String selected = Objects.toString(mode.getSelectedItem(), NOT_SET);
            if (NOT_SET.equals(selected)) {
                return Optional.empty();
            }
            return Optional.of(LITERAL.equals(selected)
                    ? ColorSource.Literal.fromWireArgb(normalizeArgb(argb.getText()))
                    : new ColorSource.Theme(FlutterThemePropertyRoles.findColorToken(
                            Objects.toString(role.getSelectedItem(), ""), allowed)
                            .orElseThrow(() -> new IllegalArgumentException(
                            "Choose a reviewed Material ColorScheme role."))));
        }

        private void chooseColor() {
            try {
                ColorSource.Literal current = ColorSource.Literal.fromWireArgb(
                        normalizeArgb(argb.getText()));
                Color selected = JColorChooser.showDialog(this, "Choose literal color",
                        new Color((int) current.argb(), true));
                if (selected != null) {
                    long alpha = (current.argb() >>> 24) & 0xFF;
                    long value = (alpha << 24) | ((long) selected.getRed() << 16)
                            | ((long) selected.getGreen() << 8) | selected.getBlue();
                    mode.setSelectedItem(LITERAL);
                    argb.setText(String.format(Locale.ROOT, "0x%08X", value));
                }
            } catch (IllegalArgumentException failure) {
                argb.putClientProperty("JComponent.outline", "error");
                argb.setToolTipText(failure.getMessage());
            }
        }

        private void notifyChange() {
            updateEnabled();
            if (!updating) {
                changed.run();
            }
        }

        private void updateEnabled() {
            boolean literal = LITERAL.equals(mode.getSelectedItem());
            argb.setEnabled(literal);
            choose.setEnabled(literal);
            role.setEnabled(THEME.equals(mode.getSelectedItem()));
        }
    }

    private static JPanel orderedTablePanel(
            JTable table, Runnable add, Runnable remove, Runnable up, Runnable down) {
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        JButton upButton = new JButton("Move up");
        JButton downButton = new JButton("Move down");
        addButton.addActionListener(ignored -> add.run());
        removeButton.addActionListener(ignored -> remove.run());
        upButton.addActionListener(ignored -> up.run());
        downButton.addActionListener(ignored -> down.run());
        addButton.getAccessibleContext().setAccessibleDescription(
                "Adds an item to the ordered list.");
        removeButton.getAccessibleContext().setAccessibleDescription(
                "Removes the selected item from the ordered list.");
        upButton.getAccessibleContext().setAccessibleDescription(
                "Moves the selected item one position earlier.");
        downButton.getAccessibleContext().setAccessibleDescription(
                "Moves the selected item one position later.");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        buttons.add(addButton);
        buttons.add(removeButton);
        buttons.add(upButton);
        buttons.add(downButton);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(buttons, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private static DefaultTableModel nonEditableFirstColumn(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
        };
    }

    private static JTable table(DefaultTableModel model, String name) {
        JTable table = new LiveDraftTable(model);
        table.setName(name);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        String accessibleName = switch (name) {
            case DECORATION_BORDER_TABLE_NAME -> "BoxDecoration border sides";
            case DECORATION_RADIUS_TABLE_NAME -> "BoxDecoration corner radii";
            case BORDER_RADIUS_TABLE_NAME -> "BorderRadius corner radii";
            case DECORATION_SHADOW_TABLE_NAME -> "Ordered BoxDecoration shadows";
            case DECORATION_GRADIENT_TABLE_NAME -> "Ordered gradient color stops";
            default -> "Structured Container values";
        };
        table.getAccessibleContext().setAccessibleName(accessibleName);
        table.getAccessibleContext().setAccessibleDescription(
                accessibleName + "; edit cells directly and use the adjacent ordering controls.");
        return table;
    }

    /**
     * Mirrors an active editor into the dialog's local table model as the user
     * types. NetBeans may request PropertyEnv validation before a look-and-feel
     * delivers its final editingStopped event; a structured value must never
     * publish the preceding cell draft in that ordering.
     */
    private static final class LiveDraftTable extends JTable {
        private final HashMap<Long, Object> liveDrafts = new HashMap<>();
        private javax.swing.text.Document activeDocument;
        private DocumentListener activeDocumentListener;
        private JComboBox<?> activeCombo;
        private ActionListener activeComboListener;

        LiveDraftTable(DefaultTableModel model) {
            super(model);
        }

        @Override
        public Component prepareEditor(TableCellEditor editor, int row, int column) {
            detachLiveEditor();
            Component component = super.prepareEditor(editor, row, column);
            if (component instanceof JTextField text) {
                activeDocument = text.getDocument();
                activeDocumentListener = listener(() -> {
                    if (getEditingRow() == row && getEditingColumn() == column) {
                        liveDrafts.put(cellKey(row, column), text.getText());
                        getModel().setValueAt(text.getText(), row, column);
                    }
                });
                activeDocument.addDocumentListener(activeDocumentListener);
            } else if (component instanceof JComboBox<?> combo) {
                activeCombo = combo;
                activeComboListener = ignored -> {
                    if (getEditingRow() == row && getEditingColumn() == column) {
                        liveDrafts.put(cellKey(row, column), combo.getSelectedItem());
                        getModel().setValueAt(combo.getSelectedItem(), row, column);
                    }
                };
                activeCombo.addActionListener(activeComboListener);
            }
            return component;
        }

        @Override
        public void removeEditor() {
            detachLiveEditor();
            super.removeEditor();
        }

        private void detachLiveEditor() {
            if (activeDocument != null && activeDocumentListener != null) {
                activeDocument.removeDocumentListener(activeDocumentListener);
            }
            if (activeCombo != null && activeComboListener != null) {
                activeCombo.removeActionListener(activeComboListener);
            }
            activeDocument = null;
            activeDocumentListener = null;
            activeCombo = null;
            activeComboListener = null;
        }

        void synchronizeLiveDrafts() {
            for (var entry : liveDrafts.entrySet()) {
                int row = (int) (entry.getKey() >>> 32);
                int column = (int) (entry.getKey().longValue());
                if (row < getRowCount() && column < getColumnCount()
                        && !Objects.equals(getModel().getValueAt(row, column), entry.getValue())) {
                    getModel().setValueAt(entry.getValue(), row, column);
                }
            }
        }

        void clearLiveDrafts() {
            liveDrafts.clear();
        }

        private static long cellKey(int row, int column) {
            return ((long) row << 32) | (column & 0xFFFF_FFFFL);
        }
    }

    private static void synchronizeLiveDrafts(JTable... tables) {
        for (JTable table : tables) {
            ((LiveDraftTable) table).synchronizeLiveDrafts();
        }
    }

    private static void installColorCellEditor(
            JTable table, int column, List<ThemeToken> allowed) {
        JComboBox<String> values = new JComboBox<>();
        values.setEditable(true);
        values.addItem("0xFF000000");
        FlutterThemePropertyRoles.colorDisplayRoles(allowed)
                .forEach(value -> values.addItem("Theme: " + value));
        table.getColumnModel().getColumn(column).setCellEditor(new DefaultCellEditor(values));
    }

    private static void removeSelected(
            JTable table, DefaultTableModel model, ArrayList<StableId> ids) {
        LiveDraftTable drafts = (LiveDraftTable) table;
        drafts.synchronizeLiveDrafts();
        drafts.clearLiveDrafts();
        int row = table.getSelectedRow();
        if (row >= 0) {
            ids.remove(row);
            model.removeRow(row);
            if (model.getRowCount() > 0) {
                table.setRowSelectionInterval(Math.min(row, model.getRowCount() - 1),
                        Math.min(row, model.getRowCount() - 1));
            }
        }
    }

    private static void moveSelected(
            JTable table, DefaultTableModel model, ArrayList<StableId> ids, int delta) {
        LiveDraftTable drafts = (LiveDraftTable) table;
        drafts.synchronizeLiveDrafts();
        drafts.clearLiveDrafts();
        int row = table.getSelectedRow();
        int target = row + delta;
        if (row < 0 || target < 0 || target >= model.getRowCount()) {
            return;
        }
        model.moveRow(row, row, target);
        StableId id = ids.remove(row);
        ids.add(target, id);
        table.setRowSelectionInterval(target, target);
    }

    private static void renameRows(DefaultTableModel model, List<String> names) {
        for (int row = 0; row < names.size() && row < model.getRowCount(); row++) {
            model.setValueAt(names.get(row), row, 0);
        }
    }

    private static void remapRows(
            DefaultTableModel model, List<String> names, int... sourceRows) {
        if (sourceRows.length != names.size() || model.getRowCount() < sourceRows.length) {
            throw new IllegalArgumentException("Row remapping must cover every named row");
        }
        ArrayList<List<Object>> snapshot = new ArrayList<>(sourceRows.length);
        for (int sourceRow : sourceRows) {
            if (sourceRow < 0 || sourceRow >= model.getRowCount()) {
                throw new IllegalArgumentException("Row remapping source is outside the table");
            }
            ArrayList<Object> values = new ArrayList<>(model.getColumnCount());
            for (int column = 0; column < model.getColumnCount(); column++) {
                values.add(model.getValueAt(sourceRow, column));
            }
            snapshot.add(List.copyOf(values));
        }
        for (int row = 0; row < sourceRows.length; row++) {
            model.setValueAt(names.get(row), row, 0);
            for (int column = 1; column < model.getColumnCount(); column++) {
                model.setValueAt(snapshot.get(row).get(column), row, column);
            }
        }
    }

    private static PropertyValue.BoxDecorationValue.BorderSide defaultSide() {
        return new PropertyValue.BoxDecorationValue.BorderSide(
                new ColorSource.Literal(0xFF000000L), BigDecimal.ZERO,
                PropertyValue.BoxDecorationValue.BorderStyle.NONE, BigDecimal.ONE.negate());
    }

    private static PropertyValue.BoxDecorationValue.Radius radius(
            BigDecimal x, BigDecimal y) {
        return new PropertyValue.BoxDecorationValue.Radius(x, y);
    }

    private static PropertyValue.AlignmentGeometryValue alignment(int x, int y) {
        return new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                BigDecimal.valueOf(x), BigDecimal.valueOf(y));
    }

    private static FlutterImageAssetChoices assetChoices(
            PropertyEnv environment) {
        if (environment == null || environment.getFeatureDescriptor() == null) {
            return FlutterImageAssetChoices.empty();
        }
        Object value = environment.getFeatureDescriptor().getValue(
                FlutterImageAssetChoices.FEATURE_ATTRIBUTE);
        return value instanceof FlutterImageAssetChoices choices
                ? choices : FlutterImageAssetChoices.empty();
    }

    private static JPanel flow(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        for (Component component : components) {
            panel.add(Objects.requireNonNull(component, "component"));
        }
        return panel;
    }

    private static Optional<Integer> optionalInteger(
            JTextField field,
            String label) {
        String text = field.getText().strip();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(text));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    label + " must be a whole number.", failure);
        }
    }

    private static List<BigDecimal> matrixValues(String text) {
        String normalized = Objects.requireNonNull(text, "text").strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "ColorFilter matrix requires exactly 20 decimal values.");
        }
        List<String> parts = Arrays.stream(normalized.split("[,\\s]+"))
                .filter(value -> !value.isBlank()).toList();
        if (parts.size() != PropertyValue.DecorationImageValue.Matrix.VALUE_COUNT) {
            throw new IllegalArgumentException(
                    "ColorFilter matrix requires exactly 20 decimal values.");
        }
        try {
            return parts.stream().map(BigDecimal::new).toList();
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "ColorFilter matrix values must be finite decimals.", failure);
        }
    }

    private static PropertyValue.AlignmentGeometryValue readAlignment(
            JComboBox<PropertyValue.AlignmentGeometryValue.HorizontalBasis> basis,
            JTextField x, JTextField y, String label) {
        return new PropertyValue.AlignmentGeometryValue(
                (PropertyValue.AlignmentGeometryValue.HorizontalBasis) basis.getSelectedItem(),
                decimal(x, label + " horizontal"), decimal(y, label + " vertical"));
    }

    private static void setAlignment(
            JComboBox<PropertyValue.AlignmentGeometryValue.HorizontalBasis> basis,
            JTextField x, JTextField y,
            PropertyValue.AlignmentGeometryValue value) {
        basis.setSelectedItem(value.basis());
        x.setText(value.horizontal().toPlainString());
        y.setText(value.vertical().toPlainString());
    }

    private static ColorSource parseColor(String text, List<ThemeToken> allowed) {
        String normalized = Objects.requireNonNull(text, "text").strip();
        if (normalized.regionMatches(true, 0, "Theme:", 0, 6)) {
            String role = normalized.substring(6).strip();
            return new ColorSource.Theme(FlutterThemePropertyRoles.findColorToken(role, allowed)
                    .orElseThrow(() -> new IllegalArgumentException(
                    "Unknown reviewed Material ColorScheme role: " + role)));
        }
        return ColorSource.Literal.fromWireArgb(normalizeArgb(normalized));
    }

    private static String colorText(ColorSource color) {
        return switch (color) {
            case ColorSource.Literal literal -> literal.wireArgb();
            case ColorSource.Theme theme ->
                "Theme: " + FlutterThemePropertyRoles.displayRole(theme.token());
        };
    }

    private static String normalizeArgb(String text) {
        String normalized = Objects.requireNonNull(text, "text").strip();
        if (normalized.length() >= 2 && normalized.substring(0, 2).equalsIgnoreCase("0x")) {
            return "0x" + normalized.substring(2).toUpperCase(Locale.ROOT);
        }
        return normalized;
    }

    private static BigDecimal nonNegative(String text, String label) {
        BigDecimal value = decimal(text, label);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(label + " must be non-negative.");
        }
        return value;
    }

    private static BigDecimal decimal(JTextField field, String label) {
        return decimal(field.getText(), label);
    }

    private static BigDecimal decimal(String text, String label) {
        try {
            return new BigDecimal(Objects.requireNonNull(text, "text").strip());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(label + " must be a finite decimal number.", failure);
        }
    }

    private static String cell(DefaultTableModel model, int row, int column) {
        return Objects.toString(model.getValueAt(row, column), "").strip();
    }

    private static void setEnabledRecursively(
            Component component, boolean enabled, Component except) {
        if (component != except) {
            component.setEnabled(enabled);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                setEnabledRecursively(child, enabled, except);
            }
        }
    }

    private static void clearErrors(Component component) {
        if (component instanceof JComponent swing) {
            swing.putClientProperty("JComponent.outline", null);
            swing.setToolTipText(null);
            Object baseline = swing.getClientProperty(ACCESSIBLE_DESCRIPTION_BASELINE);
            if (baseline == null) {
                swing.putClientProperty(ACCESSIBLE_DESCRIPTION_BASELINE,
                        Optional.ofNullable(swing.getAccessibleContext()
                                .getAccessibleDescription()));
            } else if (baseline instanceof Optional<?> description) {
                swing.getAccessibleContext().setAccessibleDescription(
                        description.map(String.class::cast).orElse(null));
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                clearErrors(child);
            }
        }
    }

    private static <T> void displayWith(
            JComboBox<T> combo,
            Function<T, String> display) {
        Objects.requireNonNull(combo, "combo");
        Objects.requireNonNull(display, "display");
        DefaultListCellRenderer delegate = new DefaultListCellRenderer();
        combo.setRenderer((list, value, index, selected, focused) -> {
            JLabel label = (JLabel) delegate.getListCellRendererComponent(
                    list, value, index, selected, focused);
            label.setText(value == null ? "" : display.apply(value));
            return label;
        });
    }

    private static String humanizeWireName(String value) {
        Objects.requireNonNull(value, "value");
        String spaced = value.replaceAll(
                "(?<=[a-z0-9])(?=[A-Z])", " ");
        return spaced.isEmpty()
                ? spaced
                : spaced.substring(0, 1).toUpperCase(Locale.ROOT)
                + spaced.substring(1);
    }

    private static DocumentListener listener(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { action.run(); }
            @Override public void removeUpdate(DocumentEvent event) { action.run(); }
            @Override public void changedUpdate(DocumentEvent event) { action.run(); }
        };
    }

    private static void addRow(JPanel panel, int row, String label, JComponent value) {
        addRow(panel, row, label, value, null);
    }

    private static void addRow(
            JPanel panel, int row, String text, JComponent value, JComponent trailing) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(4, 4, 4, 8);
        JLabel label = new JLabel(text);
        label.setLabelFor(value);
        panel.add(label, labelConstraints);
        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.insets = new Insets(4, 0, 4, 4);
        panel.add(value, valueConstraints);
        if (trailing != null) {
            GridBagConstraints trailingConstraints = new GridBagConstraints();
            trailingConstraints.gridx = 2;
            trailingConstraints.gridy = row;
            trailingConstraints.insets = new Insets(4, 0, 4, 4);
            panel.add(trailing, trailingConstraints);
        }
    }

    private static void addWideRow(JPanel panel, int row, JComponent component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 4, 4, 4);
        panel.add(component, constraints);
    }

    private static void addAlignmentRow(
            JPanel panel, int row, JLabel label,
            JComboBox<?> basis, JTextField x, JTextField y) {
        JPanel values = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        values.add(basis);
        JLabel xLabel = new JLabel("X:");
        xLabel.setLabelFor(x);
        values.add(xLabel);
        values.add(x);
        JLabel yLabel = new JLabel("Y:");
        yLabel.setLabelFor(y);
        values.add(yLabel);
        values.add(y);
        label.setLabelFor(basis);
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(4, 4, 4, 8);
        panel.add(label, labelConstraints);
        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(values, valueConstraints);
    }

    private static void addVerticalGlue(JPanel panel, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 3;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.VERTICAL;
        panel.add(new JPanel(), constraints);
    }

    private static BigDecimal[] askNumbers(
            Component parent, String title, String[] labels, BigDecimal[] defaults) {
        if (labels.length != defaults.length) {
            throw new IllegalArgumentException("Each Matrix convenience value needs a default");
        }
        JPanel panel = new JPanel(new GridBagLayout());
        JTextField[] fields = new JTextField[labels.length];
        for (int index = 0; index < labels.length; index++) {
            fields[index] = new JTextField(defaults[index].toPlainString(), 12);
            fields[index].getAccessibleContext().setAccessibleName(
                    labels[index] + " Matrix convenience value");
            fields[index].getAccessibleContext().setAccessibleDescription(
                    "Finite decimal exactly representable as a Dart double.");
            addRow(panel, index, labels[index] + ':', fields[index]);
        }
        JLabel validation = new JLabel(" ");
        validation.getAccessibleContext().setAccessibleName("Matrix input validation");
        addRow(panel, labels.length, "", validation);
        while (true) {
            int result = javax.swing.JOptionPane.showConfirmDialog(parent, panel, title,
                    javax.swing.JOptionPane.OK_CANCEL_OPTION,
                    javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (result != javax.swing.JOptionPane.OK_OPTION) {
                return null;
            }
            BigDecimal[] values = new BigDecimal[fields.length];
            boolean valid = true;
            validation.setText(" ");
            validation.getAccessibleContext().setAccessibleDescription(null);
            for (JTextField field : fields) {
                field.putClientProperty("JComponent.outline", null);
                field.setToolTipText(null);
                field.getAccessibleContext().setAccessibleDescription(
                        "Finite decimal exactly representable as a Dart double.");
            }
            for (int index = 0; index < fields.length; index++) {
                JTextField field = fields[index];
                try {
                    values[index] = parseConvenienceNumber(field.getText(), labels[index]);
                } catch (IllegalArgumentException failure) {
                    valid = false;
                    field.putClientProperty("JComponent.outline", "error");
                    field.setToolTipText(failure.getMessage());
                    field.getAccessibleContext().setAccessibleDescription(
                            "Invalid Matrix convenience value. " + failure.getMessage());
                    validation.setText(failure.getMessage());
                    validation.getAccessibleContext().setAccessibleDescription(
                            failure.getMessage());
                    field.requestFocusInWindow();
                    field.selectAll();
                    break;
                }
            }
            if (valid) {
                return values;
            }
        }
    }

    static BigDecimal parseConvenienceNumber(String text, String label) {
        BigDecimal value = decimal(text, label);
        if (!DartNumericLiterals.isRepresentableDouble(value)) {
            throw new IllegalArgumentException(
                    label + " must be exactly representable as a finite Dart double.");
        }
        return value;
    }
}
