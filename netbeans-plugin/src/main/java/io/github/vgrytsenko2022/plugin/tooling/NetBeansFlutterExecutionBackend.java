package io.github.vgrytsenko2022.plugin.tooling;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.swing.event.ChangeListener;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import io.github.vgrytsenko2022.plugin.options.FlutterOptionsPanelController;

/** NetBeans-native Output/Stop/Progress backend for Flutter tooling commands. */
final class NetBeansFlutterExecutionBackend implements FlutterExecutionBackend {
    private static final ExecutionDescriptor.RerunCondition NO_BUILT_IN_RERUN =
            new ExecutionDescriptor.RerunCondition() {
                @Override
                public void addChangeListener(ChangeListener listener) {
                }

                @Override
                public void removeChangeListener(ChangeListener listener) {
                }

                @Override
                public boolean isRerunPossible() {
                    return false;
                }
            };

    @Override
    public FlutterExecutionHandle start(FlutterExecutionRequest request) {
        org.netbeans.api.extexecution.base.ProcessBuilder builder =
                org.netbeans.api.extexecution.base.ProcessBuilder.getLocal();
        builder.setExecutable(request.executable().toString());
        builder.setWorkingDirectory(request.workingDirectory().toString());
        builder.setArguments(request.arguments());
        builder.setRedirectErrorStream(false);

        ExecutionDescriptor descriptor = new ExecutionDescriptor()
                .frontWindow(true)
                .frontWindowOnError(true)
                .controllable(true)
                .rerunCondition(NO_BUILT_IN_RERUN)
                .showProgress(true)
                .charset(StandardCharsets.UTF_8)
                .outLineBased(true)
                .errLineBased(true)
                .optionsPath(FlutterOptionsPanelController.ID);

        if (request.outputConvertor() != null) {
            descriptor = descriptor.outConvertorFactory(request::outputConvertor);
        } else {
            descriptor = descriptor.outProcessorFactory(
                    (ExecutionDescriptor.InputProcessorFactory2) defaultProcessor -> processors(
                            request.echoStandardOutput() ? defaultProcessor : null,
                            request.standardOutput()));
        }
        descriptor = descriptor.errProcessorFactory(
                (ExecutionDescriptor.InputProcessorFactory2) defaultProcessor -> processors(
                        defaultProcessor,
                        request.standardError()));

        ProcessTerminationTracker termination = new ProcessTerminationTracker();
        Future<Integer> result = ExecutionService.newService(
                () -> termination.start(builder),
                descriptor,
                request.displayName()).run();
        termination.observeLogicalResult(result);
        return new FlutterExecutionHandle(
                result,
                termination.completion(),
                mayInterruptIfRunning -> {
                    termination.requestCancellation();
                    boolean cancelled = result.cancel(mayInterruptIfRunning);
                    termination.logicalResultFinished();
                    return cancelled;
                });
    }

    /**
     * Bridges NetBeans' cancellable Future to the raw process lifecycle.
     * ExecutionService marks its Future cancelled before Process.destroy()
     * necessarily takes effect, so deletion must observe Process.onExit().
     */
    static final class ProcessTerminationTracker {
        private final Object lock = new Object();
        private final CompletableFuture<Void> completion = new CompletableFuture<>();
        private boolean creatorStarted;
        private boolean noProcessResolved;
        private boolean cancellationRequested;
        private Process process;

        Process start(Callable<? extends Process> processCreator)
                throws Exception {
            synchronized (lock) {
                if (noProcessResolved) {
                    throw new CancellationException(
                            "Flutter command finished before process creation");
                }
                creatorStarted = true;
                if (cancellationRequested) {
                    noProcessResolved = true;
                    completion.complete(null);
                    throw new CancellationException(
                            "Flutter command was cancelled before process creation");
                }
            }

            Process created;
            try {
                created = Objects.requireNonNull(
                        processCreator.call(),
                        "NetBeans process builder returned no process");
            } catch (Exception | Error ex) {
                completion.complete(null);
                throw ex;
            }

            boolean cancel;
            synchronized (lock) {
                process = created;
                cancel = cancellationRequested;
            }
            created.onExit().whenComplete((ignored, failure) -> completion.complete(null));
            if (cancel && created.isAlive()) {
                created.destroy();
            }
            return created;
        }

        CompletableFuture<Void> completion() {
            return completion;
        }

        void requestCancellation() {
            Process current;
            synchronized (lock) {
                cancellationRequested = true;
                current = process;
            }
            if (current != null && current.isAlive()) {
                current.destroy();
            }
        }

        void observeLogicalResult(Future<Integer> result) {
            Thread.ofVirtual().name("flutter-tool-command-result").start(() -> {
                boolean interrupted = false;
                for (;;) {
                    try {
                        result.get();
                        break;
                    } catch (InterruptedException ex) {
                        interrupted = true;
                    } catch (CancellationException | ExecutionException ex) {
                        break;
                    }
                }
                logicalResultFinished();
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        void logicalResultFinished() {
            synchronized (lock) {
                if (!creatorStarted) {
                    // Win the race against a Callable that was selected by
                    // ExecutionService immediately before its Future became
                    // cancelled. start() observes this terminal state and may
                    // no longer create a process.
                    noProcessResolved = true;
                    completion.complete(null);
                }
            }
        }
    }

    private static InputProcessor processors(
            InputProcessor defaultProcessor,
            Consumer<String> lineConsumer) {
        List<InputProcessor> processors = new ArrayList<>(2);
        if (defaultProcessor != null) {
            processors.add(defaultProcessor);
        }
        processors.add(InputProcessors.bridge(new ConsumerLineProcessor(lineConsumer)));
        return InputProcessors.proxy(processors.toArray(InputProcessor[]::new));
    }

    private static final class ConsumerLineProcessor implements LineProcessor {
        private final Consumer<String> consumer;

        ConsumerLineProcessor(Consumer<String> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void processLine(String line) {
            consumer.accept(line);
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {
        }
    }
}
