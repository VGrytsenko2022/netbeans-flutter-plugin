package io.github.vgrytsenko2022.designer.canvas;

import java.util.Objects;

/**
 * Complete resolved rendering identity for one Flutter Canvas presentation.
 *
 * <p>The responsive development mode and concrete Flutter platform are
 * deliberately separate. Equality therefore identifies the complete rendering
 * environment rather than only a viewport preset.</p>
 */
public record CanvasRenderProfile(
        CanvasPreviewMode previewMode,
        CanvasTargetPlatform targetPlatform,
        CanvasViewport viewport,
        CanvasDevicePixelRatio devicePixelRatio,
        CanvasResolvedTheme theme,
        CanvasLocale locale,
        CanvasTextScaleFactor textScaleFactor,
        CanvasEngineIdentity engineIdentity) {
    public static final int MAX_PHYSICAL_DIMENSION = 4_096;
    public static final long MAX_PHYSICAL_PIXELS = 8_388_608L;
    public static final long MAX_RGBA_BYTES = MAX_PHYSICAL_PIXELS * 4L;

    public CanvasRenderProfile {
        Objects.requireNonNull(previewMode, "previewMode");
        Objects.requireNonNull(targetPlatform, "targetPlatform");
        Objects.requireNonNull(viewport, "viewport");
        Objects.requireNonNull(devicePixelRatio, "devicePixelRatio");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(textScaleFactor, "textScaleFactor");
        Objects.requireNonNull(engineIdentity, "engineIdentity");
        int physicalWidth = physicalDimension(
                viewport.logicalWidth(), devicePixelRatio.value(), "width");
        int physicalHeight = physicalDimension(
                viewport.logicalHeight(), devicePixelRatio.value(), "height");
        long pixels = (long) physicalWidth * physicalHeight;
        if (pixels > MAX_PHYSICAL_PIXELS) {
            throw new IllegalArgumentException(
                    "Canvas physical surface requires " + pixels
                    + " pixels; the safe limit is " + MAX_PHYSICAL_PIXELS);
        }
    }

    public int physicalWidth() {
        return physicalDimension(
                viewport.logicalWidth(), devicePixelRatio.value(), "width");
    }

    public int physicalHeight() {
        return physicalDimension(
                viewport.logicalHeight(), devicePixelRatio.value(), "height");
    }

    public long physicalPixels() {
        return (long) physicalWidth() * physicalHeight();
    }

    public long physicalRgbaBytes() {
        return physicalPixels() * 4L;
    }

    private static int physicalDimension(
            double logicalDimension,
            double pixelRatio,
            String axis) {
        double resolved = Math.ceil(logicalDimension * pixelRatio);
        if (!Double.isFinite(resolved)
                || resolved < 1.0d
                || resolved > MAX_PHYSICAL_DIMENSION) {
            throw new IllegalArgumentException(
                    "Canvas physical " + axis + " must be between 1 and "
                    + MAX_PHYSICAL_DIMENSION + " pixels");
        }
        return (int) resolved;
    }
}
