package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Shared typed editor for the asset-only {@link PropertyValue.ImageProviderValue}
 * contract used by both Image.image and Container's DecorationImage.image.
 */
final class FlutterImageProviderEditorComponent extends JPanel {
    static final String DIRECT_PREFIX = "flutter.image.imageProvider";

    private final FlutterImageAssetChoices assetChoices;
    private final String target;
    private final Runnable changed;
    private final JComboBox<FlutterImageAssetChoices.Choice> asset =
            new JComboBox<>();
    private final JComboBox<PropertyValue.ImageProviderValue.ProviderKind> provider =
            new JComboBox<>(PropertyValue.ImageProviderValue.ProviderKind.values());
    private final JTextField exactScale = new JTextField("1", 8);
    private final JCheckBox resizeEnabled = new JCheckBox("Wrap with ResizeImage");
    private final JTextField resizeWidth = new JTextField(8);
    private final JTextField resizeHeight = new JTextField(8);
    private final JComboBox<PropertyValue.ImageProviderValue.ResizePolicy> resizePolicy =
            new JComboBox<>(PropertyValue.ImageProviderValue.ResizePolicy.values());
    private final JCheckBox allowUpscaling = new JCheckBox("Allow upscaling");
    private final JLabel inventoryStatus = new JLabel();
    private boolean updating;

    FlutterImageProviderEditorComponent(
            FlutterImageAssetChoices assetChoices,
            String target,
            String componentPrefix,
            Runnable changed) {
        this.assetChoices = Objects.requireNonNull(assetChoices, "assetChoices");
        this.target = requireText(target, "target");
        String prefix = requireText(componentPrefix, "componentPrefix");
        this.changed = Objects.requireNonNull(changed, "changed");

        setLayout(new GridBagLayout());
        setName(prefix + ".editor");
        getAccessibleContext().setAccessibleName(
                "Flutter asset image provider editor");
        getAccessibleContext().setAccessibleDescription(
                "Edits " + this.target + " using a declared app or package asset, "
                + "an AssetImage or ExactAssetImage provider, and an optional "
                + "bounded ResizeImage decode request.");

        asset.setName(prefix + ".asset");
        provider.setName(prefix + ".provider");
        exactScale.setName(prefix + ".exactScale");
        resizeEnabled.setName(prefix + ".resize.enabled");
        resizeWidth.setName(prefix + ".resize.width");
        resizeHeight.setName(prefix + ".resize.height");
        resizePolicy.setName(prefix + ".resize.policy");
        allowUpscaling.setName(prefix + ".resize.allowUpscaling");
        inventoryStatus.setName(prefix + ".assetStatus");

        asset.getAccessibleContext().setAccessibleName(
                "Declared Flutter image asset");
        asset.getAccessibleContext().setAccessibleDescription(
                "Chooses a concrete image declared by the application or a "
                + "resolved Dart package; arbitrary paths are not accepted.");
        provider.getAccessibleContext().setAccessibleName(
                "Flutter image provider kind");
        provider.getAccessibleContext().setAccessibleDescription(
                "Uses DPR-aware AssetImage or exact-path ExactAssetImage for "
                + this.target + '.');
        exactScale.getAccessibleContext().setAccessibleName(
                "ExactAssetImage scale");
        exactScale.getAccessibleContext().setAccessibleDescription(
                "Positive logical scale used only by ExactAssetImage.");
        resizeEnabled.getAccessibleContext().setAccessibleDescription(
                "Adds one typed ResizeImage wrapper around the selected asset provider.");
        resizeWidth.getAccessibleContext().setAccessibleName(
                "ResizeImage cache width");
        resizeWidth.getAccessibleContext().setAccessibleDescription(
                "Optional positive decoded width in pixels, at most 16384.");
        resizeHeight.getAccessibleContext().setAccessibleName(
                "ResizeImage cache height");
        resizeHeight.getAccessibleContext().setAccessibleDescription(
                "Optional positive decoded height in pixels, at most 16384.");
        resizePolicy.getAccessibleContext().setAccessibleName(
                "ResizeImage policy");
        allowUpscaling.getAccessibleContext().setAccessibleDescription(
                "Allows ResizeImage to decode larger than the source dimensions.");
        inventoryStatus.getAccessibleContext().setAccessibleName(
                "Declared image asset inventory status");

        displayWith(provider, value -> switch (value) {
            case ASSET -> "AssetImage (DPR-aware)";
            case EXACT_ASSET -> "ExactAssetImage";
        });
        displayWith(resizePolicy, value -> switch (value) {
            case EXACT -> "Exact dimensions";
            case FIT -> "Fit within dimensions";
        });
        assetChoices.choices().forEach(asset::addItem);

        int row = 0;
        addRow(row++, "Declared asset:", asset);
        addRow(row++, "Provider:", flow(provider, exactScale));
        addWideRow(row++, resizeEnabled);
        addRow(row++, "Resize width / height:",
                flow(resizeWidth, new JLabel("×"), resizeHeight));
        addRow(row++, "Resize policy:", flow(resizePolicy, allowUpscaling));
        addWideRow(row, inventoryStatus);

        installListeners();
        updateInventoryStatus();
        updateEnabledState(true);
    }

