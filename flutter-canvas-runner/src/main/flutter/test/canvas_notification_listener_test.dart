import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart' show ScrollDirection;
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _id = '7bec8d5b-ed9a-479a-acb9-56ba0b31e42c';
const _childId = '8a6a2a8a-e0e8-4f2c-a57c-dc27306a55ec';
const _type = 'flutter.widgets.NotificationListener';
const _presence = {'kind': 'dartObjectReferencePresence'};
const _callback = {'kind': 'callbackPresence'};
const _null = {'kind': 'null'};
const _sdkTypes = <String, Type>{
  'Notification': NotificationListener<Notification>,
  'LayoutChangedNotification': NotificationListener<LayoutChangedNotification>,
  'ScrollNotification': NotificationListener<ScrollNotification>,
  'ScrollStartNotification': NotificationListener<ScrollStartNotification>,
  'ScrollUpdateNotification': NotificationListener<ScrollUpdateNotification>,
  'OverscrollNotification': NotificationListener<OverscrollNotification>,
  'ScrollEndNotification': NotificationListener<ScrollEndNotification>,
  'UserScrollNotification': NotificationListener<UserScrollNotification>,
  'SizeChangedLayoutNotification':
      NotificationListener<SizeChangedLayoutNotification>,
  'ScrollMetricsNotification': NotificationListener<ScrollMetricsNotification>,
  'OverscrollIndicatorNotification':
      NotificationListener<OverscrollIndicatorNotification>,
  'DraggableScrollableNotification':
      NotificationListener<DraggableScrollableNotification>,
  'KeepAliveNotification': NotificationListener<KeepAliveNotification>,
  'NavigationNotification': NotificationListener<NavigationNotification>,
};

