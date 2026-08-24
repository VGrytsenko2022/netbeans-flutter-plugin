package dev.flutter.netbeans.plugin.tooling.test;

import dev.flutter.netbeans.plugin.tooling.DartSourceLocation;
import java.util.Objects;
import java.util.Optional;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.Testcase;

/** Test Results testcase retaining the Dart reporter identity and source selector. */
final class FlutterNetBeansTestcase extends Testcase {
    private final long eventId;
    private final Optional<String> relativePath;
    private final Optional<DartSourceLocation> sourceLocation;

    FlutterNetBeansTestcase(
            long eventId,
            String name,
            TestSession session,
            Optional<String> relativePath,
            Optional<DartSourceLocation> sourceLocation) {
        super(name, name, "FLUTTER", session);
        if (eventId < 0) {
            throw new IllegalArgumentException("Flutter test event id cannot be negative");
        }
        this.eventId = eventId;
        this.relativePath = Objects.requireNonNull(relativePath, "relativePath");
        this.sourceLocation = Objects.requireNonNull(sourceLocation, "sourceLocation");
        sourceLocation.ifPresent(location -> setLocation(
                location.sourcePath() + ":" + location.line() + ":" + location.column()));
    }

    long eventId() {
        return eventId;
    }

    Optional<String> relativePath() {
        return relativePath;
    }

    Optional<DartSourceLocation> sourceLocation() {
        return sourceLocation;
    }
}
