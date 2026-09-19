package io.github.vgrytsenko2022.plugin.project;

import io.github.vgrytsenko2022.api.DartSdk;
import io.github.vgrytsenko2022.api.FlutterDevice;
import io.github.vgrytsenko2022.api.FlutterProjectInfo;
import io.github.vgrytsenko2022.api.FlutterSdk;
import io.github.vgrytsenko2022.api.RunState;
import io.github.vgrytsenko2022.plugin.lifecycle.AsyncTaskTracker;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainService;
import io.github.vgrytsenko2022.plugin.settings.ModulePreferences;
import io.github.vgrytsenko2022.project.FlutterProjectPlatform;
import io.github.vgrytsenko2022.project.FlutterProjectPlatformResolver;
import io.github.vgrytsenko2022.project.FlutterProjectType;
import io.github.vgrytsenko2022.project.FlutterProjectTypeDetector;
import io.github.vgrytsenko2022.run.DevToolsLauncher;
import io.github.vgrytsenko2022.run.DevToolsSession;
import io.github.vgrytsenko2022.run.FlutterDeviceService;
import io.github.vgrytsenko2022.run.FlutterEmulator;
import io.github.vgrytsenko2022.run.FlutterEmulatorDeviceMatcher;
import io.github.vgrytsenko2022.run.FlutterEmulatorService;
import io.github.vgrytsenko2022.run.FlutterRunManager;
import io.github.vgrytsenko2022.run.FlutterRunSession;
import io.github.vgrytsenko2022.run.FlutterTargetKind;
import io.github.vgrytsenko2022.sdk.FlutterCli;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ActionProgress;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.HtmlBrowser;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/** Project-scoped orchestration of targets, Flutter processes and debugger sessions. */
public final class FlutterRunController implements AutoCloseable {
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterRunController.class.getName(), 4, true);
    private static final Duration EMULATOR_WAIT = Duration.ofSeconds(90);
    private static final String PREF_DEVICE_ID = "selectedDeviceId";
    private static final String PREF_DEVICE_NAME = "selectedDeviceName";
    private static final String PREF_TARGET_KIND = "selectedTargetKind";
    private static final String PREF_PROJECT_MIGRATED = "selectedTargetPreferencesMigrated";

    private final FlutterProject project;
    private final FlutterProjectInfo projectInfo;
    private final Path projectRoot;
    private final Dependencies dependencies;
    private final AsyncTaskTracker quiescenceTasks;
    private final Executor worker;
    private final ChangeSupport changes = new ChangeSupport(this);
    private volatile Preferences preferences;
    private final Object lock = new Object();
    private final Object outputLock = new Object();
    private final Object devToolsOutputLock = new Object();
    private final Object targetLock = new Object();
    private final ReentrantLock targetDiscoveryLock = new ReentrantLock();
    private final Set<ActionProgressCompletion> pendingActionProgress = new HashSet<>();
    private final AtomicLong targetQuerySequence = new AtomicLong();

    private volatile RunSession session;
    private volatile DebugLauncher dapLauncher;
    private volatile RunSession dapSession;
    private volatile SessionProgress sessionProgress;
    private volatile OutputTab output;
    private volatile DevToolsServer devToolsSession;
    private volatile RunSession devToolsOwner;
    private volatile DevToolsProgress devToolsProgress;
    private volatile OutputTab devToolsOutput;
    private volatile PendingOperation pendingOperation;
    private volatile boolean closed;
    private volatile List<FlutterDevice> availableTargets = List.of();
    private long publishedTargetQuery;
    private volatile long lifecycleGeneration;

    FlutterRunController(FlutterProject project, FlutterProjectInfo projectInfo) {
        this(project, projectInfo, new Dependencies());
    }

    FlutterRunController(
            FlutterProject project,
            FlutterProjectInfo projectInfo,
            Dependencies dependencies) {
        this.project = project;
        this.projectInfo = projectInfo;
        this.projectRoot = projectInfo.root().toAbsolutePath().normalize();
        this.dependencies = java.util.Objects.requireNonNull(dependencies, "dependencies");
        Executor executor = java.util.Objects.requireNonNull(
                dependencies.executor(), "executor");
        this.quiescenceTasks = new AsyncTaskTracker(executor);
        this.worker = quiescenceTasks;
    }

    public boolean isCommandEnabled(String command) {
        if (closed || !project.getProjectDirectory().isValid()) {
            return false;
        }
        RunSession current = session;
        RunState state = current == null ? RunState.STOPPED : current.state();
        return switch (command) {
            case FlutterProjectActionProvider.COMMAND_SELECT_TARGET ->
                pendingOperation == null && isTerminal(state);
            case FlutterProjectActionProvider.COMMAND_LAUNCH_EMULATOR ->
                pendingOperation == null && isTerminal(state)
                        && hasConfiguredMobilePlatform();
            case ActionProvider.COMMAND_RUN,
                 ActionProvider.COMMAND_DEBUG ->
                pendingOperation == null && (isTerminal(state) || isRestartable(state));
            case FlutterProjectActionProvider.COMMAND_HOT_RELOAD,
                  FlutterProjectActionProvider.COMMAND_HOT_RESTART -> state == RunState.RUNNING;
            case FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS ->
                canOpenDevTools(current, state);
            case FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS -> devToolsProgress != null;
            case FlutterProjectActionProvider.COMMAND_STOP ->
                state == RunState.STARTING || state == RunState.RUNNING;
            default -> false;
        };
    }

    public void invoke(String command) {
        invoke(command, null, null);
    }

    void invoke(String command, ActionProgress actionProgress) {
        invoke(command, actionProgress, null);
    }

    void invoke(
            String command,
            ActionProgress actionProgress,
            FlutterDevice requestedTarget) {
        if (!isCommandEnabled(command)) {
            finishActionProgress(actionProgress, false);
            showMessage(
                    "Flutter action unavailable",
                     "Cannot perform " + actionName(command) + " for " + projectRoot
                    + ": " + unavailableReason(command) + ".",
                    NotifyDescriptor.WARNING_MESSAGE);
            return;
        }
        switch (command) {
            case FlutterProjectActionProvider.COMMAND_SELECT_TARGET ->
                submitExclusive("select Flutter run target", this::selectTarget);
            case FlutterProjectActionProvider.COMMAND_LAUNCH_EMULATOR ->
                submitExclusive("launch mobile emulator", this::launchMobileEmulator);
            case ActionProvider.COMMAND_RUN ->
                startOrRestart(false, actionProgress, requestedTarget);
            case ActionProvider.COMMAND_DEBUG ->
                startOrRestart(true, actionProgress, requestedTarget);
            case FlutterProjectActionProvider.COMMAND_HOT_RELOAD ->
                control("Hot Reload", RunSession::hotReload);
            case FlutterProjectActionProvider.COMMAND_HOT_RESTART ->
                control("Hot Restart", RunSession::hotRestart);
            case FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS -> openDevTools();
            case FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS -> stopDevTools();
            case FlutterProjectActionProvider.COMMAND_STOP -> stop();
            default -> throw new IllegalArgumentException("Unsupported Flutter run command: " + command);
        }
    }

    public String selectedTargetLabel() {
        Preferences settings = preferences();
        String id = settings.get(PREF_DEVICE_ID, "");
        if (id.isBlank()) {
            return "No Flutter run target selected";
        }
        String name = settings.get(PREF_DEVICE_NAME, id);
        String kind = settings.get(PREF_TARGET_KIND, FlutterTargetKind.UNKNOWN.name());
        return kindLabel(kind) + ": " + name + " (" + id + ")";
    }

    /** Returns the stable id currently selected in the NetBeans target toolbar. */
    public String selectedTargetId() {
        return preferences().get(PREF_DEVICE_ID, "");
    }

    /** Refreshes Flutter devices and publishes the resulting toolbar snapshot. */
    public List<FlutterDevice> discoverTargets() throws IOException, InterruptedException {
        FlutterSdk sdk = requireFlutterSdk("list Flutter run targets");
        return queryDevices(sdk);
    }

    List<FlutterDevice> availableTargets() {
        return availableTargets;
    }

    /** Selects an exact discovered Flutter device and updates the toolbar configuration. */
    public void selectTarget(FlutterDevice device) {
        if (closed) {
            return;
        }
        remember(java.util.Objects.requireNonNull(device, "device"));
    }

    void open() {
        synchronized (lock) {
            if (!closed) {
                return;
            }
            closed = false;
            lifecycleGeneration++;
        }
        fireChange();
    }

    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }

    private void selectTarget(PendingOperation operation) throws Exception {
        operation.checkCancelled();
        FlutterSdk sdk = requireFlutterSdk("select a Flutter run target");
        List<FlutterDevice> devices = listDevices(sdk);
        operation.checkCancelled();
        FlutterDevice selected = chooseDevice(devices, "Select Flutter Run Target").orElse(null);
        operation.checkCancelled();
        if (selected != null) {
            remember(operation, selected);
            showMessage(
                    operation.lifecycle(),
                    "Flutter run target",
                    "Selected " + deviceLabel(selected) + " for Flutter project " + projectRoot + ".",
                    NotifyDescriptor.INFORMATION_MESSAGE);
        }
    }

    private void launchMobileEmulator(PendingOperation operation) throws Exception {
        MobileEmulatorProgress progress = new MobileEmulatorProgress(operation);
        FlutterEmulator selected = null;
        boolean launchIssued = false;
        progress.start();
        try {
            operation.checkCancelled();
            requireMobilePlatformConfigured();
            progress.update("Loading", "Loading configured mobile emulators");
            FlutterSdk sdk = requireFlutterSdk("launch a mobile emulator");
            FlutterCli cli = new FlutterCli(sdk);
            FlutterEmulatorService emulators = new FlutterEmulatorService(cli);
            List<FlutterEmulator> reported = dependencies.listEmulators(sdk, projectRoot);
            Set<FlutterProjectPlatform> configured = configuredRunPlatforms();
            List<FlutterEmulator> available = compatibleMobileEmulators(
                    reported,
                    configured).stream()
                    .sorted(Comparator.comparing(
                            FlutterEmulator::name,
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();
            operation.checkCancelled();
            if (available.isEmpty()) {
                throw new IOException(noCompatibleMobileEmulatorMessage(configured));
            }
            selected = dependencies.chooseMobileEmulator(
                    projectInfo.name(),
                    available).orElse(null);
            if (selected == null) {
                return;
            }
            operation.checkCancelled();
            requireEmulatorPlatformConfigured(selected);
            progress.select(selected);
            openOutput("Flutter Emulator: " + selected.name(), operation.lifecycle());

            progress.update("Checking devices", "Capturing connected devices before launch");
            FlutterDeviceService devices = new FlutterDeviceService(cli);
            Set<String> before = new HashSet<>();
            queryDevices(devices).forEach(device -> before.add(device.id()));
            operation.checkCancelled();

            progress.update("Launching", "Starting " + selected.name() + " (" + selected.id() + ")");
            writeLine(operation.lifecycle(), "Launching mobile emulator " + selected.name()
                    + " (" + selected.id() + ")...");
            requireEmulatorPlatformConfigured(selected);
            operation.checkCancelled();
            launchIssued = true;
            emulators.launch(projectRoot, selected.id());
            operation.checkCancelled();

            FlutterDevice device = waitForMobileDevice(
                    devices,
                    before,
                    selected,
                    operation,
                    progress);
            operation.checkCancelled();
            progress.update("Selecting target", "Selecting " + device.name() + " for Run and Debug");
            remember(operation, device);
            progress.update("Ready", deviceLabel(device));
            writeLine(operation.lifecycle(),
                    "Mobile emulator is ready: " + deviceLabel(device));
            showMessage(
                    operation.lifecycle(),
                    "Mobile emulator ready",
                    "Started " + selected.name() + " and selected " + deviceLabel(device)
                    + " for Run and Debug.",
                    NotifyDescriptor.INFORMATION_MESSAGE);
        } catch (OperationCancelledException ex) {
            if (selected != null) {
                writeEmulatorCancellation(operation.lifecycle(), selected, launchIssued);
            }
            throw ex;
        } catch (InterruptedException ex) {
            if (operation.isCancelled()) {
                if (selected != null) {
                    writeEmulatorCancellation(operation.lifecycle(), selected, launchIssued);
                }
                throw new OperationCancelledException();
            }
            throw ex;
        } finally {
            progress.finish();
        }
    }

    private FlutterDevice waitForMobileDevice(
            FlutterDeviceService devices,
            Set<String> before,
            FlutterEmulator emulator,
            PendingOperation operation,
            MobileEmulatorProgress progress) throws Exception {
        long startedAt = System.nanoTime();
        long deadline = System.nanoTime() + EMULATOR_WAIT.toNanos();
        int attempt = 0;
        IOException lastDiscoveryFailure = null;
        while (System.nanoTime() < deadline) {
            operation.checkCancelled();
            long elapsedSeconds = Duration.ofNanos(
                    System.nanoTime() - startedAt).toSeconds();
            progress.update(
                    "Waiting for device",
                    "Waiting for " + emulator.name() + " — " + elapsedSeconds
                    + "/" + EMULATOR_WAIT.toSeconds() + " s");
            try {
                List<FlutterDevice> current = queryDevices(devices);
                Optional<FlutterDevice> match = FlutterEmulatorDeviceMatcher.match(
                        emulator,
                        before,
                        current);
                if (match.isPresent()) {
                    return match.get();
                }
                lastDiscoveryFailure = null;
            } catch (IOException ex) {
                lastDiscoveryFailure = ex;
            }
            if (++attempt % 5 == 0) {
                String detail = lastDiscoveryFailure == null
                        ? ""
                        : " Last discovery error: " + messageOf(lastDiscoveryFailure);
                writeLine(operation.lifecycle(), "Waiting for " + emulator.name() + " ("
                        + emulator.id()
                        + ") to appear in flutter devices..." + detail);
            }
            operation.checkCancelled();
            Thread.sleep(2_000);
        }
        String lastError = lastDiscoveryFailure == null
                ? ""
                : " Last device-discovery error: " + messageOf(lastDiscoveryFailure) + ".";
        throw new IOException("emulator " + emulator.name() + " (" + emulator.id() + ", "
                + emulator.platform() + ") was launched, but NetBeans could not uniquely match "
                + "its Flutter device within " + EMULATOR_WAIT.toSeconds() + " seconds."
                + lastError);
    }

    private void writeEmulatorCancellation(
            long lifecycle,
            FlutterEmulator emulator,
            boolean launchIssued) {
        String suffix = launchIssued
                ? " The emulator launch was already requested and the emulator may continue starting."
                : " The emulator launch command was not sent.";
        writeLine(lifecycle, "Stopped waiting for mobile emulator " + emulator.name() + " ("
                + emulator.id() + ")." + suffix);
    }

    private void startOrRestart(
            boolean debug,
            ActionProgress actionProgress,
            FlutterDevice requestedTarget) {
        RunState state = currentState();
        if (!isRestartable(state)) {
            start(debug, actionProgress, requestedTarget);
            return;
        }
        final boolean confirmed;
        try {
            confirmed = confirmRestart(debug, state, requestedTarget);
        } catch (Exception ex) {
            finishActionProgress(actionProgress, false);
            reportFailure("confirm Flutter restart", messageOf(ex));
            return;
        }
        if (!confirmed) {
            finishActionProgress(actionProgress, false);
            return;
        }
        restart(debug, actionProgress, requestedTarget);
    }

    private boolean confirmRestart(
            boolean debug,
            RunState state,
            FlutterDevice requestedTarget) throws Exception {
        return callOnEdt(() -> {
            String mode = debug ? "Debug" : "Run";
            String destination = requestedTarget == null
                    ? selectedTargetLabel()
                    : deviceLabel(requestedTarget);
            NotifyDescriptor.Confirmation descriptor = new NotifyDescriptor.Confirmation(
                    "Flutter application " + projectInfo.name() + " is already "
                    + state.name().toLowerCase(java.util.Locale.ROOT) + " on "
                    + currentSessionTargetLabel() + ". Stop the current session and restart it in "
                    + mode + " mode on " + destination + "?",
                    "Restart Flutter Application",
                    NotifyDescriptor.YES_NO_OPTION,
                    NotifyDescriptor.QUESTION_MESSAGE);
            return dependencies.confirmRestart(descriptor);
        });
    }

    private void restart(
            boolean debug,
            ActionProgress actionProgress,
            FlutterDevice requestedTarget) {
        RunSession current = session;
        if (current == null || isTerminal(current.state())) {
            start(debug, actionProgress, requestedTarget);
            return;
        }
        ActionProgressCompletion actionCompletion = new ActionProgressCompletion(actionProgress);
        boolean accepted = submitExclusive(
                debug ? "restart Flutter project in Debug mode" : "restart Flutter project in Run mode",
                operation -> {
                    saveModifiedProjectDataObjects(operation);
                    writeLine(operation.lifecycle(), "Restarting " + projectInfo.name() + " in "
                            + (debug ? "Debug" : "Run") + " mode...");
                    failPendingAction(current);
                    DebugLauncher launcher = detachDebugger(current, null);
                    if (launcher != null) {
                        launcher.close();
                    }
                    current.quit();
                    try {
                        current.exitCode().get(12, TimeUnit.SECONDS);
                    } catch (TimeoutException ex) {
                        current.close();
                        throw new IOException("the existing Flutter session did not stop within 12 seconds", ex);
                    }
                    startSession(
                            debug,
                            actionCompletion,
                            requestedTarget,
                            operation);
                },
                () -> actionCompletion.finish(false));
        if (!accepted) {
            actionCompletion.finish(false);
        }
    }

    private void start(
            boolean debug,
            ActionProgress actionProgress,
            FlutterDevice requestedTarget) {
        ActionProgressCompletion actionCompletion = new ActionProgressCompletion(actionProgress);
        boolean accepted = submitExclusive(
                debug ? "debug Flutter project" : "run Flutter project",
                operation -> startSession(
                        debug,
                        actionCompletion,
                        requestedTarget,
                        operation),
                () -> actionCompletion.finish(false));
        if (!accepted) {
            actionCompletion.finish(false);
        }
    }

    private void startSession(
            boolean debug,
            ActionProgressCompletion actionCompletion,
            FlutterDevice requestedTarget,
            PendingOperation operation)
            throws Exception {
        long lifecycle = operation.lifecycle();
        FlutterSdk sdk = requireFlutterSdk(
                debug ? "debug the Flutter project" : "run the Flutter project");
        FlutterDevice target = requireTarget(sdk, requestedTarget);
        File projectDirectory = FileUtil.toFile(project.getProjectDirectory());
        if (projectDirectory == null || !projectDirectory.isDirectory()) {
            throw new IOException("project directory is not a local readable folder: " + projectRoot);
        }
        Path main = projectRoot.resolve("lib").resolve("main.dart");
        if (!main.toFile().isFile()) {
            throw new IOException("Flutter entry point was not found: " + main);
        }
        requireTargetPlatformConfigured(target);

        if (!openOutput(
                "Flutter: " + projectInfo.name() + " — " + target.name(),
                lifecycle)) {
            actionCompletion.finish(false);
            return;
        }
        if (!isLifecycleActive(lifecycle)) {
            actionCompletion.finish(false);
            return;
        }

        saveModifiedProjectDataObjects(operation);
        RunSession created = dependencies.startSession(sdk, projectRoot, target.id(), debug);
        writeLine(lifecycle, (debug ? "Debugging " : "Running ") + projectInfo.name()
                + " on " + deviceLabel(target) + "...");
        SessionProgress progress = null;
        synchronized (lock) {
            if (isLifecycleActive(lifecycle)) {
                progress = new SessionProgress(
                        created,
                        target,
                        debug,
                        actionCompletion,
                        lifecycle);
                session = created;
                sessionProgress = progress;
            }
        }
        if (progress == null) {
            created.close();
            actionCompletion.finish(false);
            return;
        }
        SessionProgress activeProgress = progress;
        created.addOutputListener(line -> writeSessionLine(created, lifecycle, line));
        activeProgress.start();
        created.addStateListener(state -> onSessionState(created, activeProgress, state));
        fireChange();
        created.exitCode().whenCompleteAsync(
                 (code, error) -> onSessionExit(created, activeProgress, code, error), worker);
        created.vmServiceUri().whenComplete((uri, error) -> {
            if (!closed && session == created) {
                fireChange();
            }
        });
        if (debug) {
            attachDebugger(created, sdk, target.id(), lifecycle);
        }
    }

    private void saveModifiedProjectDataObjects(PendingOperation operation)
            throws Exception {
        operation.checkCancelled();
        dependencies.saveModifiedProjectDataObjects(project);
        operation.checkCancelled();
    }

    private void attachDebugger(
            RunSession debugSession,
            FlutterSdk sdk,
            String deviceId,
            long lifecycle) {
        debugSession.vmServiceUri()
                .orTimeout(2, TimeUnit.MINUTES)
                .whenCompleteAsync((uri, error) -> {
                    RunState state = debugSession.state();
                    if (closed || session != debugSession || isTerminal(state)
                            || state == RunState.STOPPING) {
                        return;
                    }
                    if (error != null) {
                        failDebugAttach(debugSession, lifecycle,
                                "Flutter did not expose a VM service: "
                                + messageOf(error));
                        return;
                    }
                    AtomicReference<DebugLauncher> launcherReference = new AtomicReference<>();
                    DebugLauncher launcher = dependencies.createDebugLauncher(
                            line -> writeDebuggerLine(
                                    debugSession,
                                    lifecycle,
                                    launcherReference.get(),
                                    line),
                            exitCode -> onDebuggerAdapterExit(
                                    debugSession,
                                    lifecycle,
                                    launcherReference.get(),
                                    exitCode));
                    launcherReference.set(launcher);
                    try {
                        DebugLauncher previous;
                        synchronized (lock) {
                            state = debugSession.state();
                            if (closed || session != debugSession || isTerminal(state)
                                    || state == RunState.STOPPING) {
                                launcher.close();
                                return;
                            }
                            previous = dapLauncher;
                            dapLauncher = launcher;
                            dapSession = debugSession;
                        }
                        if (previous != null) {
                            previous.close();
                        }
                        writeSessionLine(debugSession, lifecycle,
                                "Connecting NetBeans debugger to " + uri + "...");
                        launcher.attach(sdk, projectRoot, projectInfo.name(), deviceId, uri);
                        SessionProgress progress = null;
                        synchronized (lock) {
                            state = debugSession.state();
                            if (dapLauncher == launcher && session == debugSession
                                    && dapSession == debugSession && !closed
                                    && !isTerminal(state) && state != RunState.STOPPING) {
                                SessionProgress candidate = sessionProgress;
                                if (candidate != null && candidate.session == debugSession) {
                                    progress = candidate;
                                }
                            }
                        }
                        if (progress != null && progress.markDebuggerReady()) {
                            writeSessionLine(debugSession, lifecycle,
                                    "NetBeans debugger connected for device '" + deviceId
                                    + "'. Breakpoints, stepping and variables are available.");
                        }
                    } catch (IOException ex) {
                        DebugLauncher detached = detachDebugger(debugSession, launcher);
                        launcher.close();
                        if (detached != launcher) {
                            return;
                        }
                        state = debugSession.state();
                        if (!closed && session == debugSession && !isTerminal(state)
                                && state != RunState.STOPPING) {
                            failDebugAttach(debugSession, lifecycle, messageOf(ex));
                        }
                    }
                }, worker);
    }

    private void onDebuggerAdapterExit(
            RunSession debugSession,
            long lifecycle,
            DebugLauncher expectedLauncher,
            int exitCode) {
        if (expectedLauncher == null) {
            return;
        }
        DebugLauncher detached = detachDebugger(debugSession, expectedLauncher);
        RunState state = debugSession.state();
        if (detached == null || closed || session != debugSession
                || isTerminal(state) || state == RunState.STOPPING) {
            return;
        }
        SessionProgress progress = sessionProgress;
        if (progress != null && progress.session == debugSession
                && progress.failDebuggerBeforeReady()) {
            failDebugAttach(debugSession, lifecycle,
                    "Flutter debug adapter exited before the debugger became ready (code "
                    + exitCode + ")");
            return;
        }
        if (exitCode == 0) {
            writeSessionLine(debugSession, lifecycle,
                    "Flutter debug adapter finished while the application is still running.");
            return;
        }
        failDebugAttach(debugSession, lifecycle,
                "Flutter debug adapter exited unexpectedly with code " + exitCode);
    }

    private DebugLauncher detachDebugger(
            RunSession owner,
            DebugLauncher expected) {
        synchronized (lock) {
            if (owner != null && dapSession != owner) {
                return null;
            }
            if (expected != null && dapLauncher != expected) {
                return null;
            }
            DebugLauncher launcher = dapLauncher;
            dapLauncher = null;
            dapSession = null;
            return launcher;
        }
    }

    private void failDebugAttach(
            RunSession debugSession,
            long lifecycle,
            String reason) {
        SessionProgress progress = sessionProgress;
        if (progress != null && progress.session == debugSession) {
            progress.markFailed();
        }
        writeSessionLine(debugSession, lifecycle, "Debug failed: " + reason);
        showMessage(
                lifecycle,
                "Flutter Debug failed",
                "Cannot debug Flutter project " + projectRoot + ": " + reason,
                NotifyDescriptor.ERROR_MESSAGE);
        try {
            debugSession.quit();
        } catch (IOException ex) {
            debugSession.close();
        }
    }

    private FlutterDevice requireTarget(
            FlutterSdk sdk,
            FlutterDevice requestedTarget) throws Exception {
        if (requestedTarget != null) {
            requireTargetPlatformConfigured(requestedTarget);
        }
        List<FlutterDevice> devices = listDevices(sdk);
        if (requestedTarget != null) {
            return devices.stream()
                    .filter(device -> device.id().equals(requestedTarget.id()))
                    .findFirst()
                    .orElseThrow(() -> new IOException(
                            "Flutter run target " + deviceLabel(requestedTarget)
                            + " is no longer connected. Refresh the target list and choose "
                            + "an available device."));
        }
        String selectedId = preferences().get(PREF_DEVICE_ID, "");
        Optional<FlutterDevice> remembered = devices.stream()
                .filter(device -> device.id().equals(selectedId))
                .findFirst();
        if (remembered.isPresent()) {
            return remembered.get();
        }
        FlutterDevice selected;
        if (devices.size() == 1) {
            selected = devices.getFirst();
        } else {
            selected = chooseDevice(devices, "Select Flutter Run Target").orElseThrow(() ->
                    new OperationCancelledException());
        }
        remember(selected);
        return selected;
    }

    private List<FlutterDevice> listDevices(FlutterSdk sdk) throws IOException, InterruptedException {
        List<FlutterDevice> devices = queryDevices(sdk);
        if (devices.isEmpty()) {
            throw new IOException(noCompatibleTargetsMessage());
        }
        return devices;
    }

    private List<FlutterDevice> queryDevices(FlutterSdk sdk) throws IOException, InterruptedException {
        return queryDevices(root -> dependencies.listDevices(sdk, root));
    }

    private List<FlutterDevice> queryDevices(FlutterDeviceService service)
            throws IOException, InterruptedException {
        return queryDevices(service::list);
    }

    private List<FlutterDevice> queryDevices(DeviceQuery queryService)
            throws IOException, InterruptedException {
        long requestedLifecycle = lifecycleGeneration;
        targetDiscoveryLock.lockInterruptibly();
        try {
            if (closed || requestedLifecycle != lifecycleGeneration) {
                throw new IOException("Flutter project " + projectRoot
                        + " was closed or reopened while waiting to discover devices");
            }
            long query = targetQuerySequence.incrementAndGet();
            List<FlutterDevice> reported = queryService.list(projectRoot);
            Set<FlutterProjectPlatform> configured = configuredRunPlatforms();
            List<FlutterDevice> discovered = reported.stream()
                    .filter(java.util.Objects::nonNull)
                    .filter(device -> FlutterProjectPlatformResolver.platformFor(device)
                            .filter(configured::contains)
                            .isPresent())
                    .sorted(Comparator
                            .comparing((FlutterDevice device) ->
                                    FlutterTargetKind.from(device).ordinal())
                            .thenComparing(FlutterDevice::name, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(FlutterDevice::id, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            boolean changed = false;
            synchronized (targetLock) {
                if (!closed
                        && query == targetQuerySequence.get()
                        && query >= publishedTargetQuery) {
                    publishedTargetQuery = query;
                    changed = !availableTargets.equals(discovered);
                    availableTargets = discovered;
                }
            }
            if (changed) {
                fireChange();
            }
            return discovered;
        } finally {
            targetDiscoveryLock.unlock();
        }
    }

    private boolean hasConfiguredMobilePlatform() {
        try {
            Set<FlutterProjectPlatform> configured = configuredRunPlatforms();
            return configured.contains(FlutterProjectPlatform.ANDROID)
                    || configured.contains(FlutterProjectPlatform.IOS);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private void requireMobilePlatformConfigured() throws IOException {
        if (!hasConfiguredMobilePlatform()) {
            throw new IOException("Flutter project " + projectRoot
                    + " has no configured Android or iOS platform. Use Flutter > "
                    + "Add Flutter Platforms... before launching a mobile emulator.");
        }
    }

    private void requireTargetPlatformConfigured(FlutterDevice target) throws IOException {
        FlutterProjectPlatform required = FlutterProjectPlatformResolver.platformFor(target)
                .orElseThrow(() -> new IOException(
                        "Flutter run target " + deviceLabel(target)
                        + " cannot be mapped to a supported Flutter project platform."));
        Set<FlutterProjectPlatform> configured = configuredRunPlatforms();
        if (!configured.contains(required)) {
            throw new IOException("Flutter run target " + deviceLabel(target)
                    + " requires the " + required.displayName() + " project platform, but "
                    + projectRoot.resolve(required.id())
                    + " is not a configured platform directory. Use Flutter > "
                    + "Add Flutter Platforms... to add it before Run or Debug.");
        }
    }

    private void requireEmulatorPlatformConfigured(FlutterEmulator emulator) throws IOException {
        FlutterProjectPlatform required = emulatorProjectPlatform(emulator)
                .orElseThrow(() -> new IOException(
                        "Flutter emulator " + emulator.name() + " (" + emulator.id()
                        + ") has unsupported platform '" + emulator.platform() + "'."));
        Set<FlutterProjectPlatform> configured = configuredRunPlatforms();
        if (!configured.contains(required)) {
            throw new IOException("Selected Flutter emulator " + emulator.name()
                    + " (" + emulator.id() + ") requires the " + required.displayName()
                    + " project platform, but " + projectRoot.resolve(required.id())
                    + " is no longer a configured platform directory. Use Flutter > "
                    + "Add Flutter Platforms... before launching it.");
        }
    }

    private String noCompatibleTargetsMessage() throws IOException {
        FlutterProjectType projectType = FlutterProjectTypeDetector.detect(projectRoot);
        if (projectType != FlutterProjectType.APP) {
            return "Flutter project " + projectRoot + " has project type '"
                    + projectType.id() + "'. Run and Debug targets are available only for "
                    + "Flutter application projects.";
        }
        Set<FlutterProjectPlatform> configured = configuredRunPlatforms();
        if (configured.isEmpty()) {
            return "Flutter project " + projectRoot
                    + " has no configured Android, iOS, Web, Windows, macOS, or Linux "
                    + "platform directory. Use Flutter > Add Flutter Platforms... first.";
        }
        String configuredNames = FlutterProjectPlatform.ordered(configured).stream()
                .map(FlutterProjectPlatform::displayName)
                .collect(java.util.stream.Collectors.joining(", "));
        return "Flutter reported no connected run target compatible with the configured "
                + "project platforms: " + configuredNames + ". Connect a compatible device"
                + " or start an emulator. To enable another target type, use Flutter > "
                + "Add Flutter Platforms...";
    }

    private Set<FlutterProjectPlatform> configuredRunPlatforms() throws IOException {
        FlutterProjectType projectType = FlutterProjectTypeDetector.detect(projectRoot);
        return projectType == FlutterProjectType.APP
                ? FlutterProjectPlatformResolver.configuredPlatforms(projectRoot)
                : Set.of();
    }

    private Optional<FlutterDevice> chooseDevice(List<FlutterDevice> devices, String title)
            throws Exception {
        String selectedId = preferences().get(PREF_DEVICE_ID, "");
        return dependencies.chooseRunTarget(
                projectInfo.name(),
                title,
                devices,
                selectedId);
    }

    private static <T> Optional<T> choose(
            String label,
            String title,
            List<T> values,
            Function<T, String> formatter,
            java.util.function.Predicate<T> initiallySelected) throws Exception {
        return callOnEdt(() -> {
            List<NotifyDescriptor.QuickPick.Item> items = new ArrayList<>();
            Map<NotifyDescriptor.QuickPick.Item, T> mapping = new IdentityHashMap<>();
            boolean hasSelection = false;
            for (T value : values) {
                NotifyDescriptor.QuickPick.Item item = new NotifyDescriptor.QuickPick.Item(
                        formatter.apply(value), "");
                boolean selected = initiallySelected != null && initiallySelected.test(value);
                item.setSelected(selected);
                hasSelection |= selected;
                items.add(item);
                mapping.put(item, value);
            }
            if (!hasSelection && !items.isEmpty()) {
                items.getFirst().setSelected(true);
            }
            NotifyDescriptor.QuickPick descriptor = new NotifyDescriptor.QuickPick(
                    label, title, items, false);
            Object result = DialogDisplayer.getDefault().notify(descriptor);
            if (result != NotifyDescriptor.OK_OPTION) {
                return Optional.empty();
            }
            return items.stream()
                    .filter(NotifyDescriptor.QuickPick.Item::isSelected)
                    .findFirst()
                    .map(mapping::get);
        });
    }

    private void control(String operation, SessionCommand command) {
        control(operation, session, command);
    }

    private void control(
            String operation,
            RunSession current,
            SessionCommand command) {
        long lifecycle = lifecycleGeneration;
        worker.execute(() -> {
            if (!isLifecycleActive(lifecycle) || session != current) {
                return;
            }
            try {
                command.execute(current);
                writeLine(lifecycle,
                        operation + " requested for " + projectInfo.name() + ".");
                fireChange();
            } catch (IOException | IllegalStateException ex) {
                showMessage(
                        lifecycle,
                        "Flutter " + operation + " failed",
                        "Cannot perform " + operation + " for Flutter project " + projectRoot
                        + ": " + messageOf(ex),
                        NotifyDescriptor.ERROR_MESSAGE);
            }
        });
    }

    private void stop() {
        RunSession current = session;
        failPendingAction(current);
        control("Stop", current, RunSession::quit);
    }

    private boolean canOpenDevTools(RunSession current, RunState state) {
        if (current == null || state != RunState.RUNNING
                || current.currentVmServiceUri().isEmpty()) {
            return false;
        }
        DevToolsProgress progress = devToolsProgress;
        if (progress == null) {
            return true;
        }
        DevToolsServer server = devToolsSession;
        return devToolsOwner == current
                && !progress.stopRequested()
                && server != null
                && server.isAlive()
                && readyBrowserUri(server).isPresent();
    }

    private void openDevTools() {
        RunSession owner = session;
        URI vmServiceUri = owner == null
                ? null
                : owner.currentVmServiceUri().orElse(null);
        URI existingBrowser = null;
        DevToolsServer existingServer = null;
        DevToolsProgress existingProgress = null;
        DevToolsProgress createdProgress = null;
        String unavailable = null;
        synchronized (lock) {
            if (!isActiveRunSessionLocked(owner, vmServiceUri)) {
                unavailable = devToolsUnavailableReason(owner);
            } else if (devToolsProgress != null) {
                DevToolsServer existing = devToolsSession;
                if (devToolsOwner == owner && !devToolsProgress.stopRequested()
                        && existing != null && existing.isAlive()) {
                    existingBrowser = readyBrowserUri(existing).orElse(null);
                    existingServer = existing;
                    existingProgress = devToolsProgress;
                    if (existingBrowser == null) {
                        unavailable = "DevTools is still starting for " + projectInfo.name();
                    }
                } else {
                    unavailable = "the previous DevTools process is still stopping";
                }
            } else {
                createdProgress = new DevToolsProgress(owner);
                devToolsOwner = owner;
                devToolsProgress = createdProgress;
            }
        }
        if (existingBrowser != null) {
            writeDevToolsLine(existingProgress.lifecycle(),
                    "Reopening DevTools for " + projectInfo.name() + " at "
                    + existingBrowser + ".");
            showDevToolsBrowser(owner, existingServer, existingProgress, existingBrowser);
            return;
        }
        if (unavailable != null) {
            showMessage(
                    "Flutter DevTools unavailable",
                    "Cannot open DevTools for Flutter project " + projectRoot + ": "
                    + unavailable + ".",
                    NotifyDescriptor.WARNING_MESSAGE);
            return;
        }

        DevToolsProgress progress = createdProgress;
        if (!openDevToolsOutput(progress.lifecycle())) {
            finishCancelledDevToolsStart(owner, progress);
            return;
        }
        writeDevToolsLine(progress.lifecycle(), "Starting DevTools for " + projectInfo.name()
                + " and VM Service " + vmServiceUri + "...");
        progress.start();
        fireChange();
        worker.execute(() -> startDevTools(owner, vmServiceUri, progress));
    }

    private void startDevTools(
            RunSession owner,
            URI vmServiceUri,
            DevToolsProgress progress) {
        DevToolsServer created = null;
        try {
            ensureDevToolsStartCurrent(owner, vmServiceUri, progress);
            DartSdk dartSdk = requireDartSdk("start Flutter DevTools");
            writeDevToolsLine(progress.lifecycle(), "Launching " + dartSdk.dartExecutable()
                    + " devtools --machine on an automatic loopback port...");
            created = dependencies.startDevTools(dartSdk, projectRoot, vmServiceUri);
            synchronized (lock) {
                ensureDevToolsStartCurrentLocked(owner, vmServiceUri, progress);
                devToolsSession = created;
            }
            DevToolsServer server = created;
            server.addOutputListener(line -> writeDevToolsLine(
                    owner, server, progress, line));
            server.browserUri()
                    .orTimeout(30, TimeUnit.SECONDS)
                    .whenCompleteAsync((browserUri, error) -> onDevToolsReady(
                            owner, server, progress, browserUri, error), worker);
            server.exitCode().whenComplete(
                    (code, error) -> onDevToolsExit(owner, server, progress, code, error));
            fireChange();
        } catch (OperationCancelledException ex) {
            if (created != null) {
                created.close();
            }
            finishCancelledDevToolsStart(owner, progress);
        } catch (Exception ex) {
            if (created != null) {
                created.close();
            }
            if (progress.markFailed()) {
                reportDevToolsFailure(progress.lifecycle(), "start DevTools", messageOf(ex));
            }
            finishFailedDevToolsStart(owner, progress);
        }
    }

    private void onDevToolsReady(
            RunSession owner,
            DevToolsServer server,
            DevToolsProgress progress,
            URI browserUri,
            Throwable error) {
        if (!isCurrentDevTools(owner, server, progress)) {
            server.close();
            return;
        }
        if (progress.stopRequested() || server.stopRequested()) {
            progress.stopping();
            fireChange();
            return;
        }
        if (error != null) {
            progress.stopping();
            if (progress.markFailed()) {
                reportDevToolsFailure(
                        progress.lifecycle(),
                        "connect DevTools to the running Flutter application",
                        messageOf(error));
            }
            server.stop();
            fireChange();
            return;
        }
        if (!isActiveRunSession(owner, progress.vmServiceUri())) {
            progress.requestStop();
            progress.stopping();
            server.stop();
            fireChange();
            return;
        }
        if (!progress.running(browserUri)) {
            return;
        }
        writeDevToolsLine(progress.lifecycle(), "DevTools is serving " + projectInfo.name()
                + " at " + browserUri + ".");
        fireChange();
        showDevToolsBrowser(owner, server, progress, browserUri);
    }

    private void showDevToolsBrowser(
            RunSession owner,
            DevToolsServer server,
            DevToolsProgress progress,
            URI browserUri) {
        dependencies.invokeBrowserLater(() -> {
            if (!isCurrentDevTools(owner, server, progress)
                    || !isActiveRunSession(owner, progress.vmServiceUri())
                    || progress.stopRequested()
                    || server.stopRequested()
                    || !server.isAlive()) {
                return;
            }
            try {
                dependencies.showBrowser(browserUri);
            } catch (Exception ex) {
                writeDevToolsLine(progress.lifecycle(),
                        "Could not open the configured NetBeans browser at "
                        + browserUri + ": " + messageOf(ex));
                showMessage(
                        progress.lifecycle(),
                        "Cannot open Flutter DevTools browser",
                        "DevTools for Flutter project " + projectRoot + " is running at "
                        + browserUri + ", but NetBeans could not open the configured browser: "
                        + messageOf(ex) + ".",
                        NotifyDescriptor.ERROR_MESSAGE);
            }
        });
    }

    private void stopDevTools() {
        DevToolsServer server;
        DevToolsProgress progress;
        synchronized (lock) {
            server = devToolsSession;
            progress = devToolsProgress;
        }
        if (progress == null) {
            return;
        }
        if (progress.requestStop()) {
            progress.stopping();
            writeDevToolsLine(progress.lifecycle(),
                    "Stopping DevTools for " + projectInfo.name() + "...");
        }
        if (server != null) {
            server.stop();
        } else {
            finishCancelledDevToolsStart(devToolsOwner, progress);
        }
        fireChange();
    }

    private boolean requestDevToolsStopFromProgress(DevToolsProgress source) {
        DevToolsServer server;
        synchronized (lock) {
            if (devToolsProgress != source) {
                return false;
            }
            if (!source.requestStop()) {
                return false;
            }
            source.stopping();
            server = devToolsSession;
        }
        writeDevToolsLine(source.lifecycle(),
                "Stop requested from the NetBeans progress indicator for DevTools and "
                + projectInfo.name() + ".");
        if (server != null) {
            server.stop();
        } else {
            finishCancelledDevToolsStart(devToolsOwner, source);
        }
        fireChange();
        return true;
    }

    private void stopDevToolsFor(RunSession owner, String reason) {
        DevToolsServer server;
        DevToolsProgress progress;
        synchronized (lock) {
            if (devToolsOwner != owner || devToolsProgress == null) {
                return;
            }
            server = devToolsSession;
            progress = devToolsProgress;
            if (!progress.requestStop()) {
                return;
            }
            progress.stopping();
        }
        writeDevToolsLine(progress.lifecycle(), reason);
        if (server != null) {
            server.stop();
        } else {
            finishCancelledDevToolsStart(owner, progress);
        }
        fireChange();
    }

    private void onDevToolsExit(
            RunSession owner,
            DevToolsServer server,
            DevToolsProgress progress,
            Integer code,
            Throwable error) {
        synchronized (lock) {
            if (devToolsSession != server || devToolsOwner != owner
                    || devToolsProgress != progress) {
                return;
            }
        }
        progress.finish();
        if (progress.stopRequested() || server.stopRequested()) {
            writeDevToolsLine(progress.lifecycle(),
                    "DevTools stopped for " + projectInfo.name() + ".");
        } else if (!progress.failureReported()) {
            String reason;
            if (error != null) {
                reason = messageOf(error);
            } else if (!progress.ready()) {
                reason = "the DevTools process exited with code " + code
                        + " before its server became ready";
            } else if (code != null && code != 0) {
                reason = "the DevTools process exited unexpectedly with code " + code;
            } else {
                writeDevToolsLine(progress.lifecycle(),
                        "DevTools process finished with exit code " + code + ".");
                reason = null;
            }
            if (reason != null) {
                progress.markFailed();
                reportDevToolsFailure(progress.lifecycle(), "keep DevTools running", reason);
            }
        }
        synchronized (lock) {
            if (devToolsSession == server && devToolsOwner == owner
                    && devToolsProgress == progress) {
                devToolsSession = null;
                devToolsOwner = null;
                devToolsProgress = null;
            }
        }
        fireChange();
    }

    private void ensureDevToolsStartCurrent(
            RunSession owner,
            URI vmServiceUri,
            DevToolsProgress progress) throws OperationCancelledException {
        synchronized (lock) {
            ensureDevToolsStartCurrentLocked(owner, vmServiceUri, progress);
        }
    }

    private void ensureDevToolsStartCurrentLocked(
            RunSession owner,
            URI vmServiceUri,
            DevToolsProgress progress) throws OperationCancelledException {
        if (devToolsOwner != owner || devToolsProgress != progress
                || progress.stopRequested()
                || !isActiveRunSessionLocked(owner, vmServiceUri)) {
            throw new OperationCancelledException();
        }
    }

    private boolean isActiveRunSession(RunSession owner, URI vmServiceUri) {
        synchronized (lock) {
            return isActiveRunSessionLocked(owner, vmServiceUri);
        }
    }

    private boolean isActiveRunSessionLocked(RunSession owner, URI vmServiceUri) {
        return !closed
                && owner != null
                && session == owner
                && owner.state() == RunState.RUNNING
                && vmServiceUri != null
                && owner.currentVmServiceUri().filter(vmServiceUri::equals).isPresent();
    }

    private boolean isCurrentDevTools(
            RunSession owner,
            DevToolsServer server,
            DevToolsProgress progress) {
        synchronized (lock) {
            return !closed
                    && devToolsOwner == owner
                    && devToolsSession == server
                    && devToolsProgress == progress;
        }
    }

    private static Optional<URI> readyBrowserUri(DevToolsServer server) {
        try {
            return Optional.ofNullable(server.browserUri().getNow(null));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String devToolsUnavailableReason(RunSession owner) {
        if (closed || !project.getProjectDirectory().isValid()) {
            return "the Flutter project is closed or unavailable";
        }
        if (owner == null || owner.state() != RunState.RUNNING) {
            return "the Flutter application is not running";
        }
        if (owner.currentVmServiceUri().isEmpty()) {
            return "Flutter has not supplied a VM Service URI for the running application";
        }
        return "the active Flutter session changed before DevTools could start";
    }

    private DartSdk requireDartSdk(String operation) throws IOException {
        return dependencies.requireDartSdk(operation);
    }

    private void finishCancelledDevToolsStart(
            RunSession owner,
            DevToolsProgress progress) {
        synchronized (lock) {
            if (devToolsOwner != owner || devToolsProgress != progress
                    || devToolsSession != null) {
                return;
            }
        }
        progress.finish();
        writeDevToolsLine(progress.lifecycle(),
                "DevTools start was cancelled for " + projectInfo.name() + ".");
        synchronized (lock) {
            if (devToolsOwner == owner && devToolsProgress == progress
                    && devToolsSession == null) {
                devToolsOwner = null;
                devToolsProgress = null;
            }
        }
        fireChange();
    }

    private void finishFailedDevToolsStart(
            RunSession owner,
            DevToolsProgress progress) {
        synchronized (lock) {
            if (devToolsOwner != owner || devToolsProgress != progress
                    || devToolsSession != null) {
                return;
            }
            devToolsOwner = null;
            devToolsProgress = null;
        }
        progress.finish();
        fireChange();
    }

    private void reportDevToolsFailure(long lifecycle, String operation, String reason) {
        writeDevToolsLine(lifecycle,
                "Cannot " + operation + " for " + projectRoot + ": " + reason + ".");
        showMessage(
                lifecycle,
                "Flutter DevTools failed",
                "Cannot " + operation + " for Flutter project " + projectRoot + ": "
                + reason + ".",
                NotifyDescriptor.ERROR_MESSAGE);
    }

    private void failPendingAction(RunSession source) {
        SessionProgress progress = sessionProgress;
        if (progress != null && progress.session == source) {
            progress.markFailed();
        }
    }

    private void onSessionState(
            RunSession source,
            SessionProgress progress,
            RunState state) {
        progress.update(state);
        if (state == RunState.STOPPING || isTerminal(state)) {
            stopDevToolsFor(source, "Stopping DevTools because the Flutter application is "
                    + state.name().toLowerCase(java.util.Locale.ROOT) + ".");
        }
        if (session != source) {
            return;
        }
        writeSessionLine(source, progress.lifecycle,
                "Flutter session state: " + state + ".");
        fireChange();
    }

    private void onSessionExit(
            RunSession source,
            SessionProgress progress,
            Integer code,
            Throwable error) {
        progress.finish(error == null && code != null && code == 0);
        DebugLauncher launcher = null;
        synchronized (lock) {
            if (session != source) {
                return;
            }
            if (dapSession == source) {
                launcher = dapLauncher;
                dapLauncher = null;
                dapSession = null;
            }
            if (sessionProgress == progress) {
                sessionProgress = null;
            }
        }
        if (launcher != null) {
            launcher.close();
        }
        if (error == null) {
            writeSessionLine(source, progress.lifecycle,
                    "Flutter process finished with exit code " + code + ".");
        } else {
            writeSessionLine(source, progress.lifecycle,
                    "Flutter process ended unexpectedly: " + messageOf(error));
        }
        fireChange();
    }

    private boolean submitExclusive(String operation, ThrowingTask task) {
        return submitExclusive(operation, task, () -> {
        });
    }

    private boolean submitExclusive(
            String operation,
            ThrowingTask task,
            Runnable failureCallback) {
        PendingOperation operationHandle;
        synchronized (lock) {
            if (closed || pendingOperation != null) {
                return false;
            }
            operationHandle = new PendingOperation(operation, lifecycleGeneration);
            pendingOperation = operationHandle;
        }
        fireChange();
        worker.execute(() -> {
            operationHandle.bind();
            try {
                task.run(operationHandle);
            } catch (OperationCancelledException ex) {
                // Closing the target picker is a normal user action, not a failed run.
                failureCallback.run();
            } catch (InterruptedException ex) {
                failureCallback.run();
                if (operationHandle.isCancelled()) {
                    Thread.interrupted();
                } else {
                    Thread.currentThread().interrupt();
                    reportFailure(operationHandle.lifecycle(), operation,
                            "operation was interrupted");
                }
            } catch (Exception ex) {
                failureCallback.run();
                reportFailure(operationHandle.lifecycle(), operation, messageOf(ex));
            } finally {
                operationHandle.unbind();
                if (operationHandle.isCancelled()) {
                    Thread.interrupted();
                }
                synchronized (lock) {
                    if (pendingOperation == operationHandle) {
                        pendingOperation = null;
                    }
                }
                fireChange();
            }
        });
        return true;
    }

    private boolean requestStopFromProgress(RunSession source) {
        RunState state = source.state();
        if (session != source || isTerminal(state) || state == RunState.STOPPING) {
            return false;
        }
        worker.execute(() -> {
            if (session != source || isTerminal(source.state())) {
                return;
            }
            try {
                source.quit();
                writeLine("Stop requested from the NetBeans progress indicator for "
                        + projectInfo.name() + ".");
                fireChange();
            } catch (IOException ex) {
                reportFailure("stop Flutter application", messageOf(ex));
            }
        });
        return true;
    }

    private FlutterSdk requireFlutterSdk(String operation) throws IOException {
        return dependencies.requireFlutterSdk(operation);
    }

    private void reportFailure(String operation, String reason) {
        writeLine("Cannot " + operation + " for " + projectRoot + ": " + reason);
        showMessage(
                "Flutter operation failed",
                "Cannot " + operation + " for Flutter project " + projectRoot + ": " + reason,
                NotifyDescriptor.ERROR_MESSAGE);
    }

    private void reportFailure(long lifecycle, String operation, String reason) {
        writeLine(lifecycle,
                "Cannot " + operation + " for " + projectRoot + ": " + reason);
        showMessage(
                lifecycle,
                "Flutter operation failed",
                "Cannot " + operation + " for Flutter project " + projectRoot + ": " + reason,
                NotifyDescriptor.ERROR_MESSAGE);
    }

    private void remember(FlutterDevice device) {
        synchronized (lock) {
            if (closed) {
                return;
            }
            persistTarget(device);
        }
        fireChange();
    }

    private void remember(PendingOperation operation, FlutterDevice device)
            throws OperationCancelledException {
        synchronized (lock) {
            operation.checkCancelled();
            if (pendingOperation != operation) {
                throw new OperationCancelledException();
            }
            persistTarget(device);
        }
        fireChange();
    }

    private void persistTarget(FlutterDevice device) {
        FlutterTargetKind kind = FlutterTargetKind.from(device);
        Preferences settings = preferences();
        settings.put(PREF_DEVICE_ID, device.id());
        settings.put(PREF_DEVICE_NAME, device.name());
        settings.put(PREF_TARGET_KIND, kind.name());
    }

    private Preferences preferences() {
        Preferences current = preferences;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = preferences;
            if (current == null) {
                current = dependencies.projectPreferences(project);
                ProjectPreferenceNamespaceMigration.migrate(project, current);
                migrateLegacyTargetPreference(current);
                preferences = current;
            }
        }
        return current;
    }

    private static void migrateLegacyTargetPreference(Preferences projectPreferences) {
        if (projectPreferences.getBoolean(PREF_PROJECT_MIGRATED, false)) {
            return;
        }
        if (projectPreferences.get(PREF_DEVICE_ID, "").isBlank()) {
            Preferences legacy = ModulePreferences.forModule(FlutterRunController.class);
            copyPreference(legacy, projectPreferences, PREF_DEVICE_ID);
            copyPreference(legacy, projectPreferences, PREF_DEVICE_NAME);
            copyPreference(legacy, projectPreferences, PREF_TARGET_KIND);
        }
        projectPreferences.putBoolean(PREF_PROJECT_MIGRATED, true);
    }

    private static void copyPreference(
            Preferences source,
            Preferences destination,
            String key) {
        String value = source.get(key, "");
        if (!value.isBlank()) {
            destination.put(key, value);
        }
    }

    private boolean openOutput(String name, long lifecycle) {
        if (!isLifecycleActive(lifecycle)) {
            return false;
        }
        synchronized (outputLock) {
            if (!isLifecycleActive(lifecycle)) {
                return false;
            }
            output = dependencies.createOutput(name);
            try {
                output.reset();
            } catch (IOException ex) {
                output.println("Could not clear previous Flutter output: " + messageOf(ex));
            }
            output.select();
            return true;
        }
    }

    private boolean openDevToolsOutput(long lifecycle) {
        if (!isLifecycleActive(lifecycle)) {
            return false;
        }
        synchronized (devToolsOutputLock) {
            if (!isLifecycleActive(lifecycle)) {
                return false;
            }
            devToolsOutput = dependencies.createOutput(
                    "Flutter DevTools: " + projectInfo.name());
            try {
                devToolsOutput.reset();
            } catch (IOException ex) {
                devToolsOutput.println(
                        "Could not clear previous DevTools output: " + messageOf(ex));
            }
            devToolsOutput.select();
            return true;
        }
    }

    private void writeLine(String line) {
        writeLine(lifecycleGeneration, line);
    }

    private void writeLine(long lifecycle, String line) {
        if (!isLifecycleActive(lifecycle) || line == null || line.isBlank()) {
            return;
        }
        synchronized (outputLock) {
            if (!isLifecycleActive(lifecycle)) {
                return;
            }
            if (output == null) {
                output = dependencies.createOutput("Flutter: " + projectInfo.name());
            }
            output.println(line);
        }
    }

    private void writeSessionLine(RunSession source, long lifecycle, String line) {
        if (!isLifecycleActive(lifecycle) || session != source
                || line == null || line.isBlank()) {
            return;
        }
        synchronized (outputLock) {
            if (!isLifecycleActive(lifecycle) || session != source) {
                return;
            }
            if (output == null) {
                output = dependencies.createOutput("Flutter: " + projectInfo.name());
            }
            output.println(line);
        }
    }

    private void writeDebuggerLine(
            RunSession source,
            long lifecycle,
            DebugLauncher launcher,
            String line) {
        if (!isLifecycleActive(lifecycle)
                || session != source
                || dapSession != source
                || launcher == null
                || dapLauncher != launcher
                || line == null
                || line.isBlank()) {
            return;
        }
        synchronized (outputLock) {
            if (!isLifecycleActive(lifecycle)
                    || session != source
                    || dapSession != source
                    || dapLauncher != launcher) {
                return;
            }
            if (output == null) {
                output = dependencies.createOutput("Flutter: " + projectInfo.name());
            }
            output.println(line);
        }
    }

    private void writeDevToolsLine(long lifecycle, String line) {
        if (!isLifecycleActive(lifecycle) || line == null || line.isBlank()) {
            return;
        }
        synchronized (devToolsOutputLock) {
            if (!isLifecycleActive(lifecycle)) {
                return;
            }
            if (devToolsOutput == null) {
                devToolsOutput = dependencies.createOutput(
                        "Flutter DevTools: " + projectInfo.name());
            }
            devToolsOutput.println(line);
        }
    }

    private void writeDevToolsLine(
            RunSession owner,
            DevToolsServer server,
            DevToolsProgress progress,
            String line) {
        if (!isLifecycleActive(progress.lifecycle())
                || devToolsOwner != owner
                || devToolsSession != server
                || devToolsProgress != progress
                || line == null
                || line.isBlank()) {
            return;
        }
        synchronized (devToolsOutputLock) {
            if (!isLifecycleActive(progress.lifecycle())
                    || devToolsOwner != owner
                    || devToolsSession != server
                    || devToolsProgress != progress) {
                return;
            }
            if (devToolsOutput == null) {
                devToolsOutput = dependencies.createOutput(
                        "Flutter DevTools: " + projectInfo.name());
            }
            devToolsOutput.println(line);
        }
    }

    private boolean isLifecycleActive(long lifecycle) {
        return !closed && lifecycle == lifecycleGeneration;
    }

    private void fireChange() {
        if (EventQueue.isDispatchThread()) {
            changes.fireChange();
        } else {
            EventQueue.invokeLater(changes::fireChange);
        }
    }

    private void showMessage(String title, String message, int type) {
        EventQueue.invokeLater(() -> {
            NotifyDescriptor descriptor = new NotifyDescriptor.Message(message, type);
            descriptor.setTitle(title);
            DialogDisplayer.getDefault().notify(descriptor);
        });
    }

    private void showMessage(long lifecycle, String title, String message, int type) {
        EventQueue.invokeLater(() -> {
            if (closed || lifecycle != lifecycleGeneration) {
                return;
            }
            NotifyDescriptor descriptor = new NotifyDescriptor.Message(message, type);
            descriptor.setTitle(title);
            DialogDisplayer.getDefault().notify(descriptor);
        });
    }

    private static <T> T callOnEdt(Callable<T> task) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return task.call();
        }
        FutureTask<T> future = new FutureTask<>(task);
        EventQueue.invokeAndWait(future);
        try {
            return future.get();
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("NetBeans UI dispatch failed", cause);
        }
    }

    private static boolean isTerminal(RunState state) {
        return state == RunState.STOPPED || state == RunState.FAILED;
    }

    private static boolean isRestartable(RunState state) {
        return state == RunState.STARTING || state == RunState.RUNNING;
    }

    private RunState currentState() {
        RunSession current = session;
        return current == null ? RunState.STOPPED : current.state();
    }

    private String unavailableReason(String command) {
        PendingOperation currentOperation = pendingOperation;
        if (currentOperation != null) {
            return "the operation '" + currentOperation.name() + "' is already in progress";
        }
        if (closed || !project.getProjectDirectory().isValid()) {
            return "the Flutter project is closed or its directory is no longer available";
        }
        if (FlutterProjectActionProvider.COMMAND_LAUNCH_EMULATOR.equals(command)
                && !hasConfiguredMobilePlatform()) {
            return "the project has no configured Android or iOS platform; use Flutter > "
                    + "Add Flutter Platforms... first";
        }
        if (FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS.equals(command)) {
            RunSession current = session;
            if (current == null || current.state() != RunState.RUNNING) {
                return "the Flutter application is not running";
            }
            if (current.currentVmServiceUri().isEmpty()) {
                return "Flutter has not supplied a VM Service URI for the running application";
            }
            return "DevTools is currently starting or stopping";
        }
        if (FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS.equals(command)) {
            return "DevTools is not running for this Flutter project";
        }
        return "the current Flutter session state is " + currentState();
    }

    private String currentSessionTargetLabel() {
        SessionProgress progress = sessionProgress;
        return progress == null
                ? "the current target"
                : deviceLabel(progress.target);
    }

    static List<FlutterEmulator> compatibleMobileEmulators(
            List<FlutterEmulator> emulators,
            Set<FlutterProjectPlatform> configured) {
        return emulators.stream()
                .filter(java.util.Objects::nonNull)
                .filter(emulator -> emulatorProjectPlatform(emulator)
                        .filter(configured::contains)
                        .isPresent())
                .toList();
    }

    private static Optional<FlutterProjectPlatform> emulatorProjectPlatform(
            FlutterEmulator emulator) {
        return switch (emulator.platform().toLowerCase(java.util.Locale.ROOT)) {
            case "android" -> Optional.of(FlutterProjectPlatform.ANDROID);
            case "ios" -> Optional.of(FlutterProjectPlatform.IOS);
            default -> Optional.empty();
        };
    }

    private String noCompatibleMobileEmulatorMessage(
            Set<FlutterProjectPlatform> configured) {
        boolean android = configured.contains(FlutterProjectPlatform.ANDROID);
        boolean ios = configured.contains(FlutterProjectPlatform.IOS);
        if (android && ios) {
            return "Flutter reported no configured Android emulator or iOS simulator compatible "
                    + "with the Android and iOS platforms of project " + projectRoot + ". Create "
                    + "one in Android Studio Device Manager or Xcode Simulator first.";
        }
        if (android) {
            return "Flutter reported no configured Android emulator compatible with the Android "
                    + "platform of project " + projectRoot + ". Create an Android Virtual Device "
                    + "in Android Studio Device Manager first.";
        }
        if (ios) {
            return "Flutter reported no configured iOS simulator compatible with the iOS platform "
                    + "of project " + projectRoot + ". Create one in Xcode Simulator first.";
        }
        return "Flutter project " + projectRoot
                + " no longer has a configured Android or iOS platform. Use Flutter > "
                + "Add Flutter Platforms... before launching a mobile emulator.";
    }

    static String deviceLabel(FlutterDevice device) {
        FlutterTargetKind kind = FlutterTargetKind.from(device);
        String emulator = device.emulator() ? ", emulator" : "";
        return "[" + kind.label() + "] " + device.name() + " — " + device.platform()
                + " (" + device.id() + emulator + ")";
    }

    private static String kindLabel(String storedKind) {
        try {
            return FlutterTargetKind.valueOf(storedKind).label();
        } catch (IllegalArgumentException ex) {
            return FlutterTargetKind.UNKNOWN.label();
        }
    }

    private static String actionName(String command) {
        return switch (command) {
            case ActionProvider.COMMAND_RUN -> "Run";
            case ActionProvider.COMMAND_DEBUG -> "Debug";
            case FlutterProjectActionProvider.COMMAND_SELECT_TARGET -> "Select Run Target";
            case FlutterProjectActionProvider.COMMAND_LAUNCH_EMULATOR -> "Launch Mobile Emulator";
            case FlutterProjectActionProvider.COMMAND_HOT_RELOAD -> "Hot Reload";
            case FlutterProjectActionProvider.COMMAND_HOT_RESTART -> "Hot Restart";
            case FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS -> "Open DevTools";
            case FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS -> "Stop DevTools";
            case FlutterProjectActionProvider.COMMAND_STOP -> "Stop";
            default -> command;
        };
    }

    private static String messageOf(Throwable error) {
        Throwable cause = (error instanceof CompletionException || error instanceof ExecutionException)
                && error.getCause() != null ? error.getCause() : error;
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName()
                : message;
    }

    @Override
    public void close() {
        DebugLauncher launcher;
        RunSession current;
        SessionProgress progress;
        DevToolsServer devTools;
        DevToolsProgress devProgress;
        PendingOperation operation;
        OutputTab previousOutput;
        OutputTab previousDevToolsOutput;
        List<ActionProgressCompletion> actionCompletions;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            lifecycleGeneration++;
            operation = pendingOperation;
            pendingOperation = null;
            launcher = dapLauncher;
            dapLauncher = null;
            dapSession = null;
            current = session;
            session = null;
            progress = sessionProgress;
            sessionProgress = null;
            devTools = devToolsSession;
            devToolsSession = null;
            devToolsOwner = null;
            devProgress = devToolsProgress;
            devToolsProgress = null;
            actionCompletions = List.copyOf(pendingActionProgress);
            pendingActionProgress.clear();
        }
        synchronized (targetLock) {
            publishedTargetQuery = targetQuerySequence.incrementAndGet();
        }
        synchronized (outputLock) {
            previousOutput = output;
            output = null;
        }
        synchronized (devToolsOutputLock) {
            previousDevToolsOutput = devToolsOutput;
            devToolsOutput = null;
        }
        if (operation != null) {
            operation.cancel();
        }
        for (ActionProgressCompletion completion : actionCompletions) {
            completion.finish(false);
        }
        if (current != null) {
            quiescenceTasks.track(current.exitCode());
        }
        if (devTools != null) {
            quiescenceTasks.track(devTools.exitCode());
        }
        worker.execute(() -> {
            if (launcher != null) {
                launcher.close();
            }
            if (current != null) {
                current.close();
            }
            if (devTools != null) {
                devTools.close();
            }
            if (progress != null) {
                progress.finish(false);
            }
            if (devProgress != null) {
                devProgress.finish();
            }
            if (previousOutput != null) {
                previousOutput.close();
            }
            if (previousDevToolsOutput != null) {
                previousDevToolsOutput.close();
            }
            fireChange();
        });
    }

    /** Waits for project-owned Run, Debug, and DevTools cleanup work to finish. */
    boolean awaitQuiescence(Duration timeout) throws InterruptedException {
        return quiescenceTasks.awaitIdle(timeout);
    }

    @FunctionalInterface
    private interface DeviceQuery {
        List<FlutterDevice> list(Path projectRoot) throws IOException, InterruptedException;
    }

    interface RunSession extends AutoCloseable {
        RunState state();

        void addOutputListener(Consumer<String> listener);

        void addStateListener(Consumer<RunState> listener);

        CompletableFuture<URI> vmServiceUri();

        Optional<URI> currentVmServiceUri();

        CompletableFuture<Integer> exitCode();

        void hotReload() throws IOException;

        void hotRestart() throws IOException;

        void quit() throws IOException;

        @Override
        void close();
    }

    interface DebugLauncher extends AutoCloseable {
        void attach(
                FlutterSdk sdk,
                Path projectRoot,
                String projectName,
                String deviceId,
                URI vmServiceUri) throws IOException;

        @Override
        void close();
    }

    interface DevToolsServer extends AutoCloseable {
        void addOutputListener(Consumer<String> listener);

        CompletableFuture<URI> browserUri();

        CompletableFuture<Integer> exitCode();

        boolean isAlive();

        boolean stopRequested();

        void stop();

        @Override
        void close();
    }

    interface RunProgress {
        void setInitialDelay(int milliseconds);

        void start();

        void switchToIndeterminate();

        void progress(String message);

        void setDisplayName(String name);

        void finish();
    }

    interface OutputTab {
        void reset() throws IOException;

        void select();

        void println(String line);

        void close();
    }

    static class Dependencies {
        Executor executor() {
            return command -> WORKER.post(command);
        }

        FlutterSdk requireFlutterSdk(String operation) throws IOException {
            var status = new FlutterToolchainService().resolve();
            if (status.flutterSdk().isEmpty()) {
                throw new IOException("cannot " + operation + ": " + status.flutterMessage()
                        + " Configure it in Tools > Options > Flutter.");
            }
            return status.flutterSdk().get();
        }

        void saveModifiedProjectDataObjects(FlutterProject project) throws IOException {
            FlutterProjectSavePreflight.save(project);
        }

        DartSdk requireDartSdk(String operation) throws IOException {
            var status = new FlutterToolchainService().resolve();
            if (status.dartSdk().isEmpty()) {
                throw new IOException("cannot " + operation + ": " + status.dartMessage()
                        + " Configure it in Tools > Options > Flutter.");
            }
            return status.dartSdk().get();
        }

        List<FlutterDevice> listDevices(FlutterSdk sdk, Path projectRoot)
                throws IOException, InterruptedException {
            return new FlutterDeviceService(new FlutterCli(sdk)).list(projectRoot);
        }

        List<FlutterEmulator> listEmulators(FlutterSdk sdk, Path projectRoot)
                throws IOException, InterruptedException {
            return new FlutterEmulatorService(new FlutterCli(sdk)).list(projectRoot);
        }

        RunSession startSession(
                FlutterSdk sdk,
                Path projectRoot,
                String deviceId,
                boolean debug) throws IOException {
            FlutterRunManager manager = new FlutterRunManager(new FlutterCli(sdk));
            FlutterRunSession session = debug
                    ? manager.debug(projectRoot, deviceId)
                    : manager.run(projectRoot, deviceId);
            return new DefaultRunSession(session);
        }

        DebugLauncher createDebugLauncher(
                Consumer<String> diagnosticOutput,
                Consumer<Integer> unexpectedExit) {
            return new DefaultDebugLauncher(
                    new FlutterDapLauncher(diagnosticOutput, unexpectedExit));
        }

        DevToolsServer startDevTools(
                DartSdk dartSdk,
                Path projectRoot,
                URI vmServiceUri) throws IOException {
            return new DefaultDevToolsServer(
                    new DevToolsLauncher(dartSdk).start(projectRoot, vmServiceUri));
        }

        void showBrowser(URI browserUri) throws Exception {
            HtmlBrowser.URLDisplayer.getDefault().showURL(browserUri.toURL());
        }

        boolean confirmRestart(NotifyDescriptor.Confirmation descriptor) {
            return DialogDisplayer.getDefault().notify(descriptor) == NotifyDescriptor.YES_OPTION;
        }

        Preferences projectPreferences(FlutterProject project) {
            return ProjectUtils.getPreferences(
                    project,
                    FlutterRunController.class,
                    false);
        }

        Optional<FlutterDevice> chooseRunTarget(
                String projectName,
                String title,
                List<FlutterDevice> devices,
                String selectedId) throws Exception {
            return choose(
                    "Choose where to run " + projectName + ":",
                    title,
                    devices,
                    FlutterRunController::deviceLabel,
                    device -> device.id().equals(selectedId));
        }

        Optional<FlutterEmulator> chooseMobileEmulator(
                String projectName,
                List<FlutterEmulator> emulators) throws Exception {
            return choose(
                    "Choose an emulator to start for " + projectName + ":",
                    "Launch Mobile Emulator",
                    emulators,
                    emulator -> emulator.name() + " — " + emulator.platform()
                    + " (" + emulator.id() + ")",
                    null);
        }

        void invokeBrowserLater(Runnable action) {
            EventQueue.invokeLater(action);
        }

        RunProgress createProgress(String displayName, BooleanSupplier cancel) {
            return new DefaultRunProgress(ProgressHandle.createHandle(
                    displayName,
                    cancel::getAsBoolean));
        }

        OutputTab createOutput(String displayName) {
            return new DefaultOutputTab(IOProvider.getDefault().getIO(displayName, false));
        }
    }

    private record DefaultRunSession(FlutterRunSession delegate) implements RunSession {
        @Override
        public RunState state() {
            return delegate.state();
        }

        @Override
        public void addOutputListener(Consumer<String> listener) {
            delegate.addOutputListener(listener);
        }

        @Override
        public void addStateListener(Consumer<RunState> listener) {
            delegate.addStateListener(listener);
        }

        @Override
        public CompletableFuture<URI> vmServiceUri() {
            return delegate.vmServiceUri();
        }

        @Override
        public Optional<URI> currentVmServiceUri() {
            return delegate.currentVmServiceUri();
        }

        @Override
        public CompletableFuture<Integer> exitCode() {
            return delegate.exitCode();
        }

        @Override
        public void hotReload() throws IOException {
            delegate.hotReload();
        }

        @Override
        public void hotRestart() throws IOException {
            delegate.hotRestart();
        }

        @Override
        public void quit() throws IOException {
            delegate.quit();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    private record DefaultDebugLauncher(FlutterDapLauncher delegate)
            implements DebugLauncher {
        @Override
        public void attach(
                FlutterSdk sdk,
                Path projectRoot,
                String projectName,
                String deviceId,
                URI vmServiceUri) throws IOException {
            delegate.attach(sdk, projectRoot, projectName, deviceId, vmServiceUri);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    private record DefaultDevToolsServer(DevToolsSession delegate)
            implements DevToolsServer {
        @Override
        public void addOutputListener(Consumer<String> listener) {
            delegate.addOutputListener(listener);
        }

        @Override
        public CompletableFuture<URI> browserUri() {
            return delegate.browserUri();
        }

        @Override
        public CompletableFuture<Integer> exitCode() {
            return delegate.exitCode();
        }

        @Override
        public boolean isAlive() {
            return delegate.isAlive();
        }

        @Override
        public boolean stopRequested() {
            return delegate.stopRequested();
        }

        @Override
        public void stop() {
            delegate.stop();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    private record DefaultOutputTab(InputOutput delegate) implements OutputTab {
        @Override
        public void reset() throws IOException {
            delegate.getOut().reset();
        }

        @Override
        public void select() {
            delegate.select();
        }

        @Override
        public void println(String line) {
            delegate.getOut().println(line);
            delegate.getOut().flush();
        }

        @Override
        public void close() {
            delegate.getOut().close();
        }
    }

    private record DefaultRunProgress(ProgressHandle delegate) implements RunProgress {
        @Override
        public void setInitialDelay(int milliseconds) {
            delegate.setInitialDelay(milliseconds);
        }

        @Override
        public void start() {
            delegate.start();
        }

        @Override
        public void switchToIndeterminate() {
            delegate.switchToIndeterminate();
        }

        @Override
        public void progress(String message) {
            delegate.progress(message);
        }

        @Override
        public void setDisplayName(String name) {
            delegate.setDisplayName(name);
        }

        @Override
        public void finish() {
            delegate.finish();
        }
    }

    @FunctionalInterface
    private interface ThrowingTask {
        void run(PendingOperation operation) throws Exception;
    }

    @FunctionalInterface
    private interface SessionCommand {
        void execute(RunSession session) throws IOException;
    }

    private final class MobileEmulatorProgress {
        private final PendingOperation operation;
        private final ProgressHandle handle;
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile FlutterEmulator emulator;
        private boolean handleStarted;

        MobileEmulatorProgress(PendingOperation operation) {
            this.operation = operation;
            this.handle = ProgressHandle.createHandle(
                    progressName("Preparing"),
                    this::cancel);
            this.handle.setInitialDelay(0);
        }

        synchronized void start() {
            if (finished.get()) {
                return;
            }
            handle.start();
            handleStarted = true;
            handle.switchToIndeterminate();
            handle.progress("Preparing emulator launch");
        }

        synchronized void select(FlutterEmulator selected) {
            emulator = selected;
        }

        synchronized void update(String phase, String detail) {
            if (finished.get()) {
                return;
            }
            handle.setDisplayName(progressName(phase));
            handle.progress(detail);
        }

        synchronized void finish() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            if (handleStarted) {
                handle.finish();
            }
        }

        private boolean cancel() {
            boolean accepted = operation.cancel();
            if (accepted) {
                String target = emulator == null ? "the emulator operation" : emulator.name();
                update("Cancelling", "Stopping the wait for " + target);
            }
            return accepted;
        }

        private String progressName(String phase) {
            String target = emulator == null ? "" : " / " + emulator.name();
            return "Flutter Emulator — " + phase + ": " + projectInfo.name() + target;
        }
    }

    private final class DevToolsProgress {
        private final long lifecycle;
        private final URI vmServiceUri;
        private final RunProgress handle;
        private final AtomicBoolean finished = new AtomicBoolean();
        private final AtomicBoolean stopRequested = new AtomicBoolean();
        private final AtomicBoolean failureReported = new AtomicBoolean();
        private final AtomicBoolean ready = new AtomicBoolean();
        private boolean handleStarted;

        DevToolsProgress(RunSession owner) {
            this.lifecycle = lifecycleGeneration;
            this.vmServiceUri = owner.currentVmServiceUri().orElseThrow();
            this.handle = dependencies.createProgress(
                    progressName("Starting"),
                    () -> requestDevToolsStopFromProgress(this));
            this.handle.setInitialDelay(0);
        }

        URI vmServiceUri() {
            return vmServiceUri;
        }

        long lifecycle() {
            return lifecycle;
        }

        synchronized void start() {
            if (finished.get()) {
                return;
            }
            handle.start();
            handleStarted = true;
            handle.switchToIndeterminate();
            handle.progress("Starting the Dart DevTools server");
        }

        synchronized boolean running(URI browserUri) {
            if (finished.get() || stopRequested.get()) {
                return false;
            }
            ready.set(true);
            handle.setDisplayName(progressName("Running"));
            handle.progress("Serving " + browserUri);
            return true;
        }

        synchronized void stopping() {
            if (finished.get()) {
                return;
            }
            handle.setDisplayName(progressName("Stopping"));
            handle.progress("Stopping the Dart DevTools server");
        }

        boolean requestStop() {
            return stopRequested.compareAndSet(false, true);
        }

        boolean stopRequested() {
            return stopRequested.get();
        }

        boolean markFailed() {
            return failureReported.compareAndSet(false, true);
        }

        boolean failureReported() {
            return failureReported.get();
        }

        boolean ready() {
            return ready.get();
        }

        synchronized void finish() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            if (handleStarted) {
                handle.finish();
            }
        }

        private String progressName(String phase) {
            return "Flutter DevTools — " + phase + ": " + projectInfo.name();
        }
    }

    private final class SessionProgress {
        private final RunSession session;
        private final FlutterDevice target;
        private final boolean debug;
        private final long lifecycle;
        private final ActionProgressCompletion actionCompletion;
        private final RunProgress handle;
        private final AtomicBoolean finished = new AtomicBoolean();
        private final AtomicBoolean cancelRequested = new AtomicBoolean();
        private final ActionMilestone actionMilestone = new ActionMilestone();
        private boolean handleStarted;

        SessionProgress(
                RunSession session,
                FlutterDevice target,
                boolean debug,
                ActionProgressCompletion actionCompletion,
                long lifecycle) {
            this.session = session;
            this.target = target;
            this.debug = debug;
            this.lifecycle = lifecycle;
            this.actionCompletion = actionCompletion;
            this.handle = dependencies.createProgress(
                    progressName("Starting"),
                    this::cancel);
            this.handle.setInitialDelay(0);
        }

        synchronized void start() {
            if (finished.get()) {
                return;
            }
            handle.start();
            handleStarted = true;
            handle.switchToIndeterminate();
            update(session.state());
        }

        synchronized void update(RunState state) {
            if (finished.get()) {
                return;
            }
            switch (state) {
                case STARTING -> setProgress("Starting", "Building and launching on " + target.name());
                case RUNNING -> {
                    setProgress("Running", "Running on " + target.name());
                    if (!debug && actionMilestone.succeed()) {
                        actionCompletion.finish(true);
                    }
                }
                case STOPPING -> setProgress("Stopping", "Stopping on " + target.name());
                case STOPPED, FAILED -> {
                    // The process completion callback owns finish() and ActionProgress status.
                }
            }
        }

        synchronized void markFailed() {
            actionMilestone.fail();
        }

        synchronized boolean markDebuggerReady() {
            if (!debug || !actionMilestone.succeed()) {
                return false;
            }
            actionCompletion.finish(true);
            return true;
        }

        synchronized boolean failDebuggerBeforeReady() {
            return debug && actionMilestone.fail();
        }

        private boolean cancel() {
            if (!cancelRequested.compareAndSet(false, true)) {
                return false;
            }
            synchronized (this) {
                actionMilestone.fail();
            }
            boolean accepted = requestStopFromProgress(session);
            if (accepted) {
                setProgress("Stopping", "Cancel requested; stopping on " + target.name());
            }
            return accepted;
        }

        synchronized void finish(boolean processSucceeded) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            actionMilestone.fail();
            if (handleStarted) {
                handle.finish();
            }
            // Run/Debug is successful only after its integration milestone has
            // already completed ActionProgress (RUNNING for Run, DAP attach for
            // Debug). A clean process exit before that milestone is still a
            // failed IDE action, for example when the user stops during startup.
            actionCompletion.finish(false);
        }

        private void setProgress(String phase, String detail) {
            handle.setDisplayName(progressName(phase));
            handle.progress(detail);
        }

        private String progressName(String phase) {
            String action = debug ? "Debug" : "Run";
            return "Flutter " + action + " — " + phase + ": "
                    + projectInfo.name() + " on " + target.name();
        }
    }

    static final class ActionMilestone {
        private State state = State.PENDING;

        synchronized boolean succeed() {
            if (state != State.PENDING) {
                return false;
            }
            state = State.SUCCEEDED;
            return true;
        }

        synchronized boolean fail() {
            if (state != State.PENDING) {
                return false;
            }
            state = State.FAILED;
            return true;
        }

        synchronized boolean isPending() {
            return state == State.PENDING;
        }

        private enum State {
            PENDING,
            SUCCEEDED,
            FAILED
        }
    }

    private final class ActionProgressCompletion {
        private final ActionProgress progress;
        private final AtomicBoolean finished = new AtomicBoolean();

        ActionProgressCompletion(ActionProgress progress) {
            this.progress = progress;
            boolean alreadyClosed;
            synchronized (lock) {
                alreadyClosed = closed;
                if (!alreadyClosed) {
                    pendingActionProgress.add(this);
                }
            }
            if (alreadyClosed) {
                finished.set(true);
                if (progress != null) {
                    progress.finished(false);
                }
            }
        }

        void finish(boolean success) {
            synchronized (lock) {
                if (success && closed) {
                    success = false;
                }
                if (!finished.compareAndSet(false, true)) {
                    return;
                }
                pendingActionProgress.remove(this);
            }
            if (progress != null) {
                progress.finished(success);
            }
        }
    }

    private static void finishActionProgress(ActionProgress progress, boolean success) {
        if (progress != null) {
            progress.finished(success);
        }
    }

    private final class PendingOperation {
        private final String name;
        private final long lifecycle;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<Thread> worker = new AtomicReference<>();

        PendingOperation(String name, long lifecycle) {
            this.name = name;
            this.lifecycle = lifecycle;
        }

        void bind() {
            Thread current = Thread.currentThread();
            worker.set(current);
            if (isCancelled()) {
                current.interrupt();
            }
        }

        void unbind() {
            worker.compareAndSet(Thread.currentThread(), null);
        }

        boolean cancel() {
            boolean accepted = cancelled.compareAndSet(false, true);
            Thread current = worker.get();
            if (current != null) {
                current.interrupt();
            }
            return accepted;
        }

        boolean isCancelled() {
            return cancelled.get() || closed || lifecycle != lifecycleGeneration;
        }

        void checkCancelled() throws OperationCancelledException {
            if (isCancelled()) {
                throw new OperationCancelledException();
            }
        }

        String name() {
            return name;
        }

        long lifecycle() {
            return lifecycle;
        }
    }

    private static final class OperationCancelledException extends Exception {
    }
}