void main() {
  test(
    'Exact two-property required-child contract and atomic wrapping retain protocol19',
    () {
      expect(canvasModelProtocolVersion, 19);
      expect(
        canvasRuntimeWidgetSchemaContractForTesting(),
        canvasReviewedWidgetSchemaContract.trimLeft(),
      );
      final contract = canvasReviewedWidgetSchemaContract;
      final section = contract.substring(
        contract.indexOf('W|$_type\n'),
        contract.indexOf('W|flutter.widgets.Offstage\n'),
      );
      expect('\nP|'.allMatches(section).length, 2);
      expect(section, contains('S|child|single|1|1|1|any'));
      expect(
        section,
        contains('C|$_type|paletteCreate|wrapExistingChild|child'),
      );
      expect(isCanvasPaletteWrapperWidgetType(_type), isTrue);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(
        canvasExistingChildWrapTargetSlot(
          parentWidgetType: _type,
          slotName: 'child',
        ),
        isNotNull,
      );
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: _type,
          childWidgetType: 'flutter.widgets.Text',
        ),
        isTrue,
      );
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: _type,
          childWidgetType: 'flutter.widgets.Expanded',
        ),
        isFalse,
      );
    },
  );

  test(
    'All14 closed type presets and anonymous custom type presence are accepted',
    () {
      for (final type in _sdkTypes.keys) {
        for (final callback in [null, _null, _presence, _callback]) {
          _decode(_model(type: _string(type), callback: callback));
        }
      }
      _decode(_model(type: _presence, callback: _presence));
    },
  );

  test(
    'Invalid type, handler identity, missing child and unreviewed arguments are rejected',
    () {
      for (final type in [
        null,
        _null,
        _callback,
        _string('Object'),
        _string('dynamic'),
        _string('Notification?'),
        _string('CustomNotification'),
        _string('Notification; execute()'),
        {'kind': 'dartObjectReferencePresence', 'rootSymbol': 'SecretType'},
        {
          'kind': 'dartObjectReferencePresence',
          'libraryUri': 'package:private/type.dart',
        },
        {'kind': 'dartObjectReference', 'rootSymbol': 'SecretType'},
      ]) {
        expect(
          () => _decode(_model(type: type)),
          throwsFormatException,
          reason: '$type',
        );
      }
      for (final callback in [
        _string('handler'),
        {'kind': 'boolean', 'value': true},
        {'kind': 'callbackPresence', 'handler': 'privateHandler'},
        {
          'kind': 'dartObjectReferencePresence',
          'access': 'zeroArgumentInvocation',
        },
        {'kind': 'callback', 'handler': 'privateHandler'},
      ]) {
        expect(
          () => _decode(_model(callback: callback)),
          throwsFormatException,
        );
      }
      final noChild = _model();
      _listenerJson(noChild)['slots'] = <String, Object?>{};
      expect(() => _decode(noChild), throwsFormatException);
      final emptyChild = _model();
      _listenerJson(emptyChild)['slots'] = {
        'child': {'kind': 'single', 'child': null},
      };
      expect(() => _decode(emptyChild), throwsFormatException);
      final extra = _model();
      (_listenerJson(extra)['properties']! as Map<String, Object?>)['depth'] = {
        'kind': 'integer',
        'value': 0,
      };
      expect(() => _decode(extra), throwsFormatException);
    },
  );

  for (final entry in _sdkTypes.entries) {
    testWidgets(
      'Real SDK ${entry.key} filter keeps callbacks inert and excludes unrelated notifications',
      (tester) async {
        await _pump(
          tester,
          _model(type: _string(entry.key), callback: _callback),
        );
        final dynamic listener = tester.widget(_finder);
        expect(listener.runtimeType, entry.value);
        final context = tester.element(find.text('Notification child'));
        final notification = _notification(entry.key, context);
        expect(listener.onNotification(notification), isFalse);
        final dynamic element = tester.element(_finder);
        expect(element.onNotification(notification), isFalse);
        expect(element.onNotification(const _OtherNotification()), isFalse);
        if (entry.key != 'Notification') {
          expect(
            () => listener.onNotification(const _OtherNotification()),
            throwsA(isA<TypeError>()),
          );
        }
        expect(tester.takeException(), isNull);
        await _pump(
          tester,
          _model(type: _string(entry.key), callback: _presence),
        );
        expect(
          (tester.widget(_finder) as dynamic).onNotification(notification),
          isFalse,
        );
        for (final empty in [null, _null]) {
          await _pump(
            tester,
            _model(type: _string(entry.key), callback: empty),
          );
          expect((tester.widget(_finder) as dynamic).onNotification, isNull);
        }
      },
    );
  }

  testWidgets(
    'Notification callbacks do not cancel framework bubbling or select widgets',
    (tester) async {
      final seen = <Notification>[];
      final selected = <String>[];
      await tester.pumpWidget(
        NotificationListener<Notification>(
          onNotification: (notification) {
            seen.add(notification);
            return false;
          },
          child: CanvasModelApp(
            model: _decode(_model(callback: _presence)),
            selectedWidgetId: null,
            onSelected: selected.add,
          ),
        ),
      );
      await tester.pump();
      seen.clear();
      final notification = const _OtherNotification();
      notification.dispatch(tester.element(find.text('Notification child')));
      expect(seen, contains(same(notification)));
      expect(selected, isEmpty);
      await tester.tap(find.text('Notification child'));
      expect(selected.last, _childId);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Custom type approximation is explicit and preserves child identity, geometry and selection across type changes',
    (tester) async {
      await _pump(tester, _model(type: _string('ScrollNotification')));
      final child = tester.element(find.text('Notification child'));
      final bounds = tester.getRect(find.text('Notification child'));
      for (final type in [
        _presence,
        _string('NavigationNotification'),
        _string('Notification'),
        _presence,
      ]) {
        await _pump(tester, _model(type: type, callback: _presence));
        expect(tester.element(find.text('Notification child')), same(child));
        expect(tester.getRect(find.text('Notification child')), bounds);
        expect(tester.getSize(_finder), const Size(120, 100));
        if (identical(type, _presence)) {
          expect(
            tester.widget(_finder).runtimeType,
            NotificationListener<Notification>,
          );
          expect(
            find.byWidgetPredicate(
              (widget) =>
                  widget is Tooltip &&
                  (widget.message?.contains(
                        'cannot load or execute the project Notification subtype',
                      ) ??
                      false) &&
                  (widget.message?.contains('explicit preview approximation') ??
                      false),
            ),
            findsOneWidget,
          );
        }
        expect(tester.takeException(), isNull);
      }
    },
  );

  for (final platform in ['windows', 'android', 'ios']) {
    testWidgets(
      'Zero-size required child preserves layout and external selection handle on $platform',
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
        expect(tester.getSize(_finder), Size.zero);
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-group-$_id'),
        );
        expect(handle, findsOneWidget);
        expect(find.ancestor(of: handle, matching: _finder), findsNothing);
        await tester.tap(handle);
        expect(selected.last, _id);
        expect(tester.takeException(), isNull);
      },
    );
  }
}

