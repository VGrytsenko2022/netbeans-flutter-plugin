package dev.flutter.netbeans.plugin.theme;

import dev.flutter.netbeans.project.theme.FlutterGeneratedThemeArtifact;
import dev.flutter.netbeans.project.theme.FlutterMaterialColorRole;
import dev.flutter.netbeans.project.theme.FlutterMaterialTextStyleRole;
import dev.flutter.netbeans.project.theme.FlutterProjectTheme;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeDefinition;
import dev.flutter.netbeans.project.theme.FlutterThemeBrightness;
import dev.flutter.netbeans.project.theme.FlutterThemeColorValue;
import dev.flutter.netbeans.project.theme.FlutterThemeComponentColorRole;
import dev.flutter.netbeans.project.theme.FlutterThemeFontStyle;
import dev.flutter.netbeans.project.theme.FlutterThemeFontWeight;
import dev.flutter.netbeans.project.theme.FlutterThemeMode;
import dev.flutter.netbeans.project.theme.FlutterThemeTextDecorationLine;
import dev.flutter.netbeans.project.theme.FlutterThemeTextDecorationStyle;
import dev.flutter.netbeans.project.theme.FlutterThemeTextStyleOverride;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.util.NbBundle.Messages;

/** Accessible Swing editor for one canonical project-wide theme descriptor. */
@Messages({
    "LBL_ThemeApplication=Application theme",
    "LBL_ThemeEnabled=Enable project themes",
    "LBL_ThemeDefaultMode=Default mode:",
    "LBL_ThemeLightReference=Light theme:",
    "LBL_ThemeDarkReference=Dark theme:",
    "LBL_ThemeGeneratedNotice=Generated Dart: lib/theme/app_theme.dart",
    "HINT_ThemeGeneratedNotice=Generated from .fd_templates/project.fdtheme. Disabling project themes keeps the catalog and uses Flutter defaults. Manual Dart changes are protected from overwrite.",
    "LBL_ThemeCatalog=Themes",
    "LBL_ThemeSelected=Selected theme",
    "LBL_ThemeItemEnabled=Enabled",
    "LBL_ThemeId=ID:",
    "LBL_ThemeDisplayName=Display name:",
    "LBL_ThemeBrightness=Brightness:",
    "LBL_ThemeSeedColor=Seed color:",
    "CTL_ThemeAddCustom=New",
    "CTL_ThemeDuplicate=Duplicate",
    "CTL_ThemeRemoveCustom=Delete",
    "TTL_ThemeChooseSeedColor=Choose Flutter theme seed color",
    "LBL_ThemeModeSystem=Follow system",
    "LBL_ThemeModeLight=Always light",
    "LBL_ThemeModeDark=Always dark",
    "LBL_ThemeBrightnessLight=Light",
    "LBL_ThemeBrightnessDark=Dark",
    "LBL_ThemeOff=Off",
    "TAB_ThemeGeneral=General",
    "TAB_ThemeColors=Colors",
    "TAB_ThemeTypography=Typography",
    "TAB_ThemeComponents=Components",
    "LBL_ThemeComponentColors=Component colors (36)",
    "LBL_ThemeColorRoles=ColorScheme roles",
    "LBL_ThemeColorInherited=Inherited from the seed-generated ColorScheme",
    "# {0} - exact ARGB color",
    "LBL_ThemeColorOverride=Override {0}",
    "CTL_ThemeChooseOverride=Choose override...",
    "CTL_ThemeUseDefault=Use default",
    "TTL_ThemeChooseOverrideColor=Choose theme color override",
    "LBL_ThemeTypographyRoles=TextTheme roles",
    "LBL_ThemeTypographyInherited=Inherited Material TextTheme style",
    "# {0} - explicit field count",
    "LBL_ThemeTypographyOverride=Override: {0} field(s)",
    "CTL_ThemeResetTypographyRole=Reset role",
    "LBL_ThemeTextColor=Text color",
    "LBL_ThemeBackgroundColor=Background color",
    "LBL_ThemeFontSize=Font size",
    "LBL_ThemeFontWeight=Font weight",
    "LBL_ThemeFontStyle=Font style",
    "LBL_ThemeLetterSpacing=Letter spacing",
    "LBL_ThemeWordSpacing=Word spacing",
    "LBL_ThemeLineHeight=Line height",
    "LBL_ThemeFontFamily=Font family",
    "LBL_ThemeDecoration=Decoration lines",
    "LBL_ThemeDecorationColor=Decoration color",
    "LBL_ThemeDecorationStyle=Decoration style",
    "LBL_ThemeDecorationThickness=Decoration thickness",
    "LBL_ThemeInherit=Inherit",
    "LBL_ThemeColorRole=ColorScheme role",
    "LBL_ThemeColorLiteral=ARGB literal",
    "# {0} - typography field name",
    "LBL_ThemeOverrideField=Override {0}",
    "LBL_ThemeUnderline=Underline",
    "LBL_ThemeOverline=Overline",
    "LBL_ThemeLineThrough=Line through"
})
final class FlutterThemeEditorPanel extends JPanel implements Scrollable {
    static final String PROP_VALID = "themeEditorValid";
    static final String PROP_VALIDATION_MESSAGE = "themeEditorValidationMessage";
    static final String PROP_CHANGED = "themeEditorChanged";

    private final FlutterThemeEditorDraft draft;
    private final FlutterGeneratedThemeArtifact generated;
    private final ColorChooser colorChooser;
    private final JTabbedPane tabs = new JTabbedPane();
    private final ColorOverridesEditor colorOverridesEditor;
    private final TypographyOverridesEditor typographyOverridesEditor;
    private final ComponentColorsEditor componentColorsEditor;
    private final DefaultListModel<FlutterThemeEditorDraft.ThemeRow> listModel =
            new DefaultListModel<>();
    private final JList<FlutterThemeEditorDraft.ThemeRow> themeList =
            new JList<>(listModel);
    private final JCheckBox themesEnabled =
            new JCheckBox(Bundle.LBL_ThemeEnabled());
    private final JComboBox<FlutterThemeMode> defaultMode =
            new JComboBox<>(FlutterThemeMode.values());
    private final JComboBox<FlutterThemeEditorDraft.ThemeRow> lightTheme =
            new JComboBox<>();
    private final JComboBox<FlutterThemeEditorDraft.ThemeRow> darkTheme =
            new JComboBox<>();
    private final JTextField id = new JTextField(12);
    private final JTextField displayName = new JTextField(12);
    private final JCheckBox selectedThemeEnabled =
            new JCheckBox(Bundle.LBL_ThemeItemEnabled());
    private final JComboBox<FlutterThemeBrightness> brightness =
            new JComboBox<>(FlutterThemeBrightness.values());
    private final JButton seedColor = new JButton();
    private final JButton add = new JButton(Bundle.CTL_ThemeAddCustom());
    private final JButton duplicate = new JButton(Bundle.CTL_ThemeDuplicate());
    private final JButton remove = new JButton(Bundle.CTL_ThemeRemoveCustom());
    private final JTextArea status = new JTextArea(" ", 2, 12);
    private final JLabel generatedNotice = new JLabel(Bundle.LBL_ThemeGeneratedNotice());

    private FlutterThemeEditorDraft.ThemeRow selected;
    private int selectedSeedArgb;
    private boolean updating;
    private boolean valid = true;
    private long changeRevision;
    private int activeTab;

    FlutterThemeEditorPanel(FlutterProjectTheme source) {
        this(source, (parent, initial) -> JColorChooser.showDialog(
                parent, Bundle.TTL_ThemeChooseSeedColor(), initial));
    }

    FlutterThemeEditorPanel(FlutterProjectTheme source, ColorChooser colorChooser) {
        super(new BorderLayout(8, 8));
        Objects.requireNonNull(source, "source");
        draft = new FlutterThemeEditorDraft(source);
        generated = source.generated();
        this.colorChooser = Objects.requireNonNull(colorChooser, "colorChooser");
        colorOverridesEditor = new ColorOverridesEditor();
        typographyOverridesEditor = new TypographyOverridesEditor();
        componentColorsEditor = new ComponentColorsEditor();
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        getAccessibleContext().setAccessibleName("Flutter project themes editor");
        getAccessibleContext().setAccessibleDescription(
                "Edits application theme mode, light and dark references, "
                + "and project-wide Flutter theme definitions.");

        JPanel general = new JPanel(new BorderLayout(8, 8));
        general.add(applicationPanel(), BorderLayout.NORTH);
        general.add(themeEditor(), BorderLayout.CENTER);
        tabs.addTab(Bundle.TAB_ThemeGeneral(), general);
        tabs.addTab(Bundle.TAB_ThemeColors(), colorOverridesEditor);
        tabs.addTab(Bundle.TAB_ThemeTypography(), typographyOverridesEditor);
        tabs.addTab(Bundle.TAB_ThemeComponents(), componentColorsEditor);
        add(tabs, BorderLayout.CENTER);
        configureAccessibility();
        configureListeners();
        reloadList(null);
        themesEnabled.setSelected(draft.enabled());
        defaultMode.setSelectedItem(draft.defaultMode());
        refreshReferenceModels();
        themeList.setSelectedIndex(0);
        updateApplicationControls();
    }

