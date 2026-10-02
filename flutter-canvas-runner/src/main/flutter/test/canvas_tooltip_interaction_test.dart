import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _plain = 'Accessible plain Tooltip';
const _richPreview = '[Preview unavailable: project InlineSpan]';
const _ref = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
String _id(int n) => '5417ef35-7afd-4e74-a3b3-${n.toString().padLeft(12, '0')}';
Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> _b(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _n(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _single(Object child) => {
  'kind': 'single',
  'child': child,
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
  Map<String, Object?> properties = const {},
  required Object child,
}) => _node(
  id,
  'flutter.material.Tooltip',
  {'message': _s(_plain), ...properties},
  {'child': _single(child)},
);

CanvasModel _model(
  Object root, {
  Map<String, Object?> scaffoldProperties = const {},
}) {
  final envelope =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  envelope['root'] = _node(
    900,
    'flutter.material.Scaffold',
    scaffoldProperties,
    {
      'body': _single(
        _node(901, 'flutter.widgets.Center', {}, {'child': _single(root)}),
      ),
    },
  );
  return CanvasModel.decode(
    Uint8List.fromList(utf8.encode(jsonEncode(envelope))),
  );
}

Future<void> _pump(
  WidgetTester tester,
  Object root, {
  ValueChanged<CanvasDropResolver?>? onDrop,
  List<String>? selections,
  Map<String, Object?> scaffoldProperties = const {},
  String? scenario,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: CanvasDocumentView(
        model: _model(root, scaffoldProperties: scaffoldProperties),
        selectedWidgetId: null,
        onSelected: selections?.add ?? (_) {},
        onDropResolverChanged: onDrop,
      ),
    ),
  );
  await tester.pump();
  await tester.pump();
  expect(tester.takeException(), isNull, reason: scenario);
}

Finder _anchor(int id) => find.byKey(ValueKey('canvas-widget-${_id(id)}'));
Finder _sdkTooltip(String message) => find.byWidgetPredicate(
  (widget) =>
      widget is Tooltip && widget.key is GlobalKey && widget.message == message,
);

