import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_sliver_opacity_test.dart' as o;

const type = 'flutter.widgets.SliverSafeArea';
const fields = ['left', 'top', 'right', 'bottom'];
const nestedId = 'ed3d53da-bd7a-4cf7-8956-22d72d6d0c2e';
const system = EdgeInsets.fromLTRB(7, 11, 13, 17);
const signedMinimum = EdgeInsets.fromLTRB(-3.5, 19.25, 21, -2);
bool flag(int bits, int i) => bits & (1 << i) != 0;
Map<String, Object?> insets(EdgeInsets v) => {
  'kind': 'edgeInsets',
  'left': v.left,
  'top': v.top,
  'right': v.right,
  'bottom': v.bottom,
};
Map<String, Object?> data({
  int? bits,
  EdgeInsets? minimum,
  bool empty = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  double preceding = 0,
}) {
  final raw = o.data(
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    preceding: preceding,
  );
  final node = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  node['type'] = type;
  node['properties'] = {
    if (bits != null)
      for (var i = 0; i < 4; i++) fields[i]: f.boolean(flag(bits, i)),
    if (minimum != null) 'minimum': insets(minimum),
  };
  if (empty) {
    final child = f.findNode(raw['root'] as Map<String, Object?>, o.adapterId);
    child['slots'] = <String, Object?>{};
  }
  return raw;
}

void setSystem(WidgetTester tester, {bool keyboard = false}) {
  tester.view.devicePixelRatio = 1;
  tester.view.padding = FakeViewPadding(
    left: 7,
    top: 11,
    right: 13,
    bottom: keyboard ? 0 : 17,
  );
  tester.view.viewPadding = const FakeViewPadding(
    left: 7,
    top: 11,
    right: 13,
    bottom: 17,
  );
  tester.view.viewInsets = FakeViewPadding(bottom: keyboard ? 100 : 0);
  addTearDown(tester.view.reset);
}

EdgeInsets effective(
  int bits,
  EdgeInsets minimum, [
  EdgeInsets padding = system,
]) {
  double side(int i, double system, double min) =>
      math.max(flag(bits, i) ? system : 0, min);
  return EdgeInsets.fromLTRB(
    side(0, padding.left, minimum.left),
    side(1, padding.top, minimum.top),
    side(2, padding.right, minimum.right),
    side(3, padding.bottom, minimum.bottom),
  );
}

