// Inspect the exact public fields still forwarded by the pinned SDK.
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

const _type = 'flutter.material.ExpansionTile';
const _ref = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
const _sdkCurves = <String, Curve>{
  'linear': Curves.linear,
  'decelerate': Curves.decelerate,
  'fastLinearToSlowEaseIn': Curves.fastLinearToSlowEaseIn,
  'fastEaseInToSlowEaseOut': Curves.fastEaseInToSlowEaseOut,
  'ease': Curves.ease,
  'easeIn': Curves.easeIn,
  'easeInToLinear': Curves.easeInToLinear,
  'easeInSine': Curves.easeInSine,
  'easeInQuad': Curves.easeInQuad,
  'easeInCubic': Curves.easeInCubic,
  'easeInQuart': Curves.easeInQuart,
  'easeInQuint': Curves.easeInQuint,
  'easeInExpo': Curves.easeInExpo,
  'easeInCirc': Curves.easeInCirc,
  'easeInBack': Curves.easeInBack,
  'easeOut': Curves.easeOut,
  'linearToEaseOut': Curves.linearToEaseOut,
  'easeOutSine': Curves.easeOutSine,
  'easeOutQuad': Curves.easeOutQuad,
  'easeOutCubic': Curves.easeOutCubic,
  'easeOutQuart': Curves.easeOutQuart,
  'easeOutQuint': Curves.easeOutQuint,
  'easeOutExpo': Curves.easeOutExpo,
  'easeOutCirc': Curves.easeOutCirc,
  'easeOutBack': Curves.easeOutBack,
  'easeInOut': Curves.easeInOut,
  'easeInOutSine': Curves.easeInOutSine,
  'easeInOutQuad': Curves.easeInOutQuad,
  'easeInOutCubic': Curves.easeInOutCubic,
  'easeInOutCubicEmphasized': Curves.easeInOutCubicEmphasized,
  'easeInOutQuart': Curves.easeInOutQuart,
  'easeInOutQuint': Curves.easeInOutQuint,
  'easeInOutExpo': Curves.easeInOutExpo,
  'easeInOutCirc': Curves.easeInOutCirc,
  'easeInOutBack': Curves.easeInOutBack,
  'fastOutSlowIn': Curves.fastOutSlowIn,
  'slowMiddle': Curves.slowMiddle,
  'bounceIn': Curves.bounceIn,
  'bounceOut': Curves.bounceOut,
  'bounceInOut': Curves.bounceInOut,
  'elasticIn': Curves.elasticIn,
  'elasticOut': Curves.elasticOut,
  'elasticInOut': Curves.elasticInOut,
};
String _id(int n) => '32a753d2-8525-41b2-a4fb-${n.toString().padLeft(12, '0')}';
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
Map<String, Object?> _list(List<Object?> v) => {'kind': 'list', 'children': v};
Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> props, [
  Map<String, Object?> slots = const {},
]) => {'id': _id(id), 'type': type, 'properties': props, 'slots': slots};
Map<String, Object?> _text(int id, String label) =>
    _node(id, 'flutter.widgets.Text', {'data': _s(label)});
Map<String, Object?> _tile({
  int id = 2,
  Map<String, Object?> props = const {},
  Map<String, Object?> slots = const {},
}) => _node(id, _type, props, {
  'title': _single(_text(id + 100, 'Title $id')),
  ...slots,
});
Map<String, Object?> _model(Object? tile) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  model['root'] = _node(900, 'flutter.material.Scaffold', {}, {
    'body': _single(
      _node(901, 'flutter.widgets.Center', {}, {'child': _single(tile)}),
    ),
  });
  return model;
}

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
ExpansionTile _sdk(WidgetTester tester) =>
    tester.widget<ExpansionTile>(find.byType(ExpansionTile).first);
String _messages(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((w) => w.message ?? '')
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Object? tile, {
  ThemeData? theme,
  bool rtl = false,
  String? selected,
  ValueChanged<CanvasDropResolver?>? onDrop,
  ValueChanged<CanvasMovePreviewResolver?>? onMove,
  List<String>? selections,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme,
      themeAnimationDuration: Duration.zero,
      home: Directionality(
        textDirection: rtl ? TextDirection.rtl : TextDirection.ltr,
        child: CanvasDocumentView(
          model: _decode(_model(tile)),
          selectedWidgetId: selected,
          onSelected: selections?.add ?? (_) {},
          onDropResolverChanged: onDrop,
          onMovePreviewResolverChanged: onMove,
        ),
      ),
    ),
  );
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 350));
  await tester.pump();
  expect(tester.takeException(), isNull);
}

