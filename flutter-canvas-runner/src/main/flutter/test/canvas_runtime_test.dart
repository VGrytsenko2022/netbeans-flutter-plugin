import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';

import 'canvas_model_test.dart' as fixture;

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('protocol stdout zone diverts print output to diagnostics', () {
    final diagnostics = <String>[];

    runZoned(
      () => Zone.current.print('runner diagnostic'),
      zoneSpecification: protocolOnlyStdoutZone(diagnostics.add),
    );

    expect(diagnostics, ['runner diagnostic']);
  });

  test('performs the standard wire v1 hello and close lifecycle', () async {
    final input = StreamController<List<int>>();
    final output = <List<int>>[];
    final diagnostics = <String>[];
    final runtime = CanvasRuntimeController(
      input: input.stream,
      output: (bytes) => output.add(List<int>.from(bytes)),
      flush: () async {},
      diagnostic: diagnostics.add,
    );

    final running = runtime.start();
    input.add(
      encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_hello()))),
    );
    input.add(
      encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_close()))),
    );
    await input.close();
    await running;

    expect(diagnostics, isEmpty);
    expect(runtime.closed, isTrue);
    final reader = NbfcFrameReader(Stream<List<int>>.fromIterable(output));
    final hello = await reader.read(
      maxPayloadBytes: 262144,
      expectedKind: nbfcControlJson,
    );
    final closed = await reader.read(
      maxPayloadBytes: 262144,
      expectedKind: nbfcControlJson,
    );
    final helloJson =
        jsonDecode(utf8.decode(hello!.payload)) as Map<String, Object?>;
    final closedJson =
        jsonDecode(utf8.decode(closed!.payload)) as Map<String, Object?>;

    expect(helloJson['type'], 'runner.hello');
    expect(helloJson['replyTo'], 0);
    expect(
      (helloJson['body'] as Map<String, Object?>)['acceptedCapabilities'],
      ['readOnly.render', 'readOnly.layout', 'readOnly.selection'],
    );
    expect(closedJson['type'], 'runner.closed');
    expect(closedJson['replyTo'], 1);
    expect(await reader.read(maxPayloadBytes: 262144), isNull);
  });

  test('does not replace a newer model with a stale presentation', () async {
    final input = StreamController<List<int>>();
    final output = <List<int>>[];
    final diagnostics = <String>[];
    final runtime = CanvasRuntimeController(
      input: input.stream,
      output: (bytes) => output.add(List<int>.from(bytes)),
      flush: () async {},
      diagnostic: diagnostics.add,
    );

    final running = runtime.start();
    input.add(
      encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_hello()))),
    );
    final current = fixture.modelBytesForViewTest();
    _addRender(input, current);
    final staleJson = jsonDecode(utf8.decode(current)) as Map<String, Object?>;
    staleJson['presentationSequence'] = 3;
    final stale = Uint8List.fromList(utf8.encode(jsonEncode(staleJson)));
    _addRender(input, stale);
    input.add(
      encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_close()))),
    );
    await input.close();
    await running;

    expect(diagnostics, isEmpty);
    expect(runtime.model, isNotNull);
    expect(runtime.model!.presentationSequence, 4);
    expect(runtime.model!.logicalRevisionId, 2);
  });
}

void _addRender(StreamController<List<int>> input, Uint8List model) {
  final json = jsonDecode(utf8.decode(model)) as Map<String, Object?>;
  final body = <String, Object?>{
    'presentationSequence': json['presentationSequence'],
    'documentId': json['documentId'],
    'logicalRevisionId': json['logicalRevisionId'],
    'model': {
      'kind': 'model.json',
      'payloadBytes': model.length,
      'sha256': sha256Hex(model),
    },
  };
  final control = <String, Object?>{
    'format': 'netbeans-flutter-canvas-runtime',
    'protocolVersion': 1,
    'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
    'type': 'host.render',
    'body': body,
  };
  input.add(encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(control))));
  input.add(encodeNbfcFrame(nbfcModelJson, model));
}

Map<String, Object?> _hello() => {
  'format': 'netbeans-flutter-canvas-wire',
  'protocolVersion': 1,
  'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
  'sequence': 0,
  'type': 'host.hello',
  'body': {
    'hostVersion': 'netbeans-flutter-plugin',
    'requestedCapabilities': [
      'readOnly.render',
      'readOnly.layout',
      'readOnly.selection',
    ],
    'offeredLimits': {
      'maxControlMessageBytes': 262144,
      'maxModelBytes': 16777216,
      'maxCatalogBytes': 4194304,
      'maxLayoutBytes': 8388608,
      'maxEncodedImageBytes': 16777216,
      'maxPhysicalDimension': 4096,
      'maxPhysicalPixels': 8388608,
    },
  },
};

Map<String, Object?> _close() => {
  'format': 'netbeans-flutter-canvas-wire',
  'protocolVersion': 1,
  'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
  'sequence': 1,
  'type': 'host.close',
  'body': {'reason': 'formClosed'},
};
