import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _physicalId = 'd4b76528-7bc1-497c-b905-a430abf61b02';
const _childId = 'd4b76528-7bc1-497c-b905-a430abf61b03';
const _bounds = Rect.fromLTWH(0, 0, 80, 60);
const _physicalRadius = BorderRadius.only(
  topLeft: Radius.elliptical(20, 18),
  topRight: Radius.elliptical(12, 10),
  bottomRight: Radius.elliptical(8, 6),
  bottomLeft: Radius.elliptical(4, 2),
);
const _oversizedRadius = BorderRadius.only(
  topLeft: Radius.elliptical(120, 80),
  topRight: Radius.elliptical(160, 100),
  bottomRight: Radius.elliptical(180, 120),
  bottomLeft: Radius.elliptical(140, 90),
);

void main() {
  test(
    'PhysicalModel decodes all six properties and retains ignored radius',
    () {
      for (final shape in ['rectangle', 'circle']) {
        for (final clip in Clip.values) {
          final properties = <String, Object?>{
            'shape': _enum('BoxShape', shape),
            'clipBehavior': _enum('Clip', clip.name),
            'borderRadius': _radius(),
            'elevation': {'kind': 'double', 'value': 2.5},
            'color': {'kind': 'color', 'argb': '0x80123456'},
            'shadowColor': _token('material.colorScheme.shadow'),
          };
          final before = jsonEncode(properties);
          final node = _physical(_decode(_model(properties: properties)).root);
          expect(node.properties, hasLength(6));
          expect(
            (node.properties['shape']!.value as CanvasEnumValue).value,
            shape,
          );
          expect(
            (node.properties['clipBehavior']!.value as CanvasEnumValue).value,
            clip.name,
          );
          expect(node.properties['elevation']!.value, 2.5);
          final radius =
              node.properties['borderRadius']!.value
                  as CanvasPhysicalBorderRadiusValue;
          expect(radius.topLeft.x, 20);
          expect(radius.topLeft.y, 18);
          expect(radius.bottomLeft.x, 4);
          expect(radius.bottomLeft.y, 2);
          expect(jsonEncode(properties), before);
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
          reason: token,
        );
      }
    },
  );

  test('PhysicalModel contract is exact, required-color and physical-only', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.PhysicalModel\n');
    final end = contract.indexOf('W|flutter.widgets.PhysicalShape\n', start);
    final tokens = canvasColorSchemeThemeTokens.toList()..sort();
    final colorConstraint = 'color:any;themeToken:tokens:${tokens.join(',')}';
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.PhysicalModel\n'
      'P|borderRadius|borderRadius|0|-|-|'
      'borderRadius:borderRadius:v1:physical:finiteNonNegative\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|color|color,themeToken|1|color:0xFF2196F3|-|$colorConstraint\n'
      'P|elevation|double,integer|0|-|double:0:1:*:1;'
      'integer:0:1:9007199254740991:1|double:range:0:1:*:1;'
      'integer:range:0:1:9007199254740991:1\n'
      'P|shadowColor|color,themeToken|0|-|-|$colorConstraint\n'
      'P|shape|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'BoxShape:circle,rectangle\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test(
    'PhysicalModel fails closed for malformed values and directional radii',
    () {
      expect(
        () => _decode(_model(defaultColor: false)),
        throwsFormatException,
        reason:
            'color is a required constructor argument, not a preview default',
      );
      for (final property in [
        'shape',
        'clipBehavior',
        'borderRadius',
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
          reason:
              '$property uses omission, not an unreviewed explicit null kind',
        );
      }
      for (final properties in <Map<String, Object?>>[
        {'shape': _enum('BoxShape', 'ellipse')},
        {'shape': _enum('Clip', 'none')},
        {'clipBehavior': _enum('Clip', 'future')},
        {
          'color': {'kind': 'color', 'argb': '#112233'},
        },
        {'color': _token('material.textTheme.bodyLarge')},
        {'shadowColor': _token('material.colorScheme.future')},
        {
          'shadowColor': {'kind': 'color', 'argb': '0xFFFFFFFF', 'extra': true},
        },
        {
          'elevation': {'kind': 'double', 'value': -0.01},
        },
        {
          'elevation': {'kind': 'integer', 'value': -1},
        },
        {
          'elevation': {'kind': 'integer', 'value': 1.5},
        },
        {
          'elevation': {'kind': 'integer', 'value': 9007199254740992},
        },
        {
          'elevation': {'kind': 'double', 'value': 'Infinity'},
        },
        {
          'elevation': {'kind': 'string', 'value': '1'},
        },
        {
          'borderRadius': {'kind': 'borderRadius', 'geometry': {}},
        },
        {'borderRadius': _radius(extra: true)},
        {'borderRadius': _radius(negative: true)},
        {
          'clipper': {'kind': 'dartObjectReferencePresence'},
        },
        {
          'futureProperty': {'kind': 'boolean', 'value': true},
        },
      ]) {
        expect(
          () => _decode(_model(properties: properties)),
          throwsFormatException,
          reason: properties.toString(),
        );
      }
      for (final shape in ['rectangle', 'circle']) {
        expect(
          () => _decode(
            _model(
              properties: {
                'shape': _enum('BoxShape', shape),
                'borderRadius': _radius(directional: true),
              },
            ),
          ),
          throwsFormatException,
          reason:
              'PhysicalModel requires BorderRadius even when shape is $shape',
        );
      }
    },
  );

  test(
    'PhysicalModel accepts optional ordinary child and closes slot mutations',
    () {
      expect(
        canvasDropSlotsForWidgetType('flutter.widgets.PhysicalModel'),
        const [canvasEmptyChildDropSlot],
      );
      expect(
        isCanvasPaletteWrapperWidgetType('flutter.widgets.PhysicalModel'),
        isFalse,
      );
      for (final entry in {
        'flutter.widgets.PhysicalModel': true,
        'flutter.widgets.Text': true,
        'flutter.material.AppBar': true,
        'flutter.widgets.Expanded': false,
        'flutter.widgets.Flexible': false,
        'flutter.widgets.Spacer': false,
      }.entries) {
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: 'flutter.widgets.PhysicalModel',
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
      for (final childType in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () => _decode(
            _model(child: _node(_childId, 'flutter.widgets.$childType')),
          ),
          throwsFormatException,
        );
      }
      for (final slots in [
        {
          'child': {'kind': 'list', 'children': <Object?>[]},
        },
        {
          'futureSlot': {'kind': 'single', 'child': null},
        },
      ]) {
        final model = _model(bounded: false);
        final root = model['root']! as Map<String, Object?>;
        final rootSlots = root['slots']! as Map<String, Object?>;
        final body = rootSlots['body']! as Map<String, Object?>;
        final center = body['child']! as Map<String, Object?>;
        final centerSlots = center['slots']! as Map<String, Object?>;
        final centerChild = centerSlots['child']! as Map<String, Object?>;
        (centerChild['child']! as Map<String, Object?>)['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'PhysicalModel renders native defaults and external overlays on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(_model(platform: platform, child: _sizedChild())),
            selectedWidgetId: _physicalId,
            onSelected: (_) {},
            dropHoverTarget: const CanvasDropTarget(
              parentWidgetId: _physicalId,
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
        final widget = tester.widget<PhysicalModel>(_physicalFinder());
        final render = tester.renderObject<RenderPhysicalModel>(
          _physicalFinder(),
        );
        expect(widget.shape, BoxShape.rectangle);
        expect(render.shape, BoxShape.rectangle);
        expect(widget.borderRadius, isNull);
        expect(render.borderRadius, isNull);
        expect(widget.elevation, 0);
        expect(render.elevation, 0);
        expect(widget.clipBehavior, Clip.none);
        expect(render.clipBehavior, Clip.none);
        expect(widget.color, const Color(0xFF2196F3));
        expect(render.color, const Color(0xFF2196F3));
        expect(widget.shadowColor, const Color(0xFF000000));
        expect(render.shadowColor, const Color(0xFF000000));
        expect(render.size, _bounds.size);
        expect(
          render,
          paints..rrect(
            rrect: BorderRadius.zero.toRRect(_bounds),
            color: widget.color,
          ),
        );
        expect(render, isNot(paints..clipRRect()));
        expect(render, isNot(paints..shadow()));
        expect(
          find.bySemanticsLabel(RegExp('SizedBox $_childId')),
          findsOneWidget,
        );
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
        semantics.dispose();
      },
    );

    testWidgets(
      'PhysicalModel paints both shapes, elliptical and clamped radii, and every clip on $platform',
      (tester) async {
        for (final shape in BoxShape.values) {
          for (final geometry in [
            (json: <String, Object?>{}, radius: null),
            (json: _radius(), radius: _physicalRadius),
            (json: _radius(oversized: true), radius: _oversizedRadius),
          ]) {
            for (final clip in Clip.values) {
              await tester.pumpWidget(
                CanvasModelApp(
                  model: _decode(
                    _model(
                      platform: platform,
                      child: _sizedChild(),
                      properties: {
                        'shape': _enum('BoxShape', shape.name),
                        if (geometry.json.isNotEmpty)
                          'borderRadius': geometry.json,
                        'clipBehavior': _enum('Clip', clip.name),
                      },
                    ),
                  ),
                  selectedWidgetId: null,
                  onSelected: (_) {},
                ),
              );
              await tester.pump();
              final widget = tester.widget<PhysicalModel>(_physicalFinder());
              final render = tester.renderObject<RenderPhysicalModel>(
                _physicalFinder(),
              );
              expect(widget.shape, shape);
              expect(render.shape, shape);
              expect(widget.borderRadius, geometry.radius);
              expect(render.borderRadius, geometry.radius);
              expect(render.clipBehavior, clip);
              expect(render.size, _bounds.size);
              final rrect = shape == BoxShape.circle
                  ? RRect.fromRectXY(_bounds, 40, 30)
                  : (geometry.radius ?? BorderRadius.zero).toRRect(_bounds);
              if (clip == Clip.antiAliasWithSaveLayer) {
                expect(
                  render,
                  paints
                    ..something((method, arguments) => method == #saveLayer),
                );
                expect(
                  render,
                  paints..something(
                    (method, arguments) =>
                        method == #drawPaint &&
                        (arguments.single as Paint).color.toARGB32() ==
                            widget.color.toARGB32(),
                  ),
                );
                expect(render, isNot(paints..rrect()));
              } else {
                expect(
                  render,
                  paints..rrect(rrect: rrect, color: widget.color),
                );
              }
              if (clip == Clip.none) {
                expect(render, isNot(paints..clipRRect()));
              } else {
                expect(render, paints..clipRRect(rrect: rrect));
              }
              expect(
                render.describeApproximatePaintClip(render.child!),
                clip == Clip.none ? null : _bounds,
              );
              expect(
                render.hitTest(
                  BoxHitTestResult(),
                  position: const Offset(1, 1),
                ),
                isTrue,
                reason:
                    'SDK PhysicalModel retains bounding-box hits without a clipper',
              );
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
      'PhysicalModel circle ignores but retains radius for later rectangle edits on $platform',
      (tester) async {
        RenderPhysicalModel? first;
        for (final shape in ['rectangle', 'circle', 'rectangle']) {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  child: _sizedChild(),
                  properties: {
                    'shape': _enum('BoxShape', shape),
                    'borderRadius': _radius(),
                    'clipBehavior': _enum('Clip', 'antiAlias'),
                  },
                ),
              ),
              selectedWidgetId: _physicalId,
              onSelected: (_) {},
            ),
          );
          await tester.pump();
          final render = tester.renderObject<RenderPhysicalModel>(
            _physicalFinder(),
          );
          first ??= render;
          expect(render, same(first));
          expect(render.borderRadius, _physicalRadius);
          expect(
            render,
            paints..clipRRect(
              rrect: shape == 'circle'
                  ? RRect.fromRectXY(_bounds, 40, 30)
                  : _physicalRadius.toRRect(_bounds),
            ),
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'PhysicalModel draws exact elevation, ARGB fill and transparent shadows on $platform',
      (tester) async {
        final previousShadows = debugDisableShadows;
        debugDisableShadows = false;
        try {
          for (final elevation in [0, 7, 3.25]) {
            for (final argb in ['0xFF123456', '0x80123456', '0x00123456']) {
              await tester.pumpWidget(
                CanvasModelApp(
                  model: _decode(
                    _model(
                      platform: platform,
                      child: _sizedChild(),
                      properties: {
                        'color': {'kind': 'color', 'argb': argb},
                        'shadowColor': {'kind': 'color', 'argb': '0x80445566'},
                        'elevation': {
                          'kind': elevation is int ? 'integer' : 'double',
                          'value': elevation,
                        },
                        'borderRadius': _radius(),
                      },
                    ),
                  ),
                  selectedWidgetId: null,
                  onSelected: (_) {},
                ),
              );
              await tester.pump();
              final render = tester.renderObject<RenderPhysicalModel>(
                _physicalFinder(),
              );
              final color = Color(int.parse(argb.substring(2), radix: 16));
              expect(render.color, color);
              expect(render.shadowColor, const Color(0x80445566));
              expect(render.elevation, elevation.toDouble());
              expect(
                render,
                paints..rrect(
                  rrect: _physicalRadius.toRRect(_bounds),
                  color: color,
                ),
              );
              if (elevation == 0) {
                expect(render, isNot(paints..shadow()));
              } else {
                expect(
                  render,
                  paints..shadow(
                    color: const Color(0x80445566),
                    elevation: elevation.toDouble(),
                    transparentOccluder: !argb.startsWith('0xFF'),
                    includes: [const Offset(40, 30)],
                    excludes: [const Offset(0, 0), const Offset(81, 30)],
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

    testWidgets(
      'PhysicalModel resolves fill and shadow theme roles in light and dark on $platform',
      (tester) async {
        final previousShadows = debugDisableShadows;
        debugDisableShadows = false;
        try {
          for (final brightness in ['light', 'dark']) {
            final model = _model(
              platform: platform,
              child: _sizedChild(),
              properties: {
                'color': _token('material.colorScheme.primary'),
                'shadowColor': _token('material.colorScheme.shadow'),
                'elevation': {'kind': 'double', 'value': 4.0},
              },
            );
            final theme =
                (model['profile']! as Map<String, Object?>)['theme']!
                    as Map<String, Object?>;
            theme
              ..['brightness'] = brightness
              ..['definitionId'] = brightness
              ..['digestIdentity'] = (brightness == 'light' ? 'A' : 'B') * 64
              ..['colorScheme'] = {
                'primary': brightness == 'light' ? '0xFF112233' : '0xFF445566',
                'shadow': brightness == 'light' ? '0x80667788' : '0x8099AABB',
              };
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(model),
                selectedWidgetId: null,
                onSelected: (_) {},
              ),
            );
            await tester.pump();
            final render = tester.renderObject<RenderPhysicalModel>(
              _physicalFinder(),
            );
            final scheme = Theme.of(
              tester.element(_physicalFinder()),
            ).colorScheme;
            expect(render.color, scheme.primary);
            expect(render.shadowColor, scheme.shadow);
            expect(render, paints..rrect(color: scheme.primary));
            expect(
              render,
              paints..shadow(
                color: scheme.shadow,
                elevation: 4.0,
                transparentOccluder: false,
              ),
            );
            expect(tester.takeException(), isNull);
          }
        } finally {
          debugDisableShadows = previousShadows;
        }
      },
    );

    testWidgets(
      'PhysicalModel empty bounded child preserves native no-paint behavior on $platform',
      (tester) async {
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                properties: {
                  'elevation': {'kind': 'integer', 'value': 10},
                  'clipBehavior': _enum('Clip', 'antiAliasWithSaveLayer'),
                },
              ),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final render = tester.renderObject<RenderPhysicalModel>(
          _physicalFinder(),
        );
        expect(render.child, isNull);
        expect(render.size, _bounds.size);
        expect(render, paintsNothing);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'PhysicalModel empty child stays truly absent with external zero-size DnD on $platform',
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
        final widget = tester.widget<PhysicalModel>(_physicalFinder());
        final render = tester.renderObject<RenderPhysicalModel>(
          _physicalFinder(),
        );
        expect(widget.child, isNull);
        expect(render.child, isNull);
        expect(tester.getSize(_widget()), Size.zero);
        expect(render, isNot(paints..rrect()));
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
      },
    );
  }
}

Finder _widget() => find.byKey(const ValueKey('canvas-widget-$_physicalId'));
Finder _physicalFinder() =>
    find.descendant(of: _widget(), matching: find.byType(PhysicalModel)).first;

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));

CanvasNode _physical(CanvasNode node) {
  if (node.id == _physicalId) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final match = _findPhysical(child);
      if (match != null) return match;
    }
  }
  throw StateError('PhysicalModel absent');
}

CanvasNode? _findPhysical(CanvasNode node) {
  if (node.id == _physicalId) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _findPhysical(child);
      if (found != null) return found;
    }
  }
  return null;
}

Map<String, Object?> _model({
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
  bool bounded = true,
  bool defaultColor = true,
  String platform = 'windows',
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  final physical = _node(
    _physicalId,
    'flutter.widgets.PhysicalModel',
    properties: {
      if (defaultColor) 'color': {'kind': 'color', 'argb': '0xFF2196F3'},
      ...properties,
    },
    child: child,
  );
  final content = bounded
      ? _node(
          'd4b76528-7bc1-497c-b905-a430abf61b04',
          'flutter.widgets.SizedBox',
          child: physical,
          properties: {
            'width': {'kind': 'integer', 'value': 80},
            'height': {'kind': 'integer', 'value': 60},
          },
        )
      : physical;
  model['root'] = {
    'id': 'd4b76528-7bc1-497c-b905-a430abf61b05',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          'd4b76528-7bc1-497c-b905-a430abf61b06',
          'flutter.widgets.Center',
          child: content,
        ),
      },
    },
  };
  return model;
}

Map<String, Object?> _radius({
  bool directional = false,
  bool oversized = false,
  bool negative = false,
  bool extra = false,
}) => {
  'kind': 'borderRadius',
  'geometry': {
    'kind': directional ? 'directional' : 'physical',
    directional ? 'topStart' : 'topLeft': {
      'x': negative
          ? -1.0
          : oversized
          ? 120.0
          : 20.0,
      'y': oversized ? 80.0 : 18.0,
      if (extra) 'z': 0,
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

Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _token(String token) => {
  'kind': 'themeToken',
  'token': token,
};
