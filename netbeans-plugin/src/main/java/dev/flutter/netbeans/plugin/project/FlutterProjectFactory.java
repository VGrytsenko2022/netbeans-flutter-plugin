package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.project.FlutterProjectDetector;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/** Recognizes an existing Flutter directory as a native NetBeans project. */
@ServiceProvider(service = org.netbeans.spi.project.ProjectFactory.class, position = 700)
public final class FlutterProjectFactory implements ProjectFactory2 {
    public static final String PROJECT_TYPE = "dev.flutter.netbeans.project";
    public static final String ICON_PATH
            = "dev/flutter/netbeans/plugin/project/flutterProject16.svg";

    private final FlutterProjectDetector detector;

    public FlutterProjectFactory() {
        this(new FlutterProjectDetector());
    }

    FlutterProjectFactory(FlutterProjectDetector detector) {
        this.detector = detector;
    }

    @Override
    public boolean isProject(FileObject projectDirectory) {
        return detect(projectDirectory).isPresent();
    }

    @Override
    public ProjectManager.Result isProject2(FileObject projectDirectory) {
        Optional<FlutterProjectInfo> detected = detect(projectDirectory);
        if (detected.isEmpty()) {
            return null;
        }
        FlutterProjectInfo info = detected.get();
        return new ProjectManager.Result(info.name(), PROJECT_TYPE, projectIcon());
    }

    @Override
    public Project loadProject(FileObject projectDirectory, ProjectState state) throws IOException {
        Optional<FlutterProjectInfo> detected = detect(projectDirectory);
        return detected.isPresent()
                ? new FlutterProject(projectDirectory, state, detected.get())
                : null;
    }

    @Override
    public void saveProject(Project project) throws IOException, ClassCastException {
        if (!(project instanceof FlutterProject)) {
            throw new ClassCastException("Not a Flutter project: " + project);
        }
        // Flutter projects store their model in pubspec.yaml; the plugin owns no project metadata file.
    }

    private Optional<FlutterProjectInfo> detect(FileObject projectDirectory) {
        if (projectDirectory == null || !projectDirectory.isFolder()) {
            return Optional.empty();
        }
        File localDirectory = FileUtil.toFile(projectDirectory);
        if (localDirectory == null) {
            return Optional.empty();
        }
        Path path = localDirectory.toPath();
        return detector.detect(path);
    }

    static Icon projectIcon() {
        return new ImageIcon(projectImage());
    }

    static Image projectImage() {
        Image image = ImageUtilities.loadImage(ICON_PATH, true);
        if (image != null) {
            return image;
        }
        Icon fallback = UIManager.getIcon("FileView.directoryIcon");
        return fallback != null
                ? ImageUtilities.icon2Image(fallback)
                : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }
}
