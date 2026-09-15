import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _id = '5e2d8e71-b9ea-494f-b65e-e8f704928344';
const _presence = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};

void main() {
  test(
    'ListView adds exactly one nullable typed extent-builder leaf and keeps children and protocol closed',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      expect(contract, canvasReviewedWidgetSchemaContract.trimLeft());
      expect(canvasModelProtocolVersion, 20);
      expect(
        RegExp(r'^W\|', multiLine: true).allMatches(contract),
        hasLength(218),
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(contract),
        hasLength(7441),
      );
      final start = contract.indexOf('W|flutter.widgets.ListView\n');
      final section = contract.substring(
        start,
        contract.indexOf('\nW|', start) + 1,
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(section),
        hasLength(18),
      );
      expect(
        section,
        contains(
          'P|itemExtentBuilder|dartObjectReference,null|0|-|-|'
          'dartObjectReference:dartObjectReference:v1:ItemExtentBuilder?:currentOrPackage:'
          'root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true);null:any',
        ),
      );
      expect(section, contains('S|children|list|0|0|10000|any\n'));
    },
  );

  test(
    'Payload rejects executable identities, unreviewed callback kinds and every fixed/reference conflict',
    () {
      for (final value in [
        {'kind': 'callbackPresence'},
        {'kind': 'string', 'value': 'extents'},
        {'kind': 'boolean', 'value': true},
        {'kind': 'dartObjectReference', 'rootSymbol': 'privateExtents'},
        {'kind': 'dartObjectReferencePresence', 'rootSymbol': 'privateExtents'},
        {'kind': 'dartObjectReferencePresence', 'member': 'nullableGetter'},
        {
          'kind': 'dartObjectReferencePresence',
          'libraryUri': 'package:private/extents.dart',
        },
        {
          'kind': 'dartObjectReferencePresence',
          'access': 'zeroArgumentInvocation',
        },
      ]) {
        expect(
          () => _decode(_model(builder: value)),
          throwsFormatException,
          reason: '$value',
        );
      }
      for (final extent in [0, 40, 100000]) {
        expect(
          () => _decode(_model(builder: _presence, fixed: extent)),
          throwsFormatException,
        );
        expect(
          () => _decode(_model(builder: _null, fixed: extent)),
          returnsNormally,
        );
        expect(() => _decode(_model(fixed: extent)), returnsNormally);
      }
    },
  );

  for (final fixed in [false, true]) {
    for (final explicitNull in [false, true]) {
      testWidgets(
        'Omitted/null builder retains SDK ${fixed ? 'fixed' : 'natural'} sizing, explicitNull=$explicitNull',
        (tester) async {
          await _pump(
            tester,
            _model(
              builder: explicitNull ? _null : null,
              fixed: fixed ? 48 : null,
            ),
          );
          final sdk = _sdk(tester);
          expect(sdk.itemExtentBuilder, isNull);
          expect(sdk.itemExtent, fixed ? 48 : null);
          expect(_warning, findsNothing);
          expect(
            find.descendant(
              of: _list,
              matching: find.byType(SliverFixedExtentList),
            ),
            fixed ? findsOneWidget : findsNothing,
          );
          expect(
            find.descendant(of: _list, matching: find.byType(SliverList)),
            fixed ? findsNothing : findsOneWidget,
          );
          expect(tester.takeException(), isNull);
        },
      );
    }
  }

  for (final axis in Axis.values) {
    for (final reverse in [false, true]) {
      for (final shrink in [false, true]) {
        testWidgets(
          'Natural approximation preserves geometry, children, scrolling and selection: $axis reverse=$reverse shrink=$shrink',
          (tester) async {
            final selected = <String>[];
            Map<String, Object?> model(Object? builder) => _model(
              builder: builder,
              axis: axis,
              reverse: reverse,
              shrink: shrink,
            );
            await _pump(tester, model(null), selected: selected);
            final element = tester.element(_list);
            final scrollable = _scrollable(tester);
            scrollable.position.jumpTo(60);
            await tester.pumpAndSettle();
            final bounds = tester.getRect(_list);
            final item = tester.element(find.text('Item3'));
            final childBounds = tester.getRect(
              find.byKey(ValueKey('canvas-widget-${_childId(3)}')),
            );
            final maxScrollExtent = scrollable.position.maxScrollExtent;
            for (final builder in [_presence, _null, null, _presence, null]) {
              await _pump(tester, model(builder), selected: selected);
              expect(tester.element(_list), same(element));
              expect(_scrollable(tester), same(scrollable));
              expect(scrollable.position.pixels, 60);
              expect(scrollable.position.maxScrollExtent, maxScrollExtent);
              expect(tester.getRect(_list), bounds);
              expect(tester.element(find.text('Item3')), same(item));
              expect(
                tester.getRect(
                  find.byKey(ValueKey('canvas-widget-${_childId(3)}')),
                ),
                childBounds,
              );
              expect(_sdk(tester).itemExtent, isNull);
              expect(_sdk(tester).itemExtentBuilder, isNull);
              expect(
                (_sdk(tester).childrenDelegate as SliverChildListDelegate)
                    .children,
                hasLength(20),
              );
              expect(
                _warning,
                builder == _presence ? findsOneWidget : findsNothing,
              );
              if (builder == _presence) {
                final message = tester.widget<Tooltip>(_warning).message!;
                expect(
                  message,
                  contains('does not execute project or dependency Dart'),
                );
                expect(message, contains('callback may be null'));
                expect(
                  message,
                  contains(
                    'per-index sizes and null out-of-range result are unknown',
                  ),
                );
                expect(
                  message,
                  contains('explicit approximation, not the project result'),
                );
                expect(
                  message,
                  contains('No constant extent callback is fabricated'),
                );
              }
              await tester.tap(find.text('Item3'));
              expect(selected.last, _textId(3));
              expect(tester.takeException(), isNull);
            }
          },
        );

        testWidgets(
          'Real SDK variable extents use current dimensions and safe out-of-range results: $axis reverse=$reverse shrink=$shrink',
          (tester) async {
            final controller = ScrollController();
            addTearDown(controller.dispose);
            final observed = <SliverLayoutDimensions>[];
            double? builder(int index, SliverLayoutDimensions dimensions) {
              observed.add(dimensions);
              return index >= 12 ? null : 50 + (index % 3) * 10.0;
            }

            await _runtime(
              tester,
              axis: axis,
              reverse: reverse,
              shrink: shrink,
              controller: controller,
              builder: builder,
            );
            expect(find.byType(SliverVariedExtentList), findsOneWidget);
            expect(observed, isNotEmpty);
            expect(observed.last.scrollOffset, 0);
            expect(observed.last.precedingScrollExtent, 0);
            expect(
              observed.last.viewportMainAxisExtent,
              axis == Axis.vertical ? 220 : 320,
            );
            expect(
              observed.last.crossAxisExtent,
              axis == Axis.vertical ? 320 : 220,
            );
            for (final index in [0, 1, 2]) {
              final size = tester.getSize(
                find.byKey(ValueKey('runtime-item-$index')),
              );
              expect(
                axis == Axis.vertical ? size.height : size.width,
                50 + (index % 3) * 10.0,
              );
            }
            controller.jumpTo(100);
            await tester.pumpAndSettle();
            expect(observed.last.scrollOffset, 100);
            await _runtime(
              tester,
              axis: axis,
              reverse: reverse,
              shrink: shrink,
              controller: controller,
              builder: builder,
              width: 360,
              height: 260,
            );
            expect(controller.offset, 100);
            expect(
              observed.last.viewportMainAxisExtent,
              axis == Axis.vertical ? 260 : 360,
            );
            expect(
              observed.last.crossAxisExtent,
              axis == Axis.vertical ? 360 : 260,
            );
            expect(builder(12, observed.last), isNull);
            expect(builder(100, observed.last), isNull);
            controller.jumpTo(controller.position.maxScrollExtent);
            await tester.pumpAndSettle();
            expect(
              find.byKey(const ValueKey('runtime-item-11')),
              findsOneWidget,
            );
            expect(find.byKey(const ValueKey('runtime-item-12')), findsNothing);
            expect(
              controller.position.maxScrollExtent,
              720 - (axis == Axis.vertical ? 260 : 360),
            );
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }

  testWidgets(
    'Empty referenced list stays empty without a fake extent callback or phantom children',
    (tester) async {
      await _pump(tester, _model(builder: _presence, count: 0));
      expect(_sdk(tester).itemExtentBuilder, isNull);
      expect(
        (_sdk(tester).childrenDelegate as SliverChildListDelegate).children,
        isEmpty,
      );
      expect(_scrollable(tester).position.maxScrollExtent, 0);
      expect(find.textContaining('Item'), findsNothing);
      expect(_warning, findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}

Finder get _list => find.byKey(const ValueKey('canvas-list-view-$_id'));
Finder get _warning => find.byWidgetPredicate(
  (w) =>
      w is Tooltip &&
      (w.message?.contains('ListView item-extent preview limitation') ?? false),
);
ListView _sdk(WidgetTester tester) => tester.widget<ListView>(_list);
ScrollableState _scrollable(WidgetTester tester) =>
    tester.state<ScrollableState>(
      find.descendant(of: _list, matching: find.byType(Scrollable)),
    );
String _childId(int index) => 'b3403b57-2a86-4aed-bec6-${100000000000 + index}';
String _textId(int index) => 'b3403b57-2a86-4aed-bec6-${200000000000 + index}';
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> json, {
  List<String>? selected,
}) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: _decode(json),
      selectedWidgetId: null,
      onSelected: selected?.add ?? (_) {},
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> _runtime(
  WidgetTester tester, {
  required Axis axis,
  required bool reverse,
  required bool shrink,
  required ScrollController controller,
  required ItemExtentBuilder builder,
  double width = 320,
  double height = 220,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: Center(
          child: SizedBox(
            width: width,
            height: height,
            child: ListView(
              scrollDirection: axis,
              reverse: reverse,
              shrinkWrap: shrink,
              controller: controller,
              padding: EdgeInsets.zero,
              itemExtentBuilder: builder,
              children: [
                for (var i = 0; i < 12; i++)
                  SizedBox(
                    key: ValueKey('runtime-item-$i'),
                    width: 1,
                    height: 1,
                    child: Text('$i'),
                  ),
              ],
            ),
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

CanvasModel _decode(Map<String, Object?> json) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json))));
Map<String, Object?> _model({
  Object? builder,
  int? fixed,
  Axis axis = Axis.vertical,
  bool reverse = false,
  bool shrink = false,
  int count = 20,
}) {
  final json =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  final list = _node(
    _id,
    'flutter.widgets.ListView',
    properties: {
      'itemExtentBuilder': ?builder,
      if (fixed != null) 'itemExtent': {'kind': 'integer', 'value': fixed},
      'scrollDirection': {'kind': 'enum', 'type': 'Axis', 'value': axis.name},
      'reverse': {'kind': 'boolean', 'value': reverse},
      'shrinkWrap': {'kind': 'boolean', 'value': shrink},
      'primary': {'kind': 'boolean', 'value': false},
    },
    slots: {
      'children': {
        'kind': 'list',
        'children': [
          for (var i = 0; i < count; i++)
            _node(
              _childId(i),
              'flutter.widgets.SizedBox',
              properties: {
                axis == Axis.vertical ? 'height' : 'width': {
                  'kind': 'integer',
                  'value': 32 + (i % 3) * 12,
                },
              },
              slots: {
                'child': _single(
                  _node(
                    _textId(i),
                    'flutter.widgets.Text',
                    properties: {
                      'data': {'kind': 'string', 'value': 'Item$i'},
                    },
                  ),
                ),
              },
            ),
        ],
      },
    },
  );
  json['root'] = _node(
    '6e88bff4-8d73-48aa-92b5-87aa3344f6a7',
    'flutter.material.Scaffold',
    slots: {
      'body': _single(
        _node(
          '1d6c16df-df2b-41cf-b46d-2e1d85538277',
          'flutter.widgets.Center',
          slots: {
            'child': _single(
              _node(
                'e223fd18-36c3-469b-ae9c-d7e69dd2fd83',
                'flutter.widgets.SizedBox',
                properties: {
                  'width': {'kind': 'integer', 'value': 320},
                  'height': {'kind': 'integer', 'value': 220},
                },
                slots: {'child': _single(list)},
              ),
            ),
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
