import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _id = 'f047619d-465a-4e66-9abc-19199167a7c0';
const _childId = '9ce992a6-53f5-464f-8f2f-dcab5761b603';
const _type = 'flutter.widgets.GestureDetector';
const _events = <String>[
  'onTapDown',
  'onTapUp',
  'onTap',
  'onTapMove',
  'onTapCancel',
  'onSecondaryTap',
  'onSecondaryTapDown',
  'onSecondaryTapUp',
  'onSecondaryTapCancel',
  'onTertiaryTapDown',
  'onTertiaryTapUp',
  'onTertiaryTapCancel',
  'onDoubleTapDown',
  'onDoubleTap',
  'onDoubleTapCancel',
  'onLongPressDown',
  'onLongPressCancel',
  'onLongPress',
  'onLongPressStart',
  'onLongPressMoveUpdate',
  'onLongPressUp',
  'onLongPressEnd',
  'onSecondaryLongPressDown',
  'onSecondaryLongPressCancel',
  'onSecondaryLongPress',
  'onSecondaryLongPressStart',
  'onSecondaryLongPressMoveUpdate',
  'onSecondaryLongPressUp',
  'onSecondaryLongPressEnd',
  'onTertiaryLongPressDown',
  'onTertiaryLongPressCancel',
  'onTertiaryLongPress',
  'onTertiaryLongPressStart',
  'onTertiaryLongPressMoveUpdate',
  'onTertiaryLongPressUp',
  'onTertiaryLongPressEnd',
  'onVerticalDragDown',
  'onVerticalDragStart',
  'onVerticalDragUpdate',
  'onVerticalDragEnd',
  'onVerticalDragCancel',
  'onHorizontalDragDown',
  'onHorizontalDragStart',
  'onHorizontalDragUpdate',
  'onHorizontalDragEnd',
  'onHorizontalDragCancel',
  'onPanDown',
  'onPanStart',
  'onPanUpdate',
  'onPanEnd',
  'onPanCancel',
  'onScaleStart',
  'onScaleUpdate',
  'onScaleEnd',
  'onForcePressStart',
  'onForcePressPeak',
  'onForcePressUpdate',
  'onForcePressEnd',
];
const _devices = [
  'touch',
  'mouse',
  'stylus',
  'invertedStylus',
  'trackpad',
  'unknown',
];

