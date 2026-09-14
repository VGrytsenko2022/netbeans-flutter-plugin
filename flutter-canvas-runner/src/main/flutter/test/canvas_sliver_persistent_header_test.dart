import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.SliverPersistentHeader';
Map<String, Object?> data({
  bool pinned = false,
  bool floating = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
}) {
  final raw = f.modelJson(
    empty: true,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
  );
  final node = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  node['type'] = type;
  node['properties'] = <String, Object?>{
    'delegate': {'kind': 'dartObjectReferencePresence'},
    'pinned': {'kind': 'boolean', 'value': pinned},
    'floating': {'kind': 'boolean', 'value': floating},
  };
  node['slots'] = <String, Object?>{};
  return raw;
}

void main() {
  test(
    'required delegate presence only; exact booleans and no invented children',
    () {
      for (final bad in <Object?>[
        null,
        {'kind': 'null'},
        {'kind': 'boolean', 'value': true},
        {'kind': 'string', 'value': 'empty'},
        {'kind': 'dartObjectReferencePresence', 'rootSymbol': 'secret'},
      ]) {
        final raw = data();
        final props =
            f.findNode(
                  raw['root'] as Map<String, Object?>,
                  f.fillId,
                )['properties']
                as Map;
        if (bad == null) {
          props.remove('delegate');
        } else {
          props['delegate'] = bad;
        }
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final slot in ['child', 'sliver', 'children']) {
        final raw = data();
        f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['slots'] = {
          slot: f.single(null),
        };
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final name in ['pinned', 'floating']) {
        final raw = data();
        final props =
            f.findNode(
                  raw['root'] as Map<String, Object?>,
                  f.fillId,
                )['properties']
                as Map;
        props[name] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
        props.remove(name);
        expect(f.decode(raw), isNotNull);
      }
    },
  );
  for (final pinned in [false, true]) {
    for (final floating in [false, true]) {
      for (final horizontal in [false, true]) {
        for (final reverse in [false, true]) {
          for (final rtl in [false, true]) {
            testWidgets(
              'isolated header p=$pinned f=$floating h=$horizontal rev=$reverse rtl=$rtl',
              (tester) async {
                final raw = data(
                  pinned: pinned,
                  floating: floating,
                  horizontal: horizontal,
                  reverse: reverse,
                  rtl: rtl,
                );
                await f.pump(tester, raw);
                final finder = find.byType(
                  SliverPersistentHeader,
                  skipOffstage: false,
                );
                expect(finder, findsOneWidget);
                final native = tester.widget<SliverPersistentHeader>(finder);
                expect(native.pinned, pinned);
                expect(native.floating, floating);
                expect(native.delegate.minExtent, 56);
                expect(native.delegate.maxExtent, 112);
                expect(native.delegate.vsync, isNull);
                expect(native.delegate.snapConfiguration, isNull);
                expect(native.delegate.stretchConfiguration, isNull);
                final render = tester.renderObject<RenderSliver>(finder);
                expect(render.geometry!.scrollExtent, 112);
                expect(render.geometry!.paintExtent, greaterThanOrEqualTo(0));
                final hints = find
                    .byType(Tooltip)
                    .evaluate()
                    .map((e) => (e.widget as Tooltip).message ?? '')
                    .join(' ');
                expect(hints, contains('project delegate is not executed'));
                expect(hints, contains('56–112'));
                await f.pump(tester, raw);
                expect(identical(render, tester.renderObject(finder)), isTrue);
                expect(tester.takeException(), isNull);
                expect(find.byType(CanvasDocumentView), findsOneWidget);
              },
            );
          }
        }
      }
    }
  }
}
