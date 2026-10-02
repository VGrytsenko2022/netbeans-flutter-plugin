import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.widgets.ListWheelScrollView';

Map<String, Object?> _id(int n) => {
  'id': '9e1fda0b-1a2f-4c3d-8e4f-${n.toString().padLeft(12, '0')}',
};

Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> properties,
  Map<String, Object?> slots,
) => {..._id(id), 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _number(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
};

Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};

Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};

Map<String, Object?> _text(int id, String value) =>
    _node(id, 'flutter.widgets.Text', {'data': _string(value)}, const {});

Map<String, Object?> _wheel({
  Map<String, Object?> properties = const {},
  List<Object?> children = const [],
}) => _node(2, _type, properties, {
  'children': {'kind': 'list', 'children': children},
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
  test('ListWheelScrollView schema exposes all 18 properties and children', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(
      start,
      contract.indexOf('W|', start + 2),
    );
    expect(
      section.split('\n').where((line) => line.startsWith('P|')),
      hasLength(18),
    );
    expect(section, contains('P|itemExtent|double,integer|1|integer:50|'));
    expect(section, contains('P|perspective|double|0|-|double:0:0:0.01:1|'));
    expect(
      section,
      contains(
        'P|onSelectedItemChanged|callback,dartObjectReference,null,string|',
      ),
    );
    expect(section, contains('S|children|list|0|0|10000|any'));
  });

  testWidgets('native wheel preserves reviewed geometry and ordered children', (
    tester,
  ) async {
    await _pump(
      tester,
      _wheel(
        properties: {
          'diameterRatio': _number(1.5),
          'perspective': _number(.003),
          'offAxisFraction': _number(-.25),
          'useMagnifier': {'kind': 'boolean', 'value': true},
          'magnification': _number(1.2),
          'overAndUnderCenterOpacity': _number(.7),
          'itemExtent': _number(64),
          'squeeze': _number(1.1),
          'renderChildrenOutsideViewport': {'kind': 'boolean', 'value': true},
          'clipBehavior': _enum('Clip', 'none'),
          'hitTestBehavior': _enum('HitTestBehavior', 'translucent'),
          'restorationId': _string('wheel'),
          'dragStartBehavior': _enum('DragStartBehavior', 'start'),
          'changeReportingBehavior': _enum(
            'ChangeReportingBehavior',
            'onScrollEnd',
          ),
        },
        children: [_text(3, 'One'), _text(4, 'Two')],
      ),
    );
    final wheel = tester.widget<ListWheelScrollView>(
      find.byType(ListWheelScrollView),
    );
    expect(wheel.diameterRatio, 1.5);
    expect(wheel.perspective, .003);
    expect(wheel.offAxisFraction, -.25);
    expect(wheel.useMagnifier, isTrue);
    expect(wheel.magnification, 1.2);
    expect(wheel.itemExtent, 64);
    expect(wheel.renderChildrenOutsideViewport, isTrue);
    expect(wheel.clipBehavior, Clip.none);
    expect(find.text('One'), findsOneWidget);
    expect(find.text('Two'), findsOneWidget);
  });
}
