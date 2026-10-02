import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.widgets.Builder';

Map<String, Object?> _id(int n) => {
  'id': '8d2f4e10-1a2b-4c3d-8e4f-${n.toString().padLeft(12, '0')}',
};

Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> properties,
  Map<String, Object?> slots,
) => {..._id(id), 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _builder({Object? callback}) => _node(2, _type, {
  'builder': callback ?? {'kind': 'callbackPresence'},
}, const {});

CanvasModel _model(Object root) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  model['root'] = _node(900, 'flutter.material.Scaffold', const {}, {
    'body': {'kind': 'single', 'child': root},
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
  test('Builder schema exposes a required callback and no child slots', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(
      start,
      contract.indexOf('W|', start + 2),
    );
    expect(
      section,
      contains(
        'P|builder|callback|1|string:bm9vcA|-|callback:callbackReference',
      ),
    );
    expect(section.split('\n').where((line) => line.startsWith('S|')), isEmpty);
  });

  testWidgets(
    'native Builder renders a bounded preview without executing project callback',
    (tester) async {
      await _pump(tester, _builder());
      expect(
        find.byWidgetPredicate(
          (widget) =>
              widget is SizedBox && widget.width == 48 && widget.height == 36,
        ),
        findsOneWidget,
      );
    },
  );

  test('Builder rejects a missing required callback', () {
    final model =
        jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
            as Map<String, Object?>;
    model['root'] = _node(900, 'flutter.material.Scaffold', const {}, {
      'body': {'kind': 'single', 'child': _node(2, _type, const {}, const {})},
    });
    expect(
      () => CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(model))),
      ),
      throwsFormatException,
    );
  });
}
