import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';

void main() => nativeBackdropTests();
void nativeBackdropTests() {
  testWidgets(
    'backdrop filters existing pixels, preserves foreground and clip',
    (tester) async {
      final key = GlobalKey();
      Widget view(bool enabled) => Directionality(
        textDirection: TextDirection.ltr,
        child: Center(
          child: RepaintBoundary(
            key: key,
            child: SizedBox(
              width: 64,
              height: 64,
              child: Stack(
                children: [
                  const Positioned.fill(
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Expanded(child: ColoredBox(color: Color(0xFFFF0000))),
                        Expanded(child: ColoredBox(color: Color(0xFF0000FF))),
                      ],
                    ),
                  ),
                  Positioned(
                    left: 16,
                    top: 16,
                    width: 32,
                    height: 32,
                    child: ClipRect(
                      child: BackdropFilter(
                        filter: ui.ImageFilter.blur(sigmaX: 4, sigmaY: 4),
                        enabled: enabled,
                        child: const Center(
                          child: SizedBox(
                            width: 8,
                            height: 8,
                            child: ColoredBox(color: Color(0xFF00FF00)),
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      );
      Future<List<int>> pixel(int x, int y) async {
        final render =
            key.currentContext!.findRenderObject()! as RenderRepaintBoundary;
        final bytes = await tester.runAsync(() async {
          final image = await render.toImage();
          try {
            return await image.toByteData(format: ui.ImageByteFormat.rawRgba);
          } finally {
            image.dispose();
          }
        });
        final index = (y * 64 + x) * 4;
        return [for (int n = 0; n < 4; n++) bytes!.getUint8(index + n)];
      }

      await tester.pumpWidget(view(true));
      final blurred = await pixel(30, 20);
      expect(blurred[0], inExclusiveRange(0, 255));
      expect(blurred[2], inExclusiveRange(0, 255));
      expect(await pixel(30, 8), [255, 0, 0, 255]);
      expect(await pixel(32, 32), [0, 255, 0, 255]);
      final render = tester.renderObject<RenderBackdropFilter>(
        find.byType(BackdropFilter),
      );
      expect(render.needsCompositing, isTrue);
      await tester.pumpWidget(view(false));
      expect(tester.renderObject(find.byType(BackdropFilter)), same(render));
      expect(render.enabled, isFalse);
      // Pinned RenderBackdropFilter retains compositing when it has a child.
      expect(render.needsCompositing, isTrue);
      expect(await pixel(30, 20), [255, 0, 0, 255]);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets(
    'bounded config resolves current layout bounds and preserves composition order',
    (tester) async {
      for (final bounds in [
        const Rect.fromLTWH(1, 2, 20, 30),
        const Rect.fromLTWH(8, 9, 40, 60),
      ]) {
        final context = ImageFilterContext(bounds: bounds);
        const config = ImageFilterConfig.blur(
          sigmaX: 2,
          sigmaY: 3,
          bounded: true,
          tileMode: TileMode.decal,
        );
        expect(
          config.resolve(context),
          ui.ImageFilter.blur(
            sigmaX: 2,
            sigmaY: 3,
            bounds: bounds,
            tileMode: TileMode.decal,
          ),
        );
        const outer = ImageFilterConfig(
          ColorFilter.mode(Color(0xFF00FF00), BlendMode.src),
        );
        expect(
          const ImageFilterConfig.compose(
            inner: config,
            outer: outer,
          ).resolve(context),
          ui.ImageFilter.compose(
            inner: config.resolve(context),
            outer: outer.resolve(context),
          ),
        );
      }
      final key = GlobalKey();
      Widget view(double width) => Directionality(
        textDirection: TextDirection.ltr,
        child: Center(
          child: SizedBox(
            width: width,
            height: 20,
            child: ClipRect(
              child: BackdropFilter(
                key: key,
                filterConfig: const ImageFilterConfig.blur(
                  sigmaX: 2,
                  bounded: true,
                ),
                child: const SizedBox.expand(),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(view(30));
      final render =
          key.currentContext!.findRenderObject()! as RenderBackdropFilter;
      expect(render.size, const Size(30, 20));
      await tester.pumpWidget(view(60));
      expect(key.currentContext!.findRenderObject(), same(render));
      expect(render.size, const Size(60, 20));
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets(
    'grouped nearest key, explicit key, null and no ancestor match native semantics',
    (tester) async {
      final shared = BackdropKey(),
          nested = BackdropKey(),
          explicit = BackdropKey();
      Widget filter(String id, {bool grouped = true, BackdropKey? key}) =>
          grouped
          ? BackdropFilter.grouped(
              key: ValueKey(id),
              filter: ui.ImageFilter.blur(),
              child: const SizedBox(width: 12, height: 12),
            )
          : BackdropFilter(
              key: ValueKey(id),
              filter: ui.ImageFilter.blur(),
              backdropGroupKey: key,
              child: const SizedBox(width: 12, height: 12),
            );
      await tester.pumpWidget(
        Directionality(
          textDirection: TextDirection.ltr,
          child: Column(
            children: [
              BackdropGroup(
                backdropKey: shared,
                child: Column(
                  children: [
                    filter('a'),
                    filter('b'),
                    filter('explicit', grouped: false, key: explicit),
                    filter('null', grouped: false),
                    BackdropGroup(backdropKey: nested, child: filter('nested')),
                  ],
                ),
              ),
              filter('outside'),
            ],
          ),
        ),
      );
      BackdropKey? keyOf(String id) => tester
          .renderObject<RenderBackdropFilter>(find.byKey(ValueKey(id)))
          .backdropKey;
      expect(keyOf('a'), same(shared));
      expect(keyOf('b'), same(shared));
      expect(keyOf('nested'), same(nested));
      expect(keyOf('explicit'), same(explicit));
      expect(keyOf('null'), isNull);
      expect(keyOf('outside'), isNull);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets('enabled leaves child hit testing semantics and layout intact', (
    tester,
  ) async {
    int taps = 0;
    final semantics = tester.ensureSemantics();
    Widget view(bool enabled) => Directionality(
      textDirection: TextDirection.rtl,
      child: Center(
        child: BackdropFilter(
          filterConfig: const ImageFilterConfig.blur(sigmaX: 2, bounded: true),
          enabled: enabled,
          child: Semantics(
            label: 'Backdrop child',
            button: true,
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {
                taps++;
              },
              child: const SizedBox(width: 40, height: 20),
            ),
          ),
        ),
      ),
    );
    for (final enabled in [true, false]) {
      await tester.pumpWidget(view(enabled));
      expect(tester.getSize(find.byType(BackdropFilter)), const Size(40, 20));
      await tester.tap(find.byType(BackdropFilter));
      expect(find.bySemanticsLabel('Backdrop child'), findsOneWidget);
    }
    expect(taps, 2);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    semantics.dispose();
  });
}
