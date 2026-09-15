// ignore_for_file: deprecated_member_use
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.RadioListTile';
const _ref = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
const _states = {
  WidgetState.disabled: 'Disabled',
  WidgetState.error: 'Error',
  WidgetState.dragged: 'Dragged',
  WidgetState.pressed: 'Pressed',
  WidgetState.selected: 'Selected',
  WidgetState.scrolledUnder: 'ScrolledUnder',
  WidgetState.hovered: 'Hovered',
  WidgetState.focused: 'Focused',
};
String _id(int n) => '837a4f83-8525-41b2-a4fb-${n.toString().padLeft(12, '0')}';
Map<String, Object?> _s(String v) => {'kind': 'string', 'value': v};
Map<String, Object?> _b(bool v) => {'kind': 'boolean', 'value': v};
Map<String, Object?> _n(num v) => {
  'kind': v is int ? 'integer' : 'double',
  'value': v,
};
Map<String, Object?> _e(String type, String v) => {
  'kind': 'enum',
  'type': type,
  'value': v,
};
Map<String, Object?> _c(int v) => {
  'kind': 'color',
  'argb': '0x${v.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _single(Object? v) => {'kind': 'single', 'child': v};
Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> props, [
  Map<String, Object?> slots = const {},
]) => {'id': _id(id), 'type': type, 'properties': props, 'slots': slots};
Map<String, Object?> _text(int id, String label) =>
    _node(id, 'flutter.widgets.Text', {'data': _s(label)});
