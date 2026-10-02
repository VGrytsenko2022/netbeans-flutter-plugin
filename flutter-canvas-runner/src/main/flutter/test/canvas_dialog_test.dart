import 'dart:ui' show SemanticsRole;
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> en(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
const nil = {'kind': 'null'};
const presence = {'kind': 'dartObjectReferencePresence'};
const insets = {
  'kind': 'edgeInsets',
  'left': 8,
  'top': 10,
  'right': 12,
  'bottom': 14,
};
Map<String, Object?> data({
  bool full = false,
  Map<String, Object?> props = const {},
  bool child = true,
}) {
  final raw = f.modelJson();
  raw['root'] = f.node(
    f.fillId,
    full ? canvasFullscreenDialogType : canvasDialogType,
    props,
    {
      'child': f.single(
        child
            ? f.node(f.bodyId, 'flutter.widgets.SizedBox', {
                'width': f.number(160),
                'height': f.number(80),
              })
            : null,
      ),
    },
  );
  return raw;
}

const kinds = [
  'roundedRectangle',
  'beveledRectangle',
  'continuousRectangle',
  'roundedSuperellipse',
  'circle',
  'oval',
  'stadium',
  'linear',
  'star',
  'polygon',
];
Future<void> pump(WidgetTester tester, CanvasModel model) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: model,
      selectedWidgetId: f.fillId,
      onSelected: (_) {},
    ),
  );
  await tester.pump();
}

