import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_animated_rotation_test.dart' as b;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> data({
  double turns = 0,
  double? x,
  double y = 0,
  String? quality,
  bool empty = false,
  bool tight = false,
  bool rtl = false,
}) {
  final raw = b.data(
    turns: turns,
    x: x,
    y: y,
    quality: quality,
    empty: empty,
    tight: tight,
    rtl: rtl,
  );
  final n = a.builder(raw);
  n['type'] = 'flutter.widgets.RotationTransition';
  (n['properties'] as Map).remove('durationUs');
  return raw;
}

Finder native() => find
    .descendant(
      of: find.byKey(const ValueKey('canvas-widget-${a.builderId}')),
      matching: find.byType(RotationTransition),
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
        (a.builder(raw)['properties'] as Map)['turns'] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final literal in ['1e999', '-1e999']) {
        final bytes = Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              data(turns: 9.87654321),
            ).replaceFirst('9.87654321', literal),
          ),
        );
        expect(() => CanvasModel.decode(bytes), throwsFormatException);
      }
      final raw = data(), props = a.builder(raw)['properties'] as Map;
      props.remove('turns');
      expect(() => f.decode(raw), throwsFormatException);
      for (final good in [
        f.number(-2.5),
        f.number(0),
        f.number(1e308),
        {'kind': 'integer', 'value': -9007199254740991},
        {'kind': 'dartObjectReferencePresence'},
      ]) {
        props['turns'] = good;
        expect(() => f.decode(raw), returnsNormally);
      }
      props['turns'] = f.number(1);
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
    for (final turns in [-2.5, -.25, 0.0, .125, .25, .5, 1.0, 2.0]) {
      for (final x in [-2.0, 0.0, 1.0]) {
        for (final tight in [false, true]) {
          for (final empty in [false, true]) {
            testWidgets(
              'native physical turns=$turns pivot=$x rtl=$rtl tight=$tight empty=$empty',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    turns: turns,
                    x: x,
                    y: .5,
                    rtl: rtl,
                    tight: tight,
                    empty: empty,
                  ),
                );
                final n = tester.widget<RotationTransition>(native());
                expect(n.turns, isA<AlwaysStoppedAnimation<double>>());
                expect(n.turns.value, turns);
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
                  final angle = turns * math.pi * 2,
                      c = math.cos(angle),
                      s = math.sin(angle);
                  expect(
                    origin.dx,
                    closeTo(pivot.dx * (1 - c) + pivot.dy * s, 1e-8),
                  );
                  expect(
                    origin.dy,
                    closeTo(pivot.dy * (1 - c) - pivot.dx * s, 1e-8),
                  );
                  final point =
                      child.localToGlobal(
                        const Offset(10, 20),
                        ancestor: parent,
                      ) -
                      origin;
                  expect(point.dx, closeTo(10 * c - 20 * s, 1e-8));
                  expect(point.dy, closeTo(10 * s + 20 * c, 1e-8));
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
    for (final turns in [-1.0, 0.0, 2.0]) {
      testWidgets(
        'constant animation reports forward and applies configured filter $quality $turns',
        (tester) async {
          await f.pump(tester, data(turns: turns, quality: quality));
          expect(
            tester.widget<RotationTransition>(native()).filterQuality,
            quality == null || quality == 'null'
                ? null
                : FilterQuality.values.byName(quality),
          );
          expect(
            tester.widget<RotationTransition>(native()).turns.status,
            AnimationStatus.forward,
          );
          expect(
            tester.widget<Transform>(transform()).filterQuality,
            tester.widget<RotationTransition>(native()).filterQuality,
          );
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
  testWidgets(
    'local edits preserve State child identity and turns after resize',
    (tester) async {
      await f.pump(tester, data());
      final state = tester.state(native()), child = tester.element(body());
      for (final v in [-2.5, 0.0, .5, 2.0]) {
        await f.pump(tester, data(turns: v, x: 1, y: -1, quality: 'high'));
        expect(tester.widget<RotationTransition>(native()).turns.value, v);
        expect(identical(state, tester.state(native())), isTrue);
        expect(identical(child, tester.element(body())), isTrue);
      }
      final raw = data(turns: .5, x: 1, y: 1);
      f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['properties'] =
          {'width': f.number(96), 'height': f.number(80)};
      await f.pump(tester, raw);
      final parent = tester.renderObject<RenderBox>(native());
      expect(parent.size, const Size(96, 80));
      expect(
        tester
            .renderObject<RenderBox>(body())
            .localToGlobal(Offset.zero, ancestor: parent),
        const Offset(192, 160),
      );
      expect(identical(state, tester.state(native())), isTrue);
      expect(tester.takeException(), isNull);
    },
  );
  for (final fields in [
    ['turns'],
    ['alignment'],
    ['turns', 'alignment'],
  ]) {
    testWidgets('project references are isolated and labeled $fields', (
      tester,
    ) async {
      final raw = data(turns: -.5, x: 2, y: -1, quality: 'high');
      for (final field in fields) {
        (a.builder(raw)['properties'] as Map)[field] = {
          'kind': 'dartObjectReferencePresence',
        };
      }
      await f.pump(tester, raw);
      final n = tester.widget<RotationTransition>(native());
      expect(n.turns.value, fields.contains('turns') ? 0 : -.5);
      expect(
        n.alignment,
        fields.contains('alignment')
            ? Alignment.center
            : const Alignment(2, -1),
      );
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('stopped rotation of 0 turns') == true,
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
  testWidgets('empty node remains a selectable drop destination', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    final selected = <String>[];
    await f.pump(
      tester,
      data(empty: true, turns: 0),
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
  for (final turns in [-1.0, -.25, 0.0, .125, .25, .5, 1.0]) {
    testWidgets('transformed child hits including zero identity turns=$turns', (
      tester,
    ) async {
      final raw = data(turns: turns), selected = <String>[];
      f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['slots'] = {
        'child': f.single(null),
      };
      await f.pump(tester, raw, selected: selected.add);
      await tester.tapAt(tester.getCenter(body()));
      expect(selected, contains(f.bodyId));
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets(
    'local and inert source mode changes retain State and restore geometry',
    (tester) async {
      await f.pump(tester, data(turns: .25, x: 1));
      final state = tester.state(native()), child = tester.element(body());
      for (final fields in [
        ['turns'],
        ['alignment'],
        ['turns', 'alignment'],
        <String>[],
      ]) {
        final raw = data(turns: .25, x: 1, quality: 'medium');
        for (final name in fields) {
          (a.builder(raw)['properties'] as Map)[name] = {
            'kind': 'dartObjectReferencePresence',
          };
        }
        await f.pump(tester, raw);
        await tester.pumpAndSettle();
        expect(identical(state, tester.state(native())), isTrue);
        expect(identical(child, tester.element(body())), isTrue);
        expect(
          tester.widget<RotationTransition>(native()).turns.value,
          fields.contains('turns') ? 0 : .25,
        );
        expect(
          tester.widget<RotationTransition>(native()).alignment,
          fields.contains('alignment')
              ? Alignment.center
              : Alignment.centerRight,
        );
        expect(
          tester.widget<Transform>(transform()).filterQuality,
          FilterQuality.medium,
        );
        expect(
          find.byWidgetPredicate(
            (w) => w is Tooltip && w.message?.contains('project-owned') == true,
          ),
          fields.isEmpty ? findsNothing : findsWidgets,
        );
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'extreme finite turns and physical pivots are quarantined and recover',
    (tester) async {
      await f.pump(tester, data());
      final state = tester.state(native()),
          element = tester.element(body()),
          size = tester.getSize(native());
      for (final v in [1e307, -1e307, 1e308, -1e308, 0.0, .25]) {
        await f.pump(tester, data(turns: v));
        await tester.pumpAndSettle();
        expect(tester.widget<RotationTransition>(native()).turns.value, v);
        final warning = find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('native rotation matrix is non-finite') ==
                  true,
        );
        expect(
          warning,
          (v * math.pi * 2).isFinite ? findsNothing : findsWidgets,
        );
        expect(identical(state, tester.state(native())), isTrue);
        expect(identical(element, tester.element(body())), isTrue);
        expect(tester.getSize(native()), size);
        expect(tester.takeException(), isNull);
      }
      await f.pump(tester, data(turns: .25, x: 1e308, y: 1e308));
      await tester.pumpAndSettle();
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('native rotation matrix is non-finite') ==
                  true,
        ),
        findsWidgets,
      );
      expect(tester.takeException(), isNull);
      await f.pump(tester, data(turns: 0));
      await tester.pumpAndSettle();
      expect(identical(state, tester.state(native())), isTrue);
      expect(identical(element, tester.element(body())), isTrue);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('native rotation matrix is non-finite') ==
                  true,
        ),
        findsNothing,
      );
      expect(tester.takeException(), isNull);
    },
  );
}
