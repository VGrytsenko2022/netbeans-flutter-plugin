import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.MenuItemButton';
const _ref = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
String _id(int n) => '793560e4-67ae-4e99-907b-${n.toString().padLeft(12, '0')}';
Map<String, Object?> _s(String v) => {'kind': 'string', 'value': v};
Map<String, Object?> _b(bool v) => {'kind': 'boolean', 'value': v};
Map<String, Object?> _n(num v) => {
  'kind': v is int ? 'integer' : 'double',
  'value': v,
};
Map<String, Object?> _e(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _color(int value) => {
  'kind': 'color',
  'argb': '0x${value.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _paint() => {
  'kind': 'paint',
  'color': {'kind': 'literal', 'argb': '0xFF123456'},
  'style': 'fill',
  'strokeWidth': 0.0,
  'strokeCap': 'butt',
  'strokeJoin': 'miter',
  'strokeMiterLimit': 4.0,
  'antiAlias': true,
  'blendMode': 'srcOver',
  'filterQuality': 'none',
  'invertColors': false,
};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> props, [
  Map<String, Object?> slots = const {},
]) => {'id': _id(id), 'type': type, 'properties': props, 'slots': slots};
Map<String, Object?> _text(int id, String text) =>
    _node(id, 'flutter.widgets.Text', {'data': _s(text)});
Map<String, Object?> _button({
  Map<String, Object?> props = const {},
  bool slots = true,
}) => _node(
  2,
  _type,
  {'enabled': _b(true), ...props},
  slots
      ? {
          'child': _single(_text(3, 'Action')),
          'leadingIcon': _single(_text(4, 'L')),
          'trailingIcon': _single(_text(5, 'R')),
        }
      : {},
);
CanvasModel _model(
  Object root, {
  String platform = 'windows',
  String direction = 'ltr',
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  model['root'] = _node(900, 'flutter.material.Scaffold', {}, {
    'body': _single(
      _node(901, 'flutter.widgets.Center', {}, {
        'child': _single(
          _node(
            902,
            'flutter.widgets.Directionality',
            {'textDirection': _e('TextDirection', direction)},
            {'child': _single(root)},
          ),
        ),
      }),
    ),
  });
  return CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
}

Widget _canvas(
  Object root, {
  List<String>? selections,
  String? selected,
  String direction = 'ltr',
  ValueChanged<CanvasDropResolver?>? onDrop,
}) => CanvasDocumentView(
  model: _model(root, direction: direction),
  selectedWidgetId: selected,
  onSelected: selections?.add ?? (_) {},
  onDropResolverChanged: onDrop,
);
Future<void> _pump(
  WidgetTester tester,
  Object root, {
  List<String>? selections,
  String? selected,
  String direction = 'ltr',
  ValueChanged<CanvasDropResolver?>? onDrop,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: _canvas(
        root,
        selections: selections,
        selected: selected,
        direction: direction,
        onDrop: onDrop,
      ),
    ),
  );
  await tester.pumpAndSettle();
  expect(tester.takeException(), isNull);
}

MenuItemButton _sdk(WidgetTester tester) =>
    tester.widget<MenuItemButton>(find.byType(MenuItemButton));
String _section(String contract) {
  final start = contract.indexOf('W|$_type\n');
  return contract.substring(start, contract.indexOf('W|', start + 2));
}

