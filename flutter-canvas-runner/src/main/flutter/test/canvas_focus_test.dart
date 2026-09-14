// The legacy callback is intentionally reviewed while Flutter still exposes it.
// ignore_for_file: deprecated_member_use
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _id = '7bec8d5b-ed9a-479a-acb9-56ba0b31e42c';
const _childId = '8a6a2a8a-e0e8-4f2c-a57c-dc27306a55ec';
const _nestedId = '9a6a2a8a-e0e8-4f2c-a57c-dc27306a55ec';
const _type = 'flutter.widgets.Focus';
const _events = ['onFocusChange', 'onKeyEvent', 'onKey'];
const _nullableFlags = [
  'canRequestFocus',
  'skipTraversal',
  'descendantsAreFocusable',
  'descendantsAreTraversable',
];
const _presence = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};

void main() {
  test(
    'Focus exact13property3event contract and required-child wrap retain protocol19',
    () {
      expect(canvasModelProtocolVersion, 19);
      expect(
        canvasRuntimeWidgetSchemaContractForTesting(),
        canvasReviewedWidgetSchemaContract.trimLeft(),
      );
      final contract = canvasReviewedWidgetSchemaContract;
      final section = contract.substring(
        contract.indexOf('W|$_type\n'),
        contract.indexOf('W|flutter.widgets.FractionallySizedBox\n'),
      );
      expect('\nP|'.allMatches(section).length, 13);
      expect(section, contains('S|child|single|1|1|1|any'));
      expect(
        section,
        contains('C|$_type|paletteCreate|wrapExistingChild|child'),
      );
      expect(isCanvasPaletteWrapperWidgetType(_type), isTrue);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      _decode(_model());
    },
  );

  test(
    'All callbacks accept presence/null and reject application identity and code',
    () {
      for (final event in _events) {
        for (final value in [
          {'kind': 'callbackPresence'},
          _presence,
          _null,
        ]) {
          expect(
            () => _decode(_model(properties: {event: value})),
            returnsNormally,
          );
        }
        for (final value in [
          {'kind': 'callbackPresence', 'handler': 'projectCode'},
          {'kind': 'dartObjectReferencePresence', 'rootSymbol': 'projectCode'},
          {'kind': 'callback', 'handler': 'projectCode'},
          {'kind': 'string', 'value': 'projectCode()'},
          {'kind': 'enum', 'type': 'KeyEventResult', 'value': 'handled'},
        ]) {
          expect(
            () => _decode(_model(properties: {event: value})),
            throwsFormatException,
            reason: event,
          );
        }
      }
      for (final name in ['focusNode', 'parentNode']) {
        for (final value in [_presence, _null]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            returnsNormally,
          );
        }
        expect(
          () => _decode(
            _model(
              properties: {
                name: {
                  'kind': 'dartObjectReferencePresence',
                  'libraryUri': 'package:app/private.dart',
                },
              },
            ),
          ),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Exact nullable flags and constructor admission preserve seven inactive fields',
    () {
      for (final name in _nullableFlags) {
        for (final value in [_bool(false), _bool(true), _null]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            returnsNormally,
          );
        }
      }
      for (final name in ['autofocus', 'includeSemantics']) {
        expect(
          () => _decode(_model(properties: {name: _null})),
          throwsFormatException,
        );
      }
      for (final variant in ['standard', 'withExternalFocusNode']) {
        final values = <String, Object?>{
          'variant': _string(variant),
          'focusNode': _presence,
          'debugLabel': _string('retained'),
          'onKey': {'kind': 'callbackPresence'},
          'onKeyEvent': _presence,
          for (final name in _nullableFlags) name: _bool(false),
        };
        expect(() => _decode(_model(properties: values)), returnsNormally);
      }
      for (final value in [null, _null]) {
        expect(
          () => _decode(
            _model(
              properties: {
                'variant': _string('withExternalFocusNode'),
                'focusNode': ?value,
              },
            ),
          ),
          throwsFormatException,
        );
      }
      expect(
        () => _decode(_model(properties: {'variant': _string('custom')})),
        throwsFormatException,
      );
      final model = _model();
      _findJson(model['root']! as Map<String, Object?>, _id)!['slots'] = {
        'child': {'kind': 'single', 'child': null},
      };
      expect(() => _decode(model), throwsFormatException);
    },
  );

  testWidgets(
    'Standard Focus keeps SDK defaults, nullable-update semantics, and internally owned node',
    (tester) async {
      await _pump(tester, _model());
      final original = _localFocus(tester);
      expect(_focus(tester).focusNode, isNull);
      expect(_focus(tester).parentNode, isNull);
      expect(_focus(tester).autofocus, isFalse);
      expect(_focus(tester).includeSemantics, isTrue);
      expect(original.canRequestFocus, isTrue);
      expect(original.skipTraversal, isFalse);
      for (final flags in [
        {
          'canRequestFocus': _bool(false),
          'skipTraversal': _bool(true),
          'descendantsAreFocusable': _bool(false),
          'descendantsAreTraversable': _bool(false),
          'debugLabel': _string('Local debug'),
        },
        {for (final name in _nullableFlags) name: _null, 'debugLabel': _null},
      ]) {
        await _pump(tester, _model(properties: flags));
        expect(_localFocus(tester), same(original));
        final reset = flags['debugLabel'] == _null;
        // SDK Focus.didUpdateWidget intentionally does not overwrite a node's
        // previous canRequestFocus when the new constructor argument is null.
        expect(original.canRequestFocus, isFalse);
        expect(original.skipTraversal, !reset);
        expect(original.descendantsAreFocusable, reset);
        expect(original.descendantsAreTraversable, reset);
      }
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Every callback form is inert and returns ignored for both keyboard APIs',
    (tester) async {
      final key = KeyDownEvent(
        physicalKey: PhysicalKeyboardKey.keyA,
        logicalKey: LogicalKeyboardKey.keyA,
        timeStamp: Duration.zero,
      );
      final raw = RawKeyDownEvent(
        data: const RawKeyEventDataWindows(keyCode: 65, scanCode: 30),
      );
      for (final form in [
        {'kind': 'callbackPresence'},
        _presence,
        _null,
      ]) {
        await _pump(
          tester,
          _model(properties: {for (final event in _events) event: form}),
        );
        final focus = _focus(tester);
        final node = _localFocus(tester);
        if (form == _null) {
          expect(focus.onFocusChange, isNull);
          expect(focus.onKeyEvent, isNull);
          expect(focus.onKey, isNull);
        } else {
          focus.onFocusChange!(true);
          focus.onFocusChange!(false);
          expect(focus.onKeyEvent!(node, key), KeyEventResult.ignored);
          expect(focus.onKey!(node, raw), KeyEventResult.ignored);
        }
        node.requestFocus();
        await tester.pump();
        await tester.sendKeyEvent(LogicalKeyboardKey.keyA);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'Focus acquisition and loss use actual SDK focus chain and inert child keys propagate',
    (tester) async {
      final events = <LogicalKeyboardKey>[];
      await _pump(
        tester,
        _model(
          properties: {
            'onKeyEvent': {'kind': 'callbackPresence'},
          },
          child: _node(
            _nestedId,
            _type,
            properties: {'onKeyEvent': _presence},
            child: _text(),
          ),
        ),
      );
      final outer = Focus.of(
        tester.element(find.byKey(const ValueKey('canvas-focus-$_nestedId'))),
      );
      final child = _localFocus(tester);
      outer.onKeyEvent = (_, event) {
        events.add(event.logicalKey);
        return KeyEventResult.handled;
      };
      child.requestFocus();
      await tester.pump();
      expect(child.hasPrimaryFocus, isTrue);
      expect(outer.hasFocus, isTrue);
      await tester.sendKeyEvent(LogicalKeyboardKey.keyA);
      expect(events, [LogicalKeyboardKey.keyA, LogicalKeyboardKey.keyA]);
      outer.requestFocus();
      await tester.pump();
      expect(child.hasFocus, isFalse);
      expect(outer.hasPrimaryFocus, isTrue);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Autofocus and ignored application callbacks do not steal Designer Delete commands or selection',
    (tester) async {
      var deletes = 0;
      final selected = <String>[];
      await tester.pumpWidget(
        CanvasModelApp(
          model: _decode(
            _model(
              properties: {
                'autofocus': _bool(true),
                'onKeyEvent': _presence,
                'onKey': {'kind': 'callbackPresence'},
              },
            ),
          ),
          selectedWidgetId: _childId,
          onSelected: selected.add,
          onDeleteSelected: () {
            deletes++;
            return true;
          },
        ),
      );
      await tester.pump();
      final node = _localFocus(tester);
      expect(node.hasFocus, isTrue);
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deletes, 1);
      await tester.tap(find.text('Focus child'));
      await tester.pump();
      expect(selected.last, _childId);
      expect(
        node.hasFocus,
        isFalse,
        reason: 'Designer selection owns editing focus, unchanged',
      );
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deletes, 2);
    },
  );

  testWidgets(
    'External node is stable, locally owned, explicit approximation and ignores inactive fields',
    (tester) async {
      final properties = {
        'variant': _string('withExternalFocusNode'),
        'focusNode': _presence,
        'parentNode': _presence,
        'onFocusChange': _presence,
        'onKeyEvent': _presence,
        'onKey': _presence,
        'debugLabel': _string('application label'),
        for (final name in _nullableFlags) name: _bool(false),
      };
      await _pump(tester, _model(properties: properties));
      final first = _focus(tester).focusNode!;
      expect(_localFocus(tester), same(first));
      expect(first.canRequestFocus, isTrue);
      expect(first.skipTraversal, isFalse);
      expect(first.descendantsAreFocusable, isTrue);
      expect(first.descendantsAreTraversable, isTrue);
      expect(first.debugLabel, isNot('application label'));
      expect(first.onKeyEvent, isNull);
      expect(first.onKey, isNull);
      expect(_focus(tester).onFocusChange, isNotNull);
      expect(_focus(tester).parentNode, isNull);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              (w.message?.contains('nearest preview focus ancestor') ??
                  false) &&
              (w.message?.contains('external constructor ignores') ?? false),
        ),
        findsOneWidget,
      );
      first.requestFocus();
      await tester.pump();
      expect(first.hasFocus, isTrue);
      await _pump(tester, _model(properties: properties));
      expect(_focus(tester).focusNode, same(first));
      await tester.pumpWidget(const SizedBox());
      await tester.pump();
      expect(() => first.addListener(() {}), throwsFlutterError);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Variant switches clear formerly active flags/callbacks and retain child element',
    (tester) async {
      final base = <String, Object?>{
        'focusNode': _presence,
        'onKeyEvent': _presence,
        'onKey': _presence,
        'canRequestFocus': _bool(false),
        'skipTraversal': _bool(true),
        'descendantsAreFocusable': _bool(false),
        'descendantsAreTraversable': _bool(false),
        'debugLabel': _string('standard only'),
      };
      await _pump(tester, _model(properties: base));
      final child = tester.element(find.text('Focus child'));
      final node = _focus(tester).focusNode!;
      expect(node.canRequestFocus, isFalse);
      for (final variant in [
        'withExternalFocusNode',
        'standard',
        'withExternalFocusNode',
      ]) {
        await _pump(
          tester,
          _model(properties: {...base, 'variant': _string(variant)}),
        );
        expect(tester.element(find.text('Focus child')), same(child));
        expect(_focus(tester).focusNode, same(node));
        final external = variant == 'withExternalFocusNode';
        expect(node.canRequestFocus, external);
        expect(node.skipTraversal, !external);
        expect(node.descendantsAreFocusable, external);
        expect(node.descendantsAreTraversable, external);
        expect(node.onKeyEvent == null, external);
        expect(node.onKey == null, external);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'Presence and diagnostic changes preserve standard child state without leaking project objects',
    (tester) async {
      await _pump(tester, _model());
      final child = tester.element(find.text('Focus child'));
      for (final properties in [
        <String, Object?>{'focusNode': _presence},
        {'parentNode': _presence},
        {'focusNode': _null, 'parentNode': _null},
        <String, Object?>{},
      ]) {
        await _pump(tester, _model(properties: properties));
        expect(tester.element(find.text('Focus child')), same(child));
        expect(_focus(tester).parentNode, isNull);
        expect(tester.takeException(), isNull);
      }
    },
  );

  for (final platform in ['windows', 'android']) {
    testWidgets(
      'Focus descendantsAreFocusable and traversal flags use SDK hierarchy on $platform',
      (tester) async {
        for (final allow in [true, false, true]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {
                'descendantsAreFocusable': _bool(allow),
                'descendantsAreTraversable': _bool(allow),
              },
              child: _node(_nestedId, _type, child: _text()),
            ),
          );
          final child = _localFocus(tester);
          child.requestFocus();
          await tester.pump();
          expect(child.canRequestFocus, allow);
          expect(child.hasFocus, allow);
          expect(child.skipTraversal, !allow);
          expect(tester.takeException(), isNull);
        }
      },
    );
    testWidgets(
      'Focus required zero-size child retains native layout and external Designer handle on $platform',
      (tester) async {
        final selected = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                size: Size.zero,
                child: _node(
                  _childId,
                  'flutter.widgets.SizedBox',
                  properties: {
                    'width': {'kind': 'double', 'value': 0},
                    'height': {'kind': 'double', 'value': 0},
                  },
                ),
              ),
            ),
            selectedWidgetId: null,
            onSelected: selected.add,
          ),
        );
        await tester.pump();
        expect(
          tester.getSize(find.byKey(const ValueKey('canvas-focus-$_id'))),
          Size.zero,
        );
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-group-$_id'),
        );
        expect(handle, findsOneWidget);
        expect(
          find.ancestor(
            of: handle,
            matching: find.byKey(const ValueKey('canvas-focus-$_id')),
          ),
          findsNothing,
        );
        await tester.tap(handle);
        expect(selected.last, _id);
        expect(tester.takeException(), isNull);
      },
    );
  }
}

