import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.RawImage';
const source = {'kind': 'dartObjectReferencePresence'};
const sources = [
  'image',
  'width',
  'height',
  'scale',
  'color',
  'opacity',
  'alignment',
  'centerSlice',
];
Map<String, Object?> data([Map<String, Object?> p = const {}]) {
  final raw = a.data(), n = a.builder(raw);
  n['type'] = type;
  n['slots'] = <String, Object?>{};
  n['properties'] = {'width': f.number(48), 'height': f.number(32), ...p};
  return raw;
}

Map<String, Object?> en(String name, String value) => {
  'kind': 'enum',
  'type': name,
  'value': value,
};
void main() {
  test('exact schema and domains', () {
    expect(canvasDropSlotsForWidgetType(type), isEmpty);
    expect(
      canvasReviewedWidgetSchemaContract
          .split('W|$type\n')[1]
          .split('W|')[0]
          .split('\n')
          .where((l) => l.startsWith('P|')),
      hasLength(16),
    );
    for (final field in sources) {
      expect(() => f.decode(data({field: source})), returnsNormally);
    }
    for (final field in [
      'image',
      'width',
      'height',
      'color',
      'opacity',
      'fit',
      'colorBlendMode',
      'debugImageLabel',
      'centerSlice',
    ]) {
      expect(
        () => f.decode(
          data({
            field: {'kind': 'null'},
          }),
        ),
        returnsNormally,
      );
    }
    for (final field in [
      'scale',
      'alignment',
      'repeat',
      'filterQuality',
      'invertColors',
      'isAntiAlias',
      'matchTextDirection',
    ]) {
      expect(
        () => f.decode(
          data({
            field: {'kind': 'null'},
          }),
        ),
        throwsFormatException,
      );
    }
    for (final value in [-1, 0]) {
      expect(
        () => f.decode(data({'scale': f.number(value.toDouble())})),
        throwsFormatException,
      );
    }
    for (final value in [-0.1, 1.1]) {
      expect(
        () => f.decode(data({'opacity': f.number(value)})),
        throwsFormatException,
      );
    }
  });
  testWidgets(
    'native all16 fields and stable no-image creation including zero size',
    (tester) async {
      for (final extent in [0.0, 48.0]) {
        await f.pump(
          tester,
          data({
            'width': f.number(extent),
            'scale': f.number(2),
            'debugImageLabel': {'kind': 'string', 'value': 'debug only'},
            'opacity': f.number(0.25),
            'color': {'kind': 'color', 'argb': '0xFF123456'},
            'colorBlendMode': en('BlendMode', 'multiply'),
            'fit': en('BoxFit', 'contain'),
            'alignment': {
              'kind': 'alignmentGeometry',
              'basis': 'directional',
              'horizontal': 1,
              'vertical': -1,
            },
            'repeat': en('ImageRepeat', 'repeatX'),
            'filterQuality': en('FilterQuality', 'high'),
            'matchTextDirection': {'kind': 'boolean', 'value': true},
            'invertColors': {'kind': 'boolean', 'value': true},
            'isAntiAlias': {'kind': 'boolean', 'value': true},
          }),
        );
        final n = tester.widget<RawImage>(find.byType(RawImage));
        expect(n.image, isNull);
        expect(n.centerSlice, isNull);
        expect(n.width, extent);
        expect(n.height, 32);
        expect(n.scale, 2);
        expect(n.opacity!.value, 0.25);
        expect(n.debugImageLabel, 'debug only');
        expect(n.color, const Color(0xFF123456));
        expect(n.colorBlendMode, BlendMode.multiply);
        expect(n.fit, BoxFit.contain);
        expect(n.alignment, AlignmentDirectional.topEnd);
        expect(n.repeat, ImageRepeat.repeatX);
        expect(n.matchTextDirection, isTrue);
        expect(n.invertColors, isTrue);
        expect(n.isAntiAlias, isTrue);
        expect(n.filterQuality, FilterQuality.high);
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
  testWidgets('all source-owned inputs have honest null/default preview', (
    tester,
  ) async {
    await f.pump(tester, data({for (final name in sources) name: source}));
    final n = tester.widget<RawImage>(find.byType(RawImage));
    expect(n.image, isNull);
    expect(n.opacity, isNull);
    expect(n.centerSlice, isNull);
    expect(n.color, isNull);
    expect(n.width, isNull);
    expect(n.height, isNull);
    expect(n.scale, 1);
    expect(n.alignment, Alignment.center);
    final text = tester
        .widgetList<Tooltip>(find.byType(Tooltip))
        .map((v) => v.message ?? '')
        .join(' ');
    for (final name in sources) {
      expect(text, contains(name));
    }
    expect(text, contains('not executed'));
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
  });
}
