import 'dart:convert';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

void main() {
  test('models the reviewed multi-slot Canvas insertion matrix', () {
    expect(canvasChildrenAppendDropSlot.insertionIndexFor(0), 0);
    expect(canvasChildrenAppendDropSlot.insertionIndexFor(7), 7);
    expect(canvasChildrenAppendDropSlot.insertionIndexFor(10000), isNull);
    expect(
      canvasChildrenAppendDropSlot.accepts(
        currentChildCount: 7,
        insertionIndex: 6,
      ),
      isFalse,
    );
    expect(canvasEmptyChildDropSlot.insertionIndexFor(0), 0);
    expect(canvasEmptyChildDropSlot.insertionIndexFor(1), isNull);
    expect(canvasDropSlotsForWidgetType('flutter.material.Scaffold'), const [
      canvasScaffoldBodyDropSlot,
      canvasScaffoldFloatingActionButtonDropSlot,
    ]);
    expect(
      canvasDropSlotForWidgetSlot('flutter.material.Scaffold', 'body'),
      same(canvasScaffoldBodyDropSlot),
    );
    expect(
      canvasDropSlotForWidgetSlot(
        'flutter.material.Scaffold',
        'floatingActionButton',
      ),
      same(canvasScaffoldFloatingActionButtonDropSlot),
    );
    expect(
      canvasDropSlotForWidgetSlot('flutter.material.Scaffold', 'appBar'),
      isNull,
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Padding'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Center'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.SizedBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Text'), isEmpty);
    expect(
      canvasEmptyChildDropSlot.accepts(currentChildCount: 0, insertionIndex: 1),
      isFalse,
    );
    expect(canvasChildrenAppendDropSlot.modelSlotKind, 'list');
    expect(canvasScaffoldBodyDropSlot.modelSlotKind, 'single');
  });

  testWidgets('applies every exact adaptive target to the Flutter theme', (
    tester,
  ) async {
    const expected = <String, TargetPlatform>{
      'android': TargetPlatform.android,
      'ios': TargetPlatform.iOS,
      'windows': TargetPlatform.windows,
      'macos': TargetPlatform.macOS,
      'linux': TargetPlatform.linux,
      // Web layout preview uses the browser-sized viewport while the native
      // Windows runner supplies the closest available adaptive platform.
      'web': TargetPlatform.windows,
    };

    for (final entry in expected.entries) {
      final json =
          jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
              as Map<String, Object?>;
      final profile = json['profile']! as Map<String, Object?>;
      profile['targetPlatform'] = entry.key;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );

      final app = tester.widget<MaterialApp>(find.byType(MaterialApp));
      expect(app.theme!.platform, entry.value, reason: entry.key);
      expect(app.darkTheme!.platform, entry.value, reason: entry.key);
    }
  });

  test(
    'rejects an unknown adaptive target instead of silently falling back',
    () {
      expect(
        () => canvasAdaptiveTargetPlatform('unknown'),
        throwsArgumentError,
      );
    },
  );

  testWidgets(
    'keeps the logical MediaQuery fixed while manual zoom enables panning',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      final metrics = <CanvasViewportMetrics>[];
      final changes = <CanvasViewportPresentation>[];
      final presentation = CanvasViewportPresentation.fit(
        model,
      ).copyWith(mode: 'manual', zoomMicros: 2000000);
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: SizedBox(
              width: 600,
              height: 400,
              child: CanvasDocumentView(
                model: model,
                selectedWidgetId: null,
                onSelected: (_) {},
                viewportPresentation: presentation,
                onViewportPresentationChanged: changes.add,
                onViewportMetricsChanged: metrics.add,
              ),
            ),
          ),
        ),
      );
      await tester.pump();

      final rootContext = tester.element(
        find.byKey(ValueKey('canvas-widget-${model.root.id}')),
      );
      expect(MediaQuery.sizeOf(rootContext), const Size(390, 844));
      expect(
        MediaQuery.devicePixelRatioOf(rootContext),
        model.profile.devicePixelRatio,
      );
      expect(
        find.byKey(const ValueKey('canvas-horizontal-viewport-scrollbar')),
        findsOneWidget,
      );
      expect(
        find.byKey(const ValueKey('canvas-vertical-viewport-scrollbar')),
        findsOneWidget,
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-horizontal-viewport-scrollbar'),
              ),
            )
            .height,
        12,
        reason: 'the transparent pointer target remains comfortably usable',
      );
      expect(
        tester
            .getSize(
              find.byKey(const ValueKey('canvas-vertical-viewport-scrollbar')),
            )
            .width,
        12,
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-horizontal-viewport-scrollbar-track'),
              ),
            )
            .height,
        6,
        reason: 'the visible horizontal track is exactly twice as thin',
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-vertical-viewport-scrollbar-track'),
              ),
            )
            .width,
        6,
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-horizontal-viewport-scrollbar-thumb'),
              ),
            )
            .height,
        6,
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-vertical-viewport-scrollbar-thumb'),
              ),
            )
            .width,
        6,
      );
      expect(metrics.last.effectiveScaleMicros, 2000000);
      expect(metrics.last.horizontalScrollable, isTrue);
      expect(metrics.last.verticalScrollable, isTrue);

      final horizontalHit = tester.getRect(
        find.byKey(const ValueKey('canvas-horizontal-viewport-scrollbar')),
      );
      final horizontalTrack = tester.getRect(
        find.byKey(
          const ValueKey('canvas-horizontal-viewport-scrollbar-track'),
        ),
      );
      final horizontalGutter = Offset(
        horizontalHit.center.dx,
        horizontalHit.top + 1,
      );
      expect(horizontalTrack.contains(horizontalGutter), isFalse);
      final horizontalGesture = await tester.startGesture(
        horizontalGutter,
        kind: PointerDeviceKind.mouse,
      );
      await tester.pump();
      expect(changes.last.horizontalScrollMicros, 500000);
      await horizontalGesture.moveBy(const Offset(40, 0));
      await tester.pump();
      expect(changes.last.horizontalScrollMicros, greaterThan(500000));
      await horizontalGesture.up();

      final verticalHit = tester.getRect(
        find.byKey(const ValueKey('canvas-vertical-viewport-scrollbar')),
      );
      final verticalTrack = tester.getRect(
        find.byKey(const ValueKey('canvas-vertical-viewport-scrollbar-track')),
      );
      final verticalGutter = Offset(
        verticalHit.left + 1,
        verticalHit.center.dy,
      );
      expect(verticalTrack.contains(verticalGutter), isFalse);
      final verticalGesture = await tester.startGesture(
        verticalGutter,
        kind: PointerDeviceKind.mouse,
      );
      await tester.pump();
      expect(changes.last.verticalScrollMicros, 500000);
      await verticalGesture.up();
    },
  );

  testWidgets('native wheel pans and Ctrl-wheel changes manual zoom', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    final changes = <CanvasViewportPresentation>[];
    final presentation = CanvasViewportPresentation.fit(
      model,
    ).copyWith(mode: 'manual', zoomMicros: 1500000);
    await tester.pumpWidget(
      MaterialApp(
        home: Center(
          child: SizedBox(
            width: 600,
            height: 400,
            child: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
              viewportPresentation: presentation,
              onViewportPresentationChanged: changes.add,
            ),
          ),
        ),
      ),
    );
    await tester.pump();
    final center = tester.getCenter(find.byType(CanvasDocumentView));

    tester.binding.handlePointerEvent(
      PointerScrollEvent(
        position: center,
        kind: PointerDeviceKind.mouse,
        scrollDelta: const Offset(0, 100),
      ),
    );
    await tester.pump();
    expect(changes.last.verticalScrollMicros, greaterThan(0));
    expect(changes.last.horizontalScrollMicros, 0);

    await tester.sendKeyDownEvent(LogicalKeyboardKey.shiftLeft);
    tester.binding.handlePointerEvent(
      PointerScrollEvent(
        position: center,
        kind: PointerDeviceKind.mouse,
        scrollDelta: const Offset(0, 100),
      ),
    );
    await tester.sendKeyUpEvent(LogicalKeyboardKey.shiftLeft);
    await tester.pump();
    expect(changes.last.horizontalScrollMicros, greaterThan(0));

    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    tester.binding.handlePointerEvent(
      PointerScrollEvent(
        position: center,
        kind: PointerDeviceKind.mouse,
        scrollDelta: const Offset(0, -100),
      ),
    );
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();
    expect(changes.last.mode, 'manual');
    expect(changes.last.commandSequence, presentation.commandSequence);
    expect(changes.last.zoomMicros, 1600000);
  });

  testWidgets(
    'emits Delete only while Canvas owns focus and ignores modified Delete',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      final selected = model.widgetIds.firstWhere((id) => id != model.root.id);
      var deleteCalls = 0;
      final selections = <String>[];
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: selected,
            onSelected: selections.add,
            onDeleteSelected: () {
              deleteCalls++;
              return true;
            },
          ),
        ),
      );
      await tester.pump();

      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deleteCalls, 1);

      await tester.sendKeyDownEvent(LogicalKeyboardKey.shiftLeft);
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      await tester.sendKeyUpEvent(LogicalKeyboardKey.shiftLeft);
      expect(deleteCalls, 1);

      FocusManager.instance.primaryFocus?.unfocus();
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deleteCalls, 1, reason: 'an unfocused Canvas must stay inert');

      await tester.tap(find.byKey(ValueKey('canvas-widget-$selected')));
      await tester.pump();
      expect(selections, hasLength(1));
      expect(model.widgetIds, contains(selections.single));
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deleteCalls, 2, reason: 'widget tap restores Canvas focus');
    },
  );

  testWidgets('renders the exact resolved light and dark project theme seed', (
    tester,
  ) async {
    Future<MaterialApp> pumpTheme({
      required String brightness,
      required String seedArgb,
    }) async {
      final json =
          jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
              as Map<String, Object?>;
      final profile = json['profile']! as Map<String, Object?>;
      final theme = profile['theme']! as Map<String, Object?>;
      theme
        ..['definitionId'] = brightness
        ..['seedArgb'] = seedArgb
        ..['brightness'] = brightness
        ..['digestIdentity'] = brightness == 'light' ? 'A' * 64 : 'B' * 64;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      return tester.widget<MaterialApp>(find.byType(MaterialApp));
    }

    final light = await pumpTheme(brightness: 'light', seedArgb: '0xFF123456');
    expect(light.themeMode, ThemeMode.light);
    expect(
      light.theme!.colorScheme,
      ColorScheme.fromSeed(
        seedColor: const Color(0xff123456),
        brightness: Brightness.light,
      ),
    );

    final dark = await pumpTheme(brightness: 'dark', seedArgb: '0xFF654321');
    expect(dark.themeMode, ThemeMode.dark);
    expect(
      dark.darkTheme!.colorScheme,
      ColorScheme.fromSeed(
        seedColor: const Color(0xff654321),
        brightness: Brightness.dark,
      ),
    );
  });

  testWidgets(
    'applies project role overrides and keeps local Text leaves last',
    (tester) async {
      final json = _modelJsonForView();
      final theme =
          (json['profile']! as Map<String, Object?>)['theme']!
              as Map<String, Object?>;
      theme
        ..['colorScheme'] = {'primary': '0xFF102030', 'onSurface': '0xFF405060'}
        ..['textTheme'] = {
          'bodyLarge': {
            'color': {'kind': 'colorScheme', 'role': 'onSurface'},
            'fontSize': 13.0,
            'fontWeight': 'w600',
            'decoration': ['underline', 'lineThrough'],
            'decorationColor': {'kind': 'colorScheme', 'role': 'primary'},
          },
        };
      final properties = _nodeProperties(json, 'flutter.widgets.Text');
      properties
        ..['styleThemeTextStyle'] = {
          'kind': 'themeToken',
          'token': 'material.textTheme.bodyLarge',
        }
        ..['styleFontSize'] = {'kind': 'double', 'value': 19.0}
        ..['styleColor'] = {'kind': 'color', 'argb': '0xFFABCDEF'};
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );

      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );

      final app = tester.widget<MaterialApp>(find.byType(MaterialApp));
      expect(app.theme!.colorScheme.primary, const Color(0xff102030));
      final resolvedRole = app.theme!.textTheme.bodyLarge!;
      expect(resolvedRole.color, const Color(0xff405060));
      expect(resolvedRole.fontSize, 13.0);
      expect(resolvedRole.fontWeight, FontWeight.w600);
      expect(
        resolvedRole.decoration,
        TextDecoration.combine(const [
          TextDecoration.underline,
          TextDecoration.lineThrough,
        ]),
      );
      expect(resolvedRole.decorationColor, const Color(0xff102030));

      final local = tester.widget<Text>(find.text('Hello')).style!;
      expect(
        local.fontSize,
        19.0,
        reason: 'local .fd leaf wins over theme role',
      );
      expect(local.color, const Color(0xffabcdef));
      expect(
        local.fontWeight,
        FontWeight.w600,
        reason: 'unoverridden theme leaf inherits',
      );
    },
  );

  testWidgets('renders actual Flutter widgets and reports a stable selection', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    String? selected;

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (id) => selected = id,
        ),
      ),
    );

    expect(find.text('Hello'), findsOneWidget);
    expect(find.text('World'), findsOneWidget);
    expect(find.text('Action'), findsOneWidget);
    expect(find.byType(Column), findsWidgets);
    expect(find.byType(Row), findsOneWidget);
    expect(find.byType(Padding), findsWidgets);
    expect(find.byType(Center), findsWidgets);

    final column = tester
        .widgetList<Column>(find.byType(Column))
        .singleWhere((candidate) => candidate.spacing == 12.5);
    expect(column.mainAxisSize, MainAxisSize.min);
    expect(column.crossAxisAlignment, CrossAxisAlignment.baseline);
    expect(column.textDirection, TextDirection.ltr);
    expect(column.verticalDirection, VerticalDirection.down);
    expect(column.textBaseline, TextBaseline.alphabetic);

    final row = tester.widget<Row>(find.byType(Row));
    expect(row.mainAxisAlignment, MainAxisAlignment.end);
    expect(row.mainAxisSize, MainAxisSize.min);
    expect(row.crossAxisAlignment, CrossAxisAlignment.baseline);
    expect(row.textDirection, TextDirection.rtl);
    expect(row.verticalDirection, VerticalDirection.up);
    expect(row.textBaseline, TextBaseline.ideographic);
    expect(row.spacing, 4.0);

    final hello = tester.widget<Text>(find.text('Hello'));
    expect(hello.textAlign, TextAlign.center);
    expect(hello.textDirection, TextDirection.ltr);
    expect(hello.softWrap, isFalse);
    expect(hello.overflow, TextOverflow.ellipsis);
    expect(hello.maxLines, 2);
    expect(hello.semanticsLabel, 'Primary greeting');
    expect(hello.semanticsIdentifier, 'primary-greeting');
    expect(hello.textWidthBasis, TextWidthBasis.longestLine);
    expect(hello.selectionColor, const Color(0xff336699));

    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    await tester.tap(find.byKey(const ValueKey('canvas-widget-$textId')));
    await tester.pump();

    expect(selected, textId);
  });

  testWidgets(
    'paints every widget outline dashed and transitions selection to solid',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      String? selectedWidgetId;

      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
            ),
          ),
        ),
      );

      for (final widgetId in model.widgetIds) {
        expect(
          find.byKey(ValueKey('canvas-widget-outline-$widgetId')),
          findsOneWidget,
          reason: 'every decoded Canvas node has a designer outline',
        );
      }

      const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
      const secondTextId = '64260967-f830-4e3c-bbd1-f81cb79db092';
      final unselectedTextPainter = _foregroundPainter(
        tester,
        'canvas-widget-outline-$textId',
      );
      final dashedCanvas = _recordPainter(
        unselectedTextPainter,
        const Size(96, 48),
      );
      expect(_drawRectCount(dashedCanvas), 0);
      final topDashes = _horizontalLinesAt(_drawLines(dashedCanvas), y: 0);
      expect(topDashes, hasLength(greaterThanOrEqualTo(2)));
      expect(topDashes.first.start, Offset.zero);
      expect(topDashes.first.length, greaterThan(0));
      expect(
        topDashes[1].start.dx,
        greaterThan(topDashes.first.end.dx),
        reason: 'separate drawLine commands retain a visible dash gap',
      );
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$textId')),
        findsNothing,
      );

      await tester.tap(find.byKey(const ValueKey('canvas-widget-$textId')));
      await tester.pump();

      expect(selectedWidgetId, textId);
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$textId')),
        findsOneWidget,
      );
      final selectedCanvas = _recordPainter(
        _foregroundPainter(tester, 'canvas-widget-outline-$textId'),
        const Size(96, 48),
      );
      expect(_drawRectCount(selectedCanvas), 1);
      expect(_drawLines(selectedCanvas), isEmpty);

      await tester.tap(
        find.byKey(const ValueKey('canvas-widget-$secondTextId')),
      );
      await tester.pump();

      expect(selectedWidgetId, secondTextId);
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$textId')),
        findsNothing,
      );
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$secondTextId')),
        findsOneWidget,
      );
      expect(
        _drawLines(
          _recordPainter(
            _foregroundPainter(tester, 'canvas-widget-outline-$textId'),
            const Size(96, 48),
          ),
        ),
        isNotEmpty,
        reason: 'the previously selected widget returns to a dashed outline',
      );
    },
  );

  testWidgets('keeps outline stroke and dash screen-stable across zoom', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    final base = CanvasViewportPresentation.fit(model);

    Future<({double stroke, double dash})> metricsAt(int zoomMicros) async {
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
            viewportPresentation: base.copyWith(
              mode: 'manual',
              zoomMicros: zoomMicros,
            ),
          ),
        ),
      );
      await tester.pump();
      final painter = _foregroundPainter(
        tester,
        'canvas-widget-outline-${model.root.id}',
      );
      final lines = _horizontalLinesAt(
        _drawLines(_recordPainter(painter, const Size(390, 844))),
        y: 0,
      );
      expect(lines, isNotEmpty);
      return (stroke: lines.first.paint.strokeWidth, dash: lines.first.length);
    }

    final half = await metricsAt(500000);
    final doubleZoom = await metricsAt(2000000);

    expect(half.stroke * 0.5, closeTo(1, 0.000001));
    expect(doubleZoom.stroke * 2, closeTo(1, 0.000001));
    expect(half.dash * 0.5, closeTo(4, 0.000001));
    expect(doubleZoom.dash * 2, closeTo(4, 0.000001));
  });

  testWidgets('renders empty required Text data without a synthetic fallback', (
    tester,
  ) async {
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    final json = _modelJsonForView();
    final properties = _nodeProperties(json, 'flutter.widgets.Text');
    properties['data'] = {'kind': 'string', 'value': ''};
    final model = CanvasModel.decode(
      Uint8List.fromList(utf8.encode(jsonEncode(json))),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final text = tester.widget<Text>(
      find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$textId')),
            matching: find.byType(Text),
          )
          .first,
    );
    expect(text.data, isEmpty);
  });

  testWidgets('renders exact Padding sides and nullable Center factors', (
    tester,
  ) async {
    const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';
    const centerId = '733ef462-41d7-4849-b780-5d9520666ae4';

    Future<void> pump(Map<String, Object?> json) async {
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );
    }

    Padding renderedPadding() => tester.widget<Padding>(
      find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$paddingId')),
            matching: find.byType(Padding),
          )
          .first,
    );
    Center renderedCenter() => tester.widget<Center>(
      find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$centerId')),
            matching: find.byType(Center),
          )
          .first,
    );

    final explicit = _modelJsonForView();
    final paddingProperties = _nodeProperties(
      explicit,
      'flutter.widgets.Padding',
    );
    paddingProperties['padding'] = {
      'kind': 'edgeInsets',
      'left': 1,
      'top': 2.5,
      'right': 3,
      'bottom': 4.25,
    };
    final centerProperties = _nodeProperties(
      explicit,
      'flutter.widgets.Center',
    );
    centerProperties
      ..['widthFactor'] = {'kind': 'integer', 'value': 0}
      ..['heightFactor'] = {'kind': 'double', 'value': 2.5};

    await pump(explicit);

    expect(
      renderedPadding().padding,
      const EdgeInsets.fromLTRB(1, 2.5, 3, 4.25),
    );
    expect(renderedCenter().widthFactor, 0.0);
    expect(renderedCenter().heightFactor, 2.5);

    final omitted = _modelJsonForView();
    _nodeProperties(omitted, 'flutter.widgets.Center')
      ..remove('widthFactor')
      ..remove('heightFactor');

    await pump(omitted);

    expect(renderedCenter().widthFactor, isNull);
    expect(renderedCenter().heightFactor, isNull);
  });

  testWidgets('renders exact nullable SizedBox dimensions and child', (
    tester,
  ) async {
    const sizedBoxId = '38f49912-8e51-4e62-bd4c-2517ecad4962';

    Map<String, Object?> withSizedBox({
      required Map<String, Object?> properties,
      required Map<String, Object?>? child,
    }) {
      final json = _modelJsonForView();
      final root = json['root']! as Map<String, Object?>;
      final body =
          (root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>;
      body['child'] = <String, Object?>{
        'id': sizedBoxId,
        'type': 'flutter.widgets.SizedBox',
        'properties': properties,
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': child},
        },
      };
      return json;
    }

    Future<SizedBox> pump(Map<String, Object?> json) async {
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );
      return tester.widget<SizedBox>(
        find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$sizedBoxId')),
              matching: find.byType(SizedBox),
            )
            .first,
      );
    }

    final source = _modelJsonForView();
    final sourceRoot = source['root']! as Map<String, Object?>;
    final text = _findNode(sourceRoot, 'flutter.widgets.Text');
    final explicit = await pump(
      withSizedBox(
        properties: {
          'width': {'kind': 'integer', 'value': 120},
          'height': {'kind': 'double', 'value': 48.5},
        },
        child: text,
      ),
    );
    expect(explicit.width, 120.0);
    expect(explicit.height, 48.5);
    expect(explicit.child, isNotNull);
    expect(find.text('Hello'), findsOneWidget);

    final omitted = await pump(withSizedBox(properties: const {}, child: null));
    expect(omitted.width, isNull);
    expect(omitted.height, isNull);
    expect(omitted.child, isNull);
  });

  testWidgets(
    'keeps an empty zero-size SizedBox at zero layout while exposing a selectable target',
    (tester) async {
      const sizedBoxId = '38f49912-8e51-4e62-bd4c-2517ecad4962';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredSizedBox(properties: const {}, child: null),
            ),
          ),
        ),
      );
      String? selectedWidgetId;

      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
            ),
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$sizedBoxId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$sizedBoxId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      final dashed = _recordPainter(
        _foregroundPainter(
          tester,
          'canvas-zero-size-widget-outline-$sizedBoxId',
        ),
        const Size(36, 36),
      );
      expect(_drawRectCount(dashed), 0);
      expect(_drawLines(dashed), isNotEmpty);

      await tester.tap(target);
      await tester.pump();

      expect(selectedWidgetId, sizedBoxId);
      expect(tester.getSize(rendered), Size.zero);
      final selected = _recordPainter(
        _foregroundPainter(
          tester,
          'canvas-zero-size-widget-outline-$sizedBoxId',
        ),
        const Size(36, 36),
      );
      expect(_drawRectCount(selected), 1);
      expect(_drawLines(selected), isEmpty);
    },
  );

  testWidgets(
    'cycles every exactly coincident empty SizedBox through one target',
    (tester) async {
      const sizedBoxIds = [
        '38f49912-8e51-4e62-bd4c-2517ecad4962',
        'f195f817-cf8e-455c-befc-31dd30f874df',
        '7a112f9e-8599-4bca-b37c-02e33268b48c',
      ];
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(jsonEncode(_modelWithEmptySizedBoxSiblings(sizedBoxIds))),
        ),
      );
      String? selectedWidgetId;

      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
            ),
          ),
        ),
      );
      await tester.pump();

      for (final id in sizedBoxIds) {
        expect(
          tester.getSize(find.byKey(ValueKey('canvas-widget-$id'))),
          Size.zero,
        );
        expect(
          find.byKey(ValueKey('canvas-zero-size-widget-target-$id')),
          findsNothing,
        );
      }

      final target = find.byKey(
        const ValueKey(
          'canvas-zero-size-widget-target-group-'
          '38f49912-8e51-4e62-bd4c-2517ecad4962',
        ),
      );
      const cyclingMessage =
          '3 overlapping empty SizedBox widgets. '
          'Activate repeatedly to cycle selection.';
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      expect(find.byTooltip(cyclingMessage), findsOneWidget);
      expect(find.bySemanticsLabel(cyclingMessage), findsOneWidget);

      for (final id in sizedBoxIds) {
        await tester.tap(target);
        await tester.pump();
        expect(selectedWidgetId, id);
      }
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, sizedBoxIds.first);

      for (final id in sizedBoxIds) {
        expect(
          tester.getSize(find.byKey(ValueKey('canvas-widget-$id'))),
          Size.zero,
        );
      }
    },
  );

  testWidgets(
    'does not add a synthetic target to sized or non-empty SizedBox layout',
    (tester) async {
      const sizedBoxId = '38f49912-8e51-4e62-bd4c-2517ecad4962';
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$sizedBoxId'),
      );

      Future<Size> pump(Map<String, Object?> json) async {
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: CanvasModel.decode(
                Uint8List.fromList(utf8.encode(jsonEncode(json))),
              ),
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        );
        await tester.pump();
        return tester.getSize(
          find.byKey(const ValueKey('canvas-widget-$sizedBoxId')),
        );
      }

      final explicitSize = await pump(
        _modelWithCenteredSizedBox(
          properties: const {
            'width': {'kind': 'integer', 'value': 120},
            'height': {'kind': 'double', 'value': 48.5},
          },
          child: null,
        ),
      );
      expect(explicitSize, const Size(120, 48.5));
      expect(target, findsNothing);

      final child = <String, Object?>{
        'id': 'f195f817-cf8e-455c-befc-31dd30f874df',
        'type': 'flutter.widgets.Text',
        'properties': <String, Object?>{
          'data': {'kind': 'string', 'value': 'Non-empty'},
        },
        'slots': <String, Object?>{},
      };
      final childSized = await pump(
        _modelWithCenteredSizedBox(properties: const {}, child: child),
      );
      expect(childSized.width, greaterThan(0));
      expect(childSized.height, greaterThan(0));
      expect(target, findsNothing);
    },
  );

  testWidgets('resolves physical and directional Padding in LTR and RTL', (
    tester,
  ) async {
    const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';

    Finder renderedPadding() => find
        .descendant(
          of: find.byKey(const ValueKey('canvas-widget-$paddingId')),
          matching: find.byType(Padding),
        )
        .first;

    Future<void> expectResolvedOffset({
      required Map<String, Object?> padding,
      required TextDirection direction,
      required double expectedLeft,
    }) async {
      final json = _modelJsonForView();
      _nodeProperties(json, 'flutter.widgets.Padding')['padding'] = padding;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: Directionality(
            textDirection: direction,
            child: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        ),
      );

      final renderPadding = tester.renderObject<RenderPadding>(
        renderedPadding(),
      );
      expect(renderPadding.textDirection, direction);
      final childOffset =
          (renderPadding.child!.parentData! as BoxParentData).offset;
      expect(childOffset, Offset(expectedLeft, 2));
    }

    const physical = <String, Object?>{
      'kind': 'edgeInsets',
      'left': 1,
      'top': 2,
      'right': 3,
      'bottom': 4,
    };
    await expectResolvedOffset(
      padding: physical,
      direction: TextDirection.ltr,
      expectedLeft: 1,
    );
    await expectResolvedOffset(
      padding: physical,
      direction: TextDirection.rtl,
      expectedLeft: 1,
    );

    const directional = <String, Object?>{
      'kind': 'edgeInsetsDirectional',
      'start': 1,
      'top': 2,
      'end': 3,
      'bottom': 4,
    };
    await expectResolvedOffset(
      padding: directional,
      direction: TextDirection.ltr,
      expectedLeft: 1,
    );
    await expectResolvedOffset(
      padding: directional,
      direction: TextDirection.rtl,
      expectedLeft: 3,
    );
  });

  testWidgets('keeps an empty Padding child nullable in the real widget', (
    tester,
  ) async {
    const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';
    final json = _modelJsonForView();
    final root = json['root']! as Map<String, Object?>;
    final padding = _findNode(root, 'flutter.widgets.Padding');
    ((padding['slots']! as Map<String, Object?>)['child']!
            as Map<String, Object?>)['child'] =
        null;

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: CanvasModel.decode(
            Uint8List.fromList(utf8.encode(jsonEncode(json))),
          ),
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final paddingFinder = find
        .descendant(
          of: find.byKey(const ValueKey('canvas-widget-$paddingId')),
          matching: find.byType(Padding),
        )
        .first;
    expect(tester.widget<Padding>(paddingFinder).child, isNull);
    expect(tester.renderObject<RenderPadding>(paddingFinder).child, isNull);
  });

  testWidgets(
    'resolves physical and directional Padding guides in LTR and RTL',
    (tester) async {
      const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';

      Future<List<_RecordedLine>> guideMeasurements({
        required Map<String, Object?> padding,
        required TextDirection direction,
      }) async {
        final json = _modelJsonForView();
        _nodeProperties(json, 'flutter.widgets.Padding')['padding'] = padding;
        await tester.pumpWidget(
          MaterialApp(
            home: Directionality(
              textDirection: direction,
              child: CanvasDocumentView(
                model: CanvasModel.decode(
                  Uint8List.fromList(utf8.encode(jsonEncode(json))),
                ),
                selectedWidgetId: null,
                onSelected: (_) {},
              ),
            ),
          ),
        );
        final painter = _foregroundPainter(
          tester,
          'canvas-padding-guides-$paddingId',
        );
        final lines = _drawLines(_recordPainter(painter, const Size(100, 120)));
        expect(lines, hasLength(12));
        expect(lines.first.paint.color.toARGB32(), 0xffd97706);
        return [
          for (var index = 0; index < lines.length; index += 3) lines[index],
        ];
      }

      const physical = <String, Object?>{
        'kind': 'edgeInsets',
        'left': 10,
        'top': 20,
        'right': 30,
        'bottom': 40,
      };
      final physicalLtr = await guideMeasurements(
        padding: physical,
        direction: TextDirection.ltr,
      );
      final physicalRtl = await guideMeasurements(
        padding: physical,
        direction: TextDirection.rtl,
      );
      expect(_lineEndpoints(physicalLtr), _expectedPaddingGuideEndpoints());
      expect(_lineEndpoints(physicalRtl), _expectedPaddingGuideEndpoints());

      const directional = <String, Object?>{
        'kind': 'edgeInsetsDirectional',
        'start': 10,
        'top': 20,
        'end': 30,
        'bottom': 40,
      };
      final directionalLtr = await guideMeasurements(
        padding: directional,
        direction: TextDirection.ltr,
      );
      final directionalRtl = await guideMeasurements(
        padding: directional,
        direction: TextDirection.rtl,
      );
      expect(_lineEndpoints(directionalLtr), _expectedPaddingGuideEndpoints());
      expect(
        _lineEndpoints(directionalRtl),
        _expectedPaddingGuideEndpoints(left: 30, right: 10),
      );
    },
  );

  testWidgets('paints an empty Padding all-40 guide cross', (tester) async {
    const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';
    final json = _modelJsonForView();
    final padding = _findNode(
      json['root']! as Map<String, Object?>,
      'flutter.widgets.Padding',
    );
    (padding['properties']! as Map<String, Object?>)['padding'] = {
      'kind': 'edgeInsets',
      'left': 40,
      'top': 40,
      'right': 40,
      'bottom': 40,
    };
    ((padding['slots']! as Map<String, Object?>)['child']!
            as Map<String, Object?>)['child'] =
        null;

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: CanvasModel.decode(
            Uint8List.fromList(utf8.encode(jsonEncode(json))),
          ),
          selectedWidgetId: paddingId,
          onSelected: (_) {},
        ),
      ),
    );

    final guideFinder = find.byKey(
      const ValueKey('canvas-padding-guides-$paddingId'),
    );
    expect(guideFinder, findsOneWidget);
    expect(tester.getSize(guideFinder), const Size(80, 80));
    final lines = _drawLines(
      _recordPainter(
        _foregroundPainter(tester, 'canvas-padding-guides-$paddingId'),
        const Size(80, 80),
      ),
    );
    expect(lines, hasLength(12));
    expect(
      _lineEndpoints([
        for (var index = 0; index < lines.length; index += 3) lines[index],
      ]),
      const [
        (Offset(0, 40), Offset(40, 40)),
        (Offset(40, 40), Offset(80, 40)),
        (Offset(40, 0), Offset(40, 40)),
        (Offset(40, 40), Offset(40, 80)),
      ],
    );
  });

  testWidgets('renders expanded Text properties through real Flutter objects', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.expandedTextModelBytesForViewTest(),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final hello = tester.widget<Text>(find.text('Hello'));
    expect(
      hello.locale,
      const Locale.fromSubtags(
        languageCode: 'zh',
        scriptCode: 'Hans',
        countryCode: 'CN',
      ),
    );
    expect(hello.textScaler, isNotNull);
    expect(hello.textScaler!.scale(10), 12.5);
    expect(hello.textHeightBehavior, isNotNull);
    expect(hello.textHeightBehavior!.applyHeightToFirstAscent, isFalse);
    expect(hello.textHeightBehavior!.applyHeightToLastDescent, isFalse);
    expect(
      hello.textHeightBehavior!.leadingDistribution,
      TextLeadingDistribution.even,
    );

    final style = hello.style!;
    expect(style.inherit, isFalse);
    expect(style.color, const Color(0xff102030));
    expect(style.backgroundColor, const Color(0xffe0d0c0));
    expect(style.fontSize, 18.5);
    expect(style.fontWeight, FontWeight.w600);
    expect(style.fontStyle, FontStyle.italic);
    expect(style.letterSpacing, 1.25);
    expect(style.wordSpacing, 2.5);
    expect(style.textBaseline, TextBaseline.ideographic);
    expect(style.height, 1.4);
    expect(style.leadingDistribution, TextLeadingDistribution.proportional);
    expect(
      style.locale,
      const Locale.fromSubtags(
        languageCode: 'en',
        scriptCode: 'Latn',
        countryCode: 'GB',
      ),
    );
    expect(
      style.decoration,
      TextDecoration.combine(const [
        TextDecoration.underline,
        TextDecoration.overline,
        TextDecoration.lineThrough,
      ]),
    );
    expect(style.decorationColor, const Color(0xff112233));
    expect(style.decorationStyle, TextDecorationStyle.wavy);
    expect(style.decorationThickness, 2.25);
    expect(style.debugLabel, 'designer text');
    expect(style.fontFamily, 'packages/design_fonts/Inter');
    expect(style.fontFamilyFallback, const [
      'packages/design_fonts/Noto Sans',
      'packages/design_fonts/Noto Color Emoji',
    ]);
    expect(style.overflow, TextOverflow.fade);

    final strut = hello.strutStyle!;
    expect(strut.fontFamily, 'packages/metric_fonts/Roboto');
    expect(strut.fontFamilyFallback, const [
      'packages/metric_fonts/Noto Sans',
      'packages/metric_fonts/Noto Serif',
    ]);
    expect(strut.fontSize, 16.0);
    expect(strut.height, 1.2);
    expect(strut.leadingDistribution, TextLeadingDistribution.even);
    expect(strut.leading, 0.3);
    expect(strut.fontWeight, FontWeight.w500);
    expect(strut.fontStyle, FontStyle.normal);
    expect(strut.forceStrutHeight, isTrue);
    expect(strut.debugLabel, 'designer strut');
  });

  testWidgets(
    'resolves project theme roles and complex TextStyle values in Flutter',
    (tester) async {
      final model = CanvasModel.decode(
        fixture.complexTextModelBytesForViewTest(),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );

      final context = tester.element(find.text('Hello'));
      final scheme = Theme.of(context).colorScheme;
      final hello = tester.widget<Text>(find.text('Hello'));
      expect(hello.selectionColor, scheme.primary);
      final style = hello.style!;
      expect(
        style.fontSize,
        21.0,
        reason: 'local override wins over bodyLarge',
      );
      expect(style.foreground, isNotNull);
      expect(style.foreground!.color.toARGB32(), scheme.secondary.toARGB32());
      expect(style.foreground!.style, PaintingStyle.stroke);
      expect(style.foreground!.strokeWidth, 2.5);
      expect(style.foreground!.strokeCap, StrokeCap.round);
      expect(style.foreground!.strokeJoin, StrokeJoin.bevel);
      expect(style.foreground!.filterQuality, FilterQuality.medium);
      expect(style.foreground!.maskFilter, isNotNull);
      expect(style.background!.color.toARGB32(), 0x22112233);
      expect(style.decorationColor, scheme.error);
      expect(style.shadows, hasLength(2));
      expect(style.shadows!.first.color.toARGB32(), scheme.shadow.toARGB32());
      expect(style.shadows!.first.offset, const Offset(-1.25, 2.5));
      expect(style.shadows!.first.blurRadius, 4.0);
      expect(style.fontFeatures, hasLength(2));
      expect(style.fontFeatures!.map((value) => value.feature), [
        'liga',
        'kern',
      ]);
      expect(style.fontFeatures!.map((value) => value.value), [1, 0]);
      expect(style.fontVariations, hasLength(2));
      expect(style.fontVariations!.map((value) => value.axis), [
        'wght',
        'wdth',
      ]);
      expect(style.fontVariations!.map((value) => value.value), [700.0, 100.0]);
    },
  );

  testWidgets(
    'preserves theme TextStyle inherit when the local leaf is omitted',
    (tester) async {
      final json = _modelJsonForView();
      final properties = _nodeProperties(json, 'flutter.widgets.Text');
      properties
        ..['styleThemeTextStyle'] = {
          'kind': 'themeToken',
          'token': 'material.textTheme.bodyLarge',
        }
        ..['styleFontSize'] = {'kind': 'double', 'value': 19.0}
        ..remove('styleInherit');
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );

      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(
            textTheme: const TextTheme(
              bodyLarge: TextStyle(inherit: false, fontSize: 11),
            ),
          ),
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );

      final style = tester.widget<Text>(find.text('Hello')).style!;
      expect(style.inherit, isFalse);
      expect(style.fontSize, 19.0);
    },
  );

  testWidgets('preserves explicitly empty complex TextStyle lists', (
    tester,
  ) async {
    final json = _modelJsonForView();
    final properties = _nodeProperties(json, 'flutter.widgets.Text');
    properties
      ..['styleShadows'] = {'kind': 'shadowList', 'items': <Object?>[]}
      ..['styleFontFeatures'] = {
        'kind': 'fontFeatureList',
        'items': <Object?>[],
      }
      ..['styleFontVariations'] = {
        'kind': 'fontVariationList',
        'items': <Object?>[],
      };
    final model = CanvasModel.decode(
      Uint8List.fromList(utf8.encode(jsonEncode(json))),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final style = tester.widget<Text>(find.text('Hello')).style!;
    expect(style.shadows, isEmpty);
    expect(style.fontFeatures, isEmpty);
    expect(style.fontVariations, isEmpty);
  });

  testWidgets('omits Text composite objects when no composite field exists', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final world = tester.widget<Text>(find.text('World'));
    expect(world.style, isNull);
    expect(world.strutStyle, isNull);
    expect(world.locale, isNull);
    expect(world.textScaler, isNull);
    expect(world.textHeightBehavior, isNull);
  });

  testWidgets(
    'resolves only a terminal Row or Column append zone from native coordinates',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      CanvasDropResolver? resolver;

      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );

      expect(resolver, isNotNull);
      const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final row = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$rowId')),
      );
      // The fixture Row is RTL, so its semantic append edge is the left edge.
      final point = Offset(row.left + 1, row.center.dy);
      final xMicros = ((point.dx - surface.left) / surface.width * 1000000)
          .round();
      final yMicros = ((point.dy - surface.top) / surface.height * 1000000)
          .round();

      final target = resolver!(xMicros, yMicros);
      expect(target, isNotNull);
      expect(target!.parentWidgetId, rowId);
      expect(target.slotName, 'children');
      expect(target.insertionIndex, 1);
      expect(target.zone, isNotNull);
      expect(target.zone!.isEmpty, isFalse);
      expect(
        target.zone!.rightMicros - target.zone!.leftMicros,
        lessThan(250000),
        reason: 'the RTL Row exposes only its concise terminal append band',
      );
      expect(resolver!(-1, yMicros), isNull);
      expect(resolver!(xMicros, 1000001), isNull);

      const columnId = '4c04dc44-7381-4ca8-826f-8c23bc2351d1';
      final column = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$columnId')),
      );
      expect(column.width, lessThan(model.profile.logicalWidth));
      final outsideScaledColumn = Offset(column.right + 8, column.bottom - 1);
      final outsideX =
          ((outsideScaledColumn.dx - surface.left) / surface.width * 1000000)
              .round();
      final outsideY =
          ((outsideScaledColumn.dy - surface.top) / surface.height * 1000000)
              .round();
      expect(
        resolver!(outsideX, outsideY)?.parentWidgetId,
        isNot(columnId),
        reason: 'FittedBox scaling must not enlarge the Column hit rectangle',
      );
    },
  );

  testWidgets(
    'keeps newly inserted empty Row and Column children targets hittable',
    (tester) async {
      CanvasDropResolver? resolver;

      for (final type in const [
        'flutter.widgets.Row',
        'flutter.widgets.Column',
      ]) {
        final json = _modelJsonForView();
        final root = json['root']! as Map<String, Object?>;
        final container = _findNode(root, type);
        final containerId = container['id']! as String;
        (container['properties']! as Map<String, Object?>)
          ..remove('textDirection')
          ..remove('verticalDirection');
        final children =
            (container['slots']! as Map<String, Object?>)['children']!
                as Map<String, Object?>;
        children['children'] = <Object?>[];
        ((root['slots']! as Map<String, Object?>)['body']!
                as Map<String, Object?>)['child'] =
            container;

        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: CanvasModel.decode(
                Uint8List.fromList(utf8.encode(jsonEncode(json))),
              ),
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();

        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final rect = tester.getRect(
          find.byKey(ValueKey('canvas-widget-$containerId')),
        );
        final point = type == 'flutter.widgets.Row'
            ? Offset(rect.right - 1, rect.center.dy)
            : Offset(rect.center.dx, rect.bottom - 1);
        final target = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
        expect(target?.parentWidgetId, containerId, reason: type);
        expect(target?.slotName, 'children', reason: type);
        expect(target?.insertionIndex, 0, reason: type);
        expect(target?.zone?.isEmpty, isFalse, reason: type);
        expect(target!.zone!.leftMicros, greaterThanOrEqualTo(0));
        expect(target.zone!.topMicros, greaterThanOrEqualTo(0));
        expect(target.zone!.rightMicros, lessThanOrEqualTo(1000000));
        expect(target.zone!.bottomMicros, lessThanOrEqualTo(1000000));
      }
    },
  );

  testWidgets(
    'resolves native DnD coordinates after manual zoom and scroll transforms',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      final presentation = CanvasViewportPresentation.fit(model).copyWith(
        mode: 'manual',
        zoomMicros: 1500000,
        horizontalScrollMicros: 250000,
        verticalScrollMicros: 100000,
      );
      CanvasDropResolver? resolver;
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: SizedBox(
              width: 500,
              height: 400,
              child: CanvasDocumentView(
                model: model,
                selectedWidgetId: null,
                onSelected: (_) {},
                viewportPresentation: presentation,
                onDropResolverChanged: (value) => resolver = value,
              ),
            ),
          ),
        ),
      );
      await tester.pump();

      const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final root = tester.getRect(
        find.byKey(ValueKey('canvas-widget-${model.root.id}')),
      );
      final row = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$rowId')),
      );
      expect(
        root.height,
        allOf(
          greaterThan(model.profile.logicalHeight * 1.4),
          lessThan(model.profile.logicalHeight * 1.6),
        ),
        reason: 'the transformed Flutter viewport must use the manual zoom',
      );
      expect(
        root.top,
        lessThan(surface.top),
        reason: 'the normalized vertical scroll must translate the Canvas',
      );

      final visibleRow = row.intersect(surface);
      expect(visibleRow.isEmpty, isFalse);
      final point = Offset(visibleRow.left + 1, visibleRow.center.dy);
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(target, isNotNull);
      expect(target!.parentWidgetId, rowId);
      expect(target.slotName, 'children');
      expect(target.insertionIndex, 1);
      expect(target.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'exposes an empty Column as an insertion target at manual 75 percent',
    (tester) async {
      final json = _modelJsonForView();
      final root = json['root']! as Map<String, Object?>;
      final column = _findNode(root, 'flutter.widgets.Column');
      final columnId = column['id']! as String;
      final children =
          (column['slots']! as Map<String, Object?>)['children']!
              as Map<String, Object?>;
      children['children'] = <Object?>[];
      ((root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child'] =
          column;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      final presentation = CanvasViewportPresentation.fit(
        model,
      ).copyWith(mode: 'manual', zoomMicros: 750000);
      final metrics = <CanvasViewportMetrics>[];
      CanvasDropResolver? resolver;

      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: SizedBox(
              width: 500,
              height: 500,
              child: CanvasDocumentView(
                model: model,
                selectedWidgetId: columnId,
                onSelected: (_) {},
                viewportPresentation: presentation,
                onViewportMetricsChanged: metrics.add,
                onDropResolverChanged: (value) => resolver = value,
              ),
            ),
          ),
        ),
      );
      await tester.pump();

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final columnRect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-$columnId')),
      );
      final selectionRect = tester.getRect(
        find.byKey(ValueKey('canvas-selection-outline-$columnId')),
      );
      expect(
        selectionRect,
        columnRect,
        reason: 'the selection affordance must not change real widget layout',
      );
      expect(surface.contains(columnRect.center), isTrue);
      final point = columnRect.center;
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );

      expect(metrics.last.effectiveScaleMicros, 750000);
      expect(target, isNotNull);
      expect(target!.parentWidgetId, columnId);
      expect(target.slotName, 'children');
      expect(target.insertionIndex, 0);
      expect(target.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'resolves Scaffold body and gives its compact empty FAB zone precedence',
    (tester) async {
      CanvasDropResolver? resolver;

      Future<void> pump(Map<String, Object?> json) async {
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: CanvasModel.decode(
                Uint8List.fromList(utf8.encode(jsonEncode(json))),
              ),
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
      }

      CanvasDropTarget? resolveAt(Rect surface, Offset point) => resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );

      final bothEmpty = _modelJsonForView();
      final emptyRoot = bothEmpty['root']! as Map<String, Object?>;
      final rootId = emptyRoot['id']! as String;
      final emptySlots = emptyRoot['slots']! as Map<String, Object?>;
      (emptySlots['body']! as Map<String, Object?>)['child'] = null;
      emptySlots['floatingActionButton'] = <String, Object?>{
        'kind': 'single',
        'child': null,
      };
      await pump(bothEmpty);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final rootRect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-$rootId')),
      );
      final bodyTarget = resolveAt(surface, rootRect.center);
      expect(bodyTarget, isNotNull);
      expect(bodyTarget!.parentWidgetId, rootId);
      expect(bodyTarget.slotName, 'body');
      expect(bodyTarget.insertionIndex, 0);

      final fabTarget = resolveAt(
        surface,
        rootRect.bottomRight - const Offset(1, 1),
      );
      expect(fabTarget, isNotNull);
      expect(fabTarget!.parentWidgetId, rootId);
      expect(fabTarget.slotName, 'floatingActionButton');
      expect(fabTarget.insertionIndex, 0);
      expect(
        fabTarget.zone!.rightMicros - fabTarget.zone!.leftMicros,
        lessThan(bodyTarget.zone!.rightMicros - bodyTarget.zone!.leftMicros),
        reason: 'the FAB target must stay concise and win the body overlap',
      );

      final occupiedBody = _modelJsonForView();
      final occupiedBodyRoot = occupiedBody['root']! as Map<String, Object?>;
      final occupiedBodySlots =
          occupiedBodyRoot['slots']! as Map<String, Object?>;
      final bodyText = _findNode(occupiedBodyRoot, 'flutter.widgets.Text');
      (occupiedBodySlots['body']! as Map<String, Object?>)['child'] = bodyText;
      occupiedBodySlots['floatingActionButton'] = <String, Object?>{
        'kind': 'single',
        'child': null,
      };
      await pump(occupiedBody);
      final occupiedBodySurface = tester.getRect(
        find.byType(CanvasDocumentView),
      );
      final occupiedBodyRootRect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-${occupiedBodyRoot['id']}')),
      );
      expect(
        resolveAt(
          occupiedBodySurface,
          occupiedBodyRootRect.bottomRight - const Offset(1, 1),
        )?.slotName,
        'floatingActionButton',
        reason: 'an occupied body must not hide an empty FAB target',
      );

      final occupiedFab = _modelJsonForView();
      final occupiedFabRoot = occupiedFab['root']! as Map<String, Object?>;
      final occupiedFabSlots =
          occupiedFabRoot['slots']! as Map<String, Object?>;
      final fabText = _findNode(occupiedFabRoot, 'flutter.widgets.Text');
      (occupiedFabSlots['body']! as Map<String, Object?>)['child'] = null;
      occupiedFabSlots['floatingActionButton'] = <String, Object?>{
        'kind': 'single',
        'child': fabText,
      };
      await pump(occupiedFab);
      expect(
        resolver!(500000, 500000)?.slotName,
        'body',
        reason: 'an occupied FAB must leave the empty body target available',
      );
    },
  );

  testWidgets('resolves only empty Padding and Center child slots', (
    tester,
  ) async {
    CanvasDropResolver? resolver;

    Future<void> pump(Map<String, Object?> json) async {
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();
    }

    for (final type in const [
      'flutter.widgets.Padding',
      'flutter.widgets.Center',
    ]) {
      final empty = _modelJsonForView();
      final root = empty['root']! as Map<String, Object?>;
      final container = _findNode(root, type);
      final containerId = container['id']! as String;
      ((container['slots']! as Map<String, Object?>)['child']!
              as Map<String, Object?>)['child'] =
          null;
      ((root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child'] =
          container;
      await pump(empty);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final rect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-$containerId')),
      );
      final point = rect.center;
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(target?.parentWidgetId, containerId, reason: type);
      expect(target?.slotName, 'child', reason: type);
      expect(target?.insertionIndex, 0, reason: type);

      final occupied = _modelJsonForView();
      final occupiedRoot = occupied['root']! as Map<String, Object?>;
      final occupiedContainer = _findNode(occupiedRoot, type);
      ((occupiedRoot['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child'] =
          occupiedContainer;
      await pump(occupied);
      final occupiedSurface = tester.getRect(find.byType(CanvasDocumentView));
      final occupiedRect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-$containerId')),
      );
      final occupiedPoint = occupiedRect.center;
      expect(
        resolver!(
          ((occupiedPoint.dx - occupiedSurface.left) /
                  occupiedSurface.width *
                  1000000)
              .round(),
          ((occupiedPoint.dy - occupiedSurface.top) /
                  occupiedSurface.height *
                  1000000)
              .round(),
        ),
        isNull,
        reason: '$type.child is occupied',
      );
    }
  });

  testWidgets('resolves only an empty SizedBox child slot', (tester) async {
    const sizedBoxId = '38f49912-8e51-4e62-bd4c-2517ecad4962';
    CanvasDropResolver? resolver;

    Future<void> pump({required bool occupied}) async {
      final json = _modelJsonForView();
      final root = json['root']! as Map<String, Object?>;
      final body =
          (root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>;
      final child = occupied ? _findNode(root, 'flutter.widgets.Text') : null;
      body['child'] = <String, Object?>{
        'id': sizedBoxId,
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'double', 'value': 120.0},
          'height': {'kind': 'double', 'value': 80.0},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': child},
        },
      };
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();
    }

    await pump(occupied: false);
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final rect = tester.getRect(
      find.byKey(const ValueKey('canvas-widget-$sizedBoxId')),
    );
    final point = rect.center;
    final target = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(target?.parentWidgetId, sizedBoxId);
    expect(target?.slotName, 'child');
    expect(target?.insertionIndex, 0);

    await pump(occupied: true);
    final occupiedSurface = tester.getRect(find.byType(CanvasDocumentView));
    final occupiedRect = tester.getRect(
      find.byKey(const ValueKey('canvas-widget-$sizedBoxId')),
    );
    final occupiedPoint = occupiedRect.center;
    expect(
      resolver!(
        ((occupiedPoint.dx - occupiedSurface.left) /
                occupiedSurface.width *
                1000000)
            .round(),
        ((occupiedPoint.dy - occupiedSurface.top) /
                occupiedSurface.height *
                1000000)
            .round(),
      ),
      isNull,
    );
  });

  testWidgets(
    'resolves exact existing-widget placement and paints a thin amber marker',
    (tester) async {
      CanvasMovePreviewResolver? moveResolver;
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      const sourceId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
      const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';

      Future<void> pump({CanvasDropTarget? target}) async {
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
              dropHoverTarget: target,
              dropIndicatorKind: CanvasDropIndicatorKind.widgetMove,
              onMovePreviewResolverChanged: (value) => moveResolver = value,
            ),
          ),
        );
        await tester.pump();
      }

      await pump();
      final target = moveResolver!(sourceId, rowId, 'children', 1);
      expect(target, isNotNull);
      expect(target!.parentWidgetId, rowId);
      expect(target.slotName, 'children');
      expect(target.insertionIndex, 1);
      expect(target.zone, isNotNull);
      expect(target.zone!.isEmpty, isFalse);
      expect(
        target.zone!.rightMicros - target.zone!.leftMicros,
        lessThan(100000),
        reason: 'a list move uses a concise insertion marker, not full fill',
      );

      await pump(target: target);
      final marker = tester.widget<DecoratedBox>(
        find.byKey(const ValueKey('canvas-widget-move-preview-zone')),
      );
      final decoration = marker.decoration as BoxDecoration;
      expect(decoration.color, const Color(0x24D29A17));
      expect(decoration.border!.top.color, const Color(0xffc58b08));
      expect(
        tester
            .widgetList<Semantics>(
              find.ancestor(
                of: find.byKey(
                  const ValueKey('canvas-widget-move-preview-zone'),
                ),
                matching: find.byType(Semantics),
              ),
            )
            .any(
              (widget) =>
                  widget.properties.label ==
                  'Flutter widget move target for children',
            ),
        isTrue,
      );
      expect(
        find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
        findsNothing,
        reason: 'move preview must not be confused with palette insertion',
      );

      await tester.pumpWidget(const SizedBox.shrink());
      expect(
        moveResolver,
        isNull,
        reason: 'disposing the view clears geometry',
      );
    },
  );
}

