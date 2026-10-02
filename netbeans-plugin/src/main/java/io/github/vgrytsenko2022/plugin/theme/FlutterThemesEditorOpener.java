package io.github.vgrytsenko2022.plugin.theme;

import io.github.vgrytsenko2022.api.FlutterProjectInfo;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemePaths;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;

/** Resolves the canonical descriptor before activating the docked Themes window. */
@Messages({
    "TTL_CannotOpenFlutterThemes=Cannot open Flutter project themes",
    "# {0} - descriptor path",
    "# {1} - concrete reason",
    "MSG_CannotOpenFlutterThemes=Open Flutter theme editor failed for {0}. Reason: {1}."
})
final class FlutterThemesEditorOpener {
    private FlutterThemesEditorOpener() {
    }

    static void openDescriptor(FileObject descriptorFile) {
        Objects.requireNonNull(descriptorFile, "descriptorFile");
        Project owner = FileOwnerQuery.getOwner(descriptorFile);
        FlutterProjectInfo info = owner == null
                ? null
                : owner.getLookup().lookup(FlutterProjectInfo.class);
        if (info == null) {
            showError(
                    descriptorFile.getPath(),
                    "the descriptor is not owned by a recognized Flutter project");
            return;
        }
        File localFile = FileUtil.toFile(descriptorFile);
        Path expected = info.root().toAbsolutePath().normalize()
                .resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)
                .normalize();
        if (localFile == null
                || !localFile.toPath().toAbsolutePath().normalize().equals(expected)) {
            showError(
                    descriptorFile.getPath(),
                    "this is not the canonical project descriptor " + expected);
            return;
        }
        openProject(info.root());
    }

    static void openProject(Path projectRoot) {
        FlutterThemesTopComponent.showProject(
                Objects.requireNonNull(projectRoot, "projectRoot"));
    }

    private static void showError(String target, String reason) {
        NotifyDescriptor message = new NotifyDescriptor.Message(
                Bundle.MSG_CannotOpenFlutterThemes(target, reason),
                NotifyDescriptor.ERROR_MESSAGE);
        message.setTitle(Bundle.TTL_CannotOpenFlutterThemes());
        DialogDisplayer.getDefault().notify(message);
    }
}
