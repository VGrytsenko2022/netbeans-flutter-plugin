import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_container_test.dart' as c;

const types = [
  'flutter.widgets.AnimatedPositioned',
  'flutter.widgets.AnimatedPositioned.fromRect',
  'flutter.widgets.AnimatedPositionedDirectional',
];
const positionId = 'c27cb142-a0df-4e14-893c-e9840a542ad8';
Map<String, Object?> positioned(Map<String, Object?> raw) =>
    f.findNode(raw['root'] as Map<String, Object?>, positionId);
Map<String, Object?> data(
  String type, {
  Map<String, Object?> properties = const {},
  bool rtl = false,
  bool emptyStack = false,
}) {
  final raw = c.data(rtl: rtl), stack = a.builder(raw);
  final child = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
  child['slots'] = {'child': f.single(null)};
  final p = f.node(
    positionId,
    type,
    {
      if (type.endsWith('.fromRect')) ...{
        'rect': {'kind': 'string', 'value': 'local'},
        'rectLeft': f.number(0),
        'rectTop': f.number(0),
        'rectWidth': f.number(48),
        'rectHeight': f.number(48),
      },
      'durationUs': {'kind': 'integer', 'value': 100000},
      ...properties,
    },
    {'child': f.single(child)},
  );
  stack['type'] = 'flutter.widgets.Stack';
  stack['properties'] = <String, Object?>{};
  stack['slots'] = {
    'children': {
      'kind': 'list',
      'children': emptyStack ? <Object?>[] : <Object?>[p],
    },
  };
  return raw;
}

