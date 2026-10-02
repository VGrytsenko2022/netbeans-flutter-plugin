import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Material';

String _id(int n) => 'd9d1a8d0-4f6e-4f89-a8cf-${n.toString().padLeft(12, '0')}';

Map<String, Object?> _i(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _d(double value) => {'kind': 'double', 'value': value};
Map<String, Object?> _color(String argb) => {'kind': 'color', 'argb': argb};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _reference() => {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};

Map<String, Object?> _text(int id, String value) =>
    _node(id, 'flutter.widgets.Text', {
      'data': {'kind': 'string', 'value': value},
    }, const {});

Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> properties,
  Map<String, Object?> slots,
) => {'id': _id(id), 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _material({
  Map<String, Object?>? properties,
  Object? child,
}) {
  final effectiveProperties = <String, Object?>{};
  if (properties != null) effectiveProperties.addAll(properties);
  return _node(2, _type, effectiveProperties, {'child': _single(child)});
}

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
  test('Material schema exposes all twelve properties and optional child', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(
      start,
      contract.indexOf('W|', start + 2),
    );
    expect(
      section.split('\n').where((line) => line.startsWith('P|')),
      hasLength(12),
    );
    expect(section, contains('P|materialType|enum|'));
    expect(
      section,
      contains('P|borderRadius|borderRadius,dartObjectReference,null|'),
    );
    expect(
      section,
      contains('P|animationDurationUs|dartObjectReference,integer,null|'),
    );
    expect(section, contains('S|child|single|0|0|1|any'));
  });

  testWidgets('native Material preserves reviewed fields and child', (
    tester,
  ) async {
    await _pump(
      tester,
      _material(
        properties: {
          'materialType': _enum('MaterialType', 'card'),
          'elevation': _d(3.5),
          'color': _color('0xFF123456'),
          'borderOnForeground': {'kind': 'boolean', 'value': false},
          'clipBehavior': _enum('Clip', 'hardEdge'),
          'animationDurationUs': _i(1234),
          'animateColor': {'kind': 'boolean', 'value': true},
        },
        child: _text(3, 'Surface'),
      ),
    );
    final material = tester.widget<Material>(
      find.byKey(ValueKey('canvas-material-${_id(2)}')),
    );
    expect(material.type, MaterialType.card);
    expect(material.elevation, 3.5);
    expect(material.color, const Color(0xFF123456));
    expect(material.borderOnForeground, isFalse);
    expect(material.clipBehavior, Clip.hardEdge);
    expect(material.animationDuration, const Duration(microseconds: 1234));
    expect(material.animateColor, isTrue);
    expect(find.text('Surface'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'application-owned shape and text style references stay diagnosed',
    (tester) async {
      await _pump(
        tester,
        _material(
          properties: {
            'shape': _reference(),
            'borderRadius': _reference(),
            'textStyle': _reference(),
            'materialType': _enum('MaterialType', 'circle'),
            'color': _color('0xFF123456'),
          },
        ),
      );
      final messages = tester
          .widgetList<Tooltip>(find.byType(Tooltip))
          .map((tooltip) => tooltip.message ?? '')
          .toList();
      expect(
        messages,
        contains(
          predicate<String>(
            (message) =>
                message.contains(
                  'Canvas does not execute project references',
                ) &&
                message.contains('circle Material cannot use'),
          ),
        ),
      );
      expect(find.byKey(ValueKey('canvas-material-${_id(2)}')), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}
