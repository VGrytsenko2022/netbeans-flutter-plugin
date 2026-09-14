import 'dart:convert';
import 'dart:typed_data';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_animated_slide_test.dart' as b;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> data({
  double dx = 0,
  double dy = 0,
  bool? hit,
  String direction = 'unset',
  bool rtl = false,
  bool empty = false,
  bool tight = false,
}) {
  final raw = b.data(dx: dx, dy: dy, rtl: rtl, empty: empty, tight: tight);
  final node = a.builder(raw);
  node['type'] = 'flutter.widgets.SlideTransition';
  node['properties'] = {
    'position': {'kind': 'offset', 'dx': dx, 'dy': dy},
    if (hit != null) 'transformHitTests': {'kind': 'boolean', 'value': hit},
    if (direction != 'unset')
      'textDirection': direction == 'null'
          ? {'kind': 'null'}
          : {'kind': 'enum', 'type': 'TextDirection', 'value': direction},
  };
  return raw;
}

Finder native() => find.byType(SlideTransition);
Finder body() => find.byKey(ValueKey('canvas-widget-${f.bodyId}'));
void main() {
  test(
    'strict required finite fractions nullable direction and source-only presence',
    () {
      final raw = data(), props = a.builder(raw)['properties'] as Map;
      for (final bad in [
        {'kind': 'null'},
        {'kind': 'double', 'value': 1},
        {'kind': 'boolean', 'value': false},
        {'kind': 'offset', 'dx': 'Infinity', 'dy': 0},
        {'kind': 'offset', 'dx': 0, 'dy': 'NaN'},
        {'kind': 'offset', 'dx': 0},
        {'kind': 'offset', 'dx': true, 'dy': 0},
        {'kind': 'dartObjectReferencePresence', 'rootSymbol': '_leak'},
      ]) {
        props['position'] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final literal in ['1e999', '-1e999']) {
        final raw = data(dx: 9.87654321);
        final bytes = Uint8List.fromList(
          utf8.encode(jsonEncode(raw).replaceFirst('9.87654321', literal)),
        );
        expect(() => CanvasModel.decode(bytes), throwsFormatException);
      }
      props.remove('position');
      expect(() => f.decode(raw), throwsFormatException);
      for (final good in [
        {'kind': 'offset', 'dx': -2.5, 'dy': 1e308},
        {'kind': 'dartObjectReferencePresence'},
      ]) {
        props['position'] = good;
        expect(() => f.decode(raw), returnsNormally);
      }
      props['position'] = {'kind': 'offset', 'dx': 0, 'dy': 0};
      for (final field in ['durationUs', 'curve', 'onEnd']) {
        props[field] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
        props.remove(field);
      }
      props['transformHitTests'] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException);
      props.remove('transformHitTests');
      props['textDirection'] = {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': 'auto',
      };
      expect(() => f.decode(raw), throwsFormatException);
    },
  );
  for (final rtl in [false, true]) {
    for (final direction in ['unset', 'null', 'ltr', 'rtl']) {
      for (final hit in <bool?>[null, false, true]) {
        for (final empty in [false, true]) {
          for (final tight in [false, true]) {
            testWidgets(
              'fractional layout rtl=$rtl explicit=$direction hit=$hit empty=$empty tight=$tight',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    dx: .5,
                    dy: -.25,
                    rtl: rtl,
                    direction: direction,
                    hit: hit,
                    empty: empty,
                    tight: tight,
                  ),
                );
                final n = tester.widget<SlideTransition>(native());
                expect(n.position, isA<AlwaysStoppedAnimation<Offset>>());
                expect(n.position.value, const Offset(.5, -.25));
                expect(n.transformHitTests, hit ?? true);
                expect(
                  n.textDirection,
                  direction == 'rtl'
                      ? TextDirection.rtl
                      : direction == 'ltr'
                      ? TextDirection.ltr
                      : null,
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
                  final delta = tester
                      .renderObject<RenderBox>(body())
                      .localToGlobal(Offset.zero, ancestor: parent);
                  expect(
                    delta,
                    Offset(
                      parent.size.width * (direction == 'rtl' ? -.5 : .5),
                      parent.size.height * -.25,
                    ),
                  );
                }
                expect(tester.takeException(), isNull);
              },
            );
          }
        }
      }
    }
  }
  testWidgets(
    'signed out-of-unit positions edits update immediately and keep child and State',
    (tester) async {
      await f.pump(tester, data());
      final state = tester.state(native()), child = tester.element(body());
      for (final x in [-2.5, 0.0, 1.5]) {
        await f.pump(tester, data(dx: x, dy: .5, direction: 'rtl', hit: false));
        expect(
          tester.widget<SlideTransition>(native()).position.value,
          Offset(x, .5),
        );
        final parent = tester.renderObject<RenderBox>(native());
        expect(
          tester
              .renderObject<RenderBox>(body())
              .localToGlobal(Offset.zero, ancestor: parent),
          Offset(-48 * x, 24),
        );
        expect(identical(state, tester.state(native())), isTrue);
        expect(identical(child, tester.element(body())), isTrue);
      }
      final raw = data(dx: .5, dy: .25);
      f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['properties'] =
          {'width': f.number(96), 'height': f.number(80)};
      await f.pump(tester, raw);
      final parent = tester.renderObject<RenderBox>(native());
      expect(parent.size, const Size(96, 80));
      expect(
        tester
            .renderObject<RenderBox>(body())
            .localToGlobal(Offset.zero, ancestor: parent),
        const Offset(48, 20),
      );
      expect(identical(state, tester.state(native())), isTrue);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets('project Animation is inert and explicitly previews zero', (
    tester,
  ) async {
    final raw = data(direction: 'rtl', hit: false);
    (a.builder(raw)['properties'] as Map)['position'] = {
      'kind': 'dartObjectReferencePresence',
    };
    await f.pump(tester, raw);
    final n = tester.widget<SlideTransition>(native());
    expect(n.position.value, Offset.zero);
    expect(n.transformHitTests, isFalse);
    expect(
      find.byWidgetPredicate(
        (w) =>
            w is Tooltip && w.message?.contains('stopped zero offset') == true,
      ),
      findsWidgets,
    );
    expect(tester.takeException(), isNull);
  });
  testWidgets('zero extent stays selectable and accepts a child drop', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    final selected = <String>[];
    await f.pump(
      tester,
      data(empty: true),
      selected: selected.add,
      drop: (r) => drop = r,
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
