import 'dart:io';

Stream<List<int>> canvasRuntimeProcessInput() => stdin;

void canvasRuntimeProcessOutput(List<int> bytes) => stdout.add(bytes);

Future<void> canvasRuntimeProcessFlush() => stdout.flush();

void canvasRuntimeProcessDiagnostic(String message) => stderr.writeln(message);

Never canvasRuntimeProcessExit(int code) => exit(code);

String canvasRuntimeDartSdkVersion() => Platform.version.split(' ').first;
