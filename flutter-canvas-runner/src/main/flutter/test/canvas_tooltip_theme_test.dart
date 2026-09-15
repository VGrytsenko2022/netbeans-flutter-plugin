// Fresh SDK construction deliberately preserves all behavior fields.
// ignore_for_file: deprecated_member_use
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.TooltipTheme';
Map<String, Set<String>> _reviewedProperties() {
  final contract = canvasRuntimeWidgetSchemaContractForTesting();
  final start = contract.indexOf('W|$_type\n');
  return {
    for (final line
        in contract
            .substring(start, contract.indexOf('W|', start + 2))
            .split('\n'))
      if (line.startsWith('P|'))
        line.split('|')[1]: line.split('|')[2].split(',').toSet(),
  };
}

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
Map<String, Object?> _node(
  int id,
  String type,
  Map<String, Object?> props, [
  Map<String, Object?> slots = const {},
]) => {'id': _id(id), 'type': type, 'properties': props, 'slots': slots};
Map<String, Object?> _text() =>
    _node(4, 'flutter.widgets.Text', {'data': _s('Anchor')});
Map<String, Object?> _tooltip({
  Map<String, Object?> props = const {},
  Object? child,
}) => _node(
  3,
  'flutter.material.Tooltip',
  {'message': _s('Themed Tooltip'), ...props},
  {'child': _single(child ?? _text())},
);
Map<String, Object?> _scope(
  Map<String, Object?> props, {
  Object? child,
  int id = 2,
}) => _node(id, _type, props, {'child': _single(child ?? _tooltip())});
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

Finder _tip([String message = 'Themed Tooltip']) =>
    find.byWidgetPredicate((w) => w is Tooltip && w.message == message);
TooltipState _state(WidgetTester tester, [String message = 'Themed Tooltip']) =>
    tester.state<TooltipState>(_tip(message));
TooltipThemeData _data(
  WidgetTester tester, [
  String message = 'Themed Tooltip',
]) => TooltipTheme.of(tester.element(_tip(message)));
String _messages(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((w) => w.message ?? '')
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Object root, {
  String? selected,
  bool inline = false,
  CanvasImageResourceBundle? images,
  ValueChanged<CanvasDropResolver?>? onDrop,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: CanvasDocumentView(
        model: _model(root),
        selectedWidgetId: selected,
        onSelected: (_) {},
        inlineTextEditEnabled: inline,
        imageResources: images,
        onDropResolverChanged: onDrop,
      ),
    ),
  );
  await tester.pump();
  await tester.pump();
  expect(tester.takeException(), isNull);
}

Future<void> _show(
  WidgetTester tester, [
  String message = 'Themed Tooltip',
]) async {
  _state(tester, message).ensureTooltipVisible();
  await tester.pumpAndSettle();
  expect(tester.takeException(), isNull);
  expect(find.text(message), findsOneWidget);
}

Map<String, Object?> _inset(num n, {bool directional = false}) => {
  'kind': directional ? 'edgeInsetsDirectional' : 'edgeInsets',
  directional ? 'start' : 'left': n,
  'top': n,
  directional ? 'end' : 'right': n,
  'bottom': n,
};
const _fields = [
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
];
Map<String, Object?> _decoration() => {
  'kind': 'boxDecoration',
  'color': {'kind': 'literal', 'argb': '0xFFABCDEF'},
  'image': null,
  'borderRadius': null,
  'boxShadow': <Object>[],
  'gradient': null,
  'backgroundBlendMode': null,
  'shape': 'rectangle',
  'border': null,
};

Finder _anchor(int id) => find.byKey(ValueKey('canvas-widget-${_id(id)}'));
double? _baseline(RenderBox box, TextBaseline baseline) {
  final previous = RenderObject.debugCheckingIntrinsics;
  RenderObject.debugCheckingIntrinsics = true;
  try {
    return box.getDistanceToBaseline(baseline, onlyReal: true);
  } finally {
    RenderObject.debugCheckingIntrinsics = previous;
  }
}

