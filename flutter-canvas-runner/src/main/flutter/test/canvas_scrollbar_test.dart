import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Scrollbar';

String _id(int n) => 'f5f9dc3d-3a93-4e40-8c84-${n.toString().padLeft(12, '0')}';

Map<String, Object?> _boolean(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _double(double value) => {'kind': 'double', 'value': value};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _reference() => {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> _single(Object? child) => {'kind': 'single', 'child': child};

Map<String, Object?> _text(int id, String value) => _node(
  id,
  'flutter.widgets.Text',
  {'data': {'kind': 'string', 'value': value}},
  const {},
);

Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> properties,
  Map<String, Object?> slots,
) => {'id': _id(id), 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _scrollbar({
  Map<String, Object?>? properties,
  Object? child,
}) => _node(2, _type, properties ?? const {}, {'child': _single(child)});

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
  test('Scrollbar schema exposes eight optional properties and required child', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(start, contract.indexOf('W|', start + 2));
    expect(section.split('\n').where((line) => line.startsWith('P|')), hasLength(8));
    expect(section, contains('P|scrollbarOrientation|enum,null|'));
    expect(section, contains('P|thickness|double,integer,null|'));
    expect(section, contains('S|child|single|1|1|1|any'));
    expect(section, contains('C|$_type|paletteCreate|wrapExistingChild|child'));
  });

  testWidgets('native Scrollbar preserves reviewed fields and child', (tester) async {
    await _pump(
      tester,
      _scrollbar(
        properties: {
          'thumbVisibility': _boolean(true),
          'trackVisibility': _boolean(true),
          'thickness': _double(8),
          'interactive': _boolean(false),
          'scrollbarOrientation': _enum('ScrollbarOrientation', 'left'),
        },
        child: _text(3, 'Scrollable preview'),
      ),
    );
    final scrollbar = tester.widget<Scrollbar>(
      find.byKey(ValueKey('canvas-scrollbar-${_id(2)}')),
    );
    expect(scrollbar.thumbVisibility, isTrue);
    expect(scrollbar.trackVisibility, isTrue);
    expect(scrollbar.thickness, 8);
    expect(scrollbar.interactive, isFalse);
    expect(scrollbar.scrollbarOrientation, ScrollbarOrientation.left);
    expect(find.text('Scrollable preview'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('project references remain diagnosed while defaults stay safe', (tester) async {
    await _pump(
      tester,
      _scrollbar(
        properties: {
          'controller': _reference(),
          'radius': _reference(),
          'notificationPredicate': _reference(),
        },
        child: _text(3, 'Fallback child'),
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
              message.contains('Canvas does not execute project references') &&
              message.contains('controller') &&
              message.contains('radius') &&
              message.contains('notificationPredicate'),
        ),
      ),
    );
    expect(find.text('Fallback child'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
