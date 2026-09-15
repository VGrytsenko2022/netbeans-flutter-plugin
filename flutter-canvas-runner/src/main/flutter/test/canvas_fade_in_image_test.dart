import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';
import 'fade_in_image_native_test.dart' as native;
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.FadeInImage';
const source = {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> asset(String name) => {
  'kind': 'imageProvider',
  'value': {
    'kind': 'asset',
    'assetName': name,
    'packageName': null,
    'exactScale': null,
    'resize': null,
    'resolution': {
      'kind': 'unavailable',
      'code': 'missing',
      'reason': 'Test asset is not declared',
    },
  },
};
Map<String, Object?> data([Map<String, Object?> p = const {}]) {
  final raw = a.data();
  final n = a.builder(raw);
  n['type'] = type;
  n['slots'] = <String, Object?>{};
  n['properties'] = {
    'placeholder': asset('assets/placeholder.png'),
    'image': asset('assets/target.png'),
    'width': f.number(48),
    'height': f.number(32),
    ...p,
  };
  return raw;
}

void main() {
  for (final name in canvasExpansionCurvePresets) {
    testWidgets('both native curves are preserved: $name', (tester) async {
      final raw = data({
        'fadeOutCurve': {'kind': 'string', 'value': name},
        'fadeInCurve': {'kind': 'string', 'value': name},
      });
      await f.pump(tester, raw);
      final configured = tester.widget<FadeInImage>(find.byType(FadeInImage));
      final p = native.ControlledProvider(), i = native.ControlledProvider();
      p.stream.completeImage(
        ImageInfo(image: await native.makeImage(tester, Colors.red)),
      );
      await tester.pumpWidget(
        native.host(
          FadeInImage(
            placeholder: p,
            image: i,
            width: 32,
            height: 32,
            fadeOutDuration: const Duration(milliseconds: 100),
            fadeInDuration: const Duration(milliseconds: 100),
            fadeOutCurve: configured.fadeOutCurve,
            fadeInCurve: configured.fadeInCurve,
          ),
        ),
      );
      i.stream.completeImage(
        ImageInfo(image: await native.makeImage(tester, Colors.blue)),
      );
      await tester.pump();
      for (var tick = 0; tick < 11; tick++) {
        await tester.pump(const Duration(milliseconds: 20));
        expect(tester.takeException(), isNull);
      }
      await tester.pumpWidget(const SizedBox());
      PaintingBinding.instance.imageCache.clear();
      PaintingBinding.instance.imageCache.clearLiveImages();
    });
  }
  testWidgets('both resource IDs collected and each resize binds its own content', (
    tester,
  ) async {
    final bytes = base64Decode(
      'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=',
    );
    final bytes2 = base64Decode(
      'iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAYAAACp8Z5+AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAASSURBVBhXY5DrePofGTOQLgAATc8oocRj8pEAAAAASUVORK5CYII=',
    );
    final ids = [sha256Hex(bytes), sha256Hex(bytes2)];
    final raw = data();
    var props = a.builder(raw)['properties'] as Map;
    for (final pair in ['placeholder', 'image'].indexed) {
      final v = (props[pair.$2] as Map)['value'] as Map;
      v['resolution'] = {
        'kind': 'resolved',
        'resourceId': ids[pair.$1],
        'resolvedScale': pair.$1 + 1,
      };
      v['resize'] = {
        'width': pair.$1 + 2,
        'height': null,
        'policy': pair.$1 == 0 ? 'fit' : 'exact',
        'allowUpscaling': pair.$1 == 1,
      };
    }
    final model = f.decode(raw);
    expect(model.imageResourceIds, ids.toSet());
    final resources = CanvasImageResourceBundle.fromResources([
      for (final pair in [bytes, bytes2].indexed)
        CanvasImageResource(
          resourceId: ids[pair.$1],
          mediaType: 'image/png',
          pixelWidth: pair.$1 == 0 ? 8 : 4,
          pixelHeight: pair.$1 == 0 ? 8 : 4,
          encodedBytes: pair.$2,
        ),
    ]);
    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        imageResources: resources,
        selectedWidgetId: null,
        onSelected: (_) {},
      ),
    );
    await tester.pump();
    final n = tester.widget<FadeInImage>(find.byType(FadeInImage));
    final providers = [n.placeholder as ResizeImage, n.image as ResizeImage];
    expect(providers[0].width, 2);
    expect(providers[1].width, 3);
    expect(providers[0].policy, ResizeImagePolicy.fit);
    expect(providers[1].allowUpscaling, true);
    expect((providers[0].imageProvider as MemoryImage).bytes, bytes);
    expect((providers[1].imageProvider as MemoryImage).bytes, bytes2);
    expect((providers[0].imageProvider as MemoryImage).scale, 1);
    expect((providers[1].imageProvider as MemoryImage).scale, 2);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    PaintingBinding.instance.imageCache.clear();
    PaintingBinding.instance.imageCache.clearLiveImages();
  });

  test(
    'all23 properties and exact Java Canvas contract remain synchronized',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final block = contract.split('W|$type\n').last.split('W|').first;
      expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(23));
      expect(block, contains('ImageProvider<Object>'));
      expect(block, contains('ImageErrorWidgetBuilder?'));
      expect(canvasModelProtocolVersion, 20);
      for (final name in ['placeholder', 'image']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map).remove(name);
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final name in ['fadeInDurationUs', 'fadeOutDurationUs']) {
        for (final us in [-1, 0, 1, 999]) {
          expect(
            () => f.decode(
              data({
                name: {'kind': 'integer', 'value': us},
              }),
            ),
            throwsFormatException,
          );
        }
      }
      for (final name in [
        'placeholder',
        'image',
        'alignment',
        'fadeOutCurve',
        'fadeInCurve',
        'filterQuality',
        'repeat',
      ]) {
        expect(
          () => f.decode(
            data({
              name: {'kind': 'null'},
            }),
          ),
          throwsFormatException,
        );
      }
    },
  );
  testWidgets(
    'all native fields honor independent settings and selectable zero bounds',
    (tester) async {
      for (final zero in [false, true]) {
        await f.pump(
          tester,
          data({
            'width': f.number(zero ? 0 : 48),
            'height': f.number(zero ? 0 : 32),
            'placeholderColor': {'kind': 'color', 'argb': '0xFF112233'},
            'color': {'kind': 'color', 'argb': '0xFF445566'},
            'placeholderColorBlendMode': {
              'kind': 'enum',
              'type': 'BlendMode',
              'value': 'multiply',
            },
            'colorBlendMode': {
              'kind': 'enum',
              'type': 'BlendMode',
              'value': 'srcIn',
            },
            'placeholderFit': {
              'kind': 'enum',
              'type': 'BoxFit',
              'value': 'contain',
            },
            'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'cover'},
            'placeholderFilterQuality': {
              'kind': 'enum',
              'type': 'FilterQuality',
              'value': 'low',
            },
            'filterQuality': {
              'kind': 'enum',
              'type': 'FilterQuality',
              'value': 'high',
            },
            'repeat': {
              'kind': 'enum',
              'type': 'ImageRepeat',
              'value': 'repeatX',
            },
            'matchTextDirection': f.boolean(true),
            'fadeOutDurationUs': {'kind': 'integer', 'value': 1000},
            'fadeInDurationUs': {'kind': 'integer', 'value': 2000},
            'fadeOutCurve': {'kind': 'string', 'value': 'linear'},
            'fadeInCurve': {'kind': 'string', 'value': 'easeIn'},
            'imageSemanticLabel': {'kind': 'string', 'value': 'Example'},
            'excludeFromSemantics': f.boolean(true),
          }),
        );
        final n = tester.widget<FadeInImage>(find.byType(FadeInImage));
        expect(n.width, zero ? 0 : 48);
        expect(n.height, zero ? 0 : 32);
        expect(n.placeholder, isA<MemoryImage>());
        expect(n.image, isA<MemoryImage>());
        expect(n.placeholderColor, const Color(0xFF112233));
        expect(n.color, const Color(0xFF445566));
        expect(n.placeholderColorBlendMode, BlendMode.multiply);
        expect(n.colorBlendMode, BlendMode.srcIn);
        expect(n.placeholderFit, BoxFit.contain);
        expect(n.fit, BoxFit.cover);
        expect(n.filterQuality, FilterQuality.high);
        expect(n.placeholderFilterQuality, FilterQuality.low);
        expect(n.repeat, ImageRepeat.repeatX);
        expect(n.matchTextDirection, true);
        expect(n.excludeFromSemantics, true);
        expect(n.imageSemanticLabel, 'Example');
        expect(n.fadeOutDuration, const Duration(milliseconds: 1));
        expect(n.fadeInDuration, const Duration(milliseconds: 2));
        expect(n.fadeOutCurve, Curves.linear);
        expect(n.fadeInCurve, Curves.easeIn);
        expect(
          find.byKey(const ValueKey('canvas-widget-${a.builderId}')),
          findsOneWidget,
        );
        expect(find.text('Sibling'), findsOneWidget);
        expect(tester.takeException(), isNull);
      }
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets(
    'all13 source fields receive explicit safe fallbacks without project execution',
    (tester) async {
      final p = {
        for (final name in [
          'placeholder',
          'image',
          'placeholderErrorBuilder',
          'imageErrorBuilder',
          'fadeOutDurationUs',
          'fadeInDurationUs',
          'fadeOutCurve',
          'fadeInCurve',
          'color',
          'placeholderColor',
          'width',
          'height',
          'alignment',
        ])
          name: source,
      };
      await f.pump(tester, data(p));
      final n = tester.widget<FadeInImage>(find.byType(FadeInImage));
      expect(n.placeholder, isA<MemoryImage>());
      expect(n.image, isA<MemoryImage>());
      expect(n.placeholderErrorBuilder, isNotNull);
      expect(n.imageErrorBuilder, isNotNull);
      expect(n.fadeOutDuration, const Duration(milliseconds: 300));
      expect(n.fadeInDuration, const Duration(milliseconds: 700));
      expect(n.fadeOutCurve, Curves.easeOut);
      expect(n.fadeInCurve, Curves.easeIn);
      expect(n.width, isNull);
      expect(n.height, isNull);
      expect(n.color, isNull);
      expect(n.placeholderColor, isNull);
      expect(n.alignment, Alignment.center);
      final messages = tester
          .widgetList<Tooltip>(find.byType(Tooltip))
          .map((s) => s.message ?? '')
          .join(' ');
      expect(messages, contains('project-owned'));
      expect(messages, contains('placeholder = built-in image'));
      expect(messages, contains('imageErrorBuilder = safe error placeholder'));
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
}
