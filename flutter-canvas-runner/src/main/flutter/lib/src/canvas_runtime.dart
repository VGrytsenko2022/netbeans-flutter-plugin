import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'canvas_drop.dart';
import 'canvas_model.dart';
import 'sha256.dart';

const nbfcControlJson = 1;
const nbfcModelJson = 2;
const nbfcCatalogJson = 3;
const _wireFormat = 'netbeans-flutter-canvas-wire';
const _runtimeFormat = 'netbeans-flutter-canvas-runtime';
const _protocolVersion = 1;
const _runnerVersion = '0.1.3-SNAPSHOT';
const _paletteDropCapability = 'palette.drop.catalogInsert.v1';
const _paletteDropSourceAwareCapability = 'palette.drop.sourceAware.v1';
const _deleteSelectedWidgetCapability = 'widget.deleteSelection.v1';
const _inlineTextEditCapability = 'widget.inlineTextEdit.v1';
const _widgetMovePreviewCapability = 'widget.movePreview.v1';
const _viewportPresentationCapability = 'viewport.presentation.v1';
const _surfacePresentationCapability = 'surface.presentation.v1';
const _maximumDevicePixelRatioMicros = 2147483647;
const minimumCanvasZoomMicros = 250000;
const maximumCanvasZoomMicros = 2000000;
const canvasViewportMicros = 1000000;
const maximumInlineTextUtf16Units = 65536;
const maximumInlineTextUnicodeScalars = 32768;

String? inlineTextValidationError(String value) {
  final metrics = _inlineTextMetrics(value);
  if (metrics == null) {
    return 'Text must contain well-formed UTF-16.';
  }
  if (metrics.utf16Units > maximumInlineTextUtf16Units) {
    return 'Text exceeds $maximumInlineTextUtf16Units UTF-16 code units.';
  }
  if (metrics.unicodeScalars > maximumInlineTextUnicodeScalars) {
    return 'Text exceeds $maximumInlineTextUnicodeScalars Unicode scalar values.';
  }
  return null;
}

@visibleForTesting
int reserveNextCanvasLayoutSequence(int current, int? pending) =>
    pending ?? current + 1;

const _paletteDropChannel = MethodChannel(
  'dev.flutter.netbeans/canvas_palette_drop',
);
final _paletteDropTokenPattern = RegExp(
  r'^nbfdnd:v1:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
);

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

/// View-only placement of one fixed logical Flutter viewport in the native
/// Canvas host. None of these values changes the model or its MediaQuery size.
@immutable
class CanvasViewportPresentation {
  const CanvasViewportPresentation({
    required this.commandSequence,
    required this.presentationSequence,
    required this.documentId,
    required this.logicalRevisionId,
    required this.mode,
    required this.zoomMicros,
    required this.horizontalScrollMicros,
    required this.verticalScrollMicros,
  });

  factory CanvasViewportPresentation.fit(CanvasModel model) =>
      CanvasViewportPresentation(
        commandSequence: 0,
        presentationSequence: model.presentationSequence,
        documentId: model.documentId,
        logicalRevisionId: model.logicalRevisionId,
        mode: 'fit',
        zoomMicros: canvasViewportMicros,
        horizontalScrollMicros: 0,
        verticalScrollMicros: 0,
      );

  final int commandSequence;
  final int presentationSequence;
  final String documentId;
  final int logicalRevisionId;
  final String mode;
  final int zoomMicros;
  final int horizontalScrollMicros;
  final int verticalScrollMicros;

  CanvasViewportPresentation copyWith({
    String? mode,
    int? zoomMicros,
    int? horizontalScrollMicros,
    int? verticalScrollMicros,
  }) => CanvasViewportPresentation(
    commandSequence: commandSequence,
    presentationSequence: presentationSequence,
    documentId: documentId,
    logicalRevisionId: logicalRevisionId,
    mode: mode ?? this.mode,
    zoomMicros: (zoomMicros ?? this.zoomMicros).clamp(
      minimumCanvasZoomMicros,
      maximumCanvasZoomMicros,
    ),
    horizontalScrollMicros:
        (horizontalScrollMicros ?? this.horizontalScrollMicros).clamp(
          0,
          canvasViewportMicros,
        ),
    verticalScrollMicros: (verticalScrollMicros ?? this.verticalScrollMicros)
        .clamp(0, canvasViewportMicros),
  );

  bool matchesModel(CanvasModel model) =>
      presentationSequence == model.presentationSequence &&
      documentId == model.documentId &&
      logicalRevisionId == model.logicalRevisionId;

  bool hasSameGeometry(CanvasViewportPresentation other) =>
      presentationSequence == other.presentationSequence &&
      documentId == other.documentId &&
      logicalRevisionId == other.logicalRevisionId &&
      mode == other.mode &&
      zoomMicros == other.zoomMicros &&
      horizontalScrollMicros == other.horizontalScrollMicros &&
      verticalScrollMicros == other.verticalScrollMicros;

  Map<String, Object?> toBody() => {
    'commandSequence': commandSequence,
    'presentationSequence': presentationSequence,
    'documentId': documentId,
    'logicalRevisionId': logicalRevisionId,
    'mode': mode,
    'zoomMicros': zoomMicros,
    'horizontalScrollMicros': horizontalScrollMicros,
    'verticalScrollMicros': verticalScrollMicros,
  };

  @override
  bool operator ==(Object other) =>
      other is CanvasViewportPresentation &&
      commandSequence == other.commandSequence &&
      presentationSequence == other.presentationSequence &&
      documentId == other.documentId &&
      logicalRevisionId == other.logicalRevisionId &&
      mode == other.mode &&
      zoomMicros == other.zoomMicros &&
      horizontalScrollMicros == other.horizontalScrollMicros &&
      verticalScrollMicros == other.verticalScrollMicros;

  @override
  int get hashCode => Object.hash(
    commandSequence,
    presentationSequence,
    documentId,
    logicalRevisionId,
    mode,
    zoomMicros,
    horizontalScrollMicros,
    verticalScrollMicros,
  );
}

/// Post-layout facts for one exact viewport presentation.
@immutable
class CanvasViewportMetrics {
  const CanvasViewportMetrics({
    required this.presentation,
    required this.effectiveScaleMicros,
    required this.horizontalScrollable,
    required this.verticalScrollable,
  });

  final CanvasViewportPresentation presentation;
  final int effectiveScaleMicros;
  final bool horizontalScrollable;
  final bool verticalScrollable;

  Map<String, Object?> toBody() => presentation.toBody()
    ..addAll({
      'effectiveScaleMicros': effectiveScaleMicros,
      'horizontalScrollable': horizontalScrollable,
      'verticalScrollable': verticalScrollable,
    });

  @override
  bool operator ==(Object other) =>
      other is CanvasViewportMetrics &&
      presentation == other.presentation &&
      effectiveScaleMicros == other.effectiveScaleMicros &&
      horizontalScrollable == other.horizontalScrollable &&
      verticalScrollable == other.verticalScrollable;