    void populate(PropertyValue.ImageProviderValue value) {
        Objects.requireNonNull(value, "value");
        updating = true;
        try {
            if (value.isUnresolved()) {
                asset.setSelectedItem(null);
            } else {
                FlutterImageAssetChoices.Choice choice = assetChoices.find(
                        value.packageName(), value.assetName())
                        .orElseGet(() -> storedChoice(value));
                boolean present = false;
                for (int index = 0; index < asset.getItemCount(); index++) {
                    if (asset.getItemAt(index).equals(choice)) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    asset.addItem(choice);
                }
                asset.setSelectedItem(choice);
            }
            provider.setSelectedItem(value.providerKind());
            exactScale.setText(value.exactScale()
                    .map(BigDecimal::toPlainString).orElse("1"));
            resizeEnabled.setSelected(value.resize().isPresent());
            value.resize().ifPresentOrElse(resize -> {
                resizeWidth.setText(resize.width().map(Object::toString).orElse(""));
                resizeHeight.setText(resize.height().map(Object::toString).orElse(""));
                resizePolicy.setSelectedItem(resize.policy());
                allowUpscaling.setSelected(resize.allowUpscaling());
            }, () -> {
                resizeWidth.setText("");
                resizeHeight.setText("");
                resizePolicy.setSelectedItem(
                        PropertyValue.ImageProviderValue.ResizePolicy.EXACT);
                allowUpscaling.setSelected(false);
            });
        } finally {
            updating = false;
        }
        updateInventoryStatus();
        updateEnabledState(isEnabled());
    }

    void selectFirstDeclaredAsset() {
        if (assetChoices.choices().isEmpty()) {
            return;
        }
        updating = true;
        try {
            asset.setSelectedItem(assetChoices.choices().getFirst());
            provider.setSelectedItem(
                    PropertyValue.ImageProviderValue.ProviderKind.ASSET);
            exactScale.setText("1");
            resizeEnabled.setSelected(false);
            resizeWidth.setText("");
            resizeHeight.setText("");
            resizePolicy.setSelectedItem(
                    PropertyValue.ImageProviderValue.ResizePolicy.EXACT);
            allowUpscaling.setSelected(false);
        } finally {
            updating = false;
        }
        updateInventoryStatus();
        updateEnabledState(isEnabled());
    }

