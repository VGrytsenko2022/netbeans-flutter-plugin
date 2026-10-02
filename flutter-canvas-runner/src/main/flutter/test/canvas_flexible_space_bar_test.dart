import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_app_bar_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_pinned_header_sliver_test.dart' as p;

const flexId = '830430b6-27e6-4b93-b033-086062480e16';
const backgroundId = '830430b6-27e6-4b93-b033-086062480e17';
const titleId = '830430b6-27e6-4b93-b033-086062480e18';
const presets = [
  'none',
  'zoomBackground',
  'blurBackground',
  'fadeTitle',
  'zoomBackground,blurBackground',
  'zoomBackground,fadeTitle',
  'blurBackground,fadeTitle',
  'zoomBackground,blurBackground,fadeTitle',
];
Map<String, Object?> raw({
  String type = 'flutter.material.SliverAppBar',
  Map<String, Object?> props = const {},
  bool children = true,
  bool rtl = false,
}) {
  final model = a.data(
    type,
    title: false,
    rtl: rtl,
    props: {
      'primary': f.boolean(false),
      'expandedHeight': f.number(200),
      'pinned': f.boolean(true),
      'stretch': f.boolean(true),
    },
  );
  final header = f.findNode(model['root'] as Map<String, Object?>, f.fillId);
  (header['slots'] as Map)['flexibleSpace'] = f.single(
    f.node(flexId, 'flutter.material.FlexibleSpaceBar', props, {
      if (children)
        'title': f.single(
          f.node(titleId, 'flutter.widgets.Text', {
            'data': {'kind': 'string', 'value': 'Flexible title'},
          }),
        ),
      if (children)
        'background': f.single(
          f.node(backgroundId, 'flutter.widgets.ColoredBox', {
            'color': {'kind': 'color', 'argb': '0xFF4288CC'},
          }),
        ),
    }),
  );
  return model;
}

Map<String, Object?> flex(Map<String, Object?> model) =>
    f.findNode(model['root'] as Map<String, Object?>, flexId);
