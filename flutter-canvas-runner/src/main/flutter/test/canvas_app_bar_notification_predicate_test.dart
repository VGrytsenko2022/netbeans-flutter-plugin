import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _appBarId = 'e223fd18-36c3-469b-ae9c-d7e69dd2fd83';
const _scaffoldId = '6e88bff4-8d73-48aa-92b5-87aa3344f6a7';
const _titleId = '146cc972-81e9-4797-a474-ee97d10f3b34';
const _outerId = '4c04dc44-7381-4ca8-826f-8c23bc2351d1';
const _innerId = '1d6c16df-df2b-41cf-b46d-2e1d85538277';
const _property = 'notificationPredicate';
const _presence = {'kind': 'dartObjectReferencePresence'};
const _values = <String, Object?>{
  'omitted': null,
  'default': {'kind': 'string', 'value': 'default'},
  'depthZero': {'kind': 'string', 'value': 'depthZero'},
  'all': {'kind': 'string', 'value': 'all'},
  'project presence': _presence,
};

void main() {
  test(
    'AppBar adds only the reviewed nonnull predicate union without protocol or inventory changes',
    () {
      expect(canvasModelProtocolVersion, 20);
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      expect(contract, canvasReviewedWidgetSchemaContract.trimLeft());
      expect(
        RegExp(r'^W\|', multiLine: true).allMatches(contract),
        hasLength(236),
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(contract),
        hasLength(7790),
      );
      final start = contract.indexOf('W|flutter.material.AppBar\n');
      final section = contract.substring(
        start,
        contract.indexOf('\nW|', start) + 1,
      );
      expect(
        section,
        contains(
          'P|$_property|dartObjectReference,string|0|-|-|'
          'dartObjectReference:dartObjectReference:v1:ScrollNotificationPredicate:'
          'currentOrPackage:root,optionalMember:reference,zeroArgumentInvocation:'
          'requiredConstnessBoolean(false,true);string:pattern:KD86ZGVmYXVsdHxkZXB0aFplcm98YWxsKQ',
        ),
      );
      for (final value in _values.values) {
        final appBar = _decode(
          _model(predicate: value),
        ).root.slots['appBar']!.children.single;
        expect(appBar.properties.containsKey(_property), value != null);
        if (value == _presence) {
          expect(
            appBar.properties[_property]!.kind,
            'dartObjectReferencePresence',
          );
        }
      }
    },
  );

  test(
    'AppBar rejects null, unreviewed kinds, presets and executable identity in the payload',
    () {
      for (final value in [
        {'kind': 'null'},
        {'kind': 'callbackPresence'},
        {'kind': 'boolean', 'value': true},
        {'kind': 'string', 'value': 'projectPredicate'},
        {'kind': 'dartObjectReference', 'rootSymbol': 'privatePredicate'},
        {
          'kind': 'dartObjectReferencePresence',
          'rootSymbol': 'privatePredicate',
        },
        {
          'kind': 'dartObjectReferencePresence',
          'libraryUri': 'package:private/predicate.dart',
        },
        {'kind': 'dartObjectReferencePresence', 'member': 'privateGetter'},
        {
          'kind': 'dartObjectReferencePresence',
          'access': 'zeroArgumentInvocation',
        },
        {
          'kind': 'dartObjectReferencePresence',
          'expectedType': 'ScrollNotificationPredicate',
        },
      ]) {
        expect(
          () => _decode(_model(predicate: value)),
          throwsFormatException,
          reason: '$value',
        );
      }
    },
  );

  for (final entry in _values.entries) {
    testWidgets(
      'Real AppBar ${entry.key} predicate accepts the exact depth set with explicit custom approximation',
      (tester) async {
        await _pump(tester, _model(predicate: entry.value));
        final appBar = tester.widget<AppBar>(_appBarFinder);
        if (entry.key == 'omitted' ||
            entry.key == 'default' ||
            entry.value == _presence) {
          expect(
            appBar.notificationPredicate,
            same(defaultScrollNotificationPredicate),
          );
        }
        for (final depth in [0, 1, 2, 99]) {
          expect(
            appBar.notificationPredicate(
              _DepthScrollUpdate(depth, tester.element(_appBarFinder)),
            ),
            depth == 0 || entry.key == 'all',
            reason: 'depth=$depth',
          );
        }
        expect(
          _limitationFinder,
          entry.value == _presence ? findsOneWidget : findsNothing,
        );
        if (entry.value == _presence) {
          final message = tester.widget<Tooltip>(_limitationFinder).message!;
          expect(message, contains('actual SDK depth-zero predicate'));
          expect(
            message,
            contains('explicit approximation for scrolled-under elevation'),
          );
          expect(message, contains('does not consume or stop notifications'));
          expect(
            message,
            contains('does not execute project or dependency Dart'),
          );
        }
        expect(appBar.preferredSize.height, 64);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Real nested viewport updates change AppBar elevation without consuming notifications: ${entry.key}',
      (tester) async {
        final observed = <ScrollNotification>[];
        await _pump(
          tester,
          _model(predicate: entry.value),
          onScroll: observed.add,
        );
        final outer = _scrollable(tester, _outerId);
        final inner = _scrollable(tester, _innerId);
        expect(_elevation(tester), 1);
        observed.clear();
        inner.position.jumpTo(60);
        await tester.pumpAndSettle();
        expect(
          observed.whereType<ScrollUpdateNotification>().map((n) => n.depth),
          contains(1),
        );
        expect(_elevation(tester), entry.key == 'all' ? 9 : 1);
        observed.clear();
        outer.position.jumpTo(120);
        await tester.pumpAndSettle();
        expect(
          observed.whereType<ScrollUpdateNotification>().map((n) => n.depth),
          contains(0),
        );
        expect(_elevation(tester), 9);
        observed.clear();
        outer.position.jumpTo(0);
        await tester.pumpAndSettle();
        expect(_elevation(tester), 1);
        expect(
          observed.whereType<ScrollUpdateNotification>().map((n) => n.depth),
          contains(0),
        );
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'Presence, preset change and reset preserve AppBar state, preferred size, geometry, scroll position and title selection',
    (tester) async {
      final selected = <String>[];
      await _pump(
        tester,
        _model(predicate: _values['all']),
        onSelected: selected.add,
      );
      final state = tester.state(_appBarFinder);
      final scaffold = tester.state<ScaffoldState>(_scaffoldFinder);
      final title = tester.element(find.text('Scroll title'));
      final appBarBounds = tester.getRect(_appBarFinder);
      final scaffoldBounds = tester.getRect(_scaffoldFinder);
      final outer = _scrollable(tester, _outerId);
      final inner = _scrollable(tester, _innerId);
      inner.position.jumpTo(60);
      await tester.pumpAndSettle();
      expect(_elevation(tester), 9);
      for (final value in [
        _presence,
        null,
        _values['depthZero'],
        _presence,
        _values['all'],
        null,
      ]) {
        await _pump(tester, _model(predicate: value), onSelected: selected.add);
        expect(tester.state(_appBarFinder), same(state));
        expect(tester.state<ScaffoldState>(_scaffoldFinder), same(scaffold));
        expect(tester.element(find.text('Scroll title')), same(title));
        expect(tester.getRect(_appBarFinder), appBarBounds);
        expect(tester.getRect(_scaffoldFinder), scaffoldBounds);
        expect(
          tester.widget<Scaffold>(_scaffoldFinder).appBar!.preferredSize.height,
          64,
        );
        expect(_scrollable(tester, _outerId), same(outer));
        expect(_scrollable(tester, _innerId), same(inner));
        expect(inner.position.pixels, 60);
        expect(outer.position.pixels, 0);
        // Replacing a predicate does not synthesize a scroll update or reset SDK state.
        expect(_elevation(tester), 9);
        await tester.tap(find.text('Scroll title'));
        expect(selected.last, _titleId);
        expect(tester.takeException(), isNull);
      }
      outer.position.jumpTo(20);
      await tester.pumpAndSettle();
      outer.position.jumpTo(0);
      await tester.pumpAndSettle();
      expect(_elevation(tester), 1);
    },
  );

  testWidgets(
    'Omitted scrolled-under elevation retains the SDK theme/default resolution for custom presence',
    (tester) async {
      await _pump(tester, _model(explicitElevations: false));
      final base = _elevation(tester);
      _scrollable(tester, _outerId).position.jumpTo(30);
      await tester.pumpAndSettle();
      final raised = _elevation(tester);
      expect(raised, greaterThan(base));
      expect(
        tester.widget<AppBar>(_appBarFinder).scrolledUnderElevation,
        isNull,
      );
      await _pump(
        tester,
        _model(predicate: _presence, explicitElevations: false),
      );
      expect(
        tester.widget<AppBar>(_appBarFinder).scrolledUnderElevation,
        isNull,
      );
      expect(_elevation(tester), raised);
      _scrollable(tester, _outerId).position.jumpTo(0);
      await tester.pumpAndSettle();
      expect(_elevation(tester), base);
      expect(tester.takeException(), isNull);
    },
  );
}

class _DepthScrollUpdate extends ScrollUpdateNotification {
  _DepthScrollUpdate(this.depth, BuildContext context)
    : super(
        metrics: FixedScrollMetrics(
          minScrollExtent: 0,
          maxScrollExtent: 500,
          pixels: 30,
          viewportDimension: 100,
          axisDirection: AxisDirection.down,
          devicePixelRatio: 1,
        ),
        context: context,
      );
  @override
  final int depth;
}

Finder get _appBarFinder =>
    find.byKey(const ValueKey('canvas-app-bar-$_appBarId'));
Finder get _scaffoldFinder =>
    find.byKey(const ValueKey('canvas-scaffold-$_scaffoldId'));
Finder get _limitationFinder => find.byWidgetPredicate(
  (w) =>
      w is Tooltip &&
      (w.message?.contains(
            'Custom AppBar scroll predicate preview unavailable',
          ) ??
          false),
);
double _elevation(WidgetTester tester) => tester
    .widget<Material>(
      find.descendant(of: _appBarFinder, matching: find.byType(Material)).first,
    )
    .elevation;
ScrollableState _scrollable(WidgetTester tester, String id) =>
    tester.state<ScrollableState>(
      find
          .descendant(
            of: find.byKey(ValueKey('canvas-widget-$id')),
            matching: find.byType(Scrollable),
          )
          .first,
    );
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> json, {
  ValueChanged<String>? onSelected,
  ValueChanged<ScrollNotification>? onScroll,
}) async {
  await tester.pumpWidget(
    NotificationListener<ScrollNotification>(
      onNotification: (notification) {
        onScroll?.call(notification);
        return false;
      },
      child: CanvasModelApp(
        model: _decode(json),
        selectedWidgetId: null,
        onSelected: onSelected ?? (_) {},
      ),
    ),
  );
  await tester.pumpAndSettle();
}

CanvasModel _decode(Map<String, Object?> json) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json))));
Map<String, Object?> _model({
  Object? predicate,
  bool explicitElevations = true,
}) {
  final json =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  json['root'] = _node(
    _scaffoldId,
    'flutter.material.Scaffold',
    slots: {
      'appBar': _single(
        _node(
          _appBarId,
          'flutter.material.AppBar',
          properties: {
            _property: ?predicate,
            'toolbarHeight': {'kind': 'integer', 'value': 64},
            if (explicitElevations) ...{
              'elevation': {'kind': 'integer', 'value': 1},
              'scrolledUnderElevation': {'kind': 'integer', 'value': 9},
            },
          },
          slots: {'title': _single(_text(_titleId, 'Scroll title'))},
        ),
      ),
      'body': _single(
        _node(
          _outerId,
          'flutter.widgets.ListView',
          properties: {
            'itemExtent': {'kind': 'integer', 'value': 300},
            'primary': {'kind': 'boolean', 'value': false},
          },
          slots: {
            'children': {
              'kind': 'list',
              'children': [
                _node(
                  _innerId,
                  'flutter.widgets.ListView',
                  properties: {
                    'itemExtent': {'kind': 'integer', 'value': 50},
                    'primary': {'kind': 'boolean', 'value': false},
                  },
                  slots: {
                    'children': {
                      'kind': 'list',
                      'children': [
                        for (var i = 0; i < 12; i++)
                          _text(
                            'b3403b57-2a86-4aed-bec6-${(i + 100000000000).toString()}',
                            'Inner $i',
                          ),
                      ],
                    },
                  },
                ),
                for (var i = 0; i < 8; i++)
                  _text(
                    'b3403b57-2a86-4aed-bec6-${(i + 200000000000).toString()}',
                    'Outer $i',
                  ),
              ],
            },
          },
        ),
      ),
    },
  );
  return json;
}

Map<String, Object?> _node(
  String id,
  String type, {
  Map<String, Object?> properties = const {},
  Map<String, Object?> slots = const {},
}) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};
Map<String, Object?> _single(Map<String, Object?> child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _text(String id, String data) => _node(
  id,
  'flutter.widgets.Text',
  properties: {
    'data': {'kind': 'string', 'value': data},
  },
);
