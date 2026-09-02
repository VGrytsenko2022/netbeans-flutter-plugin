import 'dart:convert';
import 'dart:ui' as ui;

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';

import 'canvas_model_test.dart' as fixture;

final Uint8List _viewPng8 = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAA'
  'AARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/'
  'j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=',
);

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
      canvasScaffoldAppBarDropSlot,
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
      same(canvasScaffoldAppBarDropSlot),
    );
    expect(canvasDropSlotsForWidgetType('flutter.material.AppBar'), const [
      canvasAppBarLeadingDropSlot,
      canvasAppBarTitleDropSlot,
      canvasAppBarActionsDropSlot,
      canvasAppBarFlexibleSpaceDropSlot,
      canvasAppBarBottomDropSlot,
    ]);
    final textSource = CanvasPaletteDragSource(
      token: 'text-source',
      widgetType: 'flutter.widgets.Text',
      traits: const {},
    );
    final appBarSource = CanvasPaletteDragSource(
      token: 'appbar-source',
      widgetType: 'flutter.material.AppBar',
      traits: const {canvasPreferredSizeWidgetTrait},
    );
    expect(canvasScaffoldAppBarDropSlot.acceptsSource(textSource), isFalse);
    expect(canvasAppBarBottomDropSlot.acceptsSource(textSource), isFalse);
    expect(canvasScaffoldAppBarDropSlot.acceptsSource(appBarSource), isTrue);
    expect(canvasAppBarBottomDropSlot.acceptsSource(appBarSource), isTrue);
    expect(canvasAppBarLeadingDropSlot.acceptsSource(textSource), isTrue);
    expect(canvasAppBarTitleDropSlot.acceptsSource(textSource), isTrue);
    expect(canvasAppBarActionsDropSlot.acceptsSource(textSource), isTrue);
    expect(canvasAppBarFlexibleSpaceDropSlot.acceptsSource(textSource), isTrue);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Padding'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Align'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Center'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Container'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.ConstrainedBox'),
      const [canvasEmptyChildDropSlot],
    );
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.UnconstrainedBox'),
      const [canvasEmptyChildDropSlot],
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.LimitedBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.OverflowBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.FractionallySizedBox'),
      const [canvasEmptyChildDropSlot],
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.FittedBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.SizedBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.AspectRatio'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Baseline'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.IntrinsicHeight'),
      const [canvasEmptyChildDropSlot],
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Opacity'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Stack'), const [
      canvasStackChildrenAppendDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.ListView'), const [
      canvasChildrenAppendDropSlot,
    ]);
    expect(
      canvasStackChildrenAppendDropSlot.zonePlacement,
      CanvasDropZonePlacement.fullNode,
    );
    expect(
      canvasDropSlotsForWidgetType('flutter.material.ElevatedButton'),
      const [canvasEmptyChildDropSlot],
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Icon'), isEmpty);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Image'), isEmpty);
    expect(canvasDropSlotsForWidgetType('flutter.material.TextField'), isEmpty);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Text'), isEmpty);
    expect(
      canvasEmptyChildDropSlot.accepts(currentChildCount: 0, insertionIndex: 1),
      isFalse,
    );
    expect(canvasChildrenAppendDropSlot.modelSlotKind, 'list');
    expect(canvasScaffoldBodyDropSlot.modelSlotKind, 'single');
    expect(canvasExpandedWrapDropSlot.wrapsExistingChild, isTrue);
    expect(canvasFlexibleWrapDropSlot, same(canvasExpandedWrapDropSlot));
    expect(canvasExpandedWrapDropSlot.insertionIndexFor(2), isNull);
    expect(
      canvasExpandedWrapDropSlot.accepts(
        currentChildCount: 2,
        insertionIndex: 1,
      ),
      isTrue,
    );
    expect(
      canvasExpandedWrapDropSlot.accepts(
        currentChildCount: 2,
        insertionIndex: 2,
      ),
      isFalse,
    );
    expect(canvasDropSlotsForWidgetType(canvasExpandedWidgetType), isEmpty);
    expect(canvasDropSlotsForWidgetType(canvasFlexibleWidgetType), isEmpty);
    expect(canvasDropSlotsForWidgetType(canvasSpacerWidgetType), isEmpty);

    final spacerSource = CanvasPaletteDragSource(
      token: 'spacer-source',
      widgetType: canvasSpacerWidgetType,
      traits: const {},
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Row',
        slotName: 'children',
        currentChildCount: 0,
        insertionIndex: 0,
        source: spacerSource,
      ),
      isTrue,
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Column',
        slotName: 'children',
        currentChildCount: 1,
        insertionIndex: 1,
        source: spacerSource,
      ),
      isTrue,
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Row',
        slotName: 'children',
        currentChildCount: 1,
        insertionIndex: 0,
        source: spacerSource,
      ),
      isFalse,
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Stack',
        slotName: 'children',
        currentChildCount: 0,
        insertionIndex: 0,
        source: spacerSource,
      ),
      isFalse,
    );
  });

  test('closes the 30-source by 29-destination compatibility matrix', () {
    const sourceTypes = {
      'flutter.material.Scaffold',
      'flutter.material.AppBar',
      'flutter.material.ElevatedButton',
      'flutter.material.TextField',
      'flutter.widgets.Align',
      'flutter.widgets.AspectRatio',
      'flutter.widgets.Baseline',
      'flutter.widgets.IntrinsicHeight',
      'flutter.widgets.Column',
      'flutter.widgets.Row',
      'flutter.widgets.Wrap',
      'flutter.widgets.Padding',
      'flutter.widgets.Center',
      'flutter.widgets.ConstrainedBox',
      'flutter.widgets.UnconstrainedBox',
      'flutter.widgets.LimitedBox',
      'flutter.widgets.OverflowBox',
      'flutter.widgets.Container',
      'flutter.widgets.Expanded',
      'flutter.widgets.Flexible',
      'flutter.widgets.Spacer',
      'flutter.widgets.FittedBox',
      'flutter.widgets.FractionallySizedBox',
      'flutter.widgets.Opacity',
      'flutter.widgets.Icon',
      'flutter.widgets.Image',
      'flutter.widgets.ListView',
      'flutter.widgets.SizedBox',
      'flutter.widgets.Stack',
      'flutter.widgets.Text',
    };
    final destinations =
        <({String parentType, CanvasDropSlotSemantics slot})>[];
    for (final type in sourceTypes) {
      destinations.addAll([
        for (final slot in canvasDropSlotsForWidgetType(type))
          (parentType: type, slot: slot),
      ]);
    }
    expect(sourceTypes, hasLength(30));
    expect(destinations, hasLength(29));

    var accepted = 0;
    var rejected = 0;
    for (final widgetType in sourceTypes) {
      final source = CanvasPaletteDragSource(
        token: widgetType,
        widgetType: widgetType,
        traits: canvasWidgetTraitsForType(widgetType),
      );
      for (final destination in destinations) {
        if (canvasDropTargetAcceptsSource(
          parentWidgetType: destination.parentType,
          slotName: destination.slot.slotName,
          currentChildCount: isCanvasFlexParentDataWidgetType(widgetType)
              ? 1
              : 0,
          insertionIndex: 0,
          source: source,
        )) {
          accepted++;
        } else {
          rejected++;
        }
      }
    }
    expect(accepted, 737);
    expect(rejected, 133);
    expect(accepted + rejected, 870);
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

  testWidgets('gates all Canvas input behind a visible exact fence barrier', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(800, 600);
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    var interactions = 0;
    var deletes = 0;
    final selections = <String>[];
    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: selections.add,
        onInteraction: () => interactions++,
        onDeleteSelected: () {
          deletes++;
          return true;
        },
        interactionInputSynchronized: false,
      ),
    );

    expect(
      find.byKey(const ValueKey('canvas-interaction-fence-overlay')),
      findsOneWidget,
    );
    expect(find.text('Synchronizing input…'), findsOneWidget);
    await tester.tapAt(const Offset(8, 300));
    await tester.tapAt(const Offset(400, 300));
    await tester.sendKeyEvent(LogicalKeyboardKey.delete);

    expect(interactions, 0);
    expect(selections, isEmpty);
    expect(deletes, 0);

    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: selections.add,
        onInteraction: () => interactions++,
        onDeleteSelected: () {
          deletes++;
          return true;
        },
        interactionInputSynchronized: true,
      ),
    );
    expect(
      find.byKey(const ValueKey('canvas-interaction-fence-overlay')),
      findsNothing,
    );
    await tester.tapAt(const Offset(8, 300));
    await tester.tapAt(const Offset(400, 300));
    await tester.sendKeyEvent(LogicalKeyboardKey.delete);

    expect(interactions, 2);
    expect(deletes, 1);
  });

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
      expect(
        deleteCalls,
        0,
        reason: 'rendering the Canvas must not claim keyboard focus',
      );

      await tester.sendKeyDownEvent(LogicalKeyboardKey.shiftLeft);
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      await tester.sendKeyUpEvent(LogicalKeyboardKey.shiftLeft);
      expect(deleteCalls, 0);

      FocusManager.instance.primaryFocus?.unfocus();
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deleteCalls, 0, reason: 'an unfocused Canvas must stay inert');

      await tester.tap(find.byKey(ValueKey('canvas-widget-$selected')));
      await tester.pump();
      expect(selections, hasLength(1));
      expect(model.widgetIds, contains(selections.single));
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deleteCalls, 1, reason: 'widget tap gives the Canvas focus');
    },
  );

  testWidgets(
    'does not steal external focus on render and claims it on Canvas pointer down',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      final externalFocusNode = FocusNode(debugLabel: 'external-owner');
      addTearDown(externalFocusNode.dispose);
      var interactions = 0;

      Widget buildApp() {
        return MaterialApp(
          home: Column(
            children: [
              Focus(
                focusNode: externalFocusNode,
                child: const SizedBox(width: 1, height: 1),
              ),
              Expanded(
                child: CanvasDocumentView(
                  model: model,
                  selectedWidgetId: null,
                  onSelected: (_) {},
                  onInteraction: () => interactions++,
                ),
              ),
            ],
          ),
        );
      }

      await tester.pumpWidget(buildApp());
      externalFocusNode.requestFocus();
      await tester.pump();
      expect(externalFocusNode.hasFocus, isTrue);

      await tester.pumpWidget(buildApp());
      await tester.pump();
      expect(
        externalFocusNode.hasFocus,
        isTrue,
        reason: 'a Canvas rebuild must preserve the current external owner',
      );

      await tester.tap(
        find.byKey(const ValueKey('canvas-interaction-surface')),
      );
      await tester.pump();
      expect(externalFocusNode.hasFocus, isFalse);
      expect(interactions, 1);
    },
  );

  testWidgets(
    'opens inline Text.data editing only by selected Text double-click or F2',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
      const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';

      Future<void> pump({required String? selected, required bool enabled}) =>
          tester.pumpWidget(
            MaterialApp(
              home: CanvasDocumentView(
                model: model,
                selectedWidgetId: selected,
                onSelected: (_) {},
                inlineTextEditEnabled: enabled,
              ),
            ),
          );

      await pump(selected: null, enabled: true);
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      expect(find.byType(TextField), findsNothing);

      await pump(selected: rowId, enabled: true);
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$rowId')),
      );
      await tester.pump();
      expect(find.byType(TextField), findsNothing);

      await pump(selected: textId, enabled: false);
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      expect(find.byType(TextField), findsNothing);

      await pump(selected: textId, enabled: true);
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      expect(
        find.byKey(const ValueKey('canvas-inline-text-editor-$textId')),
        findsOneWidget,
      );
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pump();

      await tester.tap(find.byKey(const ValueKey('canvas-widget-$textId')));
      await tester.pump(kDoubleTapTimeout);
      await tester.sendKeyEvent(LogicalKeyboardKey.f2);
      await tester.pump();
      expect(
        find.byKey(const ValueKey('canvas-inline-text-editor-$textId')),
        findsOneWidget,
      );
    },
  );

  testWidgets('keeps IME preedit local and emits one final Unicode commit', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    final commits = <({String id, String text, bool compositionObserved})>[];
    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: textId,
          onSelected: (_) {},
          inlineTextEditEnabled: true,
          onInlineTextCommit: (id, text, compositionObserved) {
            commits.add((
              id: id,
              text: text,
              compositionObserved: compositionObserved,
            ));
            return true;
          },
        ),
      ),
    );
    await _doubleTap(
      tester,
      find.byKey(const ValueKey('canvas-widget-$textId')),
    );
    await tester.pump();

    tester.testTextInput.updateEditingValue(
      const TextEditingValue(
        text: 'に',
        selection: TextSelection.collapsed(offset: 1),
        composing: TextRange(start: 0, end: 1),
      ),
    );
    await tester.pump();
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();
    expect(commits, isEmpty, reason: 'a composing preedit is never an intent');
    expect(find.byType(TextField), findsOneWidget);

    const committedText = '日本語 👩🏽‍💻';
    tester.testTextInput.updateEditingValue(
      const TextEditingValue(
        text: committedText,
        selection: TextSelection.collapsed(offset: committedText.length),
      ),
    );
    await tester.pump();
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();

    expect(commits, [
      (id: textId, text: committedText, compositionObserved: true),
    ]);
    expect(find.byType(TextField), findsNothing);
  });

  testWidgets('reports direct typing without a fabricated composition', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    final compositions = <bool>[];
    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: textId,
          onSelected: (_) {},
          inlineTextEditEnabled: true,
          onInlineTextCommit: (_, _, compositionObserved) {
            compositions.add(compositionObserved);
            return true;
          },
        ),
      ),
    );
    await tester.tap(find.byKey(const ValueKey('canvas-widget-$textId')));
    await tester.pump(kDoubleTapTimeout);
    await tester.sendKeyEvent(LogicalKeyboardKey.f2);
    await tester.pump();
    await tester.enterText(find.byType(TextField), 'plain typing');
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();

    expect(compositions, [false]);
  });

  testWidgets('preserves rejected and over-limit inline drafts in the editor', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    var commitCalls = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: textId,
          onSelected: (_) {},
          inlineTextEditEnabled: true,
          onInlineTextCommit: (_, _, _) {
            commitCalls++;
            return false;
          },
        ),
      ),
    );
    await _doubleTap(
      tester,
      find.byKey(const ValueKey('canvas-widget-$textId')),
    );
    await tester.pump();
    final editor = find.byType(TextField);
    await tester.enterText(editor, 'unsaved draft');
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();

    expect(commitCalls, 1);
    expect(editor, findsOneWidget);
    expect(tester.widget<TextField>(editor).controller!.text, 'unsaved draft');
    expect(
      find.text(
        'The host rejected this edit because its Canvas authority changed.',
      ),
      findsOneWidget,
    );

    final overScalarLimit = List<String>.filled(
      maximumInlineTextUnicodeScalars + 1,
      'a',
    ).join();
    tester.widget<TextField>(editor).controller!.text = overScalarLimit;
    await tester.pump();
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();

    expect(
      commitCalls,
      1,
      reason: 'invalid local text never becomes an intent',
    );
    expect(editor, findsOneWidget);
    expect(tester.widget<TextField>(editor).controller!.text, overScalarLimit);
    expect(
      find.text(
        'Text exceeds $maximumInlineTextUnicodeScalars Unicode scalar values.',
      ),
      findsOneWidget,
    );
  });

  testWidgets(
    'Escape waits for composition and Delete never deletes the widget',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
      var deletes = 0;
      var commits = 0;
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: textId,
            onSelected: (_) {},
            inlineTextEditEnabled: true,
            onDeleteSelected: () {
              deletes++;
              return true;
            },
            onInlineTextCommit: (_, _, _) {
              commits++;
              return true;
            },
          ),
        ),
      );
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      tester.testTextInput.updateEditingValue(
        const TextEditingValue(
          text: '文',
          selection: TextSelection.collapsed(offset: 1),
          composing: TextRange(start: 0, end: 1),
        ),
      );
      await tester.pump();

      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      await tester.pump();
      expect(find.byType(TextField), findsOneWidget);
      expect(deletes, 0);
      expect(commits, 0);

      tester.testTextInput.updateEditingValue(
        const TextEditingValue(
          text: '文',
          selection: TextSelection.collapsed(offset: 1),
        ),
      );
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pump();
      expect(find.byType(TextField), findsNothing);
      expect(deletes, 0);
      expect(commits, 0);
    },
  );

  testWidgets('selection, revision, fence, and capability loss cancel drafts', (
    tester,
  ) async {
    var model = CanvasModel.decode(fixture.modelBytesForViewTest());
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';
    var selected = textId;
    var synchronized = true;
    var enabled = true;
    var commits = 0;
    late StateSetter rebuild;
    await tester.pumpWidget(
      MaterialApp(
        home: StatefulBuilder(
          builder: (context, setState) {
            rebuild = setState;
            return CanvasDocumentView(
              model: model,
              selectedWidgetId: selected,
              onSelected: (_) {},
              interactionInputSynchronized: synchronized,
              inlineTextEditEnabled: enabled,
              onInlineTextCommit: (_, _, _) {
                commits++;
                return true;
              },
            );
          },
        ),
      ),
    );

    Future<void> openEditor() async {
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      expect(find.byType(TextField), findsOneWidget);
    }

    await openEditor();
    rebuild(() => selected = rowId);
    await tester.pump();
    expect(find.byType(TextField), findsNothing);

    rebuild(() => selected = textId);
    await tester.pump();
    await openEditor();
    rebuild(() => synchronized = false);
    await tester.pump();
    expect(find.byType(TextField), findsNothing);

    rebuild(() => synchronized = true);
    await tester.pump();
    await openEditor();
    final json =
        jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
            as Map<String, Object?>;
    json['logicalRevisionId'] = (json['logicalRevisionId']! as int) + 1;
    rebuild(
      () => model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      ),
    );
    await tester.pump();
    expect(find.byType(TextField), findsNothing);

    await openEditor();
    rebuild(() => enabled = false);
    await tester.pump();
    expect(find.byType(TextField), findsNothing);
    expect(commits, 0);
  });

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

  testWidgets('applies component colors and keeps local widget colors last', (
    tester,
  ) async {
    final json =
        jsonDecode(
              utf8.decode(
                fixture.elevatedButtonModelBytesForViewTest(
                  properties: {
                    'onPressed': {'kind': 'callbackPresence'},
                    'styleForegroundColor': {
                      'kind': 'color',
                      'argb': '0xFFABCDEF',
                    },
                  },
                ),
              ),
            )
            as Map<String, Object?>;
    final theme =
        (json['profile']! as Map<String, Object?>)['theme']!
            as Map<String, Object?>;
    theme['components'] = {
      'scaffold.backgroundColor': {'kind': 'argb', 'argb': '0xFF102030'},
      'appBar.backgroundColor': {'kind': 'argb', 'argb': '0xFF203040'},
      'appBar.foregroundColor': {'kind': 'colorScheme', 'role': 'onPrimary'},
      'appBar.shadowColor': {'kind': 'argb', 'argb': '0xFF304050'},
      'appBar.surfaceTintColor': {'kind': 'argb', 'argb': '0xFF405060'},
      'icon.color': {'kind': 'argb', 'argb': '0xFF506070'},
      'elevatedButton.backgroundColor.default': {
        'kind': 'argb',
        'argb': '0xFF607080',
      },
      'elevatedButton.backgroundColor.disabled': {
        'kind': 'argb',
        'argb': '0xFF708090',
      },
      'elevatedButton.backgroundColor.pressed': {
        'kind': 'argb',
        'argb': '0xFF8090A0',
      },
      'elevatedButton.foregroundColor.default': {
        'kind': 'argb',
        'argb': '0xFF90A0B0',
      },
      'elevatedButton.overlayColor.pressed': {
        'kind': 'argb',
        'argb': '0x66A0B0C0',
      },
    };
    final model = CanvasModel.decode(
      Uint8List.fromList(utf8.encode(jsonEncode(json))),
    );

    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final app = tester.widget<MaterialApp>(find.byType(MaterialApp));
    final resolvedTheme = app.theme!;
    expect(resolvedTheme.scaffoldBackgroundColor, const Color(0xff102030));
    expect(resolvedTheme.appBarTheme.backgroundColor, const Color(0xff203040));
    expect(
      resolvedTheme.appBarTheme.foregroundColor,
      resolvedTheme.colorScheme.onPrimary,
    );
    expect(resolvedTheme.appBarTheme.shadowColor, const Color(0xff304050));
    expect(resolvedTheme.appBarTheme.surfaceTintColor, const Color(0xff405060));
    expect(resolvedTheme.iconTheme.color, const Color(0xff506070));

    final componentStyle = resolvedTheme.elevatedButtonTheme.style!;
    expect(
      componentStyle.backgroundColor!.resolve(const <WidgetState>{}),
      const Color(0xff607080),
    );
    expect(
      componentStyle.backgroundColor!.resolve(const {
        WidgetState.disabled,
        WidgetState.pressed,
      }),
      const Color(0xff708090),
      reason: 'disabled has priority over pressed',
    );
    expect(
      componentStyle.overlayColor!.resolve(const {
        WidgetState.disabled,
        WidgetState.pressed,
      }),
      isNull,
      reason:
          'an absent disabled override must fall through to framework, '
          'never to the pressed component override',
    );

    expect(
      tester
          .widgetList<Scaffold>(find.byType(Scaffold))
          .map((widget) => widget.backgroundColor),
      contains(const Color(0xffffffff)),
      reason: 'local Scaffold color wins over the component theme',
    );
    expect(
      tester
          .widget<ElevatedButton>(find.byType(ElevatedButton))
          .style!
          .foregroundColor!
          .resolve(const <WidgetState>{}),
      const Color(0xffabcdef),
      reason: 'local ButtonStyle color wins over the component theme',
    );
  });

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

  testWidgets('renders every reviewed Scaffold scalar with framework parity', (
    tester,
  ) async {
    const scaffoldId = '6e88bff4-8d73-48aa-92b5-87aa3344f6a7';
    const themedScrim = Color(0xff5a1234);
    final json = _modelJsonForView();
    final root = json['root']! as Map<String, Object?>;
    root['properties'] = <String, Object?>{
      'floatingActionButtonLocation': {
        'kind': 'string',
        'value': 'miniCenterDocked',
      },
      'floatingActionButtonAnimator': {
        'kind': 'string',
        'value': 'noAnimation',
      },
      'persistentFooterAlignment': {'kind': 'string', 'value': 'bottomStart'},
      'onDrawerChanged': {'kind': 'callbackPresence'},
      'onEndDrawerChanged': {'kind': 'callbackPresence'},
      'backgroundColor': {'kind': 'color', 'argb': '0xFF102030'},
      'resizeToAvoidBottomInset': {'kind': 'boolean', 'value': false},
      'primary': {'kind': 'boolean', 'value': false},
      'drawerDragStartBehavior': {
        'kind': 'enum',
        'type': 'DragStartBehavior',
        'value': 'down',
      },
      'extendBody': {'kind': 'boolean', 'value': true},
      'drawerBarrierDismissible': {'kind': 'boolean', 'value': false},
      'extendBodyBehindAppBar': {'kind': 'boolean', 'value': true},
      'drawerScrimColor': {
        'kind': 'themeToken',
        'token': 'material.colorScheme.scrim',
      },
      'drawerEdgeDragWidth': {'kind': 'double', 'value': 24.5},
      'drawerEnableOpenDragGesture': {'kind': 'boolean', 'value': false},
      'endDrawerEnableOpenDragGesture': {'kind': 'boolean', 'value': false},
      'restorationId': {'kind': 'string', 'value': 'home-scaffold'},
    };
    final model = CanvasModel.decode(
      Uint8List.fromList(utf8.encode(jsonEncode(json))),
    );

    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData.from(
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xff6750a4),
            scrim: themedScrim,
          ),
        ),
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final scaffold = tester.widget<Scaffold>(
      find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$scaffoldId')),
            matching: find.byType(Scaffold),
          )
          .first,
    );
    expect(
      scaffold.floatingActionButtonLocation,
      same(FloatingActionButtonLocation.miniCenterDocked),
    );
    expect(
      scaffold.floatingActionButtonAnimator,
      same(FloatingActionButtonAnimator.noAnimation),
    );
    expect(
      scaffold.persistentFooterAlignment,
      AlignmentDirectional.bottomStart,
    );
    expect(scaffold.onDrawerChanged, isNotNull);
    expect(scaffold.onEndDrawerChanged, isNotNull);
    expect(scaffold.backgroundColor, const Color(0xff102030));
    expect(scaffold.resizeToAvoidBottomInset, isFalse);
    expect(scaffold.primary, isFalse);
    expect(scaffold.drawerDragStartBehavior, DragStartBehavior.down);
    expect(scaffold.extendBody, isTrue);
    expect(scaffold.drawerBarrierDismissible, isFalse);
    expect(scaffold.extendBodyBehindAppBar, isTrue);
    expect(scaffold.drawerScrimColor, themedScrim);
    expect(scaffold.drawerEdgeDragWidth, 24.5);
    expect(scaffold.drawerEnableOpenDragGesture, isFalse);
    expect(scaffold.endDrawerEnableOpenDragGesture, isFalse);
    expect(scaffold.restorationId, 'home-scaffold');
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

  testWidgets(
    'renders real Align defaults, factors, and extrapolated physical layout',
    (tester) async {
      const alignId = 'b51246d4-4e44-4d7c-91bc-a0892df4a341';
      final fixedChild = <String, Object?>{
        'id': '2985a65c-e7ef-46b7-b942-93f72ab18930',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'integer', 'value': 40},
          'height': {'kind': 'integer', 'value': 20},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };

      Finder alignFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$alignId')),
            matching: find.byType(Align),
          )
          .first;

      Future<RenderPositionedBox> pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredAlign(
                  properties: properties,
                  child: fixedChild,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderPositionedBox>(alignFinder());
      }

      final omitted = await pump(const {});
      final omittedWidget = tester.widget<Align>(alignFinder());
      expect(omittedWidget.alignment, Alignment.center);
      expect(omittedWidget.widthFactor, isNull);
      expect(omittedWidget.heightFactor, isNull);
      expect(omitted.child!.size, const Size(40, 20));
      expect(omitted.size.width, greaterThan(omitted.child!.size.width));
      expect(omitted.size.height, greaterThan(omitted.child!.size.height));
      expect(
        (omitted.child!.parentData! as BoxParentData).offset,
        Offset(
          (omitted.size.width - omitted.child!.size.width) / 2,
          (omitted.size.height - omitted.child!.size.height) / 2,
        ),
      );

      final extrapolated = await pump({
        'alignment': _viewAlignment(horizontal: 2, vertical: -2),
        'widthFactor': {'kind': 'integer', 'value': 2},
        'heightFactor': {'kind': 'double', 'value': 3.0},
      });
      final explicitWidget = tester.widget<Align>(alignFinder());
      expect(explicitWidget.alignment, const Alignment(2, -2));
      expect(explicitWidget.widthFactor, 2);
      expect(explicitWidget.heightFactor, 3);
      expect(extrapolated.child!.size, const Size(40, 20));
      expect(extrapolated.size, const Size(80, 60));
      expect(
        (extrapolated.child!.parentData! as BoxParentData).offset,
        const Offset(60, -20),
      );
      expect(find.byType(AnimatedAlign), findsNothing);
    },
  );

  testWidgets('resolves Align directional start in LTR and RTL', (
    tester,
  ) async {
    const alignId = 'b51246d4-4e44-4d7c-91bc-a0892df4a341';
    final child = <String, Object?>{
      'id': '2985a65c-e7ef-46b7-b942-93f72ab18930',
      'type': 'flutter.widgets.SizedBox',
      'properties': <String, Object?>{
        'width': {'kind': 'integer', 'value': 40},
        'height': {'kind': 'integer', 'value': 20},
      },
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': null},
      },
    };

    Future<({Offset offset, double remainingWidth, TextDirection direction})>
    render(String locale) async {
      final json = _modelWithCenteredAlign(
        properties: {
          'alignment': _viewAlignment(
            basis: 'directional',
            horizontal: -1,
            vertical: 0,
          ),
        },
        child: child,
      );
      (json['profile']! as Map<String, Object?>)['locale'] = locale;
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
      await tester.pump();
      final node = find.byKey(const ValueKey('canvas-widget-$alignId'));
      final align = tester.widget<Align>(
        find.descendant(of: node, matching: find.byType(Align)).first,
      );
      expect(align.alignment, const AlignmentDirectional(-1, 0));
      final render = tester.renderObject<RenderPositionedBox>(
        find.descendant(of: node, matching: find.byType(Align)).first,
      );
      return (
        offset: (render.child!.parentData! as BoxParentData).offset,
        remainingWidth: render.size.width - render.child!.size.width,
        direction: render.textDirection!,
      );
    }

    final ltr = await render('en-US');
    expect(ltr.direction, TextDirection.ltr);
    expect(ltr.offset.dx, 0);
    expect(ltr.offset.dy, greaterThan(0));

    final rtl = await render('ar-SA');
    expect(rtl.direction, TextDirection.rtl);
    expect(rtl.remainingWidth, greaterThan(0));
    expect(rtl.offset.dx, rtl.remainingWidth);
    expect(rtl.offset.dy, greaterThan(0));
  });

  testWidgets(
    'keeps empty and factor-zero Align selectable and exposes empty child DnD',
    (tester) async {
      const alignId = 'b51246d4-4e44-4d7c-91bc-a0892df4a341';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;

      final emptyModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredAlign(
                properties: const {
                  'widthFactor': {'kind': 'integer', 'value': 1},
                  'heightFactor': {'kind': 'integer', 'value': 1},
                },
                child: null,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: emptyModel,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$alignId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$alignId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, alignId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, alignId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);

      final occupiedChild = <String, Object?>{
        'id': '2985a65c-e7ef-46b7-b942-93f72ab18930',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'integer', 'value': 40},
          'height': {'kind': 'integer', 'value': 20},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };
      final factorZeroModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredAlign(
                properties: const {
                  'widthFactor': {'kind': 'integer', 'value': 0},
                  'heightFactor': {'kind': 'integer', 'value': 1},
                },
                child: occupiedChild,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: factorZeroModel,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      expect(tester.getSize(rendered), const Size(0, 20));
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      final align = tester.widget<Align>(
        find.descendant(of: rendered, matching: find.byType(Align)).first,
      );
      expect(align.child, isNotNull);
      expect(align.widthFactor, 0);
      expect(align.heightFactor, 1);
    },
  );

  testWidgets(
    'renders real FractionallySizedBox constraints, overflow, and alignment',
    (tester) async {
      const widgetId = '403df8b2-b244-4e7c-9ff1-29ba070206b6';
      final fixedChild = <String, Object?>{
        'id': '941b5560-c22e-4c5d-8180-e2985e4123a7',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'integer', 'value': 40},
          'height': {'kind': 'integer', 'value': 20},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };

      Finder widgetFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(FractionallySizedBox),
          )
          .first;

      Future<RenderFractionallySizedOverflowBox> pump(
        Map<String, Object?> properties,
      ) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredFractionallySizedBox(
                  properties: properties,
                  child: fixedChild,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderFractionallySizedOverflowBox>(
          widgetFinder(),
        );
      }

      final omitted = await pump(const {});
      final omittedWidget = tester.widget<FractionallySizedBox>(widgetFinder());
      expect(omittedWidget.alignment, Alignment.center);
      expect(omittedWidget.widthFactor, isNull);
      expect(omittedWidget.heightFactor, isNull);
      expect(omitted.size, const Size(200, 100));
      expect(omitted.child!.size, const Size(200, 100));
      expect((omitted.child!.parentData! as BoxParentData).offset, Offset.zero);

      final fractional = await pump({
        'widthFactor': {'kind': 'double', 'value': 0.5},
        'heightFactor': {'kind': 'double', 'value': 0.25},
      });
      expect(fractional.size, const Size(200, 100));
      expect(fractional.child!.size, const Size(100, 25));
      expect(
        (fractional.child!.parentData! as BoxParentData).offset,
        const Offset(50, 37.5),
      );

      final overflow = await pump({
        'widthFactor': {'kind': 'double', 'value': 1.5},
        'heightFactor': {'kind': 'integer', 'value': 2},
      });
      expect(overflow.size, const Size(200, 100));
      expect(overflow.child!.size, const Size(300, 200));
      expect(
        (overflow.child!.parentData! as BoxParentData).offset,
        const Offset(-50, -50),
      );

      final extrapolated = await pump({
        'alignment': _viewAlignment(horizontal: 2, vertical: -2),
        'widthFactor': {'kind': 'double', 'value': 0.5},
        'heightFactor': {'kind': 'double', 'value': 0.25},
      });
      final explicitWidget = tester.widget<FractionallySizedBox>(
        widgetFinder(),
      );
      expect(explicitWidget.alignment, const Alignment(2, -2));
      expect(explicitWidget.widthFactor, 0.5);
      expect(explicitWidget.heightFactor, 0.25);
      expect(
        (extrapolated.child!.parentData! as BoxParentData).offset,
        const Offset(150, -37.5),
      );
      expect(find.byType(Align), findsNothing);
    },
  );

  testWidgets(
    'resolves FractionallySizedBox directional start in LTR and RTL',
    (tester) async {
      const widgetId = '403df8b2-b244-4e7c-9ff1-29ba070206b6';
      final child = <String, Object?>{
        'id': '941b5560-c22e-4c5d-8180-e2985e4123a7',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{},
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };

      Future<({Offset offset, double remainingWidth, TextDirection direction})>
      render(String locale) async {
        final json = _modelWithCenteredFractionallySizedBox(
          properties: {
            'alignment': _viewAlignment(
              basis: 'directional',
              horizontal: -1,
              vertical: 0,
            ),
            'widthFactor': {'kind': 'double', 'value': 0.5},
            'heightFactor': {'kind': 'double', 'value': 0.5},
          },
          child: child,
        );
        (json['profile']! as Map<String, Object?>)['locale'] = locale;
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
        await tester.pump();
        final node = find.byKey(const ValueKey('canvas-widget-$widgetId'));
        final finder = find
            .descendant(of: node, matching: find.byType(FractionallySizedBox))
            .first;
        final widget = tester.widget<FractionallySizedBox>(finder);
        expect(widget.alignment, const AlignmentDirectional(-1, 0));
        final render = tester.renderObject<RenderFractionallySizedOverflowBox>(
          finder,
        );
        return (
          offset: (render.child!.parentData! as BoxParentData).offset,
          remainingWidth: render.size.width - render.child!.size.width,
          direction: render.textDirection!,
        );
      }

      final ltr = await render('en-US');
      expect(ltr.direction, TextDirection.ltr);
      expect(ltr.offset.dx, 0);
      expect(ltr.offset.dy, 25);

      final rtl = await render('ar-SA');
      expect(rtl.direction, TextDirection.rtl);
      expect(rtl.remainingWidth, 100);
      expect(rtl.offset.dx, rtl.remainingWidth);
      expect(rtl.offset.dy, 25);
    },
  );

  testWidgets(
    'keeps zero-size FractionallySizedBox selectable and exposes child DnD',
    (tester) async {
      const widgetId = '403df8b2-b244-4e7c-9ff1-29ba070206b6';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredFractionallySizedBox(
                properties: const {},
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'renders real FittedBox defaults and every reviewed fit and clip value',
    (tester) async {
      const widgetId = 'b587a092-9a65-420a-9d1c-e127cc752f8d';
      final child = <String, Object?>{
        'id': 'feea5045-2de2-4d34-b08d-79e5ee710f7e',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'integer', 'value': 40},
          'height': {'kind': 'integer', 'value': 20},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };

      Finder fittedFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(FittedBox),
          )
          .first;

      Future<RenderFittedBox> pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredFittedBox(
                  properties: properties,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderFittedBox>(fittedFinder());
      }

      final omitted = await pump(const {});
      final omittedWidget = tester.widget<FittedBox>(fittedFinder());
      expect(omittedWidget.fit, BoxFit.contain);
      expect(omittedWidget.alignment, Alignment.center);
      expect(omittedWidget.clipBehavior, Clip.none);
      expect(omitted.size, const Size(200, 100));
      expect(omitted.child!.size, const Size(40, 20));

      const fits = <String, BoxFit>{
        'fill': BoxFit.fill,
        'contain': BoxFit.contain,
        'cover': BoxFit.cover,
        'fitWidth': BoxFit.fitWidth,
        'fitHeight': BoxFit.fitHeight,
        'none': BoxFit.none,
        'scaleDown': BoxFit.scaleDown,
      };
      for (final entry in fits.entries) {
        final render = await pump({
          'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': entry.key},
        });
        expect(tester.widget<FittedBox>(fittedFinder()).fit, entry.value);
        expect(render.child!.size, const Size(40, 20), reason: entry.key);
      }

      const clips = <String, Clip>{
        'none': Clip.none,
        'hardEdge': Clip.hardEdge,
        'antiAlias': Clip.antiAlias,
        'antiAliasWithSaveLayer': Clip.antiAliasWithSaveLayer,
      };
      for (final entry in clips.entries) {
        await pump({
          'alignment': _viewAlignment(horizontal: 2, vertical: -2),
          'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': entry.key},
        });
        final widget = tester.widget<FittedBox>(fittedFinder());
        expect(widget.alignment, const Alignment(2, -2));
        expect(widget.clipBehavior, entry.value);
      }
    },
  );

  testWidgets(
    'resolves FittedBox directional alignment and applies overflow clipping',
    (tester) async {
      const widgetId = 'b587a092-9a65-420a-9d1c-e127cc752f8d';
      const childId = 'feea5045-2de2-4d34-b08d-79e5ee710f7e';

      Future<({Offset offset, TextDirection direction})> directional(
        String locale,
      ) async {
        final json = _modelWithCenteredFittedBox(
          properties: {
            'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'none'},
            'alignment': _viewAlignment(
              basis: 'directional',
              horizontal: -1,
              vertical: 0,
            ),
          },
          child: <String, Object?>{
            'id': childId,
            'type': 'flutter.widgets.SizedBox',
            'properties': <String, Object?>{
              'width': {'kind': 'integer', 'value': 40},
              'height': {'kind': 'integer', 'value': 20},
            },
            'slots': <String, Object?>{},
          },
        );
        (json['profile']! as Map<String, Object?>)['locale'] = locale;
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
        await tester.pump();
        final node = find.byKey(const ValueKey('canvas-widget-$widgetId'));
        final finder = find
            .descendant(of: node, matching: find.byType(FittedBox))
            .first;
        final render = tester.renderObject<RenderFittedBox>(finder);
        final childRect = MatrixUtils.transformRect(
          render.child!.getTransformTo(render),
          Offset.zero & render.child!.size,
        );
        return (offset: childRect.topLeft, direction: render.textDirection!);
      }

      final ltr = await directional('en-US');
      expect(ltr.direction, TextDirection.ltr);
      expect(ltr.offset, const Offset(0, 40));
      final rtl = await directional('ar-SA');
      expect(rtl.direction, TextDirection.rtl);
      expect(rtl.offset, const Offset(160, 40));

      Future<({Clip clip, Rect childRect, Size size})> overflow(
        String clip,
      ) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredFittedBox(
                  properties: {
                    'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'none'},
                    'clipBehavior': {
                      'kind': 'enum',
                      'type': 'Clip',
                      'value': clip,
                    },
                  },
                  child: <String, Object?>{
                    'id': childId,
                    'type': 'flutter.widgets.SizedBox',
                    'properties': <String, Object?>{
                      'width': {'kind': 'integer', 'value': 300},
                      'height': {'kind': 'integer', 'value': 200},
                    },
                    'slots': <String, Object?>{},
                  },
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final render = tester.renderObject<RenderFittedBox>(
          find
              .descendant(
                of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
                matching: find.byType(FittedBox),
              )
              .first,
        );
        return (
          clip: render.clipBehavior,
          childRect: MatrixUtils.transformRect(
            render.child!.getTransformTo(render),
            Offset.zero & render.child!.size,
          ),
          size: render.size,
        );
      }

      final unclipped = await overflow('none');
      expect(unclipped.clip, Clip.none);
      expect(unclipped.size, const Size(200, 100));
      expect(unclipped.childRect, const Rect.fromLTWH(-50, -50, 300, 200));
      final clipped = await overflow('hardEdge');
      expect(clipped.clip, Clip.hardEdge);
      expect(clipped.size, const Size(200, 100));
      expect(clipped.childRect, const Rect.fromLTWH(-50, -50, 300, 200));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps an empty zero-size FittedBox selectable and exposes child DnD',
    (tester) async {
      const widgetId = 'b587a092-9a65-420a-9d1c-e127cc752f8d';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredFittedBox(
                properties: const {},
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'renders finite, unbounded, expanding, and mixed ConstrainedBox axes',
    (tester) async {
      const widgetId = '93d89766-af04-4fee-af57-a56c4ed5e37c';
      final child = _viewSizedBoxNode(
        'bacaf0a6-b27a-4c98-9f88-8c8eb91597c1',
        width: 50,
        height: 30,
      );

      Finder constrainedFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(ConstrainedBox),
          )
          .first;

      Future<({BoxConstraints constraints, Size size, Size? childSize})> pump({
        required Map<String, Object?> constraints,
        required Map<String, Object?>? child,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredConstrainedBox(
                  constraints: constraints,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final widget = tester.widget<ConstrainedBox>(constrainedFinder());
        final render = tester.renderObject<RenderConstrainedBox>(
          constrainedFinder(),
        );
        expect(tester.takeException(), isNull);
        return (
          constraints: widget.constraints,
          size: render.size,
          childSize: render.child?.size,
        );
      }

      final finite = await pump(
        constraints: _viewBoxConstraints(10, 80, 20, 90),
        child: child,
      );
      expect(
        finite.constraints,
        const BoxConstraints(
          minWidth: 10,
          maxWidth: 80,
          minHeight: 20,
          maxHeight: 90,
        ),
      );
      expect(finite.size, const Size(50, 30));
      expect(finite.childSize, const Size(50, 30));

      final finiteEmpty = await pump(
        constraints: _viewBoxConstraints(10, 80, 20, 90),
        child: null,
      );
      expect(finiteEmpty.size, const Size(10, 20));
      expect(finiteEmpty.childSize, isNull);

      final unbounded = await pump(
        constraints: _viewBoxConstraints(0, null, 0, null),
        child: child,
      );
      expect(unbounded.constraints.minWidth, 0);
      expect(unbounded.constraints.maxWidth, double.infinity);
      expect(unbounded.constraints.minHeight, 0);
      expect(unbounded.constraints.maxHeight, double.infinity);
      expect(unbounded.size, const Size(50, 30));

      final unboundedEmpty = await pump(
        constraints: _viewBoxConstraints(0, null, 0, null),
        child: null,
      );
      expect(unboundedEmpty.size, Size.zero);

      final expanding = await pump(
        constraints: _viewBoxConstraints(null, null, null, null),
        child: child,
      );
      expect(expanding.constraints.minWidth, double.infinity);
      expect(expanding.constraints.maxWidth, double.infinity);
      expect(expanding.constraints.minHeight, double.infinity);
      expect(expanding.constraints.maxHeight, double.infinity);
      expect(expanding.size, const Size(200, 100));
      expect(expanding.childSize, const Size(200, 100));

      final expandingWidth = await pump(
        constraints: _viewBoxConstraints(null, null, 10, 40),
        child: child,
      );
      expect(expandingWidth.size, const Size(200, 30));
      expect(expandingWidth.childSize, const Size(200, 30));

      final expandingHeight = await pump(
        constraints: _viewBoxConstraints(25, 70, null, null),
        child: child,
      );
      expect(expandingHeight.size, const Size(50, 100));
      expect(expandingHeight.childSize, const Size(50, 100));

      final mixedEmpty = await pump(
        constraints: _viewBoxConstraints(0, null, 12, 40),
        child: null,
      );
      expect(mixedEmpty.size, const Size(0, 12));
    },
  );

  testWidgets(
    'lets finite parent constraints enforce ConstrainedBox invalid boundary',
    (tester) async {
      const widgetId = '93d89766-af04-4fee-af57-a56c4ed5e37c';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredConstrainedBox(
                constraints: _viewBoxConstraints(300, 400, 150, 200),
                child: _viewSizedBoxNode(
                  'bacaf0a6-b27a-4c98-9f88-8c8eb91597c1',
                  width: 500,
                  height: 500,
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final finder = find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(ConstrainedBox),
          )
          .first;
      final render = tester.renderObject<RenderConstrainedBox>(finder);
      expect(
        render.additionalConstraints,
        const BoxConstraints(
          minWidth: 300,
          maxWidth: 400,
          minHeight: 150,
          maxHeight: 200,
        ),
      );
      expect(
        render.constraints,
        const BoxConstraints(
          minWidth: 0,
          maxWidth: 200,
          minHeight: 0,
          maxHeight: 100,
        ),
      );
      expect(render.size, const Size(200, 100));
      expect(render.child!.size, const Size(200, 100));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps empty default ConstrainedBox layout zero while exposing bounded DnD',
    (tester) async {
      const widgetId = '93d89766-af04-4fee-af57-a56c4ed5e37c';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredConstrainedBox(
                constraints: _viewBoxConstraints(0, null, 0, null),
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real UnconstrainedBox defaults, both retained axes, and every clip',
    (tester) async {
      const widgetId = '67247867-79f8-470f-b109-59971aa7392c';
      final child = _viewSizedBoxNode(
        'ff6ed199-2782-4be8-873b-e61ed6422664',
        width: 300,
        height: 200,
      );
      var overflowObserved = false;

      Finder widgetFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(UnconstrainedBox),
          )
          .first;
      Finder renderFinder() => find
          .descendant(
            of: widgetFinder(),
            matching: find.byType(ConstraintsTransformBox),
          )
          .first;

      Future<({UnconstrainedBox widget, RenderConstraintsTransformBox render})>
      pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredUnconstrainedBox(
                  properties: properties,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final overflow = tester.takeException();
        if (overflow != null) {
          overflowObserved = true;
          expect(overflow, isA<FlutterError>());
          expect(
            overflow.toString(),
            contains('A RenderConstraintsTransformBox overflowed'),
          );
        }
        return (
          widget: tester.widget<UnconstrainedBox>(widgetFinder()),
          render: tester.renderObject<RenderConstraintsTransformBox>(
            renderFinder(),
          ),
        );
      }

      final omitted = await pump(const {});
      expect(overflowObserved, isTrue);
      expect(omitted.widget.textDirection, isNull);
      expect(omitted.widget.alignment, Alignment.center);
      expect(omitted.widget.constrainedAxis, isNull);
      expect(omitted.widget.clipBehavior, Clip.none);
      expect(omitted.render.size, const Size(200, 100));
      expect(omitted.render.child!.size, const Size(300, 200));
      expect(
        (omitted.render.child!.parentData! as BoxParentData).offset,
        const Offset(-50, -50),
      );

      final horizontal = await pump(const {
        'constrainedAxis': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
      });
      expect(horizontal.widget.constrainedAxis, Axis.horizontal);
      expect(horizontal.render.size, const Size(200, 100));
      expect(horizontal.render.child!.size, const Size(200, 200));
      expect(
        (horizontal.render.child!.parentData! as BoxParentData).offset,
        const Offset(0, -50),
      );

      final vertical = await pump(const {
        'constrainedAxis': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'vertical',
        },
      });
      expect(vertical.widget.constrainedAxis, Axis.vertical);
      expect(vertical.render.size, const Size(200, 100));
      expect(vertical.render.child!.size, const Size(300, 100));
      expect(
        (vertical.render.child!.parentData! as BoxParentData).offset,
        const Offset(-50, 0),
      );

      const clips = <String, Clip>{
        'none': Clip.none,
        'hardEdge': Clip.hardEdge,
        'antiAlias': Clip.antiAlias,
        'antiAliasWithSaveLayer': Clip.antiAliasWithSaveLayer,
      };
      for (final entry in clips.entries) {
        final result = await pump({
          'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': entry.key},
        });
        expect(result.widget.clipBehavior, entry.value, reason: entry.key);
        expect(result.render.clipBehavior, entry.value, reason: entry.key);
      }
    },
  );

  testWidgets(
    'resolves UnconstrainedBox physical and directional alignment exactly',
    (tester) async {
      const widgetId = '67247867-79f8-470f-b109-59971aa7392c';
      final child = _viewSizedBoxNode(
        'ff6ed199-2782-4be8-873b-e61ed6422664',
        width: 40,
        height: 20,
      );

      Future<({Offset offset, TextDirection direction})> pump({
        required String locale,
        required String basis,
        required double horizontal,
        String? explicitDirection,
      }) async {
        final properties = <String, Object?>{
          'alignment': _viewAlignment(
            basis: basis,
            horizontal: horizontal,
            vertical: 0,
          ),
        };
        if (explicitDirection != null) {
          properties['textDirection'] = {
            'kind': 'enum',
            'type': 'TextDirection',
            'value': explicitDirection,
          };
        }
        final json = _modelWithCenteredUnconstrainedBox(
          properties: properties,
          child: child,
        );
        (json['profile']! as Map<String, Object?>)['locale'] = locale;
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
        await tester.pump();
        final render = tester.renderObject<RenderConstraintsTransformBox>(
          find
              .descendant(
                of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
                matching: find.byType(ConstraintsTransformBox),
              )
              .first,
        );
        expect(tester.takeException(), isNull);
        return (
          offset: (render.child!.parentData! as BoxParentData).offset,
          direction: render.textDirection!,
        );
      }

      final physicalLtr = await pump(
        locale: 'en-US',
        basis: 'physical',
        horizontal: 1,
      );
      final physicalRtl = await pump(
        locale: 'ar-SA',
        basis: 'physical',
        horizontal: 1,
      );
      expect(physicalLtr.offset, const Offset(160, 40));
      expect(physicalRtl.offset, const Offset(160, 40));

      final ambientLtr = await pump(
        locale: 'en-US',
        basis: 'directional',
        horizontal: -1,
      );
      final ambientRtl = await pump(
        locale: 'ar-SA',
        basis: 'directional',
        horizontal: -1,
      );
      expect(ambientLtr.direction, TextDirection.ltr);
      expect(ambientLtr.offset, const Offset(0, 40));
      expect(ambientRtl.direction, TextDirection.rtl);
      expect(ambientRtl.offset, const Offset(160, 40));

      final explicitLtr = await pump(
        locale: 'ar-SA',
        basis: 'directional',
        horizontal: -1,
        explicitDirection: 'ltr',
      );
      final explicitRtl = await pump(
        locale: 'en-US',
        basis: 'directional',
        horizontal: -1,
        explicitDirection: 'rtl',
      );
      expect(explicitLtr.direction, TextDirection.ltr);
      expect(explicitLtr.offset, const Offset(0, 40));
      expect(explicitRtl.direction, TextDirection.rtl);
      expect(explicitRtl.offset, const Offset(160, 40));
    },
  );

  testWidgets(
    'keeps empty UnconstrainedBox selectable and exposes only its empty child slot',
    (tester) async {
      const widgetId = '67247867-79f8-470f-b109-59971aa7392c';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredUnconstrainedBox(
                properties: const {},
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real LimitedBox limits only on unbounded incoming axes',
    (tester) async {
      const widgetId = '57b8ce90-edde-4988-a4b7-bbf2eec66922';
      final child = _viewSizedBoxNode(
        'f28acc9b-e654-4b3a-ad07-59d75742b433',
        width: 150,
        height: 75,
      );

      Finder limitedFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(LimitedBox),
          )
          .first;

      Future<({LimitedBox widget, RenderLimitedBox render})> pump({
        required Map<String, Object?> properties,
        required String incoming,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredLimitedBox(
                  properties: properties,
                  child: child,
                  incoming: incoming,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
        return (
          widget: tester.widget<LimitedBox>(limitedFinder()),
          render: tester.renderObject<RenderLimitedBox>(limitedFinder()),
        );
      }

      const limits = <String, Object?>{
        'maxWidth': {'kind': 'double', 'value': 80.0},
        'maxHeight': {'kind': 'double', 'value': 40.0},
      };

      final bounded = await pump(properties: limits, incoming: 'bounded');
      expect(bounded.widget.maxWidth, 80);
      expect(bounded.widget.maxHeight, 40);
      expect(bounded.render.maxWidth, 80);
      expect(bounded.render.maxHeight, 40);
      expect(
        bounded.render.constraints,
        BoxConstraints.tight(const Size(200, 100)),
      );
      expect(bounded.render.size, const Size(200, 100));
      expect(bounded.render.child!.size, const Size(200, 100));

      final widthUnbounded = await pump(
        properties: limits,
        incoming: 'widthUnbounded',
      );
      expect(widthUnbounded.render.constraints.hasBoundedWidth, isFalse);
      expect(widthUnbounded.render.constraints.hasBoundedHeight, isTrue);
      expect(widthUnbounded.render.size, const Size(80, 100));
      expect(widthUnbounded.render.child!.size, const Size(80, 100));

      final heightUnbounded = await pump(
        properties: limits,
        incoming: 'heightUnbounded',
      );
      expect(heightUnbounded.render.constraints.hasBoundedWidth, isTrue);
      expect(heightUnbounded.render.constraints.hasBoundedHeight, isFalse);
      expect(heightUnbounded.render.size, const Size(200, 40));
      expect(heightUnbounded.render.child!.size, const Size(200, 40));

      final bothUnbounded = await pump(
        properties: limits,
        incoming: 'bothUnbounded',
      );
      expect(bothUnbounded.render.constraints.hasBoundedWidth, isFalse);
      expect(bothUnbounded.render.constraints.hasBoundedHeight, isFalse);
      expect(bothUnbounded.render.size, const Size(80, 40));
      expect(bothUnbounded.render.child!.size, const Size(80, 40));

      final omitted = await pump(
        properties: const {},
        incoming: 'bothUnbounded',
      );
      expect(omitted.widget.maxWidth, double.infinity);
      expect(omitted.widget.maxHeight, double.infinity);
      expect(omitted.render.maxWidth, double.infinity);
      expect(omitted.render.maxHeight, double.infinity);
      expect(omitted.render.size, const Size(150, 75));
      expect(omitted.render.child!.size, const Size(150, 75));
    },
  );

  testWidgets(
    'keeps empty LimitedBox selectable with bounded child-drop geometry',
    (tester) async {
      const widgetId = '57b8ce90-edde-4988-a4b7-bbf2eec66922';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredLimitedBox(
                properties: const {},
                child: null,
                incoming: 'bothUnbounded',
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real OverflowBox bound overrides, overflow, omission, and both fits',
    (tester) async {
      const widgetId = '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4';

      Finder overflowFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(OverflowBox),
          )
          .first;

      Future<({OverflowBox widget, RenderConstrainedOverflowBox render})> pump({
        required Map<String, Object?> properties,
        required Map<String, Object?>? child,
        bool bounded = true,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredOverflowBox(
                  properties: properties,
                  child: child,
                  bounded: bounded,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
        return (
          widget: tester.widget<OverflowBox>(overflowFinder()),
          render: tester.renderObject<RenderConstrainedOverflowBox>(
            overflowFinder(),
          ),
        );
      }

      final overflow = await pump(
        properties: const {
          'minWidth': {'kind': 'double', 'value': 300.0},
          'maxWidth': {'kind': 'double', 'value': 300.0},
          'minHeight': {'kind': 'double', 'value': 160.0},
          'maxHeight': {'kind': 'double', 'value': 160.0},
        },
        child: _viewSizedBoxNode(
          'f72283ea-320c-4733-9072-963bc8281e7b',
          width: 10,
          height: 10,
        ),
      );
      expect(overflow.widget.minWidth, 300);
      expect(overflow.widget.maxWidth, 300);
      expect(overflow.widget.minHeight, 160);
      expect(overflow.widget.maxHeight, 160);
      expect(overflow.widget.fit, OverflowBoxFit.max);
      expect(
        overflow.render.constraints,
        BoxConstraints.tight(const Size(200, 100)),
      );
      expect(overflow.render.size, const Size(200, 100));
      expect(overflow.render.child!.size, const Size(300, 160));

      final maxFit = await pump(
        properties: const {
          'fit': {'kind': 'enum', 'type': 'OverflowBoxFit', 'value': 'max'},
        },
        child: _viewSizedBoxNode(
          'f72283ea-320c-4733-9072-963bc8281e7b',
          width: 60,
          height: 30,
        ),
        bounded: false,
      );
      expect(maxFit.widget.alignment, Alignment.center);
      expect(maxFit.widget.minWidth, isNull);
      expect(maxFit.widget.maxWidth, isNull);
      expect(maxFit.widget.minHeight, isNull);
      expect(maxFit.widget.maxHeight, isNull);
      expect(maxFit.widget.fit, OverflowBoxFit.max);
      expect(maxFit.render.child!.size, const Size(60, 30));
      expect(maxFit.render.size.width, greaterThan(60));
      expect(maxFit.render.size.height, greaterThan(30));

      final deferToChild = await pump(
        properties: const {
          'fit': {
            'kind': 'enum',
            'type': 'OverflowBoxFit',
            'value': 'deferToChild',
          },
        },
        child: _viewSizedBoxNode(
          'f72283ea-320c-4733-9072-963bc8281e7b',
          width: 60,
          height: 30,
        ),
        bounded: false,
      );
      expect(deferToChild.widget.fit, OverflowBoxFit.deferToChild);
      expect(deferToChild.render.size, const Size(60, 30));
      expect(deferToChild.render.child!.size, const Size(60, 30));
    },
  );

  testWidgets(
    'resolves OverflowBox physical and directional alignment in LTR and RTL',
    (tester) async {
      const widgetId = '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4';
      final child = _viewSizedBoxNode(
        'f72283ea-320c-4733-9072-963bc8281e7b',
        width: 10,
        height: 10,
      );

      Future<({Offset offset, TextDirection direction})> pump({
        required String locale,
        required String basis,
        required double horizontal,
      }) async {
        final json = _modelWithCenteredOverflowBox(
          properties: {
            'alignment': _viewAlignment(
              basis: basis,
              horizontal: horizontal,
              vertical: 0,
            ),
            'minWidth': {'kind': 'double', 'value': 300.0},
            'maxWidth': {'kind': 'double', 'value': 300.0},
            'minHeight': {'kind': 'double', 'value': 160.0},
            'maxHeight': {'kind': 'double', 'value': 160.0},
          },
          child: child,
        );
        (json['profile']! as Map<String, Object?>)['locale'] = locale;
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
        await tester.pump();
        final render = tester.renderObject<RenderConstrainedOverflowBox>(
          find
              .descendant(
                of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
                matching: find.byType(OverflowBox),
              )
              .first,
        );
        expect(tester.takeException(), isNull);
        return (
          offset: (render.child!.parentData! as BoxParentData).offset,
          direction: render.textDirection!,
        );
      }

      final physicalLtr = await pump(
        locale: 'en-US',
        basis: 'physical',
        horizontal: 1,
      );
      final physicalRtl = await pump(
        locale: 'ar-SA',
        basis: 'physical',
        horizontal: 1,
      );
      expect(physicalLtr.offset, const Offset(-100, -30));
      expect(physicalRtl.offset, const Offset(-100, -30));

      final directionalLtr = await pump(
        locale: 'en-US',
        basis: 'directional',
        horizontal: -1,
      );
      final directionalRtl = await pump(
        locale: 'ar-SA',
        basis: 'directional',
        horizontal: -1,
      );
      expect(directionalLtr.direction, TextDirection.ltr);
      expect(directionalLtr.offset, const Offset(0, -30));
      expect(directionalRtl.direction, TextDirection.rtl);
      expect(directionalRtl.offset, const Offset(-100, -30));
    },
  );

  testWidgets(
    'keeps empty zero-size OverflowBox selectable with child-drop geometry',
    (tester) async {
      const widgetId = '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredOverflowBox(
                properties: const {
                  'fit': {
                    'kind': 'enum',
                    'type': 'OverflowBoxFit',
                    'value': 'deferToChild',
                  },
                },
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('renders every Wrap argument and flows children into runs', (
    tester,
  ) async {
    const wrapId = '0cdde885-b68c-4b21-bd47-246721c4e8b2';
    const firstId = 'f5d1751b-136a-4db0-9e22-2ecb498eb425';
    const secondId = '0bdf25b7-ce2c-4cb7-9f04-270e53ad3157';
    const thirdId = '26829bde-f9d3-4bbc-b557-51693708e35e';
    CanvasDropResolver? resolver;
    CanvasMovePreviewResolver? moveResolver;
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithConstrainedWrap(
              properties: {
                'direction': {
                  'kind': 'enum',
                  'type': 'Axis',
                  'value': 'horizontal',
                },
                'alignment': {
                  'kind': 'enum',
                  'type': 'WrapAlignment',
                  'value': 'center',
                },
                'spacing': {'kind': 'double', 'value': 10.0},
                'runAlignment': {
                  'kind': 'enum',
                  'type': 'WrapAlignment',
                  'value': 'end',
                },
                'runSpacing': {'kind': 'double', 'value': 5.0},
                'crossAxisAlignment': {
                  'kind': 'enum',
                  'type': 'WrapCrossAlignment',
                  'value': 'end',
                },
                'textDirection': {
                  'kind': 'enum',
                  'type': 'TextDirection',
                  'value': 'ltr',
                },
                'verticalDirection': {
                  'kind': 'enum',
                  'type': 'VerticalDirection',
                  'value': 'down',
                },
                'clipBehavior': {
                  'kind': 'enum',
                  'type': 'Clip',
                  'value': 'antiAlias',
                },
              },
              children: [
                _viewSizedBoxNode(firstId, width: 40, height: 20),
                _viewSizedBoxNode(secondId, width: 40, height: 30),
                _viewSizedBoxNode(thirdId, width: 40, height: 10),
              ],
            ),
          ),
        ),
      ),
    );

    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: (_) {},
        onDropResolverChanged: (value) => resolver = value,
        onMovePreviewResolverChanged: (value) => moveResolver = value,
      ),
    );
    await tester.pump();

    final node = find.byKey(const ValueKey('canvas-widget-$wrapId'));
    final finder = find.descendant(of: node, matching: find.byType(Wrap)).first;
    final widget = tester.widget<Wrap>(finder);
    expect(widget.direction, Axis.horizontal);
    expect(widget.alignment, WrapAlignment.center);
    expect(widget.spacing, 10);
    expect(widget.runAlignment, WrapAlignment.end);
    expect(widget.runSpacing, 5);
    expect(widget.crossAxisAlignment, WrapCrossAlignment.end);
    expect(widget.textDirection, TextDirection.ltr);
    expect(widget.verticalDirection, VerticalDirection.down);
    expect(widget.clipBehavior, Clip.antiAlias);
    expect(widget.children, hasLength(3));

    final render = tester.renderObject<RenderWrap>(finder);
    expect(render.size, const Size(90, 45));
    final offsets = <Offset>[];
    RenderBox? child = render.firstChild;
    while (child != null) {
      offsets.add((child.parentData! as WrapParentData).offset);
      child = render.childAfter(child);
    }
    expect(offsets, const [Offset(0, 10), Offset(50, 0), Offset(25, 35)]);

    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final wrapRect = tester.getRect(node);
    final point = wrapRect.bottomRight - const Offset(1, 1);
    final drop = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(drop?.parentWidgetId, wrapId);
    expect(drop?.slotName, 'children');
    expect(drop?.insertionIndex, 3);
    expect(
      drop!.zone!.rightMicros - drop.zone!.leftMicros,
      closeTo((wrapRect.width / surface.width * 1000000).round(), 2),
      reason: 'multi-run Wrap exposes its complete rendered node',
    );
    final move = moveResolver!(firstId, wrapId, 'children', 0);
    expect(move?.parentWidgetId, wrapId);
    expect(move?.slotName, 'children');
    expect(move?.insertionIndex, 0);
    expect(move?.zone?.leftMicros, drop.zone!.leftMicros);
    expect(move?.zone?.topMicros, drop.zone!.topMicros);
    expect(move?.zone?.rightMicros, drop.zone!.rightMicros);
    expect(move?.zone?.bottomMicros, drop.zone!.bottomMicros);
    expect(tester.takeException(), isNull);
  });

  testWidgets('keeps an empty Wrap selectable and exposes its append slot', (
    tester,
  ) async {
    const wrapId = '0cdde885-b68c-4b21-bd47-246721c4e8b2';
    String? selectedWidgetId;
    CanvasDropResolver? resolver;
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithConstrainedWrap(
              properties: {
                'spacing': {'kind': 'double', 'value': -4.0},
                'runSpacing': {'kind': 'double', 'value': -2.0},
              },
              children: const [],
              unboundedMainAxis: true,
            ),
          ),
        ),
      ),
    );

    await tester.pumpWidget(
      StatefulBuilder(
        builder: (context, setState) => CanvasModelApp(
          model: model,
          selectedWidgetId: selectedWidgetId,
          onSelected: (id) => setState(() => selectedWidgetId = id),
          onDropResolverChanged: (value) => resolver = value,
        ),
      ),
    );
    await tester.pump();

    final node = find.byKey(const ValueKey('canvas-widget-$wrapId'));
    final widget = tester.widget<Wrap>(
      find.descendant(of: node, matching: find.byType(Wrap)).first,
    );
    expect(widget.spacing, -4);
    expect(widget.runSpacing, -2);
    expect(tester.getSize(node), Size.zero);
    final target = find.byKey(
      const ValueKey('canvas-zero-size-widget-target-$wrapId'),
    );
    expect(target, findsOneWidget);
    expect(tester.getSize(target), const Size(36, 36));
    await tester.tap(target);
    await tester.pump();
    expect(selectedWidgetId, wrapId);

    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final point = tester.getRect(target).center;
    final drop = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(drop?.parentWidgetId, wrapId);
    expect(drop?.slotName, 'children');
    expect(drop?.insertionIndex, 0);
    expect(drop?.zone?.isEmpty, isFalse);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'renders real Stack defaults, exact fits, alignment, and z-order',
    (tester) async {
      const stackId = '5a809127-5a50-4dbf-b5fd-464619344ac4';
      const bottomId = 'f5d1751b-136a-4db0-9e22-2ecb498eb425';
      const topId = '0bdf25b7-ce2c-4cb7-9f04-270e53ad3157';
      final children = <Map<String, Object?>>[
        _viewSizedBoxNode(bottomId, width: 40, height: 20),
        _viewSizedBoxNode(topId, width: 80, height: 30),
      ];
      String? selectedWidgetId;

      Finder stackFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$stackId')),
            matching: find.byType(Stack),
          )
          .first;

      Future<RenderStack> pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithConstrainedStack(
                  properties: properties,
                  children: children,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => selectedWidgetId = id,
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderStack>(stackFinder());
      }

      List<RenderBox> renderChildren(RenderStack stack) {
        final result = <RenderBox>[];
        RenderBox? child = stack.firstChild;
        while (child != null) {
          result.add(child);
          child = stack.childAfter(child);
        }
        return result;
      }

      final defaults = await pump(const {});
      final defaultWidget = tester.widget<Stack>(stackFinder());
      expect(defaultWidget.alignment, AlignmentDirectional.topStart);
      expect(defaultWidget.textDirection, isNull);
      expect(defaultWidget.fit, StackFit.loose);
      expect(defaultWidget.clipBehavior, Clip.hardEdge);
      expect(defaults.size, const Size(100, 50));
      var renderedChildren = renderChildren(defaults);
      expect(renderedChildren.map((child) => child.size), const [
        Size(40, 20),
        Size(80, 30),
      ]);
      expect(
        renderedChildren.map(
          (child) => (child.parentData! as StackParentData).offset,
        ),
        const [Offset.zero, Offset.zero],
      );

      final stackRect = tester.getRect(stackFinder());
      await tester.tapAt(stackRect.topLeft + const Offset(10, 10));
      await tester.pump();
      expect(
        selectedWidgetId,
        topId,
        reason: 'the last Stack child paints and hit-tests on top',
      );

      final expanded = await pump({
        'fit': {'kind': 'enum', 'type': 'StackFit', 'value': 'expand'},
      });
      expect(expanded.size, const Size(200, 100));
      expect(renderChildren(expanded).map((child) => child.size), const [
        Size(200, 100),
        Size(200, 100),
      ]);

      final passthrough = await pump({
        'fit': {'kind': 'enum', 'type': 'StackFit', 'value': 'passthrough'},
      });
      expect(passthrough.size, const Size(100, 50));
      expect(renderChildren(passthrough).map((child) => child.size), const [
        Size(100, 50),
        Size(100, 50),
      ]);

      final extrapolated = await pump({
        'alignment': _viewAlignment(horizontal: 2, vertical: -2),
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'none'},
      });
      final explicitWidget = tester.widget<Stack>(stackFinder());
      expect(explicitWidget.alignment, const Alignment(2, -2));
      expect(explicitWidget.clipBehavior, Clip.none);
      renderedChildren = renderChildren(extrapolated);
      expect(
        (renderedChildren.first.parentData! as StackParentData).offset,
        const Offset(90, -15),
      );
      expect(
        (renderedChildren.last.parentData! as StackParentData).offset,
        const Offset(30, -10),
      );
    },
  );

  testWidgets('resolves Stack directional defaults and explicit override', (
    tester,
  ) async {
    const stackId = '5a809127-5a50-4dbf-b5fd-464619344ac4';
    final child = _viewSizedBoxNode(
      'f5d1751b-136a-4db0-9e22-2ecb498eb425',
      width: 40,
      height: 20,
    );

    Future<({Offset offset, TextDirection direction})> render({
      required String locale,
      String? explicitDirection,
    }) async {
      final properties = <String, Object?>{};
      if (explicitDirection != null) {
        properties['textDirection'] = {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': explicitDirection,
        };
      }
      final json = _modelWithConstrainedStack(
        properties: properties,
        children: [child],
      );
      (json['profile']! as Map<String, Object?>)['locale'] = locale;
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
      await tester.pump();
      final node = find.byKey(const ValueKey('canvas-widget-$stackId'));
      final finder = find
          .descendant(of: node, matching: find.byType(Stack))
          .first;
      final stack = tester.renderObject<RenderStack>(finder);
      final first = stack.firstChild!;
      return (
        offset: (first.parentData! as StackParentData).offset,
        direction: stack.textDirection!,
      );
    }

    final ambientLtr = await render(locale: 'en-US');
    expect(ambientLtr.direction, TextDirection.ltr);
    expect(ambientLtr.offset, Offset.zero);

    final ambientRtl = await render(locale: 'ar-SA');
    expect(ambientRtl.direction, TextDirection.rtl);
    expect(ambientRtl.offset, const Offset(60, 0));

    final overridden = await render(locale: 'ar-SA', explicitDirection: 'ltr');
    expect(overridden.direction, TextDirection.ltr);
    expect(overridden.offset, Offset.zero);
  });

  testWidgets(
    'uses full-node Stack append and move zones and preserves zero selection',
    (tester) async {
      const stackId = '5a809127-5a50-4dbf-b5fd-464619344ac4';
      const sourceId = 'f5d1751b-136a-4db0-9e22-2ecb498eb425';
      CanvasDropResolver? dropResolver;
      CanvasMovePreviewResolver? moveResolver;

      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithConstrainedStack(
                properties: const {},
                children: [_viewSizedBoxNode(sourceId, width: 40, height: 20)],
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
          onDropResolverChanged: (value) => dropResolver = value,
          onMovePreviewResolverChanged: (value) => moveResolver = value,
        ),
      );
      await tester.pump();

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final stack = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$stackId')),
      );
      final point = stack.bottomRight - const Offset(2, 2);
      final drop = dropResolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, stackId);
      expect(drop?.slotName, 'children');
      expect(drop?.insertionIndex, 1);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(
        drop!.zone!.rightMicros - drop.zone!.leftMicros,
        closeTo((stack.width / surface.width * 1000000).round(), 2),
        reason: 'Stack exposes its complete rendered node, not a terminal band',
      );

      final move = moveResolver!(sourceId, stackId, 'children', 0);
      expect(move?.parentWidgetId, stackId);
      expect(move?.slotName, 'children');
      expect(move?.insertionIndex, 0);
      expect(move?.zone?.leftMicros, drop.zone!.leftMicros);
      expect(move?.zone?.topMicros, drop.zone!.topMicros);
      expect(move?.zone?.rightMicros, drop.zone!.rightMicros);
      expect(move?.zone?.bottomMicros, drop.zone!.bottomMicros);

      final zeroModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithConstrainedStack(
                properties: const {},
                children: const [],
                unboundedMainAxis: true,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: zeroModel,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(
        tester.getSize(find.byKey(const ValueKey('canvas-widget-$stackId'))),
        Size.zero,
      );
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$stackId'),
      );
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
    },
  );

  testWidgets(
    'renders real Expanded as the direct ParentDataWidget in Row and Column',
    (tester) async {
      const firstId = '6ab52421-f234-4443-978f-bb57b9926f2e';
      const secondId = 'a17cb29f-3784-4ab8-a590-1233dd3e5731';
      final first = _viewExpandedNode(
        firstId,
        child: _viewSizedBoxNode(
          '0e991cf4-2481-4527-b608-c5c6244376f2',
          width: 30,
          height: 20,
        ),
      );
      final second = _viewExpandedNode(
        secondId,
        flex: 2,
        child: _viewSizedBoxNode(
          '1809f65f-03a7-4aba-8c23-10fe3020b364',
          width: 30,
          height: 20,
        ),
      );

      Future<void> pump(String parentType) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithFixedFlex(
                  parentType: parentType,
                  children: [first, second],
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
      }

      await pump('flutter.widgets.Row');
      var expanded = tester
          .widgetList<Expanded>(find.byType(Expanded))
          .toList();
      expect(expanded.map((widget) => widget.flex), [1, 2]);
      final firstRowSize = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$firstId')),
      );
      final secondRowSize = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$secondId')),
      );
      expect(firstRowSize.width, closeTo(100, 0.01));
      expect(secondRowSize.width, closeTo(200, 0.01));

      final zeroModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithFixedFlex(
                parentType: 'flutter.widgets.Row',
                children: [
                  _viewExpandedNode(
                    firstId,
                    flex: 0,
                    child: _viewSizedBoxNode(
                      '0e991cf4-2481-4527-b608-c5c6244376f2',
                      width: 30,
                      height: 20,
                    ),
                  ),
                  _viewExpandedNode(
                    secondId,
                    child: _viewSizedBoxNode(
                      '1809f65f-03a7-4aba-8c23-10fe3020b364',
                      width: 30,
                      height: 20,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: zeroModel,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$firstId')))
            .width,
        closeTo(30, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$secondId')))
            .width,
        closeTo(270, 0.01),
      );
      expect(tester.takeException(), isNull);

      await pump('flutter.widgets.Column');
      expanded = tester.widgetList<Expanded>(find.byType(Expanded)).toList();
      expect(expanded.map((widget) => widget.flex), [1, 2]);
      final firstColumnSize = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$firstId')),
      );
      final secondColumnSize = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$secondId')),
      );
      expect(firstColumnSize.height, closeTo(40, 0.01));
      expect(secondColumnSize.height, closeTo(80, 0.01));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real Flexible with exact flex and loose or tight fit in Row and Column',
    (tester) async {
      const zeroId = '70517708-fd15-4900-a5a1-3fb659f3fd60';
      const defaultId = '913206a7-d96d-488a-a6a6-261e0c14a9c2';
      const maximumId = '63e57fd7-7541-49bc-a81c-ae521b5f1fb9';
      final zero = _viewFlexibleNode(
        zeroId,
        flex: 0,
        child: _viewSizedBoxNode(
          'd66779a8-ac37-41a4-b77a-e4b47a2314b9',
          width: 30,
          height: 20,
        ),
      );
      final defaults = _viewFlexibleNode(
        defaultId,
        child: _viewSizedBoxNode(
          '2564005f-f3df-444a-8ed2-1718e4d9468e',
          width: 30,
          height: 20,
        ),
      );
      final maximum = _viewFlexibleNode(
        maximumId,
        flex: maxCanvasSequence,
        fit: 'tight',
        child: _viewSizedBoxNode(
          '052e72cd-569d-48fd-8ec7-d733c2a6f45b',
          width: 30,
          height: 20,
        ),
      );

      Future<void> pump(String parentType) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithFixedFlex(
                  parentType: parentType,
                  children: [zero, defaults, maximum],
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
      }

      void expectDirectFlexRenderChildren(Finder parent) {
        final renderFlex = tester.renderObject<RenderFlex>(parent);
        for (final id in <String>[zeroId, defaultId, maximumId]) {
          var directChild = tester.renderObject<RenderObject>(
            find.byKey(ValueKey('canvas-widget-$id')),
          );
          while (directChild.parent != null &&
              directChild.parent != renderFlex) {
            directChild = directChild.parent!;
          }
          expect(directChild.parent, same(renderFlex));
          expect(directChild.parentData, isA<FlexParentData>());
        }
      }

      await pump('flutter.widgets.Row');
      var flexible = tester
          .widgetList<Flexible>(find.byType(Flexible))
          .toList();
      expect(flexible.map((widget) => widget.flex), [0, 1, maxCanvasSequence]);
      expect(flexible.map((widget) => widget.fit), [
        FlexFit.loose,
        FlexFit.loose,
        FlexFit.tight,
      ]);
      expectDirectFlexRenderChildren(find.byType(Row));
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$zeroId')))
            .width,
        closeTo(30, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$defaultId')))
            .width,
        closeTo(0, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$maximumId')))
            .width,
        closeTo(270, 0.01),
      );

      await pump('flutter.widgets.Column');
      flexible = tester.widgetList<Flexible>(find.byType(Flexible)).toList();
      expect(flexible.map((widget) => widget.flex), [0, 1, maxCanvasSequence]);
      expect(flexible.map((widget) => widget.fit), [
        FlexFit.loose,
        FlexFit.loose,
        FlexFit.tight,
      ]);
      expectDirectFlexRenderChildren(find.byType(Column));
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$zeroId')))
            .height,
        closeTo(20, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$defaultId')))
            .height,
        closeTo(0, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$maximumId')))
            .height,
        closeTo(100, 0.01),
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real Spacer directly in Row and Column with selectable overlays',
    (tester) async {
      const defaultId = '18f6cf8c-4bb7-4d00-a159-a2fa5abc92ea';
      const weightedId = 'ee23441e-0993-4f6e-951e-fcc38ac2fe69';
      const leadingId = '185940b7-436d-4671-9215-f62d29d6754a';
      const trailingId = '8607bb96-03a2-4d85-a8f6-91192394cf6e';
      String? selected;

      Future<void> pump(
        String parentType, {
        bool stretchCrossAxis = false,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithFixedFlex(
                  parentType: parentType,
                  parentProperties: stretchCrossAxis
                      ? const {
                          'crossAxisAlignment': {
                            'kind': 'enum',
                            'type': 'CrossAxisAlignment',
                            'value': 'stretch',
                          },
                        }
                      : const {},
                  children: [
                    _viewSizedBoxNode(leadingId, width: 30, height: 20),
                    _viewSpacerNode(defaultId),
                    _viewSpacerNode(weightedId, flex: 2),
                    _viewSizedBoxNode(trailingId, width: 30, height: 20),
                  ],
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: selected,
            onSelected: (id) => selected = id,
          ),
        );
        await tester.pump();
        await tester.pump();
        expect(tester.takeException(), isNull);
      }

      void expectDirectSpacerRenderChildren(Finder parent) {
        final renderFlex = tester.renderObject<RenderFlex>(parent);
        for (var index = 0; index < 2; index++) {
          final renderObject = tester.renderObject<RenderObject>(
            find.byType(Spacer).at(index),
          );
          expect(renderObject.parent, same(renderFlex));
          expect(renderObject.parentData, isA<FlexParentData>());
        }
      }

      await pump('flutter.widgets.Row');
      var spacers = tester.widgetList<Spacer>(find.byType(Spacer)).toList();
      expect(spacers.map((widget) => widget.flex), [1, 2]);
      expectDirectSpacerRenderChildren(find.byType(Row));
      expect(
        tester.getSize(find.byKey(const ValueKey('canvas-widget-$defaultId'))),
        const Size(80, 0),
      );
      expect(
        tester.getSize(find.byKey(const ValueKey('canvas-widget-$weightedId'))),
        const Size(160, 0),
      );
      final defaultTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$defaultId'),
      );
      expect(defaultTarget, findsOneWidget);
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$weightedId'),
        ),
        findsOneWidget,
      );
      await tester.tap(defaultTarget);
      expect(selected, defaultId);

      await pump('flutter.widgets.Column');
      spacers = tester.widgetList<Spacer>(find.byType(Spacer)).toList();
      expect(spacers.map((widget) => widget.flex), [1, 2]);
      expectDirectSpacerRenderChildren(find.byType(Column));
      final defaultColumn = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$defaultId')),
      );
      final weightedColumn = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$weightedId')),
      );
      expect(defaultColumn.width, 0);
      expect(defaultColumn.height, closeTo(80 / 3, 0.01));
      expect(weightedColumn.width, 0);
      expect(weightedColumn.height, closeTo(160 / 3, 0.01));

      await pump('flutter.widgets.Row', stretchCrossAxis: true);
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$defaultId')))
            .height,
        120,
      );
      expect(
        find.byKey(const ValueKey('canvas-zero-size-widget-target-$defaultId')),
        findsOneWidget,
        reason: 'stretched Spacer still needs external safe instrumentation',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('offers flex wrapper zones on existing Row children only', (
    tester,
  ) async {
    const rowId = 'be4b446d-12b9-42c3-a427-03fb2fd472bd';
    const firstId = '2c65ab83-02e9-4100-b116-2485762440d9';
    const secondId = '294cdd5d-c142-4b93-9856-1b8497cc6bc7';
    const spacerId = '40ab557c-e837-420f-b66a-25ae9ebec96f';
    CanvasDropResolver? resolver;
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithFixedFlex(
              parentType: 'flutter.widgets.Row',
              parentId: rowId,
              children: [
                _viewSizedBoxNode(firstId, width: 80, height: 30),
                _viewSizedBoxNode(secondId, width: 60, height: 30),
                _viewSpacerNode(spacerId),
              ],
            ),
          ),
        ),
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: (_) {},
        onDropResolverChanged: (value) => resolver = value,
      ),
    );
    await tester.pump();

    final surface = tester.getRect(find.byType(CanvasDocumentView));
    CanvasDropTarget? resolveAt(
      String widgetId,
      CanvasPaletteDragSource source,
    ) {
      final point = tester
          .getRect(find.byKey(ValueKey('canvas-widget-$widgetId')))
          .center;
      return resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        source,
      );
    }

    for (final widgetType in <String>[
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
    ]) {
      final source = CanvasPaletteDragSource(
        token: '$widgetType-source',
        widgetType: widgetType,
        traits: const {},
      );
      final first = resolveAt(firstId, source);
      expect(first?.parentWidgetId, rowId);
      expect(first?.slotName, 'children');
      expect(first?.insertionIndex, 0);
      expect(first?.zone?.isEmpty, isFalse);

      final second = resolveAt(secondId, source);
      expect(second?.parentWidgetId, rowId);
      expect(second?.slotName, 'children');
      expect(second?.insertionIndex, 1);
      expect(second?.zone?.isEmpty, isFalse);

      expect(
        resolveAt(spacerId, source),
        isNull,
        reason: '$widgetType cannot wrap Spacer',
      );
    }

    final normalSource = CanvasPaletteDragSource(
      token: 'text-source',
      widgetType: 'flutter.widgets.Text',
      traits: const {},
    );
    final normal = resolver!(
      ((tester
                      .getRect(
                        find.byKey(const ValueKey('canvas-widget-$rowId')),
                      )
                      .right -
                  2 -
                  surface.left) /
              surface.width *
              1000000)
          .round(),
      ((tester
                      .getRect(
                        find.byKey(const ValueKey('canvas-widget-$rowId')),
                      )
                      .center
                      .dy -
                  surface.top) /
              surface.height *
              1000000)
          .round(),
      normalSource,
    );
    expect(normal?.parentWidgetId, rowId);
    expect(normal?.insertionIndex, 3);
  });

  testWidgets(
    'renders every reviewed direct Image argument with real Flutter',
    (tester) async {
      const imageId = '93e0b32e-cefa-43ea-8b73-209f9434980c';
      final resourceId = sha256Hex(_viewPng8);
      final resources = CanvasImageResourceBundle.fromResources([
        CanvasImageResource(
          resourceId: resourceId,
          mediaType: 'image/png',
          pixelWidth: 8,
          pixelHeight: 8,
          encodedBytes: _viewPng8,
        ),
      ]);
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredImage(
                id: imageId,
                properties: {
                  'image': _viewDirectImageProvider(
                    provider: _viewImageProvider(
                      kind: 'exactAsset',
                      assetName: 'assets/images/hero.png',
                      exactScale: 2,
                      resize: {
                        'width': 8,
                        'height': 8,
                        'policy': 'exact',
                        'allowUpscaling': false,
                      },
                      resolution: {
                        'kind': 'resolved',
                        'resourceId': resourceId,
                        'resolvedScale': 2,
                      },
                    ),
                  ),
                  'frameBuilder': {'kind': 'callbackPresence'},
                  'loadingBuilder': {'kind': 'callbackPresence'},
                  'errorBuilder': {'kind': 'callbackPresence'},
                  'semanticLabel': {'kind': 'string', 'value': 'Hero image'},
                  'excludeFromSemantics': {'kind': 'boolean', 'value': true},
                  'width': {'kind': 'integer', 'value': 120},
                  'height': {'kind': 'double', 'value': 80.0},
                  'color': {
                    'kind': 'themeToken',
                    'token': 'material.colorScheme.primary',
                  },
                  'opacity': {'kind': 'double', 'value': 0.6},
                  'colorBlendMode': {
                    'kind': 'enum',
                    'type': 'BlendMode',
                    'value': 'multiply',
                  },
                  'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'fill'},
                  'alignment': _viewAlignment(
                    basis: 'directional',
                    horizontal: -1,
                    vertical: 0.25,
                  ),
                  'repeat': {
                    'kind': 'enum',
                    'type': 'ImageRepeat',
                    'value': 'repeatX',
                  },
                  'centerSliceLeft': {'kind': 'double', 'value': 1.0},
                  'centerSliceTop': {'kind': 'double', 'value': 1.0},
                  'centerSliceRight': {'kind': 'double', 'value': 3.0},
                  'centerSliceBottom': {'kind': 'double', 'value': 3.0},
                  'matchTextDirection': {'kind': 'boolean', 'value': true},
                  'gaplessPlayback': {'kind': 'boolean', 'value': true},
                  'isAntiAlias': {'kind': 'boolean', 'value': true},
                  'filterQuality': {
                    'kind': 'enum',
                    'type': 'FilterQuality',
                    'value': 'high',
                  },
                },
              ),
            ),
          ),
        ),
      );

      String? selected;
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          imageResources: resources,
          selectedWidgetId: imageId,
          onSelected: (value) => selected = value,
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$imageId'));
      final image = tester.widget<Image>(
        find.descendant(of: node, matching: find.byType(Image)).first,
      );
      expect(image.image, isA<ResizeImage>());
      final resized = image.image as ResizeImage;
      expect(resized.width, 8);
      expect(resized.height, 8);
      expect(resized.policy, ResizeImagePolicy.exact);
      expect(resized.allowUpscaling, isFalse);
      expect((resized.imageProvider as MemoryImage).scale, 2);
      expect(image.frameBuilder, isNotNull);
      expect(image.loadingBuilder, isNotNull);
      expect(image.errorBuilder, isNotNull);
      expect(image.semanticLabel, 'Hero image');
      expect(image.excludeFromSemantics, isTrue);
      expect(image.width, 120);
      expect(image.height, 80);
      expect(image.color, Theme.of(tester.element(node)).colorScheme.primary);
      expect(image.opacity!.value, 0.6);
      expect(image.colorBlendMode, BlendMode.multiply);
      expect(image.fit, BoxFit.fill);
      expect(image.alignment, const AlignmentDirectional(-1, 0.25));
      expect(image.repeat, ImageRepeat.repeatX);
      expect(image.centerSlice, const Rect.fromLTRB(1, 1, 3, 3));
      expect(image.matchTextDirection, isTrue);
      expect(image.gaplessPlayback, isTrue);
      expect(image.isAntiAlias, isTrue);
      expect(image.filterQuality, FilterQuality.high);
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$imageId')),
        findsOneWidget,
      );
      await tester.tap(node);
      expect(selected, imageId);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders direct Image unavailable placeholder without an unsafe centerSlice',
    (tester) async {
      const imageId = '93e0b32e-cefa-43ea-8b73-209f9434980c';
      const reason = 'The declared project image does not exist';
      final semantics = tester.ensureSemantics();
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredImage(
                id: imageId,
                properties: {
                  'image': _viewDirectImageProvider(
                    provider: _viewImageProvider(
                      assetName: 'assets/images/missing.png',
                      packageName: 'image_pack',
                      resolution: {
                        'kind': 'unavailable',
                        'code': 'missing',
                        'reason': reason,
                      },
                    ),
                  ),
                  'width': {'kind': 'integer', 'value': 120},
                  'height': {'kind': 'integer', 'value': 80},
                  'centerSliceLeft': {'kind': 'double', 'value': 1.0},
                  'centerSliceTop': {'kind': 'double', 'value': 1.0},
                  'centerSliceRight': {'kind': 'double', 'value': 3.0},
                  'centerSliceBottom': {'kind': 'double', 'value': 3.0},
                },
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$imageId'));
      final image = tester.widget<Image>(
        find.descendant(of: node, matching: find.byType(Image)).first,
      );
      expect(image.image, isA<MemoryImage>());
      expect(image.centerSlice, isNull);
      expect(
        find.bySemanticsLabel(
          RegExp(
            'Image preview unavailable for '
            'package:image_pack:assets/images/missing.png.*'
            'Status missing.*Reason: ${RegExp.escape(reason)}',
          ),
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
      semantics.dispose();
    },
  );

  testWidgets(
    'renders all reviewed TextField arguments without editable state',
    (tester) async {
      const textFieldId = '73d9ec43-3d37-4304-8998-71fe51804284';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredTextField(
                id: textFieldId,
                properties: {
                  'keyboardType': {
                    'kind': 'string',
                    'value': 'numberSignedDecimal',
                  },
                  'textInputAction': {
                    'kind': 'enum',
                    'type': 'TextInputAction',
                    'value': 'done',
                  },
                  'textCapitalization': {
                    'kind': 'enum',
                    'type': 'TextCapitalization',
                    'value': 'words',
                  },
                  'textAlign': {
                    'kind': 'enum',
                    'type': 'TextAlign',
                    'value': 'center',
                  },
                  'textAlignVertical': {'kind': 'string', 'value': 'bottom'},
                  'textDirection': {
                    'kind': 'enum',
                    'type': 'TextDirection',
                    'value': 'ltr',
                  },
                  'readOnly': {'kind': 'boolean', 'value': true},
                  'showCursor': {'kind': 'boolean', 'value': false},
                  'autofocus': {'kind': 'boolean', 'value': true},
                  'obscuringCharacter': {'kind': 'string', 'value': '#'},
                  'obscureText': {'kind': 'boolean', 'value': false},
                  'autocorrect': {'kind': 'boolean', 'value': false},
                  'smartDashesType': {
                    'kind': 'enum',
                    'type': 'SmartDashesType',
                    'value': 'disabled',
                  },
                  'smartQuotesType': {
                    'kind': 'enum',
                    'type': 'SmartQuotesType',
                    'value': 'enabled',
                  },
                  'enableSuggestions': {'kind': 'boolean', 'value': false},
                  'maxLines': {'kind': 'integer', 'value': 4},
                  'minLines': {'kind': 'integer', 'value': 2},
                  'expands': {'kind': 'boolean', 'value': false},
                  'maxLength': {'kind': 'integer', 'value': -1},
                  'maxLengthEnforcement': {
                    'kind': 'enum',
                    'type': 'MaxLengthEnforcement',
                    'value': 'truncateAfterCompositionEnds',
                  },
                  'onChanged': {'kind': 'callbackPresence'},
                  'onEditingComplete': {'kind': 'callbackPresence'},
                  'onSubmitted': {'kind': 'callbackPresence'},
                  'onAppPrivateCommand': {'kind': 'callbackPresence'},
                  'enabled': {'kind': 'boolean', 'value': true},
                  'ignorePointers': {'kind': 'boolean', 'value': false},
                  'cursorWidth': {'kind': 'integer', 'value': 3},
                  'cursorHeight': {'kind': 'double', 'value': 22.5},
                  'cursorRadiusX': {'kind': 'double', 'value': 4.0},
                  'cursorRadiusY': {'kind': 'double', 'value': 6.0},
                  'cursorOpacityAnimates': {'kind': 'boolean', 'value': true},
                  'cursorColor': {
                    'kind': 'themeToken',
                    'token': 'material.colorScheme.primary',
                  },
                  'cursorErrorColor': {'kind': 'color', 'argb': '0xFFAA1122'},
                  'selectionHeightStyle': {
                    'kind': 'enum',
                    'type': 'BoxHeightStyle',
                    'value': 'includeLineSpacingBottom',
                  },
                  'selectionWidthStyle': {
                    'kind': 'enum',
                    'type': 'BoxWidthStyle',
                    'value': 'max',
                  },
                  'keyboardAppearance': {
                    'kind': 'enum',
                    'type': 'Brightness',
                    'value': 'dark',
                  },
                  'scrollPaddingLeft': {'kind': 'double', 'value': 1.0},
                  'scrollPaddingTop': {'kind': 'double', 'value': 2.0},
                  'scrollPaddingRight': {'kind': 'double', 'value': 3.0},
                  'scrollPaddingBottom': {'kind': 'double', 'value': 4.0},
                  'dragStartBehavior': {
                    'kind': 'enum',
                    'type': 'DragStartBehavior',
                    'value': 'down',
                  },
                  'enableInteractiveSelection': {
                    'kind': 'boolean',
                    'value': false,
                  },
                  'selectAllOnFocus': {'kind': 'boolean', 'value': true},
                  'onTap': {'kind': 'callbackPresence'},
                  'onTapAlwaysCalled': {'kind': 'boolean', 'value': true},
                  'onTapOutside': {'kind': 'callbackPresence'},
                  'onTapUpOutside': {'kind': 'callbackPresence'},
                  'mouseCursor': {'kind': 'string', 'value': 'resizeColumn'},
                  'clipBehavior': {
                    'kind': 'enum',
                    'type': 'Clip',
                    'value': 'antiAlias',
                  },
                  'restorationId': {'kind': 'string', 'value': 'profile-name'},
                  'stylusHandwritingEnabled': {
                    'kind': 'boolean',
                    'value': false,
                  },
                  'enableIMEPersonalizedLearning': {
                    'kind': 'boolean',
                    'value': false,
                  },
                  'enableInlinePrediction': {'kind': 'boolean', 'value': true},
                  'canRequestFocus': {'kind': 'boolean', 'value': true},
                },
              ),
            ),
          ),
        ),
      );

      String? selected;
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (value) => selected = value,
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$textFieldId'));
      final finder = find.descendant(
        of: node,
        matching: find.byType(TextField),
      );
      final field = tester.widget<TextField>(finder);
      expect(
        field.keyboardType,
        const TextInputType.numberWithOptions(signed: true, decimal: true),
      );
      expect(field.textInputAction, TextInputAction.done);
      expect(field.textCapitalization, TextCapitalization.words);
      expect(field.textAlign, TextAlign.center);
      expect(field.textAlignVertical, TextAlignVertical.bottom);
      expect(field.textDirection, TextDirection.ltr);
      expect(field.readOnly, isTrue);
      expect(field.showCursor, isFalse);
      expect(field.autofocus, isTrue);
      expect(field.obscuringCharacter, '#');
      expect(field.obscureText, isFalse);
      expect(field.autocorrect, isFalse);
      expect(field.smartDashesType, SmartDashesType.disabled);
      expect(field.smartQuotesType, SmartQuotesType.enabled);
      expect(field.enableSuggestions, isFalse);
      expect(field.maxLines, 4);
      expect(field.minLines, 2);
      expect(field.expands, isFalse);
      expect(field.maxLength, TextField.noMaxLength);
      expect(
        field.maxLengthEnforcement,
        MaxLengthEnforcement.truncateAfterCompositionEnds,
      );
      expect(field.onChanged, isNotNull);
      expect(field.onEditingComplete, isNotNull);
      expect(field.onSubmitted, isNotNull);
      expect(field.onAppPrivateCommand, isNotNull);
      expect(field.enabled, isTrue);
      expect(field.ignorePointers, isFalse);
      expect(field.cursorWidth, 3);
      expect(field.cursorHeight, 22.5);
      expect(field.cursorRadius, const Radius.elliptical(4, 6));
      expect(field.cursorOpacityAnimates, isTrue);
      expect(
        field.cursorColor,
        Theme.of(tester.element(finder)).colorScheme.primary,
      );
      expect(field.cursorErrorColor, const Color(0xffaa1122));
      expect(
        field.selectionHeightStyle,
        ui.BoxHeightStyle.includeLineSpacingBottom,
      );
      expect(field.selectionWidthStyle, ui.BoxWidthStyle.max);
      expect(field.keyboardAppearance, Brightness.dark);
      expect(field.scrollPadding, const EdgeInsets.fromLTRB(1, 2, 3, 4));
      expect(field.dragStartBehavior, DragStartBehavior.down);
      expect(field.enableInteractiveSelection, isFalse);
      expect(field.selectAllOnFocus, isTrue);
      expect(field.onTap, isNotNull);
      expect(field.onTapAlwaysCalled, isTrue);
      expect(field.onTapOutside, isNotNull);
      expect(field.onTapUpOutside, isNotNull);
      expect(field.mouseCursor, SystemMouseCursors.resizeColumn);
      expect(field.clipBehavior, Clip.antiAlias);
      expect(field.restorationId, 'profile-name');
      expect(field.stylusHandwritingEnabled, isFalse);
      expect(field.enableIMEPersonalizedLearning, isFalse);
      expect(field.enableInlinePrediction, isTrue);
      expect(field.canRequestFocus, isTrue);
      expect(field.controller, isNull);
      expect(field.focusNode, isNull);

      final ignorePointers = find.ancestor(
        of: finder,
        matching: find.byType(IgnorePointer),
      );
      expect(ignorePointers, findsWidgets);
      expect(
        ignorePointers
            .evaluate()
            .map((element) => element.widget)
            .whereType<IgnorePointer>(),
        contains(predicate<IgnorePointer>((widget) => widget.ignoring)),
      );
      final excludeFocus = find.ancestor(
        of: finder,
        matching: find.byType(ExcludeFocus),
      );
      expect(excludeFocus, findsOneWidget);
      expect(tester.widget<ExcludeFocus>(excludeFocus).excluding, isTrue);
      final focusGuards = find
          .ancestor(of: finder, matching: find.byType(Focus))
          .evaluate()
          .map((element) => element.widget)
          .whereType<Focus>();
      expect(
        focusGuards,
        contains(
          predicate<Focus>(
            (widget) =>
                !widget.canRequestFocus &&
                widget.skipTraversal &&
                !widget.descendantsAreFocusable &&
                !widget.descendantsAreTraversable,
          ),
        ),
      );
      final editable = tester.widget<EditableText>(
        find.descendant(of: finder, matching: find.byType(EditableText)),
      );
      expect(editable.focusNode.hasFocus, isFalse);
      await tester.tap(node);
      await tester.pump();
      expect(selected, textFieldId);
      expect(editable.focusNode.hasFocus, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('bounds TextField behind unbounded flex intermediaries', (
    tester,
  ) async {
    Future<Size> render({
      required String parentType,
      required Map<String, Object?> properties,
    }) async {
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithTextFieldInFlex(
                parentType: parentType,
                properties: properties,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      return tester.getSize(find.byType(TextField));
    }

    final rowSize = await render(
      parentType: 'flutter.widgets.Row',
      properties: {},
    );
    expect(rowSize.width, 240);
    expect(rowSize.height, greaterThan(0));

    final columnSize = await render(
      parentType: 'flutter.widgets.Column',
      properties: {
        'expands': {'kind': 'boolean', 'value': true},
      },
    );
    expect(columnSize.height, 120);
    expect(columnSize.width, greaterThan(0));
    final expanded = tester.widget<TextField>(find.byType(TextField));
    expect(expanded.expands, isTrue);
    expect(expanded.maxLines, isNull);
    expect(expanded.minLines, isNull);
    expect(tester.takeException(), isNull);
  });

  testWidgets('preserves pinned TextField defaults when leaves are omitted', (
    tester,
  ) async {
    const textFieldId = '73d9ec43-3d37-4304-8998-71fe51804284';
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithCenteredTextField(id: textFieldId, properties: {}),
          ),
        ),
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );
    await tester.pump();

    final field = tester.widget<TextField>(find.byType(TextField));
    expect(field.keyboardType, TextInputType.text);
    expect(field.textInputAction, isNull);
    expect(field.textCapitalization, TextCapitalization.none);
    expect(field.textAlign, TextAlign.start);
    expect(field.textAlignVertical, isNull);
    expect(field.textDirection, isNull);
    expect(field.readOnly, isFalse);
    expect(field.showCursor, isNull);
    expect(field.autofocus, isFalse);
    expect(field.obscuringCharacter, '•');
    expect(field.obscureText, isFalse);
    expect(field.autocorrect, isNull);
    expect(field.smartDashesType, SmartDashesType.enabled);
    expect(field.smartQuotesType, SmartQuotesType.enabled);
    expect(field.enableSuggestions, isTrue);
    expect(field.maxLines, 1);
    expect(field.minLines, isNull);
    expect(field.expands, isFalse);
    expect(field.maxLength, isNull);
    expect(field.maxLengthEnforcement, isNull);
    expect(field.enabled, isNull);
    expect(field.ignorePointers, isNull);
    expect(field.cursorWidth, 2);
    expect(field.cursorHeight, isNull);
    expect(field.cursorRadius, isNull);
    expect(field.cursorOpacityAnimates, isNull);
    expect(field.cursorColor, isNull);
    expect(field.cursorErrorColor, isNull);
    expect(field.keyboardAppearance, isNull);
    expect(field.scrollPadding, const EdgeInsets.all(20));
    expect(field.dragStartBehavior, DragStartBehavior.start);
    expect(field.enableInteractiveSelection, isTrue);
    expect(field.selectAllOnFocus, isNull);
    expect(field.onTapAlwaysCalled, isFalse);
    expect(field.mouseCursor, isNull);
    expect(field.clipBehavior, Clip.hardEdge);
    expect(field.restorationId, isNull);
    expect(
      field.stylusHandwritingEnabled,
      EditableText.defaultStylusHandwritingEnabled,
    );
    expect(field.enableIMEPersonalizedLearning, isTrue);
    expect(field.enableInlinePrediction, isNull);
    expect(field.canRequestFocus, isTrue);
    expect(tester.takeException(), isNull);
  });

  testWidgets('renders every reviewed ListView argument with real Flutter', (
    tester,
  ) async {
    const listViewId = '4febd2a9-b2ef-4f4d-8d67-2df913b46da2';
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithListView(
              properties: {
                'scrollDirection': {
                  'kind': 'enum',
                  'type': 'Axis',
                  'value': 'horizontal',
                },
                'reverse': {'kind': 'boolean', 'value': true},
                'primary': {'kind': 'boolean', 'value': false},
                'physics': {'kind': 'string', 'value': 'rangeMaintaining'},
                'shrinkWrap': {'kind': 'boolean', 'value': false},
                'padding': {
                  'kind': 'edgeInsetsDirectional',
                  'start': 1,
                  'top': 2,
                  'end': 3,
                  'bottom': 4,
                },
                'itemExtent': {'kind': 'double', 'value': 48.5},
                'addAutomaticKeepAlives': {'kind': 'boolean', 'value': false},
                'addRepaintBoundaries': {'kind': 'boolean', 'value': false},
                'addSemanticIndexes': {'kind': 'boolean', 'value': false},
                'scrollCacheExtent': {'kind': 'integer', 'value': 240},
                'semanticChildCount': {'kind': 'integer', 'value': 2},
                'dragStartBehavior': {
                  'kind': 'enum',
                  'type': 'DragStartBehavior',
                  'value': 'down',
                },
                'keyboardDismissBehavior': {
                  'kind': 'enum',
                  'type': 'ScrollViewKeyboardDismissBehavior',
                  'value': 'onDrag',
                },
                'restorationId': {'kind': 'string', 'value': 'primary-list'},
                'clipBehavior': {
                  'kind': 'enum',
                  'type': 'Clip',
                  'value': 'antiAlias',
                },
                'hitTestBehavior': {
                  'kind': 'enum',
                  'type': 'HitTestBehavior',
                  'value': 'translucent',
                },
              },
              children: [
                <String, Object?>{
                  'id': '7a6d767e-9d7b-4cf1-9bfd-83f75383a08f',
                  'type': 'flutter.widgets.Text',
                  'properties': <String, Object?>{
                    'data': {'kind': 'string', 'value': 'First'},
                  },
                  'slots': <String, Object?>{},
                },
                <String, Object?>{
                  'id': '99de11d2-8f49-4efc-bb79-c6d0e89fcae6',
                  'type': 'flutter.widgets.Text',
                  'properties': <String, Object?>{
                    'data': {'kind': 'string', 'value': 'Second'},
                  },
                  'slots': <String, Object?>{},
                },
              ],
            ),
          ),
        ),
      ),
    );

    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );
    await tester.pump();

    final node = find.byKey(const ValueKey('canvas-widget-$listViewId'));
    final finder = find.descendant(of: node, matching: find.byType(ListView));
    final listView = tester.widget<ListView>(finder);
    expect(listView.scrollDirection, Axis.horizontal);
    expect(listView.reverse, isTrue);
    expect(listView.primary, isFalse);
    expect(listView.physics, isA<RangeMaintainingScrollPhysics>());
    expect(listView.shrinkWrap, isFalse);
    expect(listView.padding, const EdgeInsetsDirectional.fromSTEB(1, 2, 3, 4));
    expect(listView.itemExtent, 48.5);
    final delegate = listView.childrenDelegate as SliverChildListDelegate;
    expect(delegate.addAutomaticKeepAlives, isFalse);
    expect(delegate.addRepaintBoundaries, isFalse);
    expect(delegate.addSemanticIndexes, isFalse);
    expect(listView.scrollCacheExtent?.value, 240);
    expect(listView.semanticChildCount, 2);
    expect(listView.dragStartBehavior, DragStartBehavior.down);
    expect(
      listView.keyboardDismissBehavior,
      ScrollViewKeyboardDismissBehavior.onDrag,
    );
    expect(listView.restorationId, 'primary-list');
    expect(listView.clipBehavior, Clip.antiAlias);
    expect(listView.hitTestBehavior, HitTestBehavior.translucent);
    expect(find.text('First'), findsOneWidget);
    expect(find.text('Second'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('bounds every required ListView axis under flex constraints', (
    tester,
  ) async {
    Future<Size> render({
      required String parentType,
      required Map<String, Object?> properties,
    }) async {
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithListView(
                properties: properties,
                children: const [],
                unboundedParentType: parentType,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      return tester.getSize(find.byType(ListView));
    }

    final vertical = await render(
      parentType: 'flutter.widgets.Column',
      properties: {},
    );
    expect(vertical.height, 120);

    final horizontal = await render(
      parentType: 'flutter.widgets.Row',
      properties: {
        'scrollDirection': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
      },
    );
    expect(horizontal.width, 240);

    final verticalCrossAxis = await render(
      parentType: 'flutter.widgets.Row',
      properties: {
        'shrinkWrap': {'kind': 'boolean', 'value': true},
      },
    );
    expect(verticalCrossAxis.width, 240);

    final horizontalCrossAxis = await render(
      parentType: 'flutter.widgets.Column',
      properties: {
        'scrollDirection': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
        'shrinkWrap': {'kind': 'boolean', 'value': true},
      },
    );
    expect(horizontalCrossAxis.height, 120);
  });

  testWidgets('exposes an empty ListView as a children insertion target', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(_modelWithListView(properties: {}, children: const [])),
        ),
      ),
    );
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
    await tester.pump();

    const listViewId = '4febd2a9-b2ef-4f4d-8d67-2df913b46da2';
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final rect = tester.getRect(
      find.byKey(const ValueKey('canvas-widget-$listViewId')),
    );
    final target = resolver!(
      ((rect.center.dx - surface.left) / surface.width * 1000000).round(),
      ((rect.center.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(target?.parentWidgetId, listViewId);
    expect(target?.slotName, 'children');
    expect(target?.insertionIndex, 0);
    expect(target?.zone?.isEmpty, isFalse);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'resolves populated ListView terminal and move edges for reverse and RTL',
    (tester) async {
      const listViewId = '4febd2a9-b2ef-4f4d-8d67-2df913b46da2';
      const firstId = '1b7b8bc1-f72e-4059-8d60-a32add0a62ef';
      const secondId = '493448dd-35a8-42ed-96b5-e14dff0764a2';

      Future<({int actualMoveEdge, int expectedMoveEdge})> resolve({
        required Axis axis,
        required bool reverse,
        required TextDirection direction,
      }) async {
        final properties = <String, Object?>{
          'reverse': {'kind': 'boolean', 'value': reverse},
          if (axis == Axis.horizontal)
            'scrollDirection': {
              'kind': 'enum',
              'type': 'Axis',
              'value': 'horizontal',
            },
        };
        final json = _modelWithListView(
          properties: properties,
          children: [
            _viewSizedBoxNode(firstId, width: 40, height: 30),
            _viewSizedBoxNode(secondId, width: 40, height: 30),
          ],
        );
        (json['profile']! as Map<String, Object?>)['locale'] =
            direction == TextDirection.rtl ? 'ar-SA' : 'en-US';
        CanvasDropResolver? dropResolver;
        CanvasMovePreviewResolver? moveResolver;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => dropResolver = value,
            onMovePreviewResolverChanged: (value) => moveResolver = value,
          ),
        );
        await tester.pump();

        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final listRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$listViewId')),
        );
        final retainedRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$secondId')),
        );
        final visuallyReversed = axis == Axis.vertical
            ? reverse
            : (direction == TextDirection.rtl) != reverse;
        final terminalPoint = axis == Axis.vertical
            ? Offset(
                listRect.center.dx,
                visuallyReversed ? listRect.top + 1 : listRect.bottom - 1,
              )
            : Offset(
                visuallyReversed ? listRect.left + 1 : listRect.right - 1,
                listRect.center.dy,
              );
        int micros(double value, double origin, double extent) =>
            ((value - origin) / extent * 1000000).round();
        final terminal = dropResolver!(
          micros(terminalPoint.dx, surface.left, surface.width),
          micros(terminalPoint.dy, surface.top, surface.height),
        );
        expect(terminal?.parentWidgetId, listViewId);
        expect(terminal?.slotName, 'children');
        expect(terminal?.insertionIndex, 2);
        expect(terminal?.zone?.isEmpty, isFalse);

        final move = moveResolver!(firstId, listViewId, 'children', 1);
        expect(move?.parentWidgetId, listViewId);
        expect(move?.slotName, 'children');
        expect(move?.insertionIndex, 1);
        expect(move?.zone?.isEmpty, isFalse);
        final zone = move!.zone!;
        final actualMoveEdge = axis == Axis.vertical
            ? (zone.topMicros + zone.bottomMicros) ~/ 2
            : (zone.leftMicros + zone.rightMicros) ~/ 2;
        final physicalEdge = axis == Axis.vertical
            ? (visuallyReversed ? retainedRect.top : retainedRect.bottom)
            : (visuallyReversed ? retainedRect.left : retainedRect.right);
        final expectedMoveEdge = axis == Axis.vertical
            ? micros(physicalEdge, surface.top, surface.height)
            : micros(physicalEdge, surface.left, surface.width);
        expect(tester.takeException(), isNull);
        return (
          actualMoveEdge: actualMoveEdge,
          expectedMoveEdge: expectedMoveEdge,
        );
      }

      final verticalReverse = await resolve(
        axis: Axis.vertical,
        reverse: true,
        direction: TextDirection.ltr,
      );
      expect(
        verticalReverse.actualMoveEdge,
        closeTo(verticalReverse.expectedMoveEdge, 2),
      );

      final rtlForward = await resolve(
        axis: Axis.horizontal,
        reverse: false,
        direction: TextDirection.rtl,
      );
      expect(
        rtlForward.actualMoveEdge,
        closeTo(rtlForward.expectedMoveEdge, 2),
      );

      final rtlReverse = await resolve(
        axis: Axis.horizontal,
        reverse: true,
        direction: TextDirection.rtl,
      );
      expect(
        rtlReverse.actualMoveEdge,
        closeTo(rtlReverse.expectedMoveEdge, 2),
      );
    },
  );

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
    'renders AspectRatio with an optional child, outlines it, and exposes only its empty slot',
    (tester) async {
      const aspectRatioId = '4a07880d-54a7-44c6-97e4-a88df4e44e67';
      CanvasDropResolver? resolver;

      Future<void> pump({
        required Map<String, Object?>? child,
        String? selectedWidgetId,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredAspectRatio(aspectRatio: 2.0, child: child),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
      }

      Finder nodeFinder() =>
          find.byKey(const ValueKey('canvas-widget-$aspectRatioId'));
      Finder ratioFinder() => find
          .descendant(of: nodeFinder(), matching: find.byType(AspectRatio))
          .first;

      await pump(child: null);

      final emptyRatio = tester.widget<AspectRatio>(ratioFinder());
      expect(emptyRatio.aspectRatio, 2.0);
      expect(emptyRatio.child, isNull);
      final emptySize = tester.getSize(nodeFinder());
      expect(emptySize.width, greaterThan(0));
      expect(emptySize.height, greaterThan(0));
      expect(emptySize.width / emptySize.height, closeTo(2.0, 0.001));
      expect(
        find.byKey(const ValueKey('canvas-widget-outline-$aspectRatioId')),
        findsOneWidget,
      );
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$aspectRatioId')),
        findsNothing,
      );

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final rect = tester.getRect(nodeFinder());
      final point = rect.center;
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(target?.parentWidgetId, aspectRatioId);
      expect(target?.slotName, 'child');
      expect(target?.insertionIndex, 0);
      expect(target?.zone?.isEmpty, isFalse);

      await pump(child: null, selectedWidgetId: aspectRatioId);
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$aspectRatioId')),
        findsOneWidget,
      );
      expect(tester.widget<AspectRatio>(ratioFinder()).child, isNull);

      final source = _modelJsonForView();
      final sourceRoot = source['root']! as Map<String, Object?>;
      final text = _findNode(sourceRoot, 'flutter.widgets.Text');
      await pump(child: text);
      expect(tester.widget<AspectRatio>(ratioFinder()).child, isNotNull);
      expect(find.text('Hello'), findsOneWidget);

      final occupiedSurface = tester.getRect(find.byType(CanvasDocumentView));
      final occupiedRect = tester.getRect(nodeFinder());
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
    },
  );

  testWidgets(
    'renders real Baseline values and both TextBaseline modes with Flutter layout',
    (tester) async {
      const baselineId = '0197b4c0-11f0-45b1-bfe8-cc84ef5a6fae';
      const childId = '0197b4c0-11f0-45b2-a191-d9b1b00a21ce';
      final child = _viewSizedBoxNode(childId, width: 40, height: 10);

      Future<({Baseline widget, RenderBaseline render, Rect childRect})> pump({
        required double baseline,
        required String baselineType,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredBaseline(
                  baseline: baseline,
                  baselineType: baselineType,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        await tester.pump();
        final finder = find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$baselineId')),
              matching: find.byType(Baseline),
            )
            .first;
        final widget = tester.widget<Baseline>(finder);
        final render = tester.renderObject<RenderBaseline>(finder);
        final renderChild = render.child!;
        return (
          widget: widget,
          render: render,
          childRect: MatrixUtils.transformRect(
            renderChild.getTransformTo(render),
            Offset.zero & renderChild.size,
          ),
        );
      }

      final alphabetic = await pump(baseline: 24, baselineType: 'alphabetic');
      expect(alphabetic.widget.baseline, 24);
      expect(alphabetic.widget.baselineType, TextBaseline.alphabetic);
      expect(alphabetic.render.size, const Size(40, 24));
      expect(alphabetic.childRect, const Rect.fromLTWH(0, 14, 40, 10));

      final ideographic = await pump(
        baseline: 36.5,
        baselineType: 'ideographic',
      );
      expect(ideographic.widget.baseline, 36.5);
      expect(ideographic.widget.baselineType, TextBaseline.ideographic);
      expect(ideographic.render.size, const Size(40, 36.5));
      expect(ideographic.childRect, const Rect.fromLTWH(0, 26.5, 40, 10));

      final negative = await pump(baseline: -6, baselineType: 'alphabetic');
      expect(negative.widget.baseline, -6);
      expect(negative.render.size, const Size(40, 0));
      expect(negative.childRect, const Rect.fromLTWH(0, -16, 40, 10));
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$baselineId'),
        ),
        findsOneWidget,
        reason: 'a legal finite baseline can collapse the real RenderBaseline',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps empty and child-bearing Baseline selectable with exact child DnD',
    (tester) async {
      const baselineId = '0197b4c0-11f0-45b1-bfe8-cc84ef5a6fae';
      const childId = '0197b4c0-11f0-45b2-a191-d9b1b00a21ce';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;

      Future<void> pump({
        required double baseline,
        required Map<String, Object?>? child,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredBaseline(
                  baseline: baseline,
                  baselineType: 'alphabetic',
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          StatefulBuilder(
            builder: (context, setState) => CanvasModelApp(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        await tester.pump();
      }

      CanvasDropTarget? resolveAt(Offset point) {
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      await pump(baseline: 24, child: null);
      final emptyNode = find.byKey(const ValueKey('canvas-widget-$baselineId'));
      final emptyTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$baselineId'),
      );
      expect(tester.getSize(emptyNode), Size.zero);
      expect(emptyTarget, findsOneWidget);
      expect(tester.getSize(emptyTarget), const Size(36, 36));
      await tester.tap(emptyTarget);
      await tester.pump();
      expect(selectedWidgetId, baselineId);
      final emptyDrop = resolveAt(tester.getRect(emptyTarget).center);
      expect(emptyDrop?.parentWidgetId, baselineId);
      expect(emptyDrop?.slotName, 'child');
      expect(emptyDrop?.insertionIndex, 0);
      expect(emptyDrop?.zone?.isEmpty, isFalse);

      selectedWidgetId = null;
      await pump(
        baseline: 24,
        child: _viewSizedBoxNode(childId, width: 40, height: 10),
      );
      final occupiedNode = find.byKey(
        const ValueKey('canvas-widget-$baselineId'),
      );
      final occupiedRect = tester.getRect(occupiedNode);
      final occupiedRender = tester.renderObject<RenderBaseline>(
        find
            .descendant(of: occupiedNode, matching: find.byType(Baseline))
            .first,
      );
      expect(occupiedRender.size, const Size(40, 24));
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$baselineId'),
        ),
        findsNothing,
      );
      await tester.tapAt(Offset(occupiedRect.center.dx, occupiedRect.top + 4));
      await tester.pump();
      expect(selectedWidgetId, baselineId);
      expect(resolveAt(occupiedRect.center)?.parentWidgetId, isNot(baselineId));

      selectedWidgetId = null;
      await pump(
        baseline: -6,
        child: _viewSizedBoxNode(childId, width: 40, height: 10),
      );
      final collapsedTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$baselineId'),
      );
      final collapsedRender = tester.renderObject<RenderBaseline>(
        find
            .descendant(of: occupiedNode, matching: find.byType(Baseline))
            .first,
      );
      expect(collapsedRender.size, const Size(40, 0));
      expect(collapsedTarget, findsOneWidget);
      await tester.tap(collapsedTarget);
      await tester.pump();
      expect(selectedWidgetId, baselineId);
      expect(
        resolveAt(tester.getRect(collapsedTarget).center)?.parentWidgetId,
        isNot(baselineId),
        reason: 'an occupied Baseline child slot cannot accept another child',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders the real IntrinsicHeight and preserves its measured child size',
    (tester) async {
      const intrinsicId = '15aa2055-201d-4f3e-bd50-24cc60fa50c0';
      const childId = 'cf781f2a-8d2e-4a88-a05a-9758e5e911d8';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredIntrinsicHeight(
                child: _viewSizedBoxNode(childId, width: 40, height: 10),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$intrinsicId'));
      final intrinsicFinder = find
          .descendant(of: node, matching: find.byType(IntrinsicHeight))
          .first;
      final widget = tester.widget<IntrinsicHeight>(intrinsicFinder);
      final render = tester.renderObject<RenderIntrinsicHeight>(
        intrinsicFinder,
      );
      expect(widget.child, isNotNull);
      expect(render.size, const Size(40, 10));
      expect(render.child!.size, const Size(40, 10));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps empty and collapsed IntrinsicHeight selectable without changing layout',
    (tester) async {
      const intrinsicId = '15aa2055-201d-4f3e-bd50-24cc60fa50c0';
      const childId = 'cf781f2a-8d2e-4a88-a05a-9758e5e911d8';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;

      Future<void> pump(Map<String, Object?>? child) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(_modelWithCenteredIntrinsicHeight(child: child)),
            ),
          ),
        );
        await tester.pumpWidget(
          StatefulBuilder(
            builder: (context, setState) => CanvasModelApp(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        await tester.pump();
      }

      CanvasDropTarget? resolveAt(Offset point) {
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      RenderIntrinsicHeight intrinsicRender() =>
          tester.renderObject<RenderIntrinsicHeight>(
            find
                .descendant(
                  of: find.byKey(const ValueKey('canvas-widget-$intrinsicId')),
                  matching: find.byType(IntrinsicHeight),
                )
                .first,
          );

      await pump(null);
      final emptyTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$intrinsicId'),
      );
      expect(intrinsicRender().size, Size.zero);
      expect(emptyTarget, findsOneWidget);
      expect(tester.getSize(emptyTarget), const Size(36, 36));
      final surfaceRect = tester.getRect(find.byType(CanvasDocumentView));
      final emptyTargetRect = tester.getRect(emptyTarget);
      expect(surfaceRect.intersect(emptyTargetRect), emptyTargetRect);
      await tester.tap(emptyTarget);
      await tester.pump();
      expect(selectedWidgetId, intrinsicId);
      final emptyDrop = resolveAt(emptyTargetRect.center);
      expect(emptyDrop?.parentWidgetId, intrinsicId);
      expect(emptyDrop?.slotName, 'child');
      expect(emptyDrop?.insertionIndex, 0);
      expect(emptyDrop?.zone?.isEmpty, isFalse);
      expect(intrinsicRender().size, Size.zero);

      selectedWidgetId = null;
      await pump(_viewSizedBoxNode(childId, width: 40, height: 10));
      final occupiedNode = find.byKey(
        const ValueKey('canvas-widget-$intrinsicId'),
      );
      expect(intrinsicRender().size, const Size(40, 10));
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$intrinsicId'),
        ),
        findsNothing,
      );
      final occupiedRect = tester.getRect(occupiedNode);
      await tester.tapAt(occupiedRect.center);
      await tester.pump();
      expect(selectedWidgetId, childId);
      expect(
        resolveAt(occupiedRect.center)?.parentWidgetId,
        isNot(intrinsicId),
      );

      selectedWidgetId = null;
      await pump(<String, Object?>{
        'id': childId,
        'type': 'flutter.widgets.Padding',
        'properties': <String, Object?>{
          'padding': {
            'kind': 'edgeInsets',
            'left': 0,
            'top': 0,
            'right': 0,
            'bottom': 0,
          },
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      });
      final collapsedTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$intrinsicId'),
      );
      expect(intrinsicRender().size, Size.zero);
      expect(collapsedTarget, findsOneWidget);
      expect(tester.getSize(collapsedTarget), const Size(36, 36));
      final collapsedTargetRect = tester.getRect(collapsedTarget);
      expect(surfaceRect.intersect(collapsedTargetRect), collapsedTargetRect);
      await tester.tap(collapsedTarget);
      await tester.pump();
      expect(selectedWidgetId, intrinsicId);
      expect(
        resolveAt(collapsedTargetRect.center)?.parentWidgetId,
        isNot(intrinsicId),
        reason: 'an occupied IntrinsicHeight child slot cannot accept another',
      );
      expect(intrinsicRender().size, Size.zero);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real Opacity at exact alpha endpoints while preserving hit testing and outer Designer control',
    (tester) async {
      const opacityId = '47f754c8-9fcb-480c-9578-87cda83bd4d5';
      final source = _modelJsonForView();
      final sourceRoot = source['root']! as Map<String, Object?>;
      final text = _findNode(sourceRoot, 'flutter.widgets.Text');
      final textId = text['id']! as String;
      Future<RenderOpacity> pump({
        required double opacity,
        required bool? alwaysIncludeSemantics,
        String? selected,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredOpacity(
                  opacity: opacity,
                  alwaysIncludeSemantics: alwaysIncludeSemantics,
                  child: text,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: selected,
              onSelected: (_) {},
            ),
          ),
        );
        await tester.pump();
        final opacityFinder = find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$opacityId')),
              matching: find.byType(Opacity),
            )
            .first;
        return tester.renderObject<RenderOpacity>(opacityFinder);
      }

      final hidden = await pump(
        opacity: 0,
        alwaysIncludeSemantics: null,
        selected: opacityId,
      );
      expect(hidden.opacity, 0);
      expect(hidden.alwaysIncludeSemantics, isFalse);
      expect(
        (hidden.updateCompositedLayer(oldLayer: null) as OpacityLayer).alpha,
        0,
      );
      final selectionOutline = find.byKey(
        const ValueKey('canvas-selection-outline-$opacityId'),
      );
      expect(selectionOutline, findsOneWidget);
      final renderedOpacity = find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$opacityId')),
            matching: find.byType(Opacity),
          )
          .first;
      expect(
        find.ancestor(of: renderedOpacity, matching: selectionOutline),
        findsOneWidget,
        reason: 'the Designer outline wrapper must remain outside Opacity',
      );
      expect(
        find.ancestor(of: selectionOutline, matching: find.byType(Opacity)),
        findsNothing,
      );
      expect(
        find.bySemanticsLabel(RegExp('Opacity $opacityId')),
        findsOneWidget,
      );
      expect(find.bySemanticsLabel(RegExp('Text $textId')), findsNothing);
      final hitTest = BoxHitTestResult();
      expect(
        hidden.hitTest(hitTest, position: hidden.size.center(Offset.zero)),
        isTrue,
        reason: 'opacity zero must not disable Flutter hit testing',
      );
      expect(
        hitTest.path.any((entry) => identical(entry.target, hidden)),
        isTrue,
      );

      final semantic = await pump(opacity: 0, alwaysIncludeSemantics: true);
      expect(semantic.alwaysIncludeSemantics, isTrue);
      expect(find.bySemanticsLabel(RegExp('Text $textId')), findsOneWidget);

      final fractional = await pump(
        opacity: 0.5,
        alwaysIncludeSemantics: false,
      );
      expect(fractional.opacity, 0.5);
      expect(
        (fractional.updateCompositedLayer(oldLayer: null) as OpacityLayer)
            .alpha,
        128,
      );
      expect(fractional.isRepaintBoundary, isTrue);

      final opaque = await pump(opacity: 1, alwaysIncludeSemantics: null);
      expect(opaque.opacity, 1);
      expect(
        (opaque.updateCompositedLayer(oldLayer: null) as OpacityLayer).alpha,
        255,
      );
      expect(find.byType(AnimatedOpacity), findsNothing);
    },
  );

  testWidgets(
    'keeps empty Opacity at zero layout with a selectable child drop target',
    (tester) async {
      const opacityId = '47f754c8-9fcb-480c-9578-87cda83bd4d5';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredOpacity(
                opacity: 0,
                alwaysIncludeSemantics: null,
                child: null,
              ),
            ),
          ),
        ),
      );
      String? selectedWidgetId;
      CanvasDropResolver? resolver;
      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$opacityId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$opacityId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, opacityId);
      expect(tester.getSize(rendered), Size.zero);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, opacityId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'renders every Container argument through real Flutter objects and keeps its outline outside transform',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
      final json = _modelJsonForView();
      final sourceRoot = json['root']! as Map<String, Object?>;
      final text = _findNode(sourceRoot, 'flutter.widgets.Text');
      final properties = <String, Object?>{
        'alignment': _viewAlignment(
          basis: 'directional',
          horizontal: 0.5,
          vertical: -0.25,
        ),
        'padding': {
          'kind': 'edgeInsetsDirectional',
          'start': 5,
          'top': 6,
          'end': 7,
          'bottom': 8,
        },
        'isAntiAlias': {'kind': 'boolean', 'value': false},
        'decoration': _viewBoxDecoration(
          color: _viewThemeColor('material.colorScheme.primaryContainer'),
          border: _viewPhysicalBorder(),
          borderRadius: _viewDirectionalRadius(),
          boxShadow: [_viewBoxShadow()],
          gradient: _viewLinearGradient(),
          backgroundBlendMode: 'multiply',
        ),
        'foregroundDecoration': _viewBoxDecoration(
          color: _viewLiteralColor('0x22112233'),
          shape: 'circle',
        ),
        'width': {'kind': 'integer', 'value': 180},
        'height': {'kind': 'double', 'value': 100.5},
        'constraints': {
          'kind': 'boxConstraints',
          'minWidth': 120,
          'maxWidth': 240,
          'minHeight': 80,
          'maxHeight': null,
        },
        'margin': {
          'kind': 'edgeInsets',
          'left': 10,
          'top': 12,
          'right': 14,
          'bottom': 16,
        },
        'transform': {
          'kind': 'matrix4',
          'storage': <Object?>[
            1,
            0,
            0,
            0,
            0,
            1,
            0,
            0,
            0,
            0,
            1,
            0,
            24,
            -12,
            0,
            1,
          ],
        },
        'transformAlignment': _viewAlignment(horizontal: 1, vertical: -1),
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'hardEdge'},
      };
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredContainer(properties: properties, child: text),
            ),
          ),
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: containerId,
            onSelected: (_) {},
          ),
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$containerId'));
      final containerFinder = find
          .descendant(of: node, matching: find.byType(Container))
          .first;
      final container = tester.widget<Container>(containerFinder);
      expect(container.alignment, const AlignmentDirectional(0.5, -0.25));
      expect(
        container.padding,
        const EdgeInsetsDirectional.fromSTEB(5, 6, 7, 8),
      );
      expect(container.color, isNull);
      expect(container.isAntiAlias, isFalse);
      expect(
        container.constraints,
        const BoxConstraints(
          minWidth: 180,
          maxWidth: 180,
          minHeight: 100.5,
          maxHeight: 100.5,
        ),
        reason: 'Container folds width and height into constraints via tighten',
      );
      expect(container.margin, const EdgeInsets.fromLTRB(10, 12, 14, 16));
      expect(container.transform!.storage[12], 24);
      expect(container.transform!.storage[13], -12);
      expect(container.transformAlignment, Alignment.topRight);
      expect(container.clipBehavior, Clip.hardEdge);
      expect(container.child, isNotNull);
      expect(find.text('Hello'), findsOneWidget);

      final context = tester.element(containerFinder);
      final decoration = container.decoration! as BoxDecoration;
      expect(decoration.color, Theme.of(context).colorScheme.primaryContainer);
      expect(decoration.border, isA<Border>());
      expect(decoration.borderRadius, isA<BorderRadiusDirectional>());
      expect(decoration.boxShadow, hasLength(1));
      expect(decoration.boxShadow!.single.blurStyle, BlurStyle.outer);
      expect(decoration.gradient, isA<LinearGradient>());
      final gradient = decoration.gradient! as LinearGradient;
      expect(gradient.begin, Alignment.topLeft);
      expect(gradient.end, AlignmentDirectional.bottomEnd);
      expect(gradient.tileMode, TileMode.mirror);
      expect(gradient.transform, isA<GradientRotation>());
      expect(decoration.backgroundBlendMode, BlendMode.multiply);
      final foreground = container.foregroundDecoration! as BoxDecoration;
      expect(foreground.color, const Color(0x22112233));
      expect(foreground.shape, BoxShape.circle);

      final outline = find.byKey(
        const ValueKey('canvas-widget-outline-$containerId'),
      );
      final guides = find.byKey(
        const ValueKey('canvas-container-insets-guides-$containerId'),
      );
      expect(outline, findsOneWidget);
      expect(guides, findsOneWidget);
      expect(
        find.ancestor(of: containerFinder, matching: outline),
        findsOneWidget,
        reason: 'the layout outline must stay outside Container.transform',
      );
      expect(
        find.descendant(of: containerFinder, matching: find.byType(Transform)),
        findsOneWidget,
        reason: 'the actual Container keeps its framework paint transform',
      );
    },
  );

  testWidgets(
    'renders every reviewed Container decoration union and constructor default',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';

      Future<Container> render(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredContainer(
                  properties: properties,
                  child: null,
                ),
              ),
            ),
          ),
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
        await tester.pump();
        return tester.widget<Container>(
          find
              .descendant(
                of: find.byKey(const ValueKey('canvas-widget-$containerId')),
                matching: find.byType(Container),
              )
              .first,
        );
      }

      final radialContainer = await render({
        'decoration': _viewBoxDecoration(
          border: _viewDirectionalBorder(),
          borderRadius: _viewPhysicalRadius(),
          gradient: _viewRadialGradient(),
        ),
      });
      final radialDecoration = radialContainer.decoration! as BoxDecoration;
      expect(radialDecoration.border, isA<BorderDirectional>());
      expect(radialDecoration.borderRadius, isA<BorderRadius>());
      final radial = radialDecoration.gradient! as RadialGradient;
      expect(radial.center, const Alignment(0.1, -0.2));
      expect(radial.radius, 0.75);
      expect(radial.focal, const AlignmentDirectional(0.4, 0.3));
      expect(radial.focalRadius, 0.15);
      expect(radial.tileMode, TileMode.decal);
      expect(radial.transform, isNull);
      expect(radial.colors.first, const Color(0xff102030));
      expect(radial.stops, const [0.0, 1.0]);

      final sweepContainer = await render({
        'decoration': _viewBoxDecoration(gradient: _viewSweepGradient()),
      });
      final sweep =
          (sweepContainer.decoration! as BoxDecoration).gradient!
              as SweepGradient;
      expect(sweep.center, const AlignmentDirectional(-0.2, 0.4));
      expect(sweep.startAngle, 0.25);
      expect(sweep.endAngle, 5.75);
      expect(sweep.tileMode, TileMode.repeated);
      expect(sweep.transform, isA<GradientRotation>());

      final plain = await render({
        'color': {'kind': 'color', 'argb': '0x7F123456'},
      });
      expect(plain.color, const Color(0x7f123456));
      expect(plain.decoration, isNull);
      expect(plain.isAntiAlias, isTrue);
      expect(plain.clipBehavior, Clip.none);
    },
  );

  testWidgets(
    'renders resolved DecorationImage arguments in RTL without disturbing selection or DnD overlays',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
      final resourceId = sha256Hex(_viewPng8);
      final resources = CanvasImageResourceBundle.fromResources([
        CanvasImageResource(
          resourceId: resourceId,
          mediaType: 'image/png',
          pixelWidth: 8,
          pixelHeight: 8,
          encodedBytes: _viewPng8,
        ),
      ]);
      final colorFilters = <Map<String, Object?>>[
        {
          'kind': 'mode',
          'color': _viewLiteralColor('0xFF336699'),
          'blendMode': 'srcATop',
        },
        {
          'kind': 'matrix',
          'values': <Object?>[
            1,
            0,
            0,
            0,
            0,
            0,
            1,
            0,
            0,
            0,
            0,
            0,
            1,
            0,
            0,
            0,
            0,
            0,
            1,
            0,
          ],
        },
        {'kind': 'linearToSrgbGamma'},
        {'kind': 'srgbToLinearGamma'},
        {'kind': 'saturation', 'value': 0.4},
      ];
      String? selected;

      for (final colorFilter in colorFilters) {
        final json = _modelWithCenteredContainer(
          properties: {
            'width': {'kind': 'integer', 'value': 120},
            'height': {'kind': 'integer', 'value': 80},
            'decoration': _viewBoxDecoration(
              image: _viewDecorationImage(
                image: _viewImageProvider(
                  kind: 'exactAsset',
                  exactScale: 2,
                  resize: {
                    'width': 8,
                    'height': 8,
                    'policy': 'exact',
                    'allowUpscaling': false,
                  },
                  resolution: {
                    'kind': 'resolved',
                    'resourceId': resourceId,
                    'resolvedScale': 2,
                  },
                ),
                onError: true,
                colorFilter: colorFilter,
                fit: 'fill',
                alignment: _viewNestedAlignment(
                  basis: 'directional',
                  horizontal: -1,
                  vertical: 0.25,
                ),
                centerSlice: {'left': 1, 'top': 1, 'right': 3, 'bottom': 3},
                repeat: 'repeatX',
                matchTextDirection: true,
                scale: 1,
                opacity: 0.65,
                filterQuality: 'high',
                invertColors: true,
                isAntiAlias: true,
              ),
            ),
          },
          child: null,
        );
        (json['profile']! as Map<String, Object?>)['locale'] = 'ar-SA';
        final model = CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(json))),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            imageResources: resources,
            selectedWidgetId: containerId,
            onSelected: (value) => selected = value,
            dropHoverTarget: const CanvasDropTarget(
              parentWidgetId: containerId,
              slotName: 'child',
              insertionIndex: 0,
              zone: CanvasDropZone(
                leftMicros: 0,
                topMicros: 0,
                rightMicros: 1000000,
                bottomMicros: 1000000,
              ),
            ),
            dropIndicatorKind: CanvasDropIndicatorKind.widgetMove,
          ),
        );
        await tester.pump();

        final node = find.byKey(const ValueKey('canvas-widget-$containerId'));
        final container = tester.widget<Container>(
          find.descendant(of: node, matching: find.byType(Container)).first,
        );
        final image = (container.decoration! as BoxDecoration).image!;
        expect(image.image, isA<ResizeImage>());
        final resized = image.image as ResizeImage;
        expect(resized.width, 8);
        expect(resized.height, 8);
        expect(resized.policy, ResizeImagePolicy.exact);
        expect(resized.allowUpscaling, isFalse);
        final memory = resized.imageProvider as MemoryImage;
        expect(memory.scale, 2);
        expect(image.colorFilter, isNotNull);
        expect(image.fit, BoxFit.fill);
        expect(image.alignment, const AlignmentDirectional(-1, 0.25));
        expect(image.centerSlice, const Rect.fromLTRB(1, 1, 3, 3));
        expect(image.repeat, ImageRepeat.repeatX);
        expect(image.matchTextDirection, isTrue);
        expect(image.scale, 1);
        expect(image.opacity, 0.65);
        expect(image.filterQuality, FilterQuality.high);
        expect(image.invertColors, isTrue);
        expect(image.isAntiAlias, isTrue);
        expect(Directionality.of(tester.element(node)), TextDirection.rtl);
        expect(
          find.byKey(const ValueKey('canvas-selection-outline-$containerId')),
          findsOneWidget,
        );
        expect(
          find.byKey(const ValueKey('canvas-widget-move-preview-zone')),
          findsOneWidget,
        );
        await tester.tap(node);
        expect(selected, containerId);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets('preserves serialized DecorationImage onError presence', (
    tester,
  ) async {
    const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
    final resourceId = sha256Hex(_viewPng8);
    final resources = CanvasImageResourceBundle.fromResources([
      CanvasImageResource(
        resourceId: resourceId,
        mediaType: 'image/png',
        pixelWidth: 8,
        pixelHeight: 8,
        encodedBytes: _viewPng8,
      ),
    ]);

    for (final configured in const [false, true]) {
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredContainer(
                properties: {
                  'decoration': _viewBoxDecoration(
                    image: _viewDecorationImage(
                      image: _viewImageProvider(
                        resolution: {
                          'kind': 'resolved',
                          'resourceId': resourceId,
                          'resolvedScale': 1,
                        },
                      ),
                      onError: configured,
                    ),
                  ),
                },
                child: null,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          imageResources: resources,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$containerId'));
      final container = tester.widget<Container>(
        find.descendant(of: node, matching: find.byType(Container)).first,
      );
      final image = (container.decoration! as BoxDecoration).image!;
      expect(
        image.onError,
        configured ? isNotNull : isNull,
        reason: 'Canvas must preserve callback presence without an identifier',
      );
      expect(tester.takeException(), isNull);
    }
  });

  testWidgets(
    'paints unavailable and runner-rejected placeholders with concrete accessible status',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
      const packageName = 'image_pack';
      const assetName = 'assets/images/panel.png';
      const assetIdentity = 'package:image_pack:assets/images/panel.png';
      final semantics = tester.ensureSemantics();

      for (final issue in const <(String, String)>[
        ('missing', 'The declared project image does not exist'),
        ('unreadable', 'The project image could not be read'),
        ('corrupt', 'The PNG payload could not be decoded'),
      ]) {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredContainer(
                  properties: {
                    'width': {'kind': 'integer', 'value': 120},
                    'height': {'kind': 'integer', 'value': 80},
                    'decoration': _viewBoxDecoration(
                      image: _viewDecorationImage(
                        image: _viewImageProvider(
                          assetName: assetName,
                          packageName: packageName,
                          resolution: {
                            'kind': 'unavailable',
                            'code': issue.$1,
                            'reason': issue.$2,
                          },
                        ),
                        centerSlice: {
                          'left': 40,
                          'top': 40,
                          'right': 80,
                          'bottom': 80,
                        },
                      ),
                    ),
                  },
                  child: null,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final node = find.byKey(const ValueKey('canvas-widget-$containerId'));
        final container = tester.widget<Container>(
          find.descendant(of: node, matching: find.byType(Container)).first,
        );
        final image = (container.decoration! as BoxDecoration).image!;
        expect(image.image, isA<MemoryImage>());
        expect(
          image.centerSlice,
          isNull,
          reason: 'placeholder dimensions are intentionally untrusted',
        );
        expect(
          find.bySemanticsLabel(
            RegExp(
              'Image preview unavailable for ${RegExp.escape(assetIdentity)}.*'
              'Status ${issue.$1}.*Reason: ${RegExp.escape(issue.$2)}',
            ),
          ),
          findsOneWidget,
        );
        expect(tester.takeException(), isNull);
      }

      final rejectedResourceId = sha256Hex(_viewPng8);
      final rejectedModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredContainer(
                properties: {
                  'width': {'kind': 'integer', 'value': 120},
                  'height': {'kind': 'integer', 'value': 80},
                  'decoration': _viewBoxDecoration(
                    image: _viewDecorationImage(
                      image: _viewImageProvider(
                        assetName: assetName,
                        packageName: packageName,
                        resolution: {
                          'kind': 'resolved',
                          'resourceId': rejectedResourceId,
                          'resolvedScale': 1,
                        },
                      ),
                      centerSlice: {
                        'left': 40,
                        'top': 40,
                        'right': 80,
                        'bottom': 80,
                      },
                    ),
                  ),
                },
                child: null,
              ),
            ),
          ),
        ),
      );
      final rejectedResources = CanvasImageResourceBundle.fromResources(
        const [],
        rejections: [
          CanvasImageResourceRejection.encodedContent(rejectedResourceId),
        ],
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: rejectedModel,
          imageResources: rejectedResources,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final rejectedNode = find.byKey(
        const ValueKey('canvas-widget-$containerId'),
      );
      final rejectedContainer = tester.widget<Container>(
        find
            .descendant(of: rejectedNode, matching: find.byType(Container))
            .first,
      );
      final rejectedImage =
          (rejectedContainer.decoration! as BoxDecoration).image!;
      expect(rejectedImage.image, isA<MemoryImage>());
      expect(rejectedImage.centerSlice, isNull);
      expect(
        find.bySemanticsLabel(
          RegExp(
            'Image preview unavailable for ${RegExp.escape(assetIdentity)}.*'
            'Status corrupt.*Reason: '
            '${RegExp.escape(CanvasImageResourceRejectionKind.encodedContent.reason)}',
          ),
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
      semantics.dispose();
    },
  );

  testWidgets(
    'resolves Container padding and margin guides with distinct stable styles',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';

      Future<List<_RecordedLine>> lines(TextDirection direction) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredContainer(
                  properties: {
                    'width': {'kind': 'integer', 'value': 120},
                    'height': {'kind': 'integer', 'value': 100},
                    'margin': {
                      'kind': 'edgeInsets',
                      'left': 10,
                      'top': 20,
                      'right': 30,
                      'bottom': 40,
                    },
                    'padding': {
                      'kind': 'edgeInsetsDirectional',
                      'start': 5,
                      'top': 6,
                      'end': 7,
                      'bottom': 8,
                    },
                  },
                  child: null,
                ),
              ),
            ),
          ),
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
        await tester.pump();
        final guide = find.byKey(
          const ValueKey('canvas-container-insets-guides-$containerId'),
        );
        expect(tester.getSize(guide), const Size(160, 160));
        return _drawLines(
          _recordPainter(
            _foregroundPainter(
              tester,
              'canvas-container-insets-guides-$containerId',
            ),
            const Size(160, 160),
          ),
        );
      }

      final ltr = await lines(TextDirection.ltr);
      final rtl = await lines(TextDirection.rtl);
      const paddingArgb = 0xffd97706;
      const marginArgb = 0xff00897b;
      List<_RecordedLine> byColor(List<_RecordedLine> all, int argb) => [
        for (final line in all)
          if (line.paint.color.toARGB32() == argb) line,
      ];

      final ltrPadding = byColor(ltr, paddingArgb);
      final rtlPadding = byColor(rtl, paddingArgb);
      final ltrMargin = byColor(ltr, marginArgb);
      expect(ltrPadding, hasLength(12));
      expect(rtlPadding, hasLength(12));
      expect(ltrMargin.length, greaterThan(12));
      expect(
        _lineEndpoints([
          for (var index = 0; index < ltrPadding.length; index += 3)
            ltrPadding[index],
        ]),
        const [
          (Offset(10, 69), Offset(15, 69)),
          (Offset(123, 69), Offset(130, 69)),
          (Offset(69, 20), Offset(69, 26)),
          (Offset(69, 112), Offset(69, 120)),
        ],
      );
      expect(
        _lineEndpoints([
          for (var index = 0; index < rtlPadding.length; index += 3)
            rtlPadding[index],
        ]),
        const [
          (Offset(10, 69), Offset(17, 69)),
          (Offset(125, 69), Offset(130, 69)),
          (Offset(71, 20), Offset(71, 26)),
          (Offset(71, 112), Offset(71, 120)),
        ],
      );
    },
  );

  testWidgets(
    'keeps an empty zero-size Container at zero layout while exposing its optional child target',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(jsonEncode(_modelWithEmptyContainerInRow())),
        ),
      );
      String? selectedWidgetId;
      CanvasDropResolver? resolver;

      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$containerId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$containerId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));

      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, containerId);
      expect(tester.getSize(rendered), Size.zero);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, containerId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
    },
  );

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
      final spacerTarget = resolver!(
        xMicros,
        yMicros,
        CanvasPaletteDragSource(
          token: 'spacer-source',
          widgetType: canvasSpacerWidgetType,
          traits: const {},
        ),
      );
      expect(spacerTarget?.parentWidgetId, rowId);
      expect(spacerTarget?.slotName, 'children');
      expect(spacerTarget?.insertionIndex, 1);
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

  testWidgets('renders every Icon argument through the real Flutter widget', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.iconModelBytesForViewTest());
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final iconFinder = find.descendant(
      of: find.byKey(
        const ValueKey('canvas-widget-${fixture.iconWidgetIdForViewTest}'),
      ),
      matching: find.byType(Icon),
    );
    final icon = tester.widget<Icon>(iconFinder);
    final iconData = icon.icon!;
    expect(iconData.codePoint, 0xe5fc);
    expect(iconData.fontFamily, 'MaterialIcons');
    expect(iconData.fontPackage, isNull);
    expect(iconData.matchTextDirection, isTrue);
    expect(iconData.fontFamilyFallback, isNull);
    expect(icon.size, 32);
    expect(icon.fill, 0.75);
    expect(icon.weight, 600);
    expect(icon.grade, -25);
    expect(icon.opticalSize, 24);
    final context = tester.element(iconFinder);
    expect(icon.color, Theme.of(context).colorScheme.primary);
    expect(icon.shadows, hasLength(1));
    expect(icon.shadows!.single.color, Theme.of(context).colorScheme.shadow);
    expect(icon.shadows!.single.offset, const Offset(-1, 2));
    expect(icon.shadows!.single.blurRadius, 3);
    expect(icon.semanticLabel, 'Reviewed icon');
    expect(icon.textDirection, TextDirection.rtl);
    expect(icon.applyTextScaling, isFalse);
    expect(icon.blendMode, BlendMode.multiply);
    expect(icon.fontWeight, FontWeight.w700);
    expect(
      find.byKey(
        const ValueKey(
          'canvas-widget-outline-${fixture.iconWidgetIdForViewTest}',
        ),
      ),
      findsOneWidget,
    );
  });

  testWidgets(
    'preserves IconTheme inheritance, null IconData and explicit empty shadows',
    (tester) async {
      const inherited = IconThemeData(
        size: 41,
        color: Color(0xff123456),
        fill: 0.3,
        weight: 525,
        grade: 12,
        opticalSize: 18,
        shadows: [
          Shadow(color: Color(0xff654321), offset: Offset(1, 2), blurRadius: 3),
        ],
      );
      Future<void> pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          fixture.iconModelBytesForViewTest(properties: properties),
        );
        await tester.pumpWidget(
          MaterialApp(
            theme: ThemeData(iconTheme: inherited),
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        );
      }

      Finder iconFinder() => find.descendant(
        of: find.byKey(
          const ValueKey('canvas-widget-${fixture.iconWidgetIdForViewTest}'),
        ),
        matching: find.byType(Icon),
      );

      await pump({
        'icon': fixture.iconDataValueForViewTest(fontFamily: 'MaterialIcons'),
      });
      final inheritedIcon = tester.widget<Icon>(iconFinder());
      expect(inheritedIcon.icon, isNotNull);
      expect(inheritedIcon.size, isNull);
      expect(inheritedIcon.color, isNull);
      expect(inheritedIcon.fill, isNull);
      expect(inheritedIcon.weight, isNull);
      expect(inheritedIcon.grade, isNull);
      expect(inheritedIcon.opticalSize, isNull);
      expect(inheritedIcon.shadows, isNull);
      expect(tester.getSize(iconFinder()), const Size.square(41));
      final richText = tester.widget<RichText>(
        find.descendant(of: iconFinder(), matching: find.byType(RichText)),
      );
      final inheritedStyle = (richText.text as TextSpan).style!;
      expect(inheritedStyle.color, inherited.color);
      expect(inheritedStyle.fontSize, 41);
      expect(
        {
          for (final variation in inheritedStyle.fontVariations!)
            variation.axis: variation.value,
        },
        const {'FILL': 0.3, 'wght': 525.0, 'GRAD': 12.0, 'opsz': 18.0},
      );
      expect(inheritedStyle.shadows, inherited.shadows);

      await pump({
        'icon': fixture.iconDataValueForViewTest(fontFamily: 'MaterialIcons'),
        'shadows': {'kind': 'shadowList', 'items': <Object?>[]},
      });
      final emptyShadowStyle =
          (tester
                      .widget<RichText>(
                        find.descendant(
                          of: iconFinder(),
                          matching: find.byType(RichText),
                        ),
                      )
                      .text
                  as TextSpan)
              .style!;
      expect(emptyShadowStyle.shadows, isEmpty);

      await pump({
        'icon': fixture.iconDataValueForViewTest(
          codePoint: null,
          fontFamily: null,
        ),
      });
      final icon = tester.widget<Icon>(iconFinder());
      expect(icon.icon, isNull);
      expect(icon.size, isNull);
      expect(icon.color, isNull);
      expect(icon.fill, isNull);
      expect(icon.weight, isNull);
      expect(icon.grade, isNull);
      expect(icon.opticalSize, isNull);
      expect(icon.shadows, isNull);
      expect(tester.getSize(iconFinder()), const Size.square(41));
      expect(
        find.descendant(of: iconFinder(), matching: find.byType(RichText)),
        findsNothing,
      );
    },
  );

  testWidgets(
    'keeps zero-size and clear Icons selectable with overlapping targets',
    (tester) async {
      const firstId = fixture.iconWidgetIdForViewTest;
      const secondId = 'a77ba7a9-61e8-438c-908f-aa495c49e7d7';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(_modelWithZeroIconSiblings([firstId, secondId])),
          ),
        ),
      );
      String? selected;
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: selected,
          onSelected: (value) => selected = value,
        ),
      );
      await tester.pump();
      await tester.pump();

      expect(
        tester.getSize(find.byKey(const ValueKey('canvas-widget-$firstId'))),
        Size.zero,
      );
      final group = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-group-$firstId'),
      );
      expect(group, findsOneWidget);
      expect(tester.getSize(group), const Size.square(36));
      await tester.tap(group);
      expect(selected, firstId);
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: selected,
          onSelected: (value) => selected = value,
        ),
      );
      await tester.pump();
      await tester.pump();
      await tester.tap(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-group-$firstId'),
        ),
      );
      expect(selected, secondId);

      final clearModel = CanvasModel.decode(
        fixture.iconModelBytesForViewTest(
          properties: {
            'icon': fixture.iconDataValueForViewTest(
              fontFamily: 'MaterialIcons',
            ),
            'size': {'kind': 'double', 'value': 24.0},
            'blendMode': {
              'kind': 'enum',
              'type': 'BlendMode',
              'value': 'clear',
            },
          },
        ),
      );
      selected = null;
      await tester.pumpWidget(
        CanvasModelApp(
          model: clearModel,
          selectedWidgetId: null,
          onSelected: (value) => selected = value,
        ),
      );
      final clearTarget = find.byKey(
        const ValueKey('canvas-widget-${fixture.iconWidgetIdForViewTest}'),
      );
      expect(tester.getSize(clearTarget), const Size.square(24));
      expect(tester.widget<Icon>(find.byType(Icon)).blendMode, BlendMode.clear);
      await tester.tap(clearTarget);
      expect(selected, fixture.iconWidgetIdForViewTest);
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

