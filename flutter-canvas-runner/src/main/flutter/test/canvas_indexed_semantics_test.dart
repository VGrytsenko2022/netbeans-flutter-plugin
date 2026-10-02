import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _indexedId = '69d6fd9a-73e8-4cd7-941e-9bb2dfdf7d54';
const _childId = 'ba43ea77-265e-4900-8cb6-98eb91b4523d';
const _textId = 'a7fd6c4e-c071-4f4f-9eaa-ed31229d6d37';
const _secondId = 'f9137943-c599-428c-a974-4ac1a9f9e28d';
const _underId = 'f9eb92b0-e57e-49cc-baf9-7e1a6868d66f';
const _centerId = '1d6c16df-df2b-41cf-b46d-2e1d85538277';
const _type = 'flutter.widgets.IndexedSemantics';
const _label = 'Application child semantics';

void main() {
  test(
    'IndexedSemantics exact required integer contract preserves every signed portable endpoint',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf('W|flutter.widgets.IndexedStack\n', start);
      expect(
        contract.substring(start, end),
        'W|$_type\nP|index|integer|1|integer:0|integer:-9007199254740991:1:9007199254740991:1|integer:range:-9007199254740991:1:9007199254740991:1\nS|child|single|0|0|1|any\n',
      );
      expect(canvasModelProtocolVersion, 20);
      expect(isCanvasReviewedWidgetType(_type), isTrue);
      expect(isCanvasPaletteWrapperWidgetType(_type), isFalse);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      for (final value in [-9007199254740991, -1, 0, 1, 9007199254740991]) {
        expect(
          _find(
            _decode(_model(properties: _index(value))).root,
          )!.properties['index']!.value,
          value,
        );
      }
    },
  );
  test(
    'IndexedSemantics decoder rejects absent null float unsafe integers invented flags and invalid children',
    () {
      expect(
        () => _decode(_model(properties: const {})),
        throwsFormatException,
      );
      for (final value in [
        {'kind': 'null'},
        {'kind': 'integer', 'value': 1.0},
        {'kind': 'integer', 'value': '1'},
        {'kind': 'integer', 'value': true},
        {'kind': 'integer', 'value': 9007199254740992},
        {'kind': 'integer', 'value': -9007199254740992},
        {'kind': 'integer', 'value': 0, 'extra': true},
        {'kind': 'double', 'value': 1.0},
        {'kind': 'string', 'value': 'runProjectCode()'},
      ]) {
        expect(
          () => _decode(_model(properties: {'index': value})),
          throwsFormatException,
        );
      }
      for (final name in [
        'key',
        'child',
        'blocking',
        'container',
        'source',
        'semanticChildCount',
      ]) {
        expect(
          () => _decode(
            _model(
              properties: {
                ..._index(0),
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
    'IndexedSemantics ordinary source and optional child DnD stay closed',
    () {
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
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
    'SDK first-child index reference for two independent semantic buttons',
    (tester) async {
      final handle = tester.ensureSemantics();
      await tester.pumpWidget(
        MaterialApp(
          home: IndexedSemantics(
            index: 7,
            child: Column(
              children: [
                ElevatedButton(
                  onPressed: () {},
                  child: const Text('First button'),
                ),
                ElevatedButton(
                  onPressed: () {},
                  child: const Text('Second button'),
                ),
              ],
            ),
          ),
        ),
      );
      final nodes = _exportedSemantics(tester);
      final first = nodes.singleWhere(
        (node) => node.getSemanticsData().label.contains('First button'),
      );
      final second = nodes.singleWhere(
        (node) => node.getSemanticsData().label.contains('Second button'),
      );
      expect(first.indexInParent, isNull);
      expect(second.indexInParent, isNull);
      expect(
        _indexedAncestor(first)?.indexInParent,
        7,
        reason: _diagnostics(nodes),
      );
      expect(
        _indexedAncestor(first),
        same(_indexedAncestor(second)),
        reason:
            'the SDK first child semantic node is an anonymous shared parent, not necessarily a leaf',
      );
      handle.dispose();
    },
  );
  for (final platform in ['windows', 'web']) {
    testWidgets(
      'IndexedSemantics suppresses only scope-owned synthetic semantics and preserves outside Designer ancestors on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        final selected = <String>[];
        for (final index in [0, 4, -1]) {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  properties: _index(index),
                  child: _textNode(_textId, _label),
                ),
              ),
              selectedWidgetId: _textId,
              onSelected: selected.add,
            ),
          );
          await tester.pump();
          final node = _exportedSemantics(
            tester,
          ).singleWhere((n) => n.getSemanticsData().label.contains(_label));
          expect(node.indexInParent, index);
          expect(node.getSemanticsData().label, 'Center $_centerId\n$_label');
          for (final id in [_indexedId, _textId]) {
            expect(_designerGesture(tester, id).excludeFromSemantics, isTrue);
          }
          // The unrelated Center ancestor is intentionally still accessible.
          // IndexedSemantics does not introduce a boundary, so its annotation
          // may naturally share that ancestor's node just as the SDK permits.
          expect(
            _designerGesture(tester, _centerId).excludeFromSemantics,
            isFalse,
          );
          tester.binding.renderViews.single.owner!.semanticsOwner!
              .performAction(node.id, SemanticsAction.tap);
          await tester.pump();
          expect(selected.last, _centerId);
          expect(selected, isNot(contains(_indexedId)));
          expect(selected, isNot(contains(_textId)));
          final config = SemanticsConfiguration();
          tester
              .renderObject<RenderIndexedSemantics>(_indexedFinder())
              .describeSemanticsConfiguration(config);
          expect(config.indexInParent, index);
          expect(tester.takeException(), isNull);
        }
        handle.dispose();
      },
    );
    testWidgets(
      'IndexedSemantics nested indices merge exclusion and blocking match the exact SDK topology on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        for (final kind in [
          'nested',
          'mergeChild',
          'mergeParent',
          'excludeTrue',
          'excludeFalse',
          'block',
        ]) {
          await tester.pumpWidget(MaterialApp(home: _sdkInteraction(kind)));
          await tester.pump();
          final sdkNodes = _exportedSemantics(tester);
          final sdkTarget = sdkNodes
              .where((node) => node.getSemanticsData().label.contains('Target'))
              .toList();
          final sdkIndex = sdkTarget.isEmpty
              ? null
              : _indexedAncestor(sdkTarget.single)?.indexInParent;
          final sdkBefore = sdkNodes.any(
            (node) => node.getSemanticsData().label.contains('Before'),
          );
          await _pump(tester, _interactionModel(platform, kind));
          final nodes = _exportedSemantics(tester);
          final targets = nodes
              .where((node) => node.getSemanticsData().label.contains('Target'))
              .toList();
          expect(
            targets.length,
            sdkTarget.length,
            reason: '$kind ${_diagnostics(nodes)}',
          );
          if (targets.isNotEmpty) {
            expect(
              _indexedAncestor(targets.single)?.indexInParent,
              sdkIndex,
              reason: '$kind ${_diagnostics(nodes)}',
            );
          }
          expect(
            nodes.any(
              (node) => node.getSemanticsData().label.contains('Before'),
            ),
            sdkBefore,
            reason: kind,
          );
          expect(tester.takeException(), isNull);
        }
        handle.dispose();
      },
    );
    testWidgets(
      'IndexedSemantics retains real renderer layout painting intrinsics and pointer editing during index updates on $platform',
      (tester) async {
        RenderIndexedSemantics? first;
        final selected = <String>[];
        for (final entry in [
          (index: -1, argb: '0xFFABCDEF', width: 90, height: 60),
          (index: 8, argb: '0x80123456', width: 120, height: 95),
        ]) {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  properties: _index(entry.index),
                  child: _content(
                    argb: entry.argb,
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
          final render = tester.renderObject<RenderIndexedSemantics>(
            _indexedFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.index, entry.index);
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
          expect(
            render.hitTest(
              BoxHitTestResult(),
              position: Offset(entry.width + 1, 30),
            ),
            isFalse,
          );
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
      'IndexedSemantics null child keeps zero and tight layout with external or body selection and DnD on $platform',
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
          final render = tester.renderObject<RenderIndexedSemantics>(
            _indexedFinder(),
          );
          expect(render.index, 0);
          expect(render.child, isNull);
          expect(
            tester.widget<IndexedSemantics>(_indexedFinder()).child,
            isNull,
          );
          expect(render.size, bounds ?? Size.zero);
          expect(render, paintsNothing);
          final external = find.byKey(
            const ValueKey('canvas-zero-size-widget-target-$_indexedId'),
          );
          if (bounds == null) {
            expect(tester.getSize(external), const Size.square(36));
            expect(
              find.ancestor(of: external, matching: _indexedFinder()),
              findsNothing,
            );
          } else {
            expect(external, findsNothing);
          }
          final point = bounds == null
              ? tester.getCenter(external)
              : render.localToGlobal(render.size.center(Offset.zero));
          await tester.tapAt(point);
          expect(selected.last, _indexedId);
          final drop = _resolve(tester, resolver!, point);
          expect(drop?.parentWidgetId, _indexedId);
          expect(drop?.slotName, 'child');
          expect(drop?.insertionIndex, 0);
          expect(tester.takeException(), isNull);
        }
      },
    );
    testWidgets(
      'IndexedSemantics nested child remains an exact geometric palette and move destination on $platform',
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
                child: _listNode(_childId, 'flutter.widgets.Column', []),
              ),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
            onMovePreviewResolverChanged: (value) => moveResolver = value,
          ),
        );
        await tester.pump();
        final render = tester.renderObject<RenderIndexedSemantics>(
          _indexedFinder(),
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
      'IndexedSemantics actual reparent out and back restores Designer annotations without leaking index on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        for (final indexed in [true, false, true]) {
          final model = _model(
            platform: platform,
            properties: _index(6),
            child: indexed ? _textNode(_textId, _label) : null,
          );
          if (!indexed) {
            final root = model['root']! as Map<String, Object?>;
            final wrapper = _findJson(root)!;
            _replaceCentered(
              model,
              _listNode(_underId, 'flutter.widgets.Column', [
                wrapper,
                _textNode(_textId, _label),
              ]),
            );
          }
          await _pump(tester, model);
          final node = _exportedSemantics(
            tester,
          ).singleWhere((n) => n.getSemanticsData().label.contains(_label));
          expect(node.indexInParent, indexed ? 6 : null);
          expect(node.getSemanticsData().label.contains(_textId), !indexed);
          final gesture = _designerGesture(tester, _textId);
          expect(gesture.excludeFromSemantics, indexed);
          expect(gesture.onTap, isNotNull);
          expect(
            _designerGesture(tester, _indexedId).excludeFromSemantics,
            isTrue,
          );
          expect(
            _designerGesture(tester, _centerId).excludeFromSemantics,
            isFalse,
          );
          expect(tester.takeException(), isNull);
        }
        handle.dispose();
      },
    );
    testWidgets(
      'IndexedSemantics retains unavailable Image diagnostics without synthetic scope identities on $platform',
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
        expect(labels, isNot(contains(_indexedId)));
        expect(labels, isNot(contains(_childId)));
        expect(tester.takeException(), isNull);
        handle.dispose();
      },
    );
    testWidgets(
      'IndexedSemantics child changes preserve renderer and descendant F2 keyboard editing on $platform',
      (tester) async {
        RenderIndexedSemantics? first;
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
          final render = tester.renderObject<RenderIndexedSemantics>(
            _indexedFinder(),
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
        await tester.enterText(find.byType(TextField), 'Updated indexed child');
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_textId, 'Updated indexed child')]);
        expect(find.byType(TextField), findsNothing);
        expect(tester.takeException(), isNull);
      },
    );
    testWidgets(
      'IndexedSemantics full Canvas annotates the actual first child node and follows live indices on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        RenderIndexedSemantics? first;
        final selected = <String>[];
        for (final index in [-9007199254740991, -1, 0, 7, 9007199254740991]) {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  properties: _index(index),
                  child: _twoButtons(),
                ),
              ),
              selectedWidgetId: null,
              onSelected: selected.add,
            ),
          );
          await tester.pump();
          final render = tester.renderObject<RenderIndexedSemantics>(
            _indexedFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.index, index);
          final nodes = _exportedSemantics(tester);
          final appFirst = nodes.singleWhere(
            (node) => node.getSemanticsData().label.contains('First button'),
          );
          final appSecond = nodes.singleWhere(
            (node) => node.getSemanticsData().label.contains('Second button'),
          );
          expect(appFirst.indexInParent, isNull);
          expect(appSecond.indexInParent, isNull);
          expect(
            _indexedAncestor(appFirst)?.indexInParent,
            index,
            reason: _diagnostics(nodes),
          );
          expect(_indexedAncestor(appFirst), same(_indexedAncestor(appSecond)));
          for (final button in [appFirst, appSecond]) {
            expect(
              button.getSemanticsData().hasAction(SemanticsAction.tap),
              isTrue,
            );
            tester.binding.renderViews.single.owner!.semanticsOwner!
                .performAction(button.id, SemanticsAction.tap);
          }
          await tester.pump();
          expect(
            selected,
            isEmpty,
            reason:
                'Actual safe-preview button actions cannot become Designer selection',
          );
          expect(tester.takeException(), isNull);
        }
        handle.dispose();
      },
    );
    testWidgets(
      'IndexedSemantics manual ListView indices reach actual rows with explicit count on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        await _pump(tester, _listModel(platform));
        final list = tester.widget<ListView>(find.byType(ListView));
        expect(
          (list.childrenDelegate as SliverChildListDelegate).addSemanticIndexes,
          isFalse,
        );
        expect(list.semanticChildCount, 2);
        final nodes = _exportedSemantics(tester);
        final first = nodes.singleWhere(
          (node) => node.getSemanticsData().label.contains('Row zero'),
        );
        final second = nodes.singleWhere(
          (node) => node.getSemanticsData().label.contains('Row one'),
        );
        expect(first.indexInParent, 0, reason: _diagnostics(nodes));
        expect(second.indexInParent, 1);
        expect(
          nodes.where((node) => node.getSemanticsData().scrollChildCount == 2),
          isNotEmpty,
        );
        expect(tester.takeException(), isNull);
        handle.dispose();
      },
    );
    testWidgets(
      'IndexedSemantics manual ListView scrollIndex skips an unindexed separator on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        await _pump(tester, _listModel(platform, tallRows: true));
        final scroll = tester.state<ScrollableState>(
          find
              .descendant(
                of: find.byType(ListView),
                matching: find.byType(Scrollable),
              )
              .first,
        );
        expect(scroll.position.maxScrollExtent, greaterThan(350));
        final before = _exportedSemantics(
          tester,
        ).singleWhere((node) => node.getSemanticsData().scrollChildCount == 2);
        expect(before.getSemanticsData().scrollIndex, 0);
        scroll.position.jumpTo(350);
        await tester.pump();
        final after = _exportedSemantics(
          tester,
        ).singleWhere((node) => node.getSemanticsData().scrollChildCount == 2);
        expect(after.getSemanticsData().scrollIndex, 1);
        expect(after.getSemanticsData().scrollPosition, 350);
        expect(tester.takeException(), isNull);
        handle.dispose();
      },
    );
  }
}

