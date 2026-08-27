import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';

import 'canvas_model.dart';
import 'sha256.dart';

const nbfcControlJson = 1;
const nbfcModelJson = 2;
const nbfcCatalogJson = 3;
const _wireFormat = 'netbeans-flutter-canvas-wire';
const _runtimeFormat = 'netbeans-flutter-canvas-runtime';
const _protocolVersion = 1;
const _runnerVersion = '0.1.3-SNAPSHOT';

/// Keeps stdout reserved for NBFC frames even if runner code uses [print].
ZoneSpecification protocolOnlyStdoutZone(void Function(String) diagnostic) {
  return ZoneSpecification(
    print: (self, parent, zone, line) => diagnostic(line),
  );
}

class NbfcFrame {
  const NbfcFrame(this.kind, this.payload, this.digestHex);
  final int kind;
  final Uint8List payload;
  final String digestHex;
}

/// Encodes one complete NBFC version 1 frame.
Uint8List encodeNbfcFrame(int kind, List<int> payload) {
  if (kind < nbfcControlJson || kind > nbfcCatalogJson) {
    throw ArgumentError.value(kind, 'kind', 'Unsupported NBFC frame kind');
  }
  if (payload.isEmpty || payload.length > 0xffffffff) {
    throw ArgumentError.value(
      payload.length,
      'payload',
      'Invalid NBFC payload size',
    );
  }
  final owned = Uint8List.fromList(payload);
  final digest = sha256Bytes(owned);
  final result = Uint8List(44 + owned.length);
  result.setRange(0, 4, const [0x4e, 0x42, 0x46, 0x43]);
  result[4] = 1;
  result[5] = kind;
  final header = ByteData.sublistView(result);
  header.setUint32(8, owned.length, Endian.big);
  result.setRange(12, 44, digest);
  result.setRange(44, result.length, owned);
  return result;
}

class NbfcFrameReader {
  NbfcFrameReader(Stream<List<int>> input) : _iterator = StreamIterator(input);

  final StreamIterator<List<int>> _iterator;
  Uint8List _chunk = Uint8List(0);
  int _offset = 0;
  bool _ended = false;

  Future<void> cancel() => _iterator.cancel();

  Future<NbfcFrame?> read({
    required int maxPayloadBytes,
    int? expectedKind,
  }) async {
    final header = await _readExact(44, cleanEndAllowed: true);
    if (header == null) {
      return null;
    }
    if (header[0] != 0x4e ||
        header[1] != 0x42 ||
        header[2] != 0x46 ||
        header[3] != 0x43) {
      throw const FormatException('NBFC frame magic is invalid.');
    }
    if (header[4] != 1) {
      throw const FormatException('NBFC frame version is not supported.');
    }
    final kind = header[5];
    if (kind < nbfcControlJson || kind > nbfcCatalogJson) {
      throw const FormatException('NBFC frame kind is not supported.');
    }
    if (header[6] != 0 || header[7] != 0) {
      throw const FormatException('NBFC version 1 flags must be zero.');
    }
    if (expectedKind != null && kind != expectedKind) {
      throw FormatException(
        'Expected NBFC frame kind $expectedKind, received $kind.',
      );
    }
    final length = ByteData.sublistView(header).getUint32(8, Endian.big);
    if (length == 0 || length > maxPayloadBytes) {
      throw const FormatException(
        'NBFC payload length exceeds the active bound.',
      );
    }
    final payload = (await _readExact(length))!;
    final actualDigest = sha256Bytes(payload);
    var mismatch = 0;
    for (var index = 0; index < 32; index++) {
      mismatch |= actualDigest[index] ^ header[12 + index];
    }
    if (mismatch != 0) {
      throw const FormatException(
        'NBFC payload digest does not match its header.',
      );
    }
    return NbfcFrame(kind, payload, _hex(actualDigest));
  }