Map<String, Object?> _tile(
  int id, {
  Map<String, Object?> props = const {},
  Map<String, Object?> slots = const {},
}) => _node(id, _type, {
  'value': _s('a'),
  'valueType': _s('String'),
  'variant': _s('standard'),
  'onChanged': _s('noop'),
  ...props,
}, slots);
Map<String, Object?> _radio(int id, String value) => _node(
  id,
  'flutter.material.Radio',
  {'value': _s(value), 'valueType': _s('String'), 'variant': _s('standard')},
);
Map<String, Object?> _column(int id, List<Object?> children) => _node(
  id,
  'flutter.widgets.Column',
  {'mainAxisSize': _e('MainAxisSize', 'min')},
  {
    'children': {'kind': 'list', 'children': children},
  },
);
Map<String, Object?> _group(
  int id,
  Object? value,
  List<Object?> children, {
  String valueType = 'String',
  bool nullable = false,
}) => _node(
  id,
  'flutter.widgets.RadioGroup',
  {
    'valueType': _s(valueType),
    'nullableValueType': _b(nullable),
    'groupValue': value,
    'onChanged': _s('noop'),
  },
  {'child': _single(_column(id + 100, children))},
);
Map<String, Object?> _model(Object? root) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  model['root'] = _node(900, 'flutter.material.Scaffold', {}, {
    'body': _single(
      _node(901, 'flutter.widgets.Center', {}, {'child': _single(root)}),
    ),
  });
  return model;
}

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _sdkTiles = find.byWidgetPredicate((w) => w is RadioListTile);
String _messages(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((w) => w.message ?? '')
    .where((s) => s.isNotEmpty)
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Object? root, {
  TargetPlatform platform = TargetPlatform.windows,
  List<String>? selected,
  bool rtl = false,
  ValueChanged<CanvasDropResolver?>? onDrop,
  ValueChanged<CanvasMovePreviewResolver?>? onMove,
}) async {
  final model = _model(root);
  (model['profile'] as Map)['targetPlatform'] = platform.name;
  await tester.pumpWidget(
    MaterialApp(
      theme: ThemeData(platform: platform),
      themeAnimationDuration: Duration.zero,
      home: Directionality(
        textDirection: rtl ? TextDirection.rtl : TextDirection.ltr,
        child: CanvasDocumentView(
          model: _decode(model),
          selectedWidgetId: null,
          onSelected: selected?.add ?? (_) {},
          onDropResolverChanged: onDrop,
          onMovePreviewResolverChanged: onMove,
        ),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 350));
  await tester.pump();
  expect(tester.takeException(), isNull);
}

RadioClient<T> _client<T>(WidgetTester tester, int id) =>
    tester.state(
          find
              .descendant(
                of: find.byKey(ValueKey('canvas-widget-${_id(id)}')),
                matching: find.byType(RadioListTile<T>),
              )
              .first,
        )
        as RadioClient<T>;

void main() {
  test(
    '153 leaves three slots and 100/4561 independent runtime contract are exact',
    () {
      final schema = canvasRuntimeWidgetSchemaContractForTesting();
      expect(schema, canvasReviewedWidgetSchemaContract.trimLeft());
      expect(
        RegExp(r'^W\|', multiLine: true).allMatches(schema),
        hasLength(226),
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(schema),
        hasLength(7495),
      );
      final start = schema.indexOf('W|$_type\n');
      final section = schema.substring(
        start,
        schema.indexOf('\nW|', start) + 1,
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(section),
        hasLength(153),
      );
      expect(
        RegExp(r'^S\|', multiLine: true).allMatches(section),
        hasLength(3),
      );
      expect(section, isNot(contains('P|groupRegistry|')));
      expect(utf8.encode(section), hasLength(96318));
      expect(
        sha256Hex(utf8.encode(section)),
        '306c07a11aaefb7dc310fa40343302af4c4e99bcf1726b7ee500d801d4518c85',
      );
      expect(canvasModelProtocolVersion, 20);
    },
  );

  testWidgets(
    'first mount registers actual tile once and SDK inner radio remains private to the tile',
    (tester) async {
      final handle = tester.ensureSemantics();
      try {
        await _pump(
          tester,
          _group(1, _s('a'), [
            _tile(2, slots: {'title': _single(_text(20, 'Choice A'))}),
          ]),
        );
        final client = _client<String>(tester, 2);
        final inner = tester.widget<Radio<String>>(find.byType(Radio<String>));
        expect(client.registry, isNotNull);
        expect(inner.groupRegistry, isNotNull);
        expect(inner.groupRegistry, isNot(same(client.registry)));
        expect(inner.groupRegistry!.groupValue, 'a');
        expect(client.radioValue, 'a');
        expect(find.byType(RadioGroup<String>), findsOneWidget);
        expect(_messages(tester), isEmpty);
        final data = tester
            .getSemantics(find.byType(Radio<String>))
            .getSemanticsData();
        expect(data.flagsCollection.isChecked, ui.CheckedState.isTrue);
        expect(data.flagsCollection.isInMutuallyExclusiveGroup, true);
        expect(data.flagsCollection.isButton, false);
        expect(data.label, contains('Choice A'));
      } finally {
        handle.dispose();
      }
    },
  );

  for (final mixed in [false, true]) {
    testWidgets(
      'first-mount duplicate selected values are quarantined before queued SDK checks mixed=$mixed',
      (tester) async {
        final handle = tester.ensureSemantics();
        try {
          await _pump(
            tester,
            _group(1, _s('a'), [_tile(2), mixed ? _radio(3, 'a') : _tile(3)]),
          );
          expect(_sdkTiles, mixed ? findsOneWidget : findsNWidgets(2));
          expect(
            _messages(tester),
            contains('group navigation preview unavailable'),
          );
          expect(_messages(tester), contains(_id(2)));
          expect(_messages(tester), contains(_id(3)));
          await _pump(
            tester,
            _group(1, _s('b'), [_tile(2), mixed ? _radio(3, 'a') : _tile(3)]),
          );
          expect(_messages(tester), isEmpty);
          await _pump(
            tester,
            _group(1, _s('a'), [
              _tile(2, props: {'enabled': _b(false)}),
              mixed ? _radio(3, 'a') : _tile(3),
            ]),
          );
          expect(
            _messages(tester),
            contains('Disabled and offstage clients count'),
          );
        } finally {
          handle.dispose();
        }
      },
    );
  }

  testWidgets(
    'mixed native Radio and RadioListTile keyboard focus traverses one client per control',
    (tester) async {
      await _pump(
        tester,
        _group(1, _s('a'), [
          _tile(2),
          _radio(3, 'b'),
          _tile(4, props: {'value': _s('c')}),
        ]),
      );
      final first = _client<String>(tester, 2);
      final last = _client<String>(tester, 4);
      first.focusNode.requestFocus();
      await tester.pump();
      expect(first.focusNode.hasFocus, true);
      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
      await tester.pump();
      expect(first.focusNode.hasFocus, false);
      expect(last.focusNode.hasFocus, false);
      final middleFocus = FocusManager.instance.primaryFocus;
      expect(middleFocus, isNotNull);
      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
      await tester.pump();
      expect(last.focusNode.hasFocus, true);
      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
      await tester.pump();
      expect(first.focusNode.hasFocus, true);
      expect(
        tester
            .widget<RadioGroup<String>>(find.byType(RadioGroup<String>))
            .groupValue,
        'a',
      );
      expect(_messages(tester), isEmpty);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'modern nonnull selection overrides legacy but modern null genuinely falls back',
    (tester) async {
      for (final group in [_s('b'), _null]) {
        await _pump(
          tester,
          _group(1, group, [
            _tile(2, props: {'groupValue': _s('a')}),
          ]),
        );
        final tile = tester.widget<RadioListTile<String>>(
          find.byType(RadioListTile<String>),
        );
        final inner = tester.widget<Radio<String>>(find.byType(Radio<String>));
        expect(tile.groupValue, 'a');
        expect(inner.groupRegistry!.groupValue, group == _null ? 'a' : 'b');
      }
      await _pump(
        tester,
        _group(1, _s('b'), [
          _tile(2, props: {'groupValue': _ref}),
        ]),
      );
      expect(_sdkTiles, findsOneWidget);
      await _pump(
        tester,
        _group(1, _null, [
          _tile(
            2,
            props: {'groupValue': _ref},
            slots: {'title': _single(_text(20, 'Retain unknown title'))},
          ),
        ]),
      );
      expect(_sdkTiles, findsNothing);
      expect(find.text('Retain unknown title'), findsOneWidget);
      expect(
        _messages(tester),
        contains('null modern group value can fall back'),
      );
    },
  );

  testWidgets(
    'same id moves across groups without leaked old native registration',
    (tester) async {
      Object tree(bool moved) => _column(10, [
        _group(1, _s('a'), [if (!moved) _tile(2) else _radio(5, 'a')]),
        _group(3, _s('b'), [if (moved) _tile(2)]),
      ]);
      await _pump(tester, tree(false));
      final old = _client<String>(tester, 2);
      final oldRegistry = old.registry;
      await _pump(tester, tree(true));
      final current = _client<String>(tester, 2);
      expect(current.registry, isNot(same(oldRegistry)));
      if (!identical(current, old)) expect(old.registry, isNull);
      expect(current.registry!.groupValue, 'b');
      expect(_messages(tester), isEmpty);
      await _pump(tester, tree(false));
      expect(_client<String>(tester, 2).registry!.groupValue, 'a');
      expect(_messages(tester), isEmpty);
    },
  );

  testWidgets(
    'modern null legacy fallback can conflict in actual assembled semantics without duplicate navigation values',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        await _pump(
          tester,
          _group(1, _null, [
            _tile(2, props: {'groupValue': _s('a')}),
            _tile(3, props: {'value': _s('b'), 'groupValue': _s('b')}),
          ]),
        );
        expect(_sdkTiles, findsNWidgets(2));
        expect(_messages(tester), contains('semantics'));
        expect(
          _messages(tester),
          isNot(contains('group navigation preview unavailable')),
        );
        await _pump(
          tester,
          _group(1, _s('a'), [
            _tile(2, props: {'groupValue': _s('a')}),
            _tile(3, props: {'value': _s('b'), 'groupValue': _s('b')}),
          ]),
        );
        expect(_messages(tester), isEmpty);
      } finally {
        semantics.dispose();
      }
    },
  );

  testWidgets(
    'numeric nonfinite scalar identity preserves SDK equality rather than geometry normalization',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        for (final type in ['double', 'num', 'Object']) {
          for (final member in ['infinity', 'negativeInfinity', 'nan']) {
            await _pump(
              tester,
              _tile(
                2,
                props: {
                  'valueType': _s(type),
                  'value': _e('double', member),
                  'groupValue': _e('double', member),
                },
              ),
            );
            final sdk = tester.widget<RadioListTile>(_sdkTiles);
            expect((sdk.value as double).isNaN, member == 'nan');
            final data = tester.getSemantics(_sdkTiles).getSemanticsData();
            expect(
              data.flagsCollection.isChecked,
              member == 'nan'
                  ? ui.CheckedState.isFalse
                  : ui.CheckedState.isTrue,
            );
            expect(_messages(tester), isEmpty);
          }
        }
      } finally {
        semantics.dispose();
      }
    },
  );

  testWidgets(
    'cached Canvas keeps duplicate quarantine through independent inherited radio and tile theme changes',
    (tester) async {
      final revision = ValueNotifier<int>(0);
      final semantics = tester.ensureSemantics();
      try {
        final cached = CanvasDocumentView(
          model: _decode(_model(_group(1, _s('a'), [_tile(2), _tile(3)]))),
          selectedWidgetId: null,
          onSelected: (_) {},
        );
        await tester.pumpWidget(
          MaterialApp(
            home: ValueListenableBuilder<int>(
              valueListenable: revision,
              child: cached,
              builder: (context, value, child) => RadioTheme(
                data: RadioThemeData(
                  fillColor: WidgetStatePropertyAll(Color(0xff123450 + value)),
                ),
                child: ListTileTheme(
                  data: ListTileThemeData(
                    horizontalTitleGap: value >= 3 ? 18 : 16,
                  ),
                  child: child!,
                ),
              ),
            ),
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
        final first = _client<String>(tester, 2);
        final second = _client<String>(tester, 3);
        for (var i = 1; i <= 5; i++) {
          revision.value = i;
          await tester.pump();
          await tester.pump(const Duration(milliseconds: 30));
          expect(tester.takeException(), isNull, reason: 'theme revision $i');
          expect(_client<String>(tester, 2), same(first));
          expect(_client<String>(tester, 3), same(second));
          final sdkRegistry = RadioGroup.maybeOf<String>(
            tester.element(find.byType(RadioListTile<String>).first),
          );
          expect(first.registry, isNot(same(sdkRegistry)));
          expect(second.registry, isNot(same(sdkRegistry)));
          expect(first.registry!.groupValue, 'a');
          expect(second.registry!.groupValue, 'a');
          expect(
            _messages(tester),
            contains('group navigation preview unavailable'),
          );
        }
      } finally {
        semantics.dispose();
        await tester.pumpWidget(const SizedBox.shrink());
        revision.dispose();
      }
    },
  );

  testWidgets(
    'nullable generic changes match only the exact typed ancestor and dispose old membership',
    (tester) async {
      await _pump(tester, _group(1, _s('a'), [_tile(2)]));
      final old = _client<String>(tester, 2);
      await _pump(
        tester,
        _group(1, _null, [
          _tile(2, props: {'value': _null, 'nullableValueType': _b(true)}),
        ], nullable: true),
      );
      expect(old.registry, isNull);
      final current = _client<String?>(tester, 2);
      expect(current.radioValue, isNull);
      expect(current.registry, isNotNull);
      expect(_messages(tester), isEmpty);
      await _pump(
        tester,
        _group(1, _s('a'), [
          _tile(
            2,
            props: {
              'nullableValueType': _b(true),
              'onChanged': _null,
              'enabled': _b(true),
            },
          ),
        ]),
      );
      expect(_sdkTiles, findsNothing);
      expect(
        _messages(tester),
        contains('no callback or matching typed group'),
      );
    },
  );

  testWidgets(
    'unsafe selection or enabled guard preserves editing children through recovery',
    (tester) async {
      final slots = {
        'title': _single(_node(20, 'flutter.material.TextField', {})),
      };
      await _pump(tester, _tile(2, slots: slots));
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      edit.widget.controller.text = 'keep';
      for (final props in <Map<String, Object?>>[
        {'value': _ref},
        {},
        {'onChanged': _null, 'enabled': _b(true)},
        {},
        {'valueType': _ref},
        {},
      ]) {
        await _pump(tester, _tile(2, props: props, slots: slots));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(edit.widget.controller.text, 'keep');
      }
    },
  );

  test(
    'strict generic, null, shape, state, cursor and slot relationships reject unsafe payloads',
    () {
      for (final props in <Map<String, Object?>>[
        {'value': _null},
        {'valueType': _s('int')},
        {'groupValue': _b(true)},
        {'variant': _s('other')},
        {
          'onChanged': {'kind': 'callbackPresence'},
        },
        {
          'onChanged': {..._ref, 'member': 'execute'},
        },
        {'groupRegistry': _ref},
        {'radioScaleFactor': _null},
        {'radioInnerRadiusDefault': _e('double', 'nan')},
        {'radioSidePressedWidth': _n(2)},
        {'radioSide': _ref, 'radioSideStateful': _b(false)},
        {'fillColor': _ref, 'fillColorDefault': _null},
        {'radioBackgroundColor': _ref, 'radioBackgroundColorDefault': _null},
        {'radioInnerRadius': _ref, 'radioInnerRadiusDefault': _null},
        {'shape': _ref, 'shapeKind': _s('circle')},
        {'visualDensity': _ref, 'visualDensityHorizontal': _n(1)},
        {'mouseCursorHovered': _s('click')},
        {'mouseCursorDefault': _null},
        {'mouseCursor': _ref, 'mouseCursorDefault': _s('click')},
        {'isThreeLine': _b(true)},
      ]) {
        expect(
          () => _decode(_model(_tile(2, props: props))),
          throwsFormatException,
          reason: '$props',
        );
      }
    },
  );

  testWidgets(
    '256 combinations of renamed color radius side and cursor state layers use exact first-match nullable semantics',
    (tester) async {
      final props = <String, Object?>{
        'radioSideStateful': _b(true),
        'radioSideColor': _c(0xff111111),
        'radioSideWidth': _n(1),
        'mouseCursorDefault': _s('basic'),
      };
      for (final family in [
        'fillColor',
        'overlayColor',
        'radioBackgroundColor',
        'radioInnerRadius',
      ]) {
        props['${family}Default'] = family == 'radioInnerRadius'
            ? _n(2)
            : _c(0xff010101);
        for (var i = 0; i < 8; i++) {
          props['$family${_states.values.elementAt(i)}'] =
              family == 'radioInnerRadius' ? _n(i + 3) : _c(0xff000020 + i);
        }
      }
      for (var i = 0; i < 8; i++) {
        final state = _states.values.elementAt(i);
        props['radioSide${state}Mode'] = _s('border');
        props['radioSide${state}Width'] = _n(i + 2);
        props['mouseCursor$state'] = _s('click');
      }
      await _pump(tester, _tile(2, props: props));
      final sdk = tester.widget<RadioListTile<String>>(_sdkTiles);
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
          sdk.radioBackgroundColor,
        ]) {
          expect(
            property!.resolve(states),
            first == null ? const Color(0xff010101) : Color(0xff000020 + first),
          );
        }
        expect(
          sdk.radioInnerRadius!.resolve(states),
          first == null ? 2 : first + 3,
        );
        expect(
          WidgetStateProperty.resolveAs<BorderSide?>(
            sdk.radioSide,
            states,
          )!.width,
          first == null ? 1 : first + 2,
        );
        expect(
          (sdk.mouseCursor as WidgetStateMouseCursor).resolve(states),
          first == null ? SystemMouseCursors.basic : SystemMouseCursors.click,
        );
      }
      for (final family in [
        'fillColor',
        'overlayColor',
        'radioBackgroundColor',
        'radioInnerRadius',
      ]) {
        props['${family}Disabled'] = _null;
      }
      props['radioSideDisabledMode'] = _s('inherit');
      props.remove('radioSideDisabledWidth');
      await _pump(tester, _tile(2, props: props));
      final updated = tester.widget<RadioListTile<String>>(_sdkTiles);
      final states = _states.keys.toSet();
      expect(updated.fillColor!.resolve(states), isNull);
      expect(updated.overlayColor!.resolve(states), isNull);
      expect(updated.radioBackgroundColor!.resolve(states), isNull);
      expect(updated.radioInnerRadius!.resolve(states), isNull);
      expect(
        WidgetStateProperty.resolveAs<BorderSide?>(updated.radioSide, states),
        isNull,
      );
    },
  );

  testWidgets(
    'all tile SDK layout arguments colors and ten local shapes preserve native finite geometry',
    (tester) async {
      final types = <Type>[];
      for (final shape in [
        'roundedRectangle',
        'beveledRectangle',
        'continuousRectangle',
        'roundedSuperellipse',
        'stadium',
        'circle',
        'oval',
        'linear',
        'star',
        'polygon',
      ]) {
        await _pump(
          tester,
          _tile(
            2,
            props: {
              'shapeKind': _s(shape),
              'groupValue': _s('a'),
              'selected': _b(false),
              'toggleable': _b(true),
              'enabled': _b(true),
              'dense': _b(true),
              'isThreeLine': _b(true),
              'enableFeedback': _b(false),
              'internalAddSemanticForOnTap': _b(false),
              'autofocus': _b(true),
              'activeColor': _c(0xff123456),
              'hoverColor': _c(0xff234567),
              'tileColor': _c(0xff345678),
              'selectedTileColor': _c(0xff456789),
              'visualDensityHorizontal': _n(-1.0),
              'visualDensityVertical': _n(1.0),
              'horizontalTitleGap': _n(12),
              'minVerticalPadding': _n(5),
              'minLeadingWidth': _n(24),
              'minTileHeight': _n(70),
              'radioScaleFactor': _n(.8),
              'splashRadius': _n(15),
              'materialTapTargetSize': _e(
                'MaterialTapTargetSize',
                'shrinkWrap',
              ),
              'titleAlignment': _e('ListTileTitleAlignment', 'center'),
              'controlAffinity': _e('ListTileControlAffinity', 'trailing'),
              'onFocusChange': _s('noop'),
            },
            slots: {
              'title': _single(_text(20, 'Title')),
              'subtitle': _single(_text(21, 'Subtitle')),
              'secondary': _single(_text(22, 'Secondary')),
            },
          ),
        );
        final sdk = tester.widget<RadioListTile<String>>(_sdkTiles);
        types.add(sdk.shape.runtimeType);
        expect(sdk.selected, false);
        expect(sdk.toggleable, true);
        expect(sdk.enabled, true);
        expect(sdk.dense, true);
        expect(sdk.activeColor, const Color(0xff123456));
        expect(sdk.hoverColor, const Color(0xff234567));
        expect(sdk.tileColor, const Color(0xff345678));
        expect(sdk.selectedTileColor, const Color(0xff456789));
        expect(
          sdk.visualDensity,
          const VisualDensity(horizontal: -1, vertical: 1),
        );
        expect(sdk.horizontalTitleGap, 12);
        expect(sdk.minVerticalPadding, 5);
        expect(sdk.minLeadingWidth, 24);
        expect(sdk.minTileHeight, 70);
        expect(sdk.radioScaleFactor, .8);
        expect(sdk.splashRadius, 15);
        expect(sdk.materialTapTargetSize, MaterialTapTargetSize.shrinkWrap);
        expect(sdk.titleAlignment, ListTileTitleAlignment.center);
        expect(sdk.onFocusChange, isNotNull);
        expect(
          tester.getCenter(find.text('Secondary')).dx,
          lessThan(tester.getCenter(find.byType(Radio<String>)).dx),
        );
        expect(tester.getSize(find.byType(ListTile)).isFinite, true);
        expect(_messages(tester).trim(), isEmpty);
      }
      expect(
        types.toSet(),
        hasLength(9),
      ); // StarBorder also provides its polygon constructor.
    },
  );

  testWidgets(
    'project appearance focus state and callback references remain anonymous inert approximations through reset',
    (tester) async {
      final selected = <String>[];
      await _pump(tester, _tile(2), selected: selected);
      final client = _client<String>(tester, 2);
      client.focusNode.requestFocus();
      await tester.pump();
      await _pump(
        tester,
        _tile(
          2,
          props: {
            for (final name in [
              'fillColor',
              'overlayColor',
              'radioBackgroundColor',
              'radioInnerRadius',
              'radioSide',
              'shape',
              'visualDensity',
              'mouseCursor',
              'contentPadding',
              'focusNode',
              'statesController',
              'activeColor',
              'hoverColor',
              'tileColor',
              'selectedTileColor',
              'onChanged',
              'onFocusChange',
            ])
              name: _ref,
          },
        ),
        selected: selected,
      );
      final sdk = tester.widget<RadioListTile<String>>(_sdkTiles);
      expect(_client<String>(tester, 2), same(client));
      expect(client.focusNode.hasFocus, true);
      expect(sdk.focusNode, isNull);
      expect(sdk.statesController, isNull);
      expect(sdk.shape, isNull);
      expect(sdk.radioSide, isNull);
      expect(sdk.radioInnerRadius, isNull);
      expect(sdk.radioBackgroundColor, isNull);
      expect(sdk.fillColor, isNull);
      expect(sdk.mouseCursor, isNull);
      expect(_messages(tester), contains('never executes project Dart'));
      sdk.onChanged!('b');
      sdk.onFocusChange!(false);
      expect(selected, contains(_id(2)));
      expect(sdk.value, 'a');
      await _pump(tester, _tile(2));
      expect(_client<String>(tester, 2), same(client));
      expect(_messages(tester), isEmpty);
    },
  );

  testWidgets(
    'optional callback omitted and explicit null infer disabled outside matching groups but retain SDK group activation',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        for (final omitted in [false, true]) {
          final tile = _tile(
            2,
            props: {'onChanged': _null},
            slots: {'title': _single(_text(20, 'Option'))},
          );
          if (omitted) (tile['properties'] as Map).remove('onChanged');
          await _pump(tester, tile);
          expect(
            tester.widget<RadioListTile<String>>(_sdkTiles).onChanged,
            isNull,
          );
          expect(tester.widget<ListTile>(find.byType(ListTile)).onTap, isNull);
          await _pump(tester, _group(1, _s('a'), [tile]));
          expect(
            tester.widget<RadioListTile<String>>(_sdkTiles).onChanged,
            isNull,
          );
          expect(
            tester.widget<ListTile>(find.byType(ListTile)).onTap,
            isNotNull,
          );
          final data = tester
              .getSemantics(find.byType(Radio<String>))
              .getSemanticsData();
          expect(data.flagsCollection.isChecked, ui.CheckedState.isTrue);
          expect(data.flagsCollection.isEnabled, ui.Tristate.isTrue);
          expect(data.flagsCollection.isButton, false);
        }
      } finally {
        semantics.dispose();
      }
    },
  );

  testWidgets(
    'signed scale and radius are preserved while nonfinite scale reaction and active tile geometry have explicit safe guards',
    (tester) async {
      final slots = {
        'title': _single(_node(20, 'flutter.material.TextField', {})),
      };
      await _pump(tester, _tile(2, slots: slots));
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      edit.widget.controller.text = 'Retained';
      for (final value in [
        _n(0),
        _n(-1),
        _n(.5),
        _e('double', 'nan'),
        _e('double', 'infinity'),
        _e('double', 'negativeInfinity'),
      ]) {
        final tile = _tile(
          2,
          props: {'radioScaleFactor': value, 'splashRadius': value},
          slots: slots,
        );
        final before = jsonEncode(tile);
        await _pump(tester, tile);
        final sdk = tester.widget<RadioListTile<String>>(_sdkTiles);
        expect(
          sdk.radioScaleFactor,
          value['kind'] == 'enum' ? 0 : value['value'],
        );
        expect(sdk.splashRadius, value['kind'] == 'enum' ? 0 : value['value']);
        expect(
          _messages(tester).contains('explicit zero approximation'),
          value['kind'] == 'enum',
        );
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(jsonEncode(tile), before);
      }
      for (final padding in [
        _e('double', 'nan'),
        _n(4),
        _e('double', 'infinity'),
        _n(5),
      ]) {
        await _pump(
          tester,
          _tile(2, props: {'minVerticalPadding': padding}, slots: slots),
        );
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(edit.widget.controller.text, 'Retained');
        expect(
          _messages(tester).contains('nonfinite'),
          padding['kind'] == 'enum',
        );
      }
      await _pump(
        tester,
        _tile(
          2,
          slots: {
            ...slots,
            'secondary': _single(
              _node(21, 'flutter.widgets.SizedBox', {
                'width': _n(1000),
                'height': _n(20),
              }),
            ),
          },
        ),
      );
      expect(_messages(tester), contains('secondary preview unavailable'));
      expect(
        tester.state<EditableTextState>(find.byType(EditableText)),
        same(edit),
      );
    },
  );

  for (final rtl in [false, true]) {
    testWidgets(
      'three empty optional single slots have distinct exact drop geometry RTL=$rtl',
      (tester) async {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        await _pump(
          tester,
          _tile(2),
          rtl: rtl,
          onDrop: (r) => drop = r,
          onMove: (r) => move = r,
        );
        expect(canvasDropSlotsForWidgetType(_type).map((s) => s.slotName), [
          'title',
          'subtitle',
          'secondary',
        ]);
        final rect = tester.getRect(find.byType(ListTile));
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final source = CanvasPaletteDragSource(
          token: 'text',
          widgetType: 'flutter.widgets.Text',
          traits: const {},
        );
        for (final entry in {
          'title': Offset(rect.center.dx, rect.top + 4),
          'subtitle': Offset(rect.center.dx, rect.bottom - 4),
          'secondary': Offset(
            rtl ? rect.left + 4 : rect.right - 4,
            rect.center.dy,
          ),
        }.entries) {
          final p = entry.value;
          final target = drop!(
            ((p.dx - surface.left) / surface.width * 1000000).round(),
            ((p.dy - surface.top) / surface.height * 1000000).round(),
            source,
          );
          expect(target?.parentWidgetId, _id(2));
          expect(target?.slotName, entry.key);
          expect(
            canvasDropTargetAcceptsSource(
              parentWidgetType: _type,
              slotName: entry.key,
              currentChildCount: 1,
              insertionIndex: 0,
              source: source,
            ),
            false,
          );
        }
        expect(move, isNotNull);
      },
    );
  }

  const values = <String, Map<String, Object?>>{
    'String': {'kind': 'string', 'value': 'a'},
    'int': {'kind': 'integer', 'value': 1},
    'double': {'kind': 'integer', 'value': 1},
    'num': {'kind': 'double', 'value': 1.5},
    'bool': {'kind': 'boolean', 'value': true},
    'Object': {'kind': 'string', 'value': 'a'},
  };
  for (final type in values.keys) {
    for (final nullable in [false, true]) {
      testWidgets(
        'real standard/adaptive generic $type nullable=$nullable preserves all three slots and controlled values',
        (tester) async {
          for (final variant in ['standard', 'adaptive']) {
            final value = nullable ? _null : values[type]!;
            await _pump(
              tester,
              _tile(
                2,
                props: {
                  'valueType': _s(type),
                  'nullableValueType': _b(nullable),
                  'value': value,
                  'groupValue': value,
                  'variant': _s(variant),
                  'useCupertinoCheckmarkStyle': _b(true),
                  'selected': _b(false),
                  'isThreeLine': _b(true),
                },
                slots: {
                  'title': _single(_text(20, 'Title')),
                  'subtitle': _single(_text(21, 'Subtitle')),
                  'secondary': _single(_text(22, 'Secondary')),
                },
              ),
              platform: TargetPlatform.iOS,
            );
            final RadioListTile tile = tester.widget(_sdkTiles);
            expect(
              tile.runtimeType.toString(),
              'RadioListTile<$type${nullable ? '?' : ''}>',
            );
            expect(tile.useCupertinoCheckmarkStyle, variant == 'adaptive');
            expect(tile.selected, false);
            expect(tile.title, isNotNull);
            expect(tile.subtitle, isNotNull);
            expect(tile.secondary, isNotNull);
            // Keep the exact T? function type rather than casting its callback.
            (tile as dynamic).onChanged(null);
            expect(tile.value, nullable ? null : values[type]!['value']);
            expect(tester.takeException(), isNull);
          }
        },
      );
    }
  }
}