String _diagnostics(List<SemanticsNode> nodes) => nodes
    .map((n) => 'index=${n.indexInParent} label=${n.getSemanticsData().label}')
    .join('\n');
SemanticsNode? _indexedAncestor(SemanticsNode node) {
  SemanticsNode? current = node;
  while (current != null) {
    if (current.indexInParent != null) return current;
    current = current.parent;
  }
  return null;
}

Map<String, Object?> _index(int index) => {
  'index': {'kind': 'integer', 'value': index},
};
Widget _sdkInteraction(String kind) {
  Widget child = const Text('Target');
  if (kind == 'nested') child = IndexedSemantics(index: 9, child: child);
  if (kind == 'mergeChild') child = MergeSemantics(child: child);
  if (kind == 'excludeTrue' || kind == 'excludeFalse') {
    child = ExcludeSemantics(excluding: kind == 'excludeTrue', child: child);
  }
  if (kind == 'block') child = BlockSemantics(child: child);
  Widget result = IndexedSemantics(index: 4, child: child);
  if (kind == 'mergeParent') result = MergeSemantics(child: result);
  if (kind == 'block') {
    result = Column(
      children: [const Text('Before'), result, const Text('After')],
    );
  }
  return result;
}

Map<String, Object?> _interactionModel(String platform, String kind) {
  Map<String, Object?> child = _textNode(_textId, 'Target');
  const wrapperId = '94bd3aee-2c73-42ae-bd19-8d4d10ae555e';
  if (kind == 'nested') {
    child = _node(wrapperId, _type, properties: _index(9), child: child);
  }
  if (kind == 'mergeChild') {
    child = _node(wrapperId, 'flutter.widgets.MergeSemantics', child: child);
  }
  if (kind == 'excludeTrue' || kind == 'excludeFalse') {
    child = _node(
      wrapperId,
      'flutter.widgets.ExcludeSemantics',
      properties: {
        'excluding': {'kind': 'boolean', 'value': kind == 'excludeTrue'},
      },
      child: child,
    );
  }
  if (kind == 'block') {
    child = _node(wrapperId, 'flutter.widgets.BlockSemantics', child: child);
  }
  final model = _model(platform: platform, properties: _index(4), child: child);
  final indexed = _findJson(model['root']! as Map<String, Object?>)!;
  if (kind == 'mergeParent') {
    _replaceCentered(
      model,
      _node(wrapperId, 'flutter.widgets.MergeSemantics', child: indexed),
    );
  }
  if (kind == 'block') {
    _replaceCentered(
      model,
      _listNode(_childId, 'flutter.widgets.Column', [
        _textNode(_underId, 'Before'),
        indexed,
        _textNode(_secondId, 'After'),
      ]),
    );
  }
  return model;
}

