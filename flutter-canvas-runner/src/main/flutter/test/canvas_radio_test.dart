// Tests intentionally exercise the pinned deprecated legacy constructor API.
// ignore_for_file: deprecated_member_use
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/cupertino.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

Radio _raw({
  bool adaptive = false,
  Object? value = 'option',
  Object? groupValue,
  bool callback = true,
  bool? enabled,
  bool toggleable = false,
  bool checkmark = false,
  Color? active,
  Color? focus,
  Color? hover,
  WidgetStateProperty<Color?>? fill,
  WidgetStateProperty<Color?>? overlay,
  WidgetStateProperty<Color?>? background,
  WidgetStateProperty<double?>? inner,
  BorderSide? side,
  double? splash,
  VisualDensity? density,
  MaterialTapTargetSize? target,
}) => adaptive
    ? Radio<Object?>.adaptive(
        value: value,
        groupValue: groupValue,
        onChanged: callback ? (_) {} : null,
        enabled: enabled,
        toggleable: toggleable,
        useCupertinoCheckmarkStyle: checkmark,
        activeColor: active,
        focusColor: focus,
        hoverColor: hover,
        fillColor: fill,
        overlayColor: overlay,
        backgroundColor: background,
        innerRadius: inner,
        side: side,
        splashRadius: splash,
        visualDensity: density,
        materialTapTargetSize: target,
      )
    : Radio<Object?>(
        value: value,
        groupValue: groupValue,
        onChanged: callback ? (_) {} : null,
        enabled: enabled,
        toggleable: toggleable,
        activeColor: active,
        focusColor: focus,
        hoverColor: hover,
        fillColor: fill,
        overlayColor: overlay,
        backgroundColor: background,
        innerRadius: inner,
        side: side,
        splashRadius: splash,
        visualDensity: density,
        materialTapTargetSize: target,
      );

