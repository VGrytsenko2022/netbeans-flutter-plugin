Stream<List<int>> canvasRuntimeProcessInput() => Stream.error(
  UnsupportedError('Canvas process input is unavailable on this platform.'),
);

void canvasRuntimeProcessOutput(List<int> bytes) {
  throw UnsupportedError(
    'Canvas process output is unavailable on this platform.',
  );
}

Future<void> canvasRuntimeProcessFlush() async {}

void canvasRuntimeProcessDiagnostic(String message) {}

Never canvasRuntimeProcessExit(int code) {
  throw UnsupportedError(
    'Canvas process exit is unavailable on this platform.',
  );
}

String canvasRuntimeDartSdkVersion() => 'unknown';
