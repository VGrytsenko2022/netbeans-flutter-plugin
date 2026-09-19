package io.github.vgrytsenko2022.plugin.tooling.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.run.FlutterToolCommand;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.modules.gsf.testrunner.api.Report;
import org.netbeans.modules.gsf.testrunner.api.RerunType;
import org.netbeans.modules.gsf.testrunner.api.Status;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.Testcase;

class NetBeansFlutterTestSessionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void mapsInterleavedJsonEventsToStandardNetBeansReports() throws Exception {
        Path root = projectWithTestFiles();
        RecordingSink sink = new RecordingSink();
        List<FlutterToolCommand> reruns = new ArrayList<>();
        NetBeansFlutterTestSession bridge = bridge(root, sink, reruns);

        line(bridge, "{\"type\":\"suite\",\"time\":1,\"suite\":{"
                + "\"id\":1,\"platform\":\"vm\",\"path\":\"test/a_test.dart\"}}");
        line(bridge, "{\"type\":\"suite\",\"time\":2,\"suite\":{"
                + "\"id\":2,\"platform\":\"chrome\",\"path\":\"test/b_test.dart\"}}");
        line(bridge, start(10, 10, "fails clearly", 1, "test/a_test.dart", 3, 4));
        line(bridge, start(20, 12, "passes", 2, "test/b_test.dart", 5, 2));
        line(bridge, start(30, 11, "skips", 1, "test/a_test.dart", 9, 1));
        line(bridge, "{\"type\":\"print\",\"time\":13,\"testID\":20,"
                + "\"messageType\":\"print\",\"message\":\"working\\n\"}");
        line(bridge, "{\"type\":\"error\",\"time\":14,\"testID\":10,"
                + "\"error\":\"expected true\",\"stackTrace\":\"test/a_test.dart 3:4\","
                + "\"isFailure\":true}");
        line(bridge, done(20, 18, "success", false, false));
        line(bridge, done(30, 19, "success", false, true));
        line(bridge, done(10, 20, "failure", false, false));
        line(bridge, "{\"type\":\"done\",\"time\":21,\"success\":false}");

        bridge.finish(0, false, null);

        assertEquals(1, sink.startCount);
        assertEquals(2, sink.reports.size());
        assertEquals(1, sink.finishCount);
        assertTrue(sink.completedFlags.stream().allMatch(Boolean::booleanValue));
        assertEquals(3, sink.reports.stream().mapToInt(Report::getTotalTests).sum());

        Testcase failed = testcase(sink, "fails clearly");
        assertEquals(Status.FAILED, failed.getStatus());
        assertEquals(10, failed.getTimeMillis());
        assertNotNull(failed.getTrouble());
        assertFalse(failed.getTrouble().isError());
        assertEquals("expected true", failed.getTrouble().getStackTrace()[0]);
        assertTrue(failed.getLocation().replace('\\', '/').endsWith(
                "/test/a_test.dart:3:4"));

        Testcase passed = testcase(sink, "passes");
        assertEquals(Status.PASSED, passed.getStatus());
        assertEquals(6, passed.getTimeMillis());
        assertEquals("working", passed.getOutput().getFirst().getLine());
        assertEquals(Status.SKIPPED, testcase(sink, "skips").getStatus());
        assertTrue(sink.outputs.stream().anyMatch(output ->
                !output.error() && output.text().equals("working\n")));

