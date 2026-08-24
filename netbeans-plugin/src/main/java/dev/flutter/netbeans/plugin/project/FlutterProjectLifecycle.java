package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.plugin.tooling.FlutterToolingController;
import org.netbeans.spi.project.ui.ProjectOpenedHook;

/** Stops project-owned Flutter processes when the project closes. */
final class FlutterProjectLifecycle extends ProjectOpenedHook {
    private final FlutterProjectConfigurationProvider configurations;
    private final FlutterRunController runController;
    private final FlutterToolingController toolingController;
    private final DartAnalysisLifecycle analysisLifecycle;

    FlutterProjectLifecycle(
            FlutterProjectConfigurationProvider configurations,
            FlutterRunController runController,
            FlutterToolingController toolingController,
            DartAnalysisLifecycle analysisLifecycle) {
        this.configurations = configurations;
        this.runController = runController;
        this.toolingController = toolingController;
        this.analysisLifecycle = analysisLifecycle;
    }

    @Override
    protected void projectOpened() {
        analysisLifecycle.open();
        runController.open();
        toolingController.open();
        configurations.start();
    }

    @Override
    protected void projectClosed() {
        analysisLifecycle.close();
        configurations.close();
        toolingController.close();
        runController.close();
    }
}
