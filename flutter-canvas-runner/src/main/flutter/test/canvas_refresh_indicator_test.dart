import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.RefreshIndicator';
const _id = 'f1dd3aa1-fc2f-45b7-a9fc-0c8b5e1ac511';
const _listId = '7c37c659-01b5-46e5-852f-11abdb77504c';
const _boxId = 'ca8c98a2-0aa1-4e41-bd02-8c123ecdbdad';
const _reference = {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _number(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _node(
  String id,
  String type,
  Map<String, Object?> properties, [
  Map<String, Object?> slots = const {},
]) => {
  'id': id,
  'type': type,
  'properties': properties,
  'slots': <String, Object?>{...slots},
};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};

Map<String, Object?> _model({
  String platform = 'windows',
  String variant = 'material',
  Map<String, Object?> properties = const {},
  double width = 300,
  double height = 400,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  final list = _node(
    _listId,
    'flutter.widgets.ListView',
    {'physics': _string('alwaysScrollable')},
    {
      'children': {
        'kind': 'list',
        'children': [
          _node(
            '374275df-4de3-4b1b-b542-56567108ff16',
            'flutter.widgets.SizedBox',
            {'height': _number(1400)},
            {
              'child': _single(
                _node(
                  '1934f733-85ab-40bb-8c7a-360cdd4fccfe',
                  'flutter.widgets.Text',
                  {'data': _string('Editable scroll child')},
                ),
              ),
            },
          ),
        ],
      },
    },
  );
  final refresh = _node(
    _id,
    _type,
    {'variant': _string(variant), ...properties},
    {'child': _single(list)},
  );
  final box = _node(
    _boxId,
    'flutter.widgets.SizedBox',
    {'width': _number(width), 'height': _number(height)},
    {'child': _single(refresh)},
  );
  model['root'] = _node(
    'e5e23da0-f3e6-46fa-88fb-0ab84d966dd3',
    'flutter.material.Scaffold',
    {},
    {
      'body': _single(
        _node(
          '29154d08-5472-4eae-8d4d-9352da0bbd52',
          'flutter.widgets.Center',
          {},
          {'child': _single(box)},
        ),
      ),
    },
  );
  return model;
}

Map _box(Map model) =>
    ((((model['root'] as Map)['slots'] as Map)['body'] as Map)['child']
            as Map)['slots']['child']['child']
        as Map;
Map _refresh(Map model) =>
    (_box(model)['slots']['child'] as Map)['child'] as Map;
Map _list(Map model) =>
    (_refresh(model)['slots']['child'] as Map)['child'] as Map;
CanvasModel _decode(Map model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder get _indicator => find.byType(RefreshIndicator);
RefreshIndicator _sdk(WidgetTester tester) =>
    tester.widget<RefreshIndicator>(_indicator);
String _diagnostics(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((t) => t.message ?? '')
    .join('\n');

Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  void Function(CanvasDropResolver?)? onDrop,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: (theme ?? ThemeData()).copyWith(
        platform: canvasAdaptiveTargetPlatform(
          (model['profile'] as Map)['targetPlatform'] as String,
        ),
      ),
      themeAnimationDuration: Duration.zero,
      home: CanvasDocumentView(
        model: _decode(model),
        selectedWidgetId: null,
        onSelected: selected?.add ?? (_) {},
        onDropResolverChanged: onDrop,
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 100));
  await tester.pump();
}

Future<TestGesture> _pull(WidgetTester tester) async {
  final gesture = await tester.startGesture(
    tester.getCenter(find.byType(ListView)),
  );
  await gesture.moveBy(const Offset(0, 20));
  await tester.pump();
  await gesture.moveBy(const Offset(0, 220));
  await tester.pump();
  return gesture;
}

Future<void> _finish(WidgetTester tester) async {
  for (var i = 0; i < 6; i++) {
    await tester.pump(const Duration(milliseconds: 300));
  }
}

CustomPainter _painter(WidgetTester tester) => tester
    .widget<CustomPaint>(
      find.byWidgetPredicate(
        (w) =>
            w is CustomPaint &&
            w.painter?.runtimeType.toString() ==
                '_RefreshProgressIndicatorPainter',
      ),
    )
    .painter!;
Future<Uint8List> _painted(WidgetTester tester, CustomPainter painter) async =>
    (await tester.runAsync(() async {
      final recorder = ui.PictureRecorder();
      painter.paint(Canvas(recorder), const Size(17, 17));
      final picture = recorder.endRecording();
      final image = await picture.toImage(17, 17);
      try {
        return (await image.toByteData(
          format: ui.ImageByteFormat.rawRgba,
        ))!.buffer.asUint8List();
      } finally {
        image.dispose();
        picture.dispose();
      }
    }))!;

void main() {
  test(
    'RefreshIndicator exact schema, constructors and typed reference markers',
    () {
      final model = _model(
        properties: {
          'displacement': _number(48),
          'edgeOffset': _number(-12.5),
          'onRefresh': _reference,
          'color': {
            'kind': 'themeToken',
            'token': 'material.colorScheme.primary',
          },
          'backgroundColor': {'kind': 'color', 'argb': '0xFF112233'},
          'notificationPredicate': _reference,
          'semanticsLabel': _string('Refresh feed'),
          'semanticsValue': _string('arbitrary stored value'),
          'strokeWidth': _number(-2.5),
          'triggerMode': _enum('RefreshIndicatorTriggerMode', 'anywhere'),
          'elevation': _number(3),
        },
      );
      expect(_decode(model).root.type, 'flutter.material.Scaffold');
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), true);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      final noSpinner = _model(
        variant: 'noSpinner',
        properties: {'onStatusChange': _reference},
      );
      expect(_decode(noSpinner).root.type, 'flutter.material.Scaffold');
      for (final preset in ['default', 'depthZero', 'all']) {
        expect(
          _decode(
            _model(properties: {'notificationPredicate': _string(preset)}),
          ).root.type,
          'flutter.material.Scaffold',
        );
      }
    },
  );

  test(
    'RefreshIndicator rejects unknown branches, missing child and unsafe wire values',
    () {
      for (final name in [
        'displacement',
        'edgeOffset',
        'color',
        'backgroundColor',
        'strokeWidth',
      ]) {
        final model = _model(
          variant: 'noSpinner',
          properties: {
            name: name.contains('olor')
                ? {'kind': 'color', 'argb': '0xFF112233'}
                : _number(0),
          },
        );
        expect(() => _decode(model), throwsFormatException, reason: name);
      }
      for (final variant in ['material', 'adaptive']) {
        expect(
          () => _decode(
            _model(
              variant: variant,
              properties: {'onStatusChange': _reference},
            ),
          ),
          throwsFormatException,
        );
      }
      for (final properties in <Map<String, Object?>>[
        {
          'strokeWidth': {'kind': 'null'},
        },
        {
          'onRefresh': {'kind': 'callbackPresence'},
        },
        {'notificationPredicate': _string('depthOne')},
        {'variant': _string('other')},
        {'displacement': _number(-1)},
        {'elevation': _number(-1)},
        {'strokeWidth': _enum('double', 'infinity')},
        {'controller': _reference},
        {'value': _number(.5)},
        {
          'onRefresh': {
            'kind': 'dartObjectReferencePresence',
            'expression': 'runProject()',
          },
        },
      ]) {
        expect(
          () => _decode(_model(properties: properties)),
          throwsFormatException,
        );
      }
      final missing = _model();
      _refresh(missing)['slots']['child'] = _single(null);
      expect(() => _decode(missing), throwsFormatException);
    },
  );

  for (final platform in ['windows', 'web']) {
    for (final variant in ['material', 'adaptive', 'noSpinner']) {
      testWidgets(
        'RefreshIndicator $variant exact SDK wrapper/child and no property defaults on $platform',
        (tester) async {
          final model = _model(platform: platform, variant: variant);
          final before = jsonEncode(model);
          await _pump(tester, model);
          expect(_indicator, findsOneWidget);
          expect(find.text('Editable scroll child'), findsOneWidget);
          expect(find.byType(RefreshProgressIndicator), findsNothing);
          expect(_sdk(tester).displacement, variant == 'noSpinner' ? 0 : 40);
          expect(_sdk(tester).strokeWidth, variant == 'noSpinner' ? 0 : 2.5);
          final state = tester.state<RefreshIndicatorState>(_indicator);
          final gesture = await _pull(tester);
          expect(
            find.byType(RefreshProgressIndicator),
            variant == 'noSpinner' ? findsNothing : findsOneWidget,
          );
          await gesture.up();
          await _finish(tester);
          expect(find.byType(RefreshProgressIndicator), findsNothing);
          expect(tester.state(_indicator), same(state));
          expect(jsonEncode(model), before);
          expect(tester.takeException(), isNull);
        },
      );
    }

    testWidgets(
      'RefreshIndicator all visual fields/theme and retained active state on $platform',
      (tester) async {
        final model = _model(
          platform: platform,
          properties: {
            'displacement': _number(55),
            'edgeOffset': _number(-6),
            'strokeWidth': _number(-2),
            'elevation': _number(6),
            'color': {'kind': 'color', 'argb': '0x8000FF00'},
            'backgroundColor': {
              'kind': 'themeToken',
              'token': 'material.colorScheme.secondary',
            },
            'semanticsLabel': _string('Refresh feed'),
            'semanticsValue': _string('45%'),
            'triggerMode': _enum('RefreshIndicatorTriggerMode', 'anywhere'),
          },
        );
        for (final m3 in [false, true]) {
          final theme = ThemeData(useMaterial3: m3);
          await _pump(tester, model, theme: theme);
          final state = tester.state<RefreshIndicatorState>(_indicator);
          final gesture = await _pull(tester);
          final progress = tester.widget<RefreshProgressIndicator>(
            find.byType(RefreshProgressIndicator),
          );
          expect(progress.strokeWidth, -2);
          expect(progress.elevation, 6);
          expect(progress.backgroundColor, theme.colorScheme.secondary);
          expect(progress.semanticsLabel, 'Refresh feed');
          expect(progress.semanticsValue, '45%');
          expect(progress.valueColor!.value!.g, 1);
          expect(
            _sdk(tester).triggerMode,
            RefreshIndicatorTriggerMode.anywhere,
          );
          (_refresh(model)['properties'] as Map)['displacement'] = _number(60);
          await _pump(tester, model, theme: theme);
          expect(tester.state(_indicator), same(state));
          expect(find.byType(RefreshProgressIndicator), findsOneWidget);
          await gesture.up();
          await _finish(tester);
          expect(tester.takeException(), isNull);
        }
      },
    );

    for (final property in ['onRefresh', 'notificationPredicate']) {
      testWidgets(
        'RefreshIndicator $property isolation preserves scrolling and never fakes success on $platform',
        (tester) async {
          final model = _model(
            platform: platform,
            properties: {property: _reference},
          );
          await _pump(tester, model);
          expect(
            _diagnostics(tester),
            contains('Refresh activation is disabled'),
          );
          final gesture = await _pull(tester);
          expect(find.byType(RefreshProgressIndicator), findsNothing);
          await gesture.up();
          await _finish(tester);
          await tester.drag(find.byType(ListView), const Offset(0, -200));
          await tester.pump();
          expect(
            tester
                .state<ScrollableState>(find.byType(Scrollable))
                .position
                .pixels,
            greaterThan(0),
          );
          if (property == 'onRefresh') {
            var completed = false;
            unawaited(_sdk(tester).onRefresh().then((_) => completed = true));
            await tester.pump();
            expect(completed, false);
          }
          expect(tester.takeException(), isNull);
        },
      );
    }

    testWidgets(
      'RefreshIndicator noSpinner status reference is diagnosed without blocking SDK cycle on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(
            platform: platform,
            variant: 'noSpinner',
            properties: {'onStatusChange': _reference},
          ),
        );
        expect(
          _diagnostics(tester),
          contains('status observer is not invoked'),
        );
        expect(_sdk(tester).onStatusChange, isNull);
        final gesture = await _pull(tester);
        await gesture.up();
        await _finish(tester);
        var completed = false;
        unawaited(_sdk(tester).onRefresh().then((_) => completed = true));
        await tester.pump();
        expect(completed, true);
        expect(find.byType(RefreshProgressIndicator), findsNothing);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'RefreshIndicator reviewed predicate depth and horizontal filtering on $platform',
      (tester) async {
        for (final preset in ['default', 'depthZero', 'all']) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {'notificationPredicate': _string(preset)},
            ),
          );
          final context = tester.element(find.text('Editable scroll child'));
          final notification = ScrollStartNotification(
            metrics: FixedScrollMetrics(
              minScrollExtent: 0,
              maxScrollExtent: 100,
              pixels: 0,
              viewportDimension: 400,
              axisDirection: AxisDirection.down,
              devicePixelRatio: 1,
            ),
            context: context,
          );
          expect(_sdk(tester).notificationPredicate(notification), true);
          // Dispatch across an inner viewport increments depth, unlike manually
          // assigning synthetic depth that the SDK does not expose as a setter.
          final horizontal = ScrollStartNotification(
            metrics: FixedScrollMetrics(
              minScrollExtent: 0,
              maxScrollExtent: 100,
              pixels: 0,
              viewportDimension: 400,
              axisDirection: AxisDirection.right,
              devicePixelRatio: 1,
            ),
            context: context,
          );
          horizontal.dispatch(context);
          await tester.pump();
          expect(find.byType(RefreshProgressIndicator), findsNothing);
        }
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'RefreshIndicator resolved unsafe spinner diagnostics never replace child on $platform',
      (tester) async {
        for (final entry in [
          (
            properties: <String, Object?>{'semanticsValue': _string('loading')},
            width: 300.0,
            message: 'semanticsValue',
          ),
          (
            properties: <String, Object?>{'strokeWidth': _number(1e308)},
            width: 300.0,
            message: 'strokeWidth',
          ),
          (properties: <String, Object?>{}, width: 35.0, message: 'non-square'),
        ]) {
          await tester.pumpWidget(const SizedBox());
          final model = _model(
            platform: platform,
            properties: entry.properties,
            width: entry.width,
          );
          await _pump(tester, model);
          final gesture = await _pull(tester);
          await tester.pump();
          expect(_diagnostics(tester), contains(entry.message));
          expect(_indicator, findsOneWidget);
          expect(find.byType(ListView), findsOneWidget);
          expect(find.byType(RefreshProgressIndicator), findsNothing);
          await gesture.up();
          await _finish(tester);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'RefreshIndicator safe zero/tiny child remains selectable and wrapper drop is atomic on $platform',
      (tester) async {
        for (final size in [0.0, .001, 300.0]) {
          final selected = <String>[];
          await _pump(
            tester,
            _model(platform: platform, width: size, height: size),
            selected: selected,
          );
          final target = size == 0
              ? find.byKey(
                  const ValueKey('canvas-zero-size-widget-target-group-$_id'),
                )
              : _widget(_id);
          expect(target, findsOneWidget);
          await tester.tap(target, warnIfMissed: false);
          await tester.pump();
          expect(
            selected,
            contains(size == 0 ? _id : '1934f733-85ab-40bb-8c7a-360cdd4fccfe'),
          );
          expect(tester.takeException(), isNull);
        }
        CanvasDropResolver? drop;
        final model = _model(platform: platform);
        _box(model)['slots']['child'] = _single(_list(model));
        await _pump(tester, model, onDrop: (r) => drop = r);
        final point = tester.getCenter(_widget(_listId));
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final target = drop!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(
            token: 'refresh-wrapper',
            widgetType: _type,
            traits: {},
          ),
        );
        expect(target?.parentWidgetId, '374275df-4de3-4b1b-b542-56567108ff16');
        expect(target?.slotName, 'child');
        expect(tester.takeException(), isNull);
      },
    );
  }

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'RefreshIndicator nested notification presets are not silently substituted on $platform',
      (tester) async {
        for (final preset in ['default', 'depthZero', 'all']) {
          await tester.pumpWidget(const SizedBox());
          final model = _model(
            platform: platform,
            properties: {'notificationPredicate': _string(preset)},
          );
          final list = _list(model);
          _refresh(model)['slots']['child'] = _single(
            _node(
              'f6480d29-b8a4-4dc7-85f7-d517b6cdf368',
              'flutter.widgets.SingleChildScrollView',
              {'scrollDirection': _enum('Axis', 'horizontal')},
              {
                'child': _single(
                  _node(
                    '52f7536c-b092-47dc-aa34-ad28d8bad093',
                    'flutter.widgets.SizedBox',
                    {'width': _number(300)},
                    {'child': _single(list)},
                  ),
                ),
              },
            ),
          );
          await _pump(tester, model);
          final gesture = await _pull(tester);
          expect(
            find.byType(RefreshProgressIndicator),
            preset == 'all' ? findsOneWidget : findsNothing,
          );
          await gesture.up();
          await _finish(tester);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'RefreshIndicator onEdge versus anywhere when drag begins away from edge on $platform',
      (tester) async {
        for (final mode in ['onEdge', 'anywhere']) {
          await tester.pumpWidget(const SizedBox());
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {
                'triggerMode': _enum('RefreshIndicatorTriggerMode', mode),
              },
            ),
          );
          tester
              .state<ScrollableState>(find.byType(Scrollable))
              .position
              .jumpTo(150);
          await tester.pump();
          final gesture = await tester.startGesture(
            tester.getCenter(find.byType(ListView)),
          );
          await gesture.moveBy(const Offset(0, 20));
          await tester.pump();
          await gesture.moveBy(const Offset(0, 180));
          await tester.pump();
          await gesture.moveBy(const Offset(0, 220));
          await tester.pump();
          expect(
            find.byType(RefreshProgressIndicator),
            mode == 'anywhere' ? findsOneWidget : findsNothing,
          );
          await gesture.up();
          await _finish(tester);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'RefreshIndicator active unsafe edits dispose only spinner cycle and recover on $platform',
      (tester) async {
        final model = _model(platform: platform);
        await _pump(tester, model);
        final scrollState = tester.state<ScrollableState>(
          find.byType(Scrollable),
        );
        final indicatorState = tester.state(_indicator);
        final gesture = await _pull(tester);
        expect(find.byType(RefreshProgressIndicator), findsOneWidget);
        (_box(model)['properties'] as Map)['width'] = _number(35);
        await _pump(tester, model);
        expect(find.byType(RefreshProgressIndicator), findsNothing);
        expect(_diagnostics(tester), contains('non-square'));
        expect(tester.state(_indicator), isNot(same(indicatorState)));
        expect(
          tester.state<ScrollableState>(find.byType(Scrollable)),
          same(scrollState),
        );
        await gesture.up();
        await _finish(tester);
        (_box(model)['properties'] as Map)['width'] = _number(300);
        await _pump(tester, model);
        final recovered = await _pull(tester);
        expect(find.byType(RefreshProgressIndicator), findsOneWidget);
        await recovered.up();
        await _finish(tester);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'RefreshIndicator TickerMode, safe history edits and variant changes retain SDK State on $platform',
      (tester) async {
        final model = _model(platform: platform);
        final refresh = _refresh(model);
        final ticker = _node(
          'df5babd2-16cf-44c4-b497-24375532ec68',
          'flutter.widgets.TickerMode',
          {
            'enabled': {'kind': 'boolean', 'value': false},
          },
          {'child': _single(refresh)},
        );
        _box(model)['slots']['child'] = _single(ticker);
        await _pump(tester, model);
        final state = tester.state<RefreshIndicatorState>(_indicator);
        var complete = false;
        unawaited(state.show().then((_) => complete = true));
        await tester.pump();
        await tester.pump(const Duration(seconds: 1));
        expect(complete, false);
        ticker['properties'] = {
          'enabled': {'kind': 'boolean', 'value': true},
        };
        await _pump(tester, model);
        await _finish(tester);
        expect(complete, true);
        for (final variant in ['adaptive', 'noSpinner', 'material']) {
          (refresh['properties'] as Map)['variant'] = _string(variant);
          await _pump(tester, model);
          expect(tester.state(_indicator), same(state));
        }
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'RefreshIndicator exact SDK semantics and transparent paint exceptions on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final value in [
            '0',
            '100%',
            'NaN',
            'NaN%',
            '',
            '-1',
            '101%',
            'loading',
          ]) {
            await tester.pumpWidget(const SizedBox());
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: {
                  'semanticsValue': _string(value),
                  'semanticsLabel': _string('Reload feed'),
                },
              ),
            );
            final gesture = await _pull(tester);
            final accepted = ['0', '100%', 'NaN', 'NaN%'].contains(value);
            expect(
              find.byType(RefreshProgressIndicator),
              accepted ? findsOneWidget : findsNothing,
              reason: value,
            );
            if (accepted) {
              expect(
                tester
                    .getSemantics(find.byType(RefreshProgressIndicator))
                    .getSemanticsData()
                    .label,
                contains('Reload feed'),
              );
            }
            await gesture.up();
            await _finish(tester);
            expect(tester.takeException(), isNull, reason: value);
          }
        } finally {
          semantics.dispose();
        }
        await tester.pumpWidget(const SizedBox());
        await _pump(
          tester,
          _model(
            platform: platform,
            width: 35,
            properties: {
              'strokeWidth': _number(1e308),
              'color': {'kind': 'color', 'argb': '0x0000FF00'},
            },
          ),
        );
        final gesture = await _pull(tester);
        expect(find.byType(RefreshProgressIndicator), findsOneWidget);
        expect(_diagnostics(tester), isNot(contains('non-square')));
        await gesture.up();
        await _finish(tester);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'RefreshIndicator adaptive Apple uses Cupertino and ignores unused visual semantics branches',
    (tester) async {
      for (final platform in [
        'android',
        'ios',
        'macos',
        'linux',
        'windows',
        'web',
      ]) {
        await tester.pumpWidget(const SizedBox());
        final apple = platform == 'ios' || platform == 'macos';
        await _pump(
          tester,
          _model(
            platform: platform,
            variant: 'adaptive',
            properties: {
              'semanticsValue': _string(
                apple ? 'free text ignored on Apple' : '50%',
              ),
              'strokeWidth': _number(apple ? 1e308 : 4),
              'elevation': _number(8),
              'color': {'kind': 'color', 'argb': '0xFF00FF00'},
              'backgroundColor': {'kind': 'color', 'argb': '0xFF0000FF'},
            },
          ),
        );
        final gesture = await _pull(tester);
        expect(
          find.byType(CupertinoActivityIndicator),
          apple ? findsOneWidget : findsNothing,
        );
        expect(
          find.byType(RefreshProgressIndicator),
          apple ? findsNothing : findsOneWidget,
        );
        if (apple) {
          expect(
            tester
                .widget<CupertinoActivityIndicator>(
                  find.byType(CupertinoActivityIndicator),
                )
                .color,
            const Color(0xff00ff00),
          );
        }
        await gesture.up();
        await _finish(tester);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'RefreshIndicator drag arrow pixels and opacity equal direct SDK under M2/M3',
    (tester) async {
      for (final m3 in [false, true]) {
        final theme = ThemeData(useMaterial3: m3);
        await _pump(
          tester,
          _model(
            properties: {
              'color': {'kind': 'color', 'argb': '0x8000FF00'},
              'strokeWidth': _number(3.5),
            },
          ),
          theme: theme,
        );
        final gesture = await _pull(tester);
        final progress = tester.widget<RefreshProgressIndicator>(
          find.byType(RefreshProgressIndicator),
        );
        final value = progress.value;
        final color = progress.valueColor!.value;
        final opacity = tester
            .widget<Opacity>(
              find
                  .descendant(
                    of: find.byType(RefreshProgressIndicator),
                    matching: find.byType(Opacity),
                  )
                  .first,
            )
            .opacity;
        final actualPixels = await _painted(tester, _painter(tester));
        await gesture.up();
        await tester.pumpWidget(
          MaterialApp(
            theme: theme,
            home: Center(
              child: RefreshProgressIndicator(
                value: value,
                valueColor: AlwaysStoppedAnimation<Color?>(color),
                strokeWidth: 3.5,
              ),
            ),
          ),
        );
        expect(
          await _painted(tester, _painter(tester)),
          orderedEquals(actualPixels),
        );
        expect(
          tester.widget<Opacity>(find.byType(Opacity).first).opacity,
          opacity,
        );
        expect(actualPixels.any((byte) => byte != 0), true);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
      }
    },
  );

  testWidgets(
    'RefreshIndicator static child preserves direct SDK intrinsic layout without invented scrollable',
    (tester) async {
      final model = _model();
      final refresh = _refresh(model);
      refresh['slots']['child'] = _single(
        _node(_listId, 'flutter.widgets.SizedBox', {
          'width': _number(80),
          'height': _number(25),
        }),
      );
      _box(model)['properties'] = <String, Object?>{};
      _box(model)['slots']['child'] = _single(
        _node(
          'f6480d29-b8a4-4dc7-85f7-d517b6cdf368',
          'flutter.widgets.IntrinsicWidth',
          {},
          {'child': _single(refresh)},
        ),
      );
      await _pump(tester, model);
      final actual = tester.getSize(_indicator);
      expect(find.byType(ListView), findsNothing);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: IntrinsicWidth(
              child: RefreshIndicator(
                onRefresh: () async {},
                child: const SizedBox(width: 80, height: 25),
              ),
            ),
          ),
        ),
      );
      expect(tester.getSize(_indicator), actual);
      expect(actual, const Size(80, 25));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'RefreshIndicator hidden scroll branches do not impose intrinsic measurement limits',
    (tester) async {
      for (final offstage in [false, true]) {
        final model = _model();
        final refresh = _refresh(model);
        final scroll = _list(model);
        _box(model)['properties'] = {'height': _number(400)};
        refresh['slots']['child'] = _single(
          _node(
            '52f7536c-b092-47dc-aa34-ad28d8bad093',
            offstage
                ? 'flutter.widgets.Offstage'
                : 'flutter.widgets.Visibility',
            offstage
                ? {}
                : {
                    'visible': {'kind': 'boolean', 'value': false},
                  },
            {
              'child': _single(scroll),
              if (!offstage)
                'replacement': _single(
                  _node(
                    'd8ca6ff9-1aa5-4bb1-944b-fdd475b5359d',
                    'flutter.widgets.Text',
                    {'data': _string('Replacement')},
                  ),
                ),
            },
          ),
        );
        _box(model)['slots']['child'] = _single(
          _node(
            'f6480d29-b8a4-4dc7-85f7-d517b6cdf368',
            'flutter.widgets.IntrinsicWidth',
            {},
            {'child': _single(refresh)},
          ),
        );
        await _pump(tester, model);
        expect(_indicator, findsOneWidget);
        if (!offstage) expect(find.text('Replacement'), findsOneWidget);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
      }
    },
  );

  testWidgets(
    'RefreshIndicator huge finite displacement diagnoses SizeTransition 1.5 overflow on both active branches',
    (tester) async {
      for (final platform in ['windows', 'ios']) {
        final model = _model(
          platform: platform,
          variant: 'adaptive',
          properties: {'displacement': _number(1.3e308)},
        );
        await _pump(tester, model);
        final gesture = await _pull(tester);
        await tester.pump();
        expect(_diagnostics(tester), contains('displacement/edgeOffset'));
        expect(find.byType(RefreshProgressIndicator), findsNothing);
        expect(find.byType(CupertinoActivityIndicator), findsNothing);
        expect(find.byType(ListView), findsOneWidget);
        expect(tester.takeException(), isNull);
        await gesture.up();
        await _finish(tester);
        await tester.pumpWidget(const SizedBox());
      }
    },
  );

  testWidgets(
    'RefreshIndicator intrinsic visible scrolling and loose active child resize preserve child State',
    (tester) async {
      for (final intrinsic in [false, true]) {
        final model = _model();
        final refresh = _refresh(model);
        final scroll = _list(model);
        final inner = _node(
          '52f7536c-b092-47dc-aa34-ad28d8bad093',
          'flutter.widgets.SizedBox',
          {'width': _number(100), 'height': _number(400)},
          {'child': _single(scroll)},
        );
        refresh['slots']['child'] = _single(inner);
        _box(model)['properties'] = <String, Object?>{};
        if (intrinsic) {
          _box(model)['slots']['child'] = _single(
            _node(
              'f6480d29-b8a4-4dc7-85f7-d517b6cdf368',
              'flutter.widgets.IntrinsicWidth',
              {},
              {'child': _single(refresh)},
            ),
          );
        }
        await _pump(tester, model);
        expect(tester.getSize(_indicator), const Size(100, 400));
        final scrollState = tester.state<ScrollableState>(
          find.byType(Scrollable),
        );
        final gesture = await _pull(tester);
        expect(find.byType(RefreshProgressIndicator), findsOneWidget);
        (inner['properties'] as Map)['width'] = _number(35);
        await _pump(tester, model);
        expect(tester.getSize(_indicator), const Size(35, 400));
        expect(find.byType(RefreshProgressIndicator), findsNothing);
        expect(_diagnostics(tester), contains('non-square'));
        expect(
          tester.state<ScrollableState>(find.byType(Scrollable)),
          same(scrollState),
        );
        expect(tester.takeException(), isNull);
        await gesture.up();
        await _finish(tester);
        (inner['properties'] as Map)['width'] = _number(200);
        await _pump(tester, model);
        final recovered = await _pull(tester);
        expect(find.byType(RefreshProgressIndicator), findsOneWidget);
        await recovered.up();
        await _finish(tester);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
      }
    },
  );

  testWidgets(
    'RefreshIndicator inherited text metrics under IntrinsicWidth match raw SDK in the same frame',
    (tester) async {
      final model = _model();
      final refresh = _refresh(model);
      refresh['slots']['child'] = _single(
        _node(_listId, 'flutter.widgets.Text', {'data': _string('Metrics')}),
      );
      _box(model)['properties'] = <String, Object?>{};
      _box(model)['slots']['child'] = _single(
        _node(
          'f6480d29-b8a4-4dc7-85f7-d517b6cdf368',
          'flutter.widgets.IntrinsicWidth',
          {},
          {'child': _single(refresh)},
        ),
      );
      final expected = <double, Size>{};
      for (final fontSize in [12.0, 30.0, 18.0]) {
        final theme = ThemeData(
          textTheme: TextTheme(bodyMedium: TextStyle(fontSize: fontSize)),
        );
        await tester.pumpWidget(
          MaterialApp(
            theme: theme,
            themeAnimationDuration: Duration.zero,
            home: Scaffold(
              body: Center(
                child: IntrinsicWidth(
                  child: RefreshIndicator(
                    onRefresh: () async {},
                    child: const Text('Metrics'),
                  ),
                ),
              ),
            ),
          ),
        );
        expected[fontSize] = tester.getSize(_indicator);
      }
      await tester.pumpWidget(const SizedBox());
      final home = CanvasDocumentView(
        model: _decode(model),
        selectedWidgetId: null,
        onSelected: (_) {},
      );
      RefreshIndicatorState? state;
      for (final fontSize in [12.0, 30.0, 18.0]) {
        await tester.pumpWidget(
          MaterialApp(
            theme: ThemeData(
              textTheme: TextTheme(bodyMedium: TextStyle(fontSize: fontSize)),
            ),
            themeAnimationDuration: Duration.zero,
            home: home,
          ),
        );
        state ??= tester.state<RefreshIndicatorState>(_indicator);
        expect(tester.state(_indicator), same(state));
        expect(
          tester.getSize(_indicator),
          expected[fontSize],
          reason: 'same frame font size $fontSize',
        );
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'raw SDK huge finite Refresh displacement overflows active scaled extent',
    (tester) async {
      final failures = <FlutterErrorDetails>[];
      final previous = FlutterError.onError;
      FlutterError.onError = (details) {
        if (details.exceptionAsString().contains(
          'given an infinite size during layout',
        )) {
          failures.add(details);
        } else {
          previous?.call(details);
        }
      };
      try {
        await tester.pumpWidget(
          MaterialApp(
            home: RefreshIndicator(
              displacement: 1.3e308,
              onRefresh: () async {},
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                children: const [SizedBox(height: 1400, child: Text('Raw'))],
              ),
            ),
          ),
        );
        final gesture = await _pull(tester);
        await gesture.moveBy(const Offset(0, 300));
        await tester.pump();
        expect(failures, isNotEmpty);
        expect(tester.takeException(), isNull);
        await gesture.cancel();
        await tester.pumpWidget(const SizedBox());
        expect(tester.takeException(), isNull);
      } finally {
        FlutterError.onError = previous;
      }
    },
  );

  testWidgets(
    'RefreshIndicator loose maximum width shrink is guarded before same-frame paint',
    (tester) async {
      final model = _model();
      final refresh = _refresh(model);
      _box(model)['properties'] = <String, Object?>{};
      final bounds = <String, Object?>{
        'kind': 'boxConstraints',
        'minWidth': 0.0,
        'maxWidth': 100.0,
        'minHeight': 0.0,
        'maxHeight': 400.0,
      };
      _box(model)['slots']['child'] = _single(
        _node(
          '52f7536c-b092-47dc-aa34-ad28d8bad093',
          'flutter.widgets.ConstrainedBox',
          {'constraints': bounds},
          {'child': _single(refresh)},
        ),
      );
      await _pump(tester, model);
      final gesture = await _pull(tester);
      expect(find.byType(RefreshProgressIndicator), findsOneWidget);
      bounds['maxWidth'] = 30.0;
      await _pump(tester, model);
      expect(find.byType(RefreshProgressIndicator), findsNothing);
      expect(_diagnostics(tester), contains('non-square'));
      expect(tester.getSize(_indicator).width, 30);
      expect(tester.takeException(), isNull);
      await gesture.up();
      await _finish(tester);
    },
  );

  testWidgets(
    'RefreshIndicator observer private build scope flushes SDK ticks without rebuilding dirty siblings',
    (tester) async {
      final counter = ValueNotifier<int>(0);
      final home = Row(
        children: [
          Expanded(
            child: CanvasDocumentView(
              model: _decode(_model()),
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
          SizedBox(
            width: 80,
            child: ValueListenableBuilder<int>(
              valueListenable: counter,
              builder: (_, value, _) => Text('Sibling $value'),
            ),
          ),
        ],
      );
      try {
        await tester.pumpWidget(MaterialApp(home: home));
        final state = tester.state<RefreshIndicatorState>(_indicator);
        final gesture = await _pull(tester);
        for (var i = 1; i <= 5; i++) {
          counter.value = i;
          await gesture.moveBy(const Offset(0, 8));
          await tester.pump(const Duration(milliseconds: 16));
          expect(find.text('Sibling $i'), findsOneWidget);
          expect(tester.state(_indicator), same(state));
          expect(tester.takeException(), isNull);
        }
        await gesture.up();
        for (var i = 6; i <= 20; i++) {
          counter.value = i;
          await tester.pump(const Duration(milliseconds: 30));
          expect(find.text('Sibling $i'), findsOneWidget);
          expect(tester.takeException(), isNull);
        }
        await _finish(tester);
        expect(find.byType(RefreshProgressIndicator), findsNothing);
        expect(tester.state(_indicator), same(state));
      } finally {
        await tester.pumpWidget(const SizedBox());
        counter.dispose();
      }
    },
  );

  testWidgets(
    'RefreshIndicator dry and intrinsic probes delegate without rebuilding SDK child',
    (tester) async {
      final model = _model();
      _refresh(model)['slots']['child'] = _single(
        _node(_listId, 'flutter.widgets.SizedBox', {
          'width': _number(80),
          'height': _number(25),
        }),
      );
      _box(model)['properties'] = <String, Object?>{};
      await _pump(tester, model);
      final state = tester.state<RefreshIndicatorState>(_indicator);
      final observer = tester.renderObject<RenderBox>(
        find.byWidgetPredicate(
          (w) => w.runtimeType.toString() == '_RefreshLayoutObserver',
        ),
      );
      expect(observer.getMinIntrinsicWidth(100), 80);
      expect(observer.getMaxIntrinsicWidth(100), 80);
      expect(observer.getMinIntrinsicHeight(100), 25);
      expect(observer.getMaxIntrinsicHeight(100), 25);
      expect(observer.getDryLayout(const BoxConstraints()), const Size(80, 25));
      expect(tester.state(_indicator), same(state));
      expect(_indicator, findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('raw SDK reverse pull arms refresh at the leading visual edge', (
    tester,
  ) async {
    var calls = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: RefreshIndicator(
          onRefresh: () async {
            calls++;
          },
          child: ListView(
            reverse: true,
            physics: const AlwaysScrollableScrollPhysics(),
            children: const [SizedBox(height: 100, child: Text('Reverse'))],
          ),
        ),
      ),
    );
    final gesture = await _pull(tester);
    await gesture.moveBy(const Offset(0, 300));
    await tester.pump();
    expect(find.byType(RefreshProgressIndicator), findsOneWidget);
    await gesture.up();
    await tester.pump();
    await _finish(tester);
    expect(calls, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'raw SDK async completion, cancel and noSpinner exact status notifications',
    (tester) async {
      final statuses = <RefreshIndicatorStatus?>[];
      final completer = Completer<void>();
      var calls = 0;
      await tester.pumpWidget(
        MaterialApp(
          home: RefreshIndicator.noSpinner(
            onRefresh: () {
              calls++;
              return completer.future;
            },
            onStatusChange: statuses.add,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              children: const [SizedBox(height: 1400, child: Text('Raw'))],
            ),
          ),
        ),
      );
      final gesture = await _pull(tester);
      await gesture.moveBy(const Offset(0, 300));
      await tester.pump();
      await gesture.up();
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(calls, 1, reason: '$statuses');
      expect(statuses, [
        RefreshIndicatorStatus.drag,
        RefreshIndicatorStatus.armed,
        RefreshIndicatorStatus.snap,
      ]);
      // The pinned SDK sets refresh/null internally but does not notify them.
      expect(find.byType(RefreshProgressIndicator), findsNothing);
      completer.complete();
      await tester.pump();
      await _finish(tester);
      expect(statuses.last, RefreshIndicatorStatus.done);
      statuses.clear();
      final short = await tester.startGesture(
        tester.getCenter(find.byType(ListView)),
      );
      await short.moveBy(const Offset(0, 30));
      await tester.pump();
      await short.up();
      await _finish(tester);
      expect(statuses, contains(RefreshIndicatorStatus.canceled));
      expect(calls, 1);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'raw SDK reverse vertical and programmatic bottom show preserve pinned top placement',
    (tester) async {
      for (final reverse in [false, true]) {
        final key = GlobalKey<RefreshIndicatorState>();
        final completer = Completer<void>();
        await tester.pumpWidget(
          MaterialApp(
            home: RefreshIndicator(
              key: key,
              onRefresh: () => completer.future,
              child: ListView(
                reverse: reverse,
                physics: const AlwaysScrollableScrollPhysics(),
                children: const [SizedBox(height: 1400, child: Text('Raw'))],
              ),
            ),
          ),
        );
        unawaited(key.currentState!.show(atTop: !reverse));
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 300));
        expect(find.byType(RefreshProgressIndicator), findsOneWidget);
        final positioned = tester.widget<Positioned>(
          find
              .descendant(
                of: find.byType(RefreshIndicator),
                matching: find.byType(Positioned),
              )
              .first,
        );
        expect(positioned.top, 0);
        expect(positioned.bottom, isNull);
        completer.complete();
        await tester.pump();
        await _finish(tester);
        expect(tester.takeException(), isNull);
      }
    },
  );
}
