import 'dart:convert';
import 'dart:ui' show CheckedState;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _mergeId = 'c5ef4c71-1c22-4ec1-85ba-391d603abf0a';
const _childId = '7cb257c5-b41c-4af4-9bd0-cba78d7b0116';
const _textId = 'c54b037f-03a0-46d2-98ea-14d7a59df095';
const _secondId = 'ee29fb87-bb0f-48df-ae94-398dafab850d';
const _underId = 'd7a5aa86-5c61-4c07-82ce-136fb53e07ce';
const _type = 'flutter.widgets.MergeSemantics';
const _label = 'Application child semantics';

void main() {
  test(
    'MergeSemantics exact structural contract has no scalar or default and one ordinary nullable child',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf('W|flutter.widgets.Offstage\n', start);
      expect(
        contract.substring(start, end),
        'W|$_type\nS|child|single|0|0|1|any\n',
      );
      expect(canvasModelProtocolVersion, 18);
      expect(_find(_decode(_model()).root)!.properties, isEmpty);
      expect(isCanvasReviewedWidgetType(_type), isTrue);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(isCanvasPaletteWrapperWidgetType(_type), isFalse);
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
    },
  );

  test(
    'MergeSemantics rejects invented flags raw code key and invalid child contracts',
    () {
      for (final name in [
        'key',
        'child',
        'merge',
        'enabled',
        'blocking',
        'container',
        'explicitChildNodes',
        'source',
        'onTap',
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
    'MergeSemantics source and child destinations preserve capacity and trait restrictions',
    () {
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
          parentWidgetType: _type,
          slotName: 'child',
          currentChildCount: 1,
          insertionIndex: 0,
          source: source,
        ),
        isFalse,
      );
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

  testWidgets(
    'real SDK MergeSemantics chooses first tree-order callback and checked conflict without reconciliation',
    (tester) async {
      final handle = tester.ensureSemantics();
      final calls = <String>[];
      for (final reverse in [false, true]) {
        final children = [
          Semantics(
            label: 'Checked first',
            checked: true,
            onTap: () => calls.add('first'),
            child: const SizedBox(width: 30, height: 30),
          ),
          Semantics(
            label: 'Unchecked second',
            checked: false,
            onTap: () => calls.add('second'),
            child: const SizedBox(width: 30, height: 30),
          ),
        ];
        await tester.pumpWidget(
          MaterialApp(
            home: MergeSemantics(
              child: Column(
                children: reverse ? children.reversed.toList() : children,
              ),
            ),
          ),
        );
        final merged = _exportedSemantics(tester).singleWhere(
          (node) => node.getSemanticsData().label.contains('Checked first'),
        );
        expect(
          merged.getSemanticsData().flagsCollection.isChecked,
          CheckedState.isTrue,
        );
        expect(
          merged.getSemanticsData().label,
          reverse
              ? 'Unchecked second\nChecked first'
              : 'Checked first\nUnchecked second',
        );
        tester.binding.renderViews.single.owner!.semanticsOwner!.performAction(
          merged.id,
          SemanticsAction.tap,
        );
        expect(calls.last, reverse ? 'second' : 'first');
      }
      handle.dispose();
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'MergeSemantics full Canvas exports one combined descendant node with separate outside nodes on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        await _pump(tester, _mergedModel(platform: platform));
        final exported = _exportedSemantics(tester);
        final first = exported.singleWhere(
          (node) => node.getSemanticsData().label.contains('First label'),
        );
        final second = exported.singleWhere(
          (node) => node.getSemanticsData().label.contains('Second label'),
        );
        expect(first, same(second));
        final label = first.getSemanticsData().label;
        expect(
          label.indexOf('First label'),
          lessThan(label.indexOf('Second label')),
        );
        expect(label.contains('\n'), isTrue);
        expect(label.contains(_childId), isFalse);
        expect(label.contains(_textId), isFalse);
        final designer = exported.singleWhere(
          (node) => node.getSemanticsData().label.contains(
            'MergeSemantics $_mergeId',
          ),
        );
        expect(designer, isNot(same(first)));
        expect(
          designer.getSemanticsData().hasAction(SemanticsAction.tap),
          isTrue,
          reason: 'the outer Designer affordance stays separate and accessible',
        );
        expect(
          exported.singleWhere(
            (node) => node.getSemanticsData().label.contains('Outside before'),
          ),
          isNot(same(first)),
        );
        expect(
          exported.singleWhere(
            (node) => node.getSemanticsData().label.contains('Outside after'),
          ),
          isNot(same(first)),
        );
        final render = tester.renderObject<RenderMergeSemantics>(
          _mergeFinder(),
        );
        final config = SemanticsConfiguration();
        render.describeSemanticsConfiguration(config);
        expect(config.isSemanticBoundary, isTrue);
        expect(config.isMergingSemanticsOfDescendants, isTrue);
        expect(tester.takeException(), isNull);
        handle.dispose();
      },
    );

    testWidgets(
      'MergeSemantics full Canvas nested merges retain renderer and follow live descendant order on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        RenderMergeSemantics? first;
        for (final nested in [false, true, false]) {
          for (final reverse in [false, true]) {
            await _pump(
              tester,
              _mergedModel(
                platform: platform,
                nested: nested,
                reverse: reverse,
              ),
            );
            final render = tester.renderObject<RenderMergeSemantics>(
              _mergeFinder(),
            );
            first ??= render;
            expect(render, same(first));
            final firstNode = _exportedSemantics(tester).singleWhere(
              (node) => node.getSemanticsData().label.contains('First label'),
            );
            final label = firstNode.getSemanticsData().label;
            expect(
              label.indexOf('First label') < label.indexOf('Second label'),
              !reverse,
            );
            expect(
              firstNode.getSemanticsData().hasAction(SemanticsAction.tap),
              isFalse,
              reason:
                  'plain application Text must not inherit synthetic Designer tap actions',
            );
            expect(tester.takeException(), isNull);
          }
        }
        handle.dispose();
      },
    );

    testWidgets(
      'MergeSemantics full Canvas TextField remains supported and contributes its genuine semantics on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        await _pump(
          tester,
          _model(
            platform: platform,
            bounds: const Size(250, 150),
            child: {
              'id': _childId,
              'type': 'flutter.widgets.Column',
              'properties': <String, Object?>{},
              'slots': {
                'children': {
                  'kind': 'list',
                  'children': [
                    _textNode(_textId, 'Field label'),
                    {
                      'id': _secondId,
                      'type': 'flutter.material.TextField',
                      'properties': <String, Object?>{},
                      'slots': <String, Object?>{},
                    },
                  ],
                },
              },
            },
          ),
        );
        expect(find.byType(TextField), findsOneWidget);
        final merged = _exportedSemantics(tester).singleWhere(
          (node) => node.getSemanticsData().label.contains('Field label'),
        );
        expect(merged.getSemanticsData().flagsCollection.isTextField, isTrue);
        expect(tester.takeException(), isNull);
        handle.dispose();
      },
    );

    testWidgets(
      'MergeSemantics preserves application paint layout intrinsics and pointer selection on $platform',
      (tester) async {
        final selected = <String>[];
        RenderMergeSemantics? first;
        for (final entry in [
          (argb: '0xFFABCDEF', text: 'Initial', width: 90, height: 60),
          (argb: '0x80123456', text: 'Updated', width: 120, height: 95),
        ]) {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  child: _content(
                    argb: entry.argb,
                    text: entry.text,
                    width: entry.width,
                    height: entry.height,
                  ),
                ),
              ),
              selectedWidgetId: _textId,
              onSelected: selected.add,
            ),
          );
          await tester.pump();
          final render = tester.renderObject<RenderMergeSemantics>(
            _mergeFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(
            render.size,
            Size(entry.width.toDouble(), entry.height.toDouble()),
          );
          expect(render.getMinIntrinsicWidth(80), entry.width);
          expect(render.getMaxIntrinsicWidth(80), entry.width);
          expect(render.getMinIntrinsicHeight(100), entry.height);
          expect(render.getMaxIntrinsicHeight(100), entry.height);
          expect(render.describeApproximatePaintClip(render.child!), isNull);
          final hit = BoxHitTestResult();
          expect(render.hitTest(hit, position: const Offset(30, 30)), isTrue);
          expect(hit.path, isNotEmpty);
          final paint = tester.renderObject(
            find
                .descendant(of: _widget(), matching: find.byType(ColoredBox))
                .first,
          );
          expect(
            paint,
            paints..rect(
              color: Color(int.parse(entry.argb.substring(2), radix: 16)),
            ),
          );
          await tester.tapAt(render.localToGlobal(const Offset(30, 30)));
          expect(selected.last, _textId);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'MergeSemantics empty child preserves zero or tight layout and external or body selection with DnD on $platform',
      (tester) async {
        for (final bounds in <Size?>[null, const Size(120, 100)]) {
          CanvasDropResolver? resolver;
          final selected = <String>[];
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(_model(platform: platform, bounds: bounds)),
              selectedWidgetId: null,
              onSelected: selected.add,
              onDropResolverChanged: (value) => resolver = value,
            ),
          );
          await tester.pump();
          final render = tester.renderObject<RenderMergeSemantics>(
            _mergeFinder(),
          );
          expect(render.child, isNull);
          expect(tester.widget<MergeSemantics>(_mergeFinder()).child, isNull);
          expect(render.size, bounds ?? Size.zero);
          expect(render, paintsNothing);
          expect(
            render.hitTest(BoxHitTestResult(), position: const Offset(10, 10)),
            isFalse,
          );
          final external = find.byKey(
            const ValueKey('canvas-zero-size-widget-target-$_mergeId'),
          );
          if (bounds == null) {
            expect(tester.getSize(external), const Size.square(36));
            expect(
              find.ancestor(of: external, matching: _mergeFinder()),
              findsNothing,
            );
          } else {
            expect(external, findsNothing);
          }
          final point = bounds == null
              ? tester.getCenter(external)
              : render.localToGlobal(render.size.center(Offset.zero));
          await tester.tapAt(point);
          expect(selected.last, _mergeId);
          final drop = _resolve(tester, resolver!, point);
          expect(drop?.parentWidgetId, _mergeId);
          expect(drop?.slotName, 'child');
          expect(drop?.insertionIndex, 0);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'MergeSemantics nested child stays a geometric palette and move destination on $platform',
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
        final render = tester.renderObject<RenderMergeSemantics>(
          _mergeFinder(),
        );
        final drop = _resolve(
          tester,
          resolver!,
          render.localToGlobal(const Offset(60, 50)),
        );
        expect(drop?.parentWidgetId, _childId);
        expect(drop?.slotName, 'children');
        final move = moveResolver!(_underId, _childId, 'children', 0);
        expect(move?.parentWidgetId, _childId);
        expect(move?.slotName, 'children');
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'MergeSemantics scope does not leak when the same descendant moves outside its ancestor on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        final selections = <String>[];
        for (final merged in [true, false, true]) {
          final model = _model(
            platform: platform,
            child: merged ? _content() : null,
          );
          if (!merged) {
            final root = model['root']! as Map<String, Object?>;
            final merge = _findJson(root)!;
            final center =
                ((root['slots']! as Map<String, Object?>)['body']!
                        as Map<String, Object?>)['child']!
                    as Map<String, Object?>;
            ((center['slots']! as Map<String, Object?>)['child']!
                as Map<String, Object?>)['child'] = {
              'id': _underId,
              'type': 'flutter.widgets.Column',
              'properties': <String, Object?>{},
              'slots': {
                'children': {
                  'kind': 'list',
                  'children': [merge, _content()],
                },
              },
            };
          }
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(model),
              selectedWidgetId: _textId,
              onSelected: selections.add,
            ),
          );
          await tester.pump();
          final child = find.byKey(const ValueKey('canvas-widget-$_textId'));
          final gesture = tester.widget<GestureDetector>(
            find
                .descendant(of: child, matching: find.byType(GestureDetector))
                .first,
          );
          expect(gesture.excludeFromSemantics, merged);
          final node = _exportedSemantics(tester).singleWhere(
            (node) => node.getSemanticsData().label.contains(_label),
          );
          expect(node.getSemanticsData().label.contains(_textId), !merged);
          expect(
            node.getSemanticsData().hasAction(SemanticsAction.tap),
            !merged,
          );
          if (!merged) {
            tester.binding.renderViews.single.owner!.semanticsOwner!
                .performAction(node.id, SemanticsAction.tap);
            await tester.pump();
            expect(selections.last, _textId);
          }
          expect(tester.takeException(), isNull);
        }
        handle.dispose();
      },
    );

    testWidgets(
      'MergeSemantics keeps unavailable image diagnostics accessible inside the clean application node on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        await _pump(
          tester,
          _model(
            platform: platform,
            child: {
              'id': _childId,
              'type': 'flutter.widgets.Image',
              'properties': {
                'image': {
                  'kind': 'imageProvider',
                  'value': {
                    'kind': 'asset',
                    'assetName': 'assets/missing.png',
                    'packageName': null,
                    'exactScale': null,
                    'resize': null,
                    'resolution': {
                      'kind': 'unavailable',
                      'code': 'missing',
                      'reason': 'Declared asset is absent',
                    },
                  },
                },
                'width': {'kind': 'integer', 'value': 120},
                'height': {'kind': 'integer', 'value': 80},
              },
              'slots': <String, Object?>{},
            },
          ),
        );
        final labels = _exportedSemantics(
          tester,
        ).map((node) => node.getSemanticsData().label).join('\n');
        expect(labels, contains('Image preview unavailable'));
        expect(labels, contains('Declared asset is absent'));
        expect(labels, contains('assets/missing.png'));
        expect(tester.takeException(), isNull);
        handle.dispose();
      },
    );

    testWidgets(
      'MergeSemantics child replacement clearing and tree-selected descendant F2 editing stay available on $platform',
      (tester) async {
        RenderMergeSemantics? first;
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
          final render = tester.renderObject<RenderMergeSemantics>(
            _mergeFinder(),
          );
          first ??= render;
          expect(render, same(first));
          if (child == null) {
            expect(render.child, isNull);
            expect(render.size, Size.zero);
          }
          expect(tester.takeException(), isNull);
        }
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
        await tester.enterText(find.byType(TextField), 'Updated merged child');
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_textId, 'Updated merged child')]);
        expect(find.byType(TextField), findsNothing);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'MergeSemantics actual button application actions are not replaced by Designer wrapper selection on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        final selected = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(_mergedModel(platform: platform, buttons: true)),
            selectedWidgetId: null,
            onSelected: selected.add,
          ),
        );
        await tester.pump();
        final node = _exportedSemantics(tester).singleWhere(
          (node) => node.getSemanticsData().label.contains('First label'),
        );
        expect(node.getSemanticsData().hasAction(SemanticsAction.tap), isTrue);
        tester.binding.renderViews.single.owner!.semanticsOwner!.performAction(
          node.id,
          SemanticsAction.tap,
        );
        await tester.pump();
        expect(
          selected,
          isEmpty,
          reason:
              'application button callback is a safe no-op, not Designer selection',
        );
        expect(tester.takeException(), isNull);
        handle.dispose();
      },
    );
  }
}

