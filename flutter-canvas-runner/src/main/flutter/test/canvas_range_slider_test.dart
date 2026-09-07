import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/foundation.dart' show listEquals;
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.RangeSlider';

class _Thumbs extends RangeSliderThumbShape {
  final centers = <Thumb, Offset>{};
  SliderThemeData? theme;
  @override
  Size getPreferredSize(bool isEnabled, bool isDiscrete) => const Size(20, 20);
  @override
  void paint(
    PaintingContext context,
    Offset center, {
    required Animation<double> activationAnimation,
    required Animation<double> enableAnimation,
    bool isDiscrete = false,
    bool isEnabled = false,
    bool isOnTop = false,
    TextDirection textDirection = TextDirection.ltr,
    required SliderThemeData sliderTheme,
    Thumb thumb = Thumb.start,
    bool isPressed = false,
  }) {
    centers[thumb] = center;
    theme = sliderTheme;
  }
}

class _Indicators extends RangeSliderValueIndicatorShape {
  final labels = <String>{};
  final scales = <double>{};
  int paints = 0;
  @override
  Size getPreferredSize(
    bool isEnabled,
    bool isDiscrete, {
    required TextPainter labelPainter,
    required double textScaleFactor,
  }) => const Size(30, 20);
  @override
  void paint(
    PaintingContext context,
    Offset center, {
    required Animation<double> activationAnimation,
    required Animation<double> enableAnimation,
    bool isDiscrete = false,
    bool isOnTop = false,
    required TextPainter labelPainter,
    double textScaleFactor = 1,
    Size sizeWithOverflow = Size.zero,
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    TextDirection textDirection = TextDirection.ltr,
    double value = 0,
    Thumb thumb = Thumb.start,
  }) {
    labels.add(labelPainter.text!.toPlainText());
    scales.add(textScaleFactor);
    paints++;
  }
}

class _Ticks extends RangeSliderTickMarkShape {
  _Ticks(this.width);
  final double width;
  int count = 0;
  @override
  Size getPreferredSize({
    required SliderThemeData sliderTheme,
    bool isEnabled = false,
  }) => Size(width, 0);
  @override
  void paint(
    PaintingContext context,
    Offset center, {
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    required Animation<double> enableAnimation,
    required Offset startThumbCenter,
    required Offset endThumbCenter,
    bool isEnabled = false,
    required TextDirection textDirection,
  }) {
    count++;
  }
}

class _WideTrack extends RoundedRectRangeSliderTrackShape {
  const _WideTrack();
  @override
  Rect getPreferredRect({
    required RenderBox parentBox,
    Offset offset = Offset.zero,
    required SliderThemeData sliderTheme,
    bool isEnabled = false,
    bool isDiscrete = false,
  }) => Rect.fromLTWH(
    offset.dx,
    offset.dy + parentBox.size.height / 2,
    1000000,
    4,
  );
}

const _id = 'e166c0e6-a533-41f3-acf8-f868f2659c13';
const _boundaryId = '658c0969-15b7-47e1-87b3-3e721cf203f1';
const _reference = {'kind': 'dartObjectReferencePresence'};

RangeSlider _raw({
  RangeValues values = const RangeValues(.25, .75),
  bool enabled = true,
  bool? year2023,
  int? divisions,
  RangeLabels? labels,
  EdgeInsetsGeometry? padding,
  Color? active,
  Color? inactive,
  WidgetStateProperty<Color?>? overlay,
  WidgetStateProperty<MouseCursor?>? cursor,
}) => RangeSlider(
  values: values,
  onChanged: enabled ? (_) {} : null,
  divisions: divisions,
  labels: labels,
  padding: padding,
  activeColor: active,
  inactiveColor: inactive,
  overlayColor: overlay,
  mouseCursor: cursor,
  // ignore: deprecated_member_use
  year2023: year2023,
);

