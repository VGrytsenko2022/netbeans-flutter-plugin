import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.NavigationDrawer';

String _id(int n) => 'b935e4e2-cd62-4d91-a7e2-${n.toString().padLeft(12, '0')}';

Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> _i(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _cb() => {'kind': 'callbackPresence'};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _list(List<Object?> children) => {
  'kind': 'list',
  'children': children,
};
Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> properties,
  Map<String, Object?> slots,
) => {'id': _id(id), 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _text(int id, String value) =>
    _node(id, 'flutter.widgets.Text', {'data': _s(value)}, const {});

Map<String, Object?> _navigationDrawer({
  int? selectedIndex = 0,
  List<Object?>? children,
  Object? header,
  Object? footer,
  bool callback = false,
}) => _node(
  2,
  _type,
  {
    'selectedIndex': selectedIndex == null
        ? const {'kind': 'null'}
        : _i(selectedIndex),
    if (callback) 'onDestinationSelected': _cb(),
  },
  {
    'header': _single(header),
    'footer': _single(footer),
    'children': _list(
      children ?? [_text(10, 'Home'), _text(11, 'Settings')],
    ),
  },
);

CanvasModel _model(Object root) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  model['root'] = _node(900, 'flutter.material.Scaffold', {}, {
    'body': _single(root),
  });
  return CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
}

Widget _canvas(Object root) => CanvasDocumentView(
  model: _model(root),
  selectedWidgetId: null,
  onSelected: (_) {},
  onDropResolverChanged: (_) {},
);

Future<void> _pump(WidgetTester tester, Object root) async {
  await tester.pumpWidget(MaterialApp(home: _canvas(root)));
  await tester.pumpAndSettle();
  expect(tester.takeException(), isNull);
}

void main() {
  test('NavigationDrawer schema and drop slots stay in protocol sync', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(start, contract.indexOf('W|', start + 2));
    expect(
      section.split('\n').where((line) => line.startsWith('P|')),
      hasLength(9),
    );
    expect(section, contains('P|selectedIndex|integer,null|1|integer:0|'));
    expect(
      section,
      contains(
        'P|onDestinationSelected|callback,dartObjectReference,null,string|0|-|-|',
      ),
    );
    expect(section, contains('S|header|single|0|0|1|any'));
    expect(section, contains('S|footer|single|0|0|1|any'));
    expect(section, contains('S|children|list|1|0|10000|any'));
    expect(canvasDropSlotsForWidgetType(_type), hasLength(3));
    expect(canvasReviewedRequiredWrapperSlot(_type), isNull);
  });

  testWidgets(
    'native NavigationDrawer adapts children and preserves selection',
    (tester) async {
      await _pump(
        tester,
        _navigationDrawer(
          selectedIndex: 1,
          header: _text(20, 'Header'),
          footer: _text(21, 'Footer'),
          callback: true,
        ),
      );
      final drawer = tester.widget<NavigationDrawer>(
        find.byType(NavigationDrawer),
      );
      expect(drawer.selectedIndex, 1);
      expect(drawer.children, hasLength(2));
      expect(drawer.onDestinationSelected, isNotNull);
      expect(find.text('Home'), findsOneWidget);
      expect(find.text('Settings'), findsOneWidget);
      expect(find.text('Destination 1'), findsOneWidget);
      expect(find.text('Destination 2'), findsOneWidget);
      expect(find.text('Header'), findsOneWidget);
      expect(find.text('Footer'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'empty children preserve nullable selection without SDK assertion',
    (tester) async {
      await _pump(
        tester,
        _navigationDrawer(selectedIndex: null, children: const []),
      );
      final drawer = tester.widget<NavigationDrawer>(
        find.byType(NavigationDrawer),
      );
      expect(drawer.selectedIndex, isNull);
      expect(drawer.children, isEmpty);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('invalid selected index is previewed safely', (tester) async {
    await _pump(tester, _navigationDrawer(selectedIndex: 9));
    final drawer = tester.widget<NavigationDrawer>(
      find.byType(NavigationDrawer),
    );
    expect(drawer.selectedIndex, isNull);
    expect(tester.takeException(), isNull);
  });
}