void main() {
  _additionalTests();
  test(
    'Radio covers all107 exact scalar fields and literal generic admission',
    () {
      final properties = <String, Object?>{
        'groupValue': _string('option'),
        'onChanged': _reference,
        'mouseCursor': _reference,
        'toggleable': _bool(true),
        'activeColor': _color(0xffabcdef),
        'fillColor': _reference,
        'focusColor': _color(0xff123456),
        'hoverColor': _color(0xff345678),
        'overlayColor': _reference,
        'splashRadius': _enum('double', 'negativeInfinity'),
        'materialTapTargetSize': _enum('MaterialTapTargetSize', 'shrinkWrap'),
        'visualDensity': _reference,
        'focusNode': _reference,
        'autofocus': _bool(true),
        'useCupertinoCheckmarkStyle': _bool(true),
        'enabled': _null,
        'groupRegistry': _null,
        'backgroundColor': _reference,
        'side': _reference,
        'innerRadius': _reference,
        'nullableValueType': _bool(true),
      };
      final covered = <String>{
        ..._decodedControl(
          _model(variant: 'adaptive', properties: properties),
        ).properties.keys,
      };
      properties.remove('visualDensity');
      properties['visualDensityHorizontal'] = _number(-4.0);
      properties['visualDensityVertical'] = _number(4.0);
      for (final family in [
        'fillColor',
        'overlayColor',
        'backgroundColor',
        'innerRadius',
      ]) {
        properties.remove(family);
        for (final state in ['Default', ..._states.values]) {
          properties['$family$state'] = _null;
        }
      }
      properties.remove('side');
      properties['sideStateful'] = _bool(true);
      for (final prefix in [
        'side',
        for (final state in _states.values) 'side$state',
      ]) {
        if (prefix != 'side') properties['${prefix}Mode'] = _string('border');
        properties['${prefix}Color'] = _color(0xffabcdef);
        properties['${prefix}Width'] = _number(1);
        properties['${prefix}Style'] = _enum('BorderStyle', 'solid');
        properties['${prefix}StrokeAlign'] = _number(-1.5);
      }
      covered.addAll(
        _decodedControl(
          _model(variant: 'adaptive', properties: properties),
        ).properties.keys,
      );
      expect(covered, hasLength(107));
      for (final type in ['String', 'int', 'double', 'num', 'bool', 'Object']) {
        final value = switch (type) {
          'int' => _number(1),
          'double' || 'num' => _number(1.5),
          'bool' => _bool(false),
          _ => _string('choice'),
        };
        expect(
          _decodedControl(
            _model(properties: {'valueType': _string(type), 'value': value}),
          ),
          isNotNull,
        );
        expect(
          _decodedControl(
            _model(
              properties: {
                'valueType': _string(type),
                'value': _null,
                'nullableValueType': _bool(true),
              },
            ),
          ),
          isNotNull,
        );
      }
      expect(
        _decodedControl(
          _model(
            properties: {
              'valueType': _reference,
              'value': _reference,
              'groupValue': _reference,
            },
          ),
        ),
        isNotNull,
      );
    },
  );

  test(
    'Radio validates type literal null constructor and state-family relationships',
    () {
      for (final properties in <Map<String, Object?>>[
        {'value': _null},
        {'valueType': _string('dynamic')},
        {'valueType': _string('String?')},
        {'valueType': _string('int'), 'value': _number(1.0)},
        {'valueType': _string('bool'), 'value': _string('false')},
        {'valueType': _string('String'), 'groupValue': _number(1)},
        {'onChanged': _string('onChanged()')},
        {'enabled': _string('true')},
        {'visualDensityHorizontal': _number(1)},
        {'visualDensityVertical': _number(4.1)},
        {'visualDensity': _reference, 'visualDensityHorizontal': _number(1.0)},
        {'useCupertinoCheckmarkStyle': _bool(false)},
        {'fillColor': _reference, 'fillColorDefault': _null},
        {'overlayColor': _reference, 'overlayColorHovered': _null},
        {'backgroundColor': _reference, 'backgroundColorSelected': _null},
        {'innerRadius': _reference, 'innerRadiusDefault': _number(1)},
        {'side': _reference, 'sideStateful': _bool(false)},
        {'sidePressedMode': _string('border')},
        {
          'sideStateful': _bool(true),
          'sidePressedMode': _string('inherit'),
          'sidePressedWidth': _number(1),
        },
        {'sideWidth': _number(-1)},
        {'splashRadius': _enum('double', 'nan')},
      ]) {
        expect(
          () => _decode(_model(properties: properties)),
          throwsFormatException,
          reason: '$properties',
        );
      }
      for (final required in ['value', 'valueType', 'variant']) {
        final model = _model();
        (_control(model)['properties'] as Map).remove(required);
        expect(() => _decode(model), throwsFormatException);
      }
    },
  );

  testWidgets(
    'all twelve actual builtin generic T and T nullable constructors render',
    (tester) async {
      for (final type in ['String', 'int', 'double', 'num', 'bool', 'Object']) {
        for (final nullable in [false, true]) {
          for (final variant in ['standard', 'adaptive']) {
            final value = switch (type) {
              'int' => _number(1),
              'double' => _number(1),
              'num' => _number(1.5),
              'bool' => _bool(false),
              _ => _string('choice'),
            };
            await _pump(
              tester,
              _model(
                variant: variant,
                properties: {
                  'valueType': _string(type),
                  'value': nullable ? _null : value,
                  'nullableValueType': _bool(nullable),
                  'groupValue': _null,
                },
              ),
            );
            expect(
              _sdk(tester).runtimeType.toString(),
              'Radio<$type${nullable ? '?' : ''}>',
            );
            expect(
              _sdk(tester).value,
              nullable ? null : (type == 'double' ? 1.0 : (value['value'])),
            );
            expect(tester.takeException(), isNull);
          }
        }
      }
    },
  );

  for (final profile in ['windows', 'web']) {
    testWidgets(
      'Radio $profile preserves optional callback enabled and null registry',
      (tester) async {
        final model = _model(profile: profile);
        await _pump(tester, model);
        expect(_sdk(tester).enabled, isNull);
        expect(_sdk(tester).onChanged, isNotNull);
        final properties = _control(model)['properties'] as Map;
        properties.remove('onChanged');
        await _pump(tester, model);
        expect(_sdk(tester).onChanged, isNull);
        expect(_sdk(tester).enabled, isNull);
        properties['onChanged'] = _null;
        properties['enabled'] = _null;
        properties['groupRegistry'] = _null;
        await _pump(tester, model);
        expect(_sdk(tester).onChanged, isNull);
        expect(_sdk(tester).groupRegistry, isNull);
        properties['enabled'] = _bool(true);
        await _pump(tester, model);
        expect(_radioFinder, findsNothing);
        expect(_diagnostics(tester), contains('matching typed RadioGroup'));
        properties['onChanged'] = _reference;
        await _pump(tester, model);
        expect(_sdk(tester).onChanged, isNotNull);
        expect(_diagnostics(tester), contains('benign controlled callback'));
        properties['enabled'] = _bool(false);
        await _pump(tester, model);
        expect(_sdk(tester).onChanged, isNotNull);
        expect(
          _diagnostics(tester),
          isNot(contains('benign controlled callback')),
        );
      },
    );
  }

  testWidgets(
    'matching inherited typed RadioGroup overrides ignored legacy group and callback',
    (tester) async {
      final changed = <String?>[];
      final model = _model(
        properties: {
          'value': _string('selected'),
          'groupValue': _reference,
          'onChanged': _reference,
          'enabled': _bool(true),
        },
      );
      await _pump(
        tester,
        model,
        wrap: (child) => RadioGroup<String>(
          groupValue: 'selected',
          onChanged: changed.add,
          child: child,
        ),
      );
      expect(_radioFinder, findsOneWidget);
      expect(_sdk(tester).groupValue, isNull);
      expect(_diagnostics(tester), isNot(contains('preview')));
      final semantics = tester.ensureSemantics();
      try {
        expect(
          tester
              .getSemantics(_radioFinder)
              .getSemanticsData()
              .flagsCollection
              .isChecked,
          ui.CheckedState.isTrue,
        );
      } finally {
        semantics.dispose();
      }
      await _pump(
        tester,
        model,
        wrap: (child) => RadioGroup<Object?>(
          groupValue: 'selected',
          onChanged: (_) {},
          child: child,
        ),
      );
      expect(_radioFinder, findsNothing);
      expect(_diagnostics(tester), contains('groupValue'));
      (_control(model)['properties'] as Map)['groupRegistry'] = _reference;
      await _pump(
        tester,
        model,
        wrap: (child) => RadioGroup<String>(
          groupValue: 'selected',
          onChanged: changed.add,
          child: child,
        ),
      );
      expect(_radioFinder, findsNothing);
      expect(_diagnostics(tester), contains('groupRegistry'));
    },
  );

  testWidgets(
    'custom generic and unresolved equality retain exact model with selectable unavailable target',
    (tester) async {
      for (final fields in <Map<String, Object?>>[
        {'valueType': _reference},
        {'value': _reference},
        {'groupValue': _reference},
        {'groupRegistry': _reference},
      ]) {
        final model = _model(properties: fields),
            before = jsonEncode(_model(properties: fields)),
            selected = <String>[];
        await _pump(tester, model, selected: selected);
        expect(_radioFinder, findsNothing);
        expect(
          _diagnostics(tester),
          contains('does not invent a selected state'),
        );
        expect(jsonEncode(model), before);
        expect(_widget(_id), findsOneWidget);
      }
    },
  );

  testWidgets(
    'all 256 state map priorities preserve nullable fallback and plain selected side semantics',
    (tester) async {
      final props = <String, Object?>{
        'sideStateful': _bool(true),
        'sideColor': _color(0xff111111),
        'sideWidth': _number(1),
      };
      for (final family in [
        'fillColor',
        'overlayColor',
        'backgroundColor',
        'innerRadius',
      ]) {
        props['${family}Default'] = family == 'innerRadius'
            ? _number(2)
            : _color(0xff010101);
        for (var i = 0; i < 8; i++) {
          props['$family${_states.values.elementAt(i)}'] =
              family == 'innerRadius' ? _number(i + 3) : _color(0xff000020 + i);
        }
      }
      for (var i = 0; i < 8; i++) {
        final prefix = 'side${_states.values.elementAt(i)}';
        props['${prefix}Mode'] = _string('border');
        props['${prefix}Width'] = _number(i + 2);
      }
      await _pump(tester, _model(properties: props));
      final sdk = _sdk(tester);
      for (var mask = 0; mask < 256; mask++) {
        final states = {
          for (var i = 0; i < 8; i++)
            if (mask & (1 << i) != 0) _states.keys.elementAt(i),
        };
        final first = List.generate(
          8,
          (i) => i,
        ).where((i) => mask & (1 << i) != 0).firstOrNull;
        for (final property in [
          sdk.fillColor,
          sdk.overlayColor,
          sdk.backgroundColor,
        ]) {
          expect(
            property!.resolve(states),
            first == null ? const Color(0xff010101) : Color(0xff000020 + first),
          );
        }
        expect(sdk.innerRadius!.resolve(states), first == null ? 2 : first + 3);
        expect(
          WidgetStateProperty.resolveAs<BorderSide?>(sdk.side, states)!.width,
          first == null ? 1 : first + 2,
        );
      }
      for (final family in [
        'fillColor',
        'overlayColor',
        'backgroundColor',
        'innerRadius',
      ]) {
        props['${family}Disabled'] = _null;
      }
      props['sideDisabledMode'] = _string('inherit');
      props.remove('sideDisabledWidth');
      await _pump(tester, _model(properties: props));
      final disabled = {
        WidgetState.disabled,
        WidgetState.selected,
        WidgetState.pressed,
      };
      expect(_sdk(tester).fillColor!.resolve(disabled), isNull);
      expect(_sdk(tester).innerRadius!.resolve(disabled), isNull);
      expect(
        WidgetStateProperty.resolveAs<BorderSide?>(_sdk(tester).side, disabled),
        isNull,
      );
    },
  );
}

