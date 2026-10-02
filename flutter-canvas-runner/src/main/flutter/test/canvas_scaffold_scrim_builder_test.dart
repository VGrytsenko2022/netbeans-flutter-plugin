import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _id = '6e88bff4-8d73-48aa-92b5-87aa3344f6a7';
const _childId = '8a6a2a8a-e0e8-4f2c-a57c-dc27306a55ec';
const _property = 'bottomSheetScrimBuilder';
const _presence = {'kind': 'dartObjectReferencePresence'};
const _signature = 'Widget? Function(BuildContext, Animation<double>)';

void main() {
  test(
    'Scaffold18property contract admits exact nonnull function references without adding slots or protocols',
    () {
      expect(canvasModelProtocolVersion, 20);
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      expect(contract, canvasReviewedWidgetSchemaContract.trimLeft());
      final start = contract.indexOf('W|flutter.material.Scaffold\n');
      final end = contract.indexOf('\nW|', start) + 1;
      final section = contract.substring(start, end);
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(section),
        hasLength(18),
      );
      expect(
        RegExp(r'^S\|', multiLine: true).allMatches(section),
        hasLength(3),
      );
      expect(
        section,
        contains(
          'P|$_property|dartObjectReference|0|-|-|'
          'dartObjectReference:dartObjectReference:v1:$_signature:currentOrPackage:'
          'root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true)',
        ),
      );
      expect(_decode(_model()).root.properties.containsKey(_property), isFalse);
      expect(
        _decode(_model(builder: _presence)).root.properties[_property]!.kind,
        'dartObjectReferencePresence',
      );
    },
  );

  test(
    'Explicit callback null, executable identities and unrelated callback kinds are rejected',
    () {
      for (final value in [
        {'kind': 'null'},
        {'kind': 'callbackPresence'},
        {'kind': 'boolean', 'value': false},
        {'kind': 'string', 'value': 'projectBuilder'},
        {'kind': 'dartObjectReference', 'rootSymbol': 'privateBuilder'},
        {'kind': 'dartObjectReferencePresence', 'rootSymbol': 'privateBuilder'},
        {
          'kind': 'dartObjectReferencePresence',
          'libraryUri': 'package:private/builders.dart',
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
    },
  );

  testWidgets(
    'Presence and reset retain exact SDK default, geometry, Scaffold state and child selection without barriers',
    (tester) async {
      final selected = <String>[];
      await _pump(tester, _model(), onSelected: selected.add);
      final state = tester.state<ScaffoldState>(_scaffoldFinder);
      final child = tester.element(find.text('Scaffold child'));
      final bounds = tester.getRect(_scaffoldFinder);
      final defaultBuilder = const Scaffold().bottomSheetScrimBuilder;
      for (final value in [_presence, null, _presence, null]) {
        await _pump(tester, _model(builder: value), onSelected: selected.add);
        expect(tester.state<ScaffoldState>(_scaffoldFinder), same(state));
        expect(tester.element(find.text('Scaffold child')), same(child));
        expect(tester.getRect(_scaffoldFinder), bounds);
        expect(
          tester.widget<Scaffold>(_scaffoldFinder).bottomSheetScrimBuilder,
          defaultBuilder,
        );
        expect(_barrierFinder, findsNothing);
        final tooltip = find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              (w.message?.contains('bottom-sheet scrim preview unavailable') ??
                  false) &&
              (w.message?.contains('actual SDK default scrim is retained') ??
                  false),
        );
        expect(tooltip, value == null ? findsNothing : findsOneWidget);
        await tester.tap(find.text('Scaffold child'));
        expect(selected.last, _childId);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'Captured Canvas default builder tracks animation and returns the SDK nondismissible black barrier',
    (tester) async {
      await _pump(tester, _model(builder: _presence));
      final sdkBuilder = tester
          .widget<Scaffold>(_scaffoldFinder)
          .bottomSheetScrimBuilder;
      final animation = AnimationController(vsync: tester);
      addTearDown(animation.dispose);
      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) => KeyedSubtree(
              key: const ValueKey('direct-sdk-scrim'),
              child: sdkBuilder(context, animation)!,
            ),
          ),
        ),
      );
      for (final value in [0.0, .25, .5, .75, 1.0, .5, 0.0]) {
        animation.value = value;
        await tester.pump();
        final barrier = tester.widget<ModalBarrier>(
          find.descendant(
            of: find.byKey(const ValueKey('direct-sdk-scrim')),
            matching: find.byType(ModalBarrier),
          ),
        );
        expect(barrier.dismissible, isFalse);
        expect(barrier.color, _sdkColor(value));
      }
      expect(tester.takeException(), isNull);
    },
  );

  for (final present in [false, true]) {
    testWidgets(
      'Real showBottomSheet maps70to100percent extent to default scrim with reference=$present',
      (tester) async {
        await _pump(tester, _model(builder: present ? _presence : null));
        final state = tester.state<ScaffoldState>(_scaffoldFinder);
        late BuildContext sheetContext;
        final sheet = state.showBottomSheet(
          (context) => Builder(
            builder: (context) {
              sheetContext = context;
              return const SizedBox(
                height: 100,
                child: Text('Runtime bottom sheet'),
              );
            },
          ),
        );
        await tester.pumpAndSettle();
        expect(_barrierFinder, findsNothing);
        for (final extent in [.69, .7, .7001, .85, 1.0, .9, .7, .5]) {
          DraggableScrollableNotification(
            extent: extent,
            minExtent: .1,
            maxExtent: 1,
            initialExtent: .5,
            context: sheetContext,
          ).dispatch(sheetContext);
          await tester.pump();
          final remaining = 1 - extent;
          if (remaining >= .3) {
            expect(_barrierFinder, findsNothing);
          } else {
            expect(_barrierFinder, findsOneWidget);
            final barrier = tester.widget<ModalBarrier>(_barrierFinder);
            expect(barrier.dismissible, isFalse);
            expect(barrier.color, _sdkColor(1 - remaining / .3));
          }
        }
        sheet.close();
        await tester.pumpAndSettle();
        expect(_barrierFinder, findsNothing);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'Builder presence changes preserve open-sheet state and default scrim instead of adding or hiding barriers',
    (tester) async {
      await _pump(tester, _model());
      final state = tester.state<ScaffoldState>(_scaffoldFinder);
      late BuildContext sheetContext;
      final sheet = state.showBottomSheet(
        (context) => Builder(
          builder: (context) {
            sheetContext = context;
            return const SizedBox(
              height: 100,
              child: Text('Persistent runtime sheet'),
            );
          },
        ),
      );
      await tester.pumpAndSettle();
      DraggableScrollableNotification(
        extent: .9,
        minExtent: .1,
        maxExtent: 1,
        initialExtent: .5,
        context: sheetContext,
      ).dispatch(sheetContext);
      await tester.pump();
      final initialBarrier = tester.widget<ModalBarrier>(_barrierFinder);
      for (final value in [_presence, null, _presence]) {
        await _pump(tester, _model(builder: value));
        expect(tester.state<ScaffoldState>(_scaffoldFinder), same(state));
        expect(find.text('Persistent runtime sheet'), findsOneWidget);
        expect(_barrierFinder, findsOneWidget);
        expect(
          tester.widget<ModalBarrier>(_barrierFinder).color,
          initialBarrier.color,
        );
        expect(
          tester.widget<ModalBarrier>(_barrierFinder).dismissible,
          isFalse,
        );
      }
      sheet.close();
      await tester.pumpAndSettle();
      expect(_barrierFinder, findsNothing);
      expect(tester.takeException(), isNull);
    },
  );
}

Color _sdkColor(double animationValue) => Colors.black.withAlpha(
  (255 * math.max(.1, .6 - .9 * (1 - animationValue))).round(),
);
Finder get _scaffoldFinder =>
    find.byKey(const ValueKey('canvas-scaffold-$_id'));
Finder get _barrierFinder =>
    find.descendant(of: _scaffoldFinder, matching: find.byType(ModalBarrier));
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> json, {
  ValueChanged<String>? onSelected,
}) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: _decode(json),
      selectedWidgetId: null,
      onSelected: onSelected ?? (_) {},
    ),
  );
  await tester.pump();
}

CanvasModel _decode(Map<String, Object?> json) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json))));
Map<String, Object?> _model({Object? builder}) {
  final json =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  json['root'] = {
    'id': _id,
    'type': 'flutter.material.Scaffold',
    'properties': {_property: ?builder},
    'slots': {
      'body': {
        'kind': 'single',
        'child': {
          'id': '1d6c16df-df2b-41cf-b46d-2e1d85538277',
          'type': 'flutter.widgets.Center',
          'properties': <String, Object?>{},
          'slots': {
            'child': {
              'kind': 'single',
              'child': {
                'id': _childId,
                'type': 'flutter.widgets.Text',
                'properties': {
                  'data': {'kind': 'string', 'value': 'Scaffold child'},
                },
                'slots': <String, Object?>{},
              },
            },
          },
        },
      },
    },
  };
  return json;
}
