import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_color_filtered_test.dart' as c;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const types = [
  'flutter.widgets.BackdropFilter',
  'flutter.widgets.BackdropFilter.grouped',
];
const presence = {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> data(
  String type, [
  Map<String, Object?> props = const {},
  bool empty = false,
]) {
  final raw = c.data({}, empty), n = a.builder(raw);
  n['type'] = type;
  n['properties'] = {
    'filter': {'kind': 'string', 'value': 'blur'},
    ...props,
  };
  return raw;
}

void main() {
  test('exact schemas and native relationship domains', () {
    for (final type in types) {
      final section = canvasReviewedWidgetSchemaContract
          .split('W|$type\n')[1]
          .split('W|')[0];
      expect(
        section.split('\n').where((s) => s.startsWith('P|')),
        hasLength(type.endsWith('.grouped') ? 25 : 26),
      );
      expect(canvasDropSlotsForWidgetType(type), hasLength(1));
      for (final field in [
        'enabled',
        'blendMode',
        'configSigmaX',
        'configSigmaY',
        'configTileMode',
        'configBounded',
        'configInner',
        'configOuter',
      ]) {
        expect(
          () => f.decode(
            data(type, {
              field: {'kind': 'null'},
            }),
          ),
          throwsFormatException,
          reason: field,
        );
      }
      expect(
        () => f.decode(
          data(type, {
            'filter': {'kind': 'null'},
          }),
        ),
        throwsFormatException,
      );
      expect(
        () => f.decode(
          data(type, {
            'filter': {'kind': 'null'},
            'filterConfig': {'kind': 'string', 'value': 'wrap'},
          }),
        ),
        throwsFormatException,
      );
      expect(
        () => f.decode(
          data(type, {
            'filter': {'kind': 'string', 'value': 'shader'},
          }),
        ),
        throwsFormatException,
      );
      expect(
        () => f.decode(
          data(type, {
            'filter': {'kind': 'string', 'value': 'shader'},
            'filterConfig': presence,
          }),
        ),
        returnsNormally,
      );
      for (final mode in ['blur', 'compose']) {
        expect(
          () => f.decode(
            data(type, {
              'filter': {'kind': 'null'},
              'filterConfig': {'kind': 'string', 'value': mode},
            }),
          ),
          returnsNormally,
        );
      }
      if (type.endsWith('.grouped')) {
        expect(
          () => f.decode(data(type, {'backdropGroupKey': presence})),
          throwsFormatException,
        );
      }
    }
    expect(
      canvasReviewedRequiredWrapperSlot('flutter.widgets.BackdropGroup'),
      'child',
    );
    expect(
      canvasExistingChildWrapTargetSlot(
        parentWidgetType: 'flutter.widgets.BackdropGroup',
        slotName: 'child',
      ),
      isNotNull,
    );
  });
  for (final type in types) {
    for (final mode in [
      'blur',
      'dilate',
      'erode',
      'matrix',
      'compose',
      'shader',
    ]) {
      for (final enabled in [true, false]) {
        for (final empty in [true, false]) {
          testWidgets('$type $mode enabled=$enabled empty=$empty', (
            tester,
          ) async {
            await f.pump(
              tester,
              data(type, {
                'filter': {'kind': 'string', 'value': mode},
                'enabled': {'kind': 'boolean', 'value': enabled},
                'sigmaX': f.number(2),
                'sigmaY': f.number(3),
                'radiusX': f.number(1),
                'radiusY': f.number(2),
                if (mode == 'shader') 'shader': presence,
              }, empty),
            );
            final widget = tester.widget<BackdropFilter>(
              find.byType(BackdropFilter),
            );
            final expected = switch (mode) {
              'blur' => ui.ImageFilter.blur(sigmaX: 2, sigmaY: 3),
              'dilate' => ui.ImageFilter.dilate(radiusX: 1, radiusY: 2),
              'erode' => ui.ImageFilter.erode(radiusX: 1, radiusY: 2),
              'matrix' => ui.ImageFilter.matrix(Matrix4.identity().storage),
              'compose' => ui.ImageFilter.compose(
                inner: ui.ImageFilter.blur(),
                outer: ui.ImageFilter.blur(),
              ),
              _ => ui.ImageFilter.blur(),
            };
            expect(widget.filter, expected);
            expect(widget.filterConfig, isNull);
            expect(widget.enabled, enabled);
            expect(
              tester.getSize(find.byType(BackdropFilter)),
              empty ? Size.zero : const Size(48, 32),
            );
            expect(tester.takeException(), isNull);
            await tester.pumpWidget(const SizedBox());
          });
        }
      }
    }
    for (final mode in ['wrap', 'blur', 'compose']) {
      testWidgets('$type config $mode and inactive drafts', (tester) async {
        await f.pump(
          tester,
          data(type, {
            'filterConfig': {'kind': 'string', 'value': mode},
            'filter': {
              'kind': 'string',
              'value': mode == 'wrap' ? 'dilate' : 'shader',
            },
            'shader': presence,
            'configSigmaX': f.number(-2),
            'configSigmaY': f.number(3),
            'configBounded': {'kind': 'boolean', 'value': true},
            'configTileMode': {
              'kind': 'enum',
              'type': 'TileMode',
              'value': 'mirror',
            },
          }),
        );
        final w = tester.widget<BackdropFilter>(find.byType(BackdropFilter));
        final expected = switch (mode) {
          'wrap' => ImageFilterConfig(ui.ImageFilter.dilate()),
          'blur' => const ImageFilterConfig.blur(
            sigmaX: -2,
            sigmaY: 3,
            bounded: true,
            tileMode: TileMode.mirror,
          ),
          _ => const ImageFilterConfig.compose(
            inner: ImageFilterConfig.blur(),
            outer: ImageFilterConfig.blur(),
          ),
        };
        expect(w.filter, isNull);
        expect(w.filterConfig, expected);
        expect(
          tester
              .widgetList<Tooltip>(find.byType(Tooltip))
              .map((w) => w.message ?? '')
              .join(' '),
          isNot(contains('project-owned shader')),
        );
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
      });
    }
    testWidgets('$type full enum and matrix/bounds parity', (tester) async {
      for (final blend in BlendMode.values) {
        await f.pump(
          tester,
          data(type, {
            'blendMode': {
              'kind': 'enum',
              'type': 'BlendMode',
              'value': blend.name,
            },
          }),
        );
        expect(
          tester.widget<BackdropFilter>(find.byType(BackdropFilter)).blendMode,
          blend,
        );
        expect(tester.takeException(), isNull);
      }
      for (final tile in TileMode.values) {
        await f.pump(
          tester,
          data(type, {
            'filterConfig': {'kind': 'string', 'value': 'blur'},
            'configTileMode': {
              'kind': 'enum',
              'type': 'TileMode',
              'value': tile.name,
            },
          }),
        );
        expect(
          tester
              .widget<BackdropFilter>(find.byType(BackdropFilter))
              .filterConfig,
          ImageFilterConfig.blur(tileMode: tile),
        );
      }
      await f.pump(
        tester,
        data(type, {'boundsLeft': f.number(-2), 'boundsWidth': f.number(20)}),
      );
      expect(
        tester.widget<BackdropFilter>(find.byType(BackdropFilter)).filter,
        ui.ImageFilter.blur(bounds: const Rect.fromLTWH(-2, 0, 20, 0)),
      );
      for (final quality in FilterQuality.values) {
        final matrix = Matrix4.identity()..setEntry(0, 3, 4);
        await f.pump(
          tester,
          data(type, {
            'filter': {'kind': 'string', 'value': 'matrix'},
            'matrix4': {'kind': 'matrix4', 'storage': matrix.storage.toList()},
            'filterQuality': {
              'kind': 'enum',
              'type': 'FilterQuality',
              'value': quality.name,
            },
          }),
        );
        expect(
          tester.widget<BackdropFilter>(find.byType(BackdropFilter)).filter,
          ui.ImageFilter.matrix(matrix.storage, filterQuality: quality),
        );
      }
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    });
    testWidgets('$type source fallbacks are active-only and safe', (
      tester,
    ) async {
      for (final field in [
        'filter',
        'filterConfig',
        'bounds',
        'matrix4',
        'inner',
        'outer',
        'shader',
        'configInner',
        'configOuter',
        if (!type.endsWith('.grouped')) 'backdropGroupKey',
      ]) {
        final mode = switch (field) {
          'matrix4' => 'matrix',
          'inner' || 'outer' => 'compose',
          'shader' => 'shader',
          _ => 'blur',
        };
        await f.pump(
          tester,
          data(type, {
            'filter': {'kind': 'string', 'value': mode},
            if (field.startsWith('config'))
              'filterConfig': {'kind': 'string', 'value': 'compose'},
            field: presence,
          }),
        );
        final widget = tester.widget<BackdropFilter>(
          find.byType(BackdropFilter),
        );
        expect(widget.backdropGroupKey, isNull);
        expect(
          tester
              .widgetList<Tooltip>(find.byType(Tooltip))
              .map((w) => w.message ?? '')
              .join(' '),
          contains('project-owned $field'),
        );
        expect(tester.takeException(), isNull);
      }
      await tester.pumpWidget(const SizedBox());
    });
  }
  testWidgets(
    'Canvas group creates a local shared key and grouped render inherits it',
    (tester) async {
      final raw = data(types.last), n = a.builder(raw), slots = n['slots'];
      n['type'] = 'flutter.widgets.BackdropGroup';
      n['properties'] = {'backdropKey': presence};
      n['slots'] = {
        'child': f.single(
          f.node(
            '4e520931-b195-494b-b55c-3a403b943ba1',
            types.last,
            {
              'filter': {'kind': 'string', 'value': 'blur'},
            },
            slots as Map<String, Object?>,
          ),
        ),
      };
      await f.pump(tester, raw);
      final group = tester.widget<BackdropGroup>(find.byType(BackdropGroup));
      expect(
        tester
            .renderObject<RenderBackdropFilter>(find.byType(BackdropFilter))
            .backdropKey,
        same(group.backdropKey),
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
}