List<Object?> _dropSamples(CanvasDropResolver resolver) => [
  for (final x in [0, 250000, 490000, 500000, 510000, 750000, 1000000])
    for (final y in [0, 250000, 490000, 500000, 510000, 750000, 1000000])
      (() {
        final t = resolver(
          x,
          y,
          CanvasPaletteDragSource(
            token: 'theme-geometry',
            widgetType: 'flutter.widgets.Text',
            traits: {},
          ),
        );
        return t == null
            ? null
            : [
                t.parentWidgetId,
                t.slotName,
                t.insertionIndex,
                t.zone?.leftMicros,
                t.zone?.topMicros,
                t.zone?.rightMicros,
                t.zone?.bottomMicros,
              ];
      })(),
];
List<SemanticsData> _semantics(WidgetTester tester) {
  final list = <SemanticsData>[];
  void visit(SemanticsNode node) {
    list.add(node.getSemanticsData());
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(tester.binding.pipelineOwner.semanticsOwner!.rootSemanticsNode!);
  return list;
}

int _tooltipSemanticsCount(WidgetTester tester) =>
    _semantics(tester).where((data) => data.tooltip == 'Themed Tooltip').length;
String _semanticLabels(WidgetTester tester) =>
    _semantics(tester).map((data) => data.label).join('\n');
void main() {
  testWidgets(
    'structured theme decorations preserve all gradient families directional geometry and bounded image resources',
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
          final root = _scope({
            'decoration': _compoundDecoration(
              gradient,
              directional,
              resourceId,
            ),
          });
          final before = jsonEncode(root);
          await _pump(tester, root, images: images);
          final decoration = _data(tester).decoration! as BoxDecoration;
          expect(
            decoration.color,
            Theme.of(tester.element(_tip())).colorScheme.surface,
          );
          expect(
            decoration.border,
            directional ? isA<BorderDirectional>() : isA<Border>(),
          );
          expect(
            decoration.borderRadius,
            directional ? isA<BorderRadiusDirectional>() : isA<BorderRadius>(),
          );
          expect(decoration.gradient, switch (gradient) {
            'linear' => isA<LinearGradient>(),
            'radial' => isA<RadialGradient>(),
            _ => isA<SweepGradient>(),
          });
          expect(decoration.boxShadow, hasLength(1));
          expect(decoration.backgroundBlendMode, BlendMode.srcOver);
          final image = decoration.image!;
          expect(image.image, isA<MemoryImage>());
          expect(image.onError, isNotNull);
          expect(image.colorFilter, isNotNull);
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
          await _show(tester);
          expect(
            find.byWidgetPredicate(
              (widget) =>
                  widget is Container && widget.decoration == decoration,
            ),
            findsOneWidget,
          );
          expect(jsonEncode(root), before);
        }
      }
    },
  );
  for (final intrinsic in [
    null,
    'flutter.widgets.IntrinsicWidth',
    'flutter.widgets.IntrinsicHeight',
  ]) {
    testWidgets(
      'theme diagnostics and selection preserve baselines intrinsic geometry and drop zones $intrinsic',
      (tester) async {
        final text = _node(4, 'flutter.widgets.Text', {
          'data': _s('Ag'),
          'styleFontSize': _n(48.0),
        });
        List<double?>? baselines;
        List<double>? sizes;
        Rect? rect;
        List<Object?>? drops;
        for (final fields in [
          <String, Object?>{},
          {'data': _ref},
          {'textStyleFontSize': _n(20.0)},
          <String, Object?>{},
        ]) {
          for (final selected in [_id(2), _id(3), _id(4), null]) {
            final scope = _scope(fields, child: _tooltip(child: text));
            final root = intrinsic == null
                ? scope
                : _node(6, intrinsic, {}, {'child': _single(scope)});
            CanvasDropResolver? resolver;
            await _pump(
              tester,
              root,
              selected: selected,
              onDrop: (value) => resolver = value,
            );
            final box = tester.renderObject<RenderBox>(_anchor(2));
            final actual = [
              for (final baseline in TextBaseline.values) ...[
                _baseline(box, baseline),
                box.getDryBaseline(box.constraints, baseline),
              ],
            ];
            final current = [
              box.size.width,
              box.size.height,
              box.getMinIntrinsicWidth(double.infinity),
              box.getMaxIntrinsicWidth(double.infinity),
              box.getMinIntrinsicHeight(box.size.width),
              box.getMaxIntrinsicHeight(box.size.width),
            ];
            baselines ??= actual;
            sizes ??= current;
            rect ??= tester.getRect(_anchor(2));
            expect(actual, baselines);
            expect(current, sizes);
            expect(tester.getRect(_anchor(2)), rect);
            expect(tester.getRect(_anchor(4)), rect);
            expect(resolver, isNotNull);
            final sampled = _dropSamples(resolver!);
            drops ??= sampled;
            expect(sampled, drops);
          }
        }
      },
    );
  }
  testWidgets(
    'empty required theme child keeps zero geometry and no invented insertion child',
    (tester) async {
      final child = _node(4, 'flutter.widgets.SizedBox', {
        'width': _n(0),
        'height': _n(0),
      });
      for (final props in [
        <String, Object?>{},
        {'data': _ref},
        <String, Object?>{},
      ]) {
        await _pump(tester, _scope(props, child: child), selected: _id(2));
        expect(tester.getSize(_anchor(2)), Size.zero);
        expect(tester.getSize(_anchor(4)), Size.zero);
        final box = tester.renderObject<RenderBox>(_anchor(2));
        expect(_baseline(box, TextBaseline.alphabetic), isNull);
        expect(box.getMaxIntrinsicWidth(double.infinity), 0);
        expect(canvasDropSlotsForWidgetType(_type), isEmpty);
        expect(
          canvasExistingChildWrapTargetSlot(
            parentWidgetType: _type,
            slotName: 'child',
          )!.accepts(currentChildCount: 1, insertionIndex: 0),
          true,
        );
      }
    },
  );
  testWidgets(
    'inline editing focus and text survive theme warning appearance',
    (tester) async {
      await _pump(tester, _scope({}), selected: _id(4), inline: true);
      await tester.tap(_anchor(4));
      await tester.pump(const Duration(milliseconds: 80));
      await tester.tap(_anchor(4));
      await tester.pumpAndSettle();
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      expect(edit.widget.focusNode.hasFocus, true);
      final tooltip = _state(tester);
      edit.widget.controller.text = 'Edited theme anchor';
      for (final fields in [
        <String, Object?>{'padding': _inset(8)},
        {'data': _ref},
        <String, Object?>{},
      ]) {
        await _pump(tester, _scope(fields), selected: _id(4), inline: true);
        expect(_state(tester), same(tooltip));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(edit.widget.focusNode.hasFocus, true);
        expect(edit.widget.controller.text, 'Edited theme anchor');
        await _show(tester);
      }
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pumpAndSettle();
    },
  );
  testWidgets(
    'theme wrap unwrap and move safely close affected overlays without model changes',
    (tester) async {
      final tooltip = _tooltip(
        child: _node(4, 'flutter.material.TextField', {'maxLength': _n(25)}),
      );
      for (final root in [
        tooltip,
        _scope({}, child: tooltip),
        _node(
          6,
          'flutter.widgets.Padding',
          {'padding': _inset(4)},
          {'child': _single(_scope({}, child: tooltip))},
        ),
        tooltip,
      ]) {
        final before = jsonEncode(root);
        await _pump(tester, root);
        expect(find.text('Themed Tooltip'), findsNothing);
        expect(_anchor(4), findsOneWidget);
        expect(tester.widget<TextField>(find.byType(TextField)).maxLength, 25);
        await _show(tester);
        expect(jsonEncode(root), before);
      }
    },
  );
  testWidgets(
    'nearest nullable semantics behavior matches native replacement and explicit override',
    (tester) async {
      final handle = tester.ensureSemantics();
      try {
        for (final excluded in [true, false, null]) {
          await _pump(
            tester,
            _scope(
              {'excludeFromSemantics': _b(true)},
              child: _scope({
                'excludeFromSemantics': excluded == null ? _null : _b(excluded),
              }, id: 5),
            ),
          );
          expect(_tooltipSemanticsCount(tester), excluded == true ? 0 : 1);
          expect(_semanticLabels(tester).contains('Anchor'), true);
        }
        await _pump(
          tester,
          _scope({
            'excludeFromSemantics': _b(true),
          }, child: _tooltip(props: {'excludeFromSemantics': _b(false)})),
        );
        expect(_tooltipSemanticsCount(tester), 1);
      } finally {
        handle.dispose();
      }
    },
  );
  test(
    'independent 47-field required-wrapper schema closes exact 100/4561 catalog',
    () {
      final start = canvasReviewedWidgetSchemaContract.indexOf('W|$_type\n');
      final section = canvasReviewedWidgetSchemaContract.substring(
        start,
        canvasReviewedWidgetSchemaContract.indexOf('W|', start + 2),
      );
      expect(utf8.encode(section), hasLength(18527));
      expect(
        sha256Hex(utf8.encode(section)),
        '41c1c3c9fc62d1fb4a8183f467ee62d1321af1c8351901d5a485aa70e6a44ee4',
      );
      expect(_reviewedProperties(), hasLength(47));
      expect(
        canvasRuntimeWidgetSchemaContractForTesting(),
        canvasReviewedWidgetSchemaContract.trimLeft(),
      );
      expect(
        RegExp(
          r'^W\|',
          multiLine: true,
        ).allMatches(canvasReviewedWidgetSchemaContract),
        hasLength(244),
      );
      expect(
        RegExp(
          r'^P\|',
          multiLine: true,
        ).allMatches(canvasReviewedWidgetSchemaContract),
        hasLength(8177),
      );
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(canvasReviewedRequiredWrapperSlot(_type), 'child');
      expect(canvasModelProtocolVersion, 20);
    },
  );
  test(
    'whole-data nonnull reference and all nullable fields strictly reject conflicting or executable wire',
    () {
      for (final props in [
        <String, Object?>{},
        {'data': _ref},
        {for (final field in _fields) field: _null},
      ]) {
        expect(() => _model(_scope(props)), returnsNormally);
      }
      for (final field in [..._fields, ..._style().keys]) {
        expect(
          () => _model(_scope({'data': _ref, field: _null})),
          throwsFormatException,
        );
      }
      for (final props in [
        {'data': _null},
        {'data': _s('projectTheme')},
        {
          'data': {'kind': 'callbackPresence'},
        },
        {
          'data': {'kind': 'dartObjectReferencePresence', 'symbol': 'secret'},
        },
        {'height': _n(1), 'constraints': _constraints()},
        {'textStyle': _null, 'textStyleFontSize': _n(18)},
        {'waitDurationUs': _n(1.5)},
        {'waitDurationUs': _n(9007199254740992)},
        {'message': _s('not a theme field')},
        {'enableTapToDismiss': _b(true)},
        {
          'textStyleForeground': _paint(),
          'textStyleColor': _color('0xFFFFFFFF'),
        },
      ]) {
        expect(() => _model(_scope(props)), throwsFormatException);
      }
      expect(() => _model(_node(2, _type, {})), throwsFormatException);
      expect(
        () => _model(_node(2, _type, {}, {'child': _single(null)})),
        throwsFormatException,
      );
    },
  );
  testWidgets(
    'fresh constructor preserves all fifteen fields and all local TextStyle leaves',
    (tester) async {
      final style = _style();
      expect({
        ...style.keys,
        'textStyleForeground',
        'textStyleBackground',
      }, hasLength(31));
      for (final paints in [false, true]) {
        final props = <String, Object?>{
          'constraints': _constraints(),
          'padding': _inset(5),
          'margin': _inset(7, directional: true),
          'verticalOffset': _n(33),
          'preferBelow': _b(false),
          'excludeFromSemantics': _b(true),
          'decoration': _decoration(),
          'textAlign': _e('TextAlign', 'end'),
          'waitDurationUs': _n(80000),
          'showDurationUs': _n(190000),
          'exitDurationUs': _n(110000),
          'triggerMode': _e('TooltipTriggerMode', 'tap'),
          'enableFeedback': _b(false),
          ...style,
        };
        if (paints) {
          props.remove('textStyleColor');
          props.remove('textStyleBackgroundColor');
          props['textStyleForeground'] = _paint();
          props['textStyleBackground'] = _paint();
        }
        final root = _scope(props);
        final before = jsonEncode(root);
        await _pump(tester, root);
        final data = _data(tester);
        expect(data.height, isNull);
        expect(
          data.constraints,
          const BoxConstraints(maxWidth: 220, maxHeight: 100),
        );
        expect(data.padding, const EdgeInsets.all(5));
        expect(data.margin, const EdgeInsetsDirectional.all(7));
        expect(data.verticalOffset, 33);
        expect(data.preferBelow, false);
        expect(data.excludeFromSemantics, true);
        expect(
          data.decoration,
          const BoxDecoration(color: Color(0xFFABCDEF), boxShadow: []),
        );
        expect(data.textAlign, TextAlign.end);
        expect(data.waitDuration, const Duration(milliseconds: 80));
        expect(data.showDuration, const Duration(milliseconds: 190));
        expect(data.exitDuration, const Duration(milliseconds: 110));
        expect(data.triggerMode, TooltipTriggerMode.tap);
        expect(data.enableFeedback, false);
        final actual = data.textStyle!;
        expect(actual.fontSize, 18.5);
        expect(actual.fontWeight, FontWeight.w600);
        expect(actual.fontStyle, FontStyle.italic);
        expect(actual.inherit, false);
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
        expect(actual.shadows, hasLength(1));
        expect(actual.fontFeatures, const [ui.FontFeature('liga', 1)]);
        expect(actual.fontVariations, const [ui.FontVariation('wght', 700)]);
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
        expect(actual.debugLabel, contains('tooltip style'));
        expect(actual.fontFamily, 'packages/design_fonts/Inter');
        expect(actual.fontFamilyFallback, [
          'packages/design_fonts/Noto Sans',
          'packages/design_fonts/Noto Color Emoji',
        ]);
        expect(actual.overflow, TextOverflow.fade);
        if (paints) {
          expect(actual.foreground!.strokeWidth, 1.5);
          expect(actual.background!.style, PaintingStyle.stroke);
        } else {
          expect(actual.color, const Color(0xFF123456));
          expect(actual.backgroundColor, const Color(0x44123456));
        }
        await _show(tester);
        expect(jsonEncode(root), before);
      }
      await _pump(tester, _scope({'height': _n(37), 'textStyle': _null}));
      expect(_data(tester).height, 37);
      expect(_data(tester).textStyle, isNull);
      await _show(tester);
    },
  );
  testWidgets(
    'empty or all-null nearest theme replaces outer data and explicit Tooltip values win',
    (tester) async {
      for (final inner in [
        <String, Object?>{},
        {for (final f in _fields) f: _null},
      ]) {
        await _pump(
          tester,
          _scope({
            'padding': _inset(17),
            'preferBelow': _b(false),
            'triggerMode': _e('TooltipTriggerMode', 'tap'),
            'exitDurationUs': _n(777),
          }, child: _scope(inner, id: 5)),
        );
        expect(_data(tester), const TooltipThemeData());
        await _show(tester);
      }
      await _pump(
        tester,
        _scope(
          {
            'padding': _inset(17),
            'verticalOffset': _n(45),
            'triggerMode': _e('TooltipTriggerMode', 'longPress'),
          },
          child: _tooltip(
            props: {
              'padding': _inset(3),
              'verticalOffset': _n(9),
              'triggerMode': _e('TooltipTriggerMode', 'manual'),
            },
          ),
        ),
      );
      final sdk = tester.widget<Tooltip>(_tip());
      expect(sdk.padding, const EdgeInsets.all(3));
      expect(sdk.verticalOffset, 9);
      expect(sdk.triggerMode, TooltipTriggerMode.manual);
      expect(_data(tester).padding, const EdgeInsets.all(17));
      await _show(tester);
    },
  );
  testWidgets(
    'unknown whole data and nullable typed local references are anonymous explicit approximations',
    (tester) async {
      final refFields = _reviewedProperties().entries
          .where((e) => e.value.contains('dartObjectReference'))
          .map((e) => e.key)
          .toList();
      expect(
        refFields,
        containsAll([
          'data',
          'decoration',
          'textStyle',
          'constraints',
          'waitDurationUs',
          'showDurationUs',
          'exitDurationUs',
        ]),
      );
      for (final field in refFields) {
        final root = _scope({field: _ref});
        final before = jsonEncode(root);
        await _pump(tester, root);
        expect(_messages(tester), contains('preview limitation for $field'));
        if (field == 'data') expect(_data(tester), const TooltipThemeData());
        await _show(tester);
        expect(jsonEncode(root), before);
      }
    },
  );
  testWidgets(
    'ordinary theme fields and unknown data warning updates retain open Tooltip and editor State',
    (tester) async {
      final field = _node(4, 'flutter.material.TextField', {});
      TooltipState? tooltip;
      EditableTextState? edit;
      for (final props in [
        <String, Object?>{},
        {'padding': _inset(4), 'exitDurationUs': _n(99999)},
        {'preferBelow': _b(false), 'textStyleFontSize': _n(20.0)},
        {'data': _ref},
        <String, Object?>{},
      ]) {
        await _pump(
          tester,
          _scope(props, child: _tooltip(child: field)),
          selected: _id(2),
        );
        tooltip ??= _state(tester);
        edit ??= tester.state<EditableTextState>(find.byType(EditableText));
        expect(_state(tester), same(tooltip));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        edit.widget.controller.text = 'Retained';
        await _show(tester);
        expect(edit.widget.controller.text, 'Retained');
      }
    },
  );
  for (final mode in ['tap', 'longPress', 'manual']) {
    testWidgets(
      'native $mode trigger and hover use inherited theme with exact wait and exit timings',
      (tester) async {
        await _pump(
          tester,
          _scope({
            'triggerMode': _e('TooltipTriggerMode', mode),
            'waitDurationUs': _n(80000),
            'exitDurationUs': _n(110000),
            'showDurationUs': _n(190000),
          }),
        );
        if (mode == 'tap') {
          await tester.tap(find.text('Anchor'));
        } else if (mode == 'longPress') {
          await tester.longPress(find.text('Anchor'));
        } else {
          _state(tester).ensureTooltipVisible();
        }
        await tester.pump(const Duration(milliseconds: 151));
        expect(find.text('Themed Tooltip'), findsOneWidget);
        Tooltip.dismissAllToolTips();
        await tester.pumpAndSettle();
        final mouse = await tester.createGesture(
          kind: ui.PointerDeviceKind.mouse,
        );
        await mouse.addPointer(location: Offset.zero);
        await mouse.moveTo(tester.getCenter(find.text('Anchor')));
        await tester.pump(const Duration(milliseconds: 79));
        expect(find.text('Themed Tooltip'), findsNothing);
        await tester.pump(const Duration(milliseconds: 1));
        await tester.pump(const Duration(milliseconds: 151));
        expect(find.text('Themed Tooltip'), findsOneWidget);
        await mouse.moveTo(Offset.zero);
        await tester.pump(const Duration(milliseconds: 109));
        expect(find.text('Themed Tooltip'), findsOneWidget);
        await tester.pump(const Duration(milliseconds: 1));
        await tester.pumpAndSettle();
        expect(find.text('Themed Tooltip'), findsNothing);
        await mouse.removePointer();
      },
    );
  }
  for (final type in [
    'flutter.material.Tooltip',
    'flutter.material.IconButton',
    'flutter.material.FloatingActionButton',
  ]) {
    testWidgets(
      'inherited unsafe geometry is guarded through real $type overlays without source mutation',
      (tester) async {
        final child = type == 'flutter.material.Tooltip'
            ? _tooltip()
            : _node(
                3,
                type,
                {
                  'enabled': _b(true),
                  'variant': _s('standard'),
                  'tooltip': _s('Themed Tooltip'),
                  if (type.endsWith('FloatingActionButton')) 'heroTag': _null,
                },
                {
                  type.endsWith('IconButton') ? 'icon' : 'child': _single(
                    _text(),
                  ),
                },
              );
        for (final props in [
          {'padding': _inset(-4), 'margin': _inset(-8, directional: true)},
          {
            'padding': _inset(1e308),
            'margin': _inset(1e308, directional: true),
          },
          {
            'height': _e('double', 'nan'),
            'verticalOffset': _e('double', 'negativeInfinity'),
          },
          {
            'height': _e('double', 'negativeInfinity'),
            'verticalOffset': _e('double', 'nan'),
          },
        ]) {
          final root = _scope(props, child: child);
          final before = jsonEncode(root);
          await _pump(tester, root);
          expect(_messages(tester), contains('preview limitation'));
          await _show(tester);
          expect(tester.getTopLeft(find.text('Themed Tooltip')).isFinite, true);
          expect(jsonEncode(root), before);
        }
        await _pump(
          tester,
          _scope(
            {'triggerMode': _e('TooltipTriggerMode', 'manual')},
            child: _node(
              5,
              'flutter.material.TooltipVisibility',
              {'visible': _b(false)},
              {'child': _single(child)},
            ),
          ),
        );
        expect(_state(tester).ensureTooltipVisible(), false);
        await tester.pumpAndSettle();
        expect(find.text('Themed Tooltip'), findsNothing);
      },
    );
  }
  testWidgets(
    'signed microsecond duration extremes are retained without copyWith loss',
    (tester) async {
      for (final value in [-9007199254740991, -1, 0, 1, 9007199254740991]) {
        await _pump(
          tester,
          _scope({
            for (final field in [
              'waitDurationUs',
              'showDurationUs',
              'exitDurationUs',
            ])
              field: _n(value),
          }),
        );
        expect(_data(tester).waitDuration!.inMicroseconds, value);
        expect(_data(tester).showDuration!.inMicroseconds, value);
        expect(_data(tester).exitDuration!.inMicroseconds, value);
      }
    },
  );
}

Map<String, Object?> _compoundDecoration(
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
