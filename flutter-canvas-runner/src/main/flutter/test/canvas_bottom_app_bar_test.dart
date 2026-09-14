import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.BottomAppBar';

String _id(int n) => 'b6a4e1c0-2d7d-4b8b-91d1-${n.toString().padLeft(12, '0')}';
Map<String, Object?> _i(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _d(String value) => {'kind': 'double', 'value': double.parse(value)};
Map<String, Object?> _color(String argb) => {'kind': 'color', 'argb': argb};
Map<String, Object?> _enum(String type, String value) => {'kind': 'enum', 'type': type, 'value': value};
Map<String, Object?> _reference() => {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> _single(Object? child) => {'kind': 'single', 'child': child};

Map<String, Object?> _node(int id, String type, Map<String, Object?> properties,
        Map<String, Object?> slots) =>
    {'id': _id(id), 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _text(int id, String value) =>
    _node(id, 'flutter.widgets.Text', {'data': {'kind': 'string', 'value': value}}, const {});

Map<String, Object?> _bottomAppBar({
  Map<String, Object?>? properties,
  Object? child,
}) => _node(2, _type, properties ?? const <String, Object?>{}, {'child': _single(child)});

CanvasModel _model(Object root) {
  final model = jsonDecode(utf8.decode(fixture.modelBytesForViewTest())) as Map<String, Object?>;
  model['root'] = _node(900, 'flutter.material.Scaffold', {}, {'body': _single(root)});
  return CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
}

Widget _canvas(Object root) => CanvasDocumentView(
      model: _model(root), selectedWidgetId: null, onSelected: (_) {}, onDropResolverChanged: (_) {},
    );

Future<void> _pump(WidgetTester tester, Object root) async {
  await tester.pumpWidget(MaterialApp(home: _canvas(root)));
  await tester.pumpAndSettle();
  expect(tester.takeException(), isNull);
}

void main() {
  test('BottomAppBar schema, projection and drop slot stay in protocol sync', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(start, contract.indexOf('W|', start + 2));
    expect(section.split('\n').where((line) => line.startsWith('P|')), hasLength(9));
    expect(section, contains('P|clipBehavior|enum|'));
    expect(section, contains('P|notchMargin|double,integer|'));
    expect(section, contains('P|padding|dartObjectReference,edgeInsets,edgeInsetsDirectional,null|'));
    expect(section, contains('P|shape|dartObjectReference,null|'));
    expect(section, contains('S|child|single|0|0|1|any'));
    expect(canvasDropSlotsForWidgetType(_type), hasLength(1));
    expect(canvasReviewedRequiredWrapperSlot(_type), isNull);
  });

  testWidgets('native BottomAppBar preserves appearance, geometry and child', (tester) async {
    await _pump(tester, _bottomAppBar(properties: {
      'color': _color('0xFF123456'),
      'elevation': _d('3.5'),
      'notchMargin': _i(8),
      'clipBehavior': _enum('Clip', 'antiAlias'),
      'height': _d('72'),
    }, child: _text(10, 'Bar content')));
    final bar = tester.widget<BottomAppBar>(find.byType(BottomAppBar));
    expect(bar.color, const Color(0xFF123456));
    expect(bar.elevation, 3.5);
    expect(bar.notchMargin, 8);
    expect(bar.clipBehavior, Clip.antiAlias);
    expect(bar.height, 72);
    expect(find.text('Bar content'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('shape references are diagnosed while BottomAppBar remains renderable', (tester) async {
    await _pump(tester, _bottomAppBar(properties: {'shape': _reference()}, child: _text(11, 'Safe content')));
    expect(find.byType(BottomAppBar), findsOneWidget);
    expect(find.text('Safe content'), findsOneWidget);
    expect(find.byWidgetPredicate((widget) => widget is Tooltip &&
        (widget.message?.contains('Canvas does not execute project references') ?? false)), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
