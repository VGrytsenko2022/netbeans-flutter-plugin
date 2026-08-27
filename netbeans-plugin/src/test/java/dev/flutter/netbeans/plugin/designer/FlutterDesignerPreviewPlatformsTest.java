package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerPreviewPlatforms.PreviewTarget;
import dev.flutter.netbeans.project.FlutterProjectPlatform;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FlutterDesignerPreviewPlatformsTest {

    @Test
    void mapsEachProjectPlatformToExactResponsiveAndAdaptiveTargets() {
        assertTargets(FlutterProjectPlatform.ANDROID,
                "MOBILE/ANDROID/Android Phone",
                "TABLET/ANDROID/Android Tablet");
        assertTargets(FlutterProjectPlatform.IOS,
                "MOBILE/IOS/iPhone",
                "TABLET/IOS/iPad");
        assertTargets(FlutterProjectPlatform.WEB,
                "WEB/WEB/Web");
        assertTargets(FlutterProjectPlatform.WINDOWS,
                "DESKTOP/WINDOWS/Windows Desktop");
        assertTargets(FlutterProjectPlatform.MACOS,
                "DESKTOP/MACOS/macOS Desktop");
        assertTargets(FlutterProjectPlatform.LINUX,
                "DESKTOP/LINUX/Linux Desktop");
    }

    @Test
    void keepsAndroidIosAndDesktopTargetsDistinctInCanonicalPreviewOrder() {
        assertEquals(List.of(
                "MOBILE/ANDROID/Android Phone",
                "MOBILE/IOS/iPhone",
                "TABLET/ANDROID/Android Tablet",
                "TABLET/IOS/iPad",
                "DESKTOP/WINDOWS/Windows Desktop",
                "DESKTOP/MACOS/macOS Desktop",
                "DESKTOP/LINUX/Linux Desktop",
                "WEB/WEB/Web"),
                descriptions(FlutterDesignerPreviewPlatforms.compatibleTargets(Set.of(
                            FlutterProjectPlatform.IOS,
                            FlutterProjectPlatform.ANDROID,
                            FlutterProjectPlatform.WINDOWS,
                            FlutterProjectPlatform.MACOS,
                            FlutterProjectPlatform.LINUX,
                            FlutterProjectPlatform.WEB))));
    }

    @Test
    void resolvedProjectWithoutPlatformsHasNoCompatiblePreview() {
        assertTrue(FlutterDesignerPreviewPlatforms.compatibleTargets(Set.of()).isEmpty());
    }

    @Test
    void retainsExactTargetThenSameModeAndOtherwiseFallsBackDeterministically() {
        PreviewTarget windows = target(FlutterProjectPlatform.WINDOWS, 0);
        PreviewTarget macos = target(FlutterProjectPlatform.MACOS, 0);
        PreviewTarget web = target(FlutterProjectPlatform.WEB, 0);
        List<PreviewTarget> macosAndWeb = List.of(macos, web);
        assertEquals(Optional.of(web),
                FlutterDesignerPreviewPlatforms.preferredOrFirst(
                        macosAndWeb, web, CanvasPreviewMode.WEB));
        assertEquals(Optional.of(macos),
                FlutterDesignerPreviewPlatforms.preferredOrFirst(
                        macosAndWeb, windows, CanvasPreviewMode.DESKTOP));
        assertEquals(Optional.of(macos),
                FlutterDesignerPreviewPlatforms.preferredOrFirst(
                        macosAndWeb, null, CanvasPreviewMode.MOBILE));
        assertEquals(Optional.empty(),
                FlutterDesignerPreviewPlatforms.preferredOrFirst(
                        List.of(), windows, CanvasPreviewMode.DESKTOP));
    }

    @Test
    void onlyWebTargetRequiresTheSeparateBrowserBackend() {
        assertFalse(target(FlutterProjectPlatform.WINDOWS, 0).requiresBrowserBackend());
        assertTrue(target(FlutterProjectPlatform.WEB, 0).requiresBrowserBackend());
    }

    private static void assertTargets(
            FlutterProjectPlatform platform,
            String... expected) {
        assertEquals(List.of(expected),
                descriptions(FlutterDesignerPreviewPlatforms.compatibleTargets(
                        Set.of(platform))));
    }

    private static PreviewTarget target(
            FlutterProjectPlatform platform,
            int index) {
        return FlutterDesignerPreviewPlatforms.compatibleTargets(Set.of(platform))
                .get(index);
    }

    private static List<String> descriptions(List<PreviewTarget> targets) {
        return targets.stream()
                .map(target -> target.mode() + "/" + target.targetPlatform()
                        + "/" + target.displayName())
                .toList();
    }
}
