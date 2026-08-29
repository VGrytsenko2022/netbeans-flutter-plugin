package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewProfileResolver;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasViewport;
import dev.flutter.netbeans.project.FlutterProjectPlatform;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Maps configured Flutter project platforms to exact Designer preview targets. */
final class FlutterDesignerPreviewPlatforms {
    private static final List<PreviewTarget> CANONICAL_TARGETS = List.of(
            new PreviewTarget(
                    CanvasPreviewMode.MOBILE,
                    CanvasTargetPlatform.ANDROID,
                    FlutterProjectPlatform.ANDROID,
                    "Android Phone"),
            new PreviewTarget(
                    CanvasPreviewMode.MOBILE,
                    CanvasTargetPlatform.IOS,
                    FlutterProjectPlatform.IOS,
                    "iPhone"),
            new PreviewTarget(
                    CanvasPreviewMode.TABLET,
                    CanvasTargetPlatform.ANDROID,
                    FlutterProjectPlatform.ANDROID,
                    "Android Tablet"),
            new PreviewTarget(
                    CanvasPreviewMode.TABLET,
                    CanvasTargetPlatform.IOS,
                    FlutterProjectPlatform.IOS,
                    "iPad"),
            new PreviewTarget(
                    CanvasPreviewMode.DESKTOP,
                    CanvasTargetPlatform.WINDOWS,
                    FlutterProjectPlatform.WINDOWS,
                    "Windows Desktop"),
            new PreviewTarget(
                    CanvasPreviewMode.DESKTOP,
                    CanvasTargetPlatform.MACOS,
                    FlutterProjectPlatform.MACOS,
                    "macOS Desktop"),
            new PreviewTarget(
                    CanvasPreviewMode.DESKTOP,
                    CanvasTargetPlatform.LINUX,
                    FlutterProjectPlatform.LINUX,
                    "Linux Desktop"),
            new PreviewTarget(
                    CanvasPreviewMode.WEB,
                    CanvasTargetPlatform.WEB,
                    FlutterProjectPlatform.WEB,
                    "Web"));

    private FlutterDesignerPreviewPlatforms() {
    }

    static List<PreviewTarget> allTargets() {
        return CANONICAL_TARGETS;
    }

    static List<PreviewTarget> compatibleTargets(
            Collection<FlutterProjectPlatform> platforms) {
        Objects.requireNonNull(platforms, "platforms");
        for (FlutterProjectPlatform platform : platforms) {
            Objects.requireNonNull(platform, "platform");
        }
        return CANONICAL_TARGETS.stream()
                .filter(target -> platforms.contains(target.projectPlatform()))
                .toList();
    }

    /** Retains an exact target, then its responsive mode, then canonical first. */
    static Optional<PreviewTarget> preferredOrFirst(
            List<PreviewTarget> available,
            PreviewTarget preferred,
            CanvasPreviewMode preferredMode) {
        Objects.requireNonNull(available, "available");
        if (preferred != null && available.contains(preferred)) {
            return Optional.of(preferred);
        }
        CanvasPreviewMode mode = preferredMode != null
                ? preferredMode
                : preferred == null ? null : preferred.mode();
        if (mode != null) {
            Optional<PreviewTarget> sameMode = available.stream()
                    .filter(target -> target.mode() == mode)
                    .findFirst();
            if (sameMode.isPresent()) {
                return sameMode;
            }
        }
        return available.stream().findFirst();
    }

    record PreviewTarget(
            CanvasPreviewMode mode,
            CanvasTargetPlatform targetPlatform,
            FlutterProjectPlatform projectPlatform,
            String displayName) {
        PreviewTarget {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(targetPlatform, "targetPlatform");
            Objects.requireNonNull(projectPlatform, "projectPlatform");
            Objects.requireNonNull(displayName, "displayName");
            if (displayName.isBlank()) {
                throw new IllegalArgumentException("displayName cannot be blank");
            }
        }

        @Override
        public String toString() {
            CanvasViewport viewport =
                    CanvasPreviewProfileResolver.defaultViewport(mode);
            return displayName + " — "
                    + logicalPixels(viewport.logicalWidth()) + "×"
                    + logicalPixels(viewport.logicalHeight());
        }

        private static String logicalPixels(double value) {
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }
    }
}
