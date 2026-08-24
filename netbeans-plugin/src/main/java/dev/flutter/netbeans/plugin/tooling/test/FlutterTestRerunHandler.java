package dev.flutter.netbeans.plugin.tooling.test;

import dev.flutter.netbeans.run.FlutterToolCommand;
import dev.flutter.netbeans.run.FlutterToolCommandType;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.api.RerunType;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.openide.util.ChangeSupport;

/** Standard Test Results rerun handler with controller-independent requests. */
public final class FlutterTestRerunHandler implements RerunHandler {
    private static final Pattern REGEX_META = Pattern.compile("([\\\\^$.|?*+()\\[\\]{}])");

    private final FlutterToolCommand originalCommand;
    private final Callback callback;
    private final ChangeSupport changes = new ChangeSupport(this);
    private volatile boolean enabled;

    public FlutterTestRerunHandler(
            FlutterToolCommand originalCommand,
            Callback callback) {
        this.originalCommand = Objects.requireNonNull(originalCommand, "originalCommand");
        if (originalCommand.type() != FlutterToolCommandType.TEST) {
            throw new IllegalArgumentException("A Flutter test command is required");
        }
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    @Override
    public void rerun() {
        if (!disableForRerun()) {
            return;
        }
        try {
            callback.rerun(new AllRequest(originalCommand));
        } catch (RuntimeException ex) {
            setEnabled(true);
            throw ex;
        }
    }

    @Override
    public void rerun(Set<Testcase> tests) {
        Objects.requireNonNull(tests, "tests");
        if (!disableForRerun()) {
            return;
        }
        SelectedRequest request = selectedRequest(tests);
        if (request.targets().isEmpty()) {
            setEnabled(true);
            return;
        }
        try {
            callback.rerun(request);
        } catch (RuntimeException ex) {
            setEnabled(true);
            throw ex;
        }
    }

    @Override
    public boolean enabled(RerunType type) {
        return enabled && (type == RerunType.ALL || type == RerunType.CUSTOM);
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }

    public void setEnabled(boolean newEnabled) {
        if (enabled == newEnabled) {
            return;
        }
        enabled = newEnabled;
        fireChangeOnEventThread();
    }

    static SelectedRequest selectedRequest(Collection<? extends Testcase> tests) {
        Map<String, Set<String>> byPath = new LinkedHashMap<>();
        tests.stream()
                .filter(FlutterNetBeansTestcase.class::isInstance)
                .map(FlutterNetBeansTestcase.class::cast)
                .filter(test -> test.relativePath().isPresent())
                .sorted(Comparator
                        .comparing((FlutterNetBeansTestcase test) -> test.relativePath().orElse(""))
                        .thenComparing(Testcase::getName)
                        .thenComparingLong(FlutterNetBeansTestcase::eventId))
                .forEach(test -> byPath
                        .computeIfAbsent(test.relativePath().orElseThrow(), ignored -> new LinkedHashSet<>())
                        .add(test.getName()));
        List<SelectedTarget> targets = new ArrayList<>(byPath.size());
        byPath.forEach((path, names) -> targets.add(new SelectedTarget(
                path,
                List.copyOf(names),
                exactNamePattern(names))));
        return new SelectedRequest(targets);
    }

    static String exactNamePattern(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("At least one Flutter test name is required");
        }
        return names.stream()
                .map(name -> {
                    if (name == null || name.isBlank()) {
                        throw new IllegalArgumentException("Flutter test names cannot be blank");
                    }
                    return REGEX_META.matcher(name).replaceAll("\\\\$1");
                })
                .sorted()
                .collect(java.util.stream.Collectors.joining("|", "^(?:", ")$"));
    }

    private synchronized boolean disableForRerun() {
        if (!enabled) {
            return false;
        }
        setEnabled(false);
        return true;
    }

    private void fireChangeOnEventThread() {
        if (EventQueue.isDispatchThread()) {
            changes.fireChange();
        } else {
            EventQueue.invokeLater(changes::fireChange);
        }
    }

    @FunctionalInterface
    public interface Callback {
        void rerun(RerunRequest request);
    }

    public sealed interface RerunRequest permits AllRequest, SelectedRequest {
    }

    public record AllRequest(FlutterToolCommand command) implements RerunRequest {
        public AllRequest {
            Objects.requireNonNull(command, "command");
        }
    }

    public record SelectedRequest(List<SelectedTarget> targets) implements RerunRequest {
        public SelectedRequest {
            targets = List.copyOf(targets);
        }
    }

    public record SelectedTarget(
            String projectRelativePath,
            List<String> testNames,
            String exactNamePattern) {
        public SelectedTarget {
            if (projectRelativePath == null || projectRelativePath.isBlank()) {
                throw new IllegalArgumentException("A Flutter test path is required");
            }
            projectRelativePath = projectRelativePath.replace('\\', '/');
            testNames = List.copyOf(testNames);
            if (testNames.isEmpty() || exactNamePattern == null || exactNamePattern.isBlank()) {
                throw new IllegalArgumentException("Flutter test names and a filter are required");
            }
        }
    }
}
