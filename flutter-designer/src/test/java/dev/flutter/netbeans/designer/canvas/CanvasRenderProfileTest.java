package dev.flutter.netbeans.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CanvasRenderProfileTest {
    private static final CanvasViewport PHONE_VIEWPORT = new CanvasViewport(390.0d, 844.0d);
    private static final CanvasDevicePixelRatio DPR = new CanvasDevicePixelRatio(3.0d);
    private static final CanvasLocale UKRAINIAN = new CanvasLocale("uk-UA");
    private static final CanvasTextScaleFactor TEXT_SCALE = new CanvasTextScaleFactor(1.0d);
    private static final CanvasResolvedTheme THEME = new CanvasResolvedTheme(
            "material_dark_default_v1",
            0xFF6750A4,
            CanvasThemeBrightness.DARK,
            "A".repeat(64));
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.44.8",
            "8505e1763e6f6647b7f94d0be8a0448a2e59f83c",
            "cf56914b326edb0ccb123ffdc60f00060bd513fa",
            "3.12.0");

    @Test
    void identifiesTheCompleteResolvedRenderingEnvironment() {
        CanvasRenderProfile profile = profile(
                CanvasPreviewMode.MOBILE, CanvasTargetPlatform.ANDROID);
        CanvasRenderProfile iosProfile = profile(
                CanvasPreviewMode.MOBILE, CanvasTargetPlatform.IOS);

        assertEquals(CanvasPreviewMode.MOBILE, profile.previewMode());
        assertEquals(CanvasTargetPlatform.ANDROID, profile.targetPlatform());
        assertEquals(PHONE_VIEWPORT, profile.viewport());
        assertEquals(DPR, profile.devicePixelRatio());
        assertEquals(THEME, profile.theme());
        assertEquals(UKRAINIAN, profile.locale());
        assertEquals(TEXT_SCALE, profile.textScaleFactor());
        assertEquals(ENGINE, profile.engineIdentity());
        assertNotEquals(profile, iosProfile);
    }

    @Test
    void keepsDevelopmentModeIndependentFromConcreteRuntimeTarget() {
        CanvasRenderProfile responsiveWebPhone = profile(
                CanvasPreviewMode.MOBILE, CanvasTargetPlatform.WEB);
        CanvasRenderProfile responsiveWebTablet = profile(
                CanvasPreviewMode.TABLET, CanvasTargetPlatform.WEB);
        CanvasRenderProfile nativePhone = profile(
                CanvasPreviewMode.MOBILE, CanvasTargetPlatform.ANDROID);

        assertEquals(CanvasPreviewMode.MOBILE, responsiveWebPhone.previewMode());
        assertEquals(CanvasTargetPlatform.WEB, responsiveWebPhone.targetPlatform());
        assertEquals(CanvasPreviewMode.TABLET, responsiveWebTablet.previewMode());
        assertEquals(CanvasTargetPlatform.WEB, responsiveWebTablet.targetPlatform());
        assertNotEquals(responsiveWebPhone, nativePhone);
    }

    @Test
    void rejectsAMissingIdentityPart() {
        assertThrows(NullPointerException.class, () -> new CanvasRenderProfile(
                null,
                CanvasTargetPlatform.ANDROID,
                PHONE_VIEWPORT,
                DPR,
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE));
        assertThrows(NullPointerException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                null,
                PHONE_VIEWPORT,
                DPR,
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE));
        assertThrows(NullPointerException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                null,
                DPR,
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE));
        assertThrows(NullPointerException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                PHONE_VIEWPORT,
                null,
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE));
        assertThrows(NullPointerException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                PHONE_VIEWPORT,
                DPR,
                null,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE));
        assertThrows(NullPointerException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                PHONE_VIEWPORT,
                DPR,
                THEME,
                null,
                TEXT_SCALE,
                ENGINE));
        assertThrows(NullPointerException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                PHONE_VIEWPORT,
                DPR,
                THEME,
                UKRAINIAN,
                null,
                ENGINE));
        assertThrows(NullPointerException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                PHONE_VIEWPORT,
                DPR,
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                null));
    }

    @Test
    void boundsViewportDevicePixelRatioAndTextScaleToFinitePositiveValues() {
        assertEquals(new CanvasViewport(10_000.0d, 0.25d),
                new CanvasViewport(10_000.0d, 0.25d));
        assertEquals(new CanvasDevicePixelRatio(10.0d),
                new CanvasDevicePixelRatio(10.0d));
        assertEquals(new CanvasTextScaleFactor(5.0d),
                new CanvasTextScaleFactor(5.0d));

        for (double invalid : new double[]{
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                -1.0d, -0.0d, 0.0d, 10_000.0001d}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new CanvasViewport(invalid, 600.0d));
            assertThrows(IllegalArgumentException.class,
                    () -> new CanvasViewport(800.0d, invalid));
        }
        for (double invalid : new double[]{
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                -1.0d, -0.0d, 0.0d, 10.0001d}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new CanvasDevicePixelRatio(invalid));
        }
        for (double invalid : new double[]{
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                -1.0d, -0.0d, 0.0d, 5.0001d}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new CanvasTextScaleFactor(invalid));
        }
    }

    @Test
    void canonicalizesLocaleTagsAndRejectsAmbiguousOrUnboundedValues() {
        assertEquals("uk-UA", new CanvasLocale("uk_ua").languageTag());
        assertEquals("zh-Hant-TW", new CanvasLocale("zh-hant-tw").languageTag());

        assertThrows(NullPointerException.class, () -> new CanvasLocale(null));
        assertThrows(IllegalArgumentException.class, () -> new CanvasLocale(" uk-UA"));
        assertThrows(IllegalArgumentException.class, () -> new CanvasLocale("e"));
        assertThrows(IllegalArgumentException.class, () -> new CanvasLocale("en--US"));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasLocale("en-" + "variant".repeat(9)));
    }

    @Test
    void requiresBoundedPrintableEngineIdentityParts() {
        assertEquals("3.44.8", ENGINE.flutterVersion());
        assertEquals("3.12.0", ENGINE.dartSdkVersion());

        assertThrows(NullPointerException.class, () -> new CanvasEngineIdentity(
                null, "framework", "engine", "dart"));
        assertThrows(IllegalArgumentException.class, () -> new CanvasEngineIdentity(
                "", "framework", "engine", "dart"));
        assertThrows(IllegalArgumentException.class, () -> new CanvasEngineIdentity(
                " flutter", "framework", "engine", "dart"));
        assertThrows(IllegalArgumentException.class, () -> new CanvasEngineIdentity(
                "flutter", "framework\nrevision", "engine", "dart"));
        assertThrows(IllegalArgumentException.class, () -> new CanvasEngineIdentity(
                "flutter", "framework", "e".repeat(129), "dart"));
    }

    @Test
    void themeIdentityDistinguishesCustomThemesWithTheSameBrightness() {
        CanvasResolvedTheme first = new CanvasResolvedTheme(
                "brand_a_v1", 0xFF112233, CanvasThemeBrightness.LIGHT,
                "A".repeat(64));
        CanvasResolvedTheme second = new CanvasResolvedTheme(
                "brand_b_v1", 0xFF112233, CanvasThemeBrightness.LIGHT,
                "B".repeat(64));

        assertNotEquals(first, second);
        assertThrows(NullPointerException.class,
                () -> new CanvasResolvedTheme(
                        null, 0, CanvasThemeBrightness.LIGHT, "A".repeat(64)));
        assertThrows(NullPointerException.class,
                () -> new CanvasResolvedTheme("brand", 0, null, "A".repeat(64)));
        assertThrows(NullPointerException.class,
                () -> new CanvasResolvedTheme(
                        "brand", 0, CanvasThemeBrightness.LIGHT, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasResolvedTheme(
                        " brand", 0, CanvasThemeBrightness.LIGHT, "A".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasResolvedTheme(
                        "x".repeat(129), 0, CanvasThemeBrightness.LIGHT,
                        "A".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasResolvedTheme(
                        "brand", 0, CanvasThemeBrightness.LIGHT,
                        "a".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasResolvedTheme(
                        "brand", 0x7F112233, CanvasThemeBrightness.LIGHT,
                        "A".repeat(64)));
        assertEquals("0xFF112233", first.seedArgbLiteral());
    }

    @Test
    void boundsTheCombinedPhysicalSurfaceBeforeAnyPixelAllocation() {
        CanvasRenderProfile exactBudget = new CanvasRenderProfile(
                CanvasPreviewMode.DESKTOP,
                CanvasTargetPlatform.WINDOWS,
                new CanvasViewport(4_096.0d, 2_048.0d),
                new CanvasDevicePixelRatio(1.0d),
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE);

        assertEquals(4_096, exactBudget.physicalWidth());
        assertEquals(2_048, exactBudget.physicalHeight());
        assertEquals(CanvasRenderProfile.MAX_PHYSICAL_PIXELS,
                exactBudget.physicalPixels());
        assertEquals(CanvasRenderProfile.MAX_RGBA_BYTES,
                exactBudget.physicalRgbaBytes());
        assertThrows(IllegalArgumentException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.DESKTOP,
                CanvasTargetPlatform.WINDOWS,
                new CanvasViewport(4_096.1d, 100.0d),
                new CanvasDevicePixelRatio(1.0d),
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE));
        assertThrows(IllegalArgumentException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.DESKTOP,
                CanvasTargetPlatform.WINDOWS,
                new CanvasViewport(4_096.0d, 4_096.0d),
                new CanvasDevicePixelRatio(1.0d),
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE));
        assertThrows(IllegalArgumentException.class, () -> new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                new CanvasViewport(2_100.0d, 1_000.0d),
                new CanvasDevicePixelRatio(2.0d),
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE));
    }

    private static CanvasRenderProfile profile(
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform) {
        return new CanvasRenderProfile(
                previewMode,
                targetPlatform,
                PHONE_VIEWPORT,
                DPR,
                THEME,
                UKRAINIAN,
                TEXT_SCALE,
                ENGINE);
    }
}
