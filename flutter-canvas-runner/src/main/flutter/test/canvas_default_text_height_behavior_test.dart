import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.widgets.DefaultTextHeightBehavior';
const _id = 'a3867d28-9495-4dfc-8ec9-087075ffb9b3';
const _childId = 'b37a51a0-598e-4189-8f08-0a7dd6d5a772';
const _textId = 'bf6064c6-d24e-4f7d-802c-0cf1e54b0574';
const _otherId = 'ce7074c6-d24e-4f7d-802c-0cf1e54b0574';
const _columnId = 'dbed0e25-f4ee-4b41-b6df-c0d1fe0bb8ee';
const _first = 'textHeightApplyFirstAscent';
const _last = 'textHeightApplyLastDescent';
const _leading = 'textHeightLeadingDistribution';
const _custom = TextHeightBehavior(
  applyHeightToFirstAscent: false,
  applyHeightToLastDescent: false,
  leadingDistribution: TextLeadingDistribution.even,
);

void main() {
  test(
    'DefaultTextHeightBehavior exact three optional leaves and required-child fingerprint',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf('\nW|', start) + 1;
      expect(
        contract.substring(start, end),
        'W|$_type\n'
        'P|$_first|boolean|0|-|-|boolean:any\n'
        'P|$_last|boolean|0|-|-|boolean:any\n'
        'P|$_leading|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional\n'
        'S|child|single|1|1|1|any\n'
        'C|$_type|paletteCreate|wrapExistingChild|child\n',
      );
      expect(canvasModelProtocolVersion, 19);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), isTrue);
      expect(isCanvasPaletteWrapperWidgetType(_type), isTrue);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(_find(_decode(_model()).root, _id)!.properties, isEmpty);
    },
  );

  test(
    'DefaultTextHeightBehavior accepts all27 tri-state combinations without persisting synthetic defaults',
    () {
      var count = 0;
      for (final first in [null, false, true]) {
        for (final last in [null, false, true]) {
          for (final leading in [null, 'even', 'proportional']) {
            final node = _find(
              _decode(_model(first: first, last: last, leading: leading)).root,
              _id,
            )!;
            expect(node.properties[_first]?.value, first);
            expect(node.properties[_last]?.value, last);
            expect(
              (node.properties[_leading]?.value as CanvasEnumValue?)?.value,
              leading,
            );
            expect(node.slot('child')!.child!.id, _textId);
            count++;
          }
        }
      }
      expect(count, 27);
    },
  );

  test(
    'DefaultTextHeightBehavior rejects malformed boolean enum unknown leaves and child shapes',
    () {
      for (final name in [_first, _last]) {
        for (final value in [
          {'kind': 'null'},
          {'kind': 'integer', 'value': 1},
          {'kind': 'string', 'value': 'false'},
          {'kind': 'boolean', 'value': null},
          {'kind': 'boolean', 'value': 1},
          {'kind': 'boolean', 'value': true, 'extra': true},
        ]) {
          final model = _model();
          _wrapperJson(model)['properties'] = {name: value};
          expect(() => _decode(model), throwsFormatException);
        }
      }
      for (final value in [
        {'kind': 'null'},
        {'kind': 'boolean', 'value': true},
        _enum('unknown'),
        {..._enum('even'), 'type': 'TextDirection'},
        {..._enum('even'), 'library': 'dart:ui'},
        {..._enum('even'), 'extra': true},
      ]) {
        final model = _model();
        _wrapperJson(model)['properties'] = {_leading: value};
        expect(() => _decode(model), throwsFormatException);
      }
      for (final name in [
        'key',
        'child',
        'textHeightBehavior',
        'applyHeightToFirstAscent',
        'styleHeight',
        'textScalerFactor',
      ]) {
        final model = _model();
        _wrapperJson(model)['properties'] = {name: _bool(true)};
        expect(() => _decode(model), throwsFormatException);
      }
      for (final slots in [
        <String, Object?>{},
        {
          'child': {'kind': 'single', 'child': null},
        },
        {
          'child': {
            'kind': 'list',
            'children': [_text()],
          },
        },
        {
          'children': {'kind': 'single', 'child': _text()},
        },
        {
          'child': {'kind': 'single', 'child': _text(), 'extra': true},
        },
      ]) {
        final model = _model();
        _wrapperJson(model)['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
      for (final type in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () =>
              _decode(_model(child: _node(_childId, 'flutter.widgets.$type'))),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'DefaultTextHeightBehavior wraps occupied ordinary slots but cannot instantiate an empty required child',
    () {
      for (final parent in [
        (type: 'flutter.widgets.Column', slot: 'children'),
        (type: 'flutter.widgets.Center', slot: 'child'),
        (type: _type, slot: 'child'),
        (type: 'flutter.widgets.Visibility', slot: 'replacement'),
      ]) {
        for (final count in [0, 1]) {
          expect(
            canvasDropTargetAcceptsSource(
              parentWidgetType: parent.type,
              slotName: parent.slot,
              currentChildCount: count,
              insertionIndex: 0,
              source: _source(_type),
            ),
            count == 1,
          );
        }
      }
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
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: _type,
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: _source('flutter.widgets.Text'),
        ),
        isFalse,
      );
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'DefaultTextHeightBehavior actual SDK constructor and inherited Text cover all27 omission states on $platform',
      (tester) async {
        for (final first in [null, false, true]) {
          for (final last in [null, false, true]) {
            for (final leading in [null, 'even', 'proportional']) {
              await _pump(
                tester,
                _model(
                  platform: platform,
                  first: first,
                  last: last,
                  leading: leading,
                ),
              );
              final expected = TextHeightBehavior(
                applyHeightToFirstAscent: first ?? true,
                applyHeightToLastDescent: last ?? true,
                leadingDistribution: leading == 'even'
                    ? TextLeadingDistribution.even
                    : TextLeadingDistribution.proportional,
              );
              expect(
                tester
                    .widget<DefaultTextHeightBehavior>(_behavior())
                    .textHeightBehavior,
                expected,
              );
              expect(
                DefaultTextHeightBehavior.of(tester.element(_widget(_textId))),
                expected,
              );
              expect(_richText(tester).textHeightBehavior, expected);
              final text = tester.widget<Text>(
                find
                    .descendant(
                      of: _widget(_textId),
                      matching: find.byType(Text),
                    )
                    .first,
              );
              expect(
                text.textHeightBehavior,
                isNull,
                reason:
                    'Text keeps null so Flutter may inherit the enclosing value',
              );
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    testWidgets(
      'DefaultTextHeightBehavior nearer all-unset resets outer behavior and Text local object takes precedence on $platform',
      (tester) async {
        for (final local in [false, true]) {
          final model = _model(
            platform: platform,
            first: false,
            last: false,
            leading: 'even',
            child: _node(
              _childId,
              _type,
              child: _text(properties: local ? {_first: _bool(false)} : {}),
            ),
          );
          await _pump(tester, model);
          expect(
            DefaultTextHeightBehavior.of(tester.element(_widget(_textId))),
            const TextHeightBehavior(),
          );
          expect(
            _richText(tester).textHeightBehavior,
            TextHeightBehavior(applyHeightToFirstAscent: !local),
          );
          expect(
            _richText(tester).textHeightBehavior!.applyHeightToLastDescent,
            isTrue,
          );
          expect(
            _richText(tester).textHeightBehavior!.leadingDistribution,
            TextLeadingDistribution.proportional,
          );
        }
      },
    );

    testWidgets(
      'DefaultTextHeightBehavior first ascent last descent and leading distribution drive real text geometry on $platform',
      (tester) async {
        final heights = <String, double>{};
        for (final first in [false, true]) {
          for (final last in [false, true]) {
            for (final leading in ['even', 'proportional']) {
              await _pump(
                tester,
                _model(
                  platform: platform,
                  first: first,
                  last: last,
                  leading: leading,
                  child: _text(
                    label: 'Ag\njp',
                    properties: {
                      'styleFontSize': {'kind': 'double', 'value': 20.0},
                      'styleHeight': {'kind': 'double', 'value': 2.0},
                    },
                  ),
                ),
              );
              final rich = _richText(tester);
              final render = tester.renderObject<RenderParagraph>(
                _richFinder(),
              );
              final painter = TextPainter(
                text: rich.text,
                textDirection: TextDirection.ltr,
                textHeightBehavior: rich.textHeightBehavior,
                textScaler: rich.textScaler,
              )..layout(maxWidth: render.constraints.maxWidth);
              try {
                expect(render.size.height, closeTo(painter.height, 0.001));
                expect(
                  render.getDryBaseline(
                    render.constraints,
                    TextBaseline.alphabetic,
                  ),
                  closeTo(painter.computeLineMetrics().first.baseline, 0.001),
                );
                expect(render.textHeightBehavior, rich.textHeightBehavior);
                heights['$first/$last/$leading'] = render.size.height;
              } finally {
                painter.dispose();
              }
              expect(tester.takeException(), isNull);
            }
          }
        }
        for (final leading in ['even', 'proportional']) {
          expect(
            heights['true/true/$leading']!,
            greaterThan(heights['false/false/$leading']!),
          );
        }
      },
    );

    testWidgets(
      'DefaultTextHeightBehavior updates retain real button focus state labels and pointer selection on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          FocusNode? original;
          final selections = <String>[];
          for (final first in [null, false, true]) {
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(
                  _model(
                    platform: platform,
                    first: first,
                    child: _node(
                      _childId,
                      'flutter.material.ElevatedButton',
                      child: _text(),
                    ),
                  ),
                ),
                selectedWidgetId: null,
                onSelected: selections.add,
              ),
            );
            await tester.pump();
            final focus = Focus.of(tester.element(find.text('Height child')));
            original ??= focus;
            expect(identical(focus, original), isTrue);
            focus.requestFocus();
            await tester.pump();
            expect(focus.hasFocus, isTrue);
            expect(
              _semanticLabels(
                tester,
              ).any((label) => label.contains('Height child')),
              isTrue,
            );
            await tester.tap(_widget(_textId));
            await tester.pump(const Duration(milliseconds: 350));
            expect(selections, isNotEmpty);
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'DefaultTextHeightBehavior F2 commit cancel reopen uses inherited metrics without stealing project focus on $platform',
      (tester) async {
        final commits = <(String, String)>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                first: false,
                last: false,
                leading: 'even',
              ),
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
        await _f2(tester);
        final field = tester.widget<TextField>(find.byType(TextField));
        expect(field.focusNode!.hasFocus, isTrue);
        expect(
          DefaultTextHeightBehavior.of(tester.element(find.byType(TextField))),
          _custom,
        );
        await tester.enterText(find.byType(TextField), 'Height editor draft');
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_textId, 'Height editor draft')]);
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
      'DefaultTextHeightBehavior child wrapping descendant insertion and required-child moves preserve placement on $platform',
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
        final wrapper = _wrapperJson(model);
        _replaceCentered(
          model,
          _list(_otherId, [
            wrapper,
            _node(
              '11704f22-ad11-4d98-a779-72c024737b27',
              'flutter.widgets.Center',
            ),
          ]),
        );
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
        final wrapped = _resolve(tester, drop!, point, _source(_type));
        expect(wrapped?.parentWidgetId, _childId);
        expect(wrapped?.slotName, 'child');
        expect(
          move!(_childId, '11704f22-ad11-4d98-a779-72c024737b27', 'child', 0),
          isNull,
        );
        expect(move!(_childId, _id, 'child', 0), isNotNull);
        expect(
          move!(_id, '11704f22-ad11-4d98-a779-72c024737b27', 'child', 0),
          isNotNull,
        );
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'DefaultTextHeightBehavior zero-size child remains genuine and exposes grouped handles on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(
            platform: platform,
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
        expect(
          find.descendant(of: _behavior(), matching: find.byType(SizedBox)),
          findsOneWidget,
        );
      },
    );

    testWidgets(
      'DefaultTextHeightBehavior inherits into project EditableText while retaining Designer preview guard on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(
            platform: platform,
            first: false,
            last: false,
            leading: 'even',
            child: {
              'id': _childId,
              'type': 'flutter.material.TextField',
              'properties': {
                'autofocus': _bool(true),
                'canRequestFocus': _bool(true),
              },
              'slots': <String, Object?>{},
            },
          ),
        );
        final field = tester.widget<EditableText>(find.byType(EditableText));
        expect(field.textHeightBehavior, isNull);
        expect(
          tester
              .state<EditableTextState>(find.byType(EditableText))
              .renderEditable
              .textHeightBehavior,
          _custom,
        );
        field.focusNode.requestFocus();
        await tester.pump();
        expect(field.focusNode.canRequestFocus, isFalse);
        expect(field.focusNode.hasFocus, isFalse);
        expect(field.controller.text, isEmpty);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'SDK Text local behavior precedes DefaultTextStyle which precedes inherited DefaultTextHeightBehavior',
    (tester) async {
      const style = TextHeightBehavior(applyHeightToFirstAscent: false);
      const local = TextHeightBehavior(applyHeightToLastDescent: false);
      for (final defaultStyle in [null, style]) {
        for (final textLocal in [null, local]) {
          await tester.pumpWidget(
            Directionality(
              textDirection: TextDirection.ltr,
              child: DefaultTextHeightBehavior(
                textHeightBehavior: _custom,
                child: DefaultTextStyle(
                  style: const TextStyle(fontSize: 20),
                  textHeightBehavior: defaultStyle,
                  child: Text('Precedence', textHeightBehavior: textLocal),
                ),
              ),
            ),
          );
          expect(
            tester.widget<RichText>(find.byType(RichText)).textHeightBehavior,
            textLocal ?? defaultStyle ?? _custom,
          );
        }
      }
    },
  );

  testWidgets(
    'SDK EditableText local behavior precedes inherited value independently from DefaultTextStyle',
    (tester) async {
      final controller = TextEditingController(text: 'Editable');
      final focus = FocusNode();
      try {
        for (final local in [null, const TextHeightBehavior()]) {
          await tester.pumpWidget(
            MaterialApp(
              home: DefaultTextHeightBehavior(
                textHeightBehavior: _custom,
                child: DefaultTextStyle(
                  style: const TextStyle(),
                  textHeightBehavior: const TextHeightBehavior(),
                  child: EditableText(
                    controller: controller,
                    focusNode: focus,
                    style: const TextStyle(fontSize: 20),
                    cursorColor: Colors.blue,
                    backgroundCursorColor: Colors.grey,
                    textHeightBehavior: local,
                  ),
                ),
              ),
            ),
          );
          expect(
            tester
                .state<EditableTextState>(find.byType(EditableText))
                .renderEditable
                .textHeightBehavior,
            local ?? _custom,
          );
        }
        await tester.pumpWidget(const SizedBox());
      } finally {
        controller.dispose();
        focus.dispose();
      }
    },
  );

  testWidgets(
    'SDK inherited updates notify only changed behavior and preserve stateful child identity',
    (tester) async {
      final key = GlobalKey<_HeightProbeState>();
      final child = _HeightProbe(key: key);
      Future<void> show(TextHeightBehavior behavior) => tester.pumpWidget(
        DefaultTextHeightBehavior(textHeightBehavior: behavior, child: child),
      );
      await show(const TextHeightBehavior());
      final state = key.currentState!;
      expect(state.builds, 1);
      expect(state.current, const TextHeightBehavior());
      await show(const TextHeightBehavior());
      expect(state.builds, 1);
      await show(_custom);
      expect(state.builds, 2);
      expect(state.current, _custom);
      expect(identical(key.currentState, state), isTrue);
      await show(const TextHeightBehavior());
      expect(state.builds, 3);
      await tester.pumpWidget(const SizedBox());
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'SDK InheritedTheme capture and wrap retain DefaultTextHeightBehavior outside the original subtree',
    (tester) async {
      BuildContext? outside;
      BuildContext? inside;
      await tester.pumpWidget(
        Builder(
          builder: (context) {
            outside = context;
            return DefaultTextHeightBehavior(
              textHeightBehavior: _custom,
              child: Builder(
                builder: (context) {
                  inside = context;
                  return const SizedBox();
                },
              ),
            );
          },
        ),
      );
      final themes = InheritedTheme.capture(from: inside!, to: outside);
      TextHeightBehavior? captured;
      await tester.pumpWidget(
        themes.wrap(
          Builder(
            builder: (context) {
              captured = DefaultTextHeightBehavior.of(context);
              return const SizedBox();
            },
          ),
        ),
      );
      expect(captured, _custom);
      final inherited = tester.widget<DefaultTextHeightBehavior>(
        find.byType(DefaultTextHeightBehavior),
      );
      TextHeightBehavior? wrapped;
      await tester.pumpWidget(
        inherited.wrap(
          tester.element(find.byType(SizedBox)),
          Builder(
            builder: (context) {
              wrapped = DefaultTextHeightBehavior.of(context);
              return const SizedBox();
            },
          ),
        ),
      );
      expect(wrapped, _custom);
    },
  );

  testWidgets(
    'SDK maybeOf stays null without a theme and of reports the absent inherited value',
    (tester) async {
      await tester.pumpWidget(
        Builder(
          builder: (context) {
            expect(DefaultTextHeightBehavior.maybeOf(context), isNull);
            expect(
              () => DefaultTextHeightBehavior.of(context),
              throwsFlutterError,
            );
            return const SizedBox();
          },
        ),
      );
    },
  );
}

Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _behavior() => find
    .descendant(
      of: _widget(_id),
      matching: find.byType(DefaultTextHeightBehavior),
    )
    .first;
Finder _richFinder() => find
    .descendant(of: _widget(_textId), matching: find.byType(RichText))
    .first;
RichText _richText(WidgetTester tester) =>
    tester.widget<RichText>(_richFinder());
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

Future<void> _f2(WidgetTester tester) async {
  await tester.tapAt(
    tester.getTopLeft(
          find.byKey(const ValueKey('canvas-interaction-surface')),
        ) +
        const Offset(2, 2),
  );
  await tester.sendKeyEvent(LogicalKeyboardKey.f2);
  await tester.pump();
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

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Map<String, Object?> _model({
  String platform = 'windows',
  bool? first,
  bool? last,
  String? leading,
  Map<String, Object?>? child,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  model['root'] = {
    'id': 'f775415c-4c7d-4467-b070-7555c3af053b',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          'eb7804ec-1f0c-4c30-8037-5e6f89cc7f1d',
          'flutter.widgets.Center',
          child: _node(
            _id,
            _type,
            properties: {
              if (first != null) _first: _bool(first),
              if (last != null) _last: _bool(last),
              if (leading != null) _leading: _enum(leading),
            },
            child: child ?? _text(),
          ),
        ),
      },
    },
  };
  return model;
}

