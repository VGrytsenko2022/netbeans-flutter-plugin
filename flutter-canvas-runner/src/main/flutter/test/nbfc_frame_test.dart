import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';

void main() {
  test('SHA-256 matches the standard abc vector', () {
    expect(
      sha256Hex(utf8.encode('abc')),
      'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
    );
  });

  test('NBFC reader accepts a fragmented version 1 frame', () async {
    final payload = utf8.encode('{"ok":true}');
    final encoded = encodeNbfcFrame(nbfcControlJson, payload);
    final reader = NbfcFrameReader(
      Stream<List<int>>.fromIterable([
        encoded.sublist(0, 3),
        encoded.sublist(3, 17),
        encoded.sublist(17, 44),
        encoded.sublist(44),
      ]),
    );

    final frame = await reader.read(
      maxPayloadBytes: 1024,
      expectedKind: nbfcControlJson,
    );

    expect(frame, isNotNull);
    expect(frame!.kind, nbfcControlJson);
    expect(frame.payload, payload);
    expect(await reader.read(maxPayloadBytes: 1024), isNull);
  });

  test('NBFC reader fails closed on a digest mismatch', () async {
    final encoded = encodeNbfcFrame(nbfcModelJson, utf8.encode('{"root":{}}'));
    encoded[encoded.length - 1] ^= 1;
    final reader = NbfcFrameReader(Stream.value(encoded));

    expect(
      reader.read(maxPayloadBytes: 1024, expectedKind: nbfcModelJson),
      throwsFormatException,
    );
  });

  test('NBFC kind 4 carries opaque image bytes', () async {
    final payload = utf8.encode('not-json-image-bytes');
    final reader = NbfcFrameReader(
      Stream.value(encodeNbfcFrame(nbfcImageBytes, payload)),
    );

    final frame = await reader.read(
      maxPayloadBytes: 1024,
      expectedKind: nbfcImageBytes,
    );

    expect(frame!.kind, nbfcImageBytes);
    expect(frame.payload, payload);
    expect(frame.digestHex, sha256Hex(payload));
  });
}