  Future<Uint8List?> _readExact(
    int count, {
    bool cleanEndAllowed = false,
  }) async {
    if (_ended) {
      if (cleanEndAllowed) {
        return null;
      }
      throw const FormatException(
        'NBFC stream ended before the declared frame length.',
      );
    }
    final result = Uint8List(count);
    var written = 0;
    while (written < count) {
      if (_offset >= _chunk.length) {
        if (!await _iterator.moveNext()) {
          _ended = true;
          if (cleanEndAllowed && written == 0) {
            return null;
          }
          throw const FormatException(
            'NBFC stream ended before the declared frame length.',
          );
        }
        _chunk = Uint8List.fromList(_iterator.current);
        _offset = 0;
        if (_chunk.isEmpty) {
          continue;
        }
      }
      final available = _chunk.length - _offset;
      final copied = math.min(available, count - written);
      result.setRange(written, written + copied, _chunk, _offset);
      written += copied;
      _offset += copied;
    }
    return result;
  }
}

class CanvasRuntimeController extends ChangeNotifier {
  CanvasRuntimeController({
    Stream<List<int>>? input,
    void Function(List<int>)? output,
    Future<void> Function()? flush,
    void Function(String)? diagnostic,
  }) : _reader = NbfcFrameReader(input ?? stdin),
       _output = output ?? stdout.add,
       _flush = flush ?? stdout.flush,
       _diagnostic = diagnostic ?? stderr.writeln,
       _ownsProcessIo = input == null && output == null && flush == null;

  final NbfcFrameReader _reader;
  final void Function(List<int>) _output;
  final Future<void> Function() _flush;
  final void Function(String) _diagnostic;
  final bool _ownsProcessIo;

  CanvasModel? _model;
  String? _selectedWidgetId;
  String? _errorMessage;
  String? _sessionId;
  _HandshakeLimits _limits = _HandshakeLimits.safe;
  int _runnerWireSequence = 1;
  int _intentSequence = 0;
  String? _lastPresentedIdentity;
  Future<void> _writeChain = Future.value();
  bool _closed = false;

  CanvasModel? get model => _model;
  String? get selectedWidgetId => _selectedWidgetId;
  String? get errorMessage => _errorMessage;
  bool get closed => _closed;

  Future<void> start() async {
    try {
      final first = await _reader.read(
        maxPayloadBytes: _HandshakeLimits.safe.maxControlMessageBytes,
        expectedKind: nbfcControlJson,
      );
      if (first == null) {
        throw const FormatException('Canvas host closed before host.hello.');
      }
      final hello = _decodeHostHello(first.payload);
      _sessionId = hello.sessionId;
      _limits = hello.limits.tightenedToSafe();
      await _writeControl(_runnerHello(hello));
      while (!_closed) {
        final frame = await _reader.read(
          maxPayloadBytes: _limits.maxControlMessageBytes,
          expectedKind: nbfcControlJson,
        );
        if (frame == null) {
          _closed = true;
          notifyListeners();
          return;
        }
        await _handleControl(frame.payload);
      }
    } on Object catch (error) {
      await _failClosed(error);
    }
  }

  void selectFromCanvas(String widgetId) {
    final current = _model;
    if (_closed || current == null || !current.widgetIds.contains(widgetId)) {
      return;
    }
    if (_selectedWidgetId != widgetId) {
      _selectedWidgetId = widgetId;
      notifyListeners();
    }
    final body = _identityBody(current)
      ..addAll({
        'frameSequence': 0,
        'layoutSequence': 0,
        'intentSequence': _intentSequence++,
        'widgetId': widgetId,
      });
    unawaited(_writeRuntime('runner.selection', body));
  }

  Future<void> _handleControl(Uint8List payload) async {
    final object = _decodeObject(payload, r'$');
    final format = object['format'];
    if (format == _wireFormat) {
      await _handleWireControl(object);
      return;
    }
    if (format != _runtimeFormat) {
      throw const FormatException('Canvas control format is not supported.');
    }
    _exactKeys(object, r'$', const {
      'format',
      'protocolVersion',
      'sessionId',
      'type',
      'body',
    });
    _requireVersionAndSession(object);
    final type = object['type'];
    if (type == 'host.render') {
      await _handleRender(_object(object['body'], r'$/body'));
    } else if (type == 'host.selection') {
      _handleHostSelection(_object(object['body'], r'$/body'));
    } else {
      throw FormatException(
        'Canvas runtime control type is not supported: $type',
      );
    }
  }

