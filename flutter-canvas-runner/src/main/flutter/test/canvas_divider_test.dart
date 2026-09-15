import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Divider';
const _id = 'acb04d43-a11e-468f-bba0-a6a90a1291ae';
const _centerId = '80e42b1c-ae0d-428a-8c19-674ad86c68ee';
const _columnId = '07303f47-9cbe-408b-a4fa-4fd9c5e1c765';
const _numbers = ['height', 'thickness', 'indent', 'endIndent'];
const _names = [..._numbers, 'color', 'radius'];

void main() {
  test(
    'Divider has exactly six optional fields and no child, trait or creation default',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final block = contract.substring(
        start,
        contract.indexOf('W|flutter.material.Drawer\n', start),
      );
      expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(6));
      for (final name in _numbers) {
        expect(
          block,
          contains(
            'P|$name|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|',
          ),
        );
      }
      expect(
        block,
        contains(
          'P|radius|borderRadius|0|-|-|borderRadius:borderRadius:v1:physical,directional:finiteNonNegative',
        ),
      );
      expect(block, contains('P|color|color,themeToken|0|-|-|color:any;'));
      expect(block, isNot(contains('S|')));
      expect(block, isNot(contains('C|')));
      expect(canvasModelProtocolVersion, 20);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(isCanvasPaletteWrapperWidgetType(_type), false);
      expect(_node(_decode(_model())).properties, isEmpty);
    },
  );

  test(
    'Divider accepts all 729 optional numeric color and radius combinations without inventing dependencies',
    () {
      for (var combination = 0; combination < 729; combination++) {
        var digits = combination;
        final properties = <String, Object?>{};
        for (final name in _names) {
          final state = digits % 3;
          digits ~/= 3;
          if (state == 0) continue;
          properties[name] = switch (name) {
            'color' =>
              state == 1
                  ? _color('0x80123456')
                  : _theme('material.colorScheme.primary'),
            'radius' => _radius(directional: state == 2, zero: state == 1),
            _ => state == 1 ? _int(0) : _double(7.5),
          };
        }
        final decoded = _decode(_model(properties: properties));
        expect(
          _node(decoded).properties.keys,
          unorderedEquals(properties.keys),
        );
        expect(decoded.imageResourceIds, isEmpty);
      }
    },
  );

  test(
    'Divider number domain accepts finite nonnegative interoperable numbers and rejects invalid envelopes',
    () {
      for (final name in _numbers) {
        for (final value in [
          _int(0),
          _int(9007199254740991),
          _double(0),
          _double(2.5),
          _double(1e200),
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            returnsNormally,
          );
        }
        for (final value in [
          null,
          {'kind': 'null'},
          _int(-1),
          _double(-.1),
          _int(9007199254740992),
          {'kind': 'integer', 'value': 1.5},
          {'kind': 'double', 'value': '1'},
          {'kind': 'boolean', 'value': false},
          {'kind': 'integer'},
          {'kind': 'double', 'value': 1, 'extra': 1},
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
            reason: '$name: $value',
          );
        }
        final encoded = jsonEncode(_model(properties: {name: _double(1.25)}));
        expect(
          () => CanvasModel.decode(
            Uint8List.fromList(
              utf8.encode(encoded.replaceFirst('1.25', '1e309')),
            ),
          ),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Divider rejects undeclared properties every child shape and malformed theme colors',
    () {
      for (final name in [
        'width',
        'space',
        'borderRadius',
        'key',
        'child',
        'children',
        'onTap',
        'useMaterial3',
      ]) {
        expect(
          () => _decode(_model(properties: {name: _int(1)})),
          throwsFormatException,
        );
      }
      for (final slots in [
        {
          'child': {'kind': 'single', 'child': null},
        },
        {
          'children': {'kind': 'list', 'children': []},
        },
      ]) {
        final model = _model();
        _leaf(model)['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
      for (final color in [
        null,
        {'kind': 'null'},
        _int(0),
        _color('#ffffff'),
        _theme('material.textTheme.bodyLarge'),
        _theme('material.colorScheme.unknown'),
        {'kind': 'color', 'argb': '0xFF112233', 'extra': true},
      ]) {
        expect(
          () => _decode(_model(properties: {'color': color})),
          throwsFormatException,
        );
      }
      for (final token in canvasColorSchemeThemeTokens) {
        expect(
          () => _decode(_model(properties: {'color': _theme(token)})),
          returnsNormally,
        );
      }
    },
  );

  test(
    'Divider keeps physical and directional elliptical radii and closes every nested radius envelope',
    () {
      for (final directional in [false, true]) {
        final radius = _radius(directional: directional);
        final node = _node(_decode(_model(properties: {'radius': radius})));
        expect(
          node.properties['radius']!.value,
          directional
              ? isA<CanvasDirectionalBorderRadiusValue>()
              : isA<CanvasPhysicalBorderRadiusValue>(),
        );
        for (final mutation in [
          'missingCorner',
          'extraCorner',
          'missingAxis',
          'extraAxis',
          'negative',
          'string',
          'rawNull',
          'kind',
          'envelope',
        ]) {
          final bad = jsonDecode(jsonEncode(radius)) as Map<String, Object?>;
          final geometry = bad['geometry'] as Map;
          final corner = directional ? 'topStart' : 'topLeft';
          switch (mutation) {
            case 'missingCorner':
              geometry.remove(corner);
            case 'extraCorner':
              geometry['other'] = {'x': 0, 'y': 0};
            case 'missingAxis':
              (geometry[corner] as Map).remove('y');
            case 'extraAxis':
              (geometry[corner] as Map)['z'] = 0;
            case 'negative':
              (geometry[corner] as Map)['x'] = -.1;
            case 'string':
              (geometry[corner] as Map)['y'] = '2';
            case 'rawNull':
              geometry[corner] = null;
            case 'kind':
              geometry['kind'] = 'mixed';
            case 'envelope':
              bad['extra'] = true;
          }
          expect(
            () => _decode(_model(properties: {'radius': bad})),
            throwsFormatException,
            reason: mutation,
          );
        }
      }
      for (final radius in [
        null,
        {'kind': 'null'},
        {'kind': 'borderRadius'},
        {'kind': 'borderRadius', 'geometry': null},
        _int(1),
      ]) {
        expect(
          () => _decode(_model(properties: {'radius': radius})),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Divider remains an ordinary leaf insertion and wrapper child with no invented destination',
    () {
      for (final parent in [
        (type: 'flutter.widgets.Column', slot: 'children'),
        (type: 'flutter.widgets.Center', slot: 'child'),
        (type: 'flutter.widgets.Stack', slot: 'children'),
        (type: 'flutter.widgets.Visibility', slot: 'replacement'),
      ]) {
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: parent.type,
            slotName: parent.slot,
            currentChildCount: 0,
            insertionIndex: 0,
            source: _source(),
          ),
          true,
        );
      }
      for (final wrapper in [
        'flutter.widgets.IconTheme',
        'flutter.widgets.Visibility',
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
          source: _source(),
        ),
        false,
      );
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.material.Scaffold',
          slotName: 'appBar',
          currentChildCount: 0,
          insertionIndex: 0,
          source: _source(),
        ),
        false,
      );
    },
  );

  for (final platform in ['windows', 'web']) {
    for (final material3 in [false, true]) {
      testWidgets(
        'Divider omitted fields use exact SDK Material ${material3 ? 3 : 2} defaults on $platform',
        (tester) async {
          final theme = ThemeData(
            useMaterial3: material3,
            dividerColor: const Color(0xFF123456),
            colorScheme: ColorScheme.fromSeed(
              seedColor: Colors.teal,
            ).copyWith(outlineVariant: const Color(0xFFABCDEF)),
          );
          await _pump(tester, _model(platform: platform), theme: theme);
          final divider = tester.widget<Divider>(_divider());
          expect([
            divider.height,
            divider.thickness,
            divider.indent,
            divider.endIndent,
            divider.color,
            divider.radius,
          ], everyElement(isNull));
          expect(tester.getSize(_divider()).height, 16);
          final decoration = _decoration(tester);
          expect(decoration.borderRadius, isNull);
          expect(
            (decoration.border! as Border).bottom.width,
            material3 ? 1 : 0,
          );
          expect(
            (decoration.border! as Border).bottom.color,
            material3 ? theme.colorScheme.outlineVariant : theme.dividerColor,
          );
          expect(tester.getSize(_line()).height, material3 ? 1 : 0);
          expect(tester.takeException(), isNull);
        },
      );
    }

    for (final direction in TextDirection.values) {
      testWidgets(
        'Divider six local values override theme and resolve elliptical directional corners and indents in $direction on $platform',
        (tester) async {
          await _pump(
            tester,
            _model(
              platform: platform,
              direction: direction,
              properties: {
                'height': _double(30.5),
                'thickness': _double(8.5),
                'indent': _int(11),
                'endIndent': _double(23.5),
                'color': _color('0x80123456'),
                'radius': _radius(directional: true),
              },
            ),
            theme: ThemeData(
              dividerTheme: const DividerThemeData(
                space: 99,
                thickness: 15,
                indent: 40,
                endIndent: 50,
                color: Colors.green,
                radius: BorderRadius.all(Radius.circular(20)),
              ),
            ),
          );
          final divider = tester.widget<Divider>(_divider());
          expect(divider.height, 30.5);
          expect(divider.thickness, 8.5);
          expect(divider.indent, 11);
          expect(divider.endIndent, 23.5);
          expect(divider.color, const Color(0x80123456));
          expect(
            divider.radius,
            const BorderRadiusDirectional.only(
              topStart: Radius.elliptical(1, 2),
              topEnd: Radius.elliptical(3, 4),
              bottomEnd: Radius.elliptical(5, 6),
              bottomStart: Radius.elliptical(7, 8),
            ),
          );
          final line = tester.renderObject<RenderDecoratedBox>(_line());
          expect(line.configuration.textDirection, direction);
          final decoration = line.decoration as BoxDecoration;
          expect(
            decoration.borderRadius!.resolve(direction).topLeft,
            direction == TextDirection.ltr
                ? const Radius.elliptical(1, 2)
                : const Radius.elliptical(3, 4),
          );
          expect(
            (decoration.border! as Border).bottom,
            const BorderSide(color: Color(0x80123456), width: 8.5),
          );
          expect(tester.getSize(_divider()).height, 30.5);
          expect(tester.getSize(_line()).height, 8.5);
          final container = tester.widget<Container>(
            find.descendant(of: _divider(), matching: find.byType(Container)),
          );
          expect(
            container.margin,
            const EdgeInsetsDirectional.only(start: 11, end: 23.5),
          );
          final lineRect = tester.getRect(_line());
          final dividerRect = tester.getRect(_divider());
          final scale = dividerRect.height / 30.5;
          expect(
            lineRect.left - dividerRect.left,
            closeTo((direction == TextDirection.ltr ? 11 : 23.5) * scale, .001),
          );
          expect(
            dividerRect.right - lineRect.right,
            closeTo((direction == TextDirection.ltr ? 23.5 : 11) * scale, .001),
          );
          expect(lineRect.center.dy, closeTo(dividerRect.center.dy, .001));
          expect(tester.takeException(), isNull);
        },
      );
    }

    testWidgets(
      'Divider omitted fields inherit all six DividerTheme values and explicit zeros replace them on $platform',
      (tester) async {
        final theme = ThemeData(
          dividerTheme: const DividerThemeData(
            space: 36,
            thickness: 6,
            indent: 7,
            endIndent: 9,
            color: Colors.purple,
            radius: BorderRadius.only(
              topLeft: Radius.elliptical(1, 2),
              bottomRight: Radius.elliptical(3, 4),
            ),
          ),
        );
        await _pump(tester, _model(platform: platform), theme: theme);
        expect(tester.getSize(_divider()).height, 36);
        expect(tester.getSize(_line()).height, 6);
        expect(_decoration(tester).borderRadius, theme.dividerTheme.radius);
        expect(
          (_decoration(tester).border! as Border).bottom.color,
          Colors.purple,
        );
        final element = tester.element(_divider());
        await _pump(
          tester,
          _model(
            platform: platform,
            properties: {
              for (final name in _numbers) name: _int(0),
              'radius': _radius(zero: true),
              'color': _color('0x00000000'),
            },
          ),
          theme: theme,
        );
        expect(identical(element, tester.element(_divider())), true);
        expect(tester.getSize(_divider()).height, 0);
        expect(tester.getSize(_line()).height, 0);
        expect(_decoration(tester).borderRadius, BorderRadius.zero);
        expect(
          (_decoration(tester).border! as Border).bottom,
          const BorderSide(color: Color(0x00000000), width: 0),
        );
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Divider physical radius and semantic theme color resolve dynamically without replacing the node on $platform',
      (tester) async {
        final properties = {
          'radius': _radius(),
          'thickness': _int(4),
          'color': _theme('material.colorScheme.primary'),
        };
        await _pump(
          tester,
          _model(platform: platform, properties: properties),
          theme: ThemeData(
            colorScheme: ColorScheme.fromSeed(
              seedColor: Colors.green,
            ).copyWith(primary: Colors.orange),
          ),
        );
        final element = tester.element(_divider());
        expect(
          (_decoration(tester).border! as Border).bottom.color,
          Colors.orange,
        );
        expect(
          _decoration(tester).borderRadius,
          const BorderRadius.only(
            topLeft: Radius.elliptical(1, 2),
            topRight: Radius.elliptical(3, 4),
            bottomRight: Radius.elliptical(5, 6),
            bottomLeft: Radius.elliptical(7, 8),
          ),
        );
        await _pump(
          tester,
          _model(platform: platform, properties: properties),
          theme: ThemeData(
            colorScheme: ColorScheme.fromSeed(
              seedColor: Colors.blue,
            ).copyWith(primary: Colors.cyan),
          ),
        );
        expect(identical(element, tester.element(_divider())), true);
        expect(
          (_decoration(tester).border! as Border).bottom.color,
          Colors.cyan,
        );
        await _pump(tester, _model(platform: platform));
        expect(identical(element, tester.element(_divider())), true);
        expect(tester.widget<Divider>(_divider()).radius, isNull);
        expect(tester.widget<Divider>(_divider()).color, isNull);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Divider has an accessible Designer target but no SDK control or F2 text editor on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          final selected = <String>[];
          await _pump(tester, _model(platform: platform), selected: selected);
          await tester.tap(_divider());
          await tester.pump();
          expect(selected, contains(_id));
          await tester.sendKeyEvent(LogicalKeyboardKey.f2);
          await tester.pump();
          expect(find.byType(TextField), findsNothing);
          expect(find.byType(EditableText), findsNothing);
          final labels = _labels(tester);
          expect(labels.any((label) => label.contains(_id)), true);
          expect(labels.any((label) => label.contains('Divider')), true);
          expect(tester.takeException(), isNull);
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'Divider zero-height remains selectable and draggable as a leaf without becoming a child destination on $platform',
      (tester) async {
        final selected = <String>[];
        await _pump(
          tester,
          _model(platform: platform, properties: {'height': _int(0)}),
          selected: selected,
        );
        expect(tester.getSize(_divider()).height, 0);
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$_id'),
        );
        expect(handle, findsOneWidget);
        await tester.tap(handle);
        await tester.pump();
        expect(selected, contains(_id));
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        expect(find.byType(TextField), findsNothing);
        CanvasDropResolver? resolver;
        final model = _model(
          platform: platform,
          properties: {'height': _int(0)},
        );
        final leaf = _leaf(model);
        _centerSlot(model)['child'] = {
          'id': _columnId,
          'type': 'flutter.widgets.Column',
          'properties': <String, Object?>{},
          'slots': {
            'children': {
              'kind': 'list',
              'children': [leaf],
            },
          },
        };
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(model),
            selectedWidgetId: _id,
            onSelected: selected.add,
            onDropResolverChanged: (value) => resolver = value,
          ),
        );
        await tester.pump();
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final point = tester.getCenter(_widget(_columnId));
        final drop = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          _source(),
        );
        expect(drop?.parentWidgetId, _columnId);
        expect(drop?.slotName, 'children');
        expect(drop?.insertionIndex, 1);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'DividerTheme nearest override and per-field local fallback stay genuine SDK behavior',
    (tester) async {
      final model = _decode(_model(properties: {'height': _int(28)}));
      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(
            dividerTheme: const DividerThemeData(
              space: 40,
              thickness: 9,
              color: Colors.green,
            ),
          ),
          home: DividerTheme(
            data: const DividerThemeData(
              space: 32,
              thickness: 3,
              color: Colors.amber,
            ),
            child: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        ),
      );
      await tester.pump();
      expect(tester.getSize(_divider()).height, 28);
      expect(tester.getSize(_line()).height, 3);
      expect(
        (_decoration(tester).border! as Border).bottom.color,
        Colors.amber,
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Raw pinned SDK Divider hairline and radius matrix characterizes real paint errors instead of accepting a fake render',
    (tester) async {
      for (final material3 in [false, true]) {
        for (final thickness in <double?>[null, 0, 2]) {
          for (final radius in <BorderRadiusGeometry?>[
            null,
            BorderRadius.zero,
            BorderRadius.circular(3),
          ]) {
            await tester.pumpWidget(
              MaterialApp(
                theme: ThemeData(useMaterial3: material3),
                home: Center(
                  child: SizedBox(
                    width: 100,
                    child: Divider(thickness: thickness, radius: radius),
                  ),
                ),
              ),
            );
            final error = tester.takeException();
            final shouldFail =
                (thickness == 0 || (thickness == null && !material3)) &&
                radius != null &&
                radius != BorderRadius.zero;
            expect(
              error == null,
              !shouldFail,
              reason: 'M3=$material3 thickness=$thickness radius=$radius',
            );
            if (shouldFail) expect('$error', contains('A hairline border'));
            await tester.pumpWidget(const SizedBox.shrink());
            expect(tester.takeException(), isNull);
          }
        }
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'Canvas Divider preserves pinned SDK hairline errors including themed and omitted thickness on $platform',
      (tester) async {
        for (final material3 in [false, true]) {
          for (final thickness in <double?>[null, 0, 2]) {
            for (final radiusMode in [
              'absent',
              'zero',
              'positive',
              'themePositive',
            ]) {
              final properties = <String, Object?>{
                if (thickness != null) 'thickness': _double(thickness),
                if (radiusMode == 'zero') 'radius': _radius(zero: true),
                if (radiusMode == 'positive')
                  'radius': _radius(directional: true),
              };
              final theme = ThemeData(
                useMaterial3: material3,
                dividerTheme: DividerThemeData(
                  radius: radiusMode == 'themePositive'
                      ? BorderRadius.circular(3)
                      : null,
                ),
              );
              // Do not issue a second frame before observing the SDK paint error.
              await _pump(
                tester,
                _model(platform: platform, properties: properties),
                theme: theme,
                settle: false,
              );
              final error = tester.takeException();
              final shouldFail =
                  (thickness == 0 || (thickness == null && !material3)) &&
                  (radiusMode == 'positive' || radiusMode == 'themePositive');
              expect(
                error == null,
                !shouldFail,
                reason:
                    '$platform M3=$material3 thickness=$thickness radius=$radiusMode',
              );
              if (shouldFail) expect('$error', contains('A hairline border'));
              final divider = tester.widget<Divider>(_divider());
              expect(
                divider.thickness,
                thickness,
                reason:
                    'No forced positive thickness or synthesized theme default',
              );
              expect(
                divider.radius == null,
                radiusMode == 'absent' || radiusMode == 'themePositive',
              );
              await tester.pumpWidget(const SizedBox.shrink());
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );
  }

  testWidgets(
    'Divider explicit zero radius overrides a themed nonzero radius and positive theme thickness repairs M2 omission',
    (tester) async {
      await _pump(
        tester,
        _model(properties: {'radius': _radius(directional: true, zero: true)}),
        theme: ThemeData(
          useMaterial3: false,
          dividerTheme: const DividerThemeData(
            radius: BorderRadius.all(Radius.circular(4)),
          ),
        ),
      );
      expect(tester.takeException(), isNull);
      expect((_decoration(tester).border! as Border).bottom.width, 0);
      await _pump(
        tester,
        _model(properties: {'radius': _radius()}),
        theme: ThemeData(
          useMaterial3: false,
          dividerTheme: const DividerThemeData(thickness: 3),
        ),
      );
      expect(tester.takeException(), isNull);
      expect((_decoration(tester).border! as Border).bottom.width, 3);
      expect(tester.widget<Divider>(_divider()).thickness, isNull);
    },
  );
}

Map<String, Object?> _model({
  String platform = 'windows',
  TextDirection? direction,
  Map<String, Object?> properties = const {},
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  final leaf = <String, Object?>{
    'id': _id,
    'type': _type,
    'properties': properties,
    'slots': <String, Object?>{},
  };
  model['root'] = {
    'id': 'cd8537b9-fdda-4d41-9c09-ff1577332716',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': {
          'id': _centerId,
          'type': 'flutter.widgets.Center',
          'properties': <String, Object?>{},
          'slots': {
            'child': {
              'kind': 'single',
              'child': direction == null
                  ? leaf
                  : {
                      'id': 'bb462f89-8f67-4a84-b03e-471bcf7f543f',
                      'type': 'flutter.widgets.Directionality',
                      'properties': {
                        'textDirection': {
                          'kind': 'enum',
                          'type': 'TextDirection',
                          'value': direction.name,
                        },
                      },
                      'slots': {
                        'child': {'kind': 'single', 'child': leaf},
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
Map<String, Object?> _int(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _double(double value) => {
  'kind': 'double',
  'value': value,
};
Map<String, Object?> _color(String value) => {'kind': 'color', 'argb': value};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
Map<String, Object?> _radius({bool directional = false, bool zero = false}) => {
  'kind': 'borderRadius',
  'geometry': {
    'kind': directional ? 'directional' : 'physical',
    directional ? 'topStart' : 'topLeft': {
      'x': zero ? 0 : 1,
      'y': zero ? 0 : 2,
    },
    directional ? 'topEnd' : 'topRight': {'x': zero ? 0 : 3, 'y': zero ? 0 : 4},
    directional ? 'bottomEnd' : 'bottomRight': {
      'x': zero ? 0 : 5,
      'y': zero ? 0 : 6,
    },
    directional ? 'bottomStart' : 'bottomLeft': {
      'x': zero ? 0 : 7,
      'y': zero ? 0 : 8,
    },
  },
};
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode _node(CanvasModel model) =>
    model.root.slot('body')!.children.single.slot('child')!.children.single;
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _divider() =>
    find.descendant(of: _widget(_id), matching: find.byType(Divider));
Finder _line() =>
    find.descendant(of: _divider(), matching: find.byType(DecoratedBox));
BoxDecoration _decoration(WidgetTester tester) =>
    tester.widget<DecoratedBox>(_line()).decoration as BoxDecoration;
CanvasPaletteDragSource _source() => CanvasPaletteDragSource(
  token: _type,
  widgetType: _type,
  traits: canvasWidgetTraitsForType(_type),
);
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  bool settle = true,
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
            throw StateError('Divider must not edit text'),
      ),
    ),
  );
  if (settle) await tester.pump();
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
