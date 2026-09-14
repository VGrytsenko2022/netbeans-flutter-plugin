import 'dart:convert';
import 'dart:typed_data';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_animated_scale_test.dart' as b;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> data({
  double scale = 1,
  double? x,
  double y = 0,
  String? quality,
  bool empty = false,
  bool tight = false,
  bool rtl = false,
}) {
  final raw = b.data(
    scale: scale,
    x: x,
    y: y,
    quality: quality,
    empty: empty,
    tight: tight,
    rtl: rtl,
  );
  final n = a.builder(raw);
  n['type'] = 'flutter.widgets.ScaleTransition';
  (n['properties'] as Map).remove('durationUs');
  return raw;
}

Finder native() => find
    .descendant(
      of: find.byKey(const ValueKey('canvas-widget-${a.builderId}')),
      matching: find.byType(ScaleTransition),
    )
    .first;
Finder transform() =>
    find.descendant(of: native(), matching: find.byType(Transform)).first;
Finder body() => find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
void main() {
  test(
    'strict signed numeric physical alignment and source presence domains',
    () {
      for (final bad in [
        {'kind': 'null'},
        {'kind': 'boolean', 'value': false},
        {'kind': 'string', 'value': '1'},
        {'kind': 'integer', 'value': 9007199254740992},
        {'kind': 'integer', 'value': -9007199254740992},
        {'kind': 'double', 'value': 'Infinity'},
        {'kind': 'double', 'value': 'NaN'},
        {'kind': 'dartObjectReferencePresence', 'rootSymbol': '_leak'},
      ]) {
        final raw = data();
        (a.builder(raw)['properties'] as Map)['scale'] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final literal in ['1e999', '-1e999']) {
        final bytes = Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              data(scale: 9.87654321),
            ).replaceFirst('9.87654321', literal),
          ),
        );
        expect(() => CanvasModel.decode(bytes), throwsFormatException);
      }
      final raw = data(), props = a.builder(raw)['properties'] as Map;
      props.remove('scale');
      expect(() => f.decode(raw), throwsFormatException);
      for (final good in [
        f.number(-2.5),
        f.number(0),
        f.number(1e308),
        {'kind': 'integer', 'value': -9007199254740991},
        {'kind': 'dartObjectReferencePresence'},
      ]) {
        props['scale'] = good;
        expect(() => f.decode(raw), returnsNormally);
      }
      props['scale'] = f.number(1);
      for (final name in [
        'durationUs',
        'curve',
        'onEnd',
        'transformHitTests',
        'onTransform',
        'animation',
      ]) {
        props[name] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
        props.remove(name);
      }
      for (final bad in [
        b.alignment(0, 0, directional: true),
        {'kind': 'null'},
        {'kind': 'dartObjectReferencePresence', 'member': '_leak'},
      ]) {
        props['alignment'] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
      props['alignment'] = b.alignment(2, -3);
      props['filterQuality'] = {
        'kind': 'enum',
        'type': 'FilterQuality',
        'value': 'invented',
      };
      expect(() => f.decode(raw), throwsFormatException);
    },
  );
  for (final rtl in [false, true]) {
    for (final scale in [-2.5, 0.0, .5, 1.0, 2.0]) {
      for (final x in [-2.0, 0.0, 1.0]) {
        for (final tight in [false, true]) {
          for (final empty in [false, true]) {
            testWidgets(
              'native physical scale=$scale pivot=$x rtl=$rtl tight=$tight empty=$empty',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    scale: scale,
                    x: x,
                    y: .5,
                    rtl: rtl,
                    tight: tight,
                    empty: empty,
                  ),
                );
                final n = tester.widget<ScaleTransition>(native());
                expect(n.scale, isA<AlwaysStoppedAnimation<double>>());
                expect(n.scale.value, scale);
                expect(n.alignment, Alignment(x, .5));
                expect(
                  tester.widget<Transform>(transform()).transformHitTests,
                  isTrue,
                );
                final parent = tester.renderObject<RenderBox>(native());
                expect(
                  parent.size,
                  tight
                      ? const Size(240, 160)
                      : empty
                      ? Size.zero
                      : const Size(48, 48),
                );
                if (!empty) {
                  final child = tester.renderObject<RenderBox>(body()),
                      pivot = Alignment(x, .5).alongSize(parent.size),
                      origin = child.localToGlobal(
                        Offset.zero,
                        ancestor: parent,
                      );
                  expect(origin.dx, closeTo(pivot.dx * (1 - scale), 1e-8));
                  expect(origin.dy, closeTo(pivot.dy * (1 - scale), 1e-8));
                  final point =
                      child.localToGlobal(
                        const Offset(10, 20),
                        ancestor: parent,
                      ) -
                      origin;
                  expect(point.dx, closeTo(10 * scale, 1e-8));
                  expect(point.dy, closeTo(20 * scale, 1e-8));
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
    for (final scale in [-1.0, 0.0, 2.0]) {
      testWidgets(
        'constant animation reports forward and applies configured filter $quality $scale',
        (tester) async {
          await f.pump(tester, data(scale: scale, quality: quality));
          expect(
            tester.widget<ScaleTransition>(native()).filterQuality,
            quality == null || quality == 'null'
                ? null
                : FilterQuality.values.byName(quality),
          );
          expect(
            tester.widget<ScaleTransition>(native()).scale.status,
            AnimationStatus.forward,
          );
          expect(
            tester.widget<Transform>(transform()).filterQuality,
            tester.widget<ScaleTransition>(native()).filterQuality,
          );
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
  testWidgets(
    'local edits preserve State child identity and scale after resize',
    (tester) async {
      await f.pump(tester, data());
      final state = tester.state(native()), child = tester.element(body());
      for (final v in [-2.5, 0.0, .5, 2.0]) {
        await f.pump(tester, data(scale: v, x: 1, y: -1, quality: 'high'));
        expect(tester.widget<ScaleTransition>(native()).scale.value, v);
        expect(identical(state, tester.state(native())), isTrue);
        expect(identical(child, tester.element(body())), isTrue);
      }
      final raw = data(scale: .5, x: 1, y: 1);
      f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['properties'] =
          {'width': f.number(96), 'height': f.number(80)};
      await f.pump(tester, raw);
      final parent = tester.renderObject<RenderBox>(native());
      expect(parent.size, const Size(96, 80));
      expect(
        tester
            .renderObject<RenderBox>(body())
            .localToGlobal(Offset.zero, ancestor: parent),
        const Offset(48, 40),
      );
      expect(identical(state, tester.state(native())), isTrue);
      expect(tester.takeException(), isNull);
    },
  );
  for (final fields in [
    ['scale'],
    ['alignment'],
    ['scale', 'alignment'],
  ]) {
    testWidgets('project references are isolated and labeled $fields', (
      tester,
    ) async {
      final raw = data(scale: -.5, x: 2, y: -1, quality: 'high');
      for (final field in fields) {
        (a.builder(raw)['properties'] as Map)[field] = {
          'kind': 'dartObjectReferencePresence',
        };
      }
      await f.pump(tester, raw);
      final n = tester.widget<ScaleTransition>(native());
      expect(n.scale.value, fields.contains('scale') ? 1 : -.5);
      expect(
        n.alignment,
        fields.contains('alignment')
            ? Alignment.center
            : const Alignment(2, -1),
      );
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip && w.message?.contains('stopped scale of 1') == true,
        ),
        findsWidgets,
      );
      expect(
        tester.widget<Transform>(transform()).filterQuality,
        FilterQuality.high,
      );
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets('empty zero-scale node remains a selectable drop destination', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    final selected = <String>[];
    await f.pump(
      tester,
      data(empty: true, scale: 0),
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
  for (final scale in [-1.0, 0.0, .5, 1.0]) {
    testWidgets(
      'transformed child hits and zero-scale selection scale=$scale',
      (tester) async {
        final raw = data(scale: scale), selected = <String>[];
        f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['slots'] = {
          'child': f.single(null),
        };
        await f.pump(tester, raw, selected: selected.add);
        await tester.tapAt(tester.getCenter(body()));
        expect(selected.contains(f.bodyId), scale != 0);
        if (scale == 0) {
          expect(selected, contains(a.builderId));
        }
        expect(tester.takeException(), isNull);
      },
    );
  }
}
