import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _blockId = 'cf131639-1d5d-48ca-8322-de5b8a1b0994';
const _childId = '3c50b67e-bfbd-49ed-ae0b-0fdf656ffca5';
const _textId = '402e13eb-5b34-43b0-a6ed-8f2660e98678';
const _underId = '9781441f-a82f-4e14-8be0-dbb390eb2d04';
const _beforeId = 'bc4b796a-9261-4d3b-8371-d3ec96a8951e';
const _afterId = '722c9074-de7d-445f-9c05-250ce3bb4e98';
const _type = 'flutter.widgets.BlockSemantics';
const _label = 'Application child semantics';
const _beforeLabel = 'Earlier painted application';
const _afterLabel = 'Later painted application';
const _outsideLabel = 'Outside earlier application';

void main() {
  test(
    'BlockSemantics exact one-boolean optional contract and unchanged protocol are closed',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf('W|flutter.widgets.Center\n', start);
      expect(
        contract.substring(start, end),
        'W|$_type\nP|blocking|boolean|0|-|-|boolean:any\nS|child|single|0|0|1|any\n',
      );
      expect(canvasModelProtocolVersion, 18);
      expect(isCanvasReviewedWidgetType(_type), isTrue);
      expect(isCanvasPaletteWrapperWidgetType(_type), isFalse);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      for (final value in <bool?>[null, false, true]) {
        final properties = _properties(value);
        final before = jsonEncode(properties);
        final node = _find(_decode(_model(properties: properties)).root)!;
        expect(node.properties.length, value == null ? 0 : 1);
        expect(node.properties['blocking']?.value, value);
        expect(jsonEncode(properties), before);
      }
    },
  );

  test(
    'BlockSemantics decoder rejects invented semantics flags, null, raw code, wrong kinds and invalid slots',
    () {
      for (final value in [
        {'kind': 'null'},
        {'kind': 'boolean', 'value': 'true'},
        {'kind': 'boolean', 'value': 1},
        {'kind': 'boolean', 'value': true, 'extra': true},
        {'kind': 'string', 'value': 'runProjectCode()'},
        {'kind': 'callbackPresence'},
      ]) {
        expect(
          () => _decode(_model(properties: {'blocking': value})),
          throwsFormatException,
        );
      }
      for (final name in [
        'key',
        'child',
        'excluding',
        'absorbing',
        'ignoring',
        'ignoringSemantics',
        'container',
        'blockUserActions',
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
      final absent = _model();
      _findJson(absent['root']! as Map<String, Object?>)!['slots'] =
          <String, Object?>{};
      expect(_find(_decode(absent).root)!.slot('child')?.child, isNull);
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
    'BlockSemantics DnD accepts an ordinary optional child and preserves source and occupied-slot contracts',
    () {
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
      for (final type in [
        _type,
        'flutter.widgets.Text',
        'flutter.widgets.ExcludeSemantics',
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

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'BlockSemantics full Canvas honors paint order and keeps own and later semantics on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        RenderBlockSemantics? first;
        for (final blocking in <bool?>[null, false, true, null]) {
          await _pump(
            tester,
            _orderedModel(platform: platform, blocking: blocking),
          );
          final labels = _allSemantics(
            tester,
          ).map((node) => node.label).join('\n');
          final render = tester.renderObject<RenderBlockSemantics>(
            _blockFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.blocking, blocking ?? true);
          final config = SemanticsConfiguration();
          render.describeSemanticsConfiguration(config);
          expect(
            config.isBlockingSemanticsOfPreviouslyPaintedNodes,
            blocking ?? true,
          );
          expect(
            labels.contains(_outsideLabel),
            !(blocking ?? true),
            reason:
                'same-container earlier ancestor sibling must also be blocked: $labels',
          );
          expect(
            labels.contains(_beforeLabel),
            !(blocking ?? true),
            reason: labels,
          );
          expect(labels.contains(_label), isTrue, reason: labels);
          expect(labels.contains(_afterLabel), isTrue, reason: labels);
          expect(
            labels.contains('BlockSemantics $_blockId'),
            isTrue,
            reason: 'Designer identity remains accessible: $labels',
          );
          expect(tester.takeException(), isNull);
        }
        handle.dispose();
      },
    );

    testWidgets(
      'BlockSemantics full Canvas childless and populated positions obey exact paint order in both scopes on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        for (final boundary in [false, true]) {
          for (final ownChild in [false, true]) {
            for (final blocking in <bool?>[null, false, true]) {
              for (final position in [0, 1, 2]) {
                await _pump(
                  tester,
                  _orderedModel(
                    platform: platform,
                    blocking: blocking,
                    boundary: boundary,
                    ownChild: ownChild,
                    blockerIndex: position,
                  ),
                );
                final labels = _allSemantics(
                  tester,
                ).map((node) => node.label).join('\n');
                final enabled = blocking ?? true;
                expect(
                  labels.contains(_beforeLabel),
                  !enabled || position == 0,
                  reason:
                      'position=$position child=$ownChild boundary=$boundary\n$labels',
                );
                expect(
                  labels.contains(_afterLabel),
                  !enabled || position < 2,
                  reason: labels,
                );
                expect(labels.contains(_label), ownChild, reason: labels);
                expect(
                  labels.contains(_outsideLabel),
                  boundary || !enabled,
                  reason: labels,
                );
                final render = tester.renderObject<RenderBlockSemantics>(
                  _blockFinder(),
                );
                if (!ownChild) {
                  expect(render.child, isNull);
                  expect(
                    tester.widget<BlockSemantics>(_blockFinder()).child,
                    isNull,
                  );
                }
                expect(tester.takeException(), isNull);
              }
            }
          }
        }
        handle.dispose();
      },
    );

    testWidgets(
      'BlockSemantics preserves real layout intrinsics paint and child pointer selection across live updates on $platform',
      (tester) async {
        RenderBlockSemantics? first;
        for (final blocking in <bool?>[null, false, true, null]) {
          final selected = <String>[];
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  properties: _properties(blocking),
                  child: _content(),
                ),
              ),
              selectedWidgetId: null,
              onSelected: selected.add,
            ),
          );
          await tester.pump();
          final render = tester.renderObject<RenderBlockSemantics>(
            _blockFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.blocking, blocking ?? true);
          expect(render.size, const Size(100, 80));
          expect(render.getMinIntrinsicWidth(80), 100);
          expect(render.getMaxIntrinsicWidth(80), 100);
          expect(render.getMinIntrinsicHeight(100), 80);
          expect(render.getMaxIntrinsicHeight(100), 80);
          expect(render.describeApproximatePaintClip(render.child!), isNull);
          final hit = BoxHitTestResult();
          expect(render.hitTest(hit, position: const Offset(60, 60)), isTrue);
          expect(
            hit.path,
            isNotEmpty,
            reason: 'semantics blocking must not absorb child pointer hits',
          );
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
          await tester.tapAt(render.localToGlobal(const Offset(60, 60)));
          expect(selected, [_textId]);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'BlockSemantics nullable child has exact zero and tight layout with selectable body or external target and DnD on $platform',
      (tester) async {
        for (final bounds in <Size?>[null, const Size(120, 100)]) {
          for (final blocking in [true, false]) {
            CanvasDropResolver? resolver;
            final selected = <String>[];
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(
                  _model(
                    platform: platform,
                    properties: _properties(blocking),
                    bounds: bounds,
                  ),
                ),
                selectedWidgetId: null,
                onSelected: selected.add,
                onDropResolverChanged: (value) => resolver = value,
              ),
            );
            await tester.pump();
            final render = tester.renderObject<RenderBlockSemantics>(
              _blockFinder(),
            );
            expect(render.child, isNull);
            expect(tester.widget<BlockSemantics>(_blockFinder()).child, isNull);
            expect(render.size, bounds ?? Size.zero);
            expect(render, paintsNothing);
            expect(
              render.hitTest(
                BoxHitTestResult(),
                position: const Offset(10, 10),
              ),
              isFalse,
            );
            final external = find.byKey(
              const ValueKey('canvas-zero-size-widget-target-$_blockId'),
            );
            if (bounds == null) {
              expect(tester.getSize(external), const Size.square(36));
              expect(
                find.ancestor(of: external, matching: _blockFinder()),
                findsNothing,
              );
            } else {
              expect(external, findsNothing);
            }
            final point = bounds == null
                ? tester.getCenter(external)
                : render.localToGlobal(render.size.center(Offset.zero));
            await tester.tapAt(point);
            expect(selected.last, _blockId);
            final drop = _resolve(tester, resolver!, point);
            expect(drop?.parentWidgetId, _blockId);
            expect(drop?.slotName, 'child');
            expect(drop?.insertionIndex, 0);
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'BlockSemantics retained renderer handles child paint updates replacement and removal on $platform',
      (tester) async {
        RenderBlockSemantics? first;
        for (final child in [
          _content(argb: '0xFFABCDEF', text: 'Initial', width: 90, height: 60),
          _content(argb: '0x80123456', text: 'Updated', width: 120, height: 95),
          _node(
            _childId,
            'flutter.widgets.SizedBox',
            properties: {
              'width': {'kind': 'integer', 'value': 24},
              'height': {'kind': 'integer', 'value': 32},
            },
          ),
          null,
        ]) {
          await _pump(tester, _model(platform: platform, child: child));
          final render = tester.renderObject<RenderBlockSemantics>(
            _blockFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.blocking, isTrue);
          if (child == null) {
            expect(render.child, isNull);
            expect(render.size, Size.zero);
          } else if (child['type'] == 'flutter.widgets.ColoredBox') {
            final color = tester.widget<ColoredBox>(
              find
                  .descendant(of: _widget(), matching: find.byType(ColoredBox))
                  .first,
            );
            expect(
              tester.renderObject(find.byWidget(color)),
              paints..rect(color: color.color),
            );
            expect(
              render.size,
              color.color == const Color(0xFFABCDEF)
                  ? const Size(90, 60)
                  : const Size(120, 95),
            );
          } else {
            expect(render.size, const Size(24, 32));
          }
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'BlockSemantics nested child retains geometric palette and move destinations on $platform',
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
        final render = tester.renderObject<RenderBlockSemantics>(
          _blockFinder(),
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
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'BlockSemantics earlier hidden application text remains tree-selected and F2 editable on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        final commits = <(String, String)>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(_orderedModel(platform: platform)),
            selectedWidgetId: _beforeId,
            onSelected: (_) {},
            inlineTextEditEnabled: true,
            onInlineTextCommit: (id, text, _) {
              commits.add((id, text));
              return true;
            },
          ),
        );
        await tester.pump();
        expect(
          _allSemantics(
            tester,
          ).any((node) => node.label.contains(_beforeLabel)),
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
          find.byKey(const ValueKey('canvas-inline-text-editor-$_beforeId')),
          findsOneWidget,
        );
        await tester.enterText(find.byType(TextField), 'Updated earlier child');
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_beforeId, 'Updated earlier child')]);
        expect(find.byType(TextField), findsNothing);
        expect(
          tester.renderObject<RenderBlockSemantics>(_blockFinder()).blocking,
          isTrue,
        );
        expect(tester.takeException(), isNull);
        semantics.dispose();
      },
    );

    testWidgets(
      'BlockSemantics full Canvas stops at a real button semantic boundary on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        for (final blocking in <bool?>[null, false, true]) {
          await _pump(
            tester,
            _orderedModel(
              platform: platform,
              blocking: blocking,
              boundary: true,
            ),
          );
          final labels = _allSemantics(
            tester,
          ).map((node) => node.label).join('\n');
          expect(
            labels.contains(_beforeLabel),
            !(blocking ?? true),
            reason: labels,
          );
          expect(labels.contains(_label), isTrue, reason: labels);
          expect(labels.contains(_afterLabel), isTrue, reason: labels);
          expect(labels.contains(_outsideLabel), isTrue, reason: labels);
          expect(tester.takeException(), isNull);
        }
        handle.dispose();
      },
    );
  }
}

