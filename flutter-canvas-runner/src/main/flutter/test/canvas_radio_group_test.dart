// Retained legacy Radio arguments are part of the pinned SDK comparison.
// ignore_for_file: deprecated_member_use
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _ref = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> _b(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _n(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
};
Map<String, Object?> _e(String value) => {
  'kind': 'enum',
  'type': 'double',
  'value': value,
};
String _id(int i) => 'ed629a28-efb5-4d4a-ac45-${i.toString().padLeft(12, '0')}';
Map<String, Object?> _node(
  int i,
  String type,
  Map<String, Object?> props, [
  Map<String, Object?> slots = const {},
]) => {'id': _id(i), 'type': type, 'properties': props, 'slots': slots};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _radio(int i, [Map<String, Object?> props = const {}]) =>
    _node(i, 'flutter.material.Radio', {
      'value': _s('x'),
      'valueType': _s('String'),
      'variant': _s('standard'),
      ...props,
    });
Map<String, Object?> _group(
  int i,
  Object child, [
  Map<String, Object?> props = const {},
]) => _node(
  i,
  'flutter.widgets.RadioGroup',
  {'valueType': _s('String'), 'onChanged': _s('noop'), ...props},
  {'child': _single(child)},
);
Map<String, Object?> _column(List<Object?> children) => _node(
  50,
  'flutter.widgets.Column',
  {
    'mainAxisSize': {'kind': 'enum', 'type': 'MainAxisSize', 'value': 'min'},
  },
  {
    'children': {'kind': 'list', 'children': children},
  },
);
Map<String, Object?> _wrap(
  int i,
  String type,
  Object child, [
  Map<String, Object?> props = const {},
]) => _node(i, type, props, {'child': _single(child)});
Map<String, Object?> _text(int i) =>
    _node(i, 'flutter.widgets.Text', {'data': _s('Retained text')});
Map<String, Object?> _model(Object group, {String profile = 'windows'}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = profile;
  model['root'] = _node(90, 'flutter.material.Scaffold', {}, {
    'body': _single(_wrap(91, 'flutter.widgets.Center', group)),
  });
  return model;
}

CanvasModel _decode(Map model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _widget(int id) => find.byKey(ValueKey('canvas-widget-${_id(id)}'));
Finder _radioAt(int id) => find.descendant(
  of: _widget(id),
  matching: find.byWidgetPredicate((w) => w is Radio),
);
String _messages(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((t) => t.message ?? '')
    .where((message) => message.isNotEmpty)
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Object group, {
  String profile = 'windows',
  ThemeData? theme,
  List<String>? selected,
  Widget Function(Widget)? wrap,
}) async {
  final view = CanvasDocumentView(
    model: _decode(_model(group, profile: profile)),
    selectedWidgetId: null,
    onSelected: selected?.add ?? (_) {},
  );
  await tester.pumpWidget(
    MaterialApp(
      theme: theme,
      themeAnimationDuration: Duration.zero,
      home: wrap?.call(view) ?? view,
    ),
  );
  await tester.pump(const Duration(milliseconds: 350));
  await tester.pump();
  expect(tester.takeException(), isNull);
}

bool _checked(WidgetTester tester, int id) {
  final raw = tester.widget<RawRadio>(
    find.descendant(
      of: _widget(id),
      matching: find.byWidgetPredicate((w) => w is RawRadio),
    ),
  );
  return raw.groupRegistry?.groupValue == raw.value;
}

Future<Uint8List> _pixels(
  WidgetTester tester,
  RenderRepaintBoundary boundary,
) async => (await tester.runAsync(() async {
  final image = await boundary.toImage();
  try {
    final data = await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    return Uint8List.fromList(data!.buffer.asUint8List());
  } finally {
    image.dispose();
  }
}))!;

void _additionalTests() {
  testWidgets(
    'unresolved group semantics does not leak into an outer different-type group',
    (tester) async {
      await _pump(
        tester,
        _group(
          1,
          _column([
            _radio(2, {'valueType': _s('double'), 'value': _n(1.0)}),
            _group(
              4,
              _radio(3, {
                'valueType': _s('int'),
                'value': _n(1),
                'groupValue': _n(1),
                'onChanged': _s('noop'),
              }),
              {'groupValue': _ref},
            ),
          ]),
          {'valueType': _s('double'), 'groupValue': _n(1.0)},
        ),
      );
      expect(_checked(tester, 2), isTrue);
      expect(_checked(tester, 3), isTrue);
      expect(_messages(tester), contains('RadioGroup ${_id(4)}'));
      expect(
        _messages(tester),
        isNot(
          contains('RadioGroup ${_id(1)}: group semantics preview unavailable'),
        ),
      );
    },
  );

  testWidgets(
    'nullable null duplicate clients are selected and guarded, null group with non-null T is not',
    (tester) async {
      await _pump(
        tester,
        _group(
          1,
          _column([
            _radio(2, {'nullableValueType': _b(true), 'value': _null}),
            _radio(3, {'nullableValueType': _b(true), 'value': _null}),
          ]),
          {'nullableValueType': _b(true), 'groupValue': _null},
        ),
      );
      expect(_checked(tester, 2), isTrue);
      expect(
        _messages(tester),
        contains('group navigation preview unavailable'),
      );
      await _pump(
        tester,
        _group(1, _column([_radio(2), _radio(3)]), {'groupValue': _null}),
      );
      expect(_checked(tester, 2), isFalse);
      expect(_messages(tester), isEmpty);
    },
  );

  testWidgets(
    'hidden maintained semantics and Block/MergeSemantics use actual assembled membership',
    (tester) async {
      for (final (type, props, invalid)
          in <(String, Map<String, Object?>, bool)>[
            (
              'flutter.widgets.Visibility',
              {
                'visible': _b(false),
                'maintainState': _b(true),
                'maintainAnimation': _b(true),
                'maintainSize': _b(true),
                'maintainSemantics': _b(true),
              },
              true,
            ),
            ('flutter.widgets.BlockSemantics', {}, false),
            ('flutter.widgets.MergeSemantics', {}, true),
          ]) {
        await _pump(
          tester,
          _group(1, _column([_radio(2), _wrap(4, type, _radio(3), props)]), {
            'groupValue': _s('x'),
          }),
        );
        expect(
          _messages(tester),
          contains('group navigation preview unavailable'),
        );
        expect(
          _messages(tester).contains('group semantics preview unavailable'),
          invalid,
          reason: type,
        );
      }
    },
  );

  testWidgets(
    'multiple independent group instances retain child and group states across theme/history',
    (tester) async {
      Object groups(String selected) => _column([
        _group(1, _radio(2), {'groupValue': _s(selected)}),
        _group(4, _radio(3), {'groupValue': _s(selected)}),
      ]);
      await _pump(tester, groups('x'));
      final states = tester.stateList(find.byType(RadioGroup<String>)).toList();
      final first = tester.state(_radioAt(2)),
          second = tester.state(_radioAt(3));
      for (final material3 in [false, true]) {
        await _pump(
          tester,
          groups('y'),
          theme: ThemeData(useMaterial3: material3),
        );
        expect(
          tester.stateList(find.byType(RadioGroup<String>)),
          orderedEquals(states),
        );
        expect(tester.state(_radioAt(2)), same(first));
        expect(tester.state(_radioAt(3)), same(second));
        await _pump(
          tester,
          groups('x'),
          theme: ThemeData(useMaterial3: material3),
        );
        expect(_checked(tester, 2), isTrue);
        expect(_checked(tester, 3), isTrue);
        expect(_messages(tester), isEmpty);
      }
    },
  );

  testWidgets(
    'actual group semantics role remains present for valid groups and recovers after invalid subtree',
    (tester) async {
      int roles() {
        var count = 0;
        void visit(SemanticsNode node) {
          if (node.getSemanticsData().role == ui.SemanticsRole.radioGroup) {
            count++;
          }
          node.visitChildren((child) {
            visit(child);
            return true;
          });
        }

        visit(tester.binding.pipelineOwner.semanticsOwner!.rootSemanticsNode!);
        return count;
      }

      Object group(String second) => _group(
        1,
        _column([
          _radio(2),
          _radio(3, {'value': _s(second)}),
        ]),
        {'groupValue': _s('x')},
      );
      await _pump(tester, group('y'));
      expect(roles(), 1);
      await _pump(tester, group('x'));
      expect(roles(), 0);
      await _pump(tester, group('y'));
      expect(roles(), 1);
    },
  );

  testWidgets(
    'valid controlled group pixels match real SDK across M2 M3 Apple adaptive and RTL',
    (tester) async {
      for (final material3 in [false, true]) {
        for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
          for (final direction in [TextDirection.ltr, TextDirection.rtl]) {
            for (final adaptive in [false, true]) {
              final theme = ThemeData(
                useMaterial3: material3,
                platform: platform,
                radioTheme: RadioThemeData(
                  fillColor: WidgetStateProperty.fromMap({
                    WidgetState.selected: Colors.orange,
                    WidgetState.any: Colors.teal,
                  }),
                ),
              );
              final group = _group(
                1,
                _column([
                  _radio(2, {
                    'variant': _s(adaptive ? 'adaptive' : 'standard'),
                  }),
                  _radio(3, {
                    'value': _s('y'),
                    'variant': _s(adaptive ? 'adaptive' : 'standard'),
                  }),
                ]),
                {'groupValue': _s('x')},
              );
              final body = _wrap(92, 'flutter.widgets.RepaintBoundary', group);
              await _pump(
                tester,
                _wrap(93, 'flutter.widgets.Directionality', body, {
                  'textDirection': {
                    'kind': 'enum',
                    'type': 'TextDirection',
                    'value': direction.name,
                  },
                }),
                theme: theme,
              );
              for (final painter in tester.renderObjectList<RenderCustomPaint>(
                find.byWidgetPredicate(
                  (widget) =>
                      widget is CustomPaint &&
                      widget.key.toString().contains('canvas-widget-outline-'),
                ),
              )) {
                painter.foregroundPainter = null;
              }
              await tester.pump();
              final boundary = tester.renderObject<RenderRepaintBoundary>(
                find
                    .descendant(
                      of: _widget(92),
                      matching: find.byType(RepaintBoundary),
                    )
                    .first,
              );
              final pixels = await _pixels(tester, boundary),
                  size = boundary.size;
              final key = GlobalKey();
              Radio<String> radio(String value) => adaptive
                  ? Radio<String>.adaptive(value: value)
                  : Radio<String>(value: value);
              await tester.pumpWidget(
                MaterialApp(
                  theme: theme,
                  themeAnimationDuration: Duration.zero,
                  home: Scaffold(
                    body: Center(
                      child: Directionality(
                        textDirection: direction,
                        child: RepaintBoundary(
                          key: key,
                          child: RadioGroup<String>(
                            groupValue: 'x',
                            onChanged: (_) {},
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [radio('x'), radio('y')],
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              );
              await tester.pump(const Duration(milliseconds: 350));
              await tester.pump();
              final raw =
                  key.currentContext!.findRenderObject()!
                      as RenderRepaintBoundary;
              expect(raw.size, size);
              expect(
                await _pixels(tester, raw),
                pixels,
                reason: '$material3 $platform $direction $adaptive',
              );
              expect(tester.takeException(), isNull);
            }
          }
        }
      }
    },
  );

  testWidgets(
    'intrinsic geometry and zero-sized child follow actual SDK without synthetic size',
    (tester) async {
      for (final intrinsic in [
        'flutter.widgets.IntrinsicWidth',
        'flutter.widgets.IntrinsicHeight',
      ]) {
        await _pump(tester, _wrap(4, intrinsic, _group(1, _text(2))));
        final measured = tester.getSize(find.byType(RadioGroup<String>));
        final raw = RadioGroup<String>(
          onChanged: (_) {},
          child: const Text('Retained text'),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: Center(
                child: intrinsic.endsWith('Width')
                    ? IntrinsicWidth(child: raw)
                    : IntrinsicHeight(child: raw),
              ),
            ),
          ),
        );
        expect(tester.getSize(find.byType(RadioGroup<String>)), measured);
        expect(tester.takeException(), isNull);
      }
    },
  );
}

void main() {
  _additionalTests();
  test('RadioGroup all four rows and required-child contract are exact', () {
    final model = _model(
      _group(1, _text(2), {'groupValue': _null, 'nullableValueType': _b(true)}),
    );
    expect(
      _decode(
        model,
      ).root.slots['body']!.child!.slots['child']!.child!.properties,
      hasLength(4),
    );
    final rows = canvasRuntimeWidgetSchemaContractForTesting().split('\n');
    final start = rows.indexOf('W|flutter.widgets.RadioGroup');
    final end = rows.indexWhere((s) => s.startsWith('W|'), start + 1);
    expect(rows.sublist(start, end), hasLength(7));
    expect(rows.sublist(start, end), contains('S|child|single|1|1|1|any'));
    expect(
      rows.sublist(start, end),
      contains(
        'C|flutter.widgets.RadioGroup|paletteCreate|wrapExistingChild|child',
      ),
    );
    for (final name in ['onChanged', 'valueType']) {
      final invalid = _group(1, _text(2));
      (invalid['properties'] as Map).remove(name);
      expect(() => _decode(_model(invalid)), throwsFormatException);
    }
    for (final callback in [_null, _s('other')]) {
      expect(
        () => _decode(_model(_group(1, _text(2), {'onChanged': callback}))),
        throwsFormatException,
      );
    }
    final empty = _group(1, _text(2));
    empty['slots'] = {'child': _single(null)};
    expect(() => _decode(_model(empty)), throwsFormatException);
    for (final type in ['String', 'int', 'double', 'num', 'bool', 'Object']) {
      for (final value in [_null, _ref]) {
        expect(
          () => _decode(
            _model(
              _group(1, _text(2), {'valueType': _s(type), 'groupValue': value}),
            ),
          ),
          returnsNormally,
        );
      }
    }
    expect(
      () => _decode(_model(_group(1, _text(2), {'groupValue': _n(1)}))),
      throwsFormatException,
    );
  });

  for (final profile in ['windows', 'web']) {
    testWidgets(
      'RadioGroup controlled generic forwarding preserves payload on $profile',
      (tester) async {
        final selected = <String>[];
        final group = _group(
          1,
          _column([
            _radio(2),
            _radio(3, {'value': _s('y')}),
          ]),
          {'groupValue': _s('x')},
        );
        final before = jsonEncode(group);
        await _pump(tester, group, profile: profile, selected: selected);
        expect(find.byType(RadioGroup<String>), findsOneWidget);
        expect(_checked(tester, 2), isTrue);
        expect(_checked(tester, 3), isFalse);
        await tester.tap(_radioAt(3));
        await tester.pumpAndSettle();
        expect(selected, contains(_id(3)));
        expect(_checked(tester, 2), isTrue);
        expect(_checked(tester, 3), isFalse);
        expect(jsonEncode(group), before);
        expect(_messages(tester), isEmpty);
      },
    );
  }

  testWidgets(
    'all twelve exact built-in T and T? dispatches preserve equality',
    (tester) async {
      for (final type in ['String', 'int', 'double', 'num', 'bool', 'Object']) {
        for (final nullable in [false, true]) {
          final value = nullable
              ? _null
              : switch (type) {
                  'String' => _s('x'),
                  'bool' => _b(true),
                  _ => _n(1),
                };
          final group = _group(
            1,
            _radio(2, {
              'valueType': _s(type),
              'nullableValueType': _b(nullable),
              'value': value,
            }),
            {
              'valueType': _s(type),
              'nullableValueType': _b(nullable),
              'groupValue': value,
            },
          );
          await _pump(tester, group);
          expect(_checked(tester, 2), isTrue, reason: '$type $nullable');
          final raw = tester.widget<RawRadio>(
            find.byWidgetPredicate((w) => w is RawRadio),
          );
          if (type == 'double' && !nullable) expect(raw.value, isA<double>());
          expect(_messages(tester), isEmpty);
        }
      }
    },
  );

  testWidgets(
    'NaN is unequal while both infinities and Object numeric equality remain exact',
    (tester) async {
      for (final type in ['double', 'num', 'Object']) {
        for (final value in ['nan', 'infinity', 'negativeInfinity']) {
          await _pump(
            tester,
            _group(
              1,
              _column([
                _radio(2, {'valueType': _s(type), 'value': _e(value)}),
                _radio(3, {'valueType': _s(type), 'value': _e(value)}),
              ]),
              {'valueType': _s(type), 'groupValue': _e(value)},
            ),
          );
          expect(_checked(tester, 2), value != 'nan');
          expect(
            _messages(tester).contains('group navigation preview unavailable'),
            value != 'nan',
          );
        }
      }
      await _pump(
        tester,
        _group(
          1,
          _column([
            _radio(2, {'valueType': _s('Object'), 'value': _n(1)}),
            _radio(3, {'valueType': _s('Object'), 'value': _n(1.0)}),
            _radio(4, {'valueType': _s('Object'), 'value': _b(true)}),
          ]),
          {'valueType': _s('Object'), 'groupValue': _n(1)},
        ),
      );
      expect(_checked(tester, 2), isTrue);
      expect(_checked(tester, 3), isTrue);
      expect(_checked(tester, 4), isFalse);
      expect(
        _messages(tester),
        contains('group navigation preview unavailable'),
      );
    },
  );

  testWidgets(
    'duplicate mounted selections are guarded and recover without remount',
    (tester) async {
      Object group(String second) => _group(
        1,
        _column([
          _radio(2),
          _radio(3, {'value': _s(second)}),
        ]),
        {'groupValue': _s('x')},
      );
      await _pump(tester, group('y'));
      final radioState = tester.state(_radioAt(2));
      final groupState = tester.state(find.byType(RadioGroup<String>));
      await _pump(tester, group('x'));
      expect(
        _messages(tester),
        contains('group navigation preview unavailable'),
      );
      expect(
        _messages(tester),
        contains('group semantics preview unavailable'),
      );
      expect(_checked(tester, 2), isTrue);
      expect(_checked(tester, 3), isTrue);
      expect(tester.state(_radioAt(2)), same(radioState));
      expect(tester.state(find.byType(RadioGroup<String>)), same(groupState));
      await _pump(tester, group('y'));
      expect(_messages(tester), isEmpty);
      expect(tester.state(_radioAt(2)), same(radioState));
      expect(tester.state(find.byType(RadioGroup<String>)), same(groupState));
      expect(tester.takeException(), isNull);
    },
  );

  for (final (name, wrapper, nav, semantics)
      in <(String, Object Function(Object), bool, bool)>[
        (
          'disabled',
          (r) {
            (r as Map)['properties']['enabled'] = _b(false);
            return r;
          },
          true,
          true,
        ),
        (
          'offstage',
          (r) => _wrap(4, 'flutter.widgets.Offstage', r),
          true,
          false,
        ),
        (
          'hidden disposed',
          (r) =>
              _wrap(4, 'flutter.widgets.Visibility', r, {'visible': _b(false)}),
          false,
          false,
        ),
        (
          'hidden retained',
          (r) => _wrap(4, 'flutter.widgets.Visibility', r, {
            'visible': _b(false),
            'maintainState': _b(true),
          }),
          true,
          false,
        ),
        (
          'excluded semantics',
          (r) => _wrap(4, 'flutter.widgets.ExcludeSemantics', r),
          true,
          false,
        ),
      ]) {
    testWidgets('membership is precise for $name', (tester) async {
      await _pump(
        tester,
        _group(1, _column([_radio(2), wrapper(_radio(3))]), {
          'groupValue': _s('x'),
        }),
      );
      final messages = _messages(tester);
      expect(messages.contains('group navigation preview unavailable'), nav);
      expect(
        messages.contains('group semantics preview unavailable'),
        semantics,
      );
    });
  }

  testWidgets(
    'semantics-only mixed generic conflict does not claim registry conflict',
    (tester) async {
      await _pump(
        tester,
        _group(
          1,
          _column([
            _radio(2),
            _radio(3, {
              'valueType': _s('int'),
              'value': _n(1),
              'groupValue': _n(1),
              'onChanged': _s('noop'),
            }),
          ]),
          {'groupValue': _s('x')},
        ),
      );
      expect(
        _messages(tester),
        contains('group semantics preview unavailable'),
      );
      expect(
        _messages(tester),
        isNot(contains('group navigation preview unavailable')),
      );
    },
  );

  testWidgets(
    'same T shadows while different and nullable T leave outer matching registry',
    (tester) async {
      for (final type in ['String', 'int', 'String?']) {
        final same = type == 'String';
        await _pump(
          tester,
          _group(
            1,
            _column([
              _radio(2),
              _group(4, _radio(3), {
                'valueType': _s(type == 'int' ? 'int' : 'String'),
                'nullableValueType': _b(type == 'String?'),
                'groupValue': type == 'int' ? _n(1) : _s('x'),
              }),
            ]),
            {'groupValue': _s('x')},
          ),
        );
        expect(_checked(tester, 2), isTrue);
        expect(_checked(tester, 3), isTrue);
        expect(
          _messages(tester).contains('group navigation preview unavailable'),
          !same,
        );
        expect(
          _messages(tester).contains('group semantics preview unavailable'),
          isFalse,
        );
      }
    },
  );

  testWidgets(
    'unresolved typed group blocks only matching radios and retains known nested boundary',
    (tester) async {
      final group = _group(
        1,
        _column([
          _radio(2),
          _radio(3, {
            'valueType': _s('int'),
            'value': _n(1),
            'groupValue': _n(1),
            'onChanged': _s('noop'),
          }),
          _group(4, _radio(5), {'groupValue': _s('x')}),
          _text(6),
        ]),
        {'groupValue': _ref},
      );
      await _pump(
        tester,
        group,
        wrap: (child) => RadioGroup<String>(
          groupValue: 'x',
          onChanged: (_) {},
          child: child,
        ),
      );
      expect(_radioAt(2), findsNothing);
      expect(_radioAt(3), findsOneWidget);
      expect(_radioAt(5), findsOneWidget);
      expect(find.text('Retained text'), findsOneWidget);
      expect(_messages(tester), contains('not allowed to join an outer group'));
      expect(_checked(tester, 5), isTrue);
    },
  );

  testWidgets(
    'custom T is a conservative barrier while nested known exact type can shadow it',
    (tester) async {
      await _pump(
        tester,
        _group(
          1,
          _column([
            _radio(2),
            _radio(3, {
              'valueType': _s('int'),
              'value': _n(1),
              'onChanged': _s('noop'),
            }),
            _group(4, _radio(5), {'groupValue': _s('x')}),
          ]),
          {'valueType': _ref},
        ),
        wrap: (child) => RadioGroup<String>(
          groupValue: 'x',
          onChanged: (_) {},
          child: child,
        ),
      );
      expect(_radioAt(2), findsNothing);
      expect(_radioAt(3), findsNothing);
      expect(_radioAt(5), findsOneWidget);
      expect(_messages(tester), contains('project types, values or equality'));
    },
  );

  testWidgets(
    'explicit registry project reference dominates and null uses inherited group',
    (tester) async {
      await _pump(
        tester,
        _group(
          1,
          _column([
            _radio(2, {'groupRegistry': _ref}),
            _radio(3, {
              'groupRegistry': _null,
              'groupValue': _ref,
              'onChanged': _ref,
            }),
          ]),
          {'groupValue': _s('x')},
        ),
      );
      expect(_radioAt(2), findsNothing);
      expect(_radioAt(3), findsOneWidget);
      expect(_checked(tester, 3), isTrue);
      expect(_messages(tester), contains('groupRegistry'));
      expect(
        _messages(tester),
        isNot(contains('preview limitation for onChanged')),
      );
    },
  );

  testWidgets(
    'callback reference uses diagnosed controlled no-op and remains selectable',
    (tester) async {
      await _pump(
        tester,
        _group(1, _radio(2), {'groupValue': _s('x'), 'onChanged': _ref}),
      );
      expect(_messages(tester), contains('project callback'));
      final dynamic group = tester.widget(find.byType(RadioGroup<String>));
      group.onChanged('other');
      await tester.pump();
      expect(_checked(tester, 2), isTrue);
    },
  );

  testWidgets(
    'ordinary child state survives generic and unavailable group transitions',
    (tester) async {
      Object group(Map<String, Object?> props) =>
          _group(1, _node(2, 'flutter.material.TextField', {}), props);
      await _pump(tester, group({'groupValue': _s('x')}));
      final state = tester.state(find.byType(TextField));
      final editableState = tester.state(find.byType(EditableText));
      final focus = tester
          .widget<EditableText>(find.byType(EditableText))
          .focusNode;
      focus.requestFocus();
      await tester.pump();
      for (final props in <Map<String, Object?>>[
        {'valueType': _s('int'), 'groupValue': _n(1)},
        {'valueType': _ref, 'groupValue': _ref},
        {'groupValue': _ref},
        {'groupValue': _s('x')},
      ]) {
        await _pump(tester, group(props));
        expect(tester.state(find.byType(TextField)), same(state));
        expect(tester.state(find.byType(EditableText)), same(editableState));
        expect(
          tester.widget<EditableText>(find.byType(EditableText)).focusNode,
          same(focus),
        );
      }
    },
  );

  testWidgets(
    'actual semantics role and keyboard navigation recover after conflict',
    (tester) async {
      Object group(String second) => _group(
        1,
        _column([
          _radio(2, {'autofocus': _b(true)}),
          _radio(3, {'value': _s(second)}),
          _radio(4, {'value': _s('z'), 'enabled': _b(false)}),
        ]),
        {'groupValue': _s('x')},
      );
      await _pump(tester, group('y'));
      final focus = tester
          .widget<RawRadio>(
            find.descendant(
              of: _widget(2),
              matching: find.byWidgetPredicate((w) => w is RawRadio),
            ),
          )
          .focusNode;
      focus.requestFocus();
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
      await tester.pump();
      final next = tester
          .widget<RawRadio>(
            find.descendant(
              of: _widget(3),
              matching: find.byWidgetPredicate((w) => w is RawRadio),
            ),
          )
          .focusNode;
      expect(next.hasFocus, isTrue);
      expect(_checked(tester, 2), isTrue);
      await _pump(tester, group('x'));
      await _pump(tester, group('y'));
      expect(_messages(tester), isEmpty);
      expect(next.hasFocus, isTrue);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'zero-size hidden duplicate group remains diagnosed and selectable',
    (tester) async {
      await _pump(
        tester,
        _group(
          1,
          _wrap(4, 'flutter.widgets.Offstage', _column([_radio(2), _radio(3)])),
          {'groupValue': _s('x')},
        ),
      );
      expect(
        _messages(tester),
        contains('group navigation preview unavailable'),
      );
      expect(
        _messages(tester),
        isNot(contains('group semantics preview unavailable')),
      );
      expect(
        find.byWidgetPredicate(
          (widget) =>
              widget.key.toString().contains(
                'canvas-zero-size-widget-target-',
              ) &&
              widget.key.toString().contains(_id(1)),
        ),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'raw SDK independently confirms registry and semantics failures',
    (tester) async {
      for (final child in <Widget>[
        Column(
          children: [
            const Radio<String>(value: 'x'),
            const Radio<String>(value: 'x', enabled: false),
          ],
        ),
        Column(
          children: [
            const Radio<String>(value: 'x'),
            Radio<int>(value: 1, groupValue: 1, onChanged: (_) {}),
          ],
        ),
      ]) {
        final errors = <String>[], previous = FlutterError.onError;
        FlutterError.onError = (details) =>
            errors.add(details.exceptionAsString());
        try {
          await tester.pumpWidget(
            MaterialApp(
              home: Material(
                child: RadioGroup<String>(
                  groupValue: 'x',
                  onChanged: (_) {},
                  child: child,
                ),
              ),
            ),
          );
        } finally {
          FlutterError.onError = previous;
        }
        expect(errors.any((e) => e.contains('multiple checked')), isTrue);
        await tester.pumpWidget(const SizedBox());
      }
    },
  );
}
