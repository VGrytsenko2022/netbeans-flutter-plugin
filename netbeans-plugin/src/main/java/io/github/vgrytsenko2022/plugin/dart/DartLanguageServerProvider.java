package io.github.vgrytsenko2022.plugin.dart;

import io.github.vgrytsenko2022.api.DartSdk;
import io.github.vgrytsenko2022.dart.DartAnalysisServer;
import io.github.vgrytsenko2022.plugin.project.DartAnalysisLifecycle;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainService;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.Project;
import org.netbeans.modules.lsp.client.spi.LanguageIdResolver;
import org.netbeans.modules.lsp.client.spi.LanguageServerProvider;
import org.netbeans.modules.lsp.client.spi.ServerRestarter;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/** Starts the configured Dart SDK's LSP server for Dart editor documents. */
@MimeRegistration(
        mimeType = DartTokenId.MIME_TYPE,
        service = LanguageServerProvider.class)
public final class DartLanguageServerProvider implements LanguageServerProvider {
    private static final Logger LOGGER =
            Logger.getLogger(DartLanguageServerProvider.class.getName());
    private static final LanguageIdResolver DART_LANGUAGE_ID = file -> "dart";

    private final Supplier<FlutterToolchainStatus> statusSupplier;
    private final ServerStarter starter;
    private final Supplier<Path> fallbackWorkingDirectory;

    public DartLanguageServerProvider() {
        this(
                new FlutterToolchainService()::resolve,
                DartLanguageServerProvider::startDartServer,
                DartLanguageServerProvider::userWorkingDirectory);
    }

    DartLanguageServerProvider(
            Supplier<FlutterToolchainStatus> statusSupplier,
            ServerStarter starter) {
        this(statusSupplier, starter, DartLanguageServerProvider::userWorkingDirectory);
    }

    DartLanguageServerProvider(
            Supplier<FlutterToolchainStatus> statusSupplier,
            ServerStarter starter,
            Supplier<Path> fallbackWorkingDirectory) {
        this.statusSupplier = Objects.requireNonNull(statusSupplier, "statusSupplier");
        this.starter = Objects.requireNonNull(starter, "starter");
        this.fallbackWorkingDirectory = Objects.requireNonNull(
                fallbackWorkingDirectory,
                "fallbackWorkingDirectory");
    }

    @Override
    public LanguageServerDescription startServer(Lookup context) {
        Lookup effectiveContext = context == null ? Lookup.EMPTY : context;
        Project project = effectiveContext.lookup(Project.class);
        DartAnalysisLifecycle lifecycle = project == null
                ? null
                : project.getLookup().lookup(DartAnalysisLifecycle.class);
        DartAnalysisLifecycle.Ticket ticket = lifecycle == null
                ? null
                : lifecycle.acquire();
        if (lifecycle != null && ticket == null) {
            return null;
        }
        ServerRestarter restarter = effectiveContext.lookup(ServerRestarter.class);

        FlutterToolchainStatus status = statusSupplier.get();
        if (status == null || status.dartSdk().isEmpty()) {
            if (lifecycle != null) {
                lifecycle.fail(ticket, status == null
                        ? "The Flutter toolchain could not be resolved."
                        : status.dartMessage());
            }
            return null;
        }

        DartSdk dartSdk = status.dartSdk().orElseThrow();
        Path workingDirectory = workingDirectory(
                effectiveContext,
                fallbackWorkingDirectory);
        if (lifecycle != null && !lifecycle.beginStart(ticket)) {
            return null;
        }
        try {
            StartedServer server = starter.start(
                    dartSdk.dartExecutable().toAbsolutePath().normalize(),
                    workingDirectory);
            if (lifecycle != null && !lifecycle.attach(
                    ticket,
                    server.owner(),
                    restarter == null ? null : restarter::restart)) {
                return null;
            }
            DartLspCompletionCompatibility compatibility =
                    new DartLspCompletionCompatibility();
            return LanguageServerDescription.create(
                    new DartLspCompletionCompatibilityInputStream(
                            server.inputStream(), compatibility),
                    new DartLspCapabilitiesOutputStream(
                            server.outputStream(), compatibility),
                    server.process(),
                    Lookups.fixed(DART_LANGUAGE_ID));
        } catch (IOException ex) {
            if (lifecycle != null) {
                String detail = ex.getMessage() == null || ex.getMessage().isBlank()
                        ? ex.getClass().getSimpleName()
                        : ex.getMessage();
                lifecycle.fail(ticket, "Could not start "
                        + dartSdk.dartExecutable().toAbsolutePath().normalize()
                        + " in " + workingDirectory + ": " + detail);
            }
            LOGGER.log(
                    Level.WARNING,
                    "Could not start Dart Language Server with executable {0} in {1}: {2}",
                    new Object[] {
                        dartSdk.dartExecutable(),
                        workingDirectory,
                        ex.getMessage()
                    });
            LOGGER.log(Level.FINE, "Dart Language Server startup failure", ex);
            return null;
        }
    }

    static LanguageIdResolver languageIdResolver() {
        return DART_LANGUAGE_ID;
    }

    static Path workingDirectory(
            Lookup context,
            Supplier<Path> fallbackWorkingDirectory) {
        Project project = context.lookup(Project.class);
        if (project != null && project.getProjectDirectory() != null) {
            var directory = FileUtil.toFile(project.getProjectDirectory());
            if (directory != null) {
                return directory.toPath().toAbsolutePath().normalize();
            }
        }
        return Objects.requireNonNull(
                        fallbackWorkingDirectory.get(),
                        "fallback working directory")
                .toAbsolutePath()
                .normalize();
    }

    private static StartedServer startDartServer(
            Path dartExecutable,
            Path workingDirectory) throws IOException {
        DartAnalysisServer server = DartAnalysisServer.start(
                dartExecutable,
                workingDirectory,
                line -> LOGGER.log(Level.FINE, "Dart Language Server stderr: {0}", line));
        return new StartedServer(
                server.inputStream(),
                server.outputStream(),
                server.process(),
                server);
    }

    private static Path userWorkingDirectory() {
        String configured = System.getProperty("user.dir", ".");
        try {
            return Path.of(configured == null || configured.isBlank() ? "." : configured);
        } catch (InvalidPathException ex) {
            LOGGER.log(Level.FINE, "Invalid user.dir; using current directory", ex);
            return Path.of(".");
        }
    }

    @FunctionalInterface
    interface ServerStarter {
        StartedServer start(Path dartExecutable, Path workingDirectory) throws IOException;
    }

    record StartedServer(
            InputStream inputStream,
            OutputStream outputStream,
            Process process,
            AutoCloseable owner) {
        StartedServer(InputStream inputStream, OutputStream outputStream, Process process) {
            this(inputStream, outputStream, process, process::destroy);
        }

        StartedServer {
            Objects.requireNonNull(inputStream, "inputStream");
            Objects.requireNonNull(outputStream, "outputStream");
            Objects.requireNonNull(process, "process");
            Objects.requireNonNull(owner, "owner");
        }
    }
}
