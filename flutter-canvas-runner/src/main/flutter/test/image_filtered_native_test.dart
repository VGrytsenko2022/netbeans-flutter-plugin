import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';

Future<List<int>> pixel(
  WidgetTester tester,
  ui.ImageFilter filter,
  int x,
  int y, {
  bool enabled = true,
}) async {
  final key = GlobalKey();
  await tester.pumpWidget(
    Directionality(
      textDirection: TextDirection.ltr,
      child: Center(
        child: RepaintBoundary(
          key: key,
          child: SizedBox(
            width: 64,
            height: 64,
            child: Center(
              child: ImageFiltered(
                imageFilter: filter,
                enabled: enabled,
                child: const ColoredBox(
                  color: Color(0xFF0000FF),
                  child: SizedBox(width: 16, height: 16),
                ),
              ),
            ),
          ),
        ),
      ),
    ),
  );
  final boundary =
      key.currentContext!.findRenderObject()! as RenderRepaintBoundary;
  final bytes = await tester.runAsync(() async {
    final image = await boundary.toImage();
    try {
      return await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    } finally {
      image.dispose();
    }
  });
  final i = (y * 64 + x) * 4;
  return [for (int n = 0; n < 4; n++) bytes!.getUint8(i + n)];
}

void main() => nativeTests();
void nativeTests() {
  testWidgets('native signed filter parameters are not clamped by Designer', (
    tester,
  ) async {
    for (final filter in [
      ui.ImageFilter.blur(sigmaX: -2, sigmaY: -3),
      ui.ImageFilter.dilate(radiusX: -2, radiusY: -3),
      ui.ImageFilter.erode(radiusX: -2, radiusY: -3),
      ui.ImageFilter.blur(sigmaX: 2, bounds: const Rect.fromLTWH(0, 0, -4, -5)),
    ]) {
      await pixel(tester, filter, 32, 32);
      expect(tester.takeException(), isNull);
    }
    await tester.pumpWidget(const SizedBox());
  });
  testWidgets('pixels blur morphology matrix and Enabled bypass', (
    tester,
  ) async {
    expect((await pixel(tester, ui.ImageFilter.blur(), 23, 32))[3], 0);
    expect(
      (await pixel(
        tester,
        ui.ImageFilter.blur(sigmaX: 2, sigmaY: 2),
        23,
        32,
      ))[3],
      greaterThan(0),
    );
    expect(
      (await pixel(
        tester,
        ui.ImageFilter.blur(sigmaX: 2, sigmaY: 2),
        23,
        32,
        enabled: false,
      ))[3],
      0,
    );
    expect(
      (await pixel(
        tester,
        ui.ImageFilter.dilate(radiusX: 2, radiusY: 2),
        23,
        32,
      ))[3],
      255,
    );
    expect(
      (await pixel(
        tester,
        ui.ImageFilter.erode(radiusX: 2, radiusY: 2),
        24,
        32,
      ))[3],
      0,
    );
    final matrix = Matrix4.translationValues(8, 0, 0);
    final filter = ui.ImageFilter.matrix(
      matrix.storage,
      filterQuality: FilterQuality.none,
    );
    matrix.storage[12] = 0;
    expect((await pixel(tester, filter, 25, 32))[3], 0);
    expect(await pixel(tester, filter, 40, 32), [0, 0, 255, 255]);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
  });
  testWidgets(
    'compose preserves outer after inner including ColorFilter subtype',
    (tester) async {
      const red = ColorFilter.mode(Color(0xFFFF0000), BlendMode.src);
      const green = ColorFilter.mode(Color(0xFF00FF00), BlendMode.src);
      expect(
        await pixel(
          tester,
          ui.ImageFilter.compose(outer: green, inner: red),
          32,
          32,
        ),
        [0, 255, 0, 255],
      );
      expect(
        await pixel(
          tester,
          ui.ImageFilter.compose(outer: red, inner: green),
          32,
          32,
        ),
        [255, 0, 0, 255],
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets(
    'layout taps semantics and render identity survive disabling filter',
    (tester) async {
      int taps = 0;
      final semantics = tester.ensureSemantics();
      Widget view(bool enabled) => Directionality(
        textDirection: TextDirection.rtl,
        child: Center(
          child: ImageFiltered(
            imageFilter: ui.ImageFilter.blur(sigmaX: 3),
            enabled: enabled,
            child: Semantics(
              label: 'Filtered child',
              button: true,
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () {
                  taps++;
                },
                child: const SizedBox(width: 64, height: 32),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(view(true));
      final render = tester.renderObject<RenderObject>(
        find.byType(ImageFiltered),
      );
      expect(tester.getSize(find.byType(ImageFiltered)), const Size(64, 32));
      expect(render.needsCompositing, isTrue);
      expect(find.bySemanticsLabel('Filtered child'), findsOneWidget);
      await tester.tap(find.byType(ImageFiltered));
      expect(taps, 1);
      await tester.pumpWidget(view(false));
      expect(tester.renderObject(find.byType(ImageFiltered)), same(render));
      expect(render.needsCompositing, isFalse);
      expect(tester.getSize(find.byType(ImageFiltered)), const Size(64, 32));
      await tester.tap(find.byType(ImageFiltered));
      expect(taps, 2);
      expect(find.bySemanticsLabel('Filtered child'), findsOneWidget);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
      semantics.dispose();
    },
  );
}
