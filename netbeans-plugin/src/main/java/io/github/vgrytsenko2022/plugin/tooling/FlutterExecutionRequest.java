package io.github.vgrytsenko2022.plugin.tooling;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.netbeans.api.extexecution.print.LineConvertor;

/** Complete, shell-free description of one short-lived Flutter tooling process. */
record FlutterExecutionRequest(
        String displayName,
        Path executable,
        Path workingDirectory,
        List<String> arguments,
        boolean echoStandardOutput,
        Consumer<String> standardOutput,
        Consumer<String> standardError,
        LineConvertor outputConvertor) {

    FlutterExecutionRequest {
        displayName = requireText(displayName, "displayName");
        executable = Objects.requireNonNull(executable, "executable").toAbsolutePath().normalize();
        workingDirectory = Objects.requireNonNull(
                workingDirectory,
                "workingDirectory").toAbsolutePath().normalize();
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        standardOutput = standardOutput == null ? ignored -> { } : standardOutput;
        standardError = standardError == null ? ignored -> { } : standardError;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
