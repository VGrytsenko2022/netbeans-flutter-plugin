package dev.flutter.netbeans.run;

import dev.flutter.netbeans.api.DartSdk;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Starts the DevTools server supplied by the configured Dart SDK. */
public final class DevToolsLauncher {
    private final DartSdk sdk;
    private final DevToolsProcessStarter processStarter;

    public DevToolsLauncher(DartSdk sdk) {
        this(sdk, (workingDirectory, arguments) -> {
            List<String> command = new ArrayList<>();
            command.add(sdk.dartExecutable().toString());
            command.addAll(arguments);
            return new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .start();
        });
    }

    DevToolsLauncher(DartSdk sdk, DevToolsProcessStarter processStarter) {
        this.sdk = Objects.requireNonNull(sdk, "sdk");
        this.processStarter = Objects.requireNonNull(processStarter, "processStarter");
    }

    public DevToolsSession start(Path projectRoot, URI vmServiceUri) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Path workingDirectory = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(workingDirectory)) {
            throw new IOException("Unable to start DevTools for " + workingDirectory
                    + ": project directory does not exist.");
        }
        URI serviceProtocolUri = serviceProtocolUri(vmServiceUri);
        List<String> arguments = List.of(
                "devtools",
                "--machine",
                "--no-launch-browser",
                "--host=127.0.0.1",
                "--port=0",
                serviceProtocolUri.toASCIIString());
        Process process = processStarter.start(workingDirectory, arguments);
        return new DevToolsSession(process, serviceProtocolUri);
    }

    /** Converts Flutter's machine-protocol WebSocket URI to the HTTP service URI DevTools expects. */
    static URI serviceProtocolUri(URI source) {
        Objects.requireNonNull(source, "vmServiceUri");
        String scheme = source.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("VM Service URI has no scheme: " + source);
        }
        if (source.isOpaque() || source.getRawAuthority() == null
                || source.getHost() == null || source.getHost().isBlank()) {
            throw new IllegalArgumentException("VM Service URI has no host: " + source);
        }
        if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
            return source;
        }
        String targetScheme;
        if (scheme.equalsIgnoreCase("ws")) {
            targetScheme = "http";
        } else if (scheme.equalsIgnoreCase("wss")) {
            targetScheme = "https";
        } else {
            throw new IllegalArgumentException("Unsupported VM Service URI scheme '"
                    + scheme + "' in " + source);
        }
        String path = source.getRawPath();
        path = path == null || path.isEmpty() ? "/" : path;
        if (path.endsWith("/ws")) {
            path = path.substring(0, path.length() - 2);
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        StringBuilder converted = new StringBuilder(targetScheme)
                .append("://")
                .append(source.getRawAuthority())
                .append(path);
        if (source.getRawQuery() != null) {
            converted.append('?').append(source.getRawQuery());
        }
        return URI.create(converted.toString());
    }

    @FunctionalInterface
    interface DevToolsProcessStarter {
        Process start(Path workingDirectory, List<String> arguments) throws IOException;
    }
}
