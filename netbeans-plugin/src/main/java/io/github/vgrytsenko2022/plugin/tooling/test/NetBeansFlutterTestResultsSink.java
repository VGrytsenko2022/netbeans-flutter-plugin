package io.github.vgrytsenko2022.plugin.tooling.test;

import java.util.Comparator;
import java.util.Objects;
import org.netbeans.modules.gsf.testrunner.api.CoreManager;
import org.netbeans.modules.gsf.testrunner.api.Report;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.openide.util.Lookup;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/** Production sink backed by the standard NetBeans Test Results window. */
final class NetBeansFlutterTestResultsSink implements FlutterTestResultsSink {
    private final CoreManager manager;
    private final FallbackOutput fallback;

    NetBeansFlutterTestResultsSink() {
        this(findCoreManager(), new IoFallbackOutput());
    }

    NetBeansFlutterTestResultsSink(CoreManager manager) {
        this(Objects.requireNonNull(manager, "manager"), null);
    }

    NetBeansFlutterTestResultsSink(FallbackOutput fallback) {
        this(null, Objects.requireNonNull(fallback, "fallback"));
    }

    private NetBeansFlutterTestResultsSink(
            CoreManager manager,
            FallbackOutput fallback) {
        this.manager = manager;
        this.fallback = manager == null
                ? Objects.requireNonNull(fallback, "fallback")
                : null;
    }

    @Override
    public void start(TestSession session) {
        if (manager != null) {
            manager.testStarted(session);
        } else {
            fallback.start(
                    "Flutter Tests - " + session.getName(),
                    "NetBeans Test Results service is unavailable; "
                    + "Flutter test results are shown in this Output tab.");
        }
    }

    @Override
    public void output(TestSession session, String text, boolean error) {
        if (manager != null) {
            manager.displayOutput(session, text, error);
        } else {
            fallback.line(text, error);
        }
    }

    @Override
    public void report(TestSession session, Report report, boolean completed) {
        if (manager != null) {
            manager.displayReport(session, report, completed);
        } else {
            fallback.line(summary(report), report.getFailures() > 0
                    || report.getErrors() > 0
                    || report.getAborted() > 0);
        }
    }

    @Override
    public void finish(TestSession session) {
        if (manager != null) {
            manager.sessionFinished(session);
        } else {
            fallback.finish("Flutter test session finished: " + session.getName());
        }
    }

    private static CoreManager findCoreManager() {
        return Lookup.getDefault()
                .lookupResult(CoreManager.class)
                .allItems()
                .stream()
                .sorted(Comparator.comparing(item -> Objects.toString(item.getId(), "")))
                .map(Lookup.Item::getInstance)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static String summary(Report report) {
        return "Flutter test suite " + report.getSuiteClassName()
                + ": status=" + report.getStatus()
                + ", total=" + report.getTotalTests()
                + ", passed=" + report.getPassed()
                + ", failed=" + report.getFailures()
                + ", errors=" + report.getErrors()
                + ", skipped=" + report.getSkipped()
                + ", aborted=" + report.getAborted();
    }

    interface FallbackOutput {
        void start(String tabName, String reason);

        void line(String text, boolean error);

        void finish(String message);
    }

    private static final class IoFallbackOutput implements FallbackOutput {
        private InputOutput io;

        @Override
        public void start(String tabName, String reason) {
            io = IOProvider.getDefault().getIO(tabName, false);
            io.select();
            io.getErr().println(reason);
            io.getErr().flush();
        }

        @Override
        public void line(String text, boolean error) {
            if (error) {
                io.getErr().println(text);
                io.getErr().flush();
            } else {
                io.getOut().println(text);
                io.getOut().flush();
            }
        }

        @Override
        public void finish(String message) {
            io.getOut().println(message);
            io.getOut().flush();
            io.getErr().flush();
        }
    }
}
