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
      encodeNbfcFrame(
        nbfcControlJson,
        utf8.encode(
          jsonEncode(
            _hello(
              viewport: true,
              widgetMovePreview: true,
              surfacePresentation: true,
            ),
          ),
        ),
      ),
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
        'palette.drop.catalogInsert.v1',
        'palette.drop.sourceAware.v1',
        'widget.deleteSelection.v1',
        'widget.movePreview.v1',
        'viewport.presentation.v1',
        'surface.presentation.v1',
      ],
    );
    expect(closedJson['type'], 'runner.closed');
    expect(closedJson['replyTo'], 1);
    expect(await reader.read(maxPayloadBytes: 262144), isNull);
  });

  test(
    'projects and clears an exact host-authorized widget move target',
    () async {
      final input = StreamController<List<int>>();
      final output = <List<int>>[];
      final diagnostics = <String>[];
      final runtime = CanvasRuntimeController(
        input: input.stream,
        output: (bytes) => output.add(List<int>.from(bytes)),
        flush: () async {},
        diagnostic: diagnostics.add,
      );
      const sourceId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
      const parentId = '1035b7df-df9b-442b-9af2-72b4c90f1462';
      const expected = CanvasDropTarget(
        parentWidgetId: parentId,
        slotName: 'children',
        insertionIndex: 1,
        zone: CanvasDropZone(
          leftMicros: 450000,
          topMicros: 200000,
          rightMicros: 470000,
          bottomMicros: 800000,
        ),
      );
      runtime.setMovePreviewResolver(
        (source, parent, slot, index) =>
            source == sourceId &&
                parent == parentId &&
                slot == 'children' &&
                index == 1
            ? expected
            : null,
      );

      final running = runtime.start();
      input.add(
        encodeNbfcFrame(
          nbfcControlJson,
          utf8.encode(jsonEncode(_hello(widgetMovePreview: true))),
        ),
      );
      final model = fixture.modelBytesForViewTest();
      _addRender(input, model);
      await _waitUntil(() => runtime.model != null);
      runtime.completePendingLayoutForTesting();
      expect(runtime.presentedLayoutSequence, 0);

      input.add(
        encodeNbfcFrame(
          nbfcControlJson,
          utf8.encode(
            jsonEncode(
              _widgetMovePreview(
                model,
                previewSequence: 1,
                sourceWidgetId: sourceId,
                parentWidgetId: parentId,
                slotName: 'children',
                insertionIndex: 1,
              ),
            ),
          ),
        ),
      );
      await _waitUntil(() => runtime.widgetMovePreviewTarget != null);
      final active = runtime.widgetMovePreviewTarget;
      expect(active, same(expected));
      expect(runtime.dropHoverTarget, same(active));

      input.add(
        encodeNbfcFrame(
          nbfcControlJson,
          utf8.encode(
            jsonEncode(_widgetMovePreviewClear(model, previewSequence: 1)),
          ),
        ),
      );
      await Future<void>.delayed(const Duration(milliseconds: 10));
      expect(
        runtime.widgetMovePreviewTarget,
        same(active),
        reason: 'a stale clear cannot erase a newer-or-equal preview',
      );

      input.add(
        encodeNbfcFrame(
          nbfcControlJson,
          utf8.encode(
            jsonEncode(_widgetMovePreviewClear(model, previewSequence: 2)),
          ),
        ),
      );
      await _waitUntil(() => runtime.widgetMovePreviewTarget == null);
      expect(runtime.widgetMovePreviewTarget, isNull);

      input.add(
        encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_close()))),
      );
      await input.close();
      await running;
      runtime.setMovePreviewResolver(null);
      expect(runtime.closed, isTrue, reason: diagnostics.join('\n'));
      expect(diagnostics, isEmpty);
      final messages = output
          .map(
            (frame) =>
                jsonDecode(utf8.decode(frame.sublist(44)))
                    as Map<String, Object?>,
          )
          .toList();
      expect(
        (messages.firstWhere(
              (message) => message['type'] == 'runner.hello',
            )['body']
            as Map<String, Object?>)['acceptedCapabilities'],
        contains('widget.movePreview.v1'),
      );
    },
  );

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
        [
          'readOnly.render',
          'readOnly.layout',
          'readOnly.selection',
          'widget.deleteSelection.v1',
        ],
      );
    },
  );

  test(
    'applies negotiated viewport state and publishes post-layout metrics',
    () async {
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
        encodeNbfcFrame(
          nbfcControlJson,
          utf8.encode(jsonEncode(_hello(viewport: true))),
        ),
      );
      final modelBytes = fixture.modelBytesForViewTest();
      _addRender(input, modelBytes);
      await _waitUntil(() => runtime.model != null);

      input.add(
        encodeNbfcFrame(
          nbfcControlJson,
          utf8.encode(
            jsonEncode(
              _viewport(
                modelBytes,
                commandSequence: 1,
                mode: 'manual',
                zoomMicros: 1250000,
                horizontalScrollMicros: 100000,
                verticalScrollMicros: 200000,
              ),
            ),
          ),
        ),
      );
      input.add(
        encodeNbfcFrame(
          nbfcControlJson,
          utf8.encode(
            jsonEncode(
              _viewport(
                modelBytes,
                commandSequence: 2,
                mode: 'manual',
                zoomMicros: 1500000,
                horizontalScrollMicros: 250000,
                verticalScrollMicros: 750000,
              ),
            ),
          ),
        ),
      );
      await _waitUntil(
        () => runtime.viewportPresentation?.zoomMicros == 1500000,
      );
      expect(
        runtime.pendingLayoutSequence,
        0,
        reason:
            'commands before the first frame coalesce into its reserved layout',
      );
      runtime.reportViewportMetrics(
        CanvasViewportMetrics(
          presentation: runtime.viewportPresentation!,
          effectiveScaleMicros: 1500000,
          horizontalScrollable: false,
          verticalScrollable: true,
        ),
      );
      await Future<void>.delayed(Duration.zero);

      input.add(
        encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_close()))),
      );
      await input.close();
      await running;
      expect(diagnostics, isEmpty);
      expect(runtime.viewportPresentation?.zoomMicros, 1500000);

      final messages = output
          .map(
            (frame) =>
                jsonDecode(utf8.decode(frame.sublist(44)))
                    as Map<String, Object?>,
          )
          .toList();
      final hello = messages.singleWhere(
        (message) => message['type'] == 'runner.hello',
      );
      expect(
        (hello['body'] as Map<String, Object?>)['acceptedCapabilities'],
        contains('viewport.presentation.v1'),
      );
      final viewport = messages
          .where((message) => message['type'] == 'runner.viewport')
          .last;
      expect(viewport['body'], {
        'commandSequence': 2,
        'presentationSequence': 4,
        'documentId': 'd2d37c77-8510-4bd0-9280-a72e5bc3871e',
        'logicalRevisionId': 2,
        'mode': 'manual',
        'zoomMicros': 1500000,
        'horizontalScrollMicros': 250000,
        'verticalScrollMicros': 750000,
        'effectiveScaleMicros': 1500000,
        'horizontalScrollable': false,
        'verticalScrollable': true,
      });
    },
  );

  test('rejects viewport zoom outside the negotiated range', () async {
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
      encodeNbfcFrame(
        nbfcControlJson,
        utf8.encode(jsonEncode(_hello(viewport: true))),
      ),
    );
    final modelBytes = fixture.modelBytesForViewTest();
    _addRender(input, modelBytes);
    input.add(
      encodeNbfcFrame(
        nbfcControlJson,
        utf8.encode(
          jsonEncode(
            _viewport(
              modelBytes,
              commandSequence: 1,
              mode: 'manual',
              zoomMicros: 2000001,
            ),
          ),
        ),
      ),
    );
    await input.close();
    await running;

    expect(runtime.closed, isTrue);
    expect(diagnostics.single, contains('zoomMicros'));
    final messages = _decodeControlMessages(output);
    expect(messages.last['type'], 'runner.failure');
  });

  test('strictly rejects a viewport command without commandSequence', () async {
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
      encodeNbfcFrame(
        nbfcControlJson,
        utf8.encode(jsonEncode(_hello(viewport: true))),
      ),
    );
    final modelBytes = fixture.modelBytesForViewTest();
    _addRender(input, modelBytes);
    final viewport = _viewport(
      modelBytes,
      commandSequence: 1,
      mode: 'fit',
      zoomMicros: 1000000,
    );
    (viewport['body']! as Map<String, Object?>).remove('commandSequence');
    input.add(
      encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(viewport))),
    );
    await input.close();
    await running;

    expect(runtime.closed, isTrue);
    expect(diagnostics.single, contains(r'$/body'));
    final messages = _decodeControlMessages(output);
    expect(messages.last['type'], 'runner.failure');
  });

  test('coalesces a rapid viewport burst into one contiguous next layout', () {
    int? pending;
    for (var change = 0; change < 100; change++) {
      pending = reserveNextCanvasLayoutSequence(7, pending);
    }
    expect(
      pending,
      8,
      reason: 'one pending frame must reserve exactly the contiguous N+1 key',
    );
    expect(
      reserveNextCanvasLayoutSequence(8, null),
      9,
      reason: 'the next completed frame advances by exactly one again',
    );
  });

  testWidgets(
    'publishes exact post-frame surface metrics and coalesces rapid resize',
    (tester) async {
      tester.view.physicalSize = const Size(800, 600);
      tester.view.devicePixelRatio = 1.25;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

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
        encodeNbfcFrame(
          nbfcControlJson,
          utf8.encode(jsonEncode(_hello(surfacePresentation: true))),
        ),
      );
      _addRender(input, fixture.modelBytesForViewTest());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (
        var attempt = 0;
        attempt < 20 &&
            _decodeControlMessages(
              output,
            ).where((message) => message['type'] == 'runner.presented').isEmpty;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }

      var presented = _decodeControlMessages(
        output,
      ).where((message) => message['type'] == 'runner.presented').toList();
      expect(presented, hasLength(1));
      expect(presented.single['body'], {
        'presentationSequence': 4,
        'documentId': 'd2d37c77-8510-4bd0-9280-a72e5bc3871e',
        'logicalRevisionId': 2,
        'frameSequence': 0,
        'layoutSequence': 0,
        'physicalWidth': 800,
        'physicalHeight': 600,
        'devicePixelRatioMicros': 1250000,
      });

      tester.view.physicalSize = const Size(1000, 700);
      tester.view.devicePixelRatio = 1.5;
      runtime.didChangeMetrics();
      tester.view.physicalSize = const Size(1200, 800);
      tester.view.devicePixelRatio = 2.0;
      for (var notification = 0; notification < 50; notification++) {
        runtime.didChangeMetrics();
      }
      expect(
        runtime.pendingLayoutSequence,
        1,
        reason: 'one resize burst must reserve one contiguous replacement',
      );

      for (
        var attempt = 0;
        attempt < 20 &&
            _decodeControlMessages(output)
                    .where((message) => message['type'] == 'runner.presented')
                    .length <
                2;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      presented = _decodeControlMessages(
        output,
      ).where((message) => message['type'] == 'runner.presented').toList();
      expect(
        presented.map(
          (message) =>
              (message['body'] as Map<String, Object?>)['layoutSequence'],
        ),
        [0, 1],
      );
      expect(presented.last['body'], containsPair('physicalWidth', 1200));
      expect(presented.last['body'], containsPair('physicalHeight', 800));
      expect(
        presented.last['body'],
        containsPair('devicePixelRatioMicros', 2000000),
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

  testWidgets('fails closed on a surface outside negotiated bounds', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(4097, 1);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

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
      encodeNbfcFrame(
        nbfcControlJson,
        utf8.encode(jsonEncode(_hello(surfacePresentation: true))),
      ),
    );
    _addRender(input, fixture.modelBytesForViewTest());
    await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
    for (var attempt = 0; attempt < 20 && diagnostics.isEmpty; attempt++) {
      await tester.pump(const Duration(milliseconds: 10));
    }
    final closing = input.close();
    await running;
    await closing;

    expect(runtime.closed, isTrue);
    expect(runtime.presentedLayoutSequence, isNull);
    expect(diagnostics.single, contains('physicalWidth'));
    final messages = _decodeControlMessages(output);
    expect(messages.last['type'], 'runner.failure');
    expect(
      messages.where((message) => message['type'] == 'runner.presented'),
      isEmpty,
    );

    await tester.pumpWidget(const SizedBox.shrink());
  });

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
        [
          'readOnly.render',
          'readOnly.layout',
          'readOnly.selection',
          'widget.deleteSelection.v1',
        ],
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

  test('publishes exact authenticated Canvas interactions', () async {
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
    final modelBytes = fixture.modelBytesForViewTest();
    _addRender(input, modelBytes);
    await _waitUntil(() => runtime.model != null);
    runtime.completePendingLayoutForTesting();
    expect(runtime.presentedLayoutSequence, 0);
    expect(runtime.interactionInputSynchronized, isFalse);
    runtime.interactFromCanvas();

    input.add(
      encodeNbfcFrame(
        nbfcControlJson,
        utf8.encode(
          jsonEncode(
            _interactionFence(modelBytes, interactionFenceSequence: 7),
          ),
        ),
      ),
    );
    await _waitUntil(() => runtime.interactionInputSynchronized);
    expect(runtime.interactionFenceSequence, 7);

    runtime.interactFromCanvas();
    runtime.interactFromCanvas();

    input.add(
      encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_close()))),
    );
    await input.close();
    await running;

    final model = runtime.model!;
    final messages = output
        .map(
          (frame) =>
              jsonDecode(utf8.decode(frame.sublist(44)))
                  as Map<String, Object?>,
        )
        .toList();
    final interactions = messages
        .where((message) => message['type'] == 'runner.interaction')
        .toList();
    expect(interactions, hasLength(2));
    final applied = messages.singleWhere(
      (message) => message['type'] == 'runner.interactionFenceApplied',
    );
    expect(applied['body'], {
      'presentationSequence': model.presentationSequence,
      'documentId': model.documentId,
      'logicalRevisionId': model.logicalRevisionId,
      'frameSequence': 0,
      'layoutSequence': 0,
      'interactionFenceSequence': 7,
    });
    expect(
      messages.indexOf(applied),
      lessThan(messages.indexOf(interactions.first)),
    );
    expect(interactions.map((message) => message['sessionId']).toSet(), {
      _hello()['sessionId'],
    });
    expect(interactions.map((message) => message['body']).toList(), [
      {
        'presentationSequence': model.presentationSequence,
        'documentId': model.documentId,
        'logicalRevisionId': model.logicalRevisionId,
        'frameSequence': 0,
        'layoutSequence': 0,
        'intentSequence': 0,
        'interactionFenceSequence': 7,
      },
      {
        'presentationSequence': model.presentationSequence,
        'documentId': model.documentId,
        'logicalRevisionId': model.logicalRevisionId,
        'frameSequence': 0,
        'layoutSequence': 0,
        'intentSequence': 1,
        'interactionFenceSequence': 7,
      },
    ]);
  });

  test('keeps input closed until the exact fence ack is flushed', () async {
    final input = StreamController<List<int>>();
    final output = <List<int>>[];
    Completer<void>? delayedAckFlush;
    var delayNextAck = true;
    String? lastOutputType;
    final runtime = CanvasRuntimeController(
      input: input.stream,
      output: (bytes) {
        output.add(List<int>.from(bytes));
        final message =
            jsonDecode(utf8.decode(bytes.sublist(44))) as Map<String, Object?>;
        lastOutputType = message['type'] as String?;
      },
      flush: () {
        if (delayNextAck &&
            lastOutputType == 'runner.interactionFenceApplied') {
          delayNextAck = false;
          delayedAckFlush = Completer<void>();
          return delayedAckFlush!.future;
        }
        return Future<void>.value();
      },
      diagnostic: fail,
    );
    final running = runtime.start();
    input.add(
      encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_hello()))),
    );
    final modelBytes = fixture.modelBytesForViewTest();
    _addRender(input, modelBytes);
    await _waitUntil(() => runtime.model != null);
    runtime.completePendingLayoutForTesting();
    input.add(
      encodeNbfcFrame(
        nbfcControlJson,
        utf8.encode(
          jsonEncode(
            _interactionFence(modelBytes, interactionFenceSequence: 7),
          ),
        ),
      ),
    );
    await _waitUntil(() => delayedAckFlush != null);

    final model = runtime.model!;
    final selected = model.widgetIds.firstWhere((id) => id != model.root.id);
    runtime.selectFromCanvas(selected);
    runtime.interactFromCanvas();
    expect(runtime.interactionInputSynchronized, isFalse);
    expect(runtime.selectedWidgetId, isNull);

    runtime.didChangeMetrics();
    expect(runtime.interactionInputSynchronized, isFalse);
    delayedAckFlush!.complete();
    await _waitUntil(() => runtime.pendingLayoutSequence == 1);
    runtime.completePendingLayoutForTesting();
    input.add(
      encodeNbfcFrame(
        nbfcControlJson,
        utf8.encode(
          jsonEncode(
            _interactionFence(
              modelBytes,
              interactionFenceSequence: 8,
              layoutSequence: 1,
            ),
          ),
        ),
      ),
    );
    await _waitUntil(() => runtime.interactionInputSynchronized);
    runtime.selectFromCanvas(selected);
    runtime.interactFromCanvas();

    input.add(
      encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_close()))),
    );
    await input.close();
    await running;

    final messages = _decodeControlMessages(output);
    final applied = messages
        .where((message) => message['type'] == 'runner.interactionFenceApplied')
        .toList();
    expect(
      applied.map(
        (message) =>
            (message['body'] as Map<String, Object?>)['layoutSequence'],
      ),
      [0, 1],
    );
    final selections = messages
        .where((message) => message['type'] == 'runner.selection')
        .toList();
    final interactions = messages
        .where((message) => message['type'] == 'runner.interaction')
        .toList();
    expect(selections, hasLength(1));
    expect(interactions, hasLength(1));
    expect(
      (selections.single['body'] as Map<String, Object?>)['layoutSequence'],
      1,
    );
    expect(
      (interactions.single['body'] as Map<String, Object?>)['layoutSequence'],
      1,
    );
    expect(
      messages.indexOf(applied.last),
      lessThan(messages.indexOf(interactions.single)),
    );
  });

  test('strictly rejects an interaction fence with missing identity', () async {
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
    final modelBytes = fixture.modelBytesForViewTest();
    _addRender(input, modelBytes);
    final fence = _interactionFence(modelBytes, interactionFenceSequence: 7);
    (fence['body']! as Map<String, Object?>).remove('layoutSequence');
    input.add(encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(fence))));
    await input.close();
    await running;

    expect(runtime.closed, isTrue);
    expect(diagnostics.single, contains(r'$/body'));
    final messages = _decodeControlMessages(output);
    expect(messages.last['type'], 'runner.failure');
  });

  test(
    'publishes exact delete intent without mutating the selected model',
    () async {
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
      await _waitUntil(() => runtime.model != null);
      runtime.completePendingLayoutForTesting();

      await _synchronizeInteractionFence(runtime, input);

      final model = runtime.model!;
      final selected = model.widgetIds.firstWhere((id) => id != model.root.id);
      runtime.selectFromCanvas(selected);
      expect(runtime.deleteSelectedFromCanvas(), isTrue);
      expect(runtime.model, same(model));
      expect(runtime.model!.widgetIds, contains(selected));

      runtime.selectFromCanvas(model.root.id);
      expect(
        runtime.deleteSelectedFromCanvas(),
        isFalse,
        reason: 'the immutable document root cannot be deleted',
      );
      input.add(
        encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(_close()))),
      );
      await input.close();
      await running;

      final messages = _decodeControlMessages(output);
      final deletions = messages
          .where((message) => message['type'] == 'runner.deleteSelection')
          .toList();
      expect(deletions, hasLength(1));
      expect(deletions.single['sessionId'], _hello()['sessionId']);
      expect(deletions.single['body'], {
        'presentationSequence': model.presentationSequence,
        'documentId': model.documentId,
        'logicalRevisionId': model.logicalRevisionId,
        'frameSequence': 0,
        'layoutSequence': 0,
        'intentSequence': 1,
        'widgetId': selected,
      });
    },
  );

  testWidgets('does not publish delete without negotiated capability', (
    tester,
  ) async {
    final input = StreamController<List<int>>();
    final runtime = CanvasRuntimeController(
      input: input.stream,
      output: (_) {},
      flush: () async {},
      diagnostic: fail,
    );
    final running = runtime.start();
    input.add(
      encodeNbfcFrame(
        nbfcControlJson,
        utf8.encode(jsonEncode(_hello(deleteSelected: false))),
      ),
    );
    _addRender(input, fixture.modelBytesForViewTest());
    await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
    for (var attempt = 0; attempt < 20 && runtime.model == null; attempt++) {
      await tester.pump(const Duration(milliseconds: 10));
    }
    await tester.pump();
    final model = runtime.model!;
    runtime.selectFromCanvas(
      model.widgetIds.firstWhere((id) => id != model.root.id),
    );
    expect(runtime.deleteSelectedFromCanvas(), isFalse);
    await input.close();
    for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
      await tester.pump(const Duration(milliseconds: 10));
    }
    await running;
    await tester.pumpWidget(const SizedBox.shrink());
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
          insertionIndex: 0,
        ),
      );
      expect(
        await _sourceAwareHover(runtime, input, {
          'token':
              'nbfdnd:v1:83331c6c-91e1-4ba3-bb2d-597fd3cfbc4d:8ee8e7b6-0556-4bf7-825c-1c340fb8616a',
          'xMicros': 500000,
          'yMicros': 500000,
          'generation': 0,
          'probeId': 0,
        }),
        isFalse,
        reason: 'a list insertion must match the exact terminal child count',
      );
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
        await _sourceAwareHover(runtime, input, {
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
        await _sourceAwareHover(runtime, input, <Object?, Object?>{
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
    'requires one exact source authority and clears it on leave and render',
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
      _addRender(input, _emptyScaffoldModelBytes());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (
        var attempt = 0;
        attempt < 20 && runtime.presentedLayoutSequence == null;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      final rootId = runtime.model!.root.id;
      CanvasPaletteDragSource? receivedSource;
      runtime.setDropResolver((_, _, [source]) {
        receivedSource = source;
        return CanvasDropTarget(
          parentWidgetId: rootId,
          slotName: 'body',
          insertionIndex: 0,
        );
      });
      const token =
          'nbfdnd:v1:da2c5989-ec72-4aef-8f28-b6c5f702c75b:35b44b3d-60dc-4bbc-95f3-1da55c3cd547';
      final request = {
        'token': token,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 1,
        'probeId': 1,
      };

      expect(
        await runtime.receiveNativePaletteHover(request),
        isFalse,
        reason: 'native hover cannot invent source type or traits',
      );
      expect(receivedSource, isNull);

      await _bindPaletteSource(runtime, input, token);
      final approvedRequest = {...request, 'probeId': 2};
      expect(await runtime.receiveNativePaletteHover(approvedRequest), isTrue);
      expect(receivedSource?.token, token);
      expect(receivedSource?.widgetType, 'flutter.widgets.Text');
      expect(receivedSource?.traits, isEmpty);
      expect(
        await runtime.receiveNativePaletteHoverLeave({'generation': 1}),
        isTrue,
      );
      expect(runtime.paletteDragSourceToken, isNull);
      expect(await runtime.receiveNativePaletteHover(request), isFalse);

      await _bindPaletteSource(runtime, input, token);
      expect(runtime.paletteDragSourceToken, token);
      final nextJson =
          jsonDecode(utf8.decode(_emptyScaffoldModelBytes()))
              as Map<String, Object?>;
      nextJson['presentationSequence'] = 5;
      nextJson['logicalRevisionId'] = 3;
      _addRender(input, Uint8List.fromList(utf8.encode(jsonEncode(nextJson))));
      for (
        var attempt = 0;
        attempt < 20 && runtime.model?.presentationSequence != 5;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      expect(runtime.model?.presentationSequence, 5);
      expect(runtime.paletteDragSourceToken, isNull);

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
    'admits ElevatedButton as an exact source-aware runtime type',
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
      _addRender(input, _emptyScaffoldModelBytes());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (
        var attempt = 0;
        attempt < 20 && runtime.presentedLayoutSequence == null;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }

      final rootId = runtime.model!.root.id;
      runtime.setDropResolver(
        (_, _, [source]) =>
            source?.widgetType == 'flutter.material.ElevatedButton'
            ? CanvasDropTarget(
                parentWidgetId: rootId,
                slotName: 'body',
                insertionIndex: 0,
              )
            : null,
      );
      const token =
          'nbfdnd:v1:bca8a67d-5ea2-4fd3-b1b6-ded21d143aa2:'
          'b0f76ad5-65bc-4016-ab42-6930c12fac83';
      final request = {
        'token': token,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 1,
        'probeId': 1,
      };
      expect(
        await _sourceAwareHover(
          runtime,
          input,
          request,
          widgetType: 'flutter.material.ElevatedButton',
        ),
        isTrue,
      );
      expect(await runtime.receiveNativePaletteDropPrepare(request), isTrue);
      final commit = runtime.receiveNativePaletteDropCommit(request);
      await tester.pump();
      expect(await commit, isTrue);

      final closing = input.close();
      for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await closing;
      await running;
      await tester.pumpWidget(const SizedBox.shrink());

      final drops = (_decodeControlMessages(
        output,
      )).where((message) => message['type'] == 'runner.paletteDrop').toList();
      expect(drops, hasLength(1));
      expect(drops.single['body'], containsPair('token', token));
      expect(drops.single['body'], containsPair('parentWidgetId', rootId));
      expect(drops.single['body'], containsPair('slotName', 'body'));
    },
    timeout: const Timeout(Duration(seconds: 15)),
  );

  testWidgets(
    'publishes exact ADD into an empty Center child and rejects other slots',
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
      _addRender(input, _emptyCenterModelBytes());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (
        var attempt = 0;
        attempt < 20 && runtime.presentedLayoutSequence == null;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      expect(runtime.presentedLayoutSequence, 0);

      const centerId = '733ef462-41d7-4849-b780-5d9520666ae4';
      const invalidToken =
          'nbfdnd:v1:1e8e54fa-c40a-487c-9fde-4553e2d46f89:ce68f433-b70f-49ed-8afe-d81d92ac207a';
      runtime.setDropResolver(
        (_, _) => const CanvasDropTarget(
          parentWidgetId: centerId,
          slotName: 'children',
          insertionIndex: 0,
        ),
      );
      expect(
        await _sourceAwareHover(runtime, input, {
          'token': invalidToken,
          'xMicros': 500000,
          'yMicros': 500000,
          'generation': 1,
          'probeId': 1,
        }),
        isFalse,
        reason: 'Center exposes child, never children',
      );

      const invalidIndexToken =
          'nbfdnd:v1:d8b9847e-7858-4057-aa79-b521ea8acb58:d33d6ec2-88d7-41fd-968d-3f9058fa581b';
      runtime.setDropResolver(
        (_, _) => const CanvasDropTarget(
          parentWidgetId: centerId,
          slotName: 'child',
          insertionIndex: 1,
        ),
      );
      expect(
        await _sourceAwareHover(runtime, input, {
          'token': invalidIndexToken,
          'xMicros': 500000,
          'yMicros': 500000,
          'generation': 2,
          'probeId': 2,
        }),
        isFalse,
        reason: 'an empty single slot accepts exactly index zero',
      );

      const token =
          'nbfdnd:v1:3a6160af-b935-4ef6-b5af-4be3bd61bef4:ab74b9a9-cf79-43ab-a692-8bb39968ac39';
      runtime.setDropResolver(
        (_, _) => const CanvasDropTarget(
          parentWidgetId: centerId,
          slotName: 'child',
          insertionIndex: 0,
        ),
      );
      final request = {
        'token': token,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 3,
        'probeId': 3,
      };
      expect(await _sourceAwareHover(runtime, input, request), isTrue);
      expect(await runtime.receiveNativePaletteDropPrepare(request), isTrue);
      final commit = runtime.receiveNativePaletteDropCommit(request);
      await tester.pump();
      expect(await commit, isTrue);

      final closing = input.close();
      for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await closing;
      await running;
      await tester.pumpWidget(const SizedBox.shrink());

      final drops = (_decodeControlMessages(
        output,
      )).where((message) => message['type'] == 'runner.paletteDrop').toList();
      expect(drops, hasLength(1));
      expect(drops.single['body'], {
        'presentationSequence': 4,
        'documentId': 'd2d37c77-8510-4bd0-9280-a72e5bc3871e',
        'logicalRevisionId': 2,
        'frameSequence': 0,
        'layoutSequence': 0,
        'intentSequence': 0,
        'token': token,
        'operation': 'ADD',
        'parentWidgetId': centerId,
        'slotName': 'child',
        'insertionIndex': 0,
      });
    },
    timeout: const Timeout(Duration(seconds: 15)),
  );

  testWidgets(
    'rejects a resolver claim for an occupied single-child slot',
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
      for (
        var attempt = 0;
        attempt < 20 && runtime.presentedLayoutSequence == null;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }

      runtime.setDropResolver(
        (_, _) => const CanvasDropTarget(
          parentWidgetId: '733ef462-41d7-4849-b780-5d9520666ae4',
          slotName: 'child',
          insertionIndex: 0,
        ),
      );
      expect(
        await _sourceAwareHover(runtime, input, {
          'token':
              'nbfdnd:v1:adae8952-a918-4a9a-89db-914014487296:e7a0872b-6166-4af8-a188-864389ae7fce',
          'xMicros': 500000,
          'yMicros': 500000,
          'generation': 1,
          'probeId': 1,
        }),
        isFalse,
      );

      final closing = input.close();
      for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await closing;
      await running;
      await tester.pumpWidget(const SizedBox.shrink());
    },
    timeout: const Timeout(Duration(seconds: 15)),
  );

  testWidgets(
    'publishes exact source-aware Scaffold appBar, body and FAB ADD intents',
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
      _addRender(input, _emptyScaffoldModelBytes());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (
        var attempt = 0;
        attempt < 20 && runtime.presentedLayoutSequence == null;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      expect(runtime.presentedLayoutSequence, 0);
      final rootId = runtime.model!.root.id;

      const invalidToken =
          'nbfdnd:v1:7f4f1680-645b-4c21-9e75-b7adbe195518:2ecf583a-bd0c-4b20-b271-3a7f92573383';
      runtime.setDropResolver(
        (_, _) => CanvasDropTarget(
          parentWidgetId: rootId,
          slotName: 'appBar',
          insertionIndex: 0,
        ),
      );
      expect(
        await _sourceAwareHover(runtime, input, {
          'token': invalidToken,
          'xMicros': 500000,
          'yMicros': 500000,
          'generation': 1,
          'probeId': 1,
        }),
        isFalse,
        reason: 'Text is not a PreferredSizeWidget',
      );

      const appBarToken =
          'nbfdnd:v1:da2c5989-ec72-4aef-8f28-b6c5f702c75b:35b44b3d-60dc-4bbc-95f3-1da55c3cd547';
      final appBarRequest = {
        'token': appBarToken,
        'xMicros': 500000,
        'yMicros': 10000,
        'generation': 2,
        'probeId': 2,
      };
      expect(
        await _sourceAwareHover(
          runtime,
          input,
          appBarRequest,
          widgetType: 'flutter.material.AppBar',
          traits: const {canvasPreferredSizeWidgetTrait},
        ),
        isTrue,
      );
      expect(
        await runtime.receiveNativePaletteDropPrepare(appBarRequest),
        isTrue,
      );
      final appBarCommit = runtime.receiveNativePaletteDropCommit(
        appBarRequest,
      );
      await tester.pump();
      expect(await appBarCommit, isTrue);

      const bodyToken =
          'nbfdnd:v1:8db14fa5-7dc0-4ebc-931b-910df02f3515:6436b8e0-4077-4a67-8b38-f5a6ef0fc848';
      runtime.setDropResolver(
        (_, _) => CanvasDropTarget(
          parentWidgetId: rootId,
          slotName: 'body',
          insertionIndex: 0,
        ),
      );
      final bodyRequest = {
        'token': bodyToken,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 3,
        'probeId': 3,
      };
      expect(await _sourceAwareHover(runtime, input, bodyRequest), isTrue);
      expect(
        await runtime.receiveNativePaletteDropPrepare(bodyRequest),
        isTrue,
      );
      final bodyCommit = runtime.receiveNativePaletteDropCommit(bodyRequest);
      await tester.pump();
      expect(await bodyCommit, isTrue);

      const fabToken =
          'nbfdnd:v1:0a0ca345-fc70-49f0-86fb-e220044564d7:34049612-cc2b-44e7-ac70-24cb5c72b22d';
      runtime.setDropResolver(
        (_, _) => CanvasDropTarget(
          parentWidgetId: rootId,
          slotName: 'floatingActionButton',
          insertionIndex: 0,
        ),
      );
      final fabRequest = {
        'token': fabToken,
        'xMicros': 900000,
        'yMicros': 900000,
        'generation': 4,
        'probeId': 4,
      };
      expect(await _sourceAwareHover(runtime, input, fabRequest), isTrue);
      expect(await runtime.receiveNativePaletteDropPrepare(fabRequest), isTrue);
      final fabCommit = runtime.receiveNativePaletteDropCommit(fabRequest);
      await tester.pump();
      expect(await fabCommit, isTrue);

      final closing = input.close();
      for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await closing;
      await running;
      await tester.pumpWidget(const SizedBox.shrink());

      final drops = (_decodeControlMessages(
        output,
      )).where((message) => message['type'] == 'runner.paletteDrop').toList();
      expect(drops, hasLength(3));
      expect(
        (drops[0]['body'] as Map<String, Object?>),
        containsPair('slotName', 'appBar'),
      );
      expect(
        (drops[1]['body'] as Map<String, Object?>),
        containsPair('slotName', 'body'),
      );
      expect(
        (drops[2]['body'] as Map<String, Object?>),
        containsPair('slotName', 'floatingActionButton'),
      );
      expect(
        drops.map(
          (drop) => (drop['body'] as Map<String, Object?>)['insertionIndex'],
        ),
        everyElement(0),
      );
    },
    timeout: const Timeout(Duration(seconds: 15)),
  );

  testWidgets(
    'publishes exact ADD into an empty Padding child',
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
      _addRender(input, _emptyPaddingModelBytes());
      await tester.pumpWidget(NativeCanvasApp(runtime: runtime));
      for (
        var attempt = 0;
        attempt < 20 && runtime.presentedLayoutSequence == null;
        attempt++
      ) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      expect(runtime.presentedLayoutSequence, 0);

      const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';
      const token =
          'nbfdnd:v1:36bb9227-fc10-4aaa-b9e8-829060121ff4:e7a020a4-a19f-4883-b0df-b5ce3cc28d7e';
      runtime.setDropResolver(
        (_, _) => const CanvasDropTarget(
          parentWidgetId: paddingId,
          slotName: 'child',
          insertionIndex: 0,
        ),
      );
      final request = {
        'token': token,
        'xMicros': 500000,
        'yMicros': 500000,
        'generation': 1,
        'probeId': 1,
      };
      expect(await _sourceAwareHover(runtime, input, request), isTrue);
      expect(await runtime.receiveNativePaletteDropPrepare(request), isTrue);
      final commit = runtime.receiveNativePaletteDropCommit(request);
      await tester.pump();
      expect(await commit, isTrue);

      final closing = input.close();
      for (var attempt = 0; attempt < 20 && !runtime.closed; attempt++) {
        await tester.pump(const Duration(milliseconds: 10));
      }
      await closing;
      await running;
      await tester.pumpWidget(const SizedBox.shrink());

      final drops = (_decodeControlMessages(
        output,
      )).where((message) => message['type'] == 'runner.paletteDrop').toList();
      expect(drops, hasLength(1));
      expect(drops.single['body'], containsPair('parentWidgetId', paddingId));
      expect(drops.single['body'], containsPair('slotName', 'child'));
      expect(drops.single['body'], containsPair('insertionIndex', 0));
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
        await _sourceAwareHover(runtime, input, {
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
        find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
        findsOneWidget,
      );
      final overlaySemantics = tester.widget<Semantics>(
        find
            .ancestor(
              of: find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
              matching: find.byType(Semantics),
            )
            .first,
      );
      expect(
        overlaySemantics.properties.label,
        'Flutter widget insertion target for children',
      );

      resolveColumn = true;
      expect(
        await _sourceAwareHover(runtime, input, {
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
        await _sourceAwareHover(runtime, input, {
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
        await _sourceAwareHover(runtime, input, {
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
        find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
        findsNothing,
      );

      expect(
        await _sourceAwareHover(runtime, input, {
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
        find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
        findsNothing,
      );
      expect(
        await _sourceAwareHover(runtime, input, {
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
        await _sourceAwareHover(runtime, input, {
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
        await _sourceAwareHover(runtime, input, {
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
        await _sourceAwareHover(runtime, input, {
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
        await _sourceAwareHover(runtime, input, {
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
        await _sourceAwareHover(runtime, input, {
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
        await _sourceAwareHover(runtime, input, {
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
      await _bindPaletteSource(runtime, input, token);
      final hoverFuture = _sourceAwareHover(runtime, input, request);
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

      final drops = (_decodeControlMessages(
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
      expect(await _sourceAwareHover(runtime, input, firstRequest), isTrue);
      expect(
        await runtime.receiveNativePaletteDropPrepare(firstRequest),
        isTrue,
      );
      expect(
        (_decodeControlMessages(
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
        (_decodeControlMessages(
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
      expect(await _sourceAwareHover(runtime, input, committedRequest), isTrue);
      expect(
        await runtime.receiveNativePaletteDropPrepare(committedRequest),
        isTrue,
      );
      expect(
        (_decodeControlMessages(
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

      final drops = (_decodeControlMessages(
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

Uint8List _emptyCenterModelBytes() {
  final json =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  final root = json['root']! as Map<String, Object?>;
  final center = _findNodeByType(root, 'flutter.widgets.Center')!;
  (center['properties']! as Map<String, Object?>)
    ..remove('widthFactor')
    ..remove('heightFactor');
  ((center['slots']! as Map<String, Object?>)['child']!
          as Map<String, Object?>)['child'] =
      null;
  ((root['slots']! as Map<String, Object?>)['body']!
          as Map<String, Object?>)['child'] =
      center;
  return Uint8List.fromList(utf8.encode(jsonEncode(json)));
}

Uint8List _emptyScaffoldModelBytes() {
  final json =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  final root = json['root']! as Map<String, Object?>;
  final slots = root['slots']! as Map<String, Object?>;
  (slots['body']! as Map<String, Object?>)['child'] = null;
  slots['floatingActionButton'] = <String, Object?>{
    'kind': 'single',
    'child': null,
  };
  return Uint8List.fromList(utf8.encode(jsonEncode(json)));
}

Uint8List _emptyPaddingModelBytes() {
  final json =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  final root = json['root']! as Map<String, Object?>;
  final padding = _findNodeByType(root, 'flutter.widgets.Padding')!;
  ((padding['slots']! as Map<String, Object?>)['child']!
          as Map<String, Object?>)['child'] =
      null;
  ((root['slots']! as Map<String, Object?>)['body']!
          as Map<String, Object?>)['child'] =
      padding;
  return Uint8List.fromList(utf8.encode(jsonEncode(json)));
}

Map<String, Object?>? _findNodeByType(Map<String, Object?> node, String type) {
  if (node['type'] == type) {
    return node;
  }
  final slots = node['slots']! as Map<String, Object?>;
  for (final rawSlot in slots.values) {
    final slot = rawSlot! as Map<String, Object?>;
    final child = slot['child'];
    if (child is Map<String, Object?>) {
      final match = _findNodeByType(child, type);
      if (match != null) {
        return match;
      }
    }
    final children = slot['children'];
    if (children is List<Object?>) {
      for (final child in children.cast<Map<String, Object?>>()) {
        final match = _findNodeByType(child, type);
        if (match != null) {
          return match;
        }
      }
    }
  }
  return null;
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

List<Map<String, Object?>> _decodeControlMessages(List<List<int>> output) {
  return [
    for (final frame in output)
      if (frame.length >= 44 && frame[5] == nbfcControlJson)
        jsonDecode(utf8.decode(frame.sublist(44))) as Map<String, Object?>,
  ];
}

Future<void> _waitUntil(bool Function() predicate) async {
  for (var attempt = 0; attempt < 200 && !predicate(); attempt++) {
    await Future<void>.delayed(const Duration(milliseconds: 1));
  }
  expect(predicate(), isTrue, reason: 'runtime condition was not reached');
}

Map<String, Object?> _hello({
  bool deleteSelected = true,
  bool viewport = false,
  bool widgetMovePreview = false,
  bool surfacePresentation = false,
  bool sourceAwarePaletteDrop = true,
}) => {
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
      'palette.drop.catalogInsert.v1',
      if (sourceAwarePaletteDrop) 'palette.drop.sourceAware.v1',
      if (deleteSelected) 'widget.deleteSelection.v1',
      if (widgetMovePreview) 'widget.movePreview.v1',
      if (viewport) 'viewport.presentation.v1',
      if (surfacePresentation) 'surface.presentation.v1',
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

Future<bool> _sourceAwareHover(
  CanvasRuntimeController runtime,
  StreamController<List<int>> input,
  Object? arguments, {
  String widgetType = 'flutter.widgets.Text',
  Set<String> traits = const {},
}) async {
  if (!runtime.interactionInputSynchronized) {
    await _synchronizeInteractionFence(runtime, input);
  }
  if (arguments is Map && arguments['token'] is String) {
    final token = arguments['token']! as String;
    if (RegExp(
          r'^nbfdnd:v1:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
        ).hasMatch(token) &&
        runtime.paletteDragSourceToken != token) {
      await _bindPaletteSource(
        runtime,
        input,
        token,
        widgetType: widgetType,
        traits: traits,
      );
    }
  }
  return runtime.receiveNativePaletteHover(arguments);
}

Future<void> _bindPaletteSource(
  CanvasRuntimeController runtime,
  StreamController<List<int>> input,
  String token, {
  String widgetType = 'flutter.widgets.Text',
  Set<String> traits = const {},
}) async {
  await _synchronizeInteractionFence(runtime, input);
  final model = runtime.model;
  final layoutSequence = runtime.presentedLayoutSequence;
  if (model == null || layoutSequence == null) {
    return;
  }
  final control = {
    'format': 'netbeans-flutter-canvas-runtime',
    'protocolVersion': 1,
    'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
    'type': 'host.paletteDragSource',
    'body': {
      'presentationSequence': model.presentationSequence,
      'documentId': model.documentId,
      'logicalRevisionId': model.logicalRevisionId,
      'frameSequence': 0,
      'layoutSequence': layoutSequence,
      'token': token,
      'widgetType': widgetType,
      'traits': traits.toList()..sort(),
    },
  };
  input.add(encodeNbfcFrame(nbfcControlJson, utf8.encode(jsonEncode(control))));
  for (
    var attempt = 0;
    attempt < 20 && runtime.paletteDragSourceToken != token;
    attempt++
  ) {
    await Future<void>.microtask(() {});
  }
  expect(runtime.paletteDragSourceToken, token);
}

Future<void> _synchronizeInteractionFence(
  CanvasRuntimeController runtime,
  StreamController<List<int>> input,
) async {
  if (runtime.interactionInputSynchronized) {
    return;
  }
  final model = runtime.model;
  final layoutSequence = runtime.presentedLayoutSequence;
  expect(model, isNotNull, reason: 'a fence requires a presented model');
  expect(
    layoutSequence,
    isNotNull,
    reason: 'a fence requires exact published layout identity',
  );
  final nextSequence = runtime.interactionFenceSequence + 1;
  final body = {
    'presentationSequence': model!.presentationSequence,
    'documentId': model.documentId,
    'logicalRevisionId': model.logicalRevisionId,
    'frameSequence': 0,
    'layoutSequence': layoutSequence,
    'interactionFenceSequence': nextSequence,
  };
  input.add(
    encodeNbfcFrame(
      nbfcControlJson,
      utf8.encode(
        jsonEncode({
          'format': 'netbeans-flutter-canvas-runtime',
          'protocolVersion': 1,
          'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
          'type': 'host.interactionFence',
          'body': body,
        }),
      ),
    ),
  );
  for (
    var attempt = 0;
    attempt < 200 && !runtime.interactionInputSynchronized;
    attempt++
  ) {
    await Future<void>.microtask(() {});
  }
  expect(
    runtime.interactionInputSynchronized,
    isTrue,
    reason: 'the exact test interaction fence was not applied',
  );
}

Map<String, Object?> _widgetMovePreview(
  Uint8List model, {
  required int previewSequence,
  required String sourceWidgetId,
  required String parentWidgetId,
  required String slotName,
  required int insertionIndex,
}) {
  final json = jsonDecode(utf8.decode(model)) as Map<String, Object?>;
  return {
    'format': 'netbeans-flutter-canvas-runtime',
    'protocolVersion': 1,
    'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
    'type': 'host.widgetMovePreview',
    'body': {
      'presentationSequence': json['presentationSequence'],
      'documentId': json['documentId'],
      'logicalRevisionId': json['logicalRevisionId'],
      'frameSequence': 0,
      'layoutSequence': 0,
      'previewSequence': previewSequence,
      'sourceWidgetId': sourceWidgetId,
      'parentWidgetId': parentWidgetId,
      'slotName': slotName,
      'insertionIndex': insertionIndex,
    },
  };
}

Map<String, Object?> _widgetMovePreviewClear(
  Uint8List model, {
  required int previewSequence,
}) {
  final json = jsonDecode(utf8.decode(model)) as Map<String, Object?>;
  return {
    'format': 'netbeans-flutter-canvas-runtime',
    'protocolVersion': 1,
    'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
    'type': 'host.widgetMovePreviewClear',
    'body': {
      'presentationSequence': json['presentationSequence'],
      'documentId': json['documentId'],
      'logicalRevisionId': json['logicalRevisionId'],
      'frameSequence': 0,
      'layoutSequence': 0,
      'previewSequence': previewSequence,
    },
  };
}

Map<String, Object?> _viewport(
  Uint8List model, {
  required int commandSequence,
  required String mode,
  required int zoomMicros,
  int horizontalScrollMicros = 0,
  int verticalScrollMicros = 0,
}) {
  final json = jsonDecode(utf8.decode(model)) as Map<String, Object?>;
  return {
    'format': 'netbeans-flutter-canvas-runtime',
    'protocolVersion': 1,
    'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
    'type': 'host.viewport',
    'body': {
      'commandSequence': commandSequence,
      'presentationSequence': json['presentationSequence'],
      'documentId': json['documentId'],
      'logicalRevisionId': json['logicalRevisionId'],
      'mode': mode,
      'zoomMicros': zoomMicros,
      'horizontalScrollMicros': horizontalScrollMicros,
      'verticalScrollMicros': verticalScrollMicros,
    },
  };
}

Map<String, Object?> _interactionFence(
  Uint8List model, {
  required int interactionFenceSequence,
  int layoutSequence = 0,
}) {
  final json = jsonDecode(utf8.decode(model)) as Map<String, Object?>;
  return {
    'format': 'netbeans-flutter-canvas-runtime',
    'protocolVersion': 1,
    'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
    'type': 'host.interactionFence',
    'body': {
      'presentationSequence': json['presentationSequence'],
      'documentId': json['documentId'],
      'logicalRevisionId': json['logicalRevisionId'],
      'frameSequence': 0,
      'layoutSequence': layoutSequence,
      'interactionFenceSequence': interactionFenceSequence,
    },
  };
}

Map<String, Object?> _close() => {
  'format': 'netbeans-flutter-canvas-wire',
  'protocolVersion': 1,
  'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
  'sequence': 1,
  'type': 'host.close',
  'body': {'reason': 'formClosed'},
};
