package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.plugin.tooling.FlutterToolingController;
import java.util.Set;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.SingleMethod;
import org.netbeans.spi.project.ui.support.DefaultProjectOperations;
import org.openide.util.Lookup;

/** Standard lifecycle operations for a Flutter project directory. */
public final class FlutterProjectActionProvider implements ActionProvider {
    public static final String COMMAND_SELECT_TARGET = "flutter.select.target";
    public static final String COMMAND_LAUNCH_EMULATOR = "flutter.launch.emulator";
    public static final String COMMAND_HOT_RELOAD = "flutter.hot.reload";
    public static final String COMMAND_HOT_RESTART = "flutter.hot.restart";
    public static final String COMMAND_STOP = "flutter.stop";
    public static final String COMMAND_PUB_GET = "flutter.pub.get";
    public static final String COMMAND_ANALYZE = "flutter.analyze";
    public static final String COMMAND_TEST_FILE = "flutter.test.file";
    public static final String COMMAND_TEST_AT_CARET = "flutter.test.caret";

    private static final String[] ACTIONS = {
        COMMAND_SELECT_TARGET,
        COMMAND_LAUNCH_EMULATOR,
        COMMAND_RUN,
        COMMAND_DEBUG,
        COMMAND_HOT_RELOAD,
        COMMAND_HOT_RESTART,
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

    FlutterProjectActionProvider(
            FlutterProject project,
            FlutterRunController runController,
            FlutterToolingController toolingController) {
        this.project = project;
        this.runController = runController;
        this.toolingController = toolingController;
    }

    @Override
    public String[] getSupportedActions() {
        return ACTIONS.clone();
    }

    @Override
    public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
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
                 COMMAND_STOP -> runController.invoke(command);
            case COMMAND_PUB_GET,
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
        if (!SUPPORTED.contains(command)) {
            throw new IllegalArgumentException("Unsupported Flutter project action: " + command);
        }
        if (isRunCommand(command)) {
            return runController.isCommandEnabled(command);
        }
        if (isToolingCommand(command)) {
            return toolingController.isCommandEnabled(command);
        }
        return project.getProjectDirectory().isValid();
    }

    private static boolean isRunCommand(String command) {
        return switch (command) {
            case COMMAND_SELECT_TARGET,
                 COMMAND_LAUNCH_EMULATOR,
                 COMMAND_RUN,
                 COMMAND_DEBUG,
                 COMMAND_HOT_RELOAD,
                 COMMAND_HOT_RESTART,
                 COMMAND_STOP -> true;
            default -> false;
        };
    }

    private static boolean isToolingCommand(String command) {
        return switch (command) {
            case COMMAND_PUB_GET,
                 COMMAND_ANALYZE,
                 COMMAND_TEST,
                 COMMAND_TEST_SINGLE,
                 SingleMethod.COMMAND_RUN_SINGLE_METHOD,
                 COMMAND_TEST_FILE,
                 COMMAND_TEST_AT_CARET -> true;
            default -> false;
        };
    }
}
