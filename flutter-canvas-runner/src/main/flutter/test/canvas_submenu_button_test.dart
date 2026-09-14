import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_menu_anchor_test.dart';
import 'canvas_model_test.dart' as fixture;

const submenuType = 'flutter.material.SubmenuButton';
Map<String, Object?> submenu({int id = 2, Map<String, Object?> props = const {},
    Object? child, List<Object?>? items, bool emptyChild = false,
    Object? leading, Object? trailing}) => menuNode(id, submenuType, props, {
  'child': menuSingle(emptyChild ? null : child ?? menuText(id + 1, 'Submenu')),
  'menuChildren': menuList(items ?? [menuItem(10, 'First'), menuItem(20, 'Second')]),
  if (leading != null) 'leadingIcon': menuSingle(leading),
  if (trailing != null) 'trailingIcon': menuSingle(trailing),
});
SubmenuButton sdkSubmenu(WidgetTester tester, {int index = 0}) =>
    tester.widgetList<SubmenuButton>(find.byType(SubmenuButton)).elementAt(index);
String submenuSection(String contract) {
  final start = contract.indexOf('W|$submenuType\n');
  return contract.substring(start, contract.indexOf('W|', start + 2));
}
Map<String, Object?> submenuIcon(int? point) => {
  'kind': 'iconData', 'codePoint': point, 'fontFamily': point == null ? null : 'MaterialIcons',
  'fontPackage': null, 'matchTextDirection': false, 'fontFamilyFallback': <String>[],
};
Map<String, Object?> submenuStyles({bool circles = false, bool paints = false}) {
  final source = fixture.elevatedButtonPropertiesForViewTest();
  final button = <String, Object?>{
    for (final entry in source.entries)
      if (entry.key.startsWith('style')) entry.key: entry.value,
  };
  for (final prefix in ['styleError', 'styleDragged', 'styleSelected', 'styleScrolledUnder']) {
    for (final entry in source.entries.where((e) => e.key.startsWith('stylePressed'))) {
      button['$prefix${entry.key.substring(12)}'] = entry.value;
    }
  }
  button.addAll({'styleIconAlignment': menuE('IconAlignment', 'end'),
    'styleBackgroundBuilder': menuRef, 'styleForegroundBuilder': menuRef});
  const prefixes = ['style', 'styleDisabled', 'styleError', 'styleDragged', 'stylePressed',
    'styleSelected', 'styleScrolledUnder', 'styleHovered', 'styleFocused'];
  for (final prefix in prefixes) {
    button['${prefix}ShapeKind'] = menuS('roundedRectangle');
    if (circles) {
      button.removeWhere((name, _) => name.startsWith('${prefix}ShapeRadius'));
      button['${prefix}ShapeKind'] = menuS('circle');
      button['${prefix}ShapeCircleEccentricity'] = menuN(0.3);
    }
    if (paints) {
      button.remove('${prefix}TextBackgroundColor');
      button['${prefix}TextBackground'] = {'kind':'paint', 'color':{'kind':'literal','argb':'0xFF654321'},
        'blendMode':'srcOver','style':'fill','strokeWidth':0.0,'strokeCap':'round','strokeJoin':'bevel',
        'strokeMiterLimit':4.0,'antiAlias':true,'filterQuality':'medium','invertColors':false};
    }
  }
  final menuNames = menuContractSection(canvasReviewedWidgetSchemaContract).split('\n')
      .where((l) => l.startsWith('P|style') && !l.startsWith('P|style|'))
      .map((l) => l.split('|')[1]).toSet();
  return {
    ...button,
    for (final entry in button.entries)
      if (menuNames.contains(entry.key)) 'menuStyle${entry.key.substring(5)}': entry.value,
  };
}

