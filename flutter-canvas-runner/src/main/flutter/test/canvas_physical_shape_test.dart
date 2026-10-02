import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _physicalId = 'ff1576da-3451-4137-b030-32ba38ddf98f';
const _childId = '7a76d1c2-99de-4685-b7c3-0503b4a89d05';
const _bounds = Rect.fromLTWH(0, 0, 80, 60);
const _shapes = [
  'roundedRectangle',
  'beveledRectangle',
  'continuousRectangle',
  'roundedSuperellipse',
  'circle',
  'stadium',
];
const _radiusValue = BorderRadius.only(
  topLeft: Radius.elliptical(20, 18),
  topRight: Radius.elliptical(12, 10),
  bottomRight: Radius.elliptical(8, 6),
  bottomLeft: Radius.elliptical(4, 2),
);
const _directionalValue = BorderRadiusDirectional.only(
  topStart: Radius.elliptical(20, 18),
  topEnd: Radius.elliptical(12, 10),
  bottomEnd: Radius.elliptical(8, 6),
  bottomStart: Radius.elliptical(4, 2),
);
const _presence = {'kind': 'dartObjectReferencePresence'};
const _warning =
    'Custom PhysicalShape preview unavailable. Generated Dart uses the '
    'configured CustomClipper<Path>; isolated Canvas does not execute project '
    'or dependency Dart.';

