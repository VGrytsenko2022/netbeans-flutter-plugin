import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _absorbId = 'f047619d-465a-4e66-9abc-19199167a7c0';
const _childId = '9ce992a6-53f5-464f-8f2f-dcab5761b603';
const _textId = '8fb279bc-7504-4424-852c-21e1c62a6d2c';
const _underId = '46b7e71d-e4dc-4f82-8eee-68cc49981683';
const _type = 'flutter.widgets.AbsorbPointer';
const _label = 'Application child semantics';

void main() {
  test(
    'AbsorbPointer exact optional boolean contract preserves all nine omission states',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf('W|flutter.widgets.Align\n', start);
      expect(
        contract.substring(start, end),
        'W|$_type\n'
        'P|absorbing|boolean|0|-|-|boolean:any\n'
        'P|ignoringSemantics|boolean|0|-|-|boolean:any\n'
        'S|child|single|0|0|1|any\n',
      );
      expect(canvasModelProtocolVersion, 19);
      expect(isCanvasReviewedWidgetType(_type), isTrue);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(isCanvasPaletteWrapperWidgetType(_type), isFalse);
      for (final absorbing in <bool?>[null, false, true]) {
        for (final semantics in <bool?>[null, false, true]) {
          final properties = _properties(absorbing, semantics);
          final saved = jsonEncode(properties);
          final node = _find(_decode(_model(properties: properties)).root)!;
          expect(
            node.properties.length,
            (absorbing == null ? 0 : 1) + (semantics == null ? 0 : 1),
          );
          expect(node.properties['absorbing']?.value, absorbing);
          expect(node.properties['ignoringSemantics']?.value, semantics);
          expect(jsonEncode(properties), saved);
        }
      }
    },
  );

  test(
    'AbsorbPointer rejects coercion, null values, unreviewed fields, raw code and invalid child slots',
    () {
      for (final name in ['absorbing', 'ignoringSemantics']) {
        for (final value in [
          {'kind': 'null'},
          {'kind': 'boolean', 'value': 'true'},
          {'kind': 'boolean', 'value': 1},
          {'kind': 'boolean', 'value': true, 'extra': true},
          {'kind': 'string', 'value': 'executeProjectCode()'},
          {'kind': 'callbackPresence'},
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
          );
        }
      }
      for (final name in ['key', 'ignoring', 'child', 'source', 'onTap']) {
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
        {
          'unknown': {'kind': 'single', 'child': null},
        },
      ]) {
        final model = _model();
        _findJson(model['root']! as Map<String, Object?>)!['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
      final absentSlot = _model();
      _findJson(absentSlot['root']! as Map<String, Object?>)!['slots'] =
          <String, Object?>{};
      expect(_find(_decode(absentSlot).root)!.slot('child')?.child, isNull);
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
    'AbsorbPointer DnD closes ordinary source and optional child capacity',
    () {
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
      for (final sourceType in [
        _type,
        'flutter.widgets.Text',
        'flutter.widgets.IgnorePointer',
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
      final source = CanvasPaletteDragSource(
        token: _type,
        widgetType: _type,
        traits: const {},
      );
      for (final destination in [
        (type: _type, slot: 'child'),
        (type: 'flutter.widgets.Column', slot: 'children'),
        (type: 'flutter.material.Scaffold', slot: 'body'),
      ]) {
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: destination.type,
            slotName: destination.slot,
            currentChildCount: 0,
            insertionIndex: 0,
            source: source,
          ),
          isTrue,
        );
      }
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.material.Scaffold',
          slotName: 'appBar',
          currentChildCount: 0,
          insertionIndex: 0,
          source: source,
        ),
        isFalse,
      );
    },
  );

  test(
    'real AbsorbPointer terminates Stack hit traversal while IgnorePointer passes to the lower sibling',
    () {
      for (final absorb in [true, false]) {
        final lower = RenderPointerListener(behavior: HitTestBehavior.opaque);
        final child = RenderPointerListener(behavior: HitTestBehavior.opaque);
        final barrier = absorb
            ? RenderAbsorbPointer(child: child)
            : RenderIgnorePointer(child: child);
        final stack = RenderStack(
          children: [lower, barrier],
          textDirection: TextDirection.ltr,
          fit: StackFit.expand,
        );
        stack.layout(BoxConstraints.tight(const Size(100, 80)));
        final hit = BoxHitTestResult();
        expect(stack.hitTest(hit, position: const Offset(50, 40)), isTrue);
        expect(hit.path.any((entry) => entry.target == child), isFalse);
        expect(hit.path.any((entry) => entry.target == lower), !absorb);
        final direct = BoxHitTestResult();
        expect(barrier.hitTest(direct, position: const Offset(50, 40)), absorb);
        expect(
          direct.path,
          isEmpty,
          reason:
              'absorption returns true without delivering a pointer event to itself or its child',
        );
        expect(
          barrier.hitTest(BoxHitTestResult(), position: const Offset(101, 40)),
          isFalse,
        );
        stack.removeAll();
        barrier.child = null;
        barrier.dispose();
        child.dispose();
        lower.dispose();
        stack.dispose();
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'AbsorbPointer retains actual renderer, painting and intrinsic layout across boolean updates on $platform',
      (tester) async {
        RenderAbsorbPointer? first;
        for (final absorbing in <bool?>[null, false, true, false]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: _properties(absorbing, null),
              child: _content(),
            ),
          );
          final render = tester.renderObject<RenderAbsorbPointer>(
            _absorbFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.absorbing, absorbing ?? true);
          expect(
            tester.widget<AbsorbPointer>(_absorbFinder()).absorbing,
            absorbing ?? true,
          );
          expect(render.size, const Size(100, 80));
          expect(render.child!.size, render.size);
          expect(render.getMinIntrinsicWidth(80), 100);
          expect(render.getMaxIntrinsicWidth(80), 100);
          expect(render.getMinIntrinsicHeight(100), 80);
          expect(render.getMaxIntrinsicHeight(100), 80);
          expect(render.describeApproximatePaintClip(render.child!), isNull);
          final hit = BoxHitTestResult();
          expect(render.hitTest(hit, position: const Offset(50, 40)), isTrue);
          expect(hit.path.isEmpty, absorbing ?? true);
          expect(
            render.hitTest(BoxHitTestResult(), position: const Offset(101, 40)),
            isFalse,
          );
          final paint = tester.renderObject(
            find
                .descendant(of: _widget(), matching: find.byType(ColoredBox))
                .first,
          );
          expect(paint, paints..rect(color: const Color(0xFF123456)));
          expect(find.text('Painted child'), findsOneWidget);
          expect(
            find.byKey(
              const ValueKey('canvas-zero-size-widget-target-$_absorbId'),
            ),
            findsNothing,
            reason:
                'nonempty AbsorbPointer keeps normal body selection, not an IgnorePointer handle',
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'AbsorbPointer honors all nine deprecated semantics combinations and restores omitted defaults on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        RenderAbsorbPointer? first;
        for (final absorbing in <bool?>[null, false, true, null]) {
          for (final semantics in <bool?>[null, false, true, null]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: _properties(absorbing, semantics),
                child: _content(),
              ),
            );
            final render = tester.renderObject<RenderAbsorbPointer>(
              _absorbFinder(),
            );
            first ??= render;
            expect(render, same(first));
            expect(render.absorbing, absorbing ?? true);
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
              expect(labels, isEmpty);
            } else {
              expect(labels, hasLength(1));
              expect(
                labels.single.getSemanticsData().hasAction(SemanticsAction.tap),
                !(absorbing ?? true) || semantics == false,
              );
            }
            expect(find.text('Painted child'), findsOneWidget);
            expect(render.size, const Size(100, 80));
            expect(tester.takeException(), isNull);
          }
        }
        handle.dispose();
      },
    );

    testWidgets(
      'AbsorbPointer selected and unselected bodies stop lower Stack taps while child remains blocked on $platform',
      (tester) async {
        for (final absorbing in <bool?>[null, false, true]) {
          for (final selection in <String?>[null, _absorbId]) {
            final selected = <String>[];
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(
                  _model(
                    platform: platform,
                    properties: _properties(absorbing, null),
                    child: _content(),
                    stack: true,
                  ),
                ),
                selectedWidgetId: selection,
                onSelected: selected.add,
              ),
            );
            await tester.pump();
            final render = tester.renderObject<RenderAbsorbPointer>(
              _absorbFinder(),
            );
            await tester.tapAt(render.localToGlobal(const Offset(60, 60)));
            await tester.pump();
            expect(selected, [(absorbing ?? true) ? _absorbId : _textId]);
            expect(selected, isNot(contains(_underId)));
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'AbsorbPointer nullable children preserve zero and tight layout with exact selection and child DnD on $platform',
      (tester) async {
        for (final bounds in <Size?>[null, const Size(120, 100)]) {
          for (final absorbing in [true, false]) {
            CanvasDropResolver? resolver;
            final selected = <String>[];
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(
                  _model(
                    platform: platform,
                    bounds: bounds,
                    properties: _properties(absorbing, null),
                  ),
                ),
                selectedWidgetId: null,
                onSelected: selected.add,
                onDropResolverChanged: (value) => resolver = value,
              ),
            );
            await tester.pump();
            final render = tester.renderObject<RenderAbsorbPointer>(
              _absorbFinder(),
            );
            expect(render.size, bounds ?? Size.zero);
            expect(render.child, isNull);
            expect(tester.widget<AbsorbPointer>(_absorbFinder()).child, isNull);
            expect(render, paintsNothing);
            expect(
              render.hitTest(
                BoxHitTestResult(),
                position: const Offset(10, 10),
              ),
              absorbing && bounds != null,
            );
            final external = find.byKey(
              const ValueKey('canvas-zero-size-widget-target-$_absorbId'),
            );
            final point = bounds == null
                ? tester.getCenter(external)
                : render.localToGlobal(render.size.center(Offset.zero));
            if (bounds == null) {
              expect(tester.getSize(external), const Size.square(36));
              expect(
                find.ancestor(of: external, matching: _absorbFinder()),
                findsNothing,
              );
            } else {
              expect(external, findsNothing);
            }
            await tester.tapAt(point);
            expect(selected.last, _absorbId);
            final drop = _resolve(tester, resolver!, point);
            expect(drop?.parentWidgetId, _absorbId);
            expect(drop?.slotName, 'child');
            expect(drop?.insertionIndex, 0);
            expect(drop?.zone?.isEmpty, isFalse);
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'AbsorbPointer live descendant properties repaint and relayout without recreating retained renderers on $platform',
      (tester) async {
        RenderAbsorbPointer? first;
        RenderObject? firstPaint;
        for (final value in [
          (argb: '0xFFABCDEF', text: 'First child', width: 90, height: 60),
          (argb: '0x80123456', text: 'Updated child', width: 120, height: 95),
        ]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              child: _content(
                argb: value.argb,
                text: value.text,
                width: value.width,
                height: value.height,
              ),
            ),
          );
          final render = tester.renderObject<RenderAbsorbPointer>(
            _absorbFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.absorbing, isTrue);
          expect(
            render.size,
            Size(value.width.toDouble(), value.height.toDouble()),
          );
          final painted = tester.renderObject(
            find
                .descendant(of: _widget(), matching: find.byType(ColoredBox))
                .first,
          );
          firstPaint ??= painted;
          expect(painted, same(firstPaint));
          expect(
            painted,
            paints..rect(
              color: Color(int.parse(value.argb.substring(2), radix: 16)),
            ),
          );
          expect(find.text(value.text), findsOneWidget);
          final hit = BoxHitTestResult();
          expect(
            render.hitTest(hit, position: render.size.center(Offset.zero)),
            isTrue,
          );
          expect(hit.path, isEmpty);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'AbsorbPointer child replacement and removal retain renderer and accept fresh descendant paint on $platform',
      (tester) async {
        RenderAbsorbPointer? first;
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
          final render = tester.renderObject<RenderAbsorbPointer>(
            _absorbFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.absorbing, isTrue);
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
      'AbsorbPointer blocked descendants remain geometric insertion and move destinations on $platform',
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
        final render = tester.renderObject<RenderAbsorbPointer>(
          _absorbFinder(),
        );
        final hit = BoxHitTestResult();
        expect(render.hitTest(hit, position: const Offset(60, 50)), isTrue);
        expect(hit.path, isEmpty);
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
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'AbsorbPointer tree-selected text keeps F2 keyboard editing without disabling absorption on $platform',
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
        final render = tester.renderObject<RenderAbsorbPointer>(
          _absorbFinder(),
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
        await tester.enterText(
          find.byType(TextField),
          'Updated absorbed child',
        );
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_textId, 'Updated absorbed child')]);
        expect(find.byType(TextField), findsNothing);
        expect(render.absorbing, isTrue);
        final hit = BoxHitTestResult();
        expect(render.hitTest(hit, position: const Offset(50, 40)), isTrue);
        expect(hit.path, isEmpty);
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

Finder _widget() => find.byKey(const ValueKey('canvas-widget-$_absorbId'));
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

Finder _absorbFinder() =>
    find.descendant(of: _widget(), matching: find.byType(AbsorbPointer)).first;
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode? _find(CanvasNode node) {
  if (node.id == _absorbId) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _find(child);
      if (found != null) return found;
    }
  }
  return null;
}

