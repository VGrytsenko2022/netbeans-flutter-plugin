import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_web_transport_core.dart';

void main() {
  const nonce =
      '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

  test('requires one exact lowercase 256-bit session nonce', () {
    for (final invalid in <String>[
      '',
      '0' * 63,
      '0' * 65,
      'G' * 64,
      'A' * 64,
    ]) {
      expect(
        () => CanvasWebTransport.open(_Adapter(invalid)),
        throwsFormatException,
      );
    }
    expect(CanvasWebTransport.open(_Adapter(nonce)).sessionNonce, nonce);
  });

  test('admits only contiguous authenticated host chunks', () async {
    final adapter = _Adapter(nonce);
    final transport = CanvasWebTransport.open(adapter);
    final collected = transport.input.toList();

    adapter.deliver(nonce, 1, base64.encode(<int>[1, 2]));
    adapter.deliver(nonce, 2, base64.encode(<int>[3]));
    await transport.close();

    expect(await collected, <List<int>>[
      <int>[1, 2],
      <int>[3],
    ]);
    expect(transport.lastHostSequence, 2);
    expect(adapter.closed, [(nonce, 0)]);
  });

  test('fails closed on nonce drift without publishing the chunk', () async {
    final adapter = _Adapter(nonce);
    final transport = CanvasWebTransport.open(adapter);

    final expectation = expectLater(
      transport.input,
      emitsInOrder(<Object>[emitsError(isA<FormatException>()), emitsDone]),
    );
    adapter.deliver('f' * 64, 1, base64.encode(<int>[1]));

    await expectation;
    expect(transport.closed, isTrue);
    expect(transport.lastHostSequence, 0);
    expect(adapter.closed, [(nonce, 0)]);
  });

  test('fails closed on replay, gap, or out-of-order host sequence', () async {
    for (final invalidSequence in <int>[0, 2, 7]) {
      final adapter = _Adapter(nonce);
      final transport = CanvasWebTransport.open(adapter);
      final expectation = expectLater(
        transport.input,
        emitsInOrder(<Object>[emitsError(isA<FormatException>()), emitsDone]),
      );
      adapter.deliver(nonce, invalidSequence, base64.encode(<int>[1]));
      await expectation;
      expect(transport.lastHostSequence, 0);
    }
  });

  test(
    'fails closed when the JavaScript bridge terminates the session',
    () async {
      final adapter = _Adapter(nonce);
      final transport = CanvasWebTransport.open(adapter);
      final expectation = expectLater(
        transport.input,
        emitsInOrder(<Object>[emitsError(isA<StateError>()), emitsDone]),
      );

      adapter.terminate(nonce, 'authenticated host chunk is malformed');

      await expectation;
      expect(transport.closed, isTrue);
      expect(adapter.closed, [(nonce, 0)]);
    },
  );

  test('encodes immutable runner chunks with a monotonic sequence', () async {
    final adapter = _Adapter(nonce);
    final transport = CanvasWebTransport.open(adapter);
    final first = <int>[1, 2, 3];

    transport.output(first);
    first[0] = 9;
    transport.output(<int>[4]);
    await transport.flush();

    expect(adapter.runnerChunks, <(String, int, String)>[
      (nonce, 1, base64.encode(<int>[1, 2, 3])),
      (nonce, 2, base64.encode(<int>[4])),
    ]);
    expect(transport.lastRunnerSequence, 2);
  });

  test(
    'graceful close drains every previously accepted runner chunk',
    () async {
      final adapter = _Adapter(nonce);
      final transport = CanvasWebTransport.open(adapter);

      transport.output(<int>[1]);
      transport.output(<int>[2]);
      await transport.close();

      expect(adapter.runnerChunks, <(String, int, String)>[
        (nonce, 1, base64.encode(<int>[1])),
        (nonce, 2, base64.encode(<int>[2])),
      ]);
      expect(adapter.closed, [(nonce, 2)]);
    },
  );

  test('bounds diagnostics and closes exactly once', () async {
    final adapter = _Adapter(nonce);
    final transport = CanvasWebTransport.open(adapter);

    transport.diagnostic('x' * (maximumCanvasWebDiagnosticCharacters + 100));
    await transport.close();
    await transport.close();

    expect(adapter.diagnostics, hasLength(1));
    expect(
      adapter.diagnostics.single.$2.length,
      maximumCanvasWebDiagnosticCharacters,
    );
    expect(adapter.closed, [(nonce, 0)]);
    expect(() => transport.output(<int>[1]), throwsStateError);
  });

  test('rejects empty, malformed, non-canonical, and oversized chunks', () {
    for (final invalid in <String>['', 'A===', ' AQ==', 'AQ==\n', 'AB==']) {
      expect(() => decodeCanvasWebChunk(invalid), throwsFormatException);
    }
    expect(
      () => decodeCanvasWebChunk(
        base64.encode(List<int>.filled(maximumCanvasWebChunkBytes + 1, 0)),
      ),
      throwsFormatException,
    );
    expect(decodeCanvasWebChunk('AQ=='), <int>[1]);
  });

  test('rejects empty and oversized runner writes before bridge delivery', () {
    final adapter = _Adapter(nonce);
    final transport = CanvasWebTransport.open(adapter);

    expect(() => transport.output(const <int>[]), throwsRangeError);
    expect(
      () =>
          transport.output(List<int>.filled(maximumCanvasWebChunkBytes + 1, 0)),
      throwsRangeError,
    );
    expect(adapter.runnerChunks, isEmpty);
  });
}

final class _Adapter implements CanvasWebBridgeAdapter {
  _Adapter(this.sessionNonce);

  @override
  final String sessionNonce;

  void Function(String, int, String)? _receiver;
  void Function(String, String)? _terminalReceiver;
  final List<(String, int, String)> runnerChunks = [];
  final List<(String, String)> diagnostics = [];
  final List<(String, int)> closed = [];

  @override
  void registerRunner(
    void Function(String sessionNonce, int sequence, String encodedChunk)
    receiver,
    void Function(String sessionNonce, String reason) terminalReceiver,
  ) {
    if (_receiver != null || _terminalReceiver != null) {
      throw StateError('already registered');
    }
    _receiver = receiver;
    _terminalReceiver = terminalReceiver;
  }

  void deliver(String nonce, int sequence, String encodedChunk) =>
      _receiver!(nonce, sequence, encodedChunk);

  void terminate(String nonce, String reason) =>
      _terminalReceiver!(nonce, reason);

  @override
  void postRunnerChunk(
    String sessionNonce,
    int sequence,
    String encodedChunk,
  ) => runnerChunks.add((sessionNonce, sequence, encodedChunk));

  @override
  void postRunnerDiagnostic(String sessionNonce, String message) =>
      diagnostics.add((sessionNonce, message));

  @override
  void closeRunner(String sessionNonce, int lastRunnerSequence) =>
      closed.add((sessionNonce, lastRunnerSequence));
}
