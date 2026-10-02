package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasHost;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasPlatform;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasPlatformProvider;
import org.openide.util.lookup.ServiceProvider;

/** Explicit fail-closed placeholder until a real macOS child-surface host exists. */
@ServiceProvider(service = NativeCanvasPlatformProvider.class, position = 300)
public final class MacOsNativeCanvasPlatformProvider
        implements NativeCanvasPlatformProvider {
    private static final String REASON =
            "macOS native Canvas is unavailable: isolated NSView hosting has not been "
            + "implemented or verified; image transfer is forbidden.";

    @Override
    public NativeCanvasPlatform platform() {
        return NativeCanvasPlatform.MACOS;
    }

    @Override
    public boolean isFallbackProvider() {
        return true;
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public String availabilityReason() {
        return REASON;
    }

    @Override
    public NativeCanvasHost createHost() {
        throw new UnsupportedOperationException(REASON);
    }
}
