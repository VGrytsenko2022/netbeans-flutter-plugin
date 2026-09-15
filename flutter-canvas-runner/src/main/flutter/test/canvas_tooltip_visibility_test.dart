import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.TooltipVisibility';
const _message = 'Scoped Tooltip';
String _id(int n) => '793560e4-67ae-4e99-907b-${n.toString().padLeft(12, '0')}';
Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> _b(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _n(num value) => {
  'kind': value is int ? 'integer' : 'double',
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
Map<String, Object?> _text([String text = 'Anchor']) =>
    _node(4, 'flutter.widgets.Text', {'data': _s(text)});
Map<String, Object?> _scope(bool visible, Object child, {int id = 2}) =>
    _node(id, _type, {'visible': _b(visible)}, {'child': _single(child)});
Map<String, Object?> _tooltip({
  String mode = 'manual',
  Object? child,
  int id = 3,
  String message = _message,
}) => _node(
  id,
  'flutter.material.Tooltip',
  {
    'message': _s(message),
    'triggerMode': {
      'kind': 'enum',
      'type': 'TooltipTriggerMode',
      'value': mode,
    },
  },
  {'child': _single(child ?? _text())},
);
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
Finder _tip([String message = _message]) => find.byWidgetPredicate(
  (widget) => widget is Tooltip && widget.message == message,
);
TooltipState _state(WidgetTester tester, [String message = _message]) =>
    tester.state<TooltipState>(_tip(message));
Future<void> _pump(
  WidgetTester tester,
  Object root, {
  String? selected,
  List<String>? selections,
  bool inline = false,
  ValueChanged<CanvasDropResolver?>? onDrop,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: CanvasDocumentView(
        model: _model(root),
        selectedWidgetId: selected,
        onSelected: selections?.add ?? (_) {},
        inlineTextEditEnabled: inline,
        onDropResolverChanged: onDrop,
      ),
    ),
  );
  await tester.pump();
  await tester.pump();
  expect(tester.takeException(), isNull);
}

Future<TestGesture?> _activate(WidgetTester tester, String method) async {
  TestGesture? hover;
  if (method == 'hover') {
    final mouse = await tester.createGesture(kind: ui.PointerDeviceKind.mouse);
    await mouse.addPointer(location: Offset.zero);
    await mouse.moveTo(tester.getCenter(find.text('Anchor')));
    await tester.pump(const Duration(milliseconds: 151));
    // Leave the device over the anchor until the caller inspects the result.
    hover = mouse;
  } else if (method == 'manual') {
    _state(tester).ensureTooltipVisible();
  } else if (method == 'longPress') {
    await tester.longPress(find.text('Anchor'));
  } else {
    await tester.tap(find.text('Anchor'));
  }
  await tester.pump(const Duration(milliseconds: 151));
  return hover;
}

List<SemanticsData> _semantics(WidgetTester tester) {
  final result = <SemanticsData>[];
  void visit(SemanticsNode node) {
    result.add(node.getSemanticsData());
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

double? _baseline(RenderBox box) {
  final previous = RenderObject.debugCheckingIntrinsics;
  RenderObject.debugCheckingIntrinsics = true;
  try {
    return box.getDistanceToBaseline(TextBaseline.alphabetic, onlyReal: true);
  } finally {
    RenderObject.debugCheckingIntrinsics = previous;
  }
}

void main() {
  test(
    'exact single-property required-wrapper schema is independent and closes the 100/4561 catalog',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      expect(contract, canvasReviewedWidgetSchemaContract.trimLeft());
      expect(
        RegExp(r'^W\|', multiLine: true).allMatches(contract),
        hasLength(217),
      );
      expect(
        RegExp(r'^P\|', multiLine: true).allMatches(contract),
        hasLength(7436),
      );
      final start = contract.indexOf('W|$_type\n');
      final section = contract.substring(
        start,
        contract.indexOf('W|', start + 2),
      );
      expect(
        section,
        'W|flutter.material.TooltipVisibility\nP|visible|boolean|1|boolean:true|-|boolean:any\nS|child|single|1|1|1|any\nC|flutter.material.TooltipVisibility|paletteCreate|wrapExistingChild|child\n',
      );
      expect(utf8.encode(section), hasLength(184));
      expect(
        sha256Hex(utf8.encode(section)),
        '24a8679187526de33d4b59d72414a4f9d9d911323b20f1b2ab809ed751c227e6',
      );
      expect(canvasModelProtocolVersion, 20);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(canvasReviewedRequiredWrapperSlot(_type), 'child');
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
        true,
      );
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: _type,
          childWidgetType: 'flutter.widgets.Expanded',
        ),
        false,
      );
    },
  );

  test(
    'both strict boolean values survive and no absent nullable or expression form is admitted',
    () {
      for (final visible in [true, false]) {
        final decoded = _model(
          _scope(visible, _text()),
        ).root.slot('body')!.child!.slot('child')!.child!;
        expect(decoded.properties.keys, ['visible']);
        expect(decoded.properties['visible']!.value, visible);
        expect(decoded.slot('child')!.child!.id, _id(4));
      }
      for (final props in [
        <String, Object?>{},
        {
          'visible': {'kind': 'null'},
        },
        {'visible': _s('true')},
        {'visible': _n(1)},
        {
          'visible': {'kind': 'dartObjectReferencePresence'},
        },
        {
          'visible': {'kind': 'callbackPresence'},
        },
        {'visible': _b(true), 'onChanged': _s('noop')},
      ]) {
        expect(
          () => _model(_node(2, _type, props, {'child': _single(_text())})),
          throwsFormatException,
        );
      }
      for (final slots in [
        <String, Object?>{},
        {'child': _single(null)},
        {
          'child': {
            'kind': 'list',
            'children': [_text()],
          },
        },
      ]) {
        expect(
          () => _model(_node(2, _type, {'visible': _b(true)}, slots)),
          throwsFormatException,
        );
      }
      expect(
        () => _model(
          _scope(
            true,
            _node(5, 'flutter.widgets.Expanded', {}, {
              'child': _single(_text()),
            }),
          ),
        ),
        throwsFormatException,
      );
    },
  );

  for (final method in ['hover', 'tap', 'longPress', 'manual']) {
    for (final visible in [false, true]) {
      testWidgets(
        'native $method activation visibility=$visible matches uninstrumented SDK',
        (tester) async {
          final mode = method == 'hover' ? 'manual' : method;
          final sdkMode = TooltipTriggerMode.values.byName(mode);
          await tester.pumpWidget(
            MaterialApp(
              home: Scaffold(
                body: Center(
                  child: TooltipVisibility(
                    visible: visible,
                    child: Tooltip(
                      message: _message,
                      triggerMode: sdkMode,
                      child: const Text('Anchor'),
                    ),
                  ),
                ),
              ),
            ),
          );
          final rawMouse = await _activate(tester, method);
          final rawShown = find.text(_message).evaluate().isNotEmpty;
          expect(rawShown, visible);
          await rawMouse?.removePointer();
          await tester.pumpWidget(const SizedBox.shrink());
          await tester.pumpAndSettle();
          final source = _scope(visible, _tooltip(mode: mode));
          final before = jsonEncode(source);
          await _pump(tester, source);
          expect(
            tester
                .widget<TooltipVisibility>(find.byType(TooltipVisibility))
                .visible,
            visible,
          );
          expect(TooltipVisibility.of(tester.element(_tip())), visible);
          final canvasMouse = await _activate(tester, method);
          expect(find.text(_message).evaluate().isNotEmpty, rawShown);
          expect(jsonEncode(source), before);
          await canvasMouse?.removePointer();
          Tooltip.dismissAllToolTips();
          await tester.pumpAndSettle();
        },
      );
    }
  }

  testWidgets(
    'false removes open overlay just like SDK while retaining Tooltip and actual field State',
    (tester) async {
      final field = _node(4, 'flutter.material.TextField', {});
      TooltipState? state;
      EditableTextState? edit;
      for (final visible in [true, false, true, false, true]) {
        final source = _scope(visible, _tooltip(child: field));
        await _pump(tester, source, selected: _id(4));
        state ??= _state(tester);
        edit ??= tester.state<EditableTextState>(find.byType(EditableText));
        expect(_state(tester), same(state));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        edit.widget.controller.text = 'Persistent anchor';
        expect(edit.widget.controller.text, 'Persistent anchor');
        expect(edit.widget.focusNode.canRequestFocus, false);
        expect(find.text(_message), findsNothing);
        expect(state.ensureTooltipVisible(), visible);
        await tester.pumpAndSettle();
        expect(find.text(_message), visible ? findsOneWidget : findsNothing);
      }
    },
  );

  testWidgets(
    'closest nested visibility overrides rather than combines with outer scope',
    (tester) async {
      TooltipState? state;
      for (final outer in [false, true, false]) {
        for (final inner in [true, false, true]) {
          await _pump(tester, _scope(outer, _scope(inner, _tooltip(), id: 5)));
          state ??= _state(tester);
          expect(_state(tester), same(state));
          expect(TooltipVisibility.of(tester.element(_tip())), inner);
          final alreadyVisible = find.text(_message).evaluate().isNotEmpty;
          expect(state.ensureTooltipVisible(), inner && !alreadyVisible);
          await tester.pumpAndSettle();
          expect(find.text(_message), inner ? findsOneWidget : findsNothing);
        }
      }
    },
  );

  testWidgets(
    'disabled outer Tooltip can retain an overridden inner open Tooltip',
    (tester) async {
      final field = _node(4, 'flutter.material.TextField', {
        'maxLength': _n(25),
      });
      final inner = _scope(
        true,
        _tooltip(id: 6, message: 'Inner scoped', child: field),
        id: 5,
      );
      TooltipState? outer;
      TooltipState? previousInner;
      for (final visible in [true, false, true, false]) {
        final source = _scope(visible, _tooltip(child: inner));
        final before = jsonEncode(source);
        await _pump(tester, source);
        outer ??= _state(tester);
        expect(_state(tester), same(outer));
        if (previousInner != null) {
          expect(_state(tester, 'Inner scoped'), isNot(same(previousInner)));
          expect(find.text('Inner scoped'), findsNothing);
        }
        previousInner = _state(tester, 'Inner scoped');
        expect(_anchor(4), findsOneWidget);
        expect(tester.widget<TextField>(find.byType(TextField)).maxLength, 25);
        _state(tester, 'Inner scoped').ensureTooltipVisible();
        await tester.pumpAndSettle();
        expect(find.text('Inner scoped'), findsOneWidget);
        expect(
          TooltipVisibility.of(tester.element(_tip('Inner scoped'))),
          true,
        );
        expect(jsonEncode(source), before);
      }
    },
  );

  testWidgets(
    'child semantics persist and tooltip annotation follows actual pinned SDK rather than its prose',
    (tester) async {
      final handle = tester.ensureSemantics();
      try {
        for (final visible in [true, false, true]) {
          await tester.pumpWidget(
            MaterialApp(
              home: TooltipVisibility(
                visible: visible,
                child: const Tooltip(
                  message: _message,
                  child: Text('Accessible anchor'),
                ),
              ),
            ),
          );
          final raw = _semantics(
            tester,
          ).where((data) => data.tooltip == _message).length;
          expect(raw, visible ? 1 : 0);
          await _pump(
            tester,
            _scope(visible, _tooltip(child: _text('Accessible anchor'))),
          );
          final actual = _semantics(tester);
          expect(actual.where((data) => data.tooltip == _message).length, raw);
          expect(
            actual.any((data) => data.label.contains('Accessible anchor')),
            true,
          );
        }
      } finally {
        handle.dispose();
      }
    },
  );

  for (final type in [
    'flutter.material.IconButton',
    'flutter.material.FloatingActionButton',
  ]) {
    testWidgets(
      'visibility scopes control implicit $type Tooltips including nested open override',
      (tester) async {
        final button = _node(
          7,
          type,
          {
            'enabled': _b(true),
            'variant': _s('standard'),
            'tooltip': _s('Implicit action'),
            if (type.endsWith('FloatingActionButton'))
              'heroTag': {'kind': 'null'},
          },
          {type.endsWith('IconButton') ? 'icon' : 'child': _single(_text())},
        );
        for (final visible in [true, false, true]) {
          await _pump(tester, _scope(visible, button));
          expect(
            TooltipVisibility.of(tester.element(_tip('Implicit action'))),
            visible,
          );
          expect(
            _state(tester, 'Implicit action').ensureTooltipVisible(),
            visible,
          );
          await tester.pumpAndSettle();
          expect(
            find.text('Implicit action'),
            visible ? findsOneWidget : findsNothing,
          );
        }
        for (final outer in [false, true, false, true]) {
          await _pump(
            tester,
            _scope(outer, _tooltip(child: _scope(true, button, id: 5))),
          );
          expect(
            TooltipVisibility.of(tester.element(_tip('Implicit action'))),
            true,
          );
          _state(tester, 'Implicit action').ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(find.text('Implicit action'), findsOneWidget);
          expect(_anchor(7), findsOneWidget);
        }
        final outerState = _state(tester);
        for (final message in ['', 'Implicit action', '']) {
          final changed = {
            ...button,
            'properties': {
              ...(button['properties'] as Map<String, Object?>),
              'tooltip': _s(message),
            },
          };
          await _pump(
            tester,
            _scope(true, _tooltip(child: _scope(true, changed, id: 5))),
          );
          expect(_state(tester), same(outerState));
          expect(
            tester
                .state<TooltipState>(
                  find.descendant(
                    of: find.byType(
                      type.endsWith('IconButton')
                          ? IconButton
                          : FloatingActionButton,
                    ),
                    matching: _tip(message),
                  ),
                )
                .ensureTooltipVisible(),
            message.isNotEmpty,
          );
          await tester.pumpAndSettle();
          expect(
            find.text('Implicit action'),
            message.isEmpty ? findsNothing : findsOneWidget,
          );
          expect(_anchor(7), findsOneWidget);
        }
      },
    );
  }

  testWidgets(
    'inline editor focus and selection survive visibility flag updates and wrapper has transparent geometry',
    (tester) async {
      final child = _tooltip(child: _text('Inline anchor'));
      await _pump(tester, _scope(true, child), selected: _id(4), inline: true);
      await tester.tap(_anchor(4));
      await tester.pump(const Duration(milliseconds: 80));
      await tester.tap(_anchor(4));
      await tester.pumpAndSettle();
      final edit = tester.state<EditableTextState>(find.byType(EditableText));
      expect(edit.widget.focusNode.hasFocus, true);
      final state = _state(tester);
      for (final visible in [false, true, false]) {
        await _pump(
          tester,
          _scope(visible, child),
          selected: _id(4),
          inline: true,
        );
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(edit),
        );
        expect(edit.widget.focusNode.hasFocus, true);
        expect(_state(tester), same(state));
      }
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pumpAndSettle();
      final text = _node(4, 'flutter.widgets.Text', {
        'data': _s('Ag'),
        'styleFontSize': _n(48.0),
      });
      for (final ancestor in ['IntrinsicWidth', 'IntrinsicHeight']) {
        Size? size;
        List<double?>? baseline;
        for (final visible in [true, false, true]) {
          await _pump(
            tester,
            _node(10, 'flutter.widgets.$ancestor', {}, {
              'child': _single(_scope(visible, _tooltip(child: text))),
            }),
            selected: _id(2),
          );
          final box = tester.renderObject<RenderBox>(_anchor(2));
          size ??= box.size;
          baseline ??= [
            _baseline(box),
            box.getDryBaseline(box.constraints, TextBaseline.alphabetic),
          ];
          expect(box.size, size);
          expect([
            _baseline(box),
            box.getDryBaseline(box.constraints, TextBaseline.alphabetic),
          ], baseline);
          expect(tester.getRect(_anchor(2)), tester.getRect(_anchor(4)));
        }
      }
    },
  );

  testWidgets(
    'required wrapper keeps zero child geometry and only existing-child replacement drops',
    (tester) async {
      CanvasDropResolver? resolver;
      final child = _node(4, 'flutter.widgets.SizedBox', {
        'width': _n(0),
        'height': _n(0),
      });
      await _pump(
        tester,
        _scope(false, child),
        selected: _id(2),
        onDrop: (value) => resolver = value,
      );
      expect(tester.getSize(_anchor(2)), Size.zero);
      expect(
        find.byWidgetPredicate(
          (widget) =>
              widget is GestureDetector &&
              widget.key.toString().contains('canvas-zero-size-widget-target-'),
        ),
        findsOneWidget,
      );
      final target = canvasExistingChildWrapTargetSlot(
        parentWidgetType: _type,
        slotName: 'child',
      );
      expect(target!.accepts(currentChildCount: 1, insertionIndex: 0), true);
      expect(target.accepts(currentChildCount: 0, insertionIndex: 0), false);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(resolver, isNotNull);
    },
  );

  testWidgets(
    'wrapping unwrapping and moving scopes safely retire old portals without model ID changes',
    (tester) async {
      final tip = _tooltip(child: _node(4, 'flutter.material.TextField', {}));
      final source = jsonEncode(tip);
      for (final root in [
        tip,
        _scope(true, tip),
        _scope(false, tip),
        tip,
        _node(
          11,
          'flutter.widgets.Padding',
          {
            'padding': {
              'kind': 'edgeInsets',
              'left': 8,
              'top': 8,
              'right': 8,
              'bottom': 8,
            },
          },
          {'child': _single(_scope(true, tip))},
        ),
      ]) {
        await _pump(tester, root);
        final active = TooltipVisibility.of(tester.element(_tip()));
        _state(tester).ensureTooltipVisible();
        await tester.pumpAndSettle();
        expect(find.text(_message), active ? findsOneWidget : findsNothing);
        expect(_anchor(4), findsOneWidget);
        expect(jsonEncode(tip), source);
        expect(tester.takeException(), isNull);
      }
    },
  );
}
