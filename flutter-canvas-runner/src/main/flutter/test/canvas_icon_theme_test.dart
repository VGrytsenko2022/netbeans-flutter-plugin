import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.widgets.IconTheme';
const _id = '8b0a49b1-a09b-4e08-8f03-36db27c52202';
const _innerId = '1e5c48e6-216a-4608-a7e2-46b5de6d2a93';
const _childId = 'aa31ff8f-7ea3-44a4-a9c9-7188f086f901';
const _iconId = '05954263-4a92-40b6-82e4-7f91fbb0a444';
const _textId = 'c147b92b-d98a-4cf4-9710-aa7b89fdcc9e';
const _columnId = '78e5d847-9d19-4bca-8bb5-36658cbbdc8b';
const _emptyId = 'b466e791-ff10-477d-b5f7-528c113654cc';
const _outer = IconThemeData(
  size: 31,
  fill: .7,
  weight: 620,
  grade: -12,
  opticalSize: 38,
  color: Color(0xCC123456),
  opacity: .6,
  shadows: [
    Shadow(color: Color(0x88446688), offset: Offset(-2, 3), blurRadius: 4),
  ],
  applyTextScaling: true,
);

void main() {
  test(
    'IconTheme exact contract has nine optional SDK leaves and required binary constructor mode',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final block = contract.substring(
        start,
        contract.indexOf('W|flutter.widgets.IgnorePointer\n', start),
      );
      expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(10));
      expect(
        block,
        contains('P|merge|boolean|1|boolean:false|-|boolean:any\n'),
      );
      expect(
        block,
        contains('P|opacity|double|0|-|double:*:1:*:1|double:range:*:1:*:1\n'),
      );
      expect(
        block,
        endsWith(
          'S|child|single|1|1|1|any\nC|$_type|paletteCreate|wrapExistingChild|child\n',
        ),
      );
      expect(canvasModelProtocolVersion, 18);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), isTrue);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      final defaults = _find(_decode(_model()).root, _id)!.properties;
      expect(defaults.keys, ['merge']);
      expect(defaults['merge']!.value, false);
      final all = _all();
      final names = all.keys.toList();
      for (final merge in [false, true]) {
        for (var mask = 0; mask < 1 << names.length; mask++) {
          final properties = <String, Object?>{
            'merge': _bool(merge),
            for (var i = 0; i < names.length; i++)
              if (mask & (1 << i) != 0) names[i]: all[names[i]],
          };
          expect(
            _find(
              _decode(_model(properties: properties)).root,
              _id,
            )!.properties.keys,
            unorderedEquals(properties.keys),
          );
        }
      }
    },
  );

  test(
    'IconTheme validates numeric bounds without narrowing finite opacity to the clamped getter',
    () {
      final valid = <String, List<num>>{
        'size': [0, .125, 32, 9007199254740991],
        'fill': [0, .5, 1],
        'weight': [.001, 400, 32767.999],
        'grade': [-32768, 0, 32767.999],
        'opticalSize': [.001, 48, 32767.999],
        'opacity': [-1e200, -1, 0, .5, 1, 12, 1e200],
      };
      for (final entry in valid.entries) {
        for (final value in entry.value) {
          final property = entry.key == 'size' && value is int
              ? _int(value)
              : _double(value.toDouble());
          expect(
            _find(
              _decode(_model(properties: {entry.key: property})).root,
              _id,
            )!.properties[entry.key]!.value,
            value,
          );
        }
      }
      for (final entry in <String, List<double>>{
        'size': [-.01],
        'fill': [-.001, 1.001],
        'weight': [-1, 0, 32768],
        'grade': [-32768.001, 32768],
        'opticalSize': [-1, 0, 32768],
      }.entries) {
        for (final value in entry.value) {
          expect(
            () => _decode(_model(properties: {entry.key: _double(value)})),
            throwsFormatException,
          );
        }
      }
      for (final name in valid.keys) {
        final json = jsonEncode(
          _model(properties: {name: _double(76543.25)}),
        ).replaceAll('76543.25', '1e309');
        expect(
          () => CanvasModel.decode(Uint8List.fromList(utf8.encode(json))),
          throwsFormatException,
        );
      }
      for (final name in [
        'fill',
        'weight',
        'grade',
        'opticalSize',
        'opacity',
      ]) {
        expect(
          () => _decode(_model(properties: {name: _int(1)})),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'IconTheme rejects absent mode explicit null wrong kinds opaque code and unsupported aliases',
    () {
      final missing = _model();
      (_wrapper(missing)['properties'] as Map).remove('merge');
      expect(() => _decode(missing), throwsFormatException);
      for (final name in [..._all().keys, 'merge']) {
        for (final value in [
          null,
          {'kind': 'null'},
          _string('raw code'),
          {'kind': 'boolean', 'value': 'true'},
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
          );
        }
      }
      for (final name in [
        'key',
        'data',
        'child',
        'fallback',
        'fontWeight',
        'blendMode',
        'semanticLabel',
        'textDirection',
      ]) {
        expect(
          () => _decode(_model(properties: {name: _bool(true)})),
          throwsFormatException,
        );
      }
      for (final token in canvasColorSchemeThemeTokens) {
        expect(
          () => _decode(_model(properties: {'color': _theme(token)})),
          returnsNormally,
        );
      }
      for (final color in [
        {'kind': 'color', 'argb': '0xff112233'},
        _theme('material.textTheme.bodyLarge'),
        {'kind': 'color', 'argb': '0xFF112233', 'extra': true},
      ]) {
        expect(
          () => _decode(_model(properties: {'color': color})),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'IconTheme shadow lists preserve ordered stable identities empty override and bounded closed envelopes',
    () {
      for (final length in [0, 1, 256]) {
        final items = List.generate(length, _shadow);
        final properties = _find(
          _decode(_model(properties: {'shadows': _shadows(items)})).root,
          _id,
        )!.properties;
        expect(
          (properties['shadows']!.value as List<CanvasShadowValue>).map(
            (e) => e.id,
          ),
          items.map((e) => e['id']),
        );
      }
      final invalid = <Object?>[
        _shadows(List.generate(257, _shadow)),
        _shadows([_shadow(0), _shadow(0)]),
        _shadows([
          {..._shadow(0), 'id': 'not-a-uuid'},
        ]),
        _shadows([
          {..._shadow(0), 'blurRadius': -1.0},
        ]),
        _shadows([
          {..._shadow(0), 'offsetX': '1.0'},
        ]),
        _shadows([
          {..._shadow(0), 'extra': true},
        ]),
        _shadows([
          {
            ..._shadow(0),
            'color': {'kind': 'theme', 'token': 'unknown'},
          },
        ]),
        {..._shadows([]), 'extra': true},
      ];
      for (final value in invalid) {
        expect(
          () => _decode(_model(properties: {'shadows': value})),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'IconTheme required child and generic wrapper reject empty slots and illegal parent data',
    () {
      for (final slots in [
        <String, Object?>{},
        {
          'child': {'kind': 'single', 'child': null},
        },
        {
          'child': {
            'kind': 'list',
            'children': [_icon()],
          },
        },
        {
          'children': {'kind': 'single', 'child': _icon()},
        },
        {
          'child': {'kind': 'single', 'child': _icon(), 'extra': true},
        },
      ]) {
        final model = _model();
        _wrapper(model)['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
      for (final type in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () =>
              _decode(_model(child: _node(_childId, 'flutter.widgets.$type'))),
          throwsFormatException,
        );
        expect(
          canvasWrapperAcceptsExistingChild(
            wrapperWidgetType: _type,
            childWidgetType: 'flutter.widgets.$type',
          ),
          isFalse,
        );
      }
      for (final destination in [
        (type: 'flutter.widgets.Column', slot: 'children'),
        (type: 'flutter.widgets.Center', slot: 'child'),
        (type: _type, slot: 'child'),
        (type: 'flutter.widgets.Visibility', slot: 'replacement'),
      ]) {
        for (final count in [0, 1]) {
          expect(
            canvasDropTargetAcceptsSource(
              parentWidgetType: destination.type,
              slotName: destination.slot,
              currentChildCount: count,
              insertionIndex: 0,
              source: _source(_type),
            ),
            count == 1,
          );
        }
      }
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: _type,
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: _source('flutter.widgets.Icon'),
        ),
        false,
      );
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'IconTheme all nine leaves direct and merge retain exact raw and resolved SDK data on $platform',
      (tester) async {
        final all = _all();
        final variants = <Map<String, Object?>>[
          {},
          all,
          for (final entry in all.entries) {entry.key: entry.value},
          for (final name in all.keys) {...all}..remove(name),
        ];
        for (final merge in [false, true]) {
          for (final properties in variants) {
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: _outerProperties(),
                child: _node(
                  _innerId,
                  _type,
                  properties: {'merge': _bool(merge), ...properties},
                  child: _icon(),
                ),
              ),
            );
            final context = tester.element(_iconFinder());
            final raw = context
                .getInheritedWidgetOfExactType<IconTheme>()!
                .data;
            final expected = merge
                ? _outer.merge(_data(properties))
                : _data(properties);
            expect(raw, expected);
            expect(
              IconTheme.of(context),
              const IconThemeData.fallback().merge(expected),
            );
            final icon = tester.widget<Icon>(_iconFinder());
            expect([
              icon.size,
              icon.fill,
              icon.weight,
              icon.grade,
              icon.opticalSize,
              icon.color,
              icon.shadows,
              icon.applyTextScaling,
            ], everyElement(isNull));
            _expectIcon(tester, const IconThemeData.fallback().merge(expected));
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'IconTheme inner direct reset and merge preserve raw nulls and explicit false empty shadows on $platform',
      (tester) async {
        for (final merge in [false, true]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: _outerProperties(),
              child: _node(
                _innerId,
                _type,
                child: _node(
                  _childId,
                  _type,
                  properties: {
                    'merge': _bool(merge),
                    'color': _color('0xFF887766'),
                    'applyTextScaling': _bool(false),
                    'shadows': _shadows([]),
                  },
                  child: _icon(),
                ),
              ),
            ),
          );
          final context = tester.element(_iconFinder());
          final raw = context.getInheritedWidgetOfExactType<IconTheme>()!.data;
          expect(
            raw.size,
            isNull,
            reason:
                'merge uses the raw nearest empty theme, not IconTheme.of fallback nor the outer rich theme',
          );
          expect(raw.fill, isNull);
          expect(raw.opacity, isNull);
          expect(raw.applyTextScaling, false);
          expect(raw.shadows, isEmpty);
          expect(IconTheme.of(context).size, 24);
          expect(_iconStyle(tester).color, const Color(0xFF887766));
          expect(_iconStyle(tester).shadows, isEmpty);
          expect(tester.getSize(_iconFinder()), const Size.square(24));
        }
      },
    );

    testWidgets(
      'IconTheme yields all local Icon overrides without suppressing inherited opacity on $platform',
      (tester) async {
        for (final merge in [false, true]) {
          final local = _all()..remove('opacity');
          local['applyTextScaling'] = _bool(false);
          local['shadows'] = _shadows([]);
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {..._outerProperties(), 'merge': _bool(merge)},
              child: _icon(properties: local),
            ),
          );
          final expected = _data(local).copyWith(opacity: _outer.opacity);
          _expectIcon(tester, expected);
          expect(_iconStyle(tester).shadows, isEmpty);
          expect(tester.getSize(_iconFinder()), const Size.square(19));
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'IconTheme finite raw opacity clamps only in SDK and multiplies glyph alpha not shadow alpha on $platform',
      (tester) async {
        for (final merge in [false, true]) {
          for (final opacity in [-5.0, 0.0, .25, 1.0, 8.0]) {
            for (final localColor in [false, true]) {
              final model = _model(
                platform: platform,
                properties: {
                  ..._outerProperties(),
                  'merge': _bool(merge),
                  'opacity': _double(opacity),
                },
                child: _icon(
                  properties: {if (localColor) 'color': _color('0x80ABCDEF')},
                ),
              );
              expect(
                _find(_decode(model).root, _id)!.properties['opacity']!.value,
                opacity,
              );
              await _pump(tester, model);
              final effective = IconTheme.of(tester.element(_iconFinder()));
              expect(effective.opacity, opacity.clamp(0.0, 1.0));
              final color = localColor
                  ? const Color(0x80ABCDEF)
                  : _outer.color!;
              expect(
                _iconStyle(tester).color,
                _attenuate(color, effective.opacity!),
              );
              expect(_iconStyle(tester).shadows, _outer.shadows);
              expect(tester.getSize(_iconFinder()), const Size.square(31));
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    testWidgets(
      'IconTheme semantic colors and ordered typed shadows resolve against actual Theme on $platform',
      (tester) async {
        for (final token in ['primary', 'secondary']) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {
                'color': _theme('material.colorScheme.$token'),
                'opacity': _double(.25),
                'shadows': _shadows([
                  {
                    ..._shadow(0),
                    'color': {
                      'kind': 'theme',
                      'token': 'material.colorScheme.$token',
                    },
                  },
                  {
                    ..._shadow(1),
                    'color': {'kind': 'literal', 'argb': '0x80123456'},
                    'offsetX': 4.0,
                  },
                ]),
              },
            ),
          );
          final scheme = Theme.of(tester.element(_iconFinder())).colorScheme;
          final expectedColor = token == 'primary'
              ? scheme.primary
              : scheme.secondary;
          expect(_iconStyle(tester).color, _attenuate(expectedColor, .25));
          expect(_iconStyle(tester).shadows!.map((e) => e.color), [
            expectedColor,
            const Color(0x80123456),
          ]);
          expect(_iconStyle(tester).shadows!.map((e) => e.offset), [
            const Offset(-2, 3),
            const Offset(4, 3),
          ]);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'IconTheme applyTextScaling inherits resets and honors explicit local false on $platform',
      (tester) async {
        for (final inherited in [null, false, true]) {
          for (final local in [null, false, true]) {
            final model = _model(
              platform: platform,
              properties: {
                'size': _int(13),
                if (inherited != null) 'applyTextScaling': _bool(inherited),
              },
              child: _icon(
                properties: {
                  if (local != null) 'applyTextScaling': _bool(local),
                },
              ),
            );
            (model['profile'] as Map)['textScaleFactor'] = 2.0;
            await _pump(tester, model);
            final size = (local ?? inherited ?? false) ? 26.0 : 13.0;
            expect(_iconStyle(tester).fontSize, size);
            expect(tester.getSize(_iconFinder()), Size.square(size));
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'IconTheme transparent and empty icons preserve semantic labels and real pointer selection on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final empty in [false, true]) {
            final selected = <String>[];
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(
                  _model(
                    platform: platform,
                    properties: {'opacity': _double(0), 'size': _int(37)},
                    child: _icon(empty: empty),
                  ),
                ),
                selectedWidgetId: null,
                onSelected: selected.add,
              ),
            );
            await tester.pump();
            expect(tester.getSize(_iconFinder()), const Size.square(37));
            expect(
              _labels(tester).any((label) => label.contains('Themed icon')),
              true,
            );
            await tester.tap(_widget(_iconId));
            await tester.pump();
            expect(selected, contains(_iconId));
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'IconTheme data updates preserve child state focus layout semantics on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final merge in [false, true]) {
            await tester.pumpWidget(const SizedBox());
            FocusNode? original;
            Size? size;
            for (final color in [null, '0xFF112233', '0xFF987654', null]) {
              await _pump(
                tester,
                _model(
                  platform: platform,
                  properties: {
                    'merge': _bool(merge),
                    if (color != null) 'color': _color(color),
                  },
                  child: _node(
                    _childId,
                    'flutter.material.ElevatedButton',
                    child: _text(),
                  ),
                ),
              );
              final focus = Focus.of(tester.element(find.text('Theme child')));
              original ??= focus;
              expect(identical(original, focus), true);
              focus.requestFocus();
              await tester.pump();
              expect(focus.hasFocus, true);
              size ??= tester.getSize(_widget(_id));
              expect(tester.getSize(_widget(_id)), size);
              expect(
                _labels(tester).any((label) => label.contains('Theme child')),
                true,
              );
              expect(tester.takeException(), isNull);
            }
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'IconTheme keeps descendant Text F2 commit cancel reopen unchanged on $platform',
      (tester) async {
        final commits = <(String, String)>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(platform: platform, properties: _all(), child: _text()),
            ),
            selectedWidgetId: _textId,
            onSelected: (_) {},
            inlineTextEditEnabled: true,
            onInlineTextCommit: (id, text, _) {
              commits.add((id, text));
              return true;
            },
          ),
        );
        await tester.pump();
        await tester.tapAt(
          tester.getTopLeft(
                find.byKey(const ValueKey('canvas-interaction-surface')),
              ) +
              const Offset(2, 2),
        );
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        expect(
          tester.widget<TextField>(find.byType(TextField)).focusNode!.hasFocus,
          true,
        );
        await tester.enterText(find.byType(TextField), 'Theme editor');
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_textId, 'Theme editor')]);
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        expect(find.byType(TextField), findsOneWidget);
        await tester.sendKeyEvent(LogicalKeyboardKey.escape);
        await tester.pump();
        expect(find.byType(TextField), findsNothing);
        expect(commits, hasLength(1));
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'IconTheme wrappers retain descendant drop targets and required child move guards on $platform',
      (tester) async {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        final model = _model(
          platform: platform,
          child: _node(
            _childId,
            'flutter.widgets.SizedBox',
            properties: _size(90, 70),
            child: _list(_columnId, []),
          ),
        );
        final wrapper = _wrapper(model);
        _centerSlot(model)['child'] = _list(_innerId, [
          wrapper,
          _node(_emptyId, 'flutter.widgets.Center'),
        ]);
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(model),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => drop = value,
            onMovePreviewResolverChanged: (value) => move = value,
          ),
        );
        await tester.pump();
        final point = tester.getCenter(_widget(_columnId));
        expect(_resolve(tester, drop!, point)?.parentWidgetId, _columnId);
        final wrap = _resolve(tester, drop!, point, _source(_type));
        expect(wrap?.parentWidgetId, _childId);
        expect(wrap?.slotName, 'child');
        expect(move!(_childId, _emptyId, 'child', 0), isNull);
        expect(move!(_childId, _id, 'child', 0), isNotNull);
        expect(move!(_id, _emptyId, 'child', 0), isNotNull);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'IconTheme zero-size child has genuine grouped selection geometry in both modes on $platform',
      (tester) async {
        for (final merge in [false, true]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {'merge': _bool(merge)},
              child: _node(
                _childId,
                'flutter.widgets.SizedBox',
                properties: _size(0, 0),
              ),
            ),
          );
          expect(tester.getSize(_widget(_id)), Size.zero);
          expect(
            find.byKey(
              const ValueKey('canvas-zero-size-widget-target-group-$_id'),
            ),
            findsOneWidget,
          );
          expect(tester.takeException(), isNull);
        }
      },
    );
  }

  testWidgets(
    'SDK IconTheme empty direct data resolves to exact fallback without mutating raw data',
    (tester) async {
      IconThemeData? raw;
      IconThemeData? resolved;
      await tester.pumpWidget(
        Directionality(
          textDirection: TextDirection.ltr,
          child: IconTheme(
            data: const IconThemeData(),
            child: Builder(
              builder: (context) {
                raw = context.getInheritedWidgetOfExactType<IconTheme>()!.data;
                resolved = IconTheme.of(context);
                return const Icon(Icons.star);
              },
            ),
          ),
        ),
      );
      expect(raw, const IconThemeData());
      expect(resolved, const IconThemeData.fallback());
      await tester.pumpWidget(
        Directionality(
          textDirection: TextDirection.ltr,
          child: Builder(
            builder: (context) {
              resolved = IconTheme.of(context);
              return const SizedBox();
            },
          ),
        ),
      );
      expect(resolved, const IconThemeData.fallback());
    },
  );

  testWidgets(
    'SDK IconTheme merge observes nearest raw data and applies parent notifications to retained child state',
    (tester) async {
      final key = GlobalKey<_ProbeState>();
      final child = _Probe(key: key);
      for (final color in [null, Colors.red, Colors.blue, null]) {
        await tester.pumpWidget(
          IconTheme(
            data: IconThemeData(color: color, size: 17),
            child: IconTheme.merge(
              data: const IconThemeData(fill: .3),
              child: child,
            ),
          ),
        );
        expect(
          key.currentState!.raw,
          IconThemeData(color: color, size: 17, fill: .3),
        );
        expect(key.currentState!.resolved!.color, color ?? Colors.black);
        expect(key.currentState!.identity, same(child));
      }
      final before = key.currentState!.builds;
      await tester.pumpWidget(
        IconTheme(
          data: const IconThemeData(size: 17),
          child: IconTheme.merge(
            data: const IconThemeData(fill: .3),
            child: child,
          ),
        ),
      );
      expect(key.currentState!.builds, before);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'SDK IconTheme capture and wrap retain unresolved data and the same direct boundary',
    (tester) async {
      CapturedThemes? captured;
      IconTheme? original;
      await tester.pumpWidget(
        IconTheme(
          data: _outer,
          child: IconTheme(
            data: const IconThemeData(fill: .2),
            child: Builder(
              builder: (context) {
                captured = InheritedTheme.capture(from: context, to: null);
                original = context.getInheritedWidgetOfExactType<IconTheme>();
                return const SizedBox();
              },
            ),
          ),
        ),
      );
      for (final capture in [false, true]) {
        IconThemeData? raw;
        IconThemeData? resolved;
        final child = Builder(
          builder: (context) {
            raw = context.getInheritedWidgetOfExactType<IconTheme>()!.data;
            resolved = IconTheme.of(context);
            return const SizedBox();
          },
        );
        await tester.pumpWidget(
          IconTheme(
            data: _outer,
            child: capture
                ? captured!.wrap(child)
                : Builder(builder: (context) => original!.wrap(context, child)),
          ),
        );
        expect(raw, const IconThemeData(fill: .2));
        expect(resolved, const IconThemeData.fallback().copyWith(fill: .2));
      }
    },
  );

  testWidgets(
    'SDK IconTheme changes in every field notify but clamped-equivalent opacity and equal shadows do not',
    (tester) async {
      final key = GlobalKey<_ProbeState>();
      final child = _Probe(key: key);
      var expected = 0;
      for (final data in [
        const IconThemeData(),
        const IconThemeData(size: 0),
        const IconThemeData(fill: 0),
        const IconThemeData(weight: 400),
        const IconThemeData(grade: 0),
        const IconThemeData(opticalSize: 48),
        const IconThemeData(color: Colors.red),
        const IconThemeData(opacity: 1),
        const IconThemeData(shadows: []),
        const IconThemeData(applyTextScaling: false),
        const IconThemeData(opacity: 5),
      ]) {
        await tester.pumpWidget(IconTheme(data: data, child: child));
        expect(key.currentState!.builds, ++expected);
      }
      await tester.pumpWidget(
        IconTheme(data: const IconThemeData(opacity: 8), child: child),
      );
      expect(key.currentState!.builds, expected);
      await tester.pumpWidget(
        IconTheme(
          data: const IconThemeData(shadows: []),
          child: child,
        ),
      );
      expect(key.currentState!.builds, ++expected);
      await tester.pumpWidget(
        IconTheme(
          data: IconThemeData(shadows: <Shadow>[]),
          child: child,
        ),
      );
      expect(key.currentState!.builds, expected);
    },
  );
}

Map<String, Object?> _all() => {
  'size': _int(19),
  'fill': _double(.2),
  'weight': _double(480),
  'grade': _double(17),
  'opticalSize': _double(28),
  'color': _color('0x8013579B'),
  'opacity': _double(.4),
  'shadows': _shadows([
    {
      ..._shadow(0),
      'color': {'kind': 'literal', 'argb': '0x80775533'},
      'offsetX': 1.0,
    },
  ]),
  'applyTextScaling': _bool(false),
};
Map<String, Object?> _outerProperties() => {
  'size': _int(31),
  'fill': _double(.7),
  'weight': _double(620),
  'grade': _double(-12),
  'opticalSize': _double(38),
  'color': _color('0xCC123456'),
  'opacity': _double(.6),
  'shadows': _shadows([_shadow(0)]),
  'applyTextScaling': _bool(true),
};
IconThemeData _data(Map<String, Object?> properties) {
  double? number(String name) =>
      ((properties[name] as Map?)?['value'] as num?)?.toDouble();
  final color = properties['color'] as Map?;
  final shadows = (properties['shadows'] as Map?)?['items'] as List?;
  return IconThemeData(
    size: number('size'),
    fill: number('fill'),
    weight: number('weight'),
    grade: number('grade'),
    opticalSize: number('opticalSize'),
    color: color == null
        ? null
        : Color(int.parse((color['argb'] as String).substring(2), radix: 16)),
    opacity: number('opacity'),
    shadows: shadows?.map((e) {
      final item = e as Map;
      return Shadow(
        color: Color(
          int.parse(
            ((item['color'] as Map)['argb'] as String).substring(2),
            radix: 16,
          ),
        ),
        offset: Offset(
          (item['offsetX'] as num).toDouble(),
          (item['offsetY'] as num).toDouble(),
        ),
        blurRadius: (item['blurRadius'] as num).toDouble(),
      );
    }).toList(),
    applyTextScaling:
        (properties['applyTextScaling'] as Map?)?['value'] as bool?,
  );
}

Color _attenuate(Color color, double opacity) => opacity == 1
    ? color
    : Color.fromARGB(
        (color.a * opacity * 255).round(),
        (color.r * 255).round(),
        (color.g * 255).round(),
        (color.b * 255).round(),
      );
void _expectIcon(WidgetTester tester, IconThemeData expected) {
  final style = _iconStyle(tester);
  expect(style.fontSize, expected.size);
  expect(style.color, _attenuate(expected.color!, expected.opacity!));
  expect(style.shadows, expected.shadows);
  expect(
    {
      for (final variation in style.fontVariations!)
        variation.axis: variation.value,
    },
    {
      'FILL': expected.fill,
      'wght': expected.weight,
      'GRAD': expected.grade,
      'opsz': expected.opticalSize,
    },
  );
}

Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _iconFinder() =>
    find.descendant(of: _widget(_iconId), matching: find.byType(Icon));
TextStyle _iconStyle(WidgetTester tester) =>
    (tester
                .widget<RichText>(
                  find.descendant(
                    of: _iconFinder(),
                    matching: find.byType(RichText),
                  ),
                )
                .text
            as TextSpan)
        .style!;
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
Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  model['root'] = _node(
    '7fbfe1d4-738c-455b-a116-5cc247a90152',
    'flutter.material.Scaffold',
  );
  (model['root'] as Map)['slots'] = {
    'body': {
      'kind': 'single',
      'child': _node(
        'f72b8d40-9826-4279-aa9c-f5fb40dc2ba2',
        'flutter.widgets.Center',
        child: _node(
          _id,
          _type,
          properties: properties,
          child: child ?? _icon(),
        ),
      ),
    },
  };
  return model;
}

Map _centerSlot(Map<String, Object?> model) =>
    (((((model['root'] as Map)['slots'] as Map)['body'] as Map)['child']
                as Map)['slots']
            as Map)['child']
        as Map;
Map<String, Object?> _wrapper(Map<String, Object?> model) =>
    _centerSlot(model)['child'] as Map<String, Object?>;
Map<String, Object?> _node(
  String id,
  String type, {
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
}) => {
  'id': id,
  'type': type,
  'properties': {if (type == _type) 'merge': _bool(false), ...properties},
  'slots': {
    'child': {'kind': 'single', 'child': child},
  },
};
Map<String, Object?> _icon({
  Map<String, Object?> properties = const {},
  bool empty = false,
}) => {
  'id': _iconId,
  'type': 'flutter.widgets.Icon',
  'properties': {
    'icon': empty
        ? {
            'kind': 'iconData',
            'codePoint': null,
            'fontFamily': null,
            'fontPackage': null,
            'matchTextDirection': false,
            'fontFamilyFallback': <String>[],
          }
        : fixture.iconDataValueForViewTest(codePoint: 0xe5f9),
    'semanticLabel': _string('Themed icon'),
    ...properties,
  },
  'slots': <String, Object?>{},
};
Map<String, Object?> _text() => {
  'id': _textId,
  'type': 'flutter.widgets.Text',
  'properties': {'data': _string('Theme child')},
  'slots': <String, Object?>{},
};
Map<String, Object?> _list(String id, List<Object?> children) => {
  'id': id,
  'type': 'flutter.widgets.Column',
  'properties': <String, Object?>{},
  'slots': {
    'children': {'kind': 'list', 'children': children},
  },
};
Map<String, Object?> _size(int width, int height) => {
  'width': _int(width),
  'height': _int(height),
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _int(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _double(double value) => {
  'kind': 'double',
  'value': value,
};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _color(String argb) => {'kind': 'color', 'argb': argb};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
Map<String, Object?> _shadows(List<Object?> items) => {
  'kind': 'shadowList',
  'items': items,
};
Map<String, Object?> _shadow(int index) => {
  'id': '09543220-7933-49a3-a858-${index.toRadixString(16).padLeft(12, '0')}',
  'color': {'kind': 'literal', 'argb': '0x88446688'},
  'offsetX': -2.0,
  'offsetY': 3.0,
  'blurRadius': 4.0,
};
CanvasNode? _find(CanvasNode node, String id) {
  if (node.id == id) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _find(child, id);
      if (found != null) return found;
    }
  }
  return null;
}

CanvasPaletteDragSource _source(String type) => CanvasPaletteDragSource(
  token: type,
  widgetType: type,
  traits: canvasWidgetTraitsForType(type),
);
CanvasDropTarget? _resolve(
  WidgetTester tester,
  CanvasDropResolver resolver,
  Offset point, [
  CanvasPaletteDragSource? source,
]) {
  final surface = tester.getRect(find.byType(CanvasDocumentView));
  return resolver(
    ((point.dx - surface.left) / surface.width * 1000000).round(),
    ((point.dy - surface.top) / surface.height * 1000000).round(),
    source,
  );
}

Set<String> _labels(WidgetTester tester) {
  final labels = <String>{};
  void visit(SemanticsNode node) {
    if (!node.isMergedIntoParent) labels.add(node.label);
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(
    tester.binding.renderViews.single.owner!.semanticsOwner!.rootSemanticsNode!,
  );
  return labels;
}

class _Probe extends StatefulWidget {
  const _Probe({super.key});
  @override
  State<_Probe> createState() => _ProbeState();
}

class _ProbeState extends State<_Probe> {
  int builds = 0;
  IconThemeData? raw;
  IconThemeData? resolved;
  late final Widget identity = widget;
  @override
  Widget build(BuildContext context) {
    builds++;
    raw = context.getInheritedWidgetOfExactType<IconTheme>()!.data;
    resolved = IconTheme.of(context);
    return const SizedBox();
  }
}
