import 'dart:js_interop';

import 'canvas_web_transport_core.dart';

@JS('netBeansCanvasBridge')
external _JavaScriptCanvasBridge? get _javaScriptCanvasBridge;

extension type _JavaScriptCanvasBridge(JSObject _) implements JSObject {
  external JSBoolean get available;
  external JSString get sessionNonce;
  external void registerRunner(
    JSFunction receiver,
    JSFunction terminalReceiver,
  );
  external void postRunnerChunk(
    JSString sessionNonce,
    JSNumber sequence,
    JSString encodedChunk,
  );
  external void postRunnerDiagnostic(JSString sessionNonce, JSString message);
  external void closeRunner(JSString sessionNonce, JSNumber lastRunnerSequence);
}

final class JavaScriptCanvasWebBridgeAdapter implements CanvasWebBridgeAdapter {
  JavaScriptCanvasWebBridgeAdapter._(this._bridge);

  factory JavaScriptCanvasWebBridgeAdapter.connect() {
    final bridge = _javaScriptCanvasBridge;
    if (bridge == null || !bridge.available.toDart) {
      throw StateError(
        'The authenticated WebView2 Canvas bridge is unavailable.',
      );
    }
    return JavaScriptCanvasWebBridgeAdapter._(bridge);
  }

  final _JavaScriptCanvasBridge _bridge;
  JSExportedDartFunction? _receiver;
  JSExportedDartFunction? _terminalReceiver;

  @override
  String get sessionNonce => _bridge.sessionNonce.toDart;

  @override
  void registerRunner(
    void Function(String sessionNonce, int sequence, String encodedChunk)
    receiver,
    void Function(String sessionNonce, String reason) terminalReceiver,
  ) {
    if (_receiver != null || _terminalReceiver != null) {
      throw StateError('The Flutter Web Canvas runner is already registered.');
    }
    final callback =
        (JSString nonce, JSNumber sequence, JSString encodedChunk) {
          receiver(nonce.toDart, sequence.toDartInt, encodedChunk.toDart);
        }.toJS;
    final terminalCallback = (JSString nonce, JSString reason) {
      terminalReceiver(nonce.toDart, reason.toDart);
    }.toJS;
    _receiver = callback;
    _terminalReceiver = terminalCallback;
    _bridge.registerRunner(callback, terminalCallback);
  }

  @override
  void postRunnerChunk(
    String sessionNonce,
    int sequence,
    String encodedChunk,
  ) => _bridge.postRunnerChunk(
    sessionNonce.toJS,
    sequence.toJS,
    encodedChunk.toJS,
  );

  @override
  void postRunnerDiagnostic(String sessionNonce, String message) =>
      _bridge.postRunnerDiagnostic(sessionNonce.toJS, message.toJS);

  @override
  void closeRunner(String sessionNonce, int lastRunnerSequence) =>
      _bridge.closeRunner(sessionNonce.toJS, lastRunnerSequence.toJS);
}
