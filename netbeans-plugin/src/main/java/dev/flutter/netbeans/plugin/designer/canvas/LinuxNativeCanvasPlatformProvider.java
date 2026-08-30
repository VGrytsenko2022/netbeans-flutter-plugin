package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasHost;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatform;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatformProvider;
import org.openide.util.lookup.ServiceProvider;

/** Explicit fail-closed placeholder until a real Linux child-surface host exists. */
@ServiceProvider(service = NativeCanvasPlatformProvider.class, position = 200)
public final class LinuxNativeCanvasPlatformProvider
        implements NativeCanvasPlatformProvider {
    private static final String REASON =
            "Linux native Canvas is unavailable: isolated X11/Wayland child-surface "
            + "embedding has not been implemented or verified; image transfer is forbidden.";

    @Override
    public NativeCanvasPlatform platform() {
        return NativeCanvasPlatform.LINUX;
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
