package io.github.vgrytsenko2022.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WindowsWebView2RuntimeDetectorTest {
    private static final WindowsWebView2RuntimeDetector.PlatformSnapshot WINDOWS_X64 =
            new WindowsWebView2RuntimeDetector.PlatformSnapshot(
                    "Windows 11", "amd64", "64");

    @Test
    void nonWindowsNeverLoadsOrTouchesTheNativeApi() {
        AtomicInteger loads = new AtomicInteger();
        WindowsWebView2RuntimeDetector detector = detector(
                new WindowsWebView2RuntimeDetector.PlatformSnapshot(
                        "Linux", "amd64", "64"),
                () -> {
                    loads.incrementAndGet();
                    throw new AssertionError("native loader must not run");
                });

        WindowsWebView2RuntimeDetector.Availability availability =
                detector.detect();

        assertFalse(availability.available());
        assertEquals("", availability.runtimeVersion());
        assertTrue(availability.reason().contains("unavailable on Linux"));
        assertTrue(availability.reason().contains("Windows x64"));
        assertEquals(0, loads.get());
    }

    @Test
    void unsupportedWindowsJvmArchitectureNeverLoadsNativeCode() {
        AtomicInteger loads = new AtomicInteger();
        WindowsWebView2RuntimeDetector detector = detector(
                new WindowsWebView2RuntimeDetector.PlatformSnapshot(
                        "Windows 11", "aarch64", "64"),
                () -> {
                    loads.incrementAndGet();
                    throw new AssertionError("native loader must not run");
                });

        WindowsWebView2RuntimeDetector.Availability availability =
                detector.detect();

        assertFalse(availability.available());
        assertTrue(availability.reason().contains("64-bit x64"));
        assertTrue(availability.reason().contains("aarch64"));
        assertEquals(0, loads.get());
    }

    @Test
    void validRuntimeIsReportedAndEveryDetectionReprobes() {
        AtomicInteger loads = new AtomicInteger();
        WindowsWebView2RuntimeDetector detector = detector(WINDOWS_X64, () -> {
            int probe = loads.incrementAndGet();
            return api(probe == 1 ? "126.0.2592.113" : "127.0.2651.8");
        });

        WindowsWebView2RuntimeDetector.Availability first = detector.detect();
        WindowsWebView2RuntimeDetector.Availability second = detector.detect();

        assertTrue(first.available());
        assertEquals("126.0.2592.113", first.runtimeVersion());
        assertTrue(first.reason().contains("126.0.2592.113"));
        assertTrue(second.available());
        assertEquals("127.0.2651.8", second.runtimeVersion());
        assertEquals(2, loads.get());
    }

    @Test
    void runtimeMustSupportEveryRequiredStableWebView2Api() {
        for (String version : List.of(
                "99.0.9999.9999",
                "100.0.1185.38",
                "100.0.1184.9999")) {
            WindowsWebView2RuntimeDetector.Availability availability =
                    detector(WINDOWS_X64, () -> api(version)).detect();

            assertFalse(availability.available(), version);
            assertTrue(availability.reason().contains(
                    WindowsWebView2RuntimeDetector.MINIMUM_RUNTIME_VERSION), version);
            assertTrue(availability.reason().contains(version), version);
            assertTrue(availability.reason().contains("Update"), version);
        }

        assertTrue(detector(WINDOWS_X64,
                () -> api(WindowsWebView2RuntimeDetector.MINIMUM_RUNTIME_VERSION))
                .detect().available());
        assertTrue(detector(WINDOWS_X64, () -> api("100.0.1185.040"))
                .detect().available());
    }

    @Test
    void missingRuntimeIsNotStickyAndASecondProbeCanRecover() {
        AtomicInteger loads = new AtomicInteger();
        WindowsWebView2RuntimeDetector detector = detector(WINDOWS_X64, () -> {
            if (loads.incrementAndGet() == 1) {
                throw new IOException("no compatible Runtime was found");
            }
            return api("126.0.2592.113");
        });

        WindowsWebView2RuntimeDetector.Availability missing = detector.detect();
        WindowsWebView2RuntimeDetector.Availability recovered = detector.detect();

        assertFalse(missing.available());
        assertTrue(missing.reason().contains("no compatible Runtime was found"));
        assertTrue(missing.reason().contains("Install or repair"));
        assertTrue(recovered.available());
        assertEquals("126.0.2592.113", recovered.runtimeVersion());
        assertEquals(2, loads.get());
    }

    @Test
    void malformedRuntimeVersionsAreRejectedIndependentlyOfNativeBinding() {
        for (String version : List.of(
                "",
                "1.2",
                "1.2.3-beta",
                "1..3",
                " 1.2.3 ",
                "1.2.3.4.5.6",
                "1".repeat(
                        WindowsWebView2RuntimeDetector
                                .MAXIMUM_RUNTIME_VERSION_CHARACTERS + 1))) {
            WindowsWebView2RuntimeDetector.Availability availability =
                    detector(WINDOWS_X64, () -> api(version)).detect();

            assertFalse(availability.available(), version);
            assertEquals("", availability.runtimeVersion(), version);
            assertTrue(availability.reason().contains("malformed"), version);
            assertTrue(availability.reason().contains("Repair or reinstall"), version);
        }

        WindowsWebView2RuntimeDetector.Availability nullVersion =
                detector(WINDOWS_X64, () -> api(null)).detect();
        assertFalse(nullVersion.available());
        assertTrue(nullVersion.reason().contains("malformed"));
    }

    @Test
    void ioLinkageAndRuntimeFailuresBecomeBoundedActionableResults() {
        List<WindowsWebView2RuntimeDetector.NativeApiLoader> failures = List.of(
                () -> {
                    throw new IOException("runtime probe failed\r\n" + "x".repeat(4_000));
                },
                () -> {
                    throw new UnsatisfiedLinkError("missing adapter dependency\nnext line");
                },
                () -> {
                    throw new IllegalStateException("unexpected native state\r\nnext line");
                });

        for (WindowsWebView2RuntimeDetector.NativeApiLoader failure : failures) {
            WindowsWebView2RuntimeDetector.Availability availability =
                    detector(WINDOWS_X64, failure).detect();

            assertFalse(availability.available());
            assertEquals("", availability.runtimeVersion());
            assertTrue(availability.reason().length()
                    <= WindowsWebView2RuntimeDetector.MAXIMUM_REASON_CHARACTERS);
            assertFalse(availability.reason().contains("\r"));
            assertFalse(availability.reason().contains("\n"));
            assertTrue(availability.reason().contains("retry"));
        }
    }

    @Test
    void runtimeVersionProbeExceptionsAndNullApiAreContained() {
        WindowsWebView2RuntimeDetector.Availability runtimeFailure =
                detector(WINDOWS_X64, () -> apiThrowing(
                        new IOException("version API unavailable"))).detect();
        WindowsWebView2RuntimeDetector.Availability nullApi =
                detector(WINDOWS_X64, () -> null).detect();

        assertFalse(runtimeFailure.available());
        assertTrue(runtimeFailure.reason().contains("version API unavailable"));
        assertFalse(nullApi.available());
        assertTrue(nullApi.reason().contains("native WebView2 API"));
    }

    @Test
    void platformProbeRuntimeFailureIsContainedBeforeNativeLoad() {
        AtomicInteger loads = new AtomicInteger();
        WindowsWebView2RuntimeDetector detector =
                new WindowsWebView2RuntimeDetector(
                        () -> {
                            throw new SecurityException("system properties denied");
                        },
                        () -> {
                            loads.incrementAndGet();
                            return api("126.0.2592.113");
                        });

        WindowsWebView2RuntimeDetector.Availability availability =
                detector.detect();

        assertFalse(availability.available());
        assertTrue(availability.reason().contains("system properties denied"));
        assertTrue(availability.reason().contains("Restart NetBeans"));
        assertEquals(0, loads.get());
    }

    private static WindowsWebView2RuntimeDetector detector(
            WindowsWebView2RuntimeDetector.PlatformSnapshot platform,
            WindowsWebView2RuntimeDetector.NativeApiLoader loader) {
        return new WindowsWebView2RuntimeDetector(() -> platform, loader);
    }

    private static WindowsWebView2NativeApi api(String version) {
        return new StubNativeApi() {
            @Override
            public String runtimeVersion() {
                return version;
            }
        };
    }

    private static WindowsWebView2NativeApi apiThrowing(IOException failure) {
        return new StubNativeApi() {
            @Override
            public String runtimeVersion() throws IOException {
                throw failure;
            }
        };
    }

    private abstract static class StubNativeApi implements WindowsWebView2NativeApi {
        @Override
        public NativeSession create(CreateRequest request, Listener listener)
                throws IOException {
            throw new UnsupportedOperationException("not used by runtime detection");
        }
    }
}
