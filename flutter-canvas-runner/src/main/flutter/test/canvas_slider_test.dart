import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/cupertino.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Slider';
const _id = 'e166c0e6-a533-41f3-acf8-f868f2659c13';
const _boundaryId = '658c0969-15b7-47e1-87b3-3e721cf203f1';
const _reference = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
const _states = <WidgetState, String>{
  WidgetState.disabled: 'Disabled',
  WidgetState.error: 'Error',
  WidgetState.dragged: 'Dragged',
  WidgetState.pressed: 'Pressed',
  WidgetState.selected: 'Selected',
  WidgetState.scrolledUnder: 'ScrolledUnder',
  WidgetState.hovered: 'Hovered',
  WidgetState.focused: 'Focused',
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _number(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _color(int value) => {
  'kind': 'color',
  'argb': '0x${value.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _node(
  String id,
  String type,
  Map<String, Object?> properties, [
  Map<String, Object?> slots = const {},
]) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};
Map<String, Object?> _model({
  String variant = 'standard',
  String profile = 'windows',
  Map<String, Object?> properties = const {},
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = profile;
  model['root'] = _node(
    '8798daa7-a409-45d6-9ee5-f8bc0686ed26',
    'flutter.material.Scaffold',
    {},
    {
      'body': _single(
        _node(
          'ef4c8288-b304-4e4c-89e2-d1ff8d31dcaa',
          'flutter.widgets.Center',
          {},
          {
            'child': _single(
              _node(_boundaryId, 'flutter.widgets.RepaintBoundary', {}, {
                'child': _single(
                  _node(_id, _type, {
                    'value': _number(.5),
                    'enabled': _bool(true),
                    'variant': _string(variant),
                    ...properties,
                  }),
                ),
              }),
            ),
          },
        ),
      ),
    },
  );
  return model;
}

Map _control(Map model) =>
    model['root']['slots']['body']['child']['slots']['child']['child']['slots']['child']['child']
        as Map;
CanvasModel _decode(Map model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode _decodedControl(Map model) => _decode(
  model,
).root.slots['body']!.child!.slots['child']!.child!.slots['child']!.child!;
Slider _sdk(WidgetTester tester) => tester.widget<Slider>(find.byType(Slider));
String _diagnostics(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((v) => v.message ?? '')
    .join('\n');
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  Widget Function(Widget)? wrap,
}) async {
  final view = CanvasDocumentView(
    model: _decode(model),
    selectedWidgetId: null,
    onSelected: selected?.add ?? (_) {},
  );
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: wrap?.call(view) ?? view,
    ),
  );
  await tester.pump(const Duration(milliseconds: 350));
  await tester.pump();
}

