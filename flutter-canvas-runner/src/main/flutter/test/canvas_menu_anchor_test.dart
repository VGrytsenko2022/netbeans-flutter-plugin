import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'canvas_model_test.dart' as fixture;

const menuAnchorType = 'flutter.material.MenuAnchor';
const menuRef = {'kind': 'dartObjectReferencePresence'};
const menuNull = {'kind': 'null'};
String menuId(int n) =>
    '793560e4-67ae-4e99-907b-${n.toString().padLeft(12, '0')}';
Map<String, Object?> menuS(String v) => {'kind': 'string', 'value': v};
Map<String, Object?> menuB(bool v) => {'kind': 'boolean', 'value': v};
Map<String, Object?> menuN(num v) => {
  'kind': v is int ? 'integer' : 'double',
  'value': v,
};
Map<String, Object?> menuE(String type, String v) => {
  'kind': 'enum',
  'type': type,
  'value': v,
};
Map<String, Object?> menuSingle(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> menuList(List<Object?> children) => {
  'kind': 'list',
  'children': children,
};
Map<String, Object?> menuNode(
  int id,
  String type,
  Map<String, Object?> props, [
  Map<String, Object?> slots = const {},
]) => {'id': menuId(id), 'type': type, 'properties': props, 'slots': slots};
Map<String, Object?> menuText(int id, String text) =>
    menuNode(id, 'flutter.widgets.Text', {'data': menuS(text)});
Map<String, Object?> menuItem(
  int id,
  String text, {
  bool enabled = true,
  bool close = true,
}) => menuNode(
  id,
  'flutter.material.MenuItemButton',
  {'enabled': menuB(enabled), 'closeOnActivate': menuB(close)},
  {'child': menuSingle(menuText(id + 1, text))},
);
Map<String, Object?> menuAnchor({
  int id = 2,
  Map<String, Object?> props = const {},
  Object? child,
  List<Object?>? items,
  bool emptyChild = false,
}) => menuNode(id, menuAnchorType, props, {
  'menuChildren': menuList(
    items ?? [menuItem(10, 'First'), menuItem(20, 'Second')],
  ),
  if (!emptyChild)
    'child': menuSingle(
      child ??
          menuNode(
            3,
            'flutter.widgets.SizedBox',
            {'width': menuN(100), 'height': menuN(40)},
            {'child': menuSingle(menuText(4, 'Anchor'))},
          ),
    ),
});
CanvasModel menuModel(
  Object root, {
  String direction = 'ltr',
  String platform = 'windows',
}) {
  final value =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (value['profile'] as Map)['targetPlatform'] = platform;
  value['root'] = menuNode(900, 'flutter.material.Scaffold', {}, {
    'body': menuSingle(
      menuNode(901, 'flutter.widgets.Center', {}, {
        'child': menuSingle(
          menuNode(
            902,
            'flutter.widgets.Directionality',
            {'textDirection': menuE('TextDirection', direction)},
            {'child': menuSingle(root)},
          ),
        ),
      }),
    ),
  });
  return CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(value))));
}

Future<void> pumpMenu(
  WidgetTester tester,
  Object root, {
  String? selected,
  List<String>? selections,
  ValueChanged<CanvasDropResolver?>? onDrop,
  ValueChanged<CanvasMovePreviewResolver?>? onMove,
  String direction = 'ltr',
  ThemeData? theme,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme,
      home: CanvasDocumentView(
        model: menuModel(root, direction: direction),
        selectedWidgetId: selected ?? menuId(2),
        onSelected: selections?.add ?? (_) {},
        onDropResolverChanged: onDrop,
        onMovePreviewResolverChanged: onMove,
      ),
    ),
  );
  await tester.pumpAndSettle();
  expect(tester.takeException(), isNull);
}

MenuAnchor sdkMenu(WidgetTester tester, {int index = 0}) =>
    tester.widgetList<MenuAnchor>(find.byType(MenuAnchor)).elementAt(index);