List<SemanticsNode> _exportedSemantics(WidgetTester tester) {
  final result = <SemanticsNode>[];
  void visit(SemanticsNode node) {
    if (!node.isMergedIntoParent) result.add(node);
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

Map<String, Object?> _mergedModel({
  String platform = 'windows',
  bool buttons = false,
  bool reverse = false,
  bool nested = false,
}) {
  var children = <Map<String, Object?>>[
    _textNode(_textId, 'First label'),
    _textNode(_secondId, 'Second label'),
  ];
  if (reverse) children = children.reversed.toList();
  if (buttons) {
    children = [
      _node(
        '7d7c351c-c7e4-47c2-b3cc-6bcc388d107b',
        'flutter.material.ElevatedButton',
        child: children[0],
      ),
      _node(
        '8dd305f7-4628-4352-bef0-a5ec8f904dd0',
        'flutter.material.ElevatedButton',
        child: children[1],
      ),
    ];
  }
  Map<String, Object?> content = {
    'id': _childId,
    'type': 'flutter.widgets.Column',
    'properties': <String, Object?>{},
    'slots': {
      'children': {'kind': 'list', 'children': children},
    },
  };
  if (nested) {
    content = _node(
      '4e0db714-e66e-4e3a-849c-0d7c9ca38b9d',
      _type,
      child: content,
    );
  }
  final model = _model(platform: platform, child: content);
  final root = model['root']! as Map<String, Object?>;
  final merge = _findJson(root)!;
  final center =
      ((root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child']!
          as Map<String, Object?>;
  ((center['slots']! as Map<String, Object?>)['child']!
      as Map<String, Object?>)['child'] = {
    'id': 'd8363313-9ec0-4f7d-bcab-a05157debf62',
    'type': 'flutter.widgets.Column',
    'properties': <String, Object?>{},
    'slots': {
      'children': {
        'kind': 'list',
        'children': [
          _textNode('824b3b73-d579-4a3d-9184-713cc8d50a3a', 'Outside before'),
          merge,
          _textNode('3592f51f-278c-4b6d-960a-ea9d88ef09c3', 'Outside after'),
        ],
      },
    },
  };
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

Finder _widget() => find.byKey(const ValueKey('canvas-widget-$_mergeId'));
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

Finder _mergeFinder() =>
    find.descendant(of: _widget(), matching: find.byType(MergeSemantics)).first;
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode? _find(CanvasNode node) {
  if (node.id == _mergeId) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _find(child);
      if (found != null) return found;
    }
  }
  return null;
}

Map<String, Object?>? _findJson(Map<String, Object?> node) {
  if (node['id'] == _mergeId) return node;
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
  var content = _node(_mergeId, _type, child: child, properties: properties);
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
        ? _node(_mergeId, _type, child: sized, properties: properties)
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