Focus _focus(WidgetTester tester) =>
    tester.widget<Focus>(find.byKey(const ValueKey('canvas-focus-$_id')));
FocusNode _localFocus(WidgetTester tester) =>
    Focus.of(tester.element(find.text('Focus child')));
Future<void> _pump(WidgetTester tester, Map<String, Object?> model) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: _decode(model),
      selectedWidgetId: null,
      onSelected: (_) {},
    ),
  );
  await tester.pump();
}

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
  Size size = const Size(120, 100),
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  model['root'] = {
    'id': '0d279f63-dfd5-4c5f-8e60-8d5c11aab787',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          '1d6c16df-df2b-41cf-b46d-2e1d85538277',
          'flutter.widgets.Center',
          child: _node(
            'e03e4228-2d19-46ba-92d6-e4acdb40796e',
            'flutter.widgets.SizedBox',
            properties: {
              'width': {'kind': 'double', 'value': size.width},
              'height': {'kind': 'double', 'value': size.height},
            },
            child: _node(
              _id,
              _type,
              properties: properties,
              child: child ?? _text(),
            ),
          ),
        ),
      },
    },
  };
  return model;
}

Map<String, Object?> _node(
  String id,
  String type, {
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
}) => {
  'id': id,
  'type': type,
  'properties': properties,
  'slots': {
    'child': {'kind': 'single', 'child': child},
  },
};
Map<String, Object?> _text() => {
  'id': _childId,
  'type': 'flutter.widgets.Text',
  'properties': {'data': _string('Focus child')},
  'slots': <String, Object?>{},
};
Map<String, Object?>? _findJson(Map<String, Object?> node, String id) {
  if (node['id'] == id) return node;
  for (final slot in (node['slots']! as Map<String, Object?>).values) {
    final child = (slot! as Map<String, Object?>)['child'];
    if (child is Map<String, Object?>) {
      final found = _findJson(child, id);
      if (found != null) return found;
    }
  }
  return null;
}
