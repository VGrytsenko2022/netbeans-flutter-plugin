package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.plugin.theme.FlutterThemesTopComponent;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import org.netbeans.api.actions.Savable;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.UserQuestionException;

/**
 * Synchronously establishes the clean on-disk project state required by Run
 * and Debug.
 *
 * <p>The standard lifecycle save API returns no success result. A launch gate
 * must instead invoke every project-owned {@link Savable} (with legacy
 * {@link SaveCookie} fallback) and verify the corresponding {@link DataObject}s
 * reached a clean state. This also reaches the Flutter Designer's one
 * pair-aware save authority, which commits its Dart and {@code .fd} files as
 * one transaction, plus the open project Themes draft.</p>
 */
public final class FlutterProjectSavePreflight {
    private static final int MAX_SAVE_PASSES = 8;

    private FlutterProjectSavePreflight() {
    }

    /** Saves all modified data objects owned by the supplied project. */
    public static void save(Project project) throws IOException {
        Objects.requireNonNull(project, "project");
        FileObject projectDirectory = project.getProjectDirectory();
        if (projectDirectory == null || !projectDirectory.isValid()) {
            throw new IOException("the Flutter project directory is no longer available");
        }
        File localDirectory = FileUtil.toFile(projectDirectory);
        if (localDirectory == null) {
            throw new IOException("the Flutter project directory is not a local folder: "
                    + projectDirectory.getPath());
        }
        save(new ProjectScope(
                projectDirectory,
                localDirectory.toPath().toAbsolutePath().normalize(),
                project));
    }

    /**
     * Saves modified data objects below a local project root.
     *
     * <p>This overload is useful to infrastructure which has an exact project
     * root but no live NetBeans {@link Project} facade.</p>
     */
    public static void save(Path projectRoot) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        File rootFile = normalizedRoot.toFile();
        FileObject projectDirectory = FileUtil.toFileObject(rootFile);
        if (projectDirectory == null) {
            FileUtil.refreshFor(rootFile);
            projectDirectory = FileUtil.toFileObject(rootFile);
        }
        if (projectDirectory == null || !projectDirectory.isFolder()) {
            throw new IOException("the Flutter project directory is not a readable local folder: "
                    + normalizedRoot);
        }
        save(new ProjectScope(projectDirectory, normalizedRoot, null));
    }

    private static void save(ProjectScope project) throws IOException {
        saveModifiedDataObjects(project);
        FlutterThemesTopComponent.saveProjectDraftBeforeLaunch(project.localRoot());
        saveModifiedDataObjects(project);
    }

    private static void saveModifiedDataObjects(ProjectScope project) throws IOException {
        for (int pass = 1; pass <= MAX_SAVE_PASSES; pass++) {
            List<ModifiedObject> modified = modifiedObjects(project);
            if (modified.isEmpty()) {
                return;
            }
            for (ModifiedObject candidate : modified) {
                DataObject dataObject = candidate.dataObject();
                if (!dataObject.isModified()) {
                    continue;
                }
                Savable savable = dataObject.getLookup().lookup(Savable.class);
                if (savable != null) {
                    save(candidate.displayPath(), savable::save);
                    continue;
                }
                SaveCookie save = dataObject.getCookie(SaveCookie.class);
                if (save == null) {
                    throw failure(candidate.displayPath(),
                            "the modified file exposes neither Savable nor SaveCookie");
                }
                save(candidate.displayPath(), save::save);
            }
        }

        List<ModifiedObject> remaining = modifiedObjects(project);
        if (!remaining.isEmpty()) {
            String files = remaining.stream()
                    .map(ModifiedObject::displayPath)
                    .limit(5)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("unknown file");
            if (remaining.size() > 5) {
                files += ", and " + (remaining.size() - 5) + " more";
            }
            throw new IOException("cannot save accumulated Flutter project changes: files remain "
                    + "modified after " + MAX_SAVE_PASSES + " synchronous save passes: " + files);
        }
    }

    private static void save(String displayPath, SaveOperation save) throws IOException {
        UserQuestionException question = null;
        while (true) {
            try {
                if (question == null) {
                    save.run();
                } else {
                    question.confirmed();
                }
                return;
            } catch (UserQuestionException nextQuestion) {
                if (!confirm(displayPath, nextQuestion)) {
                    throw failure(displayPath, "saving was cancelled by the user", nextQuestion);
                }
                question = nextQuestion;
            } catch (IOException exception) {
                throw failure(displayPath, messageOf(exception), exception);
            }
        }
    }

    private static boolean confirm(String displayPath, UserQuestionException question)
            throws IOException {
        if (EventQueue.isDispatchThread()) {
            return confirmOnEdt(question);
        }
        FutureTask<Boolean> prompt = new FutureTask<>(() -> confirmOnEdt(question));
        try {
            EventQueue.invokeAndWait(prompt);
            return prompt.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failure(displayPath,
                    "saving was interrupted while waiting for user confirmation",
                    exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            throw failure(displayPath, messageOf(cause), cause);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            throw failure(displayPath, messageOf(cause), cause);
        }
    }

    private static boolean confirmOnEdt(UserQuestionException question) {
        NotifyDescriptor.Confirmation descriptor = new NotifyDescriptor.Confirmation(
                question.getLocalizedMessage(),
                "Save Before Flutter Run",
                NotifyDescriptor.YES_NO_OPTION,
                NotifyDescriptor.WARNING_MESSAGE);
        return DialogDisplayer.getDefault().notify(descriptor) == NotifyDescriptor.YES_OPTION;
    }

    private static List<ModifiedObject> modifiedObjects(ProjectScope project) {
        List<ModifiedObject> result = new ArrayList<>();
        for (DataObject dataObject : DataObject.getRegistry().getModifiedSet()) {
            FileObject primary = dataObject.getPrimaryFile();
            if (primary == null || !belongsToProject(project, primary)) {
                continue;
            }
            result.add(new ModifiedObject(
                    dataObject,
                    displayPath(project.projectDirectory(), primary)));
        }
        result.sort(Comparator.comparing(
                ModifiedObject::displayPath,
                String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static boolean belongsToProject(ProjectScope project, FileObject primary) {
        if (project.owner() != null) {
            Project owner = FileOwnerQuery.getOwner(primary);
            if (owner != null
                    && !owner.getProjectDirectory().equals(project.projectDirectory())) {
                return false;
            }
        }
        if (primary.equals(project.projectDirectory())
                || FileUtil.isParentOf(project.projectDirectory(), primary)) {
            return true;
        }
        File local = FileUtil.toFile(primary);
        return local != null
                && local.toPath().toAbsolutePath().normalize().startsWith(project.localRoot());
    }

    private static String displayPath(FileObject projectDirectory, FileObject primary) {
        String relative = FileUtil.getRelativePath(projectDirectory, primary);
        return relative == null || relative.isBlank() ? primary.getPath() : relative;
    }

    private static IOException failure(String displayPath, String reason) {
        return new IOException("cannot save accumulated Flutter project changes in "
                + displayPath + ": " + reason);
    }

    private static IOException failure(
            String displayPath,
            String reason,
            Throwable cause) {
        return new IOException("cannot save accumulated Flutter project changes in "
                + displayPath + ": " + reason, cause);
    }

    private static String messageOf(Throwable error) {
        if (error == null) {
            return "no cause was reported";
        }
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message;
    }

    private record ProjectScope(
            FileObject projectDirectory,
            Path localRoot,
            Project owner) {
    }

    private record ModifiedObject(
            DataObject dataObject,
            String displayPath) {
    }

    @FunctionalInterface
    private interface SaveOperation {
        void run() throws IOException;
    }
}