Future<Uint8List> _pixels(
  WidgetTester tester,
  RenderRepaintBoundary boundary,
) async => (await tester.runAsync(() async {
  final image = await boundary.toImage();
  try {
    final data = await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    return Uint8List.fromList(data!.buffer.asUint8List());
  } finally {
    image.dispose();
  }
}))!;
Future<void> _expectRawPixels(
  WidgetTester tester,
  Slider raw,
  ThemeData theme,
) async {
  tester
          .renderObject<RenderCustomPaint>(
            find.byKey(const ValueKey('canvas-widget-outline-$_id')),
          )
          .foregroundPainter =
      null;
  await tester.pump();
  final boundary = tester.renderObject<RenderRepaintBoundary>(
    find
        .descendant(
          of: _widget(_boundaryId),
          matching: find.byType(RepaintBoundary),
        )
        .first,
  );
  final pixels = await _pixels(tester, boundary);
  final size = tester.getSize(find.byType(Slider));
  final direction = Directionality.of(tester.element(find.byType(Slider)));
  final key = GlobalKey();
  await tester.pumpWidget(
    MaterialApp(
      theme: theme,
      themeAnimationDuration: Duration.zero,
      home: Scaffold(
        body: Center(
          child: Directionality(
            textDirection: direction,
            child: OverflowBox(
              minWidth: 0,
              maxWidth: double.infinity,
              minHeight: 0,
              maxHeight: double.infinity,
              child: SizedBox(
                width: size.width,
                height: size.height,
                child: RepaintBoundary(key: key, child: raw),
              ),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 350));
  await tester.pump();
  expect(tester.getSize(find.byType(Slider)), size);
  expect(
    await _pixels(
      tester,
      key.currentContext!.findRenderObject()! as RenderRepaintBoundary,
    ),
    pixels,
  );
  expect(tester.takeException(), isNull);
}

Slider _raw({
  String variant = 'standard',
  double value = .5,
  bool enabled = true,
  int? divisions,
  String? label,
  double? secondary,
  bool? year2023,
  EdgeInsetsGeometry? padding,
  Color? active,
  Color? inactive,
  Color? secondaryColor,
  Color? thumb,
  ShowValueIndicator? indicator,
  WidgetStateProperty<Color?>? overlay,
}) => variant == 'adaptive'
    ? Slider.adaptive(
        value: value,
        onChanged: enabled ? (_) {} : null,
        divisions: divisions,
        label: label,
        secondaryTrackValue: secondary,
        activeColor: active,
        inactiveColor: inactive,
        secondaryActiveColor: secondaryColor,
        thumbColor: thumb,
        overlayColor: overlay,
        showValueIndicator: indicator,
        // ignore: deprecated_member_use
        year2023: year2023,
      )
    : Slider(
        value: value,
        onChanged: enabled ? (_) {} : null,
        divisions: divisions,
        label: label,
        secondaryTrackValue: secondary,
        activeColor: active,
        inactiveColor: inactive,
        secondaryActiveColor: secondaryColor,
        thumbColor: thumb,
        overlayColor: overlay,
        showValueIndicator: indicator,
        padding: padding,
        // ignore: deprecated_member_use
        year2023: year2023,
      );

class _RecordingSliderShape extends SliderComponentShape {
  int paints = 0;
  double? scale;
  String? label;
  SliderThemeData? theme;
  Offset? center;
  @override
  Size getPreferredSize(bool isEnabled, bool isDiscrete) => const Size(40, 40);
  @override
  void paint(
    PaintingContext context,
    Offset center, {
    required Animation<double> activationAnimation,
    required Animation<double> enableAnimation,
    required bool isDiscrete,
    required TextPainter labelPainter,
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    required TextDirection textDirection,
    required double value,
    required double textScaleFactor,
    required Size sizeWithOverflow,
  }) {
    paints++;
    this.center = center;
    scale = textScaleFactor;
    label = labelPainter.text?.toPlainText();
    theme = sliderTheme;
  }
}

void main() {
  testWidgets(
    'raw retained Slider shell keeps stale Material thumb after Cupertino value edits',
    (tester) async {
      final shape = _RecordingSliderShape();
      Future<void> raw(
        TargetPlatform platform,
        double value, {
        Key? key,
      }) async {
        await tester.pumpWidget(
          MaterialApp(
            theme: ThemeData(
              platform: platform,
              sliderTheme: SliderThemeData(thumbShape: shape),
            ),
            themeAnimationDuration: Duration.zero,
            home: Material(
              child: SizedBox(
                width: 300,
                height: 50,
                child: Slider.adaptive(
                  key: key,
                  value: value,
                  onChanged: (_) {},
                ),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();
      }

      await raw(TargetPlatform.windows, .2);
      final oldCenter = shape.center!;
      final state = tester.state(find.byType(Slider));
      await raw(TargetPlatform.iOS, .8);
      await raw(TargetPlatform.windows, .8);
      expect(tester.state(find.byType(Slider)), same(state));
      expect(shape.center, oldCenter);
      await raw(TargetPlatform.windows, .8, key: UniqueKey());
      expect(shape.center!.dx, greaterThan(oldCenter.dx));
      expect(tester.takeException(), isNull);
    },
  );
  test(
    'Slider decodes 33 closed fields, null and both signed infinity enums',
    () {
      final properties = <String, Object?>{
        'value': _number(.3),
        'secondaryTrackValue': _number(.7),
        'onChanged': _reference,
        'onChangeStart': _reference,
        'onChangeEnd': _reference,
        'min': _number(0),
        'max': _number(1),
        'divisions': _number(7),
        'label': _string('Value\nlabel'),
        'activeColor': _color(0xFF112233),
        'inactiveColor': _color(0xFF445566),
        'secondaryActiveColor': _color(0xFF778899),
        'thumbColor': _color(0xFFABCDEF),
        'overlayColor': _reference,
        'mouseCursor': _reference,
        'semanticFormatterCallback': _reference,
        'focusNode': _reference,
        'autofocus': _bool(true),
        'allowedInteraction': _enum('SliderInteraction', 'slideThumb'),
        'padding': {
          'kind': 'edgeInsetsDirectional',
          'start': 3,
          'end': 7,
          'top': 2,
          'bottom': 4,
        },
        'showValueIndicator': _enum('ShowValueIndicator', 'alwaysVisible'),
        'year2023': _null,
      };
      expect(
        _decodedControl(_model(properties: properties)).properties,
        hasLength(24),
      );
      properties.remove('overlayColor');
      for (final state in ['Default', ..._states.values]) {
        properties['overlayColor$state'] = _null;
      }
      expect(
        _decodedControl(_model(properties: properties)).properties,
        hasLength(32),
      );
      for (final sign in ['infinity', 'negativeInfinity']) {
        expect(
          _decodedControl(
            _model(
              properties: {
                'value': _enum('double', sign),
                'min': _enum('double', sign),
                'max': _enum('double', sign),
                'secondaryTrackValue': _null,
                'divisions': _null,
              },
            ),
          ),
          isNotNull,
        );
      }
    },
  );

  testWidgets(
    'Canvas branch-only shell repair paints current thumb while retaining focus ownership',
    (tester) async {
      final shape = _RecordingSliderShape();
      Future<void> canvas(TargetPlatform platform, double value) => _pump(
        tester,
        _model(variant: 'adaptive', properties: {'value': _number(value)}),
        theme: ThemeData(
          platform: platform,
          sliderTheme: SliderThemeData(thumbShape: shape),
        ),
      );
      await canvas(TargetPlatform.windows, .2);
      final initialCenter = shape.center!;
      final focus = _sdk(tester).focusNode!;
      var previous = tester.state(find.byType(Slider));
      await canvas(TargetPlatform.iOS, .8);
      expect(tester.state(find.byType(Slider)), isNot(same(previous)));
      previous = tester.state(find.byType(Slider));
      await canvas(TargetPlatform.windows, .8);
      expect(tester.state(find.byType(Slider)), isNot(same(previous)));
      expect(shape.center!.dx, greaterThan(initialCenter.dx));
      expect(_sdk(tester).focusNode, same(focus));
      previous = tester.state(find.byType(Slider));
      await canvas(TargetPlatform.windows, .6);
      expect(tester.state(find.byType(Slider)), same(previous));
      await canvas(TargetPlatform.iOS, .2);
      await canvas(TargetPlatform.windows, .2);
      expect(shape.center, initialCenter);
      expect(_sdk(tester).focusNode, same(focus));
      expect(_diagnostics(tester), contains('SDK animation shell'));
      expect(tester.takeException(), isNull);
    },
  );

  test(
    'Slider rejects invalid SDK range, division, variants and family coexistence',
    () {
      for (final properties in <Map<String, Object?>>[
        {'value': _null},
        {'value': _number(1.01)},
        {'min': _number(1)},
        {'max': _number(.1)},
        {'secondaryTrackValue': _number(2)},
        {'divisions': _number(0)},
        {'divisions': _number(1.5)},
        {'divisions': _number(9007199254740992)},
        {'year2023': _string('true')},
        {'overlayColor': _reference, 'overlayColorDefault': _null},
        {
          'variant': _string('adaptive'),
          'padding': {
            'kind': 'edgeInsets',
            'left': 0,
            'right': 0,
            'top': 0,
            'bottom': 0,
          },
        },
        {'value': _enum('double', 'nan')},
        {'allowedInteraction': _enum('SliderInteraction', 'all')},
      ]) {
        expect(
          () => _decode(_model(properties: properties)),
          throwsFormatException,
          reason: '$properties',
        );
      }
      final noValue = _model();
      (_control(noValue)['properties'] as Map).remove('value');
      expect(() => _decode(noValue), throwsFormatException);
      final child = _model();
      _control(child)['slots'] = {'child': _single(null)};
      expect(() => _decode(child), throwsFormatException);
    },
  );

  for (final profile in ['windows', 'web']) {
    testWidgets(
      '$profile both variants render real SDK with exact constructor fields',
      (tester) async {
        for (final variant in ['standard', 'adaptive']) {
          await _pump(
            tester,
            _model(
              profile: profile,
              variant: variant,
              properties: {
                'value': _number(2),
                'min': _number(-4),
                'max': _number(8),
                'secondaryTrackValue': _number(5),
                'divisions': _number(12),
                'label': _string('Label'),
                'activeColor': _color(0xFF112233),
                'inactiveColor': _color(0xFF445566),
                'secondaryActiveColor': _color(0xFF778899),
                'thumbColor': _color(0xFFABCDEF),
                'onChanged': _reference,
                'onChangeStart': _reference,
                'onChangeEnd': _reference,
                'mouseCursor': _string('click'),
                'focusNode': _reference,
                'autofocus': _bool(true),
                'allowedInteraction': _enum('SliderInteraction', 'tapOnly'),
                'showValueIndicator': _enum('ShowValueIndicator', 'never'),
                'year2023': _null,
                'semanticFormatterCallback': _reference,
              },
            ),
          );
          final sdk = _sdk(tester);
          expect(sdk.value, 2);
          expect(sdk.min, -4);
          expect(sdk.max, 8);
          expect(sdk.secondaryTrackValue, 5);
          expect(sdk.divisions, 12);
          expect(sdk.label, 'Label');
          expect(sdk.activeColor, const Color(0xFF112233));
          expect(sdk.inactiveColor, const Color(0xFF445566));
          expect(sdk.secondaryActiveColor, const Color(0xFF778899));
          expect(sdk.thumbColor, const Color(0xFFABCDEF));
          expect(sdk.allowedInteraction, SliderInteraction.tapOnly);
          expect(sdk.showValueIndicator, ShowValueIndicator.never);
          expect(sdk.mouseCursor, SystemMouseCursors.click);
          expect(sdk.onChanged, isNotNull);
          expect(sdk.onChangeStart, isNotNull);
          expect(sdk.onChangeEnd, isNotNull);
          expect(sdk.semanticFormatterCallback, isNull);
          expect(sdk.focusNode, isNotNull);
          expect(_diagnostics(tester), contains('default percentage'));
          expect(tester.takeException(), isNull);
        }
      },
    );
  }

  for (final material3 in [false, true]) {
    for (final variant in ['standard', 'adaptive']) {
      testWidgets(
        'exact SDK pixels M3=$material3 $variant platform, RTL, states and year branches',
        (tester) async {
          for (final platform in [TargetPlatform.windows, TargetPlatform.iOS]) {
            for (final year in [true, false]) {
              for (final enabled in [true, false]) {
                final theme = ThemeData(
                  useMaterial3: material3,
                  platform: platform,
                  sliderTheme: const SliderThemeData(
                    activeTrackColor: Colors.pink,
                    thumbColor: Colors.orange,
                    trackHeight: 6,
                  ),
                );
                final model = _model(
                  variant: variant,
                  properties: {
                    'enabled': _bool(enabled),
                    'value': _number(.25),
                    'secondaryTrackValue': _number(.75),
                    'divisions': _number(4),
                    'label': _string('Quarter'),
                    'year2023': _bool(year),
                    'activeColor': _color(0xFF008866),
                    'thumbColor': _color(0xFF123456),
                  },
                );
                final center =
                    (model['root'] as Map)['slots']['body']['child'] as Map;
                center['slots']['child']['child'] = _node(
                  '92f6d3bb-5100-4745-894f-d2570c9c226b',
                  'flutter.widgets.Directionality',
                  {'textDirection': _enum('TextDirection', 'rtl')},
                  {'child': _single(center['slots']['child']['child'])},
                );
                await _pump(tester, model, theme: theme);
                await _expectRawPixels(
                  tester,
                  _raw(
                    variant: variant,
                    value: .25,
                    enabled: enabled,
                    secondary: .75,
                    divisions: 4,
                    label: 'Quarter',
                    year2023: year,
                    active: const Color(0xFF008866),
                    thumb: const Color(0xFF123456),
                  ),
                  theme,
                );
              }
            }
          }
        },
      );
    }
  }

  testWidgets(
    'presence-aware overlay map preserves all state priorities and explicit null',
    (tester) async {
      final colors = <WidgetState, Color>{};
      final props = <String, Object?>{
        'overlayColorDefault': _color(0xFF123456),
      };
      var index = 0;
      for (final entry in _states.entries) {
        final argb = 0xFF111100 + index++;
        colors[entry.key] = Color(argb);
        props['overlayColor${entry.value}'] = _color(argb);
      }
      await _pump(tester, _model(properties: props));
      final overlay = _sdk(tester).overlayColor!;
      for (var mask = 0; mask < 256; mask++) {
        final states = <WidgetState>{
          for (var i = 0; i < 8; i++)
            if (mask & (1 << i) != 0) _states.keys.elementAt(i),
        };
        expect(
          overlay.resolve(states),
          states.isEmpty ? const Color(0xFF123456) : colors[states.first],
        );
      }
      props['overlayColorDisabled'] = _null;
      await _pump(tester, _model(properties: props));
      expect(
        _sdk(
          tester,
        ).overlayColor!.resolve({WidgetState.disabled, WidgetState.pressed}),
        isNull,
      );
      expect(_sdk(tester).overlayColor!.resolve({}), const Color(0xFF123456));
    },
  );

  testWidgets(
    'adaptive Apple ignores non-forwarded refs and fields without false diagnostics',
    (tester) async {
      final theme = ThemeData(platform: TargetPlatform.iOS);
      await _pump(
        tester,
        _model(
          variant: 'adaptive',
          properties: {
            'overlayColor': _reference,
            'focusNode': _reference,
            'mouseCursor': _reference,
            'semanticFormatterCallback': _reference,
            'autofocus': _bool(true),
            'secondaryTrackValue': _number(.8),
            'inactiveColor': _color(0xFFFF0000),
            'label': _string('Ignored'),
            'showValueIndicator': _enum('ShowValueIndicator', 'alwaysVisible'),
          },
        ),
        theme: theme,
      );
      expect(find.byType(CupertinoSlider), findsOneWidget);
      expect(_diagnostics(tester), isNot(contains('preview limitation')));
      final apple = tester.widget<CupertinoSlider>(
        find.byType(CupertinoSlider),
      );
      expect(apple.value, .5);
      expect(apple.thumbColor, CupertinoColors.white);
      await _expectRawPixels(tester, _raw(variant: 'adaptive'), theme);
    },
  );

  testWidgets(
    'retains SDK state and isolated focus across callbacks, value, variant and themes',
    (tester) async {
      final first = _model(
        properties: {'autofocus': _bool(true), 'focusNode': _reference},
      );
      await _pump(tester, first);
      final state = tester.state(find.byType(Slider));
      final focus = _sdk(tester).focusNode!;
      expect(focus.hasFocus, isTrue);
      for (final variant in ['standard', 'adaptive', 'standard']) {
        await _pump(
          tester,
          _model(
            variant: variant,
            properties: {'value': _number(.7), 'divisions': _number(10)},
          ),
        );
        expect(tester.state(find.byType(Slider)), same(state));
        expect(_sdk(tester).focusNode, same(focus));
        expect(focus.hasFocus, isTrue);
      }
      await _pump(
        tester,
        _model(variant: 'adaptive'),
        theme: ThemeData(platform: TargetPlatform.iOS),
      );
      expect(tester.state(find.byType(Slider)), isNot(same(state)));
      expect(find.byType(CupertinoSlider), findsOneWidget);
      await _pump(tester, _model(variant: 'adaptive'));
      expect(tester.state(find.byType(Slider)), isNot(same(state)));
      expect(find.byType(CupertinoSlider), findsNothing);
      expect(_sdk(tester).focusNode, same(focus));
      expect(_diagnostics(tester), contains('lifecycle guard'));
      expect(tester.takeException(), isNull);
    },
  );

  for (final variant in ['standard', 'adaptive']) {
    testWidgets(
      '$variant pointer, drag and keyboard select without modifying controlled model',
      (tester) async {
        final selected = <String>[];
        final model = _model(
          variant: variant,
          properties: {'autofocus': _bool(true)},
        );
        final bytes = jsonEncode(model);
        await _pump(tester, model, selected: selected);
        await tester.tap(find.byType(Slider));
        await tester.pump();
        expect(selected, contains(_id));
        selected.clear();
        await tester.drag(find.byType(Slider), const Offset(50, 0));
        await tester.pumpAndSettle();
        expect(selected, contains(_id));
        await tester.sendKeyEvent(LogicalKeyboardKey.arrowRight);
        await tester.pumpAndSettle();
        expect(_sdk(tester).value, .5);
        expect(jsonEncode(model), bytes);
        final pointer = await tester.startGesture(
          tester.getCenter(find.byType(Slider)),
          kind: PointerDeviceKind.mouse,
          buttons: kSecondaryMouseButton,
        );
        await pointer.up();
        await tester.pumpAndSettle();
        expect(_sdk(tester).value, .5);
        expect(jsonEncode(model), bytes);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'disabled stored callback omitted while start/end diagnostics remain precise',
    (tester) async {
      await _pump(
        tester,
        _model(properties: {'enabled': _bool(false), 'onChanged': _reference}),
      );
      expect(_sdk(tester).onChanged, isNull);
      expect(_diagnostics(tester), isNot(contains('Project value callbacks')));
      await _pump(
        tester,
        _model(
          properties: {
            'enabled': _bool(false),
            'onChanged': _reference,
            'onChangeEnd': _reference,
          },
        ),
      );
      expect(_sdk(tester).onChangeEnd, isNotNull);
      expect(_diagnostics(tester), contains('Project value callbacks'));
    },
  );

  testWidgets(
    'finite and signed-infinite equal ranges exact Material but unavailable Apple',
    (tester) async {
      for (final value in [
        _number(1),
        _enum('double', 'infinity'),
        _enum('double', 'negativeInfinity'),
      ]) {
        final model = _model(
          variant: 'adaptive',
          properties: {'value': value, 'min': value, 'max': value},
        );
        await _pump(tester, model);
        expect(find.byType(Slider), findsOneWidget);
        expect(tester.takeException(), isNull);
        await _pump(
          tester,
          model,
          theme: ThemeData(platform: TargetPlatform.iOS),
        );
        expect(find.byType(Slider), findsNothing);
        expect(_diagnostics(tester), contains('zero range'));
        expect(_widget(_id), findsOneWidget);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'unsafe normalization and inverse ranges are explicit, preserving decoded values',
    (tester) async {
      for (final props in <Map<String, Object?>>[
        {
          'value': _enum('double', 'infinity'),
          'max': _enum('double', 'infinity'),
        },
        {'value': _number(0), 'min': _enum('double', 'negativeInfinity')},
        {
          'value': _number(1e308),
          'min': _number(-1e308),
          'max': _number(1e308),
        },
        {'value': _number(0), 'min': _number(-1e308), 'max': _number(1e308)},
        {'max': _enum('double', 'infinity')},
      ]) {
        final model = _model(properties: props);
        final bytes = jsonEncode(model);
        await _pump(tester, model);
        expect(find.byType(Slider), findsNothing);
        expect(_diagnostics(tester), contains('preview unavailable'));
        expect(jsonEncode(model), bytes);
        expect(tester.takeException(), isNull);
      }
      await _pump(
        tester,
        _model(
          properties: {
            'max': _enum('double', 'infinity'),
            'enabled': _bool(false),
          },
        ),
      );
      expect(find.byType(Slider), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'huge divisions use SDK default suppression and scoped zero-tick budget',
    (tester) async {
      final model = _model(
        properties: {'divisions': _number(9007199254740991)},
      );
      await _pump(tester, model);
      expect(find.byType(Slider), findsOneWidget);
      await _pump(
        tester,
        model,
        theme: ThemeData(
          sliderTheme: SliderThemeData(
            tickMarkShape: SliderTickMarkShape.noTickMark,
          ),
        ),
      );
      expect(find.byType(Slider), findsNothing);
      expect(_diagnostics(tester), contains('10,000-division paint budget'));
      await _pump(
        tester,
        model,
        theme: ThemeData(
          sliderTheme: const SliderThemeData(
            tickMarkShape: RoundSliderTickMarkShape(tickMarkRadius: 5e-301),
          ),
        ),
      );
      expect(find.byType(Slider), findsNothing);
      expect(_diagnostics(tester), contains('10,000-division paint budget'));
      await _pump(
        tester,
        _model(
          variant: 'adaptive',
          properties: {'divisions': _number(9007199254740991)},
        ),
        theme: ThemeData(
          platform: TargetPlatform.iOS,
          sliderTheme: SliderThemeData(
            tickMarkShape: SliderTickMarkShape.noTickMark,
          ),
        ),
      );
      expect(find.byType(CupertinoSlider), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Material intrinsic Row works, adaptive Apple unbounded width is diagnosed',
    (tester) async {
      final model = _model(variant: 'adaptive');
      final root = model['root'] as Map;
      final child = root['slots']['body']['child'];
      root['slots']['body']['child'] = _node(
        '902792b7-2b42-4d85-98f8-63945da5b67f',
        'flutter.widgets.Row',
        {},
        {
          'children': {
            'kind': 'list',
            'children': [child],
          },
        },
      );
      await _pump(tester, model);
      expect(find.byType(Slider), findsOneWidget);
      expect(tester.takeException(), isNull);
      await _pump(
        tester,
        model,
        theme: ThemeData(platform: TargetPlatform.iOS),
      );
      expect(find.byType(Slider), findsNothing);
      expect(_diagnostics(tester), contains('bounded width'));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'all four interaction modes retain real SDK callbacks and controlled values',
    (tester) async {
      for (final mode in SliderInteraction.values) {
        final model = _model(
          properties: {
            'allowedInteraction': _enum('SliderInteraction', mode.name),
            'onChangeStart': _reference,
            'onChangeEnd': _reference,
          },
        );
        final selected = <String>[];
        await _pump(tester, model, selected: selected);
        final state = tester.state(find.byType(Slider));
        final rect = tester.getRect(find.byType(Slider));
        await tester.tapAt(Offset(rect.left + 50, rect.center.dy));
        await tester.pumpAndSettle();
        await tester.dragFrom(rect.center, const Offset(50, 0));
        await tester.pumpAndSettle();
        expect(_sdk(tester).allowedInteraction, mode);
        expect(_sdk(tester).value, .5);
        expect(selected, contains(_id));
        expect(tester.state(find.byType(Slider)), same(state));
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'every value indicator policy uses actual SDK visibility, label and text scale',
    (tester) async {
      for (final material3 in [false, true]) {
        for (final indicator in ShowValueIndicator.values) {
          for (final discrete in [false, true]) {
            final shape = _RecordingSliderShape();
            final model = _model(
              properties: {
                'label': _string('Stored label'),
                'showValueIndicator': _enum(
                  'ShowValueIndicator',
                  indicator.name,
                ),
                if (discrete) 'divisions': _number(4),
              },
            );
            (model['profile'] as Map)['textScaleFactor'] = 2.0;
            await _pump(
              tester,
              model,
              theme: ThemeData(
                useMaterial3: material3,
                sliderTheme: SliderThemeData(valueIndicatorShape: shape),
              ),
            );
            final initialPaints = shape.paints;
            expect(
              initialPaints > 0,
              indicator == ShowValueIndicator.alwaysVisible,
            );
            final gesture = await tester.startGesture(
              tester.getCenter(find.byType(Slider)),
            );
            await tester.pump(const Duration(milliseconds: 150));
            final shouldShow = switch (indicator) {
              ShowValueIndicator.never => false,
              ShowValueIndicator.onlyForDiscrete => discrete,
              ShowValueIndicator.onlyForContinuous => !discrete,
              _ => true,
            };
            expect(
              indicator == ShowValueIndicator.alwaysVisible
                  ? shape.paints > 0
                  : shape.paints > initialPaints,
              shouldShow,
              reason: '$material3 $indicator discrete=$discrete',
            );
            if (shouldShow) {
              expect(shape.label, 'Stored label');
              expect(shape.scale, material3 ? 1.3 : 2.0);
            }
            await gesture.up();
            await tester.pumpAndSettle();
            expect(tester.takeException(), isNull);
            await tester.pumpWidget(const SizedBox());
          }
        }
      }
    },
  );

  testWidgets(
    'resolved overlay presence and direct-active/theme precedence follow real hover and drag',
    (tester) async {
      final highlight = FocusManager.instance.highlightStrategy;
      FocusManager.instance.highlightStrategy =
          FocusHighlightStrategy.alwaysTraditional;
      addTearDown(() => FocusManager.instance.highlightStrategy = highlight);
      final shape = _RecordingSliderShape();
      final model = _model(
        properties: {
          'activeColor': _color(0xFF123456),
          'overlayColorDefault': _color(0xFF112233),
          'overlayColorHovered': _null,
          'overlayColorDragged': _color(0xFFABCDEF),
        },
      );
      await _pump(
        tester,
        model,
        theme: ThemeData(
          sliderTheme: SliderThemeData(
            overlayShape: shape,
            thumbShape: shape,
            overlayColor: Colors.orange,
          ),
        ),
      );
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer();
      await mouse.moveTo(tester.getCenter(find.byType(Slider)));
      await tester.pumpAndSettle();
      // Explicit null stops the local map; the SDK then applies activeColor before theme.
      // ignore: deprecated_member_use
      expect(
        shape.theme!.overlayColor,
        const Color(0xFF123456).withAlpha((255 * .12).round()),
      );
      final gesture = await tester.startGesture(
        tester.getCenter(find.byType(Slider)),
      );
      await tester.pump(const Duration(milliseconds: 150));
      expect(shape.theme!.overlayColor, const Color(0xFFABCDEF));
      await gesture.up();
      await mouse.removePointer();
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Apple stable finite inverse, actual thumb drag, and ignored secondary overflow stay usable',
    (tester) async {
      final model = _model(
        variant: 'adaptive',
        properties: {
          'value': _number(0),
          'min': _number(-1e308),
          'max': _number(1e308),
          'secondaryTrackValue': _number(1e308),
        },
      );
      final selected = <String>[];
      await _pump(
        tester,
        model,
        theme: ThemeData(platform: TargetPlatform.iOS),
        selected: selected,
      );
      final rect = tester.getRect(find.byType(CupertinoSlider));
      await tester.dragFrom(
        Offset(rect.left + 22, rect.center.dy),
        const Offset(60, 0),
      );
      await tester.pumpAndSettle();
      expect(find.byType(CupertinoSlider), findsOneWidget);
      expect(selected, contains(_id));
      expect(_sdk(tester).value, 0);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'padding directional geometry preserves exact bounded SDK behavior',
    (tester) async {
      final model = _model(
        properties: {
          'padding': {
            'kind': 'edgeInsetsDirectional',
            'start': 12,
            'end': 4,
            'top': 3,
            'bottom': 7,
          },
        },
      );
      final theme = ThemeData();
      await _pump(tester, model, theme: theme);
      expect(
        _sdk(tester).padding,
        const EdgeInsetsDirectional.fromSTEB(12, 3, 4, 7),
      );
      await _expectRawPixels(
        tester,
        _raw(padding: const EdgeInsetsDirectional.fromSTEB(12, 3, 4, 7)),
        theme,
      );
    },
  );

  testWidgets(
    'overflowing padding is scoped to unbounded parent and preserves zero-size selection',
    (tester) async {
      final properties = <String, Object?>{
        'padding': {
          'kind': 'edgeInsets',
          'left': 1.7e308,
          'right': 1.7e308,
          'top': 0,
          'bottom': 0,
        },
      };
      final model = _model(properties: properties);
      final control = _control(model);
      (model['root'] as Map)['slots']['body'] = _single(
        _node(
          'ce2cce42-9cf0-4bf0-90a3-919e73db4ae2',
          'flutter.widgets.Row',
          {},
          {
            'children': {
              'kind': 'list',
              'children': [control],
            },
          },
        ),
      );
      final selected = <String>[];
      await _pump(tester, model, selected: selected);
      expect(find.byType(Slider), findsNothing);
      expect(_diagnostics(tester), contains('padding sum overflows'));
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$_id'),
      );
      expect(handle, findsOneWidget);
      await tester.tap(handle);
      await tester.pump();
      expect(selected, contains(_id));
      await _pump(tester, _model(properties: properties));
      expect(find.byType(Slider), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('multiple sliders keep separate stable state and focus owners', (
    tester,
  ) async {
    final model = _model();
    final first = Map<String, Object?>.from(_control(model));
    final second = <String, Object?>{
      ...first,
      'id': '092df508-41f0-4591-a470-16b43f7e929c',
      'properties': {
        ...first['properties'] as Map,
        'variant': _string('adaptive'),
      },
    };
    (model['root'] as Map)['slots']['body'] = _single(
      _node('ce2cce42-9cf0-4bf0-90a3-919e73db4ae2', 'flutter.widgets.Row', {}, {
        'children': {
          'kind': 'list',
          'children': [first, second],
        },
      }),
    );
    await _pump(tester, model);
    final states = tester.stateList(find.byType(Slider)).toList();
    final focus = tester
        .widgetList<Slider>(find.byType(Slider))
        .map((v) => v.focusNode)
        .toList();
    expect(focus[0], isNot(same(focus[1])));
    (first['properties'] as Map)['onChanged'] = _reference;
    (second['properties'] as Map)['value'] = _number(.8);
    await _pump(tester, model);
    final newStates = tester.stateList(find.byType(Slider)).toList();
    expect(newStates[0], same(states[0]));
    expect(newStates[1], same(states[1]));
    expect(tester.takeException(), isNull);
  });

  testWidgets('semantics role percentage and TickerMode match controlled SDK', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    try {
      await _pump(
        tester,
        _model(
          properties: {
            'value': _number(.25),
            'semanticFormatterCallback': _reference,
          },
        ),
        wrap: (child) => TickerMode(enabled: false, child: child),
      );
      final nodes =
          // Direct tree inspection pins the actual SDK role and percentage.
          // ignore: deprecated_member_use
          tester.binding.pipelineOwner.semanticsOwner!.rootSemanticsNode!;
      final dump = nodes.toStringDeep();
      expect(dump, contains('25%'));
      expect(dump, contains('isSlider'));
      expect(
        TickerMode.valuesOf(tester.element(find.byType(Slider))).enabled,
        isFalse,
      );
      expect(_diagnostics(tester), contains('default percentage'));
    } finally {
      semantics.dispose();
    }
  });
}