    PropertyValue.ImageProviderValue value() {
        FlutterImageAssetChoices.Choice choice =
                (FlutterImageAssetChoices.Choice) asset.getSelectedItem();
        if (choice == null) {
            throw new IllegalArgumentException(
                    "Choose a declared Flutter image asset. Target: " + target
                    + ". Reason: " + unavailableReason());
        }
        PropertyValue.ImageProviderValue.ProviderKind providerKind =
                (PropertyValue.ImageProviderValue.ProviderKind)
                provider.getSelectedItem();
        Optional<BigDecimal> selectedExactScale = providerKind
                == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET
                ? Optional.of(decimal(exactScale.getText(), "Exact asset scale"))
                : Optional.empty();
        Optional<PropertyValue.ImageProviderValue.ResizeImageConfig> resize =
                resizeEnabled.isSelected()
                ? Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(
                        optionalInteger(resizeWidth.getText(), "Resize width"),
                        optionalInteger(resizeHeight.getText(), "Resize height"),
                        (PropertyValue.ImageProviderValue.ResizePolicy)
                        resizePolicy.getSelectedItem(),
                        allowUpscaling.isSelected()))
                : Optional.empty();
        return new PropertyValue.ImageProviderValue(
                providerKind,
                choice.assetName(),
                choice.packageName(),
                selectedExactScale,
                resize);
    }

    void updateEnabledState(boolean enabled) {
        super.setEnabled(enabled);
        boolean hasAsset = asset.getSelectedItem() != null;
        asset.setEnabled(enabled && asset.getItemCount() > 0);
        provider.setEnabled(enabled && hasAsset);
        exactScale.setEnabled(enabled && hasAsset
                && provider.getSelectedItem()
                == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET);
        resizeEnabled.setEnabled(enabled && hasAsset);
        boolean resize = enabled && hasAsset && resizeEnabled.isSelected();
        resizeWidth.setEnabled(resize);
        resizeHeight.setEnabled(resize);
        resizePolicy.setEnabled(resize);
        allowUpscaling.setEnabled(resize);
        inventoryStatus.setEnabled(enabled);
    }

    private void installListeners() {
        asset.addActionListener(ignored -> fireChanged());
        provider.addActionListener(ignored -> fireChanged());
        resizeEnabled.addActionListener(ignored -> fireChanged());
        resizePolicy.addActionListener(ignored -> fireChanged());
        allowUpscaling.addActionListener(ignored -> fireChanged());
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                fireChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                fireChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                fireChanged();
            }
        };
        exactScale.getDocument().addDocumentListener(listener);
        resizeWidth.getDocument().addDocumentListener(listener);
        resizeHeight.getDocument().addDocumentListener(listener);
    }

    private void fireChanged() {
        if (updating) {
            return;
        }
        updateInventoryStatus();
        updateEnabledState(isEnabled());
        changed.run();
    }

    private void updateInventoryStatus() {
        String status = assetChoices.choices().isEmpty()
                ? "Asset selection unavailable for " + target + ": "
                + unavailableReason()
                : asset.getSelectedItem() == null
                ? "Choose one of " + assetChoices.choices().size()
                + " declared image asset choice(s) for " + target + '.'
                : assetChoices.choices().size()
                + " declared image asset choice(s) are available for " + target + '.';
        inventoryStatus.setText(status);
        inventoryStatus.getAccessibleContext().setAccessibleDescription(status);
    }

    private String unavailableReason() {
        return assetChoices.unavailableReason().orElse(
                "no declared image asset is available.");
    }

    private static FlutterImageAssetChoices.Choice storedChoice(
            PropertyValue.ImageProviderValue value) {
        String display = value.packageName()
                .map(name -> "Package " + name + ": " + value.assetName()
                        + " (stored; unavailable)")
                .orElseGet(() -> "App: " + value.assetName()
                        + " (stored; unavailable)");
        return new FlutterImageAssetChoices.Choice(
                value.packageName(), value.assetName(), display);
    }

    private void addRow(int row, String labelText, Component editor) {
        GridBagConstraints labelConstraints = constraints(0, row);
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        JLabel label = new JLabel(labelText);
        label.setLabelFor(editor);
        add(label, labelConstraints);
        GridBagConstraints editorConstraints = constraints(1, row);
        editorConstraints.weightx = 1;
        editorConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(editor, editorConstraints);
    }

    private void addWideRow(int row, Component component) {
        GridBagConstraints constraints = constraints(0, row);
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(component, constraints);
    }

    private static GridBagConstraints constraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.anchor = GridBagConstraints.LINE_START;
        return constraints;
    }

    private static JPanel flow(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        for (Component component : components) {
            panel.add(Objects.requireNonNull(component, "components contains null"));
        }
        return panel;
    }

    private static Optional<Integer> optionalInteger(String text, String label) {
        String normalized = Objects.requireNonNull(text, "text").strip();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(normalized));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    label + " must be a whole number.", failure);
        }
    }

    private static BigDecimal decimal(String text, String label) {
        try {
            return new BigDecimal(Objects.requireNonNull(text, "text").strip());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    label + " must be a finite decimal.", failure);
        }
    }

    private static <T> void displayWith(
            JComboBox<T> combo,
            java.util.function.Function<T, String> display) {
        ListCellRenderer<? super T> renderer = combo.getRenderer();
        combo.setRenderer((JList<? extends T> list, T value, int index,
                boolean selected, boolean focus) -> {
            Component component = renderer instanceof DefaultListCellRenderer defaultRenderer
                    ? defaultRenderer.getListCellRendererComponent(
                            list, value, index, selected, focus)
                    : new JLabel();
            if (component instanceof JLabel label && value != null) {
                label.setText(display.apply(value));
            }
            return component;
        });
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String compact = value.strip().replaceAll("\\s+", " ");
        if (compact.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return compact;
    }
}