void main() {
  test('strict complete constructor domains and required sliver', () {
    final raw = data();
    final node = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
    for (final name in fields) {
      for (final bad in [
        {'kind': 'null'},
        {'kind': 'string', 'value': 'true'},
        f.number(1),
      ]) {
        node['properties'] = {name: bad};
        expect(() => f.decode(raw), throwsFormatException);
      }
    }
    for (final bad in [
      {'kind': 'null'},
      {'kind': 'string', 'value': 'EdgeInsets.zero'},
      {
        'kind': 'edgeInsetsDirectional',
        'start': 1,
        'top': 2,
        'end': 3,
        'bottom': 4,
      },
      {
        'kind': 'edgeInsets',
        'left': 'invalid',
        'top': 0,
        'right': 0,
        'bottom': 0,
      },
    ]) {
      node['properties'] = {'minimum': bad};
      expect(() => f.decode(raw), throwsFormatException);
    }
    node['properties'] = {'minimum': insets(signedMinimum)};
    expect(() => f.decode(raw), returnsNormally);
    node['properties'] = {'maintainBottomViewPadding': f.boolean(true)};
    expect(() => f.decode(raw), throwsFormatException);
    node['properties'] = <String, Object?>{};
    for (final slots in [
      <String, Object?>{},
      {'sliver': f.single(null)},
      {
        'sliver': {'kind': 'list', 'children': []},
      },
      {'sliver': f.single(f.node(nestedId, 'flutter.widgets.SizedBox'))},
    ]) {
      node['slots'] = slots;
      expect(() => f.decode(raw), throwsFormatException);
    }
    final rootRaw = data();
    rootRaw['root'] = f.findNode(
      rootRaw['root'] as Map<String, Object?>,
      f.fillId,
    );
    expect(() => f.decode(rootRaw), throwsFormatException);
  });

  for (final bits in <int?>[null, ...List.generate(16, (i) => i)]) {
    for (final minimum in <EdgeInsets?>[null, signedMinimum]) {
      for (final horizontal in [false, true]) {
        for (final reverse in [false, true]) {
          for (final rtl in [false, true]) {
            testWidgets(
              'native physical padding bits=$bits min=$minimum h=$horizontal r=$reverse rtl=$rtl',
              (tester) async {
                setSystem(tester);
                final raw = data(
                  bits: bits,
                  minimum: minimum,
                  horizontal: horizontal,
                  reverse: reverse,
                  rtl: rtl,
                );
                await f.pump(tester, raw);
                final native = tester.widget<SliverSafeArea>(
                  find.byType(SliverSafeArea),
                );
                final b = bits ?? 15;
                expect(
                  [native.left, native.top, native.right, native.bottom],
                  [for (var i = 0; i < 4; i++) flag(b, i)],
                );
                expect(native.minimum, minimum ?? EdgeInsets.zero);
                final applied = effective(b, minimum ?? EdgeInsets.zero);
                final padding = tester.renderObject<RenderSliverPadding>(
                  find.byType(SliverSafeArea),
                );
                expect(padding.padding, applied);
                expect(
                  padding.geometry!.scrollExtent,
                  48 + (horizontal ? applied.horizontal : applied.vertical),
                );
                expect(padding.child!.geometry!.scrollExtent, 48);
                final child = tester.element(
                  find.byKey(const ValueKey('canvas-widget-${o.adapterId}')),
                );
                expect(
                  MediaQuery.paddingOf(child),
                  EdgeInsets.fromLTRB(
                    flag(b, 0) ? 0 : system.left,
                    flag(b, 1) ? 0 : system.top,
                    flag(b, 2) ? 0 : system.right,
                    flag(b, 3) ? 0 : system.bottom,
                  ),
                );
                expect(tester.takeException(), isNull);
              },
            );
          }
        }
      }
    }
  }
  for (final keyboard in [false, true]) {
    testWidgets('nested removal, minimum and keyboard padding=$keyboard', (
      tester,
    ) async {
      setSystem(tester, keyboard: keyboard);
      final raw = data(minimum: signedMinimum);
      final outer = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      final oldChild =
          ((outer['slots'] as Map)['sliver'] as Map)['child']
              as Map<String, Object?>;
      outer['slots'] = {
        'sliver': f.single(
          f.node(
            nestedId,
            type,
            {'minimum': insets(signedMinimum)},
            {'sliver': f.single(oldChild)},
          ),
        ),
      };
      await f.pump(tester, raw);
      final natives = find.byType(SliverSafeArea);
      final outerRender = tester.renderObject<RenderSliverPadding>(
        natives.at(0),
      );
      final innerRender = tester.renderObject<RenderSliverPadding>(
        natives.at(1),
      );
      expect(
        outerRender.padding,
        effective(
          15,
          signedMinimum,
          keyboard ? system.copyWith(bottom: 0) : system,
        ),
      );
      expect(
        innerRender.padding,
        effective(15, signedMinimum, EdgeInsets.zero),
      );
      final child = tester.element(
        find.byKey(const ValueKey('canvas-widget-${o.adapterId}')),
      );
      expect(MediaQuery.paddingOf(child), EdgeInsets.zero);
      // The Canvas host Scaffold can consume keyboard viewInsets before the preview.
      // SliverSafeArea must preserve whatever reaches its own context.
      expect(
        MediaQuery.viewInsetsOf(child),
        MediaQuery.viewInsetsOf(tester.element(natives.at(0))),
      );
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets(
    'editing every field preserves child identity and updates descendant media padding',
    (tester) async {
      setSystem(tester);
      await f.pump(tester, data());
      final childFinder = find.byKey(
        const ValueKey('canvas-widget-${f.bodyId}'),
      );
      final initial = tester.renderObject(childFinder);
      for (var bits = 0; bits < 16; bits++) {
        await f.pump(tester, data(bits: bits, minimum: signedMinimum));
        expect(identical(initial, tester.renderObject(childFinder)), isTrue);
        final native = tester.renderObject<RenderSliverPadding>(
          find.byType(SliverSafeArea),
        );
        expect(native.padding, effective(bits, signedMinimum));
        expect(tester.takeException(), isNull);
      }
    },
  );
  for (final empty in [false, true]) {
    testWidgets('required wrapper drop and selection targets empty=$empty', (
      tester,
    ) async {
      CanvasDropResolver? resolver;
      await f.pump(
        tester,
        data(empty: empty, minimum: const EdgeInsets.all(12)),
        drop: (r) => resolver = r,
      );
      final canvas = tester.getRect(find.byType(CanvasDocumentView));
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-${f.fillId}'),
      );
      expect(handle, findsOneWidget);
      final point = tester.getCenter(handle);
      final source = CanvasPaletteDragSource(
        token: 'safe',
        widgetType: type,
        traits: {canvasSliverWidgetTrait},
      );
      final target = resolver!(
        ((point.dx - canvas.left) / canvas.width * 1000000).round(),
        ((point.dy - canvas.top) / canvas.height * 1000000).round(),
        source,
      );
      expect(target, isNotNull);
      // Occupied geometry targets the exact nested required slot; an empty
      // child leaves the owning wrapper as the visible wrap target.
      expect(target!.parentWidgetId, empty ? o.viewportId : f.fillId);
      expect(target.slotName, empty ? 'slivers' : 'sliver');
      expect(target.insertionIndex, 0);
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets('offscreen wrappers do not create misplaced overlay targets', (
    tester,
  ) async {
    await f.pump(
      tester,
      data(preceding: 700, minimum: const EdgeInsets.all(12)),
    );
    expect(
      find.byKey(const ValueKey('canvas-zero-size-widget-target-${f.fillId}')),
      findsNothing,
    );
    expect(tester.takeException(), isNull);
  });
}
