import 'dart:ui' as ui;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class ControlledStream extends ImageStreamCompleter {
  void completeImage(ImageInfo image) => setImage(image);
}

class ControlledProvider extends ImageProvider<ControlledProvider> {
  ControlledProvider();
  final stream = ControlledStream();
  @override
  Future<ControlledProvider> obtainKey(ImageConfiguration config) =>
      SynchronousFuture(this);
  @override
  ImageStreamCompleter loadImage(
    ControlledProvider key,
    ImageDecoderCallback decode,
  ) => stream;
}

Future<ui.Image> makeImage(WidgetTester tester, Color color) async =>
    (await tester.runAsync(() async {
      final recorder = ui.PictureRecorder();
      Canvas(
        recorder,
      ).drawRect(const Rect.fromLTWH(0, 0, 8, 8), Paint()..color = color);
      final picture = recorder.endRecording();
      try {
        return await picture.toImage(8, 8);
      } finally {
        picture.dispose();
      }
    }))!;
Widget host(Widget child) => MaterialApp(home: Center(child: child));
Finder images() =>
    find.descendant(of: find.byType(FadeInImage), matching: find.byType(Image));
void nativeTests() {
  testWidgets(
    'native pending placeholder then sequential fades and gapless replacement',
    (tester) async {
      final p = ControlledProvider(),
          i = ControlledProvider(),
          next = ControlledProvider();
      final pImage = await makeImage(tester, Colors.red),
          iImage = await makeImage(tester, Colors.blue);
      p.stream.completeImage(ImageInfo(image: pImage));
      Widget view(ImageProvider<Object> image) => host(
        FadeInImage(
          key: const ValueKey('stable'),
          placeholder: p,
          image: image,
          width: 48,
          height: 32,
          fadeOutDuration: const Duration(milliseconds: 100),
          fadeInDuration: const Duration(milliseconds: 200),
          fadeOutCurve: Curves.linear,
          fadeInCurve: Curves.linear,
        ),
      );
      await tester.pumpWidget(view(i));
      final state = tester.state(find.byType(FadeInImage));
      List<Image> current() => tester.widgetList<Image>(images()).toList();
      expect(current(), hasLength(2));
      expect(current().singleWhere((n) => n.image == p).opacity!.value, 1);
      i.stream.completeImage(ImageInfo(image: iImage));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 50));
      expect(
        current().singleWhere((n) => n.image == p).opacity!.value,
        closeTo(.5, .03),
      );
      expect(current().singleWhere((n) => n.image == i).opacity!.value, 0);
      await tester.pump(const Duration(milliseconds: 100));
      expect(
        current().singleWhere((n) => n.image == i).opacity!.value,
        closeTo(.25, .03),
      );
      await tester.pump(const Duration(milliseconds: 151));
      await tester.pump();
      expect(images(), findsOneWidget);
      await tester.pumpWidget(view(next));
      expect(tester.state(find.byType(FadeInImage)), same(state));
      expect(current().single.gaplessPlayback, true);
      expect(current().single.excludeFromSemantics, true);
      expect(
        tester
            .widget<RawImage>(
              find.descendant(
                of: find.byType(FadeInImage),
                matching: find.byType(RawImage),
              ),
            )
            .image,
        isNotNull,
      );
      await tester.pumpWidget(const SizedBox());
      PaintingBinding.instance.imageCache.clear();
      PaintingBinding.instance.imageCache.clearLiveImages();
      expect(p.stream.hasListeners, false);
      expect(i.stream.hasListeners, false);
      expect(next.stream.hasListeners, false);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'synchronous cached target skips placeholder and preserves one image semantic label',
    (tester) async {
      final p = ControlledProvider(), i = ControlledProvider();
      i.stream.completeImage(
        ImageInfo(image: await makeImage(tester, Colors.blue)),
      );
      final handle = tester.ensureSemantics();
      try {
        await tester.pumpWidget(
          host(
            FadeInImage(
              placeholder: p,
              image: i,
              imageSemanticLabel: 'Target',
              width: 32,
              height: 32,
            ),
          ),
        );
        expect(images(), findsOneWidget);
        expect(p.stream.hasListeners, false);
        expect(find.bySemanticsLabel('Target'), findsOneWidget);
        final sem = tester
            .getSemantics(find.bySemanticsLabel('Target'))
            .getSemanticsData();
        expect(sem.flagsCollection.isImage, true);
        expect(sem.hasAction(ui.SemanticsAction.tap), false);
        await tester.pumpWidget(
          host(
            FadeInImage(
              placeholder: p,
              image: i,
              imageSemanticLabel: 'Target',
              excludeFromSemantics: true,
              width: 32,
              height: 32,
            ),
          ),
        );
        expect(find.bySemanticsLabel('Target'), findsNothing);
        await tester.pumpWidget(const SizedBox());
      } finally {
        handle.dispose();
        PaintingBinding.instance.imageCache.clear();
        PaintingBinding.instance.imageCache.clearLiveImages();
      }
    },
  );
  testWidgets('placeholder and target error builders are independent', (
    tester,
  ) async {
    final p = ControlledProvider(), i = ControlledProvider();
    int pCalls = 0, iCalls = 0;
    await tester.pumpWidget(
      host(
        FadeInImage(
          placeholder: p,
          image: i,
          width: 32,
          height: 32,
          placeholderErrorBuilder: (c, e, s) {
            pCalls++;
            return const Text('placeholder failed');
          },
          imageErrorBuilder: (c, e, s) {
            iCalls++;
            return const Text('target failed');
          },
        ),
      ),
    );
    p.stream.reportError(
      context: ErrorDescription('placeholder'),
      exception: StateError('p'),
      silent: true,
    );
    await tester.pump();
    expect(pCalls, greaterThan(0));
    expect(iCalls, 0);
    expect(find.text('placeholder failed'), findsOneWidget);
    i.stream.reportError(
      context: ErrorDescription('target'),
      exception: StateError('i'),
      silent: true,
    );
    await tester.pump();
    expect(iCalls, greaterThan(0));
    expect(find.text('target failed'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    PaintingBinding.instance.imageCache.clear();
    PaintingBinding.instance.imageCache.clearLiveImages();
  });
  testWidgets(
    'named factory composition is representable by the general constructor',
    (tester) async {
      final asset = FadeInImage.assetNetwork(
        placeholder: 'assets/a.png',
        image: 'https://example.invalid/image.png',
        placeholderScale: 2,
        imageScale: 3,
        placeholderCacheWidth: 10,
        imageCacheHeight: 20,
      );
      expect(asset.placeholder, isA<ResizeImage>());
      expect(
        (asset.placeholder as ResizeImage).imageProvider,
        isA<ExactAssetImage>(),
      );
      expect(
        ((asset.placeholder as ResizeImage).imageProvider as ExactAssetImage)
            .scale,
        2,
      );
      expect((asset.image as ResizeImage).imageProvider, isA<NetworkImage>());
      expect(
        ((asset.image as ResizeImage).imageProvider as NetworkImage).scale,
        3,
      );
      final memory = FadeInImage.memoryNetwork(
        placeholder: Uint8List.fromList([1, 2, 3]),
        image: 'https://example.invalid/image.png',
        placeholderScale: 2,
      );
      expect(memory.placeholder, isA<MemoryImage>());
      expect((memory.placeholder as MemoryImage).scale, 2);
    },
  );
}

void main() => nativeTests();
