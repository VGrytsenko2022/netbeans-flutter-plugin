package io.github.vgrytsenko2022.plugin.tooling;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

/** Separates a command's logical result from physical child-process exit. */
final class FlutterExecutionHandle {
    @FunctionalInterface
    interface Cancellation {
        boolean cancel(boolean mayInterruptIfRunning);
    }

    private final Future<Integer> result;
    private final CompletionStage<Void> termination;
    private final Cancellation cancellation;

    FlutterExecutionHandle(
            Future<Integer> result,
            CompletionStage<Void> termination) {
        this(result, termination, result::cancel);
    }

    FlutterExecutionHandle(
            Future<Integer> result,
            CompletionStage<Void> termination,
            Cancellation cancellation) {
        this.result = Objects.requireNonNull(result, "result");
        this.termination = Objects.requireNonNull(termination, "termination");
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation");
    }

    Future<Integer> result() {
        return result;
    }

    CompletionStage<Void> termination() {
        return termination;
    }

    boolean cancel(boolean mayInterruptIfRunning) {
        return cancellation.cancel(mayInterruptIfRunning);
    }
}
