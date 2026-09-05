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

const _clipId = 'c3a65417-7bc1-497c-b905-a430abf61a02';
const _childId = 'c3a65417-7bc1-497c-b905-a430abf61a03';
const _presence = <String, Object?>{'kind': 'dartObjectReferencePresence'};

void main() {
  test('ClipPath schema covers default, path delegate and shape helper', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ClipPath\n');
    final end = contract.indexOf('W|flutter.widgets.ClipRRect\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.ClipPath\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|clipper|dartObjectReference|0|-|-|'
      'dartObjectReference:dartObjectReference:v1:CustomClipper<Path>:'
      'currentOrPackage:root,optionalMember:reference,'
      'zeroArgumentInvocation:requiredConstnessBoolean(false,true)\n'
      'P|shape|dartObjectReference|0|-|-|'
      'dartObjectReference:dartObjectReference:v1:ShapeBorder:'
      'currentOrPackage:root,optionalMember:reference,'
      'zeroArgumentInvocation:requiredConstnessBoolean(false,true)\n'
      'S|child|single|0|0|1|any\n',
    );
    for (final properties in const <Map<String, Object?>>[
      {},
      {'clipper': _presence},
      {'shape': _presence},
    ]) {
      final node = _decode(
        _model(properties: properties),
      ).root.slot('body')!.child!.slot('child')!.child!.slot('child')!.child!;
      expect(node.type, 'flutter.widgets.ClipPath');
      expect(node.properties.keys, properties.keys);
      for (final property in properties.keys) {
        expect(node.properties[property]!.kind, 'dartObjectReferencePresence');
        expect(node.properties[property]!.value, isTrue);
      }
    }
  });

  test('ClipPath rejects conflicting, malformed or leaked object branches', () {
    for (final properties in <Map<String, Object?>>[
      {'clipper': _presence, 'shape': _presence},
      {
        'clipper': {'kind': 'callbackPresence'},
      },
      {
        'shape': {'kind': 'callbackPresence'},
      },
      {
        'shape': {'kind': 'string', 'value': 'CircleBorder()'},
      },
      {
        'clipper': {'kind': 'null'},
      },
      {
        'shape': {'kind': 'null'},
      },
      {
        'shape': {
          'kind': 'dartObjectReferencePresence',
          'rootSymbol': 'secret',
        },
      },
      {
        'clipper': {'kind': 'dartObjectReferencePresence', 'uri': 'secret'},
      },
      {
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'unknown'},
      },
      {
        'borderRadius': {'kind': 'integer', 'value': 8},
      },
    ]) {
      expect(
        () => _decode(_model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    final invalidParentData = _node(
      'c3a65417-7bc1-497c-b905-a430abf61a07',
      'flutter.widgets.Expanded',
      child: _sizedChild(),
    );
    expect(
      () => _decode(_model(child: invalidParentData)),
      throwsFormatException,
    );
    final invalidSlot = _model();
    _clipNode(invalidSlot)['slots'] = {
      'children': {'kind': 'list', 'children': <Object?>[]},
    };
    expect(() => _decode(invalidSlot), throwsFormatException);
  });

  test(
    'ClipPath has an optional ordinary child and is not a ParentData wrapper',
    () {
      expect(canvasDropSlotsForWidgetType('flutter.widgets.ClipPath'), const [
        canvasEmptyChildDropSlot,
      ]);
      expect(
        isCanvasPaletteWrapperWidgetType('flutter.widgets.ClipPath'),
        isFalse,
      );
      for (final type in [
        'flutter.widgets.Text',
        'flutter.widgets.ClipPath',
        'flutter.material.AppBar',
      ]) {
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: 'flutter.widgets.ClipPath',
            slotName: 'child',
            currentChildCount: 0,
            insertionIndex: 0,
            source: CanvasPaletteDragSource(
              token: type,
              widgetType: type,
              traits: canvasWidgetTraitsForType(type),
            ),
          ),
          isTrue,
        );
      }
      for (final type in [
        'flutter.widgets.Expanded',
        'flutter.widgets.Flexible',
        'flutter.widgets.Spacer',
      ]) {
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: 'flutter.widgets.ClipPath',
            slotName: 'child',
            currentChildCount: 0,
            insertionIndex: 0,
            source: CanvasPaletteDragSource(
              token: type,
              widgetType: type,
              traits: canvasWidgetTraitsForType(type),
            ),
          ),
          isFalse,
          reason: type,
        );
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'ClipPath uses real default rectangle and all clips on $platform',
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
              .descendant(of: _widget(), matching: find.byType(ClipPath))
              .first;
          final widget = tester.widget<ClipPath>(finder);
          final render = tester.renderObject<RenderClipPath>(finder);
          expect(widget.clipper, isNull);
          expect(render.clipper, isNull);
          expect(widget.clipBehavior, clip.value);
          expect(render.clipBehavior, clip.value);
          expect(render.size, const Size(80, 60));
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
      'ClipPath omitted child stays nullable with external DnD on $platform',
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
            .descendant(of: _widget(), matching: find.byType(ClipPath))
            .first;
        expect(tester.widget<ClipPath>(finder).child, isNull);
        expect(tester.renderObject<RenderClipPath>(finder).child, isNull);
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

    for (final branch in ['clipper', 'shape']) {
      final widgetName = branch == 'shape' ? 'ClipPath.shape' : 'ClipPath';
      final expectedType = branch == 'shape'
          ? 'ShapeBorder'
          : 'CustomClipper<Path>';
      final message =
          'Custom $widgetName preview unavailable. Generated Dart uses the '
          'configured $expectedType; isolated Canvas does not execute project '
          'or dependency Dart.';
      testWidgets(
        'ClipPath $branch keeps child and explains unavailable preview on $platform',
        (tester) async {
          final semantics = tester.ensureSemantics();
          final selections = <String>[];
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  child: _sizedChild(),
                  properties: {branch: _presence},
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
            find.descendant(of: _widget(), matching: find.byType(ClipPath)),
            findsNothing,
          );
          expect(warning, findsOneWidget);
          expect(
            find.text(
              branch == 'shape'
                  ? 'Custom shape\npreview unavailable'
                  : 'Custom clipper\npreview unavailable',
            ),
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
        'ClipPath empty $branch preserves zero layout, warning and DnD on $platform',
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
                  properties: {branch: _presence},
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
            find.descendant(of: _widget(), matching: find.byType(ClipPath)),
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
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  final clip = _node(
    _clipId,
    'flutter.widgets.ClipPath',
    properties: properties,
    child: child,
  );
  final content = bounded
      ? _node(
          'c3a65417-7bc1-497c-b905-a430abf61a04',
          'flutter.widgets.SizedBox',
          child: clip,
          properties: {
            'width': {'kind': 'integer', 'value': 80},
            'height': {'kind': 'integer', 'value': 60},
          },
        )
      : clip;
  model['root'] = {
    'id': 'c3a65417-7bc1-497c-b905-a430abf61a05',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          'c3a65417-7bc1-497c-b905-a430abf61a06',
          'flutter.widgets.Center',
          child: content,
        ),
      },
    },
  };
  return model;
}

Map<String, Object?> _clipNode(Map<String, Object?> model) {
  final root = model['root']! as Map<String, Object?>;
  var result =
      ((root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child']!
          as Map<String, Object?>;
  while (result['type'] != 'flutter.widgets.ClipPath') {
    result =
        ((result['slots']! as Map<String, Object?>)['child']!
                as Map<String, Object?>)['child']!
            as Map<String, Object?>;
  }
  return result;
}

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
