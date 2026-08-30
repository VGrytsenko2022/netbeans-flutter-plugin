package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasHost;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatform;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatformProvider;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasRunnerContract;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import org.openide.util.Lookup;

/**
 * Discovers native Canvas platform providers through NetBeans Lookup and
 * selects one without depending on Lookup iteration order.
 */
public final class NativeCanvasPlatformProviders {
    private NativeCanvasPlatformProviders() {
    }

    public static NativeCanvasPlatformProvider current() {
        return select(System.getProperty("os.name", ""));
    }

    /**
     * Selects the provider for {@code operatingSystemName} from live NetBeans
     * Lookup. One extension provider replaces the bundled fallback. More than
     * one extension or fallback, or one provider that claims support without a
     * complete matching runner contract, fails closed.
     */
    public static NativeCanvasPlatformProvider select(String operatingSystemName) {
        Collection<? extends NativeCanvasPlatformProvider> discovered;
        try {
            discovered = Lookup.getDefault().lookupAll(
                    NativeCanvasPlatformProvider.class);
        } catch (RuntimeException | LinkageError | ServiceConfigurationError failure) {
            NativeCanvasPlatform platform = NativeCanvasPlatform.detect(
                    operatingSystemName);
            return unavailable(
                    platform,
                    "Native Canvas provider discovery failed for "
                    + targetName(platform, operatingSystemName) + ": "
                    + failure.getClass().getSimpleName()
                    + "; no provider was activated and image transfer is forbidden.");
        }
        return select(operatingSystemName, discovered);
    }

    /** Package-private deterministic seam for selector contract tests. */
    static NativeCanvasPlatformProvider select(
            String operatingSystemName,
            Collection<? extends NativeCanvasPlatformProvider> discovered) {
        NativeCanvasPlatform platform = NativeCanvasPlatform.detect(
                operatingSystemName);
        if (platform == NativeCanvasPlatform.UNKNOWN) {
            return unavailable(
                    platform,
                    "Native Canvas is unavailable for "
                    + targetName(platform, operatingSystemName)
                    + ": no verified native child-surface provider exists; "
                    + "image transfer is forbidden.");
        }
        if (discovered == null) {
            return unavailable(
                    platform,
                    "Native Canvas provider discovery returned null for "
                    + targetName(platform, operatingSystemName)
                    + "; no provider was activated and image transfer is forbidden.");
        }
        List<NativeCanvasPlatformProvider> extensions = new ArrayList<>();
        List<NativeCanvasPlatformProvider> fallbacks = new ArrayList<>();
        try {
            for (NativeCanvasPlatformProvider provider : discovered) {
                if (provider == null) {
                    throw new IllegalStateException(
                            "NetBeans Lookup returned a null native Canvas provider");
                }
                NativeCanvasPlatform candidatePlatform = Objects.requireNonNull(
                        provider.platform(),
                        "Native Canvas provider platform");
                if (candidatePlatform != platform) {
                    continue;
                }
                (provider.isFallbackProvider() ? fallbacks : extensions)
                        .add(provider);
            }
        } catch (RuntimeException | LinkageError | ServiceConfigurationError failure) {
            return unavailable(
                    platform,
                    "Native Canvas provider inspection failed for "
                    + targetName(platform, operatingSystemName) + ": "
                    + failure.getClass().getSimpleName()
                    + "; no provider was activated and image transfer is forbidden.");
        }

        if (extensions.size() == 1) {
            return validateSelected(platform, extensions.get(0));
        }
        if (extensions.size() > 1) {
            return ambiguous(platform, "extension", extensions);
        }
        if (fallbacks.size() == 1) {
            return validateSelected(platform, fallbacks.get(0));
        }
        if (fallbacks.size() > 1) {
            return ambiguous(platform, "fallback", fallbacks);
        }
        return validateSelected(platform, builtInFallback(platform));
    }

