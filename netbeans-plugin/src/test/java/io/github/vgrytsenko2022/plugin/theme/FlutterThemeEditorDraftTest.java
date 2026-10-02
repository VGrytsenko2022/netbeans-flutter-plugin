package io.github.vgrytsenko2022.plugin.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.project.theme.FlutterGeneratedThemeArtifact;
import io.github.vgrytsenko2022.project.theme.FlutterMaterialColorRole;
import io.github.vgrytsenko2022.project.theme.FlutterProjectTheme;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeDefinition;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemePaths;
import io.github.vgrytsenko2022.project.theme.FlutterThemeBrightness;
import io.github.vgrytsenko2022.project.theme.FlutterThemeColorValue;
import io.github.vgrytsenko2022.project.theme.FlutterThemeComponentColorRole;
import io.github.vgrytsenko2022.project.theme.FlutterThemeMode;
import org.junit.jupiter.api.Test;

class FlutterThemeEditorDraftTest {
    @Test
    void supportsCustomAddDuplicateEditAndRemove() {
        FlutterThemeEditorDraft draft = new FlutterThemeEditorDraft(defaultTheme());

        FlutterThemeEditorDraft.ThemeRow custom = draft.addCustom();
        draft.update(custom, "brand_light", "Brand Light",
                FlutterThemeBrightness.LIGHT, 0xFF123456);
        FlutterThemeEditorDraft.ThemeRow duplicate = draft.duplicate(custom);
        draft.update(duplicate, "brand_dark", "Brand Dark",
                FlutterThemeBrightness.DARK, 0xFF654321);
        draft.setLightTheme(custom);
        draft.setDarkTheme(duplicate);
        draft.setDefaultMode(FlutterThemeMode.DARK);

        FlutterProjectTheme edited = draft.build(defaultTheme().generated());
        assertEquals("brand_light", edited.lightThemeId());
        assertEquals("brand_dark", edited.darkThemeId());
        assertEquals(FlutterThemeMode.DARK, edited.defaultMode());
        assertEquals(4, edited.themes().size());

        IllegalArgumentException referenced = assertThrows(
                IllegalArgumentException.class,
                () -> draft.removeCustom(custom));
        assertTrue(referenced.getMessage().contains("referenced"));

        draft.setLightTheme(draft.themes().get(0));
        draft.removeCustom(custom);
        assertFalse(draft.themes().contains(custom));
    }

