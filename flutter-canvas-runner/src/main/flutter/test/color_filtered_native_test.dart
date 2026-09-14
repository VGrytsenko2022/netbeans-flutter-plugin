import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';

const identity = ColorFilter.matrix(<double>[
  1,
  0,
  0,
  0,
  0,
  0,
  1,
  0,
  0,
  0,
  0,
  0,
  1,
  0,
  0,
  0,
  0,
  0,
  1,
  0,
]);
Future<List<int>> pixel(
  WidgetTester tester,
  ColorFilter filter,
  Color color,
) async {
  final key = GlobalKey();
  await tester.pumpWidget(
    Directionality(
      textDirection: TextDirection.ltr,
      child: Center(
        child: RepaintBoundary(
          key: key,
          child: ColorFiltered(
            colorFilter: filter,
            child: ColoredBox(
              color: color,
              child: const SizedBox(width: 16, height: 16),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.pump();
  final boundary =
      key.currentContext!.findRenderObject()! as RenderRepaintBoundary;
  final rgba = (await tester.runAsync(() async {
    final image = await boundary.toImage(pixelRatio: 1);
    try {
      return (await image.toByteData(
        format: ui.ImageByteFormat.rawRgba,
      ))!.buffer.asUint8List().sublist((8 * 16 + 8) * 4, (8 * 16 + 8) * 4 + 4);
    } finally {
      image.dispose();
    }
  }))!;
  return rgba;
}

void nativeTests() {
  testWidgets('native pixel output for identity mode matrix gamma saturation', (
    tester,
  ) async {
    expect(await pixel(tester, identity, const Color(0xFF0000FF)), [
      0,
      0,
      255,
      255,
    ]);
    expect(
      await pixel(
        tester,
        const ColorFilter.mode(Color(0xFFFF0000), BlendMode.src),
        const Color(0xFF0000FF),
      ),
      [255, 0, 0, 255],
    );
    expect(
      await pixel(
        tester,
        const ColorFilter.matrix([
          0,
          0,
          1,
          0,
          0,
          0,
          1,
          0,
          0,
          0,
          1,
          0,
          0,
          0,
          0,
          0,
          0,
          0,
          1,
          0,
        ]),
        const Color(0xFF0000FF),
      ),
      [255, 0, 0, 255],
    );
    final gray = await pixel(
      tester,
      ColorFilter.saturation(0),
      const Color(0xFF0000FF),
    );
    expect(gray[0], closeTo(18, 1));
    expect(gray[0], gray[1]);
    expect(gray[1], gray[2]);
    expect(gray[3], 255);
    final linear = await pixel(
      tester,
      const ColorFilter.srgbToLinearGamma(),
      const Color(0xFF808080),
    );
    final srgb = await pixel(
      tester,
      const ColorFilter.linearToSrgbGamma(),
      const Color(0xFF808080),
    );
    expect(linear[0], closeTo(55, 1));
    expect(srgb[0], closeTo(188, 1));
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
  });
  testWidgets('layout hit testing semantics and layer reuse stay native', (
    tester,
  ) async {
    int taps = 0;
    final semantics = tester.ensureSemantics();
    Widget view(ColorFilter filter) => Directionality(
      textDirection: TextDirection.rtl,
      child: Center(
        child: ColorFiltered(
          key: const ValueKey('filter'),
          colorFilter: filter,
          child: Semantics(
            label: 'Filtered child',
            button: true,
            child: GestureDetector(
              onTap: () {
                taps++;
              },
              child: const ColoredBox(
                color: Colors.blue,
                child: SizedBox(width: 64, height: 32),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpWidget(view(identity));
    final render = tester.renderObject<RenderObject>(
      find.byType(ColorFiltered),
    );
    expect(tester.getSize(find.byType(ColorFiltered)), const Size(64, 32));
    expect(find.bySemanticsLabel('Filtered child'), findsOneWidget);
    await tester.tap(find.byType(ColorFiltered));
    expect(taps, 1);
    await tester.pumpWidget(
      view(const ColorFilter.mode(Colors.transparent, BlendMode.clear)),
    );
    expect(tester.renderObject(find.byType(ColorFiltered)), same(render));
    expect(tester.getSize(find.byType(ColorFiltered)), const Size(64, 32));
    await tester.tap(find.byType(ColorFiltered));
    expect(taps, 2);
    expect(find.bySemanticsLabel('Filtered child'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    semantics.dispose();
  });
  testWidgets('empty child remains legal and zero size', (tester) async {
    await tester.pumpWidget(
      const Directionality(
        textDirection: TextDirection.ltr,
        child: Center(child: ColorFiltered(colorFilter: identity)),
      ),
    );
    expect(tester.getSize(find.byType(ColorFiltered)), Size.zero);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
  });
}

void main() => nativeTests();
