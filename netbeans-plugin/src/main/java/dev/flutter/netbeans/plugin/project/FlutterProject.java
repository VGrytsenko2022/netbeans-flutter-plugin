package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.plugin.tooling.FlutterToolingController;
import dev.flutter.netbeans.plugin.tooling.test.NetBeansFlutterTestSessionFactory;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/** NetBeans project facade for an existing Flutter application or package. */
public final class FlutterProject implements Project {
    private final FileObject projectDirectory;
    private final ProjectState state;
    private final FlutterProjectInfo projectInfo;
    private final Lookup lookup;

    FlutterProject(
            FileObject projectDirectory,
            ProjectState state,
            FlutterProjectInfo projectInfo) {
        this(
                projectDirectory,
                state,
                projectInfo,
                new FlutterProjectMetadata(projectDirectory));
    }

    FlutterProject(
            FileObject projectDirectory,
            ProjectState state,
            FlutterProjectInfo projectInfo,
            FlutterProjectMetadata metadata) {
        this(projectDirectory, state, projectInfo, metadata, null);
    }

    FlutterProject(
            FileObject projectDirectory,
            ProjectState state,
            FlutterProjectInfo projectInfo,
            FlutterProjectMetadata metadata,
            FlutterProjectMoveOperation.PrivateStateWriter moveStateWriter) {
        this(
                projectDirectory,
                state,
                projectInfo,
                metadata,
                moveStateWriter,
                null);
    }

    FlutterProject(
            FileObject projectDirectory,
            ProjectState state,
            FlutterProjectInfo projectInfo,
            FlutterProjectMetadata metadata,
            FlutterProjectMoveOperation.PrivateStateWriter moveStateWriter,
            FlutterProjectMoveOperation.PrivatePreferencesFlusher movePreferencesFlusher) {
        this.projectDirectory = projectDirectory;
        this.state = state;
        this.projectInfo = projectInfo;

        FlutterProjectInformation information = new FlutterProjectInformation(
                this,
                projectInfo,
                metadata);
        FlutterProjectPlatformProvider platformProvider =
                new FlutterProjectPlatformProvider(projectDirectory, projectInfo.root());
        FlutterProjectSources sources = new FlutterProjectSources(this);
        FlutterRecommendedTemplates recommendedTemplates = new FlutterRecommendedTemplates();
        FlutterRunController runController = new FlutterRunController(this, projectInfo);
        FlutterProjectConfigurationProvider configurations =
                new FlutterProjectConfigurationProvider(runController);
        FlutterToolingController toolingController = new FlutterToolingController(
                this,
                projectInfo,
                new NetBeansFlutterTestSessionFactory(),
                platformProvider);
        DartAnalysisLifecycle analysisLifecycle = new DartAnalysisLifecycle(
                projectInfo.name(),
                new NetBeansDartAnalysisStatusReporter());
        FlutterLogicalViewProvider logicalView = new FlutterLogicalViewProvider(this, information);
        FlutterProjectMoveOperation moveOperation = moveStateWriter == null
                && movePreferencesFlusher == null
                ? new FlutterProjectMoveOperation(this, information)
                : new FlutterProjectMoveOperation(
                        this,
                        information,
                        moveStateWriter == null
                                ? (destination, attribute, value) -> destination.setAttribute(
                                        FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX
                                                + attribute,
                                        value)
                                : moveStateWriter,
                        movePreferencesFlusher == null
                                ? ignored -> {
                                }
                                : movePreferencesFlusher);
        FlutterProjectActionProvider actions = new FlutterProjectActionProvider(
                this,
                runController,
                toolingController,
                configurations,
                moveOperation);
        FlutterProjectLifecycle lifecycle = new FlutterProjectLifecycle(
                this,
                metadata,
                moveOperation,
                platformProvider,
                configurations,
                runController,
                toolingController,
                analysisLifecycle);
        this.lookup = Lookups.fixed(
                this,
                projectInfo,
                information,
                sources,
                recommendedTemplates,
                metadata,
                platformProvider,
                logicalView,
                moveOperation,
                actions,
                configurations,
                runController,
                toolingController,
                analysisLifecycle,
                lifecycle);
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDirectory;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    ProjectState state() {
        return state;
    }

    FlutterProjectInfo info() {
        return projectInfo;
    }
}
