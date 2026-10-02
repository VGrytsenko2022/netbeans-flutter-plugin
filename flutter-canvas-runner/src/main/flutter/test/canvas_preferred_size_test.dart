import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.widgets.PreferredSize';

Map<String, Object?> _id(int n) => {
  'id': '8d2f4e10-1a2b-4c3d-8e4f-${n.toString().padLeft(12, '0')}',
};

Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> properties,
  Map<String, Object?> slots,
) => {..._id(id), 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _size(num width, num height) => {
  'kind': 'size',
  'width': width,
  'height': height,
};

Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};

Map<String, Object?> _text(int id, String value) =>
    _node(id, 'flutter.widgets.Text', {'data': _string(value)}, const {});

Map<String, Object?> _preferred({
  Map<String, Object?> properties = const {},
  Object? child,
}) => _node(2, _type, properties, {
  'child': {'kind': 'single', 'child': child},
});

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
  test('PreferredSize schema exposes required typed size and child', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(
      start,
      contract.indexOf('W|', start + 2),
    );
    expect(
      section.split('\n').where((line) => line.startsWith('P|')),
      hasLength(1),
    );
    expect(section, contains('P|preferredSize|size|1|size:100,56|'));
    expect(
      section,
      contains(
        'P|preferredSize|size|1|size:100,56|-|size:size:finiteNonNegative',
      ),
    );
    expect(section, contains('S|child|single|1|1|1|any'));
    expect(
      section,
      contains(
        'C|flutter.widgets.PreferredSize|paletteCreate|wrapExistingChild|child',
      ),
    );
  });

  testWidgets(
    'native PreferredSize preserves advertised size and required child',
    (tester) async {
      await _pump(
        tester,
        _preferred(
          properties: {'preferredSize': _size(120, 64)},
          child: _text(3, 'Toolbar'),
        ),
      );
      final preferred = tester.widget<PreferredSize>(
        find.byType(PreferredSize),
      );
      expect(preferred.preferredSize, const Size(120, 64));
      expect(find.text('Toolbar'), findsOneWidget);
    },
  );
}
