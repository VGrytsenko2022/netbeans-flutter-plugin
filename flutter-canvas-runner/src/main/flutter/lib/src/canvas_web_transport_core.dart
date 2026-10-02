import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

const canvasWebBridgeFormat = 'netbeans-flutter-canvas-web-bridge';
const canvasWebBridgeVersion = 1;
const maximumCanvasWebChunkBytes = 1024 * 1024;
const maximumCanvasWebDiagnosticCharacters = 2048;

final RegExp _canvasWebSessionNoncePattern = RegExp(r'^[0-9a-f]{64}$');
final RegExp _canonicalBase64Pattern = RegExp(
  r'^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$',
);

abstract interface class CanvasWebBridgeAdapter {
  String get sessionNonce;

  void registerRunner(
    void Function(String sessionNonce, int sequence, String encodedChunk)
    receiver,
    void Function(String sessionNonce, String reason) terminalReceiver,
  );

  void postRunnerChunk(String sessionNonce, int sequence, String encodedChunk);

  void postRunnerDiagnostic(String sessionNonce, String message);

  void closeRunner(String sessionNonce, int lastRunnerSequence);
}

final class CanvasWebTransport {
  CanvasWebTransport._(this._adapter, this.sessionNonce)
    : _input = StreamController<List<int>>();

  factory CanvasWebTransport.open(CanvasWebBridgeAdapter adapter) {
    final nonce = adapter.sessionNonce;
    if (!_canvasWebSessionNoncePattern.hasMatch(nonce)) {
      throw const FormatException(
        'Flutter Web Canvas bridge supplied an invalid session nonce.',
      );
    }
    final transport = CanvasWebTransport._(adapter, nonce);
    adapter.registerRunner(
      transport._acceptHostChunk,
      transport._acceptBridgeFailure,
    );
    return transport;
  }

  final CanvasWebBridgeAdapter _adapter;
  final StreamController<List<int>> _input;
  final String sessionNonce;
  int _lastHostSequence = 0;
  int _lastRunnerSequence = 0;
  Future<void> _writeChain = Future<void>.value();
  Future<void>? _closeFuture;
  bool _closing = false;
  bool _closed = false;
  bool _adapterClosed = false;

  Stream<List<int>> get input => _input.stream;
  bool get closed => _closed;
  int get lastHostSequence => _lastHostSequence;
  int get lastRunnerSequence => _lastRunnerSequence;

  void output(List<int> bytes) {
    if (_closing || _closed) {
      throw StateError('Flutter Web Canvas bridge is closed.');
    }
    if (bytes.isEmpty || bytes.length > maximumCanvasWebChunkBytes) {
      throw RangeError.range(
        bytes.length,
        1,
        maximumCanvasWebChunkBytes,
        'bytes.length',
      );
    }
    final copy = Uint8List.fromList(bytes);
    final sequence = ++_lastRunnerSequence;
    final encoded = base64.encode(copy);
    _writeChain = _writeChain.then((_) {
      if (_closed) {
        return;
      }
      _adapter.postRunnerChunk(sessionNonce, sequence, encoded);
    });
  }

  Future<void> flush() => _writeChain;

  void diagnostic(String message) {
    if (_closing || _closed) {
      return;
    }
    final bounded = message.length <= maximumCanvasWebDiagnosticCharacters
        ? message
        : '${message.substring(0, maximumCanvasWebDiagnosticCharacters - 1)}…';
    _adapter.postRunnerDiagnostic(sessionNonce, bounded);
  }

  Future<void> close() => _closeFuture ??= _closeGracefully();

  Future<void> _closeGracefully() async {
    if (_closed) {
      return;
    }
    _closing = true;
    try {
      // Drain every write accepted before close. Marking the transport closed
      // before this await would silently discard the final protocol frame.
      await _writeChain;
    } finally {
      _closed = true;
      // A single-subscription controller's close future waits for a listener.
      // The runtime normally owns that listener, but setup failures may close
      // the bridge before runtime.start() attaches. Never let teardown hang on
      // that absent consumer.
      unawaited(_input.close());
      _notifyAdapterClosed();
    }
  }

  void _acceptHostChunk(
    String receivedNonce,
    int sequence,
    String encodedChunk,
  ) {
    if (_closing || _closed) {
      return;
    }
    try {
      if (receivedNonce != sessionNonce) {
        throw const FormatException(
          'Flutter Web Canvas bridge session nonce changed.',
        );
      }
      if (sequence != _lastHostSequence + 1) {
        throw const FormatException(
          'Flutter Web Canvas bridge host sequence is not contiguous.',
        );
      }
      final bytes = decodeCanvasWebChunk(encodedChunk);
      _lastHostSequence = sequence;
      _input.add(bytes);
    } on Object catch (error, stackTrace) {
      _failClosed(error, stackTrace);
    }
  }

  void _acceptBridgeFailure(String receivedNonce, String reason) {
    if (_closing || _closed) {
      return;
    }
    if (receivedNonce != sessionNonce) {
      _failClosed(
        const FormatException(
          'Flutter Web Canvas bridge terminal nonce changed.',
        ),
        StackTrace.current,
      );
      return;
    }
    _failClosed(
      StateError('Flutter Web Canvas bridge terminated: $reason'),
      StackTrace.current,
    );
  }

  void _failClosed(Object error, StackTrace stackTrace) {
    _closing = true;
    _closed = true;
    _input.addError(error, stackTrace);
    unawaited(_input.close());
    _notifyAdapterClosed();
  }

  void _notifyAdapterClosed() {
    if (_adapterClosed) {
      return;
    }
    _adapterClosed = true;
    _adapter.closeRunner(sessionNonce, _lastRunnerSequence);
  }
}

Uint8List decodeCanvasWebChunk(String encodedChunk) {
  if (encodedChunk.isEmpty ||
      encodedChunk.length % 4 != 0 ||
      encodedChunk.length > ((maximumCanvasWebChunkBytes + 2) ~/ 3) * 4 ||
      !_canonicalBase64Pattern.hasMatch(encodedChunk)) {
    throw const FormatException(
      'Flutter Web Canvas bridge chunk is not bounded canonical base64.',
    );
  }
  final decoded = base64.decode(encodedChunk);
  if (decoded.isEmpty ||
      decoded.length > maximumCanvasWebChunkBytes ||
      base64.encode(decoded) != encodedChunk) {
    throw const FormatException(
      'Flutter Web Canvas bridge chunk is not bounded canonical base64.',
    );
  }
  return Uint8List.fromList(decoded);
}
