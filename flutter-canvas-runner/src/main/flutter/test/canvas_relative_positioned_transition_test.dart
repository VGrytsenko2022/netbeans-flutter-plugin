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

const type = 'flutter.widgets.RelativePositionedTransition';
const fields = ['rectLeft', 'rectTop', 'rectWidth', 'rectHeight'];
Map<String, Object?> data({
  List<double> rect = const [0, 0, 48, 48],
  Size size = const Size(48, 48),
  bool rtl = false,
  bool source = false,
  bool sizeSource = false,
  bool nullValue = false,
  String fit = 'loose',
  String clip = 'hardEdge',
}) {
  final raw = p.data(p.types.first, rtl: rtl), node = p.positioned(raw);
  node['type'] = type;
  node['properties'] = <String, Object?>{
    'rect': source
        ? {'kind': 'dartObjectReferencePresence'}
        : {'kind': 'string', 'value': nullValue ? 'null' : 'local'},
    for (var i = 0; i < 4; i++) fields[i]: f.number(rect[i]),
    'size': sizeSource
        ? {'kind': 'dartObjectReferencePresence'}
        : {'kind': 'string', 'value': 'local'},
    'sizeWidth': f.number(size.width),
    'sizeHeight': f.number(size.height),
  };
  a.builder(raw)['properties'] = {
    'fit': {'kind': 'enum', 'type': 'StackFit', 'value': fit},
    'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': clip},
  };
  return raw;
}

Finder native() => find.byType(RelativePositionedTransition);
Finder child() => find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
void check(
  WidgetTester tester,
  Rect? rect,
  Size reference, {
  double width = 240,
  String clip = 'hardEdge',
}) {
  final n = tester.widget<RelativePositionedTransition>(native());
  expect(n.rect, isA<AlwaysStoppedAnimation<Rect?>>());
  expect(n.rect.value, rect);
  expect(n.size, reference);
  final value = rect ?? Rect.zero,
      offsets = RelativeRect.fromSize(value, reference);
  final box = tester.renderObject<RenderBox>(native()),
      parent = box.parent as RenderStack,
      pd = box.parentData as StackParentData;
  expect(parent.size, Size(width, 160));
  expect(
    box.size,
    Size(
      math.max(0, width - offsets.right - offsets.left).toDouble(),
      math.max(0, 160 - offsets.bottom - offsets.top).toDouble(),
    ),
  );
  expect(pd.offset, Offset(value.left, value.top));
  expect(pd.right, offsets.right);
  expect(pd.bottom, offsets.bottom);
  expect(pd.width, isNull);
  expect(pd.height, isNull);
  expect(parent.clipBehavior, Clip.values.byName(clip));
  expect(tester.takeException(), isNull);
}

