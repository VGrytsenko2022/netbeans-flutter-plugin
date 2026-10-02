import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.AnimatedBuilder.sliver';
Map<String, Object?> data({
  bool reference = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
}) {
  final raw = f.modelJson(
    empty: true,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
  );
  final node = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  node['type'] = type;
  node['properties'] = <String, Object?>{
    'animation': {'kind': 'string', 'value': 'none'},
    'builder': reference
        ? {'kind': 'dartObjectReferencePresence'}
        : {'kind': 'string', 'value': 'child'},
  };
  node['slots'] = <String, Object?>{};
  return raw;
}

void main() {
  testWidgets(
    'mobile and tablet profile rotation updates native MediaQuery without replacing the builder',
    (tester) async {
      Element? retained;
      for (final mode in ['MOBILE', 'TABLET']) {
        for (final size in [
          const Size(390, 844),
          const Size(844, 390),
          const Size(600, 600),
        ]) {
          final raw = data();
          final profile = raw['profile'] as Map;
          profile['previewMode'] = mode;
          profile['logicalWidth'] = size.width.toInt();
          profile['logicalHeight'] = size.height.toInt();
          await f.pump(tester, raw);
          final finder = find.byKey(
            ValueKey('canvas-animated-builder-${f.fillId}'),
            skipOffstage: false,
          );
          final element = tester.element(finder);
          final query = MediaQuery.of(element);
          expect(query.size, size);
          expect(
            query.orientation,
            size.width > size.height
                ? Orientation.landscape
                : Orientation.portrait,
          );
          if (retained != null) expect(identical(retained, element), isTrue);
          retained = element;
          expect(tester.takeException(), isNull);
        }
      }
    },
  );
  test(
    'strict builder required non-null closed preset or presence; optional typed Child; no invented slots',
    () {
      for (final bad in <Object?>[
        null,
        {'kind': 'null'},
        {'kind': 'boolean', 'value': true},
        {'kind': 'string', 'value': 'noop'},
        {'kind': 'string', 'value': '(_, c) => Text("bad")'},
        {'kind': 'dartObjectReferencePresence', 'rootSymbol': 'secret'},
      ]) {
        final raw = data();
        final props =
            f.findNode(
                  raw['root'] as Map<String, Object?>,
                  f.fillId,
                )['properties']
                as Map;
        if (bad == null) {
          props.remove('builder');
        } else {
          props['builder'] = bad;
        }
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final slot in ['sliver', 'children']) {
        final raw = data();
        f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['slots'] = {
          slot: f.single(null),
        };
        expect(() => f.decode(raw), throwsFormatException);
      }
    },
  );
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        for (final reference in [false, true]) {
          testWidgets(
            'native zero geometry identity selection and sibling drop h=$horizontal rev=$reverse rtl=$rtl ref=$reference',
            (tester) async {
              final raw = data(
                reference: reference,
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
              );
              CanvasDropResolver? drop;
              final selected = <String>[];
              await f.pump(
                tester,
                raw,
                drop: (r) => drop = r,
                selected: selected.add,
              );
              final finder = find.byKey(
                ValueKey('canvas-animated-builder-${f.fillId}'),
                skipOffstage: false,
              );
              expect(finder, findsOneWidget);
              final render = tester.renderObject<RenderSliver>(finder);
              expect(render.geometry!.scrollExtent, 0);
              expect(render.geometry!.paintExtent, 0);
              final handle = find.byKey(
                const ValueKey('canvas-zero-size-widget-target-${f.fillId}'),
              );
              expect(handle, findsOneWidget);
              final hints = find
                  .byType(Tooltip)
                  .evaluate()
                  .map((e) => (e.widget as Tooltip).message ?? '')
                  .join(' ');
              expect(
                hints.contains(
                  'project Listenable objects/getters/factories and builders are not executed',
                ),
                reference,
              );

              await tester.tap(handle);
              expect(selected, contains(f.fillId));
              final surface = tester.getRect(find.byType(CanvasDocumentView));
              final point = tester.getCenter(handle);
              final target = drop!(
                ((point.dx - surface.left) / surface.width * 1000000).round(),
                ((point.dy - surface.top) / surface.height * 1000000).round(),
                CanvasPaletteDragSource(
                  token: 'test',
                  widgetType: type,
                  traits: {canvasSliverWidgetTrait},
                ),
              );
              expect(target, isNotNull);
              expect(target!.parentWidgetId, f.fillId);
              expect(target.slotName, 'child');
              await f.pump(tester, raw);
              expect(identical(render, tester.renderObject(finder)), isTrue);
              expect(tester.takeException(), isNull);
            },
          );
        }
      }
    }
  }
}
