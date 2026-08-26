package dev.flutter.netbeans.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.ProcessResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterProjectCreatorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsAndVerifiesStandardFlutterApplication() throws Exception {
        var request = new FlutterProjectCreationRequest(
                temporaryDirectory,
                "sample_app",
                "dev.example",
                "Sample application.",
                EnumSet.of(
                        FlutterProjectPlatform.WEB,
                        FlutterProjectPlatform.ANDROID));
        AtomicReference<List<String>> invokedArguments = new AtomicReference<>();
        FlutterProjectCreator.FlutterCommand command = (workingDirectory, timeout, arguments) -> {
            invokedArguments.set(List.of(arguments));
            Files.createDirectories(request.targetDirectory().resolve("lib"));
            Files.createDirectories(request.targetDirectory().resolve("android"));
            Files.createDirectories(request.targetDirectory().resolve("web"));
            Files.writeString(request.targetDirectory().resolve("pubspec.yaml"), """
                    name: sample_app
                    dependencies:
                      flutter:
                        sdk: flutter
                    flutter:
                    """);
            Files.writeString(request.targetDirectory().resolve("lib/main.dart"), "void main() {}\n");
            return new ProcessResult(0, "Created project", "");
        };

        var created = new FlutterProjectCreator(command, new FlutterProjectDetector()).create(request);

        assertEquals(request.targetDirectory(), created.root());
        assertEquals(List.of(
                "create", "--template", "app", "--project-name", "sample_app",
                "--org", "dev.example", "--description", "Sample application.",
                "--platforms=android,web", request.targetDirectory().toString()),
                invokedArguments.get());
        assertEquals(request.targetDirectory().toString(),
                invokedArguments.get().get(invokedArguments.get().size() - 1));
    }

    @Test
    void rejectsSuccessfulCreateThatOmitsASelectedPlatform() throws Exception {
        var request = new FlutterProjectCreationRequest(
                temporaryDirectory,
                "sample_app",
                "dev.example",
                "Sample application.",
                java.util.Set.of(FlutterProjectPlatform.WEB));
        FlutterProjectCreator.FlutterCommand command = (workingDirectory, timeout, arguments) -> {
            Files.createDirectories(request.targetDirectory().resolve("lib"));
            Files.writeString(request.targetDirectory().resolve("pubspec.yaml"), """
                    name: sample_app
                    dependencies:
                      flutter:
                        sdk: flutter
                    flutter:
                    """);
            return new ProcessResult(0, "Created project with a warning", "");
        };

        IOException failure = assertThrows(IOException.class,
                () -> new FlutterProjectCreator(command, new FlutterProjectDetector())
                        .create(request));

        assertTrue(failure.getMessage().contains("web"));
        assertTrue(failure.getMessage().contains("not generated"));
        assertTrue(Files.isDirectory(request.targetDirectory()));
    }

    @Test
    void rejectsSuccessfulCreateThatLeavesAFileAtASelectedPlatformPath() throws Exception {
        var request = new FlutterProjectCreationRequest(
                temporaryDirectory,
                "sample_app",
                "dev.example",
                "Sample application.",
                java.util.Set.of(FlutterProjectPlatform.WEB));
        FlutterProjectCreator.FlutterCommand command = (workingDirectory, timeout, arguments) -> {
            Files.createDirectories(request.targetDirectory().resolve("lib"));
            Files.writeString(request.targetDirectory().resolve("web"), "occupied");
            Files.writeString(request.targetDirectory().resolve("pubspec.yaml"), """
                    name: sample_app
                    dependencies:
                      flutter:
                        sdk: flutter
                    flutter:
                    """);
            return new ProcessResult(0, "Created project with a warning", "");
        };

        IOException failure = assertThrows(IOException.class,
                () -> new FlutterProjectCreator(command, new FlutterProjectDetector())
                        .create(request));

        assertTrue(failure.getMessage().contains("web"));
        assertTrue(failure.getMessage().contains("not generated"));
    }

    @Test
    void reportsFlutterCliFailureWithTargetAndCause() {
        var request = new FlutterProjectCreationRequest(
                temporaryDirectory,
                "sample_app",
                "dev.example",
                "Sample application.");
        FlutterProjectCreator.FlutterCommand command = (workingDirectory, timeout, arguments) ->
                new ProcessResult(1, "", "Template download failed");

        IOException failure = assertThrows(IOException.class,
                () -> new FlutterProjectCreator(command, new FlutterProjectDetector()).create(request));

        assertTrue(failure.getMessage().contains(request.targetDirectory().toString()));
        assertTrue(failure.getMessage().contains("Template download failed"));
    }

    @Test
    void refusesToOverwriteExistingTarget() throws Exception {
        var request = new FlutterProjectCreationRequest(
                temporaryDirectory,
                "sample_app",
                "dev.example",
                "Sample application.");
        Files.createDirectories(request.targetDirectory());

        IOException failure = assertThrows(IOException.class,
                () -> new FlutterProjectCreator((workingDirectory, timeout, arguments) -> {
                    throw new AssertionError("Flutter command must not run");
                }, new FlutterProjectDetector()).create(request));

        assertTrue(failure.getMessage().contains("target already exists"));
    }
}
