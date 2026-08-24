package dev.flutter.netbeans.plugin.pubspec;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.text.Document;
import org.netbeans.api.editor.document.EditorDocumentUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/** Restricts the YAML-wide registrations to pubspecs owned by Flutter projects. */
final class PubspecFiles {
    static final String FILE_NAME = "pubspec.yaml";

    private PubspecFiles() {
    }

    static Optional<Context> from(Document document) {
        return document == null
                ? Optional.empty()
                : from(EditorDocumentUtils.getFileObject(document));
    }

    static Optional<Context> from(FileObject file) {
        if (file == null || !file.isData() || !FILE_NAME.equals(file.getNameExt())) {
            return Optional.empty();
        }
        Project owner = FileOwnerQuery.getOwner(file);
        if (owner == null || owner.getLookup().lookup(FlutterProjectInfo.class) == null) {
            return Optional.empty();
        }
        File projectDirectory = FileUtil.toFile(owner.getProjectDirectory());
        File packageDirectory = FileUtil.toFile(file.getParent());
        if (projectDirectory == null || packageDirectory == null) {
            return Optional.empty();
        }
        return Optional.of(new Context(
                projectDirectory.toPath().toAbsolutePath().normalize(),
                packageDirectory.toPath().toAbsolutePath().normalize()));
    }

    record Context(Path projectRoot, Path packageRoot) {
    }
}
