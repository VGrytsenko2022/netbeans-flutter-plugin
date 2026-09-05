import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _clipId = 'd4b76528-7bc1-497c-b905-a430abf61a02';
const _childId = 'd4b76528-7bc1-497c-b905-a430abf61a03';
const _presence = <String, Object?>{'kind': 'dartObjectReferencePresence'};
const _warningMessage =
    'Custom ClipRSuperellipse preview unavailable. Generated Dart uses the '
    'configured CustomClipper<RSuperellipse>; isolated Canvas does not execute '
    'project or dependency Dart.';

void main() {
  test(
    'ClipRSuperellipse accepts an optional ordinary child, not ParentData',
    () {
      expect(
        canvasDropSlotsForWidgetType('flutter.widgets.ClipRSuperellipse'),
        const [canvasEmptyChildDropSlot],
      );
      expect(
        isCanvasPaletteWrapperWidgetType('flutter.widgets.ClipRSuperellipse'),
        isFalse,
      );
      for (final entry in {
        'flutter.widgets.ClipRSuperellipse': true,
        'flutter.widgets.Text': true,
        'flutter.material.AppBar': true,
        'flutter.widgets.Expanded': false,
        'flutter.widgets.Flexible': false,
        'flutter.widgets.Spacer': false,
      }.entries) {
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: 'flutter.widgets.ClipRSuperellipse',
            slotName: 'child',
            currentChildCount: 0,
            insertionIndex: 0,
            source: CanvasPaletteDragSource(
              token: entry.key,
              widgetType: entry.key,
              traits: canvasWidgetTraitsForType(entry.key),
            ),
          ),
          entry.value,
          reason: entry.key,
        );
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'ClipRSuperellipse uses real default superellipse and all clips on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        for (final clip in <String?, Clip>{
          null: Clip.antiAlias,
          'none': Clip.none,
          'hardEdge': Clip.hardEdge,
          'antiAlias': Clip.antiAlias,
          'antiAliasWithSaveLayer': Clip.antiAliasWithSaveLayer,
        }.entries) {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  child: _sizedChild(),
                  properties: {
                    if (clip.key != null)
                      'clipBehavior': {
                        'kind': 'enum',
                        'type': 'Clip',
                        'value': clip.key,
                      },
                  },
                ),
              ),
              selectedWidgetId: _clipId,
              onSelected: (_) {},
              dropHoverTarget: const CanvasDropTarget(
                parentWidgetId: _clipId,
                slotName: 'child',
                insertionIndex: 0,
                zone: CanvasDropZone(
                  leftMicros: 400000,
                  topMicros: 400000,
                  rightMicros: 600000,
                  bottomMicros: 600000,
                ),
              ),
            ),
          );
          await tester.pump();
          final finder = find
              .descendant(
                of: _widget(),
                matching: find.byType(ClipRSuperellipse),
              )
              .first;
          final widget = tester.widget<ClipRSuperellipse>(finder);
          final render = tester.renderObject<RenderClipRSuperellipse>(finder);
          expect(widget.clipper, isNull);
          expect(render.clipper, isNull);
          expect(widget.borderRadius, BorderRadius.zero);
          expect(render.borderRadius, BorderRadius.zero);
          expect(widget.clipBehavior, clip.value);
          expect(render.clipBehavior, clip.value);
          expect(render.size, const Size(80, 60));
          if (clip.value == Clip.none) {
            expect(render, isNot(paints..clipRSuperellipse()));
          } else {
            expect(
              render,
              paints..clipRSuperellipse(
                rsuperellipse: BorderRadius.zero.toRSuperellipse(
                  const Rect.fromLTWH(0, 0, 80, 60),
                ),
              ),
            );
          }
          expect(render, isNot(paints..clipRRect()));
          expect(
            render.describeApproximatePaintClip(render.child!),
            clip.value == Clip.none ? null : const Rect.fromLTWH(0, 0, 80, 60),
          );
          final result = BoxHitTestResult();
          expect(render.hitTest(result, position: const Offset(1, 1)), isTrue);
          expect(
            find.bySemanticsLabel(RegExp('SizedBox $_childId')),
            findsOneWidget,
          );
          for (final overlay in [
            find.byKey(const ValueKey('canvas-selection-outline-$_clipId')),
            find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
          ]) {
            expect(overlay, findsOneWidget);
            expect(
              find.ancestor(of: overlay, matching: find.byWidget(widget)),
              findsNothing,
            );
          }
          expect(tester.takeException(), isNull);
        }
        semantics.dispose();
      },
    );

    testWidgets(
      'ClipRSuperellipse paints physical, directional, elliptical and oversized radii on $platform',
      (tester) async {
        const physical = BorderRadius.only(
          topLeft: Radius.elliptical(20, 18),
          topRight: Radius.elliptical(12, 10),
          bottomRight: Radius.elliptical(8, 6),
          bottomLeft: Radius.elliptical(4, 2),
        );
        const rtl = BorderRadius.only(
          topLeft: Radius.elliptical(12, 10),
          topRight: Radius.elliptical(20, 18),
          bottomRight: Radius.elliptical(4, 2),
          bottomLeft: Radius.elliptical(8, 6),
        );
        const oversized = BorderRadius.only(
          topLeft: Radius.elliptical(120, 80),
          topRight: Radius.elliptical(160, 100),
          bottomRight: Radius.elliptical(180, 120),
          bottomLeft: Radius.elliptical(140, 90),
        );
        for (final geometry in [
          (radius: _radius(), direction: 'ltr', expected: physical),
          (
            radius: _radius(directional: true),
            direction: 'ltr',
            expected: physical,
          ),
          (radius: _radius(directional: true), direction: 'rtl', expected: rtl),
          (
            radius: _radius(oversized: true),
            direction: 'rtl',
            expected: oversized,
          ),
          (
            radius: <String, Object?>{},
            direction: 'ltr',
            expected: BorderRadius.zero,
          ),
        ]) {
          for (final clip in Clip.values) {
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(
                  _model(
                    platform: platform,
                    direction: geometry.direction,
                    child: _sizedChild(),
                    properties: {
                      if (geometry.radius.isNotEmpty)
                        'borderRadius': geometry.radius,
                      'clipBehavior': {
                        'kind': 'enum',
                        'type': 'Clip',
                        'value': clip.name,
                      },
                    },
                  ),
                ),
                selectedWidgetId: _clipId,
                onSelected: (_) {},
              ),
            );
            await tester.pump();
            final finder = find
                .descendant(
                  of: _widget(),
                  matching: find.byType(ClipRSuperellipse),
                )
                .first;
            final widget = tester.widget<ClipRSuperellipse>(finder);
            final render = tester.renderObject<RenderClipRSuperellipse>(finder);
            expect(widget.borderRadius, geometry.expected);
            expect(render.borderRadius, geometry.expected);
            expect(
              render.textDirection,
              geometry.direction == 'rtl'
                  ? TextDirection.rtl
                  : TextDirection.ltr,
            );
            expect(render.size, const Size(80, 60));
            expect(render.clipBehavior, clip);
            if (clip == Clip.none) {
              expect(render, isNot(paints..clipRSuperellipse()));
            } else {
              expect(
                render,
                paints..clipRSuperellipse(
                  rsuperellipse: geometry.expected.toRSuperellipse(
                    const Rect.fromLTWH(0, 0, 80, 60),
                  ),
                ),
              );
            }
            expect(render, isNot(paints..clipRRect()));
            expect(
              render.hitTest(BoxHitTestResult(), position: const Offset(1, 1)),
              isTrue,
              reason:
                  'Flutter uses bounding-box hit testing without a delegate',
            );
            expect(
              render.hitTest(BoxHitTestResult(), position: const Offset(81, 1)),
              isFalse,
            );
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'ClipRSuperellipse omitted child stays nullable with external DnD on $platform',
      (tester) async {
        CanvasDropResolver? resolver;
        final selections = <String>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(_model(platform: platform, bounded: false)),
            selectedWidgetId: null,
            onSelected: selections.add,
            onDropResolverChanged: (value) => resolver = value,
          ),
        );
        await tester.pump();
        final finder = find
            .descendant(of: _widget(), matching: find.byType(ClipRSuperellipse))
            .first;
        expect(tester.widget<ClipRSuperellipse>(finder).child, isNull);
        expect(
          tester.renderObject<RenderClipRSuperellipse>(finder).child,
          isNull,
        );
        expect(tester.getSize(_widget()), Size.zero);
        final target = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$_clipId'),
        );
        expect(tester.getSize(target), const Size.square(36));
        await tester.tap(target);
        expect(selections.last, _clipId);
        _expectDrop(tester, resolver!, target);
        expect(tester.takeException(), isNull);
      },
    );

    {
      const message = _warningMessage;
      testWidgets(
        'ClipRSuperellipse custom clipper ignores radius, keeps child and explains unavailable preview on $platform',
        (tester) async {
          final semantics = tester.ensureSemantics();
          final selections = <String>[];
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  child: _sizedChild(),
                  properties: {
                    'clipper': _presence,
                    'borderRadius': _radius(oversized: true),
                    'clipBehavior': {
                      'kind': 'enum',
                      'type': 'Clip',
                      'value': 'none',
                    },
                  },
                ),
              ),
              selectedWidgetId: _clipId,
              onSelected: selections.add,
            ),
          );
          await tester.pump();
          final warning = find.byKey(
            const ValueKey('canvas-custom-clipper-preview-$_clipId'),
          );
          expect(tester.getSize(_widget()), const Size(80, 60));
          expect(
            find.descendant(
              of: _widget(),
              matching: find.byType(ClipRSuperellipse),
            ),
            findsNothing,
          );
          expect(warning, findsOneWidget);
          expect(
            find.text('Custom clipper\npreview unavailable'),
            findsOneWidget,
          );
          expect(find.bySemanticsLabel(message), findsOneWidget);
          expect(
            find.bySemanticsLabel(RegExp('SizedBox $_childId')),
            findsOneWidget,
          );
          final mouse = await tester.createGesture(
            kind: PointerDeviceKind.mouse,
          );
          await mouse.addPointer(location: Offset.zero);
          await mouse.moveTo(tester.getCenter(warning));
          await tester.pump(const Duration(milliseconds: 500));
          expect(find.text(message), findsOneWidget);
          await tester.tapAt(tester.getCenter(warning));
          expect(selections.last, _clipId);
          expect(tester.takeException(), isNull);
          await mouse.removePointer();
          await tester.pumpAndSettle();
          semantics.dispose();
        },
      );

      testWidgets(
        'ClipRSuperellipse empty custom clipper preserves zero layout, warning and DnD on $platform',
        (tester) async {
          final semantics = tester.ensureSemantics();
          final selections = <String>[];
          CanvasDropResolver? resolver;
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  bounded: false,
                  properties: {'clipper': _presence},
                ),
              ),
              selectedWidgetId: null,
              onSelected: selections.add,
              onDropResolverChanged: (value) => resolver = value,
            ),
          );
          await tester.pump();
          expect(tester.getSize(_widget()), Size.zero);
          final target = find.byKey(
            const ValueKey('canvas-zero-size-widget-target-$_clipId'),
          );
          final badge = find.byKey(
            const ValueKey('canvas-zero-size-custom-clipper-warning-$_clipId'),
          );
          expect(tester.getSize(target), const Size.square(36));
          expect(tester.getSize(badge), const Size.square(20));
          final externalSemantics = tester.widget<Semantics>(
            find.byKey(
              const ValueKey('canvas-zero-size-widget-semantics-$_clipId'),
            ),
          );
          expect(externalSemantics.properties.label, contains(message));
          expect(
            tester
                .widget<Tooltip>(
                  find.ancestor(of: badge, matching: find.byType(Tooltip)),
                )
                .message,
            message,
          );
          expect(
            find.descendant(
              of: _widget(),
              matching: find.byType(ClipRSuperellipse),
            ),
            findsNothing,
          );
          await tester.tap(target);
          expect(selections.last, _clipId);
          _expectDrop(tester, resolver!, target);
          expect(tester.takeException(), isNull);
          semantics.dispose();
        },
      );
    }
  }
}