void main() {
  test(
    'GestureDetector exact64property contract is independent and protocol19',
    () {
      expect(canvasModelProtocolVersion, 20);
      expect(
        canvasRuntimeWidgetSchemaContractForTesting(),
        canvasReviewedWidgetSchemaContract.trimLeft(),
      );
      final start = canvasReviewedWidgetSchemaContract.indexOf('W|$_type\n');
      final end = canvasReviewedWidgetSchemaContract.indexOf(
        'W|flutter.widgets.GridView\n',
        start,
      );
      final section = canvasReviewedWidgetSchemaContract.substring(start, end);
      expect('\nP|'.allMatches(section).length, 64);
      expect(section, contains('S|child|single|0|0|1|any\n'));
      expect(_events.length, 58);
      expect(isCanvasReviewedWidgetType(_type), isTrue);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(
        _decode(_model()).root
            .slot('body')!
            .child!
            .slot('child')!
            .child!
            .slot('child')!
            .child!
            .properties,
        isEmpty,
      );
    },
  );

  test(
    'All58events accept only presence or explicit null and never source identity',
    () {
      for (final event in _events) {
        for (final kind in [
          'callbackPresence',
          'dartObjectReferencePresence',
          'null',
        ]) {
          expect(
            () => _decode(
              _model(
                properties: {
                  event: {'kind': kind},
                },
              ),
            ),
            returnsNormally,
          );
        }
        for (final invalid in [
          {'kind': 'callback', 'method': 'executeApplicationCode'},
          {'kind': 'callbackPresence', 'method': 'executeApplicationCode'},
          {
            'kind': 'dartObjectReferencePresence',
            'rootSymbol': 'executeApplicationCode',
          },
          {'kind': 'string', 'value': 'executeApplicationCode()'},
          {'kind': 'boolean', 'value': true},
        ]) {
          expect(
            () => _decode(_model(properties: {event: invalid})),
            throwsFormatException,
            reason: event,
          );
        }
      }
    },
  );

  test(
    'Device sets admit all64subsets including empty and distinguish omitted/null',
    () {
      for (var bits = 0; bits < 64; bits++) {
        final values = [
          for (var i = 0; i < 6; i++)
            if (bits & (1 << i) != 0) _devices[i],
        ];
        final decoded = _gestureNode(
          _decode(
            _model(
              properties: {
                'supportedDevices': {
                  'kind': 'pointerDeviceKindSet',
                  'values': values,
                },
              },
            ),
          ).root,
        );
        expect(decoded.properties['supportedDevices']!.value, values.toSet());
      }
      expect(
        _gestureNode(
          _decode(_model()).root,
        ).properties.containsKey('supportedDevices'),
        isFalse,
      );
      expect(
        _gestureNode(
          _decode(
            _model(
              properties: {
                'supportedDevices': {'kind': 'null'},
              },
            ),
          ).root,
        ).properties['supportedDevices']!.kind,
        'null',
      );
      for (final invalid in [
        {
          'kind': 'pointerDeviceKindSet',
          'values': ['mouse', 'mouse'],
        },
        {
          'kind': 'pointerDeviceKindSet',
          'values': ['Mouse'],
        },
        {
          'kind': 'pointerDeviceKindSet',
          'values': ['keyboard'],
        },
        {
          'kind': 'pointerDeviceKindSet',
          'values': [null],
        },
        {'kind': 'pointerDeviceKindSet', 'values': 'touch'},
        {'kind': 'pointerDeviceKindSet', 'values': [], 'extra': true},
        {'kind': 'pointerDeviceKindSet'},
        {'kind': 'string', 'value': '<PointerDeviceKind>{}'},
      ]) {
        expect(
          () => _decode(_model(properties: {'supportedDevices': invalid})),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Configuration decoding rejects coercion, foreign enums and invalid slots',
    () {
      for (final entry in {
        'behavior': {
          'kind': 'enum',
          'type': 'HitTestBehavior',
          'value': 'invalid',
        },
        'dragStartBehavior': {'kind': 'enum', 'type': 'Axis', 'value': 'down'},
        'excludeFromSemantics': {'kind': 'boolean', 'value': 'true'},
        'trackpadScrollCausesScale': {'kind': 'null'},
        'trackpadScrollToScaleFactor': {'kind': 'offset', 'dx': 1},
        'key': {'kind': 'string', 'value': 'unreviewed'},
      }.entries) {
        expect(
          () => _decode(_model(properties: {entry.key: entry.value})),
          throwsFormatException,
        );
      }
      for (final child in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () =>
              _decode(_model(child: _node(_childId, 'flutter.widgets.$child'))),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'SDK pan/scale and axis conflicts reject before constructing recognizers',
    () {
      for (final kind in ['callbackPresence', 'dartObjectReferencePresence']) {
        for (final active in [
          ['onPanStart', 'onScaleUpdate'],
          ['onPanEnd', 'onVerticalDragUpdate', 'onHorizontalDragStart'],
          ['onScaleEnd', 'onVerticalDragStart', 'onHorizontalDragEnd'],
        ]) {
          expect(
            () => _decode(
              _model(
                properties: {
                  for (final name in active) name: {'kind': kind},
                },
              ),
            ),
            throwsFormatException,
          );
        }
        expect(
          () => _decode(
            _model(
              properties: {
                'onPanDown': {'kind': kind},
                'onScaleStart': {'kind': kind},
              },
            ),
          ),
          returnsNormally,
        );
        expect(
          () => _decode(
            _model(
              properties: {
                'onPanStart': {'kind': 'null'},
                'onScaleStart': {'kind': kind},
              },
            ),
          ),
          returnsNormally,
        );
      }
    },
  );

  test(
    'GestureDetector is an ordinary drag source with exactly one optional child destination',
    () {
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
      for (final sourceType in [
        _type,
        'flutter.widgets.Text',
        'flutter.widgets.Container',
      ]) {
        final source = CanvasPaletteDragSource(
          token: sourceType,
          widgetType: sourceType,
          traits: canvasWidgetTraitsForType(sourceType),
        );
        for (final count in [0, 1]) {
          expect(
            canvasDropTargetAcceptsSource(
              parentWidgetType: _type,
              slotName: 'child',
              currentChildCount: count,
              insertionIndex: 0,
              source: source,
            ),
            count == 0,
          );
        }
      }
    },
  );

  testWidgets(
    'All58events construct only isolated local callbacks with unchanged child layout',
    (tester) async {
      for (final event in _events) {
        await _pump(
          tester,
          _model(
            properties: {
              event: {'kind': 'callbackPresence'},
            },
            child: _text(),
          ),
        );
        final detector = tester.widget<GestureDetector>(_detector());
        final present = <String, bool>{
          'onTapDown': detector.onTapDown != null,
          'onTapUp': detector.onTapUp != null,
          'onTap': detector.onTap != null,
          'onTapMove': detector.onTapMove != null,
          'onTapCancel': detector.onTapCancel != null,
          'onSecondaryTap': detector.onSecondaryTap != null,
          'onSecondaryTapDown': detector.onSecondaryTapDown != null,
          'onSecondaryTapUp': detector.onSecondaryTapUp != null,
          'onSecondaryTapCancel': detector.onSecondaryTapCancel != null,
          'onTertiaryTapDown': detector.onTertiaryTapDown != null,
          'onTertiaryTapUp': detector.onTertiaryTapUp != null,
          'onTertiaryTapCancel': detector.onTertiaryTapCancel != null,
          'onDoubleTapDown': detector.onDoubleTapDown != null,
          'onDoubleTap': detector.onDoubleTap != null,
          'onDoubleTapCancel': detector.onDoubleTapCancel != null,
          'onLongPressDown': detector.onLongPressDown != null,
          'onLongPressCancel': detector.onLongPressCancel != null,
          'onLongPress': detector.onLongPress != null,
          'onLongPressStart': detector.onLongPressStart != null,
          'onLongPressMoveUpdate': detector.onLongPressMoveUpdate != null,
          'onLongPressUp': detector.onLongPressUp != null,
          'onLongPressEnd': detector.onLongPressEnd != null,
          'onSecondaryLongPressDown': detector.onSecondaryLongPressDown != null,
          'onSecondaryLongPressCancel':
              detector.onSecondaryLongPressCancel != null,
          'onSecondaryLongPress': detector.onSecondaryLongPress != null,
          'onSecondaryLongPressStart':
              detector.onSecondaryLongPressStart != null,
          'onSecondaryLongPressMoveUpdate':
              detector.onSecondaryLongPressMoveUpdate != null,
          'onSecondaryLongPressUp': detector.onSecondaryLongPressUp != null,
          'onSecondaryLongPressEnd': detector.onSecondaryLongPressEnd != null,
          'onTertiaryLongPressDown': detector.onTertiaryLongPressDown != null,
          'onTertiaryLongPressCancel':
              detector.onTertiaryLongPressCancel != null,
          'onTertiaryLongPress': detector.onTertiaryLongPress != null,
          'onTertiaryLongPressStart': detector.onTertiaryLongPressStart != null,
          'onTertiaryLongPressMoveUpdate':
              detector.onTertiaryLongPressMoveUpdate != null,
          'onTertiaryLongPressUp': detector.onTertiaryLongPressUp != null,
          'onTertiaryLongPressEnd': detector.onTertiaryLongPressEnd != null,
          'onVerticalDragDown': detector.onVerticalDragDown != null,
          'onVerticalDragStart': detector.onVerticalDragStart != null,
          'onVerticalDragUpdate': detector.onVerticalDragUpdate != null,
          'onVerticalDragEnd': detector.onVerticalDragEnd != null,
          'onVerticalDragCancel': detector.onVerticalDragCancel != null,
          'onHorizontalDragDown': detector.onHorizontalDragDown != null,
          'onHorizontalDragStart': detector.onHorizontalDragStart != null,
          'onHorizontalDragUpdate': detector.onHorizontalDragUpdate != null,
          'onHorizontalDragEnd': detector.onHorizontalDragEnd != null,
          'onHorizontalDragCancel': detector.onHorizontalDragCancel != null,
          'onPanDown': detector.onPanDown != null,
          'onPanStart': detector.onPanStart != null,
          'onPanUpdate': detector.onPanUpdate != null,
          'onPanEnd': detector.onPanEnd != null,
          'onPanCancel': detector.onPanCancel != null,
          'onScaleStart': detector.onScaleStart != null,
          'onScaleUpdate': detector.onScaleUpdate != null,
          'onScaleEnd': detector.onScaleEnd != null,
          'onForcePressStart': detector.onForcePressStart != null,
          'onForcePressPeak': detector.onForcePressPeak != null,
          'onForcePressUpdate': detector.onForcePressUpdate != null,
          'onForcePressEnd': detector.onForcePressEnd != null,
        };
        expect(
          present.entries
              .where((entry) => entry.value)
              .map((entry) => entry.key),
          [event],
        );
        expect(detector.child, isNotNull);
        expect(find.text('Gesture child'), findsOneWidget);
        expect(tester.takeException(), isNull, reason: event);
      }
    },
  );

  for (final platform in ['windows', 'android']) {
    testWidgets(
      'Real configuration and device filtering preserve Designer selection on $platform',
      (tester) async {
        final selected = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                properties: {
                  'onTap': {'kind': 'dartObjectReferencePresence'},
                  'behavior': {
                    'kind': 'enum',
                    'type': 'HitTestBehavior',
                    'value': 'opaque',
                  },
                  'dragStartBehavior': {
                    'kind': 'enum',
                    'type': 'DragStartBehavior',
                    'value': 'down',
                  },
                  'excludeFromSemantics': {'kind': 'boolean', 'value': true},
                  'trackpadScrollCausesScale': {
                    'kind': 'boolean',
                    'value': true,
                  },
                  'trackpadScrollToScaleFactor': {
                    'kind': 'offset',
                    'dx': -0.25,
                    'dy': 0.75,
                  },
                  'supportedDevices': {
                    'kind': 'pointerDeviceKindSet',
                    'values': ['mouse', 'trackpad'],
                  },
                },
              ),
            ),
            selectedWidgetId: null,
            onSelected: selected.add,
          ),
        );
        await tester.pump();
        final detector = tester.widget<GestureDetector>(_detector());
        expect(detector.behavior, HitTestBehavior.opaque);
        expect(detector.dragStartBehavior, DragStartBehavior.down);
        expect(detector.excludeFromSemantics, isTrue);
        expect(detector.trackpadScrollCausesScale, isTrue);
        expect(detector.trackpadScrollToScaleFactor, const Offset(-0.25, .75));
        expect(detector.supportedDevices, {
          PointerDeviceKind.mouse,
          PointerDeviceKind.trackpad,
        });
        expect(detector.child, isNull);
        await tester.tap(_detector(), kind: PointerDeviceKind.touch);
        await tester.pumpAndSettle();
        expect(
          selected.last,
          _id,
        ); // Designer parent remains selectable outside application device filter.
        selected.clear();
        await tester.tap(_detector(), kind: PointerDeviceKind.mouse);
        await tester.pumpAndSettle();
        expect(selected.last, _id);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Empty zero-size detector stays zero-size with external selection/drop geometry on $platform',
      (tester) async {
        CanvasDropResolver? resolver;
        final selected = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(_model(platform: platform, size: Size.zero)),
            selectedWidgetId: null,
            onSelected: selected.add,
            onDropResolverChanged: (value) => resolver = value,
          ),
        );
        await tester.pump();
        expect(tester.getSize(_detector()), Size.zero);
        expect(tester.widget<GestureDetector>(_detector()).child, isNull);
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$_id'),
        );
        expect(handle, findsOneWidget);
        expect(find.ancestor(of: handle, matching: _detector()), findsNothing);
        final point = tester.getCenter(handle);
        await tester.tapAt(point);
        expect(selected.last, _id);
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final drop = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
        expect(drop?.parentWidgetId, _id);
        expect(drop?.slotName, 'child');
        expect(drop?.insertionIndex, 0);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Application recognizers do not steal child Designer tap or geometric drops on $platform',
      (tester) async {
        final selected = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                child: _text(),
                properties: {
                  'onTap': {'kind': 'callbackPresence'},
                  'onTapCancel': {'kind': 'callbackPresence'},
                  'onDoubleTap': {'kind': 'callbackPresence'},
                  'onDoubleTapCancel': {'kind': 'callbackPresence'},
                  'onLongPressCancel': {'kind': 'callbackPresence'},
                  'onPanUpdate': {'kind': 'callbackPresence'},
                  'onPanCancel': {'kind': 'callbackPresence'},
                },
              ),
            ),
            selectedWidgetId: null,
            onSelected: selected.add,
          ),
        );
        await tester.pump();
        await tester.tap(find.text('Gesture child'));
        await tester.pump(kDoubleTapTimeout + const Duration(milliseconds: 10));
        await tester.pumpAndSettle();
        expect(selected.last, _childId);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'Defaults/null and empty devices remain distinct across retained updates',
    (tester) async {
      for (final properties in <Map<String, Object?>>[
        {},
        {
          'supportedDevices': {'kind': 'null'},
          'behavior': {'kind': 'null'},
        },
        {
          'supportedDevices': {'kind': 'pointerDeviceKindSet', 'values': []},
        },
      ]) {
        await _pump(tester, _model(properties: properties));
        final detector = tester.widget<GestureDetector>(_detector());
        expect(detector.behavior, isNull);
        expect(detector.excludeFromSemantics, isFalse);
        expect(detector.dragStartBehavior, DragStartBehavior.start);
        expect(detector.trackpadScrollCausesScale, isFalse);
        expect(
          detector.trackpadScrollToScaleFactor,
          kDefaultTrackpadScrollToScaleFactor,
        );
        expect(
          detector.supportedDevices,
          properties['supportedDevices'] is Map &&
                  (properties['supportedDevices']! as Map)['kind'] ==
                      'pointerDeviceKindSet'
              ? isEmpty
              : isNull,
        );
        expect(tester.takeException(), isNull);
      }
    },
  );
}

