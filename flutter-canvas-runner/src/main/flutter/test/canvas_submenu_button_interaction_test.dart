import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_menu_anchor_test.dart';
import 'canvas_submenu_button_test.dart';

const _message = 'Tooltip in the nested submenu';

Map<String, Object?> _tree({
  Map<String, Object?> properties = const {},
  bool empty = false,
  bool inline = false,
}) => submenu(
  child: menuText(3, 'Outer submenu'),
  items: [
    submenu(
      id: 6,
      props: properties,
      child: menuText(7, 'Inner submenu'),
      items: empty
          ? []
          : [
              menuNode(
                9,
                'flutter.material.MenuItemButton',
                {'enabled': menuB(true), 'closeOnActivate': menuB(false)},
                {
                  'child': menuSingle(
                    inline
                        ? menuText(12, 'Editable nested label')
                        : menuNode(
                            10,
                            'flutter.material.Tooltip',
                            {
                              'message': menuS(_message),
                              'triggerMode': menuE(
                                'TooltipTriggerMode',
                                'manual',
                              ),
                            },
                            {
                              'child': menuSingle(
                                menuNode(
                                  11,
                                  'flutter.widgets.SizedBox',
                                  {
                                    'width': menuN(150.0),
                                    'height': menuN(42.0),
                                  },
                                  {
                                    'child': menuSingle(
                                      menuNode(
                                        12,
                                        'flutter.material.TextField',
                                        {},
                                      ),
                                    ),
                                  },
                                ),
                              ),
                            },
                          ),
                  ),
                },
              ),
              menuItem(20, 'Inner retained item', close: false),
            ],
    ),
    menuItem(30, 'Outer retained item', close: false),
  ],
);

Finder _anchor(int id) => find.byKey(ValueKey('canvas-widget-${menuId(id)}'));
Finder get _tooltip => find.byWidgetPredicate(
  (widget) =>
      widget is Tooltip &&
      widget.key is GlobalKey &&
      widget.message == _message,
);
SubmenuButton _inner(WidgetTester tester) => sdkSubmenu(tester, index: 1);

Future<void> _openNested(WidgetTester tester) async {
  if (!sdkSubmenu(tester).controller!.isOpen) {
    await tester.tap(find.text('Outer submenu'));
    await tester.pumpAndSettle();
  }
  if (!_inner(tester).controller!.isOpen) {
    // Lifecycle tests control the isolated preview directly; native nested
    // focus/hover/click activation parity is covered by the owning menu suite.
    _inner(tester).controller!.open();
    await tester.pumpAndSettle();
  }
  expect(sdkSubmenu(tester).controller!.isOpen, true);
  expect(_inner(tester).controller!.isOpen, true);
  expect(tester.takeException(), isNull);
}

