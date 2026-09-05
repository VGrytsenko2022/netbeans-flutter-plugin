import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _boundaryId = 'fc72be46-49b7-4e95-913c-2a2c6b398ded';
const _outerId = '041c1bd4-48b5-48d7-b04b-fb1ef7d6c458';
const _childId = 'e0c332dd-b4a7-449b-8d90-60ac2525e94b';
const _leafId = '56b2b745-a1ab-4efe-9594-e5a8d1f355cc';
const _type = 'flutter.widgets.RepaintBoundary';

void main() {
  test(
    'RepaintBoundary exact contract has no properties and one optional ordinary child',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf('W|flutter.widgets.RotatedBox\n', start);
      expect(
        contract.substring(start, end),
        'W|$_type\nS|child|single|0|0|1|any\n',
      );
      expect(canvasModelProtocolVersion, 18);
      final boundary = _find(_decode(_model()).root)!;
      expect(boundary.properties, isEmpty);
      expect(boundary.slot('child')?.child, isNull);
      expect(isCanvasReviewedWidgetType(_type), isTrue);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(isCanvasPaletteWrapperWidgetType(_type), isFalse);
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
    },
  );

  test(
    'RepaintBoundary decoder rejects invented scalar, key, source and slot contracts',
    () {
      for (final property in [
        'key',
        'child',
        'enabled',
        'repaint',
        'color',
        'source',
        'toImage',
      ]) {
        final model = _model();
        _findJson(model['root']! as Map<String, Object?>)!['properties'] = {
          property: {'kind': 'boolean', 'value': true},
        };
        expect(() => _decode(model), throwsFormatException, reason: property);
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
      final missingSlot = _model();
      _findJson(missingSlot['root']! as Map<String, Object?>)!['slots'] =
          <String, Object?>{};
      expect(
        () => _decode(missingSlot),
        returnsNormally,
        reason: 'the nullable child may be absent, not only explicit null',
      );
      for (final childType in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () => _decode(
            _model(child: _node(_childId, 'flutter.widgets.$childType')),
          ),
          throwsFormatException,
          reason: 'ParentData cannot be reparented under RepaintBoundary',
        );
      }
    },
  );

  test(
    'RepaintBoundary DnD is ordinary insertion and nested child capacity remains exact',
    () {
      final boundarySource = CanvasPaletteDragSource(
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
            source: boundarySource,
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
          source: boundarySource,
        ),
        isFalse,
      );
      for (final sourceType in [
        _type,
        'flutter.widgets.Text',
        'flutter.widgets.PhysicalShape',
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
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: _type,
            slotName: 'child',
            currentChildCount: 0,
            insertionIndex: 1,
            source: source,
          ),
          isFalse,
        );
      }
      expect(
        () =>
            _decode(_model(child: _node(_childId, _type, child: _sizedLeaf()))),
        returnsNormally,
      );
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'RepaintBoundary uses actual RenderRepaintBoundary and external Designer overlays on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        await _pump(
          tester,
          _model(platform: platform, child: _paintedChild()),
          overlays: true,
        );
        final widget = tester.widget<RepaintBoundary>(_boundaryFinder());
        final render = tester.renderObject<RenderRepaintBoundary>(
          _boundaryFinder(),
        );
        expect(widget.child, isNotNull);
        expect(render.isRepaintBoundary, isTrue);
        expect(render.needsCompositing, isTrue);
        expect(render.debugLayer, isA<OffsetLayer>());
        expect(render.debugLayer!.attached, isTrue);
        expect(render.size, const Size(80, 60));
        expect(
          find.bySemanticsLabel(RegExp('ColoredBox $_childId')),
          findsOneWidget,
        );
        expect(
          find.bySemanticsLabel(RegExp('SizedBox $_leafId')),
          findsOneWidget,
        );
        expect(
          render.hitTest(BoxHitTestResult(), position: const Offset(40, 30)),
          isTrue,
        );
        expect(
          render.hitTest(BoxHitTestResult(), position: const Offset(81, 30)),
          isFalse,
        );
        expect(render.describeApproximatePaintClip(render.child!), isNull);
        for (final overlay in [
          find.byKey(const ValueKey('canvas-selection-outline-$_boundaryId')),
          find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
        ]) {
          expect(overlay, findsOneWidget);
          expect(
            find.ancestor(of: overlay, matching: _boundaryFinder()),
            findsNothing,
          );
        }
        expect(tester.takeException(), isNull);
        semantics.dispose();
      },
    );

    testWidgets(
      'RepaintBoundary preserves constraints, intrinsic geometry and descendant layout on $platform',
      (tester) async {
        for (final bounds in [null, const Size(120, 100), const Size(20, 10)]) {
          await _pump(
            tester,
            _model(platform: platform, bounds: bounds, child: _paintedChild()),
          );
          final render = tester.renderObject<RenderRepaintBoundary>(
            _boundaryFinder(),
          );
          expect(render.size, bounds ?? const Size(80, 60));
          expect(render.child!.size, render.size);
          expect(render.getMinIntrinsicWidth(100), 80);
          expect(render.getMaxIntrinsicHeight(100), 60);
          if (bounds != null) {
            expect(render.constraints, BoxConstraints.tight(bounds));
          }
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'RepaintBoundary isolates descendant and ancestor paint invalidations with retained layers on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(
            platform: platform,
            child: _paintedChild(),
            outerBoundary: true,
          ),
        );
        final inner = tester.renderObject<RenderRepaintBoundary>(
          _boundaryFinder(),
        );
        final outer = tester.renderObject<RenderRepaintBoundary>(
          _boundaryFinder(_outerId),
        );
        final innerLayer = inner.debugLayer;
        final outerLayer = outer.debugLayer;
        expect(innerLayer, isNot(same(outerLayer)));
        inner.debugResetMetrics();
        outer.debugResetMetrics();
        expect(inner.debugNeedsPaint, isFalse);
        expect(outer.debugNeedsPaint, isFalse);
        inner.child!.markNeedsPaint();
        expect(inner.debugNeedsPaint, isTrue);
        expect(
          outer.debugNeedsPaint,
          isFalse,
          reason: 'descendant repaint stops at the actual inner SDK boundary',
        );
        await tester.pump();
        expect(inner.debugLayer, same(innerLayer));
        expect(outer.debugLayer, same(outerLayer));
        expect(inner.debugAsymmetricPaintCount, greaterThan(0));
        expect(inner.debugSymmetricPaintCount, 0);
        expect(
          outer.debugSymmetricPaintCount + outer.debugAsymmetricPaintCount,
          0,
        );

        inner.debugResetMetrics();
        outer.debugResetMetrics();
        outer.markNeedsPaint();
        expect(
          inner.debugNeedsPaint,
          isFalse,
          reason: 'ancestor repaint does not invalidate the inner display list',
        );
        await tester.pump();
        expect(inner.debugLayer, same(innerLayer));
        expect(outer.debugLayer, same(outerLayer));
        expect(inner.debugAsymmetricPaintCount, greaterThan(0));
        expect(inner.debugSymmetricPaintCount, 0);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'RepaintBoundary retains layer while accepted descendant color changes remain visible on $platform',
      (tester) async {
        RenderRepaintBoundary? first;
        ContainerLayer? layer;
        for (final argb in ['0xFF112233', '0x80123456', '0xFF445566']) {
          await _pump(
            tester,
            _model(
              platform: platform,
              child: _paintedChild(argb: argb),
            ),
          );
          final boundary = tester.renderObject<RenderRepaintBoundary>(
            _boundaryFinder(),
          );
          first ??= boundary;
          layer ??= boundary.debugLayer;
          expect(boundary, same(first));
          expect(boundary.debugLayer, same(layer));
          final colored = tester.widget<ColoredBox>(
            find
                .descendant(of: _widget(), matching: find.byType(ColoredBox))
                .first,
          );
          expect(colored.color, Color(int.parse(argb.substring(2), radix: 16)));
          final paint = tester.renderObject(find.byWidget(colored));
          expect(paint, paints..rect(color: colored.color));
          expect(boundary.debugNeedsPaint, isFalse);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'RepaintBoundary accepts descendant replacement and removal without inventing a child on $platform',
      (tester) async {
        RenderRepaintBoundary? first;
        for (final child in [
          _paintedChild(),
          _node(
            _childId,
            'flutter.widgets.SizedBox',
            properties: {
              'width': {'kind': 'integer', 'value': 25},
              'height': {'kind': 'integer', 'value': 15},
            },
          ),
          null,
        ]) {
          await _pump(tester, _model(platform: platform, child: child));
          final boundary = tester.renderObject<RenderRepaintBoundary>(
            _boundaryFinder(),
          );
          first ??= boundary;
          expect(boundary, same(first));
          expect(boundary.isRepaintBoundary, isTrue);
          if (child == null) {
            expect(
              tester.widget<RepaintBoundary>(_boundaryFinder()).child,
              isNull,
            );
            expect(boundary.child, isNull);
            expect(boundary.size, Size.zero);
          }
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'RepaintBoundary nullable child remains absent under nonzero constraints on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(platform: platform, bounds: const Size(120, 100)),
        );
        final boundary = tester.renderObject<RenderRepaintBoundary>(
          _boundaryFinder(),
        );
        expect(tester.widget<RepaintBoundary>(_boundaryFinder()).child, isNull);
        expect(boundary.child, isNull);
        expect(boundary.size, const Size(120, 100));
        expect(boundary, paintsNothing);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'RepaintBoundary truly zero-size node has external selection and child DnD on $platform',
      (tester) async {
        CanvasDropResolver? resolver;
        final selections = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(_model(platform: platform)),
            selectedWidgetId: null,
            onSelected: selections.add,
            onDropResolverChanged: (value) => resolver = value,
          ),
        );
        await tester.pump();
        final boundary = tester.renderObject<RenderRepaintBoundary>(
          _boundaryFinder(),
        );
        expect(boundary.child, isNull);
        expect(boundary.size, Size.zero);
        expect(tester.getSize(_widget()), Size.zero);
        final target = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$_boundaryId'),
        );
        expect(tester.getSize(target), const Size.square(36));
        expect(
          find.ancestor(of: target, matching: _boundaryFinder()),
          findsNothing,
        );
        await tester.tap(target);
        expect(selections.last, _boundaryId);
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final point = tester.getCenter(target);
        final drop = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
        expect(drop?.parentWidgetId, _boundaryId);
        expect(drop?.slotName, 'child');
        expect(drop?.insertionIndex, 0);
        expect(drop?.zone?.isEmpty, isFalse);
        expect(tester.takeException(), isNull);
      },
    );
  }
}

Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  bool overlays = false,
}) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: _decode(model),
      selectedWidgetId: overlays ? _boundaryId : null,
      onSelected: (_) {},
      dropHoverTarget: overlays
          ? const CanvasDropTarget(
              parentWidgetId: _boundaryId,
              slotName: 'child',
              insertionIndex: 0,
              zone: CanvasDropZone(
                leftMicros: 400000,
                topMicros: 400000,
                rightMicros: 600000,
                bottomMicros: 600000,
              ),
            )
          : null,
    ),
  );
  await tester.pump();
}

