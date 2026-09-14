package dev.flutter.netbeans.designer.events;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Optional real-SDK proof of every reviewed callable stub and typedef assignment. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class EventCallbackSdkContractTest {
    @TempDir Path project;

    @Test
    void everyCatalogCallableStubIsAssignableToItsFlutterTypedef() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk")).toAbsolutePath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        Path dart = sdk.resolve(windows
                ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        assertTrue(Files.isRegularFile(flutter), "Missing configured Flutter SDK: " + flutter);
        assertTrue(Files.isRegularFile(dart), "Missing configured Dart SDK: " + dart);
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: designer_event_callback_contract
                publish_to: none
                environment:
                  sdk: '>=3.11.0 <4.0.0'
                dependencies:
                  flutter:
                    sdk: flutter
                """);

        StringBuilder assignments = new StringBuilder();
        byte[] owner = "class EventProbe {\n}\n".getBytes(StandardCharsets.UTF_8);
        int index = 0;
        for (var definition : BuiltInWidgetCatalog.getDefault().definitions()) {
            for (var callable : WidgetEventCatalog.eventsFor(definition)) {
                String name = "handle" + index;
                owner = DartEventHandlerSource.insert(owner, "EventProbe", name,
                        callable.createStub(name));
                // Every assignment is independently statically checked by Dart.
                assignments.append(callable.callbackType()).append(" probe").append(index)
                        .append(" = EventProbe().").append(name).append(";\n");
                assertTrue(definition.property(callable.propertyName()).isPresent());
                callable.bindingValue(name, definition.property(callable.propertyName()).orElseThrow());
                index++;
            }
        }
        assertFalse(index == 0, "The SDK contract must not pass with an empty inventory");
        assertEquals(index, DartEventHandlerSource.methods(owner, "EventProbe").size());
        Path source = project.resolve("events.dart");
        Files.writeString(source, """
                import 'dart:async';
                import 'package:flutter/material.dart';
                import 'package:flutter/gestures.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter/services.dart';

                """ + new String(owner, StandardCharsets.UTF_8) + "\n" + assignments);

        List<String> get = new ArrayList<>();
        if (windows) get.addAll(List.of("cmd.exe", "/d", "/c"));
        get.addAll(List.of(flutter.toString(), "pub", "get", "--offline"));
        run(get, "pub-get.log");
        run(List.of(dart.toString(), "analyze", source.toString()),
                "analyze.log");
    }

    private void run(List<String> command, String logName) throws IOException, InterruptedException {
        Path log = project.resolve(logName);
        Process process = new ProcessBuilder(command).directory(project.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished = process.waitFor(180, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
        assertTrue(finished, "Timed out: " + command + "\n" + Files.readString(log));
        assertEquals(0, process.exitValue(), () -> {
            try {
                return command + "\n" + Files.readString(log);
            } catch (IOException failure) {
                return "Cannot read SDK test log: " + failure;
            }
        });
    }
}
