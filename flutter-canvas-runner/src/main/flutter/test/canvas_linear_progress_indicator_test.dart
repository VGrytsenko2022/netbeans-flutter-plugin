// ignore_for_file: deprecated_member_use

import 'dart:convert';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.LinearProgressIndicator';
const _id = '1934f733-85ab-40bb-8c7a-360cdd4fccfe';
const _boxId = 'ca8c98a2-0aa1-4e41-bd02-8c123ecdbdad';
const _outerId = 'd5d112bb-47fc-4706-8f50-e9b4aa97df29';
const _infinity = {'kind': 'enum', 'type': 'double', 'value': 'infinity'};
const _null = {'kind': 'null'};
const _reference = {'kind': 'dartObjectReferencePresence'};

void main() {
  test(
    'LinearProgressIndicator exact thirteen optional scalar fields have no child default or trait',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final block = contract.substring(
        start,
        contract.indexOf('W|', start + 3),
      );
      expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(13));
      expect(block, isNot(contains('S|')));
      expect(block, isNot(contains('C|')));
      expect(block, contains('dartObjectReference:v1:Animation<Color?>:'));
      expect(block, contains('dartObjectReference:v1:AnimationController:'));
      expect(canvasModelProtocolVersion, 18);
      final decoded = _node(_decode(_model()));
      expect(decoded.properties, isEmpty);
      expect(decoded.slots, isEmpty);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), false);
      for (final value in [
        _int(-9007199254740991),
        _int(0),
        _int(9007199254740991),
        _double(-1.7e308),
        _double(1.7e308),
      ]) {
        expect(
          () => _decode(
            _model(
              properties: {
                'value': value,
                'stopIndicatorRadius': value,
                'trackGap': value,
              },
            ),
          ),
          returnsNormally,
        );
      }
      for (final name in ['minHeight', 'stopIndicatorRadius', 'trackGap']) {
        expect(
          () => _decode(_model(properties: {name: _infinity})),
          returnsNormally,
        );
      }
      for (final value in [_int(1), _double(.00001), _double(1.7e308)]) {
        expect(
          () => _decode(_model(properties: {'minHeight': value})),
          returnsNormally,
        );
      }
      for (final color in [
        _color('0xFF010203'),
        _theme('material.colorScheme.primary'),
        _null,
        _reference,
      ]) {
        expect(
          () => _decode(_model(properties: {'valueColor': color})),
          returnsNormally,
        );
      }
      for (final colors in canvasColorSchemeThemeTokens) {
        expect(
          () => _decode(
            _model(
              properties: {
                for (final name in [
                  'backgroundColor',
                  'color',
                  'stopIndicatorColor',
                  'valueColor',
                ])
                  name: _theme(colors),
              },
            ),
          ),
          returnsNormally,
        );
      }
      for (final properties in <Map<String, Object?>>[
        {'value': _infinity},
        {'value': _null},
        {'minHeight': _int(0)},
        {'minHeight': _double(-.1)},
        {'controller': _null},
        {'controller': _string('controller')},
        {'value': _int(0), 'controller': _reference},
        {'valueColor': _string('colorAnimation')},
        {
          'valueColor': {'kind': 'callbackPresence'},
        },
        {'valueColor': _theme('material.textTheme.bodyLarge')},
        {
          'valueColor': {
            'kind': 'dartObjectReferencePresence',
            'expression': 'run()',
          },
        },
        {
          'trackGap': {..._infinity, 'value': 'negativeInfinity'},
        },
        {
          'stopIndicatorRadius': {..._infinity, 'value': 'nan'},
        },
        {'trackGap': _string('infinity')},
        {'year2023': _null},
        {'year2023': _int(0)},
        {'semanticsLabel': _null},
        {'backgroundColor': _null},
        {
          'controller': {'kind': 'dartObjectReference', 'value': 'run()'},
        },
        {'semanticsLabel': _string('x' * 65537)},
        {'key': _string('key')},
        {'child': _string('text')},
        {'constraints': _int(4)},
      ]) {
        expect(
          () => _decode(_model(properties: properties)),
          throwsFormatException,
          reason: properties.toString(),
        );
      }
      for (final name in [
        'value',
        'minHeight',
        'stopIndicatorRadius',
        'trackGap',
      ]) {
        final bytes = jsonEncode(
          _model(properties: {name: _double(567.25)}),
        ).replaceAll('567.25', '1e309');
        expect(
          () => CanvasModel.decode(Uint8List.fromList(utf8.encode(bytes))),
          throwsFormatException,
        );
      }
      final child = _model();
      _leaf(child)['slots'] = {
        'child': {'kind': 'single', 'child': null},
      };
      expect(() => _decode(child), throwsFormatException);
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'LinearProgressIndicator preserves actual SDK M2 M3 year2023 and theme/local precedence on $platform',
      (tester) async {
        for (final material3 in [false, true]) {
          for (final year in <bool?>[null, false, true]) {
            for (final themed in [false, true]) {
              for (final local in [false, true]) {
                final theme = ThemeData(
                  useMaterial3: material3,
                  progressIndicatorTheme: themed
                      ? const ProgressIndicatorThemeData(
                          color: Color(0xFF112233),
                          linearTrackColor: Color(0xFF223344),
                          linearMinHeight: 9,
                          borderRadius: BorderRadius.all(
                            Radius.elliptical(3, 5),
                          ),
                          stopIndicatorColor: Color(0xFF334455),
                          stopIndicatorRadius: 1,
                          trackGap: 7,
                          year2023: false,
                        )
                      : null,
                );
                final properties = <String, Object?>{
                  'value': _double(.4),
                  if (year != null) 'year2023': _bool(year),
                  if (local) ...{
                    'backgroundColor': _color('0xFF445566'),
                    'color': _color('0xFF556677'),
                    'minHeight': _int(11),
                    'borderRadius': _radius(),
                    'stopIndicatorColor': _color('0xFF667788'),
                    'stopIndicatorRadius': _double(2.5),
                    'trackGap': _int(6),
                  },
                };
                await _pump(
                  tester,
                  _model(platform: platform, properties: properties),
                  theme: theme,
                );
                final indicator = tester.widget<LinearProgressIndicator>(
                  _finder(),
                );
                expect(indicator.value, .4);
                expect(indicator.minHeight, local ? 11 : null);
                expect(indicator.year2023, year);
                expect(indicator.controller, isNull);
                final painter = _painter(tester);
                final effective = Theme.of(tester.element(_finder()));
                final effectiveYear = year ?? !themed;
                expect(
                  painter.valueColor,
                  local
                      ? const Color(0xFF556677)
                      : themed
                      ? const Color(0xFF112233)
                      : effective.colorScheme.primary,
                );
                expect(
                  painter.trackColor,
                  local
                      ? const Color(0xFF445566)
                      : themed
                      ? const Color(0xFF223344)
                      : material3
                      ? effective.colorScheme.secondaryContainer
                      : effective.colorScheme.background,
                );
                expect(
                  tester.getSize(_finder()).height,
                  local
                      ? 11
                      : themed
                      ? 9
                      : 4,
                );
                expect(
                  painter.stopIndicatorColor,
                  effectiveYear
                      ? null
                      : local
                      ? const Color(0xFF667788)
                      : themed
                      ? const Color(0xFF334455)
                      : material3
                      ? effective.colorScheme.primary
                      : null,
                );
                expect(
                  painter.stopIndicatorRadius,
                  effectiveYear
                      ? null
                      : local
                      ? 2.5
                      : themed
                      ? 1
                      : material3
                      ? 2
                      : null,
                );
                expect(
                  painter.trackGap,
                  effectiveYear
                      ? null
                      : local
                      ? 6
                      : themed
                      ? 7
                      : material3
                      ? 4
                      : null,
                );
                if (local) {
                  expect(painter.indicatorBorderRadius, _resolvedRadius());
                }
                expect(tester.takeException(), isNull);
              }
            }
          }
        }
      },
    );

    testWidgets(
      'LinearProgressIndicator stopped color explicit null semantic theme and color precedence on $platform',
      (tester) async {
        final theme = ThemeData(
          progressIndicatorTheme: const ProgressIndicatorThemeData(
            color: Colors.orange,
          ),
        );
        State? state;
        for (final color in <Map<String, Object?>?>[
          null,
          _color('0xFF00FF00'),
          _theme('material.colorScheme.secondary'),
          _null,
          null,
        ]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {
                'value': _double(.5),
                'color': _color('0xFF0000FF'),
                'valueColor': ?color,
              },
            ),
            theme: theme,
          );
          final indicator = tester.widget<LinearProgressIndicator>(_finder());
          if (state != null) expect(tester.state(_finder()), same(state));
          state = tester.state(_finder());
          final expected = color == null || color['kind'] == 'null'
              ? const Color(0xFF0000FF)
              : color['kind'] == 'color'
              ? const Color(0xFF00FF00)
              : Theme.of(tester.element(_finder())).colorScheme.secondary;
          expect(
            indicator.valueColor,
            color == null ? isNull : isA<AlwaysStoppedAnimation<Color?>>(),
          );
          expect(
            _painter(tester).valueColor,
            color?['kind'] == 'color' ? const Color(0xFF00FF00) : expected,
          );
          if (color?['kind'] == 'null') {
            expect(indicator.valueColor!.value, isNull);
          }
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'LinearProgressIndicator clamps only inside SDK and exposes exact progressBar/loadingSpinner semantics on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final value in <double?>[null, -3, 0, .005, .5, 1, 6]) {
            for (final custom in [false, true]) {
              await _pump(
                tester,
                _model(
                  platform: platform,
                  properties: {
                    if (value != null) 'value': _double(value),
                    'semanticsLabel': _string('Progress download'),
                    if (custom)
                      'semanticsValue': _string(
                        value == null ? 'Three of seven' : '43%',
                      ),
                  },
                ),
              );
              final indicator = tester.widget<LinearProgressIndicator>(
                _finder(),
              );
              expect(indicator.value, value);
              expect(_painter(tester).value, value?.clamp(0, 1));
              final semantic = tester
                  .widget<Semantics>(
                    find
                        .descendant(
                          of: _finder(),
                          matching: find.byType(Semantics),
                        )
                        .first,
                  )
                  .properties;
              expect(
                semantic.role,
                value == null
                    ? ui.SemanticsRole.loadingSpinner
                    : ui.SemanticsRole.progressBar,
              );
              expect(semantic.minValue, value == null ? null : '0');
              expect(semantic.maxValue, value == null ? null : '100');
              expect(semantic.label, 'Progress download');
              expect(
                semantic.value,
                custom
                    ? value == null
                          ? 'Three of seven'
                          : '43%'
                    : value == null
                    ? null
                    : '${(value.clamp(0, 1) * 100).round()}',
              );
              expect(tester.takeException(), isNull);
            }
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'LinearProgressIndicator keeps SDK state when determinate toggles and honors TickerMode animation muting on $platform',
      (tester) async {
        State? state;
        for (final value in <double?>[null, .5, null, 0, 1, null]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {if (value != null) 'value': _double(value)},
            ),
          );
          if (state != null) expect(tester.state(_finder()), same(state));
          state = tester.state(_finder());
          final before = _painter(tester).animationValue;
          await tester.pump(const Duration(milliseconds: 300));
          final after = _painter(tester).animationValue;
          expect(after == before, value != null);
          expect(tester.takeException(), isNull);
        }
        final muted = _model(platform: platform);
        _wrap(muted, 'flutter.widgets.TickerMode', {'enabled': _bool(false)});
        await _pump(tester, muted);
        final before = _painter(tester).animationValue;
        await tester.pump(const Duration(milliseconds: 600));
        expect(_painter(tester).animationValue, before);
        ((_box(muted)['slots'] as Map)['child'] as Map)['child'] = _leaf(muted);
        // A fresh reviewed TickerMode node enables the same stable leaf.
        final enabled = _model(platform: platform);
        _wrap(enabled, 'flutter.widgets.TickerMode', {'enabled': _bool(true)});
        await _pump(tester, enabled);
        final resumed = _painter(tester).animationValue;
        await tester.pump(const Duration(milliseconds: 300));
        expect(_painter(tester).animationValue, isNot(resumed));
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'LinearProgressIndicator semantics guard exactly matches SDK strings without rewriting model on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final entry in _semanticCases.entries) {
            final model = _model(
              platform: platform,
              properties: {
                'value': _double(.5),
                'semanticsValue': _string(entry.key),
              },
            );
            final before = jsonEncode(model);
            await _pump(tester, model);
            expect(
              _finder(),
              entry.value ? findsOneWidget : findsNothing,
              reason: entry.key,
            );
            if (entry.value) {
              expect(
                tester
                    .widget<LinearProgressIndicator>(_finder())
                    .semanticsValue,
                entry.key,
              );
            } else {
              expect(
                _labels(tester),
                contains('semanticsValue preview unavailable'),
                reason: entry.key,
              );
              expect(
                find.byKey(
                  const ValueKey('canvas-zero-size-widget-target-$_id'),
                ),
                findsOneWidget,
              );
            }
            expect(jsonEncode(model), before);
            expect(tester.takeException(), isNull, reason: entry.key);
            (_leaf(model)['properties'] as Map).remove('value');
            await _pump(tester, model);
            expect(_finder(), findsOneWidget);
            expect(
              tester.widget<LinearProgressIndicator>(_finder()).semanticsValue,
              entry.key,
            );
            expect(
              tester.takeException(),
              isNull,
              reason: 'indeterminate ${entry.key}',
            );
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'LinearProgressIndicator actual painter pixels match raw SDK RTL radius gap stop and trusted themed controller on $platform',
      (tester) async {
        final controller = AnimationController(vsync: tester, value: .37);
        try {
          for (final direction in TextDirection.values) {
            for (final directional in [false, true]) {
              for (final value in <double?>[null, .3]) {
                final theme = ThemeData(
                  progressIndicatorTheme: ProgressIndicatorThemeData(
                    controller: controller,
                  ),
                );
                await _pump(
                  tester,
                  _model(
                    platform: platform,
                    height: 20,
                    properties: {
                      if (value != null) 'value': _double(value),
                      'backgroundColor': _color('0xFF112233'),
                      'valueColor': _color('0xFF00FF00'),
                      'borderRadius': _radius(directional: directional),
                      'stopIndicatorColor': _color('0xFFFF0000'),
                      'stopIndicatorRadius': _int(3),
                      'trackGap': _int(7),
                      'year2023': _bool(false),
                    },
                  ),
                  theme: theme,
                  direction: direction,
                );
                final actualPainter = _painter(tester);
                expect(actualPainter.textDirection, direction);
                if (value == null) {
                  expect(actualPainter.animationValue, .37);
                  controller.value = .64;
                  await tester.pump();
                  expect(_painter(tester).animationValue, .64);
                  controller.value = .37;
                  await tester.pump();
                }
                final actual = await _painted(tester, _painter(tester));
                final BorderRadiusGeometry radius = directional
                    ? const BorderRadiusDirectional.only(
                        topStart: Radius.elliptical(1, 2),
                        topEnd: Radius.elliptical(2, 3),
                        bottomEnd: Radius.elliptical(3, 4),
                        bottomStart: Radius.elliptical(4, 5),
                      )
                    : _resolvedRadius();
                await tester.pumpWidget(
                  MaterialApp(
                    theme: theme,
                    home: Directionality(
                      textDirection: direction,
                      child: Center(
                        child: SizedBox(
                          width: 200,
                          height: 20,
                          child: LinearProgressIndicator(
                            value: value,
                            backgroundColor: const Color(0xFF112233),
                            valueColor: const AlwaysStoppedAnimation<Color?>(
                              Color(0xFF00FF00),
                            ),
                            borderRadius: radius,
                            stopIndicatorColor: const Color(0xFFFF0000),
                            stopIndicatorRadius: 3,
                            trackGap: 7,
                            year2023: false,
                          ),
                        ),
                      ),
                    ),
                  ),
                );
                await tester.pump();
                final rawPainter = tester
                    .widget<CustomPaint>(
                      find
                          .descendant(
                            of: find.byType(LinearProgressIndicator),
                            matching: find.byType(CustomPaint),
                          )
                          .first,
                    )
                    .painter!;
                expect(
                  actual,
                  orderedEquals(await _painted(tester, rawPainter)),
                );
                expect(tester.takeException(), isNull);
              }
            }
          }
        } finally {
          await tester.pumpWidget(const SizedBox());
          controller.dispose();
        }
      },
    );

    testWidgets(
      'LinearProgressIndicator zero height or width retains external selection and remains ordinary leaf on $platform',
      (tester) async {
        for (final size in [
          const Size(200, 0),
          const Size(0, 4),
          const Size(200, 1),
          const Size(200, 4),
        ]) {
          final selected = <String>[];
          await _pump(
            tester,
            _model(
              platform: platform,
              width: size.width,
              height: size.height,
              properties: {'value': _double(.4)},
            ),
            selected: selected,
          );
          expect(tester.getSize(_finder()), size);
          final target = size.isEmpty
              ? find.byKey(
                  const ValueKey('canvas-zero-size-widget-target-$_id'),
                )
              : _widget(_id);
          expect(target, findsOneWidget);
          await tester.tap(target);
          await tester.pump();
          expect(selected, contains(_id));
          await tester.sendKeyEvent(LogicalKeyboardKey.f2);
          await tester.pump();
          expect(find.byType(EditableText), findsNothing);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'LinearProgressIndicator ordinary palette drop and host-authorized move retain correct leaf slots on $platform',
      (tester) async {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        final model = _model(platform: platform, height: 100);
        final childSlot = (_box(model)['slots'] as Map)['child'] as Map;
        final leaf = childSlot['child'];
        childSlot['child'] = null;
        await _pump(
          tester,
          model,
          onDrop: (resolver) => drop = resolver,
          onMove: (resolver) => move = resolver,
        );
        final point = tester.getCenter(_widget(_boxId));
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final target = drop!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(
            token: 'progress',
            widgetType: _type,
            traits: const {},
          ),
        );
        expect(target?.parentWidgetId, _boxId);
        expect(target?.slotName, 'child');
        expect(target?.insertionIndex, 0);
        childSlot['child'] = leaf;
        _wrap(model, 'flutter.widgets.Column', {}, list: true);
        await _pump(
          tester,
          model,
          onDrop: (resolver) => drop = resolver,
          onMove: (resolver) => move = resolver,
        );
        final placement = move!(_id, _outerId, 'children', 0);
        expect(placement?.parentWidgetId, _outerId);
        expect(placement?.slotName, 'children');
        expect(placement?.zone?.isEmpty, false);
        expect(move!(_id, _id, 'child', 0), isNull);
        expect(_finder(), findsOneWidget);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'LinearProgressIndicator reference-only controller and color animation are never replaced by a fake animation on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final properties in <Map<String, Object?>>[
            {'controller': _reference},
            {'valueColor': _reference},
            {'controller': _reference, 'valueColor': _reference},
          ]) {
            final model = _model(platform: platform, properties: properties);
            final before = jsonEncode(model);
            await _pump(tester, model);
            expect(_finder(), findsNothing);
            expect(
              find.byKey(const ValueKey('canvas-zero-size-widget-target-$_id')),
              findsOneWidget,
            );
            for (final name in properties.keys) {
              expect(_labels(tester), contains(name));
            }
            expect(_labels(tester), contains('does not execute project'));
            expect(jsonEncode(model), before);
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'LinearProgressIndicator preserves negative stop gap and explicit Infinity at bounded size on $platform',
      (tester) async {
        for (final value in <double?>[null, 0, .5, 1]) {
          for (final number in [_int(-2), _int(0), _double(.5), _infinity]) {
            final properties = {
              'stopIndicatorRadius': number,
              'trackGap': number,
              'stopIndicatorColor': _color('0xFF112233'),
              'year2023': _bool(false),
              if (value != null) 'value': _double(value),
            };
            await _pump(
              tester,
              _model(
                platform: platform,
                width: 180,
                height: 12,
                properties: properties,
              ),
            );
            expect(
              _painter(tester).stopIndicatorRadius,
              number == _infinity ? double.infinity : number['value'],
            );
            expect(
              _painter(tester).trackGap,
              number == _infinity ? double.infinity : number['value'],
            );
            expect(tester.takeException(), isNull);
          }
        }
        await _pump(
          tester,
          _model(
            platform: platform,
            width: 200,
            height: 32,
            properties: {'minHeight': _infinity},
          ),
        );
        expect(
          tester.widget<LinearProgressIndicator>(_finder()).minHeight,
          double.infinity,
        );
        expect(tester.getSize(_finder()), const Size(200, 32));
      },
    );

    testWidgets(
      'LinearProgressIndicator unresolved bounds and missing M2 stop color are diagnosed without fabricating values on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          final theme = ThemeData(useMaterial3: false);
          final model = _model(
            platform: platform,
            properties: {
              'value': _double(.5),
              'year2023': _bool(false),
              'stopIndicatorRadius': _int(3),
            },
          );
          final before = jsonEncode(model);
          await _pump(tester, model, theme: theme);
          expect(_finder(), findsNothing);
          expect(
            _labels(tester),
            contains('stopIndicatorColor preview unavailable'),
          );
          expect(jsonEncode(model), before);
          final themed = theme.copyWith(
            progressIndicatorTheme: const ProgressIndicatorThemeData(
              stopIndicatorColor: Colors.green,
            ),
          );
          await _pump(tester, model, theme: themed);
          expect(_finder(), findsOneWidget);
          expect(_painter(tester).stopIndicatorColor, Colors.green);
          expect(_painter(tester).stopIndicatorRadius, 3);
          final unboundedWidth = _model(platform: platform);
          _wrap(unboundedWidth, 'flutter.widgets.Row', {}, list: true);
          await _pump(tester, unboundedWidth);
          expect(_finder(), findsNothing);
          expect(_labels(tester), contains('unbounded width'));
          final unboundedHeight = _model(
            platform: platform,
            properties: {'minHeight': _infinity},
          );
          _wrap(unboundedHeight, 'flutter.widgets.Column', {}, list: true);
          await _pump(tester, unboundedHeight);
          expect(_finder(), findsNothing);
          expect(_labels(tester), contains('minHeight preview unavailable'));
          expect(_labels(tester), contains('bounded parent height'));
          expect(tester.takeException(), isNull);
        } finally {
          semantics.dispose();
        }
      },
    );
  }
  testWidgets(
    'raw SDK semantics progressBar validation has exact numeric percentage rules and indeterminate allows free text',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        for (final entry in _semanticCases.entries) {
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: LinearProgressIndicator(
                  value: .5,
                  semanticsValue: entry.key,
                ),
              ),
            ),
          );
          final error = tester.takeException();
          expect(
            error,
            entry.value ? isNull : isA<FlutterError>(),
            reason: entry.key,
          );
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: LinearProgressIndicator(semanticsValue: entry.key),
              ),
            ),
          );
          expect(tester.takeException(), isNull);
        }
      } finally {
        semantics.dispose();
      }
    },
  );
  testWidgets(
    'raw SDK M2 determinate positive stop radius requires a color only when year2023 false',
    (tester) async {
      for (final value in <double?>[null, .5]) {
        for (final year2023 in [false, true]) {
          await tester.pumpWidget(
            MaterialApp(
              theme: ThemeData(useMaterial3: false),
              home: Center(
                child: LinearProgressIndicator(
                  value: value,
                  year2023: year2023,
                  stopIndicatorRadius: 3,
                ),
              ),
            ),
          );
          expect(
            tester.takeException(),
            value != null && !year2023 ? isNotNull : isNull,
          );
        }
      }
    },
  );
  testWidgets(
    'raw SDK positive infinity progress geometry stays paint safe under bounded parent',
    (tester) async {
      for (final value in <double?>[null, 0, .5, 1]) {
        for (final name in ['minHeight', 'stopIndicatorRadius', 'trackGap']) {
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: SizedBox(
                  width: 200,
                  height: 30,
                  child: LinearProgressIndicator(
                    value: value,
                    year2023: false,
                    minHeight: name == 'minHeight' ? double.infinity : 4,
                    stopIndicatorColor: Colors.blue,
                    stopIndicatorRadius: name == 'stopIndicatorRadius'
                        ? double.infinity
                        : 2,
                    trackGap: name == 'trackGap' ? double.infinity : 4,
                  ),
                ),
              ),
            ),
          );
          await tester.pump(const Duration(milliseconds: 400));
          expect(tester.takeException(), isNull, reason: '$name value=$value');
        }
      }
    },
  );
}

