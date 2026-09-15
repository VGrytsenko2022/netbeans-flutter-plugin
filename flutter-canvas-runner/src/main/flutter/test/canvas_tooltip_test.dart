// Inspect every constructor field retained by the pinned SDK.
// ignore_for_file: deprecated_member_use
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Tooltip';
const _ref = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
String _id(int n) => '793560e4-67ae-4e99-907b-${n.toString().padLeft(12, '0')}';
Map<String, Object?> _s(String v) => {'kind': 'string', 'value': v};
Map<String, Object?> _b(bool v) => {'kind': 'boolean', 'value': v};
Map<String, Object?> _n(num v) => {
  'kind': v is int ? 'integer' : 'double',
  'value': v,
};
Map<String, Object?> _e(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _list(List<Object?> children) => {
  'kind': 'list',
  'children': children,
};
Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> properties, [
  Map<String, Object?> slots = const {},
]) => {'id': _id(id), 'type': type, 'properties': properties, 'slots': slots};
Map<String, Object?> _text(int id, String text) =>
    _node(id, 'flutter.widgets.Text', {'data': _s(text)});
Map<String, Object?> _tooltip({
  int id = 2,
  Map<String, Object?> props = const {},
  Object? child,
}) => _node(id, _type, {
  'message': _s('Actual Tooltip'),
  ...props,
}, child == null ? {} : {'child': _single(child)});
CanvasModel _model(Object root) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  model['root'] = _node(900, 'flutter.material.Scaffold', {}, {
    'body': _single(
      _node(901, 'flutter.widgets.Center', {}, {'child': _single(root)}),
    ),
  });
  return CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
}

Finder _anchor(int id) => find.byKey(ValueKey('canvas-widget-${_id(id)}'));
Finder _tooltipFinder([String? message = 'Actual Tooltip']) =>
    find.byWidgetPredicate(
      (w) => w is Tooltip && w.message == message && w.key is GlobalKey,
    );
Tooltip _sdk(WidgetTester tester, [String? message = 'Actual Tooltip']) =>
    tester.widget<Tooltip>(_tooltipFinder(message));
TooltipState _state(
  WidgetTester tester, [
  String? message = 'Actual Tooltip',
]) => tester.state<TooltipState>(_tooltipFinder(message));
String _messages(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((w) => w.message ?? '')
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Object root, {
  String? selected,
  ThemeData? theme,
  List<String>? selections,
  ValueChanged<CanvasDropResolver?>? onDrop,
  bool inlineEditing = false,
  CanvasImageResourceBundle? images,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme,
      themeAnimationDuration: Duration.zero,
      home: CanvasDocumentView(
        model: _model(root),
        selectedWidgetId: selected,
        onSelected: selections?.add ?? (_) {},
        onDropResolverChanged: onDrop,
        inlineTextEditEnabled: inlineEditing,
        imageResources: images,
      ),
    ),
  );
  await tester.pump();
  await tester.pump();
  expect(tester.takeException(), isNull);
}

