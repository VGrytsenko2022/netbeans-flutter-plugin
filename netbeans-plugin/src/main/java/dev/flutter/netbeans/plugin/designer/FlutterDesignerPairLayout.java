package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.plugin.project.FlutterProject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/** Maps project {@code lib/<relative>.dart} files to mirrored models under {@code .fd_templates}. */
public final class FlutterDesignerPairLayout {
    public static final String DART_ROOT = "lib";
    public static final String MODEL_ROOT = ".fd_templates";

    private FlutterDesignerPairLayout() {
    }

    /** One complete, exact physical pair owned by a Flutter project. */
    public record Pair(
            FlutterProject project,
            FileObject dartFile,
            FileObject modelFile,
            String relativeDartPath,
            String relativeModelPath) {
        public Pair {
            Objects.requireNonNull(project, "project");
            Objects.requireNonNull(dartFile, "dartFile");
            Objects.requireNonNull(modelFile, "modelFile");
            relativeDartPath = requireRelative(relativeDartPath, ".dart");
            relativeModelPath = requireRelative(relativeModelPath, ".fd");
        }
    }

    /** Resolves an existing member only when both mirrored files exist. */
    public static Optional<Pair> findCompletePair(FileObject member) {
        if (member == null || member.isFolder()) {
            return Optional.empty();
        }
        Project owner = FileOwnerQuery.getOwner(member);
        if (!(owner instanceof FlutterProject flutterProject)) {
            return Optional.empty();
        }
        return findCompletePair(member, flutterProject);
    }

    public static Optional<Pair> findCompletePair(
            FileObject member,
            FlutterProject project) {
        Objects.requireNonNull(project, "project");
        if (member == null || member.isFolder() || !member.isValid()) {
            return Optional.empty();
        }
        FileObject projectDirectory = project.getProjectDirectory();
        FileObject dartRoot = projectDirectory.getFileObject(DART_ROOT);
        if (dartRoot == null || !dartRoot.isFolder()) {
            return Optional.empty();
        }
        FileObject modelRoot = projectDirectory.getFileObject(MODEL_ROOT);

        if (member.hasExt(FlutterDesignerMime.DART_EXTENSION)) {
            String relativeDart = FileUtil.getRelativePath(dartRoot, member);
            if (!validRelative(relativeDart, ".dart") || modelRoot == null) {
                return Optional.empty();
            }
            String relativeModel = replaceExtension(relativeDart, ".dart", ".fd");
            FileObject model = modelRoot.getFileObject(relativeModel);
            return complete(project, member, model, relativeDart, relativeModel);
        }

        if (member.hasExt(FlutterDesignerMime.MODEL_EXTENSION)
                && modelRoot != null && modelRoot.isFolder()) {
            String relativeModel = FileUtil.getRelativePath(modelRoot, member);
            if (!validRelative(relativeModel, ".fd")) {
                return Optional.empty();
            }
            String relativeDart = replaceExtension(relativeModel, ".fd", ".dart");
            FileObject dart = dartRoot.getFileObject(relativeDart);
            return complete(project, dart, member, relativeDart, relativeModel);
        }
        return Optional.empty();
    }

    /** Returns the mirrored model path below {@link #MODEL_ROOT}. */
    public static String modelPathForDartPath(String relativeDartPath) {
        return replaceExtension(
                requireRelative(relativeDartPath, ".dart"),
                ".dart",
                ".fd");
    }

    private static Optional<Pair> complete(
            FlutterProject project,
            FileObject dart,
            FileObject model,
            String relativeDart,
            String relativeModel) {
        if (dart == null || model == null
                || !dart.isData() || !model.isData()
                || !dart.hasExt(FlutterDesignerMime.DART_EXTENSION)
                || !model.hasExt(FlutterDesignerMime.MODEL_EXTENSION)
                || !isPhysicalPairSafe(project, dart, model)) {
            return Optional.empty();
        }
        return Optional.of(new Pair(
                project, dart, model, relativeDart, relativeModel));
    }

    /**
     * Fails closed for virtual filesystems and for any local pair whose real
     * paths leave the project-managed roots through a symlink or junction.
     */
    private static boolean isPhysicalPairSafe(
            FlutterProject project,
            FileObject dart,
            FileObject model) {
        File projectFile;
        File dartFile;
        File modelFile;
        try {
            projectFile = FileUtil.toFile(project.getProjectDirectory());
            dartFile = FileUtil.toFile(dart);
            modelFile = FileUtil.toFile(model);
        } catch (AssertionError invalidMasterFile) {
            // MasterFS rejects non-normalized FileObjects reached through some
            // symlink/junction layouts with an AssertionError. Pair discovery
            // is a trust boundary, so an unprovable physical path fails closed.
            return false;
        }
        if (projectFile == null || dartFile == null || modelFile == null) {
            return false;
        }

        Path projectPath = projectFile.toPath().toAbsolutePath().normalize();
        Path dartRoot = projectPath.resolve(DART_ROOT).normalize();
        Path modelRoot = projectPath.resolve(MODEL_ROOT).normalize();
        Path dartPath = dartFile.toPath().toAbsolutePath().normalize();
        Path modelPath = modelFile.toPath().toAbsolutePath().normalize();
        if (!dartPath.startsWith(dartRoot) || !modelPath.startsWith(modelRoot)) {
            return false;
        }

        try {
            if (!Files.isDirectory(dartRoot, LinkOption.NOFOLLOW_LINKS)
                    || !Files.isDirectory(modelRoot, LinkOption.NOFOLLOW_LINKS)
                    || !Files.isRegularFile(dartPath, LinkOption.NOFOLLOW_LINKS)
                    || !Files.isRegularFile(modelPath, LinkOption.NOFOLLOW_LINKS)
                    || containsSymbolicLink(projectPath, dartPath)
                    || containsSymbolicLink(projectPath, modelPath)
                    || Files.isSameFile(dartPath, modelPath)) {
                return false;
            }

            Path realProject = projectPath.toRealPath();
            Path realDartRoot = dartRoot.toRealPath();
            Path realModelRoot = modelRoot.toRealPath();
            return realDartRoot.startsWith(realProject)
                    && realModelRoot.startsWith(realProject)
                    && dartPath.toRealPath().startsWith(realDartRoot)
                    && modelPath.toRealPath().startsWith(realModelRoot);
        } catch (IOException | SecurityException ex) {
            return false;
        }
    }

    private static boolean containsSymbolicLink(Path projectRoot, Path target) {
        if (!target.startsWith(projectRoot)) {
            return true;
        }
        Path current = projectRoot;
        for (Path segment : projectRoot.relativize(target)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                return true;
            }
        }
        return false;
    }

    private static String replaceExtension(
            String path,
            String oldExtension,
            String newExtension) {
        String normalized = requireRelative(path, oldExtension);
        return normalized.substring(0, normalized.length() - oldExtension.length())
                + newExtension;
    }

    private static String requireRelative(String path, String extension) {
        if (!validRelative(path, extension)) {
            throw new IllegalArgumentException(
                    "Path must be a normalized relative " + extension + " path: " + path);
        }
        return path;
    }

    private static boolean validRelative(String path, String extension) {
        if (path == null || path.isBlank() || !path.endsWith(extension)
                || path.startsWith("/") || path.startsWith("\\")
                || path.indexOf('\\') >= 0) {
            return false;
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }
        return true;
    }
}