  Future<void> _handleRender(Map<String, Object?> body) async {
    _exactKeys(body, r'$/body', const {
      'presentationSequence',
      'documentId',
      'logicalRevisionId',
      'model',
    });
    final presentationSequence = _sequence(
      body['presentationSequence'],
      r'$/body/presentationSequence',
    );
    final documentId = _stableId(body['documentId'], r'$/body/documentId');
    final logicalRevisionId = _sequence(
      body['logicalRevisionId'],
      r'$/body/logicalRevisionId',
    );
    final descriptor = _PayloadDescriptor.decode(body['model']);
    if (descriptor.payloadBytes > _limits.maxModelBytes) {
      throw const FormatException(
        'Canvas model descriptor exceeds the negotiated bound.',
      );
    }
    final frame = await _reader.read(
      maxPayloadBytes: _limits.maxModelBytes,
      expectedKind: nbfcModelJson,
    );
    if (frame == null ||
        frame.payload.length != descriptor.payloadBytes ||
        frame.digestHex != descriptor.sha256) {
      throw const FormatException(
        'Canvas model frame does not match its admitted descriptor.',
      );
    }
    final next = CanvasModel.decode(frame.payload);
    if (next.sessionId != _sessionId ||
        next.presentationSequence != presentationSequence ||
        next.documentId != documentId ||
        next.logicalRevisionId != logicalRevisionId) {
      throw const FormatException(
        'Canvas model identity does not match host.render.',
      );
    }
    final current = _model;
    if (current != null) {
      if (presentationSequence < current.presentationSequence) {
        return;
      }
      if (presentationSequence == current.presentationSequence) {
        if (current.documentId != documentId ||
            current.logicalRevisionId != logicalRevisionId) {
          throw const FormatException(
            'Canvas presentation sequence was reused with another identity.',
          );
        }
        return;
      }
    }
    _model = next;
    _selectedWidgetId = null;
    _lastPresentedIdentity = null;
    notifyListeners();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_closed || !identical(_model, next)) {
        return;
      }
      final identity = _identity(next);
      if (_lastPresentedIdentity == identity) {
        return;
      }
      _lastPresentedIdentity = identity;
      unawaited(
        _writeRuntime(
          'runner.presented',
          _identityBody(next)
            ..addAll({'frameSequence': 0, 'layoutSequence': 0}),
        ),
      );
    });
  }

  void _handleHostSelection(Map<String, Object?> body) {
    _exactKeys(body, r'$/body', const {
      'presentationSequence',
      'documentId',
      'logicalRevisionId',
      'frameSequence',
      'layoutSequence',
      'widgetId',
    });
    final current = _model;
    if (current == null ||
        _sequence(
              body['presentationSequence'],
              r'$/body/presentationSequence',
            ) !=
            current.presentationSequence ||
        _stableId(body['documentId'], r'$/body/documentId') !=
            current.documentId ||
        _sequence(body['logicalRevisionId'], r'$/body/logicalRevisionId') !=
            current.logicalRevisionId ||
        _sequence(body['frameSequence'], r'$/body/frameSequence') != 0 ||
        _sequence(body['layoutSequence'], r'$/body/layoutSequence') != 0) {
      return;
    }
    final widgetId = _stableId(body['widgetId'], r'$/body/widgetId');
    if (!current.widgetIds.contains(widgetId)) {
      return;
    }
    if (_selectedWidgetId != widgetId) {
      _selectedWidgetId = widgetId;
      notifyListeners();
    }
  }

  Future<void> _handleWireControl(Map<String, Object?> object) async {
    _exactKeys(object, r'$', const {
      'format',
      'protocolVersion',
      'sessionId',
      'sequence',
      'type',
      'body',
    });
    _requireVersionAndSession(object);
    if (object['type'] != 'host.close') {
      throw const FormatException(
        'Only host.close is legal after Canvas handshake.',
      );
    }
    final sequence = _sequence(object['sequence'], r'$/sequence');
    final body = _object(object['body'], r'$/body');
    _exactKeys(body, r'$/body', const {'reason'});
    const reasons = {'formClosed', 'ideShutdown', 'restart', 'backendReplaced'};
    if (!reasons.contains(body['reason'])) {
      throw const FormatException('Canvas host.close reason is invalid.');
    }
    await _writeControl({
      'format': _wireFormat,
      'protocolVersion': _protocolVersion,
      'sessionId': _sessionId,
      'sequence': _runnerWireSequence++,
      'type': 'runner.closed',
      'replyTo': sequence,
      'body': <String, Object?>{},
    });
    _closed = true;
    notifyListeners();
    await _reader.cancel();
    if (_ownsProcessIo) {
      await _flush();
      exit(0);
    }
  }

  Future<void> _writeRuntime(String type, Map<String, Object?> body) =>
      _writeControl({
        'format': _runtimeFormat,
        'protocolVersion': _protocolVersion,
        'sessionId': _sessionId,
        'type': type,
        'body': body,
      });

  Future<void> _writeControl(Map<String, Object?> message) {
    final payload = utf8.encode(jsonEncode(message));
    if (payload.length > _limits.maxControlMessageBytes) {
      return Future.error(
        const FormatException(
          'Canvas control response exceeds the negotiated bound.',
        ),
      );
    }
    final bytes = encodeNbfcFrame(nbfcControlJson, payload);
    _writeChain = _writeChain.then((_) async {
      _output(bytes);
      await _flush();
    });
    return _writeChain;
  }

  Future<void> _failClosed(Object error) async {
    if (_closed) {
      return;
    }
    _closed = true;
    await _reader.cancel();
    final message = _boundedFailureMessage(error);
    _errorMessage = message;
    _diagnostic('Flutter Canvas protocol failure: $message');
    notifyListeners();
    if (_sessionId != null) {
      try {
        await _writeControl({
          'format': _wireFormat,
          'protocolVersion': _protocolVersion,
          'sessionId': _sessionId,
          'sequence': _runnerWireSequence++,
          'type': 'runner.failure',
          'body': {'code': 'invalidRequest', 'fatal': true, 'message': message},
        });
      } on Object catch (writeError) {
        _diagnostic(
          'Flutter Canvas could not report protocol failure: $writeError',
        );
      }
    }
  }

  void _requireVersionAndSession(Map<String, Object?> object) {
    if (object['protocolVersion'] != _protocolVersion ||
        object['sessionId'] != _sessionId) {
      throw const FormatException(
        'Canvas control identity or version is stale.',
      );
    }
  }
}

