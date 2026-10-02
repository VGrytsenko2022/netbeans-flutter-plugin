import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.CheckboxListTile';
const _id = 'a792df78-0d7e-4cf1-a375-000000000001';
const _boundaryId = 'a792df78-0d7e-4cf1-a375-000000000082';
const _reference = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};

Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _node(
  String id,
  String type,
  Map<String, Object?> properties, [
  Map<String, Object?> slots = const {},
]) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _model({
  String variant = 'standard',
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  Map<String, Object?> slots = const {},
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  model['root'] = _node(
    'a792df78-0d7e-4cf1-a375-000000000090',
    'flutter.material.Scaffold',
    {},
    {
      'body': _single(
        _node(
          'a792df78-0d7e-4cf1-a375-000000000091',
          'flutter.widgets.Center',
          {},
          {
            'child': _single(
              _node(_boundaryId, 'flutter.widgets.RepaintBoundary', {}, {
                'child': _single(
                  _node(_id, _type, {
                    'value': _bool(false),
                    'onChanged': _string('noop'),
                    'enabled': _bool(true),
                    'variant': _string(variant),
                    ...properties,
                  }, slots),
                ),
              }),
            ),
          },
        ),
      ),
    },
  );
  return model;
}

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));

Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  String? selectedId,
  String? platform,
  List<String>? selected,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: ThemeData(
        platform: switch (platform) {
          'ios' => TargetPlatform.iOS,
          'macos' => TargetPlatform.macOS,
          _ => TargetPlatform.windows,
        },
      ),
      home: CanvasDocumentView(
        model: _decode(model),
        selectedWidgetId: selectedId,
        onSelected: selected?.add ?? (_) {},
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 350));
  await tester.pump();
  expect(tester.takeException(), isNull);
}

String _messages(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((widget) => widget.message ?? '')
    .where((message) => message.isNotEmpty)
    .join('\n');

void main() {
  test('CheckboxListTile runtime schema and drop slots are exact', () {
    String block(String source) {
      final start = source.indexOf('W|$_type\n');
      final end = source.indexOf('\nW|', start + 1);
      return source.substring(start, end);
    }

    final runtime = canvasRuntimeWidgetSchemaContractForTesting();
    final actual = block(runtime);
    expect(actual, block(canvasReviewedWidgetSchemaContract));
    expect(
      actual.split('\n').where((line) => line.startsWith('P|')),
      hasLength(154),
    );
    expect(
      actual.split('\n').where((line) => line.startsWith('S|')),
      hasLength(3),
    );
    expect(canvasDropSlotsForWidgetType(_type).map((slot) => slot.slotName), [
      'title',
      'subtitle',
      'secondary',
    ]);
    expect(_decode(_model()), isA<CanvasModel>());
  });

  testWidgets('standard and adaptive variants mount all three optional slots', (
    tester,
  ) async {
    for (final variant in ['standard', 'adaptive']) {
      await _pump(
        tester,
        _model(
          variant: variant,
          properties: {'isThreeLine': _bool(true)},
          slots: {
            'title': _single(
              _node(
                'a792df78-0d7e-4cf1-a375-000000000002',
                'flutter.widgets.Text',
                {'data': _string('Title')},
              ),
            ),
            'subtitle': _single(
              _node(
                'a792df78-0d7e-4cf1-a375-000000000003',
                'flutter.widgets.Text',
                {'data': _string('Subtitle')},
              ),
            ),
            'secondary': _single(
              _node(
                'a792df78-0d7e-4cf1-a375-000000000004',
                'flutter.widgets.Text',
                {'data': _string('Secondary')},
              ),
            ),
          },
        ),
      );
      expect(find.byType(CheckboxListTile), findsOneWidget);
      expect(find.byType(Checkbox), findsOneWidget);
      expect(find.text('Title'), findsOneWidget);
      expect(find.text('Subtitle'), findsOneWidget);
      expect(find.text('Secondary'), findsOneWidget);
      final tile = tester.widget<CheckboxListTile>(
        find.byType(CheckboxListTile),
      );
      expect(tile.value, false);
      expect(tile.onChanged, isNotNull);
      expect(tile.title, isNotNull);
      expect(tile.subtitle, isNotNull);
      expect(tile.secondary, isNotNull);
    }
  });

  testWidgets('adaptive uses CupertinoCheckbox only on Apple platforms', (
    tester,
  ) async {
    await _pump(tester, _model(variant: 'adaptive'), platform: 'ios');
    expect(find.byType(CheckboxListTile), findsOneWidget);
    expect(find.byType(CupertinoCheckbox), findsOneWidget);
    // Checkbox.adaptive remains the SDK wrapper; its child is CupertinoCheckbox.
    expect(find.byType(Checkbox), findsOneWidget);

    await _pump(tester, _model(variant: 'adaptive'), platform: 'windows');
    expect(find.byType(CheckboxListTile), findsOneWidget);
    expect(find.byType(Checkbox), findsOneWidget);
    expect(find.byType(CupertinoCheckbox), findsNothing);
  });

  testWidgets('callback presence and disabled state stay distinct', (
    tester,
  ) async {
    final selected = <String>[];
    await _pump(
      tester,
      _model(properties: {'onChanged': _reference}),
      selected: selected,
    );
    var tile = tester.widget<CheckboxListTile>(find.byType(CheckboxListTile));
    expect(tile.onChanged, isNotNull);
    tile.onChanged!(true);
    expect(selected, [_id]);
    expect(_messages(tester), contains('isolated Canvas never executes'));

    await _pump(
      tester,
      _model(properties: {'onChanged': _reference, 'enabled': _bool(false)}),
      selected: selected,
    );
    tile = tester.widget<CheckboxListTile>(find.byType(CheckboxListTile));
    expect(tile.onChanged, isNull);
    expect(_messages(tester), isEmpty);
  });

  test('strict CheckboxListTile relationships are enforced', () {
    expect(
      () => _decode(_model(properties: {'value': _null})),
      throwsFormatException,
    );
    expect(
      () => _decode(
        _model(properties: {'value': _null, 'tristate': _bool(true)}),
      ),
      returnsNormally,
    );
    expect(
      () => _decode(_model(properties: {'isThreeLine': _bool(true)})),
      throwsFormatException,
    );
    expect(
      () => _decode(
        _model(
          properties: {'isThreeLine': _bool(true)},
          slots: {
            'subtitle': _single(
              _node(
                'a792df78-0d7e-4cf1-a375-000000000005',
                'flutter.widgets.Text',
                {'data': _string('Subtitle')},
              ),
            ),
          },
        ),
      ),
      returnsNormally,
    );
  });

  test(
    'all CheckboxListTile optional drop slots accept one ordinary child',
    () {
      final slots = canvasDropSlotsForWidgetType(_type);
      for (final slot in slots) {
        final source = CanvasPaletteDragSource(
          token: 'flutter.widgets.Text',
          widgetType: 'flutter.widgets.Text',
          traits: canvasWidgetTraitsForType('flutter.widgets.Text'),
        );
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: _type,
            slotName: slot.slotName,
            currentChildCount: 0,
            insertionIndex: 0,
            source: source,
          ),
          isTrue,
        );
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: _type,
            slotName: slot.slotName,
            currentChildCount: 1,
            insertionIndex: 0,
            source: source,
          ),
          isFalse,
        );
      }
    },
  );
}
