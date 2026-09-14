import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_menu_anchor_test.dart';

const _message = 'Stateful content inside the open menu';

Map<String, Object?> _tree([Map<String, Object?> properties = const {}]) =>
    menuAnchor(
      props: properties,
      items: [
        menuNode(
          9,
          'flutter.material.MenuItemButton',
          {'enabled': menuB(true), 'closeOnActivate': menuB(false)},
          {
            'child': menuSingle(
              menuNode(
                10,
                'flutter.material.Tooltip',
                {
                  'message': menuS(_message),
                  'triggerMode': menuE('TooltipTriggerMode', 'manual'),
                },
                {
                  'child': menuSingle(
                    menuNode(
                      11,
                      'flutter.widgets.SizedBox',
                      {'width': menuN(150.0), 'height': menuN(42.0)},
                      {
                        'child': menuSingle(
                          menuNode(12, 'flutter.material.TextField', {}),
                        ),
                      },
                    ),
                  ),
                },
              ),
            ),
          },
        ),
        menuItem(20, 'Retained second item', close: false),
      ],
    );

Finder _anchor(int id) => find.byKey(ValueKey('canvas-widget-${menuId(id)}'));
Finder get _tooltip => find.byWidgetPredicate(
  (widget) =>
      widget is Tooltip &&
      widget.key is GlobalKey &&
      widget.message == _message,
);