void main() {
  testWidgets(
    'unknown exact track type guards only the oversized division loop',
    (tester) async {
      for (final divisions in [10000, 10001]) {
        final ticks = _Ticks(4);
        await _pump(
          tester,
          _model(properties: {'divisions': _number(divisions)}),
          theme: ThemeData(
            sliderTheme: SliderThemeData(
              rangeTrackShape: const _WideTrack(),
              rangeTickMarkShape: ticks,
            ),
          ),
        );
        expect(
          find.byType(RangeSlider),
          divisions == 10000 ? findsOneWidget : findsNothing,
        );
        if (divisions == 10000) {
          expect(ticks.count, greaterThanOrEqualTo(10001));
        } else {
          expect(ticks.count, 0);
          expect(_diagnostics(tester), contains('unreviewed track geometry'));
        }
      }
    },
  );
  testWidgets(
    'local state cursor is not recursively resolved as a whole cursor property',
    (tester) async {
      await _pump(
        tester,
        _model(
          properties: {
            'enabled': _bool(false),
            'mouseCursorDefault': _string('clickable'),
          },
        ),
      );
      final local = _sdk(tester).mouseCursor!;
      expect(
        local.resolve({WidgetState.disabled}),
        same(WidgetStateMouseCursor.clickable),
      );
      final regions = tester.widgetList<MouseRegion>(
        find.descendant(
          of: find.byType(RangeSlider),
          matching: find.byType(MouseRegion),
        ),
      );
      expect(
        regions.any(
          (region) =>
              identical(region.cursor, WidgetStateMouseCursor.clickable),
        ),
        isTrue,
      );
      // WidgetStateMouseCursor.createSession resolves its own empty state set.
      expect(
        (local.resolve({WidgetState.disabled}) as WidgetStateMouseCursor)
            .resolve({}),
        SystemMouseCursors.click,
      );
      await tester.pumpWidget(
        MaterialApp(
          home: Material(
            child: RangeSlider(
              values: const RangeValues(.25, .75),
              onChanged: null,
              mouseCursor: WidgetStateMouseCursor.clickable,
            ),
          ),
        ),
      );
      final rawRegions = tester.widgetList<MouseRegion>(
        find.descendant(
          of: find.byType(RangeSlider),
          matching: find.byType(MouseRegion),
        ),
      );
      expect(
        rawRegions.any((region) => region.cursor == SystemMouseCursors.basic),
        isTrue,
      );
    },
  );

  testWidgets(
    'live hover drag and focus use only SDK range states with local overlay fallback',
    (tester) async {
      final thumbs = _Thumbs();
      final selected = <String>[];
      await _pump(
        tester,
        _model(
          properties: {
            'overlayColorDefault': _color(0xff112233),
            'overlayColorHovered': _color(0xff223344),
            'overlayColorDragged': _color(0xff334455),
            'overlayColorFocused': _color(0xff445566),
            'mouseCursorDefault': _string('basic'),
            'mouseCursorHovered': _string('grab'),
            'mouseCursorDragged': _string('grabbing'),
            'mouseCursorFocused': _string('wait'),
          },
        ),
        theme: ThemeData(sliderTheme: SliderThemeData(rangeThumbShape: thumbs)),
        selected: selected,
      );
      final rect = tester.getRect(find.byType(RangeSlider));
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer(location: Offset.zero);
      await mouse.moveTo(rect.center);
      await tester.pump();
      expect(thumbs.theme!.overlayColor, const Color(0xff223344));
      await mouse.down(
        Offset(rect.left + 24 + .25 * (rect.width - 48), rect.center.dy),
      );
      await mouse.moveBy(const Offset(50, 0));
      await tester.pump(const Duration(milliseconds: 200));
      expect(thumbs.theme!.overlayColor, const Color(0xff334455));
      await mouse.up();
      await mouse.moveTo(Offset.zero);
      await tester.pump(const Duration(seconds: 1));
      expect(thumbs.theme!.overlayColor, const Color(0xff112233));
      // Thumb focus affects SDK overlay painting but is not an outer map state.
      await tester.sendKeyEvent(LogicalKeyboardKey.tab);
      await tester.pump();
      expect(thumbs.theme!.overlayColor, isNot(const Color(0xff445566)));
      expect(selected, contains(_id));
      await mouse.removePointer();
    },
  );

  testWidgets(
    'intrinsic Row and zero-sized host keep real SDK layout and selectable identity',
    (tester) async {
      final model = _model();
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
      final size = tester.getSize(find.byType(RangeSlider));
      expect(size.width, 192);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(
        MaterialApp(
          home: Material(child: Row(children: [_raw()])),
        ),
      );
      expect(tester.getSize(find.byType(RangeSlider)).width, size.width);
      (control['properties'] as Map)['padding'] = {
        'kind': 'edgeInsets',
        'left': 1e308,
        'right': 1e308,
        'top': 0,
        'bottom': 0,
      };
      await _pump(tester, model, selected: selected);
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$_id'),
      );
      expect(handle, findsOneWidget);
      await tester.tap(handle);
      await tester.pump();
      expect(selected, contains(_id));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'multiple RangeSliders retain distinct SDK states and two focus nodes each',
    (tester) async {
      final model = _model();
      final first = Map<String, Object?>.from(_control(model));
      final second = <String, Object?>{
        ...first,
        'id': '092df508-41f0-4591-a470-16b43f7e929c',
        'properties': {
          ...first['properties'] as Map,
          'valuesStart': _number(.1),
          'valuesEnd': _number(.9),
        },
      };
      (model['root'] as Map)['slots']['body'] = _single(
        _node(
          'ce2cce42-9cf0-4bf0-90a3-919e73db4ae2',
          'flutter.widgets.Row',
          {},
          {
            'children': {
              'kind': 'list',
              'children': [first, second],
            },
          },
        ),
      );
      await _pump(tester, model);
      final states = tester.stateList(find.byType(RangeSlider)).toList();
      final nodes = tester
          .widgetList<Focus>(
            find.descendant(
              of: find.byType(RangeSlider),
              matching: find.byType(Focus),
            ),
          )
          .map((focus) => focus.focusNode)
          .whereType<FocusNode>()
          .toList();
      expect(nodes, hasLength(4));
      expect(nodes.toSet(), hasLength(4));
      (first['properties'] as Map)['labels'] = _reference;
      (second['properties'] as Map)['valuesEnd'] = _number(.8);
      await _pump(tester, model);
      final later = tester.stateList(find.byType(RangeSlider)).toList();
      expect(later[0], same(states[0]));
      expect(later[1], same(states[1]));
      expect(
        tester
            .widgetList<Focus>(
              find.descendant(
                of: find.byType(RangeSlider),
                matching: find.byType(Focus),
              ),
            )
            .map((focus) => focus.focusNode)
            .whereType<FocusNode>()
            .toList(),
        orderedEquals(nodes),
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'dark theme modern range defaults and continuous raw pixels remain exact',
    (tester) async {
      final theme = ThemeData(
        brightness: Brightness.dark,
        sliderTheme: const SliderThemeData(
          // ignore: deprecated_member_use
          year2023: false,
        ),
      );
      await _pump(tester, _model(), theme: theme);
      await _expectRawPixels(tester, _raw(), theme);
    },
  );

  testWidgets(
    'TickerMode freezes SDK animation without replacing state or stored pairs',
    (tester) async {
      final thumbs = _Thumbs(),
          model = _model(properties: {'divisions': _number(4)});
      final theme = ThemeData(
        sliderTheme: SliderThemeData(rangeThumbShape: thumbs),
      );
      var ticking = true;
      Widget wrap(Widget child) => TickerMode(enabled: ticking, child: child);
      await _pump(tester, model, theme: theme, wrap: wrap);
      final state = tester.state(find.byType(RangeSlider)),
          initial = Map<Thumb, Offset>.of(thumbs.centers);
      ticking = false;
      (_control(model)['properties'] as Map)['valuesStart'] = _number(.5);
      await _pump(tester, model, theme: theme, wrap: wrap);
      expect(tester.state(find.byType(RangeSlider)), same(state));
      expect(thumbs.centers, initial);
      ticking = true;
      await _pump(tester, model, theme: theme, wrap: wrap);
      await tester.pumpAndSettle();
      expect(tester.state(find.byType(RangeSlider)), same(state));
      expect(
        thumbs.centers[Thumb.start]!.dx,
        greaterThan(initial[Thumb.start]!.dx),
      );
      expect(_sdk(tester).values, const RangeValues(.5, .75));
    },
  );

  test(
    '37 RangeSlider fields decode with exact pair null reference and infinity domains',
    () {
      final properties = <String, Object?>{
        'valuesStart': _number(.1),
        'valuesEnd': _number(.9),
        'min': _number(0),
        'max': _number(1),
        'divisions': _number(9007199254740991),
        'onChanged': _reference,
        'onChangeStart': _reference,
        'onChangeEnd': _reference,
        'labels': _reference,
        'activeColor': _color(0xffabcdef),
        'inactiveColor': _color(0xff123456),
        'overlayColor': _reference,
        'mouseCursor': _reference,
        'semanticFormatterCallback': _reference,
        'padding': {
          'kind': 'edgeInsetsDirectional',
          'start': 3,
          'end': 7,
          'top': 2,
          'bottom': 4,
        },
        'year2023': _null,
      };
      final covered = <String>{};
      covered.addAll(
        _decodedControl(_model(properties: properties)).properties.keys,
      );
      properties.remove('labels');
      properties['labelsStart'] = _string('Start\nfull label ');
      properties['labelsEnd'] = _string('');
      properties.remove('overlayColor');
      properties.remove('mouseCursor');
      for (final state in ['Default', ..._states.values]) {
        properties['overlayColor$state'] = _null;
        properties['mouseCursor$state'] = _null;
      }
      covered.addAll(
        _decodedControl(_model(properties: properties)).properties.keys,
      );
      expect(covered, hasLength(37));
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      for (final sign in ['infinity', 'negativeInfinity']) {
        expect(
          _decodedControl(
            _model(
              properties: {
                for (final name in ['valuesStart', 'valuesEnd', 'min', 'max'])
                  name: _enum('double', sign),
                'divisions': _null,
                'labels': _null,
              },
            ),
          ),
          isNotNull,
        );
      }
    },
  );

  test(
    'RangeSlider rejects invalid pairs and exact whole/local exclusions',
    () {
      for (final props in <Map<String, Object?>>[
        {'valuesStart': _number(.8), 'valuesEnd': _number(.2)},
        {'valuesStart': _number(-1)},
        {'valuesEnd': _number(2)},
        {'min': _number(3), 'max': _number(2)},
        {'valuesStart': _null},
        {'valuesEnd': _null},
        {'divisions': _number(0)},
        {'divisions': _number(1.0)},
        {'divisions': _number(9007199254740992)},
        {'labels': _null, 'labelsStart': _string('')},
        {'labels': _reference, 'labelsEnd': _string('end')},
        {'mouseCursor': _reference, 'mouseCursorDefault': _null},
        {'overlayColor': _reference, 'overlayColorHovered': _null},
        {'mouseCursorDefault': _string('notACursor')},
        {'overlayColorDefault': _string('Colors.red')},
        {'year2023': _string('false')},
        {'variant': _string('adaptive')},
        {'focusNode': _reference},
        {'valuesStart': _enum('double', 'nan')},
        {
          'padding': {
            'kind': 'edgeInsets',
            'left': -1,
            'right': 0,
            'top': 0,
            'bottom': 0,
          },
        },
      ]) {
        expect(
          () => _decode(_model(properties: props)),
          throwsFormatException,
          reason: '$props',
        );
      }
      for (final required in ['valuesStart', 'valuesEnd', 'enabled']) {
        final model = _model();
        (_control(model)['properties'] as Map).remove(required);
        expect(() => _decode(model), throwsFormatException);
      }
      final withSlot = _model();
      _control(withSlot)['slots'] = {'child': _single(null)};
      expect(() => _decode(withSlot), throwsFormatException);
    },
  );

  for (final profile in ['windows', 'web']) {
    testWidgets(
      'RangeSlider $profile payload forwards all direct fields without project execution',
      (tester) async {
        final model = _model(
          profile: profile,
          properties: {
            'valuesStart': _number(20),
            'valuesEnd': _number(70),
            'min': _number(10),
            'max': _number(90),
            'divisions': _number(8),
            'labelsStart': _string('twenty'),
            'labelsEnd': _string('seventy'),
            'activeColor': _color(0xff123456),
            'inactiveColor': _color(0xff654321),
            'onChanged': _reference,
            'onChangeStart': _reference,
            'onChangeEnd': _reference,
            'semanticFormatterCallback': _reference,
            'year2023': _bool(false),
            'padding': {
              'kind': 'edgeInsetsDirectional',
              'start': 5,
              'end': 9,
              'top': 2,
              'bottom': 4,
            },
            'overlayColorDefault': _color(0xffabcdef),
            'mouseCursorDefault': _string('precise'),
          },
        );
        await _pump(tester, model);
        final sdk = _sdk(tester);
        expect(sdk.values, const RangeValues(20, 70));
        expect(sdk.min, 10);
        expect(sdk.max, 90);
        expect(sdk.divisions, 8);
        expect(sdk.labels, const RangeLabels('twenty', 'seventy'));
        expect(sdk.padding, const EdgeInsetsDirectional.fromSTEB(5, 2, 9, 4));
        expect(sdk.activeColor, const Color(0xff123456));
        expect(sdk.inactiveColor, const Color(0xff654321));
        expect(sdk.onChangeStart, isNotNull);
        expect(sdk.onChangeEnd, isNotNull);
        expect(sdk.semanticFormatterCallback, isNull);
        expect(sdk.mouseCursor!.resolve({}), SystemMouseCursors.precise);
        expect(sdk.overlayColor!.resolve({}), const Color(0xffabcdef));
        expect(_diagnostics(tester), contains('never executes'));
        final before = jsonEncode(model);
        sdk.onChanged!(const RangeValues(30, 60));
        sdk.onChangeStart!(sdk.values);
        sdk.onChangeEnd!(sdk.values);
        expect(jsonEncode(model), before);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'nullable labels and missing local peer preserve exact SDK presence',
    (tester) async {
      for (final props in <Map<String, Object?>>[
        {},
        {'labels': _null},
        {'labelsStart': _string('left')},
        {'labelsEnd': _string('right')},
        {'labelsStart': _string(''), 'labelsEnd': _string('')},
        {'labels': _reference},
      ]) {
        await _pump(tester, _model(properties: props));
        final local =
            props.containsKey('labelsStart') || props.containsKey('labelsEnd');
        expect(
          _sdk(tester).labels,
          local
              ? RangeLabels(
                  (props['labelsStart'] as Map?)?['value'] as String? ?? '',
                  (props['labelsEnd'] as Map?)?['value'] as String? ?? '',
                )
              : null,
        );
        expect(
          _diagnostics(tester).contains('Project RangeLabels'),
          props['labels'] == _reference,
        );
      }
    },
  );

  testWidgets(
    'state color and cursor maps preserve all 256 priorities and explicit null',
    (tester) async {
      final props = <String, Object?>{
        'overlayColorDefault': _color(0xff010101),
        'mouseCursorDefault': _string('clickable'),
        for (var i = 0; i < _states.length; i++) ...{
          'overlayColor${_states.values.elementAt(i)}': _color(0xff000010 + i),
          'mouseCursor${_states.values.elementAt(i)}': _string(
            i.isEven ? 'text' : 'wait',
          ),
        },
      };
      await _pump(tester, _model(properties: props));
      final sdk = _sdk(tester);
      for (var mask = 0; mask < 256; mask++) {
        final states = {
          for (var i = 0; i < 8; i++)
            if (mask & (1 << i) != 0) _states.keys.elementAt(i),
        };
        final first = List.generate(
          8,
          (i) => i,
        ).where((i) => mask & (1 << i) != 0).firstOrNull;
        expect(
          sdk.overlayColor!.resolve(states),
          first == null ? const Color(0xff010101) : Color(0xff000010 + first),
        );
        expect(
          sdk.mouseCursor!.resolve(states),
          first == null
              ? same(WidgetStateMouseCursor.clickable)
              : (first.isEven
                    ? SystemMouseCursors.text
                    : SystemMouseCursors.wait),
        );
      }
      props['overlayColorDisabled'] = _null;
      props['mouseCursorDisabled'] = _null;
      await _pump(tester, _model(properties: props));
      expect(
        _sdk(
          tester,
        ).overlayColor!.resolve({WidgetState.disabled, WidgetState.hovered}),
        isNull,
      );
      expect(
        _sdk(
          tester,
        ).mouseCursor!.resolve({WidgetState.disabled, WidgetState.hovered}),
        isNull,
      );
      expect(
        _sdk(tester).mouseCursor!.resolve({}),
        same(WidgetStateMouseCursor.clickable),
      );
      props['mouseCursorDisabled'] = _reference;
      await _pump(tester, _model(properties: props));
      expect(_sdk(tester).mouseCursor!.resolve({WidgetState.disabled}), isNull);
      expect(_diagnostics(tester), contains('Project cursors'));
    },
  );

  testWidgets(
    'all 41 reviewed cursor presets remain actual SDK cursor objects',
    (tester) async {
      const presets = [
        'defer',
        'uncontrolled',
        'clickable',
        'adaptiveClickable',
        'textable',
        'none',
        'basic',
        'click',
        'forbidden',
        'wait',
        'progress',
        'contextMenu',
        'help',
        'text',
        'verticalText',
        'cell',
        'precise',
        'move',
        'grab',
        'grabbing',
        'noDrop',
        'alias',
        'copy',
        'disappearing',
        'allScroll',
        'resizeLeftRight',
        'resizeUpDown',
        'resizeUpLeftDownRight',
        'resizeUpRightDownLeft',
        'resizeUp',
        'resizeDown',
        'resizeLeft',
        'resizeRight',
        'resizeUpLeft',
        'resizeUpRight',
        'resizeDownLeft',
        'resizeDownRight',
        'resizeColumn',
        'resizeRow',
        'zoomIn',
        'zoomOut',
      ];
      expect(presets, hasLength(41));
      for (final preset in presets) {
        await _pump(
          tester,
          _model(properties: {'mouseCursorDefault': _string(preset)}),
        );
        expect(
          _sdk(tester).mouseCursor!.resolve({WidgetState.disabled}),
          isA<MouseCursor>(),
          reason: preset,
        );
      }
    },
  );

  for (final material3 in [false, true]) {
    for (final year in [null, true, false]) {
      testWidgets(
        'raw pixels M3=$material3 year=$year across RTL platform and disabled/discrete',
        (tester) async {
          for (final platform in [TargetPlatform.windows, TargetPlatform.iOS]) {
            for (final rtl in [false, true]) {
              for (final enabled in [false, true]) {
                final theme = ThemeData(
                  useMaterial3: material3,
                  platform: platform,
                );
                final model = _model(
                  properties: {
                    'enabled': _bool(enabled),
                    'divisions': _number(4),
                    'labelsStart': _string('start'),
                    'labelsEnd': _string('end'),
                    if (year != null) 'year2023': _bool(year),
                  },
                );
                if (rtl) {
                  final center =
                      (model['root'] as Map)['slots']['body']['child'] as Map;
                  final boundary = (center['slots'] as Map)['child']['child'];
                  (center['slots'] as Map)['child'] = _single(
                    _node(
                      '3c505ba9-42b0-40bf-9506-86c066af133f',
                      'flutter.widgets.Directionality',
                      {'textDirection': _enum('TextDirection', 'rtl')},
                      {'child': _single(boundary)},
                    ),
                  );
                }
                await _pump(tester, model, theme: theme);
                await _expectRawPixels(
                  tester,
                  _raw(
                    enabled: enabled,
                    divisions: 4,
                    labels: const RangeLabels('start', 'end'),
                    year2023: year,
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
    'direct colors local state null and padding use exact theme precedence',
    (tester) async {
      final theme = ThemeData(
        sliderTheme: SliderThemeData(
          activeTrackColor: Colors.green,
          inactiveTrackColor: Colors.yellow,
          thumbColor: Colors.purple,
          overlayColor: WidgetStateColor.resolveWith((_) => Colors.pink),
          padding: const EdgeInsets.all(15),
        ),
      );
      final model = _model(
        properties: {
          'activeColor': _color(0xff0000ff),
          'inactiveColor': _color(0xffff0000),
          'overlayColorDefault': _null,
          'padding': {
            'kind': 'edgeInsetsDirectional',
            'start': 12,
            'end': 4,
            'top': 3,
            'bottom': 7,
          },
        },
      );
      await _pump(tester, model, theme: theme);
      await _expectRawPixels(
        tester,
        _raw(
          active: const Color(0xff0000ff),
          inactive: const Color(0xffff0000),
          overlay: WidgetStateProperty<Color?>.fromMap({WidgetState.any: null}),
          padding: const EdgeInsetsDirectional.fromSTEB(12, 3, 4, 7),
        ),
        theme,
      );
    },
  );

  testWidgets(
    'labels indicators use full text scale and all SDK policy branches',
    (tester) async {
      for (final policy in ShowValueIndicator.values) {
        for (final discrete in [false, true]) {
          final indicators = _Indicators();
          final model = _model(
            properties: {
              'labelsStart': _string('Left value'),
              'labelsEnd': _string('Right value'),
              if (discrete) 'divisions': _number(4),
            },
          );
          (model['profile'] as Map)['textScaleFactor'] = 2.5;
          await _pump(
            tester,
            model,
            theme: ThemeData(
              sliderTheme: SliderThemeData(
                rangeValueIndicatorShape: indicators,
                showValueIndicator: policy,
              ),
            ),
          );
          final rect = tester.getRect(find.byType(RangeSlider));
          final gesture = await tester.startGesture(
            Offset(rect.left + 24 + .25 * (rect.width - 48), rect.center.dy),
          );
          await tester.pump(const Duration(milliseconds: 250));
          final expected = switch (policy) {
            ShowValueIndicator.onlyForDiscrete => discrete,
            ShowValueIndicator.onlyForContinuous => !discrete,
            ShowValueIndicator.never => false,
            _ => true,
          };
          expect(
            indicators.paints > 0,
            expected,
            reason: '$policy discrete=$discrete',
          );
          if (expected) {
            expect(
              indicators.labels,
              containsAll(['Left value', 'Right value']),
            );
            expect(indicators.scales, {2.5});
          }
          await gesture.up();
          await tester.pump(const Duration(seconds: 1));
          await tester.pumpWidget(const SizedBox());
        }
      }
    },
  );

  testWidgets(
    'retained SDK state survives model history labels references colors and enable changes',
    (tester) async {
      final thumbs = _Thumbs();
      final theme = ThemeData(
        sliderTheme: SliderThemeData(rangeThumbShape: thumbs),
      );
      final model = _model();
      await _pump(tester, model, theme: theme);
      final state = tester.state(find.byType(RangeSlider));
      final initial = Map<Thumb, Offset>.of(thumbs.centers);
      final properties = _control(model)['properties'] as Map;
      properties['valuesStart'] = _number(.4);
      properties['valuesEnd'] = _number(.6);
      properties['labels'] = _reference;
      properties['mouseCursor'] = _reference;
      await _pump(tester, model, theme: theme);
      expect(tester.state(find.byType(RangeSlider)), same(state));
      expect(
        thumbs.centers[Thumb.start]!.dx,
        greaterThan(initial[Thumb.start]!.dx),
      );
      expect(thumbs.centers[Thumb.end]!.dx, lessThan(initial[Thumb.end]!.dx));
      properties['enabled'] = _bool(false);
      properties['onChanged'] = _reference;
      await _pump(tester, model, theme: theme);
      expect(_sdk(tester).onChanged, isNull);
      expect(tester.state(find.byType(RangeSlider)), same(state));
      properties.remove('labels');
      properties.remove('mouseCursor');
      properties.remove('onChanged');
      properties['enabled'] = _bool(true);
      properties['valuesStart'] = _number(.25);
      properties['valuesEnd'] = _number(.75);
      await _pump(tester, model, theme: theme);
      expect(tester.state(find.byType(RangeSlider)), same(state));
      expect(thumbs.centers, initial);
      expect(_diagnostics(tester), isNot(contains('preview limitation')));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'tap drag overlap and secondary pointer select without storing SDK gesture values',
    (tester) async {
      final selected = <String>[];
      for (final values in [
        const RangeValues(.25, .75),
        const RangeValues(.5, .5),
      ]) {
        final model = _model(
          properties: {
            'valuesStart': _number(values.start),
            'valuesEnd': _number(values.end),
          },
        );
        final before = jsonEncode(model);
        await _pump(tester, model, selected: selected);
        final rect = tester.getRect(find.byType(RangeSlider));
        for (final value in [values.start, values.end]) {
          await tester.dragFrom(
            Offset(rect.left + 24 + value * (rect.width - 48), rect.center.dy),
            const Offset(45, 0),
          );
          await tester.pump(const Duration(milliseconds: 250));
        }
        final gesture = await tester.startGesture(
          rect.center,
          buttons: kSecondaryMouseButton,
        );
        await gesture.up();
        await tester.pump();
        expect(selected, contains(_id));
        expect(_sdk(tester).values, values);
        expect(jsonEncode(model), before);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'disabled project onChanged is omitted but independent callbacks remain diagnosed',
    (tester) async {
      await _pump(
        tester,
        _model(properties: {'enabled': _bool(false), 'onChanged': _reference}),
      );
      expect(_diagnostics(tester), isNot(contains('Project value callbacks')));
      await _pump(
        tester,
        _model(
          properties: {
            'enabled': _bool(false),
            'onChanged': _reference,
            'onChangeStart': _reference,
            'onChangeEnd': _reference,
            'semanticFormatterCallback': _reference,
          },
        ),
      );
      expect(_sdk(tester).onChanged, isNull);
      expect(_sdk(tester).onChangeStart, isNotNull);
      expect(_sdk(tester).onChangeEnd, isNotNull);
      expect(_diagnostics(tester), contains('Project value callbacks'));
      expect(_diagnostics(tester), contains('default percentages'));
    },
  );

  testWidgets(
    'numeric guard follows SDK normalization and stable finite inverse interpolation',
    (tester) async {
      for (final equal in [1.0, double.infinity, double.negativeInfinity]) {
        Map<String, Object?> value = equal.isFinite
            ? _number(equal)
            : _enum(
                'double',
                equal.isNegative ? 'negativeInfinity' : 'infinity',
              );
        await _pump(
          tester,
          _model(
            properties: {
              for (final name in ['valuesStart', 'valuesEnd', 'min', 'max'])
                name: value,
            },
          ),
        );
        expect(find.byType(RangeSlider), findsOneWidget);
        expect(_sdk(tester).values, RangeValues(equal, equal));
        expect(tester.takeException(), isNull);
      }
      final overflow = _model(
        properties: {
          'min': _number(-1e308),
          'max': _number(1e308),
          'valuesStart': _number(-1e308),
          'valuesEnd': _number(0),
        },
      );
      await _pump(tester, overflow);
      expect(find.byType(RangeSlider), findsOneWidget);
      await tester.dragFrom(
        tester.getRect(find.byType(RangeSlider)).centerLeft +
            const Offset(25, 0),
        const Offset(50, 0),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      (_control(overflow)['properties'] as Map)['valuesEnd'] = _number(1e308);
      await _pump(tester, overflow);
      expect(find.byType(RangeSlider), findsNothing);
      expect(_diagnostics(tester), contains('normalization'));
      for (final enabled in [false, true]) {
        final model = _model(
          properties: {
            'max': _enum('double', 'infinity'),
            'enabled': _bool(enabled),
          },
        );
        final before = jsonEncode(model);
        await _pump(tester, model);
        expect(
          find.byType(RangeSlider),
          enabled ? findsNothing : findsOneWidget,
        );
        expect(jsonEncode(model), before);
        if (enabled) {
          expect(_diagnostics(tester), contains('inverse range interpolation'));
        }
      }
    },
  );

  testWidgets(
    'large portable divisions use real SDK unless resolved tick density is unsafe',
    (tester) async {
      for (final modern in [false, true]) {
        final model = _model(
          properties: {
            'divisions': _number(9007199254740991),
            'year2023': _bool(!modern),
          },
        );
        await _pump(tester, model, theme: ThemeData());
        expect(find.byType(RangeSlider), findsOneWidget);
        for (final width in [0.0, 1e-300]) {
          final ticks = _Ticks(width);
          await _pump(
            tester,
            model,
            theme: ThemeData(
              sliderTheme: SliderThemeData(rangeTickMarkShape: ticks),
            ),
          );
          expect(find.byType(RangeSlider), findsNothing);
          expect(ticks.count, 0);
          expect(_diagnostics(tester), contains('10,000-division'));
        }
      }
    },
  );

  testWidgets(
    'bounded overflowing padding stays SDK exact and unbounded axis warns',
    (tester) async {
      final model = _model(
        properties: {
          'padding': {
            'kind': 'edgeInsets',
            'left': 1e308,
            'right': 1e308,
            'top': 0,
            'bottom': 0,
          },
        },
      );
      await _pump(tester, model);
      expect(find.byType(RangeSlider), findsOneWidget);
      final boundary =
          (model['root']
                  as Map)['slots']['body']['child']['slots']['child']['child']
              as Map;
      boundary['slots'] = {
        'child': _single(
          _node(
            '58f1fa36-f48c-45fc-acf6-5084c3985b36',
            'flutter.widgets.Row',
            {},
            {
              'children': {
                'kind': 'list',
                'children': [_control(model)],
              },
            },
          ),
        ),
      };
      await _pump(tester, model);
      expect(find.byType(RangeSlider), findsNothing);
      expect(_diagnostics(tester), contains('padding'));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'two independent semantic sliders and internal focus preserve controlled values',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        final model = _model(properties: {'divisions': _number(4)});
        await _pump(tester, model);
        final nodes = <SemanticsNode>[];
        // ignore: deprecated_member_use
        tester.binding.pipelineOwner.semanticsOwner!.rootSemanticsNode!
            .visitChildren((node) {
              void visit(SemanticsNode child) {
                if (child.getSemanticsData().flagsCollection.isSlider) {
                  nodes.add(child);
                }
                child.visitChildren((next) {
                  visit(next);
                  return true;
                });
              }

              visit(node);
              return true;
            });
        expect(nodes, hasLength(2));
        expect(nodes.map((v) => v.getSemanticsData().value).toSet(), {
          '25%',
          '75%',
        });
        final state = tester.state(find.byType(RangeSlider));
        // ignore: deprecated_member_use
        tester.binding.pipelineOwner.semanticsOwner!.performAction(
          nodes.first.id,
          ui.SemanticsAction.increase,
        );
        await tester.pump();
        expect(_sdk(tester).values, const RangeValues(.25, .75));
        await tester.sendKeyEvent(LogicalKeyboardKey.tab);
        await tester.pump();
        expect(tester.state(find.byType(RangeSlider)), same(state));
        expect(tester.takeException(), isNull);
      } finally {
        semantics.dispose();
      }
    },
  );
}

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
                    'valuesStart': _number(.25),
                    'valuesEnd': _number(.75),
                    'enabled': _bool(true),
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
RangeSlider _sdk(WidgetTester tester) =>
    tester.widget<RangeSlider>(find.byType(RangeSlider));
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
  RangeSlider raw,
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
  final size = tester.getSize(find.byType(RangeSlider));
  final direction = Directionality.of(tester.element(find.byType(RangeSlider)));
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
  expect(tester.getSize(find.byType(RangeSlider)), size);
  final actual = await _pixels(
    tester,
    key.currentContext!.findRenderObject()! as RenderRepaintBoundary,
  );
  if (!listEquals(actual, pixels)) {
    String bounds(Uint8List data) {
      var x0 = 100000, y0 = 100000, x1 = 0, y1 = 0, count = 0;
      for (var i = 3; i < data.length; i += 4) {
        if (data[i] > 0) {
          final x = (i ~/ 4) % size.width.ceil(),
              y = (i ~/ 4) ~/ size.width.ceil();
          if (x < x0) x0 = x;
          if (x > x1) x1 = x;
          if (y < y0) y0 = y;
          if (y > y1) y1 = y;
          count++;
        }
      }
      return '$x0,$y0-$x1,$y1 ($count)';
    }

    expect(
      actual,
      pixels,
      reason: 'size=$size Canvas=${bounds(pixels)} SDK=${bounds(actual)}',
    );
  }
  expect(tester.takeException(), isNull);
}
