import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _ignoreId = 'b727b6af-1c4c-4195-a2ad-2e8c01d2d23a';
const _childId = '6f7344c4-7f25-4ab2-9efc-b66cb8d35828';
const _textId = '35720c22-9dca-4bc5-bd8b-5fcc7d2e1456';
const _underId = 'd3f4767c-4d2f-480e-837e-ae8145b15b8d';
const _type = 'flutter.widgets.IgnorePointer';
const _label = 'Application child semantics';

void main() {
  test(
    'IgnorePointer exact two-boolean contract is optional, closed and protocol-stable',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf('W|flutter.widgets.Image\n', start);
      expect(
        contract.substring(start, end),
        'W|$_type\n'
        'P|ignoring|boolean|0|-|-|boolean:any\n'
        'P|ignoringSemantics|boolean|0|-|-|boolean:any\n'
        'S|child|single|0|0|1|any\n',
      );
      expect(canvasModelProtocolVersion, 18);
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
      for (final ignoring in <bool?>[null, false, true]) {
        for (final semantics in <bool?>[null, false, true]) {
          final properties = _properties(ignoring, semantics);
          final input = jsonEncode(properties);
          final node = _find(_decode(_model(properties: properties)).root)!;
          expect(
            node.properties.length,
            (ignoring == null ? 0 : 1) + (semantics == null ? 0 : 1),
          );
          expect(node.properties['ignoring']?.value, ignoring);
          expect(node.properties['ignoringSemantics']?.value, semantics);
          expect(jsonEncode(properties), input);
        }
      }
    },
  );

  test(
    'IgnorePointer decoder rejects raw code, explicit null, coercion and invalid child contracts',
    () {
      for (final name in ['ignoring', 'ignoringSemantics']) {
        for (final value in [
          {'kind': 'null'},
          {'kind': 'boolean', 'value': 'true'},
          {'kind': 'boolean', 'value': 1},
          {'kind': 'boolean', 'value': true, 'extra': true},
          {'kind': 'string', 'value': 'true'},
          {'kind': 'callbackPresence'},
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
          );
        }
      }
      for (final name in ['key', 'absorbing', 'child', 'source']) {
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
      for (final slots in [
        {
          'children': {'kind': 'list', 'children': <Object?>[]},
        },
        {
          'child': {'kind': 'list', 'children': <Object?>[]},
        },
        {
          'child': {'kind': 'single', 'child': null, 'extra': true},
        },
      ]) {
        final model = _model();
        _findJson(model['root']! as Map<String, Object?>)!['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
      for (final child in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () =>
              _decode(_model(child: _node(_childId, 'flutter.widgets.$child'))),
          throwsFormatException,
        );
      }
      expect(isCanvasPaletteWrapperWidgetType(_type), isFalse);
      for (final sourceType in [
        _type,
        'flutter.widgets.Text',
        'flutter.widgets.RepaintBoundary',
      ]) {
        final source = CanvasPaletteDragSource(
          token: sourceType,
          widgetType: sourceType,
          traits: canvasWidgetTraitsForType(sourceType),
        );
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: _type,
            slotName: 'child',
            currentChildCount: 0,
            insertionIndex: 0,
            source: source,
          ),
          isTrue,
        );
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: _type,
            slotName: 'child',
            currentChildCount: 1,
            insertionIndex: 0,
            source: source,
          ),
          isFalse,
        );
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'IgnorePointer retains real layout, painting, hit semantics and state on $platform',
      (tester) async {
        RenderIgnorePointer? first;
        for (final ignoring in <bool?>[null, false, true, false]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: _properties(ignoring, null),
              child: _content(),
            ),
          );
          final widget = tester.widget<IgnorePointer>(_ignoreFinder());
          final render = tester.renderObject<RenderIgnorePointer>(
            _ignoreFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(widget.ignoring, ignoring ?? true);
          expect(render.ignoring, ignoring ?? true);
          // ignore: deprecated_member_use
          expect(widget.ignoringSemantics, isNull);
          expect(render.size, const Size(100, 80));
          expect(render.child!.size, render.size);
          expect(render.getMinIntrinsicWidth(80), 100);
          expect(render.describeApproximatePaintClip(render.child!), isNull);
          expect(
            render.hitTest(BoxHitTestResult(), position: const Offset(50, 40)),
            !(ignoring ?? true),
          );
          expect(
            render.hitTest(BoxHitTestResult(), position: const Offset(101, 40)),
            isFalse,
          );
          final color = tester.renderObject(
            find
                .descendant(of: _widget(), matching: find.byType(ColoredBox))
                .first,
          );
          expect(color, paints..rect(color: const Color(0xFF123456)));
          expect(find.text('Painted child'), findsOneWidget);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'IgnorePointer follows all SDK deprecated semantics branches on $platform',
      (tester) async {
        final semanticsHandle = tester.ensureSemantics();
        RenderIgnorePointer? first;
        for (final ignoring in [true, false, true]) {
          for (final semantics in <bool?>[null, false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: _properties(ignoring, semantics),
                child: _content(),
              ),
            );
            final render = tester.renderObject<RenderIgnorePointer>(
              _ignoreFinder(),
            );
            first ??= render;
            expect(render, same(first));
            // ignore: deprecated_member_use
            expect(render.ignoringSemantics, semantics);
            final labels = _semanticsWithLabel(
              tester
                  .binding
                  .renderViews
                  .single
                  .owner!
                  .semanticsOwner!
                  .rootSemanticsNode!,
            );
            if (semantics == true) {
              expect(
                labels,
                isEmpty,
                reason: 'legacy true drops the complete child subtree',
              );
            } else {
              expect(labels, hasLength(1));
              final data = labels.single.getSemanticsData();
              expect(
                data.hasAction(SemanticsAction.tap),
                !ignoring || semantics == false,
                reason:
                    'null removes pointer actions only while ignoring; legacy false keeps actions',
              );
            }
            expect(tester.getSize(_widget()), const Size(100, 80));
            expect(find.text('Painted child'), findsOneWidget);
            expect(tester.takeException(), isNull);
          }
        }
        semanticsHandle.dispose();
      },
    );

    testWidgets(
      'IgnorePointer passes Stack pointer hits and taps through instead of absorbing on $platform',
      (tester) async {
        for (final ignoring in [true, false]) {
          final selected = <String>[];
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  stack: true,
                  properties: _properties(ignoring, null),
                  child: _content(),
                ),
              ),
              selectedWidgetId: null,
              onSelected: selected.add,
            ),
          );
          await tester.pump();
          final render = tester.renderObject<RenderIgnorePointer>(
            _ignoreFinder(),
          );
          final hit = BoxHitTestResult();
          expect(
            render.hitTest(hit, position: const Offset(60, 60)),
            !ignoring,
          );
          if (ignoring) expect(hit.path, isEmpty);
          await tester.tapAt(render.localToGlobal(const Offset(60, 60)));
          await tester.pump();
          expect(
            selected.last,
            ignoring ? _underId : _textId,
            reason:
                'ignored subtree must not capture the tap intended for the lower Stack child',
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'IgnorePointer nonempty external handle selects without covering the ignored body on $platform',
      (tester) async {
        final selections = <String>[];
        for (final ignoring in <bool?>[null, false, true]) {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  properties: _properties(ignoring, null),
                  child: _content(),
                ),
              ),
              selectedWidgetId: null,
              onSelected: selections.add,
            ),
          );
          await tester.pump();
          final handle = find.byKey(
            const ValueKey('canvas-zero-size-widget-target-$_ignoreId'),
          );
          if (ignoring == false) {
            expect(handle, findsNothing);
          } else {
            expect(tester.getSize(handle), const Size.square(36));
            final render = tester.renderObject<RenderIgnorePointer>(
              _ignoreFinder(),
            );
            final body = Rect.fromPoints(
              render.localToGlobal(Offset.zero),
              render.localToGlobal(render.size.bottomRight(Offset.zero)),
            );
            expect(tester.getRect(handle).overlaps(body), isFalse);
            expect(
              find.ancestor(of: handle, matching: _ignoreFinder()),
              findsNothing,
            );
            await tester.tap(handle);
            expect(selections.last, _ignoreId);
            expect(
              render.hitTest(
                BoxHitTestResult(),
                position: const Offset(50, 40),
              ),
              isFalse,
            );
          }
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'IgnorePointer small body at every viewport corner keeps its entire hit area transparent on $platform',
      (tester) async {
        for (final alignment in [
          Alignment.topLeft,
          Alignment.topRight,
          Alignment.bottomLeft,
          Alignment.bottomRight,
        ]) {
          final selected = <String>[];
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  bounds: const Size(30, 30),
                  boundsInChild: true,
                  stack: true,
                  alignment: alignment,
                ),
              ),
              selectedWidgetId: null,
              onSelected: selected.add,
            ),
          );
          await tester.pump();
          final render = tester.renderObject<RenderIgnorePointer>(
            _ignoreFinder(),
          );
          final body = Rect.fromPoints(
            render.localToGlobal(Offset.zero),
            render.localToGlobal(render.size.bottomRight(Offset.zero)),
          );
          final handle = find.byKey(
            const ValueKey('canvas-zero-size-widget-target-$_ignoreId'),
          );
          expect(tester.getSize(handle), const Size.square(36));
          expect(
            tester.getRect(handle).overlaps(body),
            isFalse,
            reason: '$alignment',
          );
          await tester.tapAt(
            render.localToGlobal(render.size.center(Offset.zero)),
          );
          expect(selected.last, _underId, reason: '$alignment');
          await tester.tap(handle);
          expect(selected.last, _ignoreId);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'IgnorePointer viewport-filling body limits the explicit Designer hit exception to one compact handle on $platform',
      (tester) async {
        final selected = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                bounds: const Size(390, 844),
                boundsInChild: true,
                stack: true,
                alignment: Alignment.topLeft,
              ),
            ),
            selectedWidgetId: null,
            onSelected: selected.add,
          ),
        );
        await tester.pump();
        final render = tester.renderObject<RenderIgnorePointer>(
          _ignoreFinder(),
        );
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$_ignoreId'),
        );
        expect(tester.getSize(handle), const Size.square(36));
        final point = render.localToGlobal(render.size.center(Offset.zero));
        expect(tester.getRect(handle).contains(point), isFalse);
        await tester.tapAt(point);
        expect(selected.last, _underId);
        await tester.tap(handle);
        expect(selected.last, _ignoreId);
        expect(
          render.hitTest(BoxHitTestResult(), position: const Offset(1, 1)),
          isFalse,
          reason:
              'even beneath the external handle, the real SDK subtree ignores hits',
        );
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'IgnorePointer null child preserves zero or tight layout and external child DnD on $platform',
      (tester) async {
        for (final bounds in <Size?>[null, const Size(120, 100)]) {
          CanvasDropResolver? resolver;
          final selections = <String>[];
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(_model(platform: platform, bounds: bounds)),
              selectedWidgetId: null,
              onSelected: selections.add,
              onDropResolverChanged: (value) => resolver = value,
            ),
          );
          await tester.pump();
          final render = tester.renderObject<RenderIgnorePointer>(
            _ignoreFinder(),
          );
          expect(render.child, isNull);
          expect(tester.widget<IgnorePointer>(_ignoreFinder()).child, isNull);
          expect(render.size, bounds ?? Size.zero);
          expect(render, paintsNothing);
          final handle = find.byKey(
            const ValueKey('canvas-zero-size-widget-target-$_ignoreId'),
          );
          expect(tester.getSize(handle), const Size.square(36));
          await tester.tap(handle);
          expect(selections.last, _ignoreId);
          final point = bounds == null
              ? tester.getCenter(handle)
              : render.localToGlobal(render.size.center(Offset.zero));
          final drop = _resolve(tester, resolver!, point);
          expect(drop?.parentWidgetId, _ignoreId);
          expect(drop?.slotName, 'child');
          expect(drop?.insertionIndex, 0);
          expect(drop?.zone?.isEmpty, isFalse);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'IgnorePointer descendant updates retain render state and do not fabricate nullable children on $platform',
      (tester) async {
        RenderIgnorePointer? first;
        for (final child in [
          _content(),
          _node(
            _childId,
            'flutter.widgets.SizedBox',
            properties: {
              'width': {'kind': 'integer', 'value': 24},
              'height': {'kind': 'integer', 'value': 32},
            },
          ),
          null,
          _content(),
        ]) {
          await _pump(tester, _model(platform: platform, child: child));
          final render = tester.renderObject<RenderIgnorePointer>(
            _ignoreFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.ignoring, isTrue);
          expect(
            render.size,
            child == null
                ? Size.zero
                : child['type'] == 'flutter.widgets.SizedBox'
                ? const Size(24, 32)
                : const Size(100, 80),
          );
          if (child == null) expect(render.child, isNull);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'IgnorePointer ignored descendant remains a geometric DnD destination on $platform',
      (tester) async {
        CanvasDropResolver? resolver;
        CanvasMovePreviewResolver? moveResolver;
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                bounds: const Size(120, 100),
                stack: true,
                child: {
                  'id': _childId,
                  'type': 'flutter.widgets.Column',
                  'properties': <String, Object?>{},
                  'slots': {
                    'children': {'kind': 'list', 'children': <Object?>[]},
                  },
                },
              ),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
            onMovePreviewResolverChanged: (value) => moveResolver = value,
          ),
        );
        await tester.pump();
        final render = tester.renderObject<RenderIgnorePointer>(
          _ignoreFinder(),
        );
        expect(
          render.hitTest(BoxHitTestResult(), position: const Offset(60, 50)),
          isFalse,
        );
        final drop = _resolve(
          tester,
          resolver!,
          render.localToGlobal(const Offset(60, 50)),
        );
        expect(drop?.parentWidgetId, _childId);
        expect(drop?.slotName, 'children');
        expect(drop?.insertionIndex, 0);
        final move = moveResolver!(_underId, _childId, 'children', 0);
        expect(move?.parentWidgetId, _childId);
        expect(move?.slotName, 'children');
        expect(move?.insertionIndex, 0);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'IgnorePointer tree-selected descendant keeps keyboard editing while pointer hits stay ignored on $platform',
      (tester) async {
        final commits = <(String, String)>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(_model(platform: platform, child: _content())),
            selectedWidgetId: _textId,
            onSelected: (_) {},
            inlineTextEditEnabled: true,
            onInlineTextCommit: (id, text, _) {
              commits.add((id, text));
              return true;
            },
          ),
        );
        await tester.pump();
        final render = tester.renderObject<RenderIgnorePointer>(
          _ignoreFinder(),
        );
        expect(
          render.hitTest(BoxHitTestResult(), position: const Offset(50, 40)),
          isFalse,
        );
        await tester.tapAt(
          tester.getTopLeft(
                find.byKey(const ValueKey('canvas-interaction-surface')),
              ) +
              const Offset(2, 2),
        );
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        expect(
          find.byKey(const ValueKey('canvas-inline-text-editor-$_textId')),
          findsOneWidget,
        );
        await tester.enterText(find.byType(TextField), 'Updated ignored child');
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_textId, 'Updated ignored child')]);
        expect(find.byType(TextField), findsNothing);
        expect(render.ignoring, isTrue);
        expect(
          render.hitTest(BoxHitTestResult(), position: const Offset(50, 40)),
          isFalse,
        );
        expect(tester.takeException(), isNull);
      },
    );
  }
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