  @override
  int get hashCode => Object.hash(
    presentation,
    effectiveScaleMicros,
    horizontalScrollable,
    verticalScrollable,
  );
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

class CanvasRuntimeController extends ChangeNotifier
    with WidgetsBindingObserver {
  CanvasRuntimeController({
    Stream<List<int>>? input,
    void Function(List<int>)? output,
    Future<void> Function()? flush,
    void Function(String)? diagnostic,
    this.nativeDropAvailabilityProbe,
  }) : _reader = NbfcFrameReader(input ?? stdin),
       _output = output ?? stdout.add,
       _flush = flush ?? stdout.flush,
       _diagnostic = diagnostic ?? stderr.writeln,
       _ownsProcessIo = input == null && output == null && flush == null;

  final NbfcFrameReader _reader;
  final void Function(List<int>) _output;
  final Future<void> Function() _flush;
  final void Function(String) _diagnostic;
  @visibleForTesting
  final Future<bool> Function()? nativeDropAvailabilityProbe;
  final bool _ownsProcessIo;

  CanvasModel? _model;
  String? _selectedWidgetId;
  String? _errorMessage;
  String? _sessionId;
  _HandshakeLimits _limits = _HandshakeLimits.safe;
  int _runnerWireSequence = 1;
  int _intentSequence = 0;
  int _interactionFenceSequence = 0;
  int _interactionFenceApplicationTicket = 0;
  bool _interactionInputSynchronized = false;
  String? _appliedInteractionFenceIdentity;
  int? _appliedInteractionFenceLayoutSequence;
  int _layoutSequence = 0;
  int? _pendingLayoutSequence;
  int _layoutPublicationTicket = 0;
  String? _lastPresentedIdentity;
  CanvasDropResolver? _dropResolver;
  CanvasMovePreviewResolver? _movePreviewResolver;
  CanvasDropTarget? _dropHoverTarget;
  CanvasDropTarget? _widgetMovePreviewTarget;
  int _widgetMovePreviewSequence = -1;
  int _nativeHoverGeneration = -1;
  int _nativeHoverProbeId = -1;
  String? _nativeHoverToken;
  bool _nativeHoverGenerationClosed = false;
  _PreparedNativeDrop? _preparedNativeDrop;
  Future<void> _writeChain = Future.value();
  bool _paletteDropNegotiated = false;
  bool _paletteDropSourceAwareNegotiated = false;
  bool _deleteSelectedWidgetNegotiated = false;
  bool _inlineTextEditNegotiated = false;
  bool _widgetMovePreviewNegotiated = false;
  bool _viewportPresentationNegotiated = false;
  bool _surfacePresentationNegotiated = false;
  CanvasViewportPresentation? _viewportPresentation;
  CanvasViewportMetrics? _viewportMetrics;
  String? _lastViewportPublication;
  bool _bindingObserverInstalled = false;
  bool _nativeDropHandlerInstalled = false;
  bool _closed = false;
  _PaletteDragSourceAuthority? _paletteDragSourceAuthority;

  CanvasModel? get model => _model;
  String? get selectedWidgetId => _selectedWidgetId;
  String? get errorMessage => _errorMessage;
  bool get closed => _closed;
  CanvasDropTarget? get dropHoverTarget =>
      _widgetMovePreviewTarget ?? _dropHoverTarget;
  bool get hasWidgetMovePreview => _widgetMovePreviewTarget != null;
  @visibleForTesting
  CanvasDropTarget? get widgetMovePreviewTarget => _widgetMovePreviewTarget;
  CanvasViewportPresentation? get viewportPresentation => _viewportPresentation;
  bool get inlineTextEditNegotiated => _inlineTextEditNegotiated;

  @visibleForTesting
  String? get paletteDragSourceToken =>
      _paletteDragSourceAuthority?.source.token;

  @visibleForTesting
  int? get presentedLayoutSequence {
    final current = _model;
    return current != null && _lastPresentedIdentity == _identity(current)
        ? _layoutSequence
        : null;
  }

  @visibleForTesting
  int? get pendingLayoutSequence => _pendingLayoutSequence;

  @visibleForTesting
  int get interactionFenceSequence => _interactionFenceSequence;

  bool get interactionInputSynchronized =>
      _interactionInputEnabledForCurrentLayout();

  bool _interactionInputEnabledForCurrentLayout() {
    final current = _model;
    if (_closed ||
        !_interactionInputSynchronized ||
        current == null ||
        _pendingLayoutSequence != null) {
      return false;
    }
    final identity = _identity(current);
    return _lastPresentedIdentity == identity &&
        _appliedInteractionFenceIdentity == identity &&
        _appliedInteractionFenceLayoutSequence == _layoutSequence;
  }

  /// Completes the currently reserved post-layout publication in protocol
  /// tests that do not own a live Flutter render surface.
  @visibleForTesting
  void completePendingLayoutForTesting() {
    final current = _model;
    final pending = _pendingLayoutSequence;
    if (current == null || pending == null) {
      throw StateError('Canvas has no pending layout publication.');
    }
    _completeLayoutPublication(current, pending, _layoutPublicationTicket);
  }

  /// Registers the current Flutter render-tree hit tester without granting it
  /// model or persistence authority.
  void setDropResolver(Object? resolver) {
    if (resolver != null &&
        resolver is! CanvasDropResolver &&
        resolver is! LegacyCanvasDropResolver) {
      throw ArgumentError.value(resolver, 'resolver');
    }
    _dropResolver = switch (resolver) {
      CanvasDropResolver value => value,
      LegacyCanvasDropResolver value => (xMicros, yMicros, [source]) => value(
        xMicros,
        yMicros,
      ),
      _ => null,
    };
    if (resolver == null) {
      _preparedNativeDrop = null;
      _setDropHoverTarget(null);
      _invalidateNativeHoverApproval();
    }
  }

  /// Registers the current render-tree geometry edge used only for a
  /// Java-authorized widget move preview.
  void setMovePreviewResolver(CanvasMovePreviewResolver? resolver) {
    _movePreviewResolver = resolver;
    if (resolver == null) {
      _setWidgetMovePreviewTarget(null);
    }
  }

  Future<void> start() async {
    try {
      if (_ownsProcessIo) {
        WidgetsBinding.instance.addObserver(this);
        _bindingObserverInstalled = true;
        _paletteDropChannel.setMethodCallHandler(_handleNativeDropMethod);
        _nativeDropHandlerInstalled = true;
      }
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
      _paletteDropNegotiated =
          hello.capabilities.contains(_paletteDropCapability) &&
          await _detectNativeDropAvailability();
      _paletteDropSourceAwareNegotiated =
          _paletteDropNegotiated &&
          hello.capabilities.contains(_paletteDropSourceAwareCapability);
      _deleteSelectedWidgetNegotiated = hello.capabilities.contains(
        _deleteSelectedWidgetCapability,
      );
      _inlineTextEditNegotiated = hello.capabilities.contains(
        _inlineTextEditCapability,
      );
      _widgetMovePreviewNegotiated = hello.capabilities.contains(
        _widgetMovePreviewCapability,
      );
      _viewportPresentationNegotiated = hello.capabilities.contains(
        _viewportPresentationCapability,
      );
      _surfacePresentationNegotiated = hello.capabilities.contains(
        _surfacePresentationCapability,
      );
      await _writeControl(
        _runnerHello(hello, paletteDropAvailable: _paletteDropNegotiated),
      );
      while (!_closed) {
        final frame = await _reader.read(
          maxPayloadBytes: _limits.maxControlMessageBytes,
          expectedKind: nbfcControlJson,
        );
        if (frame == null) {
          _closed = true;
          _lastPresentedIdentity = null;
          _invalidateInteractionFenceApplication(notify: false);
          _pendingLayoutSequence = null;
          _layoutPublicationTicket++;
          _preparedNativeDrop = null;
          _paletteDragSourceAuthority = null;
          _setDropHoverTarget(null);
          _setWidgetMovePreviewTarget(null);
          notifyListeners();
          return;
        }
        await _handleControl(frame.payload);
      }
    } on Object catch (error) {
      await _failClosed(error);
    } finally {
      if (_bindingObserverInstalled) {
        _bindingObserverInstalled = false;
        WidgetsBinding.instance.removeObserver(this);
      }
      if (_nativeDropHandlerInstalled) {
        _nativeDropHandlerInstalled = false;
        _paletteDropChannel.setMethodCallHandler(null);
      }
    }
  }

  @override
  void didChangeMetrics() {
    final current = _model;
    if (_closed || current == null) {
      return;
    }
    final identity = _identity(current);
    if (_lastPresentedIdentity != identity) {
      return;
    }
    final nextLayoutSequence = reserveNextCanvasLayoutSequence(
      _layoutSequence,
      _pendingLayoutSequence,
    );
    if (nextLayoutSequence > maxCanvasSequence) {
      _lastPresentedIdentity = null;
      unawaited(
        _failClosed(
          StateError('Canvas layout sequence is exhausted.'),
          cancelReader: false,
        ),
      );
      return;
    }
    // Interaction is invalid immediately; a new exact layout identity is
    // published only after Flutter has completed the resized frame.
    _lastPresentedIdentity = null;
    _invalidateInteractionFenceApplication();
    _preparedNativeDrop = null;
    _paletteDragSourceAuthority = null;
    _setDropHoverTarget(null);
    _setWidgetMovePreviewTarget(null);
    _invalidateNativeHoverApproval();
    _scheduleLayoutPublication(current, nextLayoutSequence);
  }

  Future<bool> _detectNativeDropAvailability() async {
    try {
      final probe = nativeDropAvailabilityProbe;
      if (probe != null) {
        return await probe();
      }
      // Unit tests and other injected protocol transports do not own a native
      // FlutterView. Preserve their deterministic protocol fixture unless a
      // test explicitly supplies a probe.
      if (!_ownsProcessIo) {
        return true;
      }
      return await _paletteDropChannel.invokeMethod<bool>('isAvailable') ==
          true;
    } on Object catch (error) {
      _diagnostic(
        'Flutter Canvas native palette DnD is unavailable; '
        'continuing read-only: $error',
      );
      return false;
    }
  }

  void selectFromCanvas(String widgetId) {
    final current = _model;
    if (_closed ||
        current == null ||
        !_interactionInputEnabledForCurrentLayout() ||
        _lastPresentedIdentity != _identity(current) ||
        !current.widgetIds.contains(widgetId)) {
      return;
    }
    if (_selectedWidgetId != widgetId) {
      _selectedWidgetId = widgetId;
      notifyListeners();
    }
    final body = _identityBody(current)
      ..addAll({
        'frameSequence': 0,
        'layoutSequence': _layoutSequence,
        'intentSequence': _intentSequence++,
        'widgetId': widgetId,
      });
    unawaited(_writeRuntime('runner.selection', body));
  }

  /// Publishes one physical pointer-down for the exact admitted Canvas layout.
  ///
  /// This carries no model-mutation authority. Java may treat it as bounded
  /// focus intent only after admitting the exact current
  /// session/revision/frame/layout and host interaction fence.
  void interactFromCanvas() {
    final current = _model;
    if (_closed ||
        current == null ||
        !_interactionInputEnabledForCurrentLayout()) {
      return;
    }
    final body = _identityBody(current)
      ..addAll({
        'frameSequence': 0,
        'layoutSequence': _layoutSequence,
        'intentSequence': _intentSequence++,
        'interactionFenceSequence': _interactionFenceSequence,
      });
    unawaited(_writeRuntime('runner.interaction', body));
  }

  /// Applies wheel or scrollbar interaction produced by the Flutter Canvas.
  /// The immutable model identity must remain exact; only view presentation is
  /// allowed to change locally.
  void updateViewportFromCanvas(CanvasViewportPresentation next) {
    final current = _model;
    if (_closed ||
        !_viewportPresentationNegotiated ||
        current == null ||
        !_interactionInputEnabledForCurrentLayout() ||
        !next.matchesModel(current)) {
      return;
    }
    _applyViewportPresentation(next);
  }

  /// Accepts post-frame viewport facts. Stale callbacks are ignored, and an
  /// unchanged report is never echoed repeatedly.
  void reportViewportMetrics(CanvasViewportMetrics metrics) {
    final current = _model;
    if (_closed ||
        !_viewportPresentationNegotiated ||
        current == null ||
        metrics.presentation != _viewportPresentation ||
        !metrics.presentation.matchesModel(current)) {
      return;
    }
    _viewportMetrics = metrics;
    _publishViewportIfReady();
  }

  void _applyViewportPresentation(CanvasViewportPresentation next) {
    final previous = _viewportPresentation;
    if (previous == next) {
      _lastViewportPublication = null;
      _publishViewportIfReady();
      return;
    }
    if (previous != null && previous.hasSameGeometry(next)) {
      final previousMetrics = _viewportMetrics;
      _viewportPresentation = next;
      _viewportMetrics = previousMetrics == null
          ? null
          : CanvasViewportMetrics(
              presentation: next,
              effectiveScaleMicros: previousMetrics.effectiveScaleMicros,
              horizontalScrollable: previousMetrics.horizontalScrollable,
              verticalScrollable: previousMetrics.verticalScrollable,
            );
      _lastViewportPublication = null;
      notifyListeners();
      _publishViewportIfReady();
      return;
    }
    _viewportPresentation = next;
    _viewportMetrics = null;
    _lastViewportPublication = null;
    _invalidateLayoutForViewportChange();
    notifyListeners();
  }

  void _invalidateLayoutForViewportChange() {
    final current = _model;
    if (current == null) {
      return;
    }
    final nextLayoutSequence = reserveNextCanvasLayoutSequence(
      _layoutSequence,
      _pendingLayoutSequence,
    );
    if (nextLayoutSequence > maxCanvasSequence) {
      _lastPresentedIdentity = null;
      unawaited(
        _failClosed(
          StateError('Canvas layout sequence is exhausted.'),
          cancelReader: false,
        ),
      );
      return;
    }
    _lastPresentedIdentity = null;
    _invalidateInteractionFenceApplication();
    _preparedNativeDrop = null;
    _paletteDragSourceAuthority = null;
    _setDropHoverTarget(null);
    _setWidgetMovePreviewTarget(null);
    _invalidateNativeHoverApproval();
    _scheduleLayoutPublication(current, nextLayoutSequence);
  }

  void _publishViewportIfReady() {
    final metrics = _viewportMetrics;
    if (!_viewportPresentationNegotiated || metrics == null || _closed) {
      return;
    }
    final body = metrics.toBody();
    final publication = jsonEncode(body);
    if (_lastViewportPublication == publication) {
      return;
    }
    _lastViewportPublication = publication;
    unawaited(_writeRuntime('runner.viewport', body));
  }

  /// Publishes one physical Delete intent without mutating the decoded model.
  ///
  /// The Java host remains the only persistence authority and revalidates the
  /// complete session/layout/selection identity before applying a command.
  bool deleteSelectedFromCanvas() {
    final current = _model;
    final widgetId = _selectedWidgetId;
    if (_closed ||
        !_deleteSelectedWidgetNegotiated ||
        current == null ||
        widgetId == null ||
        widgetId == current.root.id ||
        !_interactionInputEnabledForCurrentLayout() ||
        _lastPresentedIdentity != _identity(current) ||
        !current.widgetIds.contains(widgetId)) {
      return false;
    }
    final body = _identityBody(current)
      ..addAll({
        'frameSequence': 0,
        'layoutSequence': _layoutSequence,
        'intentSequence': _intentSequence++,
        'widgetId': widgetId,
      });
    unawaited(_writeRuntime('runner.deleteSelection', body));
    return true;
  }

  /// Publishes one final, composition-free edit of an exact Text.data value.
  ///
  /// The Java host remains the persistence authority. The immutable model is
  /// never changed in the runner, and preedit/composing values are never sent.
  bool commitInlineTextEdit(
    String widgetId,
    String text,
    bool compositionObserved,
  ) {
    final current = _model;
    final selected = _selectedWidgetId;
    if (_closed ||
        !_inlineTextEditNegotiated ||
        current == null ||
        selected != widgetId ||
        !_interactionInputEnabledForCurrentLayout() ||
        _lastPresentedIdentity != _identity(current) ||
        inlineTextValidationError(text) != null) {
      return false;
    }
    final node = _findNode(current.root, widgetId);
    if (node == null || node.type != 'flutter.widgets.Text') {
      return false;
    }
    final body = _identityBody(current)
      ..addAll({
        'frameSequence': 0,
        'layoutSequence': _layoutSequence,
        'intentSequence': _intentSequence++,
        'interactionFenceSequence': _interactionFenceSequence,
        'widgetId': widgetId,
        'text': text,
        'compositionObserved': compositionObserved,
      });
    unawaited(_writeRuntime('runner.textEditCommit', body));
    return true;
  }

  Future<Object?> _handleNativeDropMethod(MethodCall call) async {
    return switch (call.method) {
      'paletteHover' => receiveNativePaletteHover(call.arguments),
      'paletteHoverLeave' => receiveNativePaletteHoverLeave(call.arguments),
      'paletteDropPrepare' => receiveNativePaletteDropPrepare(call.arguments),
      'paletteDropCommit' => receiveNativePaletteDropCommit(call.arguments),
      'paletteDropCancel' => receiveNativePaletteDropCancel(call.arguments),
      _ => throw MissingPluginException(
        'Unsupported native Canvas method: ${call.method}',
      ),
    };
  }

  /// Resolves a coalesced native OLE hover probe against current Flutter
  /// geometry. Only the newest probe in the newest open drag generation may
  /// publish a visible target.
  @visibleForTesting
  Future<bool> receiveNativePaletteHover(Object? arguments) async {
    if (_closed ||
        !_paletteDropNegotiated ||
        !_interactionInputEnabledForCurrentLayout()) {
      return false;
    }
    final object = _tryNativeObject(arguments, r'$/nativePaletteHover', const {
      'token',
      'xMicros',
      'yMicros',
      'generation',
      'probeId',
    });
    if (object == null) {
      return false;
    }
    int generation;
    int probeId;
    try {
      generation = _sequence(
        object['generation'],
        r'$/nativePaletteHover/generation',
      );
      probeId = _sequence(object['probeId'], r'$/nativePaletteHover/probeId');
    } on FormatException {
      return false;
    }
    if (generation < _nativeHoverGeneration ||
        (generation == _nativeHoverGeneration &&
            (_nativeHoverGenerationClosed || probeId <= _nativeHoverProbeId))) {
      return false;
    }

    if (generation > _nativeHoverGeneration) {
      _preparedNativeDrop = null;
      _nativeHoverGeneration = generation;
      _nativeHoverProbeId = -1;
      _nativeHoverToken = null;
      _nativeHoverGenerationClosed = false;
      _setDropHoverTarget(null);
    }
    _nativeHoverProbeId = probeId;

    try {
      final token = _boundedText(
        object['token'],
        r'$/nativePaletteHover/token',
        1,
        160,
      );
      if (!_paletteDropTokenPattern.hasMatch(token) ||
          (_nativeHoverToken != null && _nativeHoverToken != token)) {
        _nativeHoverGenerationClosed = true;
        _setDropHoverTarget(null);
        return false;
      }
      _nativeHoverToken = token;
      final target = _resolveValidDropTarget(
        _surfaceMicros(object['xMicros'], r'$/nativePaletteHover/xMicros'),
        _surfaceMicros(object['yMicros'], r'$/nativePaletteHover/yMicros'),
        token,
      );
      _setDropHoverTarget(target);
      return target != null;
    } on FormatException {
      _setDropHoverTarget(null);
      return false;
    }
  }

  /// Clears hover only when the leave belongs to the current or a newer drag.
  @visibleForTesting
  Future<bool> receiveNativePaletteHoverLeave(Object? arguments) async {
    if (_closed || !_paletteDropNegotiated) {
      return false;
    }
    final object = _tryNativeObject(
      arguments,
      r'$/nativePaletteHoverLeave',
      const {'generation'},
    );
    if (object == null) {
      return false;
    }
    try {
      final generation = _sequence(
        object['generation'],
        r'$/nativePaletteHoverLeave/generation',
      );
      if (generation < _nativeHoverGeneration) {
        return false;
      }
      _nativeHoverGeneration = generation;
      _nativeHoverProbeId = -1;
      _nativeHoverToken = null;
      _nativeHoverGenerationClosed = true;
      final prepared = _preparedNativeDrop;
      if (prepared != null && prepared.generation <= generation) {
        _preparedNativeDrop = null;
      }
      _setDropHoverTarget(null);
      _paletteDragSourceAuthority = null;
      return true;
    } on FormatException {
      return false;
    }
  }

  /// Phase one validates and stores exactly one semantic intent, but emits no
  /// Java mutation. A timed-out native OLE call can therefore cancel without a
  /// late `runner.paletteDrop` side effect.
  @visibleForTesting
  Future<bool> receiveNativePaletteDropPrepare(Object? arguments) async {
    if (_closed ||
        !_paletteDropNegotiated ||
        !_interactionInputEnabledForCurrentLayout()) {
      return false;
    }
    final closeGeneration = _tryNativeGeneration(
      arguments,
      r'$/nativePaletteDropPrepare/generation',
    );
    var prepared = false;
    try {
      final request = _NativeDropRequest.decode(
        arguments,
        r'$/nativePaletteDropPrepare',
      );
      final approvedTarget = _dropHoverTarget;
      if (request.generation != _nativeHoverGeneration ||
          request.probeId != _nativeHoverProbeId ||
          _nativeHoverGenerationClosed ||
          request.token != _nativeHoverToken ||
          approvedTarget == null) {
        return false;
      }
      final current = _model;
      final target = _resolveValidDropTarget(
        request.xMicros,
        request.yMicros,
        request.token,
      );
      if (current == null ||
          target == null ||
          !_sameSemanticDropTarget(target, approvedTarget)) {
        return false;
      }
      _preparedNativeDrop = _PreparedNativeDrop(
        request: request,
        target: target,
        presentationSequence: current.presentationSequence,
        documentId: current.documentId,
        logicalRevisionId: current.logicalRevisionId,
        layoutSequence: _layoutSequence,
      );
      prepared = true;
      return true;
    } on FormatException {
      return false;
    } finally {
      if (closeGeneration == _nativeHoverGeneration) {
        if (!prepared) {
          _preparedNativeDrop = null;
        }
        _nativeHoverGenerationClosed = true;
        _nativeHoverToken = null;
        _setDropHoverTarget(null);
      } else if (closeGeneration == null) {
        _preparedNativeDrop = null;
        _setDropHoverTarget(null);
      }
    }
  }

  /// Phase two is sent only on the native OLE MOVE path. It consumes the
  /// prepared intent exactly once, revalidates current identity/geometry, and
  /// only then publishes the unchanged Java-facing runner intent.
  @visibleForTesting
  Future<bool> receiveNativePaletteDropCommit(Object? arguments) async {
    if (_closed ||
        !_paletteDropNegotiated ||
        !_interactionInputEnabledForCurrentLayout()) {
      return false;
    }
    try {
      final request = _NativeDropRequest.decode(
        arguments,
        r'$/nativePaletteDropCommit',
      );
      final prepared = _preparedNativeDrop;
      if (prepared == null || !prepared.matches(request)) {
        return false;
      }
      _preparedNativeDrop = null;
      final current = _model;
      if (current == null ||
          current.presentationSequence != prepared.presentationSequence ||
          current.documentId != prepared.documentId ||
          current.logicalRevisionId != prepared.logicalRevisionId ||
          _layoutSequence != prepared.layoutSequence ||
          _lastPresentedIdentity != _identity(current)) {
        return false;
      }
      final target = _resolveValidDropTarget(
        request.xMicros,
        request.yMicros,
        request.token,
      );
      if (target == null || !_sameSemanticDropTarget(target, prepared.target)) {
        return false;
      }
      final parent = _findNode(current.root, target.parentWidgetId);
      if (parent == null) {
        return false;
      }
      final body = _identityBody(current)
        ..addAll({
          'frameSequence': 0,
          'layoutSequence': _layoutSequence,
          'intentSequence': _intentSequence++,
          'token': request.token,
          'operation': 'ADD',
          'parentWidgetId': parent.id,
          'slotName': target.slotName,
          'insertionIndex': target.insertionIndex,
        });
      await _writeRuntime('runner.paletteDrop', body);
      _paletteDragSourceAuthority = null;
      return true;
    } on FormatException {
      return false;
    }
  }

  /// Cancels only the exact prepared physical Drop; stale cancellation cannot
  /// erase a newer drag generation.
  @visibleForTesting
  Future<bool> receiveNativePaletteDropCancel(Object? arguments) async {
    if (_closed || !_paletteDropNegotiated) {
      return false;
    }
    try {
      final request = _NativeDropRequest.decode(
        arguments,
        r'$/nativePaletteDropCancel',
      );
      final prepared = _preparedNativeDrop;
      if (prepared == null || !prepared.matches(request)) {
        return false;
      }
      _preparedNativeDrop = null;
      _paletteDragSourceAuthority = null;
      return true;
    } on FormatException {
      return false;
    }
  }

  CanvasDropTarget? _resolveValidDropTarget(
    int xMicros,
    int yMicros, [
    String? token,
  ]) {
    final current = _model;
    final resolver = _dropResolver;
    final authority = _paletteDragSourceAuthority;
    if (current == null ||
        resolver == null ||
        authority == null ||
        !_paletteDropSourceAwareNegotiated ||
        _lastPresentedIdentity != _identity(current) ||
        !authority.matches(current, _layoutSequence) ||
        (token != null && authority.source.token != token)) {
      return null;
    }
    final target = resolver(xMicros, yMicros, authority.source);
    if (target == null) {
      return null;
    }
    final parent = _findNode(current.root, target.parentWidgetId);
    if (parent == null) {
      return null;
    }
    final dropSlot = canvasDropSlotForWidgetSlot(parent.type, target.slotName);
    if (dropSlot == null) {
      return null;
    }
    final modelSlot = parent.slot(dropSlot.slotName);
    if (modelSlot != null && modelSlot.kind != dropSlot.modelSlotKind) {
      return null;
    }
    final currentChildCount = modelSlot?.children.length ?? 0;
    if (!dropSlot.accepts(
      currentChildCount: currentChildCount,
      insertionIndex: target.insertionIndex,
    )) {
      return null;
    }
    if (!dropSlot.acceptsSource(authority.source)) {
      return null;
    }
    return target;
  }

  void _setDropHoverTarget(CanvasDropTarget? target) {
    if (_sameDropTarget(_dropHoverTarget, target)) {
      return;
    }
    _dropHoverTarget = target;
    notifyListeners();
  }

  void _setWidgetMovePreviewTarget(CanvasDropTarget? target) {
    if (_sameDropTarget(_widgetMovePreviewTarget, target)) {
      return;
    }
    _widgetMovePreviewTarget = target;
    notifyListeners();
  }

  void _invalidateNativeHoverApproval() {
    if (!_ownsProcessIo || !_paletteDropNegotiated || _closed) {
      return;
    }
    unawaited(_sendNativeHoverInvalidation());
  }

  Future<void> _sendNativeHoverInvalidation() async {
    try {
      await _paletteDropChannel.invokeMethod<void>('invalidateHover');
    } on Object catch (error) {
      _diagnostic(
        'Flutter Canvas could not invalidate native hover approval: $error',
      );
    }
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
    } else if (type == 'host.interactionFence') {
      await _handleHostInteractionFence(_object(object['body'], r'$/body'));
    } else if (type == 'host.paletteDragSource') {
      _handleHostPaletteDragSource(_object(object['body'], r'$/body'));
    } else if (type == 'host.widgetMovePreview') {
      _handleHostWidgetMovePreview(_object(object['body'], r'$/body'));
    } else if (type == 'host.widgetMovePreviewClear') {
      _handleHostWidgetMovePreviewClear(_object(object['body'], r'$/body'));
    } else if (type == 'host.viewport') {
      _handleHostViewport(_object(object['body'], r'$/body'));
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
    _viewportPresentation = CanvasViewportPresentation.fit(next);
    _viewportMetrics = null;
    _lastViewportPublication = null;
    _selectedWidgetId = null;
    _layoutSequence = 0;
    _pendingLayoutSequence = null;
    _lastPresentedIdentity = null;
    _invalidateInteractionFenceApplication(notify: false);
    _preparedNativeDrop = null;
    _paletteDragSourceAuthority = null;
    _setDropHoverTarget(null);
    _widgetMovePreviewSequence = -1;
    _setWidgetMovePreviewTarget(null);
    _invalidateNativeHoverApproval();
    notifyListeners();
    _scheduleLayoutPublication(next, 0);
  }

  void _scheduleLayoutPublication(CanvasModel model, int layoutSequence) {
    final ticket = ++_layoutPublicationTicket;
    _pendingLayoutSequence = layoutSequence;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _completeLayoutPublication(model, layoutSequence, ticket);
    });
    WidgetsBinding.instance.scheduleFrame();
  }

  void _completeLayoutPublication(
    CanvasModel model,
    int layoutSequence,
    int ticket,
  ) {
    if (_closed ||
        ticket != _layoutPublicationTicket ||
        !identical(_model, model) ||
        _pendingLayoutSequence != layoutSequence) {
      return;
    }
    final Map<String, Object?> surfacePresentation;
    try {
      surfacePresentation = _surfacePresentationNegotiated
          ? _readSurfacePresentation()
          : const <String, Object?>{};
    } on Object catch (error) {
      // This callback may run while the protocol loop is blocked in read().
      // Let the host close the pipe after runner.failure rather than racing a
      // StreamIterator cancellation against that active read.
      unawaited(_failClosed(error, cancelReader: false));
      return;
    }
    final identity = _identity(model);
    _layoutSequence = layoutSequence;
    _pendingLayoutSequence = null;
    _lastPresentedIdentity = identity;
    _invalidateNativeHoverApproval();
    unawaited(
      _writeRuntime(
        'runner.presented',
        _identityBody(model)..addAll({
          'frameSequence': 0,
          'layoutSequence': layoutSequence,
          ...surfacePresentation,
        }),
      ),
    );
  }

  Map<String, Object?> _readSurfacePresentation() {
    final view = WidgetsBinding.instance.platformDispatcher.implicitView;
    if (view == null) {
      throw const FormatException(
        'Canvas surface presentation requires one implicit FlutterView.',
      );
    }
    final physicalSize = view.physicalSize;
    final physicalWidth = _exactPhysicalDimension(
      physicalSize.width,
      'physicalWidth',
    );
    final physicalHeight = _exactPhysicalDimension(
      physicalSize.height,
      'physicalHeight',
    );
    if (physicalWidth * physicalHeight > _limits.maxPhysicalPixels) {
      throw const FormatException(
        'Canvas surface presentation exceeds the negotiated pixel bound.',
      );
    }
    final scaledDevicePixelRatio = view.devicePixelRatio * 1000000;
    if (!scaledDevicePixelRatio.isFinite) {
      throw const FormatException(
        'Canvas surface device-pixel ratio is not finite.',
      );
    }
    final devicePixelRatioMicros = scaledDevicePixelRatio.round();
    if (devicePixelRatioMicros <= 0 ||
        devicePixelRatioMicros > _maximumDevicePixelRatioMicros) {
      throw const FormatException(
        'Canvas surface device-pixel ratio is outside the protocol bound.',
      );
    }
    return {
      'physicalWidth': physicalWidth,
      'physicalHeight': physicalHeight,
      'devicePixelRatioMicros': devicePixelRatioMicros,
    };
  }

  int _exactPhysicalDimension(double value, String name) {
    if (!value.isFinite ||
        value <= 0 ||
        value != value.roundToDouble() ||
        value > _limits.maxPhysicalDimension) {
      throw FormatException(
        'Canvas surface $name is outside the negotiated physical bound.',
      );
    }
    return value.toInt();
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
        _lastPresentedIdentity != _identity(current) ||
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
        _sequence(body['layoutSequence'], r'$/body/layoutSequence') !=
            _layoutSequence) {
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

  Future<void> _handleHostInteractionFence(Map<String, Object?> body) async {
    _exactKeys(body, r'$/body', const {
      'presentationSequence',
      'documentId',
      'logicalRevisionId',
      'frameSequence',
      'layoutSequence',
      'interactionFenceSequence',
    });
    final next = _sequence(
      body['interactionFenceSequence'],
      r'$/body/interactionFenceSequence',
    );
    final current = _model;
    if (current == null || !_matchesCurrentLayout(body, current)) {
      return;
    }
    if (next < _interactionFenceSequence) {
      return;
    }
    final identity = _identity(current);
    final layoutSequence = _layoutSequence;
    final applicationTicket = ++_interactionFenceApplicationTicket;
    _interactionFenceSequence = next;
    _interactionInputSynchronized = false;
    _appliedInteractionFenceIdentity = null;
    _appliedInteractionFenceLayoutSequence = null;
    _preparedNativeDrop = null;
    _paletteDragSourceAuthority = null;
    _setDropHoverTarget(null);
    _invalidateNativeHoverApproval();
    notifyListeners();
    await _writeRuntime(
      'runner.interactionFenceApplied',
      _identityBody(current)..addAll({
        'frameSequence': 0,
        'layoutSequence': layoutSequence,
        'interactionFenceSequence': next,
      }),
    );
    if (_closed ||
        applicationTicket != _interactionFenceApplicationTicket ||
        !identical(current, _model) ||
        _lastPresentedIdentity != identity ||
        _layoutSequence != layoutSequence ||
        _interactionFenceSequence != next) {
      return;
    }
    _interactionInputSynchronized = true;
    _appliedInteractionFenceIdentity = identity;
    _appliedInteractionFenceLayoutSequence = layoutSequence;
    notifyListeners();
  }

  void _invalidateInteractionFenceApplication({bool notify = true}) {
    _interactionFenceApplicationTicket++;
    final changed =
        _interactionInputSynchronized ||
        _appliedInteractionFenceIdentity != null ||
        _appliedInteractionFenceLayoutSequence != null;
    _interactionInputSynchronized = false;
    _appliedInteractionFenceIdentity = null;
    _appliedInteractionFenceLayoutSequence = null;
    if (notify && changed) {
      notifyListeners();
    }
  }

  void _handleHostPaletteDragSource(Map<String, Object?> body) {
    if (!_paletteDropSourceAwareNegotiated) {
      throw const FormatException(
        'Canvas source-aware Palette drop capability was not negotiated.',
      );
    }
    _exactKeys(body, r'$/body', const {
      'presentationSequence',
      'documentId',
      'logicalRevisionId',
      'frameSequence',
      'layoutSequence',
      'token',
      'widgetType',
      'traits',
    });
    final current = _model;
    if (current == null || !_matchesCurrentLayout(body, current)) {
      return;
    }
    final token = _boundedText(body['token'], r'$/body/token', 1, 160);
    if (!_paletteDropTokenPattern.hasMatch(token)) {
      throw const FormatException('Canvas Palette source token is invalid.');
    }
    final widgetType = _boundedText(
      body['widgetType'],
      r'$/body/widgetType',
      1,
      255,
    );
    if (!isCanvasReviewedWidgetType(widgetType)) {
      throw FormatException(
        'Canvas Palette source widget type is not reviewed: $widgetType',
      );
    }
    final rawTraits = body['traits'];
    if (rawTraits is! List<Object?> || rawTraits.length > 32) {
      throw const FormatException('Canvas Palette source traits are invalid.');
    }
    final traits = <String>{};
    final traitPattern = RegExp(r'^[A-Za-z][A-Za-z0-9_.-]{0,254}$');
    for (var index = 0; index < rawTraits.length; index++) {
      final trait = _boundedText(rawTraits[index], r'$/body/traits', 1, 255);
      if (!traitPattern.hasMatch(trait) || !traits.add(trait)) {
        throw FormatException(
          'Canvas Palette source trait is invalid at index $index.',
        );
      }
    }
    if (!setEquals(traits, canvasWidgetTraitsForType(widgetType))) {
      throw const FormatException(
        'Canvas Palette source traits drift from the reviewed widget schema.',
      );
    }
    _preparedNativeDrop = null;
    _setDropHoverTarget(null);
    _paletteDragSourceAuthority = _PaletteDragSourceAuthority(
      source: CanvasPaletteDragSource(
        token: token,
        widgetType: widgetType,
        traits: traits,
      ),
      presentationSequence: current.presentationSequence,
      documentId: current.documentId,
      logicalRevisionId: current.logicalRevisionId,
      layoutSequence: _layoutSequence,
    );
  }

  void _handleHostWidgetMovePreview(Map<String, Object?> body) {
    if (!_widgetMovePreviewNegotiated) {
      throw const FormatException(
        'Canvas widget move preview capability was not negotiated.',
      );
    }
    _exactKeys(body, r'$/body', const {
      'presentationSequence',
      'documentId',
      'logicalRevisionId',
      'frameSequence',
      'layoutSequence',
      'previewSequence',
      'sourceWidgetId',
      'parentWidgetId',
      'slotName',
      'insertionIndex',
    });
    final previewSequence = _positiveSequence(
      body['previewSequence'],
      r'$/body/previewSequence',
    );
    final sourceWidgetId = _stableId(
      body['sourceWidgetId'],
      r'$/body/sourceWidgetId',
    );
    final parentWidgetId = _stableId(
      body['parentWidgetId'],
      r'$/body/parentWidgetId',
    );
    final slotName = _slotName(body['slotName'], r'$/body/slotName');
    final insertionIndex = _insertionIndex(
      body['insertionIndex'],
      r'$/body/insertionIndex',
    );
    final current = _model;
    if (current == null || !_matchesCurrentLayout(body, current)) {
      return;
    }
    if (previewSequence <= _widgetMovePreviewSequence) {
      return;
    }
    _widgetMovePreviewSequence = previewSequence;
    final source = _findNode(current.root, sourceWidgetId);
    final parent = _findNode(current.root, parentWidgetId);
    final resolver = _movePreviewResolver;
    if (source == null ||
        parent == null ||
        source.id == current.root.id ||
        source.id == parent.id ||
        _findNode(source, parent.id) != null ||
        resolver == null) {
      _setWidgetMovePreviewTarget(null);
      return;
    }
    final target = resolver(
      sourceWidgetId,
      parentWidgetId,
      slotName,
      insertionIndex,
    );
    if (target == null ||
        target.parentWidgetId != parentWidgetId ||
        target.slotName != slotName ||
        target.insertionIndex != insertionIndex ||
        target.zone == null ||
        target.zone!.isEmpty) {
      _setWidgetMovePreviewTarget(null);
      return;
    }
    _setWidgetMovePreviewTarget(target);
  }

  void _handleHostWidgetMovePreviewClear(Map<String, Object?> body) {
    if (!_widgetMovePreviewNegotiated) {
      throw const FormatException(
        'Canvas widget move preview capability was not negotiated.',
      );
    }
    _exactKeys(body, r'$/body', const {
      'presentationSequence',
      'documentId',
      'logicalRevisionId',
      'frameSequence',
      'layoutSequence',
      'previewSequence',
    });
    final previewSequence = _positiveSequence(
      body['previewSequence'],
      r'$/body/previewSequence',
    );
    final current = _model;
    if (current == null ||
        !_matchesCurrentLayout(body, current) ||
        previewSequence <= _widgetMovePreviewSequence) {
      return;
    }
    _widgetMovePreviewSequence = previewSequence;
    _setWidgetMovePreviewTarget(null);
  }

  bool _matchesCurrentLayout(Map<String, Object?> body, CanvasModel current) =>
      _lastPresentedIdentity == _identity(current) &&
      _sequence(body['presentationSequence'], r'$/body/presentationSequence') ==
          current.presentationSequence &&
      _stableId(body['documentId'], r'$/body/documentId') ==
          current.documentId &&
      _sequence(body['logicalRevisionId'], r'$/body/logicalRevisionId') ==
          current.logicalRevisionId &&
      _sequence(body['frameSequence'], r'$/body/frameSequence') == 0 &&
      _sequence(body['layoutSequence'], r'$/body/layoutSequence') ==
          _layoutSequence;

  void _handleHostViewport(Map<String, Object?> body) {
    if (!_viewportPresentationNegotiated) {
      throw const FormatException(
        'Canvas viewport presentation capability was not negotiated.',
      );
    }
    _exactKeys(body, r'$/body', const {
      'commandSequence',
      'presentationSequence',
      'documentId',
      'logicalRevisionId',
      'mode',
      'zoomMicros',
      'horizontalScrollMicros',
      'verticalScrollMicros',
    });
    final next = CanvasViewportPresentation(
      commandSequence: _positiveSequence(
        body['commandSequence'],
        r'$/body/commandSequence',
      ),
      presentationSequence: _sequence(
        body['presentationSequence'],
        r'$/body/presentationSequence',
      ),
      documentId: _stableId(body['documentId'], r'$/body/documentId'),
      logicalRevisionId: _sequence(
        body['logicalRevisionId'],
        r'$/body/logicalRevisionId',
      ),
      mode: _viewportMode(body['mode'], r'$/body/mode'),
      zoomMicros: _boundedMicros(
        body['zoomMicros'],
        r'$/body/zoomMicros',
        minimumCanvasZoomMicros,
        maximumCanvasZoomMicros,
      ),
      horizontalScrollMicros: _boundedMicros(
        body['horizontalScrollMicros'],
        r'$/body/horizontalScrollMicros',
        0,
        canvasViewportMicros,
      ),
      verticalScrollMicros: _boundedMicros(
        body['verticalScrollMicros'],
        r'$/body/verticalScrollMicros',
        0,
        canvasViewportMicros,
      ),
    );
    final current = _model;
    if (current == null || !next.matchesModel(current)) {
      return;
    }
    final previous = _viewportPresentation;
    if (previous != null) {
      if (next.commandSequence < previous.commandSequence) {
        return;
      }
      if (next.commandSequence == previous.commandSequence &&
          next != previous) {
        throw const FormatException(
          'Canvas viewport command sequence was reused with another state.',
        );
      }
    }
    _applyViewportPresentation(next);
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
    _lastPresentedIdentity = null;
    _invalidateInteractionFenceApplication(notify: false);
    _pendingLayoutSequence = null;
    _viewportMetrics = null;
    _lastViewportPublication = null;
    _layoutPublicationTicket++;
    _preparedNativeDrop = null;
    _paletteDragSourceAuthority = null;
    _setDropHoverTarget(null);
    _setWidgetMovePreviewTarget(null);
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

  Future<void> _failClosed(Object error, {bool cancelReader = true}) async {
    if (_closed) {
      return;
    }
    _closed = true;
    _lastPresentedIdentity = null;
    _invalidateInteractionFenceApplication(notify: false);
    _pendingLayoutSequence = null;
    _viewportMetrics = null;
    _lastViewportPublication = null;
    _layoutPublicationTicket++;
    _preparedNativeDrop = null;
    _paletteDragSourceAuthority = null;
    _setDropHoverTarget(null);
    _setWidgetMovePreviewTarget(null);
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
    if (cancelReader) {
      await _reader.cancel();
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
  if (rawCapabilities is! List<Object?> || rawCapabilities.length > 10) {
    throw const FormatException('Canvas requested capabilities are invalid.');
  }
  const supported = {
    'readOnly.render',
    'readOnly.layout',
    'readOnly.selection',
    _paletteDropCapability,
    _paletteDropSourceAwareCapability,
    _deleteSelectedWidgetCapability,
    _inlineTextEditCapability,
    _widgetMovePreviewCapability,
    _viewportPresentationCapability,
    _surfacePresentationCapability,
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
  if (capabilities.contains(_paletteDropSourceAwareCapability) &&
      !capabilities.contains(_paletteDropCapability)) {
    throw const FormatException(
      'Canvas source-aware Palette drop requires catalog insertion DnD.',
    );
  }
  return _HostHello(
    sessionId,
    Set.unmodifiable(capabilities),
    _HandshakeLimits.decode(body['offeredLimits']),
  );
}

Map<String, Object?> _runnerHello(
  _HostHello hello, {
  required bool paletteDropAvailable,
}) {
  const capabilityOrder = [
    'readOnly.render',
    'readOnly.layout',
    'readOnly.selection',
    _paletteDropCapability,
    _paletteDropSourceAwareCapability,
    _deleteSelectedWidgetCapability,
    _inlineTextEditCapability,
    _widgetMovePreviewCapability,
    _viewportPresentationCapability,
    _surfacePresentationCapability,
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
          if (hello.capabilities.contains(capability) &&
              ((capability != _paletteDropCapability &&
                      capability != _paletteDropSourceAwareCapability) ||
                  paletteDropAvailable))
            capability,
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
  if (value is! Map) {
    throw FormatException('Canvas value must be an object: $path');
  }
  if (value.length > 64) {
    throw FormatException('Canvas object has too many fields: $path');
  }
  final result = <String, Object?>{};
  for (final entry in value.entries) {
    final key = entry.key;
    if (key is! String) {
      throw FormatException('Canvas object key must be a string: $path');
    }
    result[key] = entry.value;
  }
  return result;
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

int _positiveSequence(Object? value, String path) {
  final sequence = _sequence(value, path);
  if (sequence == 0) {
    throw FormatException('Canvas sequence must be positive: $path');
  }
  return sequence;
}

int _surfaceMicros(Object? value, String path) {
  if (value is! int || value < 0 || value > 1000000) {
    throw FormatException(
      'Canvas surface coordinate is outside its normalized range: $path',
    );
  }
  return value;
}

int _boundedMicros(Object? value, String path, int minimum, int maximum) {
  if (value is! int || value < minimum || value > maximum) {
    throw FormatException('Canvas viewport value is outside its range: $path');
  }
  return value;
}

String _slotName(Object? value, String path) {
  final text = _boundedText(value, path, 1, 64);
  if (!RegExp(r'^[A-Za-z_][A-Za-z0-9_]*$').hasMatch(text)) {
    throw FormatException('Canvas slot name is invalid: $path');
  }
  return text;
}

int _insertionIndex(Object? value, String path) {
  if (value is! int || value < 0 || value > 10000) {
    throw FormatException('Canvas insertion index is outside its range: $path');
  }
  return value;
}

String _viewportMode(Object? value, String path) {
  if (value != 'fit' && value != 'manual') {
    throw FormatException('Canvas viewport mode is invalid: $path');
  }
  return value! as String;
}

Map<String, Object?>? _tryNativeObject(
  Object? value,
  String path,
  Set<String> expected,
) {
  try {
    final object = _object(value, path);
    _exactKeys(object, path, expected);
    return object;
  } on FormatException {
    return null;
  }
}

int? _tryNativeGeneration(Object? value, String path) {
  try {
    final object = _object(value, r'$/nativePaletteDrop');
    return _sequence(object['generation'], path);
  } on FormatException {
    return null;
  }
}

class _NativeDropRequest {
  const _NativeDropRequest({
    required this.token,
    required this.xMicros,
    required this.yMicros,
    required this.generation,
    required this.probeId,
  });

  final String token;
  final int xMicros;
  final int yMicros;
  final int generation;
  final int probeId;

  static _NativeDropRequest decode(Object? value, String path) {
    final object = _object(value, path);
    _exactKeys(object, path, const {
      'token',
      'xMicros',
      'yMicros',
      'generation',
      'probeId',
    });
    final token = _boundedText(object['token'], '$path/token', 1, 160);
    if (!_paletteDropTokenPattern.hasMatch(token)) {
      throw FormatException('Native palette token is invalid: $path/token');
    }
    return _NativeDropRequest(
      token: token,
      xMicros: _surfaceMicros(object['xMicros'], '$path/xMicros'),
      yMicros: _surfaceMicros(object['yMicros'], '$path/yMicros'),
      generation: _sequence(object['generation'], '$path/generation'),
      probeId: _sequence(object['probeId'], '$path/probeId'),
    );
  }
}

class _PaletteDragSourceAuthority {
  const _PaletteDragSourceAuthority({
    required this.source,
    required this.presentationSequence,
    required this.documentId,
    required this.logicalRevisionId,
    required this.layoutSequence,
  });

  final CanvasPaletteDragSource source;
  final int presentationSequence;
  final String documentId;
  final int logicalRevisionId;
  final int layoutSequence;

  bool matches(CanvasModel model, int currentLayoutSequence) =>
      presentationSequence == model.presentationSequence &&
      documentId == model.documentId &&
      logicalRevisionId == model.logicalRevisionId &&
      layoutSequence == currentLayoutSequence;
}

class _PreparedNativeDrop {
  const _PreparedNativeDrop({
    required this.request,
    required this.target,
    required this.presentationSequence,
    required this.documentId,
    required this.logicalRevisionId,
    required this.layoutSequence,
  });

  final _NativeDropRequest request;
  final CanvasDropTarget target;
  final int presentationSequence;
  final String documentId;
  final int logicalRevisionId;
  final int layoutSequence;

  int get generation => request.generation;

  bool matches(_NativeDropRequest other) =>
      request.token == other.token &&
      request.xMicros == other.xMicros &&
      request.yMicros == other.yMicros &&
      request.generation == other.generation &&
      request.probeId == other.probeId;
}

bool _sameSemanticDropTarget(CanvasDropTarget left, CanvasDropTarget right) =>
    left.parentWidgetId == right.parentWidgetId &&
    left.slotName == right.slotName &&
    left.insertionIndex == right.insertionIndex;

bool _sameDropTarget(CanvasDropTarget? left, CanvasDropTarget? right) {
  if (identical(left, right)) {
    return true;
  }
  if (left == null || right == null || !_sameSemanticDropTarget(left, right)) {
    return false;
  }
  final leftZone = left.zone;
  final rightZone = right.zone;
  return leftZone == null && rightZone == null ||
      leftZone != null &&
          rightZone != null &&
          leftZone.leftMicros == rightZone.leftMicros &&
          leftZone.topMicros == rightZone.topMicros &&
          leftZone.rightMicros == rightZone.rightMicros &&
          leftZone.bottomMicros == rightZone.bottomMicros;
}

({int utf16Units, int unicodeScalars})? _inlineTextMetrics(String value) {
  var scalars = 0;
  for (var index = 0; index < value.length; index++) {
    final unit = value.codeUnitAt(index);
    if (unit >= 0xd800 && unit <= 0xdbff) {
      if (index + 1 >= value.length) {
        return null;
      }
      final low = value.codeUnitAt(index + 1);
      if (low < 0xdc00 || low > 0xdfff) {
        return null;
      }
      index++;
    } else if (unit >= 0xdc00 && unit <= 0xdfff) {
      return null;
    }
    scalars++;
    if (scalars > maximumInlineTextUnicodeScalars) {
      return (utf16Units: value.length, unicodeScalars: scalars);
    }
  }
  return (utf16Units: value.length, unicodeScalars: scalars);
}

CanvasNode? _findNode(CanvasNode node, String id) {
  if (node.id == id) {
    return node;
  }
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _findNode(child, id);
      if (found != null) {
        return found;
      }
    }
  }
  return null;
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
