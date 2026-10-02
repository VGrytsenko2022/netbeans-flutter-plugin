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
const _type = 'flutter.widgets.MouseRegion';
const _events = ['onEnter', 'onExit', 'onHover'];
final _cursors = <String, MouseCursor>{
  'none': SystemMouseCursors.none,
  'basic': SystemMouseCursors.basic,
  'click': SystemMouseCursors.click,
  'forbidden': SystemMouseCursors.forbidden,
  'wait': SystemMouseCursors.wait,
  'progress': SystemMouseCursors.progress,
  'contextMenu': SystemMouseCursors.contextMenu,
  'help': SystemMouseCursors.help,
  'text': SystemMouseCursors.text,
  'verticalText': SystemMouseCursors.verticalText,
  'cell': SystemMouseCursors.cell,
  'precise': SystemMouseCursors.precise,
  'move': SystemMouseCursors.move,
  'grab': SystemMouseCursors.grab,
  'grabbing': SystemMouseCursors.grabbing,
  'noDrop': SystemMouseCursors.noDrop,
  'alias': SystemMouseCursors.alias,
  'copy': SystemMouseCursors.copy,
  'disappearing': SystemMouseCursors.disappearing,
  'allScroll': SystemMouseCursors.allScroll,
  'resizeLeftRight': SystemMouseCursors.resizeLeftRight,
  'resizeUpDown': SystemMouseCursors.resizeUpDown,
  'resizeUpLeftDownRight': SystemMouseCursors.resizeUpLeftDownRight,
  'resizeUpRightDownLeft': SystemMouseCursors.resizeUpRightDownLeft,
  'resizeUp': SystemMouseCursors.resizeUp,
  'resizeDown': SystemMouseCursors.resizeDown,
  'resizeLeft': SystemMouseCursors.resizeLeft,
  'resizeRight': SystemMouseCursors.resizeRight,
  'resizeUpLeft': SystemMouseCursors.resizeUpLeft,
  'resizeUpRight': SystemMouseCursors.resizeUpRight,
  'resizeDownLeft': SystemMouseCursors.resizeDownLeft,
  'resizeDownRight': SystemMouseCursors.resizeDownRight,
  'resizeColumn': SystemMouseCursors.resizeColumn,
  'resizeRow': SystemMouseCursors.resizeRow,
  'zoomIn': SystemMouseCursors.zoomIn,
  'zoomOut': SystemMouseCursors.zoomOut,
  'defer': MouseCursor.defer,
  'uncontrolled': MouseCursor.uncontrolled,
  'clickable': WidgetStateMouseCursor.clickable,
  'adaptiveClickable': WidgetStateMouseCursor.adaptiveClickable,
  'textable': WidgetStateMouseCursor.textable,
};