void main() {
  test('exact native nullability scale and closed effects schema', () {
    final model = raw();
    for (final field in [
      'collapseMode',
      'stretchModes',
      'expandedTitleScale',
    ]) {
      flex(model)['properties'] = {
        field: {'kind': 'null'},
      };
      expect(() => f.decode(model), throwsFormatException);
    }
    for (final field in ['centerTitle', 'titlePadding']) {
      flex(model)['properties'] = {
        field: {'kind': 'null'},
      };
      expect(() => f.decode(model), returnsNormally);
    }
    for (final value in [-1.0, 0.0, 0.99]) {
      flex(model)['properties'] = {'expandedTitleScale': f.number(value)};
      expect(() => f.decode(model), throwsFormatException);
    }
    flex(model)['properties'] = {'expandedTitleScale': f.number(1)};
    expect(() => f.decode(model), returnsNormally);
    for (final value in ['', 'unknown', 'zoomBackground;evil()', 'null']) {
      flex(model)['properties'] = {
        'stretchModes': {'kind': 'string', 'value': value},
      };
      expect(() => f.decode(model), throwsFormatException);
    }
  });
  for (final type in a.types) {
    for (final collapse in ['parallax', 'pin', 'none']) {
      for (final mode in presets) {
        testWidgets(
          '$type $collapse $mode scrolls with native properties and stable child keys',
          (tester) async {
            await f.pump(
              tester,
              raw(
                type: type,
                props: {
                  'collapseMode': {
                    'kind': 'enum',
                    'type': 'CollapseMode',
                    'value': collapse,
                  },
                  'stretchModes': {'kind': 'string', 'value': mode},
                  'expandedTitleScale': f.number(1),
                },
              ),
            );
            await tester.pumpAndSettle();
            expect(tester.takeException(), isNull);
            final native = tester.widget<FlexibleSpaceBar>(
              find.byType(FlexibleSpaceBar),
            );
            expect(native.collapseMode.name, collapse);
            expect(
              native.stretchModes.map((v) => v.name).toList(),
              mode == 'none' ? <String>[] : mode.split(','),
            );
            expect(native.expandedTitleScale, 1);
            expect(
              find.text('Flexible title', skipOffstage: false),
              findsOneWidget,
            );
            final position = Scrollable.of(
              tester.element(find.byType(SliverAppBar)),
            ).position;
            for (final offset in [80.0, 150.0, 0.0]) {
              position.jumpTo(offset);
              await tester.pumpAndSettle();
              expect(tester.takeException(), isNull);
            }
          },
        );
      }
    }
  }
  for (final rtl in [false, true]) {
    testWidgets('directional padding and live title/effect edits rtl=$rtl', (
      tester,
    ) async {
      final model = raw(
        rtl: rtl,
        props: {
          'centerTitle': f.boolean(false),
          'titlePadding': {
            'kind': 'edgeInsetsDirectional',
            'start': 20.0,
            'top': 0.0,
            'end': 8.0,
            'bottom': 12.0,
          },
        },
      );
      await f.pump(tester, model);
      final native = tester.widget<FlexibleSpaceBar>(
        find.byType(FlexibleSpaceBar),
      );
      expect(native.centerTitle, isFalse);
      expect(
        native.titlePadding!.resolve(
          rtl ? TextDirection.rtl : TextDirection.ltr,
        ),
        rtl
            ? const EdgeInsets.fromLTRB(8, 0, 20, 12)
            : const EdgeInsets.fromLTRB(20, 0, 8, 12),
      );
      flex(model)['properties'] = {
        'centerTitle': f.boolean(true),
        'expandedTitleScale': f.number(2),
        'stretchModes': {'kind': 'string', 'value': 'none'},
      };
      await f.pump(tester, model);
      await tester.pumpAndSettle();
      expect(
        tester
            .widget<FlexibleSpaceBar>(find.byType(FlexibleSpaceBar))
            .centerTitle,
        isTrue,
      );
      expect(find.text('Flexible title', skipOffstage: false), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }
  for (final mode in presets) {
    testWidgets('native overscroll effects $mode', (tester) async {
      final model = raw(
        props: {
          'stretchModes': {'kind': 'string', 'value': mode},
        },
      );
      f.findNode(
        model['root'] as Map<String, Object?>,
        '82890f1f-a16d-4dc3-ac0c-12bb1b2698c8',
      )['properties'] = {
        'physics': {'kind': 'string', 'value': 'bouncing'},
      };
      await f.pump(tester, model);
      await tester.pumpAndSettle();
      final gesture = await tester.startGesture(
        tester.getCenter(find.byType(CustomScrollView)),
      );
      await gesture.moveBy(const Offset(0, 24));
      await gesture.moveBy(const Offset(0, 120));
      await tester.pump();
      final bar = find.byType(FlexibleSpaceBar);
      final settings = tester
          .element(bar)
          .dependOnInheritedWidgetOfExactType<FlexibleSpaceBarSettings>()!;
      expect(tester.getSize(bar).height, greaterThan(settings.maxExtent));
      final positions = tester
          .widgetList<Positioned>(
            find.descendant(of: bar, matching: find.byType(Positioned)),
          )
          .toList();
      expect(
        positions.first.height,
        mode.contains('zoomBackground')
            ? greaterThan(settings.maxExtent)
            : settings.maxExtent,
      );
      expect(
        find.descendant(of: bar, matching: find.byType(BackdropFilter)),
        mode.contains('blurBackground') ? findsOneWidget : findsNothing,
      );
      final titleOpacity = tester.widgetList<Opacity>(
        find.ancestor(
          of: find.text('Flexible title'),
          matching: find.byType(Opacity),
        ),
      );
      expect(
        titleOpacity.any((v) => v.opacity < 1),
        mode.contains('fadeTitle'),
      );
      expect(tester.takeException(), isNull);
      await gesture.up();
      await tester.pumpAndSettle();
    });
  }
  for (final type in a.types) {
    testWidgets('$type optional actions padding reference is not executed', (
      tester,
    ) async {
      final model = raw(type: type);
      final header = f.findNode(
        model['root'] as Map<String, Object?>,
        f.fillId,
      );
      (header['properties'] as Map)['actionsPadding'] = {
        'kind': 'dartObjectReferencePresence',
      };
      await f.pump(tester, model);
      expect(
        tester.widget<SliverAppBar>(find.byType(SliverAppBar)).actionsPadding,
        isNull,
      );
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets(
    'empty title and background have separate reachable drop zones and move previews',
    (tester) async {
      CanvasDropResolver? drop;
      CanvasMovePreviewResolver? move;
      await f.pump(
        tester,
        raw(children: false),
        drop: (v) => drop = v,
        move: (v) => move = v,
      );
      final rect = tester.getRect(find.byType(FlexibleSpaceBar));
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final slots = <String>{};
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
              token: 'flex-test',
              widgetType: 'flutter.widgets.Text',
              traits: {},
            ),
          );
          if (target?.parentWidgetId == flexId) slots.add(target!.slotName);
        }
      }
      expect(slots, {'title', 'background'});
      expect(
        move!(backgroundId, flexId, 'title', 0),
        isNull,
        reason: 'Missing move source is rejected',
      );
      for (final slot in ['title', 'background']) {
        expect(
          move!(p.tailBoxId, flexId, slot, 0),
          isNotNull,
          reason: 'Host-authorized move has matching native slot geometry',
        );
      }
    },
  );
  testWidgets(
    'missing inherited settings is labeled without crashing or inventing settings',
    (tester) async {
      final model = raw();
      model['root'] = flex(model);
      await f.pump(tester, model);
      expect(find.byType(FlexibleSpaceBar), findsNothing);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message!.contains(
                'requires inherited FlexibleSpaceBarSettings',
              ),
        ),
        findsOneWidget,
      );
      expect(find.text('Flexible title'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'project nullable padding and effects references use labeled native fallback',
    (tester) async {
      final model = raw(
        props: {
          for (final name in ['titlePadding', 'stretchModes'])
            name: {'kind': 'dartObjectReferencePresence'},
        },
      );
      await f.pump(tester, model);
      expect(tester.takeException(), isNull);
      final native = tester.widget<FlexibleSpaceBar>(
        find.byType(FlexibleSpaceBar),
      );
      expect(native.titlePadding, isNull);
      expect(native.stretchModes, [StretchMode.zoomBackground]);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              (w.message ?? '').contains('FlexibleSpaceBar') &&
              (w.message ?? '').contains('titlePadding'),
        ),
        findsWidgets,
      );
    },
  );
}
