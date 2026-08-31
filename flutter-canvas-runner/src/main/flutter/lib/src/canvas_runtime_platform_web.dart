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

const _flutterVersion = String.fromEnvironment('NBFC_FLUTTER_VERSION');
const _frameworkRevision = String.fromEnvironment('NBFC_FRAMEWORK_REVISION');
const _engineRevision = String.fromEnvironment('NBFC_ENGINE_REVISION');
const _dartSdkVersion = String.fromEnvironment('NBFC_DART_SDK_VERSION');

String canvasRuntimeFlutterVersion() =>
    _requiredIdentity('NBFC_FLUTTER_VERSION', _flutterVersion);

String canvasRuntimeFrameworkRevision() =>
    _requiredIdentity('NBFC_FRAMEWORK_REVISION', _frameworkRevision);

String canvasRuntimeEngineRevision() =>
    _requiredIdentity('NBFC_ENGINE_REVISION', _engineRevision);

String canvasRuntimeDartSdkVersion() =>
    _requiredIdentity('NBFC_DART_SDK_VERSION', _dartSdkVersion);

String _requiredIdentity(String defineName, String value) {
  if (value.isEmpty ||
      value == 'bundled' ||
      value == 'unknown' ||
      value == 'web') {
    throw StateError(
      'Flutter Web Canvas requires an exact $defineName build identity.',
    );
  }
  return value;
}