void main() {
  test('MouseRegion exact6property3event contract retains protocol19', () {
    expect(canvasModelProtocolVersion, 20);
    expect(
      canvasRuntimeWidgetSchemaContractForTesting(),
      canvasReviewedWidgetSchemaContract.trimLeft(),
    );
    final contract = canvasReviewedWidgetSchemaContract;
    final start = contract.indexOf('W|$_type\n');
    final end = contract.indexOf('\nW|', start) + 1;
    final section = contract.substring(start, end);
    expect('\nP|'.allMatches(section).length, 6);
    expect(section, contains('S|child|single|0|0|1|any\n'));
    expect(_events.length, 3);
    expect(_cursors.length, 41);
    expect(isCanvasReviewedWidgetType(_type), isTrue);
    expect(canvasWidgetTraitsForType(_type), isEmpty);
    expect(_mouseRegionNode(_decode(_model()).root).properties, isEmpty);
  });

  test(
    'All mouse callbacks accept only presence or null without application identity',
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
    },
  );

  test(
    'Cursor opaque behavior and optional child domains are closed and preserve nullable distinction',
    () {
      for (final name in _cursors.keys) {
        expect(
          () => _decode(
            _model(
              properties: {
                'cursor': {'kind': 'string', 'value': name},
              },
            ),
          ),
          returnsNormally,
        );
      }
      expect(
        () => _decode(
          _model(
            properties: {
              'cursor': {'kind': 'dartObjectReferencePresence'},
            },
          ),
        ),
        returnsNormally,
      );
      for (final invalid in [
        {'kind': 'null'},
        {'kind': 'string', 'value': 'SystemMouseCursors.click'},
        {'kind': 'string', 'value': 'customCursor'},
        {'kind': 'string', 'value': 'executeCode()'},
        {'kind': 'enum', 'type': 'MouseCursor', 'value': 'click'},
        {
          'kind': 'dartObjectReferencePresence',
          'libraryUri': 'package:app/secret.dart',
        },
      ]) {
        expect(
          () => _decode(_model(properties: {'cursor': invalid})),
          throwsFormatException,
        );
      }
      for (final value in [true, false]) {
        expect(
          () => _decode(
            _model(
              properties: {
                'opaque': {'kind': 'boolean', 'value': value},
              },
            ),
          ),
          returnsNormally,
        );
      }
      for (final value in [
        {'kind': 'null'},
        {'kind': 'string', 'value': 'true'},
        {'kind': 'boolean', 'value': true, 'stateBinding': 'secret'},
      ]) {
        expect(
          () => _decode(_model(properties: {'opaque': value})),
          throwsFormatException,
        );
      }
      for (final value in [
        {'kind': 'null'},
        for (final name in ['deferToChild', 'opaque', 'translucent'])
          _behavior(name),
      ]) {
        expect(
          () => _decode(_model(properties: {'hitTestBehavior': value})),
          returnsNormally,
        );
      }
      for (final value in [
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
          () => _decode(_model(properties: {'hitTestBehavior': value})),
          throwsFormatException,
        );
      }
      for (final name in [
        'key',
        'child',
        'onTap',
        'behavior',
        'stateBinding',
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
      for (final type in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () =>
              _decode(_model(child: _node(_childId, 'flutter.widgets.$type'))),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'MouseRegion admits ordinary sources and exactly one empty child drop slot',
    () {
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
      for (final type in [
        _type,
        'flutter.widgets.Listener',
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
    'Each callback is isolated and null removes it from retained real MouseRegion',
    (tester) async {
      RenderMouseRegion? first;
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
          final region = tester.widget<MouseRegion>(_mouseRegion());
          final renderer = tester.renderObject<RenderMouseRegion>(
            _mouseRegion(),
          );
          if (first == null) {
            first = renderer;
          } else {
            expect(renderer, same(first));
          }
          final enabled = {
            'onEnter': region.onEnter != null,
            'onExit': region.onExit != null,
            'onHover': region.onHover != null,
          };
          expect(
            enabled.entries
                .where((entry) => entry.value)
                .map((entry) => entry.key),
            kind == 'null' ? isEmpty : [event],
          );
          expect(find.text('MouseRegion child'), findsOneWidget);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  for (final entry in _cursors.entries) {
    testWidgets(
      'Closed cursor ${entry.key} reaches real renderer and active mouse tracking through the child',
      (tester) async {
        final selected = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                child: _text(),
                properties: {
                  'cursor': {'kind': 'string', 'value': entry.key},
                },
              ),
            ),
            selectedWidgetId: null,
            onSelected: selected.add,
          ),
        );
        await tester.pump();
        final expected = entry.value is WidgetStateMouseCursor
            ? (entry.value as WidgetStateMouseCursor).resolve(
                const <WidgetState>{},
              )
            : entry.value;
        expect(tester.widget<MouseRegion>(_mouseRegion()).cursor, expected);
        expect(
          tester.renderObject<RenderMouseRegion>(_mouseRegion()).cursor,
          expected,
        );
        expect(
          find.descendant(
            of: _mouseRegion(),
            matching: find.byType(MouseRegion),
          ),
          findsNothing,
        );
        final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
        await mouse.addPointer(location: const Offset(1, 1));
        await mouse.moveTo(tester.getCenter(find.text('MouseRegion child')));
        await tester.pump();
        final active = RendererBinding.instance.mouseTracker
            .debugDeviceActiveCursor(1);
        expect(
          active,
          expected == MouseCursor.defer ? SystemMouseCursors.click : expected,
        );
        expect(selected, isEmpty);
        await mouse.removePointer();
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'Project cursor references are never executed and have an explicit unavailable notice',
    (tester) async {
      await _pump(
        tester,
        _model(
          child: _text(),
          properties: {
            'cursor': {'kind': 'string', 'value': 'help'},
          },
        ),
      );
      final renderer = tester.renderObject<RenderMouseRegion>(_mouseRegion());
      await _pump(
        tester,
        _model(
          child: _text(),
          properties: {
            'cursor': {'kind': 'dartObjectReferencePresence'},
          },
        ),
      );
      expect(
        tester.renderObject<RenderMouseRegion>(_mouseRegion()),
        same(renderer),
      );
      expect(
        tester.widget<MouseRegion>(_mouseRegion()).cursor,
        MouseCursor.defer,
      );
      final tooltip = find.byWidgetPredicate(
        (widget) =>
            widget is Tooltip &&
            widget.message?.contains(
                  'MouseRegion.cursor preview unavailable',
                ) ==
                true,
      );
      expect(tooltip, findsOneWidget);
      expect(
        tester.widget<Tooltip>(tooltip).message,
        contains('is not executed in Canvas'),
      );
      expect(find.text('MouseRegion child'), findsOneWidget);
      await _pump(tester, _model(child: _text()));
      expect(
        tester.renderObject<RenderMouseRegion>(_mouseRegion()),
        same(renderer),
      );
      expect(tooltip, findsNothing);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Opaque and hit behavior preserve overlapping mouse regions and deferred cursor inheritance',
    (tester) async {
      const backId = '54d3adfe-ae24-4f03-aed3-13ca1c9b9bf1';
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer(location: const Offset(1, 1));
      for (final opaque in [false, true]) {
        for (final behavior in ['deferToChild', 'opaque', 'translucent']) {
          final model = _model();
          _replaceSubject(model, {
            'id': 'a6441bb3-a3d8-4322-93f5-6d6405f5b22f',
            'type': 'flutter.widgets.Stack',
            'properties': <String, Object?>{},
            'slots': {
              'children': {
                'kind': 'list',
                'children': [
                  _node(
                    backId,
                    _type,
                    properties: {
                      'cursor': {'kind': 'string', 'value': 'help'},
                      'onHover': {'kind': 'callbackPresence'},
                    },
                  ),
                  _node(
                    _id,
                    _type,
                    properties: {
                      'opaque': {'kind': 'boolean', 'value': opaque},
                      'hitTestBehavior': _behavior(behavior),
                      'onEnter': {'kind': 'callbackPresence'},
                      'onExit': {'kind': 'callbackPresence'},
                    },
                  ),
                ],
              },
            },
          });
          await mouse.moveTo(const Offset(1, 1));
          await _pump(tester, model);
          final front = tester.renderObject<RenderMouseRegion>(_mouseRegion());
          final back = tester.renderObject<RenderMouseRegion>(
            find.byKey(const ValueKey('canvas-mouse-region-$backId')),
          );
          final point = tester.getCenter(_mouseRegion());
          final hits = tester.hitTestOnBinding(point);
          final backReachable = !opaque || behavior != 'opaque';
          expect(
            hits.path.any((entry) => identical(entry.target, front)),
            behavior != 'deferToChild',
          );
          expect(
            hits.path.any((entry) => identical(entry.target, back)),
            backReachable,
          );
          await mouse.moveTo(point);
          await tester.pump();
          expect(
            RendererBinding.instance.mouseTracker.debugDeviceActiveCursor(1),
            backReachable ? SystemMouseCursors.help : SystemMouseCursors.click,
          );
          expect(tester.takeException(), isNull);
        }
      }
      await mouse.removePointer();
    },
  );

  testWidgets(
    'Designer cursor scope ends when a child moves out of MouseRegion',
    (tester) async {
      await _pump(
        tester,
        _model(
          child: _text(),
          properties: {
            'cursor': {'kind': 'string', 'value': 'help'},
          },
        ),
      );
      expect(
        find.descendant(of: _mouseRegion(), matching: find.byType(MouseRegion)),
        findsNothing,
      );
      final model = _model();
      _replaceSubject(model, _text());
      await _pump(tester, model);
      final text = find.byKey(const ValueKey('canvas-widget-$_childId'));
      expect(
        find.ancestor(of: text, matching: find.byType(MouseRegion)),
        findsWidgets,
      );
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer(location: const Offset(1, 1));
      await mouse.moveTo(tester.getCenter(find.text('MouseRegion child')));
      await tester.pump();
      expect(
        RendererBinding.instance.mouseTracker.debugDeviceActiveCursor(1),
        SystemMouseCursors.click,
      );
      await mouse.removePointer();
      expect(tester.takeException(), isNull);
    },
  );

  for (final platform in ['windows', 'android']) {
    testWidgets(
      'Real opaque and hit behavior preserve defaults null and all explicit combinations on $platform',
      (tester) async {
        for (final opaque in <bool?>[null, false, true]) {
          for (final value in <String?>[
            null,
            'null',
            'deferToChild',
            'opaque',
            'translucent',
          ]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: {
                  if (opaque != null)
                    'opaque': {'kind': 'boolean', 'value': opaque},
                  if (value != null)
                    'hitTestBehavior': value == 'null'
                        ? {'kind': 'null'}
                        : _behavior(value),
                },
              ),
            );
            final region = tester.widget<MouseRegion>(_mouseRegion());
            final renderer = tester.renderObject<RenderMouseRegion>(
              _mouseRegion(),
            );
            final configured = value == null || value == 'null'
                ? null
                : HitTestBehavior.values.byName(value);
            final behavior = configured ?? HitTestBehavior.opaque;
            expect(region.hitTestBehavior, configured);
            expect(region.opaque, opaque ?? true);
            expect(renderer.hitTestBehavior, behavior);
            expect(renderer.opaque, opaque ?? true);
            expect(region.child, isNull);
            expect(renderer.size, const Size(120, 100));
            final hits = BoxHitTestResult();
            expect(
              renderer.hitTest(hits, position: const Offset(20, 20)),
              behavior == HitTestBehavior.opaque && (opaque ?? true),
            );
            expect(
              hits.path.any((entry) => identical(entry.target, renderer)),
              behavior != HitTestBehavior.deferToChild,
            );
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'Mouse notifications including exit and unmount cannot steal child selection on $platform',
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
                  'cursor': {'kind': 'string', 'value': 'help'},
                },
              ),
            ),
            selectedWidgetId: null,
            onSelected: selected.add,
          ),
        );
        await tester.pump();
        final region = tester.widget<MouseRegion>(_mouseRegion());
        region.onEnter!(const PointerEnterEvent());
        region.onHover!(const PointerHoverEvent());
        region.onExit!(const PointerExitEvent());
        expect(selected, isEmpty);
        final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
        await mouse.addPointer(location: const Offset(1, 1));
        await mouse.moveTo(tester.getCenter(find.text('MouseRegion child')));
        await tester.pump();
        expect(selected, isEmpty);
        await tester.tap(find.text('MouseRegion child'));
        await tester.pumpAndSettle();
        expect(selected.last, _childId);
        final count = selected.length;
        await mouse.moveTo(const Offset(1, 1));
        await tester.pump();
        expect(selected.length, count);
        await mouse.moveTo(tester.getCenter(_mouseRegion()));
        await tester.pump();
        await tester.pumpWidget(const SizedBox());
        await tester.pump();
        expect(selected.length, count);
        await mouse.removePointer();
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Empty zero-size MouseRegion retains native layout and external selection/drop geometry on $platform',
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
        expect(tester.widget<MouseRegion>(_mouseRegion()).child, isNull);
        expect(tester.getSize(_mouseRegion()), Size.zero);
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$_id'),
        );
        expect(handle, findsOneWidget);
        expect(
          find.ancestor(of: handle, matching: _mouseRegion()),
          findsNothing,
        );
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

Finder _mouseRegion() => find.byKey(const ValueKey('canvas-mouse-region-$_id'));

void _replaceSubject(Map<String, Object?> model, Map<String, Object?> subject) {
  final root = model['root']! as Map<String, Object?>;
  final rootSlots = root['slots']! as Map<String, Object?>;
  final body = rootSlots['body']! as Map<String, Object?>;
  final center = body['child']! as Map<String, Object?>;
  final centerSlots = center['slots']! as Map<String, Object?>;
  final centerChild = centerSlots['child']! as Map<String, Object?>;
  final box = centerChild['child']! as Map<String, Object?>;
  final boxSlots = box['slots']! as Map<String, Object?>;
  (boxSlots['child']! as Map<String, Object?>)['child'] = subject;
}

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
CanvasNode _mouseRegionNode(CanvasNode node) {
  if (node.id == _id) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      try {
        return _mouseRegionNode(child);
      } on StateError {
        /* Continue siblings. */
      }
    }
  }
  throw StateError('MouseRegion missing');
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
    'data': {'kind': 'string', 'value': 'MouseRegion child'},
  },
  'slots': <String, Object?>{},
};
