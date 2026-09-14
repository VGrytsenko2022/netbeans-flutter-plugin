import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_pinned_header_sliver_test.dart' as p;

const type = 'flutter.widgets.SliverFloatingHeader';
const local = [
  'animationStyleDurationUs',
  'animationStyleCurve',
  'animationStyleReverseDurationUs',
  'animationStyleReverseCurve',
];
Map<String, Object?> data({
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  Map<String, Object?> props = const {},
}) {
  final raw = p.data(
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    extent: 80,
  );
  final header = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  header['type'] = type;
  header['properties'] = props;
  return raw;
}

Map<String, Object?> str(String v) => {'kind': 'string', 'value': v};
Map<String, Object?> integer(int v) => {'kind': 'integer', 'value': v};
const nil = <String, Object?>{'kind': 'null'};
Finder get headerFinder =>
    find.byType(SliverFloatingHeader, skipOffstage: false);
Offset delta(AxisDirection direction, double amount) => switch (direction) {
  AxisDirection.down => Offset(0, -amount),
  AxisDirection.up => Offset(0, amount),
  AxisDirection.right => Offset(-amount, 0),
  AxisDirection.left => Offset(amount, 0),
};
Future<void> drag(WidgetTester tester, double amount) async {
  final context = tester.element(headerFinder);
  final scroll = Scrollable.of(context);
  final gesture = await tester.startGesture(
    tester.getRect(find.byType(CustomScrollView)).center,
  );
  await gesture.moveBy(delta(scroll.axisDirection, amount.sign * 24));
  await tester.pump(const Duration(milliseconds: 20));
  await gesture.moveBy(delta(scroll.axisDirection, amount));
  await tester.pump(const Duration(milliseconds: 20));
  // No ballistic fling: inspect the header's own snap animation.
  await tester.pump(const Duration(milliseconds: 200));
  await gesture.up();
  await tester.pumpAndSettle();
}

