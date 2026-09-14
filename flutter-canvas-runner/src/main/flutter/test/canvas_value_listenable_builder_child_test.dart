import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_value_listenable_builder_test.dart' as box;
import 'canvas_value_listenable_builder_sliver_test.dart' as sliver;

const childId = 'ce8c975e-9e78-4364-9622-ae0bbd292736';
Map<String, Object?> model(bool rawSliver, bool source, bool builder) {
  final raw = rawSliver
      ? sliver.data(reference: builder)
      : box.data(reference: builder);
  final n = f.findNode(
    raw['root'] as Map<String, Object?>,
    rawSliver ? f.fillId : box.builderId,
  );
  (n['properties'] as Map)['valueListenable'] = source
      ? {'kind': 'dartObjectReferencePresence'}
      : {'kind': 'string', 'value': 'constant'};
  var child = f.node(childId, 'flutter.widgets.SizedBox', {
    'width': f.number(80),
    'height': f.number(48),
  });
  if (rawSliver) {
    child = f.node(
      'ce8c975e-9e78-4364-9622-ae0bbd292737',
      'flutter.widgets.SliverToBoxAdapter',
      {},
      {'child': f.single(child)},
    );
  }
  n['slots'] = {'child': f.single(child)};
  return raw;
}

void main() {
  for (final rawSliver in [false, true]) {
    test(
      'both required properties reject missing, null, raw and private symbol payloads sliver=$rawSliver',
      () {
        for (final property in ['valueListenable', 'builder']) {
          for (final invalid in <Object?>[
            null,
            {'kind': 'null'},
            {'kind': 'boolean', 'value': true},
            {'kind': 'callbackPresence'},
            {'kind': 'string', 'value': 'wrong'},
            {'kind': 'dartObjectReferencePresence', 'rootSymbol': 'secret'},
          ]) {
            final raw = model(rawSliver, false, false);
            final n = f.findNode(
              raw['root'] as Map<String, Object?>,
              rawSliver ? f.fillId : box.builderId,
            );
            final props = n['properties'] as Map;
            if (invalid == null) {
              props.remove(property);
            } else {
              props[property] = invalid;
            }
            expect(() => f.decode(raw), throwsFormatException);
          }
        }
        final ownType = rawSliver ? sliver.type : box.type;
        expect(canvasDropSlotsForWidgetType(ownType), hasLength(1));
        expect(canvasReviewedRequiredWrapperSlot(ownType), isNull);
      },
    );
    for (final source in [false, true]) {
      for (final builder in [false, true]) {
        testWidgets(
          'prebuilt child, inert refs and stable refresh sliver=$rawSliver source=$source builder=$builder',
          (tester) async {
            final raw = model(rawSliver, source, builder);
            await f.pump(tester, raw);
            final finder = find.byKey(
              ValueKey(
                'canvas-value-listenable-builder-${rawSliver ? f.fillId : box.builderId}',
              ),
            );
            final native = tester.widget<ValueListenableBuilder>(finder);
            expect(native.child, isNotNull);
            expect(
              native.valueListenable,
              isA<AlwaysStoppedAnimation<Object?>>(),
            );
            final element = tester.element(finder);
            final hints = find
                .byType(Tooltip)
                .evaluate()
                .map((e) => (e.widget as Tooltip).message ?? '')
                .join(' ');
            expect(
              hints.contains('ValueListenableBuilder preview limitation'),
              source || builder,
            );
            if (rawSliver) {
              expect(
                tester
                    .renderObject<RenderSliver>(finder)
                    .geometry!
                    .scrollExtent,
                48,
              );
            } else {
              expect(tester.getSize(finder), const Size(80, 48));
            }
            await f.pump(tester, model(rawSliver, !source, !builder));
            expect(identical(element, tester.element(finder)), isTrue);
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
}
