package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.plugin.tooling.FlutterToolingController;
import java.io.IOException;
import java.time.Duration;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ui.ProjectOpenedHook;

/** Owns metadata migration and project-scoped Flutter service lifecycles. */
final class FlutterProjectLifecycle extends ProjectOpenedHook {
    private static final Duration DELETE_QUIESCENCE_TIMEOUT = Duration.ofSeconds(15);

    private final Object lifecycleLock = new Object();
    private final FlutterProject project;
    private final FlutterProjectMetadata metadata;
    private final FlutterProjectMoveOperation moveOperation;
    private final FlutterProjectPlatformProvider platformProvider;
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
            FlutterProjectPlatformProvider platformProvider,
            FlutterProjectConfigurationProvider configurations,
            FlutterRunController runController,
            FlutterToolingController toolingController,
            DartAnalysisLifecycle analysisLifecycle) {
        this.project = project;
        this.metadata = metadata;
        this.moveOperation = moveOperation;
        this.platformProvider = platformProvider;
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
        platformProvider.start();
        analysisLifecycle.open();
        runController.open();
        toolingController.open();
        configurations.start();
    }

    @Override
    protected void projectClosed() {
        stopServices();
    }

    void prepareForDelete() throws IOException {
        long deadline = System.nanoTime() + DELETE_QUIESCENCE_TIMEOUT.toNanos();
        stopServices();
        awaitDeleteService(
                "Flutter Run, Debug, and DevTools processes",
                runController::awaitQuiescence,
                deadline);
        awaitDeleteService(
                "Flutter tooling commands",
                toolingController::awaitQuiescence,
                deadline);
        awaitDeleteService(
                "Dart analysis server",
                analysisLifecycle::awaitQuiescence,
                deadline);
    }

    private void stopServices() {
        synchronized (lifecycleLock) {
            if (!projectOpen && !servicesStarted) {
                return;
            }
            projectOpen = false;
            servicesStarted = false;
        }
        analysisLifecycle.close();
        configurations.close();
        toolingController.close();
        runController.close();
        platformProvider.close();
    }

    private void awaitDeleteService(
            String service,
            QuiescenceWait wait,
            long deadline) throws IOException {
        long remainingNanos = Math.max(0L, deadline - System.nanoTime());
        try {
            if (!wait.await(Duration.ofNanos(remainingNanos))) {
                throw new IOException(
                        "Cannot delete Flutter project at "
                        + project.getProjectDirectory().getPath()
                        + " because " + service
                        + " did not stop within "
                        + DELETE_QUIESCENCE_TIMEOUT.toSeconds() + " seconds");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Cannot delete Flutter project at "
                    + project.getProjectDirectory().getPath()
                    + " because waiting for " + service + " was interrupted",
                    ex);
        }
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

    @FunctionalInterface
    private interface QuiescenceWait {
        boolean await(Duration timeout) throws InterruptedException;
    }
}
