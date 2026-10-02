import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _ref = {'kind': 'dartObjectReferencePresence'};
String _id(int n) => 'f780ce92-859c-4349-92ea-${n.toString().padLeft(12, '0')}';
Map<String, Object?> _s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> _n(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
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
Map<String, Object?> _text(int id, String text, double fontSize) => _node(
  id,
  'flutter.widgets.Text',
  {'data': _s(text), 'styleFontSize': _n(fontSize)},
);
Map<String, Object?> _tooltip({required bool warning, Object? child}) => _node(
  2,
  'flutter.material.Tooltip',
  {'message': _s('Geometry tooltip'), if (warning) 'positionDelegate': _ref},
  child == null ? {} : {'child': _single(child)},
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
Finder _sdkFinder() => find.byWidgetPredicate(
  (widget) =>
      widget is Tooltip &&
      widget.message == 'Geometry tooltip' &&
      widget.key is GlobalKey,
);
RenderBox _box(WidgetTester tester, int id) =>
    tester.renderObject<RenderBox>(_anchor(id));
Future<void> _pump(
  WidgetTester tester,
  Object root, {
  String? selected,
  ValueChanged<CanvasDropResolver?>? onDrop,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      themeAnimationDuration: Duration.zero,
      home: CanvasDocumentView(
        model: _model(root),
        selectedWidgetId: selected,
        onSelected: (_) {},
        onDropResolverChanged: onDrop,
      ),
    ),
  );
  await tester.pump();
  await tester.pump();
  expect(tester.takeException(), isNull);
}

// The SDK permits actual baseline queries only from a parent's layout/paint or
// its diagnostic intrinsic-check pass. Test reads happen after the frame.
double? _actualBaseline(RenderBox box, TextBaseline baseline) {
  final previous = RenderObject.debugCheckingIntrinsics;
  RenderObject.debugCheckingIntrinsics = true;
  try {
    return box.getDistanceToBaseline(baseline, onlyReal: true);
  } finally {
    RenderObject.debugCheckingIntrinsics = previous;
  }
}

List<double?> _baselines(RenderBox box) => [
  for (final baseline in TextBaseline.values) ...[
    _actualBaseline(box, baseline),
    box.getDryBaseline(box.constraints, baseline),
  ],
];
List<double> _intrinsics(RenderBox box) => [
  box.getMinIntrinsicWidth(double.infinity),
  box.getMaxIntrinsicWidth(double.infinity),
  box.getMinIntrinsicHeight(box.size.width),
  box.getMaxIntrinsicHeight(box.size.width),
];
List<Object?> _dropSample(CanvasDropResolver resolver) {
  const points = [
    0,
    250000,
    475000,
    490000,
    495000,
    500000,
    505000,
    510000,
    525000,
    750000,
    1000000,
  ];
  final source = CanvasPaletteDragSource(
    token: 'tooltip-geometry-test',
    widgetType: 'flutter.widgets.Text',
    traits: {},
  );
  return [
    for (final x in points)
      for (final y in points)
        (() {
          final target = resolver(x, y, source);
          if (target == null) return null;
          final zone = target.zone;
          return [
            target.parentWidgetId,
            target.slotName,
            target.insertionIndex,
            zone?.leftMicros,
            zone?.topMicros,
            zone?.rightMicros,
            zone?.bottomMicros,
          ];
        })(),
  ];
}

void main() {
  testWidgets(
    'diagnostic sibling preserves actual and dry alphabetic and ideographic baselines',
    (tester) async {
      final child = _text(3, 'Ag', 48);
      await _pump(tester, _tooltip(warning: false, child: child));
      final sdkState = tester.state<TooltipState>(_sdkFinder());
      final baseline = _baselines(_box(tester, 2));
      final intrinsics = _intrinsics(_box(tester, 2));
      final size = _box(tester, 2).size;
      expect(baseline.every((value) => value != null && value > 12), true);
      for (final warning in [true, false, true, false]) {
        await _pump(
          tester,
          _tooltip(warning: warning, child: child),
          selected: _id(2),
        );
        expect(
          _baselines(_box(tester, 2)),
          baseline,
          reason: 'warning=$warning',
        );
        expect(_intrinsics(_box(tester, 2)), intrinsics);
        expect(_box(tester, 2).size, size);
        expect(tester.state<TooltipState>(_sdkFinder()), same(sdkState));
        expect(
          find.byKey(ValueKey('canvas-tooltip-diagnostic-${_id(2)}')),
          warning ? findsOneWidget : findsNothing,
        );
        expect(tester.takeException(), isNull);
      }
    },
  );

  for (final baseline in TextBaseline.values) {
    for (final wrapper in ['none', 'IntrinsicWidth', 'IntrinsicHeight']) {
      testWidgets(
        '$wrapper baseline Row retains both child positions when a Tooltip diagnostic changes (${baseline.name})',
        (tester) async {
          Object tree(bool warning) {
            final row = _node(
              10,
              'flutter.widgets.Row',
              {
                'mainAxisSize': _e('MainAxisSize', 'min'),
                'crossAxisAlignment': _e('CrossAxisAlignment', 'baseline'),
                'textBaseline': _e('TextBaseline', baseline.name),
              },
              {
                'children': _list([
                  _tooltip(warning: warning, child: _text(3, 'Ag', 48)),
                  _text(4, 'Peer', 16),
                ]),
              },
            );
            return wrapper == 'none'
                ? row
                : _node(11, 'flutter.widgets.$wrapper', {}, {
                    'child': _single(row),
                  });
          }

          await _pump(tester, tree(false));
          final firstRect = tester.getRect(_anchor(2));
          final peerRect = tester.getRect(_anchor(4));
          final rowRect = tester.getRect(_anchor(10));
          final rowBaselines = _baselines(_box(tester, 10));
          final actualGlobalBaseline = _box(tester, 2)
              .localToGlobal(
                Offset(0, _actualBaseline(_box(tester, 2), baseline)!),
              )
              .dy;
          final peerGlobalBaseline = _box(tester, 4)
              .localToGlobal(
                Offset(0, _actualBaseline(_box(tester, 4), baseline)!),
              )
              .dy;
          expect(actualGlobalBaseline, closeTo(peerGlobalBaseline, 0.0001));
          for (final warning in [true, false]) {
            await _pump(tester, tree(warning), selected: _id(2));
            expect(tester.getRect(_anchor(2)), firstRect);
            expect(tester.getRect(_anchor(4)), peerRect);
            expect(tester.getRect(_anchor(10)), rowRect);
            expect(_baselines(_box(tester, 10)), rowBaselines);
            expect(tester.takeException(), isNull);
          }
        },
      );
    }
  }

  testWidgets(
    'Tooltip selection and warning do not change an unrelated intrinsic subtree',
    (tester) async {
      Object tree(bool warning) => _node(
        10,
        'flutter.widgets.Row',
        {'mainAxisSize': _e('MainAxisSize', 'min')},
        {
          'children': _list([
            _tooltip(warning: warning, child: _text(3, 'Ag', 40)),
            _node(11, 'flutter.widgets.IntrinsicWidth', {}, {
              'child': _single(
                _node(12, 'flutter.widgets.IntrinsicHeight', {}, {
                  'child': _single(
                    _node(
                      13,
                      'flutter.widgets.Column',
                      {'mainAxisSize': _e('MainAxisSize', 'min')},
                      {
                        'children': _list([
                          _text(4, 'Peer', 16),
                          _text(5, 'Body', 24),
                        ]),
                      },
                    ),
                  ),
                }),
              ),
            }),
          ]),
        },
      );
      await _pump(tester, tree(false));
      final ids = [11, 12, 13, 4, 5];
      final rectangles = [for (final id in ids) tester.getRect(_anchor(id))];
      final baselines = [for (final id in ids) _baselines(_box(tester, id))];
      final intrinsics = [for (final id in ids) _intrinsics(_box(tester, id))];
      for (final selected in [_id(2), _id(3), _id(10), _id(11), null]) {
        for (final warning in [true, false]) {
          await _pump(tester, tree(warning), selected: selected);
          expect([
            for (final id in ids) tester.getRect(_anchor(id)),
          ], rectangles);
          expect([
            for (final id in ids) _baselines(_box(tester, id)),
          ], baselines);
          expect([
            for (final id in ids) _intrinsics(_box(tester, id)),
          ], intrinsics);
          expect(tester.takeException(), isNull);
        }
      }
    },
  );

  for (final absent in [true, false]) {
    testWidgets(
      'warning does not fabricate anchor size or alter semantic drop zones with ${absent ? 'absent' : 'zero-sized'} child',
      (tester) async {
        CanvasDropResolver? resolver;
        Object tree(bool warning) => _tooltip(
          warning: warning,
          child: absent
              ? null
              : _node(3, 'flutter.widgets.SizedBox', {
                  'width': _n(0),
                  'height': _n(0),
                }),
        );
        await _pump(tester, tree(false), onDrop: (value) => resolver = value);
        expect(resolver, isNotNull);
        final zones = _dropSample(resolver!);
        final anchorRect = tester.getRect(_anchor(2));
        expect(anchorRect.size, Size.zero);
        expect(tester.getSize(_sdkFinder()), Size.zero);
        // Flutter's own childless SizedBox/Tooltip does not implement a dry
        // baseline. Actual baseline and zero geometry still must stay absent.
        for (final baseline in TextBaseline.values) {
          expect(_actualBaseline(_box(tester, 2), baseline), isNull);
        }
        for (final warning in [true, false]) {
          await _pump(
            tester,
            tree(warning),
            selected: _id(2),
            onDrop: (value) => resolver = value,
          );
          expect(tester.getRect(_anchor(2)), anchorRect);
          expect(tester.getSize(_sdkFinder()), Size.zero);
          for (final baseline in TextBaseline.values) {
            expect(_actualBaseline(_box(tester, 2), baseline), isNull);
          }
          expect(_intrinsics(_box(tester, 2)), everyElement(0.0));
          expect(
            _dropSample(resolver!),
            zones,
            reason: 'diagnostic is not a new model drop destination',
          );
          if (!absent) expect(_box(tester, 3).size, Size.zero);
          final sdk = tester.widget<Tooltip>(_sdkFinder());
          expect(sdk.child == null, absent);
          expect(tester.takeException(), isNull);
        }
      },
    );
  }
}