void _replaceCentered(
  Map<String, Object?> model,
  Map<String, Object?> content,
) {
  final root = model['root']! as Map<String, Object?>;
  final center =
      ((root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child']!
          as Map<String, Object?>;
  ((center['slots']! as Map<String, Object?>)['child']!
          as Map<String, Object?>)['child'] =
      content;
}

Map<String, Object?> _twoButtons() =>
    _listNode(_childId, 'flutter.widgets.Column', [
      _node(
        '84c13e0c-de31-4977-832a-d40ba6bd7cbe',
        'flutter.material.ElevatedButton',
        child: _textNode(_textId, 'First button'),
      ),
      _node(
        'b0d5476c-972a-4dc9-bda9-59e26c14c0d9',
        'flutter.material.ElevatedButton',
        child: _textNode(_secondId, 'Second button'),
      ),
    ]);
Map<String, Object?> _listNode(
  String id,
  String type,
  List<Object?> children, {
  Map<String, Object?> properties = const {},
}) => {
  'id': id,
  'type': type,
  'properties': properties,
  'slots': {
    'children': {'kind': 'list', 'children': children},
  },
};
Map<String, Object?> _textNode(String id, String text) => {
  'id': id,
  'type': 'flutter.widgets.Text',
  'properties': {
    'data': {'kind': 'string', 'value': text},
    'semanticsLabel': {'kind': 'string', 'value': text},
  },
  'slots': <String, Object?>{},
};
Map<String, Object?> _listModel(String platform, {bool tallRows = false}) {
  final model = _model(platform: platform);
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = _listNode(
    _childId,
    'flutter.widgets.ListView',
    [
      _node(
        _indexedId,
        _type,
        properties: _index(0),
        child: tallRows
            ? _node(
                'a42a1a7b-69ed-408b-a37c-9c6a38951f4a',
                'flutter.widgets.SizedBox',
                properties: {
                  'height': {'kind': 'integer', 'value': 300},
                },
                child: _textNode(_textId, 'Row zero'),
              )
            : _textNode(_textId, 'Row zero'),
      ),
      _node(
        '703676d8-5fce-421b-9d3c-bec2ef7b9464',
        'flutter.widgets.SizedBox',
        properties: {
          'height': {'kind': 'integer', 'value': 24},
        },
      ),
      _node(
        'ab43534e-25f1-4145-aaf5-ef61a003ff15',
        _type,
        properties: _index(1),
        child: tallRows
            ? _node(
                '91b7c386-d125-42b4-950d-8c1522826299',
                'flutter.widgets.SizedBox',
                properties: {
                  'height': {'kind': 'integer', 'value': 300},
                },
                child: _textNode(_secondId, 'Row one'),
              )
            : _textNode(_secondId, 'Row one'),
      ),
      if (tallRows)
        _node(
          'a44210a9-4341-49a1-bbbf-2c7b5d102ec3',
          'flutter.widgets.SizedBox',
          properties: {
            'height': {'kind': 'integer', 'value': 900},
          },
        ),
    ],
    properties: {
      'addSemanticIndexes': {'kind': 'boolean', 'value': false},
      'semanticChildCount': {'kind': 'integer', 'value': 2},
    },
  );
  return model;
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

Finder _widget() => find.byKey(const ValueKey('canvas-widget-$_indexedId'));
GestureDetector _designerGesture(WidgetTester tester, String id) =>
    tester.widget<GestureDetector>(
      find
          .descendant(
            of: find.byKey(ValueKey('canvas-widget-$id')),
            matching: find.byType(GestureDetector),
          )
          .first,
    );
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

Finder _indexedFinder() => find
    .descendant(of: _widget(), matching: find.byType(IndexedSemantics))
    .first;
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode? _find(CanvasNode node) {
  if (node.id == _indexedId) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _find(child);
      if (found != null) return found;
    }
  }
  return null;
}

Map<String, Object?>? _findJson(Map<String, Object?> node) {
  if (node['id'] == _indexedId) return node;
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
  Map<String, Object?> properties = const {
    'index': {'kind': 'integer', 'value': 0},
  },
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
  var content = _node(_indexedId, _type, child: child, properties: properties);
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
        ? _node(_indexedId, _type, child: sized, properties: properties)
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
