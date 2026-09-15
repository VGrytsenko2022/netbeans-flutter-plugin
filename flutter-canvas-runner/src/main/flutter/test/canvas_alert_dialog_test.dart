import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_dialog_test.dart' as d;

Map<String, Object?> data({
  bool adaptive = false,
  Map<String, Object?> props = const {},
  bool children = true,
  TargetPlatform platform = TargetPlatform.android,
}) {
  final raw = f.modelJson();
  (raw['profile']! as Map<String, Object?>)['targetPlatform'] = platform.name;
  raw['root'] = f.node(
    f.fillId,
    adaptive ? canvasAdaptiveAlertDialogType : canvasAlertDialogType,
    props,
    {
      'icon': f.single(
        children
            ? f.node(f.headerId, 'flutter.widgets.Text', {'data': d.s('Icon')})
            : null,
      ),
      'title': f.single(
        children
            ? f.node(
                'f1637f52-e1a2-4ff9-8123-47a2f4d8083e',
                'flutter.widgets.Text',
                {'data': d.s('Title')},
              )
            : null,
      ),
      'content': f.single(
        children
            ? f.node(f.bodyId, 'flutter.widgets.SizedBox', {
                'width': f.number(120),
                'height': f.number(60),
              })
            : null,
      ),
      'actions': {
        'kind': 'list',
        'children': children
            ? [
                f.node(
                  '12d5c14c-9ba1-4d1c-a5d1-9e172c03a992',
                  'flutter.widgets.Text',
                  {'data': d.s('Action')},
                ),
              ]
            : [],
      },
    },
  );
  return raw;
}

Future<void> pump(WidgetTester tester, Map<String, Object?> raw) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: f.decode(raw),
      selectedWidgetId: f.fillId,
      onSelected: (_) {},
    ),
  );
  await tester.pumpAndSettle();
}

