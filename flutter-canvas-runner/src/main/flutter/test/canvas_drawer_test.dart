import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Drawer';

String _id(int n) => 'd6a4e1c0-2d7d-4b8b-91d1-${n.toString().padLeft(12, '0')}';

Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> _i(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _d(String value) => {
  'kind': 'double',
  'value': double.parse(value),
};
Map<String, Object?> _color(String argb) => {'kind': 'color', 'argb': argb};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
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

Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> properties,
  Map<String, Object?> slots,
) => {'id': _id(id), 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _text(int id, String value) =>
    _node(id, 'flutter.widgets.Text', {'data': _s(value)}, const {});

Map<String, Object?> _drawer({
  Map<String, Object?>? properties,
  Object? child,
}) => _node(2, _type, properties ?? const <String, Object?>{}, {
  'child': _single(child),
});

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
  test('Drawer schema, projection and drop slot stay in protocol sync', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final section = contract.substring(
      start,
      contract.indexOf('W|', start + 2),
    );
    expect(
      section.split('\n').where((line) => line.startsWith('P|')),
      hasLength(8),
    );
    expect(
      section,
      contains('P|backgroundColor|color,dartObjectReference,null,themeToken|'),
    );
    expect(section, contains('P|clipBehavior|enum,null|'));
    expect(section, contains('P|elevation|double,integer,null|'));
    expect(section, contains('P|shape|dartObjectReference,null|'));
    expect(section, contains('P|width|double,integer,null|'));
    expect(section, contains('S|child|single|0|0|1|any'));
    expect(canvasDropSlotsForWidgetType(_type), hasLength(1));
    expect(canvasReviewedRequiredWrapperSlot(_type), isNull);
  });

  testWidgets('native Drawer preserves typed appearance, semantics and child', (
    tester,
  ) async {
    await _pump(
      tester,
      _drawer(
        properties: {
          'backgroundColor': _color('0xFF123456'),
          'elevation': _d('4.5'),
          'shadowColor': _theme('material.colorScheme.shadow'),
          'surfaceTintColor': _color('0x00112233'),
          'width': _i(280),
          'semanticLabel': _s('Project menu'),
          'clipBehavior': _enum('Clip', 'antiAlias'),
        },
        child: _text(10, 'Drawer content'),
      ),
    );
    final drawer = tester.widget<Drawer>(find.byType(Drawer));
    expect(drawer.backgroundColor, const Color(0xFF123456));
    expect(drawer.elevation, 4.5);
    expect(
      drawer.shadowColor,
      Theme.of(tester.element(find.byType(Drawer))).colorScheme.shadow,
    );
    expect(drawer.surfaceTintColor, const Color(0x00112233));
    expect(drawer.width, 280);
    expect(drawer.semanticLabel, 'Project menu');
    expect(drawer.clipBehavior, Clip.antiAlias);
    expect(find.text('Drawer content'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'shape references are diagnosed while Drawer remains renderable',
    (tester) async {
      await _pump(
        tester,
        _drawer(
          properties: {'shape': _reference()},
          child: _text(11, 'Safe content'),
        ),
      );
      expect(find.byType(Drawer), findsOneWidget);
      expect(find.text('Safe content'), findsOneWidget);
      expect(
        find.byWidgetPredicate(
          (widget) =>
              widget is Tooltip &&
              (widget.message?.contains(
                    'Canvas does not execute project references',
                  ) ??
                  false),
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('null and omitted values use Material defaults safely', (
    tester,
  ) async {
    await _pump(
      tester,
      _drawer(
        properties: {
          'backgroundColor': const {'kind': 'null'},
          'elevation': const {'kind': 'null'},
          'width': const {'kind': 'null'},
          'clipBehavior': const {'kind': 'null'},
        },
        child: null,
      ),
    );
    final drawer = tester.widget<Drawer>(find.byType(Drawer));
    expect(drawer.backgroundColor, isNull);
    expect(drawer.elevation, isNull);
    expect(drawer.width, isNull);
    expect(drawer.clipBehavior, isNull);
    expect(tester.takeException(), isNull);
  });
}
