package io.github.vgrytsenko2022.plugin.tooling.test;

import io.github.vgrytsenko2022.plugin.tooling.DartSourceLocation;
import io.github.vgrytsenko2022.plugin.tooling.FlutterTestSessionBridge;
import io.github.vgrytsenko2022.run.FlutterTestCase;
import io.github.vgrytsenko2022.run.FlutterTestCaseState;
import io.github.vgrytsenko2022.run.FlutterTestError;
import io.github.vgrytsenko2022.run.FlutterTestEvent;
import io.github.vgrytsenko2022.run.FlutterTestJsonParser;
import io.github.vgrytsenko2022.run.FlutterTestMessage;
import io.github.vgrytsenko2022.run.FlutterTestOutcome;
import io.github.vgrytsenko2022.run.FlutterTestRunModel;
import io.github.vgrytsenko2022.run.FlutterTestSuite;
import io.github.vgrytsenko2022.run.FlutterToolCommand;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.netbeans.api.project.Project;
import org.netbeans.modules.gsf.testrunner.api.Report;
import org.netbeans.modules.gsf.testrunner.api.Status;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.TestSuite;
import org.netbeans.modules.gsf.testrunner.api.Trouble;

/** Streams one Flutter JSON test run into the standard NetBeans Test Results UI. */
public final class NetBeansFlutterTestSession implements FlutterTestSessionBridge {
    private final TestSession session;
    private final FlutterTestJsonParser parser = new FlutterTestJsonParser();
    private final FlutterTestRunModel model = new FlutterTestRunModel();
    private final DartSourceLocationResolver resolver;
    private final FlutterTestResultsSink sink;
    private final FlutterTestRerunHandler rerunHandler;

    private boolean finished;
    private IOException lastParseFailure;

    public NetBeansFlutterTestSession(
            Project project,
            Path projectRoot,
            String displayName,
            FlutterToolCommand originalCommand,
            Consumer<FlutterToolCommand> rerun) {
        this(
                project,
                projectRoot,
                displayName,
                originalCommand,
                rerun,
                new NetBeansFlutterTestResultsSink());
    }