void main() {
  test(
    'required closed domains, finite composite offsets and direct Stack placement',
    () {
      final raw = data(), props = p.positioned(raw)['properties'] as Map;
      for (final name in [
        'rect',
        ...fields,
        'size',
        'sizeWidth',
        'sizeHeight',
      ]) {
        final original = props.remove(name);
        expect(() => f.decode(raw), throwsFormatException);
        props[name] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
        props[name] = original;
      }
      for (final field in [...fields, 'sizeWidth', 'sizeHeight']) {
        final original = props[field];
        for (final bad in [
          {'kind': 'double', 'value': 'NaN'},
          {'kind': 'double', 'value': 'Infinity'},
          {'kind': 'integer', 'value': 9007199254740992},
          {'kind': 'dartObjectReferencePresence'},
        ]) {
          props[field] = bad;
          expect(() => f.decode(raw), throwsFormatException);
        }
        props[field] = original;
      }
      for (final field in ['rect', 'size']) {
        final original = props[field];
        for (final bad in [
          {'kind': 'string', 'value': 'bad'},
          {'kind': 'double', 'value': 1},
          {'kind': 'dartObjectReferencePresence', 'root': 'leak'},
        ]) {
          props[field] = bad;
          expect(() => f.decode(raw), throwsFormatException);
        }
        props[field] = original;
      }
      props['rectLeft'] = f.number(1e308);
      props['rectWidth'] = f.number(1e308);
      expect(() => f.decode(raw), throwsFormatException);
      props['rect'] = {'kind': 'string', 'value': 'null'};
      expect(() => f.decode(raw), returnsNormally);
      final root = data();
      root['root'] = p.positioned(root);
      expect(() => f.decode(root), throwsFormatException);
      for (final owner in ['Column', 'Row', 'IndexedStack']) {
        final wrong = data();
        a.builder(wrong)['type'] = 'flutter.widgets.$owner';
        a.builder(wrong)['properties'] = <String, Object?>{};
        expect(() => f.decode(wrong), throwsFormatException);
      }
      final empty = data();
      p.positioned(empty)['slots'] = {'child': f.single(null)};
      expect(() => f.decode(empty), throwsFormatException);
      expect(canvasReviewedRequiredWrapperSlot(type), 'child');
      for (final invalid in [
        'flutter.widgets.Expanded',
        'flutter.widgets.SliverToBoxAdapter',
        ...p.types,
        'flutter.widgets.PositionedTransition',
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
    },
  );
  for (final rect in <List<double>?>[
    null,
    [0, 0, 48, 48],
    [10, 20, 60, 40],
    [-10, -20, 270, 200],
    [10.5, 20.25, 60.75, 40.125],
    [0, 0, 0, 0],
    [10, 20, -30, -40],
    [300, 200, 80, 60],
  ]) {
    for (final size in [
      const Size(240, 160),
      const Size(48, 48),
      const Size(-20, 0),
    ]) {
      for (final rtl in [false, true]) {
        for (final fit in ['loose', 'expand', 'passthrough']) {
          for (final clip in [
            'none',
            'hardEdge',
            'antiAlias',
            'antiAliasWithSaveLayer',
          ]) {
            testWidgets(
              'physical LTWH=$rect reference=$size rtl=$rtl fit=$fit clip=$clip',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    rect: rect ?? [0, 0, 48, 48],
                    size: size,
                    nullValue: rect == null,
                    rtl: rtl,
                    fit: fit,
                    clip: clip,
                  ),
                );
                check(
                  tester,
                  rect == null
                      ? null
                      : Rect.fromLTWH(rect[0], rect[1], rect[2], rect[3]),
                  size,
                  clip: clip,
                );
              },
            );
          }
        }
      }
    }
  }
  testWidgets(
    'independent sources, null and actual Stack resize preserve State and child',
    (tester) async {
      await f.pump(tester, data());
      final state = tester.state(native()), element = tester.element(child());
      for (final source in [false, true]) {
        for (final sizeSource in [false, true]) {
          await f.pump(
            tester,
            data(
              rect: [10, 20, 60, 40],
              size: const Size(200, 120),
              source: source,
              sizeSource: sizeSource,
            ),
          );
          check(
            tester,
            source
                ? const Rect.fromLTWH(0, 0, 48, 48)
                : const Rect.fromLTWH(10, 20, 60, 40),
            sizeSource ? const Size(48, 48) : const Size(200, 120),
          );
          expect(identical(state, tester.state(native())), true);
          expect(identical(element, tester.element(child())), true);
          if (source || sizeSource) {
            expect(
              find.byWidgetPredicate(
                (w) =>
                    w is Tooltip &&
                    (w.message?.contains('project-owned') ?? false),
              ),
              findsWidgets,
            );
          }
        }
      }
      final raw = data(nullValue: true, size: const Size(240, 160));
      (f.findNode(raw['root'] as Map<String, Object?>, a.frameId)['properties']
          as Map)['width'] = f.number(
        300,
      );
      await f.pump(tester, raw);
      check(tester, null, const Size(240, 160), width: 300);
      expect(tester.getSize(native()), const Size(60, 0));
      expect(identical(state, tester.state(native())), true);
    },
  );
  testWidgets('collapsed child remains selectable and draggable', (
    tester,
  ) async {
    final selected = <String>[];
    CanvasDropResolver? drop;
    await f.pump(
      tester,
      data(rect: [10, 20, 0, 0], size: const Size(240, 160)),
      selected: selected.add,
      drop: (r) => drop = r,
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
