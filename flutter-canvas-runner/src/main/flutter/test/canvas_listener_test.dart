import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _id = '7bec8d5b-ed9a-479a-acb9-56ba0b31e42c';
const _childId = '8a6a2a8a-e0e8-4f2c-a57c-dc27306a55ec';
const _type = 'flutter.widgets.Listener';
const _events = <String>[
  'onPointerDown',
  'onPointerMove',
  'onPointerUp',
  'onPointerHover',
  'onPointerCancel',
  'onPointerPanZoomStart',
  'onPointerPanZoomUpdate',
  'onPointerPanZoomEnd',
  'onPointerSignal',
];

void main() {
  test('Listener exact10property9event contract retains protocol19', () {
    expect(canvasModelProtocolVersion, 20);
    expect(
      canvasRuntimeWidgetSchemaContractForTesting(),
      canvasReviewedWidgetSchemaContract.trimLeft(),
    );
    final contract = canvasReviewedWidgetSchemaContract;
    final start = contract.indexOf('W|$_type\n');
    final end = contract.indexOf('W|flutter.widgets.MatrixTransition\n', start);
    final section = contract.substring(start, end);
    expect('\nP|'.allMatches(section).length, 10);
    expect(section, contains('S|child|single|0|0|1|any\n'));
    expect(_events.length, 9);
    expect(isCanvasReviewedWidgetType(_type), isTrue);
    expect(canvasWidgetTraitsForType(_type), isEmpty);
    expect(_listenerNode(_decode(_model()).root).properties, isEmpty);
  });

  test(
    'All9pointer callbacks accept only presence or null and never application identity',
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
          {'kind': 'callbackPresence', 'handler': 'executeApplicationCode'},
          {
            'kind': 'dartObjectReferencePresence',
            'rootSymbol': 'executeApplicationCode',
          },
          {'kind': 'callback', 'handler': 'executeApplicationCode'},
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
      expect(
        () => _decode(
          _model(
            properties: {
              for (final event in _events) event: {'kind': 'callbackPresence'},
            },
          ),
        ),
        returnsNormally,
      );
    },
  );

  test(
    'Listener behavior and optional slots are exact, closed and nonnullable',
    () {
      for (final value in ['deferToChild', 'opaque', 'translucent']) {
        final node = _listenerNode(
          _decode(_model(properties: {'behavior': _behavior(value)})).root,
        );
        expect(
          (node.properties['behavior']!.value as CanvasEnumValue).value,
          value,
        );
      }
      for (final value in [
        {'kind': 'null'},
        {'kind': 'string', 'value': 'opaque'},
        {'kind': 'enum', 'type': 'Axis', 'value': 'opaque'},
        {'kind': 'enum', 'type': 'HitTestBehavior', 'value': 'custom'},
        {
          'kind': 'enum',
          'type': 'HitTestBehavior',
          'value': 'opaque',
          'extra': true,
        },
      ]) {
        expect(
          () => _decode(_model(properties: {'behavior': value})),
          throwsFormatException,
        );
      }
      for (final name in [
        'key',
        'child',
        'onTap',
        'supportedDevices',
        'ignoring',
        'source',
      ]) {
        expect(
          () => _decode(
            _model(
              properties: {
                name: {'kind': 'boolean', 'value': true},
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final childType in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () => _decode(
            _model(child: _node(_childId, 'flutter.widgets.$childType')),
          ),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Listener admits ordinary sources and exactly one empty child drop slot',
    () {
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
      for (final type in [
        _type,
        'flutter.widgets.GestureDetector',
        'flutter.widgets.Text',
      ]) {
        final source = CanvasPaletteDragSource(
          token: type,
          widgetType: type,
          traits: canvasWidgetTraitsForType(type),
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
    'Each callback is isolated and explicit null removes it from retained real Listener',
    (tester) async {
      RenderPointerListener? first;
      for (final event in _events) {
        for (final kind in [
          'callbackPresence',
          'dartObjectReferencePresence',
          'null',
        ]) {
          await _pump(
            tester,
            _model(
              properties: {
                event: {'kind': kind},
              },
              child: _text(),
            ),
          );
          final listener = tester.widget<Listener>(_listener());
          final renderer = tester.renderObject<RenderPointerListener>(
            _listener(),
          );
          if (first == null) {
            first = renderer;
          } else {
            expect(renderer, same(first));
          }
          final enabled = <String, bool>{
            'onPointerDown': listener.onPointerDown != null,
            'onPointerMove': listener.onPointerMove != null,
            'onPointerUp': listener.onPointerUp != null,
            'onPointerHover': listener.onPointerHover != null,
            'onPointerCancel': listener.onPointerCancel != null,
            'onPointerPanZoomStart': listener.onPointerPanZoomStart != null,
            'onPointerPanZoomUpdate': listener.onPointerPanZoomUpdate != null,
            'onPointerPanZoomEnd': listener.onPointerPanZoomEnd != null,
            'onPointerSignal': listener.onPointerSignal != null,
          };
          expect(
            enabled.entries
                .where((entry) => entry.value)
                .map((entry) => entry.key),
            kind == 'null' ? isEmpty : [event],
          );
          expect(find.text('Listener child'), findsOneWidget);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  for (final platform in ['windows', 'android']) {
    testWidgets(
      'Real behavior values and defaults retain native hit testing on $platform',
      (tester) async {
        for (final value in <String?>[
          null,
          'deferToChild',
          'opaque',
          'translucent',
        ]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {if (value != null) 'behavior': _behavior(value)},
            ),
          );
          final listener = tester.widget<Listener>(_listener());
          final renderer = tester.renderObject<RenderPointerListener>(
            _listener(),
          );
          final expected = HitTestBehavior.values.byName(
            value ?? 'deferToChild',
          );
          expect(listener.behavior, expected);
          expect(renderer.behavior, expected);
          expect(listener.child, isNull);
          expect(renderer.child, isNull);
          expect(renderer.size, const Size(120, 100));
          final hits = BoxHitTestResult();
          expect(
            renderer.hitTest(hits, position: const Offset(20, 20)),
            expected == HitTestBehavior.opaque,
          );
          expect(
            hits.path.any((entry) => identical(entry.target, renderer)),
            expected != HitTestBehavior.deferToChild,
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'All raw pointer notifications are inert and cannot steal child selection on $platform',
      (tester) async {
        final selected = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                child: _text(),
                properties: {
                  for (final name in _events)
                    name: {'kind': 'callbackPresence'},
                  'behavior': _behavior('opaque'),
                },
              ),
            ),
            selectedWidgetId: null,
            onSelected: selected.add,
          ),
        );
        await tester.pump();
        final listener = tester.widget<Listener>(_listener());
        listener.onPointerDown!(const PointerDownEvent());
        listener.onPointerMove!(const PointerMoveEvent());
        listener.onPointerUp!(const PointerUpEvent());
        listener.onPointerHover!(const PointerHoverEvent());
        listener.onPointerCancel!(const PointerCancelEvent());
        listener.onPointerPanZoomStart!(const PointerPanZoomStartEvent());
        listener.onPointerPanZoomUpdate!(const PointerPanZoomUpdateEvent());
        listener.onPointerPanZoomEnd!(const PointerPanZoomEndEvent());
        listener.onPointerSignal!(
          const PointerScrollEvent(scrollDelta: Offset(0, 10)),
        );
        expect(selected, isEmpty);
        await tester.tap(find.text('Listener child'));
        await tester.pumpAndSettle();
        expect(selected.last, _childId);
        final last = selected.last;
        listener.onPointerHover!(const PointerHoverEvent());
        listener.onPointerCancel!(const PointerCancelEvent());
        listener.onPointerSignal!(
          const PointerScrollEvent(scrollDelta: Offset(0, 10)),
        );
        expect(selected.last, last);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Empty zero-size Listener preserves real layout and external selection/drop geometry on $platform',
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
        final listener = tester.widget<Listener>(_listener());
        expect(listener.child, isNull);
        expect(tester.getSize(_listener()), Size.zero);
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$_id'),
        );
        expect(handle, findsOneWidget);
        expect(find.ancestor(of: handle, matching: _listener()), findsNothing);
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
  }
}

Finder _listener() => find.byKey(const ValueKey('canvas-listener-$_id'));
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
CanvasNode _listenerNode(CanvasNode node) {
  if (node.id == _id) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      try {
        return _listenerNode(child);
      } on StateError {
        /* Continue siblings. */
      }
    }
  }
  throw StateError('Listener missing');
}

Map<String, Object?> _behavior(String value) => {
  'kind': 'enum',
  'type': 'HitTestBehavior',
  'value': value,
};
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
    'data': {'kind': 'string', 'value': 'Listener child'},
  },
  'slots': <String, Object?>{},
};