Finder get _finder =>
    find.byKey(const ValueKey('canvas-notification-listener-$_id'));

class _OtherNotification extends Notification {
  const _OtherNotification();
}

Notification _notification(String type, BuildContext context) {
  final metrics = FixedScrollMetrics(
    minScrollExtent: 0,
    maxScrollExtent: 200,
    pixels: 10,
    viewportDimension: 100,
    axisDirection: AxisDirection.down,
    devicePixelRatio: 1,
  );
  return switch (type) {
    'Notification' => const _OtherNotification(),
    'LayoutChangedNotification' => const LayoutChangedNotification(),
    'SizeChangedLayoutNotification' => const SizeChangedLayoutNotification(),
    'ScrollNotification' ||
    'ScrollUpdateNotification' => ScrollUpdateNotification(
      metrics: metrics,
      context: context,
      scrollDelta: 1,
    ),
    'ScrollStartNotification' => ScrollStartNotification(
      metrics: metrics,
      context: context,
    ),
    'OverscrollNotification' => OverscrollNotification(
      metrics: metrics,
      context: context,
      overscroll: 1,
    ),
    'ScrollEndNotification' => ScrollEndNotification(
      metrics: metrics,
      context: context,
    ),
    'UserScrollNotification' => UserScrollNotification(
      metrics: metrics,
      context: context,
      direction: ScrollDirection.forward,
    ),
    'ScrollMetricsNotification' => ScrollMetricsNotification(
      metrics: metrics,
      context: context,
    ),
    'OverscrollIndicatorNotification' => OverscrollIndicatorNotification(
      leading: true,
    ),
    'DraggableScrollableNotification' => DraggableScrollableNotification(
      extent: .5,
      minExtent: 0,
      maxExtent: 1,
      initialExtent: .5,
      context: context,
    ),
    'KeepAliveNotification' => KeepAliveNotification(KeepAliveHandle()),
    'NavigationNotification' => const NavigationNotification(
      canHandlePop: true,
    ),
    _ => throw StateError(type),
  };
}

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
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _model({
  Object? type = const {'kind': 'string', 'value': 'Notification'},
  Object? callback,
  String platform = 'windows',
  Size size = const Size(120, 100),
  Map<String, Object?>? child,
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
              properties: {
                'notificationType': ?type,
                'onNotification': ?callback,
              },
              child:
                  child ??
                  {
                    'id': _childId,
                    'type': 'flutter.widgets.Text',
                    'properties': {'data': _string('Notification child')},
                    'slots': <String, Object?>{},
                  },
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
Map<String, Object?> _listenerJson(Map<String, Object?> model) {
  var current = model['root']! as Map<String, Object?>;
  while (current['id'] != _id) {
    final slots = current['slots']! as Map<String, Object?>;
    current =
        ((slots[slots.containsKey('body') ? 'body' : 'child']!
                as Map<String, Object?>)['child']!
            as Map<String, Object?>);
  }
  return current;
}