Finder native(String type) => find.byType(
  type.endsWith('Directional')
      ? AnimatedPositionedDirectional
      : AnimatedPositioned,
);
void intrinsicTests() {
  for (final type in types) {
    for (final wrapper in ['IntrinsicWidth', 'IntrinsicHeight']) {
      testWidgets(
        'Stack intrinsic sizing survives preview guard $type $wrapper',
        (tester) async {
          final raw = data(
            type,
            properties: {
              type.endsWith('.fromRect')
                  ? 'rectLeft'
                  : type.endsWith('Directional')
                  ? 'start'
                  : 'left': f.number(
                0,
              ),
            },
          );
          final align = f.findNode(
            raw['root'] as Map<String, Object?>,
            'b62f3f42-a721-422f-8cf2-6579867a5204',
          );
          final stack = a.builder(raw);
          align['slots'] = {
            'child': f.single(
              f.node(
                'c27cb142-a0df-4e14-893c-e9840a542ae0',
                'flutter.widgets.$wrapper',
                {},
                {'child': f.single(stack)},
              ),
            ),
          };
          await f.pump(tester, raw);
          expect(native(type), findsOneWidget);
          final parent =
              tester.renderObject<RenderBox>(native(type)).parent
                  as RenderStack;
          expect(
            wrapper == 'IntrinsicWidth'
                ? parent.size.width
                : parent.size.height,
            0,
          );
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
}

void edgeTests() {
  intrinsicTests();
  test('local rectangle derived edges must remain finite', () {
    for (final pair in [
      ['rectLeft', 'rectWidth'],
      ['rectTop', 'rectHeight'],
    ]) {
      expect(
        () => f.decode(
          data(
            types[1],
            properties: {pair[0]: f.number(1e308), pair[1]: f.number(1e308)},
          ),
        ),
        throwsFormatException,
      );
      expect(
        () => f.decode(
          data(
            types[1],
            properties: {pair[0]: f.number(1e308), pair[1]: f.number(-1e308)},
          ),
        ),
        returnsNormally,
      );
    }
  });
  for (final type in types) {
    test(
      'positioned required child rejects sliver and Flex parent data $type',
      () {
        for (final child in [
          'flutter.widgets.SliverToBoxAdapter',
          'flutter.widgets.Expanded',
          'flutter.widgets.Flexible',
          'flutter.widgets.Spacer',
          ...types,
        ]) {
          expect(
            canvasWrapperAcceptsExistingChild(
              wrapperWidgetType: type,
              childWidgetType: child,
            ),
            isFalse,
          );
        }
      },
    );
    testWidgets(
      'unbounded positioned-only Stack is isolated and recovers $type',
      (tester) async {
        final raw = data(
          type,
          properties: {
            type.endsWith('.fromRect')
                ? 'rectLeft'
                : type.endsWith('Directional')
                ? 'start'
                : 'left': f.number(
              0,
            ),
          },
        );
        (f.findNode(
                  raw['root'] as Map<String, Object?>,
                  a.frameId,
                )['properties']
                as Map)
            .remove('height');
        await f.pump(tester, raw);
        expect(native(type), findsNothing);
        expect(
          find.byWidgetPredicate(
            (w) =>
                w is Tooltip &&
                (w.message?.contains(
                      'all children are positioned but parent bounds are unbounded',
                    ) ??
                    false),
          ),
          findsWidgets,
        );
        expect(tester.takeException(), isNull);
        // A non-positioned sibling legitimately sizes an unbounded Stack.
        final stackChildren =
            ((a.builder(raw)['slots'] as Map)['children'] as Map)['children']
                as List;
        stackChildren.add(
          f.node(
            'c27cb142-a0df-4e14-893c-e9840a542ad9',
            'flutter.widgets.SizedBox',
            {'width': f.number(60), 'height': f.number(60)},
          ),
        );
        await f.pump(tester, raw);
        expect(native(type), findsOneWidget);
        expect(tester.takeException(), isNull);
        await f.pump(tester, data(type));
        expect(native(type), findsOneWidget);
        expect(tester.takeException(), isNull);
      },
    );
    testWidgets(
      'zero-size selection handle follows offset-only animation $type',
      (tester) async {
        final left = type.endsWith('.fromRect')
            ? 'rectLeft'
            : type.endsWith('Directional')
            ? 'start'
            : 'left';
        final top = type.endsWith('.fromRect') ? 'rectTop' : 'top';
        final width = type.endsWith('.fromRect') ? 'rectWidth' : 'width';
        final height = type.endsWith('.fromRect') ? 'rectHeight' : 'height';
        final props = {
          left: f.number(30),
          top: f.number(30),
          width: f.number(0),
          height: f.number(0),
        };
        final selected = <String>[];
        await f.pump(
          tester,
          data(type, properties: props),
          selected: selected.add,
        );
        await tester.pumpAndSettle();
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-group-$positionId'),
        );
        expect(handle, findsOneWidget);
        final start = tester.getCenter(handle);
        props[left] = f.number(110);
        await f.pump(
          tester,
          data(type, properties: props),
          selected: selected.add,
        );
        await tester.pump(const Duration(milliseconds: 50));
        await tester.pump(); // publish the post-layout overlay measurement
        final mid = tester.getCenter(handle);
        expect(mid.dx, greaterThan(start.dx + 20));
        await tester.pumpAndSettle();
        final end = tester.getCenter(handle);
        expect(end.dx, greaterThan(mid.dx + 20));
        await tester.tapAt(end);
        expect(selected, contains(positionId));
        props[width] = f.number(40);
        props[height] = f.number(40);
        await f.pump(tester, data(type, properties: props));
        await tester.pumpAndSettle();
        expect(handle, findsNothing);
        expect(tester.takeException(), isNull);
      },
    );
    testWidgets(
      'real Canvas drag resolver offers Stack wrap target only $type',
      (tester) async {
        final raw = data(type);
        final child = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
        a.builder(raw)['slots'] = {
          'children': {
            'kind': 'list',
            'children': [child],
          },
        };
        CanvasDropResolver? resolver;
        await f.pump(tester, raw, drop: (r) => resolver = r);
        final point = tester.getCenter(
          find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
        );
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final target = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(token: 'test', widgetType: type, traits: {}),
        );
        expect(target?.parentWidgetId, a.builderId);
        expect(target?.slotName, 'children');
        expect(target?.insertionIndex, 0);
        expect(tester.takeException(), isNull);
      },
    );
    if (!type.endsWith('.fromRect')) {
      testWidgets(
        'null drops tween and interrupted transition restarts at current value $type',
        (tester) async {
          final left = type.endsWith('Directional') ? 'start' : 'left';
          await f.pump(tester, data(type, properties: {left: f.number(0)}));
          final state = tester.state(native(type));
          await f.pump(tester, data(type, properties: {left: f.number(80)}));
          await tester.pump(const Duration(milliseconds: 50));
          expect(parentData(tester, type).left, closeTo(40, .001));
          await f.pump(
            tester,
            data(
              type,
              properties: {
                left: {'kind': 'null'},
              },
            ),
          );
          expect(parentData(tester, type).left, isNull);
          await f.pump(tester, data(type, properties: {left: f.number(80)}));
          expect(parentData(tester, type).left, 80);
          await f.pump(tester, data(type, properties: {left: f.number(160)}));
          await tester.pump(const Duration(milliseconds: 50));
          expect(parentData(tester, type).left, closeTo(120, .001));
          await f.pump(tester, data(type, properties: {left: f.number(200)}));
          await tester.pumpAndSettle();
          expect(parentData(tester, type).left, 200);
          expect(tester.state(native(type)), same(state));
          expect(tester.takeException(), isNull);
        },
      );
    }
    testWidgets(
      'native onEnd completes once per transition, zero duration included $type',
      (tester) async {
        var end = 0;
        double x = 0;
        var duration = const Duration(milliseconds: 100);
        late StateSetter update;
        await tester.pumpWidget(
          Directionality(
            textDirection: TextDirection.ltr,
            child: StatefulBuilder(
              builder: (context, set) {
                update = set;
                final child = const SizedBox(width: 20, height: 20);
                return Stack(
                  children: [
                    switch (type) {
                      'flutter.widgets.AnimatedPositioned.fromRect' =>
                        AnimatedPositioned.fromRect(
                          rect: Rect.fromLTWH(x, 0, 20, 20),
                          duration: duration,
                          onEnd: () => end++,
                          child: child,
                        ),
                      'flutter.widgets.AnimatedPositionedDirectional' =>
                        AnimatedPositionedDirectional(
                          start: x,
                          duration: duration,
                          onEnd: () => end++,
                          child: child,
                        ),
                      _ => AnimatedPositioned(
                        left: x,
                        duration: duration,
                        onEnd: () => end++,
                        child: child,
                      ),
                    },
                  ],
                );
              },
            ),
          ),
        );
        expect(end, 0);
        update(() {
          x = 80;
        });
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 50));
        update(() {
          x = 100;
        });
        await tester.pump();
        await tester.pumpAndSettle();
        expect(end, 1); // cancelled first target is not a completion
        update(() {});
        await tester.pumpAndSettle();
        expect(end, 1);
        update(() {
          x = 120;
          duration = Duration.zero;
        });
        await tester.pumpAndSettle();
        expect(end, 2);
        await tester.pumpWidget(const SizedBox.shrink());
        expect(tester.takeException(), isNull);
      },
    );
  }
}

