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
        this.projectDirectory = projectDirectory;
        this.state = state;
        this.projectInfo = projectInfo;

        FlutterProjectInformation information = new FlutterProjectInformation(this, projectInfo);
        FlutterProjectSources sources = new FlutterProjectSources(this);
        FlutterRecommendedTemplates recommendedTemplates = new FlutterRecommendedTemplates();
        FlutterRunController runController = new FlutterRunController(this, projectInfo);
        FlutterProjectConfigurationProvider configurations =
                new FlutterProjectConfigurationProvider(runController);
        FlutterToolingController toolingController = new FlutterToolingController(
                this,
                projectInfo,
                new NetBeansFlutterTestSessionFactory());
        DartAnalysisLifecycle analysisLifecycle = new DartAnalysisLifecycle(
                projectInfo.name(),
                new NetBeansDartAnalysisStatusReporter());
        FlutterLogicalViewProvider logicalView = new FlutterLogicalViewProvider(this, information);
        FlutterProjectActionProvider actions = new FlutterProjectActionProvider(
                this,
                runController,
                toolingController);
        FlutterProjectLifecycle lifecycle = new FlutterProjectLifecycle(
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
                logicalView,
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