String tooltips(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((t) => t.message ?? '')
    .join(' ');
void main() {
  for (final full in [false, true]) {
    testWidgets(
      'zero-sized dialog remains selectable and accepts child drop full=$full',
      (tester) async {
        final raw = data(full: full, child: false);
        raw['root'] = f.node(f.headerId, 'flutter.widgets.Center', {}, {
          'child': f.single(
            f.node(
              f.bodyId,
              'flutter.widgets.SizedBox',
              {'width': f.number(0), 'height': f.number(0)},
              {'child': f.single(raw['root'] as Map<String, Object?>)},
            ),
          ),
        });
        final selected = <String>[];
        CanvasDropResolver? drop;
        await tester.pumpWidget(
          CanvasModelApp(
            model: f.decode(raw),
            selectedWidgetId: f.fillId,
            onSelected: selected.add,
            onDropResolverChanged: (value) => drop = value,
          ),
        );
        await tester.pump();
        expect(tester.getSize(find.byType(Dialog)), Size.zero);
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-${f.fillId}'),
        );
        expect(handle, findsOneWidget);
        await tester.tap(handle);
        await tester.pump();
        expect(selected, contains(f.fillId));
        final surface = tester.getRect(find.byType(CanvasDocumentView)),
            point = tester.getCenter(handle);
        final target = drop!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(
            token: 'text',
            widgetType: 'flutter.widgets.Text',
            traits: canvasWidgetTraitsForType('flutter.widgets.Text'),
          ),
        );
        expect(target?.parentWidgetId, f.fillId);
        expect(target?.slotName, 'child');
        expect(tester.takeException(), isNull);
      },
    );
  }
  for (final full in [false, true]) {
    testWidgets('literal Key controls native identity full=$full', (
      tester,
    ) async {
      await pump(
        tester,
        f.decode(data(full: full, props: {'key': s('first')})),
      );
      final first = tester.element(find.byType(Dialog));
      expect(first.widget.key, const ValueKey('first'));
      await pump(
        tester,
        f.decode(
          data(
            full: full,
            props: {'key': s('first'), 'insetAnimationCurve': s('linear')},
          ),
        ),
      );
      expect(tester.element(find.byType(Dialog)), same(first));
      await pump(
        tester,
        f.decode(data(full: full, props: {'key': s('second')})),
      );
      expect(tester.element(find.byType(Dialog)), isNot(same(first)));
      expect(tester.takeException(), isNull);
    });
  }
  test('exact Java/Dart schema, roles and optional child drop contract', () {
    expect(
      canvasRuntimeWidgetSchemaContractForTesting().trim(),
      canvasReviewedWidgetSchemaContract.trim(),
    );
    expect(
      canvasDialogSemanticsRoles,
      SemanticsRole.values.map((e) => e.name).toSet(),
    );
    for (final type in [canvasDialogType, canvasFullscreenDialogType]) {
      final slots = canvasDropSlotsForWidgetType(type);
      expect(slots, hasLength(1));
      expect(slots.single.slotName, 'child');
    }
    for (final name in [
      'elevation',
      'shadowColor',
      'surfaceTintColor',
      'insetPadding',
      'clipBehavior',
      'shape',
      'shapeKind',
      'alignment',
      'constraints',
    ]) {
      expect(
        () => f.decode(data(full: true, props: {name: nil})),
        throwsFormatException,
        reason: name,
      );
    }
  });
  test(
    'nonnegative physical insets, durations and compound shape validation',
    () {
      f.decode(
        data(
          props: {
            'insetPadding': insets,
            'insetAnimationDurationUs': {'kind': 'integer', 'value': 0},
          },
        ),
      );
      for (final name in ['elevation', 'insetAnimationDurationUs']) {
        expect(
          () => f.decode(
            data(
              props: {
                name: {'kind': 'integer', 'value': -1},
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final name in [
        'insetAnimationDurationUs',
        'insetAnimationCurve',
        'semanticsRole',
      ]) {
        expect(() => f.decode(data(props: {name: nil})), throwsFormatException);
      }
      expect(
        () => f.decode(
          data(
            props: {
              'insetPadding': {
                'kind': 'edgeInsetsDirectional',
                'start': 1,
                'top': 2,
                'end': 3,
                'bottom': 4,
              },
            },
          ),
        ),
        throwsFormatException,
      );
      expect(
        () => f.decode(
          data(
            props: {
              'insetPadding': {...insets, 'left': -1},
            },
          ),
        ),
        throwsFormatException,
      );
      expect(
        () => f.decode(data(props: {'shape': nil, 'shapeKind': s('circle')})),
        throwsFormatException,
      );
      expect(
        () => f.decode(
          data(
            props: {
              'shapeKind': s('star'),
              'shapePointRounding': f.number(.8),
              'shapeValleyRounding': f.number(.8),
            },
          ),
        ),
        throwsFormatException,
      );
      expect(
        () => f.decode(data(props: {'shapeKind': s('unknown')})),
        throwsFormatException,
      );
      for (final name in [
        'key',
        'backgroundColor',
        'elevation',
        'shadowColor',
        'surfaceTintColor',
        'insetPadding',
        'clipBehavior',
        'shape',
        'alignment',
        'constraints',
      ]) {
        f.decode(data(props: {name: nil}));
      }
    },
  );
  for (final full in [false, true]) {
    testWidgets('native Canvas constructor defaults full=$full', (
      tester,
    ) async {
      await pump(tester, f.decode(data(full: full)));
      final d = tester.widget<Dialog>(find.byType(Dialog));
      expect(
        d.insetAnimationDuration,
        full ? Duration.zero : const Duration(milliseconds: 100),
      );
      expect(d.insetAnimationCurve, Curves.decelerate);
      expect(d.semanticsRole, SemanticsRole.dialog);
      expect(d.child, isNotNull);
      expect(d.elevation, full ? 0 : null);
      expect(d.insetPadding, full ? EdgeInsets.zero : null);
      expect(d.backgroundColor, isNull);
      expect(d.constraints, isNull);
      expect(d.shape, isNull);
      expect(tester.takeException(), isNull);
      await pump(tester, f.decode(data(full: full, child: false)));
      expect(find.byType(Dialog), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
    for (final curve in canvasExpansionCurvePresets) {
      testWidgets('all inset curves $full $curve', (tester) async {
        await pump(
          tester,
          f.decode(
            data(
              full: full,
              props: {
                'insetAnimationCurve': s(curve),
                'insetAnimationDurationUs': {'kind': 'integer', 'value': 1234},
              },
            ),
          ),
        );
        final d = tester.widget<Dialog>(find.byType(Dialog));
        expect(d.insetAnimationDuration, const Duration(microseconds: 1234));
        expect(d.insetAnimationCurve, isA<Curve>());
        expect(tester.takeException(), isNull);
      });
    }
    for (final role in canvasDialogSemanticsRoles) {
      testWidgets('semantics $full $role', (tester) async {
        await pump(
          tester,
          f.decode(
            data(
              full: full,
              props: {'semanticsRole': en('SemanticsRole', role)},
            ),
          ),
        );
        expect(
          tester.widget<Dialog>(find.byType(Dialog)).semanticsRole.name,
          role,
        );
        expect(tester.takeException(), isNull);
      });
    }
  }
  for (final kind in kinds) {
    testWidgets('native shape $kind', (tester) async {
      await pump(
        tester,
        f.decode(
          data(
            props: {
              'shapeKind': s(kind),
              'shapeSideWidth': f.number(2),
              'shapeSideColor': {'kind': 'color', 'argb': '0xFF123456'},
            },
          ),
        ),
      );
      final d = tester.widget<Dialog>(find.byType(Dialog));
      expect(d.shape, isA<ShapeBorder>());
      expect(tester.takeException(), isNull);
      if (kind == 'polygon') expect(d.shape, isA<StarBorder>());
    });
  }
  testWidgets(
    'every local argument updates the native Dialog without replacing child identity',
    (tester) async {
      await pump(tester, f.decode(data()));
      final before = tester.element(find.byType(Dialog));
      await pump(
        tester,
        f.decode(
          data(
            props: {
              'backgroundColor': {'kind': 'color', 'argb': '0xFF123456'},
              'elevation': f.number(7),
              'shadowColor': {'kind': 'color', 'argb': '0xFF0000FF'},
              'surfaceTintColor': {'kind': 'color', 'argb': '0x00000000'},
              'insetPadding': insets,
              'clipBehavior': en('Clip', 'antiAlias'),
              'shapeKind': s('circle'),
              'alignment': {
                'kind': 'alignmentGeometry',
                'basis': 'directional',
                'horizontal': -1,
                'vertical': 1,
              },
              'constraints': {
                'kind': 'boxConstraints',
                'minWidth': 100,
                'maxWidth': 400,
                'minHeight': 0,
                'maxHeight': 300,
              },
            },
          ),
        ),
      );
      final d = tester.widget<Dialog>(find.byType(Dialog));
      expect(tester.element(find.byType(Dialog)), same(before));
      expect(d.backgroundColor, const Color(0xFF123456));
      expect(d.elevation, 7);
      expect(d.shadowColor, const Color(0xFF0000FF));
      expect(d.insetPadding, const EdgeInsets.fromLTRB(8, 10, 12, 14));
      expect(d.alignment, AlignmentDirectional.bottomStart);
      expect(
        d.constraints,
        const BoxConstraints(minWidth: 100, maxWidth: 400, maxHeight: 300),
      );
      expect(d.clipBehavior, Clip.antiAlias);
      expect(d.shape, isA<CircleBorder>());
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'project sources are disclosed and never executed; custom shape stays unavailable',
    (tester) async {
      final props = {
        for (final name in [
          'key',
          'backgroundColor',
          'elevation',
          'shadowColor',
          'surfaceTintColor',
          'insetPadding',
          'alignment',
          'constraints',
          'insetAnimationDurationUs',
          'insetAnimationCurve',
        ])
          name: presence,
      };
      var model = f.decode(data(props: props));
      await pump(tester, model);
      expect(find.byType(Dialog), findsOneWidget);
      expect(tester.takeException(), isNull);
      expect(tooltips(tester), contains('project sources'));
      expect(tooltips(tester), contains('decelerate'));
      model = f.decode(data(props: {'shape': presence}));
      await pump(tester, model);
      expect(find.byType(Dialog), findsNothing);
      expect(tooltips(tester), contains('ShapeBorder'));
      expect(tester.takeException(), isNull);
    },
  );
  for (final full in [false, true]) {
    for (final m3 in [false, true]) {
      for (final dark in [false, true]) {
        testWidgets('native theme defaults full=$full m3=$m3 dark=$dark', (
          tester,
        ) async {
          final theme = ThemeData(
            useMaterial3: m3,
            brightness: dark ? Brightness.dark : Brightness.light,
            dialogTheme: const DialogThemeData(
              backgroundColor: Colors.green,
              insetPadding: EdgeInsets.all(12),
              alignment: Alignment.topRight,
              shape: StadiumBorder(),
              constraints: BoxConstraints(minWidth: 333),
            ),
          );
          await tester.pumpWidget(
            MaterialApp(
              theme: theme,
              home: Scaffold(
                body: full
                    ? const Dialog.fullscreen(
                        child: SizedBox(width: 120, height: 80),
                      )
                    : const Dialog(child: SizedBox(width: 120, height: 80)),
              ),
            ),
          );
          await tester.pumpAndSettle();
          final material = tester.widget<Material>(
            find
                .descendant(
                  of: find.byType(Dialog),
                  matching: find.byType(Material),
                )
                .first,
          );
          expect(material.color, Colors.green);
          expect(material.shape, full ? null : isA<StadiumBorder>());
          if (!full) {
            expect(
              tester
                  .getSize(
                    find
                        .descendant(
                          of: find.byType(Dialog),
                          matching: find.byType(Material),
                        )
                        .first,
                  )
                  .width,
              333,
            );
          }
          expect(tester.takeException(), isNull);
        });
      }
    }
  }
  testWidgets('keyboard insets animate and are removed below the dialog', (
    tester,
  ) async {
    EdgeInsets? observed;
    Widget app(double bottom) => MaterialApp(
      home: MediaQuery(
        data: MediaQueryData(
          size: const Size(800, 600),
          viewInsets: EdgeInsets.only(bottom: bottom),
        ),
        child: Dialog(
          insetPadding: const EdgeInsets.all(8),
          insetAnimationDuration: const Duration(milliseconds: 200),
          insetAnimationCurve: Curves.linear,
          child: Builder(
            builder: (context) {
              observed = MediaQuery.viewInsetsOf(context);
              return const SizedBox(width: 100, height: 80);
            },
          ),
        ),
      ),
    );
    await tester.pumpWidget(app(0));
    await tester.pumpAndSettle();
    final start = tester.getCenter(find.byType(Material).last).dy;
    await tester.pumpWidget(app(100));
    await tester.pump(const Duration(milliseconds: 100));
    final middle = tester.getCenter(find.byType(Material).last).dy;
    await tester.pumpAndSettle();
    final end = tester.getCenter(find.byType(Material).last).dy;
    expect(start, greaterThan(middle));
    expect(middle, greaterThan(end));
    expect(observed, EdgeInsets.zero);
    expect(tester.takeException(), isNull);
  });
  testWidgets('route result belongs to a child handler, not to Dialog Events', (
    tester,
  ) async {
    String? result;
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => TextButton(
            onPressed: () async {
              result = await showDialog<String>(
                context: context,
                builder: (context) => Dialog(
                  child: TextButton(
                    onPressed: () => Navigator.pop(context, 'saved'),
                    child: const Text('Confirm'),
                  ),
                ),
              );
            },
            child: const Text('Open'),
          ),
        ),
      ),
    );
    await tester.tap(find.text('Open'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Confirm'));
    await tester.pumpAndSettle();
    expect(result, 'saved');
    expect(find.byType(Dialog), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
