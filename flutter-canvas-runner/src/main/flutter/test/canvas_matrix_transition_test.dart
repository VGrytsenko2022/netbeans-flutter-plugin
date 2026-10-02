import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_rotation_transition_test.dart' as old;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.MatrixTransition';
Matrix4 matrix(String kind) {
  final value = Matrix4.identity();
  switch (kind) {
    case 'translation':
      value.storage[12] = 20;
      value.storage[13] = -12;
    case 'scale':
      value.storage[0] = -2;
      value.storage[5] = .5;
    case 'rotation':
      value.rotateZ(math.pi / 3);
    case 'skew':
      value.storage[4] = .4;
      value.storage[1] = -.2;
    case 'perspective':
      value.storage[3] = .001;
      value.storage[7] = -.002;
      value.storage[11] = .003;
    case 'singular':
      value.storage[0] = 0;
    case 'zero':
      value.setZero();
    case 'overflow':
      value.storage[0] = 1e308;
      value.storage[12] = -1e308;
    case 'projectiveZero':
      value.storage[15] = 0;
  }
  return value;
}

Map<String, Object?> data({
  String kind = 'identity',
  double animation = 0,
  double? x,
  double y = 0,
  String? quality,
  bool empty = false,
  bool tight = false,
  bool rtl = false,
}) {
  final raw = old.data(
    turns: animation,
    x: x,
    y: y,
    quality: quality,
    empty: empty,
    tight: tight,
    rtl: rtl,
  );
  final n = a.builder(raw), p = n['properties'] as Map;
  n['type'] = type;
  p['animation'] = p.remove('turns');
  p['onTransform'] = {
    'kind': 'matrix4',
    'storage': matrix(kind).storage.toList(),
  };
  return raw;
}

Finder native() => find.byType(MatrixTransition);
Finder transform() =>
    find.descendant(of: native(), matching: find.byType(Transform)).first;
