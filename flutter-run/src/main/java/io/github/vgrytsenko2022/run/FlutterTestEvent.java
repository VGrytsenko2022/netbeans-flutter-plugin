package io.github.vgrytsenko2022.run;

import java.util.Objects;
import java.util.Optional;

/** A supported or forward-compatible event from the Dart test JSON reporter. */
public sealed interface FlutterTestEvent permits
        FlutterTestEvent.SuiteEvent,
        FlutterTestEvent.TestStartEvent,
        FlutterTestEvent.PrintEvent,
        FlutterTestEvent.ErrorEvent,
        FlutterTestEvent.TestDoneEvent,
        FlutterTestEvent.DoneEvent,
        FlutterTestEvent.UnknownEvent {

    String type();

    long time();

    record SuiteEvent(long time, FlutterTestSuite suite) implements FlutterTestEvent {
        public SuiteEvent {
            Objects.requireNonNull(suite, "suite");
        }

        @Override
        public String type() {
            return "suite";
        }
    }

    record TestStartEvent(long time, FlutterTestCase test) implements FlutterTestEvent {
        public TestStartEvent {
            Objects.requireNonNull(test, "test");
        }

        @Override
        public String type() {
            return "testStart";
        }
    }

    record PrintEvent(
            long time,
            long testId,
            String messageType,
            String message) implements FlutterTestEvent {

        public PrintEvent {
            if (testId < 0) {
                throw new IllegalArgumentException("Flutter test id cannot be negative");
            }
            if (messageType == null || messageType.isBlank()) {
                throw new IllegalArgumentException("Flutter test message type is required");
            }
            Objects.requireNonNull(message, "message");
        }

        @Override
        public String type() {
            return "print";
        }
    }

    record ErrorEvent(
            long time,
            long testId,
            String error,
            String stackTrace,
            boolean failure) implements FlutterTestEvent {

        public ErrorEvent {
            if (testId < 0) {
                throw new IllegalArgumentException("Flutter test id cannot be negative");
            }
            if (error == null || error.isBlank()) {
                throw new IllegalArgumentException("Flutter test error text is required");
            }
            stackTrace = stackTrace == null ? "" : stackTrace;
        }

        @Override
        public String type() {
            return "error";
        }
    }

    record TestDoneEvent(
            long time,
            long testId,
            String result,
            boolean hidden,
            boolean skipped) implements FlutterTestEvent {

        public TestDoneEvent {
            if (testId < 0) {
                throw new IllegalArgumentException("Flutter test id cannot be negative");
            }
            if (result == null || result.isBlank()) {
                throw new IllegalArgumentException("Flutter test result is required");
            }
        }

        @Override
        public String type() {
            return "testDone";
        }
    }

    record DoneEvent(long time, Optional<Boolean> success) implements FlutterTestEvent {
        public DoneEvent {
            Objects.requireNonNull(success, "success");
        }

        @Override
        public String type() {
            return "done";
        }
    }

    /** Preserves an unrecognized JSON event without constraining future fields. */
    record UnknownEvent(String type, long time, String rawJson) implements FlutterTestEvent {
        public UnknownEvent {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("Unknown Flutter test event type is required");
            }
            Objects.requireNonNull(rawJson, "rawJson");
        }
    }
}
