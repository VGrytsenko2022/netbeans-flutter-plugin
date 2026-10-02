package io.github.vgrytsenko2022.dart;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns one Dart Language Server process and exposes its raw LSP streams.
 *
 * <p>The {@linkplain #inputStream() input stream} is the server's stdout and
 * must be read by the LSP client. The {@linkplain #outputStream() output
 * stream} is the server's stdin and must be written by the LSP client. Stderr
 * is kept out of the protocol stream and drained asynchronously into the
 * supplied consumer.</p>
 *
 * <p>An instance can be started again after it has been closed or after its
 * process has exited. Repeated {@link #start()} calls while the process is
 * alive return the same connection. Repeated {@link #close()} calls are safe.</p>
 */
public final class DartAnalysisServer implements AutoCloseable {
    public static final String CLIENT_ID = "netbeans-flutter";
    public static final String CLIENT_VERSION = "0.1.3";

    private static final Logger LOGGER = Logger.getLogger(DartAnalysisServer.class.getName());
    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(2);

    private final Path dartExecutable;
    private final Path workingDirectory;
    private final Consumer<String> stderrConsumer;
    private final ProcessFactory processFactory;
    private final Duration closeTimeout;

    private Connection connection;
    private InputStream stderrStream;
    private Thread stderrThread;

    /**
     * Creates and starts a Dart Language Server process.
     *
     * @param dartExecutable absolute path to the Dart executable
     * @param workingDirectory project directory supplied to the child process
     * @param stderrConsumer receives individual stderr lines asynchronously
     * @return the started, closeable server owner
     * @throws IOException when the process cannot be started
     */
    public static DartAnalysisServer start(
            Path dartExecutable,
            Path workingDirectory,
            Consumer<String> stderrConsumer) throws IOException {
        DartAnalysisServer server = new DartAnalysisServer(
                dartExecutable,
                workingDirectory,
                stderrConsumer);
        server.start();
        return server;
    }

    public DartAnalysisServer(
            Path dartExecutable,
            Path workingDirectory,
            Consumer<String> stderrConsumer) {
        this(
                dartExecutable,
                workingDirectory,
                stderrConsumer,
                DartAnalysisServer::startProcess,
                DEFAULT_CLOSE_TIMEOUT);
    }

    DartAnalysisServer(
            Path dartExecutable,
            Path workingDirectory,
            Consumer<String> stderrConsumer,
            ProcessFactory processFactory,
            Duration closeTimeout) {
        this.dartExecutable = Objects.requireNonNull(dartExecutable, "dartExecutable")
                .toAbsolutePath()
                .normalize();
        this.workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory")
                .toAbsolutePath()
                .normalize();
        this.stderrConsumer = Objects.requireNonNull(stderrConsumer, "stderrConsumer");
        this.processFactory = Objects.requireNonNull(processFactory, "processFactory");
        this.closeTimeout = Objects.requireNonNull(closeTimeout, "closeTimeout");
        if (closeTimeout.isZero() || closeTimeout.isNegative()) {
            throw new IllegalArgumentException("closeTimeout must be positive");
        }
    }

    /**
     * Starts the server if it is not already running.
     *
     * @return the current raw LSP connection
     * @throws IOException when the process cannot be started
     */
    public synchronized Connection start() throws IOException {
        if (connection != null && connection.process().isAlive()) {
            return connection;
        }

        discardExitedProcess();
        Process created = processFactory.start(command(), workingDirectory);
        InputStream createdStderr = created.getErrorStream();
        Connection createdConnection = new Connection(
                created,
                created.getInputStream(),
                created.getOutputStream());

        connection = createdConnection;
        stderrStream = createdStderr;
        stderrThread = Thread.ofVirtual()
                .name("dart-analysis-stderr")
                .start(() -> drainStderr(createdStderr));
        return createdConnection;
    }

    /** Returns the running child process. */
    public synchronized Process process() {
        return requireConnection().process();
    }

    /** Returns the server stdout stream that carries LSP messages. */
    public synchronized InputStream inputStream() {
        return requireConnection().inputStream();
    }

    /** Returns the server stdin stream that accepts LSP messages. */
    public synchronized OutputStream outputStream() {
        return requireConnection().outputStream();
    }

    public synchronized boolean isRunning() {
        return connection != null && connection.process().isAlive();
    }

    @Override
    public void close() {
        Connection closingConnection;
        InputStream closingStderr;
        Thread closingStderrThread;
        synchronized (this) {
            closingConnection = connection;
            closingStderr = stderrStream;
            closingStderrThread = stderrThread;
            connection = null;
            stderrStream = null;
            stderrThread = null;
        }

        if (closingConnection == null) {
            return;
        }

        closeQuietly(closingConnection.outputStream());
        terminate(closingConnection.process());
        closeQuietly(closingConnection.inputStream());
        closeQuietly(closingStderr);
        stopStderrThread(closingStderrThread);
    }

    private List<String> command() {
        return List.of(
                dartExecutable.toString(),
                "language-server",
                "--protocol=lsp",
                "--client-id=" + CLIENT_ID,
                "--client-version=" + CLIENT_VERSION);
    }

    private Connection requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("Dart Language Server is not started");
        }
        return connection;
    }

    private void discardExitedProcess() {
        if (connection == null) {
            return;
        }
        closeQuietly(connection.outputStream());
        closeQuietly(connection.inputStream());
        closeQuietly(stderrStream);
        if (stderrThread != null) {
            stderrThread.interrupt();
        }
        connection = null;
        stderrStream = null;
        stderrThread = null;
    }

    private void drainStderr(InputStream source) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(source, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    stderrConsumer.accept(line);
                } catch (RuntimeException ex) {
                    LOGGER.log(Level.FINE, "Dart analysis stderr consumer failed", ex);
                }
            }
        } catch (IOException ex) {
            synchronized (this) {
                if (source != stderrStream) {
                    return;
                }
            }
            LOGGER.log(Level.FINE, "Could not drain Dart analysis stderr", ex);
        }
    }

    private void terminate(Process target) {
        if (!target.isAlive()) {
            return;
        }
        target.destroy();
        if (awaitExit(target)) {
            return;
        }
        Process forced = target.destroyForcibly();
        awaitExit(forced);
    }

    private boolean awaitExit(Process target) {
        try {
            return target.waitFor(closeTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            target.destroyForcibly();
            return false;
        }
    }

    private void stopStderrThread(Thread target) {
        if (target == null || target == Thread.currentThread()) {
            return;
        }
        target.interrupt();
        try {
            target.join(Math.min(closeTimeout.toMillis(), 250L));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static Process startProcess(List<String> command, Path workingDirectory)
            throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(false);
        return builder.start();
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ex) {
            LOGGER.log(Level.FINEST, "Could not close Dart analysis process stream", ex);
        }
    }

    /** Raw process connection used by an external LSP client. */
    public record Connection(
            Process process,
            InputStream inputStream,
            OutputStream outputStream) {
        public Connection {
            Objects.requireNonNull(process, "process");
            Objects.requireNonNull(inputStream, "inputStream");
            Objects.requireNonNull(outputStream, "outputStream");
        }
    }

    @FunctionalInterface
    interface ProcessFactory {
        Process start(List<String> command, Path workingDirectory) throws IOException;
    }
}
