package dev.flutter.netbeans.plugin.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.DartSdk;
import dev.flutter.netbeans.plugin.project.DartAnalysisLifecycle;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.api.project.Project;
import org.netbeans.modules.lsp.client.spi.ServerRestarter;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

class DartLanguageServerProviderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void doesNotInvokeStarterWhenDartSdkIsUnavailable() {
        AtomicBoolean invoked = new AtomicBoolean();
        DartLanguageServerProvider provider = new DartLanguageServerProvider(
                () -> status(Optional.empty()),
                (executable, workingDirectory) -> {
                    invoked.set(true);
                    return stubServer();
                });

        assertNull(provider.startServer(Lookup.EMPTY));
        assertFalse(invoked.get());
    }

    @Test
    void startsConfiguredSdkInProjectDirectory() {
        Path dartExecutable = temporaryDirectory.resolve("sdk/bin/dart");
        DartSdk dartSdk = new DartSdk(dartExecutable.getParent().getParent(), dartExecutable);
        AtomicReference<Path> capturedExecutable = new AtomicReference<>();
        AtomicReference<Path> capturedDirectory = new AtomicReference<>();
        DartLanguageServerProvider provider = new DartLanguageServerProvider(
                () -> status(Optional.of(dartSdk)),
                (executable, workingDirectory) -> {
                    capturedExecutable.set(executable);
                    capturedDirectory.set(workingDirectory);
                    return stubServer();
                });

        FileObject projectDirectory = FileUtil.toFileObject(
                FileUtil.normalizeFile(temporaryDirectory.toFile()));
        assertNotNull(projectDirectory);
        Project project = new TestProject(projectDirectory);

        assertNotNull(provider.startServer(Lookups.singleton(project)));
        assertEquals(
                dartExecutable.toAbsolutePath().normalize(),
                capturedExecutable.get());
        assertEquals(
                temporaryDirectory.toAbsolutePath().normalize(),
                capturedDirectory.get());
    }

    @Test
    void fallsBackToConfiguredUserWorkingDirectoryWithoutProject() {
        Path dartExecutable = temporaryDirectory.resolve("sdk/bin/dart");
        DartSdk dartSdk = new DartSdk(dartExecutable.getParent().getParent(), dartExecutable);
        Path fallback = temporaryDirectory.resolve("fallback");
        AtomicReference<Path> capturedDirectory = new AtomicReference<>();
        DartLanguageServerProvider provider = new DartLanguageServerProvider(
                () -> status(Optional.of(dartSdk)),
                (executable, workingDirectory) -> {
                    capturedDirectory.set(workingDirectory);
                    return stubServer();
                },
                () -> fallback);

        assertNotNull(provider.startServer(Lookup.EMPTY));
        assertEquals(fallback.toAbsolutePath().normalize(), capturedDirectory.get());
    }

    @Test
    void advertisesDartLanguageId() {
        assertEquals(
                "dart",
                DartLanguageServerProvider.languageIdResolver().resolveLanguageId(null));
    }

    @Test
    void returnsNullWhenStarterFails() {
        Path dartExecutable = temporaryDirectory.resolve("sdk/bin/dart");
        DartSdk dartSdk = new DartSdk(dartExecutable.getParent().getParent(), dartExecutable);
        DartLanguageServerProvider provider = new DartLanguageServerProvider(
                () -> status(Optional.of(dartSdk)),
                (executable, workingDirectory) -> {
                    throw new IOException("synthetic startup failure");
                },
                () -> temporaryDirectory);

        assertNull(provider.startServer(Lookup.EMPTY));
    }

    @Test
    void doesNotStartServerForClosedFlutterProject() {
        Path dartExecutable = temporaryDirectory.resolve("sdk/bin/dart");
        DartSdk dartSdk = new DartSdk(dartExecutable.getParent().getParent(), dartExecutable);
        DartAnalysisLifecycle lifecycle = new DartAnalysisLifecycle();
        AtomicBoolean invoked = new AtomicBoolean();
        DartLanguageServerProvider provider = new DartLanguageServerProvider(
                () -> status(Optional.of(dartSdk)),
                (executable, workingDirectory) -> {
                    invoked.set(true);
                    return stubServer();
                });
        Project project = new TestProject(
                FileUtil.toFileObject(FileUtil.normalizeFile(temporaryDirectory.toFile())),
                Lookups.singleton(lifecycle));

        assertNull(provider.startServer(Lookups.singleton(project)));
        assertFalse(invoked.get());
    }

    @Test
    void projectLifecycleClosesAcceptedServer() throws InterruptedException {
        Path dartExecutable = temporaryDirectory.resolve("sdk/bin/dart");
        DartSdk dartSdk = new DartSdk(dartExecutable.getParent().getParent(), dartExecutable);
        DartAnalysisLifecycle lifecycle = new DartAnalysisLifecycle();
        lifecycle.open();
        CountDownLatch closed = new CountDownLatch(1);
        CountDownLatch cacheRemoved = new CountDownLatch(1);
        DartLanguageServerProvider provider = new DartLanguageServerProvider(
                () -> status(Optional.of(dartSdk)),
                (executable, workingDirectory) -> new DartLanguageServerProvider.StartedServer(
                        InputStream.nullInputStream(),
                        OutputStream.nullOutputStream(),
                        new StubProcess(),
                        closed::countDown));
        Project project = new TestProject(
                FileUtil.toFileObject(FileUtil.normalizeFile(temporaryDirectory.toFile())),
                Lookups.singleton(lifecycle));

        Lookup context = Lookups.fixed(
                project,
                (ServerRestarter) cacheRemoved::countDown);
        assertNotNull(provider.startServer(context));
        lifecycle.close();

        assertTrue(cacheRemoved.await(2, TimeUnit.SECONDS));
        assertTrue(closed.await(2, TimeUnit.SECONDS));
    }

    private static FlutterToolchainStatus status(Optional<DartSdk> dartSdk) {
        return new FlutterToolchainStatus(
                Optional.empty(),
                dartSdk,
                "Flutter status is irrelevant to the Dart provider test.",
                dartSdk.isPresent() ? "Dart SDK is ready." : "Dart SDK is unavailable.",
                dartSdk.isPresent());
    }

    private static DartLanguageServerProvider.StartedServer stubServer() {
        return new DartLanguageServerProvider.StartedServer(
                InputStream.nullInputStream(),
                OutputStream.nullOutputStream(),
                new StubProcess());
    }

    private record TestProject(FileObject projectDirectory, Lookup lookup) implements Project {
        TestProject(FileObject projectDirectory) {
            this(projectDirectory, Lookup.EMPTY);
        }

        @Override
        public FileObject getProjectDirectory() {
            return projectDirectory;
        }

        @Override
        public Lookup getLookup() {
            return lookup;
        }
    }

    private static final class StubProcess extends Process {
        private boolean destroyed;

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }

        @Override
        public boolean isAlive() {
            return !destroyed;
        }
    }
}
