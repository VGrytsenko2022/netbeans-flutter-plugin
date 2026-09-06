import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.FloatingActionButton';
const _id = 'f1dd3aa1-fc2f-45b7-a9fc-0c8b5e1ac511';
const _childId = '7c37c659-01b5-46e5-852f-11abdb77504c';
const _iconId = 'ca8c98a2-0aa1-4e41-bd02-8c123ecdbdad';
const _reference = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
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
      'child': _single(
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
        'icon': _single(
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
FloatingActionButton _sdk(WidgetTester tester) =>
    tester.widget<FloatingActionButton>(find.byType(FloatingActionButton));
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
  void Function(CanvasDropResolver?)? onDrop,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: CanvasDocumentView(
        model: _decode(model),
        selectedWidgetId: selectedId,
        onSelected: selected?.add ?? (_) {},
        onDropResolverChanged: onDrop,
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
  await tester.pump();
}

String _section(String contract) => contract.substring(
  contract.indexOf('W|$_type\n'),
  contract.indexOf('W|flutter.material.LinearProgressIndicator\n'),
);
const _shapes = [
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
const _radiusKinds = {
  'roundedRectangle',
  'beveledRectangle',
  'continuousRectangle',
  'roundedSuperellipse',
};
const _commonSide = {
  'shapeSideColor',
  'shapeSideWidth',
  'shapeSideStyle',
  'shapeSideStrokeAlign',
};
const _starCommon = {
  'shapePoints',
  'shapePointRounding',
  'shapeRotation',
  'shapeSquash',
};
const _starOnly = {'shapeInnerRadiusRatio', 'shapeValleyRounding'};
final _edgeNames = {
  for (final edge in ['Start', 'End', 'Top', 'Bottom']) ...[
    'shape${edge}Size',
    'shape${edge}Alignment',
  ],
};

Map<String, Object?> _allShapeDetails() => {
  'shapeRadius': _radius(),
  'shapeSideColor': _color(0x80123456),
  'shapeSideWidth': _double(3.5),
  'shapeSideStyle': _enum('BorderStyle', 'solid'),
  'shapeSideStrokeAlign': _double(.25),
  'shapeCircleEccentricity': _double(.25),
  'shapePoints': _double(5.5),
  'shapeInnerRadiusRatio': _double(.6),
  'shapePointRounding': _double(.2),
  'shapeValleyRounding': _double(.3),
  'shapeRotation': _double(-27.5),
  'shapeSquash': _double(.75),
  for (final edge in ['Start', 'End', 'Top', 'Bottom']) ...{
    'shape${edge}Size': _double(.5),
    'shape${edge}Alignment': _double(-.75),
  },
};
Set<String> _allowed(String kind) => {
  ..._commonSide,
  if (_radiusKinds.contains(kind)) 'shapeRadius',
  if (kind == 'circle' || kind == 'oval') 'shapeCircleEccentricity',
  if (kind == 'star' || kind == 'polygon') ..._starCommon,
  if (kind == 'star') ..._starOnly,
  if (kind == 'linear') ..._edgeNames,
};
Type _shapeType(String kind) => switch (kind) {
  'roundedRectangle' => RoundedRectangleBorder,
  'beveledRectangle' => BeveledRectangleBorder,
  'continuousRectangle' => ContinuousRectangleBorder,
  'roundedSuperellipse' => RoundedSuperellipseBorder,
  'circle' => CircleBorder,
  'oval' => OvalBorder,
  'stadium' => StadiumBorder,
  'linear' => LinearBorder,
  'star' || 'polygon' => StarBorder,
  _ => throw StateError(kind),
};
Map<String, Object?> _radius({bool directional = false}) => {
  'kind': 'borderRadius',
  'geometry': {
    'kind': directional ? 'directional' : 'physical',
    directional ? 'topStart' : 'topLeft': {'x': 5, 'y': 6},
    directional ? 'topEnd' : 'topRight': {'x': 7, 'y': 8},
    directional ? 'bottomEnd' : 'bottomRight': {'x': 9, 'y': 10},
    directional ? 'bottomStart' : 'bottomLeft': {'x': 11, 'y': 12},
  },
};
Map<String, Object?> _textStyles() => {
  'extendedTextStyleThemeTextStyle': _theme('material.textTheme.bodyLarge'),
  'extendedTextStyleInherit': _bool(false),
  'extendedTextStyleColor': _color(0xFF123456),
  'extendedTextStyleBackgroundColor': _color(0x44123456),
  'extendedTextStyleFontSize': _double(18.5),
  'extendedTextStyleFontWeight': _enum('FontWeight', 'w600'),
  'extendedTextStyleFontStyle': _enum('FontStyle', 'italic'),
  'extendedTextStyleLetterSpacing': _double(1.25),
  'extendedTextStyleWordSpacing': _double(2.5),
  'extendedTextStyleTextBaseline': _enum('TextBaseline', 'ideographic'),
  'extendedTextStyleHeight': _double(1.4),
  'extendedTextStyleLeadingDistribution': _enum(
    'TextLeadingDistribution',
    'even',
  ),
  'extendedTextStyleLocaleLanguageCode': _string('en'),
  'extendedTextStyleLocaleScriptCode': _string('Latn'),
  'extendedTextStyleLocaleCountryCode': _string('GB'),
  'extendedTextStyleShadows': {
    'kind': 'shadowList',
    'items': [
      {
        'id': '192489fb-3bbb-46c5-9bac-c988f412218c',
        'color': {'kind': 'theme', 'token': 'material.colorScheme.shadow'},
        'offsetX': -1.25,
        'offsetY': 2.5,
        'blurRadius': 4.0,
      },
    ],
  },
  'extendedTextStyleFontFeatures': {
    'kind': 'fontFeatureList',
    'items': [
      {'id': 'd8ca6ff9-1aa5-4bb1-944b-fdd475b5359d', 'tag': 'liga', 'value': 1},
    ],
  },
  'extendedTextStyleFontVariations': {
    'kind': 'fontVariationList',
    'items': [
      {
        'id': '2115406c-c05d-4323-81a2-7cfe7ea35dc4',
        'axis': 'wght',
        'value': 700.0,
      },
    ],
  },
  'extendedTextStyleDecorationUnderline': _bool(true),
  'extendedTextStyleDecorationOverline': _bool(true),
  'extendedTextStyleDecorationLineThrough': _bool(true),
  'extendedTextStyleDecorationColor': _theme('material.colorScheme.error'),
  'extendedTextStyleDecorationStyle': _enum('TextDecorationStyle', 'wavy'),
  'extendedTextStyleDecorationThickness': _double(2.25),
  'extendedTextStyleDebugLabel': _string('badge style'),
  'extendedTextStyleFontFamily': _string('Inter'),
  'extendedTextStyleFontFamilyFallback': _string('Noto Sans\nNoto Color Emoji'),
  'extendedTextStylePackage': _string('design_fonts'),
  'extendedTextStyleOverflow': _enum('TextOverflow', 'fade'),
};
Map<String, Object?> _double(double value) => _number(value);
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};

const _variants = ['standard', 'small', 'large', 'extended'];
Map<String, Object?> _padding() => {
  'kind': 'edgeInsetsDirectional',
  'start': 3.0,
  'top': 2.0,
  'end': 5.0,
  'bottom': 4.0,
};
RawMaterialButton _rawMaterial(WidgetTester tester) =>
    tester.widget<RawMaterialButton>(
      find.descendant(
        of: find.byType(FloatingActionButton),
        matching: find.byType(RawMaterialButton),
      ),
    );
FloatingActionButton _raw(
  String variant, {
  Widget? child = const Text('Action'),
  Widget? icon,
  Object? tag,
  bool enabled = true,
}) => switch (variant) {
  'small' => FloatingActionButton.small(
    onPressed: enabled ? () {} : null,
    heroTag: tag,
    child: child,
  ),
  'large' => FloatingActionButton.large(
    onPressed: enabled ? () {} : null,
    heroTag: tag,
    child: child,
  ),
  'extended' => FloatingActionButton.extended(
    onPressed: enabled ? () {} : null,
    heroTag: tag,
    icon: icon,
    label: child!,
  ),
  _ => FloatingActionButton(
    onPressed: enabled ? () {} : null,
    heroTag: tag,
    child: child,
  ),
};
Map<String, Object?> _common() => {
  'tooltip': _string('Action tooltip'),
  for (final name in [
    'foregroundColor',
    'backgroundColor',
    'focusColor',
    'hoverColor',
    'splashColor',
  ])
    name: _color(0xff123456),
  for (final name in [
    'elevation',
    'focusElevation',
    'hoverElevation',
    'highlightElevation',
    'disabledElevation',
  ])
    name: _number(3.5),
  'heroTag': _null,
  'onPressed': _reference,
  'focusNode': _reference,
  'mouseCursor': _string('click'),
  'autofocus': _bool(false),
  'enableFeedback': _bool(false),
  'clipBehavior': _enum('Clip', 'antiAlias'),
  'materialTapTargetSize': _enum('MaterialTapTargetSize', 'shrinkWrap'),
};

Map<String, Object?> _paint() => {
  'kind': 'paint',
  'color': {'kind': 'literal', 'argb': '0xFF123456'},
  'blendMode': 'srcOver',
  'style': 'fill',
  'strokeWidth': 0.0,
  'strokeCap': 'round',
  'strokeJoin': 'bevel',
  'strokeMiterLimit': 4.0,
  'antiAlias': true,
  'filterQuality': 'medium',
  'invertColors': false,
};

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
  for (final variant in _variants) {
    testWidgets(
      '$variant finite to all-infinite elevation history preserves child editing state and selection',
      (tester) async {
        State? state;
        for (final infinite in [false, true, false]) {
          final data = _model(
            variant: variant,
            properties: {
              'heroTag': _null,
              for (final name in [
                'elevation',
                'focusElevation',
                'hoverElevation',
                'highlightElevation',
                'disabledElevation',
              ])
                name: infinite ? _enum('double', 'infinity') : _number(2),
            },
          );
          (_button(data)['slots'] as Map)['child'] = _single(
            _node(_childId, 'flutter.material.TextField', {}),
          );
          await _pump(tester, data, selectedId: _childId);
          state ??= tester.state(find.byType(TextField));
          expect(tester.state(find.byType(TextField)), same(state));
          expect(
            tester
                .widget<CanvasDocumentView>(find.byType(CanvasDocumentView))
                .selectedWidgetId,
            _childId,
          );
          final edit = tester.state<EditableTextState>(
            find.byType(EditableText),
          );
          if (!infinite && edit.widget.controller.text.isEmpty) {
            edit.widget.controller.text = 'retained';
          }
          expect(edit.widget.controller.text, 'retained');
          expect(
            _rawMaterial(tester).elevation,
            infinite ? double.infinity : 2,
          );
          expect(tester.takeException(), isNull);
        }
      },
    );
  }
  testWidgets(
    'mixed local and theme elevation infinities are explicit Canvas limitations',
    (tester) async {
      await _pump(
        tester,
        _model(properties: {'hoverElevation': _enum('double', 'infinity')}),
      );
      expect(find.byType(FloatingActionButton), findsNothing);
      expect(_diagnostics(tester), contains('mix finite and infinite'));
      expect(find.text('Action'), findsOneWidget);
      await _pump(
        tester,
        _model(),
        theme: ThemeData(
          floatingActionButtonTheme: const FloatingActionButtonThemeData(
            hoverElevation: double.infinity,
          ),
        ),
      );
      expect(find.byType(FloatingActionButton), findsNothing);
      expect(_diagnostics(tester), contains('NaN'));
      expect(tester.takeException(), isNull);
    },
  );
  test('heroTag integers retain the existing portable exact boundary', () {
    for (final n in [9007199254740991, -9007199254740991]) {
      final model = _decode(_model(properties: {'heroTag': _number(n)}));
      expect(
        model.root
            .slot('body')!
            .child!
            .slot('child')!
            .child!
            .properties['heroTag']!
            .value,
        n,
      );
    }
    for (final n in [
      9007199254740992,
      -9007199254740992,
      9223372036854775807,
    ]) {
      expect(
        () => _decode(_model(properties: {'heroTag': _number(n)})),
        throwsFormatException,
      );
    }
  });
  for (final m3 in [false, true]) {
    testWidgets(
      'M${m3 ? 3 : 2} FAB all four variants exact SDK pixels and RTL geometry',
      (tester) async {
        for (final variant in _variants) {
          for (final rtl in [false, true]) {
            for (final enabled in [false, true]) {
              final theme = ThemeData(
                useMaterial3: m3,
                floatingActionButtonTheme: const FloatingActionButtonThemeData(
                  backgroundColor: Color(0xff468aca),
                  foregroundColor: Colors.white,
                ),
              );
              final data = _model(
                variant: variant,
                icon: variant == 'extended',
                properties: {'heroTag': _null, 'enabled': _bool(enabled)},
              );
              final button = _button(data);
              const boundaryId = '0b8f3c97-04f1-4bfe-a5d1-c2080c1b2044';
              (data['root']
                  as Map)['slots']['body']['child']['slots']['child'] = _single(
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
              if (variant == 'extended') {
                expect(
                  tester.getTopLeft(find.text('I')).dx <
                      tester.getTopLeft(find.text('Action')).dx,
                  !rtl,
                );
              }
              for (final id in [
                _id,
                _childId,
                if (variant == 'extended') _iconId,
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
                    textDirection: rtl ? TextDirection.rtl : TextDirection.ltr,
                    child: Scaffold(
                      body: Center(
                        child: RepaintBoundary(
                          key: rawKey,
                          child: _raw(
                            variant,
                            enabled: enabled,
                            icon: variant == 'extended'
                                ? const Text('I')
                                : null,
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
                raw.$1,
                canvas.$1,
                reason: '$variant RTL$rtl enabled$enabled',
              );
              expect(
                raw.$2,
                canvas.$2,
                reason: '$variant RTL$rtl enabled$enabled pixels',
              );
              // Canvas Text has its own editing-node extent. Compare exact
              // glyph paint and button bounds, not that editor wrapper's bounds.
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );
  }
  testWidgets(
    'FAB all 31 extended TextStyle leaves include exact Paint and inheritance branches',
    (tester) async {
      final properties = _textStyles()
        ..remove('extendedTextStyleColor')
        ..remove('extendedTextStyleBackgroundColor');
      properties.addAll({
        'extendedTextStyleForeground': _paint(),
        'extendedTextStyleBackground': _paint(),
      });
      expect(properties, hasLength(29));
      await _pump(tester, _model(variant: 'extended', properties: properties));
      final style = _sdk(tester).extendedTextStyle!;
      expect(style.foreground!.color.toARGB32(), 0xff123456);
      expect(style.background!.color.toARGB32(), 0xff123456);
      expect(style.inherit, false);
      expect(
        style.decoration,
        TextDecoration.combine([
          TextDecoration.underline,
          TextDecoration.overline,
          TextDecoration.lineThrough,
        ]),
      );
      expect(
        style.locale,
        const Locale.fromSubtags(
          languageCode: 'en',
          scriptCode: 'Latn',
          countryCode: 'GB',
        ),
      );
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'FAB active padding overflow is diagnosed and stored values remain exact',
    (tester) async {
      for (final props in [
        {
          'extendedPadding': {
            'kind': 'edgeInsets',
            'left': 1e308,
            'right': 1e308,
            'top': 0.0,
            'bottom': 0.0,
          },
        },
        {
          'extendedPadding': {
            'kind': 'edgeInsetsDirectional',
            'start': 1e308,
            'end': 0.0,
            'top': 0.0,
            'bottom': 0.0,
          },
          'extendedIconLabelSpacing': _number(1e308),
        },
      ]) {
        final data = _model(variant: 'extended', icon: true, properties: props);
        await _pump(tester, data);
        expect(find.byType(FloatingActionButton), findsNothing);
        expect(
          _diagnostics(tester),
          contains('extendedPadding preview unavailable'),
        );
        expect(tester.takeException(), isNull);
        expect(
          _button(data)['properties'],
          containsPair('extendedPadding', props['extendedPadding']),
        );
      }
    },
  );
  test(
    'FAB exact 78 scalar contract and nullable child / extended label constraints',
    () {
      final contract = _section(canvasRuntimeWidgetSchemaContractForTesting());
      expect(contract, _section(canvasReviewedWidgetSchemaContract));
      expect(
        contract.split('\n').where((line) => line.startsWith('P|')),
        hasLength(78),
      );
      expect(
        contract,
        contains('S|child|single|1|0|1|any\nS|icon|single|0|0|1|any'),
      );
      expect(contract, isNot(contains('C|$_type|')));
      for (final variant in _variants) {
        final data = _model(variant: variant);
        expect(
          _decode(
            data,
          ).root.slot('body')!.child!.slot('child')!.child!.properties,
          hasLength(2),
        );
        (_button(data)['slots'] as Map)['child'] = _single(null);
        expect(
          () => _decode(data),
          variant == 'extended' ? throwsFormatException : returnsNormally,
        );
        (_button(data)['slots'] as Map).remove('child');
        expect(() => _decode(data), throwsFormatException);
        expect(
          () => _decode(_model(variant: variant, icon: true)),
          variant == 'extended' ? returnsNormally : throwsFormatException,
        );
        for (final name in ['enabled', 'variant']) {
          final missing = _model(variant: variant);
          (_button(missing)['properties'] as Map).remove(name);
          expect(() => _decode(missing), throwsFormatException);
        }
        for (final entry in {
          'mini': _bool(false),
          'isExtended': _bool(false),
          'extendedIconLabelSpacing': _number(-2),
          'extendedPadding': _padding(),
          'extendedTextStyleColor': _color(0xff00ff00),
        }.entries) {
          final allowed = entry.key == 'mini'
              ? variant == 'standard'
              : entry.key == 'isExtended'
              ? variant == 'standard' || variant == 'extended'
              : variant == 'extended';
          expect(
            () => _decode(
              _model(variant: variant, properties: {entry.key: entry.value}),
            ),
            allowed ? returnsNormally : throwsFormatException,
            reason: '$variant/${entry.key}',
          );
        }
      }
    },
  );
  test(
    'FAB all Hero literal kinds and strict project refs remain model values',
    () {
      for (final value in [
        _null,
        _string('tag'),
        _string(''),
        _number(12),
        _number(12.25),
        _bool(true),
        _bool(false),
        _reference,
      ]) {
        expect(
          () => _decode(_model(properties: {'heroTag': value})),
          returnsNormally,
        );
      }
      for (final name in [
        'heroTag',
        'shape',
        'onPressed',
        'focusNode',
        'mouseCursor',
      ]) {
        expect(
          () => _decode(
            _model(
              properties: {
                name: {..._reference, 'expression': 'runProjectCode()'},
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final name in [
        'elevation',
        'focusElevation',
        'hoverElevation',
        'highlightElevation',
        'disabledElevation',
      ]) {
        for (final value in [
          _number(0),
          _number(1e308),
          _enum('double', 'infinity'),
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            returnsNormally,
          );
        }
        expect(
          () => _decode(_model(properties: {name: _number(-.1)})),
          throwsFormatException,
        );
      }
    },
  );
  test('all ten FAB shape families retain strict dependencies', () {
    for (final kind in _shapes) {
      final allowed = _allowed(kind);
      for (final e in _allShapeDetails().entries) {
        expect(
          () => _decode(
            _model(properties: {'shapeKind': _string(kind), e.key: e.value}),
          ),
          allowed.contains(e.key) ? returnsNormally : throwsFormatException,
          reason: '$kind/${e.key}',
        );
      }
    }
    expect(
      () => _decode(
        _model(
          properties: {'shape': _reference, 'shapeKind': _string('circle')},
        ),
      ),
      throwsFormatException,
    );
    expect(
      () => _decode(
        _model(
          properties: {
            'shapeKind': _string('star'),
            'shapePointRounding': _number(.8),
            'shapeValleyRounding': _number(.8),
          },
        ),
      ),
      throwsFormatException,
    );
  });
  for (final platform in ['windows', 'web']) {
    for (final m3 in [false, true]) {
      for (final variant in _variants) {
        testWidgets(
          '$platform M${m3 ? 3 : 2} $variant actual SDK geometry and theme precedence',
          (tester) async {
            for (final custom in [false, true]) {
              final theme = ThemeData(
                useMaterial3: m3,
                floatingActionButtonTheme: custom
                    ? const FloatingActionButtonThemeData(
                        foregroundColor: Colors.red,
                        backgroundColor: Colors.yellow,
                        focusColor: Colors.blue,
                        hoverColor: Colors.green,
                        splashColor: Colors.pink,
                        elevation: 1,
                        focusElevation: 2,
                        hoverElevation: 3,
                        highlightElevation: 4,
                        disabledElevation: 5,
                        sizeConstraints: BoxConstraints.tightFor(
                          width: 60,
                          height: 60,
                        ),
                        smallSizeConstraints: BoxConstraints.tightFor(
                          width: 42,
                          height: 42,
                        ),
                        largeSizeConstraints: BoxConstraints.tightFor(
                          width: 88,
                          height: 88,
                        ),
                        extendedSizeConstraints: BoxConstraints(
                          minWidth: 110,
                          maxHeight: 54,
                          minHeight: 54,
                        ),
                      )
                    : const FloatingActionButtonThemeData(),
              );
              for (final scale in [1.0, 2.0]) {
                final data = _model(
                  variant: variant,
                  platform: platform,
                  scale: scale,
                  icon: variant == 'extended',
                  properties: {'heroTag': _null},
                );
                await _pump(tester, data, theme: theme);
                final size = tester.getSize(find.byType(FloatingActionButton));
                final raw = _rawMaterial(tester);
                final color = raw.fillColor;
                final shape = raw.shape;
                expect(_sdk(tester).clipBehavior, Clip.none);
                await tester.pumpWidget(
                  MaterialApp(
                    theme: theme,
                    home: MediaQuery(
                      data: MediaQueryData(
                        textScaler: TextScaler.linear(scale),
                      ),
                      child: Scaffold(
                        body: Center(
                          child: _raw(
                            variant,
                            icon: variant == 'extended'
                                ? const Text('I')
                                : null,
                          ),
                        ),
                      ),
                    ),
                  ),
                );
                await tester.pump(const Duration(milliseconds: 300));
                expect(tester.getSize(find.byType(FloatingActionButton)), size);
                expect(_rawMaterial(tester).fillColor, color);
                expect(_rawMaterial(tester).shape, shape);
                expect(tester.takeException(), isNull);
              }
            }
          },
        );
      }
    }
  }
  testWidgets(
    'all direct fields render and local properties beat the FAB theme',
    (tester) async {
      for (final variant in _variants) {
        final properties = {
          ..._common(),
          if (variant == 'standard') ...{
            'mini': _bool(true),
            'isExtended': _bool(true),
          },
          if (variant == 'extended') ...{
            'isExtended': _bool(false),
            'extendedIconLabelSpacing': _number(-2),
            'extendedPadding': _padding(),
            ..._textStyles(),
          },
        };
        await _pump(
          tester,
          _model(
            variant: variant,
            properties: properties,
            icon: variant == 'extended',
          ),
          theme: ThemeData(
            floatingActionButtonTheme: const FloatingActionButtonThemeData(
              backgroundColor: Colors.orange,
              elevation: 8,
            ),
          ),
        );
        final sdk = _sdk(tester);
        final raw = _rawMaterial(tester);
        expect(sdk.tooltip, 'Action tooltip');
        expect(sdk.backgroundColor, const Color(0xff123456));
        expect(raw.fillColor, const Color(0xff123456));
        expect(raw.elevation, 3.5);
        expect(raw.focusElevation, 3.5);
        expect(raw.hoverElevation, 3.5);
        expect(raw.highlightElevation, 3.5);
        expect(raw.disabledElevation, 3.5);
        expect(raw.enableFeedback, false);
        expect(raw.materialTapTargetSize, MaterialTapTargetSize.shrinkWrap);
        expect(raw.clipBehavior, Clip.antiAlias);
        expect(sdk.focusNode, isNull);
        expect(sdk.heroTag, isNull);
        expect(_diagnostics(tester), contains('Project callback'));
        if (variant == 'extended') {
          expect(sdk.extendedTextStyle!.fontSize, 18.5);
          expect(sdk.extendedTextStyle!.fontFeatures, hasLength(1));
          expect(sdk.extendedTextStyle!.fontVariations, hasLength(1));
          expect(sdk.extendedTextStyle!.shadows, hasLength(1));
          expect(sdk.extendedIconLabelSpacing, -2);
        }
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'all FAB built-in shapes render and large/project shapes are explicit approximations',
    (tester) async {
      for (final kind in _shapes) {
        final props = {
          'shapeKind': _string(kind),
          for (final e in _allShapeDetails().entries)
            if (_allowed(kind).contains(e.key)) e.key: e.value,
        };
        await _pump(tester, _model(properties: props));
        expect(_sdk(tester).shape.runtimeType, _shapeType(kind));
        expect(tester.takeException(), isNull);
      }
      for (final props in [
        {'shape': _reference},
        {'shapeKind': _string('star'), 'shapePoints': _number(100000)},
      ]) {
        await _pump(tester, _model(properties: props));
        expect(_sdk(tester).shape, isNull);
        expect(_diagnostics(tester), contains('approximation'));
        expect(find.text('Action'), findsOneWidget);
      }
    },
  );
  testWidgets(
    'Hero defaults literals null unresolved references and duplicate tags are preserved honestly',
    (tester) async {
      final defaultTag = const FloatingActionButton(onPressed: null).heroTag;
      await _pump(tester, _model());
      expect(_sdk(tester).heroTag, defaultTag);
      expect(find.byType(Hero), findsOneWidget);
      for (final entry in {
        'heroTag': _reference,
        'mouseCursor': _reference,
        'focusNode': _reference,
        'onPressed': _reference,
      }.entries) {
        await _pump(tester, _model(properties: {entry.key: entry.value}));
        expect(find.byType(FloatingActionButton), findsOneWidget);
        expect(_diagnostics(tester), contains(entry.key));
        expect(
          find.byType(Hero),
          entry.key == 'heroTag' ? findsNothing : findsOneWidget,
        );
      }
      for (final tag in [
        _null,
        _string('tag'),
        _string(''),
        _string(' '),
        _string('first\r\nsecond\tthird'),
        _number(1),
        _number(1.0),
        _number(9007199254740991),
        _bool(false),
      ]) {
        await _pump(tester, _model(properties: {'heroTag': tag}));
        expect(_sdk(tester).heroTag, tag['value']);
      }
      final data = _model();
      final first = _button(data);
      final second = jsonDecode(jsonEncode(first)) as Map;
      second['id'] = 'fa616b17-e5ea-4c23-bf8a-a530322e1601';
      second['slots'] = {'child': _single(null)};
      (data['root']
          as Map)['slots']['body']['child']['slots']['child'] = _single(
        _node(
          'fd913c68-4129-425f-a188-30b215c65a35',
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
      await _pump(tester, data);
      expect(find.byType(Hero), findsNWidgets(2));
      expect(_diagnostics(tester), contains('SDK default Hero tag'));
      expect(tester.takeException(), isNull);
      first['properties']['heroTag'] = _number(1);
      second['properties']['heroTag'] = _number(1.0);
      await _pump(tester, data);
      expect(_diagnostics(tester), contains('equal literal Hero tag'));
      for (final tags in [
        (_bool(true), _number(1)),
        (_string(''), _string(' ')),
        (_string('a\nb'), _string('a\r\nb')),
        (_number(9007199254740991), _number(9007199254740990)),
      ]) {
        first['properties']['heroTag'] = tags.$1;
        second['properties']['heroTag'] = tags.$2;
        await _pump(tester, data);
        expect(find.byType(Hero), findsNWidgets(2));
        expect(_diagnostics(tester), isNot(contains('equal literal Hero tag')));
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'spacing is guarded only while extended icon and label are mounted',
    (tester) async {
      for (final spacing in [-2.0, double.infinity]) {
        final value = spacing.isInfinite
            ? _enum('double', 'infinity')
            : _number(spacing);
        for (final icon in [false, true]) {
          for (final isExtended in [false, true]) {
            await _pump(
              tester,
              _model(
                variant: 'extended',
                icon: icon,
                properties: {
                  'extendedIconLabelSpacing': value,
                  'isExtended': _bool(isExtended),
                },
              ),
            );
            expect(
              find.byType(FloatingActionButton),
              icon && isExtended ? findsNothing : findsOneWidget,
            );
            if (icon && isExtended) {
              expect(
                _diagnostics(tester),
                contains('invalid SDK SizedBox constraints'),
              );
            }
            expect(tester.takeException(), isNull);
          }
        }
      }
    },
  );
  testWidgets(
    'nullable children keep SDK size and both exact insertion destinations',
    (tester) async {
      for (final variant in _variants) {
        final data = _model(variant: variant);
        if (variant != 'extended') {
          (_button(data)['slots'] as Map)['child'] = _single(null);
        }
        CanvasDropResolver? resolver;
        await _pump(tester, data, onDrop: (r) => resolver = r);
        if (variant != 'extended') expect(_sdk(tester).child, isNull);
        final point = tester.getCenter(find.byType(FloatingActionButton));
        final surface = tester.getRect(
          find.byKey(const ValueKey('canvas-interaction-surface')),
        );
        final target = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(
            token: 'text',
            widgetType: 'flutter.widgets.Text',
            traits: {},
          ),
        );
        expect(target?.parentWidgetId, _id);
        expect(target?.slotName, variant == 'extended' ? 'icon' : 'child');
      }
      expect(isCanvasRequiredChildWrapperWidgetType(_type), false);
    },
  );
  testWidgets(
    'FAB variants and references preserve the live child state and local interactions',
    (tester) async {
      State? childState;
      for (final variant in [
        'standard',
        'small',
        'large',
        'extended',
        'standard',
      ]) {
        final data = _model(
          variant: variant,
          icon: variant == 'extended',
          properties: {
            'heroTag': _null,
            'onPressed': _reference,
            'focusNode': _reference,
          },
        );
        (_button(data)['slots'] as Map)['child'] = _single(
          _node(_childId, 'flutter.material.TextField', {}),
        );
        await _pump(tester, data);
        childState ??= tester.state(find.byType(TextField));
        expect(tester.state(find.byType(TextField)), same(childState));
        _sdk(tester).onPressed!();
        expect(tester.takeException(), isNull);
      }
    },
  );
}