Finder alerts() => find.byWidgetPredicate((w) => w is AlertDialog);
void main() {
  test('exact reviewed Java/Dart contract and four drop slots', () {
    final actual = canvasRuntimeWidgetSchemaContractForTesting().trim().split(
          '\n',
        ),
        expected = canvasReviewedWidgetSchemaContract.trim().split('\n');
    for (var i = 0; i < actual.length && i < expected.length; i++) {
      expect(actual[i], expected[i], reason: 'contract line $i');
    }
    expect(actual.length, expected.length);
    for (final type in [canvasAlertDialogType, canvasAdaptiveAlertDialogType]) {
      expect(canvasDropSlotsForWidgetType(type).map((s) => s.slotName), [
        'icon',
        'title',
        'content',
        'actions',
      ]);
    }
  });
  for (final adaptive in [false, true]) {
    testWidgets(
      'empty and zero-sized alert stays selectable with drop target adaptive=$adaptive',
      (tester) async {
        final raw = data(adaptive: adaptive, children: false);
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
            onDropResolverChanged: (v) => drop = v,
          ),
        );
        await tester.pump();
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-${f.fillId}'),
        );
        expect(handle, findsOneWidget);
        await tester.tap(handle);
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
        expect(target?.slotName, isIn(['icon', 'title', 'content', 'actions']));
        expect(tester.takeException(), isNull);
      },
    );
    for (final platform in TargetPlatform.values.where(
      (p) => p != TargetPlatform.fuchsia,
    )) {
      testWidgets('native branches adaptive=$adaptive platform=$platform', (
        tester,
      ) async {
        await pump(tester, data(adaptive: adaptive, platform: platform));
        final cupertino =
            adaptive &&
            {TargetPlatform.iOS, TargetPlatform.macOS}.contains(platform);
        expect(alerts(), findsOneWidget);
        expect(
          find.byType(CupertinoAlertDialog),
          cupertino ? findsOneWidget : findsNothing,
        );
        expect(find.text('Icon'), cupertino ? findsNothing : findsOneWidget);
        expect(find.text('Title'), findsOneWidget);
        expect(find.text('Action'), findsOneWidget);
        expect(tester.takeException(), isNull);
      });
    }
    testWidgets(
      'native properties, local styles, identity and reset adaptive=$adaptive',
      (tester) async {
        final props = <String, Object?>{
          'key': d.s('first'),
          'backgroundColor': {'kind': 'color', 'argb': '0xFF112233'},
          'elevation': f.number(5),
          'shadowColor': {'kind': 'color', 'argb': '0xFF223344'},
          'surfaceTintColor': {'kind': 'color', 'argb': '0xFF334455'},
          'iconColor': {'kind': 'color', 'argb': '0xFF445566'},
          'semanticLabel': d.s('Alert'),
          for (final n in ['titleTextStyle', 'contentTextStyle']) ...{
            '${n}FontSize': f.number(21),
            '${n}FontWeight': d.en('FontWeight', 'w700'),
          },
          for (final n in [
            'iconPadding',
            'titlePadding',
            'contentPadding',
            'actionsPadding',
            'buttonPadding',
          ])
            n: {
              'kind': 'edgeInsetsDirectional',
              'start': 4,
              'top': 6,
              'end': 8,
              'bottom': 10,
            },
          'insetPadding': d.insets,
          'scrollable': f.boolean(true),
          'actionsAlignment': d.en('MainAxisAlignment', 'spaceBetween'),
          'actionsOverflowAlignment': d.en('OverflowBarAlignment', 'center'),
          'actionsOverflowDirection': d.en('VerticalDirection', 'up'),
          'actionsOverflowButtonSpacing': f.number(-2),
          'clipBehavior': d.en('Clip', 'antiAlias'),
          'shapeKind': d.s('stadium'),
        };
        await pump(tester, data(adaptive: adaptive, props: props));
        var native = tester.widget<AlertDialog>(alerts());
        expect(native.key, const ValueKey('first'));
        expect(native.elevation, 5);
        expect(native.backgroundColor, const Color(0xff112233));
        expect(native.titleTextStyle?.fontSize, 21);
        expect(native.contentTextStyle?.fontWeight, FontWeight.w700);
        expect(
          native.iconPadding,
          const EdgeInsetsDirectional.fromSTEB(4, 6, 8, 10),
        );
        expect(native.actionsOverflowDirection, VerticalDirection.up);
        expect(native.actionsOverflowButtonSpacing, -2);
        expect(native.shape, isA<StadiumBorder>());
        expect(native.scrollable, isTrue);
        expect(native.semanticLabel, 'Alert');
        final element = tester.element(alerts());
        props['elevation'] = f.number(7);
        await pump(tester, data(adaptive: adaptive, props: props));
        expect(tester.element(alerts()), same(element));
        props['key'] = d.s('second');
        await pump(tester, data(adaptive: adaptive, props: props));
        expect(tester.element(alerts()), isNot(same(element)));
        await pump(tester, data(adaptive: adaptive));
        native = tester.widget<AlertDialog>(alerts());
        expect(native.shape, isNull);
        expect(native.titleTextStyle, isNull);
        expect(native.scrollable, isFalse);
        expect(tester.takeException(), isNull);
      },
    );
    for (final shape in d.kinds) {
      testWidgets('native shape $shape adaptive=$adaptive', (tester) async {
        await pump(
          tester,
          data(adaptive: adaptive, props: {'shapeKind': d.s(shape)}),
        );
        expect(tester.widget<AlertDialog>(alerts()).shape, isNotNull);
        expect(tester.takeException(), isNull);
      });
    }
    testWidgets(
      'sources remain opaque and Cupertino ignores unavailable Material shape adaptive=$adaptive',
      (tester) async {
        final props = <String, Object?>{
          for (final n in [
            'key',
            'iconPadding',
            'iconColor',
            'titleTextStyle',
            'contentTextStyle',
            'semanticLabel',
            'alignment',
            'constraints',
          ])
            n: d.presence,
          if (adaptive) ...{
            'scrollController': d.presence,
            'actionScrollController': d.presence,
            'insetAnimationDurationUs': d.presence,
            'insetAnimationCurve': d.presence,
          },
        };
        await pump(tester, data(adaptive: adaptive, props: props));
        expect(alerts(), findsOneWidget);
        expect(d.tooltips(tester), contains('Project sources not executed'));
        props['shape'] = d.presence;
        await pump(tester, data(adaptive: adaptive, props: props));
        expect(alerts(), findsNothing);
        if (adaptive) {
          await tester.pumpWidget(const SizedBox());
          await pump(
            tester,
            data(adaptive: true, props: props, platform: TargetPlatform.iOS),
          );
          expect(find.byType(CupertinoAlertDialog), findsOneWidget);
        }
        expect(tester.takeException(), isNull);
      },
    );
    test(
      'nullable fields, directional insets and compound conflicts adaptive=$adaptive',
      () {
        f.decode(
          data(
            adaptive: adaptive,
            props: {
              'titleTextStyle': d.nil,
              'contentTextStyle': d.nil,
              'iconPadding': d.nil,
            },
          ),
        );
        for (final p in ['titleTextStyle', 'contentTextStyle']) {
          expect(
            () => f.decode(
              data(
                adaptive: adaptive,
                props: {p: d.nil, '${p}FontSize': f.number(10)},
              ),
            ),
            throwsFormatException,
          );
          expect(
            () => f.decode(
              data(adaptive: adaptive, props: {'${p}Package': d.s('fonts')}),
            ),
            throwsFormatException,
          );
        }
        expect(
          () => f.decode(
            data(
              adaptive: adaptive,
              props: {'shape': d.nil, 'shapeKind': d.s('circle')},
            ),
          ),
          throwsFormatException,
        );
        expect(
          () =>
              f.decode(data(adaptive: adaptive, props: {'scrollable': d.nil})),
          throwsFormatException,
        );
        if (adaptive) {
          for (final p in [
            'insetPadding',
            'insetAnimationDurationUs',
            'insetAnimationCurve',
          ]) {
            expect(
              () => f.decode(data(adaptive: true, props: {p: d.nil})),
              throwsFormatException,
            );
          }
        } else {
          f.decode(data(props: {'insetPadding': d.nil}));
          expect(
            () => f.decode(data(props: {'scrollController': d.nil})),
            throwsFormatException,
          );
        }
      },
    );
  }
  for (final curve in canvasExpansionCurvePresets) {
    testWidgets('Cupertino animation curve $curve', (tester) async {
      await pump(
        tester,
        data(
          adaptive: true,
          platform: TargetPlatform.iOS,
          props: {
            'insetAnimationDurationUs': {'kind': 'integer', 'value': 220000},
            'insetAnimationCurve': d.s(curve),
          },
        ),
      );
      final native = tester.widget<CupertinoAlertDialog>(
        find.byType(CupertinoAlertDialog),
      );
      expect(native.insetAnimationDuration, const Duration(milliseconds: 220));
      expect(native.insetAnimationCurve, isNotNull);
      expect(tester.takeException(), isNull);
    });
  }
}