Finder _widget([String id = _boundaryId]) =>
    find.byKey(ValueKey('canvas-widget-$id'));
Finder _boundaryFinder([String id = _boundaryId]) => find
    .descendant(of: _widget(id), matching: find.byType(RepaintBoundary))
    .first;
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));

CanvasNode? _find(CanvasNode node) {
  if (node.id == _boundaryId) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _find(child);
      if (found != null) return found;
    }
  }
  return null;
}

Map<String, Object?>? _findJson(Map<String, Object?> node) {
  if (node['id'] == _boundaryId) return node;
  for (final value in (node['slots']! as Map<String, Object?>).values) {
    if ((value! as Map<String, Object?>)['child']
        case final Map<String, Object?> child) {
      final found = _findJson(child);
      if (found != null) return found;
    }
  }
  return null;
}

Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?>? child,
  Size? bounds,
  bool outerBoundary = false,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  var content = _node(_boundaryId, _type, child: child);
  if (bounds != null) {
    content = _node(
      '91842091-f7b6-4ec8-a25b-e9c59831fb3e',
      'flutter.widgets.SizedBox',
      child: content,
      properties: {
        'width': {'kind': 'double', 'value': bounds.width},
        'height': {'kind': 'double', 'value': bounds.height},
      },
    );
  }
  if (outerBoundary) content = _node(_outerId, _type, child: content);
  model['root'] = {
    'id': '914b371d-de47-4074-990c-e8eaac75721e',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          '3e7aaeed-150a-42e9-ad89-23b59bb1f16b',
          'flutter.widgets.Center',
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
Map<String, Object?> _paintedChild({String argb = '0xFF112233'}) => _node(
  _childId,
  'flutter.widgets.ColoredBox',
  properties: {
    'color': {'kind': 'color', 'argb': argb},
  },
  child: _sizedLeaf(),
);
Map<String, Object?> _sizedLeaf() => _node(
  _leafId,
  'flutter.widgets.SizedBox',
  properties: {
    'width': {'kind': 'integer', 'value': 80},
    'height': {'kind': 'integer', 'value': 60},
  },
);