Map<String, Object?>? _findJson(Map<String, Object?> node) {
  if (node['id'] == _absorbId) return node;
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
  var content = _node(_absorbId, _type, child: child, properties: properties);
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
        ? _node(_absorbId, _type, child: sized, properties: properties)
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

Map<String, Object?> _properties(bool? absorbing, bool? semantics) => {
  if (absorbing != null) 'absorbing': {'kind': 'boolean', 'value': absorbing},
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
Map<String, Object?> _content({
  String argb = '0xFF123456',
  String text = 'Painted child',
  int width = 100,
  int height = 80,
}) => _node(
  _childId,
  'flutter.widgets.ColoredBox',
  properties: {
    'color': {'kind': 'color', 'argb': argb},
  },
  child: _node(
    '4c58a44b-89b5-42c8-bec8-a1fbc4a1b51f',
    'flutter.widgets.SizedBox',
    properties: {
      'width': {'kind': 'integer', 'value': width},
      'height': {'kind': 'integer', 'value': height},
    },
    child: {
      'id': _textId,
      'type': 'flutter.widgets.Text',
      'properties': {
        'data': {'kind': 'string', 'value': text},
        'semanticsLabel': {'kind': 'string', 'value': _label},
      },
      'slots': <String, Object?>{},
    },
  ),
);