const _semanticCases = <String, bool>{
  '': false,
  'Three of seven': false,
  '43%': true,
  '0': true,
  '100': true,
  ' 50 ': true,
  ' 50 %': true,
  '50% ': false,
  '-1': false,
  '101': false,
  '-1%': false,
  '101%': false,
  'NaN': true,
  'NaN%': true,
  'Infinity': false,
  'Infinity%': false,
  '1e2': true,
};

Future<Uint8List> _painted(WidgetTester tester, CustomPainter painter) async =>
    (await tester.runAsync(() async {
      final recorder = ui.PictureRecorder();
      painter.paint(Canvas(recorder), const Size(200, 20));
      final picture = recorder.endRecording();
      final image = await picture.toImage(200, 20);
      try {
        return (await image.toByteData(
          format: ui.ImageByteFormat.rawRgba,
        ))!.buffer.asUint8List();
      } finally {
        image.dispose();
        picture.dispose();
      }
    }))!;

Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  double width = 200,
  double? height,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  final leaf = {
    'id': _id,
    'type': _type,
    'properties': properties,
    'slots': <String, Object?>{},
  };
  final box = {
    'id': _boxId,
    'type': 'flutter.widgets.SizedBox',
    'properties': {
      'width': _double(width),
      if (height != null) 'height': _double(height),
    },
    'slots': {
      'child': <String, Object?>{'kind': 'single', 'child': leaf},
    },
  };
  final center = {
    'id': '29154d08-5472-4eae-8d4d-9352da0bbd52',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': {
      'child': {'kind': 'single', 'child': box},
    },
  };
  model['root'] = {
    'id': 'e5e23da0-f3e6-46fa-88fb-0ab84d966dd3',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {'kind': 'single', 'child': center},
    },
  };
  return model;
}

