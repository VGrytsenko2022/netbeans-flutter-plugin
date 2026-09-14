import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.ModalBarrier';
Map<String, Object?> data({
  String dismiss = 'unset',
  String sem = 'unset',
  String color = 'unset',
  bool rtl = false,
}) {
  final raw = a.data(tight: true, rtl: rtl);
  final n = a.builder(raw);
  n['type'] = type;
  n['slots'] = <String, Object?>{};
  n['properties'] = {
    if (dismiss != 'unset') 'dismissible': f.boolean(dismiss == 'true'),
    if (sem != 'unset')
      'barrierSemanticsDismissible': sem == 'null'
          ? {'kind': 'null'}
          : f.boolean(sem == 'true'),
    if (color != 'unset')
      'color': color == 'null'
          ? {'kind': 'null'}
          : {'kind': 'color', 'argb': color == 'clear' ? '0x00000000' : '0x88223344'},
    'semanticsLabel': {'kind': 'string', 'value': 'Close dialog'},
    'semanticsOnTapHint': {'kind': 'string', 'value': 'dismiss dialog'},
  };
  return raw;
}

void main() {
  test('leaf capabilities and exact nullable domains', () {
    final raw = data();
    expect(f.decode(raw).root, isNotNull);
    expect(canvasDropSlotsForWidgetType(type), isEmpty);
    expect(isCanvasPaletteWrapperWidgetType(type), false);
    for (final name in ['onDismiss', 'clipDetailsNotifier', 'color']) {
      for (final kind in ['null', 'dartObjectReferencePresence']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map)[name] = {'kind': kind};
        expect(f.decode(raw).root, isNotNull);
      }
    }
    for (final entry in {
      'dismissible': {'kind': 'null'},
      'clipDetailsNotifier': f.number(2),
      'onDismiss': {'kind': 'string', 'value': 'raw()'},
      'semanticsLabel': f.boolean(true),
    }.entries) {
      final raw = data();
      (a.builder(raw)['properties'] as Map)[entry.key] = entry.value;
      expect(() => f.decode(raw), throwsFormatException);
    }
  });
  for (final dismiss in ['unset', 'true', 'false']) {
    for (final sem in ['unset', 'null', 'true', 'false']) {
      for (final color in ['unset', 'null', 'clear', 'solid']) {
        for (final rtl in [false, true]) {
          testWidgets(
            'native args dismiss=$dismiss semantics=$sem color=$color rtl=$rtl',
            (tester) async {
              await f.pump(
                tester,
                data(dismiss: dismiss, sem: sem, color: color, rtl: rtl),
              );
              final n = tester.widget<ModalBarrier>(find.byKey(const ValueKey('canvas-modal-barrier-${a.builderId}')));
              expect(n.dismissible, dismiss != 'false');
              expect(
                n.barrierSemanticsDismissible,
                sem == 'null' ? null : sem != 'false',
              );
              expect(
                n.color,
                color == 'unset' || color == 'null'
                    ? null
                    : Color(color == 'clear' ? 0 : 0x88223344),
              );
              expect(n.semanticsLabel, 'Close dialog');
              expect(n.semanticsOnTapHint, 'dismiss dialog');
              expect(n.clipDetailsNotifier, isNull);
              expect(n.onDismiss, isNotNull);
              expect(
                tester.getSize(find.byKey(const ValueKey('canvas-modal-barrier-${a.builderId}'))),
                const Size(240, 160),
              );
              expect(tester.takeException(), isNull);
            },
          );
        }
      }
    }
  }
  testWidgets(
    'canvas never pops routes or emits alert and remains selectable',
    (tester) async {
      final sounds = <MethodCall>[];
      tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        SystemChannels.platform,
        (call) async {
          if (call.method == 'SystemSound.play') sounds.add(call);
          return null;
        },
      );
      addTearDown(
        () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
          SystemChannels.platform,
          null,
        ),
      );
      for (final dismiss in ['true', 'false']) {
        String? selected;
        await f.pump(
          tester,
          data(dismiss: dismiss),
          selected: (id) => selected = id,
        );
        final barrier = find.byKey(const ValueKey('canvas-modal-barrier-${a.builderId}'));
        final element = tester.element(barrier);
        final navigator = Navigator.of(element);
        expect(navigator.canPop(), false);
        await tester.tapAt(tester.getCenter(barrier));
        await tester.pumpAndSettle();
        expect(selected, a.builderId);
        expect(identical(element, tester.element(barrier)), true);
        expect(navigator.canPop(), false);
        expect(sounds, isEmpty);
      }
    },
  );
  testWidgets('project fields use labelled isolated fallbacks', (tester) async {
    final raw = data(color: 'solid');
    await f.pump(tester, raw);
    final before = tester.element(find.byKey(const ValueKey('canvas-modal-barrier-${a.builderId}')));
    for (final field in ['clipDetailsNotifier', 'color', 'onDismiss']) {
      final raw = data(color: 'solid');
      (a.builder(raw)['properties'] as Map)[field] = {
        'kind': 'dartObjectReferencePresence',
      };
      await f.pump(tester, raw);
      final n = tester.widget<ModalBarrier>(find.byKey(const ValueKey('canvas-modal-barrier-${a.builderId}')));
      expect(n.clipDetailsNotifier, isNull);
      expect(n.color, field == 'color' ? null : const Color(0x88223344));
      expect(
        identical(before, tester.element(find.byKey(const ValueKey('canvas-modal-barrier-${a.builderId}')))),
        true,
      );
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('Project-owned $field') == true,
        ),
        findsWidgets,
      );
      expect(tester.takeException(), isNull);
    }
  });
  testWidgets(
    'unbounded direct Column does not crash or invent stored dimensions',
    (tester) async {
      final raw = data();
      final n = a.builder(raw);
      ((raw['root'] as Map)['slots'] as Map)['children'] = {
        'kind': 'list',
        'children': [n],
      };
      await f.pump(tester, raw);
      expect(find.byKey(const ValueKey('canvas-modal-barrier-${a.builderId}')), findsNothing);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('height is unbounded') == true,
        ),
        findsWidgets,
      );
      expect(tester.takeException(), isNull);
      expect((n['properties'] as Map).containsKey('height'), false);
      await f.pump(tester, data());
      expect(find.byKey(const ValueKey('canvas-modal-barrier-${a.builderId}')), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}
