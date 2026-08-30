package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasHost;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasParentHandle;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatform;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatformProvider;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasRunnerContract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.openide.util.Lookup;
import org.junit.jupiter.api.Test;

class NativeCanvasPlatformProvidersTest {
    private static final String NONCE = "ab".repeat(32);

    @Test
    void netBeansLookupDiscoversEachBundledPlatformProviderExactlyOnce() {
        var discovered = Lookup.getDefault().lookupAll(
                NativeCanvasPlatformProvider.class);

        assertEquals(1, discovered.stream()
                .filter(WindowsNativeCanvasPlatformProvider.class::isInstance)
                .count());
        assertEquals(1, discovered.stream()
                .filter(LinuxNativeCanvasPlatformProvider.class::isInstance)
                .count());
        assertEquals(1, discovered.stream()
                .filter(MacOsNativeCanvasPlatformProvider.class::isInstance)
                .count());
    }

    @Test
    void detectsSupportedWindowsProviderWithoutLoadingAnotherPlatform() {
        NativeCanvasPlatformProvider provider =
                NativeCanvasPlatformProviders.select("Windows 11");

        assertInstanceOf(WindowsNativeCanvasPlatformProvider.class, provider);
        assertEquals(NativeCanvasPlatform.WINDOWS, provider.platform());
        assertTrue(provider.isSupported());
        assertTrue(provider.availabilityReason().contains("available"));
    }

    @Test
    void linuxAndMacProvidersFailClosedWithConcreteNativeReasons() {
        NativeCanvasPlatformProvider linux =
                NativeCanvasPlatformProviders.select("Linux");
        NativeCanvasPlatformProvider mac =
                NativeCanvasPlatformProviders.select("Mac OS X");

        assertInstanceOf(LinuxNativeCanvasPlatformProvider.class, linux);
        assertInstanceOf(MacOsNativeCanvasPlatformProvider.class, mac);
        assertUnsupportedWithoutImageFallback(linux, "X11/Wayland");
        assertUnsupportedWithoutImageFallback(mac, "NSView");
    }

    @Test
    void unknownOperatingSystemFailsClosedWithoutInventingAProvider() {
        NativeCanvasPlatformProvider provider =
                NativeCanvasPlatformProviders.select("Plan 9");

        assertEquals(NativeCanvasPlatform.UNKNOWN, provider.platform());
        assertFalse(provider.isSupported());
        assertTrue(provider.availabilityReason().contains("Plan 9"));
        assertTrue(provider.availabilityReason().contains("image transfer is forbidden"));
        UnsupportedOperationException failure = assertThrows(
                UnsupportedOperationException.class,
                provider::createHost);
        assertEquals(provider.availabilityReason(), failure.getMessage());
    }

    @Test
    void oneLookupExtensionReplacesTheBundledFallbackDeterministically() {
        NativeCanvasPlatformProvider extension = new PrimaryWindowsExtension();

        NativeCanvasPlatformProvider selected = NativeCanvasPlatformProviders.select(
                "Windows 11",
                List.of(new WindowsNativeCanvasPlatformProvider(), extension));

        assertSame(extension, selected);
    }

    @Test
    void supportedExtensionWithoutACompleteRunnerContractFailsClosed() {
        NativeCanvasPlatformProvider extension = new SupportedLinuxExtension();

        NativeCanvasPlatformProvider selected = NativeCanvasPlatformProviders.select(
                "Linux",
                List.of(new LinuxNativeCanvasPlatformProvider(), extension));

        assertFalse(selected.isSupported());
        assertTrue(selected.availabilityReason().contains("incomplete"));
        assertTrue(selected.availabilityReason().contains("no runner contract"));
        assertTrue(selected.availabilityReason().contains("image transfer is forbidden"));
    }

    @Test
    void multipleLookupExtensionsFailClosedInsteadOfUsingIterationOrder() {
        NativeCanvasPlatformProvider selected = NativeCanvasPlatformProviders.select(
                "Windows 11",
                List.of(
                        new WindowsNativeCanvasPlatformProvider(),
                        new SecondaryWindowsExtension(),
                        new PrimaryWindowsExtension()));

        assertEquals(NativeCanvasPlatform.WINDOWS, selected.platform());
        assertFalse(selected.isSupported());
        assertTrue(selected.availabilityReason().contains("ambiguous"));
        assertTrue(selected.availabilityReason().contains("2 extension providers"));
        assertTrue(selected.availabilityReason().contains(
                PrimaryWindowsExtension.class.getName()));
        assertTrue(selected.availabilityReason().contains(
                SecondaryWindowsExtension.class.getName()));
        UnsupportedOperationException failure = assertThrows(
                UnsupportedOperationException.class,
                selected::createHost);
        assertEquals(selected.availabilityReason(), failure.getMessage());
    }

    @Test
    void supportedProviderWithForeignRunnerContractFailsClosed() {
        NativeCanvasPlatformProvider selected = NativeCanvasPlatformProviders.select(
                "Windows 11", List.of(new MismatchedWindowsExtension()));

        assertFalse(selected.isSupported());
        assertTrue(selected.availabilityReason().contains("incomplete"));
        assertTrue(selected.availabilityReason().contains("targets linux"));
    }

