import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.SliverOpacity';
const adapterId = 'cf337891-857a-4343-8227-bb84db7e016e';
const viewportId = '82890f1f-a16d-4dc3-ac0c-12bb1b2698c8';
Map<String, Object?> data({
  double opacity = 1,
  bool? semantics,
  bool empty = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  double preceding = 0,
}) {
  final raw = f.modelJson(
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    preceding: preceding,
  );
  final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  final child =
      ((fill['slots'] as Map)['child'] as Map)['child'] as Map<String, Object?>;
  fill['type'] = type;
  fill['properties'] = {
    'opacity': f.number(opacity),
    if (semantics != null) 'alwaysIncludeSemantics': f.boolean(semantics),
  };
  fill['slots'] = {
    'sliver': f.single(
      empty
          ? null
          : f.node(adapterId, 'flutter.widgets.SliverToBoxAdapter', {}, {
              'child': f.single(child),
            }),
    ),
  };
  return raw;
}

void main() {
  test(
    'strict opacity and semantics domains, optional sliver and parent-data restrictions',
    () {
      final raw = data(empty: true);
      final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      for (final value in [
        f.number(-0.01),
        f.number(1.01),
        {'kind': 'integer', 'value': 2},
        {'kind': 'null'},
        {'kind': 'string', 'value': '0.5'},
        {'kind': 'enum', 'type': 'double', 'value': 'infinity'},
      ]) {
        fill['properties'] = {'opacity': value};
        expect(() => f.decode(raw), throwsFormatException);
      }
      fill['properties'] = <String, Object?>{};
      expect(() => f.decode(raw), throwsFormatException);
      for (final integer in [0, 1]) {
        fill['properties'] = {
          'opacity': {'kind': 'integer', 'value': integer},
        };
        expect(() => f.decode(raw), returnsNormally);
      }
      for (final bad in [
        {'kind': 'null'},
        {'kind': 'integer', 'value': 1},
        {'kind': 'string', 'value': 'true'},
      ]) {
        fill['properties'] = {
          'opacity': f.number(0),
          'alwaysIncludeSemantics': bad,
        };
        expect(() => f.decode(raw), throwsFormatException);
      }
      fill['properties'] = {'opacity': f.number(0)};
      fill['slots'] = <String, Object?>{};
      expect(() => f.decode(raw), returnsNormally);
      fill['slots'] = {
        'sliver': {'kind': 'list', 'children': []},
      };
      expect(() => f.decode(raw), throwsFormatException);
      for (final child in [
        f.node(adapterId, 'flutter.widgets.Text', {
          'data': {'kind': 'string', 'value': 'Box'},
        }),
        f.node(
          adapterId,
          'flutter.widgets.SliverCrossAxisExpanded',
          {
            'flex': {'kind': 'integer', 'value': 1},
          },
          {
            'sliver': f.single(
              f.node(f.bodyId, 'flutter.widgets.SliverToBoxAdapter'),
            ),
          },
        ),
      ]) {
        fill['slots'] = {'sliver': f.single(child)};
        expect(() => f.decode(raw), throwsFormatException);
      }
      fill['slots'] = <String, Object?>{};
      raw['root'] = fill;
      expect(() => f.decode(raw), throwsFormatException);
    },
  );

  for (final opacity in [0.0, 0.125, 1.0]) {
    for (final horizontal in [false, true]) {
      for (final reverse in [false, true]) {
        for (final rtl in [false, true]) {
          for (final empty in [false, true]) {
            testWidgets(
              'native geometry opacity=$opacity h=$horizontal reverse=$reverse rtl=$rtl empty=$empty',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    opacity: opacity,
                    horizontal: horizontal,
                    reverse: reverse,
                    rtl: rtl,
                    empty: empty,
                  ),
                );
                final native = tester.widget<SliverOpacity>(
                  find.byType(SliverOpacity, skipOffstage: false),
                );
                expect(native.opacity, opacity);
                expect(native.alwaysIncludeSemantics, false);
                if (empty) {
                  expect((native.child! as SliverToBoxAdapter).child, isNull);
                }
                final render = tester.renderObject<RenderSliverOpacity>(
                  find.byType(SliverOpacity, skipOffstage: false),
                );
                expect(render.geometry!.scrollExtent, empty ? 0 : 48);
                expect(render.geometry!.paintExtent, empty ? 0 : 48);
                if (!empty) {
                  final size = tester.getSize(
                    find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
                  );
                  expect(horizontal ? size.width : size.height, 48);
                  expect(
                    horizontal ? size.height : size.width,
                    horizontal ? 160 : 300,
                  );
                  expect(render.debugLayer == null, opacity == 0);
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
    'live opacity/semantics edits preserve identity, geometry and native semantics traversal',
    (tester) async {
      RenderSliverOpacity? original;
      for (final opacity in [1.0, 0.5, 0.0, 0.001, 0.0, 1.0]) {
        for (final semantics in [null, false, true]) {
          await f.pump(tester, data(opacity: opacity, semantics: semantics));
          final render = tester.renderObject<RenderSliverOpacity>(
            find.byType(SliverOpacity, skipOffstage: false),
          );
          original ??= render;
          expect(identical(render, original), true);
          expect(render.opacity, opacity);
          expect(render.alwaysIncludeSemantics, semantics ?? false);
          final children = <RenderObject>[];
          render.visitChildrenForSemantics(children.add);
          // Flutter quantizes opacity to an 8-bit alpha, including tiny nonzero values.
          expect(
            children.isNotEmpty,
            opacity >= 0.5 / 255 || semantics == true,
          );
          expect(render.geometry!.scrollExtent, 48);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  testWidgets(
    'native zero opacity retains pointer hits and conditionally exposes semantics',
    (tester) async {
      final handle = tester.ensureSemantics();
      var taps = 0;
      for (final include in [false, true]) {
        await tester.pumpWidget(
          MaterialApp(
            home: CustomScrollView(
              slivers: [
                SliverOpacity(
                  opacity: 0,
                  alwaysIncludeSemantics: include,
                  sliver: SliverToBoxAdapter(
                    child: GestureDetector(
                      onTap: () => taps++,
                      behavior: HitTestBehavior.opaque,
                      child: Semantics(
                        label: 'Hidden sliver action',
                        child: SizedBox(height: 60),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
        await tester.pump();
        expect(
          find.bySemanticsLabel('Hidden sliver action'),
          include ? findsOneWidget : findsNothing,
        );
        await tester.tapAt(const Offset(20, 20));
        expect(taps, include ? 2 : 1);
        expect(tester.takeException(), isNull);
      }
      handle.dispose();
    },
  );

  testWidgets(
    'transparent child remains selectable and movable in the Designer',
    (tester) async {
      final selected = <String>[];
      CanvasMovePreviewResolver? move;
      await f.pump(
        tester,
        data(opacity: 0),
        selected: selected.add,
        move: (v) => move = v,
      );
      final box = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
      );
      await tester.tapAt(box.bottomRight - const Offset(5, 5));
      await tester.pump();
      expect(selected, contains('325ac58f-fc3e-4900-809a-16f1a47a9d4d'));
      expect(move!(adapterId, viewportId, 'slivers', 1), isNotNull);
      expect(tester.takeException(), isNull);
    },
  );

  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        testWidgets(
          'empty zero-opacity sliver exposes bounded insertion target h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            await f.pump(
              tester,
              data(
                opacity: 0,
                empty: true,
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
              ),
              drop: (v) => drop = v,
            );
            final surface = tester.getRect(find.byType(CanvasDocumentView));
            final viewport = tester.getRect(find.byType(CustomScrollView));
            final flipped = reverse != (horizontal && rtl);
            final point = horizontal
                ? Offset(
                    flipped ? viewport.right - 5 : viewport.left + 5,
                    viewport.center.dy,
                  )
                : Offset(
                    viewport.center.dx,
                    flipped ? viewport.bottom - 5 : viewport.top + 5,
                  );
            final target = drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'opacity-child',
                widgetType: 'flutter.widgets.SliverToBoxAdapter',
                traits: {canvasSliverWidgetTrait},
              ),
            );
            expect(target?.parentWidgetId, f.fillId);
            expect(target?.slotName, 'sliver');
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
}