class _HostHello {
  const _HostHello(this.sessionId, this.capabilities, this.limits);
  final String sessionId;
  final Set<String> capabilities;
  final _HandshakeLimits limits;
}

_HostHello _decodeHostHello(Uint8List payload) {
  final object = _decodeObject(payload, r'$');
  _exactKeys(object, r'$', const {
    'format',
    'protocolVersion',
    'sessionId',
    'sequence',
    'type',
    'body',
  });
  if (object['format'] != _wireFormat ||
      object['protocolVersion'] != _protocolVersion ||
      object['sequence'] != 0 ||
      object['type'] != 'host.hello') {
    throw const FormatException(
      'First Canvas control message must be host.hello v1.',
    );
  }
  final sessionId = _stableId(object['sessionId'], r'$/sessionId');
  final body = _object(object['body'], r'$/body');
  _exactKeys(body, r'$/body', const {
    'hostVersion',
    'requestedCapabilities',
    'offeredLimits',
  });
  _boundedText(body['hostVersion'], r'$/body/hostVersion', 1, 128);
  final rawCapabilities = body['requestedCapabilities'];
  if (rawCapabilities is! List<Object?> || rawCapabilities.length > 3) {
    throw const FormatException('Canvas requested capabilities are invalid.');
  }
  const supported = {
    'readOnly.render',
    'readOnly.layout',
    'readOnly.selection',
  };
  final capabilities = <String>{};
  for (final value in rawCapabilities) {
    if (value is! String ||
        !supported.contains(value) ||
        !capabilities.add(value)) {
      throw const FormatException(
        'Canvas requested capability is not supported.',
      );
    }
  }
  if (!capabilities.contains('readOnly.render')) {
    throw const FormatException(
      'Canvas read-only rendering capability is required.',
    );
  }
  return _HostHello(
    sessionId,
    Set.unmodifiable(capabilities),
    _HandshakeLimits.decode(body['offeredLimits']),
  );
}