Finder body() => find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
void main() {
  test('exact required input domains and optional child drop', () {
    final raw = data(), p = a.builder(raw)['properties'] as Map;
    for (final field in ['animation', 'onTransform']) {
      final saved = p.remove(field);
      expect(() => f.decode(raw), throwsFormatException);
      for (final bad in [
        {'kind': 'null'},
        {'kind': 'string', 'value': 'raw Dart'},
        {'kind': 'callback', 'handler': '_untyped'},
        {'kind': 'dartObjectReferencePresence', 'rootSymbol': 'leak'},
      ]) {
        p[field] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
      p[field] = {'kind': 'dartObjectReferencePresence'};
      expect(() => f.decode(raw), returnsNormally);
      p[field] = saved;
    }
    for (final bad in [
      List.filled(15, 1),
      List.filled(17, 1),
      List.filled(16, double.infinity),
      List.filled(16, double.nan),
    ]) {
      p['onTransform'] = {'kind': 'matrix4', 'storage': bad};
      expect(() => f.decode(raw), throwsA(anything));
    }
    p['onTransform'] = {
      'kind': 'matrix4',
      'storage': matrix('identity').storage.toList(),
    };
    for (final bad in [-double.infinity, double.infinity, double.nan]) {
      p['animation'] = f.number(bad);
      expect(() => f.decode(raw), throwsA(anything));
    }
    p['animation'] = f.number(0);
    p['alignment'] = {
      'kind': 'alignmentGeometry',
      'basis': 'directional',
      'horizontal': 0,
      'vertical': 0,
    };
    expect(() => f.decode(raw), throwsFormatException);
    p.remove('alignment');
    for (final field in [
      'durationUs',
      'curve',
      'onEnd',
      'turns',
      'origin',
      'transformHitTests',
      'clipBehavior',
    ]) {
      p[field] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException);
      p.remove(field);
    }
    expect(canvasDropSlotsForWidgetType(type).single.slotName, 'child');
    for (final bad in [
      'flutter.widgets.Expanded',
      'flutter.widgets.Positioned',
      'flutter.widgets.SliverToBoxAdapter',
    ]) {
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: type,
          childWidgetType: bad,
        ),
        false,
      );
    }
    expect(isCanvasPaletteWrapperWidgetType(type), false);
    final slot = canvasDropSlotsForWidgetType(type).single;
    expect(
      slot.acceptance.accepts(
        CanvasPaletteDragSource(
          token: 'test',
          widgetType: 'flutter.widgets.Text',
          traits: {},
        ),
      ),
      true,
    );
    expect(
      slot.acceptance.accepts(
        CanvasPaletteDragSource(
          token: 'test',
          widgetType: 'flutter.widgets.SliverToBoxAdapter',
          traits: {canvasSliverWidgetTrait},
        ),
      ),
      false,
    );
    expect(
      canvasExistingChildWrapTargetSlot(
        parentWidgetType: type,
        slotName: 'child',
      )?.slotName,
      'child',
    );
  });
  for (final kind in [
    'identity',
    'translation',
    'scale',
    'rotation',
    'skew',
    'perspective',
  ]) {
    for (final rtl in [false, true]) {
      for (final x in [-2.0, 0.0, 1.0]) {
        for (final tight in [false, true]) {
          for (final empty in [false, true]) {
            testWidgets(
              'native matrix $kind rtl=$rtl pivot=$x tight=$tight empty=$empty',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    kind: kind,
                    animation: -2.5,
                    x: x,
                    y: .5,
                    rtl: rtl,
                    tight: tight,
                    empty: empty,
                  ),
                );
                final n = tester.widget<MatrixTransition>(native());
                expect(n.animation, isA<AlwaysStoppedAnimation<double>>());
                expect(n.animation.value, -2.5);
                final first = n.onTransform(-2.5), second = n.onTransform(100);
                expect(first.storage, matrix(kind).storage);
                expect(second.storage, first.storage);
                expect(identical(first, second), false);
                first.storage[0] = 42;
                expect(n.onTransform(0).storage, matrix(kind).storage);
                expect(n.alignment, Alignment(x, .5));
                expect(
                  tester.widget<Transform>(transform()).transformHitTests,
                  true,
                );
                final box = tester.renderObject<RenderBox>(native());
                expect(
                  box.size,
                  tight
                      ? const Size(240, 160)
                      : empty
                      ? Size.zero
                      : const Size(48, 48),
                );
                if (!empty) {
                  final child = tester.renderObject<RenderBox>(body()),
                      pivot = Alignment(x, .5).alongSize(box.size);
                  final effective = Matrix4.identity()
                    ..translateByDouble(pivot.dx, pivot.dy, 0, 1)
                    ..multiply(matrix(kind))
                    ..translateByDouble(-pivot.dx, -pivot.dy, 0, 1);
                  for (final point in [Offset.zero, const Offset(10, 20)]) {
                    final expected = MatrixUtils.transformPoint(
                          effective,
                          point,
                        ),
                        actual = child.localToGlobal(point, ancestor: box);
                    expect(actual.dx, closeTo(expected.dx, 1e-8));
                    expect(actual.dy, closeTo(expected.dy, 1e-8));
                  }
                }
                expect(tester.takeException(), isNull);
              },
            );
          }
        }
      }
    }
  }
  for (final quality in <String?>[
    null,
    'null',
    'none',
    'low',
    'medium',
    'high',
  ]) {
    testWidgets('stopped animation applies native filter $quality', (
      tester,
    ) async {
      await f.pump(tester, data(quality: quality));
      final n = tester.widget<MatrixTransition>(native());
      expect(n.animation.status, AnimationStatus.forward);
      expect(
        tester.widget<Transform>(transform()).filterQuality,
        quality == null || quality == 'null'
            ? null
            : FilterQuality.values.byName(quality),
      );
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets('source-only substitution retains local fields and state', (
    tester,
  ) async {
    await f.pump(
      tester,
      data(kind: 'translation', animation: -2.5, x: 1, y: -1),
    );
    final state = tester.state(native()), element = tester.element(body());
    for (final field in ['animation', 'onTransform', 'alignment']) {
      final raw = data(kind: 'translation', animation: -2.5, x: 1, y: -1);
      (a.builder(raw)['properties'] as Map)[field] = {
        'kind': 'dartObjectReferencePresence',
      };
      await f.pump(tester, raw);
      final n = tester.widget<MatrixTransition>(native());
      expect(n.animation.value, field == 'animation' ? 0 : -2.5);
      expect(
        n.onTransform(0).storage,
        matrix(field == 'onTransform' ? 'identity' : 'translation').storage,
      );
      expect(
        n.alignment,
        field == 'alignment' ? Alignment.center : const Alignment(1, -1),
      );
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('project-owned $field') == true,
        ),
        findsWidgets,
      );
      expect(identical(state, tester.state(native())), true);
      expect(identical(element, tester.element(body())), true);
      expect(tester.takeException(), isNull);
    }
    await f.pump(tester, data(kind: 'scale'));
    expect(identical(state, tester.state(native())), true);
    expect(identical(element, tester.element(body())), true);
  });
  testWidgets(
    'empty and zero-size matrix retains actual selection and drop target',
    (tester) async {
      final selected = <String>[];
      await f.pump(tester, data(empty: true), selected: selected.add);
      expect(tester.getSize(native()), Size.zero);
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-${a.builderId}'),
      );
      expect(handle, findsOneWidget);
      await tester.tap(handle);
      expect(selected, contains(a.builderId));
      expect(tester.takeException(), isNull);
    },
  );
  for (final kind in ['singular', 'zero', 'overflow', 'projectiveZero']) {
    testWidgets('unsafe or singular $kind retains model and recovers child', (
      tester,
    ) async {
      await f.pump(tester, data(kind: kind, x: -2, y: 1));
      await tester.pump();
      final state = tester.state(native()), element = tester.element(body());
      expect(
        tester.widget<MatrixTransition>(native()).onTransform(0).storage,
        matrix(kind).storage,
      );
      expect(tester.takeException(), isNull);
      await f.pump(tester, data(kind: 'translation'));
      await tester.pump();
      expect(identical(state, tester.state(native())), true);
      expect(identical(element, tester.element(body())), true);
      expect(tester.takeException(), isNull);
    });
  }
}