String _diagnostics(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((w) => w.message ?? '')
    .join('\n');
Map<String, Object?> _allStyles() {
  final elevated = fixture.elevatedButtonPropertiesForViewTest();
  final result = <String, Object?>{
    for (final e in elevated.entries)
      if (e.key.startsWith('style')) e.key: e.value,
  };
  for (final prefix in [
    'styleError',
    'styleDragged',
    'styleSelected',
    'styleScrolledUnder',
  ]) {
    for (final e in elevated.entries.where(
      (e) => e.key.startsWith('stylePressed'),
    )) {
      result['$prefix${e.key.substring('stylePressed'.length)}'] = e.value;
    }
  }
  result.addAll({
    'styleIconAlignment': _e('IconAlignment', 'end'),
    'styleBackgroundBuilder': _ref,
    'styleForegroundBuilder': _ref,
  });
  return result;
}

void main() {
  testWidgets(
    'native horizontal unbounded menu labels remain naturally sized',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Row(
              children: [
                MenuItemButton(onPressed: () {}, child: const Text('Action')),
              ],
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      expect(tester.getSize(find.byType(MenuItemButton)).isFinite, true);
      final root = _node(10, 'flutter.widgets.Row', {}, {
        'children': {
          'kind': 'list',
          'children': [_button()],
        },
      });
      await _pump(tester, root);
      expect(
        _diagnostics(tester),
        isNot(contains('bounded horizontal layout')),
      );
      expect(tester.getSize(find.byType(MenuItemButton)).isFinite, true);
      final boundedVertical = _node(10, 'flutter.widgets.Row', {}, {
        'children': {
          'kind': 'list',
          'children': [
            _button(
              props: {
                'overflowAxis': _e('Axis', 'vertical'),
                'styleFixedWidth': _n(160),
              },
            ),
          ],
        },
      });
      await _pump(tester, boundedVertical);
      expect(
        _diagnostics(tester),
        isNot(contains('bounded horizontal layout')),
      );
      expect(
        tester.renderObject<RenderBox>(find.byType(MenuItemButton)).size.width,
        160,
      );
    },
  );
  test(
    'independent520-property schema native432key map and optionalthree-slot contract',
    () {
      final actual = canvasRuntimeWidgetSchemaContractForTesting();
      expect(_section(actual), _section(canvasReviewedWidgetSchemaContract));
      expect(
        _section(actual).split('\n').where((l) => l.startsWith('P|')),
        hasLength(520),
      );
      expect(utf8.encode(_section(actual)), hasLength(228002));
      expect(
        sha256Hex(utf8.encode(_section(actual))),
        'c4d521afeadabe85a8b8a682da1c5d3a95c3ffca50265afebbd9aa2654632226',
      );
      expect(canvasMenuShortcutKeyIds, hasLength(432));
      for (final id in canvasMenuShortcutKeyIds.values) {
        expect(LogicalKeyboardKey.findKeyByKeyId(id)!.keyId, id);
      }
      expect(canvasDropSlotsForWidgetType(_type), hasLength(3));
      expect(canvasReviewedRequiredWrapperSlot(_type), isNull);
    },
  );
  testWidgets(
    'native constructor defaults and optional slots contain no fabricated child',
    (tester) async {
      for (final slots in [false, true]) {
        await _pump(tester, _button(slots: slots));
        final sdk = _sdk(tester);
        expect(sdk.child == null, !slots);
        expect(sdk.leadingIcon == null, !slots);
        expect(sdk.trailingIcon == null, !slots);
        expect(sdk.enabled, true);
        expect(sdk.requestFocusOnHover, true);
        expect(sdk.closeOnActivate, true);
        expect(sdk.clipBehavior, Clip.none);
        expect(sdk.overflowAxis, Axis.horizontal);
        expect(sdk.autofocus, false);
        expect(sdk.shortcut, isNull);
        expect(sdk.style, isNull);
      }
    },
  );
  testWidgets(
    'real menu activation from label and icons closes root only when requested',
    (tester) async {
      for (final close in [true, false]) {
        for (final label in ['Action', 'L', 'R']) {
          final controller = MenuController();
          final selections = <String>[];
          await tester.pumpWidget(
            MaterialApp(
              home: Scaffold(
                body: MenuAnchor(
                  controller: controller,
                  menuChildren: [
                    SizedBox(
                      width: 450,
                      height: 550,
                      child: _canvas(
                        _button(props: {'closeOnActivate': _b(close)}),
                        selections: selections,
                      ),
                    ),
                  ],
                  builder: (context, controller, child) => TextButton(
                    onPressed: controller.open,
                    child: const Text('Open menu'),
                  ),
                ),
              ),
            ),
          );
          await tester.tap(find.text('Open menu'));
          await tester.pumpAndSettle();
          expect(controller.isOpen, true);
          expect(tester.takeException(), isNull);
          await tester.tap(find.text(label));
          await tester.pumpAndSettle();
          expect(
            controller.isOpen,
            !close,
            reason: 'native activation for $label',
          );
          expect(selections, hasLength(1));
          if (controller.isOpen) {
            controller.close();
            await tester.pumpAndSettle();
          }
        }
      }
    },
  );
  testWidgets(
    'all480compatiblelocalstylefields resolve all eight native states and preserve actual slots',
    (tester) async {
      final styles = _allStyles();
      expect(styles, hasLength(480));
      await _pump(tester, _button(props: styles, slots: false));
      final sdk = _sdk(tester);
      final style = sdk.style!;
      for (final states in [
        <WidgetState>{},
        for (final state in WidgetState.values) {state},
        WidgetState.values.toSet(),
      ]) {
        expect(style.textStyle!.resolve(states), isNotNull);
        expect(style.backgroundColor!.resolve(states), isNotNull);
        expect(style.foregroundColor!.resolve(states), isNotNull);
        expect(style.padding!.resolve(states), isNotNull);
        expect(style.minimumSize!.resolve(states), isNotNull);
        expect(style.maximumSize!.resolve(states), isNotNull);
        expect(style.fixedSize!.resolve(states), isNotNull);
        expect(style.shape!.resolve(states), isNotNull);
        expect(style.side!.resolve(states), isNotNull);
      }
      expect(style.backgroundBuilder, isNotNull);
      expect(style.foregroundBuilder, isNotNull);
      expect(sdk.clipBehavior, Clip.none);
      expect(_diagnostics(tester), contains('identity layers'));
      expect(sdk.child, isNull);
      expect(sdk.leadingIcon, isNull);
      expect(sdk.trailingIcon, isNull);
    },
  );
  testWidgets(
    'all432SingleActivator keys andCharacterActivator branches preserveexacthintswithoutregistration',
    (tester) async {
      for (final entry in canvasMenuShortcutKeyIds.entries) {
        await _pump(
          tester,
          _button(
            props: {
              'shortcutTrigger': _e('LogicalKeyboardKey', entry.key),
              'styleTextFontSize': _n(8.0),
              'overflowAxis': _e('Axis', 'vertical'),
              'shortcutControl': _b(true),
              'shortcutNumLock': _e('LockState', 'locked'),
              'shortcutIncludeRepeats': _b(false),
            },
            slots: false,
          ),
        );
        final shortcut = _sdk(tester).shortcut! as SingleActivator;
        expect(shortcut.trigger.keyId, entry.value);
        expect(shortcut.control, true);
        expect(shortcut.numLock, LockState.locked);
        expect(shortcut.includeRepeats, false);
      }
      for (final character in ['', '?', 'œ', 'ab', '🙂']) {
        await _pump(
          tester,
          _button(
            props: {
              'shortcutCharacter': _s(character),
              'styleTextFontSize': _n(8.0),
              'overflowAxis': _e('Axis', 'vertical'),
              'shortcutAlt': _b(true),
              'shortcutControl': _b(true),
              'shortcutMeta': _b(true),
              'shortcutIncludeRepeats': _b(false),
            },
            slots: false,
          ),
        );
        if (character.length != 1) {
          expect(_sdk(tester).shortcut, isNull);
          expect(_diagnostics(tester), contains('UTF-16 code unit'));
          expect(
            _model(_button(props: {'shortcutCharacter': _s(character)}))
                .root
                .slots['body']!
                .child!
                .slots['child']!
                .child!
                .slots['child']!
                .child!
                .properties['shortcutCharacter']!
                .value,
            character,
          );
          continue;
        }
        final shortcut = _sdk(tester).shortcut! as CharacterActivator;
        expect(shortcut.character, character);
        expect(shortcut.alt, true);
        expect(shortcut.control, true);
        expect(shortcut.meta, true);
        expect(shortcut.includeRepeats, false);
      }
    },
  );

  testWidgets('all498 local styles include every circle and Paint bucket', (
    tester,
  ) async {
    final complementary = <String, Object?>{};
    for (final prefix in [
      'style',
      for (final state in WidgetState.values)
        'style${state.name[0].toUpperCase()}${state.name.substring(1)}',
    ]) {
      complementary.addAll({
        '${prefix}ShapeKind': _s('circle'),
        '${prefix}ShapeCircleEccentricity': _n(.4),
        '${prefix}TextBackground': _paint(),
      });
    }
    await _pump(tester, _button(props: complementary));
    final style = _sdk(tester).style!;
    for (final states in [
      <WidgetState>{},
      for (final state in WidgetState.values) {state},
    ]) {
      expect((style.shape!.resolve(states)! as CircleBorder).eccentricity, .4);
      expect(
        style.textStyle!.resolve(states)!.background!.color.toARGB32(),
        0xff123456,
      );
    }
    expect({..._allStyles().keys, ...complementary.keys}, hasLength(498));
  });

  testWidgets(
    'whole references and every nullable direct field are inert and reset sparsely',
    (tester) async {
      for (final name in [
        'onPressed',
        'onHover',
        'onFocusChange',
        'focusNode',
        'statesController',
        'style',
        'shortcut',
        'styleBackgroundBuilder',
        'styleForegroundBuilder',
      ]) {
        for (final value in [
          _ref,
          if (!{
            'onPressed',
            'styleBackgroundBuilder',
            'styleForegroundBuilder',
          }.contains(name))
            _null,
        ]) {
          await _pump(tester, _button(props: {name: value}));
          final sdk = _sdk(tester);
          expect(sdk.onPressed, isNotNull);
          expect(sdk.focusNode, isNull);
          expect(sdk.statesController, isNull);
          expect(sdk.shortcut, isNull);
          if (name == 'style') expect(sdk.style, isNull);
          if (name == 'onHover') {
            expect(sdk.onHover != null, identical(value, _ref));
          }
          if (name == 'onFocusChange') {
            expect(sdk.onFocusChange != null, identical(value, _ref));
          }
          if (identical(value, _ref)) {
            expect(_diagnostics(tester), contains('preview'));
          }
          expect(find.text('Action'), findsOneWidget);
          expect(find.text('L'), findsOneWidget);
          expect(find.text('R'), findsOneWidget);
        }
      }
      await _pump(tester, _button(props: {'semanticsLabel': _null}));
      expect(_sdk(tester).semanticsLabel, isNull);
      await _pump(tester, _button());
      expect(_sdk(tester).style, isNull);
      expect(_sdk(tester).onHover, isNull);
      expect(_sdk(tester).onFocusChange, isNull);
    },
  );

  test(
    'malformed shortcut and style combinations fail closed at the wire boundary',
    () {
      for (final props in <Map<String, Object?>>[
        {'shortcutTrigger': _e('LogicalKeyboardKey', 'control')},
        {'shortcutTrigger': _e('LogicalKeyboardKey', 'notAKey')},
        {'shortcutTrigger': _s('keyA')},
        {'shortcutTrigger': _null},
        {'shortcutShift': _b(false)},
        {'shortcutCharacter': _s('A'), 'shortcutShift': _b(false)},
        {
          'shortcutCharacter': _s('A'),
          'shortcutNumLock': _e('LockState', 'ignored'),
        },
        {
          'shortcutCharacter': _s('A'),
          'shortcutTrigger': _e('LogicalKeyboardKey', 'keyA'),
        },
        {
          'shortcut': _null,
          'shortcutTrigger': _e('LogicalKeyboardKey', 'keyA'),
        },
        {'shortcut': _ref, 'shortcutCharacter': _s('A')},
        {'style': _null, 'styleEnableFeedback': _b(false)},
        {'style': _ref, 'styleBackgroundBuilder': _ref},
        {'enabled': _null},
        {'onPressed': _null},
        {
          'onPressed': {'kind': 'callbackPresence'},
        },
        {'onLongPress': _ref},
        {'variant': _s('icon')},
      ]) {
        expect(
          () => _model(_button(props: props)),
          throwsFormatException,
          reason: props.toString(),
        );
      }
      final missing = _button();
      (missing['properties'] as Map).clear();
      expect(() => _model(missing), throwsFormatException);
    },
  );

  testWidgets(
    'native MenuButtonTheme and M3 defaults win over TextButtonTheme on M2 and M3',
    (tester) async {
      for (final material3 in [false, true]) {
        for (final local in [false, true]) {
          await _pump(
            tester,
            _button(
              props: {if (local) 'styleBackgroundColor': _color(0xff123456)},
            ),
          );
          final sdk = _sdk(tester);
          await tester.pumpWidget(
            MaterialApp(
              theme: ThemeData(
                useMaterial3: material3,
                menuButtonTheme: const MenuButtonThemeData(
                  style: ButtonStyle(
                    backgroundColor: WidgetStatePropertyAll(Color(0xffabcdef)),
                  ),
                ),
                textButtonTheme: const TextButtonThemeData(
                  style: ButtonStyle(
                    backgroundColor: WidgetStatePropertyAll(Colors.red),
                  ),
                ),
              ),
              home: Scaffold(body: SizedBox(width: 360, child: sdk)),
            ),
          );
          await tester.pumpAndSettle();
          final button = tester.widget<TextButton>(find.byType(TextButton));
          expect(
            button.style!.backgroundColor!.resolve({}),
            Color(local ? 0xff123456 : 0xffabcdef),
          );
          expect(button.clipBehavior, Clip.none);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  testWidgets(
    'native focus hover press and disabled state remain controlled without project execution',
    (tester) async {
      final selections = <String>[];
      final props = {
        'onPressed': _ref,
        'onHover': _ref,
        'onFocusChange': _ref,
        'autofocus': _b(true),
        'styleBackgroundColor': _color(0xff111111),
        'stylePressedBackgroundColor': _color(0xff222222),
        'styleHoveredBackgroundColor': _color(0xff333333),
        'styleFocusedBackgroundColor': _color(0xff444444),
        'styleDisabledBackgroundColor': _color(0xff555555),
      };
      await _pump(tester, _button(props: props), selections: selections);
      final menuState = tester.state(find.byType(MenuItemButton));
      final ink = tester.widget<InkWell>(
        find
            .descendant(
              of: find.byType(MenuItemButton),
              matching: find.byType(InkWell),
            )
            .first,
      );
      final states = ink.statesController!;
      expect(states.value, contains(WidgetState.focused));
      final mouse = await tester.createGesture(
        kind: ui.PointerDeviceKind.mouse,
      );
      await mouse.addPointer(location: Offset.zero);
      await mouse.moveTo(tester.getCenter(find.text('Action')));
      await tester.pumpAndSettle();
      expect(states.value, contains(WidgetState.hovered));
      final gesture = await tester.startGesture(
        tester.getCenter(find.text('Action')),
      );
      await tester.pump(const Duration(milliseconds: 250));
      expect(states.value, contains(WidgetState.pressed));
      final material = tester.widget<Material>(
        find
            .descendant(
              of: find.byType(TextButton),
              matching: find.byType(Material),
            )
            .first,
      );
      expect(material.color, const Color(0xff222222));
      await gesture.up();
      await mouse.removePointer();
      await _pump(
        tester,
        _button(props: {...props, 'enabled': _b(false)}),
        selections: selections,
      );
      expect(tester.state(find.byType(MenuItemButton)), same(menuState));
      expect(_sdk(tester).enabled, false);
      expect(_sdk(tester).onHover, isNotNull);
      expect(_sdk(tester).onFocusChange, isNotNull);
      final disabled = tester.widget<TextButton>(find.byType(TextButton));
      expect(disabled.onPressed, isNull);
      expect(disabled.onFocusChange, isNull);
      expect(disabled.autofocus, false);
      expect(
        ((_model(_button(props: props))
                .root
                .slots['body']!
                .child!
                .slots['child']!
                .child!
                .slots['child']!
                .child!)
            .properties['enabled']!
            .value),
        true,
      );
      expect(selections, hasLength(1));
    },
  );

  testWidgets(
    'native parent menu axis overrides standalone overflowAxis without a Canvas menu provider',
    (tester) async {
      for (final host in [
        'standaloneHorizontal',
        'standaloneVertical',
        'anchor',
        'bar',
      ]) {
        final vertical = host == 'standaloneVertical' || host == 'bar';
        await _pump(
          tester,
          _button(
            props: {
              'overflowAxis': _e('Axis', vertical ? 'vertical' : 'horizontal'),
            },
          ),
        );
        final sdk = _sdk(tester);
        final controller = MenuController();
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: switch (host) {
                'anchor' => MenuAnchor(
                  controller: controller,
                  menuChildren: [sdk],
                  builder: (context, controller, child) => TextButton(
                    onPressed: controller.open,
                    child: const Text('Host'),
                  ),
                ),
                'bar' => MenuBar(children: [sdk]),
                _ => SizedBox(width: 360, child: sdk),
              },
            ),
          ),
        );
        await tester.pumpAndSettle();
        if (host == 'anchor') {
          controller.open();
          await tester.pumpAndSettle();
        }
        expect(
          find
              .ancestor(
                of: find.text('Action'),
                matching: find.byType(Expanded),
              )
              .evaluate()
              .isNotEmpty,
          host == 'anchor' || host == 'standaloneVertical',
          reason: host,
        );
        expect(tester.takeException(), isNull);
        if (controller.isOpen) {
          controller.close();
          await tester.pumpAndSettle();
        }
      }
    },
  );

  testWidgets(
    'all three empty insertion zones follow RTL without fabricated native slots',
    (tester) async {
      CanvasDropResolver? resolver;
      for (final direction in ['ltr', 'rtl']) {
        await _pump(
          tester,
          _button(slots: false),
          direction: direction,
          onDrop: (value) => resolver = value,
        );
        final button = tester.getRect(find.byType(MenuItemButton));
        final surface = tester.getRect(
          find.byKey(const ValueKey('canvas-interaction-surface')),
        );
        CanvasDropTarget? target(double fraction) {
          final point = Offset(
            button.left + button.width * fraction,
            button.center.dy,
          );
          return resolver!(
            ((point.dx - surface.left) / surface.width * 1000000).round(),
            ((point.dy - surface.top) / surface.height * 1000000).round(),
            CanvasPaletteDragSource(
              token: 'text',
              widgetType: 'flutter.widgets.Text',
              traits: {},
            ),
          );
        }

        expect(
          target(.1)?.slotName,
          direction == 'ltr' ? 'leadingIcon' : 'trailingIcon',
        );
        expect(target(.5)?.slotName, 'child');
        expect(
          target(.9)?.slotName,
          direction == 'ltr' ? 'trailingIcon' : 'leadingIcon',
        );
        for (final fraction in [.1, .5, .9]) {
          expect(target(fraction)?.parentWidgetId, _id(2));
          expect(target(fraction)?.insertionIndex, 0);
        }
        final native = _sdk(tester);
        expect(native.child, isNull);
        expect(native.leadingIcon, isNull);
        expect(native.trailingIcon, isNull);
        await _pump(
          tester,
          _button(),
          direction: direction,
          onDrop: (value) => resolver = value,
        );
        expect(
          tester.getCenter(find.text('L')).dx <
              tester.getCenter(find.text('Action')).dx,
          direction == 'ltr',
        );
        expect(
          tester.getCenter(find.text('R')).dx >
              tester.getCenter(find.text('Action')).dx,
          direction == 'ltr',
        );
      }
    },
  );

  testWidgets(
    'native semantics override replaces label and icons while disabled remains disabled',
    (tester) async {
      final handle = tester.ensureSemantics();
      for (final enabled in [true, false]) {
        await _pump(
          tester,
          _button(
            props: {
              'enabled': _b(enabled),
              'semanticsLabel': _s('Accessible action'),
            },
          ),
        );
        final semantics = tester
            .getSemantics(find.byType(MenuItemButton))
            .getSemanticsData();
        expect(semantics.label, 'Accessible action');
        expect(
          semantics.flagsCollection.isEnabled,
          enabled ? ui.Tristate.isTrue : ui.Tristate.isFalse,
        );
        expect(semantics.flagsCollection.isToggled, ui.Tristate.none);
      }
      await _pump(tester, _button());
      final label = tester
          .getSemantics(find.byType(MenuItemButton))
          .getSemanticsData()
          .label;
      expect(label, contains('Action'));
      expect(label, contains('L'));
      expect(label, contains('R'));
      handle.dispose();
    },
  );

  testWidgets(
    'selection and reference diagnostics preserve native geometry intrinsics and baseline',
    (tester) async {
      List<Object?>? baseline;
      State? state;
      for (final selected in [null, _id(2), _id(3), _id(901), null]) {
        for (final warn in [false, true]) {
          await _pump(
            tester,
            _button(props: {if (warn) 'shortcut': _ref}),
            selected: selected,
          );
          final box = tester.renderObject<RenderBox>(
            find.byType(MenuItemButton),
          );
          final previous = RenderObject.debugCheckingIntrinsics;
          RenderObject.debugCheckingIntrinsics = true;
          final double? actual;
          try {
            actual = box.getDistanceToBaseline(
              TextBaseline.alphabetic,
              onlyReal: true,
            );
          } finally {
            RenderObject.debugCheckingIntrinsics = previous;
          }
          final geometry = <Object?>[
            tester.getRect(find.byType(MenuItemButton)),
            tester.getRect(find.text('Action')),
            box.getMinIntrinsicWidth(48),
            box.getMaxIntrinsicWidth(48),
            box.getMinIntrinsicHeight(390),
            box.getMaxIntrinsicHeight(390),
            actual,
          ];
          baseline ??= geometry;
          state ??= tester.state(find.byType(MenuItemButton));
          expect(geometry, baseline);
          expect(tester.state(find.byType(MenuItemButton)), same(state));
        }
      }
    },
  );

  testWidgets(
    'unbounded and too narrow native layouts are diagnosed without losing real children or stale drop geometry',
    (tester) async {
      CanvasDropResolver? resolver;
      final child = _node(3, 'flutter.widgets.SizedBox', {
        'width': _n(100),
        'height': _n(20),
      });
      Map<String, Object?> button() => _node(
        2,
        _type,
        {'enabled': _b(true), 'overflowAxis': _e('Axis', 'vertical')},
        {'child': _single(child)},
      );
      Map<String, Object?> root(bool bounded) =>
          _node(10, 'flutter.widgets.Row', {}, {
            'children': {
              'kind': 'list',
              'children': [
                if (bounded)
                  _node(
                    11,
                    'flutter.widgets.SizedBox',
                    {'width': _n(200)},
                    {'child': _single(button())},
                  )
                else
                  button(),
              ],
            },
          });
      await _pump(tester, root(false), onDrop: (value) => resolver = value);
      expect(_diagnostics(tester), contains('bounded horizontal layout'));
      expect(find.byType(MenuItemButton), findsOneWidget);
      expect(find.byKey(ValueKey('canvas-widget-${_id(3)}')), findsOneWidget);
      await _pump(tester, root(true), onDrop: (value) => resolver = value);
      expect(
        _diagnostics(tester),
        isNot(contains('geometry preview unavailable')),
      );
      final state = tester.state(find.byType(MenuItemButton));
      // A valid fixed size can be too narrow for a native horizontal label. The
      // guard retains that exact SDK configuration while withholding its paint.
      await _pump(
        tester,
        _button(props: {'styleFixedWidth': _n(10)}),
        onDrop: (value) => resolver = value,
      );
      expect(
        _diagnostics(tester),
        contains('exceeds the available layout extent'),
      );
      expect(_sdk(tester).style!.fixedSize!.resolve({})!.width, 10);
      final surface = tester.getRect(
        find.byKey(const ValueKey('canvas-interaction-surface')),
      );
      final textRect = tester.getRect(find.text('Action'));
      final point = textRect.center;
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'text',
          widgetType: 'flutter.widgets.Text',
          traits: {},
        ),
      );
      expect(target?.parentWidgetId, isNot(_id(3)));
      expect(tester.takeException(), isNull);
      await _pump(tester, _button());
      expect(
        _diagnostics(tester),
        isNot(contains('geometry preview unavailable')),
      );
      expect(tester.state(find.byType(MenuItemButton)), same(state));
      for (final direction in ['ltr', 'rtl']) {
        await _pump(
          tester,
          _button(props: {'styleFixedWidth': _n(10)}),
          direction: direction,
        );
        expect(
          _diagnostics(tester),
          contains('exceeds the available layout extent'),
        );
        expect(_diagnostics(tester), contains('pointer input'));
        await _pump(tester, _button(), direction: direction);
        expect(
          _diagnostics(tester),
          isNot(contains('geometry preview unavailable')),
        );
        expect(tester.state(find.byType(MenuItemButton)), same(state));
      }
    },
  );
}
