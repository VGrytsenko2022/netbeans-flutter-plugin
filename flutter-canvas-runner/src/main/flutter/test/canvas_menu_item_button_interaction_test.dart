import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _message = 'Nested live menu tooltip';
const _ref = {'kind': 'dartObjectReferencePresence'};
String _id(int n) => '342f5c71-f471-4450-9e8b-${n.toString().padLeft(12, '0')}';
Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> _b(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _e(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> properties, [
  Map<String, Object?> slots = const {},
]) => {'id': _id(id), 'type': type, 'properties': properties, 'slots': slots};
Map<String, Object?> _text(int id, String text) =>
    _node(id, 'flutter.widgets.Text', {'data': _s(text)});
Map<String, Object?> _menu(
  Map<String, Object?> properties, {
  bool leading = true,
}) => _node(
  2,
  'flutter.material.MenuItemButton',
  {'enabled': _b(true), 'onPressed': _ref, ...properties},
  {
    'child': _single(
      _node(
        3,
        'flutter.material.Tooltip',
        {
          'message': _s(_message),
          'triggerMode': _e('TooltipTriggerMode', 'manual'),
        },
        {
          'child': _single(
            _node(
              4,
              'flutter.widgets.SizedBox',
              {
                'width': {'kind': 'double', 'value': 150.0},
                'height': {'kind': 'double', 'value': 42.0},
              },
              {'child': _single(_node(5, 'flutter.material.TextField', {}))},
            ),
          ),
        },
      ),
    ),
    'leadingIcon': _single(leading ? _text(6, 'L') : null),
    'trailingIcon': _single(_text(7, 'R')),
  },
);
CanvasModel _model(Object root) {
  final envelope =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  envelope['root'] = _node(900, 'flutter.material.Scaffold', {}, {
    'body': _single(
      _node(901, 'flutter.widgets.Center', {}, {'child': _single(root)}),
    ),
  });
  return CanvasModel.decode(
    Uint8List.fromList(utf8.encode(jsonEncode(envelope))),
  );
}

Finder _anchor(int id) => find.byKey(ValueKey('canvas-widget-${_id(id)}'));
Finder get _tooltip => find.byWidgetPredicate(
  (widget) =>
      widget is Tooltip &&
      widget.key is GlobalKey &&
      widget.message == _message,
);

Future<void> _open(
  WidgetTester tester,
  MenuController controller,
  ValueNotifier<Map<String, Object?>> tree, {
  List<String>? selections,
  ValueChanged<CanvasDropResolver?>? onDrop,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: MenuAnchor(
          controller: controller,
          menuChildren: [
            SizedBox(
              width: 450,
              height: 550,
              child: ValueListenableBuilder<Map<String, Object?>>(
                valueListenable: tree,
                builder: (context, value, child) => CanvasDocumentView(
                  model: _model(value),
                  selectedWidgetId: selections?.lastOrNull,
                  onSelected: selections?.add ?? (_) {},
                  onDropResolverChanged: onDrop,
                ),
              ),
            ),
          ],
          builder: (context, controller, child) => TextButton(
            onPressed: controller.open,
            child: const Text('Open host menu'),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text('Open host menu'));
  await tester.pumpAndSettle();
  expect(controller.isOpen, true);
  expect(tester.takeException(), isNull);
}

void main() {
  testWidgets(
    'active native menu retains tooltip field state and all child IDs through scalar shortcut selection and reference diagnostics without registering keys',
    (tester) async {
      final controller = MenuController();
      final tree = ValueNotifier(_menu({}));
      final selections = <String>[];
      CanvasDropResolver? resolver;
      await _open(
        tester,
        controller,
        tree,
        selections: selections,
        onDrop: (value) => resolver = value,
      );
      final itemState = tester.state(find.byType(MenuItemButton));
      final fieldState = tester.state<EditableTextState>(
        find.byType(EditableText),
      );
      final tooltipState = tester.state<TooltipState>(_tooltip);
      fieldState.widget.controller.text = 'Retain live field text';
      final button = tester.widget<TextButton>(
        find
            .descendant(
              of: find.byType(MenuItemButton),
              matching: find.byType(TextButton),
            )
            .first,
      );
      final focus = button.focusNode!;
      focus.requestFocus();
      await tester.pump();
      expect(focus.hasFocus, true);
      expect(tooltipState.ensureTooltipVisible(), true);
      await tester.pumpAndSettle();
      for (final properties in <Map<String, Object?>>[
        {
          'styleForegroundColor': {'kind': 'color', 'argb': '0xFF245678'},
        },
        {
          'shortcutTrigger': _e('LogicalKeyboardKey', 'keyK'),
          'shortcutControl': _b(true),
        },
        {'shortcutCharacter': _s('k'), 'shortcutControl': _b(true)},
        {'shortcut': _ref},
        {},
      ]) {
        final sourceTree = _menu(properties);
        final expected = jsonEncode(sourceTree);
        selections.add(_id(6));
        tree.value = sourceTree;
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        expect(controller.isOpen, true);
        expect(tester.state(find.byType(MenuItemButton)), same(itemState));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(fieldState),
        );
        expect(tester.state<TooltipState>(_tooltip), same(tooltipState));
        expect(find.text(_message), findsOneWidget);
        expect(fieldState.widget.controller.text, 'Retain live field text');
        expect(fieldState.widget.focusNode.canRequestFocus, false);
        expect(focus.hasFocus, true);
        for (final id in [2, 3, 5, 6, 7]) {
          expect(_anchor(id), findsOneWidget);
        }
        expect(jsonEncode(sourceTree), expected);
        expect(resolver, isNotNull);
        final count = selections.length;
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.keyK);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pumpAndSettle();
        expect(
          controller.isOpen,
          true,
          reason:
              'A display-only shortcut must not activate the native menu item.',
        );
        expect(selections, hasLength(count));
        expect(tester.takeException(), isNull);
      }
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      expect(resolver, isNull);
      expect(tester.takeException(), isNull);
      tree.dispose();
    },
  );

  for (final branch in [
    'onHover',
    'onFocusChange',
    'requestFocusOnHover',
    'semanticsLabel',
    'overflowAxis',
    'leadingIcon',
  ]) {
    testWidgets(
      'open nested tooltip tolerates MenuItemButton $branch topology transition and return without stale overlays',
      (tester) async {
        final controller = MenuController();
        final tree = ValueNotifier(_menu({}));
        CanvasDropResolver? resolver;
        await _open(
          tester,
          controller,
          tree,
          onDrop: (value) => resolver = value,
        );
        for (final changed in [true, false]) {
          final before = tester.state<TooltipState>(_tooltip);
          before.ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(find.text(_message), findsOneWidget);
          final properties = <String, Object?>{
            if (changed && (branch == 'onHover' || branch == 'onFocusChange'))
              branch: _ref,
            if (changed && branch == 'requestFocusOnHover') branch: _b(false),
            if (changed && branch == 'semanticsLabel')
              branch: _s('Menu semantics override'),
            if (changed && branch == 'overflowAxis')
              branch: _e('Axis', 'vertical'),
          };
          final sourceTree = _menu(
            properties,
            leading: branch != 'leadingIcon' || !changed,
          );
          final expected = jsonEncode(sourceTree);
          tree.value = sourceTree;
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull, reason: '$branch=$changed');
          expect(controller.isOpen, true);
          for (final id in [2, 3, 5, 7]) {
            expect(_anchor(id), findsOneWidget);
          }
          expect(
            _anchor(6),
            branch == 'leadingIcon' && changed ? findsNothing : findsOneWidget,
          );
          expect(jsonEncode(sourceTree), expected);
          expect(resolver, isNotNull);
          final field = tester.state<EditableTextState>(
            find.byType(EditableText),
          );
          expect(field.widget.focusNode.canRequestFocus, false);
          // Real ancestor wrapper changes may reset SDK state. Any retained
          // Tooltip must still have a live anchor and a single usable overlay.
          tester.state<TooltipState>(_tooltip).ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(find.text(_message), findsOneWidget);
          expect(tester.takeException(), isNull);
        }
        await tester.pumpWidget(const SizedBox.shrink());
        await tester.pumpAndSettle();
        await tester.pump(const Duration(seconds: 2));
        expect(resolver, isNull);
        expect(find.text(_message), findsNothing);
        expect(tester.takeException(), isNull);
        tree.dispose();
      },
    );
  }
}
