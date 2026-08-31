package dev.flutter.netbeans.plugin.designer.canvas;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Bounded, descendant-aware retirement for a started Canvas helper process.
 *
 * <p>The root is never destroyed before its current descendants have been
 * captured. Already captured live handles are traversed again while the root
 * is retiring so a child that becomes detached from the root remains owned by
 * this retirement attempt. Success means the root and every captured handle
 * were observed dead; an incomplete process-tree observation fails closed.
 */
final class CanvasProcessTreeRetirement {
    private static final Duration DEFAULT_GRACEFUL_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DEFAULT_FORCED_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(25);
    private static final int MAX_CAPTURED_DESCENDANTS = 1_024;
    private static final int MAX_REPORTED_OBSERVATION_FAILURES = 16;

    private CanvasProcessTreeRetirement() {
    }

    static void retire(Process process) throws IOException {
        retire(
                process,
                DEFAULT_GRACEFUL_TIMEOUT,
                DEFAULT_FORCED_TIMEOUT,
                DEFAULT_POLL_INTERVAL);
    }

    /** Test seam retaining the production algorithm with bounded short policy. */
    static void retire(
            Process process,
            Duration gracefulTimeout,
            Duration forcedTimeout,
            Duration pollInterval) throws IOException {
        Process acceptedProcess = Objects.requireNonNull(process, "process");
        Duration acceptedGracefulTimeout = requireNonNegative(
                gracefulTimeout, "gracefulTimeout");
        Duration acceptedForcedTimeout = requireNonNegative(
                forcedTimeout, "forcedTimeout");
        Duration acceptedPollInterval = requirePositive(
                pollInterval, "pollInterval");

        RetirementObservation observation = new RetirementObservation();
        Map<Long, ProcessHandle> descendants = new LinkedHashMap<>();
        boolean interrupted = false;
        try {
            captureDescendants(acceptedProcess, descendants, observation);
            destroyDescendants(descendants, false);
            destroyRoot(acceptedProcess, false);

            long gracefulStarted = System.nanoTime();
            while (treeMayBeAlive(acceptedProcess, descendants, observation)
                    && beforeDeadline(gracefulStarted, acceptedGracefulTimeout)) {
                captureDescendants(acceptedProcess, descendants, observation);
                destroyDescendants(descendants, false);
                interrupted |= terminationPause(acceptedPollInterval);
            }

            captureDescendants(acceptedProcess, descendants, observation);
            destroyDescendants(descendants, true);
            destroyRoot(acceptedProcess, true);
            long forcedStarted = System.nanoTime();
            while (treeMayBeAlive(acceptedProcess, descendants, observation)
                    && beforeDeadline(forcedStarted, acceptedForcedTimeout)) {
                captureDescendants(acceptedProcess, descendants, observation);
                destroyDescendants(descendants, true);
                destroyRoot(acceptedProcess, true);
                interrupted |= terminationPause(acceptedPollInterval);
            }

            // One last bounded forced phase catches descendants created during
            // the first forced phase while their already-captured parent was
            // still live. A newly captured child must be retired, not merely
            // reported as a cleanup failure and left running.
            captureDescendants(acceptedProcess, descendants, observation);
            destroyDescendants(descendants, true);
            destroyRoot(acceptedProcess, true);
            long confirmationStarted = System.nanoTime();
            while (treeMayBeAlive(acceptedProcess, descendants, observation)
                    && beforeDeadline(confirmationStarted, acceptedForcedTimeout)) {
                captureDescendants(acceptedProcess, descendants, observation);
                destroyDescendants(descendants, true);
                destroyRoot(acceptedProcess, true);
                interrupted |= terminationPause(acceptedPollInterval);
            }

            captureDescendants(acceptedProcess, descendants, observation);
            destroyDescendants(descendants, true);
            destroyRoot(acceptedProcess, true);
            List<Long> liveDescendants = liveDescendantPids(
                    descendants, observation);
            boolean rootAlive = processIsAlive(acceptedProcess, observation);
            if (rootAlive || !liveDescendants.isEmpty()
                    || !observation.complete()) {
                IOException failure = new IOException(failureMessage(
                        acceptedProcess,
                        rootAlive,
                        liveDescendants,
                        observation.complete()));
                observation.failures().forEach(failure::addSuppressed);
                throw failure;
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void captureDescendants(
            Process process,
            Map<Long, ProcessHandle> captured,
            RetirementObservation observation) {
        captureFrom(
                process::descendants,
                captured,
                observation,
                "root process descendants");
        // A child may outlive and become detached from the root process. Keep
        // discovering below every already-captured live handle until the tree
        // is physically gone.
        for (ProcessHandle handle : List.copyOf(captured.values())) {
            if (!handleMayBeAlive(handle, observation)) {
                continue;
            }
            captureFrom(
                    handle::descendants,
                    captured,
                    observation,
                    "descendants of captured process " + safePid(handle));
        }
    }

    private static void captureFrom(
            DescendantStream source,
            Map<Long, ProcessHandle> captured,
            RetirementObservation observation,
            String label) {
        try (Stream<ProcessHandle> stream = source.open()) {
            var iterator = stream.iterator();
            int observedHandles = 0;
            while (iterator.hasNext()) {
                if (++observedHandles > MAX_CAPTURED_DESCENDANTS) {
                    observation.incomplete(new IllegalStateException(
                            "One Canvas process-tree query exceeds "
                            + MAX_CAPTURED_DESCENDANTS + " descendants"));
                    return;
                }
                ProcessHandle handle = iterator.next();
                long pid;
                try {
                    pid = handle.pid();
                } catch (RuntimeException failure) {
                    observation.incomplete(failure);
                    continue;
                }
                if (!captured.containsKey(pid)
                        && captured.size() >= MAX_CAPTURED_DESCENDANTS) {
                    observation.incomplete(new IllegalStateException(
                            "Canvas process tree exceeds "
                            + MAX_CAPTURED_DESCENDANTS + " descendants"));
                    return;
                }
                captured.putIfAbsent(pid, handle);
            }
        } catch (RuntimeException failure) {
            observation.incomplete(new IllegalStateException(
                    "Cannot inspect " + label, failure));
        }
    }

    private static void destroyDescendants(
            Map<Long, ProcessHandle> descendants,
            boolean forcibly) {
        descendants.values().stream()
                .sorted(Comparator.reverseOrder())
                .forEach(handle -> destroyHandle(handle, forcibly));
    }

    private static void destroyHandle(ProcessHandle handle, boolean forcibly) {
        try {
            if (!handle.isAlive()) {
                return;
            }
            if (forcibly) {
                handle.destroyForcibly();
            } else {
                handle.destroy();
            }
        } catch (RuntimeException ignored) {
            // Final observation decides whether retirement is confirmed.
        }
    }

    private static void destroyRoot(Process process, boolean forcibly) {
        try {
            if (!process.isAlive()) {
                return;
            }
            if (forcibly) {
                process.destroyForcibly();
            } else {
                process.destroy();
            }
        } catch (RuntimeException ignored) {
            // Final observation decides whether retirement is confirmed.
        }
    }

    private static boolean treeMayBeAlive(
            Process process,
            Map<Long, ProcessHandle> descendants,
            RetirementObservation observation) {
        if (processIsAlive(process, observation)) {
            return true;
        }
        for (ProcessHandle handle : descendants.values()) {
            if (handleMayBeAlive(handle, observation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean processIsAlive(
            Process process,
            RetirementObservation observation) {
        try {
            return process.isAlive();
        } catch (RuntimeException failure) {
            observation.incomplete(failure);
            return true;
        }
    }

    private static boolean handleMayBeAlive(
            ProcessHandle handle,
            RetirementObservation observation) {
        try {
            return handle.isAlive();
        } catch (RuntimeException failure) {
            observation.incomplete(failure);
            return true;
        }
    }

    private static List<Long> liveDescendantPids(
            Map<Long, ProcessHandle> descendants,
            RetirementObservation observation) {
        List<Long> live = new ArrayList<>();
        descendants.forEach((pid, handle) -> {
            if (handleMayBeAlive(handle, observation)) {
                live.add(pid);
            }
        });
        return List.copyOf(live);
    }

    private static String failureMessage(
            Process process,
            boolean rootAlive,
            List<Long> liveDescendants,
            boolean observationComplete) {
        List<String> reasons = new ArrayList<>(3);
        if (rootAlive) {
            reasons.add("root process " + safePid(process) + " is still alive");
        }
        if (!liveDescendants.isEmpty()) {
            reasons.add("live descendants " + liveDescendants);
        }
        if (!observationComplete) {
            reasons.add("the full process tree could not be observed");
        }
        return "Canvas helper process tree retirement was not confirmed: "
                + String.join("; ", reasons);
    }

    private static long safePid(Process process) {
        try {
            return process.pid();
        } catch (RuntimeException ignored) {
            return -1L;
        }
    }

    private static long safePid(ProcessHandle handle) {
        try {
            return handle.pid();
        } catch (RuntimeException ignored) {
            return -1L;
        }
    }

    private static boolean beforeDeadline(long started, Duration timeout) {
        return System.nanoTime() - started < timeout.toNanos();
    }

    private static boolean terminationPause(Duration interval) {
        try {
            TimeUnit.NANOSECONDS.sleep(interval.toNanos());
            return false;
        } catch (InterruptedException ex) {
            return true;
        }
    }

    private static Duration requireNonNegative(Duration value, String label) {
        Duration accepted = Objects.requireNonNull(value, label);
        if (accepted.isNegative()) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
        return accepted;
    }

    private static Duration requirePositive(Duration value, String label) {
        Duration accepted = requireNonNegative(value, label);
        if (accepted.isZero()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return accepted;
    }

    @FunctionalInterface
    private interface DescendantStream {
        Stream<ProcessHandle> open();
    }

    private static final class RetirementObservation {
        private final List<Throwable> failures = new ArrayList<>();
        private boolean complete = true;

        void incomplete(Throwable failure) {
            complete = false;
            if (failures.size() < MAX_REPORTED_OBSERVATION_FAILURES) {
                failures.add(failure);
            }
        }

        boolean complete() {
            return complete;
        }

        List<Throwable> failures() {
            return List.copyOf(failures);
        }
    }
}
