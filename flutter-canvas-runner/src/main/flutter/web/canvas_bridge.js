(function installNetBeansCanvasBridge(global) {
  'use strict';

  const format = 'netbeans-flutter-canvas-web-bridge';
  const version = 1;
  const noncePattern = /^[0-9a-f]{64}$/;
  const base64Pattern = /^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/;
  const base64Alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  const maximumDecodedChunkBytes = 1048576;
  const maximumEncodedChunkCharacters = 1398104;
  const maximumPendingChunks = 64;
  const bootstrap = global.__netBeansCanvasBootstrap;
  const webview = global.chrome && global.chrome.webview;
  const sessionNonce = bootstrap && bootstrap.sessionNonce;
  const available = Boolean(
    webview && noncePattern.test(sessionNonce || ''),
  );
  let receiver = null;
  let terminalReceiver = null;
  let closed = false;
  let view = null;
  let lastHostSequence = 0;
  const pending = [];

  function belongsToSession(message) {
    return message && typeof message === 'object' &&
      message.sessionNonce === sessionNonce;
  }

  function validEnvelope(message, direction) {
    return message &&
      message.format === format &&
      message.version === version &&
      message.sessionNonce === sessionNonce &&
      message.direction === direction;
  }

  function boundedCanonicalBase64(chunk) {
    if (typeof chunk !== 'string' || chunk.length < 4 ||
        chunk.length > maximumEncodedChunkCharacters ||
        chunk.length % 4 !== 0 || !base64Pattern.test(chunk)) {
      return false;
    }
    let padding = 0;
    if (chunk.endsWith('==')) {
      padding = 2;
      if (base64Alphabet.indexOf(chunk.charAt(chunk.length - 3)) % 16 !== 0) {
        return false;
      }
    } else if (chunk.endsWith('=')) {
      padding = 1;
      if (base64Alphabet.indexOf(chunk.charAt(chunk.length - 2)) % 4 !== 0) {
        return false;
      }
    }
    const decodedLength = (chunk.length / 4) * 3 - padding;
    return decodedLength > 0 && decodedLength <= maximumDecodedChunkBytes;
  }

  function terminateAuthenticatedSession(reason) {
    if (closed) {
      return;
    }
    closed = true;
    pending.length = 0;
    receiver = null;
    const callback = terminalReceiver;
    terminalReceiver = null;
    if (available) {
      webview.removeEventListener('message', acceptHostMessage);
      try {
        webview.postMessage({
          format,
          version,
          sessionNonce,
          direction: 'runner-to-host',
          kind: 'failure',
          sequence: lastHostSequence,
          message: reason.slice(0, 2048),
        });
      } finally {
        if (callback) {
          callback(sessionNonce, reason);
        }
      }
    } else if (callback) {
      callback(sessionNonce, reason);
    }
  }

  function acceptHostMessage(event) {
    const message = event.data;
    if (closed || !belongsToSession(message)) {
      return;
    }
    if (!validEnvelope(message, 'host-to-runner') ||
        message.kind !== 'chunk' ||
        !Number.isSafeInteger(message.sequence) ||
        message.sequence !== lastHostSequence + 1 ||
        !boundedCanonicalBase64(message.chunk)) {
      terminateAuthenticatedSession(
        'Authenticated host message violated the Canvas bridge contract.',
      );
      return;
    }
    lastHostSequence = message.sequence;
    const delivery = [sessionNonce, message.sequence, message.chunk];
    if (receiver) {
      try {
        receiver(...delivery);
      } catch (_) {
        terminateAuthenticatedSession(
          'The Canvas runner rejected an authenticated host message.',
        );
      }
    } else if (pending.length < maximumPendingChunks) {
      pending.push(delivery);
    } else {
      terminateAuthenticatedSession(
        'Authenticated host messages exceeded the pending Canvas bridge bound.',
      );
    }
  }

  if (available) {
    webview.addEventListener('message', acceptHostMessage);
  }

  const bridge = {
    available,
    sessionNonce: available ? sessionNonce : '',
    registerRunner(callback, onTerminal) {
      if (!available || closed || receiver || terminalReceiver ||
          typeof callback !== 'function' || typeof onTerminal !== 'function') {
        throw new Error('Flutter Web Canvas runner registration was rejected.');
      }
      receiver = callback;
      terminalReceiver = onTerminal;
      while (!closed && pending.length) {
        try {
          receiver(...pending.shift());
        } catch (_) {
          terminateAuthenticatedSession(
            'The Canvas runner rejected a pending authenticated host message.',
          );
        }
      }
      if (closed) {
        return;
      }
      try {
        webview.postMessage({
          format,
          version,
          sessionNonce,
          direction: 'runner-to-host',
          kind: 'ready',
          sequence: 0,
        });
      } catch (_) {
        terminateAuthenticatedSession(
          'The Canvas bridge could not notify the authenticated host.',
        );
      }
    },
    postRunnerChunk(nonce, sequence, chunk) {
      if (!available || closed || nonce !== sessionNonce ||
          !Number.isSafeInteger(sequence) || sequence < 1 ||
          typeof chunk !== 'string' || chunk.length < 4 ||
          chunk.length > maximumEncodedChunkCharacters ||
          chunk.length % 4 !== 0 || !base64Pattern.test(chunk)) {
        throw new Error('Flutter Web Canvas runner chunk was rejected.');
      }
      webview.postMessage({
        format,
        version,
        sessionNonce,
        direction: 'runner-to-host',
        kind: 'chunk',
        sequence,
        chunk,
      });
    },
    postRunnerDiagnostic(nonce, message) {
      if (!available || closed || nonce !== sessionNonce ||
          typeof message !== 'string') {
        return;
      }
      webview.postMessage({
        format,
        version,
        sessionNonce,
        direction: 'runner-to-host',
        kind: 'diagnostic',
        sequence: 0,
        message: message.slice(0, 2048),
      });
    },
    closeRunner(nonce, lastRunnerSequence) {
      if (!available || closed || nonce !== sessionNonce) {
        return;
      }
      closed = true;
      pending.length = 0;
      receiver = null;
      terminalReceiver = null;
      webview.removeEventListener('message', acceptHostMessage);
      webview.postMessage({
        format,
        version,
        sessionNonce,
        direction: 'runner-to-host',
        kind: 'closed',
        sequence: Number.isSafeInteger(lastRunnerSequence)
          ? lastRunnerSequence : 0,
      });
    },
    bindView(app, viewId) {
      if (view) {
        throw new Error('Flutter Web Canvas view is already bound.');
      }
      view = Object.freeze({app, viewId});
    },
    removeView() {
      if (!view) {
        return;
      }
      view.app.removeView(view.viewId);
      view = null;
    },
  };

  Object.defineProperty(global, 'netBeansCanvasBridge', {
    value: Object.freeze(bridge),
    configurable: false,
    enumerable: false,
    writable: false,
  });
})(globalThis);