        assertTrue(bridge.testSession().getRerunHandler().enabled(RerunType.ALL));
        bridge.testSession().getRerunHandler().rerun(Set.of(passed));
        assertEquals(
                FlutterToolCommand.test("test/b_test.dart", "passes"),
                reruns.getFirst());
    }

    @Test
    void hidesSuccessfulProtocolHelpersButSurfacesLateAsyncErrors() throws Exception {
        Path root = projectWithTestFiles();
        RecordingSink sink = new RecordingSink();
        NetBeansFlutterTestSession bridge = bridge(root, sink, new ArrayList<>());
        line(bridge, "{\"type\":\"suite\",\"suite\":{"
                + "\"id\":1,\"platform\":\"vm\",\"path\":\"test/a_test.dart\"}}");
        line(bridge, start(1, 1, "hidden helper", 1, "test/a_test.dart", 1, 1));
        line(bridge, done(1, 2, "success", true, false));
        line(bridge, start(2, 3, "late error", 1, "test/a_test.dart", 2, 1));
        line(bridge, done(2, 4, "success", true, false));
        line(bridge, "{\"type\":\"error\",\"time\":5,\"testID\":2,"
                + "\"error\":\"late async error\",\"stackTrace\":\"test/a_test.dart 2:1\","
                + "\"isFailure\":false}");
        line(bridge, "{\"type\":\"done\",\"time\":6,\"success\":false}");

        bridge.finish(0, false, null);

        assertEquals(1, sink.reports.getFirst().getTotalTests());
        Testcase testcase = testcase(sink, "late error");
        assertEquals(Status.ERROR, testcase.getStatus());
        assertTrue(testcase.getTrouble().isError());
        assertTrue(allTestcases(sink).stream()
                .noneMatch(item -> item.getName().equals("hidden helper")));
    }

    @Test
    void marksIncompleteTestsAbortedAtEofAndFinishesOnlyOnce() throws Exception {
        Path root = projectWithTestFiles();
        RecordingSink sink = new RecordingSink();
        NetBeansFlutterTestSession bridge = bridge(root, sink, new ArrayList<>());
        line(bridge, "{\"type\":\"suite\",\"suite\":{"
                + "\"id\":1,\"platform\":\"vm\",\"path\":\"test/a_test.dart\"}}");
        line(bridge, start(1, 10, "still running", 1, "test/a_test.dart", 1, 1));
        bridge.standardOutput("not-json");

        bridge.finish(0, false, null);
        bridge.finish(0, true, new IllegalStateException("ignored"));
        bridge.standardError("ignored after finish");

        assertTrue(bridge.isFinished());
        assertEquals(1, sink.finishCount);
        assertEquals(Status.ABORTED, testcase(sink, "still running").getStatus());
        assertTrue(sink.outputs.stream().anyMatch(output -> output.error()
                && output.text().contains("ended before the JSON done event")
                && output.text().contains("Last parser error")));
        assertTrue(sink.outputs.stream().noneMatch(output ->
                output.text().equals("ignored after finish")));
        assertTrue(bridge.testSession().getRerunHandler().enabled(RerunType.ALL));
    }

    private NetBeansFlutterTestSession bridge(
            Path root,
            RecordingSink sink,
            List<FlutterToolCommand> reruns) throws Exception {
        return new NetBeansFlutterTestSession(
                TestProject.create(root),
                root,
                "Flutter tests",
                FlutterToolCommand.test(),
                reruns::add,
                sink);
    }

    private Path projectWithTestFiles() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("project"));
        dartFile(root.resolve("test/a_test.dart"));
        dartFile(root.resolve("test/b_test.dart"));
        return root;
    }

    private static void line(NetBeansFlutterTestSession bridge, String json) {
        bridge.standardOutput(json);
    }

    private static String start(
            long id,
            long time,
            String name,
            long suite,
            String url,
            int line,
            int column) {
        return "{\"type\":\"testStart\",\"time\":" + time + ",\"test\":{"
                + "\"id\":" + id + ",\"name\":\"" + name + "\","
                + "\"suiteID\":" + suite + ",\"groupIDs\":[],"
                + "\"url\":\"" + url + "\",\"line\":" + line
                + ",\"column\":" + column + "}}";
    }

    private static String done(
            long id,
            long time,
            String result,
            boolean hidden,
            boolean skipped) {
        return "{\"type\":\"testDone\",\"time\":" + time
                + ",\"testID\":" + id + ",\"result\":\"" + result + "\","
                + "\"hidden\":" + hidden + ",\"skipped\":" + skipped + "}";
    }

    private static Testcase testcase(RecordingSink sink, String name) {
        return allTestcases(sink).stream()
                .filter(testcase -> testcase.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static List<Testcase> allTestcases(RecordingSink sink) {
        return sink.reports.stream()
                .flatMap(report -> report.getTests().stream())
                .toList();
    }

    private static Path dartFile(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, "void main() {}\n");
    }

    private static final class RecordingSink implements FlutterTestResultsSink {
        private int startCount;
        private int finishCount;
        private final List<CapturedOutput> outputs = new ArrayList<>();
        private final List<Report> reports = new ArrayList<>();
        private final List<Boolean> completedFlags = new ArrayList<>();

        @Override
        public void start(TestSession session) {
            startCount++;
        }

        @Override
        public void output(TestSession session, String text, boolean error) {
            outputs.add(new CapturedOutput(text, error));
        }

        @Override
        public void report(TestSession session, Report report, boolean completed) {
            reports.add(report);
            completedFlags.add(completed);
        }

        @Override
        public void finish(TestSession session) {
            finishCount++;
        }
    }

    private record CapturedOutput(String text, boolean error) {
    }
}