void main() {
  test(
    'nullable content, height and exact anonymous typed-reference forms fail closed',
    () {
      for (final props in [
        {'message': _null, 'richMessage': _ref},
        {'richMessage': _null, 'height': _null, 'constraints': _constraints()},
        {'height': _n(-3), 'constraints': _null},
        {
          'waitDurationUs': _n(-9007199254740991),
          'showDurationUs': _n(9007199254740991),
          'exitDurationUs': _null,
        },
        {
          for (final name in [
            'height',
            'constraints',
            'padding',
            'margin',
            'verticalOffset',
            'preferBelow',
            'excludeFromSemantics',
            'decoration',
            'textStyle',
            'textAlign',
            'waitDurationUs',
            'showDurationUs',
            'exitDurationUs',
            'triggerMode',
            'enableFeedback',
            'onTriggered',
            'mouseCursor',
            'ignorePointer',
            'positionDelegate',
          ])
            name: _null,
        },
      ]) {
        expect(() => _model(_tooltip(props: props)), returnsNormally);
      }
      for (final props in [
        {'message': _null},
        {'richMessage': _ref},
        {'message': _null, 'richMessage': _null},
        {'enableTapToDismiss': _null},
        {'height': _n(10), 'constraints': _constraints()},
        {'waitDurationUs': _n(1.5)},
        {'showDurationUs': _n(9007199254740992)},
        {'onTriggered': _s('projectCall()')},
        {'positionDelegate': _s('myDelegate')},
        {
          'positionDelegate': {
            'kind': 'dartObjectReferencePresence',
            'expression': 'secret.factory()',
          },
        },
        {'textStyle': _null, 'textStyleFontSize': _n(16)},
        {
          'textStyleColor': _color('0xFF123456'),
          'textStyleForeground': _paint(),
        },
        {'textStyleLocaleScriptCode': _s('latn')},
      ]) {
        expect(
          () => _model(_tooltip(props: props)),
          throwsFormatException,
          reason: '$props',
        );
      }
      expect(
        () => _model(
          _tooltip(
            child: _node(
              3,
              'flutter.widgets.Expanded',
              {'flex': _n(1)},
              {'child': _single(_text(4, 'Invalid ParentData'))},
            ),
          ),
        ),
        throwsFormatException,
      );
    },
  );

  test(
    '53 independent Tooltip fields 100 widgets 4561 properties optional child and exact schema',
    () {
      final schema = canvasRuntimeWidgetSchemaContractForTesting();
      expect(schema, canvasReviewedWidgetSchemaContract.trimLeft());
      expect(
        RegExp(r'^W\|', multiLine: true).allMatches(schema),
        hasLength(237),
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(schema),
        hasLength(7806),
      );
      final start = schema.indexOf('W|$_type\n');
      final section = schema.substring(start, schema.indexOf('W|', start + 2));
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(section),
        hasLength(53),
      );
      expect(utf8.encode(section), hasLength(19924));
      expect(
        sha256Hex(utf8.encode(section)),
        'f1e7fa12338d2b49258c47bd6405ba40abcf21be9493b478865246facd65690f',
      );
      expect(section, contains('S|child|single|0|0|1|any\n'));
      expect(section, isNot(contains('wrapExistingChild')));
      expect(canvasDropSlotsForWidgetType(_type).single.slotName, 'child');
      expect(canvasModelProtocolVersion, 20);
    },
  );

  testWidgets(
    'all omitted nullable SDK arguments remain null and optional child stays absent zero sized',
    (tester) async {
      await _pump(tester, _tooltip());
      final sdk = _sdk(tester);
      expect(sdk.child, isNull);
      expect(sdk.richMessage, isNull);
      expect(sdk.height, isNull);
      expect(sdk.constraints, isNull);
      expect(sdk.padding, isNull);
      expect(sdk.margin, isNull);
      expect(sdk.waitDuration, isNull);
      expect(sdk.showDuration, isNull);
      expect(sdk.exitDuration, isNull);
      expect(sdk.onTriggered, isNull);
      expect(sdk.positionDelegate, isNull);
      expect(sdk.ignorePointer, isNull);
      expect(sdk.enableTapToDismiss, true);
      expect(tester.getSize(_tooltipFinder()), Size.zero);
      expect(
        find.byKey(ValueKey('canvas-zero-size-widget-target-${_id(2)}')),
        findsOneWidget,
      );
      final children = _model(
        _tooltip(),
      ).root.slots['body']!.child!.slots['child']!.child!.slots;
      expect(children, isEmpty);
    },
  );

  testWidgets(
    'real SDK hover wait and exit delays survive Manual touch mode without source changes',
    (tester) async {
      final root = _tooltip(
        props: {
          'triggerMode': _e('TooltipTriggerMode', 'manual'),
          'waitDurationUs': _n(200000),
          'exitDurationUs': _n(100000),
          'showDurationUs': _n(50000),
        },
        child: _text(3, 'Anchor'),
      );
      final before = jsonEncode(root);
      await _pump(tester, root);
      final mouse = await tester.createGesture(
        kind: ui.PointerDeviceKind.mouse,
      );
      await mouse.addPointer(location: Offset.zero);
      await tester.pump();
      await mouse.moveTo(tester.getCenter(_anchor(3)));
      await tester.pump(const Duration(milliseconds: 199));
      expect(find.text('Actual Tooltip'), findsNothing);
      await tester.pump(const Duration(milliseconds: 151));
      expect(find.text('Actual Tooltip'), findsOneWidget);
      await tester.pump(const Duration(seconds: 2));
      expect(find.text('Actual Tooltip'), findsOneWidget);
      await mouse.moveTo(Offset.zero);
      await tester.pump(const Duration(milliseconds: 99));
      expect(find.text('Actual Tooltip'), findsOneWidget);
      await tester.pump(const Duration(milliseconds: 200));
      await tester.pump(const Duration(milliseconds: 100));
      expect(find.text('Actual Tooltip'), findsNothing);
      await mouse.removePointer();
      expect(jsonEncode(root), before);
    },
  );

  for (final mode in ['tap', 'longPress', 'manual']) {
    testWidgets(
      'native touch $mode and deepest anchor selection do not execute project callbacks',
      (tester) async {
        final selected = <String>[];
        final root = _tooltip(
          props: {
            'triggerMode': _e('TooltipTriggerMode', mode),
            'onTriggered': _ref,
            'showDurationUs': _n(200000),
          },
          child: _node(
            3,
            'flutter.widgets.Padding',
            {
              'padding': {
                'kind': 'edgeInsets',
                'left': 12,
                'top': 12,
                'right': 12,
                'bottom': 12,
              },
            },
            {'child': _single(_text(4, 'Deep anchor'))},
          ),
        );
        final before = jsonEncode(root);
        await _pump(tester, root, selections: selected);
        if (mode == 'longPress') {
          await tester.longPress(find.text('Deep anchor'));
        } else {
          await tester.tap(find.text('Deep anchor'));
        }
        await tester.pump(const Duration(milliseconds: 151));
        expect(
          find.text('Actual Tooltip'),
          mode == 'manual' ? findsNothing : findsOneWidget,
        );
        expect(selected, [_id(4)]);
        expect(_sdk(tester).onTriggered, isNotNull);
        expect(_messages(tester), contains('never executes project Dart'));
        await tester.pump(const Duration(milliseconds: 300));
        await tester.pump(const Duration(milliseconds: 100));
        expect(find.text('Actual Tooltip'), findsNothing);
        expect(jsonEncode(root), before);
      },
    );
  }

  testWidgets(
    'changing anonymous diagnostics while real tooltip overlay is open retains tooltip and anchor State',
    (tester) async {
      final child = _node(3, 'flutter.material.TextField', {});
      await _pump(tester, _tooltip(child: child));
      final state = _state(tester);
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      edit.widget.controller.text = 'Not project code';
      state.ensureTooltipVisible();
      await tester.pumpAndSettle();
      for (final props in [
        <String, Object?>{'positionDelegate': _ref},
        <String, Object?>{},
        <String, Object?>{'message': _null, 'richMessage': _ref},
        <String, Object?>{},
      ]) {
        await _pump(tester, _tooltip(props: props, child: child));
        expect(
          _state(
            tester,
            props.containsKey('richMessage') ? null : 'Actual Tooltip',
          ),
          same(state),
        );
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(
          find.text(
            props.containsKey('richMessage')
                ? '[Preview unavailable: project InlineSpan]'
                : 'Actual Tooltip',
          ),
          findsOneWidget,
        );
      }
      expect(edit.widget.controller.text, 'Not project code');
    },
  );

  testWidgets(
    'ancestor Container SDK padding wrappers can change while a Tooltip overlay is open',
    (tester) async {
      final child = _tooltip(child: _text(3, 'Anchor'));
      for (final padding in [false, true, false]) {
        await _pump(
          tester,
          _node(
            10,
            'flutter.widgets.Container',
            {
              if (padding)
                'padding': {
                  'kind': 'edgeInsets',
                  'left': 8,
                  'top': 8,
                  'right': 8,
                  'bottom': 8,
                },
            },
            {'child': _single(child)},
          ),
        );
        _state(tester).ensureTooltipVisible();
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        expect(find.text('Actual Tooltip'), findsOneWidget);
      }
    },
  );

  testWidgets(
    'Container wrapper signatures cover alignment margin paint constraints and implicit border padding without resetting color edits',
    (tester) async {
      final tooltip = _tooltip(child: _text(3, 'Anchor'));
      const insets = {
        'kind': 'edgeInsets',
        'left': 7,
        'top': 7,
        'right': 7,
        'bottom': 7,
      };
      final decoration = _plainDecoration();
      final border = _plainDecoration(border: true);
      for (final pair in <(Map<String, Object?>, Map<String, Object?>)>[
        (
          {},
          {
            'alignment': {
              'kind': 'alignmentGeometry',
              'basis': 'physical',
              'horizontal': 0,
              'vertical': 0,
            },
          },
        ),
        ({}, {'margin': insets}),
        ({}, {'color': _color('0xFF123456')}),
        ({}, {'decoration': decoration}),
        ({}, {'foregroundDecoration': decoration}),
        ({}, {'constraints': _constraints()}),
        ({'decoration': decoration}, {'decoration': border}),
        (
          {'decoration': decoration},
          {'decoration': decoration, 'clipBehavior': _e('Clip', 'hardEdge')},
        ),
      ]) {
        for (final props in [pair.$1, pair.$2, pair.$1]) {
          final root = _node(10, 'flutter.widgets.Container', props, {
            'child': _single(tooltip),
          });
          final before = jsonEncode(root);
          await _pump(tester, root);
          _state(tester).ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull);
          expect(jsonEncode(root), before);
        }
      }
      TooltipState? state;
      for (final argb in ['0xFF123456', '0xFF654321']) {
        await _pump(
          tester,
          _node(
            10,
            'flutter.widgets.Container',
            {'color': _color(argb)},
            {'child': _single(tooltip)},
          ),
        );
        state ??= _state(tester);
        expect(_state(tester), same(state));
        state.ensureTooltipVisible();
        await tester.pumpAndSettle();
      }
    },
  );

  testWidgets(
    'Badge SDK minimum-key changes and FAB elevation-key variants safely retire nested open portals',
    (tester) async {
      final tooltip = _tooltip(child: _text(3, 'A'));
      for (final size in [16, 24, 16]) {
        await _pump(
          tester,
          _node(
            10,
            'flutter.material.Badge',
            {'largeSize': _n(size)},
            {'child': _single(tooltip), 'label': _single(_text(11, '1'))},
          ),
        );
        _state(tester).ensureTooltipVisible();
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
      }
      for (final variant in ['standard', 'extended', 'standard']) {
        for (final infinite in [false, true, false]) {
          await _pump(
            tester,
            _node(
              10,
              'flutter.material.FloatingActionButton',
              {
                'enabled': _b(true),
                'variant': _s(variant),
                'heroTag': _null,
                for (final name in [
                  'elevation',
                  'focusElevation',
                  'hoverElevation',
                  'highlightElevation',
                  'disabledElevation',
                ])
                  name: infinite ? _e('double', 'infinity') : _n(2),
              },
              {'child': _single(tooltip)},
            ),
          );
          _state(tester).ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  testWidgets(
    'custom clipper unavailable boundaries dispose only affected Tooltip previews and preserve model child identity',
    (tester) async {
      final tooltip = _tooltip(child: _text(3, 'Anchor'));
      for (final type in [
        'ClipRect',
        'ClipOval',
        'ClipRRect',
        'ClipRSuperellipse',
      ]) {
        for (final custom in [false, true, false]) {
          final root = _node(
            10,
            'flutter.widgets.$type',
            {if (custom) 'clipper': _ref},
            {'child': _single(tooltip)},
          );
          final before = jsonEncode(root);
          await _pump(tester, root);
          if (!custom) {
            _state(tester).ensureTooltipVisible();
            await tester.pumpAndSettle();
            expect(find.text('Actual Tooltip'), findsOneWidget);
          } else {
            expect(find.text('Actual Tooltip'), findsNothing);
          }
          expect(tester.takeException(), isNull);
          expect(jsonEncode(root), before);
        }
      }
    },
  );

  testWidgets(
    'empty plain message transitions retain actual Tooltip State and child editable State across selection',
    (tester) async {
      final child = _node(3, 'flutter.material.TextField', {});
      await _pump(tester, _tooltip(child: child));
      final tooltipState = _state(tester);
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      edit.widget.controller.text = 'Persistent anchor';
      _state(tester).ensureTooltipVisible();
      await tester.pumpAndSettle();
      for (final selection in [_id(2), _id(3), _id(900), null]) {
        await _pump(tester, _tooltip(child: child), selected: selection);
        expect(_state(tester), same(tooltipState));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(find.text('Actual Tooltip'), findsOneWidget);
      }
      await _pump(tester, _tooltip(props: {'message': _s('')}, child: child));
      expect(_state(tester, ''), same(tooltipState));
      expect(_state(tester, '').ensureTooltipVisible(), false);
      expect(
        tester.state<EditableTextState>(find.byType(EditableText)),
        same(edit),
      );
      expect(find.text('Actual Tooltip'), findsNothing);
      await _pump(tester, _tooltip(child: child));
      expect(edit.widget.controller.text, 'Persistent anchor');
      expect(_state(tester), same(tooltipState));
    },
  );

  testWidgets(
    'nested native Tooltips share one deepest hover and pointer selection',
    (tester) async {
      final selections = <String>[];
      final root = _tooltip(
        child: _tooltip(
          id: 3,
          props: {
            'message': _s('Inner Tooltip'),
            'triggerMode': _e('TooltipTriggerMode', 'tap'),
          },
          child: _text(4, 'Inner anchor'),
        ),
      );
      await _pump(tester, root, selections: selections);
      final mouse = await tester.createGesture(
        kind: ui.PointerDeviceKind.mouse,
      );
      await mouse.addPointer(location: Offset.zero);
      await mouse.moveTo(tester.getCenter(_anchor(4)));
      await tester.pumpAndSettle();
      expect(find.text('Inner Tooltip'), findsOneWidget);
      expect(find.text('Actual Tooltip'), findsNothing);
      await mouse.moveTo(Offset.zero);
      await tester.pump(const Duration(seconds: 1));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Inner anchor'));
      await tester.pump(const Duration(milliseconds: 151));
      expect(selections, [_id(4)]);
      expect(find.text('Inner Tooltip'), findsOneWidget);
      expect(find.text('Actual Tooltip'), findsNothing);
      await mouse.removePointer();
      Tooltip.dismissAllToolTips();
      await tester.pumpAndSettle();
    },
  );

  testWidgets(
    'real button wins native tap arena and does not duplicate deepest selection',
    (tester) async {
      var rawTriggers = 0;
      var rawButton = 0;
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Center(
              child: Tooltip(
                message: 'Raw',
                triggerMode: TooltipTriggerMode.tap,
                onTriggered: () => rawTriggers++,
                child: ElevatedButton(
                  onPressed: () => rawButton++,
                  child: const Text('Button'),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.tap(find.text('Button'));
      await tester.pumpAndSettle();
      expect(rawButton, 1);
      expect(rawTriggers, 0);
      expect(find.text('Raw'), findsNothing);
      final selections = <String>[];
      final root = _tooltip(
        props: {
          'triggerMode': _e('TooltipTriggerMode', 'tap'),
          'onTriggered': _ref,
        },
        child: _node(
          3,
          'flutter.material.ElevatedButton',
          {
            'onPressed': {'kind': 'callbackPresence'},
          },
          {'child': _single(_text(4, 'Button'))},
        ),
      );
      final before = jsonEncode(root);
      await _pump(tester, root, selections: selections);
      await tester.tap(find.text('Button'));
      await tester.pumpAndSettle();
      expect(find.text('Actual Tooltip'), findsNothing);
      expect(selections, [_id(4)]);
      expect(jsonEncode(root), before);
    },
  );

  testWidgets(
    'selection during native pointer sequence retains Tooltip State and open overlay',
    (tester) async {
      final selected = <String>[];
      final root = _tooltip(
        props: {
          'triggerMode': _e('TooltipTriggerMode', 'tap'),
          'enableTapToDismiss': _b(false),
        },
        child: _text(3, 'Anchor'),
      );
      await _pump(tester, root, selections: selected);
      final state = _state(tester);
      state.ensureTooltipVisible();
      await tester.pumpAndSettle();
      final gesture = await tester.startGesture(tester.getCenter(_anchor(3)));
      expect(selected, [_id(3)]);
      await _pump(tester, root, selected: _id(3), selections: selected);
      expect(_state(tester), same(state));
      expect(find.text('Actual Tooltip'), findsOneWidget);
      await gesture.up();
      await tester.pump();
      expect(selected, [_id(3)]);
      expect(_state(tester), same(state));
      Tooltip.dismissAllToolTips();
      await tester.pumpAndSettle();
    },
  );

  testWidgets(
    'inline editor focus survives diagnostic and selection-marker rebuild with open overlay',
    (tester) async {
      final child = _text(3, 'Editable anchor');
      await _pump(
        tester,
        _tooltip(child: child),
        selected: _id(3),
        inlineEditing: true,
      );
      await tester.tap(_anchor(3));
      await tester.pump(const Duration(milliseconds: 80));
      await tester.tap(_anchor(3));
      await tester.pumpAndSettle();
      final editor = find.byKey(
        ValueKey('canvas-inline-text-editor-${_id(3)}'),
      );
      expect(editor, findsOneWidget);
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      expect(edit.widget.focusNode.hasFocus, true);
      final state = _state(tester);
      state.ensureTooltipVisible();
      await tester.pumpAndSettle();
      for (final props in [
        {'positionDelegate': _ref},
        <String, Object?>{},
      ]) {
        await _pump(
          tester,
          _tooltip(props: props, child: child),
          selected: _id(3),
          inlineEditing: true,
        );
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(edit.widget.focusNode.hasFocus, true);
        expect(_state(tester), same(state));
        expect(find.text('Actual Tooltip'), findsOneWidget);
      }
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pumpAndSettle();
    },
  );

  testWidgets(
    'selection and warning badge preserve actual dry baselines intrinsic dimensions and tight geometry',
    (tester) async {
      final child = _node(3, 'flutter.widgets.Text', {
        'data': _s('Ag'),
        'styleFontSize': _n(48.0),
      });
      final sibling = _node(4, 'flutter.widgets.Text', {
        'data': _s('Peer'),
        'styleFontSize': _n(16.0),
      });
      Object root(Map<String, Object?> props) => _node(
        10,
        'flutter.widgets.Row',
        {
          'mainAxisSize': _e('MainAxisSize', 'min'),
          'crossAxisAlignment': _e('CrossAxisAlignment', 'baseline'),
          'textBaseline': _e('TextBaseline', 'alphabetic'),
        },
        {
          'children': _list([_tooltip(props: props, child: child), sibling]),
        },
      );
      await _pump(tester, root({}));
      final box = tester.renderObject<RenderBox>(_anchor(2));
      final baseline = _baseline(box);
      final dry = box.getDryBaseline(box.constraints, TextBaseline.alphabetic);
      final intrinsic = (
        box.getMinIntrinsicWidth(double.infinity),
        box.getMaxIntrinsicHeight(300),
      );
      final rect = tester.getRect(_anchor(3));
      final peer = tester.getRect(_anchor(4));
      for (final selected in [_id(2), _id(3), _id(10), null]) {
        for (final props in [
          {'positionDelegate': _ref},
          <String, Object?>{},
        ]) {
          await _pump(tester, root(props), selected: selected);
          final next = tester.renderObject<RenderBox>(_anchor(2));
          expect(_baseline(next), baseline);
          expect(
            next.getDryBaseline(next.constraints, TextBaseline.alphabetic),
            dry,
          );
          expect((
            next.getMinIntrinsicWidth(double.infinity),
            next.getMaxIntrinsicHeight(300),
          ), intrinsic);
          expect(tester.getRect(_anchor(3)), rect);
          expect(tester.getRect(_anchor(4)), peer);
        }
      }
      for (final width in [0, 150]) {
        final constrained = _node(
          10,
          'flutter.widgets.SizedBox',
          {'width': _n(width), 'height': _n(40)},
          {'child': _single(_tooltip(child: _text(3, 'Fixed anchor')))},
        );
        await _pump(tester, constrained);
        final size = tester.getSize(_anchor(10));
        await _pump(tester, constrained, selected: _id(10));
        expect(tester.getSize(_anchor(10)), size);
        expect(size, Size(width.toDouble(), 40));
      }
    },
  );

  for (final entry in <String, ({Object value, double expected, bool warning})>{
    'negative height': (value: _n(-32), expected: 0, warning: true),
    'NaN height': (value: _e('double', 'nan'), expected: 0, warning: true),
    'negative infinite height': (
      value: _e('double', 'negativeInfinity'),
      expected: 0,
      warning: true,
    ),
    'positive infinite height': (
      value: _e('double', 'infinity'),
      expected: double.infinity,
      warning: false,
    ),
    'negative infinite offset': (
      value: _e('double', 'negativeInfinity'),
      expected: 24,
      warning: true,
    ),
    'NaN offset': (value: _e('double', 'nan'), expected: 24, warning: true),
    'positive infinite offset': (
      value: _e('double', 'infinity'),
      expected: double.infinity,
      warning: false,
    ),
  }.entries) {
    testWidgets('overlay show phase is safe and honest for ${entry.key}', (
      tester,
    ) async {
      final isHeight = entry.key.endsWith('height');
      final props = {isHeight ? 'height' : 'verticalOffset': entry.value.value};
      final root = _tooltip(props: props, child: _text(3, 'Anchor'));
      final before = jsonEncode(root);
      await _pump(tester, root);
      expect(
        isHeight ? _sdk(tester).height : _sdk(tester).verticalOffset,
        entry.value.expected,
      );
      expect(
        _messages(tester).contains('preview limitation'),
        entry.value.warning,
      );
      _state(tester).ensureTooltipVisible();
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      expect(find.text('Actual Tooltip'), findsOneWidget);
      expect(tester.getTopLeft(find.text('Actual Tooltip')).isFinite, true);
      expect(jsonEncode(root), before);
    });
  }

  testWidgets(
    'negative and overflowing local and theme overlay insets use visible bounded approximations',
    (tester) async {
      for (final directional in [false, true]) {
        for (final overflowing in [false, true]) {
          final value = overflowing ? 1e308 : -7;
          final inset = {
            'kind': directional ? 'edgeInsetsDirectional' : 'edgeInsets',
            directional ? 'start' : 'left': value,
            'top': value,
            directional ? 'end' : 'right': value,
            'bottom': value,
          };
          await _pump(
            tester,
            _tooltip(
              props: {'padding': inset, 'margin': inset},
              child: _text(3, 'Anchor'),
            ),
          );
          expect(_sdk(tester).padding, EdgeInsets.zero);
          expect(_sdk(tester).margin, EdgeInsets.zero);
          expect(_messages(tester), contains('negative or overflowing'));
          _state(tester).ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull);
        }
      }
      await _pump(
        tester,
        _tooltip(child: _text(3, 'Anchor')),
        theme: ThemeData(
          tooltipTheme: const TooltipThemeData(
            padding: EdgeInsets.all(-4),
            margin: EdgeInsets.all(-8),
            constraints: BoxConstraints(minWidth: 60, maxWidth: 20),
            verticalOffset: double.nan,
          ),
        ),
      );
      _state(tester).ensureTooltipVisible();
      await tester.pumpAndSettle();
      expect(_sdk(tester).constraints, const BoxConstraints());
      expect(_sdk(tester).padding, EdgeInsets.zero);
      expect(_sdk(tester).verticalOffset, 24);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'duration values retain signed microseconds and references use actual SDK theme defaults',
    (tester) async {
      final theme = ThemeData(
        tooltipTheme: const TooltipThemeData(
          waitDuration: Duration(milliseconds: 80),
          showDuration: Duration(milliseconds: 120),
          exitDuration: Duration(milliseconds: 50),
          textStyle: TextStyle(fontSize: 19),
          padding: EdgeInsets.all(11),
          decoration: BoxDecoration(color: Colors.orange),
        ),
      );
      final props = {
        for (final name in [
          'waitDurationUs',
          'showDurationUs',
          'exitDurationUs',
          'textStyle',
          'decoration',
          'constraints',
          'padding',
          'margin',
          'mouseCursor',
          'positionDelegate',
        ])
          name: _ref,
      };
      await _pump(
        tester,
        _tooltip(props: props, child: _text(3, 'Anchor')),
        theme: theme,
      );
      final sdk = _sdk(tester);
      expect(sdk.waitDuration, isNull);
      expect(sdk.showDuration, isNull);
      expect(sdk.exitDuration, isNull);
      expect(sdk.textStyle, isNull);
      expect(sdk.decoration, isNull);
      expect(sdk.positionDelegate, isNull);
      expect(sdk.mouseCursor, isNull);
      _state(tester).ensureTooltipVisible();
      await tester.pumpAndSettle();
      expect(
        DefaultTextStyle.of(
          tester.element(find.text('Actual Tooltip')),
        ).style.fontSize,
        19,
      );
      await _pump(
        tester,
        _tooltip(
          props: {
            'triggerMode': _e('TooltipTriggerMode', 'tap'),
            'waitDurationUs': _n(-123),
            'showDurationUs': _n(-456),
            'exitDurationUs': _n(-789),
          },
          child: _text(3, 'Anchor'),
        ),
      );
      expect(_sdk(tester).waitDuration, const Duration(microseconds: -123));
      expect(_sdk(tester).showDuration, const Duration(microseconds: -456));
      expect(_sdk(tester).exitDuration, const Duration(microseconds: -789));
      Tooltip.dismissAllToolTips();
      await tester.pumpAndSettle();
      await tester.tap(_anchor(3));
      await tester.pump();
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'all 31 local text-style fields and both Paint branches reach the real overlay',
    (tester) async {
      final style = _style();
      expect(
        style.length,
        29,
      ); // The two Paint alternatives are mutually exclusive with colors.
      for (final paints in [false, true]) {
        final props = Map<String, Object?>.of(style);
        if (paints) {
          props.remove('textStyleColor');
          props.remove('textStyleBackgroundColor');
          props['textStyleForeground'] = _paint();
          props['textStyleBackground'] = _paint();
        }
        await _pump(tester, _tooltip(props: props, child: _text(3, 'Anchor')));
        final actual = _sdk(tester).textStyle!;
        expect(actual.inherit, false);
        expect(actual.fontSize, 18.5);
        expect(actual.fontWeight, FontWeight.w600);
        expect(actual.fontStyle, FontStyle.italic);
        expect(actual.letterSpacing, 1.25);
        expect(actual.wordSpacing, 2.5);
        expect(actual.textBaseline, TextBaseline.ideographic);
        expect(actual.height, 1.4);
        expect(actual.leadingDistribution, TextLeadingDistribution.even);
        expect(
          actual.locale,
          const Locale.fromSubtags(
            languageCode: 'en',
            scriptCode: 'Latn',
            countryCode: 'GB',
          ),
        );
        expect(actual.shadows!.single.offset, const Offset(-1.25, 2.5));
        expect(actual.fontFeatures, [const FontFeature('liga', 1)]);
        expect(actual.fontVariations, [const FontVariation('wght', 700)]);
        expect(
          actual.decoration,
          TextDecoration.combine([
            TextDecoration.underline,
            TextDecoration.overline,
            TextDecoration.lineThrough,
          ]),
        );
        expect(actual.decorationStyle, TextDecorationStyle.wavy);
        expect(actual.decorationThickness, 2.25);
        expect(actual.fontFamily, 'packages/design_fonts/Inter');
        expect(actual.fontFamilyFallback, [
          'packages/design_fonts/Noto Sans',
          'packages/design_fonts/Noto Color Emoji',
        ]);
        expect(actual.overflow, TextOverflow.fade);
        expect(actual.foreground != null, paints);
        expect(actual.background != null, paints);
        if (paints) expect(actual.foreground!.strokeWidth, 1.5);
        _state(tester).ensureTooltipVisible();
        await tester.pumpAndSettle();
        expect(
          DefaultTextStyle.of(
            tester.element(find.text('Actual Tooltip')),
          ).style,
          actual,
        );
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'structured decorations retain every compound and bounded image on the real overlay',
    (tester) async {
      final png = base64Decode(
        'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=',
      );
      final resourceId = sha256Hex(png);
      final images = CanvasImageResourceBundle.fromResources([
        CanvasImageResource(
          resourceId: resourceId,
          mediaType: 'image/png',
          pixelWidth: 8,
          pixelHeight: 8,
          encodedBytes: png,
        ),
      ]);
      for (final gradient in ['linear', 'radial', 'sweep']) {
        for (final directional in [false, true]) {
          final root = _tooltip(
            props: {
              'decoration': _decoration(gradient, directional, resourceId),
              'constraints': _constraints(),
              'padding': {
                'kind': 'edgeInsetsDirectional',
                'start': 7,
                'top': 3,
                'end': 9,
                'bottom': 5,
              },
              'textAlign': _e('TextAlign', 'end'),
              'preferBelow': _b(false),
              'enableFeedback': _b(false),
            },
            child: _text(3, 'Anchor'),
          );
          final before = jsonEncode(root);
          await _pump(tester, root, images: images);
          final sdk = _sdk(tester);
          final decoration = sdk.decoration! as BoxDecoration;
          expect(
            decoration.color,
            Theme.of(tester.element(_tooltipFinder())).colorScheme.surface,
          );
          expect(
            decoration.border,
            directional ? isA<BorderDirectional>() : isA<Border>(),
          );
          expect(
            decoration.borderRadius,
            directional ? isA<BorderRadiusDirectional>() : isA<BorderRadius>(),
          );
          expect(decoration.boxShadow!.single.offset, const Offset(3, 4));
          expect(decoration.boxShadow!.single.spreadRadius, -1);
          expect(decoration.boxShadow!.single.blurStyle, BlurStyle.outer);
          expect(decoration.gradient, switch (gradient) {
            'linear' => isA<LinearGradient>(),
            'radial' => isA<RadialGradient>(),
            _ => isA<SweepGradient>(),
          });
          expect(decoration.gradient!.stops, [0, 1]);
          expect(decoration.backgroundBlendMode, BlendMode.srcOver);
          final image = decoration.image!;
          expect(image.image, isA<MemoryImage>());
          expect(image.onError, isNotNull);
          expect(
            image.colorFilter,
            const ColorFilter.mode(Color(0xff336699), BlendMode.modulate),
          );
          expect(image.fit, BoxFit.fill);
          expect(image.alignment, const AlignmentDirectional(-1, .25));
          expect(image.centerSlice, const Rect.fromLTRB(1, 1, 3, 3));
          expect(image.repeat, ImageRepeat.repeatX);
          expect(image.matchTextDirection, true);
          expect(image.scale, 1);
          expect(image.opacity, .65);
          expect(image.filterQuality, FilterQuality.high);
          expect(image.invertColors, true);
          expect(image.isAntiAlias, true);
          expect(sdk.textAlign, TextAlign.end);
          expect(sdk.preferBelow, false);
          expect(sdk.enableFeedback, false);
          _state(tester).ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull);
          expect(
            find.byWidgetPredicate(
              (w) => w is Container && w.decoration == decoration,
            ),
            findsOneWidget,
          );
          expect(jsonEncode(root), before);
        }
      }
    },
  );

  testWidgets(
    'all 41 cursor presets remain exact SDK MouseRegions instead of the Designer click cursor',
    (tester) async {
      const cursors = <String, MouseCursor>{
        'defer': MouseCursor.defer,
        'uncontrolled': MouseCursor.uncontrolled,
        'clickable': WidgetStateMouseCursor.clickable,
        'adaptiveClickable': WidgetStateMouseCursor.adaptiveClickable,
        'textable': WidgetStateMouseCursor.textable,
        'none': SystemMouseCursors.none,
        'basic': SystemMouseCursors.basic,
        'click': SystemMouseCursors.click,
        'forbidden': SystemMouseCursors.forbidden,
        'wait': SystemMouseCursors.wait,
        'progress': SystemMouseCursors.progress,
        'contextMenu': SystemMouseCursors.contextMenu,
        'help': SystemMouseCursors.help,
        'text': SystemMouseCursors.text,
        'verticalText': SystemMouseCursors.verticalText,
        'cell': SystemMouseCursors.cell,
        'precise': SystemMouseCursors.precise,
        'move': SystemMouseCursors.move,
        'grab': SystemMouseCursors.grab,
        'grabbing': SystemMouseCursors.grabbing,
        'noDrop': SystemMouseCursors.noDrop,
        'alias': SystemMouseCursors.alias,
        'copy': SystemMouseCursors.copy,
        'disappearing': SystemMouseCursors.disappearing,
        'allScroll': SystemMouseCursors.allScroll,
        'resizeLeftRight': SystemMouseCursors.resizeLeftRight,
        'resizeUpDown': SystemMouseCursors.resizeUpDown,
        'resizeUpLeftDownRight': SystemMouseCursors.resizeUpLeftDownRight,
        'resizeUpRightDownLeft': SystemMouseCursors.resizeUpRightDownLeft,
        'resizeUp': SystemMouseCursors.resizeUp,
        'resizeDown': SystemMouseCursors.resizeDown,
        'resizeLeft': SystemMouseCursors.resizeLeft,
        'resizeRight': SystemMouseCursors.resizeRight,
        'resizeUpLeft': SystemMouseCursors.resizeUpLeft,
        'resizeUpRight': SystemMouseCursors.resizeUpRight,
        'resizeDownLeft': SystemMouseCursors.resizeDownLeft,
        'resizeDownRight': SystemMouseCursors.resizeDownRight,
        'resizeColumn': SystemMouseCursors.resizeColumn,
        'resizeRow': SystemMouseCursors.resizeRow,
        'zoomIn': SystemMouseCursors.zoomIn,
        'zoomOut': SystemMouseCursors.zoomOut,
      };
      expect(cursors, hasLength(41));
      for (final entry in cursors.entries) {
        await _pump(
          tester,
          _tooltip(
            props: {'mouseCursor': _s(entry.key)},
            child: _text(3, 'Anchor'),
          ),
        );
        expect(_sdk(tester).mouseCursor, same(entry.value));
        final regions = tester.widgetList<MouseRegion>(
          find.descendant(
            of: _tooltipFinder(),
            matching: find.byType(MouseRegion),
          ),
        );
        expect(
          regions.any((region) => identical(region.cursor, entry.value)),
          true,
          reason: entry.key,
        );
        expect(
          regions
              .where((region) => region.cursor == SystemMouseCursors.click)
              .length,
          entry.key == 'click' ? 1 : 0,
        );
      }
    },
  );

  testWidgets(
    'empty anchor has only semantic child drop and overlay content never becomes a model node',
    (tester) async {
      CanvasDropResolver? resolver;
      await _pump(
        tester,
        _tooltip(props: {'positionDelegate': _ref}),
        selected: _id(2),
        onDrop: (value) => resolver = value,
      );
      final handle = find.byKey(
        ValueKey('canvas-zero-size-widget-target-${_id(2)}'),
      );
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getCenter(handle);
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'text',
          widgetType: 'flutter.widgets.Text',
          traits: const {},
        ),
      );
      expect(target?.parentWidgetId, _id(2));
      expect(target?.slotName, 'child');
      expect(target?.insertionIndex, 0);
      expect(_sdk(tester).child, isNull);
      _state(tester).ensureTooltipVisible();
      await tester.pumpAndSettle();
      expect(find.text('Actual Tooltip'), findsOneWidget);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is KeyedSubtree &&
              w.key is ValueKey &&
              (w.key as ValueKey).value.toString().startsWith('canvas-widget-'),
        ),
        findsNWidgets(3),
      );
      expect(tester.getSize(_tooltipFinder()), Size.zero);
    },
  );
}

Map<String, Object?> _constraints() => {
  'kind': 'boxConstraints',
  'minWidth': 0,
  'maxWidth': 220,
  'minHeight': 0,
  'maxHeight': 100,
};
Map<String, Object?> _color(String argb) => {'kind': 'color', 'argb': argb};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
Map<String, Object?> _paint() => {
  'kind': 'paint',
  'color': {'kind': 'literal', 'argb': '0xFF123456'},
  'blendMode': 'srcOver',
  'style': 'stroke',
  'strokeWidth': 1.5,
  'strokeCap': 'round',
  'strokeJoin': 'bevel',
  'strokeMiterLimit': 4.0,
  'antiAlias': true,
  'filterQuality': 'medium',
  'invertColors': false,
};
Map<String, Object?> _style() => {
  'textStyleThemeTextStyle': _theme('material.textTheme.bodyLarge'),
  'textStyleInherit': _b(false),
  'textStyleColor': _color('0xFF123456'),
  'textStyleBackgroundColor': _color('0x44123456'),
  'textStyleFontSize': _n(18.5),
  'textStyleFontWeight': _e('FontWeight', 'w600'),
  'textStyleFontStyle': _e('FontStyle', 'italic'),
  'textStyleLetterSpacing': _n(1.25),
  'textStyleWordSpacing': _n(2.5),
  'textStyleTextBaseline': _e('TextBaseline', 'ideographic'),
  'textStyleHeight': _n(1.4),
  'textStyleLeadingDistribution': _e('TextLeadingDistribution', 'even'),
  'textStyleLocaleLanguageCode': _s('en'),
  'textStyleLocaleScriptCode': _s('Latn'),
  'textStyleLocaleCountryCode': _s('GB'),
  'textStyleShadows': {
    'kind': 'shadowList',
    'items': [
      {
        'id': _id(100),
        'color': {'kind': 'theme', 'token': 'material.colorScheme.shadow'},
        'offsetX': -1.25,
        'offsetY': 2.5,
        'blurRadius': 4.0,
      },
    ],
  },
  'textStyleFontFeatures': {
    'kind': 'fontFeatureList',
    'items': [
      {'id': _id(101), 'tag': 'liga', 'value': 1},
    ],
  },
  'textStyleFontVariations': {
    'kind': 'fontVariationList',
    'items': [
      {'id': _id(102), 'axis': 'wght', 'value': 700.0},
    ],
  },
  'textStyleDecorationUnderline': _b(true),
  'textStyleDecorationOverline': _b(true),
  'textStyleDecorationLineThrough': _b(true),
  'textStyleDecorationColor': _theme('material.colorScheme.error'),
  'textStyleDecorationStyle': _e('TextDecorationStyle', 'wavy'),
  'textStyleDecorationThickness': _n(2.25),
  'textStyleDebugLabel': _s('tooltip style'),
  'textStyleFontFamily': _s('Inter'),
  'textStyleFontFamilyFallback': _s('Noto Sans\nNoto Color Emoji'),
  'textStylePackage': _s('design_fonts'),
  'textStyleOverflow': _e('TextOverflow', 'fade'),
};

Map<String, Object?> _decoration(
  String kind,
  bool directional,
  String resourceId,
) {
  final stops = [
    for (final n in [0, 1])
      {
        'id': _id(110 + n),
        'stop': n,
        'color': n == 0
            ? {'kind': 'literal', 'argb': '0xFF102030'}
            : {'kind': 'theme', 'token': 'material.colorScheme.primary'},
      },
  ];
  const center = {'basis': 'directional', 'horizontal': 0, 'vertical': 0};
  final gradient = {
    'kind': kind,
    'stops': stops,
    'tileMode': 'mirror',
    'rotationRadians': .25,
    if (kind == 'linear') ...{
      'begin': center,
      'end': {'basis': 'physical', 'horizontal': 1, 'vertical': 1},
    } else ...{
      'center': center,
      if (kind == 'radial') ...{
        'radius': .75,
        'focal': {'basis': 'physical', 'horizontal': .25, 'vertical': -.25},
        'focalRadius': .1,
      } else ...{
        'startAngle': 0,
        'endAngle': 6.28,
      },
    },
  };
  const side = {
    'color': {'kind': 'theme', 'token': 'material.colorScheme.outline'},
    'width': 1,
    'style': 'solid',
    'strokeAlign': -1,
  };
  return {
    'kind': 'boxDecoration',
    'color': {'kind': 'theme', 'token': 'material.colorScheme.surface'},
    'shape': 'rectangle',
    'backgroundBlendMode': 'srcOver',
    'gradient': gradient,
    'border': {
      'kind': directional ? 'directional' : 'physical',
      'top': side,
      'bottom': side,
      directional ? 'start' : 'left': side,
      directional ? 'end' : 'right': side,
    },
    'borderRadius': {
      'kind': directional ? 'directional' : 'physical',
      for (final name
          in directional
              ? ['topStart', 'topEnd', 'bottomStart', 'bottomEnd']
              : ['topLeft', 'topRight', 'bottomLeft', 'bottomRight'])
        name: {'x': 8, 'y': 6},
    },
    'boxShadow': [
      {
        'id': _id(112),
        'color': {'kind': 'theme', 'token': 'material.colorScheme.shadow'},
        'offsetX': 3,
        'offsetY': 4,
        'blurRadius': 5,
        'spreadRadius': -1,
        'blurStyle': 'outer',
      },
    ],
    'image': {
      'image': {
        'kind': 'asset',
        'assetName': 'assets/tooltip.png',
        'packageName': null,
        'exactScale': null,
        'resize': null,
        'resolution': {
          'kind': 'resolved',
          'resourceId': resourceId,
          'resolvedScale': 1,
        },
      },
      'onError': true,
      'colorFilter': {
        'kind': 'mode',
        'color': {'kind': 'literal', 'argb': '0xFF336699'},
        'blendMode': 'modulate',
      },
      'fit': 'fill',
      'alignment': {'basis': 'directional', 'horizontal': -1, 'vertical': .25},
      'centerSlice': {'left': 1, 'top': 1, 'right': 3, 'bottom': 3},
      'repeat': 'repeatX',
      'matchTextDirection': true,
      'scale': 1,
      'opacity': .65,
      'filterQuality': 'high',
      'invertColors': true,
      'isAntiAlias': true,
    },
  };
}

Map<String, Object?> _plainDecoration({bool border = false}) => {
  'kind': 'boxDecoration',
  'color': {'kind': 'literal', 'argb': '0xFFABCDEF'},
  'image': null,
  'borderRadius': null,
  'boxShadow': <Object>[],
  'gradient': null,
  'backgroundBlendMode': null,
  'shape': 'rectangle',
  'border': border
      ? {
          'kind': 'physical',
          for (final side in ['top', 'right', 'bottom', 'left'])
            side: {
              'color': {'kind': 'literal', 'argb': '0xFF123456'},
              'width': 2,
              'style': 'solid',
              'strokeAlign': -1,
            },
        }
      : null,
};

double? _baseline(RenderBox box) {
  final previous = RenderObject.debugCheckingIntrinsics;
  RenderObject.debugCheckingIntrinsics = true;
  try {
    return box.getDistanceToBaseline(TextBaseline.alphabetic);
  } finally {
    RenderObject.debugCheckingIntrinsics = previous;
  }
}
