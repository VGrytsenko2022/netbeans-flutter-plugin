import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.material.FlexibleSpaceBarSettings';
const settingsId = 'c962f9db-45a3-4ac8-8d3b-9d40683f052e';
const barId = 'c962f9db-45a3-4ac8-8d3b-9d40683f052f';
const titleId = 'c962f9db-45a3-4ac8-8d3b-9d40683f0530';
const boxId = 'c962f9db-45a3-4ac8-8d3b-9d40683f0531';
const centerId = 'c962f9db-45a3-4ac8-8d3b-9d40683f0532';
const outerId = 'c962f9db-45a3-4ac8-8d3b-9d40683f0533';
Map<String, Object?> props({
  double opacity = 1,
  double min = 56,
  double max = 200,
  double current = 200,
}) => {
  'toolbarOpacity': f.number(opacity),
  'minExtent': f.number(min),
  'maxExtent': f.number(max),
  'currentExtent': f.number(current),
};
Map<String, Object?> raw({
  Map<String, Object?> values = const {},
  bool rtl = false,
  bool withBar = true,
}) {
  final model = f.modelJson(rtl: rtl);
  final title = f.node(titleId, 'flutter.widgets.Text', {
    'data': {'kind': 'string', 'value': 'Settings title'},
  });
  final child = withBar
      ? f.node(
          barId,
          'flutter.material.FlexibleSpaceBar',
          {'centerTitle': f.boolean(false)},
          {
            'title': f.single(title),
            'background': f.single(
              f.node(
                'c962f9db-45a3-4ac8-8d3b-9d40683f0534',
                'flutter.widgets.ColoredBox',
                {
                  'color': {'kind': 'color', 'argb': '0xFF224466'},
                },
              ),
            ),
          },
        )
      : title;
  model['root'] = f.node(centerId, 'flutter.widgets.Center', {}, {
    'child': f.single(
      f.node(
        settingsId,
        type,
        {...props(), ...values},
        {
          'child': f.single(
            f.node(
              boxId,
              'flutter.widgets.SizedBox',
              {'width': f.number(320), 'height': f.number(240)},
              {'child': f.single(child)},
            ),
          ),
        },
      ),
    ),
  });
  model['root'] = f.node(
    'c962f9db-45a3-4ac8-8d3b-9d40683f0535',
    'flutter.widgets.Directionality',
    {
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': rtl ? 'rtl' : 'ltr',
      },
    },
    {'child': f.single(model['root'] as Map<String, Object?>)},
  );
  return model;
}

Map<String, Object?> settings(Map<String, Object?> model) =>
    f.findNode(model['root'] as Map<String, Object?>, settingsId);
