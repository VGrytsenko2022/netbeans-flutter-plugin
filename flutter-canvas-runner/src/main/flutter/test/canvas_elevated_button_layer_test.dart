import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _id = 'd5659c06-8da0-45d1-91c8-75eef08d9442';
const _childId = '146cc972-81e9-4797-a474-ee97d10f3b34';
const _presence = {'kind': 'dartObjectReferencePresence'};
const _names = ['styleBackgroundBuilder', 'styleForegroundBuilder'];

void main() {
  test(
    'ElevatedButton adds exactly two typed layer references to its 288-property reviewed schema',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      expect(contract, canvasReviewedWidgetSchemaContract.trimLeft());
      expect(canvasModelProtocolVersion, 20);
      expect(
        RegExp(r'^W\|', multiLine: true).allMatches(contract),
        hasLength(249),
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(contract),
        hasLength(8476),
      );
      final start = contract.indexOf('W|flutter.material.ElevatedButton\n');
      final section = contract.substring(
        start,
        contract.indexOf('\nW|', start) + 1,
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(section),
        hasLength(288),
      );
      for (final name in _names) {
        expect(
          section,
          contains(
            'P|$name|dartObjectReference|0|-|-|'
            'dartObjectReference:dartObjectReference:v1:ButtonLayerBuilder:currentOrPackage:'
            'root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)',
          ),
        );
      }
    },
  );

  test(
    'Layer payload accepts only anonymous presence and rejects callback null and executable identities',
    () {
      for (final name in _names) {
        for (final value in [
          {'kind': 'null'},
          {'kind': 'callbackPresence'},
          {'kind': 'string', 'value': 'projectLayer'},
          {'kind': 'dartObjectReference', 'rootSymbol': 'privateLayer'},
          {'kind': 'dartObjectReferencePresence', 'rootSymbol': 'privateLayer'},
          {'kind': 'dartObjectReferencePresence', 'member': 'privateGetter'},
          {
            'kind': 'dartObjectReferencePresence',
            'libraryUri': 'package:private/layers.dart',
          },
          {
            'kind': 'dartObjectReferencePresence',
            'access': 'zeroArgumentInvocation',
          },
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
            reason: '$name: $value',
          );
        }
      }
    },
  );

  for (var mask = 0; mask < 4; mask++) {
    testWidgets(
      'Layer combination $mask preserves real child and exact SDK clipping with every explicit override',
      (tester) async {
        for (final clip in [null, ...Clip.values]) {
          await _pump(
            tester,
            _model(
              properties: {
                ..._layers(mask),
                if (clip != null)
                  'clipBehavior': {
                    'kind': 'enum',
                    'type': 'Clip',
                    'value': clip.name,
                  },
              },
            ),
          );
          final sdk = _sdk(tester);
          expect(sdk.clipBehavior, clip);
          expect(sdk.style?.backgroundBuilder != null, mask & 1 != 0);
          expect(sdk.style?.foregroundBuilder != null, mask & 2 != 0);
          expect(
            _material(tester).clipBehavior,
            clip ?? (mask == 0 ? Clip.none : Clip.antiAlias),
          );
          expect(find.text('Layer action'), findsOneWidget);
          expect(_warning, mask == 0 ? findsNothing : findsOneWidget);
          if (mask != 0) {
            final message = tester.widget<Tooltip>(_warning).message!;
            expect(
              message,
              contains('does not execute project or dependency Dart'),
            );
            expect(
              message,
              contains(
                'identity layers preserve the real child and SDK builder-dependent clipping',
              ),
            );
          }
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'Layer combination $mask keeps a real empty nullable child safe and the SDK minimum hit target',
      (tester) async {
        await _pump(tester, _model(properties: _layers(mask), empty: true));
        expect(_sdk(tester).child, isNull);
        expect(tester.getSize(_button).width, greaterThan(0));
        expect(tester.getSize(_button).height, greaterThanOrEqualTo(48));
        final gesture = await tester.startGesture(tester.getCenter(_button));
        await tester.pump(const Duration(milliseconds: 300));
        expect(_controller(tester).value, contains(WidgetState.pressed));
        await gesture.up();
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'Captured placeholders preserve identity, accept every state set and return a nonnull empty widget for null input',
    (tester) async {
      await _pump(tester, _model(properties: _layers(3)));
      final style = _sdk(tester).style!;
      final context = tester.element(_button);
      const child = SizedBox(
        key: ValueKey('identity-probe'),
        width: 13,
        height: 17,
      );
      for (final builder in [
        style.backgroundBuilder!,
        style.foregroundBuilder!,
      ]) {
        for (final states in [
          <WidgetState>{},
          WidgetState.values.toSet(),
          ...WidgetState.values.map((state) => {state}),
        ]) {
          expect(builder(context, states, child), same(child));
          final empty = builder(context, states, null);
          expect(empty, isA<SizedBox>());
          expect((empty as SizedBox).width, 0);
          expect(empty.height, 0);
        }
      }
    },
  );

  testWidgets(
    'Omission and reset inherit actual theme layers while each local presence overrides only its own layer',
    (tester) async {
      final backgrounds = <Widget?>[];
      final foregrounds = <Widget?>[];
      final theme = ThemeData(
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ButtonStyle(
            backgroundBuilder: (context, states, child) {
              backgrounds.add(child);
              return child!;
            },
            foregroundBuilder: (context, states, child) {
              foregrounds.add(child);
              return child ?? const SizedBox.shrink();
            },
          ),
        ),
      );
      for (final mask in [0, 1, 2, 3, 0]) {
        backgrounds.clear();
        foregrounds.clear();
        await _pump(tester, _model(properties: _layers(mask)), theme: theme);
        expect(backgrounds.isNotEmpty, mask & 1 == 0);
        expect(foregrounds.isNotEmpty, mask & 2 == 0);
        if (backgrounds.isNotEmpty) {
          expect(backgrounds.every((child) => child is Padding), isTrue);
        }
        if (foregrounds.isNotEmpty) {
          expect(foregrounds.last, same(_sdk(tester).child));
        }
        expect(_material(tester).clipBehavior, Clip.antiAlias);
        expect(find.text('Layer action'), findsOneWidget);
        expect(tester.takeException(), isNull);
      }
      // A local unrelated style must not suppress inherited builder fields.
      await _pump(
        tester,
        _model(
          properties: {
            'styleElevation': {'kind': 'integer', 'value': 4},
          },
        ),
        theme: theme,
      );
      expect(_sdk(tester).style!.backgroundBuilder, isNull);
      expect(_sdk(tester).style!.foregroundBuilder, isNull);
      expect(_material(tester).clipBehavior, Clip.antiAlias);
      await _pump(tester, _model(empty: true), theme: theme);
      expect(foregrounds.last, isNull);
      expect(backgrounds.last, isA<Padding>());
      await _pump(tester, _model());
      expect(_material(tester).clipBehavior, Clip.none);
    },
  );

  testWidgets(
    'Presence and reset retain SDK state, child geometry, focus, hover, press and disabled transitions',
    (tester) async {
      final selected = <String>[];
      const properties = <String, Object?>{
        'autofocus': {'kind': 'boolean', 'value': true},
        'styleBackgroundColor': {'kind': 'color', 'argb': '0xFF111111'},
        'stylePressedBackgroundColor': {'kind': 'color', 'argb': '0xFF222222'},
        'styleHoveredBackgroundColor': {'kind': 'color', 'argb': '0xFF333333'},
        'styleFocusedBackgroundColor': {'kind': 'color', 'argb': '0xFF444444'},
        'styleDisabledBackgroundColor': {'kind': 'color', 'argb': '0xFF555555'},
      };
      await _pump(tester, _model(properties: properties), selected: selected);
      final state = tester.state(_button);
      final child = tester.element(find.text('Layer action'));
      final bounds = tester.getRect(_button);
      final controller = _controller(tester);
      expect(controller.value, contains(WidgetState.focused));
      for (final mask in [1, 3, 0]) {
        await _pump(
          tester,
          _model(properties: {...properties, ..._layers(mask)}),
          selected: selected,
        );
        expect(_controller(tester), same(controller));
        expect(controller.value, contains(WidgetState.focused));
      }
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer(location: Offset.zero);
      await mouse.moveTo(tester.getCenter(_button));
      await tester.pump(const Duration(milliseconds: 300));
      expect(controller.value, contains(WidgetState.hovered));
      for (final mask in [1, 3, 2, 0]) {
        final gesture = await tester.startGesture(tester.getCenter(_button));
        await tester.pump(const Duration(milliseconds: 120));
        expect(controller.value, contains(WidgetState.pressed));
        // Pointer interaction can change the SDK focus-highlight mode; the
        // model update must preserve exactly the already-active SDK states.
        final activeStates = Set<WidgetState>.of(controller.value);
        await _pump(
          tester,
          _model(properties: {...properties, ..._layers(mask)}),
          selected: selected,
        );
        expect(tester.state(_button), same(state));
        expect(tester.element(find.text('Layer action')), same(child));
        expect(tester.getRect(_button), bounds);
        expect(_controller(tester), same(controller));
        expect(controller.value, activeStates);
        expect(_material(tester).color, const Color(0xff222222));
        await gesture.up();
        await tester.pump(const Duration(milliseconds: 300));
      }
      await mouse.removePointer();
      await _pump(
        tester,
        _model(
          properties: {
            ...properties,
            ..._layers(3),
            'enabled': {'kind': 'boolean', 'value': false},
          },
        ),
      );
      expect(tester.state(_button), same(state));
      expect(controller.value, contains(WidgetState.disabled));
      expect(_sdk(tester).onPressed, isNull);
      expect(_material(tester).color, const Color(0xff555555));
      await _pump(tester, _model(properties: properties), selected: selected);
      expect(tester.state(_button), same(state));
      expect(controller.value, isNot(contains(WidgetState.disabled)));
      await tester.tap(find.text('Layer action'));
      expect(selected.last, _childId);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Identity layers add no padding dead zone and preserve long-press-only versus disabled callbacks',
    (tester) async {
      for (final mask in [0, 3]) {
        await _pump(tester, _model(properties: _layers(mask)));
        final bounds = tester.getRect(_button);
        // Real SDK padding outside the text must remain pressable.
        final gesture = await tester.startGesture(
          Offset(bounds.left + 12, bounds.center.dy),
        );
        await tester.pump(const Duration(milliseconds: 300));
        expect(_controller(tester).value, contains(WidgetState.pressed));
        await gesture.up();
        await _pump(
          tester,
          _model(
            properties: {
              ..._layers(mask),
              'onLongPress': {'kind': 'callbackPresence'},
            },
          ),
        );
        expect(_sdk(tester).onPressed, isNull);
        expect(_sdk(tester).onLongPress, isNotNull);
        await tester.longPress(_button);
        await _pump(
          tester,
          _model(
            properties: {
              ..._layers(mask),
              'enabled': {'kind': 'boolean', 'value': false},
              'onLongPress': {'kind': 'callbackPresence'},
            },
          ),
        );
        expect(_sdk(tester).onPressed, isNull);
        expect(_sdk(tester).onLongPress, isNull);
        expect(tester.takeException(), isNull);
      }
    },
  );
}

Map<String, Object?> _layers(int mask) => {
  for (var i = 0; i < 2; i++)
    if (mask & (1 << i) != 0) _names[i]: _presence,
};
Finder get _button => find.byKey(const ValueKey('canvas-elevated-button-$_id'));
Finder get _warning => find.byWidgetPredicate(
  (w) => w is Tooltip && (w.message?.contains('ElevatedButton.style') ?? false),
);
ElevatedButton _sdk(WidgetTester tester) =>
    tester.widget<ElevatedButton>(_button);
Material _material(WidgetTester tester) => tester.widget<Material>(
  find.descendant(of: _button, matching: find.byType(Material)).first,
);
WidgetStatesController _controller(WidgetTester tester) => tester
    .widget<InkWell>(
      find.descendant(of: _button, matching: find.byType(InkWell)).first,
    )
    .statesController!;
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
        selectedWidgetId: null,
        onSelected: selected?.add ?? (_) {},
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
  await tester.pump();
}

CanvasModel _decode(Map<String, Object?> json) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json))));
Map<String, Object?> _model({
  Map<String, Object?> properties = const {},
  bool empty = false,
}) {
  final json =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  json['root'] = {
    'id': '6e88bff4-8d73-48aa-92b5-87aa3344f6a7',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': {
          'id': '1d6c16df-df2b-41cf-b46d-2e1d85538277',
          'type': 'flutter.widgets.Center',
          'properties': <String, Object?>{},
          'slots': {
            'child': {
              'kind': 'single',
              'child': {
                'id': _id,
                'type': 'flutter.material.ElevatedButton',
                'properties': properties,
                'slots': {
                  'child': {
                    'kind': 'single',
                    'child': empty
                        ? null
                        : {
                            'id': _childId,
                            'type': 'flutter.widgets.Text',
                            'properties': {
                              'data': {
                                'kind': 'string',
                                'value': 'Layer action',
                              },
                            },
                            'slots': <String, Object?>{},
                          },
                  },
                },
              },
            },
          },
        },
      },
    },
  };
  return json;
}
