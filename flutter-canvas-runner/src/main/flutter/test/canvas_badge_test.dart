import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Badge';
const _id = 'ba5e64c0-3712-45c3-9e8e-ae0cd79c4561';
const _childId = 'c36d9b31-1439-447c-bac1-b86911242036';
const _labelId = 'd649793d-e941-43cb-8052-030ae144ab19';
const _rowId = 'a1989523-4708-455e-9556-3bb997ae9c4f';

void main() {
  test(
    'Badge has exactly 41 optional fields, two ordinary slots, and no creation defaults',
    () {
      final runtime = canvasRuntimeWidgetSchemaContractForTesting();
      final block = runtime.substring(
        runtime.indexOf('W|$_type\n'),
        runtime.indexOf('W|flutter.material.BottomAppBar\n'),
      );
      final rows = block
          .split('\n')
          .where((line) => line.startsWith('P|'))
          .toList();
      expect(rows, hasLength(41));
      expect(
        rows.where((line) => line.startsWith('P|textStyle')),
        hasLength(31),
      );
      for (final row in rows) {
        expect(row.split('|').sublist(3, 5), ['0', '-'], reason: row);
      }
      expect(block, contains('S|child|single|0|0|1|any'));
      expect(block, contains('S|label|single|0|0|1|any'));
      final reviewed = canvasReviewedWidgetSchemaContract.trimLeft().split(
        '\n',
      );
      final actual = runtime.split('\n');
      expect(reviewed.length, actual.length);
      for (var index = 0; index < actual.length; index++) {
        expect(reviewed[index], actual[index], reason: 'contract line $index');
      }
      expect(_node(_decode(_model())).properties, isEmpty);
      expect(_decode(_model()).imageResourceIds, isEmpty);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), false);
      expect(canvasDropSlotsForWidgetType(_type).map((slot) => slot.slotName), [
        'label',
        'child',
      ]);
      expect(canvasModelProtocolVersion, 20);
    },
  );

  test(
    'Badge strict envelopes reject every null and wrong-kind optional property',
    () {
      final valid = _allProperties();
      expect(
        valid,
        hasLength(39),
        reason: 'two paint alternatives are separately exercised',
      );
      for (final name in [
        ...valid.keys,
        'textStyleForeground',
        'textStyleBackground',
      ]) {
        for (final value in <Object?>[
          null,
          {'kind': 'null'},
          {'kind': 'unreviewed', 'value': true},
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
            reason: '$name/$value',
          );
        }
      }
      for (final name in [
        'key',
        'variant',
        'label',
        'child',
        'semanticLabel',
        'textStyle',
        'unknown',
      ]) {
        expect(
          () => _decode(_model(properties: {name: _string('bad')})),
          throwsFormatException,
        );
      }
      final extra = _model(
        properties: {
          'count': {'kind': 'integer', 'value': 1, 'extra': 1},
        },
      );
      expect(() => _decode(extra), throwsFormatException);
    },
  );

  test(
    'Badge count presence and maxCount validate all legal limits and reject fractional or negative counts',
    () {
      for (final count in [0, 1, 998, 999, 1000, 9007199254740991]) {
        for (final max in <int?>[null, 1, 999, 9007199254740991]) {
          final properties = {
            'count': _int(count),
            if (max != null) 'maxCount': _int(max),
          };
          expect(
            _node(_decode(_model(properties: properties))).properties.length,
            properties.length,
          );
        }
      }
      for (final name in ['count', 'maxCount']) {
        for (final value in [
          _int(-1),
          _int(9007199254740992),
          _double(1),
          _bool(true),
          _string('1'),
        ]) {
          expect(
            () => _decode(_model(properties: {'count': _int(0), name: value})),
            throwsFormatException,
          );
        }
      }
      expect(
        () => _decode(
          _model(properties: {'count': _int(0), 'maxCount': _int(0)}),
        ),
        throwsFormatException,
      );
      expect(
        () => _decode(_model(properties: {'maxCount': _int(999)})),
        throwsFormatException,
      );
    },
  );

  test(
    'Badge count and label are mutually exclusive without deleting stored label even when hidden',
    () {
      for (final visible in <bool?>[null, false, true]) {
        for (final count in <int?>[null, 0, 12]) {
          final properties = {
            if (visible != null) 'isLabelVisible': _bool(visible),
            if (count != null) 'count': _int(count),
          };
          for (final label in [null, _text(label: true)]) {
            final model = _model(
              properties: properties,
              label: label,
              child: _box(),
            );
            expect(
              () => _decode(model),
              count != null && label != null
                  ? throwsFormatException
                  : returnsNormally,
            );
            expect(
              ((_leaf(model)['slots'] as Map)['label'] as Map)['child'],
              label,
            );
          }
        }
      }
    },
  );

  test(
    'Badge optional slots accept absent empty and ordinary children but reject wrong slots cardinality and flex-only children',
    () {
      final model = _model();
      _leaf(model)['slots'] = <String, Object?>{};
      expect(() => _decode(model), returnsNormally);
      for (final name in ['label', 'child']) {
        for (final invalid in [
          {'kind': 'list', 'children': []},
          {'kind': 'single', 'child': null, 'extra': true},
          {
            'kind': 'single',
            'child': {
              'id': _childId,
              'type': 'flutter.widgets.Spacer',
              'properties': <String, Object?>{},
              'slots': <String, Object?>{},
            },
          },
        ]) {
          _leaf(model)['slots'] = {name: invalid};
          expect(
            () => _decode(model),
            throwsFormatException,
            reason: '$name/$invalid',
          );
        }
      }
      for (final name in ['body', 'children', 'replacement']) {
        _leaf(model)['slots'] = {
          name: {'kind': 'single', 'child': null},
        };
        expect(() => _decode(model), throwsFormatException);
      }
      expect(
        () => _decode(_model(label: _text(label: true), child: _box())),
        returnsNormally,
      );
    },
  );

  test(
    'Badge accepts all 729 optional direct scalar states without materializing defaults',
    () {
      for (var combination = 0; combination < 729; combination++) {
        var digits = combination;
        final properties = <String, Object?>{};
        final choices = <String, List<Map<String, Object?>>>{
          'smallSize': [_int(0), _double(7.5)],
          'largeSize': [_int(0), _double(21.5)],
          'backgroundColor': [
            _color('0x80112233'),
            _theme('material.colorScheme.error'),
          ],
          'textColor': [
            _color('0xFFEEDDCC'),
            _theme('material.colorScheme.onError'),
          ],
          'padding': [_padding(), _padding(directional: true)],
          'isLabelVisible': [_bool(false), _bool(true)],
        };
        for (final entry in choices.entries) {
          final state = digits % 3;
          digits ~/= 3;
          if (state != 0) properties[entry.key] = entry.value[state - 1];
        }
        expect(
          _node(_decode(_model(properties: properties))).properties.keys,
          unorderedEquals(properties.keys),
        );
      }
    },
  );

  test(
    'Badge numeric padding alignment and offset use strict finite typed geometry',
    () {
      for (final name in ['smallSize']) {
        for (final value in [
          _int(-1),
          _double(-.1),
          _string('6'),
          _bool(false),
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
          );
        }
        for (final value in [_int(0), _double(0), _double(.125)]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            returnsNormally,
          );
        }
      }
      for (final directional in [false, true]) {
        final padding = _padding(directional: directional)..['top'] = -1;
        expect(
          () => _decode(_model(properties: {'padding': padding})),
          throwsFormatException,
        );
        expect(
          () => _decode(
            _model(
              properties: {
                'alignment': _alignment(directional: directional, x: -4, y: 7),
                'offset': _offset(-900, 800),
              },
            ),
          ),
          returnsNormally,
        );
      }
      for (final name in ['padding', 'alignment', 'offset']) {
        expect(
          () => _decode(_model(properties: {name: _double(1)})),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Badge all 31 text style fields share exact reviewed domains and relational validation',
    () {
      expect(
        () => _decode(_model(properties: _allProperties())),
        returnsNormally,
      );
      for (final pair in [
        ['textStyleColor', 'textStyleForeground'],
        ['textStyleBackgroundColor', 'textStyleBackground'],
      ]) {
        expect(
          () => _decode(
            _model(
              properties: {
                pair.first: _color('0xFF000000'),
                pair.last: _paint(),
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final properties in <Map<String, Object?>>[
        {'textStylePackage': _string('fonts')},
        {
          'textStylePackage': _string(''),
          'textStyleFontFamily': _string('Face'),
        },
        {'textStyleLocaleLanguageCode': _string('EN')},
        {'textStyleLocaleScriptCode': _string('latn')},
        {'textStyleLocaleCountryCode': _string('uk')},
        {'textStyleFontSize': _double(-1)},
        {'textStyleFontSize': _int(12)},
        {'textStyleThemeTextStyle': _theme('material.colorScheme.error')},
        {'textColor': _theme('material.textTheme.labelSmall')},
      ]) {
        expect(
          () => _decode(_model(properties: properties)),
          throwsFormatException,
          reason: '$properties',
        );
      }
      for (final name in [
        'textStyleHeight',
        'textStyleLetterSpacing',
        'textStyleWordSpacing',
        'textStyleDecorationThickness',
      ]) {
        expect(
          () => _decode(_model(properties: {name: _double(-.25)})),
          returnsNormally,
        );
      }
    },
  );

  test(
    'Badge typed style arrays preserve explicit emptiness and enforce IDs tags and bounded sizes',
    () {
      for (final kind in [
        'shadowList',
        'fontFeatureList',
        'fontVariationList',
      ]) {
        final name = {
          'shadowList': 'textStyleShadows',
          'fontFeatureList': 'textStyleFontFeatures',
          'fontVariationList': 'textStyleFontVariations',
        }[kind]!;
        expect(
          () => _decode(
            _model(
              properties: {
                name: {'kind': kind, 'items': []},
              },
            ),
          ),
          returnsNormally,
        );
        final payload = _allProperties()[name] as Map;
        final item = (payload['items'] as List).first;
        expect(
          () => _decode(
            _model(
              properties: {
                name: {
                  'kind': kind,
                  'items': [item, item],
                },
              },
            ),
          ),
          throwsFormatException,
        );
        expect(
          () => _decode(
            _model(
              properties: {
                name: {'kind': kind, 'items': List.filled(257, item)},
              },
            ),
          ),
          throwsFormatException,
        );
      }
    },
  );

  testWidgets(
    'Badge all-unset maps to actual SDK dot with nullable properties',
    (tester) async {
      await _pump(tester, _model());
      final badge = tester.widget<Badge>(_badge());
      expect(badge.label, isNull);
      expect(badge.child, isNull);
      expect(badge.backgroundColor, isNull);
      expect(badge.textColor, isNull);
      expect(badge.smallSize, isNull);
      expect(badge.largeSize, isNull);
      expect(badge.textStyle, isNull);
      expect(badge.padding, isNull);
      expect(badge.alignment, isNull);
      expect(badge.offset, isNull);
      expect(badge.isLabelVisible, true);
      expect(tester.getSize(_badge()), const Size(6, 6));
      expect(_decoration(tester).shape, const StadiumBorder());
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Badge.count uses exact zero boundary max overflow and portable-int labels',
    (tester) async {
      for (final count in [0, 1, 998, 999, 1000, 9007199254740991]) {
        for (final max in <int?>[null, 1, 999, 9007199254740991]) {
          await _pump(
            tester,
            _model(
              properties: {
                'count': _int(count),
                if (max != null) 'maxCount': _int(max),
              },
            ),
          );
          final limit = max ?? 999;
          expect(
            (tester.widget<Badge>(_badge()).label! as Text).data,
            count > limit ? '$limit+' : '$count',
          );
          expect(
            find.text(count > limit ? '$limit+' : '$count'),
            findsOneWidget,
          );
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  testWidgets('Badge M3 generated defaults also apply under Material2', (
    tester,
  ) async {
    for (final m3 in [false, true]) {
      final theme = ThemeData(
        useMaterial3: m3,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.purple),
      );
      await _pump(tester, _model(), theme: theme);
      expect(tester.getSize(_badge()), const Size(6, 6));
      expect(_decoration(tester).color, theme.colorScheme.error);
      await _pump(tester, _model(properties: {'count': _int(1)}), theme: theme);
      final style = _labelStyle(tester);
      expect(
        style.fontSize,
        Theme.of(tester.element(_badge())).textTheme.labelSmall!.fontSize,
      );
      expect(style.color, theme.colorScheme.onError);
      expect(_badgePadding(tester), const EdgeInsets.symmetric(horizontal: 4));
      expect(tester.getSize(_badge()).height, 16);
      expect(tester.takeException(), isNull);
    }
  });

  testWidgets(
    'Badge local values override BadgeTheme and clearing locals restores inherited values',
    (tester) async {
      final theme = ThemeData(
        badgeTheme: const BadgeThemeData(
          backgroundColor: Colors.green,
          textColor: Colors.yellow,
          smallSize: 11,
          largeSize: 31,
          padding: EdgeInsets.all(2),
          alignment: Alignment.bottomLeft,
          offset: Offset(10, 20),
          textStyle: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
        ),
      );
      await _pump(tester, _model(), theme: theme);
      expect(tester.getSize(_badge()), const Size(11, 11));
      expect(_decoration(tester).color, Colors.green);
      await _pump(tester, _model(properties: {'count': _int(7)}), theme: theme);
      expect(tester.getSize(_badge()).height, 31);
      expect(_labelStyle(tester).fontWeight, FontWeight.w700);
      expect(_labelStyle(tester).color, Colors.yellow);
      await _pump(
        tester,
        _model(
          properties: {
            'count': _int(7),
            'backgroundColor': _color('0xFF123456'),
            'textColor': _color('0xFF654321'),
            'largeSize': _int(50),
            'padding': _padding(),
            'textStyleFontSize': _double(10),
          },
        ),
        theme: theme,
      );
      expect(_decoration(tester).color, const Color(0xff123456));
      expect(_labelStyle(tester).color, const Color(0xff654321));
      expect(tester.getSize(_badge()).height, 50);
      expect(_badgePadding(tester), const EdgeInsets.fromLTRB(3, 5, 7, 9));
      expect(
        tester.widget<Badge>(_badge()).textStyle!.fontWeight,
        isNull,
        reason: 'local TextStyle is replacement, not a merge with BadgeTheme',
      );
      await _pump(tester, _model(properties: {'count': _int(7)}), theme: theme);
      expect(_decoration(tester).color, Colors.green);
      expect(_labelStyle(tester).fontWeight, FontWeight.w700);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Badge forwards the full 31-leaf text style domain through the shared actual TextStyle',
    (tester) async {
      final properties = _allProperties()
        ..remove('textStyleColor')
        ..remove('textStyleBackgroundColor');
      properties['textStyleForeground'] = _paint();
      properties['textStyleBackground'] = _paint(theme: false);
      await _pump(tester, _model(properties: properties));
      final badge = tester.widget<Badge>(_badge());
      final style = badge.textStyle!;
      final colors = Theme.of(tester.element(_badge())).colorScheme;
      expect(style.inherit, false);
      expect(style.foreground!.color.toARGB32(), colors.primary.toARGB32());
      expect(style.foreground!.strokeWidth, 1.5);
      expect(style.foreground!.style, PaintingStyle.stroke);
      expect(style.background!.color.toARGB32(), 0x44123456);
      expect(style.fontSize, 18.5);
      expect(style.fontWeight, FontWeight.w600);
      expect(style.fontStyle, FontStyle.italic);
      expect(style.letterSpacing, 1.25);
      expect(style.wordSpacing, 2.5);
      expect(style.height, 1.4);
      expect(style.textBaseline, TextBaseline.ideographic);
      expect(style.leadingDistribution, TextLeadingDistribution.even);
      expect(
        style.locale,
        const Locale.fromSubtags(
          languageCode: 'en',
          scriptCode: 'Latn',
          countryCode: 'GB',
        ),
      );
      expect(style.shadows!.single.color, colors.shadow);
      expect(style.shadows!.single.offset, const Offset(-1.25, 2.5));
      expect(style.shadows!.single.blurRadius, 4);
      expect(style.fontFeatures, const [FontFeature('liga', 1)]);
      expect(style.fontVariations, const [FontVariation('wght', 700)]);
      expect(
        style.decoration,
        TextDecoration.combine([
          TextDecoration.underline,
          TextDecoration.overline,
          TextDecoration.lineThrough,
        ]),
      );
      expect(style.decorationColor, colors.error);
      expect(style.decorationStyle, TextDecorationStyle.wavy);
      expect(style.decorationThickness, 2.25);
      expect(style.debugLabel, 'badge style');
      expect(style.fontFamily, 'packages/design_fonts/Inter');
      expect(style.fontFamilyFallback, [
        'packages/design_fonts/Noto Sans',
        'packages/design_fonts/Noto Color Emoji',
      ]);
      expect(style.overflow, TextOverflow.fade);
      expect(
        _labelStyle(tester).foreground!.color.toARGB32(),
        colors.primary.toARGB32(),
      );
      expect(_labelStyle(tester).color, isNull);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Badge foreground paint wins over explicit textColor in raw SDK and Canvas',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: Badge.count(
              count: 7,
              textColor: Colors.red,
              textStyle: TextStyle(foreground: Paint()..color = Colors.blue),
            ),
          ),
        ),
      );
      final raw = DefaultTextStyle.of(tester.element(find.text('7'))).style;
      expect(raw.foreground!.color.toARGB32(), Colors.blue.toARGB32());
      expect(raw.color, isNull);
      await _pump(
        tester,
        _model(
          properties: {
            'count': _int(7),
            'textColor': _color('0xFFFF0000'),
            'textStyleForeground': _paint(),
          },
        ),
      );
      expect(
        _labelStyle(tester).foreground!.color.toARGB32(),
        Theme.of(tester.element(_badge())).colorScheme.primary.toARGB32(),
      );
      expect(_labelStyle(tester).color, isNull);
      await _pump(
        tester,
        _model(
          properties: {
            'count': _int(7),
            'textColor': _color('0xFFFF0000'),
            'textStyleColor': _color('0xFF0000FF'),
          },
        ),
      );
      expect(
        tester.widget<Badge>(_badge()).textStyle!.color,
        const Color(0xff0000ff),
      );
      expect(_labelStyle(tester).color, const Color(0xffff0000));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Badge theme text base resolves dynamically and explicit false empty lists reset inherited style fields',
    (tester) async {
      final properties = <String, Object?>{
        'count': _int(1),
        'textStyleThemeTextStyle': _theme('material.textTheme.titleLarge'),
      };
      for (final size in [20.0, 28.0]) {
        final theme = ThemeData(
          textTheme: TextTheme(
            titleLarge: TextStyle(
              fontSize: size,
              shadows: const [Shadow(color: Colors.red)],
              fontFeatures: const [FontFeature('liga')],
              fontVariations: const [FontVariation('wght', 600)],
              decoration: TextDecoration.underline,
            ),
          ),
        );
        await _pump(tester, _model(properties: properties), theme: theme);
        expect(tester.widget<Badge>(_badge()).textStyle!.fontSize, size);
        await _pump(
          tester,
          _model(
            properties: {
              ...properties,
              'textStyleInherit': _bool(false),
              'textStyleDecorationUnderline': _bool(false),
              'textStyleShadows': {'kind': 'shadowList', 'items': []},
              'textStyleFontFeatures': {'kind': 'fontFeatureList', 'items': []},
              'textStyleFontVariations': {
                'kind': 'fontVariationList',
                'items': [],
              },
            },
          ),
          theme: theme,
        );
        final style = tester.widget<Badge>(_badge()).textStyle!;
        expect(style.inherit, false);
        expect(style.decoration, TextDecoration.none);
        expect(style.shadows, isEmpty);
        expect(style.fontFeatures, isEmpty);
        expect(style.fontVariations, isEmpty);
      }
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Badge label custom Text style wins after inherited Badge style and text scaling grows stadium',
    (tester) async {
      final label = _text(label: true);
      (label['properties'] as Map).addAll(<String, Map<String, Object?>>{
        'styleColor': _color('0xFF010203'),
        'styleFontSize': _double(24),
      });
      await _pump(
        tester,
        _model(
          label: label,
          properties: {
            'textColor': _color('0xFFFF0000'),
            'textStyleFontSize': _double(10),
          },
        ),
      );
      final rich = tester.widget<RichText>(
        find.descendant(of: _widget(_labelId), matching: find.byType(RichText)),
      );
      expect(rich.text.style!.color, const Color(0xff010203));
      expect(rich.text.style!.fontSize, 24);
      final first = tester.getSize(_badge());
      await _pump(tester, _model(label: label), textScale: 2);
      final scaled = tester.getSize(_badge());
      expect(scaled.height, greaterThan(first.height));
      expect(scaled.width, greaterThanOrEqualTo(scaled.height));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Pinned Badge stadium minSize is stale after a raw SDK widget or theme update',
    (tester) async {
      for (final useTheme in [false, true]) {
        await tester.pumpWidget(const SizedBox());
        for (final size in [31.0, 50.0, -10.0]) {
          await tester.pumpWidget(
            MaterialApp(
              theme: ThemeData(
                badgeTheme: useTheme ? BadgeThemeData(largeSize: size) : null,
              ),
              themeAnimationDuration: Duration.zero,
              home: Center(
                child: Badge.count(count: 1, largeSize: useTheme ? null : size),
              ),
            ),
          );
          await tester.pump();
          expect(
            tester.getSize(find.byType(Badge)).height,
            31,
            reason:
                '3.44.8 _IntrinsicHorizontalStadium lacks updateRenderObject',
          );
        }
      }
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Badge finite negative largeSize is valid in both actual SDK constructors and Canvas',
    (tester) async {
      for (final count in [false, true]) {
        for (final size in [-9007199254740991.0, -30.5, -1.0, 0.0]) {
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: count
                    ? Badge.count(
                        key: ValueKey((count, size)),
                        count: 1,
                        largeSize: size,
                      )
                    : Badge(
                        key: ValueKey((count, size)),
                        largeSize: size,
                        label: const SizedBox(width: 20, height: 20),
                      ),
              ),
            ),
          );
          final rawSize = tester.getSize(find.byType(Badge));
          expect(rawSize.height, greaterThanOrEqualTo(0));
          expect(rawSize.width, greaterThanOrEqualTo(rawSize.height));
          expect(tester.takeException(), isNull);
          await _pump(
            tester,
            _model(
              label: count ? null : _labelBox(),
              properties: {
                'largeSize': _double(size),
                if (count) 'count': _int(1),
              },
            ),
          );
          expect(tester.getSize(_badge()), rawSize);
          expect(tester.widget<Badge>(_badge()).largeSize, size);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'Badge normal label to count to dot transitions update structure and dynamic slot eligibility on $platform',
      (tester) async {
        final model = _model(
          platform: platform,
          child: _box(),
          label: _text(label: true),
        );
        CanvasMovePreviewResolver? move;
        await _pump(tester, model, onMove: (value) => move = value);
        expect(_widget(_labelId), findsOneWidget);
        final slots = _leaf(model)['slots'] as Map;
        final properties = _leaf(model)['properties'] as Map;
        (slots['label'] as Map)['child'] = null;
        properties['count'] = _int(1234);
        await _pump(tester, model, onMove: (value) => move = value);
        expect(_widget(_labelId), findsNothing);
        expect(find.text('999+'), findsOneWidget);
        expect(move!(_childId, _id, 'label', 0), isNull);
        properties.remove('count');
        await _pump(tester, model, onMove: (value) => move = value);
        expect(tester.widget<Badge>(_badge()).label, isNull);
        expect(tester.getSize(_badgeContainer()), const Size(6, 6));
        expect(move!(_childId, _id, 'label', 0)?.slotName, 'label');
        (slots['label'] as Map)['child'] = _text(label: true);
        await _pump(tester, model);
        expect(_widget(_labelId), findsOneWidget);
        expect(_widget(_childId), findsOneWidget);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Badge supports required wrapping in both populated slots and hides inactive descendant drop targets on $platform',
      (tester) async {
        for (final label in [false, true]) {
          final node = label ? _labelBox() : _box();
          final model = _model(
            platform: platform,
            child: label ? null : node,
            label: label ? node : null,
          );
          CanvasDropResolver? drop;
          CanvasMovePreviewResolver? move;
          await _pump(
            tester,
            model,
            onDrop: (value) => drop = value,
            onMove: (value) => move = value,
          );
          final id = label ? _labelId : _childId;
          final surface = tester.getRect(find.byType(CanvasDocumentView));
          final point = tester.getCenter(_widget(id));
          final wrapped = drop!(
            ((point.dx - surface.left) / surface.width * 1000000).round(),
            ((point.dy - surface.top) / surface.height * 1000000).round(),
            CanvasPaletteDragSource(
              token: 'ticker',
              widgetType: 'flutter.widgets.TickerMode',
              traits: const {},
            ),
          );
          expect(wrapped?.parentWidgetId, _id);
          expect(wrapped?.slotName, label ? 'label' : 'child');
          final slot =
              (_leaf(model)['slots'] as Map)[label ? 'label' : 'child'] as Map;
          slot['child'] = {
            'id': _rowId,
            'type': 'flutter.widgets.TickerMode',
            'properties': {'enabled': _bool(true)},
            'slots': {
              'child': {'kind': 'single', 'child': node},
            },
          };
          await _pump(tester, model, onMove: (value) => move = value);
          expect(
            move!(id, _id, label ? 'child' : 'label', 0),
            isNull,
            reason: 'moving would empty TickerMode required child',
          );
          if (label) {
            (_leaf(model)['properties'] as Map)['isLabelVisible'] = _bool(
              false,
            );
            await _pump(tester, model, onMove: (value) => move = value);
            expect(_widget(_rowId), findsNothing);
            expect(_widget(_labelId), findsNothing);
            expect(move!(_id, _labelId, 'child', 0), isNull);
          }
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'Badge live local or inherited largeSize recreates only SDK shell retaining TextField state and focus identity on $platform',
      (tester) async {
        for (final count in [false, true]) {
          for (final useTheme in [false, true]) {
            await tester.pumpWidget(const SizedBox());
            Element? fieldElement;
            State? fieldState;
            FocusNode? focus;
            Element? labelElement;
            for (final size in [31.0, 55.0, 70.0]) {
              final model = _model(
                platform: platform,
                child: _fieldBox(),
                label: count ? null : _labelBox(),
                properties: {
                  if (count) 'count': _int(1),
                  if (!useTheme) 'largeSize': _double(size),
                },
              );
              await _pump(
                tester,
                model,
                theme: ThemeData(
                  badgeTheme: useTheme ? BadgeThemeData(largeSize: size) : null,
                ),
              );
              final field = find.descendant(
                of: _widget(_childId),
                matching: find.byType(TextField),
              );
              final element = tester.element(field);
              final state = tester.state(field);
              final editable = tester.widget<EditableText>(
                find.descendant(of: field, matching: find.byType(EditableText)),
              );
              if (fieldElement != null) {
                expect(identical(fieldElement, element), true);
                expect(identical(fieldState, state), true);
                expect(identical(focus, editable.focusNode), true);
                expect(editable.controller.text, 'retained runtime value');
              } else {
                editable.controller.text = 'retained runtime value';
              }
              expect(
                editable.focusNode.canRequestFocus,
                false,
                reason:
                    'ordinary preview TextField remains isolated from Designer input',
              );
              fieldElement = element;
              fieldState = state;
              focus = editable.focusNode;
              if (!count) {
                final currentLabel = tester.element(
                  find
                      .descendant(
                        of: _widget(_labelId),
                        matching: find.byType(SizedBox),
                      )
                      .last,
                );
                if (labelElement != null) {
                  expect(identical(labelElement, currentLabel), true);
                }
                labelElement = currentLabel;
              }
              expect(tester.getSize(_badgeContainer()).height, size);
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    testWidgets(
      'Badge minimum-size SDK workaround preserves active inline label editor state focus and text on $platform',
      (tester) async {
        for (final useTheme in [false, true]) {
          await tester.pumpWidget(const SizedBox());
          final commits = <String>[];
          final model = _model(
            platform: platform,
            label: _text(label: true),
            properties: {if (!useTheme) 'largeSize': _int(30)},
          );
          await _pump(
            tester,
            model,
            selectedId: _labelId,
            commits: commits,
            theme: ThemeData(
              badgeTheme: useTheme ? const BadgeThemeData(largeSize: 30) : null,
            ),
          );
          await tester.tap(_widget(_labelId));
          await tester.pump(const Duration(milliseconds: 300));
          await tester.sendKeyEvent(LogicalKeyboardKey.f2);
          await tester.pump();
          final editor = find.byType(EditableText);
          expect(editor, findsOneWidget);
          await tester.enterText(editor, 'Keep focused label');
          final before = tester.element(editor);
          final state = tester.state(editor);
          final focus = tester.widget<EditableText>(editor).focusNode;
          if (!useTheme) {
            (_leaf(model)['properties'] as Map)['largeSize'] = _int(80);
          }
          await _pump(
            tester,
            model,
            selectedId: _labelId,
            commits: commits,
            theme: ThemeData(
              badgeTheme: useTheme ? const BadgeThemeData(largeSize: 80) : null,
            ),
          );
          expect(identical(before, tester.element(editor)), true);
          expect(identical(state, tester.state(editor)), true);
          expect(
            identical(focus, tester.widget<EditableText>(editor).focusNode),
            true,
          );
          expect(focus.hasFocus, true);
          expect(
            tester.widget<EditableText>(editor).controller.text,
            'Keep focused label',
          );
          expect(
            tester.getSize(_badgeContainer()).height,
            greaterThanOrEqualTo(80),
          );
          await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
          await tester.sendKeyEvent(LogicalKeyboardKey.enter);
          await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
          await tester.pump();
          expect(commits, ['$_labelId:Keep focused label']);
          expect(tester.takeException(), isNull);
        }
      },
    );
    testWidgets(
      'Badge actual default label positioning handles RTL default offset and SDK +8 adjustment on $platform',
      (tester) async {
        for (final direction in TextDirection.values) {
          for (final explicit in [false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                child: _box(),
                label: _labelBox(),
                properties: {
                  if (explicit) 'offset': _offset(0, 0),
                  'largeSize': _int(16),
                },
              ),
              direction: direction,
            );
            final child = tester.getRect(_widget(_childId));
            final badgeRect = tester.getRect(_badgeContainer());
            expect(tester.getSize(_badge()), const Size(100, 60));
            final x = direction == TextDirection.ltr
                ? 100 - 16 + (explicit ? 0 : 4)
                : (explicit ? 0 : -4);
            expect(
              (badgeRect.left - child.left) / _scale(tester),
              closeTo(x.toDouble(), .001),
            );
            expect(
              (badgeRect.top - child.top) / _scale(tester),
              closeTo(
                (explicit ? 8 : 4) - badgeRect.height / _scale(tester) / 2,
                .001,
              ),
            );
            expect(
              tester
                  .widget<Stack>(
                    find
                        .descendant(of: _badge(), matching: find.byType(Stack))
                        .first,
                  )
                  .clipBehavior,
              Clip.none,
            );
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'Badge small dot ignores offset but honors physical or directional alignment on $platform',
      (tester) async {
        for (final direction in TextDirection.values) {
          for (final directional in [false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                child: _box(),
                properties: {
                  'smallSize': _int(8),
                  'alignment': _alignment(directional: directional, x: 1, y: 1),
                  'offset': _offset(-400, 400),
                  'padding': _padding(),
                  'textStyleFontSize': _double(90),
                },
              ),
              direction: direction,
            );
            final child = tester.getRect(_widget(_childId));
            final dot = tester.getRect(_badgeContainer());
            expect(tester.getSize(_badgeContainer()), const Size(8, 8));
            expect((dot.top - child.top) / _scale(tester), closeTo(60, .001));
            expect(
              (dot.left - child.left) / _scale(tester),
              closeTo(
                directional && direction == TextDirection.rtl ? 0 : 92,
                .001,
              ),
            );
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'Badge directional padding and alignment resolve through actual SDK in both directions on $platform',
      (tester) async {
        for (final direction in TextDirection.values) {
          await _pump(
            tester,
            _model(
              platform: platform,
              child: _box(),
              label: _labelBox(),
              properties: {
                'padding': _padding(directional: true),
                'alignment': _alignment(directional: true, x: -1, y: 1),
                'offset': _offset(2, -3),
              },
            ),
            direction: direction,
          );
          final child = tester.getRect(_widget(_childId));
          final label = tester.getRect(_widget(_labelId));
          final container = tester.getRect(_badgeContainer());
          expect(
            _badgePadding(tester),
            const EdgeInsetsDirectional.fromSTEB(3, 5, 7, 9),
          );
          expect(
            (label.left - container.left) / _scale(tester),
            closeTo(direction == TextDirection.ltr ? 5 : 9, .001),
          );
          expect(
            (container.left - child.left) / _scale(tester),
            closeTo(direction == TextDirection.ltr ? 2 : 86, .001),
          );
          expect(
            (container.top - child.top) / _scale(tester),
            closeTo(65 - container.height / _scale(tester) / 2, .001),
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'Badge hidden label is unmounted but child keeps layout pointer and semantics on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          final selected = <String>[];
          for (final hidden in [false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                child: _text(),
                label: _text(label: true),
                properties: {'isLabelVisible': _bool(!hidden)},
              ),
              selected: selected,
            );
            expect(_widget(_childId), findsOneWidget);
            expect(_widget(_labelId), hidden ? findsNothing : findsOneWidget);
            expect(_semanticsLabels(tester).contains('Badge child'), true);
            expect(_semanticsLabels(tester).contains('Badge label'), !hidden);
            expect(tester.getSize(_badge()), tester.getSize(_widget(_childId)));
            await tester.tap(_widget(_childId));
            await tester.pump(const Duration(milliseconds: 300));
            expect(selected, contains(_childId));
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'Badge own F2 is rejected and hidden selected label cannot be resurrected on $platform',
      (tester) async {
        for (final selectedId in [_id, _labelId]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              label: _text(label: true),
              properties: {'isLabelVisible': _bool(false)},
            ),
            selectedId: selectedId,
          );
          await tester.tap(
            find.byKey(const ValueKey('canvas-zero-size-widget-target-$_id')),
          );
          await tester.pump();
          await tester.sendKeyEvent(LogicalKeyboardKey.f2);
          await tester.pump();
          expect(find.byType(EditableText), findsNothing);
          expect(_widget(_labelId), findsNothing);
          expect(tester.takeException(), isNull);
        }
        await _pump(
          tester,
          _model(platform: platform, properties: {'count': _int(7)}),
        );
        await tester.tap(_badge());
        await tester.pump();
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        expect(find.byType(EditableText), findsNothing);
      },
    );

    testWidgets(
      'Badge visible label and child support normal F2 and retained focus across style edits on $platform',
      (tester) async {
        for (final label in [false, true]) {
          final target = label ? _labelId : _childId;
          final commits = <String>[];
          final model = _model(
            platform: platform,
            child: _text(),
            label: _text(label: true),
            properties: {'alignment': _alignment(x: 0, y: 0)},
          );
          await _pump(tester, model, selectedId: target, commits: commits);
          if (label) {
            await tester.tap(_widget(target));
          } else {
            // The visible label overlaps the child center; use its unobscured
            // left edge, which is where the actual SDK permits a child click.
            final rect = tester.getRect(_widget(target));
            await tester.tapAt(Offset(rect.left + 2, rect.center.dy));
          }
          await tester.pump(const Duration(milliseconds: 300));
          await tester.sendKeyEvent(LogicalKeyboardKey.f2);
          await tester.pump();
          expect(find.byType(EditableText), findsOneWidget);
          await tester.enterText(
            find.byType(EditableText),
            'Edited badge text',
          );
          (_leaf(model)['properties'] as Map)['textColor'] = _color(
            '0xFF123456',
          );
          await _pump(tester, model, selectedId: target, commits: commits);
          expect(
            tester
                .widget<EditableText>(find.byType(EditableText))
                .focusNode
                .hasFocus,
            true,
          );
          await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
          await tester.sendKeyEvent(LogicalKeyboardKey.enter);
          await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
          await tester.pump();
          expect(commits, ['$target:Edited badge text']);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'Badge empty dot or hidden zero-size widget exposes separate label and child insert zones on $platform',
      (tester) async {
        for (final hidden in [false, true]) {
          CanvasDropResolver? resolver;
          final selected = <String>[];
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {if (hidden) 'isLabelVisible': _bool(false)},
            ),
            selected: selected,
            onDrop: (value) => resolver = value,
          );
          Rect rect;
          if (hidden) {
            final handle = find.byKey(
              const ValueKey('canvas-zero-size-widget-target-$_id'),
            );
            expect(handle, findsOneWidget);
            rect = tester.getRect(handle);
            await tester.tap(handle);
          } else {
            rect = tester.getRect(_badge());
            await tester.tap(_badge());
          }
          expect(selected, contains(_id));
          final label = _resolve(
            tester,
            resolver!,
            Offset(rect.left + rect.width * .75, rect.top + rect.height * .25),
          );
          final child = _resolve(
            tester,
            resolver!,
            Offset(rect.left + rect.width * .25, rect.top + rect.height * .75),
          );
          expect(label?.parentWidgetId, _id);
          expect(label?.slotName, 'label');
          expect(child?.parentWidgetId, _id);
          expect(child?.slotName, 'child');
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'Badge count never advertises label insertion and never accepts a host move into label on $platform',
      (tester) async {
        for (final hidden in [false, true]) {
          CanvasDropResolver? drop;
          CanvasMovePreviewResolver? move;
          await _pump(
            tester,
            _model(
              platform: platform,
              child: _box(),
              properties: {'count': _int(0), 'isLabelVisible': _bool(!hidden)},
            ),
            onDrop: (value) => drop = value,
            onMove: (value) => move = value,
          );
          final rect = tester.getRect(_badge());
          for (final x in [.1, .5, .9]) {
            for (final y in [.1, .5, .9]) {
              final target = _resolve(
                tester,
                drop!,
                Offset(rect.left + rect.width * x, rect.top + rect.height * y),
              );
              expect(
                target?.parentWidgetId == _id && target?.slotName == 'label',
                false,
              );
            }
          }
          expect(move!(_childId, _id, 'label', 0), isNull);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'Badge both optional slots can move out and accept host-authorized moves without auto deleting content on $platform',
      (tester) async {
        for (final label in [false, true]) {
          final child = label ? null : _box();
          final labelNode = label ? _labelBox() : null;
          final model = _model(
            platform: platform,
            child: child,
            label: labelNode,
          );
          final badge = _leaf(model);
          _centerSlot(model)['child'] = {
            'id': _rowId,
            'type': 'flutter.widgets.Row',
            'properties': <String, Object?>{},
            'slots': {
              'children': {
                'kind': 'list',
                'children': [badge],
              },
            },
          };
          CanvasMovePreviewResolver? move;
          await _pump(tester, model, onMove: (value) => move = value);
          final targetId = label ? _labelId : _childId;
          expect(move!(targetId, _rowId, 'children', 1)?.slotName, 'children');
          expect(
            move!(targetId, _id, label ? 'child' : 'label', 0)?.slotName,
            label ? 'child' : 'label',
          );
          final sourceSlot =
              (badge['slots'] as Map)[label ? 'label' : 'child'] as Map;
          final moved = sourceSlot['child'];
          sourceSlot['child'] = null;
          (((_centerSlot(model)['child'] as Map)['slots'] as Map)['children']
              as Map)['children'] = [
            badge,
            moved,
          ];
          await _pump(tester, model, onMove: (value) => move = value);
          expect(tester.widget<Badge>(_badge()).child, isNull);
          expect(tester.widget<Badge>(_badge()).label, isNull);
          expect(_widget(targetId), findsOneWidget);
          expect(tester.takeException(), isNull);
        }
      },
    );
  }

  testWidgets(
    'Badge count updates retain child element and hidden branch lifecycle matches raw SDK',
    (tester) async {
      final lifecycle = <String>[];
      for (final visible in [true, true, false, false, true]) {
        await tester.pumpWidget(
          MaterialApp(
            home: Center(
              child: Badge(
                isLabelVisible: visible,
                label: const Text('raw'),
                child: _LifecycleProbe(events: lifecycle),
              ),
            ),
          ),
        );
      }
      expect(lifecycle, ['init', 'init', 'dispose', 'init', 'dispose']);
      Element? child;
      for (final count in [0, 1, 1000]) {
        await _pump(
          tester,
          _model(child: _box(), properties: {'count': _int(count)}),
        );
        final current = tester.element(_widget(_childId));
        if (child != null) expect(identical(current, child), true);
        child = current;
      }
      await _pump(
        tester,
        _model(
          child: _box(),
          properties: {'count': _int(1), 'isLabelVisible': _bool(false)},
        ),
      );
      expect(
        identical(tester.element(_widget(_childId)), child),
        false,
        reason: 'actual SDK changes Stack branch; no synthetic retained state',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Badge label outside child paints unclipped but pointer bounds match raw SDK',
    (tester) async {
      var taps = 0;
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: Badge(
              label: GestureDetector(
                onTap: () => taps++,
                child: const SizedBox(width: 20, height: 20),
              ),
              child: const SizedBox(width: 100, height: 60),
            ),
          ),
        ),
      );
      final rawBadge = tester.getRect(find.byType(Badge));
      final gesture = find.descendant(
        of: find.byType(Badge),
        matching: find.byType(GestureDetector),
      );
      final outside = tester.getRect(gesture).topRight - const Offset(1, -1);
      expect(rawBadge.contains(outside), false);
      await tester.tapAt(outside);
      expect(taps, 0);
      final selected = <String>[];
      await _pump(
        tester,
        _model(child: _box(), label: _labelBox()),
        selected: selected,
      );
      final label = tester.getRect(_widget(_labelId));
      await tester.tapAt(label.topRight - const Offset(1, -1));
      expect(selected, isNot(contains(_labelId)));
      expect(tester.takeException(), isNull);
    },
  );
}

Map<String, Object?> _allProperties() => {
  'backgroundColor': _theme('material.colorScheme.error'),
  'textColor': _color('0xFF102030'),
  'smallSize': _int(7),
  'largeSize': _double(22),
  'padding': _padding(),
  'alignment': _alignment(),
  'offset': _offset(-2, 3),
  'isLabelVisible': _bool(true),
  'count': _int(7),
  'maxCount': _int(99),
  'textStyleThemeTextStyle': _theme('material.textTheme.bodyLarge'),
  'textStyleInherit': _bool(false),
  'textStyleColor': _color('0xFF123456'),
  'textStyleBackgroundColor': _color('0x44123456'),
  'textStyleFontSize': _double(18.5),
  'textStyleFontWeight': _enum('FontWeight', 'w600'),
  'textStyleFontStyle': _enum('FontStyle', 'italic'),
  'textStyleLetterSpacing': _double(1.25),
  'textStyleWordSpacing': _double(2.5),
  'textStyleTextBaseline': _enum('TextBaseline', 'ideographic'),
  'textStyleHeight': _double(1.4),
  'textStyleLeadingDistribution': _enum('TextLeadingDistribution', 'even'),
  'textStyleLocaleLanguageCode': _string('en'),
  'textStyleLocaleScriptCode': _string('Latn'),
  'textStyleLocaleCountryCode': _string('GB'),
  'textStyleShadows': {
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
  'textStyleFontFeatures': {
    'kind': 'fontFeatureList',
    'items': [
      {'id': 'd8ca6ff9-1aa5-4bb1-944b-fdd475b5359d', 'tag': 'liga', 'value': 1},
    ],
  },
  'textStyleFontVariations': {
    'kind': 'fontVariationList',
    'items': [
      {
        'id': '2115406c-c05d-4323-81a2-7cfe7ea35dc4',
        'axis': 'wght',
        'value': 700.0,
      },
    ],
  },
  'textStyleDecorationUnderline': _bool(true),
  'textStyleDecorationOverline': _bool(true),
  'textStyleDecorationLineThrough': _bool(true),
  'textStyleDecorationColor': _theme('material.colorScheme.error'),
  'textStyleDecorationStyle': _enum('TextDecorationStyle', 'wavy'),
  'textStyleDecorationThickness': _double(2.25),
  'textStyleDebugLabel': _string('badge style'),
  'textStyleFontFamily': _string('Inter'),
  'textStyleFontFamilyFallback': _string('Noto Sans\nNoto Color Emoji'),
  'textStylePackage': _string('design_fonts'),
  'textStyleOverflow': _enum('TextOverflow', 'fade'),
};

Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
  Map<String, Object?>? label,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  model['root'] = {
    'id': 'cd8537b9-fdda-4d41-9c09-ff1577332716',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': {
          'id': '80e42b1c-ae0d-428a-8c19-674ad86c68ee',
          'type': 'flutter.widgets.Center',
          'properties': <String, Object?>{},
          'slots': {
            'child': {
              'kind': 'single',
              'child': {
                'id': _id,
                'type': _type,
                'properties': Map<String, Object?>.of(properties),
                'slots': {
                  'child': {'kind': 'single', 'child': child},
                  'label': {'kind': 'single', 'child': label},
                },
              },
            },
          },
        },
      },
    },
  };
  return model;
}

Map _centerSlot(Map<String, Object?> model) =>
    (((((model['root'] as Map)['slots'] as Map)['body'] as Map)['child']
                as Map)['slots']
            as Map)['child']
        as Map;
Map<String, Object?> _leaf(Map<String, Object?> model) =>
    _centerSlot(model)['child'] as Map<String, Object?>;
Map<String, Object?> _box() => {
  'id': _childId,
  'type': 'flutter.widgets.SizedBox',
  'properties': {'width': _int(100), 'height': _int(60)},
  'slots': <String, Object?>{},
};
Map<String, Object?> _fieldBox() => {
  'id': _childId,
  'type': 'flutter.widgets.SizedBox',
  'properties': {'width': _int(150), 'height': _int(60)},
  'slots': {
    'child': {
      'kind': 'single',
      'child': {
        'id': 'a614957f-5a36-4e45-a729-1d19050cdb22',
        'type': 'flutter.material.TextField',
        'properties': <String, Object?>{},
        'slots': <String, Object?>{},
      },
    },
  },
};
Map<String, Object?> _labelBox() => {
  'id': _labelId,
  'type': 'flutter.widgets.SizedBox',
  'properties': {'width': _int(20), 'height': _int(20)},
  'slots': <String, Object?>{},
};
Map<String, Object?> _text({bool label = false}) => {
  'id': label ? _labelId : _childId,
  'type': 'flutter.widgets.Text',
  'properties': {'data': _string(label ? 'Badge label' : 'Badge child')},
  'slots': <String, Object?>{},
};
Map<String, Object?> _int(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _double(double value) => {
  'kind': 'double',
  'value': value,
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _color(String argb) => {'kind': 'color', 'argb': argb};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
Map<String, Object?> _padding({bool directional = false}) => {
  'kind': directional ? 'edgeInsetsDirectional' : 'edgeInsets',
  directional ? 'start' : 'left': 3,
  'top': 5,
  directional ? 'end' : 'right': 7,
  'bottom': 9,
};
Map<String, Object?> _alignment({
  bool directional = false,
  double x = 0,
  double y = 0,
}) => {
  'kind': 'alignmentGeometry',
  'basis': directional ? 'directional' : 'physical',
  'horizontal': x,
  'vertical': y,
};
Map<String, Object?> _offset(double x, double y) => {
  'kind': 'offset',
  'dx': x,
  'dy': y,
};
Map<String, Object?> _paint({bool theme = true}) => {
  'kind': 'paint',
  'color': theme
      ? {'kind': 'theme', 'token': 'material.colorScheme.primary'}
      : {'kind': 'literal', 'argb': '0x44123456'},
  'blendMode': 'srcOver',
  'style': 'stroke',
  'strokeWidth': 1.5,
  'strokeCap': 'round',
  'strokeJoin': 'bevel',
  'strokeMiterLimit': 4.0,
  'antiAlias': true,
  'filterQuality': 'medium',
  'invertColors': false,
};
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode _node(CanvasModel model) =>
    model.root.slot('body')!.children.single.slot('child')!.children.single;
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _badge() =>
    find.descendant(of: _widget(_id), matching: find.byType(Badge));
double _scale(WidgetTester tester) =>
    tester.getRect(_widget(_childId)).width /
    tester.getSize(_widget(_childId)).width;
Finder _badgeContainer() => find.descendant(
  of: _badge(),
  matching: find.byWidgetPredicate(
    (widget) => widget is Container && widget.decoration is ShapeDecoration,
  ),
);
ShapeDecoration _decoration(WidgetTester tester) =>
    tester.widget<Container>(_badgeContainer()).decoration! as ShapeDecoration;
EdgeInsetsGeometry? _badgePadding(WidgetTester tester) =>
    tester.widget<Container>(_badgeContainer()).padding;
TextStyle _labelStyle(WidgetTester tester) {
  final label = find.descendant(of: _badge(), matching: find.byType(Text));
  return DefaultTextStyle.of(tester.element(label.first)).style;
}

CanvasDropTarget? _resolve(
  WidgetTester tester,
  CanvasDropResolver resolver,
  Offset point,
) {
  final surface = tester.getRect(find.byType(CanvasDocumentView));
  return resolver(
    ((point.dx - surface.left) / surface.width * 1000000).round(),
    ((point.dy - surface.top) / surface.height * 1000000).round(),
    CanvasPaletteDragSource(
      token: 'text',
      widgetType: 'flutter.widgets.Text',
      traits: const {},
    ),
  );
}

Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  TextDirection direction = TextDirection.ltr,
  double textScale = 1,
  String selectedId = _id,
  List<String>? selected,
  List<String>? commits,
  void Function(CanvasDropResolver?)? onDrop,
  void Function(CanvasMovePreviewResolver?)? onMove,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: Directionality(
        textDirection: direction,
        child: MediaQuery(
          data: MediaQueryData(textScaler: TextScaler.linear(textScale)),
          child: CanvasDocumentView(
            model: _decode(model),
            selectedWidgetId: selectedId,
            onSelected: selected?.add ?? (_) {},
            inlineTextEditEnabled: true,
            onInlineTextCommit: (id, text, _) {
              commits?.add('$id:$text');
              return true;
            },
            onDropResolverChanged: onDrop,
            onMovePreviewResolverChanged: onMove,
          ),
        ),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
}

String _semanticsLabels(WidgetTester tester) {
  final labels = <String>[];
  void visit(SemanticsNode node) {
    if (!node.isMergedIntoParent) labels.add(node.label);
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(
    tester.binding.renderViews.single.owner!.semanticsOwner!.rootSemanticsNode!,
  );
  return labels.join('\n');
}

class _LifecycleProbe extends StatefulWidget {
  const _LifecycleProbe({required this.events});
  final List<String> events;
  @override
  State<_LifecycleProbe> createState() => _LifecycleProbeState();
}

class _LifecycleProbeState extends State<_LifecycleProbe> {
  @override
  void initState() {
    super.initState();
    widget.events.add('init');
  }

  @override
  void dispose() {
    widget.events.add('dispose');
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => const SizedBox(width: 100, height: 60);
}