Map<String, Object?> _runnerHello(_HostHello hello) {
  const capabilityOrder = [
    'readOnly.render',
    'readOnly.layout',
    'readOnly.selection',
  ];
  final limits = hello.limits.tightenedToSafe();
  return {
    'format': _wireFormat,
    'protocolVersion': _protocolVersion,
    'sessionId': hello.sessionId,
    'sequence': 0,
    'type': 'runner.hello',
    'replyTo': 0,
    'body': {
      'runnerVersion': _runnerVersion,
      'engine': {
        'flutterVersion': 'bundled',
        'frameworkRevision': 'bundled',
        'engineRevision': 'bundled',
        'dartSdkVersion': Platform.version.split(' ').first,
      },
      'acceptedCapabilities': [
        for (final capability in capabilityOrder)
          if (hello.capabilities.contains(capability)) capability,
      ],
      'effectiveLimits': limits.toJson(),
    },
  };
}

class _HandshakeLimits {
  const _HandshakeLimits({
    required this.maxControlMessageBytes,
    required this.maxModelBytes,
    required this.maxCatalogBytes,
    required this.maxLayoutBytes,
    required this.maxEncodedImageBytes,
    required this.maxPhysicalDimension,
    required this.maxPhysicalPixels,
  });

  static const safe = _HandshakeLimits(
    maxControlMessageBytes: 262144,
    maxModelBytes: 16777216,
    maxCatalogBytes: 4194304,
    maxLayoutBytes: 8388608,
    maxEncodedImageBytes: 16777216,
    maxPhysicalDimension: 4096,
    maxPhysicalPixels: 8388608,
  );

  final int maxControlMessageBytes;
  final int maxModelBytes;
  final int maxCatalogBytes;
  final int maxLayoutBytes;
  final int maxEncodedImageBytes;
  final int maxPhysicalDimension;
  final int maxPhysicalPixels;

  static _HandshakeLimits decode(Object? value) {
    final object = _object(value, r'$/body/offeredLimits');
    _exactKeys(object, r'$/body/offeredLimits', const {
      'maxControlMessageBytes',
      'maxModelBytes',
      'maxCatalogBytes',
      'maxLayoutBytes',
      'maxEncodedImageBytes',
      'maxPhysicalDimension',
      'maxPhysicalPixels',
    });
    return _HandshakeLimits(
      maxControlMessageBytes: _positiveBounded(
        object['maxControlMessageBytes'],
        safe.maxControlMessageBytes,
      ),
      maxModelBytes: _positiveBounded(
        object['maxModelBytes'],
        safe.maxModelBytes,
      ),
      maxCatalogBytes: _positiveBounded(
        object['maxCatalogBytes'],
        safe.maxCatalogBytes,
      ),
      maxLayoutBytes: _positiveBounded(
        object['maxLayoutBytes'],
        safe.maxLayoutBytes,
      ),
      maxEncodedImageBytes: _positiveBounded(
        object['maxEncodedImageBytes'],
        safe.maxEncodedImageBytes,
      ),
      maxPhysicalDimension: _positiveBounded(
        object['maxPhysicalDimension'],
        safe.maxPhysicalDimension,
      ),
      maxPhysicalPixels: _positiveBounded(
        object['maxPhysicalPixels'],
        safe.maxPhysicalPixels,
      ),
    );
  }

  _HandshakeLimits tightenedToSafe() => _HandshakeLimits(
    maxControlMessageBytes: math.min(
      maxControlMessageBytes,
      safe.maxControlMessageBytes,
    ),
    maxModelBytes: math.min(maxModelBytes, safe.maxModelBytes),
    maxCatalogBytes: math.min(maxCatalogBytes, safe.maxCatalogBytes),
    maxLayoutBytes: math.min(maxLayoutBytes, safe.maxLayoutBytes),
    maxEncodedImageBytes: math.min(
      maxEncodedImageBytes,
      safe.maxEncodedImageBytes,
    ),
    maxPhysicalDimension: math.min(
      maxPhysicalDimension,
      safe.maxPhysicalDimension,
    ),
    maxPhysicalPixels: math.min(maxPhysicalPixels, safe.maxPhysicalPixels),
  );

