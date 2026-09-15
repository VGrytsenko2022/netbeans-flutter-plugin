import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _id = '73d9ec43-3d37-4304-8998-71fe51804284';
const _presence = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
const _graphemes = 'a\u0301👨‍👩‍👧‍👦🇺🇦';
const _values = <String, Object?>{
  'omitted': null,
  'null': _null,
  'reference': _presence,
};

void main() {
  test(
    'TextField adds exactly two optional nullable builder leaves without widening other contracts or protocols',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      expect(contract, canvasReviewedWidgetSchemaContract.trimLeft());
      expect(canvasModelProtocolVersion, 20);
      expect(
        RegExp(r'^W\|', multiLine: true).allMatches(contract),
        hasLength(226),
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(contract),
        hasLength(7495),
      );
      final start = contract.indexOf('W|flutter.material.TextField\n');
      final section = contract.substring(
        start,
        contract.indexOf('\nW|', start) + 1,
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(section),
        hasLength(56),
      );
      for (final entry in {
        'buildCounter': 'InputCounterWidgetBuilder?',
        'contextMenuBuilder': 'EditableTextContextMenuBuilder?',
      }.entries) {
        expect(
          section,
          contains(
            'P|${entry.key}|dartObjectReference,null|0|-|-|'
            'dartObjectReference:dartObjectReference:v1:${entry.value}:currentOrPackage:'
            'root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true);null:any',
          ),
        );
      }
      for (final counter in _values.values) {
        for (final menu in _values.values) {
          final field = _decode(_model(counter: counter, menu: menu)).root;
          expect(field.properties.containsKey('buildCounter'), counter != null);
          expect(
            field.properties.containsKey('contextMenuBuilder'),
            menu != null,
          );
        }
      }
    },
  );

  test(
    'Builder payload rejects callback kinds, raw expressions and leaked project identity',
    () {
      for (final name in ['buildCounter', 'contextMenuBuilder']) {
        for (final value in [
          {'kind': 'callbackPresence'},
          {'kind': 'boolean', 'value': false},
          {'kind': 'string', 'value': 'projectBuilder'},
          {'kind': 'dartObjectReference', 'rootSymbol': 'privateBuilder'},
          {
            'kind': 'dartObjectReferencePresence',
            'rootSymbol': 'privateBuilder',
          },
          {
            'kind': 'dartObjectReferencePresence',
            'libraryUri': 'package:private/builders.dart',
          },
          {'kind': 'dartObjectReferencePresence', 'member': 'nullableGetter'},
          {
            'kind': 'dartObjectReferencePresence',
            'access': 'zeroArgumentInvocation',
          },
        ]) {
          expect(
            () => _decode(_model(extra: {name: value})),
            throwsFormatException,
            reason: '$name: $value',
          );
        }
      }
    },
  );

  testWidgets(
    'Canvas preserves text, selection, controller, geometry and the existing pointer/focus guard across omission null and reference changes',
    (tester) async {
      final selected = <String>[];
      await _pumpCanvas(tester, _model(maxLength: 10), selected: selected);
      final fieldState = tester.state(_field);
      final editableState = tester.state<EditableTextState>(_editable);
      final controller = editableState.widget.controller;
      final focus = editableState.widget.focusNode;
      controller.value = const TextEditingValue(
        text: _graphemes,
        selection: TextSelection(baseOffset: 0, extentOffset: 2),
      );
      await tester.pump();
      final bounds = tester.getRect(_field);
      expect(_decoration(tester).counterText, '3/10');
      for (final counter in _values.values) {
        for (final menu in _values.values) {
          await _pumpCanvas(
            tester,
            _model(counter: counter, menu: menu, maxLength: 10),
            selected: selected,
          );
          expect(tester.state(_field), same(fieldState));
          expect(
            tester.state<EditableTextState>(_editable),
            same(editableState),
          );
          expect(editableState.widget.controller, same(controller));
          expect(editableState.widget.focusNode, same(focus));
          expect(controller.text, _graphemes);
          expect(
            controller.selection,
            const TextSelection(baseOffset: 0, extentOffset: 2),
          );
          expect(tester.getRect(_field), bounds);
          expect(_decoration(tester).counterText, '3/10');
          expect(_sdk(tester).buildCounter, isNull);
          expect(
            _sdk(tester).contextMenuBuilder,
            menu == _null ? isNull : same(const TextField().contextMenuBuilder),
          );
          expect(focus.hasFocus, isFalse);
          expect(focus.canRequestFocus, isFalse);
          expect(
            find.ancestor(
              of: _field,
              matching: find.byWidgetPredicate(
                (w) => w is IgnorePointer && w.ignoring,
              ),
            ),
            findsWidgets,
          );
          expect(
            find.ancestor(
              of: _field,
              matching: find.byWidgetPredicate(
                (w) => w is ExcludeFocus && w.excluding,
              ),
            ),
            findsOneWidget,
          );
          expect(
            _warning,
            counter == _presence || menu == _presence
                ? findsOneWidget
                : findsNothing,
          );
          if (counter == _presence || menu == _presence) {
            final message = tester.widget<Tooltip>(_warning).message!;
            expect(
              message,
              contains('does not execute project or dependency Dart'),
            );
            expect(
              message,
              contains('explicit approximation, not the project result'),
            );
            expect(message, contains('may be null'));
            expect(message, contains('result is unknown'));
            if (counter == _presence) {
              expect(message, contains('return null or different content'));
            }
          }
          await tester.tap(_field);
          await tester.pump();
          expect(selected.last, _id);
          expect(focus.hasFocus, isFalse);
          expect(controller.text, _graphemes);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  for (final entry in _values.entries) {
    for (final limit in [null, -1, 10]) {
      testWidgets(
        'Captured SDK counter ${entry.key} with maxLength=$limit preserves focused grapheme-aware default behavior',
        (tester) async {
          await _pumpCanvas(
            tester,
            _model(counter: entry.value, maxLength: limit),
          );
          final field = _sdk(tester);
          expect(field.buildCounter, isNull);
          await _rehost(tester, field);
          final editable = tester.state<EditableTextState>(_editable);
          await tester.enterText(_field, _graphemes);
          await tester.pumpAndSettle();
          expect(editable.widget.focusNode.hasFocus, isTrue);
          expect(editable.widget.controller.text, _graphemes);
          expect(
            _decoration(tester).counterText,
            limit == null
                ? null
                : limit == -1
                ? '3'
                : '3/10',
          );
          expect(
            tester
                .widget<InputDecorator>(find.byType(InputDecorator))
                .isFocused,
            isTrue,
          );
          if (limit == 10) {
            expect(_decoration(tester).semanticCounterText, contains('7'));
          }
          editable.widget.focusNode.unfocus();
          await tester.pumpAndSettle();
          expect(
            tester
                .widget<InputDecorator>(find.byType(InputDecorator))
                .isFocused,
            isFalse,
          );
          expect(
            _decoration(tester).counterText,
            limit == null
                ? null
                : limit == -1
                ? '3'
                : '3/10',
          );
          expect(tester.takeException(), isNull);
        },
      );
    }
  }

  for (final platform in [TargetPlatform.android, TargetPlatform.windows]) {
    for (final entry in _values.entries) {
      testWidgets(
        'Captured SDK $platform menu ${entry.key} uses actual default overlay or exact explicit-null disablement',
        (tester) async {
          await _pumpCanvas(tester, _model(menu: entry.value));
          final field = _sdk(tester);
          await _rehost(tester, field, platform: platform);
          await tester.enterText(_field, 'Menu selection');
          final editable = tester.state<EditableTextState>(_editable);
          editable.selectAll(SelectionChangedCause.longPress);
          await tester.pumpAndSettle();
          editable.showToolbar();
          await tester.pumpAndSettle();
          expect(
            field.contextMenuBuilder,
            entry.value == _null
                ? isNull
                : same(const TextField().contextMenuBuilder),
          );
          expect(
            find.byType(AdaptiveTextSelectionToolbar),
            entry.value == _null ? findsNothing : findsOneWidget,
          );
          expect(editable.widget.focusNode.hasFocus, isTrue);
          expect(
            editable.widget.controller.selection,
            const TextSelection(baseOffset: 0, extentOffset: 14),
          );
          editable.hideToolbar();
          await tester.pumpAndSettle();
          expect(find.byType(AdaptiveTextSelectionToolbar), findsNothing);
          expect(tester.takeException(), isNull);
        },
      );
    }
  }

  testWidgets(
    'Rehosted menu and counter changes retain focused editable state and selection without executing unknown callbacks',
    (tester) async {
      final fields = <TextField>[];
      for (final value in [null, _presence, _null, null]) {
        await _pumpCanvas(
          tester,
          _model(counter: value, menu: value, maxLength: 10),
        );
        fields.add(_sdk(tester));
      }
      await _rehost(tester, fields.first);
      await tester.enterText(_field, _graphemes);
      final editable = tester.state<EditableTextState>(_editable);
      editable.widget.controller.selection = const TextSelection(
        baseOffset: 0,
        extentOffset: 2,
      );
      await tester.pump();
      final value = editable.widget.controller.value;
      final bounds = tester.getRect(_field);
      for (final field in fields.skip(1)) {
        await _rehost(tester, field);
        expect(tester.state<EditableTextState>(_editable), same(editable));
        expect(editable.widget.focusNode.hasFocus, isTrue);
        expect(editable.widget.controller.value, value);
        expect(tester.getRect(_field), bounds);
        expect(_decoration(tester).counterText, '3/10');
        expect(tester.takeException(), isNull);
      }
    },
  );
}

Finder get _field => find.byKey(const ValueKey('canvas-text-field-$_id'));
Finder get _editable =>
    find.descendant(of: _field, matching: find.byType(EditableText));
Finder get _warning => find.byWidgetPredicate(
  (w) =>
      w is Tooltip &&
      (w.message?.contains('TextField builder preview limitation') ?? false),
);
TextField _sdk(WidgetTester tester) => tester.widget<TextField>(_field);
InputDecoration _decoration(WidgetTester tester) => tester
    .widget<InputDecorator>(
      find.descendant(of: _field, matching: find.byType(InputDecorator)),
    )
    .decoration;
Future<void> _pumpCanvas(
  WidgetTester tester,
  Map<String, Object?> json, {
  List<String>? selected,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: CanvasDocumentView(
        model: _decode(json),
        selectedWidgetId: null,
        onSelected: selected?.add ?? (_) {},
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> _rehost(
  WidgetTester tester,
  TextField field, {
  TargetPlatform platform = TargetPlatform.android,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: ThemeData(platform: platform),
      themeAnimationDuration: Duration.zero,
      home: Scaffold(
        body: Center(child: SizedBox(width: 300, child: field)),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

CanvasModel _decode(Map<String, Object?> json) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json))));
Map<String, Object?> _model({
  Object? counter,
  Object? menu,
  int? maxLength,
  Map<String, Object?> extra = const {},
}) {
  final json =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  json['root'] = {
    'id': _id,
    'type': 'flutter.material.TextField',
    'properties': {
      'buildCounter': ?counter,
      'contextMenuBuilder': ?menu,
      if (maxLength != null)
        'maxLength': {'kind': 'integer', 'value': maxLength},
      ...extra,
    },
    'slots': <String, Object?>{},
  };
  return json;
}