    private static NativeCanvasPlatformProvider validateSelected(
            NativeCanvasPlatform platform,
            NativeCanvasPlatformProvider provider) {
        try {
            String reason = provider.availabilityReason();
            if (reason == null || reason.isBlank() || reason.length() > 2_048
                    || reason.indexOf('\0') >= 0 || reason.indexOf('\r') >= 0
                    || reason.indexOf('\n') >= 0) {
                return incomplete(platform, provider,
                        "its availability reason is missing or invalid");
            }
            if (!provider.isSupported()) {
                return provider;
            }
            NativeCanvasRunnerContract contract = provider.runnerContract()
                    .orElse(null);
            if (contract == null) {
                return incomplete(platform, provider,
                        "it claims support but exposes no runner contract");
            }
            if (contract.platform() != platform) {
                return incomplete(platform, provider,
                        "its runner contract targets "
                        + contract.platform().name().toLowerCase(
                                java.util.Locale.ROOT));
            }
            return provider;
        } catch (RuntimeException | LinkageError | ServiceConfigurationError failure) {
            return incomplete(platform, provider,
                    "provider contract inspection failed with "
                    + failure.getClass().getSimpleName());
        }
    }

    private static NativeCanvasPlatformProvider incomplete(
            NativeCanvasPlatform platform,
            NativeCanvasPlatformProvider provider,
            String detail) {
        return unavailable(
                platform,
                "Native Canvas provider contract is incomplete for "
                + platform.name().toLowerCase(java.util.Locale.ROOT) + ": "
                + provider.getClass().getName() + ' ' + detail
                + "; no provider was activated and image transfer is forbidden.");
    }

    private static NativeCanvasPlatformProvider ambiguous(
            NativeCanvasPlatform platform,
            String providerKind,
            List<NativeCanvasPlatformProvider> providers) {
        List<String> providerTypes = providers.stream()
                .map(provider -> provider.getClass().getName())
                .sorted(Comparator.naturalOrder())
                .toList();
        return unavailable(
                platform,
                "Native Canvas provider selection is ambiguous for "
                + platform.name().toLowerCase(java.util.Locale.ROOT) + ": "
                + providers.size() + ' ' + providerKind
                + " providers were discovered " + providerTypes
                + "; no provider was activated and image transfer is forbidden.");
    }

    private static NativeCanvasPlatformProvider builtInFallback(
            NativeCanvasPlatform platform) {
        return switch (platform) {
            case WINDOWS -> new WindowsNativeCanvasPlatformProvider();
            case LINUX -> new LinuxNativeCanvasPlatformProvider();
            case MACOS -> new MacOsNativeCanvasPlatformProvider();
            case UNKNOWN -> throw new IllegalArgumentException(
                    "Unknown platforms do not have a native Canvas fallback");
        };
    }

    private static NativeCanvasPlatformProvider unavailable(
            NativeCanvasPlatform platform,
            String reason) {
        return new UnavailableProvider(platform, reason);
    }

    private static String targetName(
            NativeCanvasPlatform platform,
            String operatingSystemName) {
        String suppliedName = Objects.requireNonNullElse(
                operatingSystemName, "").strip();
        if (!suppliedName.isEmpty()) {
            return suppliedName;
        }
        return platform == NativeCanvasPlatform.UNKNOWN
                ? "unknown operating system"
                : platform.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static final class UnavailableProvider
            implements NativeCanvasPlatformProvider {
        private final NativeCanvasPlatform platform;
        private final String reason;

        private UnavailableProvider(
                NativeCanvasPlatform platform,
                String reason) {
            this.platform = Objects.requireNonNull(platform, "platform");
            this.reason = Objects.requireNonNull(reason, "reason");
        }

        @Override
        public NativeCanvasPlatform platform() {
            return platform;
        }

        @Override
        public boolean isSupported() {
            return false;
        }

        @Override
        public String availabilityReason() {
            return reason;
        }

        @Override
        public NativeCanvasHost createHost() {
            throw new UnsupportedOperationException(reason);
        }
    }
}