Map _box(Map<String, Object?> model) {
  final body = ((model['root'] as Map)['slots'] as Map)['body'] as Map;
  final center = body['child'] as Map;
  final child = (center['slots'] as Map)['child'] as Map;
  return child['child'] as Map;
}

Map _leaf(Map<String, Object?> model) {
  Map node = _box(model);
  while (node['type'] != _type) {
    final slots = node['slots'] as Map;
    final slot = slots.values.first as Map;
    node = slot['kind'] == 'list'
        ? (slot['children'] as List).single as Map
        : slot['child'] as Map;
  }
  return node;
}

void _wrap(
  Map<String, Object?> model,
  String type,
  Map<String, Object?> properties, {
  bool list = false,
}) {
  final slot = ((_box(model)['slots'] as Map)['child'] as Map);
  slot['child'] = {
    'id': _outerId,
    'type': type,
    'properties': properties,
    'slots': {
      list ? 'children' : 'child': list
          ? {
              'kind': 'list',
              'children': [slot['child']],
            }
          : {'kind': 'single', 'child': slot['child']},
    },
  };
}

CanvasNode _node(CanvasModel model) {
  CanvasNode node = model.root;
  while (node.type != _type) {
    node = node.slots.values.first.children.single;
  }
  return node;
}

Map<String, Object?> _int(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _double(double value) => {
  'kind': 'double',
  'value': value,
};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _color(String value) => {'kind': 'color', 'argb': value};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
Map<String, Object?> _radius({bool directional = false}) => {
  'kind': 'borderRadius',
  'geometry': {
    'kind': directional ? 'directional' : 'physical',
    directional ? 'topStart' : 'topLeft': {'x': 1.0, 'y': 2.0},
    directional ? 'topEnd' : 'topRight': {'x': 2.0, 'y': 3.0},
    directional ? 'bottomEnd' : 'bottomRight': {'x': 3.0, 'y': 4.0},
    directional ? 'bottomStart' : 'bottomLeft': {'x': 4.0, 'y': 5.0},
  },
};
BorderRadius _resolvedRadius() => const BorderRadius.only(
  topLeft: Radius.elliptical(1, 2),
  topRight: Radius.elliptical(2, 3),
  bottomRight: Radius.elliptical(3, 4),
  bottomLeft: Radius.elliptical(4, 5),
);
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _finder() => find.descendant(
  of: _widget(_id),
  matching: find.byType(LinearProgressIndicator),
);
dynamic _painter(WidgetTester tester) => tester
    .widget<CustomPaint>(
      find.descendant(of: _finder(), matching: find.byType(CustomPaint)).first,
    )
    .painter!;
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  void Function(CanvasDropResolver?)? onDrop,
  void Function(CanvasMovePreviewResolver?)? onMove,
  TextDirection direction = TextDirection.ltr,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: Directionality(
        textDirection: direction,
        child: CanvasDocumentView(
          model: _decode(model),
          selectedWidgetId: null,
          onSelected: selected?.add ?? (_) {},
          onDropResolverChanged: onDrop,
          onMovePreviewResolverChanged: onMove,
          inlineTextEditEnabled: true,
          onInlineTextCommit: (_, _, _) =>
              throw StateError('Progress cannot edit text'),
        ),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 100));
  await tester.pump();
}

String _labels(WidgetTester tester) {
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
