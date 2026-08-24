package dev.flutter.netbeans.plugin.tooling.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.modules.gsf.testrunner.api.CoreManager;
import org.netbeans.modules.gsf.testrunner.api.Report;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.TestSuite;

class NetBeansFlutterTestResultsSinkTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void delegatesOnlyThroughThePublicCoreManagerContract() throws Exception {
        RecordingCoreManager manager = new RecordingCoreManager();
        NetBeansFlutterTestResultsSink sink = new NetBeansFlutterTestResultsSink(manager);
        TestProject project = TestProject.create(temporaryDirectory.resolve("project"));
        TestSession session = new TestSession(
                "Flutter tests", project, TestSession.SessionType.TEST);
        Report report = new Report("suite", project);

        sink.start(session);
        sink.output(session, "progress", false);
        sink.report(session, report, true);
        sink.finish(session);

        assertSame(session, manager.started);
        assertSame(session, manager.outputSession);
        assertEquals("progress", manager.outputText);
        assertSame(report, manager.report);
        assertEquals(true, manager.reportCompleted);
        assertSame(session, manager.finished);
    }

    @Test
    void fallsBackToANamedOutputTabWithReasonAndReportTotals() throws Exception {
        RecordingFallback fallback = new RecordingFallback();
        NetBeansFlutterTestResultsSink sink = new NetBeansFlutterTestResultsSink(fallback);
        TestProject project = TestProject.create(temporaryDirectory.resolve("fallback-project"));
        TestSession session = new TestSession(
                "Widget tests", project, TestSession.SessionType.TEST);
        Report report = new Report("test/widget_test.dart [vm]", project);
        report.setTotalTests(3);
        report.setPassed(1);
        report.setFailures(1);
        report.setSkipped(1);

        sink.start(session);
        sink.output(session, "runner output", false);
        sink.report(session, report, true);
        sink.finish(session);

        assertEquals("Flutter Tests - Widget tests", fallback.tabName);
        assertTrue(fallback.reason.contains("Test Results service is unavailable"));
        assertTrue(fallback.lines.stream().anyMatch(line ->
                line.text().equals("runner output") && !line.error()));
        assertTrue(fallback.lines.stream().anyMatch(line ->
                line.text().contains("status=FAILED")
                && line.text().contains("total=3")
                && line.text().contains("passed=1")
                && line.text().contains("failed=1")
                && line.text().contains("skipped=1")
                && line.error()));
        assertEquals("Flutter test session finished: Widget tests", fallback.finished);
    }

    private static final class RecordingCoreManager extends CoreManager {
        private TestSession started;
        private TestSession outputSession;
        private String outputText;
        private Report report;
        private boolean reportCompleted;
        private TestSession finished;

        @Override
        public void testStarted(TestSession session) {
            started = session;
        }

        @Override
        public void sessionFinished(TestSession session) {
            finished = session;
        }

        @Override
        public void displayReport(TestSession session, Report report) {
            this.report = report;
        }

        @Override
        public void displayReport(TestSession session, Report report, boolean completed) {
            this.report = report;
            reportCompleted = completed;
        }

        @Override
        public void displayOutput(TestSession session, String text, boolean error) {
            outputSession = session;
            outputText = text;
        }

        @Override
        public void displaySuiteRunning(TestSession session, TestSuite suite) {
        }

        @Override
        public void displaySuiteRunning(TestSession session, String suiteName) {
        }
    }

    private static final class RecordingFallback
            implements NetBeansFlutterTestResultsSink.FallbackOutput {
        private String tabName;
        private String reason;
        private String finished;
        private final List<Line> lines = new ArrayList<>();

        @Override
        public void start(String tabName, String reason) {
            this.tabName = tabName;
            this.reason = reason;
        }

        @Override
        public void line(String text, boolean error) {
            lines.add(new Line(text, error));
        }

        @Override
        public void finish(String message) {
            finished = message;
        }
    }

    private record Line(String text, boolean error) {
    }
}