void main() {
  test(
    'PhysicalShape decodes all five properties, six shapes and retained geometry',
    () {
      for (final shape in _shapes) {
        for (final clip in Clip.values) {
          for (final directional in [false, true]) {
            final input = {
              'clipper': _clipper(
                shape: shape,
                directional: directional,
                direction: 'rtl',
              ),
              'clipBehavior': _enum('Clip', clip.name),
              'elevation': {'kind': 'double', 'value': 2.5},
              'color': {'kind': 'color', 'argb': '0x80123456'},
              'shadowColor': _token('material.colorScheme.shadow'),
            };
            final before = jsonEncode(input);
            final node = _find(_decode(_model(properties: input)).root)!;
            expect(node.properties, hasLength(5));
            final value =
                node.properties['clipper']!.value
                    as CanvasShapeBorderClipperValue;
            expect(value.shape, shape);
            expect(value.textDirection, 'rtl');
            expect(
              value.borderRadius is CanvasDirectionalBorderRadiusValue,
              directional,
            );
            expect(value.borderRadius.radii.first.x, 20);
            expect(value.borderRadius.radii.first.y, 18);
            expect(node.properties['elevation']!.value, 2.5);
            expect(jsonEncode(input), before);
          }
        }
      }
      for (final token in canvasColorSchemeThemeTokens) {
        expect(
          () => _decode(
            _model(
              properties: {
                'color': _token(token),
                'shadowColor': _token(token),
              },
            ),
          ),
          returnsNormally,
        );
      }
      final custom = _find(
        _decode(_model(properties: {'clipper': _presence})).root,
      )!;
      expect(custom.properties['clipper']!.kind, 'dartObjectReferencePresence');
      expect(custom.properties['clipper']!.value, isTrue);
    },
  );

  test(
    'PhysicalShape exact closed contract includes required typed clipper and color',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|flutter.widgets.PhysicalShape\n');
      final end = contract.indexOf('\nW|', start) + 1;
      final tokens = canvasColorSchemeThemeTokens.toList()..sort();
      final colors = 'color:any;themeToken:tokens:${tokens.join(',')}';
      expect(
        contract.substring(start, end),
        'W|flutter.widgets.PhysicalShape\n'
        'P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
        'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
        'P|clipper|dartObjectReference,shapeBorderClipper|1|'
        'shapeBorderClipper:roundedRectangle:physicalZero:none|-|'
        'dartObjectReference:dartObjectReference:v1:CustomClipper<Path>:currentOrPackage:'
        'root,optionalMember:reference,zeroArgumentInvocation:requiredConstnessBoolean(false,true);'
        'shapeBorderClipper:shapeBorderClipper:v1:roundedRectangle,beveledRectangle,'
        'continuousRectangle,roundedSuperellipse,circle,stadium:finiteNonNegativeRadius:'
        'explicitDirectional:ltr,rtl\n'
        'P|color|color,themeToken|1|color:0xFF2196F3|-|$colors\n'
        'P|elevation|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|'
        'double:range:0:1:*:1;integer:range:0:1:9007199254740991:1\n'
        'P|shadowColor|color,themeToken|0|-|-|$colors\n'
        'S|child|single|0|0|1|any\n',
      );
    },
  );

  test('PhysicalShape rejects malformed union values and project-code leaks', () {
    for (final property in ['clipper', 'color']) {
      final model = _model();
      (_findJson(model['root']! as Map<String, Object?>)!['properties']!
              as Map<String, Object?>)
          .remove(property);
      expect(
        () => _decode(model),
        throwsFormatException,
        reason: 'required $property',
      );
    }
    for (final property in [
      'clipper',
      'clipBehavior',
      'elevation',
      'color',
      'shadowColor',
    ]) {
      expect(
        () => _decode(
          _model(
            properties: {
              property: {'kind': 'null'},
            },
          ),
        ),
        throwsFormatException,
      );
    }
    for (final malformed in [
      {..._clipper(), 'shape': 'custom'},
      {..._clipper(), 'shape': null},
      {..._clipper(), 'textDirection': 'ambient'},
      {
        ..._clipper(),
        'textDirection': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'rtl',
        },
      },
      {
        ..._clipper(),
        'borderRadius': {'kind': 'borderRadius', 'geometry': _radius()},
      },
      {..._clipper(), 'borderRadius': null},
      {..._clipper(), 'borderRadius': _radius(negative: true)},
      {
        ..._clipper(),
        'borderRadius': {..._radius(), 'extra': true},
      },
      {..._clipper(), 'extra': true},
      {..._clipper(), 'libraryUri': 'package:private_app/secret.dart'},
      {..._clipper(), 'shapeReference': 'PrivateClipper'},
      {..._presence, 'rootSymbol': 'PrivateClipper'},
      {..._presence, 'expectedType': 'CustomClipper<Path>'},
      {'kind': 'dartObjectReference', 'rootSymbol': 'PrivateClipper'},
    ]) {
      expect(
        () => _decode(_model(properties: {'clipper': malformed})),
        throwsFormatException,
        reason: malformed.toString(),
      );
    }
    for (final missing in ['shape', 'borderRadius', 'textDirection']) {
      final malformed = _clipper()..remove(missing);
      expect(
        () => _decode(_model(properties: {'clipper': malformed})),
        throwsFormatException,
      );
    }
    for (final shape in _shapes) {
      CanvasModel invocation() => _decode(
        _model(
          properties: {'clipper': _clipper(shape: shape, directional: true)},
        ),
      );
      if (shape == 'circle' || shape == 'stadium') {
        expect(
          invocation,
          returnsNormally,
          reason: 'ignored radius retained for $shape',
        );
      } else {
        expect(
          invocation,
          throwsFormatException,
          reason:
              'ShapeBorderClipper cannot implicitly resolve directional $shape',
        );
      }
    }
    for (final properties in [
      {
        'elevation': {'kind': 'double', 'value': -0.01},
      },
      {
        'elevation': {'kind': 'integer', 'value': 9007199254740992},
      },
      {
        'elevation': {'kind': 'double', 'value': 'NaN'},
      },
      {'clipBehavior': _enum('Clip', 'future')},
      {
        'color': {'kind': 'color', 'argb': '#112233'},
      },
      {'shadowColor': _token('material.textTheme.bodyLarge')},
      {'shape': _enum('BoxShape', 'circle')},
      {
        'borderRadius': {'kind': 'borderRadius', 'geometry': _radius()},
      },
    ]) {
      expect(
        () => _decode(_model(properties: properties)),
        throwsFormatException,
      );
    }
  });

  test(
    'PhysicalShape accepts optional ordinary children and rejects slot or ParentData abuse',
    () {
      expect(
        canvasDropSlotsForWidgetType('flutter.widgets.PhysicalShape'),
        const [canvasEmptyChildDropSlot],
      );
      expect(
        isCanvasPaletteWrapperWidgetType('flutter.widgets.PhysicalShape'),
        isFalse,
      );
      for (final type in [
        'PhysicalShape',
        'Text',
        'Expanded',
        'Flexible',
        'Spacer',
      ]) {
        final accepts = !['Expanded', 'Flexible', 'Spacer'].contains(type);
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: 'flutter.widgets.PhysicalShape',
            slotName: 'child',
            currentChildCount: 0,
            insertionIndex: 0,
            source: CanvasPaletteDragSource(
              token: type,
              widgetType: 'flutter.widgets.$type',
              traits: canvasWidgetTraitsForType('flutter.widgets.$type'),
            ),
          ),
          accepts,
        );
        if (!accepts) {
          expect(
            () => _decode(
              _model(child: _node(_childId, 'flutter.widgets.$type')),
            ),
            throwsFormatException,
          );
        }
      }
      for (final slots in [
        {
          'child': {'kind': 'list', 'children': <Object?>[]},
        },
        {
          'unknown': {'kind': 'single', 'child': null},
        },
      ]) {
        final model = _model();
        _findJson(model['root']! as Map<String, Object?>)!['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'PhysicalShape uses real SDK defaults and external overlays on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(platform: platform, child: _sizedChild()),
          overlays: true,
        );
        final widget = tester.widget<PhysicalShape>(_physicalFinder());
        final render = tester.renderObject<RenderPhysicalShape>(
          _physicalFinder(),
        );
        expect(widget.clipper, isA<ShapeBorderClipper>());
        final clipper = widget.clipper as ShapeBorderClipper;
        expect(clipper.shape, const RoundedRectangleBorder());
        expect(clipper.textDirection, isNull);
        expect(widget.elevation, 0);
        expect(widget.color, const Color(0xFF2196F3));
        expect(widget.shadowColor, const Color(0xFF000000));
        expect(widget.clipBehavior, Clip.none);
        expect(render.size, _bounds.size);
        expect(render, paints..path(color: widget.color));
        expect(render, isNot(paints..clipPath()));
        expect(render, isNot(paints..shadow()));
        for (final overlay in [
          find.byKey(const ValueKey('canvas-selection-outline-$_physicalId')),
          find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
        ]) {
          expect(overlay, findsOneWidget);
          expect(
            find.ancestor(of: overlay, matching: _physicalFinder()),
            findsNothing,
          );
        }
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'PhysicalShape paints all six SDK shapes, all clips and resolved elliptical radii on $platform',
      (tester) async {
        for (final shape in _shapes) {
          for (final geometry in [
            (directional: false, direction: null, scale: 1.0),
            (directional: false, direction: 'rtl', scale: 8.0),
            (directional: true, direction: 'ltr', scale: 1.0),
            (directional: true, direction: 'rtl', scale: 1.0),
          ]) {
            for (final clip in Clip.values) {
              await _pump(
                tester,
                _model(
                  platform: platform,
                  locale: 'ar',
                  child: _sizedChild(),
                  properties: {
                    'clipper': _clipper(
                      shape: shape,
                      directional: geometry.directional,
                      direction: geometry.direction,
                      scale: geometry.scale,
                    ),
                    'clipBehavior': _enum('Clip', clip.name),
                  },
                ),
              );
              final widget = tester.widget<PhysicalShape>(_physicalFinder());
              final render = tester.renderObject<RenderPhysicalShape>(
                _physicalFinder(),
              );
              final clipper = widget.clipper as ShapeBorderClipper;
              final radius =
                  (geometry.directional ? _directionalValue : _radiusValue) *
                  geometry.scale;
              final expectedShape = _shape(shape, radius);
              final direction = geometry.direction == null
                  ? null
                  : geometry.direction == 'ltr'
                  ? TextDirection.ltr
                  : TextDirection.rtl;
              expect(clipper.shape, expectedShape);
              expect(
                clipper.textDirection,
                direction,
                reason: 'explicit direction wins over Arabic ambient locale',
              );
              final expectedPath = expectedShape.getOuterPath(
                _bounds,
                textDirection: direction,
              );
              final actualPath = clipper.getClip(_bounds.size);
              _expectSamePath(actualPath, expectedPath);
              expect(render.clipBehavior, clip);
              expect(render.size, _bounds.size);
              if (clip == Clip.none) {
                expect(render, isNot(paints..clipPath()));
              } else {
                expect(render, paints..clipPath());
              }
              if (clip == Clip.antiAliasWithSaveLayer) {
                expect(
                  render,
                  paints
                    ..something((method, arguments) => method == #saveLayer),
                );
              } else {
                expect(render, paints..path(color: widget.color));
              }
              for (final point in [
                const Offset(1, 1),
                const Offset(40, 30),
                const Offset(79, 59),
              ]) {
                expect(
                  render.hitTest(BoxHitTestResult(), position: point),
                  actualPath.contains(point),
                );
              }
              expect(
                render.hitTest(
                  BoxHitTestResult(),
                  position: const Offset(81, 1),
                ),
                isFalse,
              );
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    testWidgets(
      'PhysicalShape circle and stadium retain ignored directional radius for later edits on $platform',
      (tester) async {
        RenderPhysicalShape? first;
        for (final shape in [
          'roundedRectangle',
          'circle',
          'stadium',
          'roundedRectangle',
        ]) {
          final json = _clipper(
            shape: shape,
            directional: true,
            direction: 'rtl',
          );
          final model = _decode(
            _model(
              platform: platform,
              child: _sizedChild(),
              properties: {
                'clipper': json,
                'clipBehavior': _enum('Clip', 'antiAlias'),
              },
            ),
          );
          final retained =
              _find(model.root)!.properties['clipper']!.value
                  as CanvasShapeBorderClipperValue;
          expect(retained.borderRadius.radii.first.x, 20);
          expect(retained.borderRadius.radii.first.y, 18);
          await tester.pumpWidget(
            CanvasModelApp(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          );
          await tester.pump();
          final render = tester.renderObject<RenderPhysicalShape>(
            _physicalFinder(),
          );
          first ??= render;
          expect(render, same(first));
          final clipper = render.clipper! as ShapeBorderClipper;
          _expectSamePath(
            clipper.getClip(_bounds.size),
            _shape(
              shape,
              _directionalValue,
            ).getOuterPath(_bounds, textDirection: TextDirection.rtl),
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'PhysicalShape renders exact ARGB, elevation and transparent shadow on $platform',
      (tester) async {
        final previousShadows = debugDisableShadows;
        debugDisableShadows = false;
        try {
          for (final elevation in [0, 7, 3.25]) {
            for (final argb in ['0xFF123456', '0x80123456', '0x00123456']) {
              await _pump(
                tester,
                _model(
                  platform: platform,
                  child: _sizedChild(),
                  properties: {
                    'clipper': _clipper(shape: 'beveledRectangle'),
                    'color': {'kind': 'color', 'argb': argb},
                    'shadowColor': {'kind': 'color', 'argb': '0x80445566'},
                    'elevation': {
                      'kind': elevation is int ? 'integer' : 'double',
                      'value': elevation,
                    },
                  },
                ),
              );
              final render = tester.renderObject<RenderPhysicalShape>(
                _physicalFinder(),
              );
              final color = Color(int.parse(argb.substring(2), radix: 16));
              expect(render.color, color);
              expect(render.shadowColor, const Color(0x80445566));
              expect(render.elevation, elevation);
              expect(render, paints..path(color: color));
              if (elevation == 0) {
                expect(render, isNot(paints..shadow()));
              } else {
                expect(
                  render,
                  paints..shadow(
                    color: const Color(0x80445566),
                    elevation: elevation.toDouble(),
                    transparentOccluder: color.a != 1,
                  ),
                );
              }
              expect(tester.takeException(), isNull);
            }
          }
        } finally {
          debugDisableShadows = previousShadows;
        }
      },
    );

    testWidgets('PhysicalShape resolves theme fill and shadow on $platform', (
      tester,
    ) async {
      await _pump(
        tester,
        _model(
          platform: platform,
          child: _sizedChild(),
          properties: {
            'color': _token('material.colorScheme.primary'),
            'shadowColor': _token('material.colorScheme.shadow'),
          },
        ),
      );
      final context = tester.element(_physicalFinder());
      final render = tester.renderObject<RenderPhysicalShape>(
        _physicalFinder(),
      );
      expect(render.color, Theme.of(context).colorScheme.primary);
      expect(render.shadowColor, Theme.of(context).colorScheme.shadow);
      expect(tester.takeException(), isNull);
    });

    testWidgets(
      'PhysicalShape null child paints nothing even with constrained size and elevation on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(
            platform: platform,
            properties: {
              'elevation': {'kind': 'integer', 'value': 10},
              'clipBehavior': _enum('Clip', 'antiAliasWithSaveLayer'),
            },
          ),
        );
        final render = tester.renderObject<RenderPhysicalShape>(
          _physicalFinder(),
        );
        expect(render.child, isNull);
        expect(render.size, _bounds.size);
        expect(render, paintsNothing);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'PhysicalShape custom reference preserves child with explicit accessible unavailable preview on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        await _pump(
          tester,
          _model(
            platform: platform,
            child: _sizedChild(),
            properties: {
              'clipper': _presence,
              'elevation': {'kind': 'integer', 'value': 7},
              'color': {'kind': 'color', 'argb': '0xFF112233'},
            },
          ),
        );
        expect(
          find.descendant(of: _widget(), matching: find.byType(PhysicalShape)),
          findsNothing,
        );
        expect(tester.getSize(_widget()), _bounds.size);
        expect(
          find.byKey(const ValueKey('canvas-widget-$_childId')),
          findsOneWidget,
        );
        expect(find.bySemanticsLabel(_warning), findsOneWidget);
        expect(
          find.text('Custom clipper\npreview unavailable'),
          findsOneWidget,
        );
        expect(tester.takeException(), isNull);
        semantics.dispose();
      },
    );

    for (final custom in [false, true]) {
      testWidgets(
        'PhysicalShape empty ${custom ? 'custom' : 'built-in'} child retains external zero-size selection and DnD on $platform',
        (tester) async {
          final semantics = tester.ensureSemantics();
          CanvasDropResolver? resolver;
          final selections = <String>[];
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  bounded: false,
                  properties: {if (custom) 'clipper': _presence},
                ),
              ),
              selectedWidgetId: null,
              onSelected: selections.add,
              onDropResolverChanged: (value) => resolver = value,
            ),
          );
          await tester.pump();
          expect(tester.getSize(_widget()), Size.zero);
          if (!custom) {
            expect(
              tester.widget<PhysicalShape>(_physicalFinder()).child,
              isNull,
            );
            expect(
              tester.renderObject<RenderPhysicalShape>(_physicalFinder()).child,
              isNull,
            );
          } else {
            expect(find.byTooltip(_warning), findsWidgets);
          }
          final target = find.byKey(
            const ValueKey('canvas-zero-size-widget-target-$_physicalId'),
          );
          expect(tester.getSize(target), const Size.square(36));
          await tester.tap(target);
          expect(selections.last, _physicalId);
          final surface = tester.getRect(find.byType(CanvasDocumentView));
          final point = tester.getCenter(target);
          final drop = resolver!(
            ((point.dx - surface.left) / surface.width * 1000000).round(),
            ((point.dy - surface.top) / surface.height * 1000000).round(),
          );
          expect(drop?.parentWidgetId, _physicalId);
          expect(drop?.slotName, 'child');
          expect(drop?.insertionIndex, 0);
          expect(drop?.zone?.isEmpty, isFalse);
          expect(tester.takeException(), isNull);
          semantics.dispose();
        },
      );
    }
  }
}

Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  bool overlays = false,
}) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: _decode(model),
      selectedWidgetId: overlays ? _physicalId : null,
      onSelected: (_) {},
      dropHoverTarget: overlays
          ? const CanvasDropTarget(
              parentWidgetId: _physicalId,
              slotName: 'child',
              insertionIndex: 0,
              zone: CanvasDropZone(
                leftMicros: 400000,
                topMicros: 400000,
                rightMicros: 600000,
                bottomMicros: 600000,
              ),
            )
          : null,
    ),
  );
  await tester.pump();
}