void main() {
  test('721 exact independent fields four slots and fail-closed 1024 budget', () {
    final section = submenuSection(canvasRuntimeWidgetSchemaContractForTesting());
    expect(section, submenuSection(canvasReviewedWidgetSchemaContract));
    expect(section.split('\n').where((l) => l.startsWith('P|')), hasLength(721));
    expect(section, contains('S|child|single|1|0|1|any'));
    expect(section, contains('S|menuChildren|list|1|0|10000|any'));
    expect(canvasDropSlotsForWidgetType(submenuType), hasLength(4));
    expect(canvasReviewedRequiredWrapperSlot(submenuType), isNull);
    for (final count in [1024, 1025]) {
      expect(() => menuModel(submenu(props: {for (var i=0; i<count; i++) 'unknown$i': menuN(1)})), throwsFormatException);
    }
    for (final props in [
      {'style': menuNull, 'styleElevation': menuN(2)},
      {'menuStyle': menuNull, 'menuStyleElevation': menuN(2)},
      {'submenuIcon': menuNull, 'submenuIconDefault': menuNull},
      {'hoverOpenDelayUs': menuNull}, {'onOpen': {'kind':'callbackPresence'}},
      {'enabled': menuB(false)}, {'onPressed': menuRef},
    ]) { expect(() => menuModel(submenu(props: props)), throwsFormatException); }
    for (final slots in [{'child': menuSingle(null)}, {'menuChildren': menuList([])}]) {
      expect(() => menuModel(menuNode(2, submenuType, {}, slots)), throwsFormatException);
    }
    for (final duration in [-9007199254740991, -1, 0, 9007199254740991]) {
      expect(menuModel(submenu(props: {'hoverOpenDelayUs': menuN(duration)})), isNotNull);
    }
  });
  testWidgets('native self-opener deepest selection and empty disabled preview without fake items', (tester) async {
    final selections = <String>[];
    await pumpMenu(tester, submenu(), selections: selections);
    final sdk = sdkSubmenu(tester);
    expect(sdk.style, isNull); expect(sdk.menuStyle, isNull);
    expect(sdk.clipBehavior, Clip.hardEdge); expect(sdk.hoverOpenDelay, Duration.zero);
    expect(sdk.focusNode, isNull); expect(sdk.statesController, isNull);
    expect(sdk.useRootOverlay, false); expect(sdk.animated, false);
    await tester.tap(find.text('Submenu')); await tester.pumpAndSettle();
    expect(sdk.controller!.isOpen, true); expect(find.text('First'), findsOneWidget);
    expect(selections, [menuId(3)]);
    await tester.tap(find.text('First')); await tester.pumpAndSettle();
    expect(sdk.controller!.isOpen, false);
    await pumpMenu(tester, submenu(items: [], emptyChild: true));
    expect(sdkSubmenu(tester).child, isNull); expect(sdkSubmenu(tester).menuChildren, isEmpty);
    final button = tester.widget<TextButton>(find.descendant(of: find.byType(SubmenuButton), matching: find.byType(TextButton)).first);
    expect(button.onPressed, isNull);
    await openMenu(tester);
    expect(sdkSubmenu(tester).controller!.isOpen, true);
    expect(find.byType(MenuItemButton), findsNothing);
    expect(button.onPressed, isNull); expect(tester.takeException(), isNull);
  });
  testWidgets('anonymous refs keep native state defaults and two distinct style families', (tester) async {
    await pumpMenu(tester, submenu(props: {
      for(final name in ['controller','focusNode','style','menuStyle','alignmentOffset','statesController',
        'submenuIcon','hoverOpenDelayUs','onHover','onFocusChange','onOpen','onClose','onAnimationStatusChanged']) name: menuRef,
      'useRootOverlay': menuB(true),
    }));
    final sdk = sdkSubmenu(tester);
    expect(sdk.style, isNull); expect(sdk.menuStyle, isNull); expect(sdk.statesController, isNull);
    expect(sdk.submenuIcon, isNull); expect(sdk.hoverOpenDelay, Duration.zero);
    expect(sdk.onHover, isNotNull); expect(sdk.onFocusChange, isNotNull);
    expect(menuDiagnostics(tester), contains('padding prepass'));
    expect(menuDiagnostics(tester), contains('root-overlay preview is isolated'));
    await openMenu(tester); expect(find.text('First'), findsOneWidget);
    final controller = sdk.controller;
    await pumpMenu(tester, submenu());
    expect(sdkSubmenu(tester).controller, same(controller));
    expect(controller!.isOpen, true);
  });
  testWidgets('dense dual styles exceed old512cap and all701 style leaves resolve independently', (tester) async {
    final seen = <String>{};
    for (final circles in [false, true]) {
      for (final paints in [false, true]) {
      final props = submenuStyles(circles: circles, paints: paints);
      seen.addAll(props.keys);
      expect(props.length, greaterThan(512));
      await pumpMenu(tester, submenu(props: props, emptyChild: true));
      final sdk = sdkSubmenu(tester);
      for(final states in <Set<WidgetState>>[{}, for(final s in WidgetState.values) {s}, WidgetState.values.toSet()]) {
        expect(sdk.style!.shape!.resolve(states), circles ? isA<CircleBorder>() : isA<RoundedRectangleBorder>());
        expect(sdk.menuStyle!.shape!.resolve(states), circles ? isA<CircleBorder>() : isA<RoundedRectangleBorder>());
        expect(sdk.style!.textStyle!.resolve(states), isNotNull);
        expect(sdk.menuStyle!.padding!.resolve(states), isNotNull);
        expect(sdk.style!.minimumSize!.resolve(states), isNotNull);
        expect(sdk.menuStyle!.minimumSize!.resolve(states), isNotNull);
      }
      final button = tester.widget<TextButton>(find.descendant(of: find.byType(SubmenuButton), matching: find.byType(TextButton)).first);
      expect(button.clipBehavior, isNull); // SDK resolves effective builders to antiAlias.
      expect(button.statesController, isNull);
      expect(sdk.style!.backgroundBuilder, isNotNull); expect(sdk.style!.foregroundBuilder, isNotNull);
      await openMenu(tester); expect(tester.takeException(), isNull);
      sdk.controller!.close(); await tester.pumpAndSettle();
      }
    }
    expect(seen, hasLength(701));
  });
  testWidgets('sparse local padding retries MenuTheme then native8 and independent button defaults', (tester) async {
    for (final themed in [false,true]) {
      await pumpMenu(tester, submenu(props: {
        'menuStyleHoveredPadding': {'kind':'edgeInsets','left':2.0,'top':3.0,'right':4.0,'bottom':5.0},
        'menuStyleVisualDensityHorizontal': menuN(1.0),
      }), theme: ThemeData(menuTheme: themed ? const MenuThemeData(style: MenuStyle(
        padding: WidgetStatePropertyAll(EdgeInsets.all(17)), backgroundColor: WidgetStatePropertyAll(Colors.amber))) : null));
      final sdk = sdkSubmenu(tester);
      expect(sdk.style, isNull);
      expect(sdk.menuStyle!.padding!.resolve({})!.resolve(TextDirection.ltr), themed ? const EdgeInsets.all(17) : const EdgeInsets.symmetric(vertical:8));
      expect(sdk.menuStyle!.padding!.resolve({WidgetState.hovered})!.resolve(TextDirection.ltr), const EdgeInsets.fromLTRB(2,3,4,5));
      expect(sdk.menuStyle!.visualDensity, const VisualDensity(horizontal:1));
      await openMenu(tester); sdk.controller!.close(); await tester.pumpAndSettle();
    }
  });
  testWidgets('four icon buckets skip omitted active states null terminates and IconNone hides glyph', (tester) async {
    await pumpMenu(tester, submenu(props: {
      'submenuIconDefault': submenuIcon(Icons.add.codePoint),
      'submenuIconDisabled': menuNull,
      'submenuIconFocused': submenuIcon(Icons.close.codePoint),
    }));
    var icons = sdkSubmenu(tester).submenuIcon!;
    expect((icons.resolve({})! as Icon).icon, Icons.add);
    expect((icons.resolve({WidgetState.hovered,WidgetState.focused})! as Icon).icon, Icons.close);
    expect(icons.resolve({WidgetState.disabled,WidgetState.focused}), isNull);
    await pumpMenu(tester, submenu(props: {'submenuIconDefault': submenuIcon(null), 'submenuIconHovered': menuRef}));
    icons = sdkSubmenu(tester).submenuIcon!;
    expect(icons.resolve({}), isA<Icon>()); expect((icons.resolve({})! as Icon).icon, isNull);
    expect(icons.resolve({WidgetState.hovered}), isNull);
    expect(menuDiagnostics(tester), contains('submenuIconHovered project reference'));
  });
  testWidgets('native keyboard and hover delays open menus without project callbacks', (tester) async {
    await pumpMenu(tester, submenu(props: {'hoverOpenDelayUs': menuN(200000)}));
    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
    await mouse.addPointer(location: Offset.zero); await mouse.moveTo(tester.getCenter(find.text('Submenu')));
    await tester.pump(const Duration(milliseconds:100)); expect(sdkSubmenu(tester).controller!.isOpen, false);
    await tester.pump(const Duration(milliseconds:150)); await tester.pumpAndSettle();
    expect(sdkSubmenu(tester).controller!.isOpen, true);
    await tester.sendKeyEvent(LogicalKeyboardKey.escape); await tester.pumpAndSettle();
    expect(sdkSubmenu(tester).controller!.isOpen, false);
    await mouse.removePointer();
    final button = tester.widget<TextButton>(find.descendant(of: find.byType(SubmenuButton), matching: find.byType(TextButton)).first);
    button.focusNode!.requestFocus(); await tester.pumpAndSettle();
    await tester.sendKeyEvent(LogicalKeyboardKey.enter); await tester.pumpAndSettle();
    expect(sdkSubmenu(tester).controller!.isOpen, true); expect(tester.takeException(), isNull);
  });
  testWidgets('actual three label slots RTL ordering native parent orientation and nested close propagation', (tester) async {
    for (final direction in ['ltr','rtl']) {
      final model = submenu(id:30, leading:menuText(32,'Leading'), trailing:menuText(33,'Trailing'),
        props:{'submenuIconDefault':submenuIcon(Icons.star.codePoint)}, items:[menuItem(40,'Nested leaf')]);
      await pumpMenu(tester, model, selected:menuId(30), direction:direction);
      expect(find.byIcon(Icons.star), findsNothing, reason:'native standalone decoration is hidden');
      final leading = tester.getCenter(find.text('Leading')).dx;
      final trailing = tester.getCenter(find.text('Trailing')).dx;
      expect(leading < trailing, direction == 'ltr');
      await pumpMenu(tester, menuAnchor(items:[model]), direction:direction);
      await openMenu(tester);
      expect(find.byIcon(Icons.star), findsOneWidget, reason:'native vertical parent reveals submenu indicator');
      await tester.tap(find.text('Submenu')); await tester.pumpAndSettle();
      expect(sdkSubmenu(tester).controller!.isOpen, true);
      expect(find.text('Nested leaf'), findsOneWidget);
      final menu = sdkMenu(tester);
      await tester.tap(find.text('Nested leaf')); await tester.pumpAndSettle();
      expect(menu.controller!.isOpen, false); expect(find.text('Nested leaf'), findsNothing);
      expect(tester.takeException(), isNull);
    }
  });
  testWidgets('native expanded disabled semantics are not replaced by a synthetic button action', (tester) async {
    final semantics = tester.ensureSemantics();
    try {
      await pumpMenu(tester, submenu());
      final native = find.descendant(of:find.byType(SubmenuButton),matching:find.byType(TextButton)).first;
      var data = tester.getSemantics(native).getSemanticsData();
      expect(data.hasAction(ui.SemanticsAction.tap), true);
       expect(data.flagsCollection.isExpanded, ui.Tristate.isFalse);
      expect(data.flagsCollection.isToggled, ui.Tristate.none);
      expect(data.flagsCollection.isChecked, ui.CheckedState.none);
      await tester.tap(find.text('Submenu')); await tester.pumpAndSettle();
      data = tester.getSemantics(native).getSemanticsData();
      expect(data.flagsCollection.isExpanded, ui.Tristate.isTrue);
      await pumpMenu(tester, submenu(items:[]));
      data = tester.getSemantics(native).getSemanticsData();
      expect(data.hasAction(ui.SemanticsAction.tap), false);
      expect(data.flagsCollection.isEnabled, ui.Tristate.isFalse);
    } finally { semantics.dispose(); }
  });
  testWidgets('native button MenuButtonTheme and panel MenuTheme remain independent with identity builders and inherited reset', (tester) async {
    final theme = ThemeData(
      textButtonTheme: TextButtonThemeData(style:TextButton.styleFrom(backgroundColor:Colors.red)),
      menuButtonTheme: MenuButtonThemeData(style:ButtonStyle(backgroundColor:const WidgetStatePropertyAll(Colors.green),
        backgroundBuilder:(context,states,child) => child ?? const SizedBox.shrink())),
      menuTheme:const MenuThemeData(style:MenuStyle(backgroundColor:WidgetStatePropertyAll(Colors.amber))),
    );
    for(final props in <Map<String,Object?>>[{}, {'styleBackgroundBuilder':menuRef,'clipBehavior':menuE('Clip','none')}, {}]) {
      await pumpMenu(tester, submenu(props:props), theme:theme);
      final sdk = sdkSubmenu(tester);
      final native = tester.widget<TextButton>(find.descendant(of:find.byType(SubmenuButton),matching:find.byType(TextButton)).first);
      expect(native.style!.backgroundColor!.resolve({}), Colors.green);
      expect(native.style!.backgroundBuilder, isNotNull);
      expect(native.clipBehavior, isNull, reason:'SubmenuButton Clip only applies to popup, not TextButton');
      await openMenu(tester);
      expect(tester.widgetList<Material>(find.ancestor(of:find.text('First'),matching:find.byType(Material))).any((m)=>m.color==Colors.amber),true);
      sdk.controller!.close(); await tester.pumpAndSettle();
    }
  });
  testWidgets('open popup is exact drop and move destination closed popup has no stale geometry', (tester) async {
    CanvasDropResolver? drop; CanvasMovePreviewResolver? move;
    await pumpMenu(tester, submenu(),onDrop:(v)=>drop=v,onMove:(v)=>move=v);
    expect(move!(menuId(10),menuId(2),'menuChildren',1),isNull);
    await openMenu(tester);
    final surface=tester.getRect(find.byKey(const ValueKey('canvas-interaction-surface')));
    final second=tester.getRect(find.byKey(ValueKey('canvas-widget-${menuId(20)}')));
    final point=Offset(second.center.dx,second.bottom-2);
    final x=((point.dx-surface.left)/surface.width*1000000).round();
    final y=((point.dy-surface.top)/surface.height*1000000).round();
    final target=drop!(x,y)!;
    expect(target.parentWidgetId,menuId(2)); expect(target.slotName,'menuChildren'); expect(target.insertionIndex,2);
    expect(move!(menuId(10),menuId(2),'menuChildren',1),isNotNull);
    sdkSubmenu(tester).controller!.close(); await tester.pumpAndSettle();
    expect(move!(menuId(10),menuId(2),'menuChildren',1),isNull);
    expect(drop!(x,y)?.parentWidgetId,isNot(menuId(2)));
    await pumpMenu(tester,submenu(items:[],emptyChild:true),onDrop:(v)=>drop=v);
    await openMenu(tester);
    final anchor=tester.getCenter(find.byKey(ValueKey('canvas-widget-${menuId(2)}')));
    final empty=drop!(((anchor.dx-surface.left)/surface.width*1000000).round(),((anchor.dy-surface.top)/surface.height*1000000).round());
    expect(empty!.slotName,'menuChildren'); expect(empty.insertionIndex,0);
  });
  testWidgets('nullable refs all optional masks and signed offsets preserve actual native arguments', (tester) async {
    for(final name in ['onHover','onFocusChange','onOpen','onClose','controller','style','menuStyle','alignmentOffset',
      'focusNode','statesController','submenuIcon','onAnimationStatusChanged',
      'submenuIconDefault','submenuIconDisabled','submenuIconHovered','submenuIconFocused']) {
      await pumpMenu(tester,submenu(props:{name:menuNull}));
      final sdk=sdkSubmenu(tester);
      expect(sdk.focusNode,isNull); expect(sdk.statesController,isNull);
      expect(sdk.style,isNull); expect(sdk.menuStyle,isNull);
      expect(find.text('Submenu'),findsOneWidget);
    }
    for(var mask=0;mask<8;mask++) {
      await pumpMenu(tester,submenu(emptyChild:mask&1==0,leading:mask&2==0?null:menuText(5,'L'),
        trailing:mask&4==0?null:menuText(6,'R'), props:{'alignmentOffset':{'kind':'offset','dx':-7.0,'dy':9.0}}));
      final sdk=sdkSubmenu(tester);
      expect(sdk.child!=null,mask&1!=0); expect(sdk.leadingIcon!=null,mask&2!=0); expect(sdk.trailingIcon!=null,mask&4!=0);
      expect(sdk.alignmentOffset,const Offset(-7,9));
    }
  });
  testWidgets('unsafe inherited popup geometry is filtered before padding prepass without losing children', (tester) async {
    for(final padding in [const EdgeInsets.all(-1),const EdgeInsets.all(double.infinity)]) {
      await pumpMenu(tester,submenu(),theme:ThemeData(menuTheme:MenuThemeData(style:MenuStyle(padding:WidgetStatePropertyAll(padding),
        minimumSize:const WidgetStatePropertyAll(Size(double.infinity,3)),elevation:const WidgetStatePropertyAll(double.nan)))));
      expect(menuDiagnostics(tester),contains('SubmenuButton inherited menu geometry preview unavailable'));
      expect(find.text('Submenu'),findsOneWidget);
      await openMenu(tester); expect(find.text('First'),findsOneWidget);
      expect(tester.getSize(find.text('First')).isFinite,true);
      sdkSubmenu(tester).controller!.close(); await tester.pumpAndSettle();
    }
  });
}