    @Test
    void builtInAppearanceIsEditableButIdentityAndRemovalAreProtected() {
        FlutterThemeEditorDraft draft = new FlutterThemeEditorDraft(defaultTheme());
        FlutterThemeEditorDraft.ThemeRow builtInLight = draft.themes().get(0);

        draft.update(builtInLight, builtInLight.id(), "Brand Light",
                FlutterThemeBrightness.LIGHT, 0xFF010203);
        FlutterProjectTheme edited = draft.build(defaultTheme().generated());

        assertEquals(FlutterProjectTheme.DEFAULT_LIGHT_THEME_ID,
                edited.lightThemeId());
        assertEquals("Brand Light", edited.lightTheme().displayName());
        assertEquals(0xFF010203, edited.lightTheme().seedArgb());
        assertThrows(IllegalArgumentException.class,
                () -> draft.update(builtInLight, "light_brand", "Brand Light",
                        FlutterThemeBrightness.LIGHT, 0xFF010203));
        assertThrows(IllegalArgumentException.class,
                () -> draft.update(builtInLight, builtInLight.id(), "Brand Light",
                        FlutterThemeBrightness.DARK, 0xFF010203));
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> draft.removeCustom(builtInLight));
        assertTrue(failure.getMessage().contains("Built-in"));
    }

    @Test
    void referencedBrightnessCannotBecomeInvalid() {
        FlutterThemeEditorDraft draft = new FlutterThemeEditorDraft(defaultTheme());
        FlutterThemeEditorDraft.ThemeRow light = draft.lightTheme();

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> draft.update(light, light.id(), light.displayName(),
                        FlutterThemeBrightness.DARK, light.seedArgb()));

        assertTrue(failure.getMessage().contains("brightness"));
        assertEquals(FlutterThemeBrightness.LIGHT, light.brightness());
    }

    @Test
    void validatesIdsDuplicatesNamesAndCatalogLimitThroughCoreContract() {
        FlutterThemeEditorDraft draft = new FlutterThemeEditorDraft(defaultTheme());
        FlutterThemeEditorDraft.ThemeRow custom = draft.addCustom();

        assertThrows(IllegalArgumentException.class,
                () -> draft.update(custom, "Not Canonical", "Custom",
                        FlutterThemeBrightness.LIGHT, 0xFF000000));
        assertThrows(IllegalArgumentException.class,
                () -> draft.update(custom, "light", "Custom",
                        FlutterThemeBrightness.LIGHT, 0xFF000000));
        assertThrows(IllegalArgumentException.class,
                () -> draft.update(custom, "custom_theme", "  ",
                        FlutterThemeBrightness.LIGHT, 0xFF000000));

        while (draft.themes().size() < FlutterProjectTheme.MAX_THEMES) {
            draft.addCustom();
        }
        assertThrows(IllegalArgumentException.class, draft::addCustom);
    }

    @Test
    void duplicateIsEnabledEvenWhenItsSourceIsDisabled() {
        FlutterThemeEditorDraft draft = new FlutterThemeEditorDraft(defaultTheme());
        draft.setEnabled(false);
        FlutterThemeEditorDraft.ThemeRow custom = draft.addCustom();
        draft.update(custom, custom.id(), custom.displayName(),
                custom.brightness(), custom.seedArgb(), false);

        FlutterThemeEditorDraft.ThemeRow duplicate = draft.duplicate(custom);

        assertFalse(custom.enabled());
        assertTrue(duplicate.enabled());
    }

    @Test
    void componentColorOverridesAreTypedCopiedAndIndependentlyInherited() {
        FlutterThemeEditorDraft draft = new FlutterThemeEditorDraft(defaultTheme());
        FlutterThemeEditorDraft.ThemeRow custom = draft.addCustom();
        FlutterThemeColorValue literal =
                new FlutterThemeColorValue.Literal(0x80123456);
        FlutterThemeColorValue semantic = new FlutterThemeColorValue.ColorRole(
                FlutterMaterialColorRole.ON_PRIMARY_CONTAINER);

        draft.setComponentColorOverride(custom,
                FlutterThemeComponentColorRole.APP_BAR_SHADOW, literal);
        draft.setComponentColorOverride(custom,
                FlutterThemeComponentColorRole.ELEVATED_BUTTON_ICON_PRESSED,
                semantic);
        FlutterThemeEditorDraft.ThemeRow duplicate = draft.duplicate(custom);

        assertEquals(literal, duplicate.overrides().componentColors().get(
                FlutterThemeComponentColorRole.APP_BAR_SHADOW));
        assertEquals(semantic, duplicate.overrides().componentColors().get(
                FlutterThemeComponentColorRole.ELEVATED_BUTTON_ICON_PRESSED));

        draft.setComponentColorOverride(custom,
                FlutterThemeComponentColorRole.APP_BAR_SHADOW, null);
        assertFalse(custom.overrides().componentColors().containsKey(
                FlutterThemeComponentColorRole.APP_BAR_SHADOW));
        assertEquals(literal, duplicate.overrides().componentColors().get(
                FlutterThemeComponentColorRole.APP_BAR_SHADOW),
                "resetting one theme must not mutate a duplicate's overrides");

        FlutterProjectTheme saved = draft.build(defaultTheme().generated());
        assertEquals(semantic, saved.themes().stream()
                .filter(theme -> custom.id().equals(theme.id()))
                .findFirst().orElseThrow().overrides().componentColors().get(
                        FlutterThemeComponentColorRole
                                .ELEVATED_BUTTON_ICON_PRESSED));
    }

    private static FlutterProjectTheme defaultTheme() {
        return FlutterProjectTheme.defaultTheme("0".repeat(64));
    }
}
