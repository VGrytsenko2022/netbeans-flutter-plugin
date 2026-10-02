import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_animated_opacity_test.dart' as b;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_rotation_test.dart' as rotation;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.SizeTransition';
Map<String, Object?> data({
  String? axis,
  double factor = 1,
  double? cross,
  String alignment = 'omit',
  double legacy = -1,
  bool rtl = false,
  bool tight = false,
  bool empty = false,
}) {
  final raw = b.data(rtl: rtl, tight: tight, empty: empty),
      node = a.builder(raw);
  node['type'] = type;
  node['properties'] = <String, Object?>{
    if (axis != null) 'axis': {'kind': 'enum', 'type': 'Axis', 'value': axis},
    'sizeFactor': f.number(factor),
    if (cross != null) 'fixedCrossAxisSizeFactor': f.number(cross),
    if (alignment == 'physical' || alignment == 'directional')
      'alignment': rotation.alignment(
        2,
        -1,
        directional: alignment == 'directional',
      ),
    if (alignment == 'legacy') 'axisAlignment': f.number(legacy),
    if (alignment == 'null') ...{
      'alignment': {'kind': 'null'},
      'axisAlignment': {'kind': 'null'},
      'fixedCrossAxisSizeFactor': {'kind': 'null'},
    },
  };
  raw['root'] = f.node(
    '30a173c1-31e1-457a-8446-5d532ea6aff1',
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

Finder native() => find
    .descendant(
      of: find.byKey(const ValueKey('canvas-widget-${a.builderId}')),
      matching: find.byType(SizeTransition),
    )
    .first;
Finder inner() =>
    find.descendant(of: native(), matching: find.byType(Align)).first;
Finder body() => find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
void main() {
  test('closed scalar wire domains and alignment strategy conflicts', () {
    for (final field in [
      'sizeFactor',
      'axisAlignment',
      'fixedCrossAxisSizeFactor',
    ]) {
      for (final bad in [
        {'kind': 'boolean', 'value': true},
        {'kind': 'string', 'value': '1'},
        {'kind': 'integer', 'value': 9007199254740992},
        {'kind': 'integer', 'value': -9007199254740992},
        {'kind': 'double', 'value': 'NaN'},
        {'kind': 'double', 'value': 'Infinity'},
        {'kind': 'dartObjectReferencePresence', 'root': 'leak'},
      ]) {
        final raw = data();
        (a.builder(raw)['properties'] as Map)[field] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
    }
    for (final value in ['1e999', '-1e999']) {
      final bytes = Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            data(factor: 9.87654321),
          ).replaceFirst('9.87654321', value),
        ),
      );
      expect(() => CanvasModel.decode(bytes), throwsFormatException);
    }
    for (final field in ['sizeFactor', 'axis']) {
      final raw = data();
      (a.builder(raw)['properties'] as Map)[field] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException);
    }
    final raw = data(), p = a.builder(raw)['properties'] as Map;
    p.remove('sizeFactor');
    expect(() => f.decode(raw), throwsFormatException);
    p['sizeFactor'] = f.number(-1);
    for (final bad in [-.5, -1.0]) {
      p['fixedCrossAxisSizeFactor'] = f.number(bad);
      expect(() => f.decode(raw), throwsFormatException);
    }
    p.remove('fixedCrossAxisSizeFactor');
    for (final name in [
      'durationUs',
      'curve',
      'onEnd',
      'filterQuality',
      'clipBehavior',
      'animation',
    ]) {
      p[name] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException);
      p.remove(name);
    }
    for (final legacy in [
      f.number(0),
      {'kind': 'dartObjectReferencePresence'},
      {'kind': 'null'},
    ]) {
      for (final modern in [
        rotation.alignment(0, 0),
        {'kind': 'dartObjectReferencePresence'},
        {'kind': 'null'},
      ]) {
        p['axisAlignment'] = legacy;
        p['alignment'] = modern;
        expect(
          () => f.decode(raw),
          legacy['kind'] == 'null' || modern['kind'] == 'null'
              ? returnsNormally
              : throwsFormatException,
        );
      }
    }
  });
  for (final axis in ['horizontal', 'vertical']) {
    for (final rtl in [false, true]) {
      for (final factor in [-1.0, 0.0, .5, 1.0, 2.0]) {
        for (final cross in <double?>[null, 0, .5, 2]) {
          for (final strategy in [
            'omit',
            'physical',
            'directional',
            'legacy',
          ]) {
            for (final tight in [false, true]) {
              testWidgets(
                'native clipped layout $axis rtl=$rtl factor=$factor cross=$cross strategy=$strategy tight=$tight',
                (tester) async {
                  await f.pump(
                    tester,
                    data(
                      axis: axis,
                      rtl: rtl,
                      factor: factor,
                      cross: cross,
                      alignment: strategy,
                      tight: tight,
                    ),
                  );
                  final n = tester.widget<SizeTransition>(native()),
                      align = tester.widget<Align>(inner());
                  expect(n.sizeFactor, isA<AlwaysStoppedAnimation<double>>());
                  expect(n.sizeFactor.value, factor);
                  expect(
                    n.axis,
                    axis == 'horizontal' ? Axis.horizontal : Axis.vertical,
                  );
                  final expected = strategy == 'physical'
                      ? const Alignment(2, -1)
                      : strategy == 'directional'
                      ? const AlignmentDirectional(2, -1)
                      : axis == 'horizontal'
                      ? AlignmentDirectional(strategy == 'legacy' ? -1 : 0, -1)
                      : AlignmentDirectional(-1, strategy == 'legacy' ? -1 : 0);
                  expect(align.alignment, expected);
                  final maxFactor = math.max(factor, 0.0);
                  expect(
                    align.widthFactor,
                    axis == 'horizontal' ? maxFactor : cross,
                  );
                  expect(
                    align.heightFactor,
                    axis == 'vertical' ? maxFactor : cross,
                  );
                  final width = tight
                      ? 240.0
                      : axis == 'horizontal'
                      ? math.min(48 * maxFactor, 240.0)
                      : cross == null
                      ? 240.0
                      : math.min(48 * cross, 240.0);
                  final height = tight
                      ? 160.0
                      : axis == 'vertical'
                      ? math.min(48 * maxFactor, 160.0)
                      : cross == null
                      ? 160.0
                      : math.min(48 * cross, 160.0);
                  final parent = tester.renderObject<RenderBox>(native()),
                      child = tester.renderObject<RenderBox>(body());
                  expect(parent.size, Size(width, height));
                  expect(child.size, const Size(48, 48));
                  final resolved = expected.resolve(
                        rtl ? TextDirection.rtl : TextDirection.ltr,
                      ),
                      offset = child.localToGlobal(
                        Offset.zero,
                        ancestor: parent,
                      );
                  expect(
                    offset.dx,
                    closeTo((width - 48) * (resolved.x + 1) / 2, 1e-8),
                  );
                  expect(
                    offset.dy,
                    closeTo((height - 48) * (resolved.y + 1) / 2, 1e-8),
                  );
                  expect(
                    tester
                        .widget<ClipRect>(
                          find
                              .descendant(
                                of: native(),
                                matching: find.byType(ClipRect),
                              )
                              .first,
                        )
                        .clipBehavior,
                    Clip.hardEdge,
                  );
                  expect(tester.takeException(), isNull);
                },
              );
            }
          }
        }
      }
    }
  }
  testWidgets(
    'empty null defaults and collapsed node retain selection and drop target',
    (tester) async {
      CanvasDropResolver? drop;
      final selected = <String>[];
      await f.pump(
        tester,
        data(factor: 0, empty: true, alignment: 'null'),
        drop: (r) => drop = r,
        selected: selected.add,
      );
      final n = tester.widget<SizeTransition>(native());
      expect(n.axis, Axis.vertical);
      expect(n.alignment, isNull);
      expect(n.fixedCrossAxisSizeFactor, isNull);
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
    'local edits null resets axis changes and source modes preserve native State and Child',
    (tester) async {
      await f.pump(tester, data(cross: .5));
      final state = tester.state(native()), child = tester.element(body());
      for (final fields in [
        <String>[],
        ['sizeFactor'],
        ['axisAlignment'],
        ['alignment'],
        ['fixedCrossAxisSizeFactor'],
        ['sizeFactor', 'alignment', 'fixedCrossAxisSizeFactor'],
        <String>[],
      ]) {
        final raw = data(factor: .5, axis: 'horizontal', alignment: 'null');
        for (final name in fields) {
          (a.builder(raw)['properties'] as Map)[name] = {
            'kind': 'dartObjectReferencePresence',
          };
        }
        await f.pump(tester, raw);
        await tester.pumpAndSettle();
        final n = tester.widget<SizeTransition>(native());
        expect(n.sizeFactor.value, fields.contains('sizeFactor') ? 1 : .5);
        expect(n.alignment, isNull);
        expect(n.fixedCrossAxisSizeFactor, isNull);
        expect(identical(state, tester.state(native())), isTrue);
        expect(identical(child, tester.element(body())), isTrue);
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
  for (final factor in [-1.0, 0.0, .5, 1.0, 2.0]) {
    testWidgets(
      'clipping rejects invisible child coordinates at factor $factor',
      (tester) async {
        final selected = <String>[];
        final raw = data(factor: factor, cross: 1);
        f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['slots'] = {
          'child': f.single(null),
        };
        await f.pump(tester, raw, selected: selected.add);
        final parent = tester.renderObject<RenderBox>(native()),
            child = tester.renderObject<RenderBox>(body());
        if (factor > 0) {
          await tester.tapAt(
            parent.localToGlobal(
              Offset(parent.size.width / 2, parent.size.height / 2),
            ),
          );
          expect(selected, contains(f.bodyId));
        }
        selected.clear();
        await tester.tapAt(child.localToGlobal(const Offset(24, 2)));
        expect(selected.contains(f.bodyId), factor >= 1);
        expect(tester.takeException(), isNull);
      },
    );
  }
}
