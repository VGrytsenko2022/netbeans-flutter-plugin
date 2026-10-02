package io.github.vgrytsenko2022.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Opt-in physical gate for the packaged JNA/loader/Runtime path. */
@EnabledOnOs(OS.WINDOWS)
@EnabledIfSystemProperty(
        named = "netbeans.flutter.webview2.physical",
        matches = "true")
class JnaWindowsWebView2PhysicalTest {
    @Test
    void loadsPackagedX64AdapterAndProbesInstalledRuntime() throws Exception {
        String version = JnaWindowsWebView2NativeApi.loadPackaged().runtimeVersion();

        assertTrue(version.matches("[0-9]+(?:\\.[0-9]+){2,4}"),
                () -> "unexpected installed WebView2 Runtime version: " + version);
    }
}