List<SemanticsNode> _allSemantics(WidgetTester tester) {
  final result = <SemanticsNode>[];
  void visit(SemanticsNode node) {
    result.add(node);
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(
    tester.binding.renderViews.single.owner!.semanticsOwner!.rootSemanticsNode!,
  );
  return result;
}

Map<String, Object?> _orderedModel({
  String platform = 'windows',
  bool? blocking,
  bool boundary = false,
  bool ownChild = true,
  int blockerIndex = 1,
}) {
  final model = _model(
    platform: platform,
    properties: _properties(blocking),
    child: ownChild ? _content() : null,
  );
  final root = model['root']! as Map<String, Object?>;
  final block = _findJson(root)!;
  final children = <Object?>[
    _textNode(_beforeId, _beforeLabel),
    _textNode(_afterId, _afterLabel),
  ];
  children.insert(blockerIndex, block);
  var ordered = <String, Object?>{
    'id': 'f4a7c8b0-f8ea-4103-8f4a-b563a59c9dcd',
    'type': 'flutter.widgets.Stack',
    'properties': <String, Object?>{},
    'slots': {
      'children': {'kind': 'list', 'children': children},
    },
  };
  if (boundary) {
    ordered = _node(
      '50a57c34-8000-4ddf-9b43-f526314ce1cf',
      'flutter.material.ElevatedButton',
      child: ordered,
    );
  }
  final outer = <String, Object?>{
    'id': '0d12c679-f507-46ba-afc5-dab54b94bc6d',
    'type': 'flutter.widgets.Column',
    'properties': <String, Object?>{},
    'slots': {
      'children': {
        'kind': 'list',
        'children': [
          _textNode('fc3b1b1f-c920-454a-b511-a44bd3d05c60', _outsideLabel),
          ordered,
        ],
      },
    },
  };
  final center =
      ((root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child']!
          as Map<String, Object?>;
  ((center['slots']! as Map<String, Object?>)['child']!
          as Map<String, Object?>)['child'] =
      outer;
  return model;
}

Map<String, Object?> _textNode(String id, String label) => {
  'id': id,
  'type': 'flutter.widgets.Text',
  'properties': {
    'data': {'kind': 'string', 'value': label},
    'semanticsLabel': {'kind': 'string', 'value': label},
  },
  'slots': <String, Object?>{},
};

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

Finder _widget() => find.byKey(const ValueKey('canvas-widget-$_blockId'));
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

Finder _blockFinder() =>
    find.descendant(of: _widget(), matching: find.byType(BlockSemantics)).first;
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode? _find(CanvasNode node) {
  if (node.id == _blockId) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _find(child);
      if (found != null) return found;
    }
  }
  return null;
}

Map<String, Object?>? _findJson(Map<String, Object?> node) {
  if (node['id'] == _blockId) return node;
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
  var content = _node(_blockId, _type, child: child, properties: properties);
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
        ? _node(_blockId, _type, child: sized, properties: properties)
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

Map<String, Object?> _properties(bool? blocking) => {
  if (blocking != null) 'blocking': {'kind': 'boolean', 'value': blocking},
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
