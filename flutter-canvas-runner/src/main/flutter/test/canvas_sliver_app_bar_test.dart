import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_pinned_header_sliver_test.dart' as p;

const types = [
  'flutter.material.SliverAppBar',
  'flutter.material.SliverAppBar.medium',
  'flutter.material.SliverAppBar.large',
];
Map<String, Object?> data(
  String type, {
  Map<String, Object?> props = const {},
  bool title = true,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
}) {
  final raw = p.data(
    empty: true,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
  );
  final header = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  header['type'] = type;
  header['properties'] = <String, Object?>{...props};
  header['slots'] = <String, Object?>{
    if (title)
      'title': f.single(
        f.node(f.bodyId, 'flutter.widgets.Text', {
          'data': {'kind': 'string', 'value': 'Sliver title'},
        }),
      ),
  };
  // Room for native expanded headers as well as the following scroll content.
  f.findNode(
    raw['root'] as Map<String, Object?>,
    'b5fef1dd-0126-4f98-beb0-4ec5effc8b08',
  )['properties'] = {
    'width': f.number(300),
    'height': f.number(400),
  };
  return raw;
}

Finder get header => find.byType(SliverAppBar, skipOffstage: false);
void main() {
  for (final type in types) {
    testWidgets('$type all five visible drop zones and strict bottom trait', (
      tester,
    ) async {
      CanvasDropResolver? drop;
      CanvasMovePreviewResolver? move;
      await f.pump(
        tester,
        data(
          type,
          title: false,
          props: {'expandedHeight': f.number(200), 'primary': f.boolean(false)},
        ),
        drop: (v) => drop = v,
        move: (v) => move = v,
      );
      await tester.pumpAndSettle();
      final rect = tester.getRect(find.byType(AppBar).first),
          surface = tester.getRect(find.byType(CanvasDocumentView));
      final slots = <String>{};
      for (final preferred in [false, true]) {
        for (int x = 1; x < 10; x++) {
          for (int y = 1; y < 20; y++) {
            final point = Offset(
              rect.left + rect.width * x / 10,
              rect.top + rect.height * y / 20,
            );
            final target = drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'test',
                widgetType: preferred
                    ? 'flutter.material.AppBar'
                    : 'flutter.widgets.Text',
                traits: preferred ? {canvasPreferredSizeWidgetTrait} : {},
              ),
            );
            if (target?.parentWidgetId == f.fillId) slots.add(target!.slotName);
          }
        }
      }
      expect(slots, {'leading', 'title', 'actions', 'flexibleSpace', 'bottom'});
      for (final slot in ['leading', 'title', 'actions', 'flexibleSpace']) {
        expect(move!(p.tailBoxId, f.fillId, slot, 0), isNotNull);
      }
      expect(move!(p.tailBoxId, f.fillId, 'bottom', 0), isNull);
      expect(tester.takeException(), isNull);
    });
    testWidgets(
      '$type bottom PreferredSize geometry, styles and live stretch configuration',
      (tester) async {
        final raw = data(
          type,
          props: {
            'primary': f.boolean(false),
            'pinned': f.boolean(true),
            'stretch': f.boolean(true),
            'stretchTriggerOffset': f.number(110),
            'onStretchTrigger': {'kind': 'string', 'value': 'noop'},
            'shapeKind': {'kind': 'string', 'value': 'roundedRectangle'},
            'shapeRadiusTopLeft': f.number(8),
            'iconThemeSize': f.number(22),
            'actionsIconThemeSize': f.number(20),
            'titleTextStyleFontSize': f.number(24),
            'toolbarTextStyleFontSize': f.number(14),
            'systemOverlayStyleStatusBarBrightness': {
              'kind': 'enum',
              'type': 'Brightness',
              'value': 'dark',
            },
            'actionsPadding': {
              'kind': 'edgeInsetsDirectional',
              'start': 4.0,
              'end': 6.0,
              'top': 2.0,
              'bottom': 3.0,
            },
          },
        );
        final h = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
        (h['slots'] as Map)['bottom'] = f.single(
          f.node(
            '11fb23dc-b07a-4c1a-aa2c-e66253e111ee',
            'flutter.widgets.PreferredSize',
            {
              'preferredSize': {'kind': 'size', 'width': 24.0, 'height': 24.0},
            },
            {
              'child': f.single(
                f.node(
                  '31856212-37b7-49d6-ad1c-b8849122b51c',
                  'flutter.widgets.SizedBox',
                  {'height': f.number(24)},
                ),
              ),
            },
          ),
        );
        await f.pump(tester, raw);
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        var native = tester.widget<SliverAppBar>(header);
        expect(native.bottom!.preferredSize.height, 24);
        expect(native.shape, isA<RoundedRectangleBorder>());
        expect(native.iconTheme!.size, 22);
        expect(native.actionsIconTheme!.size, 20);
        expect(native.titleTextStyle!.fontSize, 24);
        expect(native.toolbarTextStyle!.fontSize, 14);
        expect(native.systemOverlayStyle!.statusBarBrightness, Brightness.dark);
        expect(
          native.actionsPadding,
          const EdgeInsetsDirectional.fromSTEB(4, 2, 6, 3),
        );
        var render = tester.renderObject<RenderSliverPersistentHeader>(header);
        expect(render.stretchConfiguration!.stretchTriggerOffset, 110);
        (h['properties'] as Map)['stretchTriggerOffset'] = f.number(175);
        await f.pump(tester, raw);
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        render = tester.renderObject<RenderSliverPersistentHeader>(header);
        expect(render.stretchConfiguration!.stretchTriggerOffset, 175);
      },
    );
    test('$type exact schema and native conflicts', () {
      expect(
        canvasDropSlotsForWidgetType(type).map((s) => s.slotName).toSet(),
        {'leading', 'title', 'actions', 'flexibleSpace', 'bottom'},
      );
      expect(() => f.decode(data(type)), returnsNormally);
      for (final props in [
        {'snap': f.boolean(true)},
        {'stretchTriggerOffset': f.number(0)},
        {'collapsedHeight': f.number(55)},
        {
          'toolbarHeight': {'kind': 'null'},
        },
        {
          'notificationPredicate': {'kind': 'string', 'value': 'all'},
        },
        {
          'shape': {'kind': 'null'},
          'shapeKind': {'kind': 'string', 'value': 'circle'},
        },
      ]) {
        expect(() => f.decode(data(type, props: props)), throwsFormatException);
      }
      expect(
        () => f.decode(
          data(
            type,
            props: {
              'centerTitle': {'kind': 'null'},
              'expandedHeight': {'kind': 'null'},
              'onStretchTrigger': {'kind': 'string', 'value': 'noop'},
            },
          ),
        ),
        returnsNormally,
      );
    });
    for (final horizontal in [false, true]) {
      for (final reverse in [false, true]) {
        for (final rtl in [false, true]) {
          testWidgets(
            '$type title keys survive scroll and property edits horizontal=$horizontal reverse=$reverse rtl=$rtl',
            (tester) async {
              final raw = data(
                type,
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
              );
              await f.pump(tester, raw);
              await tester.pumpAndSettle();
              expect(tester.takeException(), isNull);
              final native = tester.widget<SliverAppBar>(header);
              expect(native.pinned, type != types.first);
              expect(native.toolbarHeight, type == types.first ? 56 : 64);
              final position = Scrollable.of(tester.element(header)).position;
              for (final offset in [80.0, 160.0, 0.0, 120.0, 0.0]) {
                position.jumpTo(offset);
                await tester.pumpAndSettle();
                expect(tester.takeException(), isNull);
              }
              (f.findNode(
                    raw['root'] as Map<String, Object?>,
                    f.fillId,
                  )['properties']
                  as Map)['stretchTriggerOffset'] = f.number(
                125,
              );
              await f.pump(tester, raw);
              await tester.pumpAndSettle();
              expect(tester.takeException(), isNull);
              expect(
                tester.widget<SliverAppBar>(header).stretchTriggerOffset,
                125,
              );
            },
          );
        }
      }
    }
    for (final pinned in [false, true]) {
      for (final floating in [false, true]) {
        for (final snap in [false, true]) {
          if (snap && !floating) continue;
          testWidgets(
            '$type scroll pinned=$pinned floating=$floating snap=$snap',
            (tester) async {
              await f.pump(
                tester,
                data(
                  type,
                  props: {
                    'pinned': f.boolean(pinned),
                    'floating': f.boolean(floating),
                    'snap': f.boolean(snap),
                    'expandedHeight': f.number(180),
                    'stretch': f.boolean(true),
                  },
                ),
              );
              await tester.pumpAndSettle();
              expect(tester.takeException(), isNull);
              final native = tester.widget<SliverAppBar>(header);
              expect(native.pinned, pinned);
              expect(native.floating, floating);
              expect(native.snap, snap);
              final position = Scrollable.of(tester.element(header)).position;
              position.jumpTo(240);
              await tester.pumpAndSettle();
              final render = tester.renderObject<RenderSliver>(header);
              expect(
                render.geometry!.paintExtent,
                pinned ? greaterThan(0) : equals(0),
              );
              position.jumpTo(0);
              await tester.pumpAndSettle();
              expect(tester.takeException(), isNull);
            },
          );
        }
      }
    }
  }
}
