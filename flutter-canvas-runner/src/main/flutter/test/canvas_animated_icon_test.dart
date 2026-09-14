import 'dart:ui' as ui;
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.material.AnimatedIcon';
const icons = {
  'add_event': AnimatedIcons.add_event,
  'arrow_menu': AnimatedIcons.arrow_menu,
  'close_menu': AnimatedIcons.close_menu,
  'ellipsis_search': AnimatedIcons.ellipsis_search,
  'event_add': AnimatedIcons.event_add,
  'home_menu': AnimatedIcons.home_menu,
  'list_view': AnimatedIcons.list_view,
  'menu_arrow': AnimatedIcons.menu_arrow,
  'menu_close': AnimatedIcons.menu_close,
  'menu_home': AnimatedIcons.menu_home,
  'pause_play': AnimatedIcons.pause_play,
  'play_pause': AnimatedIcons.play_pause,
  'search_ellipsis': AnimatedIcons.search_ellipsis,
  'view_list': AnimatedIcons.view_list,
};
Finder native() =>
    find.byKey(const ValueKey('canvas-animated-icon-${a.builderId}'));
Map<String, Object?> data({
  String icon = 'menu_close',
  double progress = 0,
  double? size = 24,
  bool rtl = false,
  String? direction,
}) {
  final raw = a.data();
  final n = a.builder(raw);
  n['type'] = type;
  n['slots'] = <String, Object?>{};
  n['properties'] = {
    'icon': {'kind': 'string', 'value': icon},
    'progress': f.number(progress),
    'color': {'kind': 'color', 'argb': '0xFF224488'},
    'size': size == null ? {'kind': 'null'} : f.number(size),
    'semanticLabel': {'kind': 'string', 'value': 'Animate menu'},
    if (direction != null)
      'textDirection': direction == 'null'
          ? {'kind': 'null'}
          : {'kind': 'enum', 'type': 'TextDirection', 'value': direction},
  };
  raw['root'] = f.node(
    'ce156af3-65da-4f78-82a9-42f796c77726',
    'flutter.widgets.Directionality',
    {
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': rtl ? 'rtl' : 'ltr',
      },
    },
    {'child': f.single(raw['root'] as Map<String, Object?>)},
  );
  return raw;
}

Future<Uint8List> pixels(
  WidgetTester tester,
  CustomPainter painter,
  Size size,
) async {
  return (await tester.runAsync(() async {
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);
    painter.paint(canvas, size);
    final picture = recorder.endRecording();
    final image = await picture.toImage(64, 64);
    final bytes = (await image.toByteData(
      format: ui.ImageByteFormat.rawRgba,
    ))!.buffer.asUint8List();
    image.dispose();
    picture.dispose();
    return bytes;
  }))!;
}

void main() {
  test('exact domains and two required properties', () {
    expect(canvasDropSlotsForWidgetType(type), isEmpty);
    expect(isCanvasPaletteWrapperWidgetType(type), false);
    for (final field in ['icon', 'progress']) {
      final raw = data();
      (a.builder(raw)['properties'] as Map).remove(field);
      expect(() => f.decode(raw), throwsFormatException);
    }
    for (final entry in {
      'icon': {'kind': 'string', 'value': 'custom'},
      'progress': {'kind': 'null'},
      'size': {'kind': 'double', 'value': 'Infinity'},
      'textDirection': {'kind': 'enum', 'type': 'TextDirection', 'value': 'up'},
    }.entries) {
      final raw = data();
      (a.builder(raw)['properties'] as Map)[entry.key] = entry.value;
      expect(() => f.decode(raw), throwsFormatException);
    }
  });
  for (final entry in icons.entries) {
    for (final progress in [-2.0, 0.0, .5, 1.0, 2.0]) {
      for (final rtl in [false, true]) {
        testWidgets('native ${entry.key} progress=$progress rtl=$rtl', (
          tester,
        ) async {
          final size = progress < 0
              ? -12.0
              : progress == 0
              ? 0.0
              : progress == .5
              ? 24.0
              : progress == 1
              ? 48.0
              : null;
          await f.pump(
            tester,
            data(icon: entry.key, progress: progress, size: size, rtl: rtl),
          );
          final n = tester.widget<AnimatedIcon>(native());
          expect(n.icon, same(entry.value));
          expect(n.progress.value, progress);
          expect(n.size, size);
          expect(n.semanticLabel, 'Animate menu');
          expect(n.textDirection, isNull);
          final painter = tester
              .widget<CustomPaint>(
                find
                    .descendant(
                      of: native(),
                      matching: find.byType(CustomPaint),
                    )
                    .first,
              )
              .painter!;
          final actual = await pixels(
            tester,
            painter,
            tester.getSize(native()),
          );
          await tester.pumpWidget(
            MaterialApp(
              home: Directionality(
                textDirection: rtl ? TextDirection.rtl : TextDirection.ltr,
                child: Center(
                  child: AnimatedIcon(
                    icon: entry.value,
                    progress: AlwaysStoppedAnimation(progress),
                    size: size,
                    color: const Color(0xff224488),
                    semanticLabel: 'Animate menu',
                  ),
                ),
              ),
            ),
          );
          final direct = find.byType(AnimatedIcon);
          final expected = tester
              .widget<CustomPaint>(
                find
                    .descendant(of: direct, matching: find.byType(CustomPaint))
                    .first,
              )
              .painter!;
          expect(
            actual,
            await pixels(tester, expected, tester.getSize(direct)),
          );
          expect(tester.takeException(), isNull);
        });
      }
    }
  }
  testWidgets(
    'source fallbacks identify only substituted fields and retain element',
    (tester) async {
      await f.pump(tester, data(progress: .5));
      final before = tester.element(native());
      for (final field in ['icon', 'progress', 'color', 'size']) {
        final raw = data(progress: .5);
        (a.builder(raw)['properties'] as Map)[field] = {
          'kind': 'dartObjectReferencePresence',
        };
        await f.pump(tester, raw);
        final n = tester.widget<AnimatedIcon>(native());
        expect(n.icon, same(AnimatedIcons.menu_close));
        expect(n.progress.value, field == 'progress' ? 0 : .5);
        expect(n.color, field == 'color' ? null : const Color(0xff224488));
        expect(n.size, field == 'size' ? null : 24);
        expect(identical(before, tester.element(native())), true);
        expect(
          find.byWidgetPredicate(
            (w) =>
                w is Tooltip &&
                w.message?.contains('project-owned $field') == true,
          ),
          findsWidgets,
        );
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'zero clear icon is selectable and explicit direction is retained',
    (tester) async {
      for (final dir in ['ltr', 'rtl', 'null']) {
        final raw = data(size: 0, direction: dir, rtl: true);
        (a.builder(raw)['properties'] as Map)['color'] = {
          'kind': 'color',
          'argb': '0x00000000',
        };
        final selected = <String>[];
        await f.pump(tester, raw, selected: selected.add);
        final handle = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-${a.builderId}'),
        );
        expect(handle, findsOneWidget);
        await tester.tap(handle);
        expect(selected, contains(a.builderId));
        expect(
          tester.widget<AnimatedIcon>(native()).textDirection,
          dir == 'null'
              ? null
              : dir == 'rtl'
              ? TextDirection.rtl
              : TextDirection.ltr,
        );
        expect(
          find.byKey(const ValueKey('canvas-widget-${a.builderId}')),
          findsOneWidget,
        );
        expect(tester.takeException(), isNull);
      }
    },
  );
}