Finder _widget() => find.byKey(const ValueKey('canvas-widget-$_ignoreId'));
CanvasDropTarget? _resolve(
  WidgetTester tester,
  CanvasDropResolver resolver,
  Offset point,
) {
  final surface = tester.getRect(find.byType(CanvasDocumentView));
  return resolver(
    ((point.dx - surface.left) / surface.width * 1000000).round(),
    ((point.dy - surface.top) / surface.height * 1000000).round(),
  );
}

List<SemanticsNode> _semanticsWithLabel(SemanticsNode root) {
  final result = <SemanticsNode>[];
  void visit(SemanticsNode node) {
    if (node.label.contains(_label)) result.add(node);
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(root);
  return result;
}

Finder _ignoreFinder() =>
    find.descendant(of: _widget(), matching: find.byType(IgnorePointer)).first;
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode? _find(CanvasNode node) {
  if (node.id == _ignoreId) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _find(child);
      if (found != null) return found;
    }
  }
  return null;
}

Map<String, Object?>? _findJson(Map<String, Object?> node) {
  if (node['id'] == _ignoreId) return node;
  for (final value in (node['slots']! as Map<String, Object?>).values) {
    final slot = value! as Map<String, Object?>;
    if (slot['child'] case final Map<String, Object?> child) {
      final found = _findJson(child);
      if (found != null) return found;
    }
    if (slot['children'] case final List<Object?> children) {
      for (final child in children) {
        final found = _findJson(child! as Map<String, Object?>);
        if (found != null) return found;
      }
    }
  }
  return null;
}

Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
  bool stack = false,
  Size? bounds,
  bool boundsInChild = false,
  Alignment? alignment,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  var content = _node(_ignoreId, _type, child: child, properties: properties);
  if (bounds != null) {
    final sized = _node(
      'e03e4228-2d19-46ba-92d6-e4acdb40796e',
      'flutter.widgets.SizedBox',
      properties: {
        'width': {'kind': 'double', 'value': bounds.width},
        'height': {'kind': 'double', 'value': bounds.height},
      },
      child: boundsInChild ? child : content,
    );
    content = boundsInChild
        ? _node(_ignoreId, _type, child: sized, properties: properties)
        : sized;
  }
  if (stack) {
    content = {
      'id': '682ca908-082d-4465-8e4d-fafcfd9cfb7a',
      'type': 'flutter.widgets.Stack',
      'properties': <String, Object?>{},
      'slots': {
        'children': {
          'kind': 'list',
          'children': [
            _node(
              _underId,
              'flutter.widgets.SizedBox',
              properties: {
                'width': {'kind': 'double', 'value': bounds?.width ?? 100},
                'height': {'kind': 'double', 'value': bounds?.height ?? 80},
              },
            ),
            content,
          ],
        },
      },
    };
  }
  model['root'] = {
    'id': '0d279f63-dfd5-4c5f-8e60-8d5c11aab787',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          '1d6c16df-df2b-41cf-b46d-2e1d85538277',
          alignment == null
              ? 'flutter.widgets.Center'
              : 'flutter.widgets.Align',
          properties: {
            if (alignment != null)
              'alignment': {
                'kind': 'alignmentGeometry',
                'basis': 'physical',
                'horizontal': alignment.x,
                'vertical': alignment.y,
              },
          },
          child: content,
        ),
      },
    },
  };
  return model;
}

Map<String, Object?> _properties(bool? ignoring, bool? semantics) => {
  if (ignoring != null) 'ignoring': {'kind': 'boolean', 'value': ignoring},
  if (semantics != null)
    'ignoringSemantics': {'kind': 'boolean', 'value': semantics},
};
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
Map<String, Object?> _content() => _node(
  _childId,
  'flutter.widgets.ColoredBox',
  properties: {
    'color': {'kind': 'color', 'argb': '0xFF123456'},
  },
  child: _node(
    '4c58a44b-89b5-42c8-bec8-a1fbc4a1b51f',
    'flutter.widgets.SizedBox',
    properties: {
      'width': {'kind': 'integer', 'value': 100},
      'height': {'kind': 'integer', 'value': 80},
    },
    child: {
      'id': _textId,
      'type': 'flutter.widgets.Text',
      'properties': {
        'data': {'kind': 'string', 'value': 'Painted child'},
        'semanticsLabel': {'kind': 'string', 'value': _label},
      },
      'slots': <String, Object?>{},
    },
  ),
);
