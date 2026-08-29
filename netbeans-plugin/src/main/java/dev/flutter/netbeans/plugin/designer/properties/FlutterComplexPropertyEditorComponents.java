package dev.flutter.netbeans.plugin.designer.properties;

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
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Transactional custom editors for structured Flutter Designer values. */
final class FlutterComplexPropertyEditorComponents {
    static final String THEME_COLOR_MODE_NAME = "flutter.themeColor.mode";
    static final String THEME_COLOR_ARGB_NAME = "flutter.color.argb";
    static final String THEME_COLOR_ROLE_NAME = "flutter.themeColor.role";
    static final String PAINT_STYLE_NAME = "flutter.paint.style";
    static final String PAINT_STROKE_WIDTH_NAME = "flutter.paint.strokeWidth";
    static final String SHADOW_TABLE_NAME = "flutter.shadows.table";
    static final String FONT_FEATURE_TABLE_NAME = "flutter.fontFeatures.table";
    static final String FONT_VARIATION_TABLE_NAME = "flutter.fontVariations.table";

    private static final String MODE_NOT_SET = "Not set";
    private static final String MODE_LITERAL = "Literal ARGB";
    private static final String MODE_THEME = "Theme role";

    private FlutterComplexPropertyEditorComponents() {
    }

    static Component customEditor(
            PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding,
            PropertyEnv environment) {
        return switch (binding.editorKind()) {
            case THEME_COLOR -> new ThemeColorPanel(editor, binding, environment);
            case PAINT -> new PaintPanel(editor, binding, environment);
            case SHADOW_LIST -> new ShadowListPanel(editor, binding, environment);
            case FONT_FEATURE_LIST -> new FontFeatureListPanel(editor, binding, environment);
            case FONT_VARIATION_LIST -> new FontVariationListPanel(editor, binding, environment);
            default -> throw new IllegalStateException(
                    "No structured editor for " + binding.editorKind());
        };
    }

    private static final class ThemeColorPanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode = new JComboBox<>();
        private final JTextField argb = new JTextField(12);
        private final JComboBox<String> role = new JComboBox<>();
        private final JButton choose = new JButton("Choose…");
        private final JColorChooser chooser = new JColorChooser();
        private final JSpinner alpha = new JSpinner(
                new SpinnerNumberModel(255, 0, 255, 1));
        private final JTextField alphaText = ((JSpinner.DefaultEditor)
                alpha.getEditor()).getTextField();
        private boolean updating;

        ThemeColorPanel(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(670, 520));
            setName("flutter.themeColor.custom");
            getAccessibleContext().setAccessibleName(
                    binding.definition().name().value() + " theme-aware color editor");
            getAccessibleContext().setAccessibleDescription(
                    "Chooses no value, an exact ARGB literal, or a semantic Material ColorScheme role.");

            if (binding.optional()) {
                mode.addItem(MODE_NOT_SET);
            }
            FlutterThemePropertyRoles.colorDisplayRoles(
                    binding.allowedColorSourceTokens()).forEach(role::addItem);
            mode.addItem(MODE_LITERAL);
            mode.addItem(MODE_THEME);
            mode.setName(THEME_COLOR_MODE_NAME);
            argb.setName(THEME_COLOR_ARGB_NAME);
            role.setName(THEME_COLOR_ROLE_NAME);
            chooser.setName(FlutterPropertyEditorComponents.COLOR_CHOOSER_NAME);
            alpha.setName(FlutterPropertyEditorComponents.COLOR_ALPHA_NAME);
            mode.getAccessibleContext().setAccessibleName("Color source mode");
            argb.getAccessibleContext().setAccessibleName("ARGB color");
            argb.getAccessibleContext().setAccessibleDescription(
                    "Exact eight-digit Flutter color in 0xAARRGGBB form.");
            role.getAccessibleContext().setAccessibleName("Material ColorScheme role");
            role.getAccessibleContext().setAccessibleDescription(
                    "Semantic color that follows the active light or dark project theme.");
            chooser.getAccessibleContext().setAccessibleName("RGB color chooser");
            chooser.getAccessibleContext().setAccessibleDescription(
                    "Chooses literal red, green, and blue color channels.");
            chooser.setPreviewPanel(new JPanel());
            alpha.getAccessibleContext().setAccessibleName("Alpha channel");
            alpha.getAccessibleContext().setAccessibleDescription(
                    "Transparency from 0 fully transparent through 255 fully opaque.");
            choose.getAccessibleContext().setAccessibleDescription(
                    "Opens a visual RGB chooser while preserving the typed alpha channel.");

            JPanel form = new JPanel(new GridBagLayout());
            addRow(form, 0, "Source:", mode, null);
            addRow(form, 1, "ARGB:", argb, choose);
            addRow(form, 2, "Alpha (0–255):", alpha, null);
            addRow(form, 3, "Theme role:", role, null);
            add(form, BorderLayout.NORTH);
            add(chooser, BorderLayout.CENTER);

