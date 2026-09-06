import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Card';
const _id = 'ba5e64c0-3712-45c3-9e8e-ae0cd79c4561';
const _childId = 'c36d9b31-1439-447c-bac1-b86911242036';
const _rowId = 'a1989523-4708-455e-9556-3bb997ae9c4f';
const _variants = ['elevated', 'filled', 'outlined'];
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
const _presence = {'kind': 'dartObjectReferencePresence'};

void main() {
  test(
    'Card exact 31-property schema has only required variant and one optional ordinary child',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final block = contract.substring(
        start,
        contract.indexOf('W|flutter.material.Divider\n', start),
      );
      expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(31));
      expect(block, contains('P|variant|string|1|string:ZWxldmF0ZWQ|'));
      expect(block, contains('P|color|color,themeToken|0|-|-|'));
      expect(
        block,
        contains(
          'P|shapeRadius|borderRadius|0|-|-|borderRadius:borderRadius:v1:physical,directional:finiteNonNegative',
        ),
      );
      expect(
        block,
        contains(
          'P|shape|dartObjectReference|0|-|-|dartObjectReference:dartObjectReference:v1:ShapeBorder:',
        ),
      );
      expect(block, contains('S|child|single|0|0|1|any'));
      expect(block, isNot(contains('C|')));
      final reviewedLines = canvasReviewedWidgetSchemaContract.trimLeft().split(
        '\n',
      );
      final runtimeLines = contract.split('\n');
      for (
        var index = 0;
        index < math.min(reviewedLines.length, runtimeLines.length);
        index++
      ) {
        expect(
          reviewedLines[index],
          runtimeLines[index],
          reason: 'contract line $index',
        );
      }
      expect(reviewedLines.length, runtimeLines.length);
      expect(canvasModelProtocolVersion, 18);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), false);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(canvasDropSlotsForWidgetType(_type).single.slotName, 'child');
      expect(_node(_decode(_model())).properties.keys, ['variant']);
      expect(_decode(_model()).imageResourceIds, isEmpty);
    },
  );

  test(
    'Card required variant rejects omission null incorrect kind and unknown constructors',
    () {
      final missing = _model();
      (_leaf(missing)['properties'] as Map).remove('variant');
      expect(() => _decode(missing), throwsFormatException);
      for (final value in [
        null,
        {'kind': 'null'},
        _bool(false),
        _string(''),
        _string('Card.filled'),
        _string('Filled'),
        _string('other'),
      ]) {
        expect(
          () => _decode(_model(properties: {'variant': value})),
          throwsFormatException,
        );
      }
      for (final variant in _variants) {
        final model = _model(properties: {'variant': _string(variant)});
        for (final slots in <Map<String, Object?>>[
          {},
          {
            'child': {'kind': 'single', 'child': null},
          },
          {
            'child': {'kind': 'single', 'child': _text()},
          },
        ]) {
          _leaf(model)['slots'] = slots;
          expect(() => _decode(model), returnsNormally);
        }
        for (final slots in [
          {
            'child': {'kind': 'list', 'children': []},
          },
          {
            'children': {'kind': 'list', 'children': []},
          },
          {
            'body': {'kind': 'single', 'child': null},
          },
        ]) {
          _leaf(model)['slots'] = slots;
          expect(() => _decode(model), throwsFormatException);
        }
      }
    },
  );

  test(
    'Card scalar optional states are preserved across all three variants without materialized SDK defaults',
    () {
      for (final variant in _variants) {
        for (final foreground in [null, false, true]) {
          for (final semantic in [null, false, true]) {
            for (final clip in <Clip?>[null, ...Clip.values]) {
              for (final color in [
                null,
                _color('0x80123456'),
                _theme('material.colorScheme.primary'),
              ]) {
                final properties = <String, Object?>{
                  'variant': _string(variant),
                  if (foreground != null)
                    'borderOnForeground': _bool(foreground),
                  if (semantic != null) 'semanticContainer': _bool(semantic),
                  if (clip != null) 'clipBehavior': _enum('Clip', clip.name),
                  'color': ?color,
                };
                expect(
                  _node(
                    _decode(_model(properties: properties)),
                  ).properties.keys,
                  unorderedEquals(properties.keys),
                );
              }
            }
          }
        }
      }
      for (final name in [
        'borderOnForeground',
        'semanticContainer',
        'margin',
        'clipBehavior',
        'color',
        'shadowColor',
        'surfaceTintColor',
        'shapeRadius',
        'elevation',
      ]) {
        for (final value in [
          null,
          {'kind': 'null'},
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
          );
        }
      }
      for (final name in [
        'key',
        'child',
        'shapeBuilder',
        'onTap',
        'width',
        'height',
        'padding',
        'shapeBorder',
      ]) {
        expect(
          () => _decode(_model(properties: {name: _bool(true)})),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Card all ten shape families admit exactly their applicable typed leaves',
    () {
      final details = _allShapeDetails();
      for (final kind in _shapes) {
        final allowed = _allowed(kind);
        final all = <String, Object?>{'shapeKind': _string(kind)};
        for (final entry in details.entries) {
          final properties = {
            'shapeKind': _string(kind),
            entry.key: entry.value,
          };
          expect(
            () => _decode(_model(properties: properties)),
            allowed.contains(entry.key)
                ? returnsNormally
                : throwsFormatException,
            reason: '$kind/${entry.key}',
          );
          if (allowed.contains(entry.key)) all[entry.key] = entry.value;
        }
        expect(
          () => _decode(_model(properties: all)),
          returnsNormally,
          reason: kind,
        );
      }
      for (final entry in details.entries) {
        expect(
          () => _decode(_model(properties: {entry.key: entry.value})),
          throwsFormatException,
          reason: entry.key,
        );
      }
      expect(
        () => _decode(_model(properties: {'shapeKind': _string('rectangle')})),
        throwsFormatException,
      );
    },
  );

  test(
    'Card custom ShapeBorder crosses the presence-only boundary and cannot mix any built-in field',
    () {
      expect(
        () => _decode(_model(properties: {'shape': _presence})),
        returnsNormally,
      );
      for (final entry in {
        'shapeKind': _string('circle'),
        ..._allShapeDetails(),
      }.entries) {
        expect(
          () => _decode(
            _model(properties: {'shape': _presence, entry.key: entry.value}),
          ),
          throwsFormatException,
        );
      }
      for (final shape in [
        null,
        {'kind': 'null'},
        _string('projectShape'),
        {'kind': 'dartObjectReference'},
        {'kind': 'dartObjectReferencePresence', 'symbol': 'projectShape'},
        {'kind': 'dartObjectReferencePresence', 'invoke': true},
      ]) {
        expect(
          () => _decode(_model(properties: {'shape': shape})),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Card numeric shape domains preserve integers fractional values and SDK rounding dependency',
    () {
      final nonnegative = {'elevation', 'shapeSideWidth'};
      final unit = {
        'shapeCircleEccentricity',
        'shapeInnerRadiusRatio',
        'shapePointRounding',
        'shapeValleyRounding',
        'shapeSquash',
        ..._edgeNames.where((name) => name.endsWith('Size')),
      };
      final signed = {
        'shapeSideStrokeAlign',
        'shapeRotation',
        ..._edgeNames.where((name) => name.endsWith('Alignment')),
      };
      for (final name in {...nonnegative, ...unit, ...signed, 'shapePoints'}) {
        final kind = name == 'shapeCircleEccentricity'
            ? 'circle'
            : _edgeNames.contains(name)
            ? 'linear'
            : 'star';
        final prefix = name == 'elevation'
            ? <String, Object?>{}
            : {'shapeKind': _string(kind)};
        for (final value in [
          null,
          {'kind': 'null'},
          _bool(true),
          _string('1'),
          {'kind': 'integer', 'value': 1.25},
          {'kind': 'double', 'value': 1, 'extra': true},
        ]) {
          expect(
            () => _decode(_model(properties: {...prefix, name: value})),
            throwsFormatException,
          );
        }
        final valid = name == 'shapePoints'
            ? [_int(2), _double(2.5), _int(4097), _double(1e100)]
            : unit.contains(name)
            ? [_int(0), _int(1), _double(.25)]
            : signed.contains(name)
            ? [_int(-3), _double(-7.5), _double(30)]
            : [_int(0), _double(1.25)];
        for (final value in valid) {
          expect(
            () => _decode(_model(properties: {...prefix, name: value})),
            returnsNormally,
            reason: name,
          );
        }
        for (final value
            in name == 'shapePoints'
                ? [_int(1), _double(1.99)]
                : unit.contains(name)
                ? [_double(-.1), _double(1.01)]
                : nonnegative.contains(name)
                ? [_int(-1), _double(-.1)]
                : <Map<String, Object?>>[]) {
          expect(
            () => _decode(_model(properties: {...prefix, name: value})),
            throwsFormatException,
          );
        }
        final json = jsonEncode(
          _model(properties: {...prefix, name: _double(12345.25)}),
        );
        expect(
          () => CanvasModel.decode(
            Uint8List.fromList(
              utf8.encode(json.replaceFirst('12345.25', '1e309')),
            ),
          ),
          throwsFormatException,
        );
      }
      expect(
        () => _decode(
          _model(
            properties: {
              'shapeKind': _string('star'),
              'shapePointRounding': _double(.5),
              'shapeValleyRounding': _double(.5),
            },
          ),
        ),
        returnsNormally,
      );
      expect(
        () => _decode(
          _model(
            properties: {
              'shapeKind': _string('star'),
              'shapePointRounding': _double(.51),
              'shapeValleyRounding': _double(.5),
            },
          ),
        ),
        throwsFormatException,
      );
    },
  );

  test(
    'Card theme colors margin geometry and elliptical radii use closed existing value envelopes',
    () {
      for (final name in [
        'color',
        'shadowColor',
        'surfaceTintColor',
        'shapeSideColor',
      ]) {
        for (final token in canvasColorSchemeThemeTokens) {
          expect(
            () => _decode(
              _model(
                properties: {
                  if (name == 'shapeSideColor') 'shapeKind': _string('circle'),
                  name: _theme(token),
                },
              ),
            ),
            returnsNormally,
          );
        }
        expect(
          () => _decode(
            _model(
              properties: {
                if (name == 'shapeSideColor') 'shapeKind': _string('circle'),
                name: _theme('material.textTheme.bodyLarge'),
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final directional in [false, true]) {
        expect(
          () => _decode(
            _model(
              properties: {
                'margin': _margin(directional: directional),
                'shapeKind': _string('roundedSuperellipse'),
                'shapeRadius': _radius(directional: directional),
              },
            ),
          ),
          returnsNormally,
        );
        final badMargin = _margin(directional: directional);
        badMargin['top'] = -1;
        expect(
          () => _decode(_model(properties: {'margin': badMargin})),
          throwsFormatException,
        );
        final badRadius = _radius(directional: directional);
        ((badRadius['geometry'] as Map)[directional ? 'topStart' : 'topLeft']
                as Map)['x'] =
            -1;
        expect(
          () => _decode(
            _model(
              properties: {
                'shapeKind': _string('roundedRectangle'),
                'shapeRadius': badRadius,
              },
            ),
          ),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Card accepts one arbitrary child and can be inserted or wrapped without becoming a required wrapper',
    () {
      for (final wrapper in [
        'flutter.widgets.Visibility',
        'flutter.widgets.IconTheme',
        'flutter.widgets.Expanded',
      ]) {
        expect(
          canvasWrapperAcceptsExistingChild(
            wrapperWidgetType: wrapper,
            childWidgetType: _type,
          ),
          true,
        );
      }
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: _type,
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: _source('flutter.widgets.Text'),
        ),
        true,
      );
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: _type,
          slotName: 'child',
          currentChildCount: 1,
          insertionIndex: 0,
          source: _source('flutter.widgets.Text'),
        ),
        false,
      );
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.Row',
          slotName: 'children',
          currentChildCount: 0,
          insertionIndex: 0,
          source: _source(_type),
        ),
        true,
      );
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.material.Scaffold',
          slotName: 'appBar',
          currentChildCount: 0,
          insertionIndex: 0,
          source: _source(_type),
        ),
        false,
      );
    },
  );

  for (final platform in ['windows', 'web']) {
    for (final material3 in [false, true]) {
      testWidgets(
        'Card all constructors retain SDK theme defaults for Material ${material3 ? 3 : 2} on $platform',
        (tester) async {
          final theme = ThemeData(
            useMaterial3: material3,
            cardColor: const Color(0xFF010203),
            shadowColor: const Color(0xFF040506),
            colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal).copyWith(
              surfaceContainerLow: const Color(0xFF111111),
              surfaceContainerHighest: const Color(0xFF222222),
              surface: const Color(0xFF333333),
              shadow: const Color(0xFF444444),
              outlineVariant: const Color(0xFF555555),
            ),
          );
          for (final variant in _variants) {
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: {'variant': _string(variant)},
                child: _box(),
              ),
              theme: theme,
            );
            final card = tester.widget<Card>(_card());
            expect([
              card.color,
              card.shadowColor,
              card.surfaceTintColor,
              card.elevation,
              card.shape,
              card.margin,
              card.clipBehavior,
            ], everyElement(isNull));
            expect(card.borderOnForeground, true);
            expect(card.semanticContainer, true);
            final material = _material(tester);
            expect(material.type, MaterialType.card);
            expect(
              material.elevation,
              !material3 || variant == 'elevated' ? 1 : 0,
            );
            expect(
              material.color,
              !material3
                  ? theme.cardColor
                  : variant == 'elevated'
                  ? theme.colorScheme.surfaceContainerLow
                  : variant == 'filled'
                  ? theme.colorScheme.surfaceContainerHighest
                  : theme.colorScheme.surface,
            );
            expect(
              material.shadowColor,
              material3 ? theme.colorScheme.shadow : theme.shadowColor,
            );
            expect(
              material.surfaceTintColor,
              material3 ? Colors.transparent : null,
            );
            final shape = material.shape! as RoundedRectangleBorder;
            expect(
              shape.borderRadius,
              BorderRadius.circular(material3 ? 12 : 4),
            );
            expect(
              shape.side,
              material3 && variant == 'outlined'
                  ? BorderSide(color: theme.colorScheme.outlineVariant)
                  : BorderSide.none,
            );
            expect(material.clipBehavior, Clip.none);
            expect(tester.getSize(_card()), const Size(108, 68));
            expect(tester.takeException(), isNull);
          }
        },
      );
    }

    testWidgets(
      'Card all variants and ten built-in shape families bind actual SDK objects and default sides on $platform',
      (tester) async {
        for (final variant in _variants) {
          for (final kind in _shapes) {
            await _pump(
              tester,
              _model(
                platform: platform,
                child: _box(),
                properties: {
                  'variant': _string(variant),
                  'shapeKind': _string(kind),
                },
              ),
            );
            final shape = tester.widget<Card>(_card()).shape! as OutlinedBorder;
            expect(
              shape.runtimeType,
              _shapeType(kind),
              reason: '$variant/$kind',
            );
            expect(shape.side, BorderSide.none);
            if (shape is CircleBorder) {
              expect(shape.eccentricity, kind == 'oval' ? 1 : 0);
            }
            if (shape is LinearBorder) {
              expect([
                shape.start,
                shape.end,
                shape.top,
                shape.bottom,
              ], everyElement(isNull));
            }
            if (shape is StarBorder) {
              expect(shape.points, 5);
              expect(
                shape.innerRadiusRatio,
                closeTo(kind == 'star' ? .4 : math.cos(math.pi / 5), 1e-12),
              );
              expect(shape.valleyRounding, 0);
            }
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'Card all shape leaves including signed stroke angles linear edges and fractional stars survive actual rendering on $platform',
      (tester) async {
        for (final kind in _shapes) {
          final properties = {
            'shapeKind': _string(kind),
            for (final entry in _allShapeDetails().entries)
              if (_allowed(kind).contains(entry.key)) entry.key: entry.value,
          };
          await _pump(
            tester,
            _model(platform: platform, child: _box(), properties: properties),
          );
          final shape = tester.widget<Card>(_card()).shape! as OutlinedBorder;
          expect(
            shape.side,
            const BorderSide(
              color: Color(0x80123456),
              width: 3.5,
              strokeAlign: .25,
            ),
          );
          if (shape is CircleBorder) expect(shape.eccentricity, .25);
          if (shape is StarBorder) {
            expect(shape.points, 5.5);
            expect(shape.pointRounding, .2);
            expect(shape.valleyRounding, kind == 'star' ? .3 : 0);
            expect(
              shape.innerRadiusRatio,
              closeTo(kind == 'star' ? .6 : math.cos(math.pi / 5.5), 1e-12),
            );
            expect(shape.rotation, closeTo(-27.5, 1e-12));
            expect(shape.squash, .75);
          }
          if (shape is LinearBorder) {
            for (final edge in [
              shape.start!,
              shape.end!,
              shape.top!,
              shape.bottom!,
            ]) {
              expect(edge.size, .5);
              expect(edge.alignment, -.75);
            }
          }
          expect(tester.takeException(), isNull, reason: kind);
        }
      },
    );

    testWidgets(
      'Card theme values apply to every variant and local false zero transparent shape clip and margin override them on $platform',
      (tester) async {
        final theme = ThemeData(
          cardTheme: const CardThemeData(
            color: Colors.green,
            shadowColor: Colors.blue,
            surfaceTintColor: Colors.red,
            elevation: 7,
            shape: StadiumBorder(
              side: BorderSide(color: Colors.amber, width: 2),
            ),
            clipBehavior: Clip.antiAlias,
            margin: EdgeInsets.all(10),
          ),
        );
        for (final variant in _variants) {
          await _pump(
            tester,
            _model(
              platform: platform,
              child: _box(),
              properties: {'variant': _string(variant)},
            ),
            theme: theme,
          );
          final inherited = _material(tester);
          expect(inherited.color, Colors.green);
          expect(inherited.shadowColor, Colors.blue);
          expect(inherited.surfaceTintColor, Colors.red);
          expect(inherited.elevation, 7);
          expect(inherited.shape, theme.cardTheme.shape);
          expect(inherited.clipBehavior, Clip.antiAlias);
          expect(tester.getSize(_card()), const Size(120, 80));
          final element = tester.element(_widget(_childId));
          await _pump(
            tester,
            _model(
              platform: platform,
              child: _box(),
              properties: {
                'variant': _string(variant),
                'color': _color('0x00000000'),
                'shadowColor': _color('0x80123456'),
                'surfaceTintColor': _color('0x00000000'),
                'elevation': _int(0),
                'borderOnForeground': _bool(false),
                'semanticContainer': _bool(false),
                'margin': _margin(zero: true),
                'clipBehavior': _enum('Clip', 'none'),
                'shapeKind': _string('circle'),
              },
            ),
            theme: theme,
          );
          final local = _material(tester);
          expect(local.color, Colors.transparent);
          expect(local.shadowColor, const Color(0x80123456));
          expect(local.surfaceTintColor, Colors.transparent);
          expect(local.elevation, 0);
          expect(local.borderOnForeground, false);
          expect(local.clipBehavior, Clip.none);
          expect(local.shape, const CircleBorder());
          expect(tester.widget<Card>(_card()).semanticContainer, false);
          expect(tester.getSize(_card()), const Size(100, 60));
          expect(identical(tester.element(_widget(_childId)), element), true);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'Card RTL resolves directional margin and elliptical corners without changing physical shapes on $platform',
      (tester) async {
        for (final direction in TextDirection.values) {
          for (final directional in [false, true]) {
            for (final kind in _radiusKinds) {
              final model = _model(
                platform: platform,
                child: _box(),
                properties: {
                  'margin': _margin(directional: directional),
                  'shapeKind': _string(kind),
                  'shapeRadius': _radius(directional: directional),
                  'shapeSideWidth': _int(2),
                  'clipBehavior': _enum('Clip', 'antiAlias'),
                },
              );
              _wrapDirection(model, direction);
              await _pump(tester, model);
              final card = tester.widget<Card>(_card());
              final margin = card.margin!.resolve(direction);
              expect(
                margin.left,
                directional && direction == TextDirection.rtl ? 7 : 3,
              );
              final shape = card.shape!;
              final radius = switch (shape) {
                RoundedRectangleBorder() => shape.borderRadius,
                RoundedSuperellipseBorder() => shape.borderRadius,
                BeveledRectangleBorder() => shape.borderRadius,
                ContinuousRectangleBorder() => shape.borderRadius,
                _ => throw StateError('$shape'),
              };
              expect(
                radius.resolve(direction).topLeft,
                directional && direction == TextDirection.rtl
                    ? const Radius.elliptical(7, 8)
                    : const Radius.elliptical(5, 6),
              );
              final physical = tester.renderObject<RenderPhysicalShape>(
                find.descendant(
                  of: _card(),
                  matching: find.byType(PhysicalShape),
                ),
              );
              expect(physical.clipBehavior, Clip.antiAlias);
              expect(
                (physical.clipper! as ShapeBorderClipper).textDirection,
                direction,
              );
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    testWidgets(
      'Card clip choices and foreground border use real Material painters with stable child state on $platform',
      (tester) async {
        Element? retained;
        for (final clip in Clip.values) {
          for (final foreground in [false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                child: _box(),
                properties: {
                  'clipBehavior': _enum('Clip', clip.name),
                  'borderOnForeground': _bool(foreground),
                  'shapeKind': _string('star'),
                  'shapeSideWidth': _int(4),
                },
              ),
            );
            expect(_material(tester).clipBehavior, clip);
            expect(_material(tester).borderOnForeground, foreground);
            final physical = tester.renderObject<RenderPhysicalShape>(
              find.descendant(
                of: _card(),
                matching: find.byType(PhysicalShape),
              ),
            );
            expect(physical.clipBehavior, clip);
            final paints = tester
                .widgetList<CustomPaint>(
                  find.descendant(
                    of: _card(),
                    matching: find.byType(CustomPaint),
                  ),
                )
                .where(
                  (paint) => '${paint.painter ?? paint.foregroundPainter}'
                      .contains('ShapeBorderPainter'),
                )
                .toList();
            expect(paints, hasLength(1));
            expect(paints.single.foregroundPainter != null, foreground);
            expect(paints.single.painter != null, !foreground);
            final element = tester.element(_widget(_childId));
            if (retained != null) expect(identical(element, retained), true);
            retained = element;
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'Card semanticContainer toggles SDK semantic boundaries while retaining labels pointer selection and rejecting own F2 on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          final selected = <String>[];
          for (final semantic in [false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                child: _text(),
                properties: {
                  'semanticContainer': _bool(semantic),
                  'margin': _margin(zero: true),
                },
              ),
              selected: selected,
            );
            final cardSemantics = tester
                .widgetList<Semantics>(
                  find.descendant(
                    of: _card(),
                    matching: find.byType(Semantics),
                  ),
                )
                .toList();
            expect(cardSemantics.first.container, semantic);
            expect(
              cardSemantics.any((node) => node.explicitChildNodes == !semantic),
              true,
            );
            expect(
              _labels(tester).any((label) => label.contains('Card child text')),
              true,
            );
            await tester.tap(_widget(_childId));
            expect(selected, contains(_childId));
            await tester.sendKeyEvent(LogicalKeyboardKey.f2);
            await tester.pump();
            expect(find.byType(EditableText), findsNothing);
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'Card empty child and zero margins expose selectable empty slot and do not force a default child on $platform',
      (tester) async {
        final selected = <String>[];
        CanvasDropResolver? dropResolver;
        final model = _model(
          platform: platform,
          properties: {'margin': _margin(zero: true)},
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(model),
            selectedWidgetId: _id,
            onSelected: selected.add,
            onDropResolverChanged: (value) => dropResolver = value,
            inlineTextEditEnabled: true,
          ),
        );
        await tester.pump();
        expect(tester.widget<Card>(_card()).child, isNull);
        expect(tester.getSize(_card()), Size.zero);
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$_id'),
        );
        expect(handle, findsOneWidget);
        await tester.tap(handle);
        await tester.pump();
        expect(selected, contains(_id));
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        expect(find.byType(EditableText), findsNothing);
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final point = tester.getCenter(handle);
        final target = dropResolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          _source('flutter.widgets.Text'),
        );
        expect(target?.parentWidgetId, _id);
        expect(target?.slotName, 'child');
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Card custom reference and huge star polygon display honest diagnostic while preserving child interaction on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final properties in <Map<String, Object?>>[
            {'shape': _presence},
            for (final kind in ['star', 'polygon'])
              {'shapeKind': _string(kind), 'shapePoints': _double(1e100)},
          ]) {
            final selected = <String>[];
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: properties,
                child: _text(),
              ),
              selected: selected,
            );
            expect(
              _card(),
              findsNothing,
              reason: 'Do not substitute a default Card or expensive SDK shape',
            );
            expect(_widget(_childId), findsOneWidget);
            expect(find.text('Card child text'), findsOneWidget);
            final messages = _labels(
              tester,
            ).where((label) => label.contains('preview unavailable')).join(' ');
            expect(messages, contains('Card.shape'));
            expect(
              messages,
              properties.containsKey('shape')
                  ? contains('does not execute project')
                  : contains('4096'),
            );
            expect(messages, contains('ShapeBorder'));
            await tester.tap(_widget(_childId));
            expect(selected, contains(_childId));
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'Card custom and complexity diagnostics remain reachable for an absent child and clear after selecting a built-in on $platform',
      (tester) async {
        for (final properties in <Map<String, Object?>>[
          {'shape': _presence},
          {'shapeKind': _string('star'), 'shapePoints': _int(4097)},
        ]) {
          await _pump(
            tester,
            _model(platform: platform, properties: properties),
          );
          final handle = find.byKey(
            const ValueKey('canvas-zero-size-widget-target-$_id'),
          );
          expect(handle, findsOneWidget);
          final tips = tester.widgetList<Tooltip>(
            find.ancestor(of: handle, matching: find.byType(Tooltip)),
          );
          expect(
            tips.any(
              (tip) => tip.message?.contains('preview unavailable') ?? false,
            ),
            true,
          );
          await _pump(
            tester,
            _model(
              platform: platform,
              child: _box(),
              properties: {'shapeKind': _string('roundedRectangle')},
            ),
          );
          expect(_card(), findsOneWidget);
          expect(find.text('Card shape\npreview unavailable'), findsNothing);
          expect(tester.takeException(), isNull);
        }
      },
    );
  }

  for (final platform in ['windows', 'web']) {
    for (final preview in [
      'ClipPath.shape',
      'ClipPath.clipper',
      'PhysicalShape',
      'Card.shape',
      'Card.complexity',
    ]) {
      testWidgets(
        'Unavailable $preview overlay preserves real child clicks F2 focus and accessible reason on $platform',
        (tester) async {
          final semantics = tester.ensureSemantics();
          try {
            final model = _model(platform: platform, child: _text());
            final node = _leaf(model);
            node['type'] = preview.startsWith('ClipPath')
                ? 'flutter.widgets.ClipPath'
                : preview == 'PhysicalShape'
                ? 'flutter.widgets.PhysicalShape'
                : _type;
            node['properties'] = switch (preview) {
              'ClipPath.shape' => <String, Object?>{'shape': _presence},
              'ClipPath.clipper' => <String, Object?>{'clipper': _presence},
              'PhysicalShape' => <String, Object?>{
                'clipper': _presence,
                'color': _color('0xFF123456'),
              },
              'Card.shape' => <String, Object?>{
                'variant': _string('filled'),
                'shape': _presence,
              },
              _ => <String, Object?>{
                'variant': _string('outlined'),
                'shapeKind': _string('polygon'),
                'shapePoints': _int(4097),
              },
            };
            final selections = <String>[];
            final commits = <String>[];
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(model),
                selectedWidgetId: _childId,
                onSelected: selections.add,
                inlineTextEditEnabled: true,
                onInlineTextCommit: (id, text, _) {
                  expect(id, _childId);
                  commits.add(text);
                  return true;
                },
              ),
            );
            await tester.pump();
            await tester.tap(_widget(_childId));
            await tester.pump(const Duration(milliseconds: 300));
            expect(selections, contains(_childId));
            expect(
              _labels(
                tester,
              ).any((label) => label.contains('preview unavailable')),
              true,
            );
            final tooltip = find.ancestor(
              of: _widget(_childId),
              matching: find.byType(Tooltip),
            );
            expect(
              tester
                  .widgetList<Tooltip>(tooltip)
                  .any(
                    (tip) =>
                        tip.message?.contains('preview unavailable') ?? false,
                  ),
              true,
            );
            await tester.sendKeyEvent(LogicalKeyboardKey.f2);
            await tester.pump();
            expect(find.byType(EditableText), findsOneWidget);
            expect(
              tester
                  .widget<EditableText>(find.byType(EditableText))
                  .focusNode
                  .hasFocus,
              true,
            );
            await tester.enterText(find.byType(TextField), 'Updated child');
            await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
            await tester.sendKeyEvent(LogicalKeyboardKey.enter);
            await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
            await tester.pump();
            expect(commits, ['Updated child']);
            expect(tester.takeException(), isNull);
          } finally {
            semantics.dispose();
          }
        },
      );
    }

    testWidgets(
      'Card SDK shape bounds child hit testing even for Clip.none while keeping the card selectable on $platform',
      (tester) async {
        for (final clip in [
          Clip.none,
          Clip.hardEdge,
          Clip.antiAliasWithSaveLayer,
        ]) {
          var rawTaps = 0;
          const rawKey = ValueKey('raw-card-child');
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: Card(
                  shape: const CircleBorder(),
                  clipBehavior: clip,
                  margin: EdgeInsets.zero,
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () => rawTaps++,
                    child: const SizedBox(key: rawKey, width: 100, height: 60),
                  ),
                ),
              ),
            ),
          );
          final rawRect = tester.getRect(find.byKey(rawKey));
          await tester.tapAt(rawRect.topLeft + const Offset(2, 2));
          await tester.pump();
          expect(
            rawTaps,
            0,
            reason:
                'RenderPhysicalShape hitTest checks shape regardless of paint clipBehavior',
          );
          await tester.tapAt(rawRect.center);
          await tester.pump();
          expect(rawTaps, 1);
          final selected = <String>[];
          await _pump(
            tester,
            _model(
              platform: platform,
              child: _box(),
              properties: {
                'shapeKind': _string('circle'),
                'margin': _margin(zero: true),
                'clipBehavior': _enum('Clip', clip.name),
              },
            ),
            selected: selected,
          );
          final rect = tester.getRect(_widget(_childId));
          await tester.tapAt(rect.topLeft + const Offset(2, 2));
          await tester.pump();
          expect(selected.last, _id);
          selected.clear();
          await tester.tapAt(rect.center);
          await tester.pump();
          expect(selected.last, _childId);
          expect(tester.takeException(), isNull);
        }
      },
    );
  }

  testWidgets(
    'Card semantic surface and side colors update from nearest Theme without replacing the child',
    (tester) async {
      Element? retained;
      for (final primary in [Colors.green, Colors.deepOrange]) {
        final model = _model(
          child: _box(),
          properties: {
            'color': _theme('material.colorScheme.primary'),
            'shadowColor': _theme('material.colorScheme.primary'),
            'surfaceTintColor': _theme('material.colorScheme.primary'),
            'shapeKind': _string('roundedRectangle'),
            'shapeSideColor': _theme('material.colorScheme.primary'),
          },
        );
        await _pump(
          tester,
          model,
          theme: ThemeData(
            colorScheme: ColorScheme.fromSeed(
              seedColor: primary,
            ).copyWith(primary: primary),
          ),
        );
        final material = _material(tester);
        expect([
          material.color,
          material.shadowColor,
          material.surfaceTintColor,
          (material.shape! as OutlinedBorder).side.color,
        ], everyElement(primary));
        final element = tester.element(_widget(_childId));
        if (retained != null) expect(identical(element, retained), true);
        retained = element;
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'Card StarBorder preview budget accepts the exact 4096 boundary and fractional points without narrowing the model',
    (tester) async {
      for (final kind in ['star', 'polygon']) {
        for (final points in [2.5, 4096.0]) {
          await _pump(
            tester,
            _model(
              child: _box(),
              properties: {
                'shapeKind': _string(kind),
                'shapePoints': _double(points),
              },
            ),
          );
          expect(
            (tester.widget<Card>(_card()).shape! as StarBorder).points,
            points,
          );
          expect(tester.takeException(), isNull);
        }
        final model = _model(
          properties: {
            'shapeKind': _string(kind),
            'shapePoints': _double(4096.25),
          },
        );
        expect(() => _decode(model), returnsNormally);
        await _pump(tester, model);
        expect(_card(), findsNothing);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'Card LinearBorder creates an edge only when one of its leaves is present and preserves extrapolated alignment',
    (tester) async {
      for (final edge in ['Start', 'End', 'Top', 'Bottom']) {
        for (final mode in ['size', 'alignment', 'both']) {
          await _pump(
            tester,
            _model(
              child: _box(),
              properties: {
                'shapeKind': _string('linear'),
                if (mode != 'alignment') 'shape${edge}Size': _double(.5),
                if (mode != 'size') 'shape${edge}Alignment': _double(2.5),
                'shapeSideWidth': _int(2),
              },
            ),
          );
          final shape = tester.widget<Card>(_card()).shape! as LinearBorder;
          final edges = {
            'Start': shape.start,
            'End': shape.end,
            'Top': shape.top,
            'Bottom': shape.bottom,
          };
          expect(edges.values.whereType<LinearBorderEdge>(), hasLength(1));
          expect(edges[edge]!.size, mode == 'alignment' ? 1 : .5);
          expect(edges[edge]!.alignment, mode == 'size' ? 0 : 2.5);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  testWidgets(
    'Card shape side style none and zero-width hairline remain real SDK values for all ten families',
    (tester) async {
      for (final kind in _shapes) {
        for (final none in [false, true]) {
          await _pump(
            tester,
            _model(
              child: _box(),
              properties: {
                'shapeKind': _string(kind),
                'shapeSideWidth': _int(0),
                'shapeSideStyle': _enum('BorderStyle', none ? 'none' : 'solid'),
                if (_radiusKinds.contains(kind)) 'shapeRadius': _radius(),
                if (kind == 'linear') 'shapeTopSize': _int(1),
              },
            ),
          );
          final side =
              (tester.widget<Card>(_card()).shape! as OutlinedBorder).side;
          expect(side.width, 0);
          expect(side.style, none ? BorderStyle.none : BorderStyle.solid);
          expect(tester.takeException(), isNull, reason: '$kind/$none');
        }
      }
    },
  );

  testWidgets(
    'Card moving its child out keeps an editable empty card and selecting a variant preserves child element identity',
    (tester) async {
      CanvasMovePreviewResolver? resolver;
      final model = _model(child: _box());
      final cardNode = _leaf(model);
      _centerSlot(model)['child'] = {
        'id': _rowId,
        'type': 'flutter.widgets.Row',
        'properties': <String, Object?>{},
        'slots': {
          'children': {
            'kind': 'list',
            'children': [cardNode],
          },
        },
      };
      await tester.pumpWidget(
        CanvasModelApp(
          model: _decode(model),
          selectedWidgetId: _id,
          onSelected: (_) {},
          onMovePreviewResolverChanged: (value) => resolver = value,
        ),
      );
      await tester.pump();
      final target = resolver!(_childId, _rowId, 'children', 1);
      expect(target?.parentWidgetId, _rowId);
      expect(target?.slotName, 'children');
      expect(target?.insertionIndex, 1);
      final element = tester.element(_widget(_childId));
      (cardNode['properties'] as Map)['variant'] = _string('outlined');
      await tester.pumpWidget(
        CanvasModelApp(
          model: _decode(model),
          selectedWidgetId: _id,
          onSelected: (_) {},
        ),
      );
      await tester.pump(const Duration(milliseconds: 300));
      expect(identical(tester.element(_widget(_childId)), element), true);
      final moved = (((cardNode['slots'] as Map)['child'] as Map).remove(
        'child',
      ))!;
      ((cardNode['slots'] as Map)['child'] as Map)['child'] = null;
      final children =
          (((_centerSlot(model)['child'] as Map)['slots'] as Map)['children']
                  as Map)['children']
              as List;
      children.add(moved);
      await tester.pumpWidget(
        CanvasModelApp(
          model: _decode(model),
          selectedWidgetId: _id,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(tester.widget<Card>(_card()).child, isNull);
      expect(_widget(_childId), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}

Map<String, Object?> _allShapeDetails() => {
  'shapeRadius': _radius(),
  'shapeSideColor': _color('0x80123456'),
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
Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
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
                'properties': {'variant': _string('elevated'), ...properties},
                'slots': {
                  'child': {'kind': 'single', 'child': child},
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

void _wrapDirection(Map<String, Object?> model, TextDirection direction) {
  final leaf = _leaf(model);
  _centerSlot(model)['child'] = {
    'id': 'b145f1fa-0f3b-4834-bb42-d71df6f30bdd',
    'type': 'flutter.widgets.Directionality',
    'properties': {'textDirection': _enum('TextDirection', direction.name)},
    'slots': {
      'child': {'kind': 'single', 'child': leaf},
    },
  };
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
Map<String, Object?> _text() => {
  'id': _childId,
  'type': 'flutter.widgets.Text',
  'properties': {'data': _string('Card child text')},
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
Map<String, Object?> _color(String value) => {'kind': 'color', 'argb': value};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
Map<String, Object?> _margin({bool directional = false, bool zero = false}) => {
  'kind': directional ? 'edgeInsetsDirectional' : 'edgeInsets',
  directional ? 'start' : 'left': zero ? 0 : 3,
  'top': zero ? 0 : 5,
  directional ? 'end' : 'right': zero ? 0 : 7,
  'bottom': zero ? 0 : 9,
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
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode _node(CanvasModel model) =>
    model.root.slot('body')!.children.single.slot('child')!.children.single;
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _card() =>
    find.descendant(of: _widget(_id), matching: find.byType(Card));
Material _material(WidgetTester tester) => tester.widget<Material>(
  find.descendant(of: _card(), matching: find.byType(Material)),
);
CanvasPaletteDragSource _source(String type) => CanvasPaletteDragSource(
  token: type,
  widgetType: type,
  traits: canvasWidgetTraitsForType(type),
);
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: CanvasDocumentView(
        model: _decode(model),
        selectedWidgetId: _id,
        onSelected: selected?.add ?? (_) {},
        inlineTextEditEnabled: true,
        onInlineTextCommit: (_, _, _) =>
            throw StateError('Card must not edit text'),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
}

Set<String> _labels(WidgetTester tester) {
  final result = <String>{};
  void visit(SemanticsNode node) {
    if (!node.isMergedIntoParent) result.add(node.label);
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(
    tester.binding.renderViews.single.owner!.semanticsOwner!.rootSemanticsNode!,
  );
  return result;
}