    FlutterProjectTheme buildTheme() {
        if (!commitSelected()) {
            throw new IllegalArgumentException(status.getText());
        }
        return draft.build(generated);
    }

    boolean isEditorValid() {
        return valid;
    }

    String validationMessage() {
        return valid ? "" : status.getText();
    }

    long changeRevision() {
        return changeRevision;
    }

    FlutterThemeEditorDraft draftForTest() {
        return draft;
    }

    JList<FlutterThemeEditorDraft.ThemeRow> themeListForTest() {
        return themeList;
    }

    JCheckBox enabledCheckBoxForTest() {
        return themesEnabled;
    }

    JComboBox<FlutterThemeMode> defaultModeForTest() {
        return defaultMode;
    }

    JComboBox<FlutterThemeEditorDraft.ThemeRow> lightThemeForTest() {
        return lightTheme;
    }

    JComboBox<FlutterThemeEditorDraft.ThemeRow> darkThemeForTest() {
        return darkTheme;
    }

    JButton addButtonForTest() {
        return add;
    }

    JButton duplicateButtonForTest() {
        return duplicate;
    }

    JButton removeButtonForTest() {
        return remove;
    }

    JCheckBox selectedThemeEnabledForTest() {
        return selectedThemeEnabled;
    }

    JTextField idFieldForTest() {
        return id;
    }

    JTextField displayNameFieldForTest() {
        return displayName;
    }

    JComboBox<FlutterThemeBrightness> brightnessForTest() {
        return brightness;
    }

    JButton seedColorButtonForTest() {
        return seedColor;
    }

    JLabel generatedNoticeForTest() {
        return generatedNotice;
    }

    JTabbedPane tabsForTest() {
        return tabs;
    }

    JList<FlutterMaterialColorRole> colorRoleListForTest() {
        return colorOverridesEditor.roles;
    }

    JButton colorOverrideButtonForTest() {
        return colorOverridesEditor.choose;
    }

    JButton colorResetButtonForTest() {
        return colorOverridesEditor.reset;
    }

    JLabel colorStateForTest() {
        return colorOverridesEditor.state;
    }

    JList<FlutterMaterialTextStyleRole> typographyRoleListForTest() {
        return typographyOverridesEditor.roles;
    }

    JButton typographyResetRoleButtonForTest() {
        return typographyOverridesEditor.resetRole;
    }

    OptionalColorControl typographyColorForTest() {
        return typographyOverridesEditor.color;
    }

    OptionalColorControl typographyBackgroundColorForTest() {
        return typographyOverridesEditor.backgroundColor;
    }

    OptionalDoubleControl typographyFontSizeForTest() {
        return typographyOverridesEditor.fontSize;
    }

    OptionalDoubleControl typographyLetterSpacingForTest() {
        return typographyOverridesEditor.letterSpacing;
    }

    OptionalDoubleControl typographyWordSpacingForTest() {
        return typographyOverridesEditor.wordSpacing;
    }

    OptionalDoubleControl typographyHeightForTest() {
        return typographyOverridesEditor.height;
    }

    OptionalStringControl typographyFontFamilyForTest() {
        return typographyOverridesEditor.fontFamily;
    }

    OptionalEnumControl<FlutterThemeFontWeight> typographyFontWeightForTest() {
        return typographyOverridesEditor.fontWeight;
    }

    OptionalEnumControl<FlutterThemeFontStyle> typographyFontStyleForTest() {
        return typographyOverridesEditor.fontStyle;
    }

    DecorationControl typographyDecorationForTest() {
        return typographyOverridesEditor.decoration;
    }

    OptionalColorControl typographyDecorationColorForTest() {
        return typographyOverridesEditor.decorationColor;
    }

    OptionalEnumControl<FlutterThemeTextDecorationStyle>
            typographyDecorationStyleForTest() {
        return typographyOverridesEditor.decorationStyle;
    }

    OptionalDoubleControl typographyDecorationThicknessForTest() {
        return typographyOverridesEditor.decorationThickness;
    }

    JList<FlutterThemeComponentColorRole> componentColorRoleListForTest() {
        return componentColorsEditor.roles;
    }

    OptionalColorControl componentColorControlForTest() {
        return componentColorsEditor.color;
    }

    String statusTextForTest() {
        return status.getText();
    }

