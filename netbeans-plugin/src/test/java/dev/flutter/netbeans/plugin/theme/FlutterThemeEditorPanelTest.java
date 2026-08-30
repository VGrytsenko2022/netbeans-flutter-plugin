package dev.flutter.netbeans.plugin.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.project.theme.FlutterMaterialColorRole;
import dev.flutter.netbeans.project.theme.FlutterMaterialTextStyleRole;
import dev.flutter.netbeans.project.theme.FlutterProjectTheme;
import dev.flutter.netbeans.project.theme.FlutterThemeColorValue;
import dev.flutter.netbeans.project.theme.FlutterThemeComponentColorRole;
import dev.flutter.netbeans.project.theme.FlutterThemeFontStyle;
import dev.flutter.netbeans.project.theme.FlutterThemeFontWeight;
import dev.flutter.netbeans.project.theme.FlutterThemeTextDecorationLine;
import dev.flutter.netbeans.project.theme.FlutterThemeTextDecorationStyle;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class FlutterThemeEditorPanelTest {
    @Test
    void exposesAccessibleNamesDescriptionsAndGeneratedSourceWarning()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            assertAccessible(panel);
            assertAccessible(panel.enabledCheckBoxForTest());
            assertAccessible(panel.themeListForTest());
            assertAccessible(panel.addButtonForTest());
            assertAccessible(panel.duplicateButtonForTest());
            assertAccessible(panel.removeButtonForTest());
            assertAccessible(panel.selectedThemeEnabledForTest());
            assertAccessible(panel.idFieldForTest());
            assertAccessible(panel.displayNameFieldForTest());
            assertAccessible(panel.brightnessForTest());
            assertAccessible(panel.seedColorButtonForTest());
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("project-wide"));
        });
    }

    @Test
    void staysCompactInTheCommonPaletteSidebar() throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);

            assertEquals(new Dimension(350, 420),
                    panel.getPreferredScrollableViewportSize());
            assertTrue(panel.getScrollableTracksViewportWidth());
            assertFalse(panel.getScrollableTracksViewportHeight());
            assertEquals(3, panel.themeListForTest().getVisibleRowCount());
            JScrollPane catalogScroll = (JScrollPane)
                    SwingUtilities.getAncestorOfClass(
                            JScrollPane.class, panel.themeListForTest());
            assertNotNull(catalogScroll);
            assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
                    catalogScroll.getHorizontalScrollBarPolicy());
            assertEquals("New", panel.addButtonForTest().getText());
            assertEquals("Duplicate", panel.duplicateButtonForTest().getText());
            assertEquals("Delete", panel.removeButtonForTest().getText());
            assertTrue(panel.generatedNoticeForTest().getText().length() < 50);
            assertTrue(panel.generatedNoticeForTest().getToolTipText()
                    .contains("Manual Dart changes"));

            panel.setSize(350, 420);
            layoutTree(panel);
            assertTrue(panel.getMinimumSize().width <= 350,
                    "the editor must be able to shrink to the commonpalette width");
            assertVisibleWithin(panel, panel.addButtonForTest(), "New");
            assertVisibleWithin(panel, panel.duplicateButtonForTest(), "Duplicate");
            assertVisibleWithin(panel, panel.removeButtonForTest(), "Delete");
            assertVisibleWithin(panel, panel.selectedThemeEnabledForTest(),
                    "selected-theme Enabled");
            assertVisibleWithin(panel, panel.idFieldForTest(), "ID");
            assertVisibleWithin(panel, panel.displayNameFieldForTest(), "Name");
            assertVisibleWithin(panel, panel.brightnessForTest(), "Brightness");
            assertVisibleWithin(panel, panel.seedColorButtonForTest(), "Seed");
        });
    }

    @Test
    void exposesCompactAccessibleGeneralColorsAndTypographySections()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);

            assertEquals(4, panel.tabsForTest().getTabCount());
            assertEquals("General", panel.tabsForTest().getTitleAt(0));
            assertEquals("Colors", panel.tabsForTest().getTitleAt(1));
            assertEquals("Typography", panel.tabsForTest().getTitleAt(2));
            assertEquals("Components", panel.tabsForTest().getTitleAt(3));
            assertEquals(46, panel.colorRoleListForTest().getModel().getSize());
            assertEquals(15,
                    panel.typographyRoleListForTest().getModel().getSize());
            assertEquals(FlutterThemeComponentColorRole.LEAF_COUNT,
                    panel.componentColorRoleListForTest().getModel().getSize());
            for (int index = 0;
                    index < FlutterThemeComponentColorRole.values().length;
                    index++) {
                assertEquals(FlutterThemeComponentColorRole.values()[index],
                        panel.componentColorRoleListForTest().getModel()
                                .getElementAt(index));
            }
            assertAccessible(panel.tabsForTest());
            assertAccessible(panel.colorRoleListForTest());
            assertAccessible(panel.colorOverrideButtonForTest());
            assertAccessible(panel.colorResetButtonForTest());
            assertAccessible(panel.typographyRoleListForTest());
            assertAccessible(panel.typographyResetRoleButtonForTest());
            assertAccessible(panel.typographyFontSizeForTest().override);
            assertAccessible(panel.typographyFontSizeForTest().value);
            assertAccessible(panel.typographyDecorationForTest().override);
            assertAccessible(panel.componentColorRoleListForTest());
            assertAccessible(panel.componentColorControlForTest().mode);
            assertAccessible(panel.componentColorControlForTest().role);
            assertAccessible(panel.componentColorControlForTest().literal);
        });
    }

    @Test
    void componentColorsPersistLiteralSemanticAndInheritedValuesAcrossThemes()
            throws Exception {
        onEdt(() -> {
            Color literal = new Color(0x12, 0x34, 0x56, 0x78);
            FlutterThemeEditorPanel panel = panel(
                    (parent, initial) -> literal);
            panel.tabsForTest().setSelectedIndex(3);
            var roles = panel.componentColorRoleListForTest();
            var color = panel.componentColorControlForTest();
            FlutterThemeEditorDraft.ThemeRow light =
                    panel.themeListForTest().getSelectedValue();

            roles.setSelectedValue(
                    FlutterThemeComponentColorRole.SCAFFOLD_BACKGROUND, true);
            color.literal.doClick();
            assertEquals(new FlutterThemeColorValue.Literal(literal.getRGB()),
                    light.overrides().componentColors().get(
                            FlutterThemeComponentColorRole.SCAFFOLD_BACKGROUND));

            roles.setSelectedValue(
                    FlutterThemeComponentColorRole.APP_BAR_FOREGROUND, true);
            color.role.setSelectedItem(FlutterMaterialColorRole.ON_PRIMARY);
            color.mode.setSelectedItem(
                    FlutterThemeEditorPanel.ColorValueMode.COLOR_ROLE);
            assertEquals(new FlutterThemeColorValue.ColorRole(
                            FlutterMaterialColorRole.ON_PRIMARY),
                    light.overrides().componentColors().get(
                            FlutterThemeComponentColorRole.APP_BAR_FOREGROUND));

            panel.themeListForTest().setSelectedIndex(1);
            assertEquals(FlutterThemeEditorPanel.ColorValueMode.INHERIT,
                    color.mode.getSelectedItem(),
                    "component leaves are scoped to one selected theme");
            panel.themeListForTest().setSelectedIndex(0);
            assertEquals(FlutterThemeEditorPanel.ColorValueMode.COLOR_ROLE,
                    color.mode.getSelectedItem());
            assertEquals(FlutterMaterialColorRole.ON_PRIMARY,
                    color.role.getSelectedItem());

            roles.setSelectedValue(
                    FlutterThemeComponentColorRole.SCAFFOLD_BACKGROUND, true);
            assertEquals(FlutterThemeEditorPanel.ColorValueMode.LITERAL,
                    color.mode.getSelectedItem());
            color.mode.setSelectedItem(
                    FlutterThemeEditorPanel.ColorValueMode.INHERIT);
            assertFalse(light.overrides().componentColors().containsKey(
                    FlutterThemeComponentColorRole.SCAFFOLD_BACKGROUND));

            FlutterProjectTheme saved = panel.buildTheme();
            assertEquals(new FlutterThemeColorValue.ColorRole(
                            FlutterMaterialColorRole.ON_PRIMARY),
                    saved.lightTheme().overrides().componentColors().get(
                            FlutterThemeComponentColorRole.APP_BAR_FOREGROUND));
            assertFalse(saved.lightTheme().overrides().componentColors()
                    .containsKey(
                            FlutterThemeComponentColorRole.SCAFFOLD_BACKGROUND));
        });
    }

    @Test
    void generalValidationRetainsConfirmedComponentOverrides()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            panel.tabsForTest().setSelectedIndex(3);
            panel.componentColorRoleListForTest().setSelectedValue(
                    FlutterThemeComponentColorRole.ICON_COLOR, true);
            panel.componentColorControlForTest().role.setSelectedItem(
                    FlutterMaterialColorRole.TERTIARY);
            panel.componentColorControlForTest().mode.setSelectedItem(
                    FlutterThemeEditorPanel.ColorValueMode.COLOR_ROLE);
            FlutterThemeEditorDraft.ThemeRow light =
                    panel.themeListForTest().getSelectedValue();
            FlutterThemeColorValue expected = new FlutterThemeColorValue.ColorRole(
                    FlutterMaterialColorRole.TERTIARY);
            assertEquals(expected, light.overrides().componentColors().get(
                    FlutterThemeComponentColorRole.ICON_COLOR));

            panel.tabsForTest().setSelectedIndex(0);
            panel.displayNameFieldForTest().setText(" ");
            assertFalse(panel.isEditorValid());
            assertEquals(expected, light.overrides().componentColors().get(
                    FlutterThemeComponentColorRole.ICON_COLOR),
                    "validation must use and retain the full selected overrides object");

            panel.displayNameFieldForTest().setText("Light");
            assertTrue(panel.isEditorValid());
            assertEquals(expected, panel.buildTheme().lightTheme().overrides()
                    .componentColors().get(
                            FlutterThemeComponentColorRole.ICON_COLOR));
        });
    }

    @Test
    void tabSwitchCommitsValidGeneralFieldsAndBlocksInvalidDraft()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            panel.addButtonForTest().doClick();
            FlutterThemeEditorDraft.ThemeRow custom =
                    panel.themeListForTest().getSelectedValue();
            panel.idFieldForTest().setText("brand_theme");

            panel.tabsForTest().setSelectedIndex(1);

            assertEquals(1, panel.tabsForTest().getSelectedIndex());
            assertEquals("brand_theme", custom.id());
            panel.tabsForTest().setSelectedIndex(0);
            panel.idFieldForTest().setText("Not valid");

            panel.tabsForTest().setSelectedIndex(2);

            assertEquals(0, panel.tabsForTest().getSelectedIndex(),
                    "invalid General input must keep the user on General");
            assertFalse(panel.isEditorValid());
            assertEquals("brand_theme", custom.id(),
                    "an invalid field must not corrupt the confirmed draft");
            panel.idFieldForTest().setText("brand_theme_2");
            panel.tabsForTest().setSelectedIndex(2);
            assertEquals(2, panel.tabsForTest().getSelectedIndex());
            assertEquals("brand_theme_2", custom.id());
        });
    }

    @Test
    void colorRolesCommitExactOverridesAndResetToSeedInheritance()
            throws Exception {
        onEdt(() -> {
            Color chosen = new Color(0x40, 0x20, 0x10, 0x80);
            FlutterThemeEditorPanel panel = panel(
                    (parent, initial) -> chosen);
            panel.tabsForTest().setSelectedIndex(1);
            panel.colorRoleListForTest().setSelectedValue(
                    FlutterMaterialColorRole.PRIMARY_FIXED, true);

            panel.colorOverrideButtonForTest().doClick();

            FlutterThemeEditorDraft.ThemeRow light =
                    panel.themeListForTest().getSelectedValue();
            assertEquals(chosen.getRGB(), light.overrides().colorScheme()
                    .get(FlutterMaterialColorRole.PRIMARY_FIXED));
            assertTrue(panel.colorStateForTest().getText()
                    .contains("0x80402010"));
            assertTrue(panel.colorResetButtonForTest().isEnabled());
            assertEquals(chosen.getRGB(), panel.buildTheme().lightTheme()
                    .overrides().colorScheme()
                    .get(FlutterMaterialColorRole.PRIMARY_FIXED));

            panel.colorResetButtonForTest().doClick();

            assertFalse(light.overrides().colorScheme()
                    .containsKey(FlutterMaterialColorRole.PRIMARY_FIXED));
            assertTrue(panel.colorStateForTest().getText()
                    .contains("Inherited"));
            assertFalse(panel.colorResetButtonForTest().isEnabled());
        });
    }

    @Test
    void typographyMasterDetailCommitsAllTypedFieldsAndResetsRole()
            throws Exception {
        onEdt(() -> {
            Color literalBackground = new Color(0x12, 0x34, 0x56, 0x80);
            FlutterThemeEditorPanel panel = panel(
                    (parent, initial) -> literalBackground);
            panel.tabsForTest().setSelectedIndex(2);
            panel.typographyRoleListForTest().setSelectedValue(
                    FlutterMaterialTextStyleRole.BODY_LARGE, true);

            panel.typographyColorForTest().mode.setSelectedItem(
                    FlutterThemeEditorPanel.ColorValueMode.COLOR_ROLE);
            panel.typographyColorForTest().role.setSelectedItem(
                    FlutterMaterialColorRole.PRIMARY_FIXED);
            panel.typographyBackgroundColorForTest().literal.doClick();
            setOptional(panel.typographyFontSizeForTest(), "20.5");
            selectOptional(panel.typographyFontWeightForTest(),
                    FlutterThemeFontWeight.W700);
            selectOptional(panel.typographyFontStyleForTest(),
                    FlutterThemeFontStyle.ITALIC);
            setOptional(panel.typographyLetterSpacingForTest(), "-0.25");
            setOptional(panel.typographyWordSpacingForTest(), "1.5");
            setOptional(panel.typographyHeightForTest(), "1.25");
            panel.typographyFontFamilyForTest().override.doClick();
            panel.typographyFontFamilyForTest().value.setText("Inter");
            panel.typographyDecorationForTest().override.doClick();
            panel.typographyDecorationForTest().underline.doClick();
            panel.typographyDecorationForTest().lineThrough.doClick();
            panel.typographyDecorationColorForTest().mode.setSelectedItem(
                    FlutterThemeEditorPanel.ColorValueMode.COLOR_ROLE);
            panel.typographyDecorationColorForTest().role.setSelectedItem(
                    FlutterMaterialColorRole.ERROR);
            selectOptional(panel.typographyDecorationStyleForTest(),
                    FlutterThemeTextDecorationStyle.WAVY);
            setOptional(panel.typographyDecorationThicknessForTest(), "2");

            FlutterThemeEditorDraft.ThemeRow light =
                    panel.themeListForTest().getSelectedValue();
            var style = light.overrides().textTheme()
                    .get(FlutterMaterialTextStyleRole.BODY_LARGE);
            assertNotNull(style, "valid controls must commit without a second Apply click");
            assertEquals(new FlutterThemeColorValue.ColorRole(
                    FlutterMaterialColorRole.PRIMARY_FIXED),
                    style.color().orElseThrow());
            assertEquals(new FlutterThemeColorValue.Literal(
                    literalBackground.getRGB()),
                    style.backgroundColor().orElseThrow());
            assertEquals(20.5d, style.fontSize().orElseThrow());
            assertEquals(FlutterThemeFontWeight.W700,
                    style.fontWeight().orElseThrow());
            assertEquals(FlutterThemeFontStyle.ITALIC,
                    style.fontStyle().orElseThrow());
            assertEquals(-0.25d, style.letterSpacing().orElseThrow());
            assertEquals(1.5d, style.wordSpacing().orElseThrow());
            assertEquals(1.25d, style.height().orElseThrow());
            assertEquals("Inter", style.fontFamily().orElseThrow());
            assertEquals(Set.of(
                    FlutterThemeTextDecorationLine.UNDERLINE,
                    FlutterThemeTextDecorationLine.LINE_THROUGH),
                    style.decoration().orElseThrow());
            assertEquals(new FlutterThemeColorValue.ColorRole(
                    FlutterMaterialColorRole.ERROR),
                    style.decorationColor().orElseThrow());
            assertEquals(FlutterThemeTextDecorationStyle.WAVY,
                    style.decorationStyle().orElseThrow());
            assertEquals(2.0d, style.decorationThickness().orElseThrow());
            assertTrue(panel.isEditorValid());

            panel.typographyResetRoleButtonForTest().doClick();

            assertFalse(light.overrides().textTheme()
                    .containsKey(FlutterMaterialTextStyleRole.BODY_LARGE));
            assertFalse(panel.typographyResetRoleButtonForTest().isEnabled());
        });
    }

    @Test
    void invalidTypographyBlocksSaveAndRoleSwitchWithoutCorruptingDraft()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            panel.tabsForTest().setSelectedIndex(2);
            panel.typographyRoleListForTest().setSelectedValue(
                    FlutterMaterialTextStyleRole.BODY_SMALL, true);
            setOptional(panel.typographyFontSizeForTest(), "14");
            FlutterThemeEditorDraft.ThemeRow light =
                    panel.themeListForTest().getSelectedValue();

            panel.typographyFontSizeForTest().value.setText("-1");

            assertFalse(panel.isEditorValid());
            assertEquals(14.0d, light.overrides().textTheme()
                    .get(FlutterMaterialTextStyleRole.BODY_SMALL)
                    .fontSize().orElseThrow(),
                    "invalid input must retain the last confirmed value");
            assertThrows(IllegalArgumentException.class, panel::buildTheme);
            panel.typographyRoleListForTest().setSelectedValue(
                    FlutterMaterialTextStyleRole.LABEL_LARGE, true);
            assertEquals(FlutterMaterialTextStyleRole.BODY_SMALL,
                    panel.typographyRoleListForTest().getSelectedValue(),
                    "invalid input must block master-role navigation");

            panel.typographyFontSizeForTest().value.setText("16");
            assertTrue(panel.isEditorValid());
            panel.typographyRoleListForTest().setSelectedValue(
                    FlutterMaterialTextStyleRole.LABEL_LARGE, true);
            assertEquals(16.0d, light.overrides().textTheme()
                    .get(FlutterMaterialTextStyleRole.BODY_SMALL)
                    .fontSize().orElseThrow());

            panel.typographyDecorationForTest().override.doClick();
            var explicitNone = light.overrides().textTheme()
                    .get(FlutterMaterialTextStyleRole.LABEL_LARGE);
            assertTrue(explicitNone.decoration().isPresent());
            assertTrue(explicitNone.decoration().orElseThrow().isEmpty(),
                    "an enabled empty decoration is TextDecoration.none, not inherit");
            panel.typographyDecorationForTest().override.doClick();
            assertFalse(light.overrides().textTheme()
                    .containsKey(FlutterMaterialTextStyleRole.LABEL_LARGE));
        });
    }

    @Test
    void invalidIdDisablesSaveContractAndReportsEveryValidationChange()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            AtomicInteger messageEvents = new AtomicInteger();
            panel.addPropertyChangeListener(
                    FlutterThemeEditorPanel.PROP_VALIDATION_MESSAGE,
                    event -> messageEvents.incrementAndGet());
            panel.addButtonForTest().doClick();

            panel.idFieldForTest().setText("Not valid");
            assertFalse(panel.isEditorValid());
            assertTrue(panel.validationMessage().contains("lower_snake_case"));
            panel.idFieldForTest().setText("Still invalid");
            assertFalse(panel.isEditorValid());
            assertTrue(messageEvents.get() >= 2,
                    "validation message changes must notify the dialog validity bridge");

            panel.idFieldForTest().setText("light_brand");
            assertTrue(panel.isEditorValid());
            assertTrue(panel.buildTheme().themes().stream()
                    .anyMatch(theme -> "light_brand".equals(theme.id())));
        });
    }

    @Test
    void colorChooserStoresAndDisplaysOpaqueArgb() throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel(
                    (parent, initial) -> new Color(0x12, 0x34, 0x56, 0x20));

            panel.seedColorButtonForTest().doClick();
            assertEquals("#FF123456", panel.seedColorButtonForTest().getText());
            assertEquals(0xFF123456, panel.buildTheme().lightTheme().seedArgb());
            assertTrue(panel.seedColorButtonForTest().getAccessibleContext()
                    .getAccessibleDescription().contains("opaque alpha FF"));
        });
    }

    @Test
    void buttonsImplementFirstSafeCrudRules() throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            assertFalse(panel.removeButtonForTest().isEnabled(),
                    "built-in selected theme must not be removable");

            panel.addButtonForTest().doClick();
            assertEquals(3, panel.themeListForTest().getModel().getSize());
            assertTrue(panel.removeButtonForTest().isEnabled());
            assertEquals(
                    panel.themeListForTest().getSelectedValue().id(),
                    panel.idFieldForTest().getText());
            panel.duplicateButtonForTest().doClick();
            assertEquals(4, panel.themeListForTest().getModel().getSize());
            assertEquals(
                    panel.themeListForTest().getSelectedValue().id(),
                    panel.idFieldForTest().getText());
            panel.removeButtonForTest().doClick();
            assertEquals(3, panel.themeListForTest().getModel().getSize());
            assertEquals(
                    panel.themeListForTest().getSelectedValue().id(),
                    panel.idFieldForTest().getText());
        });
    }

    @Test
    void builtInIdentityIsReadOnlyWhileAppearanceRemainsEditable()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);

            assertFalse(panel.idFieldForTest().isEnabled());
            assertFalse(panel.brightnessForTest().isEnabled());
            assertTrue(panel.displayNameFieldForTest().isEnabled());
            assertTrue(panel.seedColorButtonForTest().isEnabled());

            panel.addButtonForTest().doClick();
            assertTrue(panel.idFieldForTest().isEnabled());
            assertTrue(panel.brightnessForTest().isEnabled());
        });
    }

    @Test
    void successfulRowSwitchKeepsListHighlightAndEditedThemeAligned()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            panel.addButtonForTest().doClick();
            assertEquals(2, panel.themeListForTest().getSelectedIndex());

            panel.themeListForTest().setSelectedIndex(0);

            assertEquals(0, panel.themeListForTest().getSelectedIndex());
            assertEquals("light", panel.idFieldForTest().getText());
            assertFalse(panel.idFieldForTest().isEnabled());

            panel.themeListForTest().setSelectedIndex(1);
            assertEquals(1, panel.themeListForTest().getSelectedIndex());
            assertEquals("dark", panel.idFieldForTest().getText());
        });
    }

    @Test
    void disablingProjectThemesIsAnExplicitEditableDraftChange()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            AtomicInteger changes = new AtomicInteger();
            panel.addPropertyChangeListener(
                    FlutterThemeEditorPanel.PROP_CHANGED,
                    event -> changes.incrementAndGet());

            assertTrue(panel.enabledCheckBoxForTest().isSelected());
            assertTrue(panel.buildTheme().enabled());

            panel.enabledCheckBoxForTest().doClick();

            assertFalse(panel.enabledCheckBoxForTest().isSelected());
            assertFalse(panel.buildTheme().enabled());
            assertTrue(panel.defaultModeForTest().isEnabled());
            assertTrue(panel.lightThemeForTest().isEnabled());
            assertTrue(panel.darkThemeForTest().isEnabled());
            assertTrue(panel.isEditorValid());
            assertTrue(changes.get() >= 1,
                    "the docked editor must mark the project draft dirty");
        });
    }

    @Test
    void selectedThemeCanBeDisabledAndIsMarkedInTheCatalog()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            panel.addButtonForTest().doClick();

            assertTrue(panel.selectedThemeEnabledForTest().isSelected());
            panel.selectedThemeEnabledForTest().doClick();

            assertTrue(panel.isEditorValid());
            FlutterProjectTheme saved = panel.buildTheme();
            assertFalse(saved.themes().stream()
                    .filter(theme -> "custom_theme".equals(theme.id()))
                    .findFirst()
                    .orElseThrow()
                    .enabled());

            ListCellRenderer<? super FlutterThemeEditorDraft.ThemeRow> renderer =
                    panel.themeListForTest().getCellRenderer();
            Component rendered = renderer.getListCellRendererComponent(
                    panel.themeListForTest(),
                    panel.themeListForTest().getSelectedValue(),
                    panel.themeListForTest().getSelectedIndex(),
                    true,
                    false);
            assertTrue(rendered instanceof JLabel);
            assertTrue(((JLabel) rendered).getText().contains("Off"));
        });
    }

    @Test
    void referencedThemeMustBeReplacedBeforeItCanBeDisabled()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);

            panel.selectedThemeEnabledForTest().doClick();

            assertTrue(panel.isEditorValid());
            assertTrue(panel.selectedThemeEnabledForTest().isSelected());
            assertTrue(panel.statusTextForTest().contains(
                    "another application light theme"));
            assertTrue(panel.buildTheme().lightTheme().enabled());
        });
    }

    @Test
    void failedEnableToggleDoesNotHideExistingFieldValidation()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            panel.addButtonForTest().doClick();
            panel.idFieldForTest().setText("Not valid");
            assertFalse(panel.isEditorValid());

            panel.selectedThemeEnabledForTest().doClick();

            assertTrue(panel.selectedThemeEnabledForTest().isSelected());
            assertFalse(panel.isEditorValid());
            assertTrue(panel.validationMessage().contains("lower_snake_case"));
        });
    }

    @Test
    void enablingProjectThemesWithDisabledReferenceRevertsTheSwitch()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            panel.enabledCheckBoxForTest().doClick();
            panel.selectedThemeEnabledForTest().doClick();
            assertFalse(panel.buildTheme().enabled());
            assertFalse(panel.buildTheme().lightTheme().enabled());

            panel.enabledCheckBoxForTest().doClick();

            assertFalse(panel.enabledCheckBoxForTest().isSelected());
            assertTrue(panel.isEditorValid());
            assertTrue(panel.statusTextForTest().contains(
                    "select another enabled light theme"));
            assertFalse(panel.buildTheme().enabled());
        });
    }

    @Test
    void referenceSelectorKeepsOnlyEnabledThemesAndDisabledCurrentReference()
            throws Exception {
        onEdt(() -> {
            FlutterThemeEditorPanel panel = panel((parent, initial) -> null);
            panel.enabledCheckBoxForTest().doClick();
            panel.selectedThemeEnabledForTest().doClick();
            panel.addButtonForTest().doClick();

            FlutterThemeEditorDraft.ThemeRow replacement =
                    panel.themeListForTest().getSelectedValue();
            assertEquals(2, panel.lightThemeForTest().getItemCount(),
                    "the disabled current reference stays visible for recovery");

            panel.lightThemeForTest().setSelectedItem(replacement);

            assertEquals(1, panel.lightThemeForTest().getItemCount(),
                    "a disabled theme disappears once it is no longer referenced");
            assertEquals(replacement, panel.lightThemeForTest().getSelectedItem());
        });
    }

    private static FlutterThemeEditorPanel panel(
            FlutterThemeEditorPanel.ColorChooser chooser) {
        return new FlutterThemeEditorPanel(
                FlutterProjectTheme.defaultTheme("0".repeat(64)), chooser);
    }

    private static void setOptional(
            FlutterThemeEditorPanel.OptionalDoubleControl control,
            String value) {
        if (!control.override.isSelected()) {
            control.override.doClick();
        }
        control.value.setText(value);
    }

    private static <T extends Enum<T>> void selectOptional(
            FlutterThemeEditorPanel.OptionalEnumControl<T> control,
            T value) {
        if (!control.override.isSelected()) {
            control.override.doClick();
        }
        control.value.setSelectedItem(value);
    }

    private static void assertAccessible(JComponent component) {
        assertNotNull(component.getAccessibleContext().getAccessibleName());
        assertFalse(component.getAccessibleContext().getAccessibleName().isBlank());
        assertNotNull(component.getAccessibleContext().getAccessibleDescription());
        assertFalse(component.getAccessibleContext().getAccessibleDescription().isBlank());
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutTree(nested);
            }
        }
    }

    private static void assertVisibleWithin(
            JComponent panel, Component component, String name) {
        Rectangle bounds = SwingUtilities.convertRectangle(
                component.getParent(), component.getBounds(), panel);
        assertTrue(bounds.width > 0 && bounds.height > 0,
                name + " must receive usable layout bounds: " + bounds);
        assertTrue(bounds.x >= 0 && bounds.y >= 0
                        && bounds.getMaxX() <= panel.getWidth()
                        && bounds.getMaxY() <= panel.getHeight(),
                name + " must be visible in the 350x420 first screen: " + bounds);
    }

    private static void onEdt(ThrowingRunnable task) throws Exception {
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                task.run();
            } catch (Throwable thrown) {
                failure[0] = thrown;
            }
        });
        if (failure[0] instanceof Exception exception) {
            throw exception;
        }
        if (failure[0] instanceof Error error) {
            throw error;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
