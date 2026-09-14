import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.NavigationRail';

String _id(int n) => '793560e4-67ae-4e99-907b-${n.toString().padLeft(12, '0')}';

Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> _i(int value) => {'kind': 'integer', 'value': value};
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

Map<String, Object?> _navigationRail({
  int? selectedIndex = 0,
  List<Object?>? destinations,
  bool extended = false,
}) => _node(
  2,
  _type,
  {
    'selectedIndex': selectedIndex == null
        ? const {'kind': 'null'}
        : _i(selectedIndex),
    if (extended) 'extended': _b(true),
  },
  {
    'leading': _single(_text(8, 'Leading')),
    'trailing': _single(_text(9, 'Trailing')),
    'destinations': _list(
      destinations ?? [_text(10, 'Home'), _text(11, 'Settings')],
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
  test('NavigationRail schema and drop slots stay in protocol sync', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(
      start,
      contract.indexOf('W|', start + 2),
    );
    expect(
      section.split('\n').where((line) => line.startsWith('P|')),
      hasLength(20),
    );
    expect(section, contains('P|selectedIndex|integer,null|1|integer:0|'));
    expect(
      section,
      contains(
        'P|onDestinationSelected|callback,dartObjectReference,null,string|0|-|-|',
      ),
    );
    expect(section, contains('P|labelType|enum,null|0|-|-|'));
    expect(section, contains('S|leading|single|0|0|1|any'));
    expect(section, contains('S|trailing|single|0|0|1|any'));
    expect(section, contains('S|destinations|list|1|0|10000|any'));
    expect(canvasDropSlotsForWidgetType(_type), hasLength(3));
    expect(canvasReviewedRequiredWrapperSlot(_type), isNull);
  });

  testWidgets(
    'native NavigationRail adapts destinations and preserves selection',
    (tester) async {
      await _pump(tester, _navigationRail(selectedIndex: 1));
      final rail = tester.widget<NavigationRail>(find.byType(NavigationRail));
      expect(rail.selectedIndex, 1);
      expect(rail.destinations, hasLength(2));
      expect(find.text('Home'), findsOneWidget);
      expect(find.text('Settings'), findsOneWidget);
      expect(find.text('Destination 1'), findsOneWidget);
      expect(find.text('Destination 2'), findsOneWidget);
      expect(find.text('Leading'), findsOneWidget);
      expect(find.text('Trailing'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'empty destinations preserve nullable selection without SDK assertion',
    (tester) async {
      await _pump(
        tester,
        _navigationRail(selectedIndex: null, destinations: const []),
      );
      final rail = tester.widget<NavigationRail>(find.byType(NavigationRail));
      expect(rail.selectedIndex, isNull);
      expect(rail.destinations, isEmpty);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('invalid selected index is previewed safely', (tester) async {
    await _pump(tester, _navigationRail(selectedIndex: 9));
    final rail = tester.widget<NavigationRail>(find.byType(NavigationRail));
    expect(rail.selectedIndex, isNull);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'extended rail with explicit label type is normalized for Canvas',
    (tester) async {
      final root = _navigationRail(extended: true);
      (root['properties'] as Map<String, Object?>)['labelType'] = _e(
        'NavigationRailLabelType',
        'selected',
      );
      await _pump(tester, root);
      final rail = tester.widget<NavigationRail>(find.byType(NavigationRail));
      expect(rail.extended, isFalse);
      expect(tester.takeException(), isNull);
    },
  );
}