Map _centerSlot(Map<String, Object?> model) {
  final root = model['root']! as Map;
  final body = (root['slots']! as Map)['body']! as Map;
  final center = body['child']! as Map;
  return (center['slots']! as Map)['child']! as Map;
}

Map<String, Object?> _wrapperJson(Map<String, Object?> model) =>
    _centerSlot(model)['child']! as Map<String, Object?>;
void _replaceCentered(Map<String, Object?> model, Map<String, Object?> node) =>
    _centerSlot(model)['child'] = node;
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _enum(String value) => {
  'kind': 'enum',
  'type': 'TextLeadingDistribution',
  'value': value,
};
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
Map<String, Object?> _text({
  String label = 'Height child',
  Map<String, Object?> properties = const {},
}) => {
  'id': _textId,
  'type': 'flutter.widgets.Text',
  'properties': {
    'data': {'kind': 'string', 'value': label},
    ...properties,
  },
  'slots': <String, Object?>{},
};
Map<String, Object?> _size(int width, int height) => {
  'width': {'kind': 'integer', 'value': width},
  'height': {'kind': 'integer', 'value': height},
};
Map<String, Object?> _list(String id, List<Object?> children) => {
  'id': id,
  'type': 'flutter.widgets.Column',
  'properties': <String, Object?>{},
  'slots': {
    'children': {'kind': 'list', 'children': children},
  },
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

Set<String> _semanticLabels(WidgetTester tester) {
  final result = <String>{};
  void visit(SemanticsNode node) {
    if (!node.isMergedIntoParent) result.add(node.label);
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(
    tester.binding.renderViews.single.owner!.semanticsOwner!.rootSemanticsNode!,
  );
  return result;
}

class _HeightProbe extends StatefulWidget {
  const _HeightProbe({super.key});
  @override
  State<_HeightProbe> createState() => _HeightProbeState();
}

class _HeightProbeState extends State<_HeightProbe> {
  var builds = 0;
  TextHeightBehavior? current;
  @override
  Widget build(BuildContext context) {
    builds++;
    current = DefaultTextHeightBehavior.of(context);
    return const SizedBox();
  }
}
