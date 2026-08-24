package dev.flutter.netbeans.run;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * ID-based state accumulated from an asynchronous Flutter test event stream.
 *
 * <p>Events for different tests may be interleaved. Error events may also
 * arrive after a successful {@code testDone}; such an error upgrades the
 * effective result and makes a previously hidden test visible.</p>
 */
public final class FlutterTestRunModel {
    private final Map<Long, FlutterTestSuite> suites = new LinkedHashMap<>();
    private final Map<Long, MutableTest> tests = new LinkedHashMap<>();
    private final List<FlutterTestEvent.UnknownEvent> unknownEvents = new ArrayList<>();
    private boolean done;
    private Optional<Boolean> success = Optional.empty();

    public synchronized void accept(FlutterTestEvent event) {
        Objects.requireNonNull(event, "event");
        if (event instanceof FlutterTestEvent.SuiteEvent suiteEvent) {
            suites.put(suiteEvent.suite().id(), suiteEvent.suite());
            return;
        }
        if (event instanceof FlutterTestEvent.TestStartEvent startEvent) {
            MutableTest state = tests.computeIfAbsent(
                    startEvent.test().id(), MutableTest::new);
            state.test = startEvent.test();
            state.startedAt = Optional.of(startEvent.time());
            return;
        }
        if (event instanceof FlutterTestEvent.PrintEvent printEvent) {
            MutableTest state = tests.computeIfAbsent(printEvent.testId(), MutableTest::new);
            state.messages.add(new FlutterTestMessage(
                    printEvent.time(),
                    printEvent.messageType(),
                    printEvent.message()));
            return;
        }
        if (event instanceof FlutterTestEvent.ErrorEvent errorEvent) {
            MutableTest state = tests.computeIfAbsent(errorEvent.testId(), MutableTest::new);
            state.errors.add(new FlutterTestError(
                    errorEvent.time(),
                    errorEvent.error(),
                    errorEvent.stackTrace(),
                    errorEvent.failure()));
            state.outcome = merge(
                    state.outcome,
                    errorEvent.failure() ? FlutterTestOutcome.FAILURE : FlutterTestOutcome.ERROR);
            // The protocol requires a hidden test to become visible after a late error.
            state.hidden = false;
            return;
        }
        if (event instanceof FlutterTestEvent.TestDoneEvent doneEvent) {
            MutableTest state = tests.computeIfAbsent(doneEvent.testId(), MutableTest::new);
            state.completed = true;
            state.completedAt = Optional.of(doneEvent.time());
            state.hidden = doneEvent.hidden();
            state.skipped = doneEvent.skipped();
            FlutterTestOutcome reported = doneEvent.skipped()
                    ? FlutterTestOutcome.SKIPPED
                    : outcome(doneEvent.result());
            state.outcome = merge(state.outcome, reported);
            if (!state.errors.isEmpty()) {
                state.hidden = false;
            }
            return;
        }
        if (event instanceof FlutterTestEvent.DoneEvent doneEvent) {
            done = true;
            success = doneEvent.success();
            return;
        }
        if (event instanceof FlutterTestEvent.UnknownEvent unknownEvent) {
            unknownEvents.add(unknownEvent);
        }
    }

    public synchronized List<FlutterTestSuite> suites() {
        return List.copyOf(suites.values());
    }

    public synchronized Optional<FlutterTestSuite> suite(long id) {
        return Optional.ofNullable(suites.get(id));
    }

    public synchronized List<FlutterTestCaseState> tests() {
        return tests.values().stream().map(MutableTest::snapshot).toList();
    }

    public synchronized Optional<FlutterTestCaseState> test(long id) {
        MutableTest state = tests.get(id);
        return state == null ? Optional.empty() : Optional.of(state.snapshot());
    }

    public synchronized boolean done() {
        return done;
    }

    /** Empty means the runner ended early or has not emitted {@code done}. */
    public synchronized Optional<Boolean> success() {
        return success;
    }

    public synchronized List<FlutterTestEvent.UnknownEvent> unknownEvents() {
        return List.copyOf(unknownEvents);
    }

    private static FlutterTestOutcome outcome(String result) {
        return switch (result) {
            case "success" -> FlutterTestOutcome.SUCCESS;
            case "failure" -> FlutterTestOutcome.FAILURE;
            case "error" -> FlutterTestOutcome.ERROR;
            default -> FlutterTestOutcome.UNKNOWN;
        };
    }

    private static FlutterTestOutcome merge(
            FlutterTestOutcome current,
            FlutterTestOutcome next) {
        return rank(next) > rank(current) ? next : current;
    }

    private static int rank(FlutterTestOutcome outcome) {
        return switch (outcome) {
            case RUNNING -> 0;
            case SUCCESS -> 1;
            case UNKNOWN -> 2;
            case SKIPPED -> 3;
            case FAILURE -> 4;
            case ERROR -> 5;
        };
    }

    private static final class MutableTest {
        private final long id;
        private FlutterTestCase test;
        private FlutterTestOutcome outcome = FlutterTestOutcome.RUNNING;
        private boolean completed;
        private boolean hidden;
        private boolean skipped;
        private Optional<Long> startedAt = Optional.empty();
        private Optional<Long> completedAt = Optional.empty();
        private final List<FlutterTestMessage> messages = new ArrayList<>();
        private final List<FlutterTestError> errors = new ArrayList<>();

        MutableTest(long id) {
            this.id = id;
        }

        FlutterTestCaseState snapshot() {
            return new FlutterTestCaseState(
                    id,
                    Optional.ofNullable(test),
                    outcome,
                    completed,
                    hidden,
                    skipped,
                    startedAt,
                    completedAt,
                    messages,
                    errors);
        }
    }
}