void main() {
  test(
    'complete native schema required scalars required child and extent relationships',
    () {
      final model = raw();
      expect(canvasReviewedRequiredWrapperSlot(type), 'child');
      for (final field in [
        'toolbarOpacity',
        'minExtent',
        'maxExtent',
        'currentExtent',
      ]) {
        settings(model)['properties'] = {...props()}..remove(field);
        expect(() => f.decode(model), throwsFormatException);
        settings(model)['properties'] = {
          ...props(),
          field: {'kind': 'null'},
        };
        expect(() => f.decode(model), throwsFormatException);
        settings(model)['properties'] = {...props(), field: f.number(-1)};
        expect(() => f.decode(model), throwsFormatException);
      }
      for (final min in [0.0, 56.0, 200.0]) {
        for (final max in [0.0, 56.0, 200.0]) {
          for (final current in [0.0, 56.0, 200.0]) {
            settings(model)['properties'] = props(
              min: min,
              max: max,
              current: current,
            );
            expect(
              () => f.decode(model),
              min <= current && current <= max
                  ? returnsNormally
                  : throwsFormatException,
            );
          }
        }
      }
      settings(model)['properties'] = props(opacity: 2);
      expect(
        () => f.decode(model),
        returnsNormally,
        reason: 'No invented upper bound on the Settings constructor',
      );
      for (final child in [
        null,
        f.node(barId, 'flutter.widgets.SliverToBoxAdapter'),
      ]) {
        settings(model)['slots'] = {'child': f.single(child)};
        expect(() => f.decode(model), throwsFormatException);
      }
    },
  );
  for (final rtl in [false, true]) {
    for (final opacity in [0.0, 0.5, 1.0]) {
      for (final leading in [null, false, true]) {
        testWidgets(
          'native inherited opacity=$opacity leading=$leading rtl=$rtl and stable extent updates',
          (tester) async {
            final model = raw(
              rtl: rtl,
              values: {
                ...props(opacity: opacity),
                'hasLeading': leading == null
                    ? {'kind': 'null'}
                    : f.boolean(leading),
                'isScrolledUnder': f.boolean(true),
              },
            );
            await f.pump(tester, model);
            final original = tester.element(find.byType(FlexibleSpaceBar));
            expect(
              Directionality.of(original),
              rtl ? TextDirection.rtl : TextDirection.ltr,
            );
            expect(
              tester.getSize(find.byType(FlexibleSpaceBar)),
              const Size(320, 240),
            );
            for (final current in [56.0, 120.0, 200.0]) {
              (settings(model)['properties'] as Map)['currentExtent'] = f
                  .number(current);
              await f.pump(tester, model);
              final element = tester.element(find.byType(FlexibleSpaceBar));
              expect(element, same(original));
              final inherited = element
                  .dependOnInheritedWidgetOfExactType<
                    FlexibleSpaceBarSettings
                  >()!;
              expect(inherited.currentExtent, current);
              expect(inherited.toolbarOpacity, opacity);
              expect(inherited.hasLeading, leading);
              expect(inherited.isScrolledUnder, true);
              expect(
                tester.getSize(find.byType(FlexibleSpaceBar)),
                const Size(320, 240),
                reason: 'Settings do not impose their extents on layout',
              );
              if (opacity == 0) {
                expect(find.text('Settings title'), findsNothing);
              } else {
                final title = find.text('Settings title');
                expect(title, findsOneWidget);
                final style = DefaultTextStyle.of(tester.element(title)).style;
                expect(style.color!.a, closeTo(opacity, 0.005));
                final pads = tester.widgetList<Padding>(
                  find.ancestor(of: title, matching: find.byType(Padding)),
                );
                expect(
                  pads.any(
                    (w) =>
                        w.padding ==
                        EdgeInsetsDirectional.only(
                          start: leading == false ? 0 : 72,
                          bottom: 16,
                        ),
                  ),
                  true,
                );
              }
              expect(tester.takeException(), isNull);
            }
          },
        );
      }
    }
  }
  testWidgets(
    'nearest nested settings override outer values and all six changes notify',
    (tester) async {
      final model = raw();
      final root = model['root'] as Map<String, Object?>;
      model['root'] = f.node(outerId, type, props(opacity: 0), {
        'child': f.single(root),
      });
      await f.pump(tester, model);
      expect(find.text('Settings title'), findsOneWidget);
      settings(model)['properties'] = {
        ...props(opacity: 0.5, min: 0, max: 300, current: 120),
        'hasLeading': f.boolean(false),
        'isScrolledUnder': {'kind': 'null'},
      };
      await f.pump(tester, model);
      final inherited = tester
          .element(find.byType(FlexibleSpaceBar))
          .dependOnInheritedWidgetOfExactType<FlexibleSpaceBarSettings>()!;
      expect(inherited.toolbarOpacity, 0.5);
      expect(inherited.minExtent, 0);
      expect(inherited.maxExtent, 300);
      expect(inherited.currentExtent, 120);
      expect(inherited.hasLeading, false);
      expect(inherited.isScrolledUnder, isNull);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'opacity above one is retained and only incompatible FlexibleSpaceBar preview is labeled',
    (tester) async {
      await f.pump(tester, raw(values: props(opacity: 2), withBar: false));
      expect(
        tester
            .widget<FlexibleSpaceBarSettings>(
              find.byType(FlexibleSpaceBarSettings),
            )
            .toolbarOpacity,
        2,
      );
      expect(find.text('Settings title'), findsOneWidget);
      expect(tester.takeException(), isNull);
      await f.pump(tester, raw(values: props(opacity: 2)));
      expect(find.byType(FlexibleSpaceBar), findsNothing);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              (w.message ?? '').contains('Toolbar opacity exceeds 1'),
        ),
        findsOneWidget,
      );
      expect(find.text('Settings title'), findsOneWidget);
      expect(tester.takeException(), isNull);
      // Without a title the SDK never consumes toolbarOpacity as a Color alpha.
      final withoutTitle = raw(values: props(opacity: 2));
      final bar = f.findNode(
        withoutTitle['root'] as Map<String, Object?>,
        barId,
      );
      (bar['slots'] as Map<String, Object?>).remove('title');
      await f.pump(tester, withoutTitle);
      expect(find.byType(FlexibleSpaceBar), findsOneWidget);
      expect(
        find.byWidgetPredicate(
          (w) => w is ColoredBox && w.color == const Color(0xFF224466),
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets('equal zero extents use native behavior without invented size', (
    tester,
  ) async {
    await f.pump(tester, raw(values: props(min: 0, max: 0, current: 0)));
    expect(tester.getSize(find.byType(FlexibleSpaceBar)), const Size(320, 240));
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'settings is a reviewed wrapper source and occupied Child is not an append target',
    (tester) async {
      CanvasDropResolver? drop;
      CanvasMovePreviewResolver? move;
      await f.pump(
        tester,
        raw(withBar: false),
        drop: (v) => drop = v,
        move: (v) => move = v,
      );
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getCenter(find.text('Settings title'));
      final target = drop!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'settings',
          widgetType: type,
          traits: {},
        ),
      );
      expect(target, isNotNull);
      expect(move!('missing', settingsId, 'child', 0), isNull);
      expect(
        canvasDropSlotsForWidgetType(type),
        isEmpty,
        reason:
            'Required occupied Child is edited/replaced through Slots, not appended',
      );
    },
  );
}
