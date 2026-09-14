package dev.flutter.netbeans.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.flutter.netbeans.designer.model.CanvasOrientation;
import dev.flutter.netbeans.designer.model.CanvasPreferences;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CanvasPreviewProfileResolverTest {
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.44.8", "framework", "engine", "3.12.0");

    @Test
    void resolvesAllCompatibleModesAndExactAdaptiveTargets() {
        assertProfile(CanvasPreviewMode.MOBILE, CanvasTargetPlatform.ANDROID, 390, 844);
        assertProfile(CanvasPreviewMode.MOBILE, CanvasTargetPlatform.IOS, 390, 844);
        assertProfile(CanvasPreviewMode.TABLET, CanvasTargetPlatform.ANDROID, 800, 1280);
        assertProfile(CanvasPreviewMode.TABLET, CanvasTargetPlatform.IOS, 800, 1280);
        assertProfile(CanvasPreviewMode.DESKTOP, CanvasTargetPlatform.WINDOWS, 1280, 800);
        assertProfile(CanvasPreviewMode.DESKTOP, CanvasTargetPlatform.MACOS, 1280, 800);
        assertProfile(CanvasPreviewMode.DESKTOP, CanvasTargetPlatform.LINUX, 1280, 800);
        assertProfile(CanvasPreviewMode.WEB, CanvasTargetPlatform.WEB, 1440, 900);
    }

    @Test
    void exposesCanonicalViewportsForPreviewLabelsWithoutDuplicatingSizes() {
        assertEquals(new CanvasViewport(390, 844),
                CanvasPreviewProfileResolver.defaultViewport(
                        CanvasPreviewMode.MOBILE));
        assertEquals(new CanvasViewport(800, 1280),
                CanvasPreviewProfileResolver.defaultViewport(
                        CanvasPreviewMode.TABLET));
        assertEquals(new CanvasViewport(1280, 800),
                CanvasPreviewProfileResolver.defaultViewport(
                        CanvasPreviewMode.DESKTOP));
        assertEquals(new CanvasViewport(1440, 900),
                CanvasPreviewProfileResolver.defaultViewport(
                        CanvasPreviewMode.WEB));
    }

    @Test
    void appliesSavedViewportOnlyToItsInferredModeAndResolvesTheme() {
        CanvasPreferences preferences = new CanvasPreferences(
                Optional.of("phone"),
                Optional.of(BigDecimal.valueOf(412)),
                Optional.of(BigDecimal.valueOf(915)),
                Optional.of(BigDecimal.valueOf(2)),
                Optional.of(CanvasOrientation.LANDSCAPE),
                Optional.of(DesignerThemeMode.DARK),
                Optional.of(BigDecimal.valueOf(1.25)),
                Optional.of("uk-UA"));

        assertEquals(CanvasPreviewMode.MOBILE,
                CanvasPreviewProfileResolver.initialMode(Optional.of(preferences)));
        CanvasRenderProfile mobile = CanvasPreviewProfileResolver.resolve(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                Optional.of(preferences),
                ENGINE);
        assertEquals(915, mobile.viewport().logicalWidth());
        assertEquals(412, mobile.viewport().logicalHeight());
        assertEquals(2, mobile.devicePixelRatio().value());
        assertEquals(CanvasThemeBrightness.DARK, mobile.theme().brightness());
        assertEquals("uk-UA", mobile.locale().languageTag());
        assertEquals(1.25, mobile.textScaleFactor().value());

        CanvasRenderProfile desktop = CanvasPreviewProfileResolver.resolve(
                CanvasPreviewMode.DESKTOP,
                CanvasTargetPlatform.WINDOWS,
                Optional.of(preferences),
                ENGINE);
        assertEquals(1280, desktop.viewport().logicalWidth());
        assertEquals(800, desktop.viewport().logicalHeight());
        assertEquals(1, desktop.devicePixelRatio().value());
        assertEquals(CanvasThemeBrightness.DARK, desktop.theme().brightness());
    }

    @Test
    void appliesTransientOrientationToMobileAndTabletWithoutPersistedPreferences() {
        CanvasPreferences mobilePreferences = new CanvasPreferences(
                Optional.of("phone"),
                Optional.of(BigDecimal.valueOf(390)),
                Optional.of(BigDecimal.valueOf(844)),
                Optional.empty(),
                Optional.of(CanvasOrientation.PORTRAIT),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        CanvasRenderProfile mobile = CanvasPreviewProfileResolver.resolve(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                Optional.of(mobilePreferences),
                ENGINE,
                CanvasPreviewProfileResolver.legacyTheme(CanvasThemeBrightness.LIGHT),
                Optional.of(CanvasOrientation.LANDSCAPE));
        assertEquals(844, mobile.viewport().logicalWidth());
        assertEquals(390, mobile.viewport().logicalHeight());

        CanvasRenderProfile tablet = CanvasPreviewProfileResolver.resolve(
                CanvasPreviewMode.TABLET,
                CanvasTargetPlatform.ANDROID,
                Optional.of(mobilePreferences),
                ENGINE,
                CanvasPreviewProfileResolver.legacyTheme(CanvasThemeBrightness.LIGHT),
                Optional.of(CanvasOrientation.LANDSCAPE));
        assertEquals(1280, tablet.viewport().logicalWidth());
        assertEquals(800, tablet.viewport().logicalHeight());

        CanvasRenderProfile desktop = CanvasPreviewProfileResolver.resolve(
                CanvasPreviewMode.DESKTOP,
                CanvasTargetPlatform.WINDOWS,
                Optional.empty(),
                ENGINE,
                CanvasPreviewProfileResolver.legacyTheme(CanvasThemeBrightness.LIGHT),
                Optional.of(CanvasOrientation.LANDSCAPE));
        assertEquals(1280, desktop.viewport().logicalWidth());
        assertEquals(800, desktop.viewport().logicalHeight());
    }

    @Test
    void rejectsModeAndTargetPairsThatWouldMisrepresentTheRuntimeFamily() {
        assertThrows(IllegalArgumentException.class,
                () -> CanvasPreviewProfileResolver.resolve(
                        CanvasPreviewMode.MOBILE,
                        CanvasTargetPlatform.WINDOWS,
                        Optional.empty(),
                        ENGINE));
        assertThrows(IllegalArgumentException.class,
                () -> CanvasPreviewProfileResolver.resolve(
                        CanvasPreviewMode.WEB,
                        CanvasTargetPlatform.ANDROID,
                        Optional.empty(),
                        ENGINE));
    }

    @Test
    void preservesTheAlreadyVerifiedProjectThemeWithoutResolvingItAgain() {
        CanvasPreferences preferences = new CanvasPreferences(
                Optional.of("phone"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(DesignerThemeMode.DARK),
                Optional.empty(),
                Optional.empty());
        CanvasResolvedTheme verified = new CanvasResolvedTheme(
                "project_light",
                0xFF123456,
                CanvasThemeBrightness.LIGHT,
                "C".repeat(64));

        CanvasRenderProfile profile = CanvasPreviewProfileResolver.resolve(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                Optional.of(preferences),
                ENGINE,
                verified);

        assertEquals(verified, profile.theme());
    }

    private static void assertProfile(
            CanvasPreviewMode mode,
            CanvasTargetPlatform targetPlatform,
            double width,
            double height) {
        CanvasRenderProfile profile = CanvasPreviewProfileResolver.resolve(
                mode, targetPlatform, Optional.empty(), ENGINE);
        assertEquals(mode, profile.previewMode());
        assertEquals(targetPlatform, profile.targetPlatform());
        assertEquals(width, profile.viewport().logicalWidth());
        assertEquals(height, profile.viewport().logicalHeight());
    }
}
