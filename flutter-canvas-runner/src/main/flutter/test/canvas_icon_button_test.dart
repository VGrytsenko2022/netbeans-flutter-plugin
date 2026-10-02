import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.IconButton';
const _id = 'f1dd3aa1-fc2f-45b7-a9fc-0c8b5e1ac511';
const _childId = '7c37c659-01b5-46e5-852f-11abdb77504c';
const _iconId = 'ca8c98a2-0aa1-4e41-bd02-8c123ecdbdad';
const _reference = {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _number(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _color(int value) => {
  'kind': 'color',
  'argb': '0x${value.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _node(
  String id,
  String type,
  Map<String, Object?> properties, [
  Map<String, Object?> slots = const {},
]) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _model({
  String variant = 'standard',
  bool icon = false,
  String platform = 'windows',
  double scale = 1,
  Map<String, Object?> properties = const {},
  bool empty = false,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  (model['profile'] as Map)['textScaleFactor'] = scale;
  final button = _node(
    _id,
    _type,
    {'enabled': _bool(true), 'variant': _string(variant), ...properties},
    {
      'icon': _single(
        empty
            ? _node(_childId, 'flutter.widgets.SizedBox', {
                'width': _number(0),
                'height': _number(0),
              })
            : _node(_childId, 'flutter.widgets.Text', {
                'data': _string('Action'),
              }),
      ),
      if (icon)
        'selectedIcon': _single(
          _node(_iconId, 'flutter.widgets.Text', {'data': _string('I')}),
        ),
    },
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
          {'child': _single(button)},
        ),
      ),
    },
  );
  return model;
}

Map _button(Map model) =>
    model['root']['slots']['body']['child']['slots']['child']['child'] as Map;