Map<String, Object?> _modelJsonForView() =>
    jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
        as Map<String, Object?>;

Map<String, Object?> _modelWithCenteredSizedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '38f49912-8e51-4e62-bd4c-2517ecad4962',
          'type': 'flutter.widgets.SizedBox',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithEmptySizedBoxSiblings(List<String> widgetIds) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Column',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'children': <String, Object?>{
        'kind': 'list',
        'children': <Object?>[
          for (final id in widgetIds)
            <String, Object?>{
              'id': id,
              'type': 'flutter.widgets.SizedBox',
              'properties': <String, Object?>{},
              'slots': <String, Object?>{
                'child': <String, Object?>{'kind': 'single', 'child': null},
              },
            },
        ],
      },
    },
  };
  return model;
}

Map<String, Object?> _nodeProperties(Map<String, Object?> model, String type) =>
    _findNode(model['root']! as Map<String, Object?>, type)['properties']!
        as Map<String, Object?>;

Map<String, Object?> _findNode(Map<String, Object?> node, String type) {
  if (node['type'] == type) {
    return node;
  }
  final slots = node['slots']! as Map<String, Object?>;
  for (final rawSlot in slots.values) {
    final slot = rawSlot! as Map<String, Object?>;
    final child = slot['child'];
    if (child is Map<String, Object?>) {
      final match = _findNodeOrNull(child, type);
      if (match != null) {
        return match;
      }
    }
    final children = slot['children'];
    if (children is List<Object?>) {
      for (final child in children.cast<Map<String, Object?>>()) {
        final match = _findNodeOrNull(child, type);
        if (match != null) {
          return match;
        }
      }
    }
  }
  throw StateError('Fixture does not contain $type.');
}

