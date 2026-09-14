import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';

class TrackingAnimation extends AnimationController {
  TrackingAnimation({required super.vsync, super.value});
  final listeners = <VoidCallback>{};
  @override
  void addListener(VoidCallback listener) {
    listeners.add(listener);
    super.addListener(listener);
  }

  @override
  void removeListener(VoidCallback listener) {
    listeners.remove(listener);
    super.removeListener(listener);
  }

  bool get hasListeners => listeners.isNotEmpty;
}

Future<ui.Image> makeImage(WidgetTester tester, Color color) async =>
    (await tester.runAsync(() async {
      final recorder = ui.PictureRecorder();
      Canvas(
        recorder,
      ).drawRect(const Rect.fromLTWH(0, 0, 16, 12), Paint()..color = color);
      final picture = recorder.endRecording();
      try {
        return await picture.toImage(16, 12);
      } finally {
        picture.dispose();
      }
    }))!;
Widget host(Widget child) => MaterialApp(home: Center(child: child));
void nativeTests() {
  testWidgets('decoded image ownership survives replacement and unmount', (
    tester,
  ) async {
    final a = await makeImage(tester, Colors.red),
        b = await makeImage(tester, Colors.blue);
    expect(a.debugGetOpenHandleStackTraces(), hasLength(1));
    await tester.pumpWidget(host(RawImage(image: a, scale: 2)));
    expect(tester.getSize(find.byType(RawImage)), const Size(8, 6));
    expect(a.debugGetOpenHandleStackTraces(), hasLength(2));
    final render = tester.renderObject<RenderImage>(find.byType(RawImage));
    expect(render.image, isNot(same(a)));
    expect(render.image!.isCloneOf(a), isTrue);
    await tester.pumpWidget(host(RawImage(image: b, width: 32, height: 24)));
    expect(a.debugGetOpenHandleStackTraces(), hasLength(1));
    expect(b.debugGetOpenHandleStackTraces(), hasLength(2));
    await tester.pumpWidget(const SizedBox());
    expect(b.debugGetOpenHandleStackTraces(), hasLength(1));
    a.dispose();
    b.dispose();
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'live opacity listener and all painting fields use native renderer',
    (tester) async {
      final image = await makeImage(tester, Colors.red);
      final animation = TrackingAnimation(vsync: tester, value: 0.75);
      await tester.pumpWidget(
        host(
          Directionality(
            textDirection: TextDirection.rtl,
            child: RawImage(
              image: image,
              debugImageLabel: 'not semantics',
              width: 32,
              height: 24,
              scale: 1,
              color: Colors.green,
              opacity: animation,
              colorBlendMode: BlendMode.multiply,
              fit: BoxFit.fill,
              alignment: AlignmentDirectional.topEnd,
              repeat: ImageRepeat.repeatX,
              centerSlice: const Rect.fromLTRB(2, 2, 14, 10),
              matchTextDirection: true,
              invertColors: true,
              filterQuality: FilterQuality.high,
              isAntiAlias: true,
            ),
          ),
        ),
      );
      final r = tester.renderObject<RenderImage>(find.byType(RawImage));
      expect(r.opacity, same(animation));
      expect(r.color, Colors.green);
      expect(r.colorBlendMode, BlendMode.multiply);
      expect(r.fit, BoxFit.fill);
      expect(r.alignment, AlignmentDirectional.topEnd);
      expect(r.repeat, ImageRepeat.repeatX);
      expect(r.centerSlice, const Rect.fromLTRB(2, 2, 14, 10));
      expect(r.textDirection, TextDirection.rtl);
      expect(r.matchTextDirection, isTrue);
      expect(r.invertColors, isTrue);
      expect(r.isAntiAlias, isTrue);
      expect(r.filterQuality, FilterQuality.high);
      expect(animation.hasListeners, isTrue);
      animation.value = 0.25;
      await tester.pump();
      expect(r.opacity!.value, 0.25);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
      expect(animation.hasListeners, isFalse);
      animation.dispose();
      image.dispose();
    },
  );
  testWidgets('unset image is empty and debug label is not semantics', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    await tester.pumpWidget(
      host(
        const RawImage(debugImageLabel: 'debug-only', width: 30, height: 20),
      ),
    );
    expect(tester.getSize(find.byType(RawImage)), const Size(30, 20));
    expect(find.bySemanticsLabel('debug-only'), findsNothing);
    await tester.pumpWidget(host(const RawImage()));
    expect(tester.getSize(find.byType(RawImage)), Size.zero);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    semantics.dispose();
  });
}

void main() => nativeTests();
