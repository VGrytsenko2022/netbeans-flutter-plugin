package dev.flutter.netbeans.plugin.theme;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.project.theme.FlutterGeneratedThemeArtifact;
import dev.flutter.netbeans.project.theme.FlutterProjectTheme;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeCodec;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeDartGenerator;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeDigests;
import dev.flutter.netbeans.project.theme.FlutterProjectThemePaths;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeProvisioner;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterThemesTopComponentTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsOnBackgroundAndSavesDisabledThemeThroughDockedEditor()
            throws Exception {
        Path root = themeApplication("disable");
        RecordingDialogs dialogs = new RecordingDialogs();
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        FlutterThemesTopComponent[] component = new FlutterThemesTopComponent[1];

        onEdt(() -> {
            component[0] = component(dialogs, background);
            assertNull(component[0].editorForTest());
            assertFalse(component[0].saveButtonForTest().isEnabled());
            assertFalse(component[0].reloadButtonForTest().isEnabled());

            component[0].openProject(root);

            assertEquals(root.toAbsolutePath().normalize(),
                    component[0].projectRootForTest());
            assertNull(component[0].editorForTest(),
                    "filesystem loading must not run inline on the Swing EDT");
            assertEquals(1, background.pending());
        });

        background.runNextOffEdt();
        flushEdt();

        onEdt(() -> {
            FlutterThemeEditorPanel editor = component[0].editorForTest();
            assertNotNull(editor);
            assertFalse(component[0].dirtyForTest());
            assertFalse(component[0].saveButtonForTest().isEnabled());
            assertTrue(component[0].reloadButtonForTest().isEnabled());
            assertTrue(component[0].statusLabelForTest().getText()
                    .contains("ready"));
            JScrollPane editorScroll = findDescendant(
                    component[0], JScrollPane.class);
            assertNotNull(editorScroll);
            assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
                    editorScroll.getHorizontalScrollBarPolicy());

            editor.enabledCheckBoxForTest().doClick();

            assertTrue(component[0].dirtyForTest());
            assertTrue(component[0].saveButtonForTest().isEnabled());
            component[0].saveButtonForTest().doClick();
            assertFalse(component[0].saveButtonForTest().isEnabled());
            assertFalse(component[0].reloadButtonForTest().isEnabled());
            assertEquals(1, background.pending());
        });

        background.runNextOffEdt();
        flushEdt();

        assertFalse(new FlutterProjectThemeStore().load(root)
                .theme().orElseThrow().enabled());
        assertTrue(Files.readString(root.resolve(
                FlutterProjectThemePaths.GENERATED_DART_PATH))
                .contains("static ThemeData? get light => null;"));
        onEdt(() -> {
            assertFalse(component[0].dirtyForTest());
            assertFalse(component[0].saveButtonForTest().isEnabled());
            assertTrue(component[0].reloadButtonForTest().isEnabled());
            assertTrue(component[0].statusLabelForTest().getText()
                    .contains("saved"));
        });
        assertTrue(dialogs.errors.isEmpty());
    }

    @Test
    void discardOnCloseClearsTheDraftAndForcesARealReload()
            throws Exception {
        Path root = themeApplication("discard");
        RecordingDialogs dialogs = new RecordingDialogs();
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        FlutterThemesTopComponent[] component = new FlutterThemesTopComponent[1];

        onEdt(() -> {
            component[0] = component(dialogs, background);
            component[0].openProject(root);
        });
        background.runNextOffEdt();
        flushEdt();

        onEdt(() -> {
            component[0].editorForTest().enabledCheckBoxForTest().doClick();
            assertTrue(component[0].dirtyForTest());
            dialogs.unsavedChoice = FlutterThemesTopComponent.UnsavedChoice.DISCARD;

            assertTrue(component[0].canClose());

            assertFalse(component[0].dirtyForTest());
            assertNull(component[0].projectRootForTest());
            assertNull(component[0].editorForTest());
            assertFalse(component[0].saveButtonForTest().isEnabled());
            assertFalse(component[0].reloadButtonForTest().isEnabled());

            component[0].openProject(root);
            assertEquals(1, background.pending(),
                    "discarded in-memory state must not satisfy the same-project fast path");
        });
    }

    @Test
    void cannotCloseWhileSaveIsInFlightAndKeepsLaterEditsDirty()
            throws Exception {
        Path root = themeApplication("save-race");
        RecordingDialogs dialogs = new RecordingDialogs();
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        FlutterThemesTopComponent[] component = new FlutterThemesTopComponent[1];

        onEdt(() -> {
            component[0] = component(dialogs, background);
            component[0].openProject(root);
        });
        background.runNextOffEdt();
        flushEdt();

        onEdt(() -> {
            FlutterThemeEditorPanel editor = component[0].editorForTest();
            editor.enabledCheckBoxForTest().doClick();
            component[0].saveButtonForTest().doClick();
            assertEquals(1, background.pending());

            // Editing remains possible while the exact save revision is queued.
            // Returning to the old baseline must not allow the component to close:
            // the worker is still about to publish the captured disabled revision.
            editor.enabledCheckBoxForTest().doClick();
            assertFalse(component[0].dirtyForTest());
            assertFalse(component[0].canClose(),
                    "closing during publication could silently discard the newer draft");
        });

        background.runNextOffEdt();
        flushEdt();

        assertFalse(new FlutterProjectThemeStore().load(root)
                .theme().orElseThrow().enabled());
        onEdt(() -> {
            assertTrue(component[0].editorForTest()
                    .enabledCheckBoxForTest().isSelected());
            assertTrue(component[0].dirtyForTest(),
                    "the edit made after Save must remain pending against the saved baseline");
            assertTrue(component[0].saveButtonForTest().isEnabled());
            component[0].saveButtonForTest().doClick();
            assertEquals(1, background.pending());
        });

        background.runNextOffEdt();
        flushEdt();

        assertTrue(new FlutterProjectThemeStore().load(root)
                .theme().orElseThrow().enabled());
        onEdt(() -> {
            assertFalse(component[0].dirtyForTest());
            assertTrue(component[0].canClose(),
                    "a completed clean save must release the close guard");
        });
    }

    @Test
    void validationFailureCreatedDuringSaveSurvivesSaveCompletion()
            throws Exception {
        Path root = themeApplication("invalid-during-save");
        RecordingDialogs dialogs = new RecordingDialogs();
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        FlutterThemesTopComponent[] component = new FlutterThemesTopComponent[1];

        onEdt(() -> {
            component[0] = component(dialogs, background);
            component[0].openProject(root);
        });
        background.runNextOffEdt();
        flushEdt();

        onEdt(() -> {
            FlutterThemeEditorPanel editor = component[0].editorForTest();
            editor.enabledCheckBoxForTest().doClick();
            component[0].saveButtonForTest().doClick();
            assertEquals(1, background.pending());

            editor.addButtonForTest().doClick();
            editor.idFieldForTest().setText("Not valid");
            assertFalse(editor.isEditorValid());
            assertTrue(editor.validationMessage().contains("lower_snake_case"));
        });

        background.runNextOffEdt();
        flushEdt();

        onEdt(() -> {
            assertFalse(component[0].editorForTest().isEditorValid());
            assertTrue(component[0].statusLabelForTest().getText()
                    .contains("lower_snake_case"));
            assertFalse(component[0].saveButtonForTest().isEnabled());
            assertTrue(component[0].dirtyForTest());
        });
        assertTrue(dialogs.errors.isEmpty());
    }

    @Test
    void runPreflightSynchronouslySavesOnlyTheExactDirtyThemeProject()
            throws Exception {
        Path root = themeApplication("run-preflight");
        Path otherRoot = themeApplication("run-preflight-other");
        RecordingDialogs dialogs = new RecordingDialogs();
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        FlutterThemesTopComponent[] component = new FlutterThemesTopComponent[1];

        onEdt(() -> {
            component[0] = component(dialogs, background);
            component[0].openProject(root);
        });
        background.runNextOffEdt();
        flushEdt();
        onEdt(() -> {
            component[0].editorForTest().enabledCheckBoxForTest().doClick();
            assertTrue(component[0].dirtyForTest());
        });

        component[0].saveBeforeLaunch(otherRoot);

        assertTrue(new FlutterProjectThemeStore().load(root)
                .theme().orElseThrow().enabled());
        onEdt(() -> assertTrue(component[0].dirtyForTest(),
                "another project's Run must not consume this draft"));

        component[0].saveBeforeLaunch(root);

        assertEquals(0, background.pending(),
                "Run preflight must finish its physical save before returning");
        assertFalse(new FlutterProjectThemeStore().load(root)
                .theme().orElseThrow().enabled());
        assertTrue(Files.readString(root.resolve(
                FlutterProjectThemePaths.GENERATED_DART_PATH))
                .contains("static ThemeData? get light => null;"));
        onEdt(() -> {
            assertFalse(component[0].dirtyForTest());
            assertTrue(component[0].statusLabelForTest().getText().contains("saved"));
        });
        assertTrue(dialogs.errors.isEmpty());
    }

    @Test
    void runPreflightFailsClosedWhileTheExactThemeEditorIsBusy()
            throws Exception {
        Path root = themeApplication("run-preflight-busy");
        RecordingDialogs dialogs = new RecordingDialogs();
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        FlutterThemesTopComponent[] component = new FlutterThemesTopComponent[1];
        onEdt(() -> {
            component[0] = component(dialogs, background);
            component[0].openProject(root);
        });

        IOException failure = assertThrows(IOException.class,
                () -> component[0].saveBeforeLaunch(root));

        assertTrue(failure.getMessage().contains("currently loading or saving"));
        assertEquals(1, background.pending(),
                "preflight must not queue a competing theme transaction");
        assertTrue(dialogs.errors.isEmpty());
    }

    @Test
    void runPreflightRejectsAnInvalidThemeDraftWithoutWriting()
            throws Exception {
        Path root = themeApplication("run-preflight-invalid");
        byte[] descriptorBefore = Files.readAllBytes(
                root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH));
        byte[] dartBefore = Files.readAllBytes(
                root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH));
        RecordingDialogs dialogs = new RecordingDialogs();
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        FlutterThemesTopComponent[] component = new FlutterThemesTopComponent[1];
        onEdt(() -> {
            component[0] = component(dialogs, background);
            component[0].openProject(root);
        });
        background.runNextOffEdt();
        flushEdt();
        onEdt(() -> {
            FlutterThemeEditorPanel editor = component[0].editorForTest();
            editor.addButtonForTest().doClick();
            editor.idFieldForTest().setText("Not valid");
            assertFalse(editor.isEditorValid());
        });

        IOException failure = assertThrows(IOException.class,
                () -> component[0].saveBeforeLaunch(root));

        assertTrue(failure.getMessage().contains("lower_snake_case"));
        assertEquals(0, background.pending());
        assertArrayEquals(descriptorBefore, Files.readAllBytes(
                root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertArrayEquals(dartBefore, Files.readAllBytes(
                root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)));
        onEdt(() -> assertTrue(component[0].dirtyForTest()));
        assertTrue(dialogs.errors.isEmpty());
    }

    @Test
    void runPreflightRejectsAThemeEditThatOvertakesItsCapturedSave()
            throws Exception {
        Path root = themeApplication("run-preflight-race");
        RecordingDialogs dialogs = new RecordingDialogs();
        QueuedBackgroundExecutor background = new QueuedBackgroundExecutor();
        CountDownLatch writerEntered = new CountDownLatch(1);
        CountDownLatch releaseWriter = new CountDownLatch(1);
        FlutterThemePersistence persistence = new FlutterThemePersistence(
                new FlutterProjectThemeStore(),
                new FlutterProjectThemeCodec(),
                (descriptor, dart, oldDescriptor, oldDart, newDescriptor, newDart) -> {
                    writerEntered.countDown();
                    try {
                        if (!releaseWriter.await(5, TimeUnit.SECONDS)) {
                            throw new IOException("test theme writer release timed out");
                        }
                    } catch (InterruptedException failure) {
                        Thread.currentThread().interrupt();
                        throw new IOException("test theme writer was interrupted", failure);
                    }
                    Files.write(descriptor, newDescriptor);
                    Files.write(dart, newDart);
                });
        FlutterThemesTopComponent[] component = new FlutterThemesTopComponent[1];
        onEdt(() -> {
            component[0] = component(dialogs, background, persistence);
            component[0].openProject(root);
        });
        background.runNextOffEdt();
        flushEdt();
        onEdt(() -> component[0].editorForTest()
                .enabledCheckBoxForTest().doClick());

        AtomicReference<Throwable> saveFailure = new AtomicReference<>();
        Thread save = new Thread(() -> {
            try {
                component[0].saveBeforeLaunch(root);
            } catch (Throwable failure) {
                saveFailure.set(failure);
            }
        }, "flutter-theme-run-preflight-test");
        save.start();
        assertTrue(writerEntered.await(5, TimeUnit.SECONDS),
                "Run preflight did not enter theme persistence");

        onEdt(() -> component[0].editorForTest()
                .enabledCheckBoxForTest().doClick());
        releaseWriter.countDown();
        save.join(10_000);

        assertFalse(save.isAlive(), "Run preflight did not finish");
        assertTrue(saveFailure.get() instanceof IOException);
        assertTrue(saveFailure.get().getMessage().contains("live Themes editor revision changed"));
        assertFalse(new FlutterProjectThemeStore().load(root)
                .theme().orElseThrow().enabled(),
                "the captured revision is the only revision allowed to reach disk");
        onEdt(() -> {
            assertTrue(component[0].editorForTest()
                    .enabledCheckBoxForTest().isSelected());
            assertTrue(component[0].dirtyForTest(),
                    "the overtaking edit must remain pending after launch is rejected");
        });
        assertTrue(dialogs.errors.isEmpty());
    }

    private FlutterThemesTopComponent component(
            RecordingDialogs dialogs,
            QueuedBackgroundExecutor background) {
        return component(dialogs, background, new FlutterThemePersistence());
    }

    private FlutterThemesTopComponent component(
            RecordingDialogs dialogs,
            QueuedBackgroundExecutor background,
            FlutterThemePersistence persistence) {
        return new FlutterThemesTopComponent(
                new FlutterProjectThemeStore(),
                new FlutterProjectThemeProvisioner(),
                persistence,
                dialogs,
                background);
    }

    private Path themeApplication(String name) throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve(name));
        Files.writeString(root.resolve(".metadata"), """
                version:
                  revision: test
                  channel: stable
                project_type: app
                """);
        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Path dart = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        Files.createDirectories(descriptor.getParent());
        Files.createDirectories(dart.getParent());
        FlutterProjectTheme provisional = FlutterProjectTheme.defaultTheme(
                "0".repeat(64));
        byte[] dartBytes = FlutterProjectThemeDartGenerator.generate(provisional);
        FlutterProjectTheme committed = new FlutterProjectTheme(
                provisional.enabled(),
                provisional.defaultMode(),
                provisional.lightThemeId(),
                provisional.darkThemeId(),
                provisional.themes(),
                new FlutterGeneratedThemeArtifact(
                        FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH,
                        FlutterProjectThemeDigests.sha256(dartBytes)));
        Files.write(dart, dartBytes);
        Files.write(descriptor, new FlutterProjectThemeCodec().encode(committed));
        return root;
    }

    private static void flushEdt() throws Exception {
        onEdt(() -> { });
    }

    private static <T extends Component> T findDescendant(
            Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T match = findDescendant(nested, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void onEdt(ThrowingRunnable task) throws Exception {
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                task.run();
            } catch (Throwable thrown) {
                failure[0] = thrown;
            }
        });
        if (failure[0] instanceof Exception exception) {
            throw exception;
        }
        if (failure[0] instanceof Error error) {
            throw error;
        }
    }

    private static final class QueuedBackgroundExecutor
            implements FlutterThemesTopComponent.BackgroundExecutor {
        private final Queue<Runnable> operations = new ArrayDeque<>();

        @Override
        public void execute(String displayName, Runnable operation) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            operations.add(operation);
        }

        int pending() {
            return operations.size();
        }

        void runNextOffEdt() throws Exception {
            Runnable operation = operations.remove();
            Throwable[] failure = new Throwable[1];
            Thread worker = new Thread(() -> {
                try {
                    operation.run();
                } catch (Throwable thrown) {
                    failure[0] = thrown;
                }
            }, "flutter-theme-test-worker");
            worker.start();
            worker.join(10_000);
            assertFalse(worker.isAlive(), "background theme operation timed out");
            if (failure[0] instanceof Exception exception) {
                throw exception;
            }
            if (failure[0] instanceof Error error) {
                throw error;
            }
        }
    }

    private static final class RecordingDialogs
            implements FlutterThemesTopComponent.Dialogs {
        private final List<String> errors = new ArrayList<>();
        private FlutterThemesTopComponent.UnsavedChoice unsavedChoice =
                FlutterThemesTopComponent.UnsavedChoice.CANCEL;

        @Override
        public boolean confirmCreateDefault(Path root) {
            throw new AssertionError("valid fixture must not require provisioning");
        }

        @Override
        public FlutterThemesTopComponent.UnsavedChoice unsavedChoice(Path root) {
            return unsavedChoice;
        }

        @Override
        public boolean confirmReload(Path root) {
            return false;
        }

        @Override
        public void error(String title, String message) {
            errors.add(title + ": " + message);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
