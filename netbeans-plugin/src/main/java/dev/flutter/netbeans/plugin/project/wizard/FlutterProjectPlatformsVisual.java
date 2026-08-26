package dev.flutter.netbeans.plugin.project.wizard;

import dev.flutter.netbeans.project.FlutterProjectPlatform;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.awt.Mnemonics;

final class FlutterProjectPlatformsVisual extends JPanel {
    private final JComboBox<Preset> preset = new JComboBox<>(Preset.values());
    private final Map<FlutterProjectPlatform, JCheckBox> platforms =
            new EnumMap<>(FlutterProjectPlatform.class);
    private final Runnable changeCallback;
    private boolean adjusting;
    private boolean initialized;

    FlutterProjectPlatformsVisual(Runnable changeCallback) {
        this.changeCallback = changeCallback;
        buildUi();
        installListeners();
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 8, 6, 8);
        constraints.anchor = GridBagConstraints.LINE_START;

        JLabel presetLabel = new JLabel();
        Mnemonics.setLocalizedText(presetLabel, Bundle.LBL_PlatformPreset());
        presetLabel.setLabelFor(preset);
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(presetLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(preset, constraints);

        int row = 1;
        for (FlutterProjectPlatform platform : FlutterProjectPlatform.values()) {
            JCheckBox checkBox = new JCheckBox();
            Mnemonics.setLocalizedText(checkBox, label(platform));
            checkBox.getAccessibleContext().setAccessibleName(checkBox.getText());
            checkBox.getAccessibleContext().setAccessibleDescription(
                    Bundle.ACD_PlatformSelection());
            platforms.put(platform, checkBox);
            GridBagConstraints platformConstraints = new GridBagConstraints();
            platformConstraints.insets = new Insets(4, 28, 4, 8);
            platformConstraints.anchor = GridBagConstraints.LINE_START;
            platformConstraints.gridx = 0;
            platformConstraints.gridy = row++;
            platformConstraints.gridwidth = 2;
            add(checkBox, platformConstraints);
        }

        JLabel hint = new JLabel(Bundle.LBL_PlatformSelectionHint());
        GridBagConstraints hintConstraints = new GridBagConstraints();
        hintConstraints.insets = new Insets(12, 8, 6, 8);
        hintConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        hintConstraints.fill = GridBagConstraints.HORIZONTAL;
        hintConstraints.weightx = 1;
        hintConstraints.weighty = 1;
        hintConstraints.gridx = 0;
        hintConstraints.gridy = row;
        hintConstraints.gridwidth = 2;
        add(hint, hintConstraints);

        preset.getAccessibleContext().setAccessibleName(Bundle.ACS_PlatformPreset());
        preset.getAccessibleContext().setAccessibleDescription(Bundle.ACD_PlatformPreset());
        getAccessibleContext().setAccessibleName(
                Bundle.LBL_FlutterProjectPlatformsWizardStep());
        getAccessibleContext().setAccessibleDescription(Bundle.ACD_PlatformSelection());
    }

    private void installListeners() {
        preset.addActionListener(event -> {
            if (adjusting) {
                return;
            }
            Preset selected = (Preset) preset.getSelectedItem();
            if (selected != null && selected != Preset.CUSTOM) {
                applyPreset(selected);
            }
        });
        platforms.values().forEach(checkBox -> checkBox.addActionListener(event -> {
            if (!adjusting) {
                updatePresetFromSelection();
                changeCallback.run();
            }
        }));
    }

    void setSelectedPlatforms(Set<FlutterProjectPlatform> selected) {
        adjusting = true;
        try {
            platforms.forEach((platform, checkBox) ->
                    checkBox.setSelected(selected.contains(platform)));
            preset.setSelectedItem(matchingPreset(selected));
            initialized = true;
        } finally {
            adjusting = false;
        }
        changeCallback.run();
    }

