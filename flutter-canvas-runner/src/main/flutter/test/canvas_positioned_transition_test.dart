import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_animated_positioned_test.dart' as p;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.PositionedTransition';
const edges = ['rectLeft', 'rectTop', 'rectRight', 'rectBottom'];
Map<String, Object?> data({
  List<double> insets = const [0, 0, 0, 0],
  bool rtl = false,
  bool source = false,
  String fit = 'loose',
  String clip = 'hardEdge',
}) {
  final raw = p.data(p.types.first, rtl: rtl), node = p.positioned(raw);
  node['type'] = type;
  node['properties'] = <String, Object?>{
    'rect': source
        ? {'kind': 'dartObjectReferencePresence'}
        : {'kind': 'string', 'value': 'local'},
    for (var i = 0; i < 4; i++) edges[i]: f.number(insets[i]),
  };
  a.builder(raw)['properties'] = {
    'fit': {'kind': 'enum', 'type': 'StackFit', 'value': fit},
    'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': clip},
  };
  return raw;
}

Finder native() => find.byType(PositionedTransition);
Finder child() => find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
void main() {
  test('closed domains and direct Stack-only required-child placement', () {
    final raw = data(), props = p.positioned(raw)['properties'] as Map;
    for (final name in ['rect', ...edges]) {
      final original = props.remove(name);
      expect(() => f.decode(raw), throwsFormatException);
      props[name] = original;
      props[name] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException);
      props[name] = original;
    }
    for (final edge in edges) {
      final original = props[edge];
      for (final bad in [
        {'kind': 'string', 'value': '2'},
        {'kind': 'boolean', 'value': true},
        {'kind': 'double', 'value': 'NaN'},
        {'kind': 'double', 'value': 'Infinity'},
        {'kind': 'integer', 'value': 9007199254740992},
        {'kind': 'dartObjectReferencePresence'},
      ]) {
        props[edge] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
      props[edge] = original;
    }
    for (final bad in [
      {'kind': 'double', 'value': 1},
      {'kind': 'string', 'value': 'RelativeRect.fill'},
      {'kind': 'dartObjectReferencePresence', 'root': 'leak'},
    ]) {
      props['rect'] = bad;
      expect(() => f.decode(raw), throwsFormatException);
    }
    props['rect'] = {'kind': 'string', 'value': 'local'};
    for (final name in [
      'curve',
      'durationUs',
      'onEnd',
      'textDirection',
      'width',
      'height',
      'left',
      'size',
      'animation',
    ]) {
      props[name] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException);
      props.remove(name);
    }
    p.positioned(raw)['slots'] = {'child': f.single(null)};
    expect(() => f.decode(raw), throwsFormatException);
    for (final parent in ['Column', 'Row', 'IndexedStack']) {
      final wrong = data();
      a.builder(wrong)['type'] = 'flutter.widgets.$parent';
      a.builder(wrong)['properties'] = <String, Object?>{};
      expect(() => f.decode(wrong), throwsFormatException);
    }
    expect(() => f.decode(data()), returnsNormally);
    final root = data();
    root['root'] = p.positioned(root);
    expect(() => f.decode(root), throwsFormatException);
    expect(canvasReviewedRequiredWrapperSlot(type), 'child');
    for (final invalid in [
      'flutter.widgets.Expanded',
      'flutter.widgets.Flexible',
      'flutter.widgets.Spacer',
      'flutter.widgets.SliverToBoxAdapter',
      ...p.types,
      type,
    ]) {
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: type,
          childWidgetType: invalid,
        ),
        false,
      );
    }
    expect(
      canvasWrapperAcceptsExistingChild(
        wrapperWidgetType: type,
        childWidgetType: 'flutter.widgets.Text',
      ),
      true,
    );
  });
  final cases = <List<double>>[
    [0, 0, 0, 0],
    [10, 20, 30, 40],
    [-10, -20, -30, -40],
    [240, 160, 0, 0],
    [300, 200, 80, 60],
    [10.5, 20.25, 30.75, 40.125],
    [-20, 10, 300, -10],
    [0, 0, 240, 160],
    [100, 50, 100, 50],
    [1, 2, 3, 4],
    [20, 20, -20, -20],
  ];
  for (final insets in cases) {
    for (final rtl in [false, true]) {
      for (final fit in ['loose', 'expand', 'passthrough']) {
        for (final clip in [
          'none',
          'hardEdge',
          'antiAlias',
          'antiAliasWithSaveLayer',
        ]) {
          testWidgets(
            'native physical insets=$insets rtl=$rtl fit=$fit clip=$clip',
            (tester) async {
              await f.pump(
                tester,
                data(insets: insets, rtl: rtl, fit: fit, clip: clip),
              );
              final n = tester.widget<PositionedTransition>(native());
              expect(n.rect, isA<AlwaysStoppedAnimation<RelativeRect>>());
              expect(
                n.rect.value,
                RelativeRect.fromLTRB(
                  insets[0],
                  insets[1],
                  insets[2],
                  insets[3],
                ),
              );
              final box = tester.renderObject<RenderBox>(native()),
                  parent = box.parent as RenderStack;
              final pd = box.parentData as StackParentData;
              expect(parent.size, const Size(240, 160));
              expect(
                box.size,
                Size(
                  math.max(0, 240 - insets[2] - insets[0]).toDouble(),
                  math.max(0, 160 - insets[3] - insets[1]).toDouble(),
                ),
              );
              expect(pd.offset, Offset(insets[0], insets[1]));
              expect(pd.left, insets[0]);
              expect(pd.top, insets[1]);
              expect(pd.right, insets[2]);
              expect(pd.bottom, insets[3]);
              expect(pd.width, isNull);
              expect(pd.height, isNull);
              expect(parent.clipBehavior, Clip.values.byName(clip));
              expect(tester.takeException(), isNull);
            },
          );
        }
      }
    }
  }
  testWidgets(
    'source isolation mode changes and resize preserve native State and child',
    (tester) async {
      await f.pump(tester, data(insets: [10, 20, 30, 40]));
      final state = tester.state(native()), element = tester.element(child());
      await f.pump(tester, data(insets: [90, 70, 110, 90], source: true));
      expect(identical(state, tester.state(native())), true);
      expect(identical(element, tester.element(child())), true);
      expect(
        tester.widget<PositionedTransition>(native()).rect.value,
        RelativeRect.fill,
      );
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              (w.message?.contains('project-owned Animation<RelativeRect>') ??
                  false),
        ),
        findsWidgets,
      );
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              (w.message?.contains('duration as 300 ms') ?? false),
        ),
        findsNothing,
      );
      await f.pump(tester, data(insets: [15, 25, 35, 45]));
      expect(identical(state, tester.state(native())), true);
      expect(identical(element, tester.element(child())), true);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              (w.message?.contains('project-owned Animation<RelativeRect>') ??
                  false),
        ),
        findsNothing,
      );
      final raw = data(insets: [15, 25, 35, 45]);
      (f.findNode(raw['root'] as Map<String, Object?>, a.frameId)['properties']
          as Map)['width'] = f.number(
        300,
      );
      await f.pump(tester, raw);
      expect(tester.getSize(native()).width, 250);
      expect(identical(state, tester.state(native())), true);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets('collapsed positioned child remains selectable and draggable', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    final selected = <String>[];
    await f.pump(
      tester,
      data(insets: [10, 20, 230, 140]),
      drop: (r) => drop = r,
      selected: selected.add,
    );
    final handle = find.byKey(
      const ValueKey('canvas-zero-size-widget-target-group-${p.positionId}'),
    );
    expect(handle, findsOneWidget);
    await tester.tap(handle);
    expect(selected, contains(p.positionId));
    expect(drop, isNotNull);
    expect(tester.takeException(), isNull);
  });
  testWidgets('Canvas resolver wraps only the existing direct Stack child', (
    tester,
  ) async {
    final raw = data(),
        existing = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
    a.builder(raw)['slots'] = {
      'children': {
        'kind': 'list',
        'children': [existing],
      },
    };
    CanvasDropResolver? resolver;
    await f.pump(tester, raw, drop: (r) => resolver = r);
    final point = tester.getCenter(child()),
        surface = tester.getRect(find.byType(CanvasDocumentView));
    final target = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(
        token: 'position-test',
        widgetType: type,
        traits: {},
      ),
    );
    expect(target?.parentWidgetId, a.builderId);
    expect(target?.slotName, 'children');
    expect(target?.insertionIndex, 0);
    expect(tester.takeException(), isNull);
  });
  for (final wrapper in ['IntrinsicWidth', 'IntrinsicHeight']) {
    testWidgets('native intrinsic forwarding $wrapper', (tester) async {
      final raw = data(),
          align = f.findNode(
            raw['root'] as Map<String, Object?>,
            'b62f3f42-a721-422f-8cf2-6579867a5204',
          );
      align['slots'] = {
        'child': f.single(
          f.node(
            'c27cb142-a0df-4e14-893c-e9840a542ae0',
            'flutter.widgets.$wrapper',
            {},
            {'child': f.single(a.builder(raw))},
          ),
        ),
      };
      await f.pump(tester, raw);
      expect(native(), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }
  for (final source in [false, true]) {
    testWidgets(
      'unbounded positioned-only Stack shows diagnostic and recovers source=$source',
      (tester) async {
        final raw = data(source: source);
        (f.findNode(
                  raw['root'] as Map<String, Object?>,
                  a.frameId,
                )['properties']
                as Map)
            .remove('height');
        await f.pump(tester, raw);
        expect(native(), findsNothing);
        expect(
          find.byWidgetPredicate(
            (w) =>
                w is Tooltip &&
                (w.message?.contains('all children are positioned') ?? false),
          ),
          findsWidgets,
        );
        expect(tester.takeException(), isNull);
        await f.pump(tester, data(source: source));
        expect(native(), findsOneWidget);
        expect(tester.takeException(), isNull);
      },
    );
  }
}