    NetBeansFlutterTestSession(
            Project project,
            Path projectRoot,
            String displayName,
            FlutterToolCommand originalCommand,
            Consumer<FlutterToolCommand> rerun,
            FlutterTestResultsSink sink) {
        Objects.requireNonNull(project, "project");
        this.resolver = new DartSourceLocationResolver(projectRoot);
        this.sink = Objects.requireNonNull(sink, "sink");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("A Flutter test session name is required");
        }
        Objects.requireNonNull(rerun, "rerun");
        rerunHandler = new FlutterTestRerunHandler(
                originalCommand,
                request -> dispatchRerun(request, originalCommand, rerun));
        session = new TestSession(displayName.strip(), project, TestSession.SessionType.TEST);
        session.setStartingMsg("Running " + String.join(" ", originalCommand.arguments()));
        session.setRerunHandler(rerunHandler);
        sink.start(session);
    }

    @Override
    public synchronized void standardOutput(String line) {
        if (finished || line == null || line.isBlank()) {
            return;
        }
        try {
            Optional<FlutterTestEvent> parsed = parser.parseLine(line);
            if (parsed.isEmpty()) {
                return;
            }
            FlutterTestEvent event = parsed.get();
            model.accept(event);
            if (event instanceof FlutterTestEvent.PrintEvent printEvent) {
                sink.output(session, printEvent.message(), false);
            }
        } catch (IOException ex) {
            lastParseFailure = ex;
            // Flutter may print tool setup text before the machine stream begins.
            sink.output(session, line, false);
        }
    }

    @Override
    public synchronized void standardError(String line) {
        if (!finished && line != null && !line.isBlank()) {
            sink.output(session, line, true);
        }
    }

    @Override
    public synchronized void finish(
            int exitCode,
            boolean cancelled,
            Throwable failure) {
        if (finished) {
            return;
        }
        finished = true;

        try {
            String abortReason = abortReason(exitCode, cancelled, failure);
            boolean aborted = abortReason != null;
            if (aborted) {
                sink.output(session, abortReason, true);
            }
            publishReports(aborted);
        } finally {
            try {
                sink.finish(session);
            } finally {
                rerunHandler.setEnabled(true);
            }
        }
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    TestSession testSession() {
        return session;
    }

    FlutterTestRunModel model() {
        return model;
    }

    private void publishReports(boolean streamAborted) {
        Map<Long, SuiteOutput> suites = new LinkedHashMap<>();
        for (FlutterTestSuite suite : model.suites()) {
            suites.put(suite.id(), new SuiteOutput(suite));
        }
        for (FlutterTestCaseState state : model.tests()) {
            if (state.test().isEmpty()) {
                continue;
            }
            FlutterTestCase test = state.test().get();
            SuiteOutput suite = suites.computeIfAbsent(
                    test.suiteId(),
                    id -> new SuiteOutput(new FlutterTestSuite(
                            id, "unknown", Optional.empty())));
            suite.states.add(state);
        }

        for (SuiteOutput output : suites.values()) {
            TestSuite suite = new TestSuite(suiteName(output.suite));
            output.states.stream()
                    .sorted(Comparator.comparingLong(FlutterTestCaseState::id))
                    .filter(NetBeansFlutterTestSession::visible)
                    .map(state -> createTestcase(state, output.suite, streamAborted))
                    .forEach(suite::addTestcase);
            session.addSuite(suite);
            Report report = session.getReport(elapsedMillis(output.states));
            sink.report(session, report, true);
            session.finishSuite(suite);
        }
    }

    private FlutterNetBeansTestcase createTestcase(
            FlutterTestCaseState state,
            FlutterTestSuite suite,
            boolean streamAborted) {
        FlutterTestCase test = state.test().orElseThrow();
        Optional<DartSourceLocation> source = resolver.resolve(test, suite);
        FlutterNetBeansTestcase testcase = new FlutterNetBeansTestcase(
                state.id(),
                test.name(),
                session,
                resolver.projectRelativePath(test, suite),
                source);
        testcase.setStatus(status(state, streamAborted));
        testcase.setTimeMillis(state.durationMillis().orElse(0L));

        List<String> output = state.messages().stream()
                .map(FlutterTestMessage::message)
                .flatMap(message -> message.lines())
                .toList();
        if (!output.isEmpty()) {
            testcase.addOutputLines(output);
        }
        Trouble trouble = trouble(state);
        if (trouble != null) {
            testcase.setTrouble(trouble);
        }
        return testcase;
    }

    private static boolean visible(FlutterTestCaseState state) {
        return !state.hidden()
                || !state.errors().isEmpty()
                || state.outcome() == FlutterTestOutcome.FAILURE
                || state.outcome() == FlutterTestOutcome.ERROR;
    }

    private static Status status(FlutterTestCaseState state, boolean streamAborted) {
        if (!state.completed() && streamAborted) {
            return Status.ABORTED;
        }
        return switch (state.outcome()) {
            case SUCCESS -> Status.PASSED;
            case SKIPPED -> Status.SKIPPED;
            case FAILURE -> Status.FAILED;
            case ERROR -> Status.ERROR;
            case RUNNING, UNKNOWN -> Status.ABORTED;
        };
    }

    private static Trouble trouble(FlutterTestCaseState state) {
        Status status = switch (state.outcome()) {
            case FAILURE -> Status.FAILED;
            case ERROR -> Status.ERROR;
            default -> null;
        };
        if (status == null && state.errors().isEmpty()) {
            return null;
        }
        boolean error = status == Status.ERROR
                || state.errors().stream().anyMatch(item -> !item.failure());
        Trouble trouble = new Trouble(error);
        List<String> stack = new ArrayList<>();
        for (FlutterTestError item : state.errors()) {
            stack.add(item.message());
            item.stackTrace().lines()
                    .filter(line -> !line.isBlank())
                    .forEach(stack::add);
        }
        if (stack.isEmpty()) {
            stack.add(error
                    ? "Flutter test ended with an error without details."
                    : "Flutter test failed without details.");
        }
        trouble.setStackTrace(stack.toArray(String[]::new));
        return trouble;
    }

    private static long elapsedMillis(List<FlutterTestCaseState> states) {
        Optional<Long> first = states.stream()
                .flatMap(state -> state.startedAt().stream())
                .min(Long::compareTo);
        Optional<Long> last = states.stream()
                .flatMap(state -> state.completedAt().stream())
                .max(Long::compareTo);
        return first.isPresent() && last.isPresent()
                ? Math.max(0L, last.get() - first.get())
                : 0L;
    }

    private String abortReason(int exitCode, boolean cancelled, Throwable failure) {
        if (failure != null) {
            String detail = failure.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = failure.getClass().getSimpleName();
            }
            return "Flutter test process failed: " + detail;
        }
        if (cancelled) {
            return "Flutter test process was cancelled.";
        }
        if (exitCode != 0) {
            return "Flutter test process exited with code " + exitCode + ".";
        }
        if (!model.done()) {
            String detail = lastParseFailure == null
                    ? ""
                    : " Last parser error: " + lastParseFailure.getMessage();
            return "Flutter test process ended before the JSON done event"
                    + " (exit code " + exitCode + ")." + detail;
        }
        if (model.success().isEmpty()) {
            return "Flutter test JSON done event did not include a completion result.";
        }
        return null;
    }

    private static String suiteName(FlutterTestSuite suite) {
        String name = suite.path().orElse("Flutter suite " + suite.id());
        return suite.platform().equals("unknown")
                ? name
                : name + " [" + suite.platform() + "]";
    }

    private static void dispatchRerun(
            FlutterTestRerunHandler.RerunRequest request,
            FlutterToolCommand original,
            Consumer<FlutterToolCommand> rerun) {
        if (request instanceof FlutterTestRerunHandler.AllRequest all) {
            rerun.accept(all.command());
            return;
        }
        FlutterTestRerunHandler.SelectedRequest selected =
                (FlutterTestRerunHandler.SelectedRequest) request;
        if (selected.targets().size() == 1
                && selected.targets().get(0).testNames().size() == 1) {
            FlutterTestRerunHandler.SelectedTarget target = selected.targets().get(0);
            rerun.accept(FlutterToolCommand.test(
                    target.projectRelativePath(),
                    target.testNames().get(0)));
            return;
        }
        // The public command model currently represents one name filter. A
        // multi-test request safely falls back to the original test scope.
        rerun.accept(original);
    }

    private static final class SuiteOutput {
        private final FlutterTestSuite suite;
        private final List<FlutterTestCaseState> states = new ArrayList<>();

        SuiteOutput(FlutterTestSuite suite) {
            this.suite = suite;
        }
    }
}
