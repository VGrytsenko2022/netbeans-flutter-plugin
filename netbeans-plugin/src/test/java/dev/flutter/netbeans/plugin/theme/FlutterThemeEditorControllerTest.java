package dev.flutter.netbeans.plugin.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.project.theme.FlutterProjectThemeLoadStatus;
import dev.flutter.netbeans.project.theme.FlutterProjectThemePaths;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeProvisioner;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeStore;
import dev.flutter.netbeans.project.theme.FlutterThemeMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterThemeEditorControllerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void missingDescriptorCanBeSafelyOnboardedBeforeEditorOpens() throws Exception {
        Path root = freshApplication("onboard");
        RecordingDialogs dialogs = new RecordingDialogs(true, false);
        FlutterThemeEditorController controller = controller(dialogs);

        controller.openProject(root);

        assertTrue(dialogs.confirmed);
        assertEquals(1, dialogs.editCalls);
        assertTrue(dialogs.errors.isEmpty());
        assertTrue(new FlutterProjectThemeStore().load(root).valid());
        assertTrue(Files.readString(root.resolve("lib/main.dart"))
                .contains("theme: AppTheme.light"));
    }

    @Test
    void cancelledOnboardingLeavesExistingProjectExactAndUnmodified()
            throws Exception {
        Path root = freshApplication("cancel");
        byte[] originalMain = Files.readAllBytes(root.resolve("lib/main.dart"));
        RecordingDialogs dialogs = new RecordingDialogs(false, false);

        controller(dialogs).openProject(root);

        assertTrue(dialogs.confirmed);
        assertEquals(0, dialogs.editCalls);
        assertTrue(dialogs.errors.isEmpty());
        assertEquals(FlutterProjectThemeLoadStatus.MISSING,
                new FlutterProjectThemeStore().load(root).status());
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                originalMain, Files.readAllBytes(root.resolve("lib/main.dart")));
    }

    @Test
    void editorSaveRegeneratesDescriptorAndDart() throws Exception {
        Path root = freshApplication("save");
        new FlutterProjectThemeProvisioner().createDefaultIfMissing(root);
        RecordingDialogs dialogs = new RecordingDialogs(false, true);
        dialogs.editMutation = panel ->
                panel.draftForTest().setDefaultMode(FlutterThemeMode.DARK);

        controller(dialogs).openProject(root);

        assertTrue(dialogs.errors.isEmpty());
        var loaded = new FlutterProjectThemeStore().load(root);
        assertTrue(loaded.valid());
        assertEquals(FlutterThemeMode.DARK,
                loaded.theme().orElseThrow().defaultMode());
        assertTrue(Files.readString(root.resolve(
                FlutterProjectThemePaths.GENERATED_DART_PATH))
                .contains("ThemeMode.dark"));
    }

    @Test
    void hashConflictNeverOpensOrOverwritesGeneratedDart() throws Exception {
        Path root = freshApplication("conflict");
        new FlutterProjectThemeProvisioner().createDefaultIfMissing(root);
        Path dart = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        byte[] userChange = "// manually changed\n".getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
        Files.write(dart, userChange);
        RecordingDialogs dialogs = new RecordingDialogs(false, true);

        controller(dialogs).openProject(root);

        assertEquals(0, dialogs.editCalls);
        assertEquals(1, dialogs.errors.size());
        assertTrue(dialogs.errors.get(0).contains("do not match"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                userChange, Files.readAllBytes(dart));
    }

    @Test
    void productionBoundaryDefersFileWorkAndMarshalsDialogsToEdt()
            throws Exception {
        Path root = freshApplication("async");
        RecordingDialogs dialogs = new RecordingDialogs(true, false);
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        FlutterThemeEditorController controller = new FlutterThemeEditorController(
                new FlutterProjectThemeStore(),
                new FlutterProjectThemeProvisioner(),
                new FlutterThemePersistence(),
                dialogs,
                background,
                new FlutterThemeEditorController.EventDispatchUiAccess());

        controller.openProject(root);

        assertFalse(dialogs.confirmed,
                "openProject must return before filesystem/provision work starts");
        assertTrue(background.operation != null);
        Thread worker = new Thread(background.operation, "theme-test-worker");
        worker.start();
        worker.join(10_000);
        assertFalse(worker.isAlive());
        assertTrue(dialogs.confirmed);
        assertEquals(1, dialogs.editCalls);
        assertTrue(dialogs.allCallsOnEdt,
                "confirm/editor/error callbacks must be marshalled to Swing EDT");
    }

    @Test
    void duplicateOpenIsFencedUntilFirstOperationFinishes() throws Exception {
        Path root = freshApplication("duplicate");
        RecordingDialogs dialogs = new RecordingDialogs(false, false);
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        FlutterThemeEditorController controller = new FlutterThemeEditorController(
                new FlutterProjectThemeStore(),
                new FlutterProjectThemeProvisioner(),
                new FlutterThemePersistence(),
                dialogs,
                background,
                new FlutterThemeEditorController.EventDispatchUiAccess());

        controller.openProject(root);
        controller.openProject(root);

        assertEquals(1, background.submissions);
        assertEquals(1, dialogs.errors.size());
        assertTrue(dialogs.errors.get(0).contains("already active"));
        Thread worker = new Thread(background.operation, "theme-test-worker");
        worker.start();
        worker.join(10_000);
        assertFalse(worker.isAlive());
    }

    private FlutterThemeEditorController controller(RecordingDialogs dialogs) {
        return new FlutterThemeEditorController(
                new FlutterProjectThemeStore(),
                new FlutterProjectThemeProvisioner(),
                new FlutterThemePersistence(),
                dialogs);
    }

    private Path freshApplication(String name) throws Exception {
        Path root = temporaryDirectory.resolve(name);
        Files.createDirectories(root.resolve("lib"));
        Files.writeString(root.resolve(".metadata"), """
                version:
                  revision: test
                  channel: stable
                project_type: app
                """);
        Files.writeString(root.resolve("lib/main.dart"), """
                import 'package:flutter/material.dart';

                void main() {
                  runApp(const MyApp());
                }

                class MyApp extends StatelessWidget {
                  const MyApp({super.key});

                  @override
                  Widget build(BuildContext context) {
                    return MaterialApp(
                      theme: ThemeData(
                        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
                      ),
                      home: const SizedBox(),
                    );
                  }
                }
                """);
        return root;
    }

    private static final class RecordingDialogs
            implements FlutterThemeEditorController.Dialogs {
        private final boolean confirmResult;
        private final boolean editResult;
        private boolean confirmed;
        private int editCalls;
        private PanelMutation editMutation = ignored -> { };
        private final List<String> errors = new ArrayList<>();
        private boolean allCallsOnEdt = true;

        private RecordingDialogs(boolean confirmResult, boolean editResult) {
            this.confirmResult = confirmResult;
            this.editResult = editResult;
        }

        @Override
        public boolean confirmCreateDefault(Path projectRoot) {
            allCallsOnEdt &= SwingUtilities.isEventDispatchThread();
            confirmed = true;
            return confirmResult;
        }

        @Override
        public boolean edit(Path projectRoot, FlutterThemeEditorPanel panel) {
            allCallsOnEdt &= SwingUtilities.isEventDispatchThread();
            editCalls++;
            editMutation.mutate(panel);
            return editResult;
        }

        @Override
        public void error(String title, String message) {
            allCallsOnEdt &= SwingUtilities.isEventDispatchThread();
            errors.add(title + ": " + message);
        }
    }

    private static final class QueuedBackgroundExecutor
            implements FlutterThemeEditorController.BackgroundExecutor {
        private int submissions;
        private Runnable operation;

        @Override
        public void execute(String displayName, Runnable nextOperation) {
            submissions++;
            if (operation != null) {
                throw new IllegalStateException("test executor accepts one operation");
            }
            operation = nextOperation;
        }
    }

    @FunctionalInterface
    private interface PanelMutation {
        void mutate(FlutterThemeEditorPanel panel);
    }
}