    Set<FlutterProjectPlatform> selectedPlatforms() {
        EnumSet<FlutterProjectPlatform> selected =
                EnumSet.noneOf(FlutterProjectPlatform.class);
        platforms.forEach((platform, checkBox) -> {
            if (checkBox.isSelected()) {
                selected.add(platform);
            }
        });
        return Collections.unmodifiableSet(selected);
    }

    boolean isInitialized() {
        return initialized;
    }

    void selectPreset(Preset selected) {
        preset.setSelectedItem(selected);
    }

    void setPlatformSelected(FlutterProjectPlatform platform, boolean selected) {
        JCheckBox checkBox = platforms.get(platform);
        if (checkBox.isSelected() != selected) {
            checkBox.doClick();
        }
    }

    Preset selectedPreset() {
        return (Preset) preset.getSelectedItem();
    }

    private void applyPreset(Preset selected) {
        setSelectedPlatforms(selected.platforms());
    }

    private void updatePresetFromSelection() {
        adjusting = true;
        try {
            preset.setSelectedItem(matchingPreset(selectedPlatforms()));
        } finally {
            adjusting = false;
        }
    }

    private static Preset matchingPreset(Set<FlutterProjectPlatform> selected) {
        for (Preset candidate : Preset.values()) {
            if (candidate != Preset.CUSTOM && candidate.platforms().equals(selected)) {
                return candidate;
            }
        }
        return Preset.CUSTOM;
    }

    private static String label(FlutterProjectPlatform platform) {
        return switch (platform) {
            case ANDROID -> Bundle.LBL_PlatformAndroid();
            case IOS -> Bundle.LBL_PlatformIos();
            case WEB -> Bundle.LBL_PlatformWeb();
            case WINDOWS -> Bundle.LBL_PlatformWindows();
            case MACOS -> Bundle.LBL_PlatformMacos();
            case LINUX -> Bundle.LBL_PlatformLinux();
        };
    }

    enum Preset {
        RECOMMENDED,
        MOBILE,
        DESKTOP,
        WEB,
        ALL,
        CUSTOM;

        Set<FlutterProjectPlatform> platforms() {
            return switch (this) {
                case RECOMMENDED -> recommendedPlatforms();
                case MOBILE -> Collections.unmodifiableSet(EnumSet.of(
                        FlutterProjectPlatform.ANDROID,
                        FlutterProjectPlatform.IOS));
                case DESKTOP -> Collections.unmodifiableSet(EnumSet.of(
                        FlutterProjectPlatform.WINDOWS,
                        FlutterProjectPlatform.MACOS,
                        FlutterProjectPlatform.LINUX));
                case WEB -> Set.of(FlutterProjectPlatform.WEB);
                case ALL -> FlutterProjectPlatform.all();
                case CUSTOM -> Set.of();
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case RECOMMENDED -> Bundle.LBL_PlatformPresetRecommended();
                case MOBILE -> Bundle.LBL_PlatformPresetMobile();
                case DESKTOP -> Bundle.LBL_PlatformPresetDesktop();
                case WEB -> Bundle.LBL_PlatformPresetWeb();
                case ALL -> Bundle.LBL_PlatformPresetAll();
                case CUSTOM -> Bundle.LBL_PlatformPresetCustom();
            };
        }

        private static Set<FlutterProjectPlatform> recommendedPlatforms() {
            EnumSet<FlutterProjectPlatform> selected = EnumSet.of(
                    FlutterProjectPlatform.ANDROID,
                    FlutterProjectPlatform.WEB);
            String operatingSystem = System.getProperty("os.name", "")
                    .toLowerCase(java.util.Locale.ROOT);
            if (operatingSystem.startsWith("windows")) {
                selected.add(FlutterProjectPlatform.WINDOWS);
            } else if (operatingSystem.contains("mac")
                    || operatingSystem.contains("darwin")) {
                selected.add(FlutterProjectPlatform.IOS);
                selected.add(FlutterProjectPlatform.MACOS);
            } else if (operatingSystem.contains("linux")) {
                selected.add(FlutterProjectPlatform.LINUX);
            }
            return Collections.unmodifiableSet(selected);
        }
    }
}
