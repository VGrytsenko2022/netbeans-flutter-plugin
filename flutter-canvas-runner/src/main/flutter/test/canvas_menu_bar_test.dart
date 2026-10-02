import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_menu_anchor_test.dart';

const menuBarType = 'flutter.material.MenuBar';

Map<String, Object?> menuBar({
  int id = 2,
  Map<String, Object?> props = const {},
  List<Object?>? items,
}) => menuNode(
      id,
      menuBarType,
      props,
      {'children': menuList(items ?? [menuItem(10, 'File'), menuItem(20, 'Edit')])},
    );

MenuBar sdkMenuBar(WidgetTester tester) =>
    tester.widgetList<MenuBar>(find.byType(MenuBar)).first;

String menuBarSection(String contract) {
  final start = contract.indexOf('W|$menuBarType\n');
  return contract.substring(start, contract.indexOf('W|', start + 2));
}

void main() {
  test('206 MenuBar properties, required children list and drop destination stay in protocol sync', () {
    final section = menuBarSection(canvasRuntimeWidgetSchemaContractForTesting());
    expect(section, menuBarSection(canvasReviewedWidgetSchemaContract));
    expect(section.split('\n').where((line) => line.startsWith('P|')), hasLength(206));
    expect(section, contains('P|style|dartObjectReference,null|0|-|-|'));
    expect(section, contains('dartObjectReference:v1:MenuStyle'));
    expect(section, contains('P|clipBehavior|enum|0|-|-|'));
    expect(section, contains('P|controller|dartObjectReference,null|0|-|-|'));
    expect(section, contains('S|children|list|1|0|10000|any'));
    expect(canvasReviewedRequiredWrapperSlot(menuBarType), isNull);
    expect(canvasDropSlotsForWidgetType(menuBarType), hasLength(1));
    expect(menuModel(menuBar()), isNotNull);
    expect(
      () => menuModel(menuNode(2, menuBarType, {}, const {})),
      throwsFormatException,
    );
    expect(
      () => menuModel(menuBar(props: {
        'style': menuNull,
        'styleElevation': menuN(2.0),
      })),
      throwsFormatException,
    );
  });

  testWidgets('native MenuBar keeps its horizontal children mounted and exposes stable defaults',
      (tester) async {
    await pumpMenu(tester, menuBar());
    final bar = sdkMenuBar(tester);
    expect(bar.controller, isNotNull);
    expect(bar.style, isNull);
    expect(bar.clipBehavior, Clip.none);
    expect(bar.children, hasLength(2));
    expect(find.byType(MenuItemButton), findsNWidgets(2));
    expect(find.text('File'), findsOneWidget);
    expect(find.text('Edit'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('local MenuStyle is rendered as one native style and project references remain inert',
      (tester) async {
    await pumpMenu(tester, menuBar(props: {
      'styleElevation': menuN(7.0),
      'clipBehavior': menuE('Clip', 'antiAlias'),
    }));
    var bar = sdkMenuBar(tester);
    expect(bar.style, isNotNull);
    expect(bar.style!.elevation!.resolve({}), 7.0);
    expect(bar.clipBehavior, Clip.antiAlias);

    await pumpMenu(tester, menuBar(props: {
      'style': menuRef,
      'controller': menuRef,
    }));
    bar = sdkMenuBar(tester);
    expect(bar.style, isNull);
    expect(bar.controller, isNotNull);
    expect(menuDiagnostics(tester), contains('MenuBar style project reference'));
    expect(menuDiagnostics(tester), contains('MenuBar controller project reference'));
    expect(tester.takeException(), isNull);
  });
}