  Map<String, Object?> toJson() => {
    'maxControlMessageBytes': maxControlMessageBytes,
    'maxModelBytes': maxModelBytes,
    'maxCatalogBytes': maxCatalogBytes,
    'maxLayoutBytes': maxLayoutBytes,
    'maxEncodedImageBytes': maxEncodedImageBytes,
    'maxPhysicalDimension': maxPhysicalDimension,
    'maxPhysicalPixels': maxPhysicalPixels,
  };
}

class _PayloadDescriptor {
  const _PayloadDescriptor(this.payloadBytes, this.sha256);
  final int payloadBytes;
  final String sha256;

  static _PayloadDescriptor decode(Object? value) {
    final object = _object(value, r'$/body/model');
    _exactKeys(object, r'$/body/model', const {
      'kind',
      'payloadBytes',
      'sha256',
    });
    if (object['kind'] != 'model.json') {
      throw const FormatException('Canvas payload descriptor kind is invalid.');
    }
    final payloadBytes = object['payloadBytes'];
    if (payloadBytes is! int || payloadBytes <= 0) {
      throw const FormatException(
        'Canvas payload descriptor length is invalid.',
      );
    }
    final digest = object['sha256'];
    if (digest is! String || !RegExp(r'^[0-9a-f]{64}$').hasMatch(digest)) {
      throw const FormatException(
        'Canvas payload descriptor digest is invalid.',
      );
    }
    return _PayloadDescriptor(payloadBytes, digest);
  }
}

Map<String, Object?> _identityBody(CanvasModel model) => {
  'presentationSequence': model.presentationSequence,
  'documentId': model.documentId,
  'logicalRevisionId': model.logicalRevisionId,
};

String _identity(CanvasModel model) =>
    '${model.sessionId}:${model.presentationSequence}:${model.documentId}:${model.logicalRevisionId}';

Map<String, Object?> _decodeObject(Uint8List bytes, String path) {
  final Object? decoded;
  try {
    decoded = jsonDecode(utf8.decode(bytes, allowMalformed: false));
  } on Object catch (error) {
    throw FormatException('Canvas control is not valid UTF-8 JSON: $error');
  }
  return _object(decoded, path);
}

Map<String, Object?> _object(Object? value, String path) {
  if (value is! Map<String, Object?>) {
    throw FormatException('Canvas value must be an object: $path');
  }
  return value;
}

void _exactKeys(
  Map<String, Object?> object,
  String path,
  Set<String> expected,
) {
  if (object.length != expected.length ||
      !object.keys.toSet().containsAll(expected)) {
    throw FormatException('Canvas object has missing or unknown fields: $path');
  }
}

String _stableId(Object? value, String path) {
  final text = _boundedText(value, path, 36, 36);
  if (!RegExp(
    r'^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
  ).hasMatch(text)) {
    throw FormatException('Canvas identity is invalid: $path');
  }
  return text;
}

int _sequence(Object? value, String path) {
  if (value is! int || value < 0 || value > maxCanvasSequence) {
    throw FormatException(
      'Canvas sequence is outside its interoperable range: $path',
    );
  }
  return value;
}

String _boundedText(Object? value, String path, int minimum, int maximum) {
  if (value is! String ||
      value.runes.length < minimum ||
      value.runes.length > maximum ||
      value.runes.any(
        (point) => point < 0x20 || (point >= 0x7f && point <= 0x9f),
      )) {
    throw FormatException('Canvas string is invalid: $path');
  }
  return value;
}

int _positiveBounded(Object? value, int maximum) {
  if (value is! int || value <= 0 || value > maximum) {
    throw const FormatException('Canvas handshake limit is invalid.');
  }
  return value;
}

String _boundedFailureMessage(Object error) {
  final text = error.toString().replaceAll(
    RegExp(r'[\x00-\x1f\x7f-\x9f]'),
    ' ',
  );
  final runes = text.runes.take(512).toList();
  return String.fromCharCodes(
    runes.isEmpty ? 'Canvas request failed.'.runes : runes,
  );
}

String _hex(List<int> bytes) =>
    bytes.map((byte) => byte.toRadixString(16).padLeft(2, '0')).join();
