import 'dart:convert';
import 'dart:typed_data';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_opacity_test.dart' as o;

const type = 'flutter.widgets.AnimatedPadding';
Map<String, Object?> padding(
  bool directional,
  double first,
  double top,
  double last,
  double bottom,
) => {
  'kind': directional ? 'edgeInsetsDirectional' : 'edgeInsets',
  directional ? 'start' : 'left': first,
  'top': top,
  directional ? 'end' : 'right': last,
  'bottom': bottom,
};
Map<String, Object?> data({
  double first = 16,
  double top = 16,
  double last = 16,
  double bottom = 16,
  bool directional = false,
  int duration = 300000,
  String? curve,
  bool empty = false,
  bool tight = false,
  bool rtl = false,
}) {
  final raw = o.data(empty: empty, tight: tight, rtl: rtl);
  final widget = a.builder(raw);
  widget['type'] = type;
  widget['properties'] = {
    'padding': padding(directional, first, top, last, bottom),
    'durationUs': {'kind': 'integer', 'value': duration},
    if (curve != null) 'curve': {'kind': 'string', 'value': curve},
  };
  raw['root'] = f.node(
    'dc4f27ce-c562-4d13-96fb-90b3f89a0a18',
    'flutter.widgets.Directionality',
    {
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': rtl ? 'rtl' : 'ltr',
      },
    },
    {'child': f.single(raw['root'] as Map<String, Object?>)},
  );
  return raw;
}

Finder native() => find.byType(AnimatedPadding);
Finder padded() =>
    find.descendant(of: native(), matching: find.byType(Padding)).first;