void main() {
  for (final curve in [
    'linear',
    'decelerate',
    'fastLinearToSlowEaseIn',
    'fastEaseInToSlowEaseOut',
    'ease',
    'easeIn',
    'easeInToLinear',
    'easeInSine',
    'easeInQuad',
    'easeInCubic',
    'easeInQuart',
    'easeInQuint',
    'easeInExpo',
    'easeInCirc',
    'easeInBack',
    'easeOut',
    'linearToEaseOut',
    'easeOutSine',
    'easeOutQuad',
    'easeOutCubic',
    'easeOutQuart',
    'easeOutQuint',
    'easeOutExpo',
    'easeOutCirc',
    'easeOutBack',
    'easeInOut',
    'easeInOutSine',
    'easeInOutQuad',
    'easeInOutCubic',
    'easeInOutCubicEmphasized',
    'easeInOutQuart',
    'easeInOutQuint',
    'easeInOutExpo',
    'easeInOutCirc',
    'easeInOutBack',
    'fastOutSlowIn',
    'slowMiddle',
    'bounceIn',
    'bounceOut',
    'bounceInOut',
    'elasticIn',
    'elasticOut',
    'elasticInOut',
  ]) {
    testWidgets('native appearance and disappearance curve $curve', (
      tester,
    ) async {
      await f.pump(
        tester,
        data(
          props: {
            'animationStyleCurve': str(curve),
            'animationStyleReverseCurve': str(curve),
          },
        ),
      );
      final render = tester.renderObject<RenderSliver>(headerFinder);
      await drag(tester, 200);
      expect(render.geometry!.paintExtent, 0);
      await drag(tester, -45);
      expect(render.geometry!.paintExtent, 80);
      await drag(tester, 45);
      expect(render.geometry!.paintExtent, 0);
      expect(tester.takeException(), isNull);
    });
  }

  test('strict required box child and mutually exclusive styles', () {
    final raw = data();
    final node = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
    node['slots'] = {};
    expect(() => f.decode(raw), throwsFormatException);
    node['slots'] = {'child': f.single(null)};
    expect(() => f.decode(raw), throwsFormatException);
    node['slots'] = {
      'child': f.single(f.node(f.bodyId, 'flutter.widgets.SliverToBoxAdapter')),
    };
    expect(() => f.decode(raw), throwsFormatException);
    for (final name in local) {
      expect(
        () => f.decode(data(props: {'animationStyle': nil, name: nil})),
        throwsFormatException,
      );
    }
    for (final value in [
      str('bad'),
      integer(1),
      {'kind': 'enum', 'type': 'FloatingHeaderSnapMode', 'value': 'bad'},
    ]) {
      expect(
        () => f.decode(data(props: {'snapMode': value})),
        throwsFormatException,
      );
    }
    expect(canvasReviewedRequiredWrapperSlot(type), isNull);
    expect(
      canvasDropSlotsForWidgetType(type),
      isEmpty,
      reason:
          'Required occupied Child is replaced through Slots, not appended by a pointer drop.',
    );
  });
  testWidgets(
    'all style presence combinations preserve nullable constructor fields',
    (tester) async {
      for (var mask = 0; mask < 16; mask++) {
        for (final mode in ['unset', 'null', 'overlay', 'scroll']) {
          final props = <String, Object?>{
            for (var i = 0; i < 4; i++)
              if (mask & (1 << i) != 0)
                local[i]: i.isEven
                    ? integer(i == 0 ? 120000 : 240000)
                    : str(i == 1 ? 'linear' : 'easeOut'),
            if (mode != 'unset')
              'snapMode': mode == 'null'
                  ? nil
                  : {
                      'kind': 'enum',
                      'type': 'FloatingHeaderSnapMode',
                      'value': mode,
                    },
          };
          await f.pump(tester, data(props: props));
          final native = tester.widget<SliverFloatingHeader>(headerFinder);
          expect(
            native.snapMode,
            mode == 'scroll'
                ? FloatingHeaderSnapMode.scroll
                : mode == 'overlay'
                ? FloatingHeaderSnapMode.overlay
                : null,
          );
          expect(native.animationStyle == null, mask == 0);
          if (mask != 0) {
            expect(
              native.animationStyle!.duration,
              mask & 1 != 0 ? const Duration(milliseconds: 120) : null,
            );
            expect(
              native.animationStyle!.reverseDuration,
              mask & 4 != 0 ? const Duration(milliseconds: 240) : null,
            );
            expect(
              native.animationStyle!.curve,
              mask & 2 != 0 ? Curves.linear : null,
            );
            expect(
              native.animationStyle!.reverseCurve,
              mask & 8 != 0 ? Curves.easeOut : null,
            );
          }
          expect(tester.takeException(), isNull);
        }
      }
      await f.pump(tester, data(props: {for (final name in local) name: nil}));
      final style = tester
          .widget<SliverFloatingHeader>(headerFinder)
          .animationStyle!;
      expect(style.duration, isNull);
      expect(style.reverseDuration, isNull);
      expect(style.curve, isNull);
      expect(style.reverseCurve, isNull);
    },
  );
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        for (final mode in ['default', 'overlay', 'scroll']) {
          for (final style in ['default', 'local', 'noAnimation']) {
            testWidgets(
              'native gestures and snap h=$horizontal reverse=$reverse rtl=$rtl mode=$mode style=$style',
              (tester) async {
                final props = <String, Object?>{
                  if (mode != 'default')
                    'snapMode': {
                      'kind': 'enum',
                      'type': 'FloatingHeaderSnapMode',
                      'value': mode,
                    },
                  if (style == 'noAnimation')
                    'animationStyle': str('noAnimation'),
                  if (style == 'local') ...{
                    'animationStyleDurationUs': integer(120000),
                    'animationStyleReverseDurationUs': integer(240000),
                    'animationStyleCurve': str('linear'),
                    'animationStyleReverseCurve': str('easeOut'),
                  },
                };
                await f.pump(
                  tester,
                  data(
                    horizontal: horizontal,
                    reverse: reverse,
                    rtl: rtl,
                    props: props,
                  ),
                );
                final render = tester.renderObject<RenderSliver>(headerFinder);
                expect(render.geometry!.scrollExtent, 80);
                expect(render.geometry!.paintExtent, 80);
                await drag(tester, 200);
                expect(render.geometry!.paintExtent, 0);
                expect(render.geometry!.maxScrollObstructionExtent, 0);
                await drag(tester, -45);
                expect(render.geometry!.paintExtent, 80);
                expect(
                  render.geometry!.layoutExtent,
                  mode == 'scroll' ? 80 : 0,
                );
                expect(
                  identical(render, tester.renderObject(headerFinder)),
                  isTrue,
                );
                await drag(tester, 45);
                expect(render.geometry!.paintExtent, 0);
                expect(tester.takeException(), isNull);
              },
            );
          }
        }
      }
    }
  }
  testWidgets(
    'project references remain inert and negative durations are explicit preview approximations',
    (tester) async {
      for (final field in ['animationStyle', ...local]) {
        final raw = data(
          props: {
            field: {'kind': 'dartObjectReferencePresence'},
          },
        );
        await f.pump(tester, raw);
        expect(
          tester
              .widgetList<Tooltip>(find.byType(Tooltip))
              .map((v) => v.message ?? '')
              .join(' '),
          contains('Project-owned'),
        );
        await drag(tester, 200);
        await drag(tester, -45);
        expect(tester.takeException(), isNull);
      }
      final raw = data(
        props: {
          'animationStyleDurationUs': integer(-17),
          'animationStyleReverseDurationUs': integer(-19),
        },
      );
      await f.pump(tester, raw);
      final style = tester
          .widget<SliverFloatingHeader>(headerFinder)
          .animationStyle!;
      expect(style.duration, Duration.zero);
      expect(style.reverseDuration, Duration.zero);
      expect(
        tester
            .widgetList<Tooltip>(find.byType(Tooltip))
            .map((v) => v.message ?? '')
            .join(' '),
        contains('Negative'),
      );
      expect(
        (f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['properties']
            as Map)['animationStyleDurationUs'],
        integer(-17),
      );
      await drag(tester, 200);
      await drag(tester, -45);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'required child move guard, visible hit target and size relayout preserve identity',
    (tester) async {
      CanvasDropResolver? drop;
      CanvasMovePreviewResolver? move;
      final raw = data(props: {'animationStyle': str('noAnimation')});
      await f.pump(tester, raw, drop: (v) => drop = v, move: (v) => move = v);
      expect(
        move!(f.bodyId, p.tailBoxId, 'child', 0),
        isNull,
        reason: 'Moving out must not empty the required child.',
      );
      final state = tester.state(headerFinder);
      final render = tester.renderObject<RenderSliver>(headerFinder);
      await drag(tester, 200);
      await drag(tester, -45);
      final rect = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
      );
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = rect
          .intersect(tester.getRect(find.byType(CustomScrollView)))
          .center;
      final hit = drop!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'box',
          widgetType: 'flutter.widgets.Text',
          traits: {},
        ),
      );
      expect(hit?.parentWidgetId, f.bodyId);
      expect(hit?.slotName, 'child');
      (f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['properties']
          as Map)['height'] = f.number(
        100,
      );
      await f.pump(tester, raw);
      expect(identical(state, tester.state(headerFinder)), isTrue);
      expect(identical(render, tester.renderObject(headerFinder)), isTrue);
      expect(render.geometry!.scrollExtent, 100);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    },
  );
}
