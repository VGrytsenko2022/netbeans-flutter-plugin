import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_sliver_opacity_test.dart' as o;

const type = 'flutter.widgets.SliverIgnorePointer';
Map<String, Object?> data({
  bool? ignoring,
  String semantics = 'unset',
  bool empty = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  double preceding = 0,
}) {
  final raw = o.data(
    empty: empty,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    preceding: preceding,
  );
  final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  fill['type'] = type;
  fill['properties'] = {
    if (ignoring != null) 'ignoring': f.boolean(ignoring),
    if (semantics != 'unset')
      'ignoringSemantics': semantics == 'null'
          ? {'kind': 'null'}
          : f.boolean(semantics == 'true'),
  };
  return raw;
}

void main() {
  test(
    'strict boolean/null contract, omitted slot, sliver-only placement and parent data',
    () {
      final raw = data(empty: true);
      final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      for (final field in ['ignoring', 'ignoringSemantics']) {
        for (final bad in [
          {'kind': 'integer', 'value': 1},
          {'kind': 'string', 'value': 'true'},
          f.number(1),
        ]) {
          fill['properties'] = {field: bad};
          expect(() => f.decode(raw), throwsFormatException);
        }
      }
      fill['properties'] = {
        'ignoring': {'kind': 'null'},
      };
      expect(() => f.decode(raw), throwsFormatException);
      fill['properties'] = {
        'ignoringSemantics': {'kind': 'null'},
      };
      expect(() => f.decode(raw), returnsNormally);
      fill['slots'] = <String, Object?>{};
      expect(() => f.decode(raw), returnsNormally);
      fill['slots'] = {
        'sliver': {'kind': 'list', 'children': []},
      };
      expect(() => f.decode(raw), throwsFormatException);
      for (final child in [
        f.node(o.adapterId, 'flutter.widgets.Text', {
          'data': {'kind': 'string', 'value': 'Box'},
        }),
        f.node(
          o.adapterId,
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
  for (final ignoring in [null, false, true]) {
    for (final horizontal in [false, true]) {
      for (final reverse in [false, true]) {
        for (final rtl in [false, true]) {
          for (final empty in [false, true]) {
            testWidgets(
              'native ignoring=$ignoring h=$horizontal reverse=$reverse rtl=$rtl empty=$empty',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    ignoring: ignoring,
                    horizontal: horizontal,
                    reverse: reverse,
                    rtl: rtl,
                    empty: empty,
                  ),
                );
                final native = tester.widget<SliverIgnorePointer>(
                  find.byType(SliverIgnorePointer, skipOffstage: false),
                );
                expect(native.ignoring, ignoring ?? true);
                // ignore: deprecated_member_use
                expect(native.ignoringSemantics, isNull);
                if (empty) {
                  expect((native.child! as SliverToBoxAdapter).child, isNull);
                }
                final render = tester.renderObject<RenderSliverIgnorePointer>(
                  find.byType(SliverIgnorePointer, skipOffstage: false),
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
    'live ignore/semantics states retain render identity and native semantic rules',
    (tester) async {
      RenderSliverIgnorePointer? original;
      for (final ignoring in [null, false, true, false]) {
        for (final semantics in ['unset', 'null', 'false', 'true']) {
          await f.pump(tester, data(ignoring: ignoring, semantics: semantics));
          final render = tester.renderObject<RenderSliverIgnorePointer>(
            find.byType(SliverIgnorePointer),
          );
          original ??= render;
          expect(identical(render, original), true);
          expect(render.ignoring, ignoring ?? true);
          expect(
            // ignore: deprecated_member_use
            render.ignoringSemantics,
            semantics == 'unset' || semantics == 'null'
                ? null
                : semantics == 'true',
          );
          final config = SemanticsConfiguration();
          render.describeSemanticsConfiguration(config);
          expect(
            config.isBlockingUserActions,
            (ignoring ?? true) && semantics != 'false',
          );
          final children = <RenderObject>[];
          render.visitChildrenForSemantics(children.add);
          expect(children.isNotEmpty, semantics != 'true');
          expect(render.geometry!.scrollExtent, 48);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );
  for (final ignoring in [false, true]) {
    for (final semantics in <bool?>[null, false, true]) {
      testWidgets(
        'native pointer versus semantics ignoring=$ignoring override=$semantics',
        (tester) async {
          final handle = tester.ensureSemantics();
          var taps = 0;
          await tester.pumpWidget(
            MaterialApp(
              home: CustomScrollView(
                slivers: [
                  SliverIgnorePointer(
                    ignoring: ignoring,
                    // ignore: deprecated_member_use
                    ignoringSemantics: semantics,
                    sliver: SliverToBoxAdapter(
                      child: GestureDetector(
                        behavior: HitTestBehavior.opaque,
                        onTap: () => taps++,
                        child: Semantics(
                          label: 'Sliver action',
                          onTap: () {},
                          child: const SizedBox(height: 60),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
          await tester.pump();
          await tester.tapAt(const Offset(20, 20));
          expect(taps, ignoring ? 0 : 1);
          final render = tester.renderObject<RenderSliverIgnorePointer>(
            find.byType(SliverIgnorePointer),
          );
          expect(
            render.hitTest(
              SliverHitTestResult(),
              mainAxisPosition: 20,
              crossAxisPosition: 20,
            ),
            !ignoring,
          );
          final label = find.bySemanticsLabel('Sliver action');
          if (semantics == true) {
            expect(label, findsNothing);
          } else {
            expect(label, findsOneWidget);
            expect(
              tester
                  .getSemantics(label)
                  .getSemanticsData()
                  .hasAction(SemanticsAction.tap),
              !ignoring || semantics == false,
            );
          }
          expect(tester.takeException(), isNull);
          handle.dispose();
        },
      );
    }
  }
  testWidgets('ignoring pointer leaves programmatic keyboard focus intact', (
    tester,
  ) async {
    final node = FocusNode();
    var keys = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: CustomScrollView(
          slivers: [
            SliverIgnorePointer(
              sliver: SliverToBoxAdapter(
                child: Focus(
                  focusNode: node,
                  onKeyEvent: (_, event) {
                    if (event is KeyDownEvent) {
                      keys++;
                    }
                    return KeyEventResult.handled;
                  },
                  child: const SizedBox(height: 60),
                ),
              ),
            ),
          ],
        ),
      ),
    );
    node.requestFocus();
    await tester.pump();
    await tester.sendKeyEvent(LogicalKeyboardKey.keyA);
    expect(node.hasFocus, true);
    expect(keys, 1);
    await tester.pumpWidget(const SizedBox.shrink());
    node.dispose();
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'Designer handle selects ignored slivers and move geometry remains available',
    (tester) async {
      final selected = <String>[];
      CanvasMovePreviewResolver? move;
      await f.pump(
        tester,
        data(),
        selected: selected.add,
        move: (v) => move = v,
      );
      final box = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
      );
      await tester.tapAt(box.bottomRight - const Offset(5, 5));
      await tester.pump();
      expect(selected, [
        o.viewportId,
      ], reason: 'Normal body taps retain native IgnorePointer behavior.');
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-group-${f.fillId}'),
      );
      expect(handle, findsOneWidget);
      await tester.tap(handle);
      await tester.pump();
      expect(
        selected,
        contains(o.adapterId),
        reason:
            'Coincident Sliver handles cycle to the child; the widget tree exposes deeper descendants.',
      );
      expect(move!(o.adapterId, o.viewportId, 'slivers', 1), isNotNull);
      expect(
        tester
            .widget<SliverIgnorePointer>(find.byType(SliverIgnorePointer))
            .ignoring,
        true,
      );
      expect(tester.takeException(), isNull);
    },
  );
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        testWidgets(
          'empty ignored sliver drop h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            await f.pump(
              tester,
              data(
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
                token: 'ignore-child',
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
  testWidgets('offscreen ignored sliver exposes no stale nested drop target', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    await f.pump(
      tester,
      data(empty: true, preceding: 700),
      drop: (v) => drop = v,
    );
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final point =
        tester.getRect(find.byType(CustomScrollView)).topLeft +
        const Offset(5, 5);
    final target = drop!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(
        token: 'offscreen-ignore',
        widgetType: 'flutter.widgets.SliverToBoxAdapter',
        traits: {canvasSliverWidgetTrait},
      ),
    );
    expect(target?.parentWidgetId, isNot(f.fillId));
    expect(tester.takeException(), isNull);
  });
}