Finder _widget() => find.byKey(const ValueKey('canvas-widget-$_clipId'));

void _expectDrop(
  WidgetTester tester,
  CanvasDropResolver resolver,
  Finder target,
) {
  final surface = tester.getRect(find.byType(CanvasDocumentView));
  final point = tester.getCenter(target);
  final drop = resolver(
    ((point.dx - surface.left) / surface.width * 1000000).round(),
    ((point.dy - surface.top) / surface.height * 1000000).round(),
  );
  expect(drop?.parentWidgetId, _clipId);
  expect(drop?.slotName, 'child');
  expect(drop?.insertionIndex, 0);
  expect(drop?.zone?.isEmpty, isFalse);
}

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));

Map<String, Object?> _model({
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
  bool bounded = true,
  String platform = 'windows',
  String direction = 'ltr',
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  final clip = _node(
    _clipId,
    'flutter.widgets.ClipRSuperellipse',
    properties: properties,
    child: child,
  );
  final content = bounded
      ? _node(
          'd4b76528-7bc1-497c-b905-a430abf61a04',
          'flutter.widgets.SizedBox',
          child: clip,
          properties: {
            'width': {'kind': 'integer', 'value': 80},
            'height': {'kind': 'integer', 'value': 60},
          },
        )
      : clip;
  model['root'] = {
    'id': 'd4b76528-7bc1-497c-b905-a430abf61a05',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          'd4b76528-7bc1-497c-b905-a430abf61a06',
          'flutter.widgets.Center',
          child: bounded
              ? _node(
                  'd4b76528-7bc1-497c-b905-a430abf61a07',
                  'flutter.widgets.Directionality',
                  child: content,
                  properties: {
                    'textDirection': {
                      'kind': 'enum',
                      'type': 'TextDirection',
                      'value': direction,
                    },
                  },
                )
              : content,
        ),
      },
    },
  };
  return model;
}