Finder body() => find.byKey(const ValueKey('canvas-widget-$positionId'));
Positioned parentData(WidgetTester tester, String type) =>
    tester.widget<Positioned>(
      find
          .descendant(of: native(type), matching: find.byType(Positioned))
          .first,
    );
void main() {
  edgeTests();
  test(
    'three schemas strict placement required child and axis constraints',
    () {
      for (final type in types) {
        expect(() => f.decode(data(type)), returnsNormally);
        final root = data(type);
        root['root'] = positioned(root);
        expect(() => f.decode(root), throwsFormatException);
        final wrong = data(type);
        a.builder(wrong)['type'] = 'flutter.widgets.Column';
        expect(() => f.decode(wrong), throwsFormatException);
        final indexed = data(type);
        a.builder(indexed)['type'] = 'flutter.widgets.IndexedStack';
        expect(() => f.decode(indexed), throwsFormatException);
        final empty = data(type);
        positioned(empty)['slots'] = {'child': f.single(null)};
        expect(() => f.decode(empty), throwsFormatException);
        for (final bad in [-1, 9007199254740992]) {
          expect(
            () => f.decode(
              data(
                type,
                properties: {
                  'durationUs': {'kind': 'integer', 'value': bad},
                },
              ),
            ),
            throwsFormatException,
          );
        }
        if (type.endsWith('.fromRect')) continue;
        final horizontal = type.endsWith('Directional')
            ? ['start', 'end', 'width']
            : ['left', 'right', 'width'];
        for (final axis in [
          horizontal,
          ['top', 'bottom', 'height'],
        ]) {
          final conflict = {for (final name in axis) name: f.number(1)};
          expect(
            () => f.decode(data(type, properties: conflict)),
            throwsFormatException,
          );
          conflict[axis.last] = {'kind': 'null'};
          expect(
            () => f.decode(data(type, properties: conflict)),
            returnsNormally,
          );
        }
      }
    },
  );
  test(
    'DnD wraps existing Stack child only and never nests ParentData wrappers',
    () {
      for (final type in types) {
        for (final parent in [
          'flutter.widgets.Stack',
          'flutter.widgets.IndexedStack',
          'flutter.widgets.Column',
          'flutter.widgets.Padding',
        ]) {
          final slot = parent.endsWith('Padding') ? 'child' : 'children';
          expect(
            canvasDropTargetAcceptsSource(
              parentWidgetType: parent,
              slotName: slot,
              currentChildCount: 1,
              insertionIndex: 0,
              source: CanvasPaletteDragSource(
                token: 'test',
                widgetType: type,
                traits: {},
              ),
            ),
            parent == 'flutter.widgets.Stack',
          );
          expect(
            canvasWrapperAcceptsExistingChild(
              wrapperWidgetType: type,
              childWidgetType: 'flutter.widgets.Text',
            ),
            isTrue,
          );
          expect(
            canvasWrapperAcceptsExistingChild(
              wrapperWidgetType: type,
              childWidgetType: type,
            ),
            isFalse,
          );
        }
      }
    },
  );
  for (final type in types) {
    testWidgets(
      'all native arguments, project isolation and required child $type',
      (tester) async {
        final raw = data(type);
        final props = positioned(raw)['properties'] as Map;
        for (final name in [
          'curve',
          'durationUs',
          'onEnd',
          if (type.endsWith('.fromRect'))
            'rect'
          else
            type.endsWith('Directional') ? 'start' : 'left',
        ]) {
          props[name] = {'kind': 'dartObjectReferencePresence'};
        }
        await f.pump(tester, raw);
        final n = tester.widget<ImplicitlyAnimatedWidget>(native(type));
        expect(n.duration, const Duration(milliseconds: 300));
        expect(n.curve, Curves.linear);
        expect(n.onEnd, isNull);
        final pd = parentData(tester, type);
        expect(pd.left, type.endsWith('.fromRect') ? 0 : null);
        expect(pd.width, type.endsWith('.fromRect') ? 48 : null);
        expect(
          find.byWidgetPredicate(
            (w) =>
                w is Tooltip &&
                w.message?.contains('preview limitation: project-owned') ==
                    true,
          ),
          findsWidgets,
        );
        expect(tester.takeException(), isNull);
      },
    );
    for (final rtl in [false, true]) {
      testWidgets(
        'real StackParentData animation retained state and RTL $type rtl=$rtl',
        (tester) async {
          final left = type.endsWith('.fromRect')
              ? 'rectLeft'
              : type.endsWith('Directional')
              ? 'start'
              : 'left';
          final top = type.endsWith('.fromRect') ? 'rectTop' : 'top';
          await f.pump(
            tester,
            data(
              type,
              rtl: rtl,
              properties: {left: f.number(0), top: f.number(0)},
            ),
          );
          final state = tester.state(native(type));
          await f.pump(
            tester,
            data(
              type,
              rtl: rtl,
              properties: {left: f.number(80), top: f.number(40)},
            ),
          );
          await tester.pump(const Duration(milliseconds: 50));
          final pd = parentData(tester, type);
          expect(
            type.endsWith('Directional') && rtl ? pd.right : pd.left,
            closeTo(40, .01),
          );
          expect(pd.top, closeTo(20, .01));
          final box = tester.renderObject<RenderBox>(native(type));
          expect(box.parentData, isA<StackParentData>());
          expect(
            (box.parentData! as StackParentData).offset.dx,
            closeTo(
              type.endsWith('Directional') && rtl ? 240 - 40 - 48 : 40,
              .1,
            ),
          );
          expect(identical(state, tester.state(native(type))), isTrue);
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull);
        },
      );
    }
    for (final curve in <String, Curve>{
      'linear': Curves.linear,
      'decelerate': Curves.decelerate,
      'fastLinearToSlowEaseIn': Curves.fastLinearToSlowEaseIn,
      'fastEaseInToSlowEaseOut': Curves.fastEaseInToSlowEaseOut,
      'ease': Curves.ease,
      'easeIn': Curves.easeIn,
      'easeInToLinear': Curves.easeInToLinear,
      'easeInSine': Curves.easeInSine,
      'easeInQuad': Curves.easeInQuad,
      'easeInCubic': Curves.easeInCubic,
      'easeInQuart': Curves.easeInQuart,
      'easeInQuint': Curves.easeInQuint,
      'easeInExpo': Curves.easeInExpo,
      'easeInCirc': Curves.easeInCirc,
      'easeInBack': Curves.easeInBack,
      'easeOut': Curves.easeOut,
      'linearToEaseOut': Curves.linearToEaseOut,
      'easeOutSine': Curves.easeOutSine,
      'easeOutQuad': Curves.easeOutQuad,
      'easeOutCubic': Curves.easeOutCubic,
      'easeOutQuart': Curves.easeOutQuart,
      'easeOutQuint': Curves.easeOutQuint,
      'easeOutExpo': Curves.easeOutExpo,
      'easeOutCirc': Curves.easeOutCirc,
      'easeOutBack': Curves.easeOutBack,
      'easeInOut': Curves.easeInOut,
      'easeInOutSine': Curves.easeInOutSine,
      'easeInOutQuad': Curves.easeInOutQuad,
      'easeInOutCubic': Curves.easeInOutCubic,
      'easeInOutCubicEmphasized': Curves.easeInOutCubicEmphasized,
      'easeInOutQuart': Curves.easeInOutQuart,
      'easeInOutQuint': Curves.easeInOutQuint,
      'easeInOutExpo': Curves.easeInOutExpo,
      'easeInOutCirc': Curves.easeInOutCirc,
      'easeInOutBack': Curves.easeInOutBack,
      'fastOutSlowIn': Curves.fastOutSlowIn,
      'slowMiddle': Curves.slowMiddle,
      'bounceIn': Curves.bounceIn,
      'bounceOut': Curves.bounceOut,
      'bounceInOut': Curves.bounceInOut,
      'elasticIn': Curves.elasticIn,
      'elasticOut': Curves.elasticOut,
      'elasticInOut': Curves.elasticInOut,
    }.entries) {
      testWidgets('all six native tweens and overshoot $type ${curve.key}', (
        tester,
      ) async {
        final names = type.endsWith('.fromRect')
            ? ['rectLeft', 'rectTop', 'rectWidth', 'rectHeight']
            : type.endsWith('Directional')
            ? ['start', 'top', 'end', 'bottom', 'width', 'height']
            : ['left', 'top', 'right', 'bottom', 'width', 'height'];
        for (final name in names) {
          final props = <String, Object?>{
            'curve': {'kind': 'string', 'value': curve.key},
            name: f.number(40),
          };
          await tester.pumpWidget(const SizedBox.shrink());
          await f.pump(tester, data(type, properties: props));
          props[name] = f.number(80);
          await f.pump(tester, data(type, properties: props));
          await tester.pump(const Duration(milliseconds: 50));
          final pd = parentData(tester, type);
          final values = {
            'left': pd.left,
            'top': pd.top,
            'right': pd.right,
            'bottom': pd.bottom,
            'width': pd.width,
            'height': pd.height,
            'start': pd.left,
            'end': pd.right,
            'rectLeft': pd.left,
            'rectTop': pd.top,
            'rectWidth': pd.width,
            'rectHeight': pd.height,
          };
          expect(
            values[name],
            closeTo(40 + 40 * curve.value.transform(.5), .001),
          );
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull);
        }
      });
    }
    testWidgets(
      'negative requested sizes are preserved then clamped by native Stack $type',
      (tester) async {
        await f.pump(
          tester,
          data(
            type,
            properties: {
              type.endsWith('.fromRect') ? 'rectWidth' : 'width': f.number(-40),
              type.endsWith('.fromRect') ? 'rectHeight' : 'height': f.number(
                -20,
              ),
            },
          ),
        );
        expect(parentData(tester, type).width, -40);
        expect(tester.renderObject<RenderBox>(body()).size, Size.zero);
        expect(tester.takeException(), isNull);
      },
    );
  }
}