Map<String, Object?> _modelWithCenteredAspectRatio({
  required double aspectRatio,
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
          'id': '4a07880d-54a7-44c6-97e4-a88df4e44e67',
          'type': 'flutter.widgets.AspectRatio',
          'properties': <String, Object?>{
            'aspectRatio': {'kind': 'double', 'value': aspectRatio},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredBaseline({
  required double baseline,
  required String baselineType,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '0197b4c0-11f0-45b0-9c63-4caa0981d386',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '0197b4c0-11f0-45b1-bfe8-cc84ef5a6fae',
          'type': 'flutter.widgets.Baseline',
          'properties': <String, Object?>{
            'baseline': {'kind': 'double', 'value': baseline},
            'baselineType': {
              'kind': 'enum',
              'type': 'TextBaseline',
              'value': baselineType,
            },
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredIntrinsicHeight({
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '8d1a8689-6d4d-4c64-9d20-fdca66975e86',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '15aa2055-201d-4f3e-bd50-24cc60fa50c0',
          'type': 'flutter.widgets.IntrinsicHeight',
          'properties': <String, Object?>{},
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredAlign({
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
          'id': 'b51246d4-4e44-4d7c-91bc-a0892df4a341',
          'type': 'flutter.widgets.Align',
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

Map<String, Object?> _modelWithCenteredFractionallySizedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final fractionallySizedBox = <String, Object?>{
    'id': '403df8b2-b244-4e7c-9ff1-29ba070206b6',
    'type': 'flutter.widgets.FractionallySizedBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': 'd4401632-faf9-4765-9165-61de3e851ea6',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{
              'kind': 'single',
              'child': fractionallySizedBox,
            },
          },
        }
      : fractionallySizedBox;
  body['child'] = <String, Object?>{
    'id': '5e5dadcf-d272-45d9-aec8-295ac4afc7f0',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredConstrainedBox({
  required Map<String, Object?> constraints,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final constrainedBox = <String, Object?>{
    'id': '93d89766-af04-4fee-af57-a56c4ed5e37c',
    'type': 'flutter.widgets.ConstrainedBox',
    'properties': <String, Object?>{'constraints': constraints},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': '8fb51a53-f998-420d-bf96-f5cd3eaf66c6',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{
              'kind': 'single',
              'child': <String, Object?>{
                'id': 'a17a1cf7-a873-44d8-96bd-84b4e8089fa6',
                'type': 'flutter.widgets.Center',
                'properties': <String, Object?>{},
                'slots': <String, Object?>{
                  'child': <String, Object?>{
                    'kind': 'single',
                    'child': constrainedBox,
                  },
                },
              },
            },
          },
        }
      : constrainedBox;
  body['child'] = <String, Object?>{
    'id': 'faef84b3-15d3-4a5c-9fc3-f3a833bb1e59',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _viewBoxConstraints(
  Object? minWidth,
  Object? maxWidth,
  Object? minHeight,
  Object? maxHeight,
) => {
  'kind': 'boxConstraints',
  'minWidth': minWidth,
  'maxWidth': maxWidth,
  'minHeight': minHeight,
  'maxHeight': maxHeight,
};

Map<String, Object?> _modelWithCenteredUnconstrainedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final unconstrainedBox = <String, Object?>{
    'id': '67247867-79f8-470f-b109-59971aa7392c',
    'type': 'flutter.widgets.UnconstrainedBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': '0b6f7f3b-d2dc-49ce-bf61-1834859cc478',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{
              'kind': 'single',
              'child': unconstrainedBox,
            },
          },
        }
      : unconstrainedBox;
  body['child'] = <String, Object?>{
    'id': 'c7dd4e0d-a326-4bf9-84a3-f95a759bd1d8',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredLimitedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  required String incoming,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final limitedBox = <String, Object?>{
    'id': '57b8ce90-edde-4988-a4b7-bbf2eec66922',
    'type': 'flutter.widgets.LimitedBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final positionedChild = switch (incoming) {
    'bounded' => limitedBox,
    'widthUnbounded' => <String, Object?>{
      'id': '3d96e466-b5f7-4bb9-b12c-4d78b3ae6a94',
      'type': 'flutter.widgets.UnconstrainedBox',
      'properties': <String, Object?>{
        'constrainedAxis': <String, Object?>{
          'kind': 'enum',
          'type': 'Axis',
          'value': 'vertical',
        },
      },
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': limitedBox},
      },
    },
    'heightUnbounded' => <String, Object?>{
      'id': 'e20426cb-1ab5-4a15-817d-5cd5e582f8a1',
      'type': 'flutter.widgets.UnconstrainedBox',
      'properties': <String, Object?>{
        'constrainedAxis': <String, Object?>{
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
      },
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': limitedBox},
      },
    },
    'bothUnbounded' => <String, Object?>{
      'id': 'b263333d-4c52-425f-a844-793339ccdc73',
      'type': 'flutter.widgets.UnconstrainedBox',
      'properties': <String, Object?>{},
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': limitedBox},
      },
    },
    _ => throw ArgumentError.value(incoming, 'incoming'),
  };
  final boundedFrame = <String, Object?>{
    'id': '2c0e55bf-3199-4895-9145-0b487099f3df',
    'type': 'flutter.widgets.SizedBox',
    'properties': <String, Object?>{
      'width': {'kind': 'integer', 'value': 200},
      'height': {'kind': 'integer', 'value': 100},
    },
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': positionedChild},
    },
  };
  body['child'] = <String, Object?>{
    'id': '66a0a157-152c-4bd5-8455-514a87aa4154',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': boundedFrame},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredOverflowBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final overflowBox = <String, Object?>{
    'id': '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4',
    'type': 'flutter.widgets.OverflowBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': '04476d22-29cf-4b9e-aeb3-ee36d0275d08',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': overflowBox},
          },
        }
      : overflowBox;
  body['child'] = <String, Object?>{
    'id': '47924710-4a25-4cf0-9495-287483e941d6',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredFittedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final fittedBox = <String, Object?>{
    'id': 'b587a092-9a65-420a-9d1c-e127cc752f8d',
    'type': 'flutter.widgets.FittedBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': 'e5950c77-cf55-4fa5-8595-f62c9fb67662',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': fittedBox},
          },
        }
      : fittedBox;
  body['child'] = <String, Object?>{
    'id': 'c01d308e-653d-467f-a113-e77ab22c1775',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithConstrainedStack({
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  bool unboundedMainAxis = false,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final stack = <String, Object?>{
    'id': '5a809127-5a50-4dbf-b5fd-464619344ac4',
    'type': 'flutter.widgets.Stack',
    'properties': properties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final constrained = unboundedMainAxis
      ? <String, Object?>{
          'id': 'c2f2877d-3fab-4b71-a232-b4bf44dcd7c0',
          'type': 'flutter.widgets.Row',
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[stack],
            },
          },
        }
      : <String, Object?>{
          'id': 'd376e25c-2809-4d1a-bc48-d54fca11a123',
          'type': 'flutter.widgets.Container',
          'properties': <String, Object?>{
            'constraints': <String, Object?>{
              'kind': 'boxConstraints',
              'minWidth': 100,
              'maxWidth': 200,
              'minHeight': 50,
              'maxHeight': 100,
            },
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': stack},
          },
        };
  body['child'] = <String, Object?>{
    'id': '3ca862de-2561-4f9e-af0f-dfa53e7f4f28',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': constrained},
    },
  };
  return model;
}

Map<String, Object?> _modelWithConstrainedWrap({
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  bool unboundedMainAxis = false,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final wrap = <String, Object?>{
    'id': '0cdde885-b68c-4b21-bd47-246721c4e8b2',
    'type': 'flutter.widgets.Wrap',
    'properties': properties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final constrained = unboundedMainAxis
      ? <String, Object?>{
          'id': 'fc7d6ad3-bafe-43f8-9da9-d1742116bc89',
          'type': 'flutter.widgets.Row',
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[wrap],
            },
          },
        }
      : <String, Object?>{
          'id': '5b9856b4-1a5d-44d8-94a7-2106df659fba',
          'type': 'flutter.widgets.Container',
          'properties': <String, Object?>{
            'constraints': <String, Object?>{
              'kind': 'boxConstraints',
              'minWidth': 0,
              'maxWidth': 100,
              'minHeight': 0,
              'maxHeight': 100,
            },
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': wrap},
          },
        };
  body['child'] = <String, Object?>{
    'id': 'a00ef225-fe97-46a1-bc4d-2298da4348dc',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': constrained},
    },
  };
  return model;
}

Map<String, Object?> _viewExpandedNode(
  String id, {
  int? flex,
  required Map<String, Object?> child,
}) => <String, Object?>{
  'id': id,
  'type': 'flutter.widgets.Expanded',
  'properties': <String, Object?>{
    if (flex != null) 'flex': {'kind': 'integer', 'value': flex},
  },
  'slots': <String, Object?>{
    'child': <String, Object?>{'kind': 'single', 'child': child},
  },
};

Map<String, Object?> _viewFlexibleNode(
  String id, {
  int? flex,
  String? fit,
  required Map<String, Object?> child,
}) => <String, Object?>{
  'id': id,
  'type': 'flutter.widgets.Flexible',
  'properties': <String, Object?>{
    if (flex != null) 'flex': {'kind': 'integer', 'value': flex},
    if (fit != null) 'fit': {'kind': 'enum', 'type': 'FlexFit', 'value': fit},
  },
  'slots': <String, Object?>{
    'child': <String, Object?>{'kind': 'single', 'child': child},
  },
};

Map<String, Object?> _viewSpacerNode(String id, {int? flex}) =>
    <String, Object?>{
      'id': id,
      'type': 'flutter.widgets.Spacer',
      'properties': <String, Object?>{
        if (flex != null) 'flex': {'kind': 'integer', 'value': flex},
      },
      'slots': <String, Object?>{},
    };

Map<String, Object?> _modelWithFixedFlex({
  required String parentType,
  String parentId = 'be4b446d-12b9-42c3-a427-03fb2fd472bd',
  Map<String, Object?> parentProperties = const {},
  required List<Map<String, Object?>> children,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final flex = <String, Object?>{
    'id': parentId,
    'type': parentType,
    'properties': parentProperties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  body['child'] = <String, Object?>{
    'id': '384718a1-ddea-40a0-82ca-d7cb4ebaf865',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '63657b7c-9c7b-4e7c-a200-c4ae1d37326a',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 300},
            'height': {'kind': 'integer', 'value': 120},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': flex},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithListView({
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  String? unboundedParentType,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final listView = <String, Object?>{
    'id': '4febd2a9-b2ef-4f4d-8d67-2df913b46da2',
    'type': 'flutter.widgets.ListView',
    'properties': properties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final child = unboundedParentType == null
      ? <String, Object?>{
          'id': '3d80adae-3c10-4a2c-9b76-46179ea53b13',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 300},
            'height': {'kind': 'integer', 'value': 160},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': listView},
          },
        }
      : <String, Object?>{
          'id': 'f0f34b58-a9d5-46da-8fde-2daf20400796',
          'type': unboundedParentType,
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[listView],
            },
          },
        };
  body['child'] = <String, Object?>{
    'id': '31684f33-2e10-4fc6-9658-77cf0fe7eb55',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  return model;
}

Map<String, Object?> _viewSizedBoxNode(
  String id, {
  required num width,
  required num height,
}) => <String, Object?>{
  'id': id,
  'type': 'flutter.widgets.SizedBox',
  'properties': <String, Object?>{
    'width': {'kind': width is int ? 'integer' : 'double', 'value': width},
    'height': {'kind': height is int ? 'integer' : 'double', 'value': height},
  },
  'slots': <String, Object?>{
    'child': <String, Object?>{'kind': 'single', 'child': null},
  },
};

Map<String, Object?> _modelWithCenteredOpacity({
  required double opacity,
  required bool? alwaysIncludeSemantics,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final properties = <String, Object?>{
    'opacity': {'kind': 'double', 'value': opacity},
  };
  if (alwaysIncludeSemantics != null) {
    properties['alwaysIncludeSemantics'] = {
      'kind': 'boolean',
      'value': alwaysIncludeSemantics,
    };
  }
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '47f754c8-9fcb-480c-9578-87cda83bd4d5',
          'type': 'flutter.widgets.Opacity',
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

Map<String, Object?> _modelWithCenteredContainer({
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
          'id': 'd9e278fa-32f8-4ef7-a92f-4aef0867435c',
          'type': 'flutter.widgets.Container',
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

Map<String, Object?> _modelWithCenteredImage({
  required String id,
  required Map<String, Object?> properties,
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
          'id': id,
          'type': 'flutter.widgets.Image',
          'properties': properties,
          'slots': <String, Object?>{},
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredTextField({
  required String id,
  required Map<String, Object?> properties,
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
          'id': id,
          'type': 'flutter.material.TextField',
          'properties': properties,
          'slots': <String, Object?>{},
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithTextFieldInFlex({
  required String parentType,
  required Map<String, Object?> properties,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': 'c197acdc-e87f-4b42-888c-907554c30262',
    'type': parentType,
    'properties': <String, Object?>{
      'mainAxisSize': {'kind': 'enum', 'type': 'MainAxisSize', 'value': 'min'},
    },
    'slots': <String, Object?>{
      'children': <String, Object?>{
        'kind': 'list',
        'children': <Object?>[
          <String, Object?>{
            'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
            'type': 'flutter.widgets.Align',
            'properties': <String, Object?>{},
            'slots': <String, Object?>{
              'child': <String, Object?>{
                'kind': 'single',
                'child': <String, Object?>{
                  'id': '73d9ec43-3d37-4304-8998-71fe51804284',
                  'type': 'flutter.material.TextField',
                  'properties': properties,
                  'slots': <String, Object?>{},
                },
              },
            },
          },
        ],
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithEmptyContainerInRow() {
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
          <String, Object?>{
            'id': '38f49912-8e51-4e62-bd4c-2517ecad4962',
            'type': 'flutter.widgets.Row',
            'properties': <String, Object?>{},
            'slots': <String, Object?>{
              'children': <String, Object?>{
                'kind': 'list',
                'children': <Object?>[
                  <String, Object?>{
                    'id': 'd9e278fa-32f8-4ef7-a92f-4aef0867435c',
                    'type': 'flutter.widgets.Container',
                    'properties': <String, Object?>{},
                    'slots': <String, Object?>{
                      'child': <String, Object?>{
                        'kind': 'single',
                        'child': null,
                      },
                    },
                  },
                ],
              },
            },
          },
        ],
      },
    },
  };
  return model;
}

Map<String, Object?> _viewAlignment({
  String basis = 'physical',
  num horizontal = 0,
  num vertical = 0,
}) => {
  'kind': 'alignmentGeometry',
  'basis': basis,
  'horizontal': horizontal,
  'vertical': vertical,
};

Map<String, Object?> _viewNestedAlignment({
  String basis = 'physical',
  num horizontal = 0,
  num vertical = 0,
}) => {'basis': basis, 'horizontal': horizontal, 'vertical': vertical};

Map<String, Object?> _viewLiteralColor(String argb) => {
  'kind': 'literal',
  'argb': argb,
};

Map<String, Object?> _viewThemeColor(String token) => {
  'kind': 'theme',
  'token': token,
};

Map<String, Object?> _viewBorderSide() => {
  'color': _viewThemeColor('material.colorScheme.outline'),
  'width': 2,
  'style': 'solid',
  'strokeAlign': -1,
};

Map<String, Object?> _viewPhysicalBorder() {
  final side = _viewBorderSide();
  return {
    'kind': 'physical',
    'top': Map<String, Object?>.from(side),
    'right': Map<String, Object?>.from(side),
    'bottom': Map<String, Object?>.from(side),
    'left': Map<String, Object?>.from(side),
  };
}

Map<String, Object?> _viewDirectionalBorder() {
  final side = _viewBorderSide();
  return {
    'kind': 'directional',
    'top': Map<String, Object?>.from(side),
    'start': Map<String, Object?>.from(side),
    'end': Map<String, Object?>.from(side),
    'bottom': Map<String, Object?>.from(side),
  };
}

Map<String, Object?> _viewRadius(num x, num y) => {'x': x, 'y': y};

Map<String, Object?> _viewDirectionalRadius() => {
  'kind': 'directional',
  'topStart': _viewRadius(8, 4),
  'topEnd': _viewRadius(10, 5),
  'bottomEnd': _viewRadius(12, 6),
  'bottomStart': _viewRadius(14, 7),
};

Map<String, Object?> _viewPhysicalRadius() => {
  'kind': 'physical',
  'topLeft': _viewRadius(2, 3),
  'topRight': _viewRadius(4, 5),
  'bottomRight': _viewRadius(6, 7),
  'bottomLeft': _viewRadius(8, 9),
};

Map<String, Object?> _viewBoxShadow() => {
  'id': '0980edcf-8ef3-46c1-bafb-16cf30939cb2',
  'color': _viewThemeColor('material.colorScheme.shadow'),
  'offsetX': 2,
  'offsetY': 3,
  'blurRadius': 4,
  'spreadRadius': -1,
  'blurStyle': 'outer',
};

List<Map<String, Object?>> _viewGradientStops() => [
  {
    'id': '9c979578-cfe9-4232-8768-901a9fc6c3b2',
    'color': _viewLiteralColor('0xFF102030'),
    'stop': 0,
  },
  {
    'id': '417b70c2-566d-40f9-a7fb-9cd18dca7f3d',
    'color': _viewThemeColor('material.colorScheme.primary'),
    'stop': 1,
  },
];

Map<String, Object?> _viewLinearGradient() => {
  'kind': 'linear',
  'begin': _viewNestedAlignment(horizontal: -1, vertical: -1),
  'end': _viewNestedAlignment(basis: 'directional', horizontal: 1, vertical: 1),
  'stops': _viewGradientStops(),
  'tileMode': 'mirror',
  'rotationRadians': 0.25,
};

Map<String, Object?> _viewRadialGradient() => {
  'kind': 'radial',
  'center': _viewNestedAlignment(horizontal: 0.1, vertical: -0.2),
  'radius': 0.75,
  'focal': _viewNestedAlignment(
    basis: 'directional',
    horizontal: 0.4,
    vertical: 0.3,
  ),
  'focalRadius': 0.15,
  'stops': _viewGradientStops(),
  'tileMode': 'decal',
  'rotationRadians': null,
};

Map<String, Object?> _viewSweepGradient() => {
  'kind': 'sweep',
  'center': _viewNestedAlignment(
    basis: 'directional',
    horizontal: -0.2,
    vertical: 0.4,
  ),
  'startAngle': 0.25,
  'endAngle': 5.75,
  'stops': _viewGradientStops(),
  'tileMode': 'repeated',
  'rotationRadians': -0.3,
};

Map<String, Object?> _viewImageProvider({
  String kind = 'asset',
  String assetName = 'assets/images/panel.png',
  String? packageName,
  num? exactScale,
  Map<String, Object?>? resize,
  required Map<String, Object?> resolution,
}) => {
  'kind': kind,
  'assetName': assetName,
  'packageName': packageName,
  'exactScale': exactScale,
  'resize': resize,
  'resolution': resolution,
};

Map<String, Object?> _viewDirectImageProvider({
  required Map<String, Object?> provider,
}) => {'kind': 'imageProvider', 'value': provider};

Map<String, Object?> _viewDecorationImage({
  required Map<String, Object?> image,
  bool onError = false,
  Map<String, Object?>? colorFilter,
  String? fit,
  Map<String, Object?>? alignment,
  Map<String, Object?>? centerSlice,
  String repeat = 'noRepeat',
  bool matchTextDirection = false,
  num scale = 1,
  num opacity = 1,
  String filterQuality = 'medium',
  bool invertColors = false,
  bool isAntiAlias = false,
}) => {
  'image': image,
  'onError': onError,
  'colorFilter': colorFilter,
  'fit': fit,
  'alignment': alignment ?? _viewNestedAlignment(),
  'centerSlice': centerSlice,
  'repeat': repeat,
  'matchTextDirection': matchTextDirection,
  'scale': scale,
  'opacity': opacity,
  'filterQuality': filterQuality,
  'invertColors': invertColors,
  'isAntiAlias': isAntiAlias,
};

Map<String, Object?> _viewBoxDecoration({
  Map<String, Object?>? color,
  Map<String, Object?>? image,
  Map<String, Object?>? border,
  Map<String, Object?>? borderRadius,
  List<Map<String, Object?>> boxShadow = const [],
  Map<String, Object?>? gradient,
  String? backgroundBlendMode,
  String shape = 'rectangle',
}) => {
  'kind': 'boxDecoration',
  'color': color,
  'image': image,
  'border': border,
  'borderRadius': borderRadius,
  'boxShadow': boxShadow,
  'gradient': gradient,
  'backgroundBlendMode': backgroundBlendMode,
  'shape': shape,
};

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

Map<String, Object?> _modelWithZeroIconSiblings(List<String> widgetIds) {
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
              'type': 'flutter.widgets.Icon',
              'properties': <String, Object?>{
                'icon': fixture.iconDataValueForViewTest(
                  fontFamily: 'MaterialIcons',
                ),
                'size': <String, Object?>{'kind': 'double', 'value': 0.0},
              },
              'slots': <String, Object?>{},
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

Future<void> _doubleTap(WidgetTester tester, Finder finder) async {
  await tester.tap(finder);
  await tester.pump(kDoubleTapMinTime);
  await tester.tap(finder);
  await tester.pump(kDoubleTapTimeout);
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