List<SemanticsData> _semantics(WidgetTester tester) {
  final data = <SemanticsData>[];
  void visit(SemanticsNode node) {
    data.add(node.getSemanticsData());
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(
    tester.binding.renderViews.single.owner!.semanticsOwner!.rootSemanticsNode!,
  );
  return data;
}

void _noDeletedDropTargets(
  WidgetTester tester,
  CanvasDropResolver resolver,
  Iterable<Offset> previousPoints,
) {
  final surface = tester.getRect(find.byType(CanvasDocumentView));
  final points = <Offset>[
    ...previousPoints,
    for (final x in [0.1, 0.5, 0.9])
      for (final y in [0.1, 0.5, 0.9])
        Offset(
          surface.left + x * surface.width,
          surface.top + y * surface.height,
        ),
  ];
  for (final point in points) {
    final target = resolver(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(
        token: 'text',
        widgetType: 'flutter.widgets.Text',
        traits: const {},
      ),
    );
    expect(target?.parentWidgetId, isNot(isIn([_id(2), _id(3)])));
  }
}

void main() {
  testWidgets(
    'plain Tooltip semantics and exclusion match the uninstrumented SDK',
    (tester) async {
      final handle = tester.ensureSemantics();
      try {
        for (final excluded in <bool?>[null, false, true, null]) {
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: Tooltip(
                  message: _plain,
                  excludeFromSemantics: excluded,
                  child: const Text('Accessible anchor'),
                ),
              ),
            ),
          );
          final raw = _semantics(tester)
              .where((data) => data.tooltip == _plain)
              .map((data) => data.tooltip)
              .toList();
          expect(raw, excluded == true ? isEmpty : [_plain]);

          await _pump(
            tester,
            _tooltip(
              properties: {
                if (excluded != null) 'excludeFromSemantics': _b(excluded),
              },
              child: _text(4, 'Accessible anchor'),
            ),
          );
          final actual = _semantics(tester);
          expect(
            actual
                .where((data) => data.tooltip == _plain)
                .map((data) => data.tooltip),
            raw,
          );
          expect(
            actual.any((data) => data.label.contains('Accessible anchor')),
            isTrue,
          );
          expect(tester.takeException(), isNull);
        }
      } finally {
        handle.dispose();
      }
    },
  );

  testWidgets(
    'rich reference exposes only the explicit surrogate semantics and obeys exclusion',
    (tester) async {
      final handle = tester.ensureSemantics();
      try {
        for (final excluded in [false, true, false]) {
          final root = _tooltip(
            properties: {
              'message': _null,
              'richMessage': _ref,
              'excludeFromSemantics': _b(excluded),
            },
            child: _text(4, 'Rich anchor'),
          );
          final before = jsonEncode(root);
          await _pump(tester, root);
          final actual = _semantics(tester);
          expect(
            actual.where((data) => data.tooltip == _richPreview),
            excluded ? isEmpty : hasLength(1),
          );
          expect(actual.any((data) => data.tooltip == _plain), isFalse);
          expect(
            actual.any((data) => data.label.contains('Rich anchor')),
            isTrue,
          );
          final diagnostics = tester.widgetList<Tooltip>(find.byType(Tooltip));
          expect(
            diagnostics.any(
              (tooltip) =>
                  tooltip.message?.contains(
                    'WidgetSpan interaction and semantics cannot be reproduced',
                  ) ??
                  false,
            ),
            isTrue,
          );
          expect(jsonEncode(root), before);
          expect(tester.takeException(), isNull);
        }
      } finally {
        handle.dispose();
      }
    },
  );

  testWidgets(
    'unwrapping nested Tooltip with an open hover overlay removes overlays and drop ghosts',
    (tester) async {
      CanvasDropResolver? resolver;
      final selections = <String>[];
      final anchor = _text(4, 'Retained anchor');
      await _pump(
        tester,
        _tooltip(
          child: _tooltip(
            id: 3,
            properties: {'message': _s('Nested Tooltip')},
            child: anchor,
          ),
        ),
        onDrop: (value) => resolver = value,
        selections: selections,
      );
      final mouse = await tester.createGesture(
        kind: ui.PointerDeviceKind.mouse,
      );
      await mouse.addPointer(location: Offset.zero);
      final anchorPoint = tester.getCenter(_anchor(4));
      await mouse.moveTo(anchorPoint);
      await tester.pumpAndSettle();
      expect(find.text('Nested Tooltip'), findsOneWidget);
      expect(find.text(_plain), findsNothing);
      final overlayPoint = tester.getCenter(find.text('Nested Tooltip'));
      final inner = tester.state<TooltipState>(_sdkTooltip('Nested Tooltip'));
      final outer = tester.state<TooltipState>(_sdkTooltip(_plain));
      await _pump(
        tester,
        anchor,
        onDrop: (value) => resolver = value,
        selections: selections,
      );
      await tester.pump(const Duration(seconds: 2));
      await tester.pumpAndSettle();
      expect(inner.mounted, isFalse);
      expect(outer.mounted, isFalse);
      expect(find.text('Nested Tooltip'), findsNothing);
      expect(find.text(_plain), findsNothing);
      expect(_anchor(4), findsOneWidget);
      expect(resolver, isNotNull);
      _noDeletedDropTargets(tester, resolver!, [anchorPoint, overlayPoint]);
      expect(selections, isEmpty);
      await tester.tap(find.text('Retained anchor'));
      await tester.pumpAndSettle();
      expect(selections, [_id(4)]);
      await mouse.removePointer();
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pump(const Duration(seconds: 2));
      expect(resolver, isNull);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'retained inner Tooltip safely unwraps and moves between containers with an open overlay',
    (tester) async {
      CanvasDropResolver? resolver;
      final selections = <String>[];
      final field = _node(4, 'flutter.material.TextField', {});
      final inner = _tooltip(
        id: 3,
        properties: {
          'message': _s('Retained inner Tooltip'),
          'waitDurationUs': _n(123456),
          'exitDurationUs': _n(654321),
          'preferBelow': _b(false),
        },
        child: field,
      );
      final sourceValues = jsonEncode(inner);
      Future<void> pumpRoot(Object root) => _pump(
        tester,
        root,
        onDrop: (value) => resolver = value,
        selections: selections,
      );
      Future<void> showInner() async {
        final state = tester.state<TooltipState>(
          _sdkTooltip('Retained inner Tooltip'),
        );
        expect(state.ensureTooltipVisible(), isTrue);
        await tester.pumpAndSettle();
        expect(find.text('Retained inner Tooltip'), findsOneWidget);
      }

      void retainedModelIsExact() {
        expect(_anchor(3), findsOneWidget);
        expect(_anchor(4), findsOneWidget);
        expect(find.byType(TextField), findsOneWidget);
        expect(jsonEncode(inner), sourceValues);
        final tooltip = tester.widget<Tooltip>(
          _sdkTooltip('Retained inner Tooltip'),
        );
        expect(tooltip.waitDuration, const Duration(microseconds: 123456));
        expect(tooltip.exitDuration, const Duration(microseconds: 654321));
        expect(tooltip.preferBelow, isFalse);
        expect(tester.takeException(), isNull);
      }

      await pumpRoot(_tooltip(child: inner));
      await showInner();
      await pumpRoot(inner);
      await tester.pumpAndSettle();
      expect(_anchor(2), findsNothing);
      expect(find.text('Retained inner Tooltip'), findsNothing);
      retainedModelIsExact();

      Map<String, Object?> containers(bool inFirst) =>
          _node(10, 'flutter.widgets.Column', {}, {
            'children': {
              'kind': 'list',
              'children': [
                for (final id in [11, 12])
                  _node(
                    id,
                    'flutter.widgets.SizedBox',
                    {'width': _n(300), 'height': _n(90)},
                    (id == 11) == inFirst ? {'child': _single(inner)} : {},
                  ),
              ],
            },
          });
      await pumpRoot(containers(true));
      final previousAnchorPoint = tester.getCenter(_anchor(4));
      await showInner();
      await pumpRoot(containers(false));
      await tester.pump(const Duration(seconds: 2));
      await tester.pumpAndSettle();
      expect(find.text('Retained inner Tooltip'), findsNothing);
      retainedModelIsExact();
      expect(
        tester.getCenter(_anchor(4)).dy,
        greaterThan(previousAnchorPoint.dy),
      );
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final target = resolver!(
        ((previousAnchorPoint.dx - surface.left) / surface.width * 1000000)
            .round(),
        ((previousAnchorPoint.dy - surface.top) / surface.height * 1000000)
            .round(),
        CanvasPaletteDragSource(
          token: 'text',
          widgetType: 'flutter.widgets.Text',
          traits: const {},
        ),
      );
      expect(target?.parentWidgetId, _id(11));
      expect(target?.slotName, 'child');
      expect(target?.insertionIndex, 0);
      expect(selections, isEmpty);
      await showInner();
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pump(const Duration(seconds: 2));
      await tester.pumpAndSettle();
      expect(resolver, isNull);
      expect(find.text('Retained inner Tooltip'), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );

  for (final notificationAncestor in [true, false]) {
    testWidgets(
      '${notificationAncestor ? 'NotificationListener type' : 'Scaffold diagnostic'} ancestor transitions keep nested overlays safe and scalar edits retain field state and focus guard',
      (tester) async {
        CanvasDropResolver? resolver;
        final inner = _tooltip(
          id: 3,
          properties: {
            'message': _s('Nested ancestor Tooltip'),
            'preferBelow': _b(true),
            'exitDurationUs': _n(123456),
          },
          child: _node(4, 'flutter.material.TextField', {}),
        );
        final subtree = _tooltip(child: inner);
        final sourceValues = jsonEncode(subtree);
        TooltipState? previousTooltipState;
        EditableTextState? previousFieldState;
        for (final changed in [false, true, false]) {
          final root = notificationAncestor
              ? _node(
                  11,
                  'flutter.widgets.NotificationListener',
                  {
                    'notificationType': _s(
                      changed ? 'ScrollNotification' : 'Notification',
                    ),
                  },
                  {'child': _single(subtree)},
                )
              : subtree;
          final scaffoldProperties = <String, Object?>{
            if (!notificationAncestor && changed)
              'bottomSheetScrimBuilder': _ref,
          };
          final ancestorValues = jsonEncode(root);
          await _pump(
            tester,
            root,
            scaffoldProperties: scaffoldProperties,
            onDrop: (value) => resolver = value,
          );
          await tester.pumpAndSettle();
          if (notificationAncestor || previousTooltipState == null) {
            expect(find.text('Nested ancestor Tooltip'), findsNothing);
          } else {
            // The diagnostic is a stable sibling when Tooltip descendants
            // exist, so toggling the warning need not remount the anchor.
            expect(find.text('Nested ancestor Tooltip'), findsOneWidget);
          }
          expect(_anchor(2), findsOneWidget);
          expect(_anchor(3), findsOneWidget);
          expect(_anchor(4), findsOneWidget);
          expect(jsonEncode(subtree), sourceValues);
          expect(jsonEncode(root), ancestorValues);
          final tooltipState = tester.state<TooltipState>(
            _sdkTooltip('Nested ancestor Tooltip'),
          );
          final fieldState = tester.state<EditableTextState>(
            find.byType(EditableText),
          );
          if (!notificationAncestor && previousTooltipState != null) {
            expect(tooltipState, same(previousTooltipState));
            expect(fieldState, same(previousFieldState));
            expect(fieldState.widget.controller.text, 'Transient input stays');
          }
          // Application TextField previews deliberately exclude focus. Keep
          // that guard intact while checking the retained SDK controller;
          // active Designer inline-editor focus is covered independently.
          fieldState.widget.controller.text = 'Transient input stays';
          expect(fieldState.widget.focusNode.canRequestFocus, isFalse);
          expect(fieldState.widget.focusNode.hasFocus, isFalse);
          final wasVisible = find
              .text('Nested ancestor Tooltip')
              .evaluate()
              .isNotEmpty;
          expect(tooltipState.ensureTooltipVisible(), !wasVisible);
          await tester.pumpAndSettle();
          expect(find.text('Nested ancestor Tooltip'), findsOneWidget);

          final anchorPoint = tester.getCenter(_anchor(4));
          final overlayPoint = tester.getCenter(
            find.text('Nested ancestor Tooltip'),
          );
          String? dropSignature(Offset point) {
            final surface = tester.getRect(find.byType(CanvasDocumentView));
            final target = resolver!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'text',
                widgetType: 'flutter.widgets.Text',
                traits: const {},
              ),
            );
            if (target == null) return null;
            expect(
              target.parentWidgetId,
              isIn([_id(900), _id(901), _id(11), _id(2), _id(3)]),
            );
            return '${target.parentWidgetId}:${target.slotName}:${target.insertionIndex}';
          }

          final anchorDrop = dropSignature(anchorPoint);
          final overlayDrop = dropSignature(overlayPoint);
          await _pump(
            tester,
            root,
            scaffoldProperties: {
              ...scaffoldProperties,
              'backgroundColor': {'kind': 'color', 'argb': '0xFFF1F2F3'},
            },
            onDrop: (value) => resolver = value,
          );
          await tester.pumpAndSettle();
          expect(
            tester.state<TooltipState>(_sdkTooltip('Nested ancestor Tooltip')),
            same(tooltipState),
          );
          expect(
            tester.state<EditableTextState>(find.byType(EditableText)),
            same(fieldState),
          );
          expect(fieldState.widget.controller.text, 'Transient input stays');
          expect(fieldState.widget.focusNode.canRequestFocus, isFalse);
          expect(fieldState.widget.focusNode.hasFocus, isFalse);
          expect(find.text('Nested ancestor Tooltip'), findsOneWidget);
          expect(dropSignature(anchorPoint), anchorDrop);
          expect(dropSignature(overlayPoint), overlayDrop);
          expect(jsonEncode(subtree), sourceValues);
          expect(jsonEncode(root), ancestorValues);
          expect(tester.takeException(), isNull);
          previousTooltipState = tooltipState;
          previousFieldState = fieldState;
        }
        await tester.pumpWidget(const SizedBox.shrink());
        await tester.pump(const Duration(seconds: 2));
        await tester.pumpAndSettle();
        expect(find.text('Nested ancestor Tooltip'), findsNothing);
        expect(resolver, isNull);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'ExpansionTile alpha and shape branches keep open nested Tooltip lifecycle safe',
    (tester) async {
      Map<String, Object?> color(String argb) => {
        'kind': 'color',
        'argb': argb,
      };
      final inner = _tooltip(
        id: 3,
        properties: {'message': _s('Expansion header Tooltip')},
        child: _node(4, 'flutter.material.TextField', {}),
      );
      final title = _tooltip(child: inner);
      final titleBytes = jsonEncode(title);
      final profiles = <Map<String, Object?>>[
        {
          'backgroundColor': color('0x00112233'),
          'collapsedBackgroundColor': color('0x00334455'),
        },
        {
          'backgroundColor': color('0xFF112233'),
          'collapsedBackgroundColor': color('0xFF334455'),
        },
        {
          'backgroundColor': color('0x80112233'),
          'collapsedBackgroundColor': color('0x80334455'),
        },
        {
          'backgroundColor': color('0x00112233'),
          'collapsedBackgroundColor': color('0x00334455'),
        },
        {'shapeKind': _s('roundedRectangle')},
        {},
      ];
      for (final expanded in [false, true]) {
        CanvasDropResolver? resolver;
        for (final profile in profiles) {
          final tile = _node(
            11,
            'flutter.material.ExpansionTile',
            {
              'initiallyExpanded': _b(expanded),
              'maintainState': _b(true),
              ...profile,
            },
            {
              'title': _single(title),
              'children': {
                'kind': 'list',
                'children': [_text(5, 'Retained expanded content')],
              },
            },
          );
          final tileBytes = jsonEncode(tile);
          await _pump(
            tester,
            tile,
            onDrop: (value) => resolver = value,
            scenario: 'ExpansionTile expanded=$expanded profile=$profile',
          );
          await tester.pumpAndSettle();
          expect(jsonEncode(tile), tileBytes);
          expect(jsonEncode(title), titleBytes);
          expect(_anchor(11), findsOneWidget);
          expect(_anchor(2), findsOneWidget);
          expect(_anchor(3), findsOneWidget);
          expect(_anchor(4), findsOneWidget);
          expect(find.byType(TextField), findsOneWidget);
          final state = tester.state<TooltipState>(
            _sdkTooltip('Expansion header Tooltip'),
          );
          state.ensureTooltipVisible();
          await tester.pumpAndSettle();
          expect(find.text('Expansion header Tooltip'), findsOneWidget);
          expect(find.text(_plain), findsNothing);
          final surface = tester.getRect(find.byType(CanvasDocumentView));
          for (final point in [
            tester.getCenter(_anchor(4)),
            tester.getCenter(find.text('Expansion header Tooltip')),
          ]) {
            final target = resolver!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'text',
                widgetType: 'flutter.widgets.Text',
                traits: const {},
              ),
            );
            if (target != null) {
              expect(
                target.parentWidgetId,
                isIn([_id(900), _id(901), _id(11), _id(2), _id(3)]),
              );
            }
          }
          expect(tester.takeException(), isNull);
        }
        await tester.pumpWidget(const SizedBox.shrink());
        await tester.pump(const Duration(seconds: 2));
        await tester.pumpAndSettle();
        expect(find.text('Expansion header Tooltip'), findsNothing);
        expect(resolver, isNull);
        expect(tester.takeException(), isNull);
      }
    },
  );

  for (final visible in [false, true]) {
    testWidgets(
      'removing the Canvas disposes ${visible ? 'visible overlay and exit' : 'pending hover wait'} timers',
      (tester) async {
        CanvasDropResolver? resolver;
        await _pump(
          tester,
          _tooltip(
            properties: {
              'waitDurationUs': _n(500000),
              'exitDurationUs': _n(700000),
            },
            child: _text(4, 'Removed anchor'),
          ),
          onDrop: (value) => resolver = value,
        );
        final state = tester.state<TooltipState>(_sdkTooltip(_plain));
        final mouse = await tester.createGesture(
          kind: ui.PointerDeviceKind.mouse,
        );
        await mouse.addPointer(location: Offset.zero);
        await mouse.moveTo(tester.getCenter(_anchor(4)));
        await tester.pump(Duration(milliseconds: visible ? 501 : 100));
        if (visible) {
          await tester.pumpAndSettle();
          expect(find.text(_plain), findsOneWidget);
          await mouse.moveTo(Offset.zero);
          await tester.pump(const Duration(milliseconds: 100));
          expect(find.text(_plain), findsOneWidget);
        } else {
          expect(find.text(_plain), findsNothing);
        }
        await tester.pumpWidget(const SizedBox.shrink());
        await tester.pump(const Duration(seconds: 2));
        await tester.pumpAndSettle();
        expect(state.mounted, isFalse);
        expect(resolver, isNull);
        expect(find.byType(Tooltip), findsNothing);
        expect(find.text(_plain), findsNothing);
        await mouse.removePointer();
        await tester.pump();
        expect(tester.takeException(), isNull);
        expect(tester.binding.transientCallbackCount, 0);
      },
    );
  }
}
