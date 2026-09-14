import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_sliver_opacity_test.dart' as o;

const type = 'flutter.widgets.SliverVisibility';
const replacementId = '74fb6766-30bc-4bf3-8d63-55814c1080bc';
const replacementBoxId = 'de7ae05d-08fe-48a0-aed0-d1bdfb82cd90';
const fields = [
  'visible',
  'maintainState',
  'maintainAnimation',
  'maintainSize',
  'maintainSemantics',
  'maintainInteractivity',
];
bool flag(int bits, int index) => bits & (1 << index) != 0;
bool valid(int b) =>
    (!flag(b, 2) || flag(b, 1)) &&
    (!flag(b, 3) || flag(b, 2)) &&
    (!flag(b, 4) || flag(b, 3)) &&
    (!flag(b, 5) || flag(b, 3));
Map<String, Object?> data({
  int? bits,
  bool maintain = false,
  int replacement = 0,
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
  final widget = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  widget['type'] = maintain ? '$type.maintain' : type;
  widget['properties'] = {
    if (bits != null)
      for (var i = 0; i < (maintain ? 1 : fields.length); i++)
        fields[i]: f.boolean(flag(bits, i)),
  };
  if (replacement > 0) {
    (widget['slots'] as Map)['replacementSliver'] = f.single(
      replacement == 1
          ? null
          : f.node(replacementId, 'flutter.widgets.SliverToBoxAdapter', {}, {
              'child': f.single(
                f.node(replacementBoxId, 'flutter.widgets.SizedBox', {
                  'height': f.number(72),
                  'width': f.number(72),
                }),
              ),
            }),
    );
  }
  return raw;
}

void main() {
  test(
    'every boolean dependency combination is strict for both visible states',
    () {
      var accepted = 0;
      for (var bits = 0; bits < 64; bits++) {
        if (valid(bits)) {
          expect(() => f.decode(data(bits: bits)), returnsNormally);
          accepted++;
        } else {
          expect(() => f.decode(data(bits: bits)), throwsFormatException);
        }
      }
      expect(accepted, 14);
      for (final maintain in [false, true]) {
        final raw = data(maintain: maintain);
        final widget = f.findNode(
          raw['root'] as Map<String, Object?>,
          f.fillId,
        );
        for (final name in maintain ? ['visible'] : fields) {
          for (final bad in [
            {'kind': 'null'},
            {'kind': 'string', 'value': 'true'},
            {'kind': 'integer', 'value': 1},
          ]) {
            widget['properties'] = {name: bad};
            expect(() => f.decode(raw), throwsFormatException);
          }
        }
        widget['properties'] = {'maintainFocusability': f.boolean(true)};
        expect(() => f.decode(raw), throwsFormatException);
        if (maintain) {
          widget['properties'] = {'maintainSize': f.boolean(true)};
          expect(() => f.decode(raw), throwsFormatException);
        }
        widget['properties'] = <String, Object?>{};
        final slots = widget['slots'] as Map;
        final main = slots['sliver'];
        slots['sliver'] = f.single(null);
        expect(() => f.decode(raw), throwsFormatException);
        slots.remove('sliver');
        expect(() => f.decode(raw), throwsFormatException);
        slots['sliver'] = main;
        slots['replacementSliver'] = f.single(
          f.node(replacementId, 'flutter.widgets.SizedBox'),
        );
        expect(() => f.decode(raw), throwsFormatException);
        slots['replacementSliver'] = f.single(
          f.node(
            replacementId,
            'flutter.widgets.SliverCrossAxisExpanded',
            {
              'flex': {'kind': 'integer', 'value': 1},
            },
            {
              'sliver': f.single(
                f.node(replacementBoxId, 'flutter.widgets.SliverToBoxAdapter'),
              ),
            },
          ),
        );
        expect(() => f.decode(raw), throwsFormatException);
      }
    },
  );
  for (final maintain in [false, true]) {
    for (final bits in <int?>[
      null,
      for (var b = 0; b < 64; b++)
        if (maintain ? (b == 62 || b == 63) : valid(b)) b,
    ]) {
      for (final replacement in [0, 1, 2]) {
        for (final horizontal in [false, true]) {
          for (final reverse in [false, true]) {
            for (final rtl in [false, true]) {
              testWidgets(
                'native maintain=$maintain bits=$bits replacement=$replacement h=$horizontal r=$reverse rtl=$rtl',
                (tester) async {
                  await f.pump(
                    tester,
                    data(
                      maintain: maintain,
                      bits: bits,
                      replacement: replacement,
                      horizontal: horizontal,
                      reverse: reverse,
                      rtl: rtl,
                    ),
                  );
                  final native = tester.widget<SliverVisibility>(
                    find.byType(SliverVisibility, skipOffstage: false),
                  );
                  final effective = bits ?? (maintain ? 63 : 1);
                  expect(
                    [
                      native.visible,
                      native.maintainState,
                      native.maintainAnimation,
                      native.maintainSize,
                      native.maintainSemantics,
                      native.maintainInteractivity,
                    ],
                    [for (var i = 0; i < 6; i++) flag(effective, i)],
                  );
                  final render = tester.renderObject<RenderSliver>(
                    find.byType(SliverVisibility, skipOffstage: false),
                  );
                  final extent = flag(effective, 0) || flag(effective, 3)
                      ? 48
                      : flag(effective, 1)
                      ? 0
                      : replacement == 2
                      ? 72
                      : 0;
                  expect(render.geometry!.scrollExtent, extent);
                  expect(render.geometry!.paintExtent, extent);
                  expect(
                    find.byKey(
                      const ValueKey('canvas-widget-${o.adapterId}'),
                      skipOffstage: false,
                    ),
                    flag(effective, 0) || flag(effective, 1)
                        ? findsOneWidget
                        : findsNothing,
                  );
                  expect(
                    find.byKey(
                      const ValueKey('canvas-widget-$replacementId'),
                      skipOffstage: false,
                    ),
                    !flag(effective, 0) &&
                            !flag(effective, 1) &&
                            replacement == 2
                        ? findsOneWidget
                        : findsNothing,
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
    'live hidden main and inactive replacement never gain Designer handles or selection even with native interactivity',
    (tester) async {
      final selected = <String>[];
      for (final bits in [63, 62, 63, 14, 15, 0, 1]) {
        final raw = data(bits: bits, replacement: 2);
        final body = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
        body['properties'] = {'height': f.number(0), 'width': f.number(0)};
        await f.pump(tester, raw, selected: selected.add);
        if (!flag(bits, 0)) {
          expect(
            find.byKey(
              const ValueKey('canvas-zero-size-widget-target-${o.adapterId}'),
            ),
            findsNothing,
          );
          expect(
            find.byKey(
              const ValueKey('canvas-zero-size-widget-target-${f.bodyId}'),
            ),
            findsNothing,
          );
        }
        if (flag(bits, 0) || flag(bits, 1)) {
          expect(
            find.byKey(
              const ValueKey('canvas-zero-size-widget-target-$replacementId'),
            ),
            findsNothing,
          );
        }
        expect(tester.takeException(), isNull);
      }
      await f.pump(
        tester,
        data(maintain: true, bits: 62),
        selected: selected.add,
      );
      final body = tester.getRect(
        find.byKey(
          const ValueKey('canvas-widget-${f.bodyId}'),
          skipOffstage: false,
        ),
      );
      selected.clear();
      await tester.tapAt(body.center);
      expect(selected, isNot(contains(f.bodyId)));
      expect(selected, isNot(contains(o.adapterId)));
      expect(tester.takeException(), isNull);
    },
  );
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        testWidgets(
          'source-aware required wrapper and active empty replacement drops h=$horizontal r=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            CanvasMovePreviewResolver? move;
            await f.pump(
              tester,
              data(bits: 0, horizontal: horizontal, reverse: reverse, rtl: rtl),
              drop: (v) => drop = v,
              move: (v) => move = v,
            );
            final surface = tester.getRect(find.byType(CanvasDocumentView));
            final viewport = tester.getRect(find.byType(CustomScrollView));
            final flipped = reverse != (horizontal && rtl);
            final point = horizontal
                ? Offset(
                    flipped ? viewport.right - 5 : viewport.left + 5,
                    viewport.center.dy,
                  )
                : Offset(
                    viewport.center.dx,
                    flipped ? viewport.bottom - 5 : viewport.top + 5,
                  );
            CanvasDropTarget? resolve(String source) => drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'visibility-drop',
                widgetType: source,
                traits: {canvasSliverWidgetTrait},
              ),
            );
            expect(
              resolve('flutter.widgets.SliverToBoxAdapter')?.parentWidgetId,
              f.fillId,
            );
            expect(
              resolve('flutter.widgets.SliverToBoxAdapter')?.slotName,
              'replacementSliver',
            );
            expect(move!(o.adapterId, f.fillId, 'sliver', 0), isNull);
            for (final constructor in [type, '$type.maintain']) {
              // Wrap the visible owner, not the hidden main branch or an empty replacement.
              final target = resolve(constructor);
              expect(target?.parentWidgetId, o.viewportId);
              expect(target?.slotName, 'slivers');
            }
            for (final bits in [1, 2, 14, 62]) {
              await f.pump(
                tester,
                data(
                  bits: bits,
                  horizontal: horizontal,
                  reverse: reverse,
                  rtl: rtl,
                ),
                drop: (v) => drop = v,
              );
              expect(
                resolve('flutter.widgets.SliverToBoxAdapter')?.parentWidgetId,
                isNot(f.fillId),
              );
            }
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  testWidgets(
    'hidden retained nested slots do not leak drop or move geometry',
    (tester) async {
      for (final bits in [2, 6, 14, 62]) {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        final raw = data(bits: bits, replacement: 2);
        final inner = f.findNode(
          raw['root'] as Map<String, Object?>,
          o.adapterId,
        );
        inner['type'] = 'flutter.widgets.SliverOffstage';
        inner['properties'] = {'offstage': f.boolean(false)};
        inner['slots'] = <String, Object?>{};
        await f.pump(tester, raw, drop: (v) => drop = v, move: (v) => move = v);
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final point =
            tester.getRect(find.byType(CustomScrollView)).topLeft +
            const Offset(5, 5);
        final target = drop!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(
            token: 'nested',
            widgetType: 'flutter.widgets.SliverToBoxAdapter',
            traits: {canvasSliverWidgetTrait},
          ),
        );
        expect(target?.parentWidgetId, isNot(o.adapterId));
        expect(move!(replacementId, o.adapterId, 'sliver', 0), isNull);
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets('offscreen empty replacement never exposes stale geometry', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    await f.pump(tester, data(bits: 0, preceding: 700), drop: (v) => drop = v);
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final point =
        tester.getRect(find.byType(CustomScrollView)).topLeft +
        const Offset(5, 5);
    final target = drop!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(
        token: 'offscreen',
        widgetType: 'flutter.widgets.SliverToBoxAdapter',
        traits: {canvasSliverWidgetTrait},
      ),
    );
    expect(target?.parentWidgetId, isNot(f.fillId));
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'maintained-size semantics refreshes on hide/show even when interactivity never changes',
    (tester) async {
      final handle = tester.ensureSemantics();
      try {
        for (final semantics in [false, true]) {
          for (final visible in [true, false, true, false]) {
            final bits = 14 + 32 + (semantics ? 16 : 0) + (visible ? 1 : 0);
            await f.pump(tester, data(bits: bits));
            expect(
              _semanticLabels(
                tester,
              ).any((label) => label.contains('Fill body')),
              visible || semantics,
              reason: 'visible=$visible maintainSemantics=$semantics',
            );
            expect(tester.takeException(), isNull);
          }
        }
      } finally {
        handle.dispose();
      }
    },
  );
  for (final bits in [0, 2, 6, 14, 30, 46, 62]) {
    testWidgets(
      'native state tickers keyboard pointer and semantics retention bits=$bits',
      (tester) async {
        final probe = GlobalKey<_ProbeState>();
        var taps = 0;
        // Cache the native child, as in the pinned SDK characterization for
        // box Visibility. Canvas separately tests rebuilt child semantics.
        final nativeChild = SliverToBoxAdapter(
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () => taps++,
            child: _Probe(key: probe),
          ),
        );
        Widget build(bool visible) => MaterialApp(
          home: CustomScrollView(
            slivers: [
              SliverVisibility(
                visible: visible,
                maintainState: flag(bits, 1),
                maintainAnimation: flag(bits, 2),
                maintainSize: flag(bits, 3),
                maintainSemantics: flag(bits, 4),
                maintainInteractivity: flag(bits, 5),
                sliver: nativeChild,
              ),
            ],
          ),
        );
        await tester.pumpWidget(build(true));
        await tester.pump(const Duration(milliseconds: 20));
        final original = probe.currentState!;
        original.focus.requestFocus();
        await tester.pump();
        await tester.pumpWidget(build(false));
        if (flag(bits, 1)) {
          expect(probe.currentState, same(original));
          final ticks = original.ticks;
          await tester.pump(const Duration(milliseconds: 50));
          await tester.pump(const Duration(milliseconds: 50));
          expect(original.ticks, flag(bits, 2) ? greaterThan(ticks) : ticks);
          expect(original.focus.hasFocus, true);
          await tester.sendKeyEvent(LogicalKeyboardKey.keyA);
          expect(original.keys, 1);
        } else {
          expect(probe.currentState, isNull);
          expect(original.disposed, true);
        }
        await tester.tapAt(const Offset(20, 20));
        expect(taps, flag(bits, 5) ? 1 : 0);
        await tester.pumpWidget(build(true));
        expect(
          probe.currentState,
          flag(bits, 1) ? same(original) : isNot(same(original)),
        );
        await tester.pumpWidget(const SizedBox.shrink());
        expect(tester.takeException(), isNull);
      },
    );
  }
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

class _Probe extends StatefulWidget {
  const _Probe({super.key});
  @override
  State<_Probe> createState() => _ProbeState();
}

class _ProbeState extends State<_Probe> with SingleTickerProviderStateMixin {
  late final AnimationController controller;
  final focus = FocusNode();
  int ticks = 0, keys = 0;
  bool disposed = false;
  @override
  void initState() {
    super.initState();
    controller =
        AnimationController(vsync: this, duration: const Duration(seconds: 1))
          ..addListener(() => ticks++)
          ..repeat();
  }

  @override
  Widget build(BuildContext context) => Focus(
    focusNode: focus,
    onKeyEvent: (_, event) {
      if (event is KeyDownEvent) {
        keys++;
      }
      return KeyEventResult.handled;
    },
    child: const SizedBox(height: 60),
  );
  @override
  void dispose() {
    disposed = true;
    controller.dispose();
    focus.dispose();
    super.dispose();
  }
}
