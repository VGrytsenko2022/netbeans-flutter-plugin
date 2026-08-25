package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.api.FlutterDevice;
import dev.flutter.netbeans.plugin.tooling.FlutterToolingController;
import java.util.Set;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.SingleMethod;
import org.netbeans.spi.project.ui.support.DefaultProjectOperations;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;

/** Standard lifecycle operations for a Flutter project directory. */
public final class FlutterProjectActionProvider implements ActionProvider {
    public static final String COMMAND_SELECT_TARGET = "flutter.select.target";
    public static final String COMMAND_LAUNCH_EMULATOR = "flutter.launch.emulator";
    public static final String COMMAND_HOT_RELOAD = "flutter.hot.reload";
    public static final String COMMAND_HOT_RESTART = "flutter.hot.restart";
    public static final String COMMAND_OPEN_DEVTOOLS = "flutter.devtools.open";
    public static final String COMMAND_STOP_DEVTOOLS = "flutter.devtools.stop";
    public static final String COMMAND_STOP = "flutter.stop";
    public static final String COMMAND_PUB_GET = "flutter.pub.get";
    public static final String COMMAND_ANALYZE = "flutter.analyze";
    public static final String COMMAND_TEST_FILE = "flutter.test.file";
    public static final String COMMAND_TEST_AT_CARET = "flutter.test.caret";

    private static final String[] ACTIONS = {
        COMMAND_SELECT_TARGET,
        COMMAND_LAUNCH_EMULATOR,
        COMMAND_BUILD,
        COMMAND_REBUILD,
        COMMAND_CLEAN,
        COMMAND_RUN,
        COMMAND_DEBUG,
        COMMAND_HOT_RELOAD,
        COMMAND_HOT_RESTART,
        COMMAND_OPEN_DEVTOOLS,
        COMMAND_STOP_DEVTOOLS,
        COMMAND_STOP,
        COMMAND_PUB_GET,
        COMMAND_ANALYZE,
        COMMAND_TEST,
        COMMAND_TEST_SINGLE,
        SingleMethod.COMMAND_RUN_SINGLE_METHOD,
        COMMAND_TEST_FILE,
        COMMAND_TEST_AT_CARET,
        COMMAND_DELETE,
        COMMAND_COPY,
        COMMAND_MOVE,
        COMMAND_RENAME
    };
    private static final Set<String> SUPPORTED = Set.of(ACTIONS);

    private final FlutterProject project;
    private final FlutterRunController runController;
    private final FlutterToolingController toolingController;
    private final FlutterProjectConfigurationProvider configurations;
    private final FlutterProjectMoveOperation moveOperation;

    FlutterProjectActionProvider(
            FlutterProject project,
            FlutterRunController runController,
            FlutterToolingController toolingController,
            FlutterProjectConfigurationProvider configurations,
            FlutterProjectMoveOperation moveOperation) {
        this.project = project;
        this.runController = runController;
        this.toolingController = toolingController;
        this.configurations = configurations;
        this.moveOperation = moveOperation;
    }

    @Override
    public String[] getSupportedActions() {
        return ACTIONS.clone();
    }