    private JPanel applicationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(Bundle.LBL_ThemeApplication()));
        GridBagConstraints constraints = baseConstraints();

        GridBagConstraints enabledConstraints =
                (GridBagConstraints) constraints.clone();
        enabledConstraints.gridx = 0;
        enabledConstraints.gridy = 0;
        enabledConstraints.gridwidth = 2;
        enabledConstraints.weightx = 1;
        enabledConstraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(themesEnabled, enabledConstraints);
        addRow(panel, constraints, 1, Bundle.LBL_ThemeDefaultMode(), defaultMode);
        addRow(panel, constraints, 2, Bundle.LBL_ThemeLightReference(), lightTheme);
        addRow(panel, constraints, 3, Bundle.LBL_ThemeDarkReference(), darkTheme);
        GridBagConstraints noticeConstraints = (GridBagConstraints) constraints.clone();
        noticeConstraints.gridx = 0;
        noticeConstraints.gridy = 4;
        noticeConstraints.gridwidth = 2;
        noticeConstraints.weightx = 1;
        noticeConstraints.fill = GridBagConstraints.HORIZONTAL;
        noticeConstraints.insets = new Insets(8, 4, 3, 4);
        panel.add(generatedNotice, noticeConstraints);
        return panel;
    }

    private JPanel themeEditor() {
        JPanel catalog = new JPanel(new BorderLayout(4, 4));
        catalog.setBorder(BorderFactory.createTitledBorder(Bundle.LBL_ThemeCatalog()));
        themeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        themeList.setVisibleRowCount(3);
        themeList.setCellRenderer(new ThemeRenderer());
        JScrollPane catalogScroll = new JScrollPane(themeList);
        catalogScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        catalog.add(catalogScroll, BorderLayout.CENTER);

        JPanel catalogButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        catalogButtons.add(add);
        catalogButtons.add(duplicate);
        catalogButtons.add(remove);
        catalog.add(catalogButtons, BorderLayout.NORTH);

        JPanel properties = new JPanel(new BorderLayout(4, 4));
        properties.setBorder(BorderFactory.createTitledBorder(Bundle.LBL_ThemeSelected()));
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = baseConstraints();
        GridBagConstraints enabledConstraints =
                (GridBagConstraints) constraints.clone();
        enabledConstraints.gridx = 0;
        enabledConstraints.gridy = 0;
        enabledConstraints.gridwidth = 2;
        enabledConstraints.weightx = 1;
        enabledConstraints.fill = GridBagConstraints.HORIZONTAL;
        fields.add(selectedThemeEnabled, enabledConstraints);
        addRow(fields, constraints, 1, Bundle.LBL_ThemeId(), id);
        addRow(fields, constraints, 2, Bundle.LBL_ThemeDisplayName(), displayName);
        addRow(fields, constraints, 3, Bundle.LBL_ThemeBrightness(), brightness);
        addRow(fields, constraints, 4, Bundle.LBL_ThemeSeedColor(), seedColor);
        properties.add(fields, BorderLayout.NORTH);
        status.setEditable(false);
        status.setFocusable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setOpaque(false);
        status.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        properties.add(status, BorderLayout.SOUTH);

        JPanel editor = new JPanel(new GridBagLayout());
        GridBagConstraints catalogConstraints = new GridBagConstraints();
        catalogConstraints.gridx = 0;
        catalogConstraints.gridy = 0;
        catalogConstraints.weightx = 1;
        catalogConstraints.fill = GridBagConstraints.HORIZONTAL;
        catalogConstraints.anchor = GridBagConstraints.PAGE_START;
        editor.add(catalog, catalogConstraints);

        GridBagConstraints propertiesConstraints =
                (GridBagConstraints) catalogConstraints.clone();
        propertiesConstraints.gridy = 1;
        propertiesConstraints.insets = new Insets(4, 0, 0, 0);
        editor.add(properties, propertiesConstraints);

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = 2;
        filler.weightx = 1;
        filler.weighty = 1;
        filler.fill = GridBagConstraints.BOTH;
        editor.add(new JPanel(), filler);
        return editor;
    }

    private void configureAccessibility() {
        configure(tabs, "Flutter theme editor sections",
                "Switches between general theme settings, ColorScheme overrides, "
                + "TextTheme typography overrides, and the closed 36-leaf "
                + "component color contract for the selected theme.");
        configure(themesEnabled, "Enable project themes",
                "Controls whether MaterialApp receives the project light, dark, "
                + "and mode values. The theme catalog is preserved while disabled.");
        configure(defaultMode, "Default theme mode",
                "Selects the mode used when project themes are enabled.");
        configure(lightTheme, "Application light theme",
                "Selects the light theme used when project themes are enabled.");
        configure(darkTheme, "Application dark theme",
                "Selects the dark theme used when project themes are enabled.");
        configure(themeList, "Project themes",
                "Lists built-in and custom project-wide Flutter themes. "
                + "Disabled entries are marked in the list.");
        configure(selectedThemeEnabled, "Enable selected theme",
                "Controls whether the selected theme is available to the project. "
                + "A theme referenced by the application must be replaced before it can be disabled.");
        configure(id, "Theme ID",
                "Canonical lower_snake_case identifier with at most "
                + FlutterProjectThemeDefinition.MAX_ID_CODE_POINTS + " code points.");
        configure(displayName, "Theme display name",
                "Human-readable name with at most "
                + FlutterProjectThemeDefinition.MAX_DISPLAY_NAME_CODE_POINTS + " code points.");
        configure(brightness, "Theme brightness",
                "Selects whether the theme generates a light or dark Flutter ColorScheme.");
        configure(seedColor, "Theme seed color",
                "Opens an RGB color chooser for the Material ColorScheme seed color. "
                + "The stored seed is normalized to opaque alpha FF.");
        configure(add, "Add custom theme", "Adds a new custom light theme.");
        configure(duplicate, "Duplicate theme", "Duplicates the selected theme as a custom theme.");
        configure(remove, "Remove custom theme",
                "Removes an unreferenced custom theme; built-in and referenced themes are protected.");
        configure(status, "Theme validation status",
                "Reports the exact reason why the current theme values cannot be saved.");
        configure(generatedNotice, "Generated theme source notice",
                Bundle.HINT_ThemeGeneratedNotice());
        generatedNotice.setToolTipText(Bundle.HINT_ThemeGeneratedNotice());

        add.setMnemonic('A');
        duplicate.setMnemonic('D');
        remove.setMnemonic('R');
    }

    private void configureListeners() {
        tabs.addChangeListener(event -> {
            if (updating) {
                return;
            }
            int requested = tabs.getSelectedIndex();
            if (requested == activeTab) {
                return;
            }
            if (!commitSelected()) {
                updating = true;
                try {
                    tabs.setSelectedIndex(activeTab);
                } finally {
                    updating = false;
                }
                return;
            }
            activeTab = requested;
            validateCurrentInput();
        });
        themesEnabled.addActionListener(event -> {
            if (!updating) {
                boolean previous = draft.enabled();
                boolean requested = themesEnabled.isSelected();
                draft.setEnabled(requested);
                String referenceProblem = requested
                        ? themeReferenceProblem()
                        : null;
                if (referenceProblem != null) {
                    draft.setEnabled(previous);
                    updating = true;
                    try {
                        themesEnabled.setSelected(previous);
                    } finally {
                        updating = false;
                    }
                    validateCurrentInput();
                    if (valid) {
                        setValidation(true, referenceProblem);
                    }
                    return;
                }
                validateCurrentInput();
                if (previous != requested) {
                    markChanged();
                }
            }
        });
        themeList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || updating) {
                return;
            }
            FlutterThemeEditorDraft.ThemeRow next = themeList.getSelectedValue();
            if (selected != null && next != selected && !commitSelected()) {
                updating = true;
                try {
                    themeList.setSelectedValue(selected, true);
                } finally {
                    updating = false;
                }
                return;
            }
            showSelected(next);
        });
        defaultMode.addActionListener(event -> {
            if (!updating && defaultMode.getSelectedItem() != null) {
                draft.setDefaultMode((FlutterThemeMode) defaultMode.getSelectedItem());
                validateCurrentInput();
                markChanged();
            }
        });
        lightTheme.addActionListener(event -> {
            if (!updating && lightTheme.getSelectedItem() != null) {
                draft.setLightTheme((FlutterThemeEditorDraft.ThemeRow)
                        lightTheme.getSelectedItem());
                refreshReferenceModels();
                updateButtonState();
                validateCurrentInput();
                markChanged();
            }
        });
        darkTheme.addActionListener(event -> {
            if (!updating && darkTheme.getSelectedItem() != null) {
                draft.setDarkTheme((FlutterThemeEditorDraft.ThemeRow)
                        darkTheme.getSelectedItem());
                refreshReferenceModels();
                updateButtonState();
                validateCurrentInput();
                markChanged();
            }
        });
        add.addActionListener(event -> {
            if (!commitSelected()) {
                return;
            }
            try {
                FlutterThemeEditorDraft.ThemeRow created = draft.addCustom();
                reloadList(created);
                setValidation(true, " ");
                markChanged();
            } catch (IllegalArgumentException failure) {
                setValidation(false, failure.getMessage());
            }
        });
        duplicate.addActionListener(event -> {
            if (!commitSelected() || selected == null) {
                return;
            }
            try {
                FlutterThemeEditorDraft.ThemeRow created = draft.duplicate(selected);
                reloadList(created);
                setValidation(true, " ");
                markChanged();
            } catch (IllegalArgumentException failure) {
                setValidation(false, failure.getMessage());
            }
        });
        remove.addActionListener(event -> {
            if (selected == null) {
                return;
            }
            int index = themeList.getSelectedIndex();
            try {
                draft.removeCustom(selected);
                List<FlutterThemeEditorDraft.ThemeRow> remaining = draft.themes();
                FlutterThemeEditorDraft.ThemeRow next = remaining.get(
                        Math.min(index, remaining.size() - 1));
                reloadList(next);
                setValidation(true, " ");
                markChanged();
            } catch (IllegalArgumentException failure) {
                setValidation(false, failure.getMessage());
            }
        });
        brightness.addActionListener(event -> {
            if (!updating) {
                validateCurrentInput();
                markChanged();
            }
        });
        selectedThemeEnabled.addActionListener(event -> {
            if (!updating) {
                if (!commitSelected()) {
                    String reason = status.getText();
                    updating = true;
                    try {
                        selectedThemeEnabled.setSelected(
                                selected != null && selected.enabled());
                    } finally {
                        updating = false;
                    }
                    validateCurrentInput();
                    if (valid) {
                        setValidation(true, reason);
                    }
                    return;
                }
                themeList.repaint();
                markChanged();
            }
        });
        seedColor.addActionListener(event -> chooseSeedColor());
        DocumentListener validationListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                validateCurrentInput();
                markChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                validateCurrentInput();
                markChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                validateCurrentInput();
                markChanged();
            }
        };
        id.getDocument().addDocumentListener(validationListener);
        displayName.getDocument().addDocumentListener(validationListener);
    }

    private void chooseSeedColor() {
        Color initial = new Color(selectedSeedArgb, true);
        Color chosen = colorChooser.choose(this, initial);
        if (chosen == null) {
            return;
        }
        selectedSeedArgb = 0xFF000000 | chosen.getRGB();
        updateSeedButton();
        validateCurrentInput();
        markChanged();
    }

    private boolean commitSelected() {
        if (selected == null) {
            return true;
        }
        if (!typographyOverridesEditor.commitCurrent()) {
            return false;
        }
        try {
            draft.update(
                    selected,
                    id.getText(),
                    displayName.getText(),
                    (FlutterThemeBrightness) brightness.getSelectedItem(),
                    selectedSeedArgb,
                    selectedThemeEnabled.isSelected());
            // ThemeRow is the stable list-model identity. Rebuilding the model here
            // would restore the old row while a ListSelectionEvent is switching to
            // another row, leaving the highlight and edited row out of sync.
            themeList.repaint();
            refreshReferenceModels();
            setValidation(true, " ");
            return true;
        } catch (IllegalArgumentException failure) {
            setValidation(false, message(failure));
            return false;
        }
    }

    private void validateCurrentInput() {
        if (updating || selected == null) {
            return;
        }
        try {
            new FlutterProjectThemeDefinition(
                    id.getText(),
                    displayName.getText(),
                    (FlutterThemeBrightness) brightness.getSelectedItem(),
                    selectedSeedArgb,
                    selectedThemeEnabled.isSelected(),
                    selected.overrides());
            for (FlutterThemeEditorDraft.ThemeRow candidate : draft.themes()) {
                if (candidate != selected && candidate.id().equals(id.getText())) {
                    throw new IllegalArgumentException(
                            "Duplicate project theme id: " + id.getText());
                }
            }
            FlutterThemeBrightness next =
                    (FlutterThemeBrightness) brightness.getSelectedItem();
            if (draft.lightTheme() == selected
                    && next != FlutterThemeBrightness.LIGHT) {
                throw new IllegalArgumentException(
                        "Choose another application light theme before changing this brightness.");
            }
            if (draft.darkTheme() == selected
                    && next != FlutterThemeBrightness.DARK) {
                throw new IllegalArgumentException(
                        "Choose another application dark theme before changing this brightness.");
            }
            String referenceProblem = themesEnabled.isSelected()
                    ? themeReferenceProblem()
                    : null;
            if (referenceProblem != null) {
                throw new IllegalArgumentException(referenceProblem);
            }
            String typographyProblem =
                    typographyOverridesEditor.validationProblem();
            if (typographyProblem != null) {
                throw new IllegalArgumentException(typographyProblem);
            }
            setValidation(true, " ");
        } catch (IllegalArgumentException failure) {
            setValidation(false, message(failure));
        }
    }

    private void showSelected(FlutterThemeEditorDraft.ThemeRow row) {
        selected = row;
        boolean previouslyUpdating = updating;
        updating = true;
        try {
            boolean enabled = row != null;
            boolean identityEditable = enabled && !row.builtIn();
            selectedThemeEnabled.setEnabled(enabled);
            id.setEnabled(identityEditable);
            displayName.setEnabled(enabled);
            brightness.setEnabled(identityEditable);
            seedColor.setEnabled(enabled);
            if (row == null) {
                id.setText("");
                displayName.setText("");
            } else {
                selectedThemeEnabled.setSelected(row.enabled());
                id.setText(row.id());
                displayName.setText(row.displayName());
                brightness.setSelectedItem(row.brightness());
                selectedSeedArgb = row.seedArgb();
                updateSeedButton();
            }
        } finally {
            updating = previouslyUpdating;
        }
        colorOverridesEditor.showTheme(row);
        typographyOverridesEditor.showTheme(row);
        componentColorsEditor.showTheme(row);
        updateButtonState();
        validateCurrentInput();
    }

    private void reloadList(FlutterThemeEditorDraft.ThemeRow selection) {
        boolean previouslyUpdating = updating;
        updating = true;
        try {
            listModel.clear();
            for (FlutterThemeEditorDraft.ThemeRow theme : draft.themes()) {
                listModel.addElement(theme);
            }
            refreshReferenceModels();
            if (selection != null) {
                themeList.setSelectedValue(selection, true);
            }
        } finally {
            updating = previouslyUpdating;
        }
        if (selection != null) {
            showSelected(selection);
        }
    }

    private void refreshReferenceModels() {
        boolean previouslyUpdating = updating;
        updating = true;
        try {
            DefaultComboBoxModel<FlutterThemeEditorDraft.ThemeRow> lightModel =
                    new DefaultComboBoxModel<>();
            DefaultComboBoxModel<FlutterThemeEditorDraft.ThemeRow> darkModel =
                    new DefaultComboBoxModel<>();
            for (FlutterThemeEditorDraft.ThemeRow theme : draft.themes()) {
                if (theme.brightness() == FlutterThemeBrightness.LIGHT
                        && (theme.enabled() || theme == draft.lightTheme())) {
                    lightModel.addElement(theme);
                } else if (theme.brightness() == FlutterThemeBrightness.DARK
                        && (theme.enabled() || theme == draft.darkTheme())) {
                    darkModel.addElement(theme);
                }
            }
            lightTheme.setModel(lightModel);
            darkTheme.setModel(darkModel);
            lightTheme.setSelectedItem(draft.lightTheme());
            darkTheme.setSelectedItem(draft.darkTheme());
            ThemeRenderer renderer = new ThemeRenderer();
            lightTheme.setRenderer(renderer);
            darkTheme.setRenderer(renderer);
            defaultMode.setRenderer(new EnumRenderer());
            brightness.setRenderer(new EnumRenderer());
        } finally {
            updating = previouslyUpdating;
        }
    }

    private void updateSeedButton() {
        Color color = new Color(selectedSeedArgb, true);
        seedColor.setBackground(color);
        seedColor.setForeground(contrastColor(color));
        seedColor.setText("#%08X".formatted(selectedSeedArgb));
    }

    private void updateButtonState() {
        duplicate.setEnabled(selected != null);
        remove.setEnabled(selected != null
                && !selected.builtIn()
                && draft.lightTheme() != selected
                && draft.darkTheme() != selected);
    }

    private void updateApplicationControls() {
        defaultMode.setEnabled(true);
        lightTheme.setEnabled(true);
        darkTheme.setEnabled(true);
    }

    private boolean effectiveThemeEnabled(
            FlutterThemeEditorDraft.ThemeRow row) {
        return row == selected
                ? selectedThemeEnabled.isSelected()
                : row.enabled();
    }

    private String themeReferenceProblem() {
        if (!effectiveThemeEnabled(draft.lightTheme())) {
            return "Enable the application light theme or select another enabled light theme first.";
        }
        if (!effectiveThemeEnabled(draft.darkTheme())) {
            return "Enable the application dark theme or select another enabled dark theme first.";
        }
        return null;
    }

    private void markChanged() {
        if (updating) {
            return;
        }
        long oldRevision = changeRevision++;
        firePropertyChange(PROP_CHANGED, oldRevision, changeRevision);
    }

    private void setValidation(boolean nextValid, String message) {
        boolean old = valid;
        String oldMessage = status.getText();
        valid = nextValid;
        status.setForeground(nextValid
                ? javax.swing.UIManager.getColor("Label.foreground")
                : javax.swing.UIManager.getColor("nb.errorForeground") == null
                        ? Color.RED
                        : javax.swing.UIManager.getColor("nb.errorForeground"));
        String nextMessage = message == null || message.isBlank() ? " " : message;
        status.setText(nextMessage);
        typographyOverridesEditor.showValidation(nextValid, nextMessage);
        if (old != nextValid) {
            firePropertyChange(PROP_VALID, old, nextValid);
        }
        if (!Objects.equals(oldMessage, nextMessage)) {
            firePropertyChange(
                    PROP_VALIDATION_MESSAGE, oldMessage, nextMessage);
        }
    }

    private static GridBagConstraints baseConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.anchor = GridBagConstraints.LINE_START;
        return constraints;
    }

    private static void addRow(
            JPanel panel,
            GridBagConstraints prototype,
            int row,
            String labelText,
            Component field) {
        GridBagConstraints labelConstraints = (GridBagConstraints) prototype.clone();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        JLabel label = new JLabel(labelText);
        label.setLabelFor(field);
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = (GridBagConstraints) prototype.clone();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, fieldConstraints);
    }

    private static void configure(
            javax.swing.JComponent component,
            String name,
            String description) {
        component.getAccessibleContext().setAccessibleName(name);
        component.getAccessibleContext().setAccessibleDescription(description);
    }

    private static Color contrastColor(Color background) {
        double luminance = 0.299 * background.getRed()
                + 0.587 * background.getGreen()
                + 0.114 * background.getBlue();
        return luminance >= 150 ? Color.BLACK : Color.WHITE;
    }

    private static String message(RuntimeException failure) {
        String value = failure.getMessage();
        return value == null || value.isBlank()
                ? failure.getClass().getSimpleName()
                : value;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(350, 420);
    }

    @Override
    public int getScrollableUnitIncrement(
            java.awt.Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(
            java.awt.Rectangle visibleRect, int orientation, int direction) {
        return Math.max(16, orientation == SwingConstants.VERTICAL
                ? visibleRect.height - 16
                : visibleRect.width - 16);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @FunctionalInterface
    interface ColorChooser {
        Color choose(Component parent, Color initial);
    }

    private final class ColorOverridesEditor extends JPanel {
        private final DefaultListModel<FlutterMaterialColorRole> model =
                new DefaultListModel<>();
        private final JList<FlutterMaterialColorRole> roles = new JList<>(model);
        private final JLabel state = new JLabel(" ");
        private final JButton choose = new JButton(
                Bundle.CTL_ThemeChooseOverride());
        private final JButton reset = new JButton(Bundle.CTL_ThemeUseDefault());
        private FlutterThemeEditorDraft.ThemeRow theme;

        ColorOverridesEditor() {
            super(new BorderLayout(4, 4));
            setBorder(BorderFactory.createTitledBorder(
                    Bundle.LBL_ThemeColorRoles()));
            for (FlutterMaterialColorRole role
                    : FlutterMaterialColorRole.values()) {
                model.addElement(role);
            }
            roles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            roles.setVisibleRowCount(10);
            roles.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list,
                        Object value,
                        int index,
                        boolean selectedValue,
                        boolean focused) {
                    FlutterMaterialColorRole role =
                            (FlutterMaterialColorRole) value;
                    Integer override = theme == null
                            ? null
                            : theme.overrides().colorScheme().get(role);
                    String text = humanize(role.wireName()) + " — "
                            + (override == null
                                    ? Bundle.LBL_ThemeInherit()
                                    : FlutterThemeTextStyleOverride
                                            .argbLiteral(override));
                    JLabel label = (JLabel) super.getListCellRendererComponent(
                            list, text, index, selectedValue, focused);
                    label.setToolTipText(text);
                    return label;
                }
            });
            JScrollPane scroll = new JScrollPane(roles);
            scroll.setHorizontalScrollBarPolicy(
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scroll, BorderLayout.CENTER);

            JPanel detail = new JPanel(new BorderLayout(4, 4));
            detail.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            detail.add(state, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
            buttons.add(choose);
            buttons.add(reset);
            detail.add(buttons, BorderLayout.SOUTH);
            add(detail, BorderLayout.SOUTH);

            configure(roles, "Material ColorScheme roles",
                    "Lists all 46 reviewed Material ColorScheme roles and "
                    + "whether each value is inherited or overridden.");
            configure(state, "Selected ColorScheme role state",
                    "Shows whether the selected role inherits its seed-generated "
                    + "value or has an exact ARGB override.");
            configure(choose, "Choose ColorScheme role override",
                    "Chooses an exact ARGB override for the selected ColorScheme role.");
            configure(reset, "Use default ColorScheme role value",
                    "Removes the selected role override so it inherits the seed-generated value.");

            roles.addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    refresh();
                }
            });
            choose.addActionListener(event -> chooseOverride());
            reset.addActionListener(event -> resetOverride());
            roles.setSelectedIndex(0);
            refresh();
        }

        void showTheme(FlutterThemeEditorDraft.ThemeRow value) {
            theme = value;
            roles.repaint();
            if (roles.getSelectedIndex() < 0 && !model.isEmpty()) {
                roles.setSelectedIndex(0);
            }
            refresh();
        }

        private void chooseOverride() {
            FlutterMaterialColorRole role = roles.getSelectedValue();
            if (theme == null || role == null) {
                return;
            }
            Integer existing = theme.overrides().colorScheme().get(role);
            Color initial = new Color(
                    existing == null ? theme.seedArgb() : existing, true);
            Color chosen = colorChooser.choose(this, initial);
            if (chosen == null) {
                return;
            }
            int argb = chosen.getRGB();
            if (!Objects.equals(existing, argb)) {
                draft.setColorOverride(theme, role, argb);
                roles.repaint();
                refresh();
                markChanged();
                validateCurrentInput();
            }
        }

        private void resetOverride() {
            FlutterMaterialColorRole role = roles.getSelectedValue();
            if (theme == null || role == null
                    || !theme.overrides().colorScheme().containsKey(role)) {
                return;
            }
            draft.setColorOverride(theme, role, null);
            roles.repaint();
            refresh();
            markChanged();
            validateCurrentInput();
        }

        private void refresh() {
            FlutterMaterialColorRole role = roles.getSelectedValue();
            Integer override = theme == null || role == null
                    ? null
                    : theme.overrides().colorScheme().get(role);
            boolean available = theme != null && role != null;
            choose.setEnabled(available);
            reset.setEnabled(available && override != null);
            state.setText(!available
                    ? " "
                    : override == null
                            ? Bundle.LBL_ThemeColorInherited()
                            : Bundle.LBL_ThemeColorOverride(
                                    FlutterThemeTextStyleOverride
                                            .argbLiteral(override)));
        }
    }

    private final class ComponentColorsEditor extends JPanel {
        private final DefaultListModel<FlutterThemeComponentColorRole> model =
                new DefaultListModel<>();
        private final JList<FlutterThemeComponentColorRole> roles =
                new JList<>(model);
        private final OptionalColorControl color =
                new OptionalColorControl("Component color", this::storeCurrent);
        private FlutterThemeEditorDraft.ThemeRow theme;
        private boolean loading;

        ComponentColorsEditor() {
            super(new BorderLayout(4, 4));
            setBorder(BorderFactory.createTitledBorder(
                    Bundle.LBL_ThemeComponentColors()));
            for (FlutterThemeComponentColorRole role
                    : FlutterThemeComponentColorRole.values()) {
                model.addElement(role);
            }
            roles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            roles.setVisibleRowCount(11);
            roles.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list,
                        Object value,
                        int index,
                        boolean selectedValue,
                        boolean focused) {
                    FlutterThemeComponentColorRole role =
                            (FlutterThemeComponentColorRole) value;
                    FlutterThemeColorValue configured = theme == null
                            ? null
                            : theme.overrides().componentColors().get(role);
                    String state = configured == null
                            ? Bundle.LBL_ThemeInherit()
                            : switch (configured) {
                                case FlutterThemeColorValue.Literal literal ->
                                    literal.argbLiteral();
                                case FlutterThemeColorValue.ColorRole semantic ->
                                    "ColorScheme." + semantic.role().wireName();
                            };
                    String text = role.displayName() + " — " + state;
                    JLabel label = (JLabel) super.getListCellRendererComponent(
                            list, text, index, selectedValue, focused);
                    label.setToolTipText(role.wireName() + " — " + state);
                    return label;
                }
            });
            JScrollPane scroll = new JScrollPane(roles);
            scroll.setHorizontalScrollBarPolicy(
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scroll, BorderLayout.CENTER);
            add(color, BorderLayout.SOUTH);

            configure(roles, "Flutter component color leaves",
                    "Lists the exact 36 schema-v5 Scaffold, AppBar, Icon and "
                    + "ElevatedButton component color leaves.");
            roles.addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting() && !loading) {
                    loadCurrent();
                }
            });
            roles.setSelectedIndex(0);
        }

        void showTheme(FlutterThemeEditorDraft.ThemeRow next) {
            theme = next;
            roles.setEnabled(next != null);
            loadCurrent();
            roles.repaint();
        }

        private void loadCurrent() {
            loading = true;
            try {
                FlutterThemeComponentColorRole role = roles.getSelectedValue();
                FlutterThemeColorValue value = theme == null || role == null
                        ? null
                        : theme.overrides().componentColors().get(role);
                color.load(Optional.ofNullable(value));
                color.setEnabled(theme != null && role != null);
            } finally {
                loading = false;
            }
        }

        private void storeCurrent() {
            if (loading || updating || theme == null) {
                return;
            }
            FlutterThemeComponentColorRole role = roles.getSelectedValue();
            if (role == null) {
                return;
            }
            draft.setComponentColorOverride(
                    theme, role, color.read().orElse(null));
            roles.repaint();
            validateCurrentInput();
            markChanged();
        }
    }

    private final class TypographyOverridesEditor extends JPanel {
        private final DefaultListModel<FlutterMaterialTextStyleRole> model =
                new DefaultListModel<>();
        private final JList<FlutterMaterialTextStyleRole> roles =
                new JList<>(model);
        private final JLabel state = new JLabel(" ");
        private final JLabel localValidation = new JLabel(" ");
        private final JButton resetRole = new JButton(
                Bundle.CTL_ThemeResetTypographyRole());
        private final OptionalColorControl color;
        private final OptionalColorControl backgroundColor;
        private final OptionalDoubleControl fontSize;
        private final OptionalEnumControl<FlutterThemeFontWeight> fontWeight;
        private final OptionalEnumControl<FlutterThemeFontStyle> fontStyle;
        private final OptionalDoubleControl letterSpacing;
        private final OptionalDoubleControl wordSpacing;
        private final OptionalDoubleControl height;
        private final OptionalStringControl fontFamily;
        private final DecorationControl decoration;
        private final OptionalColorControl decorationColor;
        private final OptionalEnumControl<FlutterThemeTextDecorationStyle>
                decorationStyle;
        private final OptionalDoubleControl decorationThickness;
        private FlutterThemeEditorDraft.ThemeRow theme;
        private FlutterMaterialTextStyleRole role;
        private boolean loading;

        TypographyOverridesEditor() {
            super(new BorderLayout(4, 4));
            Runnable changed = this::controlChanged;
            color = new OptionalColorControl(Bundle.LBL_ThemeTextColor(), changed);
            backgroundColor = new OptionalColorControl(
                    Bundle.LBL_ThemeBackgroundColor(), changed);
            fontSize = new OptionalDoubleControl(
                    Bundle.LBL_ThemeFontSize(), "14", changed);
            fontWeight = new OptionalEnumControl<>(
                    Bundle.LBL_ThemeFontWeight(),
                    FlutterThemeFontWeight.values(), changed);
            fontStyle = new OptionalEnumControl<>(
                    Bundle.LBL_ThemeFontStyle(),
                    FlutterThemeFontStyle.values(), changed);
            letterSpacing = new OptionalDoubleControl(
                    Bundle.LBL_ThemeLetterSpacing(), "0", changed);
            wordSpacing = new OptionalDoubleControl(
                    Bundle.LBL_ThemeWordSpacing(), "0", changed);
            height = new OptionalDoubleControl(
                    Bundle.LBL_ThemeLineHeight(), "1", changed);
            fontFamily = new OptionalStringControl(
                    Bundle.LBL_ThemeFontFamily(), changed);
            decoration = new DecorationControl(changed);
            decorationColor = new OptionalColorControl(
                    Bundle.LBL_ThemeDecorationColor(), changed);
            decorationStyle = new OptionalEnumControl<>(
                    Bundle.LBL_ThemeDecorationStyle(),
                    FlutterThemeTextDecorationStyle.values(), changed);
            decorationThickness = new OptionalDoubleControl(
                    Bundle.LBL_ThemeDecorationThickness(), "1", changed);

            JPanel catalog = new JPanel(new BorderLayout(4, 4));
            catalog.setBorder(BorderFactory.createTitledBorder(
                    Bundle.LBL_ThemeTypographyRoles()));
            for (FlutterMaterialTextStyleRole value
                    : FlutterMaterialTextStyleRole.values()) {
                model.addElement(value);
            }
            roles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            roles.setVisibleRowCount(5);
            roles.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list,
                        Object value,
                        int index,
                        boolean selectedValue,
                        boolean focused) {
                    FlutterMaterialTextStyleRole textRole =
                            (FlutterMaterialTextStyleRole) value;
                    boolean overridden = theme != null
                            && theme.overrides().textTheme()
                                    .containsKey(textRole);
                    String text = humanize(textRole.wireName()) + " — "
                            + (overridden ? "Override" : Bundle.LBL_ThemeInherit());
                    JLabel label = (JLabel) super.getListCellRendererComponent(
                            list, text, index, selectedValue, focused);
                    label.setToolTipText(text);
                    return label;
                }
            });
            JScrollPane roleScroll = new JScrollPane(roles);
            roleScroll.setHorizontalScrollBarPolicy(
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            catalog.add(roleScroll, BorderLayout.CENTER);
            add(catalog, BorderLayout.NORTH);

            JPanel fields = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.PAGE_START;
            constraints.insets = new Insets(2, 2, 2, 2);
            int row = 0;
            JPanel summary = new JPanel(new BorderLayout(4, 4));
            summary.add(state, BorderLayout.CENTER);
            summary.add(resetRole, BorderLayout.LINE_END);
            addFullRow(fields, constraints, row++, summary);
            addFullRow(fields, constraints, row++, color);
            addFullRow(fields, constraints, row++, backgroundColor);
            addFullRow(fields, constraints, row++, fontSize);
            addFullRow(fields, constraints, row++, fontWeight);
            addFullRow(fields, constraints, row++, fontStyle);
            addFullRow(fields, constraints, row++, letterSpacing);
            addFullRow(fields, constraints, row++, wordSpacing);
            addFullRow(fields, constraints, row++, height);
            addFullRow(fields, constraints, row++, fontFamily);
            addFullRow(fields, constraints, row++, decoration);
            addFullRow(fields, constraints, row++, decorationColor);
            addFullRow(fields, constraints, row++, decorationStyle);
            addFullRow(fields, constraints, row++, decorationThickness);
            addFullRow(fields, constraints, row++, localValidation);
            constraints.gridy = row;
            constraints.weighty = 1;
            constraints.fill = GridBagConstraints.BOTH;
            fields.add(new JPanel(), constraints);
            JScrollPane fieldScroll = new JScrollPane(fields);
            fieldScroll.setHorizontalScrollBarPolicy(
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            fieldScroll.getVerticalScrollBar().setUnitIncrement(16);
            fieldScroll.setBorder(BorderFactory.createEmptyBorder());
            add(fieldScroll, BorderLayout.CENTER);

            configure(roles, "Material TextTheme roles",
                    "Lists all 15 Material TextTheme roles and whether each "
                    + "role inherits or has explicit fields.");
            configure(state, "Selected TextTheme role state",
                    "Shows whether the selected TextTheme role inherits or "
                    + "how many fields it overrides.");
            configure(resetRole, "Reset selected TextTheme role",
                    "Removes every explicit field from the selected role.");
            configure(localValidation, "Typography validation status",
                    "Reports the exact invalid typography field without changing "
                    + "the last confirmed theme draft.");

            roles.addListSelectionListener(event -> {
                if (event.getValueIsAdjusting() || loading) {
                    return;
                }
                FlutterMaterialTextStyleRole next = roles.getSelectedValue();
                if (role != null && next != role && !commitCurrent()) {
                    loading = true;
                    try {
                        roles.setSelectedValue(role, true);
                    } finally {
                        loading = false;
                    }
                    return;
                }
                role = next;
                loadCurrent();
                validateCurrentInput();
            });
            resetRole.addActionListener(event -> resetCurrentRole());
            roles.setSelectedIndex(0);
            role = roles.getSelectedValue();
            loadCurrent();
        }

        void showTheme(FlutterThemeEditorDraft.ThemeRow value) {
            theme = value;
            roles.repaint();
            if (role == null && !model.isEmpty()) {
                role = model.getElementAt(0);
            }
            loading = true;
            try {
                roles.setSelectedValue(role, true);
                loadCurrent();
            } finally {
                loading = false;
            }
        }

        boolean commitCurrent() {
            if (loading || theme == null || role == null) {
                return true;
            }
            try {
                FlutterThemeTextStyleOverride next = currentStyle();
                FlutterThemeTextStyleOverride previous = theme.overrides()
                        .textTheme().getOrDefault(
                                role, FlutterThemeTextStyleOverride.EMPTY);
                if (!previous.equals(next)) {
                    draft.setTextStyleOverride(theme, role, next);
                    roles.repaint();
                    refreshState(next);
                    markChanged();
                }
                return true;
            } catch (IllegalArgumentException failure) {
                setValidation(false, typographyProblem(failure));
                return false;
            }
        }

        String validationProblem() {
            if (loading || theme == null || role == null) {
                return null;
            }
            try {
                currentStyle();
                return null;
            } catch (IllegalArgumentException failure) {
                return typographyProblem(failure);
            }
        }

        void showValidation(boolean nextValid, String message) {
            localValidation.setForeground(nextValid
                    ? javax.swing.UIManager.getColor("Label.foreground")
                    : javax.swing.UIManager.getColor("nb.errorForeground") == null
                            ? Color.RED
                            : javax.swing.UIManager.getColor("nb.errorForeground"));
            localValidation.setText(nextValid ? " " : message);
            localValidation.setToolTipText(nextValid ? null : message);
        }

        private void controlChanged() {
            if (loading) {
                return;
            }
            if (commitCurrent()) {
                validateCurrentInput();
            }
        }

        private void resetCurrentRole() {
            if (theme == null || role == null) {
                return;
            }
            FlutterThemeTextStyleOverride previous = theme.overrides()
                    .textTheme().get(role);
            loading = true;
            try {
                loadStyle(FlutterThemeTextStyleOverride.EMPTY);
            } finally {
                loading = false;
            }
            if (previous != null) {
                draft.setTextStyleOverride(
                        theme, role, FlutterThemeTextStyleOverride.EMPTY);
                roles.repaint();
                markChanged();
            }
            refreshState(FlutterThemeTextStyleOverride.EMPTY);
            validateCurrentInput();
        }

        private void loadCurrent() {
            FlutterThemeTextStyleOverride style = theme == null || role == null
                    ? FlutterThemeTextStyleOverride.EMPTY
                    : theme.overrides().textTheme().getOrDefault(
                            role, FlutterThemeTextStyleOverride.EMPTY);
            boolean previous = loading;
            loading = true;
            try {
                loadStyle(style);
            } finally {
                loading = previous;
            }
            refreshState(style);
        }

        private void loadStyle(FlutterThemeTextStyleOverride style) {
            color.load(style.color());
            backgroundColor.load(style.backgroundColor());
            fontSize.load(style.fontSize());
            fontWeight.load(style.fontWeight());
            fontStyle.load(style.fontStyle());
            letterSpacing.load(style.letterSpacing());
            wordSpacing.load(style.wordSpacing());
            height.load(style.height());
            fontFamily.load(style.fontFamily());
            decoration.load(style.decoration());
            decorationColor.load(style.decorationColor());
            decorationStyle.load(style.decorationStyle());
            decorationThickness.load(style.decorationThickness());
        }

        private FlutterThemeTextStyleOverride currentStyle() {
            return new FlutterThemeTextStyleOverride(
                    color.read(),
                    backgroundColor.read(),
                    fontSize.read(),
                    fontWeight.read(),
                    fontStyle.read(),
                    letterSpacing.read(),
                    wordSpacing.read(),
                    height.read(),
                    fontFamily.read(),
                    decoration.read(),
                    decorationColor.read(),
                    decorationStyle.read(),
                    decorationThickness.read());
        }

        private void refreshState(FlutterThemeTextStyleOverride style) {
            boolean available = theme != null && role != null;
            resetRole.setEnabled(available && !style.isEmpty());
            state.setText(!available
                    ? " "
                    : style.isEmpty()
                            ? Bundle.LBL_ThemeTypographyInherited()
                            : Bundle.LBL_ThemeTypographyOverride(
                                    styleFieldCount(style)));
        }

        private String typographyProblem(IllegalArgumentException failure) {
            return (role == null ? "Typography" : humanize(role.wireName()))
                    + ": " + message(failure);
        }
    }

    enum ColorValueMode {
        INHERIT,
        COLOR_ROLE,
        LITERAL
    }

    final class OptionalColorControl extends JPanel {
        final JComboBox<ColorValueMode> mode =
                new JComboBox<>(ColorValueMode.values());
        final JComboBox<FlutterMaterialColorRole> role =
                new JComboBox<>(FlutterMaterialColorRole.values());
        final JButton literal = new JButton();
        private final String label;
        private final Runnable changed;
        private int literalArgb = 0xFF6750A4;
        private boolean loading;

        OptionalColorControl(String label, Runnable changed) {
            super(new GridBagLayout());
            this.label = label;
            this.changed = changed;
            setBorder(BorderFactory.createTitledBorder(label));
            GridBagConstraints constraints = baseConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            add(mode, constraints);
            constraints.gridy = 1;
            add(role, constraints);
            add(literal, constraints);
            mode.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list,
                        Object value,
                        int index,
                        boolean selected,
                        boolean focused) {
                    String text = switch ((ColorValueMode) value) {
                        case INHERIT -> Bundle.LBL_ThemeInherit();
                        case COLOR_ROLE -> Bundle.LBL_ThemeColorRole();
                        case LITERAL -> Bundle.LBL_ThemeColorLiteral();
                    };
                    return super.getListCellRendererComponent(
                            list, text, index, selected, focused);
                }
            });
            role.setRenderer(new WireNameRenderer());
            configure(mode, label + " source",
                    "Chooses inherit, a semantic ColorScheme role, or an exact ARGB literal.");
            configure(role, label + " ColorScheme role",
                    "Selects the semantic Material ColorScheme role.");
            configure(literal, label + " ARGB literal",
                    "Opens a color chooser for the exact ARGB literal.");
            mode.addActionListener(event -> {
                refreshMode();
                changed();
            });
            role.addActionListener(event -> changed());
            literal.addActionListener(event -> chooseLiteral());
            refreshLiteral();
            refreshMode();
        }

        void load(Optional<FlutterThemeColorValue> value) {
            loading = true;
            try {
                if (value.isEmpty()) {
                    mode.setSelectedItem(ColorValueMode.INHERIT);
                } else if (value.orElseThrow()
                        instanceof FlutterThemeColorValue.ColorRole semantic) {
                    role.setSelectedItem(semantic.role());
                    mode.setSelectedItem(ColorValueMode.COLOR_ROLE);
                } else if (value.orElseThrow()
                        instanceof FlutterThemeColorValue.Literal exact) {
                    literalArgb = exact.argb();
                    mode.setSelectedItem(ColorValueMode.LITERAL);
                }
                refreshLiteral();
                refreshMode();
            } finally {
                loading = false;
            }
        }

        Optional<FlutterThemeColorValue> read() {
            return switch ((ColorValueMode) mode.getSelectedItem()) {
                case INHERIT -> Optional.empty();
                case COLOR_ROLE -> Optional.of(
                        new FlutterThemeColorValue.ColorRole(
                                (FlutterMaterialColorRole) role.getSelectedItem()));
                case LITERAL -> Optional.of(
                        new FlutterThemeColorValue.Literal(literalArgb));
            };
        }

        private void chooseLiteral() {
            Color selected = colorChooser.choose(
                    this, new Color(literalArgb, true));
            if (selected == null) {
                return;
            }
            literalArgb = selected.getRGB();
            loading = true;
            try {
                mode.setSelectedItem(ColorValueMode.LITERAL);
                refreshLiteral();
                refreshMode();
            } finally {
                loading = false;
            }
            changed.run();
        }

        private void refreshMode() {
            ColorValueMode selected = (ColorValueMode) mode.getSelectedItem();
            role.setVisible(selected == ColorValueMode.COLOR_ROLE);
            literal.setVisible(selected == ColorValueMode.LITERAL);
            revalidate();
            repaint();
        }

        private void refreshLiteral() {
            Color value = new Color(literalArgb, true);
            literal.setText(FlutterThemeTextStyleOverride
                    .argbLiteral(literalArgb));
            literal.setBackground(value);
            literal.setForeground(contrastColor(value));
        }

        private void changed() {
            if (!loading) {
                changed.run();
            }
        }
    }

    final class OptionalDoubleControl extends JPanel {
        final JCheckBox override;
        final JTextField value = new JTextField(10);
        private final String label;
        private final String defaultValue;
        private final Runnable changed;
        private boolean loading;

        OptionalDoubleControl(
                String label, String defaultValue, Runnable changed) {
            super(new BorderLayout(4, 0));
            this.label = label;
            this.defaultValue = defaultValue;
            this.changed = changed;
            override = new JCheckBox(Bundle.LBL_ThemeOverrideField(label));
            add(override, BorderLayout.LINE_START);
            add(value, BorderLayout.CENTER);
            configure(override, "Override " + label,
                    "Select to store an explicit " + label
                    + "; clear to inherit the Material TextTheme value.");
            configure(value, label + " value",
                    "Finite Flutter logical-pixel value for " + label + ".");
            override.addActionListener(event -> {
                loading = true;
                try {
                    if (override.isSelected() && value.getText().isBlank()) {
                        value.setText(defaultValue);
                    }
                    value.setEnabled(override.isSelected());
                } finally {
                    loading = false;
                }
                changed.run();
            });
            value.getDocument().addDocumentListener(
                    documentListener(this::changed));
            value.setEnabled(false);
        }

        void load(Optional<Double> next) {
            loading = true;
            try {
                override.setSelected(next.isPresent());
                value.setText(next.map(String::valueOf).orElse(""));
                value.setEnabled(next.isPresent());
            } finally {
                loading = false;
            }
        }

        Optional<Double> read() {
            if (!override.isSelected()) {
                return Optional.empty();
            }
            String text = value.getText().strip();
            if (text.isEmpty()) {
                throw new IllegalArgumentException(
                        label + " is required when overridden");
            }
            try {
                return Optional.of(Double.valueOf(text));
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException(
                        label + " must be a finite number", failure);
            }
        }

        private void changed() {
            if (!loading) {
                changed.run();
            }
        }
    }

    final class OptionalStringControl extends JPanel {
        final JCheckBox override;
        final JTextField value = new JTextField(12);
        private final Runnable changed;
        private boolean loading;

        OptionalStringControl(String label, Runnable changed) {
            super(new BorderLayout(4, 0));
            this.changed = changed;
            override = new JCheckBox(Bundle.LBL_ThemeOverrideField(label));
            add(override, BorderLayout.LINE_START);
            add(value, BorderLayout.CENTER);
            configure(override, "Override " + label,
                    "Select to store an explicit " + label
                    + "; clear to inherit the Material TextTheme value.");
            configure(value, label + " value",
                    "Exact Flutter font-family name without surrounding whitespace.");
            override.addActionListener(event -> {
                value.setEnabled(override.isSelected());
                changed();
            });
            value.getDocument().addDocumentListener(
                    documentListener(this::changed));
            value.setEnabled(false);
        }

        void load(Optional<String> next) {
            loading = true;
            try {
                override.setSelected(next.isPresent());
                value.setText(next.orElse(""));
                value.setEnabled(next.isPresent());
            } finally {
                loading = false;
            }
        }

        Optional<String> read() {
            return override.isSelected()
                    ? Optional.of(value.getText())
                    : Optional.empty();
        }

        private void changed() {
            if (!loading) {
                changed.run();
            }
        }
    }

    final class OptionalEnumControl<T extends Enum<T>> extends JPanel {
        final JCheckBox override;
        final JComboBox<T> value;
        private final Runnable changed;
        private boolean loading;

        OptionalEnumControl(String label, T[] values, Runnable changed) {
            super(new BorderLayout(4, 0));
            this.changed = changed;
            override = new JCheckBox(Bundle.LBL_ThemeOverrideField(label));
            value = new JComboBox<>(values);
            value.setRenderer(new WireNameRenderer());
            add(override, BorderLayout.LINE_START);
            add(value, BorderLayout.CENTER);
            configure(override, "Override " + label,
                    "Select to store an explicit " + label
                    + "; clear to inherit the Material TextTheme value.");
            configure(value, label + " value", "Selects the explicit " + label + ".");
            override.addActionListener(event -> {
                value.setEnabled(override.isSelected());
                changed();
            });
            value.addActionListener(event -> changed());
            value.setEnabled(false);
        }

        void load(Optional<T> next) {
            loading = true;
            try {
                override.setSelected(next.isPresent());
                next.ifPresent(value::setSelectedItem);
                value.setEnabled(next.isPresent());
            } finally {
                loading = false;
            }
        }

        Optional<T> read() {
            return override.isSelected()
                    ? Optional.of(value.getItemAt(value.getSelectedIndex()))
                    : Optional.empty();
        }

        private void changed() {
            if (!loading) {
                changed.run();
            }
        }
    }

    final class DecorationControl extends JPanel {
        final JCheckBox override = new JCheckBox(
                Bundle.LBL_ThemeOverrideField(Bundle.LBL_ThemeDecoration()));
        final JCheckBox underline = new JCheckBox(Bundle.LBL_ThemeUnderline());
        final JCheckBox overline = new JCheckBox(Bundle.LBL_ThemeOverline());
        final JCheckBox lineThrough = new JCheckBox(
                Bundle.LBL_ThemeLineThrough());
        private final Runnable changed;
        private boolean loading;

        DecorationControl(Runnable changed) {
            super(new GridBagLayout());
            this.changed = changed;
            GridBagConstraints constraints = baseConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 3;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            add(override, constraints);
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            constraints.weightx = 0;
            add(underline, constraints);
            constraints.gridx = 1;
            add(overline, constraints);
            constraints.gridx = 2;
            add(lineThrough, constraints);
            configure(override, "Override decoration lines",
                    "Select to store a decoration override. Leaving every line "
                    + "clear explicitly generates TextDecoration.none.");
            configure(underline, "Underline decoration", "Includes TextDecoration.underline.");
            configure(overline, "Overline decoration", "Includes TextDecoration.overline.");
            configure(lineThrough, "Line-through decoration",
                    "Includes TextDecoration.lineThrough.");
            override.addActionListener(event -> {
                refreshEnabled();
                changed();
            });
            underline.addActionListener(event -> changed());
            overline.addActionListener(event -> changed());
            lineThrough.addActionListener(event -> changed());
            refreshEnabled();
        }

        void load(Optional<Set<FlutterThemeTextDecorationLine>> next) {
            loading = true;
            try {
                override.setSelected(next.isPresent());
                Set<FlutterThemeTextDecorationLine> lines =
                        next.orElseGet(Set::of);
                underline.setSelected(lines.contains(
                        FlutterThemeTextDecorationLine.UNDERLINE));
                overline.setSelected(lines.contains(
                        FlutterThemeTextDecorationLine.OVERLINE));
                lineThrough.setSelected(lines.contains(
                        FlutterThemeTextDecorationLine.LINE_THROUGH));
                refreshEnabled();
            } finally {
                loading = false;
            }
        }

        Optional<Set<FlutterThemeTextDecorationLine>> read() {
            if (!override.isSelected()) {
                return Optional.empty();
            }
            EnumSet<FlutterThemeTextDecorationLine> lines =
                    EnumSet.noneOf(FlutterThemeTextDecorationLine.class);
            if (underline.isSelected()) {
                lines.add(FlutterThemeTextDecorationLine.UNDERLINE);
            }
            if (overline.isSelected()) {
                lines.add(FlutterThemeTextDecorationLine.OVERLINE);
            }
            if (lineThrough.isSelected()) {
                lines.add(FlutterThemeTextDecorationLine.LINE_THROUGH);
            }
            return Optional.of(Set.copyOf(lines));
        }

        private void refreshEnabled() {
            boolean enabled = override.isSelected();
            underline.setEnabled(enabled);
            overline.setEnabled(enabled);
            lineThrough.setEnabled(enabled);
        }

        private void changed() {
            if (!loading) {
                changed.run();
            }
        }
    }

    private static DocumentListener documentListener(Runnable changed) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                changed.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                changed.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                changed.run();
            }
        };
    }

    private static void addFullRow(
            JPanel panel,
            GridBagConstraints prototype,
            int row,
            Component component) {
        GridBagConstraints constraints =
                (GridBagConstraints) prototype.clone();
        constraints.gridy = row;
        panel.add(component, constraints);
    }

    private static int styleFieldCount(FlutterThemeTextStyleOverride style) {
        return (int) java.util.stream.Stream.of(
                style.color(), style.backgroundColor(), style.fontSize(),
                style.fontWeight(), style.fontStyle(), style.letterSpacing(),
                style.wordSpacing(), style.height(), style.fontFamily(),
                style.decoration(), style.decorationColor(),
                style.decorationStyle(), style.decorationThickness())
                .filter(Optional::isPresent)
                .count();
    }

    private static String humanize(String wireName) {
        String words = wireName.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static final class WireNameRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focused) {
            String text;
            if (value instanceof FlutterMaterialColorRole role) {
                text = humanize(role.wireName());
            } else if (value instanceof FlutterThemeFontWeight weight) {
                text = weight.wireName();
            } else if (value instanceof FlutterThemeFontStyle style) {
                text = humanize(style.wireName());
            } else if (value instanceof FlutterThemeTextDecorationStyle style) {
                text = humanize(style.wireName());
            } else {
                text = String.valueOf(value);
            }
            return super.getListCellRendererComponent(
                    list, text, index, selected, focused);
        }
    }

    private static final class ThemeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focused) {
            Object rendered = value instanceof FlutterThemeEditorDraft.ThemeRow theme
                    ? theme.displayName() + " (" + theme.id() + ")"
                            + (theme.enabled()
                                    ? ""
                                    : " — " + Bundle.LBL_ThemeOff())
                    : value;
            JLabel component = (JLabel) super.getListCellRendererComponent(
                    list, rendered, index, selected, focused);
            component.setToolTipText(String.valueOf(rendered));
            return component;
        }
    }

    private static final class EnumRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focused) {
            String rendered = value instanceof FlutterThemeMode mode
                    ? switch (mode) {
                        case SYSTEM -> Bundle.LBL_ThemeModeSystem();
                        case LIGHT -> Bundle.LBL_ThemeModeLight();
                        case DARK -> Bundle.LBL_ThemeModeDark();
                    }
                    : value instanceof FlutterThemeBrightness valueBrightness
                            ? valueBrightness == FlutterThemeBrightness.LIGHT
                                    ? Bundle.LBL_ThemeBrightnessLight()
                                    : Bundle.LBL_ThemeBrightnessDark()
                            : String.valueOf(value);
            return super.getListCellRendererComponent(
                    list, rendered, index, selected, focused);
        }
    }
}