void main() {
  testWidgets(
    'open menu preserves native controller tooltip field state and focus through scalar selection and reference diagnostics',
    (tester) async {
      final selections = <String>[];
      CanvasDropResolver? resolver;
      await pumpMenu(
        tester,
        _tree(),
        selections: selections,
        onDrop: (value) => resolver = value,
      );
      await openMenu(tester);
      final menuState = tester.state(find.byType(MenuAnchor));
      final controller = sdkMenu(tester).controller!;
      final field = tester.state<EditableTextState>(find.byType(EditableText));
      field.widget.controller.text = 'Keep source-independent preview state';
      final tooltip = tester.state<TooltipState>(_tooltip);
      final button = tester.widget<TextButton>(
        find
            .descendant(of: _anchor(9), matching: find.byType(TextButton))
            .first,
      );
      final focus = button.focusNode!;
      focus.requestFocus();
      await tester.pump();
      expect(focus.hasFocus, true);
      expect(tooltip.ensureTooltipVisible(), true);
      await tester.pumpAndSettle();
      for (final properties in <Map<String, Object?>>[
        {
          'styleBackgroundColor': {'kind': 'color', 'argb': '0xFFDEF1FC'},
        },
        {'consumeOutsideTap': menuB(true)},
        {'controller': menuRef, 'builder': menuRef, 'style': menuRef},
        {'useRootOverlay': menuB(true)},
        {},
      ]) {
        final tree = _tree(properties);
        final stored = jsonEncode(tree);
        final count = selections.length;
        await pumpMenu(
          tester,
          tree,
          selected: menuId(12),
          selections: selections,
          onDrop: (value) => resolver = value,
        );
        expect(tester.state(find.byType(MenuAnchor)), same(menuState));
        expect(sdkMenu(tester).controller, same(controller));
        expect(controller.isOpen, true);
        expect(tester.state<TooltipState>(_tooltip), same(tooltip));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(field),
        );
        expect(
          field.widget.controller.text,
          'Keep source-independent preview state',
        );
        expect(field.widget.focusNode.canRequestFocus, false);
        expect(focus.hasFocus, true);
        expect(find.text(_message), findsOneWidget);
        for (final id in [2, 3, 4, 9, 10, 11, 12, 20, 21]) {
          expect(_anchor(id), findsOneWidget);
        }
        expect(jsonEncode(tree), stored);
        expect(selections, hasLength(count));
        expect(resolver, isNotNull);
        expect(tester.takeException(), isNull);
      }
      controller.close();
      await tester.pumpAndSettle();
      expect(_anchor(12), findsNothing);
      expect(find.text(_message), findsNothing);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      await tester.pump(const Duration(seconds: 2));
      expect(resolver, isNull);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'open linked menu adds and removes Tooltip fallback without losing model IDs or leaving overlays',
    (tester) async {
      CanvasDropResolver? resolver;
      for (final tooltipPresent in [false, true, false]) {
        final tree = _tree({'layerLink': menuRef});
        if (!tooltipPresent) {
          final slots = tree['slots'] as Map;
          final items = (slots['menuChildren'] as Map)['children'] as List;
          final itemSlots = (items.first as Map)['slots'] as Map;
          final tooltip = (itemSlots['child'] as Map)['child'] as Map;
          itemSlots['child'] = (tooltip['slots'] as Map)['child'];
        }
        final stored = jsonEncode(tree);
        await pumpMenu(tester, tree, onDrop: (value) => resolver = value);
        if (!sdkMenu(tester).controller!.isOpen) await openMenu(tester);
        expect(sdkMenu(tester).layerLink == null, tooltipPresent);
        if (tooltipPresent) {
          expect(
            menuDiagnostics(tester),
            contains('follower preview is unavailable'),
          );
          tester.state<TooltipState>(_tooltip).ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(find.text(_message), findsOneWidget);
        } else {
          expect(find.text(_message), findsNothing);
        }
        for (final id in [2, 3, 4, 9, 11, 12, 20, 21]) {
          expect(_anchor(id), findsOneWidget);
        }
        expect(jsonEncode(tree), stored);
        expect((tree['properties'] as Map)['layerLink'], menuRef);
        expect(resolver, isNotNull);
        expect(tester.takeException(), isNull);
      }
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      await tester.pump(const Duration(seconds: 2));
      expect(resolver, isNull);
      expect(find.text(_message), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'F2 inline editor in native menu retains View focus draft and controller across warning and style edits',
    (tester) async {
      final selections = <String>[];
      Map<String, Object?> tree(Map<String, Object?> properties) => menuAnchor(
        props: properties,
        items: [menuItem(10, 'Editable menu label', close: false)],
      );
      Future<void> pump(Map<String, Object?> properties, int selected) async {
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: menuModel(tree(properties)),
              selectedWidgetId: menuId(selected),
              onSelected: selections.add,
              inlineTextEditEnabled: true,
            ),
          ),
        );
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
      }

      await pump({}, 2);
      await openMenu(tester);
      final controller = sdkMenu(tester).controller!;
      await tester.tap(_anchor(11));
      await tester.pump();
      expect(selections.last, menuId(11));
      await pump({}, 11);
      await tester.sendKeyEvent(LogicalKeyboardKey.f2);
      await tester.pumpAndSettle();
      final editor = find.byKey(
        ValueKey('canvas-inline-text-editor-${menuId(11)}'),
      );
      expect(editor, findsOneWidget);
      final state = tester.state<EditableTextState>(find.byType(EditableText));
      final view = View.of(tester.element(editor));
      expect(state.widget.focusNode.hasFocus, true);
      state.widget.controller.text = 'Uncommitted menu label draft';
      for (final properties in <Map<String, Object?>>[
        {'builder': menuRef, 'controller': menuRef},
        {
          'styleBackgroundColor': {'kind': 'color', 'argb': '0xFFDDEEFF'},
        },
        {},
      ]) {
        await pump(properties, 11);
        expect(sdkMenu(tester).controller, same(controller));
        expect(controller.isOpen, true);
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(state),
        );
        expect(state.widget.focusNode.hasFocus, true);
        expect(state.widget.controller.text, 'Uncommitted menu label draft');
        expect(View.of(tester.element(editor)), same(view));
      }
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pumpAndSettle();
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    },
  );

  for (final branch in ['animated', 'layerLink']) {
    testWidgets(
      'open menu with nested tooltip safely switches $branch topology and returns without losing stored IDs or ghost overlays',
      (tester) async {
        CanvasDropResolver? resolver;
        await pumpMenu(tester, _tree(), onDrop: (value) => resolver = value);
        await openMenu(tester);
        for (final active in [true, false]) {
          tester.state<TooltipState>(_tooltip).ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(find.text(_message), findsOneWidget);
          final tree = _tree({
            if (active) branch: branch == 'animated' ? menuB(true) : menuRef,
          });
          final stored = jsonEncode(tree);
          await pumpMenu(tester, tree, onDrop: (value) => resolver = value);
          if (!sdkMenu(tester).controller!.isOpen) await openMenu(tester);
          for (final id in [2, 3, 4, 9, 10, 11, 12, 20, 21]) {
            expect(_anchor(id), findsOneWidget);
          }
          expect(jsonEncode(tree), stored);
          expect(
            tester
                .state<EditableTextState>(find.byType(EditableText))
                .widget
                .focusNode
                .canRequestFocus,
            false,
          );
          tester.state<TooltipState>(_tooltip).ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(find.text(_message), findsOneWidget);
          expect(resolver, isNotNull);
          expect(tester.takeException(), isNull);
        }
        await tester.pumpWidget(const SizedBox.shrink());
        await tester.pumpAndSettle();
        await tester.pump(const Duration(seconds: 2));
        expect(find.text(_message), findsNothing);
        expect(resolver, isNull);
        expect(tester.takeException(), isNull);
      },
    );
  }
}
