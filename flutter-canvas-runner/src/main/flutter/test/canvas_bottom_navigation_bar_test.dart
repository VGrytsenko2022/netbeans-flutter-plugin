import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.BottomNavigationBar';

String _id(int n) => 'c2f6d9bd-9f19-4af5-b7a5-${n.toString().padLeft(12, '0')}';

Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
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

Map<String, Object?> _bottomNavigationBar({
  Map<String, Object?>? properties,
  List<Object?>? items,
}) {
  final effectiveProperties = <String, Object?>{'currentIndex': _i(0)};
  if (properties != null) {
    effectiveProperties.addAll(properties);
  }
  return _node(2, _type, effectiveProperties, {
    'items': _list(items ?? [_text(10, 'Home'), _text(11, 'Settings')]),
  });
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
  test('BottomNavigationBar schema and required item slot stay in sync', () {
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
    expect(section, contains('P|barType|enum,null|'));
    expect(section, contains('P|currentIndex|integer|1|integer:0|'));
    expect(
      section,
      contains('P|onTap|callback,dartObjectReference,null,string|'),
    );
    expect(section, contains('S|items|list|1|0|10000|any'));
    expect(canvasDropSlotsForWidgetType(_type), hasLength(1));
    expect(canvasReviewedRequiredWrapperSlot(_type), isNull);
  });

  testWidgets(
    'native BottomNavigationBar preserves selection and reviewed fields',
    (tester) async {
      await _pump(
        tester,
        _bottomNavigationBar(
          properties: {
            'onTap': _s('noop'),
            'currentIndex': _i(1),
            'barType': _enum('BottomNavigationBarType', 'shifting'),
            'backgroundColor': _color('0xFF123456'),
            'iconSize': _d(30),
            'selectedFontSize': _d(16),
            'unselectedFontSize': _d(11),
            'showSelectedLabels': {'kind': 'boolean', 'value': true},
            'showUnselectedLabels': {'kind': 'boolean', 'value': false},
            'enableFeedback': {'kind': 'boolean', 'value': false},
            'landscapeLayout': _enum(
              'BottomNavigationBarLandscapeLayout',
              'centered',
            ),
            'useLegacyColorScheme': {'kind': 'boolean', 'value': false},
          },
        ),
      );
      final bar = tester.widget<BottomNavigationBar>(
        find.byType(BottomNavigationBar),
      );
      expect(bar.currentIndex, 1);
      expect(bar.type, BottomNavigationBarType.shifting);
      expect(bar.items, hasLength(2));
      expect(bar.backgroundColor, const Color(0xFF123456));
      expect(bar.iconSize, 30);
      expect(bar.selectedFontSize, 16);
      expect(bar.unselectedFontSize, 11);
      expect(bar.showSelectedLabels, isTrue);
      expect(bar.showUnselectedLabels, isFalse);
      expect(bar.enableFeedback, isFalse);
      expect(bar.landscapeLayout, BottomNavigationBarLandscapeLayout.centered);
      expect(bar.useLegacyColorScheme, isFalse);
      expect(find.text('Item 1'), findsOneWidget);
      expect(find.text('Item 2'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'empty item list is diagnosed without triggering the SDK assertion',
    (tester) async {
      await _pump(tester, _bottomNavigationBar(items: const []));
      expect(find.text('Add at least two items'), findsOneWidget);
      expect(find.byType(BottomNavigationBar), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'application-owned references remain diagnosed and render with fallback',
    (tester) async {
      await _pump(
        tester,
        _bottomNavigationBar(
          properties: {
            'selectedIconTheme': _reference(),
            'selectedLabelStyle': _reference(),
            'mouseCursor': _reference(),
          },
        ),
      );
      expect(find.byType(BottomNavigationBar), findsOneWidget);
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
}
