Stream<List<int>> canvasRuntimeProcessInput() => Stream.error(
  UnsupportedError(
    'Flutter Web Canvas requires the authenticated browser bridge.',
  ),
);

void canvasRuntimeProcessOutput(List<int> bytes) {
  throw UnsupportedError(
    'Flutter Web Canvas requires the authenticated browser bridge.',
  );
}

Future<void> canvasRuntimeProcessFlush() async {}

void canvasRuntimeProcessDiagnostic(String message) {}

Never canvasRuntimeProcessExit(int code) {
  throw UnsupportedError('A browser Canvas cannot terminate its host process.');
}

String canvasRuntimeDartSdkVersion() => 'web';