void main() {
  testWidgets(
    'nested native menus retain controllers tooltip child state and focus through independent style and reference updates',
    (tester) async {
      final selections = <String>[];
      CanvasDropResolver? resolver;
      await pumpMenu(
        tester,
        _tree(),
        selections: selections,
        onDrop: (value) => resolver = value,
      );
      await _openNested(tester);
      expect(selections, [menuId(3)]);
      final states = tester.stateList(find.byType(SubmenuButton)).toList();
      final outerController = sdkSubmenu(tester).controller!;
      final innerController = _inner(tester).controller!;
      final field = tester.state<EditableTextState>(find.byType(EditableText));
      field.widget.controller.text = 'Keep nested preview text';
      final tooltip = tester.state<TooltipState>(_tooltip);
      final focus = tester
          .widget<TextButton>(
            find
                .descendant(of: _anchor(9), matching: find.byType(TextButton))
                .first,
          )
          .focusNode!;
      focus.requestFocus();
      await tester.pump();
      expect(focus.hasFocus, true);
      tooltip.ensureTooltipVisible();
      await tester.pumpAndSettle();
      for (final properties in <Map<String, Object?>>[
        {
          'styleForegroundColor': {'kind': 'color', 'argb': '0xFF123456'},
        },
        {
          'menuStyleBackgroundColor': {'kind': 'color', 'argb': '0xFFDEF1FC'},
        },
        {
          'controller': menuRef,
          'focusNode': menuRef,
          'statesController': menuRef,
          'style': menuRef,
          'menuStyle': menuRef,
          'submenuIcon': menuRef,
          'onOpen': menuRef,
          'onHover': menuRef,
        },
        {'useRootOverlay': menuB(true)},
        {},
      ]) {
        final tree = _tree(properties: properties);
        final stored = jsonEncode(tree);
        final selectionCount = selections.length;
        await pumpMenu(
          tester,
          tree,
          selected: menuId(12),
          selections: selections,
          onDrop: (value) => resolver = value,
        );
        final current = tester.stateList(find.byType(SubmenuButton)).toList();
        expect(current[0], same(states[0]));
        expect(current[1], same(states[1]));
        expect(sdkSubmenu(tester).controller, same(outerController));
        expect(_inner(tester).controller, same(innerController));
        expect(outerController.isOpen, true);
        expect(innerController.isOpen, true);
        expect(tester.state<TooltipState>(_tooltip), same(tooltip));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(field),
        );
        expect(field.widget.controller.text, 'Keep nested preview text');
        expect(field.widget.focusNode.canRequestFocus, false);
        expect(focus.hasFocus, true);
        expect(find.text(_message), findsOneWidget);
        for (final id in [2, 3, 6, 7, 9, 10, 11, 12, 20, 21, 30, 31]) {
          expect(_anchor(id), findsOneWidget);
        }
        expect(jsonEncode(tree), stored);
        expect(selections, hasLength(selectionCount));
        expect(resolver, isNotNull);
        expect(tester.takeException(), isNull);
      }
      outerController.close();
      await tester.pumpAndSettle();
      expect(find.text(_message), findsNothing);
      expect(_anchor(12), findsNothing);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      await tester.pump(const Duration(seconds: 2));
      expect(resolver, isNull);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'actual nested menu F2 editor keeps View focus and uncommitted draft across independent style diagnostics',
    (tester) async {
      final selections = <String>[];
      Future<void> pump(Map<String, Object?> props, int selected) async {
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: menuModel(_tree(properties: props, inline: true)),
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
      await _openNested(tester);
      final outerController = sdkSubmenu(tester).controller!;
      final innerController = _inner(tester).controller!;
      await tester.tap(_anchor(12));
      await tester.pump();
      expect(selections.last, menuId(12));
      expect(
        outerController.isOpen,
        true,
        reason: 'Selecting a menu child keeps its outer menu open.',
      );
      expect(
        innerController.isOpen,
        true,
        reason: 'Selecting a menu child keeps its containing submenu open.',
      );
      await pump({}, 12);
      expect(
        innerController.isOpen,
        true,
        reason: 'Selection-only model refresh keeps the submenu open.',
      );
      await tester.sendKeyEvent(LogicalKeyboardKey.f2);
      await tester.pumpAndSettle();
      final editor = find.byKey(
        ValueKey('canvas-inline-text-editor-${menuId(12)}'),
      );
      expect(editor, findsOneWidget);
      final state = tester.state<EditableTextState>(find.byType(EditableText));
      final view = View.of(tester.element(editor));
      expect(state.widget.focusNode.hasFocus, true);
      state.widget.controller.text = 'Unsaved submenu label';
      for (final properties in <Map<String, Object?>>[
        {'controller': menuRef, 'menuStyle': menuRef, 'style': menuRef},
        {
          'styleForegroundColor': {'kind': 'color', 'argb': '0xFF123456'},
          'menuStyleBackgroundColor': {'kind': 'color', 'argb': '0xFFDEF1FC'},
        },
        {},
      ]) {
        await pump(properties, 12);
        expect(sdkSubmenu(tester).controller, same(outerController));
        expect(_inner(tester).controller, same(innerController));
        expect(outerController.isOpen, true);
        expect(innerController.isOpen, true);
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(state),
        );
        expect(state.widget.focusNode.hasFocus, true);
        expect(state.widget.controller.text, 'Unsaved submenu label');
        expect(View.of(tester.element(editor)), same(view));
      }
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pumpAndSettle();
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'open nested Tooltip safely handles animation and empty menu transitions then reopens retained IDs without ghost drops',
    (tester) async {
      CanvasDropResolver? resolver;
      await pumpMenu(tester, _tree(), onDrop: (value) => resolver = value);
      await _openNested(tester);
      for (final properties in <Map<String, Object?>>[
        {'animated': menuB(true)},
        {'animated': menuB(false)},
        {},
      ]) {
        tester.state<TooltipState>(_tooltip).ensureTooltipVisible();
        await tester.pumpAndSettle();
        expect(find.text(_message), findsOneWidget);
        final tree = _tree(properties: properties);
        final stored = jsonEncode(tree);
        await pumpMenu(tester, tree, onDrop: (value) => resolver = value);
        await _openNested(tester);
        for (final id in [2, 3, 6, 7, 9, 10, 11, 12, 20, 21, 30, 31]) {
          expect(_anchor(id), findsOneWidget);
        }
        expect(jsonEncode(tree), stored);
        expect(tester.takeException(), isNull);
      }
      tester.state<TooltipState>(_tooltip).ensureTooltipVisible();
      await tester.pumpAndSettle();
      await pumpMenu(
        tester,
        _tree(empty: true),
        onDrop: (value) => resolver = value,
      );
      if (!sdkSubmenu(tester).controller!.isOpen) {
        sdkSubmenu(tester).controller!.open();
        await tester.pumpAndSettle();
      }
      expect(_inner(tester).menuChildren, isEmpty);
      expect(find.text(_message), findsNothing);
      expect(_anchor(12), findsNothing);
      final button = tester.widget<TextButton>(
        find
            .descendant(of: _anchor(6), matching: find.byType(TextButton))
            .first,
      );
      expect(button.onPressed, isNull);
      await pumpMenu(tester, _tree(), onDrop: (value) => resolver = value);
      await _openNested(tester);
      expect(_anchor(12), findsOneWidget);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      await tester.pump(const Duration(seconds: 2));
      expect(find.text(_message), findsNothing);
      expect(resolver, isNull);
      expect(tester.takeException(), isNull);
    },
  );
}