void _expectSamePath(Path actual, Path expected) {
  expect(actual.getBounds(), expected.getBounds());
  for (var x = 0.5; x < 80; x += 3) {
    for (var y = 0.5; y < 60; y += 3) {
      expect(actual.contains(Offset(x, y)), expected.contains(Offset(x, y)));
    }
  }
}

ShapeBorder _shape(String shape, BorderRadiusGeometry radius) =>
    switch (shape) {
      'roundedRectangle' => RoundedRectangleBorder(borderRadius: radius),
      'beveledRectangle' => BeveledRectangleBorder(borderRadius: radius),
      'continuousRectangle' => ContinuousRectangleBorder(borderRadius: radius),
      'roundedSuperellipse' => RoundedSuperellipseBorder(borderRadius: radius),
      'circle' => const CircleBorder(),
      'stadium' => const StadiumBorder(),
      _ => throw StateError('unreviewed shape'),
    };

Finder _widget() => find.byKey(const ValueKey('canvas-widget-$_physicalId'));
Finder _physicalFinder() =>
    find.descendant(of: _widget(), matching: find.byType(PhysicalShape)).first;
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));

CanvasNode? _find(CanvasNode node) {
  if (node.id == _physicalId) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final result = _find(child);
      if (result != null) return result;
    }
  }
  return null;
}

