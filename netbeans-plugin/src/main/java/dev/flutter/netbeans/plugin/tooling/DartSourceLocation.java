package dev.flutter.netbeans.plugin.tooling;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.cookies.LineCookie;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;

/** Resolves and opens one-based Dart source locations inside a Flutter project. */
public final class DartSourceLocation {
    private final Path projectRoot;
    private final String sourcePath;
    private final int line;
    private final int column;

    public DartSourceLocation(Path projectRoot, String sourcePath, int line, int column) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath().normalize();
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath").strip();
        if (this.sourcePath.isEmpty() || line < 1 || column < 1) {
            throw new IllegalArgumentException("A source path and one-based location are required");
        }
        this.line = line;
        this.column = column;
    }

    public Optional<FileObject> resolve() {
        try {
            Path path = Path.of(sourcePath.replace('/', File.separatorChar));
            if (!path.isAbsolute()) {
                path = projectRoot.resolve(path);
            }
            File file = path.toAbsolutePath().normalize().toFile();
            FileObject result = FileUtil.toFileObject(FileUtil.normalizeFile(file));
            return Optional.ofNullable(result);
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }

    public void open() {
        resolve().ifPresent(file -> {
            try {
                DataObject dataObject = DataObject.find(file);
                LineCookie cookie = dataObject.getLookup().lookup(LineCookie.class);
                if (cookie == null) {
                    return;
                }
                Line target = cookie.getLineSet().getCurrent(line - 1);
                target.show(
                        Line.ShowOpenType.OPEN,
                        Line.ShowVisibilityType.FOCUS,
                        column - 1);
            } catch (DataObjectNotFoundException | IndexOutOfBoundsException ex) {
                // A stale diagnostic must not break the Output window action.
            }
        });
    }

    public String sourcePath() {
        return sourcePath;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }
}
