package dev.flutter.netbeans.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class FlutterToolCommandTest {
    @Test
    void exposesExactArgumentsForOneShotCommands() {
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
}