Map<String, Object?>? _findJson(Map<String, Object?> node) {
  if (node['id'] == _physicalId) return node;
  for (final raw in (node['slots']! as Map<String, Object?>).values) {
    final slot = raw! as Map<String, Object?>;
    if (slot['child'] case final Map<String, Object?> child) {
      final found = _findJson(child);
      if (found != null) return found;
    }
  }
  return null;
}

Map<String, Object?> _model({
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
  bool bounded = true,
  String platform = 'windows',
  String locale = 'en',
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)
    ..['targetPlatform'] = platform
    ..['locale'] = locale;
  final physical = _node(
    _physicalId,
    'flutter.widgets.PhysicalShape',
    child: child,
    properties: {
      'clipper': _clipper(zero: true),
      'color': {'kind': 'color', 'argb': '0xFF2196F3'},
      ...properties,
    },
  );
  final content = bounded
      ? _node(
          'a9d4a4e9-3d47-4ec3-8a1f-c028fb7db7fa',
          'flutter.widgets.SizedBox',
          child: physical,
          properties: {
            'width': {'kind': 'integer', 'value': 80},
            'height': {'kind': 'integer', 'value': 60},
          },
        )
      : physical;
  model['root'] = {
    'id': '8f6d475c-ee22-4a1f-9168-7672d5149a02',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          'c80e65d8-d7b7-405a-b21c-cec2138272b4',
          'flutter.widgets.Center',
          child: content,
        ),
      },
    },
  };
  return model;
}

Map<String, Object?> _clipper({
  String shape = 'roundedRectangle',
  bool directional = false,
  String? direction,
  double scale = 1,
  bool zero = false,
}) => {
  'kind': 'shapeBorderClipper',
  'shape': shape,
  'borderRadius': _radius(directional: directional, scale: zero ? 0 : scale),
  'textDirection': direction,
};
Map<String, Object?> _radius({
  bool directional = false,
  double scale = 1,
  bool negative = false,
}) => {
  'kind': directional ? 'directional' : 'physical',
  directional ? 'topStart' : 'topLeft': {
    'x': negative ? -1.0 : 20.0 * scale,
    'y': 18.0 * scale,
  },
  directional ? 'topEnd' : 'topRight': {'x': 12.0 * scale, 'y': 10.0 * scale},
  directional ? 'bottomEnd' : 'bottomRight': {
    'x': 8.0 * scale,
    'y': 6.0 * scale,
  },
  directional ? 'bottomStart' : 'bottomLeft': {
    'x': 4.0 * scale,
    'y': 2.0 * scale,
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
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _token(String token) => {
  'kind': 'themeToken',
  'token': token,
};
