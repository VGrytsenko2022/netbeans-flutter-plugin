import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.SwitchListTile';
const _id = 'd46caa0e-e357-4443-85dc-000000000001';
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
Map<String, Object?> _bool(bool v) => {'kind': 'boolean', 'value': v};
Map<String, Object?> _str(String v) => {'kind': 'string', 'value': v};
Map<String, Object?> _num(num v) => {
  'kind': v is int ? 'integer' : 'double',
  'value': v,
};
Map<String, Object?> _enum(String type, String v) => {
  'kind': 'enum',
  'type': type,
  'value': v,
};
Map<String, Object?> _color(int v) => {
  'kind': 'color',
  'argb': '0x${v.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _single(Object? v) => {'kind': 'single', 'child': v};
Map<String, Object?> _node(
  String id,
  String type,
  Map<String, Object?> props, [
  Map<String, Object?> slots = const {},
]) => {'id': id, 'type': type, 'properties': props, 'slots': slots};
Map<String, Object?> _text(String end, String text) => _node(
  'd46caa0e-e357-4443-85dc-0000000000$end',
  'flutter.widgets.Text',
  {'data': _str(text)},
);
Map<String, Object?> _model({
  String variant = 'standard',
  Map<String, Object?> props = const {},
  Map<String, Object?> slots = const {},
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  model['root'] = _node(
    'd46caa0e-e357-4443-85dc-000000000090',
    'flutter.material.Scaffold',
    {},
    {
      'body': _single(
        _node(
          'd46caa0e-e357-4443-85dc-000000000091',
          'flutter.widgets.Center',
          {},
          {
            'child': _single(
              _node(_id, _type, {
                'value': _bool(false),
                'onChanged': _str('noop'),
                'variant': _str(variant),
                ...props,
              }, slots),
            ),
          },
        ),
      ),
    },
  );
  return model;
}

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
String _messages(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((v) => v.message ?? '')
    .join('\n');
SwitchListTile _sdk(WidgetTester tester) =>
    tester.widget<SwitchListTile>(find.byType(SwitchListTile));
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  TargetPlatform platform = TargetPlatform.windows,
  List<String>? selected,
  CanvasImageResourceBundle? resources,
  CanvasImageErrorReporter? onImageError,
  ValueChanged<CanvasDropResolver?>? onDrop,
  ValueChanged<CanvasMovePreviewResolver?>? onMove,
  bool rtl = false,
}) async {
  final candidate = jsonDecode(jsonEncode(model)) as Map<String, Object?>;
  (candidate['profile'] as Map)['targetPlatform'] = platform.name;
  await tester.pumpWidget(
    MaterialApp(
      theme: ThemeData(platform: platform),
      themeAnimationDuration: Duration.zero,
      home: Directionality(
        textDirection: rtl ? TextDirection.rtl : TextDirection.ltr,
        child: CanvasDocumentView(
          selectedWidgetId: null,
          model: _decode(candidate),
          onSelected: selected?.add ?? (_) {},
          imageResources: resources,
          onImageError: onImageError,
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

final _bytes = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=',
);
Map<String, Object?> _image({bool missing = false}) => {
  'kind': 'imageProvider',
  'value': {
    'kind': 'asset',
    'assetName': 'assets/switch.png',
    'packageName': null,
    'exactScale': null,
    'resize': null,
    'resolution': missing
        ? {
            'kind': 'unavailable',
            'code': 'missing',
            'reason': 'Switch image missing',
          }
        : {
            'kind': 'resolved',
            'resourceId': sha256Hex(_bytes),
            'resolvedScale': 2,
          },
  },
};

void main() {
  test(
    'all 236 leaves, three slots and 100/4561 catalog fingerprint are exact',
    () {
      final schema = canvasRuntimeWidgetSchemaContractForTesting();
      expect(schema, canvasReviewedWidgetSchemaContract.trimLeft());
      expect(
        RegExp(r'^W\|', multiLine: true).allMatches(schema),
        hasLength(248),
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(schema),
        hasLength(8306),
      );
      final start = schema.indexOf('W|$_type\n');
      final section = schema.substring(
        start,
        schema.indexOf('\nW|', start) + 1,
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(section),
        hasLength(236),
      );
      expect(
        RegExp(r'^S\|', multiLine: true).allMatches(section),
        hasLength(3),
      );
      expect(utf8.encode(section), hasLength(140037));
      expect(
        sha256Hex(utf8.encode(section)),
        '91cd77effce95be9b0a93b617f7ab1dfa8d968912bd46e2880257e3548f3159e',
      );
      expect(section, isNot(contains('P|enabled|')));
      expect(section, isNot(contains('P|trackOutlineWidth|')));
      expect(canvasModelProtocolVersion, 20);
      expect(_decode(_model()), isA<CanvasModel>());
    },
  );

  test(
    'strict null, image, shape, cursor, density and slot relationships reject invalid payloads',
    () {
      for (final props in <Map<String, Object?>>[
        {'value': _null},
        {
          'onChanged': {'kind': 'callbackPresence'},
        },
        {'variant': _str('other')},
        {
          'onChanged': {
            'kind': 'dartObjectReferencePresence',
            'member': 'execute',
          },
        },
        {'onActiveThumbImageError': _str('noop')},
        {'onInactiveThumbImageError': _ref},
        {'isThreeLine': _bool(true)},
        {'enabled': _bool(false)},
        {'titleAlignment': _enum('ListTileTitleAlignment', 'top')},
        {'shape': _ref, 'shapeKind': _str('circle')},
        {'thumbColor': _ref, 'thumbColorDefault': _null},
        {'thumbIcon': _ref, 'thumbIconDefaultMode': _str('inherit')},
        {
          'thumbIconDefaultMode': _str('inherit'),
          'thumbIconDefaultSize': _num(10),
        },
        {'visualDensity': _ref, 'visualDensityHorizontal': _num(1.0)},
        {'mouseCursorHovered': _enum('SystemMouseCursors', 'click')},
        {'mouseCursorDefault': _null},
        {
          'mouseCursor': _ref,
          'mouseCursorDefault': _enum('SystemMouseCursors', 'click'),
        },
      ]) {
        expect(
          () => _decode(_model(props: props)),
          throwsFormatException,
          reason: '$props',
        );
      }
      for (final name in [
        'onActiveThumbImageError',
        'onInactiveThumbImageError',
      ]) {
        expect(() => _decode(_model(props: {name: _null})), returnsNormally);
      }
      expect(
        () => _decode(_model(props: {'applyCupertinoTheme': _bool(true)})),
        returnsNormally,
      );
    },
  );

  for (final variant in ['standard', 'adaptive']) {
    for (final platform in [
      TargetPlatform.windows,
      TargetPlatform.iOS,
      TargetPlatform.macOS,
    ]) {
      testWidgets(
        '$variant $platform forwards SDK fields and all three controlled slots',
        (tester) async {
          final selected = <String>[];
          await _pump(
            tester,
            _model(
              variant: variant,
              props: {
                'isThreeLine': _bool(true),
                'value': _bool(true),
                'selected': _bool(false),
                'activeThumbColor': _color(0xff123456),
                'activeTrackColor': _color(0xff234567),
                'inactiveThumbColor': _color(0xff345678),
                'inactiveTrackColor': _color(0xff456789),
                'hoverColor': _color(0xff567890),
                'tileColor': _color(0xff678901),
                'selectedTileColor': _color(0xff789012),
                'dragStartBehavior': _enum('DragStartBehavior', 'down'),
                'applyCupertinoTheme': _bool(true),
                'onFocusChange': _str('noop'),
                'visualDensityHorizontal': _num(-1.0),
                'visualDensityVertical': _num(1.0),
              },
              slots: {
                'title': _single(_text('02', 'Title')),
                'subtitle': _single(_text('03', 'Subtitle')),
                'secondary': _single(_text('04', 'Secondary')),
              },
            ),
            platform: platform,
            selected: selected,
          );
          final tile = _sdk(tester);
          expect(tile.value, true);
          expect(tile.selected, false);
          expect(tile.activeThumbColor, const Color(0xff123456));
          expect(tile.activeTrackColor, const Color(0xff234567));
          expect(tile.inactiveThumbColor, const Color(0xff345678));
          expect(tile.inactiveTrackColor, const Color(0xff456789));
          expect(tile.hoverColor, const Color(0xff567890));
          expect(tile.tileColor, const Color(0xff678901));
          expect(tile.selectedTileColor, const Color(0xff789012));
          expect(tile.dragStartBehavior, DragStartBehavior.down);
          expect(tile.applyCupertinoTheme, variant == 'adaptive');
          expect(
            tile.visualDensity,
            const VisualDensity(horizontal: -1, vertical: 1),
          );
          expect(find.text('Title'), findsOneWidget);
          expect(find.text('Subtitle'), findsOneWidget);
          expect(find.text('Secondary'), findsOneWidget);
          // SDK 3.44.8 uses a unified MaterialSwitch with a Cupertino painter,
          // not a separate CupertinoSwitch widget as its older docs suggest.
          expect(
            Theme.of(tester.element(find.byType(Switch))).platform,
            platform,
          );
          final switchWidget = tester.widget<Switch>(find.byType(Switch));
          expect(
            switchWidget.materialTapTargetSize,
            MaterialTapTargetSize.shrinkWrap,
          );
          await tester.tapAt(
            tester.getRect(find.byType(ListTile)).topLeft + const Offset(4, 4),
          );
          await tester.pump();
          expect(selected, contains(_id));
          expect(_sdk(tester).value, true);
        },
      );
    }
  }

  for (final variant in ['standard', 'adaptive']) {
    for (final platform in [TargetPlatform.windows, TargetPlatform.iOS]) {
      testWidgets(
        '$variant $platform has one merged switch semantic action without duplicate button',
        (tester) async {
          final handle = tester.ensureSemantics();
          try {
            for (final value in [false, true]) {
              for (final enabled in [false, true]) {
                await _pump(
                  tester,
                  _model(
                    variant: variant,
                    props: {
                      'value': _bool(value),
                      'onChanged': enabled ? _ref : _null,
                    },
                    slots: {'title': _single(_text('02', 'Toggle title'))},
                  ),
                  platform: platform,
                );
                final data = tester
                    .getSemantics(find.byType(Switch))
                    .getSemanticsData();
                expect(
                  data.flagsCollection.isToggled,
                  value ? ui.Tristate.isTrue : ui.Tristate.isFalse,
                );
                expect(
                  data.flagsCollection.isEnabled,
                  enabled ? ui.Tristate.isTrue : ui.Tristate.isFalse,
                );
                expect(data.flagsCollection.isButton, false);
                expect(data.label, contains('Toggle title'));
                expect(_sdk(tester).onChanged == null, !enabled);
                expect(
                  tester.widget<ListTile>(find.byType(ListTile)).onTap == null,
                  !enabled,
                );
              }
            }
          } finally {
            handle.dispose();
          }
        },
      );
    }
  }

  testWidgets(
    'four state-color families and all nine icon/cursor buckets preserve null and first-match priority',
    (tester) async {
      final props = <String, Object?>{};
      for (final family in [
        'thumbColor',
        'trackColor',
        'trackOutlineColor',
        'overlayColor',
      ]) {
        props['${family}Default'] = _color(0xff123456);
        for (final state in _states.values) {
          props['$family$state'] = _null;
        }
      }
      for (final state in ['Default', ..._states.values]) {
        props['thumbIcon${state}Mode'] = _str('icon');
        props['thumbIcon${state}SemanticLabel'] = _str(state);
        props['thumbIcon${state}Size'] = _num(18);
        props['mouseCursor$state'] = _str(
          state == 'Default' ? 'basic' : 'click',
        );
      }
      await _pump(tester, _model(props: props));
      final tile = _sdk(tester);
      for (final color in [
        tile.thumbColor!,
        tile.trackColor!,
        tile.trackOutlineColor!,
        tile.overlayColor!,
      ]) {
        expect(color.resolve({}), const Color(0xff123456));
        for (final state in _states.keys) {
          expect(color.resolve({state}), isNull);
        }
      }
      expect(tile.thumbIcon!.resolve({})!.semanticLabel, 'Default');
      for (final entry in _states.entries) {
        expect(
          tile.thumbIcon!.resolve({entry.key})!.semanticLabel,
          entry.value,
        );
        expect(
          (tile.mouseCursor as WidgetStateMouseCursor).resolve({entry.key}),
          SystemMouseCursors.click,
        );
      }
      expect(
        tile.thumbIcon!.resolve(_states.keys.toSet())!.semanticLabel,
        'Disabled',
      );
      expect(
        (tile.mouseCursor as WidgetStateMouseCursor).resolve({}),
        SystemMouseCursors.basic,
      );
    },
  );

  testWidgets(
    'anonymous project appearance and controllers approximate explicitly with no code execution',
    (tester) async {
      final props = <String, Object?>{
        for (final name in [
          'activeColor',
          'activeThumbColor',
          'activeTrackColor',
          'inactiveThumbColor',
          'inactiveTrackColor',
          'tileColor',
          'selectedTileColor',
          'hoverColor',
          'thumbColor',
          'trackColor',
          'trackOutlineColor',
          'overlayColor',
          'thumbIcon',
          'shape',
          'visualDensity',
          'contentPadding',
          'mouseCursor',
          'focusNode',
          'statesController',
          'onChanged',
          'onFocusChange',
        ])
          name: _ref,
      };
      await _pump(tester, _model(props: props));
      final tile = _sdk(tester);
      expect(tile.onChanged, isNotNull);
      expect(tile.onFocusChange, isNotNull);
      expect(tile.focusNode, isNotNull);
      expect(tile.statesController, isNull);
      expect(tile.thumbColor, isNull);
      expect(tile.thumbIcon, isNull);
      expect(tile.shape, isNull);
      expect(tile.contentPadding, isNull);
      expect(tile.visualDensity, isNull);
      expect(tile.activeThumbColor, isNull);
      expect(
        _messages(tester),
        contains('isolated Canvas never executes project Dart'),
      );
      expect(_messages(tester), contains('approximation'));
    },
  );

  testWidgets(
    'ordinary updates and Apple configuration changes retain focus and child editing state',
    (tester) async {
      final child = _node(
        'd46caa0e-e357-4443-85dc-000000000002',
        'flutter.material.TextField',
        {},
      );
      final slots = {'title': _single(child)};
      await _pump(tester, _model(slots: slots));
      final state = tester.state<EditableTextState>(find.byType(EditableText));
      state.widget.controller.value = const TextEditingValue(
        text: 'keep draft',
        selection: TextSelection.collapsed(offset: 4),
      );
      final focus = _sdk(tester).focusNode!;
      focus.requestFocus();
      await tester.pump();
      final tileElement = tester.element(find.byType(SwitchListTile));
      for (final value in [true, false]) {
        await _pump(
          tester,
          _model(
            props: {'value': _bool(value), 'onChanged': _ref},
            slots: slots,
          ),
        );
        expect(tester.element(find.byType(SwitchListTile)), same(tileElement));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(state),
        );
        expect(_sdk(tester).focusNode, same(focus));
        expect(focus.hasFocus, true);
      }
      for (final platform in [
        TargetPlatform.iOS,
        TargetPlatform.windows,
        TargetPlatform.macOS,
        TargetPlatform.windows,
      ]) {
        await _pump(
          tester,
          _model(variant: 'adaptive', slots: slots),
          platform: platform,
        );
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(state),
        );
        expect(state.widget.controller.text, 'keep draft');
        expect(state.widget.controller.selection.baseOffset, 4);
        expect(_sdk(tester).focusNode, same(focus));
        expect(focus.hasFocus, true);
      }
      expect(_messages(tester), contains('lifecycle guard'));
    },
  );

  for (final name in ['activeThumbImage', 'inactiveThumbImage']) {
    testWidgets(
      '$name resolved resource and missing resource keep real SDK fallback without project errors',
      (tester) async {
        final errors = <String>[];
        final resources = CanvasImageResourceBundle.fromResources([
          CanvasImageResource(
            resourceId: sha256Hex(_bytes),
            mediaType: 'image/png',
            pixelWidth: 8,
            pixelHeight: 8,
            encodedBytes: _bytes,
          ),
        ]);
        final error = name == 'activeThumbImage'
            ? 'onActiveThumbImageError'
            : 'onInactiveThumbImageError';
        await _pump(
          tester,
          _model(props: {name: _image(), error: _ref}),
          resources: resources,
          onImageError: (id, _, _) => errors.add(id),
        );
        final tile = _sdk(tester);
        expect(
          name == 'activeThumbImage'
              ? tile.activeThumbImage
              : tile.inactiveThumbImage,
          isA<MemoryImage>(),
        );
        (name == 'activeThumbImage'
            ? tile.onActiveThumbImageError
            : tile.onInactiveThumbImageError)!(
          StateError('probe'),
          StackTrace.current,
        );
        expect(errors, [sha256Hex(_bytes)]);
        await _pump(
          tester,
          _model(props: {name: _image(missing: true), error: _ref}),
        );
        final missing = _sdk(tester);
        expect(
          name == 'activeThumbImage'
              ? missing.activeThumbImage
              : missing.inactiveThumbImage,
          isNull,
        );
        expect(
          name == 'activeThumbImage'
              ? missing.onActiveThumbImageError
              : missing.onInactiveThumbImageError,
          isNull,
        );
        expect(find.byType(Switch), findsOneWidget);
      },
    );
  }

  testWidgets(
    'nonfinite reaction radius diagnoses only the unavailable reaction',
    (tester) async {
      for (final value in ['infinity', 'negativeInfinity', 'nan']) {
        await _pump(
          tester,
          _model(
            props: {'splashRadius': _enum('double', value)},
            slots: {'title': _single(_text('02', 'Keep geometry'))},
          ),
        );
        expect(_sdk(tester).splashRadius, 0);
        expect(find.text('Keep geometry'), findsOneWidget);
        expect(_messages(tester), contains('radius 0 approximation'));
      }
      await _pump(tester, _model(props: {'splashRadius': _num(-2)}));
      expect(_sdk(tester).splashRadius, -2);
    },
  );

  testWidgets(
    'all ten local tile shapes keep control shape and stable child geometry independent',
    (tester) async {
      for (final shape in [
        'roundedRectangle',
        'beveledRectangle',
        'continuousRectangle',
        'roundedSuperellipse',
        'circle',
        'oval',
        'stadium',
        'linear',
        'star',
        'polygon',
      ]) {
        await _pump(
          tester,
          _model(
            props: {'shapeKind': _str(shape)},
            slots: {'title': _single(_text('02', 'Shape title'))},
          ),
        );
        expect(_sdk(tester).shape, isA<ShapeBorder>());
        expect(find.byType(Switch), findsOneWidget);
        expect(tester.getSize(find.byType(ListTile)).isFinite, true);
        expect(_messages(tester).trim(), isEmpty);
      }
    },
  );

  testWidgets(
    'nonfinite active tile positions withhold unsafe paint and recover the same child State',
    (tester) async {
      final slots = {
        'title': _single(
          _node(
            'd46caa0e-e357-4443-85dc-000000000002',
            'flutter.material.TextField',
            {},
          ),
        ),
      };
      await _pump(
        tester,
        _model(props: {'minVerticalPadding': _num(4)}, slots: slots),
      );
      final state = tester.state<EditableTextState>(find.byType(EditableText));
      state.widget.controller.text = 'retained';
      for (final value in [
        _enum('double', 'nan'),
        _num(6),
        _enum('double', 'infinity'),
        _num(8),
      ]) {
        final model = _model(
          props: {'minVerticalPadding': value},
          slots: slots,
        );
        final before = jsonEncode(model);
        await _pump(tester, model);
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(state),
        );
        expect(state.widget.controller.text, 'retained');
        expect(jsonEncode(model), before);
        expect(
          _messages(tester).contains('nonfinite'),
          value['kind'] == 'enum',
        );
      }
    },
  );

  testWidgets(
    'slot full-width guard retains the child and returns safe secondary geometry',
    (tester) async {
      final secondary = _node(
        'd46caa0e-e357-4443-85dc-000000000004',
        'flutter.widgets.SizedBox',
        {'width': _num(1000.0), 'height': _num(20)},
      );
      await _pump(
        tester,
        _model(
          slots: {
            'title': _single(_text('02', 'Keep title')),
            'secondary': _single(secondary),
          },
        ),
      );
      expect(_messages(tester), contains('secondary preview unavailable'));
      expect(
        find.byKey(
          const ValueKey('canvas-widget-d46caa0e-e357-4443-85dc-000000000004'),
        ),
        findsOneWidget,
      );
      expect(find.byType(Switch), findsOneWidget);
      expect(tester.getSize(find.byType(ListTile)).isFinite, true);
    },
  );

  testWidgets(
    'focus hover press and ordinary presence reset retain the SDK painter and tile identity',
    (tester) async {
      CustomPainter painter() => tester
          .widgetList<CustomPaint>(
            find.descendant(
              of: find.byType(Switch),
              matching: find.byType(CustomPaint),
            ),
          )
          .singleWhere((widget) => widget.painter != null)
          .painter!;
      await _pump(tester, _model());
      final element = tester.element(find.byType(SwitchListTile));
      final original = painter();
      final focus = _sdk(tester).focusNode!;
      focus.requestFocus();
      await tester.pump();
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer(location: Offset.zero);
      await mouse.moveTo(tester.getCenter(find.byType(Switch)));
      await tester.pump(const Duration(milliseconds: 100));
      await mouse.down(tester.getCenter(find.byType(Switch)));
      await tester.pump(const Duration(milliseconds: 50));
      final pressedFocus = focus.hasFocus;
      await _pump(
        tester,
        _model(props: {'thumbIcon': _ref, 'onChanged': _ref}),
      );
      expect(tester.element(find.byType(SwitchListTile)), same(element));
      expect(painter(), same(original));
      expect(focus.hasFocus, pressedFocus);
      await mouse.up();
      await mouse.removePointer();
      await tester.pump();
      await _pump(tester, _model());
      expect(painter(), same(original));
      expect(_sdk(tester).focusNode, same(focus));
      await _pump(
        tester,
        _model(variant: 'adaptive'),
        platform: TargetPlatform.iOS,
      );
      expect(painter(), isNot(same(original)));
      expect(_sdk(tester).focusNode, same(focus));
    },
  );

  for (final rtl in [false, true]) {
    testWidgets(
      'three distinct empty single-slot drop targets, occupied rejection and move geometry RTL=$rtl',
      (tester) async {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        await _pump(
          tester,
          _model(),
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
          expect(target?.parentWidgetId, _id);
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
}
