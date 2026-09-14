import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.NavigationBar';

String _id(int n) => '793560e4-67ae-4e99-907b-${n.toString().padLeft(12, '0')}';

Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> _i(int value) => {'kind': 'integer', 'value': value};
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

Map<String, Object?> _navigationBar({
  int selectedIndex = 0,
  List<Object?>? destinations,
}) => _node(
  2,
  _type,
  {'selectedIndex': _i(selectedIndex)},
  {
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
  test('NavigationBar schema and drop slot stay in protocol sync', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(
      start,
      contract.indexOf('W|', start + 2),
    );
    expect(
      section.split('\n').where((line) => line.startsWith('P|')),
      hasLength(15),
    );
    expect(section, contains('P|selectedIndex|integer|1|integer:0|'));
    expect(
      section,
      contains(
        'P|onDestinationSelected|callback,dartObjectReference,null,string|0|-|-|',
      ),
    );
    expect(section, contains('P|labelBehavior|enum,null|0|-|-|'));
    expect(section, contains('S|destinations|list|1|0|10000|any'));
    expect(canvasDropSlotsForWidgetType(_type), hasLength(1));
    expect(canvasReviewedRequiredWrapperSlot(_type), isNull);
  });

  testWidgets(
    'native NavigationBar mounts all destinations and preserves selection',
    (tester) async {
      await _pump(tester, _navigationBar(selectedIndex: 1));
      final bar = tester.widget<NavigationBar>(find.byType(NavigationBar));
      expect(bar.selectedIndex, 1);
      expect(bar.destinations, hasLength(2));
      expect(find.text('Home'), findsOneWidget);
      expect(find.text('Settings'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'empty destination list is diagnosed without triggering the SDK assertion',
    (tester) async {
      await _pump(tester, _navigationBar(destinations: const []));
      expect(find.text('Add at least two destinations'), findsOneWidget);
      expect(find.byType(NavigationBar), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );
}
