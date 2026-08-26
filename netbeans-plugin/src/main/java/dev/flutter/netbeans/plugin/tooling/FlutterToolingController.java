package dev.flutter.netbeans.plugin.tooling;

import dev.flutter.netbeans.api.FlutterDevice;
import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.plugin.lifecycle.AsyncTaskTracker;
import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainService;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainStatus;
import dev.flutter.netbeans.run.FlutterToolCommand;
import dev.flutter.netbeans.run.FlutterToolCommandType;
import java.awt.EventQueue;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/** Project-scoped orchestration of cancellable one-shot Flutter tooling commands. */
public final class FlutterToolingController implements AutoCloseable {
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterToolingController.class.getName(), 3, true);

    private final Project project;
    private final Path projectRoot;
    private final FlutterExecutionBackend backend;
    private final FlutterTestSessionFactory testSessions;
    private final FlutterSdkResolver sdkResolver;
    private final AsyncTaskTracker quiescenceTasks = new AsyncTaskTracker(
            command -> WORKER.post(command));
    private final ChangeSupport changes = new ChangeSupport(this);
    private final Object lock = new Object();

    private volatile boolean closed;
    private volatile ActiveOperation active;
    private long lifecycleGeneration;

    public FlutterToolingController(
            Project project,
            FlutterProjectInfo projectInfo,
            FlutterTestSessionFactory testSessions) {
        this(
                project,
                projectInfo,
                new NetBeansFlutterExecutionBackend(),
                testSessions,
                FlutterToolingController::resolveFlutterSdk);
    }

    FlutterToolingController(
            Project project,
            FlutterProjectInfo projectInfo,
            FlutterExecutionBackend backend,
            FlutterTestSessionFactory testSessions,
            FlutterSdkResolver sdkResolver) {
        this.project = Objects.requireNonNull(project, "project");
        this.projectRoot = Objects.requireNonNull(projectInfo, "projectInfo")
                .root().toAbsolutePath().normalize();
        this.backend = Objects.requireNonNull(backend, "backend");
        this.testSessions = Objects.requireNonNull(testSessions, "testSessions");
        this.sdkResolver = Objects.requireNonNull(sdkResolver, "sdkResolver");
    }

    public boolean isCommandEnabled(String command) {
        return isToolingCommand(command)
                && !closed
                && project.getProjectDirectory().isValid()
                && active == null;
    }

    public boolean isCommandEnabled(String command, FlutterDevice buildTarget) {
        return isCommandEnabled(command)
                && (!requiresBuildTarget(command) || supportsBuildTarget(buildTarget));
    }

    public void invoke(String command, Lookup context, ActionProgress progress) {
        invoke(command, context, progress, null);
    }

    public void invoke(
            String command,
            Lookup context,
            ActionProgress progress,
            FlutterDevice buildTarget) {
        Lookup safeContext = context == null ? Lookup.EMPTY : context;
        if (!isCommandEnabled(command, buildTarget)) {
            finishProgress(progress, false);
            showMessage(
                    "Flutter action unavailable",
                    "Cannot perform " + displayName(command) + " for " + projectRoot + ": "
                    + unavailableReason(command, buildTarget) + ".",
                    NotifyDescriptor.WARNING_MESSAGE);
            return;
        }

        final ToolingPlan plan;
        try {
            plan = planFor(command, safeContext, buildTarget);
        } catch (IllegalArgumentException ex) {
            finishProgress(progress, false);
            showMessage(
                    "Flutter action unavailable",
                    "Cannot perform " + displayName(command) + " for " + projectRoot + ": "
                    + ex.getMessage() + ".",
                    NotifyDescriptor.WARNING_MESSAGE);
            return;
        }
        start(plan, progress);
    }

    public void open() {
        synchronized (lock) {
            if (!closed) {
                return;
            }
            closed = false;
            lifecycleGeneration++;
        }
        fireChange();
    }

    @Override
    public void close() {
        ActiveOperation operation;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            lifecycleGeneration++;
            operation = active;
            active = null;
        }
        if (operation != null) {
            quiescenceTasks.execute(() -> {
                FlutterExecutionHandle execution = operation.execution;
                if (execution != null) {
                    execution.cancel(true);
                }
                operation.complete(-1, true, null, false);
            });
        }
        fireChange();
    }

    /** Waits for project-owned Flutter command processes and completion work. */
    public boolean awaitQuiescence(Duration timeout) throws InterruptedException {
        return quiescenceTasks.awaitIdle(timeout);
    }

    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }

    private ToolingPlan planFor(
            String command,
            Lookup context,
            FlutterDevice buildTarget) {
        List<FlutterToolCommand> commands = switch (command) {
            case ActionProvider.COMMAND_CLEAN -> List.of(FlutterToolCommand.clean());
            case ActionProvider.COMMAND_BUILD -> List.of(FlutterToolCommand.build(buildTarget));
            case ActionProvider.COMMAND_REBUILD -> List.of(
                    FlutterToolCommand.clean(),
                    FlutterToolCommand.build(buildTarget));
            case FlutterProjectActionProvider.COMMAND_PUB_GET ->
                List.of(FlutterToolCommand.pubGet());
            case FlutterProjectActionProvider.COMMAND_ANALYZE ->
                List.of(FlutterToolCommand.analyze());
            case ActionProvider.COMMAND_TEST -> List.of(FlutterToolCommand.test());
            case ActionProvider.COMMAND_TEST_SINGLE -> {
                FlutterTestSelection selection = FlutterTestSelection.fromContext(
                                projectRoot,
                                context,
                                false)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "select an existing Dart test file inside the project"));
                yield List.of(selection.plainName()
                        .map(name -> FlutterToolCommand.test(selection.relativePath(), name))
                        .orElseGet(() -> FlutterToolCommand.test(selection.relativePath())));
            }
            case FlutterProjectActionProvider.COMMAND_TEST_FILE -> {
                FlutterTestSelection selection = FlutterTestSelection.fromContext(
                                projectRoot,
                                context,
                                false)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "select an existing Dart test file inside the project"));
                yield List.of(FlutterToolCommand.test(selection.relativePath()));
            }
            case SingleMethod.COMMAND_RUN_SINGLE_METHOD,
                 FlutterProjectActionProvider.COMMAND_TEST_AT_CARET -> {
                FlutterTestSelection selection = FlutterTestSelection.fromContext(
                                projectRoot,
                                context,
                                command.equals(FlutterProjectActionProvider.COMMAND_TEST_AT_CARET))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "place the caret in a test(...) or testWidgets(...) declaration"));
                yield List.of(FlutterToolCommand.test(
                        selection.relativePath(),
                        selection.plainName().orElseThrow()));
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported Flutter tooling command: " + command);
        };
        return new ToolingPlan(operationName(command), commands);
    }

    private void start(ToolingPlan plan, ActionProgress progress) {
        ActiveOperation operation;
        synchronized (lock) {
            if (closed || active != null || !project.getProjectDirectory().isValid()) {
                finishProgress(progress, false);
                return;
            }
            operation = new ActiveOperation(plan, lifecycleGeneration, progress);
            active = operation;
        }
        fireChange();
        quiescenceTasks.execute(() -> launch(operation));
    }

    private void launch(ActiveOperation operation) {
        try {
            if (!isCurrent(operation)) {
                operation.complete(-1, true, null, false);
                return;
            }
            FlutterSdk sdk = sdkResolver.resolve(operation.plan.operationName());
            for (FlutterToolCommand command : operation.plan.commands()) {
                if (!isCurrent(operation)) {
                    operation.complete(-1, true, null, false);
                    return;
                }
                operation.currentCommand = command;
                FlutterExecutionRequest request = request(operation, sdk, command);
                FlutterExecutionHandle execution = startStage(operation, request);
                if (execution == null) {
                    operation.complete(-1, true, null, false);
                    return;
                }

                int exitCode = execution.result().get();
                clearExecution(operation, execution);
                if (!isCurrent(operation)) {
                    operation.complete(-1, true, null, false);
                    return;
                }
                if (exitCode != 0) {
                    complete(operation, exitCode, false, null, false);
                    return;
                }
            }
            complete(operation, 0, false, null, true);
        } catch (CancellationException ex) {
            complete(operation, -1, true, null, false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            FlutterExecutionHandle execution = operation.execution;
            if (execution != null) {
                execution.cancel(true);
            }
            complete(operation, -1, true, null, false);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            complete(operation, -1, false, cause, false);
        } catch (Exception ex) {
            complete(operation, -1, false, ex, false);
        }
    }

    private FlutterExecutionHandle startStage(
            ActiveOperation operation,
            FlutterExecutionRequest request) {
        synchronized (lock) {
            if (!isCurrentLocked(operation)) {
                return null;
            }
        }
        FlutterExecutionHandle execution = Objects.requireNonNull(
                backend.start(request),
                "Flutter execution backend returned no execution handle");
        // Register physical termination before the handle can be attached or
        // cancelled. A cancelled NetBeans Future may be done while its child
        // process is still exiting.
        quiescenceTasks.track(execution.termination());
        synchronized (lock) {
            if (isCurrentLocked(operation)) {
                operation.execution = execution;
                return execution;
            }
        }
        // close() may win while backend.start() is creating the process. Track
        // the late handle before cancelling it so deletion still sees its exit.
        execution.cancel(true);
        return null;
    }

    private void clearExecution(
            ActiveOperation operation,
            FlutterExecutionHandle execution) {
        synchronized (lock) {
            if (operation.execution == execution) {
                operation.execution = null;
            }
        }
    }

    private FlutterExecutionRequest request(
            ActiveOperation operation,
            FlutterSdk sdk,
            FlutterToolCommand command) {
        String displayName = "Flutter " + operation.plan.operationName() + ": "
                + projectRoot.getFileName();
        if (command.type() == FlutterToolCommandType.TEST) {
            FlutterTestSessionBridge bridge = testSessions.create(
                    project,
                    projectRoot,
                    displayName,
                    command,
                    this::rerun);
            operation.testSession = bridge;
            return new FlutterExecutionRequest(
                    displayName,
                    sdk.flutterExecutable(),
                    projectRoot,
                    command.arguments(),
                    false,
                    bridge::standardOutput,
                    bridge::standardError,
                    null);
        }
        if (command.type() == FlutterToolCommandType.ANALYZE) {
            return new FlutterExecutionRequest(
                    displayName,
                    sdk.flutterExecutable(),
                    projectRoot,
                    command.arguments(),
                    true,
                    null,
                    null,
                    new FlutterAnalyzeLineConvertor(projectRoot));
        }
        return new FlutterExecutionRequest(
                displayName,
                sdk.flutterExecutable(),
                projectRoot,
                command.arguments(),
                true,
                null,
                null,
                null);
    }

    private void rerun(FlutterToolCommand command) {
        if (!isCommandEnabled(ActionProvider.COMMAND_TEST)) {
            showMessage(
                    "Flutter test rerun unavailable",
                    "Cannot rerun Flutter tests for " + projectRoot + ": "
                    + unavailableReason(ActionProvider.COMMAND_TEST, null) + ".",
                    NotifyDescriptor.WARNING_MESSAGE);
            return;
        }
        start(new ToolingPlan("Test", List.of(command)), null);
    }

    private void complete(
            ActiveOperation operation,
            int exitCode,
            boolean cancelled,
            Throwable failure,
            boolean success) {
        boolean current;
        synchronized (lock) {
            current = active == operation
                    && lifecycleGeneration == operation.generation
                    && !closed;
            if (active == operation) {
                active = null;
            }
        }
        if (!operation.complete(exitCode, cancelled, failure, success && current)) {
            return;
        }
        fireChange();
        if (current && failure != null) {
            showMessage(
                    "Flutter " + operation.plan.operationName() + " failed to start",
                    "Cannot run Flutter " + operation.plan.operationName()
                    + " for " + projectRoot + " using " + executableDescription()
                    + " during " + operation.stageDescription()
                    + ": " + failureMessage(failure) + ".",
                    NotifyDescriptor.ERROR_MESSAGE);
        } else if (current
                && !cancelled
                && failure == null
                && exitCode != 0
                && operation.reportsNonzeroExit()) {
            showMessage(
                    "Flutter " + operation.plan.operationName() + " failed",
                    "Flutter " + operation.plan.operationName() + " for " + projectRoot
                    + " failed during " + operation.stageDescription()
                    + " with exit code " + exitCode + ". See the Flutter "
                    + operation.plan.operationName() + " Output tab for details.",
                    NotifyDescriptor.ERROR_MESSAGE);
        }
    }

    private static FlutterSdk resolveFlutterSdk(String operation) throws IOException {
        FlutterToolchainStatus status = new FlutterToolchainService().resolve();
        return status.flutterSdk().orElseThrow(() -> new IOException(
                "cannot " + operation + " because " + status.flutterMessage()));
    }

    private boolean isCurrent(ActiveOperation operation) {
        synchronized (lock) {
            return isCurrentLocked(operation);
        }
    }

    private boolean isCurrentLocked(ActiveOperation operation) {
        return !closed
                && active == operation
                && lifecycleGeneration == operation.generation;
    }

    private String unavailableReason(String command, FlutterDevice buildTarget) {
        if (closed) {
            return "the Flutter project is closed";
        }
        if (!project.getProjectDirectory().isValid()) {
            return "the project directory no longer exists";
        }
        ActiveOperation operation = active;
        if (operation != null) {
            return "Flutter " + operation.plan.operationName() + " is already running";
        }
        if (requiresBuildTarget(command)) {
            if (buildTarget == null) {
                return "select an active Flutter target in the project toolbar";
            }
            try {
                FlutterToolCommand.build(buildTarget);
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
        }
        return "the tooling service is unavailable";
    }

    private String executableDescription() {
        return new FlutterToolchainService().resolve().flutterSdk()
                .map(sdk -> sdk.flutterExecutable().toString())
                .orElse("the configured Flutter SDK");
    }

    private static boolean isToolingCommand(String command) {
        if (command == null) {
            return false;
        }
        return switch (command) {
            case ActionProvider.COMMAND_BUILD,
                 ActionProvider.COMMAND_CLEAN,
                 ActionProvider.COMMAND_REBUILD,
                 FlutterProjectActionProvider.COMMAND_PUB_GET,
                 FlutterProjectActionProvider.COMMAND_ANALYZE,
                 FlutterProjectActionProvider.COMMAND_TEST_FILE,
                 FlutterProjectActionProvider.COMMAND_TEST_AT_CARET,
                 ActionProvider.COMMAND_TEST,
                 ActionProvider.COMMAND_TEST_SINGLE,
                 SingleMethod.COMMAND_RUN_SINGLE_METHOD -> true;
            default -> false;
        };
    }

    private static String displayName(String command) {
        return "Flutter " + operationName(command);
    }

    private static String operationName(String command) {
        return switch (command) {
            case ActionProvider.COMMAND_BUILD -> "Build";
            case ActionProvider.COMMAND_CLEAN -> "Clean";
            case ActionProvider.COMMAND_REBUILD -> "Clean and Build";
            case FlutterProjectActionProvider.COMMAND_PUB_GET -> "Pub Get";
            case FlutterProjectActionProvider.COMMAND_ANALYZE -> "Analyze";
            case FlutterProjectActionProvider.COMMAND_TEST_FILE,
                 ActionProvider.COMMAND_TEST_SINGLE -> "Test Current File";
            case FlutterProjectActionProvider.COMMAND_TEST_AT_CARET -> "Test at Caret";
            case SingleMethod.COMMAND_RUN_SINGLE_METHOD -> "Test Method";
            case ActionProvider.COMMAND_TEST -> "Test";
            default -> command;
        };
    }

    private static boolean requiresBuildTarget(String command) {
        return ActionProvider.COMMAND_BUILD.equals(command)
                || ActionProvider.COMMAND_REBUILD.equals(command);
    }

    private static boolean supportsBuildTarget(FlutterDevice buildTarget) {
        try {
            FlutterToolCommand.build(buildTarget);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void fireChange() {
        if (EventQueue.isDispatchThread()) {
            changes.fireChange();
        } else {
            EventQueue.invokeLater(changes::fireChange);
        }
    }

    private static void showMessage(String title, String message, int type) {
        Runnable display = () -> {
            NotifyDescriptor descriptor = new NotifyDescriptor.Message(message, type);
            descriptor.setTitle(title);
            DialogDisplayer.getDefault().notify(descriptor);
        };
        if (EventQueue.isDispatchThread()) {
            display.run();
        } else {
            EventQueue.invokeLater(display);
        }
    }

    private static String failureMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }

    private static void finishProgress(ActionProgress progress, boolean success) {
        if (progress != null) {
            progress.finished(success);
        }
    }

    private static final class ActiveOperation {
        private final ToolingPlan plan;
        private final long generation;
        private final ActionProgress progress;
        private final AtomicBoolean completed = new AtomicBoolean();
        private volatile FlutterExecutionHandle execution;
        private volatile FlutterToolCommand currentCommand;
        private volatile FlutterTestSessionBridge testSession = FlutterTestSessionBridge.NONE;

        ActiveOperation(
                ToolingPlan plan,
                long generation,
                ActionProgress progress) {
            this.plan = plan;
            this.generation = generation;
            this.progress = progress;
        }

        String stageDescription() {
            FlutterToolCommand command = currentCommand;
            if (command == null) {
                return plan.operationName();
            }
            if (command.type() == FlutterToolCommandType.BUILD
                    && command.arguments().size() > 1) {
                return "Build (" + command.arguments().get(1) + ")";
            }
            return command.type().displayName();
        }

        boolean reportsNonzeroExit() {
            FlutterToolCommand command = currentCommand;
            return command != null && switch (command.type()) {
                case CLEAN, BUILD, PUB_GET -> true;
                default -> false;
            };
        }

        boolean complete(
                int exitCode,
                boolean cancelled,
                Throwable failure,
                boolean success) {
            if (!completed.compareAndSet(false, true)) {
                return false;
            }
            try {
                testSession.finish(exitCode, cancelled, failure);
            } finally {
                finishProgress(progress, success && !cancelled && failure == null);
            }
            return true;
        }
    }

    private record ToolingPlan(String operationName, List<FlutterToolCommand> commands) {
        ToolingPlan {
            Objects.requireNonNull(operationName, "operationName");
            if (operationName.isBlank()) {
                throw new IllegalArgumentException("operationName must not be blank");
            }
            commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
            if (commands.isEmpty()) {
                throw new IllegalArgumentException("Flutter tooling plan must not be empty");
            }
        }
    }
}