            FlutterPropertyCellValue initial = initialValue();
            updating = true;
            try {
                Object explicit = initial.explicitValue().orElse(null);
                if (explicit instanceof PropertyValue.ThemeTokenValue token) {
                    mode.setSelectedItem(MODE_THEME);
                    role.setSelectedItem(FlutterThemePropertyRoles.displayRole(token.token()));
                    argb.setText("0xFF000000");
                } else if (explicit instanceof PropertyValue.ColorValue color) {
                    mode.setSelectedItem(MODE_LITERAL);
                    argb.setText(color.wireArgb());
                    showLiteral(new ColorSource.Literal(color.argb()));
                } else {
                    mode.setSelectedItem(binding.optional() ? MODE_NOT_SET : MODE_LITERAL);
                    argb.setText("0xFF000000");
                    showLiteral(new ColorSource.Literal(0xFF000000L));
                }
            } finally {
                updating = false;
            }
            mode.addActionListener(ignored -> updateDraft());
            role.addActionListener(ignored -> updateDraft());
            argb.getDocument().addDocumentListener(listener(this::argbChanged));
            chooser.getSelectionModel().addChangeListener(
                    ignored -> chooserChanged());
            alpha.addChangeListener(ignored -> alphaChanged());
            alphaText.getDocument().addDocumentListener(
                    listener(this::alphaTextChanged));
            choose.addActionListener(ignored -> chooseLiteralColor());
            updateDraft();
            activate();
        }

        private void chooseLiteralColor() {
            try {
                ColorSource.Literal current = ColorSource.Literal.fromWireArgb(
                        normalizeArgb(argb.getText()));
                Color selected = JColorChooser.showDialog(
                        this, "Choose literal color", toAwt(current));
                if (selected == null) {
                    return;
                }
                int alpha = (int) ((current.argb() >>> 24) & 0xFF);
                long value = ((long) alpha << 24)
                        | ((long) selected.getRed() << 16)
                        | ((long) selected.getGreen() << 8)
                        | selected.getBlue();
                mode.setSelectedItem(MODE_LITERAL);
                argb.setText(new ColorSource.Literal(value).wireArgb());
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), argb);
            }
        }

        private void updateDraft() {
            if (updating) {
                return;
            }
            String selected = Objects.toString(mode.getSelectedItem(), MODE_NOT_SET);
            boolean literal = MODE_LITERAL.equals(selected);
            boolean themed = MODE_THEME.equals(selected);
            argb.setEnabled(literal);
            choose.setEnabled(literal);
            setEnabledRecursively(chooser, literal, null);
            alpha.setEnabled(literal);
            role.setEnabled(themed);
            try {
                FlutterPropertyCellValue candidate;
                if (MODE_NOT_SET.equals(selected)) {
                    candidate = FlutterPropertyCellValue.unset();
                } else if (literal) {
                    PropertyValue.ColorValue color = PropertyValue.ColorValue
                            .fromWireArgb(normalizeArgb(argb.getText()));
                    candidate = FlutterPropertyCellValue.explicit(color);
                } else {
                    ThemeToken token = FlutterThemePropertyRoles.findColorToken(
                            Objects.toString(role.getSelectedItem(), ""),
                            binding.allowedColorSourceTokens())
                            .orElseThrow(() -> new IllegalArgumentException(
                            "Choose a reviewed Material ColorScheme role."));
                    candidate = FlutterPropertyCellValue.explicit(
                            new PropertyValue.ThemeTokenValue(token));
                }
                clearInvalid(argb,
                        "Exact eight-digit Flutter color in 0xAARRGGBB form.");
                clearInvalid(role,
                        "Semantic color that follows the active project theme.");
                markValid(candidate);
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), literal ? argb : role);
            }
        }

        private void argbChanged() {
            if (updating || !MODE_LITERAL.equals(mode.getSelectedItem())) {
                return;
            }
            try {
                PropertyValue.ColorValue color = PropertyValue.ColorValue
                        .fromWireArgb(normalizeArgb(argb.getText()));
                updating = true;
                try {
                    showLiteral(new ColorSource.Literal(color.argb()));
                } finally {
                    updating = false;
                }
            } catch (IllegalArgumentException failure) {
                // updateDraft owns the exact invalid-state presentation.
            }
            updateDraft();
        }

        private void chooserChanged() {
            if (updating || !MODE_LITERAL.equals(mode.getSelectedItem())) {
                return;
            }
            try {
                updateLiteralFromChannels(parseAlpha());
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), alphaText);
            }
        }

        private void alphaChanged() {
            if (updating || !MODE_LITERAL.equals(mode.getSelectedItem())) {
                return;
            }
            updateLiteralFromChannels(((Number) alpha.getValue()).intValue());
        }

        private void alphaTextChanged() {
            if (updating || !MODE_LITERAL.equals(mode.getSelectedItem())) {
                return;
            }
            try {
                updateLiteralFromChannels(parseAlpha());
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), alphaText);
            }
        }

        private int parseAlpha() {
            try {
                int value = Integer.parseInt(alphaText.getText().strip());
                if (value < 0 || value > 255) {
                    throw new IllegalArgumentException(
                            "Alpha must be an integer from 0 through 255.");
                }
                return value;
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException(
                        "Alpha must be an integer from 0 through 255.", failure);
            }
        }

        private void updateLiteralFromChannels(int alphaValue) {
            Color rgb = chooser.getColor();
            long value = ((long) alphaValue << 24)
                    | ((long) rgb.getRed() << 16)
                    | ((long) rgb.getGreen() << 8)
                    | rgb.getBlue();
            updating = true;
            try {
                argb.setText(new ColorSource.Literal(value).wireArgb());
            } finally {
                updating = false;
            }
            clearInvalid(alphaText,
                    "Integer alpha from 0 through 255.");
            updateDraft();
        }

        private void showLiteral(ColorSource.Literal color) {
            chooser.setColor(toAwt(color));
            alpha.setValue((int) ((color.argb() >>> 24) & 0xFF));
        }
    }

    private static final class PaintPanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private final JComboBox<String> colorMode = new JComboBox<>(
                new String[]{MODE_LITERAL, MODE_THEME});
        private final JTextField argb = new JTextField(12);
        private final JComboBox<String> colorRole = new JComboBox<>();
        private final JComboBox<String> blendMode = wireCombo(
                PropertyValue.PaintValue.BlendMode.values(),
                PropertyValue.PaintValue.BlendMode::wireName);
        private final JComboBox<String> style = wireCombo(
                PropertyValue.PaintValue.Style.values(),
                PropertyValue.PaintValue.Style::wireName);
        private final JTextField strokeWidth = new JTextField(10);
        private final JComboBox<String> strokeCap = wireCombo(
                PropertyValue.PaintValue.StrokeCap.values(),
                PropertyValue.PaintValue.StrokeCap::wireName);
        private final JComboBox<String> strokeJoin = wireCombo(
                PropertyValue.PaintValue.StrokeJoin.values(),
                PropertyValue.PaintValue.StrokeJoin::wireName);
        private final JTextField miterLimit = new JTextField(10);
        private final JCheckBox antiAlias = new JCheckBox("Enabled");
        private final JComboBox<String> filterQuality = wireCombo(
                PropertyValue.PaintValue.FilterQuality.values(),
                PropertyValue.PaintValue.FilterQuality::wireName);
        private final JCheckBox invertColors = new JCheckBox("Enabled");
        private final JCheckBox blurEnabled = new JCheckBox("Enabled");
        private final JComboBox<String> blurStyle = wireCombo(
                PropertyValue.PaintValue.BlurStyle.values(),
                PropertyValue.PaintValue.BlurStyle::wireName);
        private final JTextField blurSigma = new JTextField(10);
        private boolean updating;

        PaintPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(620, 500));
            setName("flutter.paint.custom");
            getAccessibleContext().setAccessibleName(
                    binding.definition().name().value() + " structured Paint editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits the safe serializable subset of dart:ui Paint atomically.");
            if (binding.optional()) {
                add(useDefault, BorderLayout.NORTH);
                useDefault.getAccessibleContext().setAccessibleDescription(
                        "Removes the complete Paint constructor argument.");
            }
            FlutterThemePropertyRoles.colorDisplayRoles(
                    binding.allowedColorSourceTokens()).forEach(colorRole::addItem);

            JPanel form = new JPanel(new GridBagLayout());
            style.setName(PAINT_STYLE_NAME);
            strokeWidth.setName(PAINT_STROKE_WIDTH_NAME);
            colorMode.getAccessibleContext().setAccessibleName("Paint color source mode");
            argb.getAccessibleContext().setAccessibleName("Paint ARGB color");
            colorRole.getAccessibleContext().setAccessibleName("Paint theme color role");
            style.getAccessibleContext().setAccessibleName("Painting style");
            strokeWidth.getAccessibleContext().setAccessibleName("Stroke width");
            blurSigma.getAccessibleContext().setAccessibleName("Blur sigma");
            addRow(form, 0, "Color source:", colorMode, null);
            addRow(form, 1, "Literal ARGB:", argb, null);
            addRow(form, 2, "Theme role:", colorRole, null);
            addRow(form, 3, "Blend mode:", blendMode, null);
            addRow(form, 4, "Painting style:", style, null);
            addRow(form, 5, "Stroke width:", strokeWidth, null);
            addRow(form, 6, "Stroke cap:", strokeCap, null);
            addRow(form, 7, "Stroke join:", strokeJoin, null);
            addRow(form, 8, "Miter limit:", miterLimit, null);
            addRow(form, 9, "Anti-alias:", antiAlias, null);
            addRow(form, 10, "Filter quality:", filterQuality, null);
            addRow(form, 11, "Invert colors:", invertColors, null);
            addRow(form, 12, "Blur mask:", blurEnabled, null);
            addRow(form, 13, "Blur style:", blurStyle, null);
            addRow(form, 14, "Blur sigma:", blurSigma, null);
            add(form, BorderLayout.CENTER);

            PropertyValue.PaintValue initial = initialValue().explicitValue()
                    .filter(PropertyValue.PaintValue.class::isInstance)
                    .map(PropertyValue.PaintValue.class::cast)
                    .orElseGet(() -> PropertyValue.PaintValue.defaults(
                    new ColorSource.Literal(0xFF000000L)));
            updating = true;
            try {
                useDefault.setSelected(initialValue().explicitValue().isEmpty());
                showColor(initial.color());
                blendMode.setSelectedItem(initial.blendMode().wireName());
                style.setSelectedItem(initial.style().wireName());
                strokeWidth.setText(initial.strokeWidth().toPlainString());
                strokeCap.setSelectedItem(initial.strokeCap().wireName());
                strokeJoin.setSelectedItem(initial.strokeJoin().wireName());
                miterLimit.setText(initial.strokeMiterLimit().toPlainString());
                antiAlias.setSelected(initial.antiAlias());
                filterQuality.setSelectedItem(initial.filterQuality().wireName());
                invertColors.setSelected(initial.invertColors());
                blurEnabled.setSelected(initial.maskFilter().isPresent());
                PropertyValue.PaintValue.BlurMask blur = initial.maskFilter().orElse(
                        new PropertyValue.PaintValue.BlurMask(
                                PropertyValue.PaintValue.BlurStyle.NORMAL,
                                BigDecimal.ONE));
                blurStyle.setSelectedItem(blur.style().wireName());
                blurSigma.setText(blur.sigma().toPlainString());
            } finally {
                updating = false;
            }

            useDefault.addActionListener(ignored -> updateDraft());
            for (JComboBox<String> combo : List.of(
                    colorMode, colorRole, blendMode, style, strokeCap,
                    strokeJoin, filterQuality, blurStyle)) {
                combo.addActionListener(ignored -> updateDraft());
            }
            for (JCheckBox check : List.of(
                    antiAlias, invertColors, blurEnabled)) {
                check.addActionListener(ignored -> updateDraft());
            }
            for (JTextField field : List.of(
                    argb, strokeWidth, miterLimit, blurSigma)) {
                field.getDocument().addDocumentListener(listener(this::updateDraft));
            }
            updateDraft();
            activate();
        }

        private void showColor(ColorSource color) {
            if (color instanceof ColorSource.Theme theme) {
                colorMode.setSelectedItem(MODE_THEME);
                colorRole.setSelectedItem(
                        FlutterThemePropertyRoles.displayRole(theme.token()));
                argb.setText("0xFF000000");
            } else {
                colorMode.setSelectedItem(MODE_LITERAL);
                argb.setText(((ColorSource.Literal) color).wireArgb());
            }
        }

        private void updateDraft() {
            if (updating) {
                return;
            }
            boolean unset = binding.optional() && useDefault.isSelected();
            for (Component component : getComponents()) {
                setEnabledRecursively(component, !unset, useDefault);
            }
            boolean themed = MODE_THEME.equals(colorMode.getSelectedItem());
            argb.setEnabled(!unset && !themed);
            colorRole.setEnabled(!unset && themed);
            blurStyle.setEnabled(!unset && blurEnabled.isSelected());
            blurSigma.setEnabled(!unset && blurEnabled.isSelected());
            if (unset) {
                markValid(FlutterPropertyCellValue.unset());
                return;
            }
            try {
                ColorSource color = readColor(
                        Objects.toString(colorMode.getSelectedItem(), MODE_LITERAL),
                        argb.getText(), Objects.toString(colorRole.getSelectedItem(), ""),
                        binding.allowedColorSourceTokens());
                Optional<PropertyValue.PaintValue.BlurMask> blur = blurEnabled.isSelected()
                        ? Optional.of(new PropertyValue.PaintValue.BlurMask(
                                PropertyValue.PaintValue.BlurStyle.fromWireName(
                                        selected(blurStyle)), decimal(blurSigma, "Blur sigma")))
                        : Optional.empty();
                PropertyValue.PaintValue value = new PropertyValue.PaintValue(
                        color,
                        PropertyValue.PaintValue.BlendMode.fromWireName(selected(blendMode)),
                        PropertyValue.PaintValue.Style.fromWireName(selected(style)),
                        decimal(strokeWidth, "Stroke width"),
                        PropertyValue.PaintValue.StrokeCap.fromWireName(selected(strokeCap)),
                        PropertyValue.PaintValue.StrokeJoin.fromWireName(selected(strokeJoin)),
                        decimal(miterLimit, "Miter limit"),
                        antiAlias.isSelected(),
                        PropertyValue.PaintValue.FilterQuality.fromWireName(
                                selected(filterQuality)),
                        invertColors.isSelected(), blur);
                clearFormErrors(this);
                markValid(FlutterPropertyCellValue.explicit(value));
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), firstInvalidField(failure.getMessage()));
            }
        }

        private JComponent firstInvalidField(String message) {
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("sigma")) {
                return blurSigma;
            }
            if (normalized.contains("miter")) {
                return miterLimit;
            }
            if (normalized.contains("stroke")) {
                return strokeWidth;
            }
            if (normalized.contains("theme") || normalized.contains("role")) {
                return colorRole;
            }
            return argb;
        }
    }

    private abstract static class OrderedListPanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        final JTable table;
        final DefaultTableModel model;
        final ArrayList<StableId> ids = new ArrayList<>();
        final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        final JButton add = new JButton("Add");
        final JButton remove = new JButton("Remove");
        final JButton up = new JButton("Up");
        final JButton down = new JButton("Down");
        final JPanel toolbarPanel = new JPanel(new BorderLayout());
        private final int maximum;
        private boolean updating;
        private boolean preparingCommit;

        OrderedListPanel(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment,
                String[] columns,
                String componentName,
                String accessibleName,
                String accessibleDescription,
                int maximum) {
            super(editor, binding, environment);
            this.maximum = maximum;
            setLayout(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(680, 370));
            setName(componentName + ".custom");
            getAccessibleContext().setAccessibleName(accessibleName);
            getAccessibleContext().setAccessibleDescription(accessibleDescription);
            model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return true;
                }
            };
            table = new JTable(model);
            table.setName(componentName);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            table.getAccessibleContext().setAccessibleName(accessibleName);
            table.getAccessibleContext().setAccessibleDescription(accessibleDescription);
            add(new JScrollPane(table), BorderLayout.CENTER);

            if (binding.optional()) {
                toolbarPanel.add(useDefault, BorderLayout.NORTH);
                useDefault.getAccessibleContext().setAccessibleDescription(
                        "Removes the complete ordered list constructor argument.");
            }
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
            for (JButton button : List.of(add, remove, up, down)) {
                buttons.add(button);
                button.getAccessibleContext().setAccessibleDescription(
                        button.getText() + " an item in the ordered list.");
            }
            toolbarPanel.add(buttons, BorderLayout.SOUTH);
            add(toolbarPanel, BorderLayout.NORTH);

            model.addTableModelListener(ignored -> {
                if (!preparingCommit) {
                    updateDraft();
                }
            });
            table.getSelectionModel().addListSelectionListener(ignored -> updateButtons());
            useDefault.addActionListener(ignored -> updateDraft());
            add.addActionListener(ignored -> addRow());
            remove.addActionListener(ignored -> removeSelected());
            up.addActionListener(ignored -> move(-1));
            down.addActionListener(ignored -> move(1));
        }

        final void loadRows(List<? extends Object> items) {
            updating = true;
            try {
                loadRowsImpl(items);
                useDefault.setSelected(initialValue().explicitValue().isEmpty());
            } finally {
                updating = false;
            }
            updateDraft();
            activate();
            updateButtons();
        }

        abstract void loadRowsImpl(List<? extends Object> items);

        abstract Object[] newRow();

        abstract PropertyValue buildValue();

        @Override
        boolean prepareCommit() {
            preparingCommit = true;
            try {
                if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
                    markInvalid("Finish or correct the active table cell before applying.",
                            table);
                    return false;
                }
                return refreshDraft(false);
            } finally {
                preparingCommit = false;
            }
        }

        final void append(StableId id, Object... values) {
            ids.add(id);
            model.addRow(values);
        }

        private void addRow() {
            stopEditing();
            if (model.getRowCount() >= maximum) {
                return;
            }
            append(StableId.random(), newRow());
            int row = model.getRowCount() - 1;
            table.getSelectionModel().setSelectionInterval(row, row);
            updateDraft();
        }

        private void removeSelected() {
            stopEditing();
            int row = table.getSelectedRow();
            if (row < 0) {
                return;
            }
            ids.remove(row);
            model.removeRow(row);
            if (model.getRowCount() > 0) {
                int selected = Math.min(row, model.getRowCount() - 1);
                table.getSelectionModel().setSelectionInterval(selected, selected);
            }
            updateDraft();
        }

        private void move(int delta) {
            stopEditing();
            int row = table.getSelectedRow();
            int target = row + delta;
            if (row < 0 || target < 0 || target >= model.getRowCount()) {
                return;
            }
            StableId id = ids.remove(row);
            ids.add(target, id);
            model.moveRow(row, row, target);
            table.getSelectionModel().setSelectionInterval(target, target);
            updateDraft();
        }

        private void stopEditing() {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
        }

        private void updateDraft() {
            refreshDraft(true);
        }

        private boolean refreshDraft(boolean requestValidation) {
            if (updating) {
                return false;
            }
            boolean unset = binding.optional() && useDefault.isSelected();
            table.setEnabled(!unset);
            updateButtons();
            if (unset) {
                clearInvalid(table, table.getAccessibleContext()
                        .getAccessibleDescription());
                storeValid(FlutterPropertyCellValue.unset(), requestValidation);
                return true;
            }
            try {
                PropertyValue value = buildValue();
                clearInvalid(table, table.getAccessibleContext()
                        .getAccessibleDescription());
                storeValid(
                        FlutterPropertyCellValue.explicit(value), requestValidation);
                return true;
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), table);
                return false;
            }
        }

        private void storeValid(
                FlutterPropertyCellValue value,
                boolean requestValidation) {
            if (requestValidation) {
                markValid(value);
            } else {
                stageValid(value);
            }
        }

        private void updateButtons() {
            boolean enabled = !(binding.optional() && useDefault.isSelected());
            int row = table.getSelectedRow();
            add.setEnabled(enabled && model.getRowCount() < maximum);
            remove.setEnabled(enabled && row >= 0);
            up.setEnabled(enabled && row > 0);
            down.setEnabled(enabled && row >= 0 && row < model.getRowCount() - 1);
        }
    }

    private static final class ShadowListPanel extends OrderedListPanel {
        ShadowListPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment,
                    new String[]{"Color", "Offset X", "Offset Y", "Blur radius"},
                    SHADOW_TABLE_NAME, "Ordered text shadows",
                    "Ordered Shadow values. Color accepts 0xAARRGGBB or a reviewed theme role; numeric values use logical pixels.",
                    PropertyValue.ShadowListValue.MAX_ITEMS);
            installColorSourceEditor(table, 0, binding.allowedColorSourceTokens());
            List<PropertyValue.ShadowListValue.Shadow> initial = initialValue()
                    .explicitValue().filter(PropertyValue.ShadowListValue.class::isInstance)
                    .map(PropertyValue.ShadowListValue.class::cast)
                    .map(PropertyValue.ShadowListValue::items).orElse(List.of());
            loadRows(new ArrayList<>(initial));
        }

        @Override
        void loadRowsImpl(List<? extends Object> items) {
            for (Object item : items) {
                PropertyValue.ShadowListValue.Shadow shadow =
                        (PropertyValue.ShadowListValue.Shadow) item;
                append(shadow.id(), colorText(shadow.color()),
                        shadow.offsetX().toPlainString(), shadow.offsetY().toPlainString(),
                        shadow.blurRadius().toPlainString());
            }
        }

        @Override
        Object[] newRow() {
            return new Object[]{"0xFF000000", "0", "0", "0"};
        }

        @Override
        PropertyValue buildValue() {
            ArrayList<PropertyValue.ShadowListValue.Shadow> values = new ArrayList<>();
            for (int row = 0; row < model.getRowCount(); row++) {
                values.add(new PropertyValue.ShadowListValue.Shadow(
                        ids.get(row), parseColor(cell(model, row, 0),
                                binding.allowedColorSourceTokens()),
                        decimal(cell(model, row, 1), "Shadow offset X"),
                        decimal(cell(model, row, 2), "Shadow offset Y"),
                        decimal(cell(model, row, 3), "Shadow blur radius")));
            }
            return new PropertyValue.ShadowListValue(values);
        }
    }

    private record FeaturePreset(String label, String tag, int value) {
        @Override
        public String toString() {
            return label + " (" + tag + ')';
        }
    }

    private static final List<FeaturePreset> FEATURE_PRESETS = List.of(
            new FeaturePreset("Standard ligatures", "liga", 1),
            new FeaturePreset("Kerning", "kern", 1),
            new FeaturePreset("Small capitals", "smcp", 1),
            new FeaturePreset("Tabular figures", "tnum", 1),
            new FeaturePreset("Old-style figures", "onum", 1),
            new FeaturePreset("Slashed zero", "zero", 1));

    private static final class FontFeatureListPanel extends OrderedListPanel {
        private final JComboBox<FeaturePreset> preset = new JComboBox<>(
                FEATURE_PRESETS.toArray(FeaturePreset[]::new));

        FontFeatureListPanel(PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment, new String[]{"OpenType tag", "Value"},
                    FONT_FEATURE_TABLE_NAME, "Ordered OpenType font features",
                    "Each feature uses a unique four-character printable ASCII tag and a non-negative integer value.",
                    PropertyValue.FontFeatureListValue.MAX_ITEMS);
            JComboBox<String> tags = new JComboBox<>(FEATURE_PRESETS.stream()
                    .map(FeaturePreset::tag).toArray(String[]::new));
            tags.setEditable(true);
            table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(tags));
            preset.setName("flutter.fontFeatures.preset");
            preset.getAccessibleContext().setAccessibleName("OpenType feature preset");
            addPresetSelector(preset, "Preset:");
            List<PropertyValue.FontFeatureListValue.FontFeature> initial = initialValue()
                    .explicitValue().filter(PropertyValue.FontFeatureListValue.class::isInstance)
                    .map(PropertyValue.FontFeatureListValue.class::cast)
                    .map(PropertyValue.FontFeatureListValue::items).orElse(List.of());
            loadRows(new ArrayList<>(initial));
        }

        private void addPresetSelector(JComponent component, String label) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 0));
            JLabel caption = new JLabel(label);
            caption.setLabelFor(component);
            row.add(caption);
            row.add(component);
            toolbarPanel.add(row, BorderLayout.EAST);
        }

        @Override
        void loadRowsImpl(List<? extends Object> items) {
            for (Object item : items) {
                PropertyValue.FontFeatureListValue.FontFeature feature =
                        (PropertyValue.FontFeatureListValue.FontFeature) item;
                append(feature.id(), feature.tag(), Integer.toString(feature.value()));
            }
        }

        @Override
        Object[] newRow() {
            FeaturePreset selected = (FeaturePreset) preset.getSelectedItem();
            String tag = uniqueTag(selected == null ? "liga" : selected.tag());
            int value = selected == null ? 1 : selected.value();
            return new Object[]{tag, Integer.toString(value)};
        }

        private String uniqueTag(String preferred) {
            List<String> used = columnValues(model, 0);
            if (!used.contains(preferred)) {
                return preferred;
            }
            for (FeaturePreset candidate : FEATURE_PRESETS) {
                if (!used.contains(candidate.tag())) {
                    return candidate.tag();
                }
            }
            for (int index = 0; index < 1000; index++) {
                String candidate = String.format(Locale.ROOT, "x%03d", index);
                if (!used.contains(candidate)) {
                    return candidate;
                }
            }
            return "xxxx";
        }

        @Override
        PropertyValue buildValue() {
            ArrayList<PropertyValue.FontFeatureListValue.FontFeature> values =
                    new ArrayList<>();
            for (int row = 0; row < model.getRowCount(); row++) {
                int value;
                try {
                    value = Integer.parseInt(cell(model, row, 1));
                } catch (NumberFormatException failure) {
                    throw new IllegalArgumentException(
                            "Font feature value must be a 32-bit integer.", failure);
                }
                values.add(new PropertyValue.FontFeatureListValue.FontFeature(
                        ids.get(row), cell(model, row, 0), value));
            }
            return new PropertyValue.FontFeatureListValue(values);
        }
    }

    private record VariationPreset(String label, String axis, BigDecimal value) {
        @Override
        public String toString() {
            return label + " (" + axis + ')';
        }
    }

    private static final List<VariationPreset> VARIATION_PRESETS = List.of(
            new VariationPreset("Weight", "wght", BigDecimal.valueOf(400)),
            new VariationPreset("Width", "wdth", BigDecimal.valueOf(100)),
            new VariationPreset("Optical size", "opsz", BigDecimal.valueOf(14)),
            new VariationPreset("Slant", "slnt", BigDecimal.ZERO),
            new VariationPreset("Italic", "ital", BigDecimal.ZERO),
            new VariationPreset("Grade", "GRAD", BigDecimal.ZERO));

    private static final class FontVariationListPanel extends OrderedListPanel {
        private final JComboBox<VariationPreset> preset = new JComboBox<>(
                VARIATION_PRESETS.toArray(VariationPreset[]::new));

        FontVariationListPanel(PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment, new String[]{"Axis", "Value"},
                    FONT_VARIATION_TABLE_NAME, "Ordered variable-font axes",
                    "Each variation uses a unique four-character printable ASCII axis and a finite value in its registered range.",
                    PropertyValue.FontVariationListValue.MAX_ITEMS);
            JComboBox<String> axes = new JComboBox<>(VARIATION_PRESETS.stream()
                    .map(VariationPreset::axis).toArray(String[]::new));
            axes.setEditable(true);
            table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(axes));
            preset.setName("flutter.fontVariations.preset");
            preset.getAccessibleContext().setAccessibleName("Variable-font axis preset");
            JPanel row = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 0));
            JLabel caption = new JLabel("Preset:");
            caption.setLabelFor(preset);
            row.add(caption);
            row.add(preset);
            toolbarPanel.add(row, BorderLayout.EAST);
            List<PropertyValue.FontVariationListValue.FontVariation> initial = initialValue()
                    .explicitValue().filter(PropertyValue.FontVariationListValue.class::isInstance)
                    .map(PropertyValue.FontVariationListValue.class::cast)
                    .map(PropertyValue.FontVariationListValue::items).orElse(List.of());
            loadRows(new ArrayList<>(initial));
        }

        @Override
        void loadRowsImpl(List<? extends Object> items) {
            for (Object item : items) {
                PropertyValue.FontVariationListValue.FontVariation variation =
                        (PropertyValue.FontVariationListValue.FontVariation) item;
                append(variation.id(), variation.axis(), variation.value().toPlainString());
            }
        }

        @Override
        Object[] newRow() {
            VariationPreset selected = (VariationPreset) preset.getSelectedItem();
            String axis = uniqueAxis(selected == null ? "wght" : selected.axis());
            BigDecimal value = selected == null ? BigDecimal.valueOf(400) : selected.value();
            return new Object[]{axis, value.toPlainString()};
        }

        private String uniqueAxis(String preferred) {
            List<String> used = columnValues(model, 0);
            if (!used.contains(preferred)) {
                return preferred;
            }
            for (VariationPreset candidate : VARIATION_PRESETS) {
                if (!used.contains(candidate.axis())) {
                    return candidate.axis();
                }
            }
            for (int index = 0; index < 1000; index++) {
                String candidate = String.format(Locale.ROOT, "X%03d", index);
                if (!used.contains(candidate)) {
                    return candidate;
                }
            }
            return "XXXX";
        }

        @Override
        PropertyValue buildValue() {
            ArrayList<PropertyValue.FontVariationListValue.FontVariation> values =
                    new ArrayList<>();
            for (int row = 0; row < model.getRowCount(); row++) {
                values.add(new PropertyValue.FontVariationListValue.FontVariation(
                        ids.get(row), cell(model, row, 0),
                        decimal(cell(model, row, 1), "Font variation value")));
            }
            return new PropertyValue.FontVariationListValue(values);
        }
    }

    private static void installColorSourceEditor(
            JTable table, int column, List<ThemeToken> allowed) {
        JComboBox<String> values = new JComboBox<>();
        values.setEditable(true);
        values.addItem("0xFF000000");
        for (String role : FlutterThemePropertyRoles.colorDisplayRoles(allowed)) {
            values.addItem("Theme: " + role);
        }
        values.getAccessibleContext().setAccessibleName(
                "Literal ARGB or Material ColorScheme role");
        table.getColumnModel().getColumn(column)
                .setCellEditor(new DefaultCellEditor(values));
    }

    private static ColorSource readColor(
            String mode, String argb, String role, List<ThemeToken> allowed) {
        return MODE_THEME.equals(mode)
                ? new ColorSource.Theme(FlutterThemePropertyRoles.findColorToken(
                        role, allowed)
                        .orElseThrow(() -> new IllegalArgumentException(
                        "Choose a reviewed Material ColorScheme role.")))
                : ColorSource.Literal.fromWireArgb(normalizeArgb(argb));
    }

    private static ColorSource parseColor(String text, List<ThemeToken> allowed) {
        String normalized = Objects.requireNonNull(text, "text").strip();
        if (normalized.regionMatches(true, 0, "Theme:", 0, 6)) {
            normalized = normalized.substring(6).strip();
        }
        String candidate = normalized;
        Optional<ThemeToken> token = FlutterThemePropertyRoles.findColorToken(
                candidate, allowed);
        return token.<ColorSource>map(ColorSource.Theme::new)
                .orElseGet(() -> ColorSource.Literal.fromWireArgb(
                normalizeArgb(candidate)));
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
        if (normalized.length() >= 2
                && normalized.substring(0, 2).equalsIgnoreCase("0x")) {
            return "0x" + normalized.substring(2).toUpperCase(Locale.ROOT);
        }
        return normalized;
    }

    private static BigDecimal decimal(JTextField field, String label) {
        try {
            return new BigDecimal(field.getText().strip());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(label + " must be a decimal number.", failure);
        }
    }

    private static BigDecimal decimal(String text, String label) {
        try {
            return new BigDecimal(text.strip());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(label + " must be a decimal number.", failure);
        }
    }

    private static String selected(JComboBox<String> combo) {
        return Objects.toString(combo.getSelectedItem(), "");
    }

    private static String cell(DefaultTableModel model, int row, int column) {
        return Objects.toString(model.getValueAt(row, column), "").strip();
    }

    private static List<String> columnValues(DefaultTableModel model, int column) {
        ArrayList<String> values = new ArrayList<>();
        for (int row = 0; row < model.getRowCount(); row++) {
            values.add(cell(model, row, column));
        }
        return values;
    }

    private static <E> JComboBox<String> wireCombo(
            E[] values, java.util.function.Function<E, String> wireName) {
        return new JComboBox<>(java.util.Arrays.stream(values)
                .map(wireName).toArray(String[]::new));
    }

    private static void addRow(
            JPanel panel, int row, String text, JComponent component, JComponent trailing) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(3, 3, 3, 8);
        JLabel label = new JLabel(text);
        label.setLabelFor(component);
        panel.add(label, labelConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.insets = new Insets(3, 0, 3, 3);
        panel.add(component, valueConstraints);
        if (trailing != null) {
            GridBagConstraints trailingConstraints = new GridBagConstraints();
            trailingConstraints.gridx = 2;
            trailingConstraints.gridy = row;
            trailingConstraints.insets = new Insets(3, 3, 3, 3);
            panel.add(trailing, trailingConstraints);
        }
    }

    private static DocumentListener listener(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        };
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

    private static void clearFormErrors(Component component) {
        if (component instanceof JComponent swing) {
            swing.putClientProperty("JComponent.outline", null);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                clearFormErrors(child);
            }
        }
    }

    private static Color toAwt(ColorSource.Literal literal) {
        return new Color(
                (int) ((literal.argb() >>> 16) & 0xFF),
                (int) ((literal.argb() >>> 8) & 0xFF),
                (int) (literal.argb() & 0xFF),
                (int) ((literal.argb() >>> 24) & 0xFF));
    }
}