void main() {
  test(
    '76 leaves five slots and wrapExistingChild title match independent 100/4561 contract',
    () {
      final schema = canvasRuntimeWidgetSchemaContractForTesting();
      expect(schema, canvasReviewedWidgetSchemaContract.trimLeft());
      expect(
        RegExp(r'^W\|', multiLine: true).allMatches(schema),
        hasLength(234),
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(schema),
        hasLength(7759),
      );
      final start = schema.indexOf('W|$_type\n');
      final section = schema.substring(start, schema.indexOf('W|', start + 2));
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(section),
        hasLength(76),
      );
      expect(
        RegExp(r'^S\|', multiLine: true).allMatches(section),
        hasLength(5),
      );
      expect(
        section,
        contains('C|$_type|paletteCreate|wrapExistingChild|title\n'),
      );
      expect(utf8.encode(section), hasLength(29253));
      expect(
        sha256Hex(utf8.encode(section)),
        '8062b277606f0cbf1190df21e8504ba27dc31e9cc5f563dcb349ce8c9a96a1c2',
      );
      expect(canvasExpansionCurvePresets, hasLength(43));
      expect(canvasReviewedRequiredWrapperSlot(_type), 'title');
      expect(canvasModelProtocolVersion, 20);
    },
  );

  testWidgets(
    'real SDK header arrow expands while clicking Title still selects the child without changing model seed',
    (tester) async {
      final tile = _tile(
        slots: {
          'children': _list([_text(3, 'Child')]),
        },
      );
      final before = jsonEncode(tile);
      final selections = <String>[];
      await _pump(tester, tile, selections: selections);
      expect(_sdk(tester).controller!.isExpanded, false);
      expect(_sdk(tester).onExpansionChanged, isNull);
      expect(find.text('Child'), findsNothing);
      await tester.tap(find.text('Title 2'));
      await tester.pumpAndSettle();
      expect(selections.last, _id(102));
      expect(_sdk(tester).controller!.isExpanded, false);
      await tester.tap(find.byIcon(Icons.expand_more));
      await tester.pumpAndSettle();
      expect(_sdk(tester).controller!.isExpanded, true);
      expect(find.text('Child'), findsOneWidget);
      expect(jsonEncode(tile), before);
      expect(_messages(tester).trim(), isEmpty);
    },
  );

  testWidgets(
    'ordinary updates preserve actual tile controller State and transient expansion but explicit seed edits synchronize only preview',
    (tester) async {
      await _pump(tester, _tile());
      final state = tester.state(find.byType(ExpansionTile));
      final controller = _sdk(tester).controller!;
      controller.expand();
      await tester.pumpAndSettle();
      await _pump(
        tester,
        _tile(props: {'iconColor': _c(0xff123456), 'controller': _ref}),
      );
      expect(_sdk(tester).controller, same(controller));
      expect(tester.state(find.byType(ExpansionTile)), same(state));
      expect(controller.isExpanded, true);
      await _pump(tester, _tile(props: {'initiallyExpanded': _b(true)}));
      await _pump(tester, _tile(props: {'initiallyExpanded': _b(false)}));
      expect(controller.isExpanded, false);
      expect(tester.state(find.byType(ExpansionTile)), same(state));
      await _pump(tester, _tile(props: {'initiallyExpanded': _b(true)}));
      expect(controller.isExpanded, true);
      expect(tester.state(find.byType(ExpansionTile)), same(state));
    },
  );

  testWidgets(
    'all 43 actual preset curves animate safely and only known undershooting height curves are explicitly approximated',
    (tester) async {
      for (final entry in _sdkCurves.entries) {
        await _pump(
          tester,
          _tile(
            props: {
              'expansionAnimationStyleDurationUs': _n(100000),
              'expansionAnimationStyleCurve': _s(entry.key),
              'expansionAnimationStyleReverseCurve': _s(entry.key),
            },
            slots: {
              'children': _list([_text(3, 'Body')]),
            },
          ),
        );
        final unsafe = const {
          'easeInBack',
          'easeInOutBack',
          'elasticIn',
          'elasticInOut',
        }.contains(entry.key);
        final sdk = _sdk(tester);
        expect(
          sdk.expansionAnimationStyle!.curve,
          same(unsafe ? Curves.easeIn : entry.value),
        );
        expect(
          sdk.expansionAnimationStyle!.reverseCurve,
          same(unsafe ? Curves.easeIn : entry.value),
        );
        expect(_messages(tester).contains('negative height factors'), unsafe);
        for (final expanded in [true, false]) {
          if (expanded) {
            sdk.controller!.expand();
          } else {
            sdk.controller!.collapse();
          }
          await tester.pump();
          for (var i = 0; i < 6; i++) {
            await tester.pump(const Duration(milliseconds: 20));
            expect(
              tester.takeException(),
              isNull,
              reason: '${entry.key} $expanded',
            );
          }
        }
      }
    },
  );

  testWidgets(
    'raw pinned SDK reproduces negative-duration and negative-height-factor animation assertions',
    (tester) async {
      for (final curve in [
        Curves.easeInBack,
        Curves.easeInOutBack,
        Curves.elasticIn,
        Curves.elasticInOut,
      ]) {
        final controller = ExpansibleController();
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ExpansionTile(
                controller: controller,
                title: const Text('Raw'),
                expansionAnimationStyle: AnimationStyle(
                  duration: const Duration(seconds: 1),
                  curve: curve,
                ),
                children: const [Text('Body')],
              ),
            ),
          ),
        );
        final sample = List.generate(
          99,
          (i) => i + 1,
        ).firstWhere((i) => curve.transform(i / 100) < 0);
        controller.expand();
        await tester.pump();
        await tester.pump(Duration(milliseconds: sample * 10));
        expect(tester.takeException().toString(), contains('heightFactor'));
        await tester.pumpWidget(const SizedBox.shrink());
        controller.dispose();
      }
      final controller = ExpansibleController();
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ExpansionTile(
              controller: controller,
              title: const Text('Raw'),
              expansionAnimationStyle: const AnimationStyle(
                duration: Duration(microseconds: -1),
              ),
              children: const [Text('Body')],
            ),
          ),
        ),
      );
      controller.expand();
      expect(tester.takeException().toString(), contains('simulationDuration'));
      await tester.pumpWidget(const SizedBox.shrink());
      controller.dispose();
    },
  );

  testWidgets(
    'negative duration guard noAnimation null theme and ignored reverseDuration retain exact distinct contracts',
    (tester) async {
      await _pump(
        tester,
        _tile(props: {'expansionAnimationStyleDurationUs': _n(-1)}),
      );
      expect(_sdk(tester).expansionAnimationStyle!.duration, Duration.zero);
      expect(_messages(tester), contains('negative duration'));
      _sdk(tester).controller!.expand();
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await _pump(
        tester,
        _tile(props: {'expansionAnimationStyle': _s('noAnimation')}),
      );
      expect(_sdk(tester).expansionAnimationStyle, AnimationStyle.noAnimation);
      expect(_messages(tester).trim(), isEmpty);
      await _pump(
        tester,
        _tile(props: {'expansionAnimationStyleReverseDurationUs': _n(-7)}),
      );
      expect(
        _sdk(tester).expansionAnimationStyle!.reverseDuration,
        const Duration(microseconds: -7),
      );
      expect(
        tester.widget<Expansible>(find.byType(Expansible)).duration,
        const Duration(milliseconds: 200),
      );
      expect(
        tester.widget<Expansible>(find.byType(Expansible)).animationStyle,
        isNull,
      );
      expect(_messages(tester).trim(), isEmpty);
      final theme = ThemeData(
        expansionTileTheme: const ExpansionTileThemeData(
          expansionAnimationStyle: AnimationStyle(
            duration: Duration(microseconds: 750000),
            curve: Curves.linear,
            reverseCurve: Curves.easeOut,
          ),
        ),
      );
      for (final value in [_null, _ref]) {
        await _pump(
          tester,
          _tile(
            props: {
              'expansionAnimationStyleDurationUs': value,
              'expansionAnimationStyleCurve': value,
            },
          ),
          theme: theme,
        );
        final actual = tester.widget<Expansible>(find.byType(Expansible));
        expect(actual.duration, const Duration(microseconds: 750000));
        expect(actual.curve, Curves.linear);
        expect(actual.reverseCurve, Curves.easeOut);
        expect(
          _messages(tester).contains('never executes project Dart'),
          value == _ref,
        );
      }
      await _pump(
        tester,
        _tile(props: {'expansionAnimationStyleReverseDurationUs': _ref}),
      );
      expect(
        _messages(tester).trim(),
        isEmpty,
      ); // Ignored by the pinned implementation.
    },
  );

  test(
    'required title nullable leaves enum curve and exclusive whole/local families reject invalid wire data',
    () {
      final absent = _tile();
      (absent['slots'] as Map).remove('title');
      expect(() => _decode(_model(absent)), throwsFormatException);
      expect(
        () => _decode(_model(_tile(slots: {'title': _single(null)}))),
        throwsFormatException,
      );
      for (final props in <Map<String, Object?>>[
        {'initiallyExpanded': _null},
        {'enabled': _null},
        {'showTrailingIcon': _null},
        {'expandedCrossAxisAlignment': _e('CrossAxisAlignment', 'baseline')},
        {'expansionAnimationStyleCurve': _s('unreviewed')},
        {'expansionAnimationStyleDurationUs': _n(1.0)},
        {
          'expansionAnimationStyle': _null,
          'expansionAnimationStyleCurve': _null,
        },
        {'visualDensity': _null, 'visualDensityHorizontal': _n(1.0)},
        {'shape': _null, 'shapeKind': _s('circle')},
        {'collapsedShape': _ref, 'collapsedShapeKind': _s('circle')},
        {'shapePoints': _n(5)},
        {'collapsedShapeKind': _s('circle'), 'collapsedShapePoints': _n(5)},
        {
          'controller': {
            'kind': 'dartObjectReferencePresence',
            'root': 'execute',
          },
        },
        {
          'onExpansionChanged': {'kind': 'callbackPresence'},
        },
      ]) {
        expect(
          () => _decode(_model(_tile(props: props))),
          throwsFormatException,
          reason: '$props',
        );
      }
    },
  );

  testWidgets(
    'every nullable direct SDK argument can reset independently without disabling header expansion',
    (tester) async {
      await _pump(
        tester,
        _tile(
          props: {
            for (final name in [
              'onExpansionChanged',
              'tilePadding',
              'expandedCrossAxisAlignment',
              'expandedAlignment',
              'childrenPadding',
              'backgroundColor',
              'collapsedBackgroundColor',
              'textColor',
              'collapsedTextColor',
              'iconColor',
              'collapsedIconColor',
              'shape',
              'collapsedShape',
              'clipBehavior',
              'controlAffinity',
              'controller',
              'dense',
              'splashColor',
              'visualDensity',
              'minTileHeight',
              'enableFeedback',
              'expansionAnimationStyle',
              'statesController',
            ])
              name: _null,
          },
        ),
      );
      final sdk = _sdk(tester);
      expect(sdk.enableFeedback, isNull);
      expect(sdk.enabled, true);
      expect(sdk.dense, isNull);
      expect(sdk.tilePadding, isNull);
      expect(sdk.childrenPadding, isNull);
      expect(sdk.expandedAlignment, isNull);
      expect(sdk.expandedCrossAxisAlignment, isNull);
      expect(sdk.clipBehavior, isNull);
      expect(sdk.onExpansionChanged, isNull);
      expect(sdk.statesController, isNull);
      expect(sdk.controller, isA<ExpansibleController>());
      expect(sdk.expansionAnimationStyle, isNull);
      expect(_messages(tester).trim(), isEmpty);
      await _pump(tester, _tile());
      expect(_sdk(tester).enableFeedback, true);
    },
  );

  testWidgets(
    'all five slots direct colors layout flags and leading arrow remain distinct when trailing is suppressed',
    (tester) async {
      final props = {
        'initiallyExpanded': _b(true),
        'maintainState': _b(true),
        'showTrailingIcon': _b(false),
        'enabled': _b(false),
        'dense': _b(true),
        'enableFeedback': _b(false),
        'controlAffinity': _e('ListTileControlAffinity', 'leading'),
        'expandedCrossAxisAlignment': _e('CrossAxisAlignment', 'end'),
        'expandedAlignment': {
          'kind': 'alignmentGeometry',
          'basis': 'directional',
          'horizontal': 1.0,
          'vertical': -.5,
        },
        'tilePadding': {
          'kind': 'edgeInsetsDirectional',
          'start': 7,
          'top': 3,
          'end': 9,
          'bottom': 4,
        },
        'childrenPadding': {
          'kind': 'edgeInsets',
          'left': 6,
          'top': 2,
          'right': 8,
          'bottom': 3,
        },
        'visualDensityHorizontal': _n(-1.0),
        'visualDensityVertical': _n(1.0),
        'minTileHeight': _n(80),
        'internalAddSemanticForOnTap': _b(true),
        'onExpansionChanged': _s('noop'),
        for (final name in [
          'backgroundColor',
          'collapsedBackgroundColor',
          'textColor',
          'collapsedTextColor',
          'iconColor',
          'collapsedIconColor',
          'splashColor',
        ])
          name: _c(0xff123456),
      };
      await _pump(
        tester,
        _tile(
          props: props,
          slots: {
            'subtitle': _single(_text(4, 'Subtitle')),
            'trailing': _single(_text(5, 'Stored trailing')),
            'children': _list([_text(3, 'Body')]),
          },
        ),
      );
      final sdk = _sdk(tester);
      expect(sdk.backgroundColor, const Color(0xff123456));
      expect(sdk.collapsedBackgroundColor, const Color(0xff123456));
      expect(sdk.textColor, const Color(0xff123456));
      expect(sdk.collapsedTextColor, const Color(0xff123456));
      expect(sdk.iconColor, const Color(0xff123456));
      expect(sdk.collapsedIconColor, const Color(0xff123456));
      expect(sdk.splashColor, const Color(0xff123456));
      expect(sdk.tilePadding, const EdgeInsetsDirectional.fromSTEB(7, 3, 9, 4));
      expect(sdk.childrenPadding, const EdgeInsets.fromLTRB(6, 2, 8, 3));
      expect(sdk.expandedAlignment, const AlignmentDirectional(1, -.5));
      expect(sdk.expandedCrossAxisAlignment, CrossAxisAlignment.end);
      expect(
        sdk.visualDensity,
        const VisualDensity(horizontal: -1, vertical: 1),
      );
      expect(sdk.minTileHeight, 80);
      expect(sdk.onExpansionChanged, isNotNull);
      expect(sdk.trailing, isNotNull);
      expect(find.text('Stored trailing', skipOffstage: false), findsNothing);
      expect(find.byIcon(Icons.expand_more), findsOneWidget);
      expect(
        tester.getCenter(find.byIcon(Icons.expand_more)).dx,
        lessThan(tester.getCenter(find.text('Title 2')).dx),
      );
      expect(sdk.controller!.isExpanded, true);
      await tester.tap(find.byIcon(Icons.expand_more));
      await tester.pumpAndSettle();
      expect(
        sdk.controller!.isExpanded,
        true,
      ); // disabled header, controller still usable.
      sdk.controller!.collapse();
      await tester.pumpAndSettle();
      expect(sdk.controller!.isExpanded, false);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'two independent shape families preserve all ten SDK constructors and real Material clipping',
    (tester) async {
      const shapes = [
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
      ];
      for (final shape in shapes) {
        for (final collapsed in shapes) {
          await _pump(
            tester,
            _tile(
              props: {
                'shapeKind': _s(shape),
                'collapsedShapeKind': _s(collapsed),
                'clipBehavior': _e('Clip', 'hardEdge'),
              },
              slots: {
                'children': _list([_text(3, 'Body')]),
              },
            ),
          );
          final sdk = _sdk(tester);
          expect(sdk.shape, isA<ShapeBorder>());
          expect(sdk.collapsedShape, isA<ShapeBorder>());
          final material = tester
              .widgetList<Material>(
                find.descendant(
                  of: find.byType(ExpansionTile),
                  matching: find.byType(Material),
                ),
              )
              .first;
          expect(material.clipBehavior, Clip.hardEdge);
          sdk.controller!.expand();
          await tester.pumpAndSettle();
          sdk.controller!.collapse();
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull, reason: '$shape/$collapsed');
          expect(_messages(tester).trim(), isEmpty);
        }
      }
    },
  );

  testWidgets(
    'whole project references and callback presence never execute or replace actual title/controller state',
    (tester) async {
      final slots = {
        'title': _single(_node(3, 'flutter.material.TextField', {})),
      };
      await _pump(tester, _tile(slots: slots));
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      final controller = _sdk(tester).controller!;
      edit.widget.controller.text = 'Keep';
      controller.expand();
      await tester.pumpAndSettle();
      await _pump(
        tester,
        _tile(
          props: {
            for (final name in [
              'shape',
              'collapsedShape',
              'controller',
              'statesController',
              'visualDensity',
              'expandedAlignment',
              'tilePadding',
              'childrenPadding',
              'backgroundColor',
              'collapsedBackgroundColor',
              'textColor',
              'collapsedTextColor',
              'iconColor',
              'collapsedIconColor',
              'splashColor',
              'expansionAnimationStyle',
              'onExpansionChanged',
            ])
              name: _ref,
          },
          slots: slots,
        ),
      );
      expect(_sdk(tester).controller, same(controller));
      expect(controller.isExpanded, true);
      expect(_sdk(tester).statesController, isNull);
      expect(_sdk(tester).shape, isNull);
      expect(_sdk(tester).collapsedShape, isNull);
      expect(_sdk(tester).onExpansionChanged, isNotNull);
      expect(_messages(tester), contains('never executes project Dart'));
      expect(
        tester.state<EditableTextState>(find.byType(EditableText)),
        same(edit),
      );
      expect(edit.widget.controller.text, 'Keep');
      await _pump(tester, _tile(slots: slots));
      expect(_sdk(tester).controller, same(controller));
      expect(_messages(tester).trim(), isEmpty);
    },
  );

  for (final rtl in [false, true]) {
    testWidgets(
      'header slots and expanded empty-body append band keep exact drop geometry RTL=$rtl',
      (tester) async {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        await _pump(
          tester,
          _tile(),
          rtl: rtl,
          onDrop: (r) => drop = r,
          onMove: (r) => move = r,
        );
        final source = CanvasPaletteDragSource(
          token: 'text',
          widgetType: 'flutter.widgets.Text',
          traits: const {},
        );
        CanvasDropTarget? at(Offset point) {
          final surface = tester.getRect(find.byType(CanvasDocumentView));
          return drop!(
            ((point.dx - surface.left) / surface.width * 1000000).round(),
            ((point.dy - surface.top) / surface.height * 1000000).round(),
            source,
          );
        }

        expect(
          move!(_id(102), _id(2), 'children', 0),
          isNull,
        ); // Required Title cannot be moved out.
        expect(move!(_id(102), _id(2), 'title', 0)?.slotName, 'title');
        expect(canvasDropSlotsForWidgetType(_type).map((s) => s.slotName), [
          'title',
          'leading',
          'subtitle',
          'trailing',
          'children',
        ]);
        var header = tester.getRect(find.byType(ListTile));
        for (final entry in {
          'leading': Offset(
            rtl ? header.right - 4 : header.left + 4,
            header.center.dy,
          ),
          'trailing': Offset(
            rtl ? header.left + 4 : header.right - 4,
            header.center.dy,
          ),
          'subtitle': Offset(header.center.dx, header.bottom - 4),
        }.entries) {
          expect(at(entry.value)?.parentWidgetId, _id(2));
          expect(at(entry.value)?.slotName, entry.key);
        }
        final collapsedSize = tester.getSize(find.byType(ExpansionTile));
        _sdk(tester).controller!.expand();
        await tester.pumpAndSettle();
        header = tester.getRect(find.byType(ListTile));
        expect(tester.getSize(find.byType(ExpansionTile)), collapsedSize);
        expect(
          at(Offset(header.center.dx, header.bottom - 2))?.slotName,
          'children',
        );
        expect(
          at(Offset(header.center.dx, header.bottom - 2))?.insertionIndex,
          0,
        );
      },
    );
  }

  testWidgets(
    'collapsed maintained descendants have no synthetic handles selection outline hit or drop geometry',
    (tester) async {
      CanvasDropResolver? drop;
      CanvasMovePreviewResolver? move;
      final tile = _tile(
        props: {'initiallyExpanded': _b(true), 'maintainState': _b(true)},
        slots: {
          'children': _list([
            _node(
              3,
              'flutter.widgets.Column',
              {'mainAxisSize': _e('MainAxisSize', 'min')},
              {
                'children': _list([
                  _text(4, 'Hidden child'),
                  _node(5, 'flutter.widgets.SizedBox', {
                    'width': _n(0.0),
                    'height': _n(0.0),
                  }),
                ]),
              },
            ),
          ]),
        },
      );
      await _pump(
        tester,
        tile,
        selected: _id(4),
        onDrop: (r) => drop = r,
        onMove: (r) => move = r,
      );
      expect(
        find.byKey(ValueKey('canvas-selection-outline-${_id(4)}')),
        findsOneWidget,
      );
      _sdk(tester).controller!.collapse();
      await tester.pumpAndSettle();
      expect(find.text('Hidden child'), findsNothing);
      expect(find.text('Hidden child', skipOffstage: false), findsOneWidget);
      expect(
        find.byKey(ValueKey('canvas-selection-outline-${_id(4)}')),
        findsNothing,
      );
      expect(
        find.byKey(ValueKey('canvas-zero-size-widget-target-${_id(5)}')),
        findsNothing,
      );
      expect(move!(_id(102), _id(3), 'children', 0), isNull);
      expect(drop, isNotNull);
    },
  );

  testWidgets(
    'tile title ancestor and cleared selection retain transient expansion SDK State and editable body',
    (tester) async {
      final tile = _tile(
        slots: {
          'children': _list([_node(3, 'flutter.material.TextField', {})]),
        },
      );
      await _pump(tester, tile);
      final controller = _sdk(tester).controller!;
      final state = tester.state(find.byType(ExpansionTile));
      controller.expand();
      await tester.pumpAndSettle();
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      edit.widget.controller.text = 'Selection keeps body';
      for (final selected in [_id(2), _id(102), _id(900), null]) {
        await _pump(tester, tile, selected: selected);
        expect(_sdk(tester).controller, same(controller));
        expect(tester.state(find.byType(ExpansionTile)), same(state));
        expect(controller.isExpanded, true);
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(edit.widget.controller.text, 'Selection keeps body');
        expect(
          find.byKey(ValueKey('canvas-selection-outline-${_id(2)}')),
          selected == _id(2) ? findsOneWidget : findsNothing,
        );
      }
    },
  );

  testWidgets(
    'native header focus Enter Space and semantic actions remain SDK owned while disabled header cannot expand',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        await _pump(
          tester,
          _tile(
            slots: {
              'children': _list([_text(3, 'Keyboard body')]),
            },
          ),
        );
        final controller = _sdk(tester).controller!;
        final focus = Focus.of(tester.element(find.byIcon(Icons.expand_more)));
        focus.requestFocus();
        await tester.pump();
        expect(focus.hasFocus, true);
        final data = tester
            .getSemantics(find.byType(ListTile))
            .getSemanticsData();
        expect(data.hasAction(ui.SemanticsAction.tap), true);
        expect(data.hasFlag(ui.SemanticsFlag.hasCheckedState), false);
        expect(data.hasFlag(ui.SemanticsFlag.hasToggledState), false);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.pumpAndSettle();
        expect(controller.isExpanded, true);
        expect(find.text('Keyboard body'), findsOneWidget);
        await tester.sendKeyEvent(LogicalKeyboardKey.space);
        await tester.pumpAndSettle();
        expect(controller.isExpanded, false);
        await _pump(
          tester,
          _tile(props: {'enabled': _b(false), 'onExpansionChanged': _ref}),
        );
        expect(_sdk(tester).controller, same(controller));
        expect(tester.widget<ListTile>(find.byType(ListTile)).enabled, false);
        await tester.tap(find.byIcon(Icons.expand_more));
        await tester.pumpAndSettle();
        expect(controller.isExpanded, false);
        controller.expand();
        await tester.pumpAndSettle();
        expect(controller.isExpanded, true);
        expect(tester.takeException(), isNull);
      } finally {
        semantics.dispose();
      }
    },
  );

  testWidgets(
    'nested tiles own independent controllers and retained nested expansion survives parent collapse and theme edits',
    (tester) async {
      final tile = _tile(
        props: {'initiallyExpanded': _b(true), 'maintainState': _b(true)},
        slots: {
          'children': _list([
            _tile(
              id: 3,
              props: {'maintainState': _b(true)},
              slots: {
                'children': _list([_text(4, 'Nested body')]),
              },
            ),
          ]),
        },
      );
      await _pump(tester, tile);
      final controllers = tester
          .widgetList<ExpansionTile>(find.byType(ExpansionTile))
          .map((w) => w.controller!)
          .toList();
      expect(identical(controllers[0], controllers[1]), false);
      controllers[1].expand();
      await tester.pumpAndSettle();
      controllers[0].collapse();
      await tester.pumpAndSettle();
      expect(find.text('Nested body'), findsNothing);
      expect(find.text('Nested body', skipOffstage: false), findsOneWidget);
      await _pump(
        tester,
        tile,
        theme: ThemeData(
          expansionTileTheme: const ExpansionTileThemeData(
            backgroundColor: Colors.amber,
            expansionAnimationStyle: AnimationStyle(
              curve: Curves.elasticIn,
              reverseDuration: Duration(microseconds: -1),
            ),
          ),
        ),
      );
      expect(controllers[0].isExpanded, false);
      expect(controllers[1].isExpanded, true);
      controllers[0].expand();
      await tester.pumpAndSettle();
      expect(find.text('Nested body'), findsOneWidget);
      expect(
        tester
            .widgetList<ExpansionTile>(find.byType(ExpansionTile))
            .map((w) => w.controller)
            .toList(),
        controllers,
      );
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pump();
      for (final controller in controllers) {
        expect(() => controller.addListener(() {}), throwsFlutterError);
      }
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'negative and overflowing padding approximations retain actual expanded children and reset exact layout',
    (tester) async {
      final slots = {
        'children': _list([_node(3, 'flutter.material.TextField', {})]),
      };
      await _pump(
        tester,
        _tile(props: {'initiallyExpanded': _b(true)}, slots: slots),
      );
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      edit.widget.controller.text = 'Geometry retained';
      final controller = _sdk(tester).controller!;
      for (final value in [-1.0, 1.7e308]) {
        await _pump(
          tester,
          _tile(
            props: {
              'initiallyExpanded': _b(true),
              for (final name in ['tilePadding', 'childrenPadding'])
                name: {
                  'kind': 'edgeInsets',
                  'left': value,
                  'top': 0,
                  'right': value,
                  'bottom': 0,
                },
            },
            slots: slots,
          ),
        );
        expect(_sdk(tester).tilePadding, EdgeInsets.zero);
        expect(_sdk(tester).childrenPadding, EdgeInsets.zero);
        expect(_sdk(tester).controller, same(controller));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(_messages(tester), contains('zero approximation'));
      }
      await _pump(
        tester,
        _tile(props: {'initiallyExpanded': _b(true)}, slots: slots),
      );
      expect(edit.widget.controller.text, 'Geometry retained');
      expect(_sdk(tester).childrenPadding, isNull);
      expect(_messages(tester).trim(), isEmpty);
    },
  );

  testWidgets(
    'unsafe tile height and unbounded width retain mounted title and expanded body without painted hit geometry',
    (tester) async {
      final slots = {
        'title': _single(_node(3, 'flutter.material.TextField', {})),
        'children': _list([_node(4, 'flutter.material.TextField', {})]),
      };
      await _pump(
        tester,
        _tile(props: {'initiallyExpanded': _b(true)}, slots: slots),
      );
      final edits = tester
          .stateList<EditableTextState>(find.byType(EditableText))
          .toList();
      for (final value in ['infinity', 'nan']) {
        await _pump(
          tester,
          _tile(
            props: {
              'initiallyExpanded': _b(true),
              'minTileHeight': _e('double', value),
            },
            slots: slots,
          ),
        );
        expect(
          tester
              .stateList<EditableTextState>(find.byType(EditableText))
              .toList(),
          edits,
        );
        expect(_sdk(tester).minTileHeight, 0);
        expect(_messages(tester), contains('zero approximation'));
      }
      await _pump(
        tester,
        _tile(props: {'initiallyExpanded': _b(true)}, slots: slots),
      );
      expect(
        tester.stateList<EditableTextState>(find.byType(EditableText)).toList(),
        edits,
      );
      expect(_messages(tester).trim(), isEmpty);
      await _pump(
        tester,
        _node(
          5,
          'flutter.widgets.Row',
          {'mainAxisSize': _e('MainAxisSize', 'min')},
          {
            'children': _list([_tile()]),
          },
        ),
      );
      expect(find.byType(ExpansionTile), findsOneWidget);
      expect(_messages(tester), contains('unbounded width'));
    },
  );

  testWidgets(
    'real shape Material branch mounts without ancestor while absent Material remains explicitly unavailable',
    (tester) async {
      Future<void> bare(Map<String, Object?> props) async {
        final model = _model(null)..['root'] = _tile(props: props);
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: _decode(model),
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        );
        await tester.pump();
        await tester.pump();
        expect(tester.takeException(), isNull);
      }

      await bare({});
      expect(find.byType(ExpansionTile), findsNothing);
      expect(_messages(tester), contains('No synthetic Material'));
      await bare({'shapeKind': _s('roundedRectangle')});
      expect(find.byType(ExpansionTile), findsOneWidget);
      final materials = tester.widgetList<Material>(
        find.descendant(
          of: find.byType(ExpansionTile),
          matching: find.byType(Material),
        ),
      );
      expect(materials.any((m) => m.clipBehavior == Clip.antiAlias), true);
      _sdk(tester).controller!.expand();
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    },
  );

  for (final maintain in [false, true]) {
    testWidgets(
      'maintainState=$maintain preserves exact SDK child disposal or offstage ticker retention',
      (tester) async {
        final tile = _tile(
          props: {'initiallyExpanded': _b(true), 'maintainState': _b(maintain)},
          slots: {
            'children': _list([_node(3, 'flutter.material.TextField', {})]),
          },
        );
        await _pump(tester, tile);
        final edit = tester.state<EditableTextState>(find.byType(EditableText));
        edit.widget.controller.text = 'Retain only when maintained';
        final controller = _sdk(tester).controller!;
        controller.collapse();
        await tester.pumpAndSettle();
        expect(find.byType(EditableText), findsNothing);
        expect(
          find.byType(EditableText, skipOffstage: false),
          maintain ? findsOneWidget : findsNothing,
        );
        expect(edit.mounted, maintain);
        if (maintain) {
          expect(
            TickerMode.of(
              tester.element(find.byType(EditableText, skipOffstage: false)),
            ),
            false,
          );
        }
        controller.expand();
        await tester.pumpAndSettle();
        final current = tester.state<EditableTextState>(
          find.byType(EditableText),
        );
        expect(identical(current, edit), maintain);
        expect(
          current.widget.controller.text,
          maintain ? 'Retain only when maintained' : '',
        );
        expect(tester.takeException(), isNull);
      },
    );
  }
}