    @Test
    void emptyLookupUsesTheDeterministicBundledFallback() {
        assertInstanceOf(
                WindowsNativeCanvasPlatformProvider.class,
                NativeCanvasPlatformProviders.select("Windows 11", List.of()));
        assertInstanceOf(
                LinuxNativeCanvasPlatformProvider.class,
                NativeCanvasPlatformProviders.select("Linux", List.of()));
        assertInstanceOf(
                MacOsNativeCanvasPlatformProvider.class,
                NativeCanvasPlatformProviders.select("Mac OS X", List.of()));
    }

    @Test
    void windowsProviderOwnsTheExactOpaqueLaunchBoundary() {
        NativeCanvasPlatformProvider provider =
                NativeCanvasPlatformProviders.select("Windows 11");
        Path executable = Path.of("target", "canvas runner",
                "netbeans_flutter_canvas_runner.exe")
                .toAbsolutePath();

        assertEquals(List.of(
                executable.normalize().toString(),
                "--netbeans-parent-hwnd=0x0000000000000771",
                "--netbeans-host-pid=4321",
                "--netbeans-surface-epoch=7",
                "--netbeans-session-nonce=" + NONCE),
                provider.createLaunch(
                        executable,
                        new NativeCanvasParentHandle(
                                NativeCanvasPlatform.WINDOWS,
                                "0x0000000000000771"),
                        4321L,
                        7L,
                        NONCE).command());
    }

    @Test
    void windowsProviderRejectsForeignAndMalformedOpaqueHandles() {
        NativeCanvasPlatformProvider provider =
                new WindowsNativeCanvasPlatformProvider();
        Path executable = Path.of(
                "netbeans_flutter_canvas_runner.exe").toAbsolutePath();

        assertThrows(IllegalArgumentException.class, () -> provider.createLaunch(
                executable,
                new NativeCanvasParentHandle(
                        NativeCanvasPlatform.LINUX,
                        "0x0000000000000771"),
                4321L,
                7L,
                NONCE));
        assertThrows(IllegalArgumentException.class, () -> provider.createLaunch(
                executable,
                new NativeCanvasParentHandle(
                        NativeCanvasPlatform.WINDOWS,
                        "0x771"),
                4321L,
                7L,
                NONCE));
    }

    private static void assertUnsupportedWithoutImageFallback(
            NativeCanvasPlatformProvider provider,
            String nativeTechnology) {
        assertFalse(provider.isSupported());
        assertTrue(provider.availabilityReason().contains(nativeTechnology));
        assertTrue(provider.availabilityReason().contains("image transfer is forbidden"));
        UnsupportedOperationException failure = assertThrows(
                UnsupportedOperationException.class,
                provider::createHost);
        assertEquals(provider.availabilityReason(), failure.getMessage());
    }

    private static final class PrimaryWindowsExtension
            implements NativeCanvasPlatformProvider {
        @Override
        public NativeCanvasPlatform platform() {
            return NativeCanvasPlatform.WINDOWS;
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public String availabilityReason() {
            return "Primary test extension is available.";
        }

        @Override
        public NativeCanvasHost createHost() {
            throw new UnsupportedOperationException("unused test host");
        }

        @Override
        public Optional<NativeCanvasRunnerContract> runnerContract() {
            return new WindowsNativeCanvasPlatformProvider().runnerContract();
        }
    }

    private static final class SecondaryWindowsExtension
            implements NativeCanvasPlatformProvider {
        @Override
        public NativeCanvasPlatform platform() {
            return NativeCanvasPlatform.WINDOWS;
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public String availabilityReason() {
            return "Secondary test extension is available.";
        }

        @Override
        public NativeCanvasHost createHost() {
            throw new UnsupportedOperationException("unused test host");
        }

        @Override
        public Optional<NativeCanvasRunnerContract> runnerContract() {
            return new WindowsNativeCanvasPlatformProvider().runnerContract();
        }
    }

    private static final class SupportedLinuxExtension
            implements NativeCanvasPlatformProvider {
        @Override
        public NativeCanvasPlatform platform() {
            return NativeCanvasPlatform.LINUX;
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public String availabilityReason() {
            return "Test Linux provider claims support.";
        }

        @Override
        public NativeCanvasHost createHost() {
            throw new UnsupportedOperationException("must never be activated");
        }
    }

    private static final class MismatchedWindowsExtension
            implements NativeCanvasPlatformProvider {
        @Override
        public NativeCanvasPlatform platform() {
            return NativeCanvasPlatform.WINDOWS;
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public String availabilityReason() {
            return "Mismatched test extension is available.";
        }

        @Override
        public NativeCanvasHost createHost() {
            throw new UnsupportedOperationException("unused test host");
        }

        @Override
        public Optional<NativeCanvasRunnerContract> runnerContract() {
            NativeCanvasRunnerContract windows =
                    new WindowsNativeCanvasPlatformProvider()
                            .runnerContract().orElseThrow();
            return Optional.of(new NativeCanvasRunnerContract(
                    NativeCanvasPlatform.LINUX,
                    windows.buildTarget(),
                    windows.runtimeLayout(),
                    windows.cachePolicy()));
        }
    }
}