Future<void> openMenu(WidgetTester tester) async {
  await tester.tap(find.byKey(ValueKey('canvas-menu-preview-${menuId(2)}')));
  await tester.pumpAndSettle();
  expect(tester.takeException(), isNull);
  expect(sdkMenu(tester).controller!.isOpen, true);
}

String menuDiagnostics(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((tooltip) => tooltip.message ?? '')
    .join('\n');
String menuContractSection(String value) {
  final start = value.indexOf('W|$menuAnchorType\n');
  return value.substring(start, value.indexOf('W|', start + 2));
}

void main() {
  test(
    '219 independent MenuAnchor properties exact two slots and no fabricated required child',
    () {
      final section = menuContractSection(
        canvasRuntimeWidgetSchemaContractForTesting(),
      );
      expect(section, menuContractSection(canvasReviewedWidgetSchemaContract));
      expect(
        section.split('\n').where((l) => l.startsWith('P|')),
        hasLength(219),
      );
      expect(section, contains('S|menuChildren|list|1|0|10000|any'));
      expect(section, contains('S|child|single|0|0|1|any'));
      expect(canvasReviewedRequiredWrapperSlot(menuAnchorType), isNull);
      expect(canvasDropSlotsForWidgetType(menuAnchorType), hasLength(2));
      expect(
        () => menuModel(menuNode(2, menuAnchorType, {})),
        throwsFormatException,
      );
      expect(
        () => menuModel(
          menuAnchor(props: {'style': menuNull, 'styleElevation': menuN(2.0)}),
        ),
        throwsFormatException,
      );
      expect(
        () => menuModel(menuAnchor(props: {'animated': menuNull})),
        throwsFormatException,
      );
      expect(
        () => menuModel(menuAnchor(props: {'styleTextFontSize': menuN(18.0)})),
        throwsFormatException,
      );
    },
  );
  testWidgets(
    'native defaults optional child and explicit accessible preview action do not become an app opener',
    (tester) async {
      final selections = <String>[];
      await pumpMenu(tester, menuAnchor(), selections: selections);
      final menu = sdkMenu(tester);
      expect(menu.controller, isNotNull);
      expect(menu.builder, isNull);
      expect(menu.childFocusNode, isNull);
      expect(menu.style, isNull);
      expect(menu.alignmentOffset, Offset.zero);
      expect(menu.reservedPadding, isNull);
      expect(menu.layerLink, isNull);
      expect(menu.clipBehavior, Clip.hardEdge);
      expect(menu.consumeOutsideTap, false);
      expect(menu.crossAxisUnconstrained, true);
      expect(menu.useRootOverlay, false);
      expect(menu.animated, false);
      expect(find.text('First'), findsNothing);
      await tester.tap(find.text('Anchor'));
      await tester.pumpAndSettle();
      expect(menu.controller!.isOpen, false);
      final before = selections.length;
      final geometry = tester.getRect(
        find.byKey(ValueKey('canvas-widget-${menuId(3)}')),
      );
      await openMenu(tester);
      expect(selections, hasLength(before));
      expect(find.text('First'), findsOneWidget);
      expect(
        tester.getRect(find.byKey(ValueKey('canvas-widget-${menuId(3)}'))),
        geometry,
      );
      expect(find.text('Close preview'), findsOneWidget);
      await tester.tap(find.text('Close preview'));
      await tester.pumpAndSettle();
      expect(menu.controller!.isOpen, false);
      expect(find.text('First'), findsNothing);
      await pumpMenu(tester, menuAnchor(emptyChild: true, items: []));
      expect(sdkMenu(tester).child, isNull);
      expect(sdkMenu(tester).menuChildren, isEmpty);
      await openMenu(tester);
      expect(find.byType(MenuItemButton), findsNothing);
    },
  );
  testWidgets(
    'project references are anonymous inert approximations and root overlay stays inside viewport',
    (tester) async {
      final props = {
        for (final name in [
          'controller',
          'childFocusNode',
          'builder',
          'style',
          'alignmentOffset',
          'reservedPadding',
          'layerLink',
          'onOpen',
          'onClose',
          'onAnimationStatusChanged',
        ])
          name: menuRef,
        'useRootOverlay': menuB(true),
      };
      await pumpMenu(tester, menuAnchor(props: props));
      final menu = sdkMenu(tester);
      expect(menu.builder, isNull);
      expect(menu.style, isNull);
      expect(menu.childFocusNode, isNull);
      expect(menu.layerLink, isNotNull);
      expect(menu.useRootOverlay, false);
      expect(
        menuDiagnostics(tester),
        contains('root-overlay preview is isolated'),
      );
      expect(menuDiagnostics(tester), contains('not a generated opener'));
      await openMenu(tester);
      final panel = tester.getRect(find.text('First'));
      final viewport = tester.getRect(
        find.byKey(const ValueKey('canvas-logical-viewport-overlay')),
      );
      expect(
        viewport.contains(panel.topLeft) &&
            viewport.contains(panel.bottomRight),
        true,
      );
      expect(View.of(tester.element(find.text('First'))), isNotNull);
    },
  );
  testWidgets(
    'native menu items close on activation while false retains menu and no chord registration',
    (tester) async {
      await pumpMenu(
        tester,
        menuAnchor(
          items: [menuItem(10, 'Keep', close: false), menuItem(20, 'Close')],
        ),
      );
      await openMenu(tester);
      await tester.tap(find.text('Keep'));
      await tester.pumpAndSettle();
      expect(sdkMenu(tester).controller!.isOpen, true);
      await tester.tap(find.text('Close'));
      await tester.pumpAndSettle();
      expect(sdkMenu(tester).controller!.isOpen, false);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'closing animation removes menu drop targets before controller isOpen becomes false',
    (tester) async {
      CanvasDropResolver? resolver;
      await pumpMenu(
        tester,
        menuAnchor(props: {'animated': menuB(true)}),
        onDrop: (value) => resolver = value,
      );
      await openMenu(tester);
      final surface = tester.getRect(
        find.byKey(const ValueKey('canvas-interaction-surface')),
      );
      final point = tester.getCenter(find.text('Second'));
      final x = ((point.dx - surface.left) / surface.width * 1000000).round();
      final y = ((point.dy - surface.top) / surface.height * 1000000).round();
      expect(resolver!(x, y), isNotNull);
      sdkMenu(tester).controller!.close();
      await tester.pump();
      expect(sdkMenu(tester).controller!.isOpen, true);
      expect(resolver!(x, y)?.parentWidgetId, isNot(menuId(2)));
      await tester.pumpAndSettle();
      expect(sdkMenu(tester).controller!.isOpen, false);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'all203 local MenuStyle leaves use native menu themes and retain all nine resolver buckets',
    (tester) async {
      const suffixes = {
        'BackgroundColor',
        'ShadowColor',
        'SurfaceTintColor',
        'Elevation',
        'Padding',
        'MinimumWidth',
        'MinimumHeight',
        'FixedWidth',
        'FixedHeight',
        'MaximumWidth',
        'MaximumHeight',
        'SideColor',
        'SideWidth',
        'SideStyle',
        'SideStrokeAlign',
        'ShapeKind',
        'ShapeRadiusTopLeft',
        'ShapeRadiusTopRight',
        'ShapeRadiusBottomLeft',
        'ShapeRadiusBottomRight',
        'ShapeCircleEccentricity',
        'MouseCursor',
      };
      const prefixes = [
        'style',
        'styleDisabled',
        'styleError',
        'styleDragged',
        'stylePressed',
        'styleSelected',
        'styleScrolledUnder',
        'styleHovered',
        'styleFocused',
      ];
      final base = fixture.elevatedButtonPropertiesForViewTest();
      final seen = <String>{};
      for (final circle in [false, true]) {
        final props = <String, Object?>{};
        for (final prefix in prefixes) {
          final sourcePrefix =
              [
                'styleError',
                'styleDragged',
                'styleSelected',
                'styleScrolledUnder',
              ].contains(prefix)
              ? 'stylePressed'
              : prefix;
          for (final suffix in suffixes) {
            final value = base['$sourcePrefix$suffix'];
            if (value != null &&
                suffix != 'ShapeCircleEccentricity' &&
                !(circle && suffix.startsWith('ShapeRadius'))) {
              props['$prefix$suffix'] = value;
            }
          }
          props['${prefix}ShapeKind'] = menuS(
            circle ? 'circle' : 'roundedRectangle',
          );
          if (circle) props['${prefix}ShapeCircleEccentricity'] = menuN(0.3);
        }
        for (final name in [
          'styleVisualDensityHorizontal',
          'styleVisualDensityVertical',
          'styleAlignmentKind',
          'styleAlignmentX',
          'styleAlignmentY',
        ]) {
          props[name] = base[name]!;
        }
        seen.addAll(props.keys);
        await pumpMenu(tester, menuAnchor(props: props));
        final style = sdkMenu(tester).style!;
        for (final states in <Set<WidgetState>>[
          {},
          for (final state in WidgetState.values) {state},
        ]) {
          expect(
            style.shape!.resolve(states),
            circle ? isA<CircleBorder>() : isA<RoundedRectangleBorder>(),
          );
          expect(style.side!.resolve(states), isNotNull);
          expect(style.minimumSize!.resolve(states), isNotNull);
          expect(style.maximumSize!.resolve(states), isNotNull);
          expect(style.fixedSize!.resolve(states), isNotNull);
        }
        await openMenu(tester);
        final material = tester
            .widgetList<Material>(
              find.ancestor(
                of: find.text('First'),
                matching: find.byType(Material),
              ),
            )
            .firstWhere(
              (m) => m.shape != null && m.type == MaterialType.canvas,
            );
        expect(material.color, style.backgroundColor!.resolve({}));
        sdkMenu(tester).controller!.close();
        await tester.pumpAndSettle();
      }
      expect(seen, hasLength(203));
    },
  );
  testWidgets(
    'sparse menu compound fallback and density native omitted peer zero do not borrow ButtonStyle defaults',
    (tester) async {
      final theme = ThemeData(
        menuTheme: const MenuThemeData(
          style: MenuStyle(
            minimumSize: WidgetStatePropertyAll(Size(140, 75)),
            maximumSize: WidgetStatePropertyAll(Size(300, 240)),
            shape: WidgetStatePropertyAll(
              RoundedRectangleBorder(
                side: BorderSide(color: Colors.purple, width: 3),
                borderRadius: BorderRadius.all(Radius.circular(13)),
              ),
            ),
            visualDensity: VisualDensity(horizontal: 2, vertical: 3),
            backgroundColor: WidgetStatePropertyAll(Colors.amber),
          ),
        ),
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            backgroundColor: Colors.red,
            minimumSize: const Size(10, 10),
          ),
        ),
      );
      await pumpMenu(
        tester,
        menuAnchor(
          props: {
            'styleMinimumWidth': menuN(160.0),
            'styleSideWidth': menuN(5.0),
            'styleShapeKind': menuS('roundedRectangle'),
            'styleShapeRadiusTopLeft': menuN(7.0),
            'styleVisualDensityHorizontal': menuN(1.0),
          },
        ),
        theme: theme,
      );
      final style = sdkMenu(tester).style!;
      expect(style.minimumSize!.resolve({}), const Size(160, 75));
      expect(style.maximumSize!.resolve({}), const Size(300, 240));
      expect(style.side!.resolve({})!.color, Colors.purple);
      expect(style.side!.resolve({})!.width, 5);
      final shape = style.shape!.resolve({})! as RoundedRectangleBorder;
      expect(
        shape.borderRadius,
        const BorderRadius.only(
          topLeft: Radius.circular(7),
          topRight: Radius.circular(13),
          bottomLeft: Radius.circular(13),
          bottomRight: Radius.circular(13),
        ),
      );
      expect(style.visualDensity, const VisualDensity(horizontal: 1));
      expect(style.alignment, isNull);
      await openMenu(tester);
      expect(
        tester
            .widgetList<Material>(
              find.ancestor(
                of: find.text('First'),
                matching: find.byType(Material),
              ),
            )
            .any((m) => m.color == Colors.amber),
        true,
      );
    },
  );
  testWidgets(
    'unsafe inherited menu geometry is explicitly approximated while child and model remain intact',
    (tester) async {
      final root = menuAnchor();
      await pumpMenu(
        tester,
        root,
        theme: ThemeData(
          menuTheme: const MenuThemeData(
            style: MenuStyle(
              minimumSize: WidgetStatePropertyAll(Size(double.infinity, 30)),
              elevation: WidgetStatePropertyAll(double.nan),
            ),
          ),
        ),
      );
      expect(
        menuDiagnostics(tester),
        contains('inherited menu geometry preview unavailable'),
      );
      expect(find.text('Anchor'), findsOneWidget);
      await openMenu(tester);
      expect(find.text('First'), findsOneWidget);
      expect(tester.getSize(find.text('First')).isFinite, true);
      expect(menuModel(root).root, isNotNull);
    },
  );
  testWidgets(
    'RTL directional offsets layer links and negative reserved padding retain native values',
    (tester) async {
      for (final direction in ['ltr', 'rtl']) {
        await pumpMenu(
          tester,
          menuAnchor(
            props: {
              'styleAlignmentKind': menuS('directional'),
              'styleAlignmentX': menuN(-1.0),
              'styleAlignmentY': menuN(1.0),
              'alignmentOffset': {'kind': 'offset', 'dx': 9.0, 'dy': -3.0},
              'reservedPadding': {
                'kind': 'edgeInsetsDirectional',
                'start': -2.0,
                'top': 4.0,
                'end': 10.0,
                'bottom': 6.0,
              },
              'layerLink': menuRef,
            },
          ),
          direction: direction,
        );
        expect(sdkMenu(tester).alignmentOffset, const Offset(9, -3));
        expect(
          sdkMenu(tester).reservedPadding,
          const EdgeInsetsDirectional.fromSTEB(-2, 4, 10, 6),
        );
        expect(
          sdkMenu(tester).style!.alignment,
          AlignmentDirectional.bottomStart,
        );
        await openMenu(tester);
        expect(find.byType(CompositedTransformFollower), findsWidgets);
        expect(tester.getRect(find.text('First')).isFinite, true);
        sdkMenu(tester).controller!.close();
        await tester.pumpAndSettle();
      }
    },
  );
  testWidgets(
    'logical viewport overlay preserves menu scale across zoom and root flag does not leak to host',
    (tester) async {
      final model = menuModel(
        menuAnchor(props: {'useRootOverlay': menuB(true)}),
      );
      double? logicalWidth;
      for (final zoom in [500000, 1000000]) {
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: menuId(2),
              onSelected: (_) {},
              viewportPresentation: CanvasViewportPresentation.fit(
                model,
              ).copyWith(mode: 'manual', zoomMicros: zoom),
            ),
          ),
        );
        await tester.pumpAndSettle();
        sdkMenu(tester).controller!.open();
        await tester.pumpAndSettle();
        final text = tester.renderObject<RenderBox>(find.text('First'));
        final actual = tester.getRect(find.text('First'));
        final transform = text.getTransformTo(null);
        final scale = transform.storage[0];
        expect(scale, closeTo(zoom / 1000000, 0.001));
        logicalWidth ??= text.size.width;
        expect(text.size.width, closeTo(logicalWidth, 0.001));
        expect(actual.isFinite, true);
        expect(sdkMenu(tester).useRootOverlay, false);
        expect(tester.takeException(), isNull);
        sdkMenu(tester).controller!.close();
        await tester.pumpAndSettle();
      }
    },
  );
  testWidgets(
    'native menu keyboard traversal skips disabled entries and Enter activates without project callbacks',
    (tester) async {
      await pumpMenu(
        tester,
        menuAnchor(
          items: [
            menuItem(10, 'First', close: false),
            menuItem(20, 'Disabled', enabled: false),
            menuItem(30, 'Last'),
          ],
        ),
      );
      await openMenu(tester);
      final items = tester
          .widgetList<TextButton>(
            find.descendant(
              of: find.byType(MenuItemButton),
              matching: find.byType(TextButton),
            ),
          )
          .toList();
      expect(items, hasLength(3));
      items.first.focusNode!.requestFocus();
      await tester.pump();
      expect(items.first.focusNode!.hasFocus, true);
      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
      await tester.pumpAndSettle();
      expect(items.last.focusNode!.hasFocus, true);
      expect(items[1].focusNode!.hasFocus, false);
      await tester.sendKeyEvent(LogicalKeyboardKey.enter);
      await tester.pumpAndSettle();
      expect(sdkMenu(tester).controller!.isOpen, false);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'open overlay append and exact move insertion zones follow actual menu panel not anchor geometry',
    (tester) async {
      CanvasDropResolver? drop;
      CanvasMovePreviewResolver? move;
      await pumpMenu(
        tester,
        menuAnchor(),
        onDrop: (value) => drop = value,
        onMove: (value) => move = value,
      );
      expect(move!(menuId(10), menuId(2), 'menuChildren', 1), isNull);
      await openMenu(tester);
      final surface = tester.getRect(
        find.byKey(const ValueKey('canvas-interaction-surface')),
      );
      final second = tester.getRect(
        find.byKey(ValueKey('canvas-widget-${menuId(20)}')),
      );
      final point = Offset(second.center.dx, second.bottom - 2);
      final x = ((point.dx - surface.left) / surface.width * 1000000).round();
      final y = ((point.dy - surface.top) / surface.height * 1000000).round();
      final target = drop!(x, y)!;
      expect(target.parentWidgetId, menuId(2));
      expect(target.slotName, 'menuChildren');
      expect(target.insertionIndex, 2);
      final insertion = move!(menuId(10), menuId(2), 'menuChildren', 1);
      expect(insertion, isNotNull);
      expect(insertion!.slotName, 'menuChildren');
      expect(insertion.insertionIndex, 1);
      sdkMenu(tester).controller!.close();
      await tester.pumpAndSettle();
      expect(move!(menuId(10), menuId(2), 'menuChildren', 1), isNull);
      expect(drop!(x, y)?.parentWidgetId, isNot(menuId(2)));
    },
  );
  testWidgets(
    'outside tap consumption and deprecated anchor-tap flag keep native behavior',
    (tester) async {
      for (final consume in [false, true]) {
        for (final deprecated in [false, true]) {
          await pumpMenu(
            tester,
            menuAnchor(
              props: {
                'consumeOutsideTap': menuB(consume),
                'anchorTapClosesMenu': menuB(deprecated),
              },
            ),
          );
          await openMenu(tester);
          await tester.tapAt(const Offset(5, 5));
          await tester.pumpAndSettle();
          expect(sdkMenu(tester).controller!.isOpen, false);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );
  testWidgets(
    'zero anchor exposes optional child when closed and required empty menu list only during explicit preview',
    (tester) async {
      CanvasDropResolver? drop;
      await pumpMenu(
        tester,
        menuAnchor(emptyChild: true, items: []),
        onDrop: (value) => drop = value,
      );
      final point = tester.getCenter(
        find.byKey(ValueKey('canvas-widget-${menuId(2)}')),
      );
      final surface = tester.getRect(
        find.byKey(const ValueKey('canvas-interaction-surface')),
      );
      final x = ((point.dx - surface.left) / surface.width * 1000000).round();
      final y = ((point.dy - surface.top) / surface.height * 1000000).round();
      expect(drop!(x, y)!.slotName, 'child');
      await openMenu(tester);
      expect(sdkMenu(tester).menuChildren, isEmpty);
      final target = drop!(x, y)!;
      expect(target.parentWidgetId, menuId(2));
      expect(target.slotName, 'menuChildren');
      expect(target.insertionIndex, 0);
      expect(find.byType(MenuItemButton), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );
}