    @Override
    public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
        requireSupported(command);
        if (moveOperation.isRecoveryBlocked()
                && !isRecoveryResolutionCommand(command)) {
            showRecoveryBlocked(command);
            return;
        }
        Lookup actionContext = context == null ? Lookup.EMPTY : context;
        switch (command) {
            case COMMAND_RUN,
                 COMMAND_DEBUG -> {
                FlutterTargetConfiguration configuration =
                        actionContext.lookup(FlutterTargetConfiguration.class);
                runController.invoke(
                        command,
                        ActionProgress.start(actionContext),
                        configuration == null ? null : configuration.device());
            }
            case COMMAND_SELECT_TARGET,
                 COMMAND_LAUNCH_EMULATOR,
                 COMMAND_HOT_RELOAD,
                 COMMAND_HOT_RESTART,
                 COMMAND_OPEN_DEVTOOLS,
                 COMMAND_STOP_DEVTOOLS,
                 COMMAND_STOP -> runController.invoke(command);
            case COMMAND_BUILD,
                 COMMAND_REBUILD -> toolingController.invoke(
                         command,
                         actionContext,
                         ActionProgress.start(actionContext),
                         buildTarget(actionContext));
            case COMMAND_CLEAN,
                 COMMAND_PUB_GET,
                 COMMAND_ANALYZE,
                 COMMAND_TEST,
                 COMMAND_TEST_SINGLE,
                 SingleMethod.COMMAND_RUN_SINGLE_METHOD,
                 COMMAND_TEST_FILE,
                 COMMAND_TEST_AT_CARET -> toolingController.invoke(
                         command,
                         actionContext,
                         ActionProgress.start(actionContext));
            case COMMAND_DELETE -> DefaultProjectOperations.performDefaultDeleteOperation(project);
            case COMMAND_COPY -> DefaultProjectOperations.performDefaultCopyOperation(project);
            case COMMAND_MOVE -> DefaultProjectOperations.performDefaultMoveOperation(project);
            case COMMAND_RENAME -> DefaultProjectOperations.performDefaultRenameOperation(project, null);
            default -> throw new IllegalArgumentException("Unsupported Flutter project action: " + command);
        }
    }

    @Override
    public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
        requireSupported(command);
        if (moveOperation.isRecoveryBlocked()
                && !isRecoveryResolutionCommand(command)) {
            return false;
        }
        if (isRunCommand(command)) {
            return runController.isCommandEnabled(command);
        }
        if (isToolingCommand(command)) {
            return toolingController.isCommandEnabled(command);
        }
        return project.getProjectDirectory().isValid();
    }

    private static void requireSupported(String command) {
        if (!SUPPORTED.contains(command)) {
            throw new IllegalArgumentException(
                    "Unsupported Flutter project action: " + command);
        }
    }

    private boolean isRecoveryResolutionCommand(String command) {
        return COMMAND_DELETE.equals(command)
                || (COMMAND_RENAME.equals(command)
                        && moveOperation.canResolveRecoveryByRename());
    }

    private void showRecoveryBlocked(String command) {
        String projectPath = project.getProjectDirectory().getPath();
        String resolution = moveOperation.canResolveRecoveryByRename()
                ? "Use Rename to supply the target project name and finish the interrupted "
                        + "move, or delete the project if it is no longer needed."
                : "Close the project and repair or remove the preserved .netbeans Flutter "
                        + "move handoff after verifying its contents, or delete the project "
                        + "if it is no longer needed.";
        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                "Cannot perform " + command + " for " + projectPath + ": "
                        + moveOperation.recoveryBlockReason() + ". " + resolution,
                NotifyDescriptor.WARNING_MESSAGE));
    }

    private static boolean isRunCommand(String command) {
        return switch (command) {
            case COMMAND_SELECT_TARGET,
                 COMMAND_LAUNCH_EMULATOR,
                 COMMAND_RUN,
                 COMMAND_DEBUG,
                 COMMAND_HOT_RELOAD,
                 COMMAND_HOT_RESTART,
                 COMMAND_OPEN_DEVTOOLS,
                 COMMAND_STOP_DEVTOOLS,
                 COMMAND_STOP -> true;
            default -> false;
        };
    }

    private static boolean isToolingCommand(String command) {
        return switch (command) {
            case COMMAND_PUB_GET,
                 COMMAND_ANALYZE,
                 COMMAND_BUILD,
                 COMMAND_CLEAN,
                 COMMAND_REBUILD,
                 COMMAND_TEST,
                 COMMAND_TEST_SINGLE,
                 SingleMethod.COMMAND_RUN_SINGLE_METHOD,
                 COMMAND_TEST_FILE,
                 COMMAND_TEST_AT_CARET -> true;
            default -> false;
        };
    }

    private FlutterDevice buildTarget(Lookup context) {
        return resolveBuildTarget(context, configurations);
    }

    static FlutterDevice resolveBuildTarget(
            Lookup context,
            FlutterProjectConfigurationProvider configurations) {
        Lookup safeContext = context == null ? Lookup.EMPTY : context;
        FlutterTargetConfiguration contextual = safeContext.lookup(
                FlutterTargetConfiguration.class);
        FlutterTargetConfiguration configuration = contextual != null
                ? contextual
                : ProjectManager.mutex().readAccess(configurations::getActiveConfiguration);
        return configuration == null ? null : configuration.device();
    }
}
