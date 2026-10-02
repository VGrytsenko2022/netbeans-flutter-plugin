import 'dart:ui' as ui;
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';

Future<Uint8List> raster(ui.Shader shader, Rect bounds) async {
  final recorder = ui.PictureRecorder(), canvas = Canvas(recorder);
  canvas.translate(-bounds.left, -bounds.top);
  canvas.drawRect(bounds, Paint()..shader = shader);
  final picture = recorder.endRecording();
  final image = await picture.toImage(
    bounds.width.toInt(),
    bounds.height.toInt(),
  );
  try {
    return (await image.toByteData(
      format: ui.ImageByteFormat.rawRgba,
    ))!.buffer.asUint8List();
  } finally {
    image.dispose();
    picture.dispose();
  }
}

void main() {
  testWidgets(
    'native callback receives local current bounds and render object is retained',
    (tester) async {
      final seen = <Rect>[];
      Widget host(double width) => MaterialApp(
        home: Center(
          child: SizedBox(
            width: width,
            height: 40,
            child: ShaderMask(
              key: const ValueKey('mask'),
              shaderCallback: (bounds) {
                seen.add(bounds);
                return const LinearGradient(
                  colors: [Colors.white, Colors.transparent],
                ).createShader(bounds);
              },
              child: const ColoredBox(color: Colors.red),
            ),
          ),
        ),
      );
      await tester.pumpWidget(host(80));
      final render = tester.renderObject<RenderShaderMask>(
        find.byKey(const ValueKey('mask')),
      );
      expect(seen.last, const Rect.fromLTWH(0, 0, 80, 40));
      await tester.pumpWidget(host(120));
      expect(seen.last, const Rect.fromLTWH(0, 0, 120, 40));
      expect(
        tester.renderObject(find.byKey(const ValueKey('mask'))),
        same(render),
      );
      expect(render.size, const Size(120, 40));
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets('mask changes child pixels without changing input or semantics', (
    tester,
  ) async {
    int taps = 0;
    final semantics = tester.ensureSemantics();
    await tester.pumpWidget(
      MaterialApp(
        home: Center(
          child: RepaintBoundary(
            key: const ValueKey('pixels'),
            child: SizedBox(
              width: 100,
              height: 40,
              child: ShaderMask(
                shaderCallback: (bounds) => const LinearGradient(
                  colors: [Colors.white, Colors.transparent],
                ).createShader(bounds),
                child: Semantics(
                  label: 'Masked action',
                  button: true,
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () => taps++,
                    child: const ColoredBox(color: Colors.red),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
    final boundary = tester.renderObject<RenderRepaintBoundary>(
      find.byKey(const ValueKey('pixels')),
    );
    final bytes = await tester.runAsync(() async {
      final image = await boundary.toImage(pixelRatio: 1);
      try {
        return (await image.toByteData(
          format: ui.ImageByteFormat.rawRgba,
        ))!.buffer.asUint8List();
      } finally {
        image.dispose();
      }
    });
    expect(
      bytes![(20 * 100 + 5) * 4 + 3],
      greaterThan(bytes[(20 * 100 + 95) * 4 + 3]),
    );
    await tester.tapAt(
      tester.getTopLeft(find.byKey(const ValueKey('pixels'))) +
          const Offset(95, 20),
    );
    expect(taps, 1);
    expect(find.bySemanticsLabel('Masked action'), findsOneWidget);
    semantics.dispose();
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'empty Child does not invoke a callback and needs no compositing',
    (tester) async {
      int calls = 0;
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: ShaderMask(
              shaderCallback: (bounds) {
                calls++;
                return const LinearGradient(
                  colors: [Colors.white, Colors.white],
                ).createShader(bounds);
              },
            ),
          ),
        ),
      );
      final render = tester.renderObject<RenderShaderMask>(
        find.byType(ShaderMask),
      );
      expect(calls, 0);
      expect(render.needsCompositing, isFalse);
      expect(tester.takeException(), isNull);
    },
  );
}
