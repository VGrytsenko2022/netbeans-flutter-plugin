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

const type = 'flutter.widgets.AnimatedAlign';
Map<String, Object?> alignment(bool directional, double x, double y) => {
  'kind': 'alignmentGeometry',
  'basis': directional ? 'directional' : 'physical',
  'horizontal': x,
  'vertical': y,
};
Map<String, Object?> data({
  double x = 0,
  double y = 0,
  bool directional = false,
  int duration = 300000,
  String? curve,
  Object? width = 'omitted',
  Object? height = 'omitted',
  bool empty = false,
  bool tight = false,
  bool rtl = false,
}) {
  final raw = o.data(empty: empty, tight: tight, rtl: rtl);
  final widget = a.builder(raw);
  widget['type'] = type;
  widget['properties'] = {
    'alignment': alignment(directional, x, y),
    'durationUs': {'kind': 'integer', 'value': duration},
    if (curve != null) 'curve': {'kind': 'string', 'value': curve},
    if (width != 'omitted')
      'widthFactor': width == null
          ? {'kind': 'null'}
          : f.number((width as num).toDouble()),
    if (height != 'omitted')
      'heightFactor': height == null
          ? {'kind': 'null'}
          : f.number((height as num).toDouble()),
  };
  raw['root'] = f.node(
    'b62f3f42-a721-422f-8cf2-6579867a5300',
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

Finder native() => find.byType(AnimatedAlign);
Finder aligned() =>
    find.descendant(of: native(), matching: find.byType(Align)).first;
void main() {
  test(
    'required typed alignment, nullable factors and exact closed duration domains',
    () {
      for (final name in ['alignment', 'durationUs']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map).remove(name);
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final name in ['alignment', 'curve', 'durationUs']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map)[name] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final name in ['widthFactor', 'heightFactor']) {
        for (final invalid in [-1, 'Infinity', 'NaN']) {
          final raw = data();
          (a.builder(raw)['properties'] as Map)[name] = {
            'kind': 'double',
            'value': invalid,
          };
          expect(() => f.decode(raw), throwsFormatException);
        }
      }
      for (final literal in ['1e999', '-1e999']) {
        final raw = data(width: 9.87654321);
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
      for (final n in ['alignment', 'curve', 'durationUs', 'onEnd']) {
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
      for (final xy in [-1.0, 0.0, 1.0, 2.5]) {
        for (final tight in [false, true]) {
          testWidgets(
            'physical/directional geometry outside unit square dir=$directional rtl=$rtl xy=$xy tight=$tight',
            (tester) async {
              await f.pump(
                tester,
                data(
                  directional: directional,
                  rtl: rtl,
                  x: xy,
                  y: xy,
                  tight: tight,
                ),
              );
              final widget = tester.widget<AnimatedAlign>(native());
              expect(
                widget.alignment,
                directional ? AlignmentDirectional(xy, xy) : Alignment(xy, xy),
              );
              expect(widget.duration, const Duration(milliseconds: 300));
              expect(widget.curve, Curves.linear);
              expect(widget.widthFactor, isNull);
              expect(widget.heightFactor, isNull);
              expect(widget.onEnd, isNull);
              final parent = tester.getRect(native()),
                  child = tester.getRect(
                    find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
                  );
              final x = directional && rtl ? -xy : xy;
              expect(
                child.left - parent.left,
                closeTo((parent.width - child.width) * (x + 1) / 2, 1e-6),
              );
              expect(
                child.top - parent.top,
                closeTo((parent.height - child.height) * (xy + 1) / 2, 1e-6),
              );
              expect(tester.takeException(), isNull);
            },
          );
        }
      }
    }
  }
  for (final width in <Object?>['omitted', null, 0, .5, 2]) {
    for (final height in <Object?>['omitted', null, 0, .5, 2]) {
      for (final empty in [false, true]) {
        testWidgets(
          'initial nullable independent factors width=$width height=$height empty=$empty',
          (tester) async {
            await f.pump(
              tester,
              data(width: width, height: height, empty: empty),
            );
            final widget = tester.widget<AnimatedAlign>(native());
            expect(widget.widthFactor, width == 'omitted' ? null : width);
            expect(widget.heightFactor, height == 'omitted' ? null : height);
            final size = tester.getSize(native());
            expect(size.width, width is num ? (empty ? 0 : 48) * width : 240);
            expect(
              size.height,
              height is num ? (empty ? 0 : 48) * height : 160,
            );
            expect(tester.takeException(), isNull);
          },
        );
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
        'native transition retains child curve=$curve duration=$duration',
        (tester) async {
          await f.pump(
            tester,
            data(x: -1, y: -1, curve: curve, duration: duration),
          );
          final child = tester.element(
            find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
          );
          final state = tester.state(native());
          await f.pump(
            tester,
            data(
              x: 1,
              y: 1,
              directional: true,
              curve: curve,
              duration: duration,
            ),
          );
          if (duration > 1) {
            await tester.pump(Duration(microseconds: duration ~/ 2));
            final progress = tester
                .widget<AnimatedAlign>(native())
                .curve
                .transform(.5);
            final expected = AlignmentGeometry.lerp(
              Alignment.topLeft,
              AlignmentDirectional.bottomEnd,
              progress,
            )!;
            expect(
              tester
                  .widget<Align>(aligned())
                  .alignment
                  .resolve(TextDirection.ltr),
              expected.resolve(TextDirection.ltr),
            );
          }
          await tester.pump(Duration(microseconds: duration + 1));
          expect(
            tester
                .widget<Align>(aligned())
                .alignment
                .resolve(TextDirection.ltr),
            Alignment.bottomRight,
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
  testWidgets(
    'custom values are inert, labeled and do not reset animation state',
    (tester) async {
      final raw = data();
      for (final field in ['alignment', 'curve', 'durationUs', 'onEnd']) {
        (a.builder(raw)['properties'] as Map)[field] = {
          'kind': 'dartObjectReferencePresence',
        };
      }
      await f.pump(tester, raw);
      final widget = tester.widget<AnimatedAlign>(native());
      expect(widget.alignment, Alignment.center);
      expect(widget.curve, Curves.linear);
      expect(widget.duration, const Duration(milliseconds: 300));
      expect(widget.onEnd, isNull);
      expect(
        find.byWidgetPredicate(
          (w) => w is Tooltip && w.message?.contains('AnimatedAlign') == true,
        ),
        findsWidgets,
      );
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'empty or zero-factor geometry retains drop and selection targets',
    (tester) async {
      CanvasDropResolver? drop;
      final selected = <String>[];
      await f.pump(
        tester,
        data(empty: true, width: 0, height: 0),
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
    },
  );
  testWidgets(
    'native factors interpolate and preserve the pinned null-reset behavior',
    (tester) async {
      await f.pump(tester, data(width: 2, height: 2, duration: 200000));
      await f.pump(tester, data(width: 4, height: 3, duration: 200000));
      await tester.pump(const Duration(milliseconds: 100));
      expect(tester.widget<Align>(aligned()).widthFactor, 3);
      expect(tester.widget<Align>(aligned()).heightFactor, 2.5);
      await tester.pump(const Duration(milliseconds: 101));
      await f.pump(tester, data(width: null, height: null, duration: 200000));
      await tester.pumpAndSettle();
      expect(tester.widget<AnimatedAlign>(native()).widthFactor, isNull);
      expect(tester.widget<Align>(aligned()).widthFactor, 4);
      expect(tester.widget<Align>(aligned()).heightFactor, 3);
      expect(tester.takeException(), isNull);
    },
  );
}