Map<String, Object?>? _findNodeOrNull(Map<String, Object?> node, String type) {
  try {
    return _findNode(node, type);
  } on StateError {
    return null;
  }
}

CustomPainter _foregroundPainter(WidgetTester tester, String customPaintKey) =>
    tester
        .widget<CustomPaint>(find.byKey(ValueKey(customPaintKey)))
        .foregroundPainter!;

TestRecordingCanvas _recordPainter(CustomPainter painter, Size size) {
  final canvas = TestRecordingCanvas();
  painter.paint(canvas, size);
  return canvas;
}

int _drawRectCount(TestRecordingCanvas canvas) => canvas.invocations
    .where((recorded) => recorded.invocation.memberName == #drawRect)
    .length;

typedef _RecordedLine = ({Offset start, Offset end, Paint paint});

List<_RecordedLine> _drawLines(TestRecordingCanvas canvas) => [
  for (final recorded in canvas.invocations)
    if (recorded.invocation.memberName == #drawLine)
      (
        start: recorded.invocation.positionalArguments[0] as Offset,
        end: recorded.invocation.positionalArguments[1] as Offset,
        paint: recorded.invocation.positionalArguments[2] as Paint,
      ),
];

List<_RecordedLine> _horizontalLinesAt(
  List<_RecordedLine> lines, {
  required double y,
}) => [
  for (final line in lines)
    if ((line.start.dy - y).abs() < 0.000001 &&
        (line.end.dy - y).abs() < 0.000001 &&
        line.end.dx > line.start.dx)
      line,
];

extension on _RecordedLine {
  double get length => (end - start).distance;
}

List<(Offset, Offset)> _lineEndpoints(List<_RecordedLine> lines) => [
  for (final line in lines) (line.start, line.end),
];

List<(Offset, Offset)> _expectedPaddingGuideEndpoints({
  double left = 10,
  double right = 30,
}) {
  final innerLeft = left;
  final innerRight = 100 - right;
  final guideX = (innerLeft + innerRight) / 2;
  const innerTop = 20.0;
  const innerBottom = 80.0;
  const guideY = (innerTop + innerBottom) / 2;
  return [
    (Offset.zero.translate(0, guideY), Offset(innerLeft, guideY)),
    (Offset(innerRight, guideY), const Offset(100, guideY)),
    (Offset(guideX, 0), Offset(guideX, innerTop)),
    (Offset(guideX, innerBottom), Offset(guideX, 120)),
  ];
}
