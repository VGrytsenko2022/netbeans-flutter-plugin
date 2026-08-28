import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/widgets.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
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
      [
        'readOnly.render',
        'readOnly.layout',
        'readOnly.selection',
        'palette.drop.textAppend.v1',
      ],
    );
    expect(closedJson['type'], 'runner.closed');
    expect(closedJson['replyTo'], 1);
    expect(await reader.read(maxPayloadBytes: 262144), isNull);
  });

  test(
    'omits optional palette DnD when the native target is unavailable',
    () async {
      final input = StreamController<List<int>>();
      final output = <List<int>>[];
      final diagnostics = <String>[];
      final runtime = CanvasRuntimeController(
        input: input.stream,
        output: (bytes) => output.add(List<int>.from(bytes)),
        flush: () async {},
        diagnostic: diagnostics.add,
        nativeDropAvailabilityProbe: () async => false,
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
      final helloJson =
          jsonDecode(utf8.decode(hello!.payload)) as Map<String, Object?>;
      expect(
        (helloJson['body'] as Map<String, Object?>)['acceptedCapabilities'],
        ['readOnly.render', 'readOnly.layout', 'readOnly.selection'],
      );
    },
  );

  test(
    'native availability probe failure preserves read-only Canvas',
    () async {
      final input = StreamController<List<int>>();
      final output = <List<int>>[];
      final diagnostics = <String>[];
      final runtime = CanvasRuntimeController(
        input: input.stream,
        output: (bytes) => output.add(List<int>.from(bytes)),
        flush: () async {},
        diagnostic: diagnostics.add,
        nativeDropAvailabilityProbe: () async {
          throw MissingPluginException('native DnD channel unavailable');
        },
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

      expect(runtime.closed, isTrue);
      expect(diagnostics, hasLength(1));
      expect(diagnostics.single, contains('continuing read-only'));
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
      expect(
        (helloJson['body'] as Map<String, Object?>)['acceptedCapabilities'],
        ['readOnly.render', 'readOnly.layout', 'readOnly.selection'],
      );
      expect(closedJson['type'], 'runner.closed');
    },
  );

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

  testWidgets(
    'binds palette drops to the exact post-layout Canvas geometry',
    (tester) async {
      final input = StreamController<List<int>>();
      final output = <List<int>>[];
      final runtime = CanvasRuntimeController(
        input: input.stream,
        output: (bytes) => output.add(List<int>.from(bytes)),
        flush: () async {},
        diagnostic: fail,
      );

      final running = runtime.start();
      input.add(
        encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_hello()))),
      );
      _addRender(input, fixture.modelBytesForViewTest());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (var attempt = 0; attempt < 20 && runtime.model == null; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      expect(runtime.model, isNotNull);
      await tester.pump();
      runtime.setDropResolver(
        (_, _) => const CanvasDropTarget(
          parentWidgetId: '1035b7df-df9b-442b-9af2-72b4c90f1462',
          slotName: 'children',
          insertionIndex: 1,
        ),
      );

      const firstToken =
          'nbfdnd:v1:ae91be74-fdf2-47a4-b949-51ea6dd4183b:dc090b63-23fe-4fd6-8324-bdd1ff4197ca';
      expect(
        await runtime.receiveNativePaletteHover({
          'token': firstToken,
          'xMicros': 500000,
          'yMicros': 500000,
          'generation': 1,
          'probeId': 1,
        }),
        isTrue,
      );
      expect(runtime.dropHoverTarget, isNotNull);
      final firstRequest = {
        'token': firstToken,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 1,
        'probeId': 1,
      };
      expect(
        await runtime.receiveNativePaletteDropPrepare(firstRequest),
        isTrue,
      );
      final firstDrop = runtime.receiveNativePaletteDropCommit(firstRequest);
      await tester.pump();
      expect(await firstDrop, isTrue);

      runtime.didChangeMetrics();
      expect(
        await runtime.receiveNativePaletteDropPrepare({
          'token':
              'nbfdnd:v1:3df690fd-9bc7-48a3-a929-b63fd7ce5194:b127e5ac-97e2-4c73-b924-99138bf20b28',
          'xMicros': 500000,
          'yMicros': 500000,
          'generation': 1,
          'probeId': 1,
        }),
        isFalse,
      );
      for (
        var attempt = 0;
        attempt < 10 && runtime.presentedLayoutSequence != 1;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      expect(runtime.presentedLayoutSequence, 1);

      // StandardMessageCodec returns native EncodableMap arguments with
      // Object?-typed keys rather than a reified Map<String, Object?>.
      const resizedToken =
          'nbfdnd:v1:993c476d-e430-4f53-b542-d1894b9fdfb0:2a38902a-c1d9-4d80-b87d-fec1b437be1a';
      expect(
        await runtime.receiveNativePaletteHover(<Object?, Object?>{
          'token': resizedToken,
          'xMicros': 500000,
          'yMicros': 500000,
          'generation': 2,
          'probeId': 2,
        }),
        isTrue,
      );
      final resizedRequest = <Object?, Object?>{
        'token': resizedToken,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 2,
        'probeId': 2,
      };
      expect(
        await runtime.receiveNativePaletteDropPrepare(resizedRequest),
        isTrue,
      );
      final resizedDrop = runtime.receiveNativePaletteDropCommit(
        resizedRequest,
      );
      await tester.pump();
      expect(await resizedDrop, isTrue);

      final closing = input.close();
      for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      expect(runtime.closed, isTrue);
      await closing;
      await running;
      await tester.pumpWidget(const SizedBox.shrink());

      final reader = NbfcFrameReader(Stream<List<int>>.fromIterable(output));
      final messages = <Map<String, Object?>>[];
      while (true) {
        final frame = await reader.read(maxPayloadBytes: 262144);
        if (frame == null) {
          break;
        }
        messages.add(
          jsonDecode(utf8.decode(frame.payload)) as Map<String, Object?>,
        );
      }
      final presented = messages
          .where((message) => message['type'] == 'runner.presented')
          .toList();
      expect(
        presented
            .map(
              (message) =>
                  (message['body'] as Map<String, Object?>)['layoutSequence'],
            )
            .toList(),
        [0, 1],
      );
      final drops = messages
          .where((message) => message['type'] == 'runner.paletteDrop')
          .toList();
      expect(drops, hasLength(2));
      expect(drops.first['sessionId'], '80ef60ed-b108-4674-99a6-c1f3102f01ab');
      expect(drops.first['body'], {
        'presentationSequence': 4,
        'documentId': 'd2d37c77-8510-4bd0-9280-a72e5bc3871e',
        'logicalRevisionId': 2,
        'frameSequence': 0,
        'layoutSequence': 0,
        'intentSequence': 0,
        'token':
            'nbfdnd:v1:ae91be74-fdf2-47a4-b949-51ea6dd4183b:dc090b63-23fe-4fd6-8324-bdd1ff4197ca',
        'operation': 'ADD',
        'parentWidgetId': '1035b7df-df9b-442b-9af2-72b4c90f1462',
        'slotName': 'children',
        'insertionIndex': 1,
      });
      expect((drops.last['body'] as Map<String, Object?>)['layoutSequence'], 1);
      expect(
        (drops.last['body'] as Map<String, Object?>)['token'],
        'nbfdnd:v1:993c476d-e430-4f53-b542-d1894b9fdfb0:2a38902a-c1d9-4d80-b87d-fec1b437be1a',
      );
    },
    timeout: const Timeout(Duration(seconds: 15)),
  );

  testWidgets(
    'hover probes fail closed across update, stale, invalid and leave events',
    (tester) async {
      final input = StreamController<List<int>>();
      final runtime = CanvasRuntimeController(
        input: input.stream,
        output: (_) {},
        flush: () async {},
        diagnostic: fail,
      );
      final running = runtime.start();
      input.add(
        encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_hello()))),
      );
      _addRender(input, fixture.modelBytesForViewTest());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (var attempt = 0; attempt < 20 && runtime.model == null; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await tester.pump();

      var resolveColumn = false;
      runtime.setDropResolver(
        (_, _) => CanvasDropTarget(
          parentWidgetId: resolveColumn
              ? '4c04dc44-7381-4ca8-826f-8c23bc2351d1'
              : '1035b7df-df9b-442b-9af2-72b4c90f1462',
          slotName: 'children',
          insertionIndex: resolveColumn ? 2 : 1,
          zone: const CanvasDropZone(
            leftMicros: 100000,
            topMicros: 200000,
            rightMicros: 300000,
            bottomMicros: 240000,
          ),
        ),
      );
      const token =
          'nbfdnd:v1:ae91be74-fdf2-47a4-b949-51ea6dd4183b:dc090b63-23fe-4fd6-8324-bdd1ff4197ca';

      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 1,
          'probeId': 1,
        }),
        isTrue,
      );
      expect(
        runtime.dropHoverTarget?.parentWidgetId,
        '1035b7df-df9b-442b-9af2-72b4c90f1462',
      );
      await tester.pump();
      expect(
        find.byKey(const ValueKey('canvas-text-append-drop-zone')),
        findsOneWidget,
      );

      resolveColumn = true;
      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 200000,
          'yMicros': 200000,
          'generation': 1,
          'probeId': 2,
        }),
        isTrue,
      );
      expect(
        runtime.dropHoverTarget?.parentWidgetId,
        '4c04dc44-7381-4ca8-826f-8c23bc2351d1',
      );

      resolveColumn = false;
      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 1,
          'probeId': 1,
        }),
        isFalse,
        reason: 'an older callback must not replace the newest hover',
      );
      expect(
        runtime.dropHoverTarget?.parentWidgetId,
        '4c04dc44-7381-4ca8-826f-8c23bc2351d1',
      );

      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 1000001,
          'yMicros': 200000,
          'generation': 1,
          'probeId': 3,
        }),
        isFalse,
      );
      expect(runtime.dropHoverTarget, isNull);
      await tester.pump();
      expect(
        find.byKey(const ValueKey('canvas-text-append-drop-zone')),
        findsNothing,
      );

      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 2,
          'probeId': 1,
        }),
        isTrue,
      );
      expect(
        await runtime.receiveNativePaletteHoverLeave({'generation': 1}),
        isFalse,
      );
      expect(runtime.dropHoverTarget, isNotNull);
      expect(
        await runtime.receiveNativePaletteHoverLeave({'generation': 2}),
        isTrue,
      );
      expect(runtime.dropHoverTarget, isNull);
      await tester.pump();
      expect(
        find.byKey(const ValueKey('canvas-text-append-drop-zone')),
        findsNothing,
      );
      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 2,
          'probeId': 2,
        }),
        isFalse,
        reason: 'a closed generation cannot be resurrected',
      );

      expect(
        await runtime.receiveNativePaletteHover({
          'token': 'not-a-designer-token',
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 3,
          'probeId': 1,
        }),
        isFalse,
      );
      expect(runtime.dropHoverTarget, isNull);

      resolveColumn = false;
      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 4,
          'probeId': 1,
        }),
        isTrue,
      );
      resolveColumn = true;
      expect(
        await runtime.receiveNativePaletteDropPrepare({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 4,
          'probeId': 1,
        }),
        isFalse,
        reason: 'Drop must re-resolve and match the approved semantic target',
      );
      expect(runtime.dropHoverTarget, isNull);

      resolveColumn = false;
      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 5,
          'probeId': 1,
        }),
        isTrue,
      );
      expect(
        await runtime.receiveNativePaletteDropPrepare({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 5,
          // probeId deliberately missing.
        }),
        isFalse,
      );
      expect(runtime.dropHoverTarget, isNull);
      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 5,
          'probeId': 2,
        }),
        isFalse,
        reason: 'malformed current-generation Drop must close its hover',
      );

      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 6,
          'probeId': 1,
        }),
        isTrue,
      );
      expect(
        await runtime.receiveNativePaletteDropPrepare({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 5,
          // stale and malformed; must not clear generation 6.
        }),
        isFalse,
      );
      expect(runtime.dropHoverTarget, isNotNull);
      expect(
        await runtime.receiveNativePaletteDropPrepare('malformed'),
        isFalse,
      );
      expect(runtime.dropHoverTarget, isNull);
      expect(
        await runtime.receiveNativePaletteHover({
          'token': token,
          'xMicros': 100000,
          'yMicros': 200000,
          'generation': 6,
          'probeId': 2,
        }),
        isTrue,
        reason: 'an unidentified malformed Drop cannot tombstone generation 6',
      );

      await input.close();
      for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await running;
      await tester.pumpWidget(const SizedBox.shrink());
    },
    timeout: const Timeout(Duration(seconds: 15)),
  );

  testWidgets(
    'fast release prepares after ordered hover before its reply is consumed',
    (tester) async {
      final input = StreamController<List<int>>();
      final output = <List<int>>[];
      final runtime = CanvasRuntimeController(
        input: input.stream,
        output: (bytes) => output.add(List<int>.from(bytes)),
        flush: () async {},
        diagnostic: fail,
      );
      final running = runtime.start();
      input.add(
        encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_hello()))),
      );
      _addRender(input, fixture.modelBytesForViewTest());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (var attempt = 0; attempt < 20 && runtime.model == null; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await tester.pump();
      runtime.setDropResolver(
        (_, _) => const CanvasDropTarget(
          parentWidgetId: '1035b7df-df9b-442b-9af2-72b4c90f1462',
          slotName: 'children',
          insertionIndex: 1,
        ),
      );
      const token =
          'nbfdnd:v1:ae91be74-fdf2-47a4-b949-51ea6dd4183b:dc090b63-23fe-4fd6-8324-bdd1ff4197ca';
      final request = {
        'token': token,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 1,
        'probeId': 1,
      };

      // Native sends the ordered hover without waiting for its reply, then a
      // fast physical release immediately invokes prepare for the same probe.
      final hoverFuture = runtime.receiveNativePaletteHover(request);
      final prepareFuture = runtime.receiveNativePaletteDropPrepare(request);
      expect(
        await prepareFuture,
        isTrue,
        reason: 'the ordered hover must be applied before prepare is handled',
      );
      expect(
        await hoverFuture,
        isTrue,
        reason: 'the native side has not consumed this reply before prepare',
      );
      expect(await runtime.receiveNativePaletteDropCommit(request), isTrue);
      expect(
        await runtime.receiveNativePaletteDropCommit(request),
        isFalse,
        reason: 'the fast-release prepared intent is single-use',
      );

      await input.close();
      for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await running;
      await tester.pumpWidget(const SizedBox.shrink());

      final drops = (await _decodeControlMessages(
        output,
      )).where((message) => message['type'] == 'runner.paletteDrop').toList();
      expect(drops, hasLength(1));
      expect((drops.single['body'] as Map<String, Object?>)['token'], token);
    },
    timeout: const Timeout(Duration(seconds: 15)),
  );

  testWidgets(
    'prepares without output, cancels without output, and commits exactly once',
    (tester) async {
      final input = StreamController<List<int>>();
      final output = <List<int>>[];
      final runtime = CanvasRuntimeController(
        input: input.stream,
        output: (bytes) => output.add(List<int>.from(bytes)),
        flush: () async {},
        diagnostic: fail,
      );
      final running = runtime.start();
      input.add(
        encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_hello()))),
      );
      _addRender(input, fixture.modelBytesForViewTest());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (var attempt = 0; attempt < 20 && runtime.model == null; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await tester.pump();
      runtime.setDropResolver(
        (_, _) => const CanvasDropTarget(
          parentWidgetId: '1035b7df-df9b-442b-9af2-72b4c90f1462',
          slotName: 'children',
          insertionIndex: 1,
        ),
      );
      const token =
          'nbfdnd:v1:ae91be74-fdf2-47a4-b949-51ea6dd4183b:dc090b63-23fe-4fd6-8324-bdd1ff4197ca';
      final firstRequest = {
        'token': token,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 1,
        'probeId': 1,
      };
      expect(await runtime.receiveNativePaletteHover(firstRequest), isTrue);
      expect(
        await runtime.receiveNativePaletteDropPrepare(firstRequest),
        isTrue,
      );
      expect(
        (await _decodeControlMessages(
          output,
        )).where((message) => message['type'] == 'runner.paletteDrop'),
        isEmpty,
        reason: 'prepare must not publish a Java-facing mutation',
      );
      expect(
        await runtime.receiveNativePaletteDropCancel(firstRequest),
        isTrue,
      );
      expect(
        await runtime.receiveNativePaletteDropCommit(firstRequest),
        isFalse,
        reason: 'a cancelled prepared intent cannot commit later',
      );
      expect(
        (await _decodeControlMessages(
          output,
        )).where((message) => message['type'] == 'runner.paletteDrop'),
        isEmpty,
        reason: 'cancel must not publish a Java-facing mutation',
      );

      final committedRequest = {
        'token': token,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 2,
        'probeId': 2,
      };
      expect(await runtime.receiveNativePaletteHover(committedRequest), isTrue);
      expect(
        await runtime.receiveNativePaletteDropPrepare(committedRequest),
        isTrue,
      );
      expect(
        (await _decodeControlMessages(
          output,
        )).where((message) => message['type'] == 'runner.paletteDrop'),
        isEmpty,
      );
      expect(
        await runtime.receiveNativePaletteDropCommit(committedRequest),
        isTrue,
      );
      expect(
        await runtime.receiveNativePaletteDropCommit(committedRequest),
        isFalse,
        reason: 'commit consumes the prepared intent exactly once',
      );

      await input.close();
      for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await running;
      await tester.pumpWidget(const SizedBox.shrink());

      final drops = (await _decodeControlMessages(
        output,
      )).where((message) => message['type'] == 'runner.paletteDrop').toList();
      expect(drops, hasLength(1));
      expect((drops.single['body'] as Map<String, Object?>)['token'], token);
    },
    timeout: const Timeout(Duration(seconds: 15)),
  );

  test('rejects malformed, unsupported and non-terminal native drops', () async {
    final runtime = CanvasRuntimeController(
      input: const Stream<List<int>>.empty(),
      output: (_) {},
      flush: () async {},
      diagnostic: (_) {},
    );

    expect(
      await runtime.receiveNativePaletteDropPrepare({
        'token': 'not-a-designer-token',
        'xMicros': 0,
        'yMicros': 0,
      }),
      isFalse,
    );
    expect(
      await runtime.receiveNativePaletteDropPrepare({
        'token':
            'nbfdnd:v1:ae91be74-fdf2-47a4-b949-51ea6dd4183b:dc090b63-23fe-4fd6-8324-bdd1ff4197ca',
        'xMicros': -1,
        'yMicros': 0,
      }),
      isFalse,
    );
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

Future<List<Map<String, Object?>>> _decodeControlMessages(
  List<List<int>> output,
) async {
  final reader = NbfcFrameReader(
    Stream<List<int>>.fromIterable(
      output.map((bytes) => List<int>.from(bytes)),
    ),
  );
  final messages = <Map<String, Object?>>[];
  while (true) {
    final frame = await reader.read(maxPayloadBytes: 262144);
    if (frame == null) {
      return messages;
    }
    if (frame.kind == nbfcControlJson) {
      messages.add(
        jsonDecode(utf8.decode(frame.payload)) as Map<String, Object?>,
      );
    }
  }
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
      'palette.drop.textAppend.v1',
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