void main() {
  test(
    'closed required nonnegative geometry duration and safe presence wire',
    () {
      for (final name in ['padding', 'durationUs']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map).remove(name);
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final name in ['padding', 'curve', 'durationUs']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map)[name] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final directional in [false, true]) {
        for (final side in [
          directional ? 'start' : 'left',
          'top',
          directional ? 'end' : 'right',
          'bottom',
        ]) {
          for (final invalid in [-1, 'Infinity', 'NaN']) {
            final raw = data(directional: directional);
            ((a.builder(raw)['properties'] as Map)['padding'] as Map)[side] =
                invalid;
            expect(() => f.decode(raw), throwsFormatException);
          }
        }
      }
      for (final literal in ['1e999', '-1e999']) {
        final raw = data(first: 9.87654321);
        final bytes = Uint8List.fromList(
          utf8.encode(jsonEncode(raw).replaceFirst('9.87654321', literal)),
        );
        expect(() => CanvasModel.decode(bytes), throwsFormatException);
      }
      for (final us in [-1, 9007199254740992]) {
        expect(() => f.decode(data(duration: us)), throwsFormatException);
      }
      for (final us in [0, 1, 9007199254740991]) {
        expect(() => f.decode(data(duration: us)), returnsNormally);
      }
      expect(() => f.decode(data(curve: 'invented')), throwsFormatException);
      for (final n in ['padding', 'curve', 'durationUs', 'onEnd']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map)[n] = {
          'kind': 'dartObjectReferencePresence',
          'rootSymbol': 'mustNotCrossWire',
        };
        expect(() => f.decode(raw), throwsFormatException);
      }
      final raw = data();
      (a.builder(raw)['properties'] as Map)['onEnd'] = {'kind': 'null'};
      expect(() => f.decode(raw), returnsNormally);
      for (final slot in ['sliver', 'children']) {
        final raw = data();
        a.builder(raw)['slots'] = {slot: f.single(null)};
        expect(() => f.decode(raw), throwsFormatException);
      }
    },
  );
  for (final directional in [false, true]) {
    for (final rtl in [false, true]) {
      for (final first in [0.0, .5, 16.0, 32.0]) {
        for (final tight in [false, true]) {
          for (final empty in [false, true]) {
            testWidgets(
              'independent sides dir=$directional rtl=$rtl first=$first tight=$tight empty=$empty',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    directional: directional,
                    rtl: rtl,
                    first: first,
                    top: 2,
                    last: 10,
                    bottom: 4,
                    tight: tight,
                    empty: empty,
                  ),
                );
                final widget = tester.widget<AnimatedPadding>(native());
                expect(
                  widget.padding,
                  directional
                      ? EdgeInsetsDirectional.fromSTEB(first, 2, 10, 4)
                      : EdgeInsets.fromLTRB(first, 2, 10, 4),
                );
                expect(widget.duration, const Duration(milliseconds: 300));
                expect(widget.curve, Curves.linear);
                expect(widget.onEnd, isNull);
                final parentBox = tester.renderObject<RenderBox>(native());
                final parent = Offset.zero & parentBox.size;
                expect(
                  parent.size,
                  tight
                      ? const Size(240, 160)
                      : Size(
                          (empty ? 0 : 48) + first + 10,
                          (empty ? 0 : 48) + 6,
                        ),
                );
                if (!empty) {
                  final childBox = tester.renderObject<RenderBox>(
                    find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
                  );
                  final child =
                      childBox.localToGlobal(Offset.zero, ancestor: parentBox) &
                      childBox.size;
                  expect(
                    child.left - parent.left,
                    directional && rtl ? 10 : first,
                  );
                  expect(
                    parent.right - child.right,
                    directional && rtl ? first : 10,
                  );
                  expect(child.top - parent.top, 2);
                  expect(parent.bottom - child.bottom, 4);
                }
                expect(tester.takeException(), isNull);
              },
            );
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
      for (final rtl in [false, true]) {
        testWidgets(
          'native mixed geometry clamps overshooting curve=$curve duration=$duration rtl=$rtl',
          (tester) async {
            await f.pump(
              tester,
              data(
                first: 0,
                top: 0,
                last: 0,
                bottom: 0,
                curve: curve,
                duration: duration,
                rtl: rtl,
              ),
            );
            final child = tester.element(
                  find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
                ),
                state = tester.state(native());
            await f.pump(
              tester,
              data(
                first: 24,
                top: 12,
                last: 8,
                bottom: 4,
                directional: true,
                curve: curve,
                duration: duration,
                rtl: rtl,
              ),
            );
            if (duration > 1) {
              await tester.pump(Duration(microseconds: duration ~/ 4));
              final progress = tester
                  .widget<AnimatedPadding>(native())
                  .curve
                  .transform(.25);
              final expected = EdgeInsetsGeometry.lerp(
                EdgeInsets.zero,
                const EdgeInsetsDirectional.fromSTEB(24, 12, 8, 4),
                progress,
              )!.clamp(EdgeInsets.zero, EdgeInsetsGeometry.infinity);
              expect(
                tester
                    .widget<Padding>(padded())
                    .padding
                    .resolve(rtl ? TextDirection.rtl : TextDirection.ltr),
                expected.resolve(rtl ? TextDirection.rtl : TextDirection.ltr),
              );
            }
            await tester.pump(Duration(microseconds: duration + 1));
            expect(
              tester
                  .widget<Padding>(padded())
                  .padding
                  .resolve(rtl ? TextDirection.rtl : TextDirection.ltr),
              rtl
                  ? const EdgeInsets.fromLTRB(8, 12, 24, 4)
                  : const EdgeInsets.fromLTRB(24, 12, 8, 4),
            );
            expect(
              identical(
                child,
                tester.element(
                  find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
                ),
              ),
              isTrue,
            );
            expect(identical(state, tester.state(native())), isTrue);
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  testWidgets(
    'inert custom values are labeled and use the documented fallback',
    (tester) async {
      final raw = data();
      for (final field in ['padding', 'curve', 'durationUs', 'onEnd']) {
        (a.builder(raw)['properties'] as Map)[field] = {
          'kind': 'dartObjectReferencePresence',
        };
      }
      await f.pump(tester, raw);
      final widget = tester.widget<AnimatedPadding>(native());
      expect(widget.padding, const EdgeInsets.all(16));
      expect(widget.curve, Curves.linear);
      expect(widget.duration, const Duration(milliseconds: 300));
      expect(widget.onEnd, isNull);
      expect(
        find.byWidgetPredicate(
          (w) => w is Tooltip && w.message?.contains('AnimatedPadding') == true,
        ),
        findsWidgets,
      );
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets('zero padding empty child retains drop and selection target', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    final selected = <String>[];
    await f.pump(
      tester,
      data(empty: true, first: 0, top: 0, last: 0, bottom: 0),
      drop: (r) => drop = r,
      selected: selected.add,
    );
    final handle = find.byKey(
      const ValueKey('canvas-zero-size-widget-target-${a.builderId}'),
    );
    expect(handle, findsOneWidget);
    final point = tester.getCenter(handle),
        surface = tester.getRect(find.byType(CanvasDocumentView));
    final target = drop!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(
        token: 'test',
        widgetType: 'flutter.widgets.Text',
        traits: {},
      ),
    );
    expect(target?.parentWidgetId, a.builderId);
    expect(target?.slotName, 'child');
    await tester.tapAt(point);
    expect(selected, contains(a.builderId));
    expect(tester.takeException(), isNull);
  });
}
