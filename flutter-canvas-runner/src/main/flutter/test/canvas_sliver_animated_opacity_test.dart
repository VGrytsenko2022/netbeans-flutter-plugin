import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_sliver_opacity_test.dart' as o;

const type = 'flutter.widgets.SliverAnimatedOpacity';
Map<String, Object?> data({
  double opacity = 1,
  bool? semantics,
  int duration = 300000,
  String? curve,
  bool empty = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
}) {
  final raw = o.data(
    opacity: opacity,
    semantics: semantics,
    empty: empty,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
  );
  final widget = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  widget['type'] = type;
  (widget['properties'] as Map)['durationUs'] = {
    'kind': 'integer',
    'value': duration,
  };
  if (curve != null) {
    (widget['properties'] as Map)['curve'] = {'kind': 'string', 'value': curve};
  }
  return raw;
}

void main() {
  testWidgets('project-owned values are presence-only and never executed', (
    tester,
  ) async {
    final raw = data();
    final props =
        f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['properties']
            as Map;
    for (final field in ['curve', 'durationUs', 'onEnd']) {
      props[field] = {'kind': 'dartObjectReferencePresence'};
    }
    await f.pump(tester, raw);
    final native = tester.widget<SliverAnimatedOpacity>(
      find.byType(SliverAnimatedOpacity),
    );
    expect(native.curve, Curves.linear);
    expect(native.duration, const Duration(milliseconds: 300));
    expect(native.onEnd, isNull);
    expect(tester.takeException(), isNull);
  });
  test('strict animation domains, required values and optional callback', () {
    for (final duration in [-1, 9007199254740992]) {
      expect(() => f.decode(data(duration: duration)), throwsFormatException);
    }
    for (final opacity in [-.1, 1.1]) {
      expect(() => f.decode(data(opacity: opacity)), throwsFormatException);
    }
    for (final field in [
      'opacity',
      'durationUs',
      'curve',
      'alwaysIncludeSemantics',
    ]) {
      final raw = data();
      final props =
          f.findNode(
                raw['root'] as Map<String, Object?>,
                f.fillId,
              )['properties']
              as Map;
      props[field] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException);
    }
    for (final field in ['opacity', 'durationUs']) {
      final raw = data();
      (f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['properties']
              as Map)
          .remove(field);
      expect(() => f.decode(raw), throwsFormatException);
    }
    expect(() => f.decode(data(curve: 'unreviewed')), throwsFormatException);
    for (final duration in [0, 1, 9007199254740991]) {
      expect(() => f.decode(data(duration: duration)), returnsNormally);
    }
    final raw = data();
    (f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['properties']
        as Map)['onEnd'] = {
      'kind': 'null',
    };
    expect(() => f.decode(raw), returnsNormally);
  });
  for (final opacity in [0.0, .125, 1.0]) {
    for (final semantics in <bool?>[null, false, true]) {
      for (final empty in [false, true]) {
        for (final horizontal in [false, true]) {
          for (final reverse in [false, true]) {
            for (final rtl in [false, true]) {
              testWidgets(
                'native defaults extent semantics o=$opacity s=$semantics e=$empty h=$horizontal r=$reverse rtl=$rtl',
                (tester) async {
                  await f.pump(
                    tester,
                    data(
                      opacity: opacity,
                      semantics: semantics,
                      empty: empty,
                      horizontal: horizontal,
                      reverse: reverse,
                      rtl: rtl,
                    ),
                  );
                  final native = tester.widget<SliverAnimatedOpacity>(
                    find.byType(SliverAnimatedOpacity, skipOffstage: false),
                  );
                  expect(native.opacity, opacity);
                  expect(native.curve, Curves.linear);
                  expect(native.duration, const Duration(milliseconds: 300));
                  expect(native.alwaysIncludeSemantics, semantics ?? false);
                  expect(native.onEnd, isNull);
                  final render = tester
                      .renderObject<RenderSliverAnimatedOpacity>(
                        find.byType(SliverAnimatedOpacity, skipOffstage: false),
                      );
                  expect(render.geometry!.scrollExtent, empty ? 0 : 48);
                  final children = <RenderObject>[];
                  render.visitChildrenForSemantics(children.add);
                  expect(children.isNotEmpty, opacity > 0 || semantics == true);
                  expect(tester.takeException(), isNull);
                },
              );
            }
          }
        }
      }
    }
  }
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
    for (final duration in [0, 1, 200000]) {
      testWidgets(
        'retarget animation and exact terminal state $curve $duration',
        (tester) async {
          await f.pump(tester, data(curve: curve, duration: duration));
          final body = find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
          final initial = tester.renderObject(body);
          await f.pump(
            tester,
            data(opacity: 0, curve: curve, duration: duration),
          );
          if (duration > 1) {
            await tester.pump(Duration(microseconds: duration ~/ 2));
            final fade = tester.widget<SliverFadeTransition>(
              find.byType(SliverFadeTransition, skipOffstage: false),
            );
            expect(
              fade.opacity.value,
              closeTo(
                1 -
                    tester
                        .widget<SliverAnimatedOpacity>(
                          find.byType(SliverAnimatedOpacity),
                        )
                        .curve
                        .transform(.5),
                1e-9,
              ),
            );
          }
          await tester.pump(Duration(microseconds: duration + 1));
          expect(
            tester
                .widget<SliverFadeTransition>(
                  find.byType(SliverFadeTransition, skipOffstage: false),
                )
                .opacity
                .value,
            0,
          );
          expect(identical(initial, tester.renderObject(body)), isTrue);
          await f.pump(
            tester,
            data(opacity: 1, curve: curve, duration: duration),
          );
          await tester.pump(Duration(microseconds: duration + 1));
          expect(
            tester
                .widget<SliverFadeTransition>(
                  find.byType(SliverFadeTransition, skipOffstage: false),
                )
                .opacity
                .value,
            1,
          );
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
  testWidgets(
    'empty slot accepts only slivers and invisible children remain selectable',
    (tester) async {
      CanvasDropResolver? drop;
      await f.pump(tester, data(empty: true), drop: (r) => drop = r);
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-${f.fillId}'),
      );
      expect(handle, findsOneWidget);
      final point = tester.getCenter(handle);
      final target = drop!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'test',
          widgetType: 'flutter.widgets.SliverToBoxAdapter',
          traits: {canvasSliverWidgetTrait},
        ),
      );
      expect(target?.parentWidgetId, f.fillId);
      expect(target?.slotName, 'sliver');
      final selected = <String>[];
      await f.pump(tester, data(opacity: 0), selected: selected.add);
      await tester.tapAt(
        tester
                .getRect(
                  find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
                )
                .bottomRight -
            const Offset(5, 5),
      );
      expect(selected, isNotEmpty);
      expect(tester.takeException(), isNull);
    },
  );
}