CanvasModel _decode(Map model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
IconButton _sdk(WidgetTester tester) =>
    tester.widget<IconButton>(find.byType(IconButton));
String _diagnostics(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((v) => v.message ?? '')
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  String? selectedId,
  IconThemeData? iconTheme,
  void Function(CanvasDropResolver?)? onDrop,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: IconTheme(
        data: iconTheme ?? const IconThemeData.fallback(),
        child: CanvasDocumentView(
          model: _decode(model),
          selectedWidgetId: selectedId,
          onSelected: selected?.add ?? (_) {},
          onDropResolverChanged: onDrop,
        ),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
  await tester.pump();
}

const _variants = ['standard', 'filled', 'filledTonal', 'outlined'];

IconButton _raw(
  String variant, {
  Widget icon = const Text('Action'),
  Widget? selectedIcon,
  bool? selected,
  bool enabled = true,
  ButtonStyle? style,
  double? iconSize,
  double? splashRadius,
  BoxConstraints? constraints,
  VisualDensity? density,
  Color? color,
}) {
  final args = (onPressed: enabled ? () {} : null);
  return switch (variant) {
    'filled' => IconButton.filled(
      onPressed: args.onPressed,
      icon: icon,
      selectedIcon: selectedIcon,
      isSelected: selected,
      style: style,
      iconSize: iconSize,
      splashRadius: splashRadius,
      constraints: constraints,
      visualDensity: density,
      color: color,
    ),
    'filledTonal' => IconButton.filledTonal(
      onPressed: args.onPressed,
      icon: icon,
      selectedIcon: selectedIcon,
      isSelected: selected,
      style: style,
      iconSize: iconSize,
      splashRadius: splashRadius,
      constraints: constraints,
      visualDensity: density,
      color: color,
    ),
    'outlined' => IconButton.outlined(
      onPressed: args.onPressed,
      icon: icon,
      selectedIcon: selectedIcon,
      isSelected: selected,
      style: style,
      iconSize: iconSize,
      splashRadius: splashRadius,
      constraints: constraints,
      visualDensity: density,
      color: color,
    ),
    _ => IconButton(
      onPressed: args.onPressed,
      icon: icon,
      selectedIcon: selectedIcon,
      isSelected: selected,
      style: style,
      iconSize: iconSize,
      splashRadius: splashRadius,
      constraints: constraints,
      visualDensity: density,
      color: color,
    ),
  };
}

ButtonStyleButton _m3(WidgetTester tester) => tester.widget<ButtonStyleButton>(
  find.byWidgetPredicate((w) => w is ButtonStyleButton),
);

String _section(String contract) {
  final start = contract.indexOf('W|$_type\n');
  return contract.substring(start, contract.indexOf('\nW|', start + 1) + 1);
}

Map<String, Object?> _allLocalStyles() {
  final elevated = fixture.elevatedButtonPropertiesForViewTest();
  final result = <String, Object?>{
    for (final entry in elevated.entries)
      if (entry.key.startsWith('style')) entry.key: entry.value,
  };
  for (final prefix in [
    'styleError',
    'styleDragged',
    'styleSelected',
    'styleScrolledUnder',
  ]) {
    for (final entry in elevated.entries.where(
      (entry) => entry.key.startsWith('stylePressed'),
    )) {
      result['$prefix${entry.key.substring('stylePressed'.length)}'] =
          entry.value;
    }
  }
  result.addAll({
    'styleIconAlignment': _enum('IconAlignment', 'end'),
    'styleBackgroundBuilder': _reference,
    'styleForegroundBuilder': _reference,
  });
  return result;
}

Future<(Size, Uint8List)> _pixels(
  WidgetTester tester,
  RenderRepaintBoundary boundary,
) async => (await tester.runAsync(() async {
  final image = await boundary.toImage(pixelRatio: 1);
  try {
    final bytes = (await image.toByteData(format: ui.ImageByteFormat.rawRgba))!;
    return (
      Size(image.width.toDouble(), image.height.toDouble()),
      bytes.buffer.asUint8List(),
    );
  } finally {
    image.dispose();
  }
}))!;

void main() {
  testWidgets(
    'Unsafe selected-button geometry preserves the active selected icon editing State through repair/history',
    (tester) async {
      Object? retained;
      for (final unsafe in [false, true, false, true, false]) {
        final data = _model(
          icon: true,
          properties: {
            'isSelected': _bool(true),
            'constraints': {
              'kind': 'boxConstraints',
              'minWidth': unsafe ? null : 60.0,
              'maxWidth': null,
              'minHeight': 0.0,
              'maxHeight': 80.0,
            },
          },
        );
        final button = _button(data);
        button['slots']['selectedIcon'] = _single(
          _node(
            _iconId,
            'flutter.widgets.SizedBox',
            {'width': _number(80)},
            {
              'child': _single(
                _node(
                  'd608bf11-9845-40a4-9b9b-4f8fa2e0b0ad',
                  'flutter.material.TextField',
                  {},
                ),
              ),
            },
          ),
        );
        (data['root']
            as Map)['slots']['body']['child']['slots']['child'] = _single(
          _node(
            '4961a7f0-7186-461c-9b1a-6c8986a75b4c',
            'flutter.widgets.Row',
            {},
            {
              'children': {
                'kind': 'list',
                'children': [button],
              },
            },
          ),
        );
        await _pump(tester, data, selectedId: _iconId);
        expect(_widget(_childId), findsNothing);
        expect(_widget(_iconId), findsOneWidget);
        final edit = tester.state<EditableTextState>(find.byType(EditableText));
        retained ??= edit;
        expect(edit, same(retained));
        if (edit.widget.controller.text.isEmpty) {
          edit.widget.controller.text = 'retained';
        }
        expect(edit.widget.controller.text, 'retained');
        expect(find.byType(IconButton), unsafe ? findsNothing : findsOneWidget);
        expect(tester.takeException(), isNull);
      }
    },
  );
  for (final material3 in [false, true]) {
    testWidgets(
      '${material3 ? "M3" : "M2"} direct density missing axis uses SDK zero not inherited theme axis',
      (tester) async {
        final theme = ThemeData(
          useMaterial3: material3,
          visualDensity: const VisualDensity(vertical: -3),
          iconButtonTheme: const IconButtonThemeData(
            style: ButtonStyle(visualDensity: VisualDensity(vertical: -3)),
          ),
        );
        await _pump(
          tester,
          _model(properties: {'visualDensityHorizontal': _number(1.0)}),
          theme: theme,
        );
        expect(_sdk(tester).visualDensity, const VisualDensity(horizontal: 1));
        final size = tester.getSize(find.byType(IconButton));
        await tester.pumpWidget(
          MaterialApp(
            theme: theme,
            home: Scaffold(
              body: Center(
                child: _raw(
                  'standard',
                  density: const VisualDensity(horizontal: 1),
                ),
              ),
            ),
          ),
        );
        await tester.pump(const Duration(milliseconds: 300));
        expect(tester.getSize(find.byType(IconButton)), size);
        expect(tester.takeException(), isNull);
      },
    );
    testWidgets(
      '${material3 ? "M3" : "M2"} infinite direct minimum is bounded by parent or diagnosed on unbounded axis',
      (tester) async {
        final data = _model(
          properties: {
            'constraints': {
              'kind': 'boxConstraints',
              'minWidth': null,
              'maxWidth': null,
              'minHeight': 0.0,
              'maxHeight': 100.0,
            },
          },
        );
        await _pump(tester, data, theme: ThemeData(useMaterial3: material3));
        expect(find.byType(IconButton), findsOneWidget);
        expect(tester.takeException(), isNull);
        final button = _button(data);
        (data['root']
            as Map)['slots']['body']['child']['slots']['child'] = _single(
          _node(
            '4961a7f0-7186-461c-9b1a-6c8986a75b4c',
            'flutter.widgets.Row',
            {},
            {
              'children': {
                'kind': 'list',
                'children': [button],
              },
            },
          ),
        );
        await _pump(tester, data, theme: ThemeData(useMaterial3: material3));
        expect(find.byType(IconButton), findsNothing);
        expect(
          _diagnostics(tester),
          contains('constraints preview unavailable'),
        );
        expect(tester.takeException(), isNull);
        if (material3) {
          button['properties']['styleMinimumWidth'] = _number(60.0);
          await _pump(tester, data, theme: ThemeData(useMaterial3: true));
          expect(find.byType(IconButton), findsOneWidget);
          expect(tester.takeException(), isNull);
        }
      },
    );
  }
  for (final platform in ['windows', 'web']) {
    testWidgets(
      '$platform multiple IconButtons preserve icon editing State through variants references theme and history',
      (tester) async {
        const secondButtonId = '145d1fcf-0e3b-4878-b85e-5b5e18edeb38';
        const secondChildId = 'a8a81f01-e4cf-444b-bc5f-cec892922982';
        Object? firstState;
        Object? secondState;
        for (final phase in [0, 1, 2, 3, 4, 3, 2, 1, 0]) {
          final data = _model(
            platform: platform,
            variant: _variants[phase % 4],
            properties: {
              if (phase == 2) 'style': _reference,
              if (phase == 3) 'focusNode': _reference,
              if (phase == 4) 'statesController': _reference,
              'constraints': {
                'kind': 'boxConstraints',
                'minWidth': 90.0,
                'minHeight': 45.0,
                'maxWidth': 110.0,
                'maxHeight': 60.0,
              },
            },
          );
          final first = _button(data);
          first['slots']['icon'] = _single(
            _node(
              _childId,
              'flutter.widgets.SizedBox',
              {'width': _number(80)},
              {
                'child': _single(
                  _node(
                    'd608bf11-9845-40a4-9b9b-4f8fa2e0b0ad',
                    'flutter.material.TextField',
                    {},
                  ),
                ),
              },
            ),
          );
          final second = jsonDecode(jsonEncode(first)) as Map;
          second['id'] = secondButtonId;
          second['slots']['icon']['child']['id'] = secondChildId;
          second['slots']['icon']['child']['slots']['child']['child']['id'] =
              '43b07a12-82d9-4379-a5c4-81f5d05b4ef4';
          (data['root']
              as Map)['slots']['body']['child']['slots']['child'] = _single(
            _node(
              '4961a7f0-7186-461c-9b1a-6c8986a75b4c',
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
          await _pump(
            tester,
            data,
            theme: ThemeData(useMaterial3: phase != 4),
            selectedId: _childId,
          );
          final edits = tester
              .stateList<EditableTextState>(find.byType(EditableText))
              .toList();
          expect(edits, hasLength(2));
          firstState ??= edits[0];
          secondState ??= edits[1];
          expect(edits[0], same(firstState));
          expect(edits[1], same(secondState));
          if (phase == 0 && edits[0].widget.controller.text.isEmpty) {
            edits[0].widget.controller.text = 'First';
            edits[1].widget.controller.text = 'Second';
          }
          expect(edits[0].widget.controller.text, 'First');
          expect(edits[1].widget.controller.text, 'Second');
          expect(tester.takeException(), isNull);
        }
      },
    );
  }
  testWidgets(
    'SelectedIcon mounts only in selected Material3 and primary child retention matches raw SDK',
    (tester) async {
      final selectedData = _model(
        icon: true,
        properties: {'isSelected': _bool(false)},
      );
      await _pump(tester, selectedData);
      expect(_widget(_childId), findsOneWidget);
      expect(_widget(_iconId), findsNothing);
      _button(selectedData)['properties']['isSelected'] = _bool(true);
      await _pump(tester, selectedData);
      expect(_widget(_childId), findsNothing);
      expect(_widget(_iconId), findsOneWidget);
      final selectedState = tester.element(_widget(_iconId));
      _button(selectedData)['properties']['variant'] = _string('outlined');
      await _pump(tester, selectedData);
      expect(tester.element(_widget(_iconId)), same(selectedState));
      await _pump(tester, selectedData, theme: ThemeData(useMaterial3: false));
      expect(_widget(_childId), findsOneWidget);
      expect(_widget(_iconId), findsNothing);
      _button(selectedData)['properties']['isSelected'] = {'kind': 'null'};
      await _pump(tester, selectedData);
      expect(_widget(_childId), findsOneWidget);
      expect(_widget(_iconId), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'Real local hover focus press and external style state maps remain live',
    (tester) async {
      await _pump(
        tester,
        _model(
          properties: {
            'autofocus': _bool(true),
            'styleHoveredBackgroundColor': _color(0xff123456),
            'stylePressedBackgroundColor': _color(0xffabcdef),
            'onHover': _reference,
            'onLongPress': _reference,
          },
        ),
      );
      final controller = _m3(tester).statesController!;
      expect(controller.value, contains(WidgetState.focused));
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer();
      await mouse.moveTo(tester.getCenter(find.byType(IconButton)));
      await tester.pump();
      expect(controller.value, contains(WidgetState.hovered));
      await mouse.down(tester.getCenter(find.byType(IconButton)));
      await tester.pump(const Duration(milliseconds: 30));
      // Canvas owns physical presses for selection. Exercise the SDK's local
      // controller without changing the stored model or that host policy.
      controller.update(WidgetState.pressed, true);
      await tester.pump();
      expect(controller.value, contains(WidgetState.pressed));
      expect(
        _sdk(
          tester,
        ).style!.backgroundColor!.resolve(controller.value)!.toARGB32(),
        0xffabcdef,
      );
      await mouse.up();
      await mouse.removePointer();
      controller.update(WidgetState.pressed, false);
      await tester.pump(const Duration(milliseconds: 300));
      expect(controller.value, isNot(contains(WidgetState.pressed)));
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'IconButtonTheme beats IconTheme and explicit styles/direct values keep actualSDK precedence',
    (tester) async {
      for (final iconTheme in [
        IconThemeData(size: 24, color: kDefaultIconDarkColor),
        IconThemeData(size: 24, color: Color(kDefaultIconDarkColor.toARGB32())),
        const IconThemeData(size: 37, color: Colors.purple),
      ]) {
        for (final themed in [false, true]) {
          for (final direct in [false, true]) {
            for (final local in [false, true]) {
              final theme = ThemeData(
                iconButtonTheme: themed
                    ? const IconButtonThemeData(
                        style: ButtonStyle(
                          iconSize: WidgetStatePropertyAll(30),
                          iconColor: WidgetStatePropertyAll(Colors.green),
                        ),
                      )
                    : null,
              );
              final data = _model(
                properties: {
                  if (direct) 'iconSize': _number(33),
                  if (direct) 'color': _color(0xffff0000),
                  if (local) 'styleIconSize': _number(41.0),
                  if (local) 'styleIconColor': _color(0xff0000ff),
                },
              );
              _button(data)['slots']['icon'] = _single(
                _node(_childId, 'flutter.widgets.Icon', {
                  'icon': fixture.iconDataValueForViewTest(),
                }),
              );
              await _pump(tester, data, theme: theme, iconTheme: iconTheme);
              final canvasTheme = IconTheme.of(
                tester.element(find.byType(Icon)),
              );
              final canvasSize = tester.getSize(find.byType(IconButton));
              await tester.pumpWidget(
                MaterialApp(
                  theme: theme,
                  home: IconTheme(
                    data: iconTheme,
                    child: Scaffold(
                      body: Center(
                        child: _raw(
                          'standard',
                          icon: const Icon(Icons.star),
                          iconSize: direct ? 33 : null,
                          color: direct ? const Color(0xffff0000) : null,
                          style: local
                              ? const ButtonStyle(
                                  iconSize: WidgetStatePropertyAll(41),
                                  iconColor: WidgetStatePropertyAll(
                                    Color(0xff0000ff),
                                  ),
                                )
                              : null,
                        ),
                      ),
                    ),
                  ),
                ),
              );
              await tester.pump(const Duration(milliseconds: 300));
              final rawTheme = IconTheme.of(tester.element(find.byType(Icon)));
              expect(canvasTheme.size, rawTheme.size);
              expect(canvasTheme.color, rawTheme.color);
              expect(tester.getSize(find.byType(IconButton)), canvasSize);
              expect(tester.takeException(), isNull);
            }
          }
        }
      }
    },
  );
  for (final material3 in [false, true]) {
    testWidgets(
      '${material3 ? "M3" : "M2"} all variants toggle states enabled states and RTL have exact SDK pixels',
      (tester) async {
        for (final variant in _variants) {
          for (final selected in [null, false, true]) {
            for (final enabled in [false, true]) {
              for (final rtl in [false, true]) {
                final theme = ThemeData(
                  useMaterial3: material3,
                  colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
                );
                final data = _model(
                  variant: variant,
                  icon: true,
                  properties: {
                    'enabled': _bool(enabled),
                    if (selected != null) 'isSelected': _bool(selected),
                  },
                );
                final button = _button(data);
                const boundaryId = '0b8f3c97-04f1-4bfe-a5d1-c2080c1b2044';
                (data['root']
                        as Map)['slots']['body']['child']['slots']['child'] =
                    _single(
                      _node(boundaryId, 'flutter.widgets.RepaintBoundary', {}, {
                        'child': _single(button),
                      }),
                    );
                if (rtl) {
                  final body = (data['root'] as Map)['slots']['body']['child'];
                  (data['root'] as Map)['slots']['body']['child'] = _node(
                    'bbccfa29-6a14-4a8a-8977-19716139d319',
                    'flutter.widgets.Directionality',
                    {'textDirection': _enum('TextDirection', 'rtl')},
                    {'child': _single(body)},
                  );
                }
                await _pump(tester, data, theme: theme);
                for (final id in [
                  _id,
                  material3 && selected == true ? _iconId : _childId,
                ]) {
                  tester
                          .renderObject<RenderCustomPaint>(
                            find.byKey(ValueKey('canvas-widget-outline-$id')),
                          )
                          .foregroundPainter =
                      null;
                }
                await tester.pump();
                final canvas = await _pixels(
                  tester,
                  tester.renderObject<RenderRepaintBoundary>(
                    find
                        .descendant(
                          of: _widget(boundaryId),
                          matching: find.byType(RepaintBoundary),
                        )
                        .first,
                  ),
                );
                final rawKey = GlobalKey();
                await tester.pumpWidget(
                  MaterialApp(
                    theme: theme,
                    home: Directionality(
                      textDirection: rtl
                          ? TextDirection.rtl
                          : TextDirection.ltr,
                      child: Scaffold(
                        body: Center(
                          child: RepaintBoundary(
                            key: rawKey,
                            child: _raw(
                              variant,
                              enabled: enabled,
                              selected: selected,
                              selectedIcon: const Text('I'),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                );
                await tester.pump(const Duration(milliseconds: 300));
                final raw = await _pixels(
                  tester,
                  rawKey.currentContext!.findRenderObject()!
                      as RenderRepaintBoundary,
                );
                expect(
                  canvas.$1,
                  raw.$1,
                  reason:
                      '$variant selected=$selected enabled=$enabled rtl=$rtl',
                );
                expect(
                  canvas.$2,
                  orderedEquals(raw.$2),
                  reason:
                      '$variant selected=$selected enabled=$enabled rtl=$rtl',
                );
                expect(tester.takeException(), isNull);
              }
            }
          }
        }
      },
    );
  }
  testWidgets(
    'Closed 498-field local style contract supports dense fixture and all nine state buckets',
    (tester) async {
      final styles = _allLocalStyles();
      final data = _model(properties: styles);
      await _pump(tester, data);
      final style = _sdk(tester).style!;
      const statesList = [
        WidgetState.disabled,
        WidgetState.error,
        WidgetState.dragged,
        WidgetState.pressed,
        WidgetState.selected,
        WidgetState.scrolledUnder,
        WidgetState.hovered,
        WidgetState.focused,
      ];
      for (var bits = 0; bits < 256; bits++) {
        final states = <WidgetState>{
          for (var i = 0; i < statesList.length; i++)
            if ((bits & (1 << i)) != 0) statesList[i],
        };
        expect(style.backgroundColor!.resolve(states), isNotNull);
        expect(style.foregroundColor!.resolve(states), isNotNull);
        expect(style.overlayColor!.resolve(states), isNotNull);
        expect(style.shadowColor!.resolve(states), isNotNull);
        expect(style.surfaceTintColor!.resolve(states), isNotNull);
        expect(style.elevation!.resolve(states), isNonNegative);
        expect(style.padding!.resolve(states), isNotNull);
        expect(style.minimumSize!.resolve(states), isNotNull);
        expect(style.maximumSize!.resolve(states), isNotNull);
        expect(style.fixedSize!.resolve(states), isNotNull);
        expect(style.iconColor!.resolve(states), isNotNull);
        expect(style.iconSize!.resolve(states), isNotNull);
        expect(style.textStyle!.resolve(states), isNotNull);
        expect(style.side!.resolve(states), isNotNull);
        expect(style.shape!.resolve(states), isNotNull);
        expect(style.mouseCursor!.resolve(states), isNotNull);
      }
      expect(style.backgroundBuilder, isNotNull);
      expect(style.foregroundBuilder, isNotNull);
      expect(_diagnostics(tester), contains('Project layer content'));
      final closed = Map<String, Object?>.from(styles)..['style'] = _reference;
      expect(() => _decode(_model(properties: closed)), throwsFormatException);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'All state combinations preserve disabled/error/dragged/pressed/selected/scrolled/hover/focus/default priority',
    (tester) async {
      const states = [
        WidgetState.disabled,
        WidgetState.error,
        WidgetState.dragged,
        WidgetState.pressed,
        WidgetState.selected,
        WidgetState.scrolledUnder,
        WidgetState.hovered,
        WidgetState.focused,
      ];
      const prefixes = [
        'styleDisabled',
        'styleError',
        'styleDragged',
        'stylePressed',
        'styleSelected',
        'styleScrolledUnder',
        'styleHovered',
        'styleFocused',
      ];
      await _pump(
        tester,
        _model(
          properties: {
            'styleForegroundColor': _color(0xff112200),
            for (var i = 0; i < 8; i++)
              '${prefixes[i]}ForegroundColor': _color(0xff112201 + i),
          },
        ),
      );
      for (var bits = 0; bits < 256; bits++) {
        final active = <WidgetState>{
          for (var i = 0; i < 8; i++)
            if ((bits & (1 << i)) != 0) states[i],
        };
        final first = List.generate(
          8,
          (i) => i,
        ).where((i) => (bits & (1 << i)) != 0).firstOrNull;
        expect(
          _sdk(tester).style!.foregroundColor!.resolve(active)!.toARGB32(),
          first == null ? 0xff112200 : 0xff112201 + first,
        );
      }
    },
  );
  for (final variant in _variants) {
    testWidgets(
      '$variant partial outline/shape fields preserve exact public SDK theme and selected defaults',
      (tester) async {
        final theme = ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        );
        for (final selected in [null, false, true]) {
          for (final disabled in [false, true]) {
            await _pump(
              tester,
              _model(
                variant: variant,
                properties: {
                  'enabled': _bool(!disabled),
                  if (selected != null) 'isSelected': _bool(selected),
                  'styleSideWidth': _number(3.0),
                  'styleSideStyle': _enum('BorderStyle', 'solid'),
                  'styleShapeKind': _string('stadium'),
                  'styleDisabledSideWidth': _number(3.0),
                  'styleDisabledSideStyle': _enum('BorderStyle', 'solid'),
                  'styleDisabledShapeKind': _string('stadium'),
                },
              ),
              theme: theme,
            );
            final active = <WidgetState>{
              if (disabled) WidgetState.disabled,
              if (selected == true) WidgetState.selected,
            };
            final side = _sdk(tester).style!.side!.resolve(active)!;
            final inheritedColor = variant == 'outlined' && selected != true
                ? disabled
                      ? theme.colorScheme.onSurface.withAlpha(31)
                      : theme.colorScheme.outline
                : BorderSide.none.color;
            expect(side, BorderSide(color: inheritedColor, width: 3));
            expect(
              _sdk(tester).style!.shape!.resolve(active),
              isA<StadiumBorder>(),
            );
            final canvasSize = tester.getSize(find.byType(IconButton));
            await tester.pumpWidget(
              MaterialApp(
                theme: theme,
                home: Scaffold(
                  body: Center(
                    child: _raw(
                      variant,
                      selected: selected,
                      enabled: !disabled,
                      style: ButtonStyle(
                        side: WidgetStatePropertyAll(side),
                        shape: const WidgetStatePropertyAll(StadiumBorder()),
                      ),
                    ),
                  ),
                ),
              ),
            );
            await tester.pump(const Duration(milliseconds: 300));
            expect(tester.getSize(find.byType(IconButton)), canvasSize);
            expect(tester.takeException(), isNull);
          }
        }
      },
    );
  }
  testWidgets(
    'Bounded M2 infinity size with finite ink radius is retained and unbounded axis is diagnosed',
    (tester) async {
      final props = {
        'iconSize': _enum('double', 'infinity'),
        'splashRadius': _number(20.0),
      };
      final model = _model(properties: props);
      await _pump(tester, model, theme: ThemeData(useMaterial3: false));
      expect(find.byType(IconButton), findsOneWidget);
      final size = tester.getSize(find.byType(IconButton));
      await tester.tap(find.byType(IconButton));
      await tester.pump(const Duration(milliseconds: 30));
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(useMaterial3: false),
          home: Scaffold(
            body: Center(
              child: OverflowBox(
                maxWidth: size.width,
                maxHeight: size.height,
                child: SizedBox.fromSize(
                  size: size,
                  child: _raw(
                    'standard',
                    iconSize: double.infinity,
                    splashRadius: 20,
                  ),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pump();
      expect(tester.getSize(find.byType(IconButton)), size);
      final button = _button(model);
      (model['root']
          as Map)['slots']['body']['child']['slots']['child'] = _single(
        _node(
          '11fbb111-ade1-4cfe-b15c-45cdbdbcea34',
          'flutter.widgets.Row',
          {},
          {
            'children': {
              'kind': 'list',
              'children': [button],
            },
          },
        ),
      );
      await _pump(tester, model, theme: ThemeData(useMaterial3: false));
      expect(find.byType(IconButton), findsNothing);
      expect(
        _diagnostics(tester),
        contains('unbounded Material 2 layout axis'),
      );
      expect(tester.takeException(), isNull);
    },
  );
  test(
    'All direct model domains are exact and nullable selection survives payload roundtrip',
    () {
      final cases = <String, List<Object?>>{
        'isSelected': [
          {'kind': 'null'},
          _bool(false),
          _bool(true),
        ],
        'iconSize': [
          _number(-2),
          _number(0),
          _number(3.5),
          _enum('double', 'infinity'),
        ],
        'splashRadius': [_number(1), _number(.01), _enum('double', 'infinity')],
        'visualDensityHorizontal': [_number(-4.0), _number(4.0)],
        'visualDensityVertical': [_number(-4.0), _number(4.0)],
      };
      for (final entry in cases.entries) {
        for (final value in entry.value) {
          final model = _model(properties: {entry.key: value});
          expect(
            () => _decode(model),
            returnsNormally,
            reason: '${entry.key}: $value',
          );
          expect(
            _button(jsonDecode(jsonEncode(model)))['properties'][entry.key],
            value,
          );
        }
      }
      for (final entry in {
        'splashRadius': _number(0),
        'visualDensityHorizontal': _number(4),
        'visualDensityVertical': _number(4.1),
        'iconSize': _enum('double', 'nan'),
      }.entries) {
        expect(
          () => _decode(_model(properties: {entry.key: entry.value})),
          throwsFormatException,
        );
      }
    },
  );
  for (final material3 in [false, true]) {
    for (final value in [-1.0, double.infinity]) {
      testWidgets(
        'Raw SDK ${material3 ? "M3 Icon" : "M2 square"} characterizes mounted iconSize $value',
        (tester) async {
          final errors = <FlutterErrorDetails>[];
          final old = FlutterError.onError;
          FlutterError.onError = errors.add;
          try {
            await tester.pumpWidget(
              MaterialApp(
                theme: ThemeData(useMaterial3: material3),
                home: Scaffold(
                  body: Center(
                    child: _raw(
                      'standard',
                      iconSize: value,
                      icon: material3
                          ? const Icon(Icons.star)
                          : const Text('Action'),
                    ),
                  ),
                ),
              ),
            );
            await tester.pump();
            expect(
              errors,
              !material3 && value.isInfinite ? isEmpty : isNotEmpty,
            );
            await tester.pumpWidget(const SizedBox.shrink());
          } finally {
            FlutterError.onError = old;
          }
        },
      );
      testWidgets(
        'Canvas ${material3 ? "M3 mounted Icon" : "M2 square"} isolates unsafe size $value without changing model',
        (tester) async {
          final valueData = value.isInfinite
              ? _enum('double', 'infinity')
              : _number(value);
          final model = _model(properties: {'iconSize': valueData});
          if (material3) {
            _button(model)['slots']['icon'] = _single(
              _node(_childId, 'flutter.widgets.Icon', {
                'icon': fixture.iconDataValueForViewTest(),
              }),
            );
          }
          final before = jsonEncode(model);
          await _pump(
            tester,
            model,
            theme: ThemeData(useMaterial3: material3),
            selectedId: _childId,
          );
          expect(tester.takeException(), isNull);
          expect(
            find.byType(IconButton),
            material3 ? findsOneWidget : findsNothing,
          );
          expect(_widget(_childId), findsOneWidget);
          expect(_diagnostics(tester), contains('preview unavailable'));
          expect(jsonEncode(model), before);
          if (material3) {
            _button(model)['slots']['icon']['child']['properties']['size'] =
                _number(19);
            await _pump(tester, model);
            expect(find.byType(Icon), findsOneWidget);
            expect(
              _diagnostics(tester),
              isNot(contains('iconSize preview unavailable')),
            );
            expect(tester.takeException(), isNull);
            _button(
              model,
            )['slots']['icon']['child']['properties'].remove('size');
            _button(model)['properties']['styleIconSize'] = _number(22.0);
            await _pump(tester, model);
            expect(find.byType(Icon), findsOneWidget);
            expect(
              _diagnostics(tester),
              isNot(contains('iconSize preview unavailable')),
            );
            expect(tester.takeException(), isNull);
          }
        },
      );
    }
    testWidgets(
      '${material3 ? "M3" : "M2"} positive infinity splashRadius raw SDK and Canvas interactions',
      (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            theme: ThemeData(useMaterial3: material3),
            home: Scaffold(
              body: Center(
                child: _raw('filled', splashRadius: double.infinity),
              ),
            ),
          ),
        );
        await tester.tap(find.byType(IconButton));
        await tester.pump(const Duration(milliseconds: 50));
        expect(
          tester.takeException(),
          material3 ? isNull : isA<UnsupportedError>(),
        );
        await _pump(
          tester,
          _model(properties: {'splashRadius': _enum('double', 'infinity')}),
          theme: ThemeData(useMaterial3: material3),
        );
        if (material3) await tester.tap(find.byType(IconButton));
        await tester.pump(const Duration(milliseconds: 50));
        expect(tester.takeException(), isNull);
        if (!material3) {
          expect(find.byType(IconButton), findsNothing);
          expect(
            _diagnostics(tester),
            contains('splashRadius preview unavailable'),
          );
          await _pump(
            tester,
            _model(
              properties: {
                'enabled': _bool(false),
                'splashRadius': _enum('double', 'infinity'),
              },
            ),
            theme: ThemeData(useMaterial3: false),
          );
          expect(find.byType(IconButton), findsOneWidget);
          expect(_sdk(tester).onPressed, isNull);
        }
      },
    );
    testWidgets(
      '${material3 ? "M3" : "M2"} callbacks activation and ignored project fields follow SDK',
      (tester) async {
        for (final enabled in [false, true]) {
          await _pump(
            tester,
            _model(
              properties: {
                'enabled': _bool(enabled),
                'onLongPress': _reference,
                'onHover': _reference,
                'focusNode': _reference,
                'mouseCursor': _reference,
                'statesController': _reference,
                'style': _reference,
              },
            ),
            theme: ThemeData(useMaterial3: material3),
          );
          expect(_sdk(tester).onPressed != null, enabled);
          expect(_sdk(tester).onLongPress != null, enabled);
          expect(_sdk(tester).onHover, isNotNull);
          expect(_sdk(tester).focusNode, isNull);
          expect(_sdk(tester).statesController, isNull);
          expect(_diagnostics(tester), contains('isolated Canvas'));
          if (!material3) {
            expect(_sdk(tester).style, isNull);
            expect(
              _diagnostics(tester),
              isNot(contains('ButtonStyle appearance')),
            );
            expect(_diagnostics(tester), isNot(contains('statesController')));
          }
          await tester.longPress(find.byType(IconButton));
          await tester.pump(const Duration(milliseconds: 200));
          expect(tester.takeException(), isNull);
        }
      },
    );
  }

  testWidgets(
    'Local compound dimensions complete from direct constraints while atomic style properties shadow direct values',
    (tester) async {
      final theme = ThemeData(
        iconButtonTheme: IconButtonThemeData(
          style: ButtonStyle(
            minimumSize: const WidgetStatePropertyAll(Size(60, 62)),
            maximumSize: const WidgetStatePropertyAll(Size(150, 155)),
            visualDensity: const VisualDensity(horizontal: 1, vertical: 2),
            foregroundColor: const WidgetStatePropertyAll(Colors.green),
          ),
        ),
      );
      final props = <String, Object?>{
        'constraints': {
          'kind': 'boxConstraints',
          'minWidth': 70.0,
          'minHeight': 72.0,
          'maxWidth': 160.0,
          'maxHeight': 165.0,
        },
        'visualDensityHorizontal': _number(-1.0),
        'styleVisualDensityVertical': _number(-2.0),
        'stylePressedMinimumWidth': _number(80.0),
        'stylePressedMaximumHeight': _number(140.0),
        'stylePressedForegroundColor': _color(0xff0000ff),
        'color': _color(0xffff0000),
      };
      await _pump(tester, _model(properties: props), theme: theme);
      final style = _sdk(tester).style!;
      expect(
        style.visualDensity,
        const VisualDensity(horizontal: -1, vertical: -2),
      );
      expect(
        style.minimumSize!.resolve({WidgetState.pressed}),
        const Size(80, 72),
      );
      expect(
        style.maximumSize!.resolve({WidgetState.pressed}),
        const Size(160, 140),
      );
      expect(style.minimumSize!.resolve({}), isNull);
      expect(style.foregroundColor!.resolve({}), isNull);
      final canvasSize = tester.getSize(find.byType(IconButton));
      final rawStyle = ButtonStyle(
        minimumSize: WidgetStateProperty.resolveWith(
          (states) =>
              states.contains(WidgetState.pressed) ? const Size(80, 72) : null,
        ),
        maximumSize: WidgetStateProperty.resolveWith(
          (states) => states.contains(WidgetState.pressed)
              ? const Size(160, 140)
              : null,
        ),
        foregroundColor: WidgetStateProperty.resolveWith(
          (states) => states.contains(WidgetState.pressed) ? Colors.blue : null,
        ),
        visualDensity: const VisualDensity(horizontal: -1, vertical: -2),
      );
      await tester.pumpWidget(
        MaterialApp(
          theme: theme,
          home: Scaffold(
            body: Center(
              child: _raw(
                'standard',
                constraints: const BoxConstraints(
                  minWidth: 70,
                  minHeight: 72,
                  maxWidth: 160,
                  maxHeight: 165,
                ),
                density: const VisualDensity(horizontal: -1, vertical: 2),
                color: Colors.red,
                style: rawStyle,
              ),
            ),
          ),
        ),
      );
      await tester.pump(const Duration(milliseconds: 300));
      expect(tester.getSize(find.byType(IconButton)), canvasSize);
      expect(tester.takeException(), isNull);
    },
  );

  test(
    'IconButton padding has the exact nonnegative physical/directional contract',
    () {
      final runtime = _section(canvasRuntimeWidgetSchemaContractForTesting());
      expect(
        runtime
            .split('\n')
            .singleWhere((line) => line.startsWith('P|padding|')),
        'P|padding|edgeInsets,edgeInsetsDirectional|0|-|'
        'edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|'
        'edgeInsets:edgeInsets:1:0:1:*:1;'
        'edgeInsetsDirectional:edgeInsets:1:0:1:*:1',
      );
      for (final directional in [false, true]) {
        final sides = directional
            ? ['start', 'top', 'end', 'bottom']
            : ['left', 'top', 'right', 'bottom'];
        final padding = <String, Object?>{
          'kind': directional ? 'edgeInsetsDirectional' : 'edgeInsets',
          for (var index = 0; index < sides.length; index++)
            sides[index]: index * 0.5,
        };
        final model = _model(properties: {'padding': padding});
        expect(() => _decode(model), returnsNormally);
        expect(
          _button(jsonDecode(jsonEncode(model)))['properties']['padding'],
          padding,
        );
        for (final side in sides) {
          expect(
            () => _decode(
              _model(
                properties: {
                  'padding': {...padding, side: -0.5},
                },
              ),
            ),
            throwsFormatException,
            reason: '${padding['kind']}.$side must be nonnegative',
          );
        }
      }
    },
  );

  test('IconButton runtime capability matches all 524 reviewed fields', () {
    final section = _section(canvasRuntimeWidgetSchemaContractForTesting());
    expect(
      section.split('\n').where((line) => line.startsWith('P|')).length,
      524,
    );
    final reviewed = _section(canvasReviewedWidgetSchemaContract).split('\n');
    final runtime = section.split('\n');
    expect(runtime.length, reviewed.length);
    for (var index = 0; index < runtime.length; index++) {
      expect(runtime[index], reviewed[index], reason: 'capability line $index');
    }
  });
  for (final material3 in [false, true]) {
    for (final variant in _variants) {
      testWidgets(
        '${material3 ? 'M3' : 'M2'} $variant nullable selection and exact raw SDK geometry',
        (tester) async {
          final theme = ThemeData(useMaterial3: material3);
          for (final selected in [null, false, true]) {
            for (final enabled in [false, true]) {
              final props = <String, Object?>{
                'enabled': _bool(enabled),
                if (selected != null) 'isSelected': _bool(selected),
              };
              await _pump(
                tester,
                _model(variant: variant, icon: true, properties: props),
                theme: theme,
              );
              final size = tester.getSize(find.byType(IconButton));
              final shown = material3 && selected == true ? 'I' : 'Action';
              expect(find.text(shown), findsOneWidget);
              expect(find.text(shown == 'I' ? 'Action' : 'I'), findsNothing);
              expect(_sdk(tester).onPressed != null, enabled);
              if (material3) {
                expect(
                  _m3(
                    tester,
                  ).statesController!.value.contains(WidgetState.selected),
                  selected == true,
                );
              } else {
                expect(
                  find.byWidgetPredicate((w) => w is ButtonStyleButton),
                  findsNothing,
                );
              }
              await tester.pumpWidget(
                MaterialApp(
                  theme: theme,
                  home: Scaffold(
                    body: Center(
                      child: _raw(
                        variant,
                        selectedIcon: const Text('I'),
                        selected: selected,
                        enabled: enabled,
                      ),
                    ),
                  ),
                ),
              );
              await tester.pump(const Duration(milliseconds: 300));
              expect(tester.getSize(find.byType(IconButton)), size);
              expect(find.text(shown), findsOneWidget);
              expect(tester.takeException(), isNull);
            }
          }
        },
      );
    }
  }
  test(
    'Required icon is the reviewed wrapper slot; selectedIcon is optional',
    () {
      expect(canvasReviewedRequiredWrapperSlot(_type), 'icon');
      expect(
        canvasReviewedRequiredWrapperSlot('flutter.material.TextButton'),
        'child',
      );
      expect(
        canvasDropSlotsForWidgetType(_type).single.slotName,
        'selectedIcon',
      );
      expect(
        canvasExistingChildWrapTargetSlot(
          parentWidgetType: _type,
          slotName: 'icon',
        ),
        isNotNull,
      );
      final data = _model();
      _button(data)['slots'] = {'icon': _single(null)};
      expect(() => _decode(data), throwsFormatException);
    },
  );
  for (final size in [-1.0, double.infinity]) {
    testWidgets(
      'Raw SDK M3 accepts ignored iconSize $size for a non-Icon child',
      (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: Center(child: _raw('standard', iconSize: size)),
            ),
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
      },
    );
  }
}
