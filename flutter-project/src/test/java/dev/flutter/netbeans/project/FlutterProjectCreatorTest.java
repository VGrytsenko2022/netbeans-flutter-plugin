package dev.flutter.netbeans.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.ProcessResult;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeLoadStatus;
import dev.flutter.netbeans.project.theme.FlutterProjectThemePaths;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeStore;
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
            Files.writeString(request.targetDirectory().resolve("lib/main.dart"),
                    generatedCounterMain());
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
        assertTrue(Files.isRegularFile(request.targetDirectory().resolve(
                FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertTrue(Files.isRegularFile(request.targetDirectory().resolve(
                FlutterProjectThemePaths.GENERATED_DART_PATH)));
        assertEquals(FlutterProjectThemeLoadStatus.VALID,
                new FlutterProjectThemeStore().load(request.targetDirectory()).status());
        String main = Files.readString(request.targetDirectory().resolve("lib/main.dart"));
        assertTrue(main.contains("theme: AppTheme.light"));
        assertTrue(main.contains("darkTheme: AppTheme.dark"));
        assertTrue(main.contains("themeMode: AppTheme.mode"));
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

    @Test
    void preservesGeneratedProjectWhenThemeProvisioningFailsClosed() throws Exception {
        var request = new FlutterProjectCreationRequest(
                temporaryDirectory,
                "unsupported_template",
                "dev.example",
                "Sample application.",
                java.util.Set.of(FlutterProjectPlatform.WEB));
        byte[] originalMain = "void main() {}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        FlutterProjectCreator.FlutterCommand command = (workingDirectory, timeout, arguments) -> {
            Files.createDirectories(request.targetDirectory().resolve("lib"));
            Files.createDirectories(request.targetDirectory().resolve("web"));
            Files.writeString(request.targetDirectory().resolve("pubspec.yaml"), """
                    name: unsupported_template
                    dependencies:
                      flutter:
                        sdk: flutter
                    flutter:
                    """);
            Files.write(request.targetDirectory().resolve("lib/main.dart"), originalMain);
            return new ProcessResult(0, "Created project", "");
        };

        IOException failure = assertThrows(IOException.class,
                () -> new FlutterProjectCreator(command, new FlutterProjectDetector())
                        .create(request));

        assertTrue(failure.getMessage().contains("default light and dark"));
        assertTrue(failure.getMessage().contains("preserved for inspection"));
        assertTrue(Files.isDirectory(request.targetDirectory()));
        assertTrue(java.util.Arrays.equals(originalMain,
                Files.readAllBytes(request.targetDirectory().resolve("lib/main.dart"))));
        assertTrue(Files.notExists(request.targetDirectory().resolve(
                FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertTrue(Files.notExists(request.targetDirectory().resolve(
                FlutterProjectThemePaths.GENERATED_DART_PATH)));
    }

    private static String generatedCounterMain() {
        return """
                import 'package:flutter/material.dart';

                void main() {
                  runApp(const MyApp());
                }

                class MyApp extends StatelessWidget {
                  const MyApp({super.key});

                  @override
                  Widget build(BuildContext context) {
                    return MaterialApp(
                      title: 'Flutter Demo',
                      theme: ThemeData(
                        colorScheme: .fromSeed(seedColor: Colors.deepPurple),
                      ),
                      home: const MyHomePage(title: 'Flutter Demo Home Page'),
                    );
                  }
                }

                class MyHomePage extends StatefulWidget {
                  const MyHomePage({super.key, required this.title});
                  final String title;
                  @override
                  State<MyHomePage> createState() => _MyHomePageState();
                }

                class _MyHomePageState extends State<MyHomePage> {
                  int _counter = 0;
                  void _incrementCounter() {
                    setState(() {
                      _counter++;
                    });
                  }
                  @override
                  Widget build(BuildContext context) => const Placeholder();
                }
                """;
    }
}
