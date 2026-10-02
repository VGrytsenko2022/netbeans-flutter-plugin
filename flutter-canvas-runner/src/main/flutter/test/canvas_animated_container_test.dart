import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_opacity_test.dart' as o;
import 'canvas_animated_rotation_test.dart' as r;

const type = 'flutter.widgets.AnimatedContainer';
Map<String, Object?> insets(double n, {bool rtl = false}) => {
  'kind': rtl ? 'edgeInsetsDirectional' : 'edgeInsets',
  if (rtl) 'start': n else 'left': n,
  'top': n,
  if (rtl) 'end': n else 'right': n,
  'bottom': n,
};
Map<String, Object?> decoration(int color) => {
  'kind': 'boxDecoration',
  'color': {
    'kind': 'literal',
    'argb': '0x${color.toRadixString(16).padLeft(8, '0').toUpperCase()}',
  },
  'image': null,
  'border': null,
  'borderRadius': null,
  'boxShadow': <Object?>[],
  'gradient': null,
  'backgroundBlendMode': null,
  'shape': 'rectangle',
};
Map<String, Object?> matrix(double x) => {
  'kind': 'matrix4',
  'storage': [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, x, 0, 0, 1],
};
Map<String, Object?> constraints(double n) => {
  'kind': 'boxConstraints',
  'minWidth': n,
  'maxWidth': n + 20,
  'minHeight': n,
  'maxHeight': n + 20,
};
Map<String, Object?> data({
  Map<String, Object?> properties = const {},
  bool empty = false,
  bool tight = false,
  bool rtl = false,
}) {
  final raw = o.data(empty: empty, tight: tight, rtl: rtl);
  final node = a.builder(raw);
  node['type'] = type;
  node['properties'] = <String, Object?>{
    'durationUs': {'kind': 'integer', 'value': 100000},
    ...properties,
  };
  raw['root'] = f.node(
    'ee156af3-65da-4f78-82a9-42f796c77726',
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

Finder native() => find.byType(AnimatedContainer);
Container inner(WidgetTester tester) => tester.widget<Container>(
  find.descendant(of: native(), matching: find.byType(Container)).first,
);
void main() {
  testWidgets(
    'singular Matrix4 native tween is isolated and recovers after reset',
    (tester) async {
      final zero = <double>[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1];
      await f.pump(
        tester,
        data(
          properties: {
            'transform': {'kind': 'matrix4', 'storage': zero},
          },
        ),
      );
      await f.pump(tester, data(properties: {'transform': matrix(2)}));
      await tester.pump(const Duration(milliseconds: 50));
      await tester.pump();
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              (w.message?.contains('native transform matrix is non-finite') ??
                  false),
        ),
        findsWidgets,
      );
      final state = tester.state(native());
      await f.pump(
        tester,
        data(
          properties: {
            'transform': {'kind': 'null'},
          },
        ),
      );
      await tester.pump();
      await f.pump(tester, data(properties: {'transform': matrix(2)}));
      await tester.pump();
      expect(identical(state, tester.state(native())), isTrue);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              (w.message?.contains('native transform matrix is non-finite') ??
                  false),
        ),
        findsNothing,
      );
      expect(tester.takeException(), isNull);
    },
  );

  test(
    'closed domains, nullable references, background dependency and all 15 rows',
    () {
      for (final field in [
        'alignment',
        'padding',
        'color',
        'decoration',
        'foregroundDecoration',
        'width',
        'height',
        'constraints',
        'margin',
        'transform',
        'transformAlignment',
        'onEnd',
      ]) {
        expect(
          () => f.decode(
            data(
              properties: {
                field: {'kind': 'null'},
              },
            ),
          ),
          returnsNormally,
        );
        expect(
          () => f.decode(
            data(
              properties: {
                field: {'kind': 'dartObjectReferencePresence'},
              },
            ),
          ),
          returnsNormally,
        );
        expect(
          () => f.decode(
            data(
              properties: {
                field: {
                  'kind': 'dartObjectReferencePresence',
                  'rootSymbol': 'leak',
                },
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final field in ['durationUs', 'curve', 'clipBehavior']) {
        expect(
          () => f.decode(
            data(
              properties: {
                field: {'kind': 'null'},
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final field in ['width', 'height']) {
        expect(
          () => f.decode(data(properties: {field: f.number(-1)})),
          throwsFormatException,
        );
        expect(
          () => f.decode(
            data(
              properties: {
                field: {'kind': 'enum', 'type': 'double', 'value': 'infinity'},
              },
            ),
          ),
          returnsNormally,
        );
        expect(
          () => f.decode(
            data(
              properties: {
                field: {
                  'kind': 'enum',
                  'type': 'double',
                  'value': 'negativeInfinity',
                },
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final field in ['padding', 'margin']) {
        expect(
          () => f.decode(data(properties: {field: insets(-1)})),
          throwsFormatException,
        );
      }
      expect(
        () => f.decode(
          data(
            properties: {
              'isAntiAlias': {'kind': 'boolean', 'value': true},
            },
          ),
        ),
        throwsFormatException,
      );
      final clip = {'kind': 'enum', 'type': 'Clip', 'value': 'hardEdge'};
      expect(
        () => f.decode(data(properties: {'clipBehavior': clip})),
        throwsFormatException,
      );
      expect(
        () => f.decode(
          data(
            properties: {
              'clipBehavior': clip,
              'foregroundDecoration': decoration(0xff000000),
            },
          ),
        ),
        throwsFormatException,
      );
      expect(
        () => f.decode(
          data(
            properties: {
              'clipBehavior': clip,
              'color': {'kind': 'color', 'argb': '0xFF123456'},
              'decoration': {'kind': 'null'},
            },
          ),
        ),
        returnsNormally,
      );
      expect(
        () => f.decode(
          data(
            properties: {
              'color': {'kind': 'color', 'argb': '0xFF123456'},
              'decoration': decoration(0xff000000),
            },
          ),
        ),
        throwsFormatException,
      );
    },
  );
  for (final rtl in [false, true]) {
    for (final empty in [false, true]) {
      for (final tight in [false, true]) {
        testWidgets(
          'native local constructor rtl=$rtl empty=$empty tight=$tight',
          (tester) async {
            await f.pump(
              tester,
              data(
                rtl: rtl,
                empty: empty,
                tight: tight,
                properties: {
                  'alignment': r.alignment(.5, -.5, directional: rtl),
                  'padding': insets(3, rtl: rtl),
                  'margin': insets(2, rtl: rtl),
                  'color': {'kind': 'color', 'argb': '0xFF123456'},
                  'foregroundDecoration': decoration(0x11000000),
                  'width': f.number(100),
                  'height': f.number(90),
                  'constraints': constraints(60),
                  'transform': matrix(4),
                  'transformAlignment': r.alignment(-1, 1, directional: rtl),
                  'clipBehavior': {
                    'kind': 'enum',
                    'type': 'Clip',
                    'value': 'antiAlias',
                  },
                },
              ),
            );
            final n = tester.widget<AnimatedContainer>(native());
            expect(
              n.constraints,
              const BoxConstraints.tightFor(width: 80, height: 80),
            );
            expect(
              n.padding,
              rtl
                  ? const EdgeInsetsDirectional.all(3)
                  : const EdgeInsets.all(3),
            );
            expect(
              (n.decoration as BoxDecoration).color,
              const Color(0xff123456),
            );
            expect(n.transform!.storage[12], 4);
            expect(n.clipBehavior, Clip.antiAlias);
            expect(n.child == null, empty);
            expect(n.duration, const Duration(milliseconds: 100));
            expect(n.onEnd, isNull);
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
    testWidgets(
      'all eight native tweens $curve preserve interpolation and interruption',
      (tester) async {
        Map<String, Object?> props(double t) => {
          'alignment': r.alignment(t / 10, 0),
          'padding': insets(t),
          'margin': insets(t),
          'decoration': decoration(t == 10 ? 0xff000000 : 0xffffffff),
          'foregroundDecoration': decoration(t == 10 ? 0xff000000 : 0xff888888),
          'constraints': constraints(t + 40),
          'transform': matrix(t),
          'transformAlignment': r.alignment(0, t / 10),
          'curve': {'kind': 'string', 'value': curve},
        };
        await f.pump(tester, data(properties: props(10)));
        final state = tester.state(native());
        await f.pump(tester, data(properties: props(12)));
        await tester.pump(const Duration(milliseconds: 50));
        final n = tester.widget<AnimatedContainer>(native());
        final t = n.curve.transform(.5), v = 10 + 2 * t, paint = inner(tester);
        expect(
          paint.padding!.resolve(TextDirection.ltr).left,
          closeTo(v, 1e-6),
        );
        expect(paint.margin!.resolve(TextDirection.ltr).left, closeTo(v, 1e-6));
        expect(paint.constraints!.minWidth, closeTo(50 + 2 * t, 1e-6));
        expect(paint.transform!.storage[12], closeTo(v, 1e-6));
        expect(
          paint.alignment!.resolve(TextDirection.ltr).x,
          closeTo(v / 10, 1e-6),
        );
        expect(
          paint.transformAlignment!.resolve(TextDirection.ltr).y,
          closeTo(v / 10, 1e-6),
        );
        expect(
          (paint.decoration as BoxDecoration).color,
          Color.lerp(const Color(0xff000000), const Color(0xffffffff), t),
        );
        expect(
          (paint.foregroundDecoration as BoxDecoration).color,
          Color.lerp(const Color(0xff000000), const Color(0xff888888), t),
        );
        await f.pump(tester, data(properties: props(11)));
        await tester.pump(const Duration(milliseconds: 101));
        expect(identical(state, tester.state(native())), isTrue);
        expect(inner(tester).padding!.resolve(TextDirection.ltr).left, 11);
        expect(tester.takeException(), isNull);
      },
    );
  }
  testWidgets(
    'null removes tweens immediately; zero duration and native callback lifecycle',
    (tester) async {
      var ends = 0;
      Future<void> show(
        double? size, {
        int ms = 100,
        Clip clip = Clip.none,
      }) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Center(
              child: AnimatedContainer(
                width: size,
                height: size,
                color: size == null ? null : Colors.red,
                clipBehavior: clip,
                padding: size == null ? null : const EdgeInsets.all(2),
                duration: Duration(milliseconds: ms),
                onEnd: () => ends++,
                child: const SizedBox(width: 20, height: 20),
              ),
            ),
          ),
        );
        await tester.pump();
      }

      await show(60);
      expect(ends, 0);
      await show(80);
      await tester.pump(const Duration(milliseconds: 101));
      expect(ends, 1);
      await show(80, clip: Clip.hardEdge);
      await tester.pump(const Duration(milliseconds: 101));
      expect(ends, 1);
      await show(null);
      expect(inner(tester).padding, isNull);
      expect(inner(tester).constraints, isNull);
      expect(inner(tester).decoration, isNull);
      await show(100, ms: 0);
      expect(inner(tester).constraints!.minWidth, 100);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'nullable custom geometry and decorations never execute in Canvas',
    (tester) async {
      final properties = <String, Object?>{};
      for (final field in [
        'alignment',
        'padding',
        'color',
        'foregroundDecoration',
        'width',
        'height',
        'constraints',
        'margin',
        'transform',
        'transformAlignment',
        'durationUs',
        'curve',
        'onEnd',
      ]) {
        properties[field] = {'kind': 'dartObjectReferencePresence'};
      }
      properties['clipBehavior'] = {
        'kind': 'enum',
        'type': 'Clip',
        'value': 'hardEdge',
      };
      await f.pump(tester, data(properties: properties));
      final n = tester.widget<AnimatedContainer>(native());
      expect(n.constraints, isNull);
      expect(n.decoration, isNull);
      expect(n.transform, isNull);
      expect(n.clipBehavior, Clip.none);
      expect(n.duration, const Duration(milliseconds: 300));
      expect(n.curve, Curves.linear);
      expect(n.onEnd, isNull);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip && (w.message?.contains('project-owned') ?? false),
        ),
        findsWidgets,
      );
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'infinity under bounded parent and optional child drop geometry',
    (tester) async {
      final raw = data(
        empty: true,
        properties: {
          'width': {'kind': 'enum', 'type': 'double', 'value': 'infinity'},
          'height': f.number(20),
        },
      );
      await f.pump(tester, raw);
      expect(
        tester.widget<AnimatedContainer>(native()).constraints!.maxWidth,
        double.infinity,
      );
      expect(canvasDropSlotsForWidgetType(type), hasLength(1));
      expect(f.decode(raw), isA<CanvasModel>());
      expect(tester.takeException(), isNull);
    },
  );
}
