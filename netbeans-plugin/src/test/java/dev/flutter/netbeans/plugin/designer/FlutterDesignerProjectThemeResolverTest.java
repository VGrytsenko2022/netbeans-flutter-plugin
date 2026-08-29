package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasThemeBrightness;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import dev.flutter.netbeans.project.theme.FlutterProjectTheme;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeCodec;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeDefinition;
import dev.flutter.netbeans.project.theme.FlutterMaterialColorRole;
import dev.flutter.netbeans.project.theme.FlutterMaterialTextStyleRole;
import dev.flutter.netbeans.project.theme.FlutterThemeColorValue;
import dev.flutter.netbeans.project.theme.FlutterThemeOverrides;
import dev.flutter.netbeans.project.theme.FlutterThemeTextStyleOverride;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeLoadResult;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeLoadStatus;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FlutterDesignerProjectThemeResolverTest {
    private static final Path PROJECT = Path.of("project").toAbsolutePath();
    private static final FlutterProjectTheme THEME =
            FlutterProjectTheme.defaultTheme("A".repeat(64));

    @Test
    void missingDescriptorUsesTheExactLegacyBrightnessBehavior() {
        FlutterDesignerProjectThemeResolver resolver = resolver(new FlutterProjectThemeLoadResult(
                FlutterProjectThemeLoadStatus.MISSING,
                Optional.empty(),
                "Descriptor is missing."), CanvasThemeBrightness.DARK);

        var absent = resolver.resolve(PROJECT, Optional.empty());
        var system = resolver.resolve(PROJECT, Optional.of(DesignerThemeMode.SYSTEM));
        var dark = resolver.resolve(PROJECT, Optional.of(DesignerThemeMode.DARK));

        assertTrue(absent.available());
        assertTrue(absent.legacyFallback());
        assertEquals(CanvasThemeBrightness.LIGHT,
                absent.theme().orElseThrow().brightness());
        assertEquals(CanvasThemeBrightness.DARK,
                system.theme().orElseThrow().brightness());
        assertEquals(CanvasThemeBrightness.DARK,
                dark.theme().orElseThrow().brightness());
        assertEquals(0xFF6750A4, dark.theme().orElseThrow().seedArgb());
    }

    @Test
    void validDescriptorResolvesProjectDefaultAndPerFormOverrides() {
        FlutterDesignerProjectThemeResolver resolver = resolver(valid(),
                CanvasThemeBrightness.DARK);

        var inherited = resolver.resolve(PROJECT, Optional.empty());
        var light = resolver.resolve(PROJECT, Optional.of(DesignerThemeMode.LIGHT));
        var dark = resolver.resolve(PROJECT, Optional.of(DesignerThemeMode.DARK));

        assertFalse(inherited.legacyFallback());
        assertEquals("dark", inherited.theme().orElseThrow().definitionId());
        assertEquals(CanvasThemeBrightness.DARK,
                inherited.theme().orElseThrow().brightness());
        assertEquals("light", light.theme().orElseThrow().definitionId());
        assertEquals(CanvasThemeBrightness.LIGHT,
                light.theme().orElseThrow().brightness());
        assertEquals("dark", dark.theme().orElseThrow().definitionId());
        assertEquals(64,
                inherited.theme().orElseThrow().digestIdentity().length());
    }

    @Test
    void disabledDescriptorUsesLegacyMaterialWithoutLosingPreviewBrightnessControls() {
        FlutterProjectTheme disabled = new FlutterProjectTheme(
                false,
                THEME.defaultMode(),
                THEME.lightThemeId(),
                THEME.darkThemeId(),
                THEME.themes(),
                THEME.generated());
        FlutterDesignerProjectThemeResolver resolver = resolver(
                valid(disabled), CanvasThemeBrightness.DARK);

        var inherited = resolver.resolve(PROJECT, Optional.empty());
        var system = resolver.resolve(PROJECT, Optional.of(DesignerThemeMode.SYSTEM));
        var dark = resolver.resolve(PROJECT, Optional.of(DesignerThemeMode.DARK));

        assertTrue(inherited.available());
        assertTrue(inherited.legacyFallback());
        assertEquals(CanvasThemeBrightness.LIGHT,
                inherited.theme().orElseThrow().brightness());
        assertEquals(CanvasThemeBrightness.DARK,
                system.theme().orElseThrow().brightness());
        assertEquals(CanvasThemeBrightness.DARK,
                dark.theme().orElseThrow().brightness());
        assertEquals(0xFF6750A4, dark.theme().orElseThrow().seedArgb());
        assertTrue(inherited.detail().contains("Project themes are disabled"));
    }

    @Test
    void globallyDisabledDescriptorMayRetainDisabledReferencesAndStillUsesFallback() {
        FlutterProjectTheme disabled = new FlutterProjectTheme(
                false,
                THEME.defaultMode(),
                THEME.lightThemeId(),
                THEME.darkThemeId(),
                List.of(
                        disabledDefinition(THEME.lightTheme()),
                        disabledDefinition(THEME.darkTheme())),
                THEME.generated());
        FlutterDesignerProjectThemeResolver resolver = resolver(
                valid(disabled), CanvasThemeBrightness.DARK);

        var result = resolver.resolve(
                PROJECT, Optional.of(DesignerThemeMode.DARK));

        assertTrue(result.available());
        assertTrue(result.legacyFallback());
        assertEquals(CanvasThemeBrightness.DARK,
                result.theme().orElseThrow().brightness());
        assertTrue(result.detail().contains("Project themes are disabled"));
    }

    @Test
    void everyPresentButUnverifiedThemeStateFailsClosedWithItsConcreteCause() {
        for (FlutterProjectThemeLoadStatus status : new FlutterProjectThemeLoadStatus[]{
                FlutterProjectThemeLoadStatus.INVALID_DESCRIPTOR,
                FlutterProjectThemeLoadStatus.GENERATED_DART_MISSING,
                FlutterProjectThemeLoadStatus.GENERATED_DART_HASH_MISMATCH,
                FlutterProjectThemeLoadStatus.IO_ERROR}) {
            Optional<FlutterProjectTheme> parsed = status
                    == FlutterProjectThemeLoadStatus.GENERATED_DART_MISSING
                    || status == FlutterProjectThemeLoadStatus.GENERATED_DART_HASH_MISMATCH
                    ? Optional.of(THEME)
                    : Optional.empty();
            String detail = "Concrete failure for " + status + ".";
            FlutterDesignerProjectThemeResolver resolver = resolver(
                    new FlutterProjectThemeLoadResult(status, parsed, detail),
                    CanvasThemeBrightness.LIGHT);

            var result = resolver.resolve(PROJECT, Optional.empty());

            assertFalse(result.available(), status.toString());
            assertEquals(detail, result.detail(), status.toString());
        }
    }

    @Test
    void liveSequenceRendersValidWithdrawsInvalidAndRendersRestoredRevision() {
        FlutterProjectTheme firstTheme = FlutterProjectTheme.defaultTheme("A".repeat(64));
        FlutterProjectTheme restoredTheme = FlutterProjectTheme.defaultTheme("B".repeat(64));
        AtomicReference<FlutterProjectThemeLoadResult> current =
                new AtomicReference<>(valid(firstTheme));
        FlutterDesignerProjectThemeResolver resolver =
                new FlutterDesignerProjectThemeResolver(
                        ignored -> current.get(),
                        new FlutterProjectThemeCodec(),
                        () -> CanvasThemeBrightness.LIGHT);

        var first = resolver.resolve(PROJECT, Optional.of(DesignerThemeMode.LIGHT));
        current.set(new FlutterProjectThemeLoadResult(
                FlutterProjectThemeLoadStatus.GENERATED_DART_HASH_MISMATCH,
                Optional.of(firstTheme),
                "Generated Dart hash mismatch."));
        var invalid = resolver.resolve(PROJECT, Optional.of(DesignerThemeMode.LIGHT));
        current.set(valid(restoredTheme));
        var restored = resolver.resolve(PROJECT, Optional.of(DesignerThemeMode.LIGHT));

        assertTrue(first.available());
        assertFalse(invalid.available());
        assertEquals("Generated Dart hash mismatch.", invalid.detail());
        assertTrue(restored.available());
        assertFalse(first.theme().orElseThrow().digestIdentity().equals(
                restored.theme().orElseThrow().digestIdentity()));
    }

    @Test
    void resolvesTypedColorAndTextRoleOverridesIntoTheCanvasDigestRevision() {
        FlutterThemeTextStyleOverride body = new FlutterThemeTextStyleOverride(
                Optional.of(new FlutterThemeColorValue.ColorRole(
                        FlutterMaterialColorRole.ON_SURFACE)),
                Optional.empty(), Optional.of(18.0d), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty());
        FlutterThemeOverrides overrides = new FlutterThemeOverrides(
                Map.of(FlutterMaterialColorRole.PRIMARY, 0xFF123456),
                Map.of(FlutterMaterialTextStyleRole.BODY_MEDIUM, body));
        FlutterProjectTheme customized = new FlutterProjectTheme(
                true, THEME.defaultMode(), "light", "dark",
                List.of(new FlutterProjectThemeDefinition(
                        "light", "Light", THEME.lightTheme().brightness(),
                        THEME.lightTheme().seedArgb(), true, overrides),
                        THEME.darkTheme()), THEME.generated());

        var result = resolver(valid(customized), CanvasThemeBrightness.LIGHT)
                .resolve(PROJECT, Optional.of(DesignerThemeMode.LIGHT));

        var canvas = result.theme().orElseThrow();
        assertEquals(0xFF123456, canvas.colorSchemeOverrides().get("primary"));
        assertEquals(18.0d,
                canvas.textThemeOverrides().get("bodyMedium").fontSize().orElseThrow());
        assertEquals("onSurface", ((dev.flutter.netbeans.designer.canvas.CanvasThemeColorValue
                .ColorRole) canvas.textThemeOverrides().get("bodyMedium")
                .color().orElseThrow()).role());
        assertEquals(64, canvas.digestIdentity().length());
    }

    private static FlutterProjectThemeLoadResult valid() {
        return valid(THEME);
    }

    private static FlutterProjectThemeLoadResult valid(FlutterProjectTheme theme) {
        return new FlutterProjectThemeLoadResult(
                FlutterProjectThemeLoadStatus.VALID,
                Optional.of(theme),
                "Project theme is valid.");
    }

    private static FlutterDesignerProjectThemeResolver resolver(
            FlutterProjectThemeLoadResult result,
            CanvasThemeBrightness systemBrightness) {
        return new FlutterDesignerProjectThemeResolver(
                ignored -> result,
                new FlutterProjectThemeCodec(),
                () -> systemBrightness);
    }

    private static FlutterProjectThemeDefinition disabledDefinition(
            FlutterProjectThemeDefinition source) {
        return new FlutterProjectThemeDefinition(
                source.id(),
                source.displayName(),
                source.brightness(),
                source.seedArgb(),
                false);
    }
}
