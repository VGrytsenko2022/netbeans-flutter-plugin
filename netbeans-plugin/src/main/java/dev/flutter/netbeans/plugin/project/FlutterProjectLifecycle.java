package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.plugin.tooling.FlutterToolingController;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ui.ProjectOpenedHook;

/** Owns metadata migration and project-scoped Flutter service lifecycles. */
final class FlutterProjectLifecycle extends ProjectOpenedHook {
    private final Object lifecycleLock = new Object();
    private final FlutterProject project;
    private final FlutterProjectMetadata metadata;
    private final FlutterProjectMoveOperation moveOperation;
    private final FlutterProjectConfigurationProvider configurations;
    private final FlutterRunController runController;
    private final FlutterToolingController toolingController;
    private final DartAnalysisLifecycle analysisLifecycle;
    private boolean projectOpen;
    private boolean servicesStarted;

    FlutterProjectLifecycle(
            FlutterProject project,
            FlutterProjectMetadata metadata,
            FlutterProjectMoveOperation moveOperation,
            FlutterProjectConfigurationProvider configurations,
            FlutterRunController runController,
            FlutterToolingController toolingController,
            DartAnalysisLifecycle analysisLifecycle) {
        this.project = project;
        this.metadata = metadata;
        this.moveOperation = moveOperation;
        this.configurations = configurations;
        this.runController = runController;
        this.toolingController = toolingController;
        this.analysisLifecycle = analysisLifecycle;
    }

    @Override
    protected void projectOpened() {
        synchronized (lifecycleLock) {
            projectOpen = true;
        }
        if (recoverMetadataOnOpen()
                == FlutterProjectMoveOperation.RecoveryResult.BLOCKED) {
            return;
        }
        startServicesIfOpen();
    }

    private void startServicesIfOpen() {
        synchronized (lifecycleLock) {
            if (!projectOpen || servicesStarted) {
                return;
            }
            servicesStarted = true;
        }
        analysisLifecycle.open();
        runController.open();
        toolingController.open();
        configurations.start();
    }

    @Override
    protected void projectClosed() {
        synchronized (lifecycleLock) {
            projectOpen = false;
            servicesStarted = false;
        }
        analysisLifecycle.close();
        configurations.close();
        toolingController.close();
        runController.close();
    }

    FlutterProjectMoveOperation.RecoveryResult recoverMetadataOnOpen() {
        return ProjectManager.mutex(false, project).writeAccess(() -> {
            FlutterProjectMoveOperation.RecoveryResult result =
                    moveOperation.recoverPendingHandoff();
            if (result == FlutterProjectMoveOperation.RecoveryResult.RECOVERED) {
                metadata.migrateLegacyPrivateConfiguration();
            }
            return result;
        });
    }

    void recoveryResolved() {
        startServicesIfOpen();
    }
}