Finder _detector() =>
    find.byKey(const ValueKey('canvas-gesture-detector-$_id'));
Future<void> _pump(WidgetTester tester, Map<String, Object?> model) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: _decode(model),
      selectedWidgetId: null,
      onSelected: (_) {},
    ),
  );
  await tester.pump();
}

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode _gestureNode(CanvasNode node) {
  if (node.id == _id) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      if (child.id == _id) return child;
      try {
        return _gestureNode(child);
      } on StateError {
        /* Continue sibling search. */
      }
    }
  }
  throw StateError('GestureDetector missing');
}

Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
  Size size = const Size(120, 100),
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  model['root'] = {
    'id': '0d279f63-dfd5-4c5f-8e60-8d5c11aab787',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          '1d6c16df-df2b-41cf-b46d-2e1d85538277',
          'flutter.widgets.Center',
          child: _node(
            'e03e4228-2d19-46ba-92d6-e4acdb40796e',
            'flutter.widgets.SizedBox',
            properties: {
              'width': {'kind': 'double', 'value': size.width},
              'height': {'kind': 'double', 'value': size.height},
            },
            child: _node(_id, _type, properties: properties, child: child),
          ),
        ),
      },
    },
  };
  return model;
}

Map<String, Object?> _node(
  String id,
  String type, {
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
}) => {
  'id': id,
  'type': type,
  'properties': properties,
  'slots': {
    'child': {'kind': 'single', 'child': child},
  },
};
Map<String, Object?> _text() => {
  'id': _childId,
  'type': 'flutter.widgets.Text',
  'properties': {
    'data': {'kind': 'string', 'value': 'Gesture child'},
  },
  'slots': <String, Object?>{},
};
