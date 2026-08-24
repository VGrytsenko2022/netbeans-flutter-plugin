package dev.flutter.netbeans.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.flutter.netbeans.api.FlutterDevice;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlutterToolCommandTest {
    @Test
    void exposesExactArgumentsForOneShotCommands() {
        assertCommand(
                FlutterToolCommand.clean(),
                FlutterToolCommandType.CLEAN,
                "clean");
        assertCommand(
                FlutterToolCommand.build(device("windows", "windows-x64")),
                FlutterToolCommandType.BUILD,
                "build", "windows");
        assertCommand(
                FlutterToolCommand.pubGet(),
                FlutterToolCommandType.PUB_GET,
                "pub", "get");
        assertCommand(
                FlutterToolCommand.analyze(),
                FlutterToolCommandType.ANALYZE,
                "analyze", "--no-pub", "--no-congratulate");
        assertCommand(
                FlutterToolCommand.test(),
                FlutterToolCommandType.TEST,
                "test", "--no-pub", "--reporter=json");
        assertCommand(
                FlutterToolCommand.test("test\\widget_test.dart"),
                FlutterToolCommandType.TEST,
                "test", "--no-pub", "--reporter=json", "test/widget_test.dart");
        assertCommand(
                FlutterToolCommand.test("./test/widget_test.dart", "counter increments"),
                FlutterToolCommandType.TEST,
                "test", "--no-pub", "--reporter=json", "test/widget_test.dart",
                "--plain-name", "counter increments");
    }

    @Test
    void mapsDesktopPlatformsToFlutterBuildTargets() {
        assertBuildTarget("windows", device("windows", "windows-x64"));
        assertBuildTarget("windows", device("desktop", "  WINDOWS-ARM64  "));
        assertBuildTarget("linux", device("linux", "linux-x64"));
        assertBuildTarget("linux", device("desktop", "LINUX-arm64"));
        assertBuildTarget("macos", device("macos", "macos-arm64"));
        assertBuildTarget("macos", device("desktop", "darwin-x64"));
    }

    @Test
    void mapsWebPlatformsAndBrowserDeviceIdsToWebBuild() {
        assertBuildTarget("web", device("browser", "web-javascript"));
        assertBuildTarget("web", device("chrome", "unsupported"));
        assertBuildTarget("web", device(" EDGE ", null));
        assertBuildTarget("web", device("WEB-SERVER", ""));
    }

    @Test
    void mapsMobilePlatformsToFlutterBuildTargets() {
        assertBuildTarget("apk", device("emulator-5554", "android-arm64"));
        assertBuildTarget("apk", device("phone", "ANDROID-x86"));
        assertBuildTarget("ios", device("iphone", "ios"));
        assertBuildTarget("ios", device("simulator", "IOS-SIMULATOR"));
    }

    @Test
    void rejectsMissingOrUnsupportedBuildDevicesClearly() {
        IllegalArgumentException missing = assertThrows(
                IllegalArgumentException.class,
                () -> FlutterToolCommand.build(null));
        assertEquals("Flutter build device is required", missing.getMessage());

        IllegalArgumentException blankId = assertThrows(
                IllegalArgumentException.class,
                () -> FlutterToolCommand.build(device(" ", "windows-x64")));
        assertEquals("Flutter build device id is required", blankId.getMessage());

        IllegalArgumentException unsupported = assertThrows(
                IllegalArgumentException.class,
                () -> FlutterToolCommand.build(device("fuchsia-device", "fuchsia-arm64")));
        assertEquals(
                "Unsupported Flutter build device 'fuchsia-device' with platform 'fuchsia-arm64'",
                unsupported.getMessage());

        IllegalArgumentException blankPlatform = assertThrows(
                IllegalArgumentException.class,
                () -> FlutterToolCommand.build(device("custom-device", " ")));
        assertEquals(
                "Unsupported Flutter build device 'custom-device' with platform '<blank>'",
                blankPlatform.getMessage());
    }

    @Test
    void argumentListIsImmutable() {
        List<String> arguments = FlutterToolCommand.test().arguments();

        assertThrows(UnsupportedOperationException.class, () -> arguments.add("--coverage"));
    }

    @Test
    void rejectsPathsOutsideProjectAndMissingNames() {
        assertThrows(IllegalArgumentException.class, () -> FlutterToolCommand.test("../test.dart"));
        assertThrows(IllegalArgumentException.class, () -> FlutterToolCommand.test("C:/tmp/test.dart"));
        assertThrows(IllegalArgumentException.class, () -> FlutterToolCommand.test("/tmp/test.dart"));
        assertThrows(IllegalArgumentException.class, () -> FlutterToolCommand.test("test/a.dart", " "));
    }

    private static void assertCommand(
            FlutterToolCommand command,
            FlutterToolCommandType type,
            String... arguments) {
        assertEquals(type, command.type());
        assertEquals(List.of(arguments), command.arguments());
    }

    private static void assertBuildTarget(String expected, FlutterDevice device) {
        FlutterToolCommand command = FlutterToolCommand.build(device);

        assertNotNull(command);
        assertCommand(command, FlutterToolCommandType.BUILD, "build", expected);
    }

    private static FlutterDevice device(String id, String platform) {
        return new FlutterDevice(id, "Test device", platform, false);
    }
}
