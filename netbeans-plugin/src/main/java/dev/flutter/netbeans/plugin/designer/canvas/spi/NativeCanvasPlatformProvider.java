package dev.flutter.netbeans.plugin.designer.canvas.spi;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Provider for one operating system's native Canvas host implementation.
 *
 * <p>A provider may be activated only when it owns a complete immutable runner
 * contract as well as the native host and launch boundary. Unsupported bundled
 * placeholders deliberately expose no runner contract.</p>
 */
public interface NativeCanvasPlatformProvider {
    NativeCanvasPlatform platform();

    /**
     * Identifies a bundled implementation that may be replaced by one
     * independently registered provider for the same platform.
     *
     * <p>Extension providers keep the default {@code false}. The selector uses
     * a bundled provider only when no extension provider is present, and fails
     * closed instead of choosing arbitrarily when more than one extension is
     * discovered.</p>
     */
    default boolean isFallbackProvider() {
        return false;
    }

    boolean isSupported();

    /** Concrete user-facing support or unavailability reason. */
    String availabilityReason();

    NativeCanvasHost createHost();

    /** Complete build/cache/runtime artifact contract for a supported provider. */
    default Optional<NativeCanvasRunnerContract> runnerContract() {
        return Optional.empty();
    }

    /** Human-readable target used in concrete lifecycle diagnostics. */
    default String targetDescription() {
        return platform().name().toLowerCase(java.util.Locale.ROOT)
                + " native Canvas surface";
    }

    /**
     * Builds the exact isolated-runner command for this platform's opaque
     * parent handle. Providers own handle validation and argument spelling.
     */
    default NativeCanvasRunnerLaunch createLaunch(
            Path executable,
            NativeCanvasParentHandle parentHandle,
            long hostProcessId,
            long surfaceEpoch,
            String nonce) {
        throw new UnsupportedOperationException(availabilityReason());
    }
}