Map<String, Object?> _radius({
  bool directional = false,
  bool oversized = false,
}) => {
  'kind': 'borderRadius',
  'geometry': {
    'kind': directional ? 'directional' : 'physical',
    directional ? 'topStart' : 'topLeft': {
      'x': oversized ? 120.0 : 20.0,
      'y': oversized ? 80.0 : 18.0,
    },
    directional ? 'topEnd' : 'topRight': {
      'x': oversized ? 160.0 : 12.0,
      'y': oversized ? 100.0 : 10.0,
    },
    directional ? 'bottomEnd' : 'bottomRight': {
      'x': oversized ? 180.0 : 8.0,
      'y': oversized ? 120.0 : 6.0,
    },
    directional ? 'bottomStart' : 'bottomLeft': {
      'x': oversized ? 140.0 : 4.0,
      'y': oversized ? 90.0 : 2.0,
    },
  },
};

Map<String, Object?> _node(
  String id,
  String type, {
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
}) => {
  'id': id,
  'type': type,
  'properties': properties,
  'slots': {
    'child': {'kind': 'single', 'child': child},
  },
};

Map<String, Object?> _sizedChild() => _node(
  _childId,
  'flutter.widgets.SizedBox',
  properties: {
    'width': {'kind': 'integer', 'value': 80},
    'height': {'kind': 'integer', 'value': 60},
  },
);