void _additionalTests() {
  testWidgets(
    'nonfinite numeric identity uses exact SDK equality including NaN never selected',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        for (final type in ['double', 'num', 'Object']) {
          for (final variant in ['standard', 'adaptive']) {
            for (final apple in [false, true]) {
              for (final member in ['infinity', 'negativeInfinity', 'nan']) {
                final model = _model(
                  variant: variant,
                  properties: {
                    'valueType': _string(type),
                    'value': _enum('double', member),
                    'groupValue': _enum('double', member),
                  },
                );
                final before = jsonEncode(model);
                await _pump(
                  tester,
                  model,
                  theme: ThemeData(
                    platform: apple
                        ? TargetPlatform.iOS
                        : TargetPlatform.windows,
                  ),
                );
                final value = _sdk(tester).value as double;
                expect(value.isNaN, member == 'nan');
                if (member != 'nan') {
                  expect(
                    value,
                    member == 'infinity'
                        ? double.infinity
                        : double.negativeInfinity,
                  );
                }
                expect(
                  tester
                      .getSemantics(_radioFinder)
                      .getSemanticsData()
                      .flagsCollection
                      .isChecked,
                  member == 'nan'
                      ? ui.CheckedState.isFalse
                      : ui.CheckedState.isTrue,
                );
                expect(jsonEncode(model), before);
                expect(tester.takeException(), isNull);
              }
            }
          }
        }
        for (final type in ['String', 'int', 'bool']) {
          for (final field in ['value', 'groupValue']) {
            final model = _model(
              properties: {
                'valueType': _string(type),
                'value': type == 'bool'
                    ? _bool(false)
                    : type == 'int'
                    ? _number(1)
                    : _string('option'),
                field: _enum('double', 'nan'),
              },
            );
            expect(() => _decode(model), throwsFormatException);
          }
        }
        for (final field in ['splashRadius', 'innerRadiusDefault']) {
          expect(
            () => _decode(_model(properties: {field: _enum('double', 'nan')})),
            throwsFormatException,
          );
        }
      } finally {
        semantics.dispose();
      }
    },
  );

  test(
    'Radio runtime 108 records match independent reviewed constructor capability',
    () {
      String section(String text) => text.substring(
        text.indexOf('W|flutter.material.Radio\n'),
        text.indexOf('W|', text.indexOf('W|flutter.material.Radio\n') + 2),
      );
      final actual = section(canvasRuntimeWidgetSchemaContractForTesting());
      expect(actual, section(canvasReviewedWidgetSchemaContract));
      expect(actual.trim().split('\n'), hasLength(108));
    },
  );

  testWidgets(
    'semantic color tokens resolve current theme on every retained Radio edit',
    (tester) async {
      final properties = <String, Object?>{
        for (final key in [
          'activeColor',
          'focusColor',
          'hoverColor',
          'fillColorDefault',
          'overlayColorDefault',
          'backgroundColorDefault',
          'sideColor',
        ])
          key: {'kind': 'themeToken', 'token': 'material.colorScheme.primary'},
        'groupValue': _string('option'),
      };
      final model = _model(properties: properties);
      State? retained;
      for (final color in [const Color(0xff164b83), const Color(0xffc36824)]) {
        final theme = ThemeData(
          colorScheme: ColorScheme.fromSeed(
            seedColor: color,
          ).copyWith(primary: color),
        );
        await _pump(tester, model, theme: theme);
        retained ??= tester.state(_radioFinder);
        expect(tester.state(_radioFinder), same(retained));
        expect(_sdk(tester).activeColor, color);
        expect(_sdk(tester).focusColor, color);
        expect(_sdk(tester).hoverColor, color);
        expect(_sdk(tester).fillColor.resolve({WidgetState.selected}), color);
        expect(_sdk(tester).overlayColor.resolve({WidgetState.hovered}), color);
        expect(
          _sdk(tester).backgroundColor.resolve({WidgetState.selected}),
          color,
        );
        expect(_sdk(tester).side.color, color);
      }
    },
  );

  testWidgets(
    '41 cursor presets and stateful disabled cursor use actual material and Cupertino resolution',
    (tester) async {
      const presets = [
        'defer',
        'uncontrolled',
        'clickable',
        'adaptiveClickable',
        'textable',
        'none',
        'basic',
        'click',
        'forbidden',
        'wait',
        'progress',
        'contextMenu',
        'help',
        'text',
        'verticalText',
        'cell',
        'precise',
        'move',
        'grab',
        'grabbing',
        'noDrop',
        'alias',
        'copy',
        'disappearing',
        'allScroll',
        'resizeLeftRight',
        'resizeUpDown',
        'resizeUpLeftDownRight',
        'resizeUpRightDownLeft',
        'resizeUp',
        'resizeDown',
        'resizeLeft',
        'resizeRight',
        'resizeUpLeft',
        'resizeUpRight',
        'resizeDownLeft',
        'resizeDownRight',
        'resizeColumn',
        'resizeRow',
        'zoomIn',
        'zoomOut',
      ];
      expect(presets, hasLength(41));
      for (final apple in [false, true]) {
        final theme = ThemeData(
          platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
        );
        for (final preset in presets) {
          await _pump(
            tester,
            _model(
              variant: 'adaptive',
              properties: {'mouseCursor': _string(preset)},
            ),
            theme: theme,
          );
          expect(_sdk(tester).mouseCursor, isA<MouseCursor>());
          if (apple) {
            expect(
              tester
                  .widget<CupertinoRadio<String>>(
                    find.byType(CupertinoRadio<String>),
                  )
                  .mouseCursor,
              same(_sdk(tester).mouseCursor),
            );
          }
        }
        for (final disabled in [false, true]) {
          await _pump(
            tester,
            _model(
              variant: 'adaptive',
              properties: {
                'mouseCursor': _string('clickable'),
                'enabled': _bool(!disabled),
              },
            ),
            theme: theme,
          );
          final regions = tester.widgetList<MouseRegion>(
            find.descendant(
              of: _radioFinder,
              matching: find.byType(MouseRegion),
            ),
          );
          expect(
            regions.any(
              (r) =>
                  r.cursor ==
                  (disabled
                      ? SystemMouseCursors.basic
                      : SystemMouseCursors.click),
            ),
            isTrue,
          );
        }
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'intrinsic ancestor delegates SDK Radio size and theme updates without stale state',
    (tester) async {
      for (final adaptive in [false, true]) {
        final theme = ThemeData(
          platform: adaptive ? TargetPlatform.iOS : TargetPlatform.windows,
        );
        final model = _model(variant: adaptive ? 'adaptive' : 'standard');
        final center = (model['root'] as Map)['slots']['body']['child'] as Map;
        center['slots']['child'] = _single(
          _node(
            'dfb24f35-bb51-4b87-a3ce-4d8ed08708f8',
            'flutter.widgets.IntrinsicWidth',
            {},
            {
              'child': _single(
                _node(
                  'a0230f92-b87c-4d1b-bf80-8d0c1b723c79',
                  'flutter.widgets.IntrinsicHeight',
                  {},
                  {'child': center['slots']['child']},
                ),
              ),
            },
          ),
        );
        await _pump(tester, model, theme: theme);
        final state = tester.state(_radioFinder),
            size = tester.getSize(_radioFinder);
        await _pump(
          tester,
          model,
          theme: theme.copyWith(
            radioTheme: const RadioThemeData(
              visualDensity: VisualDensity.compact,
            ),
          ),
        );
        expect(tester.state(_radioFinder), same(state));
        await _pump(tester, model, theme: theme);
        expect(tester.getSize(_radioFinder), size);
        await _expectRawPixels(tester, _raw(adaptive: adaptive), theme);
      }
    },
  );

  testWidgets(
    'nullable selected value and toggleable group callback retain SDK checked semantics',
    (tester) async {
      final changes = <String?>[];
      final model = _model(
        properties: {
          'nullableValueType': _bool(true),
          'value': _null,
          'toggleable': _bool(true),
          'groupValue': _string('ignored'),
        },
      );
      await _pump(
        tester,
        model,
        wrap: (child) => RadioGroup<String?>(
          groupValue: null,
          onChanged: changes.add,
          child: child,
        ),
      );
      final semantics = tester.ensureSemantics();
      final data = tester.getSemantics(_radioFinder).getSemanticsData();
      expect(data.flagsCollection.isChecked, ui.CheckedState.isTrue);
      expect(data.flagsCollection.isInMutuallyExclusiveGroup, isTrue);
      await tester.tap(_radioFinder);
      await tester.pumpAndSettle();
      expect(changes, [null]);
      expect((_control(model)['properties'] as Map)['value'], _null);
      semantics.dispose();
    },
  );

  testWidgets(
    'controlled hover focus pressed and TickerMode transitions retain real SDK state',
    (tester) async {
      final old = FocusManager.instance.highlightStrategy;
      FocusManager.instance.highlightStrategy =
          FocusHighlightStrategy.alwaysTraditional;
      addTearDown(() => FocusManager.instance.highlightStrategy = old);
      final model = _model(
        properties: {
          'autofocus': _bool(true),
          'fillColorFocused': _color(0xff804020),
          'overlayColorHovered': _color(0x40208040),
          'overlayColorPressed': _color(0x40602080),
          'sideStateful': _bool(true),
          'sidePressedWidth': _number(3),
        },
      );
      await _pump(tester, model);
      final state = tester.state(_radioFinder), encoded = jsonEncode(model);
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer(location: Offset.zero);
      await mouse.moveTo(tester.getCenter(_radioFinder));
      await tester.pump(const Duration(milliseconds: 200));
      await mouse.down(tester.getCenter(_radioFinder));
      await tester.pump(const Duration(milliseconds: 25));
      expect(tester.state(_radioFinder), same(state));
      await mouse.up();
      await mouse.removePointer();
      await _pump(
        tester,
        model,
        wrap: (child) => TickerMode(enabled: false, child: child),
      );
      final paused = tester.state(_radioFinder);
      (_control(model)['properties'] as Map)['groupValue'] = _string('option');
      await _pump(
        tester,
        model,
        wrap: (child) => TickerMode(enabled: false, child: child),
      );
      expect(tester.state(_radioFinder), same(paused));
      (_control(model)['properties'] as Map).remove('groupValue');
      expect(jsonEncode(model), encoded);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    '48 raw SDK pixel controls cover M2 M3 adaptive theme selection and RTL',
    (tester) async {
      var comparisons = 0;
      for (final m3 in [false, true]) {
        for (final platform in [
          TargetPlatform.windows,
          TargetPlatform.iOS,
          TargetPlatform.macOS,
        ]) {
          for (final adaptive in [false, true]) {
            for (final selected in [false, true]) {
              for (final disabled in [false, true]) {
                final theme = ThemeData(
                  useMaterial3: m3,
                  platform: platform,
                  brightness: selected ? Brightness.dark : Brightness.light,
                );
                final properties = <String, Object?>{
                  'groupValue': selected ? _string('option') : _null,
                  'enabled': _bool(!disabled),
                  'activeColor': _color(0xff123456),
                  'focusColor': _color(0xff345678),
                  'hoverColor': _color(0xffabcdef),
                  'toggleable': _bool(true),
                  if (adaptive) 'useCupertinoCheckmarkStyle': _bool(disabled),
                  'fillColorDefault': _color(0xffa02080),
                  'fillColorSelected': _null,
                  'backgroundColorDefault': _color(0xff102030),
                  'overlayColorDefault': _color(0x40112233),
                  'innerRadiusDefault': _number(3),
                  'sideColor': _color(0xff308050),
                  'sideWidth': _number(1.5),
                  'sideStrokeAlign': _number(.5),
                  'visualDensityHorizontal': _number(-1.0),
                  'materialTapTargetSize': _enum(
                    'MaterialTapTargetSize',
                    'shrinkWrap',
                  ),
                  'splashRadius': _number(-4),
                };
                await _pump(
                  tester,
                  _model(
                    variant: adaptive ? 'adaptive' : 'standard',
                    properties: properties,
                  ),
                  theme: theme,
                  wrap: (child) => Directionality(
                    textDirection: disabled
                        ? TextDirection.rtl
                        : TextDirection.ltr,
                    child: child,
                  ),
                );
                await _expectRawPixels(
                  tester,
                  _raw(
                    adaptive: adaptive,
                    groupValue: selected ? 'option' : null,
                    enabled: !disabled,
                    toggleable: true,
                    checkmark: disabled,
                    active: const Color(0xff123456),
                    focus: const Color(0xff345678),
                    hover: const Color(0xffabcdef),
                    fill: WidgetStateProperty<Color?>.fromMap({
                      WidgetState.selected: null,
                      WidgetState.any: const Color(0xffa02080),
                    }),
                    overlay: const WidgetStatePropertyAll(Color(0x40112233)),
                    background: const WidgetStatePropertyAll(Color(0xff102030)),
                    inner: const WidgetStatePropertyAll(3),
                    side: const BorderSide(
                      color: Color(0xff308050),
                      width: 1.5,
                      strokeAlign: .5,
                    ),
                    density: const VisualDensity(horizontal: -1),
                    target: MaterialTapTargetSize.shrinkWrap,
                    splash: -4,
                  ),
                  theme,
                );
                comparisons++;
              }
            }
          }
        }
      }
      expect(comparisons, 48);
    },
  );

  testWidgets(
    'typed inherited groups match all twelve builtins without erasing nullable T',
    (tester) async {
      for (final type in ['String', 'int', 'double', 'num', 'bool', 'Object']) {
        for (final nullable in [false, true]) {
          final Object selectedValue = switch (type) {
            'String' => 'option',
            'bool' => true,
            'double' => 2.5,
            'Object' => 'option',
            _ => 2,
          };
          final value = selectedValue is String
              ? _string(selectedValue)
              : selectedValue is bool
              ? _bool(selectedValue)
              : _number(selectedValue as num);
          final properties = <String, Object?>{
            'valueType': _string(type),
            'nullableValueType': _bool(nullable),
            'value': nullable ? _null : value,
            'groupValue': _reference,
            'onChanged': _reference,
            'enabled': _bool(true),
          };
          final changes = <Object?>[];
          await _pump(
            tester,
            _model(properties: properties),
            wrap: (child) => _typedGroup(
              type,
              nullable,
              nullable ? null : selectedValue,
              changes,
              child,
            ),
          );
          expect(_radioFinder, findsOneWidget);
          expect(_diagnostics(tester), isNot(contains('preview')));
          final handle = tester.ensureSemantics();
          expect(
            tester
                .getSemantics(_radioFinder)
                .getSemanticsData()
                .flagsCollection
                .isChecked,
            ui.CheckedState.isTrue,
          );
          handle.dispose();
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  testWidgets(
    'literal equality preserves numeric equality booleans whitespace and portable integers',
    (tester) async {
      final handle = tester.ensureSemantics();
      try {
        for (final (value, group, selected)
            in <(Map<String, Object?>, Map<String, Object?>, bool)>[
              (_number(1), _number(1.0), true),
              (_bool(true), _number(1), false),
              (_string(''), _string(' '), false),
              (_string('  a\n'), _string('  a\n'), true),
              (_number(9007199254740991), _number(9007199254740991), true),
              (_number(-9007199254740991), _number(9007199254740991), false),
            ]) {
          await _pump(
            tester,
            _model(
              properties: {
                'valueType': _string('Object'),
                'value': value,
                'groupValue': group,
              },
            ),
          );
          expect(
            tester
                .getSemantics(_radioFinder)
                .getSemanticsData()
                .flagsCollection
                .isChecked,
            selected ? ui.CheckedState.isTrue : ui.CheckedState.isFalse,
          );
        }
      } finally {
        handle.dispose();
      }
    },
  );

  testWidgets(
    'SDK controlled tap space and right pointer select without changing stored value',
    (tester) async {
      final model = _model(
        properties: {
          'groupValue': _string('option'),
          'toggleable': _bool(true),
          'autofocus': _bool(true),
        },
      );
      final encoded = jsonEncode(model), selected = <String>[];
      await _pump(tester, model, selected: selected);
      final sdk = tester.widget<Radio<String>>(_radioFinder);
      sdk.onChanged!(null);
      await tester.tap(_radioFinder);
      await tester.sendKeyEvent(LogicalKeyboardKey.space);
      final right = await tester.startGesture(
        tester.getCenter(_radioFinder),
        kind: PointerDeviceKind.mouse,
        buttons: kSecondaryMouseButton,
      );
      await right.up();
      await tester.pumpAndSettle();
      expect(selected, contains(_id));
      expect(jsonEncode(model), encoded);
      expect(_sdk(tester).groupValue, 'option');
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'state and private focus survive model history reference and constructor edits',
    (tester) async {
      final model = _model(properties: {'autofocus': _bool(true)});
      await _pump(tester, model);
      final state = tester.state(_radioFinder);
      final focus = FocusManager.instance.primaryFocus;
      final props = _control(model)['properties'] as Map;
      for (final variant in ['adaptive', 'standard', 'adaptive']) {
        props['variant'] = _string(variant);
        props['groupValue'] = _string('option');
        props['focusNode'] = _reference;
        props['fillColor'] = _reference;
        await _pump(
          tester,
          model,
          theme: ThemeData(
            platform: variant == 'adaptive'
                ? TargetPlatform.iOS
                : TargetPlatform.windows,
          ),
        );
        expect(tester.state(_radioFinder), same(state));
        expect(_diagnostics(tester), contains('isolated local focus'));
        props.remove('focusNode');
        props.remove('fillColor');
        props.remove('groupValue');
        await _pump(tester, model);
        expect(tester.state(_radioFinder), same(state));
      }
      expect(FocusManager.instance.primaryFocus, same(focus));
      props['valueType'] = _string('int');
      props['value'] = _number(1);
      await _pump(tester, model);
      expect(_sdk(tester).runtimeType.toString(), 'Radio<int>');
      expect(tester.state(_radioFinder), isNot(same(state)));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'adaptive Apple ignores unsupported visual references but forwards cursor and focus',
    (tester) async {
      final fields = <String, Object?>{
        for (final key in [
          'fillColor',
          'overlayColor',
          'backgroundColor',
          'innerRadius',
          'side',
          'visualDensity',
        ])
          key: _reference,
        'useCupertinoCheckmarkStyle': _bool(true),
      };
      final theme = ThemeData(platform: TargetPlatform.iOS);
      await _pump(
        tester,
        _model(variant: 'adaptive', properties: fields),
        theme: theme,
      );
      expect(
        find.byWidgetPredicate((w) => w is CupertinoRadio<String>),
        findsOneWidget,
      );
      expect(_diagnostics(tester), isNot(contains('preview')));
      fields['mouseCursor'] = _reference;
      fields['focusNode'] = _reference;
      await _pump(
        tester,
        _model(variant: 'adaptive', properties: fields),
        theme: theme,
      );
      expect(_diagnostics(tester), contains('mouseCursor, focusNode'));
      expect(_diagnostics(tester), isNot(contains('fillColor,')));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'local density missing peer zero overrides nonzero RadioTheme density',
    (tester) async {
      for (final m3 in [false, true]) {
        final theme = ThemeData(
          useMaterial3: m3,
          visualDensity: const VisualDensity(horizontal: -2, vertical: 3),
          radioTheme: const RadioThemeData(
            visualDensity: VisualDensity(horizontal: -3, vertical: -2),
          ),
        );
        await _pump(
          tester,
          _model(properties: {'visualDensityHorizontal': _number(1.0)}),
          theme: theme,
        );
        expect(_sdk(tester).visualDensity, const VisualDensity(horizontal: 1));
        await _expectRawPixels(
          tester,
          _raw(density: const VisualDensity(horizontal: 1)),
          theme,
        );
      }
    },
  );

  testWidgets(
    'plain side selected omission and stateful pressed resolution delegate exact SDK paint',
    (tester) async {
      for (final selected in [false, true]) {
        final theme = ThemeData();
        await _pump(
          tester,
          _model(
            properties: {
              'groupValue': selected ? _string('option') : _null,
              'sideStateful': _bool(true),
              'sideColor': _color(0xff0000ff),
              'sideWidth': _number(1),
              'sidePressedMode': _string('border'),
              'sidePressedColor': _color(0xffff0000),
              'sidePressedWidth': _number(3),
              'innerRadiusDefault': _number(2),
              'innerRadiusSelected': _number(3),
              'innerRadiusPressed': _number(5),
            },
          ),
          theme: theme,
        );
        await _expectRawPixels(
          tester,
          _raw(
            groupValue: selected ? 'option' : null,
            side: WidgetStateBorderSide.fromMap({
              WidgetState.pressed: const BorderSide(
                color: Color(0xffff0000),
                width: 3,
              ),
              WidgetState.any: const BorderSide(color: Color(0xff0000ff)),
            }),
            inner: WidgetStateProperty<double?>.fromMap({
              WidgetState.pressed: 5,
              WidgetState.selected: 3,
              WidgetState.any: 2,
            }),
          ),
          theme,
        );
      }
    },
  );

  testWidgets(
    'null local map entry stops fallback then uses actual RadioTheme',
    (tester) async {
      final theme = ThemeData(
        radioTheme: const RadioThemeData(
          fillColor: WidgetStatePropertyAll(Color(0xff103050)),
          backgroundColor: WidgetStatePropertyAll(Color(0xff905030)),
          innerRadius: WidgetStatePropertyAll(6),
          side: BorderSide(color: Color(0xffb06020), width: 4),
        ),
      );
      final props = <String, Object?>{
        'enabled': _bool(false),
        'groupValue': _string('option'),
        'sideStateful': _bool(true),
        'sideColor': _color(0xffffffff),
        'sideDisabledMode': _string('inherit'),
      };
      for (final family in ['fillColor', 'backgroundColor', 'innerRadius']) {
        props['${family}Default'] = family == 'innerRadius'
            ? _number(1)
            : _color(0xffffffff);
        props['${family}Disabled'] = _null;
      }
      await _pump(tester, _model(properties: props), theme: theme);
      await _expectRawPixels(
        tester,
        _raw(
          enabled: false,
          groupValue: 'option',
          fill: WidgetStateProperty<Color?>.fromMap({
            WidgetState.disabled: null,
            WidgetState.any: Colors.white,
          }),
          background: WidgetStateProperty<Color?>.fromMap({
            WidgetState.disabled: null,
            WidgetState.any: Colors.white,
          }),
          inner: WidgetStateProperty<double?>.fromMap({
            WidgetState.disabled: null,
            WidgetState.any: 1,
          }),
          side: WidgetStateBorderSide.fromMap({
            WidgetState.disabled: null,
            WidgetState.any: const BorderSide(color: Colors.white),
          }),
        ),
        theme,
      );
    },
  );

  testWidgets(
    'all signed infinity radius buckets remain exact through Windows and Web payloads',
    (tester) async {
      for (final profile in ['windows', 'web']) {
        for (final value in [
          _enum('double', 'negativeInfinity'),
          _number(-2),
          _number(0),
          _enum('double', 'infinity'),
        ]) {
          final model = _model(
            profile: profile,
            properties: {
              'groupValue': _string('option'),
              'splashRadius': value,
              for (final suffix in ['Default', ..._states.values])
                'innerRadius$suffix': value,
            },
          );
          await _pump(tester, model);
          final radius = value['kind'] == 'enum'
              ? (value['value'] == 'infinity'
                    ? double.infinity
                    : double.negativeInfinity)
              : value['value'];
          expect(_sdk(tester).splashRadius, radius);
          expect(
            _sdk(
              tester,
            ).innerRadius.resolve({WidgetState.selected, WidgetState.pressed}),
            radius,
          );
          await tester.tap(_radioFinder);
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  testWidgets(
    'multiple radios retain independent state and isolated focus ownership',
    (tester) async {
      final model = _twoRadios();
      await _pump(tester, model);
      final states = tester.stateList(_radioFinder).toList();
      expect(states, hasLength(2));
      expect(states[0], isNot(same(states[1])));
      final nodes = _radioNodes(model);
      (nodes.first['properties'] as Map)['focusNode'] = _reference;
      (nodes.last['properties'] as Map)['onChanged'] = _reference;
      await _pump(tester, model);
      expect(tester.stateList(_radioFinder), orderedEquals(states));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'inherited RadioGroup keyboard arrows and space use actual group callback not stored legacy callback',
    (tester) async {
      final model = _twoRadios(), changes = <String?>[], selected = <String>[];
      final nodes = _radioNodes(model);
      for (final node in nodes) {
        (node['properties'] as Map)['onChanged'] = _reference;
        (node['properties'] as Map)['groupValue'] = _reference;
      }
      (nodes.first['properties'] as Map)['autofocus'] = _bool(true);
      await _pump(
        tester,
        model,
        selected: selected,
        wrap: (child) => RadioGroup<String>(
          groupValue: 'first',
          onChanged: changes.add,
          child: child,
        ),
      );
      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
      await tester.pump();
      expect(changes, contains('second'));
      await tester.sendKeyEvent(LogicalKeyboardKey.space);
      await tester.pump();
      expect(changes.length, greaterThanOrEqualTo(2));
      expect(_diagnostics(tester), isNot(contains('project callback')));
      expect((nodes.first['properties'] as Map)['groupValue'], _reference);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'intrinsic and zero constrained geometry keep Radio editable without invented dimensions',
    (tester) async {
      for (final size in [0.0, 4.0, 48.0]) {
        final model = _model();
        final scaffold = model['root'] as Map;
        final center = scaffold['slots']['body']['child'] as Map;
        center['slots']['child'] = _single(
          _node(
            'e839df91-7c50-42bf-93c8-6bd1866c99d0',
            'flutter.widgets.SizedBox',
            {'width': _number(size), 'height': _number(size)},
            {'child': center['slots']['child']},
          ),
        );
        await _pump(tester, model);
        expect(_radioFinder, findsOneWidget);
        expect(_widget(_id), findsOneWidget);
        expect(tester.takeException(), isNull);
      }
      await _pump(
        tester,
        _model(),
        wrap: (child) => TickerMode(enabled: false, child: child),
      );
      expect(_radioFinder, findsOneWidget);
    },
  );
}

Widget _group<T>(Object? selected, List<Object?> changes, Widget child) =>
    RadioGroup<T>(
      groupValue: selected as T?,
      onChanged: changes.add,
      child: child,
    );
Widget _typedGroup(
  String type,
  bool nullable,
  Object? value,
  List<Object?> changes,
  Widget child,
) => switch (type) {
  'String' =>
    nullable
        ? _group<String?>(value, changes, child)
        : _group<String>(value, changes, child),
  'int' =>
    nullable
        ? _group<int?>(value, changes, child)
        : _group<int>(value, changes, child),
  'double' =>
    nullable
        ? _group<double?>(value, changes, child)
        : _group<double>(value, changes, child),
  'num' =>
    nullable
        ? _group<num?>(value, changes, child)
        : _group<num>(value, changes, child),
  'bool' =>
    nullable
        ? _group<bool?>(value, changes, child)
        : _group<bool>(value, changes, child),
  _ =>
    nullable
        ? _group<Object?>(value, changes, child)
        : _group<Object>(value, changes, child),
};
Map<String, Object?> _twoRadios() {
  final model = _model(),
      boundary =
          (model['root']
                  as Map)['slots']['body']['child']['slots']['child']['child']
              as Map;
  boundary['slots']['child'] = _single(
    _node(
      'edf41fdc-57f9-48a1-a84c-0e76d08b9532',
      'flutter.widgets.Column',
      {},
      {
        'children': {
          'kind': 'list',
          'children': [
            _node(_id, _type, {
              'value': _string('first'),
              'valueType': _string('String'),
              'variant': _string('standard'),
              'onChanged': _string('noop'),
            }),
            _node('b7448319-23ed-4916-a21c-bba23834f22f', _type, {
              'value': _string('second'),
              'valueType': _string('String'),
              'variant': _string('standard'),
              'onChanged': _string('noop'),
            }),
          ],
        },
      },
    ),
  );
  return model;
}

List _radioNodes(Map model) =>
    model['root']['slots']['body']['child']['slots']['child']['child']['slots']['child']['child']['slots']['children']['children']
        as List;

const _type = 'flutter.material.Radio';
const _id = 'e166c0e6-a533-41f3-acf8-f868f2659c13';
const _boundaryId = '658c0969-15b7-47e1-87b3-3e721cf203f1';
const _reference = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
const _states = <WidgetState, String>{
  WidgetState.disabled: 'Disabled',
  WidgetState.error: 'Error',
  WidgetState.dragged: 'Dragged',
  WidgetState.pressed: 'Pressed',
  WidgetState.selected: 'Selected',
  WidgetState.scrolledUnder: 'ScrolledUnder',
  WidgetState.hovered: 'Hovered',
  WidgetState.focused: 'Focused',
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _number(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _color(int value) => {
  'kind': 'color',
  'argb': '0x${value.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
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
  String profile = 'windows',
  String variant = 'standard',
  Map<String, Object?> properties = const {},
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = profile;
  model['root'] = _node(
    '8798daa7-a409-45d6-9ee5-f8bc0686ed26',
    'flutter.material.Scaffold',
    {},
    {
      'body': _single(
        _node(
          'ef4c8288-b304-4e4c-89e2-d1ff8d31dcaa',
          'flutter.widgets.Center',
          {},
          {
            'child': _single(
              _node(_boundaryId, 'flutter.widgets.RepaintBoundary', {}, {
                'child': _single(
                  _node(_id, _type, {
                    'value': _string('option'),
                    'valueType': _string('String'),
                    'variant': _string(variant),
                    'onChanged': _string('noop'),
                    ...properties,
                  }),
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

Map _control(Map model) =>
    model['root']['slots']['body']['child']['slots']['child']['child']['slots']['child']['child']
        as Map;
CanvasModel _decode(Map model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode _decodedControl(Map model) => _decode(
  model,
).root.slots['body']!.child!.slots['child']!.child!.slots['child']!.child!;
dynamic _sdk(WidgetTester tester) => tester.widget<Radio>(_radioFinder);
final _radioFinder = find.byWidgetPredicate((widget) => widget is Radio);
String _diagnostics(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((v) => v.message ?? '')
    .join('\n');
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  Widget Function(Widget)? wrap,
}) async {
  final view = CanvasDocumentView(
    model: _decode(model),
    selectedWidgetId: null,
    onSelected: selected?.add ?? (_) {},
  );
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: wrap?.call(view) ?? view,
    ),
  );
  await tester.pump(const Duration(milliseconds: 350));
  await tester.pump();
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
Future<void> _expectRawPixels(
  WidgetTester tester,
  Radio raw,
  ThemeData theme,
) async {
  tester
          .renderObject<RenderCustomPaint>(
            find.byKey(const ValueKey('canvas-widget-outline-$_id')),
          )
          .foregroundPainter =
      null;
  await tester.pump();
  final boundary = tester.renderObject<RenderRepaintBoundary>(
    find
        .descendant(
          of: _widget(_boundaryId),
          matching: find.byType(RepaintBoundary),
        )
        .first,
  );
  final pixels = await _pixels(tester, boundary);
  final size = tester.getSize(_radioFinder);
  final direction = Directionality.of(tester.element(_radioFinder));
  final key = GlobalKey();
  await tester.pumpWidget(
    MaterialApp(
      theme: theme,
      themeAnimationDuration: Duration.zero,
      home: Scaffold(
        body: Center(
          child: Directionality(
            textDirection: direction,
            child: OverflowBox(
              minWidth: 0,
              maxWidth: double.infinity,
              minHeight: 0,
              maxHeight: double.infinity,
              child: SizedBox(
                width: size.width,
                height: size.height,
                child: RepaintBoundary(key: key, child: raw),
              ),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 350));
  await tester.pump();
  expect(tester.getSize(_radioFinder), size);
  expect(
    await _pixels(
      tester,
      key.currentContext!.findRenderObject()! as RenderRepaintBoundary,
    ),
    pixels,
  );
  expect(tester.takeException(), isNull);
}
