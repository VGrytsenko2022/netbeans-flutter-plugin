import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_align_test.dart' as old;

const type = 'flutter.widgets.AlignTransition';
Map<String, Object?> data({
  double x = 0,
  double y = 0,
  bool directional = false,
  bool rtl = false,
  bool tight = false,
  Object? width = 'omitted',
  Object? height = 'omitted',
}) {
  final raw = old.data(
        x: x,
        y: y,
        directional: directional,
        rtl: rtl,
        tight: tight,
        width: width,
        height: height,
      ),
      node = a.builder(raw);
  node['type'] = type;
  (node['properties'] as Map).remove('durationUs');
  return raw;
}

Finder native() => find.byType(AlignTransition);
Finder child() => find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
void main() {
  test('exact required domains, factors and root/occupied child wrapping', () {
    final raw = data(),
        props = a.builder(raw)['properties'] as Map,
        alignment = props.remove('alignment');
    expect(() => f.decode(raw), throwsFormatException);
    props['alignment'] = {'kind': 'null'};
    expect(() => f.decode(raw), throwsFormatException);
    props['alignment'] = alignment;
    expect(canvasDropSlotsForWidgetType(type), isEmpty);
    expect(
      canvasExistingChildWrapTargetSlot(
        parentWidgetType: type,
        slotName: 'child',
      )!.slotName,
      'child',
    );
    expect(
      canvasWrapperAcceptsExistingChild(
        wrapperWidgetType: type,
        childWidgetType: 'flutter.widgets.Text',
      ),
      true,
    );
    for (final invalid in [
      'flutter.widgets.Expanded',
      'flutter.widgets.PositionedTransition',
      'flutter.widgets.SliverToBoxAdapter',
    ]) {
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: type,
          childWidgetType: invalid,
        ),
        false,
      );
    }
    for (final field in [
      'durationUs',
      'curve',
      'onEnd',
      'clipBehavior',
      'textDirection',
    ]) {
      props[field] = {'kind': 'boolean', 'value': true};
      expect(() => f.decode(raw), throwsFormatException);
      props.remove(field);
    }
    for (final field in ['widthFactor', 'heightFactor']) {
      for (final value in [-1, double.infinity, double.nan]) {
        props[field] = f.number(value.toDouble());
        expect(() => f.decode(raw), throwsA(anything));
      }
      props.remove(field);
    }
    raw['root'] = a.builder(raw);
    expect(() => f.decode(raw), returnsNormally);
    a.builder(raw)['slots'] = {'child': f.single(null)};
    expect(() => f.decode(raw), throwsFormatException);
  });
  for (final directional in [false, true]) {
    for (final rtl in [false, true]) {
      for (final tight in [false, true]) {
        for (final xy in [-2.0, 0.0, 1.0]) {
          for (final width in [null, 0.0, .5, 1.0, 2.0]) {
            for (final height in [null, 0.0, 1.0, 3.0]) {
              testWidgets(
                'native layout dir=$directional rtl=$rtl tight=$tight xy=$xy w=$width h=$height',
                (tester) async {
                  await f.pump(
                    tester,
                    data(
                      x: xy,
                      y: xy,
                      directional: directional,
                      rtl: rtl,
                      tight: tight,
                      width: width,
                      height: height,
                    ),
                  );
                  final n = tester.widget<AlignTransition>(native());
                  expect(
                    n.alignment,
                    isA<AlwaysStoppedAnimation<AlignmentGeometry>>(),
                  );
                  expect(
                    n.alignment.value,
                    directional
                        ? AlignmentDirectional(xy, xy)
                        : Alignment(xy, xy),
                  );
                  expect(n.widthFactor, width);
                  expect(n.heightFactor, height);
                  final box = tester.renderObject<RenderBox>(native()),
                      body = tester.renderObject<RenderBox>(child());
                  final size = tight
                      ? const Size(240, 160)
                      : Size(
                          width == null ? 240 : (48 * width).clamp(0, 240),
                          height == null ? 160 : (48 * height).clamp(0, 160),
                        );
                  expect(box.size, size);
                  expect(body.size, const Size(48, 48));
                  final expected = Offset(
                    (size.width - 48) *
                        (1 + (directional && rtl ? -xy : xy)) /
                        2,
                    (size.height - 48) * (1 + xy) / 2,
                  );
                  expect(
                    body.localToGlobal(Offset.zero, ancestor: box),
                    expected,
                  );
                  expect(tester.takeException(), isNull);
                },
              );
            }
          }
        }
      }
    }
  }
  testWidgets(
    'null and omission reset factors immediately and references preserve state',
    (tester) async {
      await f.pump(tester, data(width: 2, height: 3));
      final state = tester.state(native()), body = tester.element(child());
      for (final mode in ['source', 'null', 'omitted', 'directional']) {
        final raw = data(
          x: 1,
          y: -1,
          directional: mode == 'directional',
          width: mode == 'null' ? null : 'omitted',
          height: mode == 'null' ? null : 'omitted',
        );
        if (mode == 'source') {
          final props = a.builder(raw)['properties'] as Map;
          for (final f in ['alignment', 'widthFactor', 'heightFactor']) {
            props[f] = {'kind': 'dartObjectReferencePresence'};
          }
        }
        await f.pump(tester, raw);
        final n = tester.widget<AlignTransition>(native());
        expect(n.widthFactor, isNull);
        expect(n.heightFactor, isNull);
        if (mode == 'source') {
          expect(n.alignment.value, Alignment.center);
        }
        expect(
          find.byWidgetPredicate(
            (w) =>
                w is Tooltip && w.message?.contains('AlignTransition') == true,
          ),
          mode == 'source' ? findsWidgets : findsNothing,
        );
        expect(identical(state, tester.state(native())), true);
        expect(identical(body, tester.element(child())), true);
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets('source fallback substitutes only its own field', (tester) async {
    for (final field in ['alignment', 'widthFactor', 'heightFactor']) {
      final raw = data(x: 1, y: -.5, width: 2, height: .5);
      (a.builder(raw)['properties'] as Map)[field] = {
        'kind': 'dartObjectReferencePresence',
      };
      await f.pump(tester, raw);
      final n = tester.widget<AlignTransition>(native());
      expect(n.alignment.value,
        field == 'alignment' ? Alignment.center : const Alignment(1, -.5));
      expect(n.widthFactor, field == 'widthFactor' ? null : 2);
      expect(n.heightFactor, field == 'heightFactor' ? null : .5);
      final expected = field == 'alignment' ? 'alignment = center' : '$field = null';
      expect(find.byWidgetPredicate((w) => w is Tooltip &&
        w.message?.contains('Canvas substitutes $expected.') == true), findsWidgets);
      expect(tester.takeException(), isNull);
    }
  });
  testWidgets(
    'factor zero offers external selection without deleting required child',
    (tester) async {
      final selected = <String>[];
      await f.pump(tester, data(width: 0, height: 0), selected: selected.add);
      expect(tester.getSize(native()), Size.zero);
      expect(child(), findsOneWidget);
      expect(find.text('Opacity body'), findsOneWidget);
      final handle = find.byKey(const ValueKey('canvas-zero-size-widget-target-${a.builderId}'));
      expect(handle, findsOneWidget);
      await tester.tap(handle);
      expect(selected, contains(a.builderId));
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets('large finite local coordinates cannot poison Canvas layout', (
    tester,
  ) async {
    await f.pump(tester, data(x: 1e308, y: -1e308, width: .5, height: .5));
    await tester.pump();
    final state = tester.state(native()), body = tester.element(child());
    expect(find.byWidgetPredicate((w) => w is Tooltip &&
      w.message?.contains('native alignment offset is non-finite') == true), findsWidgets);
    expect(tester.widget<AlignTransition>(native()).alignment.value, const Alignment(1e308, -1e308));
    expect(tester.takeException(), isNull);
    await f.pump(tester, data(x: 0, y: 0));
    await tester.pump();
    expect(identical(state, tester.state(native())), true);
    expect(identical(body, tester.element(child())), true);
    expect(find.byWidgetPredicate((w) => w is Tooltip &&
      w.message?.contains('native alignment offset is non-finite') == true), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
